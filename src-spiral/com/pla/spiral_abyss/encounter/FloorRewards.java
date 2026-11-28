/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  dev.architectury.platform.Platform
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.ExperienceOrb
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.storage.loot.LootDataId
 *  net.minecraft.world.level.storage.loot.LootDataType
 *  net.minecraft.world.level.storage.loot.LootParams
 *  net.minecraft.world.level.storage.loot.LootParams$Builder
 *  net.minecraft.world.level.storage.loot.LootTable
 *  net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
 *  net.minecraft.world.level.storage.loot.parameters.LootContextParams
 *  net.minecraft.world.phys.Vec3
 */
package com.pla.spiral_abyss.encounter;

import com.mojang.logging.LogUtils;
import com.pla.spiral_abyss.encounter.ChallengeRewards;
import com.pla.spiral_abyss.encounter.RewardItems;
import com.pla.spiral_abyss.encounter.RewardPool;
import com.pla.spiral_abyss.encounter.data.FloorCatalog;
import com.pla.spiral_abyss.encounter.data.FloorDefinition;
import com.pla.spiral_abyss.encounter.data.MobCatalog;
import com.pla.spiral_abyss.entity.ChestLootEntity;
import com.pla.spiral_abyss.event.RareRewardBroadcast;
import com.pla.spiral_abyss.platform.AbyssRegistries;
import dev.architectury.platform.Platform;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootDataId;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public final class FloorRewards {
    private static final Set<ResourceLocation> missingTables = ConcurrentHashMap.newKeySet();

    private FloorRewards() {
    }

    public static void resetMissingTableLogs() {
        missingTables.clear();
    }

    static LootTable table(MinecraftServer server, ResourceLocation id) {
        LootTable table = (LootTable)server.m_278653_().m_278667_(new LootDataId(LootDataType.f_278413_, id));
        if (table == null && (id.m_135827_().equals("minecraft") || Platform.isModLoaded((String)id.m_135827_())) && missingTables.add(id)) {
            LogUtils.getLogger().warn("[Spiral Abyss] Ignoring unavailable reward loot table {} (missing or failed to load). Check the configured ID and data/{}/loot_tables/{}.json; other available rewards remain usable.", new Object[]{id, id.m_135827_(), id.m_135815_()});
        }
        return table;
    }

    public static void validateTables(MinecraftServer server) {
        ArrayList entries = new ArrayList();
        FloorCatalog.snapshot().floors().values().forEach(floor -> entries.addAll(floor.rewards().entries()));
        MobCatalog.rewards().values().forEach(entries::addAll);
        entries.stream().filter(entry -> !entry.item()).map(FloorDefinition.RewardEntry::id).distinct().forEach(id -> FloorRewards.table(server, id));
    }

    public static double partyScale(int players) {
        return players <= 0 ? 0.0 : 1.0 + 0.5 * (double)(players - 1);
    }

    public static FloorDefinition.Rewards withMobRewards(FloorDefinition.Rewards base, Set<ResourceLocation> encountered, Map<ResourceLocation, List<FloorDefinition.RewardEntry>> linked) {
        ArrayList<FloorDefinition.RewardEntry> entries = new ArrayList<FloorDefinition.RewardEntry>(base.entries());
        LinkedHashSet additions = new LinkedHashSet();
        encountered.forEach(id -> linked.getOrDefault(id, List.of()).forEach(entry -> additions.add(entry.item() ? RewardPool.rare(entry) : entry)));
        additions.removeAll(base.entries());
        entries.addAll(additions);
        return new FloorDefinition.Rewards(List.copyOf(entries), base.rolls(), base.experience());
    }

    public static Bundle roll(ServerLevel level, Vec3 position, FloorDefinition.Rewards rewards, int tier, int eligiblePlayers) {
        Item item;
        FloorDefinition.RewardEntry rare;
        ArrayList<ItemStack> items = new ArrayList<ItemStack>();
        HashSet<Integer> rareItems = new HashSet<Integer>();
        LootParams params = new LootParams.Builder(level).m_287286_(LootContextParams.f_81460_, (Object)position).m_287235_(LootContextParamSets.f_81411_);
        int rolls = ChallengeRewards.rollCount(tier, eligiblePlayers);
        RewardPool pool = new RewardPool(rewards.entries(), entry -> entry.item() ? FloorRewards.availableItem(entry) : FloorRewards.table(level.m_7654_(), entry.id()) != null);
        List<FloorDefinition.RewardEntry> bonuses = rewards.entries().stream().filter(entry -> entry.item() && !entry.rare()).toList();
        List<FloorDefinition.RewardEntry> rareBonuses = rewards.entries().stream().filter(entry -> entry.item() && entry.rare()).toList();
        RewardPool normalPool = new RewardPool(bonuses, FloorRewards::availableItem);
        RewardPool rarePool = new RewardPool(rareBonuses, FloorRewards::availableItem);
        for (int i = 0; i < rolls; ++i) {
            FloorDefinition.RewardEntry table;
            HashSet<ResourceLocation> attempted = new HashSet<ResourceLocation>();
            for (int attempt = 0; attempt < 64 && (table = pool.table(level.f_46441_, attempted)) != null; ++attempt) {
                List<ItemStack> rolled;
                attempted.add(table.id());
                if (level.f_46441_.m_188500_() >= table.chance() || (rolled = level.m_7654_().m_278653_().m_278676_(table.id()).m_287195_(params).stream().filter(stack -> !stack.m_41619_()).toList()).isEmpty()) continue;
                items.addAll(rolled);
                break;
            }
            FloorRewards.rollBonuses(level, bonuses, normalPool, items);
        }
        if (tier == 3 && (rare = rarePool.singleBonus(rareBonuses, level.f_46441_, eligiblePlayers)) != null && (item = AbyssRegistries.ITEMS.getValue(rare.id())) != null && item != Items.f_41852_) {
            rareItems.add(items.size());
            items.add(RewardItems.stack(rare, 1));
        }
        return new Bundle(items, (int)Math.round((double)rewards.experience() * ChallengeRewards.scale(eligiblePlayers)), rareItems);
    }

    private static void rollBonuses(ServerLevel level, List<FloorDefinition.RewardEntry> bonuses, RewardPool pool, List<ItemStack> items) {
        for (FloorDefinition.RewardEntry requested : bonuses) {
            ItemStack stack;
            Item item;
            FloorDefinition.RewardEntry entry = pool.bonus(requested, level.f_46441_);
            if (entry == null || level.f_46441_.m_188500_() >= entry.chance() || (item = AbyssRegistries.ITEMS.getValue(entry.id())) == null || item == Items.f_41852_) continue;
            for (int count = entry.minCount() + level.f_46441_.m_188503_(entry.maxCount() - entry.minCount() + 1); count > 0; count -= stack.m_41613_()) {
                stack = RewardItems.stack(entry, 1);
                stack.m_41764_(Math.min(count, stack.m_41741_()));
                items.add(stack);
            }
        }
    }

    private static boolean availableItem(FloorDefinition.RewardEntry entry) {
        return AbyssRegistries.ITEMS.containsKey(entry.id()) && AbyssRegistries.ITEMS.getValue(entry.id()) != Items.f_41852_;
    }

    public static void releaseItem(ServerLevel level, Vec3 position, ItemStack stack) {
        FloorRewards.releaseItem(level, position, stack, false);
    }

    public static void releaseItem(ServerLevel level, Vec3 position, ItemStack stack, boolean rare) {
        FloorRewards.releaseItem(level, position, stack, rare, 0);
    }

    public static void releaseItem(ServerLevel level, Vec3 position, ItemStack stack, boolean rare, int floor) {
        if (stack.m_41619_()) {
            return;
        }
        ChestLootEntity item = new ChestLootEntity((Level)level, position.f_82479_, position.f_82480_ + 0.8, position.f_82481_, stack.m_41777_(), rare);
        RareRewardBroadcast.markDrop(item, floor);
        item.m_20334_((level.f_46441_.m_188500_() - 0.5) * 0.6, 0.45 + level.f_46441_.m_188500_() * 0.2, (level.f_46441_.m_188500_() - 0.5) * 0.6);
        item.m_32060_();
        level.m_7967_((Entity)item);
    }

    public static void releaseExperience(ServerLevel level, Vec3 position, int experience) {
        if (experience > 0) {
            ExperienceOrb.m_147082_((ServerLevel)level, (Vec3)position, (int)experience);
        }
    }

    public record Bundle(List<ItemStack> items, int experience, Set<Integer> rareItems) {
        public Bundle {
            items = items.stream().map(ItemStack::m_41777_).toList();
            int count = items.size();
            rareItems = rareItems.stream().filter(index -> index >= 0 && index < count).collect(Collectors.toUnmodifiableSet());
        }

        public Bundle(List<ItemStack> items, int experience) {
            this(items, experience, Set.of());
        }

        public boolean rare(int index) {
            return this.rareItems.contains(index);
        }

        public boolean hasRareItems() {
            return !this.rareItems.isEmpty();
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            ListTag stacks = new ListTag();
            this.items.forEach(stack -> stacks.add((Object)stack.m_41739_(new CompoundTag())));
            tag.m_128365_("Items", (Tag)stacks);
            tag.m_128405_("Experience", this.experience);
            tag.m_128385_("RareItems", this.rareItems.stream().sorted().mapToInt(Integer::intValue).toArray());
            return tag;
        }

        public static Bundle load(CompoundTag tag) {
            ArrayList<ItemStack> stacks = new ArrayList<ItemStack>();
            tag.m_128437_("Items", 10).forEach(value -> stacks.add(ItemStack.m_41712_((CompoundTag)((CompoundTag)value))));
            HashSet<Integer> rare = new HashSet<Integer>();
            for (int index : tag.m_128465_("RareItems")) {
                rare.add(index);
            }
            return new Bundle(stacks, tag.m_128451_("Experience"), rare);
        }
    }
}


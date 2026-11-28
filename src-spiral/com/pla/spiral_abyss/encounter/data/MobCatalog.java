/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParseException
 *  com.google.gson.JsonParser
 *  com.mojang.logging.LogUtils
 *  dev.architectury.platform.Platform
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.packs.resources.Resource
 *  net.minecraft.server.packs.resources.ResourceManager
 *  net.minecraft.server.packs.resources.SimplePreparableReloadListener
 *  net.minecraft.util.profiling.ProfilerFiller
 */
package com.pla.spiral_abyss.encounter.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.pla.spiral_abyss.boss_enviroment.BossEnvironmentCatalog;
import com.pla.spiral_abyss.config.AbyssConfig;
import com.pla.spiral_abyss.encounter.RewardPool;
import com.pla.spiral_abyss.encounter.data.BossOptions;
import com.pla.spiral_abyss.encounter.data.CombatRole;
import com.pla.spiral_abyss.encounter.data.FloorDefinition;
import com.pla.spiral_abyss.encounter.data.FloorParser;
import com.pla.spiral_abyss.encounter.data.MobCategory;
import com.pla.spiral_abyss.encounter.data.MobScaling;
import com.pla.spiral_abyss.platform.AbyssRegistries;
import dev.architectury.platform.Platform;
import java.io.BufferedReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class MobCatalog
extends SimplePreparableReloadListener<Definitions> {
    private static volatile Definitions current = new Definitions(Map.of(), new Equipment(Set.of(), Set.of(), Set.of(), Set.of(), Set.of()), Set.of(), Map.of());

    public static Equipment equipment() {
        return current.equipment();
    }

    public static BossOptions bossOptions(ResourceLocation id) {
        return current.bossOptions().getOrDefault(id, BossOptions.DEFAULT);
    }

    public static boolean isBoss(ResourceLocation id) {
        return id != null && current.pools().getOrDefault((Object)MobCategory.BOSS, List.of()).contains(id);
    }

    public static boolean isGhostMob(ResourceLocation id) {
        return current.ghostMobs().contains(id);
    }

    public static boolean suppressesSpawn(ResourceLocation id) {
        return id != null && current.suppressedSpawns().contains(id);
    }

    public static Map<ResourceLocation, Set<ResourceLocation>> suppressedFallingBlocks() {
        return current.suppressedFallingBlocks();
    }

    public static Map<ResourceLocation, List<FloorDefinition.RewardEntry>> rewards() {
        return current.rewards();
    }

    public static ScalingSnapshot scalingSnapshot() {
        Definitions definitions = current;
        return new ScalingSnapshot(definitions.scaling(), definitions.defaultScaling());
    }

    public static Pools snapshot() {
        return new Pools((Boolean)AbyssConfig.MIX_MOBS.get(), current.pools()).available(MobCatalog::selectableMob);
    }

    private static Map<ResourceLocation, List<FloorDefinition.RewardEntry>> immutableRewards(Map<ResourceLocation, List<FloorDefinition.RewardEntry>> rewards) {
        LinkedHashMap copy = new LinkedHashMap();
        rewards.forEach((id, entries) -> copy.put(id, List.copyOf(entries)));
        return Collections.unmodifiableMap(copy);
    }

    private static Map<ResourceLocation, Set<ResourceLocation>> immutableSets(Map<ResourceLocation, Set<ResourceLocation>> values) {
        LinkedHashMap copy = new LinkedHashMap();
        values.forEach((id, entries) -> copy.put(id, Set.copyOf(entries)));
        return Collections.unmodifiableMap(copy);
    }

    public static boolean selectableMob(ResourceLocation id) {
        return MobCatalog.availableMob(id) && !((List)AbyssConfig.MOB_BLACKLIST.get()).contains(id.toString());
    }

    public static boolean availableMob(ResourceLocation id) {
        return AbyssRegistries.ENTITY_TYPES.containsKey(id) && !id.toString().equals("minecraft:player") && !id.toString().equals("spiral_abyss:key_entity") && !id.toString().equals("spiral_abyss:copper_chest") && !id.toString().equals("spiral_abyss:iron_chest") && !id.toString().equals("spiral_abyss:golden_chest") && !id.toString().equals("spiral_abyss:experience_chest") && !id.toString().equals("spiral_abyss:reward_chest");
    }

    public static Pool parsePool(JsonArray array) {
        return MobCatalog.parsePool(array, AbyssRegistries.ITEMS::containsKey);
    }

    public static Pool parsePool(JsonArray array, Predicate<ResourceLocation> isItem) {
        return MobCatalog.parsePool(array, isItem, false);
    }

    public static Pool parseBossPool(JsonArray array, Predicate<ResourceLocation> isItem) {
        return MobCatalog.parsePool(array, isItem, true);
    }

    private static Pool parsePool(JsonArray array, Predicate<ResourceLocation> isItem, boolean bossPool) {
        LinkedHashSet<ResourceLocation> mobs = new LinkedHashSet<ResourceLocation>();
        HashSet<ResourceLocation> configuredEnvironments = new HashSet<ResourceLocation>();
        LinkedHashSet<ResourceLocation> suppressedSpawns = new LinkedHashSet<ResourceLocation>();
        LinkedHashMap<ResourceLocation, Set<ResourceLocation>> suppressedFallingBlocks = new LinkedHashMap<ResourceLocation, Set<ResourceLocation>>();
        LinkedHashMap<ResourceLocation, List<FloorDefinition.RewardEntry>> rewards = new LinkedHashMap<ResourceLocation, List<FloorDefinition.RewardEntry>>();
        MobScaling.Plan defaultScaling = MobScaling.Plan.NORMAL_PLAN;
        boolean hasDefaultScaling = false;
        for (JsonElement value : array) {
            if (!value.isJsonObject()) continue;
            JsonObject object = value.getAsJsonObject();
            if (object.size() != 1 || !object.has("scaling") || hasDefaultScaling) {
                throw new JsonParseException("pool-level object must be one scaling definition");
            }
            defaultScaling = MobScaling.parse(object.get("scaling"), MobScaling.Plan.NORMAL_PLAN);
            hasDefaultScaling = true;
        }
        LinkedHashMap<ResourceLocation, MobScaling.Plan> scaling = new LinkedHashMap<ResourceLocation, MobScaling.Plan>();
        LinkedHashMap<ResourceLocation, BossOptions> bossOptions = new LinkedHashMap<ResourceLocation, BossOptions>();
        for (JsonElement value : array) {
            JsonArray spec;
            if (value.isJsonObject()) continue;
            JsonArray jsonArray = spec = value.isJsonArray() ? value.getAsJsonArray() : null;
            if (spec != null && (spec.size() == 0 || spec.size() > 65)) {
                throw new JsonParseException("A mob array needs an entity ID and at most 64 rewards");
            }
            ResourceLocation mob = FloorParser.id(spec == null ? value : spec.get(0));
            mobs.add(mob);
            MobScaling.Plan mobScale = defaultScaling;
            if (spec == null) continue;
            List bonuses = rewards.computeIfAbsent(mob, id -> new ArrayList());
            for (int index = 1; index < spec.size(); ++index) {
                JsonElement reward = spec.get(index);
                if (reward.isJsonPrimitive()) {
                    boolean item;
                    ResourceLocation id2;
                    FloorDefinition.RewardEntry entry = new FloorDefinition.RewardEntry(id2, item, 1, 1, 1, (item = isItem.test(id2 = FloorParser.id(reward))) ? 0.05 : 0.25);
                    bonuses.add(item ? RewardPool.rare(entry) : entry);
                    continue;
                }
                JsonObject object = reward.getAsJsonObject();
                if (BossOptions.isMetadata(object)) {
                    if (!bossPool && !object.keySet().equals(Set.of("suppress_death_blocks"))) {
                        throw new JsonParseException("only suppress_death_blocks is supported outside mob_pools/boss.json");
                    }
                    if (bossOptions.putIfAbsent(mob, BossOptions.parse(object)) == null) continue;
                    throw new JsonParseException("duplicate boss options for " + String.valueOf(mob));
                }
                if (object.has("scaling")) {
                    if (object.size() != 1 || scaling.containsKey(mob)) {
                        throw new JsonParseException("mob scaling must be a separate, unique metadata object for " + String.valueOf(mob));
                    }
                    mobScale = MobScaling.parse(object.get("scaling"), defaultScaling);
                    scaling.put(mob, mobScale);
                    continue;
                }
                if (object.has("suppress_spawned_entities")) {
                    if (object.size() != 1) {
                        throw new JsonParseException("suppress_spawned_entities must be separate from reward/environment metadata");
                    }
                    JsonElement targets = object.get("suppress_spawned_entities");
                    if (!targets.isJsonArray() || targets.getAsJsonArray().isEmpty() || targets.getAsJsonArray().size() > 64) {
                        throw new JsonParseException("suppress_spawned_entities needs 1-64 entity IDs");
                    }
                    for (JsonElement target : targets.getAsJsonArray()) {
                        suppressedSpawns.add(FloorParser.id(target));
                    }
                    continue;
                }
                if (object.has("suppress_falling_blocks")) {
                    if (object.size() != 1) {
                        throw new JsonParseException("suppress_falling_blocks must be separate from reward/environment metadata");
                    }
                    JsonElement targets = object.get("suppress_falling_blocks");
                    if (!targets.isJsonArray() || targets.getAsJsonArray().isEmpty() || targets.getAsJsonArray().size() > 64) {
                        throw new JsonParseException("suppress_falling_blocks needs 1-64 block IDs");
                    }
                    Set blocks = suppressedFallingBlocks.computeIfAbsent(mob, unused -> new LinkedHashSet());
                    for (JsonElement target : targets.getAsJsonArray()) {
                        blocks.add(FloorParser.id(target));
                    }
                    continue;
                }
                if (BossEnvironmentCatalog.isMetadata(object)) {
                    if (!bossPool) {
                        throw new JsonParseException("environment metadata is only supported in mob_pools/boss.json");
                    }
                    if (!configuredEnvironments.add(mob)) {
                        throw new JsonParseException("duplicate environment for " + String.valueOf(mob));
                    }
                    BossEnvironmentCatalog.validateMetadataFields(object);
                    continue;
                }
                FloorDefinition.RewardEntry entry = FloorParser.rewardEntry(object, object.has("item") ? 0.05 : 0.25);
                bonuses.add(entry.item() ? RewardPool.rare(entry) : entry);
            }
            scaling.putIfAbsent(mob, mobScale);
        }
        for (ResourceLocation mob : mobs) {
            scaling.putIfAbsent(mob, defaultScaling);
        }
        return new Pool(List.copyOf(mobs), rewards, suppressedSpawns, suppressedFallingBlocks, scaling, defaultScaling, bossOptions);
    }

    private static JsonArray read(ResourceManager manager, String path) {
        JsonArray jsonArray;
        block9: {
            ResourceLocation id = new ResourceLocation("spiral_abyss", path + ".json");
            Optional resource = manager.m_213713_(id);
            if (resource.isEmpty()) {
                return new JsonArray();
            }
            BufferedReader reader = ((Resource)resource.get()).m_215508_();
            try {
                jsonArray = JsonParser.parseReader((Reader)reader).getAsJsonArray();
                if (reader == null) break block9;
            }
            catch (Throwable throwable) {
                try {
                    if (reader != null) {
                        try {
                            ((Reader)reader).close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                catch (Exception ex) {
                    throw new JsonParseException("Invalid " + String.valueOf(id), (Throwable)ex);
                }
            }
            ((Reader)reader).close();
        }
        return jsonArray;
    }

    private static Set<ResourceLocation> mobType(ResourceManager manager, String key) {
        LinkedHashSet<ResourceLocation> ids = new LinkedHashSet<ResourceLocation>();
        for (JsonElement value : MobCatalog.read(manager, "mob_types/" + key)) {
            ResourceLocation id = FloorParser.id(value);
            if (MobCatalog.availableMob(id)) {
                ids.add(id);
                continue;
            }
            MobCatalog.warnUnavailableMob(id, "data/spiral_abyss/mob_types/" + key + ".json");
        }
        return ids;
    }

    static void warnUnavailableMob(ResourceLocation id, String source) {
        if (id.m_135827_().equals("minecraft") || Platform.isModLoaded((String)id.m_135827_())) {
            LogUtils.getLogger().warn("[Spiral Abyss] Ignoring unavailable or forbidden mob {} in {}; check the configured entity ID.", (Object)id, (Object)source);
        }
    }

    protected Definitions prepare(ResourceManager manager, ProfilerFiller profiler) {
        EnumMap<MobCategory, List<ResourceLocation>> pools = new EnumMap<MobCategory, List<ResourceLocation>>(MobCategory.class);
        LinkedHashMap<ResourceLocation, List<FloorDefinition.RewardEntry>> rewards = new LinkedHashMap<ResourceLocation, List<FloorDefinition.RewardEntry>>();
        LinkedHashSet<ResourceLocation> suppressedSpawns = new LinkedHashSet<ResourceLocation>();
        LinkedHashMap<ResourceLocation, Set<ResourceLocation>> suppressedFallingBlocks = new LinkedHashMap<ResourceLocation, Set<ResourceLocation>>();
        EnumMap<MobCategory, Map<ResourceLocation, MobScaling.Plan>> scaling = new EnumMap<MobCategory, Map<ResourceLocation, MobScaling.Plan>>(MobCategory.class);
        EnumMap<MobCategory, MobScaling.Plan> defaultScaling = new EnumMap<MobCategory, MobScaling.Plan>(MobCategory.class);
        LinkedHashMap<ResourceLocation, BossOptions> bossOptions = new LinkedHashMap<ResourceLocation, BossOptions>();
        for (MobCategory category : MobCategory.values()) {
            JsonArray poolEntries = MobCatalog.read(manager, "mob_pools/" + category.key());
            Pool pool = category == MobCategory.BOSS ? MobCatalog.parseBossPool(poolEntries, AbyssRegistries.ITEMS::containsKey) : MobCatalog.parsePool(poolEntries, AbyssRegistries.ITEMS::containsKey);
            pools.put(category, pool.mobs());
            scaling.put(category, pool.scaling());
            defaultScaling.put(category, pool.defaultScaling());
            bossOptions.putAll(pool.bossOptions());
            suppressedSpawns.addAll(pool.suppressedSpawns());
            pool.suppressedFallingBlocks().forEach((id, blocks) -> suppressedFallingBlocks.computeIfAbsent((ResourceLocation)id, unused -> new LinkedHashSet()).addAll(blocks));
            pool.rewards().forEach((id, entries) -> rewards.computeIfAbsent((ResourceLocation)id, unused -> new ArrayList()).addAll(entries));
        }
        return new Definitions(pools, new Equipment(MobCatalog.mobType(manager, "armor_mobs"), MobCatalog.mobType(manager, "melee_mobs"), MobCatalog.mobType(manager, "bow_users"), MobCatalog.mobType(manager, "crossbow_users"), MobCatalog.mobType(manager, "trident_users"), MobCatalog.mobType(manager, "ranged_mobs")), MobCatalog.mobType(manager, "ghost_mobs"), rewards, suppressedSpawns, suppressedFallingBlocks, scaling, defaultScaling, bossOptions);
    }

    protected void apply(Definitions definitions, ResourceManager manager, ProfilerFiller profiler) {
        current = definitions;
        definitions.pools().forEach((category, ids) -> ids.stream().filter(id -> !MobCatalog.availableMob(id)).forEach(id -> MobCatalog.warnUnavailableMob(id, "data/spiral_abyss/mob_pools/" + category.key() + ".json")));
    }

    public record Definitions(Map<MobCategory, List<ResourceLocation>> pools, Equipment equipment, Set<ResourceLocation> ghostMobs, Map<ResourceLocation, List<FloorDefinition.RewardEntry>> rewards, Set<ResourceLocation> suppressedSpawns, Map<ResourceLocation, Set<ResourceLocation>> suppressedFallingBlocks, Map<MobCategory, Map<ResourceLocation, MobScaling.Plan>> scaling, Map<MobCategory, MobScaling.Plan> defaultScaling, Map<ResourceLocation, BossOptions> bossOptions) {
        public Definitions {
            pools = new Pools(false, pools).entries();
            ghostMobs = Set.copyOf(ghostMobs);
            rewards = MobCatalog.immutableRewards(rewards);
            suppressedSpawns = Set.copyOf(suppressedSpawns);
            suppressedFallingBlocks = MobCatalog.immutableSets(suppressedFallingBlocks);
            EnumMap copy = new EnumMap(MobCategory.class);
            scaling.forEach((category, plans) -> copy.put((MobCategory)((Object)category), Map.copyOf(plans)));
            scaling = Collections.unmodifiableMap(copy);
            defaultScaling = Map.copyOf(defaultScaling);
            bossOptions = Map.copyOf(bossOptions);
        }

        public Definitions(Map<MobCategory, List<ResourceLocation>> pools, Equipment equipment, Set<ResourceLocation> ghostMobs, Map<ResourceLocation, List<FloorDefinition.RewardEntry>> rewards, Set<ResourceLocation> suppressedSpawns, Map<ResourceLocation, Set<ResourceLocation>> suppressedFallingBlocks, Map<MobCategory, Map<ResourceLocation, MobScaling.Plan>> scaling, Map<MobCategory, MobScaling.Plan> defaultScaling) {
            this(pools, equipment, ghostMobs, rewards, suppressedSpawns, suppressedFallingBlocks, scaling, defaultScaling, Map.of());
        }

        public Definitions(Map<MobCategory, List<ResourceLocation>> pools, Equipment equipment, Set<ResourceLocation> ghostMobs, Map<ResourceLocation, List<FloorDefinition.RewardEntry>> rewards) {
            this(pools, equipment, ghostMobs, rewards, Set.of(), Map.of(), Map.of(), Map.of());
        }

        public Definitions(Map<MobCategory, List<ResourceLocation>> pools, Equipment equipment, Set<ResourceLocation> ghostMobs, Map<ResourceLocation, List<FloorDefinition.RewardEntry>> rewards, Set<ResourceLocation> suppressedSpawns) {
            this(pools, equipment, ghostMobs, rewards, suppressedSpawns, Map.of(), Map.of(), Map.of());
        }
    }

    public record Equipment(Set<ResourceLocation> armor, Set<ResourceLocation> melee, Set<ResourceLocation> bows, Set<ResourceLocation> crossbows, Set<ResourceLocation> tridents, Set<ResourceLocation> ranged) {
        public Equipment {
            armor = Set.copyOf(armor);
            melee = Set.copyOf(melee);
            bows = Set.copyOf(bows);
            crossbows = Set.copyOf(crossbows);
            tridents = Set.copyOf(tridents);
            ranged = Set.copyOf(ranged);
        }

        public Equipment(Set<ResourceLocation> armor, Set<ResourceLocation> melee, Set<ResourceLocation> bows, Set<ResourceLocation> crossbows, Set<ResourceLocation> tridents) {
            this(armor, melee, bows, crossbows, tridents, Set.of());
        }

        public CombatRole role(ResourceLocation id) {
            return this.bows.contains(id) || this.crossbows.contains(id) || this.ranged.contains(id) ? CombatRole.RANGED : CombatRole.MELEE;
        }
    }

    public record ScalingSnapshot(Map<MobCategory, Map<ResourceLocation, MobScaling.Plan>> plans, Map<MobCategory, MobScaling.Plan> defaults) {
        public static final ScalingSnapshot EMPTY = new ScalingSnapshot(Map.of(), Map.of());

        public MobScaling.Floor at(MobCategory category, ResourceLocation mob, int floor) {
            if (category == null) {
                return MobScaling.Plan.NORMAL;
            }
            MobScaling.Plan fallback = this.defaults.getOrDefault((Object)category, MobScaling.Plan.NORMAL_PLAN);
            return this.plans.getOrDefault((Object)category, Map.of()).getOrDefault(mob, fallback).at(floor);
        }
    }

    public record Pools(boolean mixMobs, Map<MobCategory, List<ResourceLocation>> entries) {
        public Pools {
            EnumMap copy = new EnumMap(MobCategory.class);
            entries.forEach((category, ids) -> copy.put((MobCategory)((Object)category), List.copyOf(ids)));
            entries = Collections.unmodifiableMap(copy);
        }

        public Pools available(Predicate<ResourceLocation> installed) {
            EnumMap<MobCategory, List<ResourceLocation>> valid = new EnumMap<MobCategory, List<ResourceLocation>>(MobCategory.class);
            this.entries.forEach((category, ids) -> valid.put((MobCategory)((Object)category), ids.stream().filter(installed).toList()));
            return new Pools(this.mixMobs, valid);
        }
    }

    public record Pool(List<ResourceLocation> mobs, Map<ResourceLocation, List<FloorDefinition.RewardEntry>> rewards, Set<ResourceLocation> suppressedSpawns, Map<ResourceLocation, Set<ResourceLocation>> suppressedFallingBlocks, Map<ResourceLocation, MobScaling.Plan> scaling, MobScaling.Plan defaultScaling, Map<ResourceLocation, BossOptions> bossOptions) {
        public Pool {
            mobs = List.copyOf(mobs);
            rewards = MobCatalog.immutableRewards(rewards);
            suppressedSpawns = Set.copyOf(suppressedSpawns);
            suppressedFallingBlocks = MobCatalog.immutableSets(suppressedFallingBlocks);
            scaling = Map.copyOf(scaling);
            bossOptions = Map.copyOf(bossOptions);
        }

        public Pool(List<ResourceLocation> mobs, Map<ResourceLocation, List<FloorDefinition.RewardEntry>> rewards, Set<ResourceLocation> suppressedSpawns, Map<ResourceLocation, Set<ResourceLocation>> suppressedFallingBlocks, Map<ResourceLocation, MobScaling.Plan> scaling, MobScaling.Plan defaultScaling) {
            this(mobs, rewards, suppressedSpawns, suppressedFallingBlocks, scaling, defaultScaling, Map.of());
        }

        public Pool(List<ResourceLocation> mobs, Map<ResourceLocation, List<FloorDefinition.RewardEntry>> rewards) {
            this(mobs, rewards, Set.of(), Map.of(), Map.of(), MobScaling.Plan.NORMAL_PLAN);
        }

        public Pool(List<ResourceLocation> mobs, Map<ResourceLocation, List<FloorDefinition.RewardEntry>> rewards, Set<ResourceLocation> suppressedSpawns) {
            this(mobs, rewards, suppressedSpawns, Map.of(), Map.of(), MobScaling.Plan.NORMAL_PLAN);
        }
    }
}


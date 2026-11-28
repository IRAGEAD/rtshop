/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParseException
 *  com.google.gson.JsonParser
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.packs.resources.Resource
 *  net.minecraft.server.packs.resources.ResourceManager
 *  net.minecraft.server.packs.resources.SimplePreparableReloadListener
 *  net.minecraft.util.profiling.ProfilerFiller
 *  net.minecraft.world.entity.EquipmentSlot
 *  net.minecraft.world.entity.EquipmentSlot$Type
 *  net.minecraft.world.item.ArmorItem
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.item.ProjectileWeaponItem
 *  net.minecraft.world.item.TieredItem
 *  net.minecraft.world.item.TridentItem
 *  net.minecraft.world.item.enchantment.Enchantment
 *  net.minecraft.world.level.ItemLike
 */
package com.pla.spiral_abyss.encounter.challenge;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.pla.spiral_abyss.encounter.challenge.Challenge;
import com.pla.spiral_abyss.platform.AbyssRegistries;
import java.io.BufferedReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.ItemLike;

public final class ChallengeCatalog
extends SimplePreparableReloadListener<Snapshot> {
    private static volatile Snapshot current = new Snapshot(Map.of());

    public static Snapshot snapshot() {
        return current;
    }

    public static boolean countsKills(Challenge.Type type) {
        return type != Challenge.Type.TIME && type != Challenge.Type.NO_ARMOR && type != Challenge.Type.ARMOR_ENCHANTMENT && type != Challenge.Type.TAKE_DAMAGE && type != Challenge.Type.FISHING && type != Challenge.Type.TOTEM && type != Challenge.Type.TRICKSHOT && type != Challenge.Type.ENDER_PEARL_HIT && type != Challenge.Type.SHIELD_BLOCK;
    }

    public static boolean installed(Challenge.Type type, ResourceLocation id) {
        return switch (type) {
            case Challenge.Type.ARMOR_ENCHANTMENT, Challenge.Type.WEAPON_ENCHANTMENT -> AbyssRegistries.ENCHANTMENTS.containsKey(id);
            case Challenge.Type.WEAPON -> {
                if (AbyssRegistries.ITEMS.containsKey(id) && AbyssRegistries.ITEMS.getValue(id) != Items.f_41852_) {
                    yield true;
                }
                yield false;
            }
            case Challenge.Type.PROJECTILE -> AbyssRegistries.ENTITY_TYPES.containsKey(id);
            case Challenge.Type.PLAYER_EFFECT, Challenge.Type.MOB_EFFECT -> AbyssRegistries.MOB_EFFECTS.containsKey(id);
            default -> true;
        };
    }

    public static boolean available(Challenge challenge) {
        return challenge.ids().stream().allMatch(id -> ChallengeCatalog.installed(challenge.type(), id));
    }

    private static Challenge create(Challenge.Type type, Challenge.Difficulty difficulty, Tier tier, Random random, List<ResourceLocation> retained) {
        List pool = tier.pool().stream().filter(id -> ChallengeCatalog.installed(type, id)).distinct().collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(pool, random);
        if (type == Challenge.Type.PROJECTILE) {
            return new Challenge(type, difficulty, tier.target(), tier.percentage(), pool, List.of());
        }
        ArrayList<ResourceLocation> ids = new ArrayList<ResourceLocation>(retained);
        for (ResourceLocation id2 : pool) {
            if (ids.size() >= tier.choose()) break;
            if (ids.contains(id2)) continue;
            ids.add(id2);
            if (type != Challenge.Type.WEAPON_ENCHANTMENT || ChallengeCatalog.weaponEnchantmentsFit(ids, null)) continue;
            ids.remove(ids.size() - 1);
        }
        if (ids.size() != tier.choose()) {
            return null;
        }
        List<EquipmentSlot> slots = tier.slotSets().isEmpty() ? List.of() : tier.slotSets().get(random.nextInt(tier.slotSets().size()));
        return new Challenge(type, difficulty, tier.target(), tier.percentage(), ids, slots);
    }

    public static boolean compatible(List<Challenge> challenges) {
        EnumSet<EquipmentSlot> forbidden = EnumSet.noneOf(EquipmentSlot.class);
        challenges.stream().filter(c -> c.type() == Challenge.Type.NO_ARMOR).forEach(c -> forbidden.addAll(c.forbiddenSlots()));
        if (forbidden.size() >= 4) {
            return false;
        }
        List<ResourceLocation> armor = challenges.stream().filter(c -> c.type() == Challenge.Type.ARMOR_ENCHANTMENT).flatMap(c -> c.ids().stream()).distinct().toList();
        if (!armor.isEmpty() && !ChallengeCatalog.armorEnchantmentsFit(armor, forbidden)) {
            return false;
        }
        if (challenges.stream().anyMatch(c -> c.type() == Challenge.Type.PLAYER_BURNING) && challenges.stream().anyMatch(c -> c.type() == Challenge.Type.PLAYER_EFFECT && c.ids().stream().anyMatch(id -> id.toString().equals("minecraft:fire_resistance")))) {
            return false;
        }
        List<ResourceLocation> enchantments = challenges.stream().filter(c -> c.type() == Challenge.Type.WEAPON_ENCHANTMENT).flatMap(c -> c.ids().stream()).distinct().toList();
        if (enchantments.isEmpty()) {
            return true;
        }
        List<ResourceLocation> weapons = challenges.stream().filter(c -> c.type() == Challenge.Type.WEAPON).flatMap(c -> c.ids().stream()).filter(id -> ChallengeCatalog.installed(Challenge.Type.WEAPON, id)).toList();
        if (weapons.isEmpty()) {
            return ChallengeCatalog.weaponEnchantmentsFit(enchantments, null);
        }
        return weapons.stream().allMatch(weapon -> ChallengeCatalog.weaponEnchantmentsFit(enchantments, weapon));
    }

    public static boolean feasible(List<Challenge> challenges, int minimumEnemies) {
        if (!ChallengeCatalog.compatible(challenges) || challenges.stream().anyMatch(c -> ChallengeCatalog.countsKills(c.type()) && c.requiredCount(minimumEnemies) > minimumEnemies)) {
            return false;
        }
        HashMap weaponTargets = new HashMap();
        challenges.stream().filter(c -> c.type() == Challenge.Type.WEAPON).forEach(c -> weaponTargets.merge(c.ids(), c.requiredCount(minimumEnemies), Math::max));
        long weaponKills = weaponTargets.values().stream().mapToLong(Integer::longValue).sum();
        int enchantedKills = challenges.stream().filter(c -> c.type() == Challenge.Type.WEAPON_ENCHANTMENT).mapToInt(c -> c.requiredCount(minimumEnemies)).max().orElse(0);
        long separateKills = Math.max(weaponKills, (long)enchantedKills);
        for (Challenge.Type type : List.of(Challenge.Type.PROJECTILE, Challenge.Type.EXPLOSION)) {
            separateKills += (long)challenges.stream().filter(c -> c.type() == type).mapToInt(c -> c.requiredCount(minimumEnemies)).max().orElse(0);
        }
        return separateKills <= (long)minimumEnemies;
    }

    private static boolean armorEnchantmentsFit(List<ResourceLocation> ids, Set<EquipmentSlot> forbidden) {
        List<EquipmentSlot> slots = Arrays.stream(EquipmentSlot.values()).filter(slot -> slot.m_20743_() == EquipmentSlot.Type.ARMOR && !forbidden.contains(slot)).toList();
        EnumMap<EquipmentSlot, List<ResourceLocation>> groups = new EnumMap<EquipmentSlot, List<ResourceLocation>>(EquipmentSlot.class);
        slots.forEach(slot -> groups.put((EquipmentSlot)slot, new ArrayList()));
        return ChallengeCatalog.distributeArmor(ids, 0, groups);
    }

    private static boolean distributeArmor(List<ResourceLocation> ids, int index, Map<EquipmentSlot, List<ResourceLocation>> groups) {
        if (index == ids.size()) {
            return true;
        }
        for (Map.Entry<EquipmentSlot, List<ResourceLocation>> entry : groups.entrySet()) {
            boolean fits;
            entry.getValue().add(ids.get(index));
            List<Enchantment> enchantments = entry.getValue().stream().map(AbyssRegistries.ENCHANTMENTS::getValue).filter(Objects::nonNull).toList();
            boolean compatible = true;
            for (int i = 0; i < enchantments.size(); ++i) {
                for (int j = i + 1; j < enchantments.size(); ++j) {
                    if (enchantments.get(i).m_44695_(enchantments.get(j))) continue;
                    compatible = false;
                }
            }
            boolean bl = fits = compatible && AbyssRegistries.ITEMS.getValues().stream().filter(item -> {
                ArmorItem armor;
                return item instanceof ArmorItem && (armor = (ArmorItem)item).m_40402_() == entry.getKey();
            }).anyMatch(item -> enchantments.stream().allMatch(enchantment -> enchantment.m_6081_(new ItemStack((ItemLike)item))));
            if (fits && ChallengeCatalog.distributeArmor(ids, index + 1, groups)) {
                return true;
            }
            entry.getValue().remove(entry.getValue().size() - 1);
        }
        return false;
    }

    private static boolean weaponEnchantmentsFit(List<ResourceLocation> ids, ResourceLocation weapon) {
        List<Enchantment> enchantments = ids.stream().map(AbyssRegistries.ENCHANTMENTS::getValue).filter(Objects::nonNull).toList();
        for (int i = 0; i < enchantments.size(); ++i) {
            for (int j = i + 1; j < enchantments.size(); ++j) {
                if (enchantments.get(i).m_44695_(enchantments.get(j))) continue;
                return false;
            }
        }
        if (weapon != null) {
            Item item2 = AbyssRegistries.ITEMS.getValue(weapon);
            return item2 != null && enchantments.stream().allMatch(enchantment -> enchantment.m_6081_(new ItemStack((ItemLike)item2)));
        }
        return AbyssRegistries.ITEMS.getValues().stream().filter(item -> item instanceof TieredItem || item instanceof ProjectileWeaponItem || item instanceof TridentItem).anyMatch(item -> enchantments.stream().allMatch(enchantment -> enchantment.m_6081_(new ItemStack((ItemLike)item))));
    }

    public static Map<Challenge.Difficulty, Tier> parse(Challenge.Type type, JsonElement json) {
        if (json == null || json.isJsonNull() || json.isJsonArray() && json.getAsJsonArray().size() == 0) {
            return Map.of();
        }
        if (!json.isJsonObject()) {
            throw new JsonParseException("Challenge file must be an object or empty array");
        }
        JsonObject root = json.getAsJsonObject();
        EnumMap<Challenge.Difficulty, Tier> tiers = new EnumMap<Challenge.Difficulty, Tier>(Challenge.Difficulty.class);
        for (Challenge.Difficulty difficulty : Challenge.Difficulty.values()) {
            int choose;
            boolean requiresPool;
            double percentage;
            int target;
            JsonObject object;
            if (!root.has(difficulty.key()) || (object = root.getAsJsonObject(difficulty.key())).size() == 0) continue;
            int n = Challenge.counted(type) ? 1 : (target = object.has("target") ? object.get("target").getAsInt() : 1);
            if (target < 1 || target > 10000 || type == Challenge.Type.TIME && target > 300) {
                throw new JsonParseException("Invalid challenge target");
            }
            double d = percentage = Challenge.counted(type) ? object.get("percentage").getAsDouble() : 0.0;
            if (!Double.isFinite(percentage) || percentage < 0.0 || percentage > 100.0 || Challenge.counted(type) && percentage == 0.0) {
                throw new JsonParseException("Invalid challenge percentage");
            }
            ArrayList<ResourceLocation> pool = new ArrayList<ResourceLocation>();
            if (object.has("pool")) {
                for (JsonElement value : object.getAsJsonArray("pool")) {
                    ResourceLocation id = ResourceLocation.m_135820_((String)value.getAsString());
                    if (id == null) {
                        throw new JsonParseException("Invalid challenge registry ID: " + String.valueOf(value));
                    }
                    pool.add(id);
                }
            }
            boolean bl = requiresPool = type == Challenge.Type.ARMOR_ENCHANTMENT || type == Challenge.Type.WEAPON_ENCHANTMENT || type == Challenge.Type.WEAPON || type == Challenge.Type.PLAYER_EFFECT || type == Challenge.Type.MOB_EFFECT;
            if (requiresPool && object.has("pool") && pool.isEmpty()) continue;
            if (object.has("inherits")) {
                Challenge.Difficulty inherited;
                try {
                    inherited = Challenge.Difficulty.valueOf(object.get("inherits").getAsString().toUpperCase(Locale.ROOT));
                }
                catch (IllegalArgumentException exception) {
                    throw new JsonParseException("Unknown inherited challenge tier", (Throwable)exception);
                }
                if (inherited.ordinal() >= difficulty.ordinal()) {
                    throw new JsonParseException("A challenge tier may inherit only an earlier difficulty");
                }
                ArrayList<ResourceLocation> combined = new ArrayList<ResourceLocation>(tiers.containsKey((Object)inherited) ? ((Tier)tiers.get((Object)inherited)).pool() : List.of());
                combined.addAll(pool);
                pool = new ArrayList(new LinkedHashSet(combined));
            }
            if (pool.size() > 256) {
                throw new JsonParseException("Challenge pools allow at most 256 IDs");
            }
            int defaultChoose = switch (type) {
                case Challenge.Type.ARMOR_ENCHANTMENT, Challenge.Type.WEAPON_ENCHANTMENT -> 1;
                case Challenge.Type.WEAPON, Challenge.Type.PLAYER_EFFECT, Challenge.Type.MOB_EFFECT -> 1;
                case Challenge.Type.PROJECTILE -> pool.size();
                default -> 0;
            };
            int n2 = choose = object.has("choose") ? object.get("choose").getAsInt() : defaultChoose;
            if ((type == Challenge.Type.ARMOR_ENCHANTMENT || type == Challenge.Type.WEAPON_ENCHANTMENT || type == Challenge.Type.PLAYER_EFFECT || type == Challenge.Type.MOB_EFFECT) && choose != 1) {
                throw new JsonParseException("Enchantment and effect challenges choose exactly one ID");
            }
            if (choose < 0 || choose > (type == Challenge.Type.PROJECTILE ? 256 : 32) || defaultChoose > 0 && choose == 0) {
                throw new JsonParseException("Invalid challenge pool selection size");
            }
            ArrayList<List<EquipmentSlot>> slotSets = new ArrayList<List<EquipmentSlot>>();
            if (object.has("slot_sets")) {
                for (JsonElement entry : object.getAsJsonArray("slot_sets")) {
                    ArrayList<EquipmentSlot> slots = new ArrayList<EquipmentSlot>();
                    for (JsonElement value : entry.getAsJsonArray()) {
                        EquipmentSlot slot = EquipmentSlot.m_20747_((String)value.getAsString());
                        if (slot.m_20743_() != EquipmentSlot.Type.ARMOR) {
                            throw new JsonParseException("Only armor slots are allowed");
                        }
                        if (slots.contains(slot)) continue;
                        slots.add(slot);
                    }
                    if (slots.isEmpty() || slots.size() == 4) {
                        throw new JsonParseException("Forbidden armor slots must leave at least one armor slot usable");
                    }
                    slotSets.add(slots);
                }
            }
            if (type == Challenge.Type.NO_ARMOR && slotSets.isEmpty()) {
                throw new JsonParseException("no_armor requires slot_sets");
            }
            tiers.put(difficulty, new Tier(target, percentage, pool, choose, slotSets));
        }
        return Map.copyOf(tiers);
    }

    protected Snapshot prepare(ResourceManager manager, ProfilerFiller profiler) {
        EnumMap<Challenge.Type, Map<Challenge.Difficulty, Tier>> definitions = new EnumMap<Challenge.Type, Map<Challenge.Difficulty, Tier>>(Challenge.Type.class);
        for (Challenge.Type type : Challenge.Type.values()) {
            ResourceLocation id = new ResourceLocation("spiral_abyss", "challenge/" + type.key() + ".json");
            Optional resource = manager.m_213713_(id);
            if (resource.isEmpty()) continue;
            try (BufferedReader reader = ((Resource)resource.get()).m_215508_();){
                JsonElement json = JsonParser.parseReader((Reader)reader);
                definitions.put(type, ChallengeCatalog.parse(type, json));
            }
            catch (Exception exception) {
                throw new JsonParseException("Invalid Abyss challenge " + String.valueOf(id), (Throwable)exception);
            }
        }
        return new Snapshot(definitions);
    }

    protected void apply(Snapshot snapshot, ResourceManager manager, ProfilerFiller profiler) {
        current = snapshot;
    }

    public record Snapshot(Map<Challenge.Type, Map<Challenge.Difficulty, Tier>> types) {
        public Snapshot {
            EnumMap copy = new EnumMap(Challenge.Type.class);
            types.forEach((type, tiers) -> copy.put((Challenge.Type)((Object)type), Map.copyOf(tiers)));
            types = Map.copyOf(copy);
        }

        public Tier tier(Challenge.Type type, Challenge.Difficulty difficulty) {
            return (Tier)this.types.getOrDefault((Object)type, Map.of()).get((Object)difficulty);
        }

        public List<Challenge> candidates(Challenge.Type type, Challenge.Difficulty difficulty, long seed, Challenge existing) {
            Tier tier;
            LinkedHashSet<Challenge> choices = new LinkedHashSet<Challenge>();
            if (existing != null && existing.type() == type && existing.difficulty() == difficulty && ChallengeCatalog.available(existing)) {
                choices.add(existing);
            }
            if ((tier = this.tier(type, difficulty)) == null) {
                return List.copyOf(choices);
            }
            Random random = new Random(seed);
            if (existing != null && existing.type() == type) {
                List<ResourceLocation> retained = existing.ids().stream().filter(id -> ChallengeCatalog.installed(type, id)).toList();
                Tier repair = new Tier(existing.target(), existing.percentage(), tier.pool(), tier.choose(), List.of(existing.forbiddenSlots()));
                for (int i = 0; i < 32; ++i) {
                    Challenge candidate = ChallengeCatalog.create(type, difficulty, repair, random, retained);
                    if (candidate == null) continue;
                    choices.add(candidate);
                }
            }
            for (int i = 0; i < 64; ++i) {
                Challenge candidate = ChallengeCatalog.create(type, difficulty, tier, random, List.of());
                if (candidate == null) continue;
                choices.add(candidate);
            }
            return List.copyOf(choices);
        }
    }

    public record Tier(int target, double percentage, List<ResourceLocation> pool, int choose, List<List<EquipmentSlot>> slotSets) {
        public Tier {
            pool = List.copyOf(pool);
            slotSets = slotSets.stream().map(List::copyOf).toList();
        }
    }
}


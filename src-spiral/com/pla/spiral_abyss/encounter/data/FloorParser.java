/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParseException
 *  com.google.gson.JsonPrimitive
 *  dev.architectury.platform.Platform
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.EntityType
 */
package com.pla.spiral_abyss.encounter.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.pla.spiral_abyss.encounter.BossWavePolicy;
import com.pla.spiral_abyss.encounter.challenge.Challenge;
import com.pla.spiral_abyss.encounter.data.CombatRole;
import com.pla.spiral_abyss.encounter.data.FloorDefinition;
import com.pla.spiral_abyss.encounter.data.MobCatalog;
import com.pla.spiral_abyss.encounter.data.MobCategory;
import com.pla.spiral_abyss.platform.AbyssRegistries;
import dev.architectury.platform.Platform;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

public final class FloorParser {
    public static final int MAX_FLOORS = 6;

    private FloorParser() {
    }

    public static FloorDefinition parse(int number, JsonObject floor, SortedMap<Integer, JsonObject> waves) {
        return FloorParser.parse(number, floor, waves, Map.of());
    }

    public static FloorDefinition parse(int number, JsonObject floor, SortedMap<Integer, JsonObject> waves, Map<ResourceLocation, FloorDefinition.WaveType> waveTypes) {
        FloorParser.require(number >= 1 && number <= 6, "floor number must be 1..6");
        FloorParser.require(waves.size() == 3, "a challenge floor needs exactly three waves");
        Map<Challenge.Difficulty, List<Challenge.Type>> challenge = FloorParser.challenges(floor.get("challenge"));
        FloorParser.require(!floor.has("health_scale") && !floor.has("damage_scale"), "health_scale and damage_scale belong in mob_pools scaling metadata");
        int delay = 100;
        int ticks = 6000;
        JsonObject loot = floor.has("rewards") ? FloorParser.object(floor.get("rewards"), "rewards") : new JsonObject();
        ArrayList<FloorDefinition.RewardEntry> entries = new ArrayList<FloorDefinition.RewardEntry>();
        if (loot.has("loot_tables")) {
            for (JsonElement entry : FloorParser.array(loot.get("loot_tables"), "loot_tables")) {
                entries.add(new FloorDefinition.RewardEntry(FloorParser.id(entry), false, 1, 1, 1, 1.0));
            }
        }
        if (loot.has("entries")) {
            for (JsonElement entry : FloorParser.array(loot.get("entries"), "entries")) {
                JsonObject spec = FloorParser.object(entry, "reward entry");
                entries.add(FloorParser.rewardEntry(spec, 1.0));
            }
        }
        FloorParser.require(entries.size() <= 64, "at most 64 reward entries are allowed");
        if (loot.has("entries")) {
            int index = loot.has("loot_tables") ? loot.getAsJsonArray("loot_tables").size() : 0;
            ArrayList<FloorDefinition.RewardEntry> enabled = new ArrayList<FloorDefinition.RewardEntry>(entries.subList(0, index));
            for (JsonElement entry : loot.getAsJsonArray("entries")) {
                JsonObject spec = entry.getAsJsonObject();
                if (FloorParser.rewardProviderLoaded(spec)) {
                    enabled.add((FloorDefinition.RewardEntry)entries.get(index));
                }
                ++index;
            }
            entries = enabled;
        }
        int rolls = FloorParser.integer(loot, "rolls", entries.isEmpty() ? 0 : 1, 0, 100);
        int experience = FloorParser.integer(loot, "experience", 0, 0, 100000);
        ArrayList<FloorDefinition.Wave> parsed = new ArrayList<FloorDefinition.Wave>();
        int floorEnemies = 0;
        for (Map.Entry entry : waves.entrySet()) {
            FloorParser.require((Integer)entry.getKey() == parsed.size() + 1, "wave files must be consecutive, starting at wave_1.json");
            try {
                JsonObject wave2 = (JsonObject)entry.getValue();
                ArrayList<FloorDefinition.Enemy> enemies = new ArrayList<FloorDefinition.Enemy>();
                EnumMap<MobCategory, Integer> categories = new EnumMap<MobCategory, Integer>(MobCategory.class);
                int count = 0;
                for (MobCategory category2 : MobCategory.values()) {
                    int amount = FloorParser.integer(wave2, category2.key(), 0, 0, 128);
                    if (amount > 0) {
                        categories.put(category2, amount);
                    }
                    count += amount;
                }
                HashSet<String> known = new HashSet<String>(List.of("total_time", "bosses", "wave_types"));
                for (MobCategory category3 : MobCategory.values()) {
                    known.add(category3.key());
                }
                for (String key : wave2.keySet()) {
                    FloorParser.require(known.contains(key), "unknown wave field: " + key);
                }
                if (wave2.has("wave_types")) {
                    FloorParser.require(Arrays.stream(MobCategory.values()).noneMatch(category -> wave2.has(category.key())) && !wave2.has("bosses"), "wave_types cannot be combined with category counts or bosses");
                    Iterator names = FloorParser.array(wave2.get("wave_types"), "wave_types");
                    FloorParser.require(names.size() > 0 && names.size() <= 64, "wave_types needs 1..64 distinct type names");
                    ArrayList<FloorDefinition.WaveType> choices = new ArrayList<FloorDefinition.WaveType>();
                    HashSet<ResourceLocation> seen = new HashSet<ResourceLocation>();
                    Iterator category3 = names.iterator();
                    while (category3.hasNext()) {
                        JsonElement name = (JsonElement)category3.next();
                        ResourceLocation typeId = FloorParser.waveTypeId(name);
                        FloorParser.require(seen.add(typeId), "duplicate wave type: " + String.valueOf(typeId));
                        FloorParser.require(waveTypes.containsKey(typeId), "unknown or invalid wave type: " + String.valueOf(typeId));
                        choices.add(waveTypes.get(typeId));
                    }
                    FloorDefinition.Wave parsedWave = new FloorDefinition.Wave(Map.of(), List.of(), choices);
                    BossWavePolicy.validate(number, (Integer)entry.getKey(), parsedWave);
                    floorEnemies += parsedWave.minimumEnemies();
                    parsed.add(parsedWave);
                    continue;
                }
                for (JsonElement boss : wave2.has("bosses") ? FloorParser.array(wave2.get("bosses"), "bosses") : new JsonArray()) {
                    ResourceLocation mobId;
                    int amount = 1;
                    if (boss.isJsonPrimitive()) {
                        mobId = FloorParser.id(boss);
                    } else {
                        JsonObject spec = FloorParser.object(boss, "boss entry");
                        mobId = FloorParser.id(spec.get("id"));
                        amount = FloorParser.integer(spec, "count", 1, 1, 128);
                    }
                    if (!AbyssRegistries.ENTITY_TYPES.containsKey(mobId)) {
                        MobCatalog.warnUnavailableMob(mobId, "data/spiral_abyss/floor_" + number + "/wave_" + String.valueOf(entry.getKey()) + ".json");
                        continue;
                    }
                    FloorParser.require(AbyssRegistries.ENTITY_TYPES.getValue(mobId) != EntityType.f_20532_, "players cannot be wave enemies");
                    FloorParser.require(!mobId.equals((Object)new ResourceLocation("spiral_abyss", "key_entity")) && !mobId.equals((Object)new ResourceLocation("spiral_abyss", "reward_chest")) && !mobId.equals((Object)new ResourceLocation("spiral_abyss", "copper_chest")) && !mobId.equals((Object)new ResourceLocation("spiral_abyss", "iron_chest")) && !mobId.equals((Object)new ResourceLocation("spiral_abyss", "golden_chest")) && !mobId.equals((Object)new ResourceLocation("spiral_abyss", "experience_chest")), "arena furniture cannot be a wave enemy");
                    enemies.add(new FloorDefinition.Enemy(mobId, amount));
                    count += amount;
                }
                FloorParser.require(count > 0 && count <= 128, "each wave needs 1..128 mobs");
                floorEnemies += count;
                FloorDefinition.Wave parsedWave = new FloorDefinition.Wave(categories, enemies);
                BossWavePolicy.validate(number, (Integer)entry.getKey(), parsedWave);
                parsed.add(parsedWave);
            }
            catch (JsonParseException | IllegalArgumentException ex) {
                throw new JsonParseException("data/spiral_abyss/floor_" + number + "/wave_" + String.valueOf(entry.getKey()) + ".json: " + ex.getMessage(), ex);
            }
        }
        boolean bossOnly = parsed.stream().allMatch(FloorDefinition.Wave::bossOnly);
        FloorParser.require(parsed.stream().anyMatch(wave -> !wave.waveTypes().isEmpty()) || bossOnly || floorEnemies > 30, "ordinary legacy floors need at least 31 mobs in total");
        return new FloorDefinition(number, ticks, delay, new FloorDefinition.Rewards(entries, rolls, experience), parsed, challenge);
    }

    public static Map<Challenge.Difficulty, List<Challenge.Type>> challenges(JsonElement json) {
        JsonObject object = FloorParser.object(json, "challenge");
        for (String key : object.keySet()) {
            FloorParser.require(Set.of("easy", "medium", "hard").contains(key), "unknown challenge tier: " + key);
        }
        EnumMap result = new EnumMap(Challenge.Difficulty.class);
        for (Challenge.Difficulty difficulty : Challenge.Difficulty.values()) {
            String field = "challenge." + difficulty.key();
            JsonArray names = FloorParser.array(object.get(difficulty.key()), field);
            FloorParser.require(names.size() > 0 && names.size() <= Challenge.Type.values().length, field + " needs a nonempty list of distinct challenge names");
            ArrayList<Challenge.Type> types = new ArrayList<Challenge.Type>();
            for (JsonElement name : names) {
                FloorParser.require(name.isJsonPrimitive() && name.getAsJsonPrimitive().isString(), field + " entries must be challenge names");
                Challenge.Type type = Arrays.stream(Challenge.Type.values()).filter(value -> value.key().equals(name.getAsString())).findFirst().orElseThrow(() -> new JsonParseException(field + ": unknown challenge " + String.valueOf(name)));
                FloorParser.require(!types.contains((Object)type), field + ": duplicate challenge " + type.key());
                types.add(type);
            }
            result.put(difficulty, List.copyOf(types));
        }
        return Map.copyOf(result);
    }

    public static FloorDefinition.WaveType waveType(ResourceLocation id, JsonObject object) {
        try {
            FloorDefinition.WaveType result;
            for (String key : object.keySet()) {
                FloorParser.require(Set.of("groups", "sequence", "tier").contains(key), "unknown wave type field: " + key + "=" + String.valueOf(object.get(key)));
            }
            FloorParser.require(object.has("groups") != object.has("sequence"), "provide exactly one of groups or sequence");
            int tier = FloorParser.integer(object, "tier", 1, 1, 7);
            ArrayList<FloorDefinition.WaveStage> stages = new ArrayList<FloorDefinition.WaveStage>();
            if (object.has("groups")) {
                for (FloorDefinition.WaveGroup group : FloorParser.groups(object.get("groups"), "groups")) {
                    stages.add(new FloorDefinition.WaveStage(List.of(group)));
                }
            } else {
                JsonArray sequence = FloorParser.array(object.get("sequence"), "sequence");
                FloorParser.require(sequence.size() > 0 && sequence.size() <= 128, "sequence needs 1..128 stages; got " + sequence.size());
                for (int index = 0; index < sequence.size(); ++index) {
                    JsonObject stage = FloorParser.object(sequence.get(index), "sequence[" + index + "]");
                    for (String key : stage.keySet()) {
                        FloorParser.require(key.equals("groups"), "unknown sequence[" + index + "] field: " + key + "=" + String.valueOf(stage.get(key)));
                    }
                    stages.add(new FloorDefinition.WaveStage(FloorParser.groups(stage.get("groups"), "sequence[" + index + "].groups")));
                }
            }
            FloorParser.require((result = new FloorDefinition.WaveType(id, stages, tier)).count() <= 128, "wave type total count must be 1..128; got " + result.count());
            return result;
        }
        catch (JsonParseException ex) {
            throw new JsonParseException(String.valueOf(id) + " (data/" + id.m_135827_() + "/wave_types/" + id.m_135815_() + ".json): " + ex.getMessage(), (Throwable)ex);
        }
    }

    private static List<FloorDefinition.WaveGroup> groups(JsonElement value, String path) {
        JsonArray groups = FloorParser.array(value, path);
        FloorParser.require(groups.size() > 0 && groups.size() <= 128, path + " needs 1..128 groups; got " + groups.size());
        ArrayList<FloorDefinition.WaveGroup> result = new ArrayList<FloorDefinition.WaveGroup>();
        for (int index = 0; index < groups.size(); ++index) {
            int count;
            String field = path + "[" + index + "]";
            JsonObject group = FloorParser.object(groups.get(index), field);
            for (String key : group.keySet()) {
                FloorParser.require(Set.of("category", "count", "role").contains(key), field + ": unknown field " + key + "=" + String.valueOf(group.get(key)));
            }
            FloorParser.require(group.has("category") && group.get("category").isJsonPrimitive() && group.get("category").getAsJsonPrimitive().isString(), field + ".category must be a string; got " + String.valueOf(group.get("category")));
            MobCategory category = Arrays.stream(MobCategory.values()).filter(candidate -> candidate.key().equals(group.get("category").getAsString())).findFirst().orElseThrow(() -> new JsonParseException(field + ".category unknown: " + String.valueOf(group.get("category"))));
            FloorParser.require(group.has("count"), field + " needs count");
            try {
                count = FloorParser.integer(group, "count", 1, 1, 128);
            }
            catch (JsonParseException ex) {
                throw new JsonParseException(field + ".count=" + String.valueOf(group.get("count")) + ": " + ex.getMessage());
            }
            CombatRole role = CombatRole.ANY;
            if (group.has("role")) {
                JsonElement roleValue = group.get("role");
                FloorParser.require(roleValue.isJsonPrimitive() && roleValue.getAsJsonPrimitive().isString(), field + ".role must be melee, ranged or any; got " + String.valueOf(roleValue));
                role = Arrays.stream(CombatRole.values()).filter(candidate -> candidate.key().equals(roleValue.getAsString())).findFirst().orElseThrow(() -> new JsonParseException(field + ".role unknown: " + String.valueOf(roleValue)));
            }
            result.add(new FloorDefinition.WaveGroup(category, count, role));
        }
        return result;
    }

    private static ResourceLocation waveTypeId(JsonElement name) {
        ResourceLocation parsed = FloorParser.id(name);
        return name.getAsString().contains(":") ? parsed : new ResourceLocation("spiral_abyss", parsed.m_135815_());
    }

    private static boolean rewardProviderLoaded(JsonObject spec) {
        if (!spec.has("required_mod")) {
            return true;
        }
        JsonElement value = spec.get("required_mod");
        FloorParser.require(value.isJsonPrimitive() && value.getAsJsonPrimitive().isString() && value.getAsString().matches("[a-z][a-z0-9_]{1,63}"), "required_mod must be a mod id");
        return Platform.isModLoaded((String)value.getAsString());
    }

    public static FloorDefinition.RewardEntry rewardEntry(JsonObject spec, double defaultChance) {
        FloorParser.require(spec.has("item") != spec.has("loot_table"), "reward needs exactly one of item or loot_table");
        boolean item = spec.has("item");
        ResourceLocation rewardId = FloorParser.id(spec.get(item ? "item" : "loot_table"));
        int min = FloorParser.integer(spec, "min_count", FloorParser.integer(spec, "count", 1, 1, 4096), 1, 4096);
        int max = FloorParser.integer(spec, "max_count", min, min, 4096);
        int weight = FloorParser.integer(spec, "weight", 1, 1, 10000);
        double chance = defaultChance;
        if (spec.has("chance")) {
            JsonElement value = spec.get("chance");
            FloorParser.require(value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber(), "chance must be a number");
            chance = value.getAsDouble();
            FloorParser.require(Double.isFinite(chance) && chance >= 0.0 && chance <= 1.0, "chance must be 0..1");
        }
        LinkedHashMap<ResourceLocation, Integer> enchantments = new LinkedHashMap<ResourceLocation, Integer>();
        if (spec.has("enchantment")) {
            FloorParser.require(item, "enchantment is only supported on item rewards; use loot-table functions for tables");
            JsonObject configured = FloorParser.object(spec.get("enchantment"), "enchantment");
            FloorParser.require(configured.size() <= 64, "at most 64 reward enchantments are allowed");
            for (Map.Entry entry : configured.entrySet()) {
                ResourceLocation enchantment = FloorParser.id((JsonElement)new JsonPrimitive((String)entry.getKey()));
                FloorParser.require(!enchantments.containsKey(enchantment), "duplicate reward enchantment: " + String.valueOf(enchantment));
                enchantments.put(enchantment, FloorParser.integer(configured, (String)entry.getKey(), 1, 1, 255));
            }
        }
        return new FloorDefinition.RewardEntry(rewardId, item, min, max, weight, chance, false, enchantments);
    }

    private static int integer(JsonObject obj, String key, int fallback, int min, int max) {
        if (!obj.has(key)) {
            return fallback;
        }
        JsonElement value = obj.get(key);
        FloorParser.require(value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber(), key + "=" + String.valueOf(value) + " must be an integer");
        double number = value.getAsDouble();
        FloorParser.require(Double.isFinite(number) && number == Math.rint(number) && number >= (double)min && number <= (double)max, key + "=" + String.valueOf(value) + " must be an integer in " + min + ".." + max);
        return (int)number;
    }

    public static ResourceLocation id(JsonElement element) {
        FloorParser.require(element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString(), "resource ids must be strings");
        ResourceLocation id = ResourceLocation.m_135820_((String)element.getAsString());
        FloorParser.require(id != null, "invalid resource id: " + String.valueOf(element));
        return id;
    }

    private static JsonArray array(JsonElement value, String name) {
        FloorParser.require(value != null && value.isJsonArray(), name + " must be an array");
        return value.getAsJsonArray();
    }

    private static JsonObject object(JsonElement value, String name) {
        FloorParser.require(value != null && value.isJsonObject(), name + " must be an object");
        return value.getAsJsonObject();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new JsonParseException(message);
        }
    }
}


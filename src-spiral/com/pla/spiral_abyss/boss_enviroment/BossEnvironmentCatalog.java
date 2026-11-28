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
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.packs.resources.Resource
 *  net.minecraft.server.packs.resources.ResourceManager
 *  net.minecraft.server.packs.resources.SimplePreparableReloadListener
 *  net.minecraft.util.profiling.ProfilerFiller
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.BushBlock
 *  net.minecraft.world.level.block.FallingBlock
 *  net.minecraft.world.level.block.state.BlockState
 */
package com.pla.spiral_abyss.boss_enviroment;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.pla.spiral_abyss.boss_enviroment.EnvironmentPlacements;
import com.pla.spiral_abyss.encounter.data.FloorParser;
import com.pla.spiral_abyss.platform.AbyssRegistries;
import java.io.BufferedReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class BossEnvironmentCatalog
extends SimplePreparableReloadListener<Map<ResourceLocation, Profile>> {
    private static volatile Map<ResourceLocation, Profile> profiles = Map.of();

    private static EnvironmentType placement(JsonObject json, EnvironmentType fallback) {
        EnvironmentType legacy;
        EnvironmentType environmentType = legacy = json.has("environment_type") ? EnvironmentType.parse(json.get("environment_type")) : null;
        if (!json.has("decoration_placement")) {
            return legacy == null ? fallback : legacy;
        }
        JsonElement value = json.get("decoration_placement");
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new JsonParseException("decoration_placement must be surface, flat_room or flat_room_box");
        }
        EnvironmentType chosen = switch (value.getAsString()) {
            case "surface" -> EnvironmentType.PATCHES;
            case "flat_room" -> EnvironmentType.FLAT_ROOM;
            case "flat_room_box" -> EnvironmentType.FLAT_ROOM_BOX;
            default -> throw new JsonParseException("unknown decoration_placement " + value.getAsString());
        };
        if (legacy != null && legacy != chosen) {
            throw new JsonParseException("environment_type conflicts with decoration_placement; use decoration_placement only");
        }
        return chosen;
    }

    public static Profile get(ResourceLocation entity) {
        return profiles.get(entity);
    }

    public static Map<ResourceLocation, Profile> snapshot() {
        return profiles;
    }

    protected Map<ResourceLocation, Profile> prepare(ResourceManager manager, ProfilerFiller profiler) {
        TreeMap<ResourceLocation, Profile> result = new TreeMap<ResourceLocation, Profile>();
        HashSet overridden = new HashSet();
        manager.m_214159_("boss_environments", id -> id.m_135815_().endsWith(".json")).entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            try (BufferedReader reader = ((Resource)entry.getValue()).m_215508_();){
                JsonObject json = JsonParser.parseReader((Reader)reader).getAsJsonObject();
                Profile profile = BossEnvironmentCatalog.parse((ResourceLocation)entry.getKey(), json);
                HashSet<ResourceLocation> ids = new HashSet<ResourceLocation>();
                for (JsonElement entity : json.getAsJsonArray("entities")) {
                    if (ids.add(FloorParser.id(entity))) continue;
                    throw new JsonParseException("duplicate entity ID in profile");
                }
                for (ResourceLocation id2 : ids) {
                    if (!overridden.contains(id2)) continue;
                    throw new JsonParseException("duplicate boss environment entity " + String.valueOf(id2));
                }
                ids.forEach(id -> result.put((ResourceLocation)id, profile));
                overridden.addAll(ids);
            }
            catch (Exception ex) {
                LogUtils.getLogger().error("Invalid boss environment {}: {}", entry.getKey(), (Object)ex.getMessage());
            }
        });
        BossEnvironmentCatalog.overlayBossPool(manager, result);
        return Map.copyOf(result);
    }

    protected void apply(Map<ResourceLocation, Profile> loaded, ResourceManager manager, ProfilerFiller profiler) {
        profiles = loaded;
    }

    public static Profile parse(ResourceLocation id, JsonObject json) {
        double chance;
        Set<String> fields = Set.of("entities", "palette", "radius", "height", "patches", "animation_ticks", "hazard", "hazard_chance", "pillars", "decoration_placement", "room_radius", "environment_type");
        for (Object key : json.keySet()) {
            if (fields.contains(key)) continue;
            throw new JsonParseException("unknown field " + (String)key);
        }
        if (!json.has("entities") || json.getAsJsonArray("entities").isEmpty()) {
            throw new JsonParseException("entities needs at least one entity ID");
        }
        ArrayList<BlockState> palette = new ArrayList<BlockState>();
        for (JsonElement block : json.getAsJsonArray("palette")) {
            palette.add(BossEnvironmentCatalog.block(block));
        }
        if (palette.isEmpty() || palette.size() > 16) {
            throw new JsonParseException("palette needs 1..16 installed non-air blocks");
        }
        BlockState hazard = json.has("hazard") ? BossEnvironmentCatalog.block(json.get("hazard"), true) : null;
        double d = chance = json.has("hazard_chance") ? json.get("hazard_chance").getAsDouble() : 0.0;
        if (!Double.isFinite(chance) || chance < 0.0 || chance > 1.0) {
            throw new JsonParseException("hazard_chance must be 0..1");
        }
        Pillars pillars = null;
        if (json.has("pillars")) {
            JsonObject spec = json.getAsJsonObject("pillars");
            for (String key : spec.keySet()) {
                if (Set.of("block", "min_count", "max_count", "min_height", "max_height", "healing_crystals").contains(key)) continue;
                throw new JsonParseException("unknown pillar field " + key);
            }
            int minimum = BossEnvironmentCatalog.integer(spec, "min_count", 4, 1, 5);
            int minHeight = BossEnvironmentCatalog.integer(spec, "min_height", 18, 1, 32);
            pillars = new Pillars(BossEnvironmentCatalog.block(spec.get("block")), minimum, BossEnvironmentCatalog.integer(spec, "max_count", 5, minimum, 5), minHeight, BossEnvironmentCatalog.integer(spec, "max_height", 26, minHeight, 32), spec.has("healing_crystals") && spec.get("healing_crystals").getAsBoolean());
        }
        EnvironmentType type = BossEnvironmentCatalog.placement(json, EnvironmentType.PATCHES);
        BossEnvironmentCatalog.validateType(type, palette, hazard == null ? List.of() : List.of(hazard), false, 0, pillars, List.of(), List.of());
        return new Profile(id, palette, BossEnvironmentCatalog.integer(json, "radius", 32, 12, 32), BossEnvironmentCatalog.integer(json, "height", 3, 1, 4), BossEnvironmentCatalog.integer(json, "patches", 14, 1, 32), BossEnvironmentCatalog.integer(json, "animation_ticks", 100, 1, 100), hazard, chance, pillars, hazard == null ? List.of() : List.of(hazard), false, 0, List.of(), List.of(), type, BossEnvironmentCatalog.integer(json, "room_radius", 29, 12, 47));
    }

    private static void overlayBossPool(ResourceManager manager, Map<ResourceLocation, Profile> result) {
        ResourceLocation file = new ResourceLocation("spiral_abyss", "mob_pools/boss.json");
        Optional resource = manager.m_213713_(file);
        if (resource.isEmpty()) {
            return;
        }
        try (BufferedReader reader = ((Resource)resource.get()).m_215508_();){
            JsonArray pool = JsonParser.parseReader((Reader)reader).getAsJsonArray();
            HashSet<ResourceLocation> seen = new HashSet<ResourceLocation>();
            for (JsonElement value : pool) {
                ResourceLocation entity;
                JsonArray entry;
                if (!value.isJsonArray() || (entry = value.getAsJsonArray()).isEmpty() || !AbyssRegistries.ENTITY_TYPES.containsKey(entity = FloorParser.id(entry.get(0)))) continue;
                for (int index = 1; index < entry.size(); ++index) {
                    JsonElement metadata = entry.get(index);
                    if (!metadata.isJsonObject() || !BossEnvironmentCatalog.isMetadata(metadata.getAsJsonObject())) continue;
                    if (!seen.add(entity)) {
                        throw new JsonParseException("duplicate environment for " + String.valueOf(entity));
                    }
                    try {
                        result.put(entity, BossEnvironmentCatalog.poolEnvironment(entity, metadata.getAsJsonObject(), result.get(entity)));
                        continue;
                    }
                    catch (Exception ex) {
                        LogUtils.getLogger().error("Invalid boss environment in {} for {}: {}", new Object[]{file, entity, ex.getMessage()});
                    }
                }
            }
        }
        catch (Exception ex) {
            LogUtils.getLogger().error("Invalid boss environment metadata in {}: {}", (Object)file, (Object)ex.getMessage());
        }
    }

    public static boolean isMetadata(JsonObject object) {
        return object.has("environment") || object.has("environment_type") || object.has("decoration_placement") || object.has("room_radius") || object.has("decoration_environment") || object.has("entity_place") || object.has("block_place");
    }

    public static void validateMetadataFields(JsonObject metadata) {
        for (String key : metadata.keySet()) {
            if (Set.of("environment", "environment_type", "decoration_environment", "decoration_chance", "decoration_placement", "room_radius", "entity_place", "block_place").contains(key)) continue;
            throw new JsonParseException("unknown boss environment metadata field " + key);
        }
    }

    public static Profile poolEnvironment(ResourceLocation entity, JsonObject metadata, Profile base) {
        List<EnvironmentPlacements.Entry> entities;
        BlockState legacyHazard;
        double chance;
        List<Object> terrain;
        JsonObject json;
        BossEnvironmentCatalog.validateMetadataFields(metadata);
        JsonElement environment = metadata.get("environment");
        if (environment != null && environment.isJsonObject()) {
            if (metadata.size() != 1) {
                throw new JsonParseException("advanced environment object cannot have sibling decoration fields");
            }
            json = environment.getAsJsonObject();
        } else {
            json = new JsonObject();
            if (environment != null) {
                json.add("terrain_blocks", environment);
            }
            if (metadata.has("environment_type")) {
                json.add("environment_type", metadata.get("environment_type"));
            }
            if (metadata.has("decoration_placement")) {
                json.add("decoration_placement", metadata.get("decoration_placement"));
            }
            for (String field : List.of("entity_place", "block_place", "room_radius")) {
                if (!metadata.has(field)) continue;
                json.add(field, metadata.get(field));
            }
            if (metadata.has("decoration_environment")) {
                JsonObject decoration = new JsonObject();
                decoration.add("blocks", metadata.get("decoration_environment"));
                if (metadata.has("decoration_chance")) {
                    decoration.add("chance", metadata.get("decoration_chance"));
                }
                json.add("decoration_environment", (JsonElement)decoration);
            } else if (metadata.has("decoration_chance")) {
                throw new JsonParseException("decoration_environment is required for decoration_chance");
            }
        }
        for (String key : json.keySet()) {
            if (Set.of("terrain_blocks", "decoration_placement", "room_radius", "environment_type", "decoration_environment", "entity_place", "block_place").contains(key)) continue;
            throw new JsonParseException("unknown environment field " + key);
        }
        List<Object> list = terrain = base == null ? List.of() : base.palette();
        if (json.has("terrain_blocks")) {
            ArrayList<BlockState> chosen = new ArrayList<BlockState>();
            JsonArray blocks = BossEnvironmentCatalog.blockList(json.get("terrain_blocks"));
            if (blocks.size() > 16) {
                throw new JsonParseException("terrain_blocks supports at most 16 blocks");
            }
            for (JsonElement value : blocks) {
                try {
                    chosen.add(BossEnvironmentCatalog.block(value));
                }
                catch (JsonParseException ex) {
                    LogUtils.getLogger().warn("Ignoring boss terrain for {}: {}", (Object)entity, (Object)ex.getMessage());
                }
            }
            if (!chosen.isEmpty()) {
                terrain = chosen;
            } else if (terrain.isEmpty()) {
                throw new JsonParseException("terrain_blocks has no installed stable blocks");
            }
        }
        List<Object> decorations = base == null ? List.of() : base.decorations();
        boolean snowDirt = base != null && base.snowDirt();
        int waterDepth = base == null ? 0 : base.waterDepth();
        double d = chance = base == null ? 0.0 : base.hazardChance();
        if (json.has("decoration_environment")) {
            JsonObject decoration = json.getAsJsonObject("decoration_environment");
            for (String key : decoration.keySet()) {
                if (Set.of("blocks", "chance", "placement").contains(key)) continue;
                throw new JsonParseException("unknown decoration_environment field " + key);
            }
            if (!decoration.has("blocks")) {
                throw new JsonParseException("decoration_environment requires blocks");
            }
            ArrayList<BlockState> chosen = new ArrayList<BlockState>();
            JsonArray blocks = BossEnvironmentCatalog.blockList(decoration.get("blocks"));
            if (blocks.size() > 16) {
                throw new JsonParseException("decoration_environment supports at most 16 blocks");
            }
            snowDirt = false;
            waterDepth = 0;
            for (JsonElement value : blocks) {
                String preset;
                String string = preset = value.isJsonPrimitive() && value.getAsJsonPrimitive().isString() ? value.getAsString().replace("spiral_abyss:", "") : "";
                if (Set.of("full_water", "half_water").contains(preset)) {
                    if (blocks.size() != 1) {
                        throw new JsonParseException("Water must be the only decoration_environment entry");
                    }
                    waterDepth = preset.equals("full_water") ? 120 : 15;
                    continue;
                }
                if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString() && Set.of("snow_dirt", "spiral_abyss:snow_dirt").contains(value.getAsString())) {
                    if (blocks.size() != 1) {
                        throw new JsonParseException("snow_dirt must be the only decoration_environment entry");
                    }
                    snowDirt = true;
                    continue;
                }
                try {
                    chosen.add(BossEnvironmentCatalog.block(value, true));
                }
                catch (JsonParseException ex) {
                    LogUtils.getLogger().warn("Ignoring boss decoration for {}: {}; keeping its terrain", (Object)entity, (Object)ex.getMessage());
                }
            }
            if (decoration.has("placement")) {
                if (json.has("decoration_placement") && !json.get("decoration_placement").equals(decoration.get("placement"))) {
                    throw new JsonParseException("conflicting decoration placements");
                }
                json = json.deepCopy();
                json.add("decoration_placement", decoration.get("placement"));
            }
            double d2 = chance = decoration.has("chance") ? decoration.get("chance").getAsDouble() : 0.08;
            if (!Double.isFinite(chance) || chance < 0.0 || chance > 1.0) {
                throw new JsonParseException("decoration chance must be 0..1");
            }
            decorations = chosen;
        }
        if (snowDirt) {
            terrain = List.of(Blocks.f_50493_.m_49966_());
        }
        int radius = base == null ? 32 : base.radius();
        int height = base == null ? 3 : base.height();
        int patches = base == null ? 14 : base.patches();
        int ticks = base == null ? 100 : base.ticks();
        Pillars pillars = base == null ? null : base.pillars();
        BlockState blockState = legacyHazard = decorations.isEmpty() ? null : (BlockState)decorations.get(0);
        List<Object> list2 = json.has("entity_place") ? EnvironmentPlacements.parse(json, "entity_place", "entity") : (entities = base == null ? List.of() : base.entities());
        List<EnvironmentPlacements.Entry> blocks = json.has("block_place") ? EnvironmentPlacements.parse(json, "block_place", "block") : (base == null ? List.of() : base.blocks());
        EnvironmentType type = BossEnvironmentCatalog.placement(json, base == null ? EnvironmentType.PATCHES : base.environmentType());
        BossEnvironmentCatalog.validateType(type, terrain, decorations, snowDirt, waterDepth, pillars, entities, blocks);
        return new Profile(entity, terrain, radius, height, patches, ticks, legacyHazard, chance, pillars, decorations, snowDirt, waterDepth, entities, blocks, type, BossEnvironmentCatalog.integer(json, "room_radius", base == null ? 29 : base.roomRadius(), 12, 47));
    }

    private static void validateType(EnvironmentType type, List<BlockState> terrain, List<BlockState> decorations, boolean snowDirt, int waterDepth, Pillars pillars, List<EnvironmentPlacements.Entry> entities, List<EnvironmentPlacements.Entry> blocks) {
        if (type == EnvironmentType.PATCHES) {
            return;
        }
        if (terrain.isEmpty()) {
            throw new JsonParseException("flat_room requires an environment block palette");
        }
        if (!decorations.isEmpty() || snowDirt || waterDepth > 0 || pillars != null) {
            throw new JsonParseException("flat_room cannot contain decorations, water or pillars");
        }
    }

    private static JsonArray blockList(JsonElement value) {
        if (value == null) {
            throw new JsonParseException("block list is required");
        }
        if (value.isJsonArray()) {
            return value.getAsJsonArray();
        }
        if (!value.isJsonPrimitive()) {
            throw new JsonParseException("block list must be a block ID or array");
        }
        JsonArray list = new JsonArray();
        list.add(value);
        return list;
    }

    static BlockState block(JsonElement value) {
        return BossEnvironmentCatalog.block(value, false);
    }

    private static BlockState block(JsonElement value, boolean decoration) {
        ResourceLocation id = FloorParser.id(value);
        if (!AbyssRegistries.BLOCKS.containsKey(id) || AbyssRegistries.BLOCKS.getValue(id).m_49966_().m_60795_()) {
            throw new JsonParseException("unavailable block " + String.valueOf(id));
        }
        BlockState state = AbyssRegistries.BLOCKS.getValue(id).m_49966_();
        if (state.m_155947_()) {
            throw new JsonParseException("block entities are not supported: " + String.valueOf(id));
        }
        if (state.m_60734_() instanceof FallingBlock || !state.m_60819_().m_76178_() || state.m_60713_(Blocks.f_50077_) || !decoration && state.m_60734_() instanceof BushBlock) {
            throw new JsonParseException("unsupported terrain/decoration block: " + String.valueOf(id));
        }
        return state;
    }

    static int integer(JsonObject json, String key, int fallback, int min, int max) {
        double value;
        if (!(!json.has(key) || json.get(key).isJsonPrimitive() && json.get(key).getAsJsonPrimitive().isNumber())) {
            throw new JsonParseException(key + " must be a number");
        }
        double d = value = json.has(key) ? json.get(key).getAsDouble() : (double)fallback;
        if (!Double.isFinite(value) || value != Math.rint(value) || value < (double)min || value > (double)max) {
            throw new JsonParseException(key + " must be integer " + min + ".." + max);
        }
        return (int)value;
    }

    public static enum EnvironmentType {
        PATCHES,
        FLAT_ROOM,
        FLAT_ROOM_BOX;


        static EnvironmentType parse(JsonElement value) {
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
                throw new JsonParseException("environment_type must be patches, flat_room or flat_room_box");
            }
            return switch (value.getAsString()) {
                case "patches" -> PATCHES;
                case "flat_room" -> FLAT_ROOM;
                case "flat_room_box" -> FLAT_ROOM_BOX;
                default -> throw new JsonParseException("unknown environment_type " + value.getAsString());
            };
        }
    }

    public record Profile(ResourceLocation id, List<BlockState> palette, int radius, int height, int patches, int ticks, BlockState hazard, double hazardChance, Pillars pillars, List<BlockState> decorations, boolean snowDirt, int waterDepth, List<EnvironmentPlacements.Entry> entities, List<EnvironmentPlacements.Entry> blocks, EnvironmentType environmentType, int roomRadius) {
        public Profile {
            palette = List.copyOf(palette);
            decorations = List.copyOf(decorations);
            entities = List.copyOf(entities);
            blocks = List.copyOf(blocks);
        }

        public Profile(ResourceLocation id, List<BlockState> palette, int radius, int height, int patches, int ticks, BlockState hazard, double hazardChance, Pillars pillars, List<BlockState> decorations, boolean snowDirt) {
            this(id, palette, radius, height, patches, ticks, hazard, hazardChance, pillars, decorations, snowDirt, 0, List.of(), List.of(), EnvironmentType.PATCHES, 29);
        }

        public Profile(ResourceLocation id, List<BlockState> palette, int radius, int height, int patches, int ticks, BlockState hazard, double hazardChance, Pillars pillars, List<BlockState> decorations) {
            this(id, palette, radius, height, patches, ticks, hazard, hazardChance, pillars, decorations, false);
        }

        public Profile(ResourceLocation id, List<BlockState> palette, int radius, int height, int patches, int ticks, BlockState hazard, double hazardChance, Pillars pillars) {
            this(id, palette, radius, height, patches, ticks, hazard, hazardChance, pillars, hazard == null ? List.of() : List.of(hazard));
        }

        public boolean flatRoom() {
            return this.environmentType == EnvironmentType.FLAT_ROOM || this.roomBox();
        }

        public boolean roomBox() {
            return this.environmentType == EnvironmentType.FLAT_ROOM_BOX;
        }

        public boolean centered() {
            return this.flatRoom() || !this.entities.isEmpty() || !this.blocks.isEmpty();
        }
    }

    public record Pillars(BlockState block, int minimum, int maximum, int minHeight, int maxHeight, boolean crystals) {
    }
}


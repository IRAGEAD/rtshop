package com.rtshop.story;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 이야기 모드 몹(Mowzie's Mobs, Aquamirae)이 이야기와 상관없는 들판에서 너무 자주 나오지 않게:
 * 저절로 생기는 몹을 모드별 비율만 남기고 줄인다. 그 모드의 구조물 안이나 이야기 생물군계(얼음 미로 등)에서는 그대로 나온다.
 * config/rtshop/wild_spawns.json 으로 조절 (1 = 줄이지 않음).
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class WildSpawns {
    private WildSpawns() {
    }

    public static final class Cfg {
        public final Map<String, Double> keep = new LinkedHashMap<>();
        public final Set<String> storyBiomes = new HashSet<>();
        public boolean keepInStructures = true;
    }

    private static Cfg cfg;

    public static Cfg defaults() {
        Cfg c = new Cfg();
        c.keep.put("mowziesmobs", 0.15);
        c.keep.put("aquamirae", 0.15);
        c.storyBiomes.add("aquamirae:ice_maze");
        return c;
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("wild_spawns.json");
    }

    public static Cfg get() {
        if (cfg == null) reload();
        return cfg;
    }

    public static void reload() {
        Cfg c = defaults();
        Path f = file();
        try {
            if (Files.exists(f)) {
                JsonObject o = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
                if (o.has("keep")) {
                    c.keep.clear();
                    for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("keep").entrySet()) c.keep.put(e.getKey(), e.getValue().getAsDouble());
                }
                if (o.has("storyBiomes")) {
                    c.storyBiomes.clear();
                    for (JsonElement e : o.getAsJsonArray("storyBiomes")) c.storyBiomes.add(e.getAsString());
                }
                if (o.has("keepInStructures")) c.keepInStructures = o.get("keepInStructures").getAsBoolean();
            } else {
                Files.createDirectories(f.getParent());
                JsonObject o = new JsonObject();
                o.addProperty("_help", "이야기 모드 몹이 저절로 생기는 비율 (0~1, 1 = 줄이지 않음). 그 모드의 구조물 안과 storyBiomes 생물군계에서는 줄이지 않습니다.");
                JsonObject k = new JsonObject();
                c.keep.forEach(k::addProperty);
                o.add("keep", k);
                JsonArray b = new JsonArray();
                c.storyBiomes.forEach(b::add);
                o.add("storyBiomes", b);
                o.addProperty("keepInStructures", c.keepInStructures);
                Files.writeString(f, new GsonBuilder().setPrettyPrinting().create().toJson(o), StandardCharsets.UTF_8);
            }
        } catch (Exception ex) {
            RtShop.LOGGER.warn("[rtshop] wild_spawns.json 을 읽지 못했습니다: {}", ex.toString());
        }
        cfg = c;
    }

    /** 이야기 장소(그 모드의 구조물 안, 또는 이야기 생물군계)인가. */
    public static boolean storyArea(ServerLevel level, BlockPos pos, String ns) {
        Cfg c = get();
        var biome = level.getBiome(pos).unwrapKey();
        if (biome.isPresent() && c.storyBiomes.contains(biome.get().location().toString())) return true;
        if (c.keepInStructures) {
            var reg = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
            for (Structure s : level.structureManager().getAllStructuresAt(pos).keySet()) {
                ResourceLocation id = reg.getKey(s);
                if (id != null && id.getNamespace().equals(ns)) return true;
            }
        }
        return false;
    }

    /** 이 모드 몹이 들판에서 남는 비율 (없으면 1). */
    public static double keepFor(String ns) {
        Double d = get().keep.get(ns);
        return d == null ? 1.0 : d;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (event.isSpawnCancelled()) return;
        MobSpawnType t = event.getSpawnType();
        if (t != MobSpawnType.NATURAL && t != MobSpawnType.CHUNK_GENERATION) return;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType());
        if (id == null) return;
        double keep = keepFor(id.getNamespace());
        if (keep >= 1) return;
        ServerLevel level = event.getLevel().getLevel();
        BlockPos pos = BlockPos.containing(event.getX(), event.getY(), event.getZ());
        if (storyArea(level, pos, id.getNamespace())) return;
        if (level.random.nextDouble() >= keep) event.setSpawnCancelled(true);
    }
}

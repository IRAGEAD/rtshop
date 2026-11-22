package com.rtshop.story;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forgespi.language.IModFileInfo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 보스 구조물을 드물게: 보스 모드의 구조물 배치(structure_set)의 간격을 늘린 데이터팩을 만들어 가장 위에 얹는다.
 * 원래 값은 그 모드 jar 에서 읽어 비율만 곱하므로 모드가 바뀌어도 맞는다. 새로 만들어지는 땅에만 적용된다.
 * config/rtshop/structures.json — 모드별 배율, 구조물 세트별 배율(덮어쓰기), 0 이나 1 은 그대로.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class StructureRarity {
    private StructureRarity() {
    }

    public static final String PACK_ID = "rtshop_story_structures";

    public boolean enabled = true;
    /** 모드 ID -> 간격 배율 */
    public final Map<String, Double> mods = new LinkedHashMap<>();
    /** 구조물 세트 ID(모드:이름) -> 간격 배율 (mods 보다 우선) */
    public final Map<String, Double> sets = new LinkedHashMap<>();
    /** 마지막으로 만든 결과: 세트 ID -> "원래 간격 → 새 간격" */
    public static final Map<String, String> LAST = new LinkedHashMap<>();

    static StructureRarity defaults() {
        StructureRarity c = new StructureRarity();
        c.mods.put("cataclysm", 1.8);
        c.mods.put("mowziesmobs", 1.6);
        c.mods.put("aquamirae", 1.4);
        // 보스가 없는 작은 구조물은 그대로
        c.sets.put("cataclysm:amethyst_nest", 1.0);
        c.sets.put("cataclysm:abandoned_structures", 1.0);
        c.sets.put("aquamirae:surface", 1.0);
        c.sets.put("aquamirae:outpost", 1.0);
        return c;
    }

    static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("structures.json");
    }

    static Path packDir() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("generated").resolve(PACK_ID);
    }

    public static StructureRarity load() {
        StructureRarity d = defaults();
        Path f = file();
        try {
            if (Files.exists(f)) {
                JsonObject o = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
                StructureRarity c = new StructureRarity();
                if (o.has("enabled")) c.enabled = o.get("enabled").getAsBoolean();
                if (o.has("mods")) for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("mods").entrySet()) c.mods.put(e.getKey(), e.getValue().getAsDouble());
                if (o.has("sets")) for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("sets").entrySet()) c.sets.put(e.getKey(), e.getValue().getAsDouble());
                return c;
            }
            Files.createDirectories(f.getParent());
            JsonObject o = new JsonObject();
            o.addProperty("_help", "보스 구조물을 드물게: mods 는 모드별 구조물 간격 배율(1.8 이면 1.8배 멀리), sets 는 구조물 세트별 배율(우선). "
                    + "1 은 그대로. 새로 만들어지는 땅에만 적용되며 게임을 다시 켜야 바뀐다.");
            o.addProperty("enabled", d.enabled);
            JsonObject m = new JsonObject();
            d.mods.forEach(m::addProperty);
            o.add("mods", m);
            JsonObject s = new JsonObject();
            d.sets.forEach(s::addProperty);
            o.add("sets", s);
            Files.writeString(f, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(o), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            RtShop.LOGGER.warn("[rtshop] structures.json 을 읽지 못했습니다: {}", ex.toString());
        }
        return d;
    }

    /** 간격을 곱한다. 간격은 분리보다 커야 하므로 분리도 같이 늘린다. 원래 JSON 이 아니면 null. */
    public static JsonObject scale(JsonObject set, double k) {
        if (!set.has("placement") || !set.get("placement").isJsonObject()) return null;
        JsonObject pl = set.getAsJsonObject("placement");
        if (!pl.has("spacing") || !pl.has("separation")) return null;
        int sp = pl.get("spacing").getAsInt();
        int se = pl.get("separation").getAsInt();
        int nsp = Math.min(4096, Math.max(sp, (int) Math.round(sp * k)));
        int nse = Math.min(nsp - 1, Math.max(se, (int) Math.round(se * k)));
        JsonObject out = set.deepCopy();
        JsonObject np = out.getAsJsonObject("placement");
        np.addProperty("spacing", nsp);
        np.addProperty("separation", Math.max(0, nse));
        return out;
    }

    /** 모드 jar 를 읽어 config/rtshop/generated 아래에 데이터팩을 만든다. 만든 세트 수. */
    public static int build(StructureRarity c) {
        LAST.clear();
        Path root = packDir();
        try {
            if (Files.exists(root)) {
                try (Stream<Path> w = Files.walk(root)) {
                    List<Path> all = new ArrayList<>(w.toList());
                    all.sort((x, y) -> y.getNameCount() - x.getNameCount());
                    for (Path p : all) Files.deleteIfExists(p);
                }
            }
            Files.createDirectories(root);
            Files.writeString(root.resolve("pack.mcmeta"),
                    "{\"pack\":{\"pack_format\":15,\"description\":\"RT Shop: 보스 구조물을 드물게\"}}", StandardCharsets.UTF_8);
            if (!c.enabled) return 0;
            int n = 0;
            for (String mod : c.mods.keySet()) {
                IModFileInfo info = ModList.get().getModFileById(mod);
                if (info == null) continue;
                Path dir = info.getFile().findResource("data", mod, "worldgen", "structure_set");
                if (dir == null || !Files.isDirectory(dir)) continue;
                List<Path> files;
                try (Stream<Path> w = Files.list(dir)) {
                    files = w.filter(p -> p.getFileName().toString().endsWith(".json")).toList();
                }
                for (Path p : files) {
                    String name = p.getFileName().toString().replace(".json", "");
                    String id = mod + ":" + name;
                    double k = c.sets.getOrDefault(id, c.mods.get(mod));
                    if (k <= 0 || Math.abs(k - 1) < 1e-3) continue;
                    JsonObject set = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
                    JsonObject out = scale(set, k);
                    if (out == null) continue;
                    Path dest = root.resolve("data").resolve(mod).resolve("worldgen").resolve("structure_set").resolve(name + ".json");
                    Files.createDirectories(dest.getParent());
                    Files.writeString(dest, out.toString(), StandardCharsets.UTF_8);
                    LAST.put(id, set.getAsJsonObject("placement").get("spacing").getAsInt() + " → " + out.getAsJsonObject("placement").get("spacing").getAsInt());
                    n++;
                }
            }
            if (n > 0) RtShop.LOGGER.info("[rtshop] 보스 구조물 간격 조정: {}", LAST);
            return n;
        } catch (Exception ex) {
            RtShop.LOGGER.warn("[rtshop] 구조물 데이터팩을 만들지 못했습니다: {}", ex.toString());
            return 0;
        }
    }

    @SubscribeEvent
    public static void onPacks(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) return;
        int n = build(load());
        if (n <= 0) return;
        Path root = packDir();
        Pack pack = Pack.readMetaAndCreate(PACK_ID, Component.literal("RT Shop 보스 구조물"), true,
                id -> new PathPackResources(id, root, true), PackType.SERVER_DATA, Pack.Position.TOP, PackSource.BUILT_IN);
        if (pack != null) event.addRepositorySource(consumer -> consumer.accept(pack));
    }
}

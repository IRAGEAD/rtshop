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
 * 3.80 (사용자 11-28 14:05 "야생에서 spmovesets의 보스 구조물과 같은 구조물들이 나오지 않도록"): off 에 적은 모드(또는 모드:세트)의 구조물은 배치 확률(frequency)을 0 으로 —
 * 새로 만들어지는 땅에 아예 생기지 않는다 (이미 만들어진 땅의 구조물은 남는다). 기본 off = ["spmovesets"].
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
    /** 3.80: 아예 생기지 않게 할 구조물 — "모드" (그 모드의 모든 구조물 세트) 또는 "모드:세트". */
    public final java.util.Set<String> off = new java.util.LinkedHashSet<>();
    /** structures.json 에 off 칸이 없던 옛 파일: 기본값을 넣어 다시 쓴다. */
    transient boolean migrated;
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
        c.off.add("spmovesets");
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
                if (o.has("off") && o.get("off").isJsonArray()) {
                    for (JsonElement e : o.getAsJsonArray("off")) {
                        String id = e.getAsString().trim();
                        if (!id.isEmpty()) c.off.add(id);
                    }
                } else {
                    c.off.addAll(d.off);   // 3.80 전의 파일: 기본(spmovesets 끔)을 넣고 파일에도 적는다
                    c.migrated = true;
                }
                if (c.migrated) write(f, c);
                return c;
            }
            write(f, d);
        } catch (Exception ex) {
            RtShop.LOGGER.warn("[rtshop] structures.json 을 읽지 못했습니다: {}", ex.toString());
        }
        return d;
    }

    private static void write(Path f, StructureRarity c) throws java.io.IOException {
        Files.createDirectories(f.getParent());
        JsonObject o = new JsonObject();
        o.addProperty("_help", "보스 구조물을 드물게: mods 는 모드별 구조물 간격 배율(1.8 이면 1.8배 멀리), sets 는 구조물 세트별 배율(우선). "
                + "1 은 그대로. off (3.80): 아예 생기지 않게 할 구조물 — \"모드\" 또는 \"모드:세트\" (기본 spmovesets). 새로 만들어지는 땅에만 적용되며 서버를 다시 켜야 바뀐다.");
        o.addProperty("enabled", c.enabled);
        JsonObject m = new JsonObject();
        c.mods.forEach(m::addProperty);
        o.add("mods", m);
        JsonObject s = new JsonObject();
        c.sets.forEach(s::addProperty);
        o.add("sets", s);
        com.google.gson.JsonArray a = new com.google.gson.JsonArray();
        c.off.forEach(a::add);
        o.add("off", a);
        Files.writeString(f, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(o), StandardCharsets.UTF_8);
    }

    /** 3.80: 이 구조물 세트를 끄는가 (모드째 또는 세트 하나). */
    boolean isOff(String mod, String id) {
        return off.contains(mod) || off.contains(id);
    }

    /** 3.80: 배치 확률을 0 으로 — 그 배치는 아무 청크에도 구조물을 놓지 않는다 (간격 · 분리 · 소금은 그대로). 원래 JSON 이 아니면 null. */
    public static JsonObject disable(JsonObject set) {
        if (!set.has("placement") || !set.get("placement").isJsonObject()) return null;
        JsonObject out = set.deepCopy();
        out.getAsJsonObject("placement").addProperty("frequency", 0.0);
        return out;
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
                    "{\"pack\":{\"pack_format\":15,\"description\":\"RT Shop: 보스 구조물을 드물게 · 끈 구조물\"}}", StandardCharsets.UTF_8);
            if (!c.enabled) return 0;
            int n = 0;
            java.util.Set<String> mods = new java.util.LinkedHashSet<>(c.mods.keySet());
            for (String o : c.off) mods.add(o.contains(":") ? o.substring(0, o.indexOf(':')) : o);
            for (String mod : mods) {
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
                    JsonObject out;
                    JsonObject set;
                    if (c.isOff(mod, id)) {
                        set = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
                        out = disable(set);
                        if (out == null) continue;
                        Path dest = root.resolve("data").resolve(mod).resolve("worldgen").resolve("structure_set").resolve(name + ".json");
                        Files.createDirectories(dest.getParent());
                        Files.writeString(dest, out.toString(), StandardCharsets.UTF_8);
                        LAST.put(id, "off");
                        n++;
                        continue;
                    }
                    Double kk = c.sets.getOrDefault(id, c.mods.get(mod));
                    if (kk == null) continue;
                    double k = kk;
                    if (k <= 0 || Math.abs(k - 1) < 1e-3) continue;
                    set = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
                    out = scale(set, k);
                    if (out == null) continue;
                    Path dest = root.resolve("data").resolve(mod).resolve("worldgen").resolve("structure_set").resolve(name + ".json");
                    Files.createDirectories(dest.getParent());
                    Files.writeString(dest, out.toString(), StandardCharsets.UTF_8);
                    LAST.put(id, set.getAsJsonObject("placement").get("spacing").getAsInt() + " → " + out.getAsJsonObject("placement").get("spacing").getAsInt());
                    n++;
                }
            }
            if (n > 0) RtShop.LOGGER.info("[rtshop] 보스 구조물 간격 조정 · 끈 구조물: {}", LAST);
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

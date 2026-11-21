package com.rtshop.land;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** config/rtshop/land.json */
public final class LandConfig {
    /** 토지 문서를 쓸 때 발 아래로 포함되는 깊이 (16칸 토지 기준). 큰 토지는 2배. */
    public int depthBelowFeet = 4;
    /** 한 사람이 가질 수 있는 토지 구역 수 (0 = 제한 없음). */
    public int maxClaimsPerPlayer = 0;
    /** 폭발로부터 토지 안의 블록을 보호. */
    public boolean protectFromExplosions = true;
    /** 토지 밖의 피스톤이 토지 안의 블록을 밀거나 당기지 못하게 함. */
    public boolean protectFromPistons = true;
    public int pinMinLength = 4;
    public int pinMaxLength = 8;
    /** PIN 을 맞게 입력한 뒤 다시 묻지 않는 시간(분). */
    public int pinUnlockMinutes = 5;
    public int pinMaxAttempts = 5;
    public int pinLockoutSeconds = 60;
    /** 다른 사람의 토지 근처(블록)에는 토지를 만들 수 없음 (0 = 바로 옆에도 가능). */
    public int minDistanceToOthers = 0;
    /** 넓은 범위에 작용하는 블록: 남의 토지에서 이 거리(블록) 안에는 놓을 수 없다. */
    public final Map<String, Integer> rangedBlocks = new LinkedHashMap<>();

    // ---- 구획(칸 단위 모양의 토지, 3.9)
    /** 구획 토지가 땅속으로 덮는 깊이: 구획 안 가장 낮은 지면에서 이만큼 아래부터 월드 꼭대기까지. */
    public int plotDepthBelow = 4;
    /** 가격을 정하지 않고 구획을 만들 때 1칸(바닥 1블록)당 가격 (G). */
    public long plotPricePerBlock = 40;
    /** 구획을 반환할 때 돌려주는 비율 (구매가 기준 %). */
    public int plotRefundPercent = 50;
    /** 한 사람이 살 수 있는 구획 수 (0 = 제한 없음). */
    public int plotMaxPerPlayer = 0;
    /** 자동 채우기로 한 번에 고를 수 있는 최대 칸 수. 넘으면 경계가 끊긴 것으로 보고 멈춘다. */
    public int plotFillMaxArea = 20000;
    /** 자동 채우기 후 밭 둘레의 풀 테두리 한 줄을 함께 고른다 (길·물·다른 토지로는 넘어가지 않음). */
    public boolean plotFillGrowRim = true;
    /** 자동 채우기가 "밭"으로 보는 지면 블록 (블록 ID 또는 #태그). 작물·꽃 태그는 따로 넣지 않아도 들어간다. */
    public final List<String> plotFillBlocks = new ArrayList<>(List.of(
            "minecraft:farmland", "#minecraft:crops", "#minecraft:small_flowers", "#minecraft:tall_flowers",
            "minecraft:grass", "minecraft:tall_grass", "minecraft:fern", "minecraft:large_fern",
            "minecraft:sweet_berry_bush", "minecraft:small_dripleaf", "minecraft:cave_vines", "minecraft:cave_vines_plant",
            "minecraft:moss_block", "minecraft:moss_carpet", "minecraft:coarse_dirt", "minecraft:rooted_dirt",
            "minecraft:podzol", "minecraft:dirt", "minecraft:mud", "minecraft:packed_mud", "minecraft:hay_block",
            "minecraft:pumpkin", "minecraft:melon", "minecraft:composter", "minecraft:melon_stem", "minecraft:pumpkin_stem",
            "minecraft:attached_melon_stem", "minecraft:attached_pumpkin_stem"));
    /** 테두리를 넓힐 때 넘어가지 않는 블록 (길 등). 물·용암과 다른 토지는 항상 넘어가지 않는다. */
    public final List<String> plotRimStopBlocks = new ArrayList<>(List.of("minecraft:dirt_path", "minecraft:gravel"));

    public LandConfig() {
        rangedBlocks.put("better_farming_plus:farming_crate", 5);
    }

    private static List<String> strings(JsonElement e) {
        List<String> out = new ArrayList<>();
        if (e != null && e.isJsonArray()) {
            for (JsonElement x : e.getAsJsonArray()) {
                String v = x.getAsString().trim();
                if (!v.isEmpty()) {
                    out.add(v);
                }
            }
        }
        return out;
    }

    private static JsonArray array(List<String> list) {
        JsonArray a = new JsonArray();
        list.forEach(a::add);
        return a;
    }

    private static LandConfig instance;

    public static synchronized LandConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public static synchronized LandConfig reload() {
        instance = load();
        return instance;
    }

    private static LandConfig load() {
        LandConfig c = new LandConfig();
        Path p = FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("land.json");
        try {
            if (Files.exists(p)) {
                JsonObject o = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
                if (o.has("depthBelowFeet")) c.depthBelowFeet = Math.max(0, Math.min(15, o.get("depthBelowFeet").getAsInt()));
                if (o.has("maxClaimsPerPlayer")) c.maxClaimsPerPlayer = Math.max(0, o.get("maxClaimsPerPlayer").getAsInt());
                if (o.has("protectFromExplosions")) c.protectFromExplosions = o.get("protectFromExplosions").getAsBoolean();
                if (o.has("protectFromPistons")) c.protectFromPistons = o.get("protectFromPistons").getAsBoolean();
                if (o.has("pinMinLength")) c.pinMinLength = Math.max(1, o.get("pinMinLength").getAsInt());
                if (o.has("pinMaxLength")) c.pinMaxLength = Math.max(c.pinMinLength, Math.min(16, o.get("pinMaxLength").getAsInt()));
                if (o.has("pinUnlockMinutes")) c.pinUnlockMinutes = Math.max(0, o.get("pinUnlockMinutes").getAsInt());
                if (o.has("pinMaxAttempts")) c.pinMaxAttempts = Math.max(1, o.get("pinMaxAttempts").getAsInt());
                if (o.has("pinLockoutSeconds")) c.pinLockoutSeconds = Math.max(0, o.get("pinLockoutSeconds").getAsInt());
                if (o.has("minDistanceToOthers")) c.minDistanceToOthers = Math.max(0, o.get("minDistanceToOthers").getAsInt());
                if (o.has("rangedBlocks") && o.get("rangedBlocks").isJsonObject()) {
                    c.rangedBlocks.clear();
                    for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("rangedBlocks").entrySet()) {
                        c.rangedBlocks.put(e.getKey(), Math.max(0, Math.min(32, e.getValue().getAsInt())));
                    }
                }
                if (o.has("plotDepthBelow")) c.plotDepthBelow = Math.max(0, Math.min(64, o.get("plotDepthBelow").getAsInt()));
                if (o.has("plotPricePerBlock")) c.plotPricePerBlock = Math.max(0, o.get("plotPricePerBlock").getAsLong());
                if (o.has("plotRefundPercent")) c.plotRefundPercent = Math.max(0, Math.min(100, o.get("plotRefundPercent").getAsInt()));
                if (o.has("plotMaxPerPlayer")) c.plotMaxPerPlayer = Math.max(0, o.get("plotMaxPerPlayer").getAsInt());
                if (o.has("plotFillMaxArea")) c.plotFillMaxArea = Math.max(16, Math.min(200000, o.get("plotFillMaxArea").getAsInt()));
                if (o.has("plotFillGrowRim")) c.plotFillGrowRim = o.get("plotFillGrowRim").getAsBoolean();
                if (o.has("plotFillBlocks")) {
                    c.plotFillBlocks.clear();
                    c.plotFillBlocks.addAll(strings(o.get("plotFillBlocks")));
                }
                if (o.has("plotRimStopBlocks")) {
                    c.plotRimStopBlocks.clear();
                    c.plotRimStopBlocks.addAll(strings(o.get("plotRimStopBlocks")));
                }
                if (!o.has("plotPricePerBlock")) {
                    // 3.9 이전 파일: 값은 그대로 두고 구획 설정만 덧붙인다
                    addPlotKeys(o, c);
                    Files.writeString(p, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(o), StandardCharsets.UTF_8);
                }
            } else {
                JsonObject o = new JsonObject();
                JsonArray help = new JsonArray();
                help.add("depthBelowFeet: 문서를 쓸 때 발 아래로 포함되는 깊이(16칸 토지 기준, 큰 토지는 2배)");
                help.add("maxClaimsPerPlayer: 1인당 토지 구역 수 제한 (0 = 무제한) · minDistanceToOthers: 남의 토지와 떨어져야 하는 거리");
                help.add("pinUnlockMinutes: PIN 을 맞힌 뒤 다시 묻지 않는 시간 · pinMaxAttempts/pinLockoutSeconds: 틀린 횟수 제한과 잠금 시간");
                help.add("rangedBlocks: 넓은 범위에 작용하는 블록(자동 수확 상자 등). 남의 토지에서 이 거리 안에는 놓을 수 없음");
                o.add("_설명", help);
                o.addProperty("depthBelowFeet", c.depthBelowFeet);
                o.addProperty("maxClaimsPerPlayer", c.maxClaimsPerPlayer);
                o.addProperty("minDistanceToOthers", c.minDistanceToOthers);
                o.addProperty("protectFromExplosions", c.protectFromExplosions);
                o.addProperty("protectFromPistons", c.protectFromPistons);
                o.addProperty("pinMinLength", c.pinMinLength);
                o.addProperty("pinMaxLength", c.pinMaxLength);
                o.addProperty("pinUnlockMinutes", c.pinUnlockMinutes);
                o.addProperty("pinMaxAttempts", c.pinMaxAttempts);
                o.addProperty("pinLockoutSeconds", c.pinLockoutSeconds);
                JsonObject ranged = new JsonObject();
                c.rangedBlocks.forEach(ranged::addProperty);
                o.add("rangedBlocks", ranged);
                addPlotKeys(o, c);
                Files.createDirectories(p.getParent());
                Files.writeString(p, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(o), StandardCharsets.UTF_8);
            }
        } catch (Exception ex) {
            RtShop.LOGGER.error("[rtshop] land.json 을 읽지 못해 기본값을 사용합니다.", ex);
        }
        return c;
    }

    private static void addPlotKeys(JsonObject o, LandConfig c) {
        JsonArray help = new JsonArray();
        help.add("구획(칸 단위 모양의 토지): OP 가 /rtshop plot wand 로 받은 구획 지팡이로 모양을 고르고 /rtshop plot create 로 등록");
        help.add("plotDepthBelow: 구획 안 가장 낮은 지면에서 아래로 덮는 깊이 · plotPricePerBlock: 가격을 안 적었을 때 1칸당 가격");
        help.add("plotRefundPercent: 반환 시 구매가의 몇 %를 돌려줄지 · plotMaxPerPlayer: 1인당 구획 수 (0 = 무제한)");
        help.add("plotFillBlocks: 자동 채우기가 밭으로 보는 지면 블록(ID 또는 #태그) · plotRimStopBlocks: 테두리를 넓힐 때 넘지 않는 블록");
        o.add("_구획_설명", help);
        o.addProperty("plotDepthBelow", c.plotDepthBelow);
        o.addProperty("plotPricePerBlock", c.plotPricePerBlock);
        o.addProperty("plotRefundPercent", c.plotRefundPercent);
        o.addProperty("plotMaxPerPlayer", c.plotMaxPerPlayer);
        o.addProperty("plotFillMaxArea", c.plotFillMaxArea);
        o.addProperty("plotFillGrowRim", c.plotFillGrowRim);
        o.add("plotFillBlocks", array(c.plotFillBlocks));
        o.add("plotRimStopBlocks", array(c.plotRimStopBlocks));
    }
}

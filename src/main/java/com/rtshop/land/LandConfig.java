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
    /** 구획(농경지)을 사면 그 토지 칸 안의 지면 위 것(작물·구조물·나무)을 치운다. */
    public boolean plotClearOnBuy = true;
    /** 토지를 비운 뒤, 잘린 채 남는 나무와 공중에 뜬 조각을 토지 둘레 이만큼 안에서 치운다 (0 = 하지 않음). */
    public int sweepRadius = 10;
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

    // ---- 초보 농지 (3.11)
    /** 이 직업을 얻은 플레이어에게 초보 농지 하나를 무료로 준다 (빈 문자열 = 끔). */
    public String starterPlotJob = "farmer";
    /** 초보 농지로 쓰는 구획 묶음 이름. */
    public String starterPlotGroup = "starter";
    /** 그 직업을 그만두면 초보 농지를 회수해 다른 초보 농부에게 돌린다. */
    public boolean starterPlotReturnWhenJobLost = true;

    // ---- 성 안 토지 (3.13)
    /** 아모노스 맵이면 성 안 토지를 자동으로 등록한다. */
    public boolean castlePlotsAuto = true;
    /** 성 안 토지 16×16 한 칸(256칸) 가격. 길·성벽이 빠져 작은 토지는 넓이에 비례해 싸진다. */
    public long castlePlotPrice = 50000;
    /** 토지 상인 창에서 구획을 살 때 상인 가까이(블록) 있어야 한다. */
    public int plotMerchantRange = 12;
    /** 성 영역(아모노스 맵의 강·바다로 둘러싸인 성과 농경지): 블록 부수기·놓기·상호작용 금지, 몬스터 스폰 금지. */
    public boolean castleZoneProtect = true;

    // ---- 상점 토지 (3.79, W-4)
    /** 아모노스 맵이면 기본 장터(동문 안 광장의 상점 토지)를 자동으로 등록한다. */
    public boolean shopLotsAuto = true;
    /** 상점 토지 값 = 토지 문서(16×16) 값의 블록당 값 × 넓이 × 이 배수. 사용자 답 (11-28): 2배. */
    public double shopLotPriceFactor = 2.0;
    /** 값의 바탕이 되는 토지 문서 (토지 상인이 파는 값을 쓴다, 못 찾으면 shopLotDeedPrice). */
    public String shopLotDeedItem = "rtshop:land_deed";
    public long shopLotDeedPrice = 40000;
    /** 한 사람이 가질 수 있는 상점 토지 수 (0 = 제한 없음). 사용자 답: 제한 없음. */
    public int shopLotMaxPerPlayer = 0;
    /** 주마다 내는 자릿세 = 산 값의 이 % (사용자 답: 약간). 0 = 없음. */
    public double shopLotRentPercent = 2.0;
    /** 자릿세를 셈하는 때 (주간 초기화와 같게: 1 = 월요일, 시각). */
    public int shopLotRentDay = 1;
    public int shopLotRentHour = 6;
    /** 밀린 자릿세는 이 주 수만큼까지만 쌓인다 (오래 비운 사람이 돌아왔을 때 빚이 끝없이 커지지 않게). */
    public int shopLotRentMaxWeeks = 4;
    /** 플레이어 상점 진열대는 상점 토지 안에만 새로 놓을 수 있다 (이미 놓인 진열대는 그대로). 사용자 메모 W-4. */
    public boolean shopStandsOnlyOnShopLots = true;

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
                if (o.has("plotClearOnBuy")) c.plotClearOnBuy = o.get("plotClearOnBuy").getAsBoolean();
                if (o.has("sweepRadius")) c.sweepRadius = Math.max(0, Math.min(24, o.get("sweepRadius").getAsInt()));
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
                if (o.has("starterPlotJob")) c.starterPlotJob = o.get("starterPlotJob").getAsString().trim();
                if (o.has("starterPlotGroup")) c.starterPlotGroup = o.get("starterPlotGroup").getAsString().trim();
                if (o.has("starterPlotReturnWhenJobLost")) c.starterPlotReturnWhenJobLost = o.get("starterPlotReturnWhenJobLost").getAsBoolean();
                if (o.has("castlePlotsAuto")) c.castlePlotsAuto = o.get("castlePlotsAuto").getAsBoolean();
                if (o.has("castlePlotPrice")) c.castlePlotPrice = Math.max(0, o.get("castlePlotPrice").getAsLong());
                if (o.has("plotMerchantRange")) c.plotMerchantRange = Math.max(2, o.get("plotMerchantRange").getAsInt());
                if (o.has("castleZoneProtect")) c.castleZoneProtect = o.get("castleZoneProtect").getAsBoolean();
                if (o.has("shopLotsAuto")) c.shopLotsAuto = o.get("shopLotsAuto").getAsBoolean();
                if (o.has("shopLotPriceFactor")) c.shopLotPriceFactor = Math.max(0, o.get("shopLotPriceFactor").getAsDouble());
                if (o.has("shopLotDeedItem")) c.shopLotDeedItem = o.get("shopLotDeedItem").getAsString().trim();
                if (o.has("shopLotDeedPrice")) c.shopLotDeedPrice = Math.max(0, o.get("shopLotDeedPrice").getAsLong());
                if (o.has("shopLotMaxPerPlayer")) c.shopLotMaxPerPlayer = Math.max(0, o.get("shopLotMaxPerPlayer").getAsInt());
                if (o.has("shopLotRentPercent")) c.shopLotRentPercent = Math.max(0, Math.min(100, o.get("shopLotRentPercent").getAsDouble()));
                if (o.has("shopLotRentDay")) c.shopLotRentDay = Math.max(1, Math.min(7, o.get("shopLotRentDay").getAsInt()));
                if (o.has("shopLotRentHour")) c.shopLotRentHour = Math.max(0, Math.min(23, o.get("shopLotRentHour").getAsInt()));
                if (o.has("shopLotRentMaxWeeks")) c.shopLotRentMaxWeeks = Math.max(1, o.get("shopLotRentMaxWeeks").getAsInt());
                if (o.has("shopStandsOnlyOnShopLots")) c.shopStandsOnlyOnShopLots = o.get("shopStandsOnlyOnShopLots").getAsBoolean();
                if (!o.has("shopLotPriceFactor")) {
                    // 3.79 이전 파일: 값은 그대로 두고 상점 토지 설정만 덧붙인다
                    addShopLotKeys(o, c);
                    Files.writeString(p, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(o), StandardCharsets.UTF_8);
                }
                if (!o.has("castleZoneProtect") && o.has("castlePlotPrice")) {
                    o.addProperty("castleZoneProtect", c.castleZoneProtect);
                    Files.writeString(p, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(o), StandardCharsets.UTF_8);
                }
                if (!o.has("castlePlotPrice")) {
                    addCastleKeys(o, c);
                    Files.writeString(p, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(o), StandardCharsets.UTF_8);
                }
                if (!o.has("starterPlotJob")) {
                    // 3.11 이전 파일: 값은 그대로 두고 초보 농지 설정만 덧붙인다
                    if (!o.has("plotPricePerBlock")) {
                        addPlotKeys(o, c);
                    }
                    addStarterKeys(o, c);
                    Files.writeString(p, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(o), StandardCharsets.UTF_8);
                } else if (!o.has("plotPricePerBlock")) {
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
                addStarterKeys(o, c);
                addCastleKeys(o, c);
                addShopLotKeys(o, c);
                Files.createDirectories(p.getParent());
                Files.writeString(p, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(o), StandardCharsets.UTF_8);
            }
        } catch (Exception ex) {
            RtShop.LOGGER.error("[rtshop] land.json 을 읽지 못해 기본값을 사용합니다.", ex);
        }
        return c;
    }

    private static void addCastleKeys(JsonObject o, LandConfig c) {
        JsonArray help = new JsonArray();
        help.add("성 안 토지: 아모노스 맵(MedievalCity-Amonos)이면 성 안 네 구역(A 북서·B 북동·C 남서·D 남)에 빈틈 없이 나눈 약 256블록짜리 토지 25칸을 판매합니다 (네모가 아닌 모양, 길·성벽·표시한 건물은 제외)");
        help.add("토지 상인 창의 [구획 토지] 에서 지도로 미리 보고 삽니다. 사면 그 자리의 건물이 철거되고 평지(풀밭)가 됩니다 (길·남의 토지는 그대로)");
        help.add("castlePlotPrice: 16×16 한 칸 가격 (길이 빠져 작은 칸은 넓이만큼 싸짐) · castlePlotsAuto: false 면 자동 등록 안 함 (/rtshop plot castle install)");
        help.add("plotMerchantRange: 토지 상인 창에서 구획을 살 때 상인과의 최대 거리");
        o.add("_성안토지_설명", help);
        o.addProperty("castlePlotsAuto", c.castlePlotsAuto);
        o.addProperty("castlePlotPrice", c.castlePlotPrice);
        o.addProperty("plotMerchantRange", c.plotMerchantRange);
        o.addProperty("castleZoneProtect", c.castleZoneProtect);
    }

    private static void addShopLotKeys(JsonObject o, LandConfig c) {
        JsonArray help = new JsonArray();
        help.add("상점 토지 (3.79): 플레이어 상점 진열대를 놓는 땅. 아모노스 맵이면 동문 안 광장(중앙 호수와 동문 사이)에 장터 10칸(6×4)을 자동으로 등록합니다 (shopLotsAuto)");
        help.add("토지 상인 창의 [구획 토지] → [상점 토지] 에서 지도로 보고 삽니다. 값 = 토지 문서(shopLotDeedItem, 못 찾으면 shopLotDeedPrice) 의 블록당 값 × 넓이 × shopLotPriceFactor");
        help.add("shopLotMaxPerPlayer: 한 사람이 가질 수 있는 수 (0 = 제한 없음) · 운영자는 구획 지팡이로 만든 구획을 /rtshop plot shoplot on 으로 상점 토지로 바꿀 수 있습니다");
        help.add("shopLotRentPercent: 주마다 내는 자릿세 = 산 값의 % (예금 → 지갑 순, 접속하지 않아도). 못 내면 밀린 만큼(shopLotRentMaxWeeks 주까지) 쌓이고 그 토지의 진열대가 문을 닫습니다. 돈이 생기면 저절로 냅니다");
        help.add("shopStandsOnlyOnShopLots: true 면 진열대는 상점 토지(내 것, 또는 놓기 권한을 받은 곳) 안에만 새로 놓을 수 있습니다. 이미 놓인 진열대는 그대로 씁니다");
        o.add("_상점토지_설명", help);
        o.addProperty("shopLotsAuto", c.shopLotsAuto);
        o.addProperty("shopLotPriceFactor", c.shopLotPriceFactor);
        o.addProperty("shopLotDeedItem", c.shopLotDeedItem);
        o.addProperty("shopLotDeedPrice", c.shopLotDeedPrice);
        o.addProperty("shopLotMaxPerPlayer", c.shopLotMaxPerPlayer);
        o.addProperty("shopLotRentPercent", c.shopLotRentPercent);
        o.addProperty("shopLotRentDay", c.shopLotRentDay);
        o.addProperty("shopLotRentHour", c.shopLotRentHour);
        o.addProperty("shopLotRentMaxWeeks", c.shopLotRentMaxWeeks);
        o.addProperty("shopStandsOnlyOnShopLots", c.shopStandsOnlyOnShopLots);
    }

    private static void addStarterKeys(JsonObject o, LandConfig c) {
        JsonArray help = new JsonArray();
        help.add("초보 농지: starterPlotJob 직업을 얻은 플레이어에게 starterPlotGroup 묶음의 빈 구획 하나를 무료로 줍니다 (한 사람당 한 번)");
        help.add("OP 가 /rtshop plot starter <번호> on 으로 구획을 묶음에 넣습니다. starterPlotJob 을 \"\" 로 두면 끕니다");
        help.add("starterPlotReturnWhenJobLost: true(기본) 면 그 직업을 그만둘 때 농지를 회수해 다른 초보에게 돌립니다. false 면 계속 가집니다");
        o.add("_초보농지_설명", help);
        o.addProperty("starterPlotJob", c.starterPlotJob);
        o.addProperty("starterPlotGroup", c.starterPlotGroup);
        o.addProperty("starterPlotReturnWhenJobLost", c.starterPlotReturnWhenJobLost);
    }

    private static void addPlotKeys(JsonObject o, LandConfig c) {
        JsonArray help = new JsonArray();
        help.add("구획(칸 단위 모양의 토지): OP 가 /rtshop plot wand 로 받은 구획 지팡이로 모양을 고르고 /rtshop plot create 로 등록");
        help.add("plotDepthBelow: 구획 안 가장 낮은 지면에서 아래로 덮는 깊이 · plotPricePerBlock: 가격을 안 적었을 때 1칸당 가격");
        help.add("plotClearOnBuy: 구획(농경지)을 사면 토지 칸 안의 작물·구조물·나무를 치웁니다 (경작지·흙·물은 그대로, 치운 블록은 world/rtshop/plot_backup 에 저장)");
        help.add("sweepRadius: 토지를 비운 뒤 둘레 이만큼 안에서, 토지에 걸쳐 잘린 나무(줄기·잎 통째)와 공중에 뜬 조각을 치웁니다 (0 = 끔). 땅과 이어진 건물·길·남의 토지는 건드리지 않습니다");
        help.add("plotRefundPercent: 반환 시 구매가의 몇 %를 돌려줄지 · plotMaxPerPlayer: 1인당 구획 수 (0 = 무제한)");
        help.add("plotFillBlocks: 자동 채우기가 밭으로 보는 지면 블록(ID 또는 #태그) · plotRimStopBlocks: 테두리를 넓힐 때 넘지 않는 블록");
        o.add("_구획_설명", help);
        o.addProperty("plotDepthBelow", c.plotDepthBelow);
        o.addProperty("plotPricePerBlock", c.plotPricePerBlock);
        o.addProperty("plotRefundPercent", c.plotRefundPercent);
        o.addProperty("plotClearOnBuy", c.plotClearOnBuy);
        o.addProperty("sweepRadius", c.sweepRadius);
        o.addProperty("plotMaxPerPlayer", c.plotMaxPerPlayer);
        o.addProperty("plotFillMaxArea", c.plotFillMaxArea);
        o.addProperty("plotFillGrowRim", c.plotFillGrowRim);
        o.add("plotFillBlocks", array(c.plotFillBlocks));
        o.add("plotRimStopBlocks", array(c.plotRimStopBlocks));
    }
}

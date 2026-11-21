package com.rtshop.shop;

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

/**
 * config/rtshop/pricing.json : 레시피 원가 기반 가격 산정과 품질(무게) 배율 설정.
 *
 * 음식은 "재료를 상점에서 사서 만드는 최소 비용"에 조리 단계의 수고비와 마진을 더한 값으로 매입한다.
 * 그래서 재료를 사서 조리해 팔면 적자가 나지 않는다.
 */
public final class PricingConfig {
    /** 상점에서 산 물건을 되팔 때 받는 비율 (0.5 = 50% 감가). */
    public double sellRatio = 0.5;
    public boolean recipePricing = true;
    /** 음식의 상점 판매가 = 매입가 × 이 값 (재판매 50% 감가 규칙과 맞추기 위해 2.0). */
    public double foodBuyMultiplier = 2.0;
    /**
     * 가격표에 없는 말단 재료의 추정가. 공짜로 얻을 수 있는 재료일 수도 있으므로 낮게 둔다
     * (높으면 그 재료가 들어간 음식값이 부풀어 돈벌이 수단이 된다). 값을 제대로 쳐 주려면 상점 파일에 가격을 적는다.
     */
    public double defaultIngredientValue = 2;
    /** 가격표에 없는 '먹을 수 있는' 말단 재료의 추정가 = 포만감 공식 가격 × 이 값. */
    public double edibleEstimateFactor = 2.0;
    /**
     * 요리 매입가 하한: "레시피의 재료를 모두 상점에서 사서 만들어 팔아도 손해가 없게" 한다.
     * 켜 두면 어떤 레시피로 만들든 (재료 구입비 × (1+margin) + labor) 이상으로 매입한다.
     */
    public boolean purchaseFloor = true;
    /** 요리 재료인데 어느 상점에서도 팔지 않는 농산물·식재료를 식품 상인에 자동으로 올린다. */
    public boolean autoListFoodIngredients = true;
    /** 자동으로 올린 재료 중 값을 알 수 없는(먹을 수 없는) 것의 판매가. 먹을 수 있는 것은 포만감 공식 가격 × 2. */
    public int autoIngredientPrice = 20;
    public final Map<String, Double> margins = new LinkedHashMap<>();
    public final Map<String, Double> labor = new LinkedHashMap<>();
    public final Map<String, Double> flatExtra = new LinkedHashMap<>();
    public final Map<String, String> fluidCosts = new LinkedHashMap<>();
    public final Map<String, List<String>> typeExtras = new LinkedHashMap<>();

    /**
     * 직접 낚은 물고기(무게 정보가 있는 것)의 매입가 배율. 기준 = 가격표 가격의 50%.
     * 기본값 2.0 은 평균 무게의 물고기를 가격표 가격 그대로 사 준다는 뜻이다.
     */
    public double fishBaseMultiplier = 2.0;
    /** 물고기 무게 보정 폭: 가장 무거우면 +25%, 가장 가벼우면 -25% (기본값이면 가격표 가격의 75%~125%). */
    public double fishWeightBonus = 0.25;
    /** 황금 물고기의 배율 (기준 = 가격표 가격의 50%). 기본값 6.0 = 가격표 가격의 3배. */
    public double fishGoldenMultiplier = 6.0;
    /** 무게 정보가 없는 물고기(상점에서 산 것, 몹 드롭 등)의 배율. 1.0 = 상점 판매가의 50%. */
    public double fishNoInfoMultiplier = 1.0;

    private static PricingConfig instance;

    public static synchronized PricingConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public static synchronized PricingConfig reload() {
        instance = load();
        return instance;
    }

    private void defaults() {
        // 마진(%)과 수고비(G/조리 1회). 사람이 직접 손을 써야 하는 조리 단계에만 붙인다.
        // 화로·조합대처럼 자동화하거나 한꺼번에 찍어 낼 수 있는 단계에는 붙이지 않는다 (붙이면 돈이 무한히 불어난다).
        // 수고비는 "조리 1회"에 붙는다 (한 번에 9인분을 볶아도 1회). 여러 대를 동시에 돌릴 수 있는 도구일수록 낮게 잡았다.
        for (String t : new String[]{"kaleidoscope_cookery:pot", "kaleidoscope_cookery:flex_pot"}) {
            margins.put(t, 0.05);
            labor.put(t, 80.0);
        }
        for (String t : new String[]{"kaleidoscope_cookery:stockpot", "kaleidoscope_cookery:flex_stockpot"}) {
            margins.put(t, 0.05);
            labor.put(t, 60.0);
        }
        margins.put("kaleidoscope_cookery:steamer", 0.05);
        labor.put("kaleidoscope_cookery:steamer", 10.0);
        margins.put("kaleidoscope_cookery:teapot", 0.05);
        labor.put("kaleidoscope_cookery:teapot", 20.0);
        margins.put("kaleidoscope_tavern:barrel", 0.15);
        labor.put("kaleidoscope_tavern:barrel", 50.0);
        margins.put("kaleidoscope_tavern:shaker", 0.05);
        labor.put("kaleidoscope_tavern:shaker", 40.0);
        // Farmer's Delight 의 조리 냄비는 호퍼로 재료를 넣어 자동으로 돌릴 수 있어서 기본값으로는 수고비를 주지 않는다 (본전).
        // 자동화를 막아 둔 서버라면 pricing.json 에 "farmersdelight:cooking" 을 추가해 수고비를 줄 수 있다.
        // 액체 1회 사용분을 아이템으로 환산
        fluidCosts.put("minecraft:lava", "minecraft:lava_bucket*1");
        fluidCosts.put("minecraft:milk", "minecraft:milk_bucket*1");
        fluidCosts.put("minecraft:cod_bucket", "minecraft:cod_bucket*1");
        fluidCosts.put("minecraft:salmon_bucket", "minecraft:salmon_bucket*1");
        fluidCosts.put("minecraft:pufferfish_bucket", "minecraft:pufferfish_bucket*1");
        fluidCosts.put("minecraft:tropical_fish_bucket", "minecraft:tropical_fish_bucket*1");
        fluidCosts.put("kaleidoscope_tavern:grape_juice", "kaleidoscope_tavern:grape*2");
        fluidCosts.put("kaleidoscope_tavern:green_grape_juice", "kaleidoscope_tavern:green_grape*2");
        fluidCosts.put("kaleidoscope_tavern:gold_grape_juice", "kaleidoscope_tavern:gold_grape*2");
        fluidCosts.put("kaleidoscope_tavern:ice_grape_juice", "kaleidoscope_tavern:ice_grape*2");
        fluidCosts.put("kaleidoscope_tavern:sweet_berries_juice", "minecraft:sweet_berries*2");
        fluidCosts.put("kaleidoscope_tavern:glow_berries_juice", "minecraft:glow_berries*2");
        List<String> rice = new ArrayList<>();
        rice.add("#forge:cooked_rice");
        typeExtras.put("kaleidoscope_cookery:rice_bowl", rice);
    }

    public RecipeSolver.Config solverConfig() {
        RecipeSolver.Config c = new RecipeSolver.Config();
        c.margins.putAll(margins);
        c.labor.putAll(labor);
        c.flatExtra.putAll(flatExtra);
        c.fluidCosts.putAll(fluidCosts);
        c.typeExtras.putAll(typeExtras);
        return c;
    }

    private static void readDoubles(JsonObject o, String key, Map<String, Double> target) {
        if (o.has(key) && o.get(key).isJsonObject()) {
            target.clear();
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject(key).entrySet()) {
                target.put(e.getKey(), e.getValue().getAsDouble());
            }
        }
    }

    private static JsonObject doubles(Map<String, Double> m) {
        JsonObject o = new JsonObject();
        m.forEach(o::addProperty);
        return o;
    }

    private static PricingConfig load() {
        PricingConfig c = new PricingConfig();
        c.defaults();
        Path p = FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("pricing.json");
        try {
            if (Files.exists(p)) {
                JsonObject o = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
                if (o.has("sellRatio")) c.sellRatio = Math.max(0, Math.min(1, o.get("sellRatio").getAsDouble()));
                if (o.has("recipePricing")) c.recipePricing = o.get("recipePricing").getAsBoolean();
                if (o.has("foodBuyMultiplier")) c.foodBuyMultiplier = Math.max(1.0, o.get("foodBuyMultiplier").getAsDouble());
                if (o.has("defaultIngredientValue")) c.defaultIngredientValue = Math.max(0.01, o.get("defaultIngredientValue").getAsDouble());
                if (o.has("edibleEstimateFactor")) c.edibleEstimateFactor = Math.max(0, o.get("edibleEstimateFactor").getAsDouble());
                if (o.has("purchaseFloor")) c.purchaseFloor = o.get("purchaseFloor").getAsBoolean();
                if (o.has("autoListFoodIngredients")) c.autoListFoodIngredients = o.get("autoListFoodIngredients").getAsBoolean();
                if (o.has("autoIngredientPrice")) c.autoIngredientPrice = Math.max(1, o.get("autoIngredientPrice").getAsInt());
                if (o.has("fishBaseMultiplier")) c.fishBaseMultiplier = Math.max(0, o.get("fishBaseMultiplier").getAsDouble());
                if (o.has("fishWeightBonus")) c.fishWeightBonus = Math.max(0, Math.min(0.9, o.get("fishWeightBonus").getAsDouble()));
                if (o.has("fishGoldenMultiplier")) c.fishGoldenMultiplier = Math.max(1, o.get("fishGoldenMultiplier").getAsDouble());
                if (o.has("fishNoInfoMultiplier")) c.fishNoInfoMultiplier = Math.max(0, o.get("fishNoInfoMultiplier").getAsDouble());
                readDoubles(o, "margins", c.margins);
                readDoubles(o, "labor", c.labor);
                readDoubles(o, "flatExtra", c.flatExtra);
                if (o.has("fluidCosts") && o.get("fluidCosts").isJsonObject()) {
                    c.fluidCosts.clear();
                    for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("fluidCosts").entrySet()) {
                        c.fluidCosts.put(e.getKey(), e.getValue().getAsString());
                    }
                }
                if (o.has("typeExtras") && o.get("typeExtras").isJsonObject()) {
                    c.typeExtras.clear();
                    for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("typeExtras").entrySet()) {
                        List<String> l = new ArrayList<>();
                        e.getValue().getAsJsonArray().forEach(x -> l.add(x.getAsString()));
                        c.typeExtras.put(e.getKey(), l);
                    }
                }
            } else {
                JsonObject o = new JsonObject();
                JsonArray help = new JsonArray();
                help.add("sellRatio: 되팔 때 받는 비율 (0.5 = 구매가의 50%)");
                help.add("recipePricing: 레시피 원가로 음식 가격을 자동 계산 (끄면 상점 파일의 가격만 사용)");
                help.add("margins: 레시피 종류별 마진(0.05 = 5%) · labor: 조리 1회당 수고비(G) · flatExtra: 1회당 추가 원가(기본 없음)");
                help.add("주의: 화로·조합대처럼 자동화할 수 있는 레시피 종류에 margin/labor/flatExtra 를 주면 재료를 사서 만들어 파는 것만으로 돈이 불어납니다");
                help.add("음식 매입가 = (재료 원가 + flatExtra) × (1 + margin) + labor, 음식 판매가 = 매입가 × foodBuyMultiplier");
                help.add("purchaseFloor 가 켜져 있으면 어떤 조리법으로 만들든 재료를 상점에서 산 값 이상으로 매입합니다");
                help.add("defaultIngredientValue: 가격표에 없는 재료의 추정가(낮게 유지 권장) · edibleEstimateFactor: 먹을 수 있는 재료의 추정가 배율");
                help.add("purchaseFloor: 재료를 모두 상점에서 사서 만들어 팔아도 손해가 없도록 음식 매입가를 올림 · autoListFoodIngredients/autoIngredientPrice: 상점에 없는 요리 재료를 식품 상인에 자동으로 올림");
                help.add("fluidCosts: 레시피에 쓰이는 액체 1회분을 '아이템ID*개수'로 환산 · typeExtras: 레시피 파일에 없지만 실제로 드는 재료");
                help.add("물고기 매입가 = 기준 매입가(상점 판매가×sellRatio) × fishBaseMultiplier × (1 ± fishWeightBonus). 무거울수록 비쌈");
                help.add("fishGoldenMultiplier: 황금 물고기 배율 · fishNoInfoMultiplier: 무게 정보가 없는 물고기(상점에서 산 것 등) 배율");
                o.add("_설명", help);
                o.addProperty("sellRatio", c.sellRatio);
                o.addProperty("recipePricing", c.recipePricing);
                o.addProperty("foodBuyMultiplier", c.foodBuyMultiplier);
                o.addProperty("defaultIngredientValue", c.defaultIngredientValue);
                o.addProperty("edibleEstimateFactor", c.edibleEstimateFactor);
                o.addProperty("purchaseFloor", c.purchaseFloor);
                o.addProperty("autoListFoodIngredients", c.autoListFoodIngredients);
                o.addProperty("autoIngredientPrice", c.autoIngredientPrice);
                o.addProperty("fishBaseMultiplier", c.fishBaseMultiplier);
                o.addProperty("fishWeightBonus", c.fishWeightBonus);
                o.addProperty("fishGoldenMultiplier", c.fishGoldenMultiplier);
                o.addProperty("fishNoInfoMultiplier", c.fishNoInfoMultiplier);
                o.add("margins", doubles(c.margins));
                o.add("labor", doubles(c.labor));
                o.add("flatExtra", doubles(c.flatExtra));
                JsonObject fl = new JsonObject();
                c.fluidCosts.forEach(fl::addProperty);
                o.add("fluidCosts", fl);
                JsonObject te = new JsonObject();
                c.typeExtras.forEach((k, v) -> {
                    JsonArray a = new JsonArray();
                    v.forEach(a::add);
                    te.add(k, a);
                });
                o.add("typeExtras", te);
                Files.createDirectories(p.getParent());
                Files.writeString(p, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(o), StandardCharsets.UTF_8);
            }
        } catch (Exception ex) {
            RtShop.LOGGER.error("[rtshop] pricing.json 을 읽지 못해 기본값을 사용합니다.", ex);
        }
        return c;
    }
}

package com.rtshop.farm;

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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** config/rtshop/farming.json : 씨앗 규칙과 작물 별 등급. */
public final class FarmConfig {
    /** 작물을 캐거나 상자를 열었을 때 씨앗이 나오지 않게 한다. */
    public boolean removeSeedDrops = true;
    /** 상점에서 산 씨앗만 심을 수 있게 한다. */
    public boolean requireShopSeeds = true;
    /** 상점에서 산 "심기 전용" 작물(씨감자 등)은 먹을 수 없게 한다. */
    public boolean blockEatingShopSeeds = true;
    /** 가격표에 없는 씨앗(다른 모드의 작물)을 씨앗 상점에 자동으로 올린다. 끄면 그 작물은 키울 수 없다. */
    public boolean autoListMissingSeeds = true;
    /** 자동으로 올리는 씨앗의 가격. */
    public int autoSeedPrice = 20;
    /** 자동으로 올리는 '심기 전용' 작물의 가격 = 작물 가격 x 이 값. */
    public double autoDualSeedFactor = 2.5;
    /** 작물 자체가 씨앗 역할도 하는 아이템. 수확물은 심을 수 없고, 상점의 심기 전용 품목만 심을 수 있다. */
    public final Set<String> dualPurpose = new LinkedHashSet<>(Arrays.asList(
            "minecraft:carrot", "minecraft:potato", "minecraft:nether_wart", "minecraft:cocoa_beans",
            "minecraft:sweet_berries", "minecraft:glow_berries",
            "farmersdelight:onion", "farmersdelight:rice",
            "kaleidoscope_cookery:rice", "kaleidoscope_cookery:wild_rice",
            "farm_and_charm:onion"));
    /** 자동 판별에서 빠진 씨앗을 직접 추가. */
    public final Set<String> extraSeeds = new LinkedHashSet<>(Arrays.asList("kaleidoscope_cookery:tea_seed"));
    /** 씨앗으로 잘못 판별된 아이템을 제외. */
    public final Set<String> notSeeds = new LinkedHashSet<>();

    /** 작물로 잘못 판별된 블록을 제외 (블록 ID). */
    public final Set<String> notCrops = new LinkedHashSet<>();

    /** 다 자란 작물을 우클릭하면 수확하고, 인벤토리의 상점 씨앗 1개를 써서 바로 다시 심는다. */
    public boolean rightClickHarvest = true;
    /** 괭이로 우클릭 수확할 때의 범위: 1 = 3x3, 2 = 5x5, 3 = 7x7. 적히지 않은 괭이는 한 칸. 웅크리면 한 칸만. */
    public final Map<String, Integer> harvestSweep = new LinkedHashMap<>();
    /** 괭이로 수확할 때 작물 하나당 닳는 내구도. */
    public int harvestToolDamage = 1;
    /** 우클릭 수확을 적용하지 않는 작물 (블록 ID). */
    public final Set<String> noRightClickHarvest = new LinkedHashSet<>(Arrays.asList(
            "kaleidoscope_cookery:rice_crop"));
    /** 다른 모드(낫, 자동 수확 상자, 주민 등)가 씨앗 없이 다시 심어 놓은 싹을 치운다. */
    public boolean removeUnlicensedSprouts = true;

    public boolean starsEnabled = true;
    /** 1별/2별/3별이 나올 확률(%). 합이 100. */
    public double[] starChances = {65, 27, 8};
    /**
     * 별 등급별 매입가 배율 (기준 = 가격표 가격의 50%).
     * 기본값 1.8 / 2.2 / 2.8 은 가격표 가격의 90% / 110% / 140% 이고, 확률을 감안한 평균은 가격표 가격과 거의 같다.
     * 별이 없는 작물(상점에서 산 것, 자동 수확물)은 1.0 = 50%.
     */
    public double[] starMultipliers = {1.8, 2.2, 2.8};

    public FarmConfig() {
        harvestSweep.put("minecraft:iron_hoe", 1);
        harvestSweep.put("minecraft:golden_hoe", 1);
        harvestSweep.put("minecraft:diamond_hoe", 2);
        harvestSweep.put("minecraft:netherite_hoe", 3);
    }

    private static FarmConfig instance;

    public static synchronized FarmConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public static synchronized FarmConfig reload() {
        instance = load();
        return instance;
    }

    private static void readSet(JsonObject o, String key, Set<String> target) {
        if (o.has(key) && o.get(key).isJsonArray()) {
            target.clear();
            for (JsonElement e : o.getAsJsonArray(key)) {
                target.add(e.getAsString());
            }
        }
    }

    private static JsonArray arr(Iterable<String> it) {
        JsonArray a = new JsonArray();
        for (String s : it) {
            a.add(s);
        }
        return a;
    }

    private static FarmConfig load() {
        FarmConfig c = new FarmConfig();
        Path p = FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("farming.json");
        try {
            if (Files.exists(p)) {
                JsonObject o = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
                if (o.has("removeSeedDrops")) c.removeSeedDrops = o.get("removeSeedDrops").getAsBoolean();
                if (o.has("requireShopSeeds")) c.requireShopSeeds = o.get("requireShopSeeds").getAsBoolean();
                if (o.has("blockEatingShopSeeds")) c.blockEatingShopSeeds = o.get("blockEatingShopSeeds").getAsBoolean();
                if (o.has("starsEnabled")) c.starsEnabled = o.get("starsEnabled").getAsBoolean();
                if (o.has("autoListMissingSeeds")) c.autoListMissingSeeds = o.get("autoListMissingSeeds").getAsBoolean();
                if (o.has("autoSeedPrice")) c.autoSeedPrice = Math.max(1, o.get("autoSeedPrice").getAsInt());
                if (o.has("autoDualSeedFactor")) c.autoDualSeedFactor = Math.max(0.1, o.get("autoDualSeedFactor").getAsDouble());
                readSet(o, "dualPurpose", c.dualPurpose);
                readSet(o, "extraSeeds", c.extraSeeds);
                readSet(o, "notSeeds", c.notSeeds);
                readSet(o, "notCrops", c.notCrops);
                readSet(o, "noRightClickHarvest", c.noRightClickHarvest);
                if (o.has("rightClickHarvest")) c.rightClickHarvest = o.get("rightClickHarvest").getAsBoolean();
                if (o.has("harvestToolDamage")) c.harvestToolDamage = Math.max(0, o.get("harvestToolDamage").getAsInt());
                if (o.has("removeUnlicensedSprouts")) c.removeUnlicensedSprouts = o.get("removeUnlicensedSprouts").getAsBoolean();
                if (o.has("harvestSweep") && o.get("harvestSweep").isJsonObject()) {
                    c.harvestSweep.clear();
                    for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("harvestSweep").entrySet()) {
                        c.harvestSweep.put(e.getKey(), Math.max(0, Math.min(5, e.getValue().getAsInt())));
                    }
                }
                if (o.has("starChances") && o.getAsJsonArray("starChances").size() == 3) {
                    for (int i = 0; i < 3; i++) c.starChances[i] = Math.max(0, o.getAsJsonArray("starChances").get(i).getAsDouble());
                }
                if (o.has("starMultipliers") && o.getAsJsonArray("starMultipliers").size() == 3) {
                    for (int i = 0; i < 3; i++) c.starMultipliers[i] = Math.max(0, o.getAsJsonArray("starMultipliers").get(i).getAsDouble());
                }
            } else {
                JsonObject o = new JsonObject();
                JsonArray help = new JsonArray();
                help.add("removeSeedDrops: 작물·풀·상자 등에서 씨앗이 나오지 않게 함");
                help.add("requireShopSeeds: 상점에서 산 씨앗만 심을 수 있음 (조합해서 만든 씨앗, 수확한 당근·감자 등은 심을 수 없음)");
                help.add("dualPurpose: 작물 자체가 씨앗인 아이템. 상점의 '심기 전용' 품목만 심을 수 있음");
                help.add("autoListMissingSeeds: 가격표에 없는 모드 씨앗을 씨앗 상점에 자동으로 올림 (autoSeedPrice G, 작물 겸 씨앗은 작물 가격 x autoDualSeedFactor)");
                help.add("rightClickHarvest: 다 자란 작물을 우클릭하면 수확하고 인벤토리의 상점 씨앗으로 다시 심음 · harvestSweep: 괭이별 범위(1=3x3)");
                help.add("removeUnlicensedSprouts: 다른 모드나 주민이 씨앗 없이 다시 심은 싹을 치움");
                help.add("starChances: 1별/2별/3별 확률(%) · starMultipliers: 별 등급별 매입가 배율(기준 = 가격표 가격의 50%, 1.8 = 90%)");
                o.add("_설명", help);
                o.addProperty("removeSeedDrops", c.removeSeedDrops);
                o.addProperty("requireShopSeeds", c.requireShopSeeds);
                o.addProperty("blockEatingShopSeeds", c.blockEatingShopSeeds);
                o.addProperty("autoListMissingSeeds", c.autoListMissingSeeds);
                o.addProperty("autoSeedPrice", c.autoSeedPrice);
                o.addProperty("autoDualSeedFactor", c.autoDualSeedFactor);
                o.add("dualPurpose", arr(c.dualPurpose));
                o.add("extraSeeds", arr(c.extraSeeds));
                o.add("notSeeds", arr(c.notSeeds));
                o.add("notCrops", arr(c.notCrops));
                o.addProperty("rightClickHarvest", c.rightClickHarvest);
                JsonObject sweep = new JsonObject();
                c.harvestSweep.forEach(sweep::addProperty);
                o.add("harvestSweep", sweep);
                o.addProperty("harvestToolDamage", c.harvestToolDamage);
                o.add("noRightClickHarvest", arr(c.noRightClickHarvest));
                o.addProperty("removeUnlicensedSprouts", c.removeUnlicensedSprouts);
                o.addProperty("starsEnabled", c.starsEnabled);
                JsonArray ch = new JsonArray();
                JsonArray mu = new JsonArray();
                for (int i = 0; i < 3; i++) {
                    ch.add(c.starChances[i]);
                    mu.add(c.starMultipliers[i]);
                }
                o.add("starChances", ch);
                o.add("starMultipliers", mu);
                Files.createDirectories(p.getParent());
                Files.writeString(p, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(o), StandardCharsets.UTF_8);
            }
        } catch (Exception ex) {
            RtShop.LOGGER.error("[rtshop] farming.json 을 읽지 못해 기본값을 사용합니다.", ex);
        }
        return c;
    }
}

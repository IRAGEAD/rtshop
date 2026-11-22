package com.rtshop.enhance;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * config/rtshop/enhance.json : 장비 강화 설정.
 *  - levels: 강화 단계(1~11)마다 결과 확률(성공·유지·손상·파괴, 합 100)과 재료(Gold + 아이템).
 *  - groups: "아무거나 섞어서" 쓸 수 있는 재료 묶음 (Mining Master 보석을 희귀도별로).
 *  - weaponBonus / armorBonus: 강화 단계별 누적 능력치 증가(%).
 * 설치되지 않은 모드의 재료는 빼고 계산한다 (그 재료가 없어도 강화할 수 있음).
 */
public final class EnhanceConfig {
    public static final int MAX_LEVEL = 11;
    public static final int CONFIG_VERSION = 2;
    /** 1판 기본 확률 (그대로면 2판 확률로 바꾼다). */
    private static final double[][] OLD_ODDS = {{}, {90, 9, 1, 0}, {83, 9, 8, 0}, {75, 8.5, 16.5, 0}, {66, 8, 23, 3}, {56, 7.5, 28.5, 8},
            {46, 7, 34, 13}, {37, 6.5, 39, 17.5}, {29, 6, 43, 22}, {22, 5, 46, 27}, {15, 4, 49, 32}, {9, 3, 51, 37}};

    public static final class Material {
        /** 아이템 ID 또는 "#묶음이름". */
        public String id;
        public int count;

        Material(String id, int count) {
            this.id = id;
            this.count = count;
        }
    }

    public static final class Level {
        public int level;
        public double success, maintain, damage, destroy;
        public long gold;
        public final List<Material> materials = new ArrayList<>();
    }

    public final Level[] levels = new Level[MAX_LEVEL + 1];
    public final Map<String, List<String>> groups = new LinkedHashMap<>();
    public final Map<String, String> groupNames = new LinkedHashMap<>();
    /** 단계별 누적 % (0 단계 = 0). */
    public double[] weaponBonus = {0, 3, 6, 9, 13, 17, 22, 28, 35, 43, 52, 62};
    public double[] armorBonus = {0, 4, 8, 12, 16, 21, 26, 32, 38, 45, 53, 62};
    /** 이 단계부터 방어구에 방어 강도(toughness)가 단계마다 이만큼 붙는다. */
    public int toughnessFrom = 6;
    public double toughnessPerLevel = 0.5;
    /** 손상 결과일 때 내려가는 단계 수. */
    public int damageDrop = 1;
    /** 이 단계 이상 성공하면 서버 전체에 알리고 특별 연출을 보인다. */
    public int broadcastFrom = 7;

    private static Level lv(int n, double s, double m, double d, double x, long gold, Object... mats) {
        Level l = new Level();
        l.level = n;
        l.success = s;
        l.maintain = m;
        l.damage = d;
        l.destroy = x;
        l.gold = gold;
        for (int i = 0; i + 1 < mats.length; i += 2) {
            l.materials.add(new Material((String) mats[i], (Integer) mats[i + 1]));
        }
        return l;
    }

    public void defaults() {
        groups.clear();
        groupNames.clear();
        // Mining Master 보석: 광맥 수·나오는 곳으로 나눈 희귀도
        // 흔함(한 청크에 3번, 지상 바이옴) → 보통(2번) → 네더(1번) → 희귀(신성한 녹주석·운석에서만 나오는 공기 공작석)
        groups.put("gem_common", List.of("miningmaster:ice_sapphire", "miningmaster:spirit_garnet", "miningmaster:dive_aquamarine",
                "miningmaster:spider_kunzite"));
        groups.put("gem_fine", List.of("miningmaster:fire_ruby", "miningmaster:haste_peridot", "miningmaster:lucky_citrine",
                "miningmaster:unbreaking_iolite"));
        groups.put("gem_nether", List.of("miningmaster:heart_rhodonite", "miningmaster:power_pyrite", "miningmaster:kinetic_opal"));
        groups.put("gem_rare", List.of("miningmaster:divine_beryl", "miningmaster:air_malachite"));
        groupNames.put("gem_common", "흔한 보석");
        groupNames.put("gem_fine", "고운 보석");
        groupNames.put("gem_nether", "네더 보석");
        groupNames.put("gem_rare", "희귀 보석");
        // 확률: 성공·유지는 단계가 오를수록 내려가고, 손상·파괴는 올라간다. +4 부터 파괴 가능
        // 2판: +4 부터 성공률을 크게 낮추고 유지 비중을 높였다. +4 부터 파괴 가능
        levels[1] = lv(1, 90, 10, 0, 0, 300, "#gem_common", 1, "spelunkery:salt", 2);
        levels[2] = lv(2, 82, 15, 3, 0, 600, "#gem_common", 2, "spelunkery:salt", 3);
        levels[3] = lv(3, 72, 20, 8, 0, 1200, "#gem_common", 3, "spelunkery:raw_magnetite", 2);
        levels[4] = lv(4, 48, 36, 12, 4, 2000, "#gem_fine", 1, "#gem_common", 2, "spelunkery:raw_magnetite", 3);
        levels[5] = lv(5, 36, 38, 17, 9, 3500, "#gem_fine", 2, "spelunkery:cinnabar", 2);
        levels[6] = lv(6, 26, 38, 22, 14, 6000, "#gem_fine", 3, "spelunkery:cinnabar", 3);
        levels[7] = lv(7, 18, 37, 27, 18, 10000, "#gem_nether", 1, "#gem_fine", 2, "spelunkery:sulfur", 4);
        levels[8] = lv(8, 12, 36, 30, 22, 16000, "#gem_nether", 2, "spelunkery:rough_diamond", 1);
        levels[9] = lv(9, 8, 35, 31, 26, 25000, "#gem_nether", 3, "spelunkery:rough_diamond", 2);
        levels[10] = lv(10, 5, 33, 32, 30, 40000, "#gem_rare", 1, "#gem_nether", 2, "spelunkery:rough_emerald", 2);
        levels[11] = lv(11, 3, 31, 32, 34, 65000, "#gem_rare", 2, "#gem_nether", 3, "spelunkery:rough_diamond", 2,
                "spelunkery:rough_emerald", 2);
    }

    // ------------------------------------------------------------------ 재료 확인

    static Item item(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null || !ForgeRegistries.ITEMS.containsKey(rl)) {
            return null;
        }
        Item it = ForgeRegistries.ITEMS.getValue(rl);
        return it == null || it == Items.AIR ? null : it;
    }

    /** 이 재료로 쓸 수 있는 (설치된) 아이템들. 하나도 없으면 빈 목록 = 그 재료는 건너뜀. */
    public List<Item> itemsOf(Material m) {
        List<Item> out = new ArrayList<>();
        if (m.id.startsWith("#")) {
            for (String s : groups.getOrDefault(m.id.substring(1), List.of())) {
                Item it = item(s);
                if (it != null) {
                    out.add(it);
                }
            }
        } else {
            Item it = item(m.id);
            if (it != null) {
                out.add(it);
            }
        }
        return out;
    }

    public Level level(int target) {
        return target >= 1 && target <= MAX_LEVEL ? levels[target] : null;
    }

    // ------------------------------------------------------------------ 읽기 / 쓰기

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        JsonArray help = new JsonArray();
        help.add("장비 강화 설정. 강화 모루(대장장이에게서 삼)에 장비를 올리고 망치로 두드려 강화합니다.");
        help.add("levels: 목표 단계(1~11)마다 success(성공)·maintain(유지)·damage(손상: 단계 하락)·destroy(파괴) 확률(%), gold(드는 Gold), materials(재료).");
        help.add("materials: {item: 아이템 ID 또는 #묶음, count}. 묶음은 groups 의 아이템 중 아무거나 섞어서 씁니다. 설치되지 않은 모드의 재료는 빼고 계산합니다.");
        help.add("손상 방지권은 손상을 유지로, 파괴 방지권은 파괴를 손상으로 바꿉니다 (강화 보호권은 둘 다). 방지권은 시도할 때마다 1장 씁니다.");
        help.add("weaponBonus/armorBonus: 단계별 누적 증가율(%). 무기 = 공격력, 방어구 = 방어력. toughnessFrom 단계부터 방어 강도 +toughnessPerLevel.");
        o.add("_help", help);
        o.addProperty("configVersion", CONFIG_VERSION);
        o.addProperty("damageDrop", damageDrop);
        o.addProperty("broadcastFrom", broadcastFrom);
        JsonObject g = new JsonObject();
        for (Map.Entry<String, List<String>> e : groups.entrySet()) {
            JsonObject go = new JsonObject();
            go.addProperty("name", groupNames.getOrDefault(e.getKey(), e.getKey()));
            JsonArray a = new JsonArray();
            e.getValue().forEach(a::add);
            go.add("items", a);
            g.add(e.getKey(), go);
        }
        o.add("groups", g);
        JsonArray ls = new JsonArray();
        for (int i = 1; i <= MAX_LEVEL; i++) {
            Level l = levels[i];
            JsonObject lo = new JsonObject();
            lo.addProperty("level", i);
            lo.addProperty("success", l.success);
            lo.addProperty("maintain", l.maintain);
            lo.addProperty("damage", l.damage);
            lo.addProperty("destroy", l.destroy);
            lo.addProperty("gold", l.gold);
            JsonArray ma = new JsonArray();
            for (Material m : l.materials) {
                JsonObject mo = new JsonObject();
                mo.addProperty("item", m.id);
                mo.addProperty("count", m.count);
                ma.add(mo);
            }
            lo.add("materials", ma);
            ls.add(lo);
        }
        o.add("levels", ls);
        JsonArray wb = new JsonArray();
        for (double v : weaponBonus) {
            wb.add(v);
        }
        o.add("weaponBonus", wb);
        JsonArray ab = new JsonArray();
        for (double v : armorBonus) {
            ab.add(v);
        }
        o.add("armorBonus", ab);
        o.addProperty("toughnessFrom", toughnessFrom);
        o.addProperty("toughnessPerLevel", toughnessPerLevel);
        return o;
    }

    public static EnhanceConfig fromJson(JsonObject o) {
        EnhanceConfig c = new EnhanceConfig();
        c.defaults();
        if (o.has("broadcastFrom")) c.broadcastFrom = Math.max(1, Math.min(MAX_LEVEL + 1, o.get("broadcastFrom").getAsInt()));
        if (o.has("damageDrop")) c.damageDrop = Math.max(0, Math.min(MAX_LEVEL, o.get("damageDrop").getAsInt()));
        if (o.has("groups")) {
            c.groups.clear();
            c.groupNames.clear();
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("groups").entrySet()) {
                JsonObject go = e.getValue().getAsJsonObject();
                List<String> items = new ArrayList<>();
                for (JsonElement x : go.getAsJsonArray("items")) {
                    items.add(x.getAsString().trim());
                }
                c.groups.put(e.getKey(), items);
                c.groupNames.put(e.getKey(), go.has("name") ? go.get("name").getAsString() : e.getKey());
            }
        }
        if (o.has("levels")) {
            for (JsonElement el : o.getAsJsonArray("levels")) {
                JsonObject lo = el.getAsJsonObject();
                int n = lo.get("level").getAsInt();
                if (n < 1 || n > MAX_LEVEL) {
                    continue;
                }
                Level l = new Level();
                l.level = n;
                l.success = Math.max(0, lo.get("success").getAsDouble());
                l.maintain = Math.max(0, lo.get("maintain").getAsDouble());
                l.damage = Math.max(0, lo.get("damage").getAsDouble());
                l.destroy = Math.max(0, lo.get("destroy").getAsDouble());
                if (l.success + l.maintain + l.damage + l.destroy <= 0) {
                    throw new IllegalArgumentException("+" + n + " 강화 확률이 모두 0 입니다");
                }
                l.gold = Math.max(0, lo.has("gold") ? lo.get("gold").getAsLong() : 0);
                if (lo.has("materials")) {
                    for (JsonElement me : lo.getAsJsonArray("materials")) {
                        JsonObject mo = me.getAsJsonObject();
                        l.materials.add(new Material(mo.get("item").getAsString().trim(), Math.max(1, mo.get("count").getAsInt())));
                    }
                }
                c.levels[n] = l;
            }
        }
        if (o.has("weaponBonus")) c.weaponBonus = doubles(o.getAsJsonArray("weaponBonus"), c.weaponBonus);
        if (o.has("armorBonus")) c.armorBonus = doubles(o.getAsJsonArray("armorBonus"), c.armorBonus);
        if (o.has("toughnessFrom")) c.toughnessFrom = o.get("toughnessFrom").getAsInt();
        if (o.has("toughnessPerLevel")) c.toughnessPerLevel = o.get("toughnessPerLevel").getAsDouble();
        return c;
    }

    private static double[] doubles(JsonArray a, double[] def) {
        double[] out = def.clone();
        for (int i = 0; i < Math.min(a.size(), out.length); i++) {
            out[i] = a.get(i).getAsDouble();
        }
        return out;
    }

    private static EnhanceConfig instance;
    /** 클라이언트: 서버가 보낸 설정 (없으면 기본값). */
    private static EnhanceConfig synced;

    public static synchronized EnhanceConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public static synchronized void reload() {
        instance = load();
    }

    public static synchronized EnhanceConfig client() {
        if (synced == null) {
            synced = new EnhanceConfig();
            synced.defaults();
        }
        return synced;
    }

    public static synchronized void setClient(String json) {
        try {
            synced = fromJson(JsonParser.parseString(json).getAsJsonObject());
        } catch (Exception e) {
            RtShop.LOGGER.warn("[rtshop] 서버의 강화 설정을 읽지 못했습니다: {}", e.getMessage());
        }
    }

    public static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("enhance.json");
    }

    public static void save(EnhanceConfig c) throws java.io.IOException {
        Path p = path();
        Files.createDirectories(p.getParent());
        Files.writeString(p, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(c.toJson()), StandardCharsets.UTF_8);
    }

    /** OP 편집기에서 보낸 설정을 검사하고 저장한 뒤 적용한다. 문제가 있으면 예외. */
    public static synchronized void saveFromEditor(String json) throws Exception {
        EnhanceConfig c = fromJson(JsonParser.parseString(json).getAsJsonObject());
        for (int i = 1; i <= MAX_LEVEL; i++) {
            if (c.levels[i] == null) throw new IllegalArgumentException("+" + i + " 단계가 없습니다");
        }
        save(c);
        instance = c;
    }

    private static EnhanceConfig load() {
        Path p = path();
        try {
            if (Files.exists(p)) {
                JsonObject o = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
                EnhanceConfig c = fromJson(o);
                int ver = o.has("configVersion") ? o.get("configVersion").getAsInt() : 1;
                if (ver < CONFIG_VERSION) {
                    EnhanceConfig d = new EnhanceConfig();
                    d.defaults();
                    int changed = 0;
                    for (int i = 1; i <= MAX_LEVEL; i++) {
                        Level l = c.levels[i];
                        double[] old = OLD_ODDS[i];
                        if (l != null && l.success == old[0] && l.maintain == old[1] && l.damage == old[2] && l.destroy == old[3]) {
                            l.success = d.levels[i].success;
                            l.maintain = d.levels[i].maintain;
                            l.damage = d.levels[i].damage;
                            l.destroy = d.levels[i].destroy;
                            changed++;
                        }
                    }
                    save(c);
                    RtShop.LOGGER.info("[rtshop] enhance.json 을 2판으로 올렸습니다 (기본 확률이던 단계 {}곳을 새 확률로)", changed);
                }
                return c;
            }
            EnhanceConfig c = new EnhanceConfig();
            c.defaults();
            Files.createDirectories(p.getParent());
            Files.writeString(p, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(c.toJson()), StandardCharsets.UTF_8);
            return c;
        } catch (Exception e) {
            RtShop.LOGGER.error("[rtshop] enhance.json 을 읽지 못해 기본 강화 설정을 씁니다: {}", e.getMessage());
            EnhanceConfig c = new EnhanceConfig();
            c.defaults();
            return c;
        }
    }
}

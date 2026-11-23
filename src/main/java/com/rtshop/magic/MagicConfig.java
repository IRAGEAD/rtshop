package com.rtshop.magic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import com.rtshop.gacha.GachaConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
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
 * 마법 뽑기 설정 (config/rtshop/magic_gacha.json).
 * <ul>
 * <li>스킬북 뽑기: 분야(공격·방어·회피·패시브·이동)를 골라 뽑는 '선택 뽑기'(비쌈)와 모든 분야에서 뽑는 '무작위 뽑기'(쌈).
 *     등급·확률은 무기 뽑기와 같고(일반~전설·야마토), 야마토 등급은 공격 분야(와 무작위)에만 있다.</li>
 * <li>인챈트 재설정: 무기에 붙은 인챈트를 모두 지우고 하나를 새로 붙인다 (무기 하나에 인챈트 하나).</li>
 * <li>목록·확률·티켓 수는 게임 안 편집기(OP)나 이 파일로 고친다. 처음에는 이 세계의 스킬북을 찾아 자동으로 채운다.</li>
 * </ul>
 * 뽑기 계산은 무기 뽑기와 같은 엔진(GachaConfig + Gacha.pull)을 쓴다: 분야 = 뽑기 종류, 선택/무작위 = 뽑기 단계.
 */
public final class MagicConfig {
    public static final String[] CATS = {"attack", "guard", "dodge", "passive", "mover"};
    /** 무작위 뽑기의 목록 이름 (모든 분야를 합친 것). */
    public static final String RANDOM = "random";
    public static final String MODE_SELECT = "select", MODE_RANDOM = "random";
    public static final int CONFIG_VERSION = 1;

    /** 인챈트 한 칸. */
    public static final class Ench {
        public String id = "";
        public int level = 1;
        public String grade = "common";
        public int weight = 10;

        public JsonObject toJson() {
            JsonObject o = new JsonObject();
            o.addProperty("enchant", id);
            o.addProperty("level", level);
            o.addProperty("grade", grade);
            o.addProperty("weight", weight);
            return o;
        }

        static Ench fromJson(JsonObject o) {
            Ench e = new Ench();
            e.id = o.get("enchant").getAsString().trim();
            e.level = Math.max(1, Math.min(255, o.has("level") ? o.get("level").getAsInt() : 1));
            e.grade = o.has("grade") ? o.get("grade").getAsString() : "common";
            e.weight = Math.max(0, o.has("weight") ? o.get("weight").getAsInt() : 10);
            if (GachaConfig.gradeIndex(e.grade) < 0) throw new IllegalArgumentException("등급을 알 수 없습니다: " + e.grade);
            return e;
        }

        public Enchantment enchantment() {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            return rl == null ? null : ForgeRegistries.ENCHANTMENTS.getValue(rl);
        }
    }

    public int version = CONFIG_VERSION;
    /** 분야별 스킬북 목록 (무기 뽑기와 같은 칸 형식: item, nbt, grade, weight). */
    public final Map<String, List<GachaConfig.Entry>> pools = new LinkedHashMap<>();
    /** 등급별 확률 [선택, 무작위, 인챈트]. */
    public final double[] selectOdds = new double[GachaConfig.GRADES.length];
    public final double[] randomOdds = new double[GachaConfig.GRADES.length];
    public final double[] enchantOdds = new double[GachaConfig.GRADES.length];
    public final List<Ench> enchants = new ArrayList<>();
    /** 한 번 뽑을 때 쓰는 티켓 수. */
    public int selectCost = 1, randomCost = 1, rerollCost = 1;
    public String tenGuarantee = "rare";
    /** 천장: pityCount 번째 뽑기까지 pityGrade 이상이 안 나오면 그 등급을 확정으로 준다. shared = 선택·무작위 뽑기를 함께 센다. */
    public boolean pityOn = true, pityShared = true;
    public int pityCount = 80;
    public String pityGrade = "legendary";
    /** 오늘의 룬: blessHours 시간마다 분야 하나가 축복을 받아, 그 분야 선택 뽑기의 티켓 값이 blessDiscount % 싸지고 영웅 이상 확률이 blessBoost 배가 된다. */
    public boolean blessOn = true;
    public int blessDiscount = 20, blessHours = 24;
    public double blessBoost = 1.0;
    /** 인챈트를 다시 붙일 수 있는 장비: weapon(근접 무기)·bow·crossbow·trident·armor·tool. */
    public final List<String> rerollKinds = new ArrayList<>(List.of("weapon", "bow", "crossbow", "trident"));
    public String loadError;

    private static MagicConfig instance;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static synchronized MagicConfig get() {
        if (instance == null) instance = load(null);
        return instance;
    }

    public static synchronized void reload(MinecraftServer server) {
        instance = load(server);
    }

    public static synchronized void set(MagicConfig c) {
        instance = c;
    }

    public static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("magic_gacha.json");
    }

    public List<GachaConfig.Entry> pool(String cat) {
        return pools.computeIfAbsent(cat, k -> new ArrayList<>());
    }

    // ------------------------------------------------------------------ 기본값

    public void defaults() {
        // 무기 뽑기의 고급 뽑기와 같은 확률 (야마토 0.04%). 무작위 뽑기는 같은 확률에 값이 싸다.
        double[] high = {0, 69.96, 26.5, 3.2, 0.3, 0.04};
        System.arraycopy(high, 0, selectOdds, 0, high.length);
        System.arraycopy(high, 0, randomOdds, 0, high.length);
        double[] ench = {45, 33, 16, 5, 1, 0};
        System.arraycopy(ench, 0, enchantOdds, 0, ench.length);
        for (String c : CATS) pool(c);
    }

    /** 이 세계의 인챈트로 재설정 목록을 만든다 (저주 제외, 무기에 붙는 것만). 단계가 높을수록 높은 등급. */
    public void autoEnchants() {
        enchants.clear();
        for (var en : ForgeRegistries.ENCHANTMENTS.getEntries()) {
            Enchantment e = en.getValue();
            if (e.isCurse()) continue;
            EnchantmentCategory cat = e.category;
            boolean fits = cat == EnchantmentCategory.WEAPON || cat == EnchantmentCategory.BOW || cat == EnchantmentCategory.CROSSBOW
                    || cat == EnchantmentCategory.TRIDENT || cat == EnchantmentCategory.BREAKABLE;
            if (!fits && !e.canEnchant(new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD))) continue;
            String id = en.getKey().location().toString();
            int max = Math.max(1, e.getMaxLevel());
            for (int lv = 1; lv <= max; lv++) {
                Ench x = new Ench();
                x.id = id;
                x.level = lv;
                // 등급: 단계 비율 + 희귀도 + 보물 인챈트
                double frac = max == 1 ? 0.5 : (lv - 1) / (double) (max - 1);
                int g = frac < 0.34 ? 0 : frac < 0.67 ? 1 : 2;
                if (e.getRarity() == Enchantment.Rarity.RARE) g++;
                if (e.getRarity() == Enchantment.Rarity.VERY_RARE) g += 2;
                if (e.isTreasureOnly()) g++;
                if (lv == max && max >= 4) g++;
                x.grade = GachaConfig.GRADES[Math.min(4, g)];
                x.weight = 10;
                enchants.add(x);
            }
        }
    }

    // ------------------------------------------------------------------ 뽑기 엔진

    /** 무기 뽑기 엔진에 넘길 설정: 분야별 목록 + 무작위 목록(모든 분야), 단계 select/random. */
    public GachaConfig engine() {
        GachaConfig g = new GachaConfig();
        g.tiers.clear();
        GachaConfig.Tier s = new GachaConfig.Tier();
        s.id = MODE_SELECT;
        System.arraycopy(selectOdds, 0, s.weights, 0, selectOdds.length);
        GachaConfig.Tier r = new GachaConfig.Tier();
        r.id = MODE_RANDOM;
        System.arraycopy(randomOdds, 0, r.weights, 0, randomOdds.length);
        g.tiers.put(MODE_SELECT, s);
        g.tiers.put(MODE_RANDOM, r);
        g.tenGuarantee = tenGuarantee;
        List<GachaConfig.Entry> all = g.pool(RANDOM);
        for (String c : CATS) {
            g.pool(c).addAll(pool(c));
            all.addAll(pool(c));
        }
        return g;
    }

    /** 축복받은 분야의 선택 뽑기용 엔진 (영웅 이상 확률 blessBoost 배). 축복이 아니면 보통 엔진. */
    public GachaConfig engineFor(String mode, String cat, long nowMs) {
        GachaConfig g = engine();
        if (MODE_SELECT.equals(mode) && blessBoost > 1.0 && cat.equals(blessedCat(nowMs))) {
            double[] w = g.tiers.get(MODE_SELECT).weights;
            for (int i = 3; i < w.length; i++) w[i] *= blessBoost;
        }
        return g;
    }

    private long period(long nowMs) {
        return Math.floorDiv(nowMs + java.util.TimeZone.getDefault().getOffset(nowMs), Math.max(1, blessHours) * 3_600_000L);
    }

    /** 지금 축복받은 분야 (꺼져 있으면 ""). 주기마다 바뀌고, 다섯 주기 안에 모든 분야가 한 번씩 돈다. */
    public String blessedCat(long nowMs) {
        if (!blessOn) return "";
        long p = period(nowMs), cyc = Math.floorDiv(p, 5L);
        long h = cyc * 0x9E3779B97F4A7C15L;
        h ^= h >>> 29;
        int step = 1 + (int) Math.floorMod(h, 4L), off = (int) Math.floorMod(h >>> 7, 5L);
        return CATS[(int) Math.floorMod(Math.floorMod(p, 5L) * step + off, 5L)];
    }

    /** 다음 룬으로 바뀔 때까지 남은 초. */
    public long blessEndsIn(long nowMs) {
        long len = Math.max(1, blessHours) * 3_600_000L;
        return ((period(nowMs) + 1) * len - (nowMs + java.util.TimeZone.getDefault().getOffset(nowMs))) / 1000L;
    }

    /** n 번 뽑을 때 쓰는 티켓 수 (축복받은 분야의 선택 뽑기는 할인, 적어도 1장). */
    public int cost(String mode, String cat, int n, long nowMs) {
        boolean select = MODE_SELECT.equals(mode);
        int base = (select ? selectCost : randomCost) * n;
        if (select && blessDiscount > 0 && cat.equals(blessedCat(nowMs))) {
            return Math.max(1, (int) Math.ceil(base * (100 - blessDiscount) / 100.0 - 1e-9));
        }
        return base;
    }

    /** 야마토 등급이 공격 분야 밖에 있으면 공격 분야로 옮긴다 (야마토는 공격 분야에만). */
    public void normalize() {
        for (String c : CATS) {
            if (c.equals("attack")) continue;
            for (GachaConfig.Entry e : pool(c)) {
                if (GachaConfig.gradeIndex(e.grade) == GachaConfig.YAMATO) e.grade = "legendary";
            }
        }
        for (Ench e : enchants) if (GachaConfig.gradeIndex(e.grade) == GachaConfig.YAMATO) e.grade = "legendary";
    }

    // ------------------------------------------------------------------ 저장 · 불러오기

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("_help", "마법 뽑기 설정. pools: 분야(attack 공격·guard 방어·dodge 회피·passive 패시브·mover 이동)별 스킬북 목록 "
                + "(item, nbt, grade 등급, weight 같은 등급 안 가중치). odds: 등급별 확률 (common, uncommon, rare, epic, legendary, yamato 순). "
                + "야마토 등급은 공격 분야에만 둘 수 있다. enchants: 인챈트 재설정 목록 (enchant, level, grade, weight). "
                + "costs: 한 번에 쓰는 티켓 수. pity(천장): count 번째까지 grade 이상이 안 나오면 확정, shared=선택·무작위를 함께 센다. "
                + "blessing(오늘의 룬): periodHours 시간마다 바뀌는 분야의 선택 뽑기가 discountPercent % 싸지고 영웅 이상 확률이 oddsBoost 배. 게임 안에서는 OP 가 마법 뽑기 창의 [편집] 으로 고친다. 고친 뒤 /rtshop reload.");
        o.addProperty("configVersion", version);
        o.addProperty("tenGuarantee", tenGuarantee);
        JsonObject odds = new JsonObject();
        odds.add("select", arr(selectOdds));
        odds.add("random", arr(randomOdds));
        odds.add("enchant", arr(enchantOdds));
        o.add("odds", odds);
        JsonObject costs = new JsonObject();
        costs.addProperty("select", selectCost);
        costs.addProperty("random", randomCost);
        costs.addProperty("reroll", rerollCost);
        o.add("costs", costs);
        JsonObject pity = new JsonObject();
        pity.addProperty("enabled", pityOn);
        pity.addProperty("count", pityCount);
        pity.addProperty("grade", pityGrade);
        pity.addProperty("shared", pityShared);
        o.add("pity", pity);
        JsonObject bl = new JsonObject();
        bl.addProperty("enabled", blessOn);
        bl.addProperty("discountPercent", blessDiscount);
        bl.addProperty("periodHours", blessHours);
        bl.addProperty("oddsBoost", blessBoost);
        o.add("blessing", bl);
        JsonArray kinds = new JsonArray();
        rerollKinds.forEach(kinds::add);
        o.add("rerollKinds", kinds);
        JsonObject ps = new JsonObject();
        for (String c : CATS) {
            JsonArray a = new JsonArray();
            for (GachaConfig.Entry e : pool(c)) a.add(e.toJson());
            ps.add(c, a);
        }
        o.add("pools", ps);
        JsonArray en = new JsonArray();
        for (Ench e : enchants) en.add(e.toJson());
        o.add("enchants", en);
        return o;
    }

    private static JsonArray arr(double[] d) {
        JsonArray a = new JsonArray();
        for (double v : d) a.add(v);
        return a;
    }

    private static void read(JsonArray a, double[] out) {
        for (int i = 0; i < out.length && i < a.size(); i++) out[i] = Math.max(0, a.get(i).getAsDouble());
    }

    public static MagicConfig fromJson(JsonObject o) {
        MagicConfig c = new MagicConfig();
        c.defaults();
        c.version = o.has("configVersion") ? o.get("configVersion").getAsInt() : 1;
        if (o.has("tenGuarantee")) c.tenGuarantee = o.get("tenGuarantee").getAsString().trim();
        if (o.has("odds")) {
            JsonObject od = o.getAsJsonObject("odds");
            if (od.has("select")) read(od.getAsJsonArray("select"), c.selectOdds);
            if (od.has("random")) read(od.getAsJsonArray("random"), c.randomOdds);
            if (od.has("enchant")) read(od.getAsJsonArray("enchant"), c.enchantOdds);
        }
        if (o.has("costs")) {
            JsonObject co = o.getAsJsonObject("costs");
            if (co.has("select")) c.selectCost = Math.max(1, Math.min(64, co.get("select").getAsInt()));
            if (co.has("random")) c.randomCost = Math.max(1, Math.min(64, co.get("random").getAsInt()));
            if (co.has("reroll")) c.rerollCost = Math.max(1, Math.min(64, co.get("reroll").getAsInt()));
        }
        if (o.has("pity")) {
            JsonObject pi = o.getAsJsonObject("pity");
            if (pi.has("enabled")) c.pityOn = pi.get("enabled").getAsBoolean();
            if (pi.has("count")) c.pityCount = Math.max(1, Math.min(100000, pi.get("count").getAsInt()));
            if (pi.has("grade") && GachaConfig.gradeIndex(pi.get("grade").getAsString()) >= 0) c.pityGrade = pi.get("grade").getAsString();
            if (pi.has("shared")) c.pityShared = pi.get("shared").getAsBoolean();
        }
        if (o.has("blessing")) {
            JsonObject bl = o.getAsJsonObject("blessing");
            if (bl.has("enabled")) c.blessOn = bl.get("enabled").getAsBoolean();
            if (bl.has("discountPercent")) c.blessDiscount = Math.max(0, Math.min(90, bl.get("discountPercent").getAsInt()));
            if (bl.has("periodHours")) c.blessHours = Math.max(1, Math.min(720, bl.get("periodHours").getAsInt()));
            if (bl.has("oddsBoost")) c.blessBoost = Math.max(1.0, Math.min(10.0, bl.get("oddsBoost").getAsDouble()));
        }
        if (o.has("rerollKinds")) {
            c.rerollKinds.clear();
            for (JsonElement e : o.getAsJsonArray("rerollKinds")) c.rerollKinds.add(e.getAsString());
        }
        if (o.has("pools")) {
            JsonObject ps = o.getAsJsonObject("pools");
            for (String cat : CATS) {
                List<GachaConfig.Entry> l = c.pool(cat);
                l.clear();
                if (ps.has(cat)) for (JsonElement e : ps.getAsJsonArray(cat)) l.add(GachaConfig.Entry.fromJson(e.getAsJsonObject()));
            }
        }
        if (o.has("enchants")) {
            for (JsonElement e : o.getAsJsonArray("enchants")) c.enchants.add(Ench.fromJson(e.getAsJsonObject()));
        }
        c.normalize();
        return c;
    }

    private static MagicConfig load(MinecraftServer server) {
        Path p = path();
        try {
            if (Files.exists(p)) {
                MagicConfig c = fromJson(JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject());
                // 서버가 켜졌는데 목록이 비어 있으면 (스킬 모드를 나중에 넣은 경우 등) 채운다
                if (server != null && c.isEmpty()) {
                    MagicSkills.autoFill(c);
                    if (c.enchants.isEmpty()) c.autoEnchants();
                    c.save();
                }
                return c;
            }
        } catch (Exception e) {
            RtShop.LOGGER.error("[rtshop] magic_gacha.json 을 읽지 못했습니다: {}", e.getMessage());
            MagicConfig c = new MagicConfig();
            c.defaults();
            c.loadError = e.getMessage();
            return c;
        }
        MagicConfig c = new MagicConfig();
        c.defaults();
        if (server == null) return c; // 서버가 켜질 때 채운다
        MagicSkills.autoFill(c);
        c.autoEnchants();
        c.save();
        return c;
    }

    public boolean isEmpty() {
        for (String cat : CATS) if (!pool(cat).isEmpty()) return false;
        return true;
    }

    public void save() {
        try {
            Files.createDirectories(path().getParent());
            Files.writeString(path(), GSON.toJson(toJson()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            RtShop.LOGGER.error("[rtshop] magic_gacha.json 을 저장하지 못했습니다: {}", e.getMessage());
        }
    }
}

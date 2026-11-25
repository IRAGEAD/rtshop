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
 * <li>(예전의 '오늘의 룬'은 없앴다. 옛 파일의 blessing 항목은 읽지 않고 버린다.)</li>
 * <li>목록·확률·티켓 수는 게임 안 편집기(OP)나 이 파일로 고친다. 처음에는 이 세계의 스킬북을 찾아 자동으로 채운다.</li>
 * </ul>
 * 뽑기 계산은 무기 뽑기와 같은 엔진(GachaConfig + Gacha.pull)을 쓴다: 분야 = 뽑기 종류, 선택/무작위 = 뽑기 단계.
 */
public final class MagicConfig {
    public static final String[] CATS = {"attack", "guard", "dodge", "passive", "mover"};
    /** 무작위 뽑기의 목록 이름 (모든 분야를 합친 것). */
    public static final String RANDOM = "random";
    public static final String MODE_SELECT = "select", MODE_RANDOM = "random";
    public static final int CONFIG_VERSION = 2;

    /** 뽑기 실패 (일반 등급 자리): 스킬북 뽑기에서 이 칸이 뽑히면 아무것도 나오지 않는다. 목록 파일에는 적히지 않는다. */
    public static final GachaConfig.Entry FAIL = failEntry();
    /** 인챈트 재설정 실패 (일반 등급 자리): 주문서만 쓰이고 장비는 그대로다. */
    public static final Ench FAIL_ENCH = new Ench();

    private static GachaConfig.Entry failEntry() {
        GachaConfig.Entry e = new GachaConfig.Entry();
        e.item = "minecraft:barrier";
        e.grade = "common";
        e.weight = 1;
        return e;
    }

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
    public boolean pityOn = false, pityShared = true; // 3.49: 천장은 없앴다 (값은 읽지도 쓰지도 않는다)
    public int pityCount = 80;
    public String pityGrade = "legendary";
    /** 인챈트를 다시 붙일 수 있는 장비: weapon(근접 무기)·bow·crossbow·trident·armor·tool. */
    public final List<String> rerollKinds = new ArrayList<>(List.of("weapon", "bow", "crossbow", "trident", "armor", "tool"));
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
        defaultOdds();
        for (String c : CATS) pool(c);
    }

    /** 기본 확률. 첫 칸(일반 등급 자리)은 '실패': 스킬북은 아무것도 나오지 않고, 인챈트는 주문서만 쓰인다. */
    public void defaultOdds() {
        double[] book = {40, 41.98, 15.9, 1.92, 0.18, 0.02};
        System.arraycopy(book, 0, selectOdds, 0, book.length);
        System.arraycopy(book, 0, randomOdds, 0, book.length);
        double[] ench = {50, 32, 13, 4.3, 0.7, 0};
        System.arraycopy(ench, 0, enchantOdds, 0, ench.length);
    }

    private static boolean fitsReroll(Enchantment e) {
        EnchantmentCategory cat = e.category;
        return cat == EnchantmentCategory.WEAPON || cat == EnchantmentCategory.BOW || cat == EnchantmentCategory.CROSSBOW
                || cat == EnchantmentCategory.TRIDENT || cat == EnchantmentCategory.BREAKABLE || cat == EnchantmentCategory.DIGGER
                || cat == EnchantmentCategory.ARMOR || cat == EnchantmentCategory.ARMOR_HEAD || cat == EnchantmentCategory.ARMOR_CHEST
                || cat == EnchantmentCategory.ARMOR_LEGS || cat == EnchantmentCategory.ARMOR_FEET || cat == EnchantmentCategory.WEARABLE
                || e.canEnchant(new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD));
    }

    /** 인챈트 한 단계의 등급 (고급~전설): 단계 비율 + 희귀도 + 보물 인챈트 + 가장 높은 단계. */
    private static String enchGrade(Enchantment e, int lv) {
        int max = Math.max(1, e.getMaxLevel());
        double frac = max == 1 ? 0.5 : (lv - 1) / (double) (max - 1);
        int g = 1 + (frac < 0.34 ? 0 : frac < 0.67 ? 1 : 2);
        if (e.getRarity() == Enchantment.Rarity.RARE) g++;
        if (e.getRarity() == Enchantment.Rarity.VERY_RARE) g += 2;
        if (e.isTreasureOnly()) g++;
        if (lv == max && max >= 4) g++;
        return GachaConfig.GRADES[Math.min(4, g)];
    }

    /** 목록에 없는 인챈트(방어구·도구용 등)를 덧붙인다. 넣은 수. */
    public int addMissingEnchants() {
        java.util.Set<String> have = new java.util.HashSet<>();
        for (Ench e : enchants) have.add(e.id);
        int n = 0;
        for (var en : ForgeRegistries.ENCHANTMENTS.getEntries()) {
            Enchantment e = en.getValue();
            String id = en.getKey().location().toString();
            if (e.isCurse() || have.contains(id) || !fitsReroll(e)) continue;
            int max = Math.max(1, e.getMaxLevel());
            for (int lv = 1; lv <= max; lv++) {
                Ench x = new Ench();
                x.id = id;
                x.level = lv;
                x.grade = enchGrade(e, lv);
                x.weight = 10;
                enchants.add(x);
                n++;
            }
        }
        return n;
    }

    /** 이 세계의 인챈트로 재설정 목록을 만든다 (저주 제외, 무기·방어구·도구에 붙는 것). 단계가 높을수록 높은 등급 (고급~전설). */
    public void autoEnchants() {
        enchants.clear();
        addMissingEnchants();
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
            if (!pool(c).isEmpty()) g.pool(c).add(FAIL); // 실패 칸 (일반 등급 자리)
        }
        if (!all.isEmpty()) all.add(FAIL);
        return g;
    }

    /** 한 번 뽑을 때 쓰는 티켓 수. */
    public int cost(String mode) {
        return MODE_SELECT.equals(mode) ? selectCost : randomCost;
    }

    /** 야마토 등급은 공격 분야에만 둔다. 일반 등급은 '실패' 자리라서, 목록에 일반으로 적힌 칸은 고급으로 올린다. */
    public void normalize() {
        normalize(true);
    }

    private void normalize(boolean liftCommon) {
        for (String c : CATS) {
            for (GachaConfig.Entry e : pool(c)) {
                int gi = GachaConfig.gradeIndex(e.grade);
                if (gi == GachaConfig.YAMATO && !c.equals("attack")) e.grade = "legendary";
                if (gi == 0 && liftCommon) e.grade = "uncommon";
            }
        }
        for (Ench e : enchants) {
            int gi = GachaConfig.gradeIndex(e.grade);
            if (gi == GachaConfig.YAMATO) e.grade = "legendary";
            if (gi == 0 && liftCommon) e.grade = "uncommon";
        }
    }

    /**
     * 옛 설정(1)을 지금 규칙으로: 확률표를 새 기본값으로(첫 칸 = 실패), 인챈트 등급을 한 칸씩 올려 전설까지 쓰고,
     * 방어구·도구용 인챈트를 덧붙이고, 방어구·도구도 제단에 올릴 수 있게 한다.
     */
    public void migrate() {
        if (version >= 2) return;
        defaultOdds();
        for (Ench e : enchants) {
            int gi = GachaConfig.gradeIndex(e.grade);
            if (gi >= 0 && gi < 4) e.grade = GachaConfig.GRADES[gi + 1];
        }
        for (String k : new String[]{"armor", "tool"}) if (!rerollKinds.contains(k)) rerollKinds.add(k);
        version = 2;
    }

    // ------------------------------------------------------------------ 저장 · 불러오기

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("_help", "마법 뽑기 설정. pools: 분야(attack 공격·guard 방어·dodge 회피·passive 패시브·mover 이동)별 스킬북 목록 "
                + "(item, nbt, grade 등급, weight 같은 등급 안 가중치). odds: 등급별 확률 (실패, uncommon, rare, epic, legendary, yamato 순). "
                + "첫 칸은 '실패' 확률이다: 스킬북 뽑기는 아무것도 나오지 않고, 인챈트 재설정은 주문서만 쓰이고 장비는 그대로다. 목록에는 일반(common) 등급을 둘 수 없다 (고급으로 올라간다). "
                + "야마토 등급은 공격 분야에만 둘 수 있다. enchants: 인챈트 재설정 목록 (enchant, level, grade, weight). "
                + "costs: 한 번에 쓰는 티켓 수. 천장(확정)은 없다. "
                + "rerollKinds: 제단에 올릴 수 있는 장비 (weapon·bow·crossbow·trident·armor·tool). 게임 안에서는 OP 가 룬 제단의 [편집] 으로 고친다. 고친 뒤 /rtshop reload.");
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
        // (천장 설정은 3.49 에서 없앴다)
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
        // 옛 파일의 pity(천장) 설정은 읽지 않는다
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
        c.normalize(c.version >= CONFIG_VERSION); // 옛 설정은 migrate() 가 등급을 옮긴 뒤에 올린다
        return c;
    }

    private static MagicConfig load(MinecraftServer server) {
        Path p = path();
        try {
            if (Files.exists(p)) {
                MagicConfig c = fromJson(JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject());
                if (c.version < CONFIG_VERSION) { // 옛 설정: 지금 규칙으로 옮긴다 (서버가 켜진 뒤에 백업하고 저장한다)
                    int old = c.version;
                    c.migrate();
                    c.normalize();
                    if (server != null) {
                        try {
                            Files.copy(p, p.resolveSibling("magic_gacha.json.v" + old + ".bak"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        } catch (Exception ignored) {
                        }
                        c.addMissingEnchants();
                        c.save();
                        RtShop.LOGGER.info("[rtshop] magic_gacha.json 을 새 규칙(실패 칸·전설 인챈트·방어구)으로 옮겼습니다. 옛 파일은 .bak 으로 남겼습니다.");
                    }
                }
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

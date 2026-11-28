package com.rtshop.job;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * config/rtshop/jobs.json — 직업 목록, 패시브 수치, 직업별 무기 상성, 무기 종류, 변경 비용과 쿨타임.
 * 게임 안 직업 편집기(/rtshop job edit)로 고칠 수 있고, 메모장으로 고친 뒤 /rtshop reload 해도 된다.
 */
public final class JobConfig {
    public static final String COMBAT = "combat";
    public static final String LIFE = "life";
    /** 설정 파일 세대. 3.5 에서 무기 상성이 생기며 2 가 됐다 (이전 파일은 backup 으로 옮기고 새 기본값을 쓴다). */
    public static final int CONFIG_VERSION = 2;
    /**
     * 직업 수치의 판 (3.67.0 = 2: 생활 직업 수입 맞추기). 옛 기본값 그대로인 수치만 새 값으로 바꾼다 (운영자가 고친 수치는 그대로).
     * {직업, 수치, 옛 기본값, 새 기본값}
     */
    public static final int PARAMS_VERSION = 2;
    private static final Object[][] PARAMS_V2 = {{"farmer", "starUp", 0.20, 0.35}, {"farmer", "extraHarvest", 0.10, 0.20}, {"cook", "dishSell", 0.28, 0.20}};
    /** 수치가 %가 아니라 그대로의 값인 패시브 (체력 칸 수 등). */
    public static final Set<String> FLAT_PARAMS = Set.of("maxHealth", "distanceFull", "forgeDaily");

    public static final class JobDef {
        public String id;
        public String slot;
        public String name;
        public String icon = "minecraft:book";
        /** 자동으로 만든 설명 뒤에 붙는 추가 설명 (선택). */
        public final List<String> notes = new ArrayList<>();
        public String unlockFlag = "";
        public boolean hidden;
        public final Map<String, Double> params = new LinkedHashMap<>();
        /** 무기 종류 -> {피해 증감, 공격 속도 증감} (0.10 = +10%). */
        public final Map<String, double[]> weapons = new LinkedHashMap<>();

        public double param(String key, double def) {
            Double v = params.get(key);
            return v == null ? def : v;
        }

        public double weaponDamage(String type) {
            double[] w = weapons.get(type);
            return w == null ? 0 : w[0];
        }

        public double weaponSpeed(String type) {
            double[] w = weapons.get(type);
            return w == null ? 0 : w[1];
        }
    }

    public long changeCostCombat = 8000;
    public long changeCostLife = 5000;
    public double cooldownHoursCombat = 48;
    public double cooldownHoursLife = 24;
    public String npcName = "직업 안내인";
    /**
     * 히든 직업 (3.76.0, 위조자 · 조사관): 서버 인원 한도, 숨은 길이 열릴 확률 (위조자 = 밤에 룬 제단 곁에서 한 밤에 한 번,
     * 조사관 = 비 오는 날 기록관과 이야기할 때 하루에 한 번), 시험에서 틀리면 다시 볼 수 있을 때까지의 날 수.
     */
    public int hiddenJobCap = 2;
    public double hiddenNightChance = 0.15, hiddenRainChance = 0.30;
    public int hiddenRetryDays = 7;
    public final Map<String, JobDef> jobs = new LinkedHashMap<>();
    public final List<WeaponTypes.Type> weaponTypes = new ArrayList<>();
    /** 아이템 ID -> 무기 종류 (자동 분류가 틀린 모드 무기를 바로잡을 때). "none" = 무기 아님. */
    public final Map<String, String> weaponOverrides = new LinkedHashMap<>();
    private final Map<Item, String> cache = WeaponTypes.newCache();

    public long cost(String slot) {
        return COMBAT.equals(slot) ? changeCostCombat : changeCostLife;
    }

    public long cooldownMillis(String slot) {
        return (long) ((COMBAT.equals(slot) ? cooldownHoursCombat : cooldownHoursLife) * 3_600_000L);
    }

    public JobDef job(String id) {
        return id == null ? null : jobs.get(id);
    }

    /** 이 뽑기 등급 이상의 무기는 직업의 무기 분류에서 뺀다 ("" = 끔). */
    public String premiumFromGrade = "epic";
    /** 3.84: 바닐라 아이템은 직업의 무기 분류에서 뺀다 — 직업의 무기 보정 · 무기 패시브가 걸리지 않는다 (WeaponTypes.VANILLA). */
    public boolean vanillaNoWeaponPassives = true;
    /**
     * 3.84 (사용자 11-29 "마법사를 제외한 직업들도 마법을 사용할 수 있게 하되, 공격력, 시전 속도와 같은 수치가 마법사에 비해 많이 떨어지게" → 가: 위력 60%, 시전 · 재사용 1.3배):
     * 마법사가 아닌 직업의 주문 위력 배수와 시전 시간 · 재사용 대기 배수 (Iron's Spells · Spell Engine 모두).
     */
    public double nonMagePower = 0.6, nonMageCastTime = 1.3;
    /** 3.84 (사용자 11-29 "마법사가 지팡이를 들었을 때와 들지 않았을 때 데미지를 다르게 하기" → 나: 60%): 지팡이를 들지 않고 쓰는 주문의 위력 배수 (모든 직업). */
    public double noStaffPower = 0.6;

    /** 바닐라(마인크래프트 기본) 아이템인가. */
    public static boolean vanillaItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        net.minecraft.resources.ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && id.getNamespace().equals("minecraft");
    }

    /** 뽑기에서 나온 고급 무기인가 (등급 표시가 premiumFromGrade 이상). */
    public boolean premium(ItemStack stack) {
        if (stack == null || stack.isEmpty() || premiumFromGrade == null || premiumFromGrade.isEmpty()) return false;
        int from = com.rtshop.gacha.GachaConfig.gradeIndex(premiumFromGrade);
        return from >= 0 && com.rtshop.gacha.GachaConfig.gradeIndex(com.rtshop.gacha.Gacha.gradeOf(stack)) >= from;
    }

    /** 무기 분류의 캐시를 비운다 (weaponOverrides 를 코드에서 바꾼 뒤 — 점검용). */
    public synchronized void clearWeaponCache() {
        cache.clear();
    }

    /** 바닐라 · 고급 무기 규칙을 빼고 본 무기 종류 (설명에 원래 종류를 보일 때). */
    public synchronized String baseWeaponType(ItemStack stack) {
        return WeaponTypes.categoryOf(stack, weaponTypes, weaponOverrides, cache);
    }

    /**
     * 직업의 무기 상성에 쓰는 무기 종류. 뽑기의 고급 무기는 PREMIUM, 바닐라 무기는 VANILLA (3.84: 둘 다 직업 보정 없음).
     * 운영자가 weaponOverrides 에 직접 적은 아이템은 바닐라여도 그 종류를 따른다. 무기가 아닌 물건(none)은 바닐라여도 none.
     */
    public synchronized String weaponType(ItemStack stack) {
        if (premium(stack)) return WeaponTypes.PREMIUM;
        String base = WeaponTypes.categoryOf(stack, weaponTypes, weaponOverrides, cache);
        if (vanillaNoWeaponPassives && vanillaItem(stack) && !WeaponTypes.NONE.equals(base) && !WeaponTypes.FIST.equals(base)) {
            net.minecraft.resources.ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (id == null || !weaponOverrides.containsKey(id.toString())) return WeaponTypes.VANILLA;
        }
        return base;
    }

    public WeaponTypes.Type type(String id) {
        for (WeaponTypes.Type t : weaponTypes) {
            if (t.id.equals(id)) {
                return t;
            }
        }
        return null;
    }

    public String typeName(String id) {
        if (WeaponTypes.MAGIC.equals(id)) {
            return "주문";
        }
        if (WeaponTypes.VANILLA.equals(id)) {
            return "바닐라 · 직업 보정 없음";
        }
        if (WeaponTypes.PREMIUM.equals(id)) {
            return "고급 무기 · 직업 보정 없음";
        }
        WeaponTypes.Type t = type(id);
        return t == null ? id : t.name;
    }

    public boolean isMelee(String id) {
        WeaponTypes.Type t = type(id);
        return t != null && t.melee;
    }

    // ------------------------------------------------------------------ 기본값

    private static JobDef def(String id, String slot, String name, String icon, Object[] params, Object[] weapons) {
        JobDef d = new JobDef();
        d.id = id;
        d.slot = slot;
        d.name = name;
        d.icon = icon;
        d.unlockFlag = "unlock_job_" + id;
        for (int i = 0; i + 1 < params.length; i += 2) {
            d.params.put((String) params[i], ((Number) params[i + 1]).doubleValue());
        }
        for (int i = 0; i + 2 < weapons.length; i += 3) {
            d.weapons.put((String) weapons[i], new double[]{((Number) weapons[i + 1]).doubleValue(), ((Number) weapons[i + 2]).doubleValue()});
        }
        return d;
    }

    private static Object[] p(Object... a) {
        return a;
    }

    private void defaults() {
        weaponTypes.addAll(WeaponTypes.defaults());
        // 이름만으로는 분류되지 않는 모드팩 무기 (편집기에서 바꿀 수 있음)
        String[][] over = {
                {"cataclysm:coral_bardiche", "spear"}, {"cataclysm:soul_render", "spear"}, {"cataclysm:astrape", "spear"},
                {"cataclysm:ceraunus", "spear"}, {"cataclysm:the_incinerator", "sword"}, {"cataclysm:the_annihilator", "axe"},
                {"cataclysm:the_immolator", "staff"}, {"cataclysm:meat_shredder", "sword"}, {"wom:gesetz", "staff"},
                {"wom:demon_bracelet", "fist"}, {"spmovesets:pulseras_choque", "fist"}, {"irons_spellbooks:lightning_rod", "staff"},
                {"farmersdelight:skillet", "blunt"}};
        for (String[] o : over) {
            weaponOverrides.put(o[0], o[1]);
        }
        add(def("swordsman", COMBAT, "검사", "minecraft:iron_sword",
                p("critDamage", 0.10),
                p("sword", 0.10, 0.10, "dagger", 0, 0, "axe", -0.15, -0.10, "blunt", -0.15, -0.10, "spear", -0.15, -0.10,
                        "fist", -0.10, 0, "bow", -0.20, 0, "crossbow", -0.20, 0, "gun", -0.20, 0, "staff", -0.20, -0.10, "magic", -0.15, 0)));
        add(def("warrior", COMBAT, "전사", "minecraft:iron_axe",
                p("maxHealth", 2, "moveSpeed", 0.10),
                p("axe", 0.10, 0, "blunt", 0.10, 0, "dagger", -0.10, -0.05, "bow", -0.20, 0, "crossbow", -0.20, 0, "gun", -0.20, 0,
                        "staff", -0.20, -0.10, "magic", -0.20, 0)));
        add(def("tank", COMBAT, "탱커", "minecraft:shield",
                p("damageTaken", 0.10, "knockbackResist", 0.20, "maxHealth", 4, "shieldKnockback", 0.60, "shieldReflect", 0.05, "moveSpeed", -0.05),
                p("sword", -0.05, 0, "axe", -0.05, 0, "dagger", -0.10, -0.10, "fist", -0.05, 0, "bow", -0.25, 0, "crossbow", -0.20, 0,
                        "gun", -0.20, 0, "staff", -0.20, -0.10, "magic", -0.15, 0)));
        add(def("archer", COMBAT, "궁수", "minecraft:bow",
                p("arrowSave", 0.10, "distanceBonus", 0.15, "distanceFull", 32, "maxHealth", -2),
                p("bow", 0.15, 0, "crossbow", 0.10, 0, "gun", 0.05, 0, "sword", -0.15, -0.10, "axe", -0.20, -0.10, "blunt", -0.20, -0.15,
                        "spear", -0.15, -0.10, "fist", -0.10, 0, "staff", -0.15, -0.10, "magic", -0.10, 0)));
        add(def("mage", COMBAT, "마법사", "minecraft:enchanted_book",
                p("maxMana", 0.10, "spellPower", 0.10, "maxHealth", -2),
                p("sword", -0.25, -0.10, "axe", -0.25, -0.10, "blunt", -0.25, -0.10, "spear", -0.25, -0.10, "dagger", -0.25, -0.10,
                        "fist", -0.25, -0.10, "bow", -0.25, 0, "crossbow", -0.25, 0, "gun", -0.25, 0)));
        // 3.67.0 생활 직업 수입 맞추기 (사용자 요청 "완벽히 같게 하지는 말고 납득이 될 만큼"): 농부 ↑ · 광부에 캔 광물 판매 보너스 · 요리사 조금 ↓ (어부는 물고기 값 쪽에서)
        add(def("farmer", LIFE, "농부", "minecraft:iron_hoe", p("starUp", 0.35, "extraHarvest", 0.20), p()));
        add(def("fisher", LIFE, "어부", "minecraft:fishing_rod", p("fishSell", 0.20, "extraFish", 0.10), p()));
        add(def("miner", LIFE, "광부", "minecraft:iron_pickaxe", p("mineSpeed", 0.15, "extraOre", 0.10, "oreSell", 0.20), p()));
        add(def("cook", LIFE, "요리사", "minecraft:cooked_beef", p("foodBonus", 0.20, "dishSell", 0.20), p()));
        // 3.76.0 ④ 히든 직업 (생활 칸): 숨은 퀘스트로 표식(unlock_job_<id>)을 얻은 사람에게만 직업 안내인의 목록에 보인다
        add(def("forger", LIFE, "위조자", "rtshop:forger_kit", p("forgeSkill", 0.15, "forgeDaily", 3), p()), true);
        add(def("investigator", LIFE, "조사관", "rtshop:appraisal_loupe", p("inspectSkill", 0.25), p()), true);
    }

    private void add(JobDef d) {
        jobs.put(d.id, d);
    }

    private void add(JobDef d, boolean hidden) {
        d.hidden = hidden;
        jobs.put(d.id, d);
    }

    // ------------------------------------------------------------------ 설명 (수치에서 자동으로 만든다)

    private static String pct(double v) {
        double x = Math.abs(v) * 100;
        return (x == Math.rint(x) ? Long.toString(Math.round(x)) : String.format(Locale.ROOT, "%.1f", x)) + "%";
    }

    private static String signed(double v) {
        return (v >= 0 ? "+" : "-") + pct(v);
    }

    private static String flat(double v) {
        String n = v == Math.rint(v) ? Long.toString(Math.round(Math.abs(v))) : String.format(Locale.ROOT, "%.1f", Math.abs(v));
        return (v >= 0 ? "+" : "-") + n;
    }

    /** 패시브 하나의 설명. 좋으면 true 를 함께 돌려준다. */
    public static String[] paramLine(String key, double v, double handMargin) {
        return paramLine(key, v, handMargin, null);
    }

    /** 영어 설명 (이야기 언어가 한국어가 아닌 사람에게). 수치로 조립하는 글이라 번역 표 대신 여기서 만든다. */
    public static String[] paramLine(String key, double v, double handMargin, JobDef d, boolean en) {
        if (!en) return paramLine(key, v, handMargin, d);
        double hearts = Math.abs(v) / 2;
        String heartText = (hearts == Math.rint(hearts) ? Long.toString(Math.round(hearts)) : String.format(Locale.ROOT, "%.1f", hearts)) + (hearts == 1 ? " heart" : " hearts");
        String text = switch (key) {
            case "critDamage" -> "Sword critical damage " + signed(v);
            case "maxHealth" -> "Max health " + flat(v) + " (" + heartText + ")";
            case "moveSpeed" -> "Movement speed " + signed(v);
            case "damageTaken" -> "Damage taken " + signed(-v);
            case "knockbackResist" -> "Knockback resistance " + signed(v);
            case "arrowSave" -> pct(v) + " chance to get a fired arrow back";
            case "shieldKnockback" -> "Blocking with a shield pushes melee attackers back (strength " + pct(v) + ")";
            case "shieldReflect" -> "Returns " + pct(v) + " of the melee damage blocked with a shield";
            case "distanceBonus" -> "Projectile damage up to " + signed(v) + " the farther the hit (full at "
                    + Math.round(d == null ? 32 : d.param("distanceFull", 32)) + "+ blocks)";
            case "maxMana" -> "Max mana " + signed(v) + " (Iron's Spells)";
            case "spellPower" -> "Spell power " + signed(v) + " (Iron's Spells · Wizards)";
            case "starUp" -> pct(v) + " chance that a harvested crop's star grade goes up one step";
            case "extraHarvest" -> pct(v) + " chance of 1 extra crop per harvest";
            case "fishSell" -> "Fish sale price " + signed(v);
            case "extraFish" -> pct(v) + " chance of 1 extra fish per catch";
            case "mineSpeed" -> "Mining speed " + signed(v);
            case "extraOre" -> pct(v) + " chance of an extra drop when mining ore";
            case "oreSell" -> "Mined ore, raw metal and gem sale price " + signed(v);
            case "foodBonus" -> "Hunger restored by food " + signed(v);
            case "dishSell" -> "Hand-cooked dish sale price " + signed(v) + " (about " + Math.round((1 + handMargin) * (1 + v) * 100) + "% of the ingredient cost)";
            case "forgeSkill" -> "Forged certificates look more genuine " + signed(v) + " (hold the forger's kit, the certificate to copy in the other hand)";
            case "forgeDaily" -> "Up to " + Math.round(v) + " forged certificates a day (paper, ink sac, honeycomb)";
            case "inspectSkill" -> "Chance to find a forged certificate's flaw " + signed(v) + " (hold the appraiser's loupe, the certificate in the other hand)";
            default -> key + " " + signed(v);
        };
        return new String[]{v > 0 ? "+" : "-", text};
    }

    public static String[] paramLine(String key, double v, double handMargin, JobDef d) {
        boolean good = v > 0;
        String text = switch (key) {
            case "critDamage" -> "검 치명타 피해 " + signed(v);
            case "maxHealth" -> "최대 체력 " + flat(v) + " (하트 " + (Math.abs(v) / 2 == Math.rint(Math.abs(v) / 2) ? Long.toString(Math.round(Math.abs(v) / 2)) : String.format(Locale.ROOT, "%.1f", Math.abs(v) / 2)) + "칸)";
            case "moveSpeed" -> "이동 속도 " + signed(v);
            case "damageTaken" -> "받는 피해 " + signed(-v);
            case "knockbackResist" -> "넉백 저항 " + signed(v);
            case "arrowSave" -> pct(v) + " 확률로 쏜 화살을 돌려받음";
            case "shieldKnockback" -> "방패로 막으면 근접 공격자를 밀쳐 냄 (세기 " + pct(v) + ")";
            case "shieldReflect" -> "방패로 막은 근접 피해의 " + pct(v) + "를 되돌려 줌";
            case "distanceBonus" -> "멀리서 맞힐수록 투사체 피해 최대 " + signed(v) + " ("
                    + Math.round(d == null ? 32 : d.param("distanceFull", 32)) + "칸 이상에서 최대)";
            case "maxMana" -> "최대 마나 " + signed(v) + " (Iron's Spells)";
            case "spellPower" -> "주문 위력 " + signed(v) + " (Iron's Spells · Wizards)";
            case "starUp" -> "수확한 작물의 별 등급이 " + pct(v) + " 확률로 한 단계 오름";
            case "extraHarvest" -> pct(v) + " 확률로 수확물 1개 더";
            case "fishSell" -> "수산물 판매가 " + signed(v);
            case "extraFish" -> "낚시로 잡은 물고기 " + pct(v) + " 확률로 1마리 더";
            case "mineSpeed" -> "채굴 속도 " + signed(v);
            case "extraOre" -> "광석을 캘 때 " + pct(v) + " 확률로 추가 드롭";
            case "oreSell" -> "캐낸 광석 · 원석 · 보석 판매가 " + signed(v);
            case "foodBonus" -> "음식을 먹을 때 포만감 " + signed(v);
            case "dishSell" -> "손으로 만든 요리 판매가 " + signed(v) + " (재료비의 약 " + Math.round((1 + handMargin) * (1 + v) * 100) + "%)";
            case "forgeSkill" -> "위조 증서의 완성도 " + signed(v) + " (위조 도구를 들고, 다른 손에 본으로 삼을 증서)";
            case "forgeDaily" -> "하루에 위조 증서 " + Math.round(v) + "장까지 (종이 · 먹물 주머니 · 벌집 조각)";
            case "inspectSkill" -> "위조 증서의 흠을 찾을 확률 " + signed(v) + " (감정 돋보기를 들고, 다른 손에 증서)";
            default -> key + " " + signed(v);
        };
        return new String[]{good ? "+" : "-", text};
    }

    /** 화면에 보일 설명 줄: 앞 글자가 '+'(장점) / '-'(단점) / ' '(참고). */
    public List<String> describe(JobDef d, double handMargin) {
        return describe(d, handMargin, null);
    }

    /** 그 플레이어의 이야기 언어로 쓴 설명 줄 (p 가 null 이면 한국어). */
    public List<String> describe(JobDef d, double handMargin, net.minecraft.server.level.ServerPlayer p) {
        boolean en = com.rtshop.story.StoryText.en(p);
        List<String> pros = new ArrayList<>();
        List<String> cons = new ArrayList<>();
        for (Map.Entry<String, Double> e : d.params.entrySet()) {
            if (e.getValue() == 0 || e.getKey().equals("distanceFull")) {
                continue; // 최대 보너스 거리는 거리 보너스 설명 안에 함께 적는다
            }
            String[] l = paramLine(e.getKey(), e.getValue(), handMargin, d, en);
            (l[0].equals("+") ? pros : cons).add(l[1]);
        }
        // 같은 수치의 무기 종류는 한 줄로 묶는다
        Map<String, List<String>> groups = new LinkedHashMap<>();
        Map<String, Boolean> goodOf = new LinkedHashMap<>();
        List<String> order = new ArrayList<>();
        for (WeaponTypes.Type t : weaponTypes) {
            order.add(t.id);
        }
        order.add(WeaponTypes.MAGIC);
        for (String type : order) {
            double dmg = d.weaponDamage(type);
            double spd = isMelee(type) ? d.weaponSpeed(type) : 0;
            if (dmg == 0 && spd == 0) {
                continue;
            }
            StringBuilder sb = new StringBuilder();
            if (dmg != 0) {
                sb.append(en ? "damage " : "피해 ").append(signed(dmg));
            }
            if (spd != 0) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(en ? "attack speed " : "공격 속도 ").append(signed(spd));
            }
            String key = sb.toString();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(com.rtshop.story.StoryText.t(p, typeName(type)));
            goodOf.put(key, dmg > 0 || (dmg == 0 && spd > 0));
        }
        for (Map.Entry<String, List<String>> g : groups.entrySet()) {
            String line = en ? String.join(" / ", g.getValue()) + ": " + g.getKey() : String.join("·", g.getValue()) + " " + g.getKey();
            (goodOf.get(g.getKey()) ? pros : cons).add(line);
        }
        List<String> out = new ArrayList<>();
        pros.forEach(s -> out.add("+" + s));
        cons.forEach(s -> out.add("-" + s));
        d.notes.forEach(s -> out.add(" " + com.rtshop.story.StoryText.t(p, s)));
        return out;
    }

    // ------------------------------------------------------------------ 파일

    private static JobConfig instance;

    public static synchronized JobConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public static synchronized JobConfig reload() {
        instance = load();
        return instance;
    }

    public static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("jobs.json");
    }

    private static JobConfig load() {
        Path p = path();
        try {
            if (Files.exists(p)) {
                JsonObject o = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
                int ver = o.has("configVersion") ? o.get("configVersion").getAsInt() : 1;
                if (ver >= CONFIG_VERSION) {
                    JobConfig c = fromJson(o);
                    int pv = o.has("paramsVersion") ? o.get("paramsVersion").getAsInt() : 1;
                    if (pv < PARAMS_VERSION) {
                        int n = c.upgradeParams(pv);
                        c.save();
                        RtShop.LOGGER.info("[rtshop] jobs.json 의 생활 직업 수치를 {}판으로 올렸습니다 (기본값이던 수치 {}개)", PARAMS_VERSION, n);
                    }
                    return c;
                }
                // 이전 버전 파일: 보관하고 새 기본값으로 바꾼다 (비용·쿨타임·NPC 이름은 이어받는다)
                Path backup = FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("backup").resolve("jobs_v" + ver + "_" + System.currentTimeMillis() + ".json");
                Files.createDirectories(backup.getParent());
                Files.copy(p, backup, StandardCopyOption.REPLACE_EXISTING);
                JobConfig c = new JobConfig();
                c.defaults();
                if (o.has("changeCostCombat")) c.changeCostCombat = Math.max(0, o.get("changeCostCombat").getAsLong());
                if (o.has("changeCostLife")) c.changeCostLife = Math.max(0, o.get("changeCostLife").getAsLong());
                if (o.has("cooldownHoursCombat")) c.cooldownHoursCombat = Math.max(0, o.get("cooldownHoursCombat").getAsDouble());
                if (o.has("cooldownHoursLife")) c.cooldownHoursLife = Math.max(0, o.get("cooldownHoursLife").getAsDouble());
                if (o.has("npcName")) c.npcName = o.get("npcName").getAsString();
                c.save();
                RtShop.LOGGER.info("[rtshop] 이전 jobs.json 을 {} 로 옮기고 새 직업 설정(무기 상성 포함)을 만들었습니다.", backup);
                return c;
            }
            JobConfig c = new JobConfig();
            c.defaults();
            c.save();
            return c;
        } catch (Exception e) {
            RtShop.LOGGER.error("[rtshop] jobs.json 을 읽지 못했습니다. 기본값을 씁니다.", e);
            JobConfig c = new JobConfig();
            c.defaults();
            return c;
        }
    }

    public void save() throws java.io.IOException {
        Path p = path();
        Files.createDirectories(p.getParent());
        Files.writeString(p, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(toJson()), StandardCharsets.UTF_8);
    }

    /** 옛 판의 기본값 그대로인 수치를 새 기본값으로 바꾼다. 바꾼 수를 돌려준다. (새로 생긴 수치 — 광부 oreSell — 는 읽을 때 기본값으로 붙는다.) */
    public int upgradeParams(int from) {
        int n = 0;
        if (from < 2) {
            for (Object[] u : PARAMS_V2) {
                JobDef d = jobs.get((String) u[0]);
                Double cur = d == null ? null : d.params.get((String) u[1]);
                if (cur != null && Math.abs(cur - (Double) u[2]) < 1e-9) {
                    d.params.put((String) u[1], (Double) u[3]);
                    n++;
                }
            }
        }
        return n;
    }

    /** JSON 에서 읽는다. 형식이 틀리면 IllegalArgumentException (편집기 저장 검사에도 쓴다). */
    public static JobConfig fromJson(JsonObject o) {
        JobConfig c = new JobConfig();
        JobConfig defs = new JobConfig();
        defs.defaults();
        if (o.has("changeCostCombat")) c.changeCostCombat = Math.max(0, o.get("changeCostCombat").getAsLong());
        if (o.has("changeCostLife")) c.changeCostLife = Math.max(0, o.get("changeCostLife").getAsLong());
        if (o.has("cooldownHoursCombat")) c.cooldownHoursCombat = Math.max(0, o.get("cooldownHoursCombat").getAsDouble());
        if (o.has("cooldownHoursLife")) c.cooldownHoursLife = Math.max(0, o.get("cooldownHoursLife").getAsDouble());
        if (o.has("npcName")) c.npcName = o.get("npcName").getAsString();
        if (o.has("hiddenJobCap")) c.hiddenJobCap = Math.max(0, o.get("hiddenJobCap").getAsInt());
        if (o.has("hiddenNightChance")) c.hiddenNightChance = Math.max(0, Math.min(1, o.get("hiddenNightChance").getAsDouble()));
        if (o.has("hiddenRainChance")) c.hiddenRainChance = Math.max(0, Math.min(1, o.get("hiddenRainChance").getAsDouble()));
        if (o.has("hiddenRetryDays")) c.hiddenRetryDays = Math.max(0, Math.min(365, o.get("hiddenRetryDays").getAsInt()));
        if (o.has("weaponTypes") && o.get("weaponTypes").isJsonArray()) {
            for (JsonElement e : o.getAsJsonArray("weaponTypes")) {
                c.weaponTypes.add(WeaponTypes.read(e.getAsJsonObject()));
            }
        } else {
            c.weaponTypes.addAll(defs.weaponTypes);
        }
        if (o.has("premiumFromGrade")) c.premiumFromGrade = o.get("premiumFromGrade").getAsString();
        if (o.has("vanillaNoWeaponPassives")) c.vanillaNoWeaponPassives = o.get("vanillaNoWeaponPassives").getAsBoolean();
        if (o.has("nonMagePower")) c.nonMagePower = Math.max(0, Math.min(1, o.get("nonMagePower").getAsDouble()));
        if (o.has("nonMageCastTime")) c.nonMageCastTime = Math.max(1, Math.min(5, o.get("nonMageCastTime").getAsDouble()));
        if (o.has("noStaffPower")) c.noStaffPower = Math.max(0, Math.min(1, o.get("noStaffPower").getAsDouble()));
        if (o.has("weaponOverrides") && o.get("weaponOverrides").isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("weaponOverrides").entrySet()) {
                c.weaponOverrides.put(e.getKey(), e.getValue().getAsString());
            }
        }
        if (o.has("jobs") && o.get("jobs").isJsonArray()) {
            for (JsonElement e : o.getAsJsonArray("jobs")) {
                JobDef d = read(e.getAsJsonObject(), defs.jobs);
                c.jobs.put(d.id, d);
            }
        }
        for (JobDef d : defs.jobs.values()) {
            c.jobs.putIfAbsent(d.id, d);
        }
        return c;
    }

    private static JobDef read(JsonObject o, Map<String, JobDef> defaults) {
        String id = o.get("id").getAsString().trim();
        if (!id.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException("직업 ID 는 영문 소문자·숫자·_ 만: " + id);
        }
        JobDef base = defaults.get(id);
        JobDef d = new JobDef();
        d.id = id;
        d.slot = o.has("slot") ? o.get("slot").getAsString() : (base != null ? base.slot : LIFE);
        if (!COMBAT.equals(d.slot) && !LIFE.equals(d.slot)) {
            throw new IllegalArgumentException(id + ": slot 은 combat 또는 life 여야 합니다");
        }
        d.name = o.has("name") ? o.get("name").getAsString() : (base != null ? base.name : id);
        d.icon = o.has("icon") ? o.get("icon").getAsString() : (base != null ? base.icon : d.icon);
        d.unlockFlag = o.has("unlockFlag") ? o.get("unlockFlag").getAsString() : (base != null ? base.unlockFlag : "");
        d.hidden = o.has("hidden") ? o.get("hidden").getAsBoolean() : base != null && base.hidden;
        if (o.has("notes") && o.get("notes").isJsonArray()) {
            for (JsonElement e : o.getAsJsonArray("notes")) {
                d.notes.add(e.getAsString());
            }
        }
        if (o.has("params") && o.get("params").isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("params").entrySet()) {
                d.params.put(e.getKey(), e.getValue().getAsDouble());
            }
        }
        if (base != null) {
            // 업데이트로 새로 생긴 특성은 기본값으로 붙인다 (파일에 이미 있는 수치는 그대로)
            for (Map.Entry<String, Double> e : base.params.entrySet()) {
                d.params.putIfAbsent(e.getKey(), e.getValue());
            }
        }
        if (o.has("weapons") && o.get("weapons").isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("weapons").entrySet()) {
                JsonObject w = e.getValue().getAsJsonObject();
                double dmg = w.has("damage") ? w.get("damage").getAsDouble() : 0;
                double spd = w.has("speed") ? w.get("speed").getAsDouble() : 0;
                if (dmg < -0.95 || spd < -0.95 || dmg > 10 || spd > 10) {
                    throw new IllegalArgumentException(id + " 의 " + e.getKey() + " 수치가 너무 큽니다 (-95% ~ +1000%)");
                }
                d.weapons.put(e.getKey(), new double[]{dmg, spd});
            }
        } else if (base != null && !o.has("weapons")) {
            base.weapons.forEach((k, v) -> d.weapons.put(k, v.clone()));
        }
        return d;
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        JsonArray help = new JsonArray();
        help.add("직업 설정. 게임 안에서는 /rtshop job edit (OP) 로 고칠 수 있습니다. 메모장으로 고쳤다면 /rtshop reload.");
        help.add("changeCost*: 직업을 바꿀 때 드는 Gold (처음 고를 때는 무료) · cooldownHours*: 바꾼 뒤 다시 바꿀 수 있을 때까지의 실제 시간");
        help.add("jobs[].params: 패시브 수치 (0.10 = 10%, maxHealth 는 체력 수치: 2 = 하트 1칸). 음수는 단점.");
        help.add("jobs[].weapons: 무기 종류별 {damage: 피해 증감, speed: 공격 속도 증감} (0.10 = +10%, -0.15 = -15%). magic 은 주문 피해. 공격 속도는 근접 무기에만 적용.");
        help.add("weaponTypes: 무기 종류와 분류 규칙 (위에서부터 처음 맞는 종류). weaponOverrides: 아이템 ID -> 종류 (자동 분류가 틀렸을 때, none = 무기 아님)");
        help.add("jobs[].unlockFlag: 이 표식을 보상으로 주는 퀘스트가 있으면 그 퀘스트를 깨야 고를 수 있습니다.");
        help.add("jobs[].hidden: 표식을 얻기 전에는 직업 창에 보이지도 않는 히든 직업. hiddenJobCap = 히든 직업마다 서버에 둘 수 있는 사람 수 (0 = 한도 없음), "
                + "hiddenNightChance = 위조자의 길이 열릴 확률 (밤에 룬 제단 곁, 한 밤에 한 번), hiddenRainChance = 조사관의 길이 열릴 확률 (비 오는 날 기록관과 이야기할 때, 하루에 한 번), "
                + "hiddenRetryDays = 숨은 시험에서 틀리면 다시 볼 수 있을 때까지의 날 수.");
        o.add("_help", help);
        o.addProperty("configVersion", CONFIG_VERSION);
        o.addProperty("paramsVersion", PARAMS_VERSION);
        o.addProperty("changeCostCombat", changeCostCombat);
        o.addProperty("changeCostLife", changeCostLife);
        o.addProperty("cooldownHoursCombat", cooldownHoursCombat);
        o.addProperty("cooldownHoursLife", cooldownHoursLife);
        o.addProperty("npcName", npcName);
        o.addProperty("hiddenJobCap", hiddenJobCap);
        o.addProperty("hiddenNightChance", hiddenNightChance);
        o.addProperty("hiddenRainChance", hiddenRainChance);
        o.addProperty("hiddenRetryDays", hiddenRetryDays);
        JsonArray arr = new JsonArray();
        for (JobDef d : jobs.values()) {
            JsonObject j = new JsonObject();
            j.addProperty("id", d.id);
            j.addProperty("slot", d.slot);
            j.addProperty("name", d.name);
            j.addProperty("icon", d.icon);
            j.addProperty("unlockFlag", d.unlockFlag);
            j.addProperty("hidden", d.hidden);
            JsonArray notes = new JsonArray();
            d.notes.forEach(notes::add);
            j.add("notes", notes);
            JsonObject params = new JsonObject();
            d.params.forEach(params::addProperty);
            j.add("params", params);
            JsonObject weapons = new JsonObject();
            d.weapons.forEach((k, v) -> {
                JsonObject w = new JsonObject();
                w.addProperty("damage", v[0]);
                w.addProperty("speed", v[1]);
                weapons.add(k, w);
            });
            j.add("weapons", weapons);
            arr.add(j);
        }
        o.add("jobs", arr);
        JsonArray types = new JsonArray();
        weaponTypes.forEach(t -> types.add(WeaponTypes.write(t)));
        o.add("weaponTypes", types);
        JsonObject over = new JsonObject();
        weaponOverrides.forEach(over::addProperty);
        o.add("weaponOverrides", over);
        o.addProperty("premiumFromGrade", premiumFromGrade);
        o.addProperty("vanillaNoWeaponPassives", vanillaNoWeaponPassives);
        o.addProperty("nonMagePower", nonMagePower);
        o.addProperty("nonMageCastTime", nonMageCastTime);
        o.addProperty("noStaffPower", noStaffPower);
        return o;
    }
}

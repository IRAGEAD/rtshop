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
    /** 수치가 %가 아니라 그대로의 값인 패시브 (체력 칸 수 등). */
    public static final Set<String> FLAT_PARAMS = Set.of("maxHealth");

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

    public synchronized String weaponType(ItemStack stack) {
        return WeaponTypes.categoryOf(stack, weaponTypes, weaponOverrides, cache);
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
        add(def("swordsman", COMBAT, "검사", "minecraft:iron_sword",
                p("critDamage", 0.10),
                p("sword", 0.10, 0.10, "dagger", 0, 0, "axe", -0.15, -0.10, "blunt", -0.15, -0.10, "spear", -0.15, -0.10,
                        "fist", -0.10, 0, "bow", -0.20, 0, "crossbow", -0.20, 0, "gun", -0.20, 0, "staff", -0.20, -0.10, "magic", -0.15, 0)));
        add(def("warrior", COMBAT, "전사", "minecraft:iron_axe",
                p("maxHealth", 2, "moveSpeed", 0.10),
                p("axe", 0.10, 0, "blunt", 0.10, 0, "dagger", -0.10, -0.05, "bow", -0.20, 0, "crossbow", -0.20, 0, "gun", -0.20, 0,
                        "staff", -0.20, -0.10, "magic", -0.20, 0)));
        add(def("tank", COMBAT, "탱커", "minecraft:shield",
                p("damageTaken", 0.10, "knockbackResist", 0.20, "maxHealth", 4, "moveSpeed", -0.05),
                p("sword", -0.05, 0, "axe", -0.05, 0, "dagger", -0.10, -0.10, "fist", -0.05, 0, "bow", -0.25, 0, "crossbow", -0.20, 0,
                        "gun", -0.20, 0, "staff", -0.20, -0.10, "magic", -0.15, 0)));
        add(def("archer", COMBAT, "궁수", "minecraft:bow",
                p("arrowSave", 0.10, "maxHealth", -2),
                p("bow", 0.15, 0, "crossbow", 0.10, 0, "gun", 0.05, 0, "sword", -0.15, -0.10, "axe", -0.20, -0.10, "blunt", -0.20, -0.15,
                        "spear", -0.15, -0.10, "fist", -0.10, 0, "staff", -0.15, -0.10, "magic", -0.10, 0)));
        add(def("mage", COMBAT, "마법사", "minecraft:enchanted_book",
                p("maxMana", 0.10, "spellPower", 0.10, "maxHealth", -2),
                p("sword", -0.25, -0.10, "axe", -0.25, -0.10, "blunt", -0.25, -0.10, "spear", -0.25, -0.10, "dagger", -0.25, -0.10,
                        "fist", -0.25, -0.10, "bow", -0.25, 0, "crossbow", -0.25, 0, "gun", -0.25, 0)));
        add(def("farmer", LIFE, "농부", "minecraft:iron_hoe", p("starUp", 0.20, "extraHarvest", 0.10), p()));
        add(def("fisher", LIFE, "어부", "minecraft:fishing_rod", p("fishSell", 0.20, "extraFish", 0.10), p()));
        add(def("miner", LIFE, "광부", "minecraft:iron_pickaxe", p("mineSpeed", 0.15, "extraOre", 0.10), p()));
        add(def("cook", LIFE, "요리사", "minecraft:cooked_beef", p("foodBonus", 0.20, "dishSell", 0.28), p()));
    }

    private void add(JobDef d) {
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
        boolean good = v > 0;
        String text = switch (key) {
            case "critDamage" -> "검 치명타 피해 " + signed(v);
            case "maxHealth" -> "최대 체력 " + flat(v) + " (하트 " + (Math.abs(v) / 2 == Math.rint(Math.abs(v) / 2) ? Long.toString(Math.round(Math.abs(v) / 2)) : String.format(Locale.ROOT, "%.1f", Math.abs(v) / 2)) + "칸)";
            case "moveSpeed" -> "이동 속도 " + signed(v);
            case "damageTaken" -> "받는 피해 " + signed(-v);
            case "knockbackResist" -> "넉백 저항 " + signed(v);
            case "arrowSave" -> pct(v) + " 확률로 쏜 화살을 돌려받음";
            case "maxMana" -> "최대 마나 " + signed(v) + " (Iron's Spells)";
            case "spellPower" -> "주문 위력 " + signed(v) + " (Iron's Spells · Wizards)";
            case "starUp" -> "수확한 작물의 별 등급이 " + pct(v) + " 확률로 한 단계 오름";
            case "extraHarvest" -> pct(v) + " 확률로 수확물 1개 더";
            case "fishSell" -> "수산물 판매가 " + signed(v);
            case "extraFish" -> "낚시로 잡은 물고기 " + pct(v) + " 확률로 1마리 더";
            case "mineSpeed" -> "채굴 속도 " + signed(v);
            case "extraOre" -> "광석을 캘 때 " + pct(v) + " 확률로 추가 드롭";
            case "foodBonus" -> "음식을 먹을 때 포만감 " + signed(v);
            case "dishSell" -> "손으로 만든 요리 판매가 " + signed(v) + " (재료비의 약 " + Math.round((1 + handMargin) * (1 + v) * 100) + "%)";
            default -> key + " " + signed(v);
        };
        return new String[]{good ? "+" : "-", text};
    }

    /** 화면에 보일 설명 줄: 앞 글자가 '+'(장점) / '-'(단점) / ' '(참고). */
    public List<String> describe(JobDef d, double handMargin) {
        List<String> pros = new ArrayList<>();
        List<String> cons = new ArrayList<>();
        for (Map.Entry<String, Double> e : d.params.entrySet()) {
            if (e.getValue() == 0) {
                continue;
            }
            String[] l = paramLine(e.getKey(), e.getValue(), handMargin);
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
                sb.append("피해 ").append(signed(dmg));
            }
            if (spd != 0) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append("공격 속도 ").append(signed(spd));
            }
            String key = sb.toString();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(typeName(type));
            goodOf.put(key, dmg > 0 || (dmg == 0 && spd > 0));
        }
        for (Map.Entry<String, List<String>> g : groups.entrySet()) {
            String line = String.join("·", g.getValue()) + " " + g.getKey();
            (goodOf.get(g.getKey()) ? pros : cons).add(line);
        }
        List<String> out = new ArrayList<>();
        pros.forEach(s -> out.add("+" + s));
        cons.forEach(s -> out.add("-" + s));
        d.notes.forEach(s -> out.add(" " + s));
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
                    return fromJson(o);
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
        if (o.has("weaponTypes") && o.get("weaponTypes").isJsonArray()) {
            for (JsonElement e : o.getAsJsonArray("weaponTypes")) {
                c.weaponTypes.add(WeaponTypes.read(e.getAsJsonObject()));
            }
        } else {
            c.weaponTypes.addAll(defs.weaponTypes);
        }
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
        } else if (base != null) {
            d.params.putAll(base.params);
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
        o.add("_help", help);
        o.addProperty("configVersion", CONFIG_VERSION);
        o.addProperty("changeCostCombat", changeCostCombat);
        o.addProperty("changeCostLife", changeCostLife);
        o.addProperty("cooldownHoursCombat", cooldownHoursCombat);
        o.addProperty("cooldownHoursLife", cooldownHoursLife);
        o.addProperty("npcName", npcName);
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
        return o;
    }
}

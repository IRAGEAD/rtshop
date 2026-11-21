package com.rtshop.job;

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
 * config/rtshop/jobs.json — 직업 목록, 패시브 수치, 변경 비용과 쿨타임.
 * 파일이 없으면 기본값으로 만든다. 직업 이름·설명·수치는 파일에서 바로 고칠 수 있고 /rtshop reload 로 적용된다.
 */
public final class JobConfig {
    public static final String COMBAT = "combat";
    public static final String LIFE = "life";

    public static final class JobDef {
        public String id;
        public String slot;
        public String name;
        public String icon = "minecraft:book";
        public final List<String> desc = new ArrayList<>();
        /** 이 표식이 있어야 고를 수 있다 (선행 퀘스트 보상). 이 표식을 주는 퀘스트가 하나도 없으면 바로 고를 수 있다. */
        public String unlockFlag = "";
        /** 해금되기 전에는 목록에 보이지 않는 직업 (히든 직업). */
        public boolean hidden;
        public final Map<String, Double> params = new LinkedHashMap<>();

        public double param(String key, double def) {
            Double v = params.get(key);
            return v == null ? def : v;
        }
    }

    public long changeCostCombat = 8000;
    public long changeCostLife = 5000;
    public double cooldownHoursCombat = 48;
    public double cooldownHoursLife = 24;
    /** 직업 안내인 NPC 이름. */
    public String npcName = "직업 안내인";
    public final Map<String, JobDef> jobs = new LinkedHashMap<>();

    public long cost(String slot) {
        return COMBAT.equals(slot) ? changeCostCombat : changeCostLife;
    }

    public long cooldownMillis(String slot) {
        return (long) ((COMBAT.equals(slot) ? cooldownHoursCombat : cooldownHoursLife) * 3_600_000L);
    }

    public JobDef job(String id) {
        return id == null ? null : jobs.get(id);
    }

    // ------------------------------------------------------------------ 기본 직업

    private static JobDef def(String id, String slot, String name, String icon, String[] desc, Object... params) {
        JobDef d = new JobDef();
        d.id = id;
        d.slot = slot;
        d.name = name;
        d.icon = icon;
        d.unlockFlag = "unlock_job_" + id;
        for (String s : desc) {
            d.desc.add(s);
        }
        for (int i = 0; i + 1 < params.length; i += 2) {
            d.params.put((String) params[i], ((Number) params[i + 1]).doubleValue());
        }
        return d;
    }

    private void defaults() {
        add(def("swordsman", COMBAT, "검사", "minecraft:iron_sword",
                new String[]{"검을 들면 공격 속도 +10%", "치명타 피해 +10%"},
                "attackSpeed", 0.10, "critDamage", 0.10));
        add(def("warrior", COMBAT, "전사", "minecraft:iron_axe",
                new String[]{"도끼로 주는 피해 +8%", "최대 체력 +2 (하트 1칸)"},
                "axeDamage", 0.08, "maxHealth", 2));
        add(def("tank", COMBAT, "탱커", "minecraft:shield",
                new String[]{"받는 피해 -6%", "넉백 저항 +20%"},
                "damageTaken", 0.06, "knockbackResist", 0.20));
        add(def("archer", COMBAT, "궁수", "minecraft:bow",
                new String[]{"화살·투사체 피해 +10%", "10% 확률로 화살을 돌려받음"},
                "projectileDamage", 0.10, "arrowSave", 0.10));
        add(def("mage", COMBAT, "마법사", "minecraft:enchanted_book",
                new String[]{"최대 마나 +10% (Iron's Spells)", "주문 위력 +5% (Iron's Spells · Wizards)"},
                "maxMana", 0.10, "spellPower", 0.05));
        add(def("farmer", LIFE, "농부", "minecraft:iron_hoe",
                new String[]{"수확한 작물의 별 등급이 15% 확률로 한 단계 오름", "5% 확률로 수확물 1개 더"},
                "starUp", 0.15, "extraHarvest", 0.05));
        add(def("fisher", LIFE, "어부", "minecraft:fishing_rod",
                new String[]{"수산물 판매가 +10%", "낚시로 잡은 물고기 5% 확률로 1마리 더"},
                "fishSell", 0.10, "extraFish", 0.05));
        add(def("miner", LIFE, "광부", "minecraft:iron_pickaxe",
                new String[]{"채굴 속도 +8%", "광석을 캘 때 5% 확률로 추가 드롭"},
                "mineSpeed", 0.08, "extraOre", 0.05));
        add(def("cook", LIFE, "요리사", "minecraft:cooked_beef",
                new String[]{"음식을 먹을 때 포만감 +10%", "조리한 요리 판매가 +5%"},
                "foodBonus", 0.10, "dishSell", 0.05));
    }

    private void add(JobDef d) {
        jobs.put(d.id, d);
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

    private static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("jobs.json");
    }

    private static JobConfig load() {
        JobConfig c = new JobConfig();
        c.defaults();
        Path p = path();
        try {
            if (Files.exists(p)) {
                JsonObject o = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
                if (o.has("changeCostCombat")) c.changeCostCombat = Math.max(0, o.get("changeCostCombat").getAsLong());
                if (o.has("changeCostLife")) c.changeCostLife = Math.max(0, o.get("changeCostLife").getAsLong());
                if (o.has("cooldownHoursCombat")) c.cooldownHoursCombat = Math.max(0, o.get("cooldownHoursCombat").getAsDouble());
                if (o.has("cooldownHoursLife")) c.cooldownHoursLife = Math.max(0, o.get("cooldownHoursLife").getAsDouble());
                if (o.has("npcName")) c.npcName = o.get("npcName").getAsString();
                if (o.has("jobs") && o.get("jobs").isJsonArray()) {
                    Map<String, JobDef> defaults = new LinkedHashMap<>(c.jobs);
                    c.jobs.clear();
                    for (JsonElement e : o.getAsJsonArray("jobs")) {
                        try {
                            JobDef d = read(e.getAsJsonObject(), defaults);
                            c.jobs.put(d.id, d);
                        } catch (Exception ex) {
                            RtShop.LOGGER.warn("[rtshop] jobs.json 의 직업 하나를 읽지 못했습니다: {}", ex.getMessage());
                        }
                    }
                    // 파일에 없는 기본 직업(업데이트로 새로 생긴 것)은 기본값으로 붙인다
                    for (JobDef d : defaults.values()) {
                        c.jobs.putIfAbsent(d.id, d);
                    }
                }
            } else {
                Files.createDirectories(p.getParent());
                Files.writeString(p, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(c.toJson()),
                        StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            RtShop.LOGGER.error("[rtshop] jobs.json 을 읽지 못했습니다. 기본값을 씁니다.", e);
        }
        return c;
    }

    private static JobDef read(JsonObject o, Map<String, JobDef> defaults) {
        String id = o.get("id").getAsString().trim();
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
        if (o.has("desc") && o.get("desc").isJsonArray()) {
            for (JsonElement e : o.getAsJsonArray("desc")) {
                d.desc.add(e.getAsString());
            }
        } else if (base != null) {
            d.desc.addAll(base.desc);
        }
        if (base != null) {
            d.params.putAll(base.params);
        }
        if (o.has("params") && o.get("params").isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("params").entrySet()) {
                d.params.put(e.getKey(), e.getValue().getAsDouble());
            }
        }
        return d;
    }

    private JsonObject toJson() {
        JsonObject o = new JsonObject();
        JsonArray help = new JsonArray();
        help.add("직업 설정. 고친 뒤 /rtshop reload 로 적용됩니다.");
        help.add("changeCost*: 직업을 바꿀 때 드는 Gold (처음 고를 때는 무료) · cooldownHours*: 바꾼 뒤 다시 바꿀 수 있을 때까지의 시간(실제 시간, 접속하지 않아도 흐름)");
        help.add("jobs[].unlockFlag: 이 표식이 있어야 고를 수 있음 (선행 퀘스트의 보상 표식). 이 표식을 주는 퀘스트가 아직 없으면 바로 고를 수 있습니다.");
        help.add("jobs[].params: 패시브 수치 (0.10 = 10%). desc 는 화면에 보이는 설명이니 수치를 바꾸면 desc 도 같이 고쳐 주세요.");
        o.add("_help", help);
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
            JsonArray desc = new JsonArray();
            d.desc.forEach(desc::add);
            j.add("desc", desc);
            j.addProperty("unlockFlag", d.unlockFlag);
            j.addProperty("hidden", d.hidden);
            JsonObject params = new JsonObject();
            d.params.forEach(params::addProperty);
            j.add("params", params);
            arr.add(j);
        }
        o.add("jobs", arr);
        return o;
    }
}

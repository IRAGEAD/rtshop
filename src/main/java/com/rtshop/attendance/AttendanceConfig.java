package com.rtshop.attendance;

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
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * config/rtshop/attendance.json : 출석 보상 설정.
 * 게임 안 출석 편집기(/rtshop attendance edit, 출석 창의 '편집' 버튼)로 고칠 수 있고, 메모장으로 고친 뒤 /rtshop reload 해도 된다.
 */
public final class AttendanceConfig {
    /** 보상 하나: Gold 와 아이템들. */
    public static final class Reward {
        public long gold;
        public final List<Item> items = new ArrayList<>();
        /** 출석 창에서 강조할 날 (큰 보상). */
        public boolean special;

        public JsonObject toJson() {
            JsonObject o = new JsonObject();
            o.addProperty("gold", gold);
            JsonArray a = new JsonArray();
            for (Item i : items) {
                JsonObject io = new JsonObject();
                io.addProperty("item", i.item);
                io.addProperty("count", i.count);
                a.add(io);
            }
            o.add("items", a);
            if (special) {
                o.addProperty("special", true);
            }
            return o;
        }

        static Reward fromJson(JsonObject o) {
            Reward r = new Reward();
            r.gold = Math.max(0, o.has("gold") ? o.get("gold").getAsLong() : 0);
            r.special = o.has("special") && o.get("special").getAsBoolean();
            if (o.has("items") && o.get("items").isJsonArray()) {
                for (JsonElement e : o.getAsJsonArray("items")) {
                    JsonObject io = e.getAsJsonObject();
                    String id = io.has("item") ? io.get("item").getAsString().trim() : "";
                    int n = io.has("count") ? io.get("count").getAsInt() : 1;
                    if (!id.isEmpty() && n > 0) {
                        r.items.add(new Item(id, Math.min(n, 64 * 36)));
                    }
                }
            }
            return r;
        }
    }

    public record Item(String item, int count) {
    }

    /** 주간 보상 한 단계: 이번 주에 days 일 출석하면 받는다. */
    public static final class Weekly {
        public int days;
        public Reward reward = new Reward();
    }

    public String npcName = "시청 서기 하루";
    /** 날짜가 바뀌는 기준 (시간대와 시각). 기본: 대한민국 오전 6시. */
    public String timezone = "Asia/Seoul";
    public int resetHour = 6;
    /** 설정 파일 형식 버전 (2: 기본 기준 시각이 9시 → 6시로 바뀜). */
    private static final int CONFIG_VERSION = 2;
    /** 한 주가 시작하는 요일 (이 요일의 resetHour 에 주간 출석이 새로 시작). */
    public String weekStart = "MONDAY";
    /** 마지막 날까지 받으면 1일차부터 다시 (false 면 마지막 날 보상을 계속). */
    public boolean loop = true;
    /** 연속 출석 중 하루를 빠졌을 때 처음 한 번은 봐준다. */
    public boolean grace = true;
    /** true 면 '하루 봐주기'는 플레이어마다 처음 한 번뿐 (false 면 0일차로 초기화된 뒤 새 연속 출석에서 다시 한 번). */
    public boolean graceOnce = false;
    /** 복구권: 초기화된 뒤 몇 시간 안에 쓸 수 있는지, 한 번 쓴 뒤 다시 쓰기까지 몇 시간. */
    public double restoreWindowHours = 24;
    public double restoreCooldownHours = 24;
    /** 출석 창의 '출석 상점' 버튼이 여는 상점 ID (복구권을 판다). */
    /** 출석 NPC 창의 상점 단추 (""= 없음). 출석 상점은 출석 상점 주인 엘리에게만 있다 (3.27). */
    public String shop = "";
    public final List<Reward> days = new ArrayList<>();
    public final List<Weekly> weekly = new ArrayList<>();

    private static AttendanceConfig instance;

    public static synchronized AttendanceConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public static synchronized void reload() {
        instance = load();
    }

    public static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("attendance.json");
    }

    public ZoneId zone() {
        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            return ZoneId.of("Asia/Seoul");
        }
    }

    public DayOfWeek weekStartDay() {
        try {
            return DayOfWeek.valueOf(weekStart.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return DayOfWeek.MONDAY;
        }
    }

    // ------------------------------------------------------------------ 기본값

    private static Reward r(long gold, Object... items) {
        Reward x = new Reward();
        x.gold = gold;
        for (int i = 0; i + 1 < items.length; i += 2) {
            x.items.add(new Item((String) items[i], (Integer) items[i + 1]));
        }
        return x;
    }

    private static Reward special(Reward x) {
        x.special = true;
        return x;
    }

    /**
     * 기본 보상 (28일 주기). 생활 직업 수입이 시간당 약 20,000G 인 경제에 맞춰,
     * 평일은 잠깐 들러 받을 만한 소소한 Gold, 7일마다 큰 보상, 28일째에는 토지 문서.
     */
    public void defaults() {
        days.clear();
        days.add(r(500));
        days.add(r(500, "minecraft:bread", 8));
        days.add(r(800));
        days.add(r(800, "minecraft:torch", 32));
        days.add(r(1000));
        days.add(r(1000, "minecraft:iron_ingot", 8));
        days.add(special(r(3000, "minecraft:gold_ingot", 4)));
        days.add(r(800));
        days.add(r(800, "minecraft:cooked_beef", 8));
        days.add(r(1000));
        days.add(r(1000, "minecraft:coal", 16));
        days.add(r(1200));
        days.add(r(1200, "minecraft:iron_ingot", 12));
        days.add(special(r(5000, "minecraft:diamond", 1)));
        days.add(r(1000));
        days.add(r(1000, "minecraft:golden_carrot", 4));
        days.add(r(1200));
        days.add(r(1200, "minecraft:lapis_lazuli", 16));
        days.add(r(1500));
        days.add(r(1500, "minecraft:iron_ingot", 16));
        days.add(special(r(7000, "minecraft:diamond", 2)));
        days.add(r(1200));
        days.add(r(1200, "minecraft:golden_apple", 1));
        days.add(r(1500));
        days.add(r(1500, "minecraft:emerald", 4));
        days.add(r(2000));
        days.add(r(2000, "minecraft:diamond", 1));
        days.add(special(r(15000, "rtshop:land_deed", 1)));
        weekly.clear();
        weekly.add(week(3, r(2000)));
        weekly.add(week(5, r(4000, "minecraft:experience_bottle", 8)));
        weekly.add(week(7, special(r(8000, "minecraft:diamond", 2))));
    }

    private static Weekly week(int days, Reward r) {
        Weekly w = new Weekly();
        w.days = days;
        w.reward = r;
        return w;
    }

    // ------------------------------------------------------------------ 읽기 / 쓰기

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        JsonArray help = new JsonArray();
        help.add("출석 보상 설정. 게임 안에서 /rtshop attendance edit (또는 출석 창의 '편집')으로 고칠 수 있습니다.");
        help.add("timezone·resetHour: 출석 날짜가 바뀌는 기준 (기본: Asia/Seoul 오전 6시). weekStart: 주간 출석이 새로 시작하는 요일.");
        help.add("days: 1일차부터의 보상 {gold, items[{item, count}], special}. loop 가 true 면 마지막 날 다음은 다시 1일차.");
        help.add("weekly: 이번 주 출석 일수가 days 에 닿으면 받는 보상.");
        help.add("grace: 연속 출석 중 하루를 처음 빠졌을 때 한 번 봐주기. 그 뒤로는 하루만 빠져도 0일차로 초기화됩니다 (두 날 이상 빠지면 바로 초기화). graceOnce 가 true 면 봐주기는 플레이어마다 처음 한 번뿐, false 면 초기화된 뒤 새 연속 출석마다 한 번.");
        help.add("restoreWindowHours / restoreCooldownHours: 출석 복구권은 초기화된 때부터 이 시간 안에만 쓸 수 있고, 한 번 쓰면 이 시간 동안 다시 못 씁니다.");
        help.add("shop: 출석 창의 '출석 상점' 버튼이 여는 상점 (복구권 가격은 config/rtshop/shops/<shop>.json 에서).");
        o.add("_help", help);
        o.addProperty("configVersion", CONFIG_VERSION);
        o.addProperty("npcName", npcName);
        o.addProperty("timezone", timezone);
        o.addProperty("resetHour", resetHour);
        o.addProperty("weekStart", weekStart);
        o.addProperty("loop", loop);
        o.addProperty("grace", grace);
        o.addProperty("graceOnce", graceOnce);
        o.addProperty("restoreWindowHours", restoreWindowHours);
        o.addProperty("restoreCooldownHours", restoreCooldownHours);
        o.addProperty("shop", shop);
        o.addProperty("shopMoved", true);
        JsonArray d = new JsonArray();
        for (Reward r : days) {
            d.add(r.toJson());
        }
        o.add("days", d);
        JsonArray w = new JsonArray();
        for (Weekly x : weekly) {
            JsonObject wo = x.reward.toJson();
            wo.addProperty("days", x.days);
            w.add(wo);
        }
        o.add("weekly", w);
        return o;
    }

    /** JSON 에서 읽는다. 형식이 틀리면 IllegalArgumentException (편집기 저장 검사에도 쓴다). */
    public static AttendanceConfig fromJson(JsonObject o) {
        AttendanceConfig c = new AttendanceConfig();
        if (o.has("npcName")) c.npcName = o.get("npcName").getAsString();
        if (o.has("timezone")) c.timezone = o.get("timezone").getAsString().trim();
        if (o.has("resetHour")) c.resetHour = o.get("resetHour").getAsInt();
        if (o.has("weekStart")) c.weekStart = o.get("weekStart").getAsString().trim().toUpperCase(Locale.ROOT);
        if (o.has("loop")) c.loop = o.get("loop").getAsBoolean();
        if (o.has("grace")) c.grace = o.get("grace").getAsBoolean();
        if (o.has("graceOnce")) c.graceOnce = o.get("graceOnce").getAsBoolean();
        if (o.has("restoreWindowHours")) c.restoreWindowHours = o.get("restoreWindowHours").getAsDouble();
        if (o.has("restoreCooldownHours")) c.restoreCooldownHours = o.get("restoreCooldownHours").getAsDouble();
        if (o.has("shop")) c.shop = o.get("shop").getAsString().trim();
        if (c.npcName.isBlank()) {
            throw new IllegalArgumentException("NPC 이름이 비어 있습니다");
        }
        try {
            ZoneId.of(c.timezone);
        } catch (Exception e) {
            throw new IllegalArgumentException("시간대를 알 수 없습니다: " + c.timezone + " (예: Asia/Seoul)");
        }
        if (c.resetHour < 0 || c.resetHour > 23) {
            throw new IllegalArgumentException("기준 시각은 0~23 시 사이여야 합니다: " + c.resetHour);
        }
        try {
            DayOfWeek.valueOf(c.weekStart);
        } catch (Exception e) {
            throw new IllegalArgumentException("요일을 알 수 없습니다: " + c.weekStart + " (예: MONDAY)");
        }
        if (c.restoreWindowHours < 0 || c.restoreCooldownHours < 0) {
            throw new IllegalArgumentException("복구권 시간은 0 이상이어야 합니다");
        }
        if (o.has("days") && o.get("days").isJsonArray()) {
            for (JsonElement e : o.getAsJsonArray("days")) {
                c.days.add(Reward.fromJson(e.getAsJsonObject()));
            }
        }
        if (c.days.isEmpty()) {
            throw new IllegalArgumentException("일일 보상이 하루도 없습니다");
        }
        if (o.has("weekly") && o.get("weekly").isJsonArray()) {
            for (JsonElement e : o.getAsJsonArray("weekly")) {
                JsonObject wo = e.getAsJsonObject();
                Weekly w = new Weekly();
                w.days = wo.has("days") ? wo.get("days").getAsInt() : 0;
                if (w.days < 1 || w.days > 7) {
                    throw new IllegalArgumentException("주간 보상의 출석 일수는 1~7 이어야 합니다: " + w.days);
                }
                w.reward = Reward.fromJson(wo);
                c.weekly.add(w);
            }
        }
        c.weekly.sort((a, b) -> Integer.compare(a.days, b.days));
        return c;
    }

    public void save() throws java.io.IOException {
        Path p = path();
        Files.createDirectories(p.getParent());
        Files.writeString(p, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(toJson()), StandardCharsets.UTF_8);
    }

    private static AttendanceConfig load() {
        Path p = path();
        try {
            if (Files.exists(p)) {
                JsonObject o = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
                AttendanceConfig c = fromJson(o);
                // 3.27: 출석 상점 기능은 출석 상점 주인(엘리)에게만. 예전 기본값이면 시청 서기 창의 상점 단추를 뺀다.
                if (!o.has("shopMoved") && "attendance_shop".equals(c.shop)) {
                    c.shop = "";
                    c.save();
                }
                // 3.7.0 이 만든 파일(기준 시각 9시)은 새 기본값 6시로 바꾼다. 보상 등 다른 값은 그대로.
                if (!o.has("configVersion") && c.resetHour == 9) {
                    c.resetHour = 6;
                    c.save();
                    RtShop.LOGGER.info("[rtshop] 출석 기준 시각을 오전 9시에서 오전 6시로 바꿨습니다 (attendance.json).");
                } else if (!o.has("configVersion")) {
                    c.save();
                }
                return c;
            }
        } catch (Exception e) {
            RtShop.LOGGER.error("[rtshop] attendance.json 을 읽지 못해 기본 출석 보상을 씁니다 (파일은 그대로 둡니다): {}", e.getMessage());
            AttendanceConfig c = new AttendanceConfig();
            c.defaults();
            c.loadError = e.getMessage();
            return c;
        }
        AttendanceConfig c = new AttendanceConfig();
        c.defaults();
        try {
            c.save();
        } catch (Exception e) {
            RtShop.LOGGER.error("[rtshop] attendance.json 을 만들지 못했습니다", e);
        }
        return c;
    }

    /** 파일을 읽지 못했을 때의 이유 (없으면 null). */
    public String loadError;
}

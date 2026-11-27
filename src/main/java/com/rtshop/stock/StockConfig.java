package com.rtshop.stock;

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
import java.util.List;

/**
 * config/rtshop/stocks.json : 조합 증서(주식) 설정 (3.72.0). 고친 뒤 /rtshop stocks reload.
 * 기본값은 사용자가 고른 것: 조합 여섯 곳 · 하루 변동 ±10% · 수수료 2%.
 */
public final class StockConfig {
    /** 조합 하나. */
    public static final class Union {
        public String id = "";
        public String name = "", nameEn = "";
        public long startPrice = 500;
        /** 시세판의 선 색 (0xRRGGBB). */
        public int color = 0x3A7A6A;

        Union() {
        }

        Union(String id, String name, String nameEn, long startPrice, int color) {
            this.id = id;
            this.name = name;
            this.nameEn = nameEn;
            this.startPrice = startPrice;
            this.color = color;
        }
    }

    /** 이야기 소식: 그 퀘스트를 서버에서 처음 누군가 마치면 다음 시세에 한 번 반영된다. */
    public static final class News {
        public String quest = "", union = "";
        /** 시세에 더하는 몫 (0.08 = +8%). 그날의 하루 변동 한도 안에서. */
        public double change;
        public String text = "", textEn = "";

        News() {
        }

        News(String quest, String union, double change, String text, String textEn) {
            this.quest = quest;
            this.union = union;
            this.change = change;
            this.text = text;
            this.textEn = textEn;
        }
    }

    /**
     * 무작위 시장 사건 (3.73.0): 날이 바뀔 때 eventChance 확률로 조합 하나에 생긴다. min ~ max 사이의 몫(내리는 사건은 음수)이 그날 시세에 더해지고,
     * followUp 이 있으면 다음 날 그 몫의 followUp 배가 한 번 더 (-0.5 = 반쯤 되돌아옴). when: "" = 아무 때나, "thunder" = 전날 뇌우가 30분 넘게, "weekend" = 토요일에.
     */
    public static final class Event {
        public String id = "", union = "", when = "";
        public double min, max, weight = 1, followUp;
        public String text = "", textEn = "";

        Event() {
        }

        Event(String id, String union, double min, double max, double followUp, String when, String text, String textEn) {
            this.id = id;
            this.union = union;
            this.min = min;
            this.max = max;
            this.followUp = followUp;
            this.when = when;
            this.text = text;
            this.textEn = textEn;
        }
    }

    /**
     * 위조 증서와 감정 (3.76.0, ④ 히든 직업 위조자 · 조사관). 수치는 모두 0~1 의 몫.
     * 거래소가 가짜를 통과시킬 확률 = 완성도 - exchangeEye (0.02 ~ 0.9). 드러나면 위조자에게 벌금(그날 액면가 × fineRate, 지갑 → 예금 순),
     * 조사관이 드러냈으면 걷힌 벌금의 finderShare 를 조사관에게, 나머지는 기금으로. 같은 위조자의 가짜가 wantedWindowDays 안에 wantedAfter 번 드러나면
     * wantedDays 동안 수배(시세판 공고 · 거래소 거래 금지).
     */
    public static final class Forgery {
        public boolean enabled = true;
        public double exchangeEye = 0.25;
        public double checkFeeRate = 0.01;
        public long checkFeeMin = 100;
        public double fineRate = 0.30, finderShare = 0.5;
        public int wantedAfter = 3, wantedWindowDays = 14, wantedDays = 7;
        public double baseQuality = 0.50, glowInkBonus = 0.10, qualitySpread = 0.08, copyPenalty = 0.10;
        public double inspectBase = 0.35, inspectFlawWeight = 0.6;
    }

    public final Forgery forgery = new Forgery();

    public boolean enabled = true;
    /** 무작위 시장 사건을 켜는가 · 하루에 일어날 확률 · 조건 사건(뇌우 · 주말)이 조건을 채웠을 때 일어날 확률. */
    public boolean events = true;
    public double eventChance = 0.30, conditionChance = 0.60;
    public final List<Event> eventList = new ArrayList<>();
    /** 살 때 · 팔 때 수수료 (기금으로 간다). */
    public double fee = 0.02;
    /** 하루 시세 변동의 한도 (0.10 = ±10%). */
    public double dailyMove = 0.10;
    public long minPrice = 50, maxPrice = 50_000;
    /** 한 사람이 하루에 살 수 있는 주 수 (모든 조합을 합해). */
    public int buyPerDay = 200;
    /** 증서 한 장에 담는 주 수의 한도. */
    public int maxPerCertificate = 100;
    /** 한 사람이 하루 실적에 보탤 수 있는 Gold 의 상한 (혼자 팔았다 샀다 해서 시세를 끌어올리지 못하게). */
    public long playerDailyCap = 30_000;
    /** 실적 비교의 바닥: 평소 실적이 이보다 작으면 이 값과 견준다 (사람이 적을 때 출렁이지 않게). */
    public long metricFloor = 5_000;
    /** 실적의 무게: 오늘 실적이 평소의 두 배면 +perfWeight (기본 +6%). */
    public double perfWeight = 0.06;
    /** 날마다 섞이는 작은 무작위 (표준편차). */
    public double noise = 0.02;
    /** 처음 값으로 돌아가려는 힘 (값이 처음의 2배면 하루 약 -4%). */
    public double pull = 0.06;
    /** 한 주에 기금의 얼마를 배당으로 나눌지. */
    public double dividendPool = 0.5;
    /** 한 주 배당은 시세의 이만큼까지 (0.02 = 2%). */
    public double dividendCap = 0.02;
    /** 거래 창구(은행원)와의 거리 (칸). */
    public double deskRange = 8.0;

    public final List<Union> unions = new ArrayList<>();
    public final List<News> news = new ArrayList<>();

    static StockConfig defaults() {
        StockConfig c = new StockConfig();
        c.unions.add(new Union("farm", "농업 조합", "Farmers' Union", 400, 0x7FA23A));
        c.unions.add(new Union("fish", "어업 조합", "Fishers' Union", 400, 0x3F7FB8));
        c.unions.add(new Union("smith", "대장간 조합", "Smiths' Union", 600, 0xB0603A));
        c.unions.add(new Union("food", "식품 조합", "Grocers' Union", 400, 0xC8963A));
        c.unions.add(new Union("land", "토지 조합", "Land Union", 800, 0x8A6A4A));
        c.unions.add(new Union("survey", "조사대 기금", "Survey Corps Fund", 1000, 0x7A4AA0));
        c.news.add(new News("story_1_39", "survey", 0.06, "조사대가 끝의 섬에서 돌아왔다", "The Survey Corps is back from the End"));
        c.news.add(new News("story_2_15", "fish", 0.03, "얼음을 품은 거인이 쓰러져 북쪽 물길이 열렸다", "The ice giant fell; the northern waters are open"));
        c.news.add(new News("story_2_26", "smith", 0.04, "녹슨 맹세: 대장간에 주문이 몰린다", "A rusted oath: orders pile up at the smithy"));
        c.news.add(new News("story_3_01", "fish", -0.06, "바다가 얼어 그물이 비었다", "The sea froze; the nets come up empty"));
        c.news.add(new News("story_3_07", "food", 0.03, "얼음 길 채비로 식품 주문이 늘었다", "Supplies for the ice road: food orders rise"));
        c.news.add(new News("story_3_38", "fish", 0.08, "얼어붙은 바다가 풀렸다 — 다시 던지는 그물", "The frozen sea has thawed — nets go out again"));
        c.news.add(new News("story_4_02", "survey", -0.05, "첫 봉인이 깨어났다", "The first seal has woken"));
        c.news.add(new News("story_4_05", "smith", 0.05, "식은 화로에 다시 불이 들어왔다", "The cold forge burns again"));
        c.news.add(new News("story_4_10b", "farm", 0.05, "문 안의 정원에서 씨앗 한 알을 가져왔다", "A seed from the garden behind the gate"));
        c.news.add(new News("story_5_08", "survey", 0.08, "화염의 군주가 쓰러졌다", "The Flame Lord has fallen"));
        c.news.add(new News("story_5_08", "land", 0.04, "화염의 군주가 쓰러져 땅값이 오른다", "With the Flame Lord gone, land is in demand"));
        c.news.add(new News("story_5_12", "fish", 0.03, "폭풍이 그친 부두", "The storm has passed over the pier"));
        c.news.add(new News("story_5_12", "farm", 0.02, "폭풍이 그친 마을의 첫 수확", "The first harvest after the storm"));
        c.news.add(new News("hidden_scylla_5", "fish", 0.04, "폭풍의 여제가 잠들었다", "The Storm Empress sleeps"));
        // 무작위 시장 사건 (3.73.0, 사용자 22:55 "스토리 외에도 시세를 랜덤하게 바꾸는 요소(이벤트)도 있으면 좋겠어")
        c.eventList.add(new Event("farm_bumper", "farm", 0.03, 0.07, -0.3, "", "풍년 소식: 올해 밀이 잘 여물었다", "Bumper crop: the wheat ripened well this year"));
        c.eventList.add(new Event("farm_blight", "farm", -0.07, -0.03, -0.3, "", "병충해 소문이 돈다", "Rumours of blight in the fields"));
        c.eventList.add(new Event("farm_seeds", "farm", 0.03, 0.05, 0, "", "먼 마을에서 씨앗 주문이 들어왔다", "A far village ordered seeds"));
        c.eventList.add(new Event("fish_shoal", "fish", 0.04, 0.08, -0.4, "", "큰 물고기 떼가 부두 가까이 왔다", "A great shoal came close to the pier"));
        c.eventList.add(new Event("fish_nets", "fish", -0.04, -0.02, 0, "", "그물 값이 올랐다", "Nets got pricier"));
        c.eventList.add(new Event("fish_salt", "fish", 0.02, 0.04, 0, "", "소금 값이 내려 절임 생선이 잘 팔린다", "Salt is cheap; salted fish sells well"));
        c.eventList.add(new Event("fish_storm", "fish", -0.06, -0.03, -0.5, "thunder", "폭풍 경보: 배들이 묶였다", "Storm warning: the boats stay tied up"));
        c.eventList.add(new Event("smith_vein", "smith", 0.04, 0.08, -0.3, "", "새 광맥이 발견되었다", "A new ore vein was found"));
        c.eventList.add(new Event("smith_collapse", "smith", -0.07, -0.04, -0.4, "", "광산 갱도가 무너졌다", "A mine tunnel collapsed"));
        c.eventList.add(new Event("smith_order", "smith", 0.03, 0.05, 0, "", "수도회가 무기를 대량으로 주문했다", "The Order placed a large weapons order"));
        c.eventList.add(new Event("smith_rival", "smith", -0.04, -0.02, 0, "", "외지 대장장이가 마을에 들어왔다", "An outside smith set up shop in town"));
        c.eventList.add(new Event("food_festival", "food", 0.04, 0.07, -0.5, "", "마을 축제 준비로 음식 주문이 늘었다", "Festival preparations: food orders are up"));
        c.eventList.add(new Event("food_poison", "food", -0.07, -0.04, -0.5, "", "식중독 소문이 돈다", "Rumours of food poisoning"));
        c.eventList.add(new Event("food_market", "food", 0.02, 0.04, 0, "weekend", "주말 장터가 열렸다", "The weekend market is open"));
        c.eventList.add(new Event("land_boom", "land", 0.04, 0.08, -0.6, "", "땅 투기 열풍이 분다", "A land rush is on"));
        c.eventList.add(new Event("land_dispute", "land", -0.06, -0.03, -0.3, "", "경계 분쟁으로 거래가 멈췄다", "A boundary dispute froze the trades"));
        c.eventList.add(new Event("land_road", "land", 0.03, 0.05, 0, "", "새 길이 닦인다는 소문", "Word of a new road being built"));
        c.eventList.add(new Event("survey_haul", "survey", 0.04, 0.08, -0.3, "", "원정대가 큰 수확을 안고 돌아왔다", "An expedition came back with a great haul"));
        c.eventList.add(new Event("survey_attack", "survey", -0.06, -0.03, -0.4, "", "괴물 습격 소식이 들려온다", "News of a monster attack"));
        c.eventList.add(new Event("survey_patron", "survey", 0.02, 0.04, 0, "", "후원자가 조사대를 크게 후원했다", "A patron gave the Survey Corps a large gift"));
        return c;
    }

    public Union union(String id) {
        for (Union u : unions) if (u.id.equals(id)) return u;
        return null;
    }

    public int index(String id) {
        for (int i = 0; i < unions.size(); i++) if (unions.get(i).id.equals(id)) return i;
        return -1;
    }

    private static StockConfig instance;

    public static synchronized StockConfig get() {
        if (instance == null) instance = load();
        return instance;
    }

    public static synchronized void reload() {
        instance = load();
    }

    /** 점검용: 설정을 바로 바꾼다 (null = 파일에서 다시). */
    public static synchronized void forTest(StockConfig c) {
        instance = c == null ? load() : c;
    }

    public static StockConfig copyOfDefaults() {
        return defaults();
    }

    static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("stocks.json");
    }

    private static StockConfig load() {
        StockConfig c = defaults();
        Path f = file();
        try {
            if (Files.exists(f)) {
                JsonObject o = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
                read(c, o);
                if ((!o.has("eventList") || !o.has("forgery")) && !Boolean.getBoolean("rtshop.selftest") && !Boolean.getBoolean("rtshop.clienttest")) {
                    // 3.72.0 에서 만든 파일: 고친 값은 그대로 두고 새로 생긴 사건 목록을 적어 넣는다 (볼 수 있고 고칠 수 있게)
                    Files.writeString(f, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(write(c)), StandardCharsets.UTF_8);
                }
                return c;
            }
            if (Boolean.getBoolean("rtshop.selftest") || Boolean.getBoolean("rtshop.clienttest")) return c;
            Files.createDirectories(f.getParent());
            Files.writeString(f, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(write(c)), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            RtShop.LOGGER.warn("[rtshop] stocks.json 을 읽지 못했습니다: {}", ex.toString());
        }
        return c;
    }

    static void read(StockConfig c, JsonObject o) {
        if (o.has("enabled")) c.enabled = o.get("enabled").getAsBoolean();
        if (o.has("fee")) c.fee = clamp(o.get("fee").getAsDouble(), 0, 0.5);
        if (o.has("dailyMove")) c.dailyMove = clamp(o.get("dailyMove").getAsDouble(), 0.01, 0.5);
        if (o.has("minPrice")) c.minPrice = Math.max(1, o.get("minPrice").getAsLong());
        if (o.has("maxPrice")) c.maxPrice = Math.max(c.minPrice, o.get("maxPrice").getAsLong());
        if (o.has("buyPerDay")) c.buyPerDay = Math.max(1, Math.min(100_000, o.get("buyPerDay").getAsInt()));
        if (o.has("maxPerCertificate")) c.maxPerCertificate = Math.max(1, Math.min(10_000, o.get("maxPerCertificate").getAsInt()));
        if (o.has("playerDailyCap")) c.playerDailyCap = Math.max(0, o.get("playerDailyCap").getAsLong());
        if (o.has("metricFloor")) c.metricFloor = Math.max(1, o.get("metricFloor").getAsLong());
        if (o.has("perfWeight")) c.perfWeight = clamp(o.get("perfWeight").getAsDouble(), 0, 0.5);
        if (o.has("noise")) c.noise = clamp(o.get("noise").getAsDouble(), 0, 0.2);
        if (o.has("pull")) c.pull = clamp(o.get("pull").getAsDouble(), 0, 0.5);
        if (o.has("dividendPool")) c.dividendPool = clamp(o.get("dividendPool").getAsDouble(), 0, 1);
        if (o.has("dividendCap")) c.dividendCap = clamp(o.get("dividendCap").getAsDouble(), 0, 0.5);
        if (o.has("deskRange")) c.deskRange = clamp(o.get("deskRange").getAsDouble(), 2, 64);
        if (o.has("unions") && o.get("unions").isJsonArray()) {
            List<Union> list = new ArrayList<>();
            for (JsonElement e : o.getAsJsonArray("unions")) {
                JsonObject u = e.getAsJsonObject();
                Union x = new Union();
                x.id = u.has("id") ? u.get("id").getAsString().trim() : "";
                if (x.id.isEmpty() || !x.id.matches("[a-z0-9_]{1,24}")) continue;
                boolean dup = false;
                for (Union y : list) dup |= y.id.equals(x.id);
                if (dup) continue;
                x.name = u.has("name") ? u.get("name").getAsString() : x.id;
                x.nameEn = u.has("nameEn") ? u.get("nameEn").getAsString() : x.name;
                x.startPrice = Math.max(c.minPrice, Math.min(c.maxPrice, u.has("startPrice") ? u.get("startPrice").getAsLong() : 500));
                if (u.has("color")) {
                    try {
                        x.color = Integer.parseInt(u.get("color").getAsString().replace("#", ""), 16) & 0xFFFFFF;
                    } catch (Exception ignored) {
                    }
                }
                list.add(x);
                if (list.size() >= 12) break;
            }
            if (!list.isEmpty()) {
                c.unions.clear();
                c.unions.addAll(list);
            }
        }
        if (o.has("events")) c.events = o.get("events").getAsBoolean();
        if (o.has("eventChance")) c.eventChance = clamp(o.get("eventChance").getAsDouble(), 0, 1);
        if (o.has("conditionChance")) c.conditionChance = clamp(o.get("conditionChance").getAsDouble(), 0, 1);
        if (o.has("eventList") && o.get("eventList").isJsonArray()) {
            c.eventList.clear();
            for (JsonElement e : o.getAsJsonArray("eventList")) {
                JsonObject n = e.getAsJsonObject();
                Event x = new Event();
                x.id = n.has("id") ? n.get("id").getAsString() : "";
                x.union = n.has("union") ? n.get("union").getAsString() : "";
                x.when = n.has("when") ? n.get("when").getAsString() : "";
                double a = n.has("min") ? clamp(n.get("min").getAsDouble(), -0.5, 0.5) : 0, b = n.has("max") ? clamp(n.get("max").getAsDouble(), -0.5, 0.5) : a;
                x.min = Math.min(a, b);
                x.max = Math.max(a, b);
                x.weight = n.has("weight") ? clamp(n.get("weight").getAsDouble(), 0, 100) : 1;
                x.followUp = n.has("followUp") ? clamp(n.get("followUp").getAsDouble(), -1, 1) : 0;
                x.text = n.has("text") ? n.get("text").getAsString() : "";
                x.textEn = n.has("textEn") ? n.get("textEn").getAsString() : x.text;
                if (!x.text.isEmpty() && c.union(x.union) != null) c.eventList.add(x);
            }
        }
        if (o.has("forgery") && o.get("forgery").isJsonObject()) {
            JsonObject g = o.getAsJsonObject("forgery");
            Forgery f = c.forgery;
            if (g.has("enabled")) f.enabled = g.get("enabled").getAsBoolean();
            if (g.has("exchangeEye")) f.exchangeEye = clamp(g.get("exchangeEye").getAsDouble(), 0, 1);
            if (g.has("checkFeeRate")) f.checkFeeRate = clamp(g.get("checkFeeRate").getAsDouble(), 0, 0.5);
            if (g.has("checkFeeMin")) f.checkFeeMin = Math.max(0, g.get("checkFeeMin").getAsLong());
            if (g.has("fineRate")) f.fineRate = clamp(g.get("fineRate").getAsDouble(), 0, 5);
            if (g.has("finderShare")) f.finderShare = clamp(g.get("finderShare").getAsDouble(), 0, 1);
            if (g.has("wantedAfter")) f.wantedAfter = Math.max(1, Math.min(100, g.get("wantedAfter").getAsInt()));
            if (g.has("wantedWindowDays")) f.wantedWindowDays = Math.max(1, Math.min(365, g.get("wantedWindowDays").getAsInt()));
            if (g.has("wantedDays")) f.wantedDays = Math.max(0, Math.min(365, g.get("wantedDays").getAsInt()));
            if (g.has("baseQuality")) f.baseQuality = clamp(g.get("baseQuality").getAsDouble(), 0, 1);
            if (g.has("glowInkBonus")) f.glowInkBonus = clamp(g.get("glowInkBonus").getAsDouble(), 0, 1);
            if (g.has("qualitySpread")) f.qualitySpread = clamp(g.get("qualitySpread").getAsDouble(), 0, 0.5);
            if (g.has("copyPenalty")) f.copyPenalty = clamp(g.get("copyPenalty").getAsDouble(), 0, 1);
            if (g.has("inspectBase")) f.inspectBase = clamp(g.get("inspectBase").getAsDouble(), 0, 1);
            if (g.has("inspectFlawWeight")) f.inspectFlawWeight = clamp(g.get("inspectFlawWeight").getAsDouble(), 0, 2);
        }
        if (o.has("news") && o.get("news").isJsonArray()) {
            c.news.clear();
            for (JsonElement e : o.getAsJsonArray("news")) {
                JsonObject n = e.getAsJsonObject();
                News x = new News();
                x.quest = n.has("quest") ? n.get("quest").getAsString() : "";
                x.union = n.has("union") ? n.get("union").getAsString() : "";
                x.change = n.has("change") ? clamp(n.get("change").getAsDouble(), -0.5, 0.5) : 0;
                x.text = n.has("text") ? n.get("text").getAsString() : "";
                x.textEn = n.has("textEn") ? n.get("textEn").getAsString() : x.text;
                if (!x.quest.isEmpty() && c.union(x.union) != null) c.news.add(x);
            }
        }
    }

    static JsonObject write(StockConfig c) {
        JsonObject o = new JsonObject();
        o.addProperty("_help", "조합 증서(주식): fee = 살 때·팔 때 수수료(기금으로), dailyMove = 하루 시세 변동 한도, minPrice/maxPrice = 시세의 바닥·천장, "
                + "buyPerDay = 한 사람이 하루에 살 수 있는 주 수, maxPerCertificate = 증서 한 장의 주 수 한도, playerDailyCap = 한 사람이 하루 실적에 보탤 수 있는 Gold, "
                + "metricFloor = 실적 비교의 바닥, perfWeight = 실적의 무게, noise = 작은 무작위, pull = 처음 값으로 돌아가려는 힘, "
                + "dividendPool = 한 주에 기금의 얼마를 배당으로 나눌지, dividendCap = 한 주 배당의 시세 대비 한도, deskRange = 거래 창구(은행원)와의 거리. "
                + "unions = 조합(id · 이름 · 처음 값 · 색), news = 이야기 소식(그 퀘스트를 서버에서 처음 마치면 다음 시세에 한 번), "
                + "events · eventChance = 무작위 시장 사건을 켜는가 · 하루에 일어날 확률, conditionChance = 조건 사건이 조건을 채웠을 때의 확률, "
                + "eventList = 사건(union · min ~ max 몫(내리는 것은 음수) · weight 뽑힐 무게 · followUp 다음 날 그 몫의 몇 배(-0.5 = 반쯤 되돌아옴) · when \"\" 아무 때나 / thunder 전날 뇌우 30분 넘게 / weekend 토요일). "
                + "날짜는 출석과 같은 기준 시각에 바뀐다. 고친 뒤 /rtshop stocks reload.");
        o.addProperty("enabled", c.enabled);
        o.addProperty("fee", c.fee);
        o.addProperty("dailyMove", c.dailyMove);
        o.addProperty("minPrice", c.minPrice);
        o.addProperty("maxPrice", c.maxPrice);
        o.addProperty("buyPerDay", c.buyPerDay);
        o.addProperty("maxPerCertificate", c.maxPerCertificate);
        o.addProperty("playerDailyCap", c.playerDailyCap);
        o.addProperty("metricFloor", c.metricFloor);
        o.addProperty("perfWeight", c.perfWeight);
        o.addProperty("noise", c.noise);
        o.addProperty("pull", c.pull);
        o.addProperty("dividendPool", c.dividendPool);
        o.addProperty("dividendCap", c.dividendCap);
        o.addProperty("deskRange", c.deskRange);
        JsonArray us = new JsonArray();
        for (Union u : c.unions) {
            JsonObject x = new JsonObject();
            x.addProperty("id", u.id);
            x.addProperty("name", u.name);
            x.addProperty("nameEn", u.nameEn);
            x.addProperty("startPrice", u.startPrice);
            x.addProperty("color", String.format("#%06X", u.color));
            us.add(x);
        }
        o.add("unions", us);
        JsonArray ns = new JsonArray();
        for (News n : c.news) {
            JsonObject x = new JsonObject();
            x.addProperty("quest", n.quest);
            x.addProperty("union", n.union);
            x.addProperty("change", n.change);
            x.addProperty("text", n.text);
            x.addProperty("textEn", n.textEn);
            ns.add(x);
        }
        o.add("news", ns);
        o.addProperty("events", c.events);
        o.addProperty("eventChance", c.eventChance);
        o.addProperty("conditionChance", c.conditionChance);
        JsonArray es = new JsonArray();
        for (Event e : c.eventList) {
            JsonObject x = new JsonObject();
            x.addProperty("id", e.id);
            x.addProperty("union", e.union);
            x.addProperty("min", e.min);
            x.addProperty("max", e.max);
            x.addProperty("weight", e.weight);
            x.addProperty("followUp", e.followUp);
            x.addProperty("when", e.when);
            x.addProperty("text", e.text);
            x.addProperty("textEn", e.textEn);
            es.add(x);
        }
        o.add("eventList", es);
        JsonObject g = new JsonObject();
        Forgery f = c.forgery;
        g.addProperty("_help", "위조 증서와 감정 (히든 직업 위조자 · 조사관): exchangeEye = 거래소의 눈 (가짜가 통과할 확률 = 완성도 - 이 값), "
                + "checkFeeRate · checkFeeMin = 거래소의 봉랍 확인 수수료 (액면가의 몫, 최소 Gold), fineRate = 가짜가 드러나면 위조자가 내는 벌금 (액면가의 몫), "
                + "finderShare = 조사관이 드러냈을 때 그 벌금에서 조사관이 받는 몫, wantedAfter · wantedWindowDays · wantedDays = 며칠 안에 몇 번 드러나면 며칠 동안 수배, "
                + "baseQuality · glowInkBonus · qualitySpread · copyPenalty = 가짜의 완성도 (기본 · 발광 먹물 · 흔들림 · 가짜를 본으로 떴을 때 깎이는 몫), "
                + "inspectBase · inspectFlawWeight = 조사관이 흠을 찾을 확률 (기본 + 직업 솜씨 + (1 - 완성도) × 이 값).");
        g.addProperty("enabled", f.enabled);
        g.addProperty("exchangeEye", f.exchangeEye);
        g.addProperty("checkFeeRate", f.checkFeeRate);
        g.addProperty("checkFeeMin", f.checkFeeMin);
        g.addProperty("fineRate", f.fineRate);
        g.addProperty("finderShare", f.finderShare);
        g.addProperty("wantedAfter", f.wantedAfter);
        g.addProperty("wantedWindowDays", f.wantedWindowDays);
        g.addProperty("wantedDays", f.wantedDays);
        g.addProperty("baseQuality", f.baseQuality);
        g.addProperty("glowInkBonus", f.glowInkBonus);
        g.addProperty("qualitySpread", f.qualitySpread);
        g.addProperty("copyPenalty", f.copyPenalty);
        g.addProperty("inspectBase", f.inspectBase);
        g.addProperty("inspectFlawWeight", f.inspectFlawWeight);
        o.add("forgery", g);
        return o;
    }

    private static double clamp(double v, double lo, double hi) {
        return Double.isNaN(v) ? lo : Math.max(lo, Math.min(hi, v));
    }
}

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

    public boolean enabled = true;
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
                + "unions = 조합(id · 이름 · 처음 값 · 색), news = 이야기 소식(그 퀘스트를 서버에서 처음 마치면 다음 시세에 한 번). 날짜는 출석과 같은 기준 시각에 바뀐다. 고친 뒤 /rtshop stocks reload.");
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
        return o;
    }

    private static double clamp(double v, double lo, double hi) {
        return Double.isNaN(v) ? lo : Math.max(lo, Math.min(hi, v));
    }
}

package com.rtshop.stock;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 조합 증서의 장부 (world/data/rtshop_stocks.dat, 3.72.0): 조합마다 시세 · 지난 시세 · 실적, 수수료 기금, 발행한 증서(일련번호 → 조합 · 주 수 · 발행일 · 배당 받은 주),
 * 이미 반영한 이야기 소식, 오늘 산 주 수.
 */
public final class StockData extends SavedData {
    private static final String NAME = "rtshop_stocks";
    static final int HISTORY = 30, METRIC_DAYS = 14, NEWS_LOG = 20;

    /** 조합 하나의 상태. */
    public static final class Board {
        public long price;
        /** 지난 종가 (오래된 것부터, 마지막이 어제). */
        public final List<Long> history = new ArrayList<>();
        /** 지난 실적 (사람이 있던 날만, 오래된 것부터). */
        public final List<Long> metrics = new ArrayList<>();
        /** 오늘 사람마다 보탠 실적 (Gold). */
        public final Map<UUID, Long> today = new HashMap<>();
        public long weekMetric, lastWeekMetric;
        /** 이번 주 배당 (한 주당 Gold). */
        public long dividend;
        /** 어제 시세가 바뀐 몫 (0.03 = +3%). */
        public double lastChange;
        /** 다음 시세에 더할 이야기 소식 · 사건. */
        public double pendingNews;
        /** 그다음 날 시세에 더할 사건의 뒷몫 (3.73.0: 오늘 오르고 내일 반쯤 되돌아오는 것). */
        public double pendingNext;

        long todayTotal(long cap) {
            long t = 0;
            for (long v : today.values()) t += cap > 0 ? Math.min(cap, v) : v;
            return t;
        }
    }

    /** 발행한 증서 한 장. */
    public static final class Cert {
        public String union = "";
        public int shares;
        public long issuedDay, paidWeek = Long.MIN_VALUE, no, price;
        public UUID buyer;
        public String buyerName = "";
    }

    /**
     * 위조 증서 한 장 (3.76.0): 장부(certs)에는 없고 여기에만 있다. 아이템은 진짜와 똑같이 생겼고 (일련번호는 본으로 삼은 증서의 것),
     * 위조인지는 서버의 이 표만 안다. 드러나거나 거래소가 사들이면 이 표에서 지운다.
     */
    public static final class Forged {
        public String union = "";
        public int shares;
        public long issuedDay, no, price, madeDay;
        public double quality;
        public UUID forger;
        public String forgerName = "";
    }

    /** 시세판의 소식 한 줄. */
    public static final class NewsLine {
        public long day;
        public String union = "", text = "", textEn = "";
        public double change;
        /** "story" = 이야기 소식, "event" = 무작위 시장 사건 (3.73.0). */
        public String kind = "story";
    }

    public final Map<String, Board> boards = new LinkedHashMap<>();
    public final Map<UUID, Cert> certs = new HashMap<>();
    public final Set<String> newsDone = new HashSet<>();
    public final List<NewsLine> newsLog = new ArrayList<>();
    public final Map<UUID, Integer> boughtToday = new HashMap<>();
    /** 위조 증서 (아이템 id → 기록), 위조자마다 드러난 날들, 수배가 끝나는 날(그날까지), 오늘 떠낸 수(forgeDay 의 것), 조사관이 살핀 증서("조사관|증서" → 날). */
    public final Map<UUID, Forged> forged = new HashMap<>();
    public final Map<UUID, List<Long>> caught = new HashMap<>();
    public final Map<UUID, Long> wanted = new HashMap<>();
    public final Map<UUID, String> wantedNames = new HashMap<>();
    public final Map<UUID, Integer> forgedToday = new HashMap<>();
    public long forgeDay = Long.MIN_VALUE;
    public final Map<String, Long> appraised = new HashMap<>();
    /** 지금까지 걷은 위조 벌금 · 조사관에게 준 몫 · 통과한 가짜에 기금이 치른 값 (기록). */
    public long finesTotal, findersTotal, forgedPaidTotal;
    public long fund;
    /** 마지막으로 시세를 정한 날 (출석의 날짜 번호) · 주. Long.MIN_VALUE = 아직 없음. */
    public long day = Long.MIN_VALUE, week = Long.MIN_VALUE;
    public boolean played;
    /** 오늘 오버월드에 뇌우가 친 틱 수 (3.73.0: 길면 다음 날 어업 조합에 폭풍 경보). */
    public long thunderTicks;
    public long nextNo = 1;
    /** 지금까지 기금에 들어온 수수료 · 나간 배당 (기록). */
    public long feesTotal, dividendsTotal;

    public static StockData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(StockData::load, StockData::new, NAME);
    }

    public Board board(String union) {
        return boards.computeIfAbsent(union, k -> new Board());
    }

    /** 이 조합에 아직 거래소가 되사지 않은 주 수. */
    public long outstanding(String union) {
        long n = 0;
        for (Cert c : certs.values()) if (c.union.equals(union)) n += c.shares;
        return n;
    }

    public static StockData load(CompoundTag tag) {
        StockData d = new StockData();
        d.fund = Math.max(0, tag.getLong("fund"));
        d.day = tag.contains("day") ? tag.getLong("day") : Long.MIN_VALUE;
        d.week = tag.contains("week") ? tag.getLong("week") : Long.MIN_VALUE;
        d.played = tag.getBoolean("played");
        d.thunderTicks = tag.getLong("thunderTicks");
        d.nextNo = Math.max(1, tag.getLong("nextNo"));
        d.feesTotal = tag.getLong("feesTotal");
        d.dividendsTotal = tag.getLong("dividendsTotal");
        CompoundTag bs = tag.getCompound("boards");
        for (String k : bs.getAllKeys()) {
            CompoundTag b = bs.getCompound(k);
            Board x = new Board();
            x.price = b.getLong("price");
            for (Tag t : b.getList("history", Tag.TAG_LONG)) x.history.add(((LongTag) t).getAsLong());
            for (Tag t : b.getList("metrics", Tag.TAG_LONG)) x.metrics.add(((LongTag) t).getAsLong());
            ListTag td = b.getList("today", Tag.TAG_COMPOUND);
            for (int i = 0; i < td.size(); i++) {
                CompoundTag c = td.getCompound(i);
                if (c.hasUUID("id")) x.today.put(c.getUUID("id"), c.getLong("v"));
            }
            x.weekMetric = b.getLong("weekMetric");
            x.lastWeekMetric = b.getLong("lastWeekMetric");
            x.dividend = b.getLong("dividend");
            x.lastChange = b.getDouble("lastChange");
            x.pendingNews = b.getDouble("pendingNews");
            x.pendingNext = b.getDouble("pendingNext");
            d.boards.put(k, x);
        }
        ListTag cl = tag.getList("certs", Tag.TAG_COMPOUND);
        for (int i = 0; i < cl.size(); i++) {
            CompoundTag c = cl.getCompound(i);
            if (!c.hasUUID("id")) continue;
            Cert x = new Cert();
            x.union = c.getString("union");
            x.shares = c.getInt("shares");
            x.issuedDay = c.getLong("issued");
            x.paidWeek = c.contains("paid") ? c.getLong("paid") : Long.MIN_VALUE;
            x.no = c.getLong("no");
            x.price = c.getLong("price");
            if (c.hasUUID("buyer")) x.buyer = c.getUUID("buyer");
            x.buyerName = c.getString("buyerName");
            if (x.shares > 0 && !x.union.isEmpty()) d.certs.put(c.getUUID("id"), x);
        }
        for (Tag t : tag.getList("newsDone", Tag.TAG_STRING)) d.newsDone.add(t.getAsString());
        ListTag nl = tag.getList("newsLog", Tag.TAG_COMPOUND);
        for (int i = 0; i < nl.size(); i++) {
            CompoundTag c = nl.getCompound(i);
            NewsLine x = new NewsLine();
            x.day = c.getLong("day");
            x.union = c.getString("union");
            x.text = c.getString("text");
            x.textEn = c.getString("textEn");
            x.change = c.getDouble("change");
            if (c.contains("kind")) x.kind = c.getString("kind");
            d.newsLog.add(x);
        }
        ListTag bt = tag.getList("bought", Tag.TAG_COMPOUND);
        for (int i = 0; i < bt.size(); i++) {
            CompoundTag c = bt.getCompound(i);
            if (c.hasUUID("id")) d.boughtToday.put(c.getUUID("id"), c.getInt("n"));
        }
        ListTag fl = tag.getList("forged", Tag.TAG_COMPOUND);
        for (int i = 0; i < fl.size(); i++) {
            CompoundTag c = fl.getCompound(i);
            if (!c.hasUUID("id")) continue;
            Forged x = new Forged();
            x.union = c.getString("union");
            x.shares = c.getInt("shares");
            x.issuedDay = c.getLong("issued");
            x.no = c.getLong("no");
            x.price = c.getLong("price");
            x.madeDay = c.getLong("made");
            x.quality = c.getDouble("q");
            if (c.hasUUID("forger")) x.forger = c.getUUID("forger");
            x.forgerName = c.getString("forgerName");
            if (x.shares > 0 && !x.union.isEmpty()) d.forged.put(c.getUUID("id"), x);
        }
        ListTag ct = tag.getList("caught", Tag.TAG_COMPOUND);
        for (int i = 0; i < ct.size(); i++) {
            CompoundTag c = ct.getCompound(i);
            if (!c.hasUUID("id")) continue;
            List<Long> days = new ArrayList<>();
            for (Tag t : c.getList("days", Tag.TAG_LONG)) days.add(((LongTag) t).getAsLong());
            d.caught.put(c.getUUID("id"), days);
        }
        ListTag wt = tag.getList("wanted", Tag.TAG_COMPOUND);
        for (int i = 0; i < wt.size(); i++) {
            CompoundTag c = wt.getCompound(i);
            if (!c.hasUUID("id")) continue;
            d.wanted.put(c.getUUID("id"), c.getLong("until"));
            d.wantedNames.put(c.getUUID("id"), c.getString("name"));
        }
        ListTag ft = tag.getList("forgedToday", Tag.TAG_COMPOUND);
        for (int i = 0; i < ft.size(); i++) {
            CompoundTag c = ft.getCompound(i);
            if (c.hasUUID("id")) d.forgedToday.put(c.getUUID("id"), c.getInt("n"));
        }
        d.forgeDay = tag.contains("forgeDay") ? tag.getLong("forgeDay") : Long.MIN_VALUE;
        CompoundTag ap = tag.getCompound("appraised");
        for (String k : ap.getAllKeys()) d.appraised.put(k, ap.getLong(k));
        d.finesTotal = tag.getLong("finesTotal");
        d.findersTotal = tag.getLong("findersTotal");
        d.forgedPaidTotal = tag.getLong("forgedPaidTotal");
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLong("fund", fund);
        if (day != Long.MIN_VALUE) tag.putLong("day", day);
        if (week != Long.MIN_VALUE) tag.putLong("week", week);
        tag.putBoolean("played", played);
        tag.putLong("thunderTicks", thunderTicks);
        tag.putLong("nextNo", nextNo);
        tag.putLong("feesTotal", feesTotal);
        tag.putLong("dividendsTotal", dividendsTotal);
        CompoundTag bs = new CompoundTag();
        for (Map.Entry<String, Board> e : boards.entrySet()) {
            Board x = e.getValue();
            CompoundTag b = new CompoundTag();
            b.putLong("price", x.price);
            ListTag h = new ListTag();
            for (long v : x.history) h.add(LongTag.valueOf(v));
            b.put("history", h);
            ListTag m = new ListTag();
            for (long v : x.metrics) m.add(LongTag.valueOf(v));
            b.put("metrics", m);
            ListTag td = new ListTag();
            for (Map.Entry<UUID, Long> t : x.today.entrySet()) {
                CompoundTag c = new CompoundTag();
                c.putUUID("id", t.getKey());
                c.putLong("v", t.getValue());
                td.add(c);
            }
            b.put("today", td);
            b.putLong("weekMetric", x.weekMetric);
            b.putLong("lastWeekMetric", x.lastWeekMetric);
            b.putLong("dividend", x.dividend);
            b.putDouble("lastChange", x.lastChange);
            b.putDouble("pendingNews", x.pendingNews);
            b.putDouble("pendingNext", x.pendingNext);
            bs.put(e.getKey(), b);
        }
        tag.put("boards", bs);
        ListTag cl = new ListTag();
        for (Map.Entry<UUID, Cert> e : certs.entrySet()) {
            Cert x = e.getValue();
            CompoundTag c = new CompoundTag();
            c.putUUID("id", e.getKey());
            c.putString("union", x.union);
            c.putInt("shares", x.shares);
            c.putLong("issued", x.issuedDay);
            if (x.paidWeek != Long.MIN_VALUE) c.putLong("paid", x.paidWeek);
            c.putLong("no", x.no);
            c.putLong("price", x.price);
            if (x.buyer != null) c.putUUID("buyer", x.buyer);
            c.putString("buyerName", x.buyerName);
            cl.add(c);
        }
        tag.put("certs", cl);
        ListTag nd = new ListTag();
        for (String s : newsDone) nd.add(StringTag.valueOf(s));
        tag.put("newsDone", nd);
        ListTag nl = new ListTag();
        for (NewsLine x : newsLog) {
            CompoundTag c = new CompoundTag();
            c.putLong("day", x.day);
            c.putString("union", x.union);
            c.putString("text", x.text);
            c.putString("textEn", x.textEn);
            c.putDouble("change", x.change);
            c.putString("kind", x.kind);
            nl.add(c);
        }
        tag.put("newsLog", nl);
        ListTag bt = new ListTag();
        for (Map.Entry<UUID, Integer> e : boughtToday.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putUUID("id", e.getKey());
            c.putInt("n", e.getValue());
            bt.add(c);
        }
        tag.put("bought", bt);
        ListTag fl = new ListTag();
        for (Map.Entry<UUID, Forged> e : forged.entrySet()) {
            Forged x = e.getValue();
            CompoundTag c = new CompoundTag();
            c.putUUID("id", e.getKey());
            c.putString("union", x.union);
            c.putInt("shares", x.shares);
            c.putLong("issued", x.issuedDay);
            c.putLong("no", x.no);
            c.putLong("price", x.price);
            c.putLong("made", x.madeDay);
            c.putDouble("q", x.quality);
            if (x.forger != null) c.putUUID("forger", x.forger);
            c.putString("forgerName", x.forgerName);
            fl.add(c);
        }
        tag.put("forged", fl);
        ListTag ct = new ListTag();
        for (Map.Entry<UUID, List<Long>> e : caught.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putUUID("id", e.getKey());
            ListTag days = new ListTag();
            for (long v : e.getValue()) days.add(LongTag.valueOf(v));
            c.put("days", days);
            ct.add(c);
        }
        tag.put("caught", ct);
        ListTag wt = new ListTag();
        for (Map.Entry<UUID, Long> e : wanted.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putUUID("id", e.getKey());
            c.putLong("until", e.getValue());
            c.putString("name", wantedNames.getOrDefault(e.getKey(), ""));
            wt.add(c);
        }
        tag.put("wanted", wt);
        ListTag ft = new ListTag();
        for (Map.Entry<UUID, Integer> e : forgedToday.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putUUID("id", e.getKey());
            c.putInt("n", e.getValue());
            ft.add(c);
        }
        tag.put("forgedToday", ft);
        if (forgeDay != Long.MIN_VALUE) tag.putLong("forgeDay", forgeDay);
        CompoundTag ap = new CompoundTag();
        for (Map.Entry<String, Long> e : appraised.entrySet()) ap.putLong(e.getKey(), e.getValue());
        tag.put("appraised", ap);
        tag.putLong("finesTotal", finesTotal);
        tag.putLong("findersTotal", findersTotal);
        tag.putLong("forgedPaidTotal", forgedPaidTotal);
        return tag;
    }
}

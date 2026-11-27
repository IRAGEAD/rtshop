package com.rtshop.stock;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.rtshop.RtShop;
import com.rtshop.attendance.Attendance;
import com.rtshop.attendance.AttendanceConfig;
import com.rtshop.economy.Economy;
import com.rtshop.item.ModItems;
import com.rtshop.item.StockCertificateItem;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * 조합 증서 (주식, 3.72.0 — 사용자가 고른 (가)안의 첫 단계: 증서 · 시세 · 배당 · 시세판).
 * <ul>
 * <li>조합 여섯 곳(농업 · 어업 · 대장간 · 식품 · 토지 · 조사대 기금)의 증서를 은행원 그레고리(증권 창구)에게서 그날 시세로 사고, 되판다. 살 때 · 팔 때 수수료 2% 는 기금으로.</li>
 * <li>시세는 하루 한 번(출석과 같은 기준 시각) 바뀐다: 서버 전체의 그날 실적(그 조합 쪽으로 번 Gold, 한 사람 몫은 상한까지)을 지난 7일 평균과 견주고,
 * 작은 무작위와 처음 값으로 돌아가려는 힘, 이야기 소식을 더해 ±10% 안에서.</li>
 * <li>배당은 주 1회: 지난주 실적 비율대로 기금의 절반을 나눠 '한 주당 배당'을 정하고, 지난주부터 가진 증서를 창구에 보여 주면 받는다(증서에 도장).
 * 배당은 기금(수수료)에서만 나가므로 Gold 가 새로 생기지 않는다.</li>
 * <li>증서는 장부에 적힌 것만 통한다(복사본은 한 번만). 죽어도 떨어지지 않는다.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class Stocks {
    private Stocks() {
    }

    private static final Random RNG = new Random();
    /** 은행원의 열쇠 (증권 창구). */
    public static final String DESK = "bank";
    /** 점검용: 시계를 앞으로 돌린다 (ms). */
    public static long testShiftMs;
    /** 점검용: 창구 거리를 묻지 않는다. */
    public static boolean testAnywhere;
    /** 점검용: 가짜 플레이어(FakePlayer)도 실적 · 죽음 보관에 넣는다. */
    public static volatile boolean testing;

    // ------------------------------------------------------------------ 날짜

    static long now() {
        return System.currentTimeMillis() + testShiftMs;
    }

    public static long today() {
        return Attendance.dayOf(now(), AttendanceConfig.get());
    }

    /** 월요일에 시작하는 주의 번호 (1970-01-05 가 월요일 = 날짜 번호 4). */
    public static long weekOf(long day) {
        return Math.floorDiv(day - 4, 7);
    }

    static long weekStartDay(long week) {
        return week * 7 + 4;
    }

    public static String date(long day) {
        LocalDate d = LocalDate.ofEpochDay(day);
        return String.format("%02d-%02d", d.getMonthValue(), d.getDayOfMonth());
    }

    // ------------------------------------------------------------------ 시세

    static void ensureBoards(StockConfig c, StockData d) {
        for (StockConfig.Union u : c.unions) {
            StockData.Board b = d.board(u.id);
            if (b.price <= 0) {
                b.price = u.startPrice;
                d.setDirty();
            }
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 100 != 37) return;
        try {
            roll(event.getServer());
        } catch (Throwable t) {
            RtShop.LOGGER.warn("[rtshop] 조합 시세 갱신 실패: {}", t.toString());
        }
    }

    /** 날이 바뀌었으면 시세를 정하고(빠진 날만큼), 주가 바뀌었으면 배당을 정한다. */
    public static void roll(MinecraftServer server) {
        StockConfig c = StockConfig.get();
        if (!c.enabled) return;
        StockData d = StockData.get(server);
        ensureBoards(c, d);
        long t = today();
        if (d.day == Long.MIN_VALUE) {
            d.day = t;
            d.week = weekOf(t);
            d.setDirty();
        }
        boolean online = false;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) online |= !(p instanceof FakePlayer);
        int guard = 0;
        while (d.day < t && guard++ < 60) {
            closeDay(c, d);
            d.day++;
            d.boughtToday.clear();
            d.played = false;
            long w = weekOf(d.day);
            if (w != d.week) {
                closeWeek(c, d);
                d.week = w;
            }
            d.setDirty();
        }
        if (d.day < t) {   // 두 달 넘게 꺼져 있었다: 남은 날은 값을 그대로 두고 건너뛴다
            d.day = t;
            d.week = weekOf(t);
            d.setDirty();
        }
        if (online && !d.played) {
            d.played = true;
            d.setDirty();
        }
    }

    /** 하루를 닫는다: 조합마다 오늘 실적으로 내일 시세를 정한다. */
    static void closeDay(StockConfig c, StockData d) {
        for (StockConfig.Union u : c.unions) {
            StockData.Board b = d.board(u.id);
            long todayM = b.todayTotal(c.playerDailyCap);
            double avg = 0;
            int n = 0;
            for (int i = b.metrics.size() - 1; i >= 0 && n < 7; i--, n++) avg += b.metrics.get(i);
            avg = n == 0 ? todayM : avg / n;
            double base = Math.max(avg, c.metricFloor);
            double perf = d.played ? clamp((todayM - avg) / base, -1, 1) : 0;
            double pull = -c.pull * Math.log((double) Math.max(1, b.price) / Math.max(1, u.startPrice));
            double change = c.perfWeight * perf + RNG.nextGaussian() * c.noise * (d.played ? 1 : 0.5) + pull + b.pendingNews;
            change = clamp(change, -c.dailyMove, c.dailyMove);
            long np = Math.max(c.minPrice, Math.min(c.maxPrice, Math.round(b.price * (1 + change))));
            b.history.add(b.price);
            while (b.history.size() > StockData.HISTORY) b.history.remove(0);
            b.lastChange = b.price <= 0 ? 0 : (double) (np - b.price) / b.price;
            b.price = np;
            if (d.played) {
                b.metrics.add(todayM);
                while (b.metrics.size() > StockData.METRIC_DAYS) b.metrics.remove(0);
            }
            b.weekMetric += todayM;
            b.today.clear();
            b.pendingNews = 0;
        }
    }

    /** 한 주를 닫는다: 지난주 실적 비율대로 기금의 일부를 '한 주당 배당'으로 정한다. */
    static void closeWeek(StockConfig c, StockData d) {
        long total = 0;
        for (StockConfig.Union u : c.unions) total += d.board(u.id).weekMetric;
        long pool = (long) Math.floor(d.fund * c.dividendPool);
        for (StockConfig.Union u : c.unions) {
            StockData.Board b = d.board(u.id);
            b.lastWeekMetric = b.weekMetric;
            b.weekMetric = 0;
            long out = d.outstanding(u.id);
            if (pool <= 0 || total <= 0 || out <= 0 || b.lastWeekMetric <= 0) {
                b.dividend = 0;
                continue;
            }
            long per = (long) Math.floor(pool * ((double) b.lastWeekMetric / total) / out);
            per = Math.min(per, (long) Math.floor(b.price * c.dividendCap));
            b.dividend = Math.max(0, per);
        }
    }

    /** 실적 한 건 (그 조합 쪽으로 오간 Gold). */
    public static void activity(MinecraftServer server, UUID id, String union, long gold) {
        if (server == null || id == null || gold <= 0) return;
        try {
            StockConfig c = StockConfig.get();
            if (!c.enabled || c.union(union) == null) return;
            StockData d = StockData.get(server);
            d.board(union).today.merge(id, gold, Long::sum);
            d.setDirty();
        } catch (Throwable t) {
            RtShop.LOGGER.warn("[rtshop] 조합 실적 기록 실패: {}", t.toString());
        }
    }

    public static void activity(ServerPlayer p, String union, long gold) {
        if (p == null || p instanceof FakePlayer && !testing) return;
        activity(p.server, p.getUUID(), union, gold);
    }

    /** 수입 기록(IncomeStats)의 출처 → 조합. */
    public static String unionOfIncome(String source) {
        return switch (source) {
            case "sell_farm" -> "farm";
            case "sell_fish" -> "fish";
            case "sell_mined" -> "smith";
            case "sell_food" -> "food";
            case "raid", "abyss", "bounty" -> "survey";
            default -> null;
        };
    }

    public static void onIncome(MinecraftServer server, UUID id, String source, long amount) {
        String u = unionOfIncome(source);
        if (u != null) activity(server, id, u, amount);
    }

    /** NPC 상점에서 산 것: 대장간 → 대장간 조합, 토지 상인 → 토지 조합. */
    public static void onShopBuy(ServerPlayer p, String shopId, long gold) {
        if (shopId == null) return;
        if (shopId.startsWith("blacksmith")) activity(p, "smith", gold);
        else if (shopId.equals("land")) activity(p, "land", gold);
    }

    // ------------------------------------------------------------------ 이야기 소식

    /** 퀘스트를 마쳤다: 서버에서 처음이면 소식을 다음 시세에 싣고 모두에게 알린다. */
    public static void onQuestDone(ServerPlayer p, String questId) {
        StockConfig c = StockConfig.get();
        if (!c.enabled || p == null) return;
        StockData d = StockData.get(p.server);
        ensureBoards(c, d);
        for (StockConfig.News n : c.news) {
            if (!n.quest.equals(questId) || c.union(n.union) == null) continue;
            String key = n.quest + "|" + n.union;
            if (!d.newsDone.add(key)) continue;
            d.board(n.union).pendingNews += n.change;
            StockData.NewsLine line = new StockData.NewsLine();
            line.day = today();
            line.union = n.union;
            line.text = n.text;
            line.textEn = n.textEn;
            line.change = n.change;
            d.newsLog.add(line);
            while (d.newsLog.size() > StockData.NEWS_LOG) d.newsLog.remove(0);
            d.setDirty();
            StockConfig.Union u = c.union(n.union);
            for (ServerPlayer q : p.server.getPlayerList().getPlayers()) {
                boolean en = com.rtshop.story.StoryText.en(q);
                q.sendSystemMessage(Component.translatable("rtshop.stock.news_broadcast", en ? n.textEn : n.text, en ? u.nameEn : u.name, pct(n.change))
                        .withStyle(ChatFormatting.GOLD));
            }
        }
    }

    // ------------------------------------------------------------------ 거래

    public static final int OK = 0, ERR_OFF = 1, ERR_FAR = 2, ERR_UNION = 3, ERR_AMOUNT = 4, ERR_LIMIT = 5, ERR_MONEY = 6, ERR_FULL = 7, ERR_INVALID = 8, ERR_NONE = 9;

    /** 은행원(증권 창구)이 곁에 있는가. */
    public static boolean nearDesk(ServerPlayer p) {
        if (testAnywhere) return true;
        try {
            double r = StockConfig.get().deskRange;
            return !p.level().getEntities(p, p.getBoundingBox().inflate(r), e -> DESK.equals(com.rtshop.quest.QuestManager.npcKey(e))).isEmpty();
        } catch (Throwable t) {
            return false;
        }
    }

    /** 증서를 산다. 결과 코드. */
    public static int buy(ServerPlayer p, String union, int shares) {
        StockConfig c = StockConfig.get();
        if (!c.enabled) return ERR_OFF;
        if (!nearDesk(p)) return ERR_FAR;
        StockConfig.Union u = c.union(union);
        if (u == null) return ERR_UNION;
        if (shares < 1 || shares > c.maxPerCertificate) return ERR_AMOUNT;
        StockData d = StockData.get(p.server);
        roll(p.server);
        int bought = d.boughtToday.getOrDefault(p.getUUID(), 0);
        if (bought + shares > c.buyPerDay) return ERR_LIMIT;
        StockData.Board b = d.board(union);
        long cost = b.price * shares, fee = (long) Math.ceil(cost * c.fee), total = cost + fee;
        if (Economy.wallet(p) < total) return ERR_MONEY;
        if (p.getInventory().getFreeSlot() < 0) return ERR_FULL;
        UUID id = UUID.randomUUID();
        StockData.Cert cert = new StockData.Cert();
        cert.union = union;
        cert.shares = shares;
        cert.issuedDay = d.day;
        cert.no = d.nextNo++;
        cert.price = b.price;
        cert.buyer = p.getUUID();
        cert.buyerName = p.getGameProfile().getName();
        Economy.addWallet(p, -total);
        d.fund += fee;
        d.feesTotal += fee;
        d.certs.put(id, cert);
        d.boughtToday.merge(p.getUUID(), shares, Integer::sum);
        d.setDirty();
        ItemStack stack = StockCertificateItem.create(id, u, cert);
        if (!p.getInventory().add(stack)) p.drop(stack, false);
        lastForTest = new long[]{cost, fee, total};
        return OK;
    }

    /** 증서를 되판다 (일련번호). 받은 Gold, 실패면 -결과 코드. */
    public static long sell(ServerPlayer p, UUID serial) {
        StockConfig c = StockConfig.get();
        if (!c.enabled) return -ERR_OFF;
        if (!nearDesk(p)) return -ERR_FAR;
        StockData d = StockData.get(p.server);
        roll(p.server);
        int slot = -1;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(ModItems.STOCK_CERTIFICATE.get()) && serial.equals(StockCertificateItem.id(s))) {
                slot = i;
                break;
            }
        }
        if (slot < 0) return -ERR_NONE;
        ItemStack s = p.getInventory().getItem(slot);
        StockData.Cert cert = d.certs.get(serial);
        if (!valid(cert, s)) return -ERR_INVALID;
        StockData.Board b = d.board(cert.union);
        long value = b.price * cert.shares, fee = (long) Math.ceil(value * c.fee), pay = Math.max(0, value - fee);
        d.certs.remove(serial);
        d.fund += fee;
        d.feesTotal += fee;
        d.setDirty();
        p.getInventory().setItem(slot, ItemStack.EMPTY);
        p.getInventory().setChanged();
        Economy.addWallet(p, pay);
        lastForTest = new long[]{value, fee, pay};
        return pay;
    }

    /** 장부의 증서와 아이템이 맞는가 (조합 · 주 수). */
    static boolean valid(StockData.Cert cert, ItemStack s) {
        return cert != null && cert.union.equals(StockCertificateItem.union(s)) && cert.shares == StockCertificateItem.shares(s);
    }

    /** 이 증서가 이번 주 배당을 받을 수 있는가: 이번 주가 시작되기 전에 발행되었고 아직 도장이 없다. */
    static boolean eligible(StockData d, StockData.Cert cert) {
        return cert != null && cert.issuedDay < weekStartDay(d.week) && cert.paidWeek != d.week && d.board(cert.union).dividend > 0;
    }

    /** 가진 증서의 배당을 모두 받는다. 받은 Gold (없으면 0). */
    public static long claim(ServerPlayer p) {
        StockConfig c = StockConfig.get();
        if (!c.enabled || !nearDesk(p)) return 0;
        StockData d = StockData.get(p.server);
        roll(p.server);
        long total = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (!s.is(ModItems.STOCK_CERTIFICATE.get())) continue;
            UUID id = StockCertificateItem.id(s);
            StockData.Cert cert = id == null ? null : d.certs.get(id);
            if (!valid(cert, s) || !eligible(d, cert)) continue;
            long due = Math.min(d.fund, d.board(cert.union).dividend * cert.shares);
            if (due <= 0) continue;
            d.fund -= due;
            d.dividendsTotal += due;
            cert.paidWeek = d.week;
            StockCertificateItem.stamp(s, d.day);
            total += due;
        }
        if (total > 0) {
            d.setDirty();
            p.getInventory().setChanged();
            Economy.addWallet(p, total);
        }
        return total;
    }

    /** 점검용: 마지막 거래의 {값, 수수료, 오간 Gold}. */
    public static long[] lastForTest = new long[3];

    // ------------------------------------------------------------------ 창

    /** 시세판 · 거래 창에 보낼 내용. trade = 창구에서 연 것 (사고팔기 단추). */
    public static JsonObject view(ServerPlayer p, boolean trade, String msgKey, String[] msgArgs, boolean ok) {
        StockConfig c = StockConfig.get();
        StockData d = StockData.get(p.server);
        roll(p.server);
        ensureBoards(c, d);
        JsonObject o = new JsonObject();
        o.addProperty("trade", trade && nearDesk(p));
        o.addProperty("enabled", c.enabled);
        o.addProperty("fee", c.fee);
        o.addProperty("dailyMove", c.dailyMove);
        o.addProperty("fund", d.fund);
        o.addProperty("wallet", Economy.wallet(p));
        o.addProperty("maxPer", c.maxPerCertificate);
        o.addProperty("buyPerDay", c.buyPerDay);
        o.addProperty("buyLeft", Math.max(0, c.buyPerDay - d.boughtToday.getOrDefault(p.getUUID(), 0)));
        o.addProperty("today", date(d.day));
        long nextDay = Attendance.dayStart(d.day + 1, AttendanceConfig.get());
        o.addProperty("resetIn", Math.max(0, (nextDay - now()) / 1000L));
        o.addProperty("weekIn", Math.max(0, (Attendance.dayStart(weekStartDay(d.week + 1), AttendanceConfig.get()) - now()) / 1000L));
        if (msgKey != null) {   // 글은 화면 쪽 언어 파일로 (전용 서버에는 모드의 언어 파일이 없다)
            o.addProperty("msg", msgKey);
            JsonArray a = new JsonArray();
            if (msgArgs != null) for (String x : msgArgs) a.add(x);
            o.add("args", a);
            o.addProperty("ok", ok);
        }
        // 가진 증서 (인벤토리)
        JsonArray mine = new JsonArray();
        long[] held = new long[c.unions.size()];
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (!s.is(ModItems.STOCK_CERTIFICATE.get())) continue;
            UUID id = StockCertificateItem.id(s);
            StockData.Cert cert = id == null ? null : d.certs.get(id);
            JsonObject x = new JsonObject();
            x.addProperty("id", id == null ? "" : id.toString());
            x.addProperty("union", StockCertificateItem.union(s));
            x.addProperty("shares", StockCertificateItem.shares(s));
            x.addProperty("no", StockCertificateItem.no(s));
            boolean v = valid(cert, s);
            x.addProperty("valid", v);
            if (v) {
                int ui = c.index(cert.union);
                if (ui >= 0) held[ui] += cert.shares;
                x.addProperty("issued", date(cert.issuedDay));
                x.addProperty("bought", cert.price);
                x.addProperty("value", d.board(cert.union).price * cert.shares);
                x.addProperty("eligible", eligible(d, cert));
                x.addProperty("paid", cert.paidWeek == d.week);
                x.addProperty("due", eligible(d, cert) ? d.board(cert.union).dividend * cert.shares : 0);
            }
            mine.add(x);
        }
        o.add("mine", mine);
        JsonArray us = new JsonArray();
        for (int i = 0; i < c.unions.size(); i++) {
            StockConfig.Union u = c.unions.get(i);
            StockData.Board b = d.board(u.id);
            JsonObject x = new JsonObject();
            x.addProperty("id", u.id);
            x.addProperty("name", u.name);
            x.addProperty("nameEn", u.nameEn);
            x.addProperty("color", u.color);
            x.addProperty("price", b.price);
            x.addProperty("start", u.startPrice);
            x.addProperty("change", b.lastChange);
            x.addProperty("dividend", b.dividend);
            x.addProperty("outstanding", d.outstanding(u.id));
            x.addProperty("held", held[i]);
            JsonArray h = new JsonArray();
            int from = Math.max(0, b.history.size() - 13);
            for (int k = from; k < b.history.size(); k++) h.add(b.history.get(k));
            h.add(b.price);
            x.add("history", h);
            x.addProperty("pending", b.pendingNews != 0);
            us.add(x);
        }
        o.add("unions", us);
        JsonArray ns = new JsonArray();
        for (int i = d.newsLog.size() - 1; i >= 0 && ns.size() < 8; i--) {
            StockData.NewsLine n = d.newsLog.get(i);
            JsonObject x = new JsonObject();
            x.addProperty("date", date(n.day));
            x.addProperty("union", n.union);
            x.addProperty("text", n.text);
            x.addProperty("textEn", n.textEn);
            x.addProperty("change", n.change);
            ns.add(x);
        }
        o.add("news", ns);
        return o;
    }

    public static void open(ServerPlayer p, boolean trade, String msgKey, String[] msgArgs, boolean ok) {
        if (p == null) return;
        com.rtshop.net.ShopNet.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> p),
                new com.rtshop.net.StoryPacket(com.rtshop.net.StoryPacket.STOCKS, view(p, trade, msgKey, msgArgs, ok).toString()));
    }

    /** 창에서 한 일: 0 = 다시 보기, 1 = 사기(조합, 주 수), 2 = 팔기(일련번호), 3 = 배당 받기. */
    public static void fromScreen(ServerPlayer p, int action, String union, int arg, String serial) {
        boolean en = com.rtshop.story.StoryText.en(p);
        switch (action) {
            case 1 -> {
                int r = buy(p, union, arg);
                StockConfig.Union u = StockConfig.get().union(union);
                String name = u == null ? union : en ? u.nameEn : u.name;
                if (r == OK) open(p, true, "rtshop.stock.bought", new String[]{name, String.valueOf(arg), num(lastForTest[2]), num(lastForTest[1])}, true);
                else open(p, true, err(r), null, false);
            }
            case 2 -> {
                long r;
                try {
                    r = sell(p, UUID.fromString(serial));
                } catch (IllegalArgumentException ex) {
                    r = -ERR_NONE;
                }
                if (r >= 0) open(p, true, "rtshop.stock.sold", new String[]{num(r), num(lastForTest[1])}, true);
                else open(p, true, err((int) -r), null, false);
            }
            case 3 -> {
                long g = claim(p);
                if (g > 0) open(p, true, "rtshop.stock.claimed", new String[]{num(g)}, true);
                else open(p, true, "rtshop.stock.no_dividend", null, false);
            }
            default -> open(p, true, null, null, true);
        }
    }

    /** 결과 코드 → 언어 파일 열쇠. */
    static String err(int code) {
        return "rtshop.stock.err." + switch (code) {
            case ERR_OFF -> "off";
            case ERR_FAR -> "far";
            case ERR_UNION -> "union";
            case ERR_AMOUNT -> "amount";
            case ERR_LIMIT -> "limit";
            case ERR_MONEY -> "money";
            case ERR_FULL -> "full";
            case ERR_INVALID -> "invalid";
            default -> "none";
        };
    }

    static String num(long v) {
        return String.format("%,d", v);
    }

    static String pct(double v) {
        return (v >= 0 ? "+" : "") + Math.round(v * 1000) / 10.0 + "%";
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // ------------------------------------------------------------------ 죽어도 떨어지지 않게

    private static final String KEEP = "rtshop_keep_certs";

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p instanceof FakePlayer && !testing) return;
        ListTag keep = new ListTag();
        Iterator<ItemEntity> it = event.getDrops().iterator();
        while (it.hasNext()) {
            ItemEntity e = it.next();
            if (e.getItem().is(ModItems.STOCK_CERTIFICATE.get())) {
                keep.add(e.getItem().save(new CompoundTag()));
                it.remove();
            }
        }
        if (!keep.isEmpty()) {
            ListTag old = p.getPersistentData().getList(KEEP, Tag.TAG_COMPOUND);
            old.addAll(keep);
            p.getPersistentData().put(KEEP, old);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        ListTag keep = event.getOriginal().getPersistentData().getList(KEEP, Tag.TAG_COMPOUND);
        if (keep.isEmpty()) return;
        event.getOriginal().getPersistentData().remove(KEEP);
        for (int i = 0; i < keep.size(); i++) {
            ItemStack s = ItemStack.of(keep.getCompound(i));
            if (!s.isEmpty() && !p.getInventory().add(s)) p.drop(s, false);
        }
    }

    // ------------------------------------------------------------------ 명령

    @SubscribeEvent
    public static void onCommands(net.minecraftforge.event.RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("rtshop").then(command()));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("stocks")
                .executes(ctx -> {
                    open(ctx.getSource().getPlayerOrException(), false, null, null, true);
                    return 1;
                })
                .then(Commands.literal("trade").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    boolean near = nearDesk(p);
                    open(p, near, near ? null : "rtshop.stock.err.far", null, near);
                    return 1;
                }))
                .then(Commands.literal("reload").requires(s -> s.hasPermission(2)).executes(ctx -> {
                    StockConfig.reload();
                    ctx.getSource().sendSuccess(() -> Component.literal("[RT Shop] stocks.json reloaded"), true);
                    return 1;
                }))
                .then(Commands.literal("status").requires(s -> s.hasPermission(2)).executes(ctx -> {
                    MinecraftServer server = ctx.getSource().getServer();
                    StockConfig c = StockConfig.get();
                    StockData d = StockData.get(server);
                    roll(server);
                    StringBuilder sb = new StringBuilder("[RT Shop] stocks · day " + date(d.day) + " · fund " + num(d.fund) + " G (fees " + num(d.feesTotal)
                            + ", dividends " + num(d.dividendsTotal) + ") · certificates " + d.certs.size());
                    for (StockConfig.Union u : c.unions) {
                        StockData.Board b = d.board(u.id);
                        sb.append("\n  ").append(u.name).append(" ").append(num(b.price)).append(" G (").append(pct(b.lastChange)).append(") · shares ")
                                .append(num(d.outstanding(u.id))).append(" · dividend ").append(num(b.dividend)).append(" · today ")
                                .append(num(b.todayTotal(c.playerDailyCap))).append(" G");
                    }
                    String s = sb.toString();
                    ctx.getSource().sendSuccess(() -> Component.literal(s), false);
                    return 1;
                }))
                .then(Commands.literal("nextday").requires(s -> s.hasPermission(2)).then(Commands.argument("days", IntegerArgumentType.integer(1, 30)).executes(ctx -> {
                    int n = IntegerArgumentType.getInteger(ctx, "days");
                    // 장부의 날짜를 앞당긴다: 진짜 날짜가 그 날에 닿을 때까지는 저절로 닫지 않는다
                    advance(ctx.getSource().getServer(), n, false);
                    ctx.getSource().sendSuccess(() -> Component.literal("[RT Shop] stocks: closed " + n + " day(s) by hand (prices moved, dividends set on a new week)"), true);
                    return 1;
                })));
    }

    /** 점검용 · 운영자: 장부의 날을 n 번 닫는다 (시세 · 주가 바뀌면 배당). played = 그날 사람이 있었던 것으로 친다. */
    public static void advance(MinecraftServer server, int n, boolean played) {
        StockData d = StockData.get(server);
        StockConfig c = StockConfig.get();
        ensureBoards(c, d);
        if (d.day == Long.MIN_VALUE) roll(server);
        for (int i = 0; i < n; i++) {
            if (played) d.played = true;
            closeDay(c, d);
            d.day++;
            d.boughtToday.clear();
            d.played = false;
            long w = weekOf(d.day);
            if (w != d.week) {
                closeWeek(c, d);
                d.week = w;
            }
        }
        d.setDirty();
    }

    /** 점검용: 오늘 날짜로 처음부터 (장부 · 기금 · 소식 모두 지움). */
    public static void resetForTest(MinecraftServer server) {
        StockData d = StockData.get(server);
        d.boards.clear();
        d.certs.clear();
        d.newsDone.clear();
        d.newsLog.clear();
        d.boughtToday.clear();
        d.fund = 0;
        d.feesTotal = 0;
        d.dividendsTotal = 0;
        d.day = Long.MIN_VALUE;
        d.week = Long.MIN_VALUE;
        d.played = false;
        testShiftMs = 0;
        roll(server);
    }

    /** 점검용: 장부의 증서 목록. */
    public static List<UUID> certsForTest(MinecraftServer server) {
        return new ArrayList<>(StockData.get(server).certs.keySet());
    }
}

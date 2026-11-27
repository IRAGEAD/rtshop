package com.rtshop.stock;

import com.rtshop.economy.Economy;
import com.rtshop.economy.EconomyData;
import com.rtshop.item.ModItems;
import com.rtshop.item.StockCertificateItem;
import com.rtshop.job.JobConfig;
import com.rtshop.job.Jobs;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * 위조 증서와 감정 (3.76.0, ④ 히든 직업 — 사용자가 고른 (가)안의 두 번째 단계).
 * <ul>
 * <li>위조자: 위조 도구를 들고 다른 손의 증서를 본으로 가짜를 떠낸다 (종이 · 먹물 주머니 · 벌집 조각, 발광 먹물이면 더 그럴듯함, 하루 forgeDaily 장).
 *     가짜는 아이템 · 툴팁 · 거래소 목록까지 진짜와 똑같다 (일련번호는 본의 것). 위조인지는 서버의 기록(StockData.forged)만 안다.</li>
 * <li>거래소: 가짜를 되팔면 완성도 - exchangeEye 의 확률로 통과하고 그 값은 수수료 기금에서만 나간다 (기금이 모자라면 통과 못 함 → Gold 가 새로 생기지 않는다).
 *     걸리면 판명 도장. 배당 도장을 찍을 때는 반드시 드러난다. 누구나 작은 수수료로 봉랍 확인을 받을 수 있다 (속는 것이 선택이 되게).</li>
 * <li>조사관: 감정 돋보기를 들고 다른 손의 증서를 살핀다 (같은 증서는 하루 한 번). 흠을 찾으면 판명 도장, 위조자의 벌금에서 몫을 받는다
 *     (보상은 벌금에서만 — 둘이 짜고 기금을 빼 갈 수 없다).</li>
 * <li>드러날 때마다 위조자에게 벌금(지갑 → 예금, 접속해 있지 않아도). 같은 손의 가짜가 며칠 안에 여러 번 드러나면 수배: 시세판 공고 · 거래소 거래 금지.</li>
 * </ul>
 */
public final class Forgery {
    private Forgery() {
    }

    public static final String FORGER = "forger", INSPECTOR = "investigator";
    private static final Random RNG = new Random();
    /** 점검용: 다음 굴림들 (앞에서부터 하나씩 쓴다). 비면 무작위. */
    public static final List<Double> testRolls = new ArrayList<>();
    public static double lastQualityForTest = -1;
    /** 점검용: 마지막 판명의 {벌금, 걷힌 벌금, 조사관 몫}. */
    public static long[] lastFineForTest = new long[3];
    public static String lastJudgeForTest = "";
    public static long lastCheckFeeForTest;
    public static int lastFlawForTest = -1;

    static double roll() {
        synchronized (testRolls) {
            if (!testRolls.isEmpty()) return testRolls.remove(0);
        }
        return RNG.nextDouble();
    }

    public static boolean on() {
        StockConfig c = StockConfig.get();
        return c.enabled && c.forgery.enabled;
    }

    public static boolean hasJob(ServerPlayer p, String job) {
        return job.equals(Jobs.entry(p).life);
    }

    static double jobParam(String job, String key, double def) {
        JobConfig.JobDef d = JobConfig.get().jobs.get(job);
        return d == null ? def : d.param(key, def);
    }

    /** 거래소(되팔기 · 봉랍 확인 · 배당)가 이 가짜를 알아보지 못할 확률. */
    public static double passChance(double quality) {
        return clamp(quality - StockConfig.get().forgery.exchangeEye, 0.02, 0.9);
    }

    /** 조사관이 이 가짜의 흠을 찾을 확률. */
    public static double inspectChance(double quality) {
        StockConfig.Forgery f = StockConfig.get().forgery;
        return clamp(f.inspectBase + jobParam(INSPECTOR, "inspectSkill", 0.25) + (1 - quality) * f.inspectFlawWeight, 0.05, 0.97);
    }

    /** 위조자에게 보이는 완성도의 말 (언어 파일 열쇠의 끝). */
    public static String tier(double q) {
        return q >= 0.8 ? "flawless" : q >= 0.65 ? "convincing" : q >= 0.5 ? "clumsy" : "obvious";
    }

    /** 이 아이템이 이 가짜 기록과 맞는가 (조합 · 주 수). */
    public static boolean matches(@Nullable StockData.Forged f, ItemStack s) {
        return f != null && f.union.equals(StockCertificateItem.union(s)) && f.shares == StockCertificateItem.shares(s)
                && StockCertificateItem.exposed(s) == null;
    }

    public static boolean isWanted(StockData d, UUID id) {
        Long until = d.wanted.get(id);
        return until != null && until >= d.day;
    }

    public static boolean isWanted(ServerPlayer p) {
        StockData d = StockData.get(p.server);
        return isWanted(d, p.getUUID());
    }

    // ------------------------------------------------------------------ 위조자: 떠내기

    public static final int OK = 0, OFF = 1, NOT_JOB = 2, NO_TEMPLATE = 3, BAD_TEMPLATE = 4, EXPOSED_TEMPLATE = 5, LIMIT = 6,
            NO_PAPER = 7, NO_INK = 8, NO_WAX = 9, FULL = 10, FOUND = 11, CLEAN = 12, AGAIN = 13, WANTED = 14;

    /** 오늘 더 떠낼 수 있는 장 수. */
    public static int forgeLeft(ServerPlayer p) {
        StockData d = StockData.get(p.server);
        Stocks.roll(p.server);
        int limit = (int) Math.round(jobParam(FORGER, "forgeDaily", 3));
        int used = d.forgeDay == d.day ? d.forgedToday.getOrDefault(p.getUUID(), 0) : 0;
        return Math.max(0, limit - used);
    }

    /**
     * 다른 손의 증서(template)를 본으로 가짜를 한 장 떠낸다. 결과 코드. 본은 그대로 남는다.
     * 가짜를 본으로 뜨면 완성도가 본보다 copyPenalty 만큼 떨어진다 (가짜인지 알려 주지는 않는다).
     */
    public static int forge(ServerPlayer p, ItemStack template) {
        StockConfig c = StockConfig.get();
        if (!on()) return OFF;
        if (!hasJob(p, FORGER)) return NOT_JOB;
        if (!template.is(ModItems.STOCK_CERTIFICATE.get()) || StockCertificateItem.shares(template) <= 0) return NO_TEMPLATE;
        if (StockCertificateItem.exposed(template) != null) return EXPOSED_TEMPLATE;
        StockData d = StockData.get(p.server);
        Stocks.roll(p.server);
        UUID tid = StockCertificateItem.id(template);
        StockData.Cert real = tid == null ? null : d.certs.get(tid);
        StockData.Forged fake = tid == null ? null : d.forged.get(tid);
        boolean fromReal = Stocks.valid(real, template);
        if (!fromReal && !matches(fake, template)) return BAD_TEMPLATE;
        String union = fromReal ? real.union : fake.union;
        StockConfig.Union u = c.union(union);
        if (u == null) return BAD_TEMPLATE;
        if (d.forgeDay != d.day) {
            d.forgedToday.clear();
            d.forgeDay = d.day;
        }
        int limit = (int) Math.round(jobParam(FORGER, "forgeDaily", 3));
        if (d.forgedToday.getOrDefault(p.getUUID(), 0) >= limit) return LIMIT;
        int paper = find(p, Items.PAPER), glow = find(p, Items.GLOW_INK_SAC), ink = glow >= 0 ? glow : find(p, Items.INK_SAC), wax = find(p, Items.HONEYCOMB);
        if (paper < 0) return NO_PAPER;
        if (ink < 0) return NO_INK;
        if (wax < 0) return NO_WAX;
        if (p.getInventory().getFreeSlot() < 0) return FULL;
        if (!p.getAbilities().instabuild) {
            p.getInventory().getItem(paper).shrink(1);
            p.getInventory().getItem(ink).shrink(1);
            p.getInventory().getItem(wax).shrink(1);
        }
        StockConfig.Forgery g = c.forgery;
        double q = g.baseQuality + jobParam(FORGER, "forgeSkill", 0.15) + (glow >= 0 ? g.glowInkBonus : 0) + g.qualitySpread * (2 * roll() - 1);
        if (!fromReal) q = Math.min(q, fake.quality) - g.copyPenalty;
        q = clamp(q, 0.05, 0.95);
        StockData.Forged f = new StockData.Forged();
        f.union = union;
        f.shares = fromReal ? real.shares : fake.shares;
        f.issuedDay = fromReal ? real.issuedDay : fake.issuedDay;
        f.no = fromReal ? real.no : fake.no;
        f.price = fromReal ? real.price : fake.price;
        f.madeDay = d.day;
        f.quality = q;
        f.forger = p.getUUID();
        f.forgerName = p.getGameProfile().getName();
        UUID id = UUID.randomUUID();
        d.forged.put(id, f);
        d.forgedToday.merge(p.getUUID(), 1, Integer::sum);
        d.setDirty();
        StockData.Cert look = new StockData.Cert();
        look.union = f.union;
        look.shares = f.shares;
        look.issuedDay = f.issuedDay;
        look.no = f.no;
        look.price = f.price;
        ItemStack out = StockCertificateItem.create(id, u, look);
        if (!p.getInventory().add(out)) p.drop(out, false);
        p.getInventory().setChanged();
        lastQualityForTest = q;
        return OK;
    }

    private static int find(ServerPlayer p, Item item) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (p.getInventory().getItem(i).is(item)) return i;
        }
        return -1;
    }

    // ------------------------------------------------------------------ 드러남 · 벌금 · 수배

    /**
     * 가짜가 드러났다: 기록을 지우고 아이템에 판명 도장, 위조자에게 벌금(그날 액면가 × fineRate, 지갑 → 예금),
     * finder(조사관)가 있으면 걷힌 벌금의 finderShare 를 그에게, 나머지는 기금으로. 드러난 횟수가 차면 수배. 걷힌 벌금.
     */
    public static long expose(MinecraftServer server, StockData d, UUID id, StockData.Forged f, ItemStack stack, @Nullable ServerPlayer finder) {
        StockConfig.Forgery g = StockConfig.get().forgery;
        d.forged.remove(id);
        StockCertificateItem.markExposed(stack, finder == null ? "" : finder.getGameProfile().getName());
        long value = d.board(f.union).price * (long) f.shares;
        long fine = Math.round(value * g.fineRate);
        long got = f.forger == null ? 0 : takeFine(server, f.forger, fine);
        long toFinder = finder != null && f.forger != null && !finder.getUUID().equals(f.forger) ? Math.round(got * g.finderShare) : 0;
        d.fund += got - toFinder;
        d.finesTotal += got;
        d.findersTotal += toFinder;
        if (toFinder > 0) Economy.addWallet(finder, toFinder);
        lastFineForTest = new long[]{fine, got, toFinder};
        if (f.forger != null) {
            List<Long> days = d.caught.computeIfAbsent(f.forger, k -> new ArrayList<>());
            days.add(d.day);
            days.removeIf(x -> x <= d.day - g.wantedWindowDays);
            if (days.size() >= g.wantedAfter && g.wantedDays > 0 && !isWanted(d, f.forger)) {
                d.wanted.put(f.forger, d.day + g.wantedDays - 1);
                d.wantedNames.put(f.forger, f.forgerName);
                days.clear();
                StockData.NewsLine n = new StockData.NewsLine();
                n.day = d.day;
                n.union = "";
                n.kind = "notice";
                n.text = "거래소 공고: 위조 증서를 만든 " + f.forgerName + " 을(를) " + g.wantedDays + "일 동안 수배합니다. 그동안 거래소는 그와 거래하지 않습니다.";
                n.textEn = "Exchange notice: " + f.forgerName + " is wanted for forged certificates for " + g.wantedDays + " days. The exchange will not deal with them meanwhile.";
                d.newsLog.add(n);
                while (d.newsLog.size() > StockData.NEWS_LOG) d.newsLog.remove(0);
            }
            ServerPlayer fp = server.getPlayerList().getPlayer(f.forger);
            if (fp != null && fp != finder) {
                com.rtshop.notice.Notice.send(fp, Component.translatable("rtshop.forge.caught_notice", Stocks.num(got)).withStyle(ChatFormatting.RED), true);
            }
        }
        d.setDirty();
        return got;
    }

    /** 벌금: 지갑에서, 모자라면 예금에서 (접속해 있지 않아도). 걷힌 Gold. */
    static long takeFine(MinecraftServer server, UUID who, long fine) {
        if (fine <= 0) return 0;
        EconomyData e = EconomyData.get(server);
        long w = e.wallet(who), b = e.bank(who);
        long fromW = Math.min(w, fine), fromB = Math.min(b, fine - fromW);
        if (fromW > 0) e.setWallet(who, w - fromW);
        if (fromB > 0) e.setBank(who, b - fromB);
        ServerPlayer p = server.getPlayerList().getPlayer(who);
        if (p != null) Economy.sync(p);
        return fromW + fromB;
    }

    // ------------------------------------------------------------------ 거래소: 되팔기 · 봉랍 확인 · 배당

    /** 가짜를 되판다 (Stocks.sell 이 부른다): 통과하면 기금에서 값을 치르고, 아니면 판명 도장. 받은 Gold, 걸리면 -ERR_FORGED. */
    static long sellFake(ServerPlayer p, StockData d, UUID serial, StockData.Forged f, int slot) {
        StockConfig c = StockConfig.get();
        ItemStack s = p.getInventory().getItem(slot);
        long value = d.board(f.union).price * (long) f.shares, fee = (long) Math.ceil(value * c.fee), pay = Math.max(0, value - fee);
        if (roll() < passChance(f.quality) && d.fund >= pay) {
            d.forged.remove(serial);
            d.fund -= pay;
            d.forgedPaidTotal += pay;
            d.setDirty();
            p.getInventory().setItem(slot, ItemStack.EMPTY);
            p.getInventory().setChanged();
            Economy.addWallet(p, pay);
            Stocks.lastForTest = new long[]{value, fee, pay};
            lastJudgeForTest = "pass";
            return pay;
        }
        expose(p.server, d, serial, f, s, null);
        p.getInventory().setChanged();
        lastJudgeForTest = "caught";
        return -Stocks.ERR_FORGED;
    }

    /** 봉랍 확인의 수수료. */
    public static long checkFee(long value) {
        StockConfig.Forgery g = StockConfig.get().forgery;
        return Math.max(g.checkFeeMin, (long) Math.ceil(value * g.checkFeeRate));
    }

    /** 봉랍 확인 (거래소, 누구나): 수수료를 내고 이 증서를 확인받는다. CLEAN / FOUND(판명 도장) / 그 밖은 Stocks 의 결과 코드를 음수로. */
    public static int check(ServerPlayer p, UUID serial) {
        StockConfig c = StockConfig.get();
        if (!on()) return -Stocks.ERR_OFF;
        if (!Stocks.nearDesk(p)) return -Stocks.ERR_FAR;
        StockData d = StockData.get(p.server);
        Stocks.roll(p.server);
        if (isWanted(d, p.getUUID())) return -Stocks.ERR_WANTED;
        int slot = Stocks.slotOf(p, serial);
        if (slot < 0) return -Stocks.ERR_NONE;
        ItemStack s = p.getInventory().getItem(slot);
        if (StockCertificateItem.exposed(s) != null) return -Stocks.ERR_INVALID;
        StockData.Cert real = d.certs.get(serial);
        StockData.Forged fake = d.forged.get(serial);
        long value;
        if (Stocks.valid(real, s)) value = d.board(real.union).price * (long) real.shares;
        else if (matches(fake, s)) value = d.board(fake.union).price * (long) fake.shares;
        else return -Stocks.ERR_INVALID;
        long fee = checkFee(value);
        if (Economy.wallet(p) < fee) return -Stocks.ERR_MONEY;
        Economy.addWallet(p, -fee);
        d.fund += fee;
        d.feesTotal += fee;
        d.setDirty();
        lastCheckFeeForTest = fee;
        if (matches(fake, s) && roll() >= passChance(fake.quality)) {
            expose(p.server, d, serial, fake, s, null);
            p.getInventory().setChanged();
            return FOUND;
        }
        return CLEAN;
    }

    // ------------------------------------------------------------------ 조사관: 감정

    /** 감정 돋보기로 다른 손의 증서를 살핀다 (같은 증서는 하루 한 번). FOUND = 흠을 찾아 판명 도장, CLEAN = 찾지 못함 (진짜든 가짜든 같은 말). */
    public static int appraise(ServerPlayer p, ItemStack target) {
        if (!on()) return OFF;
        if (!hasJob(p, INSPECTOR)) return NOT_JOB;
        if (!target.is(ModItems.STOCK_CERTIFICATE.get()) || StockCertificateItem.shares(target) <= 0) return NO_TEMPLATE;
        if (StockCertificateItem.exposed(target) != null) return EXPOSED_TEMPLATE;
        UUID id = StockCertificateItem.id(target);
        if (id == null) return NO_TEMPLATE;
        StockData d = StockData.get(p.server);
        Stocks.roll(p.server);
        String key = p.getUUID() + "|" + id;
        Long last = d.appraised.get(key);
        if (last != null && last == d.day) return AGAIN;
        d.appraised.entrySet().removeIf(e -> e.getValue() < d.day - 1);
        d.appraised.put(key, d.day);
        d.setDirty();
        StockData.Forged f = d.forged.get(id);
        if (matches(f, target) && roll() < inspectChance(f.quality)) {
            lastFlawForTest = (int) (roll() * 3) % 3;
            expose(p.server, d, id, f, target, p);
            return FOUND;
        }
        return CLEAN;
    }

    private static double clamp(double v, double lo, double hi) {
        return Double.isNaN(v) ? lo : Math.max(lo, Math.min(hi, v));
    }
}

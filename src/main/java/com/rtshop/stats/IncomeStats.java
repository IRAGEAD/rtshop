package com.rtshop.stats;

import com.mojang.brigadier.context.CommandContext;
import com.rtshop.RtShop;
import com.rtshop.job.JobConfig;
import com.rtshop.job.JobData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * 직업별 수입 기록 (3.68.0, world/data/rtshop_income.dat).
 * 플레이어가 Gold 를 벌 때마다 그때의 생활 직업 · 전투 직업 칸에 출처별로 더하고, 접속해 움직이는 시간을 1분 단위로 센다.
 * 운영자는 /rtshop income 으로 직업마다의 시간당 수입을 보고, 자세한 표는 config/rtshop/export/income.txt · income.csv 로 받는다.
 * 수표 · 예금 · 땅 환불 · 운영자 지급 · 처음 받는 돈은 수입이 아니다 (부르는 곳에서 add 를 부르지 않는다).
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class IncomeStats extends SavedData {
    private static final String NAME = "rtshop_income";

    /** 출처 (표의 열 순서). */
    public static final String[] SOURCES = {"sell_fish", "sell_farm", "sell_mined", "sell_food", "sell_other", "pshop", "quest", "bounty", "title",
            "attendance", "raid", "abyss"};
    /** 시간을 세는 간격(틱)과, 움직이지 않으면 잠수로 보는 시간(ms). */
    static final int SAMPLE_TICKS = 1200;
    static final long IDLE_MS = 5 * 60 * 1000L;

    /** 한 직업 칸: 움직인 시간(초)과 출처별 수입. */
    public static final class Bucket {
        public long seconds;
        public final Map<String, Long> gold = new TreeMap<>();

        public long total() {
            long t = 0;
            for (long v : gold.values()) t += v;
            return t;
        }
    }

    public static final class PlayerRec {
        public String name = "";
        /** "life:farmer" · "combat:swordsman" · "life:-"(직업 없음) → 칸. */
        public final Map<String, Bucket> jobs = new TreeMap<>();
    }

    private final Map<UUID, PlayerRec> players = new LinkedHashMap<>();
    private long since = System.currentTimeMillis();

    public static IncomeStats get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(IncomeStats::load, IncomeStats::new, NAME);
    }

    // ------------------------------------------------------------------ 기록

    /** 생활 · 전투 직업 칸의 열쇠 (직업이 없으면 "-"). */
    static String[] keys(JobData.Entry e) {
        String life = e == null || e.life == null || e.life.isEmpty() ? "-" : e.life;
        String combat = e == null || e.combat == null || e.combat.isEmpty() ? "-" : e.combat;
        return new String[]{"life:" + life, "combat:" + combat};
    }

    /** 수입 한 건. amount <= 0 이면 무시. */
    public static void add(ServerPlayer p, String source, long amount) {
        if (p == null || amount <= 0 || p instanceof net.minecraftforge.common.util.FakePlayer && !testing) return;
        try {
            add(p.server, p.getUUID(), p.getGameProfile().getName(), source, amount);
        } catch (Throwable t) {
            RtShop.LOGGER.warn("[rtshop] 수입 기록 실패: {}", t.toString());
        }
    }

    /** 접속하지 않은 사람의 수입 (플레이어 상점의 판매 대금 등). */
    public static void add(MinecraftServer server, UUID id, String name, String source, long amount) {
        if (server == null || id == null || amount <= 0) return;
        com.rtshop.stock.Stocks.onIncome(server, id, source, amount);   // 조합 실적 (3.72)
        IncomeStats d = get(server);
        PlayerRec r = d.players.computeIfAbsent(id, k -> new PlayerRec());
        if (name != null && !name.isEmpty()) r.name = name;
        for (String k : keys(JobData.get(server).of(id))) {
            r.jobs.computeIfAbsent(k, x -> new Bucket()).gold.merge(source, amount, Long::sum);
        }
        d.setDirty();
    }

    /** 움직인 시간(초)을 지금 직업 칸에 더한다. */
    public static void addTime(ServerPlayer p, long seconds) {
        if (p == null || seconds <= 0) return;
        IncomeStats d = get(p.server);
        PlayerRec r = d.players.computeIfAbsent(p.getUUID(), k -> new PlayerRec());
        r.name = p.getGameProfile().getName();
        for (String k : keys(JobData.get(p.server).of(p.getUUID()))) {
            r.jobs.computeIfAbsent(k, x -> new Bucket()).seconds += seconds;
        }
        d.setDirty();
    }

    /** 점검용: 가짜 플레이어의 수입도 적는다. */
    public static volatile boolean testing;

    // ------------------------------------------------------------------ 움직인 시간

    private static final Map<UUID, double[]> LAST_POS = new HashMap<>();
    private static final Map<UUID, Long> LAST_ACTIVE = new HashMap<>();
    private static int ticks;

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || ++ticks < SAMPLE_TICKS) return;
        ticks = 0;
        MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        long now = System.currentTimeMillis();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            try {
                if (active(p, now)) addTime(p, SAMPLE_TICKS / 20);
            } catch (Throwable t) {
                RtShop.LOGGER.warn("[rtshop] 수입 기록의 시간 세기 실패: {}", t.toString());
            }
        }
    }

    /** 지난 번과 견주어 움직였거나 고개를 돌렸으면 지금을 '움직인 때'로 적고, 마지막으로 움직인 지 IDLE_MS 안이면 셈. */
    static boolean active(ServerPlayer p, long now) {
        if (p.isSpectator()) return false;
        double[] cur = {p.getX(), p.getY(), p.getZ(), p.getYRot(), p.getXRot()};
        double[] last = LAST_POS.put(p.getUUID(), cur);
        boolean moved = last == null || Math.abs(cur[0] - last[0]) + Math.abs(cur[1] - last[1]) + Math.abs(cur[2] - last[2]) > 0.5
                || Math.abs(cur[3] - last[3]) + Math.abs(cur[4] - last[4]) > 2;
        if (moved) LAST_ACTIVE.put(p.getUUID(), now);
        Long a = LAST_ACTIVE.get(p.getUUID());
        return a != null && now - a <= IDLE_MS;
    }

    public static boolean activeForTest(ServerPlayer p, long now) {
        return active(p, now);
    }

    /** 점검용: 위치 기록을 지운다. */
    public static void forgetForTest(UUID id) {
        LAST_POS.remove(id);
        LAST_ACTIVE.remove(id);
    }

    // ------------------------------------------------------------------ 모으기 · 보고

    /** 한 직업의 합: 사람 수 · 초 · 출처별 Gold. */
    public static final class Sum {
        public final String key;
        public int people;
        public long seconds;
        public final Map<String, Long> gold = new LinkedHashMap<>();

        Sum(String key) {
            this.key = key;
            for (String s : SOURCES) gold.put(s, 0L);
        }

        public long total() {
            long t = 0;
            for (long v : gold.values()) t += v;
            return t;
        }

        /** 시간당 Gold (움직인 시간이 10분 안 되면 -1). */
        public double perHour() {
            return seconds < 600 ? -1 : total() * 3600.0 / seconds;
        }

        public double perHour(String source) {
            return seconds < 600 ? -1 : gold.getOrDefault(source, 0L) * 3600.0 / seconds;
        }
    }

    /** slot = "life" 또는 "combat": 그 갈래의 직업별 합 (사람이 한 명이라도 있는 직업만). */
    public List<Sum> sums(String slot) {
        Map<String, Sum> out = new TreeMap<>();
        for (PlayerRec r : players.values()) {
            for (Map.Entry<String, Bucket> e : r.jobs.entrySet()) {
                if (!e.getKey().startsWith(slot + ":")) continue;
                Sum s = out.computeIfAbsent(e.getKey(), Sum::new);
                s.people++;
                s.seconds += e.getValue().seconds;
                for (Map.Entry<String, Long> g : e.getValue().gold.entrySet()) s.gold.merge(g.getKey(), g.getValue(), Long::sum);
            }
        }
        return new ArrayList<>(out.values());
    }

    public PlayerRec player(UUID id) {
        return players.get(id);
    }

    public long since() {
        return since;
    }

    public void reset() {
        players.clear();
        since = System.currentTimeMillis();
        setDirty();
    }

    static String jobName(String key) {
        String id = key.substring(key.indexOf(':') + 1);
        if (id.equals("-")) return "(직업 없음)";
        try {
            JobConfig.JobDef d = JobConfig.get().job(id);
            if (d != null && d.name != null && !d.name.isEmpty()) return d.name;
        } catch (Throwable ignored) {
        }
        return id;
    }

    static String man(double v) {
        if (v < 0) return "-";
        if (v >= 10000) return String.format(Locale.ROOT, "%.1f만", v / 10000.0);
        return String.format(Locale.ROOT, "%,d", Math.round(v));
    }

    static String hours(long seconds) {
        return String.format(Locale.ROOT, "%.1f", seconds / 3600.0);
    }

    /** 채팅 한 줄: "농부: 3명 · 12.5시간 · 시간당 2.4만 G (판매 2.1만 · 퀘스트 0.2만 · 그 밖 0.1만)". */
    public static String line(Sum s) {
        double sell = 0;
        for (String src : SOURCES) if (src.startsWith("sell_") || src.equals("pshop")) sell += s.perHour(src);
        double quest = s.perHour("quest") + s.perHour("bounty");
        double rest = s.perHour() - sell - quest;
        return String.format(Locale.ROOT, "%s: %d명 · %s시간 · 시간당 %s G (판매 %s · 퀘스트 · 현상금 %s · 그 밖 %s)", jobName(s.key), s.people, hours(s.seconds),
                man(s.perHour()), man(s.perHour() < 0 ? -1 : sell), man(s.perHour() < 0 ? -1 : quest), man(s.perHour() < 0 ? -1 : Math.max(0, rest)));
    }

    private static final String[] SOURCE_NAMES = {"물고기 판매", "농산물 판매", "광물 판매", "요리 · 음식 판매", "그 밖의 판매", "플레이어 상점", "퀘스트", "현상금", "칭호",
            "출석", "레이드", "심연"};

    /** 자세한 표 (income.txt) 와 CSV (income.csv) 를 쓰고 txt 경로를 돌려준다. */
    public Path export() throws java.io.IOException {
        Path dir = FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("export");
        Files.createDirectories(dir);
        StringBuilder t = new StringBuilder();
        StringBuilder csv = new StringBuilder("slot,job,people,hours,total_gold,gold_per_hour");
        for (String src : SOURCES) csv.append(',').append(src).append("_per_hour");
        csv.append('\n');
        String from = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(since));
        long days = Math.max(0, (System.currentTimeMillis() - since) / 86_400_000L);
        t.append("# 직업별 수입 (").append(from).append(" 부터, ").append(days).append("일). 시간은 접속해 움직인 시간만 (5분 넘게 가만히 있으면 빼고 셈).\n");
        t.append("# 시간당 = 그 직업일 때 번 Gold ÷ 그 직업일 때 움직인 시간. 10분 안 된 직업은 '-'.\n\n");
        for (String slot : new String[]{"life", "combat"}) {
            t.append(slot.equals("life") ? "== 생활 직업\n" : "\n== 전투 직업\n");
            for (Sum s : sums(slot)) {
                t.append(line(s)).append('\n');
                for (int i = 0; i < SOURCES.length; i++) {
                    long g = s.gold.getOrDefault(SOURCES[i], 0L);
                    if (g > 0) t.append(String.format(Locale.ROOT, "    · %s: %,d G (시간당 %s)%n", SOURCE_NAMES[i], g, man(s.perHour(SOURCES[i]))));
                }
                csv.append(slot).append(',').append(s.key.substring(s.key.indexOf(':') + 1)).append(',').append(s.people).append(',')
                        .append(hours(s.seconds)).append(',').append(s.total()).append(',').append(String.format(Locale.ROOT, "%.0f", Math.max(0, s.perHour())));
                for (String src : SOURCES) csv.append(',').append(String.format(Locale.ROOT, "%.0f", Math.max(0, s.perHour(src))));
                csv.append('\n');
            }
        }
        t.append("\n== 사람별 (생활 직업 칸)\n");
        for (Map.Entry<UUID, PlayerRec> e : players.entrySet()) {
            for (Map.Entry<String, Bucket> b : e.getValue().jobs.entrySet()) {
                if (!b.getKey().startsWith("life:")) continue;
                double ph = b.getValue().seconds < 600 ? -1 : b.getValue().total() * 3600.0 / b.getValue().seconds;
                t.append(String.format(Locale.ROOT, "%s | %s | %s시간 | %,d G | 시간당 %s%n", e.getValue().name.isEmpty() ? e.getKey() : e.getValue().name,
                        jobName(b.getKey()), hours(b.getValue().seconds), b.getValue().total(), man(ph)));
            }
        }
        Path txt = dir.resolve("income.txt");
        Files.writeString(txt, t.toString(), StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("income.csv"), csv.toString(), StandardCharsets.UTF_8);
        return txt;
    }

    // ------------------------------------------------------------------ 명령

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("rtshop").then(Commands.literal("income")
                .executes(c -> c.getSource().hasPermission(2) ? report(c) : me(c))
                .then(Commands.literal("me").executes(IncomeStats::me))
                .then(Commands.literal("reset").requires(s -> s.hasPermission(2)).executes(c -> {
                    get(c.getSource().getServer()).reset();
                    c.getSource().sendSuccess(() -> Component.literal("수입 기록을 지웠습니다. 지금부터 다시 셉니다.").withStyle(ChatFormatting.YELLOW), true);
                    return 1;
                }))));
    }

    private static int report(CommandContext<CommandSourceStack> c) {
        IncomeStats d = get(c.getSource().getServer());
        String from = DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(d.since));
        long days = Math.max(0, (System.currentTimeMillis() - d.since) / 86_400_000L);
        c.getSource().sendSuccess(() -> Component.literal("직업별 수입 (" + from + " 부터 · " + days + "일, 움직인 시간만)").withStyle(ChatFormatting.GOLD), false);
        boolean any = false;
        for (String slot : new String[]{"life", "combat"}) {
            List<Sum> sums = d.sums(slot);
            if (sums.isEmpty()) continue;
            any = true;
            c.getSource().sendSuccess(() -> Component.literal(slot.equals("life") ? "생활 직업" : "전투 직업").withStyle(ChatFormatting.YELLOW), false);
            for (Sum s : sums) {
                String l = " · " + line(s);
                c.getSource().sendSuccess(() -> Component.literal(l).withStyle(ChatFormatting.GRAY), false);
            }
        }
        if (!any) {
            c.getSource().sendSuccess(() -> Component.literal("아직 기록이 없습니다.").withStyle(ChatFormatting.GRAY), false);
        }
        try {
            Path f = d.export();
            c.getSource().sendSuccess(() -> Component.literal("자세한 표: " + f).withStyle(ChatFormatting.DARK_GRAY), false);
        } catch (Exception ex) {
            RtShop.LOGGER.warn("[rtshop] 수입 표를 쓰지 못했습니다: {}", ex.toString());
        }
        return 1;
    }

    private static int me(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        boolean en = com.rtshop.story.StoryText.en(p);
        PlayerRec r = get(p.server).player(p.getUUID());
        if (r == null || r.jobs.isEmpty()) {
            c.getSource().sendSuccess(() -> Component.literal(en ? "No records yet." : "아직 기록이 없습니다.").withStyle(ChatFormatting.GRAY), false);
            return 1;
        }
        c.getSource().sendSuccess(() -> Component.literal(en ? "My income (by life job, active time only)" : "내 수입 (생활 직업별, 움직인 시간만)").withStyle(ChatFormatting.GOLD), false);
        for (Map.Entry<String, Bucket> b : r.jobs.entrySet()) {
            if (!b.getKey().startsWith("life:")) continue;
            Bucket v = b.getValue();
            double ph = v.seconds < 600 ? -1 : v.total() * 3600.0 / v.seconds;
            String name = en ? b.getKey().substring(5) : jobName(b.getKey());
            String l = en ? String.format(Locale.ROOT, " · %s: %s h · %,d G · %s G per hour", name, hours(v.seconds), v.total(), ph < 0 ? "-" : String.format(Locale.ROOT, "%,d", Math.round(ph)))
                    : String.format(Locale.ROOT, " · %s: %s시간 · %,d G · 시간당 %s G", name, hours(v.seconds), v.total(), man(ph));
            c.getSource().sendSuccess(() -> Component.literal(l).withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }

    // ------------------------------------------------------------------ 저장

    public static IncomeStats load(CompoundTag tag) {
        IncomeStats d = new IncomeStats();
        d.since = tag.contains("since") ? tag.getLong("since") : System.currentTimeMillis();
        CompoundTag ps = tag.getCompound("players");
        for (String k : ps.getAllKeys()) {
            UUID id;
            try {
                id = UUID.fromString(k);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            CompoundTag pt = ps.getCompound(k);
            PlayerRec r = new PlayerRec();
            r.name = pt.getString("name");
            CompoundTag jobs = pt.getCompound("jobs");
            for (String jk : jobs.getAllKeys()) {
                CompoundTag bt = jobs.getCompound(jk);
                Bucket b = new Bucket();
                b.seconds = bt.getLong("seconds");
                CompoundTag g = bt.getCompound("gold");
                for (String src : g.getAllKeys()) b.gold.put(src, g.getLong(src));
                r.jobs.put(jk, b);
            }
            d.players.put(id, r);
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLong("since", since);
        CompoundTag ps = new CompoundTag();
        for (Map.Entry<UUID, PlayerRec> e : players.entrySet()) {
            CompoundTag pt = new CompoundTag();
            pt.putString("name", e.getValue().name);
            CompoundTag jobs = new CompoundTag();
            for (Map.Entry<String, Bucket> b : e.getValue().jobs.entrySet()) {
                CompoundTag bt = new CompoundTag();
                bt.putLong("seconds", b.getValue().seconds);
                CompoundTag g = new CompoundTag();
                b.getValue().gold.forEach(g::putLong);
                bt.put("gold", g);
                jobs.put(b.getKey(), bt);
            }
            pt.put("jobs", jobs);
            ps.put(e.getKey().toString(), pt);
        }
        tag.put("players", ps);
        return tag;
    }
}

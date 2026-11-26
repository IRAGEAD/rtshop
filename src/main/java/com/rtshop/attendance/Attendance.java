package com.rtshop.attendance;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import com.rtshop.economy.Economy;
import com.rtshop.economy.EconomyConfig;
import com.rtshop.item.ModItems;
import com.rtshop.net.AttendancePacket;
import com.rtshop.net.ResultPacket;
import com.rtshop.net.ShopNet;
import com.rtshop.shop.ShopServer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * 개인별 일일 출석 + 주간 출석 보상.
 *  - 출석은 출석 NPC(시청 서기)에게 말을 걸어 연 창에서 한다. 날짜는 기준 시각(기본: 한국 시간 오전 6시)에 바뀐다.
 *  - 연속 출석 중 하루를 처음 빠지면 한 번은 봐주고 경고한다. 그 뒤로는 하루만 빠져도, 처음부터 이틀 이상 빠지면 바로 0일차로 초기화.
 *  - 초기화된 뒤 24시간 안에는 '출석 복구권'으로 되돌릴 수 있다 (한 번 쓰면 24시간 동안 다시 못 씀).
 *  - 주간 보상: 이번 주(기본 월요일 오전 6시 시작)에 출석한 날 수가 정해진 일수에 닿으면 받는다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class Attendance {
    private Attendance() {
    }

    public static final String NPC_TAG = "rtshop_attendance";
    /** 시간 (점검에서 바꿔 끼운다). */
    public static LongSupplier clock = System::currentTimeMillis;

    private static final long HOUR = 3_600_000L;

    public enum Change { NONE, GRACE, RESET }

    public enum RestoreResult { OK, NOTHING, EXPIRED, COOLDOWN }

    /** 출석 결과: 받은 일일 보상 번호(0부터)와 새로 받은 주간 보상들. */
    public record CheckIn(int dayIndex, int streak, List<AttendanceConfig.Weekly> weekly) {
    }

    // ------------------------------------------------------------------ 날짜 계산

    /** 출석일 번호: 기준 시각 이전이면 전날로 친다. */
    public static long dayOf(long now, AttendanceConfig c) {
        return Instant.ofEpochMilli(now).atZone(c.zone()).minusHours(c.resetHour).toLocalDate().toEpochDay();
    }

    /** 그 출석일이 시작하는 시각(ms). */
    public static long dayStart(long day, AttendanceConfig c) {
        return LocalDate.ofEpochDay(day).atTime(c.resetHour, 0).atZone(c.zone()).toInstant().toEpochMilli();
    }

    /** 그 출석일이 들어 있는 주의 첫날 번호. */
    public static long weekOf(long day, AttendanceConfig c) {
        return LocalDate.ofEpochDay(day).with(TemporalAdjusters.previousOrSame(c.weekStartDay())).toEpochDay();
    }

    /** streak 번째 출석(1부터)에 받는 일일 보상 번호(0부터). */
    public static int rewardIndex(int streak, AttendanceConfig c) {
        int n = c.days.size();
        if (n == 0) {
            return -1;
        }
        int s = Math.max(1, streak);
        return c.loop ? (s - 1) % n : Math.min(s, n) - 1;
    }

    // ------------------------------------------------------------------ 규칙 (순수 계산: 점검하기 쉽도록 플레이어와 떨어뜨려 둠)

    /** 빠진 날을 반영한다. 하루를 처음 빠졌으면 봐주고(GRACE), 아니면 초기화(RESET). */
    public static Change evaluate(AttendanceData.Entry e, long now, AttendanceConfig c) {
        if (e.streak <= 0 || e.lastDay < 0) {
            return Change.NONE;
        }
        long today = dayOf(now, c);
        long missed = today - e.lastDay - 1;
        if (missed <= 0) {
            return Change.NONE;
        }
        boolean graceAvail = c.grace && !e.graceUsed;
        if (missed == 1 && graceAvail) {
            e.graceUsed = true;
            e.lastDay = today - 1; // 빠진 하루를 이어진 것으로 친다 (출석 일수는 늘지 않음)
            return Change.GRACE;
        }
        // 연속 출석이 끊긴 순간 = 봐줄 수 없는 첫 결석일이 끝난 때
        long breakDay = e.lastDay + (graceAvail ? 3 : 2);
        e.savedStreak = e.streak;
        e.resetAt = dayStart(breakDay, c);
        e.streak = 0;
        e.lastDay = -1;
        if (!c.graceOnce) {
            e.graceUsed = false; // 새 연속 출석에서는 다시 한 번 봐준다
        }
        return Change.RESET;
    }

    /** 오늘 출석. 이미 했으면 null. (먼저 evaluate 를 부를 것) */
    public static CheckIn checkIn(AttendanceData.Entry e, long now, AttendanceConfig c) {
        long today = dayOf(now, c);
        if (e.lastDay == today) {
            return null;
        }
        e.streak++;
        e.total++;
        e.lastDay = today;
        long w = weekOf(today, c);
        if (e.week != w) {
            e.week = w;
            e.weekMask = 0;
            e.weekClaimed = 0;
        }
        e.weekMask |= 1 << (int) Math.max(0, Math.min(6, today - w));
        List<AttendanceConfig.Weekly> got = new ArrayList<>();
        for (AttendanceConfig.Weekly t : c.weekly) {
            int bit = 1 << (t.days - 1);
            if (t.days <= e.weekCount() && (e.weekClaimed & bit) == 0) {
                e.weekClaimed |= bit;
                got.add(t);
            }
        }
        return new CheckIn(rewardIndex(e.streak, c), e.streak, got);
    }

    /** 복구권: 초기화되기 전 진행도로 되돌린다. (먼저 evaluate 를 부를 것) */
    public static RestoreResult restore(AttendanceData.Entry e, long now, AttendanceConfig c) {
        if (e.savedStreak <= 0 || e.resetAt <= 0) {
            return RestoreResult.NOTHING;
        }
        if (now >= e.resetAt + (long) (c.restoreWindowHours * HOUR)) {
            return RestoreResult.EXPIRED;
        }
        if (e.restoreAt > 0 && now < e.restoreAt + (long) (c.restoreCooldownHours * HOUR)) {
            return RestoreResult.COOLDOWN;
        }
        long today = dayOf(now, c);
        boolean checkedToday = e.lastDay == today;
        e.streak = e.savedStreak + (checkedToday ? e.streak : 0);
        if (!checkedToday) {
            e.lastDay = today - 1; // 오늘 출석하면 이어진다
        }
        e.graceUsed = true; // 한 번 되살렸으니 다음부터는 하루만 빠져도 초기화
        e.savedStreak = 0;
        e.resetAt = 0;
        e.restoreAt = now;
        return RestoreResult.OK;
    }

    /** 복구권을 지금 쓸 수 있으면 남은 시간(ms) 정보를 담은 문자열 대신 true. */
    public static boolean restorable(AttendanceData.Entry e, long now, AttendanceConfig c) {
        return e.savedStreak > 0 && e.resetAt > 0 && now < e.resetAt + (long) (c.restoreWindowHours * HOUR)
                && (e.restoreAt <= 0 || now >= e.restoreAt + (long) (c.restoreCooldownHours * HOUR));
    }

    // ------------------------------------------------------------------ 플레이어

    private static AttendanceData.Entry entry(ServerPlayer p) {
        return AttendanceData.get(p.server).of(p.getUUID());
    }

    private static boolean real(ServerPlayer p) {
        return !(p instanceof FakePlayer) && p.connection != null;
    }

    /** 빠진 날을 반영하고, 바뀌었으면 알린다. */
    public static Change refresh(ServerPlayer p) {
        AttendanceConfig c = AttendanceConfig.get();
        AttendanceData d = AttendanceData.get(p.server);
        AttendanceData.Entry e = d.of(p.getUUID());
        int before = e.streak;
        Change ch = evaluate(e, clock.getAsLong(), c);
        if (ch != Change.NONE) {
            d.dirty();
            if (ch == Change.GRACE) {
                com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.att.grace_warn", before).withStyle(ChatFormatting.GOLD));
            } else {
                com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.att.reset_warn", before, time(p, e.resetAt + (long) (c.restoreWindowHours * HOUR), c))
                        .withStyle(ChatFormatting.RED));
            }
        }
        return ch;
    }

    /** 출석 창을 연다. npcId < 0 이면 보기만 (명령어). */
    public static void open(ServerPlayer p, int npcId) {
        refresh(p);
        SESSIONS.put(p.getUUID(), npcId);
        send(p, npcId);
    }

    private static void send(ServerPlayer p, int npcId) {
        if (!real(p)) {
            return;
        }
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new AttendancePacket(AttendancePacket.VIEW, view(p, npcId).toString()));
    }

    private static final Map<UUID, Integer> SESSIONS = new HashMap<>();

    private static boolean nearNpc(ServerPlayer p) {
        Integer id = SESSIONS.get(p.getUUID());
        if (id == null || id < 0) {
            return false;
        }
        Entity e = p.level().getEntity(id);
        double r = EconomyConfig.get().npcRange;
        return e != null && e.isAlive() && e.getTags().contains(NPC_TAG) && e.distanceToSqr(p) <= r * r;
    }

    private static void result(ServerPlayer p, Component msg, boolean ok) {
        if (real(p)) {
            ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new ResultPacket(msg, ok));
        }
    }

    /** 출석 버튼. */
    public static boolean doCheckIn(ServerPlayer p, boolean needNpc) {
        if (needNpc && !nearNpc(p)) {
            result(p, Component.translatable("rtshop.att.need_npc", com.rtshop.story.StoryText.t(p, AttendanceConfig.get().npcName)), false);
            return false;
        }
        refresh(p);
        AttendanceConfig c = AttendanceConfig.get();
        AttendanceData d = AttendanceData.get(p.server);
        AttendanceData.Entry e = d.of(p.getUUID());
        CheckIn ci = checkIn(e, clock.getAsLong(), c);
        if (ci == null) {
            result(p, Component.translatable("rtshop.att.already"), false);
            return false;
        }
        e.history.add(clock.getAsLong());
        while (e.history.size() > 90) e.history.remove(0);
        d.dirty();
        if (ci.dayIndex() >= 0) {
            give(p, c.days.get(ci.dayIndex()));
        }
        com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.att.checked", ci.streak(), ci.dayIndex() + 1).withStyle(ChatFormatting.GREEN));
        for (AttendanceConfig.Weekly w : ci.weekly()) {
            give(p, w.reward);
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.att.weekly_got", w.days).withStyle(ChatFormatting.AQUA));
        }
        result(p, Component.translatable("rtshop.att.checked_short", ci.streak()), true);
        send(p, SESSIONS.getOrDefault(p.getUUID(), -1));
        return true;
    }

    /** 복구권 사용 (아이템 우클릭 또는 창의 버튼). 성공하면 true (아이템은 부른 쪽에서 줄인다). */
    public static boolean useTicket(ServerPlayer p) {
        refresh(p);
        AttendanceConfig c = AttendanceConfig.get();
        AttendanceData d = AttendanceData.get(p.server);
        AttendanceData.Entry e = d.of(p.getUUID());
        int saved = e.savedStreak;
        long now = clock.getAsLong();
        RestoreResult r = restore(e, now, c);
        switch (r) {
            case OK -> {
                d.dirty();
                com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.att.restored", saved, e.streak).withStyle(ChatFormatting.GREEN));
                result(p, Component.translatable("rtshop.att.restored_short", e.streak), true);
                send(p, SESSIONS.getOrDefault(p.getUUID(), -1));
                return true;
            }
            case EXPIRED -> com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.att.restore_expired").withStyle(ChatFormatting.RED));
            case COOLDOWN -> com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.att.restore_cooldown",
                    time(p, e.restoreAt + (long) (c.restoreCooldownHours * HOUR), c)).withStyle(ChatFormatting.RED));
            default -> com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.att.restore_nothing").withStyle(ChatFormatting.YELLOW));
        }
        result(p, Component.translatable("rtshop.att.restore_fail"), false);
        return false;
    }

    /** 창의 '복구권 사용' 버튼: 인벤토리에서 복구권 하나를 쓴다. */
    private static void useTicketFromInventory(ServerPlayer p) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(ModItems.RESTORE_TICKET.get())) {
                if (useTicket(p)) {
                    s.shrink(1);
                }
                return;
            }
        }
        result(p, Component.translatable("rtshop.att.no_ticket"), false);
    }

    public static void give(ServerPlayer p, AttendanceConfig.Reward r) {
        if (r.gold > 0) {
            Economy.addWallet(p, r.gold);
            com.rtshop.rank.Ranks.earned(p, r.gold);
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.quest.reward_gold", String.format("%,d", r.gold)));
        }
        for (AttendanceConfig.Item it : r.items) {
            Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(it.item()));
            if (item == null || item == net.minecraft.world.item.Items.AIR) {
                continue;
            }
            int left = it.count();
            int max = Math.max(1, new ItemStack(item).getMaxStackSize());
            while (left > 0) {
                int n = Math.min(left, max);
                ItemStack s = new ItemStack(item, n);
                if (!p.getInventory().add(s)) {
                    p.drop(s, false);
                }
                left -= n;
            }
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.quest.reward_item", new ItemStack(item).getHoverName(), it.count()));
        }
    }

    /** 창에서 온 요청. */
    public static void handleAction(ServerPlayer p, int action, String arg) {
        switch (action) {
            case com.rtshop.net.AttendanceActionPacket.CHECK_IN -> doCheckIn(p, true);
            case com.rtshop.net.AttendanceActionPacket.RESTORE -> useTicketFromInventory(p);
            case com.rtshop.net.AttendanceActionPacket.SHOP -> {
                if (!nearNpc(p)) {
                    result(p, Component.translatable("rtshop.att.need_npc", com.rtshop.story.StoryText.t(p, AttendanceConfig.get().npcName)), false);
                    return;
                }
                String shop = AttendanceConfig.get().shop;
                if (ShopServer.shops().get(shop) == null) {
                    result(p, Component.translatable("rtshop.msg.unknown_shop", shop), false);
                    return;
                }
                ShopServer.openShop(p, shop, SESSIONS.get(p.getUUID()));
            }
            case com.rtshop.net.AttendanceActionPacket.EDIT -> openEditor(p);
            case com.rtshop.net.AttendanceActionPacket.SAVE -> handleSave(p, arg);
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------ 편집 (OP)

    /** 출석부 편집은 OP(서버 관리자 목록에 있는 사람, 싱글플레이에서는 치트를 켠 주인)만 할 수 있다. */
    public static boolean canEdit(ServerPlayer p) {
        return !(p instanceof FakePlayer) && p.server.getPlayerList().isOp(p.getGameProfile()) && com.rtshop.util.Perm.op(p); // 서바이벌의 OP 는 편집할 수 없다
    }

    public static void openEditor(ServerPlayer p) {
        if (!canEdit(p)) {
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.att.op_only"));
            return;
        }
        if (real(p)) {
            ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
                    new AttendancePacket(AttendancePacket.EDITOR, AttendanceConfig.get().toJson().toString()));
        }
    }

    /** 저장. 성공하면 null, 실패하면 이유. */
    public static String save(String json) {
        AttendanceConfig c;
        try {
            c = AttendanceConfig.fromJson(JsonParser.parseString(json).getAsJsonObject());
        } catch (Exception e) {
            return "저장하지 못했습니다: " + e.getMessage();
        }
        try {
            c.save();
        } catch (Exception e) {
            return "파일에 쓰지 못했습니다: " + e.getMessage();
        }
        AttendanceConfig.reload();
        return null;
    }

    private static void handleSave(ServerPlayer p, String json) {
        if (!canEdit(p)) {
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.att.op_only"));
            return;
        }
        String err = save(json);
        if (err == null) {
            RtShop.LOGGER.info("[rtshop] {} 이(가) 출석 보상을 고쳤습니다", p.getGameProfile().getName());
            renameNpcs(p.server);
        }
        openEditor(p);
        result(p, err == null ? Component.translatable("rtshop.att.saved") : Component.literal(com.rtshop.story.StoryText.saveError(p, err)), err == null);
    }

    /** 설정의 NPC 이름이 바뀌었으면 이미 세워 둔 출석 NPC 이름도 바꾼다. */
    private static void renameNpcs(MinecraftServer server) {
        String name = com.rtshop.shop.NpcNames.display("attendance", AttendanceConfig.get().npcName);
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity e : level.getAllEntities()) {
                if (e.getTags().contains(NPC_TAG) && (e.getCustomName() == null || !e.getCustomName().getString().equals(name))) {
                    e.setCustomName(Component.literal(name));
                }
            }
        }
    }

    // ------------------------------------------------------------------ 창에 보낼 내용

    private static JsonObject rewardJson(AttendanceConfig.Reward r) {
        return r.toJson();
    }

    public static JsonObject view(ServerPlayer p, int npcId) {
        AttendanceConfig c = AttendanceConfig.get();
        AttendanceData.Entry e = entry(p);
        long now = clock.getAsLong();
        long today = dayOf(now, c);
        JsonObject o = new JsonObject();
        o.addProperty("npc", com.rtshop.story.StoryText.t(p, c.npcName));
        o.addProperty("atNpc", npcId >= 0);
        o.addProperty("canEdit", canEdit(p));
        o.addProperty("zone", c.zone().getId());
        o.addProperty("now", now);
        o.addProperty("streak", e.streak);
        o.addProperty("total", e.total);
        o.addProperty("checkedToday", e.lastDay == today);
        o.addProperty("nextDay", dayStart(today + 1, c));
        o.addProperty("grace", c.grace);
        o.addProperty("graceUsed", e.graceUsed);
        o.addProperty("loop", c.loop);
        boolean windowOpen = e.savedStreak > 0 && e.resetAt > 0 && now < e.resetAt + (long) (c.restoreWindowHours * HOUR);
        o.addProperty("savedStreak", windowOpen ? e.savedStreak : 0);
        o.addProperty("restoreUntil", windowOpen ? e.resetAt + (long) (c.restoreWindowHours * HOUR) : 0);
        long cd = e.restoreAt > 0 ? e.restoreAt + (long) (c.restoreCooldownHours * HOUR) : 0;
        o.addProperty("cooldownUntil", cd > now ? cd : 0);
        o.addProperty("restorable", restorable(e, now, c));
        o.addProperty("tickets", p.getInventory().countItem(ModItems.RESTORE_TICKET.get()));
        o.addProperty("shop", ShopServer.shops().get(c.shop) != null);
        // 오늘 출석하면 받을 날 / 이미 받은 날
        int next = e.lastDay == today ? rewardIndex(e.streak, c) : rewardIndex(e.streak + 1, c);
        o.addProperty("todayIndex", next);
        int done;
        if (e.streak <= 0) {
            done = 0;
        } else if (c.loop) {
            int n = c.days.size();
            int inCycle = ((e.streak - 1) % n) + 1;
            done = e.lastDay == today ? inCycle : (inCycle == n ? 0 : inCycle);
        } else {
            done = Math.min(e.streak, c.days.size());
        }
        o.addProperty("doneInCycle", done);
        JsonArray days = new JsonArray();
        for (AttendanceConfig.Reward r : c.days) {
            days.add(rewardJson(r));
        }
        o.add("days", days);
        long w = weekOf(today, c);
        boolean sameWeek = e.week == w;
        o.addProperty("weekMask", sameWeek ? e.weekMask : 0);
        o.addProperty("weekToday", (int) Math.max(0, Math.min(6, today - w))); // 이번 주의 몇째 날인가 (0 = 주의 첫날): 지나간 날 가운데 빠진 날을 표시하려고
        o.addProperty("weekStartDay", c.weekStartDay().getValue());
        JsonArray weekly = new JsonArray();
        for (AttendanceConfig.Weekly t : c.weekly) {
            JsonObject wo = rewardJson(t.reward);
            wo.addProperty("days", t.days);
            wo.addProperty("claimed", sameWeek && (e.weekClaimed & (1 << (t.days - 1))) != 0);
            weekly.add(wo);
        }
        o.add("weekly", weekly);
        // 내 출석 기록 (최근 것부터 45번): 날짜와 시각
        JsonArray hist = new JsonArray();
        for (int i = e.history.size() - 1; i >= 0 && hist.size() < 45; i--) hist.add(e.history.get(i));
        o.add("history", hist);
        return o;
    }

    /** 오늘 출석을 아직 받지 않았는가 (출석 NPC 가 세워져 있을 때만: HUD 안내용). */
    public static boolean dueToday(ServerPlayer p) {
        if (com.rtshop.quest.NpcPlaces.where(p.server, "attendance").isEmpty()) return false;
        AttendanceConfig c = AttendanceConfig.get();
        return entry(p).lastDay != dayOf(clock.getAsLong(), c);
    }

    private static String time(ServerPlayer p, long ms, AttendanceConfig c) {
        java.time.format.DateTimeFormatter f = com.rtshop.story.StoryText.en(p)
                ? java.time.format.DateTimeFormatter.ofPattern("MMM d, HH:mm", java.util.Locale.ENGLISH)
                : java.time.format.DateTimeFormatter.ofPattern("M월 d일 HH:mm");
        return f.format(Instant.ofEpochMilli(ms).atZone(c.zone()));
    }

    // ------------------------------------------------------------------ NPC

    public static Villager spawnNpc(ServerLevel level, Vec3 pos, float yaw) {
        Villager v = EntityType.VILLAGER.create(level);
        if (v == null) {
            return null;
        }
        v.moveTo(pos.x, pos.y, pos.z, yaw, 0.0F);
        v.setYHeadRot(yaw);   // 3.61: 몸과 머리도 그쪽을 보게 (moveTo 는 yRot 만 바꿔, 화면에서는 늘 남쪽을 보고 섰다)
        v.setYBodyRot(yaw);
        v.setNoAi(true);
        v.setInvulnerable(true);
        v.setSilent(true);
        v.setPersistenceRequired();
        v.addTag(NPC_TAG);
        v.setCustomName(Component.literal(AttendanceConfig.get().npcName));
        v.setCustomNameVisible(true);
        v.setVillagerData(new VillagerData(VillagerType.PLAINS, VillagerProfession.LIBRARIAN, 5));
        level.addFreshEntity(v);
        return v;
    }

    public static boolean isNpc(Entity e) {
        return e.getTags().contains(NPC_TAG);
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() || !event.getTarget().getTags().contains(NPC_TAG)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getHand() == InteractionHand.MAIN_HAND && event.getEntity() instanceof ServerPlayer sp) {
            open(sp, event.getTarget().getId());
        }
    }

    // ------------------------------------------------------------------ 접속 / 날짜 바뀜 안내

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            refresh(sp);
            remind(sp, true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SESSIONS.remove(event.getEntity().getUUID());
    }

    /** 아직 오늘 출석하지 않았으면 하루에 한 번(접속 때는 항상) 알려 준다. */
    private static void remind(ServerPlayer p, boolean login) {
        AttendanceConfig c = AttendanceConfig.get();
        AttendanceData d = AttendanceData.get(p.server);
        AttendanceData.Entry e = d.of(p.getUUID());
        long today = dayOf(clock.getAsLong(), c);
        if (e.lastDay == today || (!login && e.remindedDay == today)) {
            return;
        }
        e.remindedDay = today;
        d.dirty();
        MutableComponent msg = Component.translatable("rtshop.att.remind", c.npcName, e.streak).withStyle(ChatFormatting.YELLOW);
        if (e.streak > 0 && c.grace && e.graceUsed) {
            msg.append(Component.translatable("rtshop.att.remind_strict").withStyle(ChatFormatting.RED));
        }
        if (restorable(e, clock.getAsLong(), c)) {
            msg.append(Component.translatable("rtshop.att.remind_restore", e.savedStreak,
                    time(p, e.resetAt + (long) (c.restoreWindowHours * HOUR), c)).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        msg.append(Component.literal(" "));
        msg.append(Component.translatable("rtshop.att.remind_link").withStyle(st -> st.withColor(ChatFormatting.GOLD).withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rtshop attendance"))));
        com.rtshop.notice.Notice.send(p, msg);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer() == null || event.getServer().getTickCount() % 600 != 300) {
            return;
        }
        for (ServerPlayer p : event.getServer().getPlayerList().getPlayers()) {
            refresh(p);
            remind(p, false);
        }
    }

    // ------------------------------------------------------------------ 명령어 (관리자)

    public static void setStreak(ServerPlayer p, int days) {
        AttendanceData d = AttendanceData.get(p.server);
        AttendanceData.Entry e = d.of(p.getUUID());
        AttendanceConfig c = AttendanceConfig.get();
        long today = dayOf(clock.getAsLong(), c);
        e.streak = Math.max(0, days);
        e.lastDay = e.streak > 0 ? today - 1 : -1; // 오늘 출석하면 이어진다
        e.savedStreak = 0;
        e.resetAt = 0;
        d.dirty();
    }

    public static void reset(ServerPlayer p) {
        AttendanceData.get(p.server).clear(p.getUUID());
    }
}

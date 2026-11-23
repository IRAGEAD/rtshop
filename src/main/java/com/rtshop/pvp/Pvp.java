package com.rtshop.pvp;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.rtshop.RtShop;
import com.rtshop.land.Borders;
import com.rtshop.land.CastlePlots;
import com.rtshop.net.PvpPacket;
import com.rtshop.net.ShopNet;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * PvP 켜기/끄기.
 *  - 지역: 안전 구역(마을·성 영역, OP 가 정한 안전 구역, 레이드 구역) / PvP 가능 구역 / 그 밖(outside 설정에 따라 가능·불가).
 *  - 플레이어는 PvP 가 가능한 곳에서만 /pvp on 을 할 수 있고, 가능한 곳을 벗어나면 저절로 꺼진다.
 *  - 때리는 쪽과 맞는 쪽이 모두 PvP 를 켜고, 둘 다 PvP 가 가능한 곳에 있어야 피해가 들어간다 (화살·마법·길들인 동물 포함).
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class Pvp {
    private Pvp() {
    }

    /** PvP 를 켤 수 없는 곳 (구역 밖이고 바깥 PvP 가 꺼져 있음). */
    public static final int AREA_OFF = 0;
    /** PvP 가능 구역. */
    public static final int AREA_ZONE = 1;
    /** 안전 구역 (마을·성 영역·레이드 구역). */
    public static final int AREA_SAFE = 2;
    /** 구역 밖이지만 바깥 PvP 가 켜져 있음. */
    public static final int AREA_WILD = 3;

    private static final String NBT = "rtshop_pvp";

    /** 다른 기능이 "여기는 안전 구역" 이라고 알려 주는 자리 (레이드 구역). */
    public interface SafeTest {
        boolean safe(ServerLevel level, int x, int z);
    }

    public static final List<SafeTest> EXTRA_SAFE = new ArrayList<>();

    private static final class St {
        long combatUntil;
        long lastSwitch;
        long lastWarn;
        int area = -1;
        int sent = Integer.MIN_VALUE;
    }

    private static final Map<UUID, St> STATE = new HashMap<>();
    private static final Map<UUID, Object[]> POS1 = new HashMap<>();
    private static final Map<UUID, Object[]> POS2 = new HashMap<>();
    private static final Set<UUID> SHOW = new HashSet<>();
    /** 점검용: 시계를 앞으로 돌린다 (ms). */
    public static long testClockOffset = 0;

    private static long now() {
        return System.currentTimeMillis() + testClockOffset;
    }

    private static St st(ServerPlayer p) {
        return STATE.computeIfAbsent(p.getUUID(), k -> new St());
    }

    // ------------------------------------------------------------------ 상태

    public static boolean on(Player p) {
        return p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getBoolean(NBT);
    }

    private static void store(Player p, boolean on) {
        CompoundTag root = p.getPersistentData();
        CompoundTag keep = root.getCompound(Player.PERSISTED_NBT_TAG);
        if (on) {
            keep.putBoolean(NBT, true);
        } else {
            keep.remove(NBT);
        }
        root.put(Player.PERSISTED_NBT_TAG, keep);
    }

    public static String dim(Level level) {
        return level.dimension().location().toString();
    }

    /** 성 영역 안인가 (성 안 토지를 넣은 세계의 오버월드에서만). */
    public static boolean inCastle(ServerLevel level, int x, int z) {
        try {
            return level.dimension() == Level.OVERWORLD && CastlePlots.zoneKnown(level.getServer()) && CastlePlots.inZone(x, z);
        } catch (Throwable t) {
            return false;
        }
    }

    public static int area(ServerLevel level, double px, double pz) {
        PvpConfig cfg = PvpConfig.get();
        int x = Mth.floor(px), z = Mth.floor(pz);
        for (SafeTest t : EXTRA_SAFE) {
            if (t.safe(level, x, z)) {
                return AREA_SAFE;
            }
        }
        if (cfg.castleSafe && inCastle(level, x, z)) {
            return AREA_SAFE;
        }
        String dim = dim(level);
        boolean zone = false;
        for (PvpConfig.Zone zn : cfg.zones) {
            if (zn.has(dim, x, z)) {
                if (zn.safe) {
                    return AREA_SAFE;
                }
                zone = true;
            }
        }
        if (zone) {
            return AREA_ZONE;
        }
        return cfg.outside ? AREA_WILD : AREA_OFF;
    }

    public static int area(ServerPlayer p) {
        return area(p.serverLevel(), p.getX(), p.getZ());
    }

    public static boolean capable(int area) {
        return area == AREA_ZONE || area == AREA_WILD;
    }

    /** 전투 중이라 PvP 를 끌 수 없는 남은 시간(초). */
    public static int lockSeconds(ServerPlayer p) {
        St s = STATE.get(p.getUUID());
        if (s == null) {
            return 0;
        }
        long left = s.combatUntil - now();
        return left <= 0 ? 0 : (int) ((left + 999) / 1000);
    }

    /**
     * PvP 를 켜거나 끈다 (want: null = 반대로). 켤 수 없는 곳이거나 전투 중이면 바꾸지 않고 이유를 알려 준다.
     * @return 바뀌었으면 true
     */
    public static boolean set(ServerPlayer p, Boolean want, boolean force) {
        PvpConfig cfg = PvpConfig.get();
        boolean cur = on(p);
        boolean next = want == null ? !cur : want;
        St s = st(p);
        if (!cfg.enabled && !force) {
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.pvp.system_off").withStyle(ChatFormatting.GRAY), false);
            return false;
        }
        if (next == cur) {
            com.rtshop.notice.Notice.send(p, Component.translatable(cur ? "rtshop.pvp.already_on" : "rtshop.pvp.already_off").withStyle(ChatFormatting.GRAY), false);
            return false;
        }
        if (!force) {
            if (next) {
                int a = area(p);
                if (!capable(a)) {
                    com.rtshop.notice.Notice.send(p, Component.translatable(a == AREA_SAFE ? "rtshop.pvp.deny_safe" : "rtshop.pvp.deny_area")
                            .withStyle(ChatFormatting.RED), false);
                    return false;
                }
            } else {
                int lock = lockSeconds(p);
                if (lock > 0) {
                    com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.pvp.deny_combat", lock).withStyle(ChatFormatting.RED), false);
                    return false;
                }
            }
            long wait = s.lastSwitch + cfg.switchSeconds * 1000L - now();
            if (wait > 0) {
                com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.pvp.deny_cooldown", (wait + 999) / 1000).withStyle(ChatFormatting.RED), false);
                return false;
            }
        }
        store(p, next);
        s.lastSwitch = now();
        com.rtshop.notice.Notice.send(p, Component.translatable(next ? "rtshop.pvp.on" : "rtshop.pvp.off")
                .withStyle(next ? ChatFormatting.RED : ChatFormatting.GREEN), false);
        sync(p);
        return true;
    }

    /**
     * attacker 가 victim 을 때릴 수 없는 이유 (lang 키). 때려도 되면 null.
     */
    public static String denyReason(ServerPlayer attacker, ServerPlayer victim) {
        PvpConfig cfg = PvpConfig.get();
        if (!cfg.enabled) {
            return null;
        }
        int aa = area(attacker), va = area(victim);
        if (aa == AREA_SAFE || va == AREA_SAFE) {
            return "rtshop.pvp.hit_safe";
        }
        if (!capable(aa) || !capable(va)) {
            return "rtshop.pvp.hit_area";
        }
        if (!on(attacker)) {
            return "rtshop.pvp.hit_self_off";
        }
        if (!on(victim)) {
            return "rtshop.pvp.hit_other_off";
        }
        return null;
    }

    /** 이 피해를 준 플레이어 (직접, 화살·마법, 길들인 동물·소환수). 플레이어가 아니면 null. */
    public static ServerPlayer attackerOf(DamageSource src, MinecraftServer server) {
        ServerPlayer p = owner(src.getEntity(), server, 0);
        return p != null ? p : owner(src.getDirectEntity(), server, 0);
    }

    private static ServerPlayer owner(Entity e, MinecraftServer server, int depth) {
        if (e == null || depth > 3) {
            return null;
        }
        if (e instanceof ServerPlayer sp) {
            return sp;
        }
        try {
            if (e instanceof OwnableEntity o) {
                UUID id = o.getOwnerUUID();
                if (id != null && server != null) {
                    ServerPlayer sp = server.getPlayerList().getPlayer(id);
                    if (sp != null) {
                        return sp;
                    }
                }
            }
            if (e instanceof TraceableEntity t) {
                Entity ow = t.getOwner();
                if (ow != null && ow != e) {
                    return owner(ow, server, depth + 1);
                }
            }
        } catch (Throwable ignored) {
            // 다른 모드의 엔티티가 주인 조회에서 실패해도 무시
        }
        return null;
    }

    // ------------------------------------------------------------------ 이벤트

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim) || victim instanceof FakePlayer) {
            return;
        }
        ServerPlayer attacker = attackerOf(event.getSource(), victim.getServer());
        if (attacker == null || attacker == victim || attacker instanceof FakePlayer) {
            return; // 기계(가짜 플레이어)의 공격은 이 규칙으로 막지 않는다
        }
        String why = denyReason(attacker, victim);
        if (why != null) {
            event.setCanceled(true);
            St s = st(attacker);
            if (now() - s.lastWarn > 1500) {
                s.lastWarn = now();
                com.rtshop.notice.Notice.send(attacker, Component.translatable(why, victim.getDisplayName()).withStyle(ChatFormatting.RED), true);
            }
            return;
        }
        combat(attacker);
        combat(victim);
    }

    /** 전투 시작/연장: 이 시간 동안은 PvP 를 끌 수 없다. */
    public static void combat(ServerPlayer p) {
        st(p).combatUntil = now() + PvpConfig.get().combatSeconds * 1000L;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 10 != 0) {
            return;
        }
        for (ServerPlayer p : event.getServer().getPlayerList().getPlayers()) {
            try {
                tick(p);
            } catch (Throwable t) {
                RtShop.LOGGER.debug("[rtshop] pvp tick", t);
            }
        }
    }

    /** 지역이 바뀌었으면 알리고, PvP 를 켤 수 없는 곳이면 끈다. 바뀐 것이 있으면 클라이언트에 보낸다. */
    public static void tick(ServerPlayer p) {
        PvpConfig cfg = PvpConfig.get();
        St s = st(p);
        int a = area(p);
        if (a != s.area) {
            int before = s.area;
            s.area = a;
            if (cfg.enabled && cfg.announceArea && before >= 0) {
                String key = a == AREA_SAFE ? "rtshop.pvp.enter_safe" : capable(a) ? (capable(before) ? null : "rtshop.pvp.enter_zone")
                        : (capable(before) ? "rtshop.pvp.leave_zone" : null);
                if (key != null) {
                    com.rtshop.notice.Notice.send(p, Component.translatable(key).withStyle(a == AREA_SAFE ? ChatFormatting.GREEN
                            : capable(a) ? ChatFormatting.GOLD : ChatFormatting.GRAY), true);
                }
            }
        }
        if (cfg.enabled && on(p) && !capable(a)) {
            store(p, false);
            s.combatUntil = 0;
            com.rtshop.notice.Notice.send(p, Component.translatable(a == AREA_SAFE ? "rtshop.pvp.auto_off_safe" : "rtshop.pvp.auto_off")
                    .withStyle(ChatFormatting.YELLOW), false);
        }
        int hash = (on(p) ? 1 : 0) | (a << 1) | (lockSeconds(p) << 4) | (cfg.enabled ? 1 << 20 : 0);
        if (hash != s.sent) {
            s.sent = hash;
            send(p, a);
        }
    }

    public static void sync(ServerPlayer p) {
        St s = st(p);
        s.sent = Integer.MIN_VALUE;
        try {
            tick(p);
        } catch (Throwable ignored) {
        }
    }

    private static void send(ServerPlayer p, int area) {
        if (p instanceof FakePlayer || p.connection == null) {
            return;
        }
        try {
            ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
                    new PvpPacket(PvpConfig.get().enabled, on(p), area, lockSeconds(p)));
        } catch (Exception ignored) {
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            STATE.remove(p.getUUID());
            sync(p);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            sync(p);
        }
    }

    @SubscribeEvent
    public static void onChangeDim(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            sync(p);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        STATE.remove(id);
        POS1.remove(id);
        POS2.remove(id);
        SHOW.remove(id);
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        PvpConfig cfg = PvpConfig.reload();
        MinecraftServer server = event.getServer();
        if (cfg.enabled && cfg.manageServerPvp && !server.isPvpAllowed()) {
            server.setPvpAllowed(true);
            RtShop.LOGGER.info("[rtshop] server.properties 의 pvp 가 꺼져 있어 켰습니다: PvP 는 RT Shop 의 PvP 구역·켜기/끄기 규칙으로 관리됩니다 (pvp.json 의 manageServerPvp)");
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        STATE.clear();
        POS1.clear();
        POS2.clear();
        SHOW.clear();
    }

    /** 구역 경계 보기를 켠 OP 에게 근처 구역의 둘레를 보여 준다 (LandEvents 의 경계 표시 주기에서 부른다). */
    public static void borders(ServerPlayer p) {
        if (!SHOW.contains(p.getUUID())) {
            return;
        }
        String dim = dim(p.level());
        int shown = 0;
        for (PvpConfig.Zone z : PvpConfig.get().zones) {
            if (!z.dim.equals(dim)) continue;
            int dx = Math.max(Math.max(z.x0 - p.getBlockX(), p.getBlockX() - z.x1), 0);
            int dz = Math.max(Math.max(z.z0 - p.getBlockZ(), p.getBlockZ() - z.z1), 0);
            if (dx > 96 || dz > 96) continue;
            Borders.columns(p, (x, zz) -> true, z.x0, z.z0, z.x1, z.z1, p.level().getMinBuildHeight(), p.level().getMaxBuildHeight() - 1,
                    z.safe ? 0x55FF7F : 0xFF4040);
            if (++shown >= 6) break;
        }
        Object[] a = POS1.get(p.getUUID()), b = POS2.get(p.getUUID());
        if (a != null && b != null && a[0].equals(dim) && b[0].equals(dim)) {
            int x0 = Math.min((int) a[1], (int) b[1]), x1 = Math.max((int) a[1], (int) b[1]);
            int z0 = Math.min((int) a[2], (int) b[2]), z1 = Math.max((int) a[2], (int) b[2]);
            Borders.columns(p, (x, zz) -> true, x0, z0, x1, z1, p.level().getMinBuildHeight(), p.level().getMaxBuildHeight() - 1, 0xFFFFFF);
        }
    }

    // ------------------------------------------------------------------ 명령어

    private static final SuggestionProvider<CommandSourceStack> ZONES = (ctx, b) -> {
        List<String> names = new ArrayList<>();
        for (PvpConfig.Zone z : PvpConfig.get().zones) names.add(z.name);
        return SharedSuggestionProvider.suggest(names, b);
    };

    private static MutableComponent areaName(int a) {
        return Component.translatable(a == AREA_SAFE ? "rtshop.pvp.area_safe" : a == AREA_ZONE ? "rtshop.pvp.area_zone"
                : a == AREA_WILD ? "rtshop.pvp.area_wild" : "rtshop.pvp.area_off");
    }

    private static int status(ServerPlayer p) {
        int a = area(p);
        com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.pvp.status",
                Component.translatable(on(p) ? "rtshop.pvp.word_on" : "rtshop.pvp.word_off").withStyle(on(p) ? ChatFormatting.RED : ChatFormatting.GREEN),
                areaName(a).withStyle(a == AREA_SAFE ? ChatFormatting.GREEN : capable(a) ? ChatFormatting.GOLD : ChatFormatting.GRAY)), false);
        return 1;
    }

    /** /pvp [on|off|status] : 누구나. */
    public static LiteralArgumentBuilder<CommandSourceStack> playerCommand(String name) {
        return Commands.literal(name)
                .executes(ctx -> set(ctx.getSource().getPlayerOrException(), null, false) ? 1 : 0)
                .then(Commands.literal("on").executes(ctx -> set(ctx.getSource().getPlayerOrException(), true, false) ? 1 : 0))
                .then(Commands.literal("off").executes(ctx -> set(ctx.getSource().getPlayerOrException(), false, false) ? 1 : 0))
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource().getPlayerOrException())));
    }

    private static void say(CommandContext<CommandSourceStack> ctx, Component msg) {
        ctx.getSource().sendSuccess(() -> msg, true);
    }

    private static int setPos(CommandContext<CommandSourceStack> ctx, boolean first) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        Object[] v = {dim(p.level()), p.getBlockX(), p.getBlockZ()};
        (first ? POS1 : POS2).put(p.getUUID(), v);
        SHOW.add(p.getUUID());
        say(ctx, Component.translatable(first ? "rtshop.pvp.pos1" : "rtshop.pvp.pos2", v[1], v[2]));
        return 1;
    }

    private static int addZone(CommandContext<CommandSourceStack> ctx, boolean safe, int radius) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(ctx, "name");
        PvpConfig cfg = PvpConfig.get();
        if (cfg.zone(name) != null) {
            ctx.getSource().sendFailure(Component.translatable("rtshop.pvp.zone_exists", name));
            return 0;
        }
        PvpConfig.Zone z = new PvpConfig.Zone();
        z.name = name;
        z.safe = safe;
        z.dim = dim(p.level());
        if (radius > 0) {
            z.x0 = p.getBlockX() - radius;
            z.x1 = p.getBlockX() + radius;
            z.z0 = p.getBlockZ() - radius;
            z.z1 = p.getBlockZ() + radius;
        } else {
            Object[] a = POS1.get(p.getUUID()), b = POS2.get(p.getUUID());
            if (a == null || b == null || !a[0].equals(z.dim) || !b[0].equals(z.dim)) {
                ctx.getSource().sendFailure(Component.translatable("rtshop.pvp.need_pos"));
                return 0;
            }
            z.x0 = Math.min((int) a[1], (int) b[1]);
            z.x1 = Math.max((int) a[1], (int) b[1]);
            z.z0 = Math.min((int) a[2], (int) b[2]);
            z.z1 = Math.max((int) a[2], (int) b[2]);
            POS1.remove(p.getUUID());
            POS2.remove(p.getUUID());
        }
        cfg.zones.add(z);
        cfg.save();
        SHOW.add(p.getUUID());
        say(ctx, Component.translatable(safe ? "rtshop.pvp.zone_added_safe" : "rtshop.pvp.zone_added", name, z.x0, z.z0, z.x1, z.z1,
                z.x1 - z.x0 + 1, z.z1 - z.z0 + 1));
        resyncAll(ctx.getSource().getServer());
        return 1;
    }

    public static void resyncAll(MinecraftServer server) {
        if (server == null || server.getPlayerList() == null) {
            return;
        }
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            sync(p);
        }
    }

    /** /rtshop pvp ... : OP 설정. */
    public static LiteralArgumentBuilder<CommandSourceStack> adminCommand() {
        return Commands.literal("pvp").requires(src -> src.hasPermission(2))
                .executes(ctx -> {
                    PvpConfig c = PvpConfig.get();
                    say(ctx, Component.translatable("rtshop.pvp.admin_status",
                            Component.translatable(c.enabled ? "rtshop.pvp.word_on" : "rtshop.pvp.word_off"),
                            Component.translatable(c.outside ? "rtshop.pvp.word_allowed" : "rtshop.pvp.word_denied"),
                            c.zones.size(), c.combatSeconds));
                    return 1;
                })
                .then(Commands.literal("enable").executes(ctx -> {
                    PvpConfig.get().enabled = true;
                    PvpConfig.get().save();
                    say(ctx, Component.translatable("rtshop.pvp.enabled"));
                    resyncAll(ctx.getSource().getServer());
                    return 1;
                }))
                .then(Commands.literal("disable").executes(ctx -> {
                    PvpConfig.get().enabled = false;
                    PvpConfig.get().save();
                    say(ctx, Component.translatable("rtshop.pvp.disabled"));
                    resyncAll(ctx.getSource().getServer());
                    return 1;
                }))
                .then(Commands.literal("outside")
                        .then(Commands.literal("on").executes(ctx -> {
                            PvpConfig.get().outside = true;
                            PvpConfig.get().save();
                            say(ctx, Component.translatable("rtshop.pvp.outside_on"));
                            resyncAll(ctx.getSource().getServer());
                            return 1;
                        }))
                        .then(Commands.literal("off").executes(ctx -> {
                            PvpConfig.get().outside = false;
                            PvpConfig.get().save();
                            say(ctx, Component.translatable("rtshop.pvp.outside_off"));
                            resyncAll(ctx.getSource().getServer());
                            return 1;
                        })))
                .then(Commands.literal("combat").then(Commands.argument("seconds", IntegerArgumentType.integer(0, 600)).executes(ctx -> {
                    PvpConfig.get().combatSeconds = IntegerArgumentType.getInteger(ctx, "seconds");
                    PvpConfig.get().save();
                    say(ctx, Component.translatable("rtshop.pvp.combat_set", PvpConfig.get().combatSeconds));
                    return 1;
                })))
                .then(Commands.literal("pos1").executes(ctx -> setPos(ctx, true)))
                .then(Commands.literal("pos2").executes(ctx -> setPos(ctx, false)))
                .then(Commands.literal("set").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.literal("on").executes(ctx -> set(EntityArgument.getPlayer(ctx, "player"), true, true) ? 1 : 0))
                        .then(Commands.literal("off").executes(ctx -> set(EntityArgument.getPlayer(ctx, "player"), false, true) ? 1 : 0))))
                .then(Commands.literal("zone")
                        .then(Commands.literal("add").then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> addZone(ctx, false, 0))
                                .then(Commands.literal("safe").executes(ctx -> addZone(ctx, true, 0)))))
                        .then(Commands.literal("here").then(Commands.argument("name", StringArgumentType.word())
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 5000))
                                        .executes(ctx -> addZone(ctx, false, IntegerArgumentType.getInteger(ctx, "radius")))
                                        .then(Commands.literal("safe").executes(ctx -> addZone(ctx, true, IntegerArgumentType.getInteger(ctx, "radius")))))))
                        .then(Commands.literal("remove").then(Commands.argument("name", StringArgumentType.word()).suggests(ZONES).executes(ctx -> {
                            PvpConfig c = PvpConfig.get();
                            PvpConfig.Zone z = c.zone(StringArgumentType.getString(ctx, "name"));
                            if (z == null) {
                                ctx.getSource().sendFailure(Component.translatable("rtshop.pvp.zone_unknown", StringArgumentType.getString(ctx, "name")));
                                return 0;
                            }
                            c.zones.remove(z);
                            c.save();
                            say(ctx, Component.translatable("rtshop.pvp.zone_removed", z.name));
                            resyncAll(ctx.getSource().getServer());
                            return 1;
                        })))
                        .then(Commands.literal("list").executes(ctx -> {
                            PvpConfig c = PvpConfig.get();
                            if (c.zones.isEmpty()) {
                                say(ctx, Component.translatable("rtshop.pvp.zone_none"));
                                return 1;
                            }
                            for (PvpConfig.Zone z : c.zones) {
                                say(ctx, Component.translatable(z.safe ? "rtshop.pvp.zone_line_safe" : "rtshop.pvp.zone_line", z.name, z.x0, z.z0, z.x1, z.z1,
                                        z.dim.replace("minecraft:", "")));
                            }
                            return c.zones.size();
                        }))
                        .then(Commands.literal("show").executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            boolean nowOn = !SHOW.remove(p.getUUID());
                            if (nowOn) SHOW.add(p.getUUID());
                            say(ctx, Component.translatable(nowOn ? "rtshop.pvp.show_on" : "rtshop.pvp.show_off"));
                            return 1;
                        })));
    }

    // ------------------------------------------------------------------ 점검용

    public static void forgetForTest(ServerPlayer p) {
        STATE.remove(p.getUUID());
        store(p, false);
    }

    public static boolean showingForTest(ServerPlayer p) {
        return SHOW.contains(p.getUUID());
    }
}

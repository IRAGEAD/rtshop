package com.rtshop.rules;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.rtshop.RtShop;
import com.rtshop.util.Perm;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

/**
 * 마을의 기준 자리 (config/rtshop/town_spawn.json, 3.52).
 * <ul>
 * <li>기본 스폰: 운영자가 /rtshop spawn set 으로 정한 자리. 처음 들어온 사람은 여기서 시작하고, 침대(리스폰 정박기)가 없는 사람은 죽으면 여기로 돌아온다.</li>
 * <li>귀환권의 도착 자리도 여기다. 일반 귀환권은 같은 차원에서 이 자리로부터 가로 거리 normalRange(3000) 안에서만,
 *     고급 귀환권은 어디서든 쓸 수 있고 하루에 premiumDailyLimit(5)장까지만 살 수 있다.</li>
 * <li>3.53: 다시 쓰기까지의 시간은 실제 시간으로 잰다 (일반 60분, 고급 30분). 사람마다 따로 기록되어 나갔다 들어와도, 죽어도 남는다.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class TownSpawn {
    private TownSpawn() {
    }

    public static final class Config {
        public boolean set;
        public String dim = "minecraft:overworld";
        public double x, y, z;
        public float yaw, pitch;
        /** 처음 들어온 사람을 이 자리로 보낸다 / 침대 없이 죽은 사람을 이 자리에서 되살린다. */
        public boolean firstJoin = true, respawn = true;
        public static final int VERSION = 2;
        public int version = VERSION;
        public int normalRange = 3000;
        public int premiumDailyLimit = 5;
        /** 귀환권을 쓰는 데 걸리는 시간(틱). */
        public int channelTicks = 60;
        /** 쓴 뒤 같은 종류의 귀환권을 다시 쓰기까지의 시간(분, 실제 시간). 0 이면 바로 다시 쓸 수 있다. */
        public int normalCooldownMinutes = 60, premiumCooldownMinutes = 30;
        /** 옛 파일(2판 이전)을 읽어 새 기본값으로 고쳤다: 파일을 다시 쓴다. */
        transient boolean migrated;

        public int cooldownMinutes(boolean premium) {
            return premium ? premiumCooldownMinutes : normalCooldownMinutes;
        }

        JsonObject toJson() {
            JsonObject o = new JsonObject();
            o.addProperty("_help", "마을의 기준 자리. /rtshop spawn set (OP, 크리에이티브) 으로 서 있는 자리를 정합니다. firstJoin: 처음 들어온 사람의 시작 자리, respawn: 침대 없는 사람이 되살아나는 자리. "
                    + "귀환권: normalRange = 일반 귀환권을 쓸 수 있는 가로 거리, premiumDailyLimit = 고급 귀환권의 하루 구매 수, channelTicks = 쓰는 데 걸리는 틱, "
                    + "normalCooldownMinutes / premiumCooldownMinutes = 일반 / 고급 귀환권을 쓴 뒤 같은 귀환권을 다시 쓰기까지의 시간(분, 실제 시간. 0 = 없음).");
            o.addProperty("version", VERSION);
            o.addProperty("set", set);
            o.addProperty("dim", dim);
            o.addProperty("x", x);
            o.addProperty("y", y);
            o.addProperty("z", z);
            o.addProperty("yaw", yaw);
            o.addProperty("pitch", pitch);
            o.addProperty("firstJoin", firstJoin);
            o.addProperty("respawn", respawn);
            o.addProperty("normalRange", normalRange);
            o.addProperty("premiumDailyLimit", premiumDailyLimit);
            o.addProperty("channelTicks", channelTicks);
            o.addProperty("normalCooldownMinutes", normalCooldownMinutes);
            o.addProperty("premiumCooldownMinutes", premiumCooldownMinutes);
            return o;
        }

        static Config fromJson(JsonObject o) {
            Config c = new Config();
            if (o.has("set")) c.set = o.get("set").getAsBoolean();
            if (o.has("dim")) c.dim = o.get("dim").getAsString();
            if (o.has("x")) c.x = o.get("x").getAsDouble();
            if (o.has("y")) c.y = o.get("y").getAsDouble();
            if (o.has("z")) c.z = o.get("z").getAsDouble();
            if (o.has("yaw")) c.yaw = o.get("yaw").getAsFloat();
            if (o.has("pitch")) c.pitch = o.get("pitch").getAsFloat();
            if (o.has("firstJoin")) c.firstJoin = o.get("firstJoin").getAsBoolean();
            if (o.has("respawn")) c.respawn = o.get("respawn").getAsBoolean();
            if (o.has("normalRange")) c.normalRange = Math.max(0, o.get("normalRange").getAsInt());
            if (o.has("premiumDailyLimit")) c.premiumDailyLimit = Math.max(0, o.get("premiumDailyLimit").getAsInt());
            if (o.has("channelTicks")) c.channelTicks = Math.max(1, Math.min(600, o.get("channelTicks").getAsInt()));
            if (o.has("normalCooldownMinutes")) c.normalCooldownMinutes = Math.max(0, o.get("normalCooldownMinutes").getAsInt());
            if (o.has("premiumCooldownMinutes")) c.premiumCooldownMinutes = Math.max(0, o.get("premiumCooldownMinutes").getAsInt());
            int v = o.has("version") ? o.get("version").getAsInt() : 1;
            if (v < 2) {
                // 3.53: 일반 귀환권의 거리 10000 -> 3000 (운영자가 따로 바꿔 둔 값은 그대로), 쿨타임은 새 기본값(60분 / 30분)
                if (c.normalRange == 10000) c.normalRange = 3000;
                c.migrated = true;
            }
            c.version = VERSION;
            return c;
        }
    }

    private static Config config;

    public static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("town_spawn.json");
    }

    public static synchronized Config get() {
        if (config == null) config = load();
        return config;
    }

    public static synchronized void reload() {
        config = load();
    }

    public static synchronized void setForTest(Config c) {
        config = c;
    }

    public static Config fresh() {
        return new Config();
    }

    private static Config load() {
        try {
            if (Files.exists(path())) {
                Config c = Config.fromJson(JsonParser.parseString(Files.readString(path(), StandardCharsets.UTF_8)).getAsJsonObject());
                if (c.migrated) {
                    c.migrated = false;
                    save(c);
                    RtShop.LOGGER.info("[rtshop] town_spawn.json 을 새 판으로 고쳤습니다 (일반 귀환권 거리 {}칸, 쿨타임 {}분 / {}분).", c.normalRange, c.normalCooldownMinutes, c.premiumCooldownMinutes);
                }
                return c;
            }
            Config c = new Config();
            save(c);
            return c;
        } catch (Exception e) {
            RtShop.LOGGER.error("[rtshop] town_spawn.json 을 읽지 못했습니다: {}", e.getMessage());
            return new Config();
        }
    }

    private static void save(Config c) {
        try {
            Files.createDirectories(path().getParent());
            Files.writeString(path(), new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(c.toJson()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            RtShop.LOGGER.error("[rtshop] town_spawn.json 을 쓰지 못했습니다: {}", e.getMessage());
        }
    }

    public static ServerLevel level(MinecraftServer server) {
        ResourceLocation rl = ResourceLocation.tryParse(get().dim);
        ServerLevel l = rl == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, rl));
        return l == null ? server.overworld() : l;
    }

    /** 이 사람을 마을의 기준 자리로 보낸다 (정해 두지 않았으면 false). */
    public static boolean send(ServerPlayer p) {
        Config c = get();
        if (!c.set) return false;
        ServerLevel to = level(p.server);
        p.stopRiding();
        p.teleportTo(to, c.x, c.y, c.z, c.yaw, c.pitch);
        p.fallDistance = 0;
        return true;
    }

    /** 이 자리에서 일반 귀환권을 쓸 수 있는가 (같은 차원, 가로 거리 안). */
    public static boolean inNormalRange(ServerPlayer p) {
        Config c = get();
        if (!c.set || !p.level().dimension().location().toString().equals(c.dim)) return false;
        double dx = p.getX() - c.x, dz = p.getZ() - c.z;
        return dx * dx + dz * dz <= (double) c.normalRange * c.normalRange;
    }

    // ------------------------------------------------------------------ 다시 쓰기까지의 시간 (3.53, 실제 시간)

    private static final String CD_KEY = "rtshop_return_cd";

    /** 점검용: 지금 시각(밀리초)을 정한다 (0 = 진짜 시각). */
    public static long testNow;

    private static long now() {
        return testNow != 0 ? testNow : System.currentTimeMillis();
    }

    private static net.minecraft.nbt.CompoundTag kept(ServerPlayer p) {
        return p.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
    }

    /** 이 종류의 귀환권을 다시 쓰기까지 남은 시간(밀리초). 0 이면 지금 쓸 수 있다. */
    public static long cooldownLeft(ServerPlayer p, boolean premium) {
        int minutes = get().cooldownMinutes(premium);
        if (minutes <= 0) return 0;
        long used = kept(p).getCompound(CD_KEY).getLong(premium ? "premium" : "normal");
        if (used <= 0) return 0;
        long left = used + minutes * 60000L - now();
        // 서버 시계가 뒤로 간 경우: 정해진 시간보다 길게 묶이지 않게 한다
        return Math.max(0, Math.min(left, minutes * 60000L));
    }

    /**
     * 귀환권을 썼다: 쓴 시각을 적는다. 물건 칸의 쿨타임 표시(흰 가림막)는 일부러 쓰지 않는다:
     * 그 표시가 걸린 물건은 우클릭 자체가 서버에 닿지 않아 "남은 시간" 안내를 띄울 수 없기 때문이다.
     */
    public static void used(ServerPlayer p, boolean premium) {
        net.minecraft.nbt.CompoundTag keep = kept(p);
        net.minecraft.nbt.CompoundTag t = keep.getCompound(CD_KEY);
        t.putLong(premium ? "premium" : "normal", now());
        keep.put(CD_KEY, t);
        p.getPersistentData().put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG, keep);
    }

    /** 점검용: 오늘 산 수를 지운다. */
    public static void clearBought(ServerPlayer p) {
        net.minecraft.nbt.CompoundTag keep = kept(p);
        keep.remove(BUY_KEY);
        p.getPersistentData().put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG, keep);
    }

    /** 점검용 · 운영자용: 이 사람의 귀환권 쿨타임을 지운다. */
    public static void clearCooldown(ServerPlayer p) {
        net.minecraft.nbt.CompoundTag keep = kept(p);
        keep.remove(CD_KEY);
        p.getPersistentData().put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG, keep);
    }

    /** 남은 시간을 읽기 좋게: "59분 30초", "45초". */
    public static Component leftText(long ms) {
        long sec = Math.max(1, (ms + 999) / 1000);
        long m = sec / 60, s2 = sec % 60;
        if (m <= 0) return Component.translatable("rtshop.return.left_s", s2);
        if (s2 == 0) return Component.translatable("rtshop.return.left_m", m);
        return Component.translatable("rtshop.return.left_ms", m, s2);
    }

    // ------------------------------------------------------------------ 하루 구매 수 (고급 귀환권)

    private static final String BUY_KEY = "rtshop_return_buys";

    /** 오늘 더 살 수 있는 수 (제한 없는 물건은 큰 수). */
    public static int dailyLeft(ServerPlayer p, ItemStack stack) {
        if (!stack.is(com.rtshop.item.ModItems.RETURN_TICKET_PLUS.get())) return Integer.MAX_VALUE;
        int limit = get().premiumDailyLimit;
        if (limit <= 0) return Integer.MAX_VALUE;
        net.minecraft.nbt.CompoundTag t = kept(p).getCompound(BUY_KEY);
        int used = today().equals(t.getString("day")) ? t.getInt("n") : 0;
        return Math.max(0, limit - used);
    }

    public static void bought(ServerPlayer p, ItemStack stack, int amount) {
        if (!stack.is(com.rtshop.item.ModItems.RETURN_TICKET_PLUS.get())) return;
        // 3.53: 죽어도 남는 자리에 적는다 (전에는 죽으면 하루 구매 수가 지워졌다)
        net.minecraft.nbt.CompoundTag keep = kept(p);
        net.minecraft.nbt.CompoundTag t = keep.getCompound(BUY_KEY);
        int used = today().equals(t.getString("day")) ? t.getInt("n") : 0;
        t.putString("day", today());
        t.putInt("n", used + amount);
        keep.put(BUY_KEY, t);
        p.getPersistentData().put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG, keep);
    }

    /** 점검용: 날짜를 정한다 (null = 진짜 오늘). */
    public static String testDay;

    private static String today() {
        return testDay != null ? testDay : LocalDate.now().toString();
    }

    // ------------------------------------------------------------------ 처음 들어온 사람 · 되살아남

    private static final String SEEN = "rtshop_town_spawned";

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        Config c = get();
        net.minecraft.nbt.CompoundTag keep = p.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
        if (keep.getBoolean(SEEN)) return;
        keep.putBoolean(SEEN, true);
        p.getPersistentData().put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG, keep);
        // 이미 놀던 사람(통계에 플레이 시간이 있는 사람)은 옮기지 않는다: 새로 온 사람만
        boolean fresh = p.getStats().getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.PLAY_TIME)) < 200;
        if (c.set && c.firstJoin && fresh) send(p);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || event.isEndConquered()) return;
        Config c = get();
        if (c.set && c.respawn && p.getRespawnPosition() == null) send(p);
    }

    // ------------------------------------------------------------------ 명령어

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("rtshop").then(Commands.literal("spawn")
                .then(Commands.literal("set").requires(s -> s.hasPermission(2)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    if (!Perm.op(p)) {
                        com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.perm.survival_op").withStyle(ChatFormatting.RED));
                        return 0;
                    }
                    setHere(p);
                    com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.spawn.set", (int) Math.floor(p.getX()), (int) Math.floor(p.getY()),
                            (int) Math.floor(p.getZ())).withStyle(ChatFormatting.GREEN));
                    return 1;
                }))
                .then(Commands.literal("clear").requires(s -> s.hasPermission(2)).executes(ctx -> {
                    Config c = get();
                    c.set = false;
                    save(c);
                    ctx.getSource().sendSuccess(() -> Component.translatable("rtshop.spawn.cleared"), true);
                    return 1;
                }))
                .then(Commands.literal("range").requires(s -> s.hasPermission(2)).then(Commands.argument("blocks", IntegerArgumentType.integer(0)).executes(ctx -> {
                    Config c = get();
                    c.normalRange = IntegerArgumentType.getInteger(ctx, "blocks");
                    save(c);
                    ctx.getSource().sendSuccess(() -> Component.translatable("rtshop.spawn.range", get().normalRange), true);
                    return 1;
                })))
                .then(Commands.literal("resetcd").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player()).executes(ctx -> {
                            ServerPlayer t = net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player");
                            clearCooldown(t);
                            ctx.getSource().sendSuccess(() -> Component.translatable("rtshop.spawn.cd_cleared", t.getGameProfile().getName()), true);
                            return 1;
                        }))
                        .executes(ctx -> {
                            ServerPlayer t = ctx.getSource().getPlayerOrException();
                            clearCooldown(t);
                            ctx.getSource().sendSuccess(() -> Component.translatable("rtshop.spawn.cd_cleared", t.getGameProfile().getName()), true);
                            return 1;
                        }))
                .executes(ctx -> {
                    Config c = get();
                    ctx.getSource().sendSuccess(() -> c.set ? Component.translatable("rtshop.spawn.where", c.dim, (int) Math.floor(c.x), (int) Math.floor(c.y), (int) Math.floor(c.z))
                            : Component.translatable("rtshop.spawn.none"), false);
                    return 1;
                })));
    }

    /** 이 사람이 선 자리를 마을의 기준 자리로 삼는다 (오버월드면 세계의 기본 스폰도 함께 옮긴다). */
    public static void setHere(ServerPlayer p) {
        Config c = get();
        c.set = true;
        c.dim = p.level().dimension().location().toString();
        c.x = Math.floor(p.getX()) + 0.5;
        c.y = p.getY();
        c.z = Math.floor(p.getZ()) + 0.5;
        c.yaw = p.getYRot();
        c.pitch = 0;
        save(c);
        if (p.level().dimension() == Level.OVERWORLD && p.level() instanceof ServerLevel sl) {
            sl.setDefaultSpawnPos(BlockPos.containing(c.x, c.y, c.z), c.yaw);
        }
    }
}

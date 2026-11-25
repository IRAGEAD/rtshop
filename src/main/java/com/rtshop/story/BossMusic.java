package com.rtshop.story;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.rtshop.RtShop;
import com.rtshop.net.ShopNet;
import com.rtshop.net.StoryPacket;
import com.rtshop.party.BossCredit;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.PacketDistributor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 보스·레이드 음악 (config/rtshop/boss_music.json).
 * <ul>
 * <li>보스(party.json 의 보스 목록)와 싸우는 동안 그 보스의 곡이 이어서 흐르고, 싸움이 끝나면(보스가 죽거나 멀어지면) 잦아든다.</li>
 * <li>레이드 구역에서 보스가 깨어나 있는 동안에는 레이드 곡이 흐른다.</li>
 * <li>곡은 이름(track)으로 가리킨다: 소리 rtshop:music.&lt;이름&gt; (assets/rtshop/sounds/music/&lt;이름&gt;.ogg). 그 소리가 없으면 아무것도 틀지 않는다.
 *     음악 파일은 tools/add_music.py 로 넣는다 (mp3·wav·ogg → ogg).</li>
 * <li>tracks: 보스 ID → 곡 이름. "*" 는 따로 정하지 않은 모든 보스의 곡. 값이 "" 이면 그 보스는 음악 없음.</li>
 * </ul>
 * 서버는 어느 곡을 틀지만 알려 주고(StoryPacket.MUSIC), 소리는 클라이언트(BossMusicClient)가 낸다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class BossMusic {
    private BossMusic() {
    }

    public static final class Config {
        public boolean enabled = true;
        /** 이 거리 안의 보스와 싸우고 있으면 음악이 흐른다. */
        public int range = 48;
        /** 음악이 잦아드는 데 걸리는 시간 (초). */
        public double fadeSeconds = 2.5;
        /** 다른 모드가 트는 보스 음악을 우리 곡이 흐르는 동안 막는다. */
        public boolean muteOtherBossMusic = true;
        public String raid = "raid";
        public final Map<String, String> tracks = new LinkedHashMap<>();

        public JsonObject toJson() {
            JsonObject o = new JsonObject();
            o.addProperty("_설명", "보스·레이드 음악. tracks: 보스 ID → 곡 이름 ('*' = 따로 정하지 않은 모든 보스, '' = 음악 없음). raid: 레이드 구역에서 보스가 깨어 있는 동안의 곡. "
                    + "곡 이름 x 는 소리 rtshop:music.x (모드 파일 안의 assets/rtshop/sounds/music/x.ogg) 를 가리킨다. 그 소리가 없으면 틀지 않는다. "
                    + "range: 이 거리 안의 보스와 싸우는 동안 흐른다. fadeSeconds: 잦아드는 시간. muteOtherBossMusic: 우리 곡이 흐르는 동안 다른 모드의 보스 음악을 막는다. 고친 뒤 /rtshop reload. "
                    + "미리 듣기: /rtshop music play <곡 이름>, 멈추기: /rtshop music stop");
            o.addProperty("enabled", enabled);
            o.addProperty("range", range);
            o.addProperty("fadeSeconds", fadeSeconds);
            o.addProperty("muteOtherBossMusic", muteOtherBossMusic);
            o.addProperty("raid", raid);
            JsonObject t = new JsonObject();
            tracks.forEach(t::addProperty);
            o.add("tracks", t);
            return o;
        }

        /** 이 보스의 곡 이름 (없으면 ""). */
        public String trackOf(String bossId) {
            String t = tracks.get(bossId);
            if (t == null) t = tracks.getOrDefault("*", "");
            return t == null ? "" : t.trim();
        }
    }

    private static Config config;

    public static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("boss_music.json");
    }

    public static Config fresh() {
        Config c = new Config();
        c.tracks.put("*", "boss");
        // 보스마다 다른 곡을 쓰려면 여기에 줄을 더한다 (곡 파일이 없으면 '*' 의 곡으로 넘어가지 않고 조용하다)
        return c;
    }

    public static synchronized Config get() {
        if (config == null) config = load();
        return config;
    }

    public static synchronized void reload() {
        config = load();
        SENT.clear();
    }

    public static synchronized void setForTest(Config c) {
        config = c;
        SENT.clear();
    }

    private static Config load() {
        Path p = path();
        Config c = fresh();
        try {
            if (Files.exists(p)) {
                JsonObject o = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
                if (o.has("enabled")) c.enabled = o.get("enabled").getAsBoolean();
                if (o.has("range")) c.range = Math.max(8, Math.min(128, o.get("range").getAsInt()));
                if (o.has("fadeSeconds")) c.fadeSeconds = Math.max(0, Math.min(20, o.get("fadeSeconds").getAsDouble()));
                if (o.has("muteOtherBossMusic")) c.muteOtherBossMusic = o.get("muteOtherBossMusic").getAsBoolean();
                if (o.has("raid")) c.raid = o.get("raid").getAsString();
                if (o.has("tracks")) {
                    c.tracks.clear();
                    o.getAsJsonObject("tracks").entrySet().forEach(e -> c.tracks.put(e.getKey(), e.getValue().getAsString()));
                }
            } else {
                Files.createDirectories(p.getParent());
                Files.writeString(p, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(c.toJson()), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            RtShop.LOGGER.error("[rtshop] boss_music.json 을 읽지 못했습니다: {}", e.getMessage());
        }
        return c;
    }

    // ------------------------------------------------------------------ 어느 곡을 틀까

    /** 플레이어 -> 마지막으로 보낸 "곡|보스 엔티티 번호". */
    private static final Map<UUID, String> SENT = new HashMap<>();
    /** 미리 듣기 중인 사람 (다음 명령까지 자동으로 바꾸지 않는다). */
    private static final Map<UUID, String> PREVIEW = new HashMap<>();

    /** 이 보스가 이 사람과 싸우고 있는가: 보스가 이 사람을 노리거나, 이 사람이 보스에게 피해를 입혔다. */
    static boolean fighting(LivingEntity boss, ServerPlayer p) {
        if (boss instanceof Mob m && m.getTarget() == p) return true;
        return BossCredit.dealt(boss, p) > 0;
    }

    /** 지금 이 사람에게 흘러야 하는 곡: {곡 이름, 보스 엔티티 번호(-1 = 없음)}. 없으면 {"", -1}. */
    public static Object[] wanted(ServerPlayer p) {
        Config c = get();
        if (!c.enabled || !p.isAlive()) return new Object[]{"", -1};
        if (com.rtshop.raid.Raid.state() == com.rtshop.raid.Raid.ACTIVE && com.rtshop.raid.Raid.inArena(p) && !c.raid.isBlank()) {
            return new Object[]{c.raid.trim(), -1};
        }
        List<LivingEntity> near = p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(c.range),
                e -> e.isAlive() && BossCredit.isBoss(e) && fighting(e, p));
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        for (LivingEntity e : near) {
            if (c.trackOf(BossCredit.id(e)).isEmpty()) continue;
            double d = e.distanceToSqr(p);
            if (d < bd) {
                bd = d;
                best = e;
            }
        }
        return best == null ? new Object[]{"", -1} : new Object[]{c.trackOf(BossCredit.id(best)), best.getId()};
    }

    private static void send(ServerPlayer p, String track, int entity) {
        if (p instanceof FakePlayer || p.connection == null) return;
        Config c = get();
        JsonObject o = new JsonObject();
        o.addProperty("track", track);
        o.addProperty("entity", entity);
        o.addProperty("fade", c.fadeSeconds);
        o.addProperty("range", c.range);
        o.addProperty("muteOthers", c.muteOtherBossMusic);
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new StoryPacket(StoryPacket.MUSIC, o.toString()));
    }

    /** 1초마다: 흘러야 하는 곡이 바뀌었으면 알린다. 바뀐 것이 있으면 true (점검용). */
    public static boolean update(ServerPlayer p) {
        if (PREVIEW.containsKey(p.getUUID())) return false;
        Object[] w = wanted(p);
        String key = w[0] + "|" + w[1];
        String old = SENT.get(p.getUUID());
        if (key.equals(old) || (old == null && ((String) w[0]).isEmpty())) return false;
        SENT.put(p.getUUID(), key);
        send(p, (String) w[0], (int) w[1]);
        return true;
    }

    public static String sentForTest(ServerPlayer p) {
        return SENT.get(p.getUUID());
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) return;
        if (event.player instanceof ServerPlayer sp && sp.tickCount % 20 == 7) update(sp);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SENT.remove(event.getEntity().getUUID());
        PREVIEW.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("rtshop").then(Commands.literal("music").requires(s -> s.hasPermission(2))
                .then(Commands.literal("play").then(Commands.argument("track", StringArgumentType.word()).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    String t = StringArgumentType.getString(ctx, "track");
                    PREVIEW.put(p.getUUID(), t);
                    send(p, t, -1);
                    ctx.getSource().sendSuccess(() -> Component.literal("미리 듣기: rtshop:music." + t + " (그 소리 파일이 없으면 아무 소리도 나지 않습니다). 멈추기: /rtshop music stop"), false);
                    return 1;
                })))
                .then(Commands.literal("stop").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    PREVIEW.remove(p.getUUID());
                    SENT.remove(p.getUUID());
                    send(p, "", -1);
                    return 1;
                }))
                .then(Commands.literal("reload").executes(ctx -> {
                    reload();
                    ctx.getSource().sendSuccess(() -> Component.literal("boss_music.json 을 다시 읽었습니다: " + get().tracks), false);
                    return 1;
                }))));
    }
}

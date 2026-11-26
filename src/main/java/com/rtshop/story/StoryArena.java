package com.rtshop.story;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import com.rtshop.party.Party;
import com.rtshop.quest.QuestDef;
import com.rtshop.quest.QuestManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 「기억의 문」: 이야기 보스전을 그 플레이어(와 파티)만 들어가는 따로 떨어진 공간에서 치른다.
 * 다른 플레이어가 끼어들거나, 진행도가 다른 사람끼리 보스를 가로채는 일이 없다.
 * 공간은 rtshop:memory 차원(빈 하늘)에 512칸 간격으로 여러 개가 동시에 열린다. 보스마다 어울리는 경기장 모양(테마)이 있다.
 * config/rtshop/story_arenas.json — 보스별 테마·반지름·물 채움.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class StoryArena {
    private StoryArena() {
    }

    public static final ResourceKey<Level> DIM = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(RtShop.MODID, "memory"));
    public static final String BOSS_TAG = "rtshop_arena_boss";
    private static final String NBT_FROM = "rtshop_arena_from";
    private static final int SPACING = 512, FLOOR_Y = 100, MAX_SLOTS = 64;

    // ------------------------------------------------------------------ 설정

    public static final class Spec {
        public String theme = "stone";
        public int radius = 22, height = 26;
        public boolean water;
    }

    private static Map<String, Spec> specs;

    /** 문을 연 사람이 들어갈 때 함께 데려가는 파티원의 거리 (칸). */
    public static int partyRange = 16;
    /** 세계에 열린 문이 아무도 들어가지 않은 채 열려 있는 시간 (초). */
    public static int gateSeconds = 60;
    /** 보스 구역에 들어선 뒤 불씨를 건드리지 않아도 보스가 깨어나기까지의 시간 (초). 0 = 불씨를 건드려야만 깨어난다. */
    public static int waitSeconds = 45;
    /** false 면 예전처럼 단추를 누르는 즉시 보스 구역으로 간다. */
    public static boolean walkIn = true;
    /** 깨어나는 연출의 길이 (틱). */
    public static final int WAKE_TICKS = 80;

    private static Spec spec(String theme, int radius, int height, boolean water) {
        Spec s = new Spec();
        s.theme = theme;
        s.radius = radius;
        s.height = height;
        s.water = water;
        return s;
    }

    public static Map<String, Spec> defaults() {
        Map<String, Spec> m = new LinkedHashMap<>();
        m.put("mowziesmobs:ferrous_wroughtnaut", spec("iron", 18, 16, false));
        m.put("mowziesmobs:frostmaw", spec("frost", 26, 24, false));
        m.put("mowziesmobs:umvuthi", spec("sun", 24, 24, false));
        m.put("mowziesmobs:elokosa_howler", spec("stone", 22, 18, false));
        m.put("aquamirae:maze_mother", spec("abyss", 26, 22, true));
        m.put("aquamirae:captain_cornelia", spec("frost", 24, 22, false));
        m.put("ydtwo:twisted_warden", spec("deep", 24, 22, false));
        m.put("minecraft:warden", spec("deep", 22, 20, false));
        m.put("minecraft:elder_guardian", spec("abyss", 22, 18, true));
        m.put("cataclysm:netherite_monstrosity", spec("ember", 30, 28, false));
        m.put("cataclysm:ancient_remnant", spec("sand", 30, 26, false));
        m.put("cataclysm:the_harbinger", spec("mech", 28, 32, false));
        m.put("cataclysm:the_leviathan", spec("abyss", 32, 30, true));
        m.put("cataclysm:ender_guardian", spec("void", 26, 26, false));
        m.put("cataclysm:maledictus", spec("curse", 26, 26, false));
        m.put("cataclysm:ignis", spec("ember", 30, 28, false));
        m.put("minecraft:wither", spec("curse", 30, 34, false));
        m.put("illusioner:illusioner", spec("mirror", 18, 16, false));
        m.put("minecraft:evoker", spec("stone", 20, 16, false));
        return m;
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("story_arenas.json");
    }

    public static synchronized Map<String, Spec> specs() {
        if (specs == null) reload();
        return specs;
    }

    public static synchronized void reload() {
        Map<String, Spec> m = defaults();
        partyRange = 16;
        gateSeconds = 60;
        waitSeconds = 45;
        walkIn = true;
        try {
            Path f = file();
            if (Files.exists(f)) {
                JsonObject o = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
                if (o.has("partyRange")) partyRange = Math.max(1, Math.min(64, o.get("partyRange").getAsInt()));
                if (o.has("gateSeconds")) gateSeconds = Math.max(10, Math.min(600, o.get("gateSeconds").getAsInt()));
                if (o.has("waitSeconds")) waitSeconds = Math.max(0, Math.min(600, o.get("waitSeconds").getAsInt()));
                if (o.has("walkIn")) walkIn = o.get("walkIn").getAsBoolean();
                if (!o.has("partyRange") || !o.has("waitSeconds")) {
                    // 3.58: 새 항목을 예전 파일에 덧붙인다 (보스별 설정은 그대로)
                    gateKeys(o);
                    Files.writeString(f, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(o), StandardCharsets.UTF_8);
                }
                if (o.has("bosses")) {
                    for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("bosses").entrySet()) {
                        JsonObject b = e.getValue().getAsJsonObject();
                        Spec s = m.getOrDefault(e.getKey(), new Spec());
                        if (b.has("theme")) s.theme = b.get("theme").getAsString();
                        if (b.has("radius")) s.radius = Math.max(8, Math.min(48, b.get("radius").getAsInt()));
                        if (b.has("height")) s.height = Math.max(8, Math.min(60, b.get("height").getAsInt()));
                        if (b.has("water")) s.water = b.get("water").getAsBoolean();
                        m.put(e.getKey(), s);
                    }
                }
            } else {
                Files.createDirectories(f.getParent());
                JsonObject o = new JsonObject();
                o.addProperty("_help", "이야기 보스전 공간(기억의 문). 보스 ID 마다 theme(stone, iron, frost, sun, deep, abyss, ember, sand, mech, void, curse, mirror), "
                        + "radius(경기장 반지름), height(천장 높이), water(물을 채움). 여기에 있는 보스를 잡는 퀘스트에는 [보스전 입장] 단추가 생깁니다.");
                JsonObject bs = new JsonObject();
                for (Map.Entry<String, Spec> e : m.entrySet()) {
                    JsonObject b = new JsonObject();
                    b.addProperty("theme", e.getValue().theme);
                    b.addProperty("radius", e.getValue().radius);
                    b.addProperty("height", e.getValue().height);
                    b.addProperty("water", e.getValue().water);
                    bs.add(e.getKey(), b);
                }
                o.add("bosses", bs);
                gateKeys(o);
                Files.writeString(f, new GsonBuilder().setPrettyPrinting().create().toJson(o), StandardCharsets.UTF_8);
            }
        } catch (Exception ex) {
            RtShop.LOGGER.warn("[rtshop] story_arenas.json 을 읽지 못했습니다: {}", ex.toString());
        }
        specs = m;
    }

    private static void gateKeys(JsonObject o) {
        o.addProperty("_help_gate", "walkIn: 단추를 누르면 그 자리 앞에 문이 열리고 걸어 들어가야 넘어간다 (false = 누르는 즉시 이동). gateSeconds: 문이 열려 있는 시간(초). "
                + "partyRange: 문을 연 사람이 들어갈 때 함께 넘어가는 파티원의 거리(칸); 그 뒤에도 보스가 깨어나기 전까지는 파티원이 문으로 걸어 들어와 합류할 수 있다. "
                + "waitSeconds: 보스 구역에서 가운데의 불씨를 우클릭하면 보스가 깨어나고, 건드리지 않아도 이 시간(초)이 지나면 깨어난다 (0 = 건드려야만).");
        if (!o.has("walkIn")) o.addProperty("walkIn", walkIn);
        if (!o.has("gateSeconds")) o.addProperty("gateSeconds", gateSeconds);
        if (!o.has("partyRange")) o.addProperty("partyRange", partyRange);
        if (!o.has("waitSeconds")) o.addProperty("waitSeconds", waitSeconds);
    }

    /** 이 보스는 기억의 문에서 싸울 수 있는가 (모드가 설치돼 있어야). */
    public static boolean supports(String boss) {
        return specs().containsKey(boss) && ForgeRegistries.ENTITY_TYPES.containsKey(ResourceLocation.tryParse(boss));
    }

    // ------------------------------------------------------------------ 진행 중인 싸움

    public static final int WAIT = 0, FIGHT = 1, WON = 2, LOST = 3;

    public static final class Session {
        public int slot;
        public String boss;
        public UUID bossId;
        public final List<UUID> members = new ArrayList<>();
        public int state = WAIT;
        public long spawnAt, endAt;
        public Spec spec;
        /** 불씨를 건드렸거나 기다리는 시간이 다 되어, 깨어나는 연출이 돌고 있다. */
        public boolean waking;
    }

    /** 세계에 열린 문: 문을 연 사람이 걸어 들어가면 보스 구역이 열리고, 보스가 깨어나기 전까지 파티원이 뒤따라 들어올 수 있다. */
    public static final class Gate {
        public UUID owner;
        public String boss;
        public boolean force;
        public ResourceKey<Level> dim;
        public double x, y, z;
        public float yaw;
        public long closeAt;
        /** 문을 연 사람이 들어가 열린 보스 구역의 칸 번호 (아직이면 -1). */
        public int slot = -1;
        Session session;
        /** 문을 연 사람이 문 밖에 한 번 서야 켜진다 (문이 발밑에 열렸을 때 곧바로 넘어가지 않게). */
        public boolean armed;
    }

    private static final Map<UUID, Gate> GATES = new HashMap<>();

    public static Map<UUID, Gate> gatesForTest() {
        return GATES;
    }

    private static final Map<Integer, Session> SESSIONS = new HashMap<>();
    private static long now;

    public static Map<Integer, Session> sessionsForTest() {
        return SESSIONS;
    }

    /** 점검용: 기억의 차원 대신 오버월드 먼 하늘을 쓴다 (가짜 플레이어는 차원을 옮길 수 없다). */
    public static boolean testOverworld;

    public static ServerLevel level(MinecraftServer server) {
        if (testOverworld) return server.overworld();
        ServerLevel l = server.getLevel(DIM);
        return l != null ? l : server.overworld();
    }

    private static boolean ownDim(MinecraftServer server) {
        return !testOverworld && server.getLevel(DIM) != null;
    }

    /** 칸 번호의 가운데 (차원이 없으면 오버월드 먼 하늘). */
    public static BlockPos center(MinecraftServer server, int slot) {
        if (ownDim(server)) return new BlockPos(slot * SPACING, FLOOR_Y, 0);
        return new BlockPos(-30000 - slot * SPACING, 290, -30000);
    }

    public static Session sessionOf(Entity e) {
        for (Session s : SESSIONS.values()) {
            if (inSlot(e, s)) return s;
        }
        return null;
    }

    public static boolean inSlot(Entity e, Session s) {
        if (e.level() != level(e.getServer())) return false;
        BlockPos c = center(e.getServer(), s.slot);
        return Math.abs(e.getX() - c.getX()) < s.spec.radius + 12 && Math.abs(e.getZ() - c.getZ()) < s.spec.radius + 12
                && e.getY() > c.getY() - 20 && e.getY() < c.getY() + s.spec.height + 20;
    }

    /** 플레이어가 지금 이 보스를 잡아야 하는 이야기 퀘스트를 진행 중인가. */
    public static boolean questWants(ServerPlayer p, String boss) {
        for (QuestDef q : QuestManager.activeQuests(p)) {
            for (QuestDef.Objective ob : q.objectives) {
                if (ob.type == QuestDef.Type.KILL && ob.target.equals(boss)) return true;
            }
        }
        return false;
    }

    /** 입장할 수 없는 까닭 (null = 들어갈 수 있음). */
    public static String cannotEnter(ServerPlayer p, String boss, boolean force) {
        if (!supports(boss)) return "rtshop.arena.unknown";
        if (!force && !questWants(p, boss)) return "rtshop.arena.no_quest";
        for (Session s : SESSIONS.values()) {
            if (s.members.contains(p.getUUID())) return "rtshop.arena.already";
        }
        if (SESSIONS.size() >= MAX_SLOTS) return "rtshop.arena.full";
        return null;
    }

    private static ServerPlayer player(MinecraftServer server, UUID id) {
        ServerPlayer p = server.getPlayerList().getPlayer(id);
        return p != null ? p : testPlayer(id);
    }

    private static boolean standable(ServerLevel level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP) && level.getFluidState(pos).isEmpty()
                && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
                && level.getBlockState(pos.above(2)).getCollisionShape(level, pos.above(2)).isEmpty();
    }

    /** 문이 열릴 자리: 바라보는 쪽 서너 칸 앞의 디딜 수 있는 바닥. 없으면 발밑. */
    private static net.minecraft.world.phys.Vec3 gateSpot(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        double yaw = Math.toRadians(p.getYRot());
        for (double dist : new double[]{3.0, 4.0, 2.5}) {
            for (int turn : new int[]{0, 30, -30, 60, -60, 90, -90, 150, -150}) {
                double a = yaw + Math.toRadians(turn);
                double x = p.getX() - Math.sin(a) * dist, z = p.getZ() + Math.cos(a) * dist;
                for (int dy = 1; dy >= -2; dy--) {
                    BlockPos c = BlockPos.containing(x, p.getY() + dy, z);
                    if (level.isLoaded(c) && standable(level, c)) return new net.minecraft.world.phys.Vec3(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
                }
            }
        }
        return p.position();
    }

    /**
     * 단추(또는 /rtshop arena <보스>)를 눌렀다: 그 사람 앞에 문을 연다. 문 안으로 걸어 들어가야 보스 구역으로 넘어간다.
     * walkIn 이 꺼져 있으면 예전처럼 곧바로 넘어간다.
     */
    public static boolean openGate(ServerPlayer p, String boss, boolean force) {
        String why = cannotEnter(p, boss, force);
        if (why != null) {
            com.rtshop.notice.Notice.send(p, Component.translatable(why).withStyle(ChatFormatting.RED), false);
            return false;
        }
        if (!walkIn) return enter(p, boss, force);
        net.minecraft.world.phys.Vec3 at = gateSpot(p);
        Gate g = new Gate();
        g.owner = p.getUUID();
        g.boss = boss;
        g.force = force;
        g.dim = p.level().dimension();
        g.x = at.x;
        g.y = at.y;
        g.z = at.z;
        g.yaw = p.getYRot();
        g.closeAt = now + gateSeconds * 20L;
        GATES.put(p.getUUID(), g);
        com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.arena.gate_open", Hunts.name(boss), gateSeconds).withStyle(ChatFormatting.GOLD), false);
        p.serverLevel().playSound(null, BlockPos.containing(at), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 0.9F, 0.8F);
        return true;
    }

    private static boolean inGate(ServerPlayer p, ServerLevel level, Gate g, double reach) {
        if (p == null || p.level() != level || !p.isAlive()) return false;
        double dx = p.getX() - g.x, dz = p.getZ() - g.z;
        return dx * dx + dz * dz <= reach * reach && Math.abs(p.getY() - g.y) < 2.0;
    }

    private static void closeGate(MinecraftServer server, Gate g, boolean tell) {
        GATES.remove(g.owner);
        ServerPlayer owner = player(server, g.owner);
        if (tell && owner != null) com.rtshop.notice.Notice.send(owner, Component.translatable("rtshop.arena.gate_closed").withStyle(ChatFormatting.GRAY), false);
        ServerLevel level = server.getLevel(g.dim);
        if (level != null) level.playSound(null, BlockPos.containing(g.x, g.y, g.z), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 0.8F);
    }

    /** 문의 모습 (누구에게나 보인다): 바닥에 재로 그린 원, 가운데의 불씨, 그 위에서 문처럼 일렁이는 공기. */
    private static void gateParticles(ServerLevel level, Gate g) {
        for (int i = 0; i < 16; i++) {
            double a = Math.PI * 2 * i / 16 + (now % 80) * 0.02;
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.WHITE_ASH, g.x + Math.cos(a) * 1.5, g.y + 0.08, g.z + Math.sin(a) * 1.5, 1, 0.03, 0.0, 0.03, 0.0);
            if (i % 4 == 0) level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, g.x + Math.cos(a) * 1.5, g.y + 0.1, g.z + Math.sin(a) * 1.5, 1, 0.02, 0.02, 0.02, 0.0);
        }
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMALL_FLAME, g.x, g.y + 0.15, g.z, 2, 0.08, 0.05, 0.08, 0.0);
        // 문틀: 문을 연 사람이 바라보던 쪽을 마주 보게 선 너비 1.6 · 높이 2.6 의 테
        double side = Math.toRadians(g.yaw), sx = Math.cos(side), sz = Math.sin(side);
        for (int i = 0; i <= 6; i++) {
            double h = 0.3 + i * 0.4;
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, g.x - sx * 0.8, g.y + h, g.z - sz * 0.8, 1, 0.0, 0.0, 0.0, 0.0);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, g.x + sx * 0.8, g.y + h, g.z + sz * 0.8, 1, 0.0, 0.0, 0.0, 0.0);
        }
        for (int i = -1; i <= 1; i++) level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, g.x + sx * 0.4 * i, g.y + 2.9, g.z + sz * 0.4 * i, 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT, g.x, g.y + 1.6, g.z, 6, sx * 0.35 + 0.05, 0.7, sz * 0.35 + 0.05, 0.02);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, g.x, g.y + 1.5, g.z, 1, Math.abs(sx) * 0.3, 0.8, Math.abs(sz) * 0.3, 0.0);
    }

    private static void tickGates(MinecraftServer server) {
        for (Gate g : new ArrayList<>(GATES.values())) {
            ServerPlayer owner = player(server, g.owner);
            ServerLevel level = testOverworld ? server.overworld() : server.getLevel(g.dim);
            Session s = g.slot >= 0 && SESSIONS.get(g.slot) == g.session ? g.session : null;   // 칸 번호는 다시 쓰이므로 같은 싸움인지 본다
            if (owner == null || level == null) {
                closeGate(server, g, false);
                continue;
            }
            if (g.slot >= 0 ? (s == null || s.state != WAIT || s.waking) : now >= g.closeAt) {
                closeGate(server, g, g.slot < 0);
                continue;
            }
            if (now % 4 == 0) gateParticles(level, g);
            if (now % 60 == 0) level.playSound(null, BlockPos.containing(g.x, g.y, g.z), SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.7F, 0.7F);
            if (g.slot < 0) {
                if (!g.armed) {
                    if (!inGate(owner, level, g, 1.5)) g.armed = true;
                } else if (inGate(owner, level, g, 1.0)) {
                    if (enter(owner, g.boss, g.force)) {
                        for (Session x : SESSIONS.values()) {
                            if (x.members.contains(g.owner)) {
                                g.slot = x.slot;
                                g.session = x;
                            }
                        }
                    }
                    if (g.slot < 0) closeGate(server, g, false);
                }
                continue;
            }
            // 보스가 깨어나기 전까지: 같은 파티원이 문으로 걸어 들어오면 합류한다
            Party.Group grp = Party.of(server, g.owner);
            if (grp == null) continue;
            for (ServerPlayer m : Party.online(server, grp)) {
                if (s.members.contains(m.getUUID()) || !inGate(m, level, g, 1.0)) continue;
                boolean busy = false;
                for (Session o : SESSIONS.values()) busy |= o.members.contains(m.getUUID());
                if (busy) continue;
                s.members.add(m.getUUID());
                place(server, s, m, s.members.size() - 1, Math.max(6, s.members.size()));
            }
        }
    }

    /** 그 사람을 보스 구역의 가장자리에 세운다 (들어오기 전 자리를 기억해 둔다). */
    private static void place(MinecraftServer server, Session s, ServerPlayer m, int i, int n) {
        ServerLevel level = level(server);
        BlockPos c = center(server, s.slot);
        remember(m);
        double a = Math.PI * 2 * i / Math.max(1, n) + Math.PI / 2;
        double r = s.spec.radius - 4;
        double x = c.getX() + 0.5 + Math.cos(a) * r, z = c.getZ() + 0.5 + Math.sin(a) * r;
        float yaw = (float) Math.toDegrees(Math.atan2(c.getZ() + 0.5 - z, c.getX() + 0.5 - x)) - 90F;
        if (m.isPassenger()) m.stopRiding();
        if (m instanceof net.minecraftforge.common.util.FakePlayer) m.setPos(x, c.getY() + 1, z);
        else m.teleportTo(level, x, c.getY() + 1, z, yaw, 0F);
        com.rtshop.notice.Notice.send(m, Component.translatable("rtshop.arena.inside", Hunts.name(s.boss)).withStyle(ChatFormatting.GOLD), true);
    }

    /** 보스 구역을 열고 넘어간다: 그 사람과 곁(partyRange 칸)에 있는 같은 파티원을 함께 데려간다. 보스는 아직 잠들어 있다. */
    public static boolean enter(ServerPlayer p, String boss, boolean force) {
        String why = cannotEnter(p, boss, force);
        if (why != null) {
            com.rtshop.notice.Notice.send(p, Component.translatable(why).withStyle(ChatFormatting.RED), false);
            return false;
        }
        MinecraftServer server = p.server;
        int slot = 0;
        while (SESSIONS.containsKey(slot)) slot++;
        Session s = new Session();
        s.slot = slot;
        s.boss = boss;
        s.spec = specs().get(boss);
        s.members.add(p.getUUID());
        Party.Group g = Party.of(server, p.getUUID());
        if (g != null) {
            for (ServerPlayer m : Party.online(server, g)) {
                if (m == p || m.level() != p.level() || m.distanceTo(p) > partyRange) continue;
                boolean busy = false;
                for (Session o : SESSIONS.values()) busy |= o.members.contains(m.getUUID());
                if (!busy) s.members.add(m.getUUID());
            }
        }
        ServerLevel level = level(server);
        BlockPos c = center(server, slot);
        build(level, c, s.spec);
        // 가운데의 불씨: 건드리면 보스가 깨어난다
        level.setBlock(c.above(1), Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState(), 2);
        level.setBlock(c.above(2), Blocks.CAMPFIRE.defaultBlockState(), 2);
        SESSIONS.put(slot, s);
        int i = 0;
        for (UUID id : new ArrayList<>(s.members)) {
            ServerPlayer m = player(server, id);
            if (m == null) continue;
            place(server, s, m, i, s.members.size());
            i++;
        }
        level.playSound(null, c, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 0.6F, 0.8F);
        s.spawnAt = waitSeconds > 0 ? now + Math.max(waitSeconds * 20L, WAKE_TICKS + 20L) : Long.MAX_VALUE;
        return true;
    }

    /** 불씨를 건드렸다(또는 기다리는 시간이 다 됐다): 깨어나는 연출을 시작한다. 연출이 끝나면 보스가 나타난다. */
    public static boolean wake(MinecraftServer server, Session s) {
        if (s == null || s.state != WAIT || s.waking) return false;
        s.waking = true;
        s.spawnAt = now + WAKE_TICKS;
        BlockPos c = center(server, s.slot);
        level(server).playSound(null, c, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.HOSTILE, 1.0F, 0.6F);
        for (ServerPlayer p : present(server, s)) {
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.arena.entered", Hunts.name(s.boss)).withStyle(ChatFormatting.GOLD), true);
        }
        return true;
    }

    /** 이 사람이 들어와 있는 보스 구역의 불씨를 건드린다 (/rtshop arena start 와 불씨 우클릭). */
    public static boolean wake(ServerPlayer p) {
        for (Session s : SESSIONS.values()) {
            if (s.members.contains(p.getUUID()) && inSlot(p, s)) return wake(p.server, s);
        }
        return false;
    }

    private static boolean ember(MinecraftServer server, Session s, BlockPos at) {
        BlockPos c = center(server, s.slot);
        return at.getX() == c.getX() && at.getZ() == c.getZ() && at.getY() >= c.getY() + 1 && at.getY() <= c.getY() + 2;
    }

    @SubscribeEvent
    public static void onUseBlock(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        if (SESSIONS.isEmpty() || event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer p)) return;
        Session s = sessionOf(p);
        if (s == null || s.state != WAIT || !s.members.contains(p.getUUID()) || !ember(p.server, s, event.getPos())) return;
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        wake(p.server, s);
    }

    /** 불씨와 받침은 부술 수 없다 (부수려 해도 건드린 것으로 친다). */
    @SubscribeEvent
    public static void onBreakBlock(net.minecraftforge.event.level.BlockEvent.BreakEvent event) {
        if (SESSIONS.isEmpty() || !(event.getPlayer() instanceof ServerPlayer p)) return;
        Session s = sessionOf(p);
        if (s == null || s.state != WAIT || !ember(p.server, s, event.getPos())) return;
        event.setCanceled(true);
        wake(p.server, s);
    }

    /** 깨어나는 연출: 불씨의 불꽃이 원을 그리며 가운데로 모이고, 소리가 점점 높아진다. */
    private static void wakeFx(ServerLevel level, BlockPos c, Session s) {
        long t = WAKE_TICKS - (s.spawnAt - now);
        double prog = Math.max(0, Math.min(1, t / (double) WAKE_TICKS));
        double cx = c.getX() + 0.5, cy = c.getY() + 1.0, cz = c.getZ() + 0.5;
        if (t % 2 == 0) {
            double r = 1.5 + (s.spec.radius * 0.8 - 1.5) * (1 - prog);
            int n = 28;
            for (int i = 0; i < n; i++) {
                double a = Math.PI * 2 * i / n + t * 0.09;
                level.sendParticles(i % 2 == 0 ? net.minecraft.core.particles.ParticleTypes.FLAME : net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                        cx + Math.cos(a) * r, cy + 0.15 + prog * 0.6, cz + Math.sin(a) * r, 1, 0.0, 0.02, 0.0, 0.0);
            }
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA, cx, cy + 1.6, cz, 1 + (int) (prog * 3), 0.2, 0.2, 0.2, 0.0);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, cx, cy + 1.8 + prog * 2.5, cz, 2, 0.25, 0.6, 0.25, 0.01);
        }
        if (t > 0 && t % 20 == 0) level.playSound(null, c, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.HOSTILE, 1.0F, 0.6F + (float) prog * 0.9F);
    }

    /** 불씨가 타오르는 순간: 받침을 치우고 번쩍임과 함께 보스가 나타난다. */
    private static void burst(ServerLevel level, BlockPos c, Session s) {
        BlockState fill = s.spec.water ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
        level.setBlock(c.above(2), fill, 2);
        level.setBlock(c.above(1), fill, 2);
        double cx = c.getX() + 0.5, cy = c.getY() + 1.5, cz = c.getZ() + 0.5;
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH, cx, cy + 0.5, cz, 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER, cx, cy, cz, 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, cx, cy, cz, 120, 0.6, 0.8, 0.6, 0.25);
        level.playSound(null, c, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 0.8F, 0.7F);
    }

    private static final List<ServerPlayer> TEST_PLAYERS = new ArrayList<>();

    public static void addTestPlayer(ServerPlayer p) {
        if (!TEST_PLAYERS.contains(p)) TEST_PLAYERS.add(p);
    }

    private static ServerPlayer testPlayer(UUID id) {
        for (ServerPlayer p : TEST_PLAYERS) if (p.getUUID().equals(id)) return p;
        return null;
    }

    private static void remember(ServerPlayer p) {
        CompoundTag t = new CompoundTag();
        t.putString("dim", p.level().dimension().location().toString());
        t.putDouble("x", p.getX());
        t.putDouble("y", p.getY());
        t.putDouble("z", p.getZ());
        t.putFloat("yaw", p.getYRot());
        keep(p).put(NBT_FROM, t);
    }

    private static CompoundTag keep(Player p) {
        CompoundTag root = p.getPersistentData();
        CompoundTag k = root.getCompound(Player.PERSISTED_NBT_TAG);
        root.put(Player.PERSISTED_NBT_TAG, k);
        return k;
    }

    /** 들어오기 전 자리로 돌려보낸다. */
    public static void sendHome(ServerPlayer p) {
        CompoundTag from = keep(p).getCompound(NBT_FROM);
        keep(p).remove(NBT_FROM);
        ServerLevel to = p.server.overworld();
        double x, y, z;
        float yaw = p.getYRot();
        ServerLevel fl = from.contains("dim") ? p.server.getLevel(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(from.getString("dim")))) : null;
        if (fl != null && (fl.dimension() != DIM || testOverworld)) {
            to = fl;
            x = from.getDouble("x");
            y = from.getDouble("y");
            z = from.getDouble("z");
            yaw = from.getFloat("yaw");
        } else {
            BlockPos sp = to.getSharedSpawnPos();
            x = sp.getX() + 0.5;
            y = sp.getY();
            z = sp.getZ() + 0.5;
        }
        p.clearFire();
        p.fallDistance = 0;
        if (p.isPassenger()) p.stopRiding();
        if (p instanceof net.minecraftforge.common.util.FakePlayer) {
            p.setPos(x, y, z);
            return;
        }
        BlockPos at = BlockPos.containing(x, y, z);
        to.getChunk(at.getX() >> 4, at.getZ() >> 4);
        for (int i = 0; i < 64 && (!to.getBlockState(at).getCollisionShape(to, at).isEmpty() || !to.getBlockState(at.above()).getCollisionShape(to, at.above()).isEmpty()); i++) {
            at = at.above();
            y = at.getY();
        }
        p.teleportTo(to, x, y, z, yaw, 0F);
    }

    // ------------------------------------------------------------------ 경기장 짓기

    private static Block b(String id, Block fb) {
        Block x = ForgeRegistries.BLOCKS.getValue(ResourceLocation.tryParse(id));
        return x == null || x == Blocks.AIR ? fb : x;
    }

    /** 테마별 블록: 바닥, 바닥 무늬, 담, 빛. */
    private static Block[] palette(String theme) {
        return switch (theme) {
            case "iron" -> new Block[]{Blocks.DEEPSLATE_TILES, Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE_BRICKS, Blocks.LANTERN, Blocks.IRON_BARS};
            case "frost" -> new Block[]{Blocks.PACKED_ICE, Blocks.BLUE_ICE, Blocks.SNOW_BLOCK, Blocks.SEA_LANTERN, Blocks.ICE};
            case "sun" -> new Block[]{Blocks.TERRACOTTA, Blocks.ORANGE_TERRACOTTA, Blocks.SMOOTH_SANDSTONE, Blocks.GLOWSTONE, Blocks.HAY_BLOCK};
            case "deep" -> new Block[]{Blocks.SCULK, Blocks.DEEPSLATE_TILES, Blocks.REINFORCED_DEEPSLATE, Blocks.SOUL_LANTERN, Blocks.SCULK_VEIN};
            case "abyss" -> new Block[]{Blocks.PRISMARINE_BRICKS, Blocks.DARK_PRISMARINE, Blocks.PRISMARINE, Blocks.SEA_LANTERN, Blocks.PRISMARINE};
            case "ember" -> new Block[]{Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.MAGMA_BLOCK, Blocks.BLACKSTONE, Blocks.SHROOMLIGHT, Blocks.GILDED_BLACKSTONE};
            case "sand" -> new Block[]{Blocks.CUT_SANDSTONE, Blocks.CHISELED_SANDSTONE, Blocks.SANDSTONE, Blocks.GLOWSTONE, Blocks.SMOOTH_SANDSTONE};
            case "mech" -> new Block[]{Blocks.WAXED_CUT_COPPER, Blocks.WAXED_OXIDIZED_CUT_COPPER, Blocks.POLISHED_DEEPSLATE, Blocks.REDSTONE_LAMP, Blocks.IRON_BLOCK};
            case "void" -> new Block[]{Blocks.END_STONE_BRICKS, Blocks.PURPUR_BLOCK, Blocks.OBSIDIAN, Blocks.END_ROD, Blocks.PURPUR_PILLAR};
            case "curse" -> new Block[]{Blocks.SOUL_SOIL, Blocks.POLISHED_BLACKSTONE, Blocks.BLACKSTONE, Blocks.SOUL_LANTERN, Blocks.CRYING_OBSIDIAN};
            case "mirror" -> new Block[]{Blocks.DARK_OAK_PLANKS, Blocks.AMETHYST_BLOCK, Blocks.DARK_OAK_LOG, Blocks.LANTERN, Blocks.TINTED_GLASS};
            default -> new Block[]{Blocks.STONE_BRICKS, Blocks.CHISELED_STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS, Blocks.LANTERN, Blocks.POLISHED_ANDESITE};
        };
    }

    /** 둥근 경기장: 바닥(무늬), 낮은 담 위로 보이지 않는 벽과 천장, 빛. 물 테마는 안을 물로 채운다. */
    public static int build(ServerLevel level, BlockPos c, Spec spec) {
        Block[] pal = palette(spec.theme);
        int R = spec.radius, H = spec.height, n = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -R - 3; dx <= R + 3; dx++) {
            for (int dz = -R - 3; dz <= R + 3; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                level.getChunk((c.getX() + dx) >> 4, (c.getZ() + dz) >> 4);
                for (int y = -2; y <= H + 2; y++) {
                    pos.set(c.getX() + dx, c.getY() + y, c.getZ() + dz);
                    BlockState st;
                    if (d > R + 2.5) {
                        st = Blocks.AIR.defaultBlockState();
                    } else if (y == -2) {
                        st = Blocks.BEDROCK.defaultBlockState();
                    } else if (y == -1) {
                        st = pal[2].defaultBlockState();
                    } else if (y == 0) {
                        boolean ring = Math.abs(d - R * 0.45) < 0.6 || Math.abs(d - R * 0.8) < 0.6 || d < 2.5;
                        boolean lamp = d > 4 && Math.abs(dx) % 8 == 0 && Math.abs(dz) % 8 == 0;
                        st = d > R + 0.5 ? pal[2].defaultBlockState() : lamp ? pal[3].defaultBlockState() : ring ? pal[1].defaultBlockState() : pal[0].defaultBlockState();
                    } else if (d > R + 0.5) {
                        st = y <= 3 ? pal[2].defaultBlockState() : y == 4 && (dx * 31 + dz * 17) % 5 == 0 ? pal[3].defaultBlockState() : Blocks.BARRIER.defaultBlockState();
                    } else if (y == H + 1) {
                        st = Blocks.BARRIER.defaultBlockState();
                    } else if (y > H + 1) {
                        st = Blocks.AIR.defaultBlockState();
                    } else {
                        st = spec.water && y <= H - 2 ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
                    }
                    if (level.getBlockState(pos) != st) {
                        level.removeBlockEntity(pos);
                        level.setBlock(pos, st, 2);
                        n++;
                    }
                }
            }
        }
        // 기둥 여섯 개 (테마 장식)
        for (int k = 0; k < 6; k++) {
            double a = Math.PI * 2 * k / 6;
            int px = c.getX() + (int) Math.round(Math.cos(a) * (R - 3)), pz = c.getZ() + (int) Math.round(Math.sin(a) * (R - 3));
            for (int y = 1; y <= 4; y++) {
                pos.set(px, c.getY() + y, pz);
                level.setBlock(pos, (y == 4 ? pal[3] : pal[4]).defaultBlockState(), 2);
            }
        }
        // 남은 몹·물건 치우기
        AABB box = new AABB(c).inflate(R + 4, H + 4, R + 4);
        for (Entity e : level.getEntities((Entity) null, box, e -> !(e instanceof Player))) e.discard();
        return n;
    }

    // ------------------------------------------------------------------ 진행

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        now++;
        if (SESSIONS.isEmpty() && GATES.isEmpty()) return;
        tick(event.getServer());
    }

    public static void tick(MinecraftServer server) {
        ServerLevel level = level(server);
        if (!GATES.isEmpty()) tickGates(server);
        for (Session s : new ArrayList<>(SESSIONS.values())) {
            BlockPos c = center(server, s.slot);
            List<ServerPlayer> inside = present(server, s);
            switch (s.state) {
                case WAIT -> {
                    if (inside.isEmpty()) {
                        finish(server, s);
                    } else if (!s.waking) {
                        if (now >= s.spawnAt - WAKE_TICKS) {
                            wake(server, s);
                        } else if (now % 20 == 0) {
                            long left = (s.spawnAt - WAKE_TICKS - now) / 20;
                            for (ServerPlayer p : inside) {
                                p.displayClientMessage(waitSeconds > 0 ? Component.translatable("rtshop.arena.wait", left) : Component.translatable("rtshop.arena.wait_touch"), true);
                            }
                        }
                    } else if (now >= s.spawnAt) {
                        burst(level, c, s);
                        spawnBoss(level, c, s);
                    } else {
                        wakeFx(level, c, s);
                    }
                }
                case FIGHT -> {
                    Entity boss = s.bossId == null ? null : level.getEntity(s.bossId);
                    if (boss == null || !boss.isAlive()) {
                        s.state = WON;
                        s.endAt = now + 200;
                        for (ServerPlayer p : inside) com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.arena.won").withStyle(ChatFormatting.GOLD), false);
                    } else if (inside.isEmpty()) {
                        s.state = LOST;
                        s.endAt = now + 20;
                    } else if (!inSlot(boss, s)) {
                        boss.teleportTo(c.getX() + 0.5, c.getY() + 2, c.getZ() + 0.5); // 경기장 밖으로 날아가면 되돌린다
                    }
                }
                default -> {
                    if (now >= s.endAt || inside.isEmpty()) finish(server, s);
                }
            }
        }
    }

    private static List<ServerPlayer> present(MinecraftServer server, Session s) {
        List<ServerPlayer> out = new ArrayList<>();
        for (UUID id : s.members) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p == null) p = testPlayer(id);
            if (p != null && p.isAlive() && inSlot(p, s)) out.add(p);
        }
        return out;
    }

    private static void spawnBoss(ServerLevel level, BlockPos c, Session s) {
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.tryParse(s.boss));
        Entity e = type == null ? null : type.create(level);
        if (e == null) {
            s.state = LOST;
            s.endAt = now;
            return;
        }
        e.moveTo(c.getX() + 0.5, c.getY() + 1, c.getZ() + 0.5, 0F, 0F);
        if (e instanceof Mob m) {
            m.finalizeSpawn(level, level.getCurrentDifficultyAt(c), MobSpawnType.MOB_SUMMONED, null, null);
            m.setPersistenceRequired();
        }
        e.addTag(BOSS_TAG);
        level.addFreshEntity(e);
        s.bossId = e.getUUID();
        s.state = FIGHT;
        level.playSound(null, c, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.5F, 0.7F);
    }

    /** 끝: 떨어진 전리품을 남은 사람에게 넘기고, 모두 돌려보내고, 공간을 비운다. */
    public static void finish(MinecraftServer server, Session s) {
        ServerLevel level = level(server);
        BlockPos c = center(server, s.slot);
        List<ServerPlayer> inside = present(server, s);
        AABB box = new AABB(c).inflate(s.spec.radius + 4, s.spec.height + 4, s.spec.radius + 4);
        if (!inside.isEmpty()) {
            int k = 0;
            for (ItemEntity it : level.getEntitiesOfClass(ItemEntity.class, box)) {
                ServerPlayer to = inside.get(k++ % inside.size());
                if (!to.getInventory().add(it.getItem().copy())) to.drop(it.getItem().copy(), false);
                it.discard();
            }
        }
        for (UUID id : s.members) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p == null) p = testPlayer(id);
            if (p != null && inSlot(p, s)) sendHome(p);
        }
        for (Entity e : level.getEntities((Entity) null, box, e -> !(e instanceof Player))) e.discard();
        SESSIONS.remove(s.slot);
    }

    /** 경기장 안에서는 몹이 저절로 생기지 않는다. */
    @SubscribeEvent
    public static void onSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (event.getLevel().getLevel().dimension() == DIM && event.getSpawnType() == MobSpawnType.NATURAL) event.setSpawnCancelled(true);
    }

    /** 경기장에 남은 채로 접속을 끊었다가 들어오면 (서버가 다시 켜졌거나 싸움이 끝났으면) 돌려보낸다. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.level().dimension() != DIM) return;
        for (Session s : SESSIONS.values()) if (s.members.contains(p.getUUID()) && inSlot(p, s)) return;
        sendHome(p);
    }

    /** 경기장에서 쓰러지면 다시 태어난 뒤 그대로 (경기장 차원에 다시 태어나지 않도록) 돌아간 자리. */
    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && p.level().dimension() == DIM) sendHome(p);
    }

    /** 점검용: 모든 싸움을 끝낸다. */
    public static void clearForTest(MinecraftServer server) {
        GATES.clear();
        for (Session s : new ArrayList<>(SESSIONS.values())) finish(server, s);
        TEST_PLAYERS.clear();
    }

    public static LivingEntity bossForTest(MinecraftServer server, int slot) {
        Session s = SESSIONS.get(slot);
        Entity e = s == null || s.bossId == null ? null : level(server).getEntity(s.bossId);
        return e instanceof LivingEntity l ? l : null;
    }

    public static void forceTick(MinecraftServer server, int ticks) {
        for (int i = 0; i < ticks; i++) {
            now++;
            tick(server);
        }
    }
}

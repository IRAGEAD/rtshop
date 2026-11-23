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
        try {
            Path f = file();
            if (Files.exists(f)) {
                JsonObject o = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
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
                Files.writeString(f, new GsonBuilder().setPrettyPrinting().create().toJson(o), StandardCharsets.UTF_8);
            }
        } catch (Exception ex) {
            RtShop.LOGGER.warn("[rtshop] story_arenas.json 을 읽지 못했습니다: {}", ex.toString());
        }
        specs = m;
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

    /** 기억의 문을 연다: 그 사람과 곁(64칸)에 있는 같은 파티원을 함께 데려간다. */
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
                if (m == p || m.level() != p.level() || m.distanceTo(p) > 64) continue;
                boolean busy = false;
                for (Session o : SESSIONS.values()) busy |= o.members.contains(m.getUUID());
                if (!busy) s.members.add(m.getUUID());
            }
        }
        ServerLevel level = level(server);
        BlockPos c = center(server, slot);
        build(level, c, s.spec);
        SESSIONS.put(slot, s);
        int i = 0;
        for (UUID id : s.members) {
            ServerPlayer m = server.getPlayerList().getPlayer(id);
            if (m == null) m = testPlayer(id);
            if (m == null) continue;
            remember(m);
            double a = Math.PI * 2 * i / Math.max(1, s.members.size()) + Math.PI / 2;
            double r = s.spec.radius - 4;
            double x = c.getX() + 0.5 + Math.cos(a) * r, z = c.getZ() + 0.5 + Math.sin(a) * r;
            float yaw = (float) Math.toDegrees(Math.atan2(c.getZ() + 0.5 - z, c.getX() + 0.5 - x)) - 90F;
            if (m.isPassenger()) m.stopRiding();
            if (m instanceof net.minecraftforge.common.util.FakePlayer) m.setPos(x, c.getY() + 1, z);
            else m.teleportTo(level, x, c.getY() + 1, z, yaw, 0F);
            com.rtshop.notice.Notice.send(m, Component.translatable("rtshop.arena.entered", Hunts.name(boss)).withStyle(ChatFormatting.GOLD), true);
            i++;
        }
        level.playSound(null, c, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 0.6F, 0.8F);
        s.spawnAt = now + 70;
        return true;
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
        if (SESSIONS.isEmpty()) return;
        tick(event.getServer());
    }

    public static void tick(MinecraftServer server) {
        ServerLevel level = level(server);
        for (Session s : new ArrayList<>(SESSIONS.values())) {
            BlockPos c = center(server, s.slot);
            List<ServerPlayer> inside = present(server, s);
            switch (s.state) {
                case WAIT -> {
                    if (inside.isEmpty()) {
                        finish(server, s);
                    } else if (now >= s.spawnAt) {
                        spawnBoss(level, c, s);
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

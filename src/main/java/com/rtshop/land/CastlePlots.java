package com.rtshop.land;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import com.rtshop.land.LandData.Claim;
import com.rtshop.land.LandData.Land;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * 성 안 토지 (아모노스 맵): 성벽 안쪽 바깥 둘레를 16×16 토지로 나눈 목록(rtshop_defaults/castle_plots.json)을 구획으로 등록하고,
 * 처음 팔릴 때 그 자리를 평지로 만든다.
 *  - 길·물·성벽 칸은 처음부터 토지에서 빠져 있어서 건드리지 않는다.
 *  - 건물이 여러 칸에 걸쳐 있으면(같은 group) 그 칸들을 함께 평지로 만든다 (그 칸들은 아직 아무도 사지 않은 상태).
 *  - 다른 사람의 토지에 들어간 칸은 절대 건드리지 않는다. 바꾸기 전 모습은 world/rtshop/castle_backup 에 저장한다.
 */
public final class CastlePlots {
    private CastlePlots() {
    }

    public static final int VERSION = 1;
    /** 지면 위로 비우는 높이. */
    public static final int CLEAR_UP = 64;
    /** 지면 아래로 빈 곳을 흙으로 메우는 깊이. */
    public static final int FILL_DOWN = 8;

    public static final class Tile {
        public int no, x0, z0, ground, group;
        public int[] clearTo;
        public BitSet mask = new BitSet(256);

        public boolean has(int dx, int dz) {
            return dx >= 0 && dx < 16 && dz >= 0 && dz < 16 && mask.get(dz * 16 + dx);
        }

        public int area() {
            return mask.cardinality();
        }
    }

    public static final class Def {
        public String dim = "minecraft:overworld";
        public int[] spawn = new int[3];
        public final List<Object[]> signature = new ArrayList<>();
        public final List<Tile> tiles = new ArrayList<>();
        public int mapX0, mapZ0, mapW, mapH, mapSize;
        public String mapTexture = "";
    }

    private static Def def;

    /** 모드에 들어 있는 성 안 토지 목록 (서버·클라이언트 공통). */
    public static synchronized Def def() {
        if (def == null) {
            def = new Def();
            try (InputStream in = CastlePlots.class.getResourceAsStream("/rtshop_defaults/castle_plots.json")) {
                if (in != null) {
                    JsonObject o = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                    def.dim = o.get("dimension").getAsString();
                    JsonArray sp = o.getAsJsonArray("spawn");
                    def.spawn = new int[]{sp.get(0).getAsInt(), sp.get(1).getAsInt(), sp.get(2).getAsInt()};
                    for (JsonElement e : o.getAsJsonArray("signature")) {
                        JsonArray a = e.getAsJsonArray();
                        def.signature.add(new Object[]{new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt()), a.get(3).getAsString()});
                    }
                    JsonObject m = o.getAsJsonObject("map");
                    def.mapX0 = m.get("x0").getAsInt();
                    def.mapZ0 = m.get("z0").getAsInt();
                    def.mapW = m.get("w").getAsInt();
                    def.mapH = m.get("h").getAsInt();
                    def.mapSize = m.get("size").getAsInt();
                    def.mapTexture = m.get("texture").getAsString();
                    for (JsonElement e : o.getAsJsonArray("plots")) {
                        JsonObject t = e.getAsJsonObject();
                        Tile tile = new Tile();
                        tile.no = t.get("no").getAsInt();
                        tile.x0 = t.get("x0").getAsInt();
                        tile.z0 = t.get("z0").getAsInt();
                        tile.ground = t.get("ground").getAsInt();
                        tile.group = t.get("group").getAsInt();
                        if (t.has("clearTo") && t.get("clearTo").isJsonArray()) {
                            JsonArray c = t.getAsJsonArray("clearTo");
                            tile.clearTo = new int[]{c.get(0).getAsInt(), c.get(1).getAsInt(), c.get(2).getAsInt(), c.get(3).getAsInt()};
                        }
                        String mask = t.get("mask").getAsString();
                        for (int i = 0; i < Math.min(256, mask.length()); i++) {
                            if (mask.charAt(i) == '1') {
                                tile.mask.set(i);
                            }
                        }
                        def.tiles.add(tile);
                    }
                }
            } catch (Exception e) {
                RtShop.LOGGER.error("[rtshop] castle_plots.json 을 읽지 못했습니다", e);
            }
        }
        return def;
    }

    public static Tile tile(int no) {
        for (Tile t : def().tiles) {
            if (t.no == no) return t;
        }
        return null;
    }

    public static String name(int no) {
        return "성 안 토지 " + no;
    }

    public static long price(Tile t) {
        long p = LandConfig.get().castlePlotPrice * t.area() / 256;
        return Math.max(0, Math.round(p / 500.0) * 500);
    }

    // ------------------------------------------------------------------ 이 세계가 아모노스 맵인가

    private static ServerLevel level(MinecraftServer server) {
        for (ServerLevel l : server.getAllLevels()) {
            if (LandManager.dim(l).equals(def().dim)) return l;
        }
        return server.overworld();
    }

    /** 스폰이 아모노스 스폰 근처인가 (먼 곳의 청크를 쓸데없이 만들지 않으려고 먼저 본다). */
    public static boolean spawnNear(MinecraftServer server) {
        BlockPos s = server.overworld().getSharedSpawnPos();
        int[] d = def().spawn;
        return Math.abs(s.getX() - d[0]) <= 1500 && Math.abs(s.getZ() - d[2]) <= 1500;
    }

    /** 표지 블록(성 안 건물 몇 곳)이 맞는 개수. */
    public static int signatureMatches(MinecraftServer server) {
        ServerLevel level = level(server);
        int n = 0;
        for (Object[] s : def().signature) {
            BlockPos pos = (BlockPos) s[0];
            BlockState st = level.getBlockState(pos);
            ResourceLocation id = ForgeRegistries.BLOCKS.getKey(st.getBlock());
            if (id != null && id.toString().equals(s[1])) n++;
        }
        return n;
    }

    public static boolean worldMatches(MinecraftServer server) {
        return spawnNear(server) && !def().signature.isEmpty() && signatureMatches(server) * 10 >= def().signature.size() * 6;
    }

    // ------------------------------------------------------------------ 등록

    public static Land landOf(LandData data, int no) {
        for (Land l : data.lands()) {
            if (l.castleNo == no) return l;
        }
        return null;
    }

    /** 서버가 켜질 때: 아직 정하지 않았고 이 세계가 아모노스 맵이면 등록한다. */
    public static void autoInstall(MinecraftServer server) {
        try {
            LandData data = LandData.get(server);
            if (data.castleInstalled != 0 || !LandConfig.get().castlePlotsAuto || def().tiles.isEmpty()) return;
            if (!spawnNear(server)) {
                return; // 다른 맵: 나중에 OP 가 /rtshop plot castle install 로 직접 넣을 수 있음
            }
            if (!worldMatches(server)) {
                data.castleInstalled = -1;
                data.setDirty();
                RtShop.LOGGER.info("[rtshop] 스폰은 아모노스 근처지만 성 안 건물이 맞지 않아 성 안 토지를 등록하지 않습니다 ({}/{})",
                        signatureMatches(server), def().signature.size());
                return;
            }
            int[] r = install(server);
            RtShop.LOGGER.info("[rtshop] 아모노스 맵: 성 안 토지 {}칸을 등록했습니다 (건너뜀 {}, 다른 토지와 겹쳐 빠진 칸 {})", r[0], r[1], r[2]);
        } catch (Throwable t) {
            RtShop.LOGGER.warn("[rtshop] 성 안 토지 자동 등록 실패", t);
        }
    }

    /** 성 안 토지를 판매 중 구획으로 등록 (이미 있는 번호는 건너뜀). {만든 수, 건너뛴 수, 겹쳐서 빠진 칸 수}. */
    public static int[] install(MinecraftServer server) {
        LandData data = LandData.get(server);
        ServerLevel level = level(server);
        String dim = LandManager.dim(level);
        int made = 0, skipped = 0, dropped = 0;
        int minY = level.getMinBuildHeight(), maxY = level.getMaxBuildHeight() - 1;
        for (Tile t : def().tiles) {
            if (landOf(data, t.no) != null) {
                skipped++;
                continue;
            }
            BitSet keep = new BitSet(256);
            for (int i = t.mask.nextSetBit(0); i >= 0; i = t.mask.nextSetBit(i + 1)) {
                int x = t.x0 + i % 16, z = t.z0 + i / 16;
                if (!data.claimsNear(dim, x, minY, z, x, maxY, z, 0).isEmpty()) {
                    dropped++;
                    continue;
                }
                keep.set(i);
            }
            if (keep.isEmpty()) {
                skipped++;
                continue;
            }
            Claim c = new Claim();
            c.dim = dim;
            c.type = "plot";
            c.x0 = t.x0;
            c.z0 = t.z0;
            c.x1 = t.x0 + 15;
            c.z1 = t.z0 + 15;
            c.mask = keep;
            c.y0 = Math.max(minY, t.ground - Math.max(LandConfig.get().plotDepthBelow, FILL_DOWN));
            c.y1 = maxY;
            Land land = data.createPlot(name(t.no), price(t));
            land.castleNo = t.no;
            land.castleGroup = t.group;
            land.castleGround = t.ground;
            land.defaultPerms = LandData.USE; // 팔리기 전에는 누구나 문·버튼은 쓸 수 있다 (부수기·놓기·보관함은 안 됨)
            data.addClaim(land, c);
            made++;
        }
        data.castleInstalled = VERSION;
        data.touch();
        return new int[]{made, skipped, dropped};
    }

    /** 판매 중인(주인 없는) 성 안 토지를 모두 지운다. {지운 수, 주인이 있어 남긴 수}. */
    public static int[] uninstall(MinecraftServer server) {
        LandData data = LandData.get(server);
        List<Land> rm = new ArrayList<>();
        int kept = 0;
        for (Land l : data.lands()) {
            if (l.castle()) {
                if (l.forSale()) rm.add(l);
                else kept++;
            }
        }
        rm.forEach(data::removeLand);
        data.castleInstalled = -1;
        data.touch();
        return new int[]{rm.size(), kept};
    }

    // ------------------------------------------------------------------ 평지로 만들기

    /** 산 성 안 토지를 평지로 만들 때 함께 바뀌는 토지들 (같은 건물 묶음 중 아직 바뀌지 않은 것). 산 토지 자신이 먼저. */
    public static List<Land> clearSet(LandData data, Land land) {
        List<Land> out = new ArrayList<>();
        if (!land.castle() || land.cleared) return out;
        out.add(land);
        if (land.castleGroup >= 0) {
            for (Land l : data.lands()) {
                if (l != land && l.castle() && l.castleGroup == land.castleGroup && !l.cleared && (l.forSale() || l.owner.equals(land.owner))) {
                    out.add(l);
                }
            }
        }
        return out;
    }

    /** 토지를 산 직후 부른다: 건물을 철거하고 평지(풀밭)로 만든다. 바꾼 블록 수. */
    public static int prepare(MinecraftServer server, Land land) {
        LandData data = LandData.get(server);
        ServerLevel level = level(server);
        int changed = 0;
        for (Land l : clearSet(data, land)) {
            try {
                backup(server, level, l);
            } catch (Exception e) {
                RtShop.LOGGER.warn("[rtshop] 성 안 토지 {} 백업 실패: {}", l.name, e.toString());
            }
            changed += flatten(level, data, l);
            l.cleared = true;
        }
        data.touch();
        return changed;
    }

    private static boolean ownsColumn(LandData data, String dim, Land l, int x, int y, int z) {
        Claim c = data.claimAt(dim, x, y, z);
        return c != null && c.landId == l.id;
    }

    /** 이 토지의 칸(기둥)만: 지면 위는 비우고, 지면은 풀, 바로 아래는 흙, 그 아래 빈 곳은 흙으로 메운다. */
    static int flatten(ServerLevel level, LandData data, Land l) {
        Claim c = PlotManager.plotClaim(l);
        if (c == null) return 0;
        String dim = LandManager.dim(level);
        int g = l.castleGround;
        int top = Math.min(level.getMaxBuildHeight() - 1, g + CLEAR_UP);
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        int n = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int z = c.z0; z <= c.z1; z++) {
            for (int x = c.x0; x <= c.x1; x++) {
                if (!c.hasColumn(x, z) || !ownsColumn(data, dim, l, x, g, z)) continue;
                level.getChunk(x >> 4, z >> 4);
                for (int y = top; y > g; y--) {
                    pos.set(x, y, z);
                    if (!level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, air, flags);
                        n++;
                    }
                }
                pos.set(x, g, z);
                level.setBlock(pos, grass, flags);
                for (int y = g - 1; y >= g - 3; y--) {
                    pos.set(x, y, z);
                    level.setBlock(pos, dirt, flags);
                }
                for (int y = g - 4; y >= g - FILL_DOWN; y--) {
                    pos.set(x, y, z);
                    BlockState st = level.getBlockState(pos);
                    if (st.isAir() || !st.getFluidState().isEmpty() || st.canBeReplaced()) {
                        level.setBlock(pos, dirt, flags);
                    }
                }
                n += 4;
            }
        }
        // 액자·그림·갑옷 거치대·떨어진 아이템 (사람·동물·NPC 는 그대로)
        AABB box = new AABB(c.x0, g - FILL_DOWN, c.z0, c.x1 + 1, top + 1, c.z1 + 1);
        for (Entity e : level.getEntitiesOfClass(Entity.class, box,
                e -> e instanceof HangingEntity || e instanceof ArmorStand || e instanceof ItemEntity)) {
            if (c.hasColumn(e.getBlockX(), e.getBlockZ())) {
                e.discard();
            }
        }
        return n;
    }

    // ------------------------------------------------------------------ 백업 / 되돌리기

    public static Path backupFile(MinecraftServer server, Land l) {
        return server.getWorldPath(LevelResource.ROOT).resolve("rtshop").resolve("castle_backup").resolve("castle_" + l.castleNo + ".nbt");
    }

    static void backup(MinecraftServer server, ServerLevel level, Land l) throws Exception {
        Claim c = PlotManager.plotClaim(l);
        if (c == null) return;
        Path f = backupFile(server, l);
        if (Files.exists(f)) return; // 처음 모습만 남긴다
        Files.createDirectories(f.getParent());
        StructureTemplate tpl = new StructureTemplate();
        BlockPos origin = new BlockPos(c.x0, l.castleGround - FILL_DOWN, c.z0);
        tpl.fillFromWorld(level, origin, new net.minecraft.core.Vec3i(c.x1 - c.x0 + 1, FILL_DOWN + CLEAR_UP + 1, c.z1 - c.z0 + 1), false, null);
        CompoundTag tag = tpl.save(new CompoundTag());
        tag.putIntArray("rtshop_origin", new int[]{origin.getX(), origin.getY(), origin.getZ()});
        NbtIo.writeCompressed(tag, f.toFile());
    }

    /** 처음 모습으로 되돌린다 (OP). 이 토지의 칸만 되돌린다. */
    public static boolean restore(MinecraftServer server, Land l) throws Exception {
        Path f = backupFile(server, l);
        if (!Files.exists(f)) return false;
        ServerLevel level = level(server);
        CompoundTag tag = NbtIo.readCompressed(f.toFile());
        StructureTemplate tpl = new StructureTemplate();
        tpl.load(level.holderLookup(Registries.BLOCK), tag);
        int[] o = tag.getIntArray("rtshop_origin");
        BlockPos origin = new BlockPos(o[0], o[1], o[2]);
        tpl.placeInWorld(level, origin, origin, new StructurePlaceSettings(), level.getRandom(), Block.UPDATE_CLIENTS);
        l.cleared = false;
        LandData.get(server).touch();
        return true;
    }
}

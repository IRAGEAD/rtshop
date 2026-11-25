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
import java.util.HashSet;
import java.util.Set;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.tags.BlockTags;
import java.util.List;

/**
 * 성 안 토지 (아모노스 맵): 성 안 네 구역을 약 256블록짜리 토지로 빈틈 없이 나눈 목록(rtshop_defaults/castle_plots.json)을 구획으로 등록하고,
 * 처음 팔릴 때 그 자리를 평지로 만든다.
 *  - 길·물·성벽 칸은 처음부터 토지에서 빠져 있어서 건드리지 않는다.
 *  - 건물이 여러 칸에 걸쳐 있으면(같은 group) 그 칸들을 함께 평지로 만든다 (그 칸들은 아직 아무도 사지 않은 상태).
 *  - 다른 사람의 토지에 들어간 칸은 절대 건드리지 않는다. 바꾸기 전 모습은 world/rtshop/castle_backup 에 저장한다.
 */
public final class CastlePlots {
    private CastlePlots() {
    }

    /** 2: 네모 반듯한 토지 26칸 + 성 영역. 3: 주인이 지도에 표시한 네 구역(A~D)의 13칸만 (예전 칸은 주인이 없으면 지운다).
     * 4: 네 구역을 빈틈 없이 나눈 24칸 (네모가 아닌 칸 모양, 칸마다 약 256블록, 서로 맞닿음). */
    public static final int VERSION = 4;
    /** 지면 위로 비우는 높이. */
    public static final int CLEAR_UP = 64;

    /** 토지에 걸친 물체(나무·건물)의 바깥쪽 한 기둥. */
    public static final class Obj {
        public final int x, z, base;
        public final boolean tree;

        public Obj(int x, int z, int base, boolean tree) {
            this.x = x;
            this.z = z;
            this.base = base;
            this.tree = tree;
        }
    }

    public static final class Tile {
        public int no, x0, z0, w = 16, h = 16, ground;
        public final List<Obj> objects = new ArrayList<>();
        /** 칸 모양 (비트 = dz * w + dx). null = 네모 전체. */
        public java.util.BitSet mask;

        public boolean has(int dx, int dz) {
            return dx >= 0 && dx < w && dz >= 0 && dz < h && (mask == null || mask.get(dz * w + dx));
        }

        public int area() {
            return mask == null ? w * h : mask.cardinality();
        }
    }

    public static final class Def {
        public String dim = "minecraft:overworld";
        public int[] spawn = new int[3];
        public final List<Object[]> signature = new ArrayList<>();
        public final List<Tile> tiles = new ArrayList<>();
        public int mapX0, mapZ0, mapW, mapH, mapSize;
        public String mapTexture = "";
        /** 성 영역 다각형 (x, z). */
        public int[][] zone = new int[0][];
        public int zx0, zz0, zx1, zz1;
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
                    if (o.has("zone")) {
                        JsonArray z = o.getAsJsonArray("zone");
                        def.zone = new int[z.size()][];
                        def.zx0 = def.zz0 = Integer.MAX_VALUE;
                        def.zx1 = def.zz1 = Integer.MIN_VALUE;
                        for (int i = 0; i < z.size(); i++) {
                            JsonArray v = z.get(i).getAsJsonArray();
                            def.zone[i] = new int[]{v.get(0).getAsInt(), v.get(1).getAsInt()};
                            def.zx0 = Math.min(def.zx0, def.zone[i][0]);
                            def.zx1 = Math.max(def.zx1, def.zone[i][0]);
                            def.zz0 = Math.min(def.zz0, def.zone[i][1]);
                            def.zz1 = Math.max(def.zz1, def.zone[i][1]);
                        }
                    }
                    for (JsonElement e : o.getAsJsonArray("plots")) {
                        JsonObject t = e.getAsJsonObject();
                        Tile tile = new Tile();
                        tile.no = t.get("no").getAsInt();
                        tile.x0 = t.get("x0").getAsInt();
                        tile.z0 = t.get("z0").getAsInt();
                        tile.w = t.has("w") ? t.get("w").getAsInt() : 16;
                        tile.h = t.has("h") ? t.get("h").getAsInt() : 16;
                        tile.ground = t.get("ground").getAsInt();
                        if (t.has("mask")) {
                            tile.mask = PlotBrowser.unpackMask(t.get("mask").getAsString());
                        }
                        if (t.has("objects")) {
                            for (JsonElement oe : t.getAsJsonArray("objects")) {
                                JsonArray a = oe.getAsJsonArray();
                                tile.objects.add(new Obj(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt(), "T".equals(a.get(3).getAsString())));
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

    /** 시험용 토지 정의 (번호 → 칸). */
    public static final java.util.Map<Integer, Tile> TEST_TILES = new java.util.HashMap<>();

    public static Tile tile(int no) {
        Tile tt = TEST_TILES.get(no);
        if (tt != null) return tt;
        for (Tile t : def().tiles) {
            if (t.no == no) return t;
        }
        return null;
    }

    public static String name(int no) {
        return "성 안 토지 " + label(no);
    }

    /** 3판부터의 번호: 백의 자리 = 구역(1 북서 A, 2 북동 B, 3 남서 C, 4 남 D), 나머지 = 그 구역 안의 순서. 예전 번호(1~99)는 그대로. */
    public static String label(int no) {
        if (no >= 100 && no < 2700) {
            return (char) ('A' + no / 100 - 1) + "-" + (no % 100);
        }
        return Integer.toString(no);
    }

    public static long price(Tile t) {
        long p = LandConfig.get().castlePlotPrice * t.area() / 256;
        return Math.max(0, Math.round(p / 500.0) * 500);
    }

    /** 다각형 안인가 (성 영역). */
    public static boolean inZone(int x, int z) {
        Def d = def();
        if (d.zone.length < 3 || x < d.zx0 || x > d.zx1 || z < d.zz0 || z > d.zz1) return false;
        double px = x + 0.5, pz = z + 0.5;
        boolean in = false;
        for (int i = 0, j = d.zone.length - 1; i < d.zone.length; j = i++) {
            double xi = d.zone[i][0], zi = d.zone[i][1], xj = d.zone[j][0], zj = d.zone[j][1];
            if ((zi > pz) != (zj > pz) && px < (xj - xi) * (pz - zi) / (zj - zi) + xi) in = !in;
        }
        return in;
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

    /** 서버가 켜질 때: 아직 정하지 않았고 이 세계가 아모노스 맵이면 등록한다 (예전 배치면 새 배치로 바꾼다). */
    public static void autoInstall(MinecraftServer server) {
        try {
            LandData data = LandData.get(server);
            if (data.castleInstalled > 0 && data.castleInstalled < VERSION) {
                int[] up = upgrade(server);
                RtShop.LOGGER.info("[rtshop] 성 안 토지를 새 배치로 바꿨습니다: 새로 {}칸, 주인이 있어 그대로 둔 예전 칸 {}", up[0], up[1]);
                return;
            }
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
            RtShop.LOGGER.info("[rtshop] 아모노스 맵: 성 안 토지 {}칸을 등록했습니다 (이미 있음 {}, 다른 토지와 겹쳐 뺀 칸 {})", r[0], r[1], r[2]);
        } catch (Throwable t) {
            RtShop.LOGGER.warn("[rtshop] 성 안 토지 자동 등록 실패", t);
        }
    }

    /**
     * 예전 배치에서 새 배치로: 주인이 없는 예전 칸은 모두 지우고(새 목록에 없는 번호) 새 배치를 넣는다.
     * 샀다가 돌려줘서 평지가 된 채 비어 있는 칸은, 저장해 둔 처음 모습으로 되돌린 뒤 지운다.
     * 주인이 있는 칸은 그대로 둔다 (산 사람의 땅을 지우지 않는다). {새로 넣은 수, 남긴 예전 칸 수}.
     */
    public static int[] upgrade(MinecraftServer server) {
        LandData data = LandData.get(server);
        List<Land> rm = new ArrayList<>();
        int kept = 0;
        for (Land l : data.lands()) {
            if (!l.castle()) continue;
            if (l.forSale()) rm.add(l);
            else kept++;
        }
        for (Land l : rm) {
            if (l.cleared) {
                try {
                    restore(server, l);
                } catch (Exception e) {
                    RtShop.LOGGER.warn("[rtshop] 성 안 토지 {} 의 처음 모습을 되돌리지 못했습니다", l.name, e);
                }
            }
            data.removeLand(l);
        }
        int[] r = install(server);
        return new int[]{r[0], kept};
    }

    /** 성 안 토지를 판매 중 구획으로 등록 (이미 있는 번호·다른 토지와 겹치는 칸은 건너뜀). {만든 수, 이미 있음, 겹쳐서 뺌}. */
    public static int[] install(MinecraftServer server) {
        LandData data = LandData.get(server);
        ServerLevel level = level(server);
        String dim = LandManager.dim(level);
        int made = 0, skipped = 0, dropped = 0;
        int minY = level.getMinBuildHeight(), maxY = level.getMaxBuildHeight() - 1;
        LandConfig cfg = LandConfig.get();
        for (Tile t : def().tiles) {
            if (landOf(data, t.no) != null) {
                skipped++;
                continue;
            }
            boolean taken = false;
            for (Claim o : data.claimsNear(dim, t.x0, minY, t.z0, t.x0 + t.w - 1, maxY, t.z0 + t.h - 1, 0)) {
                // 칸 단위로 본다 (맞닿은 이웃 토지는 네모 범위가 겹쳐도 칸은 겹치지 않는다)
                for (int dz = 0; dz < t.h && !taken; dz++) {
                    for (int dx = 0; dx < t.w; dx++) {
                        if (t.has(dx, dz) && o.hasColumn(t.x0 + dx, t.z0 + dz)) {
                            taken = true;
                            break;
                        }
                    }
                }
                if (taken) break;
            }
            if (taken) {
                dropped++; // 빈 구멍이 생기지 않게 칸째로 뺀다
                continue;
            }
            Claim c = new Claim();
            c.dim = dim;
            c.type = "plot";
            c.x0 = t.x0;
            c.z0 = t.z0;
            c.x1 = t.x0 + t.w - 1;
            c.z1 = t.z0 + t.h - 1;
            c.mask = t.mask == null ? null : (java.util.BitSet) t.mask.clone();
            // 높이: 일반 토지 문서와 같다 (발밑 깊이 depthBelowFeet ~ 월드 꼭대기)
            c.y0 = Math.max(minY, t.ground + 1 - cfg.depthBelowFeet);
            c.y1 = maxY;
            Land land = data.createPlot(name(t.no), price(t));
            land.castleNo = t.no;
            land.castleGroup = -1;
            land.castleGround = t.ground;
            land.defaultPerms = 0; // 주인과 권한을 받은 사람만
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

    /** 이 세계에 성 영역이 있는가 (성 안 토지를 넣은 세계). 건축 보호 설정과 상관없이 PvP 안전 구역 판단에 쓴다. */
    public static boolean zoneKnown(MinecraftServer server) {
        return server != null && LandData.get(server).castleInstalled > 0 && def().zone.length >= 3;
    }

    /** 성 영역 보호가 켜져 있는가 (이 세계에 성 안 토지를 넣었을 때만). */
    public static boolean zoneActive(MinecraftServer server) {
        return server != null && LandConfig.get().castleZoneProtect && LandData.get(server).castleInstalled > 0 && def().zone.length >= 3;
    }

    // ------------------------------------------------------------------ 평지로 만들기

    /** 산 성 안 토지와 함께 바뀌는 토지 (이제는 그 토지 하나). */
    public static List<Land> clearSet(LandData data, Land land) {
        List<Land> out = new ArrayList<>();
        if (land.castle() && !land.cleared) out.add(land);
        return out;
    }

    /** 이 토지를 사면 함께 없어지는 바깥 기둥 수 (토지에 걸친 나무·건물의 나머지 부분). */
    public static int objectColumns(Land land) {
        Tile t = tile(land.castleNo);
        return t == null ? 0 : t.objects.size();
    }

    /** 토지를 산 직후 부른다: 토지 안을 평지(풀밭)로 만들고, 경계에 걸친 나무·건물은 통째로 없앤다. 바꾼 블록 수. */
    public static int prepare(MinecraftServer server, Land land) {
        if (!land.castle() || land.cleared) return 0;
        LandData data = LandData.get(server);
        ServerLevel level = level(server);
        int changed = 0;
        ListTag removed = new ListTag();
        try {
            backup(server, level, land);
        } catch (Exception e) {
            RtShop.LOGGER.warn("[rtshop] 성 안 토지 {} 백업 실패: {}", land.name, e.toString());
        }
        it.unimi.dsi.fastutil.longs.LongOpenHashSet gone = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
        changed += removeObjects(level, data, land, removed, gone);
        changed += flatten(level, data, land, gone);
        // 뒷정리: 토지에 걸쳐 잘린 나무와 공중에 뜬 조각 (남의 토지는 건드리지 않는다)
        int radius = LandConfig.get().sweepRadius;
        if (radius > 0) {
            try {
                String dim = LandManager.dim(level);
                LandSweep.Result r = LandSweep.run(level, gone, (x, y, z) -> mayTouch(data, dim, land, x, y, z), radius, removed);
                changed += r.blocks;
                if (r.blocks > 0) RtShop.LOGGER.info("[rtshop] 성 안 토지 {} 뒷정리: {}", land.name, r);
            } catch (Throwable t) {
                RtShop.LOGGER.warn("[rtshop] 성 안 토지 {} 뒷정리 실패: {}", land.name, t.toString());
            }
        }
        try {
            saveRemoved(server, land, removed);
        } catch (Exception e) {
            RtShop.LOGGER.warn("[rtshop] 성 안 토지 {} 물체 백업 실패: {}", land.name, e.toString());
        }
        land.cleared = true;
        data.touch();
        return changed;
    }

    /** 이 기둥을 건드려도 되는가: 이 토지, 주인 없는 성 안 토지, 토지가 아닌 곳(성 영역)만. 남의 토지는 절대 안 됨. */
    private static boolean mayTouch(LandData data, String dim, Land l, int x, int y, int z) {
        Claim c = data.claimAt(dim, x, y, z);
        if (c == null || c.landId == l.id) return true;
        Land o = data.land(c.landId);
        return o != null && o.castle() && o.forSale();
    }

    private static boolean treeBlock(BlockState st) {
        return st.is(BlockTags.LOGS) || st.is(BlockTags.LEAVES) || st.is(Blocks.VINE) || st.is(Blocks.BEE_NEST) || st.is(Blocks.COCOA)
                || st.is(Blocks.MANGROVE_ROOTS) || st.is(Blocks.GLOW_LICHEN) || st.is(Blocks.MOSS_CARPET) || st.is(BlockTags.SAPLINGS)
                || st.is(Blocks.SHROOMLIGHT) || st.is(BlockTags.WART_BLOCKS);
    }

    static int removeObjects(ServerLevel level, LandData data, Land l, ListTag removed, it.unimi.dsi.fastutil.longs.LongOpenHashSet gone) {
        Tile t = tile(l.castleNo);
        if (t == null) return 0;
        String dim = LandManager.dim(level);
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int n = 0;
        for (Obj o : t.objects) {
            if (!mayTouch(data, dim, l, o.x, o.base + 1, o.z)) continue;
            level.getChunk(o.x >> 4, o.z >> 4);
            int top = Math.min(level.getMaxBuildHeight() - 1, o.base + CLEAR_UP);
            for (int y = top; y > o.base; y--) {
                pos.set(o.x, y, o.z);
                BlockState st = level.getBlockState(pos);
                if (st.isAir() || (o.tree && !treeBlock(st))) continue;
                if (removed.size() < 200000) {
                    CompoundTag e = new CompoundTag();
                    e.putInt("x", o.x);
                    e.putInt("y", y);
                    e.putInt("z", o.z);
                    e.put("s", NbtUtils.writeBlockState(st));
                    removed.add(e);
                }
                level.setBlock(pos, air, flags);
                gone.add(BlockPos.asLong(o.x, y, o.z));
                n++;
            }
        }
        return n;
    }

    /** 이 토지 안만: 지면 위는 비우고, 지면은 풀, 바로 아래는 흙, 토지 바닥까지 빈 곳은 흙으로 메운다. */
    static int flatten(ServerLevel level, LandData data, Land l, it.unimi.dsi.fastutil.longs.LongOpenHashSet gone) {
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
                Claim at = data.claimAt(dim, x, g, z);
                if (at == null || at.landId != l.id) continue;
                level.getChunk(x >> 4, z >> 4);
                for (int y = top; y > g; y--) {
                    pos.set(x, y, z);
                    if (!level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, air, flags);
                        gone.add(BlockPos.asLong(x, y, z));
                        n++;
                    }
                }
                pos.set(x, g, z);
                level.setBlock(pos, grass, flags);
                for (int y = g - 1; y >= Math.max(c.y0, g - 3); y--) {
                    pos.set(x, y, z);
                    level.setBlock(pos, dirt, flags);
                }
                for (int y = g - 4; y >= c.y0; y--) {
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
        AABB box = new AABB(c.x0, c.y0, c.z0, c.x1 + 1, top + 1, c.z1 + 1);
        for (Entity e : level.getEntitiesOfClass(Entity.class, box,
                e -> e instanceof HangingEntity || e instanceof ArmorStand || e instanceof ItemEntity)) {
            e.discard();
        }
        return n;
    }

    // ------------------------------------------------------------------ 백업 / 되돌리기

    public static Path backupFile(MinecraftServer server, Land l) {
        return server.getWorldPath(LevelResource.ROOT).resolve("rtshop").resolve("castle_backup").resolve("castle_" + l.castleNo + ".nbt");
    }

    static Path removedFile(MinecraftServer server, Land l) {
        return server.getWorldPath(LevelResource.ROOT).resolve("rtshop").resolve("castle_backup").resolve("castle_" + l.castleNo + "_objects.nbt");
    }

    static void backup(MinecraftServer server, ServerLevel level, Land l) throws Exception {
        Claim c = PlotManager.plotClaim(l);
        if (c == null) return;
        Path f = backupFile(server, l);
        if (Files.exists(f)) return; // 처음 모습만 남긴다
        Files.createDirectories(f.getParent());
        StructureTemplate tpl = new StructureTemplate();
        BlockPos origin = new BlockPos(c.x0, c.y0, c.z0);
        int top = Math.min(level.getMaxBuildHeight() - 1, l.castleGround + CLEAR_UP);
        tpl.fillFromWorld(level, origin, new net.minecraft.core.Vec3i(c.x1 - c.x0 + 1, top - c.y0 + 1, c.z1 - c.z0 + 1), false, null);
        CompoundTag tag = tpl.save(new CompoundTag());
        tag.putIntArray("rtshop_origin", new int[]{origin.getX(), origin.getY(), origin.getZ()});
        NbtIo.writeCompressed(tag, f.toFile());
    }

    static void saveRemoved(MinecraftServer server, Land l, ListTag removed) throws Exception {
        Path f = removedFile(server, l);
        if (Files.exists(f) || removed.isEmpty()) return;
        Files.createDirectories(f.getParent());
        CompoundTag tag = new CompoundTag();
        tag.put("blocks", removed);
        NbtIo.writeCompressed(tag, f.toFile());
    }

    /** 처음 모습으로 되돌린다 (OP). 토지 안과, 남의 토지가 아닌 곳의 없앤 물체만 되돌린다. */
    public static boolean restore(MinecraftServer server, Land l) throws Exception {
        Path f = backupFile(server, l);
        if (!Files.exists(f)) return false;
        ServerLevel level = level(server);
        CompoundTag tag = NbtIo.readCompressed(f.toFile());
        StructureTemplate tpl = new StructureTemplate();
        tpl.load(level.holderLookup(Registries.BLOCK), tag);
        int[] o = tag.getIntArray("rtshop_origin");
        BlockPos origin = new BlockPos(o[0], o[1], o[2]);
        // 저장한 모습은 네모 범위 전체지만, 이 토지의 칸만 되돌린다 (맞닿은 이웃 토지는 건드리지 않는다)
        final Claim own = PlotManager.plotClaim(l);
        StructurePlaceSettings settings = new StructurePlaceSettings();
        if (own != null && own.mask != null) {
            settings.addProcessor(new net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor() {
                @Override
                public StructureTemplate.StructureBlockInfo processBlock(net.minecraft.world.level.LevelReader lr, BlockPos a, BlockPos b,
                                                                         StructureTemplate.StructureBlockInfo raw, StructureTemplate.StructureBlockInfo placed,
                                                                         StructurePlaceSettings s) {
                    return own.hasColumn(placed.pos().getX(), placed.pos().getZ()) ? placed : null;
                }

                @Override
                protected net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType<?> getType() {
                    return net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType.NOP;
                }
            });
        }
        tpl.placeInWorld(level, origin, origin, settings, level.getRandom(), Block.UPDATE_CLIENTS);
        Path rf = removedFile(server, l);
        if (Files.exists(rf)) {
            LandData data = LandData.get(server);
            String dim = LandManager.dim(level);
            ListTag list = NbtIo.readCompressed(rf.toFile()).getList("blocks", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag e = list.getCompound(i);
                int x = e.getInt("x"), y = e.getInt("y"), z = e.getInt("z");
                if (!mayTouch(data, dim, l, x, y, z)) continue;
                level.setBlock(new BlockPos(x, y, z), NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), e.getCompound("s")),
                        Block.UPDATE_CLIENTS);
            }
            Files.deleteIfExists(rf);
        }
        l.cleared = false;
        LandData.get(server).touch();
        return true;
    }
}

package com.rtshop.land;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 토지를 비운 뒤의 뒷정리 (3.51): 잘린 채 남는 것들을 치운다.
 *  1. 잘린 나무: 비운 자리에 닿아 있던 나무(잎이 토지 안으로 뻗어 있던 것)는 줄기와 잎까지 통째로 없앤다.
 *     통나무집을 나무로 잘못 보지 않도록 줄기가 작고(통나무 300개·너비 11칸 이하) 잎이 붙어 있는 것만 나무로 친다.
 *  2. 줄기 없이 남은 잎 덩어리.
 *  3. 공중에 뜬 조각: 비운 자리 곁에 남은 것 가운데 땅(흙·돌·모래…)에 닿지 않고 사방이 공기뿐인 작은 덩어리 (잘린 지붕 끝, 매달린 등불, 울타리 조각…).
 * 땅에 닿아 있는 건물·길·성벽은 건드리지 않는다. 남의 토지에 든 칸은 절대 건드리지 않는다 (touch).
 */
public final class LandSweep {
    private LandSweep() {
    }

    public interface Touch {
        boolean ok(int x, int y, int z);
    }

    public static final class Result {
        public int trees, leaves, floating, blocks;

        @Override
        public String toString() {
            return "trees " + trees + " leaf clumps " + leaves + " floating " + floating + " blocks " + blocks;
        }
    }

    public static final int TRUNK_MAX = 300, TRUNK_WIDE = 11, LEAF_REACH = 5, CLUMP_MAX = 400, FLOAT_MAX = 300;

    static boolean tree(BlockState st) {
        return st.is(BlockTags.LOGS) || leafy(st);
    }

    static boolean leafy(BlockState st) {
        return st.is(BlockTags.LEAVES) || st.is(Blocks.VINE) || st.is(Blocks.BEE_NEST) || st.is(Blocks.COCOA) || st.is(Blocks.MANGROVE_ROOTS)
                || st.is(Blocks.GLOW_LICHEN) || st.is(Blocks.MOSS_CARPET) || st.is(Blocks.SHROOMLIGHT) || st.is(BlockTags.WART_BLOCKS);
    }

    private static final class Run {
        final ServerLevel level;
        final LongOpenHashSet gone;
        final Touch touch;
        final ListTag removed;
        final int x0, y0, z0, x1, y1, z1;
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        final Result result = new Result();

        Run(ServerLevel level, LongOpenHashSet gone, Touch touch, int radius, ListTag removed) {
            this.level = level;
            this.gone = gone;
            this.touch = touch;
            this.removed = removed;
            int ax = Integer.MAX_VALUE, ay = Integer.MAX_VALUE, az = Integer.MAX_VALUE, bx = Integer.MIN_VALUE, by = Integer.MIN_VALUE, bz = Integer.MIN_VALUE;
            for (LongIterator it = gone.iterator(); it.hasNext(); ) {
                long p = it.nextLong();
                int x = BlockPos.getX(p), y = BlockPos.getY(p), z = BlockPos.getZ(p);
                ax = Math.min(ax, x);
                ay = Math.min(ay, y);
                az = Math.min(az, z);
                bx = Math.max(bx, x);
                by = Math.max(by, y);
                bz = Math.max(bz, z);
            }
            x0 = ax - radius;
            z0 = az - radius;
            x1 = bx + radius;
            z1 = bz + radius;
            y0 = Math.max(level.getMinBuildHeight(), ay - 2);
            y1 = Math.min(level.getMaxBuildHeight() - 1, by + radius);
        }

        boolean inBox(int x, int y, int z) {
            return x >= x0 && x <= x1 && y >= y0 && y <= y1 && z >= z0 && z <= z1;
        }

        BlockState at(int x, int y, int z) {
            return level.getBlockState(pos.set(x, y, z));
        }

        BlockState at(long p) {
            return at(BlockPos.getX(p), BlockPos.getY(p), BlockPos.getZ(p));
        }

        void remove(long p) {
            int x = BlockPos.getX(p), y = BlockPos.getY(p), z = BlockPos.getZ(p);
            BlockState st = at(x, y, z);
            if (st.isAir()) return;
            if (removed != null && removed.size() < 200000) {
                CompoundTag e = new CompoundTag();
                e.putInt("x", x);
                e.putInt("y", y);
                e.putInt("z", z);
                e.put("s", NbtUtils.writeBlockState(st));
                removed.add(e);
            }
            level.removeBlockEntity(pos.set(x, y, z));
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            gone.add(p);
            result.blocks++;
        }

        /** 비운 자리에 맞닿은(모서리 포함) 칸들. */
        LongArrayList rim() {
            LongOpenHashSet seen = new LongOpenHashSet();
            LongArrayList out = new LongArrayList();
            for (LongIterator it = gone.iterator(); it.hasNext(); ) {
                long p = it.nextLong();
                int x = BlockPos.getX(p), y = BlockPos.getY(p), z = BlockPos.getZ(p);
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            if ((dx | dy | dz) == 0 || !inBox(x + dx, y + dy, z + dz)) continue;
                            long n = BlockPos.asLong(x + dx, y + dy, z + dz);
                            if (!gone.contains(n) && seen.add(n)) out.add(n);
                        }
                    }
                }
            }
            return out;
        }

        // ---------------------------------------------------------------- 1·2. 나무

        void trees() {
            LongArrayList seeds = new LongArrayList();
            for (long p : rim()) {
                if (tree(at(p)) && touch.ok(BlockPos.getX(p), BlockPos.getY(p), BlockPos.getZ(p))) seeds.add(p);
            }
            if (seeds.isEmpty()) return;
            // 잘린 잎에서 잎을 따라가며 줄기를 찾는다
            Long2IntOpenHashMap depth = new Long2IntOpenHashMap();
            LongArrayFIFOQueue q = new LongArrayFIFOQueue();
            LongArrayList logs = new LongArrayList();
            for (long s : seeds) {
                depth.put(s, 0);
                q.enqueue(s);
            }
            while (!q.isEmpty() && depth.size() < 30000) {
                long p = q.dequeueLong();
                int d = depth.get(p);
                BlockState st = at(p);
                if (st.is(BlockTags.LOGS)) {
                    logs.add(p);
                    continue;
                }
                if (d >= LEAF_REACH + 1) continue;
                int x = BlockPos.getX(p), y = BlockPos.getY(p), z = BlockPos.getZ(p);
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            if ((dx | dy | dz) == 0 || !inBox(x + dx, y + dy, z + dz)) continue;
                            long n = BlockPos.asLong(x + dx, y + dy, z + dz);
                            if (depth.containsKey(n) || gone.contains(n) || !tree(at(x + dx, y + dy, z + dz))) continue;
                            depth.put(n, d + 1);
                            q.enqueue(n);
                        }
                    }
                }
            }
            LongOpenHashSet trunkSeen = new LongOpenHashSet();
            for (long l : logs) {
                if (trunkSeen.contains(l) || gone.contains(l)) continue;
                LongArrayList trunk = trunk(l, trunkSeen);
                if (trunk == null) continue;
                fell(trunk);
                result.trees++;
            }
            // 줄기 없이 남은 잎 덩어리
            LongOpenHashSet seen = new LongOpenHashSet();
            for (long s : seeds) {
                if (gone.contains(s) || seen.contains(s) || !leafy(at(s))) continue;
                LongArrayList clump = new LongArrayList();
                boolean clipped = false, hasLog = false;
                LongArrayFIFOQueue cq = new LongArrayFIFOQueue();
                cq.enqueue(s);
                seen.add(s);
                while (!cq.isEmpty()) {
                    long p = cq.dequeueLong();
                    clump.add(p);
                    if (clump.size() > CLUMP_MAX) {
                        clipped = true;
                        break;
                    }
                    int x = BlockPos.getX(p), y = BlockPos.getY(p), z = BlockPos.getZ(p);
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            for (int dx = -1; dx <= 1; dx++) {
                                if ((dx | dy | dz) == 0) continue;
                                int nx = x + dx, ny = y + dy, nz = z + dz;
                                long n = BlockPos.asLong(nx, ny, nz);
                                if (gone.contains(n) || seen.contains(n)) continue;
                                BlockState st = at(nx, ny, nz);
                                if (st.is(BlockTags.LOGS)) hasLog = true;
                                if (!leafy(st)) continue;
                                if (!inBox(nx, ny, nz) || !touch.ok(nx, ny, nz)) {
                                    clipped = true;
                                    continue;
                                }
                                seen.add(n);
                                cq.enqueue(n);
                            }
                        }
                    }
                }
                if (clipped || hasLog) continue;
                for (long p : clump) remove(p);
                result.leaves++;
            }
        }

        /** 이 통나무가 든 줄기 (나무로 볼 수 없으면 null). */
        LongArrayList trunk(long start, LongOpenHashSet trunkSeen) {
            LongArrayList out = new LongArrayList();
            LongArrayFIFOQueue q = new LongArrayFIFOQueue();
            q.enqueue(start);
            trunkSeen.add(start);
            boolean bad = false;
            int ax = Integer.MAX_VALUE, az = Integer.MAX_VALUE, bx = Integer.MIN_VALUE, bz = Integer.MIN_VALUE, leaves = 0;
            while (!q.isEmpty()) {
                long p = q.dequeueLong();
                out.add(p);
                if (out.size() > TRUNK_MAX) {
                    bad = true; // 계속 훑어서 이 덩어리 전체에 표시만 해 둔다 (다시 보지 않게), 단 너무 크면 그만
                    if (out.size() > TRUNK_MAX * 8) break;
                }
                int x = BlockPos.getX(p), y = BlockPos.getY(p), z = BlockPos.getZ(p);
                ax = Math.min(ax, x);
                az = Math.min(az, z);
                bx = Math.max(bx, x);
                bz = Math.max(bz, z);
                if (!touch.ok(x, y, z)) bad = true;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            if ((dx | dy | dz) == 0) continue;
                            int nx = x + dx, ny = y + dy, nz = z + dz;
                            BlockState st = at(nx, ny, nz);
                            if (st.is(BlockTags.LEAVES)) leaves++;
                            if (!st.is(BlockTags.LOGS)) continue;
                            long n = BlockPos.asLong(nx, ny, nz);
                            if (gone.contains(n) || trunkSeen.contains(n)) continue;
                            if (!inBox(nx, ny, nz)) {
                                bad = true;
                                continue;
                            }
                            trunkSeen.add(n);
                            q.enqueue(n);
                        }
                    }
                }
            }
            if (bad || bx - ax + 1 > TRUNK_WIDE || bz - az + 1 > TRUNK_WIDE || leaves < 6) return null;
            return out;
        }

        /** 줄기와, 줄기에서 잎을 따라 LEAF_REACH 칸 안의 잎을 없앤다. */
        void fell(LongArrayList trunk) {
            Long2IntOpenHashMap depth = new Long2IntOpenHashMap();
            LongArrayFIFOQueue q = new LongArrayFIFOQueue();
            for (long p : trunk) {
                depth.put(p, 0);
                q.enqueue(p);
            }
            LongArrayList take = new LongArrayList();
            while (!q.isEmpty()) {
                long p = q.dequeueLong();
                int d = depth.get(p);
                take.add(p);
                if (d >= LEAF_REACH) continue;
                int x = BlockPos.getX(p), y = BlockPos.getY(p), z = BlockPos.getZ(p);
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            if ((dx | dy | dz) == 0) continue;
                            int nx = x + dx, ny = y + dy, nz = z + dz;
                            long n = BlockPos.asLong(nx, ny, nz);
                            if (depth.containsKey(n) || gone.contains(n) || !inBox(nx, ny, nz) || !leafy(at(nx, ny, nz)) || !touch.ok(nx, ny, nz)) continue;
                            depth.put(n, d + 1);
                            q.enqueue(n);
                        }
                    }
                }
            }
            for (long p : take) remove(p);
        }

        // ---------------------------------------------------------------- 3. 공중에 뜬 조각

        void floating() {
            LongOpenHashSet seen = new LongOpenHashSet();
            for (long s : rim()) {
                if (seen.contains(s) || gone.contains(s)) continue;
                BlockState st0 = at(s);
                if (st0.isAir() || !st0.getFluidState().isEmpty() || PlotManager.isGround(st0)) continue;
                LongArrayList island = new LongArrayList();
                LongArrayFIFOQueue q = new LongArrayFIFOQueue();
                q.enqueue(s);
                seen.add(s);
                boolean clipped = false;
                while (!q.isEmpty()) {
                    long p = q.dequeueLong();
                    island.add(p);
                    if (island.size() > FLOAT_MAX) {
                        clipped = true;
                        break;
                    }
                    int x = BlockPos.getX(p), y = BlockPos.getY(p), z = BlockPos.getZ(p);
                    if (!touch.ok(x, y, z)) clipped = true;
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            for (int dx = -1; dx <= 1; dx++) {
                                if ((dx | dy | dz) == 0) continue;
                                int nx = x + dx, ny = y + dy, nz = z + dz;
                                long n = BlockPos.asLong(nx, ny, nz);
                                if (gone.contains(n) || seen.contains(n)) continue;
                                BlockState st = at(nx, ny, nz);
                                if (st.isAir() || !st.getFluidState().isEmpty()) continue;
                                if (PlotManager.isGround(st) || !inBox(nx, ny, nz)) {
                                    clipped = true; // 땅에 닿아 있다 (또는 살피는 범위 밖으로 이어진다): 뜬 조각이 아니다
                                    continue;
                                }
                                seen.add(n);
                                q.enqueue(n);
                            }
                        }
                    }
                }
                if (clipped) continue;
                for (long p : island) remove(p);
                result.floating++;
            }
        }
    }

    /**
     * @param gone    비운 칸들 (여기서 더 없앤 칸이 덧붙는다)
     * @param touch   건드려도 되는 칸인가 (남의 토지는 false)
     * @param radius  비운 자리에서 이만큼 바깥까지만 살핀다
     * @param removed 없앤 블록을 적어 둘 목록 (되돌리기용, null 이면 적지 않는다)
     */
    public static Result run(ServerLevel level, LongOpenHashSet gone, Touch touch, int radius, ListTag removed) {
        if (gone.isEmpty()) return new Result();
        Run r = new Run(level, gone, touch, Math.max(2, radius), removed);
        r.trees();
        r.floating();
        return r.result;
    }
}

package com.rtshop.land;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import com.rtshop.economy.Economy;
import com.rtshop.item.ModItems;
import com.rtshop.land.LandData.Claim;
import com.rtshop.land.LandData.Land;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3f;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 구획(칸 단위 모양의 토지).
 * <p>
 * OP 가 구획 지팡이로 구불구불한 밭의 모양을 고른 뒤 가격을 붙여 등록하면, 그 땅은 "판매 중인 구획" 으로 보호된다.
 * 플레이어는 구획 안에 서서 /rtshop plot buy 로 사고, 토지 설정 화면의 "토지 반환" 으로 되팔 수 있다.
 * 산 구획은 일반 토지와 똑같이 권한 설정·보관함 PIN 을 쓴다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class PlotManager {
    private PlotManager() {
    }

    /** 칸(x, z)이 어떤 모양 안에 있는지 묻는다. */
    public interface ColumnTest {
        boolean has(int x, int z);
    }

    // 지팡이 모드
    public static final int FILL_ADD = 0;
    public static final int FILL_REMOVE = 1;
    public static final int BRUSH_ADD = 2;
    public static final int BRUSH_REMOVE = 3;
    public static final int POLYGON = 4;
    static final String[] MODE_KEYS = {"fill_add", "fill_remove", "brush_add", "brush_remove", "polygon"};

    /** OP 한 명의 작업 중인 선택. */
    public static final class Selection {
        public String dim = "";
        public final LongOpenHashSet cells = new LongOpenHashSet();
        final ArrayDeque<LongOpenHashSet> undo = new ArrayDeque<>();
        public int mode = FILL_ADD;
        public int brush = 2;
        public final List<int[]> polygon = new ArrayList<>();
        /** 고치는 중인 구획 토지 ID (-1 = 새 구획). */
        public int editing = -1;
        int minX, minZ, maxX, maxZ;

        void bounds() {
            minX = minZ = Integer.MAX_VALUE;
            maxX = maxZ = Integer.MIN_VALUE;
            LongIterator it = cells.iterator();
            while (it.hasNext()) {
                long k = it.nextLong();
                int x = kx(k), z = kz(k);
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minZ = Math.min(minZ, z);
                maxZ = Math.max(maxZ, z);
            }
        }

        void snapshot() {
            undo.push(cells.clone());
            while (undo.size() > 30) {
                undo.removeLast();
            }
        }
    }

    private static final Map<UUID, Selection> SEL = new HashMap<>();
    /** 판매 안내를 이미 보여 준 구획: 플레이어 -> 토지 ID. */
    private static final Map<UUID, Integer> LAST_PLOT = new HashMap<>();
    private static final Map<UUID, Long> LAST_LEFT = new HashMap<>();

    public static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    public static int kx(long k) {
        return (int) (k >> 32);
    }

    public static int kz(long k) {
        return (int) k;
    }

    public static Selection selection(ServerPlayer p) {
        Selection s = SEL.computeIfAbsent(p.getUUID(), k -> new Selection());
        String dim = LandManager.dim(p.level());
        if (!dim.equals(s.dim)) {
            s.dim = dim;
            s.cells.clear();
            s.undo.clear();
            s.polygon.clear();
            s.editing = -1;
            s.bounds();
        }
        return s;
    }

    public static void clearRuntime() {
        SEL.clear();
        LAST_PLOT.clear();
        LAST_LEFT.clear();
        fieldCacheOf = null;
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        clearRuntime();
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        SEL.remove(id);
        LAST_PLOT.remove(id);
        LAST_LEFT.remove(id);
    }

    static boolean isOp(ServerPlayer p) {
        return p.hasPermissions(2);
    }

    static boolean holdsWand(ServerPlayer p) {
        return p.getMainHandItem().is(ModItems.PLOT_WAND.get());
    }

    private static void bar(ServerPlayer p, Component c) {
        p.displayClientMessage(c, true);
    }

    // ------------------------------------------------------------------ 지면 판정

    private static LandConfig fieldCacheOf;
    private static final Set<Block> FIELD_BLOCKS = new HashSet<>();
    private static final List<TagKey<Block>> FIELD_TAGS = new ArrayList<>();
    private static final Set<Block> RIM_STOP = new HashSet<>();

    private static void compile() {
        LandConfig cfg = LandConfig.get();
        if (fieldCacheOf == cfg) {
            return;
        }
        FIELD_BLOCKS.clear();
        FIELD_TAGS.clear();
        RIM_STOP.clear();
        for (String id : cfg.plotFillBlocks) {
            addBlockOrTag(id, FIELD_BLOCKS, FIELD_TAGS);
        }
        for (String id : cfg.plotRimStopBlocks) {
            addBlockOrTag(id, RIM_STOP, null);
        }
        fieldCacheOf = cfg;
    }

    private static void addBlockOrTag(String id, Set<Block> blocks, List<TagKey<Block>> tags) {
        try {
            if (id.startsWith("#")) {
                if (tags != null) {
                    tags.add(TagKey.create(Registries.BLOCK, new ResourceLocation(id.substring(1))));
                }
                return;
            }
            ResourceLocation rl = new ResourceLocation(id);
            if (ForgeRegistries.BLOCKS.containsKey(rl)) {
                blocks.add(ForgeRegistries.BLOCKS.getValue(rl));
            }
        } catch (RuntimeException ex) {
            RtShop.LOGGER.warn("[rtshop] land.json: 블록 ID '{}' 를 읽지 못했습니다.", id);
        }
    }

    static boolean isField(BlockState st) {
        compile();
        if (FIELD_BLOCKS.contains(st.getBlock())) {
            return true;
        }
        for (TagKey<Block> t : FIELD_TAGS) {
            if (st.is(t)) {
                return true;
            }
        }
        return false;
    }

    /** 칸의 맨 위 블록 (공기 제외). 청크가 불러와져 있지 않으면 null. */
    static BlockPos surface(ServerLevel level, int x, int z) {
        if (!level.hasChunk(x >> 4, z >> 4)) {
            return null;
        }
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(x, y, z);
        int min = level.getMinBuildHeight();
        while (m.getY() > min && level.getBlockState(m).isAir()) {
            m.move(0, -1, 0);
        }
        return m.immutable();
    }

    /** 다른 토지가 차지한 칸인가 (고치는 중인 구획은 제외). */
    private static boolean claimed(LandData data, String dim, int x, int y, int z, int ignoreLand) {
        Claim c = data.claimAt(dim, x, y, z);
        return c != null && c.landId != ignoreLand;
    }

    /**
     * 밭 자동 채우기: (sx, sz) 칸에서 시작해 "밭" 블록이 이어진 칸을 모두 고르고, 안쪽 구멍(나무·물길 등)을 메운 뒤
     * 둘레의 풀 테두리를 한 줄 더한다. 길·물·다른 토지는 넘지 않는다.
     *
     * @return 고른 칸. 시작 칸이 밭이 아니면 빈 집합, 너무 넓으면 null.
     */
    public static LongOpenHashSet floodField(ServerLevel level, int sx, int sz, int ignoreLand) {
        LandConfig cfg = LandConfig.get();
        LandData data = LandData.get(level.getServer());
        String dim = LandManager.dim(level);
        LongOpenHashSet out = new LongOpenHashSet();
        BlockPos start = surface(level, sx, sz);
        if (start == null || !isField(level.getBlockState(start)) || claimed(data, dim, sx, start.getY(), sz, ignoreLand)) {
            return out;
        }
        LongArrayFIFOQueue q = new LongArrayFIFOQueue();
        long s0 = key(sx, sz);
        out.add(s0);
        q.enqueue(s0);
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!q.isEmpty()) {
            long k = q.dequeueLong();
            int x = kx(k), z = kz(k);
            for (int[] d : dirs) {
                int nx = x + d[0], nz = z + d[1];
                long nk = key(nx, nz);
                if (out.contains(nk)) {
                    continue;
                }
                BlockPos sp = surface(level, nx, nz);
                if (sp == null || !isField(level.getBlockState(sp)) || claimed(data, dim, nx, sp.getY(), nz, ignoreLand)) {
                    continue;
                }
                out.add(nk);
                if (out.size() > cfg.plotFillMaxArea) {
                    return null;
                }
                q.enqueue(nk);
            }
        }
        fillHoles(out);
        if (cfg.plotFillGrowRim) {
            compile();
            LongOpenHashSet rim = new LongOpenHashSet();
            LongIterator it = out.iterator();
            while (it.hasNext()) {
                long k = it.nextLong();
                int x = kx(k), z = kz(k);
                for (int[] d : dirs) {
                    int nx = x + d[0], nz = z + d[1];
                    long nk = key(nx, nz);
                    if (out.contains(nk) || rim.contains(nk)) {
                        continue;
                    }
                    BlockPos sp = surface(level, nx, nz);
                    if (sp == null) {
                        continue;
                    }
                    BlockState st = level.getBlockState(sp);
                    if (!st.getFluidState().isEmpty() || RIM_STOP.contains(st.getBlock())
                            || claimed(data, dim, nx, sp.getY(), nz, ignoreLand)) {
                        continue;
                    }
                    rim.add(nk);
                }
            }
            out.addAll(rim);
            fillHoles(out);
        }
        return out;
    }

    /** 선택 안쪽에 갇힌 빈칸(밭 가운데의 나무, 물길, 허수아비 등)을 메운다. */
    public static void fillHoles(LongOpenHashSet cells) {
        if (cells.isEmpty()) {
            return;
        }
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        LongIterator it = cells.iterator();
        while (it.hasNext()) {
            long k = it.nextLong();
            minX = Math.min(minX, kx(k));
            maxX = Math.max(maxX, kx(k));
            minZ = Math.min(minZ, kz(k));
            maxZ = Math.max(maxZ, kz(k));
        }
        int x0 = minX - 1, z0 = minZ - 1;
        int w = maxX - minX + 3, h = maxZ - minZ + 3;
        if ((long) w * h > 4_000_000L) {
            return;
        }
        boolean[] in = new boolean[w * h];
        boolean[] outside = new boolean[w * h];
        it = cells.iterator();
        while (it.hasNext()) {
            long k = it.nextLong();
            in[(kz(k) - z0) * w + (kx(k) - x0)] = true;
        }
        int[] stack = new int[w * h];
        int sp = 0;
        stack[sp++] = 0; // (x0, z0) 는 바깥 테두리라 항상 비어 있다
        outside[0] = true;
        while (sp > 0) {
            int i = stack[--sp];
            int x = i % w, z = i / w;
            if (x > 0) sp = visit(i - 1, in, outside, stack, sp);
            if (x < w - 1) sp = visit(i + 1, in, outside, stack, sp);
            if (z > 0) sp = visit(i - w, in, outside, stack, sp);
            if (z < h - 1) sp = visit(i + w, in, outside, stack, sp);
        }
        for (int i = 0; i < w * h; i++) {
            if (!in[i] && !outside[i]) {
                cells.add(key(x0 + i % w, z0 + i / w));
            }
        }
    }

    private static int visit(int j, boolean[] in, boolean[] outside, int[] stack, int sp) {
        if (!in[j] && !outside[j]) {
            outside[j] = true;
            stack[sp++] = j;
        }
        return sp;
    }

    /** 다각형 안의 칸과 변이 지나는 칸. */
    public static LongOpenHashSet polygonCells(List<int[]> pts) {
        LongOpenHashSet out = new LongOpenHashSet();
        if (pts.size() < 3) {
            return out;
        }
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (int[] p : pts) {
            minX = Math.min(minX, p[0]);
            maxX = Math.max(maxX, p[0]);
            minZ = Math.min(minZ, p[1]);
            maxZ = Math.max(maxZ, p[1]);
        }
        int n = pts.size();
        for (int z = minZ; z <= maxZ; z++) {
            double cz = z + 0.5;
            for (int x = minX; x <= maxX; x++) {
                double cx = x + 0.5;
                boolean inside = false;
                for (int i = 0, j = n - 1; i < n; j = i++) {
                    double xi = pts.get(i)[0] + 0.5, zi = pts.get(i)[1] + 0.5;
                    double xj = pts.get(j)[0] + 0.5, zj = pts.get(j)[1] + 0.5;
                    if ((zi > cz) != (zj > cz) && cx < (xj - xi) * (cz - zi) / (zj - zi) + xi) {
                        inside = !inside;
                    }
                }
                if (inside) {
                    out.add(key(x, z));
                }
            }
        }
        for (int i = 0; i < n; i++) {
            int[] a = pts.get(i), b = pts.get((i + 1) % n);
            line(a[0], a[1], b[0], b[1], out);
        }
        return out;
    }

    private static void line(int x0, int z0, int x1, int z1, LongOpenHashSet out) {
        int dx = Math.abs(x1 - x0), dz = -Math.abs(z1 - z0);
        int sx = x0 < x1 ? 1 : -1, sz = z0 < z1 ? 1 : -1;
        int err = dx + dz;
        while (true) {
            out.add(key(x0, z0));
            if (x0 == x1 && z0 == z1) {
                return;
            }
            int e2 = 2 * err;
            if (e2 >= dz) {
                err += dz;
                x0 += sx;
            }
            if (e2 <= dx) {
                err += dx;
                z0 += sz;
            }
        }
    }

    // ------------------------------------------------------------------ 지팡이 조작

    /** 우클릭: pos 는 누른 블록 (허공이면 null → 최대 128칸 앞까지 시선이 닿는 블록). */
    public static void onRightClick(ServerPlayer p, BlockPos pos) {
        if (!isOp(p)) {
            bar(p, Component.translatable("rtshop.plot.op_only").withStyle(ChatFormatting.RED));
            return;
        }
        Selection s = selection(p);
        if (p.isShiftKeyDown()) {
            s.mode = (s.mode + 1) % MODE_KEYS.length;
            showMode(p, s);
            return;
        }
        if (pos == null) {
            HitResult hit = p.pick(128.0D, 1.0F, false);
            if (!(hit instanceof BlockHitResult bh) || hit.getType() != HitResult.Type.BLOCK) {
                bar(p, Component.translatable("rtshop.plot.no_target").withStyle(ChatFormatting.GRAY));
                return;
            }
            pos = bh.getBlockPos();
        }
        apply(p, s, pos, true);
    }

    /** 좌클릭 (손이 닿는 블록): 지금 모드의 반대 동작. */
    public static void onLeftClick(ServerPlayer p, BlockPos pos) {
        if (!isOp(p)) {
            return;
        }
        long now = p.level().getGameTime();
        Long last = LAST_LEFT.get(p.getUUID());
        if (last != null && now - last < 4 && now >= last) {
            return;
        }
        LAST_LEFT.put(p.getUUID(), now);
        apply(p, selection(p), pos, false);
    }

    /** 좌클릭은 서버에서만 가로챈다 (클라이언트에서 막으면 서버로 클릭이 전달되지 않음). 블록은 canAttackBlock 이 지킨다. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!event.getItemStack().is(ModItems.PLOT_WAND.get()) || !(event.getEntity() instanceof ServerPlayer sp)) {
            return;
        }
        event.setCanceled(true);
        onLeftClick(sp, event.getPos());
    }

    /**
     * 우클릭은 블록 자체의 동작(다 자란 작물 우클릭 수확, 문 열기 등)보다 먼저 가로챈다.
     * 그래야 밭을 고르다가 작물을 수확해 버리지 않는다. OP 가 아니면 아무것도 막지 않는다.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getItemStack().is(ModItems.PLOT_WAND.get()) || event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND
                || !event.getEntity().hasPermissions(2)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        if (event.getEntity() instanceof ServerPlayer sp) {
            onRightClick(sp, event.getPos());
        }
    }

    private static void apply(ServerPlayer p, Selection s, BlockPos pos, boolean primary) {
        ServerLevel level = p.serverLevel();
        int x = pos.getX(), z = pos.getZ();
        switch (s.mode) {
            case FILL_ADD, FILL_REMOVE -> {
                boolean add = (s.mode == FILL_ADD) == primary;
                LongOpenHashSet region = floodField(level, x, z, s.editing);
                if (region == null) {
                    p.sendSystemMessage(Component.translatable("rtshop.plot.fill_too_big", LandConfig.get().plotFillMaxArea)
                            .withStyle(ChatFormatting.RED));
                    return;
                }
                if (region.isEmpty() && add) {
                    BlockPos top = surface(level, x, z);
                    String id = top == null ? "?" : String.valueOf(ForgeRegistries.BLOCKS.getKey(level.getBlockState(top).getBlock()));
                    p.sendSystemMessage(Component.translatable("rtshop.plot.fill_not_field", id).withStyle(ChatFormatting.RED));
                    return;
                }
                if (!add && region.isEmpty()) {
                    // 밭이 아닌 곳에서 빼기: 그 칸이 속한 선택 덩어리를 통째로 뺀다
                    region = connected(s.cells, key(x, z));
                }
                s.snapshot();
                int before = s.cells.size();
                if (add) {
                    s.cells.addAll(region);
                } else {
                    s.cells.removeAll(region);
                }
                s.bounds();
                bar(p, Component.translatable(add ? "rtshop.plot.added" : "rtshop.plot.removed",
                        Math.abs(s.cells.size() - before), s.cells.size()).withStyle(ChatFormatting.YELLOW));
            }
            case BRUSH_ADD, BRUSH_REMOVE -> {
                boolean add = (s.mode == BRUSH_ADD) == primary;
                s.snapshot();
                int before = s.cells.size();
                int r = s.brush;
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (dx * dx + dz * dz > r * r + r) {
                            continue;
                        }
                        long k = key(x + dx, z + dz);
                        if (add) {
                            BlockPos top = surface(level, x + dx, z + dz);
                            if (top != null && claimed(LandData.get(p.server), s.dim, x + dx, top.getY(), z + dz, s.editing)) {
                                continue;
                            }
                            s.cells.add(k);
                        } else {
                            s.cells.remove(k);
                        }
                    }
                }
                s.bounds();
                bar(p, Component.translatable(add ? "rtshop.plot.added" : "rtshop.plot.removed",
                        Math.abs(s.cells.size() - before), s.cells.size()).withStyle(ChatFormatting.YELLOW));
            }
            case POLYGON -> {
                if (!primary) {
                    if (!s.polygon.isEmpty()) {
                        s.polygon.remove(s.polygon.size() - 1);
                    }
                    bar(p, Component.translatable("rtshop.plot.poly_undo", s.polygon.size()).withStyle(ChatFormatting.YELLOW));
                    return;
                }
                if (s.polygon.size() >= 3) {
                    int[] first = s.polygon.get(0);
                    if (Math.abs(first[0] - x) <= 1 && Math.abs(first[1] - z) <= 1) {
                        closePolygon(p, s);
                        return;
                    }
                }
                s.polygon.add(new int[]{x, z});
                bar(p, Component.translatable("rtshop.plot.poly_point", s.polygon.size()).withStyle(ChatFormatting.YELLOW));
            }
            default -> {
            }
        }
    }

    /** 다각형을 닫아 안쪽 칸을 선택에 더한다. */
    public static boolean closePolygon(ServerPlayer p, Selection s) {
        if (s.polygon.size() < 3) {
            p.sendSystemMessage(Component.translatable("rtshop.plot.poly_need3").withStyle(ChatFormatting.RED));
            return false;
        }
        LongOpenHashSet cells = polygonCells(s.polygon);
        LandData data = LandData.get(p.server);
        ServerLevel level = p.serverLevel();
        s.snapshot();
        int before = s.cells.size();
        LongIterator it = cells.iterator();
        while (it.hasNext()) {
            long k = it.nextLong();
            BlockPos top = surface(level, kx(k), kz(k));
            if (top != null && claimed(data, s.dim, kx(k), top.getY(), kz(k), s.editing)) {
                continue;
            }
            s.cells.add(k);
        }
        s.polygon.clear();
        s.bounds();
        p.sendSystemMessage(Component.translatable("rtshop.plot.added", s.cells.size() - before, s.cells.size())
                .withStyle(ChatFormatting.YELLOW));
        return true;
    }

    /** 선택 안에서 k 와 이어진 덩어리. */
    private static LongOpenHashSet connected(LongOpenHashSet cells, long k) {
        LongOpenHashSet out = new LongOpenHashSet();
        if (!cells.contains(k)) {
            return out;
        }
        LongArrayFIFOQueue q = new LongArrayFIFOQueue();
        out.add(k);
        q.enqueue(k);
        while (!q.isEmpty()) {
            long c = q.dequeueLong();
            int x = kx(c), z = kz(c);
            long[] ns = {key(x + 1, z), key(x - 1, z), key(x, z + 1), key(x, z - 1)};
            for (long n : ns) {
                if (cells.contains(n) && out.add(n)) {
                    q.enqueue(n);
                }
            }
        }
        return out;
    }

    static void showMode(ServerPlayer p, Selection s) {
        MutableComponent c = Component.translatable("rtshop.plot.mode", Component.translatable("rtshop.plot.mode." + MODE_KEYS[s.mode]));
        if (s.mode == BRUSH_ADD || s.mode == BRUSH_REMOVE) {
            c.append(Component.translatable("rtshop.plot.mode_brush", s.brush));
        }
        bar(p, c.withStyle(ChatFormatting.AQUA));
        p.sendSystemMessage(Component.translatable("rtshop.plot.mode_help." + MODE_KEYS[s.mode]).withStyle(ChatFormatting.GRAY));
    }

    public static boolean undo(ServerPlayer p) {
        Selection s = selection(p);
        if (s.undo.isEmpty()) {
            return false;
        }
        LongOpenHashSet prev = s.undo.pop();
        s.cells.clear();
        s.cells.addAll(prev);
        s.bounds();
        return true;
    }

    // ------------------------------------------------------------------ 구획 만들기 / 고치기

    /** 선택한 칸들로 구역을 만든다. 다른 토지와 겹치는 칸은 뺀다. 칸이 하나도 없으면 null. */
    public static Claim buildClaim(ServerLevel level, LongOpenHashSet cells, int ignoreLand, int[] dropped) {
        LandData data = LandData.get(level.getServer());
        String dim = LandManager.dim(level);
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight() - 1;
        int lowest = Integer.MAX_VALUE;
        LongOpenHashSet keep = new LongOpenHashSet();
        LongIterator it = cells.iterator();
        while (it.hasNext()) {
            long k = it.nextLong();
            int x = kx(k), z = kz(k);
            BlockPos top = surface(level, x, z);
            int ground = top == null ? 63 : groundY(level, top);
            boolean overlap = false;
            // 이 기둥에 이미 다른 토지가 있으면 (높이와 상관없이) 뺀다
            for (Claim o : data.claimsNear(dim, x, minY, z, x, maxY, z, 0)) {
                if (o.landId != ignoreLand) {
                    overlap = true;
                    break;
                }
            }
            if (overlap) {
                dropped[0]++;
                continue;
            }
            keep.add(k);
            lowest = Math.min(lowest, ground);
        }
        if (keep.isEmpty()) {
            return null;
        }
        Claim c = new Claim();
        c.dim = dim;
        c.type = "plot";
        c.x0 = c.z0 = Integer.MAX_VALUE;
        c.x1 = c.z1 = Integer.MIN_VALUE;
        it = keep.iterator();
        while (it.hasNext()) {
            long k = it.nextLong();
            c.x0 = Math.min(c.x0, kx(k));
            c.x1 = Math.max(c.x1, kx(k));
            c.z0 = Math.min(c.z0, kz(k));
            c.z1 = Math.max(c.z1, kz(k));
        }
        int w = c.width();
        c.mask = new BitSet(w * (c.z1 - c.z0 + 1));
        it = keep.iterator();
        while (it.hasNext()) {
            long k = it.nextLong();
            c.mask.set((kz(k) - c.z0) * w + (kx(k) - c.x0));
        }
        c.y0 = Math.max(minY, lowest - LandConfig.get().plotDepthBelow);
        c.y1 = maxY;
        return c;
    }

    /** 지면 높이: 작물·꽃·풀 같은 식물 아래의 흙 블록. 나뭇잎은 건너뛴다. */
    static int groundY(ServerLevel level, BlockPos top) {
        BlockPos.MutableBlockPos m = top.mutable();
        int min = level.getMinBuildHeight();
        for (int i = 0; i < 48 && m.getY() > min; i++) {
            BlockState st = level.getBlockState(m);
            if (!st.isAir() && st.getFluidState().isEmpty() && !st.is(net.minecraft.tags.BlockTags.LEAVES)
                    && !st.is(net.minecraft.tags.BlockTags.LOGS) && st.blocksMotion()) {
                return m.getY();
            }
            m.move(0, -1, 0);
        }
        return top.getY();
    }

    /** 새 구획을 등록한다. 성공하면 토지, 실패하면 null (메시지는 직접 보냄). */
    public static Land create(ServerPlayer p, long price, String name) {
        Selection s = selection(p);
        if (s.cells.isEmpty()) {
            p.sendSystemMessage(Component.translatable("rtshop.plot.empty").withStyle(ChatFormatting.RED));
            return null;
        }
        int[] dropped = {0};
        Claim c = buildClaim(p.serverLevel(), s.cells, -1, dropped);
        if (c == null) {
            p.sendSystemMessage(Component.translatable("rtshop.plot.all_taken").withStyle(ChatFormatting.RED));
            return null;
        }
        LandData data = LandData.get(p.server);
        long finalPrice = price >= 0 ? price : c.area() * LandConfig.get().plotPricePerBlock;
        Land land = data.createPlot(name, finalPrice);
        if (name == null || name.isBlank()) {
            land.name = defaultName(land.id);
        }
        data.addClaim(land, c);
        s.snapshot();
        s.cells.clear();
        s.polygon.clear();
        s.editing = -1;
        s.bounds();
        p.sendSystemMessage(Component.translatable("rtshop.plot.created", land.id, land.name, c.area(), money(land.price))
                .withStyle(ChatFormatting.GREEN));
        if (dropped[0] > 0) {
            p.sendSystemMessage(Component.translatable("rtshop.plot.dropped", dropped[0]).withStyle(ChatFormatting.GOLD));
        }
        return land;
    }

    /** 구획 모양을 선택으로 불러와 고친다. */
    public static boolean beginEdit(ServerPlayer p, Land land) {
        Selection s = selection(p);
        Claim c = plotClaim(land);
        if (c == null || !c.dim.equals(s.dim)) {
            p.sendSystemMessage(Component.translatable("rtshop.plot.other_dim").withStyle(ChatFormatting.RED));
            return false;
        }
        s.snapshot();
        s.cells.clear();
        LandData.forEachColumn(c, (x, z) -> s.cells.add(key(x, z)));
        s.polygon.clear();
        s.editing = land.id;
        s.bounds();
        p.sendSystemMessage(Component.translatable("rtshop.plot.editing", land.id, land.name, s.cells.size()).withStyle(ChatFormatting.YELLOW));
        return true;
    }

    /** 고친 모양을 저장한다. 주인·권한·가격은 그대로. */
    public static boolean saveEdit(ServerPlayer p) {
        Selection s = selection(p);
        LandData data = LandData.get(p.server);
        Land land = s.editing < 0 ? null : data.land(s.editing);
        if (land == null || !land.plot) {
            p.sendSystemMessage(Component.translatable("rtshop.plot.not_editing").withStyle(ChatFormatting.RED));
            return false;
        }
        Claim old = plotClaim(land);
        int[] dropped = {0};
        Claim fresh = buildClaim(p.serverLevel(), s.cells, land.id, dropped);
        if (fresh == null || old == null) {
            p.sendSystemMessage(Component.translatable("rtshop.plot.empty").withStyle(ChatFormatting.RED));
            return false;
        }
        data.replaceClaim(old, fresh);
        s.cells.clear();
        s.editing = -1;
        s.bounds();
        p.sendSystemMessage(Component.translatable("rtshop.plot.saved", land.id, land.name, fresh.area()).withStyle(ChatFormatting.GREEN));
        if (dropped[0] > 0) {
            p.sendSystemMessage(Component.translatable("rtshop.plot.dropped", dropped[0]).withStyle(ChatFormatting.GOLD));
        }
        return true;
    }

    /** 이름을 안 정했을 때의 구획 이름. 서버에는 번역 파일이 없으므로 글자로 직접 만든다. */
    static String defaultName(int id) {
        return "농지 " + id;
    }

    public static Claim plotClaim(Land land) {
        return land == null || land.claims.isEmpty() ? null : land.claims.get(0);
    }

    // ------------------------------------------------------------------ 사고 팔기

    public static String money(long v) {
        return String.format("%,d", v);
    }

    public static Land plotAt(ServerPlayer p) {
        Land l = LandData.get(p.server).landAt(LandManager.dim(p.level()), p.blockPosition());
        if (l == null) {
            // 작물 위에 서 있으면 발 위치가 한 칸 위일 수 있다
            l = LandData.get(p.server).landAt(LandManager.dim(p.level()), p.blockPosition().below());
        }
        return l != null && l.plot ? l : null;
    }

    public static int plotsOwned(LandData data, UUID id) {
        int n = 0;
        for (Land l : data.lands()) {
            if (l.plot && l.owner.equals(id)) {
                n++;
            }
        }
        return n;
    }

    /** 서 있는 구획을 산다. */
    public static boolean buy(ServerPlayer p) {
        Land land = plotAt(p);
        if (land == null) {
            p.sendSystemMessage(Component.translatable("rtshop.plot.not_in_plot").withStyle(ChatFormatting.RED));
            return false;
        }
        return buyLand(p, land);
    }

    /** 사려는 사람이 이 구획을 살 수 없는 이유 (살 수 있으면 null). */
    public static Component cannotBuy(ServerPlayer p, Land land) {
        LandData data = LandData.get(p.server);
        if (land == null || !land.plot) {
            return Component.translatable("rtshop.plot.not_in_plot");
        }
        if (land.forSale() && !land.group.isEmpty()) {
            return Component.translatable("rtshop.plot.starter_only", land.name);
        }
        if (!land.forSale()) {
            return Component.translatable(land.owner.equals(p.getUUID()) ? "rtshop.plot.already_yours" : "rtshop.plot.sold", land.name, land.ownerName);
        }
        LandConfig cfg = LandConfig.get();
        if (cfg.plotMaxPerPlayer > 0 && plotsOwned(data, p.getUUID()) >= cfg.plotMaxPerPlayer) {
            return Component.translatable("rtshop.plot.limit", cfg.plotMaxPerPlayer);
        }
        if (Economy.wallet(p) < land.price) {
            return Component.translatable("rtshop.plot.no_money", money(land.price), money(Economy.wallet(p)));
        }
        return null;
    }

    /** 구획을 산다 (서 있는 곳이든 토지 상인 창이든). 성 안 토지면 그 자리를 평지로 만든다. */
    public static boolean buyLand(ServerPlayer p, Land land) {
        LandData data = LandData.get(p.server);
        Component why = cannotBuy(p, land);
        if (why != null) {
            p.sendSystemMessage(why.copy().withStyle(ChatFormatting.RED));
            return false;
        }
        Economy.addWallet(p, -land.price);
        land.owner = p.getUUID();
        land.ownerName = p.getGameProfile().getName();
        land.paid = land.price;
        land.defaultPerms = 0;
        land.members.clear();
        data.touch();
        LAST_PLOT.remove(p.getUUID());
        LandManager.SHOW_BORDER.put(p.getUUID(), new long[]{land.id, p.level().getGameTime() + 300});
        p.sendSystemMessage(Component.translatable("rtshop.plot.bought", land.name, money(land.price)).withStyle(ChatFormatting.GREEN));
        if (land.castle() && !land.cleared) {
            int objs = CastlePlots.objectColumns(land);
            int n = CastlePlots.prepare(p.server, land);
            p.sendSystemMessage(Component.translatable(objs > 0 ? "rtshop.castle.cleared_objects" : "rtshop.castle.cleared")
                    .withStyle(ChatFormatting.GOLD));
            RtShop.LOGGER.info("[rtshop] {} 이(가) {} 을(를) 샀습니다: 바꾼 블록 {}", p.getGameProfile().getName(), land.name, n);
        }
        Claim c = plotClaim(land);
        if (c != null) {
            p.sendSystemMessage(Component.translatable("rtshop.plot.where", (c.x0 + c.x1) / 2, (c.z0 + c.z1) / 2).withStyle(ChatFormatting.GRAY));
        }
        p.sendSystemMessage(Component.translatable("rtshop.land.claimed_hint").withStyle(ChatFormatting.GRAY));
        return true;
    }

    /** 주인이 구획을 반환한다: 구매가의 일부를 돌려받고 구획은 다시 판매 중이 된다. */
    public static void giveBack(ServerPlayer p, Land land) {
        if (!land.group.isEmpty()) {
            resetToSale(LandData.get(p.server), land);
            p.sendSystemMessage(Component.translatable("rtshop.plot.starter_returned", land.name).withStyle(ChatFormatting.YELLOW));
            return;
        }
        long refund = land.paid * LandConfig.get().plotRefundPercent / 100;
        resetToSale(LandData.get(p.server), land);
        if (refund > 0) {
            Economy.addWallet(p, refund);
        }
        p.sendSystemMessage(Component.translatable("rtshop.plot.given_back", land.name, money(refund)).withStyle(ChatFormatting.YELLOW));
    }

    public static void resetToSale(LandData data, Land land) {
        data.clearPins(land);
        land.owner = LandData.NO_OWNER;
        land.ownerName = "";
        land.paid = 0;
        land.defaultPerms = 0;
        land.members.clear();
        data.touch();
    }

    public static long refundOf(Land land) {
        return land.paid * LandConfig.get().plotRefundPercent / 100;
    }

    // ------------------------------------------------------------------ 가져오기 (맵에서 미리 찾아 둔 밭)

    /**
     * config/rtshop/plots_import.json 의 구획을 등록한다. 형식:
     * {"dimension": "minecraft:overworld", "plots": [{"name": "..", "price": -1, "rows": {"z": [[x0, x1], ...]}}]}
     * price 가 음수면 1칸당 가격 × 칸 수. 이미 다른 토지가 있는 칸은 빠진다.
     */
    public static int[] importFile(ServerPlayer p, String fileName) throws Exception {
        Path path = FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve(fileName);
        JsonObject root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
        ServerLevel level = p.serverLevel();
        if (root.has("dimension")) {
            String dim = root.get("dimension").getAsString();
            for (ServerLevel l : p.server.getAllLevels()) {
                if (LandManager.dim(l).equals(dim)) {
                    level = l;
                }
            }
        }
        LandData data = LandData.get(p.server);
        int made = 0, skipped = 0, dropped = 0;
        if (root.has("replaces")) {
            // 나눠 다시 만든 밭: 예전 통짜 구획이 아직 판매 중(주인 없음)이면 지우고 새 조각으로 바꾼다
            for (JsonElement r : root.getAsJsonArray("replaces")) {
                Land old = findByName(data, r.getAsString());
                if (old != null && old.forSale()) {
                    data.removeLand(old);
                }
            }
        }
        for (JsonElement e : root.getAsJsonArray("plots")) {
            JsonObject o = e.getAsJsonObject();
            String name = o.has("name") ? o.get("name").getAsString() : "";
            if (!name.isBlank() && findByName(data, name) != null) {
                skipped++;
                continue; // 이미 가져온 구획
            }
            LongOpenHashSet cells = new LongOpenHashSet();
            for (Map.Entry<String, JsonElement> row : o.getAsJsonObject("rows").entrySet()) {
                int z = Integer.parseInt(row.getKey());
                for (JsonElement run : row.getValue().getAsJsonArray()) {
                    JsonArray r = run.getAsJsonArray();
                    for (int x = r.get(0).getAsInt(); x <= r.get(1).getAsInt(); x++) {
                        cells.add(key(x, z));
                    }
                }
            }
            int[] d = {0};
            Claim c = buildClaimNoLoad(level, cells, o, d);
            dropped += d[0];
            if (c == null) {
                skipped++;
                continue;
            }
            long price = o.has("price") ? o.get("price").getAsLong() : -1;
            Land land = data.createPlot(name, price >= 0 ? price : c.area() * LandConfig.get().plotPricePerBlock);
            if (name.isBlank()) {
                land.name = defaultName(land.id);
            }
            if (o.has("group")) {
                land.group = o.get("group").getAsString().trim();
            }
            data.addClaim(land, c);
            made++;
        }
        return new int[]{made, skipped, dropped};
    }

    /** 가져오기용: 청크를 불러오지 않고 파일에 적힌 지면 높이(groundMin)를 쓴다. */
    private static Claim buildClaimNoLoad(ServerLevel level, LongOpenHashSet cells, JsonObject o, int[] dropped) {
        LandData data = LandData.get(level.getServer());
        String dim = LandManager.dim(level);
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight() - 1;
        LongOpenHashSet keep = new LongOpenHashSet();
        LongIterator it = cells.iterator();
        while (it.hasNext()) {
            long k = it.nextLong();
            if (!data.claimsNear(dim, kx(k), minY, kz(k), kx(k), maxY, kz(k), 0).isEmpty()) {
                dropped[0]++;
                continue;
            }
            keep.add(k);
        }
        if (keep.isEmpty()) {
            return null;
        }
        Claim c = new Claim();
        c.dim = dim;
        c.type = "plot";
        c.x0 = c.z0 = Integer.MAX_VALUE;
        c.x1 = c.z1 = Integer.MIN_VALUE;
        it = keep.iterator();
        while (it.hasNext()) {
            long k = it.nextLong();
            c.x0 = Math.min(c.x0, kx(k));
            c.x1 = Math.max(c.x1, kx(k));
            c.z0 = Math.min(c.z0, kz(k));
            c.z1 = Math.max(c.z1, kz(k));
        }
        int w = c.width();
        c.mask = new BitSet(w * (c.z1 - c.z0 + 1));
        it = keep.iterator();
        while (it.hasNext()) {
            long k = it.nextLong();
            c.mask.set((kz(k) - c.z0) * w + (kx(k) - c.x0));
        }
        int ground = o.has("groundMin") ? o.get("groundMin").getAsInt() : 62;
        c.y0 = Math.max(minY, ground - LandConfig.get().plotDepthBelow);
        c.y1 = maxY;
        return c;
    }

    // ------------------------------------------------------------------ 나누기

    /**
     * 칸 묶음을 size×size 격자로 자른다. 격자 칸 안에서 떨어져 있는 조각은 따로 세고,
     * 너무 작은 조각(격자 넓이의 40% 미만)은 가장 길게 맞닿은 이웃 조각에 붙인다. 북쪽(작은 z)부터 순서대로 돌려준다.
     */
    public static List<LongOpenHashSet> splitCells(LongOpenHashSet cells, int size) {
        Map<Long, Integer> piece = new HashMap<>();
        if (cells.isEmpty()) {
            return new ArrayList<>();
        }
        int x0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE;
        LongIterator it = cells.iterator();
        while (it.hasNext()) {
            long k = it.nextLong();
            x0 = Math.min(x0, kx(k));
            z0 = Math.min(z0, kz(k));
        }
        long[] sorted = cells.toLongArray();
        java.util.Arrays.sort(sorted);
        int next = 0;
        for (long start : sorted) {
            if (piece.containsKey(start)) {
                continue;
            }
            int tx = Math.floorDiv(kx(start) - x0, size), tz = Math.floorDiv(kz(start) - z0, size);
            ArrayDeque<Long> st = new ArrayDeque<>();
            st.push(start);
            piece.put(start, next);
            while (!st.isEmpty()) {
                long c = st.pop();
                int x = kx(c), z = kz(c);
                long[] ns = {key(x + 1, z), key(x - 1, z), key(x, z + 1), key(x, z - 1)};
                for (long n : ns) {
                    if (cells.contains(n) && !piece.containsKey(n)
                            && Math.floorDiv(kx(n) - x0, size) == tx && Math.floorDiv(kz(n) - z0, size) == tz) {
                        piece.put(n, next);
                        st.push(n);
                    }
                }
            }
            next++;
        }
        int min = Math.max(1, size * size * 2 / 5);
        while (true) {
            Map<Integer, Integer> sizes = new HashMap<>();
            for (int v : piece.values()) {
                sizes.merge(v, 1, Integer::sum);
            }
            int small = -1;
            for (Map.Entry<Integer, Integer> e : sizes.entrySet()) {
                if (e.getValue() < min && (small < 0 || e.getValue() < sizes.get(small))) {
                    small = e.getKey();
                }
            }
            if (small < 0 || sizes.size() <= 1) {
                break;
            }
            Map<Integer, Integer> border = new HashMap<>();
            for (Map.Entry<Long, Integer> e : piece.entrySet()) {
                if (e.getValue() != small) {
                    continue;
                }
                int x = kx(e.getKey()), z = kz(e.getKey());
                long[] ns = {key(x + 1, z), key(x - 1, z), key(x, z + 1), key(x, z - 1)};
                for (long n : ns) {
                    Integer o = piece.get(n);
                    if (o != null && o != small) {
                        border.merge(o, 1, Integer::sum);
                    }
                }
            }
            if (border.isEmpty()) {
                break; // 떨어져 있는 작은 조각: 그대로 둔다
            }
            int target = -1;
            for (Map.Entry<Integer, Integer> e : border.entrySet()) {
                if (target < 0 || e.getValue() > border.get(target)
                        || (e.getValue().equals(border.get(target)) && sizes.get(e.getKey()) < sizes.get(target))) {
                    target = e.getKey();
                }
            }
            final int from = small, to = target;
            piece.replaceAll((k, v) -> v == from ? to : v);
        }
        Map<Integer, LongOpenHashSet> groups = new HashMap<>();
        for (Map.Entry<Long, Integer> e : piece.entrySet()) {
            groups.computeIfAbsent(e.getValue(), k -> new LongOpenHashSet()).add((long) e.getKey());
        }
        List<LongOpenHashSet> out = new ArrayList<>(groups.values());
        final int originZ = z0;
        out.sort((a, b) -> {
            double[] ca = centre(a), cb = centre(b);
            int ra = (int) Math.floor((ca[1] - originZ) / size), rb = (int) Math.floor((cb[1] - originZ) / size);
            return ra != rb ? Integer.compare(ra, rb) : Double.compare(ca[0], cb[0]);
        });
        return out;
    }

    private static double[] centre(LongOpenHashSet cells) {
        double sx = 0, sz = 0;
        LongIterator it = cells.iterator();
        while (it.hasNext()) {
            long k = it.nextLong();
            sx += kx(k);
            sz += kz(k);
        }
        return new double[]{sx / cells.size(), sz / cells.size()};
    }

    /** 판매 중인 구획을 size×size 쯤의 여러 구획으로 나눈다. 가격은 넓이에 비례해 나누고 묶음 이름은 그대로 잇는다. */
    public static List<Land> split(LandData data, Land land, int size) {
        Claim c = plotClaim(land);
        List<Land> made = new ArrayList<>();
        if (c == null || !land.forSale()) {
            return made;
        }
        LongOpenHashSet cells = new LongOpenHashSet();
        LandData.forEachColumn(c, (x, z) -> cells.add(key(x, z)));
        List<LongOpenHashSet> parts = splitCells(cells, size);
        if (parts.size() <= 1) {
            return made;
        }
        int total = cells.size();
        String base = land.name;
        long price = land.price;
        String group = land.group;
        data.removeLand(land);
        int i = 1;
        for (LongOpenHashSet part : parts) {
            Claim pc = shaped(c.dim, part, c.y0, c.y1);
            Land l = data.createPlot(base + "-" + i, price * part.size() / total);
            l.group = group;
            data.addClaim(l, pc);
            made.add(l);
            i++;
        }
        return made;
    }

    /** 칸 집합으로 모양 구역을 만든다 (겹침 검사 없음: 원래 한 구획이던 칸만 넣을 것). */
    static Claim shaped(String dim, LongOpenHashSet cells, int y0, int y1) {
        Claim c = new Claim();
        c.dim = dim;
        c.type = "plot";
        c.x0 = c.z0 = Integer.MAX_VALUE;
        c.x1 = c.z1 = Integer.MIN_VALUE;
        LongIterator it = cells.iterator();
        while (it.hasNext()) {
            long k = it.nextLong();
            c.x0 = Math.min(c.x0, kx(k));
            c.x1 = Math.max(c.x1, kx(k));
            c.z0 = Math.min(c.z0, kz(k));
            c.z1 = Math.max(c.z1, kz(k));
        }
        int w = c.width();
        c.mask = new BitSet(w * (c.z1 - c.z0 + 1));
        it = cells.iterator();
        while (it.hasNext()) {
            long k = it.nextLong();
            c.mask.set((kz(k) - c.z0) * w + (kx(k) - c.x0));
        }
        c.y0 = y0;
        c.y1 = y1;
        return c;
    }

    public static Land findByName(LandData data, String name) {
        for (Land l : data.lands()) {
            if (l.plot && l.name.equals(name)) {
                return l;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ 표시

    private static final Vector3f YELLOW = new Vector3f(1.0F, 0.9F, 0.2F);
    private static final Vector3f MAGENTA = new Vector3f(1.0F, 0.3F, 0.9F);
    private static final Vector3f WHITE = new Vector3f(1.0F, 1.0F, 1.0F);
    private static final Vector3f AQUA = new Vector3f(0.3F, 0.8F, 1.0F);
    private static final Vector3f ORANGE = new Vector3f(1.0F, 0.6F, 0.1F);

    /**
     * 10틱마다 불린다. 구획 지팡이를 든 OP 에게 선택 영역과 주변 구획을 그리고 true 를 돌려준다.
     * 그 밖의 플레이어에게는 판매 중인 구획에 들어섰을 때 안내를 한 번 보낸다.
     */
    public static boolean tick(ServerPlayer p, ServerLevel level, long now) {
        if (holdsWand(p) && isOp(p)) {
            Selection s = selection(p);
            if (!s.cells.isEmpty()) {
                drawOutline(level, p, (x, z) -> s.cells.contains(key(x, z)), s.minX, s.minZ, s.maxX, s.maxZ, YELLOW, 900);
            }
            if (!s.polygon.isEmpty()) {
                drawPolygon(level, p, s.polygon);
            }
            {
                LandData data = LandData.get(p.server);
                int bx = p.getBlockX(), bz = p.getBlockZ();
                int shown = 0;
                for (Claim o : data.claimsNear(s.dim, bx - 48, level.getMinBuildHeight(), bz - 48, bx + 48,
                        level.getMaxBuildHeight(), bz + 48, 0)) {
                    if (o.mask == null || o.landId == s.editing) {
                        continue;
                    }
                    Land l = data.land(o.landId);
                    drawOutline(level, p, o::hasColumn, o.x0, o.z0, o.x1, o.z1, l != null && l.forSale() ? WHITE : AQUA, 500);
                    if (++shown >= 6) {
                        break;
                    }
                }
            }
            return true;
        }
        Land here = plotAt(p);
        if (here == null || !here.forSale() || !here.group.isEmpty()) {
            LAST_PLOT.remove(p.getUUID());
            return false;
        }
        Integer last = LAST_PLOT.get(p.getUUID());
        if (last == null || last != here.id) {
            LAST_PLOT.put(p.getUUID(), here.id);
            Claim c = plotClaim(here);
            p.sendSystemMessage(Component.translatable("rtshop.plot.for_sale_merchant", here.name, c == null ? 0 : c.area(),
                    money(here.price)).withStyle(ChatFormatting.GOLD));
            LandManager.SHOW_BORDER.put(p.getUUID(), new long[]{here.id, now + 200});
        }
        return false;
    }

    /** 칸 모양의 바깥 둘레를 지면 높이를 따라 입자로 그린다 (플레이어 주변 48칸만). */
    public static void drawOutline(ServerLevel level, ServerPlayer sp, ColumnTest t, int x0, int z0, int x1, int z1,
                                   Vector3f color, int cap) {
        int y = sp.getBlockY();
        Borders.columns(sp, t, x0, z0, x1, z1, y - 6, y + 10, Borders.rgb(color));
    }

    private static void drawPolygon(ServerLevel level, ServerPlayer sp, List<int[]> pts) {
        DustParticleOptions dust = new DustParticleOptions(MAGENTA, 1.6F);
        int n = 0;
        for (int i = 0; i < pts.size(); i++) {
            int[] a = pts.get(i);
            int ya = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, a[0], a[1]);
            for (int k = 0; k < 6; k++) {
                level.sendParticles(sp, dust, true, a[0] + 0.5, ya + 0.3 + k * 0.4, a[1] + 0.5, 1, 0, 0, 0, 0);
            }
            if (i + 1 < pts.size()) {
                int[] b = pts.get(i + 1);
                LongOpenHashSet line = new LongOpenHashSet();
                line(a[0], a[1], b[0], b[1], line);
                LongIterator it = line.iterator();
                while (it.hasNext() && n < 600) {
                    long k = it.nextLong();
                    int x = kx(k), z = kz(k);
                    level.sendParticles(sp, dust, true, x + 0.5,
                            level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + 0.2, z + 0.5, 1, 0, 0, 0, 0);
                    n++;
                }
            }
        }
    }

    /** 서버가 켜질 때: 아무 일도 하지 않지만 설정을 미리 읽어 둔다. */
    public static void warmUp(MinecraftServer server) {
        compile();
    }
}

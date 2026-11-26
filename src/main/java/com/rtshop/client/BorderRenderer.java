package com.rtshop.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.rtshop.RtShop;
import com.rtshop.land.Borders;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * 토지·농경지의 둘레를 땅을 따라가는 낮은 테두리로 그린다: 바닥 선 하나, 한 칸 위의 옅은 선 하나, 꺾이는 곳마다 짧은 기둥.
 * 예전처럼 하늘까지 솟는 격자 벽은 그리지 않는다 (위를 보면 벽의 윗부분이 화면 가운데에 떠 보이던 문제).
 * 높이는 플레이어가 서 있는 층의 바닥을 기준으로 잡는다 (건물 안에서도 그 층 바닥에 보인다).
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID, value = Dist.CLIENT)
public final class BorderRenderer {
    private BorderRenderer() {
    }

    /** 둘레의 한 칸: alongX 면 (x,z)-(x+1,z), 아니면 (x,z)-(x,z+1). */
    private record Edge(int x, int z, boolean alongX, int color) {
    }

    private record Corner(int x, int z, int color) {
    }

    private static List<Edge> edges = List.of();
    private static List<Corner> corners = List.of();
    private static long until;
    /** 그리는 거리(칸). 이보다 멀면 그리지 않고, 끝 쪽은 흐려진다. */
    private static final double RANGE = 72;

    private static final Long2IntOpenHashMap HEIGHTS = new Long2IntOpenHashMap();
    private static int heightsRefY = Integer.MIN_VALUE;
    private static long heightsAt;

    static {
        HEIGHTS.defaultReturnValue(Integer.MIN_VALUE);
    }

    public static void set(List<Borders.Shape> shapes) {
        List<Edge> es = new ArrayList<>();
        List<Corner> cs = new ArrayList<>();
        for (Borders.Shape s : shapes) {
            build(s, es, cs);
        }
        edges = es;
        corners = cs;
        until = shapes.isEmpty() ? 0 : System.currentTimeMillis() + 1600;
    }

    private static boolean in(Borders.Shape s, int x, int z) {
        if (x < s.x0 || x > s.x1 || z < s.z0 || z > s.z1) return false;
        return s.mask == null || s.mask.get((z - s.z0) * (s.x1 - s.x0 + 1) + (x - s.x0));
    }

    private static void build(Borders.Shape s, List<Edge> out, List<Corner> cs) {
        int c = s.color;
        for (int z = s.z0; z <= s.z1 + 1; z++) {
            if (((s.open & 4) != 0 && z == s.z0) || ((s.open & 8) != 0 && z == s.z1 + 1)) continue;
            for (int x = s.x0; x <= s.x1; x++) {
                if (in(s, x, z) != in(s, x, z - 1)) out.add(new Edge(x, z, true, c));
            }
        }
        for (int x = s.x0; x <= s.x1 + 1; x++) {
            if (((s.open & 1) != 0 && x == s.x0) || ((s.open & 2) != 0 && x == s.x1 + 1)) continue;
            for (int z = s.z0; z <= s.z1; z++) {
                if (in(s, x, z) != in(s, x - 1, z)) out.add(new Edge(x, z, false, c));
            }
        }
        // 모서리: 둘레가 꺾이는 점 (네 칸 중 안쪽이 1개 또는 3개, 또는 대각선 2개)
        for (int z = s.z0; z <= s.z1 + 1; z++) {
            for (int x = s.x0; x <= s.x1 + 1; x++) {
                boolean a = in(s, x - 1, z - 1), b = in(s, x, z - 1), d = in(s, x - 1, z), e = in(s, x, z);
                int n = (a ? 1 : 0) + (b ? 1 : 0) + (d ? 1 : 0) + (e ? 1 : 0);
                boolean openSide = ((s.open & 1) != 0 && x == s.x0) || ((s.open & 2) != 0 && x == s.x1 + 1)
                        || ((s.open & 4) != 0 && z == s.z0) || ((s.open & 8) != 0 && z == s.z1 + 1);
                if (!openSide && (n == 1 || n == 3 || (n == 2 && a == e))) cs.add(new Corner(x, z, c));
            }
        }
    }

    public static boolean activeForTest() {
        return System.currentTimeMillis() < until && !edges.isEmpty();
    }

    public static int segmentsForTest() {
        return edges.size();
    }

    /** 시험용: 테두리가 플레이어 발밑에서 위로 얼마나 솟는가 (낮은 테두리인지 확인). */
    public static double maxRiseForTest() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return 0;
        int ref = (int) Math.floor(mc.player.getY());
        double max = 0;
        for (Corner c : corners) {
            max = Math.max(max, vertexY(mc.level, c.x, c.z, ref) + POST - mc.player.getY());
        }
        return max;
    }

    /**
     * 시험용: 테두리 기둥 꼭대기가 "그 자리의 실제 땅"(맞닿은 네 칸 가운데 가장 높은 윗면)보다 얼마나 위에 있는가.
     * 지형이 오르내려도 기둥 높이(POST)를 넘지 않아야 한다 (발밑 기준의 maxRiseForTest 는 언덕에서는 지형만큼 커진다).
     */
    public static double maxRiseAboveGroundForTest() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return 0;
        int ref = (int) Math.floor(mc.player.getY());
        double max = 0;
        for (Corner c : corners) {
            int top = Math.max(Math.max(mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, c.x, c.z), mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, c.x - 1, c.z)),
                    Math.max(mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, c.x, c.z - 1), mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, c.x - 1, c.z - 1)));
            max = Math.max(max, vertexY(mc.level, c.x, c.z, ref) + POST - top);
        }
        return max;
    }

    public static int cornersForTest() {
        return corners.size();
    }

    private static final double RAIL_LOW = 0.08, RAIL_HIGH = 1.1, POST = 1.7;

    // ------------------------------------------------------------------ 바닥 높이

    private static boolean solid(ClientLevel level, BlockPos.MutableBlockPos m) {
        BlockState st = level.getBlockState(m);
        return !st.isAir() && !st.is(BlockTags.LEAVES) && !st.getCollisionShape(level, m).isEmpty();
    }

    /** 그 칸에서 플레이어가 있는 층의 바닥 윗면 높이. */
    private static int floorY(ClientLevel level, int x, int z, int refY) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(x, refY + 2, z);
        if (solid(level, m)) {
            // 벽이나 언덕 속: 위로 올라가 윗면을 찾는다
            for (int i = 0; i < 10; i++) {
                m.setY(m.getY() + 1);
                if (!solid(level, m)) return m.getY();
            }
            return refY + 2;
        }
        for (int i = 0; i < 24; i++) {
            m.setY(m.getY() - 1);
            if (solid(level, m)) return m.getY() + 1;
        }
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        return Math.min(top, refY - 22);
    }

    /** 격자점 (x,z) 의 높이: 맞닿은 네 칸 중 가장 높은 바닥 (선이 땅에 묻히지 않게). */
    private static int vertexY(ClientLevel level, int x, int z, int refY) {
        long key = (((long) x) << 32) ^ (z & 0xFFFFFFFFL);
        int v = HEIGHTS.get(key);
        if (v != Integer.MIN_VALUE) return v;
        v = Math.max(Math.max(floorY(level, x, z, refY), floorY(level, x - 1, z, refY)),
                Math.max(floorY(level, x, z - 1, refY), floorY(level, x - 1, z - 1, refY)));
        HEIGHTS.put(key, v);
        return v;
    }

    // ------------------------------------------------------------------ 그리기

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || System.currentTimeMillis() > until || edges.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        ClientLevel level = mc.level;
        Camera cam = event.getCamera();
        Vec3 cp = cam.getPosition();
        int refY = (int) Math.floor(mc.player.getY());
        long now = System.currentTimeMillis();
        if (Math.abs(refY - heightsRefY) >= 2 || now - heightsAt > 1000) {
            HEIGHTS.clear();
            heightsRefY = refY;
            heightsAt = now;
        }
        PoseStack ps = event.getPoseStack();
        Matrix4f m = ps.last().pose();
        Matrix3f nm = ps.last().normal();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buf.getBuffer(RenderType.lines());
        double px = mc.player.getX(), pz = mc.player.getZ();
        for (Edge e : edges) {
            double mx = e.x + (e.alongX ? 0.5 : 0), mz = e.z + (e.alongX ? 0 : 0.5);
            double dist = Math.sqrt((mx - px) * (mx - px) + (mz - pz) * (mz - pz));
            if (dist > RANGE) continue;
            float fade = (float) Math.min(1, (RANGE - dist) / 24.0);
            int x1 = e.x + (e.alongX ? 1 : 0), z1 = e.z + (e.alongX ? 0 : 1);
            int ya = vertexY(level, e.x, e.z, heightsRefY), yb = vertexY(level, x1, z1, heightsRefY);
            float r = ((e.color >> 16) & 255) / 255F, g = ((e.color >> 8) & 255) / 255F, b = (e.color & 255) / 255F;
            line(vc, m, nm, cp, e.x, ya + RAIL_LOW, e.z, x1, yb + RAIL_LOW, z1, r, g, b, 0.95F * fade);
            line(vc, m, nm, cp, e.x, ya + RAIL_HIGH, e.z, x1, yb + RAIL_HIGH, z1, r, g, b, 0.5F * fade);
        }
        for (Corner c : corners) {
            double dist = Math.sqrt((c.x - px) * (c.x - px) + (c.z - pz) * (c.z - pz));
            if (dist > RANGE) continue;
            float fade = (float) Math.min(1, (RANGE - dist) / 24.0);
            int y = vertexY(level, c.x, c.z, heightsRefY);
            float r = ((c.color >> 16) & 255) / 255F, g = ((c.color >> 8) & 255) / 255F, b = (c.color & 255) / 255F;
            line(vc, m, nm, cp, c.x, y + RAIL_LOW, c.z, c.x, y + POST, c.z, r, g, b, fade);
            line(vc, m, nm, cp, c.x + 0.03, y + RAIL_LOW, c.z + 0.03, c.x + 0.03, y + POST, c.z + 0.03, r, g, b, fade);
        }
        buf.endBatch(RenderType.lines());
    }

    /** 카메라 기준 좌표로 바꿔 넣는다 (좌표가 수만 단위여도 떨리지 않게 뺄셈은 double 로). */
    private static void line(VertexConsumer vc, Matrix4f m, Matrix3f nm, Vec3 cp, double x0, double y0, double z0, double x1, double y1, double z1,
                             float r, float g, float b, float a) {
        float ax = (float) (x0 - cp.x), ay = (float) (y0 - cp.y), az = (float) (z0 - cp.z);
        float bx = (float) (x1 - cp.x), by = (float) (y1 - cp.y), bz = (float) (z1 - cp.z);
        float dx = bx - ax, dy = by - ay, dz = bz - az;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-4) return;
        dx /= len;
        dy /= len;
        dz /= len;
        vc.vertex(m, ax, ay, az).color(r, g, b, a).normal(nm, dx, dy, dz).endVertex();
        vc.vertex(m, bx, by, bz).color(r, g, b, a).normal(nm, dx, dy, dz).endVertex();
    }
}

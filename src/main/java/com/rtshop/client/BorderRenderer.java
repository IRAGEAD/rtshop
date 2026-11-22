package com.rtshop.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.rtshop.RtShop;
import com.rtshop.land.Borders;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
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
 * 토지 경계를 청크 경계(F3+G)처럼 선으로 그린다: 모서리는 굵고 밝은 세로선, 벽면은 2칸 간격의 옅은 격자.
 * 위아래로는 플레이어 근처만 (토지가 하늘 끝까지 이어지므로).
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID, value = Dist.CLIENT)
public final class BorderRenderer {
    private BorderRenderer() {
    }

    /** 경계의 한 변: (ax,az)-(bx,bz) 직선. */
    private record Seg(int ax, int az, int bx, int bz, int y0, int y1, int color) {
    }

    private record Corner(int x, int z, int y0, int y1, int color) {
    }

    private static List<Seg> segs = List.of();
    private static List<Corner> corners = List.of();
    private static long until;

    public static void set(List<Borders.Shape> shapes) {
        List<Seg> ss = new ArrayList<>();
        List<Corner> cs = new ArrayList<>();
        for (Borders.Shape s : shapes) {
            build(s, ss, cs);
        }
        segs = ss;
        corners = cs;
        until = shapes.isEmpty() ? 0 : System.currentTimeMillis() + 1600;
    }

    private static boolean in(Borders.Shape s, int x, int z) {
        if (x < s.x0 || x > s.x1 || z < s.z0 || z > s.z1) return false;
        return s.mask == null || s.mask.get((z - s.z0) * (s.x1 - s.x0 + 1) + (x - s.x0));
    }

    private static void build(Borders.Shape s, List<Seg> out, List<Corner> cs) {
        int c = s.color;
        // 북·남 변 (가로줄 z 고정)
        for (int z = s.z0; z <= s.z1 + 1; z++) {
            int run = Integer.MIN_VALUE;
            for (int x = s.x0; x <= s.x1 + 1; x++) {
                boolean edge = x <= s.x1 && (in(s, x, z) != in(s, x, z - 1));
                if (edge && run == Integer.MIN_VALUE) run = x;
                if (!edge && run != Integer.MIN_VALUE) {
                    if (!((s.open & 4) != 0 && z == s.z0) && !((s.open & 8) != 0 && z == s.z1 + 1))
                        out.add(new Seg(run, z, x, z, s.y0, s.y1 + 1, c));
                    run = Integer.MIN_VALUE;
                }
            }
        }
        // 서·동 변 (세로줄 x 고정)
        for (int x = s.x0; x <= s.x1 + 1; x++) {
            int run = Integer.MIN_VALUE;
            for (int z = s.z0; z <= s.z1 + 1; z++) {
                boolean edge = z <= s.z1 && (in(s, x, z) != in(s, x - 1, z));
                if (edge && run == Integer.MIN_VALUE) run = z;
                if (!edge && run != Integer.MIN_VALUE) {
                    if (!((s.open & 1) != 0 && x == s.x0) && !((s.open & 2) != 0 && x == s.x1 + 1))
                        out.add(new Seg(x, run, x, z, s.y0, s.y1 + 1, c));
                    run = Integer.MIN_VALUE;
                }
            }
        }
        // 모서리: 둘레가 꺾이는 점 (네 칸 중 안쪽이 1개 또는 3개, 또는 대각선 2개)
        for (int z = s.z0; z <= s.z1 + 1; z++) {
            for (int x = s.x0; x <= s.x1 + 1; x++) {
                boolean a = in(s, x - 1, z - 1), b = in(s, x, z - 1), d = in(s, x - 1, z), e = in(s, x, z);
                int n = (a ? 1 : 0) + (b ? 1 : 0) + (d ? 1 : 0) + (e ? 1 : 0);
                boolean openSide = ((s.open & 1) != 0 && x == s.x0) || ((s.open & 2) != 0 && x == s.x1 + 1)
                        || ((s.open & 4) != 0 && z == s.z0) || ((s.open & 8) != 0 && z == s.z1 + 1);
                if (!openSide && (n == 1 || n == 3 || (n == 2 && a == e))) cs.add(new Corner(x, z, s.y0, s.y1 + 1, c));
            }
        }
    }

    public static boolean activeForTest() {
        return System.currentTimeMillis() < until && !segs.isEmpty();
    }

    public static int segmentsForTest() {
        return segs.size();
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || System.currentTimeMillis() > until || segs.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        Camera cam = event.getCamera();
        Vec3 cp = cam.getPosition();
        PoseStack ps = event.getPoseStack();
        ps.pushPose();
        ps.translate(-cp.x, -cp.y, -cp.z);
        Matrix4f m = ps.last().pose();
        Matrix3f nm = ps.last().normal();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buf.getBuffer(RenderType.lines());
        double py = mc.player.getY();
        int lo = (int) Math.floor(py) - 10, hi = (int) Math.floor(py) + 18;
        for (Seg s : segs) {
            int ya = Math.max(s.y0, lo), yb = Math.min(s.y1, hi);
            if (yb <= ya) continue;
            float r = ((s.color >> 16) & 255) / 255F, g = ((s.color >> 8) & 255) / 255F, b = (s.color & 255) / 255F;
            // 가로줄: 2칸마다 옅게, 위·아래 끝은 진하게
            for (int y = ya; y <= yb; y += 2) {
                float a = (y == ya || y + 2 > yb) ? 0.9F : 0.35F;
                line(vc, m, nm, s.ax, y + 0.01, s.az, s.bx, y + 0.01, s.bz, r, g, b, a);
            }
            // 세로줄: 2칸마다 옅게
            boolean alongX = s.az == s.bz;
            int from = alongX ? s.ax : s.az, to = alongX ? s.bx : s.bz;
            for (int k = from; k <= to; k++) {
                if (Math.floorMod(k, 2) != 0) continue;
                double x = alongX ? k : s.ax, z = alongX ? s.az : k;
                line(vc, m, nm, x, ya, z, x, yb, z, r, g, b, 0.3F);
            }
        }
        for (Corner c : corners) {
            int ya = Math.max(c.y0, lo - 6), yb = Math.min(c.y1, hi + 6);
            if (yb <= ya) continue;
            float r = ((c.color >> 16) & 255) / 255F, g = ((c.color >> 8) & 255) / 255F, b = (c.color & 255) / 255F;
            line(vc, m, nm, c.x, ya, c.z, c.x, yb, c.z, r, g, b, 1F);
            line(vc, m, nm, c.x + 0.02, ya, c.z + 0.02, c.x + 0.02, yb, c.z + 0.02, r, g, b, 1F);
        }
        buf.endBatch(RenderType.lines());
        ps.popPose();
    }

    private static void line(VertexConsumer vc, Matrix4f m, Matrix3f nm, double x0, double y0, double z0, double x1, double y1, double z1,
                             float r, float g, float b, float a) {
        float dx = (float) (x1 - x0), dy = (float) (y1 - y0), dz = (float) (z1 - z0);
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-4) return;
        dx /= len;
        dy /= len;
        dz /= len;
        vc.vertex(m, (float) x0, (float) y0, (float) z0).color(r, g, b, a).normal(nm, dx, dy, dz).endVertex();
        vc.vertex(m, (float) x1, (float) y1, (float) z1).color(r, g, b, a).normal(nm, dx, dy, dz).endVertex();
    }
}

/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.PoseStack$Pose
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.math.Axis
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Vec3i
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleTypes
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.EnchantmentTableBlock
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Vector3f
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dyslexic.cinematic.Cinematic;
import dev.dyslexic.cinematic.client.Fx;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantmentTableBlock;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class EnchantFx {
    public static final ResourceLocation CIRCLE = Cinematic.id("textures/fx/magic_circle.png");
    public static final ResourceLocation PIXEL_CIRCLE = Cinematic.id("textures/fx/enchant_circle_pixel.png");
    public static final ResourceLocation GLOW = Cinematic.id("textures/fx/glow.png");
    public static final ResourceLocation BEAM = Cinematic.id("textures/fx/beam.png");
    private static final RenderType SURFACE_CIRCLE = EnchantFx.noDepthWrite(RenderType.m_110454_((ResourceLocation)PIXEL_CIRCLE, (boolean)false));
    public static final double LAPIS_RADIUS = 0.115;
    private static final double LAPIS_RIGHT = 0.44;
    private static final double LAPIS_NEAR = 0.18;
    private static final double LAPIS_LIFT = 0.27;
    private static final RandomSource RANDOM = RandomSource.m_216327_();
    private static final float[][] QUAD_UV = new float[][]{{0.0f, 0.0f}, {1.0f, 0.0f}, {1.0f, 1.0f}, {0.0f, 1.0f}};
    private static final int[][] BOTH_WAYS = new int[][]{{0, 1, 2, 3}, {3, 2, 1, 0}};
    private static final int[][] SURFACE_SIDE = new int[][]{{3, 2, 1, 0}};
    public static final double OUTER_LIFT = 0.012;
    public static final double INNER_LIFT = 0.02;
    public static final double HINT_LIFT = 0.028;
    public static final double SOCKET_LIFT = 0.016;
    private static final float[][] PALETTE = new float[][]{{0.75f, 0.45f, 1.0f}, {0.45f, 0.8f, 1.0f}, {1.0f, 0.92f, 1.0f}};

    private EnchantFx() {
    }

    private static RenderType noDepthWrite(RenderType original) {
        return new RenderType("cinematic_enchant_surface", original.m_110508_(), original.m_173186_(), original.m_110507_(), false, true, () -> {
            original.m_110185_();
            RenderSystem.depthMask((boolean)false);
        }, () -> {
            original.m_110188_();
            RenderSystem.depthMask((boolean)true);
        }){};
    }

    public static Vec3 top(BlockPos pos) {
        return new Vec3((double)pos.m_123341_() + 0.5, (double)pos.m_123342_() + 0.75, (double)pos.m_123343_() + 0.5);
    }

    public static Vec3 itemCenter(BlockPos pos, Direction side, double clock) {
        return EnchantFx.top(pos).m_82520_(0.0, 0.62 + Math.sin(clock * 1.6) * 0.025, 0.0);
    }

    public static Vec3 lapisCorner(BlockPos pos, Direction side) {
        Vec3 out = Vec3.m_82528_((Vec3i)side.m_122436_());
        Vec3 right = Vec3.m_82528_((Vec3i)side.m_122424_().m_122427_().m_122436_());
        return EnchantFx.top(pos).m_82549_(out.m_82490_(0.18)).m_82549_(right.m_82490_(0.44)).m_82520_(0.0, 0.27, 0.0);
    }

    public static void lapisGhost(PoseStack pose, MultiBufferSource buffers, Camera camera, Vec3 at, float brightness) {
        EnchantFx.lapisGhost(pose, buffers, camera, at, brightness, 1.0f, 0.0f);
    }

    public static void lapisGhost(PoseStack pose, MultiBufferSource buffers, Camera camera, Vec3 at, float brightness, float scale, float rotation) {
        Vec3 pivot = at.m_82546_(camera.m_90583_());
        pose.m_85836_();
        pose.m_85837_(pivot.f_82479_, pivot.f_82480_, pivot.f_82481_);
        pose.m_252781_(Axis.f_252436_.m_252977_(rotation));
        pose.m_85841_(scale, scale, scale);
        pose.m_85837_(-pivot.f_82479_, -pivot.f_82480_, -pivot.f_82481_);
        double[][] gem = new double[][]{{0.0, 0.052}, {0.035, 0.018}, {0.023, -0.032}, {-0.023, -0.032}, {-0.035, 0.018}};
        VertexConsumer vc = buffers.m_6299_(RenderType.m_110502_());
        for (int i = 0; i < gem.length; ++i) {
            double[] a = gem[i];
            double[] b = gem[(i + 1) % gem.length];
            EnchantFx.crystalLine(vc, pose.m_85850_(), camera, at, a[0], a[1], b[0], b[1], brightness);
        }
        EnchantFx.crystalLine(vc, pose.m_85850_(), camera, at, -0.035, 0.018, 0.035, 0.018, brightness * 0.65f);
        EnchantFx.crystalLine(vc, pose.m_85850_(), camera, at, 0.0, 0.052, -0.023, -0.032, brightness * 0.65f);
        EnchantFx.crystalLine(vc, pose.m_85850_(), camera, at, 0.0, 0.052, 0.023, -0.032, brightness * 0.65f);
        pose.m_85849_();
    }

    private static void crystalLine(VertexConsumer vc, PoseStack.Pose pose, Camera camera, Vec3 at, double x0, double y0, double x1, double y1, float brightness) {
        double length = Math.hypot(x1 - x0, y1 - y0);
        double dx = -(y1 - y0) / length * 0.0025;
        double dy = (x1 - x0) / length * 0.0025;
        for (int edge = -1; edge <= 1; edge += 2) {
            int[][] windings;
            double[][] ribbon = new double[][]{{x0, y0}, {x1, y1}, {x1 + dx * (double)edge, y1 + dy * (double)edge}, {x0 + dx * (double)edge, y0 + dy * (double)edge}};
            int[][] nArrayArray = windings = new int[][]{{0, 1, 2, 3}, {3, 2, 1, 0}};
            int n = nArrayArray.length;
            for (int i = 0; i < n; ++i) {
                int[] winding;
                for (int vertex : winding = nArrayArray[i]) {
                    EnchantFx.crystalVertex(vc, pose, camera, at, ribbon[vertex][0], ribbon[vertex][1], brightness, vertex < 2 ? 190 : 0);
                }
            }
        }
    }

    private static void crystalVertex(VertexConsumer vc, PoseStack.Pose pose, Camera camera, Vec3 at, double x, double y, float brightness, int alpha) {
        Vector3f left = camera.m_252775_();
        Vector3f up = camera.m_253028_();
        Vec3 cam = camera.m_90583_();
        vc.m_252986_(pose.m_252922_(), (float)(at.f_82479_ + (double)left.x() * x + (double)up.x() * y - cam.f_82479_), (float)(at.f_82480_ + (double)left.y() * x + (double)up.y() * y - cam.f_82480_), (float)(at.f_82481_ + (double)left.z() * x + (double)up.z() * y - cam.f_82481_)).m_6122_(EnchantFx.clamp(0.25f * brightness), EnchantFx.clamp(0.62f * brightness), EnchantFx.clamp(brightness), alpha).m_5752_();
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, Vec3[] c, float r, float g, float b, Vec3 normal) {
        int ri = EnchantFx.clamp(r);
        int gi = EnchantFx.clamp(g);
        int bi = EnchantFx.clamp(b);
        int[][] nArray = BOTH_WAYS;
        int n = nArray.length;
        for (int i = 0; i < n; ++i) {
            int[] order;
            for (int i2 : order = nArray[i]) {
                vc.m_252986_(pose.m_252922_(), (float)c[i2].f_82479_, (float)c[i2].f_82480_, (float)c[i2].f_82481_).m_6122_(ri, gi, bi, 255).m_7421_(QUAD_UV[i2][0], QUAD_UV[i2][1]).m_86008_(OverlayTexture.f_118083_).m_85969_(0xF000F0).m_252939_(pose.m_252943_(), (float)normal.f_82479_, (float)normal.f_82480_, (float)normal.f_82481_).m_5752_();
            }
        }
    }

    private static void gradientQuad(VertexConsumer vc, PoseStack.Pose pose, Vec3[] corners, CirclePalette palette, float brightness, boolean surface) {
        float presence = Math.max(0.0f, Math.min(1.0f, brightness / 0.4f));
        int alpha = EnchantFx.clamp(presence * presence * (3.0f - 2.0f * presence));
        if (alpha == 0) {
            return;
        }
        int[][] nArray = surface ? SURFACE_SIDE : BOTH_WAYS;
        int n = nArray.length;
        for (int i = 0; i < n; ++i) {
            int[] order;
            for (int i2 : order = nArray[i]) {
                int rgb = i2 < 2 ? palette.top : palette.bottom;
                vc.m_252986_(pose.m_252922_(), (float)corners[i2].f_82479_, (float)corners[i2].f_82480_, (float)corners[i2].f_82481_).m_6122_(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, alpha).m_7421_(QUAD_UV[i2][0], QUAD_UV[i2][1]).m_86008_(OverlayTexture.f_118083_).m_85969_(0xF000F0).m_252939_(pose.m_252943_(), 0.0f, 1.0f, 0.0f).m_5752_();
            }
        }
    }

    private static int clamp(float v) {
        return Math.max(0, Math.min(255, Math.round(v * 255.0f)));
    }

    public static void circle(PoseStack pose, MultiBufferSource buffers, Vec3 cam, Vec3 center, double radius, double angle, CirclePalette palette, float brightness) {
        Vec3[] corners = new Vec3[4];
        for (int i = 0; i < 4; ++i) {
            double a = angle + 0.7853981633974483 + (double)i * Math.PI / 2.0;
            double d = radius * Math.sqrt(2.0);
            corners[i] = new Vec3(center.f_82479_ + Math.cos(a) * d - cam.f_82479_, center.f_82480_ - cam.f_82480_, center.f_82481_ + Math.sin(a) * d - cam.f_82481_);
        }
        EnchantFx.gradientQuad(buffers.m_6299_(SURFACE_CIRCLE), pose.m_85850_(), corners, palette, brightness, true);
    }

    public static void circleBillboard(PoseStack pose, MultiBufferSource buffers, Camera camera, Vec3 center, double radius, double angle, CirclePalette palette, float brightness) {
        Vector3f left = camera.m_252775_();
        Vector3f up = camera.m_253028_();
        Vec3 cam = camera.m_90583_();
        Vec3[] corners = new Vec3[4];
        for (int i = 0; i < 4; ++i) {
            double a = angle + 0.7853981633974483 + (double)i * Math.PI / 2.0;
            double x = Math.cos(a) * radius * Math.sqrt(2.0);
            double y = Math.sin(a) * radius * Math.sqrt(2.0);
            corners[i] = new Vec3(center.f_82479_ + (double)left.x() * x + (double)up.x() * y - cam.f_82479_, center.f_82480_ + (double)left.y() * x + (double)up.y() * y - cam.f_82480_, center.f_82481_ + (double)left.z() * x + (double)up.z() * y - cam.f_82481_);
        }
        EnchantFx.gradientQuad(buffers.m_6299_(RenderType.m_110454_((ResourceLocation)PIXEL_CIRCLE, (boolean)false)), pose.m_85850_(), corners, palette, brightness, false);
    }

    public static void disc(PoseStack pose, MultiBufferSource buffers, Vec3 cam, Vec3 center, double radius, double angle, ResourceLocation texture, float r, float g, float b) {
        Vec3[] c = new Vec3[4];
        for (int i = 0; i < 4; ++i) {
            double a = angle + 0.7853981633974483 + (double)i * Math.PI / 2.0;
            double d = radius * Math.sqrt(2.0);
            c[i] = new Vec3(center.f_82479_ + Math.cos(a) * d - cam.f_82479_, center.f_82480_ - cam.f_82480_, center.f_82481_ + Math.sin(a) * d - cam.f_82481_);
        }
        EnchantFx.quad(buffers.m_6299_(RenderType.m_110488_((ResourceLocation)texture)), pose.m_85850_(), c, r, g, b, new Vec3(0.0, 1.0, 0.0));
    }

    public static void billboard(PoseStack pose, MultiBufferSource buffers, Camera camera, Vec3 center, double radius, double angle, ResourceLocation texture, float r, float g, float b) {
        Vector3f left = camera.m_252775_();
        Vector3f up = camera.m_253028_();
        Vec3 cam = camera.m_90583_();
        Vec3[] c = new Vec3[4];
        for (int i = 0; i < 4; ++i) {
            double a = angle + 0.7853981633974483 + (double)i * Math.PI / 2.0;
            double x = Math.cos(a) * radius * Math.sqrt(2.0);
            double y = Math.sin(a) * radius * Math.sqrt(2.0);
            c[i] = new Vec3(center.f_82479_ + (double)left.x() * x + (double)up.x() * y - cam.f_82479_, center.f_82480_ + (double)left.y() * x + (double)up.y() * y - cam.f_82480_, center.f_82481_ + (double)left.z() * x + (double)up.z() * y - cam.f_82481_);
        }
        Vector3f look = camera.m_253058_();
        EnchantFx.quad(buffers.m_6299_(RenderType.m_110488_((ResourceLocation)texture)), pose.m_85850_(), c, r, g, b, new Vec3((double)(-look.x()), (double)(-look.y()), (double)(-look.z())));
    }

    public static void beam(PoseStack pose, MultiBufferSource buffers, Vec3 cam, Vec3 base, double height, double width, double spin, float r, float g, float b) {
        VertexConsumer vc = buffers.m_6299_(RenderType.m_110488_((ResourceLocation)BEAM));
        for (int plane = 0; plane < 2; ++plane) {
            double a = spin + (double)plane * Math.PI / 2.0;
            double dx = Math.cos(a) * width / 2.0;
            double dz = Math.sin(a) * width / 2.0;
            Vec3[] c = new Vec3[]{new Vec3(base.f_82479_ - dx - cam.f_82479_, base.f_82480_ + height - cam.f_82480_, base.f_82481_ - dz - cam.f_82481_), new Vec3(base.f_82479_ + dx - cam.f_82479_, base.f_82480_ + height - cam.f_82480_, base.f_82481_ + dz - cam.f_82481_), new Vec3(base.f_82479_ + dx - cam.f_82479_, base.f_82480_ - cam.f_82480_, base.f_82481_ + dz - cam.f_82481_), new Vec3(base.f_82479_ - dx - cam.f_82479_, base.f_82480_ - cam.f_82480_, base.f_82481_ - dz - cam.f_82481_)};
            EnchantFx.quad(vc, pose.m_85850_(), c, r, g, b, new Vec3(Math.sin(a), 0.0, -Math.cos(a)));
        }
    }

    public static void stream(Vec3 from, Vec3 to, int count) {
        ClientLevel level = Minecraft.m_91087_().f_91073_;
        if (level == null) {
            return;
        }
        for (int i = 0; i < count; ++i) {
            double jx = (RANDOM.m_188500_() - 0.5) * 0.5;
            double jy = (RANDOM.m_188500_() - 0.5) * 0.4;
            double jz = (RANDOM.m_188500_() - 0.5) * 0.5;
            level.m_7106_((ParticleOptions)ParticleTypes.f_123809_, to.f_82479_, to.f_82480_, to.f_82481_, from.f_82479_ - to.f_82479_ + jx, from.f_82480_ - to.f_82480_ + jy, from.f_82481_ - to.f_82481_ + jz);
        }
    }

    public static void spark(Vec3 at, Vec3 velocity, double life, float size) {
        float[] c = PALETTE[RANDOM.m_188503_(PALETTE.length)];
        Fx.spark(at, velocity, life, size, c[0], c[1], c[2], -0.04, 0.0);
    }

    public static void burst(Vec3 at, double strength) {
        int i;
        for (int i2 = 0; i2 < (int)(70.0 * strength); ++i2) {
            Vec3 dir = new Vec3(RANDOM.m_188583_(), RANDOM.m_188583_() * 0.7 + 0.25, RANDOM.m_188583_()).m_82541_();
            EnchantFx.spark(at, dir.m_82490_(0.35 + RANDOM.m_188500_() * 1.0), 0.6 + RANDOM.m_188500_() * 0.8, 0.018f + RANDOM.m_188501_() * 0.03f);
        }
        ClientLevel level = Minecraft.m_91087_().f_91073_;
        if (level == null) {
            return;
        }
        for (i = 0; i < (int)(8.0 * strength); ++i) {
            level.m_7106_((ParticleOptions)ParticleTypes.f_123789_, at.f_82479_ + (RANDOM.m_188500_() - 0.5) * 0.3, at.f_82480_ + (RANDOM.m_188500_() - 0.5) * 0.3, at.f_82481_ + (RANDOM.m_188500_() - 0.5) * 0.3, (RANDOM.m_188500_() - 0.5) * 0.05, RANDOM.m_188500_() * 0.04, (RANDOM.m_188500_() - 0.5) * 0.05);
        }
        for (i = 0; i < (int)(30.0 * strength); ++i) {
            Vec3 dir = new Vec3(RANDOM.m_188583_(), RANDOM.m_188583_(), RANDOM.m_188583_()).m_82541_().m_82490_(1.1 + RANDOM.m_188500_() * 0.6);
            level.m_7106_((ParticleOptions)ParticleTypes.f_123809_, at.f_82479_, at.f_82480_, at.f_82481_, dir.f_82479_, dir.f_82480_, dir.f_82481_);
        }
    }

    public static void sparkle(Vec3 at, double radius) {
        double a = RANDOM.m_188500_() * Math.PI * 2.0;
        double r = radius * (0.6 + RANDOM.m_188500_() * 0.4);
        EnchantFx.spark(at.m_82520_(Math.cos(a) * r, (RANDOM.m_188500_() - 0.5) * 0.2, Math.sin(a) * r), new Vec3(0.0, 0.05 + RANDOM.m_188500_() * 0.05, 0.0), 1.0 + RANDOM.m_188500_() * 0.6, 0.012f + RANDOM.m_188501_() * 0.012f);
    }

    public static void gather(Vec3 at, double radius) {
        Vec3 dir = new Vec3(RANDOM.m_188583_(), RANDOM.m_188583_(), RANDOM.m_188583_()).m_82541_();
        double life = 0.45 + RANDOM.m_188500_() * 0.2;
        EnchantFx.spark(at.m_82549_(dir.m_82490_(radius)), dir.m_82490_(-radius / life * 1.6), life, 0.014f + RANDOM.m_188501_() * 0.012f);
    }

    public static List<Vec3> shelves(ClientLevel level, BlockPos pos) {
        ArrayList<Vec3> list = new ArrayList<Vec3>();
        for (BlockPos offset : EnchantmentTableBlock.f_207902_) {
            if (!EnchantmentTableBlock.m_207909_((Level)level, (BlockPos)pos, (BlockPos)offset)) continue;
            list.add(Vec3.m_82512_((Vec3i)pos.m_121955_((Vec3i)offset)));
        }
        return list;
    }

    public static enum CirclePalette {
        VIOLET(11206905, 12936959),
        RED(10694715, 13587284),
        LAPIS(3897561, 10275071);

        final int top;
        final int bottom;

        private CirclePalette(int top, int bottom) {
            this.top = top;
            this.bottom = bottom;
        }
    }
}


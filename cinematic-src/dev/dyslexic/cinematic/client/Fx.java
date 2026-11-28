/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.PoseStack$Pose
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.GameRenderer
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.client.renderer.texture.TextureAtlas
 *  net.minecraft.client.renderer.texture.TextureAtlasSprite
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix4f
 *  org.joml.Vector3f
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.dyslexic.cinematic.client.EnchantFx;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class Fx {
    static final RandomSource RANDOM = RandomSource.m_216327_();
    private static final ResourceLocation WHITE = new ResourceLocation("textures/misc/white.png");
    public static final RenderType HIGHLIGHT = new RenderType("cinematic_highlight", DefaultVertexFormat.f_85815_, VertexFormat.Mode.QUADS, 1536, false, true, () -> {
        RenderSystem.setShader(GameRenderer::m_172811_);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.enablePolygonOffset();
        RenderSystem.polygonOffset((float)-1.0f, (float)-10.0f);
    }, () -> {
        RenderSystem.polygonOffset((float)0.0f, (float)0.0f);
        RenderSystem.disablePolygonOffset();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }){};
    private static final List<Spark> SPARKS = new ArrayList<Spark>();
    private static final int MAX_SPARKS = 900;
    private static final float[][] QUAD_UV = new float[][]{{0.0f, 0.0f}, {1.0f, 0.0f}, {1.0f, 1.0f}, {0.0f, 1.0f}};
    private static final int[][] BOTH_WAYS = new int[][]{{0, 1, 2, 3}, {3, 2, 1, 0}};

    private Fx() {
    }

    static RenderType flatGlow() {
        return RenderType.m_110488_((ResourceLocation)WHITE);
    }

    static void glowVertex(VertexConsumer vc, Matrix4f m, float x, float y, float z, int r, int g, int b, Vec3 normal) {
        vc.m_252986_(m, x, y, z).m_6122_(r, g, b, 255).m_7421_(0.5f, 0.5f).m_86008_(OverlayTexture.f_118083_).m_85969_(0xF000F0).m_5601_((float)normal.f_82479_, (float)normal.f_82480_, (float)normal.f_82481_).m_5752_();
    }

    public static void spark(Vec3 at, Vec3 velocity, double life, float size, float r, float g, float b, double gravity, double streak) {
        if (SPARKS.size() >= 900) {
            return;
        }
        Spark s = new Spark();
        s.phase = SPARKS.size();
        s.x = at.f_82479_;
        s.y = at.f_82480_;
        s.z = at.f_82481_;
        s.vx = velocity.f_82479_;
        s.vy = velocity.f_82480_;
        s.vz = velocity.f_82481_;
        s.life = life;
        s.size = size;
        s.r = r;
        s.g = g;
        s.b = b;
        s.gravity = gravity;
        s.streak = streak;
        SPARKS.add(s);
    }

    public static void renderSparks(PoseStack pose, MultiBufferSource buffers, Camera camera, double dt) {
        for (int i = SPARKS.size() - 1; i >= 0; --i) {
            Spark s = SPARKS.get(i);
            s.age += dt;
            if (s.age >= s.life) {
                int last = SPARKS.size() - 1;
                SPARKS.set(i, SPARKS.get(last));
                SPARKS.remove(last);
                continue;
            }
            double drag = Math.exp((s.streak > 0.0 ? -0.9 : -2.2) * dt);
            s.vx *= drag;
            s.vy = s.vy * drag - s.gravity * dt;
            s.vz *= drag;
            s.x += s.vx * dt;
            s.y += s.vy * dt;
            s.z += s.vz * dt;
            if (s.streak > 0.0) {
                float k = (float)Math.pow(1.0 - s.age / s.life, 1.4);
                Vec3 head = new Vec3(s.x, s.y, s.z);
                Vec3 tail = head.m_82492_(s.vx * s.streak, s.vy * s.streak, s.vz * s.streak);
                Fx.streak(pose, buffers, camera, head, tail, s.size, s.r * k, s.g * k * k, s.b * k * k * k);
                continue;
            }
            float fade = (float)Math.sin(Math.PI * s.age / s.life);
            float twinkle = 0.75f + 0.25f * (float)Math.sin(s.age * 23.0 + (double)s.phase);
            float k = fade * twinkle;
            EnchantFx.billboard(pose, buffers, camera, new Vec3(s.x, s.y, s.z), (double)s.size * (0.6 + 0.4 * (double)fade), 0.0, EnchantFx.GLOW, s.r * k, s.g * k, s.b * k);
        }
    }

    public static void clearSparks() {
        SPARKS.clear();
    }

    public static void shade(PoseStack pose, MultiBufferSource buffers, Camera camera, Vec3 center, double radius, float alpha) {
        Vector3f left = camera.m_252775_();
        Vector3f up = camera.m_253028_();
        Vec3 cam = camera.m_90583_();
        double r = radius * Math.sqrt(2.0);
        Vec3[] c = new Vec3[4];
        for (int i = 0; i < 4; ++i) {
            double a = 0.7853981633974483 + (double)i * Math.PI / 2.0;
            double x = Math.cos(a) * r;
            double y = Math.sin(a) * r;
            c[i] = new Vec3(center.f_82479_ + (double)left.x() * x + (double)up.x() * y - cam.f_82479_, center.f_82480_ + (double)left.y() * x + (double)up.y() * y - cam.f_82480_, center.f_82481_ + (double)left.z() * x + (double)up.z() * y - cam.f_82481_);
        }
        VertexConsumer vc = buffers.m_6299_(RenderType.m_110473_((ResourceLocation)EnchantFx.GLOW));
        Vector3f look = camera.m_253058_();
        int a = Fx.channel(alpha);
        int[][] nArray = BOTH_WAYS;
        int n = nArray.length;
        for (int i = 0; i < n; ++i) {
            int[] order;
            for (int i2 : order = nArray[i]) {
                vc.m_252986_(pose.m_85850_().m_252922_(), (float)c[i2].f_82479_, (float)c[i2].f_82480_, (float)c[i2].f_82481_).m_6122_(0, 0, 0, a).m_7421_(QUAD_UV[i2][0], QUAD_UV[i2][1]).m_86008_(OverlayTexture.f_118083_).m_85969_(0xF000F0).m_252939_(pose.m_85850_().m_252943_(), -look.x(), -look.y(), -look.z()).m_5752_();
            }
        }
    }

    public static void facePatch(PoseStack pose, MultiBufferSource buffers, Vec3 cam, Vec3 center, Vec3 u, Vec3 v, double half, float r, float g, float b) {
        Vec3 du = u.m_82490_(half);
        Vec3 dv = v.m_82490_(half);
        Vec3[] c = new Vec3[]{center.m_82546_(du).m_82546_(dv).m_82546_(cam), center.m_82549_(du).m_82546_(dv).m_82546_(cam), center.m_82549_(du).m_82549_(dv).m_82546_(cam), center.m_82546_(du).m_82549_(dv).m_82546_(cam)};
        Fx.quad(buffers.m_6299_(RenderType.m_110488_((ResourceLocation)EnchantFx.GLOW)), pose.m_85850_(), c, r, g, b, u.m_82537_(v));
    }

    private static void streak(PoseStack pose, MultiBufferSource buffers, Camera camera, Vec3 head, Vec3 tail, double width, float r, float g, float b) {
        Vec3 cam = camera.m_90583_();
        Vec3 along = head.m_82546_(tail);
        if (along.m_82556_() < 1.0E-8) {
            along = new Vec3(0.0, 1.0E-4, 0.0);
        }
        Vec3 across = along.m_82537_(head.m_82546_(cam)).m_82541_().m_82490_(width);
        Vec3 ext = along.m_82541_().m_82490_(width * 0.8);
        Vec3 h = head.m_82549_(ext);
        Vec3 t = tail.m_82546_(ext);
        Vec3[] c = new Vec3[]{t.m_82549_(across).m_82546_(cam), h.m_82549_(across).m_82546_(cam), h.m_82546_(across).m_82546_(cam), t.m_82546_(across).m_82546_(cam)};
        Vector3f look = camera.m_253058_();
        Fx.quad(buffers.m_6299_(RenderType.m_110488_((ResourceLocation)EnchantFx.GLOW)), pose.m_85850_(), c, r, g, b, new Vec3((double)(-look.x()), (double)(-look.y()), (double)(-look.z())));
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, Vec3[] c, float r, float g, float b, Vec3 normal) {
        int ri = Fx.channel(r);
        int gi = Fx.channel(g);
        int bi = Fx.channel(b);
        int[][] nArray = BOTH_WAYS;
        int n = nArray.length;
        for (int i = 0; i < n; ++i) {
            int[] order;
            for (int i2 : order = nArray[i]) {
                vc.m_252986_(pose.m_252922_(), (float)c[i2].f_82479_, (float)c[i2].f_82480_, (float)c[i2].f_82481_).m_6122_(ri, gi, bi, 255).m_7421_(QUAD_UV[i2][0], QUAD_UV[i2][1]).m_86008_(OverlayTexture.f_118083_).m_85969_(0xF000F0).m_252939_(pose.m_252943_(), (float)normal.f_82479_, (float)normal.f_82480_, (float)normal.f_82481_).m_5752_();
            }
        }
    }

    private static int channel(float v) {
        return Math.max(0, Math.min(255, Math.round(v * 255.0f)));
    }

    private static double spread(double amount) {
        return (RANDOM.m_188500_() - 0.5) * 2.0 * amount;
    }

    public static void hotSparks(Vec3 at, Vec3 direction, double spreadAmount, int count, double speed) {
        for (int i = 0; i < count; ++i) {
            Vec3 dir = direction.m_82520_(Fx.spread(spreadAmount), Fx.spread(spreadAmount), Fx.spread(spreadAmount)).m_82541_();
            double v = speed * (0.55 + RANDOM.m_188500_() * 0.7);
            float warm = RANDOM.m_188501_();
            Fx.spark(at.m_82520_(Fx.spread(0.012), Fx.spread(0.012), Fx.spread(0.012)), dir.m_82490_(v), 0.2 + RANDOM.m_188500_() * 0.35, 0.0075f + RANDOM.m_188501_() * 0.006f, 1.0f, 0.78f + 0.2f * warm, 0.45f + 0.4f * warm, 4.5, 0.045);
        }
    }

    public static void blowSparks(Vec3 at, int count, double speed) {
        for (int i = 0; i < count; ++i) {
            double a = RANDOM.m_188500_() * Math.PI * 2.0;
            Vec3 dir = new Vec3(Math.cos(a), 0.35 + RANDOM.m_188500_() * 0.9, Math.sin(a));
            Fx.hotSparks(at, dir, 0.15, 1, speed);
        }
    }

    public static void flakes(Vec3 at, double radius, int count) {
        for (int i = 0; i < count; ++i) {
            Vec3 from = at.m_82520_(Fx.spread(radius), Fx.spread(radius * 0.4), Fx.spread(radius));
            Vec3 v = new Vec3(Fx.spread(0.25), 0.1 + RANDOM.m_188500_() * 0.2, Fx.spread(0.25));
            float pick = RANDOM.m_188501_();
            Fx.spark(from, v, 0.9 + RANDOM.m_188500_() * 0.7, 0.014f + RANDOM.m_188501_() * 0.014f, 0.75f + 0.25f * pick, 0.4f + 0.3f * pick, 1.0f, 0.5, 0.0);
        }
    }

    public static void fountain(Vec3 at, Vec3 lean, Vec3 across, int count, double speed) {
        for (int i = 0; i < count; ++i) {
            Vec3 dir = new Vec3(0.0, 1.0, 0.0).m_82549_(lean).m_82549_(across.m_82490_(Fx.spread(0.75)));
            Fx.hotSparks(at, dir, 0.18, 1, speed);
        }
    }

    public static void motes(Vec3 at, double radius, int count, float r, float g, float b, double rise) {
        for (int i = 0; i < count; ++i) {
            Fx.spark(at.m_82520_(Fx.spread(radius), Fx.spread(radius * 0.3), Fx.spread(radius)), new Vec3(Fx.spread(0.04), rise * (0.6 + RANDOM.m_188500_() * 0.6), Fx.spread(0.04)), 0.6 + RANDOM.m_188500_() * 0.6, 0.008f + RANDOM.m_188501_() * 0.01f, r, g, b, 0.0, 0.0);
        }
    }

    public static void steam(Vec3 at, double radius, int count) {
        for (int i = 0; i < count; ++i) {
            float k = 0.16f + RANDOM.m_188501_() * 0.1f;
            Fx.spark(at.m_82520_(Fx.spread(radius), 0.0, Fx.spread(radius)), new Vec3(Fx.spread(0.06), 0.18 + RANDOM.m_188500_() * 0.16, Fx.spread(0.06)), 0.9 + RANDOM.m_188500_() * 0.6, 0.028f + RANDOM.m_188501_() * 0.025f, k, k, k * 1.05f, -0.05, 0.0);
        }
    }

    public static float u(TextureAtlasSprite sprite, double fraction) {
        return sprite.m_118367_(fraction * 16.0);
    }

    public static float v(TextureAtlasSprite sprite, double fraction) {
        return sprite.m_118393_(fraction * 16.0);
    }

    public static TextureAtlasSprite sprite(String blockTexture) {
        return (TextureAtlasSprite)Minecraft.m_91087_().m_91258_(TextureAtlas.f_118259_).apply(new ResourceLocation(blockTexture));
    }

    public static VertexConsumer atlas(MultiBufferSource buffers) {
        return buffers.m_6299_(RenderType.m_110446_((ResourceLocation)TextureAtlas.f_118259_));
    }

    public static void box(PoseStack pose, VertexConsumer vc, Vec3 cam, Vec3 center, Vec3 ax, Vec3 ay, Vec3 az, TextureAtlasSprite sprite, float texels, int light) {
        Vec3 c = center.m_82546_(cam);
        Fx.face(pose, vc, c, ax, ay, az, sprite, texels, light);
        Fx.face(pose, vc, c, ax.m_82490_(-1.0), az, ay, sprite, texels, light);
        Fx.face(pose, vc, c, ay, az, ax, sprite, texels, light);
        Fx.face(pose, vc, c, ay.m_82490_(-1.0), ax, az, sprite, texels, light);
        Fx.face(pose, vc, c, az, ax, ay, sprite, texels, light);
        Fx.face(pose, vc, c, az.m_82490_(-1.0), ay, ax, sprite, texels, light);
    }

    private static void face(PoseStack pose, VertexConsumer vc, Vec3 c, Vec3 out, Vec3 u, Vec3 v, TextureAtlasSprite sprite, float texels, int light) {
        Vec3 n = out.m_82541_();
        if (u.m_82537_(v).m_82526_(n) < 0.0) {
            Vec3 t = u;
            u = v;
            v = t;
        }
        Vec3 o = c.m_82549_(out);
        Vec3[] p = new Vec3[]{o.m_82546_(u).m_82546_(v), o.m_82549_(u).m_82546_(v), o.m_82549_(u).m_82549_(v), o.m_82546_(u).m_82549_(v)};
        float t = texels / 16.0f;
        float[][] uv = new float[][]{{0.0f, t}, {t, t}, {t, 0.0f}, {0.0f, 0.0f}};
        for (int i = 0; i < 4; ++i) {
            vc.m_252986_(pose.m_85850_().m_252922_(), (float)p[i].f_82479_, (float)p[i].f_82480_, (float)p[i].f_82481_).m_6122_(255, 255, 255, 255).m_7421_(Fx.u(sprite, uv[i][0]), Fx.v(sprite, uv[i][1])).m_86008_(OverlayTexture.f_118083_).m_85969_(light).m_252939_(pose.m_85850_().m_252943_(), (float)n.f_82479_, (float)n.f_82480_, (float)n.f_82481_).m_5752_();
        }
    }

    private static final class Spark {
        double x;
        double y;
        double z;
        double vx;
        double vy;
        double vz;
        double age;
        double life;
        double gravity;
        double streak;
        float size;
        float r;
        float g;
        float b;
        int phase;

        private Spark() {
        }
    }
}


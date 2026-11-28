/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.util.FDColor
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.BufferBuilder
 *  com.mojang.blaze3d.vertex.BufferBuilder$RenderedBuffer
 *  com.mojang.blaze3d.vertex.BufferUploader
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.Tesselator
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.particle.Particle
 *  net.minecraft.client.particle.ParticleProvider
 *  net.minecraft.client.particle.ParticleRenderType
 *  net.minecraft.client.renderer.GameRenderer
 *  net.minecraft.client.renderer.texture.TextureManager
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Matrix4f
 *  org.joml.Vector4f
 */
package com.finderfeed.fdbosses.client.particles.arc_lightning;

import com.finderfeed.fdbosses.client.particles.arc_lightning.ArcLightningOptions;
import com.finderfeed.fdlib.util.FDColor;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class ArcLightningParticle
extends Particle {
    private ArcLightningOptions options;
    private Vec3 end;
    private Vec3 oldEnd;
    public static final ParticleRenderType RENDER_TYPE = new ParticleRenderType(){

        @Nullable
        public void m_6505_(BufferBuilder tesselator, TextureManager manager) {
            RenderSystem.depthMask((boolean)true);
            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.setShader(GameRenderer::m_172753_);
            RenderSystem.blendFunc((int)770, (int)771);
            tesselator.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85815_);
        }

        public void m_6294_(Tesselator tesselator) {
            tesselator.m_85914_();
        }

        public String toString() {
            return "ARC_LIGHTNING_RENDER_TYPE";
        }
    };

    public ArcLightningParticle(ArcLightningOptions options, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
        super(level, x, y, z, xd, yd, zd);
        this.f_107212_ = x;
        this.f_107213_ = y;
        this.f_107214_ = z;
        this.f_107215_ = xd;
        this.f_107216_ = yd;
        this.f_107217_ = zd;
        this.options = options;
        this.f_107225_ = options.lifetime;
        this.end = this.options.end;
        this.oldEnd = this.options.end;
        this.f_107219_ = false;
    }

    public void m_5989_() {
        super.m_5989_();
        this.oldEnd = this.end;
        this.end = this.end.m_82549_(this.options.endSpeed);
        this.m_107259_(new AABB(this.f_107212_, this.f_107213_, this.f_107214_, this.end.f_82479_, this.end.f_82480_, this.end.f_82481_));
    }

    public void m_5744_(VertexConsumer vertex, Camera camera, float partialTicks) {
        Vec3 pos = new Vec3(Mth.m_14139_((double)partialTicks, (double)this.f_107209_, (double)this.f_107212_), Mth.m_14139_((double)partialTicks, (double)this.f_107210_, (double)this.f_107213_), Mth.m_14139_((double)partialTicks, (double)this.f_107211_, (double)this.f_107214_)).m_82546_(camera.m_90583_());
        Vec3 endi = FDMathUtil.interpolateVectors((Vec3)this.oldEnd, (Vec3)this.end, (float)partialTicks);
        Vec3 between = endi.m_82546_(camera.m_90583_()).m_82546_(pos);
        double horLen = Math.sqrt(between.f_82479_ * between.f_82479_ + between.f_82481_ * between.f_82481_);
        double verticalLength = between.f_82480_;
        ArrayList<Vec3> positions = new ArrayList<Vec3>();
        positions.add(Vec3.f_82478_);
        int circleSegmentsAmount = 10;
        float circleOffset = this.options.circleOffset;
        Vec3 end = new Vec3(horLen, verticalLength, 0.0);
        Vec3 b = end.m_82541_();
        Vec3 center = end.m_82542_(0.5, 0.5, 0.0);
        Vec3 circleCenter = center.m_82549_(b.m_82535_(-1.5707964f).m_82542_((double)circleOffset, (double)circleOffset, 0.0));
        Vec3 v1 = end.m_82546_(circleCenter);
        Vec3 v2 = circleCenter.m_82548_();
        double angle = circleOffset >= 0.0f ? (Math.PI * 2 - FDMathUtil.angleBetweenVectors((Vec3)v1, (Vec3)v2)) / (double)circleSegmentsAmount : FDMathUtil.angleBetweenVectors((Vec3)v1, (Vec3)v2) / (double)circleSegmentsAmount;
        Matrix4f rot = new Matrix4f().identity().rotateZ(-((float)angle));
        Vector4f v = new Vector4f((float)v2.f_82479_, (float)v2.f_82480_, 0.0f, 1.0f);
        for (int i = 0; i < circleSegmentsAmount - 1; ++i) {
            v = rot.transform(v);
            v.x /= v.w;
            v.y /= v.w;
            v.w = 1.0f;
            positions.add(new Vec3((double)v.x + circleCenter.f_82479_, (double)v.y + circleCenter.f_82480_, 0.0));
        }
        positions.add(end);
        int lightningCounts = this.options.lightningSegments;
        List<Vec3> path = ArcLightningParticle.buildPath(this.f_107208_.m_46467_(), this.options.lightningRandomSpread, this.options.seed, lightningCounts, positions);
        Matrix4f mat = new Matrix4f();
        mat.translate((float)pos.f_82479_, (float)pos.f_82480_, (float)pos.f_82481_);
        mat.rotateY(-((float)Math.atan2(between.f_82481_, between.f_82479_)));
        FDColor color = this.options.color;
        ArcLightningParticle.drawLightning(mat, vertex, path, positions, this.options.lightningWidth, color.r, color.g, color.b, 1.0f);
        mat.translate(0.0f, 0.0f, 0.001f);
        ArcLightningParticle.drawLightning(mat, vertex, path, positions, this.options.lightningWidth * 0.15f, 1.0f, 1.0f, 1.0f, 1.0f);
        mat.translate(0.0f, 0.0f, -0.002f);
        ArcLightningParticle.drawLightning(mat, vertex, path, positions, this.options.lightningWidth * 0.15f, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    public static void fullLightningImmediateDraw(long time, int seed, int lightningBreakCount, Matrix4f transform, List<Vec3> path, float lightningWidth, float lightningRandomSpread, float r, float g, float b, float a) {
        Tesselator tesselator = RenderSystem.renderThreadTesselator();
        BufferBuilder vertexConsumer = Tesselator.m_85913_().m_85915_();
        RENDER_TYPE.m_6505_(vertexConsumer, Minecraft.m_91087_().m_91097_());
        List<Vec3> positions = ArcLightningParticle.buildPath(time, lightningRandomSpread, seed, lightningBreakCount, path);
        ArcLightningParticle.drawLightning(transform, (VertexConsumer)vertexConsumer, positions, path, lightningWidth, r, g, b, a);
        BufferUploader.m_231202_((BufferBuilder.RenderedBuffer)vertexConsumer.m_231175_());
    }

    public static void fullLightningDraw(long time, int seed, int lightningBreakCount, Matrix4f transform, VertexConsumer vertexConsumer, List<Vec3> path, float lightningWidth, float lightningRandomSpread, float r, float g, float b, float a) {
        List<Vec3> positions = ArcLightningParticle.buildPath(time, lightningRandomSpread, seed, lightningBreakCount, path);
        ArcLightningParticle.drawLightning(transform, vertexConsumer, positions, path, lightningWidth, r, g, b, a);
    }

    public static void drawLightning(Matrix4f transform, VertexConsumer vertex, List<Vec3> path, List<Vec3> positions, float lightningWidth, float r, float g, float b, float a) {
        Vec3 previousCenteredVector = new Vec3(0.0, 1.0, 0.0);
        Vec3 prevPoint = null;
        double previousw = 0.0;
        for (int i = 1; i < path.size() - 1; ++i) {
            Vec3 p1 = path.get(i - 1);
            Vec3 p2 = path.get(i);
            Vec3 p3 = path.get(i + 1);
            Vec3 v1 = p2.m_82546_(p1);
            Vec3 v2 = p3.m_82546_(p2);
            double dot = v1.m_82548_().m_82526_(v2);
            double angle = Math.acos(dot / (v1.m_82553_() * v2.m_82553_()));
            double sin = angle != 0.0 ? Math.sin(angle / 2.0) : 1.0;
            Vec3 v = ArcLightningParticle.findCenteredVector(v1, v2);
            if (v.m_82526_(previousCenteredVector) < 0.0) {
                v = v.m_82548_();
            }
            double w = (double)lightningWidth / sin;
            vertex.m_252986_(transform, (float)p1.f_82479_, (float)p1.f_82480_, 0.0f).m_85950_(r, g, b, i == 1 ? 0.0f : a).m_5752_();
            vertex.m_252986_(transform, (float)(p1.f_82479_ + previousCenteredVector.f_82479_ * previousw), (float)(p1.f_82480_ + previousCenteredVector.f_82480_ * previousw), 0.0f).m_85950_(r, g, b, 0.0f).m_5752_();
            vertex.m_252986_(transform, (float)(p2.f_82479_ + v.f_82479_ * w), (float)(p2.f_82480_ + v.f_82480_ * w), 0.0f).m_85950_(r, g, b, 0.0f).m_5752_();
            vertex.m_252986_(transform, (float)p2.f_82479_, (float)p2.f_82480_, 0.0f).m_85950_(r, g, b, a).m_5752_();
            vertex.m_252986_(transform, (float)p1.f_82479_, (float)p1.f_82480_, 0.0f).m_85950_(r, g, b, i == 1 ? 0.0f : a).m_5752_();
            vertex.m_252986_(transform, (float)(p1.f_82479_ - previousCenteredVector.f_82479_ * previousw), (float)(p1.f_82480_ - previousCenteredVector.f_82480_ * previousw), 0.0f).m_85950_(r, g, b, 0.0f).m_5752_();
            vertex.m_252986_(transform, (float)(p2.f_82479_ - v.f_82479_ * w), (float)(p2.f_82480_ - v.f_82480_ * w), 0.0f).m_85950_(r, g, b, 0.0f).m_5752_();
            vertex.m_252986_(transform, (float)p2.f_82479_, (float)p2.f_82480_, 0.0f).m_85950_(r, g, b, a).m_5752_();
            prevPoint = p2;
            previousw = w;
            previousCenteredVector = v;
        }
        Vec3 lastPos = positions.get(positions.size() - 1);
        vertex.m_252986_(transform, (float)prevPoint.f_82479_, (float)prevPoint.f_82480_, 0.0f).m_85950_(r, g, b, a).m_5752_();
        vertex.m_252986_(transform, (float)(prevPoint.f_82479_ + previousCenteredVector.f_82479_ * previousw), (float)(prevPoint.f_82480_ + previousCenteredVector.f_82480_ * previousw), 0.0f).m_85950_(r, g, b, 0.0f).m_5752_();
        vertex.m_252986_(transform, (float)lastPos.f_82479_, (float)lastPos.f_82480_, 0.0f).m_85950_(r, g, b, 0.0f).m_5752_();
        vertex.m_252986_(transform, (float)lastPos.f_82479_, (float)lastPos.f_82480_, 0.0f).m_85950_(r, g, b, a).m_5752_();
        vertex.m_252986_(transform, (float)prevPoint.f_82479_, (float)prevPoint.f_82480_, 0.0f).m_85950_(r, g, b, a).m_5752_();
        vertex.m_252986_(transform, (float)(prevPoint.f_82479_ - previousCenteredVector.f_82479_ * previousw), (float)(prevPoint.f_82480_ - previousCenteredVector.f_82480_ * previousw), 0.0f).m_85950_(r, g, b, 0.0f).m_5752_();
        vertex.m_252986_(transform, (float)lastPos.f_82479_, (float)lastPos.f_82480_, 0.0f).m_85950_(r, g, b, 0.0f).m_5752_();
        vertex.m_252986_(transform, (float)lastPos.f_82479_, (float)lastPos.f_82480_, 0.0f).m_85950_(r, g, b, a).m_5752_();
    }

    public static List<Vec3> buildPath(long time, float lightningRandomSpread, int seed, int lightningCounts, List<Vec3> positions) {
        float step = 1.0f / (float)lightningCounts;
        Random r = new Random(time * (long)seed);
        ArrayList<Vec3> path = new ArrayList<Vec3>();
        path.add(positions.get(0));
        float lightningSpread = lightningRandomSpread;
        for (float i = step; i <= 1.0f - step / 2.0f; i += step) {
            float gl = i * (float)(positions.size() - 1);
            float lp = gl - (float)((int)gl);
            Vec3 current = positions.get((int)gl);
            Vec3 next = positions.get((int)gl + 1);
            Vec3 b = next.m_82546_(current);
            Vec3 nb = b.m_82541_();
            float rmod = r.nextFloat() * lightningSpread * 2.0f - lightningSpread;
            Vec3 point = current.m_82549_(b.m_82542_((double)lp, (double)lp, 0.0)).m_82549_(nb.m_82535_(1.5707964f).m_82542_((double)rmod, (double)rmod, (double)rmod));
            path.add(point);
        }
        path.add(positions.get(positions.size() - 1));
        return path;
    }

    private static Vec3 getDirectionFromPositions(Vec3[] positions, int index) {
        if (index >= positions.length - 1) {
            return positions[positions.length - 1].m_82546_(positions[positions.length - 2]);
        }
        if (index < 0) {
            return positions[1].m_82546_(positions[0]);
        }
        return positions[index + 1].m_82546_(positions[index]);
    }

    private static Vec3 findCenteredVector(Vec3 v1, Vec3 v2) {
        v1 = v1.m_82548_().m_82541_();
        v2 = v2.m_82541_();
        return v1.m_82549_(v2).m_82541_();
    }

    public ParticleRenderType m_7556_() {
        return RENDER_TYPE;
    }

    public static class Factory
    implements ParticleProvider<ArcLightningOptions> {
        @Nullable
        public Particle createParticle(ArcLightningOptions options, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new ArcLightningParticle(options, level, x, y, z, xd, yd, zd);
        }
    }
}


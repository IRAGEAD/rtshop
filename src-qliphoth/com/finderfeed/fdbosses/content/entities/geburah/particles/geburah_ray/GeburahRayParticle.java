/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.util.math.ComplexEasingFunction
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.BufferBuilder
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.Tesselator
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  net.minecraft.client.Camera
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
package com.finderfeed.fdbosses.content.entities.geburah.particles.geburah_ray;

import com.finderfeed.fdbosses.content.entities.geburah.particles.geburah_ray.GeburahRayOptions;
import com.finderfeed.fdlib.util.math.ComplexEasingFunction;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
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

public class GeburahRayParticle
extends Particle {
    public Vec3 rayEnd;
    private GeburahRayOptions options;
    private ComplexEasingFunction easingFunction;
    public static final ParticleRenderType ADDITIVE_TRANSLUCENT = new ParticleRenderType(){

        public void m_6505_(BufferBuilder tesselator, TextureManager p_107437_) {
            RenderSystem.depthMask((boolean)true);
            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.setShader(GameRenderer::m_172753_);
            RenderSystem.blendFunc((int)770, (int)1);
            tesselator.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85815_);
        }

        public void m_6294_(Tesselator p_107438_) {
            p_107438_.m_85914_();
            RenderSystem.setShader(GameRenderer::m_172829_);
        }

        public String toString() {
            return "fdbosses:geburah_ray";
        }
    };

    public GeburahRayParticle(GeburahRayOptions rayOptions, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
        super(level, x, y, z, xd, yd, zd);
        this.options = rayOptions;
        this.f_107212_ = x;
        this.f_107213_ = y;
        this.f_107214_ = z;
        this.f_107215_ = xd;
        this.f_107216_ = yd;
        this.f_107217_ = zd;
        this.rayEnd = rayOptions.rayEnd;
        this.m_107259_(new AABB(x, y, z, this.rayEnd.f_82479_, this.rayEnd.f_82480_, this.rayEnd.f_82481_));
        this.f_107225_ = this.options.rayOptions.inTime + this.options.rayOptions.stayTime + this.options.rayOptions.outTime;
        this.easingFunction = ComplexEasingFunction.builder().addArea((float)this.options.rayOptions.inTime, FDEasings::easeIn).addArea((float)this.options.rayOptions.stayTime, FDEasings::one).addArea((float)this.options.rayOptions.outTime, FDEasings::reversedEaseOut).build();
        this.options.particleProcessor.init((Particle)this);
    }

    public void m_5989_() {
        this.options.particleProcessor.processParticle((Particle)this);
        this.f_107209_ = this.f_107212_;
        this.f_107210_ = this.f_107213_;
        this.f_107211_ = this.f_107214_;
        if (this.f_107224_++ >= this.f_107225_) {
            this.m_107274_();
        } else {
            this.f_107212_ += this.f_107215_;
            this.f_107213_ += this.f_107216_;
            this.f_107214_ += this.f_107217_;
        }
        this.m_107259_(new AABB(this.f_107212_, this.f_107213_, this.f_107214_, this.rayEnd.f_82479_, this.rayEnd.f_82480_, this.rayEnd.f_82481_));
    }

    public void m_5744_(VertexConsumer vertex, Camera camera, float pticks) {
        Vec3 pos = new Vec3(Mth.m_14139_((double)pticks, (double)this.f_107209_, (double)this.f_107212_), Mth.m_14139_((double)pticks, (double)this.f_107210_, (double)this.f_107213_), Mth.m_14139_((double)pticks, (double)this.f_107211_, (double)this.f_107214_));
        Vec3 b = this.rayEnd.m_82492_(pos.f_82479_, pos.f_82480_, pos.f_82481_);
        pos = pos.m_82546_(camera.m_90583_());
        double len = b.m_82553_();
        Matrix4f mat = new Matrix4f();
        mat.translate((float)pos.f_82479_, (float)pos.f_82480_, (float)pos.f_82481_);
        FDRenderUtil.applyMovementMatrixRotations((Matrix4f)mat, (Vec3)b);
        Vec3 n = FDMathUtil.getNormalVectorFromLineToPoint((Vec3)pos, (Vec3)b.m_82549_(pos), (Vec3)Vec3.f_82478_);
        Matrix4f mt2 = new Matrix4f();
        FDRenderUtil.applyMovementMatrixRotations((Matrix4f)mt2, (Vec3)b);
        Vector4f up = new Vector4f(0.0f, 0.0f, 1.0f, 1.0f);
        mt2.transform(up);
        Vector4f left = new Vector4f(1.0f, 0.0f, 0.0f, 1.0f);
        mt2.transform(left);
        Vec3 vup = new Vec3((double)(up.x / up.w), (double)(up.y / up.w), (double)(up.z / up.w));
        Vec3 vleft = new Vec3((double)left.x, (double)left.y, (double)left.z);
        float angle = (float)FDMathUtil.angleBetweenVectors((Vec3)n, (Vec3)vup);
        if (vleft.m_82526_(n) > 0.0) {
            mat.rotateY(angle);
        } else {
            mat.rotateY(-angle);
        }
        float p = this.easingFunction.apply((float)this.f_107224_ + pticks);
        float alpha = p * this.options.color.a;
        float w = this.options.rayWidth * p;
        for (int zh = 0; zh < 2; ++zh) {
            vertex.m_252986_(mat, 0.0f, 0.0f, 0.0f).m_85950_(this.options.color.r, this.options.color.g, this.options.color.b, alpha).m_5752_();
            vertex.m_252986_(mat, w, 0.0f, 0.0f).m_85950_(this.options.color.r, this.options.color.g, this.options.color.b, 0.0f).m_5752_();
            vertex.m_252986_(mat, w, (float)len, 0.0f).m_85950_(this.options.color.r, this.options.color.g, this.options.color.b, 0.0f).m_5752_();
            vertex.m_252986_(mat, 0.0f, (float)len, 0.0f).m_85950_(this.options.color.r, this.options.color.g, this.options.color.b, alpha).m_5752_();
            vertex.m_252986_(mat, 0.0f, 0.0f, 0.0f).m_85950_(this.options.color.r, this.options.color.g, this.options.color.b, alpha).m_5752_();
            vertex.m_252986_(mat, -w, 0.0f, 0.0f).m_85950_(this.options.color.r, this.options.color.g, this.options.color.b, 0.0f).m_5752_();
            vertex.m_252986_(mat, -w, (float)len, 0.0f).m_85950_(this.options.color.r, this.options.color.g, this.options.color.b, 0.0f).m_5752_();
            vertex.m_252986_(mat, 0.0f, (float)len, 0.0f).m_85950_(this.options.color.r, this.options.color.g, this.options.color.b, alpha).m_5752_();
        }
        mat.translate(0.0f, 0.0f, 0.01f);
        vertex.m_252986_(mat, 0.0f, 0.0f, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, alpha).m_5752_();
        vertex.m_252986_(mat, w * 0.15f, 0.0f, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 0.0f).m_5752_();
        vertex.m_252986_(mat, w * 0.15f, (float)len, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 0.0f).m_5752_();
        vertex.m_252986_(mat, 0.0f, (float)len, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, alpha).m_5752_();
        vertex.m_252986_(mat, 0.0f, 0.0f, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, alpha).m_5752_();
        vertex.m_252986_(mat, -w * 0.15f, 0.0f, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 0.0f).m_5752_();
        vertex.m_252986_(mat, -w * 0.15f, (float)len, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 0.0f).m_5752_();
        vertex.m_252986_(mat, 0.0f, (float)len, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, alpha).m_5752_();
        mat.translate(0.0f, 0.0f, 0.01f);
        this.renderInitialEffect(vertex, mat, 0, pticks, 4, (float)len, this.options.rayWidth * 0.15f);
        mat.translate(0.0f, 0.0f, 0.01f);
        this.renderInitialEffect(vertex, mat, 2, pticks, 4, (float)len, this.options.rayWidth * 0.3f);
    }

    private void renderInitialEffect(VertexConsumer vertex, Matrix4f mat, int ageDelay, float pticks, int effectLength, float rayLength, float effectWidth) {
        if (this.f_107224_ < ageDelay) {
            return;
        }
        float effectP = FDMathUtil.clamp((float)((float)(this.f_107224_ - ageDelay) + pticks), (float)0.0f, (float)effectLength) / (float)effectLength;
        effectP = FDEasings.easeIn((float)effectP);
        float maxXBorder = this.options.rayWidth;
        float currentXStartPos = Math.max(0.0f, (maxXBorder + effectWidth) * effectP - effectWidth);
        float currentXEndPos = Math.min(maxXBorder * effectP + effectWidth, maxXBorder);
        vertex.m_252986_(mat, currentXStartPos, 0.0f, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_5752_();
        vertex.m_252986_(mat, currentXEndPos, 0.0f, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_5752_();
        vertex.m_252986_(mat, currentXEndPos, rayLength, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_5752_();
        vertex.m_252986_(mat, currentXStartPos, rayLength, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_5752_();
        vertex.m_252986_(mat, -currentXStartPos, 0.0f, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_5752_();
        vertex.m_252986_(mat, -currentXEndPos, 0.0f, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_5752_();
        vertex.m_252986_(mat, -currentXEndPos, rayLength, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_5752_();
        vertex.m_252986_(mat, -currentXStartPos, rayLength, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_5752_();
    }

    public ParticleRenderType m_7556_() {
        return ADDITIVE_TRANSLUCENT;
    }

    public static class Factory
    implements ParticleProvider<GeburahRayOptions> {
        @Nullable
        public Particle createParticle(GeburahRayOptions type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new GeburahRayParticle(type, level, x, y, z, xd, yd, zd);
        }
    }
}


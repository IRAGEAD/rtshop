/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.util.math.ComplexEasingFunction
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  net.minecraft.client.Camera
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.particle.Particle
 *  net.minecraft.client.particle.ParticleProvider
 *  net.minecraft.client.particle.ParticleRenderType
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Matrix4f
 *  org.joml.Vector4f
 */
package com.finderfeed.fdbosses.client.particles.chesed_attack_ray;

import com.finderfeed.fdbosses.client.particles.arc_lightning.ArcLightningParticle;
import com.finderfeed.fdbosses.client.particles.chesed_attack_ray.ChesedRayOptions;
import com.finderfeed.fdlib.util.math.ComplexEasingFunction;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class ChesedAttackRayParticle
extends Particle {
    public Vec3 rayEnd;
    private ChesedRayOptions options;
    private ComplexEasingFunction easingFunction;

    public ChesedAttackRayParticle(ChesedRayOptions rayOptions, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
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
        float p = this.easingFunction.apply((float)this.f_107224_);
        float alpha = p * this.options.color.a;
        float w = this.options.rayWidth * p;
        vertex.m_252986_(mat, 0.0f, 0.0f, 0.0f).m_85950_(this.options.color.r, this.options.color.g, this.options.color.b, alpha);
        vertex.m_252986_(mat, w, 0.0f, 0.0f).m_85950_(this.options.color.r, this.options.color.g, this.options.color.b, 0.0f);
        vertex.m_252986_(mat, w, (float)len, 0.0f).m_85950_(this.options.color.r, this.options.color.g, this.options.color.b, 0.0f);
        vertex.m_252986_(mat, 0.0f, (float)len, 0.0f).m_85950_(this.options.color.r, this.options.color.g, this.options.color.b, alpha);
        vertex.m_252986_(mat, 0.0f, 0.0f, 0.0f).m_85950_(this.options.color.r, this.options.color.g, this.options.color.b, alpha);
        vertex.m_252986_(mat, -w, 0.0f, 0.0f).m_85950_(this.options.color.r, this.options.color.g, this.options.color.b, 0.0f);
        vertex.m_252986_(mat, -w, (float)len, 0.0f).m_85950_(this.options.color.r, this.options.color.g, this.options.color.b, 0.0f);
        vertex.m_252986_(mat, 0.0f, (float)len, 0.0f).m_85950_(this.options.color.r, this.options.color.g, this.options.color.b, alpha);
        mat.translate(0.0f, 0.0f, 0.01f);
        vertex.m_252986_(mat, 0.0f, 0.0f, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, alpha);
        vertex.m_252986_(mat, w * 0.15f, 0.0f, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 0.0f);
        vertex.m_252986_(mat, w * 0.15f, (float)len, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 0.0f);
        vertex.m_252986_(mat, 0.0f, (float)len, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, alpha);
        vertex.m_252986_(mat, 0.0f, 0.0f, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, alpha);
        vertex.m_252986_(mat, -w * 0.15f, 0.0f, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 0.0f);
        vertex.m_252986_(mat, -w * 0.15f, (float)len, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 0.0f);
        vertex.m_252986_(mat, 0.0f, (float)len, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, alpha);
        List<Vec3> positions = List.of(Vec3.f_82478_, new Vec3(0.0, len, 0.0));
        List<Vec3> path = ArcLightningParticle.buildPath(this.f_107208_.m_46467_(), w * 2.0f, 342332, Math.max((int)Math.round(len / (double)(2.0f * w * 2.0f)), 2), positions);
        mat.translate(0.0f, 0.0f, 0.02f);
        ArcLightningParticle.drawLightning(mat, vertex, path, positions, w * 0.5f, this.options.lightningColor.r, this.options.lightningColor.g, this.options.lightningColor.b, alpha);
        mat.translate(0.0f, 0.0f, 0.02f);
        ArcLightningParticle.drawLightning(mat, vertex, path, positions, w * 0.5f * 0.15f, 1.0f, 1.0f, 1.0f, alpha);
        mat.translate(0.0f, 0.0f, -0.06f);
        ArcLightningParticle.drawLightning(mat, vertex, path, positions, w * 0.5f * 0.15f, 1.0f, 1.0f, 1.0f, alpha);
    }

    public ParticleRenderType m_7556_() {
        return ArcLightningParticle.RENDER_TYPE;
    }

    public static class Factory
    implements ParticleProvider<ChesedRayOptions> {
        @Nullable
        public Particle createParticle(ChesedRayOptions type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new ChesedAttackRayParticle(type, level, x, y, z, xd, yd, zd);
        }
    }
}


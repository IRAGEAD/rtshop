/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.util.math.ComplexEasingFunction
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  net.minecraft.client.Camera
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.particle.Particle
 *  net.minecraft.client.particle.ParticleProvider
 *  net.minecraft.client.particle.ParticleRenderType
 *  net.minecraft.client.particle.SpriteSet
 *  net.minecraft.client.particle.TextureSheetParticle
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 *  org.joml.AxisAngle4f
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 *  org.joml.Vector3f
 */
package com.finderfeed.fdbosses.client.particles.sonic_particle;

import com.finderfeed.fdbosses.client.particles.sonic_particle.SonicParticleOptions;
import com.finderfeed.fdlib.util.math.ComplexEasingFunction;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

public class SonicParticle
extends TextureSheetParticle {
    private SonicParticleOptions options;
    private float currentResizeSpeed;
    private ComplexEasingFunction alphaEasingFunction;
    private float currentQuadSize;
    private float oldQuadSize;
    private Quaternionf facing;

    public SonicParticle(SonicParticleOptions options, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
        super(level, x, y, z, xd, yd, zd);
        Quaternionf q;
        this.options = options;
        this.f_107225_ = options.lifetime;
        this.currentResizeSpeed = options.resizeSpeed;
        this.f_107212_ = x;
        this.f_107213_ = y;
        this.f_107214_ = z;
        this.f_107215_ = xd;
        this.f_107216_ = yd;
        this.f_107217_ = zd;
        this.f_107663_ = options.startSize;
        this.f_107227_ = options.color.r;
        this.f_107228_ = options.color.g;
        this.f_107229_ = options.color.b;
        double finalResize = options.endSize - options.startSize;
        if (options.resizeAcceleration != 0.0f) {
            double d = (double)(options.resizeSpeed * options.resizeSpeed) + (double)(2.0f * options.resizeAcceleration) * finalResize;
            double t1 = ((double)(-options.resizeSpeed) + Math.sqrt(d)) / (double)options.resizeAcceleration;
            double t2 = ((double)(-options.resizeSpeed) - Math.sqrt(d)) / (double)options.resizeAcceleration;
            this.f_107225_ = (int)Math.ceil(Math.max(Math.abs(t2), Math.abs(t1)));
        } else {
            double t = finalResize / (double)options.resizeSpeed;
            this.f_107225_ = (int)Math.ceil(Math.abs(t));
        }
        double l = options.facingDirection.f_82479_ * options.facingDirection.f_82479_ + options.facingDirection.f_82481_ * options.facingDirection.f_82481_;
        l = Math.sqrt(l);
        float angle1 = (float)Math.atan2(options.facingDirection.f_82481_, options.facingDirection.f_82479_);
        float angle2 = (float)Math.atan2(options.facingDirection.f_82480_, l);
        this.facing = q = new Quaternionf(new AxisAngle4f(angle1, 0.0f, 1.0f, 0.0f)).rotateX(-angle2);
        if (!options.alphaOptions.isEmpty()) {
            this.alphaEasingFunction = ComplexEasingFunction.builder().addArea((float)options.alphaOptions.inTime, FDEasings::linear).addArea((float)options.alphaOptions.stayTime, f -> Float.valueOf(1.0f)).addArea((float)options.alphaOptions.outTime, FDEasings::reversedLinear).build();
            this.f_107230_ = 0.0f;
        } else {
            this.f_107230_ = options.alphaOptions.maxAlpha;
        }
        this.f_107219_ = false;
        this.f_172258_ = 0.0f;
        this.currentQuadSize = this.f_107663_;
        this.oldQuadSize = this.f_107663_;
    }

    public void m_5744_(VertexConsumer vertex, Camera camera, float pticks) {
        Quaternionf quaternionf;
        float q;
        this.f_107663_ = q = FDMathUtil.lerp((float)this.oldQuadSize, (float)this.currentQuadSize, (float)pticks);
        Vec3 vec3 = camera.m_90583_();
        float f = (float)(Mth.m_14139_((double)pticks, (double)this.f_107209_, (double)this.f_107212_) - vec3.m_7096_());
        float f1 = (float)(Mth.m_14139_((double)pticks, (double)this.f_107210_, (double)this.f_107213_) - vec3.m_7098_());
        float f2 = (float)(Mth.m_14139_((double)pticks, (double)this.f_107211_, (double)this.f_107214_) - vec3.m_7094_());
        if (this.f_107231_ == 0.0f) {
            quaternionf = this.facing;
        } else {
            quaternionf = new Quaternionf((Quaternionfc)this.facing);
            quaternionf.rotateZ(Mth.m_14179_((float)pticks, (float)this.f_107204_, (float)this.f_107231_));
        }
        Vector3f[] avector3f = new Vector3f[]{new Vector3f(-1.0f, -1.0f, 0.0f), new Vector3f(-1.0f, 1.0f, 0.0f), new Vector3f(1.0f, 1.0f, 0.0f), new Vector3f(1.0f, -1.0f, 0.0f)};
        float f3 = this.m_5902_(pticks);
        for (int i = 0; i < 4; ++i) {
            Vector3f vector3f = avector3f[i];
            vector3f.rotate((Quaternionfc)quaternionf);
            vector3f.mul(f3);
            vector3f.add(f, f1, f2);
        }
        float f6 = this.m_5970_();
        float f7 = this.m_5952_();
        float f4 = this.m_5951_();
        float f5 = this.m_5950_();
        int j = this.m_6355_(pticks);
        vertex.m_5483_((double)avector3f[0].x(), (double)avector3f[0].y(), (double)avector3f[0].z()).m_7421_(f7, f5).m_85950_(this.f_107227_, this.f_107228_, this.f_107229_, this.f_107230_).m_85969_(j).m_5752_();
        vertex.m_5483_((double)avector3f[1].x(), (double)avector3f[1].y(), (double)avector3f[1].z()).m_7421_(f7, f4).m_85950_(this.f_107227_, this.f_107228_, this.f_107229_, this.f_107230_).m_85969_(j).m_5752_();
        vertex.m_5483_((double)avector3f[2].x(), (double)avector3f[2].y(), (double)avector3f[2].z()).m_7421_(f6, f4).m_85950_(this.f_107227_, this.f_107228_, this.f_107229_, this.f_107230_).m_85969_(j).m_5752_();
        vertex.m_5483_((double)avector3f[3].x(), (double)avector3f[3].y(), (double)avector3f[3].z()).m_7421_(f6, f5).m_85950_(this.f_107227_, this.f_107228_, this.f_107229_, this.f_107230_).m_85969_(j).m_5752_();
    }

    public void m_5989_() {
        super.m_5989_();
        if (this.alphaEasingFunction != null) {
            this.f_107230_ = this.alphaEasingFunction.apply((float)this.f_107224_) * this.options.alphaOptions.maxAlpha;
        }
        float max = Math.max(this.options.startSize, this.options.endSize);
        float min = Math.min(this.options.startSize, this.options.endSize);
        this.oldQuadSize = this.currentQuadSize;
        this.currentQuadSize = Mth.m_14036_((float)(this.currentQuadSize + this.currentResizeSpeed), (float)min, (float)max);
        this.currentResizeSpeed += this.options.resizeAcceleration;
    }

    protected int m_6355_(float p_107249_) {
        return 0xF000F0;
    }

    public ParticleRenderType m_7556_() {
        return ParticleRenderType.f_107432_;
    }

    public static class Factory
    implements ParticleProvider<SonicParticleOptions> {
        private SpriteSet spriteSet;

        public Factory(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Nullable
        public Particle createParticle(SonicParticleOptions particle, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            SonicParticle p = new SonicParticle(particle, level, x, y, z, xd, yd, zd);
            p.m_108335_(this.spriteSet);
            return p;
        }
    }
}


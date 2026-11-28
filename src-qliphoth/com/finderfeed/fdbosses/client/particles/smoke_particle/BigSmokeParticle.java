/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.util.math.ComplexEasingFunction
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil$ParticleRenderTypes
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  net.minecraft.client.Camera
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.particle.Particle
 *  net.minecraft.client.particle.ParticleProvider
 *  net.minecraft.client.particle.ParticleRenderType
 *  net.minecraft.client.particle.SpriteSet
 *  net.minecraft.client.particle.TextureSheetParticle
 *  org.jetbrains.annotations.Nullable
 */
package com.finderfeed.fdbosses.client.particles.smoke_particle;

import com.finderfeed.fdbosses.client.particles.smoke_particle.BigSmokeParticleOptions;
import com.finderfeed.fdlib.util.math.ComplexEasingFunction;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import org.jetbrains.annotations.Nullable;

public class BigSmokeParticle
extends TextureSheetParticle {
    private BigSmokeParticleOptions options;
    private ComplexEasingFunction easingFunction;
    private float maxQuadSize;
    private float oQuadSize;
    private float cQuadSize;

    public BigSmokeParticle(BigSmokeParticleOptions options, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
        super(level, x, y, z, xd, yd, zd);
        this.options = options;
        this.f_107212_ = x;
        this.f_107213_ = y;
        this.f_107214_ = z;
        this.f_107215_ = xd;
        this.f_107216_ = yd;
        this.f_107217_ = zd;
        this.f_107227_ = options.color.r;
        this.f_107228_ = options.color.g;
        this.f_107229_ = options.color.b;
        this.f_107230_ = options.color.a;
        this.easingFunction = ComplexEasingFunction.builder().addArea((float)options.intOut.inTime, FDEasings::linear).addArea((float)options.intOut.stayTime, FDEasings::one).addArea((float)options.intOut.outTime, FDEasings::reversedLinear).build();
        this.cQuadSize = this.f_107663_ = this.easingFunction.apply(0.0f) * options.size;
        this.oQuadSize = this.f_107663_;
        this.f_107225_ = options.intOut.fullTime();
        this.maxQuadSize = options.size;
        this.oQuadSize = this.f_107663_;
        this.f_172258_ = options.friction;
        this.f_107219_ = false;
    }

    public void m_5744_(VertexConsumer consumer, Camera camera, float pticks) {
        this.f_107663_ = FDMathUtil.lerp((float)this.oQuadSize, (float)this.cQuadSize, (float)pticks);
        super.m_5744_(consumer, camera, pticks);
    }

    public void m_5989_() {
        double l = Math.sqrt(this.f_107215_ * this.f_107215_ + this.f_107216_ * this.f_107216_ + this.f_107217_ * this.f_107217_);
        if (l < (double)this.options.minSpeed) {
            this.f_107215_ = this.f_107215_ / l * (double)this.options.minSpeed;
            this.f_107216_ = this.f_107216_ / l * (double)this.options.minSpeed;
            this.f_107217_ = this.f_107217_ / l * (double)this.options.minSpeed;
        }
        this.oQuadSize = this.cQuadSize;
        this.cQuadSize = this.maxQuadSize * this.easingFunction.apply((float)this.f_107224_);
        super.m_5989_();
    }

    public ParticleRenderType m_7556_() {
        return FDRenderUtil.ParticleRenderTypes.NORMAL_TRANSLUCENT;
    }

    public static class Factory
    implements ParticleProvider<BigSmokeParticleOptions> {
        private SpriteSet set;

        public Factory(SpriteSet set) {
            this.set = set;
        }

        @Nullable
        public Particle createParticle(BigSmokeParticleOptions options, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            BigSmokeParticle particle = new BigSmokeParticle(options, level, x, y, z, xd, yd, zd);
            particle.m_108335_(this.set);
            return particle;
        }
    }
}


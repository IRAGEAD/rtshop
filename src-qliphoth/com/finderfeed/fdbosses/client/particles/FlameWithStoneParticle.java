/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.particle.Particle
 *  net.minecraft.client.particle.ParticleProvider
 *  net.minecraft.client.particle.ParticleRenderType
 *  net.minecraft.client.particle.SpriteSet
 *  org.jetbrains.annotations.Nullable
 */
package com.finderfeed.fdbosses.client.particles;

import com.finderfeed.fdbosses.client.particles.GravityOptionsParticle;
import com.finderfeed.fdbosses.client.particles.GravityParticleOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import org.jetbrains.annotations.Nullable;

public class FlameWithStoneParticle
extends GravityOptionsParticle {
    public FlameWithStoneParticle(GravityParticleOptions gravityParticleOptions, ClientLevel leve, double x, double y, double z, double xd, double yd, double zd) {
        super(gravityParticleOptions, leve, x, y, z, xd, yd, zd);
    }

    public ParticleRenderType m_7556_() {
        return ParticleRenderType.f_107432_;
    }

    protected int m_6355_(float p_107249_) {
        return 0xF000F0;
    }

    public static class Factory
    implements ParticleProvider<GravityParticleOptions> {
        private SpriteSet spriteSet;

        public Factory(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Nullable
        public Particle createParticle(GravityParticleOptions options, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            FlameWithStoneParticle flameWithStoneParticle = new FlameWithStoneParticle(options, level, x, y, z, xd, yd, zd);
            flameWithStoneParticle.m_108335_(this.spriteSet);
            return flameWithStoneParticle;
        }
    }
}


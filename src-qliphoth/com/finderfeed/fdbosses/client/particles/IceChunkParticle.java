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

public class IceChunkParticle
extends GravityOptionsParticle {
    public IceChunkParticle(GravityParticleOptions gravityParticleOptions, ClientLevel leve, double x, double y, double z, double xd, double yd, double zd) {
        super(gravityParticleOptions, leve, x, y, z, xd, yd, zd);
    }

    public ParticleRenderType m_7556_() {
        return ParticleRenderType.f_107431_;
    }

    public static class Factory
    implements ParticleProvider<GravityParticleOptions> {
        private SpriteSet spriteSet;

        public Factory(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Nullable
        public Particle createParticle(GravityParticleOptions options, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            IceChunkParticle iceChunkParticle = new IceChunkParticle(options, level, x, y, z, xd, yd, zd);
            iceChunkParticle.m_108335_(this.spriteSet);
            return iceChunkParticle;
        }
    }
}


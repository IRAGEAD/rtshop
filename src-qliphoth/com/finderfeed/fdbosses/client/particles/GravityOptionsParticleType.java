/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  net.minecraft.core.particles.ParticleType
 */
package com.finderfeed.fdbosses.client.particles;

import com.finderfeed.fdbosses.client.particles.GravityParticleOptions;
import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleType;

public class GravityOptionsParticleType
extends ParticleType<GravityParticleOptions> {
    public GravityOptionsParticleType(boolean overrideLimiter) {
        super(overrideLimiter, GravityParticleOptions.DESERIALIZER);
    }

    public Codec<GravityParticleOptions> m_7652_() {
        return GravityParticleOptions.CODEC;
    }
}


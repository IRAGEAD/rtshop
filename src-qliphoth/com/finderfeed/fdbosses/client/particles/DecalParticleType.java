/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  net.minecraft.core.particles.ParticleType
 */
package com.finderfeed.fdbosses.client.particles;

import com.finderfeed.fdbosses.client.particles.DecalParticleOptions;
import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleType;

public class DecalParticleType
extends ParticleType<DecalParticleOptions> {
    public DecalParticleType(boolean p_123740_) {
        super(p_123740_, DecalParticleOptions.DESERIALIZER);
    }

    public Codec<DecalParticleOptions> m_7652_() {
        return DecalParticleOptions.CODEC;
    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.screen.screen_particles.FDTexturedSParticle
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  net.minecraft.client.particle.ParticleRenderType
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.Mth
 */
package com.finderfeed.fdbosses.client.boss_codex;

import com.finderfeed.fdlib.systems.screen.screen_particles.FDTexturedSParticle;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class FlashyTexturedScreenParticle
extends FDTexturedSParticle {
    private float flashFrequency;
    private float flashOffset = new Random().nextFloat() * (float)Math.PI * 2.0f;
    private float rollFriction = 1.0f;

    public FlashyTexturedScreenParticle(Function<ResourceLocation, ? extends ParticleRenderType> factory, ResourceLocation location) {
        super(factory, location);
    }

    public static FlashyTexturedScreenParticle create(Function<ResourceLocation, ? extends ParticleRenderType> factory, ResourceLocation location) {
        return new FlashyTexturedScreenParticle(factory, location);
    }

    public void tick() {
        super.tick();
        this.setRollSpeed(this.getRollSpeed() * this.rollFriction);
    }

    public float getA() {
        float pticks = FDRenderUtil.tryGetPartialTickIgnorePause();
        float time = (float)this.getAge() + pticks;
        float p = Mth.m_14036_((float)(time / (float)this.getLifetime()), (float)0.0f, (float)1.0f);
        float flash = (float)(Math.sin(this.flashFrequency * (this.flashOffset + time)) * 0.5 + 0.5) * 0.5f + 0.5f;
        return FDMathUtil.lerp((float)0.0f, (float)super.getA(), (float)(1.0f - p)) * flash;
    }

    public FlashyTexturedScreenParticle setFlashFrequency(float flashFrequency) {
        this.flashFrequency = flashFrequency;
        return this;
    }

    public FlashyTexturedScreenParticle setFlashOffset(float flashOffset) {
        this.flashOffset = flashOffset;
        return this;
    }

    public FlashyTexturedScreenParticle setRollFriction(float rollFriction) {
        this.rollFriction = rollFriction;
        return this;
    }
}


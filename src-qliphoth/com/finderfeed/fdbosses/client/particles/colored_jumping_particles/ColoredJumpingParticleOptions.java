/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec
 *  com.finderfeed.fdlib.util.FDCodecs
 *  com.finderfeed.fdlib.util.FDColor
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleOptions$Deserializer
 *  net.minecraft.core.particles.ParticleType
 *  net.minecraft.network.FriendlyByteBuf
 */
package com.finderfeed.fdbosses.client.particles.colored_jumping_particles;

import com.finderfeed.fdbosses.client.BossParticles;
import com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec;
import com.finderfeed.fdlib.util.FDCodecs;
import com.finderfeed.fdlib.util.FDColor;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;

public class ColoredJumpingParticleOptions
implements ParticleOptions {
    public static final ParticleOptions.Deserializer<ColoredJumpingParticleOptions> DESERIALIZER = new ParticleOptions.Deserializer<ColoredJumpingParticleOptions>(){

        public ColoredJumpingParticleOptions fromCommand(ParticleType<ColoredJumpingParticleOptions> p_123733_, StringReader p_123734_) throws CommandSyntaxException {
            return ColoredJumpingParticleOptions.builder().build();
        }

        public ColoredJumpingParticleOptions fromNetwork(ParticleType<ColoredJumpingParticleOptions> p_123735_, FriendlyByteBuf p_123736_) {
            return (ColoredJumpingParticleOptions)STREAM_CODEC.fromNetwork(p_123736_);
        }
    };
    public static final NetworkCodec<ColoredJumpingParticleOptions> STREAM_CODEC = NetworkCodec.composite((NetworkCodec)NetworkCodec.COLOR, v -> v.colorStart, (NetworkCodec)NetworkCodec.COLOR, v -> v.colorEnd, (NetworkCodec)NetworkCodec.INT, v -> v.maxPointsInTrail, (NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.reflectionStrength), (NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.gravity), (NetworkCodec)NetworkCodec.INT, v -> v.lifetime, (NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.size), (NetworkCodec)NetworkCodec.INT, v -> v.maxJumpAmount, ColoredJumpingParticleOptions::new);
    public static final Codec<ColoredJumpingParticleOptions> MAP_CODEC = RecordCodecBuilder.create(instance -> instance.group((App)FDCodecs.COLOR.fieldOf("colorStart").forGetter(o -> o.colorStart), (App)FDCodecs.COLOR.fieldOf("colorEnd").forGetter(o -> o.colorEnd), (App)Codec.INT.fieldOf("max_points_in_trail").forGetter(o -> o.maxPointsInTrail), (App)Codec.FLOAT.fieldOf("reflection_strength").forGetter(o -> Float.valueOf(o.reflectionStrength)), (App)Codec.FLOAT.fieldOf("gravity").forGetter(o -> Float.valueOf(o.gravity)), (App)Codec.INT.fieldOf("lifetime").forGetter(o -> o.lifetime), (App)Codec.FLOAT.fieldOf("size").forGetter(o -> Float.valueOf(o.size)), (App)Codec.INT.fieldOf("max_jump_amount").forGetter(o -> o.maxJumpAmount)).apply((Applicative)instance, ColoredJumpingParticleOptions::new));
    public FDColor colorStart;
    public FDColor colorEnd;
    public int maxPointsInTrail;
    public float reflectionStrength;
    public float gravity;
    public int lifetime;
    public float size;
    public int maxJumpAmount;

    public ColoredJumpingParticleOptions(FDColor colorStart, FDColor colorEnd, int maxPointsInTrail, float reflectionStrength, float gravity, int lifetime, float size, int maxJumpAmount) {
        this.colorStart = colorStart;
        this.colorEnd = colorEnd;
        this.maxPointsInTrail = maxPointsInTrail;
        this.reflectionStrength = reflectionStrength;
        this.gravity = gravity;
        this.lifetime = lifetime;
        this.size = size;
        this.maxJumpAmount = maxJumpAmount;
    }

    public ColoredJumpingParticleOptions(FDColor colorStart, FDColor colorEnd, int maxPointsInTrail, float reflectionStrength, float gravity, int lifetime) {
        this(colorStart, colorEnd, maxPointsInTrail, reflectionStrength, gravity, lifetime, 0.1f, 3);
    }

    public ColoredJumpingParticleOptions(FDColor colorStart, FDColor colorEnd, int maxPointsInTrail, float reflectionStrength, float gravity) {
        this(colorStart, colorEnd, maxPointsInTrail, reflectionStrength, gravity, 200, 0.1f, 3);
    }

    public ColoredJumpingParticleOptions(FDColor colorStart, FDColor colorEnd, int maxPointsInTrail, float reflectionStrength) {
        this(colorStart, colorEnd, maxPointsInTrail, reflectionStrength, 1.0f, 200, 0.1f, 3);
    }

    public ParticleType<?> m_6012_() {
        return BossParticles.COLORED_JUMPING_PARTICLE.get();
    }

    public void m_7711_(FriendlyByteBuf p_123732_) {
        STREAM_CODEC.toNetwork(p_123732_, (Object)this);
    }

    public String m_5942_() {
        return "";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private FDColor colorStart;
        private FDColor colorEnd;
        private int maxPointsInTrail = 20;
        private float reflectionStrength = 0.5f;
        private float gravity = 1.0f;
        private int lifetime = 200;
        private float size = 0.1f;
        private int maxJumpAmount = 3;

        public Builder colorStart(FDColor colorStart) {
            this.colorStart = colorStart;
            return this;
        }

        public Builder colorEnd(FDColor colorEnd) {
            this.colorEnd = colorEnd;
            return this;
        }

        public Builder maxPointsInTrail(int value) {
            this.maxPointsInTrail = value;
            return this;
        }

        public Builder reflectionStrength(float value) {
            this.reflectionStrength = value;
            return this;
        }

        public Builder gravity(float value) {
            this.gravity = value;
            return this;
        }

        public Builder lifetime(int value) {
            this.lifetime = value;
            return this;
        }

        public Builder size(float value) {
            this.size = value;
            return this;
        }

        public Builder maxJumpAmount(int value) {
            this.maxJumpAmount = value;
            return this;
        }

        public ColoredJumpingParticleOptions build() {
            return new ColoredJumpingParticleOptions(this.colorStart, this.colorEnd, this.maxPointsInTrail, this.reflectionStrength, this.gravity, this.lifetime, this.size, this.maxJumpAmount);
        }
    }
}


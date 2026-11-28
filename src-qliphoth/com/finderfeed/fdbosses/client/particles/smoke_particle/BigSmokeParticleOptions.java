/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec
 *  com.finderfeed.fdlib.util.FDCodecs
 *  com.finderfeed.fdlib.util.FDColor
 *  com.finderfeed.fdlib.util.client.particles.options.AlphaOptions
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
package com.finderfeed.fdbosses.client.particles.smoke_particle;

import com.finderfeed.fdbosses.client.BossParticles;
import com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec;
import com.finderfeed.fdlib.util.FDCodecs;
import com.finderfeed.fdlib.util.FDColor;
import com.finderfeed.fdlib.util.client.particles.options.AlphaOptions;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;

public class BigSmokeParticleOptions
implements ParticleOptions {
    public static final ParticleOptions.Deserializer<BigSmokeParticleOptions> DESERIALIZER = new ParticleOptions.Deserializer<BigSmokeParticleOptions>(){

        public BigSmokeParticleOptions fromCommand(ParticleType<BigSmokeParticleOptions> p_123733_, StringReader p_123734_) throws CommandSyntaxException {
            return new BigSmokeParticleOptions();
        }

        public BigSmokeParticleOptions fromNetwork(ParticleType<BigSmokeParticleOptions> p_123735_, FriendlyByteBuf p_123736_) {
            return (BigSmokeParticleOptions)STREAM_CODEC.fromNetwork(p_123736_);
        }
    };
    public static final Codec<BigSmokeParticleOptions> CODEC = RecordCodecBuilder.create(p -> p.group((App)AlphaOptions.CODEC.fieldOf("inOutOptions").forGetter(v -> v.intOut), (App)FDCodecs.COLOR.fieldOf("color").forGetter(v -> v.color), (App)Codec.FLOAT.fieldOf("minSpeed").forGetter(v -> Float.valueOf(v.minSpeed)), (App)Codec.FLOAT.fieldOf("size").forGetter(v -> Float.valueOf(v.size)), (App)Codec.FLOAT.fieldOf("friction").forGetter(v -> Float.valueOf(v.friction))).apply((Applicative)p, (alpha, color, minSpeed, size, friction) -> {
        BigSmokeParticleOptions d = new BigSmokeParticleOptions();
        d.minSpeed = minSpeed.floatValue();
        d.intOut = alpha;
        d.color = color;
        d.size = size.floatValue();
        d.friction = friction.floatValue();
        return d;
    }));
    public static final NetworkCodec<BigSmokeParticleOptions> STREAM_CODEC = NetworkCodec.composite((NetworkCodec)AlphaOptions.STREAM_CODEC, v -> v.intOut, (NetworkCodec)NetworkCodec.COLOR, v -> v.color, (NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.minSpeed), (NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.size), (NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.friction), (alpha, color, minSpeed, size, friction) -> {
        BigSmokeParticleOptions d = new BigSmokeParticleOptions();
        d.intOut = alpha;
        d.color = color;
        d.size = size.floatValue();
        d.minSpeed = minSpeed.floatValue();
        d.friction = friction.floatValue();
        return d;
    });
    public AlphaOptions intOut = new AlphaOptions();
    public FDColor color = new FDColor(1.0f, 1.0f, 1.0f, 1.0f);
    public float minSpeed = 0.0f;
    public float size = 1.0f;
    public float friction = 1.0f;

    public static Builder builder() {
        return new Builder();
    }

    public ParticleType<?> m_6012_() {
        return BossParticles.BIS_SMOKE.get();
    }

    public void m_7711_(FriendlyByteBuf p_123732_) {
        STREAM_CODEC.toNetwork(p_123732_, (Object)this);
    }

    public String m_5942_() {
        return "zhopa";
    }

    public static class Builder {
        private BigSmokeParticleOptions options = new BigSmokeParticleOptions();

        public Builder lifetime(int in, int stay, int out) {
            this.options.intOut.inTime = in;
            this.options.intOut.stayTime = stay;
            this.options.intOut.outTime = out;
            return this;
        }

        public Builder size(float size) {
            this.options.size = size;
            return this;
        }

        public Builder minSpeed(float minSpeed) {
            this.options.minSpeed = minSpeed;
            return this;
        }

        public Builder friction(float friction) {
            this.options.friction = friction;
            return this;
        }

        public Builder color(int r, int g, int b, int a) {
            return this.color(new FDColor((float)r / 255.0f, (float)g / 255.0f, (float)b / 255.0f, (float)a / 255.0f));
        }

        public Builder color(int r, int g, int b) {
            return this.color(new FDColor((float)r / 255.0f, (float)g / 255.0f, (float)b / 255.0f, 1.0f));
        }

        public Builder color(float r, float g, float b) {
            return this.color(new FDColor(r, g, b, 1.0f));
        }

        public Builder color(float r, float g, float b, float a) {
            return this.color(new FDColor(r, g, b, a));
        }

        public Builder color(FDColor color) {
            this.options.color = color;
            return this;
        }

        public BigSmokeParticleOptions build() {
            return this.options;
        }
    }
}


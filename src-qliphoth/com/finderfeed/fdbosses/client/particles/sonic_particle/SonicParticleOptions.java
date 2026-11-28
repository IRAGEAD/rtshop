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
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleOptions$Deserializer
 *  net.minecraft.core.particles.ParticleType
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.client.particles.sonic_particle;

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
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

public class SonicParticleOptions
implements ParticleOptions {
    public static final ParticleOptions.Deserializer<SonicParticleOptions> DESERIALIZER = new ParticleOptions.Deserializer<SonicParticleOptions>(){

        public SonicParticleOptions fromCommand(ParticleType<SonicParticleOptions> p_123733_, StringReader p_123734_) throws CommandSyntaxException {
            return new SonicParticleOptions();
        }

        public SonicParticleOptions fromNetwork(ParticleType<SonicParticleOptions> p_123735_, FriendlyByteBuf p_123736_) {
            return (SonicParticleOptions)STREAM_CODEC.fromNetwork(p_123736_);
        }
    };
    public static final Codec<SonicParticleOptions> CODEC = RecordCodecBuilder.create(p -> p.group((App)FDCodecs.COLOR.fieldOf("color").forGetter(v -> v.color), (App)AlphaOptions.CODEC.fieldOf("alphaOptions").forGetter(v -> v.alphaOptions), (App)FDCodecs.VEC3.fieldOf("facing").forGetter(v -> v.facingDirection), (App)Codec.FLOAT.fieldOf("startSize").forGetter(v -> Float.valueOf(v.startSize)), (App)Codec.FLOAT.fieldOf("endSize").forGetter(v -> Float.valueOf(v.endSize)), (App)Codec.FLOAT.fieldOf("resizeSpeed").forGetter(v -> Float.valueOf(v.resizeSpeed)), (App)Codec.FLOAT.fieldOf("resizeAcceleration").forGetter(v -> Float.valueOf(v.resizeAcceleration)), (App)Codec.INT.fieldOf("lifetime").forGetter(v -> v.lifetime)).apply((Applicative)p, (color, alpha, facing, startSize, endSize, resizeSpeed, resizeAcceleration, lifetime) -> {
        SonicParticleOptions o = new SonicParticleOptions();
        o.color = color;
        o.lifetime = lifetime;
        o.alphaOptions = alpha;
        o.facingDirection = facing;
        o.startSize = startSize.floatValue();
        o.resizeSpeed = resizeSpeed.floatValue();
        o.resizeAcceleration = resizeAcceleration.floatValue();
        o.endSize = endSize.floatValue();
        return o;
    }));
    public static final NetworkCodec<SonicParticleOptions> STREAM_CODEC = NetworkCodec.composite((NetworkCodec)NetworkCodec.COLOR, v -> v.color, (NetworkCodec)AlphaOptions.STREAM_CODEC, v -> v.alphaOptions, (NetworkCodec)NetworkCodec.VEC3, v -> v.facingDirection, (NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.startSize), (NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.endSize), (NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.resizeSpeed), (NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.resizeAcceleration), (NetworkCodec)NetworkCodec.INT, v -> v.lifetime, (color, alpha, facing, startSize, endSize, resizeSpeed, resizeAcceleration, lifetime) -> {
        SonicParticleOptions o = new SonicParticleOptions();
        o.color = color;
        o.lifetime = lifetime;
        o.alphaOptions = alpha;
        o.facingDirection = facing;
        o.startSize = startSize.floatValue();
        o.resizeSpeed = resizeSpeed.floatValue();
        o.resizeAcceleration = resizeAcceleration.floatValue();
        o.endSize = endSize.floatValue();
        return o;
    });
    public FDColor color = new FDColor(1.0f, 1.0f, 1.0f, 1.0f);
    public AlphaOptions alphaOptions = new AlphaOptions();
    public Vec3 facingDirection = new Vec3(0.0, 1.0, 0.0);
    public float startSize = 1.0f;
    public float endSize = 0.0f;
    public float resizeSpeed = -0.1f;
    public float resizeAcceleration = 0.0f;
    public int lifetime = 60;

    public static MapCodec<SonicParticleOptions> createCodec() {
        return CODEC.fieldOf("options");
    }

    public ParticleType<?> m_6012_() {
        return BossParticles.SONIC_PARTICLE.get();
    }

    public void m_7711_(FriendlyByteBuf p_123732_) {
        STREAM_CODEC.toNetwork(p_123732_, (Object)this);
    }

    public String m_5942_() {
        return "zhopa";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private SonicParticleOptions options = new SonicParticleOptions();

        public Builder lifetime(int lifetime) {
            this.options.lifetime = lifetime;
            return this;
        }

        public Builder endSize(float endSize) {
            this.options.endSize = endSize;
            return this;
        }

        public Builder startSize(float startSize) {
            this.options.startSize = startSize;
            return this;
        }

        public Builder resizeSpeed(float resizeSpeed) {
            this.options.resizeSpeed = resizeSpeed;
            return this;
        }

        public Builder resizeAcceleration(float resizeAcceleration) {
            this.options.resizeAcceleration = resizeAcceleration;
            return this;
        }

        public Builder facing(Vec3 facing) {
            this.options.facingDirection = facing;
            return this;
        }

        public Builder facing(double x, double y, double z) {
            return this.facing(new Vec3(x, y, z));
        }

        public Builder alpha(AlphaOptions options) {
            this.options.alphaOptions = options;
            return this;
        }

        public Builder color(float r, float g, float b) {
            this.options.color.r = r;
            this.options.color.g = g;
            this.options.color.b = b;
            return this;
        }

        public Builder color(int r, int g, int b) {
            this.options.color.r = (float)r / 255.0f;
            this.options.color.g = (float)g / 255.0f;
            this.options.color.b = (float)b / 255.0f;
            return this;
        }

        public Builder color(float r, float g, float b, float a) {
            this.options.color.r = r;
            this.options.color.g = g;
            this.options.color.b = b;
            this.options.alphaOptions.maxAlpha = a;
            return this;
        }

        public SonicParticleOptions build() {
            return this.options;
        }
    }
}


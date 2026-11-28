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
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Random
 */
package com.finderfeed.fdbosses.client.particles.arc_lightning;

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
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Random;

public class ArcLightningOptions
implements ParticleOptions {
    public static final ParticleOptions.Deserializer<ArcLightningOptions> DESERIALIZER = new ParticleOptions.Deserializer<ArcLightningOptions>(){

        public ArcLightningOptions fromCommand(ParticleType<ArcLightningOptions> p_123733_, StringReader p_123734_) throws CommandSyntaxException {
            return new ArcLightningOptions(null, null, 0, 0, 0, 0.0f, 0.0f, 0.0f, null);
        }

        public ArcLightningOptions fromNetwork(ParticleType<ArcLightningOptions> p_123735_, FriendlyByteBuf p_123736_) {
            return (ArcLightningOptions)STREAM_CODEC.fromNetwork(p_123736_);
        }
    };
    public static final Codec<ArcLightningOptions> CODEC = RecordCodecBuilder.create(p -> p.group((App)FDCodecs.VEC3.fieldOf("end").forGetter(v -> v.end), (App)FDCodecs.VEC3.fieldOf("endSpeed").forGetter(v -> v.end), (App)Codec.INT.fieldOf("lifetime").forGetter(v -> v.lifetime), (App)Codec.INT.fieldOf("seed").forGetter(v -> v.seed), (App)Codec.INT.fieldOf("lightningSegments").forGetter(v -> v.lightningSegments), (App)Codec.FLOAT.fieldOf("lightningRandomSpread").forGetter(v -> Float.valueOf(v.lightningRandomSpread)), (App)Codec.FLOAT.fieldOf("lightningWidth").forGetter(v -> Float.valueOf(v.lightningWidth)), (App)Codec.FLOAT.fieldOf("circleOffset").forGetter(v -> Float.valueOf(v.circleOffset)), (App)FDCodecs.COLOR.fieldOf("color").forGetter(v -> v.color)).apply((Applicative)p, ArcLightningOptions::new));
    private static final NetworkCodec<ArcLightningOptions> STREAM_CODEC = NetworkCodec.composite((NetworkCodec)NetworkCodec.VEC3, v -> v.end, (NetworkCodec)NetworkCodec.VEC3, v -> v.endSpeed, (NetworkCodec)NetworkCodec.INT, v -> v.lifetime, (NetworkCodec)NetworkCodec.INT, v -> v.seed, (NetworkCodec)NetworkCodec.INT, v -> v.lightningSegments, (NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.lightningRandomSpread), (NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.lightningWidth), (NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.circleOffset), (NetworkCodec)NetworkCodec.COLOR, v -> v.color, ArcLightningOptions::new);
    public ParticleType<?> particleType;
    public Vec3 end;
    public Vec3 endSpeed;
    public int lifetime;
    public int seed;
    public int lightningSegments;
    public float lightningWidth;
    public float lightningRandomSpread;
    public float circleOffset;
    public FDColor color;

    public ArcLightningOptions(ParticleType<?> particleType, Vec3 end, Vec3 endSpeed, int lifetime, int seed, int segments, float lightningRandomSpread, float lightningWidth, float circleOffset, FDColor color) {
        this.particleType = particleType;
        this.endSpeed = endSpeed;
        this.lightningWidth = lightningWidth;
        this.color = color;
        this.seed = seed;
        this.end = end;
        this.lifetime = lifetime;
        this.lightningSegments = segments;
        this.circleOffset = circleOffset;
        this.lightningRandomSpread = lightningRandomSpread;
    }

    public ArcLightningOptions(Vec3 end, Vec3 endSpeed, int lifetime, int seed, int segments, float lightningRandomSpread, float lightningWidth, float circleOffset, FDColor color) {
        this(null, end, endSpeed, lifetime, seed, segments, lightningRandomSpread, lightningWidth, circleOffset, color);
    }

    public static Builder builder(ParticleType<?> type) {
        return new Builder(type);
    }

    public ParticleType<?> m_6012_() {
        return this.particleType;
    }

    public void m_7711_(FriendlyByteBuf p_123732_) {
        STREAM_CODEC.toNetwork(p_123732_, (Object)this);
    }

    public String m_5942_() {
        return "zhopa";
    }

    public static class Builder {
        private ArcLightningOptions options = new ArcLightningOptions(Vec3.f_82478_, Vec3.f_82478_, 10, 2313, 10, 0.5f, 0.25f, 0.0f, new FDColor(0.0f, 0.0f, 1.0f, 1.0f));

        public Builder(ParticleType<?> type) {
            this.options.seed = new Random().nextInt(2543532) + 134234;
            this.options.particleType = type;
        }

        public Builder seed(int seed) {
            this.options.seed = seed;
            return this;
        }

        public Builder circleOffset(float offset) {
            this.options.circleOffset = offset;
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

        public Builder width(float width) {
            this.options.lightningWidth = width;
            return this;
        }

        public Builder lightningSpread(float spread) {
            this.options.lightningRandomSpread = spread;
            return this;
        }

        public Builder segments(int segments) {
            this.options.lightningSegments = Mth.m_14045_((int)segments, (int)2, (int)Integer.MAX_VALUE);
            return this;
        }

        public Builder lifetime(int lifetime) {
            this.options.lifetime = lifetime;
            return this;
        }

        public Builder end(double x, double y, double z) {
            return this.end(new Vec3(x, y, z));
        }

        public Builder end(Vec3 end) {
            this.options.end = end;
            return this;
        }

        public Builder endSpeed(double x, double y, double z) {
            return this.endSpeed(new Vec3(x, y, z));
        }

        public Builder endSpeed(Vec3 end) {
            this.options.endSpeed = end;
            return this;
        }

        public ArcLightningOptions build() {
            return this.options;
        }
    }
}


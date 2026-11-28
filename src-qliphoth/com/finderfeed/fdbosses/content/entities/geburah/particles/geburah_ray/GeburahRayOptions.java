/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.particle.EmptyParticleProcessor
 *  com.finderfeed.fdlib.systems.particle.ParticleProcessor
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
package com.finderfeed.fdbosses.content.entities.geburah.particles.geburah_ray;

import com.finderfeed.fdbosses.client.BossParticles;
import com.finderfeed.fdlib.systems.particle.EmptyParticleProcessor;
import com.finderfeed.fdlib.systems.particle.ParticleProcessor;
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

public class GeburahRayOptions
implements ParticleOptions {
    public static final ParticleOptions.Deserializer<GeburahRayOptions> DESERIALIZER = new ParticleOptions.Deserializer<GeburahRayOptions>(){

        public GeburahRayOptions fromCommand(ParticleType<GeburahRayOptions> p_123733_, StringReader p_123734_) throws CommandSyntaxException {
            return GeburahRayOptions.builder().build();
        }

        public GeburahRayOptions fromNetwork(ParticleType<GeburahRayOptions> p_123735_, FriendlyByteBuf p_123736_) {
            return (GeburahRayOptions)STREAM_CODEC.fromNetwork(p_123736_);
        }
    };
    public static final Codec<GeburahRayOptions> CODEC = RecordCodecBuilder.create(p -> p.group((App)ParticleProcessor.CODEC.fieldOf("particleProcessor").forGetter(v -> v.particleProcessor), (App)FDCodecs.VEC3.fieldOf("rayEnd").forGetter(v -> v.rayEnd), (App)AlphaOptions.CODEC.fieldOf("alpha").forGetter(v -> v.rayOptions), (App)FDCodecs.COLOR.fieldOf("color").forGetter(v -> v.color), (App)Codec.FLOAT.fieldOf("rayWidth").forGetter(v -> Float.valueOf(v.rayWidth))).apply((Applicative)p, (particleProcessor, rayEnd, alpha, color, width) -> {
        GeburahRayOptions ray = new GeburahRayOptions();
        ray.particleProcessor = particleProcessor;
        ray.rayOptions = alpha;
        ray.color = color;
        ray.rayWidth = width.floatValue();
        ray.rayEnd = rayEnd;
        return ray;
    }));
    public static final NetworkCodec<GeburahRayOptions> STREAM_CODEC = NetworkCodec.composite((NetworkCodec)ParticleProcessor.STREAM_CODEC, v -> v.particleProcessor, (NetworkCodec)NetworkCodec.VEC3, v -> v.rayEnd, (NetworkCodec)AlphaOptions.STREAM_CODEC, v -> v.rayOptions, (NetworkCodec)NetworkCodec.COLOR, v -> v.color, (NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.rayWidth), (particleProcessor, rayEnd, alpha, color, width) -> {
        GeburahRayOptions ray = new GeburahRayOptions();
        ray.particleProcessor = particleProcessor;
        ray.rayOptions = alpha;
        ray.color = color;
        ray.rayWidth = width.floatValue();
        ray.rayEnd = rayEnd;
        return ray;
    });
    public ParticleProcessor<?> particleProcessor = new EmptyParticleProcessor();
    public Vec3 rayEnd = Vec3.f_82478_;
    public AlphaOptions rayOptions = AlphaOptions.builder().in(3).stay(10).out(3).build();
    public FDColor color = new FDColor(1.0f, 0.0f, 0.0f, 1.0f);
    public float rayWidth = 0.25f;

    public static MapCodec<GeburahRayOptions> mapCodec() {
        return CODEC.xmap(x -> x, x -> x).fieldOf("options");
    }

    public ParticleType<?> m_6012_() {
        return BossParticles.GEBURAH_RAY_ATTACK.get();
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
        private GeburahRayOptions rayOptions = new GeburahRayOptions();

        public Builder processor(ParticleProcessor<?> processor) {
            this.rayOptions.particleProcessor = processor;
            return this;
        }

        public Builder end(Vec3 rayEnd) {
            this.rayOptions.rayEnd = rayEnd;
            return this;
        }

        public Builder in(int in) {
            this.rayOptions.rayOptions.inTime = in;
            return this;
        }

        public Builder out(int out) {
            this.rayOptions.rayOptions.outTime = out;
            return this;
        }

        public Builder time(int in, int stay, int out) {
            this.rayOptions.rayOptions.inTime = in;
            this.rayOptions.rayOptions.stayTime = stay;
            this.rayOptions.rayOptions.outTime = out;
            return this;
        }

        public Builder stay(int stay) {
            this.rayOptions.rayOptions.stayTime = stay;
            return this;
        }

        public Builder color(FDColor color) {
            this.rayOptions.color = color;
            return this;
        }

        public Builder color(float r, float g, float b, float a) {
            return this.color(new FDColor(r, g, b, a));
        }

        public Builder color(float r, float g, float b) {
            return this.color(new FDColor(r, g, b, 1.0f));
        }

        public Builder color(int r, int g, int b, int a) {
            return this.color(new FDColor((float)r / 255.0f, (float)g / 255.0f, (float)b / 255.0f, (float)a / 255.0f));
        }

        public Builder color(int r, int g, int b) {
            return this.color(new FDColor((float)r / 255.0f, (float)g / 255.0f, (float)b / 255.0f, 1.0f));
        }

        public Builder width(float lwidth) {
            this.rayOptions.rayWidth = lwidth;
            return this;
        }

        public GeburahRayOptions build() {
            return this.rayOptions;
        }
    }
}


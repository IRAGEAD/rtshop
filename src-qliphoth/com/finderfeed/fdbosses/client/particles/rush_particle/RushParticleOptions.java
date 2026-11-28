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
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.client.particles.rush_particle;

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
import net.minecraft.world.phys.Vec3;

public class RushParticleOptions
implements ParticleOptions {
    public static final ParticleOptions.Deserializer<RushParticleOptions> DESERIALIZER = new ParticleOptions.Deserializer<RushParticleOptions>(){

        public RushParticleOptions fromCommand(ParticleType<RushParticleOptions> p_123733_, StringReader p_123734_) throws CommandSyntaxException {
            return new RushParticleOptions(null, null, 1.0f, 1.0f, 1);
        }

        public RushParticleOptions fromNetwork(ParticleType<RushParticleOptions> p_123735_, FriendlyByteBuf p_123736_) {
            return (RushParticleOptions)STREAM_CODEC.fromNetwork(p_123736_);
        }
    };
    public static final Codec<RushParticleOptions> CODEC = RecordCodecBuilder.create(p -> p.group((App)FDCodecs.VEC3.fieldOf("rushDirection").forGetter(v -> v.rushDirection), (App)FDCodecs.COLOR.fieldOf("color").forGetter(v -> v.color), (App)Codec.FLOAT.fieldOf("length").forGetter(v -> Float.valueOf(v.length)), (App)Codec.FLOAT.fieldOf("width").forGetter(v -> Float.valueOf(v.width)), (App)Codec.INT.fieldOf("lifetime").forGetter(v -> v.lifetime)).apply((Applicative)p, RushParticleOptions::new));
    public static final NetworkCodec<RushParticleOptions> STREAM_CODEC = NetworkCodec.composite((NetworkCodec)NetworkCodec.VEC3, v -> v.rushDirection, (NetworkCodec)NetworkCodec.COLOR, v -> v.color, (NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.length), (NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.width), (NetworkCodec)NetworkCodec.INT, v -> v.lifetime, RushParticleOptions::new);
    private Vec3 rushDirection;
    private FDColor color;
    private int lifetime;
    private float length;
    private float width;

    public RushParticleOptions(Vec3 rushDirection, FDColor color, float length, float width, int lifetime) {
        this.rushDirection = rushDirection;
        this.color = color;
        this.lifetime = lifetime;
        this.length = length;
        this.width = width;
    }

    public ParticleType<?> m_6012_() {
        return BossParticles.RUSH_PARTICLE.get();
    }

    public void m_7711_(FriendlyByteBuf p_123732_) {
        STREAM_CODEC.toNetwork(p_123732_, (Object)this);
    }

    public String m_5942_() {
        return "zhopa";
    }

    public float getLength() {
        return this.length;
    }

    public Vec3 getRushDirection() {
        return this.rushDirection;
    }

    public int getLifetime() {
        return this.lifetime;
    }

    public FDColor getColor() {
        return this.color;
    }

    public float getWidth() {
        return this.width;
    }
}


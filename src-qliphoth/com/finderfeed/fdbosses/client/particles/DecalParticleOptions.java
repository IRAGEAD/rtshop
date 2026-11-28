/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec
 *  com.finderfeed.fdlib.util.FDCodecs
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
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.registries.ForgeRegistries
 */
package com.finderfeed.fdbosses.client.particles;

import com.finderfeed.fdbosses.client.BossParticles;
import com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec;
import com.finderfeed.fdlib.util.FDCodecs;
import com.finderfeed.fdlib.util.client.particles.options.AlphaOptions;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

public class DecalParticleOptions
implements ParticleOptions {
    public static final ParticleOptions.Deserializer<DecalParticleOptions> DESERIALIZER = new ParticleOptions.Deserializer<DecalParticleOptions>(){

        public DecalParticleOptions fromCommand(ParticleType<DecalParticleOptions> particleType, StringReader stringReader) throws CommandSyntaxException {
            return new DecalParticleOptions(BossParticles.GEBURAH_RAY_DECAL.get(), new Vec3(0.0, 1.0, 0.0), new AlphaOptions(), 0.25f);
        }

        public DecalParticleOptions fromNetwork(ParticleType<DecalParticleOptions> particleType, FriendlyByteBuf friendlyByteBuf) {
            return (DecalParticleOptions)STREAM_CODEC.fromNetwork(friendlyByteBuf);
        }
    };
    public static final Codec<DecalParticleOptions> CODEC = RecordCodecBuilder.create(p -> p.group((App)BuiltInRegistries.f_257034_.m_194605_().fieldOf("particle_type").forGetter(v -> v.particleType), (App)FDCodecs.VEC3.fieldOf("direction").forGetter(v -> v.direction), (App)AlphaOptions.CODEC.fieldOf("alphaOptions").forGetter(v -> v.alphaOptions), (App)Codec.FLOAT.fieldOf("size").forGetter(v -> Float.valueOf(v.size))).apply((Applicative)p, DecalParticleOptions::new));
    public static final NetworkCodec<DecalParticleOptions> STREAM_CODEC = NetworkCodec.composite((NetworkCodec)NetworkCodec.registry(() -> ForgeRegistries.PARTICLE_TYPES), v -> v.particleType, (NetworkCodec)NetworkCodec.VEC3, v -> v.direction, (NetworkCodec)AlphaOptions.STREAM_CODEC, v -> v.alphaOptions, (NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.size), DecalParticleOptions::new);
    private ParticleType<?> particleType;
    private Vec3 direction;
    private AlphaOptions alphaOptions;
    private float size;

    public DecalParticleOptions(ParticleType<?> particleType, Vec3 direction, AlphaOptions alphaOptions, float size) {
        this.particleType = particleType;
        this.direction = direction;
        this.alphaOptions = alphaOptions;
        this.size = size;
    }

    public float getSize() {
        return this.size;
    }

    public Vec3 getDirection() {
        return this.direction;
    }

    public AlphaOptions getAlphaOptions() {
        return this.alphaOptions;
    }

    public ParticleType<?> getParticleType() {
        return this.particleType;
    }

    public ParticleType<?> m_6012_() {
        return this.particleType;
    }

    public void m_7711_(FriendlyByteBuf friendlyByteBuf) {
        STREAM_CODEC.toNetwork(friendlyByteBuf, (Object)this);
    }

    public String m_5942_() {
        return "";
    }
}


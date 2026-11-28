/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec
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
 *  net.minecraftforge.registries.ForgeRegistries
 */
package com.finderfeed.fdbosses.client.particles;

import com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec;
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
import net.minecraftforge.registries.ForgeRegistries;

public class GravityParticleOptions
implements ParticleOptions {
    public static final ParticleOptions.Deserializer<GravityParticleOptions> DESERIALIZER = new ParticleOptions.Deserializer<GravityParticleOptions>(){

        public GravityParticleOptions fromCommand(ParticleType<GravityParticleOptions> p_123733_, StringReader p_123734_) throws CommandSyntaxException {
            return null;
        }

        public GravityParticleOptions fromNetwork(ParticleType<GravityParticleOptions> p_123735_, FriendlyByteBuf p_123736_) {
            return (GravityParticleOptions)STREAM_CODEC.fromNetwork(p_123736_);
        }
    };
    public static final Codec<GravityParticleOptions> CODEC = RecordCodecBuilder.create(p -> p.group((App)BuiltInRegistries.f_257034_.m_194605_().fieldOf("type").forGetter(v -> v.particleType), (App)Codec.INT.fieldOf("lifetime").forGetter(v -> v.lifetime), (App)Codec.FLOAT.fieldOf("quadSize").forGetter(v -> Float.valueOf(v.quadSize)), (App)Codec.FLOAT.fieldOf("gravity").forGetter(v -> Float.valueOf(v.gravity)), (App)Codec.FLOAT.fieldOf("rotationModifier").forGetter(v -> Float.valueOf(v.rotationModifier)), (App)Codec.BOOL.fieldOf("fadeOut").forGetter(v -> v.fadeOut)).apply((Applicative)p, GravityParticleOptions::new));
    public static final NetworkCodec<GravityParticleOptions> STREAM_CODEC = NetworkCodec.composite((NetworkCodec)NetworkCodec.registry(() -> ForgeRegistries.PARTICLE_TYPES), v -> v.particleType, (NetworkCodec)NetworkCodec.INT, v -> v.lifetime, (NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.quadSize), (NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.gravity), (NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.rotationModifier), (NetworkCodec)NetworkCodec.BOOL, v -> v.fadeOut, GravityParticleOptions::new);
    private ParticleType<?> particleType;
    private int lifetime;
    private float gravity;
    private float rotationModifier;
    private float quadSize;
    private boolean fadeOut;

    public GravityParticleOptions(ParticleType<?> particleType, int lifetime, float quadSize, float gravity, float rotationModifier, boolean fadeOut) {
        this.particleType = particleType;
        this.gravity = gravity;
        this.rotationModifier = rotationModifier;
        this.quadSize = quadSize;
        this.fadeOut = fadeOut;
        this.lifetime = lifetime;
    }

    public float getGravity() {
        return this.gravity;
    }

    public float getQuadSize() {
        return this.quadSize;
    }

    public float getRotationModifier() {
        return this.rotationModifier;
    }

    public boolean isFadeOut() {
        return this.fadeOut;
    }

    public int getLifetime() {
        return this.lifetime;
    }

    public ParticleType<?> m_6012_() {
        return this.particleType;
    }

    public void m_7711_(FriendlyByteBuf p_123732_) {
        STREAM_CODEC.toNetwork(p_123732_, (Object)this);
    }

    public String m_5942_() {
        return "";
    }
}


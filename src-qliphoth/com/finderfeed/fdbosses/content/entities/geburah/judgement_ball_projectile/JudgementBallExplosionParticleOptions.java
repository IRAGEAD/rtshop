/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec
 *  com.finderfeed.fdlib.util.FDCodecs
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
package com.finderfeed.fdbosses.content.entities.geburah.judgement_ball_projectile;

import com.finderfeed.fdbosses.client.BossParticles;
import com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec;
import com.finderfeed.fdlib.util.FDCodecs;
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

public class JudgementBallExplosionParticleOptions
implements ParticleOptions {
    public static final ParticleOptions.Deserializer<JudgementBallExplosionParticleOptions> DESERIALIZER = new ParticleOptions.Deserializer<JudgementBallExplosionParticleOptions>(){

        public JudgementBallExplosionParticleOptions fromCommand(ParticleType<JudgementBallExplosionParticleOptions> p_123733_, StringReader p_123734_) throws CommandSyntaxException {
            return new JudgementBallExplosionParticleOptions(new Vec3(0.0, 1.0, 0.0), 10, 1.0f);
        }

        public JudgementBallExplosionParticleOptions fromNetwork(ParticleType<JudgementBallExplosionParticleOptions> p_123735_, FriendlyByteBuf p_123736_) {
            return (JudgementBallExplosionParticleOptions)STREAM_CODEC.fromNetwork(p_123736_);
        }
    };
    public static final Codec<JudgementBallExplosionParticleOptions> MAP_CODEC = RecordCodecBuilder.create(v -> v.group((App)FDCodecs.VEC3.fieldOf("movement").forGetter(a -> a.movement), (App)Codec.INT.fieldOf("lifetime").forGetter(a -> a.lifetime), (App)Codec.FLOAT.fieldOf("size").forGetter(a -> Float.valueOf(a.size))).apply((Applicative)v, JudgementBallExplosionParticleOptions::new));
    public static final NetworkCodec<JudgementBallExplosionParticleOptions> STREAM_CODEC = NetworkCodec.composite((NetworkCodec)NetworkCodec.VEC3, v -> v.movement, (NetworkCodec)NetworkCodec.INT, v -> v.lifetime, (NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.size), JudgementBallExplosionParticleOptions::new);
    public Vec3 movement;
    public float size;
    public int lifetime;

    public JudgementBallExplosionParticleOptions(Vec3 movement, int lifetime, float size) {
        this.movement = movement;
        this.size = size;
        this.lifetime = lifetime;
    }

    public ParticleType<?> m_6012_() {
        return BossParticles.JUDGEMENT_BALL_EXPLOSION.get();
    }

    public void m_7711_(FriendlyByteBuf p_123732_) {
        STREAM_CODEC.toNetwork(p_123732_, (Object)this);
    }

    public String m_5942_() {
        return "";
    }
}


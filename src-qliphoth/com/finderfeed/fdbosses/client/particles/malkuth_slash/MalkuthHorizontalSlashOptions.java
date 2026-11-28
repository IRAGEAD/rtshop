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
package com.finderfeed.fdbosses.client.particles.malkuth_slash;

import com.finderfeed.fdbosses.client.BossParticles;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthAttackType;
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

public class MalkuthHorizontalSlashOptions
implements ParticleOptions {
    public static final ParticleOptions.Deserializer<MalkuthHorizontalSlashOptions> DESERIALIZER = new ParticleOptions.Deserializer<MalkuthHorizontalSlashOptions>(){

        public MalkuthHorizontalSlashOptions fromCommand(ParticleType<MalkuthHorizontalSlashOptions> p_123733_, StringReader p_123734_) throws CommandSyntaxException {
            return new MalkuthHorizontalSlashOptions(MalkuthAttackType.FIRE, null, 1.0f, 1.0f, 1);
        }

        public MalkuthHorizontalSlashOptions fromNetwork(ParticleType<MalkuthHorizontalSlashOptions> p_123735_, FriendlyByteBuf p_123736_) {
            return (MalkuthHorizontalSlashOptions)STREAM_CODEC.fromNetwork(p_123736_);
        }
    };
    public static final NetworkCodec<MalkuthHorizontalSlashOptions> STREAM_CODEC = NetworkCodec.composite((NetworkCodec)NetworkCodec.STRING, v -> v.attackType.name(), (NetworkCodec)NetworkCodec.VEC3, v -> v.slashDirection, (NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.slashWidth), (NetworkCodec)NetworkCodec.INT, v -> v.lifetime, (NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.rotation), (type, dir, width, lifetime, rotation) -> {
        MalkuthHorizontalSlashOptions options = new MalkuthHorizontalSlashOptions(MalkuthAttackType.valueOf(type), (Vec3)dir, width.floatValue(), rotation.floatValue(), (int)lifetime);
        return options;
    });
    public static final Codec<MalkuthHorizontalSlashOptions> CODEC = RecordCodecBuilder.create(p -> p.group((App)Codec.STRING.fieldOf("attackType").forGetter(v -> v.attackType.name()), (App)FDCodecs.VEC3.fieldOf("slashDirection").forGetter(v -> v.slashDirection), (App)Codec.FLOAT.fieldOf("slashWidth").forGetter(v -> Float.valueOf(v.slashWidth)), (App)Codec.INT.fieldOf("lifetime").forGetter(v -> v.lifetime), (App)Codec.FLOAT.fieldOf("rotation").forGetter(v -> Float.valueOf(v.rotation))).apply((Applicative)p, (type, dir, width, lifetime, rotation) -> {
        MalkuthHorizontalSlashOptions options = new MalkuthHorizontalSlashOptions(MalkuthAttackType.valueOf(type), (Vec3)dir, width.floatValue(), rotation.floatValue(), (int)lifetime);
        return options;
    }));
    private MalkuthAttackType attackType;
    private Vec3 slashDirection;
    private float slashWidth;
    private float rotation;
    private int lifetime;

    public MalkuthHorizontalSlashOptions(MalkuthAttackType malkuthAttackType, Vec3 slashDirection, float slashWidth, float rotation, int lifetime) {
        this.attackType = malkuthAttackType;
        this.slashDirection = slashDirection;
        this.slashWidth = slashWidth;
        this.lifetime = lifetime;
        this.rotation = rotation;
    }

    public float getRotation() {
        return this.rotation;
    }

    public int getLifetime() {
        return this.lifetime;
    }

    public float getSlashWidth() {
        return this.slashWidth;
    }

    public MalkuthAttackType getAttackType() {
        return this.attackType;
    }

    public Vec3 getSlashDirection() {
        return this.slashDirection;
    }

    public ParticleType<?> m_6012_() {
        return BossParticles.MALKUTH_HORIZONTAL_SLASH.get();
    }

    public void m_7711_(FriendlyByteBuf p_123732_) {
        STREAM_CODEC.toNetwork(p_123732_, (Object)this);
    }

    public String m_5942_() {
        return "zhopa";
    }
}


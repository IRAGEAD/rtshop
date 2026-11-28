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
package com.finderfeed.fdbosses.client.particles.arc_preparation_particle;

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

public class ArcAttackPreparationParticleOptions
implements ParticleOptions {
    public static final ParticleOptions.Deserializer<ArcAttackPreparationParticleOptions> DESERIALIZER = new ParticleOptions.Deserializer<ArcAttackPreparationParticleOptions>(){

        public ArcAttackPreparationParticleOptions fromCommand(ParticleType<ArcAttackPreparationParticleOptions> p_123733_, StringReader p_123734_) throws CommandSyntaxException {
            return new ArcAttackPreparationParticleOptions(null, 1.0f, 1.0f, 1, 1, 1, 1.0f, 1.0f, 1.0f, 1.0f);
        }

        public ArcAttackPreparationParticleOptions fromNetwork(ParticleType<ArcAttackPreparationParticleOptions> p_123735_, FriendlyByteBuf p_123736_) {
            return (ArcAttackPreparationParticleOptions)STREAM_CODEC.fromNetwork(p_123736_);
        }
    };
    public static final NetworkCodec<ArcAttackPreparationParticleOptions> STREAM_CODEC = NetworkCodec.composite((NetworkCodec)NetworkCodec.VEC3, v -> v.horizontalDirection, (NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.length), (NetworkCodec)NetworkCodec.FLOAT, v -> Float.valueOf(v.halfAttackAngle), (NetworkCodec)NetworkCodec.INT, v -> v.attackChargeTime, (NetworkCodec)NetworkCodec.INT, v -> v.fadeIn, (NetworkCodec)NetworkCodec.INT, v -> v.fadeOut, (NetworkCodec)NetworkCodec.COLOR, v -> v.color, (vec3, aFloat, aFloat2, integer, integer2, integer3, fdColor) -> {
        ArcAttackPreparationParticleOptions options = new ArcAttackPreparationParticleOptions((Vec3)vec3, aFloat.floatValue(), aFloat2.floatValue(), (int)integer, (int)integer2, (int)integer3, fdColor.r, fdColor.g, fdColor.b, fdColor.a);
        return options;
    });
    public static final Codec<ArcAttackPreparationParticleOptions> CODEC = RecordCodecBuilder.create(p -> p.group((App)FDCodecs.VEC3.fieldOf("horizontal_direction").forGetter(v -> v.horizontalDirection), (App)Codec.FLOAT.fieldOf("length").forGetter(v -> Float.valueOf(v.length)), (App)Codec.FLOAT.fieldOf("halfAttackAngle").forGetter(v -> Float.valueOf(v.halfAttackAngle)), (App)Codec.INT.fieldOf("attachChargeTime").forGetter(v -> v.attackChargeTime), (App)Codec.INT.fieldOf("fadeIn").forGetter(v -> v.fadeIn), (App)Codec.INT.fieldOf("fadeOut").forGetter(v -> v.fadeOut), (App)FDCodecs.COLOR.fieldOf("color").forGetter(v -> v.color)).apply((Applicative)p, (vec3, aFloat, aFloat2, integer, integer2, integer3, fdColor) -> {
        ArcAttackPreparationParticleOptions options = new ArcAttackPreparationParticleOptions((Vec3)vec3, aFloat.floatValue(), aFloat2.floatValue(), (int)integer, (int)integer2, (int)integer3, fdColor.r, fdColor.g, fdColor.b, fdColor.a);
        return options;
    }));
    private Vec3 horizontalDirection;
    private float length;
    private float halfAttackAngle;
    private int attackChargeTime;
    private int fadeIn;
    private int fadeOut;
    private FDColor color;

    public ArcAttackPreparationParticleOptions(Vec3 horizontalDirection, float length, float halfAttackAngle, int attackChargeTime, int fadeIn, int fadeOut, float r, float g, float b, float a) {
        this.horizontalDirection = horizontalDirection;
        this.length = length;
        this.halfAttackAngle = halfAttackAngle;
        this.fadeIn = fadeIn;
        this.fadeOut = fadeOut;
        this.attackChargeTime = attackChargeTime;
        this.color = new FDColor(r, g, b, a);
    }

    public FDColor getColor() {
        return this.color;
    }

    public float getLength() {
        return this.length;
    }

    public float getHalfAttackAngle() {
        return this.halfAttackAngle;
    }

    public Vec3 getHorizontalDirection() {
        return this.horizontalDirection;
    }

    public int getAttackChargeTime() {
        return this.attackChargeTime;
    }

    public int getFadeIn() {
        return this.fadeIn;
    }

    public int getFadeOut() {
        return this.fadeOut;
    }

    public ParticleType<?> m_6012_() {
        return BossParticles.ARC_ATTACK_PREPARATION_PARTICLE.get();
    }

    public void m_7711_(FriendlyByteBuf p_123732_) {
        STREAM_CODEC.toNetwork(p_123732_, (Object)this);
    }

    public String m_5942_() {
        return "zhopa";
    }
}


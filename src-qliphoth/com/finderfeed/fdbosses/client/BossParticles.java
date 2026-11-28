/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  net.minecraft.core.particles.ParticleType
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraftforge.registries.DeferredRegister
 */
package com.finderfeed.fdbosses.client;

import com.finderfeed.fdbosses.client.particles.DecalParticleOptions;
import com.finderfeed.fdbosses.client.particles.DecalParticleType;
import com.finderfeed.fdbosses.client.particles.GravityOptionsParticleType;
import com.finderfeed.fdbosses.client.particles.GravityParticleOptions;
import com.finderfeed.fdbosses.client.particles.arc_lightning.ArcLightningOptions;
import com.finderfeed.fdbosses.client.particles.arc_preparation_particle.ArcAttackPreparationParticleOptions;
import com.finderfeed.fdbosses.client.particles.chesed_attack_ray.ChesedRayOptions;
import com.finderfeed.fdbosses.client.particles.colored_jumping_particles.ColoredJumpingParticleOptions;
import com.finderfeed.fdbosses.client.particles.malkuth_slash.MalkuthHorizontalSlashOptions;
import com.finderfeed.fdbosses.client.particles.rush_particle.RushParticleOptions;
import com.finderfeed.fdbosses.client.particles.smoke_particle.BigSmokeParticleOptions;
import com.finderfeed.fdbosses.client.particles.sonic_particle.SonicParticleOptions;
import com.finderfeed.fdbosses.client.particles.square_preparation_particle.RectanglePreparationParticleOptions;
import com.finderfeed.fdbosses.client.particles.stripe_particle.StripeParticleOptions;
import com.finderfeed.fdbosses.content.entities.geburah.judgement_ball_projectile.JudgementBallExplosionParticleOptions;
import com.finderfeed.fdbosses.content.entities.geburah.particles.geburah_ray.GeburahRayOptions;
import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.registries.DeferredRegister;

public class BossParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create((ResourceKey)Registries.f_256890_, (String)"fdbosses");
    public static final Supplier<ParticleType<ChesedRayOptions>> CHESED_RAY_ATTACK = PARTICLES.register("chesed_ray", () -> new ParticleType<ChesedRayOptions>(true, ChesedRayOptions.DESERIALIZER){

        public Codec<ChesedRayOptions> m_7652_() {
            return ChesedRayOptions.CODEC;
        }
    });
    public static final Supplier<ParticleType<ArcLightningOptions>> ARC_LIGHTNING = PARTICLES.register("arc", () -> new ParticleType<ArcLightningOptions>(true, ArcLightningOptions.DESERIALIZER){

        public Codec<ArcLightningOptions> m_7652_() {
            return ArcLightningOptions.CODEC;
        }
    });
    public static final Supplier<ParticleType<SonicParticleOptions>> SONIC_PARTICLE = PARTICLES.register("sonic", () -> new ParticleType<SonicParticleOptions>(true, SonicParticleOptions.DESERIALIZER){

        public Codec<SonicParticleOptions> m_7652_() {
            return SonicParticleOptions.CODEC;
        }
    });
    public static final Supplier<ParticleType<BigSmokeParticleOptions>> BIS_SMOKE = PARTICLES.register("big_smoke", () -> new ParticleType<BigSmokeParticleOptions>(true, BigSmokeParticleOptions.DESERIALIZER){

        public Codec<BigSmokeParticleOptions> m_7652_() {
            return BigSmokeParticleOptions.CODEC;
        }
    });
    public static final Supplier<ParticleType<RushParticleOptions>> RUSH_PARTICLE = PARTICLES.register("rush_particle", () -> new ParticleType<RushParticleOptions>(true, RushParticleOptions.DESERIALIZER){

        public Codec<RushParticleOptions> m_7652_() {
            return RushParticleOptions.CODEC;
        }
    });
    public static final Supplier<ParticleType<MalkuthHorizontalSlashOptions>> MALKUTH_HORIZONTAL_SLASH = PARTICLES.register("malkuth_horizontal_slash", () -> new ParticleType<MalkuthHorizontalSlashOptions>(true, MalkuthHorizontalSlashOptions.DESERIALIZER){

        public Codec<MalkuthHorizontalSlashOptions> m_7652_() {
            return MalkuthHorizontalSlashOptions.CODEC;
        }
    });
    public static final Supplier<ParticleType<ArcAttackPreparationParticleOptions>> ARC_ATTACK_PREPARATION_PARTICLE = PARTICLES.register("arc_attack_preparation_particle", () -> new ParticleType<ArcAttackPreparationParticleOptions>(true, ArcAttackPreparationParticleOptions.DESERIALIZER){

        public Codec<ArcAttackPreparationParticleOptions> m_7652_() {
            return ArcAttackPreparationParticleOptions.CODEC;
        }
    });
    public static final Supplier<ParticleType<RectanglePreparationParticleOptions>> RECTANGLE_PREPARATION_PARTICLE = PARTICLES.register("rectangle_preparation_particle", () -> new ParticleType<RectanglePreparationParticleOptions>(true, RectanglePreparationParticleOptions.DESERIALIZER){

        public Codec<RectanglePreparationParticleOptions> m_7652_() {
            return RectanglePreparationParticleOptions.CODEC;
        }
    });
    public static final Supplier<ParticleType<StripeParticleOptions>> STRIPE_PARTICLE = PARTICLES.register("stripe_particle", () -> new ParticleType<StripeParticleOptions>(true, StripeParticleOptions.DESERIALIZER){

        public Codec<StripeParticleOptions> m_7652_() {
            return StripeParticleOptions.CODEC;
        }
    });
    public static final Supplier<ParticleType<GravityParticleOptions>> FLAME_WITH_STONE = PARTICLES.register("flame_with_stone", () -> new GravityOptionsParticleType(true));
    public static final Supplier<ParticleType<GravityParticleOptions>> ICE_CHUNK = PARTICLES.register("ice_chunk", () -> new GravityOptionsParticleType(true));
    public static final Supplier<ParticleType<GeburahRayOptions>> GEBURAH_RAY_ATTACK = PARTICLES.register("geburah_ray", () -> new ParticleType<GeburahRayOptions>(true, GeburahRayOptions.DESERIALIZER){

        public Codec<GeburahRayOptions> m_7652_() {
            return GeburahRayOptions.CODEC;
        }
    });
    public static final Supplier<ParticleType<JudgementBallExplosionParticleOptions>> JUDGEMENT_BALL_EXPLOSION = PARTICLES.register("judgement_ball_explosion", () -> new ParticleType<JudgementBallExplosionParticleOptions>(true, JudgementBallExplosionParticleOptions.DESERIALIZER){

        public Codec<JudgementBallExplosionParticleOptions> m_7652_() {
            return JudgementBallExplosionParticleOptions.MAP_CODEC;
        }
    });
    public static final Supplier<ParticleType<DecalParticleOptions>> GEBURAH_RAY_DECAL = PARTICLES.register("geburah_ray_decal", () -> new DecalParticleType(true));
    public static final Supplier<ParticleType<ColoredJumpingParticleOptions>> COLORED_JUMPING_PARTICLE = PARTICLES.register("colored_jumping_particles", () -> new ParticleType<ColoredJumpingParticleOptions>(true, ColoredJumpingParticleOptions.DESERIALIZER){

        public Codec<ColoredJumpingParticleOptions> m_7652_() {
            return ColoredJumpingParticleOptions.MAP_CODEC;
        }
    });
}


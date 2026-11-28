/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.FDLibCalls
 *  com.finderfeed.fdlib.systems.bedrock.animations.Animation
 *  com.finderfeed.fdlib.systems.bedrock.animations.Animation$LoopMode
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDEntity
 *  com.finderfeed.fdlib.systems.shake.FDShakeData
 *  com.finderfeed.fdlib.systems.shake.PositionedScreenShakePacket
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions
 *  com.finderfeed.fdlib.util.math.ComplexEasingFunction
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  net.minecraft.commands.arguments.EntityAnchorArgument$Anchor
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec2
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.entities.geburah.justice_hammer;

import com.finderfeed.fdbosses.BossTargetFinder;
import com.finderfeed.fdbosses.BossUtil;
import com.finderfeed.fdbosses.client.particles.square_preparation_particle.RectanglePreparationParticleOptions;
import com.finderfeed.fdbosses.init.BossAnims;
import com.finderfeed.fdbosses.init.BossConfigs;
import com.finderfeed.fdbosses.init.BossDamageSources;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdlib.FDLibCalls;
import com.finderfeed.fdlib.systems.bedrock.animations.Animation;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDEntity;
import com.finderfeed.fdlib.systems.shake.FDShakeData;
import com.finderfeed.fdlib.systems.shake.PositionedScreenShakePacket;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions;
import com.finderfeed.fdlib.util.math.ComplexEasingFunction;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import java.util.List;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class JusticeHammerAttack
extends FDEntity {
    public static int ATTACK_WIDTH = 7;
    public static int ATTACK_LENGTH = 13;
    public static ComplexEasingFunction EASING;
    protected static ComplexEasingFunction EASING2;

    public static JusticeHammerAttack summon(Level level, Vec3 pos, Vec3 direction) {
        JusticeHammerAttack justiceHammerAttack = new JusticeHammerAttack(BossEntities.JUSTICE_HAMMER.get(), level);
        ATTACK_LENGTH = 12;
        ATTACK_WIDTH = 9;
        justiceHammerAttack.m_146884_(pos);
        justiceHammerAttack.m_7618_(EntityAnchorArgument.Anchor.FEET, pos.m_82549_(direction.m_82542_(1.0, 0.0, 1.0).m_82541_().m_82490_(200.0)));
        level.m_7967_((Entity)justiceHammerAttack);
        return justiceHammerAttack;
    }

    public JusticeHammerAttack(EntityType<?> type, Level level) {
        super(type, level);
        if (EASING == null || EASING2 != null) {
            // empty if block
        }
        int animTime = ((Animation)BossAnims.JUSTICE_HAMMER_SMACK.get()).getAnimTime();
        int fadeOut = 10;
        int fadeIn = 30;
        EASING = ComplexEasingFunction.builder().addArea((float)fadeIn, FDEasings::easeIn).addArea((float)(animTime - fadeIn), FDEasings::one).addArea((float)fadeOut, FDEasings::reversedLinear).build();
        EASING2 = ComplexEasingFunction.builder().addArea((float)(animTime - 5), f -> Float.valueOf(0.0f)).addArea(2.0f, FDEasings::easeOut).addArea((float)(fadeOut - 1), FDEasings::reversedEaseOut).build();
        this.getAnimationSystem().startAnimation("MAIN", AnimationTicker.builder(BossAnims.JUSTICE_HAMMER_SMACK).setLoopMode(Animation.LoopMode.HOLD_ON_LAST_FRAME).build());
    }

    public void m_8119_() {
        super.m_8119_();
        int animTime = ((Animation)BossAnims.JUSTICE_HAMMER_SMACK.get()).getAnimTime();
        int attackTime = animTime - 3;
        if (!this.m_9236_().f_46443_) {
            if (this.f_19797_ == attackTime - 3) {
                this.smack(ATTACK_LENGTH, ATTACK_WIDTH);
            } else if (this.f_19797_ == animTime + 10) {
                this.m_142467_(Entity.RemovalReason.DISCARDED);
            }
            if (this.f_19797_ == 1) {
                Vec3 direction = this.m_20154_().m_82542_(1.0, 0.0, 1.0).m_82541_();
                Vec3 pos = this.m_20182_().m_82546_(direction.m_82490_((double)((float)ATTACK_LENGTH / 2.0f))).m_82520_(0.0, -0.1, 0.0);
                RectanglePreparationParticleOptions options = new RectanglePreparationParticleOptions(direction, ATTACK_LENGTH, (float)ATTACK_WIDTH / 2.0f, ((Animation)BossAnims.JUSTICE_HAMMER_SMACK.get()).getAnimTime() - 10, 10, 10, 0.3f, 0.8f, 1.0f, 0.15f);
                FDLibCalls.sendParticles((ServerLevel)((ServerLevel)this.m_9236_()), (ParticleOptions)options, (Vec3)pos.m_82520_(0.0, (double)0.15f, 0.0), (double)120.0);
            }
        } else if (this.f_19797_ == attackTime - 1) {
            float r = 0.3f;
            float g = 0.7f;
            float b = 1.0f;
            for (int i = 0; i < 200; ++i) {
                BallParticleOptions ballParticleOptions = BallParticleOptions.builder().size(0.15f).scalingOptions(0, 0, 5 + this.f_19796_.m_188503_(5)).friction(0.6f).brightness(2).color(r, g, b).build();
                float xOffset = this.f_19796_.m_188501_() * (float)ATTACK_WIDTH - (float)ATTACK_WIDTH / 2.0f;
                float zOffset = this.f_19796_.m_188501_() * (float)ATTACK_LENGTH - (float)ATTACK_LENGTH / 2.0f;
                Vec3 offs = new Vec3((double)xOffset, 0.0, (double)zOffset).m_82524_((float)Math.toRadians(-this.m_146908_()));
                this.m_9236_().m_7106_((ParticleOptions)ballParticleOptions, this.m_20185_() + offs.f_82479_, this.m_20186_(), this.m_20189_() + offs.f_82481_, 0.0, (double)this.f_19796_.m_188501_(), 0.0);
            }
        }
    }

    private void smack(float lsize, float wsize) {
        Vec3 direction = this.m_20154_().m_82542_(1.0, 0.0, 1.0).m_82541_();
        Vec3 pos = this.m_20182_().m_82546_(direction.m_82490_((double)(lsize / 2.0f))).m_82520_(0.0, -0.1, 0.0);
        List<LivingEntity> entities = BossTargetFinder.getEntitiesInHorizontalBox(LivingEntity.class, this.m_9236_(), pos, new Vec2((float)direction.f_82479_, (float)direction.f_82481_), lsize, wsize, 4.0f, living -> true);
        float damage = BossUtil.transformDamage(this.m_9236_(), BossConfigs.BOSS_CONFIG.get().geburahConfig.justiceHammerDamage);
        for (LivingEntity entity : entities) {
            entity.m_6469_(BossDamageSources.GEBURAH_JUSTICE_HAMMER_SOURCE, damage);
        }
        PositionedScreenShakePacket.send((ServerLevel)((ServerLevel)this.m_9236_()), (FDShakeData)FDShakeData.builder().outTime(7).stayTime(0).inTime(0).amplitude(0.5f).frequency(10.0f).build(), (Vec3)this.m_20182_(), (double)60.0);
    }

    protected void m_8097_() {
    }
}


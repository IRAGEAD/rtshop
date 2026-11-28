/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.bedrock.animations.Animation
 *  com.finderfeed.fdlib.systems.bedrock.animations.Animation$LoopMode
 *  com.finderfeed.fdlib.systems.bedrock.animations.TransitionAnimation
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationSystem
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDEntity
 *  com.finderfeed.fdlib.systems.bedrock.models.FDModel
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.EntityDataSerializers
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.util.Mth
 *  net.minecraft.world.effect.MobEffect
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 */
package com.finderfeed.fdbosses.content.entities.chesed_boss.ray_reflector;

import com.finderfeed.fdbosses.init.BossAnims;
import com.finderfeed.fdbosses.init.BossEffects;
import com.finderfeed.fdbosses.init.BossModels;
import com.finderfeed.fdlib.systems.bedrock.animations.Animation;
import com.finderfeed.fdlib.systems.bedrock.animations.TransitionAnimation;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationSystem;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDEntity;
import com.finderfeed.fdlib.systems.bedrock.models.FDModel;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions;
import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class ChesedRayReflector
extends FDEntity {
    public static final String ACTIVE_LAYER = "ACTIVATION";
    public static final EntityDataAccessor<Boolean> ACTIVE = SynchedEntityData.m_135353_(ChesedRayReflector.class, (EntityDataSerializer)EntityDataSerializers.f_135035_);
    private static float ANIMATION_SPEED = 1.0f;
    private int activeTicker = 0;
    public int activeTickerO;
    private static FDModel clientModel;

    public ChesedRayReflector(EntityType<?> type, Level level) {
        super(type, level);
        if (clientModel == null && level.f_46443_) {
            clientModel = new FDModel(BossModels.CHESED_RAY_REFLECTOR.get());
        }
    }

    public void m_8119_() {
        super.m_8119_();
        this.activeTickerO = this.activeTicker;
        if (!this.m_9236_().f_46443_) {
            this.handleActivation(20.0f);
            this.handleAnimation();
        } else {
            this.handleClientTicker();
            this.activationParticles();
        }
    }

    private void activationParticles() {
        float modelScale = 0.8f;
        int animTime = ((Animation)BossAnims.RAY_REFLECTOR_ACTIVATE.get()).getAnimTime();
        int connectTime = animTime / 2 + 2;
        if (this.isActivating() && this.activeTicker <= connectTime) {
            float speed = 0.0f;
            float friction = 1.0f;
            int count = 10;
            float particleSize = 0.1f;
            if (this.activeTicker == connectTime) {
                speed = 10.0f;
                count = 100;
                particleSize = 0.15f;
                friction = 0.7f;
            }
            Vector3f pos1 = this.getModelPartPosition((Entity)this, "bone", clientModel).mul(modelScale);
            Vector3f pos2 = this.getModelPartPosition((Entity)this, "p1", clientModel).mul(modelScale);
            Vector3f dir1 = this.transformPoint((Entity)this, new Vector3f(1.0f, 0.0f, 0.0f), "bone", clientModel).mul(modelScale).sub((Vector3fc)pos1).mul(-1.0f);
            Vector3f dir2 = this.transformPoint((Entity)this, new Vector3f(1.0f, 0.0f, 0.0f), "p1", clientModel).mul(modelScale).sub((Vector3fc)pos2);
            Vector3f hdir1 = this.transformPoint((Entity)this, new Vector3f(0.0f, 1.0f, 0.0f), "bone", clientModel).mul(modelScale).sub((Vector3fc)pos1);
            Vector3f hdir2 = this.transformPoint((Entity)this, new Vector3f(0.0f, 1.0f, 0.0f), "p1", clientModel).mul(modelScale).sub((Vector3fc)pos2);
            float voffs = 1.25f;
            float voffs2 = 1.4f;
            for (int i = 0; i < count; ++i) {
                float r1 = this.f_19796_.m_188501_() * voffs * 2.0f - voffs;
                float r2 = this.f_19796_.m_188501_() * voffs2 * 2.0f - voffs2;
                float r1hmod = 1.0f - Math.abs(r1) / voffs;
                float r2hmod = 1.0f - Math.abs(r2) / voffs;
                float r1hrandom = this.f_19796_.m_188501_() * 0.5f - 0.4f;
                float r2hrandom = this.f_19796_.m_188501_() * 0.5f - 0.4f;
                Vec3 speed1 = new Vec3((double)(-hdir1.x * speed), 0.0, (double)(-hdir1.z * speed)).m_82541_();
                Vec3 speed2 = new Vec3((double)(-hdir2.x * speed), 0.0, (double)(-hdir2.z * speed)).m_82541_();
                this.m_9236_().m_6493_((ParticleOptions)BallParticleOptions.builder().size(particleSize).friction(friction * this.f_19796_.m_188501_()).scalingOptions(0, 0, 10 + this.f_19796_.m_188503_(5)).color(100, 230, 255).build(), true, (double)pos1.x + this.m_20185_() + (double)(dir1.x * r1) - (double)(hdir1.x * (voffs - 1.0f)) + (double)(hdir1.x * voffs * r1hmod) + (double)(hdir1.x * r1hrandom), (double)pos1.y + this.m_20186_() + (double)(dir1.y * r1) - (double)(hdir1.y * (voffs - 1.0f)) + (double)(hdir1.y * voffs * r1hmod) + (double)(hdir1.y * r1hrandom), (double)pos1.z + this.m_20189_() + (double)(dir1.z * r1) - (double)(hdir1.z * (voffs - 1.0f)) + (double)(hdir1.z * voffs * r1hmod) + (double)(hdir1.z * r1hrandom), speed1.f_82479_, speed1.f_82480_, speed1.f_82481_);
                this.m_9236_().m_6493_((ParticleOptions)BallParticleOptions.builder().size(particleSize).friction(friction * this.f_19796_.m_188501_()).scalingOptions(0, 0, 10 + this.f_19796_.m_188503_(5)).color(100, 230, 255).build(), true, (double)pos2.x + this.m_20185_() + (double)(dir2.x * r2) - (double)(hdir2.x * (voffs - 1.0f)) + (double)(hdir2.x * voffs2 * r2hmod) + (double)(hdir2.x * r2hrandom), (double)pos2.y + this.m_20186_() + (double)(dir2.y * r2) - (double)(hdir2.y * (voffs - 1.0f)) + (double)(hdir2.y * voffs2 * r2hmod) + (double)(hdir2.y * r2hrandom), (double)pos2.z + this.m_20189_() + (double)(dir2.z * r2) - (double)(hdir2.z * (voffs - 1.0f)) + (double)(hdir2.z * voffs2 * r2hmod) + (double)(hdir2.z * r2hrandom), speed2.f_82479_, speed2.f_82480_, speed2.f_82481_);
            }
        }
    }

    public boolean isActivating() {
        AnimationTicker ticker = this.getAnimationSystem().getTicker(ACTIVE_LAYER);
        if (ticker == null) {
            return false;
        }
        Animation animation = ticker.getAnimation();
        if (animation instanceof TransitionAnimation) {
            TransitionAnimation animation1 = (TransitionAnimation)animation;
            animation = animation1.getTransitionTo();
        }
        if (animation != BossAnims.RAY_REFLECTOR_ACTIVATE.get()) {
            return false;
        }
        return !ticker.isReversed();
    }

    public int getAnimationLength() {
        return Math.round((float)((Animation)BossAnims.RAY_REFLECTOR_ACTIVATE.get()).getAnimTime() / ANIMATION_SPEED);
    }

    private void handleClientTicker() {
        AnimationSystem system = this.getAnimationSystem();
        int activationAnimLength = this.getAnimationLength();
        AnimationTicker ticker = system.getTicker(ACTIVE_LAYER);
        if (ticker == null) {
            this.activeTicker = Mth.m_14045_((int)(this.activeTicker - 1), (int)0, (int)activationAnimLength);
            return;
        }
        Animation animation = ticker.getAnimation();
        if (animation instanceof TransitionAnimation) {
            TransitionAnimation transitionAnimation = (TransitionAnimation)animation;
            animation = transitionAnimation.getTransitionTo();
        }
        this.activeTicker = animation == BossAnims.RAY_REFLECTOR_ACTIVATE.get() ? (ticker.isReversed() ? Mth.m_14045_((int)(this.activeTicker - 1), (int)0, (int)activationAnimLength) : Mth.m_14045_((int)(this.activeTicker + 1), (int)0, (int)activationAnimLength)) : Mth.m_14045_((int)(this.activeTicker - 1), (int)0, (int)activationAnimLength);
    }

    private void handleAnimation() {
        AnimationSystem system = this.getAnimationSystem();
        int activationAnimLength = this.getAnimationLength();
        if (this.isActive()) {
            AnimationTicker animTicker = system.getTicker(ACTIVE_LAYER);
            if (animTicker == null) {
                system.startAnimation(ACTIVE_LAYER, AnimationTicker.builder(BossAnims.RAY_REFLECTOR_ACTIVATE).setSpeed(ANIMATION_SPEED).setToNullTransitionTime(0).build());
            }
            this.activeTicker = Mth.m_14045_((int)(this.activeTicker + 1), (int)0, (int)activationAnimLength);
        } else if (this.activeTicker != 0) {
            if (this.activeTicker != activationAnimLength) {
                this.activeTicker = Mth.m_14045_((int)(this.activeTicker + 1), (int)0, (int)activationAnimLength);
            } else {
                system.startAnimation(ACTIVE_LAYER, AnimationTicker.builder(BossAnims.RAY_REFLECTOR_ACTIVATE).setSpeed(ANIMATION_SPEED).setToNullTransitionTime(0).reversed().setLoopMode(Animation.LoopMode.ONCE).build());
                this.activeTicker = 0;
            }
        }
    }

    private void handleActivation(float detectionRadius) {
        AABB box = new AABB((double)(-detectionRadius), -1.0, (double)(-detectionRadius), (double)detectionRadius, 3.0, (double)detectionRadius).m_82383_(this.m_20182_());
        Vec3 hpos = this.m_20182_().m_82542_(1.0, 0.0, 1.0);
        List players = this.m_9236_().m_6443_(Player.class, box, player -> {
            if (!player.m_21023_((MobEffect)BossEffects.CHESED_ENERGIZED.get()) || player.m_5833_()) {
                return false;
            }
            Vec3 playerPos = player.m_20182_().m_82542_(1.0, 0.0, 1.0);
            double distance = playerPos.m_82554_(hpos);
            return distance <= (double)detectionRadius;
        });
        if (players.isEmpty()) {
            this.setActive(false);
        } else {
            this.setActive(true);
        }
    }

    public int getActiveTicker() {
        return this.activeTicker;
    }

    public void setActive(boolean active) {
        this.f_19804_.m_135381_(ACTIVE, (Object)active);
    }

    public boolean isActive() {
        return (Boolean)this.f_19804_.m_135370_(ACTIVE);
    }

    protected void m_8097_() {
        this.f_19804_.m_135372_(ACTIVE, (Object)false);
    }

    protected void m_7378_(CompoundTag tag) {
    }

    protected void m_7380_(CompoundTag tag) {
    }

    public boolean m_6087_() {
        return true;
    }
}


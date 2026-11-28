/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.nbt.AutoSerializable
 *  com.finderfeed.fdlib.nbt.SerializableField
 *  com.finderfeed.fdlib.systems.bedrock.animations.Animation
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDMob
 *  com.finderfeed.fdlib.systems.bedrock.models.FDModel
 *  com.finderfeed.fdlib.util.client.particles.FDBlockParticleOptions
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  net.minecraft.commands.arguments.EntityAnchorArgument$Anchor
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.util.Mth
 *  net.minecraft.world.damagesource.DamageSource
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.entity.ai.attributes.Attributes
 *  net.minecraft.world.entity.ai.goal.Goal
 *  net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 */
package com.finderfeed.fdbosses.content.entities.geburah.judgement_bird;

import com.finderfeed.fdbosses.content.entities.geburah.GeburahBossBuddy;
import com.finderfeed.fdbosses.content.entities.geburah.casts.GeburahRayCastingCircle;
import com.finderfeed.fdbosses.init.BossAnims;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdbosses.init.BossModels;
import com.finderfeed.fdlib.nbt.AutoSerializable;
import com.finderfeed.fdlib.nbt.SerializableField;
import com.finderfeed.fdlib.systems.bedrock.animations.Animation;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDMob;
import com.finderfeed.fdlib.systems.bedrock.models.FDModel;
import com.finderfeed.fdlib.util.client.particles.FDBlockParticleOptions;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import java.util.Optional;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class JudgementBirdEntity
extends FDMob
implements AutoSerializable,
GeburahBossBuddy {
    private static FDModel MODEL;
    private static float FLY_TO_TARGET_RADIUS;
    private static float ATTACK_RADIUS;
    @SerializableField
    private Vec3 targetPos;
    @SerializableField
    private int castingTicker = -1;
    @SerializableField
    private Vec3 castingTargetPos;
    @SerializableField
    private Vec3 noTargetFlyToPos;
    private AABB roostingBox;

    public static JudgementBirdEntity summon(Level level, Vec3 pos, Vec3 noTargetFlyToPos, AABB roostingBox) {
        JudgementBirdEntity entity = new JudgementBirdEntity(BossEntities.JUDGEMENT_BIRD.get(), level);
        entity.m_146884_(pos);
        entity.noTargetFlyToPos = noTargetFlyToPos;
        entity.roostingBox = roostingBox;
        level.m_7967_((Entity)entity);
        return entity;
    }

    public static JudgementBirdEntity summon(Level level, Vec3 pos, Vec3 noTargetFlyToPos) {
        return JudgementBirdEntity.summon(level, pos, noTargetFlyToPos, null);
    }

    public JudgementBirdEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        if (level.f_46443_ && MODEL == null) {
            MODEL = new FDModel(BossModels.JUDGEMENT_BIRD.get());
        }
        this.m_20242_(true);
    }

    protected void m_8099_() {
        super.m_8099_();
        this.f_21346_.m_25352_(0, (Goal)new NearestAttackableTargetGoal<Player>((Mob)this, Player.class, true){

            protected void m_26073_() {
                super.m_26073_();
                if (this.f_26050_ != null && !JudgementBirdEntity.this.isTargetInRoostingBox(this.f_26050_)) {
                    this.f_26050_ = null;
                }
            }
        });
    }

    public void m_8119_() {
        if (!this.m_9236_().f_46443_ && !this.m_21224_()) {
            if (this.m_5448_() != null) {
                this.checkTargetInRoostingBox(this.m_5448_());
            }
            this.flyAndAttack();
        }
        super.m_8119_();
    }

    private void flyAndAttack() {
        if (this.castingTargetPos == null) {
            this.castingTicker = -1;
        }
        if (this.castingTicker == -1) {
            Animation animation = (Animation)BossAnims.JUDGEMENT_BIRD_FLY.get();
            if (this.m_5448_() == null) {
                if (this.roostingBox != null && !this.roostingBox.m_82390_(this.m_20182_())) {
                    this.setMoveTargetPos(this.roostingBox.m_82399_());
                }
                if (this.targetPos != null) {
                    this.flyToPos(this.targetPos);
                }
            } else {
                LivingEntity target = this.m_5448_();
                Vec3 pos = this.getFlyToTargetPos(target);
                this.flyToPos(pos);
                this.m_7618_(EntityAnchorArgument.Anchor.FEET, target.m_20182_());
                if (this.m_20270_((Entity)target) < ATTACK_RADIUS) {
                    this.castingTicker = ((Animation)BossAnims.JUDGEMENT_BIRD_CAST.get()).getAnimTime();
                    this.castingTargetPos = target.m_20182_().m_82520_(0.0, (double)(target.m_20206_() / 2.0f), 0.0);
                    this.m_20256_(Vec3.f_82478_);
                }
            }
            this.getAnimationSystem().startAnimation("MAIN", AnimationTicker.builder((Animation)animation).build());
        } else {
            LivingEntity target = this.m_5448_();
            if (this.castingTicker > 30 && target != null) {
                this.castingTargetPos = target.m_20182_().m_82520_(0.0, (double)(target.m_20206_() / 2.0f), 0.0);
            }
            this.castingTicker = Mth.m_14045_((int)(this.castingTicker - 1), (int)0, (int)Integer.MAX_VALUE);
            this.getAnimationSystem().startAnimation("MAIN", AnimationTicker.builder(BossAnims.JUDGEMENT_BIRD_CAST).nextAnimation(AnimationTicker.builder(BossAnims.JUDGEMENT_BIRD_FLY).build()).build());
            this.m_7618_(EntityAnchorArgument.Anchor.FEET, this.castingTargetPos);
            if (this.castingTicker == 30) {
                Vec3 startPos = this.m_20182_();
                GeburahRayCastingCircle.summon(this.m_9236_(), startPos, this.castingTargetPos.m_82546_(startPos));
            }
            if (this.castingTicker == 0) {
                this.castingTargetPos = null;
                this.castingTicker = -1;
            }
        }
    }

    protected void m_6153_() {
        if (!this.m_9236_().m_5776_() && !this.m_213877_()) {
            this.m_5496_(SoundEvents.f_12442_, 1.0f, 0.5f);
            this.m_5496_(SoundEvents.f_144108_, 1.0f, 0.5f);
            this.m_5496_(SoundEvents.f_12115_, 1.0f, 0.5f);
            this.m_142687_(Entity.RemovalReason.KILLED);
            BallParticleOptions ballParticleOptions = BallParticleOptions.builder().size(0.25f).scalingOptions(0, 0, 10).color(0.3f, 0.7f, 1.0f).friction(0.7f).build();
            ((ServerLevel)this.m_9236_()).m_8767_((ParticleOptions)ballParticleOptions, this.m_20185_(), this.m_20186_() + (double)this.m_20206_(), this.m_20189_(), 70, (double)0.1f, (double)0.1f, (double)0.1f, (double)0.4f);
            FDBlockParticleOptions blockParticleOptions = FDBlockParticleOptions.builder().lifetime(40).state(Blocks.f_152550_).quadSizeMultiplier(0.75f + this.f_19796_.m_188501_() * 0.5f).build();
            ((ServerLevel)this.m_9236_()).m_8767_((ParticleOptions)blockParticleOptions, this.m_20185_(), this.m_20186_() + (double)this.m_20206_(), this.m_20189_(), 70, (double)0.1f, (double)0.1f, (double)0.1f, (double)0.4f);
        }
    }

    protected void m_6677_(DamageSource p_21493_) {
        this.m_5496_(SoundEvents.f_12442_, 1.0f, 0.5f);
        this.m_5496_(SoundEvents.f_144108_, 1.0f, 0.5f);
    }

    @Nullable
    protected SoundEvent m_5592_() {
        return null;
    }

    public boolean m_20068_() {
        return true;
    }

    private boolean isTargetInRoostingBox(LivingEntity target) {
        if (this.roostingBox != null) {
            Vec3 targetPos = this.getFlyToTargetPos(target);
            return new AABB(this.roostingBox.f_82288_, this.roostingBox.f_82289_ - (double)FLY_TO_TARGET_RADIUS, this.roostingBox.f_82290_, this.roostingBox.f_82291_, this.roostingBox.f_82292_ + (double)FLY_TO_TARGET_RADIUS, this.roostingBox.f_82293_).m_82390_(targetPos);
        }
        return true;
    }

    private void checkTargetInRoostingBox(LivingEntity target) {
        if (this.roostingBox != null && !this.isTargetInRoostingBox(target)) {
            this.setMoveTargetPos(this.noTargetFlyToPos);
            this.m_6710_(null);
        }
    }

    public void setMoveTargetPos(Vec3 targetPos) {
        Vec3 pos;
        this.targetPos = this.roostingBox == null ? targetPos : (this.roostingBox.m_82390_(targetPos) ? targetPos : ((pos = this.clipRoostingBox(targetPos)) == null ? this.roostingBox.m_82399_() : pos));
    }

    private Vec3 clipRoostingBox(Vec3 target) {
        Optional cl = this.roostingBox.m_82371_(target, this.m_20182_());
        if (cl.isEmpty()) {
            cl = this.roostingBox.m_82371_(this.m_20182_(), target);
            if (cl.isPresent()) {
                return (Vec3)cl.get();
            }
        } else {
            return (Vec3)cl.get();
        }
        return null;
    }

    private Vec3 getFlyToTargetPos(LivingEntity target) {
        Vec3 targetPos = target.m_20182_().m_82520_(0.0, (double)(target.m_20206_() / 2.0f), 0.0);
        Vec3 b = targetPos.m_82546_(this.m_20182_());
        double dist = b.m_82553_();
        Vec3 resultPos = dist > (double)FLY_TO_TARGET_RADIUS ? targetPos : this.m_20182_();
        if (this.roostingBox != null) {
            double y = Math.max(this.roostingBox.f_82289_, Math.min(resultPos.f_82480_, this.roostingBox.f_82292_));
            return new Vec3(resultPos.f_82479_, y, resultPos.f_82481_);
        }
        return resultPos;
    }

    private void flyToPos(Vec3 pos) {
        Vec3 between = pos.m_82546_(this.m_20182_());
        double speed = this.m_21133_(Attributes.f_22279_);
        double b = between.m_82553_();
        speed = FDMathUtil.lerp((double)0.0, (double)speed, (double)Mth.m_14036_((float)FDEasings.easeIn((float)((float)(b / Math.max(3.0, speed)))), (float)0.0f, (float)1.0f));
        Vec3 deltaMovement = between.m_82541_().m_82490_(speed);
        this.m_20256_(deltaMovement);
        this.m_7618_(EntityAnchorArgument.Anchor.FEET, pos);
    }

    public void m_7378_(CompoundTag tag) {
        super.m_7378_(tag);
        this.autoLoad(tag);
        if (tag.m_128441_("roostingBoxMinX")) {
            this.roostingBox = new AABB(tag.m_128459_("roostingBoxMinX"), tag.m_128459_("roostingBoxMinY"), tag.m_128459_("roostingBoxMinZ"), tag.m_128459_("roostingBoxMaxX"), tag.m_128459_("roostingBoxMaxY"), tag.m_128459_("roostingBoxMaxZ"));
        }
    }

    public void m_7380_(CompoundTag tag) {
        super.m_7380_(tag);
        this.autoSave(tag);
        if (this.roostingBox != null) {
            tag.m_128347_("roostingBoxMinX", this.roostingBox.f_82288_);
            tag.m_128347_("roostingBoxMinY", this.roostingBox.f_82289_);
            tag.m_128347_("roostingBoxMinZ", this.roostingBox.f_82290_);
            tag.m_128347_("roostingBoxMaxX", this.roostingBox.f_82291_);
            tag.m_128347_("roostingBoxMaxY", this.roostingBox.f_82292_);
            tag.m_128347_("roostingBoxMaxZ", this.roostingBox.f_82293_);
        }
    }

    public boolean m_21532_() {
        return true;
    }

    static {
        FLY_TO_TARGET_RADIUS = 6.5f;
        ATTACK_RADIUS = 7.0f;
    }
}


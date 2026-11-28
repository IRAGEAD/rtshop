/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.nbt.AutoSerializable
 *  com.finderfeed.fdlib.nbt.SerializableField
 *  com.finderfeed.fdlib.systems.shake.FDShakeData
 *  com.finderfeed.fdlib.systems.shake.PositionedScreenShakePacket
 *  com.finderfeed.fdlib.util.FDProjectile
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleTypes
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.damagesource.DamageSource
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Vector3f
 */
package com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_fireball;

import com.finderfeed.fdbosses.BossTargetFinder;
import com.finderfeed.fdbosses.BossUtil;
import com.finderfeed.fdbosses.client.BossParticles;
import com.finderfeed.fdbosses.client.particles.GravityParticleOptions;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthAttackType;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthDamageSource;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthEntity;
import com.finderfeed.fdbosses.init.BossDamageSources;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdbosses.init.BossEntityDataSerializers;
import com.finderfeed.fdbosses.init.BossSounds;
import com.finderfeed.fdlib.nbt.AutoSerializable;
import com.finderfeed.fdlib.nbt.SerializableField;
import com.finderfeed.fdlib.systems.shake.FDShakeData;
import com.finderfeed.fdlib.systems.shake.PositionedScreenShakePacket;
import com.finderfeed.fdlib.util.FDProjectile;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class MalkuthFireball
extends FDProjectile
implements AutoSerializable {
    public static final EntityDataAccessor<MalkuthAttackType> ATTACK_TYPE = SynchedEntityData.m_135353_(MalkuthFireball.class, (EntityDataSerializer)((EntityDataSerializer)BossEntityDataSerializers.MALKUTH_ATTACK_TYPE.get()));
    @SerializableField
    private Vec3 moveToPos = Vec3.f_82478_;
    @SerializableField
    private Vec3 targetPos = Vec3.f_82478_;
    @SerializableField
    private int movingToTargetTime = -1;
    @SerializableField
    private int currentMovingToTargetTime = 0;
    @SerializableField
    private Vec3 startedMovingFromPos = Vec3.f_82478_;
    @SerializableField
    private int accelerationTicks = 0;
    @SerializableField
    private float damage;

    public static MalkuthFireball summon(MalkuthAttackType type, Level level, Vec3 pos, Vec3 flyToPos, Vec3 targetPos, float damage) {
        MalkuthFireball malkuthFireball = new MalkuthFireball(BossEntities.MALKUTH_FIREBALL.get(), level);
        malkuthFireball.setAttackType(type);
        malkuthFireball.m_146884_(pos);
        malkuthFireball.moveToPos = flyToPos;
        malkuthFireball.targetPos = targetPos;
        malkuthFireball.damage = damage;
        malkuthFireball.doMove();
        level.m_7967_((Entity)malkuthFireball);
        return malkuthFireball;
    }

    public MalkuthFireball(EntityType<? extends FDProjectile> type, Level level) {
        super(type, level);
    }

    public void m_8119_() {
        super.m_8119_();
        if (this.f_19797_ > 1000) {
            this.m_142687_(Entity.RemovalReason.DISCARDED);
        }
        if (this.m_9236_().f_46443_) {
            this.particles();
        } else {
            this.doMove();
        }
    }

    private void doMove() {
        if (!this.isMovingToTarget()) {
            Vec3 target;
            float peakSpeed = 2.0f;
            float minspeed = 0.1f;
            float maxDist = 10.0f;
            Vec3 pos = this.m_20182_();
            double dist = pos.m_82554_(target = this.moveToPos);
            if (dist >= (double)minspeed) {
                Vec3 between = target.m_82546_(pos).m_82541_();
                float p = (float)FDMathUtil.clamp((double)(dist / (double)maxDist), (double)0.0, (double)1.0);
                float speedValue = FDMathUtil.lerp((float)minspeed, (float)peakSpeed, (float)p);
                Vec3 speed = between.m_82542_((double)speedValue, (double)speedValue, (double)speedValue);
                this.m_20256_(speed);
            } else {
                this.m_20256_(Vec3.f_82478_);
            }
        } else {
            if (this.accelerationTicks-- >= 0) {
                if (this.movingToTargetTime == this.currentMovingToTargetTime) {
                    this.m_20256_(Vec3.f_82478_);
                }
                Vec3 startPos = this.startedMovingFromPos;
                Vec3 targetPos = this.targetPos;
                Vec3 between = targetPos.m_82546_(startPos);
                double S = between.m_82553_();
                double t = (float)this.movingToTargetTime / 2.0f;
                double v = S / (t * 1.5);
                double a = v / t;
                Vec3 current = this.m_20184_();
                Vec3 next = current.m_82549_(between.m_82541_().m_82542_(a, a, a));
                this.m_20256_(next);
            }
            if (this.currentMovingToTargetTime-- <= 0) {
                this.explode();
            }
        }
    }

    public void explode() {
        if (this.getAttackType().isIce()) {
            this.m_9236_().m_6263_(null, this.m_20185_(), this.m_20186_(), this.m_20189_(), BossSounds.MALKUTH_ICE_FIREBALL_EXPLOSION.get(), SoundSource.HOSTILE, 1.0f, 1.0f);
        } else {
            this.m_9236_().m_6263_(null, this.m_20185_(), this.m_20186_(), this.m_20189_(), BossSounds.MALKUTH_FIREBALL_EXPLOSION.get(), SoundSource.HOSTILE, 1.0f, 1.0f);
        }
        BossUtil.malkuthFireballExplosionParticles((ServerLevel)this.m_9236_(), this.targetPos, this.getAttackType());
        PositionedScreenShakePacket.send((ServerLevel)((ServerLevel)this.m_9236_()), (FDShakeData)FDShakeData.builder().frequency(50.0f).amplitude(1.5f).inTime(0).stayTime(0).outTime(10).build(), (Vec3)this.targetPos, (double)10.0);
        List<LivingEntity> targets = BossTargetFinder.getEntitiesInCylinder(LivingEntity.class, this.m_9236_(), this.targetPos.m_82520_(0.0, -1.0, 0.0), 3.0f, 4.0f);
        for (LivingEntity target : targets) {
            target.m_6469_((DamageSource)new MalkuthDamageSource(BossDamageSources.MALKUTH_HELLSHAPER_SOURCE, this.getAttackType(), 70), this.damage);
        }
        this.m_142687_(Entity.RemovalReason.DISCARDED);
    }

    public void setMoveToTarget(int moveTime) {
        this.movingToTargetTime = moveTime;
        this.currentMovingToTargetTime = moveTime;
        this.accelerationTicks = moveTime / 2;
        this.startedMovingFromPos = this.m_20182_();
    }

    public boolean isMovingToTarget() {
        return this.movingToTargetTime != -1;
    }

    private void particles() {
        Vec3 between = new Vec3(this.m_20185_() - this.f_19854_, this.m_20186_() - this.f_19855_, this.m_20189_() - this.f_19856_);
        double length = between.m_82553_();
        Vec3 pos = this.m_20182_().m_82520_(0.0, (double)(this.m_20206_() / 2.0f), 0.0);
        float maxrad = 0.5f;
        float g = -0.001f;
        while ((double)g < length * (double)0.8f) {
            for (int i = 0; i < 10; ++i) {
                Vector3f color = MalkuthEntity.getAndRandomizeColor(this.getAttackType(), this.f_19796_);
                Object particleOptions = this.f_19796_.m_188501_() > 0.1f ? BallParticleOptions.builder().color(color.x, color.y, color.z).scalingOptions(0, 0, 10 + this.f_19796_.m_188503_(4)).brightness(1).build() : (this.getAttackType().isFire() ? ParticleTypes.f_123744_ : new GravityParticleOptions(BossParticles.ICE_CHUNK.get(), 20 + this.f_19796_.m_188503_(4), 0.5f + this.f_19796_.m_188501_() * 0.2f, 0.0f, 2.0f, true));
                float rndOut = maxrad / 2.0f + this.f_19796_.m_188501_() * maxrad / 2.0f;
                Vec3 rndOffs = new Vec3((double)(this.f_19796_.m_188501_() * 2.0f - 1.0f), (double)(this.f_19796_.m_188501_() * 2.0f - 1.0f), (double)(this.f_19796_.m_188501_() * 2.0f - 1.0f)).m_82541_().m_82542_((double)rndOut, (double)rndOut, (double)rndOut);
                Vec3 distOffset = between.m_82541_().m_82542_((double)g, (double)g, (double)g);
                Vec3 ppos = pos.m_82549_(rndOffs).m_82546_(distOffset);
                float vspeed = length == 0.0 ? -this.f_19796_.m_188501_() * 0.1f : 0.0f;
                this.m_9236_().m_6493_((ParticleOptions)particleOptions, true, ppos.f_82479_, ppos.f_82480_, ppos.f_82481_, 0.0, (double)vspeed, 0.0);
            }
            g += maxrad;
        }
    }

    public boolean m_6087_() {
        return true;
    }

    public MalkuthAttackType getAttackType() {
        return (MalkuthAttackType)((Object)this.f_19804_.m_135370_(ATTACK_TYPE));
    }

    public void setAttackType(MalkuthAttackType type) {
        this.f_19804_.m_135381_(ATTACK_TYPE, (Object)type);
    }

    protected void m_8097_() {
        super.m_8097_();
        this.f_19804_.m_135372_(ATTACK_TYPE, (Object)MalkuthAttackType.ICE);
    }

    public void m_7380_(CompoundTag tag) {
        super.m_7380_(tag);
        tag.m_128359_("mtype", ((MalkuthAttackType)((Object)this.f_19804_.m_135370_(ATTACK_TYPE))).name());
        this.autoSave(tag);
    }

    public void m_7378_(CompoundTag tag) {
        super.m_7378_(tag);
        if (tag.m_128441_("mtype")) {
            this.f_19804_.m_135381_(ATTACK_TYPE, (Object)MalkuthAttackType.valueOf(tag.m_128461_("mtype")));
        }
        this.autoLoad(tag);
    }
}


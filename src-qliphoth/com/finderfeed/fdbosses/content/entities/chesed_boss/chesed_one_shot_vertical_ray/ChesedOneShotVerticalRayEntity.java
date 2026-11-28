/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.FDLibCalls
 *  com.finderfeed.fdlib.nbt.AutoSerializable
 *  com.finderfeed.fdlib.nbt.SerializableField
 *  com.finderfeed.fdlib.systems.particle.CircleParticleProcessor
 *  com.finderfeed.fdlib.systems.particle.ParticleProcessor
 *  com.finderfeed.fdlib.systems.shake.FDShakeData
 *  com.finderfeed.fdlib.systems.shake.PositionedScreenShakePacket
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.EntityDataSerializers
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.entities.chesed_boss.chesed_one_shot_vertical_ray;

import com.finderfeed.fdbosses.BossUtil;
import com.finderfeed.fdbosses.client.particles.chesed_attack_ray.ChesedRayOptions;
import com.finderfeed.fdbosses.content.entities.chesed_boss.ChesedBossBuddy;
import com.finderfeed.fdbosses.init.BossDamageSources;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdbosses.init.BossSounds;
import com.finderfeed.fdlib.FDLibCalls;
import com.finderfeed.fdlib.nbt.AutoSerializable;
import com.finderfeed.fdlib.nbt.SerializableField;
import com.finderfeed.fdlib.systems.particle.CircleParticleProcessor;
import com.finderfeed.fdlib.systems.particle.ParticleProcessor;
import com.finderfeed.fdlib.systems.shake.FDShakeData;
import com.finderfeed.fdlib.systems.shake.PositionedScreenShakePacket;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions;
import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ChesedOneShotVerticalRayEntity
extends Entity
implements AutoSerializable {
    public static final EntityDataAccessor<Integer> ATTACK_PREPARATION_TIME = SynchedEntityData.m_135353_(ChesedOneShotVerticalRayEntity.class, (EntityDataSerializer)EntityDataSerializers.f_135028_);
    public static final EntityDataAccessor<Float> HEIGHT = SynchedEntityData.m_135353_(ChesedOneShotVerticalRayEntity.class, (EntityDataSerializer)EntityDataSerializers.f_135029_);
    @SerializableField
    private float damage;
    @SerializableField
    private float damageRadius = 2.5f;
    public boolean softerSound = false;

    public ChesedOneShotVerticalRayEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public static ChesedOneShotVerticalRayEntity summon(Level level, Vec3 pos, float damage, float height, int attackPrepTime) {
        ChesedOneShotVerticalRayEntity entity = new ChesedOneShotVerticalRayEntity(BossEntities.CHESED_ONE_SHOT_VERTICAL_RAY_ATTACK.get(), level);
        entity.setDamage(damage);
        entity.setHeight(height);
        entity.m_146884_(pos);
        entity.setAttackPreparationTime(attackPrepTime);
        level.m_7967_((Entity)entity);
        return entity;
    }

    public void m_8119_() {
        super.m_8119_();
        if (!this.m_9236_().f_46443_) {
            if (this.f_19797_ == this.getAttackPreparationTime()) {
                this.m_9236_().m_6263_(null, this.m_20185_(), this.m_20186_(), this.m_20189_(), BossSounds.FAST_LIGHTNING_STRIKE.get(), SoundSource.HOSTILE, this.softerSound ? 0.25f : 1.0f, 1.0f);
                this.rayParticleAndShake();
                this.doDamage();
                this.m_142687_(Entity.RemovalReason.DISCARDED);
            } else if (this.f_19797_ > this.getAttackPreparationTime()) {
                this.m_142687_(Entity.RemovalReason.DISCARDED);
            }
        } else {
            int attackPreparationTime = this.getAttackPreparationTime();
            if ((float)this.f_19797_ < (float)attackPreparationTime / 2.0f) {
                this.prepareParticles(attackPreparationTime / 2);
            } else if (this.f_19797_ == attackPreparationTime - 1) {
                this.boomParticles(this.m_20182_());
            }
        }
    }

    private void boomParticles(Vec3 pos) {
        for (int i = 0; i < 50; ++i) {
            BallParticleOptions options = BallParticleOptions.builder().color(0.3f, 1.0f, 1.0f, 1.0f).scalingOptions(0, 10, 20 + this.f_19796_.m_188503_(3)).physics(false).size(0.5f).friction(0.9f).build();
            Vec3 speed = new Vec3((double)(0.6f * this.f_19796_.m_188501_()), 0.0, 0.0).m_82524_(this.f_19796_.m_188501_() * (float)Math.PI * 2.0f).m_82520_(0.0, (double)(this.f_19796_.m_188501_() * 0.3f), 0.0);
            this.m_9236_().m_6493_((ParticleOptions)options, true, pos.f_82479_, pos.f_82480_, pos.f_82481_, speed.f_82479_, speed.f_82480_, speed.f_82481_);
        }
        float sp = 2.0f;
        for (int c = 0; c < 4; ++c) {
            int i = 0;
            while ((float)i < this.getHeight()) {
                Vec3 spawn = this.m_20182_().m_82520_(0.0, (double)((float)i + this.f_19796_.m_188501_()) - 0.5, 0.0);
                Vec3 speed = new Vec3((double)sp * ((double)this.f_19796_.m_188501_() * 0.5 + 0.5), 0.0, 0.0).m_82524_((float)Math.PI * 2 * this.f_19796_.m_188501_());
                BallParticleOptions options = BallParticleOptions.builder().size(0.5f).color(0.3f, 1.0f, 1.0f, 1.0f).physics(false).friction(0.4f).scalingOptions(0, 0, 10 + this.f_19796_.m_188503_(4)).build();
                this.m_9236_().m_6493_((ParticleOptions)options, true, spawn.f_82479_, spawn.f_82480_, spawn.f_82481_, speed.f_82479_, speed.f_82480_, speed.f_82481_);
                ++i;
            }
        }
    }

    private void prepareParticles(int lifetime) {
        float p = (float)this.f_19797_ / (float)lifetime;
        for (int c = 0; c < 2; ++c) {
            int i = 0;
            while ((float)i < this.getHeight()) {
                Vec3 center = this.m_20182_().m_82520_(0.0, (double)((float)i + this.f_19796_.m_188501_()) - 0.5, 0.0);
                Vec3 spawnOffset = new Vec3(2.0, 0.0, 0.0).m_82524_((float)Math.PI * 2 * this.f_19796_.m_188501_());
                Vec3 spawn = center.m_82549_(spawnOffset);
                BallParticleOptions options = BallParticleOptions.builder().particleProcessor((ParticleProcessor)new CircleParticleProcessor(center, true, true, 1.0f)).size(0.5f * Math.max(0.05f, p)).color(0.3f, 1.0f, 1.0f, 1.0f).physics(false).scalingOptions(lifetime, 0, 0).build();
                this.m_9236_().m_6493_((ParticleOptions)options, true, spawn.f_82479_, spawn.f_82480_, spawn.f_82481_, 0.0, 0.0, 0.0);
                ++i;
            }
        }
    }

    private void rayParticleAndShake() {
        Vec3 p = this.m_20182_();
        Vec3 end = p.m_82520_(0.0, (double)this.getHeight(), 0.0);
        ChesedRayOptions options = ChesedRayOptions.builder().time(0, 7, 5).lightningColor(90, 180, 255).color(100, 255, 255).end(end).width(0.5f).build();
        FDLibCalls.sendParticles((ServerLevel)((ServerLevel)this.m_9236_()), (ParticleOptions)options, (Vec3)p, (double)60.0);
        PositionedScreenShakePacket.send((ServerLevel)((ServerLevel)this.m_9236_()), (FDShakeData)FDShakeData.builder().frequency(5.0f).amplitude(5.0f).inTime(0).stayTime(0).outTime(5).build(), (Vec3)p, (double)10.0);
        FDLibCalls.sendParticles((ServerLevel)((ServerLevel)this.m_9236_()), (ParticleOptions)BallParticleOptions.builder().size(20.0f).scalingOptions(1, 0, 3).color(150, 230, 255).build(), (Vec3)p, (double)60.0);
    }

    private void doDamage() {
        AABB box = new AABB((double)(-this.damageRadius), 0.0, (double)(-this.damageRadius), (double)this.damageRadius, (double)this.getHeight(), (double)this.damageRadius).m_82383_(this.m_20182_());
        List list = this.m_9236_().m_6443_(LivingEntity.class, box, BossUtil.entityInVerticalRadiusPredicate(this.m_20182_(), this.damageRadius));
        for (LivingEntity entity : list) {
            if (entity instanceof ChesedBossBuddy) continue;
            entity.m_6469_(BossDamageSources.CHESED_VERTICAL_RAY_SOURCE, this.getDamage());
        }
    }

    public float getDamageRadius() {
        return this.damageRadius;
    }

    public void setDamageRadius(float damageRadius) {
        this.damageRadius = damageRadius;
    }

    public float getHeight() {
        return ((Float)this.f_19804_.m_135370_(HEIGHT)).floatValue();
    }

    public float getDamage() {
        return this.damage;
    }

    public void setHeight(float height) {
        this.f_19804_.m_135381_(HEIGHT, (Object)Float.valueOf(height));
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setAttackPreparationTime(int attackPreparationTime) {
        this.f_19804_.m_135381_(ATTACK_PREPARATION_TIME, (Object)attackPreparationTime);
    }

    public int getAttackPreparationTime() {
        return (Integer)this.f_19804_.m_135370_(ATTACK_PREPARATION_TIME);
    }

    protected void m_8097_() {
        this.f_19804_.m_135372_(ATTACK_PREPARATION_TIME, (Object)20);
        this.f_19804_.m_135372_(HEIGHT, (Object)Float.valueOf(40.0f));
    }

    protected void m_7378_(CompoundTag tag) {
        this.autoLoad(tag);
        this.setAttackPreparationTime(tag.m_128451_("prepareTime"));
        this.setHeight(tag.m_128457_("height"));
    }

    protected void m_7380_(CompoundTag tag) {
        this.autoSave(tag);
        tag.m_128405_("prepareTime", this.getAttackPreparationTime());
        tag.m_128350_("height", this.getHeight());
    }
}


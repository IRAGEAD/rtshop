/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.FDHelpers
 *  com.finderfeed.fdlib.nbt.AutoSerializable
 *  com.finderfeed.fdlib.nbt.SerializableField
 *  com.finderfeed.fdlib.network.FDPacketHandler
 *  com.finderfeed.fdlib.util.FDColor
 *  com.finderfeed.fdlib.util.FDProjectile
 *  com.finderfeed.fdlib.util.ProjectileMovementPath
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleTypes
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.EntityDataSerializers
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.world.damagesource.DamageSource
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.projectile.AbstractHurtingProjectile
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.network.PacketDistributor
 *  org.joml.Vector3f
 */
package com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_boulder;

import com.finderfeed.fdbosses.client.BossParticles;
import com.finderfeed.fdbosses.client.particles.GravityParticleOptions;
import com.finderfeed.fdbosses.client.particles.stripe_particle.StripeParticleOptions;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthAttackType;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthBossBuddy;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthDamageSource;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthEntity;
import com.finderfeed.fdbosses.init.BossDamageSources;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdbosses.init.BossEntityDataSerializers;
import com.finderfeed.fdbosses.packets.SlamParticlesPacket;
import com.finderfeed.fdlib.FDHelpers;
import com.finderfeed.fdlib.nbt.AutoSerializable;
import com.finderfeed.fdlib.nbt.SerializableField;
import com.finderfeed.fdlib.network.FDPacketHandler;
import com.finderfeed.fdlib.util.FDColor;
import com.finderfeed.fdlib.util.FDProjectile;
import com.finderfeed.fdlib.util.ProjectileMovementPath;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.joml.Vector3f;

public class MalkuthBoulderEntity
extends FDProjectile
implements AutoSerializable {
    public static final EntityDataAccessor<MalkuthAttackType> MALKUTH_ATTACK_TYPE = SynchedEntityData.m_135353_(MalkuthBoulderEntity.class, (EntityDataSerializer)((EntityDataSerializer)BossEntityDataSerializers.MALKUTH_ATTACK_TYPE.get()));
    public static final EntityDataAccessor<Integer> PREPARE_TIME = SynchedEntityData.m_135353_(MalkuthBoulderEntity.class, (EntityDataSerializer)EntityDataSerializers.f_135028_);
    public static final EntityDataAccessor<Float> PREPARE_HEIGHT = SynchedEntityData.m_135353_(MalkuthBoulderEntity.class, (EntityDataSerializer)EntityDataSerializers.f_135029_);
    @SerializableField
    private ProjectileMovementPath movementPath;
    @SerializableField
    private boolean shouldMoveToTarget = false;
    @SerializableField
    private float damage = 0.0f;
    private boolean removeNextTick = false;

    public static MalkuthBoulderEntity summon(Level level, Vec3 pos, int prepareTime, float prepareHeight, ProjectileMovementPath movementPath, MalkuthAttackType malkuthAttackType, float damage) {
        MalkuthBoulderEntity malkuthBoulderEntity = new MalkuthBoulderEntity(BossEntities.MALKUTH_BOULDER.get(), level);
        malkuthBoulderEntity.m_146884_(pos);
        malkuthBoulderEntity.movementPath = movementPath;
        malkuthBoulderEntity.f_19804_.m_135381_(PREPARE_TIME, (Object)prepareTime);
        malkuthBoulderEntity.f_19804_.m_135381_(PREPARE_HEIGHT, (Object)Float.valueOf(prepareHeight));
        malkuthBoulderEntity.setMalkuthAttackType(malkuthAttackType);
        malkuthBoulderEntity.damage = damage;
        level.m_7967_((Entity)malkuthBoulderEntity);
        return malkuthBoulderEntity;
    }

    public MalkuthBoulderEntity(EntityType<? extends AbstractHurtingProjectile> type, Level level) {
        super(type, level);
        this.f_19794_ = true;
        this.m_20242_(true);
    }

    public void m_8119_() {
        if (!this.m_9236_().f_46443_ && this.removeNextTick) {
            this.m_142687_(Entity.RemovalReason.DISCARDED);
        }
        super.m_8119_();
        if (this.m_9236_().f_46443_) {
            Vec3 between = this.m_20182_().m_82546_(new Vec3(this.f_19854_, this.f_19855_, this.f_19856_));
            Vec3 nb = between.m_82541_();
            double dist = 0.0;
            double b = between.m_82553_();
            do {
                if (this.f_19797_ <= this.getPrepareTime() / 2) continue;
                int particleCount = this.m_20184_().m_82553_() > 0.0 ? 20 : 5;
                for (int i = 0; i < particleCount; ++i) {
                    float rnd;
                    Object options = this.getMalkuthAttackType().isFire() ? ((double)(rnd = this.f_19796_.m_188501_()) > 0.5 ? new GravityParticleOptions(BossParticles.FLAME_WITH_STONE.get(), 20 + this.f_19796_.m_188503_(4), 0.15f + this.f_19796_.m_188501_() * 0.2f, 1.5999999f, 2.0f, true) : ParticleTypes.f_123756_) : new GravityParticleOptions(BossParticles.ICE_CHUNK.get(), 20 + this.f_19796_.m_188503_(4), 0.5f + this.f_19796_.m_188501_() * 0.2f, 1.5999999f, 2.0f, true);
                    if (this.m_20184_().m_82553_() > 0.0 && this.f_19796_.m_188501_() > 0.3f) {
                        if (this.getMalkuthAttackType().isIce()) {
                            rc = this.f_19796_.m_188501_() * 0.2f;
                            float gc = 0.7f + this.f_19796_.m_188501_() * 0.1f;
                            bc = 0.9f + this.f_19796_.m_188501_() * 0.1f;
                            options = BallParticleOptions.builder().color(rc, gc, bc).size(0.2f + this.f_19796_.m_188501_() * 0.2f).scalingOptions(0, 0, 20).friction(0.7f).build();
                        } else {
                            rc = 0.9f + this.f_19796_.m_188501_() * 0.1f;
                            float gc = 0.2f + this.f_19796_.m_188501_() * 0.2f;
                            bc = this.f_19796_.m_188501_() * 0.2f;
                            options = BallParticleOptions.builder().color(rc, gc, bc).size(0.2f + this.f_19796_.m_188501_() * 0.2f).scalingOptions(0, 0, 20).friction(0.8f).build();
                        }
                    }
                    Vec3 rnd2 = this.m_20182_().m_82520_((double)this.f_19796_.m_188501_() * 3.5 - 1.75, (double)this.f_19796_.m_188501_() * 3.5 - 1.75, (double)this.f_19796_.m_188501_() * 3.5 - 1.75).m_82549_(nb.m_82548_().m_82542_(dist, dist, dist));
                    Vec3 speed = this.m_20184_().m_82542_((double)0.15f, (double)0.15f, (double)0.15f);
                    this.m_9236_().m_7106_((ParticleOptions)options, rnd2.f_82479_, rnd2.f_82480_, rnd2.f_82481_, speed.f_82479_, speed.f_82480_, speed.f_82481_);
                }
            } while ((dist += 1.75) < b);
            if (this.f_19797_ == 1) {
                for (int i = 0; i < 10; ++i) {
                    this.stripeParticles(MalkuthAttackType.FIRE);
                    this.stripeParticles(MalkuthAttackType.ICE);
                }
            }
        } else {
            if (this.isAllowedToMoveToTarget()) {
                if (!this.movementPath.isFinished()) {
                    this.movementPath.tick((Entity)this);
                } else {
                    this.removeNextTick = true;
                }
                this.tickDamage();
            }
            if (this.f_19797_ == 1) {
                SlamParticlesPacket packet = new SlamParticlesPacket(new SlamParticlesPacket.SlamData(this.m_20097_(), this.m_20182_().m_82520_(0.0, 0.5, 0.0), new Vec3(1.0, 0.0, 0.0)).maxAngle((float)Math.PI * 2).maxSpeed(0.3f).collectRadius(2).maxParticleLifetime(30).count(20).maxVerticalSpeedEdges(0.15f).maxVerticalSpeedCenter(0.15f));
                FDPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> this), (Object)packet);
            }
        }
    }

    private void tickDamage() {
        Vec3 movement = this.m_20184_();
        if (movement.equals((Object)Vec3.f_82478_)) {
            return;
        }
        float bbwidthinflate = this.m_20205_() / 2.0f;
        float bbHeight = this.m_20206_();
        Vec3 startPos = this.m_20182_().m_82520_(0.0, (double)(bbHeight / 2.0f), 0.0);
        Vec3 endPos = startPos.m_82549_(movement);
        List entities = FDHelpers.traceEntities((Level)this.m_9236_(), (Vec3)startPos, (Vec3)endPos, (double)bbwidthinflate, v -> !(v instanceof MalkuthBossBuddy));
        for (Entity target : entities) {
            if (!(target instanceof LivingEntity)) continue;
            LivingEntity livingEntity = (LivingEntity)target;
            target.m_6469_((DamageSource)new MalkuthDamageSource(BossDamageSources.MALKUTH_SIDE_ROCKS_SOURCE, this.getMalkuthAttackType(), 34), this.damage);
        }
    }

    private void stripeParticles(MalkuthAttackType type) {
        float rndRadius = 2.0f + FDEasings.easeOut((float)this.f_19796_.m_188501_()) * 2.0f;
        Vec3 rnd = new Vec3((double)rndRadius, 0.0, 0.0).m_82524_((float)Math.PI * 2 * this.f_19796_.m_188501_());
        Vec3 dir = rnd.m_82541_();
        float startOffsetRand = 0.1f + this.f_19796_.m_188501_() * 0.5f;
        Vec3 startOffset = dir.m_82542_((double)startOffsetRand, (double)startOffsetRand, (double)startOffsetRand);
        Vec3 stripePos = this.m_20182_().m_82549_(startOffset);
        Vector3f colFire = MalkuthEntity.getMalkuthAttackPreparationParticleColor(type);
        FDColor fireColorStart = new FDColor(colFire.x, colFire.y - this.f_19796_.m_188501_() * 0.1f - 0.3f, colFire.z, 0.5f);
        FDColor fireColor = new FDColor(colFire.x, colFire.y + this.f_19796_.m_188501_() * 0.1f, colFire.z, 1.0f);
        float firstPointOffset = 1.0f + this.f_19796_.m_188501_() * 1.0f;
        StripeParticleOptions stripeParticleOptions = StripeParticleOptions.builder().endColor(fireColor).startColor(fireColorStart).lifetime(5 + this.f_19796_.m_188503_(5)).lod(50).scale(0.1f).stripePercentLength(0.75f).endOutPercent(0.25f).startInPercent(0.25f).offsets(new Vec3((double)0.01f, 0.0, 0.0), dir.m_82542_((double)firstPointOffset, 0.0, (double)firstPointOffset).m_82520_(0.0, 0.5, 0.0), rnd.m_82520_(0.0, (double)(1.5f + this.f_19796_.m_188501_() * 2.0f), 0.0)).build();
        this.m_9236_().m_6493_((ParticleOptions)stripeParticleOptions, true, stripePos.f_82479_, stripePos.f_82480_, stripePos.f_82481_, 0.0, 0.0, 0.0);
    }

    public void setShouldMoveToTarget(boolean shouldMoveToTarget) {
        this.shouldMoveToTarget = shouldMoveToTarget;
    }

    public boolean isAllowedToMoveToTarget() {
        return this.shouldMoveToTarget;
    }

    public void setMalkuthAttackType(MalkuthAttackType malkuthAttackType) {
        this.f_19804_.m_135381_(MALKUTH_ATTACK_TYPE, (Object)malkuthAttackType);
    }

    public MalkuthAttackType getMalkuthAttackType() {
        return (MalkuthAttackType)((Object)this.f_19804_.m_135370_(MALKUTH_ATTACK_TYPE));
    }

    protected int getPrepareTime() {
        return (Integer)this.f_19804_.m_135370_(PREPARE_TIME);
    }

    protected void setPrepareTime(int time) {
        this.f_19804_.m_135381_(PREPARE_TIME, (Object)time);
    }

    public float getPrepareHeight() {
        return ((Float)this.f_19804_.m_135370_(PREPARE_HEIGHT)).floatValue();
    }

    protected void m_8097_() {
        super.m_8097_();
        this.f_19804_.m_135372_(MALKUTH_ATTACK_TYPE, (Object)MalkuthAttackType.FIRE);
        this.f_19804_.m_135372_(PREPARE_TIME, (Object)20);
        this.f_19804_.m_135372_(PREPARE_HEIGHT, (Object)Float.valueOf(3.0f));
    }

    public void m_7378_(CompoundTag tag) {
        super.m_7378_(tag);
        this.autoLoad(tag);
        if (tag.m_128441_("mtype")) {
            this.setMalkuthAttackType(MalkuthAttackType.valueOf(tag.m_128461_("mtype")));
        }
        this.f_19804_.m_135381_(PREPARE_TIME, (Object)tag.m_128451_("prepareTime"));
        this.f_19804_.m_135381_(PREPARE_HEIGHT, (Object)Float.valueOf(tag.m_128457_("prepareHeight")));
    }

    public void m_7380_(CompoundTag tag) {
        super.m_7380_(tag);
        this.autoSave(tag);
        tag.m_128359_("mtype", this.getMalkuthAttackType().name());
        tag.m_128405_("prepareTime", ((Integer)this.f_19804_.m_135370_(PREPARE_TIME)).intValue());
        tag.m_128350_("prepareHeight", ((Float)this.f_19804_.m_135370_(PREPARE_HEIGHT)).floatValue());
    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.nbt.AutoSerializable
 *  com.finderfeed.fdlib.nbt.SerializableField
 *  com.finderfeed.fdlib.util.FDTargetFinder
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.EntityDataSerializers
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$MoveFunction
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.entities.geburah.chain_trap;

import com.finderfeed.fdbosses.BossUtil;
import com.finderfeed.fdbosses.content.entities.geburah.judgement_bird.JudgementBirdEntity;
import com.finderfeed.fdbosses.content.util.Undismountable;
import com.finderfeed.fdlib.nbt.AutoSerializable;
import com.finderfeed.fdlib.nbt.SerializableField;
import com.finderfeed.fdlib.util.FDTargetFinder;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import java.util.UUID;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class GeburahChainTrapEntity
extends Entity
implements AutoSerializable,
Undismountable {
    public static int CATCH_TIME = 10;
    public static int PULL_TIME = 4;
    public static final EntityDataAccessor<Integer> ENTITY_ABOUT_TO_TRAP = SynchedEntityData.m_135353_(GeburahChainTrapEntity.class, (EntityDataSerializer)EntityDataSerializers.f_135028_);
    @SerializableField
    private UUID entityAboutToTrap;
    @SerializableField
    private int catchTicks = 0;
    protected int catchingTime = -1;
    protected int pullingTime = -1;
    private Vec3 lastKnownTargetPos;

    public GeburahChainTrapEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public void m_8119_() {
        super.m_8119_();
        if (!this.m_9236_().f_46443_) {
            if (this.m_20197_().isEmpty()) {
                LivingEntity entityAboutToTrap = this.getEntityAboutToTrap();
                if (entityAboutToTrap != null) {
                    GeburahChainTrapEntity e;
                    Entity entity;
                    if (entityAboutToTrap.m_21224_() || (entity = entityAboutToTrap.m_20202_()) instanceof GeburahChainTrapEntity && (e = (GeburahChainTrapEntity)entity) != this) {
                        this.m_142467_(Entity.RemovalReason.DISCARDED);
                        return;
                    }
                    if (this.catchingTime == -1) {
                        this.catchingTime = CATCH_TIME;
                    } else if (this.catchingTime == 0) {
                        entityAboutToTrap.m_7998_((Entity)this, true);
                    }
                    this.m_20088_().m_135381_(ENTITY_ABOUT_TO_TRAP, (Object)entityAboutToTrap.m_19879_());
                } else {
                    for (LivingEntity entity : FDTargetFinder.getEntitiesInCylinder(LivingEntity.class, (Level)this.m_9236_(), (Vec3)this.m_20182_().m_82520_(0.0, -0.1, 0.0), (float)5.0f, (float)1.5f)) {
                        Player player;
                        if (entity instanceof Player && !BossUtil.isPlayerInSurvival(player = (Player)entity) || entity.m_20202_() instanceof GeburahChainTrapEntity) continue;
                        this.trapEntity(entity);
                        break;
                    }
                }
            } else {
                LivingEntity livingEntity;
                Object e = this.m_20197_().get(0);
                if (e instanceof LivingEntity && (livingEntity = (LivingEntity)e).m_21224_()) {
                    this.m_142687_(Entity.RemovalReason.DISCARDED);
                }
                this.tickCaughtEntity();
            }
        } else if (this.m_9236_().m_46467_() % 2L == 0L) {
            BallParticleOptions ballParticle = BallParticleOptions.builder().color(0.3f, 0.8f + this.f_19796_.m_188501_() * 0.2f, 1.0f).size(0.15f).brightness(3).scalingOptions(0, 0, 20).build();
            Vec3 rndOffset = new Vec3((double)(this.f_19796_.m_188501_() * 0.2f) + 0.3, 0.0, 0.0).m_82524_(this.f_19796_.m_188501_() * (float)Math.PI * 2.0f);
            this.m_9236_().m_7106_((ParticleOptions)ballParticle, this.m_20185_() + rndOffset.f_82479_, this.m_20186_(), this.m_20189_() + rndOffset.f_82481_, 0.0, (double)0.05f, 0.0);
        }
        if (this.catchingTime != -1) {
            this.catchingTime = Mth.m_14045_((int)(this.catchingTime - 1), (int)0, (int)CATCH_TIME);
        }
        if (this.pullingTime != -1) {
            this.pullingTime = Mth.m_14045_((int)(this.pullingTime - 1), (int)0, (int)PULL_TIME);
        }
    }

    private void tickCaughtEntity() {
        if (!this.m_20197_().isEmpty()) {
            Entity entity = (Entity)this.m_20197_().get(0);
            int duration = 600;
            if (entity instanceof Player) {
                Player player = (Player)entity;
                duration = 60;
            }
            if (this.catchTicks > duration) {
                this.m_142687_(Entity.RemovalReason.DISCARDED);
            }
            ++this.catchTicks;
        }
    }

    public boolean m_6087_() {
        return true;
    }

    public void m_7350_(EntityDataAccessor<?> accessor) {
        super.m_7350_(accessor);
        if (accessor == ENTITY_ABOUT_TO_TRAP && this.m_20197_().isEmpty()) {
            this.catchingTime = CATCH_TIME;
        }
    }

    public void trapEntity(LivingEntity entity) {
        if (entity.m_20270_((Entity)this) > 20.0f) {
            return;
        }
        this.entityAboutToTrap = entity.m_20148_();
        this.m_20088_().m_135381_(ENTITY_ABOUT_TO_TRAP, (Object)entity.m_19879_());
    }

    public LivingEntity getEntityAboutToTrap() {
        Level level = this.m_9236_();
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            Entity entity = serverLevel.m_8791_(this.entityAboutToTrap);
            if (entity instanceof LivingEntity) {
                livingEntity = (LivingEntity)entity;
                return livingEntity;
            }
        } else {
            Entity entity = this.m_9236_().m_6815_(((Integer)this.f_19804_.m_135370_(ENTITY_ABOUT_TO_TRAP)).intValue());
            if (entity instanceof LivingEntity) {
                livingEntity = (LivingEntity)entity;
                return livingEntity;
            }
        }
        return null;
    }

    protected void m_19956_(Entity entity, Entity.MoveFunction moveFunction) {
        float height = 0.0f;
        if (this.lastKnownTargetPos == null) {
            this.lastKnownTargetPos = entity.m_20182_().m_82520_(0.0, (double)height, 0.0);
        }
        if (this.pullingTime == -1) {
            this.pullingTime = PULL_TIME;
        }
        float p = 1.0f - (float)this.pullingTime / (float)PULL_TIME;
        Vec3 target = this.m_20182_().m_82520_(0.0, (double)height, 0.0);
        Vec3 pos = FDMathUtil.interpolateVectors((Vec3)this.lastKnownTargetPos, (Vec3)target, (float)p);
        if (entity instanceof JudgementBirdEntity) {
            pos = pos.m_82520_(0.0, 1.0, 0.0);
        }
        moveFunction.m_20372_(entity, pos.f_82479_, pos.f_82480_, pos.f_82481_);
    }

    public boolean shouldRiderSit() {
        return false;
    }

    public int getCatchingTime() {
        return this.catchingTime;
    }

    protected void m_8097_() {
        this.m_20088_().m_135372_(ENTITY_ABOUT_TO_TRAP, (Object)-1);
    }

    protected void m_7380_(CompoundTag tag) {
        this.autoSave(tag);
    }

    protected void m_7378_(CompoundTag tag) {
        this.autoLoad(tag);
    }

    public boolean m_6000_(double p_20296_, double p_20297_, double p_20298_) {
        return true;
    }

    public boolean m_6783_(double p_19883_) {
        return true;
    }
}


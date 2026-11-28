/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.FDLibCalls
 *  com.finderfeed.fdlib.util.FDColor
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleTypes
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.EntityDataSerializers
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec2
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Vector3f
 */
package com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_floor;

import com.finderfeed.fdbosses.BossTargetFinder;
import com.finderfeed.fdbosses.BossUtil;
import com.finderfeed.fdbosses.client.BossParticles;
import com.finderfeed.fdbosses.client.particles.GravityParticleOptions;
import com.finderfeed.fdbosses.client.particles.smoke_particle.BigSmokeParticleOptions;
import com.finderfeed.fdbosses.client.particles.stripe_particle.StripeParticleOptions;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthAttackType;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthBossBuddy;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthEntity;
import com.finderfeed.fdbosses.init.BossConfigs;
import com.finderfeed.fdbosses.init.BossDamageSources;
import com.finderfeed.fdlib.FDLibCalls;
import com.finderfeed.fdlib.util.FDColor;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class MalkuthFloorEntity
extends Entity {
    public static EntityDataAccessor<Boolean> IS_DEAD = SynchedEntityData.m_135353_(MalkuthFloorEntity.class, (EntityDataSerializer)EntityDataSerializers.f_135035_);
    public static final float RADIUS = 28.0f;
    public static final int DEATH_TME = 10;
    public int deathTicks = 10;

    public MalkuthFloorEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public void m_8119_() {
        super.m_8119_();
        if (this.m_9236_().f_46443_) {
            this.summonParticles();
            this.particles();
        } else if (!this.isDead()) {
            this.damageAndThrowUp();
        }
        if (this.isDead()) {
            if (!this.m_9236_().f_46443_ && this.deathTicks <= 0) {
                this.m_142467_(Entity.RemovalReason.DISCARDED);
            }
            this.deathTicks = FDMathUtil.clamp((int)(this.deathTicks - 1), (int)0, (int)10);
        }
    }

    private void damageAndThrowUp() {
        if (this.f_19797_ > 10) {
            float damage = BossUtil.transformDamage(this.m_9236_(), BossConfigs.BOSS_CONFIG.get().malkuthConfig.malkuthFloorDamage);
            List<LivingEntity> targets = BossTargetFinder.getEntitiesInArc(LivingEntity.class, this.m_9236_(), this.m_20182_(), new Vec2(0.0f, -1.0f), (float)Math.PI, 2.5f, 28.0f);
            for (LivingEntity target : targets) {
                if (target instanceof MalkuthBossBuddy) continue;
                target.m_6469_(BossDamageSources.MALKUTH_IMPALING_DOOM_SOURCE, damage);
                double speed = 1.2f;
                if (target instanceof ServerPlayer) {
                    ServerPlayer serverPlayer = (ServerPlayer)target;
                    FDLibCalls.setServerPlayerSpeed((ServerPlayer)serverPlayer, (Vec3)new Vec3(0.0, speed, 0.0));
                    continue;
                }
                target.m_20334_(0.0, speed, 0.0);
            }
        }
    }

    public boolean isDead() {
        return (Boolean)this.f_19804_.m_135370_(IS_DEAD);
    }

    private void setDead() {
        this.f_19804_.m_135381_(IS_DEAD, (Object)true);
    }

    private void summonParticles() {
        if (this.f_19797_ == 1) {
            float segmentSize = 1.0f;
            float radius = 28.0f;
            for (float currentRad = segmentSize; currentRad <= radius; currentRad += segmentSize) {
                float currentArcLength = (float)Math.PI * currentRad;
                for (float i = 0.0f; i <= currentArcLength; i += segmentSize) {
                    Object particleOptions;
                    float p = i / currentArcLength;
                    float currentAngle = (float)Math.PI * p;
                    Vec3 currentOffset = new Vec3((double)currentRad, 0.0, 0.0).m_82524_(currentAngle);
                    Vec3 ppos = this.m_20182_().m_82549_(currentOffset);
                    float rnd = this.f_19796_.m_188501_();
                    float speedX = this.f_19796_.m_188501_() * 0.5f - 0.25f;
                    float speedY = this.f_19796_.m_188501_() * 0.5f + 0.5f;
                    float speedZ = this.f_19796_.m_188501_() * 0.5f - 0.25f;
                    if ((double)rnd > 0.75) {
                        particleOptions = ParticleTypes.f_123756_;
                    } else if (rnd > 0.5f) {
                        particleOptions = new GravityParticleOptions(BossParticles.FLAME_WITH_STONE.get(), 20 + this.f_19796_.m_188503_(4), 0.5f + this.f_19796_.m_188501_() * 0.2f, 1.5999999f, 2.0f, true);
                    } else if (rnd > 0.25f) {
                        particleOptions = new GravityParticleOptions(BossParticles.ICE_CHUNK.get(), 20 + this.f_19796_.m_188503_(4), 0.5f + this.f_19796_.m_188501_() * 0.2f, 1.5999999f, 2.0f, true);
                    } else {
                        Vector3f colFire = MalkuthEntity.getMalkuthAttackPreparationParticleColor(MalkuthAttackType.getRandom(this.f_19796_));
                        FDColor colorStart = new FDColor(colFire.x, colFire.y - this.f_19796_.m_188501_() * 0.1f - 0.3f, colFire.z, 0.5f);
                        FDColor colorEnd = new FDColor(colFire.x, colFire.y + this.f_19796_.m_188501_() * 0.1f, colFire.z, 1.0f);
                        speedX = 0.0f;
                        speedY = 0.0f;
                        speedZ = 0.0f;
                        List<Vec3> offsets = List.of(new Vec3(0.0, 0.0, 0.01), new Vec3((double)(this.f_19796_.m_188501_() * 4.0f - 2.0f), (double)(2 + this.f_19796_.m_188503_(2)), (double)(this.f_19796_.m_188501_() * 4.0f - 2.0f)), new Vec3((double)(this.f_19796_.m_188501_() * 4.0f - 2.0f), (double)(4 + this.f_19796_.m_188503_(2)), (double)(this.f_19796_.m_188501_() * 4.0f - 2.0f)));
                        particleOptions = StripeParticleOptions.builder().startColor(colorStart).endColor(colorEnd).lifetime(5 + this.f_19796_.m_188503_(15)).lod(20).scale(0.05f).stripePercentLength(0.5f).startInPercent(0.5f).endOutPercent(0.5f).offsets(offsets).build();
                    }
                    this.m_9236_().m_6493_((ParticleOptions)particleOptions, true, ppos.f_82479_ + (double)(this.f_19796_.m_188501_() * segmentSize) - (double)(segmentSize / 2.0f), ppos.f_82480_, ppos.f_82481_ + (double)(this.f_19796_.m_188501_() * segmentSize) - (double)(segmentSize / 2.0f), (double)speedX, (double)speedY, (double)speedZ);
                }
            }
        }
    }

    private void particles() {
        if (this.m_9236_().m_46467_() % 5L == 0L) {
            float segmentSize = 3.0f;
            float radius = 25.0f;
            for (float currentRad = segmentSize; currentRad <= radius; currentRad += segmentSize) {
                float currentArcLength = (float)Math.PI * currentRad;
                for (float i = 0.0f; i <= currentArcLength; i += segmentSize) {
                    Object particleOptions;
                    float p = i / currentArcLength;
                    float currentAngle = (float)Math.PI * p;
                    Vec3 currentOffset = new Vec3((double)currentRad, 0.0, 0.0).m_82524_(currentAngle);
                    Vec3 ppos = this.m_20182_().m_82549_(currentOffset);
                    if (this.f_19796_.m_188499_()) {
                        particleOptions = ParticleTypes.f_123756_;
                    } else {
                        float col = this.f_19796_.m_188501_() * 0.2f + 0.3f;
                        particleOptions = BigSmokeParticleOptions.builder().lifetime(0, 0, 40).size(2.0f + this.f_19796_.m_188501_()).color(col, col, col).build();
                    }
                    this.m_9236_().m_6493_((ParticleOptions)particleOptions, true, ppos.f_82479_ + (double)(this.f_19796_.m_188501_() * segmentSize) - (double)(segmentSize / 2.0f), ppos.f_82480_, ppos.f_82481_ + (double)(this.f_19796_.m_188501_() * segmentSize) - (double)(segmentSize / 2.0f), 0.0, 0.0, 0.0);
                }
            }
        }
    }

    public void m_6074_() {
        this.setDead();
    }

    public boolean m_6087_() {
        return true;
    }

    protected void m_8097_() {
        this.f_19804_.m_135372_(IS_DEAD, (Object)false);
    }

    protected void m_7378_(CompoundTag tag) {
        this.f_19804_.m_135381_(IS_DEAD, (Object)tag.m_128471_("is_dead"));
    }

    protected void m_7380_(CompoundTag tag) {
        tag.m_128379_("is_dead", ((Boolean)this.f_19804_.m_135370_(IS_DEAD)).booleanValue());
    }

    public boolean m_6000_(double p_20296_, double p_20297_, double p_20298_) {
        return true;
    }

    public AABB m_6921_() {
        return super.m_6921_();
    }
}


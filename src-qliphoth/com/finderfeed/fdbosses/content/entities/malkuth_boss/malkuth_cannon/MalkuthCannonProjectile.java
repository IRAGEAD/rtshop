/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.nbt.AutoSerializable
 *  com.finderfeed.fdlib.nbt.SerializableField
 *  com.finderfeed.fdlib.network.FDPacketHandler
 *  com.finderfeed.fdlib.util.FDProjectile
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleTypes
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.damagesource.DamageSource
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.projectile.AbstractHurtingProjectile
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.EntityHitResult
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.network.PacketDistributor
 */
package com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_cannon;

import com.finderfeed.fdbosses.BossTargetFinder;
import com.finderfeed.fdbosses.BossUtil;
import com.finderfeed.fdbosses.client.BossParticles;
import com.finderfeed.fdbosses.client.particles.GravityParticleOptions;
import com.finderfeed.fdbosses.client.particles.smoke_particle.BigSmokeParticleOptions;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthAttackType;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthDamageSource;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthEntity;
import com.finderfeed.fdbosses.init.BossDamageSources;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdbosses.init.BossEntityDataSerializers;
import com.finderfeed.fdbosses.packets.SlamParticlesPacket;
import com.finderfeed.fdlib.nbt.AutoSerializable;
import com.finderfeed.fdlib.nbt.SerializableField;
import com.finderfeed.fdlib.network.FDPacketHandler;
import com.finderfeed.fdlib.util.FDProjectile;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

public class MalkuthCannonProjectile
extends FDProjectile
implements AutoSerializable {
    public static final int PROJECITLE_INVULNERABILITY_TIME = 10;
    public static final EntityDataAccessor<MalkuthAttackType> MALKUTH_ATTACK_TYPE = SynchedEntityData.m_135353_(MalkuthCannonProjectile.class, (EntityDataSerializer)((EntityDataSerializer)BossEntityDataSerializers.MALKUTH_ATTACK_TYPE.get()));
    @SerializableField
    private MalkuthAttackType malkuthAttackType = MalkuthAttackType.FIRE;
    @SerializableField
    private int reversedAge = 3000;
    @SerializableField
    private float damage;

    public MalkuthCannonProjectile(EntityType<? extends AbstractHurtingProjectile> type, Level level) {
        super(type, level);
        this.m_20242_(false);
    }

    public static MalkuthCannonProjectile summon(Level level, Vec3 pos, Vec3 speed, int lifetime, MalkuthAttackType projectileDamageType, float damage) {
        MalkuthCannonProjectile malkuthCannonProjectile = new MalkuthCannonProjectile(BossEntities.MALKUTH_CANNON_PROJECTILE.get(), level);
        malkuthCannonProjectile.m_146884_(pos);
        malkuthCannonProjectile.m_20256_(speed);
        malkuthCannonProjectile.reversedAge = lifetime;
        malkuthCannonProjectile.setMalkuthAttackType(projectileDamageType);
        malkuthCannonProjectile.damage = damage;
        level.m_7967_((Entity)malkuthCannonProjectile);
        return malkuthCannonProjectile;
    }

    public void setMalkuthAttackType(MalkuthAttackType malkuthAttackType) {
        this.malkuthAttackType = malkuthAttackType;
        this.f_19804_.m_135381_(MALKUTH_ATTACK_TYPE, (Object)malkuthAttackType);
    }

    public MalkuthAttackType getMalkuthAttackType() {
        return (MalkuthAttackType)((Object)this.f_19804_.m_135370_(MALKUTH_ATTACK_TYPE));
    }

    public void m_8119_() {
        if (!this.m_9236_().f_46443_) {
            this.setMalkuthAttackType(this.malkuthAttackType);
            BossUtil.applyGravity((Entity)this, this.getDefaultGravity());
            if (this.reversedAge-- <= 0) {
                this.explode(this.m_20182_());
            }
        }
        super.m_8119_();
        if (this.m_9236_().f_46443_) {
            this.spawnParticles();
        }
    }

    private void spawnParticles() {
        Vec3 between = new Vec3(this.f_19854_ - this.m_20185_(), this.f_19855_ - this.m_20186_(), this.f_19856_ - this.m_20189_());
        float dist = (float)between.m_82553_();
        between = between.m_82541_();
        for (float i = 0.0f; i <= dist; i += 0.5f) {
            float p = i / dist;
            for (int count = 0; count < 4; ++count) {
                float b;
                float g;
                float r;
                if (this.getMalkuthAttackType().isFire()) {
                    r = 0.8f + this.f_19796_.m_188501_() * 0.2f;
                    g = 0.3f + this.f_19796_.m_188501_() * 0.5f;
                    b = 0.1f + this.f_19796_.m_188501_() * 0.05f;
                } else {
                    r = 0.1f + this.f_19796_.m_188501_() * 0.05f;
                    g = 0.8f - this.f_19796_.m_188501_() * 0.3f;
                    b = 0.8f + this.f_19796_.m_188501_() * 0.2f;
                }
                Object options = (double)this.f_19796_.m_188501_() > 0.5 ? BallParticleOptions.builder().size(0.15f + this.f_19796_.m_188501_() * 0.1f).color(r, g, b).scalingOptions(0, 0, 10).build() : (this.getMalkuthAttackType().isFire() ? ParticleTypes.f_123762_ : new GravityParticleOptions(BossParticles.ICE_CHUNK.get(), 10, 0.25f, 0.0f, 5.0f, true));
                this.m_9236_().m_6493_((ParticleOptions)options, true, this.m_20185_() + between.f_82479_ * 1.5 + between.f_82479_ * (double)p + (double)this.f_19796_.m_188501_() - 0.5, this.m_20186_() + between.f_82480_ * 1.5 + between.f_82480_ * (double)p + (double)this.f_19796_.m_188501_() - 0.5, this.m_20189_() + between.f_82481_ * 1.5 + between.f_82481_ * (double)p + (double)this.f_19796_.m_188501_() - 0.5, 0.0, 0.0, 0.0);
            }
            float c = this.f_19796_.m_188501_() * 0.4f + 0.2f;
            BigSmokeParticleOptions bigSmokeParticleOptions = BigSmokeParticleOptions.builder().color(c, c, c).lifetime(0, 0, 10).size(1.0f + 0.1f * this.f_19796_.m_188501_()).build();
            this.m_9236_().m_6493_((ParticleOptions)bigSmokeParticleOptions, true, this.m_20185_() + between.f_82479_ * (double)p + between.f_82479_, this.m_20186_() + between.f_82480_ * (double)p + between.f_82480_, this.m_20189_() + between.f_82481_ * (double)p + between.f_82481_, 0.0, 0.0, 0.0);
        }
    }

    protected void m_8060_(BlockHitResult res) {
        super.m_8060_(res);
        if (!this.m_9236_().f_46443_ && this.f_19797_ > 10) {
            this.explode(res.m_82450_());
        }
    }

    protected void m_5790_(EntityHitResult res) {
        super.m_5790_(res);
        if (!this.m_9236_().f_46443_ && this.f_19797_ > 10 && !(res.m_82443_() instanceof MalkuthCannonProjectile)) {
            this.explode(res.m_82450_());
        }
    }

    private void explode(Vec3 pos) {
        if (!this.m_9236_().f_46443_) {
            float c = this.f_19796_.m_188501_() * 0.2f + 0.2f;
            BigSmokeParticleOptions bigSmokeParticleOptions = BigSmokeParticleOptions.builder().color(c, c, c).lifetime(0, 0, 10).size(1.0f + 0.1f * this.f_19796_.m_188501_()).friction(0.7f).build();
            ((ServerLevel)this.m_9236_()).m_8767_((ParticleOptions)bigSmokeParticleOptions, pos.f_82479_, pos.f_82480_, pos.f_82481_, 20, 0.0, 0.0, 0.0, 0.25);
            SlamParticlesPacket packet = new SlamParticlesPacket(new SlamParticlesPacket.SlamData(new BlockPos((int)pos.f_82479_, (int)pos.f_82480_, (int)pos.f_82481_), pos.m_82520_(0.0, 0.0, 0.0), new Vec3(1.0, 0.0, 0.0)).maxAngle((float)Math.PI * 2).maxSpeed(0.3f).collectRadius(2).maxParticleLifetime(30).count(20).maxVerticalSpeedEdges(0.15f).maxVerticalSpeedCenter(0.15f));
            FDPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> this), (Object)packet);
            BossUtil.malkuthFireballExplosionParticles((ServerLevel)this.m_9236_(), pos, this.getMalkuthAttackType());
            List<LivingEntity> targets = BossTargetFinder.getEntitiesInSphere(LivingEntity.class, this.m_9236_(), pos, 2.5f, target -> !(target instanceof MalkuthEntity));
            List<MalkuthEntity> malkuth = BossTargetFinder.getEntitiesInSphere(MalkuthEntity.class, this.m_9236_(), pos, 10.0f);
            for (MalkuthEntity m : malkuth) {
                m.hurtBoss(1);
            }
            for (LivingEntity target2 : targets) {
                if (this.damage == 0.0f) continue;
                target2.m_6469_((DamageSource)new MalkuthDamageSource(BossDamageSources.MALKUTH_CANNONS_SOURCE, this.getMalkuthAttackType(), 100), this.damage);
            }
            this.m_142687_(Entity.RemovalReason.DISCARDED);
        }
    }

    public void m_7380_(CompoundTag tag) {
        super.m_7380_(tag);
        this.autoSave(tag);
    }

    public void m_7378_(CompoundTag tag) {
        super.m_7378_(tag);
        this.autoLoad(tag);
        this.setMalkuthAttackType(this.malkuthAttackType);
    }

    protected double getDefaultGravity() {
        return 0.08;
    }

    protected void m_8097_() {
        super.m_8097_();
        this.f_19804_.m_135372_(MALKUTH_ATTACK_TYPE, (Object)MalkuthAttackType.FIRE);
    }
}


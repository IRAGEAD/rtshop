/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.FDLibCalls
 *  com.finderfeed.fdlib.nbt.AutoSerializable
 *  com.finderfeed.fdlib.nbt.SerializableField
 *  com.finderfeed.fdlib.systems.bedrock.animations.Animation
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDLivingEntity
 *  com.finderfeed.fdlib.util.ProjectileMovementPath
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions
 *  com.finderfeed.fdlib.util.client.particles.lightning_particle.LightningParticleOptions
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.damagesource.DamageSource
 *  net.minecraft.world.damagesource.DamageTypes
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.entities.chesed_boss.electric_sphere;

import com.finderfeed.fdbosses.client.BossParticles;
import com.finderfeed.fdbosses.client.particles.arc_lightning.ArcLightningOptions;
import com.finderfeed.fdbosses.content.entities.chesed_boss.ChesedBossBuddy;
import com.finderfeed.fdbosses.init.BossAnims;
import com.finderfeed.fdbosses.init.BossConfigs;
import com.finderfeed.fdbosses.init.BossDamageSources;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdbosses.init.BossSounds;
import com.finderfeed.fdlib.FDLibCalls;
import com.finderfeed.fdlib.nbt.AutoSerializable;
import com.finderfeed.fdlib.nbt.SerializableField;
import com.finderfeed.fdlib.systems.bedrock.animations.Animation;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDLivingEntity;
import com.finderfeed.fdlib.util.ProjectileMovementPath;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions;
import com.finderfeed.fdlib.util.client.particles.lightning_particle.LightningParticleOptions;
import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ChesedElectricSphereEntity
extends FDLivingEntity
implements AutoSerializable,
ChesedBossBuddy {
    @SerializableField
    private ProjectileMovementPath path;
    @SerializableField
    private float damage;

    public ChesedElectricSphereEntity(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
        if (this.m_9236_().f_46443_) {
            this.getAnimationSystem().startAnimation("IDLE", AnimationTicker.builder((Animation)((Animation)BossAnims.ELECTRIC_ORB_IDLE.get())).build());
        }
    }

    public static ChesedElectricSphereEntity summon(Level level, float damage, ProjectileMovementPath path) {
        ChesedElectricSphereEntity sphereEntity = new ChesedElectricSphereEntity(BossEntities.CHESED_ELECTRIC_SPHERE.get(), level);
        sphereEntity.m_146884_((Vec3)path.getPositions().get(0));
        sphereEntity.path = path;
        sphereEntity.damage = damage;
        level.m_7967_((Entity)sphereEntity);
        return sphereEntity;
    }

    public void m_8119_() {
        this.f_19794_ = true;
        super.m_8119_();
        if (!this.m_9236_().f_46443_) {
            if (this.path != null) {
                if (!this.path.isFinished()) {
                    this.path.tick((Entity)this);
                } else {
                    this.explode();
                    this.m_146870_();
                }
            }
            this.detectEntitiesAndExplode();
        } else {
            this.idleParticles();
            this.getAnimationSystem().startAnimation("IDLE", AnimationTicker.builder((Animation)((Animation)BossAnims.ELECTRIC_ORB_IDLE.get())).build());
        }
    }

    private void idleParticles() {
        if (BossConfigs.BOSS_CONFIG_CLIENT.get().lessParticles) {
            return;
        }
        if (this.f_19797_ < 10) {
            return;
        }
        if (this.f_19797_ % 3 == 0) {
            for (int i = 0; i < 1; ++i) {
                float offs = 0.25f;
                Vec3 p1 = this.m_20182_().m_82520_((double)this.f_19796_.m_188501_() * 0.025 - 0.0125, (double)offs, (double)this.f_19796_.m_188501_() * 0.025 - 0.0125);
                Vec3 p2 = this.m_20182_().m_82520_(0.0, (double)(this.m_20206_() - offs), 0.0);
                Vec3 sp = this.m_20184_();
                this.m_9236_().m_6493_((ParticleOptions)ArcLightningOptions.builder(BossParticles.ARC_LIGHTNING.get()).end(p2.f_82479_, p2.f_82480_, p2.f_82481_).endSpeed(sp).lifetime(2).color(1 + this.f_19796_.m_188503_(40), 183 + this.f_19796_.m_188503_(60), 165 + this.f_19796_.m_188503_(60)).lightningSpread(0.25f).width(0.1f).segments(6).circleOffset(0.25f).build(), false, p1.f_82479_, p1.f_82480_, p1.f_82481_, sp.f_82479_, sp.f_82480_, sp.f_82481_);
            }
        }
    }

    private void detectEntitiesAndExplode() {
        List list = this.m_9236_().m_6443_(LivingEntity.class, this.m_20191_(), living -> !(living instanceof ChesedBossBuddy));
        if (list.isEmpty()) {
            return;
        }
        this.explode();
        this.m_146870_();
    }

    private void explode() {
        List list = this.m_9236_().m_6443_(LivingEntity.class, this.m_20191_().m_82377_(0.2, 0.2, 0.2), living -> !(living instanceof ChesedBossBuddy));
        DamageSource source = BossDamageSources.CHESED_ELECTRIC_SPHERE_SOURCE;
        this.m_9236_().m_6263_(null, this.m_20185_(), this.m_20186_(), this.m_20189_(), BossSounds.FAST_LIGHTNING_STRIKE.get(), SoundSource.HOSTILE, 1.0f, 1.0f);
        FDLibCalls.sendParticles((ServerLevel)((ServerLevel)this.m_9236_()), (ParticleOptions)BallParticleOptions.builder().size(2.0f).scalingOptions(0, 0, 2).color(150, 230, 255).build(), (Vec3)this.m_20182_(), (double)30.0);
        for (LivingEntity entity : list) {
            entity.m_6469_(source, this.damage);
        }
        Level level = this.m_9236_();
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            serverLevel.m_8767_((ParticleOptions)LightningParticleOptions.builder().color(20, 200 + this.f_19796_.m_188503_(50), 255).lifetime(10).maxLightningSegments(3).randomRoll(true).physics(true).build(), this.m_20185_(), this.m_20186_() + (double)0.2f, this.m_20189_(), 30, (double)0.02f, (double)0.02f, (double)0.02f, (double)0.05f);
        }
    }

    public boolean m_6469_(DamageSource src, float damage) {
        if (!src.m_276093_(DamageTypes.f_286979_) && !src.m_276093_(DamageTypes.f_268724_)) {
            return false;
        }
        return super.m_6469_(src, damage);
    }

    protected void m_6763_(BlockState p_20005_) {
        super.m_6763_(p_20005_);
        this.explode();
    }

    public boolean m_20068_() {
        return true;
    }

    public boolean m_20223_(CompoundTag tag) {
        this.autoSave(tag);
        return super.m_20223_(tag);
    }

    public void m_20258_(CompoundTag tag) {
        this.autoLoad(tag);
        super.m_20258_(tag);
    }
}


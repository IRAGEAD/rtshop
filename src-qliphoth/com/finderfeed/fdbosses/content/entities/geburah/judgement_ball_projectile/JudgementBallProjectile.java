/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.nbt.AutoSerializable
 *  com.finderfeed.fdlib.nbt.SerializableField
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDEntity
 *  com.finderfeed.fdlib.systems.trails.FDTrailDataGenerator
 *  com.finderfeed.fdlib.util.ProjectileMovementPath
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.entities.geburah.judgement_ball_projectile;

import com.finderfeed.fdbosses.BossUtil;
import com.finderfeed.fdbosses.content.entities.geburah.judgement_ball_projectile.JudgementBallExplosionParticleOptions;
import com.finderfeed.fdbosses.init.BossDamageSources;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdlib.nbt.AutoSerializable;
import com.finderfeed.fdlib.nbt.SerializableField;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDEntity;
import com.finderfeed.fdlib.systems.trails.FDTrailDataGenerator;
import com.finderfeed.fdlib.util.ProjectileMovementPath;
import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class JudgementBallProjectile
extends FDEntity
implements AutoSerializable {
    @SerializableField
    public ProjectileMovementPath projectileMovementPath;
    @SerializableField
    public float damage = 2.0f;
    private double distanceTraveled = 0.0;
    public FDTrailDataGenerator<JudgementBallProjectile> trail;

    public static JudgementBallProjectile summon(Level level, ProjectileMovementPath projectileMovementPath, float damage) {
        JudgementBallProjectile judgementBallProjectile = new JudgementBallProjectile(BossEntities.GEBURAH_JUDGEMENT_BALL.get(), level);
        judgementBallProjectile.m_146884_((Vec3)projectileMovementPath.getPositions().get(0));
        judgementBallProjectile.damage = BossUtil.transformDamage(level, damage);
        judgementBallProjectile.projectileMovementPath = projectileMovementPath;
        projectileMovementPath.tick((Entity)judgementBallProjectile);
        level.m_7967_((Entity)judgementBallProjectile);
        return judgementBallProjectile;
    }

    public JudgementBallProjectile(EntityType<?> type, Level level) {
        super(type, level);
        if (level.f_46443_) {
            this.trail = new FDTrailDataGenerator((p, v) -> this.m_20318_(v.floatValue()), 10, 0.01f);
        }
    }

    public void m_8119_() {
        super.m_8119_();
        if (!this.m_9236_().f_46443_) {
            this.tickMovementPath();
            this.detectTargetsAndExplode();
            this.m_146884_(this.m_20182_().m_82549_(this.m_20184_()));
        } else {
            this.m_146884_(this.m_20182_().m_82549_(this.m_20184_()));
            this.particles();
            this.trail.tick((Object)this);
        }
    }

    private void particles() {
        if (this.distanceTraveled > 0.5) {
            this.distanceTraveled = 0.0;
            Vec3 ppos = this.m_20182_().m_82546_(this.m_20184_().m_82490_(1.0));
            this.m_9236_().m_7106_((ParticleOptions)new JudgementBallExplosionParticleOptions(this.m_20184_(), -10, 0.9f), ppos.f_82479_, ppos.f_82480_ + (double)(this.m_20206_() / 2.0f), ppos.f_82481_, 0.0, 0.0, 0.0);
        } else {
            this.distanceTraveled += new Vec3(this.m_20185_() - this.f_19854_, this.m_20186_() - this.f_19855_, this.m_20189_() - this.f_19856_).m_82553_();
        }
    }

    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        this.m_9236_().m_7106_((ParticleOptions)new JudgementBallExplosionParticleOptions(this.m_20184_(), 10, 2.0f), this.m_20185_(), this.m_20186_() + (double)(this.m_20206_() / 2.0f), this.m_20189_(), 0.0, 0.0, 0.0);
    }

    private void tickMovementPath() {
        if (this.projectileMovementPath == null) {
            this.m_142467_(Entity.RemovalReason.DISCARDED);
            return;
        }
        if (!this.projectileMovementPath.isFinished()) {
            this.projectileMovementPath.tick((Entity)this);
        } else {
            this.m_142467_(Entity.RemovalReason.DISCARDED);
        }
    }

    private void detectTargetsAndExplode() {
        AABB box = this.m_20191_();
        List targets = this.m_9236_().m_45976_(LivingEntity.class, box);
        if (!targets.isEmpty()) {
            for (LivingEntity target : targets) {
                target.m_6469_(BossDamageSources.GEBURAH_JUDGEMENT_BALL_SOURCE, this.damage);
            }
            this.m_142467_(Entity.RemovalReason.DISCARDED);
        }
    }

    protected void m_8097_() {
    }

    protected void m_7380_(CompoundTag tag) {
        super.m_7380_(tag);
        this.autoSave(tag);
    }

    protected void m_7378_(CompoundTag tag) {
        super.m_7378_(tag);
        this.autoLoad(tag);
    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.nbt.AutoSerializable
 *  com.finderfeed.fdlib.nbt.SerializableField
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions
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
package com.finderfeed.fdbosses.content.entities.chesed_boss;

import com.finderfeed.fdbosses.content.entities.chesed_boss.ChesedBossBuddy;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdlib.nbt.AutoSerializable;
import com.finderfeed.fdlib.nbt.SerializableField;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions;
import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ChesedFireTrailEntity
extends Entity
implements AutoSerializable {
    @SerializableField
    private float damage = 0.0f;
    @SerializableField
    private int lifetime = 0;

    public ChesedFireTrailEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public void m_8119_() {
        super.m_8119_();
        float radius = 1.0f;
        if (!this.m_9236_().f_46443_) {
            if (this.f_19797_ > this.lifetime) {
                this.m_142687_(Entity.RemovalReason.DISCARDED);
            }
            this.doDamage(radius, 0.25f);
        } else {
            this.spawnParticles(radius, 1);
        }
    }

    private void spawnParticles(float radius, int amountPerTick) {
        for (int i = 0; i < amountPerTick; ++i) {
            Vec3 v = new Vec3((double)(this.f_19796_.m_188501_() * radius), 0.0, 0.0).m_82524_(this.f_19796_.m_188501_() * (float)Math.PI * 2.0f);
            Vec3 ppos = this.m_20182_().m_82549_(v);
            float spm = 0.025f * this.f_19796_.m_188501_();
            Vec3 speed = new Vec3(0.0, (double)spm, 0.0);
            BallParticleOptions options = BallParticleOptions.builder().color(1 + this.f_19796_.m_188503_(40), 183 + this.f_19796_.m_188503_(60), 165 + this.f_19796_.m_188503_(60)).size(0.1f).scalingOptions(2, 10, 10).build();
            this.m_9236_().m_7106_((ParticleOptions)options, ppos.f_82479_, ppos.f_82480_, ppos.f_82481_, speed.f_82479_, speed.f_82480_, speed.f_82481_);
        }
    }

    private void doDamage(float radius, float damageBoxHeight) {
        List list = this.m_9236_().m_6443_(LivingEntity.class, new AABB((double)(-radius), 0.0, (double)(-radius), (double)radius, (double)damageBoxHeight, (double)radius).m_82383_(this.m_20182_()), e -> !(e instanceof ChesedBossBuddy) && e.m_20182_().m_82542_(1.0, 0.0, 1.0).m_82554_(this.m_20182_().m_82542_(1.0, 0.0, 1.0)) < (double)radius);
        for (LivingEntity entity : list) {
            entity.m_6469_(this.m_9236_().m_269111_().m_269425_(), this.damage);
        }
    }

    public static ChesedFireTrailEntity summon(Level level, Vec3 pos, float damage, int lifetime) {
        ChesedFireTrailEntity fireTrailEntity = new ChesedFireTrailEntity(BossEntities.CHESED_FIRE_TRAIL.get(), level);
        fireTrailEntity.m_146884_(pos);
        fireTrailEntity.damage = damage;
        fireTrailEntity.lifetime = lifetime;
        level.m_7967_((Entity)fireTrailEntity);
        return fireTrailEntity;
    }

    protected void m_8097_() {
    }

    protected void m_7378_(CompoundTag tag) {
        this.autoLoad(tag);
    }

    protected void m_7380_(CompoundTag tag) {
        this.autoSave(tag);
    }
}


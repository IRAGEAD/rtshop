/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.nbt.AutoSerializable
 *  com.finderfeed.fdlib.nbt.SerializableField
 *  com.finderfeed.fdlib.util.ProjectileMovementPath
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.EntityDataSerializers
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.damagesource.DamageSource
 *  net.minecraft.world.damagesource.DamageTypes
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.EquipmentSlot
 *  net.minecraft.world.entity.HumanoidArm
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.entities.chesed_boss.chesed_vertical_ray;

import com.finderfeed.fdbosses.BossUtil;
import com.finderfeed.fdbosses.content.entities.chesed_boss.ChesedBossBuddy;
import com.finderfeed.fdbosses.init.BossDamageSources;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdbosses.init.BossSounds;
import com.finderfeed.fdlib.nbt.AutoSerializable;
import com.finderfeed.fdlib.nbt.SerializableField;
import com.finderfeed.fdlib.util.ProjectileMovementPath;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ChesedMovingVerticalRay
extends LivingEntity
implements AutoSerializable {
    public static final EntityDataAccessor<Float> HEIGHT = SynchedEntityData.m_135353_(ChesedMovingVerticalRay.class, (EntityDataSerializer)EntityDataSerializers.f_135029_);
    @SerializableField
    private float height;
    @SerializableField
    private ProjectileMovementPath path;
    @SerializableField
    private float damage;

    public ChesedMovingVerticalRay(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
        this.f_19794_ = true;
    }

    public static ChesedMovingVerticalRay summon(Level level, Vec3 pos, ProjectileMovementPath movePath, float damage, float height) {
        ChesedMovingVerticalRay attack = new ChesedMovingVerticalRay(BossEntities.CHESED_VERTICAL_RAY_ATTACK.get(), level);
        attack.m_146884_(pos);
        attack.setDamage(damage);
        attack.setPath(movePath);
        attack.setHeight(height);
        movePath.tick((Entity)attack);
        level.m_7967_((Entity)attack);
        return attack;
    }

    public void m_8119_() {
        super.m_8119_();
        if (!this.m_9236_().f_46443_) {
            if (this.path == null) {
                this.m_142687_(Entity.RemovalReason.DISCARDED);
                return;
            }
            this.path.tick((Entity)this);
            if (this.path.isFinished()) {
                this.m_142687_(Entity.RemovalReason.DISCARDED);
            }
            this.doDamage();
            if (this.f_19797_ % 2 == 0) {
                this.m_9236_().m_6263_(null, this.m_20185_(), this.m_20186_(), this.m_20189_(), BossSounds.LIGHTNING_RAY_PASS.get(), SoundSource.HOSTILE, 1.0f, 1.0f);
            }
        } else {
            this.particles();
        }
    }

    private void particles() {
        Vec3 pos = this.m_20182_();
        for (int i = 0; i < 5; ++i) {
            BallParticleOptions options = BallParticleOptions.builder().color(0.3f, 1.0f, 1.0f, 1.0f).scalingOptions(0, 0, 20 + this.f_19796_.m_188503_(3)).physics(false).size(0.5f).friction(0.9f).build();
            Vec3 speed = new Vec3((double)0.3f, 0.0, 0.0).m_82524_(this.f_19796_.m_188501_() * (float)Math.PI * 2.0f).m_82520_(0.0, (double)(this.f_19796_.m_188501_() * 0.1f), 0.0);
            this.m_9236_().m_6493_((ParticleOptions)options, true, pos.f_82479_, pos.f_82480_, pos.f_82481_, speed.f_82479_, speed.f_82480_, speed.f_82481_);
        }
        pos = pos.m_82520_(0.0, (double)this.getHeight(), 0.0);
        float maxl = 2.0f;
        for (int i = 0; i < 2; ++i) {
            float l = this.f_19796_.m_188501_() * maxl;
            Vec3 add = new Vec3((double)l, 0.0, 0.0).m_82524_(this.f_19796_.m_188501_() * (float)Math.PI * 2.0f);
            BallParticleOptions options = BallParticleOptions.builder().color(0.3f, 1.0f, 1.0f, 1.0f).scalingOptions(0, 0, 20 + this.f_19796_.m_188503_(3)).physics(false).size(0.25f + (1.0f - l / maxl) * 2.0f).build();
            this.m_9236_().m_6493_((ParticleOptions)options, true, pos.f_82479_ + add.f_82479_, pos.f_82480_, pos.f_82481_ + add.f_82481_, 0.0, 0.0, 0.0);
        }
    }

    private void doDamage() {
        float damageRadius = 1.5f;
        AABB box = new AABB((double)(-damageRadius), 0.0, (double)(-damageRadius), (double)damageRadius, (double)this.getHeight(), (double)damageRadius).m_82383_(this.m_20182_());
        List list = this.m_9236_().m_6443_(LivingEntity.class, box, BossUtil.entityInVerticalRadiusPredicate(this.m_20182_(), damageRadius));
        for (LivingEntity entity : list) {
            if (entity instanceof ChesedBossBuddy || entity instanceof ChesedMovingVerticalRay) continue;
            entity.m_6469_(BossDamageSources.CHESED_VERTICAL_RAY_SOURCE, this.getDamage());
        }
    }

    public boolean m_20068_() {
        return true;
    }

    protected void m_8097_() {
        super.m_8097_();
        this.f_19804_.m_135372_(HEIGHT, (Object)Float.valueOf(30.0f));
    }

    public void m_7350_(EntityDataAccessor<?> accessor) {
        super.m_7350_(accessor);
        if (accessor == HEIGHT) {
            this.height = ((Float)this.f_19804_.m_135370_(HEIGHT)).floatValue();
        }
    }

    public void setHeight(float height) {
        this.f_19804_.m_135381_(HEIGHT, (Object)Float.valueOf(height));
        this.height = height;
    }

    public float getHeight() {
        return ((Float)this.f_19804_.m_135370_(HEIGHT)).floatValue();
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setPath(ProjectileMovementPath path) {
        this.path = path;
    }

    public ProjectileMovementPath getPath() {
        return this.path;
    }

    public float getDamage() {
        return this.damage;
    }

    public boolean m_20223_(CompoundTag tag) {
        this.autoSave(tag);
        return super.m_20223_(tag);
    }

    public void m_20258_(CompoundTag tag) {
        super.m_20258_(tag);
        this.autoLoad(tag);
        this.setHeight(this.height);
    }

    public boolean m_6469_(DamageSource src, float damage) {
        if (!src.m_276093_(DamageTypes.f_286979_) && !src.m_276093_(DamageTypes.f_268724_)) {
            return false;
        }
        return super.m_6469_(src, damage);
    }

    public Iterable<ItemStack> m_6168_() {
        return new ArrayList<ItemStack>();
    }

    public ItemStack m_6844_(EquipmentSlot p_21127_) {
        return ItemStack.f_41583_;
    }

    public void m_8061_(EquipmentSlot p_21036_, ItemStack p_21037_) {
    }

    public HumanoidArm m_5737_() {
        return HumanoidArm.LEFT;
    }

    public boolean m_6000_(double p_20296_, double p_20297_, double p_20298_) {
        return true;
    }

    public boolean m_6783_(double p_19883_) {
        return true;
    }
}


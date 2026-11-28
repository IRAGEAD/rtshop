/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.FDHelpers
 *  com.finderfeed.fdlib.nbt.AutoSerializable
 *  com.finderfeed.fdlib.nbt.SerializableField
 *  com.finderfeed.fdlib.util.FDColor
 *  com.finderfeed.fdlib.util.FDProjectile
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.EntityDataSerializers
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.util.Mth
 *  net.minecraft.world.damagesource.DamageSource
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.projectile.AbstractHurtingProjectile
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.Vec3
 *  org.joml.AxisAngle4f
 *  org.joml.Math
 *  org.joml.Quaternionf
 *  org.joml.Vector3d
 */
package com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_slash;

import com.finderfeed.fdbosses.client.particles.malkuth_slash.MalkuthHorizontalSlashOptions;
import com.finderfeed.fdbosses.client.particles.rush_particle.RushParticleOptions;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthAttackType;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthBossBuddy;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthDamageSource;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_repair_crystal.MalkuthRepairCrystal;
import com.finderfeed.fdbosses.init.BossDamageSources;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdbosses.init.BossEntityDataSerializers;
import com.finderfeed.fdlib.FDHelpers;
import com.finderfeed.fdlib.nbt.AutoSerializable;
import com.finderfeed.fdlib.nbt.SerializableField;
import com.finderfeed.fdlib.util.FDColor;
import com.finderfeed.fdlib.util.FDProjectile;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.AxisAngle4f;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector3d;

public class MalkuthSlashProjectile
extends FDProjectile
implements AutoSerializable {
    public static final float HEIGHT_SIZE_MODIFIER = 0.3461538f;
    public static final EntityDataAccessor<Integer> INCREMENT_SIZE_TIME = SynchedEntityData.m_135353_(MalkuthSlashProjectile.class, (EntityDataSerializer)EntityDataSerializers.f_135028_);
    public static final EntityDataAccessor<Float> MAX_SLASH_SIZE = SynchedEntityData.m_135353_(MalkuthSlashProjectile.class, (EntityDataSerializer)EntityDataSerializers.f_135029_);
    public static final EntityDataAccessor<Float> ROTATION = SynchedEntityData.m_135353_(MalkuthSlashProjectile.class, (EntityDataSerializer)EntityDataSerializers.f_135029_);
    public static final EntityDataAccessor<MalkuthAttackType> ATTACK_TYPE = SynchedEntityData.m_135353_(MalkuthSlashProjectile.class, (EntityDataSerializer)((EntityDataSerializer)BossEntityDataSerializers.MALKUTH_ATTACK_TYPE.get()));
    private int currentIncrementSizeTime = 0;
    private float slashSizeOld = 0.0f;
    private float slashSizeCurrent = 0.0f;
    @SerializableField
    private float damage = 2.0f;
    private double distanceTravelled;
    private double distanceTravelledO;

    public MalkuthSlashProjectile(EntityType<? extends AbstractHurtingProjectile> type, Level level) {
        super(type, level);
    }

    public static MalkuthSlashProjectile summon(Level level, Vec3 pos, Vec3 speed, MalkuthAttackType malkuthAttackType, float damage, float slashSize, float rotation, int incrementSizeTime) {
        MalkuthSlashProjectile malkuthSlashProjectile = new MalkuthSlashProjectile(BossEntities.MALKUTH_SLASH.get(), level);
        malkuthSlashProjectile.m_146884_(pos);
        malkuthSlashProjectile.m_20256_(speed);
        malkuthSlashProjectile.setAttackType(malkuthAttackType);
        malkuthSlashProjectile.f_19804_.m_135381_(INCREMENT_SIZE_TIME, (Object)incrementSizeTime);
        malkuthSlashProjectile.setMaxSlashSize(slashSize);
        malkuthSlashProjectile.damage = damage;
        malkuthSlashProjectile.setRotation(rotation);
        level.m_7967_((Entity)malkuthSlashProjectile);
        return malkuthSlashProjectile;
    }

    public void m_8119_() {
        Vec3 oldPos = this.m_20182_();
        super.m_8119_();
        this.tickSizeIncrement();
        Vec3 newPos = this.m_20182_();
        if (!this.m_9236_().f_46443_) {
            this.tickDamage();
        } else {
            this.distanceTravelledO = this.distanceTravelled;
            this.distanceTravelled += newPos.m_82546_(oldPos).m_82553_();
            this.slashParticles();
            this.tickParticles();
        }
    }

    private void tickSizeIncrement() {
        float size;
        this.slashSizeOld = this.slashSizeCurrent;
        int incrementSizeTime = (Integer)this.f_19804_.m_135370_(INCREMENT_SIZE_TIME);
        if (incrementSizeTime == 0) {
            this.slashSizeCurrent = this.getMaxSlashSize();
            this.slashSizeOld = this.getMaxSlashSize();
            return;
        }
        float p = FDMathUtil.clamp((float)((float)this.currentIncrementSizeTime / (float)incrementSizeTime), (float)0.0f, (float)1.0f);
        this.slashSizeCurrent = size = this.getMaxSlashSize() * p;
        this.currentIncrementSizeTime = Mth.m_14045_((int)(this.currentIncrementSizeTime + 1), (int)0, (int)incrementSizeTime);
    }

    private void slashParticles() {
        if (this.getSlashSize() == 0.0f || this.f_19797_ < 5) {
            return;
        }
        Vec3 movement = this.m_20184_();
        double distance = this.distanceTravelled - this.distanceTravelledO;
        float height = this.getSlashSize() * 0.3461538f;
        Vec3 pos = FDMathUtil.interpolateVectors((Vec3)this.m_20182_(), (Vec3)new Vec3(this.f_19854_, this.f_19855_, this.f_19856_), (float)0.5f).m_82520_(0.0, (double)(this.m_20206_() / 2.0f), 0.0);
        Vec3 n = movement.m_82541_().m_82548_().m_82542_((double)0.025f, (double)0.025f, (double)0.025f);
        for (double i = 0.0; i <= distance - (double)height * movement.m_82553_(); i += (double)height) {
            Vec3 nmovement = movement.m_82541_();
            double offset = (double)height + i;
            pos = pos.m_82549_(nmovement.m_82548_().m_82542_(offset, offset, offset));
            MalkuthHorizontalSlashOptions options = new MalkuthHorizontalSlashOptions(this.getAttackType(), this.m_20184_(), this.getSlashSize() * 0.9f, this.getRotation(), 5);
            this.m_9236_().m_6493_((ParticleOptions)options, true, pos.f_82479_, pos.f_82480_, pos.f_82481_, -n.f_82479_, -n.f_82480_, -n.f_82481_);
        }
    }

    private void tickParticles() {
        if (this.getSlashSize() == 0.0f) {
            return;
        }
        Vec3 pos = FDMathUtil.interpolateVectors((Vec3)this.m_20182_(), (Vec3)new Vec3(this.f_19854_, this.f_19855_, this.f_19856_), (float)0.5f).m_82520_(0.0, (double)(this.m_20206_() / 2.0f), 0.0);
        Vec3 movement = this.m_20184_();
        if (movement.equals((Object)Vec3.f_82478_)) {
            return;
        }
        Vec3 left = movement.m_82537_(new Vec3(0.0, 1.0, 0.0)).m_82541_();
        Vec3 nmovement = movement.m_82541_();
        Quaternionf quaternionf = new Quaternionf(new AxisAngle4f(Math.toRadians((float)(-this.getRotation())), (float)nmovement.f_82479_, (float)nmovement.f_82480_, (float)nmovement.f_82481_));
        Vector3d leftd = new Vector3d(left.f_82479_, left.f_82480_, left.f_82481_);
        quaternionf.transform(leftd);
        left = new Vec3(leftd.x, leftd.y, leftd.z);
        float slashLeftOffset = 0.3f;
        float height = this.getSlashSize() * 0.3461538f;
        for (float i = 0.0f; i <= this.getSlashSize(); i += slashLeftOffset) {
            Vec3 offsetPos = pos.m_82549_(left.m_82542_((double)i, (double)i, (double)i));
            Vec3 offsetPos2 = pos.m_82549_(left.m_82542_((double)(-i), (double)(-i), (double)(-i)));
            float p = FDEasings.easeOut((float)(1.0f - i / this.getSlashSize()));
            float l = 0.0f;
            while ((double)l < movement.m_82553_()) {
                float b;
                float g;
                float r;
                Vec3 noffset = nmovement.m_82542_((double)(height * p), (double)(height * p), (double)(height * p));
                Vec3 n = nmovement.m_82542_((double)(-l), (double)(-l), (double)(-l)).m_82549_(noffset).m_82549_(nmovement.m_82548_().m_82542_((double)height, (double)height, (double)height));
                float lmult = this.f_19796_.m_188501_() * slashLeftOffset - slashLeftOffset / 2.0f;
                Vec3 offsetPos12 = offsetPos.m_82549_(n).m_82549_(left.m_82542_((double)lmult, (double)lmult, (double)lmult));
                Vec3 offsetPos22 = offsetPos2.m_82549_(n).m_82549_(left.m_82542_((double)lmult, (double)lmult, (double)lmult));
                if (this.getAttackType() == MalkuthAttackType.FIRE) {
                    r = 0.8f + 0.2f * this.f_19796_.m_188501_();
                    g = 0.3f + 0.2f * this.f_19796_.m_188501_();
                    b = 0.05f + 0.05f * this.f_19796_.m_188501_();
                } else {
                    r = 0.2f + 0.2f * this.f_19796_.m_188501_();
                    g = 0.8f + 0.2f * this.f_19796_.m_188501_();
                    b = 0.8f + 0.2f * this.f_19796_.m_188501_();
                }
                Object particleOptions = this.f_19796_.m_188501_() > 0.1f ? BallParticleOptions.builder().size(0.05f + this.f_19796_.m_188501_() * 0.05f).scalingOptions(0, 0, 3).color(r, g, b).build() : new RushParticleOptions(movement.m_82548_(), new FDColor(r, g, b, 1.0f), this.f_19796_.m_188501_() * 0.5f + 0.25f, 0.05f + this.f_19796_.m_188501_() * 0.025f, 2 + this.f_19796_.m_188503_(2));
                this.m_9236_().m_6493_((ParticleOptions)particleOptions, true, offsetPos12.f_82479_, offsetPos12.f_82480_, offsetPos12.f_82481_, (double)(this.f_19796_.m_188501_() * 0.05f - 0.025f), (double)(this.f_19796_.m_188501_() * 0.05f - 0.025f), (double)(this.f_19796_.m_188501_() * 0.05f - 0.025f));
                this.m_9236_().m_6493_((ParticleOptions)particleOptions, true, offsetPos22.f_82479_, offsetPos22.f_82480_, offsetPos22.f_82481_, (double)(this.f_19796_.m_188501_() * 0.05f - 0.025f), (double)(this.f_19796_.m_188501_() * 0.05f - 0.025f), (double)(this.f_19796_.m_188501_() * 0.05f - 0.025f));
                l += 0.3f;
            }
        }
    }

    private void tickDamage() {
        Vec3 pos = this.m_20182_().m_82520_(0.0, (double)(this.m_20206_() / 2.0f), 0.0);
        Vec3 movement = this.m_20184_();
        if (movement.equals((Object)Vec3.f_82478_)) {
            return;
        }
        Vec3 left = movement.m_82537_(new Vec3(0.0, 1.0, 0.0)).m_82541_();
        Vec3 nmovement = movement.m_82541_();
        Quaternionf quaternionf = new Quaternionf(new AxisAngle4f(Math.toRadians((float)(-this.getRotation())), (float)nmovement.f_82479_, (float)nmovement.f_82480_, (float)nmovement.f_82481_));
        Vector3d leftd = new Vector3d(left.f_82479_, left.f_82480_, left.f_82481_);
        quaternionf.transform(leftd);
        left = new Vec3(leftd.x, leftd.y, leftd.z);
        ArrayList<Entity> damagedEntities = new ArrayList<Entity>();
        for (float i = 0.0f; i < this.getSlashSize(); i += 0.2f) {
            this.hurtTargetsOnOffset(damagedEntities, pos, movement, left, i);
        }
        this.hurtTargetsOnOffset(damagedEntities, pos, movement, left, this.getSlashSize());
    }

    private void hurtTargetsOnOffset(List<Entity> damagedEntities, Vec3 pos, Vec3 movement, Vec3 left, float offset) {
        Vec3 offsetPos = pos.m_82549_(left.m_82542_((double)offset, (double)offset, (double)offset));
        Vec3 offsetPos2 = pos.m_82549_(left.m_82542_((double)(-offset), (double)(-offset), (double)(-offset)));
        List targets = FDHelpers.traceEntities((Level)this.m_9236_(), (Vec3)offsetPos, (Vec3)offsetPos.m_82549_(movement), (double)0.0, e -> !(e instanceof MalkuthBossBuddy));
        List targets2 = FDHelpers.traceEntities((Level)this.m_9236_(), (Vec3)offsetPos2, (Vec3)offsetPos2.m_82549_(movement), (double)0.0, e -> !(e instanceof MalkuthBossBuddy));
        targets.addAll(targets2);
        for (Entity e2 : targets) {
            if (damagedEntities.contains(e2)) continue;
            if (e2 instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity)e2;
                livingEntity.m_6469_((DamageSource)new MalkuthDamageSource(BossDamageSources.MALKUTH_SLASHES_SOURCE, this.getAttackType(), 33), this.getDamage());
            } else if (e2 instanceof MalkuthRepairCrystal) {
                MalkuthRepairCrystal repairCrystal = (MalkuthRepairCrystal)e2;
                repairCrystal.destroyAndSummonRepairMaterial();
            }
            damagedEntities.add(e2);
        }
    }

    protected void m_8060_(BlockHitResult p_37258_) {
        super.m_8060_(p_37258_);
        if (this.f_19797_ > 3) {
            this.m_142687_(Entity.RemovalReason.KILLED);
        }
    }

    public float getMaxSlashSize() {
        return ((Float)this.f_19804_.m_135370_(MAX_SLASH_SIZE)).floatValue();
    }

    public float getSlashSize() {
        return this.slashSizeCurrent;
    }

    public float getSlashSize(float partialTicks) {
        if (!this.m_9236_().f_46443_) {
            return this.getSlashSize();
        }
        return FDMathUtil.lerp((float)this.slashSizeOld, (float)this.getSlashSize(), (float)partialTicks);
    }

    public void setMaxSlashSize(float size) {
        this.f_19804_.m_135381_(MAX_SLASH_SIZE, (Object)Float.valueOf(size));
    }

    public float getRotation() {
        return ((Float)this.f_19804_.m_135370_(ROTATION)).floatValue();
    }

    public void setRotation(float rotation) {
        this.f_19804_.m_135381_(ROTATION, (Object)Float.valueOf(rotation));
    }

    public MalkuthAttackType getAttackType() {
        return (MalkuthAttackType)((Object)this.f_19804_.m_135370_(ATTACK_TYPE));
    }

    public void setAttackType(MalkuthAttackType malkuthAttackType) {
        this.f_19804_.m_135381_(ATTACK_TYPE, (Object)malkuthAttackType);
    }

    public float getDamage() {
        return this.damage;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    protected void m_8097_() {
        super.m_8097_();
        this.f_19804_.m_135372_(MAX_SLASH_SIZE, (Object)Float.valueOf(1.0f));
        this.f_19804_.m_135372_(INCREMENT_SIZE_TIME, (Object)0);
        this.f_19804_.m_135372_(ROTATION, (Object)Float.valueOf(0.0f));
        this.f_19804_.m_135372_(ATTACK_TYPE, (Object)MalkuthAttackType.FIRE);
    }

    public void m_7380_(CompoundTag tag) {
        super.m_7380_(tag);
        tag.m_128350_("slashSize", this.getSlashSize());
        tag.m_128350_("rotation", this.getRotation());
        tag.m_128359_("attackType", this.getAttackType().name());
        tag.m_128405_("incrementSizeTime", ((Integer)this.f_19804_.m_135370_(INCREMENT_SIZE_TIME)).intValue());
        this.autoSave(tag);
    }

    public void m_7378_(CompoundTag tag) {
        super.m_7378_(tag);
        this.setRotation(tag.m_128457_("rotation"));
        this.setMaxSlashSize(tag.m_128457_("slashSize"));
        if (tag.m_128441_("attackType")) {
            this.setAttackType(MalkuthAttackType.valueOf(tag.m_128461_("attackType")));
        }
        this.f_19804_.m_135381_(INCREMENT_SIZE_TIME, (Object)tag.m_128451_("incrementSizeTime"));
        this.autoLoad(tag);
    }
}


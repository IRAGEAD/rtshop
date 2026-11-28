/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.nbt.AutoSerializable
 *  com.finderfeed.fdlib.nbt.SerializableField
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.EntityDataSerializers
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.damagesource.DamageSource
 *  net.minecraft.world.damagesource.DamageTypes
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$MoveFunction
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.EquipmentSlot
 *  net.minecraft.world.entity.HumanoidArm
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix4f
 *  org.joml.Vector3f
 */
package com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_chain;

import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthAttackType;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthEntity;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdbosses.init.BossEntityDataSerializers;
import com.finderfeed.fdlib.nbt.AutoSerializable;
import com.finderfeed.fdlib.nbt.SerializableField;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class MalkuthChainEntity
extends LivingEntity
implements AutoSerializable {
    private static final EntityDataAccessor<Integer> MALKUTH_ID = SynchedEntityData.m_135353_(MalkuthChainEntity.class, (EntityDataSerializer)EntityDataSerializers.f_135028_);
    private static final EntityDataAccessor<MalkuthAttackType> MALKUTH_ATTACK_TYPE = SynchedEntityData.m_135353_(MalkuthChainEntity.class, (EntityDataSerializer)((EntityDataSerializer)BossEntityDataSerializers.MALKUTH_ATTACK_TYPE.get()));
    @SerializableField
    private MalkuthAttackType malkuthAttackType;
    @SerializableField
    private UUID malkuthUUID;
    @SerializableField
    private UUID targetUUID;
    @SerializableField
    private int pullTime;
    @SerializableField
    private int catchTime;
    @SerializableField
    private Vec3 chainPullToPos;
    @SerializableField
    private Vec3 startingPos;

    public static MalkuthChainEntity summon(Level level, MalkuthEntity owner, MalkuthAttackType malkuthAttackType, Vec3 chainPullToPos, LivingEntity target, int pullTime, int catchTime) {
        MalkuthChainEntity chainEntity = new MalkuthChainEntity(BossEntities.MALKUTH_CHAIN.get(), level);
        chainEntity.setMalkuthAttackType(malkuthAttackType);
        chainEntity.targetUUID = target.m_20148_();
        chainEntity.chainPullToPos = chainPullToPos;
        chainEntity.startingPos = chainEntity.getTargetAttachmentPos(target);
        chainEntity.m_146884_(chainPullToPos);
        chainEntity.pullTime = pullTime;
        chainEntity.catchTime = catchTime;
        chainEntity.malkuthUUID = owner.m_20148_();
        level.m_7967_((Entity)chainEntity);
        return chainEntity;
    }

    public MalkuthChainEntity(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
        this.f_19794_ = true;
        this.m_20242_(true);
    }

    public void m_8119_() {
        super.m_8119_();
        if (!this.m_9236_().f_46443_) {
            MalkuthEntity malkuthEntity = this.getMalkuth();
            if (malkuthEntity != null) {
                this.f_19804_.m_135381_(MALKUTH_ID, (Object)malkuthEntity.m_19879_());
            }
            if (malkuthEntity == null || this.getTarget() == null || this.getTarget().m_21224_()) {
                this.m_142467_(Entity.RemovalReason.DISCARDED);
            }
            this.moveToTargetEndPoint();
        }
    }

    private void moveToTargetEndPoint() {
        if (this.f_19797_ > this.catchTime && this.f_19797_ - this.catchTime <= this.pullTime) {
            float p = (float)(this.f_19797_ - this.catchTime) / (float)this.pullTime;
            Vec3 targetPoint = FDMathUtil.interpolateVectors((Vec3)this.startingPos, (Vec3)this.chainPullToPos, (float)p);
            Vec3 current = this.m_20182_();
            Vec3 deltaMovement = targetPoint.m_82546_(current);
            this.m_20256_(deltaMovement);
            LivingEntity target = this.getTarget();
            if (target != null && !this.m_20363_((Entity)target)) {
                target.m_7998_((Entity)this, true);
            }
        } else if (this.f_19797_ <= this.catchTime) {
            LivingEntity target = this.getTarget();
            if (target == null) {
                return;
            }
            Vec3 targetAttachmentPos = this.getTargetAttachmentPos(target);
            float p = (float)this.f_19797_ / (float)this.pullTime;
            Vec3 targetPoint = FDMathUtil.interpolateVectors((Vec3)this.chainPullToPos, (Vec3)targetAttachmentPos, (float)p);
            Vec3 current = this.m_20182_();
            Vec3 deltaMovement = targetPoint.m_82546_(current);
            this.m_20256_(deltaMovement);
            if (this.f_19797_ == this.catchTime) {
                this.startingPos = targetAttachmentPos;
                this.m_146884_(targetAttachmentPos);
            }
        } else {
            this.m_20256_(Vec3.f_82478_);
        }
    }

    public boolean shouldRiderSit() {
        return false;
    }

    public Vec3 getTargetAttachmentPos(LivingEntity target) {
        return target.m_20182_().m_82520_(0.0, (double)(target.m_20206_() / 2.0f - 1.0f), 0.0);
    }

    public double m_6048_() {
        return -0.5;
    }

    protected void m_19956_(Entity entity, Entity.MoveFunction p_19958_) {
        super.m_19956_(entity, p_19958_);
    }

    public LivingEntity getTarget() {
        Entity entity;
        if (this.targetUUID != null && !this.m_9236_().f_46443_ && (entity = ((ServerLevel)this.m_9236_()).m_8791_(this.targetUUID)) instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity)entity;
            return livingEntity;
        }
        return null;
    }

    public MalkuthEntity getMalkuth() {
        if (!this.m_9236_().f_46443_) {
            if (this.malkuthUUID != null) {
                Entity entity = ((ServerLevel)this.m_9236_()).m_8791_(this.malkuthUUID);
                if (entity instanceof MalkuthEntity) {
                    MalkuthEntity malkuthEntity = (MalkuthEntity)entity;
                    return malkuthEntity;
                }
                return null;
            }
            return null;
        }
        Entity entity = this.m_9236_().m_6815_(((Integer)this.f_19804_.m_135370_(MALKUTH_ID)).intValue());
        if (entity instanceof MalkuthEntity) {
            MalkuthEntity malkuthEntity = (MalkuthEntity)entity;
            return malkuthEntity;
        }
        return null;
    }

    protected Vec3 getMalkuthHandPos(float pticks) {
        MalkuthEntity malkuthEntity = this.getMalkuth();
        if (malkuthEntity != null) {
            String name = ((MalkuthAttackType)((Object)this.f_19804_.m_135370_(MALKUTH_ATTACK_TYPE))).isFire() ? "fire_sword_place" : "ice_sword_place";
            Matrix4f mt = malkuthEntity.getModelPartTransformation((Entity)malkuthEntity, name, MalkuthEntity.getClientModel(), pticks);
            Vector3f p = mt.transformPosition(new Vector3f());
            Vec3 mpos = malkuthEntity.m_20318_(pticks);
            return new Vec3((double)p.x + mpos.f_82479_, (double)p.y + mpos.f_82480_, (double)p.z + mpos.f_82481_);
        }
        return null;
    }

    public void setMalkuthAttackType(MalkuthAttackType malkuthAttackType) {
        this.malkuthAttackType = malkuthAttackType;
        this.f_19804_.m_135381_(MALKUTH_ATTACK_TYPE, (Object)malkuthAttackType);
    }

    private void setCatchTime(int catchTime) {
        this.catchTime = catchTime;
    }

    public HumanoidArm m_5737_() {
        return HumanoidArm.LEFT;
    }

    public boolean m_6469_(DamageSource src, float p_21017_) {
        if (!src.m_276093_(DamageTypes.f_286979_)) {
            return false;
        }
        return super.m_6469_(src, p_21017_);
    }

    public Iterable<ItemStack> m_6168_() {
        return new ArrayList<ItemStack>();
    }

    public ItemStack m_6844_(EquipmentSlot p_21127_) {
        return ItemStack.f_41583_;
    }

    public void m_8061_(EquipmentSlot p_21036_, ItemStack p_21037_) {
    }

    protected void m_8097_() {
        super.m_8097_();
        this.f_19804_.m_135372_(MALKUTH_ID, (Object)-1);
        this.f_19804_.m_135372_(MALKUTH_ATTACK_TYPE, (Object)MalkuthAttackType.FIRE);
    }

    public void m_7380_(CompoundTag tag) {
        super.m_7380_(tag);
        this.autoSave(tag);
    }

    public void m_7378_(CompoundTag tag) {
        super.m_7378_(tag);
        this.autoLoad(tag);
        this.setMalkuthAttackType(this.malkuthAttackType);
        this.setCatchTime(this.catchTime);
    }

    public boolean m_6000_(double p_20296_, double p_20297_, double p_20298_) {
        return true;
    }

    public boolean m_6783_(double p_19883_) {
        return true;
    }

    public void m_5997_(double p_20286_, double p_20287_, double p_20288_) {
    }

    public void m_7334_(Entity p_21294_) {
    }

    protected void m_6138_() {
    }
}


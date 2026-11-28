/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.HolderGetter
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.NbtUtils
 *  net.minecraft.nbt.Tag
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.EntityDataSerializers
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.MoverType
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 */
package com.finderfeed.fdbosses.content.entities.chesed_boss.flying_block_entity;

import com.finderfeed.fdbosses.BossUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class FlyingBlockEntity
extends Entity {
    public static final EntityDataAccessor<BlockState> STATE = SynchedEntityData.m_135353_(FlyingBlockEntity.class, (EntityDataSerializer)EntityDataSerializers.f_135034_);
    public static final EntityDataAccessor<Float> ROTATION_SPEED = SynchedEntityData.m_135353_(FlyingBlockEntity.class, (EntityDataSerializer)EntityDataSerializers.f_135029_);
    public static final EntityDataAccessor<Float> FRICTION = SynchedEntityData.m_135353_(FlyingBlockEntity.class, (EntityDataSerializer)EntityDataSerializers.f_135029_);
    public int noPhysicsTime = 0;

    public FlyingBlockEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public void m_8119_() {
        if (!this.m_9236_().f_46443_) {
            if (this.noPhysicsTime > 0) {
                --this.noPhysicsTime;
                this.f_19794_ = true;
            } else {
                this.f_19794_ = false;
            }
            if (this.f_19797_ >= 2000 || this.m_20096_() || this.getBlockState().m_60795_()) {
                this.m_146870_();
            }
        }
        float friction = this.getAirFriction();
        this.m_20256_(this.m_20184_().m_82542_((double)friction, 1.0, (double)friction));
        BossUtil.applyGravity(this, this.getDefaultGravity());
        this.m_6478_(MoverType.SELF, this.m_20184_());
    }

    public void setNoPhysicsTime(int noPhysicsTime) {
        this.noPhysicsTime = noPhysicsTime;
    }

    public int getNoPhysicsTime() {
        return this.noPhysicsTime;
    }

    public float getRotationSpeed() {
        return ((Float)this.f_19804_.m_135370_(ROTATION_SPEED)).floatValue();
    }

    public void setRotationSpeed(float rotationSpeed) {
        this.f_19804_.m_135381_(ROTATION_SPEED, (Object)Float.valueOf(rotationSpeed));
    }

    public float getAirFriction() {
        return ((Float)this.f_19804_.m_135370_(FRICTION)).floatValue();
    }

    public void setAirFriction(float friction) {
        this.f_19804_.m_135381_(FRICTION, (Object)Float.valueOf(friction));
    }

    public BlockState getBlockState() {
        return (BlockState)this.f_19804_.m_135370_(STATE);
    }

    public void setBlockState(BlockState state) {
        this.f_19804_.m_135381_(STATE, (Object)state);
    }

    protected void m_7355_(BlockPos p_20135_, BlockState p_20136_) {
    }

    protected double getDefaultGravity() {
        return 0.04;
    }

    protected void m_8097_() {
        this.f_19804_.m_135372_(ROTATION_SPEED, (Object)Float.valueOf(1.0f));
        this.f_19804_.m_135372_(FRICTION, (Object)Float.valueOf(1.0f));
        this.f_19804_.m_135372_(STATE, (Object)Blocks.f_50069_.m_49966_());
    }

    protected void m_7378_(CompoundTag tag) {
        tag.m_128350_("rotationSpeed", this.getRotationSpeed());
        tag.m_128365_("state", (Tag)NbtUtils.m_129202_((BlockState)this.getBlockState()));
        tag.m_128405_("noPhysicsTime", this.getNoPhysicsTime());
    }

    protected void m_7380_(CompoundTag tag) {
        this.setRotationSpeed(tag.m_128457_("rotationSpeed"));
        this.setBlockState(NbtUtils.m_247651_((HolderGetter)this.m_9236_().m_246945_(Registries.f_256747_), (CompoundTag)tag.m_128469_("state")));
        this.setNoPhysicsTime(tag.m_128451_("noPhysicsTime"));
    }
}


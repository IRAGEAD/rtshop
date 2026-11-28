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
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.entities.chesed_boss.earthshatter_entity;

import com.finderfeed.fdbosses.content.entities.chesed_boss.earthshatter_entity.EarthShatterSettings;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdbosses.init.BossEntityDataSerializers;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class EarthShatterEntity
extends Entity {
    public static final EntityDataAccessor<BlockState> STATE = SynchedEntityData.m_135353_(EarthShatterEntity.class, (EntityDataSerializer)EntityDataSerializers.f_135034_);
    public static final EntityDataAccessor<EarthShatterSettings> EARTH_SHATTER_SETTINGS = SynchedEntityData.m_135353_(EarthShatterEntity.class, (EntityDataSerializer)((EntityDataSerializer)BossEntityDataSerializers.EARTH_SHATTER_SETTINGS.get()));
    public EarthShatterSettings settings;

    public EarthShatterEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public static EarthShatterEntity summon(Level level, BlockPos pos, EarthShatterSettings settings) {
        BlockState state = level.m_8055_(pos);
        if (state.m_60795_()) {
            return null;
        }
        if (!level.m_45976_(EarthShatterEntity.class, new AABB((double)pos.m_123341_() + 0.1, (double)pos.m_123342_(), (double)pos.m_123343_() + 0.1, (double)pos.m_123341_() + 0.9, (double)pos.m_123342_() + 0.9, (double)pos.m_123343_() + 0.9)).isEmpty()) {
            return null;
        }
        EarthShatterEntity entity = new EarthShatterEntity(BossEntities.EARTH_SHATTER.get(), level);
        entity.m_6034_((double)pos.m_123341_() + 0.5, pos.m_123342_(), (double)pos.m_123343_() + 0.5);
        entity.settings = settings;
        entity.f_19804_.m_135381_(EARTH_SHATTER_SETTINGS, (Object)settings);
        entity.setBlockState(state);
        level.m_7967_((Entity)entity);
        return entity;
    }

    public void m_8119_() {
        super.m_8119_();
        if (!this.m_9236_().f_46443_) {
            if (this.settings == null) {
                this.m_142687_(Entity.RemovalReason.DISCARDED);
                return;
            }
            if (this.f_19797_ > this.settings.getLifetime()) {
                this.m_142687_(Entity.RemovalReason.DISCARDED);
            }
        }
    }

    public BlockState getBlockState() {
        return (BlockState)this.f_19804_.m_135370_(STATE);
    }

    public void setBlockState(BlockState state) {
        this.f_19804_.m_135381_(STATE, (Object)state);
    }

    public Vec3 getShatterDirection() {
        if (!this.m_9236_().f_46443_) {
            return this.settings.direction;
        }
        return ((EarthShatterSettings)this.f_19804_.m_135370_(EarthShatterEntity.EARTH_SHATTER_SETTINGS)).direction;
    }

    protected void m_8097_() {
        this.f_19804_.m_135372_(STATE, (Object)Blocks.f_50069_.m_49966_());
        this.f_19804_.m_135372_(EARTH_SHATTER_SETTINGS, (Object)new EarthShatterSettings());
    }

    public void m_6457_(ServerPlayer p_20119_) {
        super.m_6457_(p_20119_);
    }

    protected void m_7378_(CompoundTag tag) {
        this.settings = new EarthShatterSettings();
        this.settings.autoLoad("settings", tag);
        this.setBlockState(NbtUtils.m_247651_((HolderGetter)this.m_9236_().m_246945_(Registries.f_256747_), (CompoundTag)tag));
        this.f_19804_.m_135381_(EARTH_SHATTER_SETTINGS, (Object)this.settings);
    }

    protected void m_7380_(CompoundTag tag) {
        this.settings.autoSave("settings", tag);
        tag.m_128365_("state", (Tag)NbtUtils.m_129202_((BlockState)this.getBlockState()));
    }

    public boolean m_6000_(double p_20296_, double p_20297_, double p_20298_) {
        return super.m_6000_(p_20296_, p_20297_, p_20298_);
    }

    public boolean m_6783_(double p_19883_) {
        return true;
    }
}


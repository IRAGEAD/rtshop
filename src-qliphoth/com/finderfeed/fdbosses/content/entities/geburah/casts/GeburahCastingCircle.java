/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.init.FDEDataSerializers
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.EntityDataSerializers
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.entities.geburah.casts;

import com.finderfeed.fdlib.init.FDEDataSerializers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public abstract class GeburahCastingCircle
extends Entity {
    public static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.m_135353_(GeburahCastingCircle.class, (EntityDataSerializer)EntityDataSerializers.f_135028_);
    public static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.m_135353_(GeburahCastingCircle.class, (EntityDataSerializer)EntityDataSerializers.f_135028_);
    public static final EntityDataAccessor<Vec3> DIRECTION = SynchedEntityData.m_135353_(GeburahCastingCircle.class, (EntityDataSerializer)((EntityDataSerializer)FDEDataSerializers.VEC3.get()));
    public static final int FADE_IN = 10;
    public static final int FADE_OUT = 10;
    public boolean wasCast = false;

    public GeburahCastingCircle(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    public void m_8119_() {
        super.m_8119_();
        if (!this.m_9236_().f_46443_) {
            int castTick = this.getDuration() / 2;
            if (this.f_19797_ == castTick) {
                if (!this.wasCast) {
                    this.cast();
                    this.wasCast = true;
                }
            } else if (this.f_19797_ > this.getDuration()) {
                this.m_146870_();
            }
        }
    }

    public abstract void cast();

    public Vec3 getCastDirection() {
        return (Vec3)this.m_20088_().m_135370_(DIRECTION);
    }

    public void setDirection(Vec3 direction) {
        this.m_20088_().m_135381_(DIRECTION, (Object)direction.m_82541_());
    }

    public int getDuration() {
        return (Integer)this.m_20088_().m_135370_(DURATION);
    }

    public void setCastDuration(int duration) {
        this.m_20088_().m_135381_(DURATION, (Object)(20 + duration));
    }

    public void setColor(int color) {
        this.m_20088_().m_135381_(COLOR, (Object)color);
    }

    protected void m_8097_() {
        this.m_20088_().m_135372_(DURATION, (Object)20);
        this.m_20088_().m_135372_(COLOR, (Object)-1);
        this.m_20088_().m_135372_(DIRECTION, (Object)new Vec3(0.0, 1.0, 0.0));
    }

    protected void m_7378_(CompoundTag tag) {
        if (tag.m_128441_("color")) {
            this.m_20088_().m_135381_(COLOR, (Object)tag.m_128451_("color"));
        }
        if (tag.m_128441_("duration")) {
            this.m_20088_().m_135381_(DURATION, (Object)tag.m_128451_("duration"));
        }
        if (tag.m_128441_("dir_x")) {
            this.m_20088_().m_135381_(DIRECTION, (Object)new Vec3(tag.m_128459_("dir_x"), tag.m_128459_("dir_y"), tag.m_128459_("dir_z")));
        }
        this.wasCast = tag.m_128471_("was_cast");
    }

    protected void m_7380_(CompoundTag tag) {
        tag.m_128405_("color", ((Integer)this.m_20088_().m_135370_(COLOR)).intValue());
        tag.m_128405_("duration", ((Integer)this.m_20088_().m_135370_(DURATION)).intValue());
        Vec3 dir = (Vec3)this.m_20088_().m_135370_(DIRECTION);
        tag.m_128347_("dir_x", dir.f_82479_);
        tag.m_128347_("dir_y", dir.f_82480_);
        tag.m_128347_("dir_z", dir.f_82481_);
        tag.m_128379_("was_cast", this.wasCast);
    }

    public boolean m_6783_(double p_19883_) {
        return true;
    }
}


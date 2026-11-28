/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.nbt.AutoSerializable
 *  com.finderfeed.fdlib.nbt.SerializableField
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.entities.geburah.casts;

import com.finderfeed.fdbosses.content.entities.geburah.casts.GeburahCastingCircle;
import com.finderfeed.fdbosses.content.entities.geburah.chain_trap.ChainTrapSummonProjectile;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdlib.nbt.AutoSerializable;
import com.finderfeed.fdlib.nbt.SerializableField;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class GeburahChainTrapCastCircle
extends GeburahCastingCircle
implements AutoSerializable {
    @SerializableField
    private Vec3 projectileEndPos;

    public static void summon(Level level, Vec3 pos, Vec3 direction, Vec3 projectileEndPos) {
        GeburahChainTrapCastCircle castCircle = new GeburahChainTrapCastCircle(BossEntities.GEBURAH_CASTING_CIRCLE_CHAIN_TRAP.get(), level);
        castCircle.m_146884_(pos);
        castCircle.projectileEndPos = projectileEndPos;
        castCircle.setDirection(direction);
        castCircle.setCastDuration(10);
        castCircle.m_20088_().m_135381_(COLOR, (Object)-5579265);
        level.m_7967_((Entity)castCircle);
    }

    public GeburahChainTrapCastCircle(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void cast() {
        if (this.projectileEndPos == null) {
            this.m_146870_();
        } else {
            ChainTrapSummonProjectile.summon(this.m_9236_(), this.m_20182_(), this.projectileEndPos);
        }
    }

    public boolean m_6087_() {
        return true;
    }

    @Override
    protected void m_7378_(CompoundTag tag) {
        super.m_7378_(tag);
        this.autoLoad(tag);
    }

    @Override
    protected void m_7380_(CompoundTag tag) {
        super.m_7380_(tag);
        this.autoSave(tag);
    }
}


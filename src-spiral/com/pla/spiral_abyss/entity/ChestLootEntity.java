/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ClientGamePacketListener
 *  net.minecraft.network.protocol.game.ClientboundAddEntityPacket
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.EntityDataSerializers
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.item.ItemEntity
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 */
package com.pla.spiral_abyss.entity;

import com.pla.spiral_abyss.init.EntityInit;
import java.util.ArrayDeque;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ChestLootEntity
extends ItemEntity {
    private static final EntityDataAccessor<Boolean> RARE = SynchedEntityData.m_135353_(ChestLootEntity.class, (EntityDataSerializer)EntityDataSerializers.f_135035_);
    private final ArrayDeque<Vec3> trail = new ArrayDeque();

    public ChestLootEntity(EntityType<? extends ItemEntity> type, Level level) {
        super(type, level);
    }

    public ChestLootEntity(Level level, double x, double y, double z, ItemStack stack, boolean rare) {
        this((EntityType<? extends ItemEntity>)((EntityType)EntityInit.CHEST_LOOT.get()), level);
        this.m_6034_(x, y, z);
        this.m_32045_(stack.m_41777_());
        this.f_19804_.m_135381_(RARE, (Object)rare);
    }

    protected void m_8097_() {
        super.m_8097_();
        this.f_19804_.m_135372_(RARE, (Object)false);
    }

    public boolean rare() {
        return (Boolean)this.f_19804_.m_135370_(RARE);
    }

    public List<Vec3> trail() {
        return List.copyOf(this.trail);
    }

    public AABB m_6921_() {
        return this.m_20191_().m_82400_(4.0);
    }

    public void m_8119_() {
        if (this.m_9236_().f_46443_) {
            if (!this.m_20096_()) {
                this.trail.addLast(this.m_20182_().m_82520_(0.0, 0.15, 0.0));
                while (this.trail.size() > 12) {
                    this.trail.removeFirst();
                }
            } else {
                this.trail.clear();
            }
        }
        super.m_8119_();
    }

    public void m_7380_(CompoundTag tag) {
        super.m_7380_(tag);
        tag.m_128379_("RareChestLoot", this.rare());
    }

    public void m_7378_(CompoundTag tag) {
        super.m_7378_(tag);
        this.f_19804_.m_135381_(RARE, (Object)tag.m_128471_("RareChestLoot"));
    }

    public Packet<ClientGamePacketListener> m_5654_() {
        return new ClientboundAddEntityPacket((Entity)this);
    }
}


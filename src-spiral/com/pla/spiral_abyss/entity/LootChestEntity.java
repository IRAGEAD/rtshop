/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.commands.arguments.EntityAnchorArgument$Anchor
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.EntityDataSerializers
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.AABB
 */
package com.pla.spiral_abyss.entity;

import com.pla.spiral_abyss.entity.ArenaInteractable;
import com.pla.spiral_abyss.world.AbyssParties;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public abstract class LootChestEntity
extends ArenaInteractable {
    public static final int IDLE = 0;
    public static final int OPEN = 1;
    public static final int DISPENSING = 2;
    public static final int LOOTED = 3;
    public static final int SPAWN = 4;
    public static final int SPAWN_TICKS = 11;
    public static final int OPEN_TICKS = 10;
    public static final int DROP_INTERVAL_TICKS = 6;
    public static final int CONTAINER_GLOW_TICKS = 100;
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.m_135353_(LootChestEntity.class, (EntityDataSerializer)EntityDataSerializers.f_135028_);
    private static final EntityDataAccessor<Long> PHASE_START = SynchedEntityData.m_135353_(LootChestEntity.class, (EntityDataSerializer)EntityDataSerializers.f_244073_);
    private static final EntityDataAccessor<Boolean> RARE = SynchedEntityData.m_135353_(LootChestEntity.class, (EntityDataSerializer)EntityDataSerializers.f_135035_);
    private final int tier;

    protected LootChestEntity(EntityType<? extends Mob> type, Level level, int tier) {
        super(type, level);
        this.tier = tier;
    }

    public int tier() {
        return this.tier;
    }

    public AABB m_6921_() {
        return this.m_20191_().m_82377_(1.0, 2.0, 1.0);
    }

    @Override
    public void m_8119_() {
        super.m_8119_();
        if (this.phase() != 0) {
            return;
        }
        Player player = this.m_9236_().m_45930_((Entity)this, 10.0);
        if (player != null) {
            this.m_7618_(EntityAnchorArgument.Anchor.EYES, player.m_20182_());
        }
    }

    protected void m_8097_() {
        super.m_8097_();
        this.f_19804_.m_135372_(PHASE, (Object)0);
        this.f_19804_.m_135372_(PHASE_START, (Object)0L);
        this.f_19804_.m_135372_(RARE, (Object)false);
    }

    public int phase() {
        return (Integer)this.f_19804_.m_135370_(PHASE);
    }

    public long phaseStart() {
        return (Long)this.f_19804_.m_135370_(PHASE_START);
    }

    public boolean rarePayout() {
        return (Boolean)this.f_19804_.m_135370_(RARE);
    }

    public void setRarePayout(boolean rare) {
        this.f_19804_.m_135381_(RARE, (Object)(this.tier == 3 && rare ? 1 : 0));
    }

    public void setPhase(int phase, long start) {
        this.f_19804_.m_135381_(PHASE, (Object)phase);
        this.f_19804_.m_135381_(PHASE_START, (Object)start);
    }

    @Override
    public void m_7380_(CompoundTag tag) {
        super.m_7380_(tag);
        tag.m_128405_("ChestPhase", this.phase());
        tag.m_128356_("ChestPhaseStart", this.phaseStart());
        tag.m_128379_("RarePayout", this.rarePayout());
    }

    @Override
    public void m_7378_(CompoundTag tag) {
        super.m_7378_(tag);
        this.setPhase(Math.max(0, Math.min(4, tag.m_128451_("ChestPhase"))), tag.m_128454_("ChestPhaseStart"));
        this.setRarePayout(tag.m_128471_("RarePayout"));
    }

    protected InteractionResult m_6071_(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)player;
            AbyssParties.get(serverPlayer.f_8924_).openChest(serverPlayer, this);
        }
        return InteractionResult.m_19078_((boolean)this.m_9236_().f_46443_);
    }
}


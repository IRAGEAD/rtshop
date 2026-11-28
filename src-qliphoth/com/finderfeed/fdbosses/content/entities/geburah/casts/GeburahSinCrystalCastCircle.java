/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.nbt.AutoSerializable
 *  com.finderfeed.fdlib.nbt.SerializableField
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.entities.geburah.casts;

import com.finderfeed.fdbosses.content.entities.geburah.casts.GeburahCastingCircle;
import com.finderfeed.fdbosses.content.entities.geburah.geburah_explosive_crystal.GeburahSinCrystal;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdlib.nbt.AutoSerializable;
import com.finderfeed.fdlib.nbt.SerializableField;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class GeburahSinCrystalCastCircle
extends GeburahCastingCircle
implements AutoSerializable {
    @SerializableField
    private UUID uuid;

    public GeburahSinCrystalCastCircle(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    public static void summon(Level level, Vec3 pos, Vec3 direction, Player player) {
        GeburahSinCrystalCastCircle castCircle = new GeburahSinCrystalCastCircle(BossEntities.GEBURAH_CASTING_CIRCLE_SIN_CRYSTAL.get(), level);
        castCircle.m_146884_(pos);
        castCircle.setDirection(direction);
        castCircle.setCastDuration(20);
        castCircle.m_20088_().m_135381_(COLOR, (Object)-5579265);
        castCircle.uuid = player.m_20148_();
        level.m_7967_((Entity)castCircle);
    }

    @Override
    public void cast() {
        Player player;
        ServerLevel serverLevel = (ServerLevel)this.m_9236_();
        if (this.uuid != null && (player = serverLevel.m_46003_(this.uuid)) instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)player;
            GeburahSinCrystal.summon(this.m_20182_(), (Player)serverPlayer, this.getCastDirection());
        }
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


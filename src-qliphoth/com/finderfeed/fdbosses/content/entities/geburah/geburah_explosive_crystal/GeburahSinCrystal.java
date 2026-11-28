/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.nbt.AutoSerializable
 *  com.finderfeed.fdlib.nbt.SerializableField
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDEntity
 *  com.finderfeed.fdlib.systems.trails.FDTrailDataGenerator
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  net.minecraft.core.NonNullList
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.item.ItemEntity
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Vector3d
 *  org.joml.Vector3dc
 */
package com.finderfeed.fdbosses.content.entities.geburah.geburah_explosive_crystal;

import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdbosses.init.BossItems;
import com.finderfeed.fdlib.nbt.AutoSerializable;
import com.finderfeed.fdlib.nbt.SerializableField;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDEntity;
import com.finderfeed.fdlib.systems.trails.FDTrailDataGenerator;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import java.util.UUID;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class GeburahSinCrystal
extends FDEntity
implements AutoSerializable {
    @SerializableField
    private UUID playerToFlyTo;
    public FDTrailDataGenerator<GeburahSinCrystal> trail;

    public static GeburahSinCrystal summon(Vec3 pos, Player player, Vec3 initialDirection) {
        GeburahSinCrystal crystal = new GeburahSinCrystal(BossEntities.GEBURAH_SIN_CRYSTAL.get(), player.m_9236_());
        crystal.playerToFlyTo = player.m_20148_();
        crystal.m_20256_(initialDirection.m_82541_());
        crystal.m_146884_(pos);
        player.m_9236_().m_7967_((Entity)crystal);
        return crystal;
    }

    public GeburahSinCrystal(EntityType<?> type, Level level) {
        super(type, level);
        if (level.f_46443_) {
            this.trail = new FDTrailDataGenerator(Entity::m_20318_, 5, 0.01f);
        }
    }

    public void m_8119_() {
        super.m_8119_();
        if (!this.m_9236_().f_46443_) {
            this.flyToTargetPlayer();
            this.m_146884_(this.m_20182_().m_82549_(this.m_20184_()));
        } else {
            this.m_146884_(this.m_20182_().m_82549_(this.m_20184_()));
            this.trail.tick((Object)this);
        }
    }

    public void flyToTargetPlayer() {
        Player player = this.getTargetPlayer();
        if (player == null || player.m_21224_()) {
            this.m_142467_(Entity.RemovalReason.DISCARDED);
        }
        Vec3 targetPos = this.getTargetPos(player);
        if (this.m_20182_().m_82554_(targetPos) < 1.0) {
            this.putCrystalInPlayersInventory(player);
            this.m_142687_(Entity.RemovalReason.DISCARDED);
        }
        float p = FDEasings.easeOut((float)Mth.m_14036_((float)((float)this.f_19797_ / 100.0f), (float)0.0f, (float)1.0f));
        Vec3 currentSpeed = this.m_20184_();
        Vec3 targetSpeed = targetPos.m_82546_(this.m_20182_());
        Vector3d currentSpeedv = new Vector3d(currentSpeed.f_82479_, currentSpeed.f_82480_, currentSpeed.f_82481_).normalize();
        Vector3d targetSpeedv = new Vector3d(targetSpeed.f_82479_, targetSpeed.f_82480_, targetSpeed.f_82481_).normalize();
        Vector3d axis = currentSpeedv.cross((Vector3dc)targetSpeedv, new Vector3d());
        double angleBetween = currentSpeedv.angle((Vector3dc)targetSpeedv);
        currentSpeedv.rotateAxis(angleBetween * (double)p, axis.x, axis.y, axis.z);
        float spMod = 1.0f;
        this.m_20334_(currentSpeedv.x * (double)spMod, currentSpeedv.y * (double)spMod, currentSpeedv.z * (double)spMod);
    }

    public Player getTargetPlayer() {
        if (this.playerToFlyTo == null) {
            return null;
        }
        return ((ServerLevel)this.m_9236_()).m_46003_(this.playerToFlyTo);
    }

    public void putCrystalInPlayersInventory(Player player) {
        Inventory inventory = player.m_150109_();
        NonNullList items = inventory.f_35974_;
        int randomSlot = this.f_19796_.m_188503_(items.size());
        ItemStack cr = (ItemStack)items.get(randomSlot);
        for (int tries = 100; (cr.m_150930_(BossItems.GEBURAH_EXPLOSIVE_CRYSTAL.get()) || Inventory.m_36045_((int)randomSlot)) && tries > 0; --tries) {
            randomSlot = this.f_19796_.m_188503_(items.size());
            cr = (ItemStack)items.get(randomSlot);
        }
        ItemStack itemInSlot = (ItemStack)items.get(randomSlot);
        items.set(randomSlot, BossItems.GEBURAH_EXPLOSIVE_CRYSTAL.get().m_7968_());
        if (!itemInSlot.m_41619_()) {
            int freeSlot = inventory.m_36062_();
            if (freeSlot != -1) {
                items.set(freeSlot, itemInSlot.m_41777_());
            } else {
                ItemEntity item = new ItemEntity(this.m_9236_(), player.m_20185_(), player.m_20186_() + (double)(player.m_20206_() / 2.0f), player.m_20189_(), itemInSlot.m_41777_());
                Vec3 speed = player.m_20154_().m_82490_(0.5);
                item.m_32060_();
                item.m_20256_(speed);
                this.m_9236_().m_7967_((Entity)item);
            }
        }
        inventory.m_6596_();
        this.m_9236_().m_6263_(null, player.m_20185_(), player.m_20186_() + (double)player.m_20192_(), player.m_20189_(), SoundEvents.f_12019_, SoundSource.PLAYERS, 1.0f, 2.0f);
    }

    public Vec3 getTargetPos(Player player) {
        return player.m_20182_().m_82520_(0.0, (double)(player.m_20206_() / 2.0f), 0.0);
    }

    protected void m_8097_() {
    }

    protected void m_7378_(CompoundTag tag) {
        super.m_7378_(tag);
        this.autoLoad(tag);
    }

    protected void m_7380_(CompoundTag tag) {
        super.m_7380_(tag);
        this.autoSave(tag);
    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.network.FDPacketHandler
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDEntity
 *  net.minecraft.core.BlockPos
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.EntityDataSerializers
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.damagesource.DamageSource
 *  net.minecraft.world.damagesource.DamageTypes
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.item.ItemEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.network.NetworkDirection
 *  net.minecraftforge.network.PacketDistributor
 */
package com.finderfeed.fdbosses.content.entities.base;

import com.finderfeed.fdbosses.content.entities.base.BossSpawnerContextAssignable;
import com.finderfeed.fdbosses.content.entities.base.ForceDossierClosePacket;
import com.finderfeed.fdbosses.init.BossItems;
import com.finderfeed.fdbosses.packets.OpenBossDossierPacket;
import com.finderfeed.fdlib.network.FDPacketHandler;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;

public abstract class BossSpawnerEntity
extends FDEntity {
    public static final String CODEX_RECEIVED = "fdbosses_boss_codex_received";
    public static final EntityDataAccessor<Boolean> ACTIVE = SynchedEntityData.m_135353_(BossSpawnerEntity.class, (EntityDataSerializer)EntityDataSerializers.f_135035_);

    public BossSpawnerEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public InteractionResult m_6096_(Player player, InteractionHand hand) {
        if (!this.isActive()) {
            return InteractionResult.PASS;
        }
        if (hand == InteractionHand.MAIN_HAND && this.isActive()) {
            if (!this.m_9236_().f_46443_) {
                CompoundTag persistentData = player.getPersistentData();
                if (!persistentData.m_128471_(CODEX_RECEIVED)) {
                    persistentData.m_128379_(CODEX_RECEIVED, true);
                    if (!player.m_150109_().m_36054_(BossItems.QLIPHOTHIC_CODEX.get().m_7968_())) {
                        ItemEntity itemEntity = new ItemEntity(this.m_9236_(), player.m_20185_(), player.m_20186_() + 1.0, player.m_20189_(), BossItems.QLIPHOTHIC_CODEX.get().m_7968_());
                        this.m_9236_().m_7967_((Entity)itemEntity);
                    }
                }
                FDPacketHandler.INSTANCE.sendTo((Object)new OpenBossDossierPacket(this), ((ServerPlayer)player).f_8906_.f_9742_, NetworkDirection.PLAY_TO_CLIENT);
            }
            return InteractionResult.SUCCESS;
        }
        return super.m_6096_(player, hand);
    }

    public void spawn() {
        if (this.isActive()) {
            FDPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> this), (Object)new ForceDossierClosePacket());
            EntityType<? extends BossSpawnerContextAssignable> type = this.getBossEntityType();
            Entity entity = type.m_20615_(this.m_9236_());
            entity.m_146922_(0.0f);
            entity.m_5618_(0.0f);
            entity.m_5616_(0.0f);
            ((BossSpawnerContextAssignable)entity).setSpawnedBy(this);
            entity.m_146884_(this.m_20182_());
            ((BossSpawnerContextAssignable)entity).setSpawnPosition(this.m_20182_());
            this.m_9236_().m_7967_(entity);
            this.setActive(false);
        }
    }

    public boolean m_6087_() {
        return true;
    }

    public void setActive(boolean state) {
        this.f_19804_.m_135381_(ACTIVE, (Object)state);
    }

    public boolean isActive() {
        return (Boolean)this.f_19804_.m_135370_(ACTIVE);
    }

    public abstract EntityType<? extends BossSpawnerContextAssignable> getBossEntityType();

    public abstract Vec3 getPlayerItemsDropPosition(ServerPlayer var1, Vec3 var2);

    public boolean m_6469_(DamageSource src, float damage) {
        return src.m_276093_(DamageTypes.f_286979_) || src.m_276093_(DamageTypes.f_268724_);
    }

    protected void m_8097_() {
        this.f_19804_.m_135372_(ACTIVE, (Object)true);
    }

    protected void m_7378_(CompoundTag tag) {
        if (tag.m_128441_("active")) {
            this.setActive(tag.m_128471_("active"));
        } else {
            this.setActive(true);
        }
    }

    protected void m_7380_(CompoundTag tag) {
        tag.m_128379_("active", this.isActive());
    }

    public abstract boolean canInteractWithBlockPos(BlockPos var1);

    public abstract Component onArenaDestructionMessage();
}


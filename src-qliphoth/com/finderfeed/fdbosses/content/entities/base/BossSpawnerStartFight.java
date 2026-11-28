/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.network.FDPacket
 *  com.finderfeed.fdlib.network.RegisterFDPacket
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.finderfeed.fdbosses.content.entities.base;

import com.finderfeed.fdbosses.content.entities.base.BossSpawnerEntity;
import com.finderfeed.fdlib.network.FDPacket;
import com.finderfeed.fdlib.network.RegisterFDPacket;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

@RegisterFDPacket(value="fdbosses:start_boss_fight")
public class BossSpawnerStartFight
extends FDPacket {
    private int spawnerEntityId;

    public BossSpawnerStartFight(int spawnerEntityId) {
        this.spawnerEntityId = spawnerEntityId;
    }

    public BossSpawnerStartFight(FriendlyByteBuf buf) {
        this.spawnerEntityId = buf.readInt();
    }

    public void write(FriendlyByteBuf FriendlyByteBuf2) {
        FriendlyByteBuf2.writeInt(this.spawnerEntityId);
    }

    public void clientAction(Supplier<NetworkEvent.Context> iPayloadContext) {
    }

    public void serverAction(Supplier<NetworkEvent.Context> iPayloadContext) {
        ServerPlayer player = iPayloadContext.get().getSender();
        Entity entity = player.m_9236_().m_6815_(this.spawnerEntityId);
        if (entity instanceof BossSpawnerEntity) {
            BossSpawnerEntity bossSpawnerEntity = (BossSpawnerEntity)entity;
            bossSpawnerEntity.spawn();
        }
    }
}


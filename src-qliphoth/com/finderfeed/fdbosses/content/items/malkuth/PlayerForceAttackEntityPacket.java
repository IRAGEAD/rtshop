/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.network.FDPacket
 *  com.finderfeed.fdlib.network.RegisterFDPacket
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.finderfeed.fdbosses.content.items.malkuth;

import com.finderfeed.fdbosses.BossClientPackets;
import com.finderfeed.fdlib.network.FDPacket;
import com.finderfeed.fdlib.network.RegisterFDPacket;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

@RegisterFDPacket(value="fdbosses:player_force_attack_entity_packet")
public class PlayerForceAttackEntityPacket
extends FDPacket {
    private int entityId;

    public PlayerForceAttackEntityPacket(LivingEntity entity) {
        this.entityId = entity.m_19879_();
    }

    public PlayerForceAttackEntityPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
    }

    public void write(FriendlyByteBuf registryFriendlyByteBuf) {
        registryFriendlyByteBuf.writeInt(this.entityId);
    }

    public void clientAction(Supplier<NetworkEvent.Context> supplier) {
        BossClientPackets.forceAttackEntity(this.entityId);
    }

    public void serverAction(Supplier<NetworkEvent.Context> supplier) {
    }
}


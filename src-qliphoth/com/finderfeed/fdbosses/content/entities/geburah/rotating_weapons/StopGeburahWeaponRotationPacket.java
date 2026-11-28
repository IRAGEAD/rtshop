/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.network.FDPacket
 *  com.finderfeed.fdlib.network.RegisterFDPacket
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.finderfeed.fdbosses.content.entities.geburah.rotating_weapons;

import com.finderfeed.fdbosses.BossClientPackets;
import com.finderfeed.fdbosses.content.entities.geburah.GeburahEntity;
import com.finderfeed.fdlib.network.FDPacket;
import com.finderfeed.fdlib.network.RegisterFDPacket;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

@RegisterFDPacket(value="fdbosses:stop_geburah_weapon_rotation")
public class StopGeburahWeaponRotationPacket
extends FDPacket {
    private int entityId;

    public StopGeburahWeaponRotationPacket(GeburahEntity geburah) {
        this.entityId = geburah.m_19879_();
    }

    public StopGeburahWeaponRotationPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
    }

    public void write(FriendlyByteBuf registryFriendlyByteBuf) {
        registryFriendlyByteBuf.writeInt(this.entityId);
    }

    public void clientAction(Supplier<NetworkEvent.Context> supplier) {
        BossClientPackets.stopWeaponRotation(this.entityId);
    }

    public void serverAction(Supplier<NetworkEvent.Context> supplier) {
    }
}


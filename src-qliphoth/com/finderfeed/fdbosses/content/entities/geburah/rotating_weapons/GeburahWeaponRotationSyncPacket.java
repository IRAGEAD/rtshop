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
import com.finderfeed.fdbosses.content.entities.geburah.rotating_weapons.GeburahWeaponRotationController;
import com.finderfeed.fdlib.network.FDPacket;
import com.finderfeed.fdlib.network.RegisterFDPacket;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

@RegisterFDPacket(value="fdbosses:weapon_rotation_sync")
public class GeburahWeaponRotationSyncPacket
extends FDPacket {
    private int entityId;
    private float currentRotation;

    public GeburahWeaponRotationSyncPacket(GeburahEntity geburah) {
        float currentRotation;
        GeburahWeaponRotationController rotatingWeaponsHandler = geburah.getWeaponRotationController();
        this.currentRotation = currentRotation = rotatingWeaponsHandler.getCurrentRotation();
        this.entityId = geburah.m_19879_();
    }

    public GeburahWeaponRotationSyncPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.currentRotation = buf.readFloat();
    }

    public void write(FriendlyByteBuf registryFriendlyByteBuf) {
        registryFriendlyByteBuf.writeInt(this.entityId);
        registryFriendlyByteBuf.writeFloat(this.currentRotation);
    }

    public void clientAction(Supplier<NetworkEvent.Context> supplier) {
        BossClientPackets.handleGeburahWeaponRotationSync(this.entityId, this.currentRotation);
    }

    public void serverAction(Supplier<NetworkEvent.Context> supplier) {
    }
}


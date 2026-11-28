/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.network.FDPacket
 *  com.finderfeed.fdlib.network.RegisterFDPacket
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.finderfeed.fdbosses.content.entities.geburah.scales_controller;

import com.finderfeed.fdbosses.BossClientPackets;
import com.finderfeed.fdbosses.content.entities.geburah.GeburahEntity;
import com.finderfeed.fdlib.network.FDPacket;
import com.finderfeed.fdlib.network.RegisterFDPacket;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

@RegisterFDPacket(value="fdbosses:geburah_scales_controller_set_displacement")
public class GeburahScalesControllerSetDisplacement
extends FDPacket {
    private int entityId;
    private int displacement;
    private int displacementTime;

    public GeburahScalesControllerSetDisplacement(GeburahEntity geburah, int displacement, int displacementTime) {
        this.entityId = geburah.m_19879_();
        this.displacement = displacement;
        this.displacementTime = displacementTime;
    }

    public GeburahScalesControllerSetDisplacement(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.displacement = buf.readInt();
        this.displacementTime = buf.readInt();
    }

    public void write(FriendlyByteBuf registryFriendlyByteBuf) {
        registryFriendlyByteBuf.writeInt(this.entityId);
        registryFriendlyByteBuf.writeInt(this.displacement);
        registryFriendlyByteBuf.writeInt(this.displacementTime);
    }

    public void clientAction(Supplier<NetworkEvent.Context> supplier) {
        BossClientPackets.geburahScalesControllerSetDisplacement(this.entityId, this.displacement, this.displacementTime);
    }

    public void serverAction(Supplier<NetworkEvent.Context> supplier) {
    }
}


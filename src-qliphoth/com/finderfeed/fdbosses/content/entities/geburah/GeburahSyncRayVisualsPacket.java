/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.network.FDPacket
 *  com.finderfeed.fdlib.network.RegisterFDPacket
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.finderfeed.fdbosses.content.entities.geburah;

import com.finderfeed.fdbosses.BossClientPackets;
import com.finderfeed.fdbosses.content.entities.geburah.GeburahEntity;
import com.finderfeed.fdlib.network.FDPacket;
import com.finderfeed.fdlib.network.RegisterFDPacket;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

@RegisterFDPacket(value="fdbosses:geburah_sync_ray_visuals")
public class GeburahSyncRayVisualsPacket
extends FDPacket {
    private int geburah;
    private boolean visualsActive;

    public GeburahSyncRayVisualsPacket(GeburahEntity geburah, boolean visualsActive) {
        this.geburah = geburah.m_19879_();
        this.visualsActive = visualsActive;
    }

    public GeburahSyncRayVisualsPacket(FriendlyByteBuf buf) {
        this.visualsActive = buf.readBoolean();
        this.geburah = buf.readInt();
    }

    public void write(FriendlyByteBuf registryFriendlyByteBuf) {
        registryFriendlyByteBuf.writeBoolean(this.visualsActive);
        registryFriendlyByteBuf.writeInt(this.geburah);
    }

    public void clientAction(Supplier<NetworkEvent.Context> supplier) {
        BossClientPackets.geburahSyncRayVisuals(this.geburah, this.visualsActive);
    }

    public void serverAction(Supplier<NetworkEvent.Context> supplier) {
    }
}


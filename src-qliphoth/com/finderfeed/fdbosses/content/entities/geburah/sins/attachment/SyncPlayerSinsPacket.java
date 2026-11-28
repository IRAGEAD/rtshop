/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.network.FDPacket
 *  com.finderfeed.fdlib.network.RegisterFDPacket
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.finderfeed.fdbosses.content.entities.geburah.sins.attachment;

import com.finderfeed.fdbosses.BossClientPackets;
import com.finderfeed.fdbosses.content.entities.geburah.sins.attachment.PlayerSins;
import com.finderfeed.fdlib.network.FDPacket;
import com.finderfeed.fdlib.network.RegisterFDPacket;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

@RegisterFDPacket(value="fdbosses:sync_player_sins_packet")
public class SyncPlayerSinsPacket
extends FDPacket {
    private PlayerSins playerSins;

    public SyncPlayerSinsPacket(PlayerSins playerSins) {
        this.playerSins = new PlayerSins(playerSins);
    }

    public SyncPlayerSinsPacket(FriendlyByteBuf friendlyByteBuf) {
        this.playerSins = (PlayerSins)PlayerSins.STREAM_CODEC.fromNetwork(friendlyByteBuf);
    }

    public void write(FriendlyByteBuf registryFriendlyByteBuf) {
        PlayerSins.STREAM_CODEC.toNetwork(registryFriendlyByteBuf, (Object)this.playerSins);
    }

    public void clientAction(Supplier<NetworkEvent.Context> supplier) {
        BossClientPackets.syncPlayerSinsPacket(this.playerSins);
    }

    public void serverAction(Supplier<NetworkEvent.Context> supplier) {
    }
}


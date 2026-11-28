/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.network.FDPacket
 *  com.finderfeed.fdlib.network.RegisterFDPacket
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.finderfeed.fdbosses.content.entities.base;

import com.finderfeed.fdbosses.BossClientPackets;
import com.finderfeed.fdlib.network.FDPacket;
import com.finderfeed.fdlib.network.RegisterFDPacket;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

@RegisterFDPacket(value="fdbosses:force_dossier_screen_close_packet")
public class ForceDossierClosePacket
extends FDPacket {
    public ForceDossierClosePacket() {
    }

    public ForceDossierClosePacket(FriendlyByteBuf buf) {
    }

    public void write(FriendlyByteBuf FriendlyByteBuf2) {
    }

    public void clientAction(Supplier<NetworkEvent.Context> iPayloadContext) {
        BossClientPackets.closeDossierScreen();
    }

    public void serverAction(Supplier<NetworkEvent.Context> iPayloadContext) {
    }
}


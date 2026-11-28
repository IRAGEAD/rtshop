/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.network.FDPacket
 *  com.finderfeed.fdlib.network.RegisterFDPacket
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.finderfeed.fdbosses.packets;

import com.finderfeed.fdbosses.BossClientPackets;
import com.finderfeed.fdlib.network.FDPacket;
import com.finderfeed.fdlib.network.RegisterFDPacket;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

@RegisterFDPacket(value="fdbosses:chesed_ray_reflect")
public class ChesedRayReflectPacket
extends FDPacket {
    public ChesedRayReflectPacket() {
    }

    public ChesedRayReflectPacket(FriendlyByteBuf buf) {
    }

    public void write(FriendlyByteBuf friendlyByteBuf) {
    }

    public void clientAction(Supplier<NetworkEvent.Context> iPayloadContext) {
        BossClientPackets.chesedRayReflectParticles();
    }

    public void serverAction(Supplier<NetworkEvent.Context> iPayloadContext) {
    }
}


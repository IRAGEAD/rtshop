/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.network.FDPacket
 *  com.finderfeed.fdlib.network.RegisterFDPacket
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.finderfeed.fdbosses.content.entities.malkuth_boss.packets;

import com.finderfeed.fdbosses.BossClientPackets;
import com.finderfeed.fdlib.network.FDPacket;
import com.finderfeed.fdlib.network.RegisterFDPacket;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

@RegisterFDPacket(value="fdbosses:set_malkuth_weakness_amount")
public class SetClientMalkuthWeaknessAmountPacket
extends FDPacket {
    private int amount;

    public SetClientMalkuthWeaknessAmountPacket(int amount) {
        this.amount = amount;
    }

    public SetClientMalkuthWeaknessAmountPacket(FriendlyByteBuf buf) {
        this.amount = buf.readInt();
    }

    public void write(FriendlyByteBuf FriendlyByteBuf2) {
        FriendlyByteBuf2.writeInt(this.amount);
    }

    public void clientAction(Supplier<NetworkEvent.Context> iPayloadContext) {
        BossClientPackets.setPlayerMalkuthWeaknessAmount(this.amount);
    }

    public void serverAction(Supplier<NetworkEvent.Context> iPayloadContext) {
    }
}


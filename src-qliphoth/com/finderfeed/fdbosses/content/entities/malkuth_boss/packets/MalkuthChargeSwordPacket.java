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
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthAttackType;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthEntity;
import com.finderfeed.fdlib.network.FDPacket;
import com.finderfeed.fdlib.network.RegisterFDPacket;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

@RegisterFDPacket(value="fdbosses:malkuth_charge_sword")
public class MalkuthChargeSwordPacket
extends FDPacket {
    private int entityId;
    private MalkuthAttackType malkuthAttackType;

    public MalkuthChargeSwordPacket(MalkuthEntity malkuthEntity, MalkuthAttackType malkuthAttackType) {
        this.entityId = malkuthEntity.m_19879_();
        this.malkuthAttackType = malkuthAttackType;
    }

    public MalkuthChargeSwordPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.malkuthAttackType = (MalkuthAttackType)buf.m_130066_(MalkuthAttackType.class);
    }

    public void write(FriendlyByteBuf FriendlyByteBuf2) {
        FriendlyByteBuf2.writeInt(this.entityId);
        FriendlyByteBuf2.m_130068_((Enum)this.malkuthAttackType);
    }

    public void clientAction(Supplier<NetworkEvent.Context> iPayloadContext) {
        BossClientPackets.malkuthSwordCharge(this.malkuthAttackType, this.entityId);
    }

    public void serverAction(Supplier<NetworkEvent.Context> iPayloadContext) {
    }
}


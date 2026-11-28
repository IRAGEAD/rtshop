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

@RegisterFDPacket(value="fdbosses:geburah_prepare_laser_attack")
public class GeburahPrepareAttackPacket
extends FDPacket {
    public int geburah;
    public int timeUntilAttack;

    public GeburahPrepareAttackPacket(GeburahEntity geburah, int timeUntilAttack) {
        this.geburah = geburah.m_19879_();
        this.timeUntilAttack = timeUntilAttack;
    }

    public GeburahPrepareAttackPacket(FriendlyByteBuf buf) {
        this.geburah = buf.readInt();
        this.timeUntilAttack = buf.readInt();
    }

    public void write(FriendlyByteBuf registryFriendlyByteBuf) {
        registryFriendlyByteBuf.writeInt(this.geburah);
        registryFriendlyByteBuf.writeInt(this.timeUntilAttack);
    }

    public void clientAction(Supplier<NetworkEvent.Context> supplier) {
        BossClientPackets.launchGeburahLaserPrepare(this.geburah, this.timeUntilAttack);
    }

    public void serverAction(Supplier<NetworkEvent.Context> supplier) {
    }
}


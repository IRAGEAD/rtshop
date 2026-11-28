/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.network.FDPacket
 *  com.finderfeed.fdlib.network.RegisterFDPacket
 *  com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.finderfeed.fdbosses.packets;

import com.finderfeed.fdbosses.BossClientPackets;
import com.finderfeed.fdlib.network.FDPacket;
import com.finderfeed.fdlib.network.RegisterFDPacket;
import com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

@RegisterFDPacket(value="fdbosses:pos_level_event")
public class PosLevelEventPacket
extends FDPacket {
    private Vec3 pos;
    private int event;
    private int data;

    public PosLevelEventPacket(Vec3 pos, int event, int data) {
        this.pos = pos;
        this.event = event;
        this.data = data;
    }

    public PosLevelEventPacket(FriendlyByteBuf buf) {
        this.pos = (Vec3)NetworkCodec.VEC3.fromNetwork(buf);
        this.event = buf.readInt();
        this.data = buf.readInt();
    }

    public void write(FriendlyByteBuf buf) {
        NetworkCodec.VEC3.toNetwork(buf, (Object)this.pos);
        buf.writeInt(this.event);
        buf.writeInt(this.data);
    }

    public void clientAction(Supplier<NetworkEvent.Context> context) {
        BossClientPackets.posEvent(this.pos, this.event, this.data);
    }

    public void serverAction(Supplier<NetworkEvent.Context> context) {
    }
}


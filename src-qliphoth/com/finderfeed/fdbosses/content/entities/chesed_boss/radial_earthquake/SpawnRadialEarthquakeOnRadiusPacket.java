/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.network.FDPacket
 *  com.finderfeed.fdlib.network.RegisterFDPacket
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.finderfeed.fdbosses.content.entities.chesed_boss.radial_earthquake;

import com.finderfeed.fdbosses.BossClientPackets;
import com.finderfeed.fdbosses.content.entities.chesed_boss.radial_earthquake.RadialEarthquakeEntity;
import com.finderfeed.fdlib.network.FDPacket;
import com.finderfeed.fdlib.network.RegisterFDPacket;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

@RegisterFDPacket(value="fdbosses:spawn_radial_earthquake_on_radius")
public class SpawnRadialEarthquakeOnRadiusPacket
extends FDPacket {
    private int entityId;
    private int radius;

    public SpawnRadialEarthquakeOnRadiusPacket(RadialEarthquakeEntity entity, int radius) {
        this.entityId = entity.m_19879_();
        this.radius = radius;
    }

    public SpawnRadialEarthquakeOnRadiusPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.radius = buf.readInt();
    }

    public void write(FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeInt(this.entityId);
        friendlyByteBuf.writeInt(this.radius);
    }

    public void clientAction(Supplier<NetworkEvent.Context> supplier) {
        BossClientPackets.spawnRadialEarthquakeShatters(this.entityId, this.radius);
    }

    public void serverAction(Supplier<NetworkEvent.Context> supplier) {
    }
}


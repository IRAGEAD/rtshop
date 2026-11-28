/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.network.FDPacket
 *  com.finderfeed.fdlib.network.RegisterFDPacket
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.finderfeed.fdbosses.content.entities.geburah.geburah_earthquake;

import com.finderfeed.fdbosses.BossClientPackets;
import com.finderfeed.fdbosses.content.entities.geburah.geburah_earthquake.GeburahEarthquake;
import com.finderfeed.fdlib.network.FDPacket;
import com.finderfeed.fdlib.network.RegisterFDPacket;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

@RegisterFDPacket(value="fdbosses:spawn_radial_earthquake_on_radius")
public class SpawnRadialEarthquakeOnRadiusPacket
extends FDPacket {
    private int entityId;
    private int radius;
    private Vec3 direction;
    private float angle;

    public SpawnRadialEarthquakeOnRadiusPacket(GeburahEarthquake entity, int radius, Vec3 direction, float angle) {
        this.entityId = entity.m_19879_();
        this.radius = radius;
        this.direction = direction;
        this.angle = angle;
    }

    public SpawnRadialEarthquakeOnRadiusPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.radius = buf.readInt();
        this.direction = new Vec3(buf.readDouble(), 0.0, buf.readDouble());
        this.angle = buf.readFloat();
    }

    public void write(FriendlyByteBuf registryFriendlyByteBuf) {
        registryFriendlyByteBuf.writeInt(this.entityId);
        registryFriendlyByteBuf.writeInt(this.radius);
        registryFriendlyByteBuf.writeDouble(this.direction.f_82479_);
        registryFriendlyByteBuf.writeDouble(this.direction.f_82481_);
        registryFriendlyByteBuf.writeFloat(this.angle);
    }

    public void clientAction(Supplier<NetworkEvent.Context> supplier) {
        BossClientPackets.geburahEarthquakeSpawnEarthshatters(this.entityId, this.direction, this.radius, this.angle);
    }

    public void serverAction(Supplier<NetworkEvent.Context> supplier) {
    }
}


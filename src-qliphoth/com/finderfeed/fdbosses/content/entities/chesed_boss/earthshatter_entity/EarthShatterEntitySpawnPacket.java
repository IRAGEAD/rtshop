/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.network.FDPacket
 *  com.finderfeed.fdlib.network.RegisterFDPacket
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.finderfeed.fdbosses.content.entities.chesed_boss.earthshatter_entity;

import com.finderfeed.fdbosses.BossClientPackets;
import com.finderfeed.fdbosses.content.entities.chesed_boss.earthshatter_entity.EarthShatterEntity;
import com.finderfeed.fdbosses.content.entities.chesed_boss.earthshatter_entity.EarthShatterSettings;
import com.finderfeed.fdlib.network.FDPacket;
import com.finderfeed.fdlib.network.RegisterFDPacket;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

@RegisterFDPacket(value="fdbosses:earth_shatter_entity_spawn")
public class EarthShatterEntitySpawnPacket
extends FDPacket {
    public int entityId;
    public EarthShatterSettings settings;

    public EarthShatterEntitySpawnPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.settings = (EarthShatterSettings)EarthShatterSettings.NETWORK_CODEC.fromNetwork(buf);
    }

    public EarthShatterEntitySpawnPacket(EarthShatterEntity entity, EarthShatterSettings settings) {
        this.entityId = entity.m_19879_();
        this.settings = settings;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(this.entityId);
        EarthShatterSettings.NETWORK_CODEC.toNetwork(buf, (Object)this.settings);
    }

    public void clientAction(Supplier<NetworkEvent.Context> context) {
        BossClientPackets.handleEarthShatterSpawnPacket(this.entityId, this.settings);
    }

    public void serverAction(Supplier<NetworkEvent.Context> context) {
    }
}


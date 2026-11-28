/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.network.FDPacket
 *  com.finderfeed.fdlib.network.RegisterFDPacket
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.finderfeed.fdbosses.content.entities.geburah.sins;

import com.finderfeed.fdbosses.BossClientPackets;
import com.finderfeed.fdlib.network.FDPacket;
import com.finderfeed.fdlib.network.RegisterFDPacket;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

@RegisterFDPacket(value="fdbosses:")
public class GeburahTriggerSinEffectPacket
extends FDPacket {
    private float soundPitch;

    public GeburahTriggerSinEffectPacket(float soundPitch) {
        this.soundPitch = soundPitch;
    }

    public GeburahTriggerSinEffectPacket(FriendlyByteBuf buf) {
        this.soundPitch = buf.readFloat();
    }

    public void write(FriendlyByteBuf registryFriendlyByteBuf) {
        registryFriendlyByteBuf.writeFloat(this.soundPitch);
    }

    public void clientAction(Supplier<NetworkEvent.Context> supplier) {
        BossClientPackets.triggerSinEffect(this.soundPitch);
    }

    public void serverAction(Supplier<NetworkEvent.Context> supplier) {
    }
}


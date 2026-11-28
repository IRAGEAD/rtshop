/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.network.syncher.EntityDataSerializer
 */
package com.finderfeed.fdbosses.content.entities.geburah;

import com.finderfeed.fdbosses.content.entities.geburah.sins.attachment.PlayerSin;
import com.finderfeed.fdbosses.init.BossRegistries;
import com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataSerializer;

public class SinsEntityDataSerializer
implements EntityDataSerializer<List<PlayerSin>> {
    public static final NetworkCodec<List<PlayerSin>> STREAM_CODEC = NetworkCodec.listOf((NetworkCodec)NetworkCodec.registry(() -> BossRegistries.PLAYER_SINS.get()));

    public void write(FriendlyByteBuf buf, List<PlayerSin> players) {
        STREAM_CODEC.toNetwork(buf, players);
    }

    public List<PlayerSin> read(FriendlyByteBuf buf) {
        return (List)STREAM_CODEC.fromNetwork(buf);
    }

    public List<PlayerSin> copy(List<PlayerSin> sins) {
        return new ArrayList<PlayerSin>(sins);
    }
}


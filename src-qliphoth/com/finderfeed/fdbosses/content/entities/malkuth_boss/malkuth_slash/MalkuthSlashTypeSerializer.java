/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.network.syncher.EntityDataSerializer
 */
package com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_slash;

import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthAttackType;
import com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataSerializer;

public class MalkuthSlashTypeSerializer
implements EntityDataSerializer<MalkuthAttackType> {
    public static final NetworkCodec<MalkuthAttackType> CODEC = NetworkCodec.composite((NetworkCodec)NetworkCodec.STRING, Enum::name, MalkuthAttackType::valueOf);

    public void write(FriendlyByteBuf p_135025_, MalkuthAttackType p_135026_) {
        CODEC.toNetwork(p_135025_, (Object)p_135026_);
    }

    public MalkuthAttackType read(FriendlyByteBuf p_135024_) {
        return (MalkuthAttackType)((Object)CODEC.fromNetwork(p_135024_));
    }

    public MalkuthAttackType copy(MalkuthAttackType malkuthAttackType) {
        return malkuthAttackType;
    }
}


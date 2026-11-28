/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.network.syncher.EntityDataSerializer
 */
package com.finderfeed.fdbosses.content.entities.chesed_boss.earthshatter_entity;

import com.finderfeed.fdbosses.content.entities.chesed_boss.earthshatter_entity.EarthShatterSettings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataSerializer;

public class EarthShatterEntityDataSerializer
implements EntityDataSerializer<EarthShatterSettings> {
    public void write(FriendlyByteBuf p_135025_, EarthShatterSettings p_135026_) {
        EarthShatterSettings.NETWORK_CODEC.toNetwork(p_135025_, (Object)p_135026_);
    }

    public EarthShatterSettings read(FriendlyByteBuf p_135024_) {
        return (EarthShatterSettings)EarthShatterSettings.NETWORK_CODEC.fromNetwork(p_135024_);
    }

    public EarthShatterSettings copy(EarthShatterSettings p_135023_) {
        return new EarthShatterSettings(p_135023_);
    }
}


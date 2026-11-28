/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.RandomSource
 */
package com.finderfeed.fdbosses.content.entities.malkuth_boss;

import net.minecraft.util.RandomSource;

public enum MalkuthAttackType {
    ICE,
    FIRE;


    public static MalkuthAttackType getOpposite(MalkuthAttackType original) {
        if (original.isIce()) {
            return FIRE;
        }
        return ICE;
    }

    public boolean isFire() {
        return this == FIRE;
    }

    public boolean isIce() {
        return this == ICE;
    }

    public static MalkuthAttackType getRandom(RandomSource randomSource) {
        return MalkuthAttackType.values()[randomSource.m_188503_(MalkuthAttackType.values().length)];
    }
}


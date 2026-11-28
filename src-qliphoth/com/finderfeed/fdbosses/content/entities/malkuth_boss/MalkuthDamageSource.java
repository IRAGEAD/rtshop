/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.damagesource.DamageSource
 */
package com.finderfeed.fdbosses.content.entities.malkuth_boss;

import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthAttackType;
import net.minecraft.world.damagesource.DamageSource;

public class MalkuthDamageSource
extends DamageSource {
    private MalkuthAttackType malkuthAttackType;
    private int malkuthAttackAmount;

    public MalkuthDamageSource(DamageSource damageSource, MalkuthAttackType attackType, int malkuthAttackAmount) {
        super(damageSource.m_269150_(), damageSource.m_7640_(), damageSource.m_7639_(), damageSource.m_269181_());
        this.malkuthAttackType = attackType;
        this.malkuthAttackAmount = malkuthAttackAmount;
    }

    public int getMalkuthAttackAmount() {
        return this.malkuthAttackAmount;
    }

    public MalkuthAttackType getMalkuthAttackType() {
        return this.malkuthAttackType;
    }
}


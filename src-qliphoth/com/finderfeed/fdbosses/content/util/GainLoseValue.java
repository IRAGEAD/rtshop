/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  net.minecraft.util.Mth
 */
package com.finderfeed.fdbosses.content.util;

import com.finderfeed.fdlib.util.math.FDMathUtil;
import net.minecraft.util.Mth;

public class GainLoseValue {
    private int maxValue;
    private int currentValue;
    private int oldValue;

    public GainLoseValue(int initValue, int maxValue) {
        this.maxValue = maxValue;
        this.oldValue = initValue;
        this.currentValue = initValue;
    }

    public void forward() {
        this.oldValue = this.currentValue;
        this.currentValue = Mth.m_14045_((int)(this.currentValue + 1), (int)0, (int)this.maxValue);
    }

    public void backward() {
        this.oldValue = this.currentValue;
        this.currentValue = Mth.m_14045_((int)(this.currentValue - 1), (int)0, (int)this.maxValue);
    }

    public void moveValue(boolean forward) {
        if (forward) {
            this.forward();
        } else {
            this.backward();
        }
    }

    public int getCurrentValue() {
        return this.currentValue;
    }

    public float getPercent(float pticks) {
        return FDMathUtil.lerp((float)this.oldValue, (float)this.currentValue, (float)pticks) / (float)this.maxValue;
    }

    public void reset() {
        this.currentValue = 0;
    }
}


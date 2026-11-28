/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.chat.Component
 *  net.minecraft.util.Mth
 */
package com.finderfeed.fdbosses.client.boss_screen.util;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class TimedText {
    public Component text;
    public int currentTime;

    public void setText(Component component, int time) {
        this.currentTime = time;
        this.text = component;
    }

    public void tick() {
        if (this.currentTime == 0) {
            this.text = null;
        }
        this.currentTime = Mth.m_14045_((int)(this.currentTime - 1), (int)0, (int)Integer.MAX_VALUE);
    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.util;

import com.finderfeed.fdbosses.content.util.ServerSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class DelayedSound {
    public ServerSoundInstance sound;
    public Vec3 pos;
    private int remainingTicks = 0;

    public DelayedSound(SoundEvent soundEvent, SoundSource source, Vec3 pos, float volume, float pitch, int delay) {
        this.sound = new ServerSoundInstance(soundEvent, source, volume, pitch);
        this.pos = pos;
        this.remainingTicks = delay;
    }

    public boolean tick() {
        this.remainingTicks = Mth.m_14045_((int)(this.remainingTicks - 1), (int)0, (int)Integer.MAX_VALUE);
        return this.remainingTicks == 0;
    }

    public boolean isFinished() {
        return this.remainingTicks == 0;
    }

    public void setRemainingTicks(int remainingTicks) {
        this.remainingTicks = remainingTicks;
    }
}


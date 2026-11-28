/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.util;

import com.finderfeed.fdbosses.content.util.ServerSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class RepeatedSound {
    public int ticker = 0;
    public int timesToRepeat = 0;
    public int repeatFrequency;
    public ServerSoundInstance sound;
    public Vec3 pos;

    public RepeatedSound(SoundEvent soundEvent, SoundSource source, Vec3 pos, float volume, float pitch, int repeatFrequency, int timesToRepeat) {
        this.sound = new ServerSoundInstance(soundEvent, source, volume, pitch);
        this.pos = pos;
        this.repeatFrequency = repeatFrequency;
        this.timesToRepeat = timesToRepeat;
    }

    public boolean tick(Level level) {
        if (this.isFinished()) {
            return true;
        }
        if (this.ticker++ % this.repeatFrequency == 0) {
            level.m_6263_(null, this.pos.f_82479_, this.pos.f_82480_, this.pos.f_82481_, this.sound.soundEvent, this.sound.soundSource, this.sound.volume, this.sound.pitch);
        }
        return false;
    }

    public boolean isFinished() {
        return this.ticker / this.repeatFrequency + 1 >= this.timesToRepeat;
    }
}


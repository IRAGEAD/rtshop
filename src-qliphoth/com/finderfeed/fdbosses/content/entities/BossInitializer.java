/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.nbt.AutoSerializable
 *  com.finderfeed.fdlib.nbt.SerializableField
 *  net.minecraft.world.entity.Entity
 */
package com.finderfeed.fdbosses.content.entities;

import com.finderfeed.fdlib.nbt.AutoSerializable;
import com.finderfeed.fdlib.nbt.SerializableField;
import net.minecraft.world.entity.Entity;

public abstract class BossInitializer<T extends Entity>
implements AutoSerializable {
    @SerializableField
    private int tick;
    @SerializableField
    private boolean finished;
    private T boss;

    public BossInitializer(T boss) {
        this.boss = boss;
    }

    public void tick() {
        if (!this.isFinished()) {
            if (this.tick == 0) {
                this.onStart();
            }
            this.onTick();
            ++this.tick;
        }
    }

    public abstract void onStart();

    public abstract void onFinish();

    public abstract void onTick();

    public void setFinished() {
        if (!this.isFinished()) {
            this.finished = true;
            this.onFinish();
        }
    }

    public T getBoss() {
        return this.boss;
    }

    public int getTick() {
        return this.tick;
    }

    public boolean isFinished() {
        return this.finished;
    }
}


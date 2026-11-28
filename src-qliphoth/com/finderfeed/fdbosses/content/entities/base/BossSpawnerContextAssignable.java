/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.entities.base;

import com.finderfeed.fdbosses.content.entities.base.BossSpawnerEntity;
import net.minecraft.world.phys.Vec3;

public interface BossSpawnerContextAssignable {
    public void setSpawnedBy(BossSpawnerEntity var1);

    public void setSpawnPosition(Vec3 var1);

    public BossSpawnerEntity getSpawner();
}


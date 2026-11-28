/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.entity.MobSpawnType
 */
package com.pla.spiral_abyss.event;

import com.pla.spiral_abyss.platform.event.AbyssEvents;
import com.pla.spiral_abyss.world.AbyssDimension;
import net.minecraft.world.entity.MobSpawnType;

public final class AbyssNaturalSpawns {
    private AbyssNaturalSpawns() {
    }

    private static boolean ambient(MobSpawnType type) {
        return type == MobSpawnType.NATURAL || type == MobSpawnType.CHUNK_GENERATION || type == MobSpawnType.PATROL;
    }

    public static void placement(AbyssEvents.MobSpawnEvent.SpawnPlacementCheck event) {
        if (event.getLevel().m_6018_().m_46472_().equals(AbyssDimension.LEVEL) && AbyssNaturalSpawns.ambient(event.getSpawnType())) {
            event.setResult(AbyssEvents.Event.Result.DENY);
        }
    }

    public static void finalizeSpawn(AbyssEvents.MobSpawnEvent.FinalizeSpawn event) {
        if (event.getLevel().m_6018_().m_46472_().equals(AbyssDimension.LEVEL) && AbyssNaturalSpawns.ambient(event.getSpawnType())) {
            event.setSpawnCancelled(true);
        }
    }
}


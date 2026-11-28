/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.level.Level
 */
package com.pla.spiral_abyss.entity;

import com.pla.spiral_abyss.entity.LootChestEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

public final class GoldenChestEntity
extends LootChestEntity {
    public GoldenChestEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level, 3);
    }
}


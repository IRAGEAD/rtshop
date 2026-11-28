/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.block.state.BlockState
 */
package com.finderfeed.fdbosses.content.tile_entities;

import com.finderfeed.fdbosses.content.tile_entities.TrophyBlockEntity;
import com.finderfeed.fdbosses.init.BossTileEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class MalkuthTrophyBlockEntity
extends TrophyBlockEntity {
    public MalkuthTrophyBlockEntity(BlockPos pos, BlockState state) {
        super(BossTileEntities.MALKUTH_TROPHY.get(), pos, state);
    }

    @Override
    public void tick() {
        super.tick();
    }
}


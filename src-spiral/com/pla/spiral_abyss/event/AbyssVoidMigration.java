/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.BlockPos$MutableBlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.chunk.ChunkAccess
 */
package com.pla.spiral_abyss.event;

import com.pla.spiral_abyss.platform.event.AbyssEvents;
import com.pla.spiral_abyss.world.AbyssDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;

public final class AbyssVoidMigration {
    private AbyssVoidMigration() {
    }

    public static void load(AbyssEvents.ChunkEvent.Load event) {
        ServerLevel level;
        LevelAccessor levelAccessor = event.getLevel();
        if (!(levelAccessor instanceof ServerLevel) || !(level = (ServerLevel)levelAccessor).m_46472_().equals(AbyssDimension.LEVEL)) {
            return;
        }
        ChunkAccess chunk = event.getChunk();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                pos.m_122178_(chunk.m_7697_().m_45604_() + x, 0, chunk.m_7697_().m_45605_() + z);
                if (!chunk.m_8055_((BlockPos)pos).m_60713_(Blocks.f_50752_)) continue;
                chunk.m_6978_((BlockPos)pos, Blocks.f_50016_.m_49966_(), false);
                chunk.m_8092_(true);
            }
        }
    }
}


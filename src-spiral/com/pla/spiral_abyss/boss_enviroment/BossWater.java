/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 */
package com.pla.spiral_abyss.boss_enviroment;

import com.pla.spiral_abyss.event.BossDeathBlocks;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

final class BossWater {
    private static final int WIDTH = 65;
    private static final int LAYER = 4225;
    private final ServerLevel level;
    private final BlockPos origin;
    private final int size;
    private final BitSet placed = new BitSet();
    private final Map<Integer, BlockState> unusualAir = new HashMap<Integer, BlockState>();
    private int cursor;
    private int cleanupCursor;

    BossWater(ServerLevel level, BlockPos center, int depth) {
        this.level = level;
        this.origin = center.m_7918_(-32, 0, -32);
        this.size = 4225 * depth;
    }

    private BlockPos position(int index) {
        return this.origin.m_7918_(index % 65, index / 4225, index % 4225 / 65);
    }

    private int index(BlockPos pos) {
        int x = pos.m_123341_() - this.origin.m_123341_();
        int y = pos.m_123342_() - this.origin.m_123342_();
        int z = pos.m_123343_() - this.origin.m_123343_();
        return x < 0 || x >= 65 || z < 0 || z >= 65 || y < 0 || y >= this.size / 4225 ? -1 : y * 4225 + z * 65 + x;
    }

    boolean contains(BlockPos pos) {
        int index = this.index(pos);
        return index >= 0 && this.placed.get(index);
    }

    int count() {
        return this.placed.cardinality();
    }

    void grow(int tick, int duration) {
        int target = (int)Math.ceil((double)(this.size * Math.min(tick, duration)) / (double)duration);
        while (this.cursor < target) {
            int index;
            ++this.cursor;
            BlockPos pos = this.position(index);
            BlockState before = this.level.m_8055_(pos);
            if (!before.m_60795_() || this.level.m_7702_(pos) != null) continue;
            this.placed.set(index);
            if (!before.m_60713_(Blocks.f_50016_)) {
                this.unusualAir.put(index, before);
            }
            BossDeathBlocks.edit(() -> this.level.m_7731_(pos, Blocks.f_49990_.m_49966_(), 18));
        }
    }

    void retract(int tick, int duration) {
        int target = (int)Math.ceil((double)(this.cursor * Math.min(tick, duration)) / (double)duration);
        while (this.cleanupCursor < target) {
            this.restore(this.cursor - 1 - this.cleanupCursor++);
        }
    }

    void restore(BlockPos pos) {
        int index = this.index(pos);
        if (index >= 0) {
            this.restore(index);
        }
    }

    private void restore(int index) {
        if (this.placed.get(index)) {
            BossDeathBlocks.edit(() -> this.level.m_7731_(this.position(index), this.unusualAir.getOrDefault(index, Blocks.f_50016_.m_49966_()), 18));
        }
    }

    void clear() {
        int index = this.placed.nextSetBit(0);
        while (index >= 0) {
            this.restore(index);
            index = this.placed.nextSetBit(index + 1);
        }
        this.placed.clear();
        this.unusualAir.clear();
    }
}


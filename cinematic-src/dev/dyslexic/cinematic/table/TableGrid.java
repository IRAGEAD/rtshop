/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 */
package dev.dyslexic.cinematic.table;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class TableGrid {
    public static final int CELLS = 9;
    public static final double CELL = 0.1875;
    public static final double GRID_MIN = 0.21875;
    public static final double GRID_MAX = 0.78125;

    private TableGrid() {
    }

    public static boolean isTable(BlockState state) {
        return state.m_60713_(Blocks.f_50091_);
    }

    public static double cellX(int index) {
        return 0.21875 + 0.1875 * ((double)(index % 3) + 0.5);
    }

    public static double cellZ(int index) {
        return 0.21875 + 0.1875 * ((double)(index / 3) + 0.5);
    }

    public static int cellAt(double localX, double localZ) {
        if (localX < 0.21875 || localZ < 0.21875 || localX >= 0.78125 || localZ >= 0.78125) {
            return -1;
        }
        double gx = (localX - 0.21875) / 0.1875;
        double gz = (localZ - 0.21875) / 0.1875;
        return (int)gx + (int)gz * 3;
    }

    public static int worldIndex(Direction side, int row, int col) {
        Direction forward = side.m_122424_();
        Direction right = forward.m_122427_();
        int dx = forward.m_122429_() * (1 - row) + right.m_122429_() * (col - 1);
        int dz = forward.m_122431_() * (1 - row) + right.m_122431_() * (col - 1);
        return 1 + dx + (1 + dz) * 3;
    }

    public static List<ItemStack> oriented(List<ItemStack> world, Direction side) {
        ArrayList<ItemStack> grid = new ArrayList<ItemStack>(9);
        for (int i = 0; i < 9; ++i) {
            grid.add(world.get(TableGrid.worldIndex(side, i / 3, i % 3)));
        }
        return grid;
    }

    public static Direction sideFacing(BlockPos pos, double x, double z) {
        double dx = x - ((double)pos.m_123341_() + 0.5);
        double dz = z - ((double)pos.m_123343_() + 0.5);
        if (Math.abs(dx) > Math.abs(dz)) {
            return dx > 0.0 ? Direction.EAST : Direction.WEST;
        }
        return dz > 0.0 ? Direction.SOUTH : Direction.NORTH;
    }
}


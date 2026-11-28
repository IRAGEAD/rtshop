/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.world.phys.Vec3
 */
package dev.dyslexic.cinematic.furnace;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public final class FurnaceZones {
    public static final int INPUT = 0;
    public static final int FUEL = 1;
    public static final int RESULT = 2;

    private FurnaceZones() {
    }

    public static Direction right(Direction facing) {
        return facing.m_122424_().m_122427_();
    }

    public static Vec3 point(BlockPos pos, Direction facing, double u, double v, double out) {
        Direction right = FurnaceZones.right(facing);
        return new Vec3((double)pos.m_123341_() + 0.5 + (double)facing.m_122429_() * (0.5 + out) + (double)right.m_122429_() * (u - 0.5), (double)pos.m_123342_() + v, (double)pos.m_123343_() + 0.5 + (double)facing.m_122431_() * (0.5 + out) + (double)right.m_122431_() * (u - 0.5));
    }

    public static Vec3 top(BlockPos pos, Direction facing, double u, double v, double up) {
        Direction right = FurnaceZones.right(facing);
        return new Vec3((double)pos.m_123341_() + 0.5 + (double)facing.m_122429_() * (0.5 - v) + (double)right.m_122429_() * (u - 0.5), (double)pos.m_123342_() + 1.0 + up, (double)pos.m_123343_() + 0.5 + (double)facing.m_122431_() * (0.5 - v) + (double)right.m_122431_() * (u - 0.5));
    }

    public static Vec3 point(BlockPos pos, Direction facing, Face face, double u, double v, double out) {
        return face == Face.FRONT ? FurnaceZones.point(pos, facing, u, v, out) : FurnaceZones.top(pos, facing, u, v, out);
    }

    public static enum Face {
        FRONT,
        TOP;

    }
}


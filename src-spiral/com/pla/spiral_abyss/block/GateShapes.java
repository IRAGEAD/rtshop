/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.Direction
 *  net.minecraft.world.phys.shapes.Shapes
 *  net.minecraft.world.phys.shapes.VoxelShape
 */
package com.pla.spiral_abyss.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

final class GateShapes {
    static final VoxelShape VERTICAL = GateShapes.box(2.0, 0.0, -2.0, 14.0, 16.0, 10.0);
    static final VoxelShape CURVE_BOTTOM = Shapes.m_83124_((VoxelShape)GateShapes.box(2.0, 0.0, 6.0, 14.0, 10.0, 18.0), (VoxelShape[])new VoxelShape[]{GateShapes.box(2.0, 10.0, 10.0, 14.0, 14.0, 14.0), GateShapes.box(2.0, 6.0, 0.0, 14.0, 16.0, 6.0), GateShapes.box(2.0, 10.0, 6.0, 14.0, 16.0, 10.0)});
    static final VoxelShape CURVE_TOP = Shapes.m_83124_((VoxelShape)GateShapes.box(2.0, 2.0, 10.0, 14.0, 6.0, 14.0), (VoxelShape[])new VoxelShape[]{GateShapes.box(2.0, 0.0, 0.0, 14.0, 10.0, 6.0), GateShapes.box(2.0, 0.0, 6.0, 14.0, 6.0, 10.0), GateShapes.box(2.0, 6.0, 6.0, 14.0, 16.0, 18.0)});
    static final VoxelShape ACTIVATION = GateShapes.diamond();

    private GateShapes() {
    }

    static VoxelShape box(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Shapes.m_83048_((double)(x1 / 16.0), (double)(y1 / 16.0), (double)(z1 / 16.0), (double)(x2 / 16.0), (double)(y2 / 16.0), (double)(z2 / 16.0));
    }

    static VoxelShape turn(VoxelShape shape, Direction facing) {
        int turns = switch (facing) {
            case Direction.EAST -> 1;
            case Direction.SOUTH -> 2;
            case Direction.WEST -> 3;
            default -> 0;
        };
        for (int i = 0; i < turns; ++i) {
            VoxelShape[] rotated = new VoxelShape[]{Shapes.m_83040_()};
            shape.m_83286_((x1, y1, z1, x2, y2, z2) -> {
                rotated[0] = Shapes.m_83110_((VoxelShape)rotated[0], (VoxelShape)Shapes.m_83048_((double)(1.0 - z2), (double)y1, (double)x1, (double)(1.0 - z1), (double)y2, (double)x2));
            });
            shape = rotated[0];
        }
        return shape;
    }

    private static VoxelShape diamond() {
        double radius = 8.0 * Math.sqrt(2.0);
        VoxelShape shape = Shapes.m_83040_();
        for (int i = 0; i < 32; ++i) {
            double y1 = 2.0 * radius * (double)i / 32.0;
            double y2 = 2.0 * radius * (double)(i + 1) / 32.0;
            double halfWidth = radius - Math.min(Math.abs(y1 - radius), Math.abs(y2 - radius));
            shape = Shapes.m_83110_((VoxelShape)shape, (VoxelShape)GateShapes.box(8.0 - halfWidth, y1, 0.0, 8.0 + halfWidth, y2, 16.0));
        }
        return shape;
    }
}


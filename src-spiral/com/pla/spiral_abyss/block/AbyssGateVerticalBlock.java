/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Direction$Plane
 *  net.minecraft.world.item.context.BlockPlaceContext
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.HorizontalDirectionalBlock
 *  net.minecraft.world.level.block.Mirror
 *  net.minecraft.world.level.block.Rotation
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.StateDefinition$Builder
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.shapes.CollisionContext
 *  net.minecraft.world.phys.shapes.VoxelShape
 */
package com.pla.spiral_abyss.block;

import com.pla.spiral_abyss.block.GateShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class AbyssGateVerticalBlock
extends HorizontalDirectionalBlock {
    private static final VoxelShape[] SHAPES = new VoxelShape[4];

    public AbyssGateVerticalBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.m_49959_((BlockState)this.m_49966_().m_61124_((Property)f_54117_, (Comparable)Direction.NORTH));
    }

    protected void m_7926_(StateDefinition.Builder<Block, BlockState> builder) {
        builder.m_61104_(new Property[]{f_54117_});
    }

    public BlockState m_5573_(BlockPlaceContext context) {
        return (BlockState)this.m_49966_().m_61124_((Property)f_54117_, (Comparable)context.m_8125_().m_122424_());
    }

    public BlockState m_6843_(BlockState state, Rotation rotation) {
        return (BlockState)state.m_61124_((Property)f_54117_, (Comparable)rotation.m_55954_((Direction)state.m_61143_((Property)f_54117_)));
    }

    public BlockState m_6943_(BlockState state, Mirror mirror) {
        return state.m_60717_(mirror.m_54846_((Direction)state.m_61143_((Property)f_54117_)));
    }

    public VoxelShape m_5940_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[((Direction)state.m_61143_((Property)f_54117_)).m_122416_()];
    }

    static {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            AbyssGateVerticalBlock.SHAPES[facing.m_122416_()] = GateShapes.turn(GateShapes.VERTICAL, facing);
        }
    }
}


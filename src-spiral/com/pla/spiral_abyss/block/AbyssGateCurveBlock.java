/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Direction$Plane
 *  net.minecraft.world.item.context.BlockPlaceContext
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.level.LevelReader
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.HorizontalDirectionalBlock
 *  net.minecraft.world.level.block.Mirror
 *  net.minecraft.world.level.block.Rotation
 *  net.minecraft.world.level.block.SimpleWaterloggedBlock
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.StateDefinition$Builder
 *  net.minecraft.world.level.block.state.properties.BlockStateProperties
 *  net.minecraft.world.level.block.state.properties.BooleanProperty
 *  net.minecraft.world.level.block.state.properties.EnumProperty
 *  net.minecraft.world.level.block.state.properties.Half
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.level.material.Fluid
 *  net.minecraft.world.level.material.FluidState
 *  net.minecraft.world.level.material.Fluids
 *  net.minecraft.world.phys.shapes.CollisionContext
 *  net.minecraft.world.phys.shapes.VoxelShape
 */
package com.pla.spiral_abyss.block;

import com.pla.spiral_abyss.block.GateShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class AbyssGateCurveBlock
extends HorizontalDirectionalBlock
implements SimpleWaterloggedBlock {
    public static final EnumProperty<Half> HALF = BlockStateProperties.f_61402_;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.f_61362_;
    private static final VoxelShape[][] SHAPES = new VoxelShape[2][4];

    public AbyssGateCurveBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.m_49959_((BlockState)((BlockState)((BlockState)this.m_49966_().m_61124_((Property)f_54117_, (Comparable)Direction.NORTH)).m_61124_(HALF, (Comparable)Half.BOTTOM)).m_61124_((Property)WATERLOGGED, (Comparable)Boolean.valueOf(false)));
    }

    protected void m_7926_(StateDefinition.Builder<Block, BlockState> builder) {
        builder.m_61104_(new Property[]{f_54117_, HALF, WATERLOGGED});
    }

    public BlockState m_5573_(BlockPlaceContext context) {
        Direction face = context.m_43719_();
        boolean top = face == Direction.DOWN || face != Direction.UP && context.m_43720_().f_82480_ - (double)context.m_8083_().m_123342_() > 0.5;
        return (BlockState)((BlockState)((BlockState)this.m_49966_().m_61124_((Property)f_54117_, (Comparable)context.m_8125_())).m_61124_(HALF, (Comparable)(top ? Half.TOP : Half.BOTTOM))).m_61124_((Property)WATERLOGGED, (Comparable)Boolean.valueOf(context.m_43725_().m_6425_(context.m_8083_()).m_76152_() == Fluids.f_76193_));
    }

    public BlockState m_6843_(BlockState state, Rotation rotation) {
        return (BlockState)state.m_61124_((Property)f_54117_, (Comparable)rotation.m_55954_((Direction)state.m_61143_((Property)f_54117_)));
    }

    public BlockState m_6943_(BlockState state, Mirror mirror) {
        return state.m_60717_(mirror.m_54846_((Direction)state.m_61143_((Property)f_54117_)));
    }

    public VoxelShape m_5940_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.m_61143_(HALF) == Half.TOP ? 1 : 0][((Direction)state.m_61143_((Property)f_54117_)).m_122416_()];
    }

    public FluidState m_5888_(BlockState state) {
        return (Boolean)state.m_61143_((Property)WATERLOGGED) != false ? Fluids.f_76193_.m_76068_(false) : super.m_5888_(state);
    }

    public BlockState m_7417_(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (((Boolean)state.m_61143_((Property)WATERLOGGED)).booleanValue()) {
            level.m_186469_(pos, (Fluid)Fluids.f_76193_, Fluids.f_76193_.m_6718_((LevelReader)level));
        }
        return super.m_7417_(state, direction, neighbor, level, pos, neighborPos);
    }

    static {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            AbyssGateCurveBlock.SHAPES[0][facing.m_122416_()] = GateShapes.turn(GateShapes.CURVE_BOTTOM, facing);
            AbyssGateCurveBlock.SHAPES[1][facing.m_122416_()] = GateShapes.turn(GateShapes.CURVE_TOP, facing);
        }
    }
}


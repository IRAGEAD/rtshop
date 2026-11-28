/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Direction$Axis
 *  net.minecraft.util.StringRepresentable
 *  net.minecraft.world.item.context.BlockPlaceContext
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Mirror
 *  net.minecraft.world.level.block.Rotation
 *  net.minecraft.world.level.block.SlabBlock
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.StateDefinition$Builder
 *  net.minecraft.world.level.block.state.properties.BlockStateProperties
 *  net.minecraft.world.level.block.state.properties.EnumProperty
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.level.block.state.properties.SlabType
 *  net.minecraft.world.phys.shapes.CollisionContext
 *  net.minecraft.world.phys.shapes.Shapes
 *  net.minecraft.world.phys.shapes.VoxelShape
 */
package com.pla.spiral_abyss.block;

import com.pla.spiral_abyss.block.GateShapes;
import com.pla.spiral_abyss.init.BlockInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class AbyssGateSlabBlock
extends SlabBlock {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.f_61364_;
    public static final EnumProperty<Extension> EXTENSION = EnumProperty.m_61587_((String)"extension", Extension.class);
    private static final VoxelShape BOTTOM = GateShapes.box(2.0, 0.0, 0.0, 14.0, 12.0, 16.0);
    private static final VoxelShape TOP = GateShapes.box(2.0, 8.0, 0.0, 14.0, 20.0, 16.0);
    private static final VoxelShape DOUBLE = GateShapes.box(2.0, 0.0, 0.0, 14.0, 16.0, 16.0);
    private static final VoxelShape EXTENDED_NORTH = Shapes.m_83124_((VoxelShape)BOTTOM, (VoxelShape[])new VoxelShape[]{GateShapes.box(2.0, 0.0, -10.0, 14.0, 1.0, -9.0), GateShapes.box(2.0, 0.0, -9.0, 14.0, 2.0, 0.0), GateShapes.box(2.0, 2.0, -8.0, 14.0, 3.0, 0.0), GateShapes.box(2.0, 3.0, -7.0, 14.0, 4.0, -6.0), GateShapes.box(2.0, 3.0, -6.0, 14.0, 5.0, -5.0), GateShapes.box(2.0, 3.0, -5.0, 14.0, 6.0, -4.0), GateShapes.box(2.0, 3.0, -4.0, 14.0, 7.0, -3.0), GateShapes.box(2.0, 3.0, -3.0, 14.0, 8.0, -2.0), GateShapes.box(2.0, 3.0, -2.0, 14.0, 9.0, -1.0), GateShapes.box(2.0, 3.0, -1.0, 14.0, 10.0, 0.0)});
    private static final VoxelShape EXTENDED_EAST = GateShapes.turn(EXTENDED_NORTH, Direction.EAST);
    private static final VoxelShape EXTENDED_SOUTH = GateShapes.turn(EXTENDED_NORTH, Direction.SOUTH);
    private static final VoxelShape EXTENDED_WEST = GateShapes.turn(EXTENDED_NORTH, Direction.WEST);
    private static final VoxelShape[] X_SHAPES = new VoxelShape[]{GateShapes.turn(BOTTOM, Direction.EAST), GateShapes.turn(TOP, Direction.EAST), GateShapes.turn(DOUBLE, Direction.EAST)};

    public AbyssGateSlabBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.m_49959_((BlockState)((BlockState)this.m_49966_().m_61124_(AXIS, (Comparable)Direction.Axis.Z)).m_61124_(EXTENSION, (Comparable)((Object)Extension.NONE)));
    }

    protected void m_7926_(StateDefinition.Builder<Block, BlockState> builder) {
        super.m_7926_(builder);
        builder.m_61104_(new Property[]{AXIS, EXTENSION});
    }

    public BlockState m_5573_(BlockPlaceContext context) {
        BlockState state = super.m_5573_(context);
        if (state == null) {
            return null;
        }
        if (!context.m_43725_().m_8055_(context.m_8083_()).m_60713_((Block)this)) {
            state = (BlockState)state.m_61124_(AXIS, (Comparable)context.m_8125_().m_122434_());
        }
        return (BlockState)state.m_61124_(EXTENSION, (Comparable)((Object)AbyssGateSlabBlock.extension(state, (BlockGetter)context.m_43725_(), context.m_8083_())));
    }

    public BlockState m_6843_(BlockState state, Rotation rotation) {
        BlockState rotated = rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90 ? (BlockState)state.m_61124_(AXIS, (Comparable)(state.m_61143_(AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X)) : state;
        Extension extension = (Extension)((Object)state.m_61143_(EXTENSION));
        return extension == Extension.NONE ? rotated : (BlockState)rotated.m_61124_(EXTENSION, (Comparable)((Object)Extension.toward(rotation.m_55954_(extension.direction))));
    }

    public BlockState m_6943_(BlockState state, Mirror mirror) {
        Extension extension = (Extension)((Object)state.m_61143_(EXTENSION));
        return extension == Extension.NONE ? state : state.m_60717_(mirror.m_54846_(extension.direction));
    }

    public BlockState m_7417_(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BlockState updated = super.m_7417_(state, direction, neighbor, level, pos, neighborPos);
        if (!direction.m_122434_().m_122479_()) {
            return updated;
        }
        Direction.Axis axis = (Direction.Axis)updated.m_61143_(AXIS);
        if (direction.m_122434_() != axis) {
            return updated;
        }
        return (BlockState)updated.m_61124_(EXTENSION, (Comparable)((Object)AbyssGateSlabBlock.extension(updated, (BlockGetter)level, pos, direction, neighbor)));
    }

    private static Extension extension(BlockState state, BlockGetter level, BlockPos pos) {
        return AbyssGateSlabBlock.extension(state, level, pos, null, null);
    }

    private static Extension extension(BlockState state, BlockGetter level, BlockPos pos, Direction changed, BlockState neighbor) {
        if (state.m_61143_((Property)f_56353_) != SlabType.BOTTOM) {
            return Extension.NONE;
        }
        Direction first = state.m_61143_(AXIS) == Direction.Axis.Z ? Direction.NORTH : Direction.WEST;
        for (Direction direction : new Direction[]{first, first.m_122424_()}) {
            BlockState adjacent;
            BlockState blockState = adjacent = direction == changed ? neighbor : level.m_8055_(pos.m_121945_(direction));
            if (!adjacent.m_60713_((Block)BlockInit.ABYSS_GATE_ACTIVATE_BLOCK.get())) continue;
            return Extension.toward(direction);
        }
        return Extension.NONE;
    }

    public VoxelShape m_5940_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape voxelShape;
        int index;
        switch ((SlabType)state.m_61143_((Property)f_56353_)) {
            default: {
                throw new IncompatibleClassChangeError();
            }
            case BOTTOM: {
                int n = 0;
                break;
            }
            case TOP: {
                int n = 1;
                break;
            }
            case DOUBLE: {
                int n = index = 2;
            }
        }
        if (index == 0 && ((Extension)((Object)state.m_61143_(AbyssGateSlabBlock.EXTENSION))).direction != null && ((Extension)((Object)state.m_61143_(AbyssGateSlabBlock.EXTENSION))).direction.m_122434_() == state.m_61143_(AXIS)) {
            return switch ((Extension)((Object)state.m_61143_(EXTENSION))) {
                case Extension.NORTH -> EXTENDED_NORTH;
                case Extension.EAST -> EXTENDED_EAST;
                case Extension.SOUTH -> EXTENDED_SOUTH;
                case Extension.WEST -> EXTENDED_WEST;
                default -> BOTTOM;
            };
        }
        if (state.m_61143_(AXIS) == Direction.Axis.X) {
            voxelShape = X_SHAPES[index];
        } else {
            switch (index) {
                case 0: {
                    voxelShape = BOTTOM;
                    break;
                }
                case 1: {
                    voxelShape = TOP;
                    break;
                }
                default: {
                    voxelShape = DOUBLE;
                }
            }
        }
        return voxelShape;
    }

    public boolean m_7923_(BlockState state) {
        return true;
    }

    public static enum Extension implements StringRepresentable
    {
        NONE("none", null),
        NORTH("north", Direction.NORTH),
        EAST("east", Direction.EAST),
        SOUTH("south", Direction.SOUTH),
        WEST("west", Direction.WEST);

        private final String name;
        private final Direction direction;

        private Extension(String name, Direction direction) {
            this.name = name;
            this.direction = direction;
        }

        public String m_7912_() {
            return this.name;
        }

        static Extension toward(Direction direction) {
            return switch (direction) {
                case Direction.NORTH -> NORTH;
                case Direction.EAST -> EAST;
                case Direction.SOUTH -> SOUTH;
                case Direction.WEST -> WEST;
                default -> NONE;
            };
        }
    }
}


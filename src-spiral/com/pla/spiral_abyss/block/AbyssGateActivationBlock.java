/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Direction$Plane
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.context.BlockPlaceContext
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.EntityBlock
 *  net.minecraft.world.level.block.HorizontalDirectionalBlock
 *  net.minecraft.world.level.block.Mirror
 *  net.minecraft.world.level.block.Rotation
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntityTicker
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.StateDefinition$Builder
 *  net.minecraft.world.level.block.state.properties.BooleanProperty
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.shapes.CollisionContext
 *  net.minecraft.world.phys.shapes.Shapes
 *  net.minecraft.world.phys.shapes.VoxelShape
 */
package com.pla.spiral_abyss.block;

import com.pla.spiral_abyss.block.GateShapes;
import com.pla.spiral_abyss.block.entity.AbyssPortalBlockEntity;
import com.pla.spiral_abyss.init.BlockEntityInit;
import com.pla.spiral_abyss.world.AbyssPortalFrame;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class AbyssGateActivationBlock
extends HorizontalDirectionalBlock
implements EntityBlock {
    public static final BooleanProperty ACTIVE = BooleanProperty.m_61465_((String)"active");
    private static final VoxelShape[] SHAPES = new VoxelShape[4];

    public AbyssGateActivationBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.m_49959_((BlockState)((BlockState)this.m_49966_().m_61124_((Property)f_54117_, (Comparable)Direction.NORTH)).m_61124_((Property)ACTIVE, (Comparable)Boolean.valueOf(false)));
    }

    protected void m_7926_(StateDefinition.Builder<Block, BlockState> builder) {
        builder.m_61104_(new Property[]{f_54117_, ACTIVE});
    }

    public BlockState m_5573_(BlockPlaceContext context) {
        return (BlockState)this.m_49966_().m_61124_((Property)f_54117_, (Comparable)context.m_8125_().m_122424_());
    }

    public BlockEntity m_142194_(BlockPos pos, BlockState state) {
        return new AbyssPortalBlockEntity(pos, state);
    }

    public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(Level level, BlockState state, BlockEntityType<T> type) {
        return !level.f_46443_ && type == BlockEntityInit.ABYSS_PORTAL.get() ? (world, pos, current, entity) -> AbyssPortalBlockEntity.tick(world, pos, current) : null;
    }

    public void m_6402_(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (level instanceof ServerLevel) {
            ServerLevel server = (ServerLevel)level;
            if (AbyssPortalFrame.fromActivation(pos, state).activate(server)) {
                level.m_5594_(null, pos, SoundEvents.f_11860_, SoundSource.BLOCKS, 0.7f, 1.0f);
                if (placer instanceof ServerPlayer) {
                    ServerPlayer player = (ServerPlayer)placer;
                    player.m_213846_((Component)Component.m_237115_((String)"message.spiral_abyss.portal_activated"));
                }
            }
        }
    }

    public void m_6810_(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!(level.f_46443_ || state.m_60713_(replacement.m_60734_()) && state.m_61143_((Property)f_54117_) == replacement.m_61143_((Property)f_54117_))) {
            AbyssPortalFrame.fromActivation(pos, state).clearOpening(level);
        }
        super.m_6810_(state, level, pos, replacement, moving);
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

    public VoxelShape m_7952_(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.m_83040_();
    }

    public int m_7753_(BlockState state, BlockGetter level, BlockPos pos) {
        return 0;
    }

    public boolean m_7420_(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    public float m_7749_(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0f;
    }

    static {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            AbyssGateActivationBlock.SHAPES[facing.m_122416_()] = GateShapes.turn(GateShapes.ACTIVATION, facing);
        }
    }
}


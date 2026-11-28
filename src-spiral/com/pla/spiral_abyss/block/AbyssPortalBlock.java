/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction$Axis
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.RenderShape
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.StateDefinition$Builder
 *  net.minecraft.world.level.block.state.properties.BlockStateProperties
 *  net.minecraft.world.level.block.state.properties.EnumProperty
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.shapes.CollisionContext
 *  net.minecraft.world.phys.shapes.VoxelShape
 */
package com.pla.spiral_abyss.block;

import com.pla.spiral_abyss.world.AbyssParties;
import com.pla.spiral_abyss.world.AbyssPortalFrame;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class AbyssPortalBlock
extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.f_61364_;
    private static final VoxelShape X_PLANE = Block.m_49796_((double)0.0, (double)0.0, (double)7.0, (double)16.0, (double)16.0, (double)9.0);
    private static final VoxelShape Z_PLANE = Block.m_49796_((double)7.0, (double)0.0, (double)0.0, (double)9.0, (double)16.0, (double)16.0);

    public AbyssPortalBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.m_49959_((BlockState)this.m_49966_().m_61124_(AXIS, (Comparable)Direction.Axis.X));
    }

    protected void m_7926_(StateDefinition.Builder<Block, BlockState> builder) {
        builder.m_61104_(new Property[]{AXIS});
    }

    public RenderShape m_7514_(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    public VoxelShape m_5940_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.m_61143_(AXIS) == Direction.Axis.X ? X_PLANE : Z_PLANE;
    }

    public void m_213898_(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.m_46832_(pos.m_7918_(-3, -5, -3), pos.m_7918_(3, 5, 3)) && AbyssPortalFrame.fromOpening((Level)level, pos, (Direction.Axis)state.m_61143_(AXIS)).isEmpty()) {
            level.m_7731_(pos, Blocks.f_50016_.m_49966_(), 2);
        }
    }

    public void m_7892_(BlockState state, Level level, BlockPos pos, Entity entity) {
        ServerPlayer player;
        if (!(entity instanceof ServerPlayer) || (player = (ServerPlayer)entity).m_5833_() || !player.m_6084_() || !level.m_46472_().equals(Level.f_46428_)) {
            return;
        }
        if (!player.m_20191_().m_82381_(this.m_5940_(state, (BlockGetter)level, pos, CollisionContext.m_82749_()).m_83215_().m_82338_(pos))) {
            return;
        }
        if (player.m_20092_()) {
            player.m_20091_();
            return;
        }
        player.m_20091_();
        AbyssPortalFrame.fromOpening(level, pos, (Direction.Axis)state.m_61143_(AXIS)).filter(frame -> frame.active(level)).ifPresent(frame -> AbyssParties.get(player.f_8924_).portals().touch(player, (AbyssPortalFrame)frame));
    }
}


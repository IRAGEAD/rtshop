/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.RenderShape
 *  net.minecraft.world.level.block.state.BlockBehaviour
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.shapes.CollisionContext
 *  net.minecraft.world.phys.shapes.Shapes
 *  net.minecraft.world.phys.shapes.VoxelShape
 *  net.minecraftforge.fml.util.thread.EffectiveSide
 */
package com.finderfeed.fdbosses.content.blocks;

import com.finderfeed.fdbosses.BossClientPackets;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.fml.util.thread.EffectiveSide;

public class NoEntitySpawnBlock
extends Block {
    public static final VoxelShape SHAPE = Block.m_49796_((double)0.0, (double)0.0, (double)0.0, (double)16.0, (double)2.0, (double)16.0);

    public NoEntitySpawnBlock() {
        super(BlockBehaviour.Properties.m_60926_((BlockBehaviour)Blocks.f_50752_).m_60910_().m_60922_((state, getter, pos, type) -> false).m_60955_().m_60988_());
    }

    public VoxelShape m_5940_(BlockState p_60555_, BlockGetter p_60556_, BlockPos p_60557_, CollisionContext p_60558_) {
        Player player;
        if (EffectiveSide.get().isClient() && (player = BossClientPackets.getClientPlayer()) != null && player.m_9236_().f_46443_ && player.m_7500_()) {
            return SHAPE;
        }
        return Shapes.m_83040_();
    }

    public RenderShape m_7514_(BlockState p_60550_) {
        return RenderShape.INVISIBLE;
    }
}


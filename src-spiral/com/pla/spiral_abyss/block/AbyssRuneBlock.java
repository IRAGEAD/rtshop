/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.Direction
 *  net.minecraft.world.item.context.BlockPlaceContext
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.HorizontalDirectionalBlock
 *  net.minecraft.world.level.block.Mirror
 *  net.minecraft.world.level.block.Rotation
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.StateDefinition$Builder
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.level.material.PushReaction
 *  org.jetbrains.annotations.Nullable
 */
package com.pla.spiral_abyss.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.PushReaction;
import org.jetbrains.annotations.Nullable;

public final class AbyssRuneBlock
extends HorizontalDirectionalBlock {
    public AbyssRuneBlock(BlockBehaviour.Properties properties) {
        super(properties.m_278166_(PushReaction.BLOCK));
        this.m_49959_((BlockState)((BlockState)this.f_49792_.m_61090_()).m_61124_((Property)f_54117_, (Comparable)Direction.NORTH));
    }

    @Nullable
    public BlockState m_5573_(BlockPlaceContext context) {
        return (BlockState)this.m_49966_().m_61124_((Property)f_54117_, (Comparable)context.m_8125_().m_122424_());
    }

    public BlockState m_6843_(BlockState state, Rotation rotation) {
        return (BlockState)state.m_61124_((Property)f_54117_, (Comparable)rotation.m_55954_((Direction)state.m_61143_((Property)f_54117_)));
    }

    public BlockState m_6943_(BlockState state, Mirror mirror) {
        return state.m_60717_(mirror.m_54846_((Direction)state.m_61143_((Property)f_54117_)));
    }

    protected void m_7926_(StateDefinition.Builder<Block, BlockState> builder) {
        builder.m_61104_(new Property[]{f_54117_});
    }

    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }
}


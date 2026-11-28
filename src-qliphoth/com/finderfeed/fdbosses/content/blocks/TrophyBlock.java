/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.bedrock.animations.Animation
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.tile.FDEntityBlock
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.context.BlockPlaceContext
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.HorizontalDirectionalBlock
 *  net.minecraft.world.level.block.RenderShape
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntityType$BlockEntitySupplier
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.StateDefinition$Builder
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.BlockHitResult
 *  org.jetbrains.annotations.Nullable
 */
package com.finderfeed.fdbosses.content.blocks;

import com.finderfeed.fdbosses.content.tile_entities.TrophyBlockEntity;
import com.finderfeed.fdlib.systems.bedrock.animations.Animation;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.tile.FDEntityBlock;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class TrophyBlock
extends FDEntityBlock {
    private Supplier<Animation> idleAnimation;
    private Supplier<Animation> clickAnimation;
    private BlockEntityType.BlockEntitySupplier<?> tileEntitySupplier;

    public TrophyBlock(BlockBehaviour.Properties block, Supplier<Animation> idleAnimation, Supplier<Animation> clickAnimation, BlockEntityType.BlockEntitySupplier<? extends TrophyBlockEntity> tileEntitySupplier) {
        super(block);
        this.idleAnimation = idleAnimation;
        this.clickAnimation = clickAnimation;
        this.tileEntitySupplier = tileEntitySupplier;
        this.m_49959_((BlockState)((BlockState)this.m_49965_().m_61090_()).m_61124_((Property)HorizontalDirectionalBlock.f_54117_, (Comparable)Direction.NORTH));
    }

    public Supplier<Animation> getIdleAnimation() {
        return this.idleAnimation;
    }

    public Supplier<Animation> getClickAnimation() {
        return this.clickAnimation;
    }

    public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand p_60507_, BlockHitResult p_60508_) {
        ServerLevel serverLevel;
        BlockEntity block;
        if (level instanceof ServerLevel && (block = (serverLevel = (ServerLevel)level).m_7702_(pos)) instanceof TrophyBlockEntity) {
            TrophyBlockEntity trophy = (TrophyBlockEntity)block;
            trophy.onClick(player);
        }
        return super.m_6227_(state, level, pos, player, p_60507_, p_60508_);
    }

    protected void m_7926_(StateDefinition.Builder<Block, BlockState> builder) {
        super.m_7926_(builder);
        builder.m_61104_(new Property[]{HorizontalDirectionalBlock.f_54117_});
    }

    @Nullable
    public BlockEntity m_142194_(BlockPos blockpos, BlockState state) {
        return this.tileEntitySupplier.m_155267_(blockpos, state);
    }

    public RenderShape m_7514_(BlockState p_60550_) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    public BlockState m_5573_(BlockPlaceContext ctx) {
        return (BlockState)this.m_49966_().m_61124_((Property)HorizontalDirectionalBlock.f_54117_, (Comparable)ctx.m_8125_());
    }
}


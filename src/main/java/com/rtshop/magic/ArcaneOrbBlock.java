package com.rtshop.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 별빛 수정 구체: 흑요석 받침 위에 자수정 구슬이 떠 있고 룬 고리 두 개가 돈다. 우클릭하면 마법 뽑기 창이 열린다. */
public class ArcaneOrbBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Shapes.or(box(1, 0, 1, 15, 2, 15), box(3, 2, 3, 13, 10, 13), box(2, 10, 2, 14, 12, 14));

    public ArcaneOrbBlock(Properties p) {
        super(p);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArcaneOrbBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!level.isClientSide && player instanceof ServerPlayer sp) MagicGacha.open(sp, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return 9;
    }
}

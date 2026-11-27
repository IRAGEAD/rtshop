package com.rtshop.stock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 조합 시세판 (3.72.0): 마을 광장에 세워 두는 게시판. 우클릭하면 조합 여섯 곳의 시세(지난 14일) · 이번 주 배당 · 소식이 열린다 (사고팔기는 은행원에게서). */
public class StockBoardBlock extends HorizontalDirectionalBlock {
    /** 기둥 둘과 판 (북쪽을 볼 때). */
    private static final VoxelShape NS = Shapes.or(Block.box(1, 0, 7, 3, 16, 9), Block.box(13, 0, 7, 15, 16, 9), Block.box(0, 5, 6.5, 16, 15.5, 9.5));
    private static final VoxelShape EW = Shapes.or(Block.box(7, 0, 1, 9, 16, 3), Block.box(7, 0, 13, 9, 16, 15), Block.box(6.5, 5, 0, 9.5, 15.5, 16));

    public StockBoardBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        Direction f = state.getValue(FACING);
        return f == Direction.EAST || f == Direction.WEST ? EW : NS;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            Stocks.open(sp, false, null, null, true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}

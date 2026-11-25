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

/** 별빛 수정 구체 「별빛 천구」: 남빛 받침 위에 푸른 구슬이 떠 있고, 금·은 고리 세 겹(혼천의)이 따로 돈다. 우클릭하면 룬 제단이 열린다. */
public class ArcaneOrbBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Shapes.or(box(2, 0, 2, 14, 2, 14), box(4, 2, 4, 12, 4, 12), box(6, 4, 6, 10, 9, 10), box(5, 9, 5, 11, 10, 11));

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
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            if (com.rtshop.rules.WorldRules.refuseAltar(sp, pos)) return InteractionResult.CONSUME; // 마을의 제단 하나만 쓴다
            MagicGacha.open(sp, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Cinematic Interactions 가 장면을 시작할 때(또는 장면을 꺼 둔 경우 창으로) 여는 메뉴. */
    @Override
    public net.minecraft.world.MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new net.minecraft.world.SimpleMenuProvider((id, inv, player) -> {
            if (player instanceof ServerPlayer sp) {
                if (com.rtshop.rules.WorldRules.refuseAltar(sp, pos)) return null;
                if (MagicGacha.locked(sp)) { // 잠겨 있으면 메뉴(장면)를 열지 않는다
                    com.rtshop.notice.Notice.send(sp, net.minecraft.network.chat.Component.translatable("rtshop.magic.locked"), true);
                    return null;
                }
                MagicGacha.menuOpened(sp, pos);
            }
            return new MagicMenu(id, inv, pos);
        }, net.minecraft.network.chat.Component.translatable("rtshop.magic.altar"));
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return 9;
    }
}

package com.rtshop.enhance;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 강화 모루: 모양·방향은 모루와 같고(그래서 minecraft:anvil 태그에 들어가 Cinematic 모루 장면이 재생됨),
 * 떨어지지 않고 닳지도 않는다. 우클릭하면 강화 창이 열린다.
 */
public class EnhanceAnvilBlock extends AnvilBlock {
    public static final Component TITLE = Component.translatable("container.rtshop.enhance");

    public EnhanceAnvilBlock(Properties props) {
        super(props);
    }

    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider((id, inv, player) -> new EnhanceMenu(id, inv, ContainerLevelAccess.create(level, pos)), TITLE);
    }

    // 3.49 의 새 모습(점판암 팔각 받침 + 단조 강철 모루)에 맞춘 테두리·충돌 모양. 윗면 높이는 모루와 같다(장면의 장비 자리).
    private static final net.minecraft.world.phys.shapes.VoxelShape BASE = net.minecraft.world.phys.shapes.Shapes.or(
            box(0.5, 0, 0.5, 15.5, 3, 15.5), box(1.5, 3, 1.5, 14.5, 4.5, 14.5), box(4.5, 4.5, 4, 11.5, 11.5, 12));
    private static final net.minecraft.world.phys.shapes.VoxelShape ALONG_Z = net.minecraft.world.phys.shapes.Shapes.or(BASE, box(3, 11.5, 0, 13, 16, 16));
    private static final net.minecraft.world.phys.shapes.VoxelShape ALONG_X = net.minecraft.world.phys.shapes.Shapes.or(BASE, box(0, 11.5, 3, 16, 16, 13));

    /** 모루의 긴 쪽이 남북(z)으로 놓였는가. */
    public static boolean alongZ(BlockState state) {
        return state.getValue(FACING).getAxis() == net.minecraft.core.Direction.Axis.Z;
    }

    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
                                                               net.minecraft.world.phys.shapes.CollisionContext ctx) {
        return alongZ(state) ? ALONG_Z : ALONG_X;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // 떨어지지 않는다
    }
}

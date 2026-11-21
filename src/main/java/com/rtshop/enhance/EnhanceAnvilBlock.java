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

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // 떨어지지 않는다
    }
}

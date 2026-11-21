package com.rtshop.item;

import com.rtshop.land.PlotManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 구획 지팡이 (OP 전용): 구불구불한 밭 같은 땅의 모양을 칸 단위로 골라 구획 토지로 등록한다.
 * 우클릭 = 지금 모드로 고르기, 좌클릭 = 빼기, 웅크리고 우클릭 = 모드 바꾸기.
 */
public class PlotWandItem extends Item {
    public PlotWandItem(Properties props) {
        super(props);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tips, TooltipFlag flag) {
        tips.add(Component.translatable("item.rtshop.plot_wand.tip1").withStyle(ChatFormatting.GOLD));
        tips.add(Component.translatable("item.rtshop.plot_wand.tip2").withStyle(ChatFormatting.GRAY));
        tips.add(Component.translatable("item.rtshop.plot_wand.tip3").withStyle(ChatFormatting.GRAY));
        tips.add(Component.translatable("item.rtshop.plot_wand.tip4").withStyle(ChatFormatting.DARK_GRAY));
    }

    /** 들고 좌클릭해도 블록이 부서지지 않게 한다 (좌클릭은 선택 빼기). */
    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp && hand == InteractionHand.MAIN_HAND) {
            PlotManager.onRightClick(sp, null);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!ctx.getLevel().isClientSide && ctx.getPlayer() instanceof ServerPlayer sp && ctx.getHand() == InteractionHand.MAIN_HAND) {
            PlotManager.onRightClick(sp, ctx.getClickedPos());
        }
        return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
    }
}

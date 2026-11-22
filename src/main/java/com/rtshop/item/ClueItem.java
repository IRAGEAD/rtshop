package com.rtshop.item;

import com.rtshop.story.ScyllaClues;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** 스킬라 단서: 쓰면 단서 수첩에 기록, 웅크리고 쓰면 수첩. 주민에게 들고 말을 걸면 소문. */
public class ClueItem extends Item {
    public ClueItem(Properties p) {
        super(p);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp) ScyllaClues.use(sp, hand);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.rtshop.scylla_clue.desc1").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tip.add(Component.translatable("item.rtshop.scylla_clue.desc2").withStyle(ChatFormatting.DARK_AQUA));
    }
}

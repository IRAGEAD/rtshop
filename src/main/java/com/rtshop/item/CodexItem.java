package com.rtshop.item;

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

/** 조사대 도감: 들고 우클릭하면 펼쳐진다. 마주친 몹은 모습과 이름이, 잡은 몹은 능력치·전리품·설명이, 손에 넣은 장비는 능력이 적힌다. */
public class CodexItem extends Item {
    public CodexItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) com.rtshop.story.Codex.open(sp);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.rtshop.codex.tip1").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("item.rtshop.codex.tip2").withStyle(ChatFormatting.DARK_GRAY));
    }
}

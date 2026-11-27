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

/**
 * 조사대 도감 · 조사대 안내서 · 조사 수첩: 들고 우클릭하면 펼쳐진다. 도감 = 마주친 몹은 모습과 이름이, 잡은 몹은 능력치·전리품·설명이, 손에 넣은 장비는 능력이 적힌다.
 * 조사 수첩 (3.75.0, 사용자 W-3 "조사대 조사 수첩도 명령어 시스템에서 가이드북과 도감처럼 아이템 형식으로") = 살펴본 흔적의 기록 (예전에는 /rtshop notebook 으로만 열었다).
 */
public class CodexItem extends Item {
    public static final int CODEX = 0, GUIDE = 1, NOTEBOOK = 2;
    private final int kind;

    public CodexItem(Properties p, boolean guide) {
        this(p, guide ? GUIDE : CODEX);
    }

    public CodexItem(Properties p, int kind) {
        super(p);
        this.kind = kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            if (kind == GUIDE) com.rtshop.story.Guide.open(sp);
            else if (kind == NOTEBOOK) com.rtshop.story.Spots.openNotebook(sp);
            else com.rtshop.story.Codex.open(sp);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable(getDescriptionId() + ".tip1").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable(getDescriptionId() + ".tip2").withStyle(ChatFormatting.DARK_GRAY));
    }
}

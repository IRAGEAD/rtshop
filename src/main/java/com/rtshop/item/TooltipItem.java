package com.rtshop.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** 설명 줄(번역 키)이 붙는 단순 아이템 (뽑기 티켓, 뽑기 조각). */
public class TooltipItem extends Item {
    private final String[] tips;
    private final boolean foil;

    public TooltipItem(Properties props, boolean foil, String... tipKeys) {
        super(props);
        this.tips = tipKeys;
        this.foil = foil;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return foil || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        for (String k : tips) {
            tip.add(Component.translatable(k).withStyle(ChatFormatting.GRAY));
        }
    }
}

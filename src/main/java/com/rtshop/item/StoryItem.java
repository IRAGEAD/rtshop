package com.rtshop.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** 이야기 물건 (꺼지지 않는 불씨, 연대기 페이지, 얼음 결정…). 설명은 언어 파일 item.rtshop.<id>.desc1~4. */
public class StoryItem extends Item {
    private final boolean foil;
    private final int lines;

    public StoryItem(Properties p, boolean foil, int lines) {
        super(p);
        this.foil = foil;
        this.lines = lines;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return foil || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        String base = getDescriptionId() + ".desc";
        for (int i = 1; i <= lines; i++) {
            tip.add(Component.translatable(base + i).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        if (stack.hasTag() && stack.getTag().contains("rtshop_page")) {
            tip.add(Component.literal(stack.getTag().getString("rtshop_page")).withStyle(ChatFormatting.GOLD));
        }
    }
}

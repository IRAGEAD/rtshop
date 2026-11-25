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
 * 이야기 물건 (꺼지지 않는 불씨, 연대기 페이지, 얼음 결정…). 설명은 언어 파일 item.rtshop.<id>.desc1~4.
 * 글이 적힌 것(서약서·연대기 페이지·일지…)은 들고 우클릭하면 펼쳐 읽는다: config/rtshop/readables.json.
 */
public class StoryItem extends Item {
    private final boolean foil;
    private final int lines;

    public StoryItem(Properties p, boolean foil, int lines) {
        super(p);
        this.foil = foil;
        this.lines = lines;
    }

    /** 기본으로 글이 들어 있는 물건 (설명에 "우클릭: 펼쳐 읽기" 를 보인다). 실제 내용은 서버의 readables.json 이 정한다. */
    public static final java.util.Set<String> READABLE = java.util.Set.of("oath_scroll", "chronicle_page", "chronicle_last_page",
            "mother_journal", "old_expedition_map", "frozen_logbook");

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp && com.rtshop.story.Readables.open(sp, stack)) return InteractionResultHolder.success(stack);
        return InteractionResultHolder.pass(stack);
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
        net.minecraft.resources.ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(this);
        if (id != null && READABLE.contains(id.getPath())) {
            tip.add(Component.translatable("rtshop.story_item.read").withStyle(ChatFormatting.YELLOW));
        }
        // 안내: 메인 스토리 물건이라는 표시와 어디에 쓰는지 (item.rtshop.<id>.guide)
        tip.add(Component.translatable("rtshop.story_item.header").withStyle(ChatFormatting.GOLD));
        tip.add(Component.translatable(getDescriptionId() + ".guide").withStyle(ChatFormatting.AQUA));
    }
}

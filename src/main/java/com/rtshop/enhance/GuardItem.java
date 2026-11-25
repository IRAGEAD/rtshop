package com.rtshop.enhance;

import com.rtshop.item.TooltipItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 강화 보호권 (3.49 부터): 강화 모루 위에 장비와 함께 올려 두면(보호권 칸) 그 강화 한 번에 한 장이 쓰이고, 결과와 상관없이 사라진다.
 * 한 번에 한 가지만 쓸 수 있다. 손에 들고 우클릭하는 옛 방식은 없앴다 (누르면 쓰는 법을 알려 준다).
 */
public class GuardItem extends TooltipItem {
    private final Enhance.Guard guard;

    public Enhance.Guard guard() {
        return guard;
    }

    public GuardItem(Properties props, boolean foil, Enhance.Guard guard, String... tips) {
        super(props, foil, tips);
        this.guard = guard;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (player instanceof ServerPlayer sp) {
            com.rtshop.notice.Notice.send(sp, net.minecraft.network.chat.Component.translatable("rtshop.enhance.guard_howto").withStyle(net.minecraft.ChatFormatting.YELLOW), true);
        }
        return InteractionResultHolder.fail(stack);
    }
}

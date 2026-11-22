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

/** 강화 보호권: 손에 들고 우클릭하면 미리 사용 (1장 소모) → 다음 강화 1번에 적용되고 결과와 상관없이 사라진다. */
public class GuardItem extends TooltipItem {
    private final Enhance.Guard guard;

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
        if (player instanceof ServerPlayer sp && Enhance.useGuardItem(sp, guard)) {
            if (!sp.getAbilities().instabuild) stack.shrink(1);
            level.playSound(null, sp.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.3F);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.fail(stack);
    }
}

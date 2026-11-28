package com.rtshop.item;

import com.rtshop.rules.TownSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 귀환권 (3.52): 우클릭을 누르고 있으면(3초) 마을의 기준 자리(/rtshop spawn set)로 돌아간다.
 * 일반: 같은 차원에서, 기준 자리로부터 가로 거리 10000칸 안에서만. 고급: 어디서든 (하루에 5장까지만 살 수 있다).
 * 레이드 구역·기억의 문 안에서는 쓸 수 없다 (거기서는 레이드 탈출권).
 */
public class ReturnTicketItem extends Item {
    private final boolean premium;

    public ReturnTicketItem(Properties props, boolean premium) {
        super(props);
        this.premium = premium;
    }

    public boolean premium() {
        return premium;
    }

    /** 지금 쓸 수 없는 까닭 (쓸 수 있으면 null). */
    public static Component refusal(ServerPlayer sp, boolean premium) {
        TownSpawn.Config c = TownSpawn.get();
        if (TownSpawn.townPoint(sp.server) == null) return Component.translatable("rtshop.return.no_spot");   // 3.81: 정하지 않았어도 아모노스 맵이면 부두
        try {
            if (com.rtshop.raid.Raid.inArena(sp) || sp.level().dimension() == com.rtshop.story.StoryArena.DIM) {
                return Component.translatable("rtshop.return.in_arena");
            }
        } catch (Throwable ignored) {
        }
        if (!premium && !TownSpawn.inNormalRange(sp)) return Component.translatable("rtshop.return.too_far", c.normalRange);
        long left = sp.getAbilities().instabuild ? 0 : TownSpawn.cooldownLeft(sp, premium);
        if (left > 0) return Component.translatable("rtshop.return.cooldown", TownSpawn.leftText(left));
        return null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            Component no = refusal(sp, premium);
            if (no != null) {
                com.rtshop.notice.Notice.send(sp, no.copy().withStyle(ChatFormatting.RED));
                return InteractionResultHolder.fail(stack);
            }
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return TownSpawn.get().channelTicks;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (user instanceof ServerPlayer sp) {
            Component no = refusal(sp, premium);
            if (no != null) {
                com.rtshop.notice.Notice.send(sp, no.copy().withStyle(ChatFormatting.RED));
                return stack;
            }
            level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.2F);
            if (TownSpawn.send(sp)) {
                if (!sp.getAbilities().instabuild) stack.shrink(1);
                if (!sp.getAbilities().instabuild) TownSpawn.used(sp, premium);
                sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.0F);
                com.rtshop.notice.Notice.send(sp, Component.translatable("rtshop.return.done").withStyle(ChatFormatting.GREEN));
            }
        }
        return stack;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return premium;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.rtshop.return_ticket.tip1").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable(premium ? "item.rtshop.return_ticket_plus.tip2" : "item.rtshop.return_ticket.tip2", TownSpawn.get().normalRange)
                .withStyle(ChatFormatting.DARK_GRAY));
        int cd = TownSpawn.get().cooldownMinutes(premium);
        if (cd > 0) tip.add(Component.translatable("item.rtshop.return_ticket.tip_cd", cd).withStyle(ChatFormatting.DARK_GRAY));
        if (premium) tip.add(Component.translatable("item.rtshop.return_ticket_plus.tip3", TownSpawn.get().premiumDailyLimit).withStyle(ChatFormatting.DARK_GRAY));
    }
}

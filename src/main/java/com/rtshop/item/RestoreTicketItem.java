package com.rtshop.item;

import com.rtshop.attendance.Attendance;
import com.rtshop.attendance.AttendanceConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** 일일 출석 진행도 복구권: 우클릭하면 초기화된 출석 진행도를 되돌린다 (초기화 후 24시간 안, 24시간마다 한 번). */
public class RestoreTicketItem extends Item {
    public RestoreTicketItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide() || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.success(stack);
        }
        if (Attendance.useTicket(sp)) {
            if (!sp.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        AttendanceConfig c = AttendanceConfig.get();
        tip.add(Component.translatable("rtshop.att.ticket_tip1").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("rtshop.att.ticket_tip2", Math.round(c.restoreWindowHours), Math.round(c.restoreCooldownHours))
                .withStyle(ChatFormatting.GRAY));
    }
}

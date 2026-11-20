package com.rtshop.item;

import com.rtshop.economy.Economy;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
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
import java.util.UUID;

/** Gold 수표. 금액과 일련번호는 서버 장부에도 기록되어 복사본은 한 번만 쓸 수 있다. */
public class CheckItem extends Item {
    public static final String TAG = "rtshop_check";

    public CheckItem(Properties props) {
        super(props);
    }

    public static ItemStack create(UUID id, long amount, String issuer) {
        ItemStack stack = new ItemStack(ModItems.GOLD_CHECK.get());
        CompoundTag t = new CompoundTag();
        t.putUUID("id", id);
        t.putLong("amount", amount);
        t.putString("issuer", issuer);
        stack.getOrCreateTag().put(TAG, t);
        return stack;
    }

    @Nullable
    public static UUID id(ItemStack stack) {
        CompoundTag t = stack.getTagElement(TAG);
        return t != null && t.hasUUID("id") ? t.getUUID("id") : null;
    }

    public static long amount(ItemStack stack) {
        CompoundTag t = stack.getTagElement(TAG);
        return t == null ? 0 : Math.max(0, t.getLong("amount"));
    }

    public static void setAmount(ItemStack stack, long amount) {
        CompoundTag t = stack.getTagElement(TAG);
        if (t != null) {
            t.putLong("amount", amount);
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        long a = amount(stack);
        if (a > 0) {
            return Component.translatable("item.rtshop.gold_check.named", String.format("%,d", a));
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tips, TooltipFlag flag) {
        CompoundTag t = stack.getTagElement(TAG);
        if (t != null && !t.getString("issuer").isEmpty()) {
            tips.add(Component.translatable("item.rtshop.gold_check.issuer", t.getString("issuer")).withStyle(ChatFormatting.GRAY));
        }
        tips.add(Component.translatable("item.rtshop.gold_check.tip1").withStyle(ChatFormatting.GOLD));
        tips.add(Component.translatable("item.rtshop.gold_check.tip2").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            Economy.redeemCheck(sp, stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}

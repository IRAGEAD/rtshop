package com.rtshop.item;

import com.rtshop.land.LandManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** 토지 문서. 원하는 곳에 서서 우클릭하면 그 자리가 내 토지가 된다. */
public class DeedItem extends Item {
    public enum Type {
        FIRST("first", 16), NORMAL("normal", 16), LARGE("large", 32);

        public final String id;
        public final int size;

        Type(String id, int size) {
            this.id = id;
            this.size = size;
        }

        public static Type byId(String id) {
            for (Type t : values()) {
                if (t.id.equals(id)) {
                    return t;
                }
            }
            return NORMAL;
        }

        public Item item() {
            return switch (this) {
                case FIRST -> ModItems.FIRST_LAND_DEED.get();
                case LARGE -> ModItems.LARGE_LAND_DEED.get();
                default -> ModItems.LAND_DEED.get();
            };
        }
    }

    public final Type type;

    public DeedItem(Type type, Properties props) {
        super(props);
        this.type = type;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tips, TooltipFlag flag) {
        tips.add(Component.translatable("item.rtshop.deed.size", type.size, type.size, type.size).withStyle(ChatFormatting.GOLD));
        tips.add(Component.translatable("item.rtshop.deed.tip1").withStyle(ChatFormatting.GRAY));
        tips.add(Component.translatable("item.rtshop.deed.tip2").withStyle(ChatFormatting.GRAY));
        tips.add(Component.translatable("item.rtshop.deed.tip3").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            LandManager.useDeed(sp, stack, type);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!ctx.getLevel().isClientSide && ctx.getPlayer() instanceof ServerPlayer sp) {
            LandManager.useDeed(sp, ctx.getItemInHand(), type);
        }
        return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
    }
}

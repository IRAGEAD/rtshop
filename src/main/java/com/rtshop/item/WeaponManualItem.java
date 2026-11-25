package com.rtshop.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 무기 설명서: 특수기가 있는 무기를 처음 얻으면 한 권씩 받는다 (WeaponManuals). 들고 우클릭하면 그 무기의 사용법이 펼쳐진다:
 * 기본 연속 공격, 달리며·뛰어서 치는 공격, 특수기의 이름·설명·쓰는 법, 전투 조작 키. 내용은 Epic Fight 의 실제 무기 정보에서 읽어 온다.
 * NBT: weapon = 그 무기의 아이템 ID.
 */
public class WeaponManualItem extends Item {
    public static final String TAG = "weapon";

    public WeaponManualItem(Properties p) {
        super(p);
    }

    public static ItemStack of(ItemStack weapon) {
        ItemStack s = new ItemStack(ModItems.WEAPON_MANUAL.get());
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(weapon.getItem());
        s.getOrCreateTag().putString(TAG, id == null ? "" : id.toString());
        return s;
    }

    /** 이 설명서가 다루는 무기 (모르면 빈 것). */
    public static ItemStack weapon(ItemStack manual) {
        if (!manual.hasTag()) return ItemStack.EMPTY;
        ResourceLocation rl = ResourceLocation.tryParse(manual.getTag().getString(TAG));
        Item it = rl == null ? null : ForgeRegistries.ITEMS.getValue(rl);
        return it == null || it == net.minecraft.world.item.Items.AIR ? ItemStack.EMPTY : new ItemStack(it);
    }

    @Override
    public Component getName(ItemStack stack) {
        ItemStack w = weapon(stack);
        return w.isEmpty() ? super.getName(stack) : Component.translatable("item.rtshop.weapon_manual.named", w.getHoverName());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.rtshop.client.ClientHooks.openWeaponManual(weapon(stack)));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.rtshop.weapon_manual.tip1").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("item.rtshop.weapon_manual.tip2").withStyle(ChatFormatting.DARK_GRAY));
    }
}

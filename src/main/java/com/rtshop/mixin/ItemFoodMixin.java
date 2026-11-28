package com.rtshop.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

import javax.annotation.Nullable;

/**
 * 3.84 (사용자 11-29 "음식에 표시된 것과 다르게 배고픔이 차는 문제"): 음식이 채우는 양을 RT Shop 이 바꾸는 것(양산 음식 70% · 요리사 패시브)을
 * 먹은 뒤에 덜어 내거나 더하는 대신, 그 음식의 값(FoodProperties) 자체를 사람에 맞게 돌려준다 — 먹을 때(FoodData.eat)와 음식 설명(Quark 의 음식 칸)이
 * 같은 값을 본다. Forge 의 IForgeItem.getFoodProperties(ItemStack, LivingEntity) 기본 구현을 Item 에서 덮는다 (그 메서드를 직접 덮은 모드 음식은 그대로).
 * Forge 가 붙인 메서드라 이름이 바뀌지 않는다 (remap 필요 없음).
 */
@Mixin(value = Item.class, remap = false)
public abstract class ItemFoodMixin {
    @Nullable
    public FoodProperties getFoodProperties(ItemStack stack, @Nullable LivingEntity entity) {
        Item self = (Item) (Object) this;
        FoodProperties base = self.getFoodProperties();
        try {
            return com.rtshop.rules.FoodScale.scale(self, stack, entity, base);
        } catch (Throwable t) {
            return base;
        }
    }
}

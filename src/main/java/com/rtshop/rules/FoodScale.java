package com.rtshop.rules;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Set;

/**
 * 3.84: 사람이 먹을 때 음식이 채우는 배고픔 (mixin/ItemFoodMixin 이 부른다). 먹는 순간과 음식 설명이 같은 값을 쓴다.
 * <ul>
 *   <li>양산 음식 (food_rules.json 의 items, 사용자 11-29 "양산 가능한 음식이 채워주는 배고픔 수치를 30% 감소시키기"): factor 배 (기본 0.7, 적어도 1칸).</li>
 *   <li>요리사 패시브 foodBonus (기본 +20%): 그 위에 더한다 (적어도 1칸).</li>
 * </ul>
 * 포만감은 배고픔에 비례하므로(배고픔 × 포만 계수 × 2) 따로 셈하지 않는다. 사람이 아닌 것(늑대 · 고양이)과 누가 먹는지 모르는 곳(상점 값 셈)은 원래 값.
 * 화면 쪽은 서버가 보내 준 값(JobSyncPacket)으로 셈한다.
 */
public final class FoodScale {
    private FoodScale() {
    }

    /** 화면 쪽: 서버가 보내 준 양산 음식 규칙과 내 요리사 보너스. */
    public static volatile boolean clientEnabled = true;
    public static volatile double clientFactor = 0.7;
    public static volatile Set<String> clientItems = Set.of();
    public static volatile double clientCookBonus = 0;
    /** 화면 쪽 값을 서버가 한 번이라도 보냈는가 (보내기 전에는 이 컴퓨터의 food_rules.json 을 쓴다). */
    public static volatile boolean clientSynced;

    private static volatile Boolean mixed;

    /** ItemFoodMixin 이 걸렸는가 (Item 에 getFoodProperties(ItemStack, LivingEntity) 가 생겼는가). 안 걸렸으면 예전처럼 먹은 뒤에 고친다. */
    public static boolean mixed() {
        Boolean m = mixed;
        if (m == null) {
            try {
                Item.class.getDeclaredMethod("getFoodProperties", ItemStack.class, LivingEntity.class);
                m = true;
            } catch (Throwable t) {
                m = false;
            }
            mixed = m;
        }
        return m;
    }

    public static FoodProperties scale(Item item, ItemStack stack, LivingEntity entity, FoodProperties base) {
        if (base == null || !(entity instanceof Player p)) return base;
        boolean staple;
        double factor, bonus;
        if (p.level().isClientSide) {
            if (clientSynced) {
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
                staple = clientEnabled && clientFactor < 1 && id != null && clientItems.contains(id.toString());
                factor = clientFactor;
            } else {
                staple = FoodRules.weak(stack.isEmpty() ? new ItemStack(item) : stack);
                factor = FoodRules.get().factor;
            }
            bonus = clientCookBonus;
        } else {
            staple = FoodRules.weak(stack.isEmpty() ? new ItemStack(item) : stack);
            factor = FoodRules.get().factor;
            bonus = p instanceof ServerPlayer sp ? com.rtshop.job.Jobs.stat(sp, "foodBonus") : 0;
        }
        int n0 = base.getNutrition();
        int n = nutrition(n0, staple, factor, bonus);
        if (n == n0) return base;
        return copy(base, n);
    }

    /** 원래 배고픔 n0 칸에서 실제로 차는 칸. */
    public static int nutrition(int n0, boolean staple, double factor, double bonus) {
        int n = n0;
        if (staple && factor < 1 && n0 > 0) n = (int) Math.max(1, Math.round(n0 * factor));
        if (bonus > 0 && n > 0) n += (int) Math.max(1, Math.round(n * bonus));
        return n;
    }

    /** 배고픔만 바꾼 같은 음식 값 (포만 계수 · 효과 · 고기 · 언제나 먹을 수 있음 · 빨리 먹기는 그대로). */
    public static FoodProperties copy(FoodProperties base, int nutrition) {
        FoodProperties.Builder b = new FoodProperties.Builder().nutrition(Math.max(0, nutrition)).saturationMod(base.getSaturationModifier());
        if (base.isMeat()) b.meat();
        if (base.canAlwaysEat()) b.alwaysEat();
        if (base.isFastFood()) b.fast();
        List<Pair<MobEffectInstance, Float>> effects = base.getEffects();
        for (Pair<MobEffectInstance, Float> e : effects) {
            MobEffectInstance inst = e.getFirst();
            if (inst == null) continue;
            b.effect(() -> new MobEffectInstance(inst), e.getSecond());
        }
        return b.build();
    }
}

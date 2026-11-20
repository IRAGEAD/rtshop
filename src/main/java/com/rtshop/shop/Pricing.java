package com.rtshop.shop;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraftforge.common.ToolActions;

/**
 * 아이템의 실제 능력치(공격력, 방어력, 내구도, 희귀도, 포만감)로 Gold 가격을 계산합니다.
 * 바닐라 철/다이아/네더라이트 장비를 기준으로 맞춘 공식이라 모드 장비도 비슷한 급이면 비슷한 가격이 됩니다.
 */
public final class Pricing {
    private Pricing() {
    }

    public static double attackDamage(ItemStack s) {
        try {
            double total = 0;
            for (AttributeModifier m : s.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE)) {
                if (m.getOperation() == AttributeModifier.Operation.ADDITION) {
                    total += m.getAmount();
                }
            }
            return total;
        } catch (Throwable t) {
            return 0;
        }
    }

    public static double rarityFactor(ItemStack s) {
        try {
            return switch (s.getRarity()) {
                case UNCOMMON -> 1.5;
                case RARE -> 2.5;
                case EPIC -> 4.0;
                default -> 1.0;
            };
        } catch (Throwable t) {
            return 1.0;
        }
    }

    /** 내구도 계수: 철(250) = 1.0, 다이아(1561) ≈ 4.3, 부서지지 않는 아이템 = 4.0 */
    static double durabilityFactor(ItemStack s) {
        int u = s.getMaxDamage();
        if (u <= 0) {
            return 4.0;
        }
        return Math.max(0.3, Math.min(6.0, Math.pow(u / 250.0, 0.8)));
    }

    static double quality(ItemStack s) {
        return durabilityFactor(s) * rarityFactor(s) * (s.getItem().isFireResistant() ? 3.0 : 1.0);
    }

    public static boolean isClass(ItemStack s, String cls) {
        Item i = s.getItem();
        switch (cls) {
            case "armor":
                return i instanceof ArmorItem;
            case "shield":
                return i instanceof ShieldItem || canBlock(s);
            case "ranged":
                return i instanceof ProjectileWeaponItem;
            case "hoe":
                return i instanceof HoeItem;
            case "fishing_rod":
                return i instanceof FishingRodItem;
            case "tool":
                return i instanceof DiggerItem && !(i instanceof HoeItem) && !(i instanceof AxeItem);
            case "food":
                return i.isEdible();
            case "weapon":
                if (i instanceof SwordItem || i instanceof AxeItem || i instanceof TridentItem) {
                    return true;
                }
                if (i instanceof DiggerItem || i instanceof ArmorItem || i instanceof ProjectileWeaponItem
                        || i instanceof ShieldItem || i instanceof FishingRodItem) {
                    return false;
                }
                return s.getMaxStackSize() == 1 && attackDamage(s) >= 4.0;
            case "gear":
                return isClass(s, "armor") || isClass(s, "shield") || isClass(s, "ranged") || isClass(s, "weapon");
            default:
                return false;
        }
    }

    private static boolean canBlock(ItemStack s) {
        try {
            return s.canPerformAction(ToolActions.SHIELD_BLOCK);
        } catch (Throwable t) {
            return false;
        }
    }

    /** 장비/도구 가격. 해당 없으면 -1. */
    public static int gear(ItemStack s) {
        Item i = s.getItem();
        double q = quality(s);
        double p;
        if (i instanceof ArmorItem a) {
            double k = 0;
            try {
                k = a.getMaterial().getKnockbackResistance();
            } catch (Throwable ignored) {
            }
            p = Math.max(20, 55.0 * a.getDefense() + 250.0 * a.getToughness() + 1500.0 * k) * q;
        } else if (isClass(s, "shield")) {
            p = 150 * q;
        } else if (i instanceof ProjectileWeaponItem) {
            p = 120 * q;
        } else if (i instanceof FishingRodItem) {
            p = 130 * q;
        } else if (i instanceof HoeItem) {
            p = 90 * q;
        } else if (isClass(s, "tool")) {
            p = 130 * q;
        } else if (isClass(s, "weapon")) {
            double d = attackDamage(s);
            p = Math.max(20, 4.0 * d * d) * q;
        } else {
            return -1;
        }
        return nice(p);
    }

    /** 음식 가격: 2 + 허기×2 + 허기×포화계수×3 (빵 ≈ 21G, 스테이크 ≈ 37G) */
    public static int food(ItemStack s) {
        FoodProperties f = null;
        try {
            f = s.getFoodProperties(null);
        } catch (Throwable ignored) {
        }
        if (f == null) {
            return -1;
        }
        double n = f.getNutrition();
        double p = (2 + n * 2 + n * f.getSaturationModifier() * 3) * rarityFactor(s);
        return nice(p);
    }

    public static int nice(double p) {
        long v;
        if (p < 100) {
            v = Math.round(p);
        } else if (p < 1000) {
            v = Math.round(p / 5.0) * 5;
        } else {
            v = Math.round(p / 50.0) * 50;
        }
        return (int) Math.max(1, Math.min(50_000_000L, v));
    }
}

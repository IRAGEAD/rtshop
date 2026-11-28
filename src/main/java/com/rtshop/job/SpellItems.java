package com.rtshop.job;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Locale;

/**
 * 주문을 쓰는 물건 (3.84, M-34 · M-47 · M-64): 지팡이 · 주문서 · 두루마리.
 * <ul>
 *   <li>지팡이를 들고 쓰는 주문은 제 위력, 들지 않으면 noStaffPower (모든 직업, jobs.json).</li>
 *   <li>마법사가 아니면 주문 위력 nonMagePower, 시전 · 재사용 시간 nonMageCastTime 배 (Jobs.applyAttributes).</li>
 * </ul>
 * 아이템 설명(ClientEvents.onLoreTooltip)이 이 규칙을 알린다.
 */
public final class SpellItems {
    private SpellItems() {
    }

    public static final String STAFF = "staff", BOOK = "book", SCROLL = "scroll";

    /** 지팡이인가: 아이템 이름 · 갈래로 본 지팡이(rules/ItemPolicy.staff)이거나 무기 종류가 지팡이 (마법서 · 블록은 아니다). */
    public static boolean staff(ItemStack s) {
        if (s == null || s.isEmpty() || s.getItem() instanceof BlockItem) return false;
        try {
            if (com.rtshop.rules.ItemPolicy.staff(s.getItem())) return true;
            if (!"staff".equals(JobConfig.get().baseWeaponType(s))) return false;
            String path = path(s.getItem());
            return !path.contains("book") && !path.contains("grimoire") && !path.contains("tome");
        } catch (Throwable t) {
            return false;
        }
    }

    /** 이 물건의 갈래: STAFF · BOOK · SCROLL (주문과 상관없으면 null). */
    public static String kind(ItemStack s) {
        if (s == null || s.isEmpty() || s.getItem() instanceof BlockItem) return null;
        try {
            if (staff(s)) return STAFF;
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(s.getItem());
            if (id == null) return null;
            String ns = id.getNamespace(), path = id.getPath().toLowerCase(Locale.ROOT);
            if (path.contains("spell_book") || path.contains("spellbook") || named(s.getItem(), "spellbook")) return BOOK;
            if (path.contains("scroll") && (ns.contains("spell") || ns.contains("magic") || ns.contains("wizard") || ns.contains("arcan"))) return SCROLL;
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String path(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        return id == null ? "" : id.getPath().toLowerCase(Locale.ROOT);
    }

    /** 아이템의 갈래(또는 그 인터페이스) 이름에 이 낱말이 있는가 (Iron's 의 SpellBook · ISpellbook). */
    private static boolean named(Item item, String word) {
        for (Class<?> k = item.getClass(); k != null && k != Item.class; k = k.getSuperclass()) {
            if (k.getSimpleName().toLowerCase(Locale.ROOT).contains(word)) return true;
            for (Class<?> i : k.getInterfaces()) {
                if (i.getSimpleName().toLowerCase(Locale.ROOT).contains(word)) return true;
            }
        }
        return false;
    }

    /** 0.6 → "60" (설명의 % 숫자). */
    public static String pct(double x) {
        return String.valueOf(Math.round(x * 100));
    }

    /** 1.3 → "1.3", 2.0 → "2" (설명의 배수 숫자). */
    public static String times(double x) {
        double r = Math.round(x * 100) / 100.0;
        return r == Math.rint(r) ? String.valueOf((long) r) : String.valueOf(r);
    }
}

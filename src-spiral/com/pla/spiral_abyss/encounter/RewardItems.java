/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.item.enchantment.EnchantmentHelper
 *  net.minecraft.world.level.ItemLike
 */
package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.encounter.data.FloorDefinition;
import com.pla.spiral_abyss.platform.AbyssRegistries;
import java.util.Map;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ItemLike;

public final class RewardItems {
    private RewardItems() {
    }

    public static ItemStack stack(FloorDefinition.RewardEntry entry, int count) {
        if (!entry.item() || !AbyssRegistries.ITEMS.containsKey(entry.id())) {
            return ItemStack.f_41583_;
        }
        Item item = AbyssRegistries.ITEMS.getValue(entry.id());
        if (item == null || item == Items.f_41852_ || count <= 0) {
            return ItemStack.f_41583_;
        }
        ListTag enchantments = new ListTag();
        entry.enchantments().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(configured -> {
            if (!AbyssRegistries.ENCHANTMENTS.containsKey((ResourceLocation)configured.getKey())) {
                return;
            }
            enchantments.add((Object)EnchantmentHelper.m_182443_((ResourceLocation)((ResourceLocation)configured.getKey()), (int)((Integer)configured.getValue())));
        });
        if (item == Items.f_42517_ && !enchantments.isEmpty()) {
            item = Items.f_42690_;
        }
        ItemStack stack = new ItemStack((ItemLike)item, count);
        if (!enchantments.isEmpty()) {
            stack.m_41700_(item == Items.f_42690_ ? "StoredEnchantments" : "Enchantments", (Tag)enchantments);
        }
        return stack;
    }
}


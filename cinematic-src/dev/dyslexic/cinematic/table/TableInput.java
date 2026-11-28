/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.NonNullList
 *  net.minecraft.world.ContainerHelper
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.entity.player.StackedContents
 *  net.minecraft.world.inventory.CraftingContainer
 *  net.minecraft.world.item.ItemStack
 */
package dev.dyslexic.cinematic.table;

import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;

public final class TableInput
implements CraftingContainer {
    private final NonNullList<ItemStack> items = NonNullList.m_122780_((int)9, (Object)ItemStack.f_41583_);

    public TableInput(List<ItemStack> recipeOrder) {
        for (int i = 0; i < 9 && i < recipeOrder.size(); ++i) {
            this.items.set(i, (Object)recipeOrder.get(i));
        }
    }

    public int m_39347_() {
        return 3;
    }

    public int m_39346_() {
        return 3;
    }

    public List<ItemStack> m_280657_() {
        return List.copyOf(this.items);
    }

    public int m_6643_() {
        return this.items.size();
    }

    public boolean m_7983_() {
        for (ItemStack stack : this.items) {
            if (stack.m_41619_()) continue;
            return false;
        }
        return true;
    }

    public ItemStack m_8020_(int slot) {
        return slot >= 0 && slot < this.items.size() ? (ItemStack)this.items.get(slot) : ItemStack.f_41583_;
    }

    public ItemStack m_8016_(int slot) {
        return ContainerHelper.m_18966_(this.items, (int)slot);
    }

    public ItemStack m_7407_(int slot, int amount) {
        return ContainerHelper.m_18969_(this.items, (int)slot, (int)amount);
    }

    public void m_6836_(int slot, ItemStack stack) {
        this.items.set(slot, (Object)stack);
    }

    public void m_6596_() {
    }

    public boolean m_6542_(Player player) {
        return true;
    }

    public void m_6211_() {
        this.items.clear();
    }

    public void m_5809_(StackedContents contents) {
        for (ItemStack stack : this.items) {
            contents.m_36466_(stack);
        }
    }
}


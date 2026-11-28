/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.chat.Component
 *  net.minecraft.world.item.Equipable
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.TooltipFlag
 *  net.minecraft.world.level.Level
 */
package com.finderfeed.fdbosses.content.items;

import com.finderfeed.fdbosses.content.data_components.ItemCoreDataComponent;
import com.finderfeed.fdbosses.content.items.CoreItem;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class ArmorCoreItem
extends CoreItem {
    public ArmorCoreItem(Item.Properties props, ItemCoreDataComponent.CoreType coreType, boolean glint, Supplier<Component> abilityText) {
        super(props, coreType, glint, abilityText);
    }

    @Override
    public void m_7373_(ItemStack stack, Level p_339594_, List<Component> components, TooltipFlag p_41424_) {
        super.m_7373_(stack, p_339594_, components, p_41424_);
    }

    @Override
    public boolean canBeAppliedTo(ItemStack itemStack) {
        Item item = itemStack.m_41720_();
        return item instanceof Equipable;
    }
}


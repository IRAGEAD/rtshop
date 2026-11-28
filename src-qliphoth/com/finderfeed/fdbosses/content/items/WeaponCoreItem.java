/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.chat.Component
 *  net.minecraft.world.entity.ai.attributes.Attributes
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.ItemStack
 */
package com.finderfeed.fdbosses.content.items;

import com.finderfeed.fdbosses.BossUtil;
import com.finderfeed.fdbosses.content.data_components.ItemCoreDataComponent;
import com.finderfeed.fdbosses.content.items.CoreItem;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class WeaponCoreItem
extends CoreItem {
    public WeaponCoreItem(Item.Properties props, ItemCoreDataComponent.CoreType coreType, boolean glint, Supplier<Component> abilityText) {
        super(props, coreType, glint, abilityText);
    }

    @Override
    public boolean canBeAppliedTo(ItemStack itemStack) {
        return BossUtil.itemContainsModifierForAttribute(itemStack, Attributes.f_22281_);
    }
}


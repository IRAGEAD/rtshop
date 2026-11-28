/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.TooltipFlag
 *  net.minecraft.world.level.Level
 *  org.jetbrains.annotations.Nullable
 */
package com.finderfeed.fdbosses.content.items;

import com.finderfeed.fdbosses.content.data_components.ItemCoreDataComponent;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public abstract class CoreItem
extends Item {
    public static final String CORE_TAG_NAME = "weapon_core";
    public final ItemCoreDataComponent.CoreType coreType;
    private boolean hasGlint;
    protected Supplier<Component> abilityText;

    public CoreItem(Item.Properties props, ItemCoreDataComponent.CoreType coreType, boolean glint, Supplier<Component> abilityText) {
        super(props);
        this.coreType = coreType;
        this.hasGlint = glint;
        this.abilityText = abilityText;
    }

    public static void setItemCore(ItemCoreDataComponent.CoreType coreType, ItemStack item) {
        CompoundTag tag = CoreItem.getItemTag(item);
        tag.m_128359_(CORE_TAG_NAME, coreType.name());
    }

    public static boolean hasCore(ItemStack item) {
        return CoreItem.getItemCore(item) != null;
    }

    public static ItemCoreDataComponent.CoreType getItemCore(ItemStack itemStack) {
        CompoundTag tag = CoreItem.getItemTagNoCreate(itemStack);
        if (tag == null) {
            return null;
        }
        if (tag.m_128441_(CORE_TAG_NAME)) {
            return ItemCoreDataComponent.CoreType.valueOf(tag.m_128461_(CORE_TAG_NAME));
        }
        return null;
    }

    public static CompoundTag getItemTagNoCreate(ItemStack itemStack) {
        return itemStack.m_41737_("fdbosses");
    }

    public static CompoundTag getItemTag(ItemStack itemStack) {
        return itemStack.m_41698_("fdbosses");
    }

    public boolean m_5812_(ItemStack p_41453_) {
        return this.hasGlint;
    }

    public void m_7373_(ItemStack p_41421_, @Nullable Level p_41422_, List<Component> components, TooltipFlag p_41424_) {
        super.m_7373_(p_41421_, p_41422_, components, p_41424_);
        components.add(this.abilityText.get());
        components.add((Component)Component.m_237115_((String)"fdbosses.word.core_lose").m_130940_(ChatFormatting.RED));
    }

    public abstract boolean canBeAppliedTo(ItemStack var1);
}


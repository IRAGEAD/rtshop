/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.util.Pair
 *  net.minecraft.world.item.ItemStack
 */
package yesman.epicfight.world.capabilities.item;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import yesman.epicfight.data.conditions.Condition;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

public class TagBasedSeparativeCapability
extends CapabilityItem {
    private final List<Pair<Condition<ItemStack>, CapabilityItem>> variations;
    private final CapabilityItem defaultCapability;

    public TagBasedSeparativeCapability(List<Pair<Condition<ItemStack>, CapabilityItem>> variations, CapabilityItem defaultCapability) {
        super(CapabilityItem.builder().category(CapabilityItem.WeaponCategories.NOT_WEAPON));
        this.variations = variations;
        this.defaultCapability = defaultCapability;
    }

    @Override
    public CapabilityItem getResult(ItemStack itemstack) {
        for (Pair<Condition<ItemStack>, CapabilityItem> pair : this.variations) {
            if (!((Condition)pair.getFirst()).predicate(itemstack)) continue;
            return ((CapabilityItem)pair.getSecond()).getResult(itemstack);
        }
        return this.defaultCapability;
    }
}


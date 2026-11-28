/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 */
package dev.dyslexic.cinematic;

import java.util.Objects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class Compat {
    private Compat() {
    }

    public static int hashItem(ItemStack stack) {
        return stack.m_41619_() ? 0 : 31 * Item.m_41393_((Item)stack.m_41720_()) + Objects.hashCode(stack.m_41783_());
    }
}


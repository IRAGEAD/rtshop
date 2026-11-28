/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.item.ItemStack
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.encounter.MobTickContext;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={Inventory.class})
public abstract class AbyssPlayerInventoryMixin {
    @Inject(method={"setItem"}, at={@At(value="HEAD")}, cancellable=true)
    private void spiral_abyss$protectInventory(int slot, ItemStack stack, CallbackInfo callback) {
        if (MobTickContext.protectsInventory()) {
            callback.cancel();
        }
    }
}


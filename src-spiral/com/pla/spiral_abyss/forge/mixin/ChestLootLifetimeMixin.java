/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.item.ItemEntity
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.pla.spiral_abyss.forge.mixin;

import com.pla.spiral_abyss.entity.ChestLootEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ChestLootEntity.class})
public abstract class ChestLootLifetimeMixin
extends ItemEntity {
    protected ChestLootLifetimeMixin(EntityType<? extends ItemEntity> type, Level level) {
        super(type, level);
    }

    @Inject(method={"<init>(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;Z)V"}, at={@At(value="RETURN")}, remap=false)
    private void spiralAbyss$lifespan(Level level, double x, double y, double z, ItemStack stack, boolean rare, CallbackInfo ci) {
        this.lifespan = stack.getEntityLifespan(level);
    }
}


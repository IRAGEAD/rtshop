/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.item.ItemEntity
 *  net.minecraft.world.item.ItemStack
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.Redirect
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.encounter.MobTickContext;
import com.pla.spiral_abyss.event.AbyssMobDrops;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={Entity.class})
public abstract class EntityDropMixin {
    @Redirect(method={"discard"}, at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/Entity;remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V"))
    private void spiralAbyss$scopeRemoval(Entity entity, Entity.RemovalReason reason) {
        MobTickContext.tick(entity, () -> entity.m_142687_(reason));
    }

    @Inject(method={"spawnAtLocation(Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;"}, at={@At(value="HEAD")}, cancellable=true)
    private void spiralAbyss$preventMobItemDrop(ItemStack stack, float offset, CallbackInfoReturnable<ItemEntity> callback) {
        if (AbyssMobDrops.suppress((Entity)this)) {
            callback.setReturnValue(null);
        }
    }
}


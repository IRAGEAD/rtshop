/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.damagesource.DamageSource
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.Redirect
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.encounter.MobTickContext;
import com.pla.spiral_abyss.event.AbyssMobDrops;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={LivingEntity.class})
public abstract class LivingEntityDropMixin {
    @Redirect(method={"hurt"}, at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/LivingEntity;die(Lnet/minecraft/world/damagesource/DamageSource;)V"))
    private void spiralAbyss$scopeScriptedDeath(LivingEntity entity, DamageSource source) {
        MobTickContext.tick((Entity)entity, () -> entity.m_6667_(source));
    }

    @Inject(method={"dropAllDeathLoot"}, at={@At(value="HEAD")}, cancellable=true)
    private void spiralAbyss$preventMobDeathLoot(DamageSource source, CallbackInfo callback) {
        if (AbyssMobDrops.suppress((Entity)this)) {
            callback.cancel();
        }
    }
}


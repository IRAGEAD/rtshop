/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.wrapoperation.Operation
 *  com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.entity.Entity
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 */
package com.pla.spiral_abyss.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.pla.spiral_abyss.encounter.MobTickContext;
import com.pla.spiral_abyss.world.AbyssDimension;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value={ServerLevel.class})
public abstract class AbyssMobTickMixin {
    @WrapOperation(method={"tickNonPassenger"}, at={@At(value="INVOKE", target="Lnet/minecraft/world/entity/Entity;tick()V")})
    private void spiral_abyss$tick(Entity entity, Operation<Void> original) {
        if (entity.m_9236_().m_46472_().equals(AbyssDimension.LEVEL)) {
            MobTickContext.tick(entity, () -> original.call(new Object[]{entity}));
        } else {
            original.call(new Object[]{entity});
        }
    }

    @WrapOperation(method={"tickPassenger"}, at={@At(value="INVOKE", target="Lnet/minecraft/world/entity/Entity;rideTick()V")})
    private void spiral_abyss$rideTick(Entity entity, Operation<Void> original) {
        if (entity.m_9236_().m_46472_().equals(AbyssDimension.LEVEL)) {
            MobTickContext.tick(entity, () -> original.call(new Object[]{entity}));
        } else {
            original.call(new Object[]{entity});
        }
    }
}


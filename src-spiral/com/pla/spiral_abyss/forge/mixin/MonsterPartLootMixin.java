/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
 *  com.llamalad7.mixinextras.injector.wrapoperation.Operation
 *  net.minecraft.world.damagesource.DamageSource
 *  net.minecraft.world.entity.Entity
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Pseudo
 */
package com.pla.spiral_abyss.forge.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.pla.spiral_abyss.encounter.MobTickContext;
import com.pla.spiral_abyss.world.AbyssDimension;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets={"net.saksolm.monsterexpansion.entity.custom.AbstractLargeMonster"}, remap=false)
public abstract class MonsterPartLootMixin {
    @WrapMethod(method={"applyDamageToPart"})
    private boolean spiral_abyss$partLoot(String partName, float amount, DamageSource source, Operation<Boolean> original) {
        Entity entity = (Entity)this;
        if (!entity.m_9236_().m_46472_().equals(AbyssDimension.LEVEL)) {
            return (Boolean)original.call(new Object[]{partName, Float.valueOf(amount), source});
        }
        boolean[] result = new boolean[1];
        MobTickContext.tick(entity, () -> {
            result[0] = (Boolean)original.call(new Object[]{partName, Float.valueOf(amount), source});
        });
        return result[0];
    }
}


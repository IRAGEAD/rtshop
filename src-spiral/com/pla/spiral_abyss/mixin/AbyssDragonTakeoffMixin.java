/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.entity.boss.enderdragon.EnderDragon
 *  net.minecraft.world.entity.boss.enderdragon.phases.DragonTakeoffPhase
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Redirect
 */
package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.encounter.DragonArena;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonTakeoffPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value={DragonTakeoffPhase.class})
public abstract class AbyssDragonTakeoffMixin {
    @Redirect(method={"findNewTarget"}, at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;findClosestNode(DDD)I"))
    private int spiralAbyss$localTakeoff(EnderDragon dragon, double x, double y, double z) {
        if (!DragonArena.managed(dragon)) {
            return dragon.m_31170_(x, y, z);
        }
        BlockPos origin = dragon.m_287165_();
        return dragon.m_31170_((double)origin.m_123341_() + x, (double)(origin.m_123342_() + 40), (double)origin.m_123343_() + z);
    }
}


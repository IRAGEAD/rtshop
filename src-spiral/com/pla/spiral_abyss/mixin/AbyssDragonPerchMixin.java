/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.entity.boss.enderdragon.EnderDragon
 *  net.minecraft.world.entity.boss.enderdragon.phases.AbstractDragonPhaseInstance
 *  net.minecraft.world.entity.boss.enderdragon.phases.DragonLandingPhase
 *  net.minecraft.world.phys.Vec3
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.encounter.DragonArena;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.AbstractDragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonLandingPhase;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={DragonLandingPhase.class})
public abstract class AbyssDragonPerchMixin
extends AbstractDragonPhaseInstance {
    @Shadow
    private Vec3 f_31303_;

    protected AbyssDragonPerchMixin(EnderDragon dragon) {
        super(dragon);
    }

    @Inject(method={"doServerTick"}, at={@At(value="HEAD")})
    private void spiralAbyss$terrainSurface(CallbackInfo ci) {
        if (DragonArena.managed(this.f_31176_)) {
            this.f_31303_ = DragonArena.landingTarget(this.f_31176_);
        }
    }
}


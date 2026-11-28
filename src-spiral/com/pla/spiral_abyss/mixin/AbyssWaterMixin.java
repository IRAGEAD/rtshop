/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.tags.FluidTags
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.material.FlowingFluid
 *  net.minecraft.world.level.material.FluidState
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.world.AbyssDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={FlowingFluid.class})
public abstract class AbyssWaterMixin {
    @Inject(method={"tick"}, at={@At(value="HEAD")}, cancellable=true)
    private void spiralAbyss$stillWater(Level level, BlockPos pos, FluidState state, CallbackInfo callback) {
        if (level.m_46472_().equals(AbyssDimension.LEVEL) && state.m_205070_(FluidTags.f_13131_)) {
            callback.cancel();
        }
    }
}


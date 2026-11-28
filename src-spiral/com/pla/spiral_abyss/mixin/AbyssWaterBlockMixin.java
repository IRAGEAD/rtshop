/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.tags.FluidTags
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.LiquidBlock
 *  net.minecraft.world.level.block.state.BlockState
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
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={LiquidBlock.class})
public abstract class AbyssWaterBlockMixin {
    @Inject(method={"onPlace"}, at={@At(value="HEAD")}, cancellable=true)
    private void spiralAbyss$staticWater(BlockState state, Level level, BlockPos pos, BlockState previous, boolean moving, CallbackInfo callback) {
        if (level.m_46472_().equals(AbyssDimension.LEVEL) && state.m_60819_().m_205070_(FluidTags.f_13131_)) {
            callback.cancel();
        }
    }
}


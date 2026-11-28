/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.entity.item.FallingBlockEntity
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.state.BlockState
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.event.AbyssMobDrops;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={FallingBlockEntity.class})
public abstract class AbyssFallingBlockMixin {
    @Inject(method={"fall"}, at={@At(value="HEAD")}, cancellable=true)
    private static void spiral_abyss$suppressBossFallingBlock(Level level, BlockPos pos, BlockState state, CallbackInfoReturnable<FallingBlockEntity> callback) {
        if (AbyssMobDrops.suppressFallingBlock(level, pos, state)) {
            callback.setReturnValue(null);
        }
    }
}


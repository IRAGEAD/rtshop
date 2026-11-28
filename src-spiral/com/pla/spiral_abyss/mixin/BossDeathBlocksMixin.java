/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.state.BlockState
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.event.BossDeathBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={Level.class})
public abstract class BossDeathBlocksMixin {
    @Inject(method={"destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z"}, at={@At(value="HEAD")}, cancellable=true)
    private void spiral_abyss$preventDirectGriefing(BlockPos pos, boolean drops, Entity source, int recursion, CallbackInfoReturnable<Boolean> callback) {
        if (BossDeathBlocks.suppressMobChange((Level)this)) {
            callback.setReturnValue((Object)false);
        }
    }

    @Inject(method={"setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z"}, at={@At(value="HEAD")}, cancellable=true)
    private void spiral_abyss$suppressDeathStructure(BlockPos pos, BlockState state, int flags, int recursion, CallbackInfoReturnable<Boolean> callback) {
        if (BossDeathBlocks.suppress((Level)this, pos, state)) {
            callback.setReturnValue((Object)false);
        }
    }
}


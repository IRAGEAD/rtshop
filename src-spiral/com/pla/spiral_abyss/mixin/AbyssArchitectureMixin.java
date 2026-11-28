/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.chunk.LevelChunk
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.event.AbyssArchitectureRepair;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={LevelChunk.class})
public abstract class AbyssArchitectureMixin {
    @Inject(method={"setBlockState"}, at={@At(value="RETURN")})
    private void spiral_abyss$queueRepair(BlockPos pos, BlockState state, boolean moving, CallbackInfoReturnable<BlockState> callback) {
        AbyssArchitectureRepair.removed(((LevelChunk)this).m_62953_(), pos, (BlockState)callback.getReturnValue(), state);
    }
}


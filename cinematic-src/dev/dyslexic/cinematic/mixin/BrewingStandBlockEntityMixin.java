/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BaseContainerBlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.entity.BrewingStandBlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package dev.dyslexic.cinematic.mixin;

import dev.dyslexic.cinematic.brewing.BrewingWatch;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={BrewingStandBlockEntity.class})
public abstract class BrewingStandBlockEntityMixin
extends BaseContainerBlockEntity
implements BrewingWatch.Fingerprinted {
    @Unique
    private int cinematic$fingerprint;

    protected BrewingStandBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public int cinematic$fingerprint() {
        return this.cinematic$fingerprint;
    }

    @Override
    public void cinematic$setFingerprint(int fingerprint) {
        this.cinematic$fingerprint = fingerprint;
    }

    @Inject(method={"serverTick"}, at={@At(value="RETURN")})
    private static void cinematic$watch(Level level, BlockPos pos, BlockState state, BrewingStandBlockEntity stand, CallbackInfo ci) {
        BrewingWatch.afterTick(level, pos, state, stand);
    }
}


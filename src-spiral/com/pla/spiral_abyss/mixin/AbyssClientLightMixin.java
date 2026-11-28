/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.SectionPos
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.chunk.DataLayer
 *  net.minecraft.world.level.chunk.LightChunkGetter
 *  net.minecraft.world.level.lighting.LightEngine
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.world.AbyssDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={LightEngine.class})
public abstract class AbyssClientLightMixin {
    @Shadow
    @Final
    protected LightChunkGetter f_283884_;

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    @Unique
    private boolean spiralAbyss$fullAmbientLight() {
        BlockGetter blockGetter = this.f_283884_.m_7653_();
        if (!(blockGetter instanceof Level)) return false;
        Level level = (Level)blockGetter;
        if (!level.f_46443_) return false;
        if (!level.m_46472_().equals(AbyssDimension.LEVEL)) return false;
        return true;
    }

    @Inject(method={"getLightValue"}, at={@At(value="HEAD")}, cancellable=true)
    private void spiralAbyss$ambientLight(BlockPos pos, CallbackInfoReturnable<Integer> callback) {
        if (this.spiralAbyss$fullAmbientLight()) {
            callback.setReturnValue((Object)15);
        }
    }

    @Inject(method={"getDataLayerData"}, at={@At(value="HEAD")}, cancellable=true)
    private void spiralAbyss$ambientSection(SectionPos section, CallbackInfoReturnable<DataLayer> callback) {
        if (this.spiralAbyss$fullAmbientLight()) {
            callback.setReturnValue((Object)new DataLayer(15));
        }
    }
}


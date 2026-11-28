/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.world.AbyssDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ServerLevel.class})
public abstract class AbyssEnvironmentMixin {
    @Inject(method={"globalLevelEvent"}, at={@At(value="HEAD")}, cancellable=true)
    private void spiralAbyss$localBossSound(int event, BlockPos position, int data, CallbackInfo callback) {
        ServerLevel level = (ServerLevel)this;
        if (!level.m_46472_().equals(AbyssDimension.LEVEL)) {
            return;
        }
        level.m_5898_(null, event, position, data);
        callback.cancel();
    }

    @Inject(method={"advanceWeatherCycle"}, at={@At(value="HEAD")}, cancellable=true)
    private void spiralAbyss$clearWeather(CallbackInfo callback) {
        ServerLevel level = (ServerLevel)this;
        if (!level.m_46472_().equals(AbyssDimension.LEVEL)) {
            return;
        }
        level.m_46734_(0.0f);
        level.m_46707_(0.0f);
        callback.cancel();
    }

    @Inject(method={"tickCustomSpawners"}, at={@At(value="HEAD")}, cancellable=true)
    private void spiralAbyss$noAmbientSpawners(boolean enemies, boolean friendlies, CallbackInfo callback) {
        if (((ServerLevel)this).m_46472_().equals(AbyssDimension.LEVEL)) {
            callback.cancel();
        }
    }
}


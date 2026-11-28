/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.server.level.ServerBossEvent
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.encounter.BossHealthBars;
import net.minecraft.server.level.ServerBossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ServerBossEvent.class})
public abstract class ServerBossEventMixin {
    @Inject(method={"<init>"}, at={@At(value="RETURN")})
    private void spiralAbyss$registerBossBar(CallbackInfo ci) {
        BossHealthBars.registered((ServerBossEvent)this);
    }
}


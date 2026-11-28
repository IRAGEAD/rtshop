/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.player.Input
 *  net.minecraft.client.player.KeyboardInput
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.finderfeed.fdbosses.mixin;

import com.finderfeed.fdbosses.BossClientEvents;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={KeyboardInput.class})
public class InputMixin {
    @Inject(method={"tick"}, at={@At(value="TAIL")})
    public void tick(boolean p_234118_, float p_234119_, CallbackInfo ci) {
        Input input = (Input)this;
        if (BossClientEvents.hasPlayerJumpedUnderSinEffect) {
            input.f_108572_ = false;
        }
    }
}


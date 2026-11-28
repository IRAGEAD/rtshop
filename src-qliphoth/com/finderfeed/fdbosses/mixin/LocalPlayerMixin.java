/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.player.Input
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.world.entity.player.Player
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.At$Shift
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.finderfeed.fdbosses.mixin;

import com.finderfeed.fdbosses.init.BossItems;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={LocalPlayer.class})
public abstract class LocalPlayerMixin {
    @Inject(method={"aiStep"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/player/LocalPlayer;canStartSprinting()Z", shift=At.Shift.BEFORE)})
    public void aiStep(CallbackInfo ci) {
        Player player = (Player)this;
        if (player.m_21211_().m_150930_(BossItems.PHASE_SPHERE.get())) {
            Input input = ((LocalPlayer)player).f_108618_;
            input.f_108566_ /= 0.2f;
            input.f_108567_ /= 0.2f;
        }
    }
}


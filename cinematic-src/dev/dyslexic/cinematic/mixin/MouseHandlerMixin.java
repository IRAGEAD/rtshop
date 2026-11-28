/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.MouseHandler
 *  net.minecraft.util.SmoothDouble
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package dev.dyslexic.cinematic.mixin;

import dev.dyslexic.cinematic.client.Scene;
import net.minecraft.client.MouseHandler;
import net.minecraft.util.SmoothDouble;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={MouseHandler.class})
public abstract class MouseHandlerMixin {
    @Shadow
    private double f_91516_;
    @Shadow
    private double f_91517_;
    @Shadow
    @Final
    private SmoothDouble f_91514_;
    @Shadow
    @Final
    private SmoothDouble f_91515_;

    @Inject(method={"turnPlayer"}, at={@At(value="HEAD")})
    private void cinematic$sceneHoldsTheView(CallbackInfo ci) {
        if (Scene.active()) {
            this.f_91516_ = 0.0;
            this.f_91517_ = 0.0;
            this.f_91514_.m_14236_();
            this.f_91515_.m_14236_();
        }
    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.entity.ExperienceOrbRenderer
 *  net.minecraft.world.entity.ExperienceOrb
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package dev.dyslexic.cinematic.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.client.Scene;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ExperienceOrbRenderer;
import net.minecraft.world.entity.ExperienceOrb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ExperienceOrbRenderer.class})
public abstract class ExperienceOrbRendererMixin {
    private static final double HIDE_WITHIN_SQR = 1.0;

    @Inject(method={"render(Lnet/minecraft/world/entity/ExperienceOrb;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"}, at={@At(value="HEAD")}, cancellable=true)
    private void cinematic$notInTheLens(ExperienceOrb orb, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (Scene.active() && orb.m_20318_(partialTicks).m_82557_(Minecraft.m_91087_().f_91063_.m_109153_().m_90583_()) < 1.0) {
            ci.cancel();
        }
    }
}


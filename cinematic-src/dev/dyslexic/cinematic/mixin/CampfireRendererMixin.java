/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.blockentity.CampfireRenderer
 *  net.minecraft.world.level.block.entity.CampfireBlockEntity
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package dev.dyslexic.cinematic.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.client.StationDisplayLayouts;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.CampfireRenderer;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={CampfireRenderer.class})
public abstract class CampfireRendererMixin {
    @Inject(method={"render(Lnet/minecraft/world/level/block/entity/CampfireBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V"}, at={@At(value="HEAD")}, cancellable=true)
    private void cinematic$layout(CampfireBlockEntity fire, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay, CallbackInfo ci) {
        if (StationDisplayLayouts.campfire(fire, pose, buffers, light)) {
            ci.cancel();
        }
    }
}


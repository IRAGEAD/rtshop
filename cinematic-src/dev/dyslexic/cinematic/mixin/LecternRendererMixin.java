/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.blockentity.LecternRenderer
 *  net.minecraft.world.level.block.entity.LecternBlockEntity
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package dev.dyslexic.cinematic.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.client.LecternStation;
import dev.dyslexic.cinematic.client.StationDisplayLayouts;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.LecternRenderer;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={LecternRenderer.class})
public abstract class LecternRendererMixin {
    @Inject(method={"render(Lnet/minecraft/world/level/block/entity/LecternBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V"}, at={@At(value="HEAD")}, cancellable=true)
    private void cinematic$readBook(LecternBlockEntity lectern, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay, CallbackInfo ci) {
        if (LecternStation.drawsBookAt(lectern.m_58899_())) {
            ci.cancel();
        } else if (StationDisplayLayouts.customLectern()) {
            pose.m_85836_();
            StationDisplayLayouts.lectern(lectern, pose);
        }
    }

    @Inject(method={"render(Lnet/minecraft/world/level/block/entity/LecternBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V"}, at={@At(value="RETURN")})
    private void cinematic$restoreLayout(LecternBlockEntity lectern, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay, CallbackInfo ci) {
        if (StationDisplayLayouts.customLectern()) {
            pose.m_85849_();
        }
    }
}


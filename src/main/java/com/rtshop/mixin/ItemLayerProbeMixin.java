package com.rtshop.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 점검용: 바닐라의 손 아이템 층(ItemInHandLayer)이 Epic Fight 와 겹쳐서도 그리는지. */
@Mixin(value = ItemInHandLayer.class, remap = false)
public abstract class ItemLayerProbeMixin {
    @Inject(method = "*(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), remap = false, require = 0)
    private void rtshop$probe(LivingEntity e, ItemStack s, ItemDisplayContext ctx, HumanoidArm arm, PoseStack ps, MultiBufferSource buf, int light, CallbackInfo ci) {
        if (!com.rtshop.client.RenderProbe.on || s.isEmpty() || !(e instanceof net.minecraft.world.entity.player.Player)) return;
        com.rtshop.client.RenderProbe.hit("vanilla-layer:" + ((Object) this).getClass().getSimpleName() + ":" + net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(s.getItem()));
    }
}

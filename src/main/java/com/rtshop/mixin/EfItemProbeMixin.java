package com.rtshop.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 점검용: Epic Fight / Avalon 의 손 아이템 렌더러가 실제로 불리는지 (RenderProbe 가 켜져 있을 때만 센다). */
@Pseudo
@Mixin(targets = {"yesman.epicfight.client.renderer.patched.item.RenderItemBase",
        "com.merlin204.avalon.entity.client.renderer.patch.item.RenderMeshItem",
        "com.merlin204.avalon.entity.client.renderer.patch.item.AbstractRenderAnimationItem"}, remap = false)
public abstract class EfItemProbeMixin {
    @Inject(method = "renderItemInHand", at = @At("HEAD"), remap = false, require = 0)
    private void rtshop$probe(CallbackInfo ci) {
        com.rtshop.client.RenderProbe.hit("ef:" + ((Object) this).getClass().getSimpleName());
    }
}

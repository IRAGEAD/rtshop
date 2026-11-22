package com.rtshop.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.LinkAnimation;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * Epic Fight Nightfall 3.4.2 의 스킬 크래시 방지 (단검 고유 스킬 등):
 * 무기 공격 속도를 계산할 때, 이어질 동작이 아직 정해지지 않은 연결 동작(LinkAnimation)의 속도를 물으면
 * Epic Fight 20.14 에서 NullPointerException 으로 서버가 멈췄다. 그 경우에는 기본 속도(1배)를 돌려준다.
 * Nightfall 이 없으면 아무것도 하지 않는다.
 */
@Pseudo
@Mixin(targets = "com.hm.efn.gameasset.EFNAnimations", remap = false)
public abstract class NightfallSpeedMixin {
    @Inject(method = "calculateWeaponSpeedWithCap", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void rtshop$safeLinkSpeed(DynamicAnimation anim, LivingEntityPatch<?> patch, float a, float b, CallbackInfoReturnable<Float> cir) {
        try {
            if (anim != null && anim.isLinkAnimation() && anim instanceof LinkAnimation link && link.getNextAnimation() == null) {
                cir.setReturnValue(1.0F);
            }
        } catch (Throwable t) {
            cir.setReturnValue(1.0F);
        }
    }
}

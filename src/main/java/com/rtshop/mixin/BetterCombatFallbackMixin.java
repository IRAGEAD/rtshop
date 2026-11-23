package com.rtshop.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Better Combat 이 이름만 보고 Epic Fight 계열 무기에 붙인 속성(claymore·spear 등)을 다시 뗀다.
 * (그 속성이 붙으면 Better Combat 이 든 모습을 가져가 Epic Fight 의 3D 무기 모델 대신 납작한 아이템 그림이 그려졌다.)
 * 설정 파일 수정(BetterCombatEpicFight)이 그 모드가 설정을 읽은 뒤에 일어나도 바로 맞도록 한 번 더 처리한다.
 */
@Pseudo
@Mixin(targets = {"net.bettercombat.logic.WeaponAttributesFallback", "net.bettercombat.logic.WeaponRegistry"}, remap = false)
public abstract class BetterCombatFallbackMixin {
    @Inject(method = {"initialize", "loadAttributes"}, at = @At("TAIL"), remap = false, require = 0)
    private static void rtshop$dropEpicFight(CallbackInfo ci) {
        com.rtshop.compat.BetterCombatEpicFight.dropFromRegistry();
    }
}

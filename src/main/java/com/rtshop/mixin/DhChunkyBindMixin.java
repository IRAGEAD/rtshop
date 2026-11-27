package com.rtshop.mixin;

import com.rtshop.compat.ChunkyReady;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * DhChunkyMixin 의 뒷받침 (3.70.0): DH 가 설정 함수의 이름을 바꿔 앞의 막이가 붙지 않아도,
 * Chunky 가 아직이면 연결 자체(bindOnGenerationProgressEvent)를 건너뛰어 튕기지 않게 한다.
 */
@Pseudo
@Mixin(targets = "com.seibel.distanthorizons.forge.wrappers.modAccessor.ChunkyAccessor", remap = false)
public abstract class DhChunkyBindMixin {
    @Inject(method = "bindOnGenerationProgressEvent", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void rtshop$skipUntilChunky(CallbackInfo ci) {
        if (ChunkyReady.now()) return;
        ChunkyReady.deferred("bind");
        ci.cancel();
    }
}

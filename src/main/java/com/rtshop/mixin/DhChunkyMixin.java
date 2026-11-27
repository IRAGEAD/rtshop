package com.rtshop.mixin;

import com.rtshop.compat.ChunkyReady;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Distant Horizons 3.3.3 + Chunky 크래시 방지 (3.70.0):
 * DH 는 청크를 처음 불러올 때 tryRunFirstTimeSetup 에서 "연결했음" 표시를 먼저 하고 Chunky 의 진행 알림에 연결하는데,
 * 세계를 여는 동안(시작 지점 청크)에는 Chunky 가 아직 등록 전이라 IllegalStateException("Chunky is not loaded") 으로 튕겼다
 * ("Exception during promotion of chunk to FULL status"). 모드팩의 dhcompat 이 막던 문제를 RT Shop 이 대신 막아,
 * 성능을 깎는 dhcompat(ImmediatelyFast 빠른 버퍼 업로드 끄기 · 2초마다 모든 스레드 훑기 등)을 뺄 수 있게 한다.
 * Chunky 가 아직이면 이번 설정을 건너뛰고("연결했음" 표시도 하지 않아) 다음 청크 때 다시 시도한다.
 */
@Pseudo
@Mixin(targets = "com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.AbstractChunkyAccessor", remap = false)
public abstract class DhChunkyMixin {
    @Inject(method = "tryRunFirstTimeSetup", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void rtshop$waitForChunky(CallbackInfo ci) {
        if (ChunkyReady.now()) return;
        ChunkyReady.deferred("setup");
        ci.cancel();
    }
}

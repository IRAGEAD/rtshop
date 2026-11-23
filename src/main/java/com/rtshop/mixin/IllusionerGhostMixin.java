package com.rtshop.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.reflect.Method;

/**
 * 환술사 모드(The Illusioner)의 기믹 우민(유령 변명자·약탈자·파괴수)이 셰이더(Oculus)를 켜면 보이지 않던 문제:
 * 셰이더를 쓰면 유령 몸을 '셰이더가 끝난 뒤' 그리려고 모아 두는데, Forge 의 Oculus 에서는 그 순간이 오지 않아 영영 그려지지 않았다.
 * 그림자 단계만 건너뛰고 나머지는 셰이더가 없을 때처럼 바로 그리게 한다.
 */
@Pseudo
@Mixin(targets = "com.fqkeh.illusioner.client.render.GhostGlass", remap = false)
public abstract class IllusionerGhostMixin {
    private static Method rtshop$shadow;
    private static boolean rtshop$looked;

    /** 셰이더의 그림자 단계인가 (그때만 '셰이더 사용 중'으로 답해 건너뛰게 한다). */
    private static boolean rtshop$shadowPass() {
        if (!rtshop$looked) {
            rtshop$looked = true;
            try {
                rtshop$shadow = Class.forName("com.fqkeh.illusioner.client.compat.IrisCompat").getMethod("renderingShadowPass");
            } catch (Throwable ignored) {
            }
        }
        try {
            return rtshop$shadow != null && Boolean.TRUE.equals(rtshop$shadow.invoke(null));
        } catch (Throwable t) {
            return false;
        }
    }

    @Redirect(method = "draw(Lcom/fqkeh/illusioner/client/render/GhostGlass$Textures;Lcom/fqkeh/illusioner/client/render/legacy/CustomDrawRenderType$Mesh;)V",
            at = @At(value = "INVOKE", target = "Lcom/fqkeh/illusioner/client/compat/IrisCompat;packInUse()Z"), remap = false, require = 0)
    private static boolean rtshop$drawNow() {
        return rtshop$shadowPass();
    }
}

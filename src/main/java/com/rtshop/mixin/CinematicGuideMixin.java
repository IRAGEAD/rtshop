package com.rtshop.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cinematic Interactions 의 장면 화면: RT Shop 의 블록(강화 모루·룬 제단)을 쓰는 동안에는
 * 조합법 안내 책(JEI 조합법을 보여 주는 단추와 창)을 띄우지 않는다. 이 블록들에는 조합법이 없다.
 * Cinematic 이 없으면 이 믹스인은 아무것도 하지 않는다.
 */
@Pseudo
@Mixin(targets = "dev.dyslexic.cinematic.client.SceneScreen", remap = false)
public abstract class CinematicGuideMixin {
    @Inject(method = "guideShown", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void rtshop$noGuide(CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.containerMenu != null
                && mc.player.containerMenu.getClass().getName().startsWith("com.rtshop.")) {
            cir.setReturnValue(false);
        }
    }
}

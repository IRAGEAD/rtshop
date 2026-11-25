package com.rtshop.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cinematic Interactions 의 모루 장면: 강화 모루에서는 장비 설명 창이 뜨지 않게 하고, 이름 바꾸기 칸이 열리지 않게 한다
 * (장비를 가방으로 옮기려다 이름 칸을 눌러 이름 바꾸기 창이 뜨던 문제). 강화에는 이름 바꾸기가 없다.
 * Cinematic 이 없으면 이 믹스인은 아무것도 하지 않는다.
 */
@Pseudo
@Mixin(targets = "dev.dyslexic.cinematic.client.AnvilStation", remap = false)
public abstract class CinematicAnvilMixin {
    private static boolean rtshop$enhancing() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.containerMenu instanceof com.rtshop.enhance.EnhanceMenu;
    }

    @Inject(method = "overName", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void rtshop$noNameClick(double[] cursor, CallbackInfoReturnable<Boolean> cir) {
        if (rtshop$enhancing()) cir.setReturnValue(false);
    }

    /**
     * 강화 모루에서는 모루 위의 장비·결과에 마우스를 올려도 설명 창(아이템 설명 + "좌클릭 — 벼리기")을 띄우지 않는다.
     * 정보는 옆의 강화 정보 창에 모두 있다. (장면은 이 글줄을 아이템 없이 그리기 때문에 설명 창 이벤트로는 막히지 않았다.)
     */
    @Inject(method = "tooltip", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void rtshop$noTooltip(net.minecraft.world.item.ItemStack carried, CallbackInfoReturnable<java.util.List<net.minecraft.network.chat.Component>> cir) {
        if (rtshop$enhancing()) {
            com.rtshop.client.EnhanceClient.sceneTooltipBlocked++;
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "rename", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void rtshop$noRename(String name, CallbackInfo ci) {
        if (rtshop$enhancing()) ci.cancel();
    }

    @Inject(method = "renaming", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void rtshop$notRenaming(CallbackInfoReturnable<Boolean> cir) {
        if (rtshop$enhancing()) cir.setReturnValue(false);
    }
}

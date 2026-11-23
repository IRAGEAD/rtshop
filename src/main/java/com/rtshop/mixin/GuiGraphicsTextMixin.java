package com.rtshop.mixin;

import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** RT Shop 창(UiText.flat)에서는 글자 그림자를 끄고, 눈부신 흰색을 낮춘다. */
@Mixin(value = GuiGraphics.class, remap = false)
public abstract class GuiGraphicsTextMixin {

    @ModifyVariable(method = {"*(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I",
            "*(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)I",
            "*(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I"},
            at = @At("HEAD"), argsOnly = true, remap = false, require = 0)
    private boolean rtshop$noShadow(boolean shadow) {
        return com.rtshop.client.UiText.flat ? false : shadow;
    }

    @ModifyVariable(method = {"*(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I",
            "*(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)I",
            "*(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I"},
            at = @At("HEAD"), argsOnly = true, ordinal = 2, remap = false, require = 0)
    private int rtshop$softWhite(int color) {
        return com.rtshop.client.UiText.flat ? com.rtshop.client.UiText.soften(color) : color;
    }
}

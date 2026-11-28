/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.advancements.Advancement
 *  net.minecraft.advancements.AdvancementProgress
 *  net.minecraft.advancements.DisplayInfo
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.screens.advancements.AdvancementTab
 *  net.minecraft.client.gui.screens.advancements.AdvancementWidget
 *  net.minecraft.locale.Language
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.util.FormattedCharSequence
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Mutable
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.finderfeed.fdbosses.mixin;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={AdvancementWidget.class})
public class AdvancementWidgetMixin {
    @Mutable
    @Shadow
    @Final
    private List<FormattedCharSequence> f_97246_;
    @Shadow
    @Nullable
    private AdvancementProgress f_97250_;
    @Shadow
    @Final
    private AdvancementTab f_97241_;
    private static List<FormattedCharSequence> sequences = new ArrayList<FormattedCharSequence>();

    @Inject(method={"<init>"}, at={@At(value="TAIL")})
    public void init(AdvancementTab p_97255_, Minecraft p_97256_, Advancement p_97257_, DisplayInfo p_97258_, CallbackInfo ci) {
        if (this.f_97241_.m_97189_() == null) {
            return;
        }
        String title = this.f_97241_.m_97189_().getString();
        if (title.equals("Qliphoth Awakening")) {
            this.f_97246_ = new ArrayList<FormattedCharSequence>(this.f_97246_);
        }
    }

    @Inject(method={"drawHover"}, at={@At(value="HEAD")})
    public void drawHover(GuiGraphics p_283068_, int p_281304_, int p_281253_, float p_281848_, int p_282097_, int p_281537_, CallbackInfo ci) {
        float progress;
        if (this.f_97241_.m_97189_() == null) {
            return;
        }
        String title = this.f_97241_.m_97189_().getString();
        if (title.equals("Qliphoth Awakening") && (progress = this.f_97250_ == null ? 0.0f : this.f_97250_.m_8213_()) < 1.0f) {
            sequences.addAll(this.f_97246_);
            this.f_97246_.clear();
            this.f_97246_.add(Language.m_128107_().m_5536_((FormattedText)Component.m_237113_((String)"???")));
        }
    }

    @Inject(method={"drawHover"}, at={@At(value="TAIL")})
    public void drawHoverPost(GuiGraphics p_283068_, int p_281304_, int p_281253_, float p_281848_, int p_282097_, int p_281537_, CallbackInfo ci) {
        float progress;
        if (this.f_97241_.m_97189_() == null) {
            return;
        }
        String title = this.f_97241_.m_97189_().getString();
        if (title.equals("Qliphoth Awakening") && (progress = this.f_97250_ == null ? 0.0f : this.f_97250_.m_8213_()) < 1.0f) {
            this.f_97246_.clear();
            this.f_97246_.addAll(sequences);
            sequences.clear();
        }
    }
}


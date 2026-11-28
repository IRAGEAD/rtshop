/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.resources.ResourceLocation
 */
package com.pla.spiral_abyss.client.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Objects;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

public final class AbyssClockRenderer {
    private static final ResourceLocation CLOCK = new ResourceLocation("spiral_abyss", "textures/gui/clock.png");

    private AbyssClockRenderer() {
    }

    public static String twoDigits(long value) {
        return value < 10L ? "0" + value : Long.toString(value);
    }

    public static void combat(GuiGraphics graphics, Font font, int centerX, int y, int seconds, float scale) {
        int x = centerX - 33;
        RenderSystem.enableBlend();
        graphics.m_280411_(CLOCK, x, y, 66, 22, 0.0f, 0.0f, 651, 215, 651, 215);
        RenderSystem.disableBlend();
        seconds = Math.max(0, seconds);
        MutableComponent time = Component.m_237110_((String)"hud.spiral_abyss.time", (Object[])new Object[]{AbyssClockRenderer.twoDigits(seconds / 60), AbyssClockRenderer.twoDigits(seconds % 60)});
        graphics.m_280168_().m_85836_();
        PoseStack poseStack = graphics.m_280168_();
        float f = x + 44;
        float f2 = y;
        Objects.requireNonNull(font);
        poseStack.m_252880_(f, f2 + (22.0f - 9.0f * scale) / 2.0f, 0.0f);
        graphics.m_280168_().m_85841_(scale, scale, 1.0f);
        graphics.m_280653_(font, (Component)time, 0, 0, -1);
        graphics.m_280168_().m_85849_();
    }

    public static void reset(GuiGraphics graphics, Font font, int centerX, int y, int availableWidth, long millis) {
        long seconds = Math.max(0L, (millis + 999L) / 1000L);
        MutableComponent text = millis < 0L ? Component.m_237115_((String)"screen.spiral_abyss.reset_disabled") : Component.m_237110_((String)"screen.spiral_abyss.reset_remaining", (Object[])new Object[]{seconds / 86400L, AbyssClockRenderer.twoDigits(seconds / 3600L % 24L), AbyssClockRenderer.twoDigits(seconds / 60L % 60L), AbyssClockRenderer.twoDigits(seconds % 60L)});
        int width = 20 + font.m_92852_((FormattedText)text);
        float scale = Math.min(1.0f, (float)Math.max(1, availableWidth) / (float)width);
        graphics.m_280168_().m_85836_();
        graphics.m_280168_().m_252880_((float)centerX - (float)width * scale / 2.0f, (float)y, 0.0f);
        graphics.m_280168_().m_85841_(scale, scale, 1.0f);
        RenderSystem.enableBlend();
        graphics.m_280411_(CLOCK, 0, 0, 16, 16, 0.0f, 0.0f, 215, 215, 651, 215);
        RenderSystem.disableBlend();
        Objects.requireNonNull(font);
        graphics.m_280614_(font, (Component)text, 20, (16 - 9) / 2, -1, true);
        graphics.m_280168_().m_85849_();
    }
}


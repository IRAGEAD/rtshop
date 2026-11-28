/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 */
package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.client.Anim;
import dev.dyslexic.cinematic.client.Reach;
import dev.dyslexic.cinematic.client.Scene;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

public final class ReachNotice {
    private static final double FADE_IN = 0.12;
    private static final double HOLD = 1.9;
    private static final double FADE_OUT = 0.4;
    private static final int FROM_BOTTOM = 70;
    @Nullable
    private static Component line;
    private static double shownAt;

    private ReachNotice() {
    }

    public static void show(Reach.Reason reason, @Nullable Component blocker) {
        String key = "cinematic.notice." + reason.name().toLowerCase(Locale.ROOT);
        line = reason == Reach.Reason.COVERED && blocker != null ? Component.m_237110_((String)key, (Object[])new Object[]{blocker}) : Component.m_237115_((String)key);
        shownAt = Scene.clock();
    }

    @Nullable
    public static String text() {
        return ReachNotice.alpha() > 0.0f && line != null ? line.getString() : null;
    }

    private static float alpha() {
        double age = Scene.clock() - shownAt;
        if (age < 0.0 || age > 2.42) {
            return 0.0f;
        }
        double k = age < 0.12 ? age / 0.12 : (age > 2.02 ? 1.0 - (age - 0.12 - 1.9) / 0.4 : 1.0);
        return (float)Anim.smoother(k);
    }

    public static void render(GuiGraphics g) {
        float alpha = ReachNotice.alpha();
        if (line == null || Math.round(alpha * 255.0f) < 8) {
            return;
        }
        Font font = Minecraft.m_91087_().f_91062_;
        int textW = font.m_92852_((FormattedText)line);
        int w = textW + 17;
        int h = 13;
        int x = (g.m_280182_() - w) / 2;
        int y = g.m_280206_() - 70 + Math.round((1.0f - alpha) * 3.0f);
        g.m_280509_(x, y, x + w, y + h, ReachNotice.argb(0.5f * alpha, 12, 12, 16));
        g.m_280509_(x, y + h - 1, x + w, y + h, ReachNotice.argb(0.45f * alpha, 255, 150, 110));
        g.m_280509_(x + 5, y + 5, x + 8, y + 8, ReachNotice.argb(0.9f * alpha, 255, 170, 120));
        g.m_280614_(font, line, x + 12, y + 3, ReachNotice.argb(alpha, 242, 234, 216), false);
    }

    private static int argb(float a, int r, int g, int b) {
        return Math.max(0, Math.min(255, Math.round(a * 255.0f))) << 24 | r << 16 | g << 8 | b;
    }

    static {
        shownAt = -100.0;
    }
}


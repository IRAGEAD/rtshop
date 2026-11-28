/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  com.mojang.blaze3d.platform.Window
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.math.Axis
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.locale.Language
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.network.chat.Style
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.FormattedCharSequence
 *  net.minecraft.util.Mth
 *  org.joml.Vector2f
 */
package com.finderfeed.fdbosses.client;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.joml.Vector2f;

public class BossRenderUtil {
    public static final ResourceLocation BOSS_TOOLTIP = FDBosses.location("textures/gui/boss_screen_tooltip.png");
    public static final int EDGE_SIZE = 7;

    public static void renderLine(GuiGraphics graphics, float x1, float y1, float x2, float y2, float width, float r, float g, float b, float a, float re, float ge, float be, float ae) {
        PoseStack matrices = graphics.m_280168_();
        matrices.m_85836_();
        matrices.m_252880_(x1, y1, 0.0f);
        float v1 = x2 - x1;
        float v2 = y2 - y1;
        float angle = (float)Math.atan2(v1, (double)v2 + 1.0E-5);
        matrices.m_252781_(Axis.f_252393_.m_252961_(angle));
        float len = new Vector2f(v1, v2).length();
        FDRenderUtil.fill((PoseStack)matrices, (float)(-width / 2.0f), (float)0.0f, (float)width, (float)len, (float)r, (float)g, (float)b, (float)a, (float)r, (float)g, (float)b, (float)a, (float)re, (float)ge, (float)be, (float)ae, (float)re, (float)ge, (float)be, (float)ae);
        matrices.m_85849_();
    }

    public static Vector2f renderBossScreenTooltip(GuiGraphics graphics, Component component, float x, float y, float desiredTextWidth, int textColor, float textScale) {
        Window window = Minecraft.m_91087_().m_91268_();
        return BossRenderUtil.renderBossScreenTooltip(graphics, component, x, y, desiredTextWidth, textColor, textScale, 0.0f, 0.0f, window.m_85445_(), window.m_85446_());
    }

    public static Vector2f renderBossScreenTooltip(GuiGraphics graphics, Component component, float x, float y, float desiredTextWidth, int textColor, float textScale, float borderXStart, float borderYStart, float borderXEnd, float borderYEnd) {
        Font font = Minecraft.m_91087_().f_91062_;
        Objects.requireNonNull(font);
        float lineHeight = 9.0f * textScale;
        List charSequences = font.m_92865_().m_92414_((FormattedText)component, Math.round(desiredTextWidth / textScale), Style.f_131099_);
        float height = (float)charSequences.size() * lineHeight;
        float width = 0.0f;
        for (FormattedText sequence : charSequences) {
            width = Math.max((float)font.m_92852_(sequence), width);
        }
        Vector2f v = BossRenderUtil.renderBossScreenTooltip(graphics.m_280168_(), x, y, width + lineHeight, height + lineHeight, borderXStart, borderYStart, borderXEnd, borderYEnd);
        int index = 0;
        for (FormattedText sequence : charSequences) {
            FDRenderUtil.renderScaledText((GuiGraphics)graphics, (FormattedCharSequence)Language.m_128107_().m_5536_(sequence), (float)(v.x + lineHeight / 2.0f), (float)(v.y + (float)index * lineHeight + lineHeight / 2.0f), (float)textScale, (boolean)false, (int)textColor);
            ++index;
        }
        return v;
    }

    public static Vector2f renderBossScreenTooltip(PoseStack matrices, float x, float y, float inWidth, float inHeight) {
        Window window = Minecraft.m_91087_().m_91268_();
        return BossRenderUtil.renderBossScreenTooltip(matrices, x, y, inWidth, inHeight, 0.0f, 0.0f, window.m_85445_(), window.m_85446_());
    }

    public static Vector2f renderBossScreenTooltip(PoseStack matrices, float x, float y, float inWidth, float inHeight, float borderXStart, float borderYStart, float borderXEnd, float borderYEnd) {
        FDRenderUtil.bindTexture((ResourceLocation)BOSS_TOOLTIP);
        float edgeWidth = 7.0f;
        float edgeHeight = 7.0f;
        float texWidth = 16.0f;
        float texHeight = 16.0f;
        float xRenderStart = Mth.m_14036_((float)x, (float)borderXStart, (float)(borderXEnd - inWidth - edgeWidth * 2.0f));
        float yRenderStart = Mth.m_14036_((float)y, (float)borderYStart, (float)(borderYEnd - inHeight - edgeHeight * 2.0f));
        FDRenderUtil.blitWithBlend((PoseStack)matrices, (float)xRenderStart, (float)yRenderStart, (float)edgeWidth, (float)edgeHeight, (float)0.0f, (float)0.0f, (float)edgeWidth, (float)edgeHeight, (float)texWidth, (float)texHeight, (float)0.0f, (float)1.0f);
        FDRenderUtil.blitWithBlend((PoseStack)matrices, (float)(xRenderStart + inWidth + edgeWidth), (float)yRenderStart, (float)edgeWidth, (float)edgeHeight, (float)edgeWidth, (float)0.0f, (float)edgeWidth, (float)edgeHeight, (float)texWidth, (float)texHeight, (float)0.0f, (float)1.0f);
        FDRenderUtil.blitWithBlend((PoseStack)matrices, (float)xRenderStart, (float)(yRenderStart + inHeight + edgeHeight), (float)edgeWidth, (float)edgeHeight, (float)0.0f, (float)edgeHeight, (float)edgeWidth, (float)edgeHeight, (float)texWidth, (float)texHeight, (float)0.0f, (float)1.0f);
        FDRenderUtil.blitWithBlend((PoseStack)matrices, (float)(xRenderStart + inWidth + edgeWidth), (float)(yRenderStart + inHeight + edgeHeight), (float)edgeWidth, (float)edgeHeight, (float)edgeWidth, (float)edgeHeight, (float)edgeWidth, (float)edgeHeight, (float)texWidth, (float)texHeight, (float)0.0f, (float)1.0f);
        FDRenderUtil.blitWithBlend((PoseStack)matrices, (float)(xRenderStart + edgeWidth), (float)yRenderStart, (float)inWidth, (float)7.0f, (float)14.0f, (float)0.0f, (float)1.0f, (float)7.0f, (float)texWidth, (float)texHeight, (float)0.0f, (float)1.0f);
        FDRenderUtil.blitWithBlend((PoseStack)matrices, (float)(xRenderStart + edgeWidth), (float)(yRenderStart + inHeight + edgeHeight), (float)inWidth, (float)7.0f, (float)15.0f, (float)0.0f, (float)1.0f, (float)7.0f, (float)texWidth, (float)texHeight, (float)0.0f, (float)1.0f);
        FDRenderUtil.blitWithBlend((PoseStack)matrices, (float)xRenderStart, (float)(yRenderStart + edgeHeight), (float)7.0f, (float)inHeight, (float)0.0f, (float)14.0f, (float)7.0f, (float)1.0f, (float)texWidth, (float)texHeight, (float)0.0f, (float)1.0f);
        FDRenderUtil.blitWithBlend((PoseStack)matrices, (float)(xRenderStart + edgeWidth + inWidth), (float)(yRenderStart + edgeHeight), (float)7.0f, (float)inHeight, (float)0.0f, (float)15.0f, (float)7.0f, (float)1.0f, (float)texWidth, (float)texHeight, (float)0.0f, (float)1.0f);
        FDRenderUtil.blitWithBlend((PoseStack)matrices, (float)(xRenderStart + edgeWidth), (float)(yRenderStart + edgeHeight), (float)inWidth, (float)inHeight, (float)15.0f, (float)0.0f, (float)1.0f, (float)1.0f, (float)texWidth, (float)texHeight, (float)0.0f, (float)1.0f);
        Vector2f toReturn = new Vector2f(xRenderStart + edgeWidth, yRenderStart + edgeHeight);
        return toReturn;
    }
}


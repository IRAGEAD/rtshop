/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.FormattedCharSequence
 *  net.minecraft.world.entity.EntityType
 */
package com.pla.spiral_abyss.client.overlay;

import com.pla.spiral_abyss.platform.AbyssRegistries;
import java.util.List;
import java.util.Objects;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EntityType;

public final class AbyssRejectionNotice {
    public static final int ICON_SIZE = 11;
    private static final int RED = -39586;

    private AbyssRejectionNotice() {
    }

    public static void icon(GuiGraphics graphics, int x, int y) {
        for (int row = 0; row < 9; ++row) {
            int spread = row / 2;
            graphics.m_280509_(x + 5 - spread, y + row + 1, x + 6 + spread, y + row + 2, -39586);
            if (row < 3 || row >= 8) continue;
            graphics.m_280509_(x + 6 - spread, y + row + 1, x + 5 + spread, y + row + 2, -15263964);
        }
        graphics.m_280509_(x + 5, y + 4, x + 6, y + 6, -39586);
        graphics.m_280509_(x + 5, y + 7, x + 6, y + 8, -39586);
    }

    public static Component tooltip(Component boss) {
        return Component.m_237115_((String)"rejection.spiral_abyss.title").m_130944_(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.BOLD}).m_7220_((Component)Component.m_237113_((String)"\n").m_130940_(ChatFormatting.RESET)).m_7220_((Component)Component.m_237110_((String)"rejection.spiral_abyss.tooltip", (Object[])new Object[]{boss}).m_130940_(ChatFormatting.GRAY).m_130938_(style -> style.m_131136_(Boolean.valueOf(false))));
    }

    public static void panel(GuiGraphics graphics, Font font, int x, int y, int availableWidth, int availableHeight, List<ResourceLocation> bosses) {
        if (bosses.isEmpty() || availableHeight <= 0) {
            return;
        }
        PanelLayout layout = AbyssRejectionNotice.layout(font, availableWidth, bosses);
        MutableComponent title = Component.m_237115_((String)"rejection.spiral_abyss.title").m_130940_(ChatFormatting.BOLD);
        MutableComponent damage = Component.m_237115_((String)"rejection.spiral_abyss.damage");
        int width = layout.width();
        int height = layout.height();
        List<FormattedCharSequence> quoteLines = layout.quoteLines();
        List<FormattedCharSequence> nameLines = layout.nameLines();
        float scale = Math.min(0.9f, (float)availableHeight / (float)height);
        graphics.m_280168_().m_85836_();
        graphics.m_280168_().m_252880_((float)x, (float)y, 0.0f);
        graphics.m_280168_().m_85841_(scale, scale, 1.0f);
        graphics.m_280509_(2, 0, width - 2, 2, 1343229979);
        graphics.m_280509_(0, 2, width, height - 2, 1343229979);
        graphics.m_280509_(2, height - 2, width - 2, height, 1343229979);
        AbyssRejectionNotice.icon(graphics, 7, 6);
        graphics.m_280614_(font, (Component)title, 23, 8, -39586, false);
        int lineY = 24;
        for (FormattedCharSequence line : quoteLines) {
            graphics.m_280649_(font, line, 8, lineY, -1973003, false);
            Objects.requireNonNull(font);
            lineY += 9 + 2;
        }
        graphics.m_280614_(font, (Component)damage, 8, lineY + 2, -21369, false);
        Objects.requireNonNull(font);
        lineY += 9 + 6;
        for (FormattedCharSequence line : nameLines) {
            graphics.m_280649_(font, line, 8, lineY, -5593672, false);
            Objects.requireNonNull(font);
            lineY += 9 + 2;
        }
        graphics.m_280168_().m_85849_();
    }

    public static PanelLayout layout(Font font, int availableWidth, List<ResourceLocation> bosses) {
        MutableComponent title = Component.m_237115_((String)"rejection.spiral_abyss.title").m_130940_(ChatFormatting.BOLD);
        MutableComponent quote = Component.m_237115_((String)"rejection.spiral_abyss.quote").m_130940_(ChatFormatting.ITALIC);
        MutableComponent damage = Component.m_237115_((String)"rejection.spiral_abyss.damage");
        MutableComponent names = Component.m_237119_();
        for (ResourceLocation id : bosses) {
            EntityType<?> type = AbyssRegistries.ENTITY_TYPES.getValue(id);
            if (!names.getString().isEmpty()) {
                names.m_7220_((Component)Component.m_237113_((String)", "));
            }
            names.m_7220_((Component)(type == null ? Component.m_237113_((String)id.toString()) : type.m_20676_()));
        }
        int contentWidth = Math.max(font.m_92852_((FormattedText)title) + 31, Math.max(font.m_92852_((FormattedText)quote), Math.max(font.m_92852_((FormattedText)damage), font.m_92852_((FormattedText)names))) + 16);
        int width = Math.max(32, Math.min(Math.min(300, availableWidth), contentWidth));
        List quoteLines = font.m_92923_((FormattedText)quote, width - 16);
        List nameLines = font.m_92923_((FormattedText)names, width - 16);
        int n = quoteLines.size() + nameLines.size() + 1;
        Objects.requireNonNull(font);
        int height = 28 + n * (9 + 2);
        return new PanelLayout(width, height, quoteLines, nameLines);
    }

    public record PanelLayout(int width, int height, List<FormattedCharSequence> quoteLines, List<FormattedCharSequence> nameLines) {
    }
}


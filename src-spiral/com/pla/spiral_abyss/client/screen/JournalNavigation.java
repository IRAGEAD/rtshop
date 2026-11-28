/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.Tooltip
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.resources.ResourceLocation
 */
package com.pla.spiral_abyss.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

public final class JournalNavigation {
    public static final ResourceLocation LOGO = new ResourceLocation("spiral_abyss", "textures/item/creative_tab_icon.png");
    private static final ResourceLocation HELP = JournalNavigation.texture("help");
    public static final ResourceLocation MENU_ICON = JournalNavigation.texture("menu_icon");

    private JournalNavigation() {
    }

    public static ResourceLocation texture(String name) {
        return new ResourceLocation("spiral_abyss", "textures/gui/journal/" + name + ".png");
    }

    public static void add(Consumer<Button> add, int width, int selected, Runnable banner, Runnable help, Runnable close) {
        int count = 2;
        int size = 44;
        int gap = 8;
        int left = (width - count * size - (count - 1) * gap) / 2;
        add.accept(JournalNavigation.icon(left, 6, size, 28, LOGO, 32, "screen.spiral_abyss.journal.banner", selected == 0, banner));
        add.accept(JournalNavigation.icon(left + size + gap, 6, size, 28, HELP, 513, "screen.spiral_abyss.journal.help", selected == 1, help));
        Button exit = new Button(width - 28, 8, 20, 20, (Component)Component.m_237113_((String)"\u00d7"), b -> close.run(), n -> (MutableComponent)n.get()){

            protected void m_87963_(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
                g.m_280509_(this.m_252754_(), this.m_252907_(), this.m_252754_() + 20, this.m_252907_() + 20, this.m_198029_() ? -579575432 : -1441259466);
                g.m_280653_(Minecraft.m_91087_().f_91062_, this.m_6035_(), this.m_252754_() + 10, this.m_252907_() + 6, -793143);
            }
        };
        exit.m_257544_(Tooltip.m_257550_((Component)Component.m_237115_((String)"screen.spiral_abyss.close")));
        add.accept(exit);
    }

    public static Button icon(int x, int y, int w, int h, final ResourceLocation texture, final int sourceSize, String label, final boolean selected, Runnable action) {
        Button result = new Button(x, y, w, h, (Component)Component.m_237115_((String)label), b -> action.run(), n -> (MutableComponent)n.get()){

            protected void m_87963_(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
                g.m_280509_(this.m_252754_(), this.m_252907_(), this.m_252754_() + this.m_5711_(), this.m_252907_() + this.m_93694_(), selected ? -580297595 : (this.m_198029_() ? -582925210 : -1071569092));
                g.m_280637_(this.m_252754_(), this.m_252907_(), this.m_5711_(), this.m_93694_(), selected || this.m_198029_() ? -1717875 : -9670260);
                int size = this.m_93694_() - 4;
                RenderSystem.enableBlend();
                g.m_280411_(texture, this.m_252754_() + (this.m_5711_() - size) / 2, this.m_252907_() + 2, size, size, 0.0f, 0.0f, sourceSize, sourceSize, sourceSize, sourceSize);
                RenderSystem.disableBlend();
            }
        };
        result.m_257544_(Tooltip.m_257550_((Component)result.m_6035_()));
        return result;
    }
}


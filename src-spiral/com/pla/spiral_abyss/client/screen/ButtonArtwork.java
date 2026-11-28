/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.resources.ResourceLocation
 */
package com.pla.spiral_abyss.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

record ButtonArtwork(ResourceLocation normal, ResourceLocation hover, ResourceLocation chosen, int u, int v, int sourceWidth, int sourceHeight, int textureWidth, int textureHeight) {
    static final ButtonArtwork BACK = new ButtonArtwork(ButtonArtwork.texture("back_button"), ButtonArtwork.texture("back_button_hover"), ButtonArtwork.texture("back_button"), 111, 0, 277, 500, 500, 500);
    static final ButtonArtwork FIGHT = new ButtonArtwork(ButtonArtwork.texture("fight_button"), ButtonArtwork.texture("fight_button_hover"), ButtonArtwork.texture("fight_button"), 0, 0, 497, 189, 500, 200);
    static final ButtonArtwork WAVE = new ButtonArtwork(ButtonArtwork.texture("wave_button_not_chosen"), ButtonArtwork.texture("wave_button_hover"), ButtonArtwork.texture("wave_button_chosen"), 0, 0, 750, 150, 750, 150);

    private static ResourceLocation texture(String name) {
        return new ResourceLocation("spiral_abyss", "textures/gui/buttons/" + name + ".png");
    }

    int fittedWidth(int maxWidth, int maxHeight) {
        return Math.max(1, Math.min(maxWidth, maxHeight * this.sourceWidth / this.sourceHeight));
    }

    int fittedHeight(int width) {
        return Math.max(1, Math.round((float)width * (float)this.sourceHeight / (float)this.sourceWidth));
    }

    void render(GuiGraphics graphics, int x, int y, int width, int height, boolean highlighted, boolean selected, boolean enabled) {
        ResourceLocation image = selected ? this.chosen : (highlighted && enabled ? this.hover : this.normal);
        RenderSystem.enableBlend();
        graphics.m_280246_(enabled || selected ? 1.0f : 0.45f, enabled || selected ? 1.0f : 0.45f, enabled || selected ? 1.0f : 0.45f, 1.0f);
        graphics.m_280411_(image, x, y, width, height, (float)this.u, (float)this.v, this.sourceWidth, this.sourceHeight, this.textureWidth, this.textureHeight);
        graphics.m_280246_(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }
}


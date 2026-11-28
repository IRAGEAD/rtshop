/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.simple_screen.FDWidget
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.datafixers.util.Either
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.item.ItemStack
 */
package com.finderfeed.fdbosses.client.boss_screen.widget;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdlib.systems.simple_screen.FDWidget;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Either;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class SkillInfoWidget
extends FDWidget {
    public static final ResourceLocation MIDDLE = FDBosses.location("textures/gui/ability_part_middle.png");
    public static final ResourceLocation DOWN = FDBosses.location("textures/gui/ability_part_down.png");
    public static final ResourceLocation UP = FDBosses.location("textures/gui/ability_part_up.png");
    private Component skillName = Component.m_237119_();
    private int color = 0xFFFFFF;
    private Either<ResourceLocation, ItemStack> skillImage;

    public SkillInfoWidget(Screen screen, float x, float y, float width, float height, Component skillName, int color) {
        super(screen, x, y, width, height);
        this.skillName = skillName;
        this.color = color;
    }

    public void renderWidget(GuiGraphics guiGraphics, float mx, float my, float pticks) {
        PoseStack matrices = guiGraphics.m_280168_();
        float cumulativeHeight = 156.0f;
        FDRenderUtil.bindTexture((ResourceLocation)UP);
        FDRenderUtil.blitWithBlend((PoseStack)matrices, (float)this.getX(), (float)this.getY(), (float)this.getWidth(), (float)103.0f, (float)0.0f, (float)0.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)0.0f, (float)1.0f);
        float middleHeight = Math.max(0.0f, this.getHeight() - cumulativeHeight);
        if (middleHeight != 0.0f) {
            FDRenderUtil.bindTexture((ResourceLocation)MIDDLE);
            FDRenderUtil.blitWithBlend((PoseStack)matrices, (float)this.getX(), (float)(this.getY() + 103.0f), (float)this.getWidth(), (float)middleHeight, (float)0.0f, (float)0.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)0.0f, (float)1.0f);
        }
        FDRenderUtil.bindTexture((ResourceLocation)DOWN);
        FDRenderUtil.blitWithBlend((PoseStack)matrices, (float)this.getX(), (float)(this.getY() + 103.0f + middleHeight), (float)this.getWidth(), (float)53.0f, (float)0.0f, (float)0.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)0.0f, (float)1.0f);
        float scale = 1.5f;
        Font font = Minecraft.m_91087_().f_91062_;
        float textWidth = (float)font.m_92852_((FormattedText)this.skillName) * scale;
        float textX = this.getX() + this.getWidth() / 2.0f + 7.0f - textWidth / 2.0f;
        float textY = 61.0f;
        float r = (this.color & 0xFF0000) >> 16;
        r /= 255.0f;
        float g = (this.color & 0xFF00) >> 8;
        g /= 255.0f;
        float b = this.color & 0xFF;
        PoseStack poseStack = guiGraphics.m_280168_();
        float f = textWidth / 2.0f + 20.0f;
        Objects.requireNonNull(font);
        FDRenderUtil.fill((PoseStack)poseStack, (float)(textX - 20.0f), (float)(textY - 2.0f), (float)f, (float)(9.0f * scale + 3.0f), (float)r, (float)g, (float)(b /= 255.0f), (float)0.0f, (float)r, (float)g, (float)b, (float)0.3f, (float)r, (float)g, (float)b, (float)0.3f, (float)r, (float)g, (float)b, (float)0.0f);
        PoseStack poseStack2 = guiGraphics.m_280168_();
        float f2 = textX + textWidth / 2.0f;
        float f3 = textWidth / 2.0f + 20.0f;
        Objects.requireNonNull(font);
        FDRenderUtil.fill((PoseStack)poseStack2, (float)f2, (float)(textY - 2.0f), (float)f3, (float)(9.0f * scale + 3.0f), (float)r, (float)g, (float)b, (float)0.3f, (float)r, (float)g, (float)b, (float)0.0f, (float)r, (float)g, (float)b, (float)0.0f, (float)r, (float)g, (float)b, (float)0.3f);
        FDRenderUtil.renderScaledText((GuiGraphics)guiGraphics, (Component)this.skillName, (float)textX, (float)textY, (float)scale, (boolean)true, (int)this.color);
        if (this.skillImage != null) {
            Optional item = this.skillImage.right();
            Optional rl = this.skillImage.left();
            float offs = 20.0f;
            if (rl.isPresent()) {
                FDRenderUtil.bindTexture((ResourceLocation)((ResourceLocation)rl.get()));
                FDRenderUtil.blitWithBlend((PoseStack)matrices, (float)(this.getX() + 97.0f), (float)(this.getY() + offs + 7.0f), (float)16.0f, (float)16.0f, (float)0.0f, (float)0.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)0.0f, (float)1.0f);
            } else if (item.isPresent()) {
                FDRenderUtil.renderScaledItemStack((GuiGraphics)guiGraphics, (float)(this.getX() + 97.0f), (float)(this.getY() + offs + 7.0f), (float)1.0f, (ItemStack)((ItemStack)item.get()));
            }
        }
        MutableComponent close = Component.m_237115_((String)"fdbosses.close_skill_info");
        FDRenderUtil.renderScaledText((GuiGraphics)guiGraphics, (Component)close, (float)(this.getX() + this.getWidth() / 2.0f - (float)font.m_92852_((FormattedText)close) * 0.25f + 6.0f), (float)(this.getY() + this.getHeight() - 21.0f), (float)0.5f, (boolean)true, (int)this.color);
    }

    public void setSkillName(Component skillName, int color) {
        this.skillName = skillName;
        this.color = color;
    }

    public void setSkillImage(Either<ResourceLocation, ItemStack> location) {
        this.skillImage = location;
    }

    public boolean onMouseClick(float v, float v1, int i) {
        return false;
    }

    public boolean onMouseRelease(float v, float v1, int i) {
        return false;
    }

    public boolean onMouseScroll(float v, float v1, float v3) {
        return false;
    }

    public boolean onCharTyped(char c, int i) {
        return false;
    }

    public boolean onKeyPress(int i, int i1, int i2) {
        return false;
    }

    public boolean onKeyRelease(int i, int i1, int i2) {
        return false;
    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.pipeline.TextureTarget
 *  com.mojang.blaze3d.platform.GlStateManager
 *  com.mojang.blaze3d.platform.Lighting
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexSorting
 *  com.mojang.logging.LogUtils
 *  com.mojang.math.Axis
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.world.item.ItemDisplayContext
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  org.joml.Matrix4f
 *  org.lwjgl.opengl.GL11
 */
package com.pla.spiral_abyss.client.screen;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import com.pla.spiral_abyss.client.screen.BannerPortrait;
import com.pla.spiral_abyss.platform.AbyssRegistries;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

final class BannerRewardRenderer {
    private final Map<Key, Portrait> portraits = new HashMap<Key, Portrait>();
    private final Set<Key> unavailable = new HashSet<Key>();

    BannerRewardRenderer() {
    }

    int fittedWidth(GuiGraphics graphics, ItemStack stack, int width, int height, float tilt) {
        Portrait portrait = this.portrait(graphics, stack, tilt);
        if (portrait == null) {
            return width;
        }
        BannerPortrait.Crop crop = portrait.crop();
        float scale = Math.min((float)width / (float)crop.width(), (float)height / (float)crop.height());
        return Math.max(1, Math.round((float)crop.width() * scale));
    }

    void render(GuiGraphics graphics, ItemStack stack, int x, int y, int width, int height, float tilt) {
        Portrait portrait = this.portrait(graphics, stack, tilt);
        if (portrait != null) {
            BannerPortrait.draw(graphics, portrait.target(), portrait.crop(), x, y, width, height);
        }
    }

    private Portrait portrait(GuiGraphics graphics, ItemStack stack, float tilt) {
        Key key = new Key(String.valueOf(AbyssRegistries.ITEMS.getKey(stack.m_41720_())), String.valueOf(stack.m_41783_()), tilt);
        if (this.unavailable.contains(key)) {
            return null;
        }
        try {
            Portrait portrait = this.portraits.get(key);
            if (portrait == null) {
                graphics.m_280262_();
                portrait = this.capture(graphics, stack, tilt);
                this.portraits.put(key, portrait);
            }
            return portrait;
        }
        catch (LinkageError | RuntimeException exception) {
            this.unavailable.add(key);
            LogUtils.getLogger().warn("Cannot render Abyss reward model {}", (Object)key.item(), (Object)exception);
            return null;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private Portrait capture(GuiGraphics graphics, ItemStack stack, float tilt) {
        Minecraft client = Minecraft.m_91087_();
        BannerPortrait.RenderState previous = new BannerPortrait.RenderState();
        TextureTarget target = null;
        boolean success = false;
        try {
            Portrait portrait;
            target = new TextureTarget(512, 512, true, Minecraft.f_91002_);
            target.m_83936_(9729);
            target.m_83931_(0.0f, 0.0f, 0.0f, 0.0f);
            RenderSystem.disableScissor();
            RenderSystem.setProjectionMatrix((Matrix4f)new Matrix4f().setOrtho(0.0f, 512.0f, 512.0f, 0.0f, 1000.0f, 21000.0f), (VertexSorting)VertexSorting.f_276633_);
            RenderSystem.getModelViewStack().m_85836_();
            try {
                RenderSystem.getModelViewStack().m_166856_();
                RenderSystem.applyModelViewMatrix();
                BannerPortrait.Crop crop = null;
                for (int attempt = 0; attempt < 4; ++attempt) {
                    RenderSystem.depthMask((boolean)true);
                    target.m_83954_(Minecraft.f_91002_);
                    target.m_83947_(true);
                    RenderSystem.enableDepthTest();
                    RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
                    Lighting.m_84930_();
                    PoseStack pose = new PoseStack();
                    pose.m_252880_(256.0f, 256.0f, -11000.0f);
                    float size = 230.0f * (float)Math.pow(0.5, attempt);
                    pose.m_85841_(size, -size, size);
                    pose.m_252781_(Axis.f_252403_.m_252977_(tilt));
                    pose.m_252781_(Axis.f_252436_.m_252977_(-18.0f));
                    client.m_91291_().m_269128_(stack, ItemDisplayContext.FIXED, 0xF000F0, OverlayTexture.f_118083_, pose, (MultiBufferSource)graphics.m_280091_(), (Level)client.f_91073_, 0);
                    graphics.m_280262_();
                    crop = BannerPortrait.finish(target);
                    if (crop.x() > 0 && crop.y() > 0 && crop.x() + crop.width() < 512 && crop.y() + crop.height() < 512) break;
                }
                success = true;
                portrait = new Portrait(target, crop);
            }
            catch (Throwable throwable) {
                try {
                    if (!success) {
                        graphics.m_280262_();
                    }
                }
                finally {
                    RenderSystem.getModelViewStack().m_85849_();
                    RenderSystem.applyModelViewMatrix();
                }
                throw throwable;
            }
            try {
                if (!success) {
                    graphics.m_280262_();
                }
            }
            finally {
                RenderSystem.getModelViewStack().m_85849_();
                RenderSystem.applyModelViewMatrix();
            }
            return portrait;
        }
        finally {
            try {
                if (!success && target != null) {
                    target.m_83930_();
                }
            }
            finally {
                Lighting.m_84931_();
                previous.restore();
            }
        }
    }

    void clear() {
        int framebuffer = GL11.glGetInteger((int)36006);
        int readFramebuffer = GL11.glGetInteger((int)36010);
        try {
            this.portraits.values().forEach(p -> p.target().m_83930_());
        }
        finally {
            GlStateManager._glBindFramebuffer((int)36160, (int)framebuffer);
            GlStateManager._glBindFramebuffer((int)36008, (int)readFramebuffer);
        }
        this.portraits.clear();
        this.unavailable.clear();
    }

    private record Portrait(TextureTarget target, BannerPortrait.Crop crop) {
    }

    private record Key(String item, String tag, float tilt) {
    }
}


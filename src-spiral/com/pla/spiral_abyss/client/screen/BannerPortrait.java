/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.pipeline.TextureTarget
 *  com.mojang.blaze3d.platform.GlStateManager
 *  com.mojang.blaze3d.platform.NativeImage
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.BufferBuilder
 *  com.mojang.blaze3d.vertex.BufferBuilder$RenderedBuffer
 *  com.mojang.blaze3d.vertex.BufferUploader
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.Tesselator
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  com.mojang.blaze3d.vertex.VertexSorting
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.renderer.GameRenderer
 *  net.minecraft.client.renderer.ShaderInstance
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.lwjgl.opengl.GL11
 */
package com.pla.spiral_abyss.client.screen;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL11;

final class BannerPortrait {
    BannerPortrait() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void draw(GuiGraphics graphics, TextureTarget portrait, Crop crop, int x, int y, int width, int height) {
        graphics.m_280262_();
        RenderState previous = new RenderState();
        try {
            float scale = Math.min((float)width / (float)crop.width, (float)height / (float)crop.height);
            float drawWidth = (float)crop.width * scale;
            float drawHeight = (float)crop.height * scale;
            float left = (float)x + ((float)width - drawWidth) / 2.0f;
            float top = (float)y + ((float)height - drawHeight) / 2.0f;
            RenderSystem.setShader(GameRenderer::m_172817_);
            RenderSystem.setShaderTexture((int)0, (int)portrait.m_83975_());
            RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            Matrix4f matrix = graphics.m_280168_().m_85850_().m_252922_();
            BufferBuilder buffer = Tesselator.m_85913_().m_85915_();
            buffer.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85817_);
            float u0 = (float)crop.x / (float)portrait.f_83915_;
            float u1 = (float)(crop.x + crop.width) / (float)portrait.f_83915_;
            float v0 = 1.0f - (float)(crop.y + crop.height) / (float)portrait.f_83916_;
            float v1 = 1.0f - (float)crop.y / (float)portrait.f_83916_;
            buffer.m_252986_(matrix, left, top + drawHeight, 0.0f).m_7421_(u0, v0).m_5752_();
            buffer.m_252986_(matrix, left + drawWidth, top + drawHeight, 0.0f).m_7421_(u1, v0).m_5752_();
            buffer.m_252986_(matrix, left + drawWidth, top, 0.0f).m_7421_(u1, v1).m_5752_();
            buffer.m_252986_(matrix, left, top, 0.0f).m_7421_(u0, v1).m_5752_();
            BufferUploader.m_231202_((BufferBuilder.RenderedBuffer)buffer.m_231175_());
        }
        finally {
            previous.restore();
        }
    }

    static Crop finish(TextureTarget target) {
        int active = GL11.glGetInteger((int)34016);
        RenderSystem.activeTexture((int)33984);
        int bound = GL11.glGetInteger((int)32873);
        try {
            int bottom;
            int right;
            int top;
            int left;
            NativeImage pixels;
            block19: {
                Crop crop;
                pixels = new NativeImage(target.f_83915_, target.f_83916_, false);
                try {
                    int i;
                    int px;
                    int py;
                    RenderSystem.bindTexture((int)target.m_83975_());
                    pixels.m_85045_(0, false);
                    pixels.m_85122_();
                    int[] distance = new int[target.f_83915_ * target.f_83916_];
                    for (py = 0; py < target.f_83916_; ++py) {
                        for (px = 0; px < target.f_83915_; ++px) {
                            i = py * target.f_83915_ + px;
                            int n = distance[i] = pixels.m_84985_(px, py) >>> 24 >= 24 ? 0 : 10000;
                            if (px > 0) {
                                distance[i] = Math.min(distance[i], distance[i - 1] + 1);
                            }
                            if (py <= 0) continue;
                            distance[i] = Math.min(distance[i], distance[i - target.f_83915_] + 1);
                        }
                    }
                    for (py = target.f_83916_ - 1; py >= 0; --py) {
                        for (px = target.f_83915_ - 1; px >= 0; --px) {
                            i = py * target.f_83915_ + px;
                            if (px + 1 < target.f_83915_) {
                                distance[i] = Math.min(distance[i], distance[i + 1] + 1);
                            }
                            if (py + 1 < target.f_83916_) {
                                distance[i] = Math.min(distance[i], distance[i + target.f_83915_] + 1);
                            }
                            if (distance[i] <= 0 || distance[i] > 6) continue;
                            pixels.m_84988_(px, py, distance[i] <= 3 ? -10467531 : -660779);
                        }
                    }
                    left = target.f_83915_;
                    top = target.f_83916_;
                    right = -1;
                    bottom = -1;
                    for (int py2 = 0; py2 < target.f_83916_; ++py2) {
                        for (int px2 = 0; px2 < target.f_83915_; ++px2) {
                            if (pixels.m_84985_(px2, py2) >>> 24 < 24) continue;
                            left = Math.min(left, px2);
                            right = Math.max(right, px2);
                            top = Math.min(top, py2);
                            bottom = Math.max(bottom, py2);
                        }
                    }
                    pixels.m_85122_();
                    pixels.m_85040_(0, 0, 0, false);
                    if (right >= left) break block19;
                    crop = new Crop(0, 0, target.f_83915_, target.f_83916_);
                }
                catch (Throwable throwable) {
                    try {
                        pixels.close();
                    }
                    catch (Throwable throwable2) {
                        throwable.addSuppressed(throwable2);
                    }
                    throw throwable;
                }
                pixels.close();
                return crop;
            }
            left = Math.max(0, left - 4);
            top = Math.max(0, top - 4);
            right = Math.min(target.f_83915_, right + 5);
            bottom = Math.min(target.f_83916_, bottom + 5);
            Crop crop = new Crop(left, top, right - left, bottom - top);
            pixels.close();
            return crop;
        }
        finally {
            RenderSystem.bindTexture((int)bound);
            RenderSystem.activeTexture((int)active);
        }
    }

    static final class RenderState {
        private final int framebuffer = GL11.glGetInteger((int)36006);
        private final int readFramebuffer = GL11.glGetInteger((int)36010);
        private final int[] viewport = new int[4];
        private final int[] scissor = new int[4];
        private final boolean scissorEnabled = GL11.glIsEnabled((int)3089);
        private final boolean depth = GL11.glIsEnabled((int)2929);
        private final boolean depthWrite = GL11.glGetBoolean((int)2930);
        private final boolean blend = GL11.glIsEnabled((int)3042);
        private final boolean cull = GL11.glIsEnabled((int)2884);
        private final int blendSource = GL11.glGetInteger((int)32969);
        private final int blendDestination = GL11.glGetInteger((int)32968);
        private final int alphaSource = GL11.glGetInteger((int)32971);
        private final int alphaDestination = GL11.glGetInteger((int)32970);
        private final int depthFunction = GL11.glGetInteger((int)2932);
        private final Matrix4f projection = new Matrix4f((Matrix4fc)RenderSystem.getProjectionMatrix());
        private final Matrix4f textureMatrix = new Matrix4f((Matrix4fc)RenderSystem.getTextureMatrix());
        private final VertexSorting sorting = RenderSystem.getVertexSorting();
        private final ShaderInstance shader = RenderSystem.getShader();
        private final float[] color = (float[])RenderSystem.getShaderColor().clone();
        private final float[] clearColor = new float[4];
        private final double clearDepth = GL11.glGetDouble((int)2931);
        private final int[] textures = new int[]{RenderSystem.getShaderTexture((int)0), RenderSystem.getShaderTexture((int)1), RenderSystem.getShaderTexture((int)2)};

        RenderState() {
            GL11.glGetIntegerv((int)2978, (int[])this.viewport);
            GL11.glGetIntegerv((int)3088, (int[])this.scissor);
            GL11.glGetFloatv((int)3106, (float[])this.clearColor);
        }

        void restore() {
            GlStateManager._glBindFramebuffer((int)36160, (int)this.framebuffer);
            GlStateManager._glBindFramebuffer((int)36008, (int)this.readFramebuffer);
            RenderSystem.viewport((int)this.viewport[0], (int)this.viewport[1], (int)this.viewport[2], (int)this.viewport[3]);
            RenderSystem.setProjectionMatrix((Matrix4f)this.projection, (VertexSorting)this.sorting);
            RenderSystem.setTextureMatrix((Matrix4f)this.textureMatrix);
            RenderSystem.clearColor((float)this.clearColor[0], (float)this.clearColor[1], (float)this.clearColor[2], (float)this.clearColor[3]);
            RenderSystem.clearDepth((double)this.clearDepth);
            RenderSystem.setShader(() -> this.shader);
            for (int i = 0; i < this.textures.length; ++i) {
                RenderSystem.setShaderTexture((int)i, (int)this.textures[i]);
            }
            RenderSystem.setShaderColor((float)this.color[0], (float)this.color[1], (float)this.color[2], (float)this.color[3]);
            if (this.scissorEnabled) {
                RenderSystem.enableScissor((int)this.scissor[0], (int)this.scissor[1], (int)this.scissor[2], (int)this.scissor[3]);
            } else {
                RenderSystem.disableScissor();
            }
            if (this.depth) {
                RenderSystem.enableDepthTest();
            } else {
                RenderSystem.disableDepthTest();
            }
            RenderSystem.depthMask((boolean)this.depthWrite);
            RenderSystem.depthFunc((int)this.depthFunction);
            RenderSystem.blendFuncSeparate((int)this.blendSource, (int)this.blendDestination, (int)this.alphaSource, (int)this.alphaDestination);
            if (this.blend) {
                RenderSystem.enableBlend();
            } else {
                RenderSystem.disableBlend();
            }
            if (this.cull) {
                RenderSystem.enableCull();
            } else {
                RenderSystem.disableCull();
            }
        }
    }

    record Crop(int x, int y, int width, int height) {
    }
}


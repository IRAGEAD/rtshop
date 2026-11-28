/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 */
package com.pla.spiral_abyss.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

final class PortalAura {
    private PortalAura() {
    }

    static void render(PoseStack pose, VertexConsumer out, boolean xAxis, float ticks, int seed, int detail, float fade) {
        float alpha;
        float progress;
        int i;
        if (detail <= 0 || fade <= 0.0f) {
            return;
        }
        int motes = detail == 1 ? 12 : 28;
        int rays = detail == 1 ? 4 : 8;
        float time = ticks + (float)Math.floorMod(seed, 719);
        for (i = 0; i < motes; ++i) {
            progress = PortalAura.fraction(time * (0.0035f + (float)(i % 3) * 6.0E-4f) + (float)i * 0.618034f);
            float side = (i & 1) == 0 ? -1.0f : 1.0f;
            float x = 0.5f + side * (1.55f + PortalAura.fraction((float)i * 0.371f) * 0.82f) + (float)Math.sin(time * 0.025f + (float)i * 2.1f) * 0.1f;
            if (i % 4 == 0) {
                x = 0.5f + (PortalAura.fraction((float)i * 0.419f + 0.15f) - 0.5f) * 3.8f;
            }
            float y = 0.3f + progress * 5.25f;
            float depth = 0.5f + (float)((i & 2) == 0 ? -1 : 1) * (0.22f + PortalAura.fraction((float)i * 0.271f) * 0.65f);
            if (i % 4 == 0) {
                depth = 0.5f + (float)((i & 4) == 0 ? -1 : 1) * 1.05f;
            }
            alpha = (float)Math.sin((double)progress * Math.PI) * fade * 0.68f;
            float size = 0.018f + (float)(i % 3) * 0.009f;
            PortalAura.quad(pose, out, xAxis, x - size, y - size, x + size, y + size, depth, 0x77EFFF, alpha, alpha);
            if (i % 5 != 0) continue;
            PortalAura.quad(pose, out, xAxis, x + size, y - size, x + size * 3.0f, y, depth, 3783148, alpha * 0.6f, alpha * 0.6f);
            PortalAura.quad(pose, out, xAxis, x + size * 2.0f, y, x + size * 3.0f, y + size * 3.0f, depth, 3783148, alpha * 0.6f, alpha * 0.6f);
        }
        for (i = 0; i < rays; ++i) {
            progress = PortalAura.fraction(time * 0.0028f + (float)i * 0.618034f);
            float x = 0.5f + (float)((i & 1) == 0 ? -1 : 1) * (1.85f + PortalAura.fraction((float)i * 0.419f) * 0.62f);
            if (i % 4 == 0) {
                x = 0.5f + (i == 0 ? -0.85f : 0.85f);
            }
            float y = 0.25f + progress * 4.7f;
            float depth = 0.5f + (float)((i & 2) == 0 ? -1 : 1) * (0.18f + PortalAura.fraction((float)i * 0.317f) * 0.65f);
            float length = 0.45f + PortalAura.fraction((float)i * 0.713f) * 0.9f;
            alpha = (float)Math.sin((double)progress * Math.PI) * fade * 0.5f;
            PortalAura.quad(pose, out, xAxis, x - 0.025f, y, x + 0.025f, y + length, depth, 2461651, alpha * 0.25f, 0.0f);
            PortalAura.quad(pose, out, xAxis, x - 0.006f, y, x + 0.006f, y + length, depth, 6939647, alpha, 0.0f);
        }
    }

    private static float fraction(float value) {
        return value - (float)Math.floor(value);
    }

    private static void quad(PoseStack pose, VertexConsumer out, boolean xAxis, float left, float bottom, float right, float top, float depth, int rgb, float lowAlpha, float highAlpha) {
        PortalAura.point(pose, out, xAxis, left, bottom, depth, rgb, lowAlpha);
        PortalAura.point(pose, out, xAxis, right, bottom, depth, rgb, lowAlpha);
        PortalAura.point(pose, out, xAxis, right, top, depth, rgb, highAlpha);
        PortalAura.point(pose, out, xAxis, left, top, depth, rgb, highAlpha);
    }

    private static void point(PoseStack pose, VertexConsumer out, boolean xAxis, float x, float y, float depth, int rgb, float alpha) {
        out.m_252986_(pose.m_85850_().m_252922_(), xAxis ? x : depth, y, xAxis ? depth : x).m_85950_((float)(rgb >> 16 & 0xFF) / 255.0f, (float)(rgb >> 8 & 0xFF) / 255.0f, (float)(rgb & 0xFF) / 255.0f, alpha).m_5752_();
    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  net.minecraft.world.phys.Vec3
 */
package com.pla.spiral_abyss.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;

public final class ChestInteriorGlow {
    private ChestInteriorGlow() {
    }

    public static void render(PoseStack pose, VertexConsumer out, float time, int seed, int color, float fade) {
        int columns = 10;
        int rows = 8;
        for (int layer = 0; layer < 4; ++layer) {
            double y = 0.57 + (double)layer * 0.09;
            float strength = fade * (layer == 0 ? 0.78f : 0.15f / (float)layer);
            for (int x = 0; x < columns; ++x) {
                for (int z = 0; z < rows; ++z) {
                    for (int corner = 0; corner < 4; ++corner) {
                        int gx = x + (corner == 1 || corner == 2 ? 1 : 0);
                        int gz = z + (corner >= 2 ? 1 : 0);
                        float nx = (float)gx / (float)columns;
                        float nz = (float)gz / (float)rows;
                        float edge = Math.min(1.0f, Math.min(Math.min(nx, 1.0f - nx), Math.min(nz, 1.0f - nz)) * 10.0f);
                        float shimmer = 0.7f + 0.3f * (float)Math.sin((double)time * 0.12 + (double)gx * 1.7 + (double)gz * 2.3 + (double)seed);
                        ChestInteriorGlow.vertex(pose, out, new Vec3(((double)nx - 0.5) * 1.08, y, ((double)nz - 0.5) * 0.8), color, strength * edge * shimmer);
                    }
                }
            }
        }
    }

    private static void vertex(PoseStack pose, VertexConsumer out, Vec3 point, int color, float alpha) {
        out.m_252986_(pose.m_85850_().m_252922_(), (float)point.f_82479_, (float)point.f_82480_, (float)point.f_82481_).m_85950_((float)(color >> 16 & 0xFF) / 255.0f, (float)(color >> 8 & 0xFF) / 255.0f, (float)(color & 0xFF) / 255.0f, alpha).m_5752_();
    }
}


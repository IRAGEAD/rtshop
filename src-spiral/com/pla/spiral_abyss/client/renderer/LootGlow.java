/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.blaze3d.vertex.VertexFormat
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.RenderType$CompositeState
 *  net.minecraft.world.phys.Vec3
 */
package com.pla.spiral_abyss.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;

public final class LootGlow
extends RenderType {
    public static final int SILVER = 0xCCDFFF;
    public static final int CYAN = 3599871;
    public static final int GOLD = 16764226;
    public static final RenderType TYPE = LootGlow.m_173215_((String)"spiral_abyss_loot_glow", (VertexFormat)DefaultVertexFormat.f_85815_, (VertexFormat.Mode)VertexFormat.Mode.QUADS, (int)1536, (boolean)false, (boolean)true, (RenderType.CompositeState)RenderType.CompositeState.m_110628_().m_173292_(f_173091_).m_110685_(f_110136_).m_110661_(f_110110_).m_110687_(f_110115_).m_110691_(false));

    private LootGlow(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling, boolean sorting, Runnable setup, Runnable clear) {
        super(name, format, mode, size, crumbling, sorting, setup, clear);
    }

    public static void ribbon(PoseStack pose, VertexConsumer out, Vec3 from, Vec3 to, Vec3 camera, float startWidth, float endWidth, int color, float startAlpha, float endAlpha) {
        Vec3 direction = to.m_82546_(from);
        if (direction.m_82556_() < 1.0E-8) {
            return;
        }
        Vec3 side = direction.m_82537_(camera.m_82546_(from)).m_82541_();
        if (side.m_82556_() < 1.0E-8) {
            side = new Vec3(1.0, 0.0, 0.0);
        }
        float[] edges = new float[]{-1.0f, -0.3f, 0.3f, 1.0f};
        float[] opacity = new float[]{0.0f, 1.0f, 1.0f, 0.0f};
        for (int i = 0; i < 3; ++i) {
            LootGlow.vertex(pose, out, from.m_82549_(side.m_82490_((double)(startWidth * edges[i]))), color, startAlpha * opacity[i]);
            LootGlow.vertex(pose, out, from.m_82549_(side.m_82490_((double)(startWidth * edges[i + 1]))), color, startAlpha * opacity[i + 1]);
            LootGlow.vertex(pose, out, to.m_82549_(side.m_82490_((double)(endWidth * edges[i + 1]))), color, endAlpha * opacity[i + 1]);
            LootGlow.vertex(pose, out, to.m_82549_(side.m_82490_((double)(endWidth * edges[i]))), color, endAlpha * opacity[i]);
        }
    }

    private static void vertex(PoseStack pose, VertexConsumer out, Vec3 point, int color, float alpha) {
        out.m_252986_(pose.m_85850_().m_252922_(), (float)point.f_82479_, (float)point.f_82480_, (float)point.f_82481_).m_85950_((float)(color >> 16 & 0xFF) / 255.0f, (float)(color >> 8 & 0xFF) / 255.0f, (float)(color & 0xFF) / 255.0f, alpha).m_5752_();
    }
}


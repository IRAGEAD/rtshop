/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.entity.EntityRendererProvider$Context
 *  net.minecraft.client.renderer.entity.ItemEntityRenderer
 *  net.minecraft.world.entity.item.ItemEntity
 *  net.minecraft.world.phys.Vec3
 */
package com.pla.spiral_abyss.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pla.spiral_abyss.client.renderer.LootGlow;
import com.pla.spiral_abyss.entity.ChestLootEntity;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;

public final class ChestLootRenderer
extends ItemEntityRenderer {
    public ChestLootRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public void m_7392_(ItemEntity item, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        int color;
        super.m_7392_(item, yaw, partialTick, pose, buffers, 0xF000F0);
        if (!(item instanceof ChestLootEntity)) {
            return;
        }
        ChestLootEntity loot = (ChestLootEntity)item;
        Vec3 origin = item.m_20318_(partialTick);
        Vec3 camera = Minecraft.m_91087_().f_91063_.m_109153_().m_90583_().m_82546_(origin);
        VertexConsumer vertices = buffers.m_6299_(LootGlow.TYPE);
        int n = color = loot.rare() ? 16764226 : 3599871;
        if (!loot.m_20096_()) {
            ArrayList<Vec3> points = new ArrayList<Vec3>(loot.trail());
            points.add(origin.m_82520_(0.0, 0.15, 0.0));
            for (int index = 1; index < points.size(); ++index) {
                Vec3 from = points.get(index - 1).m_82546_(origin);
                Vec3 to = points.get(index).m_82546_(origin);
                float a = ((float)index - 1.0f) / (float)points.size();
                float b = (float)index / (float)points.size();
                LootGlow.ribbon(pose, vertices, from, to, camera, 0.16f * a, 0.16f * b, color, 0.6f * a, 0.6f * b);
                LootGlow.ribbon(pose, vertices, from, to, camera, 0.035f * a, 0.035f * b, 0xFFFFFF, 0.9f * a, 0.9f * b);
            }
            return;
        }
        Vec3 base = new Vec3(0.0, 0.16, 0.0);
        Vec3 top = base.m_82520_(0.0, 1.75, 0.0);
        LootGlow.ribbon(pose, vertices, base, top, camera, 0.18f, 0.1f, color, 0.38f, 0.0f);
        LootGlow.ribbon(pose, vertices, base, top, camera, 0.065f, 0.035f, color, 0.85f, 0.0f);
        LootGlow.ribbon(pose, vertices, base, top, camera, 0.018f, 0.01f, 0xFFFFFF, 0.85f, 0.0f);
    }
}


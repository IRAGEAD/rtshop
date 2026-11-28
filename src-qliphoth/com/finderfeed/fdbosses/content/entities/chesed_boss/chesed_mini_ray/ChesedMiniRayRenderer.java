/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  com.finderfeed.fdlib.util.rendering.renderers.QuadRenderer
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.entity.EntityRenderer
 *  net.minecraft.client.renderer.entity.EntityRendererProvider$Context
 *  net.minecraft.client.renderer.texture.TextureAtlas
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.entities.chesed_boss.chesed_mini_ray;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdbosses.content.entities.chesed_boss.chesed_mini_ray.ChesedMiniRay;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import com.finderfeed.fdlib.util.rendering.renderers.QuadRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class ChesedMiniRayRenderer
extends EntityRenderer<ChesedMiniRay> {
    public static final ResourceLocation CHESED_RAY_PREPARE = FDBosses.location("textures/util/chesed_ray_prepare.png");

    public ChesedMiniRayRenderer(EntityRendererProvider.Context p_174008_) {
        super(p_174008_);
    }

    public void render(ChesedMiniRay ray, float yaw, float pticks, PoseStack matrices, MultiBufferSource src, int light) {
        super.m_7392_((Entity)ray, yaw, pticks, matrices, src, light);
        LivingEntity target = ray.getTarget();
        if (target == null) {
            return;
        }
        Vec3 pos = ray.getTargetPos(target, pticks);
        Vec3 thisPos = ray.m_20318_(pticks);
        Vec3 dir = pos.m_82546_(thisPos);
        float size1 = 0.8f;
        float size2 = 0.5f;
        float rotation = 0.0f;
        float alpha = 1.0f;
        int time1 = ray.f_19797_;
        int time2 = time1 - 25;
        if (time2 < 0) {
            elapsedTime = Mth.m_14036_((float)((float)time1 + pticks), (float)0.0f, (float)25.0f);
            float p = elapsedTime / 25.0f;
            alpha = FDEasings.easeOut((float)p);
            rotation = FDEasings.easeOutBack((float)p) * 180.0f;
        } else {
            elapsedTime = Mth.m_14036_((float)((float)time2 + pticks), (float)0.0f, (float)8.0f);
            float p = FDEasings.easeOut((float)(elapsedTime / 8.0f));
            alpha = 1.0f - p;
            size1 += p;
            size2 += p;
        }
        VertexConsumer consumer = src.m_6299_(RenderType.m_110497_((ResourceLocation)CHESED_RAY_PREPARE));
        QuadRenderer.start((VertexConsumer)consumer).light(0xF000F0).color(1.0f, 1.0f, 1.0f, alpha).pose(matrices).size(size1).direction(dir).rotationDegrees(rotation).renderBack().render();
        QuadRenderer.start((VertexConsumer)consumer).light(0xF000F0).color(1.0f, 1.0f, 1.0f, alpha).pose(matrices).size(size2).direction(dir).offsetOnDirection(-0.75f).rotationDegrees(-rotation).renderBack().render();
        QuadRenderer.start((VertexConsumer)consumer).light(0xF000F0).color(1.0f, 1.0f, 1.0f, alpha).pose(matrices).size(size2).direction(dir).offsetOnDirection(0.75f).rotationDegrees(-rotation).renderBack().render();
    }

    public ResourceLocation getTextureLocation(ChesedMiniRay p_114482_) {
        return TextureAtlas.f_118259_;
    }
}


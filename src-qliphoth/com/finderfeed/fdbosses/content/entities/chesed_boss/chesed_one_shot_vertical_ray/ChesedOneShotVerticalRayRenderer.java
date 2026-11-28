/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.math.Axis
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.entity.EntityRenderer
 *  net.minecraft.client.renderer.entity.EntityRendererProvider$Context
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.client.renderer.texture.TextureAtlas
 *  net.minecraft.resources.ResourceLocation
 *  org.joml.Matrix4f
 */
package com.finderfeed.fdbosses.content.entities.chesed_boss.chesed_one_shot_vertical_ray;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdbosses.content.entities.chesed_boss.chesed_one_shot_vertical_ray.ChesedOneShotVerticalRayEntity;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class ChesedOneShotVerticalRayRenderer
extends EntityRenderer<ChesedOneShotVerticalRayEntity> {
    public ChesedOneShotVerticalRayRenderer(EntityRendererProvider.Context p_174008_) {
        super(p_174008_);
    }

    public void render(ChesedOneShotVerticalRayEntity entity, float yaw, float partialTicks, PoseStack matrices, MultiBufferSource src, int light) {
        matrices.m_85836_();
        VertexConsumer vertexConsumer = src.m_6299_(RenderType.m_110488_((ResourceLocation)FDBosses.location("textures/util/circle_and_crosshair.png")));
        float time = (float)entity.f_19797_ + partialTicks;
        float p = time / (float)entity.getAttackPreparationTime();
        matrices.m_252781_(Axis.f_252436_.m_252977_(FDEasings.easeOut((float)p) * 1080.0f));
        p = FDEasings.easeOut((float)p);
        matrices.m_85841_(p * 2.0f, p * 2.0f, p * 2.0f);
        Matrix4f mt = matrices.m_85850_().m_252922_();
        vertexConsumer.m_252986_(mt, -0.5f, 0.05f, 0.5f).m_85950_(0.2f, 0.9f, 1.0f, 1.0f).m_7421_(0.0f, 0.5f).m_86008_(OverlayTexture.f_118083_).m_85969_(light).m_5601_(0.0f, 1.0f, 0.0f).m_5752_();
        vertexConsumer.m_252986_(mt, 0.5f, 0.05f, 0.5f).m_85950_(0.2f, 0.9f, 1.0f, 1.0f).m_7421_(1.0f, 0.5f).m_86008_(OverlayTexture.f_118083_).m_85969_(light).m_5601_(0.0f, 1.0f, 0.0f).m_5752_();
        vertexConsumer.m_252986_(mt, 0.5f, 0.05f, -0.5f).m_85950_(0.2f, 0.9f, 1.0f, 1.0f).m_7421_(1.0f, 0.0f).m_86008_(OverlayTexture.f_118083_).m_85969_(light).m_5601_(0.0f, 1.0f, 0.0f).m_5752_();
        vertexConsumer.m_252986_(mt, -0.5f, 0.05f, -0.5f).m_85950_(0.2f, 0.9f, 1.0f, 1.0f).m_7421_(0.0f, 0.0f).m_86008_(OverlayTexture.f_118083_).m_85969_(light).m_5601_(0.0f, 1.0f, 0.0f).m_5752_();
        vertexConsumer.m_252986_(mt, -0.5f, 0.05f, 0.5f).m_85950_(0.2f, 0.9f, 1.0f, 1.0f).m_7421_(0.0f, 1.0f).m_86008_(OverlayTexture.f_118083_).m_85969_(light).m_5601_(0.0f, 1.0f, 0.0f).m_5752_();
        vertexConsumer.m_252986_(mt, 0.5f, 0.05f, 0.5f).m_85950_(0.2f, 0.9f, 1.0f, 1.0f).m_7421_(1.0f, 1.0f).m_86008_(OverlayTexture.f_118083_).m_85969_(light).m_5601_(0.0f, 1.0f, 0.0f).m_5752_();
        vertexConsumer.m_252986_(mt, 0.5f, 0.05f, -0.5f).m_85950_(0.2f, 0.9f, 1.0f, 1.0f).m_7421_(1.0f, 0.5f).m_86008_(OverlayTexture.f_118083_).m_85969_(light).m_5601_(0.0f, 1.0f, 0.0f).m_5752_();
        vertexConsumer.m_252986_(mt, -0.5f, 0.05f, -0.5f).m_85950_(0.2f, 0.9f, 1.0f, 1.0f).m_7421_(0.0f, 0.5f).m_86008_(OverlayTexture.f_118083_).m_85969_(light).m_5601_(0.0f, 1.0f, 0.0f).m_5752_();
        matrices.m_85849_();
    }

    public ResourceLocation getTextureLocation(ChesedOneShotVerticalRayEntity p_114482_) {
        return TextureAtlas.f_118259_;
    }
}


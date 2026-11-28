/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.util.rendering.renderers.QuadRenderer
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.math.Axis
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.culling.Frustum
 *  net.minecraft.client.renderer.entity.EntityRenderer
 *  net.minecraft.client.renderer.entity.EntityRendererProvider$Context
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.client.renderer.texture.TextureAtlas
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.Mth
 *  org.joml.Matrix4f
 */
package com.finderfeed.fdbosses.content.entities.geburah.geburah_opening_floor;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdbosses.content.entities.geburah.geburah_opening_floor.GeburahOpeningFloor;
import com.finderfeed.fdlib.util.rendering.renderers.QuadRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class GeburahOpeningFloorRenderer
extends EntityRenderer<GeburahOpeningFloor> {
    public static final ResourceLocation STONE = FDBosses.location("textures/block/justicestone_bricks.png");

    public GeburahOpeningFloorRenderer(EntityRendererProvider.Context p_174008_) {
        super(p_174008_);
    }

    public void render(GeburahOpeningFloor floor, float p_114486_, float pticks, PoseStack matrices, MultiBufferSource src, int light) {
        VertexConsumer vertex = src.m_6299_(RenderType.m_110497_((ResourceLocation)STONE));
        int start = 20;
        float time = (float)floor.f_19797_ + pticks;
        float percentOpeness = Mth.m_14036_((float)((time - (float)start) / 80.0f), (float)0.0f, (float)1.0f);
        matrices.m_85836_();
        for (int i = 0; i < 2; ++i) {
            matrices.m_85836_();
            int md = i > 0 ? -1 : 1;
            float t = time * 2.0f;
            matrices.m_252781_(Axis.f_252436_.m_252977_((float)(i * 180)));
            if (time > (float)start) {
                float k = 0.02f * (float)md;
                matrices.m_85837_(Math.sin(t) * (double)k, Math.sin(t + (float)Math.PI) * (double)k, Math.cos(t + 0.7853982f) * (double)k);
            }
            float openess = percentOpeness * 5.5f;
            Matrix4f mat = matrices.m_85850_().m_252922_();
            float openess1 = 5.5f - openess;
            vertex.m_252986_(mat, 2.5f, 0.25f, openess1 + openess).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(0.0f, (float)md * openess1).m_85969_(light).m_86008_(OverlayTexture.f_118083_).m_5752_();
            vertex.m_252986_(mat, 2.5f, 0.25f, openess).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(0.0f, 0.0f).m_85969_(light).m_86008_(OverlayTexture.f_118083_).m_5752_();
            vertex.m_252986_(mat, -2.5f, 0.25f, openess).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_((float)(5 * md), 0.0f).m_85969_(light).m_86008_(OverlayTexture.f_118083_).m_5752_();
            vertex.m_252986_(mat, -2.5f, 0.25f, openess1 + openess).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_((float)(5 * md), (float)md * openess1).m_85969_(light).m_86008_(OverlayTexture.f_118083_).m_5752_();
            float openess2 = Mth.m_14036_((float)(4.5f - openess), (float)0.0f, (float)4.5f);
            float offs2 = 3.0f;
            vertex.m_252986_(mat, 0.5f + offs2, 0.25f, openess2 + openess).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(0.0f, (float)md * openess2).m_85969_(light).m_86008_(OverlayTexture.f_118083_).m_5752_();
            vertex.m_252986_(mat, 0.5f + offs2, 0.25f, openess).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(0.0f, 0.0f).m_85969_(light).m_86008_(OverlayTexture.f_118083_).m_5752_();
            vertex.m_252986_(mat, -0.5f + offs2, 0.25f, openess).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_((float)md, 0.0f).m_85969_(light).m_86008_(OverlayTexture.f_118083_).m_5752_();
            vertex.m_252986_(mat, -0.5f + offs2, 0.25f, openess2 + openess).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_((float)md, (float)md * openess2).m_85969_(light).m_86008_(OverlayTexture.f_118083_).m_5752_();
            vertex.m_252986_(mat, 0.5f - offs2, 0.25f, openess2 + openess).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(0.0f, (float)md * openess2).m_85969_(light).m_86008_(OverlayTexture.f_118083_).m_5752_();
            vertex.m_252986_(mat, 0.5f - offs2, 0.25f, openess).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(0.0f, 0.0f).m_85969_(light).m_86008_(OverlayTexture.f_118083_).m_5752_();
            vertex.m_252986_(mat, -0.5f - offs2, 0.25f, openess).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_((float)md, 0.0f).m_85969_(light).m_86008_(OverlayTexture.f_118083_).m_5752_();
            vertex.m_252986_(mat, -0.5f - offs2, 0.25f, openess2 + openess).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_((float)md, (float)md * openess2).m_85969_(light).m_86008_(OverlayTexture.f_118083_).m_5752_();
            float openess3 = Mth.m_14036_((float)(3.5f - openess), (float)0.0f, (float)3.5f);
            float offs3 = 4.0f;
            vertex.m_252986_(mat, 0.5f + offs3, 0.25f, openess3 + openess).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(0.0f, (float)md * openess3).m_85969_(light).m_86008_(OverlayTexture.f_118083_).m_5752_();
            vertex.m_252986_(mat, 0.5f + offs3, 0.25f, openess).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(0.0f, 0.0f).m_85969_(light).m_86008_(OverlayTexture.f_118083_).m_5752_();
            vertex.m_252986_(mat, -0.5f + offs3, 0.25f, openess).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_((float)md, 0.0f).m_85969_(light).m_86008_(OverlayTexture.f_118083_).m_5752_();
            vertex.m_252986_(mat, -0.5f + offs3, 0.25f, openess3 + openess).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_((float)md, (float)md * openess3).m_85969_(light).m_86008_(OverlayTexture.f_118083_).m_5752_();
            vertex.m_252986_(mat, 0.5f - offs3, 0.25f, openess3 + openess).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(0.0f, (float)md * openess3).m_85969_(light).m_86008_(OverlayTexture.f_118083_).m_5752_();
            vertex.m_252986_(mat, 0.5f - offs3, 0.25f, openess).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(0.0f, 0.0f).m_85969_(light).m_86008_(OverlayTexture.f_118083_).m_5752_();
            vertex.m_252986_(mat, -0.5f - offs3, 0.25f, openess).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_((float)md, 0.0f).m_85969_(light).m_86008_(OverlayTexture.f_118083_).m_5752_();
            vertex.m_252986_(mat, -0.5f - offs3, 0.25f, openess3 + openess).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_((float)md, (float)md * openess3).m_85969_(light).m_86008_(OverlayTexture.f_118083_).m_5752_();
            float openess4 = Mth.m_14036_((float)(2.5f - openess), (float)0.0f, (float)2.5f);
            float offs4 = 5.0f;
            vertex.m_252986_(mat, 0.5f + offs4, 0.25f, openess4 + openess).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(0.0f, (float)md * openess4).m_85969_(light).m_86008_(OverlayTexture.f_118083_).m_5752_();
            vertex.m_252986_(mat, 0.5f + offs4, 0.25f, 0.0f + openess).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(0.0f, 0.0f).m_85969_(light).m_86008_(OverlayTexture.f_118083_).m_5752_();
            vertex.m_252986_(mat, -0.5f + offs4, 0.25f, 0.0f + openess).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_((float)md, 0.0f).m_85969_(light).m_86008_(OverlayTexture.f_118083_).m_5752_();
            vertex.m_252986_(mat, -0.5f + offs4, 0.25f, openess4 + openess).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_((float)md, (float)md * openess4).m_85969_(light).m_86008_(OverlayTexture.f_118083_).m_5752_();
            vertex.m_252986_(mat, 0.5f - offs4, 0.25f, openess4 + openess).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(0.0f, (float)md * openess4).m_85969_(light).m_86008_(OverlayTexture.f_118083_).m_5752_();
            vertex.m_252986_(mat, 0.5f - offs4, 0.25f, 0.0f + openess).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(0.0f, 0.0f).m_85969_(light).m_86008_(OverlayTexture.f_118083_).m_5752_();
            vertex.m_252986_(mat, -0.5f - offs4, 0.25f, 0.0f + openess).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_((float)md, 0.0f).m_85969_(light).m_86008_(OverlayTexture.f_118083_).m_5752_();
            vertex.m_252986_(mat, -0.5f - offs4, 0.25f, openess4 + openess).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_((float)md, (float)md * openess4).m_85969_(light).m_86008_(OverlayTexture.f_118083_).m_5752_();
            matrices.m_85849_();
        }
        matrices.m_85849_();
        VertexConsumer c = src.m_6299_(RenderType.m_110497_((ResourceLocation)FDBosses.location("textures/misc/black_dot.png")));
        QuadRenderer.start((VertexConsumer)c).translate(0.0f, 0.125f, 0.0f).pose(matrices).sizeX(2.5f).sizeY(5.5f).render();
        QuadRenderer.start((VertexConsumer)c).translate(3.0f, 0.125f, 0.0f).pose(matrices).sizeX(0.5f).sizeY(4.5f).render();
        QuadRenderer.start((VertexConsumer)c).translate(-3.0f, 0.125f, 0.0f).pose(matrices).sizeX(0.5f).sizeY(4.5f).render();
        QuadRenderer.start((VertexConsumer)c).translate(4.0f, 0.125f, 0.0f).pose(matrices).sizeX(0.5f).sizeY(3.5f).render();
        QuadRenderer.start((VertexConsumer)c).translate(-4.0f, 0.125f, 0.0f).pose(matrices).sizeX(0.5f).sizeY(3.5f).render();
        QuadRenderer.start((VertexConsumer)c).translate(5.0f, 0.125f, 0.0f).pose(matrices).sizeX(0.5f).sizeY(2.5f).render();
        QuadRenderer.start((VertexConsumer)c).translate(-5.0f, 0.125f, 0.0f).pose(matrices).sizeX(0.5f).sizeY(2.5f).render();
    }

    public ResourceLocation getTextureLocation(GeburahOpeningFloor p_114482_) {
        return TextureAtlas.f_118259_;
    }

    public boolean shouldRender(GeburahOpeningFloor p_114491_, Frustum p_114492_, double p_114493_, double p_114494_, double p_114495_) {
        return true;
    }
}


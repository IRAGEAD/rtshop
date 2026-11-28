/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.math.Axis
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.culling.Frustum
 *  net.minecraft.client.renderer.entity.EntityRenderer
 *  net.minecraft.client.renderer.entity.EntityRendererProvider$Context
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix4f
 */
package com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_chain;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_chain.MalkuthChainEntity;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class MalkuthChainRenderer
extends EntityRenderer<MalkuthChainEntity> {
    public MalkuthChainRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    public void render(MalkuthChainEntity chain, float yaw, float pticks, PoseStack matrices, MultiBufferSource src, int light) {
        matrices.m_85836_();
        Vec3 handPos = chain.getMalkuthHandPos(pticks);
        if (handPos == null) {
            matrices.m_85849_();
            return;
        }
        Vec3 pos = chain.m_20182_();
        Vec3 between = handPos.m_82546_(pos);
        FDRenderUtil.applyMovementMatrixRotations((PoseStack)matrices, (Vec3)between);
        VertexConsumer b = src.m_6299_(RenderType.m_110497_((ResourceLocation)this.getTextureLocation(chain)));
        float length = (float)between.m_82553_();
        Matrix4f m = matrices.m_85850_().m_252922_();
        float width = 0.2f;
        if (!chain.m_20197_().isEmpty()) {
            matrices.m_252781_(Axis.f_252436_.m_252977_(-135.0f));
        }
        b.m_252986_(m, -width, 0.0f, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(0.0f, 0.0f).m_86008_(OverlayTexture.f_118083_).m_85969_(light).m_5752_();
        b.m_252986_(m, -width, length, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(length * 4.0f, 0.0f).m_86008_(OverlayTexture.f_118083_).m_85969_(light).m_5752_();
        b.m_252986_(m, width, length, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(length * 4.0f, 1.0f).m_86008_(OverlayTexture.f_118083_).m_85969_(light).m_5752_();
        b.m_252986_(m, width, 0.0f, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(0.0f, 1.0f).m_86008_(OverlayTexture.f_118083_).m_85969_(light).m_5752_();
        b.m_252986_(m, width, 0.0f, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(0.0f, 1.0f).m_86008_(OverlayTexture.f_118083_).m_85969_(light).m_5752_();
        b.m_252986_(m, width, length, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(length * 4.0f, 1.0f).m_86008_(OverlayTexture.f_118083_).m_85969_(light).m_5752_();
        b.m_252986_(m, -width, length, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(length * 4.0f, 0.0f).m_86008_(OverlayTexture.f_118083_).m_85969_(light).m_5752_();
        b.m_252986_(m, -width, 0.0f, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(0.0f, 0.0f).m_86008_(OverlayTexture.f_118083_).m_85969_(light).m_5752_();
        b.m_252986_(m, 0.0f, 0.0f, -width).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(0.0f, 0.0f).m_86008_(OverlayTexture.f_118083_).m_85969_(light).m_5752_();
        b.m_252986_(m, 0.0f, length, -width).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(length * 4.0f, 0.0f).m_86008_(OverlayTexture.f_118083_).m_85969_(light).m_5752_();
        b.m_252986_(m, 0.0f, length, width).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(length * 4.0f, 1.0f).m_86008_(OverlayTexture.f_118083_).m_85969_(light).m_5752_();
        b.m_252986_(m, 0.0f, 0.0f, width).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(0.0f, 1.0f).m_86008_(OverlayTexture.f_118083_).m_85969_(light).m_5752_();
        b.m_252986_(m, 0.0f, 0.0f, width).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(0.0f, 1.0f).m_86008_(OverlayTexture.f_118083_).m_85969_(light).m_5752_();
        b.m_252986_(m, 0.0f, length, width).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(length * 4.0f, 1.0f).m_86008_(OverlayTexture.f_118083_).m_85969_(light).m_5752_();
        b.m_252986_(m, 0.0f, length, -width).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(length * 4.0f, 0.0f).m_86008_(OverlayTexture.f_118083_).m_85969_(light).m_5752_();
        b.m_252986_(m, 0.0f, 0.0f, -width).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(0.0f, 0.0f).m_86008_(OverlayTexture.f_118083_).m_85969_(light).m_5752_();
        matrices.m_85849_();
    }

    public ResourceLocation getTextureLocation(MalkuthChainEntity p_114482_) {
        return FDBosses.location("textures/util/chain_segment.png");
    }

    public boolean shouldRender(MalkuthChainEntity p_114491_, Frustum p_114492_, double p_114493_, double p_114494_, double p_114495_) {
        return true;
    }
}


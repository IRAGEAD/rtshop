/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.renderer.FDFreeEntityRenderer
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix4f
 */
package com.finderfeed.fdbosses.content.items.malkuth;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdbosses.content.items.malkuth.MalkuthFistChain;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.renderer.FDFreeEntityRenderer;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class MalkuthFistChainRenderer
implements FDFreeEntityRenderer<MalkuthFistChain> {
    public static final ResourceLocation TEXTURE = FDBosses.location("textures/entities/malkuth/malkuth_fist_chain_segment.png");

    public void render(MalkuthFistChain malkuthFistChain, float yaw, float pticks, PoseStack matrices, MultiBufferSource multiBufferSource, int i) {
        int ownerId = (Integer)malkuthFistChain.m_20088_().m_135370_(MalkuthFistChain.OWNER_ID);
        Entity entity = malkuthFistChain.m_9236_().m_6815_(ownerId);
        if (!(entity instanceof Player)) {
            return;
        }
        Player player = (Player)entity;
        Vec3 playerPos = player.m_20318_(pticks).m_82520_(0.0, (double)(player.m_20206_() / 2.0f) + 0.25, 0.0);
        Vec3 thisPos = malkuthFistChain.m_20318_(pticks);
        Vec3 between = playerPos.m_82546_(thisPos);
        float length = (float)between.m_82553_();
        matrices.m_85836_();
        VertexConsumer vertex = multiBufferSource.m_6299_(RenderType.m_110458_((ResourceLocation)TEXTURE));
        FDRenderUtil.applyMovementMatrixRotations((PoseStack)matrices, (Vec3)between);
        Matrix4f mat = matrices.m_85850_().m_252922_();
        float segmentWidth = 0.05f;
        float ratio = 3.5f;
        vertex.m_252986_(mat, -segmentWidth, 0.0f, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(0.0f, 0.0f).m_86008_(OverlayTexture.f_118083_).m_85969_(i).m_252939_(matrices.m_85850_().m_252943_(), 0.0f, 0.0f, 1.0f).m_5752_();
        vertex.m_252986_(mat, -segmentWidth, length, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(0.0f, ratio * length).m_86008_(OverlayTexture.f_118083_).m_85969_(i).m_252939_(matrices.m_85850_().m_252943_(), 0.0f, 0.0f, 1.0f).m_5752_();
        vertex.m_252986_(mat, segmentWidth, length, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(1.0f, ratio * length).m_86008_(OverlayTexture.f_118083_).m_85969_(i).m_252939_(matrices.m_85850_().m_252943_(), 0.0f, 0.0f, 1.0f).m_5752_();
        vertex.m_252986_(mat, segmentWidth, 0.0f, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(1.0f, 0.0f).m_86008_(OverlayTexture.f_118083_).m_85969_(i).m_252939_(matrices.m_85850_().m_252943_(), 0.0f, 0.0f, 1.0f).m_5752_();
        vertex.m_252986_(mat, 0.0f, 0.0f, -segmentWidth).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(0.0f, 0.0f).m_86008_(OverlayTexture.f_118083_).m_85969_(i).m_252939_(matrices.m_85850_().m_252943_(), 1.0f, 0.0f, 0.0f).m_5752_();
        vertex.m_252986_(mat, 0.0f, length, -segmentWidth).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(0.0f, ratio * length).m_86008_(OverlayTexture.f_118083_).m_85969_(i).m_252939_(matrices.m_85850_().m_252943_(), 1.0f, 0.0f, 0.0f).m_5752_();
        vertex.m_252986_(mat, 0.0f, length, segmentWidth).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(1.0f, ratio * length).m_86008_(OverlayTexture.f_118083_).m_85969_(i).m_252939_(matrices.m_85850_().m_252943_(), 1.0f, 0.0f, 0.0f).m_5752_();
        vertex.m_252986_(mat, 0.0f, 0.0f, segmentWidth).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(1.0f, 0.0f).m_86008_(OverlayTexture.f_118083_).m_85969_(i).m_252939_(matrices.m_85850_().m_252943_(), 1.0f, 0.0f, 0.0f).m_5752_();
        matrices.m_85849_();
    }
}


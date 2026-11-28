/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
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
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix4f
 */
package com.finderfeed.fdbosses.content.entities.geburah.chain_trap;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdbosses.content.entities.geburah.chain_trap.GeburahChainTrapEntity;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class GeburahChainTrapRenderer
extends EntityRenderer<GeburahChainTrapEntity> {
    public static ResourceLocation CHAIN = FDBosses.location("textures/entities/geburah/geburah_chain.png");
    public static ResourceLocation CHAIN_TRAP = FDBosses.location("textures/entities/geburah/geburah_chain_trap.png");

    public GeburahChainTrapRenderer(EntityRendererProvider.Context p_174008_) {
        super(p_174008_);
    }

    public void render(GeburahChainTrapEntity trap, float yaw, float pticks, PoseStack matrices, MultiBufferSource src, int light) {
        matrices.m_85836_();
        QuadRenderer.start((VertexConsumer)src.m_6299_(RenderType.m_110497_((ResourceLocation)CHAIN_TRAP))).renderBack().size(0.5f).translate(0.0f, 0.01f, 0.0f).rotationDegrees(((float)trap.f_19797_ + pticks) / 2.0f).pose(matrices).render();
        this.renderChainCircle(trap, yaw, pticks, matrices, src, light);
        this.renderCatching(trap, yaw, pticks, matrices, src, light);
        matrices.m_85849_();
    }

    public void renderCatching(GeburahChainTrapEntity trap, float yaw, float pticks, PoseStack matrices, MultiBufferSource src, int light) {
        float catchingChainsPercent = -1.0f;
        float restrictingChainsPercent = -1.0f;
        Vec3 center = null;
        float squareRadius = -1.0f;
        if (!trap.m_20197_().isEmpty()) {
            entity = (Entity)trap.m_20197_().get(0);
            squareRadius = entity.m_20205_();
            center = entity.m_20318_(pticks).m_82520_(0.0, (double)(entity.m_20206_() / 2.0f), 0.0);
            catchingChainsPercent = 1.0f;
            restrictingChainsPercent = 1.0f - Mth.m_14036_((float)(((float)trap.pullingTime - pticks) / (float)GeburahChainTrapEntity.PULL_TIME), (float)0.0f, (float)1.0f);
        } else {
            entity = trap.getEntityAboutToTrap();
            if (entity != null) {
                squareRadius = entity.m_20205_();
                center = entity.m_20318_(pticks).m_82520_(0.0, (double)(entity.m_20206_() / 2.0f), 0.0);
                catchingChainsPercent = 1.0f - Mth.m_14036_((float)(((float)trap.catchingTime - pticks) / (float)GeburahChainTrapEntity.PULL_TIME), (float)0.0f, (float)1.0f);
            }
        }
        if (center == null) {
            return;
        }
        if (catchingChainsPercent != -1.0f) {
            float catchingChainOffset = 0.425f;
            catchingChainsPercent = FDEasings.easeOut((float)catchingChainsPercent);
            this.renderChainFromPosToPos(trap, matrices, src, trap.m_20182_().m_82520_((double)(-catchingChainOffset), 0.0, (double)(-catchingChainOffset)), center.m_82520_((double)(-squareRadius / 2.0f), 0.0, (double)(-squareRadius / 2.0f)), catchingChainsPercent);
            this.renderChainFromPosToPos(trap, matrices, src, trap.m_20182_().m_82520_((double)catchingChainOffset, 0.0, (double)catchingChainOffset), center.m_82520_((double)(squareRadius / 2.0f), 0.0, (double)(squareRadius / 2.0f)), catchingChainsPercent);
            this.renderChainFromPosToPos(trap, matrices, src, trap.m_20182_().m_82520_((double)catchingChainOffset, 0.0, (double)(-catchingChainOffset)), center.m_82520_((double)(squareRadius / 2.0f), 0.0, (double)(-squareRadius / 2.0f)), catchingChainsPercent);
            this.renderChainFromPosToPos(trap, matrices, src, trap.m_20182_().m_82520_((double)(-catchingChainOffset), 0.0, (double)catchingChainOffset), center.m_82520_((double)(-squareRadius / 2.0f), 0.0, (double)(squareRadius / 2.0f)), catchingChainsPercent);
        }
        if (restrictingChainsPercent != -1.0f) {
            this.renderChainFromPosToPos(trap, matrices, src, center.m_82520_((double)(-squareRadius / 2.0f), 0.0, (double)(squareRadius / 2.0f)), center.m_82520_((double)(-squareRadius / 2.0f), 0.0, (double)(-squareRadius / 2.0f)), restrictingChainsPercent);
            this.renderChainFromPosToPos(trap, matrices, src, center.m_82520_((double)(squareRadius / 2.0f), 0.0, (double)(-squareRadius / 2.0f)), center.m_82520_((double)(squareRadius / 2.0f), 0.0, (double)(squareRadius / 2.0f)), restrictingChainsPercent);
            this.renderChainFromPosToPos(trap, matrices, src, center.m_82520_((double)(-squareRadius / 2.0f), 0.0, (double)(-squareRadius / 2.0f)), center.m_82520_((double)(squareRadius / 2.0f), 0.0, (double)(-squareRadius / 2.0f)), restrictingChainsPercent);
            this.renderChainFromPosToPos(trap, matrices, src, center.m_82520_((double)(squareRadius / 2.0f), 0.0, (double)(squareRadius / 2.0f)), center.m_82520_((double)(-squareRadius / 2.0f), 0.0, (double)(squareRadius / 2.0f)), restrictingChainsPercent);
        }
    }

    public void renderChainFromPosToPos(GeburahChainTrapEntity trap, PoseStack matrices, MultiBufferSource src, Vec3 pos1, Vec3 pos2, float p) {
        Vec3 offset1 = pos1.m_82546_(trap.m_20182_());
        Vec3 between = pos2.m_82546_(pos1);
        VertexConsumer vertex = src.m_6299_(RenderType.m_110497_((ResourceLocation)CHAIN));
        matrices.m_85836_();
        float length = (float)between.m_82553_() * p;
        float size = 0.2f;
        float xSize = size * 0.6f / 2.0f;
        matrices.m_85837_(offset1.f_82479_, offset1.f_82480_, offset1.f_82481_);
        FDRenderUtil.applyMovementMatrixRotations((PoseStack)matrices, (Vec3)between);
        float v = length / size;
        Matrix4f m = matrices.m_85850_().m_252922_();
        matrices.m_252781_(Axis.f_252436_.m_252977_(90.0f));
        m = matrices.m_85850_().m_252922_();
        vertex.m_252986_(m, xSize, 0.0f, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(1.0f, 0.0f).m_86008_(OverlayTexture.f_118083_).m_85969_(0xF000F0).m_252939_(matrices.m_85850_().m_252943_(), 0.0f, 0.0f, 1.0f).m_5752_();
        vertex.m_252986_(m, xSize, length, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(1.0f, v).m_86008_(OverlayTexture.f_118083_).m_85969_(0xF000F0).m_252939_(matrices.m_85850_().m_252943_(), 0.0f, 0.0f, 1.0f).m_5752_();
        vertex.m_252986_(m, -xSize, length, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(0.0f, v).m_86008_(OverlayTexture.f_118083_).m_85969_(0xF000F0).m_252939_(matrices.m_85850_().m_252943_(), 0.0f, 0.0f, 1.0f).m_5752_();
        vertex.m_252986_(m, -xSize, 0.0f, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(0.0f, 0.0f).m_86008_(OverlayTexture.f_118083_).m_85969_(0xF000F0).m_252939_(matrices.m_85850_().m_252943_(), 0.0f, 0.0f, 1.0f).m_5752_();
        vertex.m_252986_(m, -xSize, 0.0f, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(0.0f, 0.0f).m_86008_(OverlayTexture.f_118083_).m_85969_(0xF000F0).m_252939_(matrices.m_85850_().m_252943_(), 0.0f, 0.0f, 1.0f).m_5752_();
        vertex.m_252986_(m, -xSize, length, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(0.0f, v).m_86008_(OverlayTexture.f_118083_).m_85969_(0xF000F0).m_252939_(matrices.m_85850_().m_252943_(), 0.0f, 0.0f, 1.0f).m_5752_();
        vertex.m_252986_(m, xSize, length, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(1.0f, v).m_86008_(OverlayTexture.f_118083_).m_85969_(0xF000F0).m_252939_(matrices.m_85850_().m_252943_(), 0.0f, 0.0f, 1.0f).m_5752_();
        vertex.m_252986_(m, xSize, 0.0f, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_(1.0f, 0.0f).m_86008_(OverlayTexture.f_118083_).m_85969_(0xF000F0).m_252939_(matrices.m_85850_().m_252943_(), 0.0f, 0.0f, 1.0f).m_5752_();
        matrices.m_85849_();
    }

    public void renderChainCircle(GeburahChainTrapEntity trap, float yaw, float pticks, PoseStack matrices, MultiBufferSource src, int light) {
        matrices.m_85836_();
        VertexConsumer vertex = src.m_6299_(RenderType.m_110497_((ResourceLocation)CHAIN));
        float time = (float)trap.f_19797_ + pticks;
        time = -time * 5.0f;
        float offset = 0.6f;
        float size = 0.12f;
        int count = 16;
        float angle = 360.0f / (float)count;
        for (int i = 0; i < count; ++i) {
            matrices.m_85836_();
            matrices.m_252880_(0.0f, 0.1f, 0.0f);
            matrices.m_252781_(Axis.f_252436_.m_252977_(angle * (float)i + time));
            matrices.m_252880_(offset, 0.0f, 0.0f);
            matrices.m_252781_(Axis.f_252403_.m_252977_(-45.0f));
            QuadRenderer.start((VertexConsumer)vertex).pose(matrices).renderBack().size(size).sizeX(size * 0.6f).render();
            matrices.m_85849_();
        }
        matrices.m_85849_();
    }

    public boolean shouldRender(GeburahChainTrapEntity p_114491_, Frustum p_114492_, double p_114493_, double p_114494_, double p_114495_) {
        return true;
    }

    public ResourceLocation getTextureLocation(GeburahChainTrapEntity p_114482_) {
        return TextureAtlas.f_118259_;
    }
}


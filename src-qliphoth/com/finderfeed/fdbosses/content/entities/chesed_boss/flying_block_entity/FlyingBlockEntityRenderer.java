/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.block.BlockRenderDispatcher
 *  net.minecraft.client.renderer.entity.EntityRenderer
 *  net.minecraft.client.renderer.entity.EntityRendererProvider$Context
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.client.renderer.texture.TextureAtlas
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.client.model.data.ModelData
 *  org.joml.Vector3f
 */
package com.finderfeed.fdbosses.content.entities.chesed_boss.flying_block_entity;

import com.finderfeed.fdbosses.content.entities.chesed_boss.flying_block_entity.FlyingBlockEntity;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.data.ModelData;
import org.joml.Vector3f;

public class FlyingBlockEntityRenderer
extends EntityRenderer<FlyingBlockEntity> {
    public FlyingBlockEntityRenderer(EntityRendererProvider.Context p_174008_) {
        super(p_174008_);
    }

    public void render(FlyingBlockEntity entity, float idk, float partialTicks, PoseStack matrices, MultiBufferSource src, int light) {
        matrices.m_85836_();
        BlockState state = entity.getBlockState();
        BlockRenderDispatcher renderer = Minecraft.m_91087_().m_91289_();
        matrices.m_85837_(-0.5, 0.0, -0.5);
        matrices.m_85837_(0.5, 0.5, 0.5);
        Vec3 horizontal = entity.m_20184_().m_82542_(1.0, 0.0, 1.0).m_82541_();
        FDRenderUtil.applyMovementMatrixRotations((PoseStack)matrices, (Vec3)horizontal);
        float time = ((float)entity.f_19797_ + partialTicks) * entity.getRotationSpeed() + (float)(entity.m_19879_() % 500 * 323);
        matrices.m_252781_(FDRenderUtil.rotationDegrees((Vector3f)FDRenderUtil.XP(), (float)time));
        matrices.m_85837_(-0.5, -0.5, -0.5);
        renderer.renderSingleBlock(state, matrices, src, light, OverlayTexture.f_118083_, ModelData.EMPTY, null);
        matrices.m_85849_();
    }

    public ResourceLocation getTextureLocation(FlyingBlockEntity p_114482_) {
        return TextureAtlas.f_118259_;
    }
}


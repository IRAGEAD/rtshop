/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.math.Axis
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.block.BlockRenderDispatcher
 *  net.minecraft.client.renderer.entity.EntityRenderer
 *  net.minecraft.client.renderer.entity.EntityRendererProvider$Context
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.client.renderer.texture.TextureAtlas
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraftforge.client.model.data.ModelData
 *  org.joml.Vector3f
 */
package com.finderfeed.fdbosses.content.entities.chesed_boss.falling_block;

import com.finderfeed.fdbosses.content.entities.chesed_boss.falling_block.ChesedFallingBlock;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.joml.Vector3f;

public class ChesedFallingBlockRenderer
extends EntityRenderer<ChesedFallingBlock> {
    public ChesedFallingBlockRenderer(EntityRendererProvider.Context p_174008_) {
        super(p_174008_);
    }

    public void render(ChesedFallingBlock entity, float idk, float partialTicks, PoseStack matrices, MultiBufferSource src, int light) {
        matrices.m_85836_();
        BlockState state = entity.getBlockState();
        BlockRenderDispatcher renderer = Minecraft.m_91087_().m_91289_();
        matrices.m_85837_(-0.5, -0.5, -0.5);
        matrices.m_85837_(0.5, 0.5, 0.5);
        matrices.m_252781_(Axis.f_252436_.m_252977_((float)(entity.m_19879_() % 500 * 432)));
        float time = (float)entity.f_19797_ + partialTicks + (float)(entity.m_19879_() % 500 * 323);
        matrices.m_252781_(FDRenderUtil.rotationDegrees((Vector3f)FDRenderUtil.XP(), (float)(time * 4.0f)));
        matrices.m_85837_(-0.5, -0.5, -0.5);
        renderer.renderSingleBlock(state, matrices, src, light, OverlayTexture.f_118083_, ModelData.EMPTY, null);
        matrices.m_85849_();
    }

    public ResourceLocation getTextureLocation(ChesedFallingBlock p_114482_) {
        return TextureAtlas.f_118259_;
    }
}


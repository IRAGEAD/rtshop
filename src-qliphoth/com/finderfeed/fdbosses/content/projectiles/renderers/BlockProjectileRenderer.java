/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.LightTexture
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.block.BlockRenderDispatcher
 *  net.minecraft.client.renderer.entity.EntityRenderer
 *  net.minecraft.client.renderer.entity.EntityRendererProvider$Context
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.client.renderer.texture.TextureAtlas
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraftforge.client.model.data.ModelData
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 */
package com.finderfeed.fdbosses.content.projectiles.renderers;

import com.finderfeed.fdbosses.content.projectiles.ChesedBlockProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

public class BlockProjectileRenderer
extends EntityRenderer<ChesedBlockProjectile> {
    public BlockProjectileRenderer(EntityRendererProvider.Context p_174008_) {
        super(p_174008_);
    }

    public void render(ChesedBlockProjectile entity, float yaw, float pticks, PoseStack matrices, MultiBufferSource src, int light) {
        BlockState state = entity.getBlockState();
        BlockPos pos = entity.m_20097_().m_6630_(2);
        light = LightTexture.m_109885_((int)this.m_6086_((Entity)entity, pos), (int)this.m_114508_((Entity)entity, pos));
        BlockRenderDispatcher renderer = Minecraft.m_91087_().m_91289_();
        matrices.m_85836_();
        matrices.m_85837_(-0.5, 0.0, -0.5);
        matrices.m_85837_(0.5, 0.5, 0.5);
        matrices.m_252781_(entity.previousRotation.slerp((Quaternionfc)entity.currentRotation, pticks, new Quaternionf()));
        matrices.m_85837_(-0.5, -0.5, -0.5);
        renderer.renderSingleBlock(state, matrices, src, light, OverlayTexture.f_118083_, ModelData.EMPTY, null);
        matrices.m_85849_();
    }

    public ResourceLocation getTextureLocation(ChesedBlockProjectile p_114482_) {
        return TextureAtlas.f_118259_;
    }
}


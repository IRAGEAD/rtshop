/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.util.math.ComplexEasingFunction
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.LightTexture
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.block.BlockRenderDispatcher
 *  net.minecraft.client.renderer.culling.Frustum
 *  net.minecraft.client.renderer.entity.EntityRenderer
 *  net.minecraft.client.renderer.entity.EntityRendererProvider$Context
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.client.renderer.texture.TextureAtlas
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.client.model.data.ModelData
 */
package com.finderfeed.fdbosses.content.entities.chesed_boss.earthshatter_entity;

import com.finderfeed.fdbosses.content.entities.chesed_boss.earthshatter_entity.EarthShatterEntity;
import com.finderfeed.fdbosses.content.entities.chesed_boss.earthshatter_entity.EarthShatterSettings;
import com.finderfeed.fdlib.util.math.ComplexEasingFunction;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.data.ModelData;

public class EarthShatterRenderer
extends EntityRenderer<EarthShatterEntity> {
    public EarthShatterRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    public void render(EarthShatterEntity entity, float idk, float pticks, PoseStack matrices, MultiBufferSource src, int light) {
        BlockRenderDispatcher renderer = Minecraft.m_91087_().m_91289_();
        BlockState state = entity.getBlockState();
        EarthShatterSettings settings = (EarthShatterSettings)entity.m_20088_().m_135370_(EarthShatterEntity.EARTH_SHATTER_SETTINGS);
        if (entity.f_19797_ < settings.delay) {
            return;
        }
        matrices.m_85836_();
        ComplexEasingFunction function = ComplexEasingFunction.builder().addArea((float)settings.upTime, FDEasings::easeOut).addArea((float)settings.stayTime, f -> Float.valueOf(1.0f)).addArea((float)settings.downTime, FDEasings::reversedEaseOut).build();
        float p = function.apply((float)entity.f_19797_ + pticks - (float)settings.delay);
        Vec3 dir = entity.getShatterDirection();
        Vec3 init = new Vec3(0.0, 1.0, 0.0);
        dir = new Vec3((double)FDMathUtil.lerp((float)((float)init.f_82479_), (float)((float)dir.f_82479_), (float)p), (double)FDMathUtil.lerp((float)((float)init.f_82480_), (float)((float)dir.f_82480_), (float)p), (double)FDMathUtil.lerp((float)((float)init.f_82481_), (float)((float)dir.f_82481_), (float)p));
        matrices.m_85837_(-0.5, (double)(p * settings.upDistance), -0.5);
        matrices.m_85837_(0.5, 0.5, 0.5);
        matrices.m_85841_(1.1f, 1.1f, 1.1f);
        FDRenderUtil.applyMovementMatrixRotations((PoseStack)matrices, (Vec3)dir);
        matrices.m_85837_(-0.5, -0.5, -0.5);
        BlockPos pos = entity.m_20097_().m_6630_(2);
        light = LightTexture.m_109885_((int)this.m_6086_(entity, pos), (int)this.m_114508_(entity, pos));
        renderer.renderSingleBlock(state, matrices, src, light, OverlayTexture.f_118083_, ModelData.EMPTY, null);
        matrices.m_85849_();
    }

    public ResourceLocation getTextureLocation(EarthShatterEntity p_114482_) {
        return TextureAtlas.f_118259_;
    }

    public boolean shouldRender(EarthShatterEntity p_114491_, Frustum p_114492_, double p_114493_, double p_114494_, double p_114495_) {
        return super.m_5523_((Entity)p_114491_, p_114492_, p_114493_, p_114494_, p_114495_);
    }
}


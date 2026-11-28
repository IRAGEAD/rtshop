/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.bedrock.models.FDModel
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.math.Axis
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.block.BlockRenderDispatcher
 *  net.minecraft.client.renderer.entity.EntityRenderer
 *  net.minecraft.client.renderer.entity.EntityRendererProvider$Context
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.client.model.data.ModelData
 */
package com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_cannon;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_cannon.MalkuthCannonProjectile;
import com.finderfeed.fdbosses.init.BossModels;
import com.finderfeed.fdlib.systems.bedrock.models.FDModel;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.data.ModelData;

public class MalkuthCannonProjectileRenderer
extends EntityRenderer<MalkuthCannonProjectile> {
    public static final ResourceLocation FIRE_TEXTURE = FDBosses.location("textures/entities/malkuth/malkuth_cannon_fire_projectile.png");
    public static final ResourceLocation ICE_TEXTURE = FDBosses.location("textures/entities/malkuth/malkuth_cannon_ice_projectile.png");
    private final FDModel model = new FDModel(BossModels.MALKUTH_CANNON_PROJECTILE.get());

    public MalkuthCannonProjectileRenderer(EntityRendererProvider.Context p_174008_) {
        super(p_174008_);
    }

    public void render(MalkuthCannonProjectile projectile, float yaw, float pticks, PoseStack matrices, MultiBufferSource src, int light) {
        matrices.m_85836_();
        matrices.m_252880_(0.0f, projectile.m_20206_() / 2.0f, 0.0f);
        FDRenderUtil.applyMovementMatrixRotations((PoseStack)matrices, (Vec3)projectile.m_20184_());
        int rotationDir = projectile.m_19879_() % 2 == 0 ? 1 : -1;
        matrices.m_252781_(Axis.f_252436_.m_252977_((float)(rotationDir * 10) * ((float)projectile.f_19797_ + pticks) + (float)projectile.m_19879_() * 434.5435f));
        BlockRenderDispatcher blockRenderDispatcher = Minecraft.m_91087_().m_91289_();
        if (projectile.getMalkuthAttackType().isFire()) {
            matrices.m_85836_();
            matrices.m_252781_(Axis.f_252393_.m_252977_(180.0f));
            matrices.m_85837_(-0.5, 0.5, -0.5);
            blockRenderDispatcher.renderSingleBlock(Blocks.f_50083_.m_49966_(), matrices, src, 0xF000F0, OverlayTexture.f_118083_, ModelData.EMPTY, null);
            matrices.m_85849_();
        }
        VertexConsumer vertexConsumer = src.m_6299_(RenderType.m_110458_((ResourceLocation)this.getTextureLocation(projectile)));
        this.model.render(matrices, vertexConsumer, 0xF000F0, OverlayTexture.f_118083_, 1.0f, 1.0f, 1.0f, 1.0f);
        matrices.m_85849_();
    }

    public ResourceLocation getTextureLocation(MalkuthCannonProjectile proj) {
        return proj.getMalkuthAttackType().isIce() ? ICE_TEXTURE : FIRE_TEXTURE;
    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.FDClientHelpers
 *  com.finderfeed.fdlib.util.client.ColoredVertexConsumer
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.math.Axis
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.entity.EntityRenderer
 *  net.minecraft.client.renderer.entity.EntityRendererProvider$Context
 *  net.minecraft.client.renderer.entity.ItemRenderer
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.client.renderer.texture.TextureAtlas
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.item.ItemDisplayContext
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.entities.chesed_sword_buff;

import com.finderfeed.fdbosses.content.entities.chesed_sword_buff.FlyingSwordEntity;
import com.finderfeed.fdlib.FDClientHelpers;
import com.finderfeed.fdlib.util.client.ColoredVertexConsumer;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

public class FlyingSwordRenderer
extends EntityRenderer<FlyingSwordEntity> {
    public FlyingSwordRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    public void render(FlyingSwordEntity entity, float yaw, float pticks, PoseStack matrices, MultiBufferSource src, int light) {
        matrices.m_85836_();
        float halfHeight = entity.m_20206_() / 2.0f;
        matrices.m_252880_(0.0f, halfHeight, 0.0f);
        int targetId = entity.getTargetIdForClient();
        ClientLevel clientLevel = (ClientLevel)FDClientHelpers.getClientLevel();
        Entity entity2 = clientLevel.m_6815_(targetId);
        if (entity2 instanceof LivingEntity) {
            LivingEntity target = (LivingEntity)entity2;
            Vec3 thisPos = entity.m_20182_().m_82520_(0.0, (double)(entity.m_20206_() / 2.0f), 0.0);
            Vec3 thatPos = entity.getTargetPos(target, pticks);
            Vec3 between = thatPos.m_82546_(thisPos);
            FDRenderUtil.applyMovementMatrixRotations((PoseStack)matrices, (Vec3)between);
        }
        float time = (float)entity.f_19797_ + pticks;
        float p = FDMathUtil.clamp((float)(time / (float)FlyingSwordEntity.DELAY_UNTIL_LAUNCH), (float)0.0f, (float)1.0f);
        float offset = -Mth.m_14031_((float)(p * (float)Math.PI)) * 2.0f;
        matrices.m_252880_(0.0f, offset, 0.0f);
        matrices.m_252781_(Axis.f_252436_.m_252977_(FDEasings.easeOut((float)p) * 720.0f));
        matrices.m_252781_(Axis.f_252393_.m_252977_(45.0f));
        float scale = FDMathUtil.clamp((float)(p * 2.0f), (float)0.0f, (float)1.0f);
        matrices.m_85841_(scale, scale, scale);
        ItemRenderer itemRenderer = Minecraft.m_91087_().m_91291_();
        itemRenderer.m_115143_(entity.getItem(), ItemDisplayContext.FIXED, false, matrices, ColoredVertexConsumer.wrapBufferSource((MultiBufferSource)src, (int)5, (int)209, (int)255, (int)200), 0xF000F0, OverlayTexture.f_118083_, itemRenderer.m_174264_(entity.getItem(), entity.m_9236_(), (LivingEntity)Minecraft.m_91087_().f_91074_, 0));
        matrices.m_85849_();
    }

    public ResourceLocation getTextureLocation(FlyingSwordEntity p_114482_) {
        return TextureAtlas.f_118259_;
    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.math.Axis
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.culling.Frustum
 *  net.minecraft.client.renderer.entity.EntityRenderer
 *  net.minecraft.client.renderer.entity.EntityRendererProvider$Context
 *  net.minecraft.client.renderer.texture.TextureAtlas
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix4f
 */
package com.finderfeed.fdbosses.content.entities.chesed_boss.chesed_vertical_ray;

import com.finderfeed.fdbosses.client.particles.arc_lightning.ArcLightningParticle;
import com.finderfeed.fdbosses.client.util.BossRenderTypes;
import com.finderfeed.fdbosses.content.entities.chesed_boss.chesed_vertical_ray.ChesedMovingVerticalRay;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class ChesedVerticalRayAttackRenderer
extends EntityRenderer<ChesedMovingVerticalRay> {
    public ChesedVerticalRayAttackRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    public void render(ChesedMovingVerticalRay entity, float yaw, float partialTicks, PoseStack matrices, MultiBufferSource src, int light) {
        super.m_7392_((Entity)entity, yaw, partialTicks, matrices, src, light);
        matrices.m_85836_();
        Camera camera = Minecraft.m_91087_().f_91063_.m_109153_();
        Vec3 camPos = camera.m_90583_();
        Vec3 thisPos = entity.m_20182_();
        Vec3 b = thisPos.m_82546_(camPos);
        double angle = Math.atan2(b.f_82479_, b.f_82481_);
        matrices.m_252781_(Axis.f_252436_.m_252961_((float)Math.PI + (float)angle));
        float height = entity.getHeight();
        int breaks = (int)Math.max(2.0f, height / 4.0f);
        VertexConsumer vertex = src.m_6299_(BossRenderTypes.LIGHTNING_NO_CULL);
        float width = 0.75f;
        matrices.m_85836_();
        matrices.m_252880_(0.0f, 0.0f, -0.01f);
        ArcLightningParticle.fullLightningDraw(entity.m_9236_().m_46467_(), entity.m_19879_() * 35535, breaks, matrices.m_85850_().m_252922_(), vertex, List.of(Vec3.f_82478_, Vec3.f_82478_.m_82520_(0.0, (double)height, 0.0)), 0.25f, width * 1.5f, 0.3f, 0.8f, 1.0f, 1.0f);
        matrices.m_85849_();
        vertex = src.m_6299_(RenderType.m_110502_());
        Matrix4f m = matrices.m_85850_().m_252922_();
        vertex.m_252986_(m, 0.0f, 0.0f, 0.0f).m_85950_(0.3f, 1.0f, 1.0f, 1.0f).m_5752_();
        vertex.m_252986_(m, 0.0f, height, 0.0f).m_85950_(0.3f, 1.0f, 1.0f, 1.0f).m_5752_();
        vertex.m_252986_(m, -width, height, 0.0f).m_85950_(0.3f, 1.0f, 1.0f, 0.0f).m_5752_();
        vertex.m_252986_(m, -width, 0.0f, 0.0f).m_85950_(0.3f, 1.0f, 1.0f, 0.0f).m_5752_();
        vertex.m_252986_(m, width, 0.0f, 0.0f).m_85950_(0.3f, 1.0f, 1.0f, 0.0f).m_5752_();
        vertex.m_252986_(m, width, height, 0.0f).m_85950_(0.3f, 1.0f, 1.0f, 0.0f).m_5752_();
        vertex.m_252986_(m, 0.0f, height, 0.0f).m_85950_(0.3f, 1.0f, 1.0f, 1.0f).m_5752_();
        vertex.m_252986_(m, 0.0f, 0.0f, 0.0f).m_85950_(0.3f, 1.0f, 1.0f, 1.0f).m_5752_();
        vertex.m_252986_(m, 0.0f, 0.0f, 0.01f).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_5752_();
        vertex.m_252986_(m, 0.0f, height, 0.01f).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_5752_();
        vertex.m_252986_(m, -width / 4.0f, height, 0.01f).m_85950_(1.0f, 1.0f, 1.0f, 0.0f).m_5752_();
        vertex.m_252986_(m, -width / 4.0f, 0.0f, 0.01f).m_85950_(1.0f, 1.0f, 1.0f, 0.0f).m_5752_();
        vertex.m_252986_(m, width / 4.0f, 0.0f, 0.01f).m_85950_(1.0f, 1.0f, 1.0f, 0.0f).m_5752_();
        vertex.m_252986_(m, width / 4.0f, height, 0.01f).m_85950_(1.0f, 1.0f, 1.0f, 0.0f).m_5752_();
        vertex.m_252986_(m, 0.0f, height, 0.01f).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_5752_();
        vertex.m_252986_(m, 0.0f, 0.0f, 0.01f).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_5752_();
        matrices.m_85849_();
    }

    public ResourceLocation getTextureLocation(ChesedMovingVerticalRay rayAttack) {
        return TextureAtlas.f_118259_;
    }

    public boolean shouldRender(ChesedMovingVerticalRay p_114491_, Frustum p_114492_, double p_114493_, double p_114494_, double p_114495_) {
        return true;
    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationSystem
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.renderer.FDFreeEntityRenderer
 *  com.finderfeed.fdlib.systems.bedrock.models.FDModel
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  com.finderfeed.fdlib.util.rendering.renderers.QuadRenderer
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.math.Axis
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Math
 *  org.joml.Matrix4f
 *  org.joml.Random
 */
package com.finderfeed.fdbosses.content.entities.chesed_boss;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdbosses.content.entities.chesed_boss.ChesedEntity;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationSystem;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.renderer.FDFreeEntityRenderer;
import com.finderfeed.fdlib.systems.bedrock.models.FDModel;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import com.finderfeed.fdlib.util.rendering.renderers.QuadRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Random;

public class ChesedRenderer
implements FDFreeEntityRenderer<ChesedEntity> {
    public static final ResourceLocation CHESED_RAY_PREPARE = FDBosses.location("textures/util/chesed_ray_prepare.png");
    public static final ResourceLocation FINAL_ATTACK_CRACKS = FDBosses.location("textures/util/chesed_wall_crack.png");

    public void render(ChesedEntity chesedEntity, float yaw, float pticks, PoseStack poseStack, MultiBufferSource multiBufferSource, int light) {
        this.renderRayEffect(chesedEntity, yaw, pticks, poseStack, multiBufferSource, light);
        this.renderRockfallRayEffect(chesedEntity, yaw, pticks, poseStack, multiBufferSource, light);
        this.renderFinalAttackCracks(chesedEntity, yaw, pticks, poseStack, multiBufferSource, light);
        this.renderEarthquakeCastFffects(chesedEntity, yaw, pticks, poseStack, multiBufferSource, light);
    }

    private void renderEarthquakeCastFffects(ChesedEntity chesedEntity, float yaw, float pticks, PoseStack poseStack, MultiBufferSource multiBufferSource, int light) {
        AnimationSystem system = chesedEntity.getAnimationSystem();
        AnimationTicker ticker = system.getTicker("EARTHQUAKE");
        if (ticker == null) {
            return;
        }
        float height = chesedEntity.getModelPartPosition((Entity)chesedEntity, (String)"core", (FDModel)ChesedEntity.clientModel).y;
        float elapsedTime = ticker.getTime(pticks);
        int timeOffset = 5;
        int animLength = 15;
        float p = Mth.m_14036_((float)((elapsedTime - (float)timeOffset) / (float)animLength), (float)0.0f, (float)1.0f);
        float size = 5.0f;
        if (elapsedTime > 21.0f) {
            float p2 = Mth.m_14036_((float)((elapsedTime - 21.0f) / 4.0f), (float)0.0f, (float)1.0f);
            size = (1.0f - p2) * 5.0f;
        }
        if (p == 0.0f) {
            return;
        }
        float easedOut = FDEasings.easeOut((float)p);
        float rotation = easedOut * 180.0f;
        VertexConsumer vertexConsumer = multiBufferSource.m_6299_(RenderType.m_234338_((ResourceLocation)CHESED_RAY_PREPARE));
        QuadRenderer.start((VertexConsumer)vertexConsumer).color(1.0f, 1.0f, 1.0f, easedOut).translate(0.0f, 0.05f, 0.0f).pose(poseStack).rotationDegrees(rotation).size(size * easedOut).render();
        QuadRenderer.start((VertexConsumer)vertexConsumer).color(1.0f, 1.0f, 1.0f, easedOut).translate(0.0f, 0.05f, 0.0f).pose(poseStack).rotationDegrees(-rotation).size(size * 2.0f * easedOut).render();
        poseStack.m_85836_();
        VertexConsumer vertex = multiBufferSource.m_6299_(RenderType.m_110502_());
        Matrix4f m = poseStack.m_85850_().m_252922_();
        Camera camera = Minecraft.m_91087_().f_91063_.m_109153_();
        Vec3 camPos = camera.m_90583_();
        Vec3 thisPos = chesedEntity.m_20182_();
        Vec3 b = thisPos.m_82546_(camPos);
        double angle = Math.atan2((double)b.f_82479_, (double)b.f_82481_);
        poseStack.m_252781_(Axis.f_252436_.m_252961_((float)java.lang.Math.PI + (float)angle));
        float width = 0.25f;
        vertex.m_252986_(m, 0.0f, 0.0f, 0.0f).m_85950_(0.3f, 1.0f, 1.0f, p).m_5752_();
        vertex.m_252986_(m, 0.0f, height, 0.0f).m_85950_(0.3f, 1.0f, 1.0f, p).m_5752_();
        vertex.m_252986_(m, -width, height, 0.0f).m_85950_(0.3f, 1.0f, 1.0f, 0.0f).m_5752_();
        vertex.m_252986_(m, -width, 0.0f, 0.0f).m_85950_(0.3f, 1.0f, 1.0f, 0.0f).m_5752_();
        vertex.m_252986_(m, width, 0.0f, 0.0f).m_85950_(0.3f, 1.0f, 1.0f, 0.0f).m_5752_();
        vertex.m_252986_(m, width, height, 0.0f).m_85950_(0.3f, 1.0f, 1.0f, 0.0f).m_5752_();
        vertex.m_252986_(m, 0.0f, height, 0.0f).m_85950_(0.3f, 1.0f, 1.0f, p).m_5752_();
        vertex.m_252986_(m, 0.0f, 0.0f, 0.0f).m_85950_(0.3f, 1.0f, 1.0f, p).m_5752_();
        vertex.m_252986_(m, 0.0f, 0.0f, 0.01f).m_85950_(1.0f, 1.0f, 1.0f, p).m_5752_();
        vertex.m_252986_(m, 0.0f, height, 0.01f).m_85950_(1.0f, 1.0f, 1.0f, p).m_5752_();
        vertex.m_252986_(m, -width / 4.0f, height, 0.01f).m_85950_(1.0f, 1.0f, 1.0f, 0.0f).m_5752_();
        vertex.m_252986_(m, -width / 4.0f, 0.0f, 0.01f).m_85950_(1.0f, 1.0f, 1.0f, 0.0f).m_5752_();
        vertex.m_252986_(m, width / 4.0f, 0.0f, 0.01f).m_85950_(1.0f, 1.0f, 1.0f, 0.0f).m_5752_();
        vertex.m_252986_(m, width / 4.0f, height, 0.01f).m_85950_(1.0f, 1.0f, 1.0f, 0.0f).m_5752_();
        vertex.m_252986_(m, 0.0f, height, 0.01f).m_85950_(1.0f, 1.0f, 1.0f, p).m_5752_();
        vertex.m_252986_(m, 0.0f, 0.0f, 0.01f).m_85950_(1.0f, 1.0f, 1.0f, p).m_5752_();
        poseStack.m_85849_();
    }

    private void renderFinalAttackCracks(ChesedEntity chesedEntity, float yaw, float pticks, PoseStack poseStack, MultiBufferSource multiBufferSource, int light) {
        float inTime;
        float offset;
        AnimationSystem system = chesedEntity.getAnimationSystem();
        AnimationTicker ticker = system.getTicker("BOOM");
        if (ticker == null) {
            return;
        }
        float elapsedTime = ticker.getElapsedTime();
        float p = Mth.m_14036_((float)((elapsedTime - (offset = 149.0f) + pticks) / (inTime = 12.0f)), (float)0.0f, (float)1.0f);
        if (p == 0.0f || elapsedTime > 190.0f) {
            return;
        }
        p = FDEasings.easeInOut((float)p);
        float crackHeightRad = 1.0f;
        int cracksAmount = 80;
        float radius = 35.4f;
        float angle = (float)java.lang.Math.PI * 2 / (float)cracksAmount;
        float passedAngle = (float)java.lang.Math.PI * 2 * p;
        VertexConsumer vertexConsumer = multiBufferSource.m_6299_(RenderType.m_110488_((ResourceLocation)FINAL_ATTACK_CRACKS));
        int id = 0;
        poseStack.m_85836_();
        poseStack.m_85837_(0.0, 1.4, 0.0);
        Random random = new Random(234234L);
        float i = 0.0f;
        while (i < passedAngle - angle) {
            poseStack.m_85836_();
            Matrix4f matrix4f = poseStack.m_85850_().m_252922_();
            poseStack.m_252781_(Axis.f_252436_.m_252961_((float)id * angle + Math.toRadians((float)(-chesedEntity.m_146908_() - 90.0f))));
            int texId = 0;
            if (id % 2 == 0) {
                texId = random.nextInt(3) + 1;
            }
            vertexConsumer.m_252986_(matrix4f, radius, -crackHeightRad, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_((float)texId * 0.25f + 0.25f, 0.0f).m_86008_(OverlayTexture.f_118083_).m_85969_(0xF000F0).m_252939_(poseStack.m_85850_().m_252943_(), -1.0f, 0.0f, 0.0f).m_5752_();
            vertexConsumer.m_252986_(matrix4f, radius, crackHeightRad, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_((float)texId * 0.25f + 0.25f, 1.0f).m_86008_(OverlayTexture.f_118083_).m_85969_(0xF000F0).m_252939_(poseStack.m_85850_().m_252943_(), -1.0f, 0.0f, 0.0f).m_5752_();
            poseStack.m_252781_(Axis.f_252436_.m_252961_(angle));
            matrix4f = poseStack.m_85850_().m_252922_();
            vertexConsumer.m_252986_(matrix4f, radius, crackHeightRad, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_((float)texId * 0.25f, 1.0f).m_86008_(OverlayTexture.f_118083_).m_85969_(0xF000F0).m_252939_(poseStack.m_85850_().m_252943_(), -1.0f, 0.0f, 0.0f).m_5752_();
            vertexConsumer.m_252986_(matrix4f, radius, -crackHeightRad, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 1.0f).m_7421_((float)texId * 0.25f, 0.0f).m_86008_(OverlayTexture.f_118083_).m_85969_(0xF000F0).m_252939_(poseStack.m_85850_().m_252943_(), -1.0f, 0.0f, 0.0f).m_5752_();
            poseStack.m_85849_();
            i += angle;
            ++id;
        }
        poseStack.m_85849_();
    }

    private void renderRockfallRayEffect(ChesedEntity chesedEntity, float yaw, float pticks, PoseStack poseStack, MultiBufferSource multiBufferSource, int light) {
        AnimationSystem system = chesedEntity.getAnimationSystem();
        AnimationTicker ticker = system.getTicker("ROCKFALL");
        if (ticker != null) {
            float elapsedTime = ticker.getTime(pticks);
            float offset = 20.0f;
            float prepareTime = 27.0f;
            float offsetP = Mth.m_14036_((float)((elapsedTime - offset) / prepareTime), (float)0.0f, (float)1.0f);
            float easedP = FDEasings.easeOut((float)offsetP);
            float rotation = 360.0f * FDEasings.easeOutBack((float)offsetP);
            float alpha = easedP;
            float directionOffset = -1.0f * easedP;
            float sizeMod = 0.0f;
            if (elapsedTime - offset - prepareTime > 0.0f) {
                float awayTime = 7.0f * ticker.getSpeedModifier();
                float offsetP2 = FDMathUtil.clamp((float)((elapsedTime - offset - prepareTime) / awayTime), (float)0.0f, (float)1.0f);
                float easedP2 = FDEasings.easeOut((float)offsetP2);
                alpha = 1.0f - offsetP2;
                directionOffset = 5.0f * easedP2;
                sizeMod = offsetP2 * 2.0f;
            }
            VertexConsumer consumer = multiBufferSource.m_6299_(RenderType.m_234338_((ResourceLocation)CHESED_RAY_PREPARE));
            Vec3 dir = new Vec3(0.0, 1.0, 0.0);
            QuadRenderer.start((VertexConsumer)consumer).color(1.0f, 1.0f, 1.0f, alpha).direction(dir).pose(poseStack).offsetOnDirection(2.0f - directionOffset).translate(0.0f, 1.5f, 0.0f).size(1.5f + sizeMod).rotationDegrees(rotation).render();
            QuadRenderer.start((VertexConsumer)consumer).color(1.0f, 1.0f, 1.0f, alpha).direction(dir).pose(poseStack).offsetOnDirection(3.0f - directionOffset).translate(0.0f, 1.5f, 0.0f).size(0.75f + sizeMod).rotationDegrees(-rotation).render();
            float offset2 = 45.0f;
            float offset2P = FDEasings.easeOut((float)Mth.m_14036_((float)((elapsedTime - offset2) / 10.0f), (float)0.0f, (float)1.0f));
            if (offset2P > 0.0f && offset2P != 1.0f) {
                QuadRenderer.start((VertexConsumer)consumer).color(1.0f, 1.0f, 1.0f, 1.0f - offset2P).offsetOnDirection(30.0f).size(offset2P * 40.0f).pose(poseStack).render();
                QuadRenderer.start((VertexConsumer)consumer).color(1.0f, 1.0f, 1.0f, 1.0f - offset2P).offsetOnDirection(30.0f).size(offset2P * 20.0f).pose(poseStack).render();
            }
        }
    }

    private void renderRayEffect(ChesedEntity chesedEntity, float yaw, float pticks, PoseStack poseStack, MultiBufferSource multiBufferSource, int light) {
        AnimationSystem system = chesedEntity.getAnimationSystem();
        AnimationTicker ticker = system.getTicker("ATTACK");
        if (ticker != null) {
            float elapsedTime = ticker.getTime(pticks);
            float offset = 10.0f;
            float prepareTime = 27.0f;
            float offsetP = Mth.m_14036_((float)((elapsedTime - offset) / prepareTime), (float)0.0f, (float)1.0f);
            float easedP = FDEasings.easeOut((float)offsetP);
            float rotation = 360.0f * FDEasings.easeOutBack((float)offsetP);
            float alpha = easedP;
            float directionOffset = 0.5f * (1.0f - easedP);
            float sizeMod = 0.0f;
            if (elapsedTime - offset - prepareTime > 0.0f) {
                float awayTime = 7.0f * ticker.getSpeedModifier();
                float offsetP2 = FDMathUtil.clamp((float)((elapsedTime - offset - prepareTime) / awayTime), (float)0.0f, (float)1.0f);
                float easedP2 = FDEasings.easeOut((float)offsetP2);
                alpha = 1.0f - offsetP2;
                directionOffset = 5.0f * easedP2;
                sizeMod = offsetP2 * 2.0f;
            }
            VertexConsumer consumer = multiBufferSource.m_6299_(RenderType.m_234338_((ResourceLocation)CHESED_RAY_PREPARE));
            Vec3 dir = new Vec3(1.0, 0.0, 0.0).m_82524_(Math.toRadians((float)(-yaw - 90.0f)));
            QuadRenderer.start((VertexConsumer)consumer).color(1.0f, 1.0f, 1.0f, alpha).direction(dir).pose(poseStack).offsetOnDirection(2.0f - directionOffset).translate(0.0f, 1.5f, 0.0f).size(1.5f + sizeMod).rotationDegrees(rotation).render();
            QuadRenderer.start((VertexConsumer)consumer).color(1.0f, 1.0f, 1.0f, alpha).direction(dir).pose(poseStack).offsetOnDirection(3.0f - directionOffset).translate(0.0f, 1.5f, 0.0f).size(0.75f + sizeMod).rotationDegrees(-rotation).render();
        }
    }
}


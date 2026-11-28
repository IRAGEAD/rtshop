/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.bedrock.models.FDModel
 *  com.finderfeed.fdlib.util.math.ComplexEasingFunction
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  com.finderfeed.fdlib.util.rendering.FDEasings
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
 *  org.joml.Math
 */
package com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_giant_sword;

import com.finderfeed.fdbosses.BossUtil;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthEntity;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_earthquake.MalkuthEarthquakeSegment;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_giant_sword.MalkuthGiantSwordSlash;
import com.finderfeed.fdbosses.init.BossModels;
import com.finderfeed.fdlib.systems.bedrock.models.FDModel;
import com.finderfeed.fdlib.util.math.ComplexEasingFunction;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.Random;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import org.joml.Math;

public class MalkuthGiantSwordSlashRenderer
extends EntityRenderer<MalkuthGiantSwordSlash> {
    private static FDModel model;

    public MalkuthGiantSwordSlashRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        model = new FDModel(BossModels.MALKUTH_SWORD.get());
    }

    public void render(MalkuthGiantSwordSlash slash, float yaw, float pticks, PoseStack matrices, MultiBufferSource src, int light) {
        matrices.m_85836_();
        float dissolveTime = slash.getDissolveTime(pticks);
        float dissolvePercent = 1.0f - dissolveTime / (float)MalkuthGiantSwordSlash.DISSOLVE_TIME;
        float riseTime = slash.getCurrentMoveUpTime(pticks);
        float hitTime = slash.getCurrentHitTime(pticks);
        float riseP = FDEasings.easeOutBack((float)(riseTime / (float)MalkuthGiantSwordSlash.TIME_TO_RISE));
        float risePn = FDEasings.easeOut((float)(riseTime / (float)MalkuthGiantSwordSlash.TIME_TO_RISE));
        float hitP = BossUtil.easeInBack(hitTime / (float)MalkuthGiantSwordSlash.TIME_TO_HIT);
        float scale = 14.0f;
        float riseHeight = 60.0f;
        matrices.m_252781_(Axis.f_252392_.m_252977_(yaw));
        matrices.m_252880_(0.0f, riseP * riseHeight - riseHeight + 2.0f, 0.0f);
        float rotation = hitP * 90.0f + Math.sin((float)(risePn * (float)java.lang.Math.PI * 2.0f)) * 2.0f;
        matrices.m_252781_(Axis.f_252529_.m_252977_(rotation));
        matrices.m_252781_(Axis.f_252436_.m_252977_(180.0f));
        matrices.m_85841_(scale, scale, scale);
        model.render(matrices, src.m_6299_(RenderType.m_110473_((ResourceLocation)MalkuthEntity.MALKUTH_SWORD_SOLID)), light, OverlayTexture.f_118083_, 1.0f, 1.0f, 1.0f, dissolvePercent);
        if (slash.getAttackType().isFire()) {
            model.render(matrices, src.m_6299_(RenderType.m_110497_((ResourceLocation)MalkuthEntity.MALKUTH_FIRE_SWORD)), 0xF000F0, OverlayTexture.f_118083_, 1.0f, 1.0f, 1.0f, dissolvePercent);
        } else {
            model.render(matrices, src.m_6299_(RenderType.m_110497_((ResourceLocation)MalkuthEntity.MALKUTH_ICE_SWORD)), 0xF000F0, OverlayTexture.f_118083_, 1.0f, 1.0f, 1.0f, dissolvePercent);
        }
        matrices.m_85849_();
        if (dissolveTime == 0.0f) {
            return;
        }
        matrices.m_85836_();
        matrices.m_252781_(Axis.f_252392_.m_252977_(yaw));
        Random random = new Random((long)slash.m_19879_() * 424L);
        float xStart = 10.0f;
        float xEnd = 40.0f;
        FDModel model = MalkuthEarthquakeSegment.getBaseModel();
        int rows = 6;
        float maxRowScale = 3.0f;
        for (int g = 0; g < rows; ++g) {
            float rowp = (float)g / ((float)rows - 1.0f);
            float timeOffset = rowp * 5.0f;
            float offsetMax = (float)MalkuthGiantSwordSlash.DISSOLVE_TIME - timeOffset;
            float t = FDMathUtil.clamp((float)(dissolveTime - timeOffset), (float)0.0f, (float)offsetMax);
            float upTime = offsetMax / 8.0f;
            float downTime = offsetMax - upTime;
            ComplexEasingFunction func = ComplexEasingFunction.builder().addArea(upTime, FDEasings::easeIn).addArea(downTime, FDEasings::reversedEaseOut).build();
            float earthquakesP = func.apply(t);
            float sc = maxRowScale - rowp * 2.5f;
            for (float i = xStart; i < xEnd; i += sc) {
                MalkuthEarthquakeSegment.Type type = MalkuthEarthquakeSegment.Type.getRandomBaseSegment(slash.getAttackType(), random, true);
                MalkuthEarthquakeSegment.Type type2 = MalkuthEarthquakeSegment.Type.getRandomBaseSegment(slash.getAttackType(), random, true);
                VertexConsumer vertexConsumer = src.m_6299_(RenderType.m_110497_((ResourceLocation)type.getTexture()));
                float translation = 2.5f - earthquakesP * 2.5f;
                float xtranslation = (float)g + (maxRowScale - sc) + random.nextFloat() * 0.25f;
                float zpos = random.nextFloat() * sc / 2.0f + i;
                matrices.m_85836_();
                matrices.m_252880_(xtranslation, 0.0f, zpos);
                matrices.m_252781_(Axis.f_252436_.m_252977_(90.0f));
                matrices.m_252781_(Axis.f_252529_.m_252977_(35.0f + random.nextFloat() * 10.0f));
                matrices.m_85841_(sc, sc, sc);
                matrices.m_252880_(0.0f, -translation, 0.0f);
                model.render(matrices, vertexConsumer, 0xF000F0, OverlayTexture.f_118083_, 1.0f, 1.0f, 1.0f, 1.0f);
                matrices.m_85849_();
                vertexConsumer = src.m_6299_(RenderType.m_110497_((ResourceLocation)type2.getTexture()));
                matrices.m_85836_();
                zpos = random.nextFloat() * sc / 2.0f + i;
                matrices.m_252880_(-xtranslation, 0.0f, zpos);
                matrices.m_252781_(Axis.f_252436_.m_252977_(-90.0f));
                matrices.m_252781_(Axis.f_252529_.m_252977_(35.0f + random.nextFloat() * 10.0f));
                matrices.m_85841_(sc, sc, sc);
                matrices.m_252880_(0.0f, -translation, 0.0f);
                model.render(matrices, vertexConsumer, 0xF000F0, OverlayTexture.f_118083_, 1.0f, 1.0f, 1.0f, 1.0f);
                matrices.m_85849_();
            }
        }
        matrices.m_85849_();
    }

    public ResourceLocation getTextureLocation(MalkuthGiantSwordSlash slash) {
        return TextureAtlas.f_118259_;
    }

    public boolean shouldRender(MalkuthGiantSwordSlash p_114491_, Frustum p_114492_, double p_114493_, double p_114494_, double p_114495_) {
        return true;
    }
}


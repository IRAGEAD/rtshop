/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.bedrock.models.FDModel
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.math.Axis
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.culling.Frustum
 *  net.minecraft.client.renderer.entity.EntityRenderer
 *  net.minecraft.client.renderer.entity.EntityRendererProvider$Context
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.client.renderer.texture.TextureAtlas
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_floor;

import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_earthquake.MalkuthEarthquakeSegment;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_floor.MalkuthFloorEntity;
import com.finderfeed.fdlib.systems.bedrock.models.FDModel;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import com.mojang.blaze3d.vertex.PoseStack;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class MalkuthFloorRenderer
extends EntityRenderer<MalkuthFloorEntity> {
    public MalkuthFloorRenderer(EntityRendererProvider.Context p_174008_) {
        super(p_174008_);
    }

    public void render(MalkuthFloorEntity floor, float yaw, float pticks, PoseStack matrices, MultiBufferSource src, int light) {
        matrices.m_85836_();
        Random random = new Random((long)floor.m_19879_() * 23L);
        float segmentSize = 1.5f;
        float radius = 28.0f;
        float time = (float)floor.f_19797_ + pticks;
        for (float currentRad = segmentSize; currentRad <= radius; currentRad += segmentSize) {
            float currentArcLength = (float)Math.PI * currentRad;
            for (float i = 0.0f; i <= currentArcLength; i += segmentSize) {
                int riseTime = 5 + random.nextInt(5);
                float downPercent = 1.0f;
                if (floor.isDead()) {
                    downPercent = ((float)floor.deathTicks - pticks) / 10.0f;
                }
                float riseP = FDMathUtil.clamp((float)(time / (float)riseTime), (float)0.0f, (float)1.0f) * downPercent;
                float p = i / currentArcLength;
                float currentAngle = (float)Math.PI * p;
                Vec3 currentOffset = new Vec3((double)currentRad, 0.0, 0.0).m_82524_(currentAngle);
                float rndYRot = random.nextFloat() * 360.0f;
                float rndRot = random.nextFloat() * 10.0f + 10.0f;
                matrices.m_85836_();
                matrices.m_85837_(currentOffset.f_82479_, 0.0, currentOffset.f_82481_);
                matrices.m_252781_(Axis.f_252436_.m_252977_(rndYRot));
                matrices.m_252781_(Axis.f_252529_.m_252977_(rndRot));
                float currentHeight = -2.0f + FDEasings.easeOutBack((float)riseP) * 2.0f;
                matrices.m_252880_(0.0f, currentHeight, 0.0f);
                float rndVal = random.nextFloat();
                MalkuthEarthquakeSegment.Type type = (double)rndVal > 0.925 ? MalkuthEarthquakeSegment.Type.FIRE_1 : (rndVal > 0.875f ? (random.nextBoolean() ? MalkuthEarthquakeSegment.Type.ICE_1 : MalkuthEarthquakeSegment.Type.ICE_2) : MalkuthEarthquakeSegment.Type.FIRE_2);
                FDModel model = MalkuthEarthquakeSegment.getBaseModel();
                model.render(matrices, src.m_6299_(RenderType.m_110497_((ResourceLocation)type.getTexture())), 0xF000F0, OverlayTexture.f_118083_, 1.0f, 1.0f, 1.0f, 1.0f);
                matrices.m_85849_();
            }
        }
        matrices.m_85849_();
    }

    public ResourceLocation getTextureLocation(MalkuthFloorEntity p_114482_) {
        return TextureAtlas.f_118259_;
    }

    public boolean shouldRender(MalkuthFloorEntity entity, Frustum frustum, double p_114493_, double p_114494_, double p_114495_) {
        return frustum.m_113029_(new AABB(-28.0, -4.0, -28.0, 28.0, 4.0, 28.0).m_82383_(entity.m_20182_()));
    }
}


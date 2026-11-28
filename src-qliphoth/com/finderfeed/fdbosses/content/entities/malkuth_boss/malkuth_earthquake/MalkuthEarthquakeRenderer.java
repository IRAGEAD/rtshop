/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.bedrock.models.FDModel
 *  com.finderfeed.fdlib.util.rendering.renderers.QuadRenderer
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
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_earthquake;

import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_earthquake.MalkuthEarthquake;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_earthquake.MalkuthEarthquakeSegment;
import com.finderfeed.fdlib.systems.bedrock.models.FDModel;
import com.finderfeed.fdlib.util.rendering.renderers.QuadRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class MalkuthEarthquakeRenderer
extends EntityRenderer<MalkuthEarthquake> {
    public MalkuthEarthquakeRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    public void render(MalkuthEarthquake entity, float yaw, float pticks, PoseStack matrices, MultiBufferSource src, int light) {
        super.m_7392_((Entity)entity, yaw, pticks, matrices, src, light);
        matrices.m_85836_();
        List<MalkuthEarthquakeSegment> segments = entity.getSegments();
        for (MalkuthEarthquakeSegment segment : segments) {
            matrices.m_85836_();
            Vec3 offset = segment.getOffset();
            float angle = segment.getAngle();
            float hangle = (float)Math.atan2(offset.f_82479_, offset.f_82481_);
            matrices.m_85837_(offset.f_82479_, offset.f_82480_, offset.f_82481_);
            matrices.m_252781_(Axis.f_252436_.m_252961_(hangle));
            matrices.m_252781_(Axis.f_252529_.m_252961_(angle));
            float p = segment.getUpPercent(pticks);
            float up = -2.0f + 2.0f * p;
            matrices.m_252880_(0.0f, up, 0.0f);
            MalkuthEarthquakeSegment.Type type = segment.getType();
            if (type.isModel()) {
                FDModel model = type.getModel().get();
                vertexConsumer = src.m_6299_(RenderType.m_110473_((ResourceLocation)type.getTexture()));
                model.render(matrices, vertexConsumer, 0xF000F0, OverlayTexture.f_118083_, 1.0f, 1.0f, 1.0f, 1.0f);
            } else {
                Vec3 cr = offset.m_82541_().m_82537_(entity.getDirectionAndLength().m_82541_());
                if (cr.f_82480_ < 0.0) {
                    matrices.m_252781_(Axis.f_252436_.m_252977_(10.0f));
                } else {
                    matrices.m_252781_(Axis.f_252436_.m_252977_(-10.0f));
                }
                vertexConsumer = src.m_6299_(RenderType.m_110497_((ResourceLocation)type.getTexture()));
                QuadRenderer.start((VertexConsumer)vertexConsumer).pose(matrices).translate(0.0f, 1.0f, 0.0f).size(1.5f).light(0xF000F0).renderBack().direction(new Vec3(1.0, 0.0, 0.0)).render();
            }
            matrices.m_85849_();
        }
        matrices.m_85849_();
    }

    public ResourceLocation getTextureLocation(MalkuthEarthquake texture) {
        return TextureAtlas.f_118259_;
    }

    public boolean shouldRender(MalkuthEarthquake p_114491_, Frustum p_114492_, double p_114493_, double p_114494_, double p_114495_) {
        return true;
    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.util.FDColor
 *  com.finderfeed.fdlib.util.math.ComplexEasingFunction
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  com.finderfeed.fdlib.util.rendering.renderers.QuadRenderer
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.entity.EntityRenderer
 *  net.minecraft.client.renderer.entity.EntityRendererProvider$Context
 *  net.minecraft.client.renderer.texture.TextureAtlas
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.entities.geburah.casts;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdbosses.content.entities.geburah.casts.GeburahCastingCircle;
import com.finderfeed.fdlib.util.FDColor;
import com.finderfeed.fdlib.util.math.ComplexEasingFunction;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import com.finderfeed.fdlib.util.rendering.renderers.QuadRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public class GeburahCastingCircleRenderer
extends EntityRenderer<GeburahCastingCircle> {
    public static final ResourceLocation GEBURAH_CASTING_CIRCLE_ROTATING = FDBosses.location("textures/entities/geburah/geburah_casting_circle_rotating.png");
    public static final ResourceLocation GEBURAH_CASTING_CIRCLE = FDBosses.location("textures/entities/geburah/geburah_casting_circle_static.png");

    public GeburahCastingCircleRenderer(EntityRendererProvider.Context p_174008_) {
        super(p_174008_);
    }

    public void render(GeburahCastingCircle entity, float yaw, float pticks, PoseStack matrices, MultiBufferSource src, int light) {
        matrices.m_85836_();
        ComplexEasingFunction complexEasingFunction = ComplexEasingFunction.builder().addArea(10.0f, FDEasings::easeOut).addArea((float)(entity.getDuration() - 10 - 10), FDEasings::one).addArea(10.0f, FDEasings::reversedEaseOut).build();
        float rotation = 5.0f * ((float)entity.f_19797_ + pticks);
        float scale = complexEasingFunction.apply((float)entity.f_19797_ + pticks);
        Vec3 direction = entity.getCastDirection();
        FDColor color = FDColor.decode((int)((Integer)entity.m_20088_().m_135370_(GeburahCastingCircle.COLOR)));
        VertexConsumer vertexConsumer = src.m_6299_(RenderType.m_110497_((ResourceLocation)GEBURAH_CASTING_CIRCLE));
        float size = scale * 1.0f;
        QuadRenderer.start((VertexConsumer)vertexConsumer).pose(matrices).direction(direction).renderBack().color(color.r, color.g, color.b, color.a).light(0xF000F0).size(size).renderBack().render();
        vertexConsumer = src.m_6299_(RenderType.m_110497_((ResourceLocation)GEBURAH_CASTING_CIRCLE_ROTATING));
        QuadRenderer.start((VertexConsumer)vertexConsumer).pose(matrices).direction(direction).rotationDegrees(rotation).renderBack().color(color.r, color.g, color.b, color.a).light(0xF000F0).size(size).renderBack().render();
        matrices.m_85849_();
    }

    public ResourceLocation getTextureLocation(GeburahCastingCircle p_114482_) {
        return TextureAtlas.f_118259_;
    }
}


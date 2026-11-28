/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.renderer.FDFreeEntityRenderer
 *  com.finderfeed.fdlib.util.FDColor
 *  com.finderfeed.fdlib.util.math.ComplexEasingFunction
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  com.finderfeed.fdlib.util.rendering.renderers.QuadRenderer
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.resources.ResourceLocation
 */
package com.finderfeed.fdbosses.content.entities.geburah.geburah_bell;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdbosses.content.entities.geburah.geburah_bell.GeburahBell;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.renderer.FDFreeEntityRenderer;
import com.finderfeed.fdlib.util.FDColor;
import com.finderfeed.fdlib.util.math.ComplexEasingFunction;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import com.finderfeed.fdlib.util.rendering.renderers.QuadRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class GeburahBellRenderer
implements FDFreeEntityRenderer<GeburahBell> {
    public static final ResourceLocation EFFECT_LOCATION = FDBosses.location("textures/entities/geburah/geburah_bell_ring_effect.png");
    public static final ComplexEasingFunction EFFECT1_ALPHA = ComplexEasingFunction.builder().addArea(10.0f, FDEasings::easeOut).addArea(10.0f, FDEasings::reversedEaseOut).build();
    public static final ComplexEasingFunction EFFECT1_POS = ComplexEasingFunction.builder().addArea(20.0f, FDEasings::easeOut).build();
    public static final ComplexEasingFunction EFFECT2_ALPHA = ComplexEasingFunction.builder().addArea(7.0f, p -> Float.valueOf(0.0f)).addArea(5.0f, FDEasings::easeOut).addArea(15.0f, FDEasings::reversedEaseOut).build();
    public static final ComplexEasingFunction EFFECT2_POS = ComplexEasingFunction.builder().addArea(7.0f, p -> Float.valueOf(0.0f)).addArea(20.0f, FDEasings::easeOut).build();

    public void render(GeburahBell geburahBell, float v, float pticks, PoseStack matrices, MultiBufferSource multiBufferSource, int i) {
        if (geburahBell.m_21224_()) {
            float time = (float)geburahBell.f_20919_ + pticks;
            VertexConsumer vertexConsumer = multiBufferSource.m_6299_(RenderType.m_110497_((ResourceLocation)EFFECT_LOCATION));
            float effect1Alpha = EFFECT1_ALPHA.apply(time);
            float effect1Pos = EFFECT1_POS.apply(time);
            float effect2Alpha = EFFECT2_ALPHA.apply(time);
            float effect2Pos = EFFECT2_POS.apply(time);
            FDColor color = geburahBell.isRed() ? new FDColor(1.0f, 0.3f, 0.1f, 1.0f) : new FDColor(0.3f, 0.8f, 1.0f, 1.0f);
            float effectDisplace = 1.25f;
            matrices.m_85836_();
            matrices.m_252880_(0.0f, geburahBell.m_20206_() / 2.0f, 0.0f);
            matrices.m_252781_(Minecraft.m_91087_().f_91063_.m_109153_().m_253121_());
            QuadRenderer.start((VertexConsumer)vertexConsumer).verticalRendering().renderBack().translate(-effectDisplace * effect1Pos, 0.0f, 0.0f).pose(matrices).size(effect1Pos * 0.75f).color(color.r, color.g, color.b, effect1Alpha * 0.25f).render();
            QuadRenderer.start((VertexConsumer)vertexConsumer).verticalRendering().renderBack().rotationDegrees(180.0f).translate(effectDisplace * effect1Pos, 0.0f, 0.0f).pose(matrices).size(effect1Pos * 0.75f).color(color.r, color.g, color.b, effect1Alpha * 0.25f).render();
            QuadRenderer.start((VertexConsumer)vertexConsumer).verticalRendering().renderBack().translate(-effectDisplace * effect2Pos, 0.0f, 0.0f).pose(matrices).size(effect2Pos * 0.75f).color(color.r, color.g, color.b, effect2Alpha * 0.25f).render();
            QuadRenderer.start((VertexConsumer)vertexConsumer).verticalRendering().renderBack().rotationDegrees(180.0f).translate(effectDisplace * effect2Pos, 0.0f, 0.0f).pose(matrices).size(effect2Pos * 0.75f).color(color.r, color.g, color.b, effect2Alpha * 0.25f).render();
            matrices.m_85849_();
        }
    }
}


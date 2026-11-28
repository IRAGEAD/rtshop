/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.renderer.FDFreeEntityRenderer
 *  com.finderfeed.fdlib.util.rendering.renderers.QuadRenderer
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 */
package com.finderfeed.fdbosses.content.entities.geburah.justice_hammer;

import com.finderfeed.fdbosses.content.entities.geburah.justice_hammer.JusticeHammerAttack;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.renderer.FDFreeEntityRenderer;
import com.finderfeed.fdlib.util.rendering.renderers.QuadRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

public class JusticeHammerAttackRenderer
implements FDFreeEntityRenderer<JusticeHammerAttack> {
    public void render(JusticeHammerAttack justiceHammerAttack, float yaw, float pticks, PoseStack poseStack, MultiBufferSource multiBufferSource, int light) {
        float length = JusticeHammerAttack.ATTACK_LENGTH;
        float width = JusticeHammerAttack.ATTACK_WIDTH;
        float p = JusticeHammerAttack.EASING2.apply((float)justiceHammerAttack.f_19797_ + pticks);
        if (p == 0.0f) {
            return;
        }
        VertexConsumer vertex = multiBufferSource.m_6299_(RenderType.m_110502_());
        float r = 0.3f;
        float g = 0.7f;
        float b = 1.0f;
        float ySize = 0.7f * p;
        poseStack.m_85836_();
        poseStack.m_85837_(0.0, (double)ySize + 0.02, 0.0);
        float alpha = p * 0.75f;
        QuadRenderer.start((VertexConsumer)vertex).pose(poseStack).direction(justiceHammerAttack.m_20154_()).offsetOnDirection(length / 2.0f).sizeX(width / 2.0f).sizeY(ySize).renderBack().color1(r, g, b, 0.0f).color2(r, g, b, 0.0f).color3(r, g, b, alpha).color4(r, g, b, alpha).render();
        QuadRenderer.start((VertexConsumer)vertex).pose(poseStack).direction(justiceHammerAttack.m_20154_()).offsetOnDirection(-length / 2.0f).sizeX(width / 2.0f).sizeY(ySize).color1(r, g, b, 0.0f).color2(r, g, b, 0.0f).color3(r, g, b, alpha).color4(r, g, b, alpha).renderBack().render();
        QuadRenderer.start((VertexConsumer)vertex).pose(poseStack).direction(justiceHammerAttack.m_20154_().m_82524_(1.5707964f)).offsetOnDirection(-width / 2.0f).sizeX(length / 2.0f).sizeY(ySize).color1(r, g, b, 0.0f).color2(r, g, b, 0.0f).color3(r, g, b, alpha).color4(r, g, b, alpha).renderBack().render();
        QuadRenderer.start((VertexConsumer)vertex).pose(poseStack).direction(justiceHammerAttack.m_20154_().m_82524_(1.5707964f)).offsetOnDirection(width / 2.0f).sizeX(length / 2.0f).sizeY(ySize).color1(r, g, b, 0.0f).color2(r, g, b, 0.0f).color3(r, g, b, alpha).color4(r, g, b, alpha).renderBack().render();
        poseStack.m_85849_();
        poseStack.m_85836_();
        QuadRenderer.start((VertexConsumer)vertex).pose(poseStack).rotationDegrees(-justiceHammerAttack.m_146908_()).offsetOnDirection(0.02f).sizeX(width / 2.0f).sizeY(length / 2.0f).color(r, g, b, alpha).render();
        poseStack.m_85849_();
    }
}


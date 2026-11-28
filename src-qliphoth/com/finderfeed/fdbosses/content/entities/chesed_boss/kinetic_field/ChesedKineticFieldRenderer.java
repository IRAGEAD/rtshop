/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.math.Axis
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix4f
 */
package com.finderfeed.fdbosses.content.entities.chesed_boss.kinetic_field;

import com.finderfeed.fdbosses.client.particles.arc_lightning.ArcLightningParticle;
import com.finderfeed.fdbosses.client.util.BossRenderTypes;
import com.finderfeed.fdbosses.content.entities.chesed_boss.kinetic_field.ChesedKineticFieldEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class ChesedKineticFieldRenderer {
    public static void render(Entity entity, float v, float pticks, PoseStack poseStack, MultiBufferSource multiBufferSource, int i) {
        ChesedKineticFieldEntity e = (ChesedKineticFieldEntity)entity;
        double radius = e.getSquareRadius();
        VertexConsumer vertex = multiBufferSource.m_6299_(BossRenderTypes.LIGHTNING_NO_CULL);
        int breakCount = (int)Math.max(2.0, Math.ceil(radius * 1.5));
        float alpha = entity.f_19797_ > 3 ? 1.0f : 0.0f;
        List<Vec3> positions = List.of(new Vec3(-radius, 1.0, 0.0), new Vec3(radius, 1.0, 0.0));
        for (int g = 0; g < 4; ++g) {
            poseStack.m_85836_();
            poseStack.m_252781_(Axis.f_252436_.m_252977_((float)(90 * g)));
            poseStack.m_85837_(0.0, 0.0, radius);
            Matrix4f t = poseStack.m_85850_().m_252922_();
            int seed = 2345 * e.m_19879_() + g * 543;
            ArcLightningParticle.fullLightningDraw(e.m_9236_().m_46467_(), seed, breakCount, t, vertex, positions, 0.15f, 0.5f, 0.3f, 1.0f, 1.0f, alpha);
            poseStack.m_85837_(0.0, 0.0, 0.05);
            ArcLightningParticle.fullLightningDraw(e.m_9236_().m_46467_(), seed, breakCount, t, vertex, positions, 0.025f, 0.5f, 1.0f, 1.0f, 1.0f, alpha);
            poseStack.m_85837_(0.0, 0.0, -0.1);
            ArcLightningParticle.fullLightningDraw(e.m_9236_().m_46467_(), seed, breakCount, t, vertex, positions, 0.025f, 0.5f, 1.0f, 1.0f, 1.0f, alpha);
            poseStack.m_85849_();
        }
    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.util.rendering.renderers.QuadRenderer
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.entity.EntityRenderer
 *  net.minecraft.client.renderer.entity.EntityRendererProvider$Context
 *  net.minecraft.resources.ResourceLocation
 */
package com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_slash;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthAttackType;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_slash.MalkuthSlashProjectile;
import com.finderfeed.fdlib.util.rendering.renderers.QuadRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class MalkuthSlashRenderer
extends EntityRenderer<MalkuthSlashProjectile> {
    public static final ResourceLocation FIRE_LOCATION = FDBosses.location("textures/entities/malkuth/fire_slash.png");
    public static final ResourceLocation ICE_LOCATION = FDBosses.location("textures/entities/malkuth/ice_slash.png");

    public MalkuthSlashRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    public void render(MalkuthSlashProjectile projectile, float idk, float pticks, PoseStack matrices, MultiBufferSource src, int light) {
        float slashSize = projectile.getSlashSize(pticks);
        QuadRenderer.start((VertexConsumer)src.m_6299_(RenderType.m_110488_((ResourceLocation)this.getTextureLocation(projectile)))).direction(projectile.m_20184_().m_82548_()).translate(0.0f, projectile.m_20206_() / 2.0f, 0.0f).sizeY(slashSize * 0.3461538f).sizeX(slashSize).pose(matrices).renderBack().verticalRendering().rotationDegrees(projectile.getRotation()).render();
    }

    public ResourceLocation getTextureLocation(MalkuthSlashProjectile projectile) {
        return projectile.getAttackType() == MalkuthAttackType.FIRE ? FIRE_LOCATION : ICE_LOCATION;
    }
}


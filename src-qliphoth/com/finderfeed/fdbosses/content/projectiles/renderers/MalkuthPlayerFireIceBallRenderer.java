/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.math.Axis
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.entity.EntityRenderer
 *  net.minecraft.client.renderer.entity.EntityRendererProvider$Context
 *  net.minecraft.client.renderer.texture.TextureAtlas
 *  net.minecraft.resources.ResourceLocation
 *  org.joml.Random
 */
package com.finderfeed.fdbosses.content.projectiles.renderers;

import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthEntity;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_fireball.MalkuthFireballRenderer;
import com.finderfeed.fdbosses.content.projectiles.MalkuthPlayerFireIceBall;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import org.joml.Random;

public class MalkuthPlayerFireIceBallRenderer
extends EntityRenderer<MalkuthPlayerFireIceBall> {
    public MalkuthPlayerFireIceBallRenderer(EntityRendererProvider.Context p_174008_) {
        super(p_174008_);
    }

    public void render(MalkuthPlayerFireIceBall fireball, float yaw, float pticks, PoseStack matrices, MultiBufferSource src, int light) {
        float b;
        float g;
        float r;
        float bi;
        float gi;
        float ri;
        matrices.m_85836_();
        float scale = FDMathUtil.clamp((float)(((float)fireball.f_19797_ + pticks) / 5.0f), (float)0.0f, (float)1.0f);
        matrices.m_85841_(scale, scale, scale);
        if (fireball.getAttackType().isFire()) {
            color = MalkuthEntity.getMalkuthAttackPreparationParticleColor(fireball.getAttackType());
            ri = FDMathUtil.clamp((float)(color.x * 1.5f), (float)0.0f, (float)1.0f);
            gi = FDMathUtil.clamp((float)(color.y * 1.5f), (float)0.0f, (float)1.0f);
            bi = FDMathUtil.clamp((float)(color.z * 1.5f), (float)0.0f, (float)1.0f);
            r = color.x;
            g = color.y;
            b = color.z;
        } else {
            color = MalkuthEntity.getMalkuthAttackPreparationParticleColor(fireball.getAttackType());
            r = 0.7f;
            g = 0.75f;
            b = 0.75f;
            ri = color.x;
            gi = color.y;
            bi = color.z;
        }
        Random random = new Random((long)fireball.m_19879_() * 21L);
        int dir = random.nextInt(2) == 1 ? -1 : 1;
        int dir2 = random.nextInt(2) == 1 ? -1 : 1;
        float time = 20.0f * ((float)fireball.f_19797_ + pticks);
        float time1 = 5.0f * ((float)fireball.f_19797_ + pticks);
        matrices.m_252880_(0.0f, fireball.m_20206_() / 2.0f, 0.0f);
        matrices.m_252781_(Axis.f_252403_.m_252977_(time * (float)dir));
        matrices.m_252781_(Axis.f_252529_.m_252977_(time1 * (float)dir2));
        MalkuthFireballRenderer.renderCube(matrices, src.m_6299_(RenderType.m_110502_()), 0.6f, r, g, b, 0.5f, 2);
        MalkuthFireballRenderer.renderCube(matrices, src.m_6299_(RenderType.m_110502_()), 0.9f, ri, gi, bi, 0.5f, 2);
        matrices.m_85849_();
    }

    public ResourceLocation getTextureLocation(MalkuthPlayerFireIceBall p_114482_) {
        return TextureAtlas.f_118259_;
    }
}


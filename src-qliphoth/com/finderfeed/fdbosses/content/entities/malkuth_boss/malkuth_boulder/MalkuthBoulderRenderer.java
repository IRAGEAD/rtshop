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
 *  net.minecraft.client.renderer.entity.EntityRenderer
 *  net.minecraft.client.renderer.entity.EntityRendererProvider$Context
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.client.renderer.texture.TextureAtlas
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.Entity
 *  org.joml.Random
 */
package com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_boulder;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_boulder.MalkuthBoulderEntity;
import com.finderfeed.fdbosses.init.BossModels;
import com.finderfeed.fdlib.systems.bedrock.models.FDModel;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.joml.Random;

public class MalkuthBoulderRenderer
extends EntityRenderer<MalkuthBoulderEntity> {
    private static final ResourceLocation ICE = FDBosses.location("textures/entities/malkuth/ice_rock.png");
    private static final ResourceLocation FIRE = FDBosses.location("textures/entities/malkuth/fire_rock.png");
    private static FDModel ICE_MODEL;
    private static FDModel FIRE_MODEL;

    public MalkuthBoulderRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        if (ICE_MODEL == null) {
            ICE_MODEL = new FDModel(BossModels.MALKUTH_BOULDER_ICE.get());
        }
        if (FIRE_MODEL == null) {
            FIRE_MODEL = new FDModel(BossModels.MALKUTH_BOULDER_FIRE.get());
        }
    }

    public void render(MalkuthBoulderEntity entity, float yaw, float pticks, PoseStack matrices, MultiBufferSource src, int light) {
        super.m_7392_((Entity)entity, yaw, pticks, matrices, src, light);
        matrices.m_85836_();
        float prepareP = FDMathUtil.clamp((float)(((float)entity.f_19797_ + pticks) / (float)entity.getPrepareTime()), (float)0.0f, (float)1.0f);
        FDModel model = entity.getMalkuthAttackType().isFire() ? FIRE_MODEL : ICE_MODEL;
        ResourceLocation texture = entity.getMalkuthAttackType().isFire() ? FIRE : ICE;
        float h = entity.getPrepareHeight();
        float yt = -h + h * FDEasings.easeOutBack((float)prepareP);
        matrices.m_252880_(0.0f, yt + entity.m_20206_() / 2.0f, 0.0f);
        float time = (float)entity.m_9236_().m_46467_() + pticks;
        int dir = new Random((long)entity.m_19879_() * 42L).nextInt(2) == 0 ? 1 : -1;
        matrices.m_252781_(Axis.f_252403_.m_252977_(time * 10.0f * (float)dir));
        matrices.m_252781_(Axis.f_252529_.m_252977_(time * 10.0f * (float)dir));
        model.render(matrices, src.m_6299_(RenderType.m_110452_((ResourceLocation)texture)), 0xF000F0, OverlayTexture.f_118083_, 1.0f, 1.0f, 1.0f, 1.0f);
        matrices.m_85849_();
    }

    public ResourceLocation getTextureLocation(MalkuthBoulderEntity p_114482_) {
        return TextureAtlas.f_118259_;
    }
}


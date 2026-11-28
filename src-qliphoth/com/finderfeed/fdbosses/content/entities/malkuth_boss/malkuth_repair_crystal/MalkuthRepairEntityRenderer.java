/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.math.Axis
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.entity.EntityRenderer
 *  net.minecraft.client.renderer.entity.EntityRendererProvider$Context
 *  net.minecraft.client.renderer.entity.ItemRenderer
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.client.renderer.texture.TextureAtlas
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemDisplayContext
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_repair_crystal;

import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthAttackType;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_repair_crystal.MalkuthRepairEntity;
import com.finderfeed.fdbosses.init.BossItems;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

public class MalkuthRepairEntityRenderer
extends EntityRenderer<MalkuthRepairEntity> {
    public MalkuthRepairEntityRenderer(EntityRendererProvider.Context p_174008_) {
        super(p_174008_);
    }

    public void render(MalkuthRepairEntity entity, float yaw, float pticks, PoseStack matrices, MultiBufferSource src, int light) {
        MalkuthAttackType type = (MalkuthAttackType)((Object)entity.m_20088_().m_135370_(MalkuthRepairEntity.ATTACK_TYPE));
        Item item = type.isFire() ? BossItems.MALKUTH_CANNON_REPAIR_MATERIAL_FIRE.get() : BossItems.MALKUTH_CANNON_REPAIR_MATERIAL_ICE.get();
        ItemRenderer renderer = Minecraft.m_91087_().m_91291_();
        matrices.m_85836_();
        FDRenderUtil.applyMovementMatrixRotations((PoseStack)matrices, (Vec3)entity.m_20184_());
        float time = (float)entity.f_19797_ + pticks;
        int dir = (Integer)entity.m_20088_().m_135370_(MalkuthRepairEntity.RANDOMIZED_DIRECTION);
        matrices.m_252781_(Axis.f_252436_.m_252977_(time * 10.0f * (float)dir));
        renderer.m_269128_(item.m_7968_(), ItemDisplayContext.FIXED, 0xF000F0, OverlayTexture.f_118083_, matrices, src, entity.m_9236_(), 0);
        matrices.m_85849_();
    }

    public ResourceLocation getTextureLocation(MalkuthRepairEntity p_114482_) {
        return TextureAtlas.f_118259_;
    }
}


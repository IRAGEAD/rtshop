/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.renderer.FDFreeEntityRenderer
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.math.Axis
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.entity.ItemRenderer
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemDisplayContext
 */
package com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_cannon;

import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthAttackType;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_cannon.MalkuthCannonEntity;
import com.finderfeed.fdbosses.init.BossItems;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.renderer.FDFreeEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;

public class MalkuthCannonRenderer
implements FDFreeEntityRenderer<MalkuthCannonEntity> {
    public void render(MalkuthCannonEntity malkuthCannonEntity, float v, float pticks, PoseStack matrices, MultiBufferSource multiBufferSource, int light) {
        if (malkuthCannonEntity.requiresRepair()) {
            matrices.m_85836_();
            MalkuthAttackType cannonType = malkuthCannonEntity.getCannonType();
            Item item = cannonType.isFire() ? BossItems.MALKUTH_CANNON_REPAIR_MATERIAL_FIRE.get() : BossItems.MALKUTH_CANNON_REPAIR_MATERIAL_ICE.get();
            ItemRenderer renderer = Minecraft.m_91087_().m_91291_();
            matrices.m_252781_(Axis.f_252436_.m_252977_(-v));
            float time = (float)malkuthCannonEntity.f_19797_ + pticks;
            float vt = (float)Math.sin(time / 30.0f);
            float vt2 = (float)Math.sin(time / 20.0f);
            matrices.m_252880_(0.0f, 4.0f + vt * 0.125f, 0.0f);
            matrices.m_252781_(Axis.f_252436_.m_252977_(vt * 5.0f));
            matrices.m_252781_(Axis.f_252529_.m_252977_(vt2 * 5.0f));
            renderer.m_269128_(item.m_7968_(), ItemDisplayContext.FIXED, 0xF000F0, OverlayTexture.f_118083_, matrices, multiBufferSource, malkuthCannonEntity.m_9236_(), 0);
            matrices.m_85849_();
        }
    }
}


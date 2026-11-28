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
package com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_repair_crystal;

import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthAttackType;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_repair_crystal.MalkuthRepairCrystal;
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

public class MalkuthRepairCrystalRenderer
implements FDFreeEntityRenderer<MalkuthRepairCrystal> {
    public void render(MalkuthRepairCrystal malkuthRepairCrystal, float v, float pticks, PoseStack poseStack, MultiBufferSource multiBufferSource, int i) {
        poseStack.m_85836_();
        MalkuthAttackType cannonType = malkuthRepairCrystal.getCrystalType();
        Item item = cannonType.isFire() ? BossItems.MALKUTH_CANNON_REPAIR_MATERIAL_FIRE.get() : BossItems.MALKUTH_CANNON_REPAIR_MATERIAL_ICE.get();
        ItemRenderer renderer = Minecraft.m_91087_().m_91291_();
        float time = (float)malkuthRepairCrystal.f_19797_ + pticks;
        poseStack.m_85837_(0.0, 2.5 + Math.sin(time / 40.0f) * (double)0.05f, 0.0);
        poseStack.m_252781_(Axis.f_252436_.m_252977_(time / 5.0f));
        poseStack.m_252781_(Axis.f_252529_.m_252977_((float)Math.sin(time / 20.0f)));
        poseStack.m_85841_(0.75f, 0.75f, 0.75f);
        renderer.m_269128_(item.m_7968_(), ItemDisplayContext.FIXED, 0xF000F0, OverlayTexture.f_118083_, poseStack, multiBufferSource, malkuthRepairCrystal.m_9236_(), 0);
        poseStack.m_85849_();
    }
}


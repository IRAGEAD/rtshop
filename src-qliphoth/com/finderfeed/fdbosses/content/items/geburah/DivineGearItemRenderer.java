/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.FDClientHelpers
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.FDFreeItemRenderer
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.math.Axis
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.Font$DisplayMode
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.world.item.ItemDisplayContext
 *  net.minecraft.world.item.ItemStack
 */
package com.finderfeed.fdbosses.content.items.geburah;

import com.finderfeed.fdbosses.content.items.geburah.DivineGearItem;
import com.finderfeed.fdlib.FDClientHelpers;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.FDFreeItemRenderer;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class DivineGearItemRenderer
implements FDFreeItemRenderer {
    public void render(ItemStack itemStack, ItemDisplayContext itemDisplayContext, PoseStack matrices, MultiBufferSource multiBufferSource, int i, int i1) {
        if (FDClientHelpers.getClientLevel() == null) {
            return;
        }
        int charge = DivineGearItem.getCharge(itemStack);
        int color = charge == 0 ? 0xFF1111 : 43775;
        String s = String.valueOf(charge);
        Font font = Minecraft.m_91087_().f_91062_;
        if (itemDisplayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || itemDisplayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            float pticks = FDRenderUtil.tryGetPartialTickIgnorePause();
            float time = (float)FDClientHelpers.getClientLevel().m_46467_() + pticks;
            float sin = (float)Math.sin(time / 10.0f) * 0.01f;
            matrices.m_85836_();
            float scale = 0.01f;
            float width = (float)font.m_92895_(s) * scale;
            matrices.m_252781_(Axis.f_252436_.m_252977_(180.0f));
            matrices.m_252781_(Axis.f_252403_.m_252977_(180.0f));
            matrices.m_85837_((double)(-width / 2.0f), -0.85 + (double)sin, -0.5);
            if (itemDisplayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
                matrices.m_85837_(0.525, 0.0, 0.0);
            } else {
                matrices.m_85837_(0.5, 0.0, 0.0);
            }
            matrices.m_85841_(scale, scale, -scale);
            font.m_271703_(String.valueOf(charge), 0.0f, 0.0f, color, true, matrices.m_85850_().m_252922_(), multiBufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);
            matrices.m_85849_();
        } else if (itemDisplayContext == ItemDisplayContext.GUI) {
            float scale = 0.05f;
            matrices.m_252781_(Axis.f_252436_.m_252977_(180.0f));
            matrices.m_252781_(Axis.f_252403_.m_252977_(180.0f));
            float width = (float)font.m_92895_(s) * scale;
            matrices.m_85837_((double)(-width / 2.0f) + 0.8, -0.9, -1.0);
            matrices.m_85841_(scale, scale, -scale);
            font.m_271703_(String.valueOf(charge), 0.0f, 0.0f, color, true, matrices.m_85850_().m_252922_(), multiBufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);
        }
    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.Lighting
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.math.Axis
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.network.chat.Component
 *  net.minecraft.world.item.ItemDisplayContext
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 *  org.joml.Vector3f
 */
package com.pla.spiral_abyss.client.screen;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pla.spiral_abyss.block.AbyssGateActivationBlock;
import com.pla.spiral_abyss.init.BlockInit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

final class ActivationRecipePreview {
    private ActivationRecipePreview() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void render(GuiGraphics graphics) {
        Minecraft client = Minecraft.m_91087_();
        graphics.m_280509_(470, 88, 696, 330, -300342212);
        graphics.m_280637_(470, 88, 226, 242, -7478048);
        graphics.m_280653_(client.f_91062_, (Component)Component.m_237115_((String)"ui.spiral_abyss.help.recipe"), 583, 102, -793403);
        graphics.m_280262_();
        Lighting.m_84931_();
        RenderSystem.setShaderLights((Vector3f)new Vector3f(-0.2f, -1.0f, 1.0f).normalize(), (Vector3f)new Vector3f(0.2f, -1.0f, 1.0f).normalize());
        PoseStack pose = graphics.m_280168_();
        pose.m_85836_();
        try {
            pose.m_252880_(559.0f, 252.0f, 60.0f);
            pose.m_85841_(86.0f, -86.0f, 86.0f);
            pose.m_252781_(Axis.f_252529_.m_252977_(40.0f));
            pose.m_252781_(Axis.f_252436_.m_252977_(-35.0f));
            ActivationRecipePreview.block(graphics, Blocks.f_50091_.m_49966_(), -0.5, 0.0, -0.5, 1.0f);
            ActivationRecipePreview.block(graphics, Blocks.f_50729_.m_49966_(), -0.11, 1.01, -0.11, 0.22f);
            ActivationRecipePreview.diamond(graphics, -0.31f, 0.0f);
            ActivationRecipePreview.diamond(graphics, 0.31f, 0.0f);
            ActivationRecipePreview.diamond(graphics, 0.0f, -0.31f);
            ActivationRecipePreview.diamond(graphics, 0.0f, 0.31f);
            graphics.m_280262_();
        }
        finally {
            pose.m_85849_();
            Lighting.m_84931_();
        }
        graphics.m_280056_(client.f_91062_, "\u2192", 624, 216, -7478048, false);
        pose.m_85836_();
        try {
            pose.m_252880_(658.0f, 240.0f, 75.0f);
            pose.m_85841_(35.0f, -35.0f, 35.0f);
            pose.m_252781_(Axis.f_252529_.m_252977_(15.0f));
            pose.m_252781_(Axis.f_252436_.m_252977_(155.0f));
            ActivationRecipePreview.block(graphics, ((AbyssGateActivationBlock)((Object)BlockInit.ABYSS_GATE_ACTIVATE_BLOCK.get())).m_49966_(), -0.5, -0.5, -0.5, 1.0f);
            graphics.m_280262_();
        }
        finally {
            pose.m_85849_();
            Lighting.m_84931_();
        }
        graphics.m_280653_(client.f_91062_, (Component)Component.m_237115_((String)"ui.spiral_abyss.help.recipe.ingredients"), 583, 291, -1976083);
        graphics.m_280653_(client.f_91062_, (Component)Component.m_237115_((String)"ui.spiral_abyss.help.recipe.result"), 583, 307, -7478048);
    }

    private static void block(GuiGraphics graphics, BlockState state, double x, double y, double z, float size) {
        PoseStack pose = graphics.m_280168_();
        pose.m_85836_();
        pose.m_85837_(x, y, z);
        pose.m_85841_(size, size, size);
        Minecraft.m_91087_().m_91289_().m_110912_(state, pose, (MultiBufferSource)graphics.m_280091_(), 0xF000F0, OverlayTexture.f_118083_);
        pose.m_85849_();
    }

    private static void diamond(GuiGraphics graphics, float x, float z) {
        Minecraft client = Minecraft.m_91087_();
        PoseStack pose = graphics.m_280168_();
        pose.m_85836_();
        pose.m_85837_((double)x, 1.035, (double)z);
        pose.m_252781_(Axis.f_252529_.m_252977_(90.0f));
        pose.m_85841_(0.3f, 0.3f, 0.3f);
        client.m_91291_().m_269128_(new ItemStack((ItemLike)Items.f_42415_), ItemDisplayContext.NONE, 0xF000F0, OverlayTexture.f_118083_, pose, (MultiBufferSource)graphics.m_280091_(), (Level)client.f_91073_, 0);
        pose.m_85849_();
    }
}


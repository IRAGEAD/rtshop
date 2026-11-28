/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.math.Axis
 *  dev.architectury.platform.Platform
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.ParticleStatus
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.Font$DisplayMode
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.blockentity.BlockEntityRenderer
 *  net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider$Context
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction$Axis
 *  net.minecraft.core.Vec3i
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.Vec3
 */
package com.pla.spiral_abyss.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.pla.spiral_abyss.block.AbyssGateActivationBlock;
import com.pla.spiral_abyss.block.entity.AbyssPortalBlockEntity;
import com.pla.spiral_abyss.client.portal.PortalEntryAnimation;
import com.pla.spiral_abyss.client.renderer.LootGlow;
import com.pla.spiral_abyss.client.renderer.PortalAura;
import com.pla.spiral_abyss.client.renderer.PortalFrameGlow;
import com.pla.spiral_abyss.client.renderer.PortalTextures;
import dev.architectury.platform.Platform;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;

public final class AbyssPortalRenderer
implements BlockEntityRenderer<AbyssPortalBlockEntity> {
    private final Font font;

    public boolean shouldRenderOffScreen(AbyssPortalBlockEntity portal) {
        return Platform.isFabric();
    }

    public AbyssPortalRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.m_173586_();
    }

    public void render(AbyssPortalBlockEntity entity, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!((Boolean)entity.m_58900_().m_61143_((Property)AbyssGateActivationBlock.ACTIVE)).booleanValue()) {
            return;
        }
        pose.m_85836_();
        BlockPos offset = entity.frame().base().m_121996_((Vec3i)entity.m_58899_());
        pose.m_252880_((float)offset.m_123341_(), (float)offset.m_123342_(), (float)offset.m_123343_());
        boolean xAxis = entity.frame().axis() == Direction.Axis.X;
        long tick = entity.m_58904_() == null ? 0L : entity.m_58904_().m_46467_();
        float entry = PortalEntryAnimation.intensity(entity, partialTick);
        int state = entry > 0.55f ? 2 : (entry > 0.0f || entity.displayedFloor() > 0 ? 1 : 0);
        ResourceLocation texture = PortalTextures.texture(state, (int)(tick / 2L % 64L));
        VertexConsumer vertices = buffers.m_6299_(RenderType.m_234335_((ResourceLocation)texture, (boolean)false));
        AbyssPortalRenderer.texturedStrip(vertices, pose, xAxis, 0.0f, 1.0f, 0.5f, 1.0f);
        AbyssPortalRenderer.texturedStrip(vertices, pose, xAxis, -1.0f, 2.0f, 1.0f, 1.5f);
        AbyssPortalRenderer.texturedStrip(vertices, pose, xAxis, -1.5f, 2.5f, 1.5f, 2.0f);
        AbyssPortalRenderer.texturedStrip(vertices, pose, xAxis, -1.5f, 2.5f, 2.0f, 4.0f);
        AbyssPortalRenderer.texturedStrip(vertices, pose, xAxis, -1.5f, 2.5f, 4.0f, 4.5f);
        AbyssPortalRenderer.texturedStrip(vertices, pose, xAxis, -1.0f, 2.0f, 4.5f, 5.0f);
        Minecraft client = Minecraft.m_91087_();
        double distance = client.f_91063_.m_109153_().m_90583_().m_82554_(Vec3.m_82512_((Vec3i)entity.frame().base()).m_82520_(0.0, 2.3, 0.0));
        int detail = switch ((ParticleStatus)client.f_91066_.m_231929_().m_231551_()) {
            default -> throw new IncompatibleClassChangeError();
            case ParticleStatus.ALL -> 2;
            case ParticleStatus.DECREASED -> 1;
            case ParticleStatus.MINIMAL -> 0;
        };
        float fade = (float)Math.max(0.0, Math.min(1.0, (48.0 - distance) / 16.0));
        if (detail > 0 && fade > 0.0f) {
            PortalAura.render(pose, buffers.m_6299_(LootGlow.TYPE), xAxis, (float)(tick % 120000L) + partialTick, entity.m_58899_().hashCode(), detail, fade);
        }
        this.renderFloor(entity, pose, buffers, xAxis);
        PortalFrameGlow.render(entity, pose, buffers);
        pose.m_85849_();
    }

    private void renderFloor(AbyssPortalBlockEntity entity, PoseStack pose, MultiBufferSource buffers, boolean xAxis) {
        if (entity.displayedFloor() == 0) {
            return;
        }
        MutableComponent label = entity.displayedFloor() == 7 ? Component.m_237113_((String)"\u221e") : Component.m_237115_((String)("enchantment.level." + entity.displayedFloor()));
        int color = entity.hostRunning() ? -11184811 : -1;
        for (int side : new int[]{0, 1}) {
            pose.m_85836_();
            pose.m_85837_(0.5, 3.0, 0.5);
            pose.m_252781_(Axis.f_252436_.m_252977_((float)((xAxis ? 0 : 90) + side * 180)));
            pose.m_85837_(0.0, 0.0, 0.015);
            pose.m_85841_(0.12f, -0.12f, 0.12f);
            float f = (float)(-this.font.m_92852_((FormattedText)label)) / 2.0f;
            Objects.requireNonNull(this.font);
            this.font.m_272077_((Component)label, f, (float)(-9) / 2.0f, color, false, pose.m_85850_().m_252922_(), buffers, Font.DisplayMode.NORMAL, 0, 0xF000F0);
            pose.m_85849_();
        }
    }

    private static void texturedStrip(VertexConsumer vertices, PoseStack pose, boolean xAxis, float left, float right, float low, float high) {
        float normal = xAxis ? 1.0f : -1.0f;
        AbyssPortalRenderer.texturedVertex(vertices, pose, xAxis, left, low, normal);
        AbyssPortalRenderer.texturedVertex(vertices, pose, xAxis, right, low, normal);
        AbyssPortalRenderer.texturedVertex(vertices, pose, xAxis, right, high, normal);
        AbyssPortalRenderer.texturedVertex(vertices, pose, xAxis, left, high, normal);
    }

    private static void texturedVertex(VertexConsumer vertices, PoseStack pose, boolean xAxis, float across, float y, float normal) {
        vertices.m_252986_(pose.m_85850_().m_252922_(), xAxis ? across : 0.5f, y, xAxis ? 0.5f : across).m_6122_(255, 255, 255, 255).m_7421_((across + 1.5f) / 4.0f, (5.0f - y) / 4.5f).m_86008_(OverlayTexture.f_118083_).m_85969_(0xF000F0).m_252939_(pose.m_85850_().m_252943_(), xAxis ? 0.0f : normal, 0.0f, xAxis ? normal : 0.0f).m_5752_();
    }

    public int m_142163_() {
        return 128;
    }
}


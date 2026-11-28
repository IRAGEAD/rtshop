/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.hud.bossbars.FDBossBarInterpolated
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil$Scissor
 *  com.finderfeed.fdlib.util.rendering.FDShaderRenderer
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.renderer.ShaderInstance
 *  net.minecraft.resources.ResourceLocation
 */
package com.finderfeed.fdbosses.content.entities.malkuth_boss;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdbosses.init.BossCoreShaders;
import com.finderfeed.fdlib.systems.hud.bossbars.FDBossBarInterpolated;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.finderfeed.fdlib.util.rendering.FDShaderRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

public class MalkuthBossBar
extends FDBossBarInterpolated {
    public static final ResourceLocation MAIN = FDBosses.location("textures/boss_bars/malkuth_bossbar.png");
    public static final ResourceLocation MAIN_GLOW = FDBosses.location("textures/boss_bars/malkuth_bossbar_glow.png");
    public static final ResourceLocation ABOVE = FDBosses.location("textures/boss_bars/malkuth_bossbar_above.png");
    private int time;

    public MalkuthBossBar(UUID uuid, int entityId) {
        super(uuid, entityId, 10);
    }

    public void renderInterpolatedBossBar(GuiGraphics graphics, float pticks, float interpolatedPercent) {
        PoseStack matrices = graphics.m_280168_();
        float width = 217.0f;
        float height = 67.0f;
        float verticalOffset = 2.0f;
        FDRenderUtil.bindTexture((ResourceLocation)MAIN_GLOW);
        FDRenderUtil.blitWithBlend((PoseStack)matrices, (float)(-width / 2.0f), (float)(-verticalOffset), (float)width, (float)height, (float)0.0f, (float)0.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)0.0f, (float)1.0f);
        float t = ((float)this.time + FDRenderUtil.tryGetPartialTickIgnorePause()) / 100.0f;
        float xHPPos = -width / 2.0f + 14.1f;
        float yHPPos = -verticalOffset + 33.0f;
        FDRenderUtil.Scissor.pushScissors((PoseStack)matrices, (float)xHPPos, (float)yHPPos, (float)(189.0f * interpolatedPercent), (float)5.0f);
        FDShaderRenderer.start((GuiGraphics)graphics, (ShaderInstance)BossCoreShaders.MALKUTH_BOSS_BAR).position(xHPPos, yHPPos, 0.5f).setResolution(189.0f, 5.0f).setUVSpan(12.0f, 0.3f).setShaderUniform("xyOffset", new float[]{0.0f, 0.0f}).setShaderUniform("time", new float[]{t}).setShaderUniform("uvSpan", new float[]{12.0f, 0.3f}).end();
        FDRenderUtil.Scissor.popScissors();
        FDRenderUtil.bindTexture((ResourceLocation)ABOVE);
        FDRenderUtil.blitWithBlend((PoseStack)matrices, (float)(-width / 2.0f), (float)(-verticalOffset), (float)width, (float)height, (float)0.0f, (float)0.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
    }

    public void tick(float topOffset) {
        super.tick(topOffset);
        ++this.time;
    }

    public float height() {
        return 70.0f;
    }

    public void hanldeBarEvent(int eventId, int data) {
    }
}


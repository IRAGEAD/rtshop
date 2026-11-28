/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.simple_screen.FDWidget
 *  com.finderfeed.fdlib.systems.simple_screen.fdwidgets.FDButton
 *  com.finderfeed.fdlib.systems.simple_screen.fdwidgets.util.FDButtonTextures
 *  com.finderfeed.fdlib.systems.simple_screen.fdwidgets.util.WidgetTexture
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  com.mojang.blaze3d.platform.Window
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.Util
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.network.chat.Component
 */
package com.finderfeed.fdbosses.client.boss_screen.widget;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdbosses.client.BossRenderUtil;
import com.finderfeed.fdbosses.client.boss_screen.SubscribersScreen;
import com.finderfeed.fdbosses.client.boss_screen.widget.FDButtonWithTexture;
import com.finderfeed.fdbosses.init.BossSounds;
import com.finderfeed.fdlib.systems.simple_screen.FDWidget;
import com.finderfeed.fdlib.systems.simple_screen.fdwidgets.FDButton;
import com.finderfeed.fdlib.systems.simple_screen.fdwidgets.util.FDButtonTextures;
import com.finderfeed.fdlib.systems.simple_screen.fdwidgets.util.WidgetTexture;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class QliphothAwakeningSocialsWidget
extends FDWidget {
    public static final List<String> PATRONS = List.of("Veem Valentine", "\u7ffc \u5165\u6c5f", "noam", "Krishna Lord");

    public QliphothAwakeningSocialsWidget(Screen screen, float x, float y, float width, float height) {
        super(screen, x, y, width, height);
        this.initSocials();
    }

    public void renderWidget(GuiGraphics guiGraphics, float mx, float my, float pticks) {
        Window window = Minecraft.m_91087_().m_91268_();
        float width = window.m_85445_();
        float height = window.m_85446_();
        PoseStack matrices = guiGraphics.m_280168_();
        matrices.m_85836_();
        matrices.m_252880_(0.0f, 0.0f, 0.0f);
        BossRenderUtil.renderBossScreenTooltip(guiGraphics.m_280168_(), this.getX(), this.getY(), this.getWidth() - 14.0f, this.getHeight() - 14.0f);
        FDRenderUtil.renderCenteredText((GuiGraphics)guiGraphics, (float)(this.getX() + this.getWidth() / 2.0f), (float)(this.getY() + 10.0f), (float)1.5f, (boolean)true, (String)Component.m_237115_((String)"fdbosses.word.socials_word").getString(), (int)14324834);
        matrices.m_85849_();
    }

    public boolean onMouseClick(float v, float v1, int i) {
        return false;
    }

    public boolean onMouseRelease(float v, float v1, int i) {
        return false;
    }

    public boolean onMouseScroll(float v, float v1, float v3) {
        return false;
    }

    public boolean onCharTyped(char c, int i) {
        return false;
    }

    public boolean onKeyPress(int i, int i1, int i2) {
        return false;
    }

    public boolean onKeyRelease(int i, int i1, int i2) {
        return false;
    }

    private void initSocials() {
        float offset = 0.0f;
        float ypos = 29.0f;
        FDButton discord = new FDButtonWithTexture(this.widgetOwner, 9.0f + offset, ypos, 32.0f, 32.0f, FDBosses.location("textures/gui/discord.png")).setTexture(new FDButtonTextures(new WidgetTexture(FDBosses.location("textures/gui/ability_button_unselected.png"), 0.0f, 0.0f), new WidgetTexture(FDBosses.location("textures/gui/ability_button_selected.png"), 1.0f, 1.0f))).setSound(BossSounds.BUTTON_CLICK.get()).setOnHoverAction((fdWidget, guiGraphics, v, v1, v2) -> {
            PoseStack poseStack = guiGraphics.m_280168_();
            poseStack.m_85836_();
            poseStack.m_252880_(0.0f, 0.0f, 100.0f);
            BossRenderUtil.renderBossScreenTooltip(guiGraphics, (Component)Component.m_237115_((String)"fdbosses.word.mod_discord"), v, v1, 200.0f, 14324834, 1.0f);
            poseStack.m_85849_();
        }).setOnClickAction((fdWidget, v, v1, i) -> {
            this.openURL("https://discord.com/invite/tW6zFRHDxC");
            return true;
        });
        FDButton patreon = new FDButtonWithTexture(this.widgetOwner, 44.0f + offset, ypos, 32.0f, 32.0f, FDBosses.location("textures/gui/patreon.png")).setTexture(new FDButtonTextures(new WidgetTexture(FDBosses.location("textures/gui/ability_button_unselected.png"), 0.0f, 0.0f), new WidgetTexture(FDBosses.location("textures/gui/ability_button_selected.png"), 1.0f, 1.0f))).setSound(BossSounds.BUTTON_CLICK.get()).setOnHoverAction((fdWidget, guiGraphics, v, v1, v2) -> {
            PoseStack poseStack = guiGraphics.m_280168_();
            poseStack.m_85836_();
            poseStack.m_252880_(0.0f, 0.0f, 100.0f);
            BossRenderUtil.renderBossScreenTooltip(guiGraphics, (Component)Component.m_237115_((String)"fdbosses.word.patreon"), v, v1, 200.0f, 14324834, 1.0f);
            poseStack.m_85849_();
        }).setOnClickAction((fdWidget, v, v1, i) -> {
            if (!Screen.m_96638_()) {
                this.openURL("https://www.patreon.com/c/FDMods");
            } else {
                Minecraft.m_91087_().m_91152_((Screen)new SubscribersScreen((Component)Component.m_237115_((String)"fdbosses.word.patrons"), PATRONS));
            }
            return true;
        });
        FDButton boosty = new FDButtonWithTexture(this.widgetOwner, 79.0f + offset, ypos, 32.0f, 32.0f, FDBosses.location("textures/gui/boosty.png")).setTexture(new FDButtonTextures(new WidgetTexture(FDBosses.location("textures/gui/ability_button_unselected.png"), 0.0f, 0.0f), new WidgetTexture(FDBosses.location("textures/gui/ability_button_selected.png"), 1.0f, 1.0f))).setSound(BossSounds.BUTTON_CLICK.get()).setOnHoverAction((fdWidget, guiGraphics, v, v1, v2) -> {
            PoseStack poseStack = guiGraphics.m_280168_();
            poseStack.m_85836_();
            poseStack.m_252880_(0.0f, 0.0f, 100.0f);
            BossRenderUtil.renderBossScreenTooltip(guiGraphics, (Component)Component.m_237115_((String)"fdbosses.word.boosty"), v, v1, 200.0f, 14324834, 1.0f);
            poseStack.m_85849_();
        }).setOnClickAction((fdWidget, v, v1, i) -> {
            if (!Screen.m_96638_()) {
                this.openURL("https://boosty.to/fdmods");
            } else {
                Minecraft.m_91087_().m_91152_((Screen)new SubscribersScreen((Component)Component.m_237115_((String)"fdbosses.word.boosters"), List.of()));
            }
            return true;
        });
        this.addChild("discord", (FDWidget)discord);
        this.addChild("patreon", (FDWidget)patreon);
        this.addChild("boosty", (FDWidget)boosty);
    }

    private void openURL(String url) {
        Util.m_137581_().m_137646_(url);
    }
}


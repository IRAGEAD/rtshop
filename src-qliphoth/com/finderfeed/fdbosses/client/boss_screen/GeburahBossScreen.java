/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationSystem
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker
 *  com.finderfeed.fdlib.systems.bedrock.models.FDModel
 *  com.finderfeed.fdlib.systems.simple_screen.fdwidgets.FDButton
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.resources.sounds.SimpleSoundInstance
 *  net.minecraft.client.resources.sounds.SoundInstance
 *  net.minecraft.client.sounds.SoundManager
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.world.item.Item
 *  org.joml.Vector2f
 */
package com.finderfeed.fdbosses.client.boss_screen;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdbosses.client.boss_screen.BaseBossScreen;
import com.finderfeed.fdbosses.client.boss_screen.screen_definitions.BossScreenOptions;
import com.finderfeed.fdbosses.content.entities.geburah.GeburahEntity;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdbosses.init.BossSounds;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationSystem;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker;
import com.finderfeed.fdlib.systems.bedrock.models.FDModel;
import com.finderfeed.fdlib.systems.simple_screen.fdwidgets.FDButton;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import org.joml.Vector2f;

public class GeburahBossScreen
extends BaseBossScreen {
    private AnimationSystem animationSystem = new AnimationSystem(){

        public void onAnimationStart(String s, AnimationTicker animationTicker) {
        }

        public void onAnimationStop(String s) {
        }

        public void onFreeze(boolean b) {
        }

        public void onVariableAdded(String s, float v) {
        }
    };
    private float bossX;
    private float bossY;
    private int geburahHelicopterTick = 0;
    private int oldTick = 0;
    private int geburahRotationSpeed = -1;

    public GeburahBossScreen(int bossSpawnerId, List<Item> possibleDrops, BossScreenOptions options) {
        super(bossSpawnerId, possibleDrops, options);
    }

    @Override
    protected void m_7856_() {
        super.m_7856_();
        Vector2f anchor = this.getAnchor(0.0f, 0.5f);
        this.bossX = this.getBossMenuXStart() / 2.0f;
        this.bossY = anchor.y + 100.0f;
        float height = 150.0f;
        this.geburahRotationSpeed = -1;
        FDButton button = new FDButton((Screen)this, this.bossX - 30.0f, this.bossY - height, 60.0f, height).setOnClickAction((fdWidget, mx, my, b) -> {
            if (b == 0) {
                if (this.geburahRotationSpeed == -1) {
                    this.geburahRotationSpeed = 0;
                }
                return true;
            }
            return false;
        });
        this.m_7787_((GuiEventListener)button);
    }

    @Override
    public void renderBoss(GuiGraphics graphics, float mx, float my, float pticks) {
        FDModel model = GeburahEntity.getClientModel();
        float time = FDMathUtil.lerp((float)this.oldTick, (float)this.geburahHelicopterTick, (float)pticks) / 100.0f;
        float yOffs = 0.0f;
        if (this.geburahRotationSpeed > 100) {
            yOffs = -((float)(this.geburahRotationSpeed - 100) + pticks) * 5.0f;
        }
        FDRenderUtil.renderFDModelInScreen((PoseStack)graphics.m_280168_(), (FDModel)model, (float)this.bossX, (float)(this.bossY + yOffs), (float)25.0f, (float)(45.0f + time), (float)0.0f, (float)8.0f, (RenderType)RenderType.m_110452_((ResourceLocation)FDBosses.location("textures/entities/geburah/geburah.png")));
    }

    @Override
    public void m_86600_() {
        super.m_86600_();
        this.animationSystem.tick();
        this.oldTick = this.geburahHelicopterTick;
        if (this.geburahRotationSpeed == 100) {
            SoundManager soundManager = Minecraft.m_91087_().m_91106_();
            soundManager.m_120367_((SoundInstance)SimpleSoundInstance.m_119755_((SoundEvent)BossSounds.GEBURAH_HELICOPTER.get(), (float)1.0f, (float)1.0f));
        }
        if (this.geburahRotationSpeed != -1) {
            ++this.geburahRotationSpeed;
            this.geburahHelicopterTick += this.geburahRotationSpeed;
        } else {
            ++this.geburahHelicopterTick;
        }
    }

    @Override
    public Component getBossName() {
        return BossEntities.GEBURAH.get().m_20676_();
    }

    @Override
    public int getBaseStringColor() {
        return 14324834;
    }

    public float getScreenWidth() {
        return 0.0f;
    }

    public float getScreenHeight() {
        return 0.0f;
    }
}


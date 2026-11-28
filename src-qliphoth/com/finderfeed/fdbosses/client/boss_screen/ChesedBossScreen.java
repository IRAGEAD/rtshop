/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.FDClientPacketExecutables
 *  com.finderfeed.fdlib.systems.bedrock.animations.Animation$LoopMode
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationSystem
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker
 *  com.finderfeed.fdlib.systems.bedrock.models.FDModel
 *  com.finderfeed.fdlib.systems.simple_screen.fdwidgets.FDButton
 *  com.finderfeed.fdlib.systems.simple_screen.fdwidgets.util.FDButtonTextures
 *  com.finderfeed.fdlib.systems.simple_screen.fdwidgets.util.WidgetTexture
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.ChatFormatting
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.network.chat.Style
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.util.FormattedCharSequence
 *  net.minecraft.util.Mth
 *  net.minecraft.world.item.Item
 *  org.joml.Vector2f
 */
package com.finderfeed.fdbosses.client.boss_screen;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdbosses.client.boss_screen.BaseBossScreen;
import com.finderfeed.fdbosses.client.boss_screen.screen_definitions.BossScreenOptions;
import com.finderfeed.fdbosses.client.boss_screen.util.KilledASefirotException;
import com.finderfeed.fdbosses.client.boss_screen.util.TimedText;
import com.finderfeed.fdbosses.init.BossAnims;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdbosses.init.BossModels;
import com.finderfeed.fdlib.FDClientPacketExecutables;
import com.finderfeed.fdlib.systems.bedrock.animations.Animation;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationSystem;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker;
import com.finderfeed.fdlib.systems.bedrock.models.FDModel;
import com.finderfeed.fdlib.systems.simple_screen.fdwidgets.FDButton;
import com.finderfeed.fdlib.systems.simple_screen.fdwidgets.util.FDButtonTextures;
import com.finderfeed.fdlib.systems.simple_screen.fdwidgets.util.WidgetTexture;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.Objects;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import org.joml.Vector2f;

public class ChesedBossScreen
extends BaseBossScreen {
    private static FDModel chesed;
    private AnimationSystem animationSystem;
    private int hurtTime = 0;
    private static final Component[] hurtTexts;
    private int maxHealth;
    private int remainingHealth = this.maxHealth = 10;
    private boolean renderBoss = false;
    private float bossX;
    private float bossY;
    private TimedText hurtText;

    public ChesedBossScreen(int bossSpawnerId, List<Item> possibleDrops, BossScreenOptions options) {
        super(bossSpawnerId, possibleDrops, options);
    }

    @Override
    protected void m_7856_() {
        super.m_7856_();
        if (chesed == null) {
            chesed = new FDModel(BossModels.CHESED.get());
        }
        this.hurtText = new TimedText();
        this.hurtTime = 0;
        this.animationSystem = new AnimationSystem(){

            public void onAnimationStart(String s, AnimationTicker animationTicker) {
            }

            public void onAnimationStop(String s) {
            }

            public void onFreeze(boolean b) {
            }

            public void onVariableAdded(String s, float v) {
            }
        };
        this.animationSystem.startAnimation("APPEAR", AnimationTicker.builder(BossAnims.CHESED_SCREEN_APPEAR).setLoopMode(Animation.LoopMode.ONCE).setToNullTransitionTime(0).build());
        Vector2f anchor = this.getAnchor(0.0f, 0.5f);
        float offsetX = this.getBossMenuXStart() / 2.0f;
        float offsetY = 100.0f;
        this.bossX = anchor.x + offsetX;
        this.bossY = anchor.y + offsetY;
        FDButton button = new FDButton((Screen)this, this.bossX - 23.0f, this.bossY - 90.0f, 50.0f, 50.0f).setOnClickAction((fdWidget, v, v1, i) -> {
            if (this.hurtTime == 0 && this.remainingHealth > 0) {
                FDClientPacketExecutables.playsoundInEars((SoundEvent)SoundEvents.f_11915_, (float)1.0f, (float)1.0f);
                this.animationSystem.startAnimation("HURT", AnimationTicker.builder(BossAnims.CHESED_IDLE).setLoopMode(Animation.LoopMode.ONCE).startTime(30.0f).setSpeed(20.0f).build());
                this.hurtTime = 10;
                int textId = this.maxHealth - this.remainingHealth;
                int time = 60;
                if (textId == hurtTexts.length - 1) {
                    time = Integer.MAX_VALUE;
                }
                this.hurtText.setText(hurtTexts[textId], time);
                this.remainingHealth = Mth.m_14045_((int)(this.remainingHealth - 1), (int)0, (int)Integer.MAX_VALUE);
                if (this.remainingHealth == 0) {
                    this.m_169411_((GuiEventListener)fdWidget);
                    this.animationSystem.startAnimation("IDLE", AnimationTicker.builder(BossAnims.CHESED_DEATH).build());
                    FDButton crashButton = new FDButton((Screen)this, this.bossX - 55.0f, this.bossY - 40.0f, 110.0f, 24.0f).setTexture(new FDButtonTextures(new WidgetTexture(FDBosses.location("textures/gui/medium_button.png")), new WidgetTexture(FDBosses.location("textures/gui/medium_button_selected.png"), 1.0f, 1.0f))).setText((Component)Component.m_237115_((String)"fdbosses.word.crash_game").m_130948_(Style.f_131099_.m_178520_(this.getBaseStringColor())), 110, 1.0f, true, 0.0f, 1.0f).setOnClickAction((fdWidget1, v2, v11, i1) -> {
                        throw new KilledASefirotException("That's what you get for messing with Chesed! I warned you!");
                    });
                    this.m_142416_((GuiEventListener)crashButton);
                }
            }
            return true;
        });
        this.m_7787_((GuiEventListener)button);
    }

    @Override
    public Component getBossName() {
        return BossEntities.CHESED.get().m_20676_();
    }

    @Override
    public int getBaseStringColor() {
        return 14324834;
    }

    @Override
    public void m_86600_() {
        super.m_86600_();
        this.animationSystem.tick();
        AnimationTicker ticker = this.animationSystem.getTicker("IDLE");
        if (ticker == null) {
            this.animationSystem.startAnimation("IDLE", AnimationTicker.builder(BossAnims.CHESED_IDLE).build());
        }
        this.hurtTime = Mth.m_14045_((int)(this.hurtTime - 1), (int)0, (int)Integer.MAX_VALUE);
        this.hurtText.tick();
    }

    @Override
    protected void renderBoss(GuiGraphics graphics, float mx, float my, float pticks) {
        PoseStack matrices = graphics.m_280168_();
        this.animationSystem.applyAnimations(chesed, FDRenderUtil.tryGetPartialTickIgnorePause());
        matrices.m_85836_();
        matrices.m_252880_(0.0f, 0.0f, -100.0f);
        int overlay = this.hurtTime > 0 ? 3 : OverlayTexture.f_118083_;
        FDRenderUtil.renderFDModelInScreen((PoseStack)matrices, (FDModel)chesed, (float)this.bossX, (float)this.bossY, (float)0.0f, (float)3.3379424f, (float)0.0f, (float)50.0f, (int)0xF000F0, (int)overlay, (RenderType)RenderType.m_110452_((ResourceLocation)FDBosses.location("textures/entities/chesed.png")));
        FDRenderUtil.renderFDModelInScreen((PoseStack)matrices, (FDModel)chesed, (float)this.bossX, (float)this.bossY, (float)0.0f, (float)3.3379424f, (float)0.0f, (float)50.0f, (int)0xF000F0, (int)overlay, (RenderType)RenderType.m_110488_((ResourceLocation)FDBosses.location("textures/entities/chesed_crystals.png")));
        matrices.m_85849_();
        Component text = this.hurtText.text;
        if (text != null) {
            int i = 0;
            for (FormattedCharSequence sequence : this.f_96547_.m_92923_((FormattedText)text, 200)) {
                float f = this.bossX - (float)this.f_96547_.m_92724_(sequence) / 2.0f;
                Objects.requireNonNull(this.f_96547_);
                FDRenderUtil.renderScaledText((GuiGraphics)graphics, (FormattedCharSequence)sequence, (float)f, (float)(this.bossY - 200.0f + (float)(i * 9)), (float)1.0f, (boolean)true, (int)this.getBaseStringColor());
                ++i;
            }
        }
    }

    public float getScreenWidth() {
        return 0.0f;
    }

    public float getScreenHeight() {
        return 0.0f;
    }

    static {
        hurtTexts = new Component[]{Component.m_237115_((String)"fdbosses.word.ouch"), Component.m_237115_((String)"fdbosses.word.that_hurts"), Component.m_237115_((String)"fdbosses.word.stop_it"), Component.m_237115_((String)"fdbosses.word.you_are_getting_on_my_nerves"), Component.m_237115_((String)"fdbosses.word.i_said_stop"), Component.m_237115_((String)"fdbosses.word.consequences"), Component.m_237115_((String)"fdbosses.word.does_that_not_scare_you"), Component.m_237115_((String)"fdbosses.word.ok_listen"), Component.m_237115_((String)"fdbosses.word.no_wait"), Component.m_237115_((String)"fdbosses.word.killed_chesed_in_screen").m_130940_(ChatFormatting.RED)};
    }
}


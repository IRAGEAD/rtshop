/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationSystem
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker
 *  com.finderfeed.fdlib.systems.bedrock.models.FDModel
 *  com.finderfeed.fdlib.systems.screen.screen_particles.FDTexturedSParticle
 *  com.finderfeed.fdlib.systems.simple_screen.FDWidget
 *  com.finderfeed.fdlib.systems.simple_screen.fdwidgets.FDButton
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticle
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil$ParticleRenderTypesS
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.item.Item
 *  org.joml.Vector2f
 *  org.joml.Vector3f
 */
package com.finderfeed.fdbosses.client.boss_screen;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdbosses.client.boss_screen.BaseBossScreen;
import com.finderfeed.fdbosses.client.boss_screen.screen_definitions.BossScreenOptions;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthAttackType;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthEntity;
import com.finderfeed.fdbosses.init.BossAnims;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdbosses.init.BossModels;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationSystem;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker;
import com.finderfeed.fdlib.systems.bedrock.models.FDModel;
import com.finderfeed.fdlib.systems.screen.screen_particles.FDTexturedSParticle;
import com.finderfeed.fdlib.systems.simple_screen.FDWidget;
import com.finderfeed.fdlib.systems.simple_screen.fdwidgets.FDButton;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticle;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class MalkuthBossScreen
extends BaseBossScreen {
    private static FDModel malkuthModel;
    private AnimationSystem animationSystem;
    private int tickCount = 0;
    private float bossX;
    private float bossY;

    public MalkuthBossScreen(int bossSpawnerId, List<Item> possibleDrops, BossScreenOptions options) {
        super(bossSpawnerId, possibleDrops, options);
        if (malkuthModel == null) {
            malkuthModel = new FDModel(BossModels.MALKUTH_SCREEN.get());
        }
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
        this.animationSystem.startAnimation("APPEAR", AnimationTicker.builder(BossAnims.MALKUTH_SCREEN_APPEAR).setSpeed(0.75f).build());
    }

    @Override
    protected void m_7856_() {
        super.m_7856_();
        Vector2f anchor = this.getAnchor(0.0f, 0.5f);
        this.bossX = this.getBossMenuXStart() / 2.0f - 20.0f;
        this.bossY = anchor.y + 90.0f;
        float height = 150.0f;
        FDButton button = new FDButton((Screen)this, this.bossX - 30.0f, this.bossY - height, 60.0f, height).setOnClickAction((fdWidget, mx, my, b) -> {
            if (b == 0) {
                this.resetAbilities(this.options.getSkills(), true);
                this.shuffleParticles();
                return true;
            }
            return false;
        });
        this.m_7787_((GuiEventListener)button);
    }

    private void shuffleParticles() {
        RandomSource random = RandomSource.m_216327_();
        int count = 0;
        for (Map.Entry children : this.bossAbilitesWidget.bossAbilitiesButtonContainer.getChildren().entrySet()) {
            ++count;
            FDWidget button = (FDWidget)children.getValue();
            float wx = button.getWidth();
            float wy = button.getHeight();
            float px = button.getX() + wx / 2.0f;
            float py = button.getY() + wy / 2.0f;
            for (int i = 0; i < 20; ++i) {
                Vector3f color = MalkuthEntity.getAndRandomizeColor(MalkuthAttackType.getRandom(random), random);
                Vector3f v = new Vector3f(random.m_188501_() * 4.0f + 1.0f, 0.0f, 0.0f).rotateZ((float)Math.PI * 2 * random.m_188501_());
                ((FDTexturedSParticle)((FDTexturedSParticle)((FDTexturedSParticle)((FDTexturedSParticle)((FDTexturedSParticle)FDTexturedSParticle.create((Function)FDRenderUtil.ParticleRenderTypesS.TEXTURES_BLUR_ADDITIVE, (ResourceLocation)BallParticle.LOCATION).setPos((double)px, (double)py, true)).setAcceleration((double)(-v.x) * 0.06, (double)(-v.y) * 0.06)).setDefaultScaleOut().setMaxQuadSize(20.0f + random.m_188501_() * 5.0f).setSpeed((double)v.x, (double)v.y)).setColor(color.x, color.y, color.z, 1.0f)).setLifetime(15 + random.m_188503_(5))).sendToScreen();
            }
            if (count < 10) continue;
            break;
        }
    }

    @Override
    public void m_86600_() {
        super.m_86600_();
        ++this.tickCount;
        this.animationSystem.tick();
        if (this.tickCount == 20) {
            this.animationSystem.startAnimation("APPEAR", AnimationTicker.builder(BossAnims.MALKUTH_SCREEN_IDLE).build());
        }
    }

    @Override
    public Component getBossName() {
        return BossEntities.MALKUTH.get().m_20676_();
    }

    @Override
    public int getBaseStringColor() {
        return 14324834;
    }

    @Override
    protected void renderBoss(GuiGraphics graphics, float mx, float my, float pticks) {
        PoseStack matrices = graphics.m_280168_();
        matrices.m_85836_();
        pticks = FDRenderUtil.tryGetPartialTickIgnorePause();
        matrices.m_252880_(0.0f, 0.0f, -75.0f);
        this.animationSystem.applyAnimations(malkuthModel, pticks);
        FDRenderUtil.renderFDModelInScreen((PoseStack)matrices, (FDModel)malkuthModel, (float)this.bossX, (float)this.bossY, (float)0.0f, (float)((float)Math.PI), (float)0.0f, (float)50.0f, (int)0xF000F0, (int)OverlayTexture.f_118083_, (RenderType)RenderType.m_110473_((ResourceLocation)FDBosses.location("textures/entities/malkuth/malkuth_screen.png")));
        FDRenderUtil.renderFDModelInScreen((PoseStack)matrices, (FDModel)malkuthModel, (float)this.bossX, (float)this.bossY, (float)0.0f, (float)((float)Math.PI), (float)0.0f, (float)50.0f, (int)0xF000F0, (int)OverlayTexture.f_118083_, (RenderType)RenderType.m_110497_((ResourceLocation)FDBosses.location("textures/entities/malkuth/malkuth_screen_emissive.png")));
        matrices.m_85849_();
    }

    public float getScreenWidth() {
        return 0.0f;
    }

    public float getScreenHeight() {
        return 0.0f;
    }
}


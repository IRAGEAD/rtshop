/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.FDClientHelpers
 *  com.finderfeed.fdlib.network.FDPacketHandler
 *  com.finderfeed.fdlib.systems.simple_screen.FDScrollableWidget
 *  com.finderfeed.fdlib.systems.simple_screen.FDWidget
 *  com.finderfeed.fdlib.systems.simple_screen.SimpleFDScreen
 *  com.finderfeed.fdlib.systems.simple_screen.fdwidgets.FDButton
 *  com.finderfeed.fdlib.systems.simple_screen.fdwidgets.text_block.TextBlockWidget
 *  com.finderfeed.fdlib.systems.simple_screen.fdwidgets.util.FDButtonTextures
 *  com.finderfeed.fdlib.systems.simple_screen.fdwidgets.util.WidgetTexture
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  com.mojang.blaze3d.platform.Window
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Renderable
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.network.chat.Style
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraftforge.registries.ForgeRegistries
 *  org.joml.Vector2f
 */
package com.finderfeed.fdbosses.client.boss_screen;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdbosses.client.BossRenderUtil;
import com.finderfeed.fdbosses.client.boss_codex.BossCodexScreen;
import com.finderfeed.fdbosses.client.boss_screen.screen_definitions.BossInfo;
import com.finderfeed.fdbosses.client.boss_screen.screen_definitions.BossScreenOptions;
import com.finderfeed.fdbosses.client.boss_screen.widget.BossAbilitesWidget;
import com.finderfeed.fdbosses.client.boss_screen.widget.BossDetailsWidget;
import com.finderfeed.fdbosses.client.boss_screen.widget.DidntReadSkillWarningWidget;
import com.finderfeed.fdbosses.client.boss_screen.widget.FDButtonWithTexture;
import com.finderfeed.fdbosses.client.boss_screen.widget.FDSkillButton;
import com.finderfeed.fdbosses.client.boss_screen.widget.QliphothAwakeningSocialsWidget;
import com.finderfeed.fdbosses.client.boss_screen.widget.SkillInfoWidget;
import com.finderfeed.fdbosses.content.entities.base.BossSpawnerEntity;
import com.finderfeed.fdbosses.content.entities.base.BossSpawnerStartFight;
import com.finderfeed.fdbosses.init.BossSounds;
import com.finderfeed.fdlib.FDClientHelpers;
import com.finderfeed.fdlib.network.FDPacketHandler;
import com.finderfeed.fdlib.systems.simple_screen.FDScrollableWidget;
import com.finderfeed.fdlib.systems.simple_screen.FDWidget;
import com.finderfeed.fdlib.systems.simple_screen.SimpleFDScreen;
import com.finderfeed.fdlib.systems.simple_screen.fdwidgets.FDButton;
import com.finderfeed.fdlib.systems.simple_screen.fdwidgets.text_block.TextBlockWidget;
import com.finderfeed.fdlib.systems.simple_screen.fdwidgets.util.FDButtonTextures;
import com.finderfeed.fdlib.systems.simple_screen.fdwidgets.util.WidgetTexture;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector2f;

public abstract class BaseBossScreen
extends SimpleFDScreen {
    public static final int DEFAULT_TEXT_COLOR = 14324834;
    public static boolean wasSkillRead = false;
    private static int OPEN_TIME = 9;
    private boolean skillOpened = false;
    private int openTicker = 0;
    protected SkillInfoWidget skillInfoWidget;
    protected TextBlockWidget skillInfoText;
    protected BossScreenOptions options;
    protected BossDetailsWidget bossDetailsWidget;
    protected BossAbilitesWidget bossAbilitesWidget;
    protected FDButton skillStatsButton;
    protected FDButton skillInfoButton;
    protected float bossMenuXStart = 0.0f;
    protected QliphothAwakeningSocialsWidget socialsWidget;
    private DidntReadSkillWarningWidget didntReadSkillWarningWidget;
    private int moveThings = 0;
    private int bossSpawnerId;
    private List<Item> possibleDrops;

    public BaseBossScreen(int bossSpawnerId, List<Item> possibleDrops, BossScreenOptions options) {
        this.possibleDrops = possibleDrops;
        this.options = options;
        this.bossSpawnerId = bossSpawnerId;
    }

    protected void m_7856_() {
        super.m_7856_();
        this.moveThings = 0;
        this.skillOpened = false;
        OPEN_TIME = 11;
        this.openTicker = 0;
        Window window = Minecraft.m_91087_().m_91268_();
        this.initBossDetails();
        this.initSkillInfoWidget();
        this.initAbilitiesWidget();
        this.initDidntReadSkillWarningWidget();
        if (this.bossSpawnerId != -1) {
            FDButton startFightButton = new FDButton((Screen)this, 6.0f, 6.0f, 112.0f, 26.0f).setTexture(new FDButtonTextures(new WidgetTexture(FDBosses.location("textures/gui/medium_button.png")), new WidgetTexture(FDBosses.location("textures/gui/medium_button_selected.png"), 0.0f, 0.0f))).setSound(BossSounds.BUTTON_CLICK.get()).setText((Component)Component.m_237115_((String)"fdbosses.word.start_fight").m_130948_(Style.f_131099_.m_178520_(this.getBaseStringColor())), 110, 1.0f, true, 0.0f, 1.0f).setOnClickAction((fdWidget1, v2, v11, i1) -> {
                if (wasSkillRead) {
                    Level level = FDClientHelpers.getClientLevel();
                    Entity patt4620$temp = level.m_6815_(this.bossSpawnerId);
                    if (patt4620$temp instanceof BossSpawnerEntity) {
                        BossSpawnerEntity bossSpawner = (BossSpawnerEntity)patt4620$temp;
                        FDPacketHandler.INSTANCE.sendToServer((Object)new BossSpawnerStartFight(this.bossSpawnerId));
                    }
                } else {
                    this.didntReadSkillWarningWidget.setActive(true);
                }
                return true;
            });
            this.m_142416_((GuiEventListener)startFightButton);
        }
        this.initTLDRButton();
        this.initSocialsButton();
    }

    private void initDidntReadSkillWarningWidget() {
        DidntReadSkillWarningWidget didntReadSkillWarningWidget;
        Vector2f anchor = this.getAnchor(0.5f, 0.5f);
        this.didntReadSkillWarningWidget = didntReadSkillWarningWidget = new DidntReadSkillWarningWidget(this, anchor.x - 150.0f, anchor.y - 40.0f, 300.0f, 80.0f);
        FDButton yes = new FDButton((Screen)this, 20.0f, didntReadSkillWarningWidget.getHeight() - 25.0f, 112.0f, 26.0f).setTexture(new FDButtonTextures(new WidgetTexture(FDBosses.location("textures/gui/medium_button.png")), new WidgetTexture(FDBosses.location("textures/gui/medium_button_selected.png"), 0.0f, 0.0f))).setSound(BossSounds.BUTTON_CLICK.get()).setText((Component)Component.m_237115_((String)"fdbosses.word.yes").m_130948_(Style.f_131099_.m_178520_(this.getBaseStringColor())), 110, 1.0f, true, 0.0f, 1.0f).setOnClickAction((fdWidget1, v2, v11, i1) -> {
            Level level = FDClientHelpers.getClientLevel();
            Entity patt6293$temp = level.m_6815_(this.bossSpawnerId);
            if (patt6293$temp instanceof BossSpawnerEntity) {
                BossSpawnerEntity bossSpawner = (BossSpawnerEntity)patt6293$temp;
                FDPacketHandler.INSTANCE.sendToServer((Object)new BossSpawnerStartFight(this.bossSpawnerId));
            }
            return true;
        });
        FDButton no = new FDButton((Screen)this, didntReadSkillWarningWidget.getWidth() - 120.0f, didntReadSkillWarningWidget.getHeight() - 25.0f, 112.0f, 26.0f).setTexture(new FDButtonTextures(new WidgetTexture(FDBosses.location("textures/gui/medium_button.png")), new WidgetTexture(FDBosses.location("textures/gui/medium_button_selected.png"), 0.0f, 0.0f))).setSound(BossSounds.BUTTON_CLICK.get()).setText((Component)Component.m_237115_((String)"fdbosses.word.no").m_130948_(Style.f_131099_.m_178520_(this.getBaseStringColor())), 110, 1.0f, true, 0.0f, 1.0f).setOnClickAction((fdWidget1, v2, v11, i1) -> {
            this.didntReadSkillWarningWidget.setActive(false);
            return true;
        });
        this.didntReadSkillWarningWidget.setActive(false);
        didntReadSkillWarningWidget.addChild("yes", (FDWidget)yes);
        didntReadSkillWarningWidget.addChild("no", (FDWidget)no);
        this.m_142416_((GuiEventListener)didntReadSkillWarningWidget);
    }

    private void initBossDetails() {
        Vector2f anchor = this.getAnchor(1.0f, 0.0f);
        float bossInfoWidth = 237.0f;
        float bossInfoHeight = 130.0f;
        float bossInfoYs = -5.0f;
        BossDetailsWidget widget = new BossDetailsWidget((Screen)this, anchor.x, bossInfoYs, bossInfoWidth, bossInfoHeight);
        widget.setBossInfo(this.options.getEntityType().m_20676_(), this.getBaseStringColor());
        this.bossMenuXStart = anchor.x - bossInfoWidth;
        TextBlockWidget bossDescription = new TextBlockWidget((Screen)this, 18.0f, 60.0f, 195.0f, 52.0f);
        bossDescription.setText(this.options.getBossDescription(), 1.0f, this.getBaseStringColor(), true);
        widget.addChild("bossDescription", (FDWidget)bossDescription);
        this.m_142416_((GuiEventListener)widget);
        this.bossDetailsWidget = widget;
    }

    private void initSkillInfoWidget() {
        FDButton fdButtonInfo;
        Vector2f anchor = this.getAnchor(1.0f, 1.0f);
        SkillInfoWidget widget = new SkillInfoWidget((Screen)this, -200.0f, 2.0f, 200.0f, anchor.y - 4.0f, (Component)Component.m_237113_((String)"TTT"), this.getBaseStringColor());
        FDButton fdButtonStats = new FDButton((Screen)this, 107.0f, 79.0f, 75.0f, 26.0f).setTexture(new FDButtonTextures(new WidgetTexture(FDBosses.location("textures/gui/small_button.png")), new WidgetTexture(FDBosses.location("textures/gui/small_button_selected.png"), 0.0f, 0.0f))).setSound(BossSounds.BUTTON_CLICK.get()).setText((Component)Component.m_237115_((String)"fdbosses.word.stats").m_130948_(Style.f_131099_.m_178520_(this.getBaseStringColor())), 61, 1.0f, true, 0.0f, 1.0f);
        this.skillInfoButton = fdButtonInfo = new FDButton((Screen)this, 29.0f, 79.0f, 75.0f, 26.0f).setTexture(new FDButtonTextures(new WidgetTexture(FDBosses.location("textures/gui/small_button.png")), new WidgetTexture(FDBosses.location("textures/gui/small_button_selected.png"), 0.0f, 0.0f))).setSound(BossSounds.BUTTON_CLICK.get()).setText((Component)Component.m_237115_((String)"fdbosses.word.info").m_130948_(Style.f_131099_.m_178520_(this.getBaseStringColor())), 61, 1.0f, true, 0.0f, 1.0f);
        this.skillStatsButton = fdButtonStats;
        TextBlockWidget textBlockWidget = new TextBlockWidget((Screen)this, 30.0f, 107.0f, 151.0f, widget.getHeight() - 130.0f);
        widget.addChild("text", (FDWidget)textBlockWidget);
        widget.addChild("statsButton", (FDWidget)fdButtonStats);
        widget.addChild("infoButton", (FDWidget)fdButtonInfo);
        this.skillInfoWidget = widget;
        this.skillInfoText = textBlockWidget;
        this.m_142416_((GuiEventListener)widget);
    }

    private void initAbilitiesWidget() {
        Vector2f anchorEnd = this.getAnchor(1.0f, 1.0f);
        BossAbilitesWidget bossAbilitesWidget = new BossAbilitesWidget((Screen)this, anchorEnd.x - 233.0f, anchorEnd.y, this.getBaseStringColor());
        List<BossInfo> skills = this.options.getSkills();
        this.initAbilitiesWidgetModeChangeButtons(bossAbilitesWidget);
        this.initAbilitiesWidgetInfoButtons(bossAbilitesWidget.bossAbilitiesButtonContainer, skills, false);
        this.m_142416_((GuiEventListener)bossAbilitesWidget);
        this.bossAbilitesWidget = bossAbilitesWidget;
    }

    private void initAbilitiesWidgetModeChangeButtons(BossAbilitesWidget bossAbilitesWidget) {
        FDButton fdButtonSkills = new FDButton((Screen)this, 34.0f, 1.0f, 75.0f, 26.0f).setTexture(new FDButtonTextures(new WidgetTexture(FDBosses.location("textures/gui/small_button.png")), new WidgetTexture(FDBosses.location("textures/gui/small_button_selected.png"), 0.0f, 0.0f))).setSound(BossSounds.BUTTON_CLICK.get()).setText((Component)Component.m_237115_((String)"fdbosses.word.abilities").m_130948_(Style.f_131099_.m_178520_(this.getBaseStringColor())), 73, 1.0f, true, 0.0f, 1.0f).setOnClickAction((fdWidget, v, v1, i) -> {
            this.resetAbilities(this.options.getSkills(), false);
            return true;
        });
        FDButton fdButtonDrops = new FDButton((Screen)this, 124.0f, 1.0f, 75.0f, 26.0f).setTexture(new FDButtonTextures(new WidgetTexture(FDBosses.location("textures/gui/small_button.png")), new WidgetTexture(FDBosses.location("textures/gui/small_button_selected.png"), 0.0f, 0.0f))).setSound(BossSounds.BUTTON_CLICK.get()).setText((Component)Component.m_237115_((String)"fdbosses.word.drops").m_130948_(Style.f_131099_.m_178520_(this.getBaseStringColor())), 73, 1.0f, true, 0.0f, 1.0f).setOnClickAction((fdWidget, v, v1, i) -> {
            this.resetAbilities(this.createPossibleDropsBossInfos(), false);
            return true;
        });
        bossAbilitesWidget.addChild("openSkills", (FDWidget)fdButtonSkills);
        bossAbilitesWidget.addChild("openDrops", (FDWidget)fdButtonDrops);
    }

    private List<BossInfo> createPossibleDropsBossInfos() {
        ArrayList<BossInfo> bossInfos = new ArrayList<BossInfo>();
        EntityType<?> entityType = this.options.getEntityType();
        ResourceLocation typeKey = ForgeRegistries.ENTITY_TYPES.getKey(entityType);
        String keyString = typeKey.toString().replace(":", ".");
        for (Item item : this.possibleDrops) {
            ItemStack itemStack = item.m_7968_();
            ResourceLocation key = ForgeRegistries.ITEMS.getKey((Object)item);
            String itemId = key.toString().replace(":", ".");
            String infoString = "fdbosses.boss_drop." + keyString + "." + itemId + ".info";
            String statsString = "fdbosses.boss_drop." + keyString + "." + itemId + ".stats";
            MutableComponent stats = null;
            Object info = this.hasTranslatableFor(infoString) ? Component.m_237115_((String)infoString) : itemStack.m_41786_();
            if (this.hasTranslatableFor(statsString)) {
                stats = Component.m_237115_((String)statsString);
            }
            BossInfo bossInfo = new BossInfo(itemStack, (Component)stats, (Component)info);
            bossInfos.add(bossInfo);
        }
        return bossInfos;
    }

    private boolean hasTranslatableFor(String id) {
        MutableComponent component = Component.m_237115_((String)id);
        String translated = component.getString();
        return !translated.equals(id);
    }

    protected void resetAbilities(List<BossInfo> bossInfos, boolean malkuthShuffle) {
        this.bossAbilitesWidget.bossAbilitiesButtonContainer.setCurrentScroll(0.0f);
        this.bossAbilitesWidget.bossAbilitiesButtonContainer.removeAllChildren();
        this.initAbilitiesWidgetInfoButtons(this.bossAbilitesWidget.bossAbilitiesButtonContainer, bossInfos, malkuthShuffle);
    }

    private void initSocialsButton() {
        Vector2f anchor = this.getAnchor(0.0f, 1.0f);
        Vector2f anchor2 = this.getAnchor(0.5f, 0.5f);
        float x = anchor2.x;
        float y = anchor2.y;
        this.socialsWidget = new QliphothAwakeningSocialsWidget((Screen)this, x - 60.0f, y - 35.0f, 120.0f, 70.0f);
        FDButton button = new FDButtonWithTexture((Screen)this, anchor.x + 4.0f, anchor.y - 74.0f, 34.0f, 34.0f, FDBosses.location("textures/gui/link.png")).setTexture(new FDButtonTextures(new WidgetTexture(FDBosses.location("textures/gui/ability_button_unselected.png"), 0.0f, 0.0f), new WidgetTexture(FDBosses.location("textures/gui/ability_button_selected.png"), 0.0f, 0.0f))).setSound(BossSounds.BUTTON_CLICK.get()).setOnHoverAction((fdWidget, guiGraphics, v, v1, v2) -> {
            PoseStack poseStack = guiGraphics.m_280168_();
            poseStack.m_85836_();
            poseStack.m_252880_(0.0f, 0.0f, 100.0f);
            BossRenderUtil.renderBossScreenTooltip(guiGraphics, (Component)Component.m_237115_((String)"fdbosses.word.socials"), v, v1, 200.0f, this.getBaseStringColor(), 1.0f);
            poseStack.m_85849_();
        }).setOnClickAction((fdWidget, v, v1, i) -> {
            this.socialsWidget.setActive(true);
            return true;
        });
        this.m_142416_((GuiEventListener)button);
        this.socialsWidget.setActive(false);
        this.m_7787_((GuiEventListener)this.socialsWidget);
    }

    private void initTLDRButton() {
        Vector2f anchor = this.getAnchor(0.0f, 1.0f);
        BossInfo bossInfo = new BossInfo(ResourceLocation.m_135820_((String)"minecraft:textures/item/book.png"), (Component)Component.m_237115_((String)"fdbosses.word.short_description"), null, this.options.getTLDRComponent());
        FDButton skill = new FDSkillButton((Screen)this, anchor.x + 4.0f, anchor.y - 37.0f, 34.0f, 34.0f, bossInfo).setTexture(new FDButtonTextures(new WidgetTexture(FDBosses.location("textures/gui/ability_button_unselected.png"), 0.0f, 0.0f), new WidgetTexture(FDBosses.location("textures/gui/ability_button_selected.png"), 0.0f, 0.0f))).setOnHoverAction((fdWidget, guiGraphics, v, v1, v2) -> {
            if (!this.skillOpened) {
                PoseStack matrices = guiGraphics.m_280168_();
                matrices.m_85836_();
                matrices.m_252880_(0.0f, 0.0f, 100.0f);
                BossRenderUtil.renderBossScreenTooltip(guiGraphics, (Component)Component.m_237115_((String)"fdbosses.word.tldr"), v, v1, 200.0f, this.getBaseStringColor(), 1.0f);
                matrices.m_85849_();
            }
        }).setSound(BossSounds.BUTTON_CLICK.get()).setOnClickAction((fdWidget, mx, my, button) -> {
            if (button != 0) {
                return false;
            }
            wasSkillRead = true;
            this.openSkillInfo(bossInfo, true);
            return true;
        });
        this.m_142416_((GuiEventListener)skill);
    }

    private void initAbilitiesWidgetInfoButtons(FDScrollableWidget bossAbilitesWidget, List<BossInfo> bossInfos, boolean malkuthShuffle) {
        int i;
        float offsx = 5.0f;
        float offsy = 4.0f;
        ArrayList<BossInfo> bossInfos2 = new ArrayList<BossInfo>(bossInfos);
        Random random = new Random();
        if (malkuthShuffle && bossInfos2.size() > 1) {
            for (i = 0; i < bossInfos2.size() * 2; ++i) {
                int id2;
                int id1 = random.nextInt(bossInfos2.size());
                BossInfo info1 = (BossInfo)bossInfos2.get(id1);
                while ((id2 = random.nextInt(bossInfos2.size())) == id1) {
                }
                BossInfo info2 = (BossInfo)bossInfos2.get(id2);
                bossInfos2.set(id1, info2);
                bossInfos2.set(id2, info1);
            }
        }
        for (i = 0; i < bossInfos.size(); ++i) {
            BossInfo bossSkill = bossInfos.get(i);
            BossInfo skillToOpen = (BossInfo)bossInfos2.get(i);
            float x = offsx + (float)(i % 5 * 39);
            float y = offsy + (float)(i / 5 * 35);
            FDButton skill = new FDSkillButton((Screen)this, x, y, 34.0f, 34.0f, bossSkill).setTexture(new FDButtonTextures(new WidgetTexture(FDBosses.location("textures/gui/ability_button_unselected.png"), 0.0f, 0.0f), new WidgetTexture(FDBosses.location("textures/gui/ability_button_selected.png"), 0.0f, 0.0f))).setOnHoverAction((fdWidget, guiGraphics, v, v1, v2) -> {
                if (!this.skillOpened) {
                    PoseStack matrices = guiGraphics.m_280168_();
                    matrices.m_85836_();
                    matrices.m_252880_(0.0f, 0.0f, 100.0f);
                    BossRenderUtil.renderBossScreenTooltip(guiGraphics, (Component)Component.m_237115_((String)"fdbosses.skills.skill_button"), v, v1, 200.0f, this.getBaseStringColor(), 1.0f, this.bossAbilitesWidget.bossAbilitiesButtonContainer.getX(), this.bossAbilitesWidget.bossAbilitiesButtonContainer.getY(), this.bossAbilitesWidget.bossAbilitiesButtonContainer.getX() + this.bossAbilitesWidget.bossAbilitiesButtonContainer.getWidth(), this.bossAbilitesWidget.bossAbilitiesButtonContainer.getY() + this.bossAbilitesWidget.bossAbilitiesButtonContainer.getHeight());
                    matrices.m_85849_();
                }
            }).setSound(BossSounds.BUTTON_CLICK.get()).setOnClickAction((fdWidget, mx, my, button) -> {
                if (button != 0) {
                    return false;
                }
                wasSkillRead = true;
                this.openSkillInfo(skillToOpen, true);
                return true;
            });
            bossAbilitesWidget.addChild("skill" + i, (FDWidget)skill);
        }
    }

    public void openSkillInfo(BossInfo info, boolean state) {
        if (!this.skillOpened && state) {
            if (info.getInfoStats() == null) {
                this.skillInfoText.setText(info.getInfoDescription(), 1.0f, this.getBaseStringColor(), true);
                this.skillInfoButton.setOnClickAction(null);
                this.skillStatsButton.setOnClickAction(null);
                this.skillInfoButton.setActive(false);
                this.skillStatsButton.setActive(false);
            } else {
                this.skillInfoText.setText(info.getInfoDescription(), 1.0f, this.getBaseStringColor(), true);
                this.skillInfoButton.setOnClickAction((fdWidget, v, v1, i) -> {
                    this.skillInfoText.setText(info.getInfoDescription(), 1.0f, this.getBaseStringColor(), true);
                    return true;
                });
                this.skillStatsButton.setOnClickAction((fdWidget, v, v1, i) -> {
                    this.skillInfoText.setText(info.getInfoStats(), 1.0f, this.getBaseStringColor(), true);
                    return true;
                });
                this.skillInfoButton.setActive(true);
                this.skillStatsButton.setActive(true);
            }
            this.skillInfoWidget.setSkillName(info.getInfoName(), this.getBaseStringColor());
            this.skillInfoWidget.setSkillImage(info.getInfoIcon());
            this.skillInfoWidget.moveWidgetTo(OPEN_TIME, -5.0f, 2.0f, FDEasings::easeOutBounce);
            this.skillOpened = true;
        } else if (this.skillOpened && !state) {
            this.skillInfoWidget.moveWidgetTo(OPEN_TIME / 2, -this.skillInfoWidget.getWidth(), 2.0f, FDEasings::easeIn);
            this.skillOpened = false;
        }
    }

    public void m_86600_() {
        super.m_86600_();
        this.openTicker = this.skillOpened ? Mth.m_14045_((int)(this.openTicker + 1), (int)0, (int)OPEN_TIME) : Mth.m_14045_((int)(this.openTicker - 1), (int)0, (int)OPEN_TIME);
        if (this.moveThings++ == 1) {
            this.bossDetailsWidget.moveWidgetTo(9, this.bossDetailsWidget.getX() - this.bossDetailsWidget.getWidth(), this.bossDetailsWidget.getY(), FDEasings::easeOut);
            this.bossAbilitesWidget.moveWidgetTo(9, this.bossAbilitesWidget.getX(), this.bossAbilitesWidget.getY() - this.bossAbilitesWidget.getHeight(), FDEasings::easeOut);
        }
    }

    public void m_88315_(GuiGraphics graphics, int mx, int my, float pticks) {
        PoseStack matrices = graphics.m_280168_();
        Vector2f anchorEnd = this.getAnchor(1.0f, 1.0f);
        matrices.m_85836_();
        matrices.m_85836_();
        matrices.m_252880_(0.0f, 0.0f, -1000.0f);
        FDRenderUtil.fill((PoseStack)matrices, (float)0.0f, (float)0.0f, (float)anchorEnd.x, (float)anchorEnd.y, (float)0.0f, (float)0.0f, (float)0.0f, (float)0.6f);
        matrices.m_85849_();
        this.renderBack(graphics, mx, my, pticks);
        this.renderBoss(graphics, mx, my, pticks);
        for (Renderable renderable : this.f_169369_) {
            if (renderable == this.skillInfoWidget || renderable == this.didntReadSkillWarningWidget) continue;
            renderable.m_88315_(graphics, mx, my, pticks);
        }
        this.renderSkillInfo(graphics, mx, my, pticks);
        this.renderDidntReadSkillInfoWidget(graphics, mx, my, pticks);
        this.renderSocialsWidget(graphics, mx, my, pticks);
        matrices.m_85849_();
    }

    private void renderDidntReadSkillInfoWidget(GuiGraphics graphics, int mx, int my, float pticks) {
        if (!this.didntReadSkillWarningWidget.m_142518_()) {
            return;
        }
        Vector2f anchorEnd = this.getAnchor(1.0f, 1.0f);
        PoseStack poseStack = graphics.m_280168_();
        poseStack.m_85836_();
        poseStack.m_252880_(0.0f, 0.0f, 200.0f);
        FDRenderUtil.fill((PoseStack)poseStack, (float)0.0f, (float)0.0f, (float)anchorEnd.x, (float)anchorEnd.y, (float)0.0f, (float)0.0f, (float)0.0f, (float)0.925f);
        this.didntReadSkillWarningWidget.m_88315_(graphics, mx, my, pticks);
        poseStack.m_85849_();
    }

    private void renderSocialsWidget(GuiGraphics graphics, int mx, int my, float pticks) {
        if (!this.socialsWidget.m_142518_()) {
            return;
        }
        Vector2f anchorEnd = this.getAnchor(1.0f, 1.0f);
        PoseStack poseStack = graphics.m_280168_();
        poseStack.m_85836_();
        poseStack.m_252880_(0.0f, 0.0f, 200.0f);
        FDRenderUtil.fill((PoseStack)poseStack, (float)0.0f, (float)0.0f, (float)anchorEnd.x, (float)anchorEnd.y, (float)0.0f, (float)0.0f, (float)0.0f, (float)0.9f);
        this.socialsWidget.m_88315_(graphics, mx, my, pticks);
        poseStack.m_85849_();
    }

    private void renderSkillInfo(GuiGraphics graphics, int mx, int my, float pticks) {
        Vector2f anchorEnd = this.getAnchor(1.0f, 1.0f);
        PoseStack matrices = graphics.m_280168_();
        matrices.m_85836_();
        matrices.m_252880_(0.0f, 0.0f, 200.0f);
        float a = (float)this.openTicker / (float)OPEN_TIME;
        a = this.skillOpened ? FDEasings.easeOut((float)FDEasings.easeOut((float)a)) : FDEasings.easeOut((float)a);
        FDRenderUtil.fill((PoseStack)matrices, (float)0.0f, (float)0.0f, (float)anchorEnd.x, (float)anchorEnd.y, (float)0.0f, (float)0.0f, (float)0.0f, (float)(a * 0.925f));
        this.skillInfoWidget.m_88315_(graphics, mx, my, pticks);
        matrices.m_85849_();
    }

    protected void renderBack(GuiGraphics graphics, float mx, float my, float pticks) {
    }

    public void m_7379_() {
        super.m_7379_();
        if (this.bossSpawnerId == -1) {
            Minecraft.m_91087_().m_91152_((Screen)new BossCodexScreen(false));
        }
    }

    public boolean m_6375_(double mx, double my, int button) {
        if (this.didntReadSkillWarningWidget.m_142518_()) {
            if (FDRenderUtil.isMouseInBounds((float)((float)mx), (float)((float)my), (float)this.didntReadSkillWarningWidget.getX(), (float)this.didntReadSkillWarningWidget.getY(), (float)this.didntReadSkillWarningWidget.getWidth(), (float)this.didntReadSkillWarningWidget.getHeight())) {
                return this.didntReadSkillWarningWidget.m_6375_(mx, my, button);
            }
            return false;
        }
        if (this.socialsWidget.m_142518_()) {
            if (FDRenderUtil.isMouseInBounds((float)((float)mx), (float)((float)my), (float)this.socialsWidget.getX(), (float)this.socialsWidget.getY(), (float)this.socialsWidget.getWidth(), (float)this.socialsWidget.getHeight())) {
                return this.socialsWidget.m_6375_(mx, my, button);
            }
            this.socialsWidget.setActive(false);
            return false;
        }
        if (this.skillOpened) {
            float xBorder = this.skillInfoWidget.getX() + this.skillInfoWidget.getWidth();
            if (mx > (double)xBorder) {
                this.openSkillInfo(null, false);
                return false;
            }
            if (FDRenderUtil.isMouseInBounds((float)((float)mx), (float)((float)my), (float)this.skillInfoWidget.getX(), (float)this.skillInfoWidget.getY(), (float)this.skillInfoWidget.getWidth(), (float)this.skillInfoWidget.getHeight())) {
                return this.skillInfoWidget.m_6375_(mx, my, button);
            }
            return false;
        }
        return super.m_6375_(mx, my, button);
    }

    public abstract Component getBossName();

    public abstract int getBaseStringColor();

    protected abstract void renderBoss(GuiGraphics var1, float var2, float var3, float var4);

    public float getBossMenuXStart() {
        return this.bossMenuXStart;
    }

    public boolean m_7043_() {
        return false;
    }
}


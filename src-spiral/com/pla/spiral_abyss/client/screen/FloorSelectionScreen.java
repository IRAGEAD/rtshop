/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.Util
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.AbstractWidget
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.PlayerFaceRenderer
 *  net.minecraft.client.gui.components.Tooltip
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner
 *  net.minecraft.client.multiplayer.PlayerInfo
 *  net.minecraft.client.resources.DefaultPlayerSkin
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.FormattedCharSequence
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.ItemLike
 */
package com.pla.spiral_abyss.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pla.spiral_abyss.client.overlay.AbyssClockRenderer;
import com.pla.spiral_abyss.client.overlay.AbyssRejectionNotice;
import com.pla.spiral_abyss.client.portal.PortalEntryAnimation;
import com.pla.spiral_abyss.client.screen.AbyssJournalScreen;
import com.pla.spiral_abyss.client.screen.ButtonArtwork;
import com.pla.spiral_abyss.client.screen.EndlessArtwork;
import com.pla.spiral_abyss.client.screen.EnemyPreviewRenderer;
import com.pla.spiral_abyss.client.screen.RewardPreviewPanel;
import com.pla.spiral_abyss.network.AbyssNetwork;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

public final class FloorSelectionScreen
extends Screen {
    private static final ResourceLocation BACKGROUND = FloorSelectionScreen.texture("background");
    private static final ResourceLocation EARTH = FloorSelectionScreen.texture("earth_image");
    private static final ItemStack STAR = new ItemStack((ItemLike)Items.f_42686_);
    private static final float ARTWORK_BUTTON_SCALE = 0.5f;
    private static final float WAVE_BUTTON_SCALE = 0.8f;
    private final AbyssNetwork.OpenFloors state;
    private List<AbyssNetwork.SectionView> sections;
    private int available;
    private final Map<Integer, AbstractWidget> sectionCards = new HashMap<Integer, AbstractWidget>();
    private final Map<Integer, Integer> sectionHeadPages = new HashMap<Integer, Integer>();
    private final EnemyPreviewRenderer previews = new EnemyPreviewRenderer();
    private final long openedAt = Util.m_137550_();
    private boolean replied;
    private int keepOpenTicks;
    private int selectedFloor;
    private int selectedWave;
    private int resetRowY;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int gridTop;
    private int gridBottom;
    private int scroll;
    private int maxScroll;
    private int waveContentY;
    private int maxChallengeLines;
    private RewardPreviewPanel rewardPanel;

    public static void updateSections(AbyssNetwork.SectionStatus status) {
        Screen screen = Minecraft.m_91087_().f_91080_;
        if (screen instanceof FloorSelectionScreen) {
            FloorSelectionScreen screen2 = (FloorSelectionScreen)screen;
            if (screen2.state.token().equals(status.token())) {
                screen2.sections = status.sections();
                if (screen2.available != status.selectable()) {
                    screen2.available = status.selectable();
                    screen2.m_169413_();
                    screen2.m_7856_();
                }
                screen2.updateSectionTooltips();
            }
        }
    }

    public static void closeForReset() {
        PortalEntryAnimation.closeForReset();
        Screen screen = Minecraft.m_91087_().f_91080_;
        if (screen instanceof FloorSelectionScreen) {
            FloorSelectionScreen screen2 = (FloorSelectionScreen)screen;
            screen2.replied = true;
            screen2.f_96541_.m_91152_(null);
        } else if (Minecraft.m_91087_().f_91080_ instanceof AbyssJournalScreen) {
            Minecraft.m_91087_().m_91152_(null);
        }
    }

    public FloorSelectionScreen(AbyssNetwork.OpenFloors state) {
        super((Component)Component.m_237115_((String)"screen.spiral_abyss.floors"));
        this.state = state;
        this.sections = state.sections();
        this.available = state.selectable();
    }

    public static void open(AbyssNetwork.OpenFloors state) {
        Minecraft.m_91087_().m_91152_((Screen)new FloorSelectionScreen(state));
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation("spiral_abyss", "textures/gui/floors/" + name + ".png");
    }

    private boolean selectable(int floor) {
        return (this.available & 1 << floor) != 0;
    }

    private ResourceLocation floorTexture(int floor) {
        String suffix = (this.state.claimed() & 1 << floor) != 0 ? "_looted" : ((this.state.unlocked() & 1 << floor) != 0 ? "" : "_locked");
        return FloorSelectionScreen.texture("floor_" + floor + suffix);
    }

    private AbyssNetwork.FloorPreview floor() {
        return this.state.floors().stream().filter(f -> f.floor() == this.selectedFloor).findFirst().orElse(null);
    }

    private AbyssNetwork.WavePreview wave(int number) {
        AbyssNetwork.FloorPreview floor = this.floor();
        return floor == null ? null : (AbyssNetwork.WavePreview)floor.waves().stream().filter(w -> w.wave() == number).findFirst().orElse(null);
    }

    private void navigate(int floor, int wave) {
        this.selectedFloor = floor;
        this.selectedWave = wave;
        this.scroll = 0;
        this.m_7522_(null);
        this.m_169413_();
        this.m_7856_();
    }

    protected void m_7856_() {
        this.rewardPanel = null;
        if (this.selectedFloor == 0) {
            this.initOverview();
        } else if (this.selectedFloor == 7) {
            this.initEndless();
        } else if (this.selectedWave == 0) {
            this.initFloor();
        } else {
            this.initWave();
        }
    }

    public void m_86600_() {
        super.m_86600_();
        if (!this.replied && this.keepOpenTicks-- <= 0) {
            AbyssNetwork.keepFloorsOpen(this.state.token());
            this.keepOpenTicks = 39;
        }
    }

    private void chooseSectionCard(int floor) {
        this.navigate(floor, 0);
    }

    private void initOverview() {
        this.sectionCards.clear();
        int gap = Math.max(2, Math.min(12, this.f_96543_ / 80));
        int count = Integer.bitCount(this.state.visible() & 0xFE);
        int cardWidth = Math.max(1, Math.min((this.f_96543_ - 24 - 6 * gap) / 7, Math.max(1, this.f_96544_ - 132) * 256 / 455));
        int cardHeight = cardWidth * 455 / 256;
        int left = (this.f_96543_ - count * cardWidth - Math.max(0, count - 1) * gap) / 2;
        int top = Math.max(48, (this.f_96544_ - cardHeight) / 2 - 12);
        int index = 0;
        for (int floor = 1; floor <= 7; ++floor) {
            if ((this.state.visible() & 1 << floor) == 0) continue;
            int number = floor;
            int x = left + index++ * (cardWidth + gap);
            MutableComponent label = Component.m_237110_((String)(floor == 7 ? "screen.spiral_abyss.endless" : "screen.spiral_abyss.floor"), (Object[])new Object[]{floor});
            Button card = floor == 7 ? (Button)this.m_142416_((GuiEventListener)new Button(x, top, cardWidth, cardHeight, (Component)label, pressed -> this.chooseSectionCard(7), narration -> (MutableComponent)narration.get()){

                protected void m_87963_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                    EndlessArtwork.render(graphics, this.m_252754_(), this.m_252907_(), this.m_5711_(), this.m_93694_());
                    if (this.m_198029_()) {
                        graphics.m_280637_(this.m_252754_(), this.m_252907_(), this.m_5711_(), this.m_93694_(), -862836);
                    }
                }
            }) : this.imageButton(x, top, cardWidth, cardHeight, (Component)label, this.floorTexture(floor), 256, 455, () -> this.chooseSectionCard(number));
            this.sectionCards.put(floor, (AbstractWidget)card);
            card.m_257544_(Tooltip.m_257550_((Component)(floor == 7 ? label : Component.m_237110_((String)"screen.spiral_abyss.view_floor", (Object[])new Object[]{floor}))));
        }
        this.updateSectionTooltips();
        int buttonY = Math.min(this.f_96544_ - 60, top + cardHeight + 15);
        this.resetRowY = buttonY + 37;
        MutableComponent exitLabel = Component.m_237115_((String)(this.state.inside() ? "screen.spiral_abyss.return_overworld" : "screen.spiral_abyss.close"));
        Button exit = this.imageButton(this.f_96543_ / 2 - 16, buttonY, 32, 27, (Component)exitLabel, EARTH, 564, 468, () -> this.submit(this.state.inside() ? -1 : 0));
        exit.m_257544_(Tooltip.m_257550_((Component)exitLabel));
    }

    private void layoutPanel() {
        this.panelWidth = Math.min(660, Math.max(120, this.f_96543_ - 24));
        this.panelHeight = Math.min(360, Math.max(140, this.f_96544_ - 24));
        this.panelX = (this.f_96543_ - this.panelWidth) / 2;
        this.panelY = (this.f_96544_ - this.panelHeight) / 2;
    }

    private void initEndless() {
        this.layoutPanel();
        int right = this.panelX + this.panelWidth / 3;
        int rightWidth = this.panelWidth * 2 / 3 - 20;
        this.artworkButton(right, this.panelY + this.panelHeight - 29, 30, 15, (Component)Component.m_237115_((String)"gui.back"), ButtonArtwork.BACK, false, () -> this.navigate(0, 0));
        Button start = this.artworkButton(this.panelX + this.panelWidth - 100, this.panelY + this.panelHeight - 29, 80, 15, (Component)Component.m_237115_((String)"screen.spiral_abyss.fight"), ButtonArtwork.FIGHT, false, () -> this.submit(7));
        start.f_93623_ = this.selectable(7);
        if (!start.f_93623_) {
            start.m_257544_(Tooltip.m_257550_((Component)Component.m_237115_((String)"screen.spiral_abyss.unavailable")));
        }
    }

    private void renderEndless(GuiGraphics graphics) {
        int cardHeight = Math.min(this.panelHeight - 32, (this.panelWidth / 3 - 24) * 228 / 128);
        int cardWidth = cardHeight * 128 / 228;
        EndlessArtwork.render(graphics, this.panelX + 12, this.panelY + 12, cardWidth, cardHeight);
        int right = this.panelX + this.panelWidth / 3;
        int textWidth = this.panelWidth * 2 / 3 - 20;
        int y = this.panelY + 18;
        graphics.m_280614_(this.f_96547_, (Component)Component.m_237115_((String)"screen.spiral_abyss.endless"), right, y, 16774103, false);
        graphics.m_280614_(this.f_96547_, (Component)Component.m_237110_((String)"hud.spiral_abyss.endless_wave", (Object[])new Object[]{this.state.endlessWave()}), right, y += 22, -1, false);
        y += 22;
        for (FormattedCharSequence line : this.f_96547_.m_92923_((FormattedText)Component.m_237115_((String)"screen.spiral_abyss.endless_description"), textWidth)) {
            Objects.requireNonNull(this.f_96547_);
            if (y + 9 > this.panelY + this.panelHeight - 44) break;
            graphics.m_280649_(this.f_96547_, line, right, y, -2561542, false);
            Objects.requireNonNull(this.f_96547_);
            y += 9 + 3;
        }
    }

    private void initFloor() {
        this.layoutPanel();
        int cardHeight = Math.min(this.panelHeight - 48, this.panelWidth / 4 * 455 / 256);
        int cardWidth = cardHeight * 256 / 455;
        int right = this.panelX + cardWidth + 32;
        int rightWidth = this.panelWidth - cardWidth - 48;
        int rowHeight = Math.max(18, Math.min(38, (this.panelHeight - 148) / 3));
        int rowGap = 4;
        for (int i = 1; i <= 3; ++i) {
            final int number = i;
            Button row = new Button(right, this.panelY + 32 + (i - 1) * (rowHeight + rowGap), rightWidth, rowHeight, (Component)Component.m_237110_((String)"screen.spiral_abyss.wave", (Object[])new Object[]{i}), button -> this.navigate(this.selectedFloor, number), narration -> (MutableComponent)narration.get()){

                protected void m_87963_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                    graphics.m_280509_(this.m_252754_(), this.m_252907_(), this.m_252754_() + this.m_5711_(), this.m_252907_() + this.m_93694_(), this.m_198029_() ? -296589696 : -583447710);
                    graphics.m_280637_(this.m_252754_(), this.m_252907_(), this.m_5711_(), this.m_93694_(), this.m_198029_() ? -862836 : -7628115);
                    Font font = FloorSelectionScreen.this.f_96547_;
                    Component component = this.m_6035_();
                    int n = this.m_252754_() + 12;
                    int n2 = this.m_252907_();
                    int n3 = this.m_93694_();
                    Objects.requireNonNull(FloorSelectionScreen.this.f_96547_);
                    graphics.m_280614_(font, component, n, n2 + (n3 - 9) / 2, -1, false);
                    AbyssNetwork.WavePreview preview = FloorSelectionScreen.this.wave(number);
                    FloorSelectionScreen.this.stars(graphics, this.m_252754_() + this.m_5711_() - 62, this.m_252907_() + (this.m_93694_() - 16) / 2, preview == null ? 0 : preview.stars());
                }
            };
            AbyssNetwork.WavePreview preview = this.wave(number);
            row.m_257544_(Tooltip.m_257550_((Component)Component.m_237110_((String)"screen.spiral_abyss.wave_stars", (Object[])new Object[]{preview == null ? 0 : preview.stars()})));
            this.m_142416_((GuiEventListener)row);
        }
        int rowsBottom = this.panelY + 32 + 3 * rowHeight + 2 * rowGap;
        int rewardY = rowsBottom + 20;
        this.rewardPanel = new RewardPreviewPanel(this.floor() == null ? List.of() : this.floor().rewards(), right, rewardY, rightWidth, Math.max(22, this.panelY + this.panelHeight - 40 - rewardY));
        int actionHeight = 15;
        int actionY = this.panelY + this.panelHeight - 29;
        this.artworkButton(right, actionY, Math.max(20, rightWidth / 4), actionHeight, (Component)Component.m_237115_((String)"gui.back"), ButtonArtwork.BACK, false, () -> this.navigate(0, 0));
        int fightWidth = ButtonArtwork.FIGHT.fittedWidth(Math.max(1, Math.round((float)rightWidth / 2.0f * 0.5f)), actionHeight);
        Button start = this.artworkButton(right + rightWidth - fightWidth, actionY, fightWidth, actionHeight, (Component)Component.m_237115_((String)"screen.spiral_abyss.fight"), ButtonArtwork.FIGHT, false, () -> this.submit(this.selectedFloor));
        start.f_93623_ = this.selectable(this.selectedFloor);
        if (!start.f_93623_) {
            start.m_257544_(Tooltip.m_257550_((Component)Component.m_237115_((String)"screen.spiral_abyss.unavailable")));
        }
    }

    private void initWave() {
        this.layoutPanel();
        int tabWidth = (this.panelWidth - 32) / 3;
        int tabHeight = Math.round(0.8f * (float)Math.max(18, Math.min(36, (this.panelHeight - 120) / 4)));
        for (int i = 1; i <= 3; ++i) {
            int number = i;
            ButtonArtwork artwork = ButtonArtwork.WAVE;
            int imageWidth = artwork.fittedWidth(Math.max(1, Math.round((float)tabWidth * 0.8f)), tabHeight);
            Button tab = this.artworkButton(this.panelX + 12 + (i - 1) * (tabWidth + 4) + (tabWidth - imageWidth) / 2, this.panelY + 10, imageWidth, tabHeight, (Component)Component.m_237110_((String)"screen.spiral_abyss.wave", (Object[])new Object[]{i}), artwork, i == this.selectedWave, () -> this.navigate(this.selectedFloor, number));
            tab.f_93623_ = i != this.selectedWave;
        }
        this.waveContentY = this.panelY + 20 + tabHeight;
        int availableRowHeight = Math.max(16, Math.min(34, (this.panelHeight - tabHeight - 94) / 3));
        Objects.requireNonNull(this.f_96547_);
        this.maxChallengeLines = Math.max(1, (availableRowHeight - 4) / 9);
        Objects.requireNonNull(this.f_96547_);
        this.gridTop = this.waveContentY + 2 * 9 + 22;
        AbyssNetwork.WavePreview preview = this.wave(this.selectedWave);
        if (preview != null) {
            for (Component objective : preview.challenges()) {
                this.gridTop += this.challengeRowHeight(objective);
            }
        }
        this.gridBottom = this.panelY + this.panelHeight - 44;
        int backHeight = Math.round(15.0f);
        this.artworkButton(this.panelX + 14, this.panelY + this.panelHeight - 38 + (30 - backHeight) / 2, Math.round(12.0f), backHeight, (Component)Component.m_237115_((String)"gui.back"), ButtonArtwork.BACK, false, () -> this.navigate(this.selectedFloor, 0));
    }

    private Button artworkButton(int x, int y, int maxWidth, int maxHeight, Component label, final ButtonArtwork artwork, final boolean selected, Runnable action) {
        int width = artwork.fittedWidth(maxWidth, maxHeight);
        int height = artwork.fittedHeight(width);
        3 button = this.m_142416_((GuiEventListener)new Button(x, y + (maxHeight - height) / 2, width, height, label, pressed -> action.run(), narration -> (MutableComponent)narration.get()){

            protected void m_87963_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                artwork.render(graphics, this.m_252754_(), this.m_252907_(), this.m_5711_(), this.m_93694_(), this.m_198029_(), selected, this.f_93623_);
                if (artwork == ButtonArtwork.WAVE || artwork == ButtonArtwork.FIGHT) {
                    int textWidth = FloorSelectionScreen.this.f_96547_.m_92852_((FormattedText)this.m_6035_());
                    float f = (float)this.m_5711_() * 0.76f / (float)Math.max(1, textWidth);
                    float f2 = (float)this.m_93694_() * 0.65f;
                    Objects.requireNonNull(FloorSelectionScreen.this.f_96547_);
                    float scale = Math.min(1.0f, Math.min(f, f2 / 9.0f));
                    graphics.m_280168_().m_85836_();
                    graphics.m_280168_().m_252880_((float)this.m_252754_() + (float)this.m_5711_() / 2.0f, (float)this.m_252907_() + (float)this.m_93694_() / 2.0f, 0.0f);
                    graphics.m_280168_().m_85841_(scale, scale, 1.0f);
                    Font font = FloorSelectionScreen.this.f_96547_;
                    Component component = this.m_6035_();
                    int n = -textWidth / 2;
                    Objects.requireNonNull(FloorSelectionScreen.this.f_96547_);
                    graphics.m_280614_(font, component, n, -9 / 2, this.f_93623_ || selected ? -14866893 : -5592406, false);
                    graphics.m_280168_().m_85849_();
                }
            }
        });
        button.m_257544_(Tooltip.m_257550_((Component)label));
        return button;
    }

    private Button imageButton(int x, int y, int w, int h, Component label, final ResourceLocation icon, final int sourceW, final int sourceH, Runnable action) {
        return (Button)this.m_142416_((GuiEventListener)new Button(x, y, w, h, label, button -> action.run(), narration -> (MutableComponent)narration.get()){

            protected void m_87963_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                RenderSystem.enableBlend();
                graphics.m_280411_(icon, this.m_252754_(), this.m_252907_(), this.m_5711_(), this.m_93694_(), 0.0f, 0.0f, sourceW, sourceH, sourceW, sourceH);
                if (this.m_198029_()) {
                    graphics.m_280637_(this.m_252754_(), this.m_252907_(), this.m_5711_(), this.m_93694_(), -862836);
                }
                RenderSystem.disableBlend();
            }
        });
    }

    private void stars(GuiGraphics graphics, int x, int y, int earned) {
        for (int i = 0; i < 3; ++i) {
            graphics.m_280480_(STAR, x + i * 19, y);
            if (i < earned) continue;
            graphics.m_280168_().m_85836_();
            graphics.m_280168_().m_252880_(0.0f, 0.0f, 200.0f);
            graphics.m_280509_(x + i * 19, y, x + i * 19 + 16, y + 16, -869713843);
            graphics.m_280168_().m_85849_();
        }
    }

    public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        float scale = Math.max((float)this.f_96543_ / 2560.0f, (float)this.f_96544_ / 1440.0f);
        int imageWidth = (int)Math.ceil(2560.0f * scale);
        int imageHeight = (int)Math.ceil(1440.0f * scale);
        graphics.m_280411_(BACKGROUND, (this.f_96543_ - imageWidth) / 2, (this.f_96544_ - imageHeight) / 2, imageWidth, imageHeight, 0.0f, 0.0f, 2560, 1440, 2560, 1440);
        graphics.m_280509_(0, 0, this.f_96543_, this.f_96544_, 1426593065);
        if (this.selectedFloor == 0) {
            this.renderOverview(graphics);
        } else {
            graphics.m_280509_(this.panelX, this.panelY, this.panelX + this.panelWidth, this.panelY + this.panelHeight, -299551411);
            graphics.m_280637_(this.panelX, this.panelY, this.panelWidth, this.panelHeight, -9207145);
            if (this.selectedFloor == 7) {
                this.renderEndless(graphics);
            } else if (this.selectedWave == 0) {
                this.renderFloor(graphics);
            } else {
                this.renderWave(graphics, mouseX, mouseY);
            }
        }
        boolean overRewardTooltip = this.rewardPanel != null && this.rewardPanel.popupContains(mouseX, mouseY);
        super.m_88315_(graphics, overRewardTooltip ? -1 : mouseX, overRewardTooltip ? -1 : mouseY, partialTick);
        if (this.selectedFloor == 0) {
            this.renderSections(graphics, mouseX, mouseY);
        }
        if (this.rewardPanel != null) {
            this.rewardPanel.render(graphics, mouseX, mouseY, this.f_96543_, this.f_96544_);
        }
    }

    private Component sectionLabel(AbyssNetwork.SectionView section) {
        return Component.m_237115_((String)("screen.spiral_abyss.section_" + (section.status() == 1 ? "fighting" : (section.status() == 2 ? "claiming" : "idle"))));
    }

    private void updateSectionTooltips() {
        for (Map.Entry<Integer, AbstractWidget> entry : this.sectionCards.entrySet()) {
            MutableComponent text = Component.m_237110_((String)"screen.spiral_abyss.view_floor", (Object[])new Object[]{entry.getKey()});
            for (AbyssNetwork.SectionView section : this.sections) {
                if (section.floor() != entry.getKey().intValue()) continue;
                text.m_130946_("\n").m_7220_(this.sectionLabel(section));
                text.m_130946_("\n").m_7220_((Component)Component.m_237110_((String)"screen.spiral_abyss.section_players", (Object[])new Object[]{section.players().size()}));
            }
            entry.getValue().m_257544_(Tooltip.m_257550_((Component)text));
        }
    }

    private HeadGrid headGrid(AbstractWidget card) {
        int size = Math.max(4, Math.min(10, Math.min(card.m_5711_() - 6, card.m_93694_() - 20)));
        int columns = Math.max(1, (card.m_5711_() - 4) / (size + 2));
        int rows = Math.max(1, Math.min(2, (card.m_93694_() - 18) / (size + 2)));
        return new HeadGrid(size, columns, rows);
    }

    private int headPage(AbyssNetwork.SectionView section, HeadGrid grid) {
        return Math.max(0, Math.min(grid.pages(section.players().size()) - 1, this.sectionHeadPages.getOrDefault(section.floor(), 0)));
    }

    private void renderSections(GuiGraphics graphics, int mouseX, int mouseY) {
        MutableComponent hovered = null;
        for (AbyssNetwork.SectionView section : this.sections) {
            AbstractWidget card = this.sectionCards.get(section.floor());
            if (card == null || section.players().isEmpty() || card.m_5711_() < 8 || card.m_93694_() < 26) continue;
            int x = card.m_252754_();
            int bottom = card.m_252907_() + card.m_93694_();
            HeadGrid grid = this.headGrid(card);
            int count = section.players().size();
            int page = this.headPage(section, grid);
            int first = page * grid.pageSize(count);
            int shown = Math.min(grid.pageSize(count), count - first);
            boolean overflow = count > grid.capacity();
            int cells = shown + (overflow && grid.capacity() > 1 ? 1 : 0);
            int rows = (cells + grid.columns() - 1) / grid.columns();
            int panelHeight = rows * (grid.size() + 2) + 18;
            graphics.m_280509_(x + 1, bottom - panelHeight, x + card.m_5711_() - 1, bottom - 1, -653255893);
            for (int cell = 0; cell < cells; ++cell) {
                MutableComponent tooltip;
                int row = cell / grid.columns();
                int column = cell % grid.columns();
                int rowCells = Math.min(grid.columns(), cells - row * grid.columns());
                int headX = x + (card.m_5711_() - rowCells * (grid.size() + 2) + 2) / 2 + column * (grid.size() + 2);
                int headY = bottom - panelHeight + 3 + row * (grid.size() + 2);
                if (cell < shown) {
                    AbyssNetwork.SectionPlayer person = section.players().get(first + cell);
                    PlayerInfo info = this.f_96541_.m_91403_() == null ? null : this.f_96541_.m_91403_().m_104949_(person.id());
                    ResourceLocation skin = info == null ? DefaultPlayerSkin.m_118627_((UUID)person.id()) : info.m_105337_();
                    PlayerFaceRenderer.m_280354_((GuiGraphics)graphics, (ResourceLocation)skin, (int)headX, (int)headY, (int)grid.size());
                    tooltip = Component.m_237113_((String)person.name());
                } else {
                    this.drawFitted(graphics, (Component)Component.m_237113_((String)("+" + (count - shown))), headX, headY, grid.size(), 0xFFFFFF);
                    tooltip = Component.m_237110_((String)"screen.spiral_abyss.section_players", (Object[])new Object[]{count});
                }
                if (mouseX < headX || mouseX >= headX + grid.size() || mouseY < headY || mouseY >= headY + grid.size()) continue;
                graphics.m_280637_(headX - 1, headY - 1, grid.size() + 2, grid.size() + 2, -10645);
                hovered = overflow ? tooltip.m_6881_().m_130946_("\n").m_7220_((Component)Component.m_237110_((String)"screen.spiral_abyss.section_players_scroll", (Object[])new Object[]{page + 1, grid.pages(count)})) : tooltip;
            }
            String label = this.sectionLabel(section).getString();
            graphics.m_280137_(this.f_96547_, this.f_96547_.m_92834_(label, Math.max(1, card.m_5711_() - 4)), x + card.m_5711_() / 2, bottom - 12, section.status() == 0 ? 11855792 : 16766571);
        }
        if (hovered != null) {
            this.m_262791_(Tooltip.m_257550_(hovered), DefaultTooltipPositioner.f_262752_, true);
        }
    }

    private void renderOverview(GuiGraphics graphics) {
        graphics.m_280653_(this.f_96547_, this.f_96539_, this.f_96543_ / 2, 12, 16774103);
        long remaining = this.state.resetRemainingMillis() < 0L ? -1L : Math.max(0L, this.state.resetRemainingMillis() - (Util.m_137550_() - this.openedAt));
        AbyssClockRenderer.reset(graphics, this.f_96547_, this.f_96543_ / 2, this.resetRowY, this.f_96543_ - 24, remaining);
    }

    private void renderFloor(GuiGraphics graphics) {
        int cardHeight = Math.min(this.panelHeight - 48, this.panelWidth / 4 * 455 / 256);
        int cardWidth = cardHeight * 256 / 455;
        RenderSystem.enableBlend();
        graphics.m_280411_(this.floorTexture(this.selectedFloor), this.panelX + 12, this.panelY + 12, cardWidth, cardHeight, 0.0f, 0.0f, 256, 455, 256, 455);
        RenderSystem.disableBlend();
        graphics.m_280614_(this.f_96547_, (Component)Component.m_237110_((String)"screen.spiral_abyss.floor", (Object[])new Object[]{this.selectedFloor}), this.panelX + cardWidth + 32, this.panelY + 17, -1, false);
    }

    private int challengeRowHeight(Component objective) {
        int lines = Math.min(this.maxChallengeLines, this.f_96547_.m_92923_((FormattedText)objective, Math.max(1, this.panelWidth - 52)).size());
        Objects.requireNonNull(this.f_96547_);
        return Math.max(16, lines * 9 + 3);
    }

    private void renderWave(GuiGraphics graphics, int mouseX, int mouseY) {
        int y;
        graphics.m_280614_(this.f_96547_, (Component)Component.m_237115_((String)"screen.spiral_abyss.challenge"), this.panelX + 15, this.waveContentY, -1, false);
        List<Component> objectives = this.wave(this.selectedWave) == null ? List.of() : this.wave(this.selectedWave).challenges();
        Component hoveredObjective = null;
        Objects.requireNonNull(this.f_96547_);
        int challengeY = this.waveContentY + 2 * 9;
        for (Component objective : objectives) {
            int rowHeight = this.challengeRowHeight(objective);
            graphics.m_280480_(STAR, this.panelX + 14, challengeY - 3);
            List lines = this.f_96547_.m_92923_((FormattedText)objective, Math.max(1, this.panelWidth - 52));
            if (this.maxChallengeLines == 1) {
                this.drawFitted(graphics, objective, this.panelX + 36, challengeY, this.panelWidth - 50, -1);
            } else {
                for (int line = 0; line < Math.min(this.maxChallengeLines, lines.size()); ++line) {
                    FormattedCharSequence formattedCharSequence = (FormattedCharSequence)lines.get(line);
                    Objects.requireNonNull(this.f_96547_);
                    graphics.m_280649_(this.f_96547_, formattedCharSequence, this.panelX + 36, challengeY + line * 9, -1, false);
                }
            }
            if (mouseX >= this.panelX + 14 && mouseX < this.panelX + this.panelWidth - 14 && mouseY >= challengeY - 3 && mouseY < challengeY + rowHeight - 3) {
                hoveredObjective = objective;
            }
            challengeY += rowHeight;
        }
        graphics.m_280614_(this.f_96547_, (Component)Component.m_237115_((String)"screen.spiral_abyss.enemies"), this.panelX + 15, this.gridTop - 15, -1, false);
        AbyssNetwork.WavePreview preview = this.wave(this.selectedWave);
        List<Object> enemies = preview == null ? List.of() : preview.enemies();
        int columns = Math.max(1, (this.panelWidth - 30) / 86);
        int tileWidth = (this.panelWidth - 30) / columns;
        int tileHeight = Math.max(36, Math.min(88, this.gridBottom - this.gridTop));
        int rows = (enemies.size() + columns - 1) / columns;
        this.maxScroll = Math.max(0, rows * tileHeight - Math.max(1, this.gridBottom - this.gridTop));
        this.scroll = Math.min(this.scroll, this.maxScroll);
        Component hovered = hoveredObjective;
        boolean hoveredRejection = false;
        graphics.m_280588_(this.panelX + 10, this.gridTop, this.panelX + this.panelWidth - 10, Math.max(this.gridTop, this.gridBottom));
        for (int index = 0; index < enemies.size(); ++index) {
            int x = this.panelX + 14 + index % columns * tileWidth;
            y = this.gridTop + index / columns * tileHeight - this.scroll;
            if (y + tileHeight < this.gridTop || y > this.gridBottom) continue;
            AbyssNetwork.EnemyPreview enemy = (AbyssNetwork.EnemyPreview)enemies.get(index);
            graphics.m_280509_(x, y, x + tileWidth - 4, y + tileHeight - 5, 1614038366);
            this.previews.render(graphics, enemy.id(), enemy.animation(), x + 4, y + 3, tileWidth - 12, tileHeight - 24);
            graphics.m_280653_(this.f_96547_, (Component)Component.m_237110_((String)"screen.spiral_abyss.enemy_count", (Object[])new Object[]{enemy.count()}), x + (tileWidth - 4) / 2, y + tileHeight - 18, -1);
            if (mouseX >= x && mouseX < x + tileWidth - 4 && mouseY >= Math.max(y, this.gridTop) && mouseY < Math.min(y + tileHeight - 5, this.gridBottom)) {
                hovered = this.previews.name(enemy.id());
            }
            if (!enemy.rejected()) continue;
            int warningX = x + tileWidth - 19;
            int warningY = y + 4;
            graphics.m_280168_().m_85836_();
            graphics.m_280168_().m_252880_(0.0f, 0.0f, 200.0f);
            graphics.m_280509_(warningX - 2, warningY - 2, warningX + 13, warningY + 13, -435154917);
            AbyssRejectionNotice.icon(graphics, warningX, warningY);
            graphics.m_280168_().m_85849_();
            if (mouseX < warningX - 2 || mouseX >= warningX + 13 || mouseY < Math.max(warningY - 2, this.gridTop) || mouseY >= Math.min(warningY + 13, this.gridBottom)) continue;
            hovered = AbyssRejectionNotice.tooltip(this.previews.name(enemy.id()));
            hoveredRejection = true;
        }
        graphics.m_280618_();
        if (enemies.isEmpty()) {
            graphics.m_280653_(this.f_96547_, (Component)Component.m_237115_((String)"screen.spiral_abyss.preview_unavailable"), this.f_96543_ / 2, this.gridTop + 12, 14215674);
        }
        if (this.maxScroll > 0) {
            int track = Math.max(1, this.gridBottom - this.gridTop);
            int thumb = Math.max(12, track * track / Math.max(1, rows * tileHeight));
            y = this.gridTop + (track - thumb) * this.scroll / this.maxScroll;
            graphics.m_280509_(this.panelX + this.panelWidth - 7, this.gridTop, this.panelX + this.panelWidth - 4, this.gridBottom, -13286053);
            graphics.m_280509_(this.panelX + this.panelWidth - 7, y, this.panelX + this.panelWidth - 4, y + thumb, -5062189);
        }
        if (hovered != null) {
            if (hoveredRejection) {
                graphics.m_280245_(this.f_96547_, this.f_96547_.m_92923_((FormattedText)hovered, Math.max(40, Math.min(280, this.f_96543_ - 24))), mouseX, mouseY);
            } else {
                graphics.m_280557_(this.f_96547_, hovered, mouseX, mouseY);
            }
        }
    }

    private void drawFitted(GuiGraphics graphics, Component text, int x, int y, int available, int color) {
        float scale = Math.min(1.0f, (float)available / (float)Math.max(1, this.f_96547_.m_92852_((FormattedText)text)));
        graphics.m_280168_().m_85836_();
        graphics.m_280168_().m_252880_((float)x, (float)y, 0.0f);
        graphics.m_280168_().m_85841_(scale, scale, 1.0f);
        graphics.m_280614_(this.f_96547_, text, 0, 0, color, false);
        graphics.m_280168_().m_85849_();
    }

    public boolean m_6050_(double mouseX, double mouseY, double amount) {
        if (this.selectedFloor == 0 && amount != 0.0) {
            for (AbyssNetwork.SectionView section : this.sections) {
                AbstractWidget card = this.sectionCards.get(section.floor());
                if (card == null || !card.m_5953_(mouseX, mouseY) || section.players().isEmpty()) continue;
                HeadGrid grid = this.headGrid(card);
                if (section.players().size() <= grid.capacity()) continue;
                this.sectionHeadPages.put(section.floor(), Math.max(0, Math.min(grid.pages(section.players().size()) - 1, this.headPage(section, grid) + (amount > 0.0 ? -1 : 1))));
                return true;
            }
        }
        if (this.rewardPanel != null && this.rewardPanel.scroll(mouseX, mouseY, amount)) {
            return true;
        }
        if (this.selectedWave != 0 && mouseX >= (double)this.panelX && mouseX < (double)(this.panelX + this.panelWidth) && mouseY >= (double)this.gridTop && mouseY < (double)this.gridBottom) {
            this.scroll = Math.max(0, Math.min(this.maxScroll, this.scroll - (int)(amount * 30.0)));
            return true;
        }
        return super.m_6050_(mouseX, mouseY, amount);
    }

    public boolean m_6375_(double mouseX, double mouseY, int button) {
        if (this.rewardPanel != null && this.rewardPanel.popupContains(mouseX, mouseY)) {
            return true;
        }
        return super.m_6375_(mouseX, mouseY, button);
    }

    private void submit(int floor) {
        if (this.replied) {
            return;
        }
        this.replied = true;
        AbyssNetwork.select(this.state.token(), floor);
        this.f_96541_.m_91152_(null);
    }

    public void m_7379_() {
        if (this.selectedWave != 0) {
            this.navigate(this.selectedFloor, 0);
        } else if (this.selectedFloor != 0) {
            this.navigate(0, 0);
        } else {
            this.f_96541_.m_91152_(null);
        }
    }

    public void m_7861_() {
        if (!this.replied) {
            this.replied = true;
            AbyssNetwork.select(this.state.token(), 0);
        }
        this.previews.clear();
    }

    public boolean m_7043_() {
        return false;
    }

    private record HeadGrid(int size, int columns, int rows) {
        int capacity() {
            return this.columns * this.rows;
        }

        int pageSize(int count) {
            return count > this.capacity() ? Math.max(1, this.capacity() - 1) : this.capacity();
        }

        int pages(int count) {
            return (count + this.pageSize(count) - 1) / this.pageSize(count);
        }
    }
}


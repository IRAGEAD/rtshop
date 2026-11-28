/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.FormattedCharSequence
 *  net.minecraft.util.Mth
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.ItemLike
 */
package com.pla.spiral_abyss.client.overlay;

import com.pla.spiral_abyss.client.overlay.AbyssClockRenderer;
import com.pla.spiral_abyss.client.overlay.AbyssRejectionNotice;
import com.pla.spiral_abyss.client.platform.AbyssClientEvents;
import com.pla.spiral_abyss.client.screen.AbyssHudConfigScreen;
import com.pla.spiral_abyss.config.AbyssClientConfig;
import com.pla.spiral_abyss.network.AbyssNetwork;
import com.pla.spiral_abyss.world.AbyssDimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

public final class AbyssEncounterOverlay {
    private static final ItemStack STAR = new ItemStack((ItemLike)Items.f_42686_);
    private static final float HUD_SCALE = 0.882f;
    private static final float TIMER_SCALE = 1.5f;
    private static final float COUNTDOWN_SCALE = 4.0f;
    private static final int OBJECTIVE_WIDTH = 240;
    private static AbyssNetwork.EncounterStatus state = AbyssNetwork.EncounterStatus.HIDDEN;
    public static final AbyssClientEvents.Overlay HUD = (gui, graphics, partialTick, width, height) -> {
        Minecraft client = Minecraft.m_91087_();
        if (!state.active() || client.f_91066_.f_92062_ || client.f_91074_ == null || client.f_91073_ == null || !client.f_91073_.m_46472_().equals(AbyssDimension.LEVEL) || client.f_91080_ instanceof AbyssHudConfigScreen) {
            return;
        }
        AbyssEncounterOverlay.render(graphics, client.f_91062_, width, height, state);
    };

    private AbyssEncounterOverlay() {
    }

    public static void update(AbyssNetwork.EncounterStatus status) {
        state = status;
    }

    public static void logout(AbyssClientEvents.ClientPlayerNetworkEvent.LoggingOut event) {
        state = AbyssNetwork.EncounterStatus.HIDDEN;
    }

    public static Bounds bounds(AbyssClientConfig.HudGroup group, int width, int height) {
        return AbyssEncounterOverlay.bounds(group, width, height, false);
    }

    public static Bounds bounds(AbyssClientConfig.HudGroup group, int width, int height, boolean countdownPreview) {
        Font font = Minecraft.m_91087_().f_91062_;
        AbyssNetwork.EncounterStatus status = AbyssEncounterOverlay.preview(countdownPreview);
        return AbyssEncounterOverlay.layout(group, font, width, height, status, AbyssEncounterOverlay.challengeLayout(font, status));
    }

    public static void renderPreview(GuiGraphics graphics, int width, int height) {
        AbyssEncounterOverlay.renderPreview(graphics, width, height, false);
    }

    public static void renderPreview(GuiGraphics graphics, int width, int height, boolean countdownPreview) {
        AbyssEncounterOverlay.render(graphics, Minecraft.m_91087_().f_91062_, width, height, AbyssEncounterOverlay.preview(countdownPreview));
    }

    private static AbyssNetwork.EncounterStatus preview(boolean countdown) {
        return new AbyssNetwork.EncounterStatus(true, false, 6, 3, 12, 24, 185, countdown ? 5 : 0, List.of(new AbyssNetwork.ChallengeView((Component)Component.m_237110_((String)"challenge.spiral_abyss.time", (Object[])new Object[]{3}), 0, 0, true, false), new AbyssNetwork.ChallengeView((Component)Component.m_237110_((String)"challenge.spiral_abyss.projectile", (Object[])new Object[]{8}), 3, 8, false, false), new AbyssNetwork.ChallengeView((Component)Component.m_237110_((String)"challenge.spiral_abyss.take_damage", (Object[])new Object[]{5}), 2, 5, false, false)), List.of(new ResourceLocation("minecraft", "wither")));
    }

    private static void render(GuiGraphics graphics, Font font, int width, int height, AbyssNetwork.EncounterStatus status) {
        ChallengeLayout challengeLayout = AbyssEncounterOverlay.challengeLayout(font, status);
        if (!status.preparing()) {
            Bounds timer = AbyssEncounterOverlay.layout(AbyssClientConfig.HudGroup.TIMER, font, width, height, status, challengeLayout);
            AbyssEncounterOverlay.begin(graphics, timer);
            if (status.countdownSeconds() > 0) {
                graphics.m_280168_().m_252880_(33.0f, 0.0f, 0.0f);
                graphics.m_280168_().m_85841_(4.0f, 4.0f, 1.0f);
                graphics.m_280137_(font, Integer.toString(status.countdownSeconds()), 0, 0, -1);
            } else {
                AbyssClockRenderer.combat(graphics, font, 33, 0, status.seconds(), 1.5f);
            }
            graphics.m_280168_().m_85849_();
        }
        Bounds challenges = AbyssEncounterOverlay.layout(AbyssClientConfig.HudGroup.CHALLENGES, font, width, height, status, challengeLayout);
        AbyssEncounterOverlay.begin(graphics, challenges);
        AbyssEncounterOverlay.drawChallenges(graphics, font, status, challengeLayout);
        graphics.m_280168_().m_85849_();
        if (!status.preparing() && status.floor() != 7 && !status.rejectedBosses().isEmpty()) {
            Bounds rejection = AbyssEncounterOverlay.layout(AbyssClientConfig.HudGroup.REJECTION, font, width, height, status, challengeLayout);
            graphics.m_280168_().m_85836_();
            graphics.m_280168_().m_252880_((float)rejection.x(), (float)rejection.y(), 0.0f);
            graphics.m_280168_().m_85841_(rejection.scale() / 0.9f, rejection.scale() / 0.9f, 1.0f);
            AbyssRejectionNotice.panel(graphics, font, 0, 0, 300, Integer.MAX_VALUE, status.rejectedBosses());
            graphics.m_280168_().m_85849_();
        }
    }

    private static void begin(GuiGraphics graphics, Bounds bounds) {
        graphics.m_280168_().m_85836_();
        graphics.m_280168_().m_252880_((float)bounds.x(), (float)bounds.y(), 0.0f);
        graphics.m_280168_().m_85841_(bounds.scale(), bounds.scale(), 1.0f);
    }

    private static Bounds layout(AbyssClientConfig.HudGroup group, Font font, int screenWidth, int screenHeight, AbyssNetwork.EncounterStatus status, ChallengeLayout challengeLayout) {
        double defaultY;
        double defaultX;
        float baseScale;
        int naturalHeight;
        int naturalWidth;
        AbyssClientConfig.HudSettings settings = AbyssClientConfig.hud(group);
        if (group == AbyssClientConfig.HudGroup.TIMER) {
            int n;
            naturalWidth = 66;
            if (status.countdownSeconds() > 0) {
                Objects.requireNonNull(font);
                n = (int)(9.0f * 4.0f);
            } else {
                n = 22;
            }
            naturalHeight = n;
            baseScale = 1.0f;
            defaultX = ((double)screenWidth - (double)naturalWidth * (Double)settings.scale.get()) / 2.0;
            defaultY = status.countdownSeconds() > 0 ? 12.0 : 36.0;
        } else if (group == AbyssClientConfig.HudGroup.CHALLENGES) {
            naturalWidth = challengeLayout.width();
            naturalHeight = challengeLayout.height();
            baseScale = 0.882f;
            defaultX = 12.0;
            defaultY = Math.max(44, screenHeight / 4);
        } else {
            AbyssRejectionNotice.PanelLayout panel = AbyssRejectionNotice.layout(font, 300, status.rejectedBosses());
            naturalWidth = panel.width();
            naturalHeight = panel.height();
            baseScale = 0.7938f;
            Bounds challenges = AbyssEncounterOverlay.layout(AbyssClientConfig.HudGroup.CHALLENGES, font, screenWidth, screenHeight, status, challengeLayout);
            defaultX = (float)challenges.x() - 4.0f * challenges.scale();
            defaultY = (float)challenges.y() + (float)(challengeLayout.height() + 4) * challenges.scale();
        }
        float scale = (float)((double)baseScale * (Double)settings.scale.get());
        scale = Math.min(scale, Math.min((float)Math.max(1, screenWidth) / (float)naturalWidth, (float)Math.max(1, screenHeight) / (float)naturalHeight));
        int width = Math.min(screenWidth, Math.max(1, Mth.m_14167_((float)((float)naturalWidth * scale))));
        int height = Math.min(screenHeight, Math.max(1, Mth.m_14167_((float)((float)naturalHeight * scale))));
        int spaceX = Math.max(0, screenWidth - width);
        int spaceY = Math.max(0, screenHeight - height);
        double x = (Double)settings.x.get() < 0.0 ? defaultX : (Double)settings.x.get() * (double)spaceX;
        double y = (Double)settings.y.get() < 0.0 ? defaultY : (Double)settings.y.get() * (double)spaceY;
        return new Bounds((int)Math.round(Mth.m_14008_((double)x, (double)0.0, (double)spaceX)), (int)Math.round(Mth.m_14008_((double)y, (double)0.0, (double)spaceY)), width, height, scale);
    }

    private static Component floorWave(AbyssNetwork.EncounterStatus status) {
        return status.floor() == 7 ? Component.m_237110_((String)"hud.spiral_abyss.endless_wave", (Object[])new Object[]{status.wave()}) : Component.m_237110_((String)"hud.spiral_abyss.floor_wave", (Object[])new Object[]{status.floor(), status.wave()});
    }

    private static Component objective(AbyssNetwork.ChallengeView challenge) {
        return challenge.target() > 0 ? Component.m_237110_((String)"challenge.spiral_abyss.progress", (Object[])new Object[]{challenge.description(), challenge.progress(), challenge.target()}) : challenge.description();
    }

    private static ChallengeLayout challengeLayout(Font font, AbyssNetwork.EncounterStatus status) {
        int y;
        int width = font.m_92852_((FormattedText)AbyssEncounterOverlay.floorWave(status));
        if (!status.preparing()) {
            width = Math.max(width, font.m_92852_((FormattedText)Component.m_237110_((String)"hud.spiral_abyss.defeated", (Object[])new Object[]{status.defeated(), status.total()})));
        }
        if (status.preparing()) {
            v0 = 0;
        } else {
            Objects.requireNonNull(font);
            v0 = y = 9 + 6;
        }
        if (status.floor() == 7) {
            int n = Math.max(1, width);
            Objects.requireNonNull(font);
            return new ChallengeLayout(n, y + 9, List.of());
        }
        width = Math.max(width, font.m_92852_((FormattedText)Component.m_237115_((String)"screen.spiral_abyss.challenge")));
        Objects.requireNonNull(font);
        Objects.requireNonNull(font);
        y += 9 + 6 + 9 + 4;
        ArrayList<List<FormattedCharSequence>> rows = new ArrayList<List<FormattedCharSequence>>(status.challenges().size());
        for (AbyssNetwork.ChallengeView challenge : status.challenges()) {
            List lines = font.m_92923_((FormattedText)AbyssEncounterOverlay.objective(challenge), 240);
            rows.add(lines);
            for (FormattedCharSequence line : lines) {
                width = Math.max(width, 21 + font.m_92724_(line));
            }
            int n = lines.size();
            Objects.requireNonNull(font);
            y += Math.max(18, n * (9 + 2) + 4);
        }
        return new ChallengeLayout(Math.max(1, width), y, rows);
    }

    private static void drawChallenges(GuiGraphics graphics, Font font, AbyssNetwork.EncounterStatus status, ChallengeLayout layout) {
        int n;
        if (!status.preparing()) {
            graphics.m_280614_(font, (Component)Component.m_237110_((String)"hud.spiral_abyss.defeated", (Object[])new Object[]{status.defeated(), status.total()}), 0, 0, -1, true);
        }
        if (status.preparing()) {
            n = 0;
        } else {
            Objects.requireNonNull(font);
            n = 9 + 6;
        }
        int y = n;
        graphics.m_280614_(font, AbyssEncounterOverlay.floorWave(status), 0, y, -1, true);
        if (status.floor() == 7) {
            return;
        }
        Objects.requireNonNull(font);
        graphics.m_280614_(font, (Component)Component.m_237115_((String)"screen.spiral_abyss.challenge"), 0, y += 9 + 6, -1, true);
        Objects.requireNonNull(font);
        y += 9 + 4;
        for (int row = 0; row < status.challenges().size(); ++row) {
            AbyssNetwork.ChallengeView challenge = status.challenges().get(row);
            boolean highlighted = status.preparing() ? !challenge.failed() : challenge.satisfied() && !challenge.failed();
            graphics.m_280480_(STAR, 0, y - 3);
            if (!highlighted) {
                graphics.m_280509_(0, y - 3, 16, y + 13, -1722789808);
            }
            int start = y;
            for (FormattedCharSequence line : layout.rows().get(row)) {
                graphics.m_280649_(font, line, 21, y, highlighted ? -1 : -7829368, true);
                Objects.requireNonNull(font);
                y += 9 + 2;
            }
            y = Math.max(start + 18, y + 4);
        }
    }

    public record Bounds(int x, int y, int width, int height, float scale) {
        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= (double)this.x && mouseX < (double)(this.x + this.width) && mouseY >= (double)this.y && mouseY < (double)(this.y + this.height);
        }
    }

    private record ChallengeLayout(int width, int height, List<List<FormattedCharSequence>> rows) {
    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.Util
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.Tooltip
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.FormattedCharSequence
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.TooltipFlag
 *  net.minecraft.world.item.TooltipFlag$Default
 */
package com.pla.spiral_abyss.client.screen;

import com.pla.spiral_abyss.client.screen.AbyssHelpPanel;
import com.pla.spiral_abyss.client.screen.BannerArtwork;
import com.pla.spiral_abyss.client.screen.BannerBossRenderer;
import com.pla.spiral_abyss.client.screen.BannerRewardRenderer;
import com.pla.spiral_abyss.client.screen.JournalNavigation;
import com.pla.spiral_abyss.client.screen.JournalWorldBackdrop;
import com.pla.spiral_abyss.network.AbyssNetwork;
import java.lang.invoke.CallSite;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class AbyssJournalScreen
extends Screen {
    private final BannerArtwork artwork = new BannerArtwork();
    private final AbyssNetwork.OpenJournal state;
    private final BannerBossRenderer bosses;
    private final AbyssHelpPanel help = new AbyssHelpPanel();
    private final long openedAt = Util.m_137550_();
    private boolean helpPage;
    private int rewardPage;
    private int x;
    private int y;
    private int w;
    private int h;
    private static final int REWARDS_PER_VIEW = 3;
    private final BannerRewardRenderer rewardModels = new BannerRewardRenderer();
    private Button previous;
    private Button next;
    private final List<RewardGroup> rewardGroups = new ArrayList<RewardGroup>();
    private List<FormattedCharSequence> hoveredTooltip;

    public AbyssJournalScreen(AbyssNetwork.OpenJournal state) {
        super((Component)Component.m_237115_((String)"screen.spiral_abyss.floors"));
        this.state = state;
        this.bosses = new BannerBossRenderer(state == null ? 0L : state.poseSeed());
    }

    protected void m_7856_() {
        this.w = Math.min(900, Math.max(160, this.f_96543_ - 56));
        this.h = Math.min(this.w / 2, Math.max(100, this.f_96544_ - 96));
        this.w = Math.min(this.w, this.h * 2);
        this.x = (this.f_96543_ - this.w) / 2;
        this.y = 44 + (this.f_96544_ - 78 - this.h) / 2;
        JournalNavigation.add(x$0 -> {
            Button cfr_ignored_0 = (Button)this.m_142416_((GuiEventListener)x$0);
        }, this.f_96543_, this.helpPage ? 1 : 0, () -> this.changeTab(false), () -> this.changeTab(true), () -> this.f_96541_.m_91152_(null));
        this.previous = (Button)this.m_142416_((GuiEventListener)this.arrow(this.helpPage ? 2 : this.x - 23, this.y + this.h / 2 - 15, false));
        this.next = (Button)this.m_142416_((GuiEventListener)this.arrow(this.helpPage ? this.f_96543_ - 22 : this.x + this.w + 3, this.y + this.h / 2 - 15, true));
        this.rebuildRewards();
    }

    private void changeTab(boolean help) {
        this.helpPage = help;
        this.m_169413_();
        this.m_7856_();
    }

    private Button arrow(int left, int top, final boolean forward) {
        Button button = new Button(left, top, 20, 30, (Component)Component.m_237115_((String)(this.helpPage ? "screen.spiral_abyss.journal.banner" : "screen.spiral_abyss.journal.help")), b -> this.changeTab(!this.helpPage), n -> (MutableComponent)n.get()){

            protected void m_87963_(GuiGraphics g, int mx, int my, float tick) {
                int color = this.m_198029_() ? -8296 : -988707;
                for (int i = 0; i < 8; ++i) {
                    int dx = forward ? i : 7 - i;
                    g.m_280509_(this.m_252754_() + 5 + dx, this.m_252907_() + 7 + i, this.m_252754_() + 6 + dx, this.m_252907_() + 23 - i, color);
                }
            }
        };
        button.m_257544_(Tooltip.m_257550_((Component)button.m_6035_()));
        return button;
    }

    private List<AbyssNetwork.BannerBoss> featured() {
        if (this.state == null || this.state.bosses().isEmpty()) {
            return List.of();
        }
        ArrayList result = new ArrayList();
        this.state.bosses().stream().filter(b -> b.floor() == 5 && b.wave() == 3).findFirst().ifPresent(result::add);
        List<AbyssNetwork.BannerBoss> finals = this.state.bosses().stream().filter(b -> b.floor() == 6 && b.wave() == 3).toList();
        if (finals.size() < 2) {
            this.state.bosses().stream().filter(b -> b.floor() == 6 && b.wave() == 2).findFirst().ifPresent(result::add);
        }
        result.addAll(finals.stream().limit(2L).toList());
        LinkedHashMap<ResourceLocation, AbyssNetwork.BannerBoss> unique = new LinkedHashMap<ResourceLocation, AbyssNetwork.BannerBoss>();
        for (AbyssNetwork.BannerBoss boss : result) {
            unique.remove(boss.id());
            unique.put(boss.id(), boss);
        }
        return List.copyOf(unique.values());
    }

    private void rebuildRewards() {
        this.rewardGroups.clear();
        LinkedHashMap<ResourceLocation, RewardGroup> unique = new LinkedHashMap<ResourceLocation, RewardGroup>();
        HashSet<ResourceLocation> displayed = new HashSet<ResourceLocation>();
        for (AbyssNetwork.BannerBoss bannerBoss : this.featured()) {
            displayed.add(bannerBoss.id());
        }
        for (AbyssNetwork.BannerBoss bannerBoss : this.state == null ? List.of() : this.state.bosses()) {
            for (AbyssNetwork.RewardPreview reward : bannerBoss.rewards()) {
                if (!displayed.contains(bannerBoss.id()) || !reward.rare() || reward.table() || reward.icon().m_41619_()) continue;
                RewardGroup group = unique.computeIfAbsent(reward.id(), id -> new RewardGroup(reward.icon().m_41777_(), new ArrayList<RewardSource>()));
                group.sources().add(new RewardSource(bannerBoss, reward.icon().m_41777_()));
            }
        }
        this.rewardGroups.addAll(unique.values());
    }

    public void m_88315_(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.hoveredTooltip = null;
        if (this.helpPage) {
            JournalWorldBackdrop.render(g, this.f_96543_, this.f_96544_, 26, 42, this.f_96543_ - 52, this.f_96544_ - 52);
        } else {
            JournalWorldBackdrop.render(g, this.f_96543_, this.f_96544_, this.x, this.y, this.w, this.h);
        }
        if (this.helpPage) {
            this.help.render(g, 26, 42, this.f_96543_ - 52, this.f_96544_ - 52, mouseX, mouseY);
        } else {
            this.renderBanner(g, mouseX, mouseY);
        }
        super.m_88315_(g, mouseX, mouseY, partialTick);
        if (this.hoveredTooltip != null) {
            g.m_280245_(this.f_96547_, this.hoveredTooltip, mouseX, mouseY);
        }
    }

    private void renderBanner(GuiGraphics g, int mouseX, int mouseY) {
        long remaining;
        float unit = (float)this.w / 800.0f;
        this.artwork.render(g, this.x, this.y, this.w, this.h);
        int tx = this.x + Math.round((float)this.w * 0.035f);
        int tw = Math.round((float)this.w * 0.33f);
        long l = remaining = this.state == null || this.state.resetRemainingMillis() < 0L ? -1L : Math.max(0L, this.state.resetRemainingMillis() - (Util.m_137550_() - this.openedAt));
        Component countdown = remaining < 0L ? AbyssJournalScreen.tr("reset_disabled", new Object[0]) : (remaining < 86400000L ? AbyssJournalScreen.tr("journal.reset_today", new Object[0]) : AbyssJournalScreen.tr("journal.reset_days", (remaining + 86399999L) / 86400000L));
        this.text(g, AbyssJournalScreen.tr("journal.cycle", new Object[0]), tx, (float)this.y + (float)this.h * 0.06f, tw, -12429961, 1.0f * unit);
        this.wrapped(g, countdown, tx, (float)this.y + (float)this.h * 0.14f, tw, 2.8f * unit, -14271131);
        float rulesBottom = this.wrapped(g, AbyssJournalScreen.tr("journal.rules", new Object[0]), tx, (float)this.y + (float)this.h * 0.37f, tw, 1.03f * unit, -13349780);
        List<AbyssNetwork.BannerBoss> featured = this.featured();
        ArrayList<BossLabel> labels = new ArrayList<BossLabel>();
        g.m_280588_(this.x + 1, this.y + 1, this.x + this.w - 1, this.y + this.h - 1);
        if (featured.isEmpty()) {
            this.text(g, AbyssJournalScreen.tr("preview_unavailable", new Object[0]), (float)this.x + (float)this.w * 0.43f, (float)this.y + (float)this.h * 0.46f, (float)this.w * 0.52f, -1, unit);
        } else if (featured.size() == 1) {
            labels.add(this.renderBoss(g, featured.get(0), 0.4f, 0.08f, 0.54f, 0.8f));
        } else if (featured.size() == 2) {
            labels.add(this.renderBoss(g, featured.get(0), 0.55f, 0.1f, 0.34f, 0.34f));
            labels.add(this.renderBoss(g, featured.get(1), 0.36f, 0.38f, 0.6f, 0.55f));
        } else {
            labels.add(this.renderBoss(g, featured.get(0), 0.68f, 0.09f, 0.28f, 0.39f));
            labels.add(this.renderBoss(g, featured.get(1), 0.68f, 0.49f, 0.28f, 0.39f));
            labels.add(this.renderBoss(g, featured.get(2), 0.4f, 0.12f, 0.34f, 0.71f));
        }
        for (BossLabel label : labels) {
            BannerBossRenderer.Bounds bounds = label.bounds();
            MutableComponent name = this.bosses.name(label.boss().id()).m_6881_().m_130940_(ChatFormatting.BOLD);
            float scale = Math.min(Math.max(0.55f, label.nameScale() * unit), label.nameWidth() / (float)Math.max(1, this.f_96547_.m_92852_((FormattedText)name)));
            float labelWidth = (float)this.f_96547_.m_92852_((FormattedText)name) * scale;
            float labelX = (float)bounds.x() + ((float)bounds.width() - labelWidth) / 2.0f;
            float labelY = (float)bounds.y() + (float)bounds.height() * 0.88f;
            this.text(g, (Component)name, labelX, labelY, labelWidth + 1.0f, -1, scale);
            if (mouseX < bounds.x() || mouseX >= bounds.x() + bounds.width() || mouseY < bounds.y() || mouseY >= bounds.y() + bounds.height()) continue;
            this.hoveredTooltip = List.of(this.bosses.name(label.boss().id()).m_7532_());
        }
        this.renderRewards(g, mouseX, mouseY, unit, rulesBottom);
        Component bossFooter = AbyssJournalScreen.tr("journal.featured", new Object[0]);
        float footerScale = Math.max(0.55f, 1.0f * unit);
        float footerWidth = Math.min((float)this.w * 0.54f, (float)this.f_96547_.m_92852_((FormattedText)bossFooter) * footerScale);
        this.text(g, bossFooter, (float)this.x + (float)this.w * 0.965f - footerWidth, (float)this.y + (float)this.h * 0.9f, footerWidth, -1708557, footerScale);
        g.m_280618_();
    }

    private BossLabel renderBoss(GuiGraphics g, AbyssNetwork.BannerBoss boss, float bx, float by, float bw, float bh) {
        int left = this.x + Math.round((float)this.w * bx);
        int top = this.y + Math.round((float)this.h * by);
        int modelWidth = Math.round((float)this.w * bw);
        int modelHeight = Math.round((float)this.h * bh);
        this.bosses.render(g, boss.id(), boss.animation(), left, top, modelWidth, modelHeight);
        return new BossLabel(boss, this.bosses.contentBounds(boss.id(), left, top, modelWidth, modelHeight), modelWidth, bw < 0.3f ? 1.15f : 1.6f);
    }

    private void renderRewards(GuiGraphics g, int mx, int my, float unit, float rulesBottom) {
        int pages = Math.max(1, (this.rewardGroups.size() + 3 - 1) / 3);
        this.rewardPage = Math.min(this.rewardPage, pages - 1);
        int count = Math.min(3, this.rewardGroups.size() - this.rewardPage * 3);
        float areaLeft = (float)this.x + (float)this.w * 0.04f;
        float areaWidth = (float)this.w * 0.285f;
        float headingY = Math.max((float)this.y + (float)this.h * 0.575f, rulesBottom + 5.0f * unit);
        Objects.requireNonNull(this.f_96547_);
        float top = headingY + 9.0f * Math.max(0.55f, 1.15f * unit) + 5.0f * unit;
        float modelHeight = 0.7f * Math.max(10.0f, (float)this.y + (float)this.h * 0.845f - top);
        top = Math.max(top, (float)this.y + (float)this.h * 0.69f);
        float gap = Math.max(1.0f, 6.0f * unit);
        int maxWidth = Math.max(1, (int)(0.7f * (areaWidth - gap * (float)Math.max(0, count - 1)) / (float)Math.max(1, count)));
        int[] itemWidths = new int[count];
        float groupWidth = gap * (float)Math.max(0, count - 1);
        for (int i = 0; i < count; ++i) {
            RewardGroup reward = this.rewardGroups.get(this.rewardPage * 3 + i);
            float tilt = ((float)i - (float)(count - 1) / 2.0f) * -10.0f;
            itemWidths[i] = this.rewardModels.fittedWidth(g, reward.icon(), maxWidth, Math.round(modelHeight), tilt);
            groupWidth += (float)itemWidths[i];
        }
        float cursor = areaLeft + (areaWidth - groupWidth) / 2.0f;
        for (int i = 0; i < count; ++i) {
            RewardGroup reward = this.rewardGroups.get(this.rewardPage * 3 + i);
            float left = cursor;
            cursor += (float)itemWidths[i] + gap;
            this.rewardModels.render(g, reward.icon(), Math.round(left), Math.round(top), itemWidths[i], Math.round(modelHeight), ((float)i - (float)(count - 1) / 2.0f) * -10.0f);
            if (!((float)mx >= left) || !((float)mx < left + (float)itemWidths[i]) || !((float)my >= top) || !((float)my < top + modelHeight)) continue;
            ArrayList<Object> lines = new ArrayList<Object>();
            lines.add(reward.icon().m_41786_());
            HashSet<CallSite> seen = new HashSet<CallSite>();
            for (RewardSource source : reward.sources()) {
                String key = String.valueOf(source.boss().id()) + ":" + String.valueOf(source.stack().m_41783_());
                if (!seen.add((CallSite)((Object)key))) continue;
                lines.add(this.bosses.name(source.boss().id()).m_6881_().m_130940_(ChatFormatting.GOLD));
                List details = source.stack().m_41651_((Player)this.f_96541_.f_91074_, (TooltipFlag)TooltipFlag.Default.f_256752_);
                for (int j = 1; j < details.size(); ++j) {
                    lines.add((Component)details.get(j));
                }
            }
            if (pages > 1) {
                lines.add(AbyssJournalScreen.tr("journal.scroll_rewards", this.rewardPage + 1, pages));
            }
            this.hoveredTooltip = lines.stream().flatMap(c -> this.f_96547_.m_92923_((FormattedText)c, Math.min(260, this.f_96543_ - 24)).stream()).toList();
        }
        int left = Math.round(areaLeft);
        int available = Math.round(areaWidth);
        this.wrapped(g, (Component)AbyssJournalScreen.tr(count == 0 ? "no_rewards" : "journal.rare", new Object[0]).m_6881_().m_130940_(ChatFormatting.BOLD), (float)this.x + (float)this.w * 0.035f, (float)this.y + (float)this.h * 0.9f, available, 1.0f * unit, -13349780);
        if (pages > 1) {
            this.text(g, AbyssJournalScreen.tr("journal.scroll_rewards", this.rewardPage + 1, pages), left, (float)this.y + (float)this.h * 0.855f, available, -12429961, 0.65f * unit);
        }
    }

    private static Component tr(String key, Object ... args) {
        return Component.m_237110_((String)("screen.spiral_abyss." + key), (Object[])args);
    }

    private void text(GuiGraphics g, Component text, float left, float top, float available, int color, float desiredScale) {
        float scale = Math.min(Math.max(0.55f, desiredScale), available / (float)Math.max(1, this.f_96547_.m_92852_((FormattedText)text)));
        g.m_280168_().m_85836_();
        g.m_280168_().m_252880_(left, top, 0.0f);
        g.m_280168_().m_85841_(scale, scale, 1.0f);
        g.m_280614_(this.f_96547_, text, 0, 0, color, (color & 0xFF0000) > 0x800000);
        g.m_280168_().m_85849_();
    }

    private float wrapped(GuiGraphics g, Component text, float left, float top, float available, float scale, int color) {
        scale = Math.max(0.55f, scale);
        g.m_280168_().m_85836_();
        g.m_280168_().m_252880_(left, top, 0.0f);
        g.m_280168_().m_85841_(scale, scale, 1.0f);
        int row = 0;
        for (FormattedCharSequence line : this.f_96547_.m_92923_((FormattedText)text, Math.max(1, (int)(available / scale)))) {
            g.m_280649_(this.f_96547_, line, 0, row, color, (color & 0xFF0000) > 0x800000);
            Objects.requireNonNull(this.f_96547_);
            row += 9 + 2;
        }
        g.m_280168_().m_85849_();
        return top + (float)row * scale;
    }

    public boolean m_6050_(double mx, double my, double amount) {
        if (!this.helpPage && mx >= (double)this.x + (double)this.w * 0.035 && mx < (double)this.x + (double)this.w * 0.335 && my >= (double)this.y + (double)this.h * 0.57 && my < (double)this.y + (double)this.h * 0.89) {
            int pages = Math.max(1, (this.rewardGroups.size() + 3 - 1) / 3);
            this.rewardPage = Math.floorMod(this.rewardPage + (amount < 0.0 ? 1 : -1), pages);
            return true;
        }
        return super.m_6050_(mx, my, amount);
    }

    public boolean m_7933_(int key, int scan, int modifiers) {
        if (key == 263 || key == 262) {
            this.changeTab(!this.helpPage);
            return true;
        }
        return super.m_7933_(key, scan, modifiers);
    }

    public void m_7861_() {
        this.bosses.clear();
        this.rewardModels.clear();
        this.artwork.close();
    }

    public boolean m_7043_() {
        return false;
    }

    private record RewardGroup(ItemStack icon, List<RewardSource> sources) {
    }

    private record RewardSource(AbyssNetwork.BannerBoss boss, ItemStack stack) {
    }

    private record BossLabel(AbyssNetwork.BannerBoss boss, BannerBossRenderer.Bounds bounds, float nameWidth, float nameScale) {
    }
}


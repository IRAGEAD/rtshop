/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.network.chat.CommonComponents
 *  net.minecraft.network.chat.Component
 *  net.minecraft.util.Mth
 */
package com.pla.spiral_abyss.client.screen;

import com.pla.spiral_abyss.client.overlay.AbyssEncounterOverlay;
import com.pla.spiral_abyss.config.AbyssClientConfig;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class AbyssHudConfigScreen
extends Screen {
    private static final int HANDLE_RADIUS = 6;
    private final Screen parent;
    private AbyssClientConfig.HudGroup selected = AbyssClientConfig.HudGroup.TIMER;
    private Button groupButton;
    private Button countdownButton;
    private boolean countdownPreview;
    private boolean moving;
    private boolean scaling;
    private boolean pendingSave;
    private double grabX;
    private double grabY;
    private double initialScale;
    private double resizeX;
    private double resizeY;
    private int initialWidth;
    private int initialHeight;
    private int resizeLeft;
    private int resizeTop;
    private int footerY;

    public AbyssHudConfigScreen(Screen parent) {
        super((Component)Component.m_237115_((String)"screen.spiral_abyss.config.hud"));
        this.parent = parent;
    }

    protected void m_7856_() {
        this.finishDrag();
        this.footerY = this.f_96544_ - 26;
        int left = this.f_96543_ / 2 - 150;
        this.groupButton = (Button)this.m_142416_((GuiEventListener)Button.m_253074_((Component)this.groupName(), button -> {
            this.selected = AbyssClientConfig.HudGroup.values()[(this.selected.ordinal() + 1) % AbyssClientConfig.HudGroup.values().length];
            this.groupButton.m_93666_(this.groupName());
        }).m_252987_(left, this.footerY, 134, 20).m_253136_());
        this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237115_((String)"screen.spiral_abyss.config.hud_reset"), button -> {
            AbyssClientConfig.hud(this.selected).reset();
            this.pendingSave = true;
            this.saveChanges();
        }).m_252987_(left + 138, this.footerY, 100, 20).m_253136_());
        this.m_142416_((GuiEventListener)Button.m_253074_((Component)CommonComponents.f_130655_, button -> this.m_7379_()).m_252987_(left + 242, this.footerY, 58, 20).m_253136_());
        this.countdownButton = (Button)this.m_142416_((GuiEventListener)Button.m_253074_((Component)this.countdownLabel(), button -> {
            this.countdownPreview = !this.countdownPreview;
            this.countdownButton.m_93666_(this.countdownLabel());
        }).m_252987_(left, this.footerY - 24, 160, 20).m_253136_());
    }

    private Component groupName() {
        return Component.m_237115_((String)("screen.spiral_abyss.config.hud_" + this.selected.name().toLowerCase(Locale.ROOT)));
    }

    private Component countdownLabel() {
        return Component.m_237115_((String)(this.countdownPreview ? "screen.spiral_abyss.config.preview_countdown" : "screen.spiral_abyss.config.preview_timer"));
    }

    private AbyssEncounterOverlay.Bounds bounds(AbyssClientConfig.HudGroup group) {
        return AbyssEncounterOverlay.bounds(group, this.f_96543_, this.f_96544_, this.countdownPreview);
    }

    private static boolean contains(AbyssEncounterOverlay.Bounds bounds, double x, double y) {
        return x >= (double)bounds.x() && x < (double)(bounds.x() + bounds.width()) && y >= (double)bounds.y() && y < (double)(bounds.y() + bounds.height());
    }

    private static int handleX(AbyssEncounterOverlay.Bounds bounds) {
        return bounds.x() + bounds.width() - 6;
    }

    private static int handleY(AbyssEncounterOverlay.Bounds bounds) {
        return bounds.y() + bounds.height() - 6;
    }

    private static boolean onHandle(AbyssEncounterOverlay.Bounds bounds, double x, double y) {
        double dy;
        double dx = x - (double)AbyssHudConfigScreen.handleX(bounds);
        return dx * dx + (dy = y - (double)AbyssHudConfigScreen.handleY(bounds)) * dy <= 64.0;
    }

    public boolean m_6375_(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.m_6375_(mouseX, mouseY, button);
        }
        AbyssEncounterOverlay.Bounds current = this.bounds(this.selected);
        if (AbyssHudConfigScreen.onHandle(current, mouseX, mouseY)) {
            this.scaling = true;
            this.initialScale = (Double)AbyssClientConfig.hud((AbyssClientConfig.HudGroup)this.selected).scale.get();
            this.initialWidth = current.width();
            this.initialHeight = current.height();
            this.resizeLeft = current.x();
            this.resizeTop = current.y();
            this.resizeX = mouseX;
            this.resizeY = mouseY;
            this.place(this.resizeLeft, this.resizeTop, current);
        } else {
            AbyssClientConfig.HudGroup hit;
            if (mouseY >= (double)(this.footerY - 27)) {
                return super.m_6375_(mouseX, mouseY, button);
            }
            AbyssClientConfig.HudGroup hudGroup = hit = AbyssHudConfigScreen.contains(current, mouseX, mouseY) ? this.selected : null;
            if (hit == null) {
                for (AbyssClientConfig.HudGroup group : AbyssClientConfig.HudGroup.values()) {
                    if (!AbyssHudConfigScreen.contains(this.bounds(group), mouseX, mouseY)) continue;
                    hit = group;
                }
            }
            if (hit == null) {
                return super.m_6375_(mouseX, mouseY, button);
            }
            this.selected = hit;
            this.groupButton.m_93666_(this.groupName());
            current = this.bounds(this.selected);
            this.moving = true;
            this.grabX = mouseX - (double)current.x();
            this.grabY = mouseY - (double)current.y();
        }
        this.m_7522_(null);
        this.m_7897_(true);
        return true;
    }

    private void place(double x, double y, AbyssEncounterOverlay.Bounds bounds) {
        AbyssClientConfig.HudSettings setting = AbyssClientConfig.hud(this.selected);
        double normalizedX = Mth.m_14008_((double)(x / (double)Math.max(1, this.f_96543_ - bounds.width())), (double)0.0, (double)1.0);
        double normalizedY = Mth.m_14008_((double)(y / (double)Math.max(1, this.f_96544_ - bounds.height())), (double)0.0, (double)1.0);
        if ((Double)setting.x.get() != normalizedX || (Double)setting.y.get() != normalizedY) {
            setting.x.set((Object)normalizedX);
            setting.y.set((Object)normalizedY);
            this.pendingSave = true;
        }
    }

    public boolean m_7979_(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && this.moving) {
            this.place(mouseX - this.grabX, mouseY - this.grabY, this.bounds(this.selected));
            return true;
        }
        if (button == 0 && this.scaling) {
            double diagonal = (double)this.initialWidth * (double)this.initialWidth + (double)this.initialHeight * (double)this.initialHeight;
            double ratio = 1.0 + ((mouseX - this.resizeX) * (double)this.initialWidth + (mouseY - this.resizeY) * (double)this.initialHeight) / Math.max(1.0, diagonal);
            double scale = Mth.m_14008_((double)(this.initialScale * ratio), (double)0.5, (double)2.5);
            AbyssClientConfig.hud((AbyssClientConfig.HudGroup)this.selected).scale.set((Object)scale);
            this.pendingSave = true;
            this.place(this.resizeLeft, this.resizeTop, this.bounds(this.selected));
            return true;
        }
        return super.m_7979_(mouseX, mouseY, button, dragX, dragY);
    }

    public boolean m_6348_(double mouseX, double mouseY, int button) {
        if (button == 0 && (this.moving || this.scaling)) {
            this.finishDrag();
            return true;
        }
        return super.m_6348_(mouseX, mouseY, button);
    }

    private void saveChanges() {
        if (!this.pendingSave) {
            return;
        }
        AbyssClientConfig.SPEC.save();
        this.pendingSave = false;
    }

    private void finishDrag() {
        this.saveChanges();
        this.scaling = false;
        this.moving = false;
        this.m_7897_(false);
    }

    public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.m_280273_(graphics);
        AbyssEncounterOverlay.renderPreview(graphics, this.f_96543_, this.f_96544_, this.countdownPreview);
        graphics.m_280168_().m_85836_();
        graphics.m_280168_().m_252880_(0.0f, 0.0f, 400.0f);
        for (AbyssClientConfig.HudGroup group : AbyssClientConfig.HudGroup.values()) {
            if (group == this.selected) continue;
            AbyssHudConfigScreen.outline(graphics, this.bounds(group), -8947832);
        }
        AbyssEncounterOverlay.Bounds current = this.bounds(this.selected);
        AbyssHudConfigScreen.outline(graphics, current, -13210);
        graphics.m_280509_(0, 0, this.f_96543_, 34, -804253669);
        graphics.m_280653_(this.f_96547_, this.f_96539_, this.f_96543_ / 2, 5, 0xFFFFFF);
        graphics.m_280653_(this.f_96547_, (Component)Component.m_237115_((String)"screen.spiral_abyss.config.hud_drag"), this.f_96543_ / 2, 19, 0xBBBBCC);
        graphics.m_280509_(0, this.footerY - 28, this.f_96543_, this.f_96544_, -535818213);
        graphics.m_280430_(this.f_96547_, (Component)Component.m_237110_((String)"screen.spiral_abyss.config.hud_scale", (Object[])new Object[]{Math.round((Double)AbyssClientConfig.hud((AbyssClientConfig.HudGroup)this.selected).scale.get() * 100.0)}), this.f_96543_ / 2 + 16, this.footerY - 18, -13210);
        super.m_88315_(graphics, mouseX, mouseY, partialTick);
        graphics.m_280168_().m_252880_(0.0f, 0.0f, 100.0f);
        AbyssHudConfigScreen.drawHandle(graphics, AbyssHudConfigScreen.handleX(current), AbyssHudConfigScreen.handleY(current), AbyssHudConfigScreen.onHandle(current, mouseX, mouseY) || this.scaling);
        graphics.m_280168_().m_85849_();
    }

    private static void outline(GuiGraphics graphics, AbyssEncounterOverlay.Bounds bounds, int color) {
        graphics.m_280637_(bounds.x() - 1, bounds.y() - 1, bounds.width() + 2, bounds.height() + 2, color);
    }

    private static void drawHandle(GuiGraphics graphics, int x, int y, boolean hovered) {
        int color = hovered ? -1 : -13210;
        for (int row = -6; row <= 6; ++row) {
            int halfWidth = (int)Math.sqrt(36 - row * row);
            graphics.m_280509_(x - halfWidth, y + row, x + halfWidth + 1, y + row + 1, color);
        }
        graphics.m_280509_(x - 3, y, x + 4, y + 1, -14671827);
        graphics.m_280509_(x, y - 3, x + 1, y + 4, -14671827);
    }

    public void m_7379_() {
        this.finishDrag();
        this.f_96541_.m_91152_(this.parent);
    }

    public void m_7861_() {
        this.finishDrag();
    }

    public boolean m_7043_() {
        return false;
    }
}


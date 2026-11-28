/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.ChatFormatting
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.world.item.ItemStack
 */
package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.client.ClientNet;
import dev.dyslexic.cinematic.client.Picking;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.net.StationActionPayload;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

final class RecipeChoiceStrip {
    private static final int CELL = 20;
    private static final int PAD = 4;
    private static final int TITLE = 11;
    private final List<ItemStack> outputs = new ArrayList<ItemStack>();
    private int selected = -1;
    private int x;
    private int y;
    private int width;
    private int height;
    private boolean shown;

    RecipeChoiceStrip() {
    }

    void set(List<ItemStack> results, int inUse) {
        this.outputs.clear();
        this.outputs.addAll(results);
        this.selected = inUse;
    }

    void clear() {
        this.outputs.clear();
        this.selected = -1;
    }

    boolean active() {
        return this.outputs.size() >= 2;
    }

    int selected() {
        return this.selected;
    }

    void render(GuiGraphics g, Font font, float alpha) {
        boolean bl = this.shown = this.active() && alpha > 0.05f;
        if (!this.shown) {
            return;
        }
        Minecraft mc = Minecraft.m_91087_();
        int screenW = mc.m_91268_().m_85445_();
        int screenH = mc.m_91268_().m_85446_();
        MutableComponent title = Component.m_237115_((String)"cinematic.choices.title");
        this.width = Math.max(this.outputs.size() * 20 + 8, font.m_92852_((FormattedText)title) + 8);
        this.height = 39;
        this.x = (screenW - this.width) / 2;
        this.y = Math.min((int)((double)screenH * 0.85) - this.height, screenH - 34 - this.height);
        int a = Math.round(alpha * 255.0f);
        g.m_280509_(this.x, this.y, this.x + this.width, this.y + this.height, Math.round(alpha * 0.62f * 255.0f) << 24 | 0xC0C10);
        g.m_280509_(this.x, this.y + this.height - 1, this.x + this.width, this.y + this.height, Math.round(alpha * 0.22f * 255.0f) << 24 | 0xFFFFFF);
        g.m_280614_(font, (Component)title, this.x + (this.width - font.m_92852_((FormattedText)title)) / 2, this.y + 4 - 1, a << 24 | 0xD8D8D8, false);
        double[] mouse = Picking.mouseGui();
        int hover = this.at(mouse[0], mouse[1]);
        int left = this.x + (this.width - this.outputs.size() * 20) / 2;
        for (int i = 0; i < this.outputs.size(); ++i) {
            int cx = left + i * 20;
            int cy = this.y + 4 + 11;
            if (i == hover) {
                g.m_280509_(cx, cy, cx + 20, cy + 20, Math.round(alpha * 0.18f * 255.0f) << 24 | 0xFFFFFF);
            }
            if (i == this.selected) {
                int rim = a << 24 | 0x8CC4EE;
                g.m_280509_(cx, cy, cx + 20, cy + 1, rim);
                g.m_280509_(cx, cy + 20 - 1, cx + 20, cy + 20, rim);
                g.m_280509_(cx, cy, cx + 1, cy + 20, rim);
                g.m_280509_(cx + 20 - 1, cy, cx + 20, cy + 20, rim);
            }
            g.m_280480_(this.outputs.get(i), cx + 2, cy + 2);
            g.m_280370_(font, this.outputs.get(i), cx + 2, cy + 2);
        }
    }

    boolean over(double mx, double my) {
        return this.shown && mx >= (double)this.x && mx < (double)(this.x + this.width) && my >= (double)this.y && my < (double)(this.y + this.height);
    }

    int at(double mx, double my) {
        if (!this.shown) {
            return -1;
        }
        int left = this.x + (this.width - this.outputs.size() * 20) / 2;
        int top = this.y + 4 + 11;
        if (my < (double)top || my >= (double)(top + 20) || mx < (double)left) {
            return -1;
        }
        int i = (int)((mx - (double)left) / 20.0);
        return i < this.outputs.size() ? i : -1;
    }

    int click(double mx, double my) {
        if (!this.over(mx, my)) {
            return -2;
        }
        int i = this.at(mx, my);
        if (i < 0 || i == this.selected) {
            return -1;
        }
        this.selected = i;
        Scene.sound((SoundEvent)SoundEvents.f_12490_.m_203334_(), 0.25f, 1.4f);
        return i;
    }

    static void send(BlockPos pos, StationKind kind, int index) {
        ClientNet.sendToServer(new StationActionPayload(pos, kind, StationActionPayload.Action.CHOOSE, index, 0, false, 0));
    }

    @Nullable
    List<Component> tooltip(double mx, double my) {
        int i = this.at(mx, my);
        if (i < 0) {
            return null;
        }
        ArrayList<Component> lines = new ArrayList<Component>(Screen.m_280152_((Minecraft)Minecraft.m_91087_(), (ItemStack)this.outputs.get(i)));
        lines.add((Component)Component.m_237115_((String)(i == this.selected ? "cinematic.choices.in_use" : "cinematic.choices.pick")).m_130940_(i == this.selected ? ChatFormatting.AQUA : ChatFormatting.GRAY));
        return lines;
    }
}


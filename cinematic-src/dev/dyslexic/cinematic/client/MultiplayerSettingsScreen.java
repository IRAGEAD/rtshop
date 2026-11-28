/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.Tooltip
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.util.FormattedCharSequence
 */
package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.client.Settings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

final class MultiplayerSettingsScreen
extends Screen {
    private final Screen parent;

    MultiplayerSettingsScreen(Screen parent) {
        super((Component)Component.m_237115_((String)"cinematic.configuration.multiplayer"));
        this.parent = parent;
    }

    protected void m_7856_() {
        boolean inWorld = this.f_96541_.f_91073_ != null;
        Button mode = Button.m_253074_((Component)this.label(), button -> {
            Settings.CLIENT_ONLY_MODE.set((Object)((Boolean)Settings.CLIENT_ONLY_MODE.get() == false ? 1 : 0));
            Settings.SPEC.save();
            button.m_93666_(this.label());
        }).m_252987_(this.f_96543_ / 2 - 155, this.f_96544_ / 2 - 10, 310, 20).m_257505_(Tooltip.m_257550_((Component)Component.m_237115_((String)(inWorld ? "cinematic.settings.clientOnlyMode.locked" : "cinematic.configuration.clientOnlyMode.tooltip")))).m_253136_();
        mode.f_93623_ = !inWorld;
        this.m_142416_((GuiEventListener)mode);
        this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237115_((String)"gui.done"), button -> this.m_7379_()).m_252987_(this.f_96543_ / 2 - 60, this.f_96544_ - 30, 120, 20).m_253136_());
    }

    private Component label() {
        return Component.m_237115_((String)"cinematic.configuration.clientOnlyMode").m_130946_(": ").m_7220_((Component)Component.m_237115_((String)((Boolean)Settings.CLIENT_ONLY_MODE.get() != false ? "cinematic.settings.on" : "cinematic.settings.off")));
    }

    public void m_88315_(GuiGraphics graphics, int x, int y, float tick) {
        super.m_88315_(graphics, x, y, tick);
        graphics.m_280653_(this.f_96547_, this.f_96539_, this.f_96543_ / 2, 20, 0xFFFFFF);
        int line = 0;
        for (FormattedCharSequence text : this.f_96547_.m_92923_((FormattedText)Component.m_237115_((String)"cinematic.settings.multiplayer.note"), Math.min(this.f_96543_ - 40, 540))) {
            graphics.m_280364_(this.f_96547_, text, this.f_96543_ / 2, this.f_96544_ / 2 - 72 + line++ * 12, 11051670);
        }
    }

    public void m_7379_() {
        this.f_96541_.m_91152_(this.parent);
    }
}


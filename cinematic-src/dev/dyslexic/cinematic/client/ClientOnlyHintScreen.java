/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.MultiLineLabel
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.network.chat.Style
 */
package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.client.ServerListHint;
import dev.dyslexic.cinematic.client.Settings;
import java.util.Objects;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

final class ClientOnlyHintScreen
extends Screen {
    private final Screen list;
    private MultiLineLabel message = MultiLineLabel.f_94331_;
    private int top;

    ClientOnlyHintScreen(Screen list) {
        super((Component)Component.m_237119_().m_7220_((Component)Component.m_237113_((String)"Cinematic Interactions").m_130948_(Style.f_131099_.m_178520_(9225454))).m_7220_((Component)Component.m_237113_((String)" \u00b7 ").m_130948_(Style.f_131099_.m_178520_(0xA8A8A8))).m_7220_((Component)Component.m_237115_((String)"cinematic.serverHint.title")));
        this.list = list;
    }

    protected void m_7856_() {
        this.message = MultiLineLabel.m_94341_((Font)this.f_96547_, (FormattedText)Component.m_237115_((String)"cinematic.serverHint.message"), (int)Math.min(this.f_96543_ - 40, 300));
        MutableComponent enable = Component.m_237115_((String)"cinematic.serverHint.enable");
        MutableComponent carryOn = Component.m_237115_((String)"cinematic.serverHint.continue");
        int buttonWidth = Math.max(150, Math.max(this.f_96547_.m_92852_((FormattedText)enable), this.f_96547_.m_92852_((FormattedText)carryOn)) + 20);
        boolean sideBySide = buttonWidth * 2 + 8 <= this.f_96543_ - 20;
        Objects.requireNonNull(this.f_96547_);
        int n = this.message.m_5770_();
        Objects.requireNonNull(this.f_96547_);
        int block = 9 + 14 + n * 9 + 20 + (sideBySide ? 20 : 44);
        this.top = Math.max(10, (this.f_96544_ - block) / 2);
        Objects.requireNonNull(this.f_96547_);
        int n2 = this.message.m_5770_();
        Objects.requireNonNull(this.f_96547_);
        int buttonsTop = this.top + 9 + 14 + n2 * 9 + 20;
        Button on = Button.m_253074_((Component)enable, button -> {
            Settings.CLIENT_ONLY_MODE.set((Object)true);
            ServerListHint.seen();
            this.f_96541_.m_91152_(this.list);
        }).m_252987_(sideBySide ? this.f_96543_ / 2 - buttonWidth - 4 : (this.f_96543_ - buttonWidth) / 2, buttonsTop, buttonWidth, 20).m_253136_();
        Button off = Button.m_253074_((Component)carryOn, button -> this.m_7379_()).m_252987_(sideBySide ? this.f_96543_ / 2 + 4 : (this.f_96543_ - buttonWidth) / 2, buttonsTop + (sideBySide ? 0 : 24), buttonWidth, 20).m_253136_();
        this.m_142416_((GuiEventListener)on);
        this.m_142416_((GuiEventListener)off);
    }

    public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float tick) {
        super.m_88315_(graphics, mouseX, mouseY, tick);
        graphics.m_280653_(this.f_96547_, this.f_96539_, this.f_96543_ / 2, this.top, 0xFFFFFF);
        int n = this.f_96543_ / 2;
        Objects.requireNonNull(this.f_96547_);
        Objects.requireNonNull(this.f_96547_);
        this.message.m_6514_(graphics, n, this.top + 9 + 14, 9, 0xD8D8D8);
    }

    public void m_7379_() {
        ServerListHint.seen();
        this.f_96541_.m_91152_(this.list);
    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.components.PlainTextButton
 *  net.minecraft.client.gui.components.Tooltip
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.network.chat.Style
 *  net.minecraftforge.client.event.ScreenEvent$Init$Post
 */
package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.client.ClientOnlyHintScreen;
import dev.dyslexic.cinematic.client.MultiplayerSettingsScreen;
import dev.dyslexic.cinematic.client.Settings;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraftforge.client.event.ScreenEvent;

public final class ServerListHint {
    static final int ACCENT = 9225454;
    private static final int MUTED = 0xA8A8A8;

    private ServerListHint() {
    }

    static void onScreenInit(ScreenEvent.Init.Post event) {
        Minecraft mc = Minecraft.m_91087_();
        Screen screen = event.getScreen();
        if (!(screen instanceof JoinMultiplayerScreen)) {
            return;
        }
        JoinMultiplayerScreen list = (JoinMultiplayerScreen)screen;
        MutableComponent text = Component.m_237119_().m_7220_((Component)Component.m_237113_((String)"Cinematic").m_130948_(Style.f_131099_.m_178520_(9225454))).m_7220_((Component)Component.m_237113_((String)" \u00b7 ").m_130948_(Style.f_131099_.m_178520_(0xA8A8A8))).m_7220_((Component)Component.m_237110_((String)"cinematic.serverHint.state", (Object[])new Object[]{Component.m_237115_((String)(Settings.clientOnlyMode() ? "cinematic.settings.on" : "cinematic.settings.off"))}).m_130948_(Style.f_131099_.m_178520_(0xA8A8A8)));
        int width = mc.f_91062_.m_92852_((FormattedText)text);
        int x = list.f_96543_ - width - 8;
        int y = 20;
        if (x < list.f_96543_ / 2 + mc.f_91062_.m_92852_((FormattedText)list.m_96636_()) / 2 + 8) {
            x = (list.f_96543_ - width) / 2;
            y = 6;
        }
        Objects.requireNonNull(mc.f_91062_);
        PlainTextButton line = new PlainTextButton(x, y, width, 9, (Component)text, button -> mc.m_91152_((Screen)new MultiplayerSettingsScreen((Screen)list)), mc.f_91062_);
        line.m_257544_(Tooltip.m_257550_((Component)Component.m_237115_((String)"cinematic.serverHint.state.tooltip")));
        event.addListener((GuiEventListener)line);
        if (!Settings.clientOnlyMode() && !((Boolean)Settings.SERVER_HINT_SEEN.get()).booleanValue()) {
            mc.m_6937_(() -> {
                if (mc.f_91080_ == list) {
                    mc.m_91152_((Screen)new ClientOnlyHintScreen((Screen)list));
                }
            });
        }
    }

    static void seen() {
        Settings.SERVER_HINT_SEEN.set((Object)true);
        Settings.SPEC.save();
    }
}


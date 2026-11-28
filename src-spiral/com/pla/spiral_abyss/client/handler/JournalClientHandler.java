/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.KeyMapping
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.components.AbstractWidget
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen
 *  net.minecraft.client.gui.screens.inventory.InventoryScreen
 */
package com.pla.spiral_abyss.client.handler;

import com.pla.spiral_abyss.client.platform.AbyssClientEvents;
import com.pla.spiral_abyss.client.platform.AbyssClientPlatform;
import com.pla.spiral_abyss.client.screen.AbyssJournalScreen;
import com.pla.spiral_abyss.client.screen.FloorSelectionScreen;
import com.pla.spiral_abyss.client.screen.JournalNavigation;
import com.pla.spiral_abyss.config.AbyssClientConfig;
import com.pla.spiral_abyss.network.AbyssNetwork;
import com.pla.spiral_abyss.world.AbyssDimension;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

public final class JournalClientHandler {
    private static final Map<Screen, Button> INVENTORY_BUTTONS = new WeakHashMap<Screen, Button>();
    public static final KeyMapping OPEN = AbyssClientPlatform.journalKey();

    private JournalClientHandler() {
    }

    public static void open(AbyssNetwork.OpenJournal message) {
        Minecraft client = Minecraft.m_91087_();
        if (client.f_91074_ != null && !client.f_91074_.m_9236_().m_46472_().equals(AbyssDimension.LEVEL)) {
            client.m_91152_((Screen)new AbyssJournalScreen(message));
        }
    }

    public static void inventory(AbyssClientEvents.ScreenInit event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof InventoryScreen) && !(screen instanceof CreativeModeInventoryScreen)) {
            return;
        }
        Button button = JournalNavigation.icon(0, 0, 22, 22, JournalNavigation.MENU_ICON, 256, "screen.spiral_abyss.floors", false, AbyssNetwork::requestJournal);
        JournalClientHandler.positionButton(screen, button);
        INVENTORY_BUTTONS.put(screen, button);
        event.addListener((AbstractWidget)button);
    }

    public static void renderInventory(AbyssClientEvents.ScreenEvent.Render.Pre event) {
        Button button = INVENTORY_BUTTONS.get(event.getScreen());
        if (button != null) {
            JournalClientHandler.positionButton(event.getScreen(), button);
        }
    }

    private static void positionButton(Screen screen, Button button) {
        button.m_252865_(Math.min((Integer)AbyssClientConfig.INVENTORY_BUTTON_X.get(), Math.max(0, screen.f_96543_ - button.m_5711_())));
        button.m_253211_(Math.min((Integer)AbyssClientConfig.INVENTORY_BUTTON_Y.get(), Math.max(0, screen.f_96544_ - button.m_93694_())));
    }

    public static void tick(AbyssClientEvents.TickEvent.ClientTickEvent event) {
        if (event.phase != AbyssClientEvents.TickEvent.Phase.END) {
            return;
        }
        Minecraft client = Minecraft.m_91087_();
        while (OPEN.m_90859_()) {
            if (client.f_91080_ != null || client.f_91074_ == null) continue;
            AbyssNetwork.requestJournal();
        }
    }

    public static void key(AbyssClientEvents.ScreenEvent.KeyPressed.Pre event) {
        if (!OPEN.m_90832_(event.getKeyCode(), event.getScanCode())) {
            return;
        }
        Screen screen = event.getScreen();
        if (screen instanceof AbyssJournalScreen || screen instanceof FloorSelectionScreen) {
            Minecraft.m_91087_().m_91152_(null);
            event.setCanceled(true);
        } else if (screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen) {
            AbyssNetwork.requestJournal();
            event.setCanceled(true);
        }
    }
}


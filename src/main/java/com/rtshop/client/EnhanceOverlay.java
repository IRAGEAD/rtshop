package com.rtshop.client;

import com.rtshop.RtShop;
import com.rtshop.enhance.EnhanceMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Cinematic Interactions 의 모루 장면(강화 창 대신 보이는 화면) 위에 강화 정보 패널을 늘 띄운다:
 * 마우스를 올리지 않아도 확률·재료·Gold 가 보이고, [확률표] 를 누르면 전체 확률 공개표가 뜬다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID, value = Dist.CLIENT)
public final class EnhanceOverlay {
    private EnhanceOverlay() {
    }

    public static final int W = 168, H = 166;
    private static boolean table;

    /** 모루 장면 중인가 (강화 창 메뉴는 열려 있는데 우리 강화 창이 아닌 다른 화면). */
    public static boolean active(Screen s) {
        Minecraft mc = Minecraft.getInstance();
        return s != null && mc.player != null && mc.player.containerMenu instanceof EnhanceMenu && !(s instanceof EnhanceScreen)
                && !(s instanceof EnhanceOddsScreen) && !(s instanceof EnhanceEditScreen);
    }

    private static int px(Screen s) {
        return s.width - W - 6;
    }

    private static int py(Screen s) {
        return Math.max(4, (s.height - H) / 2 - 10);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRender(ScreenEvent.Render.Post e) {
        Screen s = e.getScreen();
        if (!active(s)) {
            table = false;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        GuiGraphics g = e.getGuiGraphics();
        g.pose().pushPose();
        g.pose().translate(0, 0, 500);
        int x = px(s), y = py(s);
        EnhanceScreen.drawPanel(g, mc.font, (EnhanceMenu) mc.player.containerMenu, mc.player, x, y, W, H);
        boolean hov = Ui.inside(e.getMouseX(), e.getMouseY(), x, y + H + 3, W, 16);
        Ui.button(g, mc.font, x, y + H + 3, W, 16, Component.translatable(table ? "rtshop.enhodds.hide" : "rtshop.enhodds.open").getString(), hov, true);
        if (table) {
            EnhanceOddsScreen.drawTable(g, mc.font, (s.width - EnhanceOddsScreen.tableWidth()) / 2, Math.max(2, (s.height - EnhanceOddsScreen.tableHeight()) / 2));
        }
        g.pose().popPose();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClick(ScreenEvent.MouseButtonPressed.Pre e) {
        Screen s = e.getScreen();
        if (!active(s)) return;
        int x = px(s), y = py(s);
        if (table) {
            table = false;
            e.setCanceled(true);
            return;
        }
        if (Ui.inside(e.getMouseX(), e.getMouseY(), x, y + H + 3, W, 16)) {
            table = true;
            e.setCanceled(true);
        }
    }

    public static void showTableForTest(boolean on) {
        table = on;
    }
}

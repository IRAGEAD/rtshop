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

    public static final int W = EnhancePanel.W, H = EnhancePanel.H;
    private static boolean table;

    /** 모루 장면 중인가 (강화 창 메뉴는 열려 있는데 우리 강화 창이 아닌 다른 화면). */
    public static boolean active(Screen s) {
        Minecraft mc = Minecraft.getInstance();
        return s != null && mc.player != null && mc.player.containerMenu instanceof EnhanceMenu && !(s instanceof EnhanceScreen)
                && !(s instanceof EnhanceOddsScreen) && !(s instanceof EnhanceEditScreen) && !(s instanceof EnhanceUiScreen);
    }

    static int[] rect(Screen s) {
        return EnhancePanel.rect(s, s.width - W - 6, Math.max(4, (s.height - H) / 2 - 10));
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
        EnhancePanel.render(g, mc.font, (EnhanceMenu) mc.player.containerMenu, s, rect(s), e.getMouseX(), e.getMouseY(), table);
        if (table) {
            EnhanceOddsScreen.drawTable(g, mc.font, (s.width - EnhanceOddsScreen.tableWidth()) / 2, Math.max(2, (s.height - EnhanceOddsScreen.tableHeight()) / 2));
        }
        g.pose().popPose();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClick(ScreenEvent.MouseButtonPressed.Pre e) {
        Screen s = e.getScreen();
        if (!active(s)) return;
        if (table) {
            table = false;
            e.setCanceled(true);
            return;
        }
        int k = EnhancePanel.clicked(rect(s), e.getMouseX(), e.getMouseY());
        if (k == 0) {
            table = true;
            e.setCanceled(true);
        } else if (k > 0) {
            e.setCanceled(true);
            EnhancePanel.act(k, s);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onScroll(ScreenEvent.MouseScrolled.Pre e) {
        Screen s = e.getScreen();
        if (active(s) && EnhancePanel.scroll(rect(s), e.getMouseX(), e.getMouseY(), e.getScrollDelta())) {
            e.setCanceled(true);
        }
    }

    public static void showTableForTest(boolean on) {
        table = on;
    }
}

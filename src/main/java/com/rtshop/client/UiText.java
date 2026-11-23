package com.rtshop.client;

import com.rtshop.RtShop;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * RT Shop 창의 글자: 그림자 없이, 눈부신 흰색은 조금 낮춘 크림색으로 (GuiGraphicsTextMixin 이 이 값을 본다).
 * 창을 그리는 동안에만 켜지고, 연출(뽑기·강화·이야기)은 원래 모양대로 그린다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID, value = Dist.CLIENT)
public final class UiText {
    private UiText() {
    }

    /** 지금 RT Shop 창을 그리는 중인가 */
    public static boolean flat = false;
    /** 흰 글자 대신 쓸 색 */
    public static final int SOFT_WHITE = 0xE6DCC8;

    public static boolean ours(Object screen) {
        return screen != null && screen.getClass().getName().startsWith("com.rtshop.client.");
    }

    /** 흰색에 가까운 글자색을 부드러운 크림색으로 (투명도는 그대로). */
    public static int soften(int argb) {
        int r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
        if (r >= 0xF0 && g >= 0xF0 && b >= 0xE8) return argb & 0xFF000000 | SOFT_WHITE;
        return argb;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPre(ScreenEvent.Render.Pre e) {
        flat = ours(e.getScreen());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPost(ScreenEvent.Render.Post e) {
        flat = false;
    }

    /** 연출처럼 원래 모양(그림자 등)으로 그려야 하는 부분을 감싼다. */
    public static boolean pause() {
        boolean was = flat;
        flat = false;
        return was;
    }

    public static void resume(boolean was) {
        flat = was;
    }
}

package com.rtshop.client;

import com.rtshop.RtShop;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * 화면보다 긴 설명 창(툴팁) (3.78, 사용자 "일부 아이템의 툴팁 내용이 너무 많아서 잘려 보이는 문제"):
 * 바닐라는 설명 창이 화면보다 길면 위쪽(이름부터)이 화면 밖으로 잘린다. 그런 창은 화면에 맞게 줄여 그리고(최소 MIN_SCALE),
 * 그래도 넘치면 마우스 휠로 위아래를 넘겨 본다 (넘길 것이 있으면 오른쪽에 ▲ ▼). 화면 안에 들어오는 창은 건드리지 않는다.
 * 다른 모드가 그 창을 직접 그리려고 막았으면(취소) 손대지 않는다. client.json 의 fitLongTooltips 로 끈다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID, value = Dist.CLIENT)
public final class TooltipFit {
    private TooltipFit() {
    }

    /** 이보다 작게는 줄이지 않는다 (글자를 읽을 수 있게). 그 대신 넘겨 본다. */
    static final float MIN_SCALE = 0.75F;
    private static final int MARGIN = 4;

    private static boolean drawing;
    private static long lastTallMs;
    private static int scroll, scrollMax;
    private static Object lastKey;
    /** 점검용: 마지막으로 줄여 그린 창 {배율, 화면 위의 위쪽 y, 아래쪽 y, 넘길 수 있는 양}. */
    public static float lastScaleForTest;
    public static int lastTopForTest, lastBottomForTest, lastScrollMaxForTest, fittedForTest;

    /** 설명 창 크기 {너비, 높이} (바닐라와 같은 셈: 한 줄이면 -2, 첫 줄 아래 2칸). */
    static int[] size(Font font, List<ClientTooltipComponent> comps) {
        int w = 0, h = comps.size() == 1 ? -2 : 0;
        for (int i = 0; i < comps.size(); i++) {
            ClientTooltipComponent c = comps.get(i);
            w = Math.max(w, c.getWidth(font));
            h += c.getHeight() + (i == 0 && comps.size() > 1 ? 2 : 0);
        }
        return new int[]{w, h};
    }

    /** 이 높이의 창을 이 화면에 그릴 배율 (1 = 그대로). 시험에서도 쓴다. */
    static float scaleFor(int screenH, int h) {
        int room = screenH - 2 * MARGIN;
        if (h + 3 <= room) return 1F;
        return Math.max(MIN_SCALE, room / (float) (h + 3));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTooltip(RenderTooltipEvent.Pre e) {
        if (drawing) return;
        try {
            if (!ClientConfig.get().fitLongTooltips) return;
            List<ClientTooltipComponent> comps = e.getComponents();
            if (comps.isEmpty()) return;
            Font font = e.getFont();
            int[] wh = size(font, comps);
            int sw = e.getScreenWidth(), sh = e.getScreenHeight();
            float s = scaleFor(sh, wh[1]);
            if (s >= 1F) return;
            e.setCanceled(true);
            draw(e.getGraphics(), font, comps, e.getItemStack(), e.getX(), e.getY(), sw, sh, wh[0], wh[1], s);
        } catch (Throwable t) {
            RtShop.LOGGER.debug("[rtshop] 긴 설명 창을 줄이지 못했습니다: {}", t.toString());
        }
    }

    private static void draw(GuiGraphics g, Font font, List<ClientTooltipComponent> comps, ItemStack stack, int mx, int my,
                             int sw, int sh, int w, int h, float s) {
        // 같은 창을 계속 보고 있는가 (아니면 넘긴 자리를 처음으로)
        Object key = stack.isEmpty() ? comps.size() + ":" + h : stack;
        if (key != lastKey && !key.equals(lastKey)) {
            lastKey = key;
            scroll = 0;
        }
        lastTallMs = System.currentTimeMillis();
        // 줄인 좌표에서 자리 잡기 (바닐라의 마우스 옆 자리와 같은 규칙)
        float vw = sw / s, vh = sh / s;
        float x = mx / s + 12, y = my / s - 12;
        if (x + w > vw - MARGIN) x = Math.max(x - 24 - w, MARGIN);
        if (y + h + 3 > vh - MARGIN) y = vh - MARGIN - h - 3;
        scrollMax = 0;
        if (y < MARGIN) {
            // 줄여도 넘친다: 위쪽(이름)부터 보이게 놓고, 휠로 아래를 넘겨 본다
            scrollMax = (int) Math.ceil(MARGIN - y);
            scroll = Math.max(0, Math.min(scroll, scrollMax));
            y = Math.max(MARGIN - scroll, vh - MARGIN - h - 3);
        }
        int ix = Math.round(x), iy = Math.round(y);
        g.pose().pushPose();
        g.pose().scale(s, s, 1F);
        drawing = true;
        try {
            RenderTooltipEvent.Color c = net.minecraftforge.client.ForgeHooksClient.onRenderTooltipColor(stack, g, ix, iy, font, comps);
            int fx = ix, fy = iy;
            g.drawManaged(() -> TooltipRenderUtil.renderTooltipBackground(g, fx, fy, w, h, 400,
                    c.getBackgroundStart(), c.getBackgroundEnd(), c.getBorderStart(), c.getBorderEnd()));
            g.pose().translate(0.0F, 0.0F, 400.0F);
            int ty = iy;
            for (int i = 0; i < comps.size(); i++) {
                ClientTooltipComponent cc = comps.get(i);
                cc.renderText(font, ix, ty, g.pose().last().pose(), g.bufferSource());
                ty += cc.getHeight() + (i == 0 ? 2 : 0);
            }
            ty = iy;
            for (int i = 0; i < comps.size(); i++) {
                ClientTooltipComponent cc = comps.get(i);
                cc.renderImage(font, ix, ty, g);
                ty += cc.getHeight() + (i == 0 ? 2 : 0);
            }
            if (scrollMax > 0) {
                // 넘길 것이 남은 쪽에 화살표
                if (scroll < scrollMax) g.drawString(font, "▼", ix + w - 6, Math.min(iy + h - 7, Math.round(vh - MARGIN - 9)), 0xFFFFD866, true);
                if (scroll > 0) g.drawString(font, "▲", ix + w - 6, Math.max(iy, MARGIN), 0xFFFFD866, true);
            }
            g.flush();
        } finally {
            drawing = false;
            g.pose().popPose();
        }
        fittedForTest++;
        lastScaleForTest = s;
        lastTopForTest = Math.round(iy * s);
        lastBottomForTest = Math.round((iy + h + 3) * s);
        lastScrollMaxForTest = scrollMax;
    }

    /** 긴 설명 창이 떠 있는 동안 마우스 휠은 그 창을 넘긴다 (뒤의 화면은 넘기지 않음). */
    @SubscribeEvent
    public static void onScroll(ScreenEvent.MouseScrolled.Pre e) {
        if (scrollMax <= 0 || System.currentTimeMillis() - lastTallMs > 150) return;
        int step = 12;
        scroll = Math.max(0, Math.min(scrollMax, scroll - (int) Math.signum(e.getScrollDelta()) * step));
        e.setCanceled(true);
    }

    /** 시험용: 넘긴 자리를 처음으로. */
    static void resetForTest() {
        scroll = 0;
        scrollMax = 0;
        lastKey = null;
        fittedForTest = 0;
    }

    /** 시험용: 휠을 한 번 굴린 것처럼 (아래로 = 음수). */
    static void scrollForTest(double delta) {
        lastTallMs = System.currentTimeMillis();
        scroll = Math.max(0, Math.min(scrollMax, scroll - (int) Math.signum(delta) * 12));
    }

    static int scrollForTest() {
        return scroll;
    }
}

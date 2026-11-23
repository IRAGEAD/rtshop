package com.rtshop.client;

import com.rtshop.RtShop;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/**
 * RT Shop 알림 상자: 채팅 대신 화면 아래(핫바 위, 경고 문구 자리)에 잠시 보였다 사라진다.
 * <ul>
 * <li>창 글꼴(UiFont)로 그림자 없이, 어두운 가죽색 바탕 위에 그린다.</li>
 * <li>화면 폭에 맞춰 줄을 바꾸고(최대 3줄, 넘치면 …), 상자는 화면 밖으로 나가지 않는다.</li>
 * <li>한 번에 4개까지 쌓이고, 나머지는 차례를 기다린다. 같은 알림이 거듭 오면 ×2, ×3 으로 묶는다.</li>
 * <li>창을 열고 있으면 화면 맨 아래에, 연출(뽑기·스토리) 중에는 숨겼다가 끝나면 이어서 보인다.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID, value = Dist.CLIENT)
public final class NoticeHud {
    private NoticeHud() {
    }

    static final int MAX_VISIBLE = 4, MAX_LINES = 3, MAX_WIDTH = 340, PAD_X = 8, PAD_Y = 4, GAP = 3;
    static final long FADE_IN = 140, FADE_OUT = 380;

    static final class Entry {
        final Component text;
        final String plain;
        final long life;
        final boolean status;
        long age;
        int count = 1;

        Entry(Component text, boolean status) {
            this.text = text;
            this.status = status;
            this.plain = text.getString();
            this.life = Math.min(8000, 2600 + plain.length() * 55L);
        }
    }

    private static final Deque<Entry> ACTIVE = new ArrayDeque<>();
    private static final Deque<Entry> WAITING = new ArrayDeque<>();
    private static final List<String> HISTORY = new ArrayList<>();
    private static long lastFrame = -1;

    /** 점검용 */
    public static int activeForTest() {
        return ACTIVE.size() + WAITING.size();
    }

    public static List<String> historyForTest() {
        return HISTORY;
    }

    public static void clearForTest() {
        ACTIVE.clear();
        WAITING.clear();
    }

    /** 마지막 그리기에서 상자가 화면 안에 다 들어갔는가 (점검용). */
    public static boolean lastInsideForTest = true;
    public static int lastLinesForTest;

    public static void push(Component c) {
        push(c, false);
    }

    /** status = 예전 경고 문구(액션바) 자리의 알림: 진행도·경고처럼 자꾸 바뀌는 것은 한 칸을 바꿔 쓴다. */
    public static void push(Component c, boolean status) {
        if (c == null) return;
        String plain = c.getString();
        if (plain.isBlank()) return;
        synchronized (ACTIVE) {
            if (status) {
                for (Iterator<Entry> it = ACTIVE.iterator(); it.hasNext(); ) {
                    Entry e = it.next();
                    if (!e.status) continue;
                    if (e.plain.equals(plain)) {
                        e.age = Math.min(e.age, FADE_IN);
                        return;
                    }
                    it.remove();
                }
                WAITING.removeIf(e -> e.status);
                HISTORY.add(plain);
                if (HISTORY.size() > 100) HISTORY.remove(0);
                ACTIVE.add(new Entry(c, true));
                while (ACTIVE.size() > MAX_VISIBLE) {
                    Entry old = ACTIVE.pollFirst();
                    if (old != null) WAITING.addFirst(old);
                }
                return;
            }
            HISTORY.add(plain);
            if (HISTORY.size() > 100) HISTORY.remove(0);
            for (Entry e : ACTIVE) {
                if (e.plain.equals(plain)) {
                    e.count++;
                    e.age = Math.min(e.age, FADE_IN);
                    return;
                }
            }
            for (Entry e : WAITING) {
                if (e.plain.equals(plain)) {
                    e.count++;
                    return;
                }
            }
            WAITING.add(new Entry(c, false));
            promote();
        }
        RtShop.LOGGER.info("[RT Shop notice] {}", plain);
    }

    private static void promote() {
        while (ACTIVE.size() < MAX_VISIBLE && !WAITING.isEmpty()) ACTIVE.add(WAITING.poll());
        // 기다리는 알림이 많으면 가장 오래된 것을 빨리 내보낸다
        if (!WAITING.isEmpty()) {
            Entry oldest = ACTIVE.peekFirst();
            if (oldest != null && oldest.age < oldest.life - FADE_OUT - 600) oldest.age = Math.max(oldest.age, oldest.life - FADE_OUT - 600);
        }
    }

    private static void advance(boolean paused) {
        long now = Util.getMillis();
        long dt = lastFrame < 0 ? 0 : Math.max(0, Math.min(250, now - lastFrame));
        lastFrame = now;
        if (paused) return;
        synchronized (ACTIVE) {
            for (Iterator<Entry> it = ACTIVE.iterator(); it.hasNext(); ) {
                Entry e = it.next();
                e.age += dt;
                if (e.age >= e.life) it.remove();
            }
            promote();
        }
    }

    /** 줄 나누기: 폭에 맞춰 최대 3줄, 넘치면 마지막 줄 끝을 … 으로. */
    static List<FormattedCharSequence> lines(Font font, Component text, int width) {
        List<FormattedText> parts = font.getSplitter().splitLines(text, width, Style.EMPTY);
        List<FormattedCharSequence> out = new ArrayList<>();
        int n = Math.min(MAX_LINES, parts.size());
        for (int i = 0; i < n; i++) {
            FormattedText ft = parts.get(i);
            if (i == n - 1 && parts.size() > MAX_LINES) {
                Component dots = Component.literal("…");
                ft = FormattedText.composite(font.substrByWidth(ft, Math.max(10, width - font.width(dots))), dots);
            }
            out.add(Language.getInstance().getVisualOrder(ft));
        }
        return out;
    }

    /** 게임 화면 (창이 없을 때): 핫바·체력 줄 위. */
    public static void renderHud(GuiGraphics g, int sw, int sh) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return; // 창이 열려 있으면 창 위에 따로 그린다
        boolean hidden = ClientEvents.hideHud() || mc.options.hideGui;
        advance(hidden);
        if (hidden) return;
        draw(g, sw, sh, sh - 62);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onScreen(ScreenEvent.Render.Post e) {
        boolean hidden = ClientEvents.hideHud();
        advance(hidden);
        if (hidden || ACTIVE.isEmpty()) return;
        GuiGraphics g = e.getGuiGraphics();
        g.pose().pushPose();
        g.pose().translate(0, 0, 450);
        draw(g, e.getScreen().width, e.getScreen().height, e.getScreen().height - 8);
        g.pose().popPose();
    }

    private static void draw(GuiGraphics g, int sw, int sh, int bottom) {
        List<Entry> shown;
        synchronized (ACTIVE) {
            if (ACTIVE.isEmpty()) return;
            shown = new ArrayList<>(ACTIVE);
        }
        Font font = UiFont.get();
        int lh = font.lineHeight + 2;
        int maxW = Math.max(60, Math.min(MAX_WIDTH, sw - 24));
        int y = bottom;
        boolean inside = true;
        int totalLines = 0;
        boolean was = UiText.pause();
        try {
            // 새 알림이 아래, 오래된 알림이 위로 밀려 올라간다
            for (int i = shown.size() - 1; i >= 0; i--) {
                Entry e = shown.get(i);
                Component text = e.count > 1 ? e.text.copy().append(Component.literal("  ×" + e.count).withStyle(s -> s.withColor(0xC8B48C))) : e.text;
                List<FormattedCharSequence> ls = lines(font, text, maxW - PAD_X * 2);
                if (ls.isEmpty()) continue;
                totalLines += ls.size();
                int tw = 0;
                for (FormattedCharSequence l : ls) tw = Math.max(tw, font.width(l));
                int bw = Math.min(maxW, tw + PAD_X * 2);
                int bh = ls.size() * lh + PAD_Y * 2 - 2;
                int x0 = (sw - bw) / 2;
                int y0 = y - bh;
                if (y0 < 2) break; // 화면 위로 넘치면 더 쌓지 않는다
                if (x0 < 0 || x0 + bw > sw) inside = false;
                float a = alpha(e);
                if (a > 0.04f) {
                    int bg = ((int) (a * 0xD8) << 24) | 0x1C130B;
                    int edge = ((int) (a * 0x90) << 24) | 0xA87A22;
                    g.fill(x0 + 1, y0, x0 + bw - 1, y0 + bh, bg);
                    g.fill(x0, y0 + 1, x0 + 1, y0 + bh - 1, bg);
                    g.fill(x0 + bw - 1, y0 + 1, x0 + bw, y0 + bh - 1, bg);
                    g.fill(x0 + 2, y0, x0 + bw - 2, y0 + 1, edge);
                    g.fill(x0 + 2, y0 + bh - 1, x0 + bw - 2, y0 + bh, edge);
                    int ta = Math.max(8, (int) (a * 255));
                    int color = (ta << 24) | UiText.SOFT_WHITE;
                    int ty = y0 + PAD_Y;
                    for (FormattedCharSequence l : ls) {
                        int lw = font.width(l);
                        g.drawString(font, l, x0 + (bw - lw) / 2, ty, color, false);
                        ty += lh;
                    }
                }
                y = y0 - GAP;
            }
        } finally {
            UiText.resume(was);
        }
        lastInsideForTest = inside;
        lastLinesForTest = totalLines;
    }

    private static float alpha(Entry e) {
        if (e.age < FADE_IN) return Math.max(0f, e.age / (float) FADE_IN);
        long left = e.life - e.age;
        if (left < FADE_OUT) return Math.max(0f, left / (float) FADE_OUT);
        return 1f;
    }
}

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
 * RT Shop 알림: 채팅 대신 화면 위 가운데에 얇은 띠로 잠시 보였다 사라진다 (보스 체력 줄이 있으면 그 아래).
 * <ul>
 * <li>띠 하나에 알림 하나: 왼쪽의 작은 점이 알림의 빛깔(성공 초록·실패 빨강·그 밖은 금빛)을 알려 주고, 테두리도 같은 빛이다.</li>
 * <li>창 글꼴(UiFont)로 그림자 없이. 화면 폭에 맞춰 줄을 바꾸고(최대 2줄, 넘치면 …), 띠는 화면 밖으로 나가지 않는다.</li>
 * <li>오래된 알림이 위, 새 알림이 그 아래에 차례로 선다. 한 번에 3개까지, 나머지는 차례를 기다린다. 같은 알림이 거듭 오면 ×2, ×3 으로 묶는다.</li>
 * <li>창을 열고 있어도 같은 자리(맨 위)에 뜨고, 연출(뽑기·스토리) 중에는 숨겼다가 끝나면 이어서 보인다.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID, value = Dist.CLIENT)
public final class NoticeHud {
    private NoticeHud() {
    }

    static final int MAX_VISIBLE = 3, MAX_LINES = 2, MAX_WIDTH = 300, PAD_X = 8, PAD_Y = 3, GAP = 3, DOT = 9;
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
    /** 마지막 그리기에서 맨 위 띠의 y 와, 그린 띠의 수 (점검용). */
    public static int lastTopForTest = -1, lastCountForTest;

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

    /** 보스 체력 줄이 차지한 높이 (없으면 0): 알림 띠는 그 아래에서 시작한다. */
    private static int bossBarsHeight(Minecraft mc) {
        try {
            Object overlay = mc.gui.getBossOverlay();
            for (java.lang.reflect.Field f : overlay.getClass().getDeclaredFields()) {
                if (!java.util.Map.class.isAssignableFrom(f.getType())) continue;
                f.setAccessible(true);
                int n = ((java.util.Map<?, ?>) f.get(overlay)).size();
                return n <= 0 ? 0 : 12 + Math.min(n, 5) * 19;
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    /** 게임 화면 (창이 없을 때): 화면 위 가운데. */
    public static void renderHud(GuiGraphics g, int sw, int sh) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return; // 창이 열려 있으면 창 위에 따로 그린다
        boolean hidden = ClientEvents.hideHud() || mc.options.hideGui;
        advance(hidden);
        if (hidden) return;
        g.pose().pushPose();
        g.pose().translate(0, 0, 260);
        draw(g, sw, sh, Math.max(6, bossBarsHeight(mc) + 2), MAX_VISIBLE);
        g.pose().popPose();
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onScreen(ScreenEvent.Render.Post e) {
        boolean hidden = ClientEvents.hideHud();
        advance(hidden);
        if (hidden || ACTIVE.isEmpty()) return;
        GuiGraphics g = e.getGuiGraphics();
        g.pose().pushPose();
        g.pose().translate(0, 0, 450);
        draw(g, e.getScreen().width, e.getScreen().height, 4, 2); // 창이 열려 있으면 가장 새 알림 2개만
        g.pose().popPose();
    }

    /** 알림의 빛깔: 글에 입힌 색이 있으면 그 색(성공 초록·실패 빨강 등), 없으면 금빛. */
    static int tone(Component text) {
        net.minecraft.network.chat.TextColor c = text.getStyle().getColor();
        if (c == null) {
            for (Component sib : text.getSiblings()) {
                if (sib.getStyle().getColor() != null) {
                    c = sib.getStyle().getColor();
                    break;
                }
            }
        }
        int rgb = c == null ? 0xE8B84A : c.getValue() & 0xFFFFFF;
        int r = rgb >> 16 & 255, gr = rgb >> 8 & 255, b = rgb & 255;
        if (r > 232 && gr > 232 && b > 232) return 0xE8B84A; // 흰 글은 금빛 점
        if (r < 70 && gr < 70 && b < 70) return 0xE8B84A;     // 너무 어두운 색도
        return rgb;
    }

    private static int withAlpha(int rgb, float a, int max) {
        return ((int) (a * max) << 24) | (rgb & 0xFFFFFF);
    }

    /** 위에서 아래로 쌓는다: 오래된 알림이 위, 새 알림이 아래. top = 첫 띠의 y, maxCount = 보일 띠의 수 (가장 새 것부터 센다). */
    private static void draw(GuiGraphics g, int sw, int sh, int top, int maxCount) {
        List<Entry> shown;
        synchronized (ACTIVE) {
            if (ACTIVE.isEmpty()) return;
            shown = new ArrayList<>(ACTIVE);
        }
        Font font = UiFont.get();
        int lh = font.lineHeight + 1;
        // 가운데 기둥만 쓴다 (오른쪽·왼쪽 HUD 와 겹치지 않게): 화면 폭의 56%, 160~300
        int maxW = Math.max(Math.min(160, sw - 16), Math.min(MAX_WIDTH, (int) (sw * 0.56)));
        int y = top;
        boolean inside = true;
        int totalLines = 0, drawn = 0;
        lastTopForTest = top;
        boolean was = UiText.pause();
        try {
            for (int i = Math.max(0, shown.size() - maxCount); i < shown.size(); i++) {
                Entry e = shown.get(i);
                boolean newest = i == shown.size() - 1;
                Component text = e.count > 1 ? e.text.copy().append(Component.literal("  ×" + e.count).withStyle(s -> s.withColor(0xC8B48C))) : e.text;
                List<FormattedCharSequence> ls = lines(font, text, maxW - PAD_X * 2 - DOT);
                if (ls.isEmpty()) continue;
                totalLines += ls.size();
                int tw = 0;
                for (FormattedCharSequence l : ls) tw = Math.max(tw, font.width(l));
                int bw = Math.min(maxW, tw + PAD_X * 2 + DOT);
                int bh = ls.size() * lh + PAD_Y * 2 - 1;
                int x0 = (sw - bw) / 2;
                int y0 = y;
                if (y0 < 2 || y0 + bh > sh - 2) break; // 화면 밖으로 넘치면 더 쌓지 않는다
                if (x0 < 0 || x0 + bw > sw) inside = false;
                float a = alpha(e) * (newest ? 1f : 0.82f);
                if (a > 0.04f) {
                    int tone = tone(e.text);
                    int bg = withAlpha(0x15100C, a, 0xD8);
                    int edge = withAlpha(tone, a, newest ? 0xC0 : 0x80);
                    // 얇은 띠: 양 끝을 둥글린 어두운 바탕 + 알림 빛깔의 가는 테
                    g.fill(x0 + 2, y0, x0 + bw - 2, y0 + bh, bg);
                    g.fill(x0 + 1, y0 + 1, x0 + 2, y0 + bh - 1, bg);
                    g.fill(x0 + bw - 2, y0 + 1, x0 + bw - 1, y0 + bh - 1, bg);
                    g.fill(x0, y0 + 2, x0 + 1, y0 + bh - 2, bg);
                    g.fill(x0 + bw - 1, y0 + 2, x0 + bw, y0 + bh - 2, bg);
                    g.fill(x0 + 3, y0, x0 + bw - 3, y0 + 1, edge);
                    g.fill(x0 + 3, y0 + bh - 1, x0 + bw - 3, y0 + bh, edge);
                    g.fill(x0, y0 + 3, x0 + 1, y0 + bh - 3, edge);
                    g.fill(x0 + bw - 1, y0 + 3, x0 + bw, y0 + bh - 3, edge);
                    // 빛깔 점
                    int dc = withAlpha(tone, a, 0xFF);
                    int dx = x0 + PAD_X - 1, dy = y0 + PAD_Y + (lh - 1) / 2 - 2;
                    g.fill(dx + 1, dy, dx + 3, dy + 4, dc);
                    g.fill(dx, dy + 1, dx + 4, dy + 3, dc);
                    g.flush(); // 바탕을 먼저 그려 둔다 (글자와 한 묶음으로 그려지면 바탕이 빠질 수 있다)
                    int ta = Math.max(8, (int) (a * 255));
                    int color = (ta << 24) | UiText.SOFT_WHITE;
                    int ty = y0 + PAD_Y;
                    for (FormattedCharSequence l : ls) {
                        g.drawString(font, l, x0 + PAD_X + DOT - 2, ty, color, false);
                        ty += lh;
                    }
                    g.flush();
                    drawn++;
                }
                y = y0 + bh + GAP;
            }
        } finally {
            UiText.resume(was);
        }
        lastInsideForTest = inside;
        lastLinesForTest = totalLines;
        lastCountForTest = drawn;
    }

    private static float alpha(Entry e) {
        if (e.age < FADE_IN) return Math.max(0f, e.age / (float) FADE_IN);
        long left = e.life - e.age;
        if (left < FADE_OUT) return Math.max(0f, left / (float) FADE_OUT);
        return 1f;
    }
}

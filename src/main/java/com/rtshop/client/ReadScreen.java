package com.rtshop.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * 이야기 물건을 펼쳐 읽는 화면: 양피지 한 장에 제목과 글. 글이 길면 쪽을 넘긴다 (◀ ▶, 방향키, 마우스 휠).
 * 내용은 서버가 보낸다 (story/Readables): {title, sub, pages: [{head, text, by}]}.
 */
public class ReadScreen extends Screen {
    private static final int W = 236, H = 228, PAD = 18, LINE = 11, TOP = 44, BOTTOM = 26;
    private static final int C_HEAD = 0xFF7A3B1E, C_TEXT = 0xFF4A3520;

    private record Source(String head, String text, String by) {
    }

    /** 화면에 보이는 한 쪽 (긴 글은 여러 쪽으로 나뉜다). */
    private record Page(String head, List<FormattedCharSequence> lines, String by) {
    }

    private final String heading, sub;
    private final List<Source> sources = new ArrayList<>();
    private final List<Page> pages = new ArrayList<>();
    private int page, fx, fy;

    public ReadScreen(JsonObject o) {
        super(Component.literal(o.has("title") ? o.get("title").getAsString() : ""));
        heading = o.has("title") ? o.get("title").getAsString() : "";
        sub = o.has("sub") ? o.get("sub").getAsString() : "";
        if (o.has("pages")) {
            for (JsonElement e : o.getAsJsonArray("pages")) {
                JsonObject j = e.getAsJsonObject();
                sources.add(new Source(str(j, "head"), str(j, "text"), str(j, "by")));
            }
        }
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : "";
    }

    @Override
    protected void init() {
        fx = (this.width - W) / 2;
        fy = (this.height - H) / 2;
        int keep = page;
        pages.clear();
        int rows = (H - TOP - BOTTOM) / LINE;
        for (Source s : sources) {
            List<FormattedCharSequence> all = new ArrayList<>();
            for (String para : s.text.split("\n", -1)) {
                if (para.isEmpty()) all.add(FormattedCharSequence.EMPTY);
                else all.addAll(this.font.split(Component.literal(para), W - PAD * 2));
            }
            int head = s.head.isEmpty() ? 0 : 2, by = s.by.isEmpty() ? 0 : 2;
            int per = Math.max(3, rows - head - by);
            if (all.isEmpty()) all.add(FormattedCharSequence.EMPTY);
            for (int i = 0; i < all.size(); i += per) {
                boolean last = i + per >= all.size();
                pages.add(new Page(s.head, new ArrayList<>(all.subList(i, Math.min(all.size(), i + per))), last ? s.by : ""));
            }
        }
        if (pages.isEmpty()) pages.add(new Page("", List.of(), ""));
        page = Math.max(0, Math.min(keep, pages.size() - 1));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        this.renderBackground(g);
        g.fill(fx + 3, fy + 4, fx + W + 3, fy + H + 4, 0x66000000);                       // 종이 그림자
        Ui.bevel(g, fx, fy, W, H, Ui.C_PARCH, Ui.C_PARCH_HI, Ui.C_PARCH_SHADE);
        frame(g, fx + 5, fy + 5, W - 10, H - 10, Ui.C_PARCH_SHADE);                         // 안쪽 테두리 한 줄
        g.drawString(this.font, heading, fx + (W - this.font.width(heading)) / 2, fy + 13, Ui.C_INK, false);
        if (!sub.isEmpty()) {
            String s = Ui.clip(this.font, sub, W - PAD * 2);
            g.drawString(this.font, s, fx + (W - this.font.width(s)) / 2, fy + 25, Ui.C_INK_SOFT, false);
        }
        int ry = fy + 37;
        g.fill(fx + PAD, ry, fx + W - PAD, ry + 1, Ui.C_PARCH_SHADE);
        g.fill(fx + W / 2 - 2, ry - 1, fx + W / 2 + 2, ry + 2, Ui.C_GOLD_DARK);
        Page p = pages.get(page);
        int y = fy + TOP;
        if (!p.head.isEmpty()) {
            String h = Ui.clip(this.font, p.head, W - PAD * 2);
            g.drawString(this.font, h, fx + PAD, y, C_HEAD, false);
            y += LINE * 2;
        }
        for (FormattedCharSequence line : p.lines) {
            g.drawString(this.font, line, fx + PAD, y, C_TEXT, false);
            y += LINE;
        }
        if (!p.by.isEmpty()) {
            String b = "— " + p.by;
            g.drawString(this.font, b, fx + W - PAD - this.font.width(b), y + LINE, Ui.C_INK_SOFT, false);
        }
        // 쪽 넘김
        int by = fy + H - 20;
        if (pages.size() > 1) {
            String n = (page + 1) + " / " + pages.size();
            g.drawString(this.font, n, fx + (W - this.font.width(n)) / 2, by + 2, Ui.C_INK_SOFT, false);
            arrow(g, mx, my, fx + W / 2 - 46, by, "◀", page > 0);
            arrow(g, mx, my, fx + W / 2 + 30, by, "▶", page < pages.size() - 1);
        }
        super.render(g, mx, my, partial);
    }

    private void arrow(GuiGraphics g, int mx, int my, int x, int y, String s, boolean on) {
        boolean hov = on && Ui.inside(mx, my, x, y, 16, 12);
        if (hov) g.fill(x, y, x + 16, y + 12, 0x30A87A22);
        g.drawString(this.font, s, x + (16 - this.font.width(s)) / 2, y + 2, on ? (hov ? C_HEAD : Ui.C_INK) : Ui.C_PARCH_SHADE, false);
    }

    private static void frame(GuiGraphics g, int x, int y, int w, int h, int c) {
        g.fill(x, y, x + w, y + 1, c);
        g.fill(x, y + h - 1, x + w, y + h, c);
        g.fill(x, y, x + 1, y + h, c);
        g.fill(x + w - 1, y, x + w, y + h, c);
    }

    private void turn(int d) {
        int to = Math.max(0, Math.min(pages.size() - 1, page + d));
        if (to == page) return;
        page = to;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1F));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int by = fy + H - 20;
        if (button == 0 && pages.size() > 1) {
            if (Ui.inside(mx, my, fx + W / 2 - 46, by, 16, 12)) { turn(-1); return true; }
            if (Ui.inside(mx, my, fx + W / 2 + 30, by, 16, 12)) { turn(1); return true; }
            // 종이의 왼쪽·오른쪽 절반을 눌러도 넘어간다
            if (Ui.inside(mx, my, fx, fy + TOP, W, H - TOP - BOTTOM)) { turn(mx < fx + W / 2.0 ? -1 : 1); return true; }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        turn(delta < 0 ? 1 : -1);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 262 || key == 264 || key == 267 || key == 32) { turn(1); return true; }    // → ↓ PageDown Space
        if (key == 263 || key == 265 || key == 266) { turn(-1); return true; }                 // ← ↑ PageUp
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---- 시험용
    public int pagesForTest() {
        return pages.size();
    }

    public int pageForTest() {
        return page;
    }

    public void turnForTest(int d) {
        turn(d);
    }

    public String headingForTest() {
        return heading;
    }

    /** 지금 쪽의 글이 종이 안에 들어가는가. */
    public boolean fitsForTest() {
        Page p = pages.get(page);
        int rows = (H - TOP - BOTTOM) / LINE;
        return (p.head.isEmpty() ? 0 : 2) + p.lines.size() + (p.by.isEmpty() ? 0 : 2) <= rows;
    }
}

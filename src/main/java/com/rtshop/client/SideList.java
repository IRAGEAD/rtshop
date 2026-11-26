package com.rtshop.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 관리 화면 왼쪽의 묶음 목록: 머리줄(누를 수 없음)과 항목(누르면 고름). 항목 오른쪽에 개수, 앞에 색 점(바뀐 것 표시).
 * 직접 그리고 직접 눌림을 처리한다 (단추를 수백 개 만들지 않게).
 */
final class SideList {
    static final class Item {
        final String key, label;
        final boolean header;
        final int count;
        final int dot; // 0 = 점 없음

        Item(String key, String label, boolean header, int count, int dot) {
            this.key = key;
            this.label = label;
            this.header = header;
            this.count = count;
            this.dot = dot;
        }
    }

    final List<Item> items = new ArrayList<>();
    String selected = "";
    /** 함께 골라진 것으로 보일 항목들 (분류를 여러 개 겹쳐 쓸 때). */
    final java.util.Set<String> marks = new java.util.HashSet<>();
    int scroll;
    int x, y, w, h;
    static final int ROW = 13;

    void header(String label) {
        items.add(new Item("", label, true, -1, 0));
    }

    void item(String key, String label, int count, int dot) {
        items.add(new Item(key, label, false, count, dot));
    }

    int visible() {
        return Math.max(1, h / ROW);
    }

    void clampScroll() {
        scroll = Math.max(0, Math.min(scroll, Math.max(0, items.size() - visible())));
    }

    /** 고른 항목이 보이도록 스크롤. */
    void reveal() {
        for (int i = 0; i < items.size(); i++) {
            if (!items.get(i).header && items.get(i).key.equals(selected)) {
                if (i < scroll) scroll = i;
                if (i >= scroll + visible()) scroll = i - visible() + 1;
            }
        }
        clampScroll();
    }

    void render(GuiGraphics g, Font font, int mx, int my) {
        Ui.bevel(g, x - 2, y - 2, w + 4, h + 4, 0xFFD9C697, Ui.C_PARCH_HI, Ui.C_PARCH_SHADE);
        clampScroll();
        int n = visible();
        for (int i = 0; i < n && i + scroll < items.size(); i++) {
            Item it = items.get(i + scroll);
            int ry = y + i * ROW;
            if (it.header) {
                g.fill(x, ry + 1, x + w, ry + ROW - 1, 0xFF6B4A2A);
                g.drawString(font, Ui.clip(font, it.label, w - 6), x + 3, ry + 3, Ui.C_CREAM, false);
                continue;
            }
            boolean sel = it.key.equals(selected) || marks.contains(it.key);
            boolean hover = mx >= x && mx < x + w && my >= ry && my < ry + ROW;
            if (sel) {
                g.fill(x, ry, x + w, ry + ROW, 0xFFF2E4BD);
                g.fill(x, ry, x + 2, ry + ROW, 0xFFB07A2A);
            } else if (hover) {
                g.fill(x, ry, x + w, ry + ROW, 0x40FFFFFF);
            }
            int tx = x + 5;
            if (it.dot != 0) {
                g.fill(tx, ry + 5, tx + 3, ry + 8, 0xFF000000 | it.dot);
                tx += 5;
            }
            String c = it.count >= 0 ? Integer.toString(it.count) : "";
            int cw = c.isEmpty() ? 0 : font.width(c) + 4;
            g.drawString(font, Ui.clip(font, it.label, x + w - cw - tx - 2), tx, ry + 3, sel ? Ui.C_INK : 0xFF4A3420, false);
            if (!c.isEmpty()) g.drawString(font, c, x + w - cw, ry + 3, Ui.C_INK_SOFT, false);
        }
        if (items.size() > n) {
            int barH = Math.max(8, h * n / items.size());
            int by = y + (h - barH) * scroll / Math.max(1, items.size() - n);
            g.fill(x + w, by, x + w + 2, by + barH, 0xFF8A6A40);
        }
    }

    boolean click(double mx, double my, Consumer<String> pick) {
        if (mx < x || mx >= x + w || my < y || my >= y + h) return false;
        int i = (int) ((my - y) / ROW) + scroll;
        if (i < 0 || i >= items.size()) return true;
        Item it = items.get(i);
        if (!it.header) {
            selected = it.key;
            pick.accept(it.key);
        }
        return true;
    }

    boolean scroll(double mx, double my, double delta) {
        if (mx < x || mx >= x + w + 4 || my < y || my >= y + h) return false;
        scroll -= (int) Math.signum(delta) * 3;
        clampScroll();
        return true;
    }
}

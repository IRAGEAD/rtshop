package com.rtshop.client;

import com.google.gson.JsonArray;
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
 * 조사 수첩: 지금까지 살펴본 흔적들을 장별로 다시 읽는다. 왼쪽에서 흔적을 고르면 오른쪽에 그때 본 글이 나온다.
 */
public class NotebookScreen extends Screen {
    private static final int W = 392, H = 236, LIST_W = 132, ROW_H = 13;

    private record Entry(String chapter, String quest, String label, List<String> lines, boolean header) {
    }

    private final List<Entry> rows = new ArrayList<>();
    private int total;
    private int selected = -1, listScroll, textScroll, fx, fy;

    public NotebookScreen(JsonObject o) {
        super(Component.translatable("rtshop.notebook.title"));
        String last = null;
        if (o.has("entries")) {
            for (JsonElement e : o.getAsJsonArray("entries")) {
                JsonObject j = e.getAsJsonObject();
                String chap = j.get("chapter").getAsString();
                if (!chap.equals(last)) {
                    rows.add(new Entry(chap, "", chap, List.of(), true));
                    last = chap;
                }
                List<String> lines = new ArrayList<>();
                JsonArray a = j.getAsJsonArray("lines");
                for (JsonElement l : a) lines.add(l.getAsString());
                rows.add(new Entry(chap, j.get("quest").getAsString(), j.get("label").getAsString(), lines, false));
                if (selected < 0) selected = rows.size() - 1;
                total++;
            }
        }
    }

    public int countForTest() {
        return total;
    }

    @Override
    protected void init() {
        fx = (this.width - W) / 2;
        fy = (this.height - H) / 2;
    }

    private int visibleRows() {
        return (H - 36) / ROW_H;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        this.renderBackground(g);
        Ui.woodFrame(g, fx, fy, W, H);
        Ui.titleBand(g, fx + 5, fy + 4, fx + W - 5, fy + 23);
        g.fill(fx + 5, fy + 22, fx + W - 5, fy + 23, Ui.C_GOLD_DARK);
        String t = this.title.getString() + "  (" + total + ")";
        g.drawString(this.font, t, fx + (W - this.font.width(t)) / 2, fy + 9, Ui.C_GOLD, true);
        int lx = fx + 8, ly = fy + 28, lh = H - 36;
        Ui.bevel(g, lx, ly, LIST_W, lh, Ui.C_WOOD_DARK, Ui.C_WOOD, Ui.C_SHADOW);
        int dx = lx + LIST_W + 6, dw = W - LIST_W - 22;
        Ui.bevel(g, dx, ly, dw, lh, Ui.C_PARCH, Ui.C_PARCH_HI, Ui.C_PARCH_SHADE);
        if (total == 0) {
            List<FormattedCharSequence> w = this.font.split(Component.translatable("rtshop.notebook.empty"), dw - 16);
            for (int i = 0; i < w.size(); i++) g.drawString(this.font, w.get(i), dx + 8, ly + 10 + i * 11, Ui.C_INK_SOFT, false);
            super.render(g, mx, my, partial);
            return;
        }
        for (int i = 0; i < visibleRows() && i + listScroll < rows.size(); i++) {
            Entry e = rows.get(i + listScroll);
            int ry = ly + 3 + i * ROW_H;
            if (e.header) {
                g.drawString(this.font, Ui.clip(this.font, e.label, LIST_W - 8), lx + 4, ry + 2, Ui.C_GOLD, false);
                continue;
            }
            boolean on = i + listScroll == selected, hov = Ui.inside(mx, my, lx + 2, ry, LIST_W - 4, ROW_H);
            if (on || hov) g.fill(lx + 2, ry, lx + LIST_W - 2, ry + ROW_H - 1, on ? 0x80E8B84A : 0x40E8B84A);
            g.drawString(this.font, Ui.clip(this.font, "· " + e.label, LIST_W - 10), lx + 6, ry + 2, on ? 0xFFFFF2C8 : Ui.C_CREAM, false);
        }
        if (selected >= 0 && selected < rows.size()) {
            Entry e = rows.get(selected);
            List<Object[]> out = new ArrayList<>(); // {글줄, 색}
            for (FormattedCharSequence s : this.font.split(Component.literal(e.label), dw - 16)) out.add(new Object[]{s, Ui.C_INK});
            out.add(new Object[]{Component.literal(e.quest).getVisualOrderText(), Ui.C_INK_SOFT});
            out.add(new Object[]{FormattedCharSequence.EMPTY, 0});
            for (String line : e.lines) {
                for (FormattedCharSequence s : this.font.split(Component.literal(line), dw - 16)) out.add(new Object[]{s, 0xFF4A3520});
                out.add(new Object[]{FormattedCharSequence.EMPTY, 0});
            }
            int vis = (lh - 12) / 11;
            textScroll = Math.max(0, Math.min(textScroll, Math.max(0, out.size() - vis)));
            for (int i = 0; i < vis && i + textScroll < out.size(); i++) {
                Object[] r = out.get(i + textScroll);
                g.drawString(this.font, (FormattedCharSequence) r[0], dx + 8, ly + 7 + i * 11, (Integer) r[1], false);
            }
            if (out.size() > vis) {
                int bh = Math.max(12, (lh - 6) * vis / out.size()), by = ly + 3 + (lh - 6 - bh) * textScroll / Math.max(1, out.size() - vis);
                g.fill(dx + dw - 5, by, dx + dw - 3, by + bh, Ui.C_PARCH_SHADE);
            }
        }
        super.render(g, mx, my, partial);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int lx = fx + 8, ly = fy + 28;
        if (button == 0 && Ui.inside(mx, my, lx, ly, LIST_W, H - 36)) {
            int i = (int) ((my - ly - 3) / ROW_H) + listScroll;
            if (i >= 0 && i < rows.size() && !rows.get(i).header && i != selected) {
                selected = i;
                textScroll = 0;
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1F));
            }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (Ui.inside(mx, my, fx + 8, fy + 28, LIST_W, H - 36)) {
            listScroll = Math.max(0, Math.min(Math.max(0, rows.size() - visibleRows()), listScroll - (int) Math.signum(delta) * 2));
        } else {
            textScroll = Math.max(0, textScroll - (int) Math.signum(delta) * 2);
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

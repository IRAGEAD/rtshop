package com.rtshop.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rtshop.net.PetitionPacket;
import com.rtshop.net.ShopNet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * 민원 창 (3.61): 시청 서기 하루에게 말을 걸어 [민원] 을 고르면 열린다.
 * 왼쪽 탭 「새 민원」: 분류를 고르고 글을 써서 [접수]. 오른쪽 탭 「내 민원」: 낸 민원의 상태와 답변, 답이 없는 것은 [거두기].
 * 내용은 서버가 보낸다 (petition/Petitions.view): {max, maxOpen, cats, mine: [{id, cat, state, when, text, answer, by}], open, msg, filed}.
 */
public class PetitionScreen extends Screen {
    private static final int W = 268, H = 236, PAD = 14, LINE = 10, LIST_TOP = 50, LIST_BOTTOM = 30;
    private static final int C_OPEN = 0xFF9A6A12, C_ANSWERED = 0xFF2F6B2F, C_CLOSED = 0xFF7A6240;

    /** 내 민원 목록의 한 줄: 글, 빛깔, 들여쓰기, 거둘 수 있는 민원의 번호(없으면 0). */
    private record Row(FormattedCharSequence text, int color, int indent, int withdraw) {
    }

    private final List<String> cats = new ArrayList<>();
    private final List<JsonObject> mine = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();
    private int fx, fy, tab, cat = -1, scroll, max = 300, maxOpen = 5, openCount;
    private String msg = "", draft = "";
    private MultiLineEditBox box;

    public PetitionScreen(JsonObject o) {
        super(Component.translatable("rtshop.petition.title"));
        set(o);
    }

    public static void open(JsonObject o) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PetitionScreen s) {
            s.set(o);
            s.rebuildWidgets();
        } else {
            mc.setScreen(new PetitionScreen(o));
        }
    }

    private void set(JsonObject o) {
        cats.clear();
        mine.clear();
        if (o.has("cats")) for (JsonElement e : o.getAsJsonArray("cats")) cats.add(e.getAsString());
        if (o.has("mine")) for (JsonElement e : o.getAsJsonArray("mine")) mine.add(e.getAsJsonObject());
        max = o.has("max") ? o.get("max").getAsInt() : 300;
        maxOpen = o.has("maxOpen") ? o.get("maxOpen").getAsInt() : 5;
        openCount = o.has("open") ? o.get("open").getAsInt() : 0;
        msg = o.has("msg") ? o.get("msg").getAsString() : "";
        if (o.has("filed") && o.get("filed").getAsBoolean()) {
            draft = "";
            cat = -1;
            tab = 1;
            scroll = 0;
        }
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : "";
    }

    private int listRows() {
        return (H - LIST_TOP - LIST_BOTTOM) / LINE;
    }

    @Override
    protected void init() {
        fx = (this.width - W) / 2;
        fy = (this.height - H) / 2;
        box = null;
        if (tab == 0) {
            box = new MultiLineEditBox(this.font, fx + PAD, fy + 86, W - PAD * 2, 92, Component.empty(), Component.empty());
            box.setCharacterLimit(max);
            box.setValue(draft);
            box.setValueListener(v -> draft = v);
            this.addRenderableWidget(box);
        }
        rows.clear();
        int wrap = W - PAD * 2 - 8;
        for (JsonObject j : mine) {
            int state = j.has("state") ? j.get("state").getAsInt() : 0;
            int color = state == 0 ? C_OPEN : state == 1 ? C_ANSWERED : C_CLOSED;
            String head = "#" + str(j, "id") + "  " + Component.translatable("rtshop.petition.cat." + str(j, "cat")).getString()
                    + " · " + Component.translatable("rtshop.petition.state." + state).getString() + " · " + str(j, "when");
            rows.add(new Row(Component.literal(head).getVisualOrderText(), color, 0, state == 0 ? j.get("id").getAsInt() : 0));
            for (FormattedCharSequence l : this.font.split(Component.literal(str(j, "text")), wrap)) rows.add(new Row(l, Ui.C_INK, 6, 0));
            String answer = str(j, "answer");
            if (!answer.isEmpty()) {
                String by = str(j, "by");
                String a = "└ " + Component.translatable("rtshop.petition.answer").getString() + (by.isEmpty() ? "" : " (" + by + ")") + ": " + answer;
                for (FormattedCharSequence l : this.font.split(Component.literal(a), wrap)) rows.add(new Row(l, C_ANSWERED, 6, 0));
            }
            rows.add(new Row(FormattedCharSequence.EMPTY, 0, 0, 0));
        }
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - listRows())));
    }

    private int tabW() {
        return (W - PAD * 2 - 6) / 2;
    }

    private int catW() {
        return (W - PAD * 2 - 4 * Math.max(0, cats.size() - 1)) / Math.max(1, cats.size());
    }

    private boolean canFile() {
        return cat >= 0 && !draft.trim().isEmpty() && openCount < maxOpen;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        this.renderBackground(g);
        g.fill(fx + 3, fy + 4, fx + W + 3, fy + H + 4, 0x66000000);
        Ui.bevel(g, fx, fy, W, H, Ui.C_PARCH, Ui.C_PARCH_HI, Ui.C_PARCH_SHADE);
        String title = this.title.getString();
        g.drawString(this.font, title, fx + (W - this.font.width(title)) / 2, fy + 10, Ui.C_INK, false);
        g.fill(fx + PAD, fy + 22, fx + W - PAD, fy + 23, Ui.C_PARCH_SHADE);
        // 탭
        int tw = tabW();
        String t0 = Component.translatable("rtshop.petition.tab_new").getString();
        String t1 = Component.translatable("rtshop.petition.tab_mine", mine.size()).getString();
        Ui.button(g, this.font, fx + PAD, fy + 28, tw, 16, t0, tab == 0 || Ui.inside(mx, my, fx + PAD, fy + 28, tw, 16), true);
        Ui.button(g, this.font, fx + PAD + tw + 6, fy + 28, tw, 16, t1, tab == 1 || Ui.inside(mx, my, fx + PAD + tw + 6, fy + 28, tw, 16), true);
        if (tab == 0) {
            g.drawString(this.font, Component.translatable("rtshop.petition.pick_cat").getString(), fx + PAD, fy + 52, Ui.C_INK_SOFT, false);
            int cw = catW();
            for (int i = 0; i < cats.size(); i++) {
                int x = fx + PAD + i * (cw + 4), y = fy + 63;
                String label = Ui.clip(this.font, Component.translatable("rtshop.petition.cat." + cats.get(i)).getString(), cw - 4);
                Ui.button(g, this.font, x, y, cw, 16, label, i == cat || Ui.inside(mx, my, x, y, cw, 16), true);
                if (i == cat) {
                    g.fill(x, y + 17, x + cw, y + 19, Ui.C_GOLD_DARK);
                }
            }
            String count = Component.translatable("rtshop.petition.count", draft.length(), max).getString();
            g.drawString(this.font, count, fx + W - PAD - this.font.width(count), fy + 181, Ui.C_INK_SOFT, false);
            String left = Component.translatable("rtshop.petition.open_left", openCount, maxOpen).getString();
            g.drawString(this.font, left, fx + PAD, fy + 181, openCount >= maxOpen ? 0xFF9A2A1A : Ui.C_INK_SOFT, false);
            int bw = 70, bx = fx + W - PAD - bw, by = fy + H - 26;
            Ui.button(g, this.font, bx, by, bw, 18, Component.translatable("rtshop.petition.submit").getString(), canFile() && Ui.inside(mx, my, bx, by, bw, 18), canFile());
            List<FormattedCharSequence> hint = this.font.split(Component.literal(msg.isEmpty() ? Component.translatable("rtshop.petition.hint").getString() : msg), W - PAD * 2 - bw - 8);
            for (int i = 0; i < Math.min(2, hint.size()); i++) {
                g.drawString(this.font, hint.get(i), fx + PAD, fy + H - 27 + i * LINE, msg.isEmpty() ? Ui.C_INK_SOFT : C_OPEN, false);
            }
        } else {
            int top = fy + LIST_TOP, vis = listRows();
            if (rows.isEmpty()) {
                String none = Component.translatable("rtshop.petition.none").getString();
                g.drawString(this.font, none, fx + (W - this.font.width(none)) / 2, top + 30, Ui.C_INK_SOFT, false);
            }
            String wd = "[" + Component.translatable("rtshop.petition.withdraw").getString() + "]";
            int ww = this.font.width(wd);
            for (int i = 0; i < vis && scroll + i < rows.size(); i++) {
                Row r = rows.get(scroll + i);
                int y = top + i * LINE;
                g.drawString(this.font, r.text, fx + PAD + r.indent, y, r.color, false);
                if (r.withdraw > 0) {
                    boolean hov = Ui.inside(mx, my, fx + W - PAD - ww, y - 1, ww, LINE);
                    g.drawString(this.font, wd, fx + W - PAD - ww, y, hov ? 0xFF9A2A1A : Ui.C_INK_SOFT, false);
                }
            }
            if (rows.size() > vis) {
                int trackH = vis * LINE, barH = Math.max(12, trackH * vis / rows.size());
                int barY = top + (trackH - barH) * scroll / Math.max(1, rows.size() - vis);
                g.fill(fx + W - 8, top, fx + W - 6, top + trackH, Ui.C_PARCH_SHADE);
                g.fill(fx + W - 8, barY, fx + W - 6, barY + barH, Ui.C_GOLD_DARK);
            }
            if (!msg.isEmpty()) {
                g.drawString(this.font, Ui.clip(this.font, msg, W - PAD * 2), fx + PAD, fy + H - 20, C_OPEN, false);
            }
        }
        super.render(g, mx, my, partial);
    }

    private void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int tw = tabW();
        if (Ui.inside(mx, my, fx + PAD, fy + 28, tw, 16) && tab != 0) {
            tab = 0;
            msg = "";
            click();
            this.rebuildWidgets();
            return true;
        }
        if (Ui.inside(mx, my, fx + PAD + tw + 6, fy + 28, tw, 16) && tab != 1) {
            tab = 1;
            msg = "";
            click();
            this.rebuildWidgets();
            return true;
        }
        if (tab == 0) {
            int cw = catW();
            for (int i = 0; i < cats.size(); i++) {
                if (Ui.inside(mx, my, fx + PAD + i * (cw + 4), fy + 63, cw, 16)) {
                    cat = i;
                    click();
                    return true;
                }
            }
            int bw = 70, bx = fx + W - PAD - bw, by = fy + H - 26;
            if (Ui.inside(mx, my, bx, by, bw, 18)) {
                if (canFile()) {
                    click();
                    ShopNet.CHANNEL.sendToServer(new PetitionPacket(0, cat, draft));
                }
                return true;
            }
        } else {
            String wd = "[" + Component.translatable("rtshop.petition.withdraw").getString() + "]";
            int ww = this.font.width(wd), top = fy + LIST_TOP, vis = listRows();
            for (int i = 0; i < vis && scroll + i < rows.size(); i++) {
                Row r = rows.get(scroll + i);
                if (r.withdraw > 0 && Ui.inside(mx, my, fx + W - PAD - ww, top + i * LINE - 1, ww, LINE)) {
                    click();
                    ShopNet.CHANNEL.sendToServer(new PetitionPacket(1, r.withdraw, ""));
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (tab == 1) {
            scroll = Math.max(0, Math.min(Math.max(0, rows.size() - listRows()), scroll - (int) Math.signum(delta) * 2));
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public int rowsForTest() {
        return rows.size();
    }
}

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
 * 3.71: 운영자(크리에이티브·관전의 OP)에게는 셋째 탭 「운영」 — 모든 민원을 상태 · 분류로 걸러 보고 [답변] [종결] [이동] [삭제].
 * 어디서나 /rtshop report admin 으로 이 탭을 연다.
 * 내용은 서버가 보낸다 (petition/Petitions.view): {max, maxOpen, cats, mine: [{id, cat, state, when, text, answer, by}], open, msg, filed,
 * tab?, op, all?: [{id, cat, state, when, name, where, text, answer, by}], waiting?}.
 */
public class PetitionScreen extends Screen {
    private static final int W1 = 268, H1 = 236, W2 = 340, H2 = 262, PAD = 14, LINE = 10, LIST_TOP = 50, LIST_BOTTOM = 30;
    /** 운영 탭: 걸러 보는 줄의 아래부터 목록, 답을 쓰는 칸의 높이. */
    private static final int ADMIN_TOP = 72, ANSWER_H = 40;
    private int W = W1, H = H1;
    private static final int C_OPEN = 0xFF9A6A12, C_ANSWERED = 0xFF2F6B2F, C_CLOSED = 0xFF7A6240;

    /** 내 민원 목록의 한 줄: 글, 빛깔, 들여쓰기, 거둘 수 있는 민원의 번호(없으면 0), 운영 탭의 단추 줄이면 그 민원(없으면 null). */
    private record Row(FormattedCharSequence text, int color, int indent, int withdraw, JsonObject acts) {
        Row(FormattedCharSequence text, int color, int indent, int withdraw) {
            this(text, color, indent, withdraw, null);
        }
    }

    /** 운영 탭 단추: 3 답변 · 4 종결 · 6 이동 · 5 지우기 (서버의 fromScreen 일 번호). */
    private static final int[] ACTS = {3, 4, 6, 5};
    private static final String[] ACT_KEYS = {"rtshop.petition.act_answer", "rtshop.petition.act_close", "rtshop.petition.act_tp", "rtshop.petition.act_delete"};

    private final List<String> cats = new ArrayList<>();
    private final List<JsonObject> mine = new ArrayList<>();
    private final List<JsonObject> all = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();
    private int fx, fy, tab, cat = -1, scroll, max = 300, maxOpen = 5, openCount, waiting;
    /** 운영 탭의 거르기: 상태 0 대기 · 1 답변함 · 2 종결 · 3 전체, 분류 -1 = 전체. 답을 쓰는 중인 민원 번호 (0 = 아님). */
    private int fState, fCat = -1, answering;
    private boolean op;
    private String msg = "", draft = "", answerDraft = "";
    private MultiLineEditBox box, answerBox;

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
        all.clear();
        if (o.has("cats")) for (JsonElement e : o.getAsJsonArray("cats")) cats.add(e.getAsString());
        if (o.has("mine")) for (JsonElement e : o.getAsJsonArray("mine")) mine.add(e.getAsJsonObject());
        op = o.has("op") && o.get("op").getAsBoolean();
        if (o.has("all")) for (JsonElement e : o.getAsJsonArray("all")) all.add(e.getAsJsonObject());
        waiting = o.has("waiting") ? o.get("waiting").getAsInt() : 0;
        if (o.has("tab")) {
            int t = o.get("tab").getAsInt();
            if (t == 2 && op) {
                if (tab != 2) scroll = 0;
                tab = 2;
            } else if (t >= 0 && t <= 1) {
                tab = t;
            }
        }
        if (tab == 2 && !op) tab = 0;
        if (answering > 0) {
            boolean still = false;
            for (JsonObject j : all) if (j.get("id").getAsInt() == answering && j.get("state").getAsInt() != 2) still = true;
            if (!still || (o.has("msg") && !o.get("msg").getAsString().isEmpty() && tab == 2)) {
                answering = 0;
                answerDraft = "";
            }
        }
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
        if (tab == 2) return (H - ADMIN_TOP - (answering > 0 ? ANSWER_H + 34 : 26)) / LINE;
        return (H - LIST_TOP - LIST_BOTTOM) / LINE;
    }

    @Override
    protected void init() {
        W = tab == 2 ? W2 : W1;
        H = tab == 2 ? H2 : H1;
        fx = (this.width - W) / 2;
        fy = (this.height - H) / 2;
        box = null;
        answerBox = null;
        if (tab == 2) {
            initAdmin();
            return;
        }
        if (tab == 0) {
            // 바닐라의 검은 상자 대신 창에 어울리는 짙은 갈색 바탕 (글자는 밝은 빛 그대로)
            box = new MultiLineEditBox(this.font, fx + PAD, fy + 86, W - PAD * 2, 92, Component.translatable("rtshop.petition.placeholder"), Component.empty()) {
                public void renderBackground(GuiGraphics g) {
                    g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), isFocused() ? Ui.C_GOLD_DARK : Ui.C_PARCH_SHADE);
                    g.fill(getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + getHeight() - 1, 0xFF3B2A1A);
                }
            };
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

    /** 운영 탭: 걸러 낸 민원을 줄로 펴고, 답을 쓰는 중이면 아래에 글 칸. */
    private void initAdmin() {
        rows.clear();
        int wrap = W - PAD * 2 - 8;
        for (JsonObject j : all) {
            int state = j.has("state") ? j.get("state").getAsInt() : 0;
            if (fState < 3 && state != fState) continue;
            if (fCat >= 0 && (fCat >= cats.size() || !cats.get(fCat).equals(str(j, "cat")))) continue;
            int color = state == 0 ? C_OPEN : state == 1 ? C_ANSWERED : C_CLOSED;
            String head = "#" + str(j, "id") + "  " + Component.translatable("rtshop.petition.cat." + str(j, "cat")).getString()
                    + " · " + Component.translatable("rtshop.petition.state." + state).getString() + " · " + str(j, "when") + " · " + str(j, "name");
            rows.add(new Row(Component.literal(head).getVisualOrderText(), color, 0, 0));
            rows.add(new Row(Component.literal("@ " + str(j, "where")).getVisualOrderText(), Ui.C_INK_SOFT, 6, 0));
            for (FormattedCharSequence l : this.font.split(Component.literal(str(j, "text")), wrap)) rows.add(new Row(l, Ui.C_INK, 6, 0));
            String answer = str(j, "answer");
            if (!answer.isEmpty()) {
                String by = str(j, "by");
                String a = "└ " + Component.translatable("rtshop.petition.answer").getString() + (by.isEmpty() ? "" : " (" + by + ")") + ": " + answer;
                for (FormattedCharSequence l : this.font.split(Component.literal(a), wrap)) rows.add(new Row(l, C_ANSWERED, 6, 0));
            }
            rows.add(new Row(FormattedCharSequence.EMPTY, 0, 6, 0, j));
            rows.add(new Row(FormattedCharSequence.EMPTY, 0, 0, 0));
        }
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - listRows())));
        if (answering > 0) {
            int ay = fy + H - ANSWER_H - 30;
            answerBox = new MultiLineEditBox(this.font, fx + PAD, ay + 12, W - PAD * 2 - 84, ANSWER_H, Component.translatable("rtshop.petition.answer_placeholder"), Component.empty()) {
                public void renderBackground(GuiGraphics g) {
                    g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), isFocused() ? Ui.C_GOLD_DARK : Ui.C_PARCH_SHADE);
                    g.fill(getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + getHeight() - 1, 0xFF3B2A1A);
                }
            };
            answerBox.setCharacterLimit(max);
            answerBox.setValue(answerDraft);
            answerBox.setValueListener(v -> answerDraft = v);
            this.addRenderableWidget(answerBox);
            this.setFocused(answerBox);
        }
    }

    /** 운영 탭 거르기 줄: 상태 넷 + 분류 하나(누를 때마다 다음 분류). */
    private int stateChipW() {
        return 50;
    }

    private String catLabel() {
        String c = fCat < 0 || fCat >= cats.size() ? Component.translatable("rtshop.petition.f_all").getString()
                : Component.translatable("rtshop.petition.cat." + cats.get(fCat)).getString();
        return Component.translatable("rtshop.petition.f_cat", c).getString();
    }

    /** 단추 줄에서 n 번째 단추의 왼쪽 끝 (글 너비로 늘어놓음). */
    private int actX(int n, boolean closed) {
        int x = fx + PAD + 6;
        for (int i = 0; i < n; i++) {
            if (closed && i < 2) continue;
            x += this.font.width("[" + Component.translatable(ACT_KEYS[i]).getString() + "]") + 8;
        }
        return x;
    }

    private int tabW() {
        if (op) return (W - PAD * 2 - 12) / 3;
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
        Ui.button(g, this.font, fx + PAD, fy + 28, tw, 16, Ui.clip(this.font, t0, tw - 4), tab == 0 || Ui.inside(mx, my, fx + PAD, fy + 28, tw, 16), true);
        Ui.button(g, this.font, fx + PAD + tw + 6, fy + 28, tw, 16, Ui.clip(this.font, t1, tw - 4), tab == 1 || Ui.inside(mx, my, fx + PAD + tw + 6, fy + 28, tw, 16), true);
        if (op) {
            String t2 = Component.translatable("rtshop.petition.tab_admin", waiting).getString();
            int x2 = fx + PAD + (tw + 6) * 2;
            Ui.button(g, this.font, x2, fy + 28, tw, 16, Ui.clip(this.font, t2, tw - 4), tab == 2 || Ui.inside(mx, my, x2, fy + 28, tw, 16), true);
        }
        if (tab == 2) {
            renderAdmin(g, mx, my);
        } else if (tab == 0) {
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

    private void renderAdmin(GuiGraphics g, int mx, int my) {
        // 거르기 줄
        int cw = stateChipW(), y = fy + 50;
        String[] states = {"rtshop.petition.state.0", "rtshop.petition.state.1", "rtshop.petition.state.2", "rtshop.petition.f_all"};
        for (int i = 0; i < 4; i++) {
            int x = fx + PAD + i * (cw + 3);
            Ui.button(g, this.font, x, y, cw, 15, Ui.clip(this.font, Component.translatable(states[i]).getString(), cw - 4), i == fState || Ui.inside(mx, my, x, y, cw, 15), true);
            if (i == fState) g.fill(x, y + 16, x + cw, y + 18, Ui.C_GOLD_DARK);
        }
        int cx = fx + PAD + 4 * (cw + 3) + 4, cwid = fx + W - PAD - cx;
        Ui.button(g, this.font, cx, y, cwid, 15, Ui.clip(this.font, catLabel(), cwid - 4), Ui.inside(mx, my, cx, y, cwid, 15), true);
        // 목록
        int top = fy + ADMIN_TOP, vis = listRows();
        if (rows.isEmpty()) {
            String none = Component.translatable("rtshop.petition.admin_none").getString();
            g.drawString(this.font, none, fx + (W - this.font.width(none)) / 2, top + 30, Ui.C_INK_SOFT, false);
        }
        for (int i = 0; i < vis && scroll + i < rows.size(); i++) {
            Row r = rows.get(scroll + i);
            int ry = top + i * LINE;
            if (r.acts != null) {
                boolean closed = r.acts.get("state").getAsInt() == 2;
                int id = r.acts.get("id").getAsInt();
                for (int k = 0; k < ACTS.length; k++) {
                    if (closed && k < 2) continue;
                    String label = "[" + Component.translatable(ACT_KEYS[k]).getString() + "]";
                    int ax = actX(k, closed), aw = this.font.width(label);
                    boolean hov = Ui.inside(mx, my, ax, ry - 1, aw, LINE);
                    int col = k == 3 ? (hov ? 0xFF9A2A1A : 0xFF8A5A4A) : (hov ? Ui.C_GOLD_DARK : (answering == id && k == 0 ? C_ANSWERED : Ui.C_INK_SOFT));
                    g.drawString(this.font, label, ax, ry, col, false);
                }
            } else {
                g.drawString(this.font, r.text, fx + PAD + r.indent, ry, r.color, false);
            }
        }
        if (rows.size() > vis) {
            int trackH = vis * LINE, barH = Math.max(12, trackH * vis / rows.size());
            int barY = top + (trackH - barH) * scroll / Math.max(1, rows.size() - vis);
            g.fill(fx + W - 8, top, fx + W - 6, top + trackH, Ui.C_PARCH_SHADE);
            g.fill(fx + W - 8, barY, fx + W - 6, barY + barH, Ui.C_GOLD_DARK);
        }
        if (answering > 0) {
            int ay = fy + H - ANSWER_H - 30;
            g.drawString(this.font, Component.translatable("rtshop.petition.answer_to", answering).getString(), fx + PAD, ay + 2, C_ANSWERED, false);
            int bx = fx + W - PAD - 78;
            boolean can = !answerDraft.trim().isEmpty();
            Ui.button(g, this.font, bx, ay + 12, 78, 18, Component.translatable("rtshop.petition.send").getString(), can && Ui.inside(mx, my, bx, ay + 12, 78, 18), can);
            Ui.button(g, this.font, bx, ay + 34, 78, 18, Component.translatable("rtshop.petition.cancel").getString(), Ui.inside(mx, my, bx, ay + 34, 78, 18), true);
        }
        if (!msg.isEmpty()) {
            g.drawString(this.font, Ui.clip(this.font, msg, W - PAD * 2), fx + PAD, fy + H - 16, C_OPEN, false);
        }
    }

    private boolean clickAdmin(double mx, double my) {
        int cw = stateChipW(), y = fy + 50;
        for (int i = 0; i < 4; i++) {
            if (Ui.inside(mx, my, fx + PAD + i * (cw + 3), y, cw, 15)) {
                if (fState != i) {
                    fState = i;
                    scroll = 0;
                    click();
                    this.rebuildWidgets();
                }
                return true;
            }
        }
        int cx = fx + PAD + 4 * (cw + 3) + 4;
        if (Ui.inside(mx, my, cx, y, fx + W - PAD - cx, 15)) {
            fCat = fCat + 1 >= cats.size() ? -1 : fCat + 1;
            scroll = 0;
            click();
            this.rebuildWidgets();
            return true;
        }
        int top = fy + ADMIN_TOP, vis = listRows();
        for (int i = 0; i < vis && scroll + i < rows.size(); i++) {
            Row r = rows.get(scroll + i);
            if (r.acts == null) continue;
            int ry = top + i * LINE;
            boolean closed = r.acts.get("state").getAsInt() == 2;
            int id = r.acts.get("id").getAsInt();
            for (int k = 0; k < ACTS.length; k++) {
                if (closed && k < 2) continue;
                String label = "[" + Component.translatable(ACT_KEYS[k]).getString() + "]";
                if (!Ui.inside(mx, my, actX(k, closed), ry - 1, this.font.width(label), LINE)) continue;
                click();
                if (ACTS[k] == 3) {
                    answering = answering == id ? 0 : id;
                    answerDraft = answering > 0 ? str(r.acts, "answer") : "";
                    this.rebuildWidgets();
                } else if (ACTS[k] == 5 && !hasShiftDown() && !confirmDelete(id)) {
                    msg = Component.translatable("rtshop.petition.delete_confirm", id).getString();
                } else {
                    if (ACTS[k] == 6) Minecraft.getInstance().setScreen(null);
                    ShopNet.CHANNEL.sendToServer(new PetitionPacket(ACTS[k], id, ""));
                }
                return true;
            }
        }
        if (answering > 0) {
            int ay = fy + H - ANSWER_H - 30, bx = fx + W - PAD - 78;
            if (Ui.inside(mx, my, bx, ay + 12, 78, 18)) {
                if (!answerDraft.trim().isEmpty()) {
                    click();
                    ShopNet.CHANNEL.sendToServer(new PetitionPacket(3, answering, answerDraft));
                }
                return true;
            }
            if (Ui.inside(mx, my, bx, ay + 34, 78, 18)) {
                click();
                answering = 0;
                answerDraft = "";
                this.rebuildWidgets();
                return true;
            }
        }
        return false;
    }

    /** 지우기는 두 번 눌러야 된다 (Shift 를 누르고 누르면 바로). */
    private int deleteArmed;

    private boolean confirmDelete(int id) {
        if (deleteArmed == id) {
            deleteArmed = 0;
            return true;
        }
        deleteArmed = id;
        return false;
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
            scroll = 0;
            click();
            this.rebuildWidgets();
            return true;
        }
        if (Ui.inside(mx, my, fx + PAD + tw + 6, fy + 28, tw, 16) && tab != 1) {
            tab = 1;
            msg = "";
            scroll = 0;
            click();
            this.rebuildWidgets();
            return true;
        }
        if (op && Ui.inside(mx, my, fx + PAD + (tw + 6) * 2, fy + 28, tw, 16) && tab != 2) {
            tab = 2;
            msg = "";
            scroll = 0;
            click();
            ShopNet.CHANNEL.sendToServer(new PetitionPacket(7, 0, ""));   // 최신 목록
            this.rebuildWidgets();
            return true;
        }
        if (tab == 2) {
            if (clickAdmin(mx, my)) return true;
        } else if (tab == 0) {
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
        if (tab == 1 || tab == 2) {
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

    public void tabForTest(int t) {
        tab = t;
        this.rebuildWidgets();
    }

    public boolean opForTest() {
        return op;
    }

    public int allForTest() {
        return all.size();
    }

    public int tabNowForTest() {
        return tab;
    }

    /** 점검용: 운영 탭에서 그 민원의 [답변] 을 눌러 글 칸을 연다. */
    public void answerForTest(int id, String text) {
        answering = id;
        answerDraft = text;
        this.rebuildWidgets();
    }

    public void filterForTest(int state, int category) {
        fState = state;
        fCat = category;
        this.rebuildWidgets();
    }
}

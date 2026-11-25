package com.rtshop.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * 칭호 (칭호 관리인): 왼쪽에서 종목을 고르면 오른쪽에 그 종목의 칭호가 단계별로 나온다.
 * 열린 칭호는 [힘 쓰기] 로 효과를 켜고(2개까지, 종목이 달라야 한다), [내걸기] 로 이름 앞에 건다(1개).
 * 맨 아래 「이번 주의 …」 는 지난주 1위에게 한 주 동안 열리는, 내걸기만 되는 칭호.
 */
public class TitleScreen extends Screen {
    private static final int W = 348, H = 236, LIST_W = 98, CAT_ROW = 17, ROW = 31;
    private static final String WEEK = "_week";

    private JsonObject data;
    private final List<JsonObject> cats = new ArrayList<>();
    private final List<String> effects = new ArrayList<>(), week = new ArrayList<>();
    private String show = "";
    private int sel, scroll, fx, fy;

    public TitleScreen(JsonObject o) {
        super(Component.translatable("rtshop.title.title"));
        set(o);
    }

    public static void open(JsonObject o) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof TitleScreen t) t.set(o);
        else mc.setScreen(new TitleScreen(o));
    }

    private void set(JsonObject o) {
        data = o;
        cats.clear();
        effects.clear();
        week.clear();
        for (JsonElement e : o.getAsJsonArray("cats")) cats.add(e.getAsJsonObject());
        for (JsonElement e : o.getAsJsonArray("effects")) effects.add(e.getAsString());
        for (JsonElement e : o.getAsJsonArray("week")) week.add(e.getAsString());
        show = o.get("show").getAsString();
        sel = Math.max(0, Math.min(sel, rowsLeft() - 1));
    }

    @Override
    protected void init() {
        fx = (width - W) / 2;
        fy = (height - H) / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** 왼쪽 줄 수: 종목들 + (지난주 1위가 있으면) 주간 칭호. */
    private int rowsLeft() {
        return cats.size() + (week.isEmpty() ? 0 : 1);
    }

    private boolean weekSel() {
        return sel >= cats.size();
    }

    static String name(String id, String fallback) {
        if (id.startsWith("week:")) return Component.translatable("rtshop.title.week." + id.substring(5)).getString();
        String key = "rtshop.title.name." + id;
        return I18n.exists(key) ? I18n.get(key) : fallback;
    }

    static String num(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    static String effect(String effect, double v) {
        return Component.translatable("rtshop.title.effect." + effect, num(v)).getString();
    }

    private String shownName() {
        if (show.isEmpty()) return Component.translatable("rtshop.title.none").getString();
        for (JsonObject c : cats) {
            for (JsonElement e : c.getAsJsonArray("titles")) {
                JsonObject t = e.getAsJsonObject();
                if (t.get("id").getAsString().equals(show)) return name(show, t.get("name").getAsString());
            }
        }
        return name(show, show);
    }

    private int visible() {
        return (H - 62) / ROW;
    }

    /** 오른쪽에 보일 칭호들: {id, 이름, 필요, 효과, 값, 열림}. */
    private List<JsonObject> rows() {
        List<JsonObject> out = new ArrayList<>();
        if (weekSel()) {
            for (String cat : week) {
                JsonObject o = new JsonObject();
                o.addProperty("id", "week:" + cat);
                o.addProperty("name", "");
                o.addProperty("need", 0);
                o.addProperty("effect", "");
                o.addProperty("value", 0);
                o.addProperty("open", true);
                o.addProperty("cat", cat);
                out.add(o);
            }
        } else if (!cats.isEmpty()) {
            for (JsonElement e : cats.get(sel).getAsJsonArray("titles")) out.add(e.getAsJsonObject());
        }
        return out;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        Ui.woodFrame(g, fx, fy, W, H);
        Ui.titleBand(g, fx + 5, fy + 4, fx + W - 5, fy + 23);
        g.fill(fx + 5, fy + 22, fx + W - 5, fy + 23, Ui.C_GOLD_DARK);
        String t = title.getString();
        g.drawString(font, t, fx + (W - font.width(t)) / 2, fy + 9, Ui.C_GOLD, true);
        // 지금 켠 것
        String now = Component.translatable("rtshop.title.now", effects.size(), data.get("max").getAsInt(), shownName()).getString();
        g.drawString(font, Ui.clip(font, now, W - 20), fx + 10, fy + 28, Ui.C_CREAM, false);
        int lx = fx + 8, ly = fy + 41, lh = H - 49;
        Ui.bevel(g, lx, ly, LIST_W, lh, Ui.C_WOOD_DARK, Ui.C_WOOD, Ui.C_SHADOW);
        for (int i = 0; i < rowsLeft(); i++) {
            int ry = ly + 3 + i * CAT_ROW;
            boolean on = i == sel, hov = Ui.inside(mx, my, lx + 2, ry, LIST_W - 4, CAT_ROW - 1);
            if (on || hov) g.fill(lx + 2, ry, lx + LIST_W - 2, ry + CAT_ROW - 1, on ? 0x80E8B84A : 0x40E8B84A);
            boolean wk = i >= cats.size();
            String name = wk ? Component.translatable("rtshop.title.cat_week").getString()
                    : Component.translatable("rtshop.rank.cat." + cats.get(i).get("id").getAsString()).getString();
            g.drawString(font, Ui.clip(font, name, LIST_W - 12), lx + 6, ry + 4, wk ? 0xFFE8A8FF : on ? 0xFFFFF2C8 : Ui.C_CREAM, false);
        }
        int dx = lx + LIST_W + 6, dw = W - LIST_W - 22;
        Ui.bevel(g, dx, ly, dw, lh, Ui.C_PARCH_DARK, Ui.C_PARCH_SHADE, Ui.C_SHADOW);
        List<JsonObject> rows = rows();
        long have = weekSel() || cats.isEmpty() ? 0 : cats.get(sel).get("value").getAsLong();
        String cat = weekSel() || cats.isEmpty() ? "" : cats.get(sel).get("id").getAsString();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - visible())));
        for (int k = 0; k < visible() && scroll + k < rows.size(); k++) {
            JsonObject r = rows.get(scroll + k);
            String id = r.get("id").getAsString();
            boolean open = r.get("open").getAsBoolean(), wk = id.startsWith("week:");
            boolean eff = effects.contains(id), shown = id.equals(show);
            int ry = ly + 3 + k * ROW;
            Ui.bevel(g, dx + 3, ry, dw - 6, ROW - 2, open ? (eff || shown ? Ui.C_PARCH_HI : Ui.C_PARCH) : Ui.C_PARCH_DARK, Ui.C_PARCH_HI, Ui.C_PARCH_SHADE);
            if (eff) g.fill(dx + 3, ry, dx + 5, ry + ROW - 2, 0xFFA02C1E);
            String nm = name(id, r.get("name").getAsString());
            g.drawString(font, Ui.clip(font, nm, dw - 124), dx + 9, ry + 4, open ? (wk ? 0xFF7A2A8A : Ui.C_INK) : Ui.C_INK_SOFT, false);
            String sub;
            if (wk) {
                sub = Component.translatable("rtshop.title.week_sub", Component.translatable("rtshop.rank.cat." + r.get("cat").getAsString())).getString();
            } else {
                long need = r.get("need").getAsLong();
                String ef = effect(r.get("effect").getAsString(), r.get("value").getAsDouble());
                sub = open ? ef : Component.translatable("rtshop.title.locked", RankScreen.value(cat, Math.min(have, need)), RankScreen.value(cat, need)).getString() + "  ·  " + ef;
            }
            Ui.scaledText(g, font, Ui.clip(font, sub, (int) ((dw - 20) / 0.8F)), dx + 9, ry + 17, 0.8F, open ? 0xFF4A6A24 : Ui.C_INK_SOFT, false);
            if (open) {
                int bx = dx + dw - 110;
                if (!wk) {
                    String l1 = Component.translatable(eff ? "rtshop.title.btn_effect_off" : "rtshop.title.btn_effect").getString();
                    Ui.button(g, font, bx, ry + 2, 50, 13, l1, Ui.inside(mx, my, bx, ry + 2, 50, 13), true);
                }
                String l2 = Component.translatable(shown ? "rtshop.title.btn_show_off" : "rtshop.title.btn_show").getString();
                Ui.button(g, font, bx + 54, ry + 2, 50, 13, l2, Ui.inside(mx, my, bx + 54, ry + 2, 50, 13), true);
            }
        }
        if (rows.size() > visible()) {
            int bh = Math.max(12, (lh - 6) * visible() / rows.size()), by = ly + 3 + (lh - 6 - bh) * scroll / Math.max(1, rows.size() - visible());
            g.fill(dx + dw - 3, by, dx + dw - 1, by + bh, Ui.C_PARCH_SHADE);
        }
        super.render(g, mx, my, partial);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        int lx = fx + 8, ly = fy + 41, lh = H - 49;
        if (Ui.inside(mx, my, lx, ly, LIST_W, lh)) {
            int i = (int) ((my - ly - 3) / CAT_ROW);
            if (i >= 0 && i < rowsLeft() && i != sel) {
                sel = i;
                scroll = 0;
                click();
            }
            return true;
        }
        int dx = lx + LIST_W + 6, dw = W - LIST_W - 22;
        List<JsonObject> rows = rows();
        for (int k = 0; k < visible() && scroll + k < rows.size(); k++) {
            JsonObject r = rows.get(scroll + k);
            if (!r.get("open").getAsBoolean()) continue;
            String id = r.get("id").getAsString();
            int ry = ly + 3 + k * ROW, bx = dx + dw - 110;
            if (!id.startsWith("week:") && Ui.inside(mx, my, bx, ry + 2, 50, 13)) {
                click();
                send("rtshop title effect " + id);
                return true;
            }
            if (Ui.inside(mx, my, bx + 54, ry + 2, 50, 13)) {
                click();
                send("rtshop title show " + id);
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scroll = Math.max(0, scroll - (int) Math.signum(delta));
        return true;
    }

    private void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private void send(String cmd) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.connection.sendCommand(cmd);
    }

    // ---- 시험용
    public int catsForTest() {
        return cats.size();
    }

    public void selectForTest(String cat) {
        for (int i = 0; i < cats.size(); i++) if (cats.get(i).get("id").getAsString().equals(cat)) sel = i;
        if (cat.equals(WEEK) && !week.isEmpty()) sel = cats.size();
        scroll = 0;
    }

    public int openForTest() {
        int n = 0;
        for (JsonObject c : cats) for (JsonElement e : c.getAsJsonArray("titles")) if (e.getAsJsonObject().get("open").getAsBoolean()) n++;
        return n;
    }

    public int effectsForTest() {
        return effects.size();
    }

    public String shownForTest() {
        return show;
    }

    public void effectForTest(String id) {
        send("rtshop title effect " + id);
    }

    public void showForTest(String id) {
        send("rtshop title show " + id);
    }

    /** 모든 칭호의 이름과 설명이 줄 안에 들어가는가 (잘리지 않는가). */
    public boolean fitsForTest() {
        int dw = W - LIST_W - 22;
        for (JsonObject c : cats) {
            String cat = c.get("id").getAsString();
            JsonArray ts = c.getAsJsonArray("titles");
            for (JsonElement e : ts) {
                JsonObject r = e.getAsJsonObject();
                String nm = name(r.get("id").getAsString(), r.get("name").getAsString());
                if (font.width(nm) > dw - 124) return false;
                long need = r.get("need").getAsLong();
                String sub = Component.translatable("rtshop.title.locked", RankScreen.value(cat, need), RankScreen.value(cat, need)).getString() + "  ·  "
                        + effect(r.get("effect").getAsString(), r.get("value").getAsDouble());
                if (font.width(sub) * 0.8F > dw - 20) return false;
            }
        }
        return rowsLeft() * CAT_ROW <= H - 53;
    }
}

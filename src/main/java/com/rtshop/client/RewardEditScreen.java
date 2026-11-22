package com.rtshop.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rtshop.net.RaidActionPacket;
import com.rtshop.net.ShopNet;
import com.rtshop.raid.Raid;
import com.rtshop.raid.Reward;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 보상·수치·문구 편집기 (OP). 주간 레이드와 주간 던전 보상이 같은 화면을 쓴다.
 * [수치] [문구]: 줄을 눌러 값을 고친다. [보상]: 묶음을 고르고, 손에 든 아이템을 넣고, 개수·등급·확률을 바꾸거나 뺀다. [저장] 을 눌러야 적용된다.
 */
public class RewardEditScreen extends Screen {
    private static final int W = 430, H = 236, ROW = 18;

    private static final class F {
        String k, label, v;
        boolean num;
    }

    private static final class G {
        String id, label;
        boolean grades, chance;
    }

    private String id;
    private String titleText;
    private final List<F> fields = new ArrayList<>();
    private final List<G> groups = new ArrayList<>();
    private final List<Reward> rewards = new ArrayList<>();
    private int tab = 0;
    private int group = 0;
    private int addGrade = 0;
    private int scroll = 0;
    private EditBox box;
    private F editing;
    private boolean dirty = false;

    public RewardEditScreen(JsonObject model) {
        super(Component.literal("editor"));
        read(model);
    }

    public String id() {
        return id;
    }

    public void update(JsonObject model) {
        stopEdit();
        read(model);
        dirty = false;
    }

    private void read(JsonObject m) {
        id = m.get("id").getAsString();
        titleText = m.get("title").getAsString();
        fields.clear();
        groups.clear();
        rewards.clear();
        for (JsonElement e : m.getAsJsonArray("fields")) {
            JsonObject o = e.getAsJsonObject();
            F f = new F();
            f.k = o.get("k").getAsString();
            f.label = o.get("label").getAsString();
            f.v = o.get("v").getAsString();
            f.num = o.get("num").getAsBoolean();
            fields.add(f);
        }
        for (JsonElement e : m.getAsJsonArray("groups")) {
            JsonObject o = e.getAsJsonObject();
            G g = new G();
            g.id = o.get("id").getAsString();
            g.label = o.get("label").getAsString();
            g.grades = o.get("grades").getAsBoolean();
            g.chance = o.get("chance").getAsBoolean();
            groups.add(g);
        }
        for (JsonElement e : m.getAsJsonArray("rewards")) {
            rewards.add(Reward.parse(e.getAsJsonObject()));
        }
        group = Math.max(0, Math.min(group, groups.size() - 1));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int fx() {
        return (this.width - W) / 2;
    }

    private int fy() {
        return (this.height - H) / 2;
    }

    private int listY() {
        return fy() + (tab == 2 ? 66 : 46);
    }

    private int listH() {
        return fy() + H - 34 - listY();
    }

    private List<F> shown() {
        List<F> out = new ArrayList<>();
        for (F f : fields) {
            if (f.num == (tab == 0)) out.add(f);
        }
        return out;
    }

    private List<Reward> shownRewards() {
        List<Reward> out = new ArrayList<>();
        if (groups.isEmpty()) return out;
        for (Reward r : rewards) {
            if (r.group.equals(groups.get(group).id)) out.add(r);
        }
        return out;
    }

    private int count() {
        return tab == 2 ? shownRewards().size() : shown().size();
    }

    private int maxScroll() {
        return Math.max(0, count() * ROW - listH());
    }

    private int valueX() {
        return fx() + (tab == 0 ? 290 : 130);
    }

    private int valueW() {
        return fx() + W - 14 - valueX();
    }

    private void stopEdit() {
        if (box != null) {
            if (editing != null) editing.v = box.getValue();
            this.removeWidget(box);
            box = null;
            editing = null;
        }
    }

    private void startEdit(F f, int y) {
        stopEdit();
        editing = f;
        box = new EditBox(this.font, valueX() + 3, y + 5, valueW() - 6, 10, Component.literal(f.label));
        box.setBordered(false);
        box.setMaxLength(f.num ? 12 : 200);
        box.setTextColor(0xFFF5E6C0);
        if (f.num) box.setFilter(s -> s.matches("[0-9.]*"));
        box.setValue(f.v);
        box.setResponder(s -> {
            if (editing != null && !s.equals(editing.v)) {
                editing.v = s;
                dirty = true;
            }
        });
        this.addRenderableWidget(box);
        this.setFocused(box);
        box.setFocused(true);
    }

    private static String pct(double v) {
        return (Math.abs(v - Math.rint(v)) < 1e-9 ? Long.toString((long) Math.rint(v)) : String.format(java.util.Locale.ROOT, "%.2f", v)) + "%";
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        int x = fx(), y = fy();
        g.fill(0, 0, this.width, this.height, 0x88000000);
        Ui.woodFrame(g, x, y, W, H);
        g.drawString(this.font, titleText + (dirty ? " *" : ""), x + 12, y + 9, Ui.C_GOLD, true);
        String[] tabs = {"rtshop.redit.tab_num", "rtshop.redit.tab_text", "rtshop.redit.tab_rewards"};
        for (int i = 0; i < 3; i++) {
            int tx = x + 12 + i * 74;
            Ui.bevel(g, tx, y + 24, 70, 16, tab == i ? Ui.C_PARCH : Ui.C_WOOD_DARK, tab == i ? Ui.C_PARCH_HI : Ui.C_WOOD_HI, Ui.C_SHADOW);
            g.drawCenteredString(this.font, Component.translatable(tabs[i]), tx + 35, y + 28, tab == i ? 0xFF3A2814 : Ui.C_CREAM);
        }
        int ly = listY(), lh = listH();
        g.fill(x + 10, ly - 2, x + W - 10, ly + lh + 2, Ui.C_PARCH);
        if (tab == 2) {
            for (int i = 0; i < groups.size(); i++) {
                int[] b = groupBtn(i);
                Ui.button(g, this.font, b[0], b[1], b[2], b[3], groups.get(i).label, Ui.inside(mx, my, b[0], b[1], b[2], b[3]), i != group);
            }
        }
        g.enableScissor(x + 10, ly - 1, x + W - 10, ly + lh + 1);
        int ry = ly - scroll;
        if (tab != 2) {
            for (F f : shown()) {
                if (ry > ly - ROW && ry < ly + lh) {
                    g.drawString(this.font, Ui.clip(this.font, f.label, valueX() - x - 20), x + 14, ry + 5, Ui.C_INK, false);
                    g.fill(valueX(), ry + 1, valueX() + valueW(), ry + ROW - 1, 0xFF3E2512);
                    if (f != editing) {
                        g.drawString(this.font, Ui.clip(this.font, f.v, valueW() - 6), valueX() + 3, ry + 5, 0xFFF5E6C0, false);
                    }
                }
                ry += ROW;
            }
        } else if (!groups.isEmpty()) {
            G gr = groups.get(group);
            for (Reward r : shownRewards()) {
                if (ry > ly - ROW && ry < ly + lh) {
                    ItemStack s = r.stack();
                    if (!s.isEmpty()) g.renderItem(s, x + 14, ry + 1);
                    String name = s.isEmpty() ? r.item + " (?)" : s.getHoverName().getString();
                    g.drawString(this.font, Ui.clip(this.font, name, 150), x + 34, ry + 5, s.isEmpty() ? Ui.C_RED : Ui.C_INK, false);
                    int bx = x + 190;
                    Ui.button(g, this.font, bx, ry + 2, 14, 14, "-", false, true);
                    g.drawCenteredString(this.font, "×" + r.count, bx + 34, ry + 5, 0xFF3A2814);
                    Ui.button(g, this.font, bx + 54, ry + 2, 14, 14, "+", false, true);
                    if (gr.grades) {
                        g.fill(bx + 76, ry + 2, bx + 136, ry + 16, 0xFF3E2512);
                        g.drawCenteredString(this.font, RaidScreen.gradeName(r.grade), bx + 106, ry + 5, r.grade == 5 ? 0xFF9A9A9A : RaidScreen.GRADE_COLORS[r.grade]);
                    }
                    if (gr.chance) {
                        Ui.button(g, this.font, bx + 76, ry + 2, 14, 14, "-", false, true);
                        g.drawCenteredString(this.font, pct(r.chance), bx + 113, ry + 5, 0xFF3A2814);
                        Ui.button(g, this.font, bx + 136, ry + 2, 14, 14, "+", false, true);
                    }
                    Ui.button(g, this.font, x + W - 34, ry + 2, 18, 14, "✖", false, true);
                }
                ry += ROW;
            }
            if (shownRewards().isEmpty()) {
                g.drawCenteredString(this.font, Component.translatable("rtshop.redit.empty"), x + W / 2, ly + lh / 2 - 4, Ui.C_INK_SOFT);
            }
        }
        g.disableScissor();
        int by = y + H - 26;
        if (tab == 2 && !groups.isEmpty()) {
            if (groups.get(group).grades) {
                Ui.button(g, this.font, x + 12, by, 84, 18, RaidScreen.gradeName(addGrade) + " ▼", Ui.inside(mx, my, x + 12, by, 84, 18), true);
            }
            Ui.button(g, this.font, x + 100, by, 130, 18, Component.translatable("rtshop.redit.add_held").getString(), Ui.inside(mx, my, x + 100, by, 130, 18), true);
        } else {
            g.drawString(this.font, Component.translatable("rtshop.redit.hint"), x + 14, by + 5, Ui.C_CREAM, false);
        }
        Ui.button(g, this.font, x + W - 150, by, 66, 18, Component.translatable("rtshop.redit.save").getString(), Ui.inside(mx, my, x + W - 150, by, 66, 18), true);
        Ui.button(g, this.font, x + W - 80, by, 66, 18, Component.translatable("gui.done").getString(), Ui.inside(mx, my, x + W - 80, by, 66, 18), true);
        super.render(g, mx, my, partial);
    }

    private int[] groupBtn(int i) {
        int n = Math.max(1, groups.size());
        int w = Math.min(200, (W - 24 - (n - 1) * 4) / n);
        return new int[]{fx() + 12 + i * (w + 4), fy() + 44, w, 16};
    }

    private JsonObject state() {
        stopEdit();
        JsonObject o = new JsonObject();
        o.addProperty("id", id);
        JsonObject f = new JsonObject();
        for (F x : fields) f.addProperty(x.k, x.v);
        o.add("fields", f);
        JsonArray a = new JsonArray();
        for (Reward r : rewards) a.add(r.toJson());
        o.add("rewards", a);
        return o;
    }

    public void save() {
        ShopNet.CHANNEL.sendToServer(new RaidActionPacket(Raid.ACT_EDIT_SAVE, state().toString()));
    }

    public void addHeld() {
        if (groups.isEmpty()) return;
        JsonObject o = state();
        o.addProperty("addGroup", groups.get(group).id);
        o.addProperty("addGrade", groups.get(group).grades ? addGrade : 0);
        ShopNet.CHANNEL.sendToServer(new RaidActionPacket(Raid.ACT_EDIT_ADD, o.toString()));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int x = fx(), y = fy();
        for (int i = 0; i < 3; i++) {
            if (Ui.inside(mx, my, x + 12 + i * 74, y + 24, 70, 16)) {
                stopEdit();
                tab = i;
                scroll = 0;
                return true;
            }
        }
        int by = y + H - 26;
        if (Ui.inside(mx, my, x + W - 150, by, 66, 18)) {
            save();
            return true;
        }
        if (Ui.inside(mx, my, x + W - 80, by, 66, 18)) {
            this.onClose();
            return true;
        }
        int ly = listY(), lh = listH();
        boolean shift = hasShiftDown();
        if (tab == 2 && !groups.isEmpty()) {
            for (int i = 0; i < groups.size(); i++) {
                int[] b = groupBtn(i);
                if (Ui.inside(mx, my, b[0], b[1], b[2], b[3])) {
                    group = i;
                    scroll = 0;
                    return true;
                }
            }
            G gr = groups.get(group);
            if (gr.grades && Ui.inside(mx, my, x + 12, by, 84, 18)) {
                addGrade = (addGrade + (button == 1 ? 5 : 1)) % 6;
                return true;
            }
            if (Ui.inside(mx, my, x + 100, by, 130, 18)) {
                addHeld();
                return true;
            }
            if (my >= ly && my < ly + lh) {
                int idx = (int) ((my - ly + scroll) / ROW);
                List<Reward> list = shownRewards();
                if (idx >= 0 && idx < list.size()) {
                    Reward r = list.get(idx);
                    int ry = ly - scroll + idx * ROW;
                    int bx = x + 190;
                    int step = shift ? 10 : 1;
                    if (Ui.inside(mx, my, bx, ry + 2, 14, 14)) {
                        r.count = Math.max(1, r.count - step);
                    } else if (Ui.inside(mx, my, bx + 54, ry + 2, 14, 14)) {
                        r.count = Math.min(6400, r.count + step);
                    } else if (gr.grades && Ui.inside(mx, my, bx + 76, ry + 2, 60, 14)) {
                        r.grade = (r.grade + (button == 1 ? 5 : 1)) % 6;
                    } else if (gr.chance && Ui.inside(mx, my, bx + 76, ry + 2, 14, 14)) {
                        r.chance = Math.max(0, r.chance - (shift ? 10 : r.chance <= 1 ? 0.05 : 1));
                    } else if (gr.chance && Ui.inside(mx, my, bx + 136, ry + 2, 14, 14)) {
                        r.chance = Math.min(100, r.chance + (shift ? 10 : r.chance < 1 ? 0.05 : 1));
                    } else if (Ui.inside(mx, my, x + W - 34, ry + 2, 18, 14)) {
                        rewards.remove(r);
                    } else {
                        return true;
                    }
                    r.chance = Math.round(r.chance * 100.0) / 100.0;
                    dirty = true;
                    return true;
                }
            }
            return true;
        }
        if (tab != 2 && my >= ly && my < ly + lh && mx >= valueX() && mx < valueX() + valueW()) {
            int idx = (int) ((my - ly + scroll) / ROW);
            List<F> list = shown();
            if (idx >= 0 && idx < list.size()) {
                if (box != null && editing == list.get(idx)) {
                    return super.mouseClicked(mx, my, button);
                }
                startEdit(list.get(idx), ly - scroll + idx * ROW);
                return true;
            }
        }
        stopEdit();
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        stopEdit();
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) (delta * ROW * 2)));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (box != null && (key == 257 || key == 335)) {
            stopEdit();
            return true;
        }
        if (box != null && key != 256 && box.keyPressed(key, scan, mods)) {
            return true;
        }
        if (box != null && key != 256 && box.canConsumeInput()) {
            return true; // 글자를 치는 동안 인벤토리 키(E)로 창이 닫히지 않게
        }
        return super.keyPressed(key, scan, mods);
    }

    // ------------------------------------------------------------------ 점검용

    public void tabForTest(int t, int g) {
        stopEdit();
        tab = t;
        group = Math.max(0, Math.min(g, groups.size() - 1));
        scroll = 0;
    }

    public int fieldsForTest(boolean num) {
        int n = 0;
        for (F f : fields) if (f.num == num) n++;
        return n;
    }

    public int rewardsForTest() {
        return rewards.size();
    }

    public void setFieldForTest(String key, String value) {
        for (F f : fields) {
            if (f.k.equals(key)) {
                f.v = value;
                dirty = true;
            }
        }
    }
}

package com.rtshop.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rtshop.net.RulesActionPacket;
import com.rtshop.net.ShopNet;
import com.rtshop.rules.RulesEditor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * OP 전리품 편집기 (/rtshop loot): 왼쪽에서 상자 전리품표를 고르면, 서버가 400번 열어 본 확률(기본 / 적용 후)이 나온다.
 * 아이템마다 [기본] → [끔] → [직접 %] 로 바꾸고, [+ 아이템] 으로 새로 넣는다. [전부 지우기] 는 원래 나오던 것을 모두 없앤다.
 */
public class LootEditScreen extends Screen {
    static final class Row {
        String item;
        double rawChance, rawAvg, afterChance;
        int rawMin, rawMax;
        boolean natural;
        int mode; // 0 기본, 1 끔, 2 직접
        double chance = 10;
        int min = 1, max = 1;
    }

    private final List<String> tables = new ArrayList<>();
    private final Set<String> ruled = new LinkedHashSet<>();
    private final List<String> shownTables = new ArrayList<>();
    private String tableSearch = "";
    private int tableScroll;
    private String selected = "";
    private final List<Row> rows = new ArrayList<>();
    private boolean clear;
    private final List<String> groups = new ArrayList<>();
    private boolean loading, dirty;
    private int samples = 400;
    private int scroll;
    private int fx, fy, W, H, lw, rx, listY, visible, tVisible;
    private static final int ROW = 20;
    private final List<AbstractWidget> rowWidgets = new ArrayList<>();
    private final List<Object[]> drawn = new ArrayList<>();

    public LootEditScreen(JsonObject o) {
        super(Component.translatable("rtshop.rules.loot_title"));
        for (JsonElement e : o.getAsJsonArray("tables")) tables.add(e.getAsString());
        for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("rules").getAsJsonObject("tables").entrySet()) ruled.add(e.getKey());
    }

    void applyTable(JsonObject o) {
        String t = o.get("table").getAsString();
        if (!t.equals(selected)) return;
        loading = false;
        samples = o.get("samples").getAsInt();
        rows.clear();
        Map<String, Row> by = new LinkedHashMap<>();
        for (JsonElement e : o.getAsJsonArray("raw")) {
            JsonObject x = e.getAsJsonObject();
            Row r = new Row();
            r.item = x.get("item").getAsString();
            r.rawChance = x.get("chance").getAsDouble();
            r.rawAvg = x.get("avg").getAsDouble();
            r.rawMin = x.get("min").getAsInt();
            r.rawMax = x.get("max").getAsInt();
            r.natural = true;
            r.min = r.rawMin;
            r.max = r.rawMax;
            r.chance = Math.round(r.rawChance * 1000) / 10.0;
            by.put(r.item, r);
        }
        for (JsonElement e : o.getAsJsonArray("after")) {
            JsonObject x = e.getAsJsonObject();
            Row r = by.computeIfAbsent(x.get("item").getAsString(), k -> {
                Row n = new Row();
                n.item = k;
                return n;
            });
            r.afterChance = x.get("chance").getAsDouble();
        }
        JsonObject rule = o.getAsJsonObject("rule");
        clear = rule.has("clear") && rule.get("clear").getAsBoolean();
        if (rule.has("remove")) {
            for (JsonElement e : rule.getAsJsonArray("remove")) {
                Row r = by.computeIfAbsent(e.getAsString(), k -> {
                    Row n = new Row();
                    n.item = k;
                    return n;
                });
                r.mode = 1;
            }
        }
        if (rule.has("add")) {
            for (JsonElement e : rule.getAsJsonArray("add")) {
                JsonObject x = e.getAsJsonObject();
                Row r = by.computeIfAbsent(x.get("item").getAsString(), k -> {
                    Row n = new Row();
                    n.item = k;
                    return n;
                });
                r.mode = 2;
                r.chance = Math.round(x.get("chance").getAsDouble() * 1000) / 10.0;
                r.min = x.get("min").getAsInt();
                r.max = x.get("max").getAsInt();
            }
        }
        rows.addAll(by.values());
        rows.sort((a, b) -> Double.compare(Math.max(b.rawChance, b.mode == 2 ? b.chance / 100 : 0), Math.max(a.rawChance, a.mode == 2 ? a.chance / 100 : 0)));
        groups.clear();
        for (JsonElement e : o.getAsJsonArray("groups")) groups.add(e.getAsString());
        if (o.has("saved")) {
            dirty = false;
            if (ruleJson() == null) ruled.remove(selected);
            else ruled.add(selected);
        }
        if (minecraft != null && minecraft.screen == this) rebuildWidgets();
    }

    /** 지금 상태를 규칙으로 (바꾼 것이 없으면 null). */
    JsonObject ruleJson() {
        JsonObject o = new JsonObject();
        JsonArray rem = new JsonArray();
        JsonArray add = new JsonArray();
        for (Row r : rows) {
            if (r.mode == 1 || (r.mode == 2 && r.natural)) rem.add(r.item);
            if (r.mode == 2) {
                JsonObject a = new JsonObject();
                a.addProperty("item", r.item);
                a.addProperty("chance", Math.max(0, Math.min(100, r.chance)) / 100.0);
                a.addProperty("min", Math.max(1, r.min));
                a.addProperty("max", Math.max(Math.max(1, r.min), r.max));
                add.add(a);
            }
        }
        if (!clear && rem.isEmpty() && add.isEmpty()) return null;
        if (clear) o.addProperty("clear", true);
        o.add("remove", rem);
        o.add("add", add);
        return o;
    }

    private void select(String t) {
        selected = t;
        rows.clear();
        loading = true;
        dirty = false;
        scroll = 0;
        ShopNet.CHANNEL.sendToServer(new RulesActionPacket(RulesEditor.LOOT_SAMPLE, t, ""));
        rebuildWidgets();
    }

    private void filterTables() {
        shownTables.clear();
        String q = tableSearch.toLowerCase(Locale.ROOT).trim();
        for (String t : tables) if (q.isEmpty() || t.contains(q)) shownTables.add(t);
    }

    @Override
    protected void init() {
        W = Math.min(500, width - 10);
        H = Math.min(280, height - 10);
        fx = (width - W) / 2;
        fy = (height - H) / 2;
        lw = Math.min(130, W / 4);
        rx = fx + lw + 12;
        filterTables();
        EditBox sb = new EditBox(font, fx + 8, fy + 26, lw - 2, 14, Component.empty());
        sb.setValue(tableSearch);
        sb.setHint(Component.translatable("rtshop.rules.search_table"));
        sb.setResponder(v -> {
            if (!v.equals(tableSearch)) {
                tableSearch = v;
                tableScroll = 0;
                rebuildWidgets();
            }
        });
        addRenderableWidget(sb);
        tVisible = (fy + H - 10 - (fy + 44)) / 14;
        tableScroll = Math.max(0, Math.min(tableScroll, Math.max(0, shownTables.size() - tVisible)));
        for (int i = 0; i < tVisible && i + tableScroll < shownTables.size(); i++) {
            String t = shownTables.get(i + tableScroll);
            String label = (ruled.contains(t) ? "● " : "") + t.replace("minecraft:chests/", "").replace(":chests/", ": ");
            Button b = Button.builder(Component.literal(Ui.clip(font, label, lw - 8)), x -> select(t)).bounds(fx + 8, fy + 44 + i * 14, lw - 2, 13).build();
            b.active = !t.equals(selected);
            addRenderableWidget(b);
        }
        listY = fy + 60;
        visible = (fy + H - 30 - listY) / ROW;
        rebuildRows();
    }

    private void rebuildRows() {
        for (AbstractWidget w : rowWidgets) removeWidget(w);
        rowWidgets.clear();
        drawn.clear();
        if (selected.isEmpty()) return;
        add(Button.builder(Component.translatable(clear ? "rtshop.rules.clear_on" : "rtshop.rules.clear_off"), b -> {
            clear = !clear;
            dirty = true;
            rebuildRows();
        }).bounds(rx, fy + 40, 104, 16).build());
        add(Button.builder(Component.translatable("rtshop.rules.add_item"), b -> minecraft.setScreen(new PickerScreen(this, PickerScreen.Mode.ITEM, v -> {
            Row r = rows.stream().filter(q -> q.item.equals(v)).findFirst().orElse(null);
            if (r == null) {
                r = new Row();
                r.item = v;
                rows.add(0, r);
            }
            r.mode = 2;
            dirty = true;
        }))).bounds(rx + 108, fy + 40, 90, 16).build());
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - visible)));
        int right = fx + W - 8;
        for (int i = 0; i < visible && i + scroll < rows.size(); i++) {
            Row r = rows.get(i + scroll);
            int y = listY + i * ROW;
            drawn.add(new Object[]{r, y});
            String[] keys = {"rtshop.rules.mode_default", "rtshop.rules.mode_off", "rtshop.rules.mode_custom"};
            add(Button.builder(Component.translatable(keys[r.mode]), b -> {
                r.mode = (r.mode + 1) % 3;
                if (!r.natural && r.mode == 0) r.mode = 1;
                dirty = true;
                rebuildRows();
            }).bounds(right - 152, y, 44, 18).build());
            if (r.mode == 2) {
                add(num(right - 104, y + 2, 38, fmt(r.chance), v -> r.chance = Double.parseDouble(v)));
                add(num(right - 50, y + 2, 20, Integer.toString(r.min), v -> r.min = Integer.parseInt(v)));
                add(num(right - 24, y + 2, 22, Integer.toString(r.max), v -> r.max = Integer.parseInt(v)));
            }
        }
        int by = fy + H - 24;
        add(Button.builder(Component.translatable("rtshop.enhedit.save"), b -> save()).bounds(fx + W - 248, by, 80, 18).build());
        add(Button.builder(Component.translatable("rtshop.enhedit.revert"), b -> select(selected)).bounds(fx + W - 164, by, 76, 18).build());
        add(Button.builder(Component.translatable("rtshop.enhodds.close"), b -> onClose()).bounds(fx + W - 84, by, 76, 18).build());
    }

    private static String fmt(double v) {
        return v == Math.floor(v) ? Long.toString((long) v) : Double.toString(v);
    }

    void save() {
        JsonObject r = ruleJson();
        loading = true;
        ShopNet.CHANNEL.sendToServer(new RulesActionPacket(RulesEditor.LOOT_SAVE, selected, r == null ? "" : r.toString()));
    }

    private EditBox num(int x, int y, int w, String value, java.util.function.Consumer<String> set) {
        EditBox b = new EditBox(font, x, y, w, 14, Component.empty());
        b.setMaxLength(6);
        b.setValue(value);
        b.setFilter(s -> s.isEmpty() || s.matches("[0-9]*\\.?[0-9]*"));
        b.setResponder(v -> {
            try {
                if (!v.isEmpty()) {
                    set.accept(v);
                    dirty = true;
                }
            } catch (NumberFormatException ignored) {
            }
        });
        return b;
    }

    private void add(AbstractWidget w) {
        addRenderableWidget(w);
        rowWidgets.add(w);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (mx < rx) {
            tableScroll = Math.max(0, tableScroll - (int) Math.signum(delta) * 3);
            rebuildWidgets();
        } else {
            scroll = Math.max(0, scroll - (int) Math.signum(delta) * 2);
            rebuildRows();
        }
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        Ui.woodFrame(g, fx, fy, W, H);
        Ui.bevel(g, rx - 4, fy + 58, fx + W - rx - 2, H - 86, Ui.C_PARCH, Ui.C_PARCH_HI, Ui.C_PARCH_SHADE);
        g.drawString(font, title, fx + 10, fy + 8, Ui.C_GOLD, true);
        int tx = fx + 14 + font.width(title);
        if (dirty) {
            g.drawString(font, Component.translatable("rtshop.enhedit.unsaved"), tx, fy + 8, 0xFFFF9050, true);
            tx += 4 + font.width(Component.translatable("rtshop.enhedit.unsaved"));
        }
        if (!selected.isEmpty()) g.drawString(font, Ui.clip(font, "· " + selected, fx + W - 10 - tx), tx, fy + 8, Ui.C_CREAM, true);
        if (selected.isEmpty()) {
            g.drawString(font, Component.translatable("rtshop.rules.pick_table"), rx + 4, fy + 70, Ui.C_CREAM, false);
        } else {
            if (loading) {
                g.drawString(font, Component.translatable("rtshop.rules.sampling", samples), rx + 4, fy + 70, Ui.C_INK, false);
            }
            int right = fx + W - 8;
            for (Object[] d : drawn) {
                Row r = (Row) d[0];
                int y = (int) d[1];
                if (r.mode == 1) g.fill(rx - 2, y - 1, right, y + ROW - 2, 0x30FF2020);
                if (r.mode == 2) g.fill(rx - 2, y - 1, right, y + ROW - 2, 0x2020A020);
                g.renderItem(new ItemStack(RulesScreens.item(r.item)), rx, y + 1);
                int textW = right - 156 - (rx + 20);
                g.drawString(font, Ui.clip(font, RulesScreens.name(r.item), textW), rx + 20, y + 1, Ui.C_INK, false);
                String base = r.natural ? Component.translatable("rtshop.rules.stat_base", pct(r.rawChance),
                        r.rawMin == r.rawMax ? Integer.toString(r.rawMin) : r.rawMin + "~" + r.rawMax).getString()
                        : Component.translatable("rtshop.rules.stat_none").getString();
                String now = Component.translatable("rtshop.rules.stat_now", pct(r.afterChance)).getString();
                Ui.scaledText(g, font, Ui.clip(font, base + " · " + now, (int) (textW / 0.75F)), rx + 20, y + 11, 0.75F,
                        r.afterChance > 0 ? Ui.C_GREEN : Ui.C_INK_SOFT, false);
                if (r.mode == 2) {
                    g.drawString(font, "%", right - 64, y + 5, Ui.C_INK, false);
                    g.drawString(font, "~", right - 29, y + 5, Ui.C_INK, false);
                }
            }
            String foot = Component.translatable("rtshop.rules.loot_foot", samples).getString();
            if (!groups.isEmpty()) foot += "  " + Component.translatable("rtshop.rules.loot_groups", String.join(", ", groups)).getString();
            Ui.scaledText(g, font, Ui.clip(font, foot, (int) ((fx + W - 252 - (rx - 4)) / 0.75F)), rx - 4, fy + H - 17, 0.75F, Ui.C_CREAM, false);
        }
        super.render(g, mx, my, partial);
    }

    private static String pct(double v) {
        double p = v * 100;
        return (p >= 10 || p == 0 ? Long.toString(Math.round(p)) : String.format(Locale.ROOT, "%.1f", p)) + "%";
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // 시험용
    public int tablesForTest() {
        return tables.size();
    }

    public void selectForTest(String t) {
        select(t);
    }

    public boolean loadedForTest() {
        return !loading && !rows.isEmpty();
    }

    public int rowsForTest() {
        return rows.size();
    }

    public void setForTest(String item, int mode, double chance, int min, int max) {
        Row r = rows.stream().filter(q -> q.item.equals(item)).findFirst().orElse(null);
        if (r == null) {
            r = new Row();
            r.item = item;
            rows.add(0, r);
        }
        r.mode = mode;
        r.chance = chance;
        r.min = min;
        r.max = max;
        dirty = true;
        rebuildRows();
    }

    public void saveForTest() {
        save();
    }

    public double afterForTest(String item) {
        return rows.stream().filter(q -> q.item.equals(item)).mapToDouble(q -> q.afterChance).findFirst().orElse(-1);
    }
}

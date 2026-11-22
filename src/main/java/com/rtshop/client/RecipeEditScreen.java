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
 * OP 조합법 편집기 (/rtshop recipes): 조합법을 찾아 막거나 풀고, 직접 조합법을 만든다.
 *  - [막기] 는 그 조합법 하나, [아이템] 은 그 아이템을 만드는 조합법 전부(item:...).
 *  - [+ 새 조합법] 으로 3×3 조합법을 만들 수 있다 (원래 조합법을 막고 새로 만들면 '조절').
 */
public class RecipeEditScreen extends Screen {
    record Row(String id, String type, String out, int count, boolean custom) {
    }

    private final List<Row> all = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();
    final Set<String> disabled = new LinkedHashSet<>();
    final Map<String, JsonObject> custom = new LinkedHashMap<>();
    private final List<String> errors = new ArrayList<>();
    private String search = "";
    private int view = 0; // 0 전체, 1 막힌 것, 2 직접 만든 것
    private int scroll;
    boolean dirty;
    private int fx, fy, W, H, listY, visible;
    private static final int ROW = 21;
    private final List<AbstractWidget> rowWidgets = new ArrayList<>();
    private final List<Object[]> drawn = new ArrayList<>();

    public RecipeEditScreen(JsonObject o) {
        super(Component.translatable("rtshop.rules.recipes_title"));
        apply(o);
    }

    void apply(JsonObject o) {
        all.clear();
        disabled.clear();
        custom.clear();
        errors.clear();
        JsonObject rules = o.getAsJsonObject("rules");
        for (JsonElement e : rules.getAsJsonArray("disabled")) disabled.add(e.getAsString());
        for (JsonElement e : rules.getAsJsonArray("custom")) {
            JsonObject x = e.getAsJsonObject();
            custom.put(x.get("id").getAsString(), x.getAsJsonObject("recipe"));
        }
        for (JsonElement e : o.getAsJsonArray("recipes")) {
            JsonObject x = e.getAsJsonObject();
            String id = x.get("id").getAsString();
            all.add(new Row(id, x.get("type").getAsString(), x.get("out").getAsString(), x.get("count").getAsInt(), custom.containsKey(id)));
        }
        if (o.has("errors")) for (JsonElement e : o.getAsJsonArray("errors")) errors.add(e.getAsString());
        dirty = false;
        if (minecraft != null) {
            if (minecraft.screen != this && minecraft.screen instanceof RecipeMakeScreen) minecraft.setScreen(this);
            else rebuildWidgets();
        }
    }

    /** 새로 만든(또는 고친) 조합법을 목록에 넣는다 (저장은 아직). */
    void putCustom(String id, JsonObject recipe, boolean blockOriginal, String out) {
        custom.put(id, recipe);
        all.removeIf(r -> r.id.equals(id) && r.custom);
        boolean exists = all.stream().anyMatch(r -> r.id.equals(id));
        JsonObject res = recipe.getAsJsonObject("result");
        if (!exists) all.add(0, new Row(id, "custom", res.get("item").getAsString(), res.has("count") ? res.get("count").getAsInt() : 1, true));
        if (blockOriginal && !out.isEmpty()) disabled.add("item:" + out);
        dirty = true;
        view = 2;
        search = "";
    }

    JsonObject rulesJson() {
        JsonObject o = new JsonObject();
        JsonArray d = new JsonArray();
        disabled.forEach(d::add);
        o.add("disabled", d);
        JsonArray c = new JsonArray();
        for (Map.Entry<String, JsonObject> e : custom.entrySet()) {
            JsonObject x = new JsonObject();
            x.addProperty("id", e.getKey());
            x.add("recipe", e.getValue());
            c.add(x);
        }
        o.add("custom", c);
        return o;
    }

    boolean blocked(Row r) {
        return disabled.contains(r.id) || (!r.custom && (disabled.contains("item:" + r.out) || disabled.contains("mod:" + r.id.split(":")[0])));
    }

    private void filter() {
        rows.clear();
        String q = search.toLowerCase(Locale.ROOT).trim();
        for (Row r : all) {
            if (view == 1 && !blocked(r)) continue;
            if (view == 2 && !custom.containsKey(r.id)) continue;
            if (!q.isEmpty()) {
                String name = RulesScreens.name(r.out).toLowerCase(Locale.ROOT);
                if (!r.id.contains(q) && !r.out.contains(q) && !name.contains(q)) continue;
            }
            rows.add(r);
        }
    }

    @Override
    protected void init() {
        W = Math.min(480, width - 10);
        H = Math.min(280, height - 10);
        fx = (width - W) / 2;
        fy = (height - H) / 2;
        filter();
        EditBox sb = new EditBox(font, fx + 10, fy + 26, 150, 14, Component.empty());
        sb.setValue(search);
        sb.setHint(Component.translatable("rtshop.rules.search_recipe"));
        sb.setResponder(v -> {
            if (!v.equals(search)) {
                search = v;
                scroll = 0;
                filter();
                rebuildRows();
            }
        });
        addRenderableWidget(sb);
        String[] views = {"rtshop.rules.view_all", "rtshop.rules.view_blocked", "rtshop.rules.view_custom"};
        int vx = fx + 166;
        for (int i = 0; i < 3; i++) {
            int k = i;
            Button b = Button.builder(Component.translatable(views[i]), x -> {
                view = k;
                scroll = 0;
                rebuildWidgets();
            }).bounds(vx, fy + 25, 66, 16).build();
            b.active = view != i;
            addRenderableWidget(b);
            vx += 68;
        }
        addRenderableWidget(Button.builder(Component.translatable("rtshop.rules.new_recipe"), b -> minecraft.setScreen(new RecipeMakeScreen(this, null, null)))
                .bounds(fx + W - 92, fy + 25, 84, 16).build());
        listY = fy + 46;
        visible = (fy + H - 30 - listY) / ROW;
        rebuildRows();
    }

    private void rebuildRows() {
        for (AbstractWidget w : rowWidgets) removeWidget(w);
        rowWidgets.clear();
        drawn.clear();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - visible)));
        int x = fx + 10;
        for (int i = 0; i < visible && i + scroll < rows.size(); i++) {
            Row r = rows.get(i + scroll);
            int y = listY + i * ROW;
            drawn.add(new Object[]{r, y});
            int bx = fx + W - 10;
            if (custom.containsKey(r.id)) {
                bx -= 18;
                add(Button.builder(Component.literal("✕"), b -> {
                    custom.remove(r.id);
                    all.removeIf(q -> q.id.equals(r.id) && q.custom);
                    disabled.remove(r.id);
                    dirty = true;
                    filter();
                    rebuildRows();
                }).bounds(bx, y, 18, 18).build());
                bx -= 40;
                add(Button.builder(Component.translatable("rtshop.rules.edit"), b -> minecraft.setScreen(new RecipeMakeScreen(this, r.id, custom.get(r.id))))
                        .bounds(bx, y, 38, 18).build());
            }
            if (!r.out.isEmpty() && !custom.containsKey(r.id)) {
                boolean itemOff = disabled.contains("item:" + r.out);
                bx -= 50;
                Button ib = Button.builder(Component.translatable(itemOff ? "rtshop.rules.item_on" : "rtshop.rules.item_off"), b -> {
                    if (!disabled.remove("item:" + r.out)) disabled.add("item:" + r.out);
                    dirty = true;
                    rebuildRows();
                }).bounds(bx, y, 48, 18).build();
                ib.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("rtshop.rules.item_tip", RulesScreens.name(r.out))));
                add(ib);
            }
            boolean off = disabled.contains(r.id);
            bx -= 46;
            add(Button.builder(Component.translatable(off ? "rtshop.rules.unblock" : "rtshop.rules.block"), b -> {
                if (!disabled.remove(r.id)) disabled.add(r.id);
                dirty = true;
                rebuildRows();
            }).bounds(bx, y, 44, 18).build());
        }
        int by = fy + H - 24;
        add(Button.builder(Component.translatable("rtshop.enhedit.save"), b ->
                ShopNet.CHANNEL.sendToServer(new RulesActionPacket(RulesEditor.RECIPE_SAVE, "", rulesJson().toString()))).bounds(fx + W - 248, by, 80, 18).build());
        add(Button.builder(Component.translatable("rtshop.enhedit.revert"), b ->
                ShopNet.CHANNEL.sendToServer(new RulesActionPacket(RulesEditor.RECIPE_OPEN, "", ""))).bounds(fx + W - 164, by, 76, 18).build());
        add(Button.builder(Component.translatable("rtshop.enhodds.close"), b -> onClose()).bounds(fx + W - 84, by, 76, 18).build());
    }

    private void add(AbstractWidget w) {
        addRenderableWidget(w);
        rowWidgets.add(w);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scroll = Math.max(0, scroll - (int) Math.signum(delta) * 3);
        rebuildRows();
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        Ui.woodFrame(g, fx, fy, W, H);
        Ui.bevel(g, fx + 6, fy + 43, W - 12, H - 70, Ui.C_PARCH, Ui.C_PARCH_HI, Ui.C_PARCH_SHADE);
        g.drawString(font, title, fx + 10, fy + 8, Ui.C_GOLD, true);
        if (dirty) g.drawString(font, Component.translatable("rtshop.enhedit.unsaved"), fx + 14 + font.width(title), fy + 8, 0xFFFF9050, true);
        for (Object[] d : drawn) {
            Row r = (Row) d[0];
            int y = (int) d[1];
            boolean off = blocked(r);
            if (off) g.fill(fx + 8, y - 1, fx + W - 8, y + ROW - 2, 0x30FF2020);
            g.renderItem(new ItemStack(RulesScreens.item(r.out)), fx + 10, y + 1);
            if (r.count > 1) g.drawString(font, "×" + r.count, fx + 22, y + 10, 0xFFFFFFFF, true);
            String name = r.out.isEmpty() ? r.id : RulesScreens.name(r.out);
            g.drawString(font, Ui.clip(font, name, W - 250), fx + 32, y + 1, off ? Ui.C_RED : Ui.C_INK, false);
            String sub = r.id + " · " + (custom.containsKey(r.id) ? Component.translatable("rtshop.rules.custom_tag").getString() : r.type)
                    + (off ? " · " + Component.translatable("rtshop.rules.blocked_tag").getString() : "");
            Ui.scaledText(g, font, Ui.clip(font, sub, (int) ((W - 250) / 0.75F)), fx + 32, y + 11, 0.75F, Ui.C_INK_SOFT, false);
        }
        String count = Component.translatable("rtshop.rules.recipe_count", rows.size(), all.size(), disabled.size(), custom.size()).getString();
        g.drawString(font, Ui.clip(font, count, W - 270), fx + 10, fy + H - 18, Ui.C_CREAM, false);
        if (!errors.isEmpty()) g.drawString(font, Ui.clip(font, "⚠ " + errors.get(0), W - 20), fx + 10, fy + H - 30, 0xFFFF7060, true);
        super.render(g, mx, my, partial);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // 시험용
    public int rowsForTest() {
        return rows.size();
    }

    public int totalForTest() {
        return all.size();
    }

    public void searchForTest(String q, int v) {
        search = q;
        view = v;
        scroll = 0;
        rebuildWidgets();
    }

    public void toggleFirstForTest() {
        if (!rows.isEmpty()) {
            Row r = rows.get(0);
            if (!disabled.remove(r.id)) disabled.add(r.id);
            dirty = true;
            rebuildRows();
        }
    }

    public void blockForTest(String id) {
        disabled.add(id);
        dirty = true;
        rebuildRows();
    }

    public void saveForTest() {
        ShopNet.CHANNEL.sendToServer(new RulesActionPacket(RulesEditor.RECIPE_SAVE, "", rulesJson().toString()));
    }

    public boolean isBlockedForTest(String id) {
        return disabled.contains(id);
    }
}

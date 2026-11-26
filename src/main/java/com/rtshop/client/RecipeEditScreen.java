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
    /** 왼쪽 묶음에서 고른 보기: "view:0" · "view:1" · "view:2" */
    private String group = "view:0";
    /** 겹쳐 쓰는 분류 (비어 있으면 가리지 않음): 상점 판매(sold/unsold) · 아이템 종류 · 만드는 곳 · 모드 */
    private String fShop = "", fKind = "", fType = "", fMod = "";
    private final SideList side = new SideList();
    private int lx, rx0;
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

    private static String modOf(Row r) {
        return r.id.substring(0, Math.max(0, r.id.indexOf(':')));
    }

    /** 검색어: 조합법 ID · 결과 아이템 ID · 아이템 이름 · 모드 이름. */
    private boolean matchesText(Row r, String q) {
        return q.isEmpty() || r.id.contains(q) || r.out.contains(q) || RulesScreens.nameLc(r.out).contains(q)
                || LootEditScreen.modName(modOf(r)).toLowerCase(Locale.ROOT).contains(q);
    }

    private boolean anyFilter() {
        return view != 0 || !fShop.isEmpty() || !fKind.isEmpty() || !fType.isEmpty() || !fMod.isEmpty() || !search.trim().isEmpty();
    }

    private void clearFilters() {
        view = 0;
        group = "view:0";
        fShop = "";
        fKind = "";
        fType = "";
        fMod = "";
        search = "";
        scroll = 0;
    }

    private void filter() {
        rows.clear();
        String q = search.toLowerCase(Locale.ROOT).trim();
        for (Row r : all) {
            if (view == 1 && !blocked(r)) continue;
            if (view == 2 && !custom.containsKey(r.id)) continue;
            if (!fShop.isEmpty() && RulesScreens.SOLD.contains(r.out) != fShop.equals("sold")) continue;
            if (!fKind.isEmpty() && !RulesScreens.kind(r.out).equals(fKind)) continue;
            if (!fType.isEmpty() && !shortType(r).equals(fType)) continue;
            if (!fMod.isEmpty() && !modOf(r).equals(fMod)) continue;
            if (!matchesText(r, q)) continue;
            rows.add(r);
        }
        buildSide();
    }

    /** 조합 종류의 짧은 ID (crafting, smelting, …). */
    static String shortType(Row r) {
        if (r.custom) return "custom";
        String t = r.type;
        int c = t.indexOf(':');
        String p = c >= 0 ? t.substring(c + 1) : t;
        if (p.startsWith("crafting")) return "crafting";
        return p;
    }

    private String typeName(String t) {
        String key = "rtshop.rules.type." + t;
        return net.minecraft.client.resources.language.I18n.exists(key) ? Component.translatable(key).getString() : t.replace('_', ' ');
    }

    private static final int[] TYPE_COLORS = {0x8A7A60, 0xB0602E, 0x6E7F8F, 0x7A5AA8, 0x3D7A8B, 0x8B6A3D};

    private int typeColor(String t) {
        return switch (t) {
            case "custom" -> 0x2F6EA8;
            case "crafting" -> 0x8A7A60;
            case "smelting", "blasting", "smoking", "campfire_cooking" -> 0xB0602E;
            case "stonecutting" -> 0x6E7F8F;
            case "smithing", "smithing_transform", "smithing_trim" -> 0x5A5A6A;
            default -> TYPE_COLORS[Math.abs(t.hashCode()) % TYPE_COLORS.length];
        };
    }

    /** 왼쪽 묶음을 다시 만든다. 개수는 "검색어 + 다른 분류"를 적용했을 때 그 항목에 남는 수다 (골라 보기 전에 몇 개인지 알 수 있게). */
    private void buildSide() {
        side.items.clear();
        side.selected = group;
        side.marks.clear();
        if (!fShop.isEmpty()) side.marks.add("shop:" + fShop);
        if (!fKind.isEmpty()) side.marks.add("kind:" + fKind);
        if (!fType.isEmpty()) side.marks.add("type:" + fType);
        if (!fMod.isEmpty()) side.marks.add("mod:" + fMod);
        String q = search.toLowerCase(Locale.ROOT).trim();
        int[] views = new int[3];
        Map<String, Integer> shops = new java.util.HashMap<>();
        Map<String, Integer> kinds = new java.util.HashMap<>();
        Map<String, Integer> types = new java.util.TreeMap<>();
        Map<String, Integer> mods = new java.util.TreeMap<>((a, b) -> a.equals(b) ? 0 : a.equals("minecraft") ? -1 : b.equals("minecraft") ? 1 : a.compareTo(b));
        for (Row r : all) {
            if (!matchesText(r, q)) continue;
            boolean isBlocked = blocked(r), isCustom = custom.containsKey(r.id);
            String kind = RulesScreens.kind(r.out), type = shortType(r), mod = modOf(r);
            String shop = RulesScreens.SOLD.contains(r.out) ? "sold" : "unsold";
            boolean okView = view == 0 || (view == 1 && isBlocked) || (view == 2 && isCustom);
            boolean okShop = fShop.isEmpty() || fShop.equals(shop);
            boolean okKind = fKind.isEmpty() || fKind.equals(kind);
            boolean okType = fType.isEmpty() || fType.equals(type);
            boolean okMod = fMod.isEmpty() || fMod.equals(mod);
            if (okShop && okKind && okType && okMod) {
                views[0]++;
                if (isBlocked) views[1]++;
                if (isCustom) views[2]++;
            }
            if (okView && okKind && okType && okMod) shops.merge(shop, 1, Integer::sum);
            if (okView && okShop && okType && okMod) kinds.merge(kind, 1, Integer::sum);
            if (okView && okShop && okKind && okMod) types.merge(type, 1, Integer::sum);
            if (okView && okShop && okKind && okType) mods.merge(mod, 1, Integer::sum);
        }
        side.header(Component.translatable("rtshop.rules.side_view").getString());
        side.item("view:0", Component.translatable("rtshop.rules.view_all").getString(), views[0], 0);
        side.item("view:1", Component.translatable("rtshop.rules.view_blocked").getString(), views[1], 0xB03A2E);
        side.item("view:2", Component.translatable("rtshop.rules.view_custom").getString(), views[2], 0x2F6EA8);
        side.header(Component.translatable("rtshop.rules.side_shop").getString());
        side.item("shop:sold", Component.translatable("rtshop.rules.shop.sold").getString(), shops.getOrDefault("sold", 0), 0x3D8B3D);
        side.item("shop:unsold", Component.translatable("rtshop.rules.shop.unsold").getString(), shops.getOrDefault("unsold", 0), 0);
        side.header(Component.translatable("rtshop.rules.side_kind").getString());
        for (String k : RulesScreens.KINDS) {
            int n = kinds.getOrDefault(k, 0);
            if (n > 0 || k.equals(fKind)) side.item("kind:" + k, RulesScreens.kindName(k), n, RulesScreens.kindColor(k));
        }
        if (!fType.isEmpty()) types.putIfAbsent(fType, 0);
        if (!fMod.isEmpty()) mods.putIfAbsent(fMod, 0);
        side.header(Component.translatable("rtshop.rules.side_type").getString());
        for (Map.Entry<String, Integer> e : types.entrySet()) side.item("type:" + e.getKey(), typeName(e.getKey()), e.getValue(), typeColor(e.getKey()));
        side.header(Component.translatable("rtshop.rules.side_mod").getString());
        for (Map.Entry<String, Integer> e : mods.entrySet()) side.item("mod:" + e.getKey(), LootEditScreen.modName(e.getKey()), e.getValue(), 0);
    }

    @Override
    protected void init() {
        W = Math.min(520, width - 10);
        H = Math.min(280, height - 10);
        fx = (width - W) / 2;
        fy = (height - H) / 2;
        lx = fx + 10;
        int lw = Math.min(130, W / 4);
        rx0 = lx + lw + 8;
        side.x = lx;
        side.y = fy + 28;
        side.w = lw - 4;
        side.h = fy + H - 14 - side.y;
        filter();
        int nw = font.width(Component.translatable("rtshop.rules.new_recipe")) + 12;
        int newX = Math.max(rx0 + 4, fx + W - 8 - nw);
        // 검색 칸: 목록 바로 위에 넓게 (이름 · ID · 모드 이름으로 찾는다). 오른쪽의 ✕ 는 검색어와 분류를 모두 푼다
        int sw = Math.max(60, newX - 24 - rx0);
        EditBox sb = new EditBox(font, rx0, fy + 26, sw, 14, Component.empty());
        sb.setMaxLength(60);
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
        Button clr = MButton.of(Component.literal("✕"), b -> {
            clearFilters();
            rebuildWidgets();
        }).bounds(rx0 + sw + 3, fy + 25, 16, 16).build();
        clr.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("rtshop.rules.clear_filters")));
        addRenderableWidget(clr);
        addRenderableWidget(MButton.of(Component.translatable("rtshop.rules.new_recipe"), b -> minecraft.setScreen(new RecipeMakeScreen(this, null, null)))
                .bounds(newX, fy + 25, nw, 16).build());
        listY = fy + 47;
        visible = (fy + H - 30 - listY) / ROW;
        rebuildRows();
    }

    private void rebuildRows() {
        for (AbstractWidget w : rowWidgets) removeWidget(w);
        rowWidgets.clear();
        drawn.clear();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - visible)));
        for (int i = 0; i < visible && i + scroll < rows.size(); i++) {
            Row r = rows.get(i + scroll);
            int y = listY + i * ROW;
            drawn.add(new Object[]{r, y});
            int bx = fx + W - 10;
            if (custom.containsKey(r.id)) {
                bx -= 18;
                add(MButton.of(Component.literal("✕"), b -> {
                    custom.remove(r.id);
                    all.removeIf(q -> q.id.equals(r.id) && q.custom);
                    disabled.remove(r.id);
                    dirty = true;
                    filter();
                    rebuildRows();
                }).bounds(bx, y, 18, 18).build());
                bx -= 40;
                add(MButton.of(Component.translatable("rtshop.rules.edit"), b -> minecraft.setScreen(new RecipeMakeScreen(this, r.id, custom.get(r.id))))
                        .bounds(bx, y, 38, 18).build());
            }
            if (!r.out.isEmpty() && !custom.containsKey(r.id)) {
                boolean itemOff = disabled.contains("item:" + r.out);
                bx -= 50;
                Button ib = MButton.of(Component.translatable(itemOff ? "rtshop.rules.item_on" : "rtshop.rules.item_off"), b -> {
                    if (!disabled.remove("item:" + r.out)) disabled.add("item:" + r.out);
                    dirty = true;
                    buildSide();
                    rebuildRows();
                }).bounds(bx, y, 48, 18).build();
                ib.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("rtshop.rules.item_tip", RulesScreens.name(r.out))));
                add(ib);
            }
            boolean off = disabled.contains(r.id);
            bx -= 46;
            add(MButton.of(Component.translatable(off ? "rtshop.rules.unblock" : "rtshop.rules.block"), b -> {
                if (!disabled.remove(r.id)) disabled.add(r.id);
                dirty = true;
                buildSide();
                rebuildRows();
            }).bounds(bx, y, 44, 18).build());
        }
        int by = fy + H - 24;
        add(MButton.of(Component.translatable("rtshop.enhedit.save"), b ->
                ShopNet.CHANNEL.sendToServer(new RulesActionPacket(RulesEditor.RECIPE_SAVE, "", rulesJson().toString()))).bounds(fx + W - 194, by, 60, 18).build());
        add(MButton.of(Component.translatable("rtshop.enhedit.revert"), b ->
                ShopNet.CHANNEL.sendToServer(new RulesActionPacket(RulesEditor.RECIPE_OPEN, "", ""))).bounds(fx + W - 130, by, 60, 18).build());
        add(MButton.of(Component.translatable("rtshop.enhodds.close"), b -> onClose()).bounds(fx + W - 66, by, 58, 18).build());
    }

    private void add(AbstractWidget w) {
        addRenderableWidget(w);
        rowWidgets.add(w);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (side.click(mx, my, k -> {
            if (k.startsWith("view:")) {
                group = k;
                view = Integer.parseInt(k.substring(5));
            } else if (k.startsWith("shop:")) {
                fShop = fShop.equals(k.substring(5)) ? "" : k.substring(5);
            } else if (k.startsWith("kind:")) {
                fKind = fKind.equals(k.substring(5)) ? "" : k.substring(5);
            } else if (k.startsWith("type:")) {
                fType = fType.equals(k.substring(5)) ? "" : k.substring(5);
            } else if (k.startsWith("mod:")) {
                fMod = fMod.equals(k.substring(4)) ? "" : k.substring(4);
            }
            scroll = 0;
            filter();
            rebuildRows();
        })) return true;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (side.scroll(mx, my, delta)) return true;
        scroll = Math.max(0, scroll - (int) Math.signum(delta) * 3);
        rebuildRows();
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        Ui.woodFrame(g, fx, fy, W, H);
        Ui.bevel(g, rx0 - 2, fy + 43, fx + W - 6 - (rx0 - 2), H - 70, Ui.C_PARCH, Ui.C_PARCH_HI, Ui.C_PARCH_SHADE);
        side.selected = group;
        side.render(g, font, mx, my);
        g.drawString(font, title, fx + 10, fy + 8, Ui.C_GOLD, true);
        if (dirty) g.drawString(font, Component.translatable("rtshop.enhedit.unsaved"), fx + 14 + font.width(title), fy + 8, 0xFFFF9050, true);
        for (Object[] d : drawn) {
            Row r = (Row) d[0];
            int y = (int) d[1];
            boolean off = blocked(r);
            if (off) g.fill(rx0, y - 1, fx + W - 8, y + ROW - 2, 0x30FF2020);
            int ix = rx0 + 2;
            g.renderItem(new ItemStack(RulesScreens.item(r.out)), ix, y + 1);
            if (r.count > 1) {
                g.pose().pushPose();
                g.pose().translate(0, 0, 200);
                String c = Integer.toString(r.count);
                g.drawString(font, c, ix + 17 - font.width(c), y + 10, 0xFFFFFFFF, true);
                g.pose().popPose();
            }
            int textW = fx + W - 160 - (ix + 22);
            String name = r.out.isEmpty() ? r.id : RulesScreens.name(r.out);
            g.drawString(font, Ui.clip(font, name, textW), ix + 22, y + 1, off ? Ui.C_RED : Ui.C_INK, false);
            // 꼬리표: 종류 / 상태
            int cx = ix + 22;
            String st = shortType(r);
            cx = chip(g, cx, y + 11, typeName(st), typeColor(st));
            if (!r.out.isEmpty()) {
                String kd = RulesScreens.kind(r.out);
                if (!kd.equals("other")) cx = chip(g, cx, y + 11, RulesScreens.kindName(kd), RulesScreens.kindColor(kd));
                if (RulesScreens.SOLD.contains(r.out)) cx = chip(g, cx, y + 11, Component.translatable("rtshop.rules.sold_tag").getString(), 0x3D8B3D);
            }
            if (off) cx = chip(g, cx, y + 11, Component.translatable("rtshop.rules.blocked_tag").getString(), 0xB03A2E);
            Ui.scaledText(g, font, Ui.clip(font, r.id, (int) ((ix + 22 + textW - cx) / 0.7F)), cx + 2, y + 12, 0.7F, Ui.C_INK_SOFT, false);
            g.fill(rx0, y + ROW - 2, fx + W - 8, y + ROW - 1, 0x20000000);
        }
        String count = Component.translatable("rtshop.rules.recipe_count", rows.size(), all.size(), disabled.size(), custom.size()).getString();
        Ui.scaledText(g, font, Ui.clip(font, count, (int) ((fx + W - 198 - rx0) / 0.75F)), rx0, fy + H - 17, 0.75F, Ui.C_CREAM, false);
        if (!errors.isEmpty()) g.drawString(font, Ui.clip(font, "⚠ " + errors.get(0), fx + W - 10 - rx0), rx0, fy + H - 30, 0xFFFF7060, true);
        super.render(g, mx, my, partial);
    }

    /** 색 꼬리표 (작은 글씨). 다음 x 를 돌려준다. */
    private int chip(GuiGraphics g, int x, int y, String text, int rgb) {
        int w = (int) (font.width(text) * 0.7F) + 5;
        g.fill(x, y, x + w, y + 8, 0xFF000000 | rgb);
        Ui.scaledText(g, font, text, x + 2, y + 1, 0.7F, 0xFFFFFFFF, false);
        return x + w + 2;
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
        clearFilters();
        search = q;
        view = v;
        group = "view:" + v;
        rebuildWidgets();
    }

    /** 분류를 겹쳐 건다 (빈 글자 = 가리지 않음). */
    public void filterForTest(String shop, String kind, String type, String mod) {
        fShop = shop;
        fKind = kind;
        fType = type;
        fMod = mod;
        scroll = 0;
        rebuildWidgets();
    }

    /** 왼쪽 묶음에 이 항목이 있으면 그 개수, 없으면 -1. */
    public int sideCountForTest(String key) {
        for (SideList.Item it : side.items) if (!it.header && it.key.equals(key)) return it.count;
        return -1;
    }

    /** 지금 보이는 줄이 모두 이 조건에 맞는가 (sold: 1 파는 것, 0 없는 것, -1 안 봄). */
    public boolean rowsAllForTest(String kind, int sold) {
        for (Row r : rows) {
            if (!kind.isEmpty() && !RulesScreens.kind(r.out).equals(kind)) return false;
            if (sold >= 0 && RulesScreens.SOLD.contains(r.out) != (sold == 1)) return false;
        }
        return true;
    }

    public int soldKnownForTest() {
        return RulesScreens.SOLD.size();
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

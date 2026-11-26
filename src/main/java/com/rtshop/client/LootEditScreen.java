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
    /** 전리품표 가리기: 0 전체 · 1 상자 · 2 몹 */
    private int tableKind;
    /** 규칙을 고친 전리품표만 */
    private boolean onlyRuled;
    /** true 면 검색 칸의 글자를 아이템 이름으로 보고, 그 아이템이 나오는 전리품표를 서버에서 찾아 온다. */
    private boolean byItem;
    /** 아이템으로 찾은 전리품표 (null = 답을 기다리는 중) */
    private Set<String> found = new LinkedHashSet<>();
    private String foundQuery = "";
    /** 오른쪽 줄 가리기: 검색어 · 아이템 종류 · 상점 판매(0 전체, 1 파는 것, 2 없는 것) */
    private String rowSearch = "", rowKind = "";
    private int rowShop;
    private String selected = "";
    private final List<Row> rows = new ArrayList<>();
    private boolean clear;
    private final List<String> groups = new ArrayList<>();
    private boolean loading, dirty;
    private int samples = 400;
    private int scroll;
    private int fx, fy, W, H, lw, rx, listY, visible, tVisible;
    private static final int ROW = 24;
    private final SideList side = new SideList();
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
        for (String t : tables) {
            boolean mobTable = t.contains(":entities/");
            if (tableKind == 1 && mobTable) continue;
            if (tableKind == 2 && !mobTable) continue;
            if (onlyRuled && !ruled.contains(t)) continue;
            if (byItem) {
                if (q.length() >= 2 && (found == null || !found.contains(t))) continue;
            } else if (!q.isEmpty() && !t.contains(q) && !tableName(t).toLowerCase(Locale.ROOT).contains(q)
                    && !modName(t.substring(0, Math.max(0, t.indexOf(':')))).toLowerCase(Locale.ROOT).contains(q)) {
                continue;
            }
            shownTables.add(t);
        }
        // 종류(상자·몹) → 모드별로 묶는다
        side.items.clear();
        side.selected = selected;
        for (String kind : new String[]{"chest", "mob"}) {
            Map<String, List<String>> byMod = new java.util.TreeMap<>((a, b) -> a.equals(b) ? 0 : a.equals("minecraft") ? -1 : b.equals("minecraft") ? 1 : a.compareTo(b));
            for (String t : shownTables) {
                boolean mob = t.contains(":entities/");
                if (mob != kind.equals("mob")) continue;
                byMod.computeIfAbsent(t.substring(0, t.indexOf(':')), k -> new ArrayList<>()).add(t);
            }
            for (Map.Entry<String, List<String>> e : byMod.entrySet()) {
                side.header(Component.translatable(kind.equals("mob") ? "rtshop.rules.group_mob" : "rtshop.rules.group_chest", modName(e.getKey())).getString());
                List<String> l = e.getValue();
                l.sort((a, b) -> tableName(a).compareTo(tableName(b)));
                for (String t : l) side.item(t, tableName(t), -1, ruled.contains(t) ? 0x3D8B3D : 0);
            }
        }
    }

    /** 검색 칸의 글자가 이름·ID 에 든 아이템을 추려, 그 아이템이 나오는 전리품표를 서버에 묻는다. */
    private void requestFind() {
        String q = tableSearch.trim();
        if (q.length() > 100) q = q.substring(0, 100);
        foundQuery = q;
        found = new LinkedHashSet<>();
        if (!byItem || q.length() < 2) return;
        List<String> ids = RulesScreens.itemsMatching(q, 80);
        if (ids.isEmpty()) return;
        found = null;
        JsonArray a = new JsonArray();
        ids.forEach(a::add);
        ShopNet.CHANNEL.sendToServer(new RulesActionPacket(RulesEditor.LOOT_FIND, q, a.toString()));
    }

    /** 서버의 답: 그 아이템이 나오는 전리품표들. 그 사이에 검색어가 바뀌었으면 버린다. */
    void applyFound(JsonObject o) {
        if (!o.has("query") || !o.get("query").getAsString().equals(foundQuery)) return;
        Set<String> f = new LinkedHashSet<>();
        for (JsonElement e : o.getAsJsonArray("tables")) f.add(e.getAsString());
        found = f;
        side.scroll = 0;
        filterTables();
    }

    private static String nsOf(String id) {
        return id.substring(0, Math.max(0, id.indexOf(':')));
    }

    /** 오른쪽에 보일 줄: 검색어(이름 · ID · 모드 이름)와 종류 · 상점 판매로 가린다. */
    private List<Row> shownRows() {
        String q = rowSearch.toLowerCase(Locale.ROOT).trim();
        if (q.isEmpty() && rowKind.isEmpty() && rowShop == 0) return rows;
        List<Row> out = new ArrayList<>();
        for (Row r : rows) {
            if (!q.isEmpty() && !r.item.contains(q) && !RulesScreens.nameLc(r.item).contains(q)
                    && !modName(nsOf(r.item)).toLowerCase(Locale.ROOT).contains(q)) continue;
            if (!rowKind.isEmpty() && !RulesScreens.kind(r.item).equals(rowKind)) continue;
            if (rowShop != 0 && RulesScreens.SOLD.contains(r.item) != (rowShop == 1)) continue;
            out.add(r);
        }
        return out;
    }

    static String modName(String ns) {
        return net.minecraftforge.fml.ModList.get().getModContainerById(ns).map(c -> c.getModInfo().getDisplayName()).orElse(ns);
    }

    /** 전리품표의 알아보기 쉬운 이름: 몹은 그 몹 이름, 바닐라 상자는 한글 이름, 그 밖은 경로를 다듬어서. */
    static String tableName(String t) {
        int c = t.indexOf(':');
        String ns = t.substring(0, c), path = t.substring(c + 1);
        if (path.startsWith("entities/")) {
            net.minecraft.world.entity.EntityType<?> et = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES
                    .getValue(net.minecraft.resources.ResourceLocation.tryParse(ns + ":" + path.substring(9)));
            String n = et == null ? path.substring(9) : et.getDescription().getString();
            return path.indexOf('/', 9) > 0 ? n + " (" + path.substring(path.indexOf('/', 9) + 1) + ")" : n;
        }
        String key = "rtshop.loot." + ns + "." + path.replace('/', '.');
        if (net.minecraft.client.resources.language.I18n.exists(key)) return Component.translatable(key).getString();
        String p = path.replace("chests/", "").replace('/', ' ').replace('_', ' ');
        return p.isEmpty() ? path : Character.toUpperCase(p.charAt(0)) + p.substring(1);
    }

    @Override
    protected void init() {
        W = Math.min(500, width - 10);
        H = Math.min(280, height - 10);
        fx = (width - W) / 2;
        fy = (height - H) / 2;
        lw = Math.min(150, W / 3);
        rx = fx + lw + 12;
        filterTables();
        EditBox sb = new EditBox(font, fx + 8, fy + 26, lw - 2, 14, Component.empty());
        sb.setMaxLength(60);
        sb.setValue(tableSearch);
        sb.setHint(Component.translatable(byItem ? "rtshop.rules.search_item" : "rtshop.rules.search_table"));
        sb.setResponder(v -> {
            if (!v.equals(tableSearch)) {
                tableSearch = v;
                tableScroll = 0;
                side.scroll = 0;
                if (byItem) requestFind();
                filterTables();   // 창을 다시 짓지 않는다: 다시 지으면 글자를 칠 때마다 검색 칸의 초점이 풀린다
            }
        });
        addRenderableWidget(sb);
        // 가리기 단추 셋: 찾는 방법(표 이름 / 아이템) · 종류(전체 / 상자 / 몹) · 고친 것만
        int fbx = fx + 8, fw1 = 50, fw2 = 36, fw3 = Math.max(30, lw - 2 - fw1 - fw2 - 4);
        Button modeBtn = MButton.of(Component.translatable(byItem ? "rtshop.rules.find_item" : "rtshop.rules.find_table"), b -> {
            byItem = !byItem;
            requestFind();
            rebuildWidgets();
        }).bounds(fbx, fy + 43, fw1, 14).build();
        modeBtn.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("rtshop.rules.find_tip")));
        addRenderableWidget(modeBtn);
        addRenderableWidget(MButton.of(Component.translatable("rtshop.rules.tkind." + tableKind), b -> {
            tableKind = (tableKind + 1) % 3;
            side.scroll = 0;
            rebuildWidgets();
        }).bounds(fbx + fw1 + 2, fy + 43, fw2, 14).build());
        addRenderableWidget(MButton.of(Component.translatable(onlyRuled ? "rtshop.rules.only_ruled_on" : "rtshop.rules.only_ruled_off"), b -> {
            onlyRuled = !onlyRuled;
            side.scroll = 0;
            rebuildWidgets();
        }).bounds(fbx + fw1 + fw2 + 4, fy + 43, fw3, 14).build());
        side.x = fx + 10;
        side.y = fy + 62;
        side.w = lw - 6;
        side.h = fy + H - 14 - side.y;
        // 오른쪽 줄 가리기: 검색 · 아이템 종류 · 상점 판매
        if (!selected.isEmpty()) {
            int rgt = fx + W - 8, kw = 66, shw = 88;
            int rsw = Math.max(40, rgt - rx - kw - shw - 6);
            EditBox rs = new EditBox(font, rx, fy + 24, rsw, 13, Component.empty());
            rs.setMaxLength(60);
            rs.setValue(rowSearch);
            rs.setHint(Component.translatable("rtshop.rules.row_search"));
            rs.setResponder(v -> {
                if (!v.equals(rowSearch)) {
                    rowSearch = v;
                    scroll = 0;
                    rebuildRows();
                }
            });
            addRenderableWidget(rs);
            String kindLabel = rowKind.isEmpty() ? Component.translatable("rtshop.rules.view_all").getString() : RulesScreens.kindName(rowKind);
            addRenderableWidget(MButton.of(Component.translatable("rtshop.rules.row_kind", kindLabel), b -> {
                int at = -1;
                for (int i = 0; i < RulesScreens.KINDS.length; i++) if (RulesScreens.KINDS[i].equals(rowKind)) at = i;
                rowKind = at + 1 >= RulesScreens.KINDS.length ? "" : RulesScreens.KINDS[at + 1];
                scroll = 0;
                rebuildWidgets();
            }).bounds(rx + rsw + 3, fy + 23, kw, 15).build());
            addRenderableWidget(MButton.of(Component.translatable("rtshop.rules.row_shop." + rowShop), b -> {
                rowShop = (rowShop + 1) % 3;
                scroll = 0;
                rebuildWidgets();
            }).bounds(rx + rsw + kw + 6, fy + 23, shw, 15).build());
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
        add(MButton.of(Component.translatable(clear ? "rtshop.rules.clear_on" : "rtshop.rules.clear_off"), b -> {
            clear = !clear;
            dirty = true;
            rebuildRows();
        }).bounds(rx, fy + 40, 104, 16).build());
        add(MButton.of(Component.translatable("rtshop.rules.add_item"), b -> minecraft.setScreen(new PickerScreen(this, PickerScreen.Mode.ITEM, v -> {
            Row r = rows.stream().filter(q -> q.item.equals(v)).findFirst().orElse(null);
            if (r == null) {
                r = new Row();
                r.item = v;
                rows.add(0, r);
            }
            r.mode = 2;
            dirty = true;
        }))).bounds(rx + 108, fy + 40, 90, 16).build());
        List<Row> list = shownRows();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, list.size() - visible)));
        int right = fx + W - 8;
        for (int i = 0; i < visible && i + scroll < list.size(); i++) {
            Row r = list.get(i + scroll);
            int y = listY + i * ROW;
            drawn.add(new Object[]{r, y});
            String[] keys = {"rtshop.rules.mode_default", "rtshop.rules.mode_off", "rtshop.rules.mode_custom"};
            net.minecraft.ChatFormatting[] cols = {net.minecraft.ChatFormatting.WHITE, net.minecraft.ChatFormatting.RED, net.minecraft.ChatFormatting.GREEN};
            add(MButton.of(Component.translatable(keys[r.mode]).withStyle(cols[r.mode]), b -> {
                r.mode = (r.mode + 1) % 3;
                if (!r.natural && r.mode == 0) r.mode = 1;
                dirty = true;
                rebuildRows();
            }).bounds(right - 152, y + 3, 44, 18).build());
            if (r.mode == 2) {
                add(num(right - 104, y + 5, 38, fmt(r.chance), v -> r.chance = Double.parseDouble(v)));
                add(num(right - 50, y + 5, 20, Integer.toString(r.min), v -> r.min = Integer.parseInt(v)));
                add(num(right - 24, y + 5, 22, Integer.toString(r.max), v -> r.max = Integer.parseInt(v)));
            }
        }
        int by = fy + H - 24;
        add(MButton.of(Component.translatable("rtshop.enhedit.save"), b -> save()).bounds(fx + W - 194, by, 60, 18).build());
        add(MButton.of(Component.translatable("rtshop.enhedit.revert"), b -> select(selected)).bounds(fx + W - 130, by, 60, 18).build());
        add(MButton.of(Component.translatable("rtshop.enhodds.close"), b -> onClose()).bounds(fx + W - 66, by, 58, 18).build());
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
    public boolean mouseClicked(double mx, double my, int button) {
        if (side.click(mx, my, this::select)) return true;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (mx < rx) {
            side.scroll(mx, my, delta);
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
        side.selected = selected;
        side.render(g, font, mx, my);
        if (shownTables.isEmpty()) {
            String why = byItem && found == null ? "rtshop.rules.finding" : byItem && tableSearch.trim().length() >= 2 ? "rtshop.rules.found_none" : "rtshop.rules.no_tables";
            int wy = side.y + 4;
            for (net.minecraft.util.FormattedCharSequence line : font.split(Component.translatable(why), Math.max(40, (int) ((side.w - 6) / 0.8F)))) {
                g.pose().pushPose();
                g.pose().translate(side.x + 3, wy, 0);
                g.pose().scale(0.8F, 0.8F, 1F);
                g.drawString(font, line, 0, 0, 0xFF4A3420, false);
                g.pose().popPose();
                wy += 9;
            }
        }
        int tx = fx + 14 + font.width(title);
        if (dirty) {
            g.drawString(font, Component.translatable("rtshop.enhedit.unsaved"), tx, fy + 8, 0xFFFF9050, true);
            tx += 4 + font.width(Component.translatable("rtshop.enhedit.unsaved"));
        }
        if (!selected.isEmpty()) g.drawString(font, Ui.clip(font, "· " + tableName(selected), fx + W - 10 - tx), tx, fy + 8, Ui.C_CREAM, true);
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
                g.fill(rx - 2, y + ROW - 2, right, y + ROW - 1, 0x20000000);
                g.renderItem(new ItemStack(RulesScreens.item(r.item)), rx, y + 3);
                int textW = right - 156 - (rx + 20);
                String shownName = Ui.clip(font, RulesScreens.name(r.item), textW);
                g.drawString(font, shownName, rx + 20, y + 1, Ui.C_INK, false);
                if (RulesScreens.SOLD.contains(r.item) && font.width(shownName) + 30 < textW) {
                    Ui.scaledText(g, font, Component.translatable("rtshop.rules.sold_tag").getString(), rx + 24 + font.width(shownName), y + 3, 0.7F, Ui.C_GREEN, false);
                }
                // 확률 막대: 위 = 원래(기본), 아래 = 지금 (적용 후)
                int bx = rx + 20, bw = Math.max(30, textW - 70);
                g.fill(bx, y + 11, bx + bw, y + 14, 0xFFCDB98D);
                if (r.natural) g.fill(bx, y + 11, bx + Math.max(1, (int) (bw * Math.min(1, r.rawChance))), y + 14, 0xFF9E8A62);
                g.fill(bx, y + 16, bx + bw, y + 19, 0xFFCDB98D);
                if (r.afterChance > 0) g.fill(bx, y + 16, bx + Math.max(1, (int) (bw * Math.min(1, r.afterChance))), y + 19, 0xFF5A8F3A);
                String base = r.natural ? Component.translatable("rtshop.rules.stat_base_short", pct(r.rawChance),
                        r.rawMin == r.rawMax ? Integer.toString(r.rawMin) : r.rawMin + "~" + r.rawMax).getString()
                        : Component.translatable("rtshop.rules.stat_none").getString();
                Ui.scaledText(g, font, Ui.clip(font, base, (int) (66 / 0.7F)), bx + bw + 4, y + 10, 0.7F, Ui.C_INK_SOFT, false);
                Ui.scaledText(g, font, Component.translatable("rtshop.rules.stat_now_short", pct(r.afterChance)).getString(), bx + bw + 4, y + 16, 0.7F,
                        r.afterChance > 0 ? Ui.C_GREEN : Ui.C_INK_SOFT, false);
                if (r.mode == 2) {
                    g.drawString(font, "%", right - 64, y + 8, Ui.C_INK, false);
                    g.drawString(font, "~", right - 29, y + 8, Ui.C_INK, false);
                }
            }
            String foot = Component.translatable("rtshop.rules.loot_foot", samples).getString();
            if (!groups.isEmpty()) foot += "  " + Component.translatable("rtshop.rules.loot_groups", String.join(", ", groups)).getString();
            Ui.scaledText(g, font, Ui.clip(font, foot, (int) ((fx + W - 198 - (rx - 4)) / 0.75F)), rx - 4, fy + H - 17, 0.75F, Ui.C_CREAM, false);
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
    public int shownTablesForTest() {
        return shownTables.size();
    }

    public boolean shownHasForTest(String table) {
        return shownTables.contains(table);
    }

    /** 표 가리기: kind 0 전체 · 1 상자 · 2 몹. */
    public void tableFilterForTest(int kind, boolean ruledOnly) {
        tableKind = kind;
        onlyRuled = ruledOnly;
        rebuildWidgets();
    }

    /** 아이템 이름으로 전리품표 찾기 (빈 글자면 표 이름 검색으로 되돌린다). */
    public void findItemForTest(String q) {
        byItem = !q.isEmpty();
        tableSearch = q;
        requestFind();
        rebuildWidgets();
    }

    public boolean foundReadyForTest() {
        return found != null;
    }

    public void rowFilterForTest(String q, String kind, int shop) {
        rowSearch = q;
        rowKind = kind;
        rowShop = shop;
        scroll = 0;
        rebuildWidgets();
    }

    public int rowsShownForTest() {
        return shownRows().size();
    }

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

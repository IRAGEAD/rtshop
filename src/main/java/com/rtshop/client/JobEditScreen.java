package com.rtshop.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.job.JobConfig;
import com.rtshop.job.WeaponTypes;
import com.rtshop.net.JobEditSavePacket;
import com.rtshop.net.ShopNet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 직업 편집기 (OP, /rtshop job edit).
 * 왼쪽: 직업 목록 + "무기 분류". 오른쪽: 패시브 수치 / 무기 상성(전투 직업) / 무기 분류 예외.
 * 수치는 [-] [+] 로 5% 씩 (Shift 를 누르면 1% 씩) 바꾸고, 저장하면 바로 모든 플레이어에게 적용된다.
 */
public class JobEditScreen extends Screen {
    private static final int LIST_W = 112;
    private static final int ROW = 13;

    private record Hit(int x, int y, int w, int h, Supplier<String> label, BooleanSupplier enabled, Consumer<Integer> action) {
    }

    private record Text(int x, int y, Supplier<String> text, int color) {
    }

    private record Icon(int x, int y, ItemStack stack) {
    }

    private JsonObject model;
    private String lastJson;
    /** 고른 직업 번호, -1 = 무기 분류 화면. */
    private int selected = 0;
    private int tab = 0;
    private int scroll = 0;
    private boolean dirty = false;
    private Component status;
    private boolean statusOk;
    private int statusTicks;
    private final List<Hit> hits = new ArrayList<>();
    private final List<Text> texts = new ArrayList<>();
    private final List<Icon> icons = new ArrayList<>();
    private int fx, fy, W, H;

    public JobEditScreen(String json) {
        super(Component.translatable("rtshop.jedit.title"));
        this.lastJson = json;
        this.model = JsonParser.parseString(json).getAsJsonObject();
    }

    public void update(String json) {
        lastJson = json;
        if (!dirty) {
            model = JsonParser.parseString(json).getAsJsonObject();
            rebuild();
        }
    }

    public void showResult(Component msg, boolean ok) {
        status = msg;
        statusOk = ok;
        statusTicks = 200;
        if (ok) {
            dirty = false;
            model = JsonParser.parseString(lastJson).getAsJsonObject();
            rebuild();
        }
    }

    private void local(String key, boolean ok, Object... args) {
        status = Component.translatable(key, args);
        statusOk = ok;
        statusTicks = 160;
    }

    // ------------------------------------------------------------------ 모델 도우미

    private JsonArray jobs() {
        return model.getAsJsonArray("jobs");
    }

    private JsonObject job() {
        return selected >= 0 && selected < jobs().size() ? jobs().get(selected).getAsJsonObject() : null;
    }

    private static JsonObject obj(JsonObject o, String key) {
        if (!o.has(key) || !o.get(key).isJsonObject()) {
            o.add(key, new JsonObject());
        }
        return o.getAsJsonObject(key);
    }

    private static double num(JsonObject o, String key) {
        return o.has(key) ? o.get(key).getAsDouble() : 0;
    }

    private static double round(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }

    private JobConfig parsed() {
        try {
            return JobConfig.fromJson(model);
        } catch (Exception e) {
            return null;
        }
    }

    private static String pct(double v) {
        double x = v * 100;
        String n = Math.abs(x - Math.rint(x)) < 1e-6 ? Long.toString(Math.round(x)) : String.format(Locale.ROOT, "%.1f", x);
        return (v > 0 ? "+" : "") + n + "%";
    }

    private static String flat(double v) {
        String n = Math.abs(v - Math.rint(v)) < 1e-6 ? Long.toString(Math.round(v)) : String.format(Locale.ROOT, "%.1f", v);
        return (v > 0 ? "+" : "") + n;
    }

    private static ItemStack icon(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id == null ? "" : id);
        Item item = rl == null ? null : ForgeRegistries.ITEMS.getValue(rl);
        return item == null || item == Items.AIR ? new ItemStack(Items.BARRIER) : new ItemStack(item);
    }

    private static String itemName(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        Item item = rl == null ? null : ForgeRegistries.ITEMS.getValue(rl);
        return item == null || item == Items.AIR ? id : new ItemStack(item).getHoverName().getString();
    }

    private double step() {
        return hasShiftDown() ? 0.01 : 0.05;
    }

    // ------------------------------------------------------------------ 배치

    @Override
    protected void init() {
        W = Math.min(this.width - 8, 480);
        H = Math.min(this.height - 8, 270);
        fx = (this.width - W) / 2;
        fy = (this.height - H) / 2;
        rebuild();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (statusTicks > 0 && --statusTicks == 0) {
            status = null;
        }
    }

    private int rx() {
        return fx + 6 + LIST_W + 6;
    }

    private int rw() {
        return W - LIST_W - 18;
    }

    private int top() {
        return fy + 44;
    }

    private int bodyH() {
        return H - 44 - 28;
    }

    private int bottomY() {
        return fy + H - 23;
    }

    private void hit(int x, int y, int w, int h, Supplier<String> label, BooleanSupplier en, Consumer<Integer> act) {
        hits.add(new Hit(x, y, w, h, label, en, act));
    }

    private void text(int x, int y, Supplier<String> t, int color) {
        texts.add(new Text(x, y, t, color));
    }

    private void rebuild() {
        hits.clear();
        texts.clear();
        icons.clear();
        clearWidgets();
        // 왼쪽 목록
        JsonArray jobs = jobs();
        for (int i = 0; i < jobs.size(); i++) {
            final int idx = i;
            JsonObject j = jobs.get(i).getAsJsonObject();
            String slot = j.has("slot") ? j.get("slot").getAsString() : "";
            String name = (JobConfig.COMBAT.equals(slot) ? "⚔ " : "✿ ") + j.get("name").getAsString();
            hit(fx + 6, top() + i * 15, LIST_W, 14, () -> name, () -> selected != idx, b -> {
                selected = idx;
                tab = 0;
                scroll = 0;
                rebuild();
            });
        }
        hit(fx + 6, top() + jobs.size() * 15 + 6, LIST_W, 14, () -> tr("rtshop.jedit.weapon_types"), () -> selected != -1, b -> {
            selected = -1;
            typesTab = 0;
            scroll = 0;
            rebuild();
        });
        // 아래 버튼
        hit(rx() + rw() - 54, bottomY(), 54, 16, () -> tr("rtshop.qedit.save"), () -> dirty, b -> save());
        hit(rx() + rw() - 112, bottomY(), 54, 16, () -> tr("rtshop.qedit.revert"), () -> dirty, b -> {
            dirty = false;
            model = JsonParser.parseString(lastJson).getAsJsonObject();
            rebuild();
        });

        if (selected == -1) {
            int tw = (rw() - 4) / 2;
            hit(rx(), fy + 27, tw, 14, () -> tr("rtshop.jedit.sub_overrides"), () -> typesTab == 1 || typesTab == 2, b -> {
                typesTab = 0;
                scroll = 0;
                rebuild();
            });
            hit(rx() + tw + 4, fy + 27, tw, 14, () -> tr("rtshop.jedit.sub_categories"), () -> typesTab == 0, b -> {
                typesTab = 1;
                scroll = 0;
                rebuild();
            });
            if (typesTab == 0) {
                buildTypes();
            } else if (typesTab == 1) {
                buildCategories();
            } else {
                buildCategoryItems();
            }
            return;
        }
        JsonObject j = job();
        if (j == null) {
            return;
        }
        boolean combat = JobConfig.COMBAT.equals(j.get("slot").getAsString());
        if (combat) {
            int tw = (rw() - 4) / 2;
            hit(rx(), fy + 27, tw, 14, () -> tr("rtshop.jedit.tab_params"), () -> tab != 0, b -> {
                tab = 0;
                rebuild();
            });
            hit(rx() + tw + 4, fy + 27, tw, 14, () -> tr("rtshop.jedit.tab_weapons"), () -> tab != 1, b -> {
                tab = 1;
                rebuild();
            });
        } else {
            tab = 0;
        }
        if (tab == 0) {
            buildParams(j);
        } else {
            buildWeapons(j);
        }
    }

    private static String tr(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    private static String paramLabel(String key) {
        String k = "rtshop.jobparam." + key;
        String t = tr(k);
        return t.equals(k) ? key : t;
    }

    private void buildParams(JsonObject j) {
        JsonObject params = obj(j, "params");
        int x = rx() + 6;
        int y = top() + 4;
        int valX = rx() + rw() - 96;
        for (Map.Entry<String, JsonElement> e : params.entrySet()) {
            String key = e.getKey();
            boolean isFlat = JobConfig.FLAT_PARAMS.contains(key);
            text(x, y + 2, () -> paramLabel(key), Ui.C_INK);
            text(valX + 18, y + 2, () -> {
                double v = num(params, key);
                return isFlat ? flat(v) : pct(v);
            }, Ui.C_INK);
            hit(valX, y, 14, 12, () -> "-", () -> true, b -> {
                params.addProperty(key, round(num(params, key) - (isFlat ? 1 : step())));
                dirty = true;
            });
            hit(valX + 62, y, 14, 12, () -> "+", () -> true, b -> {
                params.addProperty(key, round(num(params, key) + (isFlat ? 1 : step())));
                dirty = true;
            });
            y += ROW + 2;
        }
        text(x, y + 4, () -> tr("rtshop.jedit.hint_step"), Ui.C_INK_SOFT);
        previewY = y + 18;
    }

    private int previewY = -1;

    private void buildWeapons(JsonObject j) {
        JsonObject weapons = obj(j, "weapons");
        JobConfig cfg = parsed();
        int x = rx() + 6;
        int y = top() + 3;
        int dmgX = rx() + rw() - 176;
        int spdX = rx() + rw() - 88;
        text(x + 16, y, () -> tr("rtshop.jedit.col_type"), Ui.C_GOLD_DARK);
        text(dmgX + 16, y, () -> tr("rtshop.jedit.col_damage"), Ui.C_GOLD_DARK);
        text(spdX + 10, y, () -> tr("rtshop.jedit.col_speed"), Ui.C_GOLD_DARK);
        y += 10;
        int row = Math.max(10, Math.min(ROW, (top() + bodyH() - 4 - y) / 12));
        List<String[]> rows = new ArrayList<>();
        if (cfg != null) {
            for (WeaponTypes.Type t : cfg.weaponTypes) {
                rows.add(new String[]{t.id, t.name, t.icon, t.melee ? "1" : "0"});
            }
        }
        rows.add(new String[]{WeaponTypes.MAGIC, tr("rtshop.jedit.magic"), "minecraft:enchanted_book", "0"});
        for (String[] r : rows) {
            String type = r[0];
            boolean melee = r[3].equals("1");
            icons.add(new Icon(x, y - 2, icon(r[2])));
            text(x + 18, y + 1, () -> r[1], Ui.C_INK);
            text(dmgX + 16, y + 1, () -> pct(weaponValue(weapons, type, "damage")), Ui.C_INK);
            hit(dmgX, y, 12, row - 1, () -> "-", () -> true, b -> addWeapon(weapons, type, "damage", -step()));
            hit(dmgX + 60, y, 12, row - 1, () -> "+", () -> true, b -> addWeapon(weapons, type, "damage", step()));
            if (melee) {
                text(spdX + 16, y + 1, () -> pct(weaponValue(weapons, type, "speed")), Ui.C_INK);
                hit(spdX, y, 12, row - 1, () -> "-", () -> true, b -> addWeapon(weapons, type, "speed", -step()));
                hit(spdX + 60, y, 12, row - 1, () -> "+", () -> true, b -> addWeapon(weapons, type, "speed", step()));
            } else {
                text(spdX + 24, y + 1, () -> "—", Ui.C_INK_SOFT);
            }
            y += row;
        }
        previewY = -1;
    }

    private static double weaponValue(JsonObject weapons, String type, String key) {
        return weapons.has(type) ? num(weapons.getAsJsonObject(type), key) : 0;
    }

    private void addWeapon(JsonObject weapons, String type, String key, double delta) {
        JsonObject w = obj(weapons, type);
        double v = round(Math.max(-0.95, Math.min(10, num(w, key) + delta)));
        w.addProperty(key, v);
        if (!w.has("damage")) {
            w.addProperty("damage", 0);
        }
        if (!w.has("speed")) {
            w.addProperty("speed", 0);
        }
        dirty = true;
    }

    // ------------------------------------------------------------------ 무기 분류

    private List<String> typeCycle() {
        List<String> l = new ArrayList<>();
        l.add("");
        JobConfig cfg = parsed();
        if (cfg != null) {
            for (WeaponTypes.Type t : cfg.weaponTypes) {
                l.add(t.id);
            }
        }
        l.add(WeaponTypes.NONE);
        return l;
    }

    private String typeLabel(String id) {
        if (id.isEmpty()) {
            return tr("rtshop.jedit.auto");
        }
        if (WeaponTypes.NONE.equals(id)) {
            return tr("rtshop.jedit.not_weapon");
        }
        JobConfig cfg = parsed();
        return cfg == null ? id : cfg.typeName(id);
    }

    private void setOverride(String itemId, String type) {
        JsonObject over = obj(model, "weaponOverrides");
        if (type.isEmpty()) {
            over.remove(itemId);
        } else {
            over.addProperty(itemId, type);
        }
        dirty = true;
    }

    private String autoType(String itemId) {
        JobConfig cfg = parsed();
        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(itemId));
        if (cfg == null || item == null) {
            return "?";
        }
        return typeLabel(WeaponTypes.classify(item, cfg.weaponTypes, Map.of()));
    }

    private void buildTypes() {
        int x = rx() + 6;
        int y = top() + 4;
        int w = rw() - 12;
        // 손에 든 아이템
        Minecraft mc = Minecraft.getInstance();
        ItemStack held = mc.player == null ? ItemStack.EMPTY : mc.player.getMainHandItem();
        text(x, y + 2, () -> tr("rtshop.jedit.held"), Ui.C_GOLD_DARK);
        y += 13;
        if (held.isEmpty()) {
            text(x, y + 2, () -> tr("rtshop.jedit.held_empty"), Ui.C_INK_SOFT);
        } else {
            String id = String.valueOf(ForgeRegistries.ITEMS.getKey(held.getItem()));
            icons.add(new Icon(x, y - 2, held.copy()));
            text(x + 18, y + 2, () -> Ui.clip(this.font, held.getHoverName().getString(), w - 110), Ui.C_INK);
            addCycle(rx() + rw() - 96, y, id);
        }
        y += 18;
        text(x, y, () -> tr("rtshop.jedit.overrides"), Ui.C_GOLD_DARK);
        y += 12;
        JsonObject over = obj(model, "weaponOverrides");
        List<String> ids = new ArrayList<>(over.keySet());
        int maxRows = Math.max(1, (top() + bodyH() - 36 - y) / 15);
        scroll = Math.max(0, Math.min(Math.max(0, ids.size() - maxRows), scroll));
        for (int i = scroll; i < Math.min(ids.size(), scroll + maxRows); i++) {
            String id = ids.get(i);
            icons.add(new Icon(x, y - 2, icon(id)));
            text(x + 18, y + 2, () -> Ui.clip(this.font, itemName(id), w - 140), Ui.C_INK);
            addCycle(rx() + rw() - 112, y, id);
            hit(rx() + rw() - 20, y - 1, 14, 13, () -> "X", () -> true, b -> {
                setOverride(id, "");
                rebuild();
            });
            y += 15;
        }
        if (ids.isEmpty()) {
            text(x, y + 2, () -> tr("rtshop.jedit.no_overrides"), Ui.C_INK_SOFT);
            y += 14;
        }
        int addY = top() + bodyH() - 32;
        hit(x, addY, 130, 14, () -> tr("rtshop.jedit.add_override"), () -> true, b -> {
            if (minecraft != null) {
                minecraft.setScreen(new PickerScreen(this, PickerScreen.Mode.ITEM, v -> {
                    if (!obj(model, "weaponOverrides").has(v)) {
                        obj(model, "weaponOverrides").addProperty(v, WeaponTypes.NONE);
                        dirty = true;
                    }
                }));
            }
        });
        text(x, addY + 18, () -> tr("rtshop.jedit.hint_types"), Ui.C_INK_SOFT);
    }

    private String currentType(String id) {
        JsonObject over = obj(model, "weaponOverrides");
        return over.has(id) ? over.get(id).getAsString() : "";
    }

    private void addCycle(int x, int y, String itemId) {
        hit(x, y - 1, 92, 13, () -> {
            String cur = currentType(itemId);
            return (cur.isEmpty() ? tr("rtshop.jedit.auto_is", autoType(itemId)) : typeLabel(cur)) + " ▼";
        }, () -> true, b -> openTypePopup(x, y - 1, itemId));
    }

    private ChoicePopup popup;

    private void openTypePopup(int x, int y, String itemId) {
        List<ChoicePopup.Option> opts = new ArrayList<>();
        JobConfig cfg = parsed();
        opts.add(new ChoicePopup.Option("", tr("rtshop.jedit.auto_is", autoType(itemId))));
        if (cfg != null) {
            for (WeaponTypes.Type t : cfg.weaponTypes) {
                opts.add(new ChoicePopup.Option(t.id, t.name, icon(t.icon)));
            }
        }
        opts.add(new ChoicePopup.Option(WeaponTypes.NONE, tr("rtshop.jedit.not_weapon"), new ItemStack(Items.BARRIER)));
        popup = new ChoicePopup(x, y, 92, 13, this.height, this.font, opts, currentType(itemId), v -> {
            setOverride(itemId, v);
            popup = null;
        });
    }

    /** 점검용: 무기 분류 화면에서 첫 예외 항목의 종류 팝업을 연다. */
    public void openFirstTypePopupForTest() {
        JsonObject over = obj(model, "weaponOverrides");
        if (!over.keySet().isEmpty()) {
            openTypePopup(rx() + rw() - 112, top() + 60, over.keySet().iterator().next());
        }
    }

    public ChoicePopup popupForTest() {
        return popup;
    }

    // ------------------------------------------------------------------ 무기 종류 (카테고리) 관리

    /** 0 = 아이템 예외, 1 = 무기 종류 목록, 2 = 고른 종류에 든 아이템 */
    private int typesTab = 0;
    private int selType = 0;

    /** 모델의 무기 종류 배열 (없으면 지금 설정에서 채운다). */
    private JsonArray typeArray() {
        if (!model.has("weaponTypes") || !model.get("weaponTypes").isJsonArray()) {
            JsonArray a = new JsonArray();
            JobConfig cfg = parsed();
            for (WeaponTypes.Type t : cfg != null ? cfg.weaponTypes : WeaponTypes.defaults()) a.add(WeaponTypes.write(t));
            model.add("weaponTypes", a);
        }
        return model.getAsJsonArray("weaponTypes");
    }

    private static String joinList(JsonObject t, String key) {
        if (!t.has(key) || !t.get(key).isJsonArray()) return "";
        List<String> l = new ArrayList<>();
        for (JsonElement e : t.getAsJsonArray(key)) l.add(e.getAsString());
        return String.join(", ", l);
    }

    private static void setList(JsonObject t, String key, String text) {
        JsonArray a = new JsonArray();
        for (String part : text.split(",")) {
            String v = part.trim().toLowerCase(Locale.ROOT);
            if (!v.isEmpty()) a.add(v);
        }
        t.add(key, a);
    }

    private void buildCategories() {
        JsonArray types = typeArray();
        int x = rx() + 6;
        int y = top() + 4;
        int listRows = 7;
        selType = Math.max(0, Math.min(selType, types.size() - 1));
        scroll = Math.max(0, Math.min(Math.max(0, types.size() - listRows), scroll));
        for (int i = scroll; i < Math.min(types.size(), scroll + listRows); i++) {
            final int idx = i;
            JsonObject t = types.get(i).getAsJsonObject();
            boolean melee = !t.has("melee") || t.get("melee").getAsBoolean();
            icons.add(new Icon(x, y - 2, icon(t.has("icon") ? t.get("icon").getAsString() : "")));
            hit(x + 16, y - 1, rw() - 166, 13, () -> (idx == selType ? "▶ " : "") + t.get("name").getAsString(), () -> idx != selType, b -> {
                selType = idx;
                rebuild();
            });
            int bx = rx() + rw() - 146;
            hit(bx, y - 1, 46, 13, () -> tr(melee ? "rtshop.jedit.melee" : "rtshop.jedit.ranged"), () -> true, b -> {
                t.addProperty("melee", !melee);
                dirty = true;
                rebuild();
            });
            hit(bx + 48, y - 1, 14, 13, () -> "▲", () -> idx > 0, b -> {
                moveType(idx, -1);
            });
            hit(bx + 64, y - 1, 14, 13, () -> "▼", () -> idx < types.size() - 1, b -> {
                moveType(idx, 1);
            });
            hit(bx + 80, y - 1, 40, 13, () -> tr("rtshop.jedit.items_of"), () -> true, b -> {
                selType = idx;
                typesTab = 2;
                scroll = 0;
                rebuild();
            });
            hit(bx + 122, y - 1, 14, 13, () -> "X", () -> types.size() > 1, b -> deleteType(idx));
            y += 15;
        }
        hit(x, y, 110, 13, () -> tr("rtshop.jedit.add_type"), () -> true, b -> addType());
        text(x + 116, y + 3, () -> tr("rtshop.jedit.order_hint"), Ui.C_INK_SOFT);
        y += 18;
        if (types.isEmpty()) return;
        JsonObject t = types.get(selType).getAsJsonObject();
        // 고른 종류의 자세한 설정
        g2(x, y, tr("rtshop.jedit.f_name"), t.get("name").getAsString(), 40, v -> {
            t.addProperty("name", v.isBlank() ? t.get("id").getAsString() : v);
            dirty = true;
        });
        hit(rx() + rw() - 96, y - 1, 90, 14, () -> tr("rtshop.jedit.f_icon"), () -> true, b -> {
            if (minecraft != null) {
                minecraft.setScreen(new PickerScreen(this, PickerScreen.Mode.ITEM, v -> {
                    t.addProperty("icon", v);
                    dirty = true;
                }));
            }
        });
        y += 17;
        g2(x, y, tr("rtshop.jedit.f_has"), joinList(t, "nameHas"), 400, v -> {
            setList(t, "nameHas", v);
            dirty = true;
        });
        y += 17;
        g2(x, y, tr("rtshop.jedit.f_ends"), joinList(t, "nameEnds"), 200, v -> {
            setList(t, "nameEnds", v);
            dirty = true;
        });
        y += 17;
        g2(x, y, tr("rtshop.jedit.f_mods"), joinList(t, "mods"), 200, v -> {
            setList(t, "mods", v);
            dirty = true;
        });
        y += 17;
        g2(x, y, tr("rtshop.jedit.f_classes"), joinList(t, "classes"), 200, v -> {
            setList(t, "classes", v);
            dirty = true;
        });
        text(x, y + 16, () -> tr("rtshop.jedit.classes_hint"), Ui.C_INK_SOFT);
    }

    /** 이름표 + 입력 칸 한 줄 */
    private void g2(int x, int y, String label, String value, int max, Consumer<String> onChange) {
        text(x, y + 3, () -> label, Ui.C_GOLD_DARK);
        int lx = x + 70;
        int w = rx() + rw() - 104 - lx;
        net.minecraft.client.gui.components.EditBox box = new net.minecraft.client.gui.components.EditBox(this.font, lx, y, w, 13, Component.literal(label));
        box.setMaxLength(max);
        box.setValue(value);
        box.setResponder(onChange);
        addRenderableWidget(box);
    }

    private void moveType(int idx, int dir) {
        JsonArray a = typeArray();
        int j = idx + dir;
        if (j < 0 || j >= a.size()) return;
        JsonElement e = a.get(idx);
        a.set(idx, a.get(j));
        a.set(j, e);
        if (selType == idx) selType = j;
        dirty = true;
        rebuild();
    }

    private void addType() {
        JsonArray a = typeArray();
        int n = 1;
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (JsonElement e : a) ids.add(e.getAsJsonObject().get("id").getAsString());
        while (ids.contains("custom_" + n)) n++;
        WeaponTypes.Type t = new WeaponTypes.Type();
        t.id = "custom_" + n;
        t.name = tr("rtshop.jedit.new_type", n);
        t.icon = "minecraft:iron_sword";
        // 새 종류는 맨 위: 이름 규칙이 다른 종류보다 먼저 맞도록
        JsonArray b = new JsonArray();
        b.add(WeaponTypes.write(t));
        a.forEach(b::add);
        model.add("weaponTypes", b);
        selType = 0;
        scroll = 0;
        dirty = true;
        rebuild();
    }

    /** 종류를 지운다: 그 종류로 정한 아이템 예외는 '자동'으로, 직업의 그 종류 상성은 지운다. */
    private void deleteType(int idx) {
        JsonArray a = typeArray();
        if (a.size() <= 1) return;
        String id = a.get(idx).getAsJsonObject().get("id").getAsString();
        a.remove(idx);
        JsonObject over = obj(model, "weaponOverrides");
        for (String k : new ArrayList<>(over.keySet())) if (id.equals(over.get(k).getAsString())) over.remove(k);
        for (JsonElement je : jobs()) {
            JsonObject j = je.getAsJsonObject();
            if (j.has("weapons") && j.get("weapons").isJsonObject()) j.getAsJsonObject("weapons").remove(id);
        }
        selType = Math.max(0, Math.min(selType, a.size() - 1));
        dirty = true;
        rebuild();
    }

    /** 지금 설정(저장 전)으로 이 종류에 들어가는 아이템 */
    private List<String> itemsOfType(String typeId) {
        JobConfig cfg = parsed();
        List<String> out = new ArrayList<>();
        if (cfg == null) return out;
        for (Item item : ForgeRegistries.ITEMS) {
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
            if (rl == null) continue;
            if (typeId.equals(WeaponTypes.classify(item, cfg.weaponTypes, cfg.weaponOverrides))) out.add(rl.toString());
        }
        return out;
    }

    private List<String> itemsCache;
    private String itemsCacheKey = "";

    private void buildCategoryItems() {
        JsonArray types = typeArray();
        if (types.isEmpty()) {
            typesTab = 1;
            return;
        }
        selType = Math.max(0, Math.min(selType, types.size() - 1));
        JsonObject t = types.get(selType).getAsJsonObject();
        String typeId = t.get("id").getAsString();
        String key = typeId + "|" + model.toString().hashCode();
        if (itemsCache == null || !key.equals(itemsCacheKey)) {
            itemsCache = itemsOfType(typeId);
            itemsCacheKey = key;
        }
        List<String> ids = itemsCache;
        int x = rx() + 6;
        int y = top() + 4;
        int w = rw() - 12;
        hit(x, y - 1, 60, 13, () -> "◀ " + tr("rtshop.jedit.back"), () -> true, b -> {
            typesTab = 1;
            scroll = 0;
            rebuild();
        });
        text(x + 66, y + 2, () -> tr("rtshop.jedit.items_title", t.get("name").getAsString(), ids.size()), Ui.C_GOLD_DARK);
        y += 16;
        int maxRows = Math.max(1, (top() + bodyH() - 20 - y) / 15);
        scroll = Math.max(0, Math.min(Math.max(0, ids.size() - maxRows), scroll));
        for (int i = scroll; i < Math.min(ids.size(), scroll + maxRows); i++) {
            String id = ids.get(i);
            icons.add(new Icon(x, y - 2, icon(id)));
            text(x + 18, y + 2, () -> Ui.clip(this.font, itemName(id), w - 120), Ui.C_INK);
            addCycle(rx() + rw() - 100, y, id);
            y += 15;
        }
        if (ids.isEmpty()) text(x, y + 2, () -> tr("rtshop.jedit.items_none"), Ui.C_INK_SOFT);
        text(x, top() + bodyH() - 12, () -> tr("rtshop.jedit.items_hint"), Ui.C_INK_SOFT);
    }

    /** 점검용 */
    public int typeCountForTest() {
        return typeArray().size();
    }

    public void addTypeForTest() {
        addType();
    }

    public void showCategoriesForTest(int sub) {
        selected = -1;
        typesTab = sub;
        rebuild();
    }

    public boolean typeExistsForTest(String id) {
        for (JsonElement e : typeArray()) if (e.getAsJsonObject().get("id").getAsString().equals(id)) return true;
        return false;
    }

    // ------------------------------------------------------------------ 저장

    private void save() {
        String json = model.toString();
        JobConfig test;
        try {
            test = JobConfig.fromJson(model);
        } catch (Exception e) {
            status = Component.literal(String.valueOf(e.getMessage()));
            statusOk = false;
            statusTicks = 160;
            return;
        }
        if (test == null || json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > JobEditSavePacket.MAX - 100) {
            local("rtshop.qedit.err_too_long", false);
            return;
        }
        ShopNet.CHANNEL.sendToServer(new JobEditSavePacket(json));
    }

    // ------------------------------------------------------------------ 그리기

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        this.renderBackground(g);
        Ui.woodFrame(g, fx, fy, W, H);
        Ui.titleBand(g, fx + 5, fy + 4, fx + W - 5, fy + 23);
        g.fill(fx + 5, fy + 22, fx + W - 5, fy + 23, Ui.C_GOLD_DARK);
        String t = this.title.getString() + (dirty ? " *" : "");
        g.drawString(this.font, t, fx + 10, fy + 9, Ui.C_GOLD, true);
        String help = Ui.clip(this.font, tr(selected >= 0 && tab == 1 ? "rtshop.jedit.hint_weapons" : "rtshop.jedit.help"), W - 30 - this.font.width(t));
        g.drawString(this.font, help, fx + W - 10 - this.font.width(help), fy + 9, Ui.C_PARCH_SHADE, false);
        g.drawString(this.font, tr("rtshop.jedit.jobs"), fx + 8, fy + 30, Ui.C_CREAM, false);
        Ui.bevel(g, rx(), top(), rw(), bodyH(), Ui.C_PARCH, Ui.C_PARCH_HI, Ui.C_PARCH_SHADE);
        for (Hit h : hits) {
            Ui.button(g, this.font, h.x(), h.y(), h.w(), h.h(), h.label().get(), Ui.inside(mx, my, h.x(), h.y(), h.w(), h.h()),
                    h.enabled().getAsBoolean());
        }
        for (Icon ic : icons) {
            g.pose().pushPose();
            g.pose().translate(ic.x(), ic.y(), 0);
            g.pose().scale(0.875F, 0.875F, 1.0F);
            g.renderItem(ic.stack(), 0, 0);
            g.pose().popPose();
        }
        for (Text tx : texts) {
            g.drawString(this.font, tx.text().get(), tx.x(), tx.y(), tx.color(), false);
        }
        // 패시브 탭: 지금 수치로 만든 설명 미리보기 (직업 화면에 이대로 보인다)
        if (selected >= 0 && tab == 0 && previewY > 0) {
            JobConfig cfg = parsed();
            JsonObject j = job();
            if (cfg != null && j != null) {
                JobConfig.JobDef d = cfg.job(j.get("id").getAsString());
                int y = previewY;
                g.drawString(this.font, tr("rtshop.jedit.preview"), rx() + 6, y, Ui.C_GOLD_DARK, false);
                y += 11;
                if (d != null) {
                    for (String line : cfg.describe(d, 0.10)) {
                        char k = line.charAt(0);
                        int col = k == '+' ? Ui.C_GREEN : k == '-' ? Ui.C_RED : Ui.C_INK;
                        for (FormattedCharSequence s : this.font.split(Component.literal((k == '+' ? "▲ " : k == '-' ? "▼ " : "• ") + line.substring(1)), rw() - 14)) {
                            if (y > top() + bodyH() - 10) {
                                break;
                            }
                            g.drawString(this.font, s, rx() + 8, y, col, false);
                            y += 10;
                        }
                    }
                }
            }
        }
        if (status != null) {
            List<FormattedCharSequence> lines = this.font.split(status, rw() - 124);
            for (int i = 0; i < Math.min(2, lines.size()); i++) {
                g.drawString(this.font, lines.get(i), rx(), bottomY() + (lines.size() > 1 ? 0 : 4) + i * 9, statusOk ? 0xFFB8F0A0 : 0xFFFFB0A0, true);
            }
        }
        super.render(g, mx, my, partial);
        if (popup != null) {
            popup.render(g, this.font, mx, my);
        }
    }

    // ------------------------------------------------------------------ 입력

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (popup != null) {
            if (popup.click(mx, my)) {
                popup = null;
            }
            return true;
        }
        if (button == 0 || button == 1) {
            for (Hit h : new ArrayList<>(hits)) {
                if (Ui.inside(mx, my, h.x(), h.y(), h.w(), h.h()) && h.enabled().getAsBoolean()) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    h.action().accept(button);
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (popup != null) {
            popup.scroll(delta);
            return true;
        }
        if (selected == -1) {
            scroll -= (int) Math.signum(delta);
            rebuild();
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    private boolean confirmClose = false;

    @Override
    public void onClose() {
        if (popup != null) {
            popup = null;
            return;
        }
        if (dirty && !confirmClose) {
            confirmClose = true;
            local("rtshop.qedit.confirm_close", false);
            return;
        }
        super.onClose();
    }

    // ------------------------------------------------------------------ 점검용

    public void showForTest(String jobId, int tabIndex) {
        for (int i = 0; i < jobs().size(); i++) {
            if (jobs().get(i).getAsJsonObject().get("id").getAsString().equals(jobId)) {
                selected = i;
            }
        }
        if (jobId.equals("#types")) {
            selected = -1;
        }
        tab = tabIndex;
        rebuild();
    }

    /** 점검용: 지금 직업의 무기 피해 수치를 바꾸고 저장한다. */
    public void changeAndSaveForTest(String type, double delta) {
        JsonObject j = job();
        if (j != null) {
            addWeapon(obj(j, "weapons"), type, "damage", delta);
            save();
        }
    }

    public boolean dirtyForTest() {
        return dirty;
    }
}

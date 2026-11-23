package com.rtshop.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.net.EnhanceActionPacket;
import com.rtshop.net.ShopNet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 강화 설정 편집기 (OP): 단계별 확률(성공·유지·손상·파괴)·Gold·재료, 보석 묶음, 능력치 증가율.
 * 재료와 묶음 아이템은 목록(검색)에서 고른다. 저장하면 config/rtshop/enhance.json 에 쓰고 모두에게 바로 적용된다.
 */
public class EnhanceEditScreen extends Screen {
    private JsonObject cfg;
    private int tab = 0; // 0 단계, 1 묶음, 2 능력치
    private int level = 1;
    private String group = "";
    private boolean dirty;
    private int fx, fy, W, H;
    private final List<Object[]> labels = new ArrayList<>();
    private final List<Object[]> icons = new ArrayList<>();

    public EnhanceEditScreen(String json) {
        super(Component.translatable("rtshop.enhedit.title"));
        update(json);
    }

    public void update(String json) {
        cfg = JsonParser.parseString(json).getAsJsonObject();
        dirty = false;
        if (group.isEmpty() || !cfg.getAsJsonObject("groups").has(group)) {
            group = cfg.getAsJsonObject("groups").keySet().stream().findFirst().orElse("");
        }
        if (width > 0) rebuildWidgets();
    }

    private JsonObject levelObj(int lv) {
        for (JsonElement e : cfg.getAsJsonArray("levels")) {
            if (e.getAsJsonObject().get("level").getAsInt() == lv) return e.getAsJsonObject();
        }
        return null;
    }

    private static Item item(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null || !ForgeRegistries.ITEMS.containsKey(rl) ? Items.BARRIER : ForgeRegistries.ITEMS.getValue(rl);
    }

    private ItemStack iconOf(String id) {
        if (id.startsWith("#")) {
            JsonObject g = cfg.getAsJsonObject("groups").getAsJsonObject(id.substring(1));
            if (g != null && g.getAsJsonArray("items").size() > 0) {
                JsonArray a = g.getAsJsonArray("items");
                return new ItemStack(item(a.get((int) (System.currentTimeMillis() / 1000 % a.size())).getAsString()));
            }
            return new ItemStack(Items.CHEST);
        }
        return new ItemStack(item(id));
    }

    private String nameOf(String id) {
        if (id.startsWith("#")) {
            JsonObject g = cfg.getAsJsonObject("groups").getAsJsonObject(id.substring(1));
            return (g != null && g.has("name") ? g.get("name").getAsString() : id) + " (#)";
        }
        Item it = item(id);
        return it == Items.BARRIER ? id : it.getDescription().getString();
    }

    private void changed() {
        dirty = true;
    }

    private EditBox num(int x, int y, int w, String value, Consumer<String> set) {
        EditBox b = new EditBox(font, x, y, w, 14, Component.empty());
        b.setMaxLength(12);
        b.setValue(value);
        b.setFilter(s -> s.isEmpty() || s.matches("[0-9.]*"));
        b.setResponder(v -> {
            set.accept(v);
            changed();
        });
        addRenderableWidget(b);
        return b;
    }

    private static String fmt(double v) {
        return v == Math.floor(v) ? Long.toString((long) v) : Double.toString(v);
    }

    private static double parse(String s) {
        try {
            return s.isEmpty() ? 0 : Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    protected void init() {
        labels.clear();
        icons.clear();
        W = Math.min(440, width - 10);
        H = Math.min(260, height - 10);
        fx = (width - W) / 2;
        fy = (height - H) / 2;
        String[] tabs = {"rtshop.enhedit.tab_levels", "rtshop.enhedit.tab_groups", "rtshop.enhedit.tab_bonus"};
        int tx = fx + 8;
        for (int i = 0; i < 3; i++) {
            int idx = i;
            Component t = Component.translatable(tabs[i]);
            int w = font.width(t) + 16;
            Button b = MButton.of(t, x -> {
                tab = idx;
                rebuildWidgets();
            }).bounds(tx, fy + 22, w, 16).build();
            b.active = tab != i;
            addRenderableWidget(b);
            tx += w + 3;
        }
        int cx = fx + 8, cy = fy + 44;
        if (tab == 0) initLevels(cx, cy);
        else if (tab == 1) initGroups(cx, cy);
        else initBonus(cx, cy);
        int by = fy + H - 24;
        addRenderableWidget(MButton.of(Component.translatable("rtshop.enhedit.save"), b -> {
            ShopNet.CHANNEL.sendToServer(new EnhanceActionPacket(EnhanceActionPacket.SAVE, cfg.toString()));
        }).bounds(fx + W - 248, by, 80, 18).build());
        addRenderableWidget(MButton.of(Component.translatable("rtshop.enhedit.revert"), b ->
                ShopNet.CHANNEL.sendToServer(new EnhanceActionPacket(EnhanceActionPacket.OPEN_EDITOR, ""))).bounds(fx + W - 164, by, 76, 18).build());
        addRenderableWidget(MButton.of(Component.translatable("rtshop.enhodds.close"), b -> onClose()).bounds(fx + W - 84, by, 76, 18).build());
    }

    private void initLevels(int cx, int cy) {
        for (int lv = 1; lv <= 11; lv++) {
            int l = lv;
            Button b = MButton.of(Component.literal("+" + lv), x -> {
                level = l;
                rebuildWidgets();
            }).bounds(cx + ((lv - 1) % 2) * 30, cy + ((lv - 1) / 2) * 20, 28, 18).build();
            b.active = level != lv;
            addRenderableWidget(b);
        }
        JsonObject o = levelObj(level);
        if (o == null) return;
        int x = cx + 70, y = cy;
        String[] keys = {"success", "maintain", "damage", "destroy"};
        String[] names = {"rtshop.enhodds.success", "rtshop.enhodds.maintain", "rtshop.enhodds.damage", "rtshop.enhodds.destroy"};
        for (int i = 0; i < 4; i++) {
            String k = keys[i];
            labels.add(new Object[]{Component.translatable(names[i]), x + i * 54, y});
            num(x + i * 54, y + 10, 48, fmt(o.get(k).getAsDouble()), v -> o.addProperty(k, parse(v)));
        }
        labels.add(new Object[]{Component.translatable("rtshop.enhodds.gold"), x + 220, y});
        num(x + 220, y + 10, 64, Long.toString(o.get("gold").getAsLong()), v -> o.addProperty("gold", (long) parse(v)));
        y += 34;
        labels.add(new Object[]{Component.translatable("rtshop.enhedit.materials"), x, y});
        y += 11;
        JsonArray mats = o.getAsJsonArray("materials");
        for (int i = 0; i < mats.size(); i++) {
            JsonObject m = mats.get(i).getAsJsonObject();
            int idx = i;
            String id = m.get("item").getAsString();
            icons.add(new Object[]{id, x, y + 1});
            Button pick = MButton.of(Component.literal(nameOf(id)), b -> pickMaterial(mats, idx)).bounds(x + 20, y, 160, 18).build();
            addRenderableWidget(pick);
            num(x + 184, y + 2, 36, Integer.toString(m.get("count").getAsInt()), v -> m.addProperty("count", Math.max(1, (int) parse(v))));
            addRenderableWidget(MButton.of(Component.literal("✕"), b -> {
                mats.remove(idx);
                changed();
                rebuildWidgets();
            }).bounds(x + 224, y, 18, 18).build());
            y += 20;
        }
        if (mats.size() < 6) {
            addRenderableWidget(MButton.of(Component.translatable("rtshop.enhedit.add_item"), b -> {
                minecraft.setScreen(new PickerScreen(this, PickerScreen.Mode.ITEM, v -> {
                    JsonObject m = new JsonObject();
                    m.addProperty("item", v);
                    m.addProperty("count", 1);
                    mats.add(m);
                    changed();
                }));
            }).bounds(x, y + 2, 110, 18).build());
            addRenderableWidget(MButton.of(Component.translatable("rtshop.enhedit.add_group"), b -> {
                List<String> gs = new ArrayList<>(cfg.getAsJsonObject("groups").keySet());
                if (gs.isEmpty()) return;
                JsonObject m = new JsonObject();
                m.addProperty("item", "#" + gs.get(0));
                m.addProperty("count", 1);
                mats.add(m);
                changed();
                rebuildWidgets();
            }).bounds(x + 114, y + 2, 110, 18).build());
        }
    }

    /** 재료 한 칸 바꾸기: 묶음이면 다음 묶음으로 돌리고, 아이템이면 목록에서 고른다. */
    private void pickMaterial(JsonArray mats, int idx) {
        JsonObject m = mats.get(idx).getAsJsonObject();
        String id = m.get("item").getAsString();
        if (id.startsWith("#")) {
            List<String> gs = new ArrayList<>(cfg.getAsJsonObject("groups").keySet());
            int i = gs.indexOf(id.substring(1));
            m.addProperty("item", "#" + gs.get((i + 1) % gs.size()));
            changed();
            rebuildWidgets();
        } else {
            minecraft.setScreen(new PickerScreen(this, PickerScreen.Mode.ITEM, v -> {
                m.addProperty("item", v);
                changed();
            }));
        }
    }

    private void initGroups(int cx, int cy) {
        JsonObject groups = cfg.getAsJsonObject("groups");
        int y = cy;
        for (Map.Entry<String, JsonElement> e : groups.entrySet()) {
            String k = e.getKey();
            String name = e.getValue().getAsJsonObject().has("name") ? e.getValue().getAsJsonObject().get("name").getAsString() : k;
            Button b = MButton.of(Component.literal(name), x -> {
                group = k;
                rebuildWidgets();
            }).bounds(cx, y, 100, 18).build();
            b.active = !k.equals(group);
            addRenderableWidget(b);
            y += 20;
        }
        JsonObject g = groups.getAsJsonObject(group);
        if (g == null) return;
        int x = cx + 110;
        y = cy;
        labels.add(new Object[]{Component.translatable("rtshop.enhedit.group_name"), x, y});
        EditBox nb = new EditBox(font, x, y + 10, 150, 14, Component.empty());
        nb.setValue(g.has("name") ? g.get("name").getAsString() : group);
        nb.setResponder(v -> {
            g.addProperty("name", v);
            changed();
        });
        addRenderableWidget(nb);
        y += 30;
        JsonArray items = g.getAsJsonArray("items");
        for (int i = 0; i < items.size(); i++) {
            int idx = i;
            String id = items.get(i).getAsString();
            icons.add(new Object[]{id, x + (i % 2) * 150, y + 1 + (i / 2) * 20});
            addRenderableWidget(MButton.of(Component.literal(nameOf(id)), b -> minecraft.setScreen(new PickerScreen(this, PickerScreen.Mode.ITEM, v -> {
                items.set(idx, new com.google.gson.JsonPrimitive(v));
                changed();
            }))).bounds(x + 20 + (i % 2) * 150, y + (i / 2) * 20, 106, 18).build());
            addRenderableWidget(MButton.of(Component.literal("✕"), b -> {
                items.remove(idx);
                changed();
                rebuildWidgets();
            }).bounds(x + 128 + (i % 2) * 150, y + (i / 2) * 20, 18, 18).build());
        }
        int rows = (items.size() + 1) / 2;
        if (items.size() < 10) {
            addRenderableWidget(MButton.of(Component.translatable("rtshop.enhedit.add_item"), b -> minecraft.setScreen(
                    new PickerScreen(this, PickerScreen.Mode.ITEM, v -> {
                        items.add(v);
                        changed();
                    }))).bounds(x, y + rows * 20 + 4, 110, 18).build());
        }
    }

    private void initBonus(int cx, int cy) {
        JsonArray wb = cfg.getAsJsonArray("weaponBonus"), ab = cfg.getAsJsonArray("armorBonus");
        labels.add(new Object[]{Component.translatable("rtshop.enhedit.weapon_bonus"), cx, cy});
        labels.add(new Object[]{Component.translatable("rtshop.enhedit.armor_bonus"), cx, cy + 50});
        for (int i = 1; i <= 11 && i < wb.size(); i++) {
            int idx = i;
            int x = cx + (i - 1) * 38;
            labels.add(new Object[]{Component.literal("+" + i), x, cy + 12});
            num(x, cy + 22, 34, fmt(wb.get(i).getAsDouble()), v -> wb.set(idx, new com.google.gson.JsonPrimitive(parse(v))));
            labels.add(new Object[]{Component.literal("+" + i), x, cy + 62});
            num(x, cy + 72, 34, fmt(ab.get(i).getAsDouble()), v -> ab.set(idx, new com.google.gson.JsonPrimitive(parse(v))));
        }
        int y = cy + 100;
        labels.add(new Object[]{Component.translatable("rtshop.enhedit.tough_from"), cx, y});
        num(cx, y + 10, 50, Integer.toString(cfg.get("toughnessFrom").getAsInt()), v -> cfg.addProperty("toughnessFrom", (int) parse(v)));
        labels.add(new Object[]{Component.translatable("rtshop.enhedit.tough_per"), cx + 140, y});
        num(cx + 140, y + 10, 50, fmt(cfg.get("toughnessPerLevel").getAsDouble()), v -> cfg.addProperty("toughnessPerLevel", parse(v)));
        labels.add(new Object[]{Component.translatable("rtshop.enhedit.damage_drop"), cx + 280, y});
        num(cx + 280, y + 10, 50, Integer.toString(cfg.get("damageDrop").getAsInt()), v -> cfg.addProperty("damageDrop", (int) parse(v)));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        Ui.woodFrame(g, fx, fy, W, H);
        Ui.bevel(g, fx + 5, fy + 40, W - 10, H - 70, Ui.C_PARCH, Ui.C_PARCH_HI, Ui.C_PARCH_SHADE);
        g.drawString(font, title, fx + 10, fy + 8, Ui.C_GOLD, true);
        if (dirty) g.drawString(font, Component.translatable("rtshop.enhedit.unsaved"), fx + 10 + font.width(title) + 10, fy + 8, 0xFFFF9050, true);
        for (Object[] l : labels) {
            g.drawString(font, (Component) l[0], (int) l[1], (int) l[2], Ui.C_INK, false);
        }
        for (Object[] ic : icons) {
            g.renderItem(iconOf((String) ic[0]), (int) ic[1], (int) ic[2]);
        }
        if (tab == 0) {
            JsonObject o = levelObj(level);
            if (o != null) {
                double sum = o.get("success").getAsDouble() + o.get("maintain").getAsDouble() + o.get("damage").getAsDouble()
                        + o.get("destroy").getAsDouble();
                Component c = Component.translatable("rtshop.enhedit.sum", EnhanceClient.pct(sum));
                g.drawString(font, c, fx + W - 14 - font.width(c), fy + 8, Math.abs(sum - 100) < 0.001 ? 0xFF7CFF6A : 0xFFFF6060, true);
            }
        }
        super.render(g, mx, my, partial);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** 시험용. */
    public JsonObject configForTest() {
        return cfg;
    }

    public void setTabForTest(int t) {
        tab = t;
        rebuildWidgets();
    }
}

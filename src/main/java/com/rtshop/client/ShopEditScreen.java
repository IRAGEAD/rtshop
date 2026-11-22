package com.rtshop.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.net.ShopEditActionPacket;
import com.rtshop.net.ShopNet;
import com.rtshop.shop.ShopEditor;
import net.minecraft.client.Minecraft;
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

/**
 * 상점 편집기 (OP, 상인의 상점 화면에서 [상점 편집]): 이 상점의 품목과 가격을 바로 고친다.
 * 판매가(플레이어가 살 때) · 매입가(플레이어가 팔 때, 비우면 판매가의 기본 비율) · 판매/매입 여부 · 카테고리 · 빼기 · 아이템 추가(목록에서).
 * 저장하면 상점 파일을 고쳐 쓰고(예전 파일은 .bak) 모든 상점을 다시 읽는다.
 */
public class ShopEditScreen extends Screen {
    private final String shopId;
    private JsonObject file;
    private final List<JsonObject> rows = new ArrayList<>();
    private String category = "";
    private String search = "";
    private int scroll;
    private boolean dirty;
    private int fx, fy, W, H, listY, visible;
    private static final int ROW = 20;
    private final List<Object[]> icons = new ArrayList<>();

    public ShopEditScreen(String id, String json) {
        super(Component.translatable("rtshop.shopedit.title", id));
        this.shopId = id;
        this.file = JsonParser.parseString(json).getAsJsonObject();
    }

    public static void open(String id, String json) {
        Minecraft.getInstance().setScreen(new ShopEditScreen(id, json));
    }

    private JsonArray items() {
        if (!file.has("items")) file.add("items", new JsonArray());
        return file.getAsJsonArray("items");
    }

    private List<String> categories() {
        List<String> out = new ArrayList<>();
        if (file.has("categories")) {
            for (JsonElement e : file.getAsJsonArray("categories")) out.add(e.getAsJsonObject().get("id").getAsString());
        }
        return out;
    }

    private String categoryName(String id) {
        if (file.has("categories")) {
            for (JsonElement e : file.getAsJsonArray("categories")) {
                JsonObject c = e.getAsJsonObject();
                if (c.get("id").getAsString().equals(id)) return c.has("name") ? c.get("name").getAsString() : id;
            }
        }
        return id.isEmpty() ? Component.translatable("rtshop.shopedit.all").getString() : id;
    }

    private static Item item(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null || !ForgeRegistries.ITEMS.containsKey(rl) ? Items.BARRIER : ForgeRegistries.ITEMS.getValue(rl);
    }

    private void filter() {
        rows.clear();
        String q = search.toLowerCase();
        for (JsonElement e : items()) {
            JsonObject o = e.getAsJsonObject();
            String cat = o.has("category") ? o.get("category").getAsString() : "";
            if (!category.isEmpty() && !category.equals(cat)) continue;
            String id = o.get("item").getAsString();
            if (!q.isEmpty() && !id.toLowerCase().contains(q) && !item(id).getDescription().getString().toLowerCase().contains(q)) continue;
            rows.add(o);
        }
    }

    private static String fmt(JsonObject o, String k) {
        if (!o.has(k) || o.get(k).isJsonNull()) return "";
        double v = o.get(k).getAsDouble();
        return v == Math.floor(v) ? Long.toString((long) v) : Double.toString(v);
    }

    @Override
    protected void init() {
        icons.clear();
        W = Math.min(470, width - 10);
        H = Math.min(270, height - 10);
        fx = (width - W) / 2;
        fy = (height - H) / 2;
        filter();
        // 카테고리
        List<String> cats = new ArrayList<>();
        cats.add("");
        cats.addAll(categories());
        int cy = fy + 26;
        for (String c : cats) {
            Button b = Button.builder(Component.literal(categoryName(c)), x -> {
                category = c;
                scroll = 0;
                rebuildWidgets();
            }).bounds(fx + 8, cy, 84, 16).build();
            b.active = !c.equals(category);
            addRenderableWidget(b);
            cy += 17;
            if (cy > fy + H - 50) break;
        }
        EditBox sb = new EditBox(font, fx + 100, fy + 26, 120, 14, Component.empty());
        sb.setValue(search);
        sb.setHint(Component.translatable("rtshop.shopedit.search"));
        sb.setResponder(v -> {
            if (!v.equals(search)) {
                search = v;
                scroll = 0;
                filter();
                rebuildRows();
            }
        });
        addRenderableWidget(sb);
        addRenderableWidget(Button.builder(Component.translatable("rtshop.shopedit.add"), b -> minecraft.setScreen(
                new PickerScreen(this, PickerScreen.Mode.ITEM, v -> {
                    JsonObject o = new JsonObject();
                    o.addProperty("item", v);
                    o.addProperty("price", 100);
                    List<String> cs = categories();
                    o.addProperty("category", !category.isEmpty() ? category : cs.isEmpty() ? "misc" : cs.get(0));
                    items().add(o);
                    dirty = true;
                    search = "";
                }))).bounds(fx + 226, fy + 25, 90, 16).build());
        listY = fy + 50;
        visible = (fy + H - 30 - listY) / ROW;
        rebuildRows();
    }

    private final List<net.minecraft.client.gui.components.AbstractWidget> rowWidgets = new ArrayList<>();

    private void rebuildRows() {
        for (var w : rowWidgets) removeWidget(w);
        rowWidgets.clear();
        icons.clear();
        int x = fx + 100;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - visible)));
        for (int i = 0; i < visible && i + scroll < rows.size(); i++) {
            JsonObject o = rows.get(i + scroll);
            int y = listY + i * ROW;
            icons.add(new Object[]{o.get("item").getAsString(), x, y + 1});
            EditBox price = num(x + 134, y + 2, 56, fmt(o, "price"), v -> {
                if (v.isEmpty()) return;
                o.addProperty("price", Double.parseDouble(v));
            });
            EditBox sell = num(x + 194, y + 2, 50, fmt(o, "sellPrice"), v -> {
                if (v.isEmpty()) o.remove("sellPrice");
                else o.addProperty("sellPrice", Double.parseDouble(v));
            });
            rowWidgets.add(price);
            rowWidgets.add(sell);
            boolean buy = !o.has("buy") || o.get("buy").getAsBoolean();
            boolean sl = !o.has("sell") || o.get("sell").getAsBoolean();
            Button bb = Button.builder(Component.translatable(buy ? "rtshop.shopedit.buy_on" : "rtshop.shopedit.buy_off"), b -> {
                o.addProperty("buy", !(!o.has("buy") || o.get("buy").getAsBoolean()));
                dirty = true;
                rebuildRows();
            }).bounds(x + 248, y, 34, 18).build();
            Button sbn = Button.builder(Component.translatable(sl ? "rtshop.shopedit.sell_on" : "rtshop.shopedit.sell_off"), b -> {
                o.addProperty("sell", !(!o.has("sell") || o.get("sell").getAsBoolean()));
                dirty = true;
                rebuildRows();
            }).bounds(x + 284, y, 34, 18).build();
            String cat = o.has("category") ? o.get("category").getAsString() : "";
            Button cb = Button.builder(Component.literal(Ui.clip(font, categoryName(cat), 40)), b -> {
                List<String> cs = categories();
                if (cs.isEmpty()) return;
                int k = cs.indexOf(cat);
                o.addProperty("category", cs.get((k + 1) % cs.size()));
                dirty = true;
                filter();
                rebuildRows();
            }).bounds(x + 320, y, 44, 18).build();
            Button del = Button.builder(Component.literal("✕"), b -> {
                items().remove(o);
                dirty = true;
                filter();
                rebuildRows();
            }).bounds(x + 366, y, 18, 18).build();
            for (Button b : new Button[]{bb, sbn, cb, del}) {
                addRenderableWidget(b);
                rowWidgets.add(b);
            }
        }
        // 아래 단추
        int by = fy + H - 24;
        Button save = Button.builder(Component.translatable("rtshop.enhedit.save"), b ->
                ShopNet.CHANNEL.sendToServer(new ShopEditActionPacket(ShopEditor.SAVE, shopId, file.toString()))).bounds(fx + W - 248, by, 80, 18).build();
        Button revert = Button.builder(Component.translatable("rtshop.enhedit.revert"), b ->
                ShopNet.CHANNEL.sendToServer(new ShopEditActionPacket(ShopEditor.OPEN, shopId, ""))).bounds(fx + W - 164, by, 76, 18).build();
        Button close = Button.builder(Component.translatable("rtshop.enhodds.close"), b -> onClose()).bounds(fx + W - 84, by, 76, 18).build();
        for (Button b : new Button[]{save, revert, close}) {
            addRenderableWidget(b);
            rowWidgets.add(b);
        }
    }

    private EditBox num(int x, int y, int w, String value, java.util.function.Consumer<String> set) {
        EditBox b = new EditBox(font, x, y, w, 14, Component.empty());
        b.setMaxLength(12);
        b.setValue(value);
        b.setFilter(s -> s.isEmpty() || s.matches("[0-9]*\\.?[0-9]*"));
        b.setResponder(v -> {
            try {
                set.accept(v);
                dirty = true;
            } catch (NumberFormatException ignored) {
            }
        });
        addRenderableWidget(b);
        return b;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (mx > fx + 96) {
            scroll = Math.max(0, scroll - (int) Math.signum(delta) * 3);
            rebuildRows();
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        Ui.woodFrame(g, fx, fy, W, H);
        Ui.bevel(g, fx + 96, fy + 44, W - 104, H - 72, Ui.C_PARCH, Ui.C_PARCH_HI, Ui.C_PARCH_SHADE);
        g.drawString(font, title, fx + 10, fy + 8, Ui.C_GOLD, true);
        if (dirty) g.drawString(font, Component.translatable("rtshop.enhedit.unsaved"), fx + 14 + font.width(title), fy + 8, 0xFFFF9050, true);
        int x = fx + 100;
        g.drawString(font, Component.translatable("rtshop.shopedit.col_item"), x + 20, listY - 9, Ui.C_INK, false);
        g.drawString(font, Component.translatable("rtshop.shopedit.col_price"), x + 134, listY - 9, Ui.C_INK, false);
        g.drawString(font, Component.translatable("rtshop.shopedit.col_sell"), x + 194, listY - 9, Ui.C_INK, false);
        for (Object[] ic : icons) {
            Item it = item((String) ic[0]);
            int ix = (int) ic[1], iy = (int) ic[2];
            g.renderItem(new ItemStack(it), ix, iy);
            g.drawString(font, Ui.clip(font, it == Items.BARRIER ? (String) ic[0] : it.getDescription().getString(), 110), ix + 20, iy + 4, Ui.C_INK, false);
        }
        String count = Component.translatable("rtshop.shopedit.count", rows.size(), items().size()).getString();
        g.drawString(font, count, fx + 100, fy + H - 18, Ui.C_INK_SOFT, false);
        super.render(g, mx, my, partial);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public int rowsForTest() {
        return rows.size();
    }
}

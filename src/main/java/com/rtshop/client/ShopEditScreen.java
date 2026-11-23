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
    /** 열 위치: 왼쪽 분류 단추 너비, 목록 시작 x, 각 열의 x (오른쪽 끝에서부터 계산해 좁은 화면에서도 잘리지 않는다). */
    private int catW, x0, xName, nameW, xPrice, xSell, xBuy, xSellT, xCat, xDel, toggleW, catBtnW;
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

    /** 자동 규칙으로 진열된 상품 (서버가 "_auto" 로 보내 준다). 고치면 직접 항목으로 옮긴다. */
    private JsonArray autos() {
        if (!file.has("_auto")) file.add("_auto", new JsonArray());
        return file.getAsJsonArray("_auto");
    }

    private static boolean isAuto(JsonObject o) {
        return o.has("_auto") && o.get("_auto").getAsBoolean();
    }

    /** 자동 상품을 고쳤다: 직접 항목으로 옮긴다 (그래야 저장된다). */
    private void promote(JsonObject o) {
        if (!isAuto(o)) return;
        o.remove("_auto");
        autos().remove(o);
        items().add(o);
        dirty = true;
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
        List<JsonElement> all = new ArrayList<>();
        items().forEach(all::add);
        autos().forEach(all::add);
        for (JsonElement e : all) {
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
        layoutColumns();
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
            }).bounds(fx + 8, cy, catW, 16).build();
            b.active = !c.equals(category);
            addRenderableWidget(b);
            cy += 17;
            if (cy > fy + H - 50) break;
        }
        int addW = Math.max(60, font.width(Component.translatable("rtshop.shopedit.add")) + 12);
        int sbW = Math.max(60, Math.min(160, fx + W - 12 - addW - 6 - x0));
        EditBox sb = new EditBox(font, x0, fy + 26, sbW, 14, Component.empty());
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
                }))).bounds(x0 + sbW + 6, fy + 25, addW, 16).build());
        listY = fy + 60;
        visible = Math.max(1, (fy + H - 30 - listY) / ROW);
        rebuildRows();
    }

    private final List<net.minecraft.client.gui.components.AbstractWidget> rowWidgets = new ArrayList<>();

    private void rebuildRows() {
        for (var w : rowWidgets) removeWidget(w);
        rowWidgets.clear();
        icons.clear();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - visible)));
        for (int i = 0; i < visible && i + scroll < rows.size(); i++) {
            JsonObject o = rows.get(i + scroll);
            int y = listY + i * ROW;
            icons.add(new Object[]{o.get("item").getAsString(), x0, y + 1});
            icons.get(icons.size() - 1)[0] = o.get("item").getAsString() + (isAuto(o) ? "\u0000auto" : "");
            EditBox price = num(xPrice, y + 2, xSell - 3 - xPrice, fmt(o, "price"), v -> {
                if (v.isEmpty()) return;
                if (v.equals(fmt(o, "price"))) return;
                o.addProperty("price", Double.parseDouble(v));
                promote(o);
            });
            EditBox sell = num(xSell, y + 2, xBuy - 4 - xSell, fmt(o, "sellPrice"), v -> {
                if (v.equals(fmt(o, "sellPrice"))) return;
                if (v.isEmpty()) o.remove("sellPrice");
                else o.addProperty("sellPrice", Double.parseDouble(v));
                promote(o);
            });
            rowWidgets.add(price);
            rowWidgets.add(sell);
            boolean buy = !o.has("buy") || o.get("buy").getAsBoolean();
            boolean sl = !o.has("sell") || o.get("sell").getAsBoolean();
            Button bb = Button.builder(Component.translatable(buy ? "rtshop.shopedit.buy_on" : "rtshop.shopedit.buy_off"), b -> {
                o.addProperty("buy", !(!o.has("buy") || o.get("buy").getAsBoolean()));
                promote(o);
                dirty = true;
                rebuildRows();
            }).bounds(xBuy, y, toggleW, 18).build();
            Button sbn = Button.builder(Component.translatable(sl ? "rtshop.shopedit.sell_on" : "rtshop.shopedit.sell_off"), b -> {
                o.addProperty("sell", !(!o.has("sell") || o.get("sell").getAsBoolean()));
                promote(o);
                dirty = true;
                rebuildRows();
            }).bounds(xSellT, y, toggleW, 18).build();
            String cat = o.has("category") ? o.get("category").getAsString() : "";
            Button cb = Button.builder(Component.literal(Ui.clip(font, categoryName(cat), catBtnW - 6)), b -> {
                List<String> cs = categories();
                if (cs.isEmpty()) return;
                int k = cs.indexOf(cat);
                o.addProperty("category", cs.get((k + 1) % cs.size()));
                promote(o);
                dirty = true;
                filter();
                rebuildRows();
            }).bounds(xCat, y, catBtnW, 18).build();
            Button del = Button.builder(Component.literal("✕"), b -> {
                if (isAuto(o)) {
                    // 자동 상품은 목록에서 빼도록 exclude 에 적는다
                    if (!file.has("exclude")) file.add("exclude", new JsonArray());
                    file.getAsJsonArray("exclude").add(o.get("item").getAsString());
                    autos().remove(o);
                }
                items().remove(o);
                dirty = true;
                filter();
                rebuildRows();
            }).bounds(xDel, y, 16, 18).build();
            for (Button b : new Button[]{bb, sbn, cb, del}) {
                addRenderableWidget(b);
                rowWidgets.add(b);
            }
        }
        // 아래 단추
        int by = fy + H - 24;
        int[] bw = footerWidths();
        int bx = fx + W - 8;
        Button close = Button.builder(Component.translatable("rtshop.enhodds.close"), b -> onClose()).bounds(bx - bw[2], by, bw[2], 18).build();
        bx -= bw[2] + 4;
        Button revert = Button.builder(Component.translatable("rtshop.enhedit.revert"), b ->
                ShopNet.CHANNEL.sendToServer(new ShopEditActionPacket(ShopEditor.OPEN, shopId, ""))).bounds(bx - bw[1], by, bw[1], 18).build();
        bx -= bw[1] + 4;
        Button save = Button.builder(Component.translatable("rtshop.enhedit.save"), b ->
                ShopNet.CHANNEL.sendToServer(new ShopEditActionPacket(ShopEditor.SAVE, shopId, file.toString()))).bounds(bx - bw[0], by, bw[0], 18).build();
        for (Button b : new Button[]{save, revert, close}) {
            addRenderableWidget(b);
            rowWidgets.add(b);
        }
    }

    /** 화면 너비에 맞춰 열 위치를 정한다. 오른쪽 끝(✕)부터 왼쪽으로 쌓고, 남는 너비를 이름 칸에 준다. */
    private void layoutColumns() {
        catW = W < 440 ? 66 : 84;
        x0 = fx + catW + 16;
        int right = fx + W - 12;
        toggleW = Math.max(28, Math.max(font.width(Component.translatable("rtshop.shopedit.buy_on")),
                font.width(Component.translatable("rtshop.shopedit.sell_off"))) + 8);
        catBtnW = W < 440 ? 38 : 46;
        xDel = right - 16;
        xCat = xDel - 2 - catBtnW;
        xSellT = xCat - 3 - toggleW;
        xBuy = xSellT - 2 - toggleW;
        int numW = W < 440 ? 42 : 50;
        xSell = xBuy - 4 - numW;
        xPrice = xSell - 3 - numW;
        xName = x0 + 20;
        nameW = Math.max(30, xPrice - 4 - xName);
    }

    private int[] footerWidths() {
        int[] w = new int[3];
        String[] keys = {"rtshop.enhedit.save", "rtshop.enhedit.revert", "rtshop.enhodds.close"};
        for (int i = 0; i < 3; i++) w[i] = Math.max(56, font.width(Component.translatable(keys[i])) + 14);
        return w;
    }

    /** 테스트: 모든 단추와 칸이 창 안에 있는가. */
    public boolean fitsForTest() {
        for (var r : children()) {
            if (r instanceof net.minecraft.client.gui.components.AbstractWidget w
                    && (w.getX() < fx || w.getX() + w.getWidth() > fx + W || w.getY() + w.getHeight() > fy + H)) {
                return false;
            }
        }
        return fx >= 0 && fx + W <= width;
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
        if (mx > x0 - 4) {
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
        Ui.bevel(g, x0 - 4, fy + 45, fx + W - 8 - (x0 - 4), H - 73, Ui.C_PARCH, Ui.C_PARCH_HI, Ui.C_PARCH_SHADE);
        String t = Ui.clip(font, title.getString(), W - 20 - (dirty ? font.width(Component.translatable("rtshop.enhedit.unsaved")) + 6 : 0));
        g.drawString(font, t, fx + 10, fy + 8, Ui.C_GOLD, true);
        if (dirty) g.drawString(font, Component.translatable("rtshop.enhedit.unsaved"), fx + 16 + font.width(t), fy + 8, 0xFFFF9050, true);
        int hy = fy + 49;
        g.drawString(font, Ui.clip(font, Component.translatable("rtshop.shopedit.col_item").getString(), nameW), xName, hy, Ui.C_INK, false);
        g.drawString(font, Ui.clip(font, Component.translatable("rtshop.shopedit.col_price").getString(), xSell - xPrice - 2), xPrice, hy, Ui.C_INK, false);
        g.drawString(font, Ui.clip(font, Component.translatable("rtshop.shopedit.col_sell").getString(), xBuy - xSell - 2), xSell, hy, Ui.C_INK, false);
        g.fill(x0, listY - 2, fx + W - 12, listY - 1, 0x40000000);
        String autoTag = Component.translatable("rtshop.shopedit.auto_tag").getString();
        for (Object[] ic : icons) {
            String raw = (String) ic[0];
            boolean auto = raw.endsWith("\u0000auto");
            String id = auto ? raw.substring(0, raw.length() - 5) : raw;
            Item it = item(id);
            int ix = (int) ic[1], iy = (int) ic[2];
            g.renderItem(new ItemStack(it), ix, iy);
            int tagW = auto ? font.width(autoTag) + 4 : 0;
            g.drawString(font, Ui.clip(font, it == Items.BARRIER ? id : it.getDescription().getString(), nameW - tagW), ix + 20, iy + 4,
                    auto ? Ui.C_INK_SOFT : Ui.C_INK, false);
            if (auto) {
                int tx = ix + 20 + nameW - tagW + 2;
                g.fill(tx - 1, iy + 2, tx + tagW - 3, iy + 13, 0x40306090);
                g.drawString(font, autoTag, tx, iy + 4, 0xFF2A5A8A, false);
            }
        }
        int[] bw = footerWidths();
        int countW = fx + W - 8 - bw[0] - bw[1] - bw[2] - 12 - 4 - (fx + 10);
        String count = Ui.clip(font, Component.translatable("rtshop.shopedit.count", rows.size(), items().size() + autos().size()).getString(), countW);
        g.drawString(font, count, fx + 10, fy + H - 22, Ui.C_PARCH, true);
        String hint = Component.translatable("rtshop.shopedit.hint").getString();
        g.pose().pushPose();
        g.pose().translate(fx + 10, fy + H - 11, 0);
        g.pose().scale(0.75F, 0.75F, 1);
        g.drawString(font, Ui.clip(font, hint, (int) (countW / 0.75F)), 0, 0, Ui.C_PARCH_SHADE, false);
        g.pose().popPose();
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

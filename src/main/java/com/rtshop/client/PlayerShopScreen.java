package com.rtshop.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.net.PShopActionPacket;
import com.rtshop.net.ShopNet;
import com.rtshop.pshop.PlayerShops;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 플레이어 상점 화면 (NPC 상점과 비슷한 모양).
 * 손님: 진열된 품목 · 개당 가격 · 재고를 보고 수량을 정해 산다. 오른쪽에 같은 물건의 NPC 상점 가격이 함께 나온다.
 * 주인: [진열대] 탭에서 가격 변경 · 채우기 · 꺼내기 · 내리기, [가방에서 올리기] 탭에서 가방의 물건을 골라 가격 · 수량을 정해 올린다.
 * 올릴 때 그 물건을 파는 NPC 상점과 그 가격(판매가 · 매입가)을 보고 비교할 수 있다.
 */
public class PlayerShopScreen extends Screen {
    private static final int H = 236, ROW = 22, LIST_W = 206;
    /** 창 너비: 화면이 넓으면 오른쪽 칸을 넓혀 글자가 잘리지 않게 한다. */
    private int W = 384;
    /** 오른쪽 칸의 "개당 가격/수량" 글자 너비 (입력 칸은 그 뒤에 놓는다). */
    private int labW = 40;

    record Row(int index, ItemStack item, long price, int stock, long sold, int count, JsonArray npc) {
    }

    private BlockPos pos;
    private JsonObject data;
    private final List<Row> listings = new ArrayList<>();
    private final List<Row> bag = new ArrayList<>();
    private boolean owner;
    private int tab; // 0 진열대, 1 가방에서 올리기
    private int sel = -1;
    private int scroll;
    private Component result;
    private int fx, fy, listY, visible;
    private EditBox priceBox, qtyBox, nameBox;
    private String keepPrice = null, keepQty = null;
    private int boxSel = -2, boxTab = -1;

    public PlayerShopScreen(String json) {
        super(Component.translatable("rtshop.pshop.title"));
        read(json);
    }

    /** 서버에서 내용이 왔다: 이미 열린 같은 상점이면 고쳐 그리고, 아니면 새로 연다. */
    public static void onData(String json) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PlayerShopScreen s) {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            BlockPos p = new BlockPos(o.get("x").getAsInt(), o.get("y").getAsInt(), o.get("z").getAsInt());
            if (p.equals(s.pos)) {
                s.read(json);
                s.rebuildWidgets();
                return;
            }
        }
        mc.setScreen(new PlayerShopScreen(json));
    }

    private static ItemStack stack(String snbt) {
        try {
            return ItemStack.of(TagParser.parseTag(snbt));
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    private void read(String json) {
        data = JsonParser.parseString(json).getAsJsonObject();
        pos = new BlockPos(data.get("x").getAsInt(), data.get("y").getAsInt(), data.get("z").getAsInt());
        owner = data.get("isOwner").getAsBoolean();
        listings.clear();
        for (JsonElement e : data.getAsJsonArray("listings")) {
            JsonObject o = e.getAsJsonObject();
            listings.add(new Row(o.get("i").getAsInt(), stack(o.get("item").getAsString()), o.get("price").getAsLong(), o.get("stock").getAsInt(),
                    o.get("sold").getAsLong(), 0, o.getAsJsonArray("npc")));
        }
        bag.clear();
        if (data.has("inventory")) {
            for (JsonElement e : data.getAsJsonArray("inventory")) {
                JsonObject o = e.getAsJsonObject();
                ItemStack st = stack(o.get("item").getAsString());
                boolean dup = false;
                for (Row r : bag) {
                    if (ItemStack.isSameItemSameTags(r.item, st)) dup = true;
                }
                if (!dup) bag.add(new Row(o.get("slot").getAsInt(), st, 0, 0, 0, o.get("count").getAsInt(), o.getAsJsonArray("npc")));
            }
        }
        if (!owner) tab = 0;
        if (data.has("result")) {
            try {
                result = Component.Serializer.fromJson(data.get("result").getAsString());
            } catch (Exception ex) {
                result = null;
            }
        }
        List<Row> rows = rows();
        if (sel >= rows.size()) sel = rows.size() - 1;
    }

    private List<Row> rows() {
        return tab == 1 ? bag : listings;
    }

    private Row selected() {
        List<Row> rows = rows();
        return sel >= 0 && sel < rows.size() ? rows.get(sel) : null;
    }

    private String shopName() {
        String n = data.get("name").getAsString();
        return n.isEmpty() ? Component.translatable("rtshop.pshop.default_name", data.get("owner").getAsString()).getString() : n;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        W = Math.max(384, Math.min(456, this.width - 12));
        fx = (this.width - W) / 2;
        fy = (this.height - H) / 2;
        labW = Math.max(36, Math.max(this.font.width(Component.translatable("rtshop.pshop.price")),
                this.font.width(Component.translatable("rtshop.pshop.qty"))) + 8);
        listY = fy + (owner ? 44 : 28);
        visible = (fy + H - 26 - listY) / ROW;
        int px = fx + LIST_W + 14, pw = W - LIST_W - 20;
        if (owner) {
            addRenderableWidget(Button.builder(Component.translatable("rtshop.pshop.tab_stand", listings.size(), com.rtshop.pshop.PlayerShopBlockEntity.MAX_LISTINGS),
                    b -> switchTab(0)).bounds(fx + 6, fy + 24, 100, 16).build()).active = tab != 0;
            addRenderableWidget(Button.builder(Component.translatable("rtshop.pshop.tab_bag"), b -> switchTab(1))
                    .bounds(fx + 108, fy + 24, 104, 16).build()).active = tab != 1;
        }
        Row r = selected();
        int y = fy + H - 98;
        if (r != null) {
            if (!owner) {
                qtyBox = box(px + labW, y + 22, 50, keepQty == null ? "1" : keepQty);
                addRenderableWidget(Button.builder(Component.translatable("rtshop.pshop.max"), b -> qtyBox.setValue(Integer.toString(maxBuy(r))))
                        .bounds(px + labW + 54, y + 22, pw - labW - 58, 16).build());
                addRenderableWidget(Button.builder(Component.translatable("rtshop.pshop.buy"), b -> buy(r)).bounds(px + 4, y + 44, pw - 8, 18).build());
            } else if (tab == 0) {
                int bx = px + labW + 58, bw = pw - labW - 62;
                priceBox = box(px + labW, y, 54, keepPrice == null ? Long.toString(r.price) : keepPrice);
                addRenderableWidget(Button.builder(Component.translatable("rtshop.pshop.set_price"), b -> act(PlayerShops.PRICE, "{\"i\":" + r.index
                        + ",\"price\":" + num(priceBox) + "}")).bounds(bx, y, bw, 16).build());
                qtyBox = box(px + labW, y + 20, 54, keepQty == null ? "1" : keepQty);
                addRenderableWidget(Button.builder(Component.translatable("rtshop.pshop.restock"), b -> act(PlayerShops.RESTOCK, "{\"i\":" + r.index
                        + ",\"n\":" + num(qtyBox) + "}")).bounds(bx, y + 20, bw / 2, 16).build());
                addRenderableWidget(Button.builder(Component.translatable("rtshop.pshop.take"), b -> act(PlayerShops.TAKE, "{\"i\":" + r.index
                        + ",\"n\":" + num(qtyBox) + "}")).bounds(bx + bw / 2, y + 20, bw - bw / 2, 16).build());
                addRenderableWidget(Button.builder(Component.translatable("rtshop.pshop.remove"), b -> {
                    sel = -1;
                    act(PlayerShops.REMOVE, "{\"i\":" + r.index + "}");
                }).bounds(px + 4, y + 44, pw - 8, 18).build());
            } else {
                priceBox = box(px + labW, y, 54, keepPrice == null ? Long.toString(suggest(r)) : keepPrice);
                qtyBox = box(px + labW, y + 20, 54, keepQty == null ? Integer.toString(Math.min(r.count, com.rtshop.pshop.PlayerShopBlockEntity.MAX_STOCK)) : keepQty);
                addRenderableWidget(Button.builder(Component.translatable("rtshop.pshop.all"), b -> qtyBox.setValue(Integer.toString(r.count)))
                        .bounds(px + labW + 58, y + 20, pw - labW - 62, 16).build());
                addRenderableWidget(Button.builder(Component.translatable("rtshop.pshop.add"), b -> {
                    act(PlayerShops.ADD, "{\"slot\":" + r.index + ",\"price\":" + num(priceBox) + ",\"n\":" + num(qtyBox) + "}");
                    tab = 0;
                    sel = -1;
                }).bounds(px + 4, y + 44, pw - 8, 18).build());
            }
        }
        keepPrice = null;
        keepQty = null;
        boxSel = sel;
        boxTab = tab;
        if (owner) {
            int renW = Math.max(40, this.font.width(Component.translatable("rtshop.pshop.rename")) + 12);
            nameBox = new EditBox(this.font, fx + 220, fy + 25, W - 226 - renW - 6, 14, Component.empty());
            nameBox.setMaxLength(32);
            nameBox.setValue(data.get("name").getAsString());
            nameBox.setHint(Component.translatable("rtshop.pshop.name_hint"));
            addRenderableWidget(nameBox);
            addRenderableWidget(Button.builder(Component.translatable("rtshop.pshop.rename"), b -> {
                JsonObject o = new JsonObject();
                o.addProperty("name", nameBox.getValue());
                act(PlayerShops.RENAME, o.toString());
            }).bounds(fx + W - 6 - renW, fy + 24, renW, 16).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(fx + W - 56, fy + H - 22, 50, 16).build());
    }

    private EditBox box(int x, int y, int w, String v) {
        EditBox e = new EditBox(this.font, x, y + 1, w, 14, Component.empty());
        e.setMaxLength(9);
        e.setFilter(s -> s.matches("[0-9]*"));
        e.setValue(v);
        addRenderableWidget(e);
        return e;
    }

    private static long num(EditBox b) {
        try {
            return b == null || b.getValue().isEmpty() ? 0 : Long.parseLong(b.getValue());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** 올릴 때 처음 넣어 두는 가격: NPC 판매가가 있으면 그 값, 없으면 NPC 매입가의 2배, 그것도 없으면 10. */
    private static long suggest(Row r) {
        for (JsonElement e : r.npc) {
            JsonObject o = e.getAsJsonObject();
            if (o.get("buyable").getAsBoolean() && o.get("price").getAsLong() > 0) return o.get("price").getAsLong();
        }
        for (JsonElement e : r.npc) {
            JsonObject o = e.getAsJsonObject();
            if (o.get("sellable").getAsBoolean() && o.get("sell").getAsDouble() > 0) return Math.max(1, Math.round(o.get("sell").getAsDouble() * 2));
        }
        return 10;
    }

    private int maxBuy(Row r) {
        long wallet = data.get("wallet").getAsLong();
        return (int) Math.max(0, Math.min(r.stock, r.price <= 0 ? r.stock : wallet / r.price));
    }

    private void buy(Row r) {
        act(PlayerShops.BUY, "{\"i\":" + r.index + ",\"n\":" + num(qtyBox) + "}");
    }

    private void act(int action, String json) {
        ShopNet.CHANNEL.sendToServer(new PShopActionPacket(pos, action, json));
    }

    private void switchTab(int t) {
        tab = t;
        sel = -1;
        scroll = 0;
        rebuildWidgets();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        List<Row> rows = rows();
        if (Ui.inside(mx, my, fx + 6, listY, LIST_W, visible * ROW)) {
            int i = scroll + (int) ((my - listY) / ROW);
            if (i >= 0 && i < rows.size()) {
                sel = i;
                rebuildWidgets();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int max = Math.max(0, rows().size() - visible);
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(delta)));
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        Ui.woodFrame(g, fx, fy, W, H);
        g.fill(fx + 5, fy + 4, fx + W - 5, fy + 19, Ui.C_WOOD_DARK);
        g.fill(fx + 5, fy + 18, fx + W - 5, fy + 19, Ui.C_GOLD_DARK);
        Ui.coin(g, fx + 9, fy + 4, 14);
        String right = Component.translatable("rtshop.pshop.wallet", Ui.num(data.get("wallet").getAsLong())).getString();
        g.drawString(this.font, right, fx + W - 8 - this.font.width(right), fy + 8, Ui.C_CREAM, false);
        int titleRight = fx + W - 8 - this.font.width(right) - 10;
        if (!owner) {
            String by = Ui.clip(this.font, Component.translatable("rtshop.pshop.owner", data.get("owner").getAsString()).getString(), 120);
            titleRight -= this.font.width(by);
            g.drawString(this.font, by, titleRight, fy + 8, Ui.C_PARCH_SHADE, false);
            titleRight -= 10;
        }
        g.drawString(this.font, Ui.clip(this.font, shopName(), Math.min(200, titleRight - (fx + 27))), fx + 27, fy + 8, Ui.C_GOLD, true);
        // 목록
        int lx = fx + 6;
        Ui.bevel(g, lx - 1, listY - 1, LIST_W + 2, visible * ROW + 2, Ui.C_PARCH, Ui.C_PARCH_SHADE, Ui.C_PARCH_HI);
        List<Row> rows = rows();
        if (rows.isEmpty()) {
            Component empty = Component.translatable(tab == 1 ? "rtshop.pshop.bag_empty" : owner ? "rtshop.pshop.empty_owner" : "rtshop.pshop.empty");
            g.drawWordWrap(this.font, empty, lx + 8, listY + 10, LIST_W - 16, Ui.C_INK_SOFT);
        }
        for (int k = 0; k < visible && scroll + k < rows.size(); k++) {
            int i = scroll + k;
            Row r = rows.get(i);
            int y = listY + k * ROW;
            boolean hov = Ui.inside(mx, my, lx, y, LIST_W, ROW);
            if (i == sel) g.fill(lx, y, lx + LIST_W, y + ROW, Ui.C_PARCH_HI);
            else if (hov) g.fill(lx, y, lx + LIST_W, y + ROW, 0x22FFFFFF);
            if (i == sel) g.fill(lx, y, lx + 2, y + ROW, Ui.C_GOLD_DARK);
            g.fill(lx + 2, y + ROW - 1, lx + LIST_W - 2, y + ROW, 0x22000000);
            g.renderItem(r.item, lx + 4, y + 3);
            g.drawString(this.font, Ui.clip(this.font, r.item.getHoverName().getString(), 96), lx + 24, y + 3, Ui.C_INK, false);
            if (tab == 1) {
                g.drawString(this.font, Component.translatable("rtshop.pshop.have", r.count), lx + 24, y + 12, Ui.C_INK_SOFT, false);
            } else {
                Component st = r.stock <= 0 ? Component.translatable("rtshop.pshop.sold_out_short")
                        : Component.translatable("rtshop.pshop.stock", r.stock);
                g.drawString(this.font, st, lx + 24, y + 12, r.stock <= 0 ? Ui.C_RED : Ui.C_INK_SOFT, false);
                String pr = Ui.num(r.price) + "G";
                Ui.coin(g, lx + LIST_W - 16 - this.font.width(pr), y + 6, 9);
                g.drawString(this.font, pr, lx + LIST_W - 5 - this.font.width(pr), y + 7, Ui.C_INK, false);
            }
        }
        if (rows.size() > visible) {
            int sb = lx + LIST_W - 3, vh = visible * ROW;
            int th = Math.max(10, vh * visible / rows.size());
            int ty = listY + (vh - th) * scroll / Math.max(1, rows.size() - visible);
            g.fill(sb, ty, sb + 2, ty + th, Ui.C_WOOD);
        }
        // 오른쪽: 고른 품목 + NPC 비교
        int px = fx + LIST_W + 14, pw = W - LIST_W - 20, py = listY;
        Ui.bevel(g, px - 1, py - 1, pw + 2, fy + H - 26 - py + 2, Ui.C_PARCH, Ui.C_PARCH_SHADE, Ui.C_PARCH_HI);
        Row r = selected();
        if (r == null) {
            g.drawWordWrap(this.font, Component.translatable(owner ? "rtshop.pshop.pick_owner" : "rtshop.pshop.pick"), px + 6, py + 8, pw - 12, Ui.C_INK_SOFT);
        } else {
            g.renderItem(r.item, px + 4, py + 4);
            g.drawString(this.font, Ui.clip(this.font, r.item.getHoverName().getString(), pw - 28), px + 24, py + 4, Ui.C_INK, false);
            String line2 = tab == 1 ? Component.translatable("rtshop.pshop.have", r.count).getString()
                    : Component.translatable("rtshop.pshop.price_stock", Ui.num(r.price), r.stock).getString()
                    + (owner ? " · " + Component.translatable("rtshop.pshop.sold", r.sold).getString() : "");
            g.drawString(this.font, Ui.clip(this.font, line2, pw - 28), px + 24, py + 13, Ui.C_INK_SOFT, false);
            int ny = py + 28;
            g.drawString(this.font, Component.translatable("rtshop.pshop.npc_title"), px + 5, ny, Ui.C_WOOD, false);
            ny += 11;
            // 가격 비교 줄은 아래쪽 비교 문구(y - 10) 위에서 끝나야 한다
            int limit = fy + H - 98 - 12;
            if (r.npc.isEmpty()) {
                for (var line : this.font.split(Component.translatable("rtshop.pshop.npc_none"), pw - 12)) {
                    if (ny + 9 > limit) break;
                    g.drawString(this.font, line, px + 7, ny, Ui.C_INK_SOFT, false);
                    ny += 10;
                }
            }
            int shown = 0;
            for (JsonElement e : r.npc) {
                JsonObject o = e.getAsJsonObject();
                StringBuilder sb = new StringBuilder();
                if (o.get("buyable").getAsBoolean()) sb.append(Component.translatable("rtshop.pshop.npc_buy", Ui.num(o.get("price").getAsLong())).getString());
                if (o.get("sellable").getAsBoolean()) {
                    if (o.get("buyable").getAsBoolean()) sb.append(" · ");
                    sb.append(Component.translatable("rtshop.pshop.npc_sell", Ui.num(Math.round(o.get("sell").getAsDouble()))).getString());
                }
                String shop = o.get("shop").getAsString() + ": ";
                String one = shop + sb;
                boolean fits = this.font.width(one) <= pw - 12;
                int need = fits ? 10 : 20;
                if (ny + need - 1 > limit) break;
                if (fits) {
                    g.drawString(this.font, one, px + 7, ny, Ui.C_INK, false);
                } else {
                    // 한 줄에 다 안 들어가면 상점 이름과 가격을 두 줄로 나눈다
                    g.drawString(this.font, Ui.clip(this.font, shop, pw - 12), px + 7, ny, Ui.C_INK, false);
                    g.drawString(this.font, Ui.clip(this.font, sb.toString(), pw - 20), px + 13, ny + 10, Ui.C_INK, false);
                }
                ny += need;
                shown++;
            }
            if (r.npc.size() > shown && shown > 0) {
                String more = "… +" + (r.npc.size() - shown);
                g.drawString(this.font, more, px + pw - 6 - this.font.width(more), py + 28, Ui.C_INK_SOFT, false);
            }
            int y = fy + H - 98;
            if (!owner) {
                long n = num(qtyBox);
                long cost = n * r.price;
                g.drawString(this.font, Ui.clip(this.font, Component.translatable("rtshop.pshop.total", Ui.num(cost)).getString(), pw - 10), px + 5, y + 6,
                        cost > data.get("wallet").getAsLong() ? Ui.C_RED : Ui.C_INK, false);
                g.drawString(this.font, Component.translatable("rtshop.pshop.qty"), px + 5, y + 26, Ui.C_INK, false);
                // NPC 판매가와 비교
                compare(g, r, r.price, px + 5, y - 8);
            } else {
                g.drawString(this.font, Component.translatable("rtshop.pshop.price"), px + 5, y + 4, Ui.C_INK, false);
                g.drawString(this.font, Component.translatable("rtshop.pshop.qty"), px + 5, y + 24, Ui.C_INK, false);
                compare(g, r, num(priceBox), px + 5, y - 10);
            }
        }
        if (result != null) {
            g.drawString(this.font, Ui.clip(this.font, result.getString(), W - 80), fx + 8, fy + H - 18,
                    result.getStyle().getColor() != null ? result.getStyle().getColor().getValue() | 0xFF000000 : Ui.C_CREAM, false);
        } else if (owner) {
            String s = Component.translatable("rtshop.pshop.total_sales", Ui.num(data.get("totalSales").getAsLong())).getString();
            g.drawString(this.font, s, fx + 8, fy + H - 18, Ui.C_CREAM, false);
        }
        super.render(g, mx, my, pt);
        for (int k = 0; k < visible && scroll + k < rows.size(); k++) {
            int y = listY + k * ROW;
            if (Ui.inside(mx, my, lx + 4, y + 3, 16, 16)) g.renderTooltip(this.font, rows.get(scroll + k).item, mx, my);
        }
    }

    /** 가격이 NPC 판매가보다 싼지 비싼지 한 줄로. */
    private void compare(GuiGraphics g, Row r, long price, int x, int y) {
        long npc = -1;
        for (JsonElement e : r.npc) {
            JsonObject o = e.getAsJsonObject();
            if (o.get("buyable").getAsBoolean() && (npc < 0 || o.get("price").getAsLong() < npc)) npc = o.get("price").getAsLong();
        }
        if (npc <= 0 || price <= 0) return;
        long pct = Math.round((price - npc) * 100.0 / npc);
        Component c = pct < 0 ? Component.translatable("rtshop.pshop.cheaper", -pct) : pct > 0 ? Component.translatable("rtshop.pshop.dearer", pct)
                : Component.translatable("rtshop.pshop.same");
        int pw = W - LIST_W - 20;
        g.drawString(this.font, Ui.clip(this.font, c.getString(), pw - 10), x, y, pct <= 0 ? Ui.C_GREEN : Ui.C_RED, false);
    }

    @Override
    public void tick() {
        if (priceBox != null) priceBox.tick();
        if (qtyBox != null) qtyBox.tick();
        if (nameBox != null) nameBox.tick();
    }

    @Override
    protected void rebuildWidgets() {
        boolean same = sel == boxSel && tab == boxTab;
        keepPrice = same && priceBox != null ? priceBox.getValue() : null;
        keepQty = same && qtyBox != null ? qtyBox.getValue() : null;
        priceBox = null;
        qtyBox = null;
        nameBox = null;
        super.rebuildWidgets();
    }

    // ------------------------------------------------------------------ 시험용

    public boolean ownerForTest() {
        return owner;
    }

    public int listingsForTest() {
        return listings.size();
    }

    public int stockForTest(int i) {
        return i < listings.size() ? listings.get(i).stock : -1;
    }

    public int npcCountForTest() {
        Row r = selected();
        return r == null ? -1 : r.npc.size();
    }

    public void bagSelectForTest(net.minecraft.world.item.Item item) {
        tab = 1;
        sel = -1;
        for (int i = 0; i < bag.size(); i++) {
            if (bag.get(i).item.is(item)) sel = i;
        }
        rebuildWidgets();
    }

    public void addForTest(long price, int n) {
        if (priceBox != null) priceBox.setValue(Long.toString(price));
        if (qtyBox != null) qtyBox.setValue(Integer.toString(n));
        Row r = selected();
        if (r != null) {
            act(PlayerShops.ADD, "{\"slot\":" + r.index + ",\"price\":" + price + ",\"n\":" + n + "}");
            tab = 0;
            sel = 0;
        }
    }

    public void selectForTest(int i) {
        sel = i;
        rebuildWidgets();
    }

    public void buyForTest(int n) {
        if (qtyBox != null) qtyBox.setValue(Integer.toString(n));
        Row r = selected();
        if (r != null) buy(r);
    }

    public boolean fitsForTest() {
        for (var c : children()) {
            if (c instanceof net.minecraft.client.gui.components.AbstractWidget w
                    && (w.getX() < fx || w.getX() + w.getWidth() > fx + W || w.getWidth() < 24 && !(w instanceof EditBox))) {
                return false;
            }
        }
        return fx >= 0 && fy >= 0 && fx + W <= this.width && fy + H <= this.height;
    }
}

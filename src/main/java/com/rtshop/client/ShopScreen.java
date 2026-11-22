package com.rtshop.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rtshop.farm.SeedRules;
import com.rtshop.net.ClosePacket;
import com.rtshop.net.SellAllPacket;
import com.rtshop.net.ShopNet;
import com.rtshop.net.TransactionPacket;
import com.rtshop.shop.InvUtil;
import com.rtshop.shop.ShopData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 중세풍(갈색 계열 + 크림색) 상점 화면.
 * - 검색 / 카테고리 / 정렬 / 격자·목록 전환 / 구매·판매 탭
 * - 좌클릭: 수량 입력 창, 우클릭: 10개, Shift+클릭: 한 묶음(판매는 전부)
 * - 판매 탭은 내 인벤토리를 기준으로, 같은 아이템이라도 품질(작물 별 등급, 물고기 무게)에 따라 줄이 나뉜다.
 */
public class ShopScreen extends Screen implements NpcButtons.Host {
    @Override
    public int[] npcButtonSpot() {
        return new int[]{fx + 29 + this.font.width(titleText()) + 10, fy + 7};
    }


    // ---- 레이아웃 상수
    private static final int FRAME_W = 396;
    private static final int FRAME_H = 236;
    private static final int TITLE_H = 24;
    private static final int STATUS_H = 20;
    private static final int CAT_W = 76;
    private static final int CAT_H = 20;
    private static final int CELL = 36;
    private static final int GAP = 2;
    private static final int LIST_H = 22;
    private static final int SB = 6;
    private static final int TAB_W = 44;
    private static final int TAB_H = 16;
    private static final int SORT_W = 66;
    private static final int VIEW_W = 34;
    private static final int ALL_W = 58;

    private static final int C_SHADOW = Ui.C_SHADOW;
    private static final int C_WOOD = Ui.C_WOOD;
    private static final int C_WOOD_HI = Ui.C_WOOD_HI;
    private static final int C_WOOD_DARK = Ui.C_WOOD_DARK;
    private static final int C_GOLD = Ui.C_GOLD;
    private static final int C_GOLD_DARK = Ui.C_GOLD_DARK;
    private static final int C_PARCH = Ui.C_PARCH;
    private static final int C_PARCH_DARK = Ui.C_PARCH_DARK;
    private static final int C_PARCH_SHADE = Ui.C_PARCH_SHADE;
    private static final int C_PARCH_HI = Ui.C_PARCH_HI;
    private static final int C_INK = Ui.C_INK;
    private static final int C_INK_SOFT = Ui.C_INK_SOFT;
    private static final int C_CREAM = Ui.C_CREAM;
    private static final int C_RED = Ui.C_RED;
    private static final int C_GREEN = Ui.C_GREEN;

    private enum Sort {
        DEFAULT("rtshop.sort.default"),
        NAME_ASC("rtshop.sort.name_asc"),
        NAME_DESC("rtshop.sort.name_desc"),
        PRICE_ASC("rtshop.sort.price_asc"),
        PRICE_DESC("rtshop.sort.price_desc");

        final String key;

        Sort(String key) {
            this.key = key;
        }

        Sort next() {
            return values()[(ordinal() + 1) % values().length];
        }

        Sort prev() {
            return values()[(ordinal() + values().length - 1) % values().length];
        }
    }

    /** 화면에 보이는 한 줄(한 칸). */
    private static final class Row {
        final int index;
        final ShopData.Entry entry;
        final ItemStack shown;   // 화면에 그릴 아이템 (판매 탭에서는 내가 가진 실제 아이템)
        final String name;
        double unit;             // 표시 단가
        long owned;              // 판매 탭: 보유 수량
        long key;                // 판매 탭: 품질 구분 키
        double mult = 1.0;       // 판매 탭: 품질 배율
        boolean affordable = true;
        boolean fits = true;
        boolean sellable = true;

        Row(int index, ShopData.Entry entry, ItemStack shown, String name) {
            this.index = index;
            this.entry = entry;
            this.shown = shown;
            this.name = name;
        }
    }

    private final ShopData data;
    private final ItemStack allIcon = new ItemStack(Items.CHEST);
    /** 서버가 창이 열린 뒤에 알려 준 '지금 살 수 있는 품목' (식당 메뉴). null = 상점 데이터 그대로. */
    private java.util.BitSet menu;

    private boolean buyable(int index, ShopData.Entry e) {
        return menu != null ? menu.get(index) : e.buyable;
    }

    /** 메뉴가 하나씩 늘어나는 상점(식당)인가. */
    private boolean menuShop() {
        return !data.emptyNote.isEmpty();
    }

    private int menuCount() {
        int n = 0;
        for (int i = 0; i < data.entries.size(); i++) {
            if (buyable(i, data.entries.get(i))) n++;
        }
        return n;
    }

    private Component titleText() {
        if (!menuShop()) {
            return this.title;
        }
        return Component.literal(data.title + " · ").append(Component.translatable("rtshop.restaurant.menu_count", menuCount(), data.entries.size()));
    }

    public void setMenu(String shopId, int version, int[] buyable) {
        if (!data.id.equals(shopId) || data.version != version) {
            return;
        }
        java.util.BitSet b = new java.util.BitSet(data.entries.size());
        for (int i : buyable) {
            if (i >= 0 && i < data.entries.size()) b.set(i);
        }
        menu = b;
        rebuild();
    }

    public int menuCountForTest() {
        return menuCount();
    }

    public int rowsForTest() {
        return rows.size();
    }

    public boolean sellLockedForTest() {
        return !data.sellLockNote.isEmpty();
    }

    private boolean sellMode = false;
    private int category = -1; // -1 = 전체
    private Sort sort = Sort.DEFAULT;
    private boolean grid = true;
    private int scroll = 0;
    private int catScroll = 0;
    private boolean draggingBar = false;

    private EditBox search;
    private final List<Row> rows = new ArrayList<>();
    private long money = 0;
    private int invSig = 0;

    private Component status = null;
    private boolean statusOk = true;
    private int statusTicks = 0;

    // ---- 수량 입력 창
    private static final int DW = 176;
    private static final int DH = 106;
    private static final long QTY_LIMIT = 36L * InvUtil.MAX_STACK_CAP;
    private EditBox qtyBox;
    private Row dlgRow = null;
    private boolean dlgSell = false;
    private boolean confirmAll = false; // "전부 판매" 확인 창
    private int dx, dy;

    // 레이아웃(init 에서 계산)
    private int fx, fy, fw, fh;
    private int catX, catY, catH;
    private int tbX, tbY, tbW;
    private int searchW, allX, sortX, viewX;
    private int cx, cy, cw, ch;
    private int vx, vy, vw, vh;
    private int statusY;
    private int tabSellX, tabBuyX, tabY;

    public ShopScreen(ShopData data) {
        super(Component.literal(data.title));
        this.data = data;
    }

    // =====================================================================================
    // 초기화 / 상태
    // =====================================================================================

    @Override
    protected void init() {
        fw = Math.min(FRAME_W, this.width - 8);
        fh = Math.min(FRAME_H, this.height - 8);
        fx = (this.width - fw) / 2;
        fy = (this.height - fh) / 2;

        statusY = fy + fh - 6 - STATUS_H;
        catX = fx + 6;
        catY = fy + TITLE_H + 4;
        catH = statusY - 4 - catY;

        tbX = catX + CAT_W + 6;
        tbY = catY;
        tbW = fx + fw - 6 - tbX;
        layoutToolbar();

        cx = tbX;
        cy = tbY + 18 + 4;
        cw = tbW;
        ch = statusY - 4 - cy;

        vx = cx + 3;
        vy = cy + 3;
        vw = cw - 6 - SB - 1;
        vh = ch - 6;

        tabY = fy + 4;
        tabSellX = fx + fw - 8 - TAB_W;
        tabBuyX = tabSellX - TAB_W - 4;

        String old = search != null ? search.getValue() : "";
        search = new EditBox(this.font, tbX + 5, tbY + 5, Math.max(20, searchW - 10), 10, Component.translatable("rtshop.ui.search"));
        search.setBordered(false);
        search.setMaxLength(40);
        search.setTextColor(0xFFF5E6C0);
        search.setValue(old);
        search.setResponder(s -> {
            scroll = 0;
            rebuild();
        });
        this.addRenderableWidget(search);

        // 수량 입력 상자: 창이 열렸을 때만 직접 그리고 직접 입력을 전달한다(위젯 목록에 넣지 않음)
        dx = (this.width - DW) / 2;
        dy = (this.height - DH) / 2;
        String oldQty = qtyBox != null ? qtyBox.getValue() : "1";
        qtyBox = new EditBox(this.font, dx + 66, dy + 48, 44, 10, Component.translatable("rtshop.qty.title_buy"));
        qtyBox.setBordered(false);
        qtyBox.setMaxLength(5);
        qtyBox.setTextColor(0xFFF5E6C0);
        qtyBox.setFilter(t -> t.matches("[0-9]*"));
        qtyBox.setValue(oldQty);
        qtyBox.setFocused(dlgRow != null);

        rebuild();
    }

    /** 판매 탭에서는 "전부 판매" 버튼 자리를 만든다. */
    private void layoutToolbar() {
        int extra = sellMode ? ALL_W + 4 : 0;
        searchW = Math.max(40, tbW - SORT_W - VIEW_W - 8 - extra);
        allX = tbX + searchW + 4;
        sortX = tbX + searchW + 4 + extra;
        viewX = sortX + SORT_W + 4;
        if (search != null) {
            search.setWidth(Math.max(20, searchW - 10));
        }
    }

    @Override
    public boolean isPauseScreen() {
        // 싱글플레이에서 게임이 멈추면 서버가 패킷을 처리하지 못하므로 반드시 false
        return false;
    }

    @Override
    public void removed() {
        super.removed();
        ShopNet.CHANNEL.sendToServer(new ClosePacket());
    }

    public void showResult(Component message, boolean ok) {
        this.status = message;
        this.statusOk = ok;
        this.statusTicks = 80;
        Minecraft mc = Minecraft.getInstance();
        mc.getSoundManager().play(SimpleSoundInstance.forUI(ok ? SoundEvents.EXPERIENCE_ORB_PICKUP : SoundEvents.VILLAGER_NO, 1.0F));
    }

    private void playClick() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public void tick() {
        super.tick();
        if (search != null) {
            search.tick();
        }
        if (statusTicks > 0) {
            statusTicks--;
            if (statusTicks == 0) {
                status = null;
            }
        }
        if (qtyBox != null && dlgRow != null) {
            qtyBox.tick();
        }
        int sig = inventorySignature();
        if (sig != invSig) {
            invSig = sig;
            rebuild();
        }
    }

    private List<ItemStack> inventory() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? List.of() : mc.player.getInventory().items;
    }

    private int inventorySignature() {
        int sig = 7 + Long.hashCode(ClientEconomy.wallet);
        for (ItemStack s : inventory()) {
            if (!s.isEmpty()) {
                sig = sig * 31 + s.getItem().hashCode();
                sig = sig * 31 + s.getCount();
                if (s.hasTag()) {
                    sig = sig * 31 + s.getTag().hashCode();
                }
            } else {
                sig = sig * 31 + 1;
            }
        }
        return sig;
    }

    // =====================================================================================
    // 목록 구성 (검색 / 카테고리 / 정렬 / 탭)
    // =====================================================================================

    private boolean passesFilter(ShopData.Entry e, String name, String catId, String q) {
        if (catId != null && !catId.equals(e.category)) {
            return false;
        }
        if (!q.isEmpty()) {
            String id = String.valueOf(ForgeRegistries.ITEMS.getKey(e.stack.getItem())).toLowerCase(Locale.ROOT);
            return name.toLowerCase(Locale.ROOT).contains(q) || id.contains(q);
        }
        return true;
    }

    private void rebuild() {
        rows.clear();
        List<ItemStack> inv = inventory();
        money = ClientEconomy.wallet;
        String q = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        String catId = (category >= 0 && category < data.categories.size()) ? data.categories.get(category).id : null;

        if (sellMode) {
            // 내 인벤토리에서 이 상점이 사 주는 것을 품질별로 묶는다
            Map<String, Row> groups = new LinkedHashMap<>();
            for (ItemStack s : inv) {
                if (s.isEmpty() || !data.sellLockNote.isEmpty()) {
                    continue; // 이 상점에 팔 수 없는 플레이어 (식당: 요리사만)
                }
                int idx = data.findSellEntry(s);
                if (idx < 0) {
                    continue;
                }
                ShopData.Entry e = data.entries.get(idx);
                long key = data.variantKey(e, s);
                String gk = idx + ":" + key;
                Row r = groups.get(gk);
                if (r == null) {
                    String name = s.getHoverName().getString();
                    if (!passesFilter(e, name, catId, q)) {
                        continue;
                    }
                    ItemStack shown = s.copy();
                    shown.setCount(1);
                    r = new Row(idx, e, shown, name);
                    r.key = key;
                    r.mult = data.qualityMultiplier(e, s);
                    r.unit = key / 100.0;
                    groups.put(gk, r);
                }
                r.owned += s.getCount();
            }
            for (Row r : groups.values()) {
                r.sellable = ShopData.revenue(r.unit, r.owned) >= 1;
                rows.add(r);
            }
        } else {
            for (int i = 0; i < data.entries.size(); i++) {
                ShopData.Entry e = data.entries.get(i);
                if (!buyable(i, e)) {
                    continue;
                }
                String name = e.stack.getHoverName().getString();
                if (!passesFilter(e, name, catId, q)) {
                    continue;
                }
                Row r = new Row(i, e, e.stack, name);
                r.unit = e.price;
                r.affordable = money >= e.price;
                r.fits = InvUtil.capacity(inv, e.stack) >= 1;
                rows.add(r);
            }
        }

        switch (sort) {
            case NAME_ASC -> rows.sort(Comparator.comparing((Row r) -> r.name, String.CASE_INSENSITIVE_ORDER));
            case NAME_DESC -> rows.sort(Comparator.comparing((Row r) -> r.name, String.CASE_INSENSITIVE_ORDER).reversed());
            case PRICE_ASC -> rows.sort(Comparator.comparingDouble((Row r) -> r.unit)
                    .thenComparing((Row r) -> r.name, String.CASE_INSENSITIVE_ORDER));
            case PRICE_DESC -> rows.sort(Comparator.comparingDouble((Row r) -> r.unit).reversed()
                    .thenComparing((Row r) -> r.name, String.CASE_INSENSITIVE_ORDER));
            default -> {
            }
        }
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }

    /** 자동 화면 점검(ClientSelfTest) 전용. */
    ShopData debugData() {
        return data;
    }

    /** 자동 화면 점검(ClientSelfTest) 전용: 탭과 보기 방식을 바꾸고, 필요하면 첫 줄의 수량 입력 창을 연다. */
    void debugView(boolean sell, boolean gridView, boolean dialog) {
        setMode(sell);
        if (this.grid != gridView) {
            this.grid = gridView;
            this.scroll = 0;
            rebuild();
        }
        if (dialog && !rows.isEmpty()) {
            openDialog(rows.get(0));
        }
    }

    private void setMode(boolean sell) {
        if (sellMode != sell) {
            sellMode = sell;
            scroll = 0;
            layoutToolbar();
            rebuild();
        }
    }

    // =====================================================================================
    // 레이아웃 계산
    // =====================================================================================

    private int cols() {
        return grid ? Math.max(1, (vw + GAP) / (CELL + GAP)) : 1;
    }

    private int rowStep() {
        return grid ? CELL + GAP : LIST_H + GAP;
    }

    private int gridX0() {
        int used = cols() * (CELL + GAP) - GAP;
        return vx + Math.max(0, (vw - used) / 2);
    }

    private int totalHeight() {
        int c = cols();
        int lines = (rows.size() + c - 1) / c;
        return Math.max(0, lines * rowStep() - GAP);
    }

    private int maxScroll() {
        return Math.max(0, totalHeight() - vh);
    }

    private int thumbH() {
        int total = totalHeight();
        if (total <= 0) {
            return vh;
        }
        return Math.max(14, Math.min(vh, vh * vh / total));
    }

    private int maxCatScroll() {
        return Math.max(0, (data.categories.size() + 1) * CAT_H + 4 - catH);
    }

    private int rowAt(double mx, double my) {
        if (mx < vx || mx >= vx + vw || my < vy || my >= vy + vh) {
            return -1;
        }
        int c = cols();
        int step = rowStep();
        int relY = (int) my - vy + scroll;
        int line = relY / step;
        int inY = relY % step;
        int col;
        if (grid) {
            int relX = (int) mx - gridX0();
            if (relX < 0) {
                return -1;
            }
            col = relX / (CELL + GAP);
            int inX = relX % (CELL + GAP);
            if (col >= c || inX >= CELL || inY >= CELL) {
                return -1;
            }
        } else {
            col = 0;
            if (inY >= LIST_H) {
                return -1;
            }
        }
        int idx = line * c + col;
        return (idx >= 0 && idx < rows.size()) ? idx : -1;
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return Ui.inside(mx, my, x, y, w, h);
    }

    private boolean popupOpen() {
        return dlgRow != null || confirmAll;
    }

    // =====================================================================================
    // 그리기
    // =====================================================================================

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        this.renderBackground(g);
        final int realMx = mx;
        final int realMy = my;
        if (popupOpen()) {
            // 팝업이 열려 있으면 뒤쪽 화면은 마우스에 반응하지 않게 한다
            mx = -10000;
            my = -10000;
        }
        drawFrame(g, mx, my);
        drawCategories(g, mx, my);
        drawToolbar(g, mx, my);
        int hover = drawContent(g, mx, my);
        drawStatus(g);

        super.render(g, mx, my, partial); // 검색창

        if (search != null && search.getValue().isEmpty() && !search.isFocused()) {
            g.drawString(this.font, Component.translatable("rtshop.ui.search"), tbX + 5, tbY + 5, 0xFF9C8460, false);
        }

        if (dlgRow != null) {
            drawQtyDialog(g, realMx, realMy, partial);
        } else if (confirmAll) {
            drawConfirmAll(g, realMx, realMy);
        } else if (hover >= 0 && hover < rows.size()) {
            drawRowTooltip(g, rows.get(hover), mx, my);
        }
    }

    private void bevel(GuiGraphics g, int x, int y, int w, int h, int fill, int hi, int lo) {
        Ui.bevel(g, x, y, w, h, fill, hi, lo);
    }

    private String clip(String s, int maxWidth) {
        return Ui.clip(this.font, s, maxWidth);
    }

    private void drawFrame(GuiGraphics g, int mx, int my) {
        Ui.woodFrame(g, fx, fy, fw, fh);
        // 제목 줄
        g.fill(fx + 5, fy + 4, fx + fw - 5, fy + TITLE_H - 1, C_WOOD_DARK);
        g.fill(fx + 5, fy + TITLE_H - 2, fx + fw - 5, fy + TITLE_H - 1, C_GOLD_DARK);
        Ui.coin(g, fx + 9, fy + 4, 16);
        g.drawString(this.font, titleText(), fx + 29, fy + 9, C_GOLD, true);

        drawTab(g, mx, my, tabBuyX, Component.translatable("rtshop.ui.buy"), !sellMode);
        drawTab(g, mx, my, tabSellX, Component.translatable("rtshop.ui.sell"), sellMode);
        if (landShop()) {
            int w = plotTabW();
            int x = tabBuyX - 6 - w;
            boolean hov = inside(mx, my, x, tabY, w, TAB_H);
            bevel(g, x, tabY, w, TAB_H, hov ? C_GOLD : C_GOLD_DARK, C_GOLD, C_SHADOW);
            Component label = Component.translatable("rtshop.plotui.open_tab");
            g.drawString(this.font, label, x + (w - this.font.width(label)) / 2, tabY + 4, C_INK, false);
        }
    }

    public String shopIdForEdit() {
        return data.id;
    }

    /** 토지 상인: 문서 외에 구획·성 안 토지를 보는 탭이 붙는다. */
    private boolean landShop() {
        return "land".equals(data.id) || data.entries.stream().anyMatch(e -> e.stack.getItem() instanceof com.rtshop.item.DeedItem);
    }

    private int plotTabW() {
        return this.font.width(Component.translatable("rtshop.plotui.open_tab")) + 14;
    }

    /** 시험용. */
    public void openPlotsForTest() {
        com.rtshop.net.ShopNet.CHANNEL.sendToServer(new com.rtshop.net.PlotActionPacket(com.rtshop.land.PlotBrowser.OPEN, 0));
    }

    private void drawTab(GuiGraphics g, int mx, int my, int x, Component label, boolean selected) {
        boolean hov = inside(mx, my, x, tabY, TAB_W, TAB_H);
        int fill = selected ? C_PARCH : (hov ? C_WOOD_HI : C_WOOD);
        bevel(g, x, tabY, TAB_W, TAB_H, fill, selected ? C_PARCH_HI : C_WOOD_HI, C_SHADOW);
        int color = selected ? C_INK : C_CREAM;
        int tx = x + (TAB_W - this.font.width(label)) / 2;
        g.drawString(this.font, label, tx, tabY + 4, color, false);
    }

    private void drawCategories(GuiGraphics g, int mx, int my) {
        bevel(g, catX, catY, CAT_W, catH, C_PARCH_DARK, C_PARCH_SHADE, C_SHADOW);
        g.enableScissor(catX + 1, catY + 1, catX + CAT_W - 1, catY + catH - 1);
        int count = data.categories.size() + 1;
        for (int p = 0; p < count; p++) {
            int y = catY + 2 + p * CAT_H - catScroll;
            if (y + CAT_H < catY || y > catY + catH) {
                continue;
            }
            boolean selected = (p - 1) == category;
            boolean hov = inside(mx, my, catX + 2, y, CAT_W - 4, CAT_H - 1)
                    && inside(mx, my, catX, catY, CAT_W, catH);
            if (selected) {
                g.fill(catX + 2, y, catX + CAT_W - 2, y + CAT_H - 1, C_PARCH_HI);
                g.fill(catX + 2, y, catX + 4, y + CAT_H - 1, C_GOLD_DARK);
            } else if (hov) {
                g.fill(catX + 2, y, catX + CAT_W - 2, y + CAT_H - 1, 0xFFE6D3A3);
            }
            ItemStack icon = p == 0 ? allIcon : data.categories.get(p - 1).icon;
            g.renderItem(icon, catX + 5, y + 1);
            String label = p == 0 ? Component.translatable("rtshop.ui.all").getString() : data.categories.get(p - 1).name;
            g.drawString(this.font, clip(label, CAT_W - 26 - 4), catX + 24, y + 6, C_INK, false);
        }
        g.disableScissor();
    }

    private void drawToolbar(GuiGraphics g, int mx, int my) {
        bevel(g, tbX, tbY, searchW, 18, C_WOOD_DARK, C_SHADOW, C_WOOD_HI);
        if (sellMode) {
            Ui.button(g, this.font, allX, tbY, ALL_W, 18, Component.translatable("rtshop.ui.sell_all").getString(),
                    inside(mx, my, allX, tbY, ALL_W, 18), !rows.isEmpty());
        }
        Ui.button(g, this.font, sortX, tbY, SORT_W, 18, Component.translatable(sort.key).getString(),
                inside(mx, my, sortX, tbY, SORT_W, 18), true);
        Ui.button(g, this.font, viewX, tbY, VIEW_W, 18, Component.translatable(grid ? "rtshop.ui.grid" : "rtshop.ui.list").getString(),
                inside(mx, my, viewX, tbY, VIEW_W, 18), true);
    }

    private int drawContent(GuiGraphics g, int mx, int my) {
        bevel(g, cx, cy, cw, ch, C_PARCH, C_PARCH_SHADE, C_SHADOW);
        int hover = rowAt(mx, my);

        g.enableScissor(vx, vy, vx + vw, vy + vh);
        int c = cols();
        int step = rowStep();
        int firstLine = scroll / step;
        int lastLine = (scroll + vh) / step;
        for (int line = firstLine; line <= lastLine; line++) {
            for (int col = 0; col < c; col++) {
                int idx = line * c + col;
                if (idx >= rows.size()) {
                    break;
                }
                Row r = rows.get(idx);
                int y = vy + line * step - scroll;
                if (grid) {
                    drawCell(g, r, gridX0() + col * (CELL + GAP), y, idx == hover);
                } else {
                    drawListRow(g, r, vx, y, vw, idx == hover);
                }
            }
        }
        g.disableScissor();

        // 스크롤바
        int sbx = vx + vw + 1;
        g.fill(sbx, vy, sbx + SB, vy + vh, C_PARCH_SHADE);
        int max = maxScroll();
        if (max > 0) {
            int th = thumbH();
            int ty = vy + (int) ((long) (vh - th) * scroll / max);
            g.fill(sbx, ty, sbx + SB, ty + th, draggingBar ? C_GOLD_DARK : C_WOOD);
            g.fill(sbx, ty, sbx + SB - 1, ty + 1, C_WOOD_HI);
        }

        if (rows.isEmpty()) {
            boolean searching = search != null && !search.getValue().isBlank();
            Component msg = Component.translatable(sellMode ? "rtshop.ui.empty_sell" : "rtshop.ui.empty");
            if (sellMode && !data.sellLockNote.isEmpty()) {
                msg = Component.translatable(data.sellLockNote);
            } else if (!sellMode && !data.emptyNote.isEmpty() && !searching && menuCount() == 0) {
                msg = Component.translatable(data.emptyNote);
            }
            List<net.minecraft.util.FormattedCharSequence> lines = this.font.split(msg, Math.max(40, vw - 16));
            int ly = vy + vh / 2 - lines.size() * 5;
            for (net.minecraft.util.FormattedCharSequence line : lines) {
                g.drawString(this.font, line, vx + (vw - this.font.width(line)) / 2, ly, C_INK_SOFT, false);
                ly += 10;
            }
        }
        return hover;
    }

    private void dim(GuiGraphics g, int x, int y, int w, int h, int color) {
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(0.0F, 0.0F, 200.0F);
        g.fill(x, y, x + w, y + h, color);
        ps.popPose();
    }

    private void overText(GuiGraphics g, String text, int x, int y, int color) {
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(0.0F, 0.0F, 300.0F);
        g.drawString(this.font, text, x, y, color, true);
        ps.popPose();
    }

    private int priceWidth(String txt) {
        return 10 + this.font.width(txt);
    }

    private void drawPrice(GuiGraphics g, String txt, int x, int y, int color) {
        Ui.coin(g, x, y, 8);
        g.drawString(this.font, txt, x + 10, y, color, false);
    }

    private int priceColor(Row r) {
        if (sellMode) {
            return r.sellable ? C_GREEN : C_INK_SOFT;
        }
        return r.affordable ? C_INK : C_RED;
    }

    /** 품질 표시: 별 등급 또는 무게 보정률. 없으면 빈 문자열. */
    private String qualityMark(Row r) {
        int star = SeedRules.star(r.shown);
        if (star > 0) {
            return "★".repeat(star);
        }
        if (sellMode && Math.abs(r.mult - 1.0) > 0.004) {
            return listPercent(r);
        }
        return "";
    }

    /** 이 줄의 매입가가 상점 판매가(가격표 가격)의 몇 % 인지. */
    private static String listPercent(Row r) {
        return Math.round(r.unit / Math.max(1, r.entry.price) * 100.0) + "%";
    }

    private void drawCell(GuiGraphics g, Row r, int x, int y, boolean hov) {
        bevel(g, x, y, CELL, CELL, hov ? C_PARCH_HI : C_PARCH_DARK, hov ? C_GOLD : C_PARCH_HI, hov ? C_GOLD_DARK : C_PARCH_SHADE);
        g.renderItem(r.shown, x + (CELL - 16) / 2, y + 3);
        if (sellMode) {
            ItemStack counted = r.shown.copy();
            counted.setCount((int) Math.min(r.owned, 9999L));
            if (counted.getTag() != null) {
                counted.getTag().remove(SeedRules.STAR_TAG); // 별은 아래에서 큰 글자로 따로 그린다 (아이콘의 작은 별과 겹치지 않게)
            }
            g.renderItemDecorations(this.font, counted, x + (CELL - 16) / 2, y + 3);
        } else {
            // 구매 탭: '심기 전용' 표시(초록 점)만 보이도록 개수 없이 장식을 그린다
            ItemStack one = r.shown.copy();
            one.setCount(1);
            g.renderItemDecorations(this.font, one, x + (CELL - 16) / 2, y + 3);
        }
        String mark = qualityMark(r);
        if (!mark.isEmpty()) {
            PoseStack ps = g.pose();
            ps.pushPose();
            ps.translate(x + 2, y + 2, 300.0F);
            ps.scale(0.75F, 0.75F, 1.0F);
            g.drawString(this.font, mark, 0, 0, r.mult >= 1.0 ? 0xFFFFD24A : 0xFFFF9C8C, true);
            ps.popPose();
        }
        String txt = Ui.compact(r.unit);
        int pw = priceWidth(txt);
        drawPrice(g, txt, x + (CELL - pw) / 2, y + CELL - 11, priceColor(r));

        boolean blocked = sellMode ? !r.sellable : !r.fits;
        if (blocked) {
            dim(g, x + 1, y + 1, CELL - 2, CELL - 2, 0x99403020);
            if (!sellMode) {
                Component full = Component.translatable("rtshop.ui.full");
                overText(g, full.getString(), x + (CELL - this.font.width(full)) / 2, y + 12, 0xFFFFC0B0);
            }
        }
    }

    private void drawListRow(GuiGraphics g, Row r, int x, int y, int w, boolean hov) {
        bevel(g, x, y, w, LIST_H, hov ? C_PARCH_HI : C_PARCH_DARK, hov ? C_GOLD : C_PARCH_HI, hov ? C_GOLD_DARK : C_PARCH_SHADE);
        g.renderItem(r.shown, x + 4, y + 3);

        String txt = Ui.num(r.unit);
        int pw = priceWidth(txt);
        int nameMax = w - 26 - pw - 10;
        String mark = qualityMark(r);
        String title = mark.isEmpty() ? r.name : r.name + " " + mark;
        g.drawString(this.font, clip(title, nameMax), x + 25, y + 3, C_INK, false);

        String second = sellMode ? Component.translatable("rtshop.ui.owned_count", r.owned).getString() : categoryName(r.entry.category);
        g.drawString(this.font, clip(second, nameMax), x + 25, y + 12, C_INK_SOFT, false);

        drawPrice(g, txt, x + w - 5 - pw, y + (LIST_H - 8) / 2, priceColor(r));

        boolean blocked = sellMode ? !r.sellable : !r.fits;
        if (blocked) {
            dim(g, x + 1, y + 1, w - 2, LIST_H - 2, 0x99403020);
            if (!sellMode) {
                Component full = Component.translatable("rtshop.ui.full");
                overText(g, full.getString(), x + w - 8 - this.font.width(full) - pw, y + 7, 0xFFFFC0B0);
            }
        }
    }

    private String categoryName(String id) {
        for (ShopData.Category c : data.categories) {
            if (c.id.equals(id)) {
                return c.name;
            }
        }
        return "";
    }

    private void drawStatus(GuiGraphics g) {
        int x = fx + 6;
        int w = fw - 12;
        bevel(g, x, statusY, w, STATUS_H, C_WOOD_DARK, C_SHADOW, C_WOOD_HI);

        String owned = Ui.num(money) + " G";
        Component label = Component.translatable("rtshop.ui.money");
        int rightW = this.font.width(label) + 4 + 10 + this.font.width(owned);
        int rx = x + w - 6 - rightW;
        g.drawString(this.font, label, rx, statusY + 6, C_CREAM, false);
        Ui.coin(g, rx + this.font.width(label) + 4, statusY + 6, 8);
        g.drawString(this.font, owned, rx + this.font.width(label) + 14, statusY + 6, C_GOLD, false);

        int maxW = rx - (x + 6) - 8;
        if (status != null) {
            g.drawString(this.font, clip(status.getString(), maxW), x + 6, statusY + 6, statusOk ? 0xFFB8E08A : 0xFFFF9C8C, false);
        } else {
            Component hint = Component.translatable(sellMode ? "rtshop.ui.hint_sell" : "rtshop.ui.hint_buy");
            g.drawString(this.font, clip(hint.getString(), maxW), x + 6, statusY + 6, 0xFFBFA472, false);
        }
    }

    private void drawRowTooltip(GuiGraphics g, Row r, int mx, int my) {
        Minecraft mc = Minecraft.getInstance();
        TooltipFlag flag = mc.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL;
        List<Component> tip = new ArrayList<>(r.shown.getTooltipLines(mc.player, flag));
        Component cur = Component.literal("G");
        tip.add(Component.empty());
        if (sellMode) {
            tip.add(Component.translatable("rtshop.tip.sell_unit", Ui.num(r.unit)).withStyle(ChatFormatting.GREEN));
            if (Math.abs(r.mult - 1.0) > 0.004) {
                tip.add(qualityLine(r));
            }
            tip.add(Component.translatable("rtshop.tip.owned", r.owned).withStyle(ChatFormatting.GRAY));
            if (!r.sellable) {
                tip.add(Component.translatable("rtshop.tip.sell_zero").withStyle(ChatFormatting.RED));
            }
            if (!data.sellNote.isEmpty()) {
                String[] note = data.sellNote.split("\\|", 2);
                tip.add(Component.translatable(note[0], note.length > 1 ? note[1] : "").withStyle(ChatFormatting.GOLD));
            }
            appendRecipe(tip, r.entry);
            tip.add(Component.translatable("rtshop.ui.hint_sell").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tip.add(Component.translatable("rtshop.tip.price", Ui.num(r.unit), cur).withStyle(ChatFormatting.GOLD));
            if (r.entry.sellable) {
                tip.add(Component.translatable("rtshop.tip.sell_base", Ui.num(r.entry.sellUnit)).withStyle(ChatFormatting.GRAY));
            }
            if (!r.affordable) {
                tip.add(Component.translatable("rtshop.msg.no_money").withStyle(ChatFormatting.RED));
            }
            if (!r.fits) {
                tip.add(Component.translatable("rtshop.msg.inv_full_buy").withStyle(ChatFormatting.RED));
            }
            appendRecipe(tip, r.entry);
            tip.add(Component.translatable("rtshop.ui.hint_buy").withStyle(ChatFormatting.DARK_GRAY));
        }
        g.renderComponentTooltip(this.font, tip, mx, my);
    }

    /** 만들 수 있는 품목: 매입가를 어떤 재료 기준으로 계산했는지 보여 준다 (이 재료대로 만들어 팔면 손해가 없다). */
    private static void appendRecipe(List<Component> tip, ShopData.Entry e) {
        if (e == null || e.recipe.isEmpty()) {
            return;
        }
        tip.add(Component.translatable("rtshop.tip.recipe_head", amount(e.recipeOut)).withStyle(ChatFormatting.DARK_AQUA));
        int shown = 0;
        for (ShopData.Part part : e.recipe) {
            if (shown++ >= 9) {
                break;
            }
            tip.add(Component.translatable("rtshop.tip.recipe_line", part.item.getDescription(), amount(part.count))
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    private static String amount(float v) {
        return Math.abs(v - Math.round(v)) < 0.01F ? Integer.toString(Math.round(v)) : String.format(Locale.ROOT, "%.1f", v);
    }

    private Component qualityLine(Row r) {
        int star = SeedRules.star(r.shown);
        String pct = listPercent(r);
        if (star > 0) {
            return Component.translatable("rtshop.tip.star_bonus", SeedRules.starText(star), pct).withStyle(ChatFormatting.YELLOW);
        }
        CompoundTag tag = r.shown.getTag();
        if (tag != null && tag.contains(ShopData.FISH_INFO_TAG, 10)) {
            CompoundTag info = tag.getCompound(ShopData.FISH_INFO_TAG);
            if ("golden".equals(info.getString("rarity"))) {
                return Component.translatable("rtshop.tip.fish_golden", pct).withStyle(ChatFormatting.GOLD);
            }
            return Component.translatable("rtshop.tip.fish_weight", weightText(info.getFloat("weight")), pct)
                    .withStyle(r.mult >= 1 ? ChatFormatting.AQUA : ChatFormatting.RED);
        }
        return Component.translatable("rtshop.tip.fish_noinfo", pct).withStyle(ChatFormatting.RED);
    }

    private static String weightText(float grams) {
        if (grams >= 1000) {
            return String.format(Locale.ROOT, "%.2fkg", grams / 1000.0F);
        }
        return Math.round(grams) + "g";
    }

    // =====================================================================================
    // 수량 입력 창 / 전부 판매 확인 창
    // =====================================================================================

    private void openDialog(Row r) {
        dlgRow = r;
        dlgSell = sellMode;
        confirmAll = false;
        this.setFocused(null);
        if (search != null) {
            search.setFocused(false);
        }
        qtyBox.setValue("1");
        qtyBox.setFocused(true);
        qtyBox.moveCursorToEnd();
        qtyBox.setHighlightPos(0);
        playClick();
    }

    private void closeDialog() {
        dlgRow = null;
        confirmAll = false;
        if (qtyBox != null) {
            qtyBox.setFocused(false);
        }
    }

    private long qty() {
        try {
            return qtyBox.getValue().isEmpty() ? 0 : Long.parseLong(qtyBox.getValue());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private void setQty(long v) {
        v = Math.max(1, Math.min(QTY_LIMIT, v));
        qtyBox.setValue(Long.toString(v));
        qtyBox.moveCursorToEnd();
    }

    /** 지금 거래할 수 있는 최대 수량 (구매: 소지금과 인벤토리 빈 공간, 판매: 보유 수량). */
    private long dlgMax() {
        if (dlgRow == null) {
            return 0;
        }
        List<ItemStack> inv = inventory();
        ShopData.Entry e = dlgRow.entry;
        if (dlgSell) {
            long owned = 0;
            for (ItemStack s : inv) {
                if (!s.isEmpty() && data.findSellEntry(s) == dlgRow.index && data.variantKey(e, s) == dlgRow.key) {
                    owned += s.getCount();
                }
            }
            return Math.min(QTY_LIMIT, owned);
        }
        long afford = ClientEconomy.wallet / Math.max(1, e.price);
        return Math.min(QTY_LIMIT, Math.min(afford, InvUtil.capacity(inv, e.stack)));
    }

    private long dlgTotal(long q) {
        return dlgSell ? ShopData.revenue(dlgRow.unit, q) : (long) dlgRow.entry.price * q;
    }

    private boolean dlgValid() {
        long q = qty();
        return q >= 1 && q <= dlgMax() && (!dlgSell || dlgTotal(q) >= 1);
    }

    // 버튼 위치: [-10][-1][ 입력 ][+1][+10] / [최대] / [확인][취소]
    private static final int[] STEP_X = {12, 39, 118, 141};
    private static final int[] STEP_W = {24, 20, 20, 24};
    private static final long[] STEP_V = {-10, -1, 1, 10};
    private static final String[] STEP_L = {"-10", "-1", "+1", "+10"};

    private void drawQtyDialog(GuiGraphics g, int mx, int my, float partial) {
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(0.0F, 0.0F, 400.0F);

        g.fill(fx, fy, fx + fw, fy + fh, 0x88140B05); // 뒤 화면 어둡게
        Ui.woodFrame(g, dx, dy, DW, DH);
        bevel(g, dx + 6, dy + 6, DW - 12, 34, C_PARCH, C_PARCH_SHADE, C_SHADOW);

        long max = dlgMax();
        long q = qty();
        boolean valid = dlgValid();

        g.renderItem(dlgRow.shown, dx + 10, dy + 9);
        String title = Component.translatable(dlgSell ? "rtshop.qty.title_sell" : "rtshop.qty.title_buy").getString();
        g.drawString(this.font, clip(title + " - " + dlgRow.name, DW - 44), dx + 30, dy + 10, C_INK, false);
        g.drawString(this.font, clip(Component.translatable("rtshop.qty.unit", Ui.num(dlgRow.unit), Ui.num(max)).getString(), DW - 44),
                dx + 30, dy + 24, C_INK_SOFT, false);

        for (int i = 0; i < 4; i++) {
            Ui.button(g, this.font, dx + STEP_X[i], dy + 43, STEP_W[i], 18, STEP_L[i],
                    Ui.inside(mx, my, dx + STEP_X[i], dy + 43, STEP_W[i], 18), true);
        }
        bevel(g, dx + 62, dy + 43, 53, 18, C_WOOD_DARK, C_SHADOW, C_WOOD_HI);
        qtyBox.render(g, mx, my, partial);

        Ui.button(g, this.font, dx + 12, dy + 65, 34, 14, Component.translatable("rtshop.qty.max").getString(),
                Ui.inside(mx, my, dx + 12, dy + 65, 34, 14), max >= 1);
        String total;
        int totalColor;
        if (q > max) {
            total = Component.translatable("rtshop.qty.over", Ui.num(max)).getString();
            totalColor = 0xFFFF9C8C;
        } else if (dlgSell && q >= 1 && dlgTotal(q) < 1) {
            total = Component.translatable("rtshop.tip.sell_zero").getString();
            totalColor = 0xFFFF9C8C;
        } else {
            total = Component.translatable(dlgSell ? "rtshop.qty.total_sell" : "rtshop.qty.total_buy",
                    Ui.num(dlgTotal(Math.max(0, q)))).getString();
            totalColor = C_GOLD;
        }
        total = clip(total, DW - 12 - 52);
        g.drawString(this.font, total, dx + DW - 12 - this.font.width(total), dy + 68, totalColor, false);

        Ui.button(g, this.font, dx + 12, dy + 83, 72, 17,
                Component.translatable(dlgSell ? "rtshop.ui.sell" : "rtshop.ui.buy").getString(),
                valid && Ui.inside(mx, my, dx + 12, dy + 83, 72, 17), valid);
        Ui.button(g, this.font, dx + DW - 12 - 72, dy + 83, 72, 17, Component.translatable("rtshop.qty.cancel").getString(),
                Ui.inside(mx, my, dx + DW - 12 - 72, dy + 83, 72, 17), true);

        ps.popPose();
    }

    private void confirmDialog() {
        if (dlgRow == null) {
            return;
        }
        if (!dlgValid()) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_NO, 1.0F));
            return;
        }
        ShopNet.CHANNEL.sendToServer(new TransactionPacket(data.id, data.version, dlgRow.index, dlgSell, (int) qty(),
                dlgSell ? dlgRow.key : 0L));
        closeDialog();
    }

    private void dialogClicked(double mx, double my, int button) {
        if (button != 0) {
            return;
        }
        if (!Ui.inside(mx, my, dx, dy, DW, DH)) {
            closeDialog(); // 창 밖을 누르면 닫기
            return;
        }
        for (int i = 0; i < 4; i++) {
            if (Ui.inside(mx, my, dx + STEP_X[i], dy + 43, STEP_W[i], 18)) {
                playClick();
                setQty(qty() + STEP_V[i]);
                return;
            }
        }
        if (Ui.inside(mx, my, dx + 12, dy + 65, 34, 14)) {
            long max = dlgMax();
            if (max >= 1) {
                playClick();
                setQty(max);
            }
            return;
        }
        if (Ui.inside(mx, my, dx + 12, dy + 83, 72, 17)) {
            confirmDialog();
            return;
        }
        if (Ui.inside(mx, my, dx + DW - 12 - 72, dy + 83, 72, 17)) {
            playClick();
            closeDialog();
            return;
        }
        qtyBox.setFocused(true);
        qtyBox.mouseClicked(mx, my, button);
    }

    private long[] sellAllTotals() {
        long count = 0;
        long gold = 0;
        for (Row r : rows) {
            long rev = ShopData.revenue(r.unit, r.owned);
            if (rev >= 1) {
                count += r.owned;
                gold += rev;
            }
        }
        return new long[]{count, gold};
    }

    private void drawConfirmAll(GuiGraphics g, int mx, int my) {
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(0.0F, 0.0F, 400.0F);
        g.fill(fx, fy, fx + fw, fy + fh, 0x88140B05);
        Ui.woodFrame(g, dx, dy + 10, DW, 80);
        bevel(g, dx + 6, dy + 16, DW - 12, 40, C_PARCH, C_PARCH_SHADE, C_SHADOW);
        long[] t = sellAllTotals();
        String l1 = Component.translatable("rtshop.all.line1", Ui.num(t[0])).getString();
        String l2 = Component.translatable("rtshop.all.line2", Ui.num(t[1])).getString();
        String l3 = Component.translatable("rtshop.all.line3").getString();
        g.drawString(this.font, clip(l1, DW - 20), dx + 11, dy + 21, C_INK, false);
        g.drawString(this.font, clip(l2, DW - 20), dx + 11, dy + 32, C_GREEN, false);
        g.drawString(this.font, clip(l3, DW - 20), dx + 11, dy + 43, C_INK_SOFT, false);
        boolean ok = t[0] > 0;
        Ui.button(g, this.font, dx + 12, dy + 64, 72, 17, Component.translatable("rtshop.ui.sell").getString(),
                ok && Ui.inside(mx, my, dx + 12, dy + 64, 72, 17), ok);
        Ui.button(g, this.font, dx + DW - 12 - 72, dy + 64, 72, 17, Component.translatable("rtshop.qty.cancel").getString(),
                Ui.inside(mx, my, dx + DW - 12 - 72, dy + 64, 72, 17), true);
        ps.popPose();
    }

    private void doSellAll() {
        List<Row> list = new ArrayList<>();
        for (Row r : rows) {
            if (ShopData.revenue(r.unit, r.owned) >= 1) {
                list.add(r);
            }
        }
        if (list.isEmpty()) {
            return;
        }
        int[] idx = new int[list.size()];
        long[] keys = new long[list.size()];
        for (int i = 0; i < list.size(); i++) {
            idx[i] = list.get(i).index;
            keys[i] = list.get(i).key;
        }
        ShopNet.CHANNEL.sendToServer(new SellAllPacket(data.id, data.version, idx, keys));
    }

    private void confirmAllClicked(double mx, double my, int button) {
        if (button != 0) {
            return;
        }
        if (Ui.inside(mx, my, dx + 12, dy + 64, 72, 17)) {
            playClick();
            doSellAll();
            closeDialog();
        } else if (Ui.inside(mx, my, dx + DW - 12 - 72, dy + 64, 72, 17) || !Ui.inside(mx, my, dx, dy + 10, DW, 80)) {
            playClick();
            closeDialog();
        }
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (dlgRow != null) {
            qtyBox.charTyped(c, mods);
            return true;
        }
        if (confirmAll) {
            return true;
        }
        return super.charTyped(c, mods);
    }

    // =====================================================================================
    // 입력
    // =====================================================================================

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (dlgRow != null) {
            dialogClicked(mx, my, button);
            return true;
        }
        if (confirmAll) {
            confirmAllClicked(mx, my, button);
            return true;
        }
        if (super.mouseClicked(mx, my, button)) {
            return true; // 검색창 클릭
        }
        if (button == 0) {
            if (inside(mx, my, tabBuyX, tabY, TAB_W, TAB_H)) {
                playClick();
                setMode(false);
                return true;
            }
            if (inside(mx, my, tabSellX, tabY, TAB_W, TAB_H)) {
                playClick();
                setMode(true);
                return true;
            }
            if (landShop() && inside(mx, my, tabBuyX - 6 - plotTabW(), tabY, plotTabW(), TAB_H)) {
                playClick();
                openPlotsForTest();
                return true;
            }
            int sbx = vx + vw + 1;
            if (maxScroll() > 0 && inside(mx, my, sbx, vy, SB, vh)) {
                draggingBar = true;
                dragScrollTo(my);
                return true;
            }
            if (inside(mx, my, catX, catY, CAT_W, catH)) {
                int rel = (int) my - (catY + 2) + catScroll;
                if (rel >= 0) {
                    int p = rel / CAT_H;
                    if (p == 0) {
                        selectCategory(-1);
                    } else if (p - 1 < data.categories.size()) {
                        selectCategory(p - 1);
                    }
                }
                return true;
            }
            if (inside(mx, my, viewX, tbY, VIEW_W, 18)) {
                playClick();
                grid = !grid;
                scroll = 0;
                rebuild();
                return true;
            }
            if (sellMode && inside(mx, my, allX, tbY, ALL_W, 18)) {
                if (!rows.isEmpty()) {
                    playClick();
                    confirmAll = true;
                    this.setFocused(null);
                    if (search != null) {
                        search.setFocused(false);
                    }
                }
                return true;
            }
        }
        if ((button == 0 || button == 1) && inside(mx, my, sortX, tbY, SORT_W, 18)) {
            playClick();
            sort = button == 1 ? sort.prev() : sort.next();
            scroll = 0;
            rebuild();
            return true;
        }
        if (button == 0 || button == 1) {
            int idx = rowAt(mx, my);
            if (idx >= 0) {
                clickRow(rows.get(idx), button);
                return true;
            }
        }
        return false;
    }

    private void selectCategory(int index) {
        playClick();
        category = index;
        scroll = 0;
        rebuild();
    }

    private void clickRow(Row r, int button) {
        if (button == 0 && !hasShiftDown()) {
            openDialog(r); // 좌클릭: 수량을 직접 입력
            return;
        }
        int amount;
        if (sellMode) {
            if (hasShiftDown()) {
                amount = (int) Math.min(r.owned, QTY_LIMIT);
            } else {
                amount = 10;
            }
            amount = (int) Math.min((long) amount, r.owned);
        } else {
            if (hasShiftDown()) {
                amount = Math.max(1, Math.min(r.entry.stack.getMaxStackSize(), InvUtil.MAX_STACK_CAP));
            } else {
                amount = 10;
            }
        }
        if (amount <= 0) {
            return;
        }
        ShopNet.CHANNEL.sendToServer(new TransactionPacket(data.id, data.version, r.index, sellMode, amount, sellMode ? r.key : 0L));
    }

    private void dragScrollTo(double my) {
        int max = maxScroll();
        if (max <= 0) {
            return;
        }
        int th = thumbH();
        double t = (my - vy - th / 2.0D) / (double) Math.max(1, vh - th);
        scroll = Math.max(0, Math.min(max, (int) Math.round(t * max)));
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double ddx, double ddy) {
        if (draggingBar) {
            dragScrollTo(my);
            return true;
        }
        return super.mouseDragged(mx, my, button, ddx, ddy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (draggingBar) {
            draggingBar = false;
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (dlgRow != null) {
            setQty(qty() + (delta > 0 ? 1 : -1));
            return true;
        }
        if (confirmAll) {
            return true;
        }
        if (inside(mx, my, catX, catY, CAT_W, catH)) {
            catScroll = Math.max(0, Math.min(maxCatScroll(), catScroll - (int) (delta * 14)));
            return true;
        }
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) (delta * 18)));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (dlgRow != null) {
            if (key == 256) { // Esc: 수량 창만 닫기
                closeDialog();
            } else if (key == 257 || key == 335) { // Enter
                confirmDialog();
            } else {
                qtyBox.keyPressed(key, scan, mods);
            }
            return true;
        }
        if (confirmAll) {
            if (key == 256) {
                closeDialog();
            } else if (key == 257 || key == 335) {
                doSellAll();
                closeDialog();
            }
            return true;
        }
        if (search != null && !search.isFocused() && this.minecraft != null
                && this.minecraft.options.keyInventory.matches(key, scan)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }
}

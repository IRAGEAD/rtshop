package com.rtshop.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rtshop.net.ClosePacket;
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
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 중세풍(갈색 계열 + 크림색) 상점 화면.
 * - 검색창 / 카테고리 / 정렬 / 격자·목록 전환 / 구매·판매 탭
 * - 인벤토리가 가득 차 구매할 수 없는 아이템은 어둡게 표시되고 "가득 참" 문구가 나옵니다.
 */
public class ShopScreen extends Screen {

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
    private static final int SORT_W = 72;
    private static final int VIEW_W = 40;

    // ---- 색상 (ARGB)
    private static final int C_SHADOW = 0xFF140B05;
    private static final int C_WOOD = 0xFF5C3A1E;
    private static final int C_WOOD_HI = 0xFF8A5A30;
    private static final int C_WOOD_DARK = 0xFF3E2512;
    private static final int C_GOLD = 0xFFE8B84A;
    private static final int C_GOLD_DARK = 0xFFA87A22;
    private static final int C_PARCH = 0xFFEADBB5;
    private static final int C_PARCH_DARK = 0xFFD9C592;
    private static final int C_PARCH_SHADE = 0xFFBFA472;
    private static final int C_PARCH_HI = 0xFFF6EBCB;
    private static final int C_INK = 0xFF3A2814;
    private static final int C_INK_SOFT = 0xFF7A6240;
    private static final int C_CREAM = 0xFFF5E6C0;
    private static final int C_RED = 0xFFA02C1E;
    private static final int C_GREEN = 0xFF2F6B24;

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

    private static final class Row {
        final int index;
        final ShopData.Entry entry;
        final String name;
        double unit;          // 표시 단가 (구매: 가격, 판매: 가격 * 비율)
        long owned;           // 판매 모드: 보유 수량
        boolean affordable = true;
        boolean fits = true;
        boolean sellable = true;

        Row(int index, ShopData.Entry entry, String name) {
            this.index = index;
            this.entry = entry;
            this.name = name;
        }
    }

    private final ShopData data;
    private final ItemStack allIcon = new ItemStack(Items.CHEST);

    private boolean sellMode = false;
    private int category = -1; // -1 = 전체
    private Sort sort = Sort.DEFAULT;
    private boolean grid = true;
    private int scroll = 0;
    private int catScroll = 0;
    private boolean draggingBar = false;

    private EditBox search;

    // ---- 수량 입력 창
    private static final int DW = 176;
    private static final int DH = 106;
    private static final long QTY_LIMIT = 36L * InvUtil.MAX_STACK_CAP;
    private EditBox qtyBox;
    private ShopData.Entry dlgEntry = null;
    private int dlgIndex = -1;
    private boolean dlgSell = false;
    private int dx, dy;
    private final List<Row> rows = new ArrayList<>();
    private long money = 0;
    private int invSig = 0;

    private Component status = null;
    private boolean statusOk = true;
    private int statusTicks = 0;

    // 레이아웃(init 에서 계산)
    private int fx, fy, fw, fh;
    private int catX, catY, catH;
    private int tbX, tbY, tbW;
    private int sortX, viewX;
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

        int searchW = Math.max(40, tbW - SORT_W - VIEW_W - 8);
        sortX = tbX + searchW + 4;
        viewX = sortX + SORT_W + 4;

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
        search = new EditBox(this.font, tbX + 5, tbY + 5, searchW - 10, 10, Component.translatable("rtshop.ui.search"));
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
        qtyBox.setFocused(dlgEntry != null);

        rebuild();
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
        if (ok) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F));
        } else {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_NO, 1.0F));
        }
    }

    private void playClick() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public void tick() {
        super.tick();
        if (statusTicks > 0) {
            statusTicks--;
            if (statusTicks == 0) {
                status = null;
            }
        }
        if (qtyBox != null && dlgEntry != null) {
            qtyBox.tick();
        }
        int sig = inventorySignature();
        if (sig != invSig) {
            invSig = sig;
            rebuild();
        }
    }

    private int inventorySignature() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return 0;
        }
        int sig = 7 + Long.hashCode(ClientEconomy.wallet);
        for (ItemStack s : mc.player.getInventory().items) {
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
    // 목록 구성 (검색 / 카테고리 / 정렬 / 모드)
    // =====================================================================================

    private void rebuild() {
        rows.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        Inventory inv = mc.player.getInventory();
        List<ItemStack> base = InvUtil.copyMain(inv);
        money = ClientEconomy.wallet;

        String q = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        String catId = (category >= 0 && category < data.categories.size()) ? data.categories.get(category).id : null;

        for (int i = 0; i < data.entries.size(); i++) {
            final ShopData.Entry e = data.entries.get(i);
            if (catId != null && !catId.equals(e.category)) {
                continue;
            }
            String name = e.stack.getHoverName().getString();
            if (!q.isEmpty()) {
                String id = String.valueOf(ForgeRegistries.ITEMS.getKey(e.stack.getItem())).toLowerCase(Locale.ROOT);
                if (!name.toLowerCase(Locale.ROOT).contains(q) && !id.contains(q)) {
                    continue;
                }
            }
            Row r = new Row(i, e, name);
            if (sellMode) {
                r.owned = InvUtil.count(base, e::matches);
                if (r.owned <= 0) {
                    continue;
                }
                r.unit = e.price * data.sellRatio;
                r.sellable = data.sellRevenue(e.price, r.owned) >= 1;
                r.fits = true; // Gold 는 인벤토리 칸을 차지하지 않음
            } else {
                r.unit = e.price;
                r.affordable = money >= e.price;
                r.fits = InvUtil.capacity(base, e.stack) >= 1;
            }
            rows.add(r);
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

    private void setMode(boolean sell) {
        if (sellMode != sell) {
            sellMode = sell;
            scroll = 0;
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
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // =====================================================================================
    // 그리기
    // =====================================================================================

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        this.renderBackground(g);
        final int realMx = mx;
        final int realMy = my;
        if (dlgEntry != null) {
            // 수량 창이 열려 있으면 뒤쪽 화면은 마우스에 반응하지 않게 한다
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

        if (dlgEntry != null) {
            drawQtyDialog(g, realMx, realMy, partial);
        } else if (hover >= 0 && hover < rows.size()) {
            drawRowTooltip(g, rows.get(hover), mx, my);
        }
    }

    // =====================================================================================
    // 수량 입력 창
    // =====================================================================================

    private void openDialog(Row r) {
        dlgEntry = r.entry;
        dlgIndex = r.index;
        dlgSell = sellMode;
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
        dlgEntry = null;
        dlgIndex = -1;
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
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || dlgEntry == null) {
            return 0;
        }
        List<ItemStack> items = mc.player.getInventory().items;
        if (dlgSell) {
            return Math.min(QTY_LIMIT, InvUtil.count(items, dlgEntry::matches));
        }
        long afford = ClientEconomy.wallet / Math.max(1, dlgEntry.price);
        return Math.min(QTY_LIMIT, Math.min(afford, InvUtil.capacity(items, dlgEntry.stack)));
    }

    private long dlgTotal(long q) {
        return dlgSell ? data.sellRevenue(dlgEntry.price, q) : (long) dlgEntry.price * q;
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

        // 아이템과 단가
        g.renderItem(dlgEntry.stack, dx + 10, dy + 9);
        String title = Component.translatable(dlgSell ? "rtshop.qty.title_sell" : "rtshop.qty.title_buy").getString();
        g.drawString(this.font, clip(title + " - " + dlgEntry.stack.getHoverName().getString(), DW - 44),
                dx + 30, dy + 10, C_INK, false);
        double unit = dlgSell ? dlgEntry.price * data.sellRatio : dlgEntry.price;
        g.drawString(this.font, clip(Component.translatable("rtshop.qty.unit", Ui.num(unit), Ui.num(max)).getString(), DW - 44),
                dx + 30, dy + 24, C_INK_SOFT, false);

        // 수량 조절
        for (int i = 0; i < 4; i++) {
            Ui.button(g, this.font, dx + STEP_X[i], dy + 43, STEP_W[i], 18, STEP_L[i],
                    Ui.inside(mx, my, dx + STEP_X[i], dy + 43, STEP_W[i], 18), true);
        }
        bevel(g, dx + 62, dy + 43, 53, 18, C_WOOD_DARK, C_SHADOW, C_WOOD_HI);
        qtyBox.render(g, mx, my, partial);

        // 최대 버튼과 합계
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

        // 확인 / 취소
        Ui.button(g, this.font, dx + 12, dy + 83, 72, 17,
                Component.translatable(dlgSell ? "rtshop.ui.sell" : "rtshop.ui.buy").getString(),
                valid && Ui.inside(mx, my, dx + 12, dy + 83, 72, 17), valid);
        Ui.button(g, this.font, dx + DW - 12 - 72, dy + 83, 72, 17, Component.translatable("rtshop.qty.cancel").getString(),
                Ui.inside(mx, my, dx + DW - 12 - 72, dy + 83, 72, 17), true);

        ps.popPose();
    }

    private void confirmDialog() {
        if (dlgEntry == null) {
            return;
        }
        if (!dlgValid()) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_NO, 1.0F));
            return;
        }
        ShopNet.CHANNEL.sendToServer(new TransactionPacket(data.id, data.version, dlgIndex, dlgSell, (int) qty()));
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

    @Override
    public boolean charTyped(char c, int mods) {
        if (dlgEntry != null) {
            qtyBox.charTyped(c, mods);
            return true;
        }
        return super.charTyped(c, mods);
    }

    private void bevel(GuiGraphics g, int x, int y, int w, int h, int fill, int hi, int lo) {
        g.fill(x, y, x + w, y + h, lo);
        g.fill(x, y, x + w - 1, y + h - 1, hi);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
    }

    private void drawFrame(GuiGraphics g, int mx, int my) {
        // 바깥 그림자 + 나무 틀
        g.fill(fx - 2, fy - 2, fx + fw + 2, fy + fh + 2, C_SHADOW);
        g.fill(fx, fy, fx + fw, fy + fh, C_WOOD);
        for (int yy = fy + 3; yy < fy + fh - 2; yy += 5) {
            g.fill(fx + 1, yy, fx + fw - 1, yy + 1, 0x22000000);
        }
        g.fill(fx, fy, fx + fw, fy + 1, C_WOOD_HI);
        g.fill(fx, fy, fx + 1, fy + fh, C_WOOD_HI);
        // 모서리 금속 장식
        int[][] corners = {{fx + 2, fy + 2}, {fx + fw - 5, fy + 2}, {fx + 2, fy + fh - 5}, {fx + fw - 5, fy + fh - 5}};
        for (int[] c : corners) {
            g.fill(c[0], c[1], c[0] + 3, c[1] + 3, C_GOLD_DARK);
            g.fill(c[0], c[1], c[0] + 2, c[1] + 2, C_GOLD);
        }

        // 제목 줄
        g.fill(fx + 5, fy + 4, fx + fw - 5, fy + TITLE_H - 1, C_WOOD_DARK);
        g.fill(fx + 5, fy + TITLE_H - 2, fx + fw - 5, fy + TITLE_H - 1, C_GOLD_DARK);
        Ui.coin(g, fx + 9, fy + 4, 16);
        g.drawString(this.font, this.title, fx + 29, fy + 9, C_GOLD, true);

        // 구매 / 판매 탭
        drawTab(g, mx, my, tabBuyX, Component.translatable("rtshop.ui.buy"), !sellMode);
        drawTab(g, mx, my, tabSellX, Component.translatable("rtshop.ui.sell"), sellMode);
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
        // 검색창 배경
        int searchW = sortX - 4 - tbX;
        bevel(g, tbX, tbY, searchW, 18, C_WOOD_DARK, C_SHADOW, C_WOOD_HI);

        drawSmallButton(g, mx, my, sortX, tbY, SORT_W, Component.translatable(sort.key));
        drawSmallButton(g, mx, my, viewX, tbY, VIEW_W,
                Component.translatable(grid ? "rtshop.ui.grid" : "rtshop.ui.list"));
    }

    private void drawSmallButton(GuiGraphics g, int mx, int my, int x, int y, int w, Component label) {
        boolean hov = inside(mx, my, x, y, w, 18);
        bevel(g, x, y, w, 18, hov ? C_WOOD_HI : C_WOOD, C_WOOD_HI, C_SHADOW);
        String s = clip(label.getString(), w - 6);
        int tx = x + (w - this.font.width(s)) / 2;
        g.drawString(this.font, s, tx, y + 5, C_CREAM, false);
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
            Component msg = Component.translatable(sellMode ? "rtshop.ui.empty_sell" : "rtshop.ui.empty");
            g.drawString(this.font, msg, vx + (vw - this.font.width(msg)) / 2, vy + vh / 2 - 4, C_INK_SOFT, false);
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

    private void drawSmallIcon(GuiGraphics g, ItemStack stack, int x, int y) {
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate((float) x, (float) y, 0.0F);
        ps.scale(0.5F, 0.5F, 1.0F);
        g.renderItem(stack, 0, 0);
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

    private void drawCell(GuiGraphics g, Row r, int x, int y, boolean hov) {
        bevel(g, x, y, CELL, CELL, hov ? C_PARCH_HI : C_PARCH_DARK, hov ? C_GOLD : C_PARCH_HI, hov ? C_GOLD_DARK : C_PARCH_SHADE);
        g.renderItem(r.entry.stack, x + (CELL - 16) / 2, y + 3);
        if (sellMode) {
            ItemStack shown = r.entry.stack.copy();
            shown.setCount((int) Math.min(r.owned, 9999L));
            g.renderItemDecorations(this.font, shown, x + (CELL - 16) / 2, y + 3);
        }
        String txt = compact(r.unit);
        int pw = priceWidth(txt);
        drawPrice(g, txt, x + (CELL - pw) / 2, y + CELL - 11, priceColor(r));

        boolean blocked = sellMode ? (!r.sellable || !r.fits) : !r.fits;
        if (blocked) {
            dim(g, x + 1, y + 1, CELL - 2, CELL - 2, 0x99403020);
            if (!r.fits) {
                Component full = Component.translatable("rtshop.ui.full");
                PoseStack ps = g.pose();
                ps.pushPose();
                ps.translate(0.0F, 0.0F, 300.0F);
                g.drawString(this.font, full, x + (CELL - this.font.width(full)) / 2, y + 12, 0xFFFFC0B0, true);
                ps.popPose();
            }
        }
    }

    private void drawListRow(GuiGraphics g, Row r, int x, int y, int w, boolean hov) {
        bevel(g, x, y, w, LIST_H, hov ? C_PARCH_HI : C_PARCH_DARK, hov ? C_GOLD : C_PARCH_HI, hov ? C_GOLD_DARK : C_PARCH_SHADE);
        g.renderItem(r.entry.stack, x + 4, y + 3);

        String txt = fmt(r.unit);
        int pw = priceWidth(txt);
        int nameMax = w - 26 - pw - 10;
        g.drawString(this.font, clip(r.name, nameMax), x + 25, y + 3, C_INK, false);

        String second;
        if (sellMode) {
            second = Component.translatable("rtshop.ui.owned_count", r.owned).getString();
        } else {
            second = categoryName(r.entry.category);
        }
        g.drawString(this.font, clip(second, nameMax), x + 25, y + 12, C_INK_SOFT, false);

        drawPrice(g, txt, x + w - 5 - pw, y + (LIST_H - 8) / 2, priceColor(r));

        boolean blocked = sellMode ? (!r.sellable || !r.fits) : !r.fits;
        if (blocked) {
            dim(g, x + 1, y + 1, w - 2, LIST_H - 2, 0x99403020);
            if (!r.fits) {
                Component full = Component.translatable("rtshop.ui.full");
                PoseStack ps = g.pose();
                ps.pushPose();
                ps.translate(0.0F, 0.0F, 300.0F);
                g.drawString(this.font, full, x + w - 8 - this.font.width(full) - pw, y + 7, 0xFFFFC0B0, true);
                ps.popPose();
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

        // 오른쪽: 보유 화폐
        String owned = Ui.num(money) + " G";
        Component label = Component.translatable("rtshop.ui.money");
        int rightW = this.font.width(label) + 4 + 10 + this.font.width(owned);
        int rx = x + w - 6 - rightW;
        g.drawString(this.font, label, rx, statusY + 6, C_CREAM, false);
        Ui.coin(g, rx + this.font.width(label) + 4, statusY + 6, 8);
        g.drawString(this.font, owned, rx + this.font.width(label) + 14, statusY + 6, C_GOLD, false);

        // 왼쪽: 결과 메시지 또는 조작 안내
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
        List<Component> tip = new ArrayList<>(r.entry.stack.getTooltipLines(mc.player, flag));
        Component cur = Component.literal("G");
        tip.add(Component.empty());
        if (sellMode) {
            int pct = (int) Math.round(data.sellRatio * 100.0D);
            tip.add(Component.translatable("rtshop.tip.sell_price", fmt(r.unit), cur, pct).withStyle(ChatFormatting.GREEN));
            tip.add(Component.translatable("rtshop.tip.owned", r.owned).withStyle(ChatFormatting.GRAY));
            if (!r.sellable) {
                tip.add(Component.translatable("rtshop.tip.sell_zero").withStyle(ChatFormatting.RED));
            }
            tip.add(Component.translatable("rtshop.ui.hint_sell").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tip.add(Component.translatable("rtshop.tip.price", fmt(r.unit), cur).withStyle(ChatFormatting.GOLD));
            if (!r.affordable) {
                tip.add(Component.translatable("rtshop.msg.no_money").withStyle(ChatFormatting.RED));
            }
            if (!r.fits) {
                tip.add(Component.translatable("rtshop.msg.inv_full_buy").withStyle(ChatFormatting.RED));
            }
            tip.add(Component.translatable("rtshop.ui.hint_buy").withStyle(ChatFormatting.DARK_GRAY));
        }
        g.renderComponentTooltip(this.font, tip, mx, my);
    }

    // =====================================================================================
    // 입력
    // =====================================================================================

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (dlgEntry != null) {
            dialogClicked(mx, my, button);
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
                amount = (int) Math.min(r.owned, 36L * InvUtil.MAX_STACK_CAP);
            } else {
                amount = button == 1 ? 10 : 1;
            }
            amount = (int) Math.min((long) amount, r.owned);
        } else {
            if (hasShiftDown()) {
                amount = Math.max(1, Math.min(r.entry.stack.getMaxStackSize(), InvUtil.MAX_STACK_CAP));
            } else {
                amount = button == 1 ? 10 : 1;
            }
        }
        if (amount <= 0) {
            return;
        }
        ShopNet.CHANNEL.sendToServer(new TransactionPacket(data.id, data.version, r.index, sellMode, amount));
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
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingBar) {
            dragScrollTo(my);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
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
        if (dlgEntry != null) {
            setQty(qty() + (delta > 0 ? 1 : -1));
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
        if (dlgEntry != null) {
            if (key == 256) { // Esc: 수량 창만 닫기
                closeDialog();
            } else if (key == 257 || key == 335) { // Enter
                confirmDialog();
            } else {
                qtyBox.keyPressed(key, scan, mods);
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

    // =====================================================================================
    // 문자열 도우미
    // =====================================================================================

    private String clip(String s, int maxWidth) {
        if (maxWidth <= 0) {
            return "";
        }
        if (this.font.width(s) <= maxWidth) {
            return s;
        }
        String ell = "…";
        return this.font.plainSubstrByWidth(s, Math.max(0, maxWidth - this.font.width(ell))) + ell;
    }

    private static String fmt(double v) {
        if (Math.abs(v - Math.rint(v)) < 1.0e-9) {
            return Long.toString((long) Math.rint(v));
        }
        return String.format(Locale.ROOT, "%.1f", v);
    }

    private static String compact(double v) {
        if (v >= 1_000_000) {
            return (long) (v / 1_000_000) + "M";
        }
        if (v >= 10_000) {
            return (long) (v / 1_000) + "k";
        }
        return fmt(v);
    }
}

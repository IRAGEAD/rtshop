package com.rtshop.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rtshop.net.ShopNet;
import com.rtshop.net.StockPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * 조합 증서 창 (3.72.0). 시세판(블록 · 명령 · 증서 우클릭)으로 열면 보기만, 은행원의 [증권 거래]로 열면 사고팔기와 배당 받기.
 * 왼쪽: 조합 여섯 곳(시세 · 어제 대비 · 작은 그래프). 오른쪽: 고른 조합의 지난 14일 그래프, 이번 주 배당 · 발행 주식 · 내 증서,
 * 사기(주 수 · 값 미리 보기), 내 증서 되팔기(두 번 눌러 확인). 아래: 이야기 소식과 기금, 배당 받기.
 */
public class StockScreen extends Screen {
    private static final int W = 380, H = 248, LIST_W = 124, ROW = 28, NEWS_H = 40;

    private JsonObject data;
    private final List<JsonObject> unions = new ArrayList<>();
    private final List<JsonObject> mine = new ArrayList<>();
    private int sel, fx, fy, amount = 10, scroll;
    private long openedAt, msgAt;
    private String msg = "";
    private boolean msgOk = true;
    /** 되팔기 확인: 이 증서의 [팔기]를 한 번 눌렀다 (3초 안에 다시 누르면 판다). */
    private String armed = "";
    private long armedAt;
    private boolean refreshAsked;

    public StockScreen(JsonObject o) {
        super(Component.translatable("rtshop.stock.title"));
        set(o);
    }

    public static void open(JsonObject o) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof StockScreen s) s.set(o);
        else mc.setScreen(new StockScreen(o));
    }

    private void set(JsonObject o) {
        String keep = unions.isEmpty() || sel >= unions.size() ? "" : unions.get(sel).get("id").getAsString();
        data = o;
        unions.clear();
        for (JsonElement e : o.getAsJsonArray("unions")) unions.add(e.getAsJsonObject());
        mine.clear();
        for (JsonElement e : o.getAsJsonArray("mine")) mine.add(e.getAsJsonObject());
        sel = 0;
        for (int i = 0; i < unions.size(); i++) if (unions.get(i).get("id").getAsString().equals(keep)) sel = i;
        openedAt = System.currentTimeMillis();
        refreshAsked = false;
        if (o.has("msg")) {
            JsonArray a = o.getAsJsonArray("args");
            Object[] args = new Object[a == null ? 0 : a.size()];
            for (int i = 0; i < args.length; i++) args[i] = a.get(i).getAsString();
            msg = Component.translatable(o.get("msg").getAsString(), args).getString();
            msgOk = !o.has("ok") || o.get("ok").getAsBoolean();
            msgAt = System.currentTimeMillis();
        }
        clampAmount();
        armed = "";
    }

    private boolean trade() {
        return data.has("trade") && data.get("trade").getAsBoolean();
    }

    private static boolean en() {
        return HudLang.storyEn();
    }

    private static String name(JsonObject u) {
        return en() && u.has("nameEn") ? u.get("nameEn").getAsString() : u.get("name").getAsString();
    }

    private String unionName(String id) {
        for (JsonObject u : unions) if (u.get("id").getAsString().equals(id)) return name(u);
        return id;
    }

    private int unionColor(String id) {
        for (JsonObject u : unions) if (u.get("id").getAsString().equals(id)) return 0xFF000000 | u.get("color").getAsInt();
        return Ui.C_INK;
    }

    private void clampAmount() {
        int max = Math.max(1, Math.min(data.get("maxPer").getAsInt(), Math.max(1, data.get("buyLeft").getAsInt())));
        amount = Math.max(1, Math.min(max, amount));
    }

    @Override
    protected void init() {
        fx = (width - W) / 2;
        fy = (height - H) / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private long left(String key) {
        return Math.max(0, data.get(key).getAsLong() - (System.currentTimeMillis() - openedAt) / 1000L);
    }

    static String pct(double v) {
        return (v >= 0 ? "+" : "") + String.format("%.1f%%", v * 100);
    }

    private static String hm(long s) {
        long d = s / 86400, h = (s % 86400) / 3600, m = (s % 3600) / 60;
        if (d > 0) return Component.translatable("rtshop.stock.left_dh", d, h).getString();
        return Component.translatable("rtshop.stock.left_hm", h, m).getString();
    }

    @Override
    public void tick() {
        // 날이 바뀌면 새 시세를 한 번 받아 온다
        if (!refreshAsked && data.get("resetIn").getAsLong() > 0 && left("resetIn") == 0) {
            refreshAsked = true;
            ShopNet.CHANNEL.sendToServer(new StockPacket(0, "", 0, ""));
        }
        if (!armed.isEmpty() && System.currentTimeMillis() - armedAt > 3000) armed = "";
    }

    // ------------------------------------------------------------------ 그리기

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        Ui.woodFrame(g, fx, fy, W, H);
        Ui.titleBand(g, fx + 5, fy + 4, fx + W - 5, fy + 23);
        g.fill(fx + 5, fy + 22, fx + W - 5, fy + 23, Ui.C_GOLD_DARK);
        String t = Component.translatable(trade() ? "rtshop.stock.title_trade" : "rtshop.stock.title").getString();
        g.drawString(font, t, fx + 10, fy + 9, Ui.C_GOLD, true);
        String lf = Component.translatable("rtshop.stock.next_price", hm(left("resetIn"))).getString();
        Ui.scaledText(g, font, lf, fx + W - 10 - font.width(lf) * 0.8F, fy + 10, 0.8F, Ui.C_CREAM, false);

        int lx = fx + 8, ly = fy + 28, lh = H - 36 - NEWS_H - 4;
        Ui.bevel(g, lx, ly, LIST_W, lh, Ui.C_WOOD_DARK, Ui.C_WOOD, Ui.C_SHADOW);
        for (int i = 0; i < unions.size() && i < lh / ROW; i++) drawRow(g, unions.get(i), lx, ly + 2 + i * ROW, i == sel, Ui.inside(mx, my, lx + 2, ly + 2 + i * ROW, LIST_W - 4, ROW - 1));
        int dx = lx + LIST_W + 6, dw = W - LIST_W - 22;
        Ui.bevel(g, dx, ly, dw, lh, Ui.C_PARCH, Ui.C_PARCH_HI, Ui.C_PARCH_SHADE);
        checkTip = null;
        if (!unions.isEmpty()) drawDetail(g, unions.get(sel), dx, ly, dw, lh, mx, my);
        drawNews(g, lx, fy + H - NEWS_H - 8, W - 16, NEWS_H, mx, my);
        super.render(g, mx, my, partial);
        if (checkTip != null) g.renderTooltip(font, checkTip, mx, my);
    }

    private void drawRow(GuiGraphics g, JsonObject u, int x, int y, boolean on, boolean hov) {
        if (on || hov) g.fill(x + 2, y, x + LIST_W - 2, y + ROW - 1, on ? 0x80E8B84A : 0x40E8B84A);
        int col = 0xFF000000 | u.get("color").getAsInt();
        g.fill(x + 4, y + 3, x + 7, y + ROW - 4, col);
        g.drawString(font, Ui.clip(font, name(u), LIST_W - 16), x + 10, y + 3, on ? 0xFFFFF2C8 : Ui.C_CREAM, false);
        String price = Ui.num(u.get("price").getAsLong()) + " G";
        Ui.scaledText(g, font, price, x + 10, y + 15, 0.8F, Ui.C_GOLD, false);
        double ch = u.get("change").getAsDouble();
        String c = pct(ch);
        Ui.scaledText(g, font, c, x + 10 + font.width(price) * 0.8F + 4, y + 15, 0.8F, ch > 0.0005 ? 0xFF8FD46A : ch < -0.0005 ? 0xFFE07A6A : 0xFFB8A888, false);
        // 작은 그래프
        JsonArray h = u.getAsJsonArray("history");
        spark(g, h, x + LIST_W - 36, y + 14, 30, 10, col, 1);
    }

    /** 값들을 꺾은선으로 (두께 t). */
    private static void spark(GuiGraphics g, JsonArray h, int x, int y, int w, int hh, int col, int t) {
        if (h == null || h.size() < 2) return;
        long lo = Long.MAX_VALUE, hi = Long.MIN_VALUE;
        for (JsonElement e : h) {
            lo = Math.min(lo, e.getAsLong());
            hi = Math.max(hi, e.getAsLong());
        }
        double pad = Math.max(1, (hi - lo) * 0.1);
        double a = lo - pad, b = hi + pad;
        int n = h.size();
        int px = -1, py = -1;
        for (int i = 0; i < n; i++) {
            int qx = x + (int) Math.round((double) i * (w - 1) / (n - 1));
            int qy = y + hh - 1 - (int) Math.round((h.get(i).getAsLong() - a) / (b - a) * (hh - 1));
            if (px >= 0) line(g, px, py, qx, qy, col, t);
            px = qx;
            py = qy;
        }
    }

    private static void line(GuiGraphics g, int x0, int y0, int x1, int y1, int col, int t) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        for (int s = 0; s <= steps; s++) {
            int x = x0 + (steps == 0 ? 0 : (x1 - x0) * s / steps), y = y0 + (steps == 0 ? 0 : (y1 - y0) * s / steps);
            g.fill(x, y, x + t, y + t, col);
        }
    }

    private void drawDetail(GuiGraphics g, JsonObject u, int dx, int ly, int dw, int lh, int mx, int my) {
        int col = 0xFF000000 | u.get("color").getAsInt();
        int y = ly + 6;
        g.drawString(font, name(u), dx + 8, y, Ui.C_INK, false);
        long price = u.get("price").getAsLong();
        double ch = u.get("change").getAsDouble();
        String pc = pct(ch);
        String pr = Ui.num(price) + " G";
        int pcw = (int) (font.width(pc) * 0.8F);
        g.drawString(font, pr, dx + dw - 12 - pcw - font.width(pr), y, Ui.C_INK, false);
        Ui.scaledText(g, font, pc, dx + dw - 8 - pcw, y + 1, 0.8F, ch > 0.0005 ? Ui.C_GREEN : ch < -0.0005 ? Ui.C_RED : Ui.C_INK_SOFT, false);
        if (u.get("pending").getAsBoolean()) Ui.scaledText(g, font, Component.translatable("rtshop.stock.news_pending").getString(), dx + 8 + font.width(name(u)) + 6, y + 1, 0.8F, 0xFF9A5A2A, false);
        // 그래프 (지난 14일)
        int gx = dx + 30, gy = y + 13, gw = dw - 40, gh = 46;
        g.fill(gx, gy, gx + gw, gy + gh, 0x22000000);
        JsonArray h = u.getAsJsonArray("history");
        long lo = Long.MAX_VALUE, hi = Long.MIN_VALUE;
        for (JsonElement e : h) {
            lo = Math.min(lo, e.getAsLong());
            hi = Math.max(hi, e.getAsLong());
        }
        if (h.size() > 0) {
            String sh = Ui.compact(hi), sl = Ui.compact(lo);
            Ui.scaledText(g, font, sh, gx - 3 - font.width(sh) * 0.7F, gy, 0.7F, Ui.C_INK_SOFT, false);
            Ui.scaledText(g, font, sl, gx - 3 - font.width(sl) * 0.7F, gy + gh - 6, 0.7F, Ui.C_INK_SOFT, false);
        }
        for (int k = 1; k < 3; k++) g.fill(gx, gy + gh * k / 3, gx + gw, gy + gh * k / 3 + 1, 0x18000000);
        if (h.size() >= 2) spark(g, h, gx + 2, gy + 3, gw - 4, gh - 6, col, 2);
        else Ui.scaledText(g, font, Component.translatable("rtshop.stock.no_history").getString(), gx + 6, gy + gh / 2 - 3, 0.8F, Ui.C_INK_SOFT, false);
        Ui.scaledText(g, font, Component.translatable("rtshop.stock.days_ago", Math.max(0, h.size() - 1)).getString(), gx, gy + gh + 2, 0.7F, Ui.C_INK_SOFT, false);
        String today = Component.translatable("rtshop.stock.today").getString();
        Ui.scaledText(g, font, today, gx + gw - font.width(today) * 0.7F, gy + gh + 2, 0.7F, Ui.C_INK_SOFT, false);
        // 배당 · 발행 · 내 것
        y = gy + gh + 11;
        long div = u.get("dividend").getAsLong();
        String info = Component.translatable("rtshop.stock.info", div > 0 ? Ui.num(div) + " G" : "—", Ui.num(u.get("outstanding").getAsLong()), Ui.num(u.get("held").getAsLong())).getString();
        Ui.scaledText(g, font, Ui.clip(font, info, (int) ((dw - 16) / 0.8F)), dx + 8, y, 0.8F, Ui.C_INK, false);
        y += 11;
        g.fill(dx + 6, y, dx + dw - 6, y + 1, Ui.C_PARCH_SHADE);
        y += 4;
        if (trade()) {
            // 사기: [-] 주 수 [+] · 1 10 50 100 · 값 미리 보기 · [사기]
            int bx = dx + 8;
            Ui.button(g, font, bx, y, 14, 13, "-", Ui.inside(mx, my, bx, y, 14, 13), amount > 1);
            String am = Component.translatable("rtshop.stock.shares", amount).getString();
            g.drawString(font, am, bx + 16 + (36 - font.width(am)) / 2, y + 3, Ui.C_INK, false);
            Ui.button(g, font, bx + 54, y, 14, 13, "+", Ui.inside(mx, my, bx + 54, y, 14, 13), true);
            int[] q = {1, 10, 50, 100};
            for (int i = 0; i < q.length; i++) {
                int qx = bx + 74 + i * 24;
                Ui.button(g, font, qx, y, 22, 13, String.valueOf(q[i]), Ui.inside(mx, my, qx, y, 22, 13), q[i] <= data.get("maxPer").getAsInt());
            }
            int buyX = dx + dw - 50;
            boolean can = data.get("buyLeft").getAsInt() > 0 && data.get("enabled").getAsBoolean();
            Ui.button(g, font, buyX, y, 42, 13, Component.translatable("rtshop.stock.buy").getString(), Ui.inside(mx, my, buyX, y, 42, 13), can);
            y += 16;
            long cost = price * amount, fee = (long) Math.ceil(cost * data.get("fee").getAsDouble());
            String pv = wanted() ? Component.translatable("rtshop.stock.wanted_banner").getString()
                    : Component.translatable("rtshop.stock.cost", Ui.num(cost), Ui.num(fee), Ui.num(cost + fee), data.get("buyLeft").getAsInt()).getString();
            Ui.scaledText(g, font, Ui.clip(font, pv, (int) ((dw - 16) / 0.8F)), dx + 8, y, 0.8F, wanted() || cost + fee > data.get("wallet").getAsLong() ? Ui.C_RED : Ui.C_INK_SOFT, false);
            y += 10;
        } else {
            Ui.scaledText(g, font, Component.translatable("rtshop.stock.view_hint").getString(), dx + 8, y, 0.8F, Ui.C_INK_SOFT, false);
            y += 10;
        }
        // 내 증서 (이 조합)
        List<JsonObject> mineHere = mineOf(u.get("id").getAsString());
        int listTop = y + 2, rows = Math.max(1, (ly + lh - 14 - listTop) / 12);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, mineHere.size() - rows)));
        if (mineHere.isEmpty()) {
            Ui.scaledText(g, font, Component.translatable("rtshop.stock.none_mine").getString(), dx + 8, listTop + 2, 0.8F, Ui.C_INK_SOFT, false);
        }
        for (int i = 0; i < rows && i + scroll < mineHere.size(); i++) {
            JsonObject c = mineHere.get(i + scroll);
            int ry = listTop + i * 12;
            boolean valid = c.get("valid").getAsBoolean();
            boolean exposed = c.has("exposed") && c.get("exposed").getAsBoolean();
            String line = valid
                    ? Component.translatable("rtshop.stock.cert_line", String.format("%06d", c.get("no").getAsLong()), c.get("shares").getAsInt(), c.get("issued").getAsString(),
                    Ui.num(c.get("value").getAsLong())).getString()
                    : Component.translatable(exposed ? "rtshop.stock.cert_exposed" : "rtshop.stock.cert_invalid", String.format("%06d", c.get("no").getAsLong())).getString();
            if (valid && c.get("paid").getAsBoolean()) line += " ✔";
            boolean checkBtn = trade() && valid && checking();
            Ui.scaledText(g, font, Ui.clip(font, line, (int) ((dw - (trade() ? (checkBtn ? 98 : 66) : 16)) / 0.8F)), dx + 8, ry + 2, 0.8F, valid ? Ui.C_INK : Ui.C_RED, false);
            if (trade() && valid) {
                int sx = dx + dw - 50;
                boolean arm = armed.equals(c.get("id").getAsString());
                Ui.button(g, font, sx, ry, 42, 11, Component.translatable(arm ? "rtshop.stock.sell_confirm" : "rtshop.stock.sell").getString(), Ui.inside(mx, my, sx, ry, 42, 11), true);
            }
            if (checkBtn) {
                // 3.76.0: 봉랍 확인 (수수료를 내고 거래소가 위조인지 살핀다)
                int cx = dx + dw - 82;
                boolean over = Ui.inside(mx, my, cx, ry, 30, 11);
                Ui.button(g, font, cx, ry, 30, 11, Component.translatable("rtshop.stock.check").getString(), over, true);
                if (over && c.has("checkFee")) checkTip = Component.translatable("rtshop.stock.check_tip", Ui.num(c.get("checkFee").getAsLong()));
            }
        }
        if (mineHere.size() > rows) {
            String more = (scroll + 1) + "-" + Math.min(mineHere.size(), scroll + rows) + " / " + mineHere.size();
            Ui.scaledText(g, font, more, dx + dw - 8 - font.width(more) * 0.7F, ly + lh - 9, 0.7F, Ui.C_INK_SOFT, false);
        }
        // 결과 글
        if (!msg.isEmpty() && System.currentTimeMillis() - msgAt < 6000) {
            Ui.scaledText(g, font, Ui.clip(font, msg, (int) ((dw - 16) / 0.8F)), dx + 8, ly + lh - 9, 0.8F, msgOk ? Ui.C_GREEN : Ui.C_RED, false);
        }
    }

    /** 봉랍 확인 단추 위의 설명 (render 끝에 그린다). */
    private Component checkTip;

    private boolean checking() {
        return data != null && data.has("checking") && data.get("checking").getAsBoolean();
    }

    private boolean wanted() {
        return data != null && data.has("wanted") && data.get("wanted").getAsBoolean();
    }

    private List<JsonObject> mineOf(String union) {
        List<JsonObject> out = new ArrayList<>();
        for (JsonObject c : mine) if (c.get("union").getAsString().equals(union)) out.add(c);
        return out;
    }

    private long dueTotal() {
        long t = 0;
        for (JsonObject c : mine) if (c.has("due")) t += c.get("due").getAsLong();
        return t;
    }

    private int claimX() {
        return fx + W - 8 - 118;
    }

    private void drawNews(GuiGraphics g, int x, int y, int w, int h, int mx, int my) {
        Ui.bevel(g, x, y, w, h, Ui.C_PARCH, Ui.C_PARCH_HI, Ui.C_PARCH_SHADE);
        JsonArray ns = data.getAsJsonArray("news");
        int tw = w - 130;
        if (ns.size() == 0) {
            Ui.scaledText(g, font, Component.translatable("rtshop.stock.no_news").getString(), x + 6, y + 5, 0.8F, Ui.C_INK_SOFT, false);
        }
        for (int i = 0; i < ns.size() && i < 3; i++) {
            JsonObject n = ns.get(i).getAsJsonObject();
            String text = en() && n.has("textEn") ? n.get("textEn").getAsString() : n.get("text").getAsString();
            boolean story = !n.has("kind") || n.get("kind").getAsString().equals("story");
            boolean notice = n.has("kind") && n.get("kind").getAsString().equals("notice");
            String head = notice ? "⚠ " + n.get("date").getAsString() + " · "
                    : (story ? "✦ " : "") + n.get("date").getAsString() + " " + unionName(n.get("union").getAsString()) + " " + pct(n.get("change").getAsDouble()) + " · ";
            int ly = y + 4 + i * 11;
            Ui.scaledText(g, font, head, x + 6, ly, 0.8F, notice ? Ui.C_RED : unionColor(n.get("union").getAsString()), false);
            Ui.scaledText(g, font, Ui.clip(font, text, (int) ((tw - font.width(head) * 0.8F) / 0.8F)), x + 6 + font.width(head) * 0.8F, ly, 0.8F, Ui.C_INK, false);
        }
        String fund = Component.translatable("rtshop.stock.fund", Ui.num(data.get("fund").getAsLong())).getString();
        Ui.scaledText(g, font, fund, x + w - 8 - font.width(fund) * 0.8F, y + 4, 0.8F, Ui.C_INK_SOFT, false);
        String wk = Component.translatable("rtshop.stock.next_dividend", hm(left("weekIn"))).getString();
        Ui.scaledText(g, font, wk, x + w - 8 - font.width(wk) * 0.7F, y + 13, 0.7F, Ui.C_INK_SOFT, false);
        long due = dueTotal();
        if (trade()) {
            String label = due > 0 ? Component.translatable("rtshop.stock.claim", Ui.num(due)).getString() : Component.translatable("rtshop.stock.claim_none").getString();
            Ui.button(g, font, claimX(), y + h - 16, 110, 13, label, Ui.inside(mx, my, claimX(), y + h - 16, 110, 13), due > 0);
        }
    }

    // ------------------------------------------------------------------ 누르기

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        int lx = fx + 8, ly = fy + 28, lh = H - 36 - NEWS_H - 4;
        if (Ui.inside(mx, my, lx, ly, LIST_W, lh)) {
            int i = (int) ((my - ly - 2) / ROW);
            if (i >= 0 && i < unions.size() && i != sel) {
                sel = i;
                scroll = 0;
                armed = "";
                click();
            }
            return true;
        }
        if (trade() && !unions.isEmpty()) {
            int dx = lx + LIST_W + 6, dw = W - LIST_W - 22;
            JsonObject u = unions.get(sel);
            int y = buyRowY(ly);
            int bx = dx + 8;
            if (Ui.inside(mx, my, bx, y, 14, 13)) {
                amount = Math.max(1, amount - (hasShiftDown() ? 10 : 1));
                clampAmount();
                click();
                return true;
            }
            if (Ui.inside(mx, my, bx + 54, y, 14, 13)) {
                amount += hasShiftDown() ? 10 : 1;
                clampAmount();
                click();
                return true;
            }
            int[] q = {1, 10, 50, 100};
            for (int i = 0; i < q.length; i++) {
                if (Ui.inside(mx, my, bx + 74 + i * 24, y, 22, 13) && q[i] <= data.get("maxPer").getAsInt()) {
                    amount = q[i];
                    clampAmount();
                    click();
                    return true;
                }
            }
            if (Ui.inside(mx, my, dx + dw - 50, y, 42, 13) && data.get("buyLeft").getAsInt() > 0) {
                click();
                ShopNet.CHANNEL.sendToServer(new StockPacket(1, u.get("id").getAsString(), amount, ""));
                return true;
            }
            // 내 증서 [팔기] (두 번 눌러 확인)
            List<JsonObject> mineHere = mineOf(u.get("id").getAsString());
            int listTop = y + 16 + 10 + 2, rows = Math.max(1, (ly + lh - 14 - listTop) / 12);
            for (int i = 0; i < rows && i + scroll < mineHere.size(); i++) {
                JsonObject c = mineHere.get(i + scroll);
                if (!c.get("valid").getAsBoolean()) continue;
                int ry = listTop + i * 12;
                if (checking() && Ui.inside(mx, my, dx + dw - 82, ry, 30, 11)) {
                    click();
                    ShopNet.CHANNEL.sendToServer(new StockPacket(4, "", 0, c.get("id").getAsString()));
                    return true;
                }
                if (Ui.inside(mx, my, dx + dw - 50, ry, 42, 11)) {
                    String id = c.get("id").getAsString();
                    click();
                    if (armed.equals(id)) {
                        armed = "";
                        ShopNet.CHANNEL.sendToServer(new StockPacket(2, "", 0, id));
                    } else {
                        armed = id;
                        armedAt = System.currentTimeMillis();
                    }
                    return true;
                }
            }
            int ny = fy + H - NEWS_H - 8;
            if (dueTotal() > 0 && Ui.inside(mx, my, claimX(), ny + NEWS_H - 16, 110, 13)) {
                click();
                ShopNet.CHANNEL.sendToServer(new StockPacket(3, "", 0, ""));
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    /** 사기 줄의 y (drawDetail 과 같은 계산). */
    private int buyRowY(int ly) {
        int y = ly + 6;
        int gy = y + 13, gh = 46;
        y = gy + gh + 11;
        y += 11;
        y += 4;
        return y;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scroll = Math.max(0, scroll - (int) Math.signum(delta));
        return true;
    }

    private void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    // ---- 시험용
    public int unionsForTest() {
        return unions.size();
    }

    public boolean tradeForTest() {
        return trade();
    }

    /** 점검용: 내 증서 줄의 {판명되지 않은 것(진짜로 보이는 것), 위조 판명}. */
    public int[] rowsForTest() {
        int ok = 0, ex = 0;
        for (JsonObject c : mine) {
            if (c.has("exposed") && c.get("exposed").getAsBoolean()) ex++;
            else if (c.get("valid").getAsBoolean()) ok++;
        }
        return new int[]{ok, ex};
    }

    public boolean checkingForTest() {
        return checking();
    }

    public int mineForTest() {
        return mine.size();
    }

    public String msgForTest() {
        return msg;
    }

    public void selectForTest(String id) {
        for (int i = 0; i < unions.size(); i++) if (unions.get(i).get("id").getAsString().equals(id)) sel = i;
    }

    /** 조합 이름 · 시세 · 소식이 칸 안에 들어가는가 (긴 영어 이름 포함). */
    public boolean fitsForTest() {
        for (JsonObject u : unions) {
            if (font.width(name(u)) > LIST_W - 16) return false;
        }
        return unions.size() * ROW <= H - 36 - NEWS_H - 4;
    }
}

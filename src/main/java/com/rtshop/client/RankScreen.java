package com.rtshop.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * 주간 순위 (순위 기록원): 왼쪽에서 종목을 고르면 오른쪽에 이번 주(또는 지난주) 순위가 나온다.
 * 아래에는 내 순위와 보상, 초기화까지 남은 시간, 맡겨 둔 보상을 받는 단추.
 */
public class RankScreen extends Screen {
    private static final int W = 336, H = 232, LIST_W = 96, ROW = 13, CAT_ROW = 17;
    private static final int[] MEDAL = {0xFFB8861E, 0xFF7C7C84, 0xFF9A5A2A};

    private JsonObject data;
    private final List<JsonObject> cats = new ArrayList<>();
    private int sel, fx, fy;
    private boolean lastWeek;
    private long openedAt = System.currentTimeMillis();

    public RankScreen(JsonObject o) {
        super(Component.translatable("rtshop.rank.title"));
        set(o);
    }

    public static void open(JsonObject o) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof RankScreen r) r.set(o);
        else mc.setScreen(new RankScreen(o));
    }

    private void set(JsonObject o) {
        data = o;
        cats.clear();
        for (JsonElement e : o.getAsJsonArray("cats")) cats.add(e.getAsJsonObject());
        sel = Math.max(0, Math.min(sel, cats.size() - 1));
        openedAt = System.currentTimeMillis();
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

    static boolean gold(String cat) {
        return cat.equals("earned") || cat.equals("spent") || cat.equals("gold");
    }

    static String value(String cat, long v) {
        return gold(cat) ? Ui.num(v) + " G" : Ui.num(v);
    }

    private String left() {
        long s = Math.max(0, data.get("resetIn").getAsLong() - (System.currentTimeMillis() - openedAt) / 1000L);
        long d = s / 86400, h = (s % 86400) / 3600, m = (s % 3600) / 60;
        if (d > 0) return Component.translatable("rtshop.rank.left_dh", d, h).getString();
        return Component.translatable("rtshop.rank.left_hm", h, m).getString();
    }

    private int btnX() {
        return fx + W - 118;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        Ui.woodFrame(g, fx, fy, W, H);
        Ui.titleBand(g, fx + 5, fy + 4, fx + W - 5, fy + 23);
        g.fill(fx + 5, fy + 22, fx + W - 5, fy + 23, Ui.C_GOLD_DARK);
        String t = title.getString();
        g.drawString(font, t, fx + (W - font.width(t)) / 2, fy + 9, Ui.C_GOLD, true);
        String lf = left();
        Ui.scaledText(g, font, lf, fx + W - 10 - font.width(lf) * 0.8F, fy + 10, 0.8F, Ui.C_CREAM, false);
        int lx = fx + 8, ly = fy + 28, lh = H - 36;
        Ui.bevel(g, lx, ly, LIST_W, lh, Ui.C_WOOD_DARK, Ui.C_WOOD, Ui.C_SHADOW);
        for (int i = 0; i < cats.size(); i++) {
            JsonObject c = cats.get(i);
            int ry = ly + 3 + i * CAT_ROW;
            boolean on = i == sel, hov = Ui.inside(mx, my, lx + 2, ry, LIST_W - 4, CAT_ROW - 1);
            if (on || hov) g.fill(lx + 2, ry, lx + LIST_W - 2, ry + CAT_ROW - 1, on ? 0x80E8B84A : 0x40E8B84A);
            String name = Component.translatable("rtshop.rank.cat." + c.get("id").getAsString()).getString();
            g.drawString(font, Ui.clip(font, name, LIST_W - 24), lx + 6, ry + 4, on ? 0xFFFFF2C8 : Ui.C_CREAM, false);
            int my_ = c.get("myRank").getAsInt();
            if (my_ > 0) {
                String r = "#" + my_;
                Ui.scaledText(g, font, r, lx + LIST_W - 5 - font.width(r) * 0.8F, ry + 5, 0.8F, my_ <= 3 ? Ui.C_GOLD : 0xFFB8A888, false);
            }
        }
        int dx = lx + LIST_W + 6, dw = W - LIST_W - 22;
        Ui.bevel(g, dx, ly, dw, lh, Ui.C_PARCH, Ui.C_PARCH_HI, Ui.C_PARCH_SHADE);
        if (cats.isEmpty()) {
            super.render(g, mx, my, partial);
            return;
        }
        JsonObject c = cats.get(sel);
        String cat = c.get("id").getAsString();
        g.drawString(font, Component.translatable("rtshop.rank.cat." + cat).getString(), dx + 8, ly + 7, Ui.C_INK, false);
        // 이번 주 / 지난주
        String tw = Component.translatable(lastWeek ? "rtshop.rank.last_week" : "rtshop.rank.this_week").getString();
        int tx = dx + dw - 62;
        Ui.button(g, font, tx, ly + 3, 56, 14, tw, Ui.inside(mx, my, tx, ly + 3, 56, 14), true);
        g.fill(dx + 6, ly + 20, dx + dw - 6, ly + 21, Ui.C_PARCH_SHADE);
        JsonArray rows = c.getAsJsonArray(lastWeek ? "last" : "top");
        int y = ly + 25;
        if (rows.size() == 0) {
            g.drawString(font, Component.translatable(lastWeek ? "rtshop.rank.none_last" : "rtshop.rank.none").getString(), dx + 8, y + 2, Ui.C_INK_SOFT, false);
        }
        for (int i = 0; i < rows.size() && i < 10; i++) {
            JsonObject r = rows.get(i).getAsJsonObject();
            boolean me = r.get("me").getAsBoolean();
            if (me) g.fill(dx + 4, y - 1, dx + dw - 4, y + ROW - 2, 0x40E8B84A);
            String rank = String.valueOf(i + 1);
            if (i < 3) {
                g.fill(dx + 7, y, dx + 19, y + 10, MEDAL[i]);
                g.drawString(font, rank, dx + 13 - font.width(rank) / 2, y + 1, 0xFFFFFFFF, false);
            } else {
                g.drawString(font, rank, dx + 13 - font.width(rank) / 2, y + 1, Ui.C_INK_SOFT, false);
            }
            String v = value(cat, r.get("value").getAsLong());
            g.drawString(font, Ui.clip(font, r.get("name").getAsString(), dw - 40 - font.width(v)), dx + 24, y + 1, me ? 0xFF7A3B1E : Ui.C_INK, false);
            g.drawString(font, v, dx + dw - 8 - font.width(v), y + 1, me ? 0xFF7A3B1E : Ui.C_INK, false);
            y += ROW;
        }
        // 아래: 내 기록 · 보상 · 받기
        int by = ly + lh - 42;
        g.fill(dx + 6, by, dx + dw - 6, by + 1, Ui.C_PARCH_SHADE);
        int myRank = c.get("myRank").getAsInt();
        String mine = myRank > 0
                ? Component.translatable("rtshop.rank.mine", myRank, c.get("players").getAsInt(), value(cat, c.get("myValue").getAsLong())).getString()
                : Component.translatable("rtshop.rank.mine_none").getString();
        g.drawString(font, Ui.clip(font, mine, dw - 16), dx + 8, by + 5, Ui.C_INK, false);
        StringBuilder rw = new StringBuilder(Component.translatable("rtshop.rank.rewards").getString());
        JsonArray rewards = c.getAsJsonArray("rewards");
        for (int i = 0; i < rewards.size(); i++) rw.append(i == 0 ? " " : " · ").append(i + 1).append(Component.translatable("rtshop.rank.place").getString())
                .append(" ").append(Ui.num(rewards.get(i).getAsLong())).append(" G");
        Ui.scaledText(g, font, Ui.clip(font, rw.toString(), (int) ((dw - 16) / 0.8F)), dx + 8, by + 17, 0.8F, Ui.C_INK_SOFT, false);
        long pending = data.get("pending").getAsLong();
        if (pending > 0) {
            String label = Component.translatable("rtshop.rank.claim", Ui.num(pending)).getString();
            Ui.button(g, font, btnX(), by + 26, 104, 14, label, Ui.inside(mx, my, btnX(), by + 26, 104, 14), true);
        } else {
            Ui.scaledText(g, font, Component.translatable("rtshop.rank.claim_hint").getString(), dx + 8, by + 29, 0.8F, Ui.C_INK_SOFT, false);
        }
        super.render(g, mx, my, partial);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        int lx = fx + 8, ly = fy + 28, lh = H - 36;
        if (Ui.inside(mx, my, lx, ly, LIST_W, lh)) {
            int i = (int) ((my - ly - 3) / CAT_ROW);
            if (i >= 0 && i < cats.size() && i != sel) {
                sel = i;
                click();
            }
            return true;
        }
        int dx = lx + LIST_W + 6, dw = W - LIST_W - 22;
        if (Ui.inside(mx, my, dx + dw - 62, ly + 3, 56, 14)) {
            lastWeek = !lastWeek;
            click();
            return true;
        }
        int by = ly + lh - 42;
        if (data.get("pending").getAsLong() > 0 && Ui.inside(mx, my, btnX(), by + 26, 104, 14)) {
            click();
            claim();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    private void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private void claim() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.connection.sendCommand("rtshop rank claim");
    }

    // ---- 시험용
    public int catsForTest() {
        return cats.size();
    }

    public void selectForTest(String cat) {
        for (int i = 0; i < cats.size(); i++) if (cats.get(i).get("id").getAsString().equals(cat)) sel = i;
    }

    public int rowsForTest() {
        return cats.isEmpty() ? 0 : cats.get(sel).getAsJsonArray(lastWeek ? "last" : "top").size();
    }

    public void lastWeekForTest(boolean on) {
        lastWeek = on;
    }

    public long pendingForTest() {
        return data.get("pending").getAsLong();
    }

    public void claimForTest() {
        claim();
    }

    /** 종목 이름과 맨 윗줄의 값이 칸 안에 들어가는가. */
    public boolean fitsForTest() {
        int dw = W - LIST_W - 22;
        for (JsonObject c : cats) {
            String cat = c.get("id").getAsString();
            if (font.width(Component.translatable("rtshop.rank.cat." + cat).getString()) > LIST_W - 24) return false;
            for (JsonElement e : c.getAsJsonArray("top")) {
                if (font.width(value(cat, e.getAsJsonObject().get("value").getAsLong())) > dw - 80) return false;
            }
        }
        return cats.size() * CAT_ROW <= H - 40;
    }
}

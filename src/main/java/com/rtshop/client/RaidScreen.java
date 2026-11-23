package com.rtshop.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rtshop.net.RaidActionPacket;
import com.rtshop.net.ShopNet;
import com.rtshop.raid.Raid;
import com.rtshop.raid.RaidConfig;
import com.rtshop.raid.Reward;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 레이드 안내인 창: 짧은 대화(3줄) → 불꽃 테마의 입장 화면(조건·보상 목록) → "정말 입장하시겠습니까?" 확인.
 */
public class RaidScreen extends Screen {
    private static final int W = 400, H = 226;
    public static final int[] GRADE_COLORS = {0xFFB8B8B8, 0xFF55C24A, 0xFF4A9BE8, 0xFFB45AE8, 0xFFF0A030, 0xFF101010};
    public static final String[] GRADE_KEYS = {"rtshop.gacha.grade.common", "rtshop.gacha.grade.uncommon", "rtshop.gacha.grade.rare",
            "rtshop.gacha.grade.epic", "rtshop.gacha.grade.legendary", ""};

    private final JsonObject data;
    /** 0~2 대화, 3 입장 화면. */
    private int phase = 0;
    private boolean confirm = false;
    private int scroll = 0;
    private final long openedAt = System.currentTimeMillis();

    private record Row(int kind, String text, int color, ItemStack stack, String right) {
    }

    private final List<Row> rows = new ArrayList<>();
    /** 입장 화면의 일러스트: 불꽃 후광을 등지고 불타는 대검을 짚고 선 화염의 군주 (이 모드의 창작 그림). */
    private static final net.minecraft.resources.ResourceLocation LORD = new net.minecraft.resources.ResourceLocation("rtshop", "textures/gui/raid_lord.png");
    public RaidScreen(JsonObject data) {
        super(Component.literal("raid"));
        this.data = data;
        buildRows();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private String t(String k) {
        JsonObject t = data.getAsJsonObject("texts");
        return t.has(k) ? t.get(k).getAsString() : k;
    }

    public static String gradeName(int g) {
        return g >= 5 ? "[???]" : Component.translatable(GRADE_KEYS[g]).getString();
    }

    private static String pct(double v) {
        return (Math.abs(v - Math.rint(v)) < 1e-9 ? Long.toString((long) Math.rint(v)) : Double.toString(v)) + "%";
    }

    private void buildRows() {
        rows.clear();
        boolean claimed = data.get("weeklyClaimed").getAsBoolean();
        List<Reward> all = new ArrayList<>();
        for (JsonElement e : data.getAsJsonArray("rewards")) all.add(Reward.parse(e.getAsJsonObject()));
        rows.add(new Row(0, t("rewardWeekly"), 0xFFE8B84A, ItemStack.EMPTY, claimed ? "✔" : ""));
        long gold = data.get("weeklyGold").getAsLong();
        if (gold > 0) {
            rows.add(new Row(2, Ui.num(gold) + " G", 0xFFE8B84A, ItemStack.EMPTY, claimed ? "✔" : ""));
        }
        for (Reward r : all) {
            if (!r.group.equals(RaidConfig.G_WEEKLY)) continue;
            ItemStack s = r.stack();
            rows.add(new Row(1, s.getHoverName().getString() + (s.getCount() > 1 ? " ×" + s.getCount() : ""), 0xFFF5E6C0, s, claimed ? "✔" : ""));
        }
        JsonArray ch = data.getAsJsonArray(claimed ? "chanceRepeat" : "chanceFresh");
        int rolls = data.get(claimed ? "rollsRepeat" : "rollsFresh").getAsInt();
        rows.add(new Row(0, t("rewardDrops") + "  ×" + rolls, 0xFFE8B84A, ItemStack.EMPTY, ""));
        for (int g = 5; g >= 0; g--) {
            boolean any = false;
            for (Reward r : all) {
                if (r.group.equals(RaidConfig.G_DROP) && r.grade == g) any = true;
            }
            if (!any) continue;
            rows.add(new Row(3, gradeName(g), GRADE_COLORS[g], ItemStack.EMPTY, pct(ch.get(g).getAsDouble())));
            for (Reward r : all) {
                if (!r.group.equals(RaidConfig.G_DROP) || r.grade != g) continue;
                ItemStack s = r.stack();
                if (g == 5) {
                    rows.add(new Row(1, "???", 0xFF9A9A9A, ItemStack.EMPTY, ""));
                } else {
                    rows.add(new Row(1, s.getHoverName().getString() + (s.getCount() > 1 ? " ×" + s.getCount() : ""), 0xFFF5E6C0, s, ""));
                }
            }
        }
    }

    private int fx() {
        return (this.width - W) / 2;
    }

    private int fy() {
        return (this.height - H) / 2;
    }

    // ---------------------------------------------------------------- 배치 (일러스트와 글자는 서로 다른 칸에만 놓인다)
    private static final int ART_X = 16, ART_Y = 52, ART_W = 156, ART_H = 136;
    private static final int COL_X = 186, COL_W = W - 186 - 16;
    private static final int ROW_H = 13;

    private int[] enterBtn() {
        return new int[]{fx() + ART_X, fy() + H - 28, ART_W, 18};
    }

    private int[] closeBtn() {
        return new int[]{fx() + W - 24, fy() + 10, 14, 14};
    }

    private int[] yesBtn() {
        return new int[]{this.width / 2 - 112, this.height / 2 + 24, 106, 18};
    }

    private int[] noBtn() {
        return new int[]{this.width / 2 + 6, this.height / 2 + 24, 106, 18};
    }

    /** 오른쪽 칸 위쪽(조건·수치)이 끝나는 높이 = 보상 목록이 시작하는 높이. 그릴 때 정해진다. */
    private int listTop = 140;

    private int listX() {
        return fx() + COL_X + 4;
    }

    private int listY() {
        return listTop;
    }

    private int listW() {
        return COL_W - 10;
    }

    private int listH() {
        return fy() + H - 14 - listTop;
    }

    private int maxScroll() {
        return Math.max(0, rows.size() * ROW_H - listH());
    }

    private String tr(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        if (phase < 3) {
            renderDialogue(g);
            return;
        }
        int x = fx(), y = fy();
        float time = (System.currentTimeMillis() - openedAt) / 1000F;
        Fx2D.begin(g, false);
        Fx2D.rect(0, 0, this.width, this.height, 0xB8000000, 0xB8000000, 0xD8000000, 0xD8000000);
        Fx2D.end();
        RaidFx.luxPanel(g, x, y, W, H, time, RaidFx.FIRE);
        RaidFx.embers(g, x + 6, y + 40, W - 12, H - 46, time, 26, false);

        // ── 머리: 제목, 부제, 금줄
        RaidFx.luxTitle(g, this.font, t("uiTitle"), x + W / 2, y + 9, 0xF3D9A0, time);
        String sub = t("uiSub");
        int sw = RaidText.w(this.font, sub, RaidText.SANS);
        RaidText.center(g, this.font, sub, RaidText.SANS, x + W / 2, y + 29, 0xFFC99A62);
        RaidFx.hairline(g, x + W / 2F - sw / 2F - 70, x + W / 2F - sw / 2F - 8, y + 33, 0xD4A85A, 0.9F);
        RaidFx.hairline(g, x + W / 2F + sw / 2F + 8, x + W / 2F + sw / 2F + 70, y + 33, 0xD4A85A, 0.9F);
        RaidFx.hairline(g, x + 12, x + W - 12, y + 42, 0xD4A85A, 0.75F);
        int[] c = closeBtn();
        boolean ch = !confirm && Ui.inside(mx, my, c[0], c[1], c[2], c[3]);
        RaidFx.box(g, c[0], c[1], c[0] + c[2], c[1] + c[3], ch ? RaidFx.GOLD_HI : RaidFx.GOLD_LO);
        g.fill(c[0] + 1, c[1] + 1, c[0] + c[2] - 1, c[1] + c[3] - 1, ch ? 0xFF4A1A10 : 0xFF180E0C);
        Fx2D.begin(g, false);
        Fx2D.line(c[0] + 4, c[1] + 4, c[0] + c[2] - 4, c[1] + c[3] - 4, 1.3F, RaidFx.GOLD_HI, RaidFx.GOLD_HI);
        Fx2D.line(c[0] + 4, c[1] + c[3] - 4, c[0] + c[2] - 4, c[1] + 4, 1.3F, RaidFx.GOLD_HI, RaidFx.GOLD_HI);
        Fx2D.end();

        // ── 왼쪽: 액자 속 일러스트 (그 위에는 글자를 얹지 않는다)
        int ax = x + ART_X, ay = y + ART_Y;
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        g.fill(ax, ay, ax + ART_W, ay + ART_H, 0xFF0E0506);
        g.blit(LORD, ax, ay, ART_W, ART_H, 0F, 0F, 512, 448, 512, 448);
        g.enableScissor(ax, ay, ax + ART_W, ay + ART_H);
        RaidFx.embers(g, ax, ay + 10, ART_W, ART_H - 10, time * 1.2F + 5F, 18, false);
        Fx2D.begin(g, true);
        Fx2D.glow(ax + ART_W * 0.5F, ay + ART_H * 0.265F, 14, Fx2D.rgba(0xFFE0A0, 0.18F + 0.14F * (float) Math.sin(time * 2.4F)), Fx2D.rgba(0xFF8030, 0));
        Fx2D.end();
        g.disableScissor();
        RaidFx.frame(g, ax, ay, ART_W, ART_H, time);

        // ── 오른쪽 위: 입장 조건
        int cx = x + COL_X, cw = COL_W;
        RaidFx.section(g, this.font, tr("rtshop.raid.ui_req"), cx, y + 50, cw);
        int ly = y + 63;
        int need = data.get("needKills").getAsInt(), kills = data.get("kills").getAsInt();
        if (need > 0) {
            boolean ok = kills >= need;
            RaidFx.badge(g, cx + 6, ly + 4, ok);
            RaidText.draw(g, this.font, RaidText.clip(this.font, tr("rtshop.raid.ui_kills", kills, need), RaidText.SANS, cw - 16), RaidText.SANS, cx + 14, ly, ok ? 0xFFE9F5E0 : 0xFFF2C8BE);
            ly += 12;
        }
        int tickets = data.get("tickets").getAsInt();
        RaidFx.badge(g, cx + 6, ly + 4, tickets > 0);
        RaidText.draw(g, this.font, RaidText.clip(this.font, tr("rtshop.raid.ui_tickets", tickets), RaidText.SANS, cw - 16), RaidText.SANS, cx + 14, ly, tickets > 0 ? 0xFFE9F5E0 : 0xFFF2C8BE);
        ly += 12;
        int st = data.get("state").getAsInt();
        boolean open = st == Raid.IDLE;
        Fx2D.begin(g, true);
        Fx2D.glow(cx + 6, ly + 4, 7, Fx2D.rgba(open ? 0x6FD483 : 0xFF5A48, 0.35F + 0.25F * (float) Math.sin(time * 3F)), Fx2D.rgba(open ? 0x6FD483 : 0xFF5A48, 0));
        Fx2D.end();
        Fx2D.begin(g, false);
        Fx2D.ring(cx + 6, ly + 4, 0, 3.2F, 0xFF000000, 0xFF000000);
        Fx2D.ring(cx + 6, ly + 4, 0, 2.4F, open ? 0xFFC8FFD0 : 0xFFFFD0C8, open ? 0xFF4FB463 : 0xFFE0402E);
        Fx2D.end();
        RaidText.draw(g, this.font, RaidText.clip(this.font, tr(open ? "rtshop.raid.ui_open" : "rtshop.raid.ui_locked", data.get("inside").getAsInt()), RaidText.SANS, cw - 16),
                RaidText.SANS, cx + 14, ly, open ? 0xFFD8CDB8 : 0xFFF2C8BE);
        ly += 12;
        String why = data.get("why").getAsString();
        if (!why.isEmpty() && !data.get("canEnter").getAsBoolean()) {
            List<FormattedCharSequence> wl = RaidText.split(this.font, why, RaidText.SANS, cw - 12);
            int n = Math.min(2, wl.size());
            g.fill(cx + 2, ly - 1, cx + cw, ly + n * 10 + 1, 0x70300806);
            g.fill(cx + 2, ly - 1, cx + 3, ly + n * 10 + 1, 0xFFFF5A48);
            for (int i = 0; i < n; i++) g.drawString(this.font, wl.get(i), cx + 7, ly + i * 10, 0xFFFF9A88, false);
            ly += n * 10 + 4;
        }

        // ── 수치 패: 보스 체력 · 보스 공격력 · 주간 초기화
        int ty = ly + 2, tw = (cw - 8) / 3, th = 25;
        long ms = data.get("resetIn").getAsLong();
        String[][] tiles = {
                {tr("rtshop.raid.ui_hp"), pctMult(data.get("health").getAsDouble()) + "%"},
                {tr("rtshop.raid.ui_atk"), pctMult(data.get("damage").getAsDouble()) + "%"},
                {tr("rtshop.raid.ui_reset_short"), tr("rtshop.raid.ui_time", ms / 86_400_000L, (ms / 3_600_000L) % 24)}};
        for (int i = 0; i < 3; i++) {
            int tx = cx + i * (tw + 4);
            Fx2D.begin(g, false);
            Fx2D.rect(tx, ty, tx + tw, ty + th, 0xFF2A1710, 0xFF2A1710, 0xFF120A09, 0xFF120A09);
            Fx2D.end();
            RaidFx.box(g, tx, ty, tx + tw, ty + th, 0xFF5A4024);
            g.fill(tx + 1, ty, tx + tw - 1, ty + 1, RaidFx.GOLD_MID);
            g.pose().pushPose();
            g.pose().translate(tx + tw / 2F, ty + 4, 0);
            g.pose().scale(0.8F, 0.8F, 1F);
            RaidText.center(g, this.font, tiles[i][0], RaidText.SANS, 0, 0, 0xFFB4946A);
            g.pose().popPose();
            String v = RaidText.clip(this.font, tiles[i][1], RaidText.SERIF, tw - 4);
            RaidText.center(g, this.font, v, RaidText.SERIF, tx + tw / 2, ty + 13, i == 2 ? 0xFFF3D9A0 : 0xFFFFB07A);
        }

        // ── 오른쪽 아래: 보상 목록
        listTop = ty + th + 8;
        int lx = listX(), lt = listY(), lw = listW(), lh = listH();
        g.fill(lx - 4, lt - 3, lx + lw + 6, lt + lh + 3, 0xD0080405);
        RaidFx.box(g, lx - 4, lt - 3, lx + lw + 6, lt + lh + 3, 0xFF5A4024);
        g.fill(lx - 3, lt - 3, lx + lw + 5, lt - 2, 0x80D4A85A);
        scroll = Math.min(scroll, maxScroll());
        g.enableScissor(lx - 3, lt - 2, lx + lw + 5, lt + lh + 2);
        int ry = lt - scroll;
        for (Row r : rows) {
            if (ry > lt - ROW_H - 2 && ry < lt + lh + 2) drawRow(g, r, lx, ry, lw);
            ry += ROW_H;
        }
        g.disableScissor();
        if (maxScroll() > 0) {
            int bx = lx + lw + 3;
            g.fill(bx, lt, bx + 1, lt + lh, 0xFF3A2818);
            int th2 = Math.max(10, lh * lh / (rows.size() * ROW_H));
            int tyy = lt + (lh - th2) * scroll / maxScroll();
            g.fill(bx - 1, tyy, bx + 2, tyy + th2, RaidFx.GOLD_MID);
            Fx2D.begin(g, true);
            Fx2D.glow(bx + 0.5F, lt + lh, 14, 6, Fx2D.rgba(0xD4A85A, scroll < maxScroll() ? 0.35F : 0), Fx2D.rgba(0xD4A85A, 0));
            Fx2D.end();
        }

        // ── 입장 단추 (일러스트 아래)
        boolean can = data.get("canEnter").getAsBoolean();
        int[] e = enterBtn();
        boolean hov = !confirm && can && Ui.inside(mx, my, e[0], e[1], e[2], e[3]);
        RaidFx.luxButton(g, this.font, e[0], e[1], e[2], e[3], t("enterButton"), can, hov, time, true);

        if (confirm) renderConfirm(g, mx, my, time);
    }

    private void drawRow(GuiGraphics g, Row r, int lx, int ry, int lw) {
        int ty = ry + 3;
        if (r.kind == 0) {
            Fx2D.begin(g, false);
            Fx2D.rect(lx - 3, ry, lx + lw + 5, ry + ROW_H - 1, 0xC0502A12, 0x00301008, 0x00301008, 0xC0502A12);
            Fx2D.end();
            g.fill(lx - 3, ry + ROW_H - 1, lx + lw + 5, ry + ROW_H, 0x40D4A85A);
            Fx2D.begin(g, false);
            Fx2D.diamond(lx + 2, ry + 6, 2.4F, 2.4F, 0, RaidFx.GOLD_MID);
            Fx2D.end();
            RaidText.draw(g, this.font, RaidText.clip(this.font, r.text, RaidText.SERIF, lw - 24), RaidText.SERIF, lx + 8, ty, RaidFx.GOLD_HI);
        } else if (r.kind == 3) {
            boolean myst = r.color == GRADE_COLORS[5];
            int col = myst ? 0xFF8A8A8A : r.color;
            Fx2D.begin(g, false);
            Fx2D.rect(lx + 2, ry + 1, lx + lw, ry + ROW_H - 1, Fx2D.alpha(col, 0.30F), 0x00000000, 0x00000000, Fx2D.alpha(col, 0.30F));
            Fx2D.diamond(lx + 6, ry + 6, 3F, 3F, 0, 0xFF000000);
            Fx2D.diamond(lx + 6, ry + 6, 2.2F, 2.2F, 0, myst ? 0xFF1A1A1A : col);
            Fx2D.end();
            String name = myst ? "???" : r.text;
            int nw = RaidText.w(this.font, name, RaidText.SANS);
            RaidText.draw(g, this.font, name, RaidText.SANS, lx + 12, ty, myst ? 0xFFB0B0B0 : col);
            int rw = RaidText.w(this.font, r.right, RaidText.SANS);
            for (int dx = lx + 16 + nw; dx < lx + lw - rw - 6; dx += 3) g.fill(dx, ry + 9, dx + 1, ry + 10, 0x50D4A85A);
            RaidText.right(g, this.font, r.right, RaidText.SANS, lx + lw - 1, ty, 0xFFF3D9A0);
            return;
        } else {
            int tx = lx + 10;
            if (!r.stack.isEmpty()) {
                g.pose().pushPose();
                g.pose().translate(lx + 9, ry, 0);
                g.pose().scale(0.75F, 0.75F, 1F);
                g.renderItem(r.stack, 0, 0);
                g.pose().popPose();
                tx = lx + 24;
            } else if (r.kind == 2) {
                Fx2D.begin(g, false);
                Fx2D.ring(lx + 15, ry + 6, 0, 4.2F, 0xFFFFE08A, 0xFFB07A20);
                Fx2D.ring(lx + 15, ry + 6, 2.2F, 2.9F, 0x90704010, 0x90704010);
                Fx2D.end();
                tx = lx + 24;
            }
            RaidText.draw(g, this.font, RaidText.clip(this.font, r.text, RaidText.SANS, lw - (tx - lx) - 14), RaidText.SANS, tx, ty, r.kind == 2 ? 0xFFFFD27A : r.color);
        }
        if (r.right.equals("✔")) {
            RaidFx.badge(g, lx + lw - 4, ry + 6, true);
        } else if (!r.right.isEmpty()) {
            RaidText.right(g, this.font, r.right, RaidText.SANS, lx + lw - 1, ty, 0xFFF3D9A0);
        }
    }

    /** 글자 사이를 조금 띄운다 (부제처럼 작은 글씨를 우아하게). */
    private static String spaced(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (i > 0) b.append(s.charAt(i) == ' ' || s.charAt(i - 1) == ' ' ? "" : " ");
            b.append(s.charAt(i));
        }
        return b.toString();
    }

    private void renderConfirm(GuiGraphics g, int mx, int my, float time) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        g.fill(0, 0, this.width, this.height, 0xB0000000);
        int bw = 280, bh = 110, bx = (this.width - bw) / 2, by = this.height / 2 - 62;
        RaidFx.luxPanel(g, bx, by, bw, bh, time, RaidFx.FIRE);
        RaidFx.embers(g, bx + 4, by + 4, bw - 8, bh - 8, time, 14, false);
        RaidFx.luxTitle(g, this.font, t("confirmTitle"), this.width / 2, by + 10, 0xF3D9A0, time);
        RaidFx.hairline(g, bx + 24, bx + bw - 24, by + 31, 0xD4A85A, 0.9F);
        RaidFx.crest(g, this.width / 2F, by + 31, time);
        int ty = by + 40;
        for (FormattedCharSequence l : RaidText.split(this.font, t("confirmBody"), RaidText.SANS, bw - 70)) {
            g.drawCenteredString(this.font, l, this.width / 2, ty, 0xFFE9DFCB);
            ty += 11;
        }
        int[] yb = yesBtn(), nb = noBtn();
        RaidFx.luxButton(g, this.font, yb[0], yb[1], yb[2], yb[3], t("confirmYes"), true, Ui.inside(mx, my, yb[0], yb[1], yb[2], yb[3]), time, true);
        RaidFx.luxButton(g, this.font, nb[0], nb[1], nb[2], nb[3], t("confirmNo"), true, Ui.inside(mx, my, nb[0], nb[1], nb[2], nb[3]), time, false);
        g.pose().popPose();
    }

    private static String pctMult(double m) {
        return Long.toString(Math.round(m * 100));
    }

    private void renderDialogue(GuiGraphics g) {
        int bw = Math.min(420, this.width - 40), bh = 64, bx = (this.width - bw) / 2, by = this.height - bh - 30;
        float time = (System.currentTimeMillis() - openedAt) / 1000F;
        Fx2D.begin(g, false);
        Fx2D.rect(0, by - 70, this.width, this.height, 0x00000000, 0x00000000, 0xD0000000, 0xD0000000);
        Fx2D.end();
        RaidFx.luxPanel(g, bx, by, bw, bh, time, RaidFx.FIRE);
        RaidFx.embers(g, bx + 4, by + 4, bw - 8, bh - 8, time, 12, false);
        // 이름패
        String name = t("npcName");
        int nw = RaidText.w(this.font, name, RaidText.SERIF) + 28, nx = bx + 14, ny = by - 9;
        g.fill(nx - 2, ny - 2, nx + nw + 2, ny + 15, 0xFF000000);
        RaidFx.box(g, nx - 1, ny - 1, nx + nw + 1, ny + 14, RaidFx.GOLD_MID);
        Fx2D.begin(g, false);
        Fx2D.rect(nx, ny, nx + nw, ny + 13, 0xFF9A2612, 0xFF9A2612, 0xFF420A06, 0xFF420A06);
        Fx2D.diamond(nx + 6, ny + 6.5F, 2F, 2F, 0, RaidFx.GOLD_HI);
        Fx2D.diamond(nx + nw - 6, ny + 6.5F, 2F, 2F, 0, RaidFx.GOLD_HI);
        Fx2D.end();
        RaidText.outlined(g, this.font, name, RaidText.SERIF, nx + 14, ny + 3, 0xFFFFF1D6, 0xC0000000);
        String full = t("dialogue" + (phase + 1));
        int shown = (int) Math.min(full.length(), (System.currentTimeMillis() - lineAt) / 22);
        int ty = by + 14;
        for (FormattedCharSequence l : RaidText.split(this.font, full.substring(0, shown), RaidText.SANS, bw - 32)) {
            g.drawString(this.font, l, bx + 16, ty, 0xFFEDE3CC, false);
            ty += 12;
        }
        if (shown >= full.length()) {
            float bob = (float) Math.sin(time * 5F) * 1.5F;
            Fx2D.begin(g, false);
            Fx2D.tri(bx + bw - 20, by + bh - 14 + bob, RaidFx.GOLD_HI, bx + bw - 12, by + bh - 14 + bob, RaidFx.GOLD_HI, bx + bw - 16, by + bh - 9 + bob, RaidFx.GOLD_MID);
            Fx2D.end();
        }
    }

    private long lineAt = System.currentTimeMillis();

    private void advance() {
        String full = t("dialogue" + (phase + 1));
        if ((System.currentTimeMillis() - lineAt) / 22 < full.length()) {
            lineAt = 0; // 글자가 다 나오지 않았으면 먼저 다 보여 준다
            return;
        }
        phase++;
        lineAt = System.currentTimeMillis();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (phase < 3) {
            advance();
            return true;
        }
        if (confirm) {
            int[] yb = yesBtn(), nb = noBtn();
            if (Ui.inside(mx, my, yb[0], yb[1], yb[2], yb[3])) {
                enter();
            } else if (Ui.inside(mx, my, nb[0], nb[1], nb[2], nb[3])) {
                confirm = false;
            }
            return true;
        }
        int[] e = enterBtn(), c = closeBtn();
        if (data.get("canEnter").getAsBoolean() && Ui.inside(mx, my, e[0], e[1], e[2], e[3])) {
            confirm = true;
            return true;
        }
        if (Ui.inside(mx, my, c[0], c[1], c[2], c[3])) {
            this.onClose();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (phase < 3 && (key == 32 || key == 257 || key == 335)) {
            advance();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) (delta * ROW_H)));
        return true;
    }

    private void enter() {
        ShopNet.CHANNEL.sendToServer(new RaidActionPacket(Raid.ACT_ENTER, ""));
        this.onClose();
    }

    // ------------------------------------------------------------------ 점검용

    public int phaseForTest() {
        return phase;
    }

    public void phaseForTest(int p, boolean confirmOpen) {
        phase = p;
        confirm = confirmOpen;
        lineAt = 0;
    }

    public int rowsForTest() {
        return rows.size();
    }

    public boolean canEnterForTest() {
        return data.get("canEnter").getAsBoolean();
    }

    public void enterForTest() {
        enter();
    }
}

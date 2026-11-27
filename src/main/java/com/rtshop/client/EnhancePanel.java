package com.rtshop.client;

import com.rtshop.enhance.Enhance;
import com.rtshop.enhance.EnhanceConfig;
import com.rtshop.enhance.EnhanceMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 강화 정보 창. 장비에 마우스를 올려도 설명 창은 뜨지 않고, 그 내용이 모두 여기에 들어 있다.
 *  - 강화 보기: 장비 이름·단계, 능력치(지금 → 다음: 보너스 %, 실제 공격 피해/방어력), 확률, 재료, Gold, 보호 상태.
 *  - 상세 보기([상세]): 장비 설명 전체 (예전 설명 창의 글줄 그대로, 마우스 휠로 넘김).
 * 자리·크기는 client.json (게임 안에서는 창 아래 [⚙]). 창을 끄면 [⚙ 강화 정보] 단추만 남는다.
 */
public final class EnhancePanel {
    private EnhancePanel() {
    }

    /** 3.49: 「세로 한 장」 — 모루 블록과 같은 재료(점판암 바탕 · 팔각 강철 테 · 놋쇠 상감). 그림은 tools/gen_enhance_panel.py. */
    public static final int W = 204, H = 280, BTN = 16;
    private static final net.minecraft.resources.ResourceLocation T_PANEL = new net.minecraft.resources.ResourceLocation(com.rtshop.RtShop.MODID, "textures/gui/enhance/panel.png");
    private static final net.minecraft.resources.ResourceLocation T_RUNES = new net.minecraft.resources.ResourceLocation(com.rtshop.RtShop.MODID, "textures/gui/enhance/runes.png");
    private static final net.minecraft.resources.ResourceLocation T_PLATE = new net.minecraft.resources.ResourceLocation(com.rtshop.RtShop.MODID, "textures/gui/enhance/plate.png");
    // 색: 점판암 위의 글자(밝음) · 연한 금빛 판 위의 글자(따뜻한 먹빛) · 놋쇠
    static final int C_ON_SLATE = 0xFFF2EEE2, C_ON_SLATE_SOFT = 0xFFCFCAB8, C_BRASS = 0xFFF0CC7A, C_PLATE_INK = 0xFF2E2412, C_PLATE_SOFT = 0xFF705C3A,
            C_UP = 0xFF1E7C46, C_OK_SLATE = 0xFF8CE89A, C_BAD_SLATE = 0xFFFF9A8A, C_BAD_PLATE = 0xFFA8382A;
    /** 3.53: 이 창의 글꼴 — 한글 송명, 숫자·영문 Playfair Display (assets/rtshop/font/enhance.json). client.json 의 enhanceLuxFont=false 면 게임 글꼴. */
    private static final net.minecraft.resources.ResourceLocation FONT_ID = new net.minecraft.resources.ResourceLocation(com.rtshop.RtShop.MODID, "enhance");
    /** 금빛 판 위 글자의 새김 빛 (글자 바로 아래의 밝은 선). */
    private static final int C_CARVE = 0xD0FFFCEC;
    /**
     * 3.64: 사용자 요청 "강화 정보 창의 글자에서 그림자를 모두 제거" — 이 창의 글자는 그림자 없이 그린다 (drawString 의 마지막 값은 모두 false),
     * 금빛 판 위 글자 밑에 깔던 밝은 새김 선(글자가 겹쳐 보이는 원인)도 끈다. 새김 선만 되살리려면 true.
     */
    private static final boolean CARVE = false;

    static boolean lux() {
        return ClientConfig.get().enhanceLuxFont;
    }

    /** 금빛 판 위의 글자: 아래에 밝은 선을 한 번 깔아 새긴 것처럼 보이게 한다. */
    private static void carved(GuiGraphics g, Font font, String t, int x, int y, int col) {
        if (CARVE && lux()) {
            g.pose().pushPose();
            g.pose().translate(0, 0.5F, 0);
            g.drawString(font, t, x, y, C_CARVE, false);
            g.pose().popPose();
        }
        g.drawString(font, t, x, y, col, false);
    }

    /** 칸의 너비(maxW)를 넘으면 줄여서 그린다 (칸 밖으로 나가지 않게). anchor: 0 왼쪽, 1 가운데, 2 오른쪽 기준. carve: 새김 빛. */
    static void fit(GuiGraphics g, Font font, String t, float x, float y, int maxW, float scale, int col, int anchor, boolean shadow, boolean carve) {
        float w = font.width(t) * scale;
        float sc = w > maxW && w > 0 ? scale * maxW / w : scale;
        if (sc < scale * 0.6F) {                       // 너무 작아지면 줄임표로 자른다
            sc = scale * 0.6F;
            t = Ui.clip(font, t, (int) (maxW / sc));
        }
        float tw = font.width(t) * sc;
        float ox = anchor == 1 ? x - tw / 2F : anchor == 2 ? x - tw : x;
        g.pose().pushPose();
        g.pose().translate(ox, y + (scale - sc) * 4F, 0);   // 줄어든 만큼 아래로 내려 줄을 맞춘다
        g.pose().scale(sc, sc, 1);
        if (CARVE && carve && lux()) {
            g.pose().pushPose();
            g.pose().translate(0, 0.5F / sc, 0);
            g.drawString(font, t, 0, 0, C_CARVE, false);
            g.pose().popPose();
        }
        g.drawString(font, t, 0, 0, col, false);   // 3.64: 그림자 없음 (shadow 값은 쓰지 않는다)
        g.pose().popPose();
    }

    /** 제목: 금박(위는 밝은 금, 아래는 짙은 금) + 양옆의 가는 장식 선. */
    private static void title(GuiGraphics g, Font font, String t, int cx, int y) {
        if (!lux()) {
            slateCentered(g, font, t, cx, y, C_BRASS);
            return;
        }
        float sc = 1.2F;
        int w = Math.round(font.width(t) * sc), x0 = cx - w / 2;
        g.pose().pushPose();
        g.pose().translate(x0, y - 1, 0);
        g.pose().scale(sc, sc, 1);
        g.drawString(font, t, 0, 0, 0xFFFFE9A8, false);
        g.pose().popPose();
        // 아래 절반은 짙은 금으로 덧그린다 (가위질)
        int mid = y + 4;
        org.joml.Vector4f p0 = g.pose().last().pose().transform(new org.joml.Vector4f(x0 - 2, mid, 0, 1)), p1 = g.pose().last().pose().transform(new org.joml.Vector4f(x0 + w + 2, y + 14, 0, 1));
        g.enableScissor((int) p0.x(), (int) p0.y(), (int) Math.ceil(p1.x()), (int) Math.ceil(p1.y()));
        g.pose().pushPose();
        g.pose().translate(x0, y - 1, 0);
        g.pose().scale(sc, sc, 1);
        g.drawString(font, t, 0, 0, 0xFFD9A94E, false);
        g.pose().popPose();
        g.disableScissor();
        for (int s = -1; s <= 1; s += 2) {
            int a = cx + s * (w / 2 + 5), b = cx + s * (w / 2 + 24);
            g.fill(Math.min(a, b), y + 5, Math.max(a, b), y + 6, 0xC0D6AA54);
            g.fill(a - 1, y + 4, a + 1, y + 7, 0xFFF0CC7A);
        }
    }
    /** 마지막으로 그릴 때의 마우스 자리 (창 안의 좌표; 재료 칸에 올리면 이름을 보여 준다). */
    private static double hoverX = -1, hoverY = -1;
    /** 점검용: 마지막으로 그린 재료 칸의 수(Gold 칸 포함), 켜진 룬의 수. */
    public static int lastSlotsForTest, lastRunesForTest;
    /** 점검용: 룬을 그릴 때 섞기가 켜져 있었는가 (3.78). */
    public static boolean lastRuneBlendForTest;

    /** 화면에 다 들어오게 줄인 배율. */
    public static double scale(Screen s) {
        double sc = ClientConfig.get().enhPanelScale;
        return Math.max(0.5, Math.min(sc, Math.min((s.height - BTN - 9.0) / H, (s.width - 8.0) / W)));
    }
    /** 단추 종류. */
    public static final int B_ODDS = 0, B_SETTINGS = 1, B_EDIT = 2, B_SHOW = 3, B_DETAIL = 4;

    private static boolean detail;
    private static int detailScroll;
    private static ItemStack cachedFor = ItemStack.EMPTY;
    private static long cachedAt;
    private static List<FormattedCharSequence> cachedLines = List.of();

    public static boolean detailForTest() {
        return detail;
    }

    public static void setDetailForTest(boolean on) {
        detail = on;
        detailScroll = 0;
    }

    public static int detailLinesForTest() {
        return cachedLines.size();
    }

    /** {x, y, w(화면), h(화면), 배율×100}. auto = 강화 창이면 창 오른쪽, 아니면 화면 오른쪽 가운데. */
    public static int[] rect(Screen s, int autoX, int autoY) {
        ClientConfig c = ClientConfig.get();
        double sc = scale(s);
        int w = (int) Math.round(W * sc), h = (int) Math.round(H * sc);
        int x, y;
        if (c.enhPanelX < 0 || c.enhPanelY < 0) {
            x = autoX;
            y = autoY;
            if (x + w > s.width - 2) x = s.width - w - 4;
        } else {
            x = (int) Math.round(c.enhPanelX * s.width);
            y = (int) Math.round(c.enhPanelY * s.height);
        }
        x = Math.max(2, Math.min(s.width - w - 2, x));
        y = Math.max(2, Math.min(s.height - h - BTN - 5, y));
        return new int[]{x, y, w, h, (int) Math.round(sc * 100)};
    }

    /** 단추들: {x, y, w, 종류}. [확률표] [상세] (OP: [편집]) [⚙]. */
    public static List<int[]> buttons(int[] r) {
        List<int[]> out = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        int by = r[1] + r[3] + 3;
        if (!ClientConfig.get().enhPanelEnabled) {
            int w = Math.min(r[2], mc.font.width(label(B_SHOW, false)) + 12);
            out.add(new int[]{r[0] + r[2] - w, r[1], w, B_SHOW});
            return out;
        }
        boolean op = mc.player != null && com.rtshop.util.Perm.op(mc.player);
        int gear = 18, gap = 2;
        int n = op ? 3 : 2;
        int rest = r[2] - gear - gap * n;
        int bw = rest / n;
        int x = r[0];
        out.add(new int[]{x, by, bw, B_ODDS});
        x += bw + gap;
        out.add(new int[]{x, by, bw, B_DETAIL});
        x += bw + gap;
        if (op) {
            out.add(new int[]{x, by, r[0] + r[2] - gear - gap - x, B_EDIT});
        }
        out.add(new int[]{r[0] + r[2] - gear, by, gear, B_SETTINGS});
        return out;
    }

    static String label(int kind, boolean tableOpen) {
        return switch (kind) {
            case B_ODDS -> Component.translatable(tableOpen ? "rtshop.enhodds.hide" : "rtshop.enhodds.open").getString();
            case B_SETTINGS -> "⚙";
            case B_EDIT -> Component.translatable("rtshop.enhodds.edit_short").getString();
            case B_DETAIL -> Component.translatable(detail ? "rtshop.enhui.detail_back" : "rtshop.enhui.detail").getString();
            default -> Component.translatable("rtshop.enhui.show_again").getString();
        };
    }

    public static void render(GuiGraphics g, Font font, EnhanceMenu menu, Screen s, int[] r, int mx, int my, boolean tableOpen) {
        Minecraft mc = Minecraft.getInstance();
        if (ClientConfig.get().enhPanelEnabled) {
            float sc = r[4] / 100F;
            hoverX = (mx - r[0]) / sc;
            hoverY = (my - r[1]) / sc;
            g.pose().pushPose();
            g.pose().translate(r[0], r[1], 0);
            g.pose().scale(sc, sc, 1);
            draw(g, font, menu, mc.player, 0, 0, W, H);
            g.pose().popPose();
            hoverX = hoverY = -1;
        }
        for (int[] b : buttons(r)) {
            Ui.button(g, font, b[0], b[1], b[2], BTN, Ui.clip(font, label(b[3], tableOpen), b[2] - 4),
                    Ui.inside(mx, my, b[0], b[1], b[2], BTN), true);
        }
    }

    /** 누른 단추 종류 (-1 = 없음). */
    public static int clicked(int[] r, double mx, double my) {
        for (int[] b : buttons(r)) {
            if (Ui.inside(mx, my, b[0], b[1], b[2], BTN)) return b[3];
        }
        return -1;
    }

    /** 단추 동작 (확률표는 부르는 쪽에서). */
    public static void act(int kind, Screen from) {
        Minecraft mc = Minecraft.getInstance();
        if (kind == B_SETTINGS) {
            mc.setScreen(new EnhanceUiScreen(from));
        } else if (kind == B_EDIT) {
            mc.player.closeContainer();
            com.rtshop.net.ShopNet.CHANNEL.sendToServer(new com.rtshop.net.EnhanceActionPacket(com.rtshop.net.EnhanceActionPacket.OPEN_EDITOR, ""));
        } else if (kind == B_SHOW) {
            ClientConfig.get().enhPanelEnabled = true;
            ClientConfig.get().save();
        } else if (kind == B_DETAIL) {
            detail = !detail;
            detailScroll = 0;
        }
    }

    /** 마우스 휠: 상세 보기에서 글을 넘긴다. 창 위에서 굴렸으면 true. */
    public static boolean scroll(int[] r, double mx, double my, double delta) {
        if (!ClientConfig.get().enhPanelEnabled || !detail || !Ui.inside(mx, my, r[0], r[1], r[2], r[3])) return false;
        detailScroll -= (int) Math.signum(delta) * 2;
        return true;
    }

    // ------------------------------------------------------------------ 그리기

    public static void draw(GuiGraphics g, Font font, EnhanceMenu menu, Player player, int x, int y, int w, int h) {
        blend();
        g.blit(T_PANEL, x, y, W, H, 0, 0, W * 2, H * 2, W * 2, H * 2);
        ItemStack eq = menu.equipment();
        Font lf = lux() ? UiFont.of(FONT_ID) : font;
        if (detail) {
            drawDetail(g, lf, font, eq, x, y, w, h);
        } else {
            drawMain(g, lf, menu, player, eq, x, y, w, h);
        }
    }

    /** 점판암 위의 글자: 밝은 색 (3.64: 그림자 없음). */
    private static void slate(GuiGraphics g, Font font, String t, int x, int y, int col) {
        g.drawString(font, t, x, y, col, false);
    }

    private static void slateCentered(GuiGraphics g, Font font, String t, int cx, int y, int col) {
        g.drawString(font, t, cx - font.width(t) / 2, y, col, false);
    }

    /**
     * 반투명 그림을 그리기 전에 섞기(blend)를 켠다. 3.78 (사용자 "강화 정보 창의 룬이 채워질 때 글자 테두리를 벗어나는 문제"):
     * 그 앞에 그린 장비 아이콘 · 글자가 섞기를 꺼 두어, 켜진 룬의 옅은 빛무리가 불투명하게 찍혀 홈 밖으로 번진 굵은 덩어리(붉은 · 노란 테)로 보였다.
     */
    static void blend() {
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
    }

    /** 닦은 강철 판 (가운데가 늘어나는 9칸 그림; 그림은 화면 48칸 = 96픽셀, 모서리 6칸). */
    static void plate(GuiGraphics g, int x, int y, int w, int h) {
        blend();
        int c = 6, t = 96, cc = 12, mid = 96 - 24;
        int[][] cols = {{x, c, 0, cc}, {x + c, w - 2 * c, cc, mid}, {x + w - c, c, t - cc, cc}};
        int[][] rows = {{y, c, 0, cc}, {y + c, h - 2 * c, cc, mid}, {y + h - c, c, t - cc, cc}};
        for (int[] ry : rows) {
            for (int[] cx : cols) {
                if (cx[1] > 0 && ry[1] > 0) g.blit(T_PLATE, cx[0], ry[0], cx[1], ry[1], cx[2], ry[2], cx[3], ry[3], t, t);
            }
        }
    }

    private static void help(GuiGraphics g, Font font, int tx, int ty, int tw, int bottom, String... keys) {
        for (String k : keys) {
            for (FormattedCharSequence line : font.split(Component.translatable(k), tw)) {
                if (ty > bottom) return;
                g.drawString(font, line, tx, ty, C_ON_SLATE_SOFT, false);
                ty += 10;
            }
            ty += 3;
        }
    }

    /** 글자를 가운데에 두고 키워 그린다. */
    private static void big(GuiGraphics g, Font font, String t, float cx, float y, float sc, int col, boolean shadow) {
        g.pose().pushPose();
        g.pose().translate(cx - font.width(t) * sc / 2F, y, 0);
        g.pose().scale(sc, sc, 1);
        g.drawString(font, t, 0, 0, col, false);   // 3.64: 그림자 없음 (shadow 값은 쓰지 않는다)
        g.pose().popPose();
    }

    private static boolean over(int x, int y, int w, int h) {
        return hoverX >= x && hoverX < x + w && hoverY >= y && hoverY < y + h;
    }

    /** 점검용: 마지막으로 그린 실패 누적 글자 ("" = 없음). */
    public static String lastFailBadgeForTest = "";

    private static void drawMain(GuiGraphics g, Font font, EnhanceMenu menu, Player player, ItemStack eq, int x, int y, int w, int h) {
        int cx = x + w / 2;
        EnhanceConfig cfg = EnhanceConfig.client();
        title(g, font, Component.translatable("rtshop.enhance.panel").getString(), cx, y + 15);
        lastSlotsForTest = 0;
        lastRunesForTest = 0;
        lastFailBadgeForTest = "";
        if (eq.isEmpty() || !Enhance.enhanceable(eq)) {
            help(g, font, x + 16, y + 132, w - 32, y + h - 24, "rtshop.enhance.help1", "rtshop.enhance.help2", "rtshop.enhance.help3", "rtshop.enhance.help4");
            return;
        }
        int cur = Enhance.level(eq);
        EnhanceConfig.Level l = cfg.level(cur + 1);
        // 팔각 메달 속의 장비 (두 배 크기)
        g.pose().pushPose();
        g.pose().translate(cx - 16, y + 42, 0);
        g.pose().scale(2F, 2F, 1F);
        g.renderItem(eq, 0, 0);
        g.pose().popPose();
        fit(g, font, eq.getHoverName().getString(), cx, y + 91, w - 30, 1F, C_ON_SLATE, 1, true, false);
        // 룬 띠: 강화 단계만큼 불이 들어온다 (다음 단계의 룬이 가장 밝다). 빛은 띠에 새긴 홈 안에만 (3.78: 섞기를 켜고, 홈과 같은 굵기의 그림)
        blend();
        lastRuneBlendForTest = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND);
        float step = (W - 28 - 16) / (float) (EnhanceConfig.MAX_LEVEL - 1);
        for (int i = 0; i < EnhanceConfig.MAX_LEVEL && i < 11; i++) {
            boolean lit = i < cur, next = i == cur && l != null;
            if (!lit && !next) continue;
            int rx = Math.round(x + 14 + 8 + i * step) - 8;
            g.blit(T_RUNES, rx, y + 108, 16, 15, i * 32, next ? 30 : 0, 32, 30, 352, 60);
            if (lit) lastRunesForTest++;
        }
        // +지금 ▶ +다음
        if (l == null) {
            big(g, font, "+" + cur + " MAX", cx, y + 128, 1.25F, C_BRASS, false);
        } else {
            String a = "+" + cur, b = "+" + (cur + 1);
            g.drawString(font, a, cx - 12 - font.width(a), y + 130, C_ON_SLATE_SOFT, false);
            g.drawString(font, "▶", cx - 4, y + 130, C_BRASS, false);
            g.pose().pushPose();
            g.pose().translate(cx + 10, y + 127, 0);
            g.pose().scale(1.4F, 1.4F, 1);
            g.drawString(font, b, 0, 0, C_BRASS, false);
            g.pose().popPose();
            // 3.66.0 실패 누적: 오른쪽 끝에 "누적 3/5" (성공률은 아래 확률에 이미 반영)
            int fails = Math.min(Enhance.failStacks(eq), cfg.failStackMax);
            if (fails > 0 && cfg.failBonusPct > 0) {
                String fs = Component.translatable("rtshop.enhui.fail_stacks", fails, cfg.failStackMax).getString();
                fit(g, font, fs, x + w - 16, y + 131, w / 3, 0.85F, C_UP, 2, false, false);   // 오른쪽 끝에 맞춤
                lastFailBadgeForTest = fs;
            }
        }
        // 능력치: 지금 → 다음 (강철 판)
        List<Stat> stats = stats(eq, cur, l != null, cfg);
        int px = x + 14, pw = w - 28, py = y + 146, ph = 8 + stats.size() * 11;
        plate(g, px, py, pw, ph);
        int ty = py + 5;
        for (Stat st : stats) {
            // 값이 너무 길면 값부터 줄이고, 남은 자리에 이름을 맞춘다 (판 밖으로 나가지 않게)
            int vw = font.width(st.now + st.next);
            float vs = vw > pw * 0.62F ? pw * 0.62F / vw : 1F;
            int vwS = Math.round(vw * vs);
            fit(g, font, st.label, px + 6, ty, pw - 16 - vwS, 1F, C_PLATE_SOFT, 0, false, true);
            float vx = px + pw - 6 - vwS;
            fit(g, font, st.now, vx, ty, pw, vs, C_PLATE_INK, 0, false, true);
            if (!st.next.isEmpty()) fit(g, font, st.next, vx + font.width(st.now) * vs, ty, pw, vs, C_UP, 0, false, true);
            ty += 11;
        }
        int oy = py + ph + 4;
        if (l == null) {
            help(g, font, x + 16, oy + 4, w - 32, y + h - 14, "rtshop.enhance.max_note");
            return;
        }
        // 확률: 성공은 크게, 나머지 셋은 작게 (실패 누적이 있으면 성공이 올라 있다: Enhance.levelFor)
        Enhance.Guard guard = Enhance.guardFor(player, menu.guardStack());
        int mask = Enhance.maskOf(guard);
        boolean safe = Enhance.unbreakable(cfg, eq);
        l = Enhance.levelFor(cfg, l, eq);
        double[] e = EnhanceClient.effective(l, guard, cur);
        String[] names = {"rtshop.enhui.odds_success", "rtshop.enhui.odds_maintain", "rtshop.enhui.odds_damage", "rtshop.enhui.odds_destroy"};
        int[] cols = {0xFF1E7C46, 0xFF6A5E48, 0xFFC46E12, C_BAD_PLATE};
        int oh = 38, bigW = 58, smW = (pw - bigW - 9) / 3;
        plate(g, px, oy, bigW, oh);
        fit(g, font, EnhanceClient.pct(e[0]), px + bigW / 2F, oy + 7, bigW - 8, 1.6F, cols[0], 1, false, true);
        fit(g, font, Component.translatable(names[0]).getString(), px + bigW / 2F, oy + 25, bigW - 6, 1F, C_PLATE_SOFT, 1, false, false);
        for (int i = 1; i < 4; i++) {
            int bx = px + bigW + 3 + (i - 1) * (smW + 3);
            plate(g, bx, oy, smW, oh);
            boolean on = e[i] > 0;
            g.fill(bx + smW / 2 - 2, oy + 5, bx + smW / 2 + 2, oy + 9, on ? cols[i] : 0xFFCDBE96);
            fit(g, font, EnhanceClient.pct(e[i]), bx + smW / 2F, oy + 13, smW - 6, 1F, on ? cols[i] : 0xFFA89870, 1, false, on);
            fit(g, font, Component.translatable(names[i]).getString(), bx + smW / 2F, oy + 25, smW - 6, 1F, C_PLATE_SOFT, 1, false, false);
        }
        // 재료 칸 + Gold 칸 + 보호권 칸
        int my0 = oy + oh + 5, size = 26, gap = 29;
        boolean free = player.getAbilities().instabuild;
        long tick = System.currentTimeMillis() / 1000;
        List<Enhance.Need> needs = Enhance.needs(cfg, l, player);
        int shown = Math.min(3, needs.size());
        Component tip = null;
        for (int i = 0; i <= shown; i++) {
            int bx = px + i * gap;
            plate(g, bx, my0, size, size);
            String cnt;
            boolean ok;
            Component name;
            if (i < shown) {
                Enhance.Need n = needs.get(i);
                g.renderItem(new ItemStack(n.items.get((int) (tick % n.items.size()))), bx + 5, my0 + 3);
                cnt = n.have + "/" + n.need;
                ok = n.ok() || free;
                name = EnhanceClient.materialName(cfg, n);
            } else {
                Ui.coin(g, bx + 7, my0 + 4, 12);
                cnt = Ui.compact(l.gold);
                ok = free || ClientEconomy.wallet >= l.gold;
                name = Component.literal(Ui.num(l.gold) + " G  (" + Ui.num(ClientEconomy.wallet) + ")");
            }
            g.pose().pushPose();
            g.pose().translate(0, 0, 200);
            fit(g, font, cnt, bx + size / 2F, my0 + size - 9, size - 5, 0.75F, ok ? C_UP : C_BAD_PLATE, 1, false, false);   // 칸 안 가운데 아래
            g.pose().popPose();
            if (over(bx, my0, size, size)) tip = name.copy().append(Component.literal("  " + cnt).withStyle(ok ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED));
            lastSlotsForTest++;
        }
        int gx = px + (shown + 1) * gap, gw = px + pw - gx;
        plate(g, gx, my0, gw, size);
        boolean guarded = guard != Enhance.Guard.NONE;
        String g1 = Component.translatable("rtshop.enhui.guard").getString();
        String g2 = guarded ? Enhance.guardName(mask).getString() : Component.translatable(l.destroy > 0 ? "rtshop.enhui.guard_risk" : safe ? "rtshop.enhui.guard_safe" : "rtshop.enhui.guard_none").getString();
        fit(g, font, g1, gx + 5, my0 + 4, gw - 10, 1F, C_PLATE_INK, 0, false, true);
        fit(g, font, g2, gx + 5, my0 + 14, gw - 10, 1F, guarded ? 0xFF1F6F8B : l.destroy > 0 ? C_BAD_PLATE : C_PLATE_SOFT, 0, false, false);
        if (over(gx, my0, gw, size)) {
            tip = guarded ? Component.translatable("rtshop.enhance.guard_active", Enhance.guardName(mask))
                    : Component.translatable(l.destroy > 0 ? "rtshop.enhance.destroy_warn" : safe ? "rtshop.enhance.safe_note" : "rtshop.enhance.no_guard");
        }
        // 맨 아래 한마디: 누르면 강화 / 무엇이 모자란지
        boolean ok = menu.getSlot(2).hasItem() && menu.getSlot(2).mayPickup(player);
        List<FormattedCharSequence> hl = font.split(Component.translatable(ok ? "rtshop.enhance.click_hint" : "rtshop.enhance.missing"), w - 30);
        int hy = y + h - 12 - Math.min(2, hl.size()) * 10;
        for (int i = 0; i < hl.size() && i < 2; i++) {
            g.drawString(font, hl.get(i), cx - font.width(hl.get(i)) / 2, hy + i * 10, ok ? C_OK_SLATE : C_BAD_SLATE, false);
        }
        if (tip != null) {
            // 재료·보호권 칸에 마우스를 올리면 그 이름을 창 안에 띠로 보여 준다 (설명 창은 쓰지 않는다)
            List<FormattedCharSequence> tl = font.split(tip, w - 36);
            int th = tl.size() * 10 + 6, ty0 = my0 - th - 2;
            g.pose().pushPose();
            g.pose().translate(0, 0, 300);
            g.fill(x + 14, ty0, x + w - 14, ty0 + th, 0xF0101420);
            g.fill(x + 14, ty0, x + w - 14, ty0 + 1, 0xFFC8A050);
            for (int i = 0; i < tl.size(); i++) g.drawString(font, tl.get(i), x + 18, ty0 + 4 + i * 10, 0xFFFFFFFF, false);
            g.pose().popPose();
        }
    }

    private static void drawDetail(GuiGraphics g, Font lf, Font font, ItemStack eq, int x, int y, int w, int h) {
        int tx = x + 10, ty = y + 10, tw = w - 20;
        ty = y + 124;
        tx = x + 16;
        tw = w - 32;
        if (!eq.isEmpty()) {
            g.pose().pushPose();
            g.pose().translate(x + w / 2F - 16, y + 42, 0);
            g.pose().scale(2F, 2F, 1F);
            g.renderItem(eq, 0, 0);
            g.pose().popPose();
            fit(g, lf, eq.getHoverName().getString(), x + w / 2F, y + 91, w - 30, 1F, C_ON_SLATE, 1, true, false);
        }
        title(g, lf, Component.translatable("rtshop.enhance.panel").getString(), x + w / 2, y + 15);
        g.drawString(lf, Component.translatable("rtshop.enhui.detail_title"), tx, ty, C_ON_SLATE, false);
        int bx0 = x + 14, by0 = y + 136, bx1 = x + w - 14, by1 = y + h - 14;
        // 설명 창과 같은 어두운 바탕 (글자 색이 어두운 바탕용이라서)
        g.fill(bx0, by0, bx1, by1, 0xF0100010);
        g.fill(bx0, by0, bx1, by0 + 1, 0xFF5000FF);
        g.fill(bx0, by1 - 1, bx1, by1, 0xFF28007F);
        g.fill(bx0, by0, bx0 + 1, by1, 0xFF5000FF);
        g.fill(bx1 - 1, by0, bx1, by1, 0xFF28007F);
        if (eq.isEmpty()) {
            cachedLines = List.of();
            int yy = by0 + 6;
            for (FormattedCharSequence line : font.split(Component.translatable("rtshop.enhui.detail_empty"), tw - 10)) {
                g.drawString(font, line, bx0 + 5, yy, 0xFFB0B0B0, false);
                yy += 10;
            }
            return;
        }
        long now = System.currentTimeMillis();
        if (now - cachedAt > 700 || !ItemStack.matches(cachedFor, eq)) {
            List<FormattedCharSequence> lines = new ArrayList<>();
            for (Component c : EnhanceClient.describe(eq)) {
                List<FormattedCharSequence> part = font.split(c, tw - 12);
                if (part.isEmpty()) lines.add(FormattedCharSequence.EMPTY);
                else lines.addAll(part);
            }
            cachedLines = lines;
            cachedFor = eq.copy();
            cachedAt = now;
        }
        int rows = (by1 - by0 - 8) / 10;
        int max = Math.max(0, cachedLines.size() - rows);
        detailScroll = Math.max(0, Math.min(max, detailScroll));
        int yy = by0 + 5;
        for (int i = detailScroll; i < cachedLines.size() && i < detailScroll + rows; i++) {
            g.drawString(font, cachedLines.get(i), bx0 + 5, yy, 0xFFFFFFFF, false);
            yy += 10;
        }
        if (max > 0) {
            int trackH = by1 - by0 - 4;
            int barH = Math.max(10, trackH * rows / cachedLines.size());
            int barY = by0 + 2 + (trackH - barH) * detailScroll / max;
            g.fill(bx1 - 3, barY, bx1 - 1, barY + barH, 0xFFB0A0FF);
            String more = Component.translatable("rtshop.enhui.detail_scroll").getString();
            g.drawString(font, more, x + w - 16 - font.width(more), ty, C_ON_SLATE_SOFT, false);
        }
    }

    // ------------------------------------------------------------------ 능력치 (지금 → 다음)

    /** label: 이름, now: 지금 값, next: " → 다음 값" (최고 단계면 빈 글). */
    public record Stat(String label, String now, String next) {
    }

    private static double total(ItemStack s, EquipmentSlot slot, Attribute a, double base) {
        double v = base;
        for (AttributeModifier m : s.getAttributeModifiers(slot).get(a)) {
            if (m.getOperation() == AttributeModifier.Operation.ADDITION) v += m.getAmount();
        }
        return v;
    }

    private static String num(double v) {
        double r = Math.round(v * 10) / 10.0;
        return r == Math.floor(r) ? Long.toString((long) r) : String.format(java.util.Locale.ROOT, "%.1f", r);
    }

    private static String arrow(String next) {
        return " → " + next;
    }

    /** 강화로 달라지는 수치: 보너스 %, 그리고 그 장비의 실제 공격 피해 / 방어력 / 방어 강도. */
    private static double reduce(EnhanceConfig cfg, int lv) {
        return lv <= 0 || cfg.armorReduce.length == 0 ? 0 : cfg.armorReduce[Math.min(lv, cfg.armorReduce.length - 1)];
    }

    public static List<Stat> stats(ItemStack eq, int cur, boolean hasNext, EnhanceConfig cfg) {
        List<Stat> out = new ArrayList<>();
        ItemStack a = eq.copy(), b = eq.copy();
        Enhance.clearPreview(a);
        Enhance.clearPreview(b);
        Enhance.setLevel(a, cur);
        Enhance.setLevel(b, cur + 1);
        Enhance.Kind kind = Enhance.kind(eq);
        boolean armor = kind == Enhance.Kind.ARMOR;
        String pNow = "+" + EnhanceClient.pct(Enhance.bonusPct(cfg, kind, cur));
        String pNext = hasNext ? arrow("+" + EnhanceClient.pct(Enhance.bonusPct(cfg, kind, cur + 1))) : "";
        String label = switch (kind) {
            case ARMOR -> "rtshop.enhui.stat_armor_pct";
            case RANGED -> "rtshop.enhui.stat_ranged_pct";
            case MAGIC -> "rtshop.enhui.stat_magic_pct";
            default -> "rtshop.enhui.stat_attack_pct";
        };
        double rNow = armor ? reduce(cfg, cur) : 0, rNext = armor && hasNext ? reduce(cfg, cur + 1) : 0;
        if (rNow > 0 || rNext > 0) {
            // 3.66.0: +8 부터 방어구는 받는 피해가 줄어든다 — 방어력 % 줄 대신 보여 준다 (방어력은 아래 줄에 그대로, 줄 수는 같게)
            out.add(new Stat(Component.translatable("rtshop.enhui.stat_reduce").getString(), "-" + EnhanceClient.pct(rNow),
                    hasNext ? arrow("-" + EnhanceClient.pct(rNext)) : ""));
        } else {
            out.add(new Stat(Component.translatable(label).getString(), pNow, pNext));
        }
        if (kind == Enhance.Kind.RANGED || kind == Enhance.Kind.MAGIC) {
            return out;   // 원거리 · 마법 무기는 속성이 아니라 쏜 것 / 주문의 피해가 오른다: 보너스 % 만
        }
        try {
            if (armor) {
                EquipmentSlot slot = LivingEntity.getEquipmentSlotForItem(eq);
                double n0 = total(a, slot, Attributes.ARMOR, 0), n1 = total(b, slot, Attributes.ARMOR, 0);
                out.add(new Stat(Component.translatable("rtshop.enhui.stat_armor").getString(), num(n0), hasNext ? arrow(num(n1)) : ""));
                double t0 = total(a, slot, Attributes.ARMOR_TOUGHNESS, 0), t1 = total(b, slot, Attributes.ARMOR_TOUGHNESS, 0);
                if (t0 > 0 || (hasNext && t1 > 0)) {
                    out.add(new Stat(Component.translatable("rtshop.enhui.stat_toughness").getString(), num(t0), hasNext ? arrow(num(t1)) : ""));
                }
            } else {
                double d0 = total(a, EquipmentSlot.MAINHAND, Attributes.ATTACK_DAMAGE, 1), d1 = total(b, EquipmentSlot.MAINHAND, Attributes.ATTACK_DAMAGE, 1);
                out.add(new Stat(Component.translatable("rtshop.enhui.stat_attack").getString(), num(d0), hasNext ? arrow(num(d1)) : ""));
            }
        } catch (Exception ignored) {
            // 다른 모드의 특이한 장비: 보너스 % 만 보여 준다
        }
        return out;
    }
}

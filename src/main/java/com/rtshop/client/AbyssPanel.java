package com.rtshop.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rtshop.net.RaidActionPacket;
import com.rtshop.net.ShopNet;
import com.rtshop.raid.AbyssWeekly;
import com.rtshop.raid.Reward;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

/**
 * Spiral Abyss 층 선택 화면의 왼쪽 아래에 그리는 작은 "주간 보상" 카드: 여섯 층의 별(받음 = 금빛)과 다음 보상 Gold,
 * [자세히 보기] 를 누르면 층마다 보상을 크게 보여 주는 AbyssRewardScreen 이 열린다.
 * 심연(남보라 밤하늘)에 어울리는 금박 테두리·명조 제목.
 */
public final class AbyssPanel {
    private AbyssPanel() {
    }

    public static final int W = 132, H = 62;
    static final String[] ROMAN = {"I", "II", "III", "IV", "V", "VI"};
    static final int STAR = 0xF3D9A0, VIOLET = 0x9C7BF0, DEEP = 0x5A3FC0;

    public static boolean isFloorScreen(Screen s) {
        return s != null && s.getClass().getName().equals("com.pla.spiral_abyss.client.screen.FloorSelectionScreen");
    }

    public static void request() {
        ShopNet.CHANNEL.sendToServer(new RaidActionPacket(AbyssWeekly.ACT_VIEW, ""));
    }

    public static int height() {
        return H;
    }

    /** 단계 이름: 무한 모드면 웨이브 수(10, 20, …), 아니면 층의 로마 숫자. */
    static String label(JsonObject o, int f) {
        boolean endless = o.has("endless") && o.get("endless").getAsBoolean();
        return endless ? Integer.toString(f * (o.has("step") ? o.get("step").getAsInt() : 10)) : ROMAN[f - 1];
    }

    static String t(JsonObject o, String k) {
        JsonObject t = o.getAsJsonObject("texts");
        return t.has(k) ? t.get(k).getAsString() : k;
    }

    static String reset(JsonObject o) {
        long ms = o.get("resetIn").getAsLong();
        try {
            return String.format(t(o, "reset"), ms / 86_400_000L, (ms / 3_600_000L) % 24);
        } catch (Exception e) {
            return "";
        }
    }

    static boolean done(JsonObject o, int f) {
        return (o.get("mask").getAsLong() & (1L << (f - 1))) != 0;
    }

    static long gold(JsonObject o, int f) {
        JsonArray gold = o.getAsJsonArray("gold");
        return f - 1 < gold.size() ? gold.get(f - 1).getAsLong() : 0;
    }

    static List<Reward> rewards(JsonObject o) {
        List<Reward> all = new ArrayList<>();
        for (JsonElement e : o.getAsJsonArray("rewards")) all.add(Reward.parse(e.getAsJsonObject()));
        return all;
    }

    private static int[] box(int sw, int sh) {
        return new int[]{8, sh - H - 8, W, H};
    }

    /** [자세히 보기] 단추 자리. */
    static int[] detailBtn(int sw, int sh) {
        int[] b = box(sw, sh);
        return new int[]{b[0] + 8, b[1] + H - 17, W - 16, 12};
    }

    /** 심연의 판: 남보라 밤하늘 + 잔별 + 금박 이중 테두리 + 모서리 별. */
    static void frame(GuiGraphics g, int x, int y, int w, int h, float time) {
        for (int i = 4; i >= 1; i--) g.fill(x - 2 - i, y - 2 - i, x + w + 2 + i, y + h + 2 + i, (0x14 + (4 - i) * 0x08) << 24);
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xFF05040C);
        Fx2D.begin(g, false);
        Fx2D.rect(x, y, x + w, y + h, 0xF0120E26, 0xF0120E26, 0xF0201846, 0xF0201846);
        Fx2D.end();
        Fx2D.begin(g, true);
        Fx2D.glow(x + w * 0.5F, y + h + 6, w * 0.65F, h * 0.6F, Fx2D.rgba(DEEP, 0.35F), Fx2D.rgba(DEEP, 0));
        for (int i = 0; i < Math.max(6, w * h / 900); i++) {
            float sx = x + 4 + Fx2D.hash(i, 21) * (w - 8), sy = y + 4 + Fx2D.hash(i, 23) * (h - 8);
            float tw = 0.25F + 0.75F * (0.5F + 0.5F * (float) Math.sin(time * (1.2F + Fx2D.hash(i, 25)) + i));
            Fx2D.glow(sx, sy, 0.9F + Fx2D.hash(i, 27) * 1.3F, Fx2D.rgba(0xE8E0FF, 0.55F * tw), Fx2D.rgba(0xE8E0FF, 0));
        }
        Fx2D.end();
        RaidFx.box(g, x - 2, y - 2, x + w + 2, y + h + 2, RaidFx.GOLD_LO);
        RaidFx.box(g, x, y, x + w, y + h, 0xC0D4A85A);
        g.fill(x, y, x + w, y + 1, RaidFx.GOLD_HI);
        RaidFx.box(g, x + 2, y + 2, x + w - 2, y + h - 2, 0x407C64E0);
        Fx2D.begin(g, false);
        for (int[] c : new int[][]{{x, y}, {x + w, y}, {x, y + h}, {x + w, y + h}}) {
            Fx2D.diamond(c[0], c[1], 3.6F, 3.6F, 0, 0xFF05040C);
            Fx2D.diamond(c[0], c[1], 2.8F, 2.8F, 0, RaidFx.GOLD_MID);
            Fx2D.diamond(c[0], c[1], 1.2F, 1.2F, 0, 0xFFFFF6DC);
        }
        Fx2D.end();
    }

    /** 층의 별: 받은 층은 금빛으로 빛나고, 아직이면 보랏빛 윤곽. */
    static void star(GuiGraphics g, float cx, float cy, float r, boolean done, float time, int i) {
        Fx2D.begin(g, true);
        if (done) {
            Fx2D.glow(cx, cy, r * 2.6F, Fx2D.rgba(STAR, 0.45F + 0.15F * (float) Math.sin(time * 2 + i)), Fx2D.rgba(STAR, 0));
        } else {
            Fx2D.glow(cx, cy, r * 2.0F, Fx2D.rgba(VIOLET, 0.22F), Fx2D.rgba(VIOLET, 0));
        }
        Fx2D.end();
        Fx2D.begin(g, false);
        Fx2D.diamond(cx, cy, r + 1.2F, r + 1.2F, 0, 0xFF05040C);
        Fx2D.diamond(cx, cy, r, r, 0, done ? 0xFFE8C26A : 0xFF3A2C70);
        Fx2D.diamond(cx, cy, r * 0.55F, r * 0.55F, 0, done ? 0xFFFFF3CC : 0xFF5C48A8);
        Fx2D.end();
        if (done) {
            Fx2D.begin(g, false);
            Fx2D.line(cx - r * 0.45F, cy, cx - r * 0.1F, cy + r * 0.35F, 1.1F, 0xFF3A2A08, 0xFF3A2A08);
            Fx2D.line(cx - r * 0.1F, cy + r * 0.35F, cx + r * 0.5F, cy - r * 0.35F, 1.1F, 0xFF3A2A08, 0xFF3A2A08);
            Fx2D.end();
        }
    }

    /** 화면 왼쪽 아래에 그린다. 받은 정보가 없으면 그리지 않는다. */
    public static void render(GuiGraphics g, int sw, int sh, int mx, int my) {
        JsonObject o = RaidClient.abyss;
        if (o == null || !o.get("enabled").getAsBoolean()) return;
        Font font = Minecraft.getInstance().font;
        float time = (System.currentTimeMillis() % 100_000L) / 1000F;
        int[] b = box(sw, sh);
        int x = b[0], y = b[1];
        g.pose().pushPose();
        g.pose().translate(0, 0, 350);
        frame(g, x, y, W, H, time);
        RaidText.outlined(g, font, t(o, "title"), RaidText.SERIF, x + 8, y + 5, RaidFx.GOLD_HI, 0xA0000000);
        int got = 0;
        for (int f = 1; f <= AbyssWeekly.FLOORS; f++) if (done(o, f)) got++;
        String cnt = got + " / " + AbyssWeekly.FLOORS;
        RaidText.right(g, font, cnt, RaidText.SERIF, x + W - 8, y + 5, got == AbyssWeekly.FLOORS ? RaidFx.GOLD_HI : 0xFFC8B8F0);
        // 여섯 층의 별
        float step = (W - 24) / 5F;
        Fx2D.begin(g, false);
        Fx2D.rect(x + 12, y + 27, x + W - 12, y + 28, 0x307C64E0, 0x307C64E0, 0x307C64E0, 0x307C64E0);
        Fx2D.end();
        for (int f = 1; f <= AbyssWeekly.FLOORS; f++) {
            float cx = x + 12 + (f - 1) * step;
            star(g, cx, y + 27.5F, 5F, done(o, f), time, f);
            g.pose().pushPose();
            g.pose().translate(cx, y + 35, 0);
            g.pose().scale(0.7F, 0.7F, 1F);
            RaidText.center(g, font, label(o, f), RaidText.SERIF, 0, 0, done(o, f) ? RaidFx.GOLD_HI : 0xFF9C8CC8);
            g.pose().popPose();
        }
        // 자세히 보기
        int[] d = detailBtn(sw, sh);
        boolean hov = Ui.inside(mx, my, d[0], d[1], d[2], d[3]);
        g.fill(d[0], d[1], d[0] + d[2], d[1] + d[3], hov ? 0xC0402C88 : 0x90201846);
        RaidFx.box(g, d[0], d[1], d[0] + d[2], d[1] + d[3], hov ? RaidFx.GOLD_HI : 0xA0D4A85A);
        String label = "자세히 보기";
        g.pose().pushPose();
        g.pose().translate(d[0] + d[2] / 2F, d[1] + 3, 0);
        g.pose().scale(0.85F, 0.85F, 1F);
        RaidText.center(g, font, label, RaidText.SANS, 0, 0, hov ? 0xFFFFF4DC : 0xFFE0D4FF);
        g.pose().popPose();
        g.pose().pushPose();
        g.pose().translate(x + 8, y + 17, 0);
        g.pose().scale(0.7F, 0.7F, 1F);
        RaidText.draw(g, font, reset(o), RaidText.SANS, 0, 0, 0xFFA898D0);
        g.pose().popPose();
        g.pose().popPose();
    }

    /** 층 선택 화면에서 누른 곳이 [자세히 보기] 면 상세 창을 연다. */
    public static boolean click(Screen screen, double mx, double my) {
        JsonObject o = RaidClient.abyss;
        if (o == null || !o.get("enabled").getAsBoolean()) return false;
        int[] d = detailBtn(screen.width, screen.height);
        if (!Ui.inside(mx, my, d[0], d[1], d[2], d[3])) return false;
        Minecraft.getInstance().setScreen(new AbyssRewardScreen(screen));
        return true;
    }
}

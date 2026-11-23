package com.rtshop.client;

import com.rtshop.pvp.Pvp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * PvP 상태 표시: 켜짐(붉은 칼) / 꺼짐(방패) + 지금 있는 곳(안전 구역 · PvP 가능 구역 · PvP 불가).
 * 위치·크기·글꼴은 /rtshop hud 에서 Gold·퀘스트 표시처럼 바꾼다.
 */
public final class PvpHud {
    private PvpHud() {
    }

    /** 서버가 알려 준 상태. */
    public static volatile boolean systemOn = false;
    public static volatile boolean on = false;
    public static volatile int area = Pvp.AREA_OFF;
    public static volatile int lock = 0;
    private static long lockAt = 0;
    /** 상태가 바뀐 시각 (잠깐 반짝이게). */
    private static long changedAt = 0;

    private static final ItemStack SWORD = new ItemStack(Items.IRON_SWORD);
    private static final ItemStack SHIELD = new ItemStack(Items.SHIELD);

    public static void set(boolean enabled, boolean nowOn, int nowArea, int lockSeconds) {
        if (nowOn != on || enabled != systemOn) {
            changedAt = System.currentTimeMillis();
        }
        systemOn = enabled;
        on = nowOn;
        area = nowArea;
        lock = lockSeconds;
        lockAt = System.currentTimeMillis();
    }

    public static void clear() {
        systemOn = false;
        on = false;
        area = Pvp.AREA_OFF;
        lock = 0;
    }

    /** 전투 잠금 남은 초 (서버가 마지막으로 알려 준 뒤 흐른 시간만큼 줄여서). */
    public static int lockLeft() {
        if (lock <= 0) {
            return 0;
        }
        long passed = (System.currentTimeMillis() - lockAt) / 1000;
        return (int) Math.max(0, lock - passed);
    }

    public static int[] bounds(int sw, int sh) {
        ClientConfig cfg = ClientConfig.get();
        int w = Math.max(40, (int) Math.round(cfg.pvpHudWidth * sw));
        int h = Math.max(14, (int) Math.round(cfg.pvpHudHeight * sh));
        int x = (int) Math.round(cfg.pvpHudX * sw);
        int y = (int) Math.round(cfg.pvpHudY * sh);
        x = Math.max(0, Math.min(sw - w, x));
        y = Math.max(0, Math.min(sh - h, y));
        return new int[]{x, y, w, h};
    }

    public static void render(GuiGraphics g, int sw, int sh) {
        Minecraft mc = Minecraft.getInstance();
        ClientConfig cfg = ClientConfig.get();
        if (!systemOn || !cfg.pvpHudEnabled || mc.options.hideGui || mc.player == null || mc.screen instanceof HudEditScreen) {
            return;
        }
        int[] b = bounds(sw, sh);
        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        draw(g, b[0], b[1], b[2], b[3], on, area, lockLeft(), System.currentTimeMillis() - changedAt);
        g.pose().popPose();
    }

    public static String areaKey(int a) {
        return a == Pvp.AREA_SAFE ? "rtshop.pvp.area_safe" : a == Pvp.AREA_ZONE ? "rtshop.pvp.area_zone"
                : a == Pvp.AREA_WILD ? "rtshop.pvp.area_wild" : "rtshop.pvp.area_off";
    }

    public static int areaColor(int a) {
        return a == Pvp.AREA_SAFE ? 0xFF8FE39A : Pvp.capable(a) ? 0xFFF2C14E : 0xFFB8AF9C;
    }

    /** sinceChange: 상태가 바뀐 뒤 흐른 ms (700ms 동안 테두리가 밝아진다). */
    public static void draw(GuiGraphics g, int x, int y, int w, int h, boolean pvpOn, int where, int lockSeconds, long sinceChange) {
        int edge = pvpOn ? 0xFFB3261E : 0xFF3F6B45;
        int inner = pvpOn ? 0xFF3A1210 : 0xFF14261A;
        if (sinceChange >= 0 && sinceChange < 700) {
            float k = 1 - sinceChange / 700F;
            edge = Fx2D.mix(edge, 0xFFFFFFFF, k * 0.7F);
        }
        g.fill(x, y, x + w, y + h, Ui.C_SHADOW);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, Ui.C_WOOD);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, Ui.C_WOOD_HI);
        g.fill(x + 1, y + 1, x + 2, y + h - 1, Ui.C_WOOD_HI);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, edge);
        g.fill(x + 4, y + 4, x + w - 4, y + h - 4, inner);

        int pad = Math.max(3, Math.min(6, h / 5));
        int icon = Math.max(8, h - pad * 2);
        float is = icon / 16F;
        g.pose().pushPose();
        g.pose().translate(x + pad, y + (h - icon) / 2F, 0);
        g.pose().scale(is, is, 1F);
        g.renderItem(pvpOn ? SWORD : SHIELD, 0, 0);
        g.pose().popPose();

        Font font = Minecraft.getInstance().font;
        ClientConfig cfg = ClientConfig.get();
        String lang = ClientConfig.get().pvpHudLang;
        String main = HudLang.tr(lang, pvpOn ? "rtshop.pvp.hud_on" : "rtshop.pvp.hud_off");
        String sub = lockSeconds > 0 ? HudLang.tr(lang, "rtshop.pvp.hud_combat", lockSeconds)
                : HudLang.tr(lang, areaKey(where));
        int subColor = lockSeconds > 0 ? 0xFFFF8A75 : areaColor(where);
        float avail = w - icon - pad * 3;
        float innerH = h - 6;
        int mainW = Math.max(1, HudFonts.width(font, main, cfg.pvpHudFont));
        int subW = Math.max(1, HudFonts.width(font, sub, cfg.pvpHudFont));
        float ms = (float) Math.min(avail / mainW, innerH * 0.56F / 8F * cfg.pvpHudTextScale);
        ms = Math.max(0.3F, Math.min(3F, ms));
        float ss = (float) Math.min(avail / subW, innerH * 0.38F / 8F * cfg.pvpHudTextScale);
        ss = Math.max(0.3F, Math.min(ms, ss));
        float total = 8 * ms + 1 + 8 * ss;
        float ty = y + (h - total) / 2F + 0.5F;
        float tx = x + pad * 2 + icon;
        g.pose().pushPose();
        g.pose().translate(tx, ty, 0);
        g.pose().scale(ms, ms, 1F);
        HudFonts.draw(g, font, main, 0, 0, pvpOn ? 0xFFFF6A5A : 0xFF9BE3A6, true, cfg.pvpHudFont);
        g.pose().popPose();
        g.pose().pushPose();
        g.pose().translate(tx, ty + 8 * ms + 1, 0);
        g.pose().scale(ss, ss, 1F);
        HudFonts.draw(g, font, sub, 0, 0, subColor, true, cfg.pvpHudFont);
        g.pose().popPose();
    }
}

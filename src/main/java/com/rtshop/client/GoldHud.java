package com.rtshop.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** 화면에 표시되는 Gold 보유량 박스. 위치는 /rtshop hud 로 옮길 수 있다. */
public final class GoldHud {
    private GoldHud() {
    }

    /** 현재 설정에 따른 박스 위치 {x, y, w, h} (GUI 좌표). */
    public static int[] bounds(int sw, int sh) {
        ClientConfig cfg = ClientConfig.get();
        int w = Math.max(36, (int) Math.round(cfg.hudWidth * sw));
        int h = Math.max(14, (int) Math.round(cfg.hudHeight * sh));
        int x = (int) Math.round(cfg.hudX * sw);
        int y = (int) Math.round(cfg.hudY * sh);
        x = Math.max(0, Math.min(sw - w, x));
        y = Math.max(0, Math.min(sh - h, y));
        return new int[]{x, y, w, h};
    }

    public static void render(GuiGraphics g, int sw, int sh) {
        Minecraft mc = Minecraft.getInstance();
        ClientConfig cfg = ClientConfig.get();
        if (!cfg.hudEnabled || mc.options.hideGui || mc.player == null || mc.screen instanceof HudEditScreen) {
            return;
        }
        int[] b = bounds(sw, sh);
        draw(g, b[0], b[1], b[2], b[3], ClientEconomy.wallet);
    }

    public static void draw(GuiGraphics g, int x, int y, int w, int h, long amount) {
        // 나무 틀 + 어두운 안쪽 판
        g.fill(x, y, x + w, y + h, Ui.C_SHADOW);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, Ui.C_WOOD);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, Ui.C_WOOD_HI);
        g.fill(x + 1, y + 1, x + 2, y + h - 1, Ui.C_WOOD_HI);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, Ui.C_GOLD_DARK);
        g.fill(x + 4, y + 4, x + w - 4, y + h - 4, Ui.C_WOOD_DARK);

        int pad = Math.max(3, Math.min(6, h / 5));
        int icon = Math.max(8, h - pad * 2);
        Ui.coin(g, x + pad, y + (h - icon) / 2, icon);

        Font font = Minecraft.getInstance().font;
        String txt = Ui.num(amount) + " G";
        float avail = w - icon - pad * 3;
        float scale = Math.min(avail / Math.max(1, font.width(txt)), (h * 0.5F) / 8.0F);
        scale = Math.max(0.4F, Math.min(3.0F, scale));
        float tw = font.width(txt) * scale;
        Ui.scaledText(g, font, txt, x + w - pad - tw, y + (h - 8 * scale) / 2.0F + 0.5F, scale, Ui.C_GOLD, true);
    }
}

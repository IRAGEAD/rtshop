package com.rtshop.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * 룬 제단 화면 설정 패널: 글꼴 · 창 크기 · 글자 그림자 · 뽑기 연출 · 장면 옆 정보 패널의 자리.
 * 줄을 누르면 다음 값으로 넘어가고, 바로 저장되어 화면에 적용된다 (config/rtshop/client.json 의 magic*).
 */
public final class MagicUiPanel {
    public static final int W = 230, H = 162;
    private final Runnable onClose;
    private final List<Object[]> hits = new ArrayList<>(); // {x, y, w, h, Runnable}

    public MagicUiPanel(Runnable onClose) {
        this.onClose = onClose;
    }

    private static String tr(String k, Object... a) {
        return Component.translatable(k, a).getString();
    }

    private static int indexOf(double[] a, double v) {
        int best = 0;
        for (int i = 0; i < a.length; i++) if (Math.abs(a[i] - v) < Math.abs(a[best] - v)) best = i;
        return best;
    }

    /** 줄 번호(0 글꼴 · 1 창 크기 · 2 그림자 · 3 연출 · 4 패널 자리)의 값을 다음으로 넘긴다. */
    public static void cycle(int row) {
        ClientConfig c = ClientConfig.get();
        switch (row) {
            case 0 -> {
                int i = 0;
                for (int k = 0; k < MagicUi.FACES.length; k++) if (MagicUi.FACES[k].equals(c.magicFont)) i = k;
                c.magicFont = MagicUi.FACES[(i + 1) % MagicUi.FACES.length];
            }
            case 1 -> c.magicScale = MagicUi.SCALES[(indexOf(MagicUi.SCALES, c.magicScale) + 1) % MagicUi.SCALES.length];
            case 2 -> c.magicShadow = !c.magicShadow;
            case 3 -> c.magicFx = (c.magicFx + 1) % 3;
            case 4 -> c.magicPanelLeft = !c.magicPanelLeft;
            default -> {
            }
        }
        c.save();
    }

    private static String value(int row) {
        ClientConfig c = ClientConfig.get();
        return switch (row) {
            case 0 -> tr("rtshop.magic.ui.font." + (java.util.Arrays.asList(MagicUi.FACES).contains(c.magicFont) ? c.magicFont : "sans"));
            case 1 -> Math.round(MagicUi.SCALES[indexOf(MagicUi.SCALES, c.magicScale)] * 100) + "%";
            case 2 -> tr(c.magicShadow ? "rtshop.magic.ui.on" : "rtshop.magic.ui.off");
            case 3 -> tr("rtshop.magic.ui.fx." + Math.max(0, Math.min(2, c.magicFx)));
            default -> tr(c.magicPanelLeft ? "rtshop.magic.ui.panel.left" : "rtshop.magic.ui.panel.right");
        };
    }

    /** 화면(sw × sh) 한가운데에 그린다. */
    public void render(GuiGraphics g, Font font, int mx, int my, int sw, int sh) {
        hits.clear();
        ResourceLocation f = MagicUi.body(), ft = MagicUi.head();
        int x = (sw - W) / 2, y = (sh - H) / 2;
        MagicGachaScreen.slab(g, x, y, W, H);
        MagicUi.draw(g, font, tr("rtshop.magic.ui.title"), ft, x + 10, y + 8, MagicGachaScreen.C_GOLD);
        g.fill(x + 8, y + 22, x + W - 8, y + 23, 0xFF3A2A66);
        String[] names = {"rtshop.magic.ui.font", "rtshop.magic.ui.scale", "rtshop.magic.ui.shadow", "rtshop.magic.ui.fx", "rtshop.magic.ui.panel"};
        for (int i = 0; i < names.length; i++) {
            final int row = i;
            int ry = y + 28 + i * 18;
            MagicUi.draw(g, font, MagicUi.clip(font, tr(names[i]), f, 104), f, x + 12, ry + 4, MagicGachaScreen.C_TEXT);
            int bx = x + 122, bw = W - 122 - 12;
            boolean hov = mx >= bx && mx < bx + bw && my >= ry && my < ry + 15;
            g.fill(bx, ry, bx + bw, ry + 15, hov ? 0xFF2E2250 : 0xFF161028);
            MagicGachaScreen.box(g, bx, ry, bw, 15, hov ? MagicGachaScreen.C_EDGE : 0xFF5A4690);
            String v = MagicUi.clip(font, value(i), f, bw - 8);
            MagicUi.draw(g, font, v, f, bx + (bw - MagicUi.w(font, v, f)) / 2, ry + 4, hov ? MagicGachaScreen.C_GOLD : MagicGachaScreen.C_TEXT);
            hits.add(new Object[]{bx, ry, bw, 15, (Runnable) () -> cycle(row)});
        }
        // 견본 한 줄
        int sy = y + 28 + names.length * 18 + 3;
        g.fill(x + 8, sy, x + W - 8, sy + 1, 0xFF3A2A66);
        String sample = MagicUi.clip(font, tr("rtshop.magic.ui.sample"), f, W - 24);
        MagicUi.draw(g, font, sample, f, x + 12, sy + 6, MagicGachaScreen.C_DIM);
        int by = y + H - 22;
        button(g, font, mx, my, x + 12, by, 70, tr("rtshop.magic.ui.reset"), () -> {
            ClientConfig.get().resetMagic();
            ClientConfig.get().save();
        });
        button(g, font, mx, my, x + W - 72, by, 60, tr("rtshop.enhodds.close"), onClose);
    }

    private void button(GuiGraphics g, Font font, int mx, int my, int x, int y, int w, String label, Runnable r) {
        ResourceLocation f = MagicUi.body();
        boolean hov = mx >= x && mx < x + w && my >= y && my < y + 16;
        g.fill(x, y, x + w, y + 16, hov ? 0xFF2E2250 : 0xFF161028);
        MagicGachaScreen.box(g, x, y, w, 16, hov ? MagicGachaScreen.C_EDGE : 0xFF5A4690);
        String s = MagicUi.clip(font, label, f, w - 6);
        MagicUi.draw(g, font, s, f, x + (w - MagicUi.w(font, s, f)) / 2, y + 4, hov ? MagicGachaScreen.C_GOLD : MagicGachaScreen.C_TEXT);
        hits.add(new Object[]{x, y, w, 16, r});
    }

    public boolean click(int mx, int my) {
        for (Object[] h : new ArrayList<>(hits)) {
            int x = (int) h[0], y = (int) h[1], w = (int) h[2], hh = (int) h[3];
            if (mx >= x && mx < x + w && my >= y && my < y + hh) {
                net.minecraft.client.Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1F));
                ((Runnable) h[4]).run();
                return true;
            }
        }
        return false;
    }
}

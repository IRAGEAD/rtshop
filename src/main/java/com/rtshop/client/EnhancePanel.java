package com.rtshop.client;

import com.rtshop.enhance.EnhanceMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 강화 정보 패널의 자리·크기 (설정은 client.json, 게임 안에서는 패널 아래 [⚙] 로 바꾼다).
 * 패널 아래에 [확률표] [⚙ 설정] (OP 는 [편집]) 단추가 붙는다. 패널을 끄면 [⚙ 강화 정보] 단추만 남는다.
 */
public final class EnhancePanel {
    private EnhancePanel() {
    }

    public static final int W = 168, H = 166, BTN = 16;

    /** {x, y, w(화면), h(화면), 배율×100}. auto = 강화 창이면 창 오른쪽, 아니면 화면 오른쪽 가운데. */
    public static int[] rect(Screen s, int autoX, int autoY) {
        ClientConfig c = ClientConfig.get();
        double sc = c.enhPanelScale;
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

    /** 단추들: {x, y, w, 종류(0 확률표, 1 설정, 2 OP 편집, 3 다시 켜기)}. */
    public static java.util.List<int[]> buttons(int[] r) {
        java.util.List<int[]> out = new java.util.ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        int by = r[1] + r[3] + 3;
        if (!ClientConfig.get().enhPanelEnabled) {
            out.add(new int[]{r[0] + r[2] - 90, r[1], 90, 3});
            return out;
        }
        boolean op = mc.player != null && mc.player.hasPermissions(2);
        int n = op ? 3 : 2;
        int gap = 2;
        int bw = (r[2] - gap * (n - 1)) / n;
        out.add(new int[]{r[0], by, bw, 0});
        out.add(new int[]{r[0] + bw + gap, by, bw, 1});
        if (op) out.add(new int[]{r[0] + 2 * (bw + gap), by, r[2] - 2 * (bw + gap), 2});
        return out;
    }

    static String label(int kind, boolean tableOpen) {
        return switch (kind) {
            case 0 -> Component.translatable(tableOpen ? "rtshop.enhodds.hide" : "rtshop.enhodds.open").getString();
            case 1 -> Component.translatable("rtshop.enhui.settings").getString();
            case 2 -> Component.translatable("rtshop.enhodds.edit_short").getString();
            default -> Component.translatable("rtshop.enhui.show_again").getString();
        };
    }

    public static void render(GuiGraphics g, Font font, EnhanceMenu menu, Screen s, int[] r, int mx, int my, boolean tableOpen) {
        Minecraft mc = Minecraft.getInstance();
        if (ClientConfig.get().enhPanelEnabled) {
            float sc = r[4] / 100F;
            g.pose().pushPose();
            g.pose().translate(r[0], r[1], 0);
            g.pose().scale(sc, sc, 1);
            EnhanceScreen.drawPanel(g, font, menu, mc.player, 0, 0, W, H);
            g.pose().popPose();
        }
        for (int[] b : buttons(r)) {
            Ui.button(g, font, b[0], b[1], b[2], BTN, Ui.clip(font, label(b[3], tableOpen), b[2] - 6),
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
        if (kind == 1) {
            mc.setScreen(new EnhanceUiScreen(from));
        } else if (kind == 2) {
            mc.player.closeContainer();
            com.rtshop.net.ShopNet.CHANNEL.sendToServer(new com.rtshop.net.EnhanceActionPacket(com.rtshop.net.EnhanceActionPacket.OPEN_EDITOR, ""));
        } else if (kind == 3) {
            ClientConfig.get().enhPanelEnabled = true;
            ClientConfig.get().save();
        }
    }
}

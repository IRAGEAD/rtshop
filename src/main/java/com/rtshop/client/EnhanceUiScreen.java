package com.rtshop.client;

import com.rtshop.enhance.EnhanceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 강화 정보 패널 설정: 끌어서 옮기기, 크기, 켜기/끄기, 기본 위치. 닫으면 강화 화면(또는 모루 장면)으로 돌아간다.
 */
public class EnhanceUiScreen extends Screen {
    private final Screen parent;
    private boolean dragging;
    private double grabX, grabY;
    private ChoicePopup popup;

    public EnhanceUiScreen(Screen parent) {
        super(Component.translatable("rtshop.enhui.title"));
        this.parent = parent;
    }

    private int[] rect() {
        if (parent instanceof EnhanceScreen es) return EnhancePanel.rect(this, es.autoX(), es.autoY());
        return EnhanceOverlay.rect(this);
    }

    private int[][] row() {
        int[] ws = {86, 86, 86, 70};
        int total = ws[0] + ws[1] + ws[2] + ws[3] + 12;
        int x = Math.max(4, Math.min(this.width / 2 - total / 2, this.width - total - 4));
        int[][] out = new int[4][];
        for (int i = 0; i < 4; i++) {
            out[i] = new int[]{x, ws[i]};
            x += ws[i] + 4;
        }
        return out;
    }

    private int rowY() {
        return this.height - 26;
    }

    private String rowLabel(int i) {
        ClientConfig c = ClientConfig.get();
        return switch (i) {
            case 0 -> Component.translatable(c.enhPanelEnabled ? "rtshop.hud.shown" : "rtshop.hud.hidden").getString();
            case 1 -> Component.translatable("rtshop.hud.text_size", Math.round(c.enhPanelScale * 100)).getString() + " ▼";
            case 2 -> Component.translatable("rtshop.hud.reset").getString();
            default -> Component.translatable("rtshop.hud.done").getString();
        };
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        g.fill(0, 0, width, height, 0x88000000);
        int[] r = rect();
        if (minecraft.player != null && minecraft.player.containerMenu instanceof EnhanceMenu m) {
            if (ClientConfig.get().enhPanelEnabled) {
                float sc = r[4] / 100F;
                g.pose().pushPose();
                g.pose().translate(r[0], r[1], 0);
                g.pose().scale(sc, sc, 1);
                EnhancePanel.draw(g, font, m, minecraft.player, 0, 0, EnhancePanel.W, EnhancePanel.H);
                g.pose().popPose();
            }
        } else {
            Ui.woodFrame(g, r[0], r[1], r[2], r[3]);
        }
        int c = dragging ? 0xFFFFE08A : Ui.inside(mx, my, r[0], r[1], r[2], r[3]) ? 0xCCFFFFFF : 0x88FFFFFF;
        g.fill(r[0] - 2, r[1] - 2, r[0] + r[2] + 2, r[1] - 1, c);
        g.fill(r[0] - 2, r[1] + r[3] + 1, r[0] + r[2] + 2, r[1] + r[3] + 2, c);
        g.fill(r[0] - 2, r[1] - 1, r[0] - 1, r[1] + r[3] + 1, c);
        g.fill(r[0] + r[2] + 1, r[1] - 1, r[0] + r[2] + 2, r[1] + r[3] + 1, c);
        if (!ClientConfig.get().enhPanelEnabled) {
            g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], 0x99000000);
            g.drawCenteredString(font, Component.translatable("rtshop.hud.hidden"), r[0] + r[2] / 2, r[1] + r[3] / 2 - 4, 0xFFCCCCCC);
        }
        g.drawCenteredString(font, title, width / 2, 10, 0xFFF5E6C0);
        g.drawCenteredString(font, Component.translatable("rtshop.enhui.help"), width / 2, 22, 0xFFD9C592);
        int[][] row = row();
        for (int i = 0; i < row.length; i++) {
            Ui.button(g, font, row[i][0], rowY(), row[i][1], 18, rowLabel(i), Ui.inside(mx, my, row[i][0], rowY(), row[i][1], 18), true);
        }
        if (popup != null) popup.render(g, font, mx, my);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (popup != null) {
            popup.click(mx, my);
            popup = null;
            ClientConfig.get().save();
            return true;
        }
        int[][] row = row();
        ClientConfig c = ClientConfig.get();
        for (int i = 0; i < row.length; i++) {
            if (!Ui.inside(mx, my, row[i][0], rowY(), row[i][1], 18)) continue;
            switch (i) {
                case 0 -> c.enhPanelEnabled = !c.enhPanelEnabled;
                case 1 -> {
                    List<ChoicePopup.Option> opts = new ArrayList<>();
                    for (int n : new int[]{60, 70, 80, 90, 100, 110, 120, 135, 150}) opts.add(new ChoicePopup.Option(Integer.toString(n), n + "%"));
                    popup = new ChoicePopup(row[1][0], rowY(), row[1][1], 18, height, font, opts, Long.toString(Math.round(c.enhPanelScale * 100)),
                            v -> c.enhPanelScale = Integer.parseInt(v) / 100.0);
                }
                case 2 -> c.resetEnhPanel();
                default -> onClose();
            }
            c.save();
            return true;
        }
        int[] r = rect();
        if (Ui.inside(mx, my, r[0], r[1], r[2], r[3])) {
            dragging = true;
            grabX = mx - r[0];
            grabY = my - r[1];
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging) {
            ClientConfig c = ClientConfig.get();
            c.enhPanelX = Math.max(0, Math.min(1, (mx - grabX) / Math.max(1, width)));
            c.enhPanelY = Math.max(0, Math.min(1, (my - grabY) / Math.max(1, height)));
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging) {
            dragging = false;
            ClientConfig.get().save();
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public void onClose() {
        ClientConfig.get().save();
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

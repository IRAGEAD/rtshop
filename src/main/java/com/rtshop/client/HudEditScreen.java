package com.rtshop.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Gold 표시 박스를 끌어서 옮기고 크기를 바꾸는 화면. 닫을 때 자동 저장된다. */
public class HudEditScreen extends Screen {
    private boolean dragging = false;
    private double grabX, grabY;

    public HudEditScreen() {
        super(Component.translatable("rtshop.hud.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int btnX(int i) {
        return this.width / 2 - 153 + i * 78;
    }

    private int btnY() {
        return this.height - 30;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        g.fill(0, 0, this.width, this.height, 0x66000000);
        int[] b = GoldHud.bounds(this.width, this.height);
        boolean hover = Ui.inside(mx, my, b[0], b[1], b[2], b[3]);
        g.fill(b[0] - 2, b[1] - 2, b[0] + b[2] + 2, b[1] + b[3] + 2, hover || dragging ? 0xFFFFE08A : 0x88FFFFFF);
        GoldHud.draw(g, b[0], b[1], b[2], b[3], ClientEconomy.wallet);

        g.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFF5E6C0);
        g.drawCenteredString(this.font, Component.translatable("rtshop.hud.help1"), this.width / 2, 26, 0xFFD9C592);
        g.drawCenteredString(this.font, Component.translatable("rtshop.hud.help2"), this.width / 2, 38, 0xFFD9C592);

        String[] labels = {"rtshop.hud.smaller", "rtshop.hud.bigger", "rtshop.hud.reset", "rtshop.hud.done"};
        for (int i = 0; i < 4; i++) {
            Ui.button(g, this.font, btnX(i), btnY(), 72, 18, Component.translatable(labels[i]).getString(),
                    Ui.inside(mx, my, btnX(i), btnY(), 72, 18), true);
        }
        ClientConfig cfg = ClientConfig.get();
        String toggle = Component.translatable(cfg.hudEnabled ? "rtshop.hud.shown" : "rtshop.hud.hidden").getString();
        Ui.button(g, this.font, this.width / 2 - 50, btnY() - 22, 100, 18, toggle,
                Ui.inside(mx, my, this.width / 2 - 50, btnY() - 22, 100, 18), true);
    }

    private void scale(double factor) {
        ClientConfig cfg = ClientConfig.get();
        double cx = cfg.hudX + cfg.hudWidth / 2;
        double cy = cfg.hudY + cfg.hudHeight / 2;
        cfg.hudWidth *= factor;
        cfg.hudHeight *= factor;
        cfg.hudWidth = Math.max(0.05, Math.min(0.6, cfg.hudWidth));
        cfg.hudHeight = Math.max(0.03, Math.min(0.4, cfg.hudHeight));
        cfg.hudX = cx - cfg.hudWidth / 2;
        cfg.hudY = cy - cfg.hudHeight / 2;
        cfg.clamp();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) {
            return false;
        }
        ClientConfig cfg = ClientConfig.get();
        if (Ui.inside(mx, my, btnX(0), btnY(), 72, 18)) {
            scale(1 / 1.1);
            return true;
        }
        if (Ui.inside(mx, my, btnX(1), btnY(), 72, 18)) {
            scale(1.1);
            return true;
        }
        if (Ui.inside(mx, my, btnX(2), btnY(), 72, 18)) {
            cfg.resetHud();
            return true;
        }
        if (Ui.inside(mx, my, btnX(3), btnY(), 72, 18)) {
            this.onClose();
            return true;
        }
        if (Ui.inside(mx, my, this.width / 2 - 50, btnY() - 22, 100, 18)) {
            cfg.hudEnabled = !cfg.hudEnabled;
            return true;
        }
        int[] b = GoldHud.bounds(this.width, this.height);
        if (Ui.inside(mx, my, b[0], b[1], b[2], b[3])) {
            dragging = true;
            grabX = mx - b[0];
            grabY = my - b[1];
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging) {
            ClientConfig cfg = ClientConfig.get();
            cfg.hudX = (mx - grabX) / Math.max(1, this.width);
            cfg.hudY = (my - grabY) / Math.max(1, this.height);
            cfg.clamp();
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scale(delta > 0 ? 1.05 : 1 / 1.05);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        ClientConfig cfg = ClientConfig.get();
        double sx = 1.0 / Math.max(1, this.width);
        double sy = 1.0 / Math.max(1, this.height);
        switch (key) {
            case 263 -> cfg.hudX -= sx; // 왼쪽
            case 262 -> cfg.hudX += sx; // 오른쪽
            case 265 -> cfg.hudY -= sy; // 위
            case 264 -> cfg.hudY += sy; // 아래
            default -> {
                return super.keyPressed(key, scan, mods);
            }
        }
        cfg.clamp();
        return true;
    }

    @Override
    public void removed() {
        super.removed();
        ClientConfig.get().save();
    }
}

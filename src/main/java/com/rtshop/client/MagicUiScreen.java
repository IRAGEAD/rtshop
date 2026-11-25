package com.rtshop.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** 룬 제단 화면 설정만 따로 띄운 창 (Cinematic 장면에서 [화면 설정] 을 누르면 장면에서 물러난 뒤 뜬다). */
public class MagicUiScreen extends Screen {
    private final MagicUiPanel panel = new MagicUiPanel(this::onClose);

    public MagicUiScreen() {
        super(Component.translatable("rtshop.magic.ui.title"));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        panel.render(g, font, mx, my, width, height);
        super.render(g, mx, my, partial);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        return panel.click((int) mx, (int) my) || super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

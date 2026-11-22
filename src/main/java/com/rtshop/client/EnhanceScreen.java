package com.rtshop.client;

import com.rtshop.enhance.Enhance;
import com.rtshop.enhance.EnhanceConfig;
import com.rtshop.enhance.EnhanceMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 강화 모루 창 (Cinematic Interactions 가 없거나 장면을 끈 경우에 보이는 창).
 * 왼쪽은 모루 칸(장비 · 방지권 · 결과), 오른쪽 패널에 확률·재료·Gold 가 보인다. 결과 칸을 클릭하면 강화한다.
 */
public class EnhanceScreen extends AbstractContainerScreen<EnhanceMenu> {
    private static final ResourceLocation BG = new ResourceLocation("minecraft", "textures/gui/container/anvil.png");
    public static final int PANEL_W = EnhancePanel.W;
    private boolean showOdds;

    public EnhanceScreen(EnhanceMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelX = 60;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        // 패널이 자동 자리(창 오른쪽)에 들어갈 만큼 창을 왼쪽으로 민다
        if (ClientConfig.get().enhPanelX < 0 && ClientConfig.get().enhPanelEnabled) {
            int total = imageWidth + 6 + (int) Math.round(EnhancePanel.W * ClientConfig.get().enhPanelScale);
            this.leftPos = Math.max(2, (this.width - total) / 2);
        }
    }

    int[] panelRect() {
        return EnhancePanel.rect(this, autoX(), autoY());
    }

    int autoX() {
        return leftPos + imageWidth + 6;
    }

    /** 정보 창이 강화 창보다 길어서, 둘의 가운데를 맞춘다. */
    int autoY() {
        int ph = (int) Math.round(EnhancePanel.H * ClientConfig.get().enhPanelScale) + EnhancePanel.BTN + 3;
        return Math.max(2, topPos - (ph - imageHeight) / 2);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        super.render(g, mx, my, partial);
        renderPanel(g, mx, my);
        renderTooltip(g, mx, my);
        if (showOdds) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 400);
            EnhanceOddsScreen.drawTable(g, font, (width - EnhanceOddsScreen.tableWidth()) / 2, Math.max(2, (height - EnhanceOddsScreen.tableHeight()) / 2));
            g.pose().popPose();
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        int x = leftPos, y = topPos;
        g.blit(BG, x, y, 0, 0, imageWidth, imageHeight);
        // 이름 입력칸 자리에 현재 → 다음 단계 표시
        g.fill(x + 59, y + 20, x + 169, y + 36, 0xFF3B3B3B);
        g.fill(x + 60, y + 21, x + 168, y + 35, 0xFF1E1E1E);
        ItemStack eq = menu.equipment();
        String label;
        int col = 0xFFE0E0E0;
        if (eq.isEmpty()) {
            label = Component.translatable("rtshop.enhance.put_item").getString();
            col = 0xFF9A9A9A;
        } else if (!Enhance.enhanceable(eq)) {
            label = Component.translatable("rtshop.enhance.not_enhanceable").getString();
            col = 0xFFFF6A6A;
        } else if (Enhance.level(eq) >= EnhanceConfig.MAX_LEVEL) {
            label = Component.translatable("rtshop.enhance.max").getString();
            col = 0xFFFF77FF;
        } else {
            label = "+" + Enhance.level(eq) + "  →  +" + (Enhance.level(eq) + 1);
            col = 0xFFFFD95A;
        }
        label = Ui.clip(font, label, 104);
        g.drawString(font, label, x + 114 - font.width(label) / 2, y + 25, col, false);
        boolean has = menu.getSlot(2).hasItem();
        if (!eq.isEmpty() && !has) {
            g.blit(BG, x + 99, y + 45, imageWidth, 0, 28, 21);
        }
        if (has && !menu.getSlot(2).mayPickup(minecraft.player)) {
            g.fill(x + 134, y + 47, x + 150, y + 63, 0x80FF2020);
        }
    }

    private void renderPanel(GuiGraphics g, int mx, int my) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        EnhancePanel.render(g, font, menu, this, panelRect(), mx, my, showOdds);
        g.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (showOdds) {
            showOdds = false;
            return true;
        }
        int k = EnhancePanel.clicked(panelRect(), mx, my);
        if (k == 0) {
            showOdds = true;
            return true;
        }
        if (k > 0) {
            EnhancePanel.act(k, this);
            if (k == 3) rebuildWidgets();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        return EnhancePanel.scroll(panelRect(), mx, my, delta) || super.mouseScrolled(mx, my, delta);
    }

    public void showOddsForTest(boolean on) {
        showOdds = on;
    }

    /** 시험용: 오른쪽 패널이 화면 안에 들어오는가. */
    public boolean fitsForTest() {
        int[] r = panelRect();
        return leftPos >= 0 && leftPos + imageWidth <= width && r[0] + r[2] <= width && r[1] + r[3] + EnhancePanel.BTN + 3 <= height;
    }
}

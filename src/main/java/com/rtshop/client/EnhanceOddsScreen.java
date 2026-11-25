package com.rtshop.client;

import com.rtshop.enhance.Enhance;
import com.rtshop.enhance.EnhanceConfig;
import com.rtshop.net.EnhanceActionPacket;
import com.rtshop.net.ShopNet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** 강화 확률 공개표: 단계마다 성공·유지·손상·파괴 확률, 드는 Gold·재료, 강화 효과. */
public class EnhanceOddsScreen extends Screen {
    private final Screen parent;
    public static final int ROW = 17;

    public EnhanceOddsScreen(Screen parent) {
        super(Component.translatable("rtshop.enhodds.title"));
        this.parent = parent;
    }

    public static int tableWidth() {
        return 452;
    }

    public static int tableHeight() {
        return 28 + ROW * EnhanceConfig.MAX_LEVEL + 34;
    }

    /** 표를 (x, y) 에 그린다. 시네마틱 장면 위 오버레이에서도 쓴다. */
    public static void drawTable(GuiGraphics g, Font font, int x, int y) {
        EnhanceConfig cfg = EnhanceConfig.client();
        int w = tableWidth(), h = tableHeight();
        Ui.woodFrame(g, x, y, w, h);
        Ui.bevel(g, x + 5, y + 5, w - 10, h - 10, Ui.C_PARCH, Ui.C_PARCH_HI, Ui.C_PARCH_SHADE);
        String[] head = {"rtshop.enhodds.target", "rtshop.enhodds.success", "rtshop.enhodds.maintain", "rtshop.enhodds.damage",
                "rtshop.enhodds.destroy", "rtshop.enhodds.effect", "rtshop.enhodds.gold", "rtshop.enhodds.materials"};
        int[] cx = {x + 12, x + 52, x + 92, x + 132, x + 172, x + 212, x + 268, x + 318};
        int[] col = {Ui.C_INK, Ui.C_GREEN, 0xFF4A4A4A, 0xFFB86A10, Ui.C_RED, 0xFF1F6F8B, Ui.C_GOLD_DARK, Ui.C_INK};
        for (int i = 0; i < head.length; i++) {
            g.drawString(font, Component.translatable(head[i]), cx[i], y + 11, col[i], false);
        }
        g.fill(x + 10, y + 22, x + w - 10, y + 23, Ui.C_PARCH_SHADE);
        long tick = System.currentTimeMillis() / 1000;
        for (int lv = 1; lv <= EnhanceConfig.MAX_LEVEL; lv++) {
            EnhanceConfig.Level l = cfg.level(lv);
            int ry = y + 26 + (lv - 1) * ROW;
            if (lv % 2 == 0) g.fill(x + 8, ry - 2, x + w - 8, ry + ROW - 2, 0x18000000);
            if (l == null) continue;
            double t = l.success + l.maintain + l.damage + l.destroy;
            g.drawString(font, "+" + lv, cx[0], ry + 3, EnhanceClient.color(lv).getColor() == null ? Ui.C_INK : 0xFF000000 | darker(EnhanceClient.color(lv).getColor()), false);
            g.drawString(font, EnhanceClient.pct(l.success * 100 / t), cx[1], ry + 3, col[1], false);
            g.drawString(font, EnhanceClient.pct(l.maintain * 100 / t), cx[2], ry + 3, col[2], false);
            g.drawString(font, EnhanceClient.pct(l.damage * 100 / t), cx[3], ry + 3, col[3], false);
            g.drawString(font, EnhanceClient.pct(l.destroy * 100 / t), cx[4], ry + 3, col[4], false);
            String eff = "+" + EnhanceClient.pct(cfg.weaponBonus[Math.min(lv, cfg.weaponBonus.length - 1)]);
            g.drawString(font, eff, cx[5], ry + 3, col[5], false);
            g.drawString(font, Ui.compact(l.gold), cx[6], ry + 3, col[6], false);
            int mx = cx[7];
            for (Enhance.Need n : Enhance.needs(cfg, l, Minecraft.getInstance().player)) {
                Item icon = n.items.get((int) (tick % n.items.size()));
                g.renderItem(new ItemStack(icon), mx, ry - 1);
                g.renderItemDecorations(font, new ItemStack(icon, n.need), mx, ry - 1);
                mx += 22;
            }
        }
        int fy = y + 26 + EnhanceConfig.MAX_LEVEL * ROW + 2;
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.translatable("rtshop.enhodds.footer"), w - 24);
        for (net.minecraft.util.FormattedCharSequence line : lines) {
            g.drawString(font, line, x + 12, fy, Ui.C_INK_SOFT, false);
            fy += 10;
        }
    }

    private static int darker(int rgb) {
        return (((rgb >> 16) & 255) * 3 / 5 << 16) | (((rgb >> 8) & 255) * 3 / 5 << 8) | ((rgb & 255) * 3 / 5);
    }

    @Override
    protected void init() {
        int x = (width - tableWidth()) / 2, y = Math.max(4, (height - tableHeight() - 24) / 2);
        int by = y + tableHeight() + 4;
        addRenderableWidget(MButton.of(Component.translatable("rtshop.enhodds.close"), b -> onClose()).bounds(x + tableWidth() - 80, by, 80, 18).build());
        if (minecraft.player != null && com.rtshop.util.Perm.op(minecraft.player)) {
            addRenderableWidget(MButton.of(Component.translatable("rtshop.enhodds.edit"),
                    b -> ShopNet.CHANNEL.sendToServer(new EnhanceActionPacket(EnhanceActionPacket.OPEN_EDITOR, ""))).bounds(x, by, 100, 18).build());
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        int x = (width - tableWidth()) / 2, y = Math.max(4, (height - tableHeight() - 24) / 2);
        drawTable(g, font, x, y);
        g.drawCenteredString(font, title, width / 2, Math.max(0, y - 10), 0xFFFFFFFF);
        super.render(g, mx, my, partial);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

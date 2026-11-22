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
    public static final int PANEL_W = 168;
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
        this.leftPos = (this.width - (this.imageWidth + 6 + PANEL_W)) / 2;
        int bx = leftPos + imageWidth + 6, by = topPos + imageHeight + 3;
        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("rtshop.enhodds.open"),
                b -> showOdds = !showOdds).bounds(bx, by, 80, 16).build());
        if (minecraft.player != null && minecraft.player.hasPermissions(2)) {
            addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("rtshop.enhodds.edit"),
                    b -> {
                        onClose();
                        com.rtshop.net.ShopNet.CHANNEL.sendToServer(new com.rtshop.net.EnhanceActionPacket(
                                com.rtshop.net.EnhanceActionPacket.OPEN_EDITOR, ""));
                    }).bounds(bx + 84, by, 84, 16).build());
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        super.render(g, mx, my, partial);
        renderPanel(g);
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

    private void renderPanel(GuiGraphics g) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        drawPanel(g, font, menu, minecraft.player, leftPos + imageWidth + 6, topPos, PANEL_W, imageHeight);
        g.pose().popPose();
    }

    /** 강화 정보 패널 (확률·재료·Gold·방지권). 강화 창 옆과 시네마틱 모루 장면 위에 함께 쓴다. */
    public static void drawPanel(GuiGraphics g, net.minecraft.client.gui.Font font, EnhanceMenu menu, net.minecraft.world.entity.player.Player player,
                                 int x, int y, int w, int h) {
        Ui.woodFrame(g, x, y, w, h);
        Ui.bevel(g, x + 5, y + 5, w - 10, h - 10, Ui.C_PARCH, Ui.C_PARCH_HI, Ui.C_PARCH_SHADE);
        int tx = x + 10, ty = y + 10, tw = w - 20;
        g.drawString(font, Component.translatable("rtshop.enhance.panel"), tx, ty, Ui.C_INK, false);
        ty += 13;
        ItemStack eq = menu.equipment();
        EnhanceConfig cfg = EnhanceConfig.client();
        if (eq.isEmpty() || !Enhance.enhanceable(eq) || cfg.level(Enhance.level(eq) + 1) == null) {
            for (String k : new String[]{"rtshop.enhance.help1", "rtshop.enhance.help2", "rtshop.enhance.help3", "rtshop.enhance.help4"}) {
                for (FormattedCharSequence line : font.split(Component.translatable(k), tw)) {
                    if (ty > y + h - 24) break;
                    g.drawString(font, line, tx, ty, Ui.C_INK_SOFT, false);
                    ty += 10;
                }
                ty += 3;
            }
            return;
        }
        int cur = Enhance.level(eq);
        EnhanceConfig.Level l = cfg.level(cur + 1);
        g.drawString(font, Component.literal("+" + cur + " → +" + (cur + 1)), tx + tw - font.width("+" + cur + " → +" + (cur + 1)), y + 10, 0xFF8A5A00, false);
        ItemStack gs = menu.guardStack();
        Enhance.Guard guard = Enhance.guardOf(gs);
        if (!guard.relevant(l)) guard = Enhance.Guard.NONE;
        double[] e = EnhanceClient.effective(l, guard, cur);
        String[] keys = {"rtshop.enhance.odds_success", "rtshop.enhance.odds_maintain", "rtshop.enhance.odds_damage", "rtshop.enhance.odds_destroy"};
        int[] cols = {Ui.C_GREEN, 0xFF4A4A4A, 0xFFB86A10, Ui.C_RED};
        for (int i = 0; i < 4; i++) {
            int cx = tx + (i % 2) * (tw / 2), cy = ty + (i / 2) * 11;
            g.drawString(font, Component.translatable(keys[i], EnhanceClient.pct(e[i])), cx, cy, cols[i], false);
        }
        ty += 25;
        g.fill(tx, ty - 2, tx + tw, ty - 1, Ui.C_PARCH_SHADE);
        boolean free = player.getAbilities().instabuild;
        long tick = System.currentTimeMillis() / 1000;
        for (Enhance.Need n : Enhance.needs(cfg, l, player)) {
            Item icon = n.items.get((int) (tick % n.items.size()));
            g.renderItem(new ItemStack(icon), tx, ty);
            String name = Ui.clip(font, EnhanceClient.materialName(cfg, n).getString(), tw - 52);
            g.drawString(font, name, tx + 19, ty + 4, Ui.C_INK, false);
            String cnt = n.have + "/" + n.need;
            g.drawString(font, cnt, tx + tw - font.width(cnt), ty + 4, n.ok() || free ? Ui.C_GREEN : Ui.C_RED, false);
            ty += 17;
        }
        Ui.coin(g, tx + 2, ty + 1, 12);
        String gold = Ui.num(l.gold) + " G";
        boolean goldOk = free || ClientEconomy.wallet >= l.gold;
        g.drawString(font, gold, tx + 19, ty + 3, goldOk ? Ui.C_GOLD_DARK : Ui.C_RED, false);
        String have = "(" + Ui.num(ClientEconomy.wallet) + ")";
        g.drawString(font, have, tx + tw - font.width(have), ty + 3, Ui.C_INK_SOFT, false);
        ty += 16;
        Component gl = guard != Enhance.Guard.NONE ? Component.translatable("rtshop.enhance.guard_used", gs.getHoverName())
                : Component.translatable(l.destroy > 0 ? "rtshop.enhance.destroy_warn" : "rtshop.enhance.no_guard");
        for (FormattedCharSequence line : font.split(gl, tw)) {
            g.drawString(font, line, tx, ty, guard != Enhance.Guard.NONE ? 0xFF1F6F8B : l.destroy > 0 ? Ui.C_RED : Ui.C_INK_SOFT, false);
            ty += 10;
        }
        boolean ok = menu.getSlot(2).hasItem() && menu.getSlot(2).mayPickup(player);
        Component hint = Component.translatable(ok ? "rtshop.enhance.click_hint" : "rtshop.enhance.missing");
        List<FormattedCharSequence> hl = font.split(hint, tw);
        int hy = y + h - 10 - hl.size() * 10;
        if (hy > ty) {
            for (FormattedCharSequence line : hl) {
                g.drawString(font, line, tx, hy, ok ? Ui.C_GREEN : Ui.C_RED, false);
                hy += 10;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (showOdds) {
            showOdds = false;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    public void showOddsForTest(boolean on) {
        showOdds = on;
    }

    /** 시험용: 오른쪽 패널이 화면 안에 들어오는가. */
    public boolean fitsForTest() {
        return leftPos >= 0 && leftPos + imageWidth + 6 + PANEL_W <= width;
    }
}

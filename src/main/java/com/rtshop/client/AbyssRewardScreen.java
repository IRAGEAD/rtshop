package com.rtshop.client;

import com.google.gson.JsonObject;
import com.rtshop.raid.AbyssWeekly;
import com.rtshop.raid.Reward;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;

/** Spiral Abyss 주간 보상 자세히 보기: 층마다 보상 아이템(확률), Gold, 받았는지. 닫으면 층 선택 화면으로 돌아간다. */
public class AbyssRewardScreen extends Screen {
    private final Screen parent;
    private final long openedAt = System.currentTimeMillis();
    private static final int W = 330, ROW = 28;

    public AbyssRewardScreen(Screen parent) {
        super(Component.literal("abyss rewards"));
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int h() {
        return 50 + AbyssWeekly.FLOORS * ROW + 28;
    }

    private int x0() {
        return (this.width - W) / 2;
    }

    private int y0() {
        return Math.max(8, (this.height - h()) / 2);
    }

    private int[] closeBtn() {
        return new int[]{x0() + W / 2 - 50, y0() + h() - 22, 100, 15};
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        JsonObject o = RaidClient.abyss;
        g.fill(0, 0, this.width, this.height, 0xC0050310);
        if (o == null) {
            RaidText.center(g, this.font, "…", RaidText.SERIF, this.width / 2, this.height / 2, 0xFFE0D4FF);
            return;
        }
        float time = (System.currentTimeMillis() - openedAt) / 1000F;
        int x = x0(), y = y0(), h = h();
        AbyssPanel.frame(g, x, y, W, h, time);
        // 머리
        String title = AbyssPanel.t(o, "title");
        int tw = RaidText.w(this.font, title, RaidText.TITLE);
        Fx2D.begin(g, true);
        Fx2D.glow(x + W / 2F, y + 16, tw * 0.6F, 14, Fx2D.rgba(AbyssPanel.VIOLET, 0.35F), Fx2D.rgba(AbyssPanel.VIOLET, 0));
        Fx2D.end();
        RaidText.outlined(g, this.font, title, RaidText.TITLE, x + W / 2 - tw / 2, y + 9, RaidFx.GOLD_HI, 0xB0100820);
        RaidText.center(g, this.font, AbyssPanel.reset(o), RaidText.SANS, x + W / 2, y + 28, 0xFFB8A8E0);
        RaidFx.hairline(g, x + 16, x + W - 16, y + 40, 0xD4A85A, 0.8F);
        Fx2D.begin(g, false);
        Fx2D.diamond(x + W / 2F, y + 40.5F, 3F, 3F, 0, RaidFx.GOLD_MID);
        Fx2D.end();
        // 층별 보상
        List<Reward> all = AbyssPanel.rewards(o);
        ItemStack hover = ItemStack.EMPTY;
        double hoverChance = 100;
        for (int f = 1; f <= AbyssWeekly.FLOORS; f++) {
            int ry = y + 48 + (f - 1) * ROW;
            boolean done = AbyssPanel.done(o, f);
            Fx2D.begin(g, false);
            int band = done ? 0x40D4A85A : 0x40201846;
            Fx2D.rect(x + 10, ry, x + W - 10, ry + ROW - 4, band, 0x10000000, 0x10000000, band);
            Fx2D.end();
            g.fill(x + 10, ry + ROW - 4, x + W - 10, ry + ROW - 3, 0x307C64E0);
            AbyssPanel.star(g, x + 24, ry + 12, 7F, done, time, f);
            RaidText.draw(g, this.font, "제 " + AbyssPanel.ROMAN[f - 1] + " 층", RaidText.SERIF, x + 38, ry + 8, done ? RaidFx.GOLD_HI : 0xFFE0D4FF);
            int ix = x + 92;
            for (Reward r : all) {
                if (!r.group.equals("floor" + f) || ix > x + W - 96) continue;
                ItemStack s = r.stack();
                g.fill(ix - 1, ry + 3, ix + 17, ry + 21, 0x50000000);
                RaidFx.box(g, ix - 2, ry + 2, ix + 18, ry + 22, done ? 0x80D4A85A : 0x807C64E0);
                g.renderItem(s, ix, ry + 4);
                g.renderItemDecorations(this.font, s, ix, ry + 4);
                if (r.chance < 99.99) {
                    String c = (r.chance < 1 ? String.format(java.util.Locale.ROOT, "%.2f", r.chance) : Long.toString(Math.round(r.chance))) + "%";
                    g.pose().pushPose();
                    g.pose().translate(ix + 8, ry - 3, 300);
                    g.pose().scale(0.65F, 0.65F, 1F);
                    RaidText.outlined(g, this.font, c, RaidText.SANS, -RaidText.w(this.font, c, RaidText.SANS) / 2, 0, 0xFFFFE08A, 0xC0000000);
                    g.pose().popPose();
                }
                if (Ui.inside(mx, my, ix, ry + 4, 16, 16)) {
                    hover = s;
                    hoverChance = r.chance;
                }
                ix += 24;
            }
            long gv = AbyssPanel.gold(o, f);
            String right = done ? "받음" : gv > 0 ? Ui.num(gv) + " G" : "";
            g.pose().pushPose();
            g.pose().translate(0, 0, 300);
            if (done) {
                RaidFx.badge(g, x + W - 52, ry + 12, true);
                RaidText.draw(g, this.font, right, RaidText.SERIF, x + W - 44, ry + 8, 0xFF9FE8A8);
            } else if (!right.isEmpty()) {
                Fx2D.begin(g, false);
                Fx2D.ring(x + W - 18 - RaidText.w(this.font, right, RaidText.SERIF) - 8, ry + 12, 0, 3.6F, 0xFFFFE08A, 0xFFB07A20);
                Fx2D.end();
                RaidText.right(g, this.font, right, RaidText.SERIF, x + W - 16, ry + 8, 0xFFFFD27A);
            }
            g.pose().popPose();
        }
        int[] c = closeBtn();
        boolean hov = Ui.inside(mx, my, c[0], c[1], c[2], c[3]);
        g.fill(c[0], c[1], c[0] + c[2], c[1] + c[3], hov ? 0xC0402C88 : 0x90201846);
        RaidFx.box(g, c[0], c[1], c[0] + c[2], c[1] + c[3], hov ? RaidFx.GOLD_HI : 0xA0D4A85A);
        RaidText.center(g, this.font, "닫기", RaidText.SERIF, c[0] + c[2] / 2, c[1] + 4, hov ? 0xFFFFF4DC : 0xFFE0D4FF);
        if (!hover.isEmpty()) {
            List<Component> tip = new ArrayList<>(hover.getTooltipLines(Minecraft.getInstance().player, TooltipFlag.Default.NORMAL));
            if (hoverChance < 99.99) {
                tip.add(Component.translatable("rtshop.abyss.chance", Math.round(hoverChance * 100) / 100.0).withStyle(ChatFormatting.GOLD));
            }
            g.pose().pushPose();
            g.pose().translate(0, 0, 400);
            g.renderComponentTooltip(this.font, tip, mx, my);
            g.pose().popPose();
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int[] c = closeBtn();
        if (Ui.inside(mx, my, c[0], c[1], c[2], c[3])) {
            onClose();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}

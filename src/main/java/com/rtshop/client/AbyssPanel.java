package com.rtshop.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rtshop.net.RaidActionPacket;
import com.rtshop.net.ShopNet;
import com.rtshop.raid.AbyssWeekly;
import com.rtshop.raid.Reward;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;

/**
 * Spiral Abyss 층 선택 화면의 왼쪽 아래에 그리는 "주간 보상" 창: 층마다 이번 주 보상(아이템·Gold)을 보여 주고, 받은 층에는 ✔ 를 붙인다.
 */
public final class AbyssPanel {
    private AbyssPanel() {
    }

    public static final int W = 168, ROW = 17, HEAD = 24;
    private static final String[] ROMAN = {"I", "II", "III", "IV", "V", "VI"};

    public static boolean isFloorScreen(Screen s) {
        return s != null && s.getClass().getName().equals("com.pla.spiral_abyss.client.screen.FloorSelectionScreen");
    }

    public static void request() {
        ShopNet.CHANNEL.sendToServer(new RaidActionPacket(AbyssWeekly.ACT_VIEW, ""));
    }

    public static int height() {
        return HEAD + AbyssWeekly.FLOORS * ROW + 6;
    }

    private static String t(JsonObject o, String k) {
        JsonObject t = o.getAsJsonObject("texts");
        return t.has(k) ? t.get(k).getAsString() : k;
    }

    /** 화면 왼쪽 아래에 그린다. 받은 정보가 없으면 그리지 않는다. */
    public static void render(GuiGraphics g, int sw, int sh, int mx, int my) {
        JsonObject o = RaidClient.abyss;
        if (o == null || !o.get("enabled").getAsBoolean()) return;
        Font font = Minecraft.getInstance().font;
        int h = height(), x = 6, y = sh - h - 6;
        g.pose().pushPose();
        g.pose().translate(0, 0, 350);
        g.fill(x - 1, y - 1, x + W + 1, y + h + 1, 0xFF000000);
        g.fill(x, y, x + W, y + h, 0xE61A1422);
        g.fill(x, y, x + W, y + 1, 0xFFB48AE8);
        g.drawString(font, t(o, "title"), x + 6, y + 5, 0xFFE8D8FF, true);
        long ms = o.get("resetIn").getAsLong();
        String reset;
        try {
            reset = String.format(t(o, "reset"), ms / 86_400_000L, (ms / 3_600_000L) % 24);
        } catch (Exception e) {
            reset = "";
        }
        g.pose().pushPose();
        g.pose().translate(x + 6, y + 15, 0);
        g.pose().scale(0.75F, 0.75F, 1F);
        g.drawString(font, reset, 0, 0, 0xFFA898C0, false);
        g.pose().popPose();
        long mask = o.get("mask").getAsLong();
        JsonArray gold = o.getAsJsonArray("gold");
        List<Reward> all = new ArrayList<>();
        for (JsonElement e : o.getAsJsonArray("rewards")) all.add(Reward.parse(e.getAsJsonObject()));
        ItemStack hover = ItemStack.EMPTY;
        double hoverChance = 100;
        for (int f = 1; f <= AbyssWeekly.FLOORS; f++) {
            int ry = y + HEAD + (f - 1) * ROW;
            boolean done = (mask & (1L << (f - 1))) != 0;
            g.fill(x + 3, ry, x + W - 3, ry + ROW - 1, done ? 0x5530A050 : 0x55000000);
            g.drawString(font, ROMAN[f - 1], x + 7, ry + 4, done ? 0xFF8FE39A : 0xFFE8D8FF, false);
            int ix = x + 28;
            for (Reward r : all) {
                if (!r.group.equals("floor" + f) || ix > x + W - 70) continue;
                ItemStack s = r.stack();
                g.renderItem(s, ix, ry);
                g.renderItemDecorations(font, s, ix, ry);
                if (r.chance < 99.99) {
                    g.pose().pushPose();
                    g.pose().translate(ix, ry, 300);
                    g.pose().scale(0.5F, 0.5F, 1F);
                    g.drawString(font, (r.chance < 1 ? String.format(java.util.Locale.ROOT, "%.2f", r.chance) : Long.toString(Math.round(r.chance))) + "%", 0, 0, 0xFFFFE08A, true);
                    g.pose().popPose();
                }
                if (Ui.inside(mx, my, ix, ry, 16, 16)) {
                    hover = s;
                    hoverChance = r.chance;
                }
                ix += 18;
            }
            long gv = f - 1 < gold.size() ? gold.get(f - 1).getAsLong() : 0;
            String right = done ? "✔ " + t(o, "done") : gv > 0 ? Ui.compact(gv) + " G" : "";
            g.pose().pushPose();
            g.pose().translate(0, 0, 300);
            g.drawString(font, right, x + W - 6 - font.width(right), ry + 4, done ? 0xFF8FE39A : 0xFFE8B84A, false);
            if (done) {
                g.fill(x + 26, ry, x + W - 62, ry + ROW - 1, 0x66101010);
            }
            g.pose().popPose();
        }
        g.pose().popPose();
        if (!hover.isEmpty()) {
            Minecraft mc = Minecraft.getInstance();
            List<Component> tip = new ArrayList<>(hover.getTooltipLines(mc.player, TooltipFlag.Default.NORMAL));
            if (hoverChance < 99.99) {
                tip.add(Component.translatable("rtshop.abyss.chance", Math.round(hoverChance * 100) / 100.0).withStyle(ChatFormatting.GOLD));
            }
            g.pose().pushPose();
            g.pose().translate(0, 0, 400);
            g.renderComponentTooltip(font, tip, mx, my);
            g.pose().popPose();
        }
    }
}

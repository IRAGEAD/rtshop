package com.rtshop.client;

import com.rtshop.enhance.Enhance;
import com.rtshop.enhance.EnhanceConfig;
import com.rtshop.enhance.EnhanceMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 강화 정보 창. 장비에 마우스를 올려도 설명 창은 뜨지 않고, 그 내용이 모두 여기에 들어 있다.
 *  - 강화 보기: 장비 이름·단계, 능력치(지금 → 다음: 보너스 %, 실제 공격 피해/방어력), 확률, 재료, Gold, 보호 상태.
 *  - 상세 보기([상세]): 장비 설명 전체 (예전 설명 창의 글줄 그대로, 마우스 휠로 넘김).
 * 자리·크기는 client.json (게임 안에서는 창 아래 [⚙]). 창을 끄면 [⚙ 강화 정보] 단추만 남는다.
 */
public final class EnhancePanel {
    private EnhancePanel() {
    }

    public static final int W = 168, H = 196, BTN = 16;
    /** 단추 종류. */
    public static final int B_ODDS = 0, B_SETTINGS = 1, B_EDIT = 2, B_SHOW = 3, B_DETAIL = 4;

    private static boolean detail;
    private static int detailScroll;
    private static ItemStack cachedFor = ItemStack.EMPTY;
    private static long cachedAt;
    private static List<FormattedCharSequence> cachedLines = List.of();

    public static boolean detailForTest() {
        return detail;
    }

    public static void setDetailForTest(boolean on) {
        detail = on;
        detailScroll = 0;
    }

    public static int detailLinesForTest() {
        return cachedLines.size();
    }

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

    /** 단추들: {x, y, w, 종류}. [확률표] [상세] (OP: [편집]) [⚙]. */
    public static List<int[]> buttons(int[] r) {
        List<int[]> out = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        int by = r[1] + r[3] + 3;
        if (!ClientConfig.get().enhPanelEnabled) {
            int w = Math.min(r[2], mc.font.width(label(B_SHOW, false)) + 12);
            out.add(new int[]{r[0] + r[2] - w, r[1], w, B_SHOW});
            return out;
        }
        boolean op = mc.player != null && mc.player.hasPermissions(2);
        int gear = 18, gap = 2;
        int n = op ? 3 : 2;
        int rest = r[2] - gear - gap * n;
        int bw = rest / n;
        int x = r[0];
        out.add(new int[]{x, by, bw, B_ODDS});
        x += bw + gap;
        out.add(new int[]{x, by, bw, B_DETAIL});
        x += bw + gap;
        if (op) {
            out.add(new int[]{x, by, r[0] + r[2] - gear - gap - x, B_EDIT});
        }
        out.add(new int[]{r[0] + r[2] - gear, by, gear, B_SETTINGS});
        return out;
    }

    static String label(int kind, boolean tableOpen) {
        return switch (kind) {
            case B_ODDS -> Component.translatable(tableOpen ? "rtshop.enhodds.hide" : "rtshop.enhodds.open").getString();
            case B_SETTINGS -> "⚙";
            case B_EDIT -> Component.translatable("rtshop.enhodds.edit_short").getString();
            case B_DETAIL -> Component.translatable(detail ? "rtshop.enhui.detail_back" : "rtshop.enhui.detail").getString();
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
            draw(g, font, menu, mc.player, 0, 0, W, H);
            g.pose().popPose();
        }
        for (int[] b : buttons(r)) {
            Ui.button(g, font, b[0], b[1], b[2], BTN, Ui.clip(font, label(b[3], tableOpen), b[2] - 4),
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
        if (kind == B_SETTINGS) {
            mc.setScreen(new EnhanceUiScreen(from));
        } else if (kind == B_EDIT) {
            mc.player.closeContainer();
            com.rtshop.net.ShopNet.CHANNEL.sendToServer(new com.rtshop.net.EnhanceActionPacket(com.rtshop.net.EnhanceActionPacket.OPEN_EDITOR, ""));
        } else if (kind == B_SHOW) {
            ClientConfig.get().enhPanelEnabled = true;
            ClientConfig.get().save();
        } else if (kind == B_DETAIL) {
            detail = !detail;
            detailScroll = 0;
        }
    }

    /** 마우스 휠: 상세 보기에서 글을 넘긴다. 창 위에서 굴렸으면 true. */
    public static boolean scroll(int[] r, double mx, double my, double delta) {
        if (!ClientConfig.get().enhPanelEnabled || !detail || !Ui.inside(mx, my, r[0], r[1], r[2], r[3])) return false;
        detailScroll -= (int) Math.signum(delta) * 2;
        return true;
    }

    // ------------------------------------------------------------------ 그리기

    public static void draw(GuiGraphics g, Font font, EnhanceMenu menu, Player player, int x, int y, int w, int h) {
        Ui.woodFrame(g, x, y, w, h);
        Ui.bevel(g, x + 5, y + 5, w - 10, h - 10, Ui.C_PARCH, Ui.C_PARCH_HI, Ui.C_PARCH_SHADE);
        ItemStack eq = menu.equipment();
        if (detail) {
            drawDetail(g, font, eq, x, y, w, h);
        } else {
            drawMain(g, font, menu, player, eq, x, y, w, h);
        }
    }

    private static void help(GuiGraphics g, Font font, int tx, int ty, int tw, int bottom, String... keys) {
        for (String k : keys) {
            for (FormattedCharSequence line : font.split(Component.translatable(k), tw)) {
                if (ty > bottom) return;
                g.drawString(font, line, tx, ty, Ui.C_INK_SOFT, false);
                ty += 10;
            }
            ty += 3;
        }
    }

    /** 장비 이름 줄: 아이콘 + 이름, 오른쪽에 단계. 다음 줄의 y 를 돌려준다. */
    private static int header(GuiGraphics g, Font font, ItemStack eq, String right, int tx, int ty, int tw) {
        g.renderItem(eq, tx - 1, ty - 2);
        int rw = font.width(right);
        g.drawString(font, right, tx + tw - rw, ty + 3, 0xFF8A5A00, false);
        String name = Ui.clip(font, eq.getHoverName().getString(), tw - 20 - rw - 4);
        g.drawString(font, name, tx + 18, ty + 3, Ui.C_INK, false);
        return ty + 18;
    }

    private static void drawMain(GuiGraphics g, Font font, EnhanceMenu menu, Player player, ItemStack eq, int x, int y, int w, int h) {
        int tx = x + 10, ty = y + 10, tw = w - 20;
        EnhanceConfig cfg = EnhanceConfig.client();
        if (eq.isEmpty() || !Enhance.enhanceable(eq)) {
            g.drawString(font, Component.translatable("rtshop.enhance.panel"), tx, ty, Ui.C_INK, false);
            help(g, font, tx, ty + 13, tw, y + h - 24, "rtshop.enhance.help1", "rtshop.enhance.help2", "rtshop.enhance.help3", "rtshop.enhance.help4");
            return;
        }
        int cur = Enhance.level(eq);
        EnhanceConfig.Level l = cfg.level(cur + 1);
        ty = header(g, font, eq, l == null ? "+" + cur + " MAX" : "+" + cur + " → +" + (cur + 1), tx, ty, tw);
        // 능력치: 지금 → 다음
        for (Stat st : stats(eq, cur, l != null, cfg)) {
            g.drawString(font, Ui.clip(font, st.label, tw - font.width(st.now + st.next) - 4), tx, ty, Ui.C_INK_SOFT, false);
            int vx = tx + tw - font.width(st.now + st.next);
            g.drawString(font, st.now, vx, ty, Ui.C_INK, false);
            if (!st.next.isEmpty()) g.drawString(font, st.next, vx + font.width(st.now), ty, 0xFF1E7A2E, false);
            ty += 10;
        }
        ty += 3;
        g.fill(tx, ty - 2, tx + tw, ty - 1, Ui.C_PARCH_SHADE);
        if (l == null) {
            help(g, font, tx, ty + 2, tw, y + h - 14, "rtshop.enhance.max_note");
            return;
        }
        int mask = Enhance.activeGuard(player);
        Enhance.Guard guard = Enhance.fromMask(mask);
        double[] e = EnhanceClient.effective(l, guard, cur);
        String[] keys = {"rtshop.enhance.odds_success", "rtshop.enhance.odds_maintain", "rtshop.enhance.odds_damage", "rtshop.enhance.odds_destroy"};
        int[] cols = {Ui.C_GREEN, 0xFF4A4A4A, 0xFFB86A10, Ui.C_RED};
        ty += 1;
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
        boolean ok = menu.getSlot(2).hasItem() && menu.getSlot(2).mayPickup(player);
        Component hint = Component.translatable(ok ? "rtshop.enhance.click_hint" : "rtshop.enhance.missing");
        List<FormattedCharSequence> hl = font.split(hint, tw);
        int hy = y + h - 10 - hl.size() * 10;
        Component gl = guard != Enhance.Guard.NONE ? Component.translatable("rtshop.enhance.guard_active", Enhance.guardName(mask))
                : Component.translatable(l.destroy > 0 ? "rtshop.enhance.destroy_warn" : "rtshop.enhance.no_guard");
        for (FormattedCharSequence line : font.split(gl, tw)) {
            if (ty + 9 > hy) break;
            g.drawString(font, line, tx, ty, guard != Enhance.Guard.NONE ? 0xFF1F6F8B : l.destroy > 0 ? Ui.C_RED : Ui.C_INK_SOFT, false);
            ty += 10;
        }
        if (hy >= ty) {
            for (FormattedCharSequence line : hl) {
                g.drawString(font, line, tx, hy, ok ? Ui.C_GREEN : Ui.C_RED, false);
                hy += 10;
            }
        }
    }

    private static void drawDetail(GuiGraphics g, Font font, ItemStack eq, int x, int y, int w, int h) {
        int tx = x + 10, ty = y + 10, tw = w - 20;
        g.drawString(font, Component.translatable("rtshop.enhui.detail_title"), tx, ty, Ui.C_INK, false);
        int bx0 = x + 8, by0 = y + 22, bx1 = x + w - 8, by1 = y + h - 8;
        // 설명 창과 같은 어두운 바탕 (글자 색이 어두운 바탕용이라서)
        g.fill(bx0, by0, bx1, by1, 0xF0100010);
        g.fill(bx0, by0, bx1, by0 + 1, 0xFF5000FF);
        g.fill(bx0, by1 - 1, bx1, by1, 0xFF28007F);
        g.fill(bx0, by0, bx0 + 1, by1, 0xFF5000FF);
        g.fill(bx1 - 1, by0, bx1, by1, 0xFF28007F);
        if (eq.isEmpty()) {
            cachedLines = List.of();
            int yy = by0 + 6;
            for (FormattedCharSequence line : font.split(Component.translatable("rtshop.enhui.detail_empty"), tw - 10)) {
                g.drawString(font, line, bx0 + 5, yy, 0xFFB0B0B0, false);
                yy += 10;
            }
            return;
        }
        long now = System.currentTimeMillis();
        if (now - cachedAt > 700 || !ItemStack.matches(cachedFor, eq)) {
            List<FormattedCharSequence> lines = new ArrayList<>();
            for (Component c : EnhanceClient.describe(eq)) {
                List<FormattedCharSequence> part = font.split(c, tw - 12);
                if (part.isEmpty()) lines.add(FormattedCharSequence.EMPTY);
                else lines.addAll(part);
            }
            cachedLines = lines;
            cachedFor = eq.copy();
            cachedAt = now;
        }
        int rows = (by1 - by0 - 8) / 10;
        int max = Math.max(0, cachedLines.size() - rows);
        detailScroll = Math.max(0, Math.min(max, detailScroll));
        int yy = by0 + 5;
        for (int i = detailScroll; i < cachedLines.size() && i < detailScroll + rows; i++) {
            g.drawString(font, cachedLines.get(i), bx0 + 5, yy, 0xFFFFFFFF, true);
            yy += 10;
        }
        if (max > 0) {
            int trackH = by1 - by0 - 4;
            int barH = Math.max(10, trackH * rows / cachedLines.size());
            int barY = by0 + 2 + (trackH - barH) * detailScroll / max;
            g.fill(bx1 - 3, barY, bx1 - 1, barY + barH, 0xFFB0A0FF);
            String more = Component.translatable("rtshop.enhui.detail_scroll").getString();
            g.drawString(font, more, x + w - 10 - font.width(more), ty, Ui.C_INK_SOFT, false);
        }
    }

    // ------------------------------------------------------------------ 능력치 (지금 → 다음)

    /** label: 이름, now: 지금 값, next: " → 다음 값" (최고 단계면 빈 글). */
    public record Stat(String label, String now, String next) {
    }

    private static double total(ItemStack s, EquipmentSlot slot, Attribute a, double base) {
        double v = base;
        for (AttributeModifier m : s.getAttributeModifiers(slot).get(a)) {
            if (m.getOperation() == AttributeModifier.Operation.ADDITION) v += m.getAmount();
        }
        return v;
    }

    private static String num(double v) {
        double r = Math.round(v * 10) / 10.0;
        return r == Math.floor(r) ? Long.toString((long) r) : String.format(java.util.Locale.ROOT, "%.1f", r);
    }

    private static String arrow(String next) {
        return " → " + next;
    }

    /** 강화로 달라지는 수치: 보너스 %, 그리고 그 장비의 실제 공격 피해 / 방어력 / 방어 강도. */
    public static List<Stat> stats(ItemStack eq, int cur, boolean hasNext, EnhanceConfig cfg) {
        List<Stat> out = new ArrayList<>();
        ItemStack a = eq.copy(), b = eq.copy();
        Enhance.clearPreview(a);
        Enhance.clearPreview(b);
        Enhance.setLevel(a, cur);
        Enhance.setLevel(b, cur + 1);
        boolean armor = Enhance.kind(eq) == Enhance.Kind.ARMOR;
        double[] arr = armor ? cfg.armorBonus : cfg.weaponBonus;
        String pNow = "+" + EnhanceClient.pct(arr[Math.min(cur, arr.length - 1)]);
        String pNext = hasNext ? arrow("+" + EnhanceClient.pct(arr[Math.min(cur + 1, arr.length - 1)])) : "";
        out.add(new Stat(Component.translatable(armor ? "rtshop.enhui.stat_armor_pct" : "rtshop.enhui.stat_attack_pct").getString(), pNow, pNext));
        try {
            if (armor) {
                EquipmentSlot slot = LivingEntity.getEquipmentSlotForItem(eq);
                double n0 = total(a, slot, Attributes.ARMOR, 0), n1 = total(b, slot, Attributes.ARMOR, 0);
                out.add(new Stat(Component.translatable("rtshop.enhui.stat_armor").getString(), num(n0), hasNext ? arrow(num(n1)) : ""));
                double t0 = total(a, slot, Attributes.ARMOR_TOUGHNESS, 0), t1 = total(b, slot, Attributes.ARMOR_TOUGHNESS, 0);
                if (t0 > 0 || (hasNext && t1 > 0)) {
                    out.add(new Stat(Component.translatable("rtshop.enhui.stat_toughness").getString(), num(t0), hasNext ? arrow(num(t1)) : ""));
                }
            } else {
                double d0 = total(a, EquipmentSlot.MAINHAND, Attributes.ATTACK_DAMAGE, 1), d1 = total(b, EquipmentSlot.MAINHAND, Attributes.ATTACK_DAMAGE, 1);
                out.add(new Stat(Component.translatable("rtshop.enhui.stat_attack").getString(), num(d0), hasNext ? arrow(num(d1)) : ""));
            }
        } catch (Exception ignored) {
            // 다른 모드의 특이한 장비: 보너스 % 만 보여 준다
        }
        return out;
    }
}

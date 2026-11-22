package com.rtshop.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rtshop.net.RaidActionPacket;
import com.rtshop.net.ShopNet;
import com.rtshop.raid.Raid;
import com.rtshop.raid.RaidConfig;
import com.rtshop.raid.Reward;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 레이드 안내인 창: 짧은 대화(3줄) → 불꽃 테마의 입장 화면(조건·보상 목록) → "정말 입장하시겠습니까?" 확인.
 */
public class RaidScreen extends Screen {
    private static final int W = 400, H = 226;
    public static final int[] GRADE_COLORS = {0xFFB8B8B8, 0xFF55C24A, 0xFF4A9BE8, 0xFFB45AE8, 0xFFF0A030, 0xFF101010};
    public static final String[] GRADE_KEYS = {"rtshop.gacha.grade.common", "rtshop.gacha.grade.uncommon", "rtshop.gacha.grade.rare",
            "rtshop.gacha.grade.epic", "rtshop.gacha.grade.legendary", ""};

    private final JsonObject data;
    /** 0~2 대화, 3 입장 화면. */
    private int phase = 0;
    private boolean confirm = false;
    private int scroll = 0;
    private final long openedAt = System.currentTimeMillis();

    private record Row(int kind, String text, int color, ItemStack stack, String right) {
    }

    private final List<Row> rows = new ArrayList<>();
    /** 입장 화면에 띄울 보스 (게임 속 실제 모델). 만들 수 없으면 null. */
    private net.minecraft.world.entity.LivingEntity bossEntity;
    private boolean bossTried = false;
    private boolean bossDrawn = false;

    private net.minecraft.world.entity.LivingEntity bossEntity() {
        if (!bossTried) {
            bossTried = true;
            try {
                net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.tryParse(data.get("boss").getAsString());
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (rl != null && mc.level != null && net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.containsKey(rl)
                        && net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(rl).create(mc.level) instanceof net.minecraft.world.entity.LivingEntity le) {
                    le.setNoGravity(true);
                    le.setSilent(true);
                    bossEntity = le;
                }
            } catch (Throwable t) {
                bossEntity = null;
            }
        }
        return bossEntity;
    }

    /** 보스를 불빛 속에 크게 그린다 (천천히 좌우로 몸을 돌린다). */
    private void drawBoss(GuiGraphics g, int cx, int feetY, int maxH, int maxW) {
        net.minecraft.world.entity.LivingEntity e = bossEntity();
        if (e == null) return;
        float time = (System.currentTimeMillis() - openedAt) / 1000F;
        float eh = Math.max(0.6F, e.getBbHeight()), ew = Math.max(0.6F, e.getBbWidth());
        int scale = (int) Math.max(8, Math.min(maxH / eh, maxW / (ew * 1.25F)));
        Fx2D.begin(g, true);
        Fx2D.glow(cx, feetY - maxH * 0.45F, maxW * 0.62F, maxH * 0.62F, 0x70FF5A14, 0x00FF5A14);
        Fx2D.glow(cx, feetY, maxW * 0.55F, 9, 0x90FFB040, 0x00FF7A20);
        Fx2D.end();
        float yaw = 180F + (float) Math.sin(time * 0.6F) * 28F;
        float oBody = e.yBodyRot, oY = e.getYRot(), oX = e.getXRot(), oHeadO = e.yHeadRotO, oHead = e.yHeadRot;
        try {
            e.yBodyRot = yaw;
            e.setYRot(yaw);
            e.setXRot(0);
            e.yHeadRot = yaw;
            e.yHeadRotO = yaw;
            e.tickCount = (int) (time * 20);
            org.joml.Quaternionf pose = new org.joml.Quaternionf().rotateZ((float) Math.PI);
            org.joml.Quaternionf cam = new org.joml.Quaternionf().rotateX(-0.08F);
            pose.mul(cam);
            net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventory(g, cx, feetY, scale, pose, cam, e);
            bossDrawn = true;
        } catch (Throwable t) {
            bossEntity = null; // 그릴 수 없는 보스면 그림 없이 간다
            g.flush();
        } finally {
            if (bossEntity != null) {
                e.yBodyRot = oBody;
                e.setYRot(oY);
                e.setXRot(oX);
                e.yHeadRot = oHead;
                e.yHeadRotO = oHeadO;
            }
        }
    }

    public boolean bossDrawnForTest() {
        return bossDrawn;
    }

    public RaidScreen(JsonObject data) {
        super(Component.literal("raid"));
        this.data = data;
        buildRows();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private String t(String k) {
        JsonObject t = data.getAsJsonObject("texts");
        return t.has(k) ? t.get(k).getAsString() : k;
    }

    public static String gradeName(int g) {
        return g >= 5 ? "[???]" : Component.translatable(GRADE_KEYS[g]).getString();
    }

    private static String pct(double v) {
        return (Math.abs(v - Math.rint(v)) < 1e-9 ? Long.toString((long) Math.rint(v)) : Double.toString(v)) + "%";
    }

    private void buildRows() {
        rows.clear();
        boolean claimed = data.get("weeklyClaimed").getAsBoolean();
        List<Reward> all = new ArrayList<>();
        for (JsonElement e : data.getAsJsonArray("rewards")) all.add(Reward.parse(e.getAsJsonObject()));
        rows.add(new Row(0, t("rewardWeekly"), 0xFFE8B84A, ItemStack.EMPTY, claimed ? "✔" : ""));
        long gold = data.get("weeklyGold").getAsLong();
        if (gold > 0) {
            rows.add(new Row(2, Ui.num(gold) + " G", 0xFFE8B84A, ItemStack.EMPTY, claimed ? "✔" : ""));
        }
        for (Reward r : all) {
            if (!r.group.equals(RaidConfig.G_WEEKLY)) continue;
            ItemStack s = r.stack();
            rows.add(new Row(1, s.getHoverName().getString() + (s.getCount() > 1 ? " ×" + s.getCount() : ""), 0xFFF5E6C0, s, claimed ? "✔" : ""));
        }
        JsonArray ch = data.getAsJsonArray(claimed ? "chanceRepeat" : "chanceFresh");
        int rolls = data.get(claimed ? "rollsRepeat" : "rollsFresh").getAsInt();
        rows.add(new Row(0, t("rewardDrops") + "  ×" + rolls, 0xFFE8B84A, ItemStack.EMPTY, ""));
        for (int g = 5; g >= 0; g--) {
            boolean any = false;
            for (Reward r : all) {
                if (r.group.equals(RaidConfig.G_DROP) && r.grade == g) any = true;
            }
            if (!any) continue;
            rows.add(new Row(3, gradeName(g), GRADE_COLORS[g], ItemStack.EMPTY, pct(ch.get(g).getAsDouble())));
            for (Reward r : all) {
                if (!r.group.equals(RaidConfig.G_DROP) || r.grade != g) continue;
                ItemStack s = r.stack();
                if (g == 5) {
                    rows.add(new Row(1, "???", 0xFF9A9A9A, ItemStack.EMPTY, ""));
                } else {
                    rows.add(new Row(1, s.getHoverName().getString() + (s.getCount() > 1 ? " ×" + s.getCount() : ""), 0xFFF5E6C0, s, ""));
                }
            }
        }
    }

    private int fx() {
        return (this.width - W) / 2;
    }

    private int fy() {
        return (this.height - H) / 2;
    }

    private int[] enterBtn() {
        return new int[]{fx() + 16, fy() + H - 30, 150, 20};
    }

    private int[] closeBtn() {
        return new int[]{fx() + W - 76, fy() + H - 30, 60, 20};
    }

    private int[] yesBtn() {
        return new int[]{this.width / 2 - 112, this.height / 2 + 20, 106, 20};
    }

    private int[] noBtn() {
        return new int[]{this.width / 2 + 6, this.height / 2 + 20, 106, 20};
    }

    private int listX() {
        return fx() + 196;
    }

    private int listY() {
        return fy() + 58;
    }

    private int listW() {
        return W - 196 - 14;
    }

    private int listH() {
        return H - 58 - 40;
    }

    private int maxScroll() {
        return Math.max(0, rows.size() * 14 - listH());
    }

    private void embers(GuiGraphics g, int x, int y, int w, int h) {
        float time = (System.currentTimeMillis() - openedAt) / 1000F;
        Fx2D.begin(g, true);
        Fx2D.glow(x + w / 2F, y + h + 30, w * 0.75F, 150, 0x66FF4A10, 0x00FF4A10);
        Fx2D.glow(x + 96, y + 78, 90, 60, 0x40FF7A20, 0x00FF7A20);
        for (int i = 0; i < 46; i++) {
            float sp = 0.10F + Fx2D.hash(i, 3) * 0.16F;
            float ph = (time * sp + Fx2D.hash(i, 5)) % 1F;
            float px = x + Fx2D.hash(i, 1) * w + (float) Math.sin(time * 1.3F + i) * 5F;
            float py = y + h - ph * h;
            float a = (1 - ph) * 0.8F;
            int c = Fx2D.rgba(i % 3 == 0 ? 0xFFD060 : 0xFF6A20, a);
            Fx2D.glow(px, py, 1.6F + Fx2D.hash(i, 7) * 2F, c, Fx2D.rgba(0xFF3000, 0));
        }
        Fx2D.end();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        if (phase < 3) {
            renderDialogue(g);
            return;
        }
        int x = fx(), y = fy();
        g.fill(0, 0, this.width, this.height, 0x99000000);
        g.fill(x - 3, y - 3, x + W + 3, y + H + 3, 0xFF000000);
        g.fill(x - 2, y - 2, x + W + 2, y + H + 2, 0xFF6A1A0C);
        g.fill(x, y, x + W, y + H, 0xFF120606);
        embers(g, x, y, W, H);
        g.fill(x, y, x + W, y + 2, 0xFFFF7A20);
        g.fill(x, y + H - 2, x + W, y + H, 0xFF6A1A0C);
        // 제목
        g.pose().pushPose();
        g.pose().translate(x + 16, y + 12, 0);
        g.pose().scale(1.7F, 1.7F, 1F);
        g.drawString(this.font, t("uiTitle"), 0, 0, 0xFFFFB040, true);
        g.pose().popPose();
        g.drawString(this.font, t("uiSub"), x + 17, y + 31, 0xFFD06030, false);
        g.fill(x + 14, y + 44, x + W - 14, y + 45, 0xFF6A1A0C);
        // 왼쪽 아래: 보스의 모습
        g.enableScissor(x + 2, y + 46, x + 192, y + H - 2);
        drawBoss(g, x + 98, y + H - 36, 92, 150);
        g.disableScissor();
        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        // 왼쪽: 조건과 상태
        int ly = y + 52;
        int need = data.get("needKills").getAsInt(), kills = data.get("kills").getAsInt();
        if (need > 0) {
            boolean ok = kills >= need;
            line(g, x + 16, ly, (ok ? "✔ " : "✖ ") + Component.translatable("rtshop.raid.ui_kills", kills, need).getString(), ok ? 0xFF8FE39A : 0xFFFF7060);
            ly += 12;
        }
        int tickets = data.get("tickets").getAsInt();
        line(g, x + 16, ly, (tickets > 0 ? "✔ " : "✖ ") + Component.translatable("rtshop.raid.ui_tickets", tickets).getString(), tickets > 0 ? 0xFF8FE39A : 0xFFFF7060);
        ly += 12;
        int st = data.get("state").getAsInt();
        line(g, x + 16, ly, Component.translatable(st == Raid.IDLE ? "rtshop.raid.ui_open" : "rtshop.raid.ui_locked", data.get("inside").getAsInt()).getString(),
                st == Raid.IDLE ? 0xFFF5E6C0 : 0xFFFF7060);
        ly += 12;
        line(g, x + 16, ly, Component.translatable("rtshop.raid.ui_power", pctMult(data.get("health").getAsDouble()), pctMult(data.get("damage").getAsDouble())).getString(), 0xFFD9B0A8);
        ly += 12;
        long ms = data.get("resetIn").getAsLong();
        line(g, x + 16, ly, Component.translatable("rtshop.raid.ui_reset", ms / 86_400_000L, (ms / 3_600_000L) % 24).getString(), 0xFFB8AF9C);
        String why = data.get("why").getAsString();
        if (!why.isEmpty()) {
            List<FormattedCharSequence> wl = this.font.split(Component.literal(why), 168);
            int wy = y + H - 36 - wl.size() * 10;
            g.fill(x + 12, wy - 3, x + 190, y + H - 34, 0xB0000000);
            for (FormattedCharSequence l : wl) {
                g.drawString(this.font, l, x + 16, wy, 0xFFFF9080, false);
                wy += 10;
            }
        }
        g.pose().popPose();
        // 오른쪽: 보상 목록
        int lx = listX(), lt = listY(), lw = listW(), lh = listH();
        g.fill(lx - 4, lt - 4, lx + lw + 4, lt + lh + 4, 0xAA000000);
        g.enableScissor(lx - 4, lt - 2, lx + lw + 4, lt + lh + 2);
        int ry = lt - scroll;
        for (Row r : rows) {
            if (ry > lt - 16 && ry < lt + lh + 2) {
                if (r.kind == 0) {
                    g.drawString(this.font, r.text, lx, ry + 3, r.color, false);
                } else if (r.kind == 3) {
                    if (r.color == GRADE_COLORS[5]) {
                        g.fill(lx + 4, ry + 1, lx + 10 + this.font.width(r.text), ry + 12, 0xFF8A8A8A);
                    }
                    g.drawString(this.font, r.text, lx + 7, ry + 3, r.color, false);
                } else {
                    if (!r.stack.isEmpty()) {
                        g.pose().pushPose();
                        g.pose().translate(lx + 12, ry, 0);
                        g.pose().scale(0.75F, 0.75F, 1F);
                        g.renderItem(r.stack, 0, 0);
                        g.pose().popPose();
                    }
                    g.drawString(this.font, Ui.clip(this.font, r.text, lw - 44), lx + 28, ry + 3, r.color, false);
                }
                if (!r.right.isEmpty()) {
                    g.drawString(this.font, r.right, lx + lw - this.font.width(r.right) - 2, ry + 3, r.right.equals("✔") ? 0xFF8FE39A : 0xFFD9C592, false);
                }
            }
            ry += 14;
        }
        g.disableScissor();
        // 버튼
        boolean can = data.get("canEnter").getAsBoolean();
        int[] e = enterBtn(), c = closeBtn();
        boolean hov = !confirm && can && Ui.inside(mx, my, e[0], e[1], e[2], e[3]);
        g.fill(e[0] - 1, e[1] - 1, e[0] + e[2] + 1, e[1] + e[3] + 1, 0xFF000000);
        g.fill(e[0], e[1], e[0] + e[2], e[1] + e[3], !can ? 0xFF2A1A16 : hov ? 0xFFD0481C : 0xFF9A2A12);
        g.fill(e[0], e[1], e[0] + e[2], e[1] + 1, can ? 0xFFFFA050 : 0xFF4A3028);
        g.drawCenteredString(this.font, t("enterButton"), e[0] + e[2] / 2, e[1] + 6, can ? 0xFFFFF0D0 : 0xFF7A6A60);
        Ui.button(g, this.font, c[0], c[1], c[2], c[3], Component.translatable("gui.cancel").getString(), !confirm && Ui.inside(mx, my, c[0], c[1], c[2], c[3]), true);
        if (confirm) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 300);
            g.fill(0, 0, this.width, this.height, 0xAA000000);
            int bw = 260, bh = 96, bx = (this.width - bw) / 2, by = this.height / 2 - 50;
            g.fill(bx - 2, by - 2, bx + bw + 2, by + bh + 2, 0xFFFF7A20);
            g.fill(bx, by, bx + bw, by + bh, 0xFF160808);
            g.drawCenteredString(this.font, t("confirmTitle"), this.width / 2, by + 10, 0xFFFFB040);
            int ty = by + 28;
            for (FormattedCharSequence l : this.font.split(Component.literal(t("confirmBody")), bw - 24)) {
                g.drawCenteredString(this.font, l, this.width / 2, ty, 0xFFF5E6C0);
                ty += 10;
            }
            int[] yb = yesBtn(), nb = noBtn();
            Ui.button(g, this.font, yb[0], yb[1], yb[2], yb[3], t("confirmYes"), Ui.inside(mx, my, yb[0], yb[1], yb[2], yb[3]), true);
            Ui.button(g, this.font, nb[0], nb[1], nb[2], nb[3], t("confirmNo"), Ui.inside(mx, my, nb[0], nb[1], nb[2], nb[3]), true);
            g.pose().popPose();
        }
    }

    private static String pctMult(double m) {
        return Long.toString(Math.round(m * 100));
    }

    private void line(GuiGraphics g, int x, int y, String s, int color) {
        g.drawString(this.font, Ui.clip(this.font, s, 172), x, y, color, false);
    }

    private void renderDialogue(GuiGraphics g) {
        int bw = Math.min(420, this.width - 40), bh = 62, bx = (this.width - bw) / 2, by = this.height - bh - 34;
        g.fill(bx - 2, by - 2, bx + bw + 2, by + bh + 2, 0xFF6A1A0C);
        g.fill(bx, by, bx + bw, by + bh, 0xF0120606);
        g.fill(bx + 8, by - 8, bx + 16 + this.font.width(t("npcName")), by + 6, 0xFF9A2A12);
        g.drawString(this.font, t("npcName"), bx + 12, by - 5, 0xFFFFF0D0, false);
        String full = t("dialogue" + (phase + 1));
        int shown = (int) Math.min(full.length(), (System.currentTimeMillis() - lineAt) / 22);
        int ty = by + 12;
        for (FormattedCharSequence l : this.font.split(Component.literal(full.substring(0, shown)), bw - 24)) {
            g.drawString(this.font, l, bx + 12, ty, 0xFFF5E6C0, false);
            ty += 11;
        }
        if (shown >= full.length() && (System.currentTimeMillis() / 400) % 2 == 0) {
            g.drawString(this.font, "▶", bx + bw - 16, by + bh - 13, 0xFFFFB040, false);
        }
    }

    private long lineAt = System.currentTimeMillis();

    private void advance() {
        String full = t("dialogue" + (phase + 1));
        if ((System.currentTimeMillis() - lineAt) / 22 < full.length()) {
            lineAt = 0; // 글자가 다 나오지 않았으면 먼저 다 보여 준다
            return;
        }
        phase++;
        lineAt = System.currentTimeMillis();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (phase < 3) {
            advance();
            return true;
        }
        if (confirm) {
            int[] yb = yesBtn(), nb = noBtn();
            if (Ui.inside(mx, my, yb[0], yb[1], yb[2], yb[3])) {
                enter();
            } else if (Ui.inside(mx, my, nb[0], nb[1], nb[2], nb[3])) {
                confirm = false;
            }
            return true;
        }
        int[] e = enterBtn(), c = closeBtn();
        if (data.get("canEnter").getAsBoolean() && Ui.inside(mx, my, e[0], e[1], e[2], e[3])) {
            confirm = true;
            return true;
        }
        if (Ui.inside(mx, my, c[0], c[1], c[2], c[3])) {
            this.onClose();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (phase < 3 && (key == 32 || key == 257 || key == 335)) {
            advance();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) (delta * 14)));
        return true;
    }

    private void enter() {
        ShopNet.CHANNEL.sendToServer(new RaidActionPacket(Raid.ACT_ENTER, ""));
        this.onClose();
    }

    // ------------------------------------------------------------------ 점검용

    public int phaseForTest() {
        return phase;
    }

    public void phaseForTest(int p, boolean confirmOpen) {
        phase = p;
        confirm = confirmOpen;
        lineAt = 0;
    }

    public int rowsForTest() {
        return rows.size();
    }

    public boolean canEnterForTest() {
        return data.get("canEnter").getAsBoolean();
    }

    public void enterForTest() {
        enter();
    }
}

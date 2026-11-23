package com.rtshop.client;

import com.rtshop.net.QuestHudPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * HUD 를 끌어서 옮기고 크기를 바꾸는 화면 (Gold 표시, 퀘스트 HUD, PvP 표시). 닫을 때 자동 저장된다.
 * 원하는 HUD 를 눌러 고른 뒤 끌거나 아래 버튼·마우스 휠·방향키로 조절한다.
 */
public class HudEditScreen extends Screen {
    public static final int GOLD = 0;
    public static final int QUEST = 1;
    public static final int PVP = 2;

    private int selected = GOLD;
    private boolean dragging = false;
    private double grabX, grabY;
    private ChoicePopup popup;

    public HudEditScreen() {
        super(Component.translatable("rtshop.hud.title"));
        HudFonts.refresh();
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

    private int row2Y() {
        return btnY() - 22;
    }

    /** 두 번째 줄 버튼들 {x, w} : 보임/숨김, (퀘스트) 보일 개수, 좁게, 넓게. */
    private int[][] row2() {
        if (selected != QUEST) {
            return new int[][]{{this.width / 2 - 60, 120}};
        }
        int total = 120 + 4 + 96 + 4 + 50 + 4 + 50;
        int x = this.width / 2 - total / 2;
        return new int[][]{{x, 120}, {x + 124, 96}, {x + 224, 50}, {x + 278, 50}};
    }

    private int row3Y() {
        return row2Y() - 22;
    }

    /** 세 번째 줄 버튼들 {x, w} : 글꼴, 글자 크기, (퀘스트·PvP HUD) 언어. */
    private int[][] row3() {
        if (selected == GOLD) {
            int total = 160 + 4 + 120;
            int x = this.width / 2 - total / 2;
            return new int[][]{{x, 160}, {x + 164, 120}};
        }
        int total = 150 + 4 + 110 + 4 + 120;
        int x = this.width / 2 - total / 2;
        return new int[][]{{x, 150}, {x + 154, 110}, {x + 268, 120}};
    }

    // ---- 맨 위 줄: RT Shop 창 글꼴과 글자 크기 (HUD 와 따로)
    private int uiRowY() {
        return 66;
    }

    /** 맨 위 줄: 창 글꼴, 창 글자 크기, 스토리 대사 언어. */
    private int[][] uiRow() {
        int total = 130 + 4 + 110 + 4 + 150;
        int x = this.width / 2 - total / 2;
        return new int[][]{{x, 130}, {x + 134, 110}, {x + 248, 150}};
    }

    private String uiRowLabel(int i) {
        ClientConfig cfg = ClientConfig.get();
        if (i == 0) {
            String face = "serif".equals(cfg.uiFont) ? "serif" : "pixel".equals(cfg.uiFont) ? "pixel" : "sans";
            return Ui.clip(this.font, Component.translatable("rtshop.hud.ui_font", Component.translatable("rtshop.hud.ui_font." + face)).getString(), 130 - 18) + " ▼";
        }
        if (i == 2) {
            return Ui.clip(this.font, Component.translatable("rtshop.hud.story_lang", HudLang.label(cfg.storyLang)).getString(), 150 - 18) + " ▼";
        }
        return Ui.clip(this.font, Component.translatable("rtshop.hud.ui_size", Component.translatable("rtshop.hud.ui_size." + cfg.uiFontSize)).getString(), 110 - 18) + " ▼";
    }

    private void uiRowClick(int i) {
        ClientConfig cfg = ClientConfig.get();
        int[][] r = uiRow();
        List<ChoicePopup.Option> opts = new ArrayList<>();
        if (i == 2) {
            for (String l : HudLang.LANGS) opts.add(new ChoicePopup.Option(l.isEmpty() ? "auto" : l, HudLang.label(l)));
            popup = new ChoicePopup(r[2][0], uiRowY(), r[2][1], 18, this.height, this.font, opts, cfg.storyLang.isEmpty() ? "auto" : cfg.storyLang, v -> {
                cfg.storyLang = HudLang.normalize(v);
                cfg.save();
                HudLang.sendStoryLang();
            });
            return;
        }
        if (i == 0) {
            for (String f : UiFont.FACES) opts.add(new ChoicePopup.Option(f, Component.translatable("rtshop.hud.ui_font." + f).getString()));
            popup = new ChoicePopup(r[0][0], uiRowY(), r[0][1], 18, this.height, this.font, opts, cfg.uiFont, v -> {
                cfg.uiFont = v;
                cfg.save();
                UiFont.changed();
            });
            return;
        }
        for (int n = 0; n < 3; n++) opts.add(new ChoicePopup.Option(Integer.toString(n), Component.translatable("rtshop.hud.ui_size." + n).getString()));
        popup = new ChoicePopup(r[1][0], uiRowY(), r[1][1], 18, this.height, this.font, opts, Integer.toString(cfg.uiFontSize), v -> {
            cfg.uiFontSize = Integer.parseInt(v);
            cfg.save();
            UiFont.changed();
        });
    }

    private String row3Label(int i) {
        ClientConfig cfg = ClientConfig.get();
        if (i == 0) {
            String f = selected == GOLD ? cfg.hudFont : selected == PVP ? cfg.pvpHudFont : cfg.questHudFont;
            return Ui.clip(this.font, Component.translatable("rtshop.hud.font", HudFonts.label(HudFonts.effective(f))).getString(), r3w(0) - 18) + " ▼";
        }
        if (i == 2) {
            String l = selected == PVP ? cfg.pvpHudLang : cfg.questHudLang;
            return Ui.clip(this.font, Component.translatable("rtshop.hud.lang", HudLang.label(l)).getString(), 120 - 18) + " ▼";
        }
        double pct = (selected == GOLD ? cfg.hudTextScale : selected == PVP ? cfg.pvpHudTextScale : cfg.questHudScale) * 100;
        return Ui.clip(this.font, Component.translatable("rtshop.hud.text_size", Math.round(pct)).getString(), r3w(1) - 18) + " ▼";
    }

    private int r3w(int i) {
        return row3()[i][1];
    }

    private void row3Click(int i) {
        ClientConfig cfg = ClientConfig.get();
        int[][] r3 = row3();
        List<ChoicePopup.Option> opts = new ArrayList<>();
        if (i == 2) {
            for (String l : HudLang.LANGS) opts.add(new ChoicePopup.Option(l.isEmpty() ? "auto" : l, HudLang.label(l)));
            String cur = selected == PVP ? cfg.pvpHudLang : cfg.questHudLang;
            popup = new ChoicePopup(r3[2][0], row3Y(), r3[2][1], 18, this.height, this.font, opts, cur.isEmpty() ? "auto" : cur, v -> {
                if (selected == PVP) cfg.pvpHudLang = HudLang.normalize(v);
                else cfg.questHudLang = HudLang.normalize(v);
            });
            return;
        }
        if (i == 0) {
            for (String f : HudFonts.available()) {
                opts.add(new ChoicePopup.Option(f, HudFonts.label(f)));
            }
            String cur = HudFonts.effective(selected == GOLD ? cfg.hudFont : selected == PVP ? cfg.pvpHudFont : cfg.questHudFont);
            popup = new ChoicePopup(r3[0][0], row3Y(), r3[0][1], 18, this.height, this.font, opts, cur, v -> {
                if (selected == GOLD) {
                    cfg.hudFont = v;
                } else if (selected == PVP) {
                    cfg.pvpHudFont = v;
                } else {
                    cfg.questHudFont = v;
                }
            });
            return;
        }
        int[] sizes = selected != QUEST ? new int[]{50, 60, 70, 80, 90, 100, 110, 120, 130, 150}
                : new int[]{50, 60, 70, 80, 90, 100, 110, 125, 150, 175, 200, 250};
        for (int n : sizes) {
            opts.add(new ChoicePopup.Option(Integer.toString(n), n + "%"));
        }
        double now = (selected == GOLD ? cfg.hudTextScale : selected == PVP ? cfg.pvpHudTextScale : cfg.questHudScale) * 100;
        String cur = Integer.toString((int) Math.round(now));
        popup = new ChoicePopup(r3[1][0], row3Y(), r3[1][1], 18, this.height, this.font, opts, cur, v -> {
            double sc = Integer.parseInt(v) / 100.0;
            if (selected == GOLD) {
                cfg.hudTextScale = sc;
                cfg.clamp();
            } else if (selected == PVP) {
                cfg.pvpHudTextScale = sc;
                cfg.clampPvp();
            } else {
                // 글자 크기만 바꾼다 (너비는 그대로)
                cfg.questHudScale = sc;
                cfg.clampQuest();
            }
        });
    }

    private int[] goldBounds() {
        return GoldHud.bounds(this.width, this.height);
    }

    private int[] questBounds() {
        return QuestHud.bounds(this.width, this.height, QuestHud.previewEntries());
    }

    private int[] pvpBounds() {
        return PvpHud.bounds(this.width, this.height);
    }

    private static String nameKey(int which) {
        return which == GOLD ? "rtshop.hud.name_gold" : which == PVP ? "rtshop.hud.name_pvp" : "rtshop.hud.name_quest";
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        g.fill(0, 0, this.width, this.height, 0x66000000);
        ClientConfig cfg = ClientConfig.get();
        int[] gb = goldBounds();
        int[] qb = questBounds();
        frame(g, gb, selected == GOLD, Ui.inside(mx, my, gb[0], gb[1], gb[2], gb[3]), cfg.hudEnabled);
        GoldHud.draw(g, gb[0], gb[1], gb[2], gb[3], ClientEconomy.wallet);
        frame(g, qb, selected == QUEST, Ui.inside(mx, my, qb[0], qb[1], qb[2], qb[3]), cfg.questHudEnabled);
        QuestHud.draw(g, this.width, this.height, QuestHud.previewEntries());
        int[] pb = pvpBounds();
        frame(g, pb, selected == PVP, Ui.inside(mx, my, pb[0], pb[1], pb[2], pb[3]), cfg.pvpHudEnabled);
        // 미리보기: 지금 내 상태 그대로 (서버가 PvP 표시를 보내지 않았으면 '꺼짐 · 안전 구역' 견본)
        PvpHud.draw(g, pb[0], pb[1], pb[2], pb[3], PvpHud.on, PvpHud.systemOn ? PvpHud.area : com.rtshop.pvp.Pvp.AREA_SAFE, 0, -1);
        if (!cfg.pvpHudEnabled) {
            hiddenMark(g, pb);
        }
        if (!cfg.hudEnabled) {
            hiddenMark(g, gb);
        }
        if (!cfg.questHudEnabled) {
            hiddenMark(g, qb);
        }
        // HUD 글자(와 게임 채팅)가 버튼·목록 위로 비치지 않게, 나머지는 더 앞(z)에 그린다
        g.flush();
        g.pose().pushPose();
        g.pose().translate(0, 0, 250);

        g.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFF5E6C0);
        g.drawCenteredString(this.font, Component.translatable("rtshop.hud.help1"), this.width / 2, 26, 0xFFD9C592);
        g.drawCenteredString(this.font, Component.translatable("rtshop.hud.help2"), this.width / 2, 38, 0xFFD9C592);
        g.drawCenteredString(this.font, Component.translatable("rtshop.hud.selected",
                Component.translatable(nameKey(selected))), this.width / 2, 52, 0xFFE8B84A);

        String[] labels = {"rtshop.hud.smaller", "rtshop.hud.bigger", "rtshop.hud.reset", "rtshop.hud.done"};
        for (int i = 0; i < 4; i++) {
            Ui.button(g, this.font, btnX(i), btnY(), 72, 18, Component.translatable(labels[i]).getString(),
                    Ui.inside(mx, my, btnX(i), btnY(), 72, 18), true);
        }
        int[][] r2 = row2();
        for (int i = 0; i < r2.length; i++) {
            Ui.button(g, this.font, r2[i][0], row2Y(), r2[i][1], 18, row2Label(i), Ui.inside(mx, my, r2[i][0], row2Y(), r2[i][1], 18), true);
        }
        int[][] ur = uiRow();
        for (int i = 0; i < ur.length; i++) {
            Ui.button(g, this.font, ur[i][0], uiRowY(), ur[i][1], 18, uiRowLabel(i), Ui.inside(mx, my, ur[i][0], uiRowY(), ur[i][1], 18), true);
        }
        int[][] r3 = row3();
        for (int i = 0; i < r3.length; i++) {
            Ui.button(g, this.font, r3[i][0], row3Y(), r3[i][1], 18, row3Label(i), Ui.inside(mx, my, r3[i][0], row3Y(), r3[i][1], 18), true);
        }
        if (popup != null) {
            popup.render(g, this.font, mx, my);
        }
        g.pose().popPose();
    }

    private String row2Label(int i) {
        ClientConfig cfg = ClientConfig.get();
        return switch (i) {
            case 0 -> Component.translatable((selected == GOLD ? cfg.hudEnabled : selected == PVP ? cfg.pvpHudEnabled : cfg.questHudEnabled)
                    ? "rtshop.hud.shown" : "rtshop.hud.hidden").getString();
            case 1 -> Component.translatable("rtshop.hud.quest_count", cfg.questHudMax).getString() + " ▼";
            case 2 -> Component.translatable("rtshop.hud.narrower").getString();
            default -> Component.translatable("rtshop.hud.wider").getString();
        };
    }

    private static void frame(GuiGraphics g, int[] b, boolean sel, boolean hover, boolean shown) {
        int c = sel ? 0xFFFFE08A : (hover ? 0xCCFFFFFF : 0x66FFFFFF);
        int t = sel ? 2 : 1;
        g.fill(b[0] - 1 - t, b[1] - 1 - t, b[0] + b[2] + 1 + t, b[1] - 1, c);
        g.fill(b[0] - 1 - t, b[1] + b[3] + 1, b[0] + b[2] + 1 + t, b[1] + b[3] + 1 + t, c);
        g.fill(b[0] - 1 - t, b[1] - 1, b[0] - 1, b[1] + b[3] + 1, c);
        g.fill(b[0] + b[2] + 1, b[1] - 1, b[0] + b[2] + 1 + t, b[1] + b[3] + 1, c);
    }

    private void hiddenMark(GuiGraphics g, int[] b) {
        g.fill(b[0], b[1], b[0] + b[2], b[1] + b[3], 0x99000000);
        g.drawCenteredString(this.font, Component.translatable("rtshop.hud.hidden"), b[0] + b[2] / 2, b[1] + b[3] / 2 - 4, 0xFFCCCCCC);
    }

    /** 고른 HUD 의 크기를 바꾼다 (퀘스트 HUD 는 글자와 너비를 함께). */
    private void scale(double factor) {
        ClientConfig cfg = ClientConfig.get();
        if (selected == QUEST) {
            double before = cfg.questHudScale;
            cfg.questHudScale = Math.max(0.5, Math.min(2.5, cfg.questHudScale * factor));
            cfg.questHudWidth *= cfg.questHudScale / before;
            cfg.clampQuest();
            return;
        }
        if (selected == PVP) {
            double cx = cfg.pvpHudX + cfg.pvpHudWidth / 2;
            double cy = cfg.pvpHudY + cfg.pvpHudHeight / 2;
            cfg.pvpHudWidth = Math.max(0.05, Math.min(0.6, cfg.pvpHudWidth * factor));
            cfg.pvpHudHeight = Math.max(0.03, Math.min(0.4, cfg.pvpHudHeight * factor));
            cfg.pvpHudX = cx - cfg.pvpHudWidth / 2;
            cfg.pvpHudY = cy - cfg.pvpHudHeight / 2;
            cfg.clampPvp();
            return;
        }
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
        if (popup != null) {
            if (popup.click(mx, my)) {
                popup = null;
            }
            return true;
        }
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
            if (selected == GOLD) {
                cfg.resetHud();
            } else if (selected == PVP) {
                cfg.resetPvpHud();
            } else {
                cfg.resetQuestHud();
            }
            return true;
        }
        if (Ui.inside(mx, my, btnX(3), btnY(), 72, 18)) {
            this.onClose();
            return true;
        }
        int[][] r2 = row2();
        for (int i = 0; i < r2.length; i++) {
            if (Ui.inside(mx, my, r2[i][0], row2Y(), r2[i][1], 18)) {
                row2Click(i);
                return true;
            }
        }
        int[][] ur = uiRow();
        for (int i = 0; i < ur.length; i++) {
            if (Ui.inside(mx, my, ur[i][0], uiRowY(), ur[i][1], 18)) {
                uiRowClick(i);
                return true;
            }
        }
        int[][] r3 = row3();
        for (int i = 0; i < r3.length; i++) {
            if (Ui.inside(mx, my, r3[i][0], row3Y(), r3[i][1], 18)) {
                row3Click(i);
                return true;
            }
        }
        // 위에 그려진 퀘스트 HUD 부터 확인
        int[] qb = questBounds();
        int[] gb = goldBounds();
        if (Ui.inside(mx, my, qb[0], qb[1], qb[2], qb[3])) {
            select(QUEST);
            dragging = true;
            grabX = mx - qb[0];
            grabY = my - qb[1];
            return true;
        }
        int[] pb = pvpBounds();
        if (Ui.inside(mx, my, pb[0], pb[1], pb[2], pb[3])) {
            select(PVP);
            dragging = true;
            grabX = mx - pb[0];
            grabY = my - pb[1];
            return true;
        }
        if (Ui.inside(mx, my, gb[0], gb[1], gb[2], gb[3])) {
            select(GOLD);
            dragging = true;
            grabX = mx - gb[0];
            grabY = my - gb[1];
            return true;
        }
        return false;
    }

    private void row2Click(int i) {
        ClientConfig cfg = ClientConfig.get();
        switch (i) {
            case 0 -> {
                if (selected == GOLD) {
                    cfg.hudEnabled = !cfg.hudEnabled;
                } else if (selected == PVP) {
                    cfg.pvpHudEnabled = !cfg.pvpHudEnabled;
                } else {
                    cfg.questHudEnabled = !cfg.questHudEnabled;
                }
            }
            case 1 -> openCountPopup();
            case 2 -> {
                cfg.questHudWidth /= 1.1;
                cfg.clampQuest();
            }
            default -> {
                cfg.questHudWidth *= 1.1;
                cfg.clampQuest();
            }
        }
    }

    private void openCountPopup() {
        int[][] r2 = row2();
        if (r2.length < 2) {
            return;
        }
        List<ChoicePopup.Option> opts = new ArrayList<>();
        for (int n = 1; n <= 5; n++) {
            opts.add(new ChoicePopup.Option(Integer.toString(n), Component.translatable("rtshop.hud.quest_count", n).getString()));
        }
        ClientConfig cfg = ClientConfig.get();
        popup = new ChoicePopup(r2[1][0], row2Y(), r2[1][1], 18, this.height, this.font, opts, Integer.toString(cfg.questHudMax), v -> {
            cfg.questHudMax = Integer.parseInt(v);
            cfg.clampQuest();
        });
    }

    public void select(int which) {
        selected = which;
        popup = null;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging) {
            ClientConfig cfg = ClientConfig.get();
            double nx = (mx - grabX) / Math.max(1, this.width);
            double ny = (my - grabY) / Math.max(1, this.height);
            if (selected == QUEST) {
                int[] qb = questBounds();
                cfg.questHudX = nx;
                cfg.questHudY = Math.min(ny, (this.height - qb[3]) / (double) Math.max(1, this.height));
                cfg.clampQuest();
            } else if (selected == PVP) {
                cfg.pvpHudX = nx;
                cfg.pvpHudY = ny;
                cfg.clampPvp();
            } else {
                cfg.hudX = nx;
                cfg.hudY = ny;
                cfg.clamp();
            }
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
        if (popup != null) {
            popup.scroll(delta);
            return true;
        }
        scale(delta > 0 ? 1.05 : 1 / 1.05);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        ClientConfig cfg = ClientConfig.get();
        double sx = 1.0 / Math.max(1, this.width);
        double sy = 1.0 / Math.max(1, this.height);
        boolean q = selected == QUEST;
        boolean pv = selected == PVP;
        switch (key) {
            case 263 -> { // 왼쪽
                if (q) cfg.questHudX -= sx; else if (pv) cfg.pvpHudX -= sx; else cfg.hudX -= sx;
            }
            case 262 -> { // 오른쪽
                if (q) cfg.questHudX += sx; else if (pv) cfg.pvpHudX += sx; else cfg.hudX += sx;
            }
            case 265 -> { // 위
                if (q) cfg.questHudY -= sy; else if (pv) cfg.pvpHudY -= sy; else cfg.hudY -= sy;
            }
            case 264 -> { // 아래
                if (q) cfg.questHudY += sy; else if (pv) cfg.pvpHudY += sy; else cfg.hudY += sy;
            }
            case 258 -> { // Tab: 다음 HUD 고르기 (Gold → 퀘스트 → PvP)
                select(selected == GOLD ? QUEST : selected == QUEST ? PVP : GOLD);
                return true;
            }
            default -> {
                return super.keyPressed(key, scan, mods);
            }
        }
        cfg.clamp();
        cfg.clampQuest();
        cfg.clampPvp();
        return true;
    }

    @Override
    public void removed() {
        super.removed();
        ClientConfig.get().save();
    }

    // ---------------------------------------------------------------- 점검용

    public int selectedForTest() {
        return selected;
    }

    public void clickRow2ForTest(int i) {
        row2Click(i);
    }

    public void clickRow3ForTest(int i) {
        row3Click(i);
    }

    public ChoicePopup popupForTest() {
        return popup;
    }

    public List<QuestHudPacket.Entry> previewForTest() {
        return QuestHud.previewEntries();
    }
}

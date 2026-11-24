package com.rtshop.client;

import com.rtshop.net.ClosePacket;
import com.rtshop.net.QuestActionPacket;
import com.rtshop.net.QuestOpenPacket;
import com.rtshop.net.ShopNet;
import com.rtshop.quest.QuestDef;
import com.rtshop.quest.QuestView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * 퀘스트 화면. NPC 앞에서는 그 NPC 의 퀘스트(받을 수 있는 것 + 진행 중인 것), 수첩에서는 진행 중인 모든 퀘스트를 보여 준다.
 * 왼쪽 목록에서 퀘스트를 고르면 오른쪽에 이야기·목표·보상이 보이고, 아래 버튼으로 수락/완료/포기한다.
 */
public class QuestScreen extends Screen {
    private static final int W = 392;
    private static final int H = 236;
    private static final int LIST_W = 124;
    private static final int ROW_H = 24;

    private QuestOpenPacket data;
    /** 수첩: 서버가 보낸 전체 목록 (data 는 지금 탭에 맞게 거른 것). */
    private QuestOpenPacket full;
    /** 수첩 탭: 0 진행 중, 1 받을 수 있음, 2 완료 */
    private int tab = 0;
    private int selected = 0;
    private int listScroll = 0;
    private int detailScroll = 0;
    private int choice = -1;
    private int fx, fy;
    /** 상인 화면의 [퀘스트] 단추로 들어왔을 때: 돌아갈 상인의 엔티티 번호 (-1 = 돌아갈 곳 없음). */
    private int backNpc = -1;

    public QuestScreen(QuestOpenPacket data) {
        super(Component.literal(data.title));
        this.full = data;
        this.data = data;
        if (data.journal) {
            tab = count(0) > 0 ? 0 : count(1) > 0 ? 1 : 0;
            this.data = filtered();
        }
        if (!data.journal && System.currentTimeMillis() - NpcButtons.returnArmedAt < 6000) {
            backNpc = NpcButtons.returnNpc;
        }
        NpcButtons.returnArmedAt = 0;
    }

    private String backLabel() {
        return "\u25C0 " + Component.translatable("rtshop.quest.back").getString();
    }

    private String talkLabel() {
        return Component.translatable("rtshop.talk.button").getString();
    }

    private int talkW() {
        return this.font.width(talkLabel()) + 12;
    }

    private String noteLabel() {
        return Component.translatable("rtshop.notebook.title").getString();
    }

    private int noteW() {
        return this.font.width(noteLabel()) + 12;
    }

    private int noteX() {
        return fx + 10 + this.font.width(this.title.getString()) + 10;
    }

    private String arenaLabel() {
        return "\u2694 " + Component.translatable("rtshop.arena.button").getString();
    }

    private int arenaW() {
        return this.font.width(arenaLabel()) + 16;
    }

    private int backW() {
        return this.font.width(backLabel()) + 10;
    }

    public boolean hasBackForTest() {
        return backNpc >= 0;
    }

    /** 상인의 원래 화면(상점·은행 등)으로 돌아간다: 그 상인을 다시 우클릭한 것과 같다. */
    public void goBack() {
        Minecraft mc = Minecraft.getInstance();
        net.minecraft.world.entity.Entity e = mc.level == null ? null : mc.level.getEntity(backNpc);
        onClose();
        if (e != null && mc.gameMode != null && mc.player != null) {
            mc.gameMode.interact(mc.player, e, net.minecraft.world.InteractionHand.MAIN_HAND);
        }
    }

    private static int tabOf(QuestView v) {
        return switch (v.state()) {
            case QuestView.AVAILABLE -> 1;
            case QuestView.DONE -> 2;
            default -> 0;
        };
    }

    private int count(int t) {
        int n = 0;
        for (QuestView v : full.views) if (tabOf(v) == t) n++;
        return n;
    }

    private QuestOpenPacket filtered() {
        List<QuestView> l = new java.util.ArrayList<>();
        for (QuestView v : full.views) if (tabOf(v) == tab) l.add(v);
        return new QuestOpenPacket(full.npcKey, full.title, full.greeting, true, l);
    }

    private static final String[] TAB_KEYS = {"rtshop.quest.tab_active", "rtshop.quest.tab_available", "rtshop.quest.tab_done"};

    private String tabLabel(int t) {
        return Component.translatable(TAB_KEYS[t], count(t)).getString();
    }

    /** 수첩 탭의 x 위치 (오른쪽부터). */
    private int tabX(int t) {
        int x = fx + W - 8;
        for (int i = 2; i >= t; i--) x -= this.font.width(tabLabel(i)) + 12 + (i == t ? 0 : 3);
        return x;
    }

    public int tabForTest() {
        return tab;
    }

    public void tabForTest(int t) {
        tab = t;
        data = filtered();
        selected = 0;
        listScroll = 0;
        detailScroll = 0;
    }

    public int shownForTest() {
        return data.views.size();
    }

    /** 서버가 보낸 새 내용으로 바꾼다. 보고 있던 퀘스트는 그대로 유지한다. */
    public void update(QuestOpenPacket packet) {
        String keep = current() == null ? null : current().id();
        this.full = packet;
        this.data = packet.journal ? filtered() : packet;
        selected = 0;
        for (int i = 0; i < data.views.size(); i++) {
            if (data.views.get(i).id().equals(keep)) {
                selected = i;
                break;
            }
        }
        if (keep == null || current() == null || !current().id().equals(keep)) {
            choice = -1;
            detailScroll = 0;
        }
        clampList();
    }

    @Override
    protected void init() {
        fx = (this.width - W) / 2;
        fy = (this.height - H) / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        super.removed();
        ShopNet.CHANNEL.sendToServer(new ClosePacket());
    }

    private QuestView current() {
        return selected >= 0 && selected < data.views.size() ? data.views.get(selected) : null;
    }

    private int listX() {
        return fx + 8;
    }

    private int listY() {
        return fy + 30;
    }

    private int listH() {
        return H - 30 - 10;
    }

    private int visibleRows() {
        return Math.max(1, listH() / ROW_H);
    }

    private void clampList() {
        int max = Math.max(0, data.views.size() - visibleRows());
        listScroll = Math.max(0, Math.min(max, listScroll));
    }

    private int detX() {
        return fx + 8 + LIST_W + 6;
    }

    private int detW() {
        return W - 16 - LIST_W - 6;
    }

    private int detY() {
        return fy + 30;
    }

    private int detH() {
        return H - 30 - 10;
    }

    private void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    // ------------------------------------------------------------------ 글자 만들기

    private static String itemName(String id) {
        if (id.startsWith("#")) {
            return id;
        }
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        if (item == null) {
            return id;
        }
        return new ItemStack(item).getHoverName().getString();
    }

    public static String objectiveName(QuestView.ObjView o) {
        return objectiveName(o, "");
    }

    /** 목표 한 줄. lang: HUD 언어 ("" = 게임 언어) */
    public static String objectiveName(QuestView.ObjView o, String lang) {
        QuestDef.Type t = QuestDef.Type.values()[Math.max(0, Math.min(QuestDef.Type.values().length - 1, o.type()))];
        switch (t) {
            case DELIVER: {
                String name = o.text().isEmpty() ? itemName(o.target()) : o.text();
                return HudLang.tr(lang, "rtshop.quest.obj.deliver", name);
            }
            case KILL: {
                String name;
                if (!o.text().isEmpty()) {
                    name = o.text();
                } else if (o.target().equals("*")) {
                    name = HudLang.tr(lang, "rtshop.quest.any_monster");
                } else {
                    name = EntityType.byString(o.target()).map(e -> e.getDescription().getString()).orElse(o.target());
                }
                return HudLang.tr(lang, "rtshop.quest.obj.kill", name);
            }
            case VISIT:
                return HudLang.tr(lang, "rtshop.quest.obj.visit", o.text());
            case TALK:
                return HudLang.tr(lang, "rtshop.quest.obj.talk", o.text());
            case STRUCTURE:
                return HudLang.tr(lang, "rtshop.quest.obj.structure", o.text().isEmpty() ? o.target() : o.text());
            case ADVANCEMENT:
                return HudLang.tr(lang, "rtshop.quest.obj.advancement", o.text().isEmpty() ? o.target() : o.text());
            case INSPECT:
                return HudLang.tr(lang, "rtshop.quest.obj.inspect", o.text().isEmpty() ? HudLang.tr(lang, "rtshop.spot.trace") : o.text());
            case FLAG:
                return o.text().isEmpty() ? o.target() : o.text();
            case DIMENSION:
                return HudLang.tr(lang, "rtshop.quest.obj.dimension", o.text().isEmpty() ? o.target() : o.text());
            default:
                return HudLang.tr(lang, "rtshop.quest.obj.pay");
        }
    }

    private static int stateColor(int state) {
        return switch (state) {
            case QuestView.READY -> Ui.C_GREEN;
            case QuestView.ACTIVE -> Ui.C_GOLD_DARK;
            case QuestView.DONE -> Ui.C_INK_SOFT;
            default -> Ui.C_INK;
        };
    }

    private static String stateLabel(int state) {
        return Component.translatable(switch (state) {
            case QuestView.READY -> "rtshop.quest.state_ready";
            case QuestView.ACTIVE -> "rtshop.quest.state_active";
            case QuestView.DONE -> "rtshop.quest.state_done";
            default -> "rtshop.quest.state_new";
        }).getString();
    }

    // ------------------------------------------------------------------ 그리기

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        this.renderBackground(g);
        Ui.woodFrame(g, fx, fy, W, H);
        Ui.titleBand(g, fx + 5, fy + 4, fx + W - 5, fy + 23);
        g.fill(fx + 5, fy + 22, fx + W - 5, fy + 23, Ui.C_GOLD_DARK);
        int side = Math.max(backNpc >= 0 ? backW() + 14 : 20, data.journal ? 20 : talkW() + 14);
        if (data.journal) {
            // 수첩: 제목은 왼쪽, 오른쪽에 탭 (진행 중 / 받을 수 있음 / 완료)
            int tx0 = tabX(0);
            String t = Ui.clip(this.font, this.title.getString(), tx0 - fx - 18);
            g.drawString(this.font, t, fx + 10, fy + 9, Ui.C_GOLD, true);
            // [조사 수첩]: 살펴본 흔적 다시 읽기
            int nx = noteX(), nw = noteW();
            if (nx + nw + 6 <= tx0) Ui.button(g, this.font, nx, fy + 6, nw, 14, noteLabel(), Ui.inside(mx, my, nx, fy + 6, nw, 14), true);
            for (int i = 0; i < 3; i++) {
                int tx = tabX(i), tw = this.font.width(tabLabel(i)) + 12;
                boolean on = i == tab, hov = Ui.inside(mx, my, tx, fy + 6, tw, 14);
                Ui.bevel(g, tx, fy + 6, tw, 14, on ? Ui.C_GOLD : hov ? Ui.C_GOLD_DARK : Ui.C_WOOD, on ? 0xFFFFE9A8 : Ui.C_WOOD_HI, Ui.C_SHADOW);
                g.drawString(this.font, tabLabel(i), tx + 6, fy + 9, on ? Ui.C_INK : Ui.C_CREAM, false);
            }
        } else {
            String t = Ui.clip(this.font, this.title.getString(), W - 2 * side);
            g.drawString(this.font, t, fx + (W - this.font.width(t)) / 2, fy + 9, Ui.C_GOLD, true);
        }
        if (backNpc >= 0) {
            int bw = backW();
            boolean hov = Ui.inside(mx, my, fx + 8, fy + 6, bw, 14);
            Ui.bevel(g, fx + 8, fy + 6, bw, 14, hov ? Ui.C_GOLD : Ui.C_GOLD_DARK, Ui.C_GOLD, Ui.C_SHADOW);
            g.drawString(this.font, backLabel(), fx + 13, fy + 9, Ui.C_INK, false);
        }
        if (!data.journal) {
            // [대화]: 이 NPC 와 이야기 (일상·세계관)
            int tw = talkW();
            boolean hov = Ui.inside(mx, my, fx + W - 8 - tw, fy + 6, tw, 14);
            Ui.button(g, this.font, fx + W - 8 - tw, fy + 6, tw, 14, talkLabel(), hov, true);
        }

        renderList(g, mx, my);
        renderDetail(g, mx, my);
    }

    private void renderList(GuiGraphics g, int mx, int my) {
        int x = listX();
        int y = listY();
        Ui.bevel(g, x, y, LIST_W, listH(), Ui.C_PARCH_DARK, Ui.C_PARCH_SHADE, Ui.C_SHADOW);
        if (data.views.isEmpty()) {
            return;
        }
        clampList();
        int rows = visibleRows();
        for (int r = 0; r < rows; r++) {
            int i = listScroll + r;
            if (i >= data.views.size()) {
                break;
            }
            QuestView v = data.views.get(i);
            int ry = y + 2 + r * ROW_H;
            boolean sel = i == selected;
            boolean hover = Ui.inside(mx, my, x + 2, ry, LIST_W - 4, ROW_H - 2);
            Ui.bevel(g, x + 2, ry, LIST_W - 4, ROW_H - 2,
                    sel ? Ui.C_PARCH_HI : (hover ? Ui.C_PARCH : Ui.C_PARCH_DARK),
                    sel ? Ui.C_GOLD : Ui.C_PARCH_HI, sel ? Ui.C_GOLD_DARK : Ui.C_PARCH_SHADE);
            g.drawString(this.font, Ui.clip(this.font, v.title(), LIST_W - 14), x + 6, ry + 3, Ui.C_INK, false);
            g.drawString(this.font, stateLabel(v.state()), x + 6, ry + 13, stateColor(v.state()), false);
        }
        if (data.views.size() > rows) {
            int total = data.views.size();
            int barH = Math.max(10, listH() * rows / total);
            int barY = y + (listH() - barH) * listScroll / Math.max(1, total - rows);
            g.fill(x + LIST_W - 3, barY, x + LIST_W - 1, barY + barH, Ui.C_WOOD_HI);
        }
    }

    private List<FormattedCharSequence> wrap(String s, int width) {
        return this.font.split(Component.literal(s), Math.max(20, width));
    }

    private void renderDetail(GuiGraphics g, int mx, int my) {
        int x = detX();
        int y = detY();
        int w = detW();
        int h = detH();
        Ui.bevel(g, x, y, w, h, Ui.C_PARCH, Ui.C_PARCH_HI, Ui.C_PARCH_SHADE);
        QuestView v = current();
        if (v == null) {
            String msg = data.views.isEmpty() ? Component.translatable(data.journal
                    ? "rtshop.quest.empty_journal" : "rtshop.quest.empty_npc").getString() : "";
            int yy = y + 12;
            if (!data.greeting.isEmpty()) {
                for (FormattedCharSequence line : wrap(untr(data.greeting), w - 20)) {
                    g.drawString(this.font, line, x + 10, yy, Ui.C_INK, false);
                    yy += 11;
                }
                yy += 6;
            }
            for (FormattedCharSequence line : wrap(msg, w - 20)) {
                g.drawString(this.font, line, x + 10, yy, Ui.C_INK_SOFT, false);
                yy += 11;
            }
            return;
        }

        // 본문 영역(스크롤 가능)과 아래 버튼 영역을 나눈다.
        int footer = footerHeight(v);
        int bodyTop = y + 4;
        int bodyBottom = y + h - footer - 2;
        g.enableScissor(x + 1, bodyTop, x + w - 1, bodyBottom);
        int yy = bodyTop - detailScroll;
        Ui.scaledText(g, this.font, Ui.clip(this.font, v.title(), (int) ((w - 20) / 1.2F)), x + 8, yy + 2, 1.2F, Ui.C_INK, false);
        yy += 16;
        if (!v.chain().isEmpty()) {
            g.drawString(this.font, Ui.clip(this.font, v.chain(), w - 20), x + 8, yy, Ui.C_INK_SOFT, false);
            yy += 12;
        }
        yy += 2;
        boolean prevTip = false;
        for (QuestText.Line tl : QuestText.lines(v.text())) {
            if (tl.tip() && !prevTip) {
                yy += 3;
            }
            int indent = tl.tip() ? 4 : 0;
            List<FormattedCharSequence> parts = this.font.split(QuestText.component(tl), Math.max(20, w - 20 - indent));
            if (parts.isEmpty()) {
                yy += 11;
            }
            for (FormattedCharSequence line : parts) {
                if (tl.tip()) {
                    g.fill(x + 6, yy - 1, x + 7, yy + 10, QuestText.TIP_HEAD | 0x66000000);
                }
                g.drawString(this.font, line, x + 8 + indent, yy, Ui.C_INK, false);
                yy += 11;
            }
            prevTip = tl.tip();
        }
        yy += 6;
        if (!v.objectives().isEmpty()) {
            g.drawString(this.font, Component.translatable("rtshop.quest.objectives"), x + 8, yy, Ui.C_GOLD_DARK, false);
            yy += 12;
            for (QuestView.ObjView o : v.objectives()) {
                boolean done = o.cur() >= o.need();
                String label = objectiveName(o);
                if (o.need() > 1 && o.type() != QuestDef.Type.PAY.ordinal()) {
                    label += " (" + Math.min(o.cur(), o.need()) + "/" + o.need() + ")";
                } else if (o.type() == QuestDef.Type.PAY.ordinal()) {
                    label = Component.translatable("rtshop.quest.obj.pay_amount", Ui.num(o.need())).getString();
                }
                int color = v.state() == QuestView.AVAILABLE ? Ui.C_INK : (done ? Ui.C_GREEN : Ui.C_INK);
                String mark = v.state() == QuestView.AVAILABLE ? "- " : (done ? "[x] " : "[ ] ");
                for (FormattedCharSequence line : wrap(mark + label, w - 24)) {
                    g.drawString(this.font, line, x + 12, yy, color, false);
                    yy += 11;
                }
            }
            yy += 4;
        }
        if (v.rewardGold() > 0 || !v.rewardItems().isEmpty()) {
            g.drawString(this.font, Component.translatable("rtshop.quest.rewards"), x + 8, yy, Ui.C_GOLD_DARK, false);
            yy += 12;
            if (v.rewardGold() > 0) {
                Ui.coin(g, x + 12, yy - 1, 10);
                g.drawString(this.font, Ui.num(v.rewardGold()) + " G", x + 26, yy, Ui.C_INK, false);
                yy += 12;
            }
            for (QuestView.RewardItem r : v.rewardItems()) {
                g.drawString(this.font, Ui.clip(this.font, itemName(r.item()) + " x" + r.count(), w - 28), x + 12, yy, Ui.C_INK, false);
                yy += 12;
            }
        }
        int contentEnd = yy + detailScroll;
        g.disableScissor();
        int visible = bodyBottom - bodyTop;
        maxDetailScroll = Math.max(0, contentEnd - bodyTop - visible + 4);
        detailScroll = Math.max(0, Math.min(maxDetailScroll, detailScroll));

        renderFooter(g, v, x, y + h - footer, w, mx, my);
    }

    private int maxDetailScroll = 0;

    private int footerHeight(QuestView v) {
        int rows = 0;
        if (v.state() == QuestView.READY && !v.choices().isEmpty()) {
            rows = v.choices().size();
        }
        return 26 + rows * 14 + (rows > 0 ? 14 : 0);
    }

    // 버튼 위치 (그리기와 클릭이 같은 값을 쓰도록 계산 함수로 둔다)
    private int btnY(QuestView v) {
        return detY() + detH() - 22;
    }

    private int choiceY(QuestView v, int i) {
        int footer = footerHeight(v);
        return detY() + detH() - footer + 14 + i * 14;
    }

    private void renderFooter(GuiGraphics g, QuestView v, int x, int top, int w, int mx, int my) {
        g.fill(x + 4, top - 1, x + w - 4, top, Ui.C_PARCH_SHADE);
        if (v.state() == QuestView.READY && !v.choices().isEmpty()) {
            g.drawString(this.font, Component.translatable("rtshop.quest.choose"), x + 8, top + 3, Ui.C_GOLD_DARK, false);
            for (int i = 0; i < v.choices().size(); i++) {
                int cy = choiceY(v, i);
                boolean sel = i == choice;
                boolean hover = Ui.inside(mx, my, x + 8, cy, w - 16, 13);
                if (hover || sel) {
                    g.fill(x + 8, cy, x + w - 8, cy + 13, sel ? 0x33A87A22 : 0x18000000);
                }
                g.drawString(this.font, (sel ? "(o) " : "( ) ") + Ui.clip(this.font, v.choices().get(i), w - 48),
                        x + 12, cy + 2, Ui.C_INK, false);
            }
        }
        int by = btnY(v);
        if (!v.arena().isEmpty()) {
            // 기억의 문: 이 보스는 나(와 파티)만 들어가는 공간에서 싸운다
            int aw = arenaW();
            Ui.button(g, this.font, x + w - 8 - aw, by, aw, 18, arenaLabel(), Ui.inside(mx, my, x + w - 8 - aw, by, aw, 18), true);
            if (data.journal) return;
            Ui.button(g, this.font, x + 8, by, 72, 18, Component.translatable("rtshop.quest.abandon").getString(),
                    Ui.inside(mx, my, x + 8, by, 72, 18), true);
            return;
        }
        if (data.journal) {
            // 수첩에서는 내용만 본다: 받기·완료·포기는 그 사람에게 직접 가서
            String key = switch (v.state()) {
                case QuestView.AVAILABLE -> "rtshop.quest.where_accept";
                case QuestView.READY -> "rtshop.quest.where_turnin";
                case QuestView.DONE -> "rtshop.quest.where_done";
                default -> "rtshop.quest.where_active";
            };
            String who = v.npc().isEmpty() ? "?" : v.npc();
            String msg = Ui.clip(this.font, Component.translatable(key, who).getString(), w - 16);
            g.drawString(this.font, msg, x + 8, by + 5, v.state() == QuestView.READY ? Ui.C_GREEN : Ui.C_INK_SOFT, false);
            return;
        }
        if (v.state() == QuestView.AVAILABLE) {
            Ui.button(g, this.font, x + w - 96, by, 88, 18, Component.translatable("rtshop.quest.accept").getString(),
                    Ui.inside(mx, my, x + w - 96, by, 88, 18), true);
        } else {
            boolean ready = v.state() == QuestView.READY;
            boolean needChoice = ready && !v.choices().isEmpty() && choice < 0;
            if (!data.journal) {
                Ui.button(g, this.font, x + w - 96, by, 88, 18, Component.translatable("rtshop.quest.complete").getString(),
                        Ui.inside(mx, my, x + w - 96, by, 88, 18), ready && !needChoice);
            }
            Ui.button(g, this.font, x + 8, by, 72, 18, Component.translatable("rtshop.quest.abandon").getString(),
                    Ui.inside(mx, my, x + 8, by, 72, 18), true);
        }
    }

    // ------------------------------------------------------------------ 입력

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) {
            return super.mouseClicked(mx, my, button);
        }
        if (backNpc >= 0 && Ui.inside(mx, my, fx + 8, fy + 6, backW(), 14)) {
            click();
            goBack();
            return true;
        }
        if (data.journal && noteX() + noteW() + 6 <= tabX(0) && Ui.inside(mx, my, noteX(), fy + 6, noteW(), 14)) {
            click();
            if (Minecraft.getInstance().player != null) Minecraft.getInstance().player.connection.sendCommand("rtshop notebook");
            return true;
        }
        if (data.journal) {
            for (int i = 0; i < 3; i++) {
                if (Ui.inside(mx, my, tabX(i), fy + 6, this.font.width(tabLabel(i)) + 12, 14)) {
                    if (i != tab) {
                        click();
                        tabForTest(i);
                    }
                    return true;
                }
            }
        }
        if (!data.journal && Ui.inside(mx, my, fx + W - 8 - talkW(), fy + 6, talkW(), 14)) {
            click();
            onClose();
            if (Minecraft.getInstance().player != null) Minecraft.getInstance().player.connection.sendCommand("rtshop talk here");
            return true;
        }
        // 목록
        int x = listX();
        int y = listY();
        if (Ui.inside(mx, my, x + 2, y + 2, LIST_W - 4, listH() - 4)) {
            int r = (int) ((my - (y + 2)) / ROW_H);
            int i = listScroll + r;
            if (r >= 0 && r < visibleRows() && i < data.views.size()) {
                if (i != selected) {
                    selected = i;
                    choice = -1;
                    detailScroll = 0;
                }
                click();
                return true;
            }
        }
        QuestView v = current();
        if (v == null) {
            return super.mouseClicked(mx, my, button);
        }
        int dx = detX();
        int dw = detW();
        // 선택지
        if (v.state() == QuestView.READY && !v.choices().isEmpty()) {
            for (int i = 0; i < v.choices().size(); i++) {
                if (Ui.inside(mx, my, dx + 8, choiceY(v, i), dw - 16, 13)) {
                    choice = i;
                    click();
                    return true;
                }
            }
        }
        int by = btnY(v);
        if (!v.arena().isEmpty() && Ui.inside(mx, my, dx + dw - 8 - arenaW(), by, arenaW(), 18)) {
            click();
            onClose();
            if (Minecraft.getInstance().player != null) Minecraft.getInstance().player.connection.sendCommand("rtshop arena " + v.arena());
            return true;
        }
        if (data.journal) {
            return super.mouseClicked(mx, my, button);
        }
        if (v.state() == QuestView.AVAILABLE) {
            if (Ui.inside(mx, my, dx + dw - 96, by, 88, 18)) {
                click();
                ShopNet.CHANNEL.sendToServer(new QuestActionPacket(QuestActionPacket.ACCEPT, v.id(), -1));
                return true;
            }
        } else {
            boolean ready = v.state() == QuestView.READY;
            boolean needChoice = ready && !v.choices().isEmpty() && choice < 0;
            if (!data.journal && ready && !needChoice && Ui.inside(mx, my, dx + dw - 96, by, 88, 18)) {
                click();
                ShopNet.CHANNEL.sendToServer(new QuestActionPacket(QuestActionPacket.TURN_IN, v.id(), choice));
                return true;
            }
            if (Ui.inside(mx, my, dx + 8, by, 72, 18)) {
                click();
                ShopNet.CHANNEL.sendToServer(new QuestActionPacket(QuestActionPacket.ABANDON, v.id(), -1));
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (Ui.inside(mx, my, listX(), listY(), LIST_W, listH())) {
            listScroll -= (int) Math.signum(delta);
            clampList();
            return true;
        }
        if (Ui.inside(mx, my, detX(), detY(), detW(), detH())) {
            detailScroll = Math.max(0, Math.min(maxDetailScroll, detailScroll - (int) Math.signum(delta) * 22));
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    /** 점검용: 지금 고른 퀘스트의 본문 (원문). */
    public String currentTextForTest() {
        QuestView v = current();
        return v == null ? "" : v.text();
    }

    /** 서버가 보낸 "\u0001키\u0002인자..." 조각을 이 클라이언트의 언어로 바꾼다. */
    public static String untr(String text) {
        if (text.indexOf('\u0001') < 0) return text;
        StringBuilder out = new StringBuilder();
        String[] lines = text.split("\n", -1);
        for (int li = 0; li < lines.length; li++) {
            String line = lines[li];
            int at = line.indexOf('\u0001');
            if (at >= 0) {
                String[] p = line.substring(at + 1).split("\u0002", -1);
                Object[] args = new Object[p.length - 1];
                System.arraycopy(p, 1, args, 0, args.length);
                line = line.substring(0, at) + Component.translatable(p[0], args).getString();
            }
            if (li > 0) out.append('\n');
            out.append(line);
        }
        return out.toString();
    }
}

package com.rtshop.client;

import com.rtshop.net.ClosePacket;
import com.rtshop.net.QuestActionPacket;
import com.rtshop.net.QuestOpenPacket;
import com.rtshop.net.ShopNet;
import com.rtshop.quest.QuestDef;
import com.rtshop.quest.QuestView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * NPC 와 나누는 퀘스트 이야기 (3.51): 퀘스트를 받고 · 보고하고 · 그만두는 일을 모두 대화 상자에서 한다 (예전의 양피지 창 대신).
 * <ol>
 * <li>의뢰 고르기: [보고] · [새 의뢰] · [진행 중] 이 선택지로 나온다.</li>
 * <li>NPC 의 말: 퀘스트 본문이 한 쪽씩, 한 자씩 써진다 (클릭·스페이스로 넘긴다). "[모드] …" 줄은 도움말 쪽으로 나온다.</li>
 * <li>의뢰서: 대화 상자 위에 목표와 보상이 뜨고, 대답을 고른다 (맡는다 · 보고한다 · 그만둔다, 보고할 때의 갈림길 선택지도 여기서).</li>
 * </ol>
 * 퀘스트 수첩(QuestScreen)은 /rtshop quest 로 어디서나 내용을 볼 때만 쓴다.
 */
public class QuestTalkScreen extends Screen {
    private static final int LIST = 0, LINES = 1, OFFER = 2, RESULT = 3, CONFIRM = 4;

    private QuestOpenPacket data;
    private final String npcName;
    /** 상인 화면의 [퀘스트] 단추로 들어왔을 때: 돌아갈 상인의 엔티티 번호 (-1 = 없음). */
    private int backNpc = -1;
    private int mode = LIST;
    private String curId = "";
    /** {말하는 사람, 대사, 갈래("" 말 · "tip" 도움말 · "note" 알림)} */
    private final List<String[]> lines = new ArrayList<>();
    private int index;
    private float shown;
    private int sinceSound;
    private int hover = -1;
    private int listScroll, cardScroll, cardMax;
    private int pendingAction = -1;
    private String pendingId = "", pendingTitle = "";

    public QuestTalkScreen(QuestOpenPacket data) {
        super(Component.literal(data.title));
        this.data = data;
        this.npcName = data.title;
        if (System.currentTimeMillis() - NpcButtons.returnArmedAt < 6000) backNpc = NpcButtons.returnNpc;
        NpcButtons.returnArmedAt = 0;
        enterList(true);
    }

    // ------------------------------------------------------------------ 상태

    private static String tr(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    /** 보여 줄 차례: 보고할 것 → 새 의뢰 → 진행 중 → 지난 일. */
    private List<QuestView> views() {
        List<QuestView> out = new ArrayList<>(data.views);
        out.sort(Comparator.comparingInt(v -> switch (v.state()) {
            case QuestView.READY -> 0;
            case QuestView.AVAILABLE -> 1;
            case QuestView.ACTIVE -> 2;
            default -> 3;
        }));
        return out;
    }

    private QuestView find(String id) {
        for (QuestView v : data.views) if (v.id().equals(id)) return v;
        return null;
    }

    private QuestView cur() {
        return find(curId);
    }

    private void enterList(boolean first) {
        mode = LIST;
        curId = "";
        hover = -1;
        listScroll = 0;
        lines.clear();
        List<QuestView> vs = views();
        if (first && vs.size() == 1 && vs.get(0).state() != QuestView.DONE) {
            select(vs.get(0)); // 이야기가 하나뿐이면 바로 그 이야기로
            return;
        }
        String greet = QuestScreen.untr(data.greeting == null ? "" : data.greeting).trim();
        if (!greet.isEmpty()) {
            for (String para : greet.split("\n")) if (!para.isBlank()) lines.add(new String[]{npcName, para.trim(), ""});
        }
        if (lines.isEmpty()) lines.add(new String[]{"", tr(vs.isEmpty() ? "rtshop.qtalk.none" : "rtshop.qtalk.list"), "note"});
        index = lines.size() - 1;
        shown = line()[1].length();
    }

    private void select(QuestView v) {
        curId = v.id();
        hover = -1;
        cardScroll = 0;
        lines.clear();
        Font f = Minecraft.getInstance().font;
        for (QuestText.Line l : QuestText.lines(v.text())) {
            String text = l.text().trim();
            if (text.isEmpty()) continue;
            if (l.tip()) {
                for (String page : pages(f, text)) lines.add(new String[]{tr("rtshop.qtalk.tip", QuestText.modName(l.mod())), page, "tip"});
            } else {
                String[] who = split(text);
                for (String page : pages(f, who[1])) lines.add(new String[]{who[0], page, ""});
            }
        }
        index = 0;
        shown = 0;
        if (lines.isEmpty()) {
            lines.add(new String[]{"", v.title(), "note"});
            shown = v.title().length();
            mode = OFFER;
        } else {
            mode = LINES;
            log();
        }
    }

    /** "이름: 대사" → {이름, 대사}. 이름이 없으면 이 NPC. */
    private String[] split(String s) {
        int c = s.indexOf(": ");
        String head = c > 0 ? s.substring(0, c) : "";
        if (c > 0 && c <= 20 && head.split(" ").length <= 3 && !head.matches(".*[.?!,\"'…].*")) {
            return new String[]{head, s.substring(c + 2)};
        }
        return new String[]{npcName, s};
    }

    /** 긴 문단은 대화 상자에 들어가는 만큼(네 줄)씩 문장 단위로 나눈다. */
    private List<String> pages(Font f, String text) {
        List<String> out = new ArrayList<>();
        int w = Math.max(120, boxW() - 24);
        if (f == null || f.split(Component.literal(text), w).size() <= 4) {
            out.add(text);
            return out;
        }
        StringBuilder page = new StringBuilder();
        for (String sentence : text.split("(?<=[.!?…。])\\s+")) {
            String next = page.length() == 0 ? sentence : page + " " + sentence;
            if (page.length() > 0 && f.split(Component.literal(next), w).size() > 4) {
                out.add(page.toString());
                page = new StringBuilder(sentence);
            } else {
                page = new StringBuilder(next);
            }
        }
        if (page.length() > 0) out.add(page.toString());
        return out;
    }

    private String[] line() {
        return index >= 0 && index < lines.size() ? lines.get(index) : new String[]{npcName, "", ""};
    }

    private boolean lineDone() {
        return shown >= line()[1].length();
    }

    private boolean choosing() {
        return mode == LIST || mode == OFFER || mode == CONFIRM;
    }

    private void log() {
        String[] l = line();
        if (!l[2].equals("note") && !l[1].isEmpty()) TalkHistory.add(data.npcKey, l[0], l[1]);
    }

    private void logMine(String text) {
        String me = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getName().getString() : "me";
        TalkHistory.add(data.npcKey, "▷ " + me, text);
    }

    @Override
    public void tick() {
        if (!choosing() && !lineDone()) {
            shown = Math.min(line()[1].length(), shown + 1.6F);
            if (++sinceSound >= 3 && ClientConfig.get().dialogueSound) {
                sinceSound = 0;
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HAT.value(), 1.6F + (float) Math.random() * 0.2F, 0.12F));
            }
        }
    }

    private void advance() {
        if (choosing()) return;
        if (!lineDone()) {
            shown = line()[1].length();
            return;
        }
        if (index + 1 < lines.size()) {
            index++;
            shown = 0;
            log();
            return;
        }
        if (mode == LINES) {
            mode = OFFER;
            hover = -1;
        } else { // RESULT
            if (data.views.isEmpty() && backNpc < 0) {
                onClose();
            } else {
                enterList(false);
            }
        }
    }

    /** 서버가 보낸 새 내용: 방금 한 일(맡기·보고·그만두기)의 결과를 알리고 목록으로 돌아간다. */
    public void update(QuestOpenPacket packet) {
        this.data = packet;
        if (pendingAction >= 0) {
            QuestView now = find(pendingId);
            boolean ok = switch (pendingAction) {
                case QuestActionPacket.ACCEPT -> now != null && (now.state() == QuestView.ACTIVE || now.state() == QuestView.READY);
                case QuestActionPacket.TURN_IN -> now == null || now.state() == QuestView.DONE || now.state() == QuestView.AVAILABLE;
                default -> now == null || now.state() == QuestView.AVAILABLE;
            };
            String key = pendingAction == QuestActionPacket.ACCEPT ? "rtshop.qtalk.accepted"
                    : pendingAction == QuestActionPacket.TURN_IN ? "rtshop.qtalk.turned_in" : "rtshop.qtalk.abandoned";
            pendingAction = -1;
            if (ok) {
                mode = RESULT;
                lines.clear();
                String note = tr(key, pendingTitle);
                lines.add(new String[]{"", note, "note"});
                index = 0;
                shown = note.length();
                curId = "";
                return;
            }
            // 되지 않았다 (까닭은 서버가 알림으로 보냈다): 의뢰서로 돌아간다
            if (now != null) {
                mode = OFFER;
                return;
            }
            enterList(false);
            return;
        }
        if (mode != LIST && mode != RESULT && cur() == null) enterList(false);
    }

    // ------------------------------------------------------------------ 고를 것

    /** {갈래, 값, 글, 빛깔(16진수 문자열)} */
    private List<String[]> choices() {
        List<String[]> out = new ArrayList<>();
        if (mode == LIST) {
            for (QuestView v : views()) {
                String tag = switch (v.state()) {
                    case QuestView.READY -> "rtshop.qtalk.tag_ready";
                    case QuestView.AVAILABLE -> "rtshop.qtalk.tag_new";
                    case QuestView.ACTIVE -> "rtshop.qtalk.tag_active";
                    default -> "rtshop.qtalk.tag_done";
                };
                String col = switch (v.state()) {
                    case QuestView.READY -> "FF9FE88A";
                    case QuestView.AVAILABLE -> "FFFFC63A";
                    case QuestView.ACTIVE -> "FFE8DCC0";
                    default -> "FF9A8E78";
                };
                out.add(new String[]{"quest", v.id(), tr(tag) + " " + v.title(), col});
            }
            if (backNpc >= 0) out.add(new String[]{"back", "", "◀ " + tr("rtshop.quest.back"), "FFE8DCC0"});
            out.add(new String[]{"talk", "", tr("rtshop.qtalk.to_talk"), "FFE8DCC0"});
            out.add(new String[]{"bye", "", tr("rtshop.talk.bye"), "FFE8DCC0"});
            return out;
        }
        QuestView v = cur();
        if (v == null) return out;
        if (mode == CONFIRM) {
            out.add(new String[]{"abandon_yes", "", tr("rtshop.qtalk.abandon_yes"), "FFFF9080"});
            out.add(new String[]{"offer", "", tr("rtshop.qtalk.abandon_no"), "FFE8DCC0"});
            return out;
        }
        switch (v.state()) {
            case QuestView.AVAILABLE -> {
                out.add(new String[]{"accept", "", tr("rtshop.qtalk.accept"), "FFFFC63A"});
                out.add(new String[]{"list", "", tr("rtshop.qtalk.later"), "FFE8DCC0"});
            }
            case QuestView.READY -> {
                if (v.choices().isEmpty()) {
                    out.add(new String[]{"turnin", "-1", tr("rtshop.qtalk.report"), "FF9FE88A"});
                } else {
                    for (int i = 0; i < v.choices().size(); i++) out.add(new String[]{"turnin", String.valueOf(i), v.choices().get(i), "FFBFDDF5"});
                }
                out.add(new String[]{"list", "", tr("rtshop.qtalk.not_yet"), "FFE8DCC0"});
            }
            case QuestView.ACTIVE -> {
                out.add(new String[]{"list", "", tr("rtshop.qtalk.ok"), "FFE8DCC0"});
                out.add(new String[]{"abandon", "", tr("rtshop.qtalk.abandon"), "FFB8A890"});
            }
            default -> out.add(new String[]{"list", "", tr("rtshop.qtalk.back"), "FFE8DCC0"});
        }
        if (!v.arena().isEmpty() && v.state() != QuestView.DONE) {
            out.add(new String[]{"arena", "", "⚔ " + tr("rtshop.arena.button"), "FFBFDDF5"});
        }
        return out;
    }

    private void choose(String[] c) {
        Minecraft mc = Minecraft.getInstance();
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        QuestView v = cur();
        switch (c[0]) {
            case "quest" -> {
                QuestView q = find(c[1]);
                if (q != null) {
                    logMine(q.title());
                    select(q);
                }
            }
            case "accept" -> act(QuestActionPacket.ACCEPT, v, -1, c[2]);
            case "turnin" -> act(QuestActionPacket.TURN_IN, v, Integer.parseInt(c[1]), c[2]);
            case "abandon" -> {
                mode = CONFIRM;
                hover = -1;
            }
            case "abandon_yes" -> act(QuestActionPacket.ABANDON, v, -1, c[2]);
            case "offer" -> {
                mode = OFFER;
                hover = -1;
            }
            case "list" -> enterList(false);
            case "arena" -> {
                onClose();
                if (mc.player != null && v != null) mc.player.connection.sendCommand("rtshop arena " + v.arena());
            }
            case "back" -> goBack();
            case "talk" -> {
                onClose();
                if (mc.player != null) mc.player.connection.sendCommand("rtshop talk here");
            }
            default -> onClose();
        }
    }

    private void act(int action, QuestView v, int choice, String said) {
        if (v == null || pendingAction >= 0) return;
        logMine(said);
        pendingAction = action;
        pendingId = v.id();
        pendingTitle = v.title();
        ShopNet.CHANNEL.sendToServer(new QuestActionPacket(action, v.id(), choice));
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

    // ------------------------------------------------------------------ 자리

    private int boxX() {
        return Math.max(8, width / 2 - 230);
    }

    private int boxW() {
        int w = width <= 0 ? Minecraft.getInstance().getWindow().getGuiScaledWidth() : width;
        return Math.min(w - 16, 460);
    }

    private int cols(int n) {
        return mode == LIST && n > 5 ? 2 : 1;
    }

    private static final int MAX_ROWS = 8;

    private int rows(int n) {
        int c = cols(n);
        return Math.min(MAX_ROWS, (n + c - 1) / c);
    }

    private int textRows() {
        if (font == null) return 1;
        int max = mode == LIST ? 3 : 2;
        return Math.max(1, Math.min(max, font.split(Component.literal(line()[1]), boxW() - 24).size()));
    }

    private int boxH() {
        if (!choosing()) return 70;
        int n = choices().size();
        return Math.max(70, 28 + (textRows() - 1) * 11 + rows(n) * 11);
    }

    private int boxY() {
        return height - boxH() - 10;
    }

    @Override
    public void renderBackground(GuiGraphics g) {
        g.fillGradient(0, height / 2, width, height, 0x00000000, 0xA0000000);
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

    // ------------------------------------------------------------------ 그리기

    private static void frame(GuiGraphics g, int x, int y, int w, int h, int fill) {
        g.fill(x, y, x + w, y + h, fill);
        g.fill(x, y, x + w, y + 1, 0xFFC7A25A);
        g.fill(x, y + h - 1, x + w, y + h, 0xFFC7A25A);
        g.fill(x, y, x + 1, y + h, 0xFFC7A25A);
        g.fill(x + w - 1, y, x + w, y + h, 0xFFC7A25A);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        int x = boxX(), y = boxY(), w = boxW(), h = boxH();
        if (mode == OFFER || mode == CONFIRM) renderCard(g, mx, my, x, y, w);
        frame(g, x, y, w, h, 0xFC1B140C);
        g.fill(x + 2, y + 2, x + w - 2, y + 3, 0x40C7A25A);
        String[] c = line();
        boolean note = c[2].equals("note"), tip = c[2].equals("tip");
        if (!note && !c[0].isEmpty()) {
            int nw = font.width(c[0]) + 12, ny = y - 11;
            g.fill(x + 10, ny, x + 10 + nw, ny + 13, 0xFFC7A25A);
            g.fill(x + 11, ny + 1, x + 9 + nw, ny + 12, tip ? 0xFF9FD0FF : c[0].equals(npcName) ? 0xFFE8C878 : 0xFF9FD0FF);
            g.drawString(font, c[0], x + 16, ny + 3, 0xFF1B140C, false);
        }
        int textCol = note ? 0xFFE8C878 : tip ? 0xFFCFE6F8 : 0xFFF6ECD2;
        if (choosing()) {
            String head = mode == CONFIRM && cur() != null ? tr("rtshop.qtalk.abandon_ask", cur().title()) : c[1];
            List<FormattedCharSequence> last = font.split(Component.literal(head), w - 24);
            int trw = mode == CONFIRM ? Math.min(2, last.size()) : textRows();
            for (int i = 0; i < Math.min(trw, last.size()); i++) {
                g.drawString(font, last.get(i), x + 12, y + 8 + i * 11, mode == CONFIRM ? 0xFFFFC8B0 : (textCol & 0xFFFFFF) | (mode == LIST ? 0xFF000000 : 0xB0000000), false);
            }
            int top = y + 21 + (trw - 1) * 11;
            List<String[]> cs = choices();
            int cols = cols(cs.size()), rows = rows(cs.size());
            int pageSize = rows * cols;
            listScroll = Math.max(0, Math.min(listScroll, Math.max(0, cs.size() - pageSize)));
            int cw = (w - 24) / cols;
            hover = -1;
            for (int k = 0; k < pageSize && listScroll + k < cs.size(); k++) {
                int i = listScroll + k;
                int cx = x + 12 + (k % cols) * cw, cy = top + (k / cols) * 11;
                boolean hov = mx >= cx && mx < cx + cw - 4 && my >= cy - 1 && my < cy + 10;
                if (hov) hover = i;
                String label = (hov ? "▶ " : "▷ ") + cs.get(i)[2];
                int col = (int) Long.parseLong(cs.get(i)[3], 16);
                g.drawString(font, font.plainSubstrByWidth(label, cw - 6), cx, cy, hov ? 0xFFFFE3A3 : col, false);
            }
            if (cs.size() > pageSize) {
                String more = (listScroll / cols + 1) + " / " + ((cs.size() - pageSize + cols - 1) / cols + 1) + "  " + tr("rtshop.qtalk.scroll");
                Ui.scaledText(g, font, more, x + w - 10 - (int) (font.width(more) * 0.7F), y + h - 10, 0.7F, 0x88F6ECD2, false);
            }
        } else {
            String text = c[1].substring(0, Mth.clamp((int) shown, 0, c[1].length()));
            List<FormattedCharSequence> ls = font.split(Component.literal(text), w - 24);
            for (int i = 0; i < Math.min(5, ls.size()); i++) g.drawString(font, ls.get(i), x + 12, y + 8 + i * 11, textCol, false);
            if (lineDone() && (System.currentTimeMillis() / 400) % 2 == 0) g.drawString(font, "▼", x + w - 16, y + h - 12, 0xFFC7A25A, false);
            String hint = tr("rtshop.qtalk.hint");
            Ui.scaledText(g, font, hint, x + w - 10 - (int) (font.width(hint) * 0.7F) - 14, y + h - 10, 0.7F, 0x88F6ECD2, false);
            if (mode == LINES) Ui.scaledText(g, font, (index + 1) + " / " + lines.size(), x + 10, y + h - 10, 0.7F, 0x66F6ECD2, false);
        }
        super.render(g, mx, my, partial);
    }

    private static ItemStack stackOf(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        Item item = rl == null ? null : ForgeRegistries.ITEMS.getValue(rl);
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    /** 의뢰서: 대화 상자 위에 뜨는 목표와 보상. */
    private void renderCard(GuiGraphics g, int mx, int my, int x, int boxY, int w) {
        QuestView v = cur();
        if (v == null) return;
        // 줄을 먼저 모은다: {갈래, 글, 빛깔} — 갈래 h 머리 · t 글 · g 금화 · i 아이템(글 = 아이템 ID \t 표시)
        List<Object[]> rowsOut = new ArrayList<>();
        int inner = w - 28;
        if (!v.objectives().isEmpty()) {
            rowsOut.add(new Object[]{"h", tr("rtshop.quest.objectives"), 0xFFC7A25A});
            for (QuestView.ObjView o : v.objectives()) {
                boolean done = o.cur() >= o.need();
                String label = QuestScreen.objectiveName(o);
                if (o.type() == QuestDef.Type.PAY.ordinal()) {
                    label = tr("rtshop.quest.obj.pay_amount", Ui.num(o.need()));
                } else if (o.need() > 1) {
                    label += " (" + Math.min(o.cur(), o.need()) + "/" + o.need() + ")";
                }
                boolean fresh = v.state() == QuestView.AVAILABLE;
                String mark = fresh ? "· " : done ? "✔ " : "□ ";
                List<FormattedCharSequence> parts = font.split(Component.literal(mark + label), inner);
                for (FormattedCharSequence p : parts) rowsOut.add(new Object[]{"t", p, !fresh && done ? 0xFF9FE88A : 0xFFF0E6CC});
            }
        }
        if (v.rewardGold() > 0 || !v.rewardItems().isEmpty()) {
            rowsOut.add(new Object[]{"h", tr("rtshop.quest.rewards"), 0xFFC7A25A});
            if (v.rewardGold() > 0) rowsOut.add(new Object[]{"g", Ui.num(v.rewardGold()) + " G", 0xFFFFE9A8});
            for (QuestView.RewardItem r : v.rewardItems()) {
                ItemStack s = stackOf(r.item());
                String name = (s.isEmpty() ? r.item() : s.getHoverName().getString()) + " x" + r.count();
                rowsOut.add(new Object[]{"i", s, 0xFFF0E6CC, Ui.clip(font, name, inner - 16)});
            }
        }
        int contentH = 22 + (v.chain().isEmpty() ? 0 : 11);
        for (Object[] r : rowsOut) contentH += r[0].equals("h") ? 15 : r[0].equals("i") ? 14 : 11;
        contentH += 6;
        int room = boxY - 18 - 8;
        int ch = Math.min(contentH, Math.max(60, room));
        int cy = boxY - 18 - ch;
        cardMax = Math.max(0, contentH - ch);
        cardScroll = Math.max(0, Math.min(cardMax, cardScroll));
        frame(g, x, cy, w, ch, 0xF21B140C);
        g.fill(x + 2, cy + 2, x + w - 2, cy + 3, 0x40C7A25A);
        g.enableScissor(x + 1, cy + 3, x + w - 1, cy + ch - 2);
        int yy = cy + 7 - cardScroll;
        String tag = tr(switch (v.state()) {
            case QuestView.READY -> "rtshop.qtalk.tag_ready";
            case QuestView.AVAILABLE -> "rtshop.qtalk.tag_new";
            case QuestView.ACTIVE -> "rtshop.qtalk.tag_active";
            default -> "rtshop.qtalk.tag_done";
        });
        int tagCol = v.state() == QuestView.READY ? 0xFF9FE88A : v.state() == QuestView.AVAILABLE ? 0xFFFFC63A : 0xFFB8A890;
        g.drawString(font, tag, x + 12, yy + 2, tagCol, false);
        Ui.scaledText(g, font, Ui.clip(font, v.title(), (int) ((inner - font.width(tag) - 8) / 1.2F)), x + 16 + font.width(tag), yy, 1.2F, 0xFFFFE9B0, false);
        yy += 15;
        if (!v.chain().isEmpty()) {
            g.drawString(font, Ui.clip(font, v.chain(), inner), x + 12, yy, 0xFF9A8E78, false);
            yy += 11;
        }
        for (Object[] r : rowsOut) {
            switch ((String) r[0]) {
                case "h" -> {
                    g.fill(x + 12, yy + 2, x + w - 12, yy + 3, 0x30C7A25A);
                    g.drawString(font, (String) r[1], x + 12, yy + 5, (Integer) r[2], false);
                    yy += 15;
                }
                case "g" -> {
                    Ui.coin(g, x + 16, yy - 1, 10);
                    g.drawString(font, (String) r[1], x + 30, yy, (Integer) r[2], false);
                    yy += 11;
                }
                case "i" -> {
                    ItemStack s = (ItemStack) r[1];
                    if (!s.isEmpty()) {
                        g.pose().pushPose();
                        g.pose().translate(x + 15, yy - 1, 0);
                        g.pose().scale(0.75F, 0.75F, 1);
                        g.renderItem(s, 0, 0);
                        g.pose().popPose();
                    }
                    g.drawString(font, (String) r[3], x + 30, yy + 2, (Integer) r[2], false);
                    yy += 14;
                }
                default -> {
                    g.drawString(font, (FormattedCharSequence) r[1], x + 16, yy, (Integer) r[2], false);
                    yy += 11;
                }
            }
        }
        g.disableScissor();
        if (cardMax > 0) {
            int barH = Math.max(10, (ch - 6) * ch / contentH);
            int barY = cy + 3 + (ch - 6 - barH) * cardScroll / cardMax;
            g.fill(x + w - 4, barY, x + w - 2, barY + barH, 0xFFC7A25A);
        }
    }

    // ------------------------------------------------------------------ 입력

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (choosing()) {
            List<String[]> cs = choices();
            if (hover >= 0 && hover < cs.size()) choose(cs.get(hover));
            return true;
        }
        advance();
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int step = (int) Math.signum(delta);
        if ((mode == OFFER || mode == CONFIRM) && my < boxY() && cardMax > 0) {
            cardScroll = Math.max(0, Math.min(cardMax, cardScroll - step * 11));
            return true;
        }
        if (choosing()) {
            int n = choices().size();
            listScroll = Math.max(0, listScroll - step * cols(n));
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 32 || key == 257 || key == 335) { // 스페이스·엔터
            if (!choosing()) advance();
            return true;
        }
        if (choosing() && key >= 49 && key <= 57) { // 숫자로 고르기 (보이는 차례대로)
            List<String[]> cs = choices();
            int i = listScroll + key - 49;
            if (i < cs.size()) choose(cs.get(i));
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    // ------------------------------------------------------------------ 점검용

    public int viewsForTest() {
        return data.views.size();
    }

    public int modeForTest() {
        return mode;
    }

    /** 지금 고른(없으면 첫) 퀘스트의 본문. */
    public String currentTextForTest() {
        QuestView v = cur() != null ? cur() : views().isEmpty() ? null : views().get(0);
        return v == null ? "" : v.text();
    }

    public String currentIdForTest() {
        return curId;
    }

    public boolean selectForTest(String id) {
        QuestView v = find(id);
        if (v != null) select(v);
        return v != null;
    }

    /** 말을 끝까지 넘겨 의뢰서(대답 고르기)까지 간다. */
    public void skipForTest() {
        for (int i = 0; i < 400 && !choosing(); i++) advance();
    }

    /** 이 갈래의 첫 선택지를 누른다 (accept · turnin · abandon · abandon_yes · list · quest …). */
    public boolean chooseForTest(String kind) {
        for (String[] c : choices()) {
            if (c[0].equals(kind)) {
                choose(c);
                return true;
            }
        }
        return false;
    }

    public int choiceCountForTest(String kind) {
        int n = 0;
        for (String[] c : choices()) if (c[0].equals(kind)) n++;
        return n;
    }

    public int pagesForTest() {
        return lines.size();
    }

    /** 대화 상자와 의뢰서가 화면 안에 들어오는가. */
    public boolean fitsForTest() {
        return boxX() >= 0 && boxX() + boxW() <= width && boxY() >= 0 && boxY() + boxH() <= height;
    }
}

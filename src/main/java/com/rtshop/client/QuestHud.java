package com.rtshop.client;

import com.rtshop.net.QuestHudPacket;
import com.rtshop.quest.QuestDef;
import com.rtshop.quest.QuestView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 화면 구석에 진행 중인 퀘스트와 목표를 보여 주는 HUD.
 * 위치·너비·글자 크기·보일 퀘스트 수는 Gold 표시와 같은 /rtshop hud 화면에서 바꾼다 (config/rtshop/client.json).
 */
public final class QuestHud {
    private QuestHud() {
    }

    private static final int CREAM = 0xFFF5E6C0;
    private static final int SOFT = 0xFFCDB98E;
    private static final int DONE = 0xFF8FD67A;
    private static final int NEXT = 0xFF9FD0F0;
    private static final int LINE = 10;
    private static final int PAD = 4;
    /** 퀘스트와 퀘스트 사이 간격 (구분선이 들어간다). */
    private static final int GAP = 4;

    private static int height(List<Row> rows) {
        int h = 0;
        for (int i = 0; i < rows.size(); i++) {
            h += LINE + (rows.get(i).title() && i > 0 ? GAP : 0);
        }
        return h;
    }

    /** 그릴 줄 하나 (글, 색, 들여쓰기). */
    private record Row(String text, int color, int indent, boolean title) {
    }

    public static void render(GuiGraphics g, int sw, int sh) {
        Minecraft mc = Minecraft.getInstance();
        ClientConfig cfg = ClientConfig.get();
        if (!cfg.questHudEnabled || mc.options.hideGui || mc.player == null || mc.screen instanceof HudEditScreen
                || mc.screen instanceof QuestScreen) {
            return;
        }
        List<QuestHudPacket.Entry> list = ClientQuestHud.entries;
        if (list.isEmpty()) {
            return;
        }
        // 채팅 창보다 위에 그린다 (채팅이 HUD 를 가리지 않게)
        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        draw(g, sw, sh, list);
        g.pose().popPose();
    }

    /** 현재 설정에 따른 HUD 영역 {x, y, w, h} (GUI 좌표). */
    public static int[] bounds(int sw, int sh, List<QuestHudPacket.Entry> list) {
        ClientConfig cfg = ClientConfig.get();
        float s = (float) cfg.questHudScale;
        int w = Math.max(60, (int) Math.round(cfg.questHudWidth * sw));
        int inner = (int) ((w - PAD * 2 * s) / s);
        int h = (int) Math.ceil((height(rows(Minecraft.getInstance().font, list, inner)) + PAD * 2) * s);
        int x = (int) Math.round(cfg.questHudX * sw);
        int y = (int) Math.round(cfg.questHudY * sh);
        x = Math.max(0, Math.min(sw - w, x));
        y = Math.max(0, Math.min(sh - h, y));
        return new int[]{x, y, w, h};
    }

    public static void draw(GuiGraphics g, int sw, int sh, List<QuestHudPacket.Entry> list) {
        Font font = Minecraft.getInstance().font;
        ClientConfig cfg = ClientConfig.get();
        float s = (float) cfg.questHudScale;
        int[] b = bounds(sw, sh, list);
        int x = b[0];
        int y = b[1];
        int w = b[2];
        int h = b[3];
        // Gold 표시와 같은 나무 틀, 안쪽은 반투명
        g.fill(x, y, x + w, y + h, 0xCC140B05);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xB03E2512);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, 0xCC8A5A30);
        g.fill(x + 1, y + 1, x + 2, y + h - 1, 0xCC8A5A30);
        int inner = (int) ((w - PAD * 2 * s) / s);
        List<Row> rows = rows(font, list, inner);
        g.pose().pushPose();
        g.pose().translate(x + PAD * s, y + PAD * s, 0);
        g.pose().scale(s, s, 1.0F);
        int yy = 0;
        for (Row r : rows) {
            if (r.title() && yy > 0) {
                yy += GAP;
                g.fill(0, yy - 3, inner, yy - 2, 0x55E8B84A);
            }
            HudFonts.draw(g, font, r.text(), r.indent(), yy + 1, r.color(), true, cfg.questHudFont);
            yy += LINE;
        }
        g.pose().popPose();
    }

    /** 보일 줄들: 퀘스트 제목, 목표(진행도), 완료할 수 있으면 돌아갈 곳. 너무 길면 줄임표로 자른다. */
    private static List<Row> rows(Font font, List<QuestHudPacket.Entry> list, int inner) {
        ClientConfig cfg = ClientConfig.get();
        List<Row> out = new ArrayList<>();
        QuestHudPacket.Entry next = null;
        List<QuestHudPacket.Entry> quests = new ArrayList<>();
        for (QuestHudPacket.Entry e : list) {
            if (e.id().startsWith("_next:")) next = e;
            else quests.add(e);
        }
        QuestHudPacket.Entry attend = null;
        for (QuestHudPacket.Entry e : quests) if (e.id().equals("_attend")) attend = e;
        quests.remove(attend);
        list = quests;
        int shown = Math.min(list.size(), cfg.questHudMax);
        for (int i = 0; i < shown; i++) {
            QuestHudPacket.Entry e = list.get(i);
            String title = (e.ready() ? "✔ " : "◆ ") + e.title();
            out.add(new Row(HudFonts.clip(font, title, inner, cfg.questHudFont), e.ready() ? DONE : Ui.C_GOLD, 0, true));
            if (e.ready()) {
                String back = HudLang.tr(cfg.questHudLang, "rtshop.qhud.return", e.turnIn());
                out.add(new Row(HudFonts.clip(font, back, inner - 8, cfg.questHudFont), DONE, 8, false));
                where(out, font, e.where(), inner, cfg, DONE);
                continue;
            }
            for (QuestView.ObjView o : e.objectives()) {
                boolean done = o.cur() >= o.need();
                String label = QuestScreen.objectiveName(o, cfg.questHudLang);
                if (o.type() == QuestDef.Type.PAY.ordinal()) {
                    label = HudLang.tr(cfg.questHudLang, "rtshop.quest.obj.pay_amount", Ui.num(o.need()));
                } else if (o.need() > 1) {
                    label += " " + Math.min(o.cur(), o.need()) + "/" + o.need();
                }
                out.add(new Row(HudFonts.clip(font, (done ? "✔ " : "· ") + label, inner - 8, cfg.questHudFont), done ? DONE : CREAM, 8, false));
            }
            where(out, font, e.where(), inner, cfg, SOFT);
        }
        if (list.size() > shown) {
            out.add(new Row(HudFonts.clip(font, HudLang.tr(cfg.questHudLang, "rtshop.qhud.more", list.size() - shown), inner, cfg.questHudFont), SOFT, 0, false));
        }
        if (next != null) {
            // 받을 수 있는 퀘스트: 다음에 찾아갈 NPC
            out.add(new Row(HudFonts.clip(font, "➤ " + HudLang.tr(cfg.questHudLang, "rtshop.qhud.next"), inner, cfg.questHudFont), NEXT, 0, true));
            out.add(new Row(HudFonts.clip(font, HudLang.tr(cfg.questHudLang, "rtshop.qhud.next_npc", next.turnIn()), inner - 8, cfg.questHudFont), CREAM, 8, false));
            out.add(new Row(HudFonts.clip(font, "「" + next.title() + "」", inner - 8, cfg.questHudFont), SOFT, 8, false));
            where(out, font, next.where(), inner, cfg, SOFT);
            int more = 0;
            try {
                more = Integer.parseInt(next.id().substring(6));
            } catch (NumberFormatException ignored) {
            }
            if (more > 0) out.add(new Row(HudFonts.clip(font, HudLang.tr(cfg.questHudLang, "rtshop.qhud.next_more", more), inner - 8, cfg.questHudFont), SOFT, 8, false));
        }
        if (attend != null) {
            // 오늘 출석을 아직 받지 않았다 (받으면 사라진다)
            out.add(new Row(HudFonts.clip(font, "✉ " + HudLang.tr(cfg.questHudLang, "rtshop.qhud.attend"), inner, cfg.questHudFont), 0xFFFFD27A, 0, true));
            out.add(new Row(HudFonts.clip(font, HudLang.tr(cfg.questHudLang, "rtshop.qhud.next_npc", attend.turnIn()), inner - 8, cfg.questHudFont), CREAM, 8, false));
            where(out, font, attend.where(), inner, cfg, SOFT);
        }
        return out;
    }

    /** NPC 가 있는 쪽과 거리를 한 줄로 ("차원;x;y;z"). 모르면 아무 줄도 넣지 않는다. */
    private static void where(List<Row> out, Font font, String where, int inner, ClientConfig cfg, int color) {
        String t = whereText(where, cfg.questHudLang);
        if (!t.isEmpty()) out.add(new Row(HudFonts.clip(font, "➤ " + t, inner - 8, cfg.questHudFont), color, 8, false));
    }

    public static String whereText(String where, String lang) {
        Minecraft mc = Minecraft.getInstance();
        if (where == null || where.isEmpty() || mc.player == null) return "";
        String[] p = where.split(";");
        if (p.length != 4) return "";
        try {
            if (!mc.player.level().dimension().location().toString().equals(p[0])) return HudLang.tr(lang, "rtshop.qhud.where_dim");
            double dx = Integer.parseInt(p[1]) + 0.5 - mc.player.getX(), dz = Integer.parseInt(p[3]) + 0.5 - mc.player.getZ();
            int dist = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
            if (dist < 6) return HudLang.tr(lang, "rtshop.qhud.where_here");
            int idx = Math.floorMod((int) Math.round(Math.toDegrees(Math.atan2(-dx, dz)) / 45.0), 8);
            return HudLang.tr(lang, "rtshop.qhud.where", HudLang.tr(lang, "rtshop.dir." + idx), dist, p[1], p[3]);
        } catch (NumberFormatException e) {
            return "";
        }
    }

    /** 편집 화면에서 보여 줄 보기 (진행 중인 퀘스트가 없을 때). */
    public static List<QuestHudPacket.Entry> sample() {
        List<QuestHudPacket.Entry> l = new ArrayList<>();
        l.add(new QuestHudPacket.Entry("sample_1", Component.translatable("rtshop.qhud.sample_title1").getString(), false, "",
                List.of(new QuestView.ObjView(QuestDef.Type.KILL.ordinal(), "minecraft:zombie", "", 5, 12),
                        new QuestView.ObjView(QuestDef.Type.DELIVER.ordinal(), "minecraft:bone", "", 16, 16))));
        l.add(new QuestHudPacket.Entry("sample_2", Component.translatable("rtshop.qhud.sample_title2").getString(), true,
                Component.translatable("rtshop.qhud.sample_npc").getString(), List.of()));
        return l;
    }

    /** 편집 화면용: 진행 중인 퀘스트가 있으면 그것을, 없으면 보기를 쓴다. */
    public static List<QuestHudPacket.Entry> previewEntries() {
        List<QuestHudPacket.Entry> l = ClientQuestHud.entries;
        return l.isEmpty() ? sample() : l;
    }
}

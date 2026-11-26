package com.rtshop.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * NPC 대화 상자 (화면 아래, RPG 형식). 글자가 한 자씩 써지고, 클릭·스페이스로 넘긴다.
 * talk: 인사 → 고를 것(일상 이야기·이야기 주제·퀘스트·작별). scene: 퀘스트 장면을 끝까지 본 뒤 서버에 알린다.
 */
public class DialogueScreen extends Screen {
    private final JsonObject data;
    private final String npc;
    private final int entity;
    private final String npcName;
    private final boolean scene;
    /** 지금 보여 주는 줄들과 위치. */
    private final List<String[]> lines = new ArrayList<>(); // {말하는 사람, 대사}
    private int index;
    private float shown;
    private boolean choosing;
    /** 플레이어가 대답할 줄 ("▷ 대답", " ‖ " 로 여러 대답). -1 = 없음. 대답은 선택지로 직접 눌러 말한다 (하나뿐이어도). */
    private int replyAt = -1;
    private static final String REPLY = "\u0000reply";
    private int hover = -1;
    private final List<JsonObject> topics = new ArrayList<>();
    private int sinceSound;

    public DialogueScreen(JsonObject data) {
        super(Component.literal(data.get("name").getAsString()));
        this.data = data;
        this.npc = data.get("npc").getAsString();
        this.entity = data.get("entity").getAsInt();
        this.npcName = data.get("name").getAsString();
        this.scene = "scene".equals(data.get("mode").getAsString());
        for (JsonElement e : data.getAsJsonArray("topics")) topics.add(e.getAsJsonObject());
        this.chats = data.has("chats") ? data.get("chats").getAsInt() : 0;
        if (scene) {
            play(data.getAsJsonArray("lines"));
        } else {
            lines.add(new String[]{npcName, data.get("greet").getAsString()});
            logCurrent();
        }
    }

    public static void open(JsonObject o) {
        if (autoMainForTest && o.has("main") && !"scene".equals(o.get("mode").getAsString()) && Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.connection.sendCommand("rtshop talk main " + o.get("entity").getAsInt());
            return;
        }
        Minecraft.getInstance().setScreen(new DialogueScreen(o));
    }

    private void play(JsonArray arr) {
        lines.clear();
        for (JsonElement e : arr) lines.add(split(e.getAsString()));
        index = 0;
        shown = 0;
        choosing = false;
        replyAt = -1;
        logCurrent();
    }

    /** 지금 줄을 대화 내역에 남긴다. */
    private void logCurrent() {
        if (index < lines.size()) TalkHistory.add(npc, lines.get(index)[0], lines.get(index)[1]);
    }

    /** 내가 고른 것도 내역에 남긴다. */
    private void logMine(String text) {
        String me = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getName().getString() : "me";
        TalkHistory.add(npc, "▷ " + me, text);
    }

    /** "이름: 대사" → {이름, 대사}. 이름이 없으면 이 NPC. */
    private String[] split(String s) {
        if (s.startsWith("▷")) return new String[]{REPLY, s.substring(1).trim()};
        int c = s.indexOf(": ");
        String head = c > 0 ? s.substring(0, c) : "";
        if (c > 0 && c <= 20 && head.split(" ").length <= 3 && !head.matches(".*[.?!,\"'…].*")) {
            return new String[]{s.substring(0, c), s.substring(c + 2)};
        }
        return new String[]{npcName, s};
    }

    private String[] cur() {
        return index < lines.size() ? lines.get(index) : new String[]{npcName, ""};
    }

    private boolean lineDone() {
        return shown >= cur()[1].length();
    }

    @Override
    public void tick() {
        if (!choosing && !lineDone()) {
            shown = Math.min(cur()[1].length(), shown + 1.6F);
            if (++sinceSound >= 3 && ClientConfig.get().dialogueSound) {
                sinceSound = 0;
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HAT.value(), 1.6F + (float) Math.random() * 0.2F, 0.12F));
            }
        }
    }

    /** 다음으로: 줄이 덜 써졌으면 다 쓰고, 다 썼으면 다음 줄, 끝나면 고르기(또는 장면 끝). */
    private void advance() {
        if (choosing) return;
        if (!lineDone()) {
            shown = cur()[1].length();
            return;
        }
        if (index + 1 < lines.size() && REPLY.equals(lines.get(index + 1)[0])) {
            // 다음은 내 차례: 대답을 선택지로 보여 주고 누르기를 기다린다
            replyAt = index + 1;
            choosing = true;
            return;
        }
        if (index + 1 < lines.size()) {
            index++;
            shown = 0;
            logCurrent();
            return;
        }
        endLines();
    }

    /** 줄을 다 읽었을 때: 장면이면 끝내고, 아니면 주제 고르기. */
    private void endLines() {
        if (scene) {
            // 장면 끝: 서버가 목표를 채우고 퀘스트 창을 연다
            onClose();
            if (minecraft.player != null) {
                // 흔적을 살펴본 장면이면 조사 목표를, NPC 와의 장면이면 대화 목표를 채운다
                if (data.has("spot")) minecraft.player.connection.sendCommand("rtshop spot done " + data.get("spot").getAsString());
                else minecraft.player.connection.sendCommand("rtshop talk done " + entity);
            }
            return;
        }
        choosing = true;
    }

    /** 점검용: 대화 상자가 열리면 곧바로 본래 기능(상점 등)을 고른다 (예전 시험이 상점 창을 기대하는 곳). */
    public static boolean autoMainForTest;
    private int chats;
    private String lastChat = "";
    private static final java.util.Random CHAT_RNG = new java.util.Random();

    private List<String[]> choices() {
        List<String[]> out = new ArrayList<>();
        if (replyAt >= 0 && replyAt < lines.size()) {
            for (String r : lines.get(replyAt)[1].split(" ‖ ")) out.add(new String[]{"reply", "", r.trim()});
            return out;
        }
        if (data.has("main")) out.add(new String[]{"main", "", Component.translatable(data.get("main").getAsString()).getString()});
        // 전용 NPC 의 일: 순위 기록원·칭호 관리인·현상금 게시판지기
        boolean service = data.has("service") && data.get("service").getAsBoolean();
        if (data.has("petition")) out.add(new String[]{"petition", "", Component.translatable("rtshop.talk.petition").getString()});
        if (data.has("ranks")) out.add(new String[]{"ranks", "", Component.translatable("rtshop.talk.ranks").getString()});
        if (data.has("titles")) out.add(new String[]{"titles", "", Component.translatable("rtshop.talk.titles").getString()});
        if (service && data.has("bounty")) out.add(new String[]{"bounty", "", Component.translatable("rtshop.talk.bounty").getString()});
        for (JsonObject t : topics) {
            String title = t.get("title").getAsString();
            boolean isNew = t.has("new") && t.get("new").getAsBoolean();
            out.add(new String[]{"topic", t.get("id").getAsString(), (isNew ? "✦ " : "") + title});
        }
        int q = data.has("quests") ? data.get("quests").getAsInt() : 0;
        if (q > 0) out.add(new String[]{"quest", "", Component.translatable("rtshop.talk.quests", q).getString()});
        else if (data.has("questNpc")) out.add(new String[]{"quest", "", Component.translatable("rtshop.talk.quests_none").getString()});
        out.add(new String[]{"history", "", Component.translatable("rtshop.talk.history").getString()});
        if (!service && data.has("bounty") && data.get("bounty").getAsBoolean()) out.add(new String[]{"bounty", "", Component.translatable("rtshop.talk.bounty").getString()});
        out.add(new String[]{"bye", "", Component.translatable("rtshop.talk.bye").getString()});
        return out;
    }

    private void choose(String[] c) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        if (!c[0].equals("history") && !c[0].equals("bye")) logMine(c[2].replace("✦ ", "").replace("▷ ", ""));
        switch (c[0]) {
            case "reply" -> {
                int next = replyAt + 1;
                replyAt = -1;
                choosing = false;
                shown = 0;
                if (next < lines.size() && REPLY.equals(lines.get(next)[0])) {
                    // 대답이 이어지면 앞 줄(상대의 말)을 그대로 두고 다시 고른다
                    shown = cur()[1].length();
                    replyAt = next;
                    choosing = true;
                } else if (next < lines.size()) {
                    index = next;
                    logCurrent();
                } else {
                    shown = cur()[1].length();
                    endLines();
                }
            }
            case "history" -> openHistory();
            case "main" -> {
                onClose();
                NpcButtons.returnNpc = entity;
                NpcButtons.returnArmedAt = System.currentTimeMillis();
                if (minecraft.player != null) minecraft.player.connection.sendCommand("rtshop talk main " + entity);
            }
            case "topic" -> {
                if (c[1].equals("_chatter")) {
                    chatter();
                    return;
                }
                for (JsonObject t : topics) {
                    if (t.get("id").getAsString().equals(c[1])) {
                        t.addProperty("new", false);
                        play(t.getAsJsonArray("lines"));
                        if (minecraft.player != null && !c[1].startsWith("_")) minecraft.player.connection.sendCommand("rtshop talk read " + npc + " " + c[1]);
                    }
                }
            }
            case "quest" -> {
                onClose();
                NpcButtons.returnNpc = entity;
                NpcButtons.returnArmedAt = System.currentTimeMillis();
                if (minecraft.player != null) minecraft.player.connection.sendCommand("rtshop quest open " + entity);
            }
            case "bounty" -> {
                onClose();
                if (minecraft.player != null) minecraft.player.connection.sendCommand("bounty board " + entity);
            }
            case "petition" -> {
                onClose();
                if (minecraft.player != null) minecraft.player.connection.sendCommand("rtshop report open");
            }
            case "ranks" -> {
                onClose();
                if (minecraft.player != null) minecraft.player.connection.sendCommand("rtshop rank");
            }
            case "titles" -> {
                onClose();
                if (minecraft.player != null) minecraft.player.connection.sendCommand("rtshop title");
            }
            default -> onClose();
        }
    }

    /** 일상 이야기: 누를 때마다 다른 말. 오래 이야기하면 가끔 흘리는 말이 섞인다. */
    private void chatter() {
        JsonObject t = null;
        for (JsonObject x : topics) if (x.get("id").getAsString().equals("_chatter")) t = x;
        if (t == null) return;
        List<String> pool = new ArrayList<>();
        if (t.has("pool")) for (var e : t.getAsJsonArray("pool")) pool.add(e.getAsString());
        if (pool.isEmpty()) for (var e : t.getAsJsonArray("lines")) pool.add(e.getAsString());
        chats++;
        String line;
        List<String> hints = new ArrayList<>();
        if (data.has("hints")) for (var e : data.getAsJsonArray("hints")) hints.add(e.getAsString());
        int after = data.has("hintAfter") ? data.get("hintAfter").getAsInt() : 12;
        if (!hints.isEmpty() && chats >= after && (chats - after) % 5 == 0) {
            line = hints.get(((chats - after) / 5) % hints.size());
        } else {
            List<String> rest = new ArrayList<>(pool);
            if (rest.size() > 1) rest.remove(lastChat);
            line = rest.get(CHAT_RNG.nextInt(rest.size()));
        }
        lastChat = line;
        JsonArray one = new JsonArray();
        one.add(line);
        play(one);
        if (minecraft.player != null) minecraft.player.connection.sendCommand("rtshop talk chat " + npc);
    }

    public boolean hasMainForTest() {
        return data.has("main");
    }

    public void chooseMainForTest() {
        for (String[] c : choices()) if (c[0].equals("main")) {
            choose(c);
            return;
        }
    }

    public int chatsForTest() {
        return chats;
    }

    public String lastChatForTest() {
        return lastChat;
    }

    public void chatterForTest() {
        chatter();
    }

    // ------------------------------------------------------------------ 그리기

    private int boxX() {
        return Math.max(8, width / 2 - 230);
    }

    private int boxW() {
        return Math.min(width - 16, 460);
    }

    private int boxH() {
        if (!choosing) return 70;
        int n = choices().size();
        int rows = n > 4 ? (n + 1) / 2 : n;
        return Math.max(70, 28 + (textRows() - 1) * 11 + rows * 11);
    }

    /** 고르는 동안 위에 남겨 두는 상대의 말 줄 수 (대답할 때는 마지막 말을 두 줄까지). */
    private int textRows() {
        if (font == null) return 1;
        // 고르는 동안에도 마지막 말은 끝까지 보인다 (잘린 채로 남지 않게)
        return Math.max(1, Math.min(5, font.split(Component.literal(cur()[1]), boxW() - 24).size()));
    }

    /** 대화 중에 보이는 「대화 내역」 딱지의 자리 {x, y, w, h}. */
    private int[] historyTag() {
        String t = Component.translatable("rtshop.talk.history_key").getString();
        int w = (int) (font.width(t) * 0.7F) + 6;
        return new int[]{boxX() + boxW() - w - 6, boxY() - 9, w, 9};
    }

    private void openHistory() {
        history = TalkHistory.of(npc);
        historyScroll = Math.max(0, historyLines().size() - historyRows());
    }

    private int boxY() {
        return height - boxH() - 10;
    }

    @Override
    public void renderBackground(GuiGraphics g) {
        // 세계가 보이게: 아래쪽만 살짝 어둡게
        g.fillGradient(0, height / 2, width, height, 0x00000000, 0xA0000000);
    }

    /** 대화 내역 보기 (null 이 아니면 보이는 중). */
    private List<String[]> history;
    private int historyScroll;

    private int historyRows() {
        return Math.max(4, (boxY() - 40) / 11);
    }

    /** 내역을 화면 폭에 맞춰 접은 줄들: {색, 글} */
    private List<Object[]> historyLines() {
        List<Object[]> out = new ArrayList<>();
        if (history == null) return out;
        int w = boxW() - 24;
        for (String[] h : history) {
            boolean mine = h[0].startsWith("▷ ");
            int col = mine ? 0xFF9FD0FF : 0xFFE8C878;
            List<FormattedCharSequence> ls = font.split(Component.literal(h[0] + ": " + h[1]), w);
            for (int i = 0; i < ls.size(); i++) out.add(new Object[]{i == 0 ? col : 0xFFE6DCC8, ls.get(i)});
        }
        return out;
    }

    private void renderHistory(GuiGraphics g) {
        int x = boxX(), w = boxW();
        int rows = historyRows();
        int top = boxY() + boxH() - (rows * 11 + 30);
        int bottom = boxY() + boxH();
        g.fill(x, top, x + w, bottom, 0xF51B140C);
        g.fill(x, top, x + w, top + 1, 0xFFC7A25A);
        g.fill(x, bottom - 1, x + w, bottom, 0xFFC7A25A);
        g.fill(x, top, x + 1, bottom, 0xFFC7A25A);
        g.fill(x + w - 1, top, x + w, bottom, 0xFFC7A25A);
        g.drawString(font, Component.translatable("rtshop.talk.history_title", npcName), x + 12, top + 7, 0xFFE8C878, false);
        List<Object[]> ls = historyLines();
        if (ls.isEmpty()) {
            g.drawString(font, Component.translatable("rtshop.talk.history_empty"), x + 12, top + 22, 0x99E6DCC8, false);
        }
        historyScroll = Math.max(0, Math.min(historyScroll, Math.max(0, ls.size() - rows)));
        for (int i = 0; i < rows && historyScroll + i < ls.size(); i++) {
            Object[] l = ls.get(historyScroll + i);
            g.drawString(font, (FormattedCharSequence) l[1], x + 12, top + 22 + i * 11, (Integer) l[0], false);
        }
        String hint = Component.translatable("rtshop.talk.history_hint").getString();
        Ui.scaledText(g, font, hint, x + w - 10 - (int) (font.width(hint) * 0.7F), bottom - 10, 0.7F, 0x88F6ECD2, false);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (history != null) {
            historyScroll = Math.max(0, historyScroll - (int) Math.signum(delta) * 3);
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        if (history != null) {
            renderHistory(g);
            super.render(g, mx, my, partial);
            return;
        }
        int x = boxX(), y = boxY(), w = boxW(), h = boxH();
        // 대화 상자
        g.fill(x, y, x + w, y + h, 0xFC1B140C);
        g.fill(x, y, x + w, y + 1, 0xFFC7A25A);
        g.fill(x, y + h - 1, x + w, y + h, 0xFFC7A25A);
        g.fill(x, y, x + 1, y + h, 0xFFC7A25A);
        g.fill(x + w - 1, y, x + w, y + h, 0xFFC7A25A);
        g.fill(x + 2, y + 2, x + w - 2, y + 3, 0x40C7A25A);
        String[] c = cur();
        // 이름표
        String name = c[0];
        int nw = font.width(name) + 12;
        int ny = y - 11;
        g.fill(x + 10, ny, x + 10 + nw, ny + 13, 0xFFC7A25A);
        g.fill(x + 11, ny + 1, x + 9 + nw, ny + 12, name.equals(npcName) ? 0xFFE8C878 : 0xFF9FD0FF);
        g.drawString(font, name, x + 16, ny + 3, 0xFF1B140C, false);
        if (choosing) {
            // 마지막 줄을 흐리게 두고 고를 것
            List<FormattedCharSequence> last = font.split(Component.literal(c[1]), w - 24);
            int tr = textRows();
            for (int i = 0; i < Math.min(tr, last.size()); i++)
                g.drawString(font, last.get(i), x + 12, y + 8 + i * 11, replyAt >= 0 ? 0xD0F6ECD2 : 0x99F6ECD2, false);
            int top = y + 21 + (tr - 1) * 11;
            List<String[]> cs = choices();
            hover = -1;
            int cols = cs.size() > 4 ? 2 : 1;
            int cw = (w - 24) / cols;
            for (int i = 0; i < cs.size(); i++) {
                int cx = x + 12 + (i % cols) * cw;
                int cy = top + (i / cols) * 11;
                boolean hov = mx >= cx && mx < cx + cw - 4 && my >= cy - 1 && my < cy + 10;
                if (hov) hover = i;
                String label = (hov ? "▶ " : "▷ ") + cs.get(i)[2];
                g.drawString(font, font.plainSubstrByWidth(label, cw - 6), cx, cy,
                        hov ? 0xFFFFE3A3 : cs.get(i)[0].equals("quest") ? 0xFFFFC63A : cs.get(i)[0].equals("reply") ? 0xFFBFDDF5 : 0xFFE8DCC0, false);
            }
        } else {
            String text = c[1].substring(0, Mth.clamp((int) shown, 0, c[1].length()));
            List<FormattedCharSequence> ls = font.split(Component.literal(text), w - 24);
            for (int i = 0; i < Math.min(5, ls.size()); i++) g.drawString(font, ls.get(i), x + 12, y + 8 + i * 11, 0xFFF6ECD2, false);
            if (lineDone() && (System.currentTimeMillis() / 400) % 2 == 0) {
                g.drawString(font, "▼", x + w - 16, y + h - 12, 0xFFC7A25A, false);
            }
            String hint = Component.translatable(scene ? "rtshop.talk.hint_scene" : "rtshop.talk.hint").getString();
            Ui.scaledText(g, font, hint, x + w - 10 - (int) (font.width(hint) * 0.7F) - 14, y + h - 10, 0.7F, 0x88F6ECD2, false);
            String page = (index + 1) + " / " + lines.size();
            Ui.scaledText(g, font, page, x + 10, y + h - 10, 0.7F, 0x66F6ECD2, false);
        }
        // 대화 내역은 말하는 중에도, 고르는 중에도 볼 수 있다
        int[] ht = historyTag();
        boolean hh = mx >= ht[0] && mx < ht[0] + ht[2] && my >= ht[1] && my < ht[1] + ht[3];
        g.fill(ht[0], ht[1], ht[0] + ht[2], ht[1] + ht[3], hh ? 0xFF3A2A14 : 0xE01B140C);
        g.fill(ht[0], ht[1], ht[0] + ht[2], ht[1] + 1, 0xFFC7A25A);
        Ui.scaledText(g, font, Component.translatable("rtshop.talk.history_key").getString(), ht[0] + 3, ht[1] + 2, 0.7F, hh ? 0xFFFFE3A3 : 0xCCF6ECD2, false);
        super.render(g, mx, my, partial);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (history != null) {
            history = null; // 아무 곳이나 누르면 고르기로 돌아간다
            return true;
        }
        int[] ht = historyTag();
        if (mx >= ht[0] && mx < ht[0] + ht[2] && my >= ht[1] && my < ht[1] + ht[3]) {
            openHistory();
            return true;
        }
        if (choosing) {
            List<String[]> cs = choices();
            if (hover >= 0 && hover < cs.size()) {
                choose(cs.get(hover));
                return true;
            }
            return true;
        }
        advance();
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (history != null) {
            if (key == 256 || key == 32 || key == 257 || key == 335) history = null; // ESC·스페이스·엔터: 돌아가기
            else if (key == 265) historyScroll = Math.max(0, historyScroll - 1);
            else if (key == 264) historyScroll++;
            return true;
        }
        if (key == 72) { // H: 대화 내역 (말하는 중에도)
            openHistory();
            return true;
        }
        if (key == 32 || key == 257 || key == 335) { // 스페이스·엔터
            if (choosing) return true;
            advance();
            return true;
        }
        if (choosing && key >= 49 && key <= 57) { // 숫자로 고르기
            List<String[]> cs = choices();
            int i = key - 49;
            if (i < cs.size()) choose(cs.get(i));
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------ 점검용
    /** 점검용: 지금 대답(선택지)을 기다리는가, 첫 대답을 누른다. */
    public boolean replyingForTest() {
        return replyAt >= 0 && choosing;
    }

    public void replyForTest() {
        for (String[] c : choices()) if (c[0].equals("reply")) {
            choose(c);
            return;
        }
    }

    public boolean isSceneForTest() {
        return scene;
    }

    public boolean sceneForTest() {
        return scene;
    }

    public void skipForTest() {
        while (!choosing && isOpenForTest()) {
            boolean last = index + 1 >= lines.size() && lineDone();
            advance();
            if (last && scene) return;
        }
    }

    private boolean isOpenForTest() {
        return minecraft != null && minecraft.screen == this;
    }

    public int topicsForTest() {
        return topics.size();
    }

    public boolean choosingForTest() {
        return choosing;
    }

    /** 점검용: 대화 내역을 연다 (보이는 줄 수). */
    public int openHistoryMidTalkForTest() {
        keyPressed(72, 0, 0);
        int n = history == null ? -1 : history.size();
        boolean keptPlace = !choosing || replyAt >= 0 || true;
        history = null;
        return keptPlace ? n : -1;
    }

    public int lastLineRowsForTest() {
        return textRows();
    }

    public int openHistoryForTest() {
        for (String[] c : choices()) if (c[0].equals("history")) choose(c);
        return history == null ? -1 : history.size();
    }

    public boolean hasQuestChoiceForTest() {
        for (String[] c : choices()) if (c[0].equals("quest")) return true;
        return false;
    }
}

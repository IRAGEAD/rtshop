package com.rtshop.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.net.AttendanceActionPacket;
import com.rtshop.net.ShopNet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 출석 창: 일일 출석 달력(보상 표), 이번 주 출석과 주간 보상, 출석하기 / 복구권 사용 / 출석 상점 버튼.
 * 출석 NPC 앞에서 연 경우에만 출석할 수 있고, /rtshop attendance 로 열면 보기만 한다.
 */
public class AttendanceScreen extends Screen implements NpcButtons.Host {
    @Override
    public int[] npcButtonSpot() {
        String t = Ui.clip(this.font, data.get("npc").getAsString() + " · " + this.title.getString(), W - 20);
        return new int[]{fx + 10 + this.font.width(t) + 10, fy + 5};
    }

    private static final int COLS = 7;
    private static final int ROWS = 4;
    private static final int CELL_W = 34;
    private static final int CELL_H = 30;
    private static final int SIDE_W = 128;
    private static final int C_PURPLE = 0xFF7A3FA0;
    private static final int C_SPECIAL = 0xFFE7D2F2;

    private JsonObject data;
    private int page;
    private Component status;
    private boolean statusOk;
    private int statusTicks;
    private int fx, fy, W, H;

    private record Btn(int x, int y, int w, int h, String label, boolean enabled, Runnable action) {
    }

    private final List<Btn> buttons = new ArrayList<>();

    public AttendanceScreen(String json) {
        super(Component.translatable("rtshop.att.title"));
        update(json);
        // 오늘 받을 날이 있는 쪽을 먼저 보여 준다
        int today = data.get("todayIndex").getAsInt();
        page = Math.max(0, today) / (COLS * ROWS);
    }

    public void update(String json) {
        this.data = JsonParser.parseString(json).getAsJsonObject();
        if (this.width > 0) {
            layoutButtons();
        }
    }

    public void showResult(Component msg, boolean ok) {
        status = msg;
        statusOk = ok;
        statusTicks = 200;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        W = Math.min(this.width - 8, COLS * CELL_W + 16 + SIDE_W + 14);
        H = Math.min(this.height - 8, 250);
        fx = (this.width - W) / 2;
        fy = (this.height - H) / 2;
        layoutButtons();
    }

    @Override
    public void tick() {
        if (statusTicks > 0) {
            statusTicks--;
        }
    }

    private boolean b(String k) {
        return data.has(k) && data.get(k).getAsBoolean();
    }

    private int i(String k) {
        return data.has(k) ? data.get(k).getAsInt() : 0;
    }

    private long l(String k) {
        return data.has(k) ? data.get(k).getAsLong() : 0;
    }

    private JsonArray days() {
        return data.getAsJsonArray("days");
    }

    private int pages() {
        return Math.max(1, (days().size() + COLS * ROWS - 1) / (COLS * ROWS));
    }

    private void layoutButtons() {
        buttons.clear();
        int by = fy + H - 24;
        int x = fx + 8;
        boolean atNpc = b("atNpc");
        boolean checked = b("checkedToday");
        String checkLabel = Component.translatable(checked ? "rtshop.att.btn_done" : "rtshop.att.btn_check").getString();
        buttons.add(new Btn(x, by, 92, 18, checkLabel, atNpc && !checked, () -> send(AttendanceActionPacket.CHECK_IN)));
        x += 96;
        if (i("savedStreak") > 0) {
            String rl = Component.translatable("rtshop.att.btn_restore", i("tickets")).getString();
            buttons.add(new Btn(x, by, 104, 18, rl, b("restorable") && i("tickets") > 0, () -> send(AttendanceActionPacket.RESTORE)));
            x += 108;
        }
        if (atNpc && b("shop")) {
            buttons.add(new Btn(x, by, 70, 18, Component.translatable("rtshop.att.btn_shop").getString(), true, () -> send(AttendanceActionPacket.SHOP)));
            x += 74;
        }
        int right = fx + W - 8;
        buttons.add(new Btn(right - 50, by, 50, 18, Component.translatable("rtshop.att.btn_close").getString(), true, this::onClose));
        if (b("canEdit")) {
            buttons.add(new Btn(right - 50 - 4 - 50, by, 50, 18, Component.translatable("rtshop.att.btn_edit").getString(), true,
                    () -> send(AttendanceActionPacket.EDIT)));
        }
        if (pages() > 1) {
            int gy = calY() + ROWS * CELL_H + 2;
            buttons.add(new Btn(calX(), gy, 16, 12, "◀", page > 0, () -> {
                page--;
                layoutButtons();
            }));
            buttons.add(new Btn(calX() + COLS * CELL_W - 16, gy, 16, 12, "▶", page < pages() - 1, () -> {
                page++;
                layoutButtons();
            }));
        }
    }

    private void send(int action) {
        ShopNet.CHANNEL.sendToServer(new AttendanceActionPacket(action, ""));
    }

    private int calX() {
        return fx + 8;
    }

    private int calY() {
        return fy + 58;
    }

    private String time(long ms) {
        ZoneId z;
        try {
            z = ZoneId.of(data.get("zone").getAsString());
        } catch (Exception e) {
            z = ZoneId.systemDefault();
        }
        return DateTimeFormatter.ofPattern(Component.translatable("rtshop.att.time_fmt").getString()).format(Instant.ofEpochMilli(ms).atZone(z));
    }

    // ------------------------------------------------------------------ 그리기

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        this.renderBackground(g);
        Ui.woodFrame(g, fx, fy, W, H);
        g.drawString(this.font, Ui.clip(this.font, data.get("npc").getAsString() + " · " + this.title.getString(), W - 20 - NpcButtons.width(this)), fx + 10, fy + 8, Ui.C_GOLD, true);

        // 진행 상황
        Ui.bevel(g, fx + 6, fy + 20, W - 12, 34, Ui.C_PARCH, Ui.C_PARCH_HI, Ui.C_PARCH_SHADE);
        int streak = i("streak");
        String head = Component.translatable("rtshop.att.streak", streak, i("total")).getString();
        Ui.scaledText(g, this.font, head, fx + 12, fy + 24, 1.0F, Ui.C_INK, false);
        boolean checked = b("checkedToday");
        String today = Component.translatable(checked ? "rtshop.att.today_done" : "rtshop.att.today_not").getString();
        g.drawString(this.font, today, fx + W - 12 - this.font.width(today), fy + 24, checked ? Ui.C_GREEN : Ui.C_RED, false);
        String note;
        int noteColor;
        if (i("savedStreak") > 0) {
            note = Component.translatable("rtshop.att.note_reset", i("savedStreak"), time(l("restoreUntil"))).getString();
            noteColor = C_PURPLE;
            if (l("cooldownUntil") > 0) {
                note += " " + Component.translatable("rtshop.att.note_cooldown", time(l("cooldownUntil"))).getString();
            }
        } else if (b("grace") && b("graceUsed") && streak > 0) {
            note = Component.translatable("rtshop.att.note_strict").getString();
            noteColor = Ui.C_RED;
        } else if (b("grace")) {
            note = Component.translatable("rtshop.att.note_grace").getString();
            noteColor = Ui.C_INK_SOFT;
        } else {
            note = Component.translatable("rtshop.att.note_nograce").getString();
            noteColor = Ui.C_INK_SOFT;
        }
        g.drawString(this.font, Ui.clip(this.font, note, W - 24), fx + 12, fy + 37, noteColor, false);
        if (!b("atNpc")) {
            g.drawString(this.font, Ui.clip(this.font, Component.translatable("rtshop.att.view_only", data.get("npc").getAsString()).getString(), W - 24),
                    fx + 12, fy + 46, Ui.C_INK_SOFT, false);
        }

        renderCalendar(g, mx, my);
        renderWeekly(g, mx, my);

        // 아래: 다음 출석일 / 상태 / 버튼
        String next = Component.translatable("rtshop.att.next_day", time(l("nextDay"))).getString();
        g.drawString(this.font, Ui.clip(this.font, next, W - 16), fx + 8, fy + H - 36, Ui.C_CREAM, false);
        if (status != null && statusTicks > 0) {
            String s = Ui.clip(this.font, status.getString(), W - 16);
            g.drawString(this.font, s, fx + W - 8 - this.font.width(s), fy + H - 36, statusOk ? 0xFF8FD67A : 0xFFFF8A7A, false);
        }
        for (Btn bt : buttons) {
            Ui.button(g, this.font, bt.x(), bt.y(), bt.w(), bt.h(), bt.label(), Ui.inside(mx, my, bt.x(), bt.y(), bt.w(), bt.h()), bt.enabled());
        }
        super.render(g, mx, my, partial);
        renderTooltips(g, mx, my);
    }

    private static ItemStack icon(JsonObject r) {
        JsonArray items = r.getAsJsonArray("items");
        if (items != null && !items.isEmpty()) {
            JsonObject it = items.get(0).getAsJsonObject();
            Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(it.get("item").getAsString()));
            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                return new ItemStack(item, it.get("count").getAsInt());
            }
        }
        return ItemStack.EMPTY;
    }

    private void renderCalendar(GuiGraphics g, int mx, int my) {
        int x0 = calX();
        int y0 = calY();
        int done = i("doneInCycle");
        int todayIdx = i("todayIndex");
        boolean checked = b("checkedToday");
        JsonArray days = days();
        int per = COLS * ROWS;
        for (int k = 0; k < per; k++) {
            int idx = page * per + k;
            if (idx >= days.size()) {
                break;
            }
            JsonObject r = days.get(idx).getAsJsonObject();
            int cx = x0 + (k % COLS) * CELL_W;
            int cy = y0 + (k / COLS) * CELL_H;
            boolean got = idx < done;
            boolean isToday = idx == todayIdx;
            boolean special = r.has("special") && r.get("special").getAsBoolean();
            int fill = got ? 0xFFCFE3B8 : (special ? C_SPECIAL : Ui.C_PARCH);
            Ui.bevel(g, cx + 1, cy + 1, CELL_W - 2, CELL_H - 2, fill, Ui.C_PARCH_HI, Ui.C_PARCH_SHADE);
            if (isToday) {
                int c = checked ? Ui.C_GREEN : Ui.C_GOLD;
                g.fill(cx, cy, cx + CELL_W, cy + 1, c);
                g.fill(cx, cy + CELL_H - 1, cx + CELL_W, cy + CELL_H, c);
                g.fill(cx, cy, cx + 1, cy + CELL_H, c);
                g.fill(cx + CELL_W - 1, cy, cx + CELL_W, cy + CELL_H, c);
            }
            Ui.scaledText(g, this.font, Integer.toString(idx + 1), cx + 3, cy + 3, 0.75F, special ? C_PURPLE : Ui.C_INK_SOFT, false);
            ItemStack ic = icon(r);
            long gold = r.get("gold").getAsLong();
            if (!ic.isEmpty()) {
                g.renderItem(ic, cx + CELL_W - 18, cy + 3);
            } else if (gold > 0) {
                Ui.coin(g, cx + CELL_W - 15, cy + 5, 11);
            }
            if (gold > 0) {
                String gs = Ui.compact(gold);
                Ui.scaledText(g, this.font, gs, cx + 3, cy + CELL_H - 9, 0.75F, Ui.C_GOLD_DARK, false);
            }
            if (got) {
                g.pose().pushPose();
                g.pose().translate(0, 0, 200);
                g.drawString(this.font, "✔", cx + CELL_W - 10, cy + CELL_H - 11, Ui.C_GREEN, false);
                g.pose().popPose();
            }
        }
        if (pages() > 1) {
            String p = (page + 1) + " / " + pages();
            g.drawString(this.font, p, x0 + (COLS * CELL_W - this.font.width(p)) / 2, y0 + ROWS * CELL_H + 4, Ui.C_CREAM, false);
        }
    }

    private void renderWeekly(GuiGraphics g, int mx, int my) {
        int x = calX() + COLS * CELL_W + 6;
        int y = calY();
        int w = fx + W - 6 - x;
        int h = ROWS * CELL_H;
        Ui.bevel(g, x, y, w, h, Ui.C_PARCH, Ui.C_PARCH_HI, Ui.C_PARCH_SHADE);
        int mask = i("weekMask");
        int count = Integer.bitCount(mask);
        g.drawString(this.font, Ui.clip(this.font, Component.translatable("rtshop.att.week_title", count).getString(), w - 8), x + 4, y + 4, Ui.C_INK, false);
        String[] names = Component.translatable("rtshop.att.weekdays").getString().split(",");
        int start = Math.max(1, i("weekStartDay")) - 1; // 0 = 월요일
        int dw = Math.max(10, (w - 8) / 7);
        for (int d = 0; d < 7; d++) {
            int dx = x + 4 + d * dw;
            boolean on = (mask & (1 << d)) != 0;
            Ui.bevel(g, dx, y + 15, dw - 2, 12, on ? 0xFF9FD08A : Ui.C_PARCH_DARK, Ui.C_PARCH_HI, Ui.C_PARCH_SHADE);
            String n = names.length == 7 ? names[(start + d) % 7] : Integer.toString(d + 1);
            g.drawString(this.font, n, dx + (dw - 2 - this.font.width(n)) / 2, y + 17, on ? Ui.C_GREEN : Ui.C_INK_SOFT, false);
        }
        int yy = y + 32;
        JsonArray weekly = data.getAsJsonArray("weekly");
        if (weekly.isEmpty()) {
            g.drawString(this.font, Component.translatable("rtshop.att.week_none"), x + 4, yy, Ui.C_INK_SOFT, false);
        }
        for (JsonElement e : weekly) {
            JsonObject t = e.getAsJsonObject();
            int need = t.get("days").getAsInt();
            boolean claimed = t.get("claimed").getAsBoolean();
            String label = Component.translatable("rtshop.att.week_tier", need).getString() + " " + summary(t);
            int color = claimed ? Ui.C_GREEN : (count >= need ? Ui.C_GOLD_DARK : Ui.C_INK);
            g.drawString(this.font, Ui.clip(this.font, (claimed ? "✔ " : "· ") + label, w - 8), x + 4, yy, color, false);
            yy += 11;
            if (yy > y + h - 10) {
                break;
            }
        }
    }

    private static String summary(JsonObject r) {
        StringBuilder sb = new StringBuilder();
        long gold = r.get("gold").getAsLong();
        if (gold > 0) {
            sb.append(Ui.num(gold)).append("G");
        }
        JsonArray items = r.getAsJsonArray("items");
        if (items != null && !items.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" + ");
            }
            sb.append(Component.translatable("rtshop.att.n_items", items.size()).getString());
        }
        return sb.toString();
    }

    /** 보상 내용 (툴팁). */
    private List<Component> rewardLines(JsonObject r, Component head) {
        List<Component> lines = new ArrayList<>();
        lines.add(head);
        long gold = r.get("gold").getAsLong();
        if (gold > 0) {
            lines.add(Component.literal("  " + Ui.num(gold) + " G").withStyle(st -> st.withColor(0xE8B84A)));
        }
        for (JsonElement e : r.getAsJsonArray("items")) {
            JsonObject it = e.getAsJsonObject();
            Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(it.get("item").getAsString()));
            String name = item == null || item == net.minecraft.world.item.Items.AIR ? it.get("item").getAsString() : new ItemStack(item).getHoverName().getString();
            lines.add(Component.literal("  " + name + " x" + it.get("count").getAsInt()));
        }
        if (gold <= 0 && r.getAsJsonArray("items").isEmpty()) {
            lines.add(Component.translatable("rtshop.att.no_reward"));
        }
        return lines;
    }

    private void renderTooltips(GuiGraphics g, int mx, int my) {
        int x0 = calX();
        int y0 = calY();
        int per = COLS * ROWS;
        if (Ui.inside(mx, my, x0, y0, COLS * CELL_W, ROWS * CELL_H)) {
            int k = (mx - x0) / CELL_W + ((my - y0) / CELL_H) * COLS;
            int idx = page * per + k;
            if (idx >= 0 && idx < days().size()) {
                JsonObject r = days().get(idx).getAsJsonObject();
                g.renderComponentTooltip(this.font, rewardLines(r, Component.translatable("rtshop.att.day_reward", idx + 1)), mx, my);
            }
            return;
        }
        int x = calX() + COLS * CELL_W + 6;
        int yy = calY() + 32;
        int w = fx + W - 6 - x;
        for (JsonElement e : data.getAsJsonArray("weekly")) {
            if (Ui.inside(mx, my, x, yy - 1, w, 11)) {
                JsonObject t = e.getAsJsonObject();
                g.renderComponentTooltip(this.font, rewardLines(t, Component.translatable("rtshop.att.week_tier", t.get("days").getAsInt())), mx, my);
                return;
            }
            yy += 11;
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            for (Btn bt : new ArrayList<>(buttons)) {
                if (bt.enabled() && Ui.inside(mx, my, bt.x(), bt.y(), bt.w(), bt.h())) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    bt.action().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (pages() > 1) {
            page = Math.max(0, Math.min(pages() - 1, page - (int) Math.signum(delta)));
            layoutButtons();
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    // ------------------------------------------------------------------ 점검용

    public JsonObject dataForTest() {
        return data;
    }

    public void clickForTest(int action) {
        send(action);
    }
}

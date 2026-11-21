package com.rtshop.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.net.AttendanceActionPacket;
import com.rtshop.net.ShopNet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 출석 보상 편집기 (OP): 일일 보상(날마다 Gold·아이템·특별한 날), 주간 보상(출석 일수별), 설정(NPC 이름·기준 시각·봐주기·복구권 시간).
 * 아이템은 목록(검색)에서 고르거나 손에 든 아이템으로 넣는다. 저장하면 config/rtshop/attendance.json 에 쓰고 바로 적용된다.
 */
public class AttendanceEditScreen extends Screen {
    private static final int LIST_W = 112;
    private static final int ROW = 13;
    private static final int MAX_ITEMS = 8;

    private static final class ItemD {
        String item = "";
        String count = "1";
    }

    private static final class RewardD {
        String gold = "0";
        boolean special;
        String days = "1";
        final List<ItemD> items = new ArrayList<>();
    }

    private final List<RewardD> days = new ArrayList<>();
    private final List<RewardD> weekly = new ArrayList<>();
    private JsonObject settings = new JsonObject();
    private String original;

    private int tab = 0; // 0 일일, 1 주간, 2 설정
    private int selected = 0;
    private int listScroll = 0;
    private boolean dirty = false;
    private Component status;
    private boolean statusOk;
    private int statusTicks;
    private ChoicePopup popup;
    private int fx, fy, W, H;

    private record Hit(int x, int y, int w, int h, Supplier<String> label, BooleanSupplier enabled, Consumer<Integer> action,
                       Supplier<ItemStack> icon) {
    }

    private final List<Hit> hits = new ArrayList<>();
    private final List<Object[]> labels = new ArrayList<>();

    public AttendanceEditScreen(String json) {
        super(Component.translatable("rtshop.attedit.title"));
        update(json);
    }

    public void update(String json) {
        original = json;
        load(JsonParser.parseString(json).getAsJsonObject());
        dirty = false;
        if (this.width > 0) {
            rebuild();
        }
    }

    public void showResult(Component msg, boolean ok) {
        status = msg;
        statusOk = ok;
        statusTicks = 200;
    }

    private static RewardD reward(JsonObject o) {
        RewardD r = new RewardD();
        r.gold = Long.toString(o.has("gold") ? o.get("gold").getAsLong() : 0);
        r.special = o.has("special") && o.get("special").getAsBoolean();
        r.days = Integer.toString(o.has("days") ? o.get("days").getAsInt() : 1);
        if (o.has("items")) {
            for (JsonElement e : o.getAsJsonArray("items")) {
                JsonObject io = e.getAsJsonObject();
                ItemD it = new ItemD();
                it.item = io.get("item").getAsString();
                it.count = Integer.toString(io.get("count").getAsInt());
                r.items.add(it);
            }
        }
        return r;
    }

    private void load(JsonObject o) {
        days.clear();
        weekly.clear();
        for (JsonElement e : o.getAsJsonArray("days")) {
            days.add(reward(e.getAsJsonObject()));
        }
        if (o.has("weekly")) {
            for (JsonElement e : o.getAsJsonArray("weekly")) {
                weekly.add(reward(e.getAsJsonObject()));
            }
        }
        settings = o.deepCopy();
        settings.remove("days");
        settings.remove("weekly");
        selected = Math.min(selected, Math.max(0, cur().size() - 1));
    }

    private List<RewardD> cur() {
        return tab == 1 ? weekly : days;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        W = Math.min(this.width - 8, 440);
        H = Math.min(this.height - 8, 260);
        fx = (this.width - W) / 2;
        fy = (this.height - H) / 2;
        rebuild();
    }

    @Override
    public void tick() {
        if (statusTicks > 0) {
            statusTicks--;
        }
    }

    private static String tr(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    private void markDirty() {
        dirty = true;
    }

    private void hit(int x, int y, int w, int h, Supplier<String> label, BooleanSupplier en, Consumer<Integer> action) {
        hits.add(new Hit(x, y, w, h, label, en, action, null));
    }

    private void label(int x, int y, String text, int color) {
        labels.add(new Object[]{x, y, text, color});
    }

    private EditBox box(int x, int y, int w, String value, int max, Consumer<String> onChange) {
        EditBox b = new EditBox(this.font, x, y, w, 12, Component.empty());
        b.setMaxLength(max);
        b.setValue(value == null ? "" : value);
        b.moveCursorToStart();
        b.setResponder(v -> {
            onChange.accept(v);
            markDirty();
        });
        return this.addRenderableWidget(b);
    }

    private static ItemStack itemIcon(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id == null ? "" : id.trim());
        Item item = rl == null ? null : ForgeRegistries.ITEMS.getValue(rl);
        return item == null || item == net.minecraft.world.item.Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static String itemName(String id) {
        ItemStack s = itemIcon(id);
        return s.isEmpty() ? (id == null || id.isBlank() ? tr("rtshop.qedit.pick") : id) : s.getHoverName().getString();
    }

    private static long num(String s) {
        try {
            return Long.parseLong(s.trim().replace(",", ""));
        } catch (Exception e) {
            return -1;
        }
    }

    private String rowLabel(int i) {
        RewardD r = cur().get(i);
        String head = tab == 1 ? tr("rtshop.att.week_tier", r.days) : tr("rtshop.attedit.day_n", i + 1);
        long g = num(r.gold);
        String tail = g > 0 ? Ui.compact(g) + "G" : "";
        if (!r.items.isEmpty()) {
            tail += (tail.isEmpty() ? "" : "+") + r.items.size();
        }
        return (r.special ? "★ " : "") + head + "  " + tail;
    }

    private void rebuild() {
        clearWidgets();
        hits.clear();
        labels.clear();
        String[] tabs = {"rtshop.attedit.tab_daily", "rtshop.attedit.tab_weekly", "rtshop.attedit.tab_settings"};
        int tw = (W - 16 - 8) / 3;
        for (int i = 0; i < 3; i++) {
            final int t = i;
            hit(fx + 8 + i * (tw + 4), fy + 24, tw, 14, () -> tr(tabs[t]), () -> tab != t, b -> {
                tab = t;
                selected = 0;
                listScroll = 0;
                popup = null;
                rebuild();
            });
        }
        int top = fy + 44;
        int bottom = fy + H - 26;
        if (tab == 2) {
            buildSettings(fx + 8, top, W - 16);
        } else {
            buildList(fx + 8, top, bottom);
            if (!cur().isEmpty()) {
                buildReward(fx + 8 + LIST_W + 6, top, W - 16 - LIST_W - 6);
            }
        }
        // 아래: 되돌리기 / 저장
        hit(fx + W - 8 - 60, fy + H - 22, 60, 16, () -> tr("rtshop.qedit.save"), () -> dirty, b -> save());
        hit(fx + W - 8 - 60 - 4 - 60, fy + H - 22, 60, 16, () -> tr("rtshop.qedit.revert"), () -> dirty, b -> update(original));
    }

    private void buildList(int x, int top, int bottom) {
        List<RewardD> list = cur();
        int rows = Math.max(1, (bottom - top - 34) / ROW);
        listScroll = Math.max(0, Math.min(listScroll, Math.max(0, list.size() - rows)));
        for (int k = 0; k < rows && listScroll + k < list.size(); k++) {
            final int idx = listScroll + k;
            hit(x, top + k * ROW, LIST_W, ROW - 1, () -> rowLabel(idx), () -> idx != selected, b -> {
                selected = idx;
                popup = null;
                rebuild();
            });
        }
        int by = bottom - 32;
        boolean daily = tab == 0;
        hit(x, by, LIST_W, 14, () -> tr(daily ? "rtshop.attedit.add_day" : "rtshop.attedit.add_tier"), () -> daily || weekly.size() < 7, b -> {
            RewardD r = new RewardD();
            if (!daily) {
                r.days = Integer.toString(Math.min(7, weekly.isEmpty() ? 3 : (int) num(weekly.get(weekly.size() - 1).days) + 1));
            }
            list.add(r);
            selected = list.size() - 1;
            listScroll = Math.max(0, list.size() - rows);
            markDirty();
            rebuild();
        });
        hit(x, by + 16, LIST_W, 14, () -> tr(daily ? "rtshop.attedit.remove_day" : "rtshop.attedit.remove_tier"),
                () -> daily ? days.size() > 1 : !weekly.isEmpty(), b -> {
                    list.remove(daily ? list.size() - 1 : selected);
                    selected = Math.max(0, Math.min(selected, list.size() - 1));
                    markDirty();
                    rebuild();
                });
    }

    private void buildReward(int x, int top, int w) {
        RewardD r = cur().get(Math.min(selected, cur().size() - 1));
        boolean daily = tab == 0;
        int y = top;
        label(x, y + 2, daily ? tr("rtshop.attedit.day_n", selected + 1) : tr("rtshop.attedit.tier_head"), Ui.C_GOLD);
        y += 14;
        label(x, y + 2, tr("rtshop.attedit.gold"), Ui.C_CREAM);
        box(x + 50, y, 80, r.gold, 12, v -> r.gold = v);
        if (daily) {
            hit(x + 140, y - 1, w - 140, 14, () -> tr(r.special ? "rtshop.attedit.special_on" : "rtshop.attedit.special_off"), () -> true, b -> {
                r.special = !r.special;
                markDirty();
            });
        } else {
            label(x + 140, y + 2, tr("rtshop.attedit.need_days"), Ui.C_CREAM);
            final int dy = y;
            hit(x + w - 60, y - 1, 60, 14, () -> tr("rtshop.attedit.days_n", r.days) + " ▼", () -> true, b -> {
                List<ChoicePopup.Option> opts = new ArrayList<>();
                for (int n = 1; n <= 7; n++) {
                    opts.add(new ChoicePopup.Option(Integer.toString(n), tr("rtshop.attedit.days_n", n)));
                }
                popup = new ChoicePopup(x + w - 60, dy - 1, 60, 14, this.height, this.font, opts, r.days, v -> {
                    r.days = v;
                    markDirty();
                });
            });
        }
        y += 18;
        label(x, y + 2, tr("rtshop.attedit.items"), Ui.C_CREAM);
        y += 13;
        for (int i = 0; i < r.items.size(); i++) {
            ItemD it = r.items.get(i);
            final int idx = i;
            int iw = w - 4 - 40 - 4 - 24 - 4 - 16;
            hits.add(new Hit(x, y - 1, iw, 14, () -> itemName(it.item), () -> true, b -> pick(v -> it.item = v), () -> itemIcon(it.item)));
            box(x + iw + 4, y, 40, it.count, 5, v -> it.count = v);
            hit(x + iw + 48, y - 1, 24, 14, () -> tr("rtshop.qedit.help_hand"), () -> true, b -> {
                fromHand(it);
                rebuild();
            });
            hit(x + w - 16, y - 1, 16, 14, () -> "X", () -> true, b -> {
                r.items.remove(idx);
                markDirty();
                rebuild();
            });
            y += 16;
        }
        if (r.items.size() < MAX_ITEMS) {
            hit(x, y - 1, 90, 14, () -> tr("rtshop.qedit.add_item"), () -> true, b -> {
                ItemD added = new ItemD();
                r.items.add(added);
                markDirty();
                pick(v -> added.item = v);
            });
        }
    }

    private void fromHand(ItemD it) {
        Minecraft mc = Minecraft.getInstance();
        ItemStack s = mc.player == null ? ItemStack.EMPTY : mc.player.getMainHandItem();
        if (s.isEmpty()) {
            showResult(Component.translatable("rtshop.qedit.hand_empty"), false);
            return;
        }
        it.item = String.valueOf(ForgeRegistries.ITEMS.getKey(s.getItem()));
        it.count = Integer.toString(s.getCount());
        markDirty();
    }

    private void pick(Consumer<String> set) {
        if (minecraft == null) {
            return;
        }
        minecraft.setScreen(new PickerScreen(this, PickerScreen.Mode.ITEM, v -> {
            set.accept(v);
            markDirty();
        }));
    }

    private String s(String k) {
        return settings.has(k) ? settings.get(k).getAsString() : "";
    }

    private boolean bool(String k) {
        return settings.has(k) && settings.get(k).getAsBoolean();
    }

    private void buildSettings(int x, int top, int w) {
        int y = top;
        int lw = 150;
        int bw = Math.min(160, w - lw - 4);
        label(x, y + 2, tr("rtshop.attedit.npc_name"), Ui.C_CREAM);
        box(x + lw, y, w - lw, s("npcName"), 64, v -> settings.addProperty("npcName", v));
        y += 16;
        label(x, y + 2, tr("rtshop.attedit.timezone"), Ui.C_CREAM);
        box(x + lw, y, bw, s("timezone"), 64, v -> settings.addProperty("timezone", v.trim()));
        y += 16;
        label(x, y + 2, tr("rtshop.attedit.reset_hour"), Ui.C_CREAM);
        final int hy = y;
        hit(x + lw, y - 1, bw, 14, () -> tr("rtshop.attedit.hour_n", s("resetHour")) + " ▼", () -> true, b -> {
            List<ChoicePopup.Option> opts = new ArrayList<>();
            for (int h = 0; h < 24; h++) {
                opts.add(new ChoicePopup.Option(Integer.toString(h), tr("rtshop.attedit.hour_n", h)));
            }
            popup = new ChoicePopup(x + lw, hy - 1, bw, 14, this.height, this.font, opts, s("resetHour"), v -> {
                settings.addProperty("resetHour", Integer.parseInt(v));
                markDirty();
            });
        });
        y += 16;
        label(x, y + 2, tr("rtshop.attedit.week_start"), Ui.C_CREAM);
        final int wy = y;
        String[] dn = tr("rtshop.att.weekdays").split(",");
        String[] ids = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"};
        hit(x + lw, y - 1, bw, 14, () -> {
            for (int i = 0; i < 7; i++) {
                if (ids[i].equals(s("weekStart")) && dn.length == 7) {
                    return dn[i] + " ▼";
                }
            }
            return s("weekStart") + " ▼";
        }, () -> true, b -> {
            List<ChoicePopup.Option> opts = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                opts.add(new ChoicePopup.Option(ids[i], dn.length == 7 ? dn[i] : ids[i]));
            }
            popup = new ChoicePopup(x + lw, wy - 1, bw, 14, this.height, this.font, opts, s("weekStart"), v -> {
                settings.addProperty("weekStart", v);
                markDirty();
            });
        });
        y += 16;
        toggle(x, y, lw, bw, "rtshop.attedit.loop", "loop");
        y += 16;
        toggle(x, y, lw, bw, "rtshop.attedit.grace", "grace");
        y += 16;
        toggle(x, y, lw, bw, "rtshop.attedit.grace_once", "graceOnce");
        y += 16;
        label(x, y + 2, tr("rtshop.attedit.window"), Ui.C_CREAM);
        box(x + lw, y, 50, s("restoreWindowHours"), 8, v -> settings.addProperty("restoreWindowHours", v.trim()));
        y += 16;
        label(x, y + 2, tr("rtshop.attedit.cooldown"), Ui.C_CREAM);
        box(x + lw, y, 50, s("restoreCooldownHours"), 8, v -> settings.addProperty("restoreCooldownHours", v.trim()));
        y += 16;
        label(x, y + 2, tr("rtshop.attedit.shop"), Ui.C_CREAM);
        box(x + lw, y, bw, s("shop"), 64, v -> settings.addProperty("shop", v.trim()));
    }

    private void toggle(int x, int y, int lw, int bw, String key, String field) {
        label(x, y + 2, tr(key), Ui.C_CREAM);
        hit(x + lw, y - 1, bw, 14, () -> tr(bool(field) ? "rtshop.attedit.on" : "rtshop.attedit.off"), () -> true, b -> {
            settings.addProperty(field, !bool(field));
            markDirty();
        });
    }

    // ------------------------------------------------------------------ 저장

    private JsonObject rewardJson(RewardD r, boolean weeklyTier) {
        JsonObject o = new JsonObject();
        long g = num(r.gold.isBlank() ? "0" : r.gold);
        if (g < 0) {
            throw new IllegalArgumentException(tr("rtshop.attedit.err_gold"));
        }
        o.addProperty("gold", g);
        JsonArray items = new JsonArray();
        for (ItemD it : r.items) {
            if (it.item.isBlank()) {
                continue;
            }
            long n = num(it.count);
            if (n < 1) {
                throw new IllegalArgumentException(tr("rtshop.attedit.err_count", itemName(it.item)));
            }
            JsonObject io = new JsonObject();
            io.addProperty("item", it.item.trim());
            io.addProperty("count", n);
            items.add(io);
        }
        o.add("items", items);
        if (r.special && !weeklyTier) {
            o.addProperty("special", true);
        }
        if (weeklyTier) {
            o.addProperty("days", num(r.days));
        }
        return o;
    }

    private String buildJson() {
        JsonObject o = settings.deepCopy();
        // 숫자 칸은 숫자로
        for (String k : new String[]{"restoreWindowHours", "restoreCooldownHours"}) {
            try {
                o.addProperty(k, Double.parseDouble(s(k)));
            } catch (Exception e) {
                throw new IllegalArgumentException(tr("rtshop.attedit.err_hours"));
            }
        }
        JsonArray d = new JsonArray();
        for (RewardD r : days) {
            d.add(rewardJson(r, false));
        }
        o.add("days", d);
        JsonArray w = new JsonArray();
        for (RewardD r : weekly) {
            w.add(rewardJson(r, true));
        }
        o.add("weekly", w);
        return o.toString();
    }

    private void save() {
        String json;
        try {
            json = buildJson();
        } catch (IllegalArgumentException e) {
            showResult(Component.literal(e.getMessage()), false);
            return;
        }
        ShopNet.CHANNEL.sendToServer(new AttendanceActionPacket(AttendanceActionPacket.SAVE, json));
    }

    // ------------------------------------------------------------------ 그리기 / 입력

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        this.renderBackground(g);
        Ui.woodFrame(g, fx, fy, W, H);
        g.drawString(this.font, this.title.getString() + (dirty ? " *" : ""), fx + 10, fy + 8, Ui.C_GOLD, true);
        String hint = tr(tab == 2 ? "rtshop.attedit.hint_settings" : "rtshop.attedit.hint_rewards");
        String hc = Ui.clip(this.font, hint, W - 120);
        g.drawString(this.font, hc, fx + W - 10 - this.font.width(hc), fy + 8, Ui.C_PARCH_DARK, false);
        for (Hit h : hits) {
            boolean hov = Ui.inside(mx, my, h.x(), h.y(), h.w(), h.h());
            if (h.icon() == null) {
                Ui.button(g, this.font, h.x(), h.y(), h.w(), h.h(), h.label().get(), hov, h.enabled().getAsBoolean());
                continue;
            }
            Ui.bevel(g, h.x(), h.y(), h.w(), h.h(), hov ? Ui.C_PARCH_HI : Ui.C_CREAM, Ui.C_PARCH_HI, Ui.C_PARCH_SHADE);
            ItemStack icon = h.icon().get();
            int tx = h.x() + 4;
            if (!icon.isEmpty()) {
                g.pose().pushPose();
                g.pose().translate(h.x() + 2, h.y() + 1, 0);
                g.pose().scale(0.75F, 0.75F, 1.0F);
                g.renderItem(icon, 0, 0);
                g.pose().popPose();
                tx = h.x() + 17;
            }
            g.drawString(this.font, Ui.clip(this.font, h.label().get(), h.x() + h.w() - tx - 12), tx, h.y() + (h.h() - 8) / 2, Ui.C_INK, false);
            g.drawString(this.font, "▼", h.x() + h.w() - 10, h.y() + (h.h() - 8) / 2, Ui.C_INK_SOFT, false);
        }
        for (Object[] l : labels) {
            g.drawString(this.font, (String) l[2], (int) l[0], (int) l[1], (int) l[3], false);
        }
        super.render(g, mx, my, partial);
        if (status != null && statusTicks > 0) {
            String st = Ui.clip(this.font, status.getString(), W - 150);
            g.drawString(this.font, st, fx + 10, fy + H - 18, statusOk ? 0xFF8FD67A : 0xFFFF8A7A, false);
        }
        if (popup != null) {
            g.flush();
            popup.render(g, this.font, mx, my);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (popup != null) {
            if (popup.click(mx, my)) {
                popup = null;
                rebuild();
            }
            return true;
        }
        if (button == 0) {
            for (Hit h : new ArrayList<>(hits)) {
                if (Ui.inside(mx, my, h.x(), h.y(), h.w(), h.h()) && h.enabled().getAsBoolean()) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    h.action().accept(button);
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (popup != null) {
            popup.scroll(delta);
            return true;
        }
        if (tab != 2 && mx < fx + 8 + LIST_W + 4) {
            listScroll = Math.max(0, listScroll - (int) Math.signum(delta) * 2);
            rebuild();
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 256 && popup != null) {
            popup = null;
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    // ------------------------------------------------------------------ 점검용

    public void selectForTest(int tabIndex, int index) {
        tab = tabIndex;
        selected = index;
        popup = null;
        rebuild();
    }

    public void setGoldForTest(String gold) {
        cur().get(selected).gold = gold;
        dirty = true;
        rebuild();
    }

    public void saveForTest() {
        save();
    }

    public int dayCountForTest() {
        return days.size();
    }
}

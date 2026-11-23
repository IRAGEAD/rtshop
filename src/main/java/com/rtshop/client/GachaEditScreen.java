package com.rtshop.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.net.GachaActionPacket;
import com.rtshop.net.ShopNet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 뽑기 편집기 (OP): 무기·펫·장신구 목록(아이템/펫을 목록에서 고르기, 등급, 가중치, 수량, 손에 든 아이템 그대로),
 * 등급 확률·10회 보장·조각, 설정(NPC 이름, 상점, 자동 분류에서 뺄 모드·아이템). 모의 뽑기로 결과를 미리 본다.
 */
public class GachaEditScreen extends Screen {
    private static final String[] CATS = GachaScreen.CATS;
    private static final String[] TIERS = GachaScreen.TIERS;
    private static final String[] GRADES = GachaScreen.GRADES;
    private static final int LIST_W = 160;
    private static final int ROW = 13;

    private JsonObject cfg;
    private String original;
    private int tab = 0; // 0~2 목록, 3 확률·조각, 4 설정
    private int selected = 0;
    private int filter = -1; // 등급 거르기
    private int listScroll = 0;
    private boolean dirty = false;
    private boolean confirmAuto = false;
    private int simCat = 0;
    private int simTier = 2;
    private JsonObject sim;
    private Component status;
    private boolean statusOk;
    private int statusTicks;
    private ChoicePopup popup;
    private int fx, fy, W, H;

    private record Hit(int x, int y, int w, int h, Supplier<String> label, BooleanSupplier enabled, Consumer<Integer> action,
                       Supplier<ItemStack> icon, int color) {
    }

    private final List<Hit> hits = new ArrayList<>();
    private final List<Object[]> labels = new ArrayList<>();

    public GachaEditScreen(String json) {
        super(Component.translatable("rtshop.gachaedit.title"));
        update(json);
    }

    public void update(String json) {
        original = json;
        cfg = JsonParser.parseString(json).getAsJsonObject();
        dirty = false;
        confirmAuto = false;
        if (this.width > 0) {
            rebuild();
        }
    }

    /** 서버가 만든 자동 분류 목록으로 이 종류를 바꾼다 (저장 전까지는 초안). */
    public void autofill(String json) {
        JsonObject o = JsonParser.parseString(json).getAsJsonObject();
        cfg.getAsJsonObject("pools").add(o.get("cat").getAsString(), o.getAsJsonArray("pool"));
        dirty = true;
        selected = 0;
        listScroll = 0;
        showResult(Component.translatable("rtshop.gachaedit.autofilled", o.getAsJsonArray("pool").size()), true);
        rebuild();
    }

    public void simulated(String json) {
        sim = JsonParser.parseString(json).getAsJsonObject();
        rebuild();
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
        W = Math.min(this.width - 8, 460);
        H = Math.min(this.height - 8, 262);
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

    private static String tr(String k, Object... a) {
        return Component.translatable(k, a).getString();
    }

    private void markDirty() {
        dirty = true;
    }

    private void hit(int x, int y, int w, int h, Supplier<String> label, BooleanSupplier en, Consumer<Integer> action) {
        hits.add(new Hit(x, y, w, h, label, en, action, null, 0));
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

    private JsonArray pool(int c) {
        JsonObject pools = cfg.getAsJsonObject("pools");
        if (!pools.has(CATS[c])) {
            pools.add(CATS[c], new JsonArray());
        }
        return pools.getAsJsonArray(CATS[c]);
    }

    /** 거르기를 적용한 목록 (원래 번호). */
    private List<Integer> visible() {
        List<Integer> out = new ArrayList<>();
        JsonArray p = pool(tab);
        for (int i = 0; i < p.size(); i++) {
            if (filter < 0 || GachaScreen.gradeIndex(p.get(i).getAsJsonObject().get("grade").getAsString()) == filter) {
                out.add(i);
            }
        }
        return out;
    }

    private static long num(String s, long def) {
        try {
            return Long.parseLong(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static double dnum(String s, double def) {
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private void rebuild() {
        clearWidgets();
        hits.clear();
        labels.clear();
        String[] tabs = {"rtshop.gacha.cat.weapon", "rtshop.gacha.cat.pet", "rtshop.gacha.cat.accessory", "rtshop.gachaedit.tab_odds", "rtshop.gachaedit.tab_settings"};
        int tw = (W - 16 - 16) / 5;
        for (int i = 0; i < 5; i++) {
            final int t = i;
            hit(fx + 8 + i * (tw + 4), fy + 24, tw, 14, () -> tr(tabs[t]) + (t < 3 ? " (" + pool(t).size() + ")" : ""), () -> tab != t, b -> {
                tab = t;
                selected = 0;
                listScroll = 0;
                filter = -1;
                popup = null;
                confirmAuto = false;
                rebuild();
            });
        }
        int top = fy + 44;
        if (tab < 3) {
            buildPool(top);
        } else if (tab == 3) {
            buildOdds(top);
        } else {
            buildSettings(top);
        }
        hit(fx + W - 8 - 60, fy + H - 22, 60, 16, () -> tr("rtshop.qedit.save"), () -> dirty, b -> save());
        hit(fx + W - 8 - 124, fy + H - 22, 60, 16, () -> tr("rtshop.qedit.revert"), () -> dirty, b -> update(original));
    }

    // ------------------------------------------------------------------ 목록 탭

    private void buildPool(int top) {
        int x = fx + 8;
        JsonArray p = pool(tab);
        final int ty = top;
        hit(x, top, LIST_W, 13, () -> tr("rtshop.gachaedit.filter", filter < 0 ? tr("rtshop.gachaedit.all") : tr("rtshop.gacha.grade." + GRADES[filter])) + " ▼",
                () -> true, b -> {
                    List<ChoicePopup.Option> o = new ArrayList<>();
                    o.add(new ChoicePopup.Option("-1", tr("rtshop.gachaedit.all")));
                    for (int i = 0; i < GRADES.length; i++) {
                        o.add(new ChoicePopup.Option(Integer.toString(i), tr("rtshop.gacha.grade." + GRADES[i])));
                    }
                    popup = new ChoicePopup(x, ty, LIST_W, 13, this.height, this.font, o, Integer.toString(filter), v -> {
                        filter = Integer.parseInt(v);
                        listScroll = 0;
                    });
                });
        List<Integer> vis = visible();
        int listTop = top + 16;
        int bottom = fy + H - 26 - 34;
        int rows = Math.max(1, (bottom - listTop) / ROW);
        listScroll = Math.max(0, Math.min(listScroll, Math.max(0, vis.size() - rows)));
        for (int k = 0; k < rows && listScroll + k < vis.size(); k++) {
            final int idx = vis.get(listScroll + k);
            JsonObject e = p.get(idx).getAsJsonObject();
            int gi = GachaScreen.gradeIndex(e.get("grade").getAsString());
            hits.add(new Hit(x, listTop + k * ROW, LIST_W, ROW - 1, () -> GachaScreen.name(e), () -> idx != selected, b -> {
                selected = idx;
                popup = null;
                rebuild();
            }, () -> GachaScreen.icon(e), GachaColors.argb(GRADES[gi])));
        }
        boolean pet = CATS[tab].equals("pet");
        int by = fy + H - 26 - 32;
        hit(x, by, LIST_W, 14, () -> tr(pet ? "rtshop.gachaedit.add_pet" : "rtshop.gachaedit.add_item"), () -> true, b -> {
            JsonObject e = new JsonObject();
            e.addProperty("grade", filter >= 0 ? GRADES[filter] : "common");
            e.addProperty("weight", 10);
            if (pet) {
                pick(PickerScreen.Mode.ENTITY, v -> {
                    e.addProperty("entity", v);
                    p.add(e);
                    selected = p.size() - 1;
                });
            } else {
                pick(PickerScreen.Mode.ITEM, v -> {
                    e.addProperty("item", v);
                    e.addProperty("count", 1);
                    p.add(e);
                    selected = p.size() - 1;
                });
            }
        });
        hit(x, by + 16, LIST_W, 14, () -> tr(confirmAuto ? "rtshop.gachaedit.auto_confirm" : "rtshop.gachaedit.auto"), () -> true, b -> {
            if (!confirmAuto) {
                confirmAuto = true;
                return;
            }
            confirmAuto = false;
            JsonObject a = new JsonObject();
            a.addProperty("cat", CATS[tab]);
            ShopNet.CHANNEL.sendToServer(new GachaActionPacket(GachaActionPacket.AUTOFILL, a.toString()));
        });
        if (p.isEmpty()) {
            label(x + LIST_W + 10, top + 4, tr("rtshop.gachaedit.empty"), Ui.C_CREAM);
            return;
        }
        selected = Math.max(0, Math.min(selected, p.size() - 1));
        buildEntry(x + LIST_W + 8, top, W - 16 - LIST_W - 8, p.get(selected).getAsJsonObject(), p);
    }

    private void buildEntry(int x, int top, int w, JsonObject e, JsonArray p) {
        boolean pet = e.has("entity");
        int y = top;
        hits.add(new Hit(x, y, w - 20, 16, () -> GachaScreen.name(e) + " ▼", () -> true,
                b -> pick(pet ? PickerScreen.Mode.ENTITY : PickerScreen.Mode.ITEM, v -> {
                    e.addProperty(pet ? "entity" : "item", v);
                    if (!pet) {
                        e.remove("nbt");
                    }
                }), () -> GachaScreen.icon(e), 0));
        hit(x + w - 16, y + 1, 16, 14, () -> "X", () -> true, b -> {
            p.remove(selected);
            selected = Math.max(0, selected - 1);
            markDirty();
            rebuild();
        });
        y += 20;
        label(x, y + 2, tr("rtshop.gachaedit.grade"), Ui.C_CREAM);
        final int gy = y;
        int gx = x + 60;
        hit(gx, y - 1, 90, 14, () -> tr("rtshop.gacha.grade." + e.get("grade").getAsString()) + " ▼", () -> true, b -> {
            List<ChoicePopup.Option> o = new ArrayList<>();
            for (String gname : GRADES) {
                o.add(new ChoicePopup.Option(gname, tr("rtshop.gacha.grade." + gname)));
            }
            popup = new ChoicePopup(gx, gy - 1, 90, 14, this.height, this.font, o, e.get("grade").getAsString(), v -> {
                e.addProperty("grade", v);
                markDirty();
            });
        });
        y += 16;
        label(x, y + 2, tr("rtshop.gachaedit.weight"), Ui.C_CREAM);
        box(x + 60, y, 50, e.get("weight").getAsString(), 6, v -> e.addProperty("weight", (int) Math.max(0, num(v, 0))));
        label(x + 116, y + 2, Ui.clip(this.font, tr("rtshop.gachaedit.weight_hint"), w - 116), Ui.C_PARCH_DARK);
        y += 16;
        if (!pet) {
            label(x, y + 2, tr("rtshop.gachaedit.count"), Ui.C_CREAM);
            box(x + 60, y, 50, e.has("count") ? e.get("count").getAsString() : "1", 3, v -> e.addProperty("count", (int) Math.max(1, Math.min(64, num(v, 1)))));
            hit(x + 116, y - 1, 90, 14, () -> tr("rtshop.gachaedit.from_hand"), () -> true, b -> fromHand(e));
            y += 16;
            if (e.has("nbt") && !e.get("nbt").getAsString().isEmpty()) {
                label(x, y + 2, Ui.clip(this.font, tr("rtshop.gachaedit.has_nbt"), w), Ui.C_GOLD);
                hit(x + w - 60, y - 1, 60, 14, () -> tr("rtshop.gachaedit.clear_nbt"), () -> true, b -> {
                    e.remove("nbt");
                    markDirty();
                    rebuild();
                });
                y += 16;
            }
        }
        y += 6;
        label(x, y, tr("rtshop.gachaedit.chance_head"), Ui.C_GOLD);
        y += 12;
        for (int t = 0; t < TIERS.length; t++) {
            double pr = chance(tab, t, e);
            label(x + 6, y, tr("rtshop.gacha.tier." + TIERS[t]) + ": " + (pr <= 0 ? "-" : GachaScreen.pct(pr)), Ui.C_CREAM);
            y += 11;
        }
        y += 6;
        int[] counts = new int[GRADES.length];
        for (JsonElement el : p) {
            counts[GachaScreen.gradeIndex(el.getAsJsonObject().get("grade").getAsString())]++;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < GRADES.length; i++) {
            sb.append(i == 0 ? "" : " · ").append(tr("rtshop.gacha.grade." + GRADES[i])).append(' ').append(counts[i]);
        }
        label(x, y, Ui.clip(this.font, sb.toString(), w), Ui.C_PARCH_DARK);
    }

    /** 이 칸이 이 뽑기 단계에서 나올 확률 (아이템이 없는 등급은 빼고 계산). */
    private double chance(int c, int tier, JsonObject target) {
        JsonObject w = cfg.getAsJsonObject("tiers").getAsJsonObject(TIERS[tier]);
        double[] gw = new double[GRADES.length];
        double[] sumW = new double[GRADES.length];
        for (JsonElement el : pool(c)) {
            JsonObject e = el.getAsJsonObject();
            int gi = GachaScreen.gradeIndex(e.get("grade").getAsString());
            int wt = e.get("weight").getAsInt();
            if (wt > 0 && exists(e)) {
                sumW[gi] += wt;
            }
        }
        double total = 0;
        for (int i = 0; i < GRADES.length; i++) {
            gw[i] = sumW[i] > 0 && w != null && w.has(GRADES[i]) ? w.get(GRADES[i]).getAsDouble() : 0;
            total += gw[i];
        }
        int gi = GachaScreen.gradeIndex(target.get("grade").getAsString());
        int wt = target.get("weight").getAsInt();
        if (total <= 0 || sumW[gi] <= 0 || wt <= 0 || !exists(target)) {
            return 0;
        }
        return gw[gi] / total * wt / sumW[gi];
    }

    private static boolean exists(JsonObject e) {
        if (e.has("entity")) {
            return ForgeRegistries.ENTITY_TYPES.containsKey(net.minecraft.resources.ResourceLocation.tryParse(e.get("entity").getAsString()));
        }
        return !GachaScreen.icon(e).isEmpty();
    }

    private void fromHand(JsonObject e) {
        Minecraft mc = Minecraft.getInstance();
        ItemStack s = mc.player == null ? ItemStack.EMPTY : mc.player.getMainHandItem();
        if (s.isEmpty()) {
            showResult(Component.translatable("rtshop.qedit.hand_empty"), false);
            return;
        }
        e.addProperty("item", String.valueOf(ForgeRegistries.ITEMS.getKey(s.getItem())));
        e.addProperty("count", s.getCount());
        CompoundTag tag = s.getTag() == null ? null : s.getTag().copy();
        if (tag != null) {
            tag.remove("rtshop_gacha");
        }
        if (tag != null && !tag.isEmpty()) {
            e.addProperty("nbt", tag.toString());
        } else {
            e.remove("nbt");
        }
        markDirty();
        rebuild();
    }

    private void pick(PickerScreen.Mode mode, Consumer<String> set) {
        if (minecraft == null) {
            return;
        }
        minecraft.setScreen(new PickerScreen(this, mode, v -> {
            set.accept(v);
            markDirty();
        }));
    }

    // ------------------------------------------------------------------ 확률·조각 탭

    private void buildOdds(int top) {
        int x = fx + 8;
        int y = top;
        int cw = Math.min(54, (W - 16 - 70) / GRADES.length - 4);
        label(x, y + 2, tr("rtshop.gachaedit.odds_head"), Ui.C_GOLD);
        for (int i = 0; i < GRADES.length; i++) {
            label(x + 70 + i * (cw + 4), y + 2, Ui.clip(this.font, tr("rtshop.gacha.grade." + GRADES[i]), cw + 2), GachaColors.argb(GRADES[i]));
        }
        y += 14;
        JsonObject tiers = cfg.getAsJsonObject("tiers");
        for (String t : TIERS) {
            JsonObject w = tiers.getAsJsonObject(t);
            label(x, y + 2, tr("rtshop.gacha.tier." + t), Ui.C_CREAM);
            for (int i = 0; i < GRADES.length; i++) {
                final String gname = GRADES[i];
                box(x + 70 + i * (cw + 4), y, cw, w.has(gname) ? fmt(w.get(gname).getAsDouble()) : "0", 8,
                        v -> w.addProperty(gname, Math.max(0, dnum(v, 0))));
            }
            y += 14;
        }
        y += 2;
        label(x, y + 2, tr("rtshop.gachaedit.guarantee"), Ui.C_CREAM);
        final int gy = y;
        hit(x + 140, y - 1, 100, 14, () -> {
            String g = cfg.get("tenGuarantee").getAsString();
            return (g.isEmpty() ? tr("rtshop.gachaedit.none") : tr("rtshop.gacha.grade." + g)) + " ▼";
        }, () -> true, b -> {
            List<ChoicePopup.Option> o = new ArrayList<>();
            o.add(new ChoicePopup.Option("", tr("rtshop.gachaedit.none")));
            for (String gname : GRADES) {
                o.add(new ChoicePopup.Option(gname, tr("rtshop.gacha.grade." + gname)));
            }
            popup = new ChoicePopup(x + 140, gy - 1, 100, 14, this.height, this.font, o, cfg.get("tenGuarantee").getAsString(), v -> {
                cfg.addProperty("tenGuarantee", v);
                markDirty();
            });
        });
        JsonObject sh = cfg.getAsJsonObject("shards");
        hit(x + 250, y - 1, Math.min(170, W - 16 - 250), 14, () -> tr(sh.has("exchangeEnabled") && !sh.get("exchangeEnabled").getAsBoolean()
                ? "rtshop.gachaedit.exchange_off" : "rtshop.gachaedit.exchange_on"), () -> true, b -> {
            boolean on = !sh.has("exchangeEnabled") || sh.get("exchangeEnabled").getAsBoolean();
            sh.addProperty("exchangeEnabled", !on);
            markDirty();
        });
        y += 17;
        {
            JsonObject m = sh.getAsJsonObject("dismantle");
            label(x, y + 2, tr("rtshop.gachaedit.dismantle"), Ui.C_CREAM);
            for (int i = 0; i < GRADES.length; i++) {
                final String gname = GRADES[i];
                box(x + 70 + i * (cw + 4), y, cw, m.has(gname) ? m.get(gname).getAsString() : "0", 7,
                        v -> m.addProperty(gname, (int) Math.max(0, num(v, 0))));
            }
            y += 14;
        }
        // 조각 → 티켓 교환 비용 (종류별 일반·중급·고급)
        if (!sh.has("tickets")) {
            sh.add("tickets", new JsonObject());
        }
        JsonObject tk = sh.getAsJsonObject("tickets");
        label(x, y + 2, tr("rtshop.gachaedit.ticket_cost"), Ui.C_CREAM);
        int tx = x + 70;
        for (int c = 0; c < CATS.length; c++) {
            final String cname = CATS[c];
            if (!tk.has(cname)) {
                tk.add(cname, new JsonObject());
            }
            JsonObject t = tk.getAsJsonObject(cname);
            int lw = this.font.width(tr("rtshop.gacha.cat." + cname)) + 3;
            if (tx + lw + 3 * 28 > fx + W - 8) {
                // 한 줄에 다 안 들어가면 다음 줄로
                y += 16;
                tx = x + 70;
            }
            label(tx, y + 2, tr("rtshop.gacha.cat." + cname), Ui.C_PARCH_DARK);
            for (int i = 0; i < TIERS.length; i++) {
                final String tname = TIERS[i];
                box(tx + lw + i * 28, y, 26, t.has(tname) ? t.get(tname).getAsString() : "0", 4,
                        v -> t.addProperty(tname, (int) Math.max(1, num(v, 1))));
            }
            tx += lw + 3 * 28 + 8;
        }
        y += 14;
        y += 4;
        // 모의 뽑기
        final int sy = y;
        hit(x, y, 80, 14, () -> tr("rtshop.gacha.cat." + CATS[simCat]) + " ▼", () -> true, b -> {
            List<ChoicePopup.Option> o = new ArrayList<>();
            for (int i = 0; i < CATS.length; i++) {
                o.add(new ChoicePopup.Option(Integer.toString(i), tr("rtshop.gacha.cat." + CATS[i])));
            }
            popup = new ChoicePopup(x, sy, 80, 14, this.height, this.font, o, Integer.toString(simCat), v -> simCat = Integer.parseInt(v));
        });
        hit(x + 84, y, 80, 14, () -> tr("rtshop.gacha.tier." + TIERS[simTier]) + " ▼", () -> true, b -> {
            List<ChoicePopup.Option> o = new ArrayList<>();
            for (int i = 0; i < TIERS.length; i++) {
                o.add(new ChoicePopup.Option(Integer.toString(i), tr("rtshop.gacha.tier." + TIERS[i])));
            }
            popup = new ChoicePopup(x + 84, sy, 80, 14, this.height, this.font, o, Integer.toString(simTier), v -> simTier = Integer.parseInt(v));
        });
        hit(x + 168, y, 130, 14, () -> tr("rtshop.gachaedit.simulate"), () -> true, b -> {
            JsonObject a = new JsonObject();
            a.addProperty("cat", CATS[simCat]);
            a.addProperty("tier", TIERS[simTier]);
            ShopNet.CHANNEL.sendToServer(new GachaActionPacket(GachaActionPacket.SIMULATE, a.toString()));
        });
        y += 17;
        if (sim != null) {
            JsonArray g = sim.getAsJsonArray("grades");
            StringBuilder sb = new StringBuilder(tr("rtshop.gachaedit.sim_head", sim.get("n").getAsInt(),
                    tr("rtshop.gacha.cat." + sim.get("cat").getAsString()), tr("rtshop.gacha.tier." + sim.get("tier").getAsString())));
            label(x, y, sb.toString(), Ui.C_GOLD);
            y += 11;
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < GRADES.length; i++) {
                line.append(i == 0 ? "" : " · ").append(tr("rtshop.gacha.grade." + GRADES[i])).append(' ').append(g.get(i).getAsInt());
            }
            label(x, y, Ui.clip(this.font, line.toString(), W - 16), Ui.C_CREAM);
            y += 11;
            StringBuilder topLine = new StringBuilder(tr("rtshop.gachaedit.sim_top"));
            for (JsonElement el : sim.getAsJsonArray("top")) {
                JsonObject t = el.getAsJsonObject();
                JsonObject fake = new JsonObject();
                String id = t.get("id").getAsString();
                if (ForgeRegistries.ENTITY_TYPES.containsKey(net.minecraft.resources.ResourceLocation.tryParse(id))
                        && !ForgeRegistries.ITEMS.containsKey(net.minecraft.resources.ResourceLocation.tryParse(id))) {
                    fake.addProperty("entity", id);
                } else {
                    fake.addProperty("item", id);
                }
                topLine.append(' ').append(GachaScreen.name(fake)).append('(').append(t.get("count").getAsInt()).append(')');
            }
            label(x, y, Ui.clip(this.font, topLine.toString(), W - 16), Ui.C_PARCH_DARK);
        }
    }

    private static String fmt(double d) {
        return d == Math.rint(d) ? Long.toString((long) d) : Double.toString(d);
    }

    // ------------------------------------------------------------------ 설정 탭

    private void buildSettings(int top) {
        int x = fx + 8;
        int y = top;
        int lw = 150;
        int bw = W - 16 - lw;
        label(x, y + 2, tr("rtshop.attedit.npc_name"), Ui.C_CREAM);
        box(x + lw, y, bw, cfg.get("npcName").getAsString(), 64, v -> cfg.addProperty("npcName", v));
        y += 16;
        label(x, y + 2, tr("rtshop.gachaedit.shop"), Ui.C_CREAM);
        box(x + lw, y, Math.min(160, bw), cfg.get("shop").getAsString(), 64, v -> cfg.addProperty("shop", v.trim()));
        y += 16;
        label(x, y + 2, tr("rtshop.gachaedit.exclude_mods"), Ui.C_CREAM);
        box(x + lw, y, bw, join(cfg.getAsJsonArray("excludeMods")), 2000, v -> cfg.add("excludeMods", split(v)));
        y += 16;
        label(x, y + 2, tr("rtshop.gachaedit.exclude_items"), Ui.C_CREAM);
        box(x + lw, y, bw, join(cfg.getAsJsonArray("excludeItems")), 8000, v -> cfg.add("excludeItems", split(v)));
        y += 16;
        label(x, y + 2, tr("rtshop.gachaedit.weapon_mods"), Ui.C_CREAM);
        box(x + lw, y, bw, join(cfg.getAsJsonArray("weaponMods")), 2000, v -> cfg.add("weaponMods", split(v)));
        y += 16;
        label(x, y + 2, tr("rtshop.gachaedit.top_mods"), Ui.C_CREAM);
        box(x + lw, y, bw, join(cfg.getAsJsonArray("topWeaponMods")), 2000, v -> cfg.add("topWeaponMods", split(v)));
        y += 18;
        label(x, y, tr("rtshop.gachaedit.settings_hint1"), Ui.C_PARCH_DARK);
        y += 11;
        label(x, y, tr("rtshop.gachaedit.settings_hint2"), Ui.C_PARCH_DARK);
    }

    private static String join(JsonArray a) {
        List<String> l = new ArrayList<>();
        if (a != null) {
            for (JsonElement e : a) {
                l.add(e.getAsString());
            }
        }
        return String.join(", ", l);
    }

    private static JsonArray split(String s) {
        JsonArray a = new JsonArray();
        for (String p : s.split("[,\\s]+")) {
            if (!p.isBlank()) {
                a.add(p.trim());
            }
        }
        return a;
    }

    private void save() {
        JsonObject a = new JsonObject();
        a.addProperty("json", cfg.toString());
        ShopNet.CHANNEL.sendToServer(new GachaActionPacket(GachaActionPacket.SAVE, a.toString()));
    }

    // ------------------------------------------------------------------ 그리기 / 입력

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        this.renderBackground(g);
        Ui.woodFrame(g, fx, fy, W, H);
        g.drawString(this.font, this.title.getString() + (dirty ? " *" : ""), fx + 10, fy + 8, Ui.C_GOLD, true);
        String hint = Ui.clip(this.font, tr(tab < 3 ? "rtshop.gachaedit.hint_pool" : "rtshop.gachaedit.hint_odds"), W - 140);
        g.drawString(this.font, hint, fx + W - 10 - this.font.width(hint), fy + 8, Ui.C_PARCH_DARK, false);
        for (Hit h : hits) {
            boolean hov = Ui.inside(mx, my, h.x(), h.y(), h.w(), h.h());
            if (h.icon() == null) {
                Ui.button(g, this.font, h.x(), h.y(), h.w(), h.h(), h.label().get(), hov, h.enabled().getAsBoolean());
                continue;
            }
            boolean sel = !h.enabled().getAsBoolean();
            Ui.bevel(g, h.x(), h.y(), h.w(), h.h(), sel ? Ui.C_PARCH_HI : (hov ? Ui.C_PARCH : Ui.C_CREAM), Ui.C_PARCH_HI, Ui.C_PARCH_SHADE);
            if (h.color() != 0) {
                g.fill(h.x() + 1, h.y() + 1, h.x() + 3, h.y() + h.h() - 1, h.color());
            }
            ItemStack icon = h.icon().get();
            int tx = h.x() + 5;
            if (!icon.isEmpty()) {
                g.pose().pushPose();
                g.pose().translate(h.x() + 4, h.y() + (h.h() - 12) / 2.0F, 0);
                g.pose().scale(0.75F, 0.75F, 1.0F);
                g.renderItem(icon, 0, 0);
                g.pose().popPose();
                tx = h.x() + 18;
            }
            g.drawString(this.font, Ui.clip(this.font, h.label().get(), h.x() + h.w() - tx - 3), tx, h.y() + (h.h() - 8) / 2, Ui.C_INK, false);
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
                    if (minecraft != null && minecraft.screen == this) {
                        rebuild();
                    }
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
        if (tab < 3 && mx < fx + 8 + LIST_W + 4) {
            listScroll = Math.max(0, listScroll - (int) Math.signum(delta) * 3);
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
        filter = -1;
        popup = null;
        rebuild();
    }

    public int poolSizeForTest(int c) {
        return pool(c).size();
    }

    public void setWeightForTest(int c, int index, int weight) {
        pool(c).get(index).getAsJsonObject().addProperty("weight", weight);
        dirty = true;
        rebuild();
    }

    public void saveForTest() {
        save();
    }

    public void simulateForTest(int c, int t) {
        simCat = c;
        simTier = t;
        JsonObject a = new JsonObject();
        a.addProperty("cat", CATS[c]);
        a.addProperty("tier", TIERS[t]);
        ShopNet.CHANNEL.sendToServer(new GachaActionPacket(GachaActionPacket.SIMULATE, a.toString()));
    }

    public JsonObject simForTest() {
        return sim;
    }
}

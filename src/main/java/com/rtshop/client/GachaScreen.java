package com.rtshop.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.item.ModItems;
import com.rtshop.net.GachaActionPacket;
import com.rtshop.net.ShopNet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 뽑기 창: 무기·펫·장신구 탭 × (뽑기 / 조각 교환 / 분해).
 * 뽑기: 일반·중급·고급 칸마다 등급 확률과 1회·10회 버튼, 확률표(아이템별 확률), 티켓 사기(상점).
 */
public class GachaScreen extends Screen {
    public static final String[] CATS = {"weapon", "pet", "accessory"};
    public static final String[] TIERS = {"normal", "mid", "high"};
    public static final String[] GRADES = {"common", "uncommon", "rare", "epic", "legendary"};
    private static final int ROW = 18;

    private JsonObject data;
    private int cat = 0;
    private int mode = 0; // 0 뽑기, 1 조각 교환, 2 분해
    private int probTier = -1; // 확률표를 보는 중이면 단계 번호
    private int scroll = 0;
    private JsonArray results;
    private int resultTicks;
    private Component status;
    private boolean statusOk;
    private int statusTicks;
    private int fx, fy, W, H;

    private record Btn(int x, int y, int w, int h, String label, boolean enabled, Runnable action) {
    }

    private final List<Btn> buttons = new ArrayList<>();

    public GachaScreen(String json) {
        super(Component.translatable("rtshop.gacha.title"));
        update(json, false);
    }

    public void update(String json, boolean result) {
        data = JsonParser.parseString(json).getAsJsonObject();
        if (result && data.has("results")) {
            results = data.getAsJsonArray("results");
            resultTicks = 0;
            for (int i = 0; i < CATS.length; i++) {
                if (CATS[i].equals(data.get("resultCat").getAsString())) {
                    cat = i;
                }
            }
        }
        if (this.width > 0) {
            layout();
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
        W = Math.min(this.width - 8, 440);
        H = Math.min(this.height - 8, 256);
        fx = (this.width - W) / 2;
        fy = (this.height - H) / 2;
        layout();
    }

    @Override
    public void tick() {
        if (statusTicks > 0) {
            statusTicks--;
        }
        if (results != null) {
            resultTicks++;
        }
    }

    private static String tr(String k, Object... a) {
        return Component.translatable(k, a).getString();
    }

    private boolean b(String k) {
        return data.has(k) && data.get(k).getAsBoolean();
    }

    private int tickets(int t) {
        return data.getAsJsonArray("tickets").get(t).getAsInt();
    }

    private void send(int action, JsonObject arg) {
        ShopNet.CHANNEL.sendToServer(new GachaActionPacket(action, arg == null ? "" : arg.toString()));
    }

    private JsonArray pool() {
        return data.getAsJsonObject("pools").getAsJsonArray(CATS[cat]);
    }

    private double[] chances(int tier) {
        JsonArray a = data.getAsJsonObject("chances").getAsJsonObject(TIERS[tier]).getAsJsonArray(CATS[cat]);
        double[] out = new double[GRADES.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = a.get(i).getAsDouble();
        }
        return out;
    }

    static int gradeIndex(String g) {
        for (int i = 0; i < GRADES.length; i++) {
            if (GRADES[i].equals(g)) {
                return i;
            }
        }
        return 0;
    }

    static ItemStack icon(JsonObject e) {
        if (e.has("entity") && !e.get("entity").getAsString().isEmpty()) {
            EntityType<?> t = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.tryParse(e.get("entity").getAsString()));
            ItemStack egg = t == null ? ItemStack.EMPTY : PickerScreen.eggFor(t);
            return egg.isEmpty() ? new ItemStack(ModItems.PET_SUMMON.get()) : egg;
        }
        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(e.get("item").getAsString()));
        if (item == null) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, e.has("count") ? Math.max(1, e.get("count").getAsInt()) : 1);
    }

    static String name(JsonObject e) {
        if (e.has("entity") && !e.get("entity").getAsString().isEmpty()) {
            EntityType<?> t = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.tryParse(e.get("entity").getAsString()));
            return t == null ? e.get("entity").getAsString() : t.getDescription().getString();
        }
        ItemStack s = icon(e);
        String n = s.isEmpty() ? e.get("item").getAsString() : s.getHoverName().getString();
        int c = e.has("count") ? e.get("count").getAsInt() : 1;
        return c > 1 ? n + " x" + c : n;
    }

    // ------------------------------------------------------------------ 배치

    private int bodyY() {
        return fy + 44;
    }

    private int bodyH() {
        return H - 44 - 28;
    }

    private void layout() {
        buttons.clear();
        if (results != null) {
            buttons.add(new Btn(fx + W / 2 - 40, fy + H - 26, 80, 18, tr("rtshop.gacha.ok"), true, () -> results = null));
            return;
        }
        if (probTier >= 0) {
            buttons.add(new Btn(fx + W / 2 - 40, fy + H - 26, 80, 18, tr("rtshop.gacha.back"), true, () -> {
                probTier = -1;
                scroll = 0;
                layout();
            }));
            return;
        }
        String[] cats = {"rtshop.gacha.cat.weapon", "rtshop.gacha.cat.pet", "rtshop.gacha.cat.accessory"};
        int x = fx + 8;
        for (int i = 0; i < 3; i++) {
            final int c = i;
            buttons.add(new Btn(x, fy + 24, 54, 16, tr(cats[i]), cat != i, () -> {
                cat = c;
                scroll = 0;
                layout();
            }));
            x += 56;
        }
        String[] modes = {"rtshop.gacha.mode.pull", "rtshop.gacha.mode.exchange", "rtshop.gacha.mode.dismantle"};
        int mx = fx + W - 8 - 3 * 64 - 4;
        for (int i = 0; i < 3; i++) {
            final int m = i;
            boolean en = mode != i && (i != 1 || b("exchangeEnabled"));
            buttons.add(new Btn(mx + i * 66, fy + 24, 64, 16, tr(modes[i]), en, () -> {
                mode = m;
                scroll = 0;
                layout();
            }));
        }
        boolean at = b("atNpc");
        if (mode == 0) {
            int pw = (W - 16 - 8) / 3;
            for (int t = 0; t < 3; t++) {
                final int tt = t;
                int px = fx + 8 + t * (pw + 4);
                int by = bodyY() + bodyH() - 40;
                buttons.add(new Btn(px + 4, by, (pw - 12) / 2, 16, tr("rtshop.gacha.pull1"), at && tickets(t) >= 1, () -> pull(tt, 1)));
                buttons.add(new Btn(px + 8 + (pw - 12) / 2, by, (pw - 12) / 2, 16, tr("rtshop.gacha.pull10"), at && tickets(t) >= 10, () -> pull(tt, 10)));
                buttons.add(new Btn(px + 4, by + 18, pw - 8, 14, tr("rtshop.gacha.odds"), true, () -> {
                    probTier = tt;
                    scroll = 0;
                    layout();
                }));
            }
        } else if (mode == 1) {
            List<JsonObject> rows = exchangeRows();
            int rows0 = bodyH() / ROW;
            scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - rows0)));
            int shards = data.get("shards").getAsInt();
            for (int k = 0; k < rows0 && scroll + k < rows.size(); k++) {
                JsonObject e = rows.get(scroll + k);
                int cost = cost(e);
                int y = bodyY() + k * ROW;
                int idx = e.get("index").getAsInt();
                buttons.add(new Btn(fx + W - 8 - 60, y + 1, 60, 15, tr("rtshop.gacha.exchange_btn"), at && shards >= cost, () -> {
                    JsonObject a = new JsonObject();
                    a.addProperty("cat", CATS[cat]);
                    a.addProperty("index", idx);
                    send(GachaActionPacket.EXCHANGE, a);
                }));
            }
        } else {
            JsonArray inv = data.getAsJsonArray("inventory");
            int rows0 = bodyH() / ROW - 1;
            scroll = Math.max(0, Math.min(scroll, Math.max(0, inv.size() - rows0)));
            for (int k = 0; k < rows0 && scroll + k < inv.size(); k++) {
                JsonObject e = inv.get(scroll + k).getAsJsonObject();
                int slot = e.get("slot").getAsInt();
                int y = bodyY() + k * ROW;
                buttons.add(new Btn(fx + W - 8 - 60, y + 1, 60, 15, tr("rtshop.gacha.dismantle_btn"), at, () -> {
                    JsonObject a = new JsonObject();
                    a.addProperty("slot", slot);
                    send(GachaActionPacket.DISMANTLE, a);
                }));
            }
            int by = bodyY() + bodyH() - 16;
            buttons.add(new Btn(fx + 8, by, 130, 16, tr("rtshop.gacha.dismantle_low"), at && !inv.isEmpty(), () -> dismantleAll(1)));
            buttons.add(new Btn(fx + 142, by, 130, 16, tr("rtshop.gacha.dismantle_rare"), at && !inv.isEmpty(), () -> dismantleAll(2)));
        }
        int by = fy + H - 24;
        if (at && b("shop")) {
            buttons.add(new Btn(fx + 8, by, 80, 18, tr("rtshop.gacha.buy_tickets"), true, () -> send(GachaActionPacket.SHOP, null)));
        }
        int right = fx + W - 8;
        buttons.add(new Btn(right - 50, by, 50, 18, tr("rtshop.att.btn_close"), true, this::onClose));
        if (b("canEdit")) {
            buttons.add(new Btn(right - 104, by, 50, 18, tr("rtshop.att.btn_edit"), true, () -> send(GachaActionPacket.EDIT, null)));
        }
    }

    private void pull(int tier, int n) {
        JsonObject a = new JsonObject();
        a.addProperty("cat", CATS[cat]);
        a.addProperty("tier", TIERS[tier]);
        a.addProperty("n", n);
        send(GachaActionPacket.PULL, a);
    }

    private void dismantleAll(int max) {
        JsonObject a = new JsonObject();
        a.addProperty("max", max);
        send(GachaActionPacket.DISMANTLE_ALL, a);
    }

    private int cost(JsonObject e) {
        return data.getAsJsonArray("exchange").get(gradeIndex(e.get("grade").getAsString())).getAsInt();
    }

    private List<JsonObject> exchangeRows() {
        List<JsonObject> rows = new ArrayList<>();
        for (JsonElement el : pool()) {
            JsonObject e = el.getAsJsonObject();
            if (e.get("valid").getAsBoolean()) {
                rows.add(e);
            }
        }
        rows.sort(Comparator.comparingInt((JsonObject e) -> -gradeIndex(e.get("grade").getAsString())).thenComparing(GachaScreen::name));
        return rows;
    }

    // ------------------------------------------------------------------ 그리기

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        this.renderBackground(g);
        Ui.woodFrame(g, fx, fy, W, H);
        g.drawString(this.font, Ui.clip(this.font, data.get("npc").getAsString() + " · " + this.title.getString(), W / 2), fx + 10, fy + 8, Ui.C_GOLD, true);
        // 오른쪽 위: 티켓·조각
        int x = fx + W - 10;
        String sh = Integer.toString(data.get("shards").getAsInt());
        x -= this.font.width(sh);
        g.drawString(this.font, sh, x, fy + 8, Ui.C_CREAM, false);
        x -= 18;
        g.renderItem(new ItemStack(ModItems.SHARD.get()), x, fy + 3);
        Item[] tk = {ModItems.TICKET_NORMAL.get(), ModItems.TICKET_MID.get(), ModItems.TICKET_HIGH.get()};
        for (int t = 2; t >= 0; t--) {
            String n = Integer.toString(tickets(t));
            x -= this.font.width(n) + 6;
            g.drawString(this.font, n, x, fy + 8, Ui.C_CREAM, false);
            x -= 18;
            g.renderItem(new ItemStack(tk[t]), x, fy + 3);
        }
        if (results != null) {
            renderResults(g, mx, my);
        } else if (probTier >= 0) {
            renderOdds(g);
        } else if (mode == 0) {
            renderPull(g);
        } else if (mode == 1) {
            renderExchange(g);
        } else {
            renderDismantle(g);
        }
        if (status != null && statusTicks > 0) {
            String s = Ui.clip(this.font, status.getString(), W - 200);
            g.drawString(this.font, s, fx + 94, fy + H - 19, statusOk ? 0xFF8FD67A : 0xFFFF8A7A, false);
        } else if (!b("atNpc") && results == null) {
            g.drawString(this.font, Ui.clip(this.font, tr("rtshop.gacha.view_only", data.get("npc").getAsString()), W - 200), fx + 94, fy + H - 19, Ui.C_PARCH_DARK, false);
        }
        g.flush();
        for (Btn bt : buttons) {
            Ui.button(g, this.font, bt.x(), bt.y(), bt.w(), bt.h(), bt.label(), Ui.inside(mx, my, bt.x(), bt.y(), bt.w(), bt.h()), bt.enabled());
        }
        super.render(g, mx, my, partial);
    }

    private void renderPull(GuiGraphics g) {
        int pw = (W - 16 - 8) / 3;
        Item[] tk = {ModItems.TICKET_NORMAL.get(), ModItems.TICKET_MID.get(), ModItems.TICKET_HIGH.get()};
        for (int t = 0; t < 3; t++) {
            int px = fx + 8 + t * (pw + 4);
            int py = bodyY();
            Ui.bevel(g, px, py, pw, bodyH(), Ui.C_PARCH, Ui.C_PARCH_HI, Ui.C_PARCH_SHADE);
            g.renderItem(new ItemStack(tk[t]), px + 4, py + 4);
            g.drawString(this.font, tr("rtshop.gacha.tier." + TIERS[t]), px + 24, py + 5, Ui.C_INK, false);
            g.drawString(this.font, tr("rtshop.gacha.have", tickets(t)), px + 24, py + 14, Ui.C_INK_SOFT, false);
            double[] c = chances(t);
            int yy = py + 30;
            boolean any = false;
            for (int i = GRADES.length - 1; i >= 0; i--) {
                if (c[i] <= 0) {
                    continue;
                }
                any = true;
                String line = tr("rtshop.gacha.grade." + GRADES[i]) + "  " + pct(c[i]);
                g.drawString(this.font, line, px + 8, yy, GachaColors.dark(GRADES[i]), false);
                g.fill(px + 4, yy + 1, px + 6, yy + 7, GachaColors.argb(GRADES[i]));
                yy += 11;
            }
            if (!any) {
                g.drawString(this.font, tr("rtshop.gacha.empty_short"), px + 8, yy, Ui.C_INK_SOFT, false);
            }
        }
        String gu = data.get("tenGuarantee").getAsString();
        if (!gu.isEmpty()) {
            String s = tr("rtshop.gacha.guarantee", tr("rtshop.gacha.grade." + gu));
            g.drawString(this.font, Ui.clip(this.font, s, W - 16), fx + 8, fy + H - 36, Ui.C_CREAM, false);
        }
    }

    static String pct(double p) {
        double v = p * 100;
        if (v >= 10) {
            return String.format("%.0f%%", v);
        }
        if (v >= 1) {
            return String.format("%.1f%%", v);
        }
        return String.format("%.2f%%", v);
    }

    /** 확률표: 이 단계에서 아이템 하나하나가 나올 확률. */
    private void renderOdds(GuiGraphics g) {
        double[] c = chances(probTier);
        double[] sum = new double[GRADES.length];
        List<JsonObject> rows = new ArrayList<>();
        for (JsonElement el : pool()) {
            JsonObject e = el.getAsJsonObject();
            if (e.get("valid").getAsBoolean()) {
                sum[gradeIndex(e.get("grade").getAsString())] += e.get("weight").getAsInt();
                rows.add(e);
            }
        }
        rows.removeIf(e -> c[gradeIndex(e.get("grade").getAsString())] <= 0);
        rows.sort(Comparator.comparingInt((JsonObject e) -> -gradeIndex(e.get("grade").getAsString())).thenComparing(GachaScreen::name));
        g.drawString(this.font, tr("rtshop.gacha.odds_title", tr("rtshop.gacha.cat." + CATS[cat]), tr("rtshop.gacha.tier." + TIERS[probTier])),
                fx + 8, fy + 28, Ui.C_GOLD, false);
        int x = fx + 8;
        int y = bodyY();
        int h = bodyH();
        Ui.bevel(g, x, y, W - 16, h, Ui.C_PARCH, Ui.C_PARCH_HI, Ui.C_PARCH_SHADE);
        int per = (h - 4) / 12;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - per)));
        for (int k = 0; k < per && scroll + k < rows.size(); k++) {
            JsonObject e = rows.get(scroll + k);
            int gi = gradeIndex(e.get("grade").getAsString());
            double p = c[gi] * e.get("weight").getAsInt() / Math.max(1, sum[gi]);
            int yy = y + 3 + k * 12;
            g.fill(x + 3, yy, x + 5, yy + 9, GachaColors.argb(GRADES[gi]));
            g.drawString(this.font, Ui.clip(this.font, name(e), W - 130), x + 8, yy + 1, Ui.C_INK, false);
            String gp = tr("rtshop.gacha.grade." + GRADES[gi]) + "  " + pct(p);
            g.drawString(this.font, gp, x + W - 24 - this.font.width(gp), yy + 1, Ui.C_INK_SOFT, false);
        }
        if (rows.isEmpty()) {
            g.drawString(this.font, tr("rtshop.gacha.empty_short"), x + 8, y + 6, Ui.C_INK_SOFT, false);
        }
    }

    private void renderExchange(GuiGraphics g) {
        List<JsonObject> rows = exchangeRows();
        int per = bodyH() / ROW;
        for (int k = 0; k < per && scroll + k < rows.size(); k++) {
            JsonObject e = rows.get(scroll + k);
            int gi = gradeIndex(e.get("grade").getAsString());
            int y = bodyY() + k * ROW;
            Ui.bevel(g, fx + 8, y, W - 16 - 64, ROW - 1, Ui.C_PARCH, Ui.C_PARCH_HI, Ui.C_PARCH_SHADE);
            g.fill(fx + 9, y + 1, fx + 11, y + ROW - 2, GachaColors.argb(GRADES[gi]));
            g.renderItem(icon(e), fx + 13, y);
            String n = name(e) + (e.get("owned").getAsBoolean() ? "  ✔" : "");
            g.drawString(this.font, Ui.clip(this.font, n, W - 220), fx + 32, y + 5, Ui.C_INK, false);
            String c = tr("rtshop.gacha.grade." + GRADES[gi]) + " · " + tr("rtshop.gacha.cost", cost(e));
            g.drawString(this.font, c, fx + W - 76 - this.font.width(c), y + 5, Ui.C_INK_SOFT, false);
        }
        if (rows.isEmpty()) {
            g.drawString(this.font, tr("rtshop.gacha.empty_short"), fx + 12, bodyY() + 6, Ui.C_CREAM, false);
        }
    }

    private void renderDismantle(GuiGraphics g) {
        JsonArray inv = data.getAsJsonArray("inventory");
        int per = bodyH() / ROW - 1;
        JsonArray dm = data.getAsJsonArray("dismantle");
        for (int k = 0; k < per && scroll + k < inv.size(); k++) {
            JsonObject e = inv.get(scroll + k).getAsJsonObject();
            int gi = gradeIndex(e.get("grade").getAsString());
            int y = bodyY() + k * ROW;
            Ui.bevel(g, fx + 8, y, W - 16 - 64, ROW - 1, Ui.C_PARCH, Ui.C_PARCH_HI, Ui.C_PARCH_SHADE);
            g.fill(fx + 9, y + 1, fx + 11, y + ROW - 2, GachaColors.argb(GRADES[gi]));
            g.renderItem(icon(e), fx + 13, y);
            g.drawString(this.font, Ui.clip(this.font, name(e), W - 220), fx + 32, y + 5, Ui.C_INK, false);
            String c = tr("rtshop.gacha.grade." + GRADES[gi]) + " · " + tr("rtshop.gacha.gives", dm.get(gi).getAsInt() * e.get("count").getAsInt());
            g.drawString(this.font, c, fx + W - 76 - this.font.width(c), y + 5, Ui.C_INK_SOFT, false);
        }
        if (inv.isEmpty()) {
            g.drawString(this.font, tr("rtshop.gacha.nothing_to_dismantle"), fx + 12, bodyY() + 6, Ui.C_CREAM, false);
        }
    }

    /** 뽑기 결과: 카드가 하나씩 뒤집힌다 (높은 등급은 빛남). */
    private void renderResults(GuiGraphics g, int mx, int my) {
        g.fill(fx + 4, fy + 20, fx + W - 4, fy + H - 30, 0xCC100804);
        int n = results.size();
        int cols = Math.min(5, n);
        int rows = (n + cols - 1) / cols;
        int cw = 74;
        int ch = 74;
        int gx = fx + (W - cols * (cw + 4)) / 2;
        int gy = fy + 26 + (H - 60 - rows * (ch + 4)) / 2;
        for (int i = 0; i < n; i++) {
            JsonObject e = results.get(i).getAsJsonObject();
            int cx = gx + (i % cols) * (cw + 4);
            int cy = gy + (i / cols) * (ch + 4);
            int gi = gradeIndex(e.get("grade").getAsString());
            boolean shown = resultTicks >= i * 3 + 4;
            if (!shown) {
                Ui.bevel(g, cx, cy, cw, ch, Ui.C_WOOD, Ui.C_WOOD_HI, Ui.C_WOOD_DARK);
                g.drawCenteredString(this.font, "?", cx + cw / 2, cy + ch / 2 - 4, Ui.C_GOLD);
                continue;
            }
            int col = GachaColors.argb(GRADES[gi]);
            if (gi >= 3) {
                int pulse = (int) (40 + 30 * Math.sin((resultTicks + partialPulse()) * 0.3));
                g.fill(cx - 2, cy - 2, cx + cw + 2, cy + ch + 2, (pulse << 24) | (col & 0xFFFFFF));
            }
            Ui.bevel(g, cx, cy, cw, ch, Ui.C_PARCH, Ui.C_PARCH_HI, Ui.C_PARCH_SHADE);
            g.fill(cx, cy, cx + cw, cy + 3, col);
            g.fill(cx, cy + ch - 3, cx + cw, cy + ch, col);
            g.pose().pushPose();
            g.pose().translate(cx + cw / 2 - 16, cy + 10, 0);
            g.pose().scale(2, 2, 1);
            g.renderItem(icon(e), 0, 0);
            g.pose().popPose();
            String gn = tr("rtshop.gacha.grade." + GRADES[gi]);
            g.drawString(this.font, gn, cx + (cw - this.font.width(gn)) / 2, cy + 46, GachaColors.dark(GRADES[gi]), false);
            String nm = Ui.clip(this.font, name(e), cw - 6);
            g.drawString(this.font, nm, cx + (cw - this.font.width(nm)) / 2, cy + 58, Ui.C_INK, false);
            if (e.has("new") && e.get("new").getAsBoolean()) {
                g.drawString(this.font, "NEW", cx + 3, cy + 5, 0xFFE03C2C, false);
            }
        }
    }

    private float partialPulse() {
        return 0;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            if (results != null && resultTicks < results.size() * 3 + 4) {
                resultTicks = results.size() * 3 + 4; // 클릭하면 바로 모두 뒤집기
                return true;
            }
            for (Btn bt : new ArrayList<>(buttons)) {
                if (bt.enabled() && Ui.inside(mx, my, bt.x(), bt.y(), bt.w(), bt.h())) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    bt.action().run();
                    layout();
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scroll = Math.max(0, scroll - (int) Math.signum(delta) * 2);
        layout();
        return true;
    }

    // ------------------------------------------------------------------ 점검용

    public JsonObject dataForTest() {
        return data;
    }

    public JsonArray resultsForTest() {
        return results;
    }

    public void pullForTest(int catIndex, int tier, int n) {
        cat = catIndex;
        pull(tier, n);
    }

    public void showForTest(int catIndex, int modeIndex, int oddsTier) {
        results = null;
        cat = catIndex;
        mode = modeIndex;
        probTier = oddsTier;
        scroll = 0;
        layout();
    }

    public void revealForTest() {
        if (results != null) {
            resultTicks = 1000;
        }
    }

    /** 점검용: 편집 버튼과 같은 요청. */
    public void clickForTest() {
        send(GachaActionPacket.EDIT, null);
    }
}

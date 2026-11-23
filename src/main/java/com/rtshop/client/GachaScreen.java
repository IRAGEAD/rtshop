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
public class GachaScreen extends Screen implements NpcButtons.Host {
    @Override
    public int[] npcButtonSpot() {
        String t = Ui.clip(this.font, data.get("npc").getAsString() + " · " + this.title.getString(), W / 2 - 40);
        return new int[]{fx + 10 + this.font.width(t) + 10, fy + 6};
    }

    public static final String[] CATS = {"weapon", "pet", "accessory"};
    public static final String[] TIERS = {"normal", "mid", "high"};
    public static final String[] GRADES = com.rtshop.gacha.GachaConfig.GRADES;
    private static final int ROW = 18;
    private static final int BANNER = 20;

    /** 뽑기 종류마다 다른 색·문양·아이콘 (바꿨는지 한눈에 보이게). */
    record Theme(int accent, int dark, int light, int panel, int panelHi, int panelShade, String icon) {
    }

    static final Theme[] THEMES = {
            new Theme(0xFFC8402E, 0xFF4A140E, 0xFFFFD2C4, 0xFFEFD8CC, 0xFFFBEAE2, 0xFFC49A88, "minecraft:diamond_sword"),
            new Theme(0xFF3E9E4A, 0xFF123A18, 0xFFD2F5C8, 0xFFDDEBCB, 0xFFF0F8E4, 0xFFA2BE8E, "minecraft:saddle"),
            new Theme(0xFF8A5AE0, 0xFF2A1650, 0xFFE4D2FF, 0xFFE4DAF2, 0xFFF4EEFB, 0xFFAE9CCC, "minecraft:amethyst_shard")};

    static Theme theme(int cat) {
        return THEMES[Math.max(0, Math.min(THEMES.length - 1, cat))];
    }

    private final GachaFx anim = new GachaFx();
    private int switchTicks = 0;
    private int animTicks = 0;

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
            // 가장 높은 등급 하나로 연출부터 (등급마다 연출이 다르다)
            int best = -1;
            JsonObject bestE = null;
            for (JsonElement el : results) {
                JsonObject e = el.getAsJsonObject();
                int gi = gradeIndex(e.get("grade").getAsString());
                if (gi > best) {
                    best = gi;
                    bestE = e;
                }
            }
            if (best >= 0 && bestE != null) {
                anim.start(best, icon(bestE), name(bestE));
            } else {
                anim.skip();
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
    public void removed() {
        anim.stop(); // 야마토 음악 등
        super.removed();
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
        animTicks++;
        if (statusTicks > 0) {
            statusTicks--;
        }
        if (switchTicks > 0) {
            switchTicks--;
        }
        if (anim.active()) {
            anim.tick();
            return;
        }
        if (results != null) {
            resultTicks++;
            for (int i = 0; i < results.size(); i++) {
                if (resultTicks == i * 3 + 4) {
                    flipSound(gradeIndex(results.get(i).getAsJsonObject().get("grade").getAsString()));
                }
            }
        }
    }

    private static void sfx(net.minecraft.sounds.SoundEvent e, float pitch, float vol) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(e, pitch, vol));
    }

    private static void flipSound(int gi) {
        switch (gi) {
            case 5 -> sfx(SoundEvents.TRIDENT_RIPTIDE_3, 1.4F, 0.8F);
            case 4 -> sfx(SoundEvents.PLAYER_LEVELUP, 1.5F, 0.7F);
            case 3 -> sfx(SoundEvents.AMETHYST_CLUSTER_BREAK, 1.2F, 0.9F);
            case 2 -> sfx(SoundEvents.AMETHYST_BLOCK_CHIME, 1.4F, 0.8F);
            default -> sfx(SoundEvents.BOOK_PAGE_TURN, 1.0F + gi * 0.15F, 0.6F);
        }
    }

    private void setCat(int c) {
        if (c == cat) {
            return;
        }
        cat = c;
        scroll = 0;
        switchTicks = 10;
        switch (c) {
            case 0 -> sfx(SoundEvents.ARMOR_EQUIP_NETHERITE, 1.0F, 0.9F);
            case 1 -> sfx(SoundEvents.WOLF_AMBIENT, 1.3F, 0.6F);
            default -> sfx(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.0F);
        }
    }

    private static String tr(String k, Object... a) {
        return Component.translatable(k, a).getString();
    }

    private boolean b(String k) {
        return data.has(k) && data.get(k).getAsBoolean();
    }

    private int tickets(int t) {
        return data.getAsJsonObject("tickets").getAsJsonArray(CATS[cat]).get(t).getAsInt();
    }

    private int ticketCost(int t) {
        return data.getAsJsonObject("ticketCost").getAsJsonArray(CATS[cat]).get(t).getAsInt();
    }

    static Item ticketItem(int cat, int t) {
        return switch (cat) {
            case 1 -> (t == 2 ? ModItems.PET_TICKET_HIGH : t == 1 ? ModItems.PET_TICKET_MID : ModItems.PET_TICKET_NORMAL).get();
            case 2 -> (t == 2 ? ModItems.ACC_TICKET_HIGH : t == 1 ? ModItems.ACC_TICKET_MID : ModItems.ACC_TICKET_NORMAL).get();
            default -> (t == 2 ? ModItems.TICKET_HIGH : t == 1 ? ModItems.TICKET_MID : ModItems.TICKET_NORMAL).get();
        };
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
        ItemStack st = new ItemStack(item, e.has("count") ? Math.max(1, e.get("count").getAsInt()) : 1);
        if (e.has("nbt") && !e.get("nbt").getAsString().isEmpty()) {
            try {
                st.setTag(net.minecraft.nbt.TagParser.parseTag(e.get("nbt").getAsString()));
            } catch (Exception ignored) {
                // 읽을 수 없는 데이터면 기본 모양
            }
        }
        return st;
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
        return fy + 44 + BANNER + 2;
    }

    private int bodyH() {
        return H - 44 - BANNER - 2 - 28;
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
        // 탭 너비는 글자 길이에 맞춘다 (영어처럼 긴 이름도 버튼 안에)
        String[] catNames = {"rtshop.gacha.cat.weapon", "rtshop.gacha.cat.pet", "rtshop.gacha.cat.accessory"};
        String[] modes = {"rtshop.gacha.mode.pull", "rtshop.gacha.mode.exchange", "rtshop.gacha.mode.dismantle"};
        int[] cw = new int[3];
        int[] mw = new int[3];
        int total = 0;
        for (int i = 0; i < 3; i++) {
            cw[i] = Math.max(50, 26 + this.font.width(tr(catNames[i])));
            mw[i] = Math.max(44, 12 + this.font.width(tr(modes[i])));
            total += cw[i] + mw[i] + 4;
        }
        int room = W - 16 - 8;
        if (total > room) {
            double k = room / (double) total;
            for (int i = 0; i < 3; i++) {
                cw[i] = (int) (cw[i] * k);
                mw[i] = (int) (mw[i] * k);
            }
        }
        int x = fx + 8;
        for (int i = 0; i < 3; i++) {
            final int c = i;
            buttons.add(new Btn(x, fy + 23, cw[i], 18, "#cat" + i, true, () -> {
                setCat(c);
                layout();
            }));
            x += cw[i] + 2;
        }
        int mx = fx + W - 8 - (mw[0] + mw[1] + mw[2] + 4);
        for (int i = 0; i < 3; i++) {
            final int m = i;
            boolean en = mode != i && (i != 1 || b("exchangeEnabled"));
            int bx = mx;
            mx += mw[i] + 2;
            buttons.add(new Btn(bx, fy + 24, mw[i], 16, tr(modes[i]), en, () -> {
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
            int shards = data.get("shards").getAsInt();
            for (int t = 0; t < 3; t++) {
                final int tt = t;
                int y = bodyY() + 6 + t * 30;
                int cost = ticketCost(t);
                buttons.add(new Btn(fx + W - 8 - 112, y + 5, 52, 16, tr("rtshop.gacha.trade1"), at && shards >= cost, () -> trade(tt, 1)));
                buttons.add(new Btn(fx + W - 8 - 56, y + 5, 52, 16, tr("rtshop.gacha.trade5"), at && shards >= cost * 5, () -> trade(tt, 5)));
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

    private void trade(int tier, int n) {
        JsonObject a = new JsonObject();
        a.addProperty("cat", CATS[cat]);
        a.addProperty("tier", TIERS[tier]);
        a.addProperty("n", n);
        send(GachaActionPacket.EXCHANGE, a);
    }

    private void dismantleAll(int max) {
        JsonObject a = new JsonObject();
        a.addProperty("max", max);
        send(GachaActionPacket.DISMANTLE_ALL, a);
    }

    // ------------------------------------------------------------------ 그리기

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        this.renderBackground(g);
        Theme th = theme(cat);
        Ui.woodFrame(g, fx, fy, W, H);
        // 뽑기 종류 색 테두리
        g.fill(fx + 3, fy + 3, fx + W - 3, fy + 5, th.accent());
        g.fill(fx + 3, fy + H - 5, fx + W - 3, fy + H - 3, th.accent());
        g.drawString(this.font, Ui.clip(this.font, data.get("npc").getAsString() + " · " + this.title.getString(), W / 2 - 40), fx + 10, fy + 9, Ui.C_GOLD, true);
        // 오른쪽 위: 이 뽑기의 티켓과 조각
        int x = fx + W - 10;
        String sh = Integer.toString(data.get("shards").getAsInt());
        x -= this.font.width(sh);
        g.drawString(this.font, sh, x, fy + 9, Ui.C_CREAM, false);
        x -= 18;
        g.renderItem(new ItemStack(ModItems.SHARD.get()), x, fy + 4);
        for (int t = 2; t >= 0; t--) {
            String n = Integer.toString(tickets(t));
            x -= this.font.width(n) + 6;
            g.drawString(this.font, n, x, fy + 9, Ui.C_CREAM, false);
            x -= 18;
            g.renderItem(new ItemStack(ticketItem(cat, t)), x, fy + 4);
        }
        if (results != null) {
            renderResults(g, mx, my, partial);
        } else {
            renderTabs(g, mx, my);
            renderBanner(g, partial);
            if (probTier >= 0) {
                renderOdds(g);
            } else if (mode == 0) {
                renderPull(g);
            } else if (mode == 1) {
                renderExchange(g);
            } else {
                renderDismantle(g);
            }
        }
        if (status != null && statusTicks > 0) {
            String s = Ui.clip(this.font, status.getString(), W - 200);
            g.drawString(this.font, s, fx + 94, fy + H - 19, statusOk ? 0xFF8FD67A : 0xFFFF8A7A, false);
        } else if (!b("atNpc") && results == null) {
            g.drawString(this.font, Ui.clip(this.font, tr("rtshop.gacha.view_only", data.get("npc").getAsString()), W - 200), fx + 94, fy + H - 19, Ui.C_PARCH_DARK, false);
        }
        g.flush();
        if (!anim.active()) {
            for (Btn bt : buttons) {
                if (bt.label().startsWith("#cat")) {
                    continue;
                }
                Ui.button(g, this.font, bt.x(), bt.y(), bt.w(), bt.h(), bt.label(), Ui.inside(mx, my, bt.x(), bt.y(), bt.w(), bt.h()), bt.enabled());
            }
        }
        super.render(g, mx, my, partial);
        if (anim.active()) {
            g.flush();
            anim.render(g, this.font, this.width, this.height, partial);
        }
    }

    /** 종류 탭: 고른 탭은 그 종류 색으로 칠하고 아이콘을 띄운다. */
    private void renderTabs(GuiGraphics g, int mx, int my) {
        String[] names = {"rtshop.gacha.cat.weapon", "rtshop.gacha.cat.pet", "rtshop.gacha.cat.accessory"};
        for (Btn bt : buttons) {
            if (!bt.label().startsWith("#cat")) {
                continue;
            }
            int i = bt.label().charAt(4) - '0';
            Theme t = theme(i);
            boolean sel = i == cat;
            boolean hover = Ui.inside(mx, my, bt.x(), bt.y(), bt.w(), bt.h());
            int fill = sel ? t.accent() : hover ? Ui.C_WOOD_HI : Ui.C_WOOD_DARK;
            Ui.bevel(g, bt.x(), bt.y(), bt.w(), bt.h() + (sel ? 3 : 0), fill, sel ? t.light() : Ui.C_WOOD_HI, sel ? t.dark() : Ui.C_SHADOW);
            g.pose().pushPose();
            g.pose().translate(bt.x() + 2, bt.y() + 1, 0);
            g.pose().scale(0.95F, 0.95F, 1);
            g.renderItem(new ItemStack(ticketItem(i, 2)), 0, 0);
            g.pose().popPose();
            String label = tr(names[i]);
            int avail = bt.w() - 23;
            int tw = this.font.width(label);
            int col = sel ? 0xFFFFFFFF : (hover ? Ui.C_CREAM : Ui.C_PARCH_SHADE);
            if (tw <= avail) {
                g.drawString(this.font, label, bt.x() + 20, bt.y() + 5, col, sel);
            } else if (tw * 0.75F <= avail) {
                Ui.scaledText(g, this.font, label, bt.x() + 20, bt.y() + 6, avail / (float) tw, col, sel);
            } else {
                g.drawString(this.font, Ui.clip(this.font, label, avail), bt.x() + 20, bt.y() + 5, col, sel);
            }
        }
    }

    /** 종류 배너: 색 띠 + 빗금 무늬 + 큰 아이콘·이름·설명 + 지나가는 빛. 종류를 바꾸면 옆에서 밀려 들어온다. */
    private void renderBanner(GuiGraphics g, float partial) {
        Theme t = theme(cat);
        int x0 = fx + 6;
        int x1 = fx + W - 6;
        int y0 = fy + 44;
        int y1 = y0 + BANNER;
        g.fill(x0, y0, x1, y1, t.dark());
        g.enableScissor(x0, y0, x1, y1);
        // 종류마다 다른 무늬
        int stripe = 0x22000000 | (t.light() & 0xFFFFFF);
        if (cat == 0) {
            for (int sx = x0 - BANNER; sx < x1; sx += 10) {
                for (int r = 0; r < BANNER; r++) {
                    g.fill(sx + r, y0 + r, sx + r + 3, y0 + r + 1, stripe);
                }
            }
        } else if (cat == 1) {
            for (int sx = x0 + 4; sx < x1; sx += 22) {
                int yy = y0 + ((sx / 22) % 2 == 0 ? 3 : 10);
                g.fill(sx + 1, yy + 3, sx + 6, yy + 7, stripe);
                g.fill(sx, yy, sx + 2, yy + 2, stripe);
                g.fill(sx + 5, yy, sx + 7, yy + 2, stripe);
            }
        } else {
            for (int sx = x0 + 6; sx < x1; sx += 18) {
                int yy = y0 + ((sx / 18) % 3) * 5 + 3;
                g.fill(sx + 2, yy, sx + 3, yy + 5, stripe);
                g.fill(sx, yy + 2, sx + 5, yy + 3, stripe);
            }
        }
        // 지나가는 빛
        float sweep = ((animTicks + partial) % 80) / 80F;
        int bx = (int) (x0 - 40 + sweep * (x1 - x0 + 80));
        for (int r = 0; r < BANNER; r++) {
            g.fill(bx + r / 2, y0 + r, bx + r / 2 + 14, y0 + r + 1, 0x18FFFFFF);
        }
        g.disableScissor();
        float slide = switchTicks > 0 ? (switchTicks - partial) / 10F : 0;
        int off = (int) (-60 * slide * slide);
        g.fill(x0, y0, x1, y0 + 1, t.accent());
        g.fill(x0, y1 - 1, x1, y1, t.accent());
        g.renderItem(new ItemStack(ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(t.icon()))), x0 + 4 + off, y0 + 2);
        Ui.scaledText(g, this.font, tr("rtshop.gacha.banner." + CATS[cat]), x0 + 24 + off, y0 + 5, 1.25F, 0xFF000000 | (t.light() & 0xFFFFFF), true);
        String sub = tr("rtshop.gacha.banner_sub." + CATS[cat]);
        g.drawString(this.font, Ui.clip(this.font, sub, W - 160), x1 - 6 - Math.min(this.font.width(sub), W - 160), y0 + 6, 0xCCFFFFFF & (0xFF000000 | t.light()), false);
        if (switchTicks > 0) {
            g.fill(x0, y0, x1, y1, ((int) (slide * 120) << 24) | 0xFFFFFF);
        }
    }

    private void renderPull(GuiGraphics g) {
        Theme th = theme(cat);
        int pw = (W - 16 - 8) / 3;
        for (int t = 0; t < 3; t++) {
            int px = fx + 8 + t * (pw + 4);
            int py = bodyY();
            Ui.bevel(g, px, py, pw, bodyH(), th.panel(), th.panelHi(), th.panelShade());
            g.fill(px + 1, py + 1, px + pw - 1, py + 22, th.accent());
            g.fill(px + 1, py + 22, px + pw - 1, py + 23, th.dark());
            g.renderItem(new ItemStack(ticketItem(cat, t)), px + 4, py + 3);
            g.drawString(this.font, tr("rtshop.gacha.tier." + TIERS[t]), px + 24, py + 4, 0xFFFFFFFF, true);
            g.drawString(this.font, tr("rtshop.gacha.have", tickets(t)), px + 24, py + 13, 0xFF000000 | (th.light() & 0xFFFFFF), false);
            double[] c = chances(t);
            int yy = py + 27;
            boolean any = false;
            for (int i = GRADES.length - 1; i >= 0; i--) {
                if (c[i] <= 0) {
                    continue;
                }
                any = true;
                String line = tr("rtshop.gacha.grade." + GRADES[i]) + "  " + pct(c[i]);
                g.drawString(this.font, line, px + 8, yy, GachaColors.dark(GRADES[i]), false);
                g.fill(px + 4, yy + 1, px + 6, yy + 7, GachaColors.argb(GRADES[i]));
                yy += 10;
            }
            if (cat == 1 && any && c[0] + c[1] > 0) {
                g.drawString(this.font, Ui.clip(this.font, tr("rtshop.gacha.pet_note"), pw - 8), px + 4, yy + 1, Ui.C_INK_SOFT, false);
            }
            if (!any) {
                g.drawString(this.font, tr("rtshop.gacha.empty_short"), px + 8, yy, Ui.C_INK_SOFT, false);
            }
            String gu = data.get("tenGuarantee").getAsString();
            if (!gu.isEmpty() && any) {
                String s = tr("rtshop.gacha.guarantee_short", tr("rtshop.gacha.grade." + gu));
                g.drawString(this.font, Ui.clip(this.font, s, pw - 8), px + 4, py + bodyH() - 50, Ui.C_INK_SOFT, false);
            }
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
        return v >= 0.1 ? String.format("%.2f%%", v) : String.format("%.3f%%", v).replaceAll("0+%$", "%");
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
        g.fill(fx + 6, fy + 23, fx + W - 6, fy + 41, theme(cat).dark());
        g.drawString(this.font, tr("rtshop.gacha.odds_title", tr("rtshop.gacha.cat." + CATS[cat]), tr("rtshop.gacha.tier." + TIERS[probTier])),
                fx + 10, fy + 28, Ui.C_GOLD, false);
        int x = fx + 8;
        int y = bodyY();
        int h = bodyH();
        Theme th = theme(cat);
        Ui.bevel(g, x, y, W - 16, h, th.panel(), th.panelHi(), th.panelShade());
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

    /** 조각으로 이 종류의 티켓을 바꾼다. */
    private void renderExchange(GuiGraphics g) {
        Theme th = theme(cat);
        int shards = data.get("shards").getAsInt();
        for (int t = 0; t < 3; t++) {
            int y = bodyY() + 6 + t * 30;
            Ui.bevel(g, fx + 8, y, W - 16, 26, th.panel(), th.panelHi(), th.panelShade());
            g.fill(fx + 9, y + 1, fx + 12, y + 25, th.accent());
            g.renderItem(new ItemStack(ticketItem(cat, t)), fx + 16, y + 5);
            g.drawString(this.font, new ItemStack(ticketItem(cat, t)).getHoverName().getString(), fx + 36, y + 4, Ui.C_INK, false);
            g.drawString(this.font, tr("rtshop.gacha.trade_cost", ticketCost(t), tickets(t)), fx + 36, y + 15, Ui.C_INK_SOFT, false);
        }
        int y = bodyY() + 6 + 3 * 30 + 4;
        g.drawString(this.font, Ui.clip(this.font, tr("rtshop.gacha.trade_note", shards), W - 20), fx + 10, y, Ui.C_CREAM, false);
    }

    private void renderDismantle(GuiGraphics g) {
        JsonArray inv = data.getAsJsonArray("inventory");
        int per = bodyH() / ROW - 1;
        JsonArray dm = data.getAsJsonArray("dismantle");
        for (int k = 0; k < per && scroll + k < inv.size(); k++) {
            JsonObject e = inv.get(scroll + k).getAsJsonObject();
            int gi = gradeIndex(e.get("grade").getAsString());
            int y = bodyY() + k * ROW;
            Theme th = theme(cat);
            Ui.bevel(g, fx + 8, y, W - 16 - 64, ROW - 1, th.panel(), th.panelHi(), th.panelShade());
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

    /** 뽑기 결과: 카드가 하나씩 뒤집힌다 (영웅은 보랏빛, 전설은 금빛 테두리가 돈다). */
    private void renderResults(GuiGraphics g, int mx, int my, float partial) {
        Theme th = theme(cat);
        g.fill(fx + 4, fy + 20, fx + W - 4, fy + H - 30, 0xE0100804);
        g.fill(fx + 4, fy + 20, fx + W - 4, fy + 22, th.accent());
        if (anim.active()) {
            return;
        }
        int n = results.size();
        int cols = Math.min(5, n);
        int rows = (n + cols - 1) / cols;
        int cw = 74;
        int ch = 74;
        int gx = fx + (W - cols * (cw + 4)) / 2;
        int gy = fy + 26 + (H - 60 - rows * (ch + 4)) / 2;
        float time = resultTicks + partial;
        for (int i = 0; i < n; i++) {
            JsonObject e = results.get(i).getAsJsonObject();
            int cx = gx + (i % cols) * (cw + 4);
            int cy = gy + (i / cols) * (ch + 4);
            int gi = gradeIndex(e.get("grade").getAsString());
            float since = time - (i * 3 + 4);
            if (since < 0) {
                Ui.bevel(g, cx, cy, cw, ch, Ui.C_WOOD, Ui.C_WOOD_HI, Ui.C_WOOD_DARK);
                g.fill(cx + 4, cy + 4, cx + cw - 4, cy + 6, th.accent());
                g.fill(cx + 4, cy + ch - 6, cx + cw - 4, cy + ch - 4, th.accent());
                g.drawCenteredString(this.font, "?", cx + cw / 2, cy + ch / 2 - 4, Ui.C_GOLD);
                continue;
            }
            // 뒤집히는 동안 옆으로 좁아졌다 넓어진다
            float flip = Math.min(1, since / 3F);
            g.pose().pushPose();
            g.pose().translate(cx + cw / 2F, cy + ch / 2F, 0);
            g.pose().scale(Math.max(0.05F, flip), 1, 1);
            g.pose().translate(-(cx + cw / 2F), -(cy + ch / 2F), 0);
            int col = GachaColors.argb(GRADES[gi]);
            if (gi >= 3) {
                int pulse = (int) (60 + 40 * Math.sin(time * 0.3));
                g.fill(cx - 3, cy - 3, cx + cw + 3, cy + ch + 3, (pulse << 24) | (col & 0xFFFFFF));
            }
            Ui.bevel(g, cx, cy, cw, ch, Ui.C_PARCH, Ui.C_PARCH_HI, Ui.C_PARCH_SHADE);
            if (gi >= 3) {
                // 테두리를 따라 도는 빛
                float per = 2 * (cw + ch);
                for (int k = 0; k < (gi >= 4 ? 2 : 1); k++) {
                    float pos = ((time * (gi >= 4 ? 6 : 4) + k * per / 2) % per);
                    for (int j = 0; j < 14; j++) {
                        float p = (pos - j * 2 + per) % per;
                        int px, py;
                        if (p < cw) {
                            px = (int) p; py = 0;
                        } else if (p < cw + ch) {
                            px = cw - 1; py = (int) (p - cw);
                        } else if (p < 2 * cw + ch) {
                            px = (int) (cw - 1 - (p - cw - ch)); py = ch - 1;
                        } else {
                            px = 0; py = (int) (ch - 1 - (p - 2 * cw - ch));
                        }
                        int a = 255 - j * 16;
                        g.fill(cx + px - 1, cy + py - 1, cx + px + 2, cy + py + 2, (a << 24) | (gi == 5 ? 0xBFE6FF : gi == 4 ? 0xFFF4C0 : 0xF0D8FF));
                    }
                }
            }
            g.fill(cx, cy, cx + cw, cy + 3, col);
            g.fill(cx, cy + ch - 3, cx + cw, cy + ch, col);
            g.pose().pushPose();
            g.pose().translate(cx + cw / 2F - 16, cy + 10, 0);
            g.pose().scale(2, 2, 1);
            g.renderItem(icon(e), 0, 0);
            g.renderItemDecorations(this.font, icon(e), 0, 0);
            g.pose().popPose();
            g.flush();
            g.pose().pushPose();
            g.pose().translate(0, 0, 300);
            String gn = tr("rtshop.gacha.grade." + GRADES[gi]);
            g.drawString(this.font, gn, cx + (cw - this.font.width(gn)) / 2, cy + 46, GachaColors.dark(GRADES[gi]), false);
            String nm = Ui.clip(this.font, name(e), cw - 6);
            g.drawString(this.font, nm, cx + (cw - this.font.width(nm)) / 2, cy + 58, Ui.C_INK, false);
            if (e.has("new") && e.get("new").getAsBoolean()) {
                g.drawString(this.font, "NEW", cx + 3, cy + 5, 0xFFE03C2C, false);
            }
            g.pose().popPose();
            g.pose().popPose();
        }
    }

    /** 연출 중에는 대화·퀘스트 단추를 숨긴다. */
    @Override
    public boolean npcButtonsHidden() {
        return anim.active();
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (anim.active()) return true; // ESC 포함 모든 키를 막는다
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean keyReleased(int key, int scan, int mods) {
        if (anim.active()) return true;
        return super.keyReleased(key, scan, mods);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (anim.active()) return true;
        return super.charTyped(c, mods);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (anim.active()) return true;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (anim.active()) return true;
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (anim.active()) {
            anim.skip(); // 어느 단추로 눌러도 연출만 넘긴다
            return true;
        }
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
        if (anim.active()) return true;
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
        anim.skip();
        cat = catIndex;
        mode = modeIndex;
        probTier = oddsTier;
        scroll = 0;
        layout();
    }

    public void revealForTest() {
        anim.skip();
        if (results != null) {
            resultTicks = 1000;
        }
    }

    public int fxTickForTest() {
        return anim.tickForTest();
    }

    /** 점검용: 연출(0 일반 ~ 5 야마토)의 한 장면을 띄운다. */
    public void fxForTest(int grade, int tick) {
        JsonObject e = null;
        for (JsonElement el : pool()) {
            if (el.getAsJsonObject().get("valid").getAsBoolean()) {
                e = el.getAsJsonObject();
                if (gradeIndex(e.get("grade").getAsString()) >= grade) {
                    break;
                }
            }
        }
        if (results == null) {
            results = new JsonArray();
        }
        anim.start(grade, e == null ? new ItemStack(ModItems.SHARD.get()) : icon(e), e == null ? "?" : name(e));
        anim.jumpTo(tick);
        layout();
    }

    /** 점검용: 탭과 모드 버튼의 글자가 버튼 안에 들어가는지 (탭은 아이콘 옆 공간 기준, 줄여 그리는 것은 허용). */
    public boolean tabsFitForTest() {
        return tabsReportForTest().isEmpty();
    }

    public String tabsReportForTest() {
        StringBuilder sb = new StringBuilder();
        String[] names = {"rtshop.gacha.cat.weapon", "rtshop.gacha.cat.pet", "rtshop.gacha.cat.accessory"};
        for (Btn bt : buttons) {
            if (bt.label().startsWith("#cat")) {
                String l = tr(names[bt.label().charAt(4) - '0']);
                if (this.font.width(l) * 0.75F > bt.w() - 23) {
                    sb.append(l).append(' ');
                }
            } else if (this.font.width(bt.label()) > bt.w() - 4) {
                sb.append(bt.label()).append(' ');
            }
            if (bt.x() + bt.w() > fx + W - 4) {
                sb.append("outside:").append(bt.label()).append(' ');
            }
        }
        return sb.toString().trim();
    }

    public boolean fxActiveForTest() {
        return anim.active();
    }

    /** 점검용: 편집 버튼과 같은 요청. */
    public void clickForTest() {
        send(GachaActionPacket.EDIT, null);
    }
}

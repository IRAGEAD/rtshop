package com.rtshop.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

/**
 * 조사대 도감: 위에서 몹·보스·장비를 고르고, 왼쪽 칸에서 하나를 누르면 오른쪽에 자세한 내용이 나온다.
 * 아직 마주치지 않은 몹·얻지 못한 장비는 「?」 로 잠겨 있다. 마주친 몹은 모습과 이름만, 잡은 몹은 능력치·전리품·설명까지.
 * 이름으로 찾기, 모드별로 보기, 적힌 것만 보기.
 */
public class CodexScreen extends Screen {
    private static final int W = 396, H = 236, CELL = 20, COLS = 9, ROWS = 9;
    private static final int TAB_MOB = 0, TAB_BOSS = 1, TAB_GEAR = 2;

    private static final class Entry {
        String id, ns, name = "", lower = "";
        boolean boss, gear;
        int state, kills;
        ItemStack icon = ItemStack.EMPTY;
        EntityType<?> type;
    }

    private final List<Entry> all = new ArrayList<>(), shown = new ArrayList<>();
    private final List<String> mods = new ArrayList<>();
    private static final Map<String, JsonObject> DETAILS = new HashMap<>();
    private static final Map<String, String> MOD_NAMES = new HashMap<>();
    private int tab = TAB_MOB, scroll, textScroll, fx, fy, modIndex = -1;
    private boolean knownOnly;
    private Entry sel;
    private EditBox search;
    private String query = "";
    private final int[] known = new int[3], total = new int[3];

    public CodexScreen(JsonObject o) {
        super(Component.translatable("rtshop.codex.title"));
        DETAILS.clear();
        for (JsonElement e : o.getAsJsonArray("mobs")) {
            JsonArray a = e.getAsJsonArray();
            Entry en = new Entry();
            en.id = a.get(0).getAsString();
            en.boss = a.get(1).getAsInt() == 1;
            en.state = a.get(2).getAsInt();
            en.kills = a.get(3).getAsInt();
            ResourceLocation rl = ResourceLocation.tryParse(en.id);
            en.type = rl == null ? null : ForgeRegistries.ENTITY_TYPES.getValue(rl);
            if (en.type == null) continue;
            en.ns = rl.getNamespace();
            en.name = en.type.getDescription().getString();
            SpawnEggItem egg = SpawnEggItem.byId(en.type);
            if (egg != null) en.icon = new ItemStack(egg);
            en.lower = en.name.toLowerCase(Locale.ROOT);
            all.add(en);
        }
        for (JsonElement e : o.getAsJsonArray("gear")) {
            JsonArray a = e.getAsJsonArray();
            Entry en = new Entry();
            en.id = a.get(0).getAsString();
            en.gear = true;
            en.state = a.get(1).getAsInt() == 1 ? 2 : 0;
            ResourceLocation rl = ResourceLocation.tryParse(en.id);
            Item item = rl == null ? null : ForgeRegistries.ITEMS.getValue(rl);
            if (item == null || item == Items.AIR) continue;
            en.ns = rl.getNamespace();
            en.icon = new ItemStack(item);
            en.name = en.icon.getHoverName().getString();
            en.lower = en.name.toLowerCase(Locale.ROOT);
            all.add(en);
        }
        for (Entry en : all) {
            int t = tabOf(en);
            total[t]++;
            if (en.state > 0) known[t]++;
        }
    }

    public static void open(JsonObject o) {
        Minecraft.getInstance().setScreen(new CodexScreen(o));
    }

    /** 서버가 보낸 몹 하나의 자세한 내용. */
    public static void detail(JsonObject o) {
        DETAILS.put(o.get("id").getAsString(), o);
    }

    private static int tabOf(Entry e) {
        return e.gear ? TAB_GEAR : e.boss ? TAB_BOSS : TAB_MOB;
    }

    static String modName(String ns) {
        return MOD_NAMES.computeIfAbsent(ns, k -> k.equals("minecraft") ? "Minecraft"
                : ModList.get().getModContainerById(k).map(c -> c.getModInfo().getDisplayName()).orElse(k));
    }

    @Override
    protected void init() {
        fx = (width - W) / 2;
        fy = (height - H) / 2;
        search = new EditBox(font, fx + 206, fy + 29, 84, 12, Component.translatable("rtshop.codex.search"));
        search.setMaxLength(40);
        search.setValue(query);
        search.setHint(Component.translatable("rtshop.codex.search"));
        search.setResponder(s -> {
            query = s;
            refilter();
        });
        addRenderableWidget(search);
        refilter();
    }

    private void refilter() {
        mods.clear();
        TreeSet<String> ns = new TreeSet<>();
        for (Entry e : all) if (tabOf(e) == tab) ns.add(e.ns);
        mods.addAll(ns);
        if (modIndex >= mods.size()) modIndex = -1;
        String mod = modIndex < 0 ? null : mods.get(modIndex);
        String q = query.trim().toLowerCase(Locale.ROOT);
        shown.clear();
        for (Entry e : all) {
            if (tabOf(e) != tab) continue;
            if (mod != null && !mod.equals(e.ns)) continue;
            if (knownOnly && e.state == 0) continue;
            if (!q.isEmpty() && (e.state == 0 || !e.lower.contains(q))) continue;   // 잠긴 것은 이름으로 찾을 수 없다
            shown.add(e);
        }
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
        if (sel != null && !shown.contains(sel)) sel = null;
    }

    private int maxScroll() {
        return Math.max(0, (shown.size() + COLS - 1) / COLS - ROWS);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int gx() {
        return fx + 8;
    }

    private int gy() {
        return fy + 46;
    }

    private int dx() {
        return fx + 8 + COLS * CELL + 12;
    }

    private int dw() {
        return fx + W - 8 - dx();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        Ui.woodFrame(g, fx, fy, W, H);
        Ui.titleBand(g, fx + 5, fy + 4, fx + W - 5, fy + 23);
        g.fill(fx + 5, fy + 22, fx + W - 5, fy + 23, Ui.C_GOLD_DARK);
        String t = title.getString();
        g.drawString(font, t, fx + (W - font.width(t)) / 2, fy + 9, Ui.C_GOLD, true);
        String cnt = Component.translatable("rtshop.codex.count", known[tab], total[tab]).getString();
        Ui.scaledText(g, font, cnt, fx + W - 10 - font.width(cnt) * 0.8F, fy + 10, 0.8F, Ui.C_CREAM, false);
        // 갈래
        String[] tabs = {"rtshop.codex.tab_mob", "rtshop.codex.tab_boss", "rtshop.codex.tab_gear"};
        for (int i = 0; i < 3; i++) {
            int tx = fx + 8 + i * 64;
            boolean on = tab == i, hov = Ui.inside(mx, my, tx, fy + 27, 62, 15);
            Ui.bevel(g, tx, fy + 27, 62, 15, on ? Ui.C_WOOD_HI : Ui.C_WOOD_DARK, on ? Ui.C_GOLD_DARK : Ui.C_WOOD, Ui.C_SHADOW);
            String label = Component.translatable(tabs[i]).getString();
            g.drawString(font, label, tx + (62 - font.width(label)) / 2, fy + 31, on ? 0xFFFFF2C8 : hov ? Ui.C_GOLD : Ui.C_CREAM, false);
        }
        // 모드·적힌 것만
        int bx = fx + 294;
        String mod = modIndex < 0 ? Component.translatable("rtshop.codex.all_mods").getString() : modName(mods.get(modIndex));
        Ui.button(g, font, bx, fy + 27, 62, 15, Ui.clip(font, mod, 56), Ui.inside(mx, my, bx, fy + 27, 62, 15), true);
        String ko = Component.translatable(knownOnly ? "rtshop.codex.known_on" : "rtshop.codex.known_off").getString();
        Ui.button(g, font, bx + 64, fy + 27, 30, 15, ko, Ui.inside(mx, my, bx + 64, fy + 27, 30, 15), true);
        // 칸
        int gx = gx(), gy = gy();
        Ui.bevel(g, gx - 2, gy - 2, COLS * CELL + 8, ROWS * CELL + 4, Ui.C_WOOD_DARK, Ui.C_WOOD, Ui.C_SHADOW);
        Entry hover = null;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int i = (scroll + r) * COLS + c;
                if (i >= shown.size()) break;
                Entry e = shown.get(i);
                int x = gx + c * CELL, y = gy + r * CELL;
                boolean hov = Ui.inside(mx, my, x, y, CELL - 1, CELL - 1);
                if (hov) hover = e;
                g.fill(x, y, x + CELL - 1, y + CELL - 1, e == sel ? 0xFF7A5A2A : e.state == 0 ? 0xFF24160C : 0xFF4A3018);
                if (hov) g.fill(x, y, x + CELL - 1, y + CELL - 1, 0x30FFFFFF);
                if (e.state == 0) {
                    g.drawString(font, "?", x + 7, y + 6, 0xFF6A5438, false);
                } else if (!e.icon.isEmpty()) {
                    g.renderItem(e.icon, x + 1, y + 1);
                } else {
                    String ch = e.name.isEmpty() ? "?" : e.name.substring(0, e.name.offsetByCodePoints(0, 1));
                    g.drawString(font, ch, x + 9 - font.width(ch) / 2, y + 6, Ui.C_CREAM, false);
                }
                if (!e.gear && e.state == 2) g.fill(x + CELL - 5, y + 1, x + CELL - 2, y + 4, Ui.C_GOLD);        // 잡은 몹
                else if (!e.gear && e.state == 1) g.fill(x + CELL - 5, y + 1, x + CELL - 2, y + 4, 0xFF8A8A8A);   // 마주치기만 한 몹
            }
        }
        if (shown.isEmpty()) g.drawString(font, Component.translatable("rtshop.codex.none").getString(), gx + 6, gy + 6, 0xFFB8A888, false);
        if (maxScroll() > 0) {
            int sh = ROWS * CELL, bh = Math.max(12, sh * ROWS / ((shown.size() + COLS - 1) / COLS));
            int by = gy + (sh - bh) * scroll / maxScroll();
            g.fill(gx + COLS * CELL + 1, by, gx + COLS * CELL + 4, by + bh, Ui.C_GOLD_DARK);
        }
        // 자세히
        int dx = dx(), dw = dw();
        Ui.bevel(g, dx, gy - 2, dw, ROWS * CELL + 4, Ui.C_WOOD_DARK, Ui.C_WOOD, Ui.C_SHADOW);
        g.enableScissor(dx + 2, gy, dx + dw - 2, gy + ROWS * CELL);
        if (sel == null) {
            List<FormattedCharSequence> w = font.split(Component.translatable("rtshop.codex.pick"), dw - 16);
            for (int i = 0; i < w.size(); i++) g.drawString(font, w.get(i), dx + 8, gy + 8 + i * 11, 0xFFB8A888, false);
        } else if (sel.gear) {
            gearDetail(g, dx, gy, dw);
        } else {
            mobDetail(g, mx, my, dx, gy, dw);
        }
        g.disableScissor();
        super.render(g, mx, my, partial);
        if (hover != null && !search.isFocused()) {
            g.renderTooltip(font, Component.literal(hover.state == 0 ? "???" : hover.name), mx, my);
        }
        if (dropHover != null) {
            g.renderTooltip(font, dropHover, mx, my);
            dropHover = null;
        }
    }

    private Component dropHover;

    private void mobDetail(GuiGraphics g, int mx, int my, int dx, int gy, int dw) {
        Entry e = sel;
        int y = gy + 4 - textScroll;
        if (e.state == 0) {
            g.drawString(font, "???", dx + 8, y + 4, Ui.C_CREAM, false);
            lines(g, Component.translatable("rtshop.codex.mob_locked").getString(), dx + 8, y + 20, dw - 16, 0xFFB8A888);
            return;
        }
        g.fill(dx + 6, y, dx + 70, y + 64, 0xFF24160C);
        BountyScreen.drawMob(g, e.id, dx + 6, y, 64, (System.currentTimeMillis() % 14400) / 40F);
        int tx = dx + 76, tw = dw - 82;
        int ny = y + 2;
        for (FormattedCharSequence s : font.split(Component.literal(e.name), tw)) {
            g.drawString(font, s, tx, ny, e.boss ? 0xFFFF9A6A : Ui.C_GOLD, false);
            ny += 10;
        }
        Ui.scaledText(g, font, Ui.clip(font, modName(e.ns), (int) (tw / 0.8F)), tx, ny + 1, 0.8F, 0xFFB8A888, false);
        ny += 11;
        String st = e.state == 2 ? Component.translatable("rtshop.codex.kills", e.kills).getString() : Component.translatable("rtshop.codex.met_only").getString();
        g.drawString(font, st, tx, ny, e.state == 2 ? 0xFF9AD08A : 0xFFB8A888, false);
        JsonObject d = DETAILS.get(e.id);
        y += 70;
        if (e.state < 2) {
            lines(g, Component.translatable("rtshop.codex.mob_met").getString(), dx + 8, y, dw - 16, 0xFFB8A888);
            return;
        }
        if (d == null || !d.has("hp")) {
            g.drawString(font, "…", dx + 8, y, 0xFFB8A888, false);
            return;
        }
        if (d.has("epithet") && !d.get("epithet").getAsString().isEmpty()) {
            y = lines(g, d.get("epithet").getAsString(), dx + 8, y, dw - 16, 0xFFFFC890) + 2;
        }
        StringBuilder stats = new StringBuilder(Component.translatable("rtshop.codex.hp", num(d.get("hp").getAsDouble())).getString());
        if (d.has("atk")) stats.append("  ").append(Component.translatable("rtshop.codex.atk", num(d.get("atk").getAsDouble())).getString());
        if (d.has("armor") && d.get("armor").getAsDouble() > 0) stats.append("  ").append(Component.translatable("rtshop.codex.armor", num(d.get("armor").getAsDouble())).getString());
        y = lines(g, stats.toString(), dx + 8, y, dw - 16, Ui.C_CREAM) + 3;
        JsonArray drops = d.has("drops") ? d.getAsJsonArray("drops") : new JsonArray();
        g.drawString(font, Component.translatable("rtshop.codex.drops").getString(), dx + 8, y, Ui.C_GOLD, false);
        y += 11;
        if (drops.size() == 0) {
            g.drawString(font, Component.translatable("rtshop.codex.no_drops").getString(), dx + 8, y, 0xFFB8A888, false);
            y += 12;
        } else {
            int per = Math.max(1, (dw - 14) / 18);
            for (int i = 0; i < drops.size(); i++) {
                JsonArray a = drops.get(i).getAsJsonArray();
                Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(a.get(0).getAsString()));
                if (item == null) continue;
                int ix = dx + 8 + (i % per) * 18, iy = y + (i / per) * 18;
                g.fill(ix - 1, iy - 1, ix + 17, iy + 17, 0xFF24160C);
                ItemStack s = new ItemStack(item);
                g.renderItem(s, ix, iy);
                if (Ui.inside(mx, my, ix, iy, 17, 17) && my >= gy && my < gy + ROWS * CELL) {
                    int lo = a.get(1).getAsInt(), hi = a.get(2).getAsInt();
                    dropHover = Component.translatable("rtshop.codex.drop_tip", s.getHoverName(), lo == hi ? String.valueOf(lo) : lo + "~" + hi, a.get(3).getAsInt());
                }
            }
            y += ((drops.size() + per - 1) / per) * 18 + 3;
        }
        String note = d.has("note") ? d.get("note").getAsString() : "";
        if (!note.isEmpty()) {
            g.fill(dx + 8, y, dx + dw - 8, y + 1, Ui.C_WOOD_HI);
            y += 5;
            for (String para : note.split("\n")) y = lines(g, para, dx + 8, y, dw - 16, 0xFFE0D2B0) + 2;
        }
        contentH = y + textScroll - gy;
    }

    private void gearDetail(GuiGraphics g, int dx, int gy, int dw) {
        Entry e = sel;
        int y = gy + 4 - textScroll;
        if (e.state == 0) {
            g.drawString(font, "???", dx + 8, y + 4, Ui.C_CREAM, false);
            lines(g, Component.translatable("rtshop.codex.gear_locked").getString(), dx + 8, y + 20, dw - 16, 0xFFB8A888);
            return;
        }
        g.fill(dx + 6, y, dx + 58, y + 52, 0xFF24160C);
        g.pose().pushPose();
        g.pose().translate(dx + 8, y + 2, 0);
        g.pose().scale(3F, 3F, 1F);
        g.renderItem(e.icon, 0, 0);
        g.pose().popPose();
        int tx = dx + 64, tw = dw - 70, ny = y + 2;
        for (FormattedCharSequence s : font.split(e.icon.getHoverName(), tw)) {
            g.drawString(font, s, tx, ny, Ui.C_GOLD, false);
            ny += 10;
        }
        Ui.scaledText(g, font, Ui.clip(font, modName(e.ns), (int) (tw / 0.8F)), tx, ny + 1, 0.8F, 0xFFB8A888, false);
        y += 58;
        List<Component> tip;
        try {
            tip = e.icon.getTooltipLines(Minecraft.getInstance().player, TooltipFlag.Default.NORMAL);
        } catch (Throwable t) {
            tip = List.of();
        }
        for (int i = 1; i < tip.size(); i++) {
            Component line = tip.get(i);
            if (line.getString().isEmpty()) {
                y += 5;
                continue;
            }
            for (FormattedCharSequence s : font.split(line, dw - 16)) {
                g.drawString(font, s, dx + 8, y, 0xFFE0D2B0, false);
                y += 10;
            }
        }
        contentH = y + textScroll - gy;
    }

    private int contentH;

    private int lines(GuiGraphics g, String text, int x, int y, int w, int color) {
        for (FormattedCharSequence s : font.split(Component.literal(text), w)) {
            g.drawString(font, s, x, y, color, false);
            y += 10;
        }
        return y;
    }

    static String num(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.1f", v);
    }

    private void select(Entry e) {
        sel = e;
        textScroll = 0;
        contentH = 0;
        if (e != null && !e.gear && e.state == 2 && !DETAILS.containsKey(e.id)) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) mc.player.connection.sendCommand("rtshop codex mob " + e.id);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 || button == 1) {
            for (int i = 0; i < 3; i++) {
                if (Ui.inside(mx, my, fx + 8 + i * 64, fy + 27, 62, 15) && tab != i) {
                    tab = i;
                    modIndex = -1;
                    scroll = 0;
                    sel = null;
                    click();
                    refilter();
                    return true;
                }
            }
            int bx = fx + 294;
            if (Ui.inside(mx, my, bx, fy + 27, 62, 15)) {
                // 왼쪽 단추 = 다음 모드, 오른쪽 단추 = 앞 모드 (-1 = 전체)
                int n = mods.size() + 1, cur = modIndex + 1;
                cur = ((cur + (button == 0 ? 1 : -1)) % n + n) % n;
                modIndex = cur - 1;
                scroll = 0;
                click();
                refilter();
                return true;
            }
            if (Ui.inside(mx, my, bx + 64, fy + 27, 30, 15)) {
                knownOnly = !knownOnly;
                scroll = 0;
                click();
                refilter();
                return true;
            }
            int gx = gx(), gy = gy();
            if (Ui.inside(mx, my, gx, gy, COLS * CELL, ROWS * CELL)) {
                int c = (int) ((mx - gx) / CELL), r = (int) ((my - gy) / CELL);
                int i = (scroll + r) * COLS + c;
                if (i >= 0 && i < shown.size()) {
                    select(shown.get(i));
                    click();
                }
                search.setFocused(false);
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (mx >= dx()) {
            int max = Math.max(0, contentH - ROWS * CELL + 8);
            textScroll = Math.max(0, Math.min(max, textScroll - (int) Math.signum(delta) * 12));
        } else {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(delta)));
        }
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods_) {
        if (search.isFocused() && key != 256) {
            search.keyPressed(key, scan, mods_);
            return true;   // 찾는 글을 치는 동안에는 다른 단축키가 먹지 않게
        }
        return super.keyPressed(key, scan, mods_);
    }

    private void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    // ---- 시험용
    public int totalForTest(int t) {
        return total[t];
    }

    public int knownForTest(int t) {
        return known[t];
    }

    public int shownForTest() {
        return shown.size();
    }

    public void tabForTest(int t) {
        tab = t;
        modIndex = -1;
        scroll = 0;
        sel = null;
        refilter();
    }

    public void knownOnlyForTest(boolean on) {
        knownOnly = on;
        refilter();
    }

    public void searchForTest(String q) {
        search.setValue(q);
    }

    /** 이 ID 를 고른다 (목록에 보일 때만). 골랐으면 true. */
    public boolean selectForTest(String id) {
        for (Entry e : shown) {
            if (e.id.equals(id)) {
                select(e);
                return true;
            }
        }
        return false;
    }

    public int stateForTest(String id) {
        for (Entry e : all) if (e.id.equals(id)) return e.state;
        return -1;
    }

    public static boolean hasDetailForTest(String id) {
        JsonObject d = DETAILS.get(id);
        return d != null && d.has("hp");
    }

    public int modsForTest() {
        return mods.size();
    }
}

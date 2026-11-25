package com.rtshop.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.rtshop.magic.MagicMenu;
import com.rtshop.net.MagicActionPacket;
import com.rtshop.net.ShopNet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * 룬 제단 창: [분야 선택 뽑기] [무작위 뽑기] [인챈트 재설정].
 * 흑요석 판 위에 빛나는 소환진, 가운데 구슬, 오각으로 놓인 룬석 다섯(분야 단추). 뽑으면 「룬석 소환진」 연출(MagicFx) 뒤에 결과를 보여 준다.
 * 인챈트 재설정은 무기·장비를 제단 위 칸에 올려놓고 한다 (가방 칸이 함께 보인다). 글꼴·크기·그림자·연출은 [화면 설정] 에서 고른다.
 */
public class MagicGachaScreen extends Screen {
    public static final String[] CATS = {"attack", "guard", "dodge", "passive", "mover"};
    static final String[] GRADES = {"common", "uncommon", "rare", "epic", "legendary", "yamato"};

    private JsonObject data;
    private int tab = 0; // 0 선택, 1 무작위, 2 재설정
    private int cat = 0;
    private final MagicFx fx = new MagicFx();
    private List<MagicFx.Card> shown = new ArrayList<>();
    private String rerollText = "";
    private String rerollGrade = "";
    private boolean rerollFailed;
    /** 화면 설정 패널 (열려 있으면 그 위에 뜬다). */
    private MagicUiPanel settings;
    /** 창 크기 배율과, 그 배율로 본 화면 크기. */
    private float sc = 1F;
    private int vw, vh;
    private Component status;
    private boolean statusOk;
    private int statusTicks;
    private int fx0, fy0, W, H;
    private final List<Object[]> hits = new ArrayList<>(); // {x, y, w, h, Runnable, enabled, label, kind}

    public MagicGachaScreen(JsonObject data) {
        super(Component.translatable("rtshop.magic.title"));
        this.data = data;
    }

    // ------------------------------------------------------------------ 데이터

    public void update(JsonObject o, boolean result) {
        this.data = o;
        if (result) {
            List<MagicFx.Card> cards = cardsOf(o);
            if (o.has("reroll")) {
                JsonObject r = o.getAsJsonObject("reroll");
                rerollFailed = r.has("fail");
                rerollText = rerollFailed ? tr("rtshop.magic.reroll_failed") : r.get("name").getAsString();
                rerollGrade = r.get("grade").getAsString();
            }
            shown = cards;
            if (o.has("pityHit")) showResult(Component.translatable("rtshop.magic.pity_hit"), true);
            bestGrade = bestGradeOf(cards);
            tintOff = 0;
            fx.start(cards);
            if (minecraft != null && minecraft.level != null && o.has("x")) {
                MagicClient.FLARE.put(new net.minecraft.core.BlockPos(o.get("x").getAsInt(), o.get("y").getAsInt(), o.get("z").getAsInt()),
                        System.currentTimeMillis());
            }
        }
    }

    static ItemStack stackOf(JsonObject e) {
        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(e.has("item") ? e.get("item").getAsString() : ""));
        ItemStack s = new ItemStack(item == null ? Items.BOOK : item);
        if (e.has("nbt") && !e.get("nbt").getAsString().isEmpty()) {
            try {
                s.getOrCreateTag().merge(TagParser.parseTag(e.get("nbt").getAsString()));
            } catch (Exception ignored) {
            }
        }
        return s;
    }

    /** 스킬 이름: Epic Fight 번역 키 skill.모드.이름 이 있으면 그것, 없으면 아이템 이름. */
    static String skillName(JsonObject e, ItemStack s) {
        String sk = e.has("skill") ? e.get("skill").getAsString() : "";
        if (!sk.isEmpty() && sk.contains(":")) {
            String key = "skill." + sk.replace(':', '.');
            if (net.minecraft.client.resources.language.I18n.exists(key)) {
                return stripCodes(Component.translatable(key).getString());
            }
            return sk.substring(sk.indexOf(':') + 1).replace('_', ' ');
        }
        return e.has("name") ? e.get("name").getAsString() : s.getHoverName().getString();
    }

    /** 다른 모드 번역에 섞인 색 코드(§)와 장식 괄호를 걷어 낸다 (창 글꼴에서 깨지지 않게). */
    static String stripCodes(String s) {
        return s.replaceAll("§.", "").replace("『", "").replace("』", "").trim();
    }

    /** 결과 꾸러미(뽑은 스킬북들 또는 새 인챈트)를 카드 목록으로. */
    public static List<MagicFx.Card> cardsOf(JsonObject o) {
        List<MagicFx.Card> cards = new ArrayList<>();
        if (o.has("results")) {
            for (JsonElement e : o.getAsJsonArray("results")) {
                JsonObject r = e.getAsJsonObject();
                if (r.has("fail")) cards.add(new MagicFx.Card(ItemStack.EMPTY, tr("rtshop.magic.fail_book"), "fail")); // 실패: 빈 카드
                else cards.add(card(r));
            }
        }
        if (o.has("reroll")) {
            JsonObject r = o.getAsJsonObject("reroll");
            if (r.has("fail")) cards.add(new MagicFx.Card(ItemStack.EMPTY, tr("rtshop.magic.fail_enchant"), "fail"));
            else cards.add(new MagicFx.Card(new ItemStack(Items.ENCHANTED_BOOK), r.get("name").getAsString(), r.get("grade").getAsString()));
        }
        return cards;
    }

    public static String bestGradeOf(List<MagicFx.Card> cards) {
        int best = -1;
        for (MagicFx.Card c : cards) best = Math.max(best, java.util.Arrays.asList(GRADES).indexOf(c.grade));
        return best < 0 ? "" : GRADES[best];
    }

    private static MagicFx.Card card(JsonObject e) {
        ItemStack s = stackOf(e);
        return new MagicFx.Card(s, skillName(e, s), e.has("grade") ? e.get("grade").getAsString() : "common");
    }

    public int tickets(String k) {
        return data.has("tickets") ? data.getAsJsonObject("tickets").get(k).getAsInt() : 0;
    }

    public int cost(String k) {
        return data.has("costs") ? data.getAsJsonObject("costs").get(k).getAsInt() : 1;
    }

    private double[] chances() {
        JsonArray a;
        if (tab == 0) a = data.getAsJsonObject("cats").getAsJsonObject(CATS[cat]).getAsJsonArray("chances");
        else if (tab == 1) a = data.getAsJsonObject("random").getAsJsonArray("chances");
        else a = data.getAsJsonArray("enchantChances");
        double[] d = new double[GRADES.length];
        for (int i = 0; i < d.length && i < a.size(); i++) d[i] = a.get(i).getAsDouble();
        return d;
    }

    private static String tr(String k, Object... a) {
        return Component.translatable(k, a).getString();
    }

    public void showResult(Component msg, boolean ok) {
        status = msg;
        statusOk = ok;
        statusTicks = 160;
    }

    // ------------------------------------------------------------------ 점검용

    public boolean fxActiveForTest() {
        return fx.active();
    }

    public int shownForTest() {
        return shown.size();
    }

    public void tabForTest(int t) {
        tab = t;
    }

    public void catForTest(int c) {
        cat = c;
    }

    public void pullForTest() {
        pull();
    }

    public boolean settingsOpenForTest() {
        return settings != null;
    }

    public void settingsForTest(boolean open) {
        settings = open ? new MagicUiPanel(() -> settings = null) : null;
    }

    /** 가방 칸(0~35)의 물건을 제단 위로 올린다 (칸을 누른 것과 같다). */
    public void placeForTest(int inventoryIndex) {
        slotClick(MagicMenu.slotOfInventory(inventoryIndex), 0, true);
    }

    public ItemStack altarForTest() {
        return altarStack();
    }

    public void rerollForTest() {
        reroll();
    }

    public String rerollTextForTest() {
        return rerollText;
    }

    public JsonObject dataForTest() {
        return data;
    }

    // ------------------------------------------------------------------ 동작

    private void pull() {
        JsonObject a = new JsonObject();
        a.addProperty("mode", tab == 0 ? "select" : "random");
        a.addProperty("cat", CATS[cat]);
        a.addProperty("n", 1);
        ShopNet.CHANNEL.sendToServer(new MagicActionPacket(MagicActionPacket.PULL, a.toString()));
    }

    // ------------------------------------------------------------------ 제단 위 칸 · 가방

    /** 열려 있는 제단 메뉴 (없으면 null). */
    protected MagicMenu altarMenu() {
        return minecraft != null && minecraft.player != null && minecraft.player.containerMenu instanceof MagicMenu m ? m : null;
    }

    /** 제단 위에 올려 둔 장비. */
    public ItemStack altarStack() {
        MagicMenu m = altarMenu();
        return m == null ? ItemStack.EMPTY : m.getSlot(MagicMenu.ALTAR).getItem();
    }

    private ItemStack carried() {
        MagicMenu m = altarMenu();
        return m == null ? ItemStack.EMPTY : m.getCarried();
    }

    private void slotClick(int slot, int button, boolean shift) {
        MagicMenu m = altarMenu();
        if (m == null || minecraft.gameMode == null || slot < 0 || slot >= m.slots.size()) return;
        minecraft.gameMode.handleInventoryMouseClick(m.containerId, slot, button,
                shift ? net.minecraft.world.inventory.ClickType.QUICK_MOVE : net.minecraft.world.inventory.ClickType.PICKUP, minecraft.player);
    }

    /** 가방 칸 그림의 자리 {x, y} (재설정 탭 왼쪽 아래). */
    private int[] bagOrigin() {
        return new int[]{fx0 + 104 - 81, fy0 + H - 108};
    }

    /** 이 자리에 있는 메뉴 칸 번호 (제단 위 = 0, 가방 = 1~36, 없으면 -1). */
    private int slotAt(int mx, int my) {
        if (tab != 2 || altarMenu() == null) return -1;
        int[] a = altarSpot();
        if (Math.abs(mx - a[0]) <= 17 && Math.abs(my - a[1]) <= 17) return MagicMenu.ALTAR;
        int[] b = bagOrigin();
        int col = Math.floorDiv(mx - b[0], 18);
        if (col < 0 || col >= 9) return -1;
        int ry = my - b[1];
        if (ry >= 0 && ry < 54) return 1 + (ry / 18) * 9 + col;
        if (ry >= 58 && ry < 76) return 28 + col;
        return -1;
    }

    /** 재설정 탭에서 제단 위 칸의 가운데 {x, y}. */
    private int[] altarSpot() {
        int top = fy0 + 34, bottom = bagOrigin()[1] - 14;
        return new int[]{fx0 + 104, (top + bottom) / 2};
    }

    private void reroll() {
        ShopNet.CHANNEL.sendToServer(new MagicActionPacket(MagicActionPacket.REROLL, ""));
    }

    /** 창 크기 배율(화면 설정)에 맞춰 자리를 잡는다. */
    private void layout() {
        sc = MagicUi.scale();
        vw = Math.round(width / sc);
        vh = Math.round(height / sc);
        W = Math.min(vw - 8, 430);
        H = Math.min(vh - 8, 252);
        fx0 = (vw - W) / 2;
        fy0 = (vh - H) / 2;
        F = MagicUi.body();
        FT = MagicUi.head();
        FTITLE = MagicUi.title();
    }

    @Override
    protected void init() {
        layout();
        ShopNet.CHANNEL.sendToServer(new MagicActionPacket(MagicActionPacket.REFRESH, ""));
    }

    @Override
    public void tick() {
        if (statusTicks > 0 && --statusTicks == 0) status = null;
    }

    // ------------------------------------------------------------------ 그리기 (룬 제단)

    private static ResourceLocation tex(String n) {
        return new ResourceLocation("rtshop", "textures/gui/magic/" + n + ".png");
    }

    private static final ResourceLocation SLAB = tex("slab"), RING_OUT = tex("ring_outer"), RING_IN = tex("ring_inner"),
            TICKS = tex("ring_ticks"), STAR = tex("star"), ORB = tex("orb"), GLOW = tex("glow"),
            STONES = tex("stones"), STONES_ON = tex("stones_on"), STONES_SEL = tex("stones_sel");
    /** 글꼴 (화면 설정에 따라 layout() 이 고른다): 본문 · 머리말 · 제목. */
    private ResourceLocation F = MagicUi.body(), FT = MagicUi.head(), FTITLE = MagicUi.title();
    static final int C_RUNE = 0xFF8FE4FF;
    static final int C_TEXT = 0xFFEDE4FF, C_DIM = 0xFF9C8CC8, C_GOLD = 0xFFFFE9B0, C_EDGE = 0xFFD8B060, C_FAIL = 0xFF9A90B0;
    private final long born = System.currentTimeMillis();
    private String bestGrade = "";
    private long tintOff = 0;

    private void txt(GuiGraphics g, String s, int x, int y, int color) {
        MagicUi.draw(g, font, s, F, x, y, color);
    }

    private void txt(GuiGraphics g, String s, ResourceLocation f, int x, int y, int color) {
        MagicUi.draw(g, font, s, f, x, y, color);
    }

    private int tw(String s) {
        return RaidText.w(font, s, F);
    }

    private String clipF(String s, int w) {
        return RaidText.clip(font, s, F, Math.max(8, w));
    }

    private static int mix(int a, int b, float k) {
        if (k <= 0) return a & 0xFFFFFF;
        int r = Math.round((a >> 16 & 255) + ((b >> 16 & 255) - (a >> 16 & 255)) * k);
        int gg = Math.round((a >> 8 & 255) + ((b >> 8 & 255) - (a >> 8 & 255)) * k);
        int bl = Math.round((a & 255) + ((b & 255) - (a & 255)) * k);
        return r << 16 | gg << 8 | bl;
    }

    private boolean pityOn() {
        return data.has("pity") && data.getAsJsonObject("pity").get("on").getAsBoolean();
    }

    /** 천장 게이지: 소환진 바깥 눈금 48개가 횟수만큼 금빛으로 찬다. */
    private static void gauge(GuiGraphics g, int cx, int cy, int r, float frac, int cOn, int cOff) {
        g.flush();
        int lit = frac <= 0 ? 0 : Math.max(1, Math.round(frac * 48));
        for (int i = 0; i < 48; i++) {
            g.pose().pushPose();
            g.pose().translate(cx + 0.5F, cy + 0.5F, 0);
            g.pose().mulPose(Axis.ZP.rotationDegrees(i * 7.5F));
            int len = i % 4 == 0 ? 5 : 3;
            g.fill(0, -(r + len), 1, -r, i < lit ? cOn : cOff);
            g.pose().popPose();
        }
    }

    /** 방금 뽑은 최고 등급의 빛이 제단에 남는 세기 (0~1). */
    private float tint() {
        if (bestGrade.isEmpty() || fx.active()) return 0; // 연출이 끝나기 전에는 등급 색을 비추지 않는다
        if (!shown.isEmpty()) return 1;
        float k = 1 - (System.currentTimeMillis() - tintOff) / 4000F;
        if (k <= 0) {
            bestGrade = "";
            return 0;
        }
        return k;
    }

    /** 가운데를 축으로 돌려 그린다 (ts = 질감 크기, r = 화면에서의 반지름). add = 빛 더하기. */
    private static void spin(GuiGraphics g, ResourceLocation t, int ts, float cx, float cy, float r, float deg, int rgb, float alpha, boolean add) {
        g.flush();
        RenderSystem.enableBlend();
        if (add) RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        if (deg != 0) g.pose().mulPose(Axis.ZP.rotationDegrees(deg));
        float s = r / (ts / 2F);
        g.pose().scale(s, s, 1);
        g.setColor((rgb >> 16 & 255) / 255F, (rgb >> 8 & 255) / 255F, (rgb & 255) / 255F, Math.max(0, Math.min(1, alpha)));
        g.blit(t, -ts / 2, -ts / 2, 0F, 0F, ts, ts, ts, ts);
        g.setColor(1, 1, 1, 1);
        g.pose().popPose();
        if (add) RenderSystem.defaultBlendFunc();
    }

    private static void stone(GuiGraphics g, ResourceLocation sheet, int i, float cx, float cy, float scale, float bright, float alpha) {
        g.flush();
        RenderSystem.enableBlend();
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().scale(scale, scale, 1);
        g.setColor(bright, bright, bright, alpha);
        g.blit(sheet, -32, -40, i * 64F, 0F, 64, 80, 320, 80);
        g.setColor(1, 1, 1, 1);
        g.pose().popPose();
    }

    static void box(GuiGraphics g, int x, int y, int w, int h, int c) {
        g.fill(x, y, x + w, y + 1, c);
        g.fill(x, y + h - 1, x + w, y + h, c);
        g.fill(x, y + 1, x + 1, y + h - 1, c);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, c);
    }

    static void gem(GuiGraphics g, int cx, int cy, int r, int c) {
        for (int dy = -r; dy <= r; dy++) {
            int w = r - Math.abs(dy);
            g.fill(cx - w, cy + dy, cx + w + 1, cy + dy + 1, c);
        }
        g.fill(cx, cy - r + 1, cx + 1, cy - r + 2, 0xC0FFFFFF);
    }

    /** 흑요석 판: 어두운 돌결 · 보랏빛 겹테 · 금빛 속테 · 모서리 보석. */
    static void slab(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x - 3, y - 3, x + w + 3, y + h + 3, 0xFF07050C);
        g.setColor(1, 1, 1, 1);
        Ui.tile(g, SLAB, x, y, w, h, 64);
        g.fillGradient(x, y, x + w, y + 30, 0xB0180E2C, 0x00180E2C);
        g.fillGradient(x, y + h - 34, x + w, y + h, 0x00000000, 0xC0000000);
        box(g, x - 2, y - 2, w + 4, h + 4, 0xFF6A4AB8);
        box(g, x - 1, y - 1, w + 2, h + 2, 0xFF2A1C4C);
        box(g, x + 2, y + 2, w - 4, h - 4, 0x90D8B060);
        int[][] cs = {{x, y}, {x + w - 1, y}, {x, y + h - 1}, {x + w - 1, y + h - 1}};
        for (int[] c : cs) {
            gem(g, c[0], c[1], 4, 0xFF140E24);
            gem(g, c[0], c[1], 3, C_EDGE);
            gem(g, c[0], c[1], 1, 0xFFB080FF);
        }
    }

    // ------------------------------------------------------------------ Cinematic 장면 옆 패널

    /** 창을 띄우지 않고 그리기 도구로만 쓸 때: 글꼴·크기를 맞춘다. */
    public void prepare(net.minecraft.client.Minecraft mc, int w, int h) {
        this.minecraft = mc;
        this.font = mc.font;
        this.width = w;
        this.height = h;
        F = MagicUi.body();
        FT = MagicUi.head();
        FTITLE = MagicUi.title();
    }

    public void setData(JsonObject o) {
        this.data = o;
    }

    public JsonObject data() {
        return data;
    }

    public static final int HUD_H = 150;

    /** 장면 옆에 겹쳐 보이는 룬 제단 패널: 제목 · 티켓 수 · 등급 확률 · 천장. */
    public void renderHud(GuiGraphics g, int x, int y, int w) {
        if (!data.has("cats")) return;
        slab(g, x, y, w, HUD_H);
        String head = tr("rtshop.magic.altar");
        txt(g, head, FT, x + 8, y + 6, C_GOLD);
        int tx = x + 8;
        String[][] tk = {{"select", "rtshop:magic_ticket_select"}, {"random", "rtshop:magic_ticket"}, {"reroll", "rtshop:enchant_scroll"}};
        for (String[] k : tk) {
            Item it = ForgeRegistries.ITEMS.getValue(new ResourceLocation(k[1]));
            if (it != null) g.renderItem(new ItemStack(it), tx, y + 19);
            String n = String.valueOf(tickets(k[0]));
            txt(g, n, tx + 18, y + 24, C_TEXT);
            tx += 18 + tw(n) + 10;
        }
        g.fill(x + 6, y + 38, x + w - 6, y + 39, 0xFF3A2A66);
        int rx = x + 8, rw = w - 16, py = y + 44, limit = y + HUD_H - 4;
        if (tab == 2) {
            ItemStack s = altarStack();
            boolean ok = data.has("heldOk") && data.get("heldOk").getAsBoolean();
            txt(g, clipF(s.isEmpty() ? tr("rtshop.magic.reroll_hold") : stripCodes(s.getHoverName().getString()), rw), rx, py, ok ? C_GOLD : s.isEmpty() ? C_DIM : 0xFFFF9080);
            txt(g, clipF(tr("rtshop.magic.odds_of", tr("rtshop.magic.tab.reroll")), rw), rx, py + 13, C_TEXT);
            g.fill(rx, py + 24, rx + rw, py + 25, 0xFF3A2A66);
            odds(g, rx, py + 28, rw);
        } else {
            infoLines(g, rx, py, rw, limit);
        }
    }

    /** 장면 패널 아래의 작은 돌 단추 (누름 처리는 장면이 한다). */
    public void hudButton(GuiGraphics g, int x, int y, int w, int h, String label, boolean hov) {
        g.fill(x, y, x + w, y + h, hov ? 0xFF2E2250 : 0xE0161028);
        box(g, x, y, w, h, hov ? C_EDGE : 0xFF5A4690);
        String s = clipF(label, w - 6);
        txt(g, s, x + (w - tw(s)) / 2, y + (h - 9) / 2 + 2, hov ? C_GOLD : C_TEXT);
    }

    /** 장면 패널 아래 한 줄 알림. */
    public void hudLine(GuiGraphics g, int x, int y, int w, String s, int color) {
        String c = clipF(s, w - 8);
        g.fill(x, y - 3, x + w, y + 11, 0xD00A0612);
        txt(g, c, x + (w - tw(c)) / 2, y, color);
    }

    /** 수정 단추 (양 끝이 뾰족한 육각): 윗줄 큰 글자, 아랫줄 값. */
    private void crystal(GuiGraphics g, int mx, int my, int x, int y, int w, int h, String top, String sub, boolean enabled, Runnable r) {
        boolean hov = enabled && mx >= x && mx < x + w && my >= y && my < y + h;
        int edge = enabled ? (hov ? 0xFFFFF0C0 : C_EDGE) : 0xFF4A4060;
        int c1 = enabled ? (hov ? 0x8A60E0 : 0x6642B8) : 0x2C2638, c2 = enabled ? (hov ? 0x3C2284 : 0x2A1860) : 0x1A1626;
        if (hov) {
            for (int i = -2; i < h + 2; i++) {
                int in = Math.round(Math.abs(i - (h - 1) / 2F) * 0.55F);
                g.fill(x + in - 2, y + i, x + w - in + 2, y + i + 1, 0x30FFD890);
            }
        }
        for (int i = 0; i < h; i++) {
            int in = Math.round(Math.abs(i - (h - 1) / 2F) * 0.55F);
            g.fill(x + in, y + i, x + w - in, y + i + 1, edge);
            if (i > 0 && i < h - 1) {
                g.fill(x + in + 1, y + i, x + w - in - 1, y + i + 1, 0xFF000000 | mix(c1, c2, i / (float) h));
            }
        }
        int in1 = Math.round(Math.abs(1 - (h - 1) / 2F) * 0.55F);
        g.fill(x + in1 + 3, y + 2, x + w - in1 - 3, y + 3, enabled ? 0x50FFFFFF : 0x18FFFFFF);
        String a = MagicUi.clip(font, top, FT, w - 18);
        txt(g, a, FT, x + (w - MagicUi.w(font, a, FT)) / 2, y + 5, enabled ? C_GOLD : 0xFF7A7090);
        String b = clipF(sub, w - 18);
        txt(g, b, x + (w - tw(b)) / 2, y + h - 10, enabled ? 0xFFD8CCF8 : 0xFF6A6080);
        hits.add(new Object[]{x, y, w, h, r, enabled});
    }

    /** 아래쪽 작은 돌 단추. */
    private void slate(GuiGraphics g, int mx, int my, int x, int y, int w, int h, String label, boolean enabled, Runnable r) {
        boolean hov = enabled && mx >= x && mx < x + w && my >= y && my < y + h;
        g.fill(x, y, x + w, y + h, hov ? 0xFF2E2250 : 0xFF161028);
        box(g, x, y, w, h, hov ? C_EDGE : 0xFF5A4690);
        String s = clipF(label, w - 6);
        txt(g, s, x + (w - tw(s)) / 2, y + (h - 9) / 2 + 2, !enabled ? 0xFF6A6080 : hov ? C_GOLD : C_TEXT);
        hits.add(new Object[]{x, y, w, h, r, enabled});
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        layout();
        int vmx = Math.round(mx / sc), vmy = Math.round(my / sc);
        g.pose().pushPose();
        g.pose().scale(sc, sc, 1F);
        draw(g, vmx, vmy);
        g.pose().popPose();
        super.render(g, mx, my, partial);
        fx.render(g, font, width, height);
    }

    /** 창 전체 (크기 배율이 걸린 좌표로 그린다). */
    private void draw(GuiGraphics g, int mx, int my) {
        hits.clear();
        boolean fxOn = fx.active();
        boolean busy = fxOn || settings != null;
        float t = (System.currentTimeMillis() - born) / 1000F;
        int x0 = fx0, y0 = fy0;
        slab(g, x0, y0, W, H);
        String head = tr("rtshop.magic.altar");
        txt(g, head, FTITLE, x0 + 14, y0 + 8, C_GOLD);
        if (!data.has("cats")) return; // 아직 서버 답이 오지 않음

        // ---- 머리: 제목 · 티켓
        int tx = x0 + W - 12;
        String[][] tk = {{"reroll", "rtshop:enchant_scroll"}, {"random", "rtshop:magic_ticket"}, {"select", "rtshop:magic_ticket_select"}};
        for (String[] k : tk) {
            String n = String.valueOf(tickets(k[0]));
            tx -= tw(n);
            txt(g, n, tx, y0 + 11, C_TEXT);
            tx -= 18;
            Item it = ForgeRegistries.ITEMS.getValue(new ResourceLocation(k[1]));
            if (it != null) g.renderItem(new ItemStack(it), tx, y0 + 6);
            tx -= 8;
        }
        g.fill(x0 + 10, y0 + 28, x0 + W - 10, y0 + 29, 0xFF3A2A66);
        g.fill(x0 + 10, y0 + 29, x0 + W - 10, y0 + 30, 0x60000000);
        gem(g, x0 + 10, y0 + 28, 2, C_EDGE);
        gem(g, x0 + W - 11, y0 + 28, 2, C_EDGE);

        // ---- 왼쪽: 소환진 (뽑기) 또는 제단 위 칸과 가방 (재설정)
        if (tab == 2) altarAndBag(g, mx, my, t, busy);
        else circle(g, mx, my, t, busy);

        // ---- 오른쪽: 탭 · 확률 · 수정 단추
        int rx = x0 + 212, rw = W - 212 - 14;
        String[] tabs = {tr("rtshop.magic.tab2.select"), tr("rtshop.magic.tab2.random"), tr("rtshop.magic.tab2.reroll")};
        int tbw = rw / 3;
        for (int i = 0; i < 3; i++) {
            final int ti = i;
            int bx = rx + i * tbw;
            boolean sel = tab == i;
            boolean hov = !busy && mx >= bx && mx < bx + tbw - 3 && my >= y0 + 34 && my < y0 + 49;
            g.fill(bx, y0 + 34, bx + tbw - 3, y0 + 49, sel ? 0xFF34225E : hov ? 0xFF221838 : 0xFF151024);
            if (sel) g.fill(bx, y0 + 34, bx + tbw - 3, y0 + 35, 0xFF8A6AD8);
            g.fill(bx, y0 + 48, bx + tbw - 3, y0 + 49, sel ? 0xFFE8C070 : 0xFF4A3A7A);
            String s = clipF(tabs[i], tbw - 8);
            txt(g, s, bx + (tbw - 3 - tw(s)) / 2, y0 + 38, sel ? C_GOLD : 0xFFA898CC);
            hits.add(new Object[]{bx, y0 + 34, tbw - 3, 15, (Runnable) () -> tab = ti, true});
        }
        int py = y0 + 56;
        int by2 = y0 + H - 60;
        if (tab == 2) {
            rerollPanel(g, mx, my, rx, py, rw, by2, busy);
        } else {
            String mode = tab == 0 ? "select" : "random";
            infoLines(g, rx, py, rw, by2);
            int have = tickets(mode);
            JsonObject src = tab == 0 ? data.getAsJsonObject("cats").getAsJsonObject(CATS[cat]) : data.getAsJsonObject("random");
            int c1 = src.has("cost1") ? src.get("cost1").getAsInt() : cost(mode);
            crystal(g, mx, my, rx, by2, rw, 28, tr("rtshop.magic.times"), tr("rtshop.magic.cost." + mode, c1),
                    !busy && have >= c1 && src.get("count").getAsInt() > 0, this::pull);
        }

        // ---- 아래 단추
        int by = y0 + H - 24;
        slate(g, mx, my, x0 + 12, by, 72, 16, tr("rtshop.magic.ui_settings"), !busy, () -> settings = new MagicUiPanel(() -> settings = null));
        if (data.has("canEdit") && data.get("canEdit").getAsBoolean()) {
            slate(g, mx, my, x0 + W - 130, by, 54, 16, tr("rtshop.magic.edit"), !busy,
                    () -> ShopNet.CHANNEL.sendToServer(new MagicActionPacket(MagicActionPacket.EDIT, "")));
        }
        slate(g, mx, my, x0 + W - 72, by, 60, 16, tr("rtshop.enhodds.close"), !busy, this::onClose);
        if (status != null) {
            String s = clipF(status.getString(), W - 92 - 136);
            txt(g, s, x0 + 90, by + 4, statusOk ? 0xFF9FE88A : 0xFFFF9080);
        } else if (data.has("error")) {
            txt(g, clipF(tr("rtshop.gacha.broken"), W - 92 - 136), x0 + 90, by + 4, 0xFFFF9080);
        }
        // 결과 (연출이 끝난 뒤)
        if (!fxOn && !shown.isEmpty()) drawResults(g, shown, vw, vh);
        // 들고 있는 물건 · 칸 설명
        ItemStack carried = carried();
        if (!carried.isEmpty()) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 350);
            g.renderItem(carried, mx - 8, my - 8);
            g.renderItemDecorations(font, carried, mx - 8, my - 8);
            g.pose().popPose();
        } else if (!busy && shown.isEmpty()) {
            int sl = slotAt(mx, my);
            MagicMenu m = altarMenu();
            if (sl >= 0 && m != null && m.getSlot(sl).hasItem()) g.renderTooltip(font, m.getSlot(sl).getItem(), mx, my);
        }
        if (settings != null) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 420);
            g.fill(0, 0, vw, vh, 0x90000000);
            settings.render(g, font, mx, my, vw, vh);
            g.pose().popPose();
        }
    }

    /** 소환진 · 구슬 · 룬석 다섯 (분야 단추). */
    private void circle(GuiGraphics g, int mx, int my, float t, boolean busy) {
        int x0 = fx0, y0 = fy0;
        int R = Math.max(40, Math.min(66, (H - 62) / 2 - 22));
        int cx = x0 + 104, cy = y0 + 30 + (H - 60) / 2;
        float k = tint();
        int gc = bestGrade.isEmpty() ? 0 : GachaColors.of(bestGrade);
        int cOut = mix(0xB080FF, gc, k), cIn = mix(0xECC478, gc, k), cGlow = mix(tab == 1 ? 0xC8A0FF : 0x9060F0, gc, k);
        float sp = tab == 1 ? 24 : 9;
        float pulse = (float) Math.sin(t * 2.2F);
        spin(g, GLOW, 128, cx, cy, R * 1.55F, 0, cGlow, 0.30F + 0.06F * pulse + 0.2F * k, true);
        spin(g, STAR, 256, cx, cy, R, 0, cOut, 0.50F + 0.2F * k, false);
        if (pityOn()) {
            JsonObject pi = data.getAsJsonObject("pity");
            float frac = Math.min(1F, pi.get(tab == 0 ? "select" : "random").getAsInt() / (float) Math.max(1, pi.get("count").getAsInt()));
            gauge(g, cx, cy, R + 5, frac, 0xFF000000 | mix(0xFFD070, gc, k), 0x80000000 | cOut);
        } else {
            spin(g, TICKS, 256, cx, cy, R + 7, 0, cOut, 0.55F, false);
        }
        spin(g, RING_OUT, 256, cx, cy, R, t * sp, cOut, 1F, false);
        spin(g, RING_IN, 256, cx, cy, R * 0.62F, -t * sp * 1.5F, cIn, 0.9F, false);
        motes(g, cx, cy, R, t, gc, k);
        float bob = (float) Math.sin(t * 1.6F) * 1.5F;
        spin(g, GLOW, 128, cx, cy + bob, 40 + 3 * pulse, 0, cGlow, 0.55F, true);
        spin(g, ORB, 128, cx, cy + bob, 21, 0, mix(0xFFFFFF, gc, k * 0.55F), 1F, false);
        spin(g, GLOW, 128, cx - 5, cy + bob - 6, 12, 0, 0xFFFFFF, 0.25F + 0.1F * pulse, true);
        int flashAt = tab == 1 ? (int) (t * 5) % 5 : -1;
        for (int i = 0; i < CATS.length; i++) {
            final int ci = i;
            double a = Math.toRadians(-90 + 72 * i);
            int sx = cx + (int) Math.round(Math.cos(a) * R), sy = cy + (int) Math.round(Math.sin(a) * R);
            boolean chosen = tab == 0 && cat == i;
            boolean lit = chosen || tab == 1;
            boolean hov = !busy && Math.abs(mx - sx) <= 14 && Math.abs(my - sy) <= 19;
            // 고르지 않은 룬석도 어두워지지 않는다: 고른 것은 조금 커지고, 금빛 테가 푸른빛으로 바뀌고 푸른 고리가 돈다
            float scl = (chosen ? 0.56F + 0.012F * (float) Math.sin(t * 4) : 0.48F) + (hov ? 0.04F : 0) + (flashAt == i ? 0.04F : 0);
            if (lit) spin(g, GLOW, 128, sx, sy, 27, 0, chosen ? 0x70C0FF : mix(0xFFD890, gc, k), chosen ? 0.45F : flashAt == i ? 0.6F : 0.22F, true);
            if (chosen) spin(g, RING_IN, 256, sx, sy, 23, t * 14, 0x9FD8FF, 0.9F, false);
            stone(g, chosen ? STONES_SEL : STONES_ON, i, sx, sy, scl, lit || hov ? 1F : 0.82F, 1F);
            String nm = tr("rtshop.magic.cat." + CATS[i]);
            g.fill(sx - tw(nm) / 2 - 4, sy + 22, sx + tw(nm) / 2 + 4, sy + 34, 0x900A0612);
            txt(g, nm, sx - tw(nm) / 2, sy + 24, chosen ? 0xFFBFE6FF : lit ? C_GOLD : 0xFFCFC2F0);
            if (chosen) g.fill(sx - tw(nm) / 2, sy + 34, sx + tw(nm) / 2, sy + 35, 0xFF8FD0FF);
            hits.add(new Object[]{sx - 14, sy - 19, 28, 50, (Runnable) () -> {
                tab = 0;
                cat = ci;
            }, true});
        }
    }

    /** 떠오르는 마력 알갱이. */
    private static void motes(GuiGraphics g, int cx, int cy, int R, float t, int gc, float k) {
        for (int i = 0; i < 18; i++) {
            float ph = (t * (0.10F + (i % 5) * 0.025F) + i * 0.173F) % 1F;
            float px = cx + (float) Math.sin(i * 7.31F) * R * 1.05F + (float) Math.sin(t * 1.3F + i) * 3F;
            float py = cy + R - ph * R * 2.1F;
            int al = (int) (Math.sin(ph * Math.PI) * 190);
            if (al <= 8) continue;
            int col = (al << 24) | (i % 3 == 0 ? mix(0xFFE0A0, gc, k) : mix(0xC0A0FF, gc, k));
            int sz = i % 4 == 0 ? 2 : 1;
            g.fill((int) px, (int) py, (int) px + sz, (int) py + sz, col);
        }
    }

    private void cell(GuiGraphics g, int x, int y, ItemStack s, boolean hov) {
        g.fill(x, y, x + 18, y + 18, 0xFF2A1F48);
        g.fill(x + 1, y + 1, x + 17, y + 17, hov ? 0xFF2E2250 : 0xFF120C22);
        if (!s.isEmpty()) {
            g.renderItem(s, x + 1, y + 1);
            g.renderItemDecorations(font, s, x + 1, y + 1);
        }
    }

    /** 재설정 탭의 왼쪽: 작은 소환진 가운데의 '제단 위' 칸과, 그 아래 가방 칸. 칸을 눌러 집고 놓는다 (Shift 클릭 = 바로 올리기·내리기). */
    private void altarAndBag(GuiGraphics g, int mx, int my, float t, boolean busy) {
        MagicMenu m = altarMenu();
        int[] a = altarSpot();
        int[] b = bagOrigin();
        int cx = a[0], cy = a[1];
        int R = Math.max(22, Math.min(36, (b[1] - 14 - (fy0 + 34)) / 2 - 9));
        float k = tint();
        int gc = bestGrade.isEmpty() ? 0 : GachaColors.of(bestGrade);
        int cOut = mix(0x80B0FF, gc, k), cIn = mix(0xECC478, gc, k);
        float pulse = (float) Math.sin(t * 2.2F);
        ItemStack on = altarStack();
        int hovSlot = busy ? -1 : slotAt(mx, my);
        spin(g, GLOW, 128, cx, cy, R * 1.6F, 0, mix(0x6090F0, gc, k), 0.30F + 0.06F * pulse + (on.isEmpty() ? 0 : 0.12F), true);
        spin(g, TICKS, 256, cx, cy, R + 7, 0, cOut, 0.55F, false);
        spin(g, RING_OUT, 256, cx, cy, R, t * 5, cOut, 1F, false);
        spin(g, RING_IN, 256, cx, cy, R * 0.62F, -t * 7.5F, cIn, 0.9F, false);
        motes(g, cx, cy, R, t, gc, k);
        // 제단 위 칸
        boolean hovA = hovSlot == MagicMenu.ALTAR;
        g.fill(cx - 15, cy - 15, cx + 15, cy + 15, hovA ? 0xE02E2250 : 0xC0120C22);
        box(g, cx - 15, cy - 15, 30, 30, hovA ? C_EDGE : 0xFF6A4AB8);
        if (!on.isEmpty()) {
            g.flush();
            g.pose().pushPose();
            g.pose().translate(cx, cy, 50);
            g.pose().scale(1.5F, 1.5F, 1);
            g.renderItem(on, -8, -8);
            g.pose().popPose();
        } else {
            gem(g, cx, cy, 4, 0xFF4A3A7A);
        }
        String lab = tr("rtshop.magic.altar_slot");
        txt(g, lab, cx - tw(lab) / 2, cy + R + 10, on.isEmpty() ? C_DIM : C_GOLD);
        // 가방
        txt(g, tr("rtshop.magic.bag"), b[0], b[1] - 11, C_DIM);
        if (m == null) return;
        for (int i = 0; i < 27; i++) {
            int x = b[0] + (i % 9) * 18, y = b[1] + (i / 9) * 18;
            cell(g, x, y, m.getSlot(1 + i).getItem(), hovSlot == 1 + i);
        }
        for (int i = 0; i < 9; i++) cell(g, b[0] + i * 18, b[1] + 58, m.getSlot(28 + i).getItem(), hovSlot == 28 + i);
    }

    /** 확률 머리말 · 등급 확률(첫 줄 = 실패) · 천장 · 도움말 (limit 아래로는 그리지 않는다). */
    private void infoLines(GuiGraphics g, int rx, int py, int rw, int by2) {
        String mode = tab == 0 ? "select" : "random";
        String name = tab == 0 ? tr("rtshop.magic.cat." + CATS[cat]) : tr("rtshop.magic.all_fields");
        int count = tab == 0 ? data.getAsJsonObject("cats").getAsJsonObject(CATS[cat]).get("count").getAsInt()
                : data.getAsJsonObject("random").get("count").getAsInt();
        String kinds = tr("rtshop.magic.kinds", count);
        txt(g, clipF(tr("rtshop.magic.odds_of", name), rw - tw(kinds) - 6), rx, py, C_TEXT);
        txt(g, kinds, rx + rw - tw(kinds), py, C_DIM);
        g.fill(rx, py + 11, rx + rw, py + 12, 0xFF3A2A66);
        int oy = odds(g, rx, py + 15, rw);
        if (pityOn() && oy + 11 < by2) {
            JsonObject pi = data.getAsJsonObject("pity");
            String pg = pi.get("grade").getAsString();
            gem(g, rx + 3, oy + 6, 3, GachaColors.argb(pg));
            txt(g, clipF(tr("rtshop.magic.pity", pi.get(mode).getAsInt(), pi.get("count").getAsInt(), tr("rtshop.gacha.grade." + pg)), rw - 10),
                    rx + 10, oy + 2, C_GOLD);
            oy += 11;
        }
        if (oy + 11 < by2) {
            txt(g, clipF(tr("rtshop.magic.fail_note"), rw), rx, oy + 2, C_DIM);
            oy += 11;
        }
        if (oy + 11 < by2) txt(g, clipF(tr(tab == 0 ? "rtshop.magic.select_hint" : "rtshop.magic.random_short"), rw), rx, oy + 2, C_DIM);
    }

    /** 등급 확률 줄 (첫 줄은 실패 확률). 마지막 줄 아래 y 를 돌려준다. */
    private int odds(GuiGraphics g, int x, int y, int w) {
        double[] d = chances();
        int row = 0;
        for (int i = 0; i < GRADES.length; i++) {
            if (d[i] <= 0) continue;
            int yy = y + row * 12;
            int col = i == 0 ? C_FAIL : GachaColors.window(GRADES[i], false);
            gem(g, x + 3, yy + 5, 3, col);
            txt(g, clipF(i == 0 ? tr("rtshop.magic.fail") : GachaColors.windowName(GRADES[i]), 46), x + 10, yy + 2, col);
            String p = GachaScreen.pct(d[i]);
            int bx = x + 58, bw = Math.max(10, w - 58 - 44);
            g.fill(bx - 1, yy + 2, bx + bw + 1, yy + 8, 0xFF2A1F48);
            g.fill(bx, yy + 3, bx + bw, yy + 7, 0xFF0E0A1A);
            int fw = Math.max(1, (int) (bw * Math.min(1, d[i])));
            g.fill(bx, yy + 3, bx + fw, yy + 7, col);
            g.fill(bx, yy + 3, bx + fw, yy + 4, 0x60FFFFFF);
            txt(g, p, x + w - tw(p), yy + 2, C_TEXT);
            row++;
        }
        if (row == 0 || (row == 1 && d[0] > 0)) {
            int yy = y;
            for (var line : MagicUi.split(font, tr("rtshop.magic.empty"), F, w)) {
                MagicUi.draw(g, font, line, x, yy, 0xFFFF9080);
                yy += 10;
            }
            return yy;
        }
        return y + row * 12;
    }

    private void rerollPanel(GuiGraphics g, int mx, int my, int x, int y, int w, int by2, boolean busy) {
        boolean ok = data.has("heldOk") && data.get("heldOk").getAsBoolean();
        ItemStack s = altarStack();
        if (s.isEmpty()) {
            int yy = y;
            for (var line : MagicUi.split(font, tr("rtshop.magic.reroll_hold"), F, w)) {
                if (yy > y + 11) break;
                MagicUi.draw(g, font, line, x, yy, C_DIM);
                yy += 11;
            }
        } else {
            txt(g, clipF(stripCodes(s.getHoverName().getString()), w), x, y, C_GOLD);
            var ench = net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(s);
            String now = ench.isEmpty() ? tr("rtshop.magic.no_enchant")
                    : String.join(", ", ench.entrySet().stream().map(en -> en.getKey().getFullname(en.getValue()).getString()).toList());
            if (ok) txt(g, clipF(tr("rtshop.magic.current", now), w), x, y + 11, 0xFFC8B8F0);
            else txt(g, clipF(tr("rtshop.magic.reroll_bad_item"), w), x, y + 11, 0xFFFF9080);
        }
        if (!rerollText.isEmpty()) {
            txt(g, clipF(rerollFailed ? rerollText : tr("rtshop.magic.reroll_got", rerollText), w), x, y + 22, rerollFailed ? C_FAIL : GachaColors.argb(rerollGrade));
        }
        int hy = y + 36;
        txt(g, clipF(tr("rtshop.magic.odds_of", tr("rtshop.magic.tab.reroll")), w), x, hy, C_TEXT);
        g.fill(x, hy + 11, x + w, hy + 12, 0xFF3A2A66);
        odds(g, x, hy + 15, w);
        int c = cost("reroll");
        crystal(g, mx, my, x, by2, w, 28, tr("rtshop.magic.reroll_btn"), tr("rtshop.magic.cost.reroll", c),
                !busy && ok && !s.isEmpty() && tickets("reroll") >= c, this::reroll);
    }

    /** 뽑은 것 (화면 한가운데). Cinematic 장면에서도 쓴다. w·h = 그리는 화면의 크기. */
    public void drawResults(GuiGraphics g, List<MagicFx.Card> shown, int sw, int sh) {
        int n = shown.size();
        if (n == 0) return; // 보여 줄 카드가 없으면 그리지 않는다 (0 으로 나누지 않게)
        int cols = n > 5 ? 5 : n;
        int rows = (n + cols - 1) / cols;
        int cw = 96, ch = 30;
        int w = Math.max(cols * (cw + 4) + 8, 130), h = rows * (ch + 4) + 34;
        int x = (sw - w) / 2, y = (sh - h) / 2;
        g.flush();
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fill(0, 0, sw, sh, 0xA0000000);
        slab(g, x - 6, y - 6, w + 12, h + 12);
        txt(g, tr("rtshop.magic.results"), FT, x + 4, y + 2, C_GOLD);
        g.fill(x + 2, y + 15, x + w - 2, y + 16, 0xFF3A2A66);
        int gx = x + (w - (cols * (cw + 4) - 4)) / 2;
        for (int i = 0; i < n; i++) {
            MagicFx.Card c = shown.get(i);
            int col = GachaColors.argb(c.grade);
            int cx = gx + (i % cols) * (cw + 4), cy = y + 20 + (i / cols) * (ch + 4);
            g.fill(cx, cy, cx + cw, cy + ch, 0xF0120C22);
            g.fillGradient(cx, cy, cx + cw, cy + ch, (col & 0xFFFFFF) | 0x50000000, 0x00000000);
            box(g, cx, cy, cw, ch, (col & 0xFFFFFF) | 0xB0000000);
            g.fill(cx, cy, cx + cw, cy + 2, col);
            if (c.icon.isEmpty()) gem(g, cx + 12, cy + 16, 5, 0xFF4A4060);
            else g.renderItem(c.icon, cx + 4, cy + 8);
            txt(g, clipF(c.name, cw - 26), cx + 23, cy + 6, C_TEXT);
            txt(g, clipF(tr("rtshop.gacha.grade." + c.grade), cw - 26), cx + 23, cy + 17, col);
        }
        String close = tr("rtshop.magic.click_close");
        txt(g, close, x + w - 4 - tw(close), y + h - 10, C_DIM);
        g.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double rmx, double rmy, int button) {
        if (fx.active()) {
            fx.skip();
            return true;
        }
        if (!shown.isEmpty()) {
            shown = new ArrayList<>();
            tintOff = System.currentTimeMillis();
            return true;
        }
        int mx = Math.round((float) rmx / sc), my = Math.round((float) rmy / sc);
        if (settings != null) {
            settings.click(mx, my);
            return true;
        }
        int sl = slotAt(mx, my);
        if (sl >= 0 && (button == 0 || button == 1)) {
            slotClick(sl, button, hasShiftDown());
            return true;
        }
        for (Object[] h : hits) {
            int x = (int) h[0], y = (int) h[1], w = (int) h[2], hh = (int) h[3];
            if ((boolean) h[5] && mx >= x && mx < x + w && my >= y && my < y + hh) {
                net.minecraft.client.Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1F));
                ((Runnable) h[4]).run();
                return true;
            }
        }
        return super.mouseClicked(rmx, rmy, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (fx.active() && key != 256) {
            fx.skip();
            return true;
        }
        if (settings != null && key == 256) {
            settings = null;
            return true;
        }
        if (minecraft != null && minecraft.options.keyInventory.matches(key, scan)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void onClose() {
        fx.skip();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

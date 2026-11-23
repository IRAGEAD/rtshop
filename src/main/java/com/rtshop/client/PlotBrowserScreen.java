package com.rtshop.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.rtshop.RtShop;
import com.rtshop.land.CastlePlots;
import com.rtshop.land.PlotBrowser;
import com.rtshop.net.PlotActionPacket;
import com.rtshop.net.PlotPreviewPacket;
import com.rtshop.net.ShopNet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Inflater;

/**
 * 토지 상인 창의 [구획 토지]: 성 안 토지(성 지도에서 고르기) · 판매 중인 구획 · 초보 농지.
 * 고른 토지의 둘레를 위에서 본 지도로 "지금 모습" 과 "구매 후 모습(건물 철거 · 평지)" 을 미리 본다.
 */
public class PlotBrowserScreen extends Screen {
    public record Entry(int id, String name, String kind, int area, long price, String status, String owner,
                        int x0, int z0, int x1, int z1, int castleNo, int group, boolean cleared, int clearWith) {
        boolean sale() {
            return "sale".equals(status);
        }

        boolean mine() {
            return "mine".equals(status);
        }
    }

    /** 네모가 아닌 토지의 실제 모양 (지도에 그릴 채움 네모들과 둘레 선들, 세계 좌표). */
    static final class Shape {
        java.util.BitSet mask;
        int x0, z0, w, h;
        /** 채움: {x0, z0, x1, z1} (끝 좌표는 포함하지 않음). */
        final List<int[]> rects = new ArrayList<>();
        /** 둘레: 가로선 {x0, x1, z}, 세로선 {x, z0, z1}. */
        final List<int[]> hEdges = new ArrayList<>(), vEdges = new ArrayList<>();
        double cx, cz;

        boolean in(int x, int z) {
            return x >= x0 && x < x0 + w && z >= z0 && z < z0 + h && mask.get((z - z0) * w + (x - x0));
        }

        static Shape of(java.util.BitSet mask, int x0, int z0, int x1, int z1) {
            Shape s = new Shape();
            s.mask = mask;
            s.x0 = x0;
            s.z0 = z0;
            s.w = x1 - x0 + 1;
            s.h = z1 - z0 + 1;
            // 채움: 줄마다 이어진 칸을 묶고, 위아래로 똑같은 묶음은 하나의 네모로 합친다
            java.util.Map<Long, Integer> open = new java.util.HashMap<>();
            long sumX = 0, sumZ = 0, n = 0;
            for (int z = z0; z <= z1 + 1; z++) {
                java.util.Map<Long, Integer> next = new java.util.HashMap<>();
                int run = Integer.MIN_VALUE;
                for (int x = x0; x <= x1 + 1; x++) {
                    boolean on = z <= z1 && x <= x1 && s.in(x, z);
                    if (on) {
                        sumX += x;
                        sumZ += z;
                        n++;
                    }
                    if (on && run == Integer.MIN_VALUE) run = x;
                    if (!on && run != Integer.MIN_VALUE) {
                        long key = (((long) run) << 32) ^ (x & 0xFFFFFFFFL);
                        Integer start = open.remove(key);
                        next.put(key, start == null ? z : start);
                        run = Integer.MIN_VALUE;
                    }
                }
                for (java.util.Map.Entry<Long, Integer> e : open.entrySet()) {
                    s.rects.add(new int[]{(int) (e.getKey() >> 32), e.getValue(), (int) (long) e.getKey(), z});
                }
                open = next;
            }
            s.cx = n == 0 ? (x0 + x1 + 1) / 2.0 : sumX / (double) n + 0.5;
            s.cz = n == 0 ? (z0 + z1 + 1) / 2.0 : sumZ / (double) n + 0.5;
            for (int z = z0; z <= z1 + 1; z++) {
                int run = Integer.MIN_VALUE;
                for (int x = x0; x <= x1 + 1; x++) {
                    boolean edge = x <= x1 && (s.in(x, z) != s.in(x, z - 1));
                    if (edge && run == Integer.MIN_VALUE) run = x;
                    if (!edge && run != Integer.MIN_VALUE) {
                        s.hEdges.add(new int[]{run, x, z});
                        run = Integer.MIN_VALUE;
                    }
                }
            }
            for (int x = x0; x <= x1 + 1; x++) {
                int run = Integer.MIN_VALUE;
                for (int z = z0; z <= z1 + 1; z++) {
                    boolean edge = z <= z1 && (s.in(x, z) != s.in(x - 1, z));
                    if (edge && run == Integer.MIN_VALUE) run = z;
                    if (!edge && run != Integer.MIN_VALUE) {
                        s.vEdges.add(new int[]{x, run, z});
                        run = Integer.MIN_VALUE;
                    }
                }
            }
            return s;
        }
    }

    private static final java.util.Map<Integer, Shape> SHAPES = new java.util.HashMap<>();

    public static int shapesForTest() {
        return SHAPES.size();
    }

    /** 점검용: 이 토지의 받은 모양 {칸 수, 둘레 선 조각 수}. 없으면 null. */
    public static int[] shapeForTest(int landId) {
        Shape s = SHAPES.get(landId);
        return s == null ? null : new int[]{s.mask.cardinality(), s.hEdges.size() + s.vEdges.size()};
    }

    /** 받은 미리보기 지도. */
    static final class Preview {
        int landId, x0, z0, step, w, h;
        byte[] flags;
        DynamicTexture now, after;
        ResourceLocation nowId, afterId;
    }

    /** 넓은 지도 (성 지도 밖의 토지). */
    static final class Area {
        int landId, x0, z0, step, w, h;
        DynamicTexture tex;
    }

    private static final ResourceLocation AREA_TEX = new ResourceLocation(RtShop.MODID, "dynamic/plot_area");
    private static Area area;
    private static final ResourceLocation NOW_TEX = new ResourceLocation(RtShop.MODID, "dynamic/plot_now");
    private static final ResourceLocation AFTER_TEX = new ResourceLocation(RtShop.MODID, "dynamic/plot_after");
    private static Preview preview;

    private static final String[] TAB_KINDS = {"castle", "plot", "starter"};
    private static final int MODE_MAP = 0, MODE_NOW = 1, MODE_AFTER = 2;

    private final List<Entry> all = new ArrayList<>();
    private JsonObject starter = new JsonObject();
    private long wallet;
    private int tab = 0;
    private int mode = MODE_MAP;
    private final Map<Integer, Integer> selected = new HashMap<>();
    private int scroll = 0;
    private Component result;
    private long resultAt;
    private int confirmId = -1;
    private long confirmAt;
    private boolean first = true;

    private int fx, fy, fw, fh, lx, ly, lw, lh, vx, vy, vw, vh, by;
    /** 지도 보기: 가운데 좌표와 배율(블록당 화면 칸). */
    private double mapCx, mapCz;
    private float mapScale = -1;
    private int mapFor = Integer.MIN_VALUE;
    private boolean dragging;
    private static final int ROW = 21;

    public PlotBrowserScreen() {
        super(Component.translatable("rtshop.plotui.title"));
    }

    // ------------------------------------------------------------------ 패킷

    public static void onList(String json) {
        Minecraft mc = Minecraft.getInstance();
        PlotBrowserScreen s = mc.screen instanceof PlotBrowserScreen p ? p : null;
        if (s == null) {
            s = new PlotBrowserScreen();
            mc.setScreen(s);
        }
        s.apply(JsonParser.parseString(json).getAsJsonObject());
    }

    public static void onPreview(PlotPreviewPacket m) {
        Minecraft mc = Minecraft.getInstance();
        try {
            byte[] rgb = inflate(m.rgb, m.w * m.h * 3);
            if (m.area) {
                NativeImage img = new NativeImage(m.w, m.h, false);
                for (int z = 0; z < m.h; z++) {
                    for (int x = 0; x < m.w; x++) {
                        int i = z * m.w + x;
                        img.setPixelRGBA(x, z, 0xFF000000 | ((rgb[i * 3 + 2] & 255) << 16) | ((rgb[i * 3 + 1] & 255) << 8) | (rgb[i * 3] & 255));
                    }
                }
                if (area != null) mc.getTextureManager().release(AREA_TEX);
                Area a = new Area();
                a.landId = m.landId;
                a.x0 = m.x0;
                a.z0 = m.z0;
                a.step = m.step;
                a.w = m.w;
                a.h = m.h;
                a.tex = new DynamicTexture(img);
                mc.getTextureManager().register(AREA_TEX, a.tex);
                area = a;
                return;
            }
            Preview p = new Preview();
            p.landId = m.landId;
            p.x0 = m.x0;
            p.z0 = m.z0;
            p.step = m.step;
            p.w = m.w;
            p.h = m.h;
            p.flags = m.flags;
            NativeImage a = new NativeImage(m.w, m.h, false);
            NativeImage b = new NativeImage(m.w, m.h, false);
            for (int z = 0; z < m.h; z++) {
                for (int x = 0; x < m.w; x++) {
                    int i = z * m.w + x;
                    int r = rgb[i * 3] & 255, g = rgb[i * 3 + 1] & 255, bl = rgb[i * 3 + 2] & 255;
                    a.setPixelRGBA(x, z, 0xFF000000 | (bl << 16) | (g << 8) | r);
                    int f = m.flags[i];
                    if ((f & (PlotBrowser.FLAG_PLOT | PlotBrowser.FLAG_CLEAR)) != 0) {
                        // 구매 후: 평평한 풀밭 (잔디 색, 바둑판처럼 아주 약한 무늬)
                        int sh = ((x + z) & 1) == 0 ? 236 : 220;
                        int gr = 0x7FB238;
                        r = ((gr >> 16) & 255) * sh / 255;
                        g = ((gr >> 8) & 255) * sh / 255;
                        bl = (gr & 255) * sh / 255;
                    }
                    b.setPixelRGBA(x, z, 0xFF000000 | (bl << 16) | (g << 8) | r);
                }
            }
            if (preview != null) {
                mc.getTextureManager().release(NOW_TEX);
                mc.getTextureManager().release(AFTER_TEX);
            }
            p.now = new DynamicTexture(a);
            p.after = new DynamicTexture(b);
            mc.getTextureManager().register(NOW_TEX, p.now);
            mc.getTextureManager().register(AFTER_TEX, p.after);
            p.nowId = NOW_TEX;
            p.afterId = AFTER_TEX;
            preview = p;
        } catch (Exception e) {
            RtShop.LOGGER.warn("[rtshop] 토지 미리보기를 그리지 못했습니다", e);
        }
    }

    private static byte[] inflate(byte[] in, int size) throws Exception {
        Inflater inf = new Inflater();
        inf.setInput(in);
        byte[] out = new byte[size];
        int n = 0;
        while (n < size && !inf.finished()) {
            int k = inf.inflate(out, n, size - n);
            if (k == 0 && (inf.needsInput() || inf.needsDictionary())) break;
            n += k;
        }
        inf.end();
        return out;
    }

    private void apply(JsonObject o) {
        all.clear();
        SHAPES.clear();
        for (JsonElement el : o.getAsJsonArray("plots")) {
            JsonObject e = el.getAsJsonObject();
            if (e.has("shape")) {
                java.util.BitSet mask = PlotBrowser.unpackMask(e.get("shape").getAsString());
                if (mask != null) {
                    SHAPES.put(e.get("id").getAsInt(), Shape.of(mask, e.get("x0").getAsInt(), e.get("z0").getAsInt(), e.get("x1").getAsInt(),
                            e.get("z1").getAsInt()));
                }
            }
            all.add(new Entry(e.get("id").getAsInt(), e.get("name").getAsString(), e.get("kind").getAsString(), e.get("area").getAsInt(),
                    e.get("price").getAsLong(), e.get("status").getAsString(), e.get("owner").getAsString(),
                    e.get("x0").getAsInt(), e.get("z0").getAsInt(), e.get("x1").getAsInt(), e.get("z1").getAsInt(),
                    e.get("castleNo").getAsInt(), e.get("group").getAsInt(), e.get("cleared").getAsBoolean(),
                    e.has("clearWith") ? e.get("clearWith").getAsInt() : 0));
        }
        all.sort((a, b) -> a.castleNo != b.castleNo ? Integer.compare(a.castleNo, b.castleNo) : Integer.compare(a.id, b.id));
        starter = o.has("starter") ? o.getAsJsonObject("starter") : new JsonObject();
        wallet = o.get("wallet").getAsLong();
        if (o.has("result")) {
            result = Component.Serializer.fromJson(o.get("result").getAsString());
            resultAt = System.currentTimeMillis();
        }
        if (first) {
            first = false;
            tab = count("castle") > 0 ? 0 : count("plot") > 0 ? 1 : 2;
            mode = MODE_MAP;
            Entry e = firstOf(TAB_KINDS[tab]);
            if (e != null) select(e, false);
        }
    }

    private int count(String kind) {
        int n = 0;
        for (Entry e : all) if (e.kind.equals(kind)) n++;
        return n;
    }

    private Entry firstOf(String kind) {
        Entry best = null;
        for (Entry e : all) {
            if (!e.kind.equals(kind)) continue;
            if (best == null || (e.sale() && !best.sale())) best = e;
            if (best.sale()) break;
        }
        return best;
    }

    private List<Entry> rows() {
        List<Entry> out = new ArrayList<>();
        for (Entry e : all) if (e.kind.equals(TAB_KINDS[tab])) out.add(e);
        return out;
    }

    private Entry sel() {
        Integer id = selected.get(tab);
        if (id == null) return null;
        for (Entry e : all) if (e.id == id) return e;
        return null;
    }

    private void select(Entry e, boolean sound) {
        selected.put(tab, e.id);
        confirmId = -1;
        if (preview == null || preview.landId != e.id) {
            ShopNet.CHANNEL.sendToServer(new PlotActionPacket(PlotBrowser.PREVIEW, e.id));
        }
        if (!onCastleMap(e) && (area == null || !covers(area, e))) {
            ShopNet.CHANNEL.sendToServer(new PlotActionPacket(PlotBrowser.AREA, e.id));
        }
        centerOn(e);
        if (sound) click();
    }

    private static boolean onCastleMap(Entry e) {
        CastlePlots.Def d = CastlePlots.def();
        return d.mapW > 0 && e.x0 >= d.mapX0 && e.x1 < d.mapX0 + d.mapW && e.z0 >= d.mapZ0 && e.z1 < d.mapZ0 + d.mapH;
    }

    private static boolean covers(Area a, Entry e) {
        return e.x0 >= a.x0 && e.z0 >= a.z0 && e.x1 < a.x0 + a.w * a.step && e.z1 < a.z0 + a.h * a.step;
    }

    private void centerOn(Entry e) {
        mapCx = (e.x0 + e.x1 + 1) / 2.0;
        mapCz = (e.z0 + e.z1 + 1) / 2.0;
        if (mapScale < 0) mapScale = Math.max(1F, Math.min(6F, vw / 110F));
        mapFor = e.id;
    }

    private void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    // ------------------------------------------------------------------ 배치

    @Override
    protected void init() {
        fw = Math.min(470, width - 10);
        fh = Math.min(268, height - 10);
        fx = (width - fw) / 2;
        fy = (height - fh) / 2;
        lx = fx + 8;
        ly = fy + 44;
        lw = Math.min(150, fw / 3);
        by = fy + fh - 42;
        lh = by - 4 - ly;
        vx = lx + lw + 6;
        vy = ly;
        vw = fx + fw - 8 - vx;
        vh = lh;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------ 그리기

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        Ui.woodFrame(g, fx, fy, fw, fh);
        Ui.titleBand(g, fx + 5, fy + 4, fx + fw - 5, fy + 21);
        g.fill(fx + 5, fy + 20, fx + fw - 5, fy + 21, Ui.C_GOLD_DARK);
        String back = Component.translatable("rtshop.plotui.back").getString();
        int backW = font.width(back) + 10;
        boolean hovBack = Ui.inside(mx, my, fx + 7, fy + 5, backW, 14);
        Ui.bevel(g, fx + 7, fy + 5, backW, 14, hovBack ? Ui.C_GOLD : Ui.C_GOLD_DARK, Ui.C_GOLD, Ui.C_SHADOW);
        g.drawString(font, back, fx + 12, fy + 8, Ui.C_INK, false);
        g.drawString(font, title, fx + 14 + backW, fy + 9, Ui.C_GOLD, true);
        String money = Ui.num(Math.max(wallet, ClientEconomy.wallet)) + " G";
        Ui.coin(g, fx + fw - 25 - font.width(money), fy + 7, 11);
        g.drawString(font, money, fx + fw - 10 - font.width(money), fy + 9, Ui.C_CREAM, false);

        // 탭
        String[] tabKeys = {"rtshop.plotui.tab_castle", "rtshop.plotui.tab_plot", "rtshop.plotui.tab_starter"};
        int tx = fx + 8;
        for (int i = 0; i < 3; i++) {
            String label = Component.translatable(tabKeys[i], count(TAB_KINDS[i])).getString();
            int w = font.width(label) + 12;
            boolean hov = Ui.inside(mx, my, tx, fy + 25, w, 15);
            Ui.bevel(g, tx, fy + 25, w, 15, i == tab ? Ui.C_PARCH : hov ? Ui.C_WOOD_HI : Ui.C_WOOD, Ui.C_WOOD_HI, Ui.C_SHADOW);
            g.drawString(font, label, tx + 6, fy + 29, i == tab ? Ui.C_INK : Ui.C_CREAM, false);
            tx += w + 3;
        }
        // 보기 버튼
        int[] modes = modes();
        int bx = fx + fw - 8;
        for (int i = modes.length - 1; i >= 0; i--) {
            String label = Component.translatable("rtshop.plotui.mode" + modes[i]).getString();
            int w = font.width(label) + 10;
            bx -= w;
            boolean hov = Ui.inside(mx, my, bx, fy + 25, w, 15);
            Ui.bevel(g, bx, fy + 25, w, 15, modes[i] == mode ? Ui.C_PARCH : hov ? Ui.C_WOOD_HI : Ui.C_WOOD, Ui.C_WOOD_HI, Ui.C_SHADOW);
            g.drawString(font, label, bx + 5, fy + 29, modes[i] == mode ? Ui.C_INK : Ui.C_CREAM, false);
            bx -= 3;
        }

        renderList(g, mx, my);
        Ui.bevel(g, vx, vy, vw, vh, 0xFF1A140C, Ui.C_SHADOW, Ui.C_WOOD_HI);
        if (mode == MODE_MAP) {
            renderMap(g, mx, my);
        } else {
            renderPreview(g, mx, my);
        }
        renderBottom(g, mx, my);
        super.render(g, mx, my, partial);
    }

    private int[] modes() {
        if (tab == 0) return new int[]{MODE_MAP, MODE_NOW, MODE_AFTER};
        return new int[]{MODE_MAP, MODE_NOW};
    }

    private void renderList(GuiGraphics g, int mx, int my) {
        Ui.bevel(g, lx, ly, lw, lh, Ui.C_PARCH_DARK, Ui.C_PARCH_SHADE, Ui.C_SHADOW);
        List<Entry> rows = rows();
        int visible = (lh - 4) / ROW;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - visible)));
        g.enableScissor(lx + 1, ly + 1, lx + lw - 1, ly + lh - 1);
        Entry s = sel();
        for (int i = 0; i < visible + 1 && i + scroll < rows.size(); i++) {
            Entry e = rows.get(i + scroll);
            int ry = ly + 2 + i * ROW;
            boolean hov = Ui.inside(mx, my, lx + 2, ry, lw - 4, ROW - 1);
            if (s != null && s.id == e.id) {
                g.fill(lx + 2, ry, lx + lw - 2, ry + ROW - 1, Ui.C_PARCH_HI);
                g.fill(lx + 2, ry, lx + 4, ry + ROW - 1, Ui.C_GOLD_DARK);
            } else if (hov) {
                g.fill(lx + 2, ry, lx + lw - 2, ry + ROW - 1, 0x40FFFFFF);
            }
            g.drawString(font, Ui.clip(font, e.name, lw - 12), lx + 7, ry + 2, Ui.C_INK, false);
            String line2;
            int col;
            if (e.mine()) {
                line2 = Component.translatable("rtshop.plotui.st_mine").getString();
                col = Ui.C_GREEN;
            } else if (e.sale()) {
                line2 = e.kind.equals("starter") ? Component.translatable("rtshop.plotui.st_free").getString()
                        : Ui.num(e.price) + " G · " + Component.translatable("rtshop.plotui.cells", e.area).getString();
                col = e.kind.equals("starter") ? Ui.C_INK_SOFT : (Math.max(wallet, ClientEconomy.wallet) >= e.price ? Ui.C_GOLD_DARK : Ui.C_RED);
            } else {
                line2 = Component.translatable("rtshop.plotui.st_sold", e.owner).getString();
                col = Ui.C_INK_SOFT;
            }
            Ui.scaledText(g, font, Ui.clip(font, line2, (int) ((lw - 12) / 0.8F)), lx + 7, ry + 12, 0.8F, col, false);
        }
        g.disableScissor();
        if (rows.isEmpty()) {
            int y = ly + 6;
            for (FormattedCharSequence l : font.split(Component.translatable("rtshop.plotui.empty_" + TAB_KINDS[tab]), lw - 12)) {
                g.drawString(font, l, lx + 6, y, Ui.C_INK_SOFT, false);
                y += 10;
            }
        }
    }

    private int statusColor(Entry e, int alpha) {
        int rgb = e.mine() ? 0x40E040 : e.sale() ? 0xFFFFFF : 0x606060;
        return (alpha << 24) | rgb;
    }

    private int sx(double wx) {
        return (int) Math.round(vx + vw / 2.0 + (wx - mapCx) * mapScale);
    }

    private int sz(double wz) {
        return (int) Math.round(vy + vh / 2.0 + (wz - mapCz) * mapScale);
    }

    /**
     * 넓은 지도: 확대(휠·+/-)·이동(끌기). 성 근처는 미리 그려 둔 성 지도, 그 밖은 서버가 그린 둘레 지도를 깐다.
     * 모든 토지를 상태별 색으로, 고른 토지는 노란 굵은 테두리, 내 위치는 흰 점으로 표시한다.
     */
    private void renderMap(GuiGraphics g, int mx, int my) {
        Entry sel = sel();
        CastlePlots.Def d = CastlePlots.def();
        if (mapScale < 0) {
            if (sel != null) centerOn(sel);
            else if (d.mapW > 0) {
                mapCx = d.mapX0 + d.mapW / 2.0;
                mapCz = d.mapZ0 + d.mapH / 2.0;
                mapScale = Math.max(0.4F, Math.min(vw / (float) d.mapW, vh / (float) d.mapH));
            } else {
                centered(g, Component.translatable("rtshop.plotui.pick"));
                return;
            }
        } else if (sel != null && mapFor != sel.id) {
            centerOn(sel);
        }
        g.enableScissor(vx + 1, vy + 1, vx + vw - 1, vy + vh - 1);
        g.fill(vx + 1, vy + 1, vx + vw - 1, vy + vh - 1, 0xFF2A2418);
        // 바탕 지도
        boolean castleBase = d.mapW > 0 && (tab == 0 || (sel != null && onCastleMap(sel)));
        g.pose().pushPose();
        g.pose().translate(vx + vw / 2.0, vy + vh / 2.0, 0);
        g.pose().scale(mapScale, mapScale, 1);
        g.pose().translate(-mapCx, -mapCz, 0);
        if (area != null && (sel == null || covers(area, sel))) {
            g.blit(AREA_TEX, area.x0, area.z0, area.w * area.step, area.h * area.step, 0, 0, area.w, area.h, area.w, area.h);
        }
        if (castleBase || d.mapW > 0) {
            g.blit(new ResourceLocation(d.mapTexture), d.mapX0, d.mapZ0, d.mapW, d.mapH, 0, 0, d.mapW, d.mapH, d.mapSize, d.mapSize);
        }
        g.pose().popPose();
        if (sel != null && !onCastleMap(sel) && (area == null || !covers(area, sel))) {
            Component c = Component.translatable("rtshop.plotui.loading");
            g.drawString(font, c, vx + vw / 2 - font.width(c) / 2, vy + 6, 0xFFFFFFFF, true);
        }
        // 토지들
        // 지금 보고 있는 갈래(성 안 토지 / 구획 토지 / 초보 농지)의 토지만 그린다. 다른 갈래를 함께 그리면 서로 겹쳐 보인다.
        Entry hover = null;
        for (Entry e : all) {
            if (!e.kind.equals(TAB_KINDS[tab])) continue;
            int x0 = sx(e.x0), z0 = sz(e.z0), x1 = sx(e.x1 + 1), z1 = sz(e.z1 + 1);
            if (x1 < vx || x0 > vx + vw || z1 < vy || z0 > vy + vh) continue;
            boolean isSel = sel != null && sel.id == e.id;
            int fill = statusColor(e, e.sale() ? 0x50 : 0x80);
            long blink = System.currentTimeMillis() / 250 % 2;
            int selCol = blink == 0 ? 0xFFFFE040 : 0xFFFFFFA0;
            Shape sh = SHAPES.get(e.id);
            float lx, lz;
            boolean over;
            if (sh == null) {
                g.fill(x0, z0, Math.max(x0 + 1, x1), Math.max(z0 + 1, z1), fill);
                outline(g, x0, z0, Math.max(x0 + 2, x1), Math.max(z0 + 2, z1), 0xC0000000);
                if (isSel) {
                    outline(g, x0 - 1, z0 - 1, x1 + 1, z1 + 1, selCol);
                    outline(g, x0 - 2, z0 - 2, x1 + 2, z1 + 2, selCol);
                }
                lx = (x0 + x1) / 2F;
                lz = (z0 + z1) / 2F;
                over = mx >= x0 && mx < x1 && my >= z0 && my < z1;
            } else {
                // 실제 모양대로 (농경지처럼 네모가 아닌 토지)
                for (int[] r : sh.rects) {
                    g.fill(sx(r[0]), sz(r[1]), sx(r[2]), sz(r[3]), fill);
                }
                int oc = isSel ? selCol : 0xC0000000;
                int th = isSel ? 2 : 1;
                for (int[] r : sh.hEdges) {
                    g.fill(sx(r[0]), sz(r[2]) - (th - 1), sx(r[1]) + 1, sz(r[2]) + 1, oc);
                }
                for (int[] r : sh.vEdges) {
                    g.fill(sx(r[0]) - (th - 1), sz(r[1]), sx(r[0]) + 1, sz(r[2]) + 1, oc);
                }
                lx = sx(sh.cx);
                lz = sz(sh.cz);
                int wx = (int) Math.floor(mapCx + (mx - (vx + vw / 2.0)) / mapScale), wz = (int) Math.floor(mapCz + (my - (vy + vh / 2.0)) / mapScale);
                over = sh.in(wx, wz);
            }
            if (mapScale * Math.min(e.x1 - e.x0 + 1, e.z1 - e.z0 + 1) >= 12) {
                String label = e.castleNo > 0 ? CastlePlots.label(e.castleNo).replace("-", "") : "";
                if (mapScale >= 5 || (e.castleNo <= 0 && mapScale * (e.x1 - e.x0 + 1) >= font.width(e.name) * 0.8F + 6)) label = e.name;
                float ts = Math.max(0.5F, Math.min(1F, mapScale / 3F));
                if (e.castleNo <= 0) ts = Math.max(ts, 0.75F);
                Ui.scaledText(g, font, label, lx - font.width(label) * ts / 2, lz - 4 * ts, ts, 0xFFFFFFFF, true);
            }
            if (over && Ui.inside(mx, my, vx, vy, vw, vh)) hover = e;
        }
        // 내 위치
        net.minecraft.client.player.LocalPlayer me = Minecraft.getInstance().player;
        if (me != null) {
            int px = sx(me.getX()), pz = sz(me.getZ());
            if (Ui.inside(px, pz, vx, vy, vw, vh)) {
                g.fill(px - 3, pz - 3, px + 3, pz + 3, 0xFF000000);
                g.fill(px - 2, pz - 2, px + 2, pz + 2, 0xFFFFFFFF);
                String meLabel = Component.translatable("rtshop.plotui.you").getString();
                Ui.scaledText(g, font, meLabel, px + 4, pz - 4, 0.75F, 0xFFFFFFFF, true);
            }
        }
        g.disableScissor();
        // 조작 단추와 안내
        String[] btn = {"+", "-", "⌖"};
        for (int i = 0; i < 3; i++) {
            int bx = vx + vw - 18, byy = vy + 4 + i * 16;
            boolean hov = Ui.inside(mx, my, bx, byy, 14, 14);
            Ui.bevel(g, bx, byy, 14, 14, hov ? Ui.C_WOOD_HI : Ui.C_WOOD, Ui.C_WOOD_HI, Ui.C_SHADOW);
            g.drawString(font, btn[i], bx + 7 - font.width(btn[i]) / 2, byy + 3, Ui.C_CREAM, false);
        }
        Component hint = Component.translatable("rtshop.plotui.map_hint");
        Ui.scaledText(g, font, hint.getString(), vx + 4, vy + 4, 0.75F, 0xFFE0E0E0, true);
        legend(g, vx + 4, vy + vh - 12, new int[]{0xFFFFE040, 0xFFFFFFFF, 0xFF40E040, 0xFF606060},
                new String[]{"rtshop.plotui.lg_this", "rtshop.plotui.lg_sale", "rtshop.plotui.lg_mine", "rtshop.plotui.lg_sold"});
        if (hover != null) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.literal(hover.name));
            tip.add(Component.literal(statusText(hover)));
            g.renderComponentTooltip(font, tip, mx, my);
        }
    }

    private boolean mapButton(double mx, double my) {
        for (int i = 0; i < 3; i++) {
            int bx = vx + vw - 18, byy = vy + 4 + i * 16;
            if (Ui.inside(mx, my, bx, byy, 14, 14)) {
                if (i == 0) zoom(1.5, vx + vw / 2.0, vy + vh / 2.0);
                else if (i == 1) zoom(1 / 1.5, vx + vw / 2.0, vy + vh / 2.0);
                else {
                    Entry e = sel();
                    if (e != null) centerOn(e);
                }
                click();
                return true;
            }
        }
        return false;
    }

    /** 커서 아래 지점을 그대로 두고 확대·축소. */
    private void zoom(double f, double mx, double my) {
        double wx = mapCx + (mx - (vx + vw / 2.0)) / mapScale, wz = mapCz + (my - (vy + vh / 2.0)) / mapScale;
        mapScale = (float) Math.max(0.3, Math.min(16, mapScale * f));
        mapCx = wx - (mx - (vx + vw / 2.0)) / mapScale;
        mapCz = wz - (my - (vy + vh / 2.0)) / mapScale;
    }

    private void outline(GuiGraphics g, int x0, int y0, int x1, int y1, int c) {
        g.fill(x0, y0, x1, y0 + 1, c);
        g.fill(x0, y1 - 1, x1, y1, c);
        g.fill(x0, y0, x0 + 1, y1, c);
        g.fill(x1 - 1, y0, x1, y1, c);
    }

    private void legend(GuiGraphics g, int x, int y, int[] cols, String[] keys) {
        for (int i = 0; i < cols.length; i++) {
            String l = Component.translatable(keys[i]).getString();
            g.fill(x - 1, y - 1, x + 9 + font.width(l) + 4, y + 9, 0xA0000000);
            g.fill(x, y + 1, x + 7, y + 8, cols[i]);
            g.drawString(font, l, x + 9, y + 1, 0xFFE0E0E0, false);
            x += 16 + font.width(l);
        }
    }

    private void renderPreview(GuiGraphics g, int mx, int my) {
        Entry e = sel();
        Preview p = preview;
        if (e == null) {
            centered(g, Component.translatable("rtshop.plotui.pick"));
            return;
        }
        if (p == null || p.landId != e.id) {
            centered(g, Component.translatable("rtshop.plotui.loading"));
            return;
        }
        boolean after = mode == MODE_AFTER;
        float cs = Math.min((vw - 6) / (float) p.w, (vh - 18) / (float) p.h);
        int dw = (int) (p.w * cs), dh = (int) (p.h * cs);
        int ox = vx + (vw - dw) / 2, oy = vy + 3 + (vh - 18 - dh) / 2;
        g.blit(after ? p.afterId : p.nowId, ox, oy, dw, dh, 0, 0, p.w, p.h, p.w, p.h);
        for (int z = 0; z < p.h; z++) {
            for (int x = 0; x < p.w; x++) {
                int f = p.flags[z * p.w + x];
                int x0 = ox + (int) (x * cs), x1 = ox + (int) ((x + 1) * cs), y0 = oy + (int) (z * cs), y1 = oy + (int) ((z + 1) * cs);
                if ((f & PlotBrowser.FLAG_PLOT) != 0) {
                    if (!after) g.fill(x0, y0, x1, y1, 0x30FFE040);
                } else if ((f & PlotBrowser.FLAG_CLEAR) != 0) {
                    if (!after && ((x + z) % 3 == 0)) g.fill(x0, y0, x1, y1, 0x70FF3030);
                    else if (!after) g.fill(x0, y0, x1, y1, 0x30FF3030);
                } else if ((f & PlotBrowser.FLAG_MINE) != 0) {
                    g.fill(x0, y0, x1, y1, 0x3040FF40);
                } else if ((f & PlotBrowser.FLAG_OTHER) != 0) {
                    g.fill(x0, y0, x1, y1, 0x303070FF);
                }
                if ((f & PlotBrowser.FLAG_PLOT) != 0) {
                    int c = 0xFFFFE040;
                    if (z == 0 || (p.flags[(z - 1) * p.w + x] & PlotBrowser.FLAG_PLOT) == 0) g.fill(x0, y0, x1, y0 + 1, c);
                    if (z == p.h - 1 || (p.flags[(z + 1) * p.w + x] & PlotBrowser.FLAG_PLOT) == 0) g.fill(x0, y1 - 1, x1, y1, c);
                    if (x == 0 || (p.flags[z * p.w + x - 1] & PlotBrowser.FLAG_PLOT) == 0) g.fill(x0, y0, x0 + 1, y1, c);
                    if (x == p.w - 1 || (p.flags[z * p.w + x + 1] & PlotBrowser.FLAG_PLOT) == 0) g.fill(x1 - 1, y0, x1, y1, c);
                }
            }
        }
        // 나침반 (위 = 북)
        g.drawString(font, "N", vx + vw - 12, vy + 5, 0xFFFFFFFF, true);
        if (e.kind.equals("castle") && !e.cleared && e.sale()) {
            legend(g, vx + 4, vy + vh - 12, new int[]{0xFFFFE040, 0xFFFF4040, 0xFF4070FF},
                    new String[]{"rtshop.plotui.lg_this", after ? "rtshop.plotui.lg_flat_too" : "rtshop.plotui.lg_clear", "rtshop.plotui.lg_other"});
        } else {
            legend(g, vx + 4, vy + vh - 12, new int[]{0xFFFFE040, 0xFF4070FF, 0xFF40E040},
                    new String[]{"rtshop.plotui.lg_this", "rtshop.plotui.lg_other", "rtshop.plotui.lg_yours"});
        }
        if (after) {
            Component t = Component.translatable("rtshop.plotui.after_note");
            g.drawString(font, t, vx + vw / 2 - font.width(t) / 2, vy + 5, 0xFFFFFFFF, true);
        }
    }

    private void centered(GuiGraphics g, Component c) {
        g.drawString(font, c, vx + vw / 2 - font.width(c) / 2, vy + vh / 2 - 4, 0xFFC0C0C0, false);
    }

    private String statusText(Entry e) {
        if (e.mine()) return Component.translatable("rtshop.plotui.st_mine").getString();
        if (!e.sale()) return Component.translatable("rtshop.plotui.st_sold", e.owner).getString();
        if (e.kind.equals("starter")) return Component.translatable("rtshop.plotui.st_free").getString();
        return Component.translatable("rtshop.plotui.st_sale", Ui.num(e.price)).getString();
    }

    private int buyW() {
        return 104;
    }

    private void renderBottom(GuiGraphics g, int mx, int my) {
        Ui.bevel(g, fx + 8, by, fw - 16, 34, Ui.C_PARCH, Ui.C_PARCH_HI, Ui.C_PARCH_SHADE);
        Entry e = sel();
        int textW = fw - 16 - buyW() - 66 - 14;
        if (result != null && System.currentTimeMillis() - resultAt < 6000) {
            int y = by + 4;
            for (FormattedCharSequence l : font.split(Component.literal(result.getString()), textW)) {
                g.drawString(font, l, fx + 14, y, Ui.C_INK, false);
                y += 10;
                if (y > by + 26) break;
            }
        } else if (tab == 2 && (e == null || !e.mine())) {
            String job = starter.has("job") ? starter.get("job").getAsString() : "";
            Component c = job.isEmpty() ? Component.translatable("rtshop.plotui.starter_off")
                    : starter.has("given") && starter.get("given").getAsBoolean() ? Component.translatable("rtshop.plotui.starter_given")
                    : Component.translatable("rtshop.plotui.starter_info");
            int y = by + 4;
            for (FormattedCharSequence l : font.split(c, textW)) {
                g.drawString(font, l, fx + 14, y, Ui.C_INK_SOFT, false);
                y += 10;
                if (y > by + 26) break;
            }
        } else if (e != null) {
            g.drawString(font, Ui.clip(font, e.name + "  ·  " + statusText(e), textW), fx + 14, by + 4, Ui.C_INK, false);
            String l2 = Component.translatable("rtshop.plotui.info", e.area, (e.x0 + e.x1) / 2, (e.z0 + e.z1) / 2).getString();
            if (e.kind.equals("castle") && e.sale() && !e.cleared) {
                l2 += "  " + Component.translatable(e.clearWith > 0 ? "rtshop.plotui.clear_group" : "rtshop.plotui.clear_one", e.clearWith).getString();
            }
            int y = by + 15;
            for (FormattedCharSequence l : font.split(Component.literal(l2), textW)) {
                g.drawString(font, l, fx + 14, y, Ui.C_INK_SOFT, false);
                y += 9;
                if (y > by + 26) break;
            }
        }
        // 버튼
        int bx = fx + fw - 14 - buyW();
        int byy = by + 9;
        boolean can = canAct(e);
        boolean hovBuy = Ui.inside(mx, my, bx, byy, buyW(), 16);
        Ui.button(g, font, bx, byy, buyW(), 16, actionLabel(e), hovBuy, can);
        int bw = 62;
        int bx2 = bx - 4 - bw;
        boolean hovB = Ui.inside(mx, my, bx2, byy, bw, 16);
        Ui.button(g, font, bx2, byy, bw, 16, Component.translatable("rtshop.plotui.border_btn").getString(), hovB, e != null);
    }

    private boolean canAct(Entry e) {
        if (tab == 2) {
            boolean hasJob = starter.has("hasJob") && starter.get("hasJob").getAsBoolean();
            boolean given = starter.has("given") && starter.get("given").getAsBoolean();
            return hasJob && !given && all.stream().anyMatch(x -> x.kind.equals("starter") && x.sale());
        }
        return e != null && e.sale() && Math.max(wallet, ClientEconomy.wallet) >= e.price;
    }

    private String actionLabel(Entry e) {
        if (tab == 2) return Component.translatable("rtshop.plotui.starter_btn").getString();
        if (e == null) return Component.translatable("rtshop.plotui.buy_btn", "-").getString();
        if (e.mine()) return Component.translatable("rtshop.plotui.st_mine").getString();
        if (!e.sale()) return Component.translatable("rtshop.plotui.sold_btn").getString();
        if (confirmId == e.id && System.currentTimeMillis() - confirmAt < 4000) {
            return Component.translatable("rtshop.plotui.confirm_btn").getString();
        }
        return Component.translatable("rtshop.plotui.buy_btn", Ui.compact(e.price)).getString();
    }

    // ------------------------------------------------------------------ 입력

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        String back = Component.translatable("rtshop.plotui.back").getString();
        if (Ui.inside(mx, my, fx + 7, fy + 5, font.width(back) + 10, 14)) {
            click();
            ShopNet.CHANNEL.sendToServer(new PlotActionPacket(PlotBrowser.BACK, 0));
            return true;
        }
        // 탭
        String[] tabKeys = {"rtshop.plotui.tab_castle", "rtshop.plotui.tab_plot", "rtshop.plotui.tab_starter"};
        int tx = fx + 8;
        for (int i = 0; i < 3; i++) {
            int w = font.width(Component.translatable(tabKeys[i], count(TAB_KINDS[i])).getString()) + 12;
            if (Ui.inside(mx, my, tx, fy + 25, w, 15)) {
                setTab(i);
                click();
                return true;
            }
            tx += w + 3;
        }
        int[] modes = modes();
        int bx = fx + fw - 8;
        for (int i = modes.length - 1; i >= 0; i--) {
            int w = font.width(Component.translatable("rtshop.plotui.mode" + modes[i]).getString()) + 10;
            bx -= w;
            if (Ui.inside(mx, my, bx, fy + 25, w, 15)) {
                mode = modes[i];
                click();
                return true;
            }
            bx -= 3;
        }
        // 목록
        if (Ui.inside(mx, my, lx, ly, lw, lh)) {
            int i = (int) ((my - ly - 2) / ROW) + scroll;
            List<Entry> rows = rows();
            if (i >= 0 && i < rows.size()) {
                select(rows.get(i), true);

            }
            return true;
        }
        // 지도: 단추, 토지 고르기, 끌어서 이동
        if (mode == MODE_MAP && Ui.inside(mx, my, vx, vy, vw, vh)) {
            if (mapButton(mx, my)) return true;
            for (Entry e : all) {
                if (!e.kind.equals(TAB_KINDS[tab])) continue;
                Shape sh = SHAPES.get(e.id);
                boolean hit = mx >= sx(e.x0) && mx < sx(e.x1 + 1) && my >= sz(e.z0) && my < sz(e.z1 + 1);
                if (hit && sh != null) {
                    hit = sh.in((int) Math.floor(mapCx + (mx - (vx + vw / 2.0)) / mapScale), (int) Math.floor(mapCz + (my - (vy + vh / 2.0)) / mapScale));
                }
                if (hit) {
                    select(e, true);
                    List<Entry> rows = rows();
                    int idx = rows.indexOf(e);
                    int visible = (lh - 4) / ROW;
                    if (idx >= 0 && (idx < scroll || idx >= scroll + visible)) scroll = Math.max(0, idx - visible / 2);
                    return true;
                }
            }
            dragging = true;
            return true;
        }
        // 버튼
        Entry e = sel();
        int bxb = fx + fw - 14 - buyW();
        int byy = by + 9;
        if (Ui.inside(mx, my, bxb, byy, buyW(), 16)) {
            if (!canAct(e)) return true;
            click();
            if (tab == 2) {
                ShopNet.CHANNEL.sendToServer(new PlotActionPacket(PlotBrowser.STARTER, 0));
            } else if (confirmId == e.id && System.currentTimeMillis() - confirmAt < 4000) {
                confirmId = -1;
                ShopNet.CHANNEL.sendToServer(new PlotActionPacket(PlotBrowser.BUY, e.id));
            } else {
                confirmId = e.id;
                confirmAt = System.currentTimeMillis();
            }
            return true;
        }
        if (e != null && Ui.inside(mx, my, bxb - 66, byy, 62, 16)) {
            click();
            ShopNet.CHANNEL.sendToServer(new PlotActionPacket(PlotBrowser.BORDER, e.id));
            onClose();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging && mode == MODE_MAP && mapScale > 0) {
            mapCx -= dx / mapScale;
            mapCz -= dy / mapScale;
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    private void setTab(int i) {
        tab = i;
        scroll = 0;
        confirmId = -1;
        mode = MODE_MAP;
        Entry s = sel();
        if (s == null) s = firstOf(TAB_KINDS[i]);
        if (s != null) select(s, false);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (mode == MODE_MAP && Ui.inside(mx, my, vx, vy, vw, vh) && mapScale > 0) {
            zoom(delta > 0 ? 1.25 : 0.8, mx, my);
            return true;
        }
        if (Ui.inside(mx, my, lx, ly, lw, lh)) {
            scroll = Math.max(0, scroll - (int) Math.signum(delta));
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    // ------------------------------------------------------------------ 시험용

    public int tabForTest() {
        return tab;
    }

    public void showForTest(int tab, int mode, int castleNo) {
        setTab(tab);
        this.mode = mode;
        if (castleNo > 0) {
            for (Entry e : all) {
                if (e.castleNo == castleNo) select(e, false);
            }
        }
    }

    public Entry selectedForTest() {
        return sel();
    }

    public int countForTest(String kind) {
        return count(kind);
    }

    public boolean areaReadyForTest() {
        Entry e = sel();
        return e != null && (onCastleMap(e) || (area != null && covers(area, e)));
    }

    public void zoomForTest(double f) {
        zoom(f, vx + vw / 2.0, vy + vh / 2.0);
    }

    public boolean previewReadyForTest() {
        Entry e = sel();
        return e != null && preview != null && preview.landId == e.id;
    }

    public boolean fitsForTest() {
        return fx >= 0 && fy >= 0 && fx + fw <= width && fy + fh <= height && lw > 60 && vw > 120;
    }

    public void buyForTest(int id) {
        ShopNet.CHANNEL.sendToServer(new PlotActionPacket(PlotBrowser.BUY, id));
    }
}

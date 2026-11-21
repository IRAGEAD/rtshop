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

    /** 받은 미리보기 지도. */
    static final class Preview {
        int landId, x0, z0, step, w, h;
        byte[] flags;
        DynamicTexture now, after;
        ResourceLocation nowId, afterId;
    }

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
        for (JsonElement el : o.getAsJsonArray("plots")) {
            JsonObject e = el.getAsJsonObject();
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
            mode = tab == 0 ? MODE_MAP : MODE_NOW;
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
        if (sound) click();
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
        g.fill(fx + 5, fy + 4, fx + fw - 5, fy + 21, Ui.C_WOOD_DARK);
        g.fill(fx + 5, fy + 20, fx + fw - 5, fy + 21, Ui.C_GOLD_DARK);
        g.drawString(font, title, fx + 10, fy + 9, Ui.C_GOLD, true);
        String money = Ui.num(Math.max(wallet, ClientEconomy.wallet)) + " G";
        Ui.coin(g, fx + fw - 18 - font.width(money), fy + 7, 11);
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
        if (mode == MODE_MAP && tab == 0) {
            renderCastleMap(g, mx, my);
        } else {
            renderPreview(g, mx, my);
        }
        renderBottom(g, mx, my);
        super.render(g, mx, my, partial);
    }

    private int[] modes() {
        if (tab == 0) return new int[]{MODE_MAP, MODE_NOW, MODE_AFTER};
        return new int[]{MODE_NOW};
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

    /** 성 지도: 모든 성 안 토지를 칸 모양으로 표시. 판매 중 = 흰색, 내 토지 = 초록, 팔림 = 회색, 고른 토지 = 노란 테두리. */
    private void renderCastleMap(GuiGraphics g, int mx, int my) {
        CastlePlots.Def d = CastlePlots.def();
        if (d.mapW <= 0) return;
        float s = Math.min((vw - 6) / (float) d.mapW, (vh - 6) / (float) d.mapH);
        int dw = (int) (d.mapW * s), dh = (int) (d.mapH * s);
        int ox = vx + (vw - dw) / 2, oy = vy + (vh - dh) / 2;
        g.blit(new ResourceLocation(d.mapTexture), ox, oy, dw, dh, 0, 0, d.mapW, d.mapH, d.mapSize, d.mapSize);
        Entry sel = sel();
        Entry hover = null;
        for (Entry e : all) {
            if (!e.kind.equals("castle")) continue;
            CastlePlots.Tile t = CastlePlots.tile(e.castleNo);
            if (t == null) continue;
            int alpha = e.sale() ? 0x55 : 0x90;
            for (int dz = 0; dz < 16; dz++) {
                int run = -1;
                for (int dx = 0; dx <= 16; dx++) {
                    boolean in = dx < 16 && t.has(dx, dz);
                    if (in && run < 0) run = dx;
                    if (!in && run >= 0) {
                        int x0 = ox + (int) ((t.x0 + run - d.mapX0) * s), x1 = ox + (int) ((t.x0 + dx - d.mapX0) * s);
                        int y0 = oy + (int) ((t.z0 + dz - d.mapZ0) * s), y1 = oy + (int) ((t.z0 + dz + 1 - d.mapZ0) * s);
                        if (y1 > y0 && x1 > x0) g.fill(x0, y0, x1, y1, statusColor(e, alpha));
                        run = -1;
                    }
                }
            }
            int rx0 = ox + (int) ((t.x0 - d.mapX0) * s), rz0 = oy + (int) ((t.z0 - d.mapZ0) * s);
            int rx1 = ox + (int) ((t.x0 + 16 - d.mapX0) * s), rz1 = oy + (int) ((t.z0 + 16 - d.mapZ0) * s);
            boolean isSel = sel != null && sel.id == e.id;
            int border = isSel ? 0xFFFFE040 : 0xC0000000;
            outline(g, rx0, rz0, rx1, rz1, border);
            if (isSel) outline(g, rx0 - 1, rz0 - 1, rx1 + 1, rz1 + 1, 0xFFFFE040);
            String no = Integer.toString(e.castleNo);
            float ts = Math.max(0.5F, Math.min(1F, s * 1.2F));
            Ui.scaledText(g, font, no, (rx0 + rx1) / 2F - font.width(no) * ts / 2, (rz0 + rz1) / 2F - 4 * ts, ts, 0xFFFFFFFF, true);
            if (Ui.inside(mx, my, rx0, rz0, rx1 - rx0, rz1 - rz0)) hover = e;
        }
        legend(g, vx + 4, vy + vh - 12, new int[]{0xFFFFFFFF, 0xFF40E040, 0xFF606060},
                new String[]{"rtshop.plotui.lg_sale", "rtshop.plotui.lg_mine", "rtshop.plotui.lg_sold"});
        if (hover != null) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.literal(hover.name));
            tip.add(Component.literal(statusText(hover)));
            g.renderComponentTooltip(font, tip, mx, my);
        }
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
            for (FormattedCharSequence l : font.split(result, textW)) {
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
                if (mode == MODE_MAP && tab != 0) mode = MODE_NOW;
            }
            return true;
        }
        // 성 지도에서 고르기
        if (mode == MODE_MAP && tab == 0 && Ui.inside(mx, my, vx, vy, vw, vh)) {
            CastlePlots.Def d = CastlePlots.def();
            float s = Math.min((vw - 6) / (float) d.mapW, (vh - 6) / (float) d.mapH);
            int dw = (int) (d.mapW * s), dh = (int) (d.mapH * s);
            int ox = vx + (vw - dw) / 2, oy = vy + (vh - dh) / 2;
            double wx = d.mapX0 + (mx - ox) / s, wz = d.mapZ0 + (my - oy) / s;
            for (Entry e : all) {
                if (!e.kind.equals("castle")) continue;
                CastlePlots.Tile t = CastlePlots.tile(e.castleNo);
                if (t != null && wx >= t.x0 && wx < t.x0 + 16 && wz >= t.z0 && wz < t.z0 + 16) {
                    select(e, true);
                    List<Entry> rows = rows();
                    int idx = rows.indexOf(e);
                    int visible = (lh - 4) / ROW;
                    if (idx >= 0 && (idx < scroll || idx >= scroll + visible)) scroll = Math.max(0, idx - visible / 2);
                    return true;
                }
            }
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

    private void setTab(int i) {
        tab = i;
        scroll = 0;
        confirmId = -1;
        mode = i == 0 ? MODE_MAP : MODE_NOW;
        Entry s = sel();
        if (s == null) s = firstOf(TAB_KINDS[i]);
        if (s != null) select(s, false);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
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

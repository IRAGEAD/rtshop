package com.rtshop.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** 레이드 화면 꾸밈: 불타는 테두리의 판, 마법진, 타오르는 단추, 장식 줄. 모두 시간(초)의 함수라서 정지 화면으로도 시험할 수 있다. */
public final class RaidFx {
    private RaidFx() {
    }

    public static final int GOLD = 0xFFB040, FIRE = 0xFF5A14, DEEP = 0x8A1408;

    /** 어두운 붉은 바탕 + 아래에서 올라오는 열기 + 갈라진 틈의 불빛 + 불타는 테두리와 모서리 장식. cold = true 면 금빛(성공), false 면 불빛. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h, float time, boolean gold) {
        int edge = gold ? 0xE8B84A : FIRE, hot = gold ? 0xFFF0B0 : 0xFFD070;
        g.fill(x - 4, y - 4, x + w + 4, y + h + 4, 0xFF000000);
        Fx2D.begin(g, false);
        Fx2D.rect(x, y, x + w, y + h, 0xFF0A0405, 0xFF0A0405, gold ? 0xFF2A1C08 : 0xFF3A0A06, gold ? 0xFF2A1C08 : 0xFF3A0A06);
        Fx2D.end();
        Fx2D.begin(g, true);
        // 아래에서 올라오는 열기
        Fx2D.glow(x + w * 0.5F, y + h + 20, w * 0.7F, h * 0.75F, Fx2D.rgba(edge, 0.42F), Fx2D.rgba(edge, 0));
        // 바닥의 갈라진 틈
        for (int i = 0; i < 7; i++) {
            float bx = x + 10 + Fx2D.hash(i, 11) * (w - 20), by = y + h - 3;
            float px = bx, py = by;
            float pulse = 0.35F + 0.35F * (float) Math.sin(time * 1.7F + i * 1.3F);
            for (int k = 0; k < 4; k++) {
                float nx = px + (Fx2D.hash(i * 7 + k, 13) - 0.5F) * 22, ny = py - 5 - Fx2D.hash(i * 7 + k, 17) * 9;
                Fx2D.beam(px, py, nx, ny, 3.5F - k * 0.6F, Fx2D.rgba(hot, pulse * (1 - k * 0.22F)));
                px = nx;
                py = ny;
            }
        }
        // 테두리: 은은한 선 + 그 위를 도는 불빛
        float per = 2F * (w + h);
        for (int pass = 0; pass < 2; pass++) {
            int in = pass * 3;
            int c = Fx2D.rgba(edge, pass == 0 ? 0.85F : 0.35F);
            Fx2D.beam(x + in, y + in, x + w - in, y + in, 2.2F, c);
            Fx2D.beam(x + in, y + h - in, x + w - in, y + h - in, 2.2F, c);
            Fx2D.beam(x + in, y + in, x + in, y + h - in, 2.2F, c);
            Fx2D.beam(x + w - in, y + in, x + w - in, y + h - in, 2.2F, c);
        }
        for (int i = 0; i < 4; i++) {
            float d = ((time * 55F + i * per / 4F) % per + per) % per;
            float[] p = along(x, y, w, h, d), q = along(x, y, w, h, d + 26);
            Fx2D.glow(p[0], p[1], 9, Fx2D.rgba(hot, 0.9F), Fx2D.rgba(edge, 0));
            if (Math.abs(p[0] - q[0]) < 1 || Math.abs(p[1] - q[1]) < 1) {
                Fx2D.beam(p[0], p[1], q[0], q[1], 5F, Fx2D.rgba(hot, 0.7F));
            }
        }
        // 모서리 장식
        int[][] cs = {{x, y}, {x + w, y}, {x, y + h}, {x + w, y + h}};
        for (int[] c : cs) {
            Fx2D.glow(c[0], c[1], 13, Fx2D.rgba(edge, 0.55F), Fx2D.rgba(edge, 0));
            Fx2D.star(c[0], c[1], 9 + 2 * (float) Math.sin(time * 2.2F), 1.6F, Fx2D.rgba(hot, 0.95F));
        }
        Fx2D.end();
        Fx2D.begin(g, false);
        for (int[] c : cs) {
            Fx2D.diamond(c[0], c[1], 4.5F, 4.5F, 0, 0xFF000000 | edge);
            Fx2D.diamond(c[0], c[1], 2.2F, 2.2F, 0, 0xFF000000 | hot);
        }
        Fx2D.end();
    }

    private static float[] along(int x, int y, int w, int h, float d) {
        float per = 2F * (w + h);
        d = ((d % per) + per) % per;
        if (d < w) return new float[]{x + d, y};
        d -= w;
        if (d < h) return new float[]{x + w, y + d};
        d -= h;
        if (d < w) return new float[]{x + w - d, y + h};
        d -= w;
        return new float[]{x, y + h - d};
    }

    /** 떠오르는 불티. */
    public static void embers(GuiGraphics g, int x, int y, int w, int h, float time, int n, boolean gold) {
        Fx2D.begin(g, true);
        for (int i = 0; i < n; i++) {
            float sp = 0.10F + Fx2D.hash(i, 3) * 0.18F;
            float ph = (time * sp + Fx2D.hash(i, 5)) % 1F;
            float px = x + Fx2D.hash(i, 1) * w + (float) Math.sin(time * 1.3F + i) * 6F;
            float py = y + h - ph * h;
            float a = (1 - ph) * 0.85F;
            int c = gold ? (i % 3 == 0 ? 0xFFF4C0 : 0xE8B84A) : (i % 3 == 0 ? 0xFFD060 : 0xFF6A20);
            Fx2D.glow(px, py, 1.4F + Fx2D.hash(i, 7) * 2.2F, Fx2D.rgba(c, a), Fx2D.rgba(c, 0));
        }
        Fx2D.end();
    }

    /** 바닥에 누운 마법진(타원)과 위로 솟는 불기둥: 보스가 그 위에 선다. */
    public static void summonCircle(GuiGraphics g, float cx, float cy, float rx, float time) {
        float ry = rx * 0.28F;
        Fx2D.begin(g, true);
        Fx2D.glow(cx, cy, rx * 1.15F, ry * 1.8F, Fx2D.rgba(FIRE, 0.55F), Fx2D.rgba(FIRE, 0));
        // 불기둥
        for (int i = 0; i < 9; i++) {
            float a = (float) (i * Math.PI * 2 / 9 + time * 0.35F);
            float px = cx + (float) Math.cos(a) * rx * 0.86F, py = cy + (float) Math.sin(a) * ry * 0.86F;
            float len = 34 + 22 * (float) Math.sin(time * 2.1F + i * 1.9F);
            float back = (float) Math.sin(a) < 0 ? 0.55F : 0.3F; // 뒤쪽 기둥이 더 잘 보이게 (앞쪽은 보스를 가리지 않게 옅게)
            Fx2D.beam(px, py, px, py - len, 7F, Fx2D.rgba(0xFFB040, back));
        }
        // 타원 고리 두 겹 + 눈금
        int seg = 48;
        for (int ring = 0; ring < 2; ring++) {
            float k = ring == 0 ? 1F : 0.72F;
            float rot = time * (ring == 0 ? 0.5F : -0.8F);
            for (int i = 0; i < seg; i++) {
                double a0 = i * Math.PI * 2 / seg + rot, a1 = (i + 1) * Math.PI * 2 / seg + rot;
                boolean dash = ring == 1 && i % 4 == 3;
                if (dash) continue;
                Fx2D.beam(cx + (float) Math.cos(a0) * rx * k, cy + (float) Math.sin(a0) * ry * k,
                        cx + (float) Math.cos(a1) * rx * k, cy + (float) Math.sin(a1) * ry * k, ring == 0 ? 3.2F : 2.2F, Fx2D.rgba(ring == 0 ? 0xFFD070 : 0xFF7A20, 0.9F));
            }
        }
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3 + time * 0.5F;
            double b = a + Math.PI * 2 / 3;
            Fx2D.beam(cx + (float) Math.cos(a) * rx, cy + (float) Math.sin(a) * ry, cx + (float) Math.cos(b) * rx, cy + (float) Math.sin(b) * ry, 1.8F, Fx2D.rgba(0xFF7A20, 0.5F));
        }
        Fx2D.end();
    }

    /** 가운데 마름모가 박힌 장식 줄. */
    public static void divider(GuiGraphics g, int x0, int x1, int y, int rgb) {
        Fx2D.begin(g, true);
        float cx = (x0 + x1) / 2F;
        Fx2D.beam(x0, y, cx, y, 3F, Fx2D.rgba(rgb, 0.0F));
        Fx2D.tri(x0, y, Fx2D.rgba(rgb, 0), cx, y - 1, Fx2D.rgba(rgb, 0.9F), cx, y + 1, Fx2D.rgba(rgb, 0.9F));
        Fx2D.tri(x1, y, Fx2D.rgba(rgb, 0), cx, y - 1, Fx2D.rgba(rgb, 0.9F), cx, y + 1, Fx2D.rgba(rgb, 0.9F));
        Fx2D.glow(cx, y, 8, Fx2D.rgba(rgb, 0.6F), Fx2D.rgba(rgb, 0));
        Fx2D.end();
        Fx2D.begin(g, false);
        Fx2D.diamond(cx, y, 4, 4, 0, 0xFF000000 | rgb);
        Fx2D.diamond(cx, y, 1.8F, 1.8F, 0, 0xFFFFF0D0);
        Fx2D.end();
    }

    /** 타오르는 단추: 켜져 있으면 맥박처럼 빛나고 빛줄기가 지나간다. */
    public static void hotButton(GuiGraphics g, Font font, int x, int y, int w, int h, String label, boolean enabled, boolean hover, float time, boolean gold) {
        int a = gold ? 0xC89020 : 0xC83A14, b = gold ? 0x6A4A0C : 0x6A1408, hi = gold ? 0xFFF0B0 : 0xFFC070;
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xFF000000);
        if (!enabled) {
            g.fill(x, y, x + w, y + h, 0xFF241614);
            g.fill(x, y, x + w, y + 1, 0xFF4A3028);
            g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, 0xFF7A6A60);
            return;
        }
        float pulse = 0.5F + 0.5F * (float) Math.sin(time * 3.2F);
        Fx2D.begin(g, true);
        Fx2D.glow(x + w / 2F, y + h / 2F, w * 0.62F, h * 1.5F, Fx2D.rgba(a, 0.35F + 0.3F * pulse + (hover ? 0.25F : 0)), Fx2D.rgba(a, 0));
        Fx2D.end();
        Fx2D.begin(g, false);
        Fx2D.rect(x, y, x + w, y + h, 0xFF000000 | (hover ? Fx2D.mix(a, hi, 0.35F) & 0xFFFFFF : a), 0xFF000000 | (hover ? Fx2D.mix(a, hi, 0.35F) & 0xFFFFFF : a), 0xFF000000 | b, 0xFF000000 | b);
        Fx2D.end();
        Fx2D.begin(g, true);
        float sweep = ((time * 0.6F) % 1.6F) - 0.3F;
        float sx = x + sweep * w;
        if (sx > x - 14 && sx < x + w + 14) {
            float l = Math.max(x, sx - 12), r = Math.min(x + w, sx + 12);
            Fx2D.rect(l, y, r, y + h, Fx2D.rgba(hi, 0), Fx2D.rgba(hi, 0.35F), Fx2D.rgba(hi, 0.35F), Fx2D.rgba(hi, 0));
        }
        Fx2D.beam(x, y, x + w, y, 2F, Fx2D.rgba(hi, 0.9F));
        Fx2D.end();
        g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, 0xFFFFF4DC);
    }

    /** 빛나는 큰 제목. */
    public static void title(GuiGraphics g, Font font, String text, int x, int y, float scale, int rgb, float time) {
        float w = font.width(text) * scale;
        Fx2D.begin(g, true);
        Fx2D.glow(x + w / 2F, y + 4 * scale, w * 0.6F, 12 * scale, Fx2D.rgba(FIRE, 0.4F + 0.12F * (float) Math.sin(time * 2F)), Fx2D.rgba(FIRE, 0));
        Fx2D.end();
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1F);
        g.drawString(font, text, 1, 1, 0xFF3A0A04, false);
        g.drawString(font, text, 0, 0, 0xFF000000 | rgb, false);
        g.pose().popPose();
    }

    // ================================================================== 고급 테마 (금박 테두리 · 흑요석 바탕)
    public static final int GOLD_HI = 0xFFF3D9A0, GOLD_MID = 0xFFD4A85A, GOLD_LO = 0xFF8A6430, BRONZE = 0xFF4A3018;

    /** 1픽셀 사각 테두리. */
    public static void box(GuiGraphics g, int x0, int y0, int x1, int y1, int c) {
        g.fill(x0, y0, x1, y0 + 1, c);
        g.fill(x0, y1 - 1, x1, y1, c);
        g.fill(x0, y0 + 1, x0 + 1, y1 - 1, c);
        g.fill(x1 - 1, y0 + 1, x1, y1 - 1, c);
    }

    /** 가운데가 진하고 양끝이 사라지는 가로 금줄. */
    public static void hairline(GuiGraphics g, float x0, float x1, float y, int rgb, float a) {
        float cx = (x0 + x1) / 2F;
        Fx2D.begin(g, false);
        Fx2D.rect(x0, y, cx, y + 1, Fx2D.rgba(rgb, 0), Fx2D.rgba(rgb, a), Fx2D.rgba(rgb, a), Fx2D.rgba(rgb, 0));
        Fx2D.rect(cx, y, x1, y + 1, Fx2D.rgba(rgb, a), Fx2D.rgba(rgb, 0), Fx2D.rgba(rgb, 0), Fx2D.rgba(rgb, a));
        Fx2D.end();
    }

    private static void gem(GuiGraphics g, float cx, float cy, float r, int rgb, float time, float phase) {
        Fx2D.begin(g, true);
        Fx2D.glow(cx, cy, r * 3.2F, Fx2D.rgba(rgb, 0.45F + 0.2F * (float) Math.sin(time * 2F + phase)), Fx2D.rgba(rgb, 0));
        Fx2D.end();
        Fx2D.begin(g, false);
        Fx2D.diamond(cx, cy, r + 1.5F, r + 1.5F, 0, 0xFF000000 | (GOLD_MID & 0xFFFFFF));
        Fx2D.diamond(cx, cy, r, r, 0, 0xFF000000 | rgb);
        Fx2D.diamond(cx - r * 0.25F, cy - r * 0.3F, r * 0.35F, r * 0.35F, 0, 0xFFFFF4E0);
        Fx2D.end();
    }

    /**
     * 고급 판: 흑요석 바탕(은은한 마름모 무늬, 아래에서 올라오는 열기) + 이중 금박 테두리 + 모서리 금장식과 보석.
     * accent: 보석과 열기의 색 (불꽃 = FIRE, 승리 = 금빛).
     */
    public static void luxPanel(GuiGraphics g, int x, int y, int w, int h, float time, int accent) {
        for (int i = 6; i >= 1; i--) g.fill(x - 3 - i, y - 3 - i, x + w + 3 + i, y + h + 3 + i, (int) (0x16 + (6 - i) * 0x06) << 24);
        g.fill(x - 3, y - 3, x + w + 3, y + h + 3, 0xFF000000);
        Fx2D.begin(g, false);
        Fx2D.rect(x, y, x + w, y + h, 0xFF100A0B, 0xFF100A0B, 0xFF221010, 0xFF221010);
        Fx2D.end();
        Fx2D.begin(g, true);
        Fx2D.glow(x + w * 0.5F, y + h + 10, w * 0.62F, h * 0.55F, Fx2D.rgba(accent, 0.26F + 0.05F * (float) Math.sin(time * 1.4F)), Fx2D.rgba(accent, 0));
        Fx2D.glow(x + w * 0.5F, y - 6, w * 0.5F, 40, Fx2D.rgba(0xD4A85A, 0.10F), Fx2D.rgba(0xD4A85A, 0));
        for (int gx = x + 12; gx < x + w - 6; gx += 16) {
            for (int gy = y + 12; gy < y + h - 6; gy += 16) {
                Fx2D.diamond(gx + ((gy - y) / 16 % 2) * 8, gy, 1.2F, 1.2F, 0, Fx2D.rgba(0xD4A85A, 0.05F));
            }
        }
        Fx2D.end();
        // 이중 금박
        box(g, x - 3, y - 3, x + w + 3, y + h + 3, GOLD_LO);
        box(g, x - 1, y - 1, x + w + 1, y + h + 1, GOLD_MID);
        box(g, x + 2, y + 2, x + w - 2, y + h - 2, 0x60D4A85A);
        g.fill(x - 1, y - 1, x + w + 1, y, GOLD_HI);
        // 위 테두리를 따라 천천히 흐르는 빛
        Fx2D.begin(g, true);
        float sx = x + ((time * 0.12F) % 1F) * w;
        Fx2D.glow(sx, y - 1, 30, 3, Fx2D.rgba(0xFFF0C0, 0.55F), Fx2D.rgba(0xFFF0C0, 0));
        Fx2D.glow(x + w - (sx - x), y + h, 30, 3, Fx2D.rgba(0xFFF0C0, 0.35F), Fx2D.rgba(0xFFF0C0, 0));
        Fx2D.end();
        // 모서리 금장식: 꺾쇠 두 겹 + 끝의 작은 마름모 + 보석
        int[][] cs = {{x, y, 1, 1}, {x + w, y, -1, 1}, {x, y + h, 1, -1}, {x + w, y + h, -1, -1}};
        int k = 0;
        for (int[] c : cs) {
            int cx = c[0], cy = c[1], sx2 = c[2], sy2 = c[3];
            for (int j = 0; j < 2; j++) {
                int o = 5 + j * 3, L = j == 0 ? 22 : 13;
                int col = j == 0 ? GOLD_MID : GOLD_LO;
                int ax = cx + sx2 * o, ay = cy + sy2 * o;
                g.fill(Math.min(ax, ax + sx2 * L), ay, Math.max(ax, ax + sx2 * L) + 1, ay + 1, col);
                g.fill(ax, Math.min(ay, ay + sy2 * L), ax + 1, Math.max(ay, ay + sy2 * L) + 1, col);
            }
            Fx2D.begin(g, false);
            Fx2D.diamond(cx + sx2 * 28.5F, cy + sy2 * 5.5F, 2F, 2F, 0, GOLD_HI);
            Fx2D.diamond(cx + sx2 * 5.5F, cy + sy2 * 28.5F, 2F, 2F, 0, GOLD_HI);
            Fx2D.end();
            gem(g, cx, cy, 3.2F, accent, time, k++ * 1.7F);
        }
    }

    /** 그림 액자: 검은 여백 + 금박 + 안쪽 가는 금선, 위·아래 가운데 문장과 네 모서리 마름모. */
    public static void frame(GuiGraphics g, int x, int y, int w, int h, float time) {
        box(g, x - 4, y - 4, x + w + 4, y + h + 4, GOLD_LO);
        box(g, x - 3, y - 3, x + w + 3, y + h + 3, 0xFF000000);
        box(g, x - 2, y - 2, x + w + 2, y + h + 2, GOLD_MID);
        g.fill(x - 2, y - 2, x + w + 2, y - 1, GOLD_HI);
        box(g, x - 1, y - 1, x + w + 1, y + h + 1, 0xFF000000);
        Fx2D.begin(g, false);
        for (int[] c : new int[][]{{x - 2, y - 2}, {x + w + 2, y - 2}, {x - 2, y + h + 2}, {x + w + 2, y + h + 2}}) {
            Fx2D.diamond(c[0], c[1], 4.5F, 4.5F, 0, 0xFF000000);
            Fx2D.diamond(c[0], c[1], 3.5F, 3.5F, 0, GOLD_MID);
            Fx2D.diamond(c[0], c[1], 1.6F, 1.6F, 0, GOLD_HI);
        }
        Fx2D.end();
        crest(g, x + w / 2F, y - 2, time);
        crest(g, x + w / 2F, y + h + 2, time);
    }

    /** 가운데 문장: 날개처럼 퍼지는 금줄과 붉은 보석. */
    public static void crest(GuiGraphics g, float cx, float cy, float time) {
        Fx2D.begin(g, false);
        Fx2D.tri(cx - 18, cy, GOLD_LO, cx - 5, cy - 2.5F, GOLD_HI, cx - 5, cy + 2.5F, GOLD_HI);
        Fx2D.tri(cx + 18, cy, GOLD_LO, cx + 5, cy - 2.5F, GOLD_HI, cx + 5, cy + 2.5F, GOLD_HI);
        Fx2D.end();
        gem(g, cx, cy, 3.4F, FIRE, time, 0.5F);
    }

    /** 장식 머리말: ◆ 글자 ─────── (오른쪽으로 사라지는 금줄). */
    public static void section(GuiGraphics g, Font font, String text, int x, int y, int w) {
        Fx2D.begin(g, false);
        Fx2D.diamond(x + 3, y + 4, 2.6F, 2.6F, 0, GOLD_MID);
        Fx2D.diamond(x + 3, y + 4, 1.1F, 1.1F, 0, GOLD_HI);
        Fx2D.end();
        RaidText.draw(g, font, text, RaidText.SERIF, x + 9, y, GOLD_HI);
        int tx = x + 13 + RaidText.w(font, text, RaidText.SERIF);
        if (tx < x + w) {
            Fx2D.begin(g, false);
            Fx2D.rect(tx, y + 4, x + w, y + 5, Fx2D.alpha(GOLD_MID, 0.85F), Fx2D.alpha(GOLD_MID, 0), Fx2D.alpha(GOLD_MID, 0), Fx2D.alpha(GOLD_MID, 0.85F));
            Fx2D.end();
        }
    }

    /** 둥근 표식: 통과(초록 ✔) / 미달(붉은 ✖). */
    public static void badge(GuiGraphics g, float cx, float cy, boolean ok) {
        int c = ok ? 0x6FD483 : 0xFF5A48;
        Fx2D.begin(g, true);
        Fx2D.glow(cx, cy, 7, Fx2D.rgba(c, 0.35F), Fx2D.rgba(c, 0));
        Fx2D.end();
        Fx2D.begin(g, false);
        Fx2D.ring(cx, cy, 0, 4.6F, 0xFF000000, 0xFF000000);
        Fx2D.ring(cx, cy, 0, 3.9F, 0xFF000000 | Fx2D.mix(c, 0x000000, 0.45F), 0xFF000000 | c);
        if (ok) {
            Fx2D.line(cx - 2.1F, cy + 0.1F, cx - 0.6F, cy + 1.7F, 1.2F, 0xFFFFFFFF, 0xFFFFFFFF);
            Fx2D.line(cx - 0.6F, cy + 1.7F, cx + 2.2F, cy - 1.8F, 1.2F, 0xFFFFFFFF, 0xFFFFFFFF);
        } else {
            Fx2D.line(cx - 1.7F, cy - 1.7F, cx + 1.7F, cy + 1.7F, 1.2F, 0xFFFFFFFF, 0xFFFFFFFF);
            Fx2D.line(cx - 1.7F, cy + 1.7F, cx + 1.7F, cy - 1.7F, 1.2F, 0xFFFFFFFF, 0xFFFFFFFF);
        }
        Fx2D.end();
    }

    /**
     * 고급 단추: 금박 테두리, 주 단추는 진홍빛 칠과 지나가는 금빛, 보조 단추는 어두운 칠. 글자는 명조.
     */
    public static void luxButton(GuiGraphics g, Font font, int x, int y, int w, int h, String label, boolean enabled, boolean hover, float time, boolean primary) {
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xFF000000);
        if (!enabled) {
            box(g, x - 1, y - 1, x + w + 1, y + h + 1, 0xFF4A4038);
            Fx2D.begin(g, false);
            Fx2D.rect(x, y, x + w, y + h, 0xFF2A2422, 0xFF2A2422, 0xFF161212, 0xFF161212);
            Fx2D.end();
            RaidText.center(g, font, label, RaidText.SERIF, x + w / 2, y + (h - 9) / 2 + 1, 0xFF7E7268);
            return;
        }
        float pulse = 0.5F + 0.5F * (float) Math.sin(time * 2.6F);
        if (primary) {
            Fx2D.begin(g, true);
            Fx2D.glow(x + w / 2F, y + h / 2F, w * 0.6F, h * 1.4F, Fx2D.rgba(FIRE, 0.22F + 0.16F * pulse + (hover ? 0.2F : 0)), Fx2D.rgba(FIRE, 0));
            Fx2D.end();
        }
        box(g, x - 1, y - 1, x + w + 1, y + h + 1, hover ? GOLD_HI : GOLD_MID);
        int top = primary ? (hover ? 0xFFB8361A : 0xFF9A2612) : (hover ? 0xFF3E2C20 : 0xFF2C1E16);
        int bot = primary ? (hover ? 0xFF5A0E08 : 0xFF420A06) : (hover ? 0xFF1C120E : 0xFF140C0A);
        Fx2D.begin(g, false);
        Fx2D.rect(x, y, x + w, y + h, top, top, bot, bot);
        Fx2D.end();
        g.fill(x, y, x + w, y + 1, primary ? 0x90FFD9A0 : 0x50F3D9A0);
        box(g, x + 1, y + 1, x + w - 1, y + h - 1, 0x30000000);
        if (primary) {
            Fx2D.begin(g, true);
            float sweep = ((time * 0.45F) % 1.8F) - 0.4F;
            float sx = x + sweep * w;
            if (sx > x - 16 && sx < x + w + 16) {
                float l = Math.max(x, sx - 14), r = Math.min(x + w, sx + 14);
                Fx2D.rect(l, y + 1, r, y + h - 1, Fx2D.rgba(0xFFE6B0, 0), Fx2D.rgba(0xFFE6B0, 0.28F), Fx2D.rgba(0xFFE6B0, 0.28F), Fx2D.rgba(0xFFE6B0, 0));
            }
            Fx2D.end();
        }
        Fx2D.begin(g, false);
        Fx2D.diamond(x + 6, y + h / 2F, 2F, 2F, 0, GOLD_MID);
        Fx2D.diamond(x + w - 6, y + h / 2F, 2F, 2F, 0, GOLD_MID);
        Fx2D.end();
        RaidText.outlined(g, font, label, RaidText.SERIF, x + w / 2 - RaidText.w(font, label, RaidText.SERIF) / 2, y + (h - 9) / 2 + 1,
                primary ? 0xFFFFF1D6 : GOLD_HI, 0xC0000000);
    }

    /** 큰 제목: 빛무리 + 어두운 테두리 + 금빛 글자 (명조 제목 글꼴). */
    public static void luxTitle(GuiGraphics g, Font font, String text, int cx, int y, int rgb, float time) {
        int w = RaidText.w(font, text, RaidText.TITLE);
        Fx2D.begin(g, true);
        Fx2D.glow(cx, y + 7, w * 0.62F, 16, Fx2D.rgba(FIRE, 0.30F + 0.10F * (float) Math.sin(time * 1.8F)), Fx2D.rgba(FIRE, 0));
        Fx2D.end();
        // 핏빛 불빛 (고딕 필사본: 금 각인 + 붉은 빛)
        Fx2D.begin(g, true);
        Fx2D.glow(cx, y + 7, w * 0.5F, 12, Fx2D.rgba(0xC81E14, 0.26F + 0.08F * (float) Math.sin(time * 2.1F)), Fx2D.rgba(0xC81E14, 0));
        Fx2D.end();
        RaidText.engraved(g, font, text, RaidText.TITLE, cx - w / 2, y, 12);
    }

    /** 고딕 필사체 영문 장식 (제목 위의 작은 글씨) + 양옆 장식선. */
    public static void gothicBanner(GuiGraphics g, Font font, String text, int cx, int y, float time) {
        int w = RaidText.w(font, text, RaidText.GOTHIC);
        RaidText.draw(g, font, text, RaidText.GOTHIC, cx - w / 2 + 1, y + 1, 0xC0200806);
        RaidText.draw(g, font, text, RaidText.GOTHIC, cx - w / 2, y, 0xFFE8C078);
        hairline(g, cx - w / 2F - 46, cx - w / 2F - 6, y + 5, 0xD4A85A, 0.8F);
        hairline(g, cx + w / 2F + 6, cx + w / 2F + 46, y + 5, 0xD4A85A, 0.8F);
    }
}

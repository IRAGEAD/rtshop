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
}

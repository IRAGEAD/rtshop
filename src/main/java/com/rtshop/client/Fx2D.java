package com.rtshop.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

/**
 * 화면 연출용 2D 도형 (색만 있는 삼각형들): 빛 번짐, 고리, 호, 빛줄기, 선.
 * begin(g, additive) ... end() 사이에서 부른다. additive 면 겹칠수록 밝아진다(빛).
 * 색은 0xAARRGGBB.
 */
public final class Fx2D {
    private Fx2D() {
    }

    private static BufferBuilder buf;
    private static Matrix4f mat;

    public static void begin(GuiGraphics g, boolean additive) {
        g.flush();
        mat = new Matrix4f(g.pose().last().pose());
        RenderSystem.enableBlend();
        if (additive) {
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        } else {
            RenderSystem.defaultBlendFunc();
        }
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
    }

    public static void end() {
        if (buf == null) return;
        BufferUploader.drawWithShader(buf.end());
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        buf = null;
    }

    /** 그리던 도중 오류가 났을 때: 열어 둔 묶음을 닫고 그리기 상태를 되돌린다. */
    public static void abort() {
        try {
            end();
        } catch (RuntimeException ignored) {
            buf = null;
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
        }
    }

    private static void v(float x, float y, int c) {
        buf.vertex(mat, x, y, 0).color((c >> 16) & 255, (c >> 8) & 255, c & 255, (c >>> 24)).endVertex();
    }

    public static int alpha(int color, float a) {
        int al = Math.max(0, Math.min(255, (int) ((color >>> 24) * a)));
        return (al << 24) | (color & 0xFFFFFF);
    }

    public static int rgba(int rgb, float a) {
        int al = Math.max(0, Math.min(255, (int) (a * 255)));
        return (al << 24) | (rgb & 0xFFFFFF);
    }

    public static int mix(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int aa = (int) ((a >>> 24) + ((b >>> 24) - (a >>> 24)) * t);
        int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int gg = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bb = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return (aa << 24) | (r << 16) | (gg << 8) | bb;
    }

    public static void tri(float x0, float y0, int c0, float x1, float y1, int c1, float x2, float y2, int c2) {
        v(x0, y0, c0);
        v(x1, y1, c1);
        v(x2, y2, c2);
    }

    /** 네 귀퉁이 색이 다른 네모: (x0,y0) 왼쪽 위 ~ (x1,y1) 오른쪽 아래. */
    public static void rect(float x0, float y0, float x1, float y1, int cTL, int cTR, int cBR, int cBL) {
        tri(x0, y0, cTL, x1, y0, cTR, x1, y1, cBR);
        tri(x0, y0, cTL, x1, y1, cBR, x0, y1, cBL);
    }

    public static void rect(float x0, float y0, float x1, float y1, int c) {
        rect(x0, y0, x1, y1, c, c, c, c);
    }

    /** 가운데가 밝고 가장자리로 사라지는 원 (빛 번짐). */
    public static void glow(float cx, float cy, float r, int center, int edge) {
        glow(cx, cy, r, r, center, edge);
    }

    public static void glow(float cx, float cy, float rx, float ry, int center, int edge) {
        float big = Math.max(rx, ry);
        int n = big < 6 ? 8 : big < 30 ? 16 : 32;
        for (int i = 0; i < n; i++) {
            double a0 = i * Math.PI * 2 / n, a1 = (i + 1) * Math.PI * 2 / n;
            tri(cx, cy, center, cx + (float) Math.cos(a0) * rx, cy + (float) Math.sin(a0) * ry, edge,
                    cx + (float) Math.cos(a1) * rx, cy + (float) Math.sin(a1) * ry, edge);
        }
    }

    /** 고리의 일부: 각도 a0~a1 (도, 0 = 오른쪽, 시계 방향), 안쪽 반지름 r0, 바깥 r1. */
    public static void arc(float cx, float cy, float r0, float r1, float a0, float a1, int inner, int outer) {
        float sweep = a1 - a0;
        int n = Math.max(2, (int) (Math.abs(sweep) / 6));
        for (int i = 0; i < n; i++) {
            double t0 = Math.toRadians(a0 + sweep * i / n), t1 = Math.toRadians(a0 + sweep * (i + 1) / n);
            float c0 = (float) Math.cos(t0), s0 = (float) Math.sin(t0), c1 = (float) Math.cos(t1), s1 = (float) Math.sin(t1);
            tri(cx + c0 * r0, cy + s0 * r0, inner, cx + c0 * r1, cy + s0 * r1, outer, cx + c1 * r1, cy + s1 * r1, outer);
            tri(cx + c0 * r0, cy + s0 * r0, inner, cx + c1 * r1, cy + s1 * r1, outer, cx + c1 * r0, cy + s1 * r0, inner);
        }
    }

    public static void ring(float cx, float cy, float r0, float r1, int inner, int outer) {
        arc(cx, cy, r0, r1, 0, 360, inner, outer);
    }

    /** 가운데가 진하고 양쪽 가장자리가 사라지는 부드러운 고리 (충격파). */
    public static void softRing(float cx, float cy, float r, float width, int color) {
        int clear = color & 0xFFFFFF;
        arc(cx, cy, Math.max(0, r - width), r, 0, 360, clear, color);
        arc(cx, cy, r, r + width, 0, 360, color, clear);
    }

    /** 한 점에서 뻗는 빛줄기 (부채꼴): 방향 deg, 반각 half, 길이 len. 뿌리는 진하고 끝은 사라진다. */
    public static void ray(float cx, float cy, float deg, float half, float len, int root, int tip) {
        double a0 = Math.toRadians(deg - half), a1 = Math.toRadians(deg + half);
        tri(cx, cy, root, cx + (float) Math.cos(a0) * len, cy + (float) Math.sin(a0) * len, tip,
                cx + (float) Math.cos(a1) * len, cy + (float) Math.sin(a1) * len, tip);
    }

    /** 굵기가 있는 선. */
    public static void line(float x0, float y0, float x1, float y1, float width, int c0, int c1) {
        float dx = x1 - x0, dy = y1 - y0;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-4F) return;
        float nx = -dy / len * width / 2, ny = dx / len * width / 2;
        tri(x0 + nx, y0 + ny, c0, x1 + nx, y1 + ny, c1, x1 - nx, y1 - ny, c1);
        tri(x0 + nx, y0 + ny, c0, x1 - nx, y1 - ny, c1, x0 - nx, y0 - ny, c0);
    }

    /** 가운데가 진하고 옆으로 사라지는 빛나는 선. */
    public static void beam(float x0, float y0, float x1, float y1, float width, int color) {
        float dx = x1 - x0, dy = y1 - y0;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-4F) return;
        float nx = -dy / len * width / 2, ny = dx / len * width / 2;
        int clear = color & 0xFFFFFF;
        tri(x0, y0, color, x1, y1, color, x1 + nx, y1 + ny, clear);
        tri(x0, y0, color, x1 + nx, y1 + ny, clear, x0 + nx, y0 + ny, clear);
        tri(x0, y0, color, x1, y1, color, x1 - nx, y1 - ny, clear);
        tri(x0, y0, color, x1 - nx, y1 - ny, clear, x0 - nx, y0 - ny, clear);
    }

    /** 네 갈래 별 (반짝임). */
    public static void star(float cx, float cy, float r, float thick, int color) {
        int clear = color & 0xFFFFFF;
        for (int i = 0; i < 4; i++) {
            double a = Math.toRadians(i * 90);
            float tx = cx + (float) Math.cos(a) * r, ty = cy + (float) Math.sin(a) * r;
            float px = (float) -Math.sin(a) * thick, py = (float) Math.cos(a) * thick;
            tri(cx + px, cy + py, color, tx, ty, clear, cx - px, cy - py, color);
        }
    }

    /** 마름모 조각. */
    public static void diamond(float cx, float cy, float rx, float ry, float rotDeg, int color) {
        double a = Math.toRadians(rotDeg);
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        float ax = c * rx, ay = s * rx, bx = -s * ry, by = c * ry;
        tri(cx + ax, cy + ay, color, cx + bx, cy + by, color, cx - ax, cy - ay, color);
        tri(cx + ax, cy + ay, color, cx - ax, cy - ay, color, cx - bx, cy - by, color);
    }

    // ---- 난수 없이 시간으로만 정해지는 "무작위" (같은 시각이면 같은 그림 → 정지 화면 시험 가능)

    public static float hash(int i, int salt) {
        int h = i * 374761393 + salt * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }

    public static float ease(float t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    public static float easeOut(float t) {
        t = Math.max(0, Math.min(1, t));
        return 1 - (1 - t) * (1 - t) * (1 - t);
    }

    public static float easeIn(float t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * t;
    }

    /** 튀어 오르며 자리 잡기 (0→1, 중간에 1 을 조금 넘는다). */
    public static float pop(float t) {
        t = Math.max(0, Math.min(1, t));
        float c1 = 1.70158F, c3 = c1 + 1;
        return 1 + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
    }

    public static float clamp01(float t) {
        return Math.max(0, Math.min(1, t));
    }

    /** t 가 a~b 사이에서 0→1. */
    public static float span(float t, float a, float b) {
        return clamp01((t - a) / (b - a));
    }
}

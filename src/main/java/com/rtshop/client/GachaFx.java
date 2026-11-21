package com.rtshop.client;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 영웅·전설 등급이 나왔을 때의 연출 (화면 전체).
 * 영웅: 보랏빛 · 심장 박동 두세 번 · 빛줄기가 모이며 카드가 떨리다 터진다 (약 3초).
 * 전설: 완전히 어두워지고 빨라지는 심장 박동 · 처음엔 보랏빛(영웅인 척) → 천둥과 함께 금빛으로 '승급' →
 *       금빛 빛줄기와 불씨가 차오르다 크게 터진다 (약 6초). 클릭하면 건너뛴다.
 */
public final class GachaFx {
    public static final int EPIC = 3;
    public static final int LEGENDARY = 4;
    private static final int EPIC_LEN = 66;
    private static final int LEG_LEN = 124;
    private static final int EPIC_BURST = 42;
    private static final int LEG_UPGRADE = 54;
    private static final int LEG_BURST = 88;

    private static final int PURPLE = 0xB45AE8;
    private static final int PURPLE_LIGHT = 0xE6C2FF;
    private static final int GOLD = 0xFFC63A;
    private static final int GOLD_LIGHT = 0xFFF0B0;

    private static final class Spark {
        float x, y, px, py, vx, vy, drag, grav, size;
        int life, max, color;
    }

    private final RandomSource rnd = RandomSource.create();
    private final List<Spark> sparks = new ArrayList<>();
    private int grade = -1;
    private int t;
    private ItemStack stack = ItemStack.EMPTY;
    private String name = "";

    public void start(int grade, ItemStack stack, String name) {
        this.grade = grade;
        this.stack = stack;
        this.name = name;
        this.t = 0;
        sparks.clear();
        events(0);
    }

    public boolean active() {
        return grade >= EPIC && t < length();
    }

    public int grade() {
        return grade;
    }

    private int length() {
        return grade == LEGENDARY ? LEG_LEN : EPIC_LEN;
    }

    public void skip() {
        t = length();
        sparks.clear();
    }

    /** 점검용: 연출의 한 장면으로 바로 간다 (그 사이 파티클은 대충 만든다). */
    public void jumpTo(int tick) {
        sparks.clear();
        for (int i = 0; i <= tick; i++) {
            t = i;
            spawn(i);
            update();
        }
        t = tick;
    }

    public void tick() {
        if (!active()) {
            return;
        }
        t++;
        events(t);
        spawn(t);
        update();
    }

    private void update() {
        for (int i = sparks.size() - 1; i >= 0; i--) {
            Spark s = sparks.get(i);
            s.px = s.x;
            s.py = s.y;
            s.x += s.vx;
            s.y += s.vy;
            s.vx *= s.drag;
            s.vy = s.vy * s.drag + s.grav;
            if (++s.life >= s.max) {
                sparks.remove(i);
            }
        }
    }

    private static void sfx(SoundEvent e, float pitch, float vol) {
        try {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(e, pitch, vol));
        } catch (Throwable ignored) {
        }
    }

    /** 정해진 순간의 효과음. */
    private void events(int t) {
        if (grade == EPIC) {
            switch (t) {
                case 1 -> sfx(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.6F, 1.0F);
                case 6, 20 -> sfx(SoundEvents.WARDEN_HEARTBEAT, 1.0F, 1.0F);
                case 26 -> sfx(SoundEvents.WARDEN_SONIC_CHARGE, 1.6F, 0.5F);
                case 32 -> sfx(SoundEvents.WARDEN_HEARTBEAT, 1.15F, 1.0F);
                case EPIC_BURST -> {
                    sfx(SoundEvents.EVOKER_CAST_SPELL, 1.2F, 1.0F);
                    sfx(SoundEvents.AMETHYST_CLUSTER_BREAK, 0.8F, 1.0F);
                    sfx(SoundEvents.FIREWORK_ROCKET_BLAST, 1.0F, 0.8F);
                    burst(70, 3.5F, PURPLE, PURPLE_LIGHT, 0xFFFFFF);
                }
                case EPIC_BURST + 4 -> sfx(SoundEvents.PLAYER_LEVELUP, 1.3F, 0.8F);
                default -> {
                }
            }
        } else if (grade == LEGENDARY) {
            switch (t) {
                case 2 -> sfx(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.5F, 1.0F);
                case 8 -> sfx(SoundEvents.WARDEN_HEARTBEAT, 0.9F, 1.0F);
                case 20 -> sfx(SoundEvents.WARDEN_HEARTBEAT, 0.95F, 1.0F);
                case 30 -> {
                    sfx(SoundEvents.WARDEN_HEARTBEAT, 1.0F, 1.0F);
                    sfx(SoundEvents.WARDEN_SONIC_CHARGE, 0.8F, 0.6F);
                }
                case 38 -> sfx(SoundEvents.WARDEN_HEARTBEAT, 1.05F, 1.0F);
                case 44 -> sfx(SoundEvents.WARDEN_HEARTBEAT, 1.1F, 1.0F);
                case 49 -> sfx(SoundEvents.WARDEN_HEARTBEAT, 1.15F, 1.0F);
                case LEG_UPGRADE -> {
                    sfx(SoundEvents.LIGHTNING_BOLT_THUNDER, 1.0F, 0.8F);
                    sfx(SoundEvents.LIGHTNING_BOLT_IMPACT, 1.0F, 1.0F);
                    burst(40, 2.5F, GOLD, 0xFFFFFF, PURPLE);
                }
                case LEG_UPGRADE + 6 -> sfx(SoundEvents.BEACON_ACTIVATE, 1.2F, 1.0F);
                case 72 -> sfx(SoundEvents.BELL_BLOCK, 1.0F, 1.0F);
                case 80 -> sfx(SoundEvents.BELL_BLOCK, 1.26F, 1.0F);
                case 84 -> sfx(SoundEvents.WARDEN_SONIC_CHARGE, 1.8F, 0.6F);
                case LEG_BURST -> {
                    sfx(SoundEvents.TOTEM_USE, 1.0F, 0.9F);
                    sfx(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
                    sfx(SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.0F, 1.0F);
                    burst(140, 5.0F, GOLD, GOLD_LIGHT, 0xFFFFFF);
                }
                case LEG_BURST + 6 -> sfx(SoundEvents.FIREWORK_ROCKET_TWINKLE, 1.0F, 1.0F);
                case LEG_BURST + 14 -> sfx(SoundEvents.FIREWORK_ROCKET_TWINKLE_FAR, 1.2F, 1.0F);
                default -> {
                }
            }
        }
    }

    /** 계속 생기는 파티클 (모여드는 빛, 금빛 불씨, 떨어지는 색종이). */
    private void spawn(int t) {
        if (grade == EPIC) {
            if (t >= 10 && t < EPIC_BURST && t % 2 == 0) {
                converge(PURPLE, 2);
            }
            if (t > EPIC_BURST && t % 3 == 0) {
                rise(PURPLE_LIGHT, 1);
            }
        } else if (grade == LEGENDARY) {
            if (t >= 12 && t < LEG_UPGRADE && t % 2 == 0) {
                converge(PURPLE, 2);
            }
            if (t >= LEG_UPGRADE && t < LEG_BURST) {
                converge(GOLD, 3);
                rise(GOLD, 2);
            }
            if (t > LEG_BURST) {
                confetti(2);
                if (t % 2 == 0) {
                    rise(GOLD_LIGHT, 1);
                }
            }
        }
    }

    private Spark add(float x, float y, float vx, float vy, int life, int color, float size) {
        Spark s = new Spark();
        s.x = s.px = x;
        s.y = s.py = y;
        s.vx = vx;
        s.vy = vy;
        s.max = life;
        s.color = color;
        s.size = size;
        s.drag = 1.0F;
        sparks.add(s);
        return s;
    }

    private void converge(int color, int n) {
        for (int i = 0; i < n; i++) {
            float a = rnd.nextFloat() * Mth.TWO_PI;
            float r = 110 + rnd.nextFloat() * 60;
            int life = 16 + rnd.nextInt(6);
            float x = Mth.cos(a) * r;
            float y = Mth.sin(a) * r;
            add(x, y, -x / life, -y / life, life, color, 1.5F + rnd.nextFloat());
        }
    }

    private void rise(int color, int n) {
        for (int i = 0; i < n; i++) {
            Spark s = add((rnd.nextFloat() - 0.5F) * 260, 90 + rnd.nextFloat() * 30, (rnd.nextFloat() - 0.5F) * 0.4F,
                    -1.2F - rnd.nextFloat() * 1.5F, 40 + rnd.nextInt(30), color, 1 + rnd.nextFloat() * 1.5F);
            s.drag = 0.995F;
        }
    }

    private void confetti(int n) {
        int[] cols = {GOLD, GOLD_LIGHT, 0xFFFFFF, 0xFF8A3A, 0xFFD24A};
        for (int i = 0; i < n; i++) {
            Spark s = add((rnd.nextFloat() - 0.5F) * 360, -130 - rnd.nextFloat() * 20, (rnd.nextFloat() - 0.5F) * 0.8F,
                    0.6F + rnd.nextFloat() * 0.8F, 90, cols[rnd.nextInt(cols.length)], 1.5F + rnd.nextFloat() * 1.5F);
            s.grav = 0.01F;
        }
    }

    private void burst(int n, float speed, int... colors) {
        for (int i = 0; i < n; i++) {
            float a = rnd.nextFloat() * Mth.TWO_PI;
            float v = speed * (0.4F + rnd.nextFloat());
            Spark s = add(0, 0, Mth.cos(a) * v, Mth.sin(a) * v, 26 + rnd.nextInt(24), colors[rnd.nextInt(colors.length)],
                    1.5F + rnd.nextFloat() * 2);
            s.drag = 0.94F;
            s.grav = 0.03F;
        }
    }

    // ------------------------------------------------------------------ 그리기

    private static int argb(int rgb, float a) {
        return (Mth.clamp((int) (a * 255), 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    private static int lerpRgb(int a, int b, float f) {
        f = Mth.clamp(f, 0, 1);
        int r = (int) Mth.lerp(f, (a >> 16) & 255, (b >> 16) & 255);
        int g = (int) Mth.lerp(f, (a >> 8) & 255, (b >> 8) & 255);
        int bl = (int) Mth.lerp(f, a & 255, b & 255);
        return (r << 16) | (g << 8) | bl;
    }

    /** 가운데가 밝은 빛 무리 (겹친 사각형 두 겹: 0도·45도). */
    private static void glow(GuiGraphics g, float r, int rgb, float alpha) {
        int steps = 7;
        for (int k = steps; k >= 1; k--) {
            float rr = r * k / steps;
            float a = alpha * 0.22F;
            int c = argb(rgb, a);
            g.fill((int) -rr, (int) -rr, (int) rr, (int) rr, c);
            g.pose().pushPose();
            g.pose().mulPose(Axis.ZP.rotationDegrees(45));
            g.fill((int) -rr, (int) -rr, (int) rr, (int) rr, c);
            g.pose().popPose();
        }
    }

    private static void rays(GuiGraphics g, int n, float angle, float len, float width, int rgb, float alpha) {
        for (int i = 0; i < n; i++) {
            g.pose().pushPose();
            g.pose().mulPose(Axis.ZP.rotationDegrees(angle + i * 360.0F / n));
            g.fillGradient((int) -width, 8, (int) width, (int) len, argb(rgb, alpha), argb(rgb, 0));
            g.pose().popPose();
        }
    }

    /** 뒤집히기 전의 카드 (뒷면). */
    private static void cardBack(GuiGraphics g, Font font, int trim, float crack, int crackRgb) {
        int w = 26;
        int h = 36;
        g.fill(-w - 2, -h - 2, w + 2, h + 2, 0xFF140B05);
        g.fill(-w, -h, w, h, 0xFF3E2512);
        g.fill(-w + 2, -h + 2, w - 2, h - 2, 0xFF5C3A1E);
        g.fill(-w + 4, -h + 4, w - 4, -h + 6, 0xFF000000 | trim);
        g.fill(-w + 4, h - 6, w - 4, h - 4, 0xFF000000 | trim);
        g.pose().pushPose();
        g.pose().translate(0, 0, 10);
        g.pose().scale(2.5F, 2.5F, 1);
        g.drawCenteredString(font, "?", 0, -4, 0xFF000000 | trim);
        g.pose().popPose();
        if (crack > 0) {
            // 금이 가는 선
            int c = 0xFF000000 | crackRgb;
            int[][] lines = {{0, -h, -6, -14}, {-6, -14, 4, 2}, {4, 2, -3, 18}, {-3, 18, 2, h}, {4, 2, 18, 10}, {-6, -14, -20, -22}};
            int shown = (int) Math.ceil(crack * lines.length);
            for (int i = 0; i < shown && i < lines.length; i++) {
                line(g, lines[i][0], lines[i][1], lines[i][2], lines[i][3], c);
            }
        }
    }

    private static void line(GuiGraphics g, int x0, int y0, int x1, int y1, int c) {
        int n = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        for (int i = 0; i <= n; i++) {
            int x = x0 + (x1 - x0) * i / Math.max(1, n);
            int y = y0 + (y1 - y0) * i / Math.max(1, n);
            g.fill(x, y, x + 1, y + 1, c);
        }
    }

    public void render(GuiGraphics g, Font font, int width, int height, float partial) {
        if (!active()) {
            return;
        }
        float tt = t + partial;
        boolean leg = grade == LEGENDARY;
        int burstAt = leg ? LEG_BURST : EPIC_BURST;
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        // 어둠
        float dark = Mth.clamp(tt / (leg ? 14F : 10F), 0, 1);
        g.fill(0, 0, width, height, argb(leg ? 0x050302 : 0x0A0614, dark * (leg ? 0.96F : 0.86F)));
        int cx = width / 2;
        int cy = height / 2 - 6;
        // 화면 흔들림 (승급 천둥 · 대폭발)
        float shake = 0;
        if (leg && tt >= LEG_UPGRADE && tt < LEG_UPGRADE + 10) {
            shake = 4 * (1 - (tt - LEG_UPGRADE) / 10F);
        }
        if (tt >= burstAt && tt < burstAt + 8) {
            shake = Math.max(shake, (leg ? 5 : 3) * (1 - (tt - burstAt) / 8F));
        }
        float sx = shake > 0 ? (rnd.nextFloat() - 0.5F) * 2 * shake : 0;
        float sy = shake > 0 ? (rnd.nextFloat() - 0.5F) * 2 * shake : 0;
        g.pose().pushPose();
        g.pose().translate(cx + sx, cy + sy, 0);

        boolean gold = leg && tt >= LEG_UPGRADE;
        int mainRgb = gold ? GOLD : PURPLE;
        int lightRgb = gold ? GOLD_LIGHT : PURPLE_LIGHT;
        if (tt < burstAt) {
            // 모으는 단계: 빛 무리가 박동에 맞춰 커지고, 빛줄기가 돌며 길어지고, 카드가 떨린다
            float beat = heartbeat(tt, leg);
            float charge = Mth.clamp((tt - 8) / (burstAt - 8F), 0, 1);
            glow(g, 34 + charge * 60 + beat * 14, mainRgb, 0.35F + charge * 0.45F + beat * 0.2F);
            if (tt > (leg ? 20 : 22)) {
                float ra = tt * (gold ? 4.5F : 3F);
                rays(g, gold ? 16 : 10, ra, 40 + charge * 150, gold ? 2.5F : 2, lightRgb, 0.25F + charge * 0.5F);
                if (gold) {
                    rays(g, 8, -ra * 0.7F, 30 + charge * 110, 4, mainRgb, 0.3F);
                }
            }
            float amp = charge * charge * (leg ? 4.5F : 3.0F) + beat * 1.5F;
            float ox = (rnd.nextFloat() - 0.5F) * 2 * amp;
            float oy = (rnd.nextFloat() - 0.5F) * 2 * amp;
            float lift = Mth.sin(tt * 0.15F) * 2;
            float sc = 1 + (gold ? (tt - LEG_UPGRADE) / (float) (LEG_BURST - LEG_UPGRADE) * 0.35F : charge * 0.12F) + beat * 0.05F;
            g.pose().pushPose();
            g.pose().translate(ox, oy + lift, 20);
            g.pose().scale(sc, sc, 1);
            float crack = gold ? Mth.clamp((tt - LEG_UPGRADE) / 24F, 0.2F, 1) : (!leg && tt > 30 ? (tt - 30) / 12F : 0);
            cardBack(g, font, lightRgb, crack, gold ? 0xFFF6C0 : PURPLE_LIGHT);
            g.pose().popPose();
        } else {
            // 공개
            float since = tt - burstAt;
            glow(g, 90 + Mth.sin(tt * 0.2F) * 6, mainRgb, 0.55F);
            rays(g, leg ? 18 : 12, tt * (leg ? 1.6F : 1.2F), leg ? 260 : 190, leg ? 3 : 2, lightRgb, 0.45F);
            if (leg) {
                rays(g, 9, -tt * 1.1F, 200, 6, GOLD, 0.25F);
                // 주위를 도는 별
                for (int i = 0; i < 6; i++) {
                    float a = tt * 0.08F + i * Mth.TWO_PI / 6;
                    int x = (int) (Mth.cos(a) * 58);
                    int y = (int) (Mth.sin(a) * 58);
                    g.pose().pushPose();
                    g.pose().translate(x, y, 30);
                    g.pose().mulPose(Axis.ZP.rotationDegrees(45 + tt * 6));
                    g.fill(-3, -3, 3, 3, 0xFFFFF4C8);
                    g.pose().popPose();
                }
            }
            float pop = since < 10 ? 1 + 0.6F * (1 - since / 10F) * Mth.cos(since * 0.9F) : 1;
            float sc = (leg ? 4.5F : 4F) * pop;
            float bob = Mth.sin(tt * 0.12F) * 2;
            g.pose().pushPose();
            g.pose().translate(-8 * sc, -8 * sc - 8 + bob, 40);
            g.pose().scale(sc, sc, 1);
            g.renderItem(stack, 0, 0);
            g.pose().popPose();
            // 등급 글자
            g.flush();
            g.pose().pushPose();
            g.pose().translate(0, 50, 400);
            float ts = (leg ? 3F : 2.5F) * (since < 8 ? 1 + 0.5F * (1 - since / 8F) : 1);
            g.pose().scale(ts, ts, 1);
            String label = Component.translatable(leg ? "rtshop.gacha.fx.legendary" : "rtshop.gacha.fx.epic").getString();
            int col = leg ? lerpRgb(GOLD, 0xFFFFFF, 0.5F + 0.5F * Mth.sin(tt * 0.35F)) : PURPLE_LIGHT;
            g.drawString(font, label, -font.width(label) / 2, 0, 0xFF000000 | col, true);
            g.pose().popPose();
            g.pose().pushPose();
            g.pose().translate(0, 50 + 9 * ts + 6, 400);
            g.pose().scale(1.5F, 1.5F, 1);
            g.drawString(font, name, -font.width(name) / 2, 0, 0xFFFFFFFF, true);
            g.pose().popPose();
        }
        // 파티클
        g.pose().pushPose();
        g.pose().translate(0, 0, 350);
        for (Spark s : sparks) {
            float x = Mth.lerp(partial, s.px, s.x);
            float y = Mth.lerp(partial, s.py, s.y);
            float a = 1 - (float) s.life / s.max;
            int sz = Math.max(1, Math.round(s.size));
            g.fill((int) x, (int) y, (int) x + sz, (int) y + sz, argb(s.color, Math.min(1, a * 1.3F)));
        }
        g.pose().popPose();
        g.pose().popPose();
        // 번쩍임 (승급 · 폭발)
        float flash = 0;
        int flashRgb = 0xFFFFFF;
        if (leg && tt >= LEG_UPGRADE && tt < LEG_UPGRADE + 6) {
            flash = 0.9F * (1 - (tt - LEG_UPGRADE) / 6F);
        }
        if (tt >= burstAt && tt < burstAt + 10) {
            flash = Math.max(flash, 1 - (tt - burstAt) / 10F);
            flashRgb = leg ? 0xFFF3C8 : 0xF0DDFF;
        }
        if (flash > 0) {
            g.fill(0, 0, width, height, argb(flashRgb, flash));
        }
        String skip = Component.translatable("rtshop.gacha.fx.skip").getString();
        g.drawString(font, skip, width - font.width(skip) - 6, height - 12, 0x88FFFFFF, false);
        g.pose().popPose();
    }

    /** 심장 박동 순간에 1 로 튀었다가 빠르게 줄어드는 값. */
    private static float heartbeat(float tt, boolean leg) {
        int[] beats = leg ? new int[]{8, 20, 30, 38, 44, 49} : new int[]{6, 20, 32};
        float v = 0;
        for (int b : beats) {
            float d = tt - b;
            if (d >= 0 && d < 6) {
                v = Math.max(v, 1 - d / 6F);
            }
        }
        return v;
    }
}

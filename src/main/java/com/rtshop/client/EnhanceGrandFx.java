package com.rtshop.client;

import com.mojang.math.Axis;
import com.rtshop.enhance.Enhance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 높은 단계(기본 +7 이상) 강화의 연출.
 *  1) 긴장 (약 3.4초): 화면이 어두워지고 장비만 비춘다. 심장 소리가 점점 빨라지고, 장비가 떨리며 불꽃이 튄다. 마지막엔 잠깐 정적.
 *  2) 결과: 성공이면 흰 섬광 → 회전하는 금빛 광선, 화면 흔들림, 폭죽·팡파르, 떨어지는 색종이, 커다란 "+N 강화 성공!".
 *     실패(유지·손상·파괴)면 어둠이 걷히고 평소 결과 표시.
 * 결과는 서버가 이미 정했고, 화면에서만 공개를 늦춘다.
 */
public final class EnhanceGrandFx {
    private EnhanceGrandFx() {
    }

    public static final long SUSPENSE = 3400;
    public static final long REVEAL = 5200;

    private static long startAt;
    private static boolean running;
    private static boolean revealed;
    private static Enhance.Outcome outcome;
    private static boolean guarded;
    private static int from, to;
    private static ItemStack item = ItemStack.EMPTY;
    private static final long[] BEATS = {0, 650, 1200, 1680, 2080, 2400, 2660, 2860, 3020};
    private static int beatIdx;
    private static final long[] HITS = {350, 1350, 2250};
    private static int hitIdx;
    private static int revealSoundStep;
    private static final Random RND = new Random();
    private static final List<float[]> CONFETTI = new ArrayList<>();
    /** 시험용: 마지막으로 시작한 연출. */
    public static volatile String last = "";

    public static boolean running() {
        return running;
    }

    public static boolean suspense() {
        return running && !revealed;
    }

    public static void start(Enhance.Outcome o, boolean g, int f, int t, ItemStack it) {
        outcome = o;
        guarded = g;
        from = f;
        to = t;
        item = it.copy();
        startAt = System.currentTimeMillis();
        running = true;
        revealed = false;
        beatIdx = 0;
        hitIdx = 0;
        revealSoundStep = 0;
        CONFETTI.clear();
        last = o.name() + ":" + f + ":" + t;
        play(SoundEvents.BEACON_ACTIVATE, 0.6F, 0.7F);
    }

    private static void play(SoundEvent s, float pitch, float vol) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(s, pitch, vol));
    }

    private static void tickSounds(long t) {
        while (beatIdx < BEATS.length && t >= BEATS[beatIdx]) {
            float k = beatIdx / (float) BEATS.length;
            play(SoundEvents.WARDEN_HEARTBEAT, 0.9F + k * 0.5F, 0.7F + k * 0.6F);
            beatIdx++;
        }
        while (hitIdx < HITS.length && t >= HITS[hitIdx]) {
            play(SoundEvents.ANVIL_PLACE, 0.6F + hitIdx * 0.25F, 0.35F + hitIdx * 0.1F);
            play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F + hitIdx * 0.3F, 0.8F);
            hitIdx++;
        }
    }

    private static void reveal() {
        revealed = true;
        Minecraft mc = Minecraft.getInstance();
        if (outcome == Enhance.Outcome.SUCCESS) {
            play(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
            play(SoundEvents.PLAYER_LEVELUP, 0.8F, 1.0F);
            play(SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.0F, 1.0F);
            if (to >= 10) play(SoundEvents.TOTEM_USE, 1.0F, 0.9F);
            if (mc.player != null) {
                mc.particleEngine.createTrackingEmitter(mc.player, ParticleTypes.TOTEM_OF_UNDYING, 40);
                mc.particleEngine.createTrackingEmitter(mc.player, ParticleTypes.FIREWORK, 30);
                if (to >= 10) mc.particleEngine.createTrackingEmitter(mc.player, ParticleTypes.END_ROD, 40);
            }
            for (int i = 0; i < 140; i++) {
                // x(0~1), y(-0.3~0), vx, vy, 회전, 색
                CONFETTI.add(new float[]{RND.nextFloat(), -RND.nextFloat() * 0.35F, (RND.nextFloat() - 0.5F) * 0.12F,
                        0.12F + RND.nextFloat() * 0.25F, RND.nextFloat() * 360, RND.nextInt(6)});
            }
        }
        // 결과 글자·채팅은 평소 표시를 쓴다 (성공이면 아래 그리기에서 더 크게 덮어 그림)
        EnhanceFx.showNow(outcome, guarded, from, to, item, outcome == Enhance.Outcome.SUCCESS);
    }

    private static final int[] CONF = {0xFFFFD54A, 0xFFFF6A6A, 0xFF6AD1FF, 0xFF8CFF7A, 0xFFE08BFF, 0xFFFFFFFF};

    public static void render(GuiGraphics g, int w, int h) {
        if (!running) return;
        long t = System.currentTimeMillis() - startAt;
        if (!revealed) {
            tickSounds(t);
            if (t >= SUSPENSE) reveal();
        }
        boolean success = outcome == Enhance.Outcome.SUCCESS;
        long rt = t - SUSPENSE;
        if (revealed && (!success || rt > REVEAL)) {
            if (!success && rt > 500) running = false;
            if (success && rt > REVEAL) running = false;
        }
        Font font = Minecraft.getInstance().font;
        g.pose().pushPose();
        g.pose().translate(0, 0, 950);
        float cx = w / 2F, cy = h * 0.42F;
        if (!revealed) {
            float k = Math.min(1F, t / (float) SUSPENSE);
            // 어둠 + 가운데만 밝은 테두리 그림자
            int dark = (int) (Mth.clamp(t / 500F, 0, 1) * 200);
            g.fill(0, 0, w, h, dark << 24);
            for (int i = 0; i < 6; i++) {
                int pad = (int) (i * Math.min(w, h) * 0.06F);
                g.fill(0, 0, w, pad, 0x22000000);
                g.fill(0, h - pad, w, h, 0x22000000);
            }
            // 빛나는 원판 (가운데 스포트라이트)
            int glow = (int) (60 + 80 * k);
            for (int r = 50; r > 0; r -= 5) {
                int a = (int) (glow * (1 - r / 55F));
                g.fill((int) (cx - r), (int) (cy - r * 0.7F), (int) (cx + r), (int) (cy + r * 0.7F), (a << 24) | 0xFFD27A);
            }
            // 떨리는 장비 (점점 세게)
            float shake = k * k * 3.2F;
            float sx = (RND.nextFloat() - 0.5F) * shake, sy = (RND.nextFloat() - 0.5F) * shake;
            float sc = 3.2F + k * 0.8F + (float) Math.sin(t / 90.0) * 0.08F * k;
            if (t > SUSPENSE - 220) {
                sx = 0;
                sy = 0; // 마지막 정적
            }
            g.pose().pushPose();
            g.pose().translate(cx + sx, cy + sy, 0);
            g.pose().mulPose(Axis.ZP.rotationDegrees((float) Math.sin(t / 130.0) * 6 * k));
            g.pose().scale(sc, sc, 1);
            g.renderItem(item, -8, -8);
            g.pose().popPose();
            // 튀는 불꽃
            int sparks = (int) (6 + 30 * k);
            for (int i = 0; i < sparks; i++) {
                double ang = RND.nextDouble() * Math.PI * 2;
                double dist = 30 + RND.nextDouble() * (40 + 50 * k);
                int x = (int) (cx + Math.cos(ang) * dist), y = (int) (cy + Math.sin(ang) * dist * 0.8);
                g.fill(x, y, x + 1 + RND.nextInt(2), y + 1, 0xFFFFC040 | (RND.nextInt(80) << 8));
            }
            // 글자: "+N 강화 중" 과 점점 차오르는 게이지
            Component label = Component.translatable("rtshop.enhance.grand_trying", from + 1);
            String dots = ".".repeat(1 + (int) ((t / 300) % 3));
            String s = label.getString() + dots;
            float ts = 1.8F;
            g.pose().pushPose();
            g.pose().translate(cx, cy + 54, 0);
            g.pose().scale(ts, ts, 1);
            g.drawString(font, s, -font.width(label.getString()) / 2, 0, 0xFFFFE6A0, true);
            g.pose().popPose();
            int bw = 160, bx = (int) (cx - bw / 2F), by = (int) (cy + 78);
            g.fill(bx - 1, by - 1, bx + bw + 1, by + 5, 0xFF000000);
            g.fill(bx, by, bx + (int) (bw * Math.min(1, k * 1.04F)), by + 4, k > 0.85F ? 0xFFFFFFFF : 0xFFFFB020);
            // 마지막 순간: 하얗게 차오름
            if (t > SUSPENSE - 220) {
                int a = (int) ((t - (SUSPENSE - 220)) / 220F * 120);
                g.fill(0, 0, w, h, (Mth.clamp(a, 0, 255) << 24) | 0xFFFFFF);
            }
        } else if (success && rt <= REVEAL) {
            // 흰 섬광
            if (rt < 500) {
                int a = (int) (255 * (1 - rt / 500F));
                g.fill(0, 0, w, h, (Mth.clamp(a, 0, 255) << 24) | 0xFFFFFF);
            }
            float fade = rt > REVEAL - 700 ? (REVEAL - rt) / 700F : 1F;
            int dark = (int) (150 * fade);
            g.fill(0, 0, w, h, Mth.clamp(dark, 0, 255) << 24);
            // 흔들림
            float shake = rt < 700 ? (700 - rt) / 700F * 6 : 0;
            g.pose().pushPose();
            g.pose().translate((RND.nextFloat() - 0.5F) * shake, (RND.nextFloat() - 0.5F) * shake, 0);
            // 회전하는 광선
            int rayColor = to >= 10 ? 0xFF77FF : 0xFFC83C;
            int rays = 18;
            float rot = rt / 25F;
            float len = Math.min(w, h) * 0.9F;
            for (int i = 0; i < rays; i++) {
                g.pose().pushPose();
                g.pose().translate(cx, cy, 0);
                g.pose().mulPose(Axis.ZP.rotationDegrees(rot + i * (360F / rays)));
                int a = (int) ((i % 2 == 0 ? 110 : 60) * fade);
                g.fill(-3, 0, 3, (int) len, (Mth.clamp(a, 0, 255) << 24) | rayColor);
                g.fill(-1, 0, 1, (int) len, (Mth.clamp(a + 40, 0, 255) << 24) | 0xFFFFFF);
                g.pose().popPose();
            }
            // 빛 고리
            float ring = Math.min(1F, rt / 600F);
            int rr = (int) (20 + ring * 90);
            int ra = (int) (200 * (1 - ring) * fade);
            for (int i = 0; i < 24; i++) {
                double ang = i * Math.PI * 2 / 24;
                int x = (int) (cx + Math.cos(ang) * rr), y = (int) (cy + Math.sin(ang) * rr * 0.8);
                g.fill(x - 2, y - 2, x + 2, y + 2, (Mth.clamp(ra, 0, 255) << 24) | 0xFFFFFF);
            }
            // 장비 (커졌다 자리 잡음)
            float pop = rt < 350 ? 5.5F - 1.5F * (rt / 350F) : 4F + (float) Math.sin(rt / 220.0) * 0.12F;
            g.pose().pushPose();
            g.pose().translate(cx, cy, 0);
            g.pose().scale(pop, pop, 1);
            g.renderItem(item, -8, -8);
            g.pose().popPose();
            // 글자 뒤 어두운 띠 (빛줄기 위에서도 읽히게)
            int bandA = (int) (150 * Math.max(0.15F, fade));
            int bandTop = (int) (cy + 40), bandBot = (int) (cy + 108);
            for (int i = 0; i < 6; i++) {
                int a = bandA * (i + 1) / 6;
                g.fill(0, bandTop - 6 + i, w, bandTop - 5 + i, a << 24);
                g.fill(0, bandBot + 5 - i, w, bandBot + 6 - i, a << 24);
            }
            g.fill(0, bandTop, w, bandBot, bandA << 24);
            // 큰 글자
            Component big = Component.translatable("rtshop.enhance.grand_success", to);
            float ts = rt < 300 ? 5F - 2F * (rt / 300F) : 3F;
            int textColor = ((int) (255 * Math.max(0.15F, fade)) << 24) | (to >= 10 ? 0xFF8CFF : 0xFFD54A);
            g.pose().pushPose();
            g.pose().translate(cx, cy + 46, 0);
            g.pose().scale(ts, ts, 1);
            g.drawString(font, big, -font.width(big) / 2, 0, textColor, true);
            g.pose().popPose();
            Component nm = item.getHoverName();
            g.drawString(font, nm, (int) (cx - font.width(nm) / 2F), (int) (cy + 84), ((int) (255 * Math.max(0.15F, fade)) << 24) | 0xFFFFFF, true);
            Component ann = Component.translatable("rtshop.enhance.grand_announced");
            g.drawString(font, ann, (int) (cx - font.width(ann) / 2F), (int) (cy + 96), ((int) (220 * Math.max(0.15F, fade)) << 24) | 0xFFE6A0, true);
            g.pose().popPose();
            // 색종이
            float dt = 1 / 60F;
            for (float[] c : CONFETTI) {
                c[0] += c[2] * dt;
                c[1] += c[3] * dt;
                c[4] += 6;
                int x = (int) (c[0] * w), y = (int) (c[1] * h);
                if (y > h) continue;
                g.pose().pushPose();
                g.pose().translate(x, y, 0);
                g.pose().mulPose(Axis.ZP.rotationDegrees(c[4]));
                int col = CONF[(int) c[5]];
                int a = (int) (255 * fade);
                g.fill(-2, -1, 2, 1, (Mth.clamp(a, 0, 255) << 24) | (col & 0xFFFFFF));
                g.pose().popPose();
            }
            // 반짝이 소리 (한 번씩)
            if (revealSoundStep == 0 && rt > 450) {
                play(SoundEvents.FIREWORK_ROCKET_TWINKLE, 1.0F, 0.9F);
                revealSoundStep = 1;
            } else if (revealSoundStep == 1 && rt > 1100) {
                play(SoundEvents.FIREWORK_ROCKET_TWINKLE_FAR, 1.2F, 0.9F);
                revealSoundStep = 2;
            } else if (revealSoundStep == 2 && rt > 1700) {
                play(SoundEvents.FIREWORK_ROCKET_BLAST_FAR, 0.9F, 0.8F);
                revealSoundStep = 3;
            }
        } else if (!success && rt < 500) {
            int a = (int) (200 * (1 - rt / 500F));
            g.fill(0, 0, w, h, Mth.clamp(a, 0, 255) << 24);
        }
        g.pose().popPose();
    }
}

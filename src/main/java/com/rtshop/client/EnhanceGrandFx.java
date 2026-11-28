package com.rtshop.client;

import com.mojang.math.Axis;
import com.rtshop.enhance.Enhance;
import com.rtshop.enhance.EnhanceConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * 높은 단계 강화의 연출. 결과는 서버가 이미 정했고, 화면에서만 공개를 늦춘다. 단계 묶음마다 전혀 다른 장면이다.
 *
 *  1) 단조 (+7~+8, 긴장 3.0초): 달궈진 모루 위에서 세 번의 망치질. 내려칠 때마다 불꽃이 튀고 표식이 하나씩 켜진다.
 *     마지막엔 하얗게 달아오른 채 숨을 멈춘다 → 성공이면 금빛 햇살과 불티.
 *  2) 각인 (+9~+10, 긴장 4.2초): 마법진이 스스로 그려지고 여섯 봉인이 하나씩 켜지며 빛줄기가 모인다.
 *     마법진이 장비 속으로 빨려 들어간 뒤 완전한 어둠 → 성공이면 빛기둥과 수정 조각.
 *  3) 초월 (+11, 긴장 8.2초): 화면이 닫히고, 어둠 속에서 열 개의 별이 차례로 켜진다. 열한 번째 별은 세 번이나 꺼지고,
 *     장비에 금이 가며 모든 것이 한 점으로 무너진 뒤 긴 정적 → 성공이면 새벽빛·무지개 고리·세 번의 충격파와 "+11".
 *     파괴면 장비가 조각나 흩어진다.
 *
 * 모든 그림은 "시작 후 몇 ms" 만으로 정해진다 (난수·누적 상태 없음). 그래서 시험에서 정지 화면을 찍을 수 있다.
 */
public final class EnhanceGrandFx {
    private EnhanceGrandFx() {
    }

    public static final int T_FORGE = 1, T_RUNE = 2, T_FINAL = 3;

    private static long startAt;
    private static boolean running;
    private static boolean revealed;
    private static boolean bannerPending;
    private static Enhance.Outcome outcome = Enhance.Outcome.MAINTAIN;
    private static boolean guarded;
    private static int from, to, tier = 1;
    private static ItemStack item = ItemStack.EMPTY;
    private static int cueIdx, revealCueIdx;
    /** 시험용: 0 이상이면 그 시각(ms)의 장면에서 멈춘다. */
    private static long frozen = -1;
    /** 시험용: 마지막으로 시작한 연출. */
    public static volatile String last = "";

    public static boolean running() {
        return running;
    }

    public static boolean suspense() {
        return running && !revealed;
    }

    /** 평소 결과 띠를 아직 그리면 안 되는가 (긴장 중이거나, 실패 장면이 끝나기 전). */
    public static boolean hidesBanner() {
        return running && (!revealed || bannerPending);
    }

    public static int tier() {
        return tier;
    }

    public static long revealMs(int tier, boolean success) {
        if (success) return tier == T_FINAL ? 8500 : tier == T_RUNE ? 5200 : 4200;
        return tier == T_FINAL ? 2400 : tier == T_RUNE ? 1500 : 950;
    }

    private static long bannerDelay(int tier) {
        return tier == T_FINAL ? 900 : tier == T_RUNE ? 520 : 280;
    }

    public static void start(Enhance.Outcome o, boolean g, int f, int t, ItemStack it) {
        outcome = o;
        guarded = g;
        from = f;
        to = t;
        item = it.copy();
        tier = Math.max(1, Enhance.fxTier(f + 1, EnhanceConfig.client().broadcastFrom));
        startAt = System.currentTimeMillis();
        running = true;
        revealed = false;
        bannerPending = false;
        cueIdx = 0;
        revealCueIdx = 0;
        frozen = -1;
        last = o.name() + ":" + f + ":" + t;
    }

    /** 시험용: target 단계 도전의 연출을 atMs 시각에서 멈춘 채 보여 준다 (소리 없음). */
    public static void freezeForTest(int target, Enhance.Outcome o, long atMs, ItemStack it) {
        outcome = o;
        guarded = false;
        from = target - 1;
        to = o == Enhance.Outcome.SUCCESS ? target : target - 1;
        item = it.copy();
        tier = Math.max(1, Enhance.fxTier(target, 7));
        running = true;
        frozen = atMs;
        revealed = atMs >= Enhance.suspenseMs(tier);
        bannerPending = false;
        last = "FROZEN:" + target + ":" + o.name() + ":" + atMs;
    }

    public static void stopForTest() {
        running = false;
        frozen = -1;
    }

    // ------------------------------------------------------------------ 소리

    private record Cue(long at, SoundEvent sound, float pitch, float vol) {
    }

    private static void play(SoundEvent s, float pitch, float vol) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(s, pitch, vol));
    }

    private static Cue c(long at, SoundEvent s, double pitch, double vol) {
        return new Cue(at, s, (float) pitch, (float) vol);
    }

    private static final long[] STRIKES = {620, 1420, 2220};
    private static final long[] NODES = {1200, 1560, 1920, 2280, 2640, 3000};
    private static final long[] MARKS = {1150, 1550, 1910, 2230, 2520, 2780, 3010, 3215, 3400, 3565};
    private static final long[] FLICKERS = {4400, 5050, 5650};

    private static final Cue[][] SUSPENSE = {
            {},
            {
                    c(0, SoundEvents.BLASTFURNACE_FIRE_CRACKLE, 0.8, 1.0), c(0, SoundEvents.BEACON_ACTIVATE, 0.55, 0.45),
                    c(440, SoundEvents.PLAYER_ATTACK_SWEEP, 0.6, 0.5), c(620, SoundEvents.ANVIL_LAND, 0.72, 0.75), c(630, SoundEvents.FIRECHARGE_USE, 1.4, 0.25),
                    c(1240, SoundEvents.PLAYER_ATTACK_SWEEP, 0.7, 0.55), c(1420, SoundEvents.ANVIL_LAND, 0.88, 0.85), c(1430, SoundEvents.FIRECHARGE_USE, 1.6, 0.3),
                    c(2040, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8, 0.6), c(2220, SoundEvents.ANVIL_LAND, 1.05, 1.0), c(2230, SoundEvents.FIRECHARGE_USE, 1.8, 0.35),
                    c(2480, SoundEvents.WARDEN_HEARTBEAT, 1.1, 1.0), c(2700, SoundEvents.WARDEN_HEARTBEAT, 1.3, 1.1),
                    c(2820, SoundEvents.FIRE_EXTINGUISH, 1.5, 0.45)
            },
            {
                    c(0, SoundEvents.BEACON_ACTIVATE, 0.7, 0.7), c(300, SoundEvents.ENCHANTMENT_TABLE_USE, 0.7, 0.9),
                    c(900, SoundEvents.WARDEN_HEARTBEAT, 0.9, 0.8),
                    c(1200, SoundEvents.AMETHYST_BLOCK_CHIME, 0.75, 1.0), c(1200, SoundEvents.BEACON_POWER_SELECT, 0.8, 0.35),
                    c(1560, SoundEvents.AMETHYST_BLOCK_CHIME, 0.84, 1.0), c(1560, SoundEvents.WARDEN_HEARTBEAT, 1.0, 0.9),
                    c(1920, SoundEvents.AMETHYST_BLOCK_CHIME, 0.94, 1.0), c(1920, SoundEvents.BEACON_POWER_SELECT, 1.0, 0.35),
                    c(2280, SoundEvents.AMETHYST_BLOCK_CHIME, 1.06, 1.0), c(2280, SoundEvents.WARDEN_HEARTBEAT, 1.15, 1.0),
                    c(2640, SoundEvents.AMETHYST_BLOCK_CHIME, 1.19, 1.0), c(2640, SoundEvents.BEACON_POWER_SELECT, 1.2, 0.35),
                    c(3000, SoundEvents.AMETHYST_BLOCK_CHIME, 1.34, 1.0), c(3000, SoundEvents.WARDEN_HEARTBEAT, 1.3, 1.1),
                    c(3180, SoundEvents.WARDEN_HEARTBEAT, 1.45, 1.1),
                    c(3300, SoundEvents.EVOKER_PREPARE_SUMMON, 1.25, 0.9), c(3300, SoundEvents.ILLUSIONER_PREPARE_MIRROR, 0.8, 0.7),
                    c(3850, SoundEvents.BEACON_DEACTIVATE, 0.6, 0.8), c(3990, SoundEvents.WARDEN_HEARTBEAT, 0.7, 1.2)
            },
            {
                    c(0, SoundEvents.BEACON_DEACTIVATE, 0.55, 0.9), c(60, SoundEvents.CONDUIT_DEACTIVATE, 0.6, 0.8),
                    c(900, SoundEvents.CONDUIT_AMBIENT, 0.6, 0.9),
                    c(1150, SoundEvents.AMETHYST_BLOCK_CHIME, 0.50, 1.0), c(1550, SoundEvents.AMETHYST_BLOCK_CHIME, 0.56, 1.0),
                    c(1910, SoundEvents.AMETHYST_BLOCK_CHIME, 0.63, 1.0), c(2230, SoundEvents.AMETHYST_BLOCK_CHIME, 0.67, 1.0),
                    c(2520, SoundEvents.AMETHYST_BLOCK_CHIME, 0.75, 1.0), c(2780, SoundEvents.AMETHYST_BLOCK_CHIME, 0.84, 1.0),
                    c(3010, SoundEvents.AMETHYST_BLOCK_CHIME, 0.94, 1.0), c(3215, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0, 1.0),
                    c(3400, SoundEvents.AMETHYST_BLOCK_CHIME, 1.12, 1.0), c(3565, SoundEvents.AMETHYST_BLOCK_CHIME, 1.26, 1.0),
                    c(4000, SoundEvents.WARDEN_HEARTBEAT, 0.8, 1.0),
                    c(4400, SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.8, 0.8), c(4600, SoundEvents.FIRE_EXTINGUISH, 1.6, 0.25),
                    c(4550, SoundEvents.WARDEN_HEARTBEAT, 0.9, 1.0),
                    c(5050, SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.0, 0.9), c(5250, SoundEvents.FIRE_EXTINGUISH, 1.7, 0.25),
                    c(5060, SoundEvents.WARDEN_HEARTBEAT, 1.0, 1.1), c(5250, SoundEvents.WARDEN_SONIC_CHARGE, 0.8, 1.0),
                    c(5500, SoundEvents.WARDEN_HEARTBEAT, 1.1, 1.1),
                    c(5650, SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.25, 1.0), c(5850, SoundEvents.FIRE_EXTINGUISH, 1.8, 0.3),
                    c(5900, SoundEvents.WARDEN_HEARTBEAT, 1.25, 1.2), c(6250, SoundEvents.WARDEN_HEARTBEAT, 1.4, 1.2),
                    c(6550, SoundEvents.WARDEN_HEARTBEAT, 1.55, 1.3), c(6780, SoundEvents.WARDEN_HEARTBEAT, 1.7, 1.3),
                    c(6900, SoundEvents.BEACON_DEACTIVATE, 0.5, 1.0),
                    c(7550, SoundEvents.WARDEN_HEARTBEAT, 0.6, 1.4),
                    c(8020, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.8, 0.6)
            }
    };

    private static final Cue[][] SUCCESS = {
            {},
            {
                    c(0, SoundEvents.ANVIL_USE, 1.5, 0.8), c(0, SoundEvents.PLAYER_LEVELUP, 1.0, 1.0), c(0, SoundEvents.FIREWORK_ROCKET_BLAST, 1.0, 0.9),
                    c(160, SoundEvents.BELL_BLOCK, 1.3, 0.8), c(650, SoundEvents.FIREWORK_ROCKET_TWINKLE, 1.0, 0.8),
                    c(1500, SoundEvents.FIREWORK_ROCKET_BLAST_FAR, 1.1, 0.7)
            },
            {
                    c(0, SoundEvents.EVOKER_CAST_SPELL, 1.0, 1.0), c(0, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0, 1.0),
                    c(0, SoundEvents.AMETHYST_BLOCK_BREAK, 0.6, 1.0), c(120, SoundEvents.PLAYER_LEVELUP, 0.75, 1.0),
                    c(320, SoundEvents.ENCHANTMENT_TABLE_USE, 1.2, 1.0), c(700, SoundEvents.FIREWORK_ROCKET_TWINKLE, 1.1, 0.9),
                    c(1500, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0, 1.0), c(2300, SoundEvents.FIREWORK_ROCKET_TWINKLE_FAR, 1.2, 0.8)
            },
            {
                    c(0, SoundEvents.WARDEN_SONIC_BOOM, 1.0, 1.0), c(0, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.9, 0.7), c(0, SoundEvents.END_PORTAL_SPAWN, 1.0, 0.8),
                    c(260, SoundEvents.TOTEM_USE, 1.0, 0.9), c(620, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0, 1.0),
                    c(1200, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.0, 1.0), c(1500, SoundEvents.PLAYER_LEVELUP, 0.6, 1.0),
                    c(2000, SoundEvents.FIREWORK_ROCKET_TWINKLE, 1.0, 0.9), c(2900, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST_FAR, 0.9, 0.9),
                    c(3600, SoundEvents.FIREWORK_ROCKET_TWINKLE_FAR, 1.2, 0.9), c(4500, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0, 1.0),
                    c(5200, SoundEvents.FIREWORK_ROCKET_BLAST_FAR, 1.0, 0.8), c(6000, SoundEvents.BELL_RESONATE, 1.2, 0.6)
            }
    };

    private static final Cue[][] FAIL = {
            {},
            {c(0, SoundEvents.FIRE_EXTINGUISH, 0.8, 0.8), c(0, SoundEvents.ANVIL_LAND, 0.5, 0.5)},
            {c(0, SoundEvents.GLASS_BREAK, 0.7, 1.0), c(0, SoundEvents.BEACON_DEACTIVATE, 0.8, 0.8), c(160, SoundEvents.AMETHYST_BLOCK_BREAK, 0.5, 0.9)},
            {c(0, SoundEvents.BEACON_DEACTIVATE, 0.45, 1.0), c(0, SoundEvents.CONDUIT_DEACTIVATE, 0.5, 1.0), c(350, SoundEvents.FIRE_EXTINGUISH, 0.6, 0.5)}
    };

    static {
        for (Cue[][] set : new Cue[][][]{SUSPENSE, SUCCESS, FAIL}) {
            for (Cue[] a : set) java.util.Arrays.sort(a, java.util.Comparator.comparingLong(Cue::at));
        }
    }

    private static int playCues(Cue[] cues, long t, int idx) {
        while (idx < cues.length && t >= cues[idx].at) {
            // 한참 지난 소리(창을 늦게 그리기 시작한 경우)는 건너뛴다
            if (t - cues[idx].at < 400) play(cues[idx].sound, cues[idx].pitch, cues[idx].vol);
            idx++;
        }
        return idx;
    }

    private static void reveal() {
        revealed = true;
        boolean success = outcome == Enhance.Outcome.SUCCESS;
        if (success) {
            // 3.79 (사용자 11-28 11:20 "강화 시에 나오는 파티클을 그냥 삭제해줘"): 큰 성공 장면도 입자를 뿌리지 않는다 (예전: 불꽃 · 토템 · 폭죽 · 엔드 막대 · 빛 입자가 몸을 따라다님)
            // 채팅 기록 등은 평소 표시를 쓰되, 글자는 이 연출이 직접 그린다
            EnhanceFx.showNow(outcome, guarded, from, to, item, true);
        } else {
            if (tier == T_FINAL && outcome == Enhance.Outcome.DESTROY) {
                play(SoundEvents.GLASS_BREAK, 0.5F, 1.0F);
                play(SoundEvents.ITEM_BREAK, 0.5F, 1.0F);
                play(SoundEvents.GENERIC_EXPLODE, 0.6F, 0.5F);
            }
            bannerPending = true;
        }
    }

    // ------------------------------------------------------------------ 그리기 공통

    private static float unit(int w, int h) {
        return Math.max(0.6F, Math.min(w / 480F, h / 270F));
    }

    private static float jitter(long t, int salt) {
        return (Fx2D.hash((int) (t / 18), salt) - 0.5F) * 2F;
    }

    private static void drawItem(GuiGraphics g, float x, float y, float sx, float sy, float rotDeg) {
        if (item.isEmpty() || sx <= 0.01F || sy <= 0.01F) return;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        if (rotDeg != 0) g.pose().mulPose(Axis.ZP.rotationDegrees(rotDeg));
        g.pose().scale(sx, sy, 1);
        g.renderItem(item, -8, -8);
        g.pose().popPose();
    }

    private static void text(GuiGraphics g, Font font, Component c, float x, float y, float scale, int rgb, float alpha) {
        int a = (int) (255 * Fx2D.clamp01(alpha));
        if (a < 10) return;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, c, -font.width(c) / 2, -4, (a << 24) | (rgb & 0xFFFFFF), true);
        g.pose().popPose();
    }

    /** 위아래 검은 띠 (영화 화면처럼). k = 0~1. */
    private static void bars(GuiGraphics g, int w, int h, float k) {
        int bh = (int) (h * 0.105F * Fx2D.clamp01(k));
        if (bh <= 0) return;
        g.fill(0, 0, w, bh, 0xFF000000);
        g.fill(0, h - bh, w, h, 0xFF000000);
    }

    private static float fadeOut(long rt, long total, long len) {
        return rt > total - len ? Fx2D.clamp01((total - rt) / (float) len) : 1F;
    }

    /** 성공 장면의 글자: 큰 제목, 장비 이름, "서버 전체에 알렸습니다". */
    private static void successText(GuiGraphics g, Font font, int w, float cx, float y, float u, long rt, float fade, Component big, int bigColor,
                                    float bigScale, Component extra) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        float ts = Math.max(1F, Math.round(u));
        int bandTop = (int) (y - 8 * u), bandBot = (int) (y + bigScale * u * 9 + (extra == null ? 30 : 44) * ts);
        int bandA = (int) (150 * fade * Fx2D.span(rt, 0, 300));
        if (bandA > 4) {
            for (int i = 0; i < 6; i++) {
                int a = bandA * (i + 1) / 6;
                g.fill(0, bandTop - 6 + i, w, bandTop - 5 + i, a << 24);
                g.fill(0, bandBot + 5 - i, w, bandBot + 6 - i, a << 24);
            }
            g.fill(0, bandTop, w, bandBot, bandA << 24);
        }
        float stamp = 1F + 1.3F * (1F - Fx2D.easeOut(Fx2D.span(rt, 60, 380)));
        float appear = Fx2D.span(rt, 40, 200);
        text(g, font, big, cx, y + bigScale * u * 4, bigScale * u * stamp, bigColor, fade * appear);
        float ly = y + bigScale * u * 9 + 6 * ts;
        if (extra != null) {
            text(g, font, extra, cx, ly + 2 * ts, 1.4F * ts, 0xFFE9A8, fade * Fx2D.span(rt, 500, 800));
            ly += 15 * ts;
        }
        text(g, font, item.getHoverName(), cx, ly, ts, 0xFFFFFF, fade * Fx2D.span(rt, 350, 650));
        text(g, font, Component.translatable("rtshop.enhance.grand_announced"), cx, ly + 12 * ts, ts, 0xFFE6A0, 0.9F * fade * Fx2D.span(rt, 900, 1300));
        g.pose().popPose();
    }

    public static void render(GuiGraphics g, int w, int h) {
        if (!running) return;
        long suspense = Enhance.suspenseMs(tier);
        long t = frozen >= 0 ? frozen : System.currentTimeMillis() - startAt;
        boolean success = outcome == Enhance.Outcome.SUCCESS;
        if (frozen < 0) {
            if (!revealed) {
                cueIdx = playCues(SUSPENSE[tier], t, cueIdx);
                if (t >= suspense) reveal();
            }
            if (revealed) {
                revealCueIdx = playCues(success ? SUCCESS[tier] : FAIL[tier], t - suspense, revealCueIdx);
                if (bannerPending && t - suspense >= bannerDelay(tier)) {
                    bannerPending = false;
                    EnhanceFx.showNow(outcome, guarded, from, to, item, false);
                }
            }
        }
        long rt = t - suspense;
        if (rt > revealMs(tier, success)) {
            if (frozen < 0) running = false;
            return;
        }
        Font font = Minecraft.getInstance().font;
        g.pose().pushPose();
        g.pose().translate(0, 0, 950);
        try {
            switch (tier) {
                case T_RUNE -> rune(g, font, w, h, t, rt, success);
                case T_FINAL -> transcend(g, font, w, h, t, rt, success);
                default -> forge(g, font, w, h, t, rt, success);
            }
        } catch (RuntimeException ex) {
            Fx2D.abort();
            running = false;
            com.rtshop.RtShop.LOGGER.error("[rtshop] 강화 연출을 그리지 못했습니다", ex);
        }
        g.pose().popPose();
    }

    // ================================================================== 1) 단조 (+7 ~ +8)

    private static void sparks(float cx, float cy, float u, long d, int seed) {
        for (int j = 0; j < 28; j++) {
            float life = 480 + 420 * Fx2D.hash(j, seed * 7 + 1);
            if (d > life) continue;
            double ang = Math.toRadians(-172 + 164 * Fx2D.hash(j, seed * 7 + 2));
            float v = (110 + 190 * Fx2D.hash(j, seed * 7 + 3)) * u;
            float tau = d / 1000F, prev = Math.max(0, tau - 0.04F);
            float gr = 420 * u;
            float x = cx + (float) Math.cos(ang) * v * tau, y = cy + (float) Math.sin(ang) * v * tau + 0.5F * gr * tau * tau;
            float px = cx + (float) Math.cos(ang) * v * prev, py = cy + (float) Math.sin(ang) * v * prev + 0.5F * gr * prev * prev;
            float k = d / life;
            int col = Fx2D.alpha(Fx2D.mix(0xFFFFF0A0, 0xFFFF5A1E, k), 1 - k);
            Fx2D.line(px, py, x, y, 1.5F * u, col & 0x00FFFFFF, col);
        }
    }

    private static void forge(GuiGraphics g, Font font, int w, int h, long t, long rt, boolean success) {
        float u = unit(w, h), cx = w / 2F, cy = h * 0.44F;
        if (rt < 0) {
            float in = Fx2D.ease(t / 350F);
            int done = 0;
            long since = 99999;
            for (long s : STRIKES) {
                if (t >= s) {
                    done++;
                    since = t - s;
                }
            }
            float heat = done / 3F;
            float hold = Fx2D.span(t, 2800, 3000);
            float amp = since < 260 && hold <= 0 ? (1 - since / 260F) * (2.5F + done * 1.3F) * u : 0;
            g.fill(0, 0, w, h, Fx2D.rgba(0x0A0603, 0.92F * in));
            g.pose().pushPose();
            g.pose().translate(jitter(t, 1) * amp, jitter(t, 2) * amp, 0);
            Fx2D.begin(g, true);
            Fx2D.glow(cx, cy + 6 * u, 190 * u, 130 * u, Fx2D.rgba(0xFF7A1E, (0.16F + 0.2F * heat) * in), 0x00FF7A1E);
            Fx2D.beam(cx - 78 * u, cy + 27 * u, cx + 78 * u, cy + 27 * u, 7 * u, Fx2D.rgba(0xFFB050, (0.25F + 0.5F * heat) * in));
            for (int i = 0; i < 3; i++) {
                long d = t - STRIKES[i];
                if (d >= -200 && d < 0) {
                    float p = (d + 200) / 200F;
                    float y1 = cy - 150 * u + 140 * u * Fx2D.easeIn(p);
                    Fx2D.beam(cx, y1 - 70 * u * p, cx, y1, 10 * u, Fx2D.rgba(0xFFE7B0, 0.9F * p));
                }
                if (d >= 0 && d < 340) {
                    float e = d / 340F;
                    Fx2D.softRing(cx, cy + 8 * u, (16 + 120 * Fx2D.easeOut(e)) * u, 6 * u, Fx2D.rgba(0xFFF2C8, 0.9F * (1 - e)));
                    if (d < 170) Fx2D.glow(cx, cy, 74 * u, Fx2D.rgba(0xFFFFFF, 0.75F * (1 - d / 170F)), 0x00FFFFFF);
                }
                if (d >= 0) sparks(cx, cy + 4 * u, u, d, i);
            }
            Fx2D.end();
            float squash = since < 140 ? 0.2F * (1 - since / 140F) : 0;
            float sc = 3.0F * u * (1 + 0.03F * (float) Math.sin(t / 260.0));
            drawItem(g, cx, cy + squash * 10 * u, sc * (1 + squash * 0.5F), sc * (1 - squash), -8 + 2 * (float) Math.sin(t / 700.0));
            Fx2D.begin(g, true);
            Fx2D.glow(cx, cy, 36 * u, Fx2D.rgba(0xFF6A00, (0.06F + 0.26F * heat) * in), 0x00FF6A00);
            if (hold > 0) Fx2D.glow(cx, cy, 48 * u, Fx2D.rgba(0xFFFFFF, 0.9F * hold), 0x00FFFFFF);
            for (int i = 0; i < 3; i++) {
                long d = t - STRIKES[i];
                float px = cx + (i - 1) * 24 * u, py = cy + 47 * u;
                if (d < 0) {
                    Fx2D.diamond(px, py, 4.5F * u, 4.5F * u, 0, Fx2D.rgba(0x8A6A40, 0.5F * in));
                } else {
                    float s = 1 + 0.9F * (1 - Math.min(1, d / 220F)) + (hold > 0 ? 0.25F * (float) Math.sin(t / 40.0) : 0);
                    Fx2D.glow(px, py, 11 * u * s, Fx2D.rgba(0xFFC040, 0.8F), 0x00FFC040);
                    Fx2D.diamond(px, py, 5 * u * s, 5 * u * s, 0, 0xFFFFE9A8);
                }
            }
            Fx2D.end();
            g.pose().popPose();
            bars(g, w, h, in);
            text(g, font, Component.translatable("rtshop.enhance.grand_trying", from + 1), cx, cy + 66 * u, Math.max(1F, 1.2F * u), 0xFFE6A0,
                    0.85F * in * (1 - hold));
            return;
        }
        if (!success) {
            float k = rt / 950F;
            g.fill(0, 0, w, h, Fx2D.rgba(0x0A0603, 0.92F * (1 - Fx2D.easeIn(k))));
            Fx2D.begin(g, true);
            Fx2D.glow(cx, cy, 70 * u, Fx2D.rgba(0x6A8CFF, 0.3F * (1 - k)), 0x006A8CFF);
            for (int i = 0; i < 9; i++) {
                float x = cx + (Fx2D.hash(i, 31) - 0.5F) * 70 * u, y = cy - (10 + 60 * k * (0.6F + Fx2D.hash(i, 32))) * u;
                Fx2D.glow(x, y, (9 + 22 * k) * u, Fx2D.rgba(0xFFFFFF, 0.22F * (1 - k)), 0x00FFFFFF);
            }
            for (int i = 0; i < 3; i++) {
                float px = cx + (i - 1) * 24 * u + (i - 1) * 8 * u * k, py = cy + 47 * u + 40 * u * k * k;
                Fx2D.diamond(px, py, 5 * u, 5 * u, 50 * k * (i - 1), Fx2D.rgba(0x8A8A8A, 0.8F * (1 - k)));
            }
            Fx2D.end();
            if (rt < 480 && outcome != Enhance.Outcome.DESTROY) drawItem(g, cx, cy, 3.0F * u, 3.0F * u, -8);
            bars(g, w, h, 1 - k);
            return;
        }
        long total = revealMs(T_FORGE, true);
        float fade = fadeOut(rt, total, 600);
        float in = Fx2D.span(rt, 0, 250);
        g.fill(0, 0, w, h, Fx2D.rgba(0x120A04, 0.84F * fade));
        float amp = rt < 500 ? (1 - rt / 500F) * 5 * u : 0;
        g.pose().pushPose();
        g.pose().translate(jitter(rt, 3) * amp, jitter(rt, 4) * amp, 0);
        Fx2D.begin(g, true);
        if (rt < 450) Fx2D.rect(0, 0, w, h, Fx2D.rgba(0xFFE9B0, 0.8F * (1 - rt / 450F)));
        float rot = rt * 0.012F;
        for (int i = 0; i < 14; i++) {
            Fx2D.ray(cx, cy, rot + i * 360F / 14, 6.5F, 210 * u, Fx2D.rgba(0xFFC83C, (i % 2 == 0 ? 0.34F : 0.2F) * fade * in), 0x00FFC83C);
        }
        Fx2D.glow(cx, cy, 80 * u, Fx2D.rgba(0xFFD98A, 0.55F * fade), 0x00FFD98A);
        if (rt < 750) {
            float e = rt / 750F;
            Fx2D.softRing(cx, cy, (30 + 240 * Fx2D.easeOut(e)) * u, 7 * u, Fx2D.rgba(0xFFF4D0, 1 - e));
        }
        for (int i = 0; i < 70; i++) {
            float speed = 0.025F + 0.05F * Fx2D.hash(i, 41);
            float y = h + 8 - ((rt * speed * u + Fx2D.hash(i, 42) * (h + 30)) % (h + 30));
            float x = Fx2D.hash(i, 43) * w + (float) Math.sin(rt * 0.002 + i) * 8 * u;
            float s = (1.1F + 1.3F * Fx2D.hash(i, 44)) * u;
            Fx2D.glow(x, y, s * 2.2F, Fx2D.rgba(i % 3 == 0 ? 0xFFF0B0 : 0xFF8A2A, 0.8F * fade * in), 0x00FF8A2A);
        }
        float len = 100 * u * Fx2D.easeOut(Fx2D.span(rt, 350, 900));
        float uy = cy + 47 * u;
        Fx2D.line(cx, uy, cx - len, uy, 1.4F * u, Fx2D.rgba(0xFFE9A8, fade), 0x00FFE9A8);
        Fx2D.line(cx, uy, cx + len, uy, 1.4F * u, Fx2D.rgba(0xFFE9A8, fade), 0x00FFE9A8);
        Fx2D.end();
        float pop = Fx2D.pop(Fx2D.span(rt, 0, 420));
        float sc = 3.7F * u * pop;
        drawItem(g, cx, cy - 4 * u + (float) Math.sin(rt / 400.0) * 2 * u, sc, sc, -14 * (1 - Fx2D.ease(Fx2D.span(rt, 0, 520))));
        g.pose().popPose();
        bars(g, w, h, fade);
        successText(g, font, w, cx, cy + 54 * u, u, rt, fade, Component.translatable("rtshop.enhance.grand_success", to), 0xFFD54A, 2.6F, null);
    }

    // ================================================================== 2) 각인 (+9 ~ +10)

    private static final int[] NODE_COLORS = {0xC79BFF, 0x8FD8FF, 0xFF8FE8, 0xC79BFF, 0x8FD8FF, 0xFF8FE8};

    /** 마법진: 바깥 고리, 안쪽 고리, 눈금, 육각별, 여섯 봉인. p = 그려진 정도(0~1), lit = 켜진 봉인 수. */
    private static void circle(float cx, float cy, float r, float u, float theta, float p1, float p2, float p3, long t, float alpha, boolean rays) {
        if (r < 1) return;
        float th = 1.3F * u;
        Fx2D.arc(cx, cy, r - th, r + th, -90 + theta, -90 + theta + 360 * p1, Fx2D.rgba(0xB48CFF, 0.95F * alpha), Fx2D.rgba(0xB48CFF, 0.55F * alpha));
        float r2 = r * 0.70F;
        Fx2D.arc(cx, cy, r2 - th * 0.8F, r2 + th * 0.8F, -90 - theta * 1.4F, -90 - theta * 1.4F - 360 * p2, Fx2D.rgba(0x7CE7FF, 0.85F * alpha),
                Fx2D.rgba(0x7CE7FF, 0.45F * alpha));
        for (int i = 0; i < 24; i++) {
            if (i / 24F > p1) break;
            double a = Math.toRadians(theta + i * 15);
            float c = (float) Math.cos(a), s = (float) Math.sin(a);
            float l0 = r + 3 * u, l1 = r + (i % 2 == 0 ? 8 : 5.5F) * u;
            Fx2D.line(cx + c * l0, cy + s * l0, cx + c * l1, cy + s * l1, 1.1F * u, Fx2D.rgba(0xE0D0FF, 0.7F * alpha), Fx2D.rgba(0xE0D0FF, 0.2F * alpha));
        }
        // 육각별 (삼각형 두 개)
        for (int k = 0; k < 2; k++) {
            for (int i = 0; i < 3; i++) {
                double a0 = Math.toRadians(-90 - theta * 1.4F + k * 60 + i * 120), a1 = Math.toRadians(-90 - theta * 1.4F + k * 60 + (i + 1) * 120);
                float x0 = cx + (float) Math.cos(a0) * r2, y0 = cy + (float) Math.sin(a0) * r2;
                float x1 = cx + (float) Math.cos(a1) * r2, y1 = cy + (float) Math.sin(a1) * r2;
                Fx2D.line(x0, y0, x0 + (x1 - x0) * p3, y0 + (y1 - y0) * p3, 1.0F * u, Fx2D.rgba(0xE0C8FF, 0.55F * alpha), Fx2D.rgba(0xE0C8FF, 0.55F * alpha));
            }
        }
        for (int i = 0; i < 6; i++) {
            double a = Math.toRadians(-90 + i * 60 + theta);
            float nx = cx + (float) Math.cos(a) * r, ny = cy + (float) Math.sin(a) * r;
            long d = t - NODES[i];
            int col = NODE_COLORS[i];
            if (d < 0) {
                Fx2D.softRing(nx, ny, 4 * u, 1.2F * u, Fx2D.rgba(0xB0A0D0, 0.4F * p1 * alpha));
                continue;
            }
            float grow = Fx2D.ease(Fx2D.span(d, 0, 150));
            Fx2D.beam(nx, ny, nx + (cx - nx) * grow, ny + (cy - ny) * grow, 3.2F * u, Fx2D.rgba(col, 0.55F * alpha));
            if (d < 280) {
                float e = d / 280F;
                Fx2D.softRing(nx, ny, (6 + 26 * e) * u, 2 * u, Fx2D.rgba(0xFFFFFF, (1 - e) * alpha));
            }
            float flash = 1 + 0.6F * (1 - Math.min(1, d / 240F));
            Fx2D.glow(nx, ny, 12 * u * flash, Fx2D.rgba(col, 0.95F * alpha), col & 0xFFFFFF);
            Fx2D.star(nx, ny, 9 * u * flash, 1.5F * u, Fx2D.rgba(0xFFFFFF, 0.95F * alpha));
            if (rays) {
                float deg = (float) Math.toDegrees(a);
                Fx2D.ray(nx, ny, deg, 2.2F, 170 * u, Fx2D.rgba(col, 0.5F * alpha), col & 0xFFFFFF);
            }
        }
    }

    private static void rune(GuiGraphics g, Font font, int w, int h, long t, long rt, boolean success) {
        float u = unit(w, h), cx = w / 2F, cy = h * 0.44F;
        if (rt < 0) {
            float in = Fx2D.ease(t / 400F);
            int lit = 0;
            for (long n : NODES) if (t >= n) lit++;
            float lk = lit / 6F;
            float contract = Fx2D.easeIn(Fx2D.span(t, 3300, 3850));
            boolean black = t >= 3850;
            g.fill(0, 0, w, h, Fx2D.rgba(0x05040F, 0.93F * in));
            float amp = lk * 1.3F * u + contract * 3 * u;
            g.pose().pushPose();
            g.pose().translate(jitter(t, 5) * amp, jitter(t, 6) * amp, 0);
            if (!black) {
                Fx2D.begin(g, true);
                Fx2D.glow(cx, cy, 180 * u, Fx2D.rgba(0x5A2BD8, (0.14F + 0.12F * lk) * in), 0x005A2BD8);
                float theta = t * 0.02F * (1 + lk * 3) + contract * 720;
                float r = 76 * u * (1 - 0.78F * contract);
                circle(cx, cy, r, u, theta, Fx2D.easeOut(Fx2D.span(t, 300, 1100)), Fx2D.easeOut(Fx2D.span(t, 500, 1200)), Fx2D.span(t, 700, 1200), t, 1F, false);
                Fx2D.glow(cx, cy, (12 + 28 * lk) * u, Fx2D.rgba(0xFFFFFF, 0.2F + 0.45F * lk), 0x00FFFFFF);
                Fx2D.end();
                float pulse = 1 + 0.06F * lk + 0.03F * (float) Math.sin(t / 120.0) * lk;
                float sc = 2.8F * u * pulse * (1 - 0.35F * contract);
                drawItem(g, cx, cy, sc, sc, 0);
                if (contract > 0) {
                    Fx2D.begin(g, true);
                    Fx2D.glow(cx, cy, 60 * u, Fx2D.rgba(0xE6D6FF, 0.95F * contract), 0x00E6D6FF);
                    Fx2D.end();
                }
            } else {
                // 완전한 어둠: 가운데 작은 빛 하나만 뛴다
                g.fill(0, 0, w, h, 0xFF000000);
                Fx2D.begin(g, true);
                float beat = 1 - Fx2D.span(t, 3990, 4200);
                float rr = (2.6F + 1.2F * (float) Math.sin(t / 55.0) + (t >= 3990 ? 5 * beat : 0)) * u;
                Fx2D.glow(cx, cy, rr * 3, Fx2D.rgba(0xC79BFF, 0.6F), 0x00C79BFF);
                Fx2D.glow(cx, cy, rr, 0xFFFFFFFF, 0x00FFFFFF);
                Fx2D.end();
            }
            g.pose().popPose();
            bars(g, w, h, in);
            if (!black) {
                text(g, font, Component.translatable("rtshop.enhance.grand_trying", from + 1), cx, cy + 96 * u, Math.max(1F, 1.2F * u), 0xD8C8FF,
                        0.85F * in * (1 - contract));
            }
            return;
        }
        if (!success) {
            // 마법진이 깨져 흩어진다
            long total = revealMs(T_RUNE, false);
            float k = rt / (float) total;
            g.fill(0, 0, w, h, Fx2D.rgba(0x05040F, 0.93F * (1 - Fx2D.easeIn(k))));
            Fx2D.begin(g, true);
            if (rt < 200) Fx2D.rect(0, 0, w, h, Fx2D.rgba(0xC0B0E0, 0.35F * (1 - rt / 200F)));
            float tau = rt / 1000F;
            float r = 76 * u;
            for (int i = 0; i < 24; i++) {
                float a0 = i * 15;
                double mid = Math.toRadians(a0 + 7.5);
                float out = (20 + 70 * Fx2D.hash(i, 51)) * u * tau;
                float ox = (float) Math.cos(mid) * out, oy = (float) Math.sin(mid) * out + 0.5F * 380 * u * tau * tau;
                float spin = (Fx2D.hash(i, 52) - 0.5F) * 260 * tau;
                Fx2D.arc(cx + ox, cy + oy, r - 1.3F * u, r + 1.3F * u, a0 + spin, a0 + 12 + spin, Fx2D.rgba(0x9A86C8, 0.9F * (1 - k)), Fx2D.rgba(0x9A86C8, 0.5F * (1 - k)));
                if (i % 2 == 0) {
                    float r2 = r * 0.7F;
                    Fx2D.arc(cx + ox * 0.6F, cy + oy * 0.8F, r2 - u, r2 + u, a0 - spin, a0 + 12 - spin, Fx2D.rgba(0x6FA8C0, 0.8F * (1 - k)), Fx2D.rgba(0x6FA8C0, 0.4F * (1 - k)));
                }
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.toRadians(-90 + i * 60);
                float nx = cx + (float) Math.cos(a) * r * (1 + 0.4F * tau), ny = cy + (float) Math.sin(a) * r * (1 + 0.4F * tau) + 0.5F * 300 * u * tau * tau;
                Fx2D.glow(nx, ny, 8 * u, Fx2D.rgba(0x8A8A9A, 0.7F * (1 - k)), 0x008A8A9A);
            }
            Fx2D.end();
            if (rt < 600 && outcome != Enhance.Outcome.DESTROY) drawItem(g, cx, cy, 2.8F * u, 2.8F * u, 0);
            bars(g, w, h, 1 - k);
            return;
        }
        long total = revealMs(T_RUNE, true);
        float fade = fadeOut(rt, total, 700);
        g.fill(0, 0, w, h, Fx2D.rgba(0x07051A, 0.88F * fade));
        float amp = rt < 600 ? (1 - rt / 600F) * 5 * u : 0;
        g.pose().pushPose();
        g.pose().translate(jitter(rt, 7) * amp, jitter(rt, 8) * amp, 0);
        Fx2D.begin(g, true);
        if (rt < 380) Fx2D.rect(0, 0, w, h, Fx2D.rgba(0xE8DCFF, 0.9F * (1 - rt / 380F)));
        // 빛기둥
        float pw = (26 + 60 * Fx2D.easeOut(rt / 300F)) * u;
        float pa = 0.8F * (1 - 0.7F * Fx2D.span(rt, 300, 2200)) * fade;
        Fx2D.rect(cx - pw, 0, cx, h, 0x00B48CFF, Fx2D.rgba(0xFFFFFF, pa), Fx2D.rgba(0xFFFFFF, pa), 0x00B48CFF);
        Fx2D.rect(cx, 0, cx + pw, h, Fx2D.rgba(0xFFFFFF, pa), 0x00B48CFF, 0x00B48CFF, Fx2D.rgba(0xFFFFFF, pa));
        Fx2D.glow(cx, cy, 170 * u, Fx2D.rgba(0x7A4BFF, 0.3F * fade), 0x007A4BFF);
        float r = 96 * u * Fx2D.pop(Fx2D.span(rt, 0, 450));
        circle(cx, cy, r, u, rt * 0.03F, 1, 1, 1, 99999, fade, true);
        for (int s = 0; s < 2; s++) {
            long d = rt - s * 250L;
            if (d >= 0 && d < 800) {
                float e = d / 800F;
                Fx2D.softRing(cx, cy, (40 + 300 * Fx2D.easeOut(e)) * u, 8 * u, Fx2D.rgba(s == 0 ? 0xFFFFFF : 0xC79BFF, (1 - e) * fade));
            }
        }
        float tau = rt / 1000F;
        if (tau < 1.4F) {
            for (int i = 0; i < 64; i++) {
                double a = Math.toRadians(Fx2D.hash(i, 61) * 360);
                float v = (70 + 210 * Fx2D.hash(i, 62)) * u;
                float dist = v * tau * (1 - 0.3F * tau);
                float x = cx + (float) Math.cos(a) * dist, y = cy + (float) Math.sin(a) * dist;
                int col = i % 3 == 0 ? 0xFFFFFF : NODE_COLORS[i % 6];
                Fx2D.diamond(x, y, 1.6F * u, 4.5F * u, (float) Math.toDegrees(a) + 90 + rt * 0.4F, Fx2D.rgba(col, (1 - tau / 1.4F) * fade));
            }
        }
        if (rt > 400) {
            for (int i = 0; i < 30; i++) {
                double a = Math.toRadians(Fx2D.hash(i, 63) * 360);
                float dist = (30 + 120 * Fx2D.hash(i, 64)) * u;
                float tw = Math.abs((float) Math.sin(rt * 0.006 + i * 1.7));
                Fx2D.star(cx + (float) Math.cos(a) * dist, cy + (float) Math.sin(a) * dist * 0.8F, (2 + 3.5F * tw) * u, 0.7F * u, Fx2D.rgba(0xFFFFFF, 0.75F * tw * fade));
            }
        }
        Fx2D.end();
        float pop = Fx2D.pop(Fx2D.span(rt, 0, 450));
        float sc = 3.6F * u * pop;
        drawItem(g, cx, cy + (float) Math.sin(rt / 420.0) * 2 * u, sc, sc, 0);
        g.pose().popPose();
        bars(g, w, h, fade);
        successText(g, font, w, cx, cy + 62 * u, u, rt, fade, Component.translatable("rtshop.enhance.grand_success", to), 0xF0B8FF, 2.8F, null);
    }

    // ================================================================== 3) 초월 (+11)

    private static float markAngle(int i) {
        // 11번째(=0)가 맨 위, 1~10번은 시계 방향
        return -90 + i * (360F / 11F);
    }

    /** 금 간 자국: 가운데에서 뻗는 꺾인 선. grow = 0~1. */
    private static void fissure(float cx, float cy, float u, int i, float grow, float alpha) {
        if (grow <= 0) return;
        float ang = Fx2D.hash(i, 71) * 360;
        float total = (24 + 60 * Fx2D.hash(i, 72)) * u * grow;
        float x = cx, y = cy;
        int segs = 5;
        for (int s = 0; s < segs; s++) {
            float a = ang + (Fx2D.hash(i * 13 + s, 73) - 0.5F) * 70;
            float len = total / segs;
            float nx = x + (float) Math.cos(Math.toRadians(a)) * len, ny = y + (float) Math.sin(Math.toRadians(a)) * len;
            float wd = (2.0F - 1.4F * s / segs) * u;
            float al = alpha * (1 - 0.6F * s / segs);
            Fx2D.beam(x, y, nx, ny, wd * 2.2F, Fx2D.rgba(0xFFE9B0, al));
            x = nx;
            y = ny;
        }
    }

    private static void transcend(GuiGraphics g, Font font, int w, int h, long t, long rt, boolean success) {
        float u = unit(w, h), cx = w / 2F, cy = h * 0.44F;
        float far = Math.max(w, h);
        if (rt < 0) {
            // --- 화면이 닫힌다
            float close = Fx2D.easeIn(Fx2D.span(t, 0, 700));
            if (t < 900) {
                g.fill(0, 0, w, h, Fx2D.rgba(0x000000, 0.6F * Fx2D.span(t, 0, 500)));
                int bh = (int) (h / 2F * close) + 1;
                g.fill(0, 0, w, bh, 0xFF000000);
                g.fill(0, h - bh, w, h, 0xFF000000);
                if (close < 1) {
                    Fx2D.begin(g, true);
                    Fx2D.beam(0, bh, w, bh, 3 * u, Fx2D.rgba(0xFFFFFF, 0.5F * close));
                    Fx2D.beam(0, h - bh, w, h - bh, 3 * u, Fx2D.rgba(0xFFFFFF, 0.5F * close));
                    Fx2D.end();
                }
                return;
            }
            g.fill(0, 0, w, h, 0xFF000000);
            if (t >= 6900) {
                // --- 정적: 한 점
                Fx2D.begin(g, true);
                float beat = t >= 7550 ? 1 - Fx2D.span(t, 7550, 7900) : 0;
                float tremble = Fx2D.span(t, 8000, 8200);
                float px = cx + jitter(t, 9) * tremble * 1.2F * u, py = cy + jitter(t, 10) * tremble * 1.2F * u;
                float rr = (2.4F + 0.9F * (float) Math.sin(t / 170.0) + 4.5F * beat + 2 * tremble) * u;
                Fx2D.glow(px, py, rr * 4, Fx2D.rgba(0xFFE9B0, 0.35F + 0.3F * beat), 0x00FFE9B0);
                Fx2D.glow(px, py, rr, 0xFFFFFFFF, 0x00FFFFFF);
                if (tremble > 0) Fx2D.beam(cx - 60 * u * tremble, cy, cx + 60 * u * tremble, cy, 2.4F * u, Fx2D.rgba(0xFFFFFF, 0.85F * tremble));
                Fx2D.end();
                return;
            }
            float in = Fx2D.span(t, 900, 1300);
            float spin = Fx2D.easeIn(Fx2D.span(t, 4200, 6900)) * 540;
            float shrink = Fx2D.easeIn(Fx2D.span(t, 5600, 6900));
            float r = 66 * u * (1 - 0.72F * shrink);
            float strain = Fx2D.span(t, 4300, 6900);
            float amp = strain * strain * 4 * u;
            g.pose().pushPose();
            g.pose().translate(jitter(t, 11) * amp, jitter(t, 12) * amp, 0);
            Fx2D.begin(g, true);
            // 가운데로 빨려드는 별빛
            for (int i = 0; i < 110; i++) {
                float sp = 0.6F + 1.2F * Fx2D.hash(i, 81);
                float fr = (t * 0.00022F * sp * (1 + 2.5F * strain) + Fx2D.hash(i, 82)) % 1F;
                float dist = (float) Math.pow(1 - fr, 1.6) * far * 0.62F + 6 * u;
                double a = Math.toRadians(Fx2D.hash(i, 83) * 360);
                float c = (float) Math.cos(a), s = (float) Math.sin(a);
                float len = (4 + 14 * fr) * u;
                Fx2D.line(cx + c * (dist + len), cy + s * (dist + len), cx + c * dist, cy + s * dist, 0.9F * u, 0x00FFFFFF,
                        Fx2D.rgba(i % 5 == 0 ? 0xFFE9B0 : 0xFFFFFF, 0.75F * fr * in));
            }
            Fx2D.glow(cx, cy, 44 * u, Fx2D.rgba(0xFFFFFF, 0.10F + 0.25F * strain), 0x00FFFFFF);
            // 숫자판: 가는 고리와 열한 개의 자리
            Fx2D.ring(cx, cy, r - 0.5F * u, r + 0.5F * u, Fx2D.rgba(0xFFFFFF, 0.22F * in), Fx2D.rgba(0xFFFFFF, 0.22F * in));
            float lastX = 0, lastY = 0;
            boolean hasLast = false;
            for (int i = 1; i <= 10; i++) {
                double a = Math.toRadians(markAngle(i) + spin);
                float mx = cx + (float) Math.cos(a) * r, my = cy + (float) Math.sin(a) * r;
                long d = t - MARKS[i - 1];
                if (d < 0) {
                    Fx2D.glow(mx, my, 2.2F * u, Fx2D.rgba(0x9090A0, 0.5F * in), 0x009090A0);
                    hasLast = false;
                    continue;
                }
                if (hasLast) Fx2D.beam(lastX, lastY, mx, my, 2 * u, Fx2D.rgba(0xFFE9B0, 0.4F));
                float flash = 1 + 1.2F * (1 - Math.min(1, d / 260F));
                Fx2D.glow(mx, my, 8 * u * flash, Fx2D.rgba(0xFFE9B0, 0.9F), 0x00FFE9B0);
                Fx2D.star(mx, my, 7 * u * flash, 1.3F * u, 0xFFFFFFFF);
                if (d < 300) Fx2D.softRing(mx, my, (4 + 20 * d / 300F) * u, 1.6F * u, Fx2D.rgba(0xFFFFFF, 1 - d / 300F));
                lastX = mx;
                lastY = my;
                hasLast = true;
            }
            // 열한 번째 자리: 켜지려다 꺼진다
            double a11 = Math.toRadians(markAngle(0) + spin);
            float ex = cx + (float) Math.cos(a11) * r, ey = cy + (float) Math.sin(a11) * r;
            Fx2D.softRing(ex, ey, 5.5F * u, 1.3F * u, Fx2D.rgba(0xFFFFFF, 0.5F * Fx2D.span(t, 3565, 3900)));
            for (int f = 0; f < FLICKERS.length; f++) {
                long d = t - FLICKERS[f];
                if (d >= 0 && d < 420) {
                    float e = d / 420F;
                    float on = d < 180 ? 1 : (float) (0.5 + 0.5 * Math.sin(d / 14.0)) * (1 - e);
                    Fx2D.glow(ex, ey, (10 + 6 * f) * u, Fx2D.rgba(0xFFFFFF, 0.9F * on), 0x00FFFFFF);
                    Fx2D.star(ex, ey, (10 + 5 * f) * u, 1.6F * u, Fx2D.rgba(0xFFFFFF, on));
                    for (int j = 0; j < 10; j++) {
                        float tau = d / 1000F;
                        double sa = Math.toRadians(-150 + 120 * Fx2D.hash(j, 90 + f));
                        float v = (30 + 60 * Fx2D.hash(j, 93 + f)) * u;
                        float sx = ex + (float) Math.cos(sa) * v * tau, sy = ey + (float) Math.sin(sa) * v * tau + 0.5F * 300 * u * tau * tau;
                        Fx2D.glow(sx, sy, 1.6F * u, Fx2D.rgba(0xFFE9B0, 1 - e), 0x00FFE9B0);
                    }
                }
            }
            Fx2D.end();
            float sc = 2.4F * u * (1 + 0.05F * strain + 0.03F * (float) Math.sin(t / 110.0) * strain);
            drawItem(g, cx, cy, sc, sc, 0);
            // 장비에 금이 간다 (장비 위에 그린다)
            Fx2D.begin(g, true);
            int cracks = 2 + (int) (strain * 8);
            for (int i = 0; i < cracks && t >= 4300; i++) {
                float born = 4300 + i * 270;
                fissure(cx, cy, u, i, Fx2D.span(t, born, born + 520), 0.9F);
            }
            if (shrink > 0) Fx2D.glow(cx, cy, 70 * u, Fx2D.rgba(0xFFFFFF, 0.95F * shrink), 0x00FFFFFF);
            Fx2D.end();
            g.pose().popPose();
            text(g, font, Component.translatable("rtshop.enhance.final_caption"), cx, cy + 100 * u, Math.max(1F, 1.2F * u), 0xD8D8E8,
                    0.8F * in * (1 - Fx2D.span(t, 5600, 6400)));
            return;
        }
        if (!success) {
            failFinal(g, w, h, u, cx, cy, rt);
            return;
        }
        long total = revealMs(T_FINAL, true);
        float fade = fadeOut(rt, total, 900);
        // --- 새벽
        Fx2D.begin(g, false);
        Fx2D.rect(0, 0, w, h, Fx2D.rgba(0x0B1026, 0.95F * fade), Fx2D.rgba(0x0B1026, 0.95F * fade), Fx2D.rgba(0x2A1A3A, 0.95F * fade), Fx2D.rgba(0x2A1A3A, 0.95F * fade));
        Fx2D.end();
        float amp = rt < 900 ? (1 - rt / 900F) * 7 * u : 0;
        g.pose().pushPose();
        g.pose().translate(jitter(rt, 13) * amp, jitter(rt, 14) * amp, 0);
        Fx2D.begin(g, true);
        Fx2D.glow(cx, cy, far * 0.75F, Fx2D.rgba(0xFFD98A, 0.5F * fade), 0x00FFD98A);
        Fx2D.glow(cx, cy, 130 * u, Fx2D.rgba(0xFFFFFF, 0.45F * fade), 0x00FFFFFF);
        // 빛기둥과 가로 빛
        float pw = (18 + 90 * Fx2D.easeOut(rt / 500F)) * u;
        float pa = (0.8F - 0.45F * Fx2D.span(rt, 500, 2500)) * fade;
        Fx2D.rect(cx - pw, 0, cx, h, 0x00FFD98A, Fx2D.rgba(0xFFFFFF, pa), Fx2D.rgba(0xFFFFFF, pa), 0x00FFD98A);
        Fx2D.rect(cx, 0, cx + pw, h, Fx2D.rgba(0xFFFFFF, pa), 0x00FFD98A, 0x00FFD98A, Fx2D.rgba(0xFFFFFF, pa));
        float la = 0.7F * (1 - Fx2D.span(rt, 400, 2600)) * fade;
        Fx2D.beam(cx - w * 0.6F, cy, cx + w * 0.6F, cy, (12 - 8 * Fx2D.span(rt, 0, 1500)) * u, Fx2D.rgba(0xFFFFFF, la));
        // 햇살: 굵은 것과, 반대로 도는 가는 것
        float in = Fx2D.span(rt, 0, 400);
        float rot = rt * 0.012F;
        for (int i = 0; i < 22; i++) {
            boolean big = i % 2 == 0;
            Fx2D.ray(cx, cy, rot + i * 360F / 22, 4F, (big ? 250 : 180) * u, Fx2D.rgba(0xFFE9B0, (big ? 0.3F : 0.18F) * fade * in), 0x00FFE9B0);
        }
        for (int i = 0; i < 11; i++) {
            Fx2D.ray(cx, cy, -rot * 1.7F + i * 360F / 11, 0.8F, 320 * u, Fx2D.rgba(0xFFFFFF, 0.28F * fade * in), 0x00FFFFFF);
        }
        // 숫자판: 열한 개의 별이 모두 타오른다. 맨 위(11번째)가 왕관.
        float r = 92 * u * Fx2D.pop(Fx2D.span(rt, 0, 600));
        Fx2D.ring(cx, cy, r - u, r + u, Fx2D.rgba(0xFFE9B0, 0.9F * fade), Fx2D.rgba(0xFFE9B0, 0.5F * fade));
        // 무지개 고리
        float r3 = r + 11 * u;
        for (int i = 0; i < 44; i++) {
            int col = Mth.hsvToRgb(((i / 44F) + rt * 0.0002F) % 1F, 0.55F, 1F);
            float a0 = i * 360F / 44 - rot * 0.6F;
            Fx2D.arc(cx, cy, r3 - 1.4F * u, r3 + 1.4F * u, a0, a0 + 360F / 44, Fx2D.rgba(col, 0.6F * fade * in), Fx2D.rgba(col, 0.25F * fade * in));
        }
        for (int i = 0; i <= 10; i++) {
            double a = Math.toRadians(markAngle(i));
            float mx = cx + (float) Math.cos(a) * r, my = cy + (float) Math.sin(a) * r;
            float tw = 1 + 0.25F * (float) Math.sin(rt * 0.008 + i * 1.3);
            if (i == 0) {
                Fx2D.glow(mx, my, 20 * u * tw, Fx2D.rgba(0xFFFFFF, 0.95F * fade), 0x00FFE9B0);
                Fx2D.star(mx, my, 24 * u * tw, 2.2F * u, Fx2D.rgba(0xFFFFFF, fade));
                Fx2D.softRing(mx, my, (12 + 4 * tw) * u, 1.5F * u, Fx2D.rgba(0xFFE9B0, 0.8F * fade));
            } else {
                Fx2D.glow(mx, my, 9 * u * tw, Fx2D.rgba(0xFFE9B0, 0.9F * fade), 0x00FFE9B0);
                Fx2D.star(mx, my, 10 * u * tw, 1.4F * u, Fx2D.rgba(0xFFFFFF, fade));
            }
        }
        // 세 번의 충격파
        long[] waves = {0, 320, 760};
        for (long wv : waves) {
            long d = rt - wv;
            if (d >= 0 && d < 1100) {
                float e = d / 1100F;
                Fx2D.softRing(cx, cy, (30 + far * 0.8F * Fx2D.easeOut(e) / u) * u, 10 * u, Fx2D.rgba(0xFFFFFF, (1 - e) * fade));
            }
        }
        // 떠오르는 빛 알갱이
        float motes = Fx2D.span(rt, 300, 900) * fade;
        for (int i = 0; i < 130; i++) {
            float speed = 0.03F + 0.05F * Fx2D.hash(i, 101);
            float y = h + 8 - ((rt * speed * u + Fx2D.hash(i, 102) * (h + 20)) % (h + 20));
            float x = Fx2D.hash(i, 103) * w + (float) Math.sin(rt * 0.0015 + i) * 10 * u;
            float s = (1 + 1.4F * Fx2D.hash(i, 104)) * u;
            Fx2D.glow(x, y, s * 2.4F, Fx2D.rgba(i % 4 == 0 ? 0xFFFFFF : 0xFFE08A, 0.8F * motes), 0x00FFE08A);
        }
        // 천천히 떨어지는 무지개 조각
        if (rt > 1400) {
            float cf = Fx2D.span(rt, 1400, 2000) * fade;
            for (int i = 0; i < 90; i++) {
                float speed = 0.02F + 0.035F * Fx2D.hash(i, 111);
                float y = -10 + (((rt - 1400) * speed * u + Fx2D.hash(i, 112) * (h + 20)) % (h + 20));
                float x = Fx2D.hash(i, 113) * w + (float) Math.sin(rt * 0.002 + i * 2.1) * 14 * u;
                int col = Mth.hsvToRgb(Fx2D.hash(i, 114), 0.5F, 1F);
                Fx2D.diamond(x, y, 1.8F * u, 3.4F * u, rt * 0.25F + i * 40, Fx2D.rgba(col, 0.85F * cf));
            }
        }
        // 처음의 흰 섬광
        if (rt < 900) Fx2D.rect(0, 0, w, h, Fx2D.rgba(0xFFFFFF, 1 - Fx2D.easeOut(rt / 900F)));
        Fx2D.end();
        float pop = Fx2D.pop(Fx2D.span(rt, 80, 780));
        float sc = 4.2F * u * pop;
        drawItem(g, cx, cy - 2 * u + (float) Math.sin(rt / 450.0) * 2.5F * u, sc, sc, 0);
        g.pose().popPose();
        successText(g, font, w, cx, cy + 58 * u, u, rt, fade, Component.literal("+" + to), 0xFFFFFF, 4.6F, Component.translatable("rtshop.enhance.final_success"));
    }

    /** +11 실패: 한 점이 꺼진다. 파괴면 장비가 조각나 흩어진다. */
    private static void failFinal(GuiGraphics g, int w, int h, float u, float cx, float cy, long rt) {
        long total = revealMs(T_FINAL, false);
        float k = rt / (float) total;
        boolean destroy = outcome == Enhance.Outcome.DESTROY;
        g.fill(0, 0, w, h, Fx2D.rgba(0x000000, 1 - Fx2D.easeIn(Fx2D.span(rt, total - 900, total))));
        Fx2D.begin(g, true);
        if (destroy) {
            if (rt < 260) Fx2D.rect(0, 0, w, h, Fx2D.rgba(0xFF3020, 0.55F * (1 - rt / 260F)));
            Fx2D.glow(cx, cy, 120 * u, Fx2D.rgba(0xFF2A1A, 0.3F * (1 - k)), 0x00FF2A1A);
        } else {
            // 점이 힘없이 떨어지며 꺼진다
            float fall = Fx2D.easeIn(Fx2D.span(rt, 100, 1200));
            float a = 1 - Fx2D.span(rt, 300, 1200);
            Fx2D.glow(cx, cy + 60 * u * fall, 8 * u, Fx2D.rgba(0xFFE9B0, 0.5F * a), 0x00FFE9B0);
            Fx2D.glow(cx, cy + 60 * u * fall, 2.4F * u, Fx2D.rgba(0xFFFFFF, a), 0x00FFFFFF);
            if (outcome == Enhance.Outcome.DAMAGE && rt < 300) Fx2D.rect(0, 0, w, h, Fx2D.rgba(0xFF8A2A, 0.3F * (1 - rt / 300F)));
        }
        // 꺼진 숫자판: 열 개의 별이 식어 간다, 열한 번째는 끝내 비어 있다
        float r = 66 * u;
        float da = (1 - k) * Fx2D.span(rt, 150, 500);
        Fx2D.ring(cx, cy, r - 0.5F * u, r + 0.5F * u, Fx2D.rgba(0x9090A0, 0.4F * da), Fx2D.rgba(0x9090A0, 0.4F * da));
        for (int i = 1; i <= 10; i++) {
            double a = Math.toRadians(markAngle(i));
            Fx2D.glow(cx + (float) Math.cos(a) * r, cy + (float) Math.sin(a) * r, 5 * u, Fx2D.rgba(0xB0A890, 0.7F * da), 0x00B0A890);
        }
        double a11 = Math.toRadians(markAngle(0));
        Fx2D.softRing(cx + (float) Math.cos(a11) * r, cy + (float) Math.sin(a11) * r, 5.5F * u, 1.3F * u, Fx2D.rgba(0x9090A0, 0.8F * da));
        Fx2D.end();
        if (destroy) {
            // 장비를 3×3 조각으로 잘라 흩뿌린다 (가위 영역 = 조각)
            float sc = 2.6F * u;
            float size = 16 * sc;
            float tau = rt / 1000F;
            if (tau < 1.5F) {
                for (int gy = 0; gy < 3; gy++) {
                    for (int gx = 0; gx < 3; gx++) {
                        int i = gy * 3 + gx;
                        float vx = (gx - 1) * (70 + 60 * Fx2D.hash(i, 121)) * u + (Fx2D.hash(i, 122) - 0.5F) * 50 * u;
                        float vy = (gy - 1) * 60 * u - (60 + 80 * Fx2D.hash(i, 123)) * u;
                        float ox = vx * tau, oy = vy * tau + 0.5F * 520 * u * tau * tau;
                        float x0 = cx - size / 2 + gx * size / 3 + ox, y0 = cy - size / 2 + gy * size / 3 + oy;
                        int sx0 = (int) Math.floor(x0), sy0 = (int) Math.floor(y0), sx1 = (int) Math.ceil(x0 + size / 3), sy1 = (int) Math.ceil(y0 + size / 3);
                        if (sx1 <= 0 || sy1 <= 0 || sx0 >= w || sy0 >= h) continue;
                        g.enableScissor(Math.max(0, sx0), Math.max(0, sy0), Math.min(w, sx1), Math.min(h, sy1));
                        drawItem(g, cx + ox, cy + oy, sc, sc, 0);
                        g.disableScissor();
                    }
                }
            }
        } else if (rt < 900) {
            drawItem(g, cx, cy, 2.4F * u, 2.4F * u, 0);
        }
    }
}

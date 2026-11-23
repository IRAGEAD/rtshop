package com.rtshop.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * 뽑기 연출 (화면 전체). 등급마다 길이·긴장·터지는 맛이 다르다. 클릭하면 건너뛴다.
 * <ul>
 * <li>일반 (1.4초): 카드가 툭 떨어져 한 번 뒤집힌다.</li>
 * <li>고급 (1.9초): 카드가 떠오르며 초록빛이 한 번 고동치고, 반짝이며 뒤집힌다.</li>
 * <li>희귀 (2.8초): 어두워지고 카드가 돌며 종소리가 세 번 올라간다 → 숨을 멈추는 정적 → 유리처럼 깨지며 푸른 충격파.</li>
 * <li>영웅 (4.4초): 마법진이 그려지고 심장이 뛴다 → 보랏빛 번개가 카드에 내리꽂힌다 → 빨려 들어갔다가 폭발.</li>
 * <li>전설 (9.1초): 영웅 연출과 똑같이 가다가 폭발 직전에 모든 소리가 끊기며 암전 → 어둠 속에 금빛 금이 간다 →
 *     웅장한 화음과 함께 전설 카드가 튀어나온다 → 빛기둥·금빛 고리·올라가는 종소리 → 빨려 들어갔다가 대폭발.</li>
 * <li>YAMATO (17.2초, 무기 뽑기 전용): 전설과 똑같이 가다가 네 번째 종이 울린 뒤 다시 암전 → 칼을 뽑는 소리와 함께
 *     어둠을 가르는 칼빛 → 점점 빨라지는 차원참 40번과 마지막 X 자 → 화면이 수백 조각으로 깨지고 세상이 흑백으로 멈춘다 →
 *     천천히 칼집에 넣고 '딸깍' → 조각이 한꺼번에 터져 나가며 색이 돌아오고, 환영검에 둘러싸인 야마토가 나타난다.
 *     소리는 모두 음악(tools/gacha_audio.py) 하나에 박자를 맞춰 들어 있다.</li>
 * </ul>
 */
public final class GachaFx {
    public static final int COMMON = 0, UNCOMMON = 1, RARE = 2, EPIC = 3, LEGENDARY = 4, YAMATO = 5;
    private static final int[] LEN = {28, 38, 56, 88, 182, 344};

    // 희귀
    private static final int RARE_HOLD = 32, RARE_BURST = 40;
    // 영웅
    private static final int EPIC_BOLTS = 48, EPIC_IMPLODE = 62, EPIC_BURST = 66;
    // 전설: 영웅인 척(~63) → 암전(64) → 전설 카드(78) → 종 네 번 → 폭발(142). YAMATO 도 129 틱까지 똑같다
    private static final int LEG_DARK = 64, LEG_STRIKE = 78, LEG_IMPLODE = 136, LEG_BURST = 142;
    private static final int[] LEG_BELLS = {92, 104, 116, 126};
    // YAMATO: 음악(rtshop:gacha.yamato)이 YM_START 에 시작. 아래는 음악 시간(초) × 20 + YM_START
    public static final int YM_START = 130;              // 0.00초 다시 암전
    private static final int YM_DRAW = YM_START + 8;     // 0.40초 칼을 뽑는다
    /** 음악 속 차원참 (초). tools/gacha_audio.py 의 SLASH 와 같아야 한다. */
    private static final double[] SLASH_TIMES = {0.75, 0.816, 0.88, 0.941, 1.0, 1.057, 1.112, 1.165, 1.217, 1.267, 1.316, 1.363, 1.409, 1.454,
            1.497, 1.539, 1.581, 1.621, 1.66, 1.699, 1.736, 1.773, 1.809, 1.844, 1.879, 1.912, 1.946, 1.978, 2.01, 2.041, 2.072, 2.102, 2.131, 2.16,
            2.189, 2.217, 2.244, 2.271, 2.298, 2.324};
    /** 마지막 X 자 (초). */
    private static final double[] X_TIMES = {2.38, 2.44};
    public static final int YM_BREAK = YM_START + 50;    // 2.50초 화면이 깨지고 흑백으로 멈춘다
    private static final int YM_SHEATHE = YM_BREAK + 8;  // 2.90초부터 천천히 칼집에 넣는다
    private static final int YM_CLICK = YM_START + 72;   // 3.60초 '딸깍'
    public static final int YM_BOOM = YM_START + 74;     // 3.70초 대폭발, 색이 돌아온다
    private static final int YM_TITLE = YM_BOOM + 14;

    private static final int GRAY = 0xC8C8C8, GREEN = 0x55D24A, GREEN_LIGHT = 0xC8FFB8, BLUE = 0x4A9BE8, BLUE_LIGHT = 0xCFE8FF;
    private static final int PURPLE = 0xB45AE8, PURPLE_LIGHT = 0xE6C2FF, GOLD = 0xFFC63A, GOLD_LIGHT = 0xFFF0B0;
    private static final int YM_BLUE = 0x49B6FF, YM_CYAN = 0x9FF4FF, YM_DEEP = 0x0A1C46;
    private static final ResourceLocation YAMATO_MUSIC = new ResourceLocation("rtshop", "gacha.yamato");
    private static final ResourceLocation BLACKOUT_SFX = new ResourceLocation("rtshop", "gacha.blackout");
    private static final ResourceLocation LEGENDARY_SFX = new ResourceLocation("rtshop", "gacha.legendary_pop");

    private static final class Spark {
        float x, y, px, py, vx, vy, drag, grav, size;
        int life, max, color;
    }

    /** 화면 조각 (YAMATO): 세 꼭짓점(화면 좌표)과 그 자리의 그림. */
    private static final class Shard {
        float[] x = new float[3], y = new float[3];
        float cx, cy, vx, vy, rot, vr, dx, dy, speed;
        int delay;
        boolean blown;
    }

    private final RandomSource rnd = RandomSource.create();
    private final List<Spark> sparks = new ArrayList<>();
    private final List<Shard> shards = new ArrayList<>();
    /** 베기 선: x0,y0,x1,y1 (화면 가운데 기준) */
    private final List<float[]> slashes = new ArrayList<>();
    private int grade = -1;
    private int t;
    private ItemStack stack = ItemStack.EMPTY;
    private String name = "";
    private SoundInstance music;
    private DynamicTexture shot, shotGray;
    private ResourceLocation shotId, grayId;
    private boolean captured;
    private int lastW = 320, lastH = 240;

    public void start(int grade, ItemStack stack, String name) {
        stop();
        this.grade = Mth.clamp(grade, COMMON, YAMATO);
        this.stack = stack;
        this.name = name;
        this.t = 0;
        sparks.clear();
        shards.clear();
        slashes.clear();
        captured = false;
        if (this.grade == YAMATO) {
            // 화면 전체를 가로지르는 선: {각도, 가운데에서 비켜난 x, y (화면 비율), 거품 여부}
            RandomSource r = RandomSource.create(9173);
            float last = 0;
            for (int i = 0; i < SLASH_TIMES.length; i++) {
                float a = last + 0.6F + r.nextFloat() * 1.9F; // 앞의 선과 많이 다른 각도
                last = a;
                slashes.add(new float[]{a, (r.nextFloat() - 0.5F) * 0.8F, (r.nextFloat() - 0.5F) * 0.7F, i % 3 == 1 ? 1 : 0});
            }
            slashes.add(new float[]{(float) Math.toRadians(35), 0, 0, 0});
            slashes.add(new float[]{(float) Math.toRadians(145), 0, 0, 0});
        }
        events(0);
    }

    public boolean active() {
        return grade >= 0 && t < LEN[grade];
    }

    public int grade() {
        return grade;
    }

    public int tickForTest() {
        return t;
    }

    public void skip() {
        if (grade >= 0) t = LEN[grade];
        sparks.clear();
        stop();
    }

    /** 음악을 멈추고 화면 그림을 놓는다 (건너뛰기·창 닫기). */
    public void stop() {
        if (music != null) {
            try {
                Minecraft.getInstance().getSoundManager().stop(music);
            } catch (Throwable ignored) {
            }
            music = null;
        }
        for (ResourceLocation id : new ResourceLocation[]{shotId, grayId}) {
            if (id == null) continue;
            try {
                Minecraft.getInstance().getTextureManager().release(id);
            } catch (Throwable ignored) {
            }
        }
        shotId = grayId = null;
        shot = shotGray = null;
        shards.clear();
    }

    /** 점검용: 연출의 한 장면으로 바로 간다 (소리는 내지 않는다). */
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
            if (music != null || shotId != null) stop();
            return;
        }
        t++;
        events(t);
        spawn(t);
        update();
        for (Shard s : shards) {
            if (t - YM_BREAK < s.delay) continue;
            if (t < YM_BOOM) {
                // 흑백 속 느린 시간: 아주 천천히 벌어진다
                s.cx += s.vx * 0.06F;
                s.cy += s.vy * 0.06F;
                s.rot += s.vr * 0.06F;
                continue;
            }
            if (!s.blown) {
                s.blown = true;
                s.vx = s.dx * s.speed;
                s.vy = s.dy * s.speed - 2;
                s.vr *= 4;
            }
            s.cx += s.vx;
            s.cy += s.vy;
            s.vy += 0.4F;
            s.vx *= 0.98F;
            s.rot += s.vr;
        }
        if (!active()) stop();
    }

    private boolean frozen() {
        return grade == YAMATO && t >= YM_START;
    }

    private void update() {
        if (frozen() && t < YM_BOOM) {
            return; // 시간이 멈춘다
        }
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

    /** 지금 나는 연출 소리를 모두 끊는다 (암전). */
    private static void cutAll() {
        try {
            Minecraft.getInstance().getSoundManager().stop(null, SoundSource.MASTER);
        } catch (Throwable ignored) {
        }
    }

    private static void sfx2(ResourceLocation id, float vol) {
        try {
            Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(id, SoundSource.MASTER, vol, 1.0F, RandomSource.create(), false, 0,
                    SoundInstance.Attenuation.NONE, 0, 0, 0, true));
        } catch (Throwable ignored) {
        }
    }

    private void playMusic() {
        try {
            music = new SimpleSoundInstance(YAMATO_MUSIC, SoundSource.MASTER, 1.0F, 1.0F, RandomSource.create(), false, 0,
                    SoundInstance.Attenuation.NONE, 0, 0, 0, true);
            Minecraft.getInstance().getSoundManager().play(music);
        } catch (Throwable ignored) {
        }
    }

    // ------------------------------------------------------------------ 소리

    private void events(int t) {
        switch (grade) {
            case COMMON -> {
                switch (t) {
                    case 0 -> sfx(SoundEvents.ITEM_PICKUP, 0.6F, 0.6F);
                    case 8 -> sfx(SoundEvents.WOOD_PLACE, 0.8F, 1.0F);
                    case 14 -> {
                        sfx(SoundEvents.BOOK_PAGE_TURN, 1.2F, 1.0F);
                        sfx(SoundEvents.NOTE_BLOCK_HAT.value(), 1.0F, 0.6F);
                        burst(18, 1.6F, GRAY, 0xFFFFFF);
                    }
                    default -> {
                    }
                }
            }
            case UNCOMMON -> {
                switch (t) {
                    case 0 -> sfx(SoundEvents.AMETHYST_BLOCK_STEP, 1.4F, 0.6F);
                    case 10 -> sfx(SoundEvents.AMETHYST_BLOCK_CHIME, 0.9F, 1.0F);
                    case 18 -> sfx(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.6F, 0.6F);
                    case 22 -> {
                        sfx(SoundEvents.AMETHYST_CLUSTER_BREAK, 1.5F, 0.9F);
                        sfx(SoundEvents.NOTE_BLOCK_CHIME.value(), 1.5F, 0.8F);
                        burst(36, 2.4F, GREEN, GREEN_LIGHT, 0xFFFFFF);
                    }
                    case 25 -> sfx(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.2F, 0.7F);
                    default -> {
                    }
                }
            }
            case RARE -> {
                switch (t) {
                    case 1 -> sfx(SoundEvents.BEACON_AMBIENT, 1.6F, 0.7F);
                    case 14 -> sfx(SoundEvents.NOTE_BLOCK_BELL.value(), 0.8F, 0.9F);
                    case 20 -> sfx(SoundEvents.NOTE_BLOCK_BELL.value(), 1.0F, 0.9F);
                    case 26 -> sfx(SoundEvents.NOTE_BLOCK_BELL.value(), 1.26F, 1.0F);
                    case RARE_HOLD -> sfx(SoundEvents.CONDUIT_ATTACK_TARGET, 1.4F, 0.4F);
                    case RARE_BURST -> {
                        sfx(SoundEvents.GLASS_BREAK, 1.0F, 1.0F);
                        sfx(SoundEvents.AMETHYST_CLUSTER_BREAK, 0.7F, 1.0F);
                        sfx(SoundEvents.NOTE_BLOCK_CHIME.value(), 1.0F, 1.0F);
                        burst(60, 3.2F, BLUE, BLUE_LIGHT, 0xFFFFFF);
                    }
                    case RARE_BURST + 3 -> sfx(SoundEvents.PLAYER_LEVELUP, 1.6F, 0.5F);
                    default -> {
                    }
                }
            }
            case EPIC -> epicEvents(t);
            default -> legendaryEvents(t);
        }
    }

    private void epicEvents(int t) {
        switch (t) {
            case 1 -> sfx(SoundEvents.ENCHANTMENT_TABLE_USE, 0.7F, 1.0F);
            case 4 -> sfx(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.6F, 1.0F);
            case 8, 22 -> sfx(SoundEvents.WARDEN_HEARTBEAT, 1.0F, 1.0F);
            case 34 -> sfx(SoundEvents.WARDEN_HEARTBEAT, 1.1F, 1.0F);
            case 42 -> sfx(SoundEvents.WARDEN_HEARTBEAT, 1.2F, 1.0F);
            case EPIC_BOLTS, EPIC_BOLTS + 5, EPIC_BOLTS + 9, EPIC_BOLTS + 12 -> {
                sfx(SoundEvents.LIGHTNING_BOLT_IMPACT, 1.6F + (t - EPIC_BOLTS) * 0.03F, 0.45F);
                sfx(SoundEvents.TRIDENT_THUNDER, 1.8F, 0.25F);
            }
            case EPIC_IMPLODE -> sfx(SoundEvents.WARDEN_SONIC_CHARGE, 1.5F, 0.8F);
            case EPIC_BURST -> {
                sfx(SoundEvents.EVOKER_CAST_SPELL, 1.0F, 1.0F);
                sfx(SoundEvents.WARDEN_SONIC_BOOM, 1.6F, 0.6F);
                sfx(SoundEvents.AMETHYST_CLUSTER_BREAK, 0.6F, 1.0F);
                sfx(SoundEvents.FIREWORK_ROCKET_BLAST, 1.0F, 0.9F);
                burst(90, 4.0F, PURPLE, PURPLE_LIGHT, 0xFFFFFF);
            }
            case EPIC_BURST + 4 -> sfx(SoundEvents.PLAYER_LEVELUP, 1.2F, 0.8F);
            default -> {
            }
        }
    }

    private void legendaryEvents(int t) {
        if (grade == YAMATO && t >= YM_START) {
            yamatoEvents(t);
            return;
        }
        if (t < LEG_DARK) {
            epicEvents(t); // 영웅인 척 (폭발 직전까지)
            return;
        }
        for (int k = 0; k < LEG_BELLS.length; k++) {
            if (t == LEG_BELLS[k]) {
                sfx(SoundEvents.BELL_BLOCK, new float[]{1.0F, 1.19F, 1.41F, 1.68F}[k], 1.0F);
                sfx(SoundEvents.NOTE_BLOCK_BELL.value(), new float[]{1.0F, 1.19F, 1.41F, 1.68F}[k], 0.5F);
            }
        }
        switch (t) {
            case LEG_DARK -> {
                cutAll();
                sfx2(BLACKOUT_SFX, 1.0F);
            }
            case LEG_DARK + 7 -> sfx(SoundEvents.WARDEN_HEARTBEAT, 0.7F, 0.7F);
            case LEG_DARK + 11 -> sfx(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.5F, 0.6F);
            case LEG_STRIKE -> {
                sfx2(LEGENDARY_SFX, 1.0F);
                sfx(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.9F, 0.7F);
                sfx(SoundEvents.TOTEM_USE, 1.4F, 0.5F);
                burst(110, 5.0F, GOLD, 0xFFFFFF, GOLD_LIGHT);
            }
            case LEG_STRIKE + 6 -> sfx(SoundEvents.BEACON_ACTIVATE, 1.1F, 1.0F);
            case LEG_STRIKE + 26 -> sfx(SoundEvents.WARDEN_SONIC_CHARGE, 0.8F, 0.4F);
            case LEG_IMPLODE -> {
                sfx(SoundEvents.WARDEN_SONIC_CHARGE, 1.9F, 0.8F);
                sfx(SoundEvents.BEACON_POWER_SELECT, 0.6F, 1.0F);
            }
            case LEG_BURST -> {
                sfx(SoundEvents.TOTEM_USE, 1.0F, 1.0F);
                sfx(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
                sfx(SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.0F, 1.0F);
                sfx(SoundEvents.GENERIC_EXPLODE, 1.4F, 0.5F);
                burst(180, 6.0F, GOLD, GOLD_LIGHT, 0xFFFFFF);
            }
            case LEG_BURST + 5 -> sfx(SoundEvents.FIREWORK_ROCKET_TWINKLE, 1.0F, 1.0F);
            case LEG_BURST + 12 -> sfx(SoundEvents.FIREWORK_ROCKET_TWINKLE_FAR, 1.2F, 1.0F);
            case LEG_BURST + 18 -> sfx(SoundEvents.FIREWORK_ROCKET_LARGE_BLAST_FAR, 1.1F, 0.8F);
            default -> {
            }
        }
    }

    private void yamatoEvents(int t) {
        if (t == YM_START) {
            cutAll();
            playMusic(); // 이 뒤의 소리는 모두 음악 안에 있다
        }
        if (t == YM_BOOM) {
            burst(220, 7.0F, YM_BLUE, YM_CYAN, 0xFFFFFF);
        }
    }

    // ------------------------------------------------------------------ 파티클

    private void spawn(int t) {
        switch (grade) {
            case COMMON -> {
            }
            case UNCOMMON -> {
                if (t >= 4 && t < 22 && t % 2 == 0) orbit(GREEN_LIGHT, 1, 40);
                if (t > 22 && t % 3 == 0) rise(GREEN_LIGHT, 1);
            }
            case RARE -> {
                if (t >= 8 && t < RARE_HOLD && t % 2 == 0) converge(BLUE, 2);
                if (t > RARE_BURST && t % 3 == 0) rise(BLUE_LIGHT, 1);
            }
            case EPIC -> {
                if (t >= 10 && t < EPIC_IMPLODE && t % 2 == 0) converge(PURPLE, 2);
                if (t >= EPIC_IMPLODE && t < EPIC_BURST) converge(PURPLE_LIGHT, 6);
                if (t > EPIC_BURST && t % 2 == 0) rise(PURPLE_LIGHT, 1);
            }
            default -> {
                if (frozen()) {
                    if (t == YM_START) sparks.clear();
                    if (t >= YM_BOOM + 6 && t % 2 == 0) drift(YM_CYAN, 1);
                    return;
                }
                if (t < LEG_DARK) {
                    // 영웅과 같은 모습
                    if (t >= 10 && t < EPIC_IMPLODE && t % 2 == 0) converge(PURPLE, 2);
                    if (t >= EPIC_IMPLODE) converge(PURPLE_LIGHT, 6);
                    return;
                }
                if (t == LEG_DARK) sparks.clear();
                if (t < LEG_STRIKE) {
                    // 어둠 속 금에서 새어 나오는 금빛 부스러기
                    if (t >= LEG_DARK + 8) {
                        Spark sp = add((rnd.nextFloat() - 0.5F) * 4, (rnd.nextFloat() - 0.5F) * 60, (rnd.nextFloat() - 0.5F) * 1.6F, -0.3F - rnd.nextFloat(),
                                10 + rnd.nextInt(8), GOLD_LIGHT, 1 + rnd.nextFloat());
                        sp.drag = 0.95F;
                    }
                    return;
                }
                if (t >= LEG_STRIKE && t < LEG_IMPLODE) {
                    converge(GOLD, 2);
                    rise(GOLD, 2);
                }
                if (t >= LEG_IMPLODE && t < LEG_BURST) converge(GOLD_LIGHT, 8);
                if (t > LEG_BURST) {
                    confetti(3);
                    if (t % 2 == 0) rise(GOLD_LIGHT, 1);
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
            float r = 110 + rnd.nextFloat() * 70;
            int life = 14 + rnd.nextInt(6);
            float x = Mth.cos(a) * r;
            float y = Mth.sin(a) * r;
            add(x, y, -x / life, -y / life, life, color, 1.5F + rnd.nextFloat());
        }
    }

    private void orbit(int color, int n, float r) {
        for (int i = 0; i < n; i++) {
            float a = rnd.nextFloat() * Mth.TWO_PI;
            Spark s = add(Mth.cos(a) * r, Mth.sin(a) * r, -Mth.sin(a) * 1.6F, Mth.cos(a) * 1.6F - 0.4F, 18, color, 1.5F);
            s.drag = 0.96F;
        }
    }

    private void rise(int color, int n) {
        for (int i = 0; i < n; i++) {
            Spark s = add((rnd.nextFloat() - 0.5F) * 280, 90 + rnd.nextFloat() * 30, (rnd.nextFloat() - 0.5F) * 0.4F,
                    -1.2F - rnd.nextFloat() * 1.5F, 40 + rnd.nextInt(30), color, 1 + rnd.nextFloat() * 1.5F);
            s.drag = 0.995F;
        }
    }

    private void drift(int color, int n) {
        for (int i = 0; i < n; i++) {
            Spark s = add((rnd.nextFloat() - 0.5F) * 400, (rnd.nextFloat() - 0.5F) * 260, (rnd.nextFloat() - 0.5F) * 0.3F,
                    -0.2F - rnd.nextFloat() * 0.4F, 60 + rnd.nextInt(40), color, 1 + rnd.nextFloat());
            s.drag = 1F;
        }
    }

    private void confetti(int n) {
        int[] cols = {GOLD, GOLD_LIGHT, 0xFFFFFF, 0xFF8A3A, 0xFFD24A};
        for (int i = 0; i < n; i++) {
            Spark s = add((rnd.nextFloat() - 0.5F) * 380, -140 - rnd.nextFloat() * 20, (rnd.nextFloat() - 0.5F) * 0.8F,
                    0.6F + rnd.nextFloat() * 0.8F, 100, cols[rnd.nextInt(cols.length)], 1.5F + rnd.nextFloat() * 1.5F);
            s.grav = 0.01F;
        }
    }

    private void burst(int n, float speed, int... colors) {
        for (int i = 0; i < n; i++) {
            float a = rnd.nextFloat() * Mth.TWO_PI;
            float v = speed * (0.4F + rnd.nextFloat());
            Spark s = add(0, 0, Mth.cos(a) * v, Mth.sin(a) * v, 24 + rnd.nextInt(26), colors[rnd.nextInt(colors.length)],
                    1.5F + rnd.nextFloat() * 2);
            s.drag = 0.94F;
            s.grav = 0.03F;
        }
    }

    // ------------------------------------------------------------------ 그리기 도구

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

    private static float ease(float x) {
        x = Mth.clamp(x, 0, 1);
        return 1 - (1 - x) * (1 - x) * (1 - x);
    }

    /** 가운데가 밝은 빛 무리. */
    private static void glow(GuiGraphics g, float r, int rgb, float alpha) {
        int steps = 7;
        for (int k = steps; k >= 1; k--) {
            float rr = r * k / steps;
            int c = argb(rgb, alpha * 0.2F);
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
            g.fillGradient((int) -width, 8, (int) Math.max(1, width), (int) len, argb(rgb, alpha), argb(rgb, 0));
            g.pose().popPose();
        }
    }

    /** 가는 선 (각도 자유). */
    private static void bar(GuiGraphics g, float x0, float y0, float x1, float y1, float w, int argb) {
        float dx = x1 - x0, dy = y1 - y0;
        float len = Mth.sqrt(dx * dx + dy * dy);
        g.pose().pushPose();
        g.pose().translate((x0 + x1) / 2, (y0 + y1) / 2, 0);
        g.pose().mulPose(Axis.ZP.rotation((float) Math.atan2(dy, dx)));
        int hw = Math.max(1, Math.round(len / 2));
        float hh = Math.max(0.5F, w / 2);
        g.pose().scale(1, hh, 1);
        g.fill(-hw, -1, hw, 1, argb);
        g.pose().popPose();
    }

    /** 고리 (점선으로 근사한 원). */
    private static void ring(GuiGraphics g, float r, float w, int rgb, float alpha, int segs, float rot) {
        for (int i = 0; i < segs; i++) {
            float a0 = rot + i * Mth.TWO_PI / segs;
            float a1 = rot + (i + 0.82F) * Mth.TWO_PI / segs;
            bar(g, Mth.cos(a0) * r, Mth.sin(a0) * r, Mth.cos(a1) * r, Mth.sin(a1) * r, w, argb(rgb, alpha));
        }
    }

    /** 마법진: 두 겹 원, 룬 눈금, 돌아가는 별 모양. draw 0~1 = 그려진 정도. */
    private static void circle(GuiGraphics g, float r, float tt, int rgb, float alpha, float draw) {
        int segs = 48;
        int shown = (int) (segs * Mth.clamp(draw, 0, 1));
        for (int i = 0; i < shown; i++) {
            float a0 = tt * 0.02F + i * Mth.TWO_PI / segs;
            float a1 = tt * 0.02F + (i + 1) * Mth.TWO_PI / segs;
            bar(g, Mth.cos(a0) * r, Mth.sin(a0) * r, Mth.cos(a1) * r, Mth.sin(a1) * r, 1.5F, argb(rgb, alpha));
            float r2 = r * 0.78F;
            float b0 = -tt * 0.03F + i * Mth.TWO_PI / segs;
            float b1 = -tt * 0.03F + (i + 1) * Mth.TWO_PI / segs;
            bar(g, Mth.cos(b0) * r2, Mth.sin(b0) * r2, Mth.cos(b1) * r2, Mth.sin(b1) * r2, 1, argb(rgb, alpha * 0.7F));
            if (i % 4 == 0) {
                float m = (a0 + a1) / 2;
                bar(g, Mth.cos(m) * r * 0.82F, Mth.sin(m) * r * 0.82F, Mth.cos(m) * r * 0.96F, Mth.sin(m) * r * 0.96F, 2, argb(rgb, alpha));
            }
        }
        if (draw > 0.6F) {
            float a = (draw - 0.6F) / 0.4F;
            float rr = r * 0.74F;
            for (int k = 0; k < 6; k++) {
                float p0 = tt * 0.025F + k * Mth.TWO_PI / 6;
                float p1 = tt * 0.025F + (k + 2) * Mth.TWO_PI / 6;
                bar(g, Mth.cos(p0) * rr, Mth.sin(p0) * rr, Mth.cos(p1) * rr, Mth.sin(p1) * rr, 1, argb(rgb, alpha * a * 0.8F));
            }
        }
    }

    /** 지그재그 번개 (위에서 카드로). */
    private void bolt(GuiGraphics g, float x0, float y0, float x1, float y1, int rgb, float alpha, long seed, float w) {
        RandomSource r = RandomSource.create(seed);
        int n = 9;
        float px = x0, py = y0;
        for (int i = 1; i <= n; i++) {
            float f = i / (float) n;
            float x = Mth.lerp(f, x0, x1) + (i == n ? 0 : (r.nextFloat() - 0.5F) * 26);
            float y = Mth.lerp(f, y0, y1) + (i == n ? 0 : (r.nextFloat() - 0.5F) * 10);
            bar(g, px, py, x, y, w * 3, argb(rgb, alpha * 0.35F));
            bar(g, px, py, x, y, w, argb(0xFFFFFF, alpha));
            px = x;
            py = y;
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
            int c = 0xFF000000 | crackRgb;
            float[][] lines = {{0, -h, -6, -14}, {-6, -14, 4, 2}, {4, 2, -3, 18}, {-3, 18, 2, h}, {4, 2, 18, 10}, {-6, -14, -20, -22}, {-3, 18, -18, 26}};
            int shown = (int) Math.ceil(crack * lines.length);
            for (int i = 0; i < shown && i < lines.length; i++) {
                bar(g, lines[i][0], lines[i][1], lines[i][2], lines[i][3], 1.5F, c);
            }
        }
    }

    private void item(GuiGraphics g, float scale, float y) {
        g.pose().pushPose();
        g.pose().translate(-8 * scale, -8 * scale + y, 40);
        g.pose().scale(scale, scale, 1);
        g.renderItem(stack, 0, 0);
        g.pose().popPose();
    }

    /** 등급 글자 + 이름 (아래쪽). */
    private void label(GuiGraphics g, Font font, String key, int rgb, float since, float base, boolean fancy) {
        g.flush();
        g.pose().pushPose();
        g.pose().translate(0, 54, 400);
        float ts = base * (since < 8 ? 1 + 0.6F * (1 - ease(since / 8F)) : 1);
        g.pose().scale(ts, ts, 1);
        Component label = fancy ? RaidText.c(Component.translatable(key).getString(), RaidText.TITLE) : Component.translatable(key);
        int lw = font.width(label);
        if (fancy) {
            g.drawString(font, label, -lw / 2 + 1, 1, argb(0x000000, 0.6F), false);
        }
        g.drawString(font, label, -lw / 2, 0, 0xFF000000 | rgb, !fancy);
        g.pose().popPose();
        g.pose().pushPose();
        g.pose().translate(0, 54 + 9 * ts + 7, 400);
        g.pose().scale(1.4F, 1.4F, 1);
        g.drawString(font, name, -font.width(name) / 2, 0, 0xFFFFFFFF, true);
        g.pose().popPose();
    }

    private void particles(GuiGraphics g, float partial) {
        boolean still = frozen() && t < YM_BOOM;
        g.pose().pushPose();
        g.pose().translate(0, 0, 350);
        for (Spark s : sparks) {
            float x = still ? s.x : Mth.lerp(partial, s.px, s.x);
            float y = still ? s.y : Mth.lerp(partial, s.py, s.y);
            float a = 1 - (float) s.life / s.max;
            int sz = Math.max(1, Math.round(s.size));
            int col = still ? lerpRgb(s.color, 0x9FB8D8, 0.75F) : s.color;
            g.fill((int) x, (int) y, (int) x + sz, (int) y + sz, argb(col, Math.min(1, a * 1.3F)));
        }
        g.pose().popPose();
    }

    // ------------------------------------------------------------------ 그리기

    public void render(GuiGraphics g, Font font, int width, int height, float partial) {
        if (!active()) {
            return;
        }
        lastW = width;
        lastH = height;
        float tt = t + partial;
        if (frozen()) {
            renderYamato(g, font, width, height, partial);
        } else {
            g.pose().pushPose();
            g.pose().translate(0, 0, 300);
            switch (grade) {
                case COMMON -> renderCommon(g, font, width, height, tt, partial);
                case UNCOMMON -> renderUncommon(g, font, width, height, tt, partial);
                case RARE -> renderRare(g, font, width, height, tt, partial);
                case EPIC -> renderEpic(g, font, width, height, tt, partial);
                default -> renderLegendary(g, font, width, height, tt, partial);
            }
            g.pose().popPose();
        }
        String skip = Component.translatable("rtshop.gacha.fx.skip").getString();
        g.pose().pushPose();
        g.pose().translate(0, 0, 900);
        g.drawString(font, skip, width - font.width(skip) - 6, height - 12, 0x88FFFFFF, false);
        g.pose().popPose();
    }

    private void center(GuiGraphics g, int width, int height, float shake) {
        float sx = shake > 0 ? (rnd.nextFloat() - 0.5F) * 2 * shake : 0;
        float sy = shake > 0 ? (rnd.nextFloat() - 0.5F) * 2 * shake : 0;
        g.pose().translate(width / 2F + sx, height / 2F - 8 + sy, 0);
    }

    private static void flash(GuiGraphics g, int width, int height, int rgb, float a) {
        if (a > 0) g.fill(0, 0, width, height, argb(rgb, Mth.clamp(a, 0, 1)));
    }

    /** 일반: 카드가 위에서 툭 떨어져 통통 튀고, 한 번 뒤집힌다. */
    private void renderCommon(GuiGraphics g, Font font, int width, int height, float tt, float partial) {
        g.fill(0, 0, width, height, argb(0x050505, Math.min(1, tt / 4) * 0.6F));
        g.pose().pushPose();
        center(g, width, height, tt >= 8 && tt < 11 ? 1.5F : 0);
        float drop = tt < 8 ? -(1 - ease(tt / 8F)) * (height / 2F + 50) : tt < 12 ? -Math.abs(Mth.sin((tt - 8) / 4F * Mth.PI)) * 6 : 0;
        if (tt < 14) {
            float flip = tt < 12 ? 1 : 1 - (tt - 12) / 2F;
            float squash = tt >= 8 && tt < 10 ? 1 - 0.12F * (1 - (tt - 8) / 2F) : 1;
            g.pose().pushPose();
            g.pose().translate(0, drop, 20);
            g.pose().scale(Math.max(0.05F, flip) * (2 - squash), squash, 1);
            cardBack(g, font, GRAY, 0, 0);
            g.pose().popPose();
        } else {
            float since = tt - 14;
            ring(g, 20 + since * 6, 2, GRAY, Math.max(0, 0.6F - since / 14F), 24, 0);
            float open = Math.min(1, since / 2F);
            g.pose().pushPose();
            g.pose().scale(Math.max(0.05F, open), 1, 1);
            item(g, 3.5F, 0);
            g.pose().popPose();
            label(g, font, "rtshop.gacha.fx.common", GRAY, since, 1.8F, false);
        }
        particles(g, partial);
        g.pose().popPose();
    }

    /** 고급: 아래에서 떠오르는 카드, 초록빛이 한 번 고동치고 반짝이며 뒤집힌다. */
    private void renderUncommon(GuiGraphics g, Font font, int width, int height, float tt, float partial) {
        g.fill(0, 0, width, height, argb(0x03100A, Math.min(1, tt / 5) * 0.7F));
        g.pose().pushPose();
        center(g, width, height, tt >= 22 && tt < 26 ? 2 : 0);
        if (tt < 22) {
            float rise = (1 - ease(tt / 10F)) * (height / 2F + 40);
            float pulse = tt >= 10 && tt < 18 ? Mth.sin((tt - 10) / 8F * Mth.PI) : 0;
            glow(g, 30 + pulse * 26, GREEN, 0.25F + pulse * 0.4F);
            if (tt > 10) ring(g, 30 + (tt - 10) * 5, 1.5F, GREEN_LIGHT, Math.max(0, 0.6F - (tt - 10) / 16F), 30, tt * 0.05F);
            float shake = tt > 16 ? (tt - 16) * 0.4F : 0;
            g.pose().pushPose();
            g.pose().translate((rnd.nextFloat() - 0.5F) * shake, rise + Mth.sin(tt * 0.3F) * 2, 20);
            float flip = tt < 20 ? 1 : 1 - (tt - 20) / 2F;
            g.pose().scale(Math.max(0.05F, flip), 1, 1);
            cardBack(g, font, GREEN_LIGHT, 0, 0);
            g.pose().popPose();
        } else {
            float since = tt - 22;
            glow(g, 60 + Mth.sin(tt * 0.25F) * 5, GREEN, 0.4F);
            rays(g, 8, tt * 1.4F, 120, 1.5F, GREEN_LIGHT, 0.35F);
            ring(g, 26 + since * 9, 2, GREEN_LIGHT, Math.max(0, 0.8F - since / 12F), 36, 0);
            float pop = since < 8 ? 1 + 0.35F * (1 - since / 8F) * Mth.cos(since * 0.9F) : 1;
            item(g, 3.8F * pop, Mth.sin(tt * 0.12F) * 2);
            label(g, font, "rtshop.gacha.fx.uncommon", GREEN_LIGHT, since, 2.0F, false);
        }
        particles(g, partial);
        g.pose().popPose();
        if (tt >= 22 && tt < 26) flash(g, width, height, GREEN_LIGHT, 0.5F * (1 - (tt - 22) / 4F));
    }

    /** 희귀: 돌며 다가오는 카드, 세 번 올라가는 종소리, 정적, 유리처럼 깨지는 공개. */
    private void renderRare(GuiGraphics g, Font font, int width, int height, float tt, float partial) {
        g.fill(0, 0, width, height, argb(0x020814, Math.min(1, tt / 8) * 0.86F));
        g.pose().pushPose();
        float shake = tt >= RARE_HOLD && tt < RARE_BURST ? (tt - RARE_HOLD) * 0.35F : tt >= RARE_BURST && tt < RARE_BURST + 6 ? 3 * (1 - (tt - RARE_BURST) / 6F) : 0;
        center(g, width, height, shake);
        if (tt < RARE_BURST) {
            float charge = Mth.clamp(tt / RARE_HOLD, 0, 1);
            glow(g, 28 + charge * 40, BLUE, 0.25F + charge * 0.4F);
            for (int b : new int[]{14, 20, 26}) {
                if (tt >= b) ring(g, 24 + (tt - b) * 7, 1.5F, BLUE_LIGHT, Math.max(0, 0.7F - (tt - b) / 12F), 32, b);
            }
            if (tt > 12) rays(g, 10, tt * 3, 30 + charge * 120, 1.5F, BLUE_LIGHT, 0.2F + charge * 0.4F);
            // 돌아가는 카드 (점점 느려지다 멈춘다)
            float spin = tt < RARE_HOLD ? (1 - ease(tt / RARE_HOLD)) * 14 * Mth.PI : 0;
            float sx = Mth.cos(spin);
            float zoom = 0.6F + 0.4F * ease(tt / 14F);
            g.pose().pushPose();
            g.pose().translate(0, Mth.sin(tt * 0.2F) * 2, 20);
            g.pose().scale(zoom * Math.max(0.06F, Math.abs(sx)), zoom, 1);
            float crack = tt >= RARE_HOLD ? (tt - RARE_HOLD) / (RARE_BURST - RARE_HOLD) : 0;
            cardBack(g, font, BLUE_LIGHT, crack, BLUE_LIGHT);
            g.pose().popPose();
        } else {
            float since = tt - RARE_BURST;
            glow(g, 80 + Mth.sin(tt * 0.2F) * 6, BLUE, 0.5F);
            rays(g, 12, tt * 1.2F, 190, 2, BLUE_LIGHT, 0.4F);
            ring(g, 20 + since * 14, 3, BLUE_LIGHT, Math.max(0, 0.9F - since / 10F), 40, 0);
            ring(g, 10 + since * 9, 1.5F, 0xFFFFFF, Math.max(0, 0.7F - since / 10F), 40, 0.3F);
            float pop = since < 10 ? 1 + 0.5F * (1 - since / 10F) * Mth.cos(since * 0.9F) : 1;
            item(g, 4F * pop, Mth.sin(tt * 0.12F) * 2);
            label(g, font, "rtshop.gacha.fx.rare", BLUE_LIGHT, since, 2.3F, true);
        }
        particles(g, partial);
        g.pose().popPose();
        if (tt >= RARE_BURST && tt < RARE_BURST + 8) flash(g, width, height, 0xE6F3FF, 0.9F * (1 - (tt - RARE_BURST) / 8F));
    }

    /** 영웅: 마법진, 심장 박동, 보랏빛 번개, 빨려 들어갔다가 폭발. */
    private void renderEpic(GuiGraphics g, Font font, int width, int height, float tt, float partial) {
        float beat = heartbeat(tt, new int[]{8, 22, 34, 42});
        g.fill(0, 0, width, height, argb(0x0A0414, Math.min(1, tt / 10) * (0.9F + beat * 0.08F)));
        g.pose().pushPose();
        float shake = tt >= EPIC_BOLTS && tt < EPIC_IMPLODE ? 1.5F : tt >= EPIC_BURST && tt < EPIC_BURST + 8 ? 4 * (1 - (tt - EPIC_BURST) / 8F) : beat * 1.2F;
        center(g, width, height, shake);
        if (tt < EPIC_BURST) {
            float charge = Mth.clamp((tt - 6) / (EPIC_IMPLODE - 6F), 0, 1);
            float implode = tt >= EPIC_IMPLODE ? (tt - EPIC_IMPLODE) / (EPIC_BURST - EPIC_IMPLODE) : 0;
            float cr = (92 + beat * 6) * (1 - implode * 0.85F);
            circle(g, cr, tt, PURPLE, 0.55F + beat * 0.3F, (tt - 2) / 26F);
            glow(g, (30 + charge * 50 + beat * 16) * (1 - implode * 0.6F), PURPLE, 0.3F + charge * 0.4F + beat * 0.2F);
            if (tt >= EPIC_BOLTS && tt < EPIC_IMPLODE) {
                for (int k = 0; k < 4; k++) {
                    int start = EPIC_BOLTS + new int[]{0, 5, 9, 12}[k];
                    if (tt >= start && tt < start + 5) {
                        float a = k * 1.7F + 0.6F;
                        bolt(g, Mth.cos(a) * cr, Mth.sin(a) * cr, 0, 0, PURPLE_LIGHT, 1 - (tt - start) / 5F, 31L * k + 7, 1.5F);
                    }
                }
            }
            float amp = charge * charge * 3.5F + beat * 1.5F;
            float lift = -charge * 8 + Mth.sin(tt * 0.15F) * 2;
            float sc = 1 + charge * 0.15F + beat * 0.06F - implode * 0.3F;
            g.pose().pushPose();
            g.pose().translate((rnd.nextFloat() - 0.5F) * 2 * amp, lift + (rnd.nextFloat() - 0.5F) * 2 * amp, 20);
            g.pose().scale(sc, sc, 1);
            float crack = tt > 30 ? (tt - 30) / 30F : 0;
            cardBack(g, font, PURPLE_LIGHT, crack, PURPLE_LIGHT);
            g.pose().popPose();
        } else {
            float since = tt - EPIC_BURST;
            circle(g, 110, tt, PURPLE, 0.3F, 1);
            glow(g, 92 + Mth.sin(tt * 0.2F) * 6, PURPLE, 0.55F);
            rays(g, 14, tt * 1.4F, 220, 2.5F, PURPLE_LIGHT, 0.45F);
            ring(g, 18 + since * 16, 4, PURPLE_LIGHT, Math.max(0, 1 - since / 10F), 48, 0);
            ring(g, 10 + since * 10, 2, 0xFFFFFF, Math.max(0, 0.8F - since / 10F), 48, 0.4F);
            float pop = since < 10 ? 1 + 0.6F * (1 - since / 10F) * Mth.cos(since * 0.9F) : 1;
            item(g, 4.3F * pop, Mth.sin(tt * 0.12F) * 2);
            // 색이 갈라진 등급 글자
            if (since < 10) {
                g.pose().pushPose();
                g.pose().translate(-3 * (1 - since / 10F), 0, -1);
                label(g, font, "rtshop.gacha.fx.epic", 0xFF4AD0, since, 2.6F, true);
                g.pose().popPose();
            }
            label(g, font, "rtshop.gacha.fx.epic", PURPLE_LIGHT, since, 2.6F, true);
        }
        particles(g, partial);
        g.pose().popPose();
        if (tt >= EPIC_BURST && tt < EPIC_BURST + 10) flash(g, width, height, 0xF0DDFF, 1 - (tt - EPIC_BURST) / 10F);
        for (int k = 0; k < 4; k++) {
            int start = EPIC_BOLTS + new int[]{0, 5, 9, 12}[k];
            if (tt >= start && tt < start + 2) flash(g, width, height, 0xE6C2FF, 0.25F);
        }
    }

    /** 전설 (YAMATO 의 앞부분도 이것). */
    private void renderLegendary(GuiGraphics g, Font font, int width, int height, float tt, float partial) {
        if (tt < LEG_DARK) {
            renderEpic(g, font, width, height, tt, partial); // 영웅인 척
            return;
        }
        if (tt < LEG_STRIKE) {
            renderBlackout(g, width, height, tt, partial);
            return;
        }
        boolean still = frozen();
        float beat = 0;
        boolean silent = false;
        boolean gold = true;
        float sinceStrike = tt - LEG_STRIKE;
        g.fill(0, 0, width, height, argb(0x050302, 0.95F));
        // 빛기둥
        if (gold && tt < LEG_BURST + 30) {
            float pw = Math.min(1, (tt - LEG_STRIKE) / 10F) * (tt >= LEG_IMPLODE && tt < LEG_BURST ? 1 - (tt - LEG_IMPLODE) / 6F : 1);
            int cx = width / 2;
            int w = (int) (6 + pw * 34 + Mth.sin(tt * 0.4F) * 2);
            g.fillGradient(cx - w, 0, cx + w, height, argb(GOLD, 0.0F), argb(GOLD_LIGHT, 0.35F * pw));
            g.fillGradient(cx - w / 3, 0, cx + w / 3, height, argb(0xFFFFFF, 0.1F * pw), argb(0xFFFFFF, 0.55F * pw));
        }
        g.pose().pushPose();
        float shake = 0;
        if (tt >= LEG_STRIKE && tt < LEG_STRIKE + 12) shake = 6 * (1 - (tt - LEG_STRIKE) / 12F);
        if (tt >= LEG_IMPLODE && tt < LEG_BURST) shake = 2;
        if (tt >= LEG_BURST && tt < LEG_BURST + 10) shake = Math.max(shake, 6 * (1 - (tt - LEG_BURST) / 10F));
        if (silent) shake = 0.6F;
        center(g, width, height, still ? 0 : shake + beat);
        if (tt < LEG_BURST) {
            int main = gold ? GOLD : PURPLE;
            int light = gold ? GOLD_LIGHT : PURPLE_LIGHT;
            float implode = tt >= LEG_IMPLODE ? (tt - LEG_IMPLODE) / (LEG_BURST - LEG_IMPLODE) : 0;
            if (!silent) {
                float draw = gold ? 1 : (tt - 2) / 26F;
                circle(g, (gold ? 104 : 92) * (1 - implode * 0.85F) + beat * 6, tt, main, gold ? 0.6F : 0.5F + beat * 0.3F, draw);
                float charge = gold ? (tt - LEG_STRIKE) / (float) (LEG_IMPLODE - LEG_STRIKE) : Mth.clamp((tt - 8) / 44F, 0, 1);
                glow(g, (34 + charge * 60 + beat * 14) * (1 - implode * 0.6F), main, 0.3F + charge * 0.45F + beat * 0.2F);
                if (gold) {
                    float ra = tt * 4.5F;
                    rays(g, 16, ra, (40 + charge * 160) * (1 - implode), 2.5F, light, 0.3F + charge * 0.4F);
                    rays(g, 8, -ra * 0.7F, (30 + charge * 120) * (1 - implode), 4, main, 0.3F);
                    ring(g, 70 + Mth.sin(tt * 0.1F) * 4, 2, GOLD_LIGHT, 0.5F * (1 - implode), 30, tt * 0.06F);
                    ring(g, 54, 1.5F, GOLD, 0.45F * (1 - implode), 22, -tt * 0.09F);
                }
            }
            if (tt >= LEG_STRIKE && tt < LEG_STRIKE + 8) {
                float f = 1 - (tt - LEG_STRIKE) / 8F;
                bolt(g, -10, -height / 2F - 10, 0, 0, GOLD_LIGHT, f, 99, 3F);
                bolt(g, 30, -height / 2F - 10, 0, 0, 0xFFFFFF, 0.8F * f, 51, 2F);
                bolt(g, -width / 2F + 20, -height / 2F, 0, 0, GOLD, 0.6F * f, 13, 1.5F);
                bolt(g, width / 2F - 20, -height / 2F, 0, 0, GOLD, 0.6F * f, 77, 1.5F);
            }
            if (sinceStrike < 16) {
                // 튀어나오는 순간의 충격파
                ring(g, 20 + sinceStrike * 22, 7 * (1 - sinceStrike / 16F) + 1, GOLD_LIGHT, 1 - sinceStrike / 16F, 64, 0);
                ring(g, 10 + sinceStrike * 14, 3, 0xFFFFFF, 0.9F * (1 - sinceStrike / 16F), 64, 0.2F);
                rays(g, 24, sinceStrike * 2, 60 + sinceStrike * 28, 2.5F, GOLD_LIGHT, 0.7F * (1 - sinceStrike / 16F));
            }
            float amp = still ? 0 : silent ? 0.8F : (gold ? 1.5F + implode * 3 : Mth.clamp((tt - 8) / 44F, 0, 1) * 3) + beat * 1.5F;
            float sc = 1 + (gold ? (tt - LEG_STRIKE) / (float) (LEG_IMPLODE - LEG_STRIKE) * 0.3F - implode * 0.4F : 0.1F) + beat * 0.05F;
            // 어둠을 뚫고 튀어나온다: 크게 → 출렁이며 제자리
            if (sinceStrike < 14) sc *= sinceStrike < 2 ? 0.3F + sinceStrike * 0.85F : 1 + 0.45F * (1 - (sinceStrike - 2) / 12F) * Mth.cos((sinceStrike - 2) * 0.9F);
            g.pose().pushPose();
            g.pose().translate((rnd.nextFloat() - 0.5F) * 2 * amp, Mth.sin(tt * 0.15F) * 2 + (rnd.nextFloat() - 0.5F) * 2 * amp, 20);
            g.pose().scale(sc, sc, 1);
            float crack = gold ? Mth.clamp((tt - LEG_STRIKE) / 30F, 0.15F, 1) : tt > 34 ? (tt - 34) / 36F : 0;
            cardBack(g, font, silent ? 0x5A4A3A : light, crack, gold ? 0xFFF6C0 : PURPLE_LIGHT);
            g.pose().popPose();
        } else {
            float since = tt - LEG_BURST;
            circle(g, 120, tt, GOLD, 0.35F, 1);
            glow(g, 100 + Mth.sin(tt * 0.2F) * 6, GOLD, 0.6F);
            rays(g, 18, tt * 1.6F, 280, 3, GOLD_LIGHT, 0.45F);
            rays(g, 9, -tt * 1.1F, 220, 6, GOLD, 0.25F);
            ring(g, 20 + since * 18, 5, GOLD_LIGHT, Math.max(0, 1 - since / 12F), 56, 0);
            ring(g, 12 + since * 12, 2, 0xFFFFFF, Math.max(0, 0.9F - since / 12F), 56, 0.3F);
            for (int i = 0; i < 8; i++) {
                float a = tt * 0.08F + i * Mth.TWO_PI / 8;
                g.pose().pushPose();
                g.pose().translate(Mth.cos(a) * 62, Mth.sin(a) * 62, 30);
                g.pose().mulPose(Axis.ZP.rotationDegrees(45 + tt * 6));
                g.fill(-3, -3, 3, 3, 0xFFFFF4C8);
                g.pose().popPose();
            }
            float pop = since < 12 ? 1 + 0.7F * (1 - since / 12F) * Mth.cos(since * 0.9F) : 1;
            item(g, 4.8F * pop, Mth.sin(tt * 0.12F) * 2);
            int col = lerpRgb(GOLD, 0xFFFFFF, 0.5F + 0.5F * Mth.sin(tt * 0.35F));
            label(g, font, "rtshop.gacha.fx.legendary", col, since, 3.0F, true);
        }
        particles(g, partial);
        g.pose().popPose();
        if (tt >= LEG_STRIKE && tt < LEG_STRIKE + 7) flash(g, width, height, 0xFFF8E0, 0.9F * (1 - (tt - LEG_STRIKE) / 7F) * (1 - (tt - LEG_STRIKE) / 7F));
        if (tt >= LEG_BURST && tt < LEG_BURST + 12) flash(g, width, height, 0xFFF3C8, 1 - (tt - LEG_BURST) / 12F);
    }

    /** 전설 암전: 모든 것이 꺼지고, 어둠 한가운데 금빛 금이 세로로 벌어진다. */
    private void renderBlackout(GuiGraphics g, int width, int height, float tt, float partial) {
        float d = tt - LEG_DARK;
        g.fill(0, 0, width, height, 0xFF000000);
        // 꺼지는 순간 남는 잔상
        if (d < 4) {
            g.pose().pushPose();
            g.pose().translate(width / 2F, height / 2F - 8, 0);
            glow(g, 60 * (1 - d / 4F), PURPLE_LIGHT, 0.5F * (1 - d / 4F));
            g.pose().popPose();
        }
        if (d >= 6) {
            float open = ease((d - 6) / (LEG_STRIKE - LEG_DARK - 6F));
            int cx = width / 2, cy = height / 2 - 8;
            float h = 6 + open * 70;
            float w = 0.5F + open * open * 6;
            g.pose().pushPose();
            g.pose().translate(cx, cy, 0);
            glow(g, 10 + open * 40, GOLD, 0.15F + open * 0.35F);
            // 지그재그 금
            RandomSource r = RandomSource.create(31);
            float py = -h;
            float px = 0;
            for (int i = 0; i < 8; i++) {
                float ny = -h + (i + 1) * (2 * h / 8);
                float nx = (r.nextFloat() - 0.5F) * 6 * (1 - Math.abs(ny) / h);
                bar(g, px, py, nx, ny, w * 3 + 2, argb(GOLD, 0.35F));
                bar(g, px, py, nx, ny, w + 0.6F, argb(GOLD_LIGHT, 0.95F));
                px = nx;
                py = ny;
            }
            g.pose().popPose();
        }
        g.pose().pushPose();
        g.pose().translate(width / 2F, height / 2F - 8, 0);
        particles(g, partial);
        g.pose().popPose();
    }

    // ------------------------------------------------------------------ YAMATO

    private void renderYamato(GuiGraphics g, Font font, int width, int height, float partial) {
        float tt = t + partial;
        if (t < YM_BREAK) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 300);
            if (t < YM_START + 15) {
                renderDraw(g, width, height, tt);
            } else {
                renderSlashes(g, font, width, height, tt);
            }
            g.pose().popPose();
            if (t == YM_BREAK - 1 && !captured) {
                // 깨지기 직전의 베인 화면을 찍어 둔다 (조각의 그림)
                g.flush();
                captured = true;
                capture(width, height);
            }
            return;
        }
        if (shards.isEmpty()) {
            capture(width, height); // 건너뛰어 바로 온 경우: 그림 없이 조각만
        }
        if (t < YM_BOOM) {
            renderGray(g, width, height, tt, partial);
            return;
        }
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        float since = tt - YM_BOOM;
        // 허공
        g.fill(0, 0, width, height, 0xFF02050C);
        int cx = width / 2;
        int cy = height / 2 - 8;
        g.fillGradient(0, 0, width, height / 2, argb(YM_DEEP, 0.0F), argb(YM_DEEP, 0.9F));
        g.fillGradient(0, height / 2, width, height, argb(YM_DEEP, 0.9F), argb(YM_DEEP, 0.0F));
        // 차원의 틈 (가로로 갈라진 푸른 선)
        RandomSource r = RandomSource.create(4242);
        for (int i = 0; i < 9; i++) {
            float y = r.nextFloat() * height;
            float a = 0.18F + 0.12F * Mth.sin(tt * 0.2F + i);
            g.fill(0, (int) y, width, (int) y + 1, argb(YM_BLUE, a * Math.min(1, since / 10F)));
        }
        g.pose().pushPose();
        float shake = since < 12 ? 9 * (1 - since / 12F) : 0;
        center(g, width, height, shake);
        // 폭발: 겹겹의 충격파와 사방으로 뻗는 칼빛
        if (since < 26) {
            float f = 1 - since / 26F;
            ring(g, 20 + since * 26, 10 * f + 1, 0xFFFFFF, f, 72, 0);
            ring(g, 10 + since * 18, 6 * f + 1, YM_CYAN, f, 72, 0.3F);
            ring(g, since * 11, 3, YM_BLUE, f, 72, 0.6F);
            RandomSource rr = RandomSource.create(606);
            for (int i = 0; i < 26; i++) {
                float ang = rr.nextFloat() * Mth.TWO_PI;
                float len = (180 + rr.nextFloat() * 260) * ease(since / 6F);
                float in = len * Math.max(0, (since - 4) / 22F);
                bar(g, Mth.cos(ang) * in, Mth.sin(ang) * in, Mth.cos(ang) * len, Mth.sin(ang) * len, 3 * f + 0.5F, argb(i % 3 == 0 ? 0xFFFFFF : YM_CYAN, 0.9F * f));
            }
        }
        float in = ease(since / 10F);
        glow(g, 60 + in * 40 + Mth.sin(tt * 0.18F) * 6, YM_BLUE, 0.45F * in);
        rays(g, 12, tt * 0.8F, 230 * in, 1.5F, YM_CYAN, 0.25F * in);
        rays(g, 6, -tt * 0.5F, 160 * in, 4F, YM_BLUE, 0.18F * in);
        // 환영검: 하나씩 나타나 칼끝을 안으로 두고 돈다
        int swords = 12;
        for (int i = 0; i < swords; i++) {
            float appear = since - 6 - i * 1.3F;
            if (appear < 0) continue;
            float a = tt * 0.035F + i * Mth.TWO_PI / swords;
            float rr2 = 100 - 26 * (1 - ease(appear / 8F)) + Mth.sin(tt * 0.1F + i) * 3;
            float alpha = Math.min(1, appear / 4F) * (0.75F + 0.25F * Mth.sin(tt * 0.3F + i));
            g.pose().pushPose();
            g.pose().translate(Mth.cos(a) * rr2, Mth.sin(a) * rr2, 30);
            g.pose().mulPose(Axis.ZP.rotation(a - Mth.HALF_PI)); // 칼끝이 야마토를 향한다
            spectralSword(g, alpha);
            g.pose().popPose();
        }
        // 야마토
        float pop = since < 16 ? (since < 3 ? 0 : 1 + 0.6F * (1 - (since - 3) / 13F) * Mth.cos((since - 3) * 0.8F)) : 1;
        if (since >= 3) {
            glow(g, 34, YM_CYAN, 0.35F);
            item(g, 5.4F * pop, -10 + Mth.sin(tt * 0.1F) * 2);
        }
        particles(g, partial);
        g.pose().popPose();
        // 터져 나가는 화면 조각 (흑백에서 색이 돌아오며)
        renderShards(g, tt - YM_BREAK, partial, Math.min(1, since / 5F));
        // 글자: 한 글자씩
        if (t >= YM_TITLE) {
            float ts = tt - YM_TITLE;
            String title = "YAMATO";
            int shown = Math.min(title.length(), (int) (ts / 2.5F) + 1);
            g.flush();
            g.pose().pushPose();
            float scale = 3.2F;
            int ty = Math.min(cy + 50, height - 66);
            g.pose().translate(cx, ty, 500);
            g.pose().scale(scale, scale, 1);
            Component full = RaidText.c(title, RaidText.TITLE);
            int fw = font.width(full);
            int x = -fw / 2;
            for (int i = 0; i < shown; i++) {
                String ch = String.valueOf(title.charAt(i));
                Component c = RaidText.c(ch, RaidText.TITLE);
                float age = ts - i * 2.5F;
                float jitter = age < 4 ? (1 - age / 4F) * 1.5F : 0;
                // 글자가 칼에 베인 듯 위아래로 어긋났다가 붙는다
                float split = age < 5 ? (1 - age / 5F) * 2 : 0;
                g.drawString(font, c, (int) (x + jitter), 0, argb(YM_BLUE, 0.6F), false);
                g.drawString(font, c, x, (int) -split, 0xFF000000 | lerpRgb(0xFFFFFF, YM_CYAN, 0.5F + 0.5F * Mth.sin(tt * 0.3F + i)), false);
                x += font.width(c);
            }
            g.pose().popPose();
            if (ts > 16) {
                float a = Math.min(1, (ts - 16) / 8F);
                g.pose().pushPose();
                g.pose().translate(cx, ty + 9 * scale + 6, 500);
                g.pose().scale(1.5F, 1.5F, 1);
                g.drawString(font, name, -font.width(name) / 2, 0, argb(0xFFFFFF, a), true);
                g.pose().popPose();
                String sub = Component.translatable("rtshop.gacha.fx.yamato_sub").getString();
                g.pose().pushPose();
                g.pose().translate(cx, ty + 9 * scale + 22, 500);
                g.drawString(font, sub, -font.width(sub) / 2, 0, argb(YM_CYAN, a * 0.8F), false);
                g.pose().popPose();
            }
        }
        g.pose().popPose();
        if (since < 8) flash(g, width, height, 0xFFFFFF, (1 - since / 8F) * 0.95F);
        if (since < 14) flash(g, width, height, 0x9FD8FF, (1 - since / 14F) * 0.25F);
    }

    /** 다시 암전 → 칼을 뽑는 소리와 함께 어둠을 가로지르는 칼빛. */
    private void renderDraw(GuiGraphics g, int width, int height, float tt) {
        g.fill(0, 0, width, height, 0xFF000000);
        if (tt < YM_DRAW) return;
        float d = tt - YM_DRAW;
        int cy = height / 2 - 8;
        // 칼날이 왼쪽에서 오른쪽으로 스친다 (0.25초) → '챙' 하는 순간 번쩍
        float sweep = ease(Math.min(1, d / 5F));
        float x = -20 + sweep * (width + 40);
        float fade = d < 5 ? 1 : Math.max(0, 1 - (d - 5) / 6F);
        g.fillGradient(0, cy - 1, (int) x, cy + 1, argb(YM_BLUE, 0), argb(YM_CYAN, 0.9F * fade));
        g.fill(0, cy, (int) x, cy + 1, argb(0xFFFFFF, 0.85F * fade));
        g.fillGradient(0, cy - 6, (int) x, cy + 6, argb(YM_BLUE, 0), argb(YM_BLUE, 0.18F * fade));
        // 칼끝의 반짝임
        g.pose().pushPose();
        g.pose().translate(Math.min(x, width - 10), cy, 10);
        float star = d < 5 ? 1 : Math.max(0, 1 - (d - 5) / 4F);
        float sz = 10 + 30 * (d >= 4 && d < 8 ? 1 - Math.abs(d - 5) / 3F : 0);
        g.fill((int) -sz, 0, (int) sz, 1, argb(0xFFFFFF, star));
        g.fill(0, (int) (-sz / 2), 1, (int) (sz / 2), argb(0xFFFFFF, star));
        glow(g, 8 + sz * 0.4F, YM_CYAN, 0.5F * star);
        g.pose().popPose();
        if (d >= 4.4F && d < 7) flash(g, width, height, 0xCFEFFF, 0.35F * (1 - (d - 4.4F) / 2.6F));
    }

    /** 이 칼이 몇 틱에 베는지 (음악 시간 → 틱). */
    private static float slashTick(int i) {
        double sec = i < SLASH_TIMES.length ? SLASH_TIMES[i] : X_TIMES[i - SLASH_TIMES.length];
        return (float) (YM_START + sec * 20);
    }

    /** 멈춘 전설 장면 위로 점점 빨라지는 차원참. */
    private void renderSlashes(GuiGraphics g, Font font, int width, int height, float tt) {
        // 가장 최근 칼에 맞춰 화면이 튄다
        float shake = 0;
        float lastAge = 99;
        for (int i = 0; i < slashes.size(); i++) {
            float age = tt - slashTick(i);
            if (age < 0) continue;
            boolean big = i >= SLASH_TIMES.length;
            if (age < 3) shake = Math.max(shake, (big ? 12 : 5) * (1 - age / 3F));
            lastAge = Math.min(lastAge, age);
        }
        RandomSource sr = RandomSource.create((long) (tt * 3));
        float ox = (sr.nextFloat() - 0.5F) * 2 * shake, oy = (sr.nextFloat() - 0.5F) * 2 * shake;
        g.pose().pushPose();
        g.pose().translate(ox, oy, 0);
        renderLegendary(g, font, width, height, YM_START - 1, 0); // 멈춘 장면
        g.pose().popPose();
        float progress = Mth.clamp((tt - slashTick(0)) / (YM_BREAK - slashTick(0)), 0, 1);
        // 푸르게 얼어붙은 세상, 벨 때마다 잠깐 밝아진다
        float lit = lastAge < 2 ? 1 - lastAge / 2F : 0;
        g.fill(0, 0, width, height, argb(0x000000, 0.55F - 0.25F * lit));
        g.fill(0, 0, width, height, argb(0x0B2A6A, 0.35F + 0.15F * progress));
        g.pose().pushPose();
        g.pose().translate(width / 2F + ox, height / 2F + oy, 100);
        float len = Mth.sqrt(width * width + height * height) * 0.75F;
        for (int i = 0; i < slashes.size(); i++) {
            float at = slashTick(i);
            if (tt < at) continue;
            float[] s = slashes.get(i);
            boolean big = i >= SLASH_TIMES.length;
            float age = tt - at;
            float px = s[1] * width, py = s[2] * height;
            float cx = Mth.cos(s[0]), sy = Mth.sin(s[0]);
            float x0 = px - cx * len, y0 = py - sy * len;
            float grow = Math.min(1, age / (big ? 1.2F : 0.6F));
            float x1 = x0 + (2 * len * cx) * grow, y1 = y0 + (2 * len * sy) * grow;
            float fresh = Math.max(0, 1 - age / (big ? 8F : 5F));
            float w = (big ? 16 : 9) * fresh + (big ? 3 : 1.5F);
            bar(g, x0, y0, x1, y1, w * 2.2F, argb(YM_BLUE, 0.18F + 0.25F * fresh));
            bar(g, x0, y0, x1, y1, w, argb(YM_CYAN, 0.35F + 0.4F * fresh));
            bar(g, x0, y0, x1, y1, 1 + (big ? 2.5F : 1.4F) * fresh, argb(0xFFFFFF, 0.95F));
            // 공간이 둥글게 일그러지는 '차원참' 거품
            if (s[3] > 0 && age < 10) {
                float f = 1 - age / 10F;
                float br = 8 + ease(age / 4F) * 34;
                g.pose().pushPose();
                g.pose().translate(px, py, 0);
                g.fill((int) -br, (int) -br, (int) br, (int) br, argb(0x000814, 0.35F * f));
                ring(g, br, 2.5F * f + 0.5F, YM_CYAN, f, 40, s[0]);
                ring(g, br * 0.6F, 1, 0xFFFFFF, 0.7F * f, 30, -s[0]);
                for (int k = 0; k < 4; k++) {
                    float ka = s[0] + k * 0.9F;
                    bar(g, -Mth.cos(ka) * br, -Mth.sin(ka) * br, Mth.cos(ka) * br, Mth.sin(ka) * br, 1.2F, argb(0xFFFFFF, 0.8F * f));
                }
                g.pose().popPose();
            }
        }
        g.pose().popPose();
        // 벨 때마다 번쩍 (X 자는 크게)
        for (int i = 0; i < slashes.size(); i++) {
            float age = tt - slashTick(i);
            boolean big = i >= SLASH_TIMES.length;
            if (age >= 0 && age < 2) flash(g, width, height, big ? 0xFFFFFF : 0xDDF4FF, (big ? 0.6F : 0.16F) * (1 - age / 2F));
        }
        // 마지막 순간: 화면 가장자리부터 하얗게 질린다
        if (tt > YM_BREAK - 3) flash(g, width, height, 0xFFFFFF, 0.3F * (tt - (YM_BREAK - 3)) / 3F);
    }

    /** 흑백으로 멈춘 세상: 조각이 천천히 벌어지고, 한가운데서 칼을 천천히 칼집에 넣는다. */
    private void renderGray(GuiGraphics g, int width, int height, float tt, float partial) {
        float since = tt - YM_BREAK;
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        // 조각 사이로 보이는 하얀 허공 (흑백 반전된 세계)
        g.fill(0, 0, width, height, 0xFFE8E8E8);
        g.fillGradient(0, 0, width, height / 2, 0xFFFFFFFF, 0xFFD0D0D0);
        g.fillGradient(0, height / 2, width, height, 0xFFD0D0D0, 0xFF9A9A9A);
        g.pose().popPose();
        // 깨진 직후 한 순간은 아직 색이 남아 있다
        renderShards(g, since, partial, since < 3 ? 1 - since / 3F : 0);
        // 칼집에 넣는 칼 (가운데)
        g.flush();
        g.pose().pushPose();
        g.pose().translate(width / 2F, height / 2F + 34, 520);
        float k = Mth.clamp((tt - YM_SHEATHE) / (YM_CLICK - YM_SHEATHE), 0, 1);
        float slide = k * k * k; // 천천히 → 마지막에 단숨에
        float appear = Mth.clamp((tt - YM_BREAK - 2) / 5F, 0, 1);
        int ink = argb(0x0A0A0A, appear);
        // 칼이 잘 보이도록 뒤에 옅은 빛의 띠
        g.fillGradient(-200, -16, 210, 0, argb(0xFFFFFF, 0), argb(0xFFFFFF, 0.75F * appear));
        g.fillGradient(-200, 0, 210, 16, argb(0xFFFFFF, 0.75F * appear), argb(0xFFFFFF, 0));
        // 칼집 (왼쪽, 입구 x=-10). 칼은 오른쪽으로 뽑혀 있다가 왼쪽으로 미끄러져 들어간다
        float tsuba = 140 - 150 * slide;
        float tipVisible = Math.max(-10, tsuba - 150);
        if (tsuba > tipVisible) {
            g.fill((int) tipVisible, -2, (int) tsuba, 2, argb(0x0A0A0A, appear * 0.85F)); // 칼날 테두리
            g.fill((int) tipVisible, -1, (int) tsuba, 0, argb(0xFFFFFF, appear));        // 칼날 빛
        }
        g.fill((int) tsuba, -7, (int) tsuba + 4, 7, ink);          // 코등이
        g.fill((int) tsuba + 4, -2, (int) tsuba + 50, 2, ink);      // 손잡이
        for (int i = 0; i < 6; i++) g.fill((int) tsuba + 8 + i * 7, -3, (int) tsuba + 11 + i * 7, 3, ink);
        g.fill(-150, -3, -10, 3, ink);                              // 칼집
        g.fill(-152, -2, -150, 2, ink);
        g.fill(-12, -4, -10, 4, ink);                               // 칼집 입구
        g.pose().popPose();
        if (since < 3) flash(g, width, height, 0xFFFFFF, 0.8F * (1 - since / 3F));
        // '딸깍': 칼집 입구에서 작은 섬광
        float c = tt - YM_CLICK;
        if (c >= 0) {
            g.pose().pushPose();
            g.pose().translate(width / 2F - 10, height / 2F + 34, 530);
            ring(g, 4 + c * 10, 1.5F, 0x000000, Math.max(0, 1 - c / 2F), 32, 0);
            g.pose().popPose();
        }
    }

    /** 푸른 환영검 (칼끝이 위). */
    private static void spectralSword(GuiGraphics g, float alpha) {
        int edge = argb(YM_CYAN, alpha);
        int core = argb(0xFFFFFF, alpha * 0.9F);
        int glow = argb(YM_BLUE, alpha * 0.35F);
        g.fill(-4, -22, 4, 16, glow);
        g.fill(-1, -20, 1, 10, core);
        g.fill(-2, -18, -1, 10, edge);
        g.fill(1, -18, 2, 10, edge);
        g.fill(-1, -24, 1, -20, edge);
        g.fill(-6, 10, 6, 12, edge);
        g.fill(-1, 12, 1, 18, edge);
    }

    /** 바로 앞 프레임을 찍어 삼각형 조각으로 나눈다. */
    private void capture(int width, int height) {
        shards.clear();
        if (!captured) {
            shot = null;
            shotId = null;
        } else {
            try {
                Minecraft mc = Minecraft.getInstance();
                NativeImage img = Screenshot.takeScreenshot(mc.getMainRenderTarget());
                // 흑백 사본 (대비를 조금 올린다)
                NativeImage gray = new NativeImage(img.getWidth(), img.getHeight(), false);
                for (int y = 0; y < img.getHeight(); y++) {
                    for (int x = 0; x < img.getWidth(); x++) {
                        int c = img.getPixelRGBA(x, y); // ABGR
                        float l = 0.3F * (c & 255) + 0.59F * ((c >> 8) & 255) + 0.11F * ((c >> 16) & 255);
                        int v = Mth.clamp((int) ((l - 110) * 1.35F + 128), 0, 255);
                        gray.setPixelRGBA(x, y, 0xFF000000 | (v << 16) | (v << 8) | v);
                    }
                }
                shot = new DynamicTexture(img);
                shotId = mc.getTextureManager().register("rtshop_yamato_break", shot);
                shotGray = new DynamicTexture(gray);
                grayId = mc.getTextureManager().register("rtshop_yamato_gray", shotGray);
            } catch (Throwable ex) {
                shot = shotGray = null;
                shotId = grayId = null;
            }
        }
        // 흔들린 격자 → 삼각형 (베기 선 근처가 먼저 깨진다)
        int cols = 20, rows = 12;
        float[][] px = new float[cols + 1][rows + 1];
        float[][] py = new float[cols + 1][rows + 1];
        RandomSource r = RandomSource.create(777);
        for (int i = 0; i <= cols; i++) {
            for (int j = 0; j <= rows; j++) {
                boolean edge = i == 0 || j == 0 || i == cols || j == rows;
                px[i][j] = (float) i / cols * width + (edge ? 0 : (r.nextFloat() - 0.5F) * width / cols * 0.8F);
                py[i][j] = (float) j / rows * height + (edge ? 0 : (r.nextFloat() - 0.5F) * height / rows * 0.8F);
            }
        }
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                boolean flip = ((i + j) & 1) == 0;
                float[][] tri = flip
                        ? new float[][]{{px[i][j], py[i][j]}, {px[i + 1][j], py[i + 1][j]}, {px[i + 1][j + 1], py[i + 1][j + 1]},
                        {px[i][j], py[i][j]}, {px[i + 1][j + 1], py[i + 1][j + 1]}, {px[i][j + 1], py[i][j + 1]}}
                        : new float[][]{{px[i][j], py[i][j]}, {px[i + 1][j], py[i + 1][j]}, {px[i][j + 1], py[i][j + 1]},
                        {px[i + 1][j], py[i + 1][j]}, {px[i + 1][j + 1], py[i + 1][j + 1]}, {px[i][j + 1], py[i][j + 1]}};
                for (int k = 0; k < 2; k++) {
                    Shard s = new Shard();
                    for (int v = 0; v < 3; v++) {
                        s.x[v] = tri[k * 3 + v][0];
                        s.y[v] = tri[k * 3 + v][1];
                    }
                    float mx = (s.x[0] + s.x[1] + s.x[2]) / 3, my = (s.y[0] + s.y[1] + s.y[2]) / 3;
                    float dx = mx - width / 2F, dy = my - height / 2F;
                    float d = Mth.sqrt(dx * dx + dy * dy) + 1;
                    s.dx = dx / d;
                    s.dy = dy / d;
                    s.speed = 9 + r.nextFloat() * 14 + (1 - d / Math.max(width, height)) * 8;
                    // 흑백 속에서 천천히 벌어질 때의 속도 (0.06 배로 움직인다)
                    s.vx = s.dx * (4 + r.nextFloat() * 8) + (r.nextFloat() - 0.5F) * 4;
                    s.vy = s.dy * (4 + r.nextFloat() * 8) + (r.nextFloat() - 0.5F) * 4;
                    s.vr = (r.nextFloat() - 0.5F) * 0.12F;
                    s.delay = (int) (d / Math.max(width, height) * 4);
                    shards.add(s);
                }
            }
        }
    }

    /** since: 깨진 뒤 틱. colorMix: 흑백 조각 위에 원래 색 조각을 얼마나 겹칠지. */
    private void renderShards(GuiGraphics g, float since, float partial, float colorMix) {
        float boom = since - (YM_BOOM - YM_BREAK);
        float fade = boom < 0 ? 1 : 1 - boom / 24F;
        if (shards.isEmpty() || fade <= 0) return;
        g.flush();
        Matrix4f m = g.pose().last().pose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        float W = Math.max(1, lastW), H = Math.max(1, lastH);
        float speed = boom < 0 ? 0.06F : 1;
        float shrink = boom < 0 ? 0.975F : 0.92F; // 조각 사이 틈
        for (int pass = 0; pass < 2; pass++) {
            ResourceLocation id = pass == 0 ? (grayId != null ? grayId : shotId) : shotId;
            float a = pass == 0 ? fade : fade * colorMix;
            if (pass == 1 && (a <= 0.01F || shotId == null)) break;
            boolean tex = id != null;
            if (tex) {
                RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
                RenderSystem.setShaderTexture(0, id);
            } else {
                RenderSystem.setShader(GameRenderer::getPositionColorShader);
            }
            BufferBuilder bb = Tesselator.getInstance().getBuilder();
            bb.begin(VertexFormat.Mode.TRIANGLES, tex ? DefaultVertexFormat.POSITION_COLOR_TEX : DefaultVertexFormat.POSITION_COLOR);
            for (Shard s : shards) {
                boolean moving = since >= s.delay;
                float mx = (s.x[0] + s.x[1] + s.x[2]) / 3, my = (s.y[0] + s.y[1] + s.y[2]) / 3;
                float lag = moving ? partial * speed : 0;
                float ox = moving ? s.cx + s.vx * lag : 0, oy = moving ? s.cy + s.vy * lag : 0;
                float rot = moving ? s.rot + s.vr * lag : 0;
                float cos = Mth.cos(rot), sin = Mth.sin(rot);
                for (int v = 0; v < 3; v++) {
                    float lx = (s.x[v] - mx) * shrink, ly = (s.y[v] - my) * shrink;
                    float x = mx + lx * cos - ly * sin + ox;
                    float y = my + lx * sin + ly * cos + oy;
                    if (tex) {
                        bb.vertex(m, x, y, 450 + pass).color(1F, 1F, 1F, a).uv(s.x[v] / W, s.y[v] / H).endVertex();
                    } else {
                        bb.vertex(m, x, y, 450 + pass).color(0.55F, 0.55F, 0.58F, a * 0.9F).endVertex();
                    }
                }
            }
            BufferUploader.drawWithShader(bb.end());
        }
        RenderSystem.enableCull();
        // 조각 가장자리의 빛 (터진 뒤에만: 흑백일 때는 색이 없다)
        if (boom < 0) return;
        g.pose().pushPose();
        g.pose().translate(0, 0, 460);
        for (Shard s : shards) {
            if (since < s.delay) continue;
            float mx = (s.x[0] + s.x[1] + s.x[2]) / 3 + s.cx, my = (s.y[0] + s.y[1] + s.y[2]) / 3 + s.cy;
            if ((s.delay + (int) mx) % 3 == 0) g.fill((int) mx, (int) my, (int) mx + 2, (int) my + 2, argb(YM_CYAN, fade));
        }
        g.pose().popPose();
    }

    /** 심장 박동 순간에 1 로 튀었다가 빠르게 줄어드는 값. */
    private static float heartbeat(float tt, int[] beats) {
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

package com.rtshop.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 이야기 연출 (화면 위에 겹쳐 그린다, 조작은 막지 않는다):
 * <ul>
 *   <li>title  : 장 제목 화면 — 영화처럼 위아래 검은 띠, 장 번호와 제목(명조), 장식 줄, 테마 색 빛</li>
 *   <li>boss   : 보스 등장 — 검은 띠, 보스 테마의 효과(불티·눈보라·모래바람·전기·물방울·공허 룬·저주 안개·번개·환영),
 *                화면 흔들림, 보스 이름과 별칭, 테마 소리</li>
 *   <li>vision : 환영 — 화면이 어두워지고 테두리에 불티, 가운데에 한두 줄의 목소리</li>
 *   <li>line   : 자막 한 줄 (말한 사람 + 대사) — 아래쪽</li>
 * </ul>
 * 모두 시간의 함수라 점검에서 원하는 순간을 멈춰 볼 수 있다 (timeForTest).
 */
public final class Cinematic {
    private Cinematic() {
    }

    private static JsonObject cur;
    private static long startAt;
    /** 점검용: 0 이상이면 이 시각(초)으로 고정. */
    public static float timeForTest = -1;

    public static void play(String json) {
        try {
            cur = JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception e) {
            return;
        }
        startAt = System.currentTimeMillis();
        // 보스 등장: 서버가 보스의 엔티티 번호를 보냈으면 카메라가 보스 곁에서 움직인다 (3.61, client.json 의 bossCamera)
        BossCam.stop();
        if ("boss".equals(str("kind", "")) && cur.has("entity")) {
            try {
                BossCam.start(cur.get("entity").getAsInt(), duration());
            } catch (Throwable ignored) {
            }
        }
        String snd = str("sound", "");
        Minecraft mc = Minecraft.getInstance();
        for (String s : snd.split(",")) {
            s = s.trim();
            if (s.isEmpty()) continue;
            SoundEvent ev = ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.tryParse(s));
            if (ev != null) mc.getSoundManager().play(SimpleSoundInstance.forUI(ev, 1.0F, 0.9F));
        }
    }

    public static boolean active() {
        return cur != null && time() < duration();
    }

    /** 창을 닫고 시작한 연출(퀘스트를 받을 때의 장 제목·환영 등)인가: 도는 동안에는 다른 창이 그 위로 열리지 않게 미룬다. */
    public static boolean holdsScreens() {
        return active() && cur.has("closeScreen") && cur.get("closeScreen").getAsBoolean();
    }

    public static String kindForTest() {
        return cur == null ? "" : str("kind", "");
    }

    private static String str(String k, String d) {
        return cur != null && cur.has(k) ? cur.get(k).getAsString() : d;
    }

    private static float time() {
        return timeForTest >= 0 ? timeForTest : (System.currentTimeMillis() - startAt) / 1000F;
    }

    private static float duration() {
        return cur != null && cur.has("seconds") ? cur.get("seconds").getAsFloat() : switch (str("kind", "")) {
            case "boss" -> 5.0F;
            case "title" -> 5.5F;
            case "vision" -> 6.0F;
            case "names" -> 3.2F + 1.3F * Math.max(1, str("text", "").split("\n").length);
            default -> 4.0F;
        };
    }

    /** 화면 흔들림 (카메라 각도에 더한다). */
    public static float[] shake() {
        if (!active() || !"boss".equals(str("kind", ""))) return new float[]{0, 0};
        float t = time();
        float s = 0;
        int pulses = cur.has("shake") ? cur.get("shake").getAsInt() : 2;
        for (int i = 0; i < pulses; i++) {
            float at = 0.55F + i * 0.45F;
            float d = t - at;
            if (d >= 0 && d < 0.35F) s = Math.max(s, (1 - d / 0.35F));
        }
        if (s <= 0) return new float[]{0, 0};
        float a = 1.6F * s;
        return new float[]{(float) Math.sin(t * 90) * a, (float) Math.cos(t * 73) * a * 0.7F};
    }

    private static int col(String k, int d) {
        if (cur == null || !cur.has(k)) return d;
        try {
            return (int) Long.parseLong(cur.get(k).getAsString().replace("#", ""), 16);
        } catch (Exception e) {
            return d;
        }
    }

    // ------------------------------------------------------------------ 그리기

    public static void render(GuiGraphics g, int sw, int sh) {
        Minecraft mc = Minecraft.getInstance();
        if (!active() || mc.options.hideGui) {
            return;
        }
        float t = time(), dur = duration();
        g.pose().pushPose();
        g.pose().translate(0, 0, 500);
        try {
            switch (str("kind", "")) {
                case "title" -> title(g, mc.font, sw, sh, t, dur);
                case "boss" -> boss(g, mc.font, sw, sh, t, dur);
                case "vision" -> vision(g, mc.font, sw, sh, t, dur);
                case "names" -> names(g, mc.font, sw, sh, t, dur);
                default -> line(g, mc.font, sw, sh, t, dur);
            }
        } catch (Throwable e) {
            Fx2D.abort();
        }
        g.pose().popPose();
    }

    private static float fadeInOut(float t, float dur, float in, float out) {
        return Math.max(0, Math.min(1, Math.min(t / in, (dur - t) / out)));
    }

    /** 영화처럼 위아래에서 내려오는 검은 띠. */
    private static void bars(GuiGraphics g, int sw, int sh, float k) {
        int b = Math.round(sh * 0.12F * Fx2D.ease(k));
        if (b <= 0) return;
        g.fill(0, 0, sw, b, 0xFF000000);
        g.fill(0, sh - b, sw, sh, 0xFF000000);
        g.fill(0, b, sw, b + 1, 0x40D4A85A);
        g.fill(0, sh - b - 1, sw, sh - b, 0x40D4A85A);
    }

    private static void title(GuiGraphics g, Font font, int sw, int sh, float t, float dur) {
        float k = fadeInOut(t, dur, 0.7F, 0.9F);
        int theme = col("color", 0xFF7A20);
        bars(g, sw, sh, k);
        Fx2D.begin(g, false);
        Fx2D.rect(0, 0, sw, sh, Fx2D.rgba(0x000000, 0.35F * k), Fx2D.rgba(0x000000, 0.35F * k), Fx2D.rgba(0x000000, 0.55F * k), Fx2D.rgba(0x000000, 0.55F * k));
        Fx2D.end();
        Fx2D.begin(g, true);
        Fx2D.glow(sw / 2F, sh / 2F, sw * 0.45F, 46, Fx2D.rgba(theme, 0.30F * k), Fx2D.rgba(theme, 0));
        drift(g, sw, sh, t, theme, 40, k, str("theme", "ember"));
        Fx2D.end();
        float rise = (1 - Fx2D.easeOut(Math.min(1, t / 1.2F))) * 8;
        String chap = str("chapter", "");
        String name = str("title", "");
        String sub = str("subtitle", "");
        int a = Math.round(255 * k);
        if (a < 8) return;
        if (!chap.isEmpty()) {
            String c = spaced(chap);
            RaidText.center(g, font, c, RaidText.SERIF, sw / 2, Math.round(sh / 2F - 34 + rise), (a << 24) | 0xC9A062);
        }
        g.pose().pushPose();
        g.pose().translate(sw / 2F, sh / 2F - 16 + rise, 0);
        g.pose().scale(1.5F, 1.5F, 1);
        int w = RaidText.w(font, name, RaidText.TITLE);
        RaidText.draw(g, font, name, RaidText.TITLE, -w / 2 + 1, 1, (a << 24) | 0x1A0804);
        RaidText.draw(g, font, name, RaidText.TITLE, -w / 2, 0, (a << 24) | 0xF3D9A0);
        g.pose().popPose();
        float lw = Math.min(sw * 0.3F, 160) * Fx2D.easeOut(Math.min(1, t / 1.0F));
        RaidFx.hairline(g, sw / 2F - lw, sw / 2F + lw, sh / 2F + 14, 0xD4A85A, 0.9F * k);
        Fx2D.begin(g, false);
        Fx2D.diamond(sw / 2F, sh / 2F + 14.5F, 2.6F, 2.6F, 0, (a << 24) | 0xD4A85A);
        Fx2D.end();
        if (!sub.isEmpty()) {
            RaidText.center(g, font, sub, RaidText.SANS, sw / 2, Math.round(sh / 2F + 22), (a << 24) | 0xE0D6C4);
        }
    }

    private static void boss(GuiGraphics g, Font font, int sw, int sh, float t, float dur) {
        float k = fadeInOut(t, dur, 0.5F, 0.8F);
        int theme = col("color", 0xFF5A14);
        String th = str("theme", "ember");
        bars(g, sw, sh, k);
        // 테마 색 안개(가장자리부터)
        Fx2D.begin(g, false);
        int edge = Fx2D.rgba(theme, 0.45F * k), mid = Fx2D.rgba(theme, 0);
        float e = Math.min(sw, sh) * 0.22F;
        Fx2D.rect(0, 0, e, sh, edge, mid, mid, edge);
        Fx2D.rect(sw - e, 0, sw, sh, mid, edge, edge, mid);
        Fx2D.rect(0, 0, sw, e, edge, edge, mid, mid);
        Fx2D.rect(0, sh - e, sw, sh, mid, mid, edge, edge);
        Fx2D.end();
        // 섬광 (등장 순간)
        float flash = Math.max(0, 1 - Math.abs(t - 0.55F) / 0.25F);
        if (flash > 0) {
            Fx2D.begin(g, true);
            Fx2D.rect(0, 0, sw, sh, Fx2D.rgba(theme, 0.35F * flash), Fx2D.rgba(theme, 0.35F * flash), Fx2D.rgba(theme, 0.35F * flash), Fx2D.rgba(theme, 0.35F * flash));
            Fx2D.end();
        }
        Fx2D.begin(g, true);
        drift(g, sw, sh, t, theme, 90, k, th);
        Fx2D.end();
        // 이름 카드
        float c = Math.max(0, Math.min(1, (t - 0.9F) / 0.6F)) * fadeInOut(t, dur, 0.01F, 0.8F);
        if (c <= 0.02F) return;
        int a = Math.round(255 * c);
        String name = str("name", "");
        String epithet = str("epithet", "");
        float cy = sh * 0.66F;
        Fx2D.begin(g, true);
        Fx2D.glow(sw / 2F, cy, sw * 0.4F, 34, Fx2D.rgba(theme, 0.35F * c), Fx2D.rgba(theme, 0));
        Fx2D.end();
        if (!epithet.isEmpty()) {
            RaidText.center(g, font, spaced(epithet), RaidText.SANS, sw / 2, Math.round(cy - 26), (a << 24) | 0xE8DCC8);
        }
        float sc = 1.9F + 0.15F * (1 - Fx2D.easeOut(Math.min(1, (t - 0.9F) / 0.8F)));
        g.pose().pushPose();
        g.pose().translate(sw / 2F, cy - 12, 0);
        g.pose().scale(sc, sc, 1);
        int w = RaidText.w(font, name, RaidText.TITLE);
        if ("illusion".equals(th)) {
            for (int i = 1; i <= 2; i++) {
                float off = (float) Math.sin(t * 3 + i * 2) * 6 * i;
                RaidText.draw(g, font, name, RaidText.TITLE, Math.round(-w / 2F + off), 0, (Math.round(a * 0.3F) << 24) | (theme & 0xFFFFFF));
            }
        }
        RaidText.draw(g, font, name, RaidText.TITLE, -w / 2 + 1, 1, (a << 24) | 0x100404);
        RaidText.draw(g, font, name, RaidText.TITLE, -w / 2, 0, (a << 24) | 0xF8E8C8);
        g.pose().popPose();
        float lw = Math.min(sw * 0.32F, 170) * Fx2D.easeOut(Math.min(1, (t - 1.0F) / 0.8F));
        RaidFx.hairline(g, sw / 2F - lw, sw / 2F + lw, cy + 12, theme, 0.95F * c);
        Fx2D.begin(g, false);
        Fx2D.diamond(sw / 2F, cy + 12.5F, 3F, 3F, 0, (a << 24) | (theme & 0xFFFFFF));
        Fx2D.diamond(sw / 2F, cy + 12.5F, 1.3F, 1.3F, 0, (a << 24) | 0xFFF4E0);
        Fx2D.end();
        String quote = str("quote", "");
        if (!quote.isEmpty()) {
            float q = Math.max(0, Math.min(1, (t - 1.8F) / 0.6F)) * c;
            RaidText.center(g, font, "“" + quote + "”", RaidText.SERIF, sw / 2, Math.round(cy + 20), (Math.round(255 * q) << 24) | 0xCFC2AA);
        }
    }

    private static void vision(GuiGraphics g, Font font, int sw, int sh, float t, float dur) {
        float k = fadeInOut(t, dur, 1.0F, 1.2F);
        int theme = col("color", 0xFF7A20);
        Fx2D.begin(g, false);
        Fx2D.rect(0, 0, sw, sh, Fx2D.rgba(0x050203, 0.72F * k), Fx2D.rgba(0x050203, 0.72F * k), Fx2D.rgba(0x050203, 0.72F * k), Fx2D.rgba(0x050203, 0.72F * k));
        Fx2D.end();
        Fx2D.begin(g, true);
        Fx2D.glow(sw / 2F, sh / 2F, sw * 0.35F, sh * 0.3F, Fx2D.rgba(theme, 0.25F * k), Fx2D.rgba(theme, 0));
        drift(g, sw, sh, t, theme, 50, k, str("theme", "ember"));
        Fx2D.end();
        String speaker = str("speaker", "");
        String[] lines = str("text", "").split("\n");
        int y = sh / 2 - lines.length * 7;
        int a = Math.round(255 * k);
        if (!speaker.isEmpty()) {
            RaidText.center(g, font, spaced(speaker), RaidText.SANS, sw / 2, y - 16, (Math.round(a * 0.8F) << 24) | 0xC9A062);
        }
        for (int i = 0; i < lines.length; i++) {
            float li = Math.max(0, Math.min(1, (t - 0.6F - i * 0.9F) / 0.7F)) * k;
            RaidText.center(g, font, lines[i], RaidText.SERIF, sw / 2, y + i * 14, (Math.round(255 * li) << 24) | 0xF3E6CC);
        }
    }

    /**
     * 이름 적기: 잠재운 존재의 이름을 기록관이 연대기에 한 줄씩 적는다 (불은 이름을 먹는다, 그래서 이름을 적는다).
     * text = 줄바꿈으로 나눈 이름들, speaker = 적는 사람, title = 머리말.
     */
    private static void names(GuiGraphics g, Font font, int sw, int sh, float t, float dur) {
        float k = fadeInOut(t, dur, 0.8F, 1.0F);
        String[] lines = str("text", "").split("\n");
        int pw = Math.min(sw - 60, 250), ph = 54 + lines.length * 18;
        int x = (sw - pw) / 2, y = (sh - ph) / 2;
        Fx2D.begin(g, false);
        Fx2D.rect(0, 0, sw, sh, Fx2D.rgba(0x060302, 0.62F * k), Fx2D.rgba(0x060302, 0.62F * k), Fx2D.rgba(0x060302, 0.62F * k), Fx2D.rgba(0x060302, 0.62F * k));
        // 양피지 한 장
        Fx2D.rect(x, y, x + pw, y + ph, Fx2D.rgba(0xE9D9B0, 0.96F * k), Fx2D.rgba(0xE9D9B0, 0.96F * k), Fx2D.rgba(0xD6C292, 0.96F * k), Fx2D.rgba(0xD6C292, 0.96F * k));
        Fx2D.rect(x + 14, y + 26, x + pw - 14, y + 27, Fx2D.rgba(0x7A5A2E, 0.7F * k), Fx2D.rgba(0x7A5A2E, 0.7F * k), Fx2D.rgba(0x7A5A2E, 0.7F * k), Fx2D.rgba(0x7A5A2E, 0.7F * k));
        Fx2D.end();
        int a = Math.round(255 * k);
        if (a < 8) return;
        RaidText.center(g, font, str("title", ""), RaidText.SANS, sw / 2, y + 10, (a << 24) | 0x6A4A22);
        for (int i = 0; i < lines.length; i++) {
            // 한 글자씩 적힌다
            float start = 1.0F + i * 1.3F;
            int n = Math.max(0, Math.min(lines[i].length(), Math.round((t - start) / 0.9F * lines[i].length())));
            if (n <= 0) continue;
            String part = lines[i].substring(0, n);
            int full = RaidText.w(font, lines[i], RaidText.SERIF);
            int lx = sw / 2 - full / 2, ly = y + 36 + i * 18;
            RaidText.draw(g, font, part, RaidText.SERIF, lx, ly, (a << 24) | 0x2A1A0C);
            if (n < lines[i].length()) { // 펜 끝의 젖은 잉크
                int px = lx + RaidText.w(font, part, RaidText.SERIF);
                g.fill(px + 1, ly + 8, px + 3, ly + 10, (a << 24) | 0x1A0E06);
            }
        }
        String by = str("speaker", "");
        if (!by.isEmpty()) RaidText.right(g, font, by, RaidText.SANS, x + pw - 14, y + ph - 14, (Math.round(a * 0.8F) << 24) | 0x6A4A22);
    }

    private static void line(GuiGraphics g, Font font, int sw, int sh, float t, float dur) {
        float k = fadeInOut(t, dur, 0.3F, 0.5F);
        String speaker = str("speaker", "");
        String text = str("text", "");
        int w = Math.min(sw - 40, Math.max(160, RaidText.w(font, text, RaidText.SANS) + 30));
        int x = (sw - w) / 2, y = sh - 74;
        int a = Math.round(255 * k);
        Fx2D.begin(g, false);
        Fx2D.rect(x, y, x + w, y + 24, Fx2D.rgba(0x000000, 0), Fx2D.rgba(0x000000, 0), Fx2D.rgba(0x000000, 0), Fx2D.rgba(0x000000, 0));
        Fx2D.rect(x, y, x + w / 2F, y + 26, Fx2D.rgba(0x000000, 0), Fx2D.rgba(0x000000, 0.6F * k), Fx2D.rgba(0x000000, 0.6F * k), Fx2D.rgba(0x000000, 0));
        Fx2D.rect(x + w / 2F, y, x + w, y + 26, Fx2D.rgba(0x000000, 0.6F * k), Fx2D.rgba(0x000000, 0), Fx2D.rgba(0x000000, 0), Fx2D.rgba(0x000000, 0.6F * k));
        Fx2D.end();
        if (!speaker.isEmpty()) RaidText.center(g, font, speaker, RaidText.SERIF, sw / 2, y + 2, (a << 24) | 0xE8C27A);
        RaidText.center(g, font, text, RaidText.SANS, sw / 2, y + 14, (a << 24) | 0xF2EADC);
    }

    /** 테마별로 흩날리는 것들. Fx2D.begin(additive) 안에서 부른다. */
    private static void drift(GuiGraphics g, int sw, int sh, float t, int theme, int n, float k, String th) {
        for (int i = 0; i < n; i++) {
            float h1 = Fx2D.hash(i, 1), h2 = Fx2D.hash(i, 2), h3 = Fx2D.hash(i, 3);
            switch (th) {
                case "frost" -> { // 비스듬히 쏟아지는 눈보라
                    float ph = (t * (0.5F + h2) + h1) % 1F;
                    float x = (h1 * sw * 1.3F + ph * sw * 0.6F) % (sw + 40) - 20, y = ph * sh;
                    Fx2D.line(x, y, x - 10 - 8 * h3, y - 6, 1.1F + h3, Fx2D.rgba(0xFFFFFF, 0), Fx2D.rgba(0xE8F6FF, 0.75F * k));
                }
                case "sand" -> { // 옆으로 부는 모래바람
                    float ph = (t * (0.6F + h2 * 0.8F) + h1) % 1F;
                    float x = ph * (sw + 60) - 30, y = h3 * sh;
                    Fx2D.line(x - 22 - 20 * h2, y + 2, x, y, 1.0F + h1, Fx2D.rgba(0xE8C07A, 0), Fx2D.rgba(0xF0D090, 0.6F * k));
                }
                case "abyss" -> { // 떠오르는 물방울
                    float ph = (t * (0.12F + h2 * 0.15F) + h1) % 1F;
                    float x = h1 * sw + (float) Math.sin(t * 2 + i) * 4, y = sh - ph * sh;
                    Fx2D.ring(x, y, 1.2F + h3 * 2.2F, 1.9F + h3 * 2.6F, Fx2D.rgba(0x9FE0FF, 0.55F * k * (1 - ph)), Fx2D.rgba(0x9FE0FF, 0.3F * k * (1 - ph)));
                }
                case "mech" -> { // 전기 잡음 줄과 불꽃
                    if (Fx2D.hash(i, (int) (t * 12)) < 0.12F) {
                        float y = Fx2D.hash(i * 7, (int) (t * 12)) * sh;
                        Fx2D.rect(0, y, sw, y + 1 + h3 * 2, Fx2D.rgba(theme, 0.25F * k), Fx2D.rgba(0xBFEFFF, 0.35F * k), Fx2D.rgba(0xBFEFFF, 0.35F * k), Fx2D.rgba(theme, 0.25F * k));
                    }
                    float ph = (t * (0.3F + h2) + h1) % 1F;
                    Fx2D.glow(h1 * sw, sh * (0.2F + 0.6F * h3) - ph * 30, 1.2F + h2 * 2, Fx2D.rgba(0xBFEFFF, 0.8F * k * (1 - ph)), Fx2D.rgba(theme, 0));
                }
                case "void" -> { // 공허의 룬: 원을 그리며 도는 보랏빛
                    float ang = (float) (i * Math.PI * 2 / n + t * (0.3F + h2 * 0.4F));
                    float r = Math.min(sw, sh) * (0.28F + 0.18F * h1);
                    float x = sw / 2F + (float) Math.cos(ang) * r * 1.4F, y = sh / 2F + (float) Math.sin(ang) * r * 0.7F;
                    Fx2D.diamond(x, y, 2 + h3 * 2, 2 + h3 * 2, ang * 57F, Fx2D.rgba(0xC08BFF, 0.6F * k));
                    Fx2D.glow(x, y, 5, Fx2D.rgba(0x9A5BFF, 0.3F * k), Fx2D.rgba(0x9A5BFF, 0));
                }
                case "curse" -> { // 천천히 흐르는 저주의 안개
                    float ph = (t * 0.06F + h1) % 1F;
                    Fx2D.glow(ph * (sw + 200) - 100, h3 * sh, 30 + h2 * 50, 14 + h2 * 20, Fx2D.rgba(0x6A8A90, 0.16F * k), Fx2D.rgba(0x6A8A90, 0));
                }
                case "storm" -> { // 번개
                    if (i < 2 && Fx2D.hash(i, (int) (t * 4)) < 0.25F) {
                        float x0 = Fx2D.hash(i + 9, (int) (t * 4)) * sw, x = x0, y = 0;
                        for (int s = 0; s < 6; s++) {
                            float nx = x + (Fx2D.hash(s, (int) (t * 4) + i) - 0.5F) * 40, ny = y + sh / 6F;
                            Fx2D.beam(x, y, nx, ny, 2.5F, Fx2D.rgba(0xDDEEFF, 0.8F * k));
                            x = nx;
                            y = ny;
                        }
                    }
                    float ph = (t * (1.4F + h2) + h1) % 1F;
                    Fx2D.line(h1 * sw, ph * sh, h1 * sw - 3, ph * sh - 12, 1F, Fx2D.rgba(0xBFD8FF, 0), Fx2D.rgba(0xBFD8FF, 0.5F * k));
                }
                case "iron" -> { // 떨어지는 녹 가루
                    float ph = (t * (0.15F + h2 * 0.2F) + h1) % 1F;
                    Fx2D.diamond(h1 * sw + (float) Math.sin(t + i) * 5, ph * sh, 1.2F + h3 * 1.6F, 1.2F + h3 * 1.6F, t * 90 + i * 30, Fx2D.rgba(0xB8642A, 0.7F * k));
                }
                case "sun" -> { // 위에서 내리꽂는 햇살
                    if (i < 9) {
                        float x = sw * (0.1F + 0.8F * h1), w = 10 + h2 * 26, a = (0.12F + 0.1F * (float) Math.sin(t * 2 + i)) * k;
                        Fx2D.rect(x - w, 0, x + w, sh, Fx2D.rgba(0xFFE08A, a), Fx2D.rgba(0xFFE08A, a), Fx2D.rgba(0xFFE08A, 0), Fx2D.rgba(0xFFE08A, 0));
                    }
                    float ph = (t * (0.2F + h2 * 0.2F) + h1) % 1F;
                    Fx2D.glow(h1 * sw, ph * sh, 1.5F + h3 * 2, Fx2D.rgba(0xFFF0B0, 0.8F * k), Fx2D.rgba(0xFFB040, 0));
                }
                case "ghost" -> { // 차가운 안개와 떠도는 넋
                    float ph = (t * 0.05F + h1) % 1F;
                    Fx2D.glow(ph * (sw + 200) - 100, sh * (0.5F + 0.5F * h3), 40 + h2 * 60, 16 + h2 * 18, Fx2D.rgba(0x7FD8C8, 0.12F * k), Fx2D.rgba(0x7FD8C8, 0));
                    float py = (t * (0.08F + h2 * 0.1F) + h3) % 1F;
                    Fx2D.glow(h1 * sw + (float) Math.sin(t * 1.5F + i) * 10, sh - py * sh, 2 + h2 * 2, Fx2D.rgba(0xB8FFF0, 0.5F * k * (1 - py)), Fx2D.rgba(0x7FD8C8, 0));
                }
                case "stone" -> { // 흩어지는 돌 부스러기
                    float ph = Math.max(0, t - 0.5F) * (0.6F + h2);
                    float x = sw / 2F + (h1 - 0.5F) * sw * ph, y = sh * 0.5F + (h3 - 0.7F) * sh * 0.5F * ph + 60 * ph * ph;
                    Fx2D.diamond(x, y, 1.5F + h2 * 2.5F, 1.5F + h2 * 2.5F, t * 60 + i * 40, Fx2D.rgba(0xC8C0B0, 0.7F * k * Math.max(0, 1 - ph * 0.6F)));
                }
                case "illusion" -> { // 흔들리는 보랏빛 거울 조각
                    float ph = (t * (0.1F + h2 * 0.1F) + h1) % 1F;
                    float x = h1 * sw + (float) Math.sin(t * 2.2F + i) * 14, y = h3 * sh;
                    Fx2D.diamond(x, y, 2 + h2 * 4, 4 + h2 * 6, ph * 360, Fx2D.rgba(0xB59BFF, 0.35F * k));
                }
                default -> { // ember: 떠오르는 불티
                    float ph = (t * (0.12F + h2 * 0.2F) + h1) % 1F;
                    float x = h1 * sw + (float) Math.sin(t * 1.3F + i) * 8, y = sh - ph * sh;
                    int c = i % 3 == 0 ? 0xFFE08A : (theme & 0xFFFFFF);
                    Fx2D.glow(x, y, 1.4F + h3 * 2.4F, Fx2D.rgba(c, 0.9F * k * (1 - ph)), Fx2D.rgba(c, 0));
                }
            }
        }
    }

    private static String spaced(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (i > 0 && s.charAt(i) != ' ' && s.charAt(i - 1) != ' ') b.append(' ');
            b.append(s.charAt(i));
        }
        return b.toString();
    }
}

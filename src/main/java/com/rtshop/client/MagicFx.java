package com.rtshop.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.rtshop.RtShop;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 마법 뽑기 연출 「룬석 소환진」.
 * 룬석 다섯 개가 원을 그리며 돌고, 가운데로 빛 입자가 모여 카드가 된다. 카드는 좌우로 돌다가 멈추며 앞면을 보인다.
 * 룬석은 하나씩 등급 색으로 켜져 등급을 미리 알려 준다 (일반 1개 … 전설 5개, 야마토는 다섯 개가 모두 하늘색으로 번쩍).
 * 10회 뽑기는 가장 높은 등급의 카드를 먼저 보여 주고, 열 장을 차례로 뒤집는다. 아무 곳이나 누르면 건너뛴다.
 */
public final class MagicFx {
    static final ResourceLocation RUNES = new ResourceLocation(RtShop.MODID, "textures/gui/magic_runes.png");

    public static final class Card {
        public final ItemStack icon;
        public final String name;
        public final String grade;

        public Card(ItemStack icon, String name, String grade) {
            this.icon = icon;
            this.name = name;
            this.grade = grade;
        }
    }

    private final List<Card> cards = new ArrayList<>();
    private Card best;
    private int bestGrade;
    private long start = -1;
    private long length;
    private boolean done = true;
    private int lastLit = -1;
    private boolean soundForm, soundReveal;

    public boolean active() {
        return !done;
    }

    public boolean finished() {
        return done;
    }

    public List<Card> cards() {
        return cards;
    }

    public void start(List<Card> list) {
        cards.clear();
        cards.addAll(list);
        best = null;
        bestGrade = -1;
        for (Card c : list) {
            int g = gi(c.grade);
            if (g > bestGrade) {
                bestGrade = g;
                best = c;
            }
        }
        if (best == null) {
            done = true;
            return;
        }
        length = switch (bestGrade) {
            case 5 -> 5200;
            case 4 -> 4000;
            case 3 -> 3300;
            default -> 2600;
        } + (list.size() > 1 ? 1400 : 0);
        start = System.currentTimeMillis();
        done = false;
        lastLit = -1;
        soundForm = soundReveal = false;
        play(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.7F, 1F);
    }

    public void skip() {
        done = true;
    }

    /** 연출 시간 (ms) — 점검용. */
    public long lengthForTest() {
        return length;
    }

    static int gi(String g) {
        return switch (g) {
            case "uncommon" -> 1;
            case "rare" -> 2;
            case "epic" -> 3;
            case "legendary" -> 4;
            case "yamato" -> 5;
            default -> 0;
        };
    }

    private static void play(SoundEvent s, float pitch, float vol) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(s, pitch, vol));
    }

    private static int argb(int rgb, float a) {
        return (Mth.clamp((int) (a * 255), 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    /** 화면 전체에 그린다 (뽑기 창 위). */
    public void render(GuiGraphics g, Font font, int sw, int sh) {
        if (done) return;
        long t = System.currentTimeMillis() - start;
        if (t >= length + 600) {
            done = true;
            return;
        }
        boolean was = UiText.pause();
        try {
            draw(g, font, sw, sh, t);
        } finally {
            UiText.resume(was);
        }
    }

    private void draw(GuiGraphics g, Font font, int sw, int sh, long t) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 500);
        float fadeIn = Mth.clamp(t / 400F, 0F, 1F);
        // 어두운 장막
        g.fill(0, 0, sw, sh, argb(0x0A0614, 0.86F * fadeIn));
        int cx = sw / 2, cy = sh / 2 - 6;
        int R = Math.min(78, Math.min(sw, sh) / 3);
        int gradeRgb = GachaColors.of(best.grade);
        boolean yamato = bestGrade == 5;

        // 소환진: 점선 원 두 겹 (천천히 돈다)
        float spin = t / 1000F;
        drawCircle(g, cx, cy, R + 6, 72, spin * 0.3F, argb(0x9A7AD8, 0.55F * fadeIn));
        drawCircle(g, cx, cy, R - 18, 48, -spin * 0.5F, argb(0xE0C070, 0.45F * fadeIn));

        // 룬석이 하나씩 켜진다 (등급 예고)
        int lit = 0;
        int maxLit = yamato ? 5 : Math.min(5, bestGrade + 1);
        if (t > 1100) lit = Math.min(maxLit, 1 + (int) ((t - 1100) / 220));
        if (lit != lastLit && lit > 0) {
            lastLit = lit;
            play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F + lit * 0.15F, 1F);
            if (lit == maxLit && bestGrade >= 4) play(yamato ? SoundEvents.END_PORTAL_FRAME_FILL : SoundEvents.BEACON_POWER_SELECT, 1.0F, 1F);
        }
        float orbit = spin * (1.4F + (t > 1100 ? 1.2F : 0F));
        for (int i = 0; i < 5; i++) {
            double a = orbit + i * Math.PI * 2 / 5;
            int rx = cx + (int) (Math.cos(a) * R) - 8;
            int ry = cy + (int) (Math.sin(a) * R * 0.92) - 8;
            boolean on = i < lit;
            int col = yamato && on ? 0x49B6FF : gradeRgb;
            if (on) {
                // 켜진 룬석 둘레의 빛
                for (int k = 3; k >= 1; k--) g.fill(rx - k * 2, ry - k * 2, rx + 16 + k * 2, ry + 16 + k * 2, argb(col, 0.10F * fadeIn));
            }
            RenderSystem.enableBlend();
            if (on) RenderSystem.setShaderColor(((col >> 16) & 255) / 255F * 0.6F + 0.4F, ((col >> 8) & 255) / 255F * 0.6F + 0.4F, (col & 255) / 255F * 0.6F + 0.4F, fadeIn);
            else RenderSystem.setShaderColor(0.75F, 0.7F, 0.85F, fadeIn * 0.8F);
            g.blit(RUNES, rx, ry, i * 16, 0, 16, 16, 80, 16);
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        }

        // 빛 입자가 가운데로 모인다
        if (t > 400 && t < 1700) {
            float k = Mth.clamp((t - 400) / 1300F, 0F, 1F);
            for (int i = 0; i < 70; i++) {
                double ang = (i * 2.399963) + i * 0.13;
                double dist = (R * 1.6 + (i % 7) * 9) * (1 - easeIn(Mth.clamp(k * 1.25F - (i % 10) * 0.03F, 0F, 1F)));
                int px = cx + (int) (Math.cos(ang) * dist);
                int py = cy + (int) (Math.sin(ang) * dist);
                int c = i % 3 == 0 ? gradeRgb : 0xE6D8FF;
                g.fill(px, py, px + 2, py + 2, argb(c, 0.85F * (1 - k * 0.4F)));
            }
        }

        // 카드: 입자가 굳어 생기고, 좌우로 돌다가 멈춘다
        float formT = Mth.clamp((t - 1350) / 450F, 0F, 1F);
        if (formT > 0 && !soundForm) {
            soundForm = true;
            play(SoundEvents.ENCHANTMENT_TABLE_USE, 1.1F, 1F);
        }
        long revealAt = length - (cards.size() > 1 ? 1700 : 500);
        if (formT > 0) {
            float spinT = Mth.clamp((t - 1350) / (float) (revealAt - 1350), 0F, 1F);
            // 좌우 왕복 회전: 빠르게 시작해 천천히 멈춘다 (마지막에 앞면)
            double theta = (1 - Math.pow(1 - spinT, 3)) * Math.PI * 7;
            float sx = (float) Math.cos(theta);
            boolean front = spinT >= 1F;
            if (front && !soundReveal) {
                soundReveal = true;
                play(bestGrade >= 4 ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.PLAYER_LEVELUP, bestGrade >= 4 ? 1F : 1.4F, 0.8F);
            }
            float scale = 0.6F + 0.4F * easeOut(formT);
            drawCard(g, font, cx, cy, best, front ? 1F : Math.abs(sx), scale * (cards.size() > 1 && t > revealAt + 300 ? 0.8F : 1F),
                    front || sx > 0 && spinT > 0.85F, formT);
            if (yamato && front) {
                float fl = Mth.clamp(1 - (t - revealAt) / 700F, 0F, 1F);
                g.fill(0, 0, sw, sh, argb(0x9AD8FF, 0.45F * fl));
            }
        }

        // 10회: 열 장을 차례로 뒤집어 보인다
        if (cards.size() > 1 && t > revealAt + 300) {
            int n = cards.size();
            int cw = 34, ch = 46, gap = 6;
            int cols = Math.min(5, n);
            int rows = (n + cols - 1) / cols;
            int gx = cx - (cols * cw + (cols - 1) * gap) / 2;
            int gy = cy + 52;
            if (gy + rows * (ch + gap) > sh - 4) gy = sh - 4 - rows * (ch + gap);
            for (int i = 0; i < n; i++) {
                long ct = t - revealAt - 300 - i * 90L;
                if (ct < 0) continue;
                float k = Mth.clamp(ct / 220F, 0F, 1F);
                int x = gx + (i % cols) * (cw + gap) + cw / 2;
                int y = gy + (i / cols) * (ch + gap) + ch / 2;
                miniCard(g, x, y, cw, ch, cards.get(i), k);
            }
        }
        // 건너뛰기 안내
        String skip = Component.translatable("rtshop.magic.skip").getString();
        g.drawString(font, skip, sw - 8 - font.width(skip), sh - 14, argb(0xCFC2E8, 0.6F), false);
        g.pose().popPose();
    }

    private static float easeIn(float x) {
        return x * x;
    }

    private static float easeOut(float x) {
        return 1 - (1 - x) * (1 - x);
    }

    private static void drawCircle(GuiGraphics g, int cx, int cy, int r, int dots, float rot, int color) {
        for (int i = 0; i < dots; i++) {
            double a = rot + i * Math.PI * 2 / dots;
            int x = cx + (int) Math.round(Math.cos(a) * r);
            int y = cy + (int) Math.round(Math.sin(a) * r);
            g.fill(x, y, x + 1 + (i % 6 == 0 ? 1 : 0), y + 1 + (i % 6 == 0 ? 1 : 0), color);
        }
    }

    /** 큰 카드. sx = 가로 비율(회전), face = 앞면인가. */
    private void drawCard(GuiGraphics g, Font font, int cx, int cy, Card c, float sx, float scale, boolean face, float alpha) {
        int w = 64, h = 88;
        int col = GachaColors.of(c.grade);
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().scale(Math.max(0.04F, sx) * scale, scale, 1F);
        int a = (int) (alpha * 255);
        // 빛무리
        for (int k = 4; k >= 1; k--) g.fill(-w / 2 - k * 3, -h / 2 - k * 3, w / 2 + k * 3, h / 2 + k * 3, argb(col, 0.07F * alpha));
        g.fill(-w / 2 - 2, -h / 2 - 2, w / 2 + 2, h / 2 + 2, (a << 24) | (col & 0xFFFFFF));
        g.fill(-w / 2, -h / 2, w / 2, h / 2, (a << 24) | 0x1E1430);
        g.fill(-w / 2 + 3, -h / 2 + 3, w / 2 - 3, h / 2 - 3, (a << 24) | 0x2A1C44);
        if (!face) {
            // 뒷면: 룬 문양과 물음표
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1F, 1F, 1F, alpha);
            g.blit(RUNES, -8, -26, 64, 0, 16, 16, 80, 16);
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            Component q = Component.literal("?").withStyle(st -> st.withFont(net.minecraft.network.chat.Style.DEFAULT_FONT));
            var mf = Minecraft.getInstance().font;
            g.pose().pushPose();
            g.pose().scale(2F, 2F, 1F);
            g.drawString(mf, q, -(mf.width(q) - 1) / 2, -1, (a << 24) | 0xE8DCFF, false);
            g.pose().popPose();
        } else {
            g.pose().pushPose();
            g.pose().translate(-16, -32, 0);
            g.pose().scale(2F, 2F, 1F);
            g.renderItem(c.icon, 0, 0);
            g.pose().popPose();
            String gname = Component.translatable("rtshop.gacha.grade." + c.grade).getString();
            g.drawString(font, gname, -font.width(gname) / 2, 6, (a << 24) | col, false);
            List<net.minecraft.util.FormattedCharSequence> ls = font.split(Component.literal(c.name), w - 6);
            for (int i = 0; i < Math.min(2, ls.size()); i++) {
                g.drawString(font, ls.get(i), -font.width(ls.get(i)) / 2, 18 + i * (font.lineHeight + 1), (a << 24) | 0xEDE4FF, false);
            }
        }
        g.pose().popPose();
    }

    private static void miniCard(GuiGraphics g, int x, int y, int w, int h, Card c, float k) {
        int col = GachaColors.of(c.grade);
        float sx = Math.abs((float) Math.cos((1 - k) * Math.PI));
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(Math.max(0.05F, sx), 1F, 1F);
        g.fill(-w / 2 - 1, -h / 2 - 1, w / 2 + 1, h / 2 + 1, 0xFF000000 | col);
        g.fill(-w / 2, -h / 2, w / 2, h / 2, 0xFF22183A);
        if (k > 0.5F) g.renderItem(c.icon, -8, -12);
        g.fill(-w / 2 + 3, h / 2 - 6, w / 2 - 3, h / 2 - 4, 0xFF000000 | col);
        g.pose().popPose();
    }
}

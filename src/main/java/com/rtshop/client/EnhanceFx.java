package com.rtshop.client;

import com.rtshop.RtShop;
import com.rtshop.enhance.Enhance;
import com.rtshop.enhance.EnhanceConfig;
import com.rtshop.net.EnhancePacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 강화 결과 연출: 효과음, 입자, 화면 가운데 큰 글씨(창이 열려 있어도 보임), 채팅 기록. */
@Mod.EventBusSubscriber(modid = RtShop.MODID, value = Dist.CLIENT)
public final class EnhanceFx {
    private EnhanceFx() {
    }

    private static long shownAt;
    private static Component title = Component.empty();
    private static Component sub = Component.empty();
    private static int color = 0xFFFFFF;
    private static ItemStack icon = ItemStack.EMPTY;
    /** 시험용: 마지막 결과. */
    public static volatile String last = "";

    public static void handle(EnhancePacket msg) {
        if (msg.mode == EnhancePacket.CONFIG) {
            EnhanceConfig.setClient(msg.json);
            return;
        }
        if (msg.mode == EnhancePacket.EDITOR) {
            EnhanceConfig.setClient(msg.json);
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof EnhanceEditScreen ed) ed.update(msg.json);
            else mc.setScreen(new EnhanceEditScreen(msg.json));
            return;
        }
        if (msg.mode == EnhancePacket.GUARD) {
            Enhance.clientGuard = msg.outcome;
            return;
        }
        if (msg.mode == EnhancePacket.LEGEND) {
            legend(msg.json, msg.to, msg.item);
            return;
        }
        if (msg.mode == EnhancePacket.ODDS) {
            EnhanceConfig.setClient(msg.json);
            Minecraft.getInstance().setScreen(new EnhanceOddsScreen(Minecraft.getInstance().screen));
            return;
        }
        Enhance.Outcome o = Enhance.Outcome.values()[Math.max(0, Math.min(3, msg.outcome))];
        show(o, msg.guarded, msg.from, msg.to, msg.item);
    }

    public static void show(Enhance.Outcome o, boolean guarded, int from, int to, ItemStack item) {
        // 높은 단계(+7 이상 시도)는 단계 묶음마다 다른 긴장 연출 뒤에 결과를 공개한다
        if (Enhance.fxTier(from + 1, com.rtshop.enhance.EnhanceConfig.client().broadcastFrom) > 0) {
            last = o.name() + ":" + from + ":" + to + (guarded ? ":guarded" : "");
            EnhanceGrandFx.start(o, guarded, from, to, item);
            return;
        }
        showNow(o, guarded, from, to, item, false);
    }

    /** 시험용: 마지막으로 뿌린 강화 입자의 수와 자리, 그때 눈과의 거리. */
    public static volatile int lastBurstForTest;
    public static volatile double lastBurstEyeDistForTest = -1;

    /**
     * 강화 입자 (3.79, 사용자 11-28 10:51 "강화 시 생기는 초록색 파티클이 시야를 너무 가리는"): 몸을 따라다니는 입자(createTrackingEmitter — 틱마다 16개씩,
     * 눈앞까지) 대신 모루 위(바라보는 블록이 8칸 안이면 그 위, 아니면 눈앞 2칸 아래쪽)에 한 번만 count 개를 뿌린다. 게임의 입자 설정(전부 · 줄임 · 최소)을 따른다.
     */
    static void burst(net.minecraft.core.particles.ParticleOptions type, int count, double spread, double speed) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || count <= 0 || !ClientConfig.get().enhanceParticles) return;
        net.minecraft.world.phys.Vec3 eye = mc.player.getEyePosition();
        net.minecraft.world.phys.Vec3 c = burstCenter(mc, eye);
        net.minecraft.util.RandomSource r = mc.player.getRandom();
        for (int i = 0; i < count; i++) {
            double dx = (r.nextDouble() - 0.5) * 2 * spread, dz = (r.nextDouble() - 0.5) * 2 * spread, dy = r.nextDouble() * spread;
            mc.level.addParticle(type, c.x + dx, c.y + dy, c.z + dz, (r.nextDouble() - 0.5) * speed, r.nextDouble() * speed, (r.nextDouble() - 0.5) * speed);
        }
        lastBurstForTest = count;
        lastBurstEyeDistForTest = c.distanceTo(eye);
    }

    /** 모루(바라보는 블록) 윗면 가운데. 블록을 보고 있지 않거나 너무 가까우면(1.2칸 안) 눈앞 2칸, 눈높이보다 0.7칸 아래. */
    static net.minecraft.world.phys.Vec3 burstCenter(Minecraft mc, net.minecraft.world.phys.Vec3 eye) {
        if (mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult bh && bh.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            net.minecraft.world.phys.Vec3 top = net.minecraft.world.phys.Vec3.atBottomCenterOf(bh.getBlockPos().above());
            double d = top.distanceTo(eye);
            if (d < 8 && d > 1.2) return top;
        }
        net.minecraft.world.phys.Vec3 look = mc.player.getViewVector(1F);
        return eye.add(look.x * 2.0, look.y * 2.0 - 0.7, look.z * 2.0);
    }

    /** 결과 표시 (grand = 큰 성공 연출이 이미 글자를 그리므로 띠는 생략). */
    public static void showNow(Enhance.Outcome o, boolean guarded, int from, int to, ItemStack item, boolean grand) {
        Minecraft mc = Minecraft.getInstance();
        grandShown = grand;
        last = o.name() + ":" + from + ":" + to + (guarded ? ":guarded" : "");
        icon = item.copy();
        shownAt = System.currentTimeMillis();
        switch (o) {
            case SUCCESS -> {
                title = Component.translatable("rtshop.enhance.fx_success", to);
                color = to >= 10 ? 0xFF77FF : to >= 7 ? 0xFFC83C : 0x7CFF6A;
                if (!grand) {
                    play(SoundEvents.PLAYER_LEVELUP, 1.2F, 0.9F);
                    play(SoundEvents.ANVIL_USE, 1.4F, 0.6F);
                }
                if (!grand) {
                    // 3.79: 예전에는 몸을 따라다니며 25틱 동안 400개쯤 뿌려 시야를 가렸다 → 모루 위에 한 번만 조금
                    if (to >= 10) burst(ParticleTypes.TOTEM_OF_UNDYING, 12, 0.35, 0.12);
                    else burst(ParticleTypes.HAPPY_VILLAGER, 8, 0.4, 0.04);
                }
            }
            case MAINTAIN -> {
                title = Component.translatable("rtshop.enhance.fx_maintain", to);
                color = 0xE8E8E8;
                play(SoundEvents.ANVIL_LAND, 1.0F, 0.5F);
            }
            case DAMAGE -> {
                title = Component.translatable("rtshop.enhance.fx_damage", from, to);
                color = 0xFF9A2E;
                play(SoundEvents.ITEM_BREAK, 0.8F, 0.9F);
                play(SoundEvents.ANVIL_LAND, 0.6F, 0.5F);
            }
            case DESTROY -> {
                title = Component.translatable("rtshop.enhance.fx_destroy");
                color = 0xFF3A3A;
                play(SoundEvents.ITEM_BREAK, 0.5F, 1.0F);
                play(SoundEvents.GENERIC_EXPLODE, 1.3F, 0.4F);
                burst(ParticleTypes.LARGE_SMOKE, 6, 0.3, 0.03);
            }
        }
        sub = guarded ? Component.translatable("rtshop.enhance.fx_guarded") : item.getHoverName();
        if (mc.player != null) {
            Component chat = Component.translatable("rtshop.enhance.chat_prefix").withStyle(ChatFormatting.GOLD).append(item.getHoverName().copy().withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" - ").withStyle(ChatFormatting.DARK_GRAY)).append(title.copy().withStyle(s -> s.withColor(color)));
            if (guarded) chat = chat.copy().append(Component.literal(" (").append(Component.translatable("rtshop.enhance.fx_guarded")).append(")")
                    .withStyle(ChatFormatting.AQUA));
            NoticeHud.push(chat);
        }
    }

    private static void play(SoundEvent s, float pitch, float vol) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(s, pitch, vol));
    }

    private static boolean grandShown;

    public static boolean active() {
        return EnhanceGrandFx.running() || System.currentTimeMillis() - shownAt < 2600 || System.currentTimeMillis() - legendAt < LEGEND_MS;
    }

    // ---- 누군가 최고 단계 강화에 성공했을 때: 다른 모든 플레이어의 화면 위쪽에 잠시 뜨는 띠

    private static final long LEGEND_MS = 7000;
    private static long legendAt;
    private static String legendWho = "";
    private static int legendLevel;
    private static ItemStack legendItem = ItemStack.EMPTY;
    /** 시험용. */
    public static volatile String lastLegend = "";

    public static void legend(String who, int level, ItemStack item) {
        legendWho = who;
        legendLevel = level;
        legendItem = item.copy();
        legendAt = System.currentTimeMillis();
        lastLegend = who + ":" + level;
        play(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 0.8F);
        play(SoundEvents.BELL_RESONATE, 1.3F, 0.5F);
    }

    private static void renderLegend(GuiGraphics g, int w, int h) {
        long t = System.currentTimeMillis() - legendAt;
        if (t < 0 || t > LEGEND_MS) return;
        Font font = Minecraft.getInstance().font;
        float slide = Fx2D.easeOut(Fx2D.span(t, 0, 500)) * (1 - Fx2D.easeIn(Fx2D.span(t, LEGEND_MS - 600, LEGEND_MS)));
        int bh = 30;
        int y = (int) (-bh - 4 + (bh + 10) * slide);
        g.pose().pushPose();
        g.pose().translate(0, 0, 940);
        g.fill(0, y, w, y + bh, 0xD8100818);
        Fx2D.begin(g, true);
        float sweep = (t % 2200) / 2200F * (w + 200) - 100;
        Fx2D.beam(0, y + 0.5F, w, y + 0.5F, 3, 0xC0FFD98A);
        Fx2D.beam(0, y + bh - 0.5F, w, y + bh - 0.5F, 3, 0xC0FFD98A);
        Fx2D.glow(sweep, y + bh / 2F, 90, 26, 0x50FFE9B0, 0x00FFE9B0);
        for (int i = 0; i < 12; i++) {
            float tw = Math.abs((float) Math.sin(t * 0.005 + i * 1.9));
            Fx2D.star(Fx2D.hash(i, 201) * w, y + 4 + Fx2D.hash(i, 202) * (bh - 8), 2 + 3 * tw, 0.7F, Fx2D.rgba(0xFFFFFF, 0.7F * tw * slide));
        }
        Fx2D.end();
        Component line = Component.translatable("rtshop.enhance.legend_banner", legendWho, legendItem.getHoverName(), legendLevel);
        float scale = Math.min(1.3F, (w - 60) / (float) Math.max(1, font.width(line)));
        int tw = (int) (font.width(line) * scale);
        int tx = (w - tw) / 2 + 10;
        if (!legendItem.isEmpty()) g.renderItem(legendItem, tx - 22, y + 7);
        g.pose().pushPose();
        g.pose().translate(tx, y + 15 - 4 * scale, 300);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, line, 0, 0, 0xFFFFE9A8, true);
        g.pose().popPose();
        g.pose().popPose();
    }

    public static void render(GuiGraphics g, int w, int h) {
        boolean was = UiText.pause(); // 연출은 원래 모양으로
        try {
            renderFx(g, w, h);
        } finally {
            UiText.resume(was);
        }
    }

    private static void renderFx(GuiGraphics g, int w, int h) {
        renderLegend(g, w, h);
        EnhanceGrandFx.render(g, w, h);
        if (grandShown || EnhanceGrandFx.hidesBanner()) return;
        long t = System.currentTimeMillis() - shownAt;
        if (t > 2600 || shownAt == 0) return;
        float a = t < 150 ? t / 150F : t > 2100 ? (2600 - t) / 500F : 1F;
        int alpha = Math.max(8, Math.min(255, (int) (a * 255)));
        Font font = Minecraft.getInstance().font;
        float pop = t < 200 ? 1.6F - 0.6F * (t / 200F) : 1F;
        float scale = 2.2F * pop;
        g.pose().pushPose();
        g.pose().translate(0, 0, 900);
        int cy = (int) (h * 0.28F);
        g.fill(0, cy - 34, w, cy + 30, (Math.min(alpha, 150) << 24));
        if (!icon.isEmpty()) {
            g.pose().pushPose();
            g.pose().translate(w / 2F - 8, cy - 31, 0);
            g.renderItem(icon, 0, 0);
            g.pose().popPose();
        }
        g.pose().pushPose();
        g.pose().translate(w / 2F, cy, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, title, -font.width(title) / 2, -4, (alpha << 24) | color, true);
        g.pose().popPose();
        g.drawString(font, sub, w / 2 - font.width(sub) / 2, cy + 16, (alpha << 24) | 0xDDDDDD, true);
        g.pose().popPose();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreen(ScreenEvent.Render.Post e) {
        if (active()) render(e.getGuiGraphics(), e.getScreen().width, e.getScreen().height);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHud(RenderGuiEvent.Post e) {
        if (active() && Minecraft.getInstance().screen == null) {
            render(e.getGuiGraphics(), e.getWindow().getGuiScaledWidth(), e.getWindow().getGuiScaledHeight());
        }
    }
}

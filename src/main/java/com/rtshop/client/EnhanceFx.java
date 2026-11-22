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
        if (msg.mode == EnhancePacket.ODDS) {
            EnhanceConfig.setClient(msg.json);
            Minecraft.getInstance().setScreen(new EnhanceOddsScreen(Minecraft.getInstance().screen));
            return;
        }
        Enhance.Outcome o = Enhance.Outcome.values()[Math.max(0, Math.min(3, msg.outcome))];
        show(o, msg.guarded, msg.from, msg.to, msg.item);
    }

    public static void show(Enhance.Outcome o, boolean guarded, int from, int to, ItemStack item) {
        Minecraft mc = Minecraft.getInstance();
        last = o.name() + ":" + from + ":" + to + (guarded ? ":guarded" : "");
        icon = item.copy();
        shownAt = System.currentTimeMillis();
        switch (o) {
            case SUCCESS -> {
                title = Component.translatable("rtshop.enhance.fx_success", to);
                color = to >= 10 ? 0xFF77FF : to >= 7 ? 0xFFC83C : 0x7CFF6A;
                play(SoundEvents.PLAYER_LEVELUP, 1.2F, 0.9F);
                play(SoundEvents.ANVIL_USE, 1.4F, 0.6F);
                if (mc.player != null) {
                    mc.particleEngine.createTrackingEmitter(mc.player, to >= 10 ? ParticleTypes.TOTEM_OF_UNDYING : ParticleTypes.HAPPY_VILLAGER, 25);
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
                if (mc.player != null) {
                    mc.particleEngine.createTrackingEmitter(mc.player, ParticleTypes.LARGE_SMOKE, 15);
                }
            }
        }
        sub = guarded ? Component.translatable("rtshop.enhance.fx_guarded") : item.getHoverName();
        if (mc.player != null) {
            Component chat = Component.translatable("rtshop.enhance.chat_prefix").withStyle(ChatFormatting.GOLD).append(item.getHoverName().copy().withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" - ").withStyle(ChatFormatting.DARK_GRAY)).append(title.copy().withStyle(s -> s.withColor(color)));
            if (guarded) chat = chat.copy().append(Component.literal(" (").append(Component.translatable("rtshop.enhance.fx_guarded")).append(")")
                    .withStyle(ChatFormatting.AQUA));
            mc.player.displayClientMessage(chat, false);
        }
    }

    private static void play(SoundEvent s, float pitch, float vol) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(s, pitch, vol));
    }

    public static boolean active() {
        return System.currentTimeMillis() - shownAt < 2600;
    }

    public static void render(GuiGraphics g, int w, int h) {
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

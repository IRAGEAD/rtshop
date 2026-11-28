/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.ChatFormatting
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.network.chat.Style
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.FormattedCharSequence
 *  net.minecraft.util.Mth
 *  net.minecraft.world.item.ItemStack
 */
package com.pla.spiral_abyss.client.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.pla.spiral_abyss.client.platform.AbyssClientEvents;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public final class RareRewardOverlay {
    private static final ResourceLocation TEXTURE = new ResourceLocation("spiral_abyss", "textures/gui/toast/terracraft.png");
    private static final Component TITLE = Component.m_237115_((String)"toast.spiral_abyss.rare_reward.title");
    private static final int DURATION = 150;
    private static final Deque<Reward> QUEUE = new ArrayDeque<Reward>();
    private static Reward current;
    private static int ticks;
    public static final AbyssClientEvents.Overlay HUD;

    private RareRewardOverlay() {
    }

    public static void enqueue(Component player, ItemStack stack) {
        if (stack.m_41619_()) {
            return;
        }
        ItemStack icon = stack.m_255036_(1);
        MutableComponent item = Component.m_237113_((String)"[").m_7220_(RareRewardOverlay.singleLine(icon.m_41786_(), ChatFormatting.GOLD)).m_130946_("]").m_130940_(ChatFormatting.GOLD);
        Component description = RareRewardOverlay.singleLine((Component)Component.m_237110_((String)"toast.spiral_abyss.rare_reward.description", (Object[])new Object[]{RareRewardOverlay.singleLine(player, ChatFormatting.AQUA), item}), null);
        QUEUE.addLast(new Reward(icon, description));
    }

    private static Component singleLine(Component text, ChatFormatting color) {
        MutableComponent result = Component.m_237119_();
        text.m_7451_((style, value) -> {
            result.m_7220_((Component)Component.m_237113_((String)value.replace('\n', ' ').replace('\r', ' ')).m_6270_(color == null ? style : style.m_131140_(color)));
            return Optional.empty();
        }, Style.f_131099_);
        return result;
    }

    public static void logout(AbyssClientEvents.ClientPlayerNetworkEvent.LoggingOut event) {
        RareRewardOverlay.clear();
    }

    public static void tick(AbyssClientEvents.TickEvent.ClientTickEvent event) {
        if (event.phase != AbyssClientEvents.TickEvent.Phase.END) {
            return;
        }
        Minecraft client = Minecraft.m_91087_();
        if (client.f_91073_ == null || client.f_91074_ == null) {
            RareRewardOverlay.clear();
            return;
        }
        if (client.m_91104_() || client.f_91066_.f_92062_) {
            return;
        }
        if (current != null && ++ticks >= 150) {
            current = null;
        }
        if (current == null && !QUEUE.isEmpty()) {
            current = QUEUE.removeFirst();
            ticks = 0;
        }
    }

    private static void clear() {
        QUEUE.clear();
        current = null;
        ticks = 0;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void render(GuiGraphics graphics, Font font, int screenWidth, int screenHeight, float time, Reward reward) {
        int width = Mth.m_14045_((int)(font.m_92852_((FormattedText)reward.description()) + 24), (int)240, (int)360);
        float scale = Math.min(1.0f, (float)Math.max(1, screenWidth - 16) / (float)width);
        float top = (float)screenHeight * 0.15f;
        float exit = RareRewardOverlay.easeIn(RareRewardOverlay.progress(time, 110, 40));
        PoseStack pose = graphics.m_280168_();
        pose.m_85836_();
        pose.m_252880_(((float)screenWidth - (float)width * scale) / 2.0f, top - exit * (top + 80.0f * scale), 200.0f);
        pose.m_85841_(scale, scale, 1.0f);
        RenderSystem.enableBlend();
        try {
            int alpha;
            float iconProgress;
            float iconScale;
            if (time > 31.0f) {
                pose.m_85836_();
                pose.m_252880_(0.0f, -200.0f * (1.0f - RareRewardOverlay.easeOut(RareRewardOverlay.progress(time, 31, 20))), 0.0f);
                RareRewardOverlay.strip(graphics, width, 20, 40, 0, 5);
                graphics.m_280163_(TEXTURE, width - 18, 54, 0.0f, 108.0f, 9, 14, 256, 256);
                pose.m_85849_();
            }
            if (time > 32.0f) {
                pose.m_85836_();
                RareRewardOverlay.scaleAround(graphics, RareRewardOverlay.easeOut(RareRewardOverlay.progress(time, 32, 14)), 1.0f, (float)width / 2.0f, 0.0f);
                RareRewardOverlay.strip(graphics, width, 5, 14, 68, 24);
                pose.m_85849_();
            }
            if ((iconScale = RareRewardOverlay.easeOut(iconProgress = RareRewardOverlay.progress(time, 0, 40))) > 0.001f) {
                pose.m_85836_();
                RareRewardOverlay.scaleAround(graphics, iconScale, iconScale, (float)width / 2.0f, 13.0f);
                pose.m_252880_(0.0f, -40.0f * (1.0f - RareRewardOverlay.elasticOut(iconProgress)) + (float)Math.sin(time * 0.1f) * 1.5f - 5.0f, 0.0f);
                graphics.m_280163_(TEXTURE, width / 2 - 13, 0, 52.0f, 82.0f, 26, 26, 256, 256);
                graphics.m_280203_(reward.icon(), width / 2 - 8, 5);
                pose.m_85849_();
            }
            if (time > 40.0f && (alpha = Math.round(RareRewardOverlay.progress(time, 40, 20) * 255.0f)) > 3) {
                RareRewardOverlay.fittedLine(graphics, font, TITLE, width, 25, alpha << 24 | 0xFFAA00);
                RareRewardOverlay.description(graphics, font, reward.description(), width, alpha << 24 | 0xFFFFFF);
            }
        }
        finally {
            graphics.m_280246_(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
            pose.m_85849_();
        }
    }

    private static void strip(GuiGraphics graphics, int width, int y, int height, int v, int edge) {
        graphics.m_280163_(TEXTURE, 0, y, 0.0f, (float)v, edge, height, 256, 256);
        graphics.m_280411_(TEXTURE, edge, y, width - edge * 2, height, (float)edge, (float)v, 162 - edge * 2, height, 256, 256);
        graphics.m_280163_(TEXTURE, width - edge, y, (float)(162 - edge), (float)v, edge, height, 256, 256);
    }

    private static void fittedLine(GuiGraphics graphics, Font font, Component text, int width, int y, int color) {
        float scale = Math.min(1.0f, ((float)width - 20.0f) / (float)Math.max(1, font.m_92852_((FormattedText)text)));
        graphics.m_280168_().m_85836_();
        graphics.m_280168_().m_252880_((float)width / 2.0f, (float)y, 0.0f);
        graphics.m_280168_().m_85841_(scale, scale, 1.0f);
        graphics.m_280653_(font, text, 0, 0, color);
        graphics.m_280168_().m_85849_();
    }

    private static void description(GuiGraphics graphics, Font font, Component text, int width, int color) {
        float scale = 1.0f;
        List lines = font.m_92923_((FormattedText)text, width - 20);
        for (int attempt = 0; lines.size() > 2 && attempt < 12; ++attempt) {
            lines = font.m_92923_((FormattedText)text, (int)((float)(width - 20) / (scale *= 0.9f)));
        }
        if (lines.size() > 2) {
            RareRewardOverlay.fittedLine(graphics, font, text, width, 42, color);
            return;
        }
        graphics.m_280168_().m_85836_();
        graphics.m_280168_().m_252880_((float)width / 2.0f, lines.size() == 1 ? 42.0f : 38.0f, 0.0f);
        graphics.m_280168_().m_85841_(scale, scale, 1.0f);
        for (int line = 0; line < lines.size(); ++line) {
            FormattedCharSequence formattedCharSequence = (FormattedCharSequence)lines.get(line);
            Objects.requireNonNull(font);
            graphics.m_280364_(font, formattedCharSequence, 0, line * 9, color);
        }
        graphics.m_280168_().m_85849_();
    }

    private static void scaleAround(GuiGraphics graphics, float x, float y, float centerX, float centerY) {
        graphics.m_280168_().m_252880_(centerX, centerY, 0.0f);
        graphics.m_280168_().m_85841_(x, y, 1.0f);
        graphics.m_280168_().m_252880_(-centerX, -centerY, 0.0f);
    }

    private static float progress(float time, int start, int duration) {
        return Mth.m_14036_((float)((time - (float)start) / (float)duration), (float)0.0f, (float)1.0f);
    }

    private static float easeIn(float progress) {
        float squared = progress * progress;
        float fourth = squared * squared;
        return fourth * fourth;
    }

    private static float easeOut(float progress) {
        return 1.0f - RareRewardOverlay.easeIn(1.0f - progress);
    }

    private static float elasticOut(float progress) {
        if (progress == 0.0f || progress == 1.0f) {
            return progress;
        }
        return (float)(Math.pow(2.0, -10.0f * progress) * Math.sin((double)(progress * 10.0f - 0.75f) * Math.PI / 3.0) + 1.0);
    }

    static {
        HUD = (gui, graphics, partialTick, width, height) -> {
            Minecraft client = Minecraft.m_91087_();
            if (current == null || client.f_91066_.f_92062_ || client.f_91074_ == null || client.f_91073_ == null) {
                return;
            }
            RareRewardOverlay.render(graphics, client.f_91062_, width, height, (float)ticks + partialTick, current);
        };
    }

    private record Reward(ItemStack icon, Component description) {
    }
}


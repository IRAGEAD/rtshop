/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraftforge.client.gui.overlay.VanillaGuiOverlay
 */
package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.client.Anim;
import dev.dyslexic.cinematic.client.CampfireHint;
import dev.dyslexic.cinematic.client.ClientStation;
import dev.dyslexic.cinematic.client.FreeLook;
import dev.dyslexic.cinematic.client.ReachNotice;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.Settings;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

public final class Overlay {
    private static final Set<ResourceLocation> KEEP = Set.of(VanillaGuiOverlay.CHAT_PANEL.id(), VanillaGuiOverlay.SUBTITLES.id(), VanillaGuiOverlay.DEBUG_TEXT.id(), VanillaGuiOverlay.FPS_GRAPH.id(), VanillaGuiOverlay.PLAYER_LIST.id());

    private Overlay() {
    }

    public static boolean hides(ResourceLocation layer) {
        return Scene.active() && !KEEP.contains(layer);
    }

    private static double bars() {
        return switch (Scene.phase) {
            case Scene.Phase.INTRO -> {
                if (Scene.transition || Scene.quick) {
                    yield 0.0;
                }
                double u = Scene.introProgress();
                yield Anim.inOutSine(Anim.window(u, 0.0, 0.3)) * (1.0 - Anim.inOutSine(Anim.window(u, 0.72, 1.0)));
            }
            case Scene.Phase.OUTRO -> {
                double u = Scene.time / 1.05;
                yield Anim.inOutSine(Anim.window(u, 0.0, 0.25)) * (1.0 - Anim.inOutSine(Anim.window(u, 0.7, 1.0)));
            }
            case Scene.Phase.ACTIVE -> 0.55 * FreeLook.amount();
            default -> 0.0;
        };
    }

    public static void render(GuiGraphics graphics) {
        double k;
        ClientStation station;
        CampfireHint.render(graphics);
        ReachNotice.render(graphics);
        ClientStation clientStation = station = Scene.active() ? Scene.station : null;
        if (station != null && station.flash() > 0.003 && Settings.screenFlash()) {
            int a = (int)(Math.min(1.0, station.flash()) * 255.0);
            graphics.m_280509_(0, 0, graphics.m_280182_(), graphics.m_280206_(), a << 24 | station.flashColor());
        }
        double d = k = Settings.letterbox() ? Overlay.bars() : 0.0;
        if (k <= 0.001) {
            return;
        }
        int h = (int)Math.round((double)graphics.m_280206_() * 0.085 * k);
        graphics.m_280509_(0, 0, graphics.m_280182_(), h, -16777216);
        graphics.m_280509_(0, graphics.m_280206_() - h, graphics.m_280182_(), graphics.m_280206_(), -16777216);
    }
}


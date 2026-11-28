/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.InputConstants$Type
 *  javax.annotation.Nullable
 *  net.minecraft.client.KeyMapping
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult
 *  net.minecraft.world.phys.HitResult$Type
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.dyslexic.cinematic.client.Anim;
import dev.dyslexic.cinematic.client.CampfireStation;
import dev.dyslexic.cinematic.client.ClientOnly;
import dev.dyslexic.cinematic.client.KeyLabels;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.Settings;
import dev.dyslexic.cinematic.station.StationKind;
import javax.annotation.Nullable;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class CampfireHint {
    public static final KeyMapping SIT = new KeyMapping("key.cinematic.sit", InputConstants.Type.KEYSYM, 72, "key.categories.cinematic");
    private static double shown;
    @Nullable
    private static BlockPos checkedPos;
    @Nullable
    private static BlockPos checkedFrom;
    private static boolean checkedOk;

    private CampfireHint() {
    }

    @Nullable
    private static BlockPos looked() {
        BlockHitResult hit;
        HitResult hitResult;
        Minecraft mc = Minecraft.m_91087_();
        if (!ClientOnly.serverHasMod() || Scene.active() || mc.f_91080_ != null || mc.f_91074_ == null || mc.f_91073_ == null || mc.f_91074_.m_5833_() || !Settings.scene(StationKind.CAMPFIRE) || !((hitResult = mc.f_91077_) instanceof BlockHitResult) || (hit = (BlockHitResult)hitResult).m_6662_() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockPos pos = hit.m_82425_();
        if (!StationKind.CAMPFIRE.matches(mc.f_91073_.m_8055_(pos)) || mc.f_91074_.m_20238_(pos.m_252807_()) >= 20.25) {
            return null;
        }
        return pos;
    }

    @Nullable
    private static BlockPos target() {
        Minecraft mc = Minecraft.m_91087_();
        BlockPos pos = CampfireHint.looked();
        if (pos == null || mc.f_91074_ == null) {
            return null;
        }
        if (!pos.equals((Object)checkedPos) || !mc.f_91074_.m_20183_().equals((Object)checkedFrom)) {
            checkedPos = pos.m_7949_();
            checkedFrom = mc.f_91074_.m_20183_();
            checkedOk = CampfireStation.sittable(mc.f_91074_, pos);
        }
        return checkedOk ? pos : null;
    }

    public static void tick() {
        while (SIT.m_90859_()) {
            BlockPos pos = CampfireHint.looked();
            if (pos == null) continue;
            Scene.begin(pos, StationKind.CAMPFIRE);
        }
    }

    public static void render(GuiGraphics g) {
        boolean looking = Settings.campfireInvite() && CampfireHint.target() != null;
        float alpha = (float)Anim.smoother(shown = Anim.approach(shown, looking ? 1.0 : 0.0, looking ? 9.0 : 12.0, Scene.frameDt));
        if (Math.round(alpha * 255.0f) < 8) {
            return;
        }
        Font font = Minecraft.m_91087_().f_91062_;
        Component key = KeyLabels.of(SIT);
        MutableComponent text = Component.m_237115_((String)"cinematic.campfire.sit");
        int keyW = Math.max(11, font.m_92852_((FormattedText)key) + 6);
        int w = 6 + keyW + 5 + font.m_92852_((FormattedText)text) + 7;
        int h = 15;
        int x = (g.m_280182_() - w) / 2;
        int y = g.m_280206_() / 2 + 13 + Math.round((1.0f - alpha) * 5.0f);
        g.m_280509_(x, y, x + w, y + h, CampfireHint.argb(0.5f * alpha, 12, 12, 16));
        g.m_280509_(x, y + h - 1, x + w, y + h, CampfireHint.argb(0.35f * alpha, 255, 196, 120));
        int kx = x + 6;
        int ky = y + 2;
        g.m_280509_(kx, ky, kx + keyW, ky + 11, CampfireHint.argb(0.85f * alpha, 242, 234, 216));
        g.m_280509_(kx + 1, ky + 1, kx + keyW - 1, ky + 10, CampfireHint.argb(0.9f * alpha, 30, 28, 34));
        g.m_280614_(font, key, kx + (keyW - font.m_92852_((FormattedText)key)) / 2, ky + 2, CampfireHint.argb(alpha, 255, 226, 150), false);
        g.m_280614_(font, (Component)text, kx + keyW + 5, y + 4, CampfireHint.argb(alpha, 242, 234, 216), false);
    }

    private static int argb(float a, int r, int g, int b) {
        return Math.max(0, Math.min(255, Math.round(a * 255.0f))) << 24 | r << 16 | g << 8 | b;
    }
}


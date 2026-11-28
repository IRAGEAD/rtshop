/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.NativeImage
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.texture.AbstractTexture
 *  net.minecraft.client.renderer.texture.DynamicTexture
 *  net.minecraft.resources.ResourceLocation
 */
package com.pla.spiral_abyss.client.renderer;

import com.mojang.blaze3d.platform.NativeImage;
import com.pla.spiral_abyss.client.renderer.PortalPixels;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

final class PortalTextures {
    private static final Slot[] SLOTS = new Slot[3];

    private PortalTextures() {
    }

    static ResourceLocation texture(int state, int frame) {
        int mode = Math.max(0, Math.min(2, state));
        if (SLOTS[mode] == null) {
            PortalTextures.SLOTS[mode] = new Slot(mode);
        }
        Slot slot = SLOTS[mode];
        int animationFrame = Math.floorMod(frame, 64);
        if (slot.frame != animationFrame) {
            NativeImage pixels = slot.texture.m_117991_();
            if (pixels != null) {
                for (int y = 0; y < 72; ++y) {
                    for (int x = 0; x < 64; ++x) {
                        pixels.m_84988_(x, y, PortalPixels.pixel(x, y, mode, animationFrame));
                    }
                }
                slot.texture.m_117985_();
                slot.texture.m_117960_(false, false);
            }
            slot.frame = animationFrame;
        }
        return slot.location;
    }

    private static final class Slot {
        private final ResourceLocation location;
        private final DynamicTexture texture;
        private int frame = -1;

        private Slot(int mode) {
            this.location = new ResourceLocation("spiral_abyss", "dynamic/portal_" + mode);
            this.texture = new DynamicTexture(new NativeImage(64, 72, false));
            this.texture.m_117960_(false, false);
            Minecraft.m_91087_().m_91097_().m_118495_(this.location, (AbstractTexture)this.texture);
        }
    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.logging.LogUtils
 *  dev.architectury.platform.Platform
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.LivingEntity
 */
package com.pla.spiral_abyss.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.pla.spiral_abyss.client.screen.GeckoPreviewSampler;
import com.pla.spiral_abyss.encounter.data.BossOptions;
import com.pla.spiral_abyss.platform.AbyssRegistries;
import dev.architectury.platform.Platform;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

final class OptionalGeoPreview {
    private static boolean unavailable;

    private OptionalGeoPreview() {
    }

    static boolean supports(LivingEntity entity) {
        return OptionalGeoPreview.supports(entity, BossOptions.PreviewAnimation.DEFAULT);
    }

    static boolean supports(LivingEntity entity, BossOptions.PreviewAnimation animation) {
        ResourceLocation id = AbyssRegistries.ENTITY_TYPES.getKey(entity.m_6095_());
        if (unavailable || id == null || !animation.configured() && !id.m_135827_().equals("block_factorys_bosses") || !Platform.isModLoaded((String)"geckolib")) {
            return false;
        }
        try {
            return GeckoPreviewSampler.supports(entity);
        }
        catch (LinkageError exception) {
            unavailable = true;
            LogUtils.getLogger().warn("Installed GeckoLib API does not support Abyss preview sampling", (Throwable)exception);
            return false;
        }
    }

    static boolean render(LivingEntity entity, long seed, PoseStack pose, MultiBufferSource buffers, int light) {
        return OptionalGeoPreview.render(entity, seed, BossOptions.PreviewAnimation.DEFAULT, pose, buffers, light);
    }

    static boolean render(LivingEntity entity, long seed, BossOptions.PreviewAnimation animation, PoseStack pose, MultiBufferSource buffers, int light) {
        return OptionalGeoPreview.supports(entity, animation) && GeckoPreviewSampler.render(entity, seed, animation, pose, buffers, light);
    }
}


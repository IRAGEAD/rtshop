/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.renderer.entity.EntityRendererProvider$Context
 *  net.minecraft.resources.ResourceLocation
 */
package com.pla.spiral_abyss.client.renderer;

import com.pla.spiral_abyss.client.renderer.ExperienceChestModel;
import com.pla.spiral_abyss.client.renderer.LootChestModel;
import com.pla.spiral_abyss.client.renderer.LootChestRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public final class ExperienceChestRenderer
extends LootChestRenderer {
    private static final ResourceLocation TEXTURE = new ResourceLocation("spiral_abyss", "textures/entity/exp_chest.png");

    public ExperienceChestRenderer(EntityRendererProvider.Context context) {
        super(context, (LootChestModel)new ExperienceChestModel(context.m_174023_(ExperienceChestModel.LAYER_LOCATION)), TEXTURE);
    }
}


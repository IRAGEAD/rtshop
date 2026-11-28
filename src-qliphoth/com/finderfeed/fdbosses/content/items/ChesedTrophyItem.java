/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.FDModelItemRenderer
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.FDModelItemRendererOptions
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.item.BlockItem
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.ItemDisplayContext
 *  net.minecraft.world.level.block.Block
 *  net.minecraftforge.client.extensions.common.IClientItemExtensions
 *  org.joml.Vector3f
 */
package com.finderfeed.fdbosses.content.items;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdbosses.init.BossModels;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.FDModelItemRenderer;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.FDModelItemRendererOptions;
import java.util.function.Consumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.joml.Vector3f;

public class ChesedTrophyItem
extends BlockItem {
    public ChesedTrophyItem(Block p_40565_, Item.Properties p_40566_) {
        super(p_40565_, p_40566_);
    }

    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        super.initializeClient(consumer);
        consumer.accept(FDModelItemRenderer.createExtensions((FDModelItemRendererOptions)FDModelItemRendererOptions.create().addModel(BossModels.CHESED, RenderType.m_110452_((ResourceLocation)FDBosses.location("textures/entities/chesed.png"))).addModel(BossModels.CHESED_CRYSTAL_LAYER, RenderType.m_110488_((ResourceLocation)FDBosses.location("textures/entities/chesed_crystals.png"))).setScale(ctx -> {
            if (ctx == ItemDisplayContext.GROUND) {
                return Float.valueOf(0.15f);
            }
            return Float.valueOf(0.25f);
        }).addRotation(itemDisplayContext -> {
            if (itemDisplayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || itemDisplayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                return Float.valueOf(180.0f);
            }
            if (itemDisplayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
                return Float.valueOf(40.0f);
            }
            if (itemDisplayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
                return Float.valueOf(-40.0f);
            }
            return Float.valueOf(0.0f);
        }).addTranslation(ctx -> {
            if (ctx == ItemDisplayContext.GUI) {
                return new Vector3f(0.1f, -0.1f, 0.0f);
            }
            if (ctx == ItemDisplayContext.GROUND) {
                return new Vector3f();
            }
            return new Vector3f(0.1f, 0.0f, 0.0f);
        })));
    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.architectury.registry.registries.DeferredRegister
 *  dev.architectury.registry.registries.RegistrySupplier
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.entity.BlockEntityType$Builder
 */
package com.pla.spiral_abyss.init;

import com.pla.spiral_abyss.block.entity.AbyssPortalBlockEntity;
import com.pla.spiral_abyss.init.BlockInit;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class BlockEntityInit {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create((String)"spiral_abyss", (ResourceKey)Registries.f_256922_);
    public static final RegistrySupplier<BlockEntityType<AbyssPortalBlockEntity>> ABYSS_PORTAL = BLOCK_ENTITIES.register("abyss_gate_portal", () -> BlockEntityType.Builder.m_155273_(AbyssPortalBlockEntity::new, (Block[])new Block[]{(Block)BlockInit.ABYSS_GATE_ACTIVATE_BLOCK.get()}).m_58966_(null));

    private BlockEntityInit() {
    }
}


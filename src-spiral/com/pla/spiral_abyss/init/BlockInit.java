/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.architectury.registry.registries.DeferredRegister
 *  dev.architectury.registry.registries.RegistrySupplier
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockBehaviour
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.material.PushReaction
 */
package com.pla.spiral_abyss.init;

import com.pla.spiral_abyss.block.AbyssGateActivationBlock;
import com.pla.spiral_abyss.block.AbyssGateCurveBlock;
import com.pla.spiral_abyss.block.AbyssGateSlabBlock;
import com.pla.spiral_abyss.block.AbyssGateVerticalBlock;
import com.pla.spiral_abyss.block.AbyssPortalBlock;
import com.pla.spiral_abyss.block.AbyssRuneBlock;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

public class BlockInit {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create((String)"spiral_abyss", (ResourceKey)Registries.f_256747_);
    public static final RegistrySupplier<AbyssPortalBlock> ABYSS_PORTAL = BLOCKS.register("abyss_portal", () -> new AbyssPortalBlock(BlockBehaviour.Properties.m_60926_((BlockBehaviour)Blocks.f_50142_).m_222994_().m_60953_(state -> 15)));
    public static final RegistrySupplier<Block> SPINNING_BLOCK = BLOCKS.register("spinning_block", () -> new Block(BlockBehaviour.Properties.m_60926_((BlockBehaviour)Blocks.f_50752_)));
    public static final RegistrySupplier<Block> ABYSS_BASE_BLOCK = BLOCKS.register("abyss_base_block", () -> new Block(BlockInit.unbreakable()));
    public static final RegistrySupplier<Block> ABYSS_PILLAR = BLOCKS.register("abyss_pillar", () -> new Block(BlockInit.unbreakable()));
    public static final RegistrySupplier<Block> ABYSS_RUNE_1 = BLOCKS.register("abyss_rune_1", () -> new AbyssRuneBlock(BlockInit.unbreakable()));
    public static final RegistrySupplier<Block> ABYSS_RUNE_2 = BLOCKS.register("abyss_rune_2", () -> new AbyssRuneBlock(BlockInit.unbreakable()));
    public static final RegistrySupplier<Block> ABYSS_RUNE_3 = BLOCKS.register("abyss_rune_3", () -> new AbyssRuneBlock(BlockInit.unbreakable()));
    public static final RegistrySupplier<Block> ABYSS_RUNE_4 = BLOCKS.register("abyss_rune_4", () -> new AbyssRuneBlock(BlockInit.unbreakable()));
    public static final RegistrySupplier<Block> ABYSS_BORDER_MIDDLE = BLOCKS.register("abyss_border_middle", () -> new Block(BlockInit.unbreakable()));
    public static final RegistrySupplier<Block> ABYSS_BORDER_OUTSIDE = BLOCKS.register("abyss_border_outside", () -> new Block(BlockInit.unbreakable()));
    public static final RegistrySupplier<AbyssGateVerticalBlock> ABYSS_GATE_VERTICAL = BLOCKS.register("abyss_gate_vertical", () -> new AbyssGateVerticalBlock(BlockInit.gateProperties()));
    public static final RegistrySupplier<AbyssGateSlabBlock> ABYSS_GATE_HORIZONTAL = BLOCKS.register("abyss_gate_horizontal", () -> new AbyssGateSlabBlock(BlockInit.gateProperties()));
    public static final RegistrySupplier<AbyssGateCurveBlock> ABYSS_GATE_CURVE = BLOCKS.register("abyss_gate_curve", () -> new AbyssGateCurveBlock(BlockInit.gateProperties()));
    public static final RegistrySupplier<AbyssGateActivationBlock> ABYSS_GATE_ACTIVATE_BLOCK = BLOCKS.register("abyss_gate_activate_block", () -> new AbyssGateActivationBlock(BlockInit.gateProperties().m_60988_().m_60960_((state, level, pos) -> false).m_60971_((state, level, pos) -> false)));

    private static BlockBehaviour.Properties gateProperties() {
        return BlockBehaviour.Properties.m_60926_((BlockBehaviour)Blocks.f_50652_).m_60955_().m_278166_(PushReaction.BLOCK).m_60953_(state -> 15);
    }

    private static BlockBehaviour.Properties unbreakable() {
        return BlockBehaviour.Properties.m_60926_((BlockBehaviour)Blocks.f_50752_).m_60913_(-1.0f, 3600000.0f).m_60953_(state -> 15);
    }

    public static boolean isAbyssArchitecture(Block block) {
        return block == ABYSS_BASE_BLOCK.get() || block == ABYSS_PILLAR.get() || block == ABYSS_RUNE_1.get() || block == ABYSS_RUNE_2.get() || block == ABYSS_RUNE_3.get() || block == ABYSS_RUNE_4.get() || block == ABYSS_BORDER_MIDDLE.get() || block == ABYSS_BORDER_OUTSIDE.get();
    }
}


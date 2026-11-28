/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.bedrock.animations.Animation
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.RotatedPillarBlock
 *  net.minecraft.world.level.block.SlabBlock
 *  net.minecraft.world.level.block.StairBlock
 *  net.minecraft.world.level.block.WallBlock
 *  net.minecraft.world.level.block.entity.BlockEntityType$BlockEntitySupplier
 *  net.minecraft.world.level.block.state.BlockBehaviour
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.properties.NoteBlockInstrument
 *  net.minecraft.world.level.material.MapColor
 *  net.minecraftforge.registries.DeferredRegister
 */
package com.finderfeed.fdbosses.init;

import com.finderfeed.fdbosses.content.blocks.NoEntitySpawnBlock;
import com.finderfeed.fdbosses.content.blocks.TrophyBlock;
import com.finderfeed.fdbosses.content.entities.geburah.respawn_point_setter_block.GeburahRespiteBlock;
import com.finderfeed.fdbosses.content.tile_entities.TrophyBlockEntity;
import com.finderfeed.fdbosses.init.BossAnims;
import com.finderfeed.fdbosses.init.BossTileEntities;
import com.finderfeed.fdlib.systems.bedrock.animations.Animation;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;

public class BossBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create((ResourceKey)Registries.f_256747_, (String)"fdbosses");
    public static final Supplier<Block> NO_ENTITY_SPAWN_BLOCK = BLOCKS.register("no_entity_spawn_block", () -> new NoEntitySpawnBlock());
    public static final Supplier<TrophyBlock> CHESED_TROPHY = BLOCKS.register("chesed_trophy", () -> new TrophyBlock(BlockBehaviour.Properties.m_284310_().m_284180_(MapColor.f_283947_).m_280658_(NoteBlockInstrument.BASEDRUM).m_60913_(1.5f, 6.0f).m_60966_().m_60955_(), (Supplier<Animation>)BossAnims.CHESED_IDLE, null, (BlockEntityType.BlockEntitySupplier<? extends TrophyBlockEntity>)((BlockEntityType.BlockEntitySupplier)(pos, state) -> (TrophyBlockEntity)BossTileEntities.CHESED_TROPHY.get().m_155264_(pos, state))));
    public static final Supplier<TrophyBlock> MALKUTH_TROPHY = BLOCKS.register("malkuth_trophy", () -> new TrophyBlock(BlockBehaviour.Properties.m_284310_().m_284180_(MapColor.f_283947_).m_280658_(NoteBlockInstrument.BASEDRUM).m_60913_(1.5f, 6.0f).m_60966_().m_60955_(), (Supplier<Animation>)BossAnims.MALKUTH_SCREEN_IDLE, null, (BlockEntityType.BlockEntitySupplier<? extends TrophyBlockEntity>)((BlockEntityType.BlockEntitySupplier)(pos, state) -> (TrophyBlockEntity)BossTileEntities.MALKUTH_TROPHY.get().m_155264_(pos, state))));
    public static final Supplier<TrophyBlock> GEBURAH_TROPHY = BLOCKS.register("geburah_trophy", () -> new TrophyBlock(BlockBehaviour.Properties.m_284310_().m_284180_(MapColor.f_283947_).m_280658_(NoteBlockInstrument.BASEDRUM).m_60913_(1.5f, 6.0f).m_60966_().m_60955_(), null, null, (BlockEntityType.BlockEntitySupplier<? extends TrophyBlockEntity>)((BlockEntityType.BlockEntitySupplier)(pos, state) -> (TrophyBlockEntity)BossTileEntities.GEBURAH_TROPHY.get().m_155264_(pos, state))));
    public static final Supplier<Block> JUSTICESTONE_BRICKS = BLOCKS.register("justicestone_bricks", () -> new Block(BlockBehaviour.Properties.m_60926_((BlockBehaviour)Blocks.f_50069_)));
    public static final Supplier<Block> JUSTICESTONE_BRICKS_SLAB = BLOCKS.register("justicestone_bricks_slab", () -> new SlabBlock(BlockBehaviour.Properties.m_60926_((BlockBehaviour)Blocks.f_50069_)));
    public static final Supplier<Block> CHISELED_JUSTICESTONE_BRICKS = BLOCKS.register("chiseled_justicestone_bricks", () -> new Block(BlockBehaviour.Properties.m_60926_((BlockBehaviour)Blocks.f_50069_)));
    public static final Supplier<Block> JUSTICESTONE_BEAM = BLOCKS.register("justicestone_beam", () -> new RotatedPillarBlock(BlockBehaviour.Properties.m_60926_((BlockBehaviour)Blocks.f_50069_)));
    public static final Supplier<Block> JUSTICESTONE_COLUMN = BLOCKS.register("justicestone_column", () -> new RotatedPillarBlock(BlockBehaviour.Properties.m_60926_((BlockBehaviour)Blocks.f_50069_)));
    public static final Supplier<Block> JUSTICESTONE_WALL = BLOCKS.register("justicestone_wall", () -> new WallBlock(BlockBehaviour.Properties.m_60926_((BlockBehaviour)Blocks.f_50609_).m_280606_()));
    public static final Supplier<Block> JUSTICESTONE_COLUMN_WALL = BLOCKS.register("justicestone_column_wall", () -> new WallBlock(BlockBehaviour.Properties.m_60926_((BlockBehaviour)Blocks.f_50609_).m_280606_()));
    public static final Supplier<Block> JUSTICESTONE_STAIRS = BLOCKS.register("justicestone_stairs", () -> new StairBlock(() -> JUSTICESTONE_BRICKS.get().m_49966_(), BlockBehaviour.Properties.m_60926_((BlockBehaviour)Blocks.f_50635_)));
    public static final Supplier<Block> POLISHED_JUSTICESTONE = BLOCKS.register("polished_justicestone", () -> new Block(BlockBehaviour.Properties.m_60926_((BlockBehaviour)Blocks.f_50069_)));
    public static final Supplier<Block> POLISHED_JUSTICESTONE_SLAB = BLOCKS.register("polished_justicestone_slab", () -> new SlabBlock(BlockBehaviour.Properties.m_60926_((BlockBehaviour)Blocks.f_50069_)));
    public static final Supplier<Block> POLISHED_JUSTICESTONE_STAIRS = BLOCKS.register("polished_justicestone_stairs", () -> new StairBlock(() -> POLISHED_JUSTICESTONE.get().m_49966_(), BlockBehaviour.Properties.m_60926_((BlockBehaviour)Blocks.f_50635_)));
    public static final Supplier<Block> GEBURAH_RESPAWN_POINT_SETTER = BLOCKS.register("geburah_respite_point", () -> new GeburahRespiteBlock(BlockBehaviour.Properties.m_60926_((BlockBehaviour)Blocks.f_50752_)));
}


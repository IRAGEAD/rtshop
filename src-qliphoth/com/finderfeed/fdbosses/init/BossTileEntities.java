/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.entity.BlockEntityType$Builder
 *  net.minecraftforge.registries.DeferredRegister
 */
package com.finderfeed.fdbosses.init;

import com.finderfeed.fdbosses.content.entities.geburah.respawn_point_setter_block.GeburahRespiteBlockEntity;
import com.finderfeed.fdbosses.content.tile_entities.ChesedTrophyTileEntity;
import com.finderfeed.fdbosses.content.tile_entities.GeburahTrophyBlockEntity;
import com.finderfeed.fdbosses.content.tile_entities.MalkuthTrophyBlockEntity;
import com.finderfeed.fdbosses.content.tile_entities.TrophyBlockEntity;
import com.finderfeed.fdbosses.init.BossBlocks;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;

public class BossTileEntities {
    public static final DeferredRegister<BlockEntityType<?>> TILE_ENTITIES = DeferredRegister.create((ResourceKey)Registries.f_256922_, (String)"fdbosses");
    public static final Supplier<BlockEntityType<? extends TrophyBlockEntity>> CHESED_TROPHY = TILE_ENTITIES.register("chesed_trophy_tile_entity", () -> BlockEntityType.Builder.m_155273_(ChesedTrophyTileEntity::new, (Block[])new Block[]{(Block)BossBlocks.CHESED_TROPHY.get()}).m_58966_(null));
    public static final Supplier<BlockEntityType<? extends TrophyBlockEntity>> MALKUTH_TROPHY = TILE_ENTITIES.register("malkuth_trophy_tile_entity", () -> BlockEntityType.Builder.m_155273_(MalkuthTrophyBlockEntity::new, (Block[])new Block[]{(Block)BossBlocks.MALKUTH_TROPHY.get()}).m_58966_(null));
    public static final Supplier<BlockEntityType<? extends TrophyBlockEntity>> GEBURAH_TROPHY = TILE_ENTITIES.register("geburah_trophy_tile_entity", () -> BlockEntityType.Builder.m_155273_(GeburahTrophyBlockEntity::new, (Block[])new Block[]{(Block)BossBlocks.GEBURAH_TROPHY.get()}).m_58966_(null));
    public static final Supplier<BlockEntityType<GeburahRespiteBlockEntity>> GEBURAH_RESPITE_BLOCK = TILE_ENTITIES.register("geburah_respite_point", () -> BlockEntityType.Builder.m_155273_(GeburahRespiteBlockEntity::new, (Block[])new Block[]{BossBlocks.GEBURAH_RESPAWN_POINT_SETTER.get()}).m_58966_(null));
}


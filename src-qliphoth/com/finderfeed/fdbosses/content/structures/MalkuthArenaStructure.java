/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  com.mojang.serialization.codecs.RecordCodecBuilder$Instance
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Holder
 *  net.minecraft.world.level.ChunkPos
 *  net.minecraft.world.level.levelgen.Heightmap$Types
 *  net.minecraft.world.level.levelgen.structure.Structure
 *  net.minecraft.world.level.levelgen.structure.Structure$GenerationContext
 *  net.minecraft.world.level.levelgen.structure.Structure$GenerationStub
 *  net.minecraft.world.level.levelgen.structure.Structure$StructureSettings
 *  net.minecraft.world.level.levelgen.structure.StructureType
 *  net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement
 *  net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool
 */
package com.finderfeed.fdbosses.content.structures;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdbosses.init.BossStructures;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public class MalkuthArenaStructure
extends Structure {
    public static final Codec<MalkuthArenaStructure> CODEC = RecordCodecBuilder.create(p -> p.group((App)MalkuthArenaStructure.m_226567_((RecordCodecBuilder.Instance)p), (App)StructureTemplatePool.f_210555_.fieldOf("start_pool").forGetter(structure -> structure.startPool)).apply((Applicative)p, MalkuthArenaStructure::new));
    private final Holder<StructureTemplatePool> startPool;

    public MalkuthArenaStructure(Structure.StructureSettings structureSettings, Holder<StructureTemplatePool> startPool) {
        super(structureSettings);
        this.startPool = startPool;
    }

    protected Optional<Structure.GenerationStub> m_214086_(Structure.GenerationContext ctx) {
        ChunkPos chunkpos = ctx.f_226628_();
        BlockPos blockpos = new BlockPos(chunkpos.m_45604_() + 8, -1, chunkpos.m_45605_() + 8);
        int maxDistanceFromCenter = 128;
        return JigsawPlacement.m_227238_((Structure.GenerationContext)ctx, this.startPool, Optional.of(FDBosses.location("malkuth_arena_part_1")), (int)20, (BlockPos)blockpos, (boolean)false, Optional.of(Heightmap.Types.WORLD_SURFACE_WG), (int)maxDistanceFromCenter);
    }

    public StructureType<?> m_213658_() {
        return BossStructures.MALKUTH_ARENA.get();
    }
}


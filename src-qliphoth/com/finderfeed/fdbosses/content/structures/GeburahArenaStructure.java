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
 *  net.minecraft.world.level.block.Rotation
 *  net.minecraft.world.level.levelgen.structure.Structure
 *  net.minecraft.world.level.levelgen.structure.Structure$GenerationContext
 *  net.minecraft.world.level.levelgen.structure.Structure$GenerationStub
 *  net.minecraft.world.level.levelgen.structure.Structure$StructureSettings
 *  net.minecraft.world.level.levelgen.structure.StructureType
 *  net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement
 *  net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool
 */
package com.finderfeed.fdbosses.content.structures;

import com.finderfeed.fdbosses.init.BossStructures;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public class GeburahArenaStructure
extends Structure {
    public static final Codec<GeburahArenaStructure> CODEC = RecordCodecBuilder.create(p -> p.group((App)GeburahArenaStructure.m_226567_((RecordCodecBuilder.Instance)p), (App)StructureTemplatePool.f_210555_.fieldOf("start_pool").forGetter(structure -> structure.startPool)).apply((Applicative)p, GeburahArenaStructure::new));
    private final Holder<StructureTemplatePool> startPool;

    public GeburahArenaStructure(Structure.StructureSettings settings, Holder<StructureTemplatePool> startPool) {
        super(settings);
        this.startPool = startPool;
    }

    protected Optional<Structure.GenerationStub> m_214086_(Structure.GenerationContext ctx) {
        BlockPos pos = this.m_226582_(ctx, Rotation.NONE);
        if (pos.m_123342_() != -1) {
            return Optional.empty();
        }
        BlockPos blockpos = new BlockPos(pos.m_123341_(), 140, pos.m_123343_());
        return JigsawPlacement.m_227238_((Structure.GenerationContext)ctx, this.startPool, Optional.empty(), (int)20, (BlockPos)blockpos, (boolean)false, Optional.empty(), (int)200);
    }

    public StructureType<?> m_213658_() {
        return BossStructures.GEBURAH_ARENA.get();
    }
}


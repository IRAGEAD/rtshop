/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.sugar.ref.LocalDoubleRef
 *  it.unimi.dsi.fastutil.objects.ObjectList
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.level.ChunkPos
 *  net.minecraft.world.level.StructureManager
 *  net.minecraft.world.level.block.Rotation
 *  net.minecraft.world.level.levelgen.Beardifier$Rigid
 *  net.minecraft.world.level.levelgen.DensityFunction$FunctionContext
 *  net.minecraft.world.level.levelgen.structure.BoundingBox
 *  net.minecraft.world.level.levelgen.structure.Structure
 *  net.minecraft.world.level.levelgen.structure.StructureStart
 *  net.minecraft.world.level.levelgen.structure.TerrainAdjustment
 *  net.minecraft.world.phys.Vec3
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.finderfeed.fdbosses;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdbosses.content.structures.MalkuthArenaStructure;
import com.finderfeed.fdbosses.content.structures.malkuth.MalkuthStructureBoundingBox;
import com.llamalad7.mixinextras.sugar.ref.LocalDoubleRef;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class BossMixinHandler {
    public static void computeMixin(DensityFunction.FunctionContext p_208200_, CallbackInfoReturnable<Double> cir, LocalDoubleRef d0) {
    }

    public static boolean addBeardifiersToStructureManually(StructureManager structureManager, ChunkPos chunkPos, ObjectList<Beardifier.Rigid> beardifiers, StructureStart structureStart) {
        Structure structure = structureStart.m_226861_();
        if (structure instanceof MalkuthArenaStructure) {
            MalkuthArenaStructure malkuthArenaStructure = (MalkuthArenaStructure)structure;
            int centerBBRadius = 60;
            int height = 40;
            BoundingBox bb = structureStart.m_73601_();
            BlockPos center = bb.m_162394_();
            BlockPos startPos = new BlockPos(center.m_123341_(), bb.m_162396_(), center.m_123343_());
            MalkuthStructureBoundingBox centerBB = new MalkuthStructureBoundingBox(new BoundingBox(-centerBBRadius, -height, -centerBBRadius, centerBBRadius, -6, centerBBRadius).m_71045_(startPos.m_123341_(), startPos.m_123342_(), startPos.m_123343_()));
            if (BossMixinHandler.boxIntersectsChunk(centerBB, chunkPos, 6)) {
                beardifiers.add((Object)new Beardifier.Rigid((BoundingBox)centerBB, TerrainAdjustment.NONE, 0));
            }
            int otherBoxRadius = 13;
            BossMixinHandler.addMalkuthArenaBeardifiersInRadius(chunkPos, startPos, structureManager, beardifiers, centerBBRadius + otherBoxRadius - 1, otherBoxRadius, 6, 32, height);
            return true;
        }
        return false;
    }

    private static boolean boxIntersectsChunk(BoundingBox box, ChunkPos chunkPos, int bigger) {
        return box.m_71019_(chunkPos.m_45604_() - bigger, chunkPos.m_45605_() - bigger, chunkPos.m_45608_() + bigger, chunkPos.m_45609_() + bigger);
    }

    private static void addMalkuthArenaBeardifiersInRadius(ChunkPos chunkPos, BlockPos startPos, StructureManager structureManager, ObjectList<Beardifier.Rigid> beardifiers, int startRadius, int boxRadius, int verticalOffset, int count, int height) {
        float angle = (float)Math.PI * 2 / (float)count;
        for (int i = 0; i < count; ++i) {
            Vec3 offset = new Vec3((double)startRadius, 0.0, 0.0).m_82524_(angle * (float)i);
            BlockPos pos = startPos.m_7918_((int)Math.floor(offset.f_82479_), (int)Math.floor(offset.f_82480_), (int)Math.floor(offset.f_82481_));
            MalkuthStructureBoundingBox box = new MalkuthStructureBoundingBox(new BoundingBox(-boxRadius, -height, -boxRadius, boxRadius, -verticalOffset, boxRadius).m_71045_(pos.m_123341_(), pos.m_123342_(), pos.m_123343_()));
            if (!BossMixinHandler.boxIntersectsChunk(box, chunkPos, 6)) continue;
            beardifiers.add((Object)new Beardifier.Rigid((BoundingBox)box, TerrainAdjustment.NONE, 0));
        }
    }

    public static Rotation getRotationForStructure(Optional<ResourceLocation> location) {
        if (location.isPresent() && location.get().equals((Object)FDBosses.location("malkuth_arena_part_1"))) {
            return Rotation.NONE;
        }
        return null;
    }
}


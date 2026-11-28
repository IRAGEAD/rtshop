/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.sugar.Local
 *  com.llamalad7.mixinextras.sugar.ref.LocalDoubleRef
 *  it.unimi.dsi.fastutil.objects.ObjectList
 *  net.minecraft.util.Mth
 *  net.minecraft.world.level.ChunkPos
 *  net.minecraft.world.level.StructureManager
 *  net.minecraft.world.level.levelgen.Beardifier
 *  net.minecraft.world.level.levelgen.Beardifier$Rigid
 *  net.minecraft.world.level.levelgen.DensityFunction$FunctionContext
 *  net.minecraft.world.level.levelgen.structure.BoundingBox
 *  net.minecraft.world.level.levelgen.structure.StructureStart
 *  net.minecraft.world.level.levelgen.structure.pools.JigsawJunction
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.At$Shift
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.finderfeed.fdbosses.mixin;

import com.finderfeed.fdbosses.BossMixinHandler;
import com.finderfeed.fdbosses.content.structures.malkuth.MalkuthStructureBoundingBox;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalDoubleRef;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={Beardifier.class})
public class BeardifierMixin {
    @Inject(method={"forStructuresInChunk"}, at={@At(value="INVOKE", target="Lnet/minecraft/world/level/StructureManager;startsForStructure(Lnet/minecraft/world/level/ChunkPos;Ljava/util/function/Predicate;)Ljava/util/List;")})
    private static void forStructuresInChunk(StructureManager structureManager, ChunkPos chunkPos, CallbackInfoReturnable<Beardifier> cir, @Local(ordinal=0) ObjectList<Beardifier.Rigid> beardifiers, @Local(ordinal=1) ObjectList<JigsawJunction> jigsawJunctions) {
        List starts = structureManager.m_220477_(chunkPos, structure -> true);
        for (StructureStart structureStart : starts) {
            BossMixinHandler.addBeardifiersToStructureManually(structureManager, chunkPos, beardifiers, structureStart);
        }
    }

    @Inject(method={"compute"}, at={@At(value="INVOKE", target="Lnet/minecraft/world/level/levelgen/Beardifier$Rigid;terrainAdjustment()Lnet/minecraft/world/level/levelgen/structure/TerrainAdjustment;", ordinal=1, shift=At.Shift.AFTER)})
    public void compute(DensityFunction.FunctionContext ctx, CallbackInfoReturnable<Double> cir, @Local Beardifier.Rigid beardifier, @Local(ordinal=0) LocalDoubleRef d0) {
        int i = ctx.m_207115_();
        int j = ctx.m_207114_();
        int k = ctx.m_207113_();
        BoundingBox boundingbox = beardifier.f_223944_();
        if (boundingbox instanceof MalkuthStructureBoundingBox) {
            int l = beardifier.f_223946_();
            int i1 = Math.max(0, Math.max(boundingbox.m_162395_() - i, i - boundingbox.m_162399_()));
            int j1 = Math.max(0, Math.max(boundingbox.m_162398_() - k, k - boundingbox.m_162401_()));
            int k1 = boundingbox.m_162396_() + l;
            int l1 = j - k1;
            int i2 = Math.max(0, Math.max(boundingbox.m_162396_() - j, j - boundingbox.m_162400_()));
            double addition = BeardifierMixin.getMyBuryContribution((double)i1 / 2.0, (double)i2 / 2.0, (double)j1 / 2.0) * 0.8;
            d0.set(d0.get() + addition);
        }
    }

    private static double getMyBuryContribution(double p_340947_, double p_340921_, double p_341266_) {
        double d0 = Mth.m_184648_((double)p_340947_, (double)p_340921_, (double)p_341266_);
        return Mth.m_144851_((double)d0, (double)0.0, (double)6.0, (double)1.0, (double)0.0);
    }
}


package com.rtshop.mixin;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 단계적 이야기 건축물: 아직 풀리지 않은 건축물은 새 지형에 생기지 않게 한다 (StagedStructures).
 * ChunkGenerator.tryGenerateStructure 를 이름 대신 인자 모양으로 찾는다 (개발 환경과 실제 게임의 메서드 이름이 달라서).
 */
@Mixin(value = ChunkGenerator.class, remap = false)
public abstract class StagedStructureMixin {
    @Inject(method = "*(Lnet/minecraft/world/level/levelgen/structure/StructureSet$StructureSelectionEntry;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/core/RegistryAccess;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplateManager;JLnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/core/SectionPos;)Z",
            at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void rtshop$stagedStructure(StructureSet.StructureSelectionEntry entry, StructureManager sm, RegistryAccess ra, RandomState rs,
                                        StructureTemplateManager stm, long seed, ChunkAccess chunk, ChunkPos pos, SectionPos sp,
                                        CallbackInfoReturnable<Boolean> cir) {
        try {
            String id = entry.structure().unwrapKey().map(k -> k.location().toString()).orElse("");
            if (com.rtshop.story.StagedStructures.blocks(id)) cir.setReturnValue(false);
        } catch (Throwable ignored) {
        }
    }
}

/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Position
 *  net.minecraft.core.Vec3i
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.ChunkPos
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.ServerLevelAccessor
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings
 *  net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate
 *  net.minecraft.world.phys.Vec3
 */
package com.pla.spiral_abyss.world;

import com.mojang.logging.LogUtils;
import com.pla.spiral_abyss.event.AbyssArchitectureRepair;
import com.pla.spiral_abyss.world.AbyssPlayers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

public final class AbyssDimension {
    public static final ResourceKey<Level> LEVEL = ResourceKey.m_135785_((ResourceKey)Registries.f_256858_, (ResourceLocation)new ResourceLocation("spiral_abyss", "spiral_abyss"));
    private static final ResourceLocation ARENA = new ResourceLocation("spiral_abyss", "abyss_base");

    private AbyssDimension() {
    }

    public static void initializeArena(ServerLevel level, AbyssPlayers.Profile profile) {
        if (!level.m_46472_().equals(LEVEL)) {
            throw new IllegalArgumentException("Not the Abyss");
        }
        Vec3 center = profile.bounds().center();
        for (int x = -1; x <= 1; ++x) {
            for (int z = -1; z <= 1; ++z) {
                int cx = ((int)center.f_82479_ >> 4) + x;
                int cz = ((int)center.f_82481_ >> 4) + z;
                if (!level.m_8902_().contains(ChunkPos.m_45589_((int)cx, (int)cz))) {
                    level.m_8602_(cx, cz, true);
                }
                level.m_6325_(cx, cz);
            }
        }
        AbyssDimension.restoreArena(level, profile);
    }

    public static void restoreArena(ServerLevel level, AbyssPlayers.Profile profile) {
        if (!level.m_46472_().equals(LEVEL)) {
            throw new IllegalArgumentException("Not the Abyss");
        }
        AbyssArchitectureRepair.edit(level, () -> AbyssDimension.restoreArenaBlocks(level, profile));
    }

    private static void restoreArenaBlocks(ServerLevel level, AbyssPlayers.Profile profile) {
        Vec3 center = profile.bounds().center();
        StructureTemplate template = (StructureTemplate)level.m_215082_().m_230407_(ARENA).orElseThrow(() -> new IllegalStateException("Missing abyss_base template"));
        Vec3i size = template.m_163801_();
        if (size.m_123341_() != 65 || size.m_123342_() != 6 || size.m_123343_() != 65) {
            throw new IllegalStateException("abyss_base must fit its 65 x 6 x 65 spawn footprint");
        }
        BlockPos origin = BlockPos.m_274446_((Position)center).m_7918_(-size.m_123341_() / 2, -1, -size.m_123343_() / 2);
        BlockPos top = new BlockPos(origin.m_123341_() + size.m_123341_() - 1, 185, origin.m_123343_() + size.m_123343_() - 1);
        AbyssArchitectureRepair.forget(level, origin, top.m_122029_());
        if (profile.needsLegacyCleanup()) {
            AbyssDimension.clearLegacyArena(level, BlockPos.m_274446_((Position)center));
        }
        for (BlockPos pos : BlockPos.m_121940_((BlockPos)origin, (BlockPos)top)) {
            if (level.m_46859_(pos)) continue;
            level.m_7731_(pos, Blocks.f_50016_.m_49966_(), 2);
        }
        if (!template.m_230328_((ServerLevelAccessor)level, origin, origin, new StructurePlaceSettings().m_74392_(true), level.f_46441_, 2)) {
            throw new IllegalStateException("Arena placement failed");
        }
        profile.markPlaced();
        LogUtils.getLogger().info("Restored Abyss arena for host {} in slot {} at {}", new Object[]{profile.owner, profile.slot, origin});
    }

    private static void clearLegacyArena(ServerLevel level, BlockPos center) {
        for (BlockPos pos : BlockPos.m_121940_((BlockPos)center.m_7918_(-32, -1, -32), (BlockPos)center.m_7918_(33, 19, 32))) {
            if (level.m_46859_(pos)) continue;
            level.m_7731_(pos, Blocks.f_50016_.m_49966_(), 2);
        }
    }

    public static void releaseArena(ServerLevel level, AbyssPlayers.Profile profile) {
        Vec3 center = profile.bounds().center();
        for (int x = -1; x <= 1; ++x) {
            for (int z = -1; z <= 1; ++z) {
                int cx = ((int)center.f_82479_ >> 4) + x;
                int cz = ((int)center.f_82481_ >> 4) + z;
                if (!level.m_8902_().contains(ChunkPos.m_45589_((int)cx, (int)cz))) continue;
                level.m_8602_(cx, cz, false);
            }
        }
    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 */
package com.pla.spiral_abyss.boss_enviroment;

import com.pla.spiral_abyss.boss_enviroment.BossEnvironmentCatalog;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class BossTerrainPlan {
    public static final int ROOM_RADIUS = 29;
    public static final int ROOM_WALL_HEIGHT = 6;
    public static final int ROOM_BOX_HEIGHT = 32;

    private BossTerrainPlan() {
    }

    public static boolean reserved(int x, int z) {
        return x * x + z * z <= 36 || Math.abs(x) <= 3 && z >= 0;
    }

    public static double distance(BlockPos pos, BlockPos center) {
        return Math.hypot(pos.m_123341_() - center.m_123341_(), pos.m_123343_() - center.m_123343_());
    }

    public static Comparator<Placement> outward(BlockPos center) {
        return Comparator.comparingDouble(placement -> BossTerrainPlan.distance(placement.pos(), center)).thenComparingInt(placement -> placement.pos().m_123342_());
    }

    public static List<Placement> patches(BossEnvironmentCatalog.Profile profile, BlockPos center, RandomSource random) {
        if (profile.palette().isEmpty()) {
            return List.of();
        }
        if (profile.flatRoom()) {
            return BossTerrainPlan.flatRoom(profile, center, random);
        }
        record Ridge(double x, double z, double width) {
        }
        ArrayList<Ridge> ridges = new ArrayList<Ridge>();
        double rotation = random.m_188500_() * Math.PI * 2.0;
        for (int index = 0; index < profile.patches(); ++index) {
            double angle = rotation + ((double)index + random.m_188500_() * 0.4) * Math.PI * 2.0 / (double)profile.patches();
            double radius = 9.0 + random.m_188500_() * (double)Math.max(1, profile.radius() - 15);
            ridges.add(new Ridge(Math.cos(angle) * radius, Math.sin(angle) * radius, 3.0 + random.m_188500_() * 4.0));
        }
        ArrayList<Placement> result = new ArrayList<Placement>();
        for (int x = -profile.radius(); x <= profile.radius(); ++x) {
            for (int z = -profile.radius(); z <= profile.radius(); ++z) {
                int height = 1;
                if (!BossTerrainPlan.reserved(x, z)) {
                    for (int index = 0; index < ridges.size(); ++index) {
                        double dz;
                        Ridge from = (Ridge)ridges.get(index);
                        Ridge to = (Ridge)ridges.get((index + 1) % ridges.size());
                        double dx = to.x() - from.x();
                        double length = dx * dx + (dz = to.z() - from.z()) * dz;
                        double along = length == 0.0 ? 0.0 : Math.max(0.0, Math.min(1.0, (((double)x - from.x()) * dx + ((double)z - from.z()) * dz) / length));
                        double width = from.width() + (to.width() - from.width()) * along;
                        double distance = Math.hypot((double)x - from.x() - dx * along, (double)z - from.z() - dz * along);
                        double shape = Math.max(0.0, 1.0 - distance / width);
                        height = Math.max(height, 1 + (int)Math.ceil(shape * (double)(profile.height() - 1)));
                    }
                }
                for (int y = 0; y < height; ++y) {
                    BlockState state = profile.snowDirt() ? (y == height - 1 ? Blocks.f_50440_ : Blocks.f_50493_).m_49966_() : profile.palette().get(random.m_188503_(profile.palette().size()));
                    result.add(new Placement(center.m_7918_(x, y, z), state, 0));
                }
                if (profile.snowDirt()) {
                    result.add(new Placement(center.m_7918_(x, height, z), Blocks.f_50125_.m_49966_(), 1));
                    continue;
                }
                if (BossTerrainPlan.reserved(x, z) || profile.decorations().isEmpty() || !(random.m_188500_() < profile.hazardChance())) continue;
                result.add(new Placement(center.m_7918_(x, height, z), profile.decorations().get(random.m_188503_(profile.decorations().size())), 1));
            }
        }
        result.sort(BossTerrainPlan.outward(center));
        return List.copyOf(result);
    }

    private static List<Placement> flatRoom(BossEnvironmentCatalog.Profile profile, BlockPos center, RandomSource random) {
        ArrayList<Placement> result = new ArrayList<Placement>();
        int radius = profile.roomRadius();
        int wallHeight = profile.roomBox() ? 32 : 6;
        for (int x = -radius; x <= radius; ++x) {
            for (int z = -radius; z <= radius; ++z) {
                result.add(new Placement(center.m_7918_(x, -1, z), profile.palette().get(random.m_188503_(profile.palette().size())), 2));
                boolean wall = Math.abs(x) == radius || Math.abs(z) == radius;
                for (int y = 0; y < (radius > 29 ? 32 : wallHeight); ++y) {
                    result.add(new Placement(center.m_7918_(x, y, z), wall && y < wallHeight ? profile.palette().get(random.m_188503_(profile.palette().size())) : Blocks.f_50016_.m_49966_(), 2));
                }
                if (!profile.roomBox()) continue;
                result.add(new Placement(center.m_7918_(x, 32, z), profile.palette().get(random.m_188503_(profile.palette().size())), 2));
            }
        }
        result.sort(BossTerrainPlan.outward(center));
        return List.copyOf(result);
    }

    public record Placement(BlockPos pos, BlockState state, int priority) {
    }
}


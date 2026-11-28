/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.levelgen.structure.BoundingBox
 */
package com.pla.spiral_abyss.event;

import com.pla.spiral_abyss.event.BossDeathBlocks;
import com.pla.spiral_abyss.init.BlockInit;
import com.pla.spiral_abyss.platform.event.AbyssEvents;
import com.pla.spiral_abyss.world.AbyssDimension;
import com.pla.spiral_abyss.world.AbyssParties;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public final class AbyssArchitectureRepair {
    private static final Map<ServerLevel, Pending> LEVELS = new WeakHashMap<ServerLevel, Pending>();

    private AbyssArchitectureRepair() {
    }

    public static void removed(Level level, BlockPos pos, BlockState previous, BlockState replacement) {
        ServerLevel server;
        block9: {
            block8: {
                if (!(level instanceof ServerLevel)) break block8;
                server = (ServerLevel)level;
                if (level.m_46472_().equals(AbyssDimension.LEVEL)) break block9;
            }
            return;
        }
        Pending existing = LEVELS.get(server);
        if (existing != null && existing.editDepth > 0) {
            existing.blocks.remove(pos);
            return;
        }
        if (previous == null) {
            return;
        }
        if ((double)pos.m_123342_() == 64.0 && AbyssParties.queueBossFloorRepair(server, pos)) {
            return;
        }
        if (previous.m_60713_(replacement.m_60734_()) || !BlockInit.isAbyssArchitecture(previous.m_60734_())) {
            return;
        }
        Pending pending = LEVELS.computeIfAbsent(server, ignored -> new Pending());
        if (pending.creativeBreaks.remove(pos)) {
            return;
        }
        pending.blocks.putIfAbsent(pos.m_7949_(), previous);
    }

    public static void edit(ServerLevel level, Runnable action) {
        Pending pending = LEVELS.computeIfAbsent(level, ignored -> new Pending());
        ++pending.editDepth;
        try {
            BossDeathBlocks.edit(action);
        }
        finally {
            --pending.editDepth;
        }
    }

    public static void forget(ServerLevel level, BlockPos from, BlockPos to) {
        Pending pending = LEVELS.get(level);
        if (pending == null) {
            return;
        }
        BoundingBox area = BoundingBox.m_162375_((Vec3i)from, (Vec3i)to);
        pending.blocks.keySet().removeIf(arg_0 -> ((BoundingBox)area).m_71051_(arg_0));
        pending.creativeBreaks.removeIf(arg_0 -> ((BoundingBox)area).m_71051_(arg_0));
    }

    public static void creativeBreak(AbyssEvents.BlockEvent.BreakEvent event) {
        ServerLevel level;
        LevelAccessor levelAccessor;
        if (event.getPlayer().m_7500_() && (levelAccessor = event.getLevel()) instanceof ServerLevel && (level = (ServerLevel)levelAccessor).m_46472_().equals(AbyssDimension.LEVEL)) {
            Pending pending = LEVELS.computeIfAbsent(level, ignored -> new Pending());
            pending.creativeBreaks.add(event.getPos().m_7949_());
            pending.blocks.remove(event.getPos());
        }
    }

    public static void tick(AbyssEvents.TickEvent.LevelTickEvent event) {
        Level level;
        if (event.phase == AbyssEvents.TickEvent.Phase.END && (level = event.level) instanceof ServerLevel) {
            ServerLevel level2 = (ServerLevel)level;
            AbyssArchitectureRepair.repair(level2);
        }
    }

    public static void repair(ServerLevel level) {
        Pending pending = LEVELS.get(level);
        if (pending == null) {
            return;
        }
        pending.creativeBreaks.clear();
        if (pending.blocks.isEmpty()) {
            return;
        }
        HashMap<BlockPos, BlockState> repairs = new HashMap<BlockPos, BlockState>(pending.blocks);
        pending.blocks.clear();
        AbyssArchitectureRepair.edit(level, () -> repairs.forEach((pos, state) -> {
            if (level.m_46805_(pos) && level.m_8055_(pos) != state) {
                level.m_7731_(pos, state, 3);
            }
        }));
    }

    public static void unload(AbyssEvents.LevelEvent.Unload event) {
        LevelAccessor levelAccessor = event.getLevel();
        if (levelAccessor instanceof ServerLevel) {
            ServerLevel level = (ServerLevel)levelAccessor;
            LEVELS.remove(level);
        }
    }

    private static final class Pending {
        final Map<BlockPos, BlockState> blocks = new HashMap<BlockPos, BlockState>();
        final Set<BlockPos> creativeBreaks = new HashSet<BlockPos>();
        int editDepth;

        private Pending() {
        }
    }
}


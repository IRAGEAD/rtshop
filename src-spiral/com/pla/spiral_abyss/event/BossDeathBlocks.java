/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.state.BlockState
 */
package com.pla.spiral_abyss.event;

import com.pla.spiral_abyss.encounter.MobTickContext;
import com.pla.spiral_abyss.encounter.data.DeathBlockRule;
import com.pla.spiral_abyss.encounter.data.MobCatalog;
import com.pla.spiral_abyss.platform.AbyssPlatform;
import com.pla.spiral_abyss.platform.AbyssRegistries;
import com.pla.spiral_abyss.platform.event.AbyssEvents;
import com.pla.spiral_abyss.world.AbyssDimension;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class BossDeathBlocks {
    public static final String TAG = "spiral_abyss_suppress_death_blocks";
    private static final Map<ServerLevel, Map<UUID, Guard>> GUARDS = new WeakHashMap<ServerLevel, Map<UUID, Guard>>();
    private static final ThreadLocal<Integer> EDIT_DEPTH = ThreadLocal.withInitial(() -> 0);

    private BossDeathBlocks() {
    }

    public static void joined(AbyssEvents.EntityJoinLevelEvent event) {
        Mob mob;
        block5: {
            block4: {
                Entity entity = event.getEntity();
                if (!(entity instanceof Mob)) break block4;
                mob = (Mob)entity;
                if (event.getLevel() instanceof ServerLevel && event.getLevel().m_46472_().equals(AbyssDimension.LEVEL) && AbyssPlatform.data((Entity)mob).m_128403_("spiral_abyss_run")) break block5;
            }
            return;
        }
        CompoundTag data = AbyssPlatform.data((Entity)mob);
        if (!data.m_128441_(TAG) && MobCatalog.bossOptions(AbyssRegistries.ENTITY_TYPES.getKey(mob.m_6095_())).deathBlocks().configured()) {
            data.m_128379_(TAG, true);
        }
    }

    public static void died(AbyssEvents.LivingDeathEvent event) {
        ServerLevel level;
        Mob mob;
        LivingEntity livingEntity = event.getEntity();
        if (!(livingEntity instanceof Mob && (livingEntity = (mob = (Mob)livingEntity).m_9236_()) instanceof ServerLevel && (level = (ServerLevel)livingEntity).m_46472_().equals(AbyssDimension.LEVEL) && (!AbyssPlatform.data((Entity)mob).m_128441_(TAG) ? AbyssPlatform.data((Entity)mob).m_128403_("spiral_abyss_run") : AbyssPlatform.data((Entity)mob).m_128471_(TAG)))) {
            return;
        }
        DeathBlockRule rule = MobCatalog.bossOptions(AbyssRegistries.ENTITY_TYPES.getKey(mob.m_6095_())).deathBlocks();
        if (!rule.configured()) {
            return;
        }
        GUARDS.computeIfAbsent(level, unused -> new HashMap()).put(mob.m_20148_(), new Guard(mob.m_20183_().m_7949_(), rule, level.m_46467_() + (long)rule.ticks()));
    }

    public static boolean suppress(Level level, BlockPos pos, BlockState state) {
        ServerLevel server;
        block7: {
            block6: {
                if (!(level instanceof ServerLevel)) break block6;
                server = (ServerLevel)level;
                if (level.m_46472_().equals(AbyssDimension.LEVEL) && EDIT_DEPTH.get() <= 0) break block7;
            }
            return false;
        }
        if (BossDeathBlocks.suppressMobChange(level)) {
            return true;
        }
        Guard scripted = BossDeathBlocks.scriptedGuard(level);
        if (scripted != null && scripted.contains(pos) && scripted.rule().blocks().contains(AbyssRegistries.BLOCKS.getKey(state.m_60734_()))) {
            return true;
        }
        Map<UUID, Guard> guards = GUARDS.get(server);
        if (guards == null || guards.isEmpty()) {
            return false;
        }
        ResourceLocation id = AbyssRegistries.BLOCKS.getKey(state.m_60734_());
        long now = level.m_46467_();
        return guards.values().stream().anyMatch(guard -> now <= guard.until() && guard.rule().blocks().contains(id) && guard.contains(pos));
    }

    public static boolean suppressMobChange(Level level) {
        LivingEntity source = MobTickContext.blockChangeSource();
        return level instanceof ServerLevel && level.m_46472_().equals(AbyssDimension.LEVEL) && EDIT_DEPTH.get() == 0 && source != null && source.m_9236_() == level;
    }

    public static boolean suppressLoot(Level level, BlockPos pos) {
        ServerLevel server;
        block5: {
            block4: {
                if (!(level instanceof ServerLevel)) break block4;
                server = (ServerLevel)level;
                if (level.m_46472_().equals(AbyssDimension.LEVEL) && EDIT_DEPTH.get() <= 0) break block5;
            }
            return false;
        }
        Guard scripted = BossDeathBlocks.scriptedGuard(level);
        if (scripted != null && scripted.contains(pos)) {
            return true;
        }
        Map<UUID, Guard> guards = GUARDS.get(server);
        return guards != null && guards.values().stream().anyMatch(guard -> level.m_46467_() <= guard.until() && guard.contains(pos));
    }

    private static Guard scriptedGuard(Level level) {
        LivingEntity source = MobTickContext.scriptedRewardSource();
        if (source == null || source.m_9236_() != level) {
            return null;
        }
        DeathBlockRule rule = MobCatalog.bossOptions(AbyssRegistries.ENTITY_TYPES.getKey(source.m_6095_())).deathBlocks();
        return rule.configured() ? new Guard(source.m_20183_(), rule, level.m_46467_()) : null;
    }

    public static void edit(Runnable action) {
        int depth = EDIT_DEPTH.get();
        EDIT_DEPTH.set(depth + 1);
        try {
            action.run();
        }
        finally {
            if (depth == 0) {
                EDIT_DEPTH.remove();
            } else {
                EDIT_DEPTH.set(depth);
            }
        }
    }

    public static void tick(AbyssEvents.TickEvent.LevelTickEvent event) {
        Level level;
        if (event.phase != AbyssEvents.TickEvent.Phase.END || !((level = event.level) instanceof ServerLevel)) {
            return;
        }
        ServerLevel level2 = (ServerLevel)level;
        Map<UUID, Guard> guards = GUARDS.get(level2);
        if (guards == null) {
            return;
        }
        guards.values().removeIf(guard -> level2.m_46467_() > guard.until());
        if (guards.isEmpty()) {
            GUARDS.remove(level2);
        }
    }

    public static void unload(AbyssEvents.LevelEvent.Unload event) {
        GUARDS.remove(event.getLevel());
    }

    private record Guard(BlockPos origin, DeathBlockRule rule, long until) {
        boolean contains(BlockPos pos) {
            return Math.abs((long)pos.m_123341_() - (long)this.origin.m_123341_()) <= (long)this.rule.radius() && Math.abs((long)pos.m_123343_() - (long)this.origin.m_123343_()) <= (long)this.rule.radius() && pos.m_123342_() >= this.origin.m_123342_() && (long)pos.m_123342_() - (long)this.origin.m_123342_() <= (long)this.rule.height();
        }
    }
}


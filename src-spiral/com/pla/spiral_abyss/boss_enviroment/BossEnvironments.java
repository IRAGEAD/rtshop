/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonParseException
 *  com.google.gson.JsonPrimitive
 *  com.mojang.logging.LogUtils
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction$Axis
 *  net.minecraft.core.Position
 *  net.minecraft.core.Vec3i
 *  net.minecraft.core.particles.BlockParticleOption
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleTypes
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.entity.boss.enderdragon.EndCrystal
 *  net.minecraft.world.entity.boss.enderdragon.EnderDragon
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelReader
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.DoublePlantBlock
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.DoubleBlockHalf
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 *  net.minecraft.world.phys.shapes.VoxelShape
 */
package com.pla.spiral_abyss.boss_enviroment;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.mojang.logging.LogUtils;
import com.pla.spiral_abyss.boss_enviroment.BossEnvironmentCatalog;
import com.pla.spiral_abyss.boss_enviroment.BossPlacedEntities;
import com.pla.spiral_abyss.boss_enviroment.BossTerrainPlan;
import com.pla.spiral_abyss.boss_enviroment.BossWater;
import com.pla.spiral_abyss.boss_enviroment.EnvironmentPlacements;
import com.pla.spiral_abyss.boss_enviroment.TerrainLayers;
import com.pla.spiral_abyss.encounter.DragonArena;
import com.pla.spiral_abyss.encounter.QliphothEncounters;
import com.pla.spiral_abyss.encounter.data.MobCatalog;
import com.pla.spiral_abyss.event.AbyssArchitectureRepair;
import com.pla.spiral_abyss.platform.AbyssPlatform;
import com.pla.spiral_abyss.platform.AbyssRegistries;
import com.pla.spiral_abyss.world.AbyssDimension;
import com.pla.spiral_abyss.world.ArenaBounds;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class BossEnvironments {
    public static final String CRYSTAL_OWNER = "spiral_abyss_crystal_owner";
    public static final String CRYSTAL_RUN = "spiral_abyss_crystal_run";
    public static final int CLEANUP_TICKS = 100;
    private final ServerLevel level;
    private final ArenaBounds bounds;
    private final UUID host;
    private final TerrainLayers<BlockPos, BlockState> terrain;
    private final Map<UUID, Effect> effects = new LinkedHashMap<UUID, Effect>();
    private final Set<BlockPos> damagedFloor = new HashSet<BlockPos>();

    public BossEnvironments(ServerLevel level, ArenaBounds bounds, UUID host) {
        if (!level.m_46472_().equals(AbyssDimension.LEVEL)) {
            throw new IllegalArgumentException("Boss environment requires the Abyss");
        }
        this.level = level;
        this.bounds = bounds;
        this.host = host;
        this.terrain = new TerrainLayers<BlockPos, BlockState>(arg_0 -> ((ServerLevel)level).m_8055_(arg_0), (pos, state) -> {
            boolean plant = state.m_60734_() instanceof DoublePlantBlock || level.m_8055_(pos).m_60734_() instanceof DoublePlantBlock;
            AbyssArchitectureRepair.edit(level, () -> level.m_7731_(pos, state, plant ? 18 : 3));
        });
    }

    public boolean protects(BlockPos pos) {
        return this.terrain.contains(pos) || this.effects.values().stream().anyMatch(effect -> effect.water != null && effect.water.contains(pos));
    }

    public boolean queueFloorRepair(BlockPos pos) {
        if ((double)pos.m_123342_() != 64.0 || !this.terrain.contains(pos)) {
            return false;
        }
        if (this.effects.values().stream().noneMatch(effect -> effect.profile.flatRoom() && effect.placed.contains(pos))) {
            return false;
        }
        this.damagedFloor.add(pos.m_7949_());
        return true;
    }

    public boolean ownsPlacement(Entity entity) {
        return entity.m_9236_() == this.level && this.effects.values().stream().anyMatch(effect -> effect.cleanupTicks < 0 && (effect.entities.owns(entity) || QliphothEncounters.owns(entity) && effect.boss.m_20148_().equals(AbyssPlatform.data(entity).m_128342_("spiral_abyss_environment_owner"))));
    }

    public double waterSurface() {
        return this.effects.values().stream().anyMatch(effect -> effect.profile.waterDepth() == 15 && effect.ticks >= effect.profile.ticks() && effect.cleanupTicks < 0) ? 80.0 : 0.0;
    }

    public double combatRadius() {
        return this.effects.values().stream().filter(effect -> effect.cleanupTicks < 0 && effect.profile.flatRoom()).mapToDouble(effect -> effect.profile.roomRadius() + 1).max().orElse(30.0);
    }

    public int changedBlocks() {
        return this.terrain.size();
    }

    public int activeBosses() {
        return this.effects.size();
    }

    public boolean cleaning() {
        return this.effects.values().stream().anyMatch(effect -> effect.cleanupTicks >= 0);
    }

    public boolean ownsCrystal(Entity entity) {
        EndCrystal crystal;
        if (!(entity instanceof EndCrystal) || !AbyssPlatform.data((Entity)(crystal = (EndCrystal)entity)).m_128403_(CRYSTAL_OWNER)) {
            return false;
        }
        Effect effect = this.effects.get(AbyssPlatform.data((Entity)crystal).m_128342_(CRYSTAL_OWNER));
        return effect != null && effect.cleanupTicks < 0 && effect.crystals.stream().anyMatch(owned -> owned.m_20148_().equals(crystal.m_20148_()));
    }

    public static boolean crystalFor(EndCrystal crystal, EnderDragon dragon) {
        CompoundTag data = AbyssPlatform.data((Entity)crystal);
        return !crystal.m_213877_() && data.m_128403_(CRYSTAL_OWNER) && data.m_128342_(CRYSTAL_OWNER).equals(dragon.m_20148_()) && data.m_128403_(CRYSTAL_RUN) && AbyssPlatform.data((Entity)dragon).m_128403_("spiral_abyss_run") && data.m_128342_(CRYSTAL_RUN).equals(AbyssPlatform.data((Entity)dragon).m_128342_("spiral_abyss_run"));
    }

    public void begin(Collection<? extends LivingEntity> mobs) {
        if (this.cleaning()) {
            throw new IllegalStateException("Previous boss terrain is still retracting");
        }
        BlockPos center = BlockPos.m_274446_((Position)this.bounds.center());
        for (LivingEntity livingEntity : mobs) {
            BossEnvironmentCatalog.Profile profile = BossEnvironmentCatalog.get(AbyssRegistries.ENTITY_TYPES.getKey(livingEntity.m_6095_()));
            if (profile == null || this.effects.containsKey(livingEntity.m_20148_())) continue;
            ArrayList<BossTerrainPlan.Placement> plan = new ArrayList<BossTerrainPlan.Placement>(BossTerrainPlan.patches(profile, center, this.level.f_46441_));
            if (profile.flatRoom()) {
                for (BlockState blockState : profile.palette()) {
                    if (blockState.m_60838_((BlockGetter)this.level, center)) continue;
                    throw new IllegalArgumentException("flat_room requires full collision blocks: " + String.valueOf(blockState));
                }
                if (plan.stream().anyMatch(placement -> this.level.m_7702_(placement.pos()) != null)) {
                    throw new IllegalStateException("flat_room cannot replace a block entity");
                }
            } else {
                plan.removeIf(placement -> {
                    BlockPos base = new BlockPos(placement.pos().m_123341_(), center.m_123342_(), placement.pos().m_123343_());
                    return !this.level.m_46859_(base) && !this.terrain.contains(base);
                });
            }
            List<Column> columns = this.columns(profile, center, livingEntity instanceof EnderDragon);
            plan.removeIf(placement -> columns.stream().anyMatch(column -> BossEnvironments.pillarCell(column, placement.pos().m_123341_() - column.base().m_123341_(), placement.pos().m_123343_() - column.base().m_123343_())));
            for (Column column : columns) {
                for (int y = 0; y < column.height(); ++y) {
                    for (int x = -2; x <= 2; ++x) {
                        for (int z = -2; z <= 2; ++z) {
                            if (!BossEnvironments.pillarCell(column, x, z)) continue;
                            plan.add(new BossTerrainPlan.Placement(column.base().m_7918_(x, y, z), column.state(), 2));
                        }
                    }
                }
            }
            plan.sort(Comparator.comparingDouble(placement -> BossEnvironments.radialOrder(placement.pos(), columns, center)).thenComparingInt(placement -> placement.pos().m_123342_()));
            Effect effect = new Effect(livingEntity, profile, List.copyOf(plan), columns);
            if (profile.waterDepth() > 0) {
                effect.water = new BossWater(this.level, center, profile.waterDepth());
            }
            this.effects.put(livingEntity.m_20148_(), effect);
        }
    }

    private List<Column> columns(BossEnvironmentCatalog.Profile profile, BlockPos center, boolean dragonShape) {
        BossEnvironmentCatalog.Pillars spec = profile.pillars();
        if (spec == null && dragonShape && !profile.flatRoom()) {
            spec = new BossEnvironmentCatalog.Pillars(Blocks.f_50080_.m_49966_(), 4, 5, 18, 26, true);
        }
        if (spec == null) {
            return List.of();
        }
        int count = spec.minimum() + this.level.f_46441_.m_188503_(spec.maximum() - spec.minimum() + 1);
        double rotation = this.level.f_46441_.m_188500_() * Math.PI * 2.0;
        ArrayList<Column> result = new ArrayList<Column>();
        for (int index = 0; index < count; ++index) {
            Column chosen = null;
            for (int attempt = 0; attempt < 80 && chosen == null; ++attempt) {
                double angle = rotation + (double)index * Math.PI * 2.0 / (double)count + (this.level.f_46441_.m_188500_() - 0.5) * 0.8;
                double radius = (double)Math.min(21, profile.radius() - 7) + this.level.f_46441_.m_188500_() * 5.0;
                BlockPos base = center.m_7918_((int)Math.round(Math.cos(angle) * radius), 0, (int)Math.round(Math.sin(angle) * radius));
                int height = spec.minHeight() + this.level.f_46441_.m_188503_(spec.maxHeight() - spec.minHeight() + 1);
                if (result.stream().anyMatch(other -> other.base().m_123331_((Vec3i)base) < 36.0) || !this.clearColumn(base, height, center, dragonShape)) continue;
                chosen = new Column(base, height, spec.block(), spec.crystals(), dragonShape);
            }
            if (chosen == null) {
                throw new IllegalStateException("No safe position for boss environment pillar " + (index + 1));
            }
            result.add(chosen);
        }
        return List.copyOf(result);
    }

    private static double radialOrder(BlockPos pos, List<Column> columns, BlockPos center) {
        for (Column column : columns) {
            if (!BossEnvironments.pillarCell(column, pos.m_123341_() - column.base().m_123341_(), pos.m_123343_() - column.base().m_123343_())) continue;
            return BossTerrainPlan.distance(column.base(), center);
        }
        return BossTerrainPlan.distance(pos, center);
    }

    private static boolean pillarCell(Column column, int x, int z) {
        return column.dragonShape() ? BossEnvironments.dragonPillarCell(x, z) : Math.abs(x) <= 1 && Math.abs(z) <= 1;
    }

    static boolean dragonPillarCell(int x, int z) {
        return Math.abs(x) <= 2 && Math.abs(z) <= 2 && (Math.abs(x) != 2 || Math.abs(z) != 2);
    }

    private boolean clearColumn(BlockPos base, int height, BlockPos center, boolean dragonShape) {
        for (int x = -2; x <= 2; ++x) {
            for (int z = -2; z <= 2; ++z) {
                if (!dragonShape ? Math.abs(x) > 1 || Math.abs(z) > 1 : !BossEnvironments.dragonPillarCell(x, z)) continue;
                if (BossTerrainPlan.reserved(base.m_123341_() + x - center.m_123341_(), base.m_123343_() + z - center.m_123343_()) || this.level.m_8055_(base.m_7918_(x, -1, z)).m_60795_()) {
                    return false;
                }
                for (int y = 0; y <= height + 1; ++y) {
                    BlockPos pos = base.m_7918_(x, y, z);
                    if ((this.level.m_46859_(pos) || this.terrain.contains(pos)) && this.level.m_7702_(pos) == null) continue;
                    return false;
                }
            }
        }
        return true;
    }

    public void tick(Predicate<LivingEntity> retain) {
        List<BlockPos> repairs = List.copyOf(this.damagedFloor);
        this.damagedFloor.clear();
        for (BlockPos pos : repairs) {
            if (!this.level.m_46805_(pos)) continue;
            this.terrain.repair(pos);
        }
        for (Effect effect : List.copyOf(this.effects.values())) {
            int target;
            LivingEntity boss = effect.boss;
            if (effect.cleanupTicks < 0 && !retain.test(boss) && (DragonArena.finished(boss, this.level) || boss.m_9236_() != this.level || this.level.m_8791_(boss.m_20148_()) != boss)) {
                this.retract(effect);
            }
            if (effect.cleanupTicks >= 0) {
                ++effect.cleanupTicks;
                target = (int)Math.ceil((double)(effect.retract.size() * Math.min(effect.cleanupTicks, 100)) / 100.0);
                while (effect.cleanupCursor < target) {
                    BlockPos pos = effect.retract.get(effect.cleanupCursor++);
                    this.particles(pos, this.level.m_8055_(pos));
                    this.removePlacement(effect, pos);
                }
                if (effect.water != null) {
                    effect.water.retract(effect.cleanupTicks, 100);
                }
                if (effect.cleanupTicks < 100) continue;
                this.terrain.remove(boss.m_20148_());
                this.effects.remove(boss.m_20148_());
                continue;
            }
            effect.entities.tick(this.level, this.bounds, effect.profile.waterDepth() == 15);
            if (effect.ticks >= effect.profile.ticks()) continue;
            ++effect.ticks;
            target = (int)Math.ceil((double)(effect.plan.size() * effect.ticks) / (double)effect.profile.ticks());
            while (effect.cursor < target) {
                this.place(effect, effect.plan.get(effect.cursor++));
            }
            if (effect.water != null) {
                effect.water.grow(effect.ticks, effect.profile.ticks());
            }
            if (effect.ticks != effect.profile.ticks()) continue;
            this.finishPlacement(effect);
            for (Column column : effect.columns) {
                this.crystal(effect, column);
            }
        }
    }

    private void finishPlacement(Effect effect) {
        boolean halfWater;
        this.placeBlocks(effect);
        boolean bl = halfWater = effect.profile.waterDepth() == 15;
        if (effect.profile.centered() || halfWater) {
            Vec3 destination = effect.profile.flatRoom() ? this.bounds.center() : BossPlacedEntities.surface(this.level, this.bounds.center(), halfWater);
            LivingEntity boss = effect.boss;
            destination = destination.m_82520_(0.0, MobCatalog.bossOptions(EntityType.m_20613_((EntityType)boss.m_6095_())).spawnYOffset(), 0.0);
            AABB moved = boss.m_20191_().m_82383_(destination.m_82546_(boss.m_20182_()));
            if (!this.level.m_45756_((Entity)boss, moved)) {
                throw new IllegalStateException("Boss environment center is obstructed");
            }
            boss.m_6021_(destination.f_82479_, destination.f_82480_, destination.f_82481_);
            if (boss instanceof Mob) {
                Mob ai = (Mob)boss;
                ai.m_21573_().m_26573_();
            }
            boss.m_20256_(Vec3.f_82478_);
            boss.f_19789_ = 0.0f;
        }
        if (halfWater) {
            for (ServerPlayer player : this.level.m_6907_()) {
                if (Math.abs(player.m_20185_() - this.bounds.center().f_82479_) > 30.0 || Math.abs(player.m_20189_() - this.bounds.center().f_82481_) > 30.0 || player.m_20186_() >= 80.0) continue;
                Vec3 destination = new Vec3(player.m_20185_(), 80.0, player.m_20189_());
                if (!this.level.m_45756_((Entity)player, player.m_20191_().m_82383_(destination.m_82546_(player.m_20182_())))) {
                    destination = BossPlacedEntities.surface(this.level, this.bounds.entrance(), true);
                }
                if (!this.level.m_45756_((Entity)player, player.m_20191_().m_82383_(destination.m_82546_(player.m_20182_())))) continue;
                player.m_8127_();
                player.f_8906_.m_9774_(destination.f_82479_, destination.f_82480_, destination.f_82481_, player.m_146908_(), player.m_146909_());
                player.m_20256_(Vec3.f_82478_);
                player.f_19789_ = 0.0f;
            }
        }
        effect.entities.spawn(this.level, this.bounds, this.host, effect.boss, effect.profile);
        QliphothEncounters.ready(effect.boss);
    }

    private void placeBlocks(Effect effect) {
        HashSet<BlockPos> used = new HashSet<BlockPos>();
        for (EnvironmentPlacements.Entry entry : effect.profile.blocks()) {
            BlockState state;
            try {
                state = BossEnvironmentCatalog.block((JsonElement)new JsonPrimitive(entry.id().toString()));
            }
            catch (JsonParseException ex) {
                if (entry.offset() != null) {
                    throw new IllegalStateException("Required environment block unavailable: " + String.valueOf(entry.id()), ex);
                }
                LogUtils.getLogger().warn("Skipping environment block {}: {}", (Object)entry.id(), (Object)ex.getMessage());
                continue;
            }
            Vec3 anchor = entry.anchor(this.bounds.center(), 0.5);
            int placed = 0;
            for (int attempt = 0; attempt < (entry.offset() == null ? 256 : entry.count()) && placed < entry.count(); ++attempt) {
                BlockPos pos;
                Vec3 point = BossPlacedEntities.candidate(anchor, attempt, 1.0);
                if (Math.abs(point.f_82479_ - this.bounds.center().f_82479_) > (effect.profile.flatRoom() ? (double)effect.profile.roomRadius() : 30.0) - 0.5 || Math.abs(point.f_82481_ - this.bounds.center().f_82481_) > (effect.profile.flatRoom() ? (double)effect.profile.roomRadius() : 30.0) - 0.5) continue;
                if (entry.offset() == null) {
                    point = BossPlacedEntities.surface(this.level, point, effect.profile.waterDepth() == 15);
                }
                if (used.contains(pos = BlockPos.m_274561_((double)point.f_82479_, (double)Math.ceil(point.f_82480_), (double)point.f_82481_)) || Math.abs((double)pos.m_123341_() + 0.5 - this.bounds.center().f_82479_) > (effect.profile.flatRoom() ? (double)effect.profile.roomRadius() : 30.0) - 0.5) continue;
                if (Math.abs((double)pos.m_123343_() + 0.5 - this.bounds.center().f_82481_) > (effect.profile.flatRoom() ? (double)effect.profile.roomRadius() : 30.0) - 0.5 || (double)pos.m_123342_() >= 118.0 || !this.level.m_46859_(pos) && (effect.water == null || !effect.water.contains(pos)) || !state.m_60710_((LevelReader)this.level, pos) || !this.makeRoom(new BossTerrainPlan.Placement(pos, state, 0))) continue;
                this.terrain.put(effect.boss.m_20148_(), pos, state, 3);
                effect.placed.add(pos);
                used.add(pos);
                ++placed;
            }
            if (placed != entry.count() && entry.offset() != null) {
                throw new IllegalStateException("Required exact block placement failed: " + String.valueOf(entry.id()));
            }
            if (placed == entry.count()) continue;
            LogUtils.getLogger().warn("Placed {}/{} environment blocks for {}", new Object[]{placed, entry.count(), entry.id()});
        }
    }

    private void place(Effect effect, BossTerrainPlan.Placement placement) {
        BlockPos pos = placement.pos();
        if (effect.profile.flatRoom()) {
            if (this.level.m_7702_(pos) != null) {
                throw new IllegalStateException("flat_room cannot replace a block entity");
            }
            if (!placement.state().m_60795_() && !this.makeRoom(placement, (double)pos.m_123342_() == 64.0)) {
                throw new IllegalStateException("flat_room is obstructed at " + String.valueOf(pos));
            }
            if (placement.state().m_60795_() && this.level.m_46859_(pos) && !this.terrain.contains(pos)) {
                return;
            }
            this.terrain.put(effect.boss.m_20148_(), pos, placement.state(), placement.priority());
            effect.placed.add(pos);
            if ((effect.cursor & 7) == 0 && !placement.state().m_60795_()) {
                this.particles(pos, placement.state());
            }
            return;
        }
        if ((double)pos.m_123342_() < 65.0 || this.level.m_7702_(pos) != null || !this.level.m_46859_(pos) && !this.terrain.contains(pos) && (effect.water == null || !effect.water.contains(pos))) {
            return;
        }
        if (this.level.m_8055_(pos.m_7495_()).m_60795_() || !placement.state().m_60710_((LevelReader)this.level, pos)) {
            return;
        }
        if (placement.state().m_60734_() instanceof DoublePlantBlock) {
            this.placePlant(effect, placement);
            return;
        }
        if (!this.makeRoom(placement)) {
            return;
        }
        this.terrain.put(effect.boss.m_20148_(), pos, placement.state(), placement.priority());
        effect.placed.add(pos);
        if ((effect.cursor & 7) == 0) {
            this.particles(pos, placement.state());
        }
    }

    private void placePlant(Effect effect, BossTerrainPlan.Placement placement) {
        BlockPos lower = placement.pos();
        BlockPos upper = lower.m_7494_();
        if (!this.level.m_46859_(lower) || !this.level.m_46859_(upper) || this.level.m_7702_(upper) != null || (double)upper.m_123342_() >= 120.0 || !this.level.m_6443_(LivingEntity.class, new AABB(lower).m_82363_(0.0, 1.0, 0.0), LivingEntity::m_6084_).isEmpty()) {
            return;
        }
        BlockState lowerState = (BlockState)placement.state().m_61124_((Property)DoublePlantBlock.f_52858_, (Comparable)DoubleBlockHalf.LOWER);
        BlockState upperState = (BlockState)lowerState.m_61124_((Property)DoublePlantBlock.f_52858_, (Comparable)DoubleBlockHalf.UPPER);
        LinkedHashMap<BlockPos, BlockState> pair = new LinkedHashMap<BlockPos, BlockState>();
        pair.put(lower, lowerState);
        pair.put(upper, upperState);
        effect.placed.addAll(pair.keySet());
        effect.plantBases.put(lower, lower);
        effect.plantBases.put(upper, lower);
        this.terrain.putAll(effect.boss.m_20148_(), pair, placement.priority());
        if ((effect.cursor & 7) == 0) {
            this.particles(lower, lowerState);
        }
    }

    private void removePlacement(Effect effect, BlockPos pos) {
        BlockPos lower = effect.plantBases.get(pos);
        if (lower != null) {
            this.terrain.remove(effect.boss.m_20148_(), lower.m_7494_());
            this.terrain.remove(effect.boss.m_20148_(), lower);
        } else {
            this.terrain.remove(effect.boss.m_20148_(), pos);
        }
        if (effect.water != null) {
            effect.water.restore(pos);
        }
    }

    private boolean makeRoom(BossTerrainPlan.Placement placement) {
        return this.makeRoom(placement, placement.priority() == 0);
    }

    private boolean makeRoom(BossTerrainPlan.Placement placement, boolean liftActors) {
        BlockPos pos = placement.pos();
        List actors = this.level.m_6443_(LivingEntity.class, new AABB(pos), LivingEntity::m_6084_);
        if (actors.isEmpty()) {
            return true;
        }
        if (placement.state().m_60713_(Blocks.f_50125_) && placement.state().m_60812_((BlockGetter)this.level, pos).m_83281_()) {
            return true;
        }
        if (!liftActors) {
            return false;
        }
        VoxelShape shape = placement.state().m_60812_((BlockGetter)this.level, pos);
        if (shape.m_83281_()) {
            return true;
        }
        double top = (double)pos.m_123342_() + shape.m_83297_(Direction.Axis.Y);
        LinkedHashMap<LivingEntity, Double> lifts = new LinkedHashMap<LivingEntity, Double>();
        for (LivingEntity actor2 : actors) {
            double lift2 = top - actor2.m_20191_().f_82289_;
            if (lift2 <= 0.0) continue;
            if (lift2 > 1.1251 || actor2.m_20159_() || actor2.m_20160_()) {
                return false;
            }
            AABB moved = actor2.m_20191_().m_82386_(0.0, lift2, 0.0);
            if (moved.f_82292_ > 120.0 || !this.level.m_45756_((Entity)actor2, moved)) {
                return false;
            }
            if (actor2 instanceof ServerPlayer && (moved.f_82288_ < this.bounds.center().f_82479_ - 30.0 || moved.f_82291_ > this.bounds.center().f_82479_ + 30.0 || moved.f_82290_ < this.bounds.center().f_82481_ - 30.0 || moved.f_82293_ > this.bounds.center().f_82481_ + 30.0)) {
                return false;
            }
            lifts.put(actor2, lift2);
        }
        lifts.forEach((actor, lift) -> {
            if (actor instanceof ServerPlayer) {
                ServerPlayer player = (ServerPlayer)actor;
                player.f_8906_.m_9774_(player.m_20185_(), player.m_20186_() + lift, player.m_20189_(), player.m_146908_(), player.m_146909_());
            } else {
                actor.m_6034_(actor.m_20185_(), actor.m_20186_() + lift, actor.m_20189_());
            }
            actor.f_19789_ = 0.0f;
        });
        return true;
    }

    private void crystal(Effect effect, Column column) {
        if (!column.crystals()) {
            return;
        }
        for (int y = 0; y < column.height(); ++y) {
            for (int x = -2; x <= 2; ++x) {
                for (int z = -2; z <= 2; ++z) {
                    if (!BossEnvironments.pillarCell(column, x, z) || effect.placed.contains(column.base().m_7918_(x, y, z))) continue;
                    return;
                }
            }
        }
        BlockPos top = column.base().m_6630_(column.height());
        if (!this.level.m_6443_(LivingEntity.class, new AABB(top).m_82400_(1.0), LivingEntity::m_6084_).isEmpty()) {
            return;
        }
        EndCrystal crystal = (EndCrystal)EntityType.f_20564_.m_20615_((Level)this.level);
        if (crystal == null) {
            throw new IllegalStateException("Unable to create arena healing crystal");
        }
        crystal.m_7678_((double)top.m_123341_() + 0.5, (double)top.m_123342_(), (double)top.m_123343_() + 0.5, 0.0f, 0.0f);
        crystal.m_31056_(false);
        AbyssPlatform.data((Entity)crystal).m_128362_(CRYSTAL_OWNER, effect.boss.m_20148_());
        AbyssPlatform.data((Entity)crystal).m_128362_(CRYSTAL_RUN, AbyssPlatform.data((Entity)effect.boss).m_128342_("spiral_abyss_run"));
        AbyssPlatform.data((Entity)crystal).m_128362_("spiral_abyss_host", this.host);
        effect.crystals.add(crystal);
        if (!this.level.m_7967_((Entity)crystal)) {
            throw new IllegalStateException("Arena crystal spawn rejected");
        }
    }

    private void particles(BlockPos pos, BlockState state) {
        if (!state.m_60795_()) {
            this.level.m_8767_((ParticleOptions)new BlockParticleOption(ParticleTypes.f_123794_, state), (double)pos.m_123341_() + 0.5, (double)pos.m_123342_() + 0.5, (double)pos.m_123343_() + 0.5, 2, 0.3, 0.2, 0.3, 0.03);
        }
    }

    private void discardCrystals(Effect effect) {
        effect.crystals.forEach(crystal -> {
            Entity loaded = this.level.m_8791_(crystal.m_20148_());
            if (loaded instanceof EndCrystal && AbyssPlatform.data(loaded).m_128403_(CRYSTAL_OWNER) && effect.boss.m_20148_().equals(AbyssPlatform.data(loaded).m_128342_(CRYSTAL_OWNER))) {
                loaded.m_146870_();
            }
            crystal.m_146870_();
        });
        effect.crystals.clear();
    }

    private void retract(Effect effect) {
        this.discardCrystals(effect);
        QliphothEncounters.clear(effect.boss);
        effect.entities.clear();
        effect.cleanupTicks = 0;
        BlockPos center = BlockPos.m_274446_((Position)this.bounds.center());
        effect.retract = effect.placed.stream().sorted(Comparator.comparingDouble(pos -> BossEnvironments.radialOrder(pos, effect.columns, center)).thenComparingInt(Vec3i::m_123342_).reversed()).toList();
    }

    public void remove(UUID owner) {
        Effect effect = this.effects.remove(owner);
        if (effect == null) {
            return;
        }
        this.discardCrystals(effect);
        QliphothEncounters.clear(effect.boss);
        effect.entities.clear();
        effect.placed.stream().sorted(Comparator.comparingInt(pos -> pos.m_123342_()).reversed()).forEach(pos -> this.removePlacement(effect, (BlockPos)pos));
        this.terrain.remove(owner);
        if (effect.water != null) {
            effect.water.clear();
        }
    }

    public void clear() {
        this.damagedFloor.clear();
        for (UUID owner : List.copyOf(this.effects.keySet())) {
            this.remove(owner);
        }
        this.terrain.clear();
    }

    private static final class Effect {
        final LivingEntity boss;
        final BossEnvironmentCatalog.Profile profile;
        final List<BossTerrainPlan.Placement> plan;
        final List<Column> columns;
        final Set<BlockPos> placed = new LinkedHashSet<BlockPos>();
        final Map<BlockPos, BlockPos> plantBases = new HashMap<BlockPos, BlockPos>();
        final List<EndCrystal> crystals = new ArrayList<EndCrystal>();
        final BossPlacedEntities entities = new BossPlacedEntities();
        BossWater water;
        List<BlockPos> retract = List.of();
        int cursor;
        int ticks;
        int cleanupTicks = -1;
        int cleanupCursor;

        Effect(LivingEntity boss, BossEnvironmentCatalog.Profile profile, List<BossTerrainPlan.Placement> plan, List<Column> columns) {
            this.boss = boss;
            this.profile = profile;
            this.plan = plan;
            this.columns = columns;
        }
    }

    private record Column(BlockPos base, int height, BlockState state, boolean crystals, boolean dragonShape) {
    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction$Axis
 *  net.minecraft.core.Position
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.TicketType
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.entity.MobSpawnType
 *  net.minecraft.world.entity.SpawnGroupData
 *  net.minecraft.world.entity.item.ItemEntity
 *  net.minecraft.world.entity.monster.Zombie
 *  net.minecraft.world.entity.monster.Zombie$ZombieGroupData
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.ChunkPos
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.ServerLevelAccessor
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 *  net.minecraft.world.phys.shapes.VoxelShape
 */
package com.pla.spiral_abyss.boss_enviroment;

import com.mojang.logging.LogUtils;
import com.pla.spiral_abyss.boss_enviroment.BossEnvironmentCatalog;
import com.pla.spiral_abyss.boss_enviroment.EnvironmentPlacements;
import com.pla.spiral_abyss.encounter.MobSpawnData;
import com.pla.spiral_abyss.encounter.QliphothEncounters;
import com.pla.spiral_abyss.encounter.WaveAllies;
import com.pla.spiral_abyss.encounter.data.MobCatalog;
import com.pla.spiral_abyss.entity.ArenaInteractable;
import com.pla.spiral_abyss.platform.AbyssPlatform;
import com.pla.spiral_abyss.platform.AbyssRegistries;
import com.pla.spiral_abyss.world.ArenaBounds;
import com.pla.spiral_abyss.world.ArenaFlight;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

final class BossPlacedEntities {
    static final String OWNER = "spiral_abyss_environment_owner";
    private static final TicketType<UUID> TICKET = TicketType.m_9465_((String)"spiral_abyss_environment", UUID::compareTo, (int)40);
    private final List<Entity> entities = new ArrayList<Entity>();
    private final Map<UUID, ChunkPos> tickets = new HashMap<UUID, ChunkPos>();
    private ServerLevel ticketLevel;

    BossPlacedEntities() {
    }

    boolean owns(Entity entity) {
        return this.entities.stream().anyMatch(owned -> owned == entity && !owned.m_213877_());
    }

    private void keepActive(ServerLevel level, Entity entity) {
        this.ticketLevel = level;
        ChunkPos next = entity.m_146902_();
        ChunkPos previous = this.tickets.put(entity.m_20148_(), next);
        if (previous != null && !previous.equals((Object)next)) {
            level.m_7726_().m_8438_(TICKET, previous, 2, (Object)entity.m_20148_());
        }
        level.m_7726_().m_8387_(TICKET, next, 2, (Object)entity.m_20148_());
    }

    void tick(ServerLevel level, ArenaBounds bounds, boolean halfWater) {
        Iterator<Entity> iterator = this.entities.iterator();
        while (iterator.hasNext()) {
            Mob mob;
            Entity entity = iterator.next();
            if (entity.m_9236_() != level) {
                entity.m_146870_();
            }
            if (!entity.m_213877_()) {
                boolean outside;
                boolean bl = outside = Math.abs(entity.m_20185_() - bounds.center().f_82479_) > 128.0 || Math.abs(entity.m_20189_() - bounds.center().f_82481_) > 128.0 || entity.m_20186_() < 0.0 || entity.m_20186_() > 240.0;
                if (entity instanceof Mob) {
                    mob = (Mob)entity;
                    ArenaFlight.steer(mob, bounds);
                    boolean ghost = MobCatalog.isGhostMob(EntityType.m_20613_((EntityType)mob.m_6095_()));
                    if (bounds.rescueMob((Entity)mob, ghost)) {
                        Vec3 point = BossPlacedEntities.surface(level, bounds.center(), halfWater);
                        mob.m_6021_(point.f_82479_, point.f_82480_, point.f_82481_);
                        ArenaFlight.stop(mob);
                    }
                } else if (outside) {
                    entity.m_146870_();
                }
            }
            if (entity.m_213877_()) {
                ChunkPos chunk = this.tickets.remove(entity.m_20148_());
                if (chunk != null) {
                    level.m_7726_().m_8438_(TICKET, chunk, 2, (Object)entity.m_20148_());
                }
                if (entity instanceof Mob) {
                    mob = (Mob)entity;
                    WaveAllies.remove((LivingEntity)mob);
                }
                iterator.remove();
                continue;
            }
            this.keepActive(level, entity);
        }
    }

    static Vec3 surface(ServerLevel level, Vec3 point, boolean halfWater) {
        for (int y = 119; y >= 64; --y) {
            BlockPos pos = BlockPos.m_274561_((double)point.f_82479_, (double)y, (double)point.f_82481_);
            VoxelShape shape = level.m_8055_(pos).m_60812_((BlockGetter)level, pos);
            if (shape.m_83281_()) continue;
            return new Vec3(point.f_82479_, Math.max(halfWater ? 80.0 : 65.0, (double)y + shape.m_83297_(Direction.Axis.Y)), point.f_82481_);
        }
        return new Vec3(point.f_82479_, 65.0 + (double)(halfWater ? 15 : 0), point.f_82481_);
    }

    static Vec3 candidate(Vec3 anchor, int index, double spacing) {
        if (index == 0) {
            return anchor;
        }
        int ring = (int)Math.ceil((Math.sqrt(index + 1) - 1.0) / 2.0);
        int start = (2 * ring - 1) * (2 * ring - 1);
        int offset = index - start;
        int edge = 2 * ring;
        int side = offset / edge;
        int step = offset % edge;
        double d = (switch (side) {
            case 0 -> -ring + step;
            case 1 -> ring;
            case 2 -> ring - step;
            default -> -ring;
        }) * spacing;
        return anchor.m_82520_(d, 0.0, (switch (side) {
            case 0 -> -ring;
            case 1 -> -ring + step;
            case 2 -> ring;
            default -> ring - step;
        }) * spacing);
    }

    static boolean fits(Entity entity, ArenaBounds bounds) {
        AABB box = entity.m_20191_();
        Vec3 center = bounds.center();
        return box.f_82288_ >= center.f_82479_ - bounds.combatRadius(entity) && box.f_82291_ <= center.f_82479_ + bounds.combatRadius(entity) && box.f_82290_ >= center.f_82481_ - bounds.combatRadius(entity) && box.f_82293_ <= center.f_82481_ + bounds.combatRadius(entity) && box.f_82289_ >= 65.0 && box.f_82292_ <= 240.0;
    }

    void spawn(ServerLevel level, ArenaBounds bounds, UUID host, LivingEntity boss, BossEnvironmentCatalog.Profile profile) {
        block0: for (EnvironmentPlacements.Entry entry : profile.entities()) {
            EntityType<?> type = AbyssRegistries.ENTITY_TYPES.getValue(entry.id());
            if (!AbyssRegistries.ENTITY_TYPES.containsKey(entry.id()) || type == null) {
                if (entry.offset() != null) {
                    throw new IllegalStateException("Required environment entity unavailable: " + String.valueOf(entry.id()));
                }
                LogUtils.getLogger().warn("Skipping unavailable environment entity {}", (Object)entry.id());
                continue;
            }
            for (int count = 0; count < entry.count(); ++count) {
                CompoundTag overrides;
                Entity entity = type.m_20615_((Level)level);
                if (entity == null && entry.offset() != null) {
                    throw new IllegalStateException("Cannot create required environment entity " + String.valueOf(entry.id()));
                }
                if (entity == null) {
                    LogUtils.getLogger().warn("Cannot create environment entity {}", (Object)entry.id());
                    continue block0;
                }
                this.entities.add(entity);
                if (entity instanceof Player || entity instanceof ArenaInteractable || entity instanceof ItemEntity) {
                    entity.m_146870_();
                    LogUtils.getLogger().warn("Unsupported environment entity {}", (Object)entry.id());
                    continue block0;
                }
                AbyssPlatform.data(entity).m_128362_(OWNER, boss.m_20148_());
                AbyssPlatform.data(entity).m_128362_("spiral_abyss_run", AbyssPlatform.data((Entity)boss).m_128342_("spiral_abyss_run"));
                AbyssPlatform.data(entity).m_128362_("spiral_abyss_host", host);
                Vec3 anchor = entry.anchor(bounds.center(), entity.m_20205_() / 2.0f);
                entity.m_146884_(anchor);
                if (entity instanceof Mob) {
                    Mob mob = (Mob)entity;
                    Zombie.ZombieGroupData group = mob instanceof Zombie ? new Zombie.ZombieGroupData(false, false) : null;
                    mob.m_6518_((ServerLevelAccessor)level, level.m_6436_(BlockPos.m_274446_((Position)anchor)), MobSpawnType.COMMAND, (SpawnGroupData)group, null);
                    new ArrayList<Entity>(mob.m_20197_()).forEach(Entity::m_146870_);
                    mob.m_8127_();
                    mob.m_21530_();
                    if (boss instanceof Mob) {
                        Mob ai = (Mob)boss;
                        mob.m_6710_(ai.m_5448_());
                    }
                    anchor = entry.anchor(bounds.center(), mob.m_20205_() / 2.0f);
                }
                if (!(overrides = MobSpawnData.parse(entry.spawnNbt())).m_128456_()) {
                    CompoundTag saved = entity.m_20240_(new CompoundTag());
                    for (String key : overrides.m_128431_()) {
                        Tag value = overrides.m_128423_(key);
                        if (value == null || !saved.m_128425_(key, (int)value.m_7060_())) {
                            throw new IllegalArgumentException(String.valueOf(entry.id()) + " has no matching native save field " + key);
                        }
                        saved.m_128365_(key, value.m_6426_());
                    }
                    entity.m_20258_(saved);
                }
                boolean placed = false;
                for (int attempt = 0; attempt < (entry.offset() == null ? 256 : 1); ++attempt) {
                    Mob mob;
                    Vec3 point = BossPlacedEntities.candidate(anchor, attempt, Math.max(1.25, (double)entity.m_20205_() + 0.5));
                    double inset = bounds.combatRadius(entity) - (double)(entity.m_20205_() / 2.0f);
                    if (Math.abs(point.f_82479_ - bounds.center().f_82479_) > inset || Math.abs(point.f_82481_ - bounds.center().f_82481_) > inset) continue;
                    if (entry.offset() == null) {
                        point = BossPlacedEntities.surface(level, point, profile.waterDepth() == 15);
                    }
                    entity.m_7678_(point.f_82479_, point.f_82480_, point.f_82481_, 0.0f, 0.0f);
                    if (!BossPlacedEntities.fits(entity, bounds) || !level.m_45786_(entity) || this.entities.stream().anyMatch(other -> other != entity && !other.m_213877_() && other.m_20191_().m_82381_(entity.m_20191_()))) continue;
                    Vec3 direction = boss.m_146892_().m_82546_(entity.m_146892_());
                    float yaw = (float)(Math.toDegrees(Math.atan2(direction.f_82481_, direction.f_82479_)) - 90.0);
                    float pitch = (float)(-Math.toDegrees(Math.atan2(direction.f_82480_, Math.hypot(direction.f_82479_, direction.f_82481_))));
                    entity.m_7678_(point.f_82479_, point.f_82480_, point.f_82481_, yaw, pitch);
                    if (entity instanceof LivingEntity) {
                        LivingEntity living = (LivingEntity)entity;
                        living.m_5616_(yaw);
                        living.m_5618_(yaw);
                    }
                    if (entity instanceof Mob) {
                        mob = (Mob)entity;
                        mob.m_21563_().m_148051_((Entity)boss);
                    }
                    this.keepActive(level, entity);
                    QliphothEncounters.own(boss, entity);
                    if (!level.m_7967_(entity)) {
                        throw new IllegalStateException("Environment entity spawn rejected: " + String.valueOf(entry.id()));
                    }
                    if (entity instanceof Mob) {
                        mob = (Mob)entity;
                        WaveAllies.add((LivingEntity)mob);
                    }
                    placed = true;
                    break;
                }
                if (!placed && entry.offset() != null) {
                    throw new IllegalStateException("Required exact helper placement failed: " + String.valueOf(entry.id()));
                }
                if (placed) continue;
                entity.m_146870_();
                LogUtils.getLogger().warn("No safe environment position for {}", (Object)entry.id());
            }
        }
    }

    void clear() {
        this.entities.forEach(entity -> {
            if (entity instanceof Mob) {
                Mob mob = (Mob)entity;
                WaveAllies.remove((LivingEntity)mob);
            }
            entity.m_146870_();
        });
        if (this.ticketLevel != null) {
            this.tickets.forEach((id, chunk) -> this.ticketLevel.m_7726_().m_8438_(TICKET, chunk, 2, id));
        }
        this.tickets.clear();
        this.entities.clear();
    }
}


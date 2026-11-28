/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Position
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.entity.MobSpawnType
 *  net.minecraft.world.entity.SpawnGroupData
 *  net.minecraft.world.entity.boss.enderdragon.EnderDragon
 *  net.minecraft.world.entity.monster.Slime
 *  net.minecraft.world.entity.monster.Zombie
 *  net.minecraft.world.entity.monster.Zombie$ZombieGroupData
 *  net.minecraft.world.entity.monster.piglin.AbstractPiglin
 *  net.minecraft.world.entity.monster.piglin.Piglin
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.ServerLevelAccessor
 *  net.minecraft.world.phys.Vec3
 */
package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.boss_enviroment.BossEnvironmentCatalog;
import com.pla.spiral_abyss.encounter.DragonArena;
import com.pla.spiral_abyss.encounter.MobAwakening;
import com.pla.spiral_abyss.encounter.MobSpawnData;
import com.pla.spiral_abyss.encounter.QliphothEncounters;
import com.pla.spiral_abyss.encounter.WaveAllies;
import com.pla.spiral_abyss.encounter.WaveEquipment;
import com.pla.spiral_abyss.encounter.WaveScaling;
import com.pla.spiral_abyss.encounter.data.FloorDefinition;
import com.pla.spiral_abyss.encounter.data.MobCatalog;
import com.pla.spiral_abyss.encounter.data.MobCategory;
import com.pla.spiral_abyss.platform.AbyssPlatform;
import com.pla.spiral_abyss.platform.AbyssRegistries;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

public final class WaveSpawner {
    public static final String RUN_TAG = "spiral_abyss_run";
    public static final String DAMAGE_TAG = "spiral_abyss_damage_scale";
    public static final String BOSS_TAG = "spiral_abyss_wave_boss";

    public List<LivingEntity> spawn(ServerLevel level, Vec3 center, ServerPlayer target, List<FloorDefinition.Enemy> composition, UUID runId, WaveEquipment equipment, int floor, double partyScale, MobCatalog.ScalingSnapshot scaling) {
        return this.spawn(level, center, target, composition, runId, equipment, floor, partyScale, scaling, 0);
    }

    /*
     * WARNING - void declaration
     */
    public List<LivingEntity> spawn(ServerLevel level, Vec3 center, ServerPlayer target, List<FloorDefinition.Enemy> composition, UUID runId, WaveEquipment equipment, int floor, double partyScale, MobCatalog.ScalingSnapshot scaling, int endlessWave) {
        for (FloorDefinition.Enemy enemy : composition) {
            if (MobCatalog.selectableMob(enemy.id())) continue;
            throw new IllegalArgumentException(String.valueOf(enemy.id()) + " is unavailable or blacklisted in mob_blacklist");
        }
        ArrayList<void> created = new ArrayList<void>();
        try {
            for (FloorDefinition.Enemy entry : composition) {
                EntityType<?> type = AbyssRegistries.ENTITY_TYPES.getValue(entry.id());
                for (int i = 0; i < entry.count(); ++i) {
                    AbstractPiglin piglin;
                    Mob ai;
                    Zombie.ZombieGroupData group;
                    void mob2;
                    Entity entity;
                    Entity entity2 = entity = type == null ? null : type.m_20615_((Level)level);
                    if (!(entity instanceof LivingEntity)) {
                        if (entity != null) {
                            entity.m_146870_();
                        }
                        throw new IllegalArgumentException(String.valueOf(entry.id()) + " is not a summonable living encounter");
                    }
                    LivingEntity mob22 = (LivingEntity)entity;
                    if (!(mob2 instanceof Mob) && !QliphothEncounters.supports(entry.id())) {
                        throw new IllegalArgumentException(String.valueOf(entry.id()) + " has no living encounter adapter");
                    }
                    created.add(mob2);
                    mob2.m_7678_(center.f_82479_, center.f_82480_, center.f_82481_, 0.0f, 0.0f);
                    Zombie.ZombieGroupData zombieGroupData = group = mob2 instanceof Zombie ? new Zombie.ZombieGroupData(false, false) : null;
                    if (mob2 instanceof Mob) {
                        Mob ai2 = (Mob)mob2;
                        ai2.m_6518_((ServerLevelAccessor)level, level.m_6436_(BlockPos.m_274446_((Position)center)), MobSpawnType.COMMAND, (SpawnGroupData)group, null);
                    }
                    MobSpawnData.apply((LivingEntity)mob2, MobSpawnData.parse(MobCatalog.bossOptions(entry.id()).spawnNbt()));
                    Map<String, Integer> initialState = MobCatalog.bossOptions(entry.id()).spawnIntegers();
                    if (!initialState.isEmpty()) {
                        CompoundTag saved = new CompoundTag();
                        mob2.m_7380_(saved);
                        initialState.forEach((key, value) -> {
                            if (!saved.m_128425_(key, 3)) {
                                throw new IllegalArgumentException(String.valueOf(entry.id()) + " has no saved integer " + key);
                            }
                            saved.m_128405_(key, value.intValue());
                        });
                        mob2.m_7378_(saved);
                    }
                    if (mob2 instanceof Mob) {
                        ai = (Mob)mob2;
                        MobAwakening.apply(ai);
                    }
                    if (mob2 instanceof Slime) {
                        Slime slime = (Slime)mob2;
                        if (entry.category() != null) {
                            if (entry.category() == MobCategory.FODDER_MOB) {
                                slime.m_7839_(1 + level.f_46441_.m_188503_(2), true);
                            } else if (entry.category() == MobCategory.COMMON_MOB) {
                                slime.m_7839_(4, true);
                            }
                        }
                    }
                    if (mob2 instanceof AbstractPiglin) {
                        piglin = (AbstractPiglin)mob2;
                        piglin.m_34670_(true);
                    }
                    if (mob2 instanceof Piglin) {
                        piglin = (Piglin)mob2;
                        piglin.m_6863_(false);
                    }
                    if (mob2 instanceof Mob) {
                        ai = (Mob)mob2;
                        equipment.apply(ai);
                    }
                    DragonArena.prepare((LivingEntity)mob2, center);
                    new ArrayList<Entity>(mob2.m_20197_()).forEach(Entity::m_146870_);
                    mob2.m_8127_();
                    if (mob2 instanceof Mob) {
                        ai = (Mob)mob2;
                        ai.m_21530_();
                    }
                    AbyssPlatform.data((Entity)mob2).m_128362_(RUN_TAG, runId);
                    AbyssPlatform.data((Entity)mob2).m_128379_(BOSS_TAG, entry.category() == MobCategory.BOSS);
                    if (MobCatalog.bossOptions(entry.id()).hideBossBar()) {
                        AbyssPlatform.data((Entity)mob2).m_128379_("spiral_abyss_hide_boss_bar", true);
                    }
                    if (endlessWave > 0) {
                        WaveScaling.applyEndless((LivingEntity)mob2, endlessWave, entry.category(), partyScale);
                    } else {
                        WaveScaling.apply((LivingEntity)mob2, floor, entry.category(), entry.id(), partyScale, scaling);
                    }
                    if (!this.place(level, (LivingEntity)mob2, center)) {
                        throw new IllegalStateException("No safe spawn position for " + String.valueOf(entry.id()));
                    }
                    if (mob2 instanceof Mob) {
                        ai = (Mob)mob2;
                        ai.m_6710_((LivingEntity)target);
                    }
                    QliphothEncounters.prepare((LivingEntity)mob2, center);
                    if (!level.m_7967_((Entity)mob2)) {
                        throw new IllegalStateException("Spawn rejected for " + String.valueOf(entry.id()));
                    }
                    WaveAllies.add((LivingEntity)mob2);
                }
            }
            return List.copyOf(created);
        }
        catch (RuntimeException runtimeException) {
            created.forEach(mob -> {
                QliphothEncounters.clear(mob);
                WaveAllies.remove(mob);
                mob.m_146870_();
            });
            throw runtimeException;
        }
    }

    public boolean place(ServerLevel level, LivingEntity mob, Vec3 center) {
        double spawnY = center.f_82480_ + MobCatalog.bossOptions(AbyssRegistries.ENTITY_TYPES.getKey(mob.m_6095_())).spawnYOffset();
        BossEnvironmentCatalog.Profile environment = BossEnvironmentCatalog.get(AbyssRegistries.ENTITY_TYPES.getKey(mob.m_6095_()));
        if (environment != null && environment.centered()) {
            level.m_46745_(BlockPos.m_274446_((Position)center));
            mob.m_7678_(center.f_82479_, spawnY, center.f_82481_, 0.0f, 0.0f);
            return level.m_6857_().m_61935_(mob.m_20191_()) && level.m_45786_((Entity)mob);
        }
        if (mob instanceof EnderDragon) {
            level.m_46745_(BlockPos.m_274446_((Position)center));
            mob.m_7678_(center.f_82479_, spawnY + 20.0, center.f_82481_, level.f_46441_.m_188501_() * 360.0f, 0.0f);
            return level.m_6857_().m_61935_(mob.m_20191_()) && level.m_45786_((Entity)mob);
        }
        for (int attempt = 0; attempt < 80; ++attempt) {
            double angle = level.f_46441_.m_188500_() * Math.PI * 2.0;
            double radius = 5.0 + level.f_46441_.m_188500_() * 12.0;
            double x = center.f_82479_ + Math.cos(angle) * radius;
            double z = center.f_82481_ + Math.sin(angle) * radius;
            BlockPos feet = BlockPos.m_274561_((double)x, (double)center.f_82480_, (double)z);
            level.m_46745_(feet);
            mob.m_7678_(x, spawnY, z, level.f_46441_.m_188501_() * 360.0f, 0.0f);
            if (!level.m_6857_().m_61935_(mob.m_20191_()) || !level.m_8055_(feet.m_7495_()).m_60804_((BlockGetter)level, feet.m_7495_()) || !level.m_45786_((Entity)mob) || level.m_46855_(mob.m_20191_())) continue;
            return true;
        }
        return false;
    }
}


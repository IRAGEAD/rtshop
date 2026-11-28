/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.server.level.TicketType
 *  net.minecraft.world.effect.MobEffect
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ChunkPos
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 */
package com.pla.spiral_abyss.encounter;

import com.mojang.logging.LogUtils;
import com.pla.spiral_abyss.boss_enviroment.BossEnvironmentCatalog;
import com.pla.spiral_abyss.platform.AbyssPlatform;
import com.pla.spiral_abyss.platform.AbyssRegistries;
import com.pla.spiral_abyss.world.AbyssDimension;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class QliphothEncounters {
    private static final TicketType<UUID> TICKET = TicketType.m_9465_((String)"spiral_abyss_puzzle", UUID::compareTo, (int)40);
    private static final String PLAYER_FIGHT = "spiral_abyss_qliphoth_fight";
    private static final String OWNER = "spiral_abyss_environment_owner";
    private static final Map<UUID, Fight> FIGHTS = new HashMap<UUID, Fight>();
    private static final ThreadLocal<Fight> CURRENT = new ThreadLocal();

    private QliphothEncounters() {
    }

    public static boolean supports(ResourceLocation id) {
        return id.m_135827_().equals("fdbosses") && Set.of("chesed", "malkuth", "geburah").contains(id.m_135815_());
    }

    public static boolean helper(Entity entity) {
        return entity != null && EntityType.m_20613_((EntityType)entity.m_6095_()).m_135827_().equals("fdbosses") && AbyssPlatform.data(entity).m_128403_(OWNER);
    }

    public static void prepare(LivingEntity boss, Vec3 center) {
        if (!QliphothEncounters.supports(EntityType.m_20613_((EntityType)boss.m_6095_()))) {
            return;
        }
        BossEnvironmentCatalog.Profile profile = BossEnvironmentCatalog.get(EntityType.m_20613_((EntityType)boss.m_6095_()));
        if (profile == null || !profile.flatRoom()) {
            throw new IllegalArgumentException("Qliphoth requires its boss room");
        }
        FIGHTS.put(boss.m_20148_(), new Fight(boss, center));
        QliphothEncounters.invoke(boss, "setSpawnPosition", boss.m_20182_());
    }

    public static void ready(LivingEntity boss) {
        Fight fight = FIGHTS.get(boss.m_20148_());
        if (fight == null) {
            return;
        }
        String spawnerId = EntityType.m_20613_((EntityType)boss.m_6095_()).m_135815_() + "_boss_spawner";
        Entity spawner = fight.helpers.stream().filter(entity -> EntityType.m_20613_((EntityType)entity.m_6095_()).m_135815_().equals(spawnerId)).findFirst().orElseThrow(() -> new IllegalStateException("Missing native boss spawner " + spawnerId));
        QliphothEncounters.invoke(spawner, "setActive", false);
        QliphothEncounters.invoke(boss, "setSpawnedBy", spawner);
        QliphothEncounters.invoke(boss, "setSpawnPosition", boss.m_20182_());
        boss.f_19797_ = 0;
        fight.ready = true;
    }

    public static void own(LivingEntity boss, Entity entity) {
        Fight fight = FIGHTS.get(boss.m_20148_());
        if (fight == null) {
            return;
        }
        CompoundTag data = AbyssPlatform.data(entity);
        data.m_128362_(OWNER, boss.m_20148_());
        data.m_128362_("spiral_abyss_run", AbyssPlatform.data((Entity)boss).m_128342_("spiral_abyss_run"));
        if (AbyssPlatform.data((Entity)boss).m_128403_("spiral_abyss_host")) {
            data.m_128362_("spiral_abyss_host", AbyssPlatform.data((Entity)boss).m_128342_("spiral_abyss_host"));
        }
        fight.helpers.add(entity);
    }

    public static void capture(Entity entity) {
        Fight fight = CURRENT.get();
        if (fight != null && entity.m_9236_() == fight.boss.m_9236_() && entity != fight.boss && EntityType.m_20613_((EntityType)entity.m_6095_()).m_135827_().equals("fdbosses")) {
            QliphothEncounters.own(fight.boss, entity);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void tick(Entity entity, Runnable action) {
        boolean root;
        if (entity.m_9236_().f_46443_ || !entity.m_9236_().m_46472_().equals(AbyssDimension.LEVEL)) {
            action.run();
            return;
        }
        Fight fight = FIGHTS.get(entity.m_20148_());
        boolean bl = root = fight != null;
        if (fight == null && QliphothEncounters.helper(entity)) {
            fight = FIGHTS.get(AbyssPlatform.data(entity).m_128342_(OWNER));
        }
        if (fight == null) {
            action.run();
            return;
        }
        if (root && !fight.ready) {
            return;
        }
        if (root) {
            ServerLevel level = (ServerLevel)entity.m_9236_();
            for (Entity helper : fight.helpers) {
                boolean outside;
                boolean bl2 = outside = helper.m_9236_() != level || Math.abs(helper.m_20185_() - fight.center.f_82479_) > 128.0 || Math.abs(helper.m_20189_() - fight.center.f_82481_) > 128.0 || helper.m_20186_() < 0.0 || helper.m_20186_() > 240.0;
                if (outside && !helper.m_213877_()) {
                    helper.m_146870_();
                }
                ChunkPos previousChunk = fight.tickets.get(helper.m_20148_());
                if (helper.m_213877_()) {
                    if (previousChunk != null) {
                        level.m_7726_().m_8438_(TICKET, previousChunk, 2, (Object)helper.m_20148_());
                    }
                    fight.tickets.remove(helper.m_20148_());
                    continue;
                }
                ChunkPos chunk = helper.m_146902_();
                if (previousChunk != null && !previousChunk.equals((Object)chunk)) {
                    level.m_7726_().m_8438_(TICKET, previousChunk, 2, (Object)helper.m_20148_());
                }
                level.m_7726_().m_8387_(TICKET, chunk, 2, (Object)helper.m_20148_());
                fight.tickets.put(helper.m_20148_(), chunk);
            }
            fight.helpers.removeIf(Entity::m_213877_);
            for (ServerPlayer player : ((ServerLevel)entity.m_9236_()).m_6907_()) {
                if (!(player.m_20182_().m_82557_(fight.center) < 4096.0)) continue;
                fight.players.add(player.m_20148_());
                AbyssPlatform.data((Entity)player).m_128359_(PLAYER_FIGHT, EntityType.m_20613_((EntityType)entity.m_6095_()).m_135815_());
            }
        }
        Fight previous = CURRENT.get();
        CURRENT.set(fight);
        try {
            action.run();
        }
        finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    public static LivingEntity blockSource() {
        Fight fight = CURRENT.get();
        return fight == null ? null : fight.boss;
    }

    public static boolean owns(Entity entity) {
        if (!QliphothEncounters.helper(entity)) {
            return false;
        }
        Fight fight = FIGHTS.get(AbyssPlatform.data(entity).m_128342_(OWNER));
        return fight != null && fight.helpers.contains(entity) && !entity.m_213877_();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void clear(LivingEntity boss) {
        Fight fight = FIGHTS.remove(boss.m_20148_());
        if (fight == null) {
            return;
        }
        try {
            if (fight.ready && EntityType.m_20613_((EntityType)boss.m_6095_()).m_135815_().equals("geburah")) {
                QliphothEncounters.invoke(boss, "trySetAllPlayersSins", List.of(), 0, true);
                QliphothEncounters.invoke(boss, "removeCrystalsFromPlayerInventories", new Object[0]);
                QliphothEncounters.invoke(boss, "removeAllArenaTrash", new Object[0]);
            }
            if (fight.ready) {
                QliphothEncounters.invoke(boss, "onFDDespawn", new Object[0]);
            }
        }
        catch (RuntimeException ex) {
            LogUtils.getLogger().error("Qliphoth encounter cleanup failed for {}", (Object)EntityType.m_20613_((EntityType)boss.m_6095_()), (Object)ex);
        }
        finally {
            for (UUID id2 : fight.players) {
                ServerPlayer player = boss.m_9236_().m_7654_().m_6846_().m_11259_(id2);
                if (player == null) continue;
                QliphothEncounters.clearPlayer(player);
            }
            fight.helpers.forEach(Entity::m_146870_);
            fight.helpers.clear();
            Level level = boss.m_9236_();
            if (level instanceof ServerLevel) {
                ServerLevel level2 = (ServerLevel)level;
                fight.tickets.forEach((id, chunk) -> level2.m_7726_().m_8438_(TICKET, chunk, 2, id));
            }
            fight.tickets.clear();
        }
    }

    /*
     * WARNING - void declaration
     */
    public static void clearPlayer(ServerPlayer player) {
        void var5_10;
        CompoundTag data = AbyssPlatform.data((Entity)player);
        String boss = data.m_128461_(PLAYER_FIGHT);
        if (boss.isEmpty()) {
            return;
        }
        String string = boss;
        int n = -1;
        switch (string.hashCode()) {
            case -1361512526: {
                if (!string.equals("chesed")) break;
                boolean bl = false;
                break;
            }
            case 833725110: {
                if (!string.equals("malkuth")) break;
                boolean bl = true;
                break;
            }
            case -90753848: {
                if (!string.equals("geburah")) break;
                int n2 = 2;
            }
        }
        Set<Object> effects = switch (var5_10) {
            case 0 -> Set.of("chesed_gaze", "chesed_darken", "chesed_energized", "shocked");
            case 1 -> Set.of("knight", "coward");
            case 2 -> Set.of("sinner");
            default -> Set.of();
        };
        for (String string2 : effects) {
            MobEffect effect = AbyssRegistries.MOB_EFFECTS.getValue(new ResourceLocation("fdbosses", string2));
            if (effect == null) continue;
            player.m_21195_(effect);
        }
        try {
            Class<?> calls = Class.forName("com.finderfeed.fdlib.FDLibCalls");
            calls.getMethod("stopCutsceneForPlayer", ServerPlayer.class).invoke(null, player);
            if (boss.equals("geburah")) {
                Class<?> clazz = Class.forName("com.finderfeed.fdbosses.content.entities.geburah.sins.attachment.PlayerSins");
                Object sins = clazz.getMethod("getPlayerSins", Player.class).invoke(null, player);
                QliphothEncounters.invoke(sins, "setActiveSins", List.of());
                QliphothEncounters.invoke(sins, "setSinnedTimes", 0);
                QliphothEncounters.invoke(sins, "setSinGainCooldown", 0);
                clazz.getMethod("setPlayerSins", Player.class, clazz).invoke(null, player, sins);
                for (int index = 0; index < player.m_150109_().m_6643_(); ++index) {
                    if (!AbyssRegistries.ITEMS.getKey(player.m_150109_().m_8020_(index).m_41720_()).equals((Object)new ResourceLocation("fdbosses", "geburah_explosive_crystal"))) continue;
                    player.m_150109_().m_6836_(index, ItemStack.f_41583_);
                }
            } else if (boss.equals("malkuth")) {
                Class.forName("com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthWeaknessHandler").getMethod("setCurrentWeakness", Player.class, Integer.TYPE).invoke(null, player, 0);
            }
            data.m_128473_(PLAYER_FIGHT);
        }
        catch (ClassNotFoundException ex) {
            data.m_128473_(PLAYER_FIGHT);
        }
        catch (ReflectiveOperationException | RuntimeException ex) {
            LogUtils.getLogger().error("Cannot clear native Qliphoth player state for {}", (Object)player.m_20148_(), (Object)ex);
        }
    }

    private static Object invoke(Object target, String name, Object ... args) {
        try {
            for (Method method : target.getClass().getMethods()) {
                if (!method.getName().equals(name) || method.getParameterCount() != args.length) continue;
                return method.invoke(target, args);
            }
            throw new NoSuchMethodException(name);
        }
        catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unsupported Qliphoth native contract " + name, ex);
        }
    }

    private static final class Fight {
        final LivingEntity boss;
        final Vec3 center;
        final Set<Entity> helpers = new HashSet<Entity>();
        final Set<UUID> players = new HashSet<UUID>();
        final Map<UUID, ChunkPos> tickets = new HashMap<UUID, ChunkPos>();
        boolean ready;

        Fight(LivingEntity boss, Vec3 center) {
            this.boss = boss;
            this.center = center;
        }
    }
}


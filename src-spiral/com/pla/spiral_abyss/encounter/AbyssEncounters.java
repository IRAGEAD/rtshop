/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.server.packs.resources.PreparableReloadListener
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.level.Level
 */
package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.boss_enviroment.BossEnvironmentCatalog;
import com.pla.spiral_abyss.encounter.FloorRewards;
import com.pla.spiral_abyss.encounter.challenge.ChallengeCatalog;
import com.pla.spiral_abyss.encounter.data.FloorCatalog;
import com.pla.spiral_abyss.encounter.data.MobCatalog;
import com.pla.spiral_abyss.platform.AbyssPlatform;
import com.pla.spiral_abyss.platform.event.AbyssEvents;
import com.pla.spiral_abyss.world.AbyssDimension;
import com.pla.spiral_abyss.world.AbyssParties;
import com.pla.spiral_abyss.world.ArenaSession;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

public final class AbyssEncounters {
    private AbyssEncounters() {
    }

    public static void reload(AbyssEvents.AddReloadListenerEvent event) {
        FloorRewards.resetMissingTableLogs();
        event.addListener((PreparableReloadListener)new MobCatalog());
        event.addListener((PreparableReloadListener)new FloorCatalog());
        event.addListener((PreparableReloadListener)new BossEnvironmentCatalog());
        event.addListener((PreparableReloadListener)new ChallengeCatalog());
    }

    public static void started(AbyssEvents.ServerStartedEvent event) {
        FloorRewards.validateTables(event.getServer());
        ServerLevel level = event.getServer().m_129880_(AbyssDimension.LEVEL);
        if (level != null) {
            AbyssParties.configure(level);
        }
        AbyssParties.get(event.getServer()).prepareCycle();
    }

    public static void datapacksSynced(AbyssEvents.OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            FloorRewards.validateTables(event.getPlayerList().m_7873_());
        }
    }

    public static void tick(AbyssEvents.TickEvent.ServerTickEvent event) {
        if (event.phase == AbyssEvents.TickEvent.Phase.END) {
            AbyssParties.get(event.getServer()).tick();
        }
    }

    public static void stop(AbyssEvents.ServerStoppingEvent event) {
        AbyssParties.stop(event.getServer());
    }

    public static void preventPlayerDeath(AbyssEvents.LivingDeathEvent event) {
        ServerPlayer player;
        LivingEntity livingEntity = event.getEntity();
        if (!(livingEntity instanceof ServerPlayer) || !(player = (ServerPlayer)livingEntity).m_9236_().m_46472_().equals(AbyssDimension.LEVEL)) {
            return;
        }
        event.setCanceled(true);
        AbyssParties parties = AbyssParties.get(player.f_8924_);
        ArenaSession session = parties.sessionFor(player);
        if (session == null) {
            player.m_21153_(player.m_21233_());
            parties.enterOwn(player);
            session = parties.sessionFor(player);
        }
        if (session != null) {
            session.defeat(player);
        } else {
            ArenaSession.restoreSurvival(player);
            parties.leave(player);
        }
    }

    public static void isolateDamage(AbyssEvents.LivingAttackEvent event) {
        ServerLevel level;
        Level level2 = event.getEntity().m_9236_();
        if (!(level2 instanceof ServerLevel) || !(level = (ServerLevel)level2).m_46472_().equals(AbyssDimension.LEVEL)) {
            return;
        }
        Entity attacker = event.getSource().m_7639_();
        if (attacker == null) {
            return;
        }
        AbyssParties parties = AbyssParties.get(level.m_7654_());
        UUID from = parties.hostOf(attacker);
        UUID to = parties.hostOf((Entity)event.getEntity());
        if (from != null && to != null && !from.equals(to)) {
            event.setCanceled(true);
        }
    }

    public static void scaleDamage(AbyssEvents.LivingHurtEvent event) {
        double scale;
        Entity attacker = event.getSource().m_7639_();
        if (attacker != null && AbyssPlatform.data(attacker).m_128403_("spiral_abyss_run") && Double.isFinite(scale = AbyssPlatform.data(attacker).m_128459_("spiral_abyss_damage_scale")) && scale > 0.0) {
            event.setAmount((float)((double)event.getAmount() * scale));
        }
    }

    public static void scaleEffectiveHealth(AbyssEvents.LivingDamageEvent event) {
        Mob mob;
        LivingEntity livingEntity = event.getEntity();
        if (!(livingEntity instanceof Mob) || !AbyssPlatform.data((Entity)(mob = (Mob)livingEntity)).m_128403_("spiral_abyss_run")) {
            return;
        }
        double ratio = AbyssPlatform.data((Entity)mob).m_128459_("spiral_abyss_health_damage_ratio");
        if (Double.isFinite(ratio) && ratio > 0.0 && ratio != 1.0) {
            event.setAmount((float)((double)event.getAmount() * ratio));
        }
    }

    public static void scaleEffectiveHealing(AbyssEvents.LivingHealEvent event) {
        Mob mob;
        LivingEntity livingEntity = event.getEntity();
        if (!(livingEntity instanceof Mob) || !AbyssPlatform.data((Entity)(mob = (Mob)livingEntity)).m_128403_("spiral_abyss_run")) {
            return;
        }
        double ratio = AbyssPlatform.data((Entity)mob).m_128459_("spiral_abyss_health_damage_ratio");
        if (Double.isFinite(ratio) && ratio > 0.0 && ratio != 1.0) {
            event.setAmount((float)((double)event.getAmount() * ratio));
        }
    }
}


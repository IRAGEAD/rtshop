/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.entity.projectile.FishingHook
 *  net.minecraft.world.entity.projectile.Projectile
 *  net.minecraft.world.entity.projectile.ThrownEnderpearl
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.EntityHitResult
 *  net.minecraft.world.phys.HitResult
 */
package com.pla.spiral_abyss.encounter.challenge;

import com.pla.spiral_abyss.encounter.challenge.ChallengeTracker;
import com.pla.spiral_abyss.platform.event.AbyssEvents;
import com.pla.spiral_abyss.world.AbyssDimension;
import com.pla.spiral_abyss.world.AbyssParties;
import com.pla.spiral_abyss.world.ArenaSession;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public final class ChallengeEvents {
    private ChallengeEvents() {
    }

    private static ChallengeTracker tracker(LivingEntity mob) {
        ServerLevel level;
        if (mob instanceof Player) {
            return null;
        }
        Level level2 = mob.m_9236_();
        if (!(level2 instanceof ServerLevel) || !(level = (ServerLevel)level2).m_46472_().equals(AbyssDimension.LEVEL)) {
            return null;
        }
        AbyssParties parties = AbyssParties.get(level.m_7654_());
        ArenaSession session = parties.hostedBy(parties.hostOf((Entity)mob));
        return session == null ? null : session.controller().challengeTracker();
    }

    public static void damage(AbyssEvents.LivingDamageEvent event) {
        ServerPlayer player;
        LivingEntity livingEntity = event.getEntity();
        if (livingEntity instanceof ServerPlayer && (player = (ServerPlayer)livingEntity).m_9236_().m_46472_().equals(AbyssDimension.LEVEL)) {
            ArenaSession session = AbyssParties.get(player.f_8924_).sessionFor(player);
            if (session != null && session.controller().challengeTracker() != null) {
                session.controller().challengeTracker().playerDamaged(player, event);
            }
            return;
        }
        LivingEntity mob = event.getEntity();
        ChallengeTracker tracker = ChallengeEvents.tracker(mob);
        if (tracker != null) {
            tracker.damaged(mob, event);
        }
    }

    public static void death(AbyssEvents.LivingDeathEvent event) {
        LivingEntity mob = event.getEntity();
        ChallengeTracker tracker = ChallengeEvents.tracker(mob);
        if (tracker != null) {
            tracker.died(mob, event);
        }
    }

    public static void projectile(AbyssEvents.EntityJoinLevelEvent event) {
        Projectile projectile;
        Entity entity;
        block5: {
            block4: {
                entity = event.getEntity();
                if (!(entity instanceof Projectile)) break block4;
                projectile = (Projectile)entity;
                if (!event.getLevel().m_5776_() && event.getLevel().m_46472_().equals(AbyssDimension.LEVEL) && (entity = projectile.m_19749_()) instanceof ServerPlayer) break block5;
            }
            return;
        }
        ServerPlayer player = (ServerPlayer)entity;
        ArenaSession session = AbyssParties.get(player.f_8924_).sessionFor(player);
        if (session != null && session.controller().challengeTracker() != null) {
            session.controller().challengeTracker().projectileLaunched(projectile);
        }
    }

    public static void impact(AbyssEvents.ProjectileImpactEvent event) {
        EntityHitResult hit;
        HitResult hitResult;
        if (!(event.getProjectile() instanceof FishingHook) && !(event.getProjectile() instanceof ThrownEnderpearl) || !((hitResult = event.getRayTraceResult()) instanceof EntityHitResult) || !((hitResult = (hit = (EntityHitResult)hitResult).m_82443_()) instanceof LivingEntity)) {
            return;
        }
        LivingEntity mob = (LivingEntity)hitResult;
        ChallengeTracker tracker = ChallengeEvents.tracker(mob);
        if (tracker != null) {
            tracker.projectileImpacted(event, mob);
        }
    }

    public static void totem(AbyssEvents.LivingUseTotemEvent event) {
        ServerPlayer player;
        LivingEntity livingEntity = event.getEntity();
        if (!(livingEntity instanceof ServerPlayer) || !(player = (ServerPlayer)livingEntity).m_9236_().m_46472_().equals(AbyssDimension.LEVEL)) {
            return;
        }
        ArenaSession session = AbyssParties.get(player.f_8924_).sessionFor(player);
        if (session != null && session.controller().challengeTracker() != null) {
            session.controller().challengeTracker().totemUsed(player, event);
        }
    }

    public static void shield(AbyssEvents.ShieldBlockEvent event) {
        ServerPlayer player;
        LivingEntity livingEntity = event.getEntity();
        if (!(livingEntity instanceof ServerPlayer) || !(player = (ServerPlayer)livingEntity).m_9236_().m_46472_().equals(AbyssDimension.LEVEL)) {
            return;
        }
        ArenaSession session = AbyssParties.get(player.f_8924_).sessionFor(player);
        if (session != null && session.controller().challengeTracker() != null) {
            session.controller().challengeTracker().shieldBlocked(player, event);
        }
    }

    public static void equipment(AbyssEvents.LivingEquipmentChangeEvent event) {
        ServerPlayer player;
        LivingEntity livingEntity = event.getEntity();
        if (!(livingEntity instanceof ServerPlayer) || !(player = (ServerPlayer)livingEntity).m_9236_().m_46472_().equals(AbyssDimension.LEVEL)) {
            return;
        }
        ArenaSession session = AbyssParties.get(player.f_8924_).sessionFor(player);
        if (session != null && session.controller().challengeTracker() != null) {
            session.controller().challengeTracker().equipmentChanged(player, event.getSlot(), event.getTo());
        }
    }

    public static void useItem(AbyssEvents.PlayerInteractEvent.RightClickItem event) {
        ChallengeEvents.use(event);
    }

    public static void looseArrow(AbyssEvents.ArrowLooseEvent event) {
        ServerPlayer player;
        Player player2 = event.getEntity();
        if (!(player2 instanceof ServerPlayer) || !(player = (ServerPlayer)player2).m_9236_().m_46472_().equals(AbyssDimension.LEVEL)) {
            return;
        }
        ArenaSession session = AbyssParties.get(player.f_8924_).sessionFor(player);
        if (session != null && session.controller().challengeTracker() != null) {
            session.controller().challengeTracker().itemUsed(player, event.getBow());
        }
    }

    public static void useBlock(AbyssEvents.PlayerInteractEvent.RightClickBlock event) {
        ChallengeEvents.use(event);
    }

    private static void use(AbyssEvents.PlayerInteractEvent event) {
        ServerPlayer player;
        Player player2 = event.getEntity();
        if (!(player2 instanceof ServerPlayer) || !(player = (ServerPlayer)player2).m_9236_().m_46472_().equals(AbyssDimension.LEVEL)) {
            return;
        }
        ArenaSession session = AbyssParties.get(player.f_8924_).sessionFor(player);
        if (session != null && session.controller().challengeTracker() != null) {
            session.controller().challengeTracker().itemUsed(player, event.getItemStack());
        }
    }
}


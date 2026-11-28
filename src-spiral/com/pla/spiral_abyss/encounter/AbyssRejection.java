/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.damagesource.DamageSource
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.OwnableEntity
 */
package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.config.AbyssConfig;
import com.pla.spiral_abyss.encounter.WaveAllies;
import com.pla.spiral_abyss.encounter.data.MobCatalog;
import com.pla.spiral_abyss.platform.AbyssPlatform;
import com.pla.spiral_abyss.platform.AbyssRegistries;
import com.pla.spiral_abyss.platform.event.AbyssEvents;
import com.pla.spiral_abyss.world.AbyssParties;
import com.pla.spiral_abyss.world.ArenaSession;
import com.pla.spiral_abyss.world.BossProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;

public final class AbyssRejection {
    public static final float DAMAGE_MULTIPLIER = 0.4f;

    private AbyssRejection() {
    }

    public static boolean isRejected(ServerPlayer player, int floor, ResourceLocation id) {
        return player != null && (Boolean)AbyssConfig.ABYSSAL_REJECTION.get() != false && (floor == 5 || floor == 6) && MobCatalog.isBoss(id) && !BossProgress.hasDefeated(player, id);
    }

    public static boolean applies(ServerPlayer player, LivingEntity victim) {
        if (player == null || !WaveAllies.waveMob((Entity)victim) || !AbyssPlatform.data((Entity)victim).m_128403_("spiral_abyss_host")) {
            return false;
        }
        ArenaSession session = AbyssParties.get(player.f_8924_).hostedBy(AbyssPlatform.data((Entity)victim).m_128342_("spiral_abyss_host"));
        return session != null && session.controller().owns((Entity)victim) && AbyssRejection.isRejected(player, session.controller().currentFloor(), AbyssRejection.bossId(victim));
    }

    public static ResourceLocation bossId(LivingEntity victim) {
        ResourceLocation root = ResourceLocation.m_135820_((String)AbyssPlatform.data((Entity)victim).m_128461_("spiral_abyss_phase_boss"));
        return root != null && MobCatalog.isBoss(root) ? root : AbyssRegistries.ENTITY_TYPES.getKey(victim.m_6095_());
    }

    public static ServerPlayer attacker(DamageSource source) {
        ServerPlayer player = AbyssRejection.playerOwner(source.m_7639_());
        return player != null ? player : AbyssRejection.playerOwner(source.m_7640_());
    }

    private static ServerPlayer playerOwner(Entity entity) {
        for (int depth = 0; entity != null && depth < 8; ++depth) {
            if (entity instanceof ServerPlayer) {
                ServerPlayer player = (ServerPlayer)entity;
                return player;
            }
            Entity next = WaveAllies.owner(entity);
            if (next == entity && entity instanceof OwnableEntity) {
                OwnableEntity owned = (OwnableEntity)entity;
                next = owned.m_269323_();
            }
            if (next == entity) {
                return null;
            }
            entity = next;
        }
        return null;
    }

    public static void scaleDamage(AbyssEvents.LivingDamageEvent event) {
        if (!event.getEntity().m_9236_().f_46443_ && AbyssRejection.applies(AbyssRejection.attacker(event.getSource()), event.getEntity())) {
            event.setAmount(event.getAmount() * 0.4f);
        }
    }
}


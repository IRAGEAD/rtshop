/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.StringTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.ServerScoreboard
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.damagesource.DamageSource
 *  net.minecraft.world.entity.AreaEffectCloud
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.item.PrimedTnt
 *  net.minecraft.world.entity.projectile.EvokerFangs
 *  net.minecraft.world.entity.projectile.Projectile
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.saveddata.SavedData
 *  net.minecraft.world.scores.PlayerTeam
 *  net.minecraft.world.scores.Scoreboard
 */
package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.encounter.QliphothEncounters;
import com.pla.spiral_abyss.platform.AbyssPlatform;
import com.pla.spiral_abyss.platform.event.AbyssEvents;
import com.pla.spiral_abyss.world.AbyssDimension;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

public final class WaveAllies {
    public static final String TEAM = "abyss";

    private WaveAllies() {
    }

    public static boolean waveMob(Entity entity) {
        return entity instanceof LivingEntity && !QliphothEncounters.helper(entity) && entity.m_9236_().m_46472_().equals(AbyssDimension.LEVEL) && AbyssPlatform.data(entity).m_128403_("spiral_abyss_run");
    }

    public static Entity owner(Entity entity) {
        for (int depth = 0; entity != null && depth < 8; ++depth) {
            Entity next;
            if (entity instanceof Projectile) {
                Projectile projectile = (Projectile)entity;
                v0 = projectile.m_19749_();
            } else if (entity instanceof AreaEffectCloud) {
                AreaEffectCloud cloud = (AreaEffectCloud)entity;
                v0 = cloud.m_19749_();
            } else if (entity instanceof EvokerFangs) {
                EvokerFangs fangs = (EvokerFangs)entity;
                v0 = fangs.m_19749_();
            } else if (entity instanceof PrimedTnt) {
                PrimedTnt tnt = (PrimedTnt)entity;
                v0 = tnt.m_19749_();
            } else {
                v0 = next = null;
            }
            if (next == null || next == entity) {
                return entity;
            }
            entity = next;
        }
        return entity;
    }

    private static boolean friendly(Entity victim, DamageSource source) {
        if (!WaveAllies.waveMob(victim)) {
            return false;
        }
        Entity attacker = WaveAllies.owner(source.m_7639_());
        if (!WaveAllies.waveMob(attacker)) {
            attacker = WaveAllies.owner(source.m_7640_());
        }
        return WaveAllies.waveMob(attacker);
    }

    public static void add(LivingEntity mob) {
        Level level;
        if (!WaveAllies.waveMob((Entity)mob) || !((level = mob.m_9236_()) instanceof ServerLevel)) {
            return;
        }
        ServerLevel level2 = (ServerLevel)level;
        WaveAllies.addMember((Scoreboard)level2.m_7654_().m_129896_(), WaveAllies.ledger(level2.m_7654_()), mob.m_6302_());
    }

    static void addMember(Scoreboard board, Membership ledger, String name) {
        PlayerTeam team = board.m_83489_(TEAM);
        if (team == null) {
            team = board.m_83492_(TEAM);
        }
        team.m_83355_(false);
        board.m_6546_(name, team);
        if (ledger.members.add(name)) {
            ledger.m_77762_();
        }
    }

    public static void remove(LivingEntity mob) {
        Level level = mob.m_9236_();
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel level2 = (ServerLevel)level;
        WaveAllies.removeMember((Scoreboard)level2.m_7654_().m_129896_(), WaveAllies.ledger(level2.m_7654_()), mob.m_6302_());
    }

    static void removeMember(Scoreboard board, Membership ledger, String name) {
        if (!ledger.members.remove(name)) {
            return;
        }
        ledger.m_77762_();
        PlayerTeam team = board.m_83489_(TEAM);
        if (team != null && board.m_83500_(name) == team) {
            board.m_6519_(name, team);
        }
    }

    public static void join(AbyssEvents.EntityJoinLevelEvent event) {
        QliphothEncounters.capture(event.getEntity());
        if (!event.getLevel().m_5776_() && WaveAllies.waveMob(event.getEntity())) {
            WaveAllies.add((LivingEntity)event.getEntity());
        }
    }

    public static void leave(AbyssEvents.EntityLeaveLevelEvent event) {
        if (!event.getLevel().m_5776_() && WaveAllies.waveMob(event.getEntity())) {
            WaveAllies.remove((LivingEntity)event.getEntity());
        }
    }

    public static void target(AbyssEvents.LivingChangeTargetEvent event) {
        if (WaveAllies.waveMob((Entity)event.getEntity()) && WaveAllies.waveMob((Entity)event.getNewTarget())) {
            event.setNewTarget(null);
        }
    }

    public static void attack(AbyssEvents.LivingAttackEvent event) {
        if (WaveAllies.friendly((Entity)event.getEntity(), event.getSource())) {
            event.setCanceled(true);
        }
    }

    public static void hurt(AbyssEvents.LivingHurtEvent event) {
        if (WaveAllies.friendly((Entity)event.getEntity(), event.getSource())) {
            event.setCanceled(true);
        }
    }

    public static void started(AbyssEvents.ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        Membership ledger = WaveAllies.ledger(server);
        ServerScoreboard board = server.m_129896_();
        WaveAllies.clearMembers((Scoreboard)board, ledger);
        ServerLevel abyss = server.m_129880_(AbyssDimension.LEVEL);
        if (abyss != null) {
            for (Entity entity : abyss.m_8583_()) {
                if (!WaveAllies.waveMob(entity)) continue;
                WaveAllies.add((LivingEntity)entity);
            }
        }
    }

    private static Membership ledger(MinecraftServer server) {
        return (Membership)server.m_129783_().m_8895_().m_164861_(Membership::load, Membership::new, "spiral_abyss_team");
    }

    static void clearMembers(Scoreboard board, Membership ledger) {
        for (String member : Set.copyOf(ledger.members)) {
            WaveAllies.removeMember(board, ledger, member);
        }
    }

    static final class Membership
    extends SavedData {
        private final Set<String> members = new HashSet<String>();

        Membership() {
        }

        static Membership load(CompoundTag tag) {
            Membership data = new Membership();
            for (Tag member : tag.m_128437_("Members", 8)) {
                data.members.add(member.m_7916_());
            }
            return data;
        }

        public CompoundTag m_7176_(CompoundTag tag) {
            ListTag list = new ListTag();
            this.members.forEach(name -> list.add((Object)StringTag.m_129297_((String)name)));
            tag.m_128365_("Members", (Tag)list);
            return tag;
        }
    }
}


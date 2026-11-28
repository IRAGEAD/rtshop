/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.entity.ai.control.MoveControl
 *  net.minecraft.world.entity.boss.wither.WitherBoss
 *  net.minecraft.world.entity.monster.Ghast
 *  net.minecraft.world.phys.Vec3
 */
package com.pla.spiral_abyss.world;

import com.pla.spiral_abyss.world.AbyssDimension;
import com.pla.spiral_abyss.world.ArenaBounds;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.phys.Vec3;

public final class ArenaFlight {
    private ArenaFlight() {
    }

    public static boolean combatFlyer(Mob mob) {
        return mob instanceof WitherBoss || mob instanceof Ghast;
    }

    public static void steer(Mob mob, ArenaBounds bounds) {
        double offset;
        if (!mob.m_9236_().m_46472_().equals(AbyssDimension.LEVEL) || !ArenaFlight.combatFlyer(mob)) {
            return;
        }
        double top = 120.0 - (double)mob.m_20206_();
        LivingEntity target = mob.m_5448_();
        double d = offset = mob instanceof Ghast ? 3.0 : 5.0;
        double hover = Mth.m_14008_((double)(target != null && target.m_6084_() && !target.m_5833_() ? target.m_20186_() + offset : 65.0 + (double)(mob instanceof Ghast ? 3 : 8)), (double)(65.0 + offset), (double)top);
        MoveControl control = mob.m_21566_();
        if (control.m_24995_() && (control.m_25001_() > hover || control.m_25001_() < 65.0 || Math.abs(control.m_25000_() - bounds.center().f_82479_) > 30.0 || Math.abs(control.m_25002_() - bounds.center().f_82481_) > 30.0)) {
            mob.m_21573_().m_26573_();
            control.m_6849_(Mth.m_14008_((double)control.m_25000_(), (double)(bounds.center().f_82479_ - 24.0), (double)(bounds.center().f_82479_ + 24.0)), hover, Mth.m_14008_((double)control.m_25002_(), (double)(bounds.center().f_82481_ - 24.0), (double)(bounds.center().f_82481_ + 24.0)), 1.0);
        }
        if (mob.m_20186_() > hover) {
            Vec3 velocity = mob.m_20184_();
            mob.m_20334_(velocity.f_82479_, Math.min(velocity.f_82480_, -0.3), velocity.f_82481_);
            mob.f_19812_ = true;
        }
    }

    public static void stop(Mob mob) {
        mob.m_21573_().m_26573_();
        mob.m_21566_().m_6849_(mob.m_20185_(), mob.m_20186_(), mob.m_20189_(), 0.0);
        mob.m_20256_(Vec3.f_82478_);
        mob.f_19789_ = 0.0f;
        mob.f_19812_ = true;
    }
}


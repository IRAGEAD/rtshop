/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 */
package com.pla.spiral_abyss.world;

import com.pla.spiral_abyss.world.AbyssDimension;
import com.pla.spiral_abyss.world.AbyssParties;
import com.pla.spiral_abyss.world.ArenaFlight;
import com.pla.spiral_abyss.world.ArenaSession;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record ArenaBounds(Vec3 center) {
    public static final double HALF_SIZE = 30.0;
    public static final double FLIGHT_RADIUS = 128.0;
    public static final double FLOOR_Y = 65.0;
    public static final double CEILING_Y = 120.0;
    public static final double FLIGHT_CEILING_Y = 240.0;

    public Vec3 entrance() {
        return this.center.m_82520_(0.0, 0.0, 8.0);
    }

    public AABB structureArea() {
        return new AABB(this.center.f_82479_ - 34.0, 64.0, this.center.f_82481_ - 34.0, this.center.f_82479_ + 35.0, 121.0, this.center.f_82481_ + 35.0);
    }

    public double combatRadius(Entity entity) {
        ServerLevel level;
        Level level2 = entity.m_9236_();
        if (!(level2 instanceof ServerLevel) || !(level = (ServerLevel)level2).m_46472_().equals(AbyssDimension.LEVEL)) {
            return 30.0;
        }
        AbyssParties parties = AbyssParties.get(level.m_7654_());
        ArenaSession session = parties.hostedBy(parties.hostOf(entity));
        return session == null || session.bounds().center().m_82557_(this.center) >= 1.0 ? 30.0 : session.controller().bossEnvironments().combatRadius();
    }

    public boolean outsidePlayer(Entity entity) {
        double radius = this.combatRadius(entity);
        AABB box = entity.m_20191_();
        return box.f_82288_ < this.center.f_82479_ - radius || box.f_82291_ > this.center.f_82479_ + radius || box.f_82290_ < this.center.f_82481_ - radius || box.f_82293_ > this.center.f_82481_ + radius || box.f_82289_ < this.feetFloor(entity) || box.f_82292_ > 120.0;
    }

    public boolean rescueMob(Entity entity, boolean ghost) {
        block6: {
            block5: {
                Mob mob;
                if (Math.abs(entity.m_20185_() - this.center.f_82479_) > 128.0 || Math.abs(entity.m_20189_() - this.center.f_82481_) > 128.0) break block5;
                double d = entity.m_20191_().f_82292_;
                double d2 = entity instanceof Mob && ArenaFlight.combatFlyer(mob = (Mob)entity) ? 120.0 : 240.0;
                if (!(d > d2)) break block6;
            }
            return true;
        }
        if (ghost && entity.m_20186_() < 65.0) {
            return false;
        }
        if (entity.m_20186_() < this.feetFloor(entity)) {
            return true;
        }
        return entity.m_20186_() <= 65.05 && (Math.abs(entity.m_20185_() - this.center.f_82479_) > this.combatRadius(entity) - (double)(entity.m_20205_() / 2.0f) || Math.abs(entity.m_20189_() - this.center.f_82481_) > this.combatRadius(entity) - (double)(entity.m_20205_() / 2.0f));
    }

    private double feetFloor(Entity entity) {
        BlockPos surface = BlockPos.m_274561_((double)entity.m_20185_(), (double)64.0, (double)entity.m_20189_());
        return entity.m_9236_().m_8055_(surface).m_60713_(Blocks.f_50135_) ? 64.875 : 65.0;
    }
}


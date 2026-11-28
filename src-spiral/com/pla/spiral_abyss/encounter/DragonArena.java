/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction$Axis
 *  net.minecraft.core.Position
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.boss.enderdragon.EnderDragon
 *  net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.phys.Vec3
 *  net.minecraft.world.phys.shapes.VoxelShape
 */
package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.encounter.MobDeathLifecycle;
import com.pla.spiral_abyss.platform.AbyssPlatform;
import com.pla.spiral_abyss.world.AbyssDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class DragonArena {
    public static final String ORIGIN = "spiral_abyss_dragon_origin";

    private DragonArena() {
    }

    public static boolean abyss(EnderDragon dragon) {
        return dragon.m_9236_().m_46472_().equals(AbyssDimension.LEVEL);
    }

    public static boolean managed(EnderDragon dragon) {
        return DragonArena.abyss(dragon) && AbyssPlatform.data((Entity)dragon).m_128441_(ORIGIN);
    }

    public static Vec3 landingTarget(EnderDragon dragon) {
        BlockPos origin = dragon.m_287165_();
        double y = origin.m_123342_();
        for (int dy = 0; dy <= 8; ++dy) {
            BlockPos pos = origin.m_6630_(dy);
            VoxelShape shape = dragon.m_9236_().m_8055_(pos).m_60812_((BlockGetter)dragon.m_9236_(), pos);
            if (shape.m_83281_()) continue;
            y = (double)pos.m_123342_() + shape.m_83297_(Direction.Axis.Y);
        }
        return new Vec3((double)origin.m_123341_() + 0.5, y, (double)origin.m_123343_() + 0.5);
    }

    public static void prepare(LivingEntity mob, Vec3 center) {
        EnderDragon dragon;
        if (!(mob instanceof EnderDragon) || !DragonArena.abyss(dragon = (EnderDragon)mob)) {
            return;
        }
        BlockPos origin = BlockPos.m_274446_((Position)center);
        AbyssPlatform.data((Entity)dragon).m_128356_(ORIGIN, origin.m_121878_());
        dragon.m_287266_(origin);
        dragon.m_287231_(null);
        dragon.m_31157_().m_31416_(EnderDragonPhase.f_31377_);
    }

    public static boolean defeated(LivingEntity mob) {
        EnderDragon dragon;
        return mob.m_21224_() || mob.m_213877_() || !mob.m_6084_() || mob instanceof EnderDragon && (dragon = (EnderDragon)mob).m_31157_().m_31415_().m_7309_() == EnderDragonPhase.f_31386_;
    }

    public static boolean finished(LivingEntity mob, ServerLevel level) {
        if (mob.m_213877_() || mob.m_9236_() != level || level.m_8791_(mob.m_20148_()) != mob) {
            return true;
        }
        return !MobDeathLifecycle.waitsForRemoval(mob) && (mob.m_21224_() || !mob.m_6084_());
    }

    public static BlockPos node(BlockPos origin, int index) {
        int start;
        int count;
        if (index < 0 || index >= 24) {
            throw new IllegalArgumentException("Dragon node index must be 0..23");
        }
        int n = index < 12 ? 12 : (count = index < 20 ? 8 : 4);
        int n2 = index < 12 ? 0 : (start = index < 20 ? 12 : 20);
        int radius = index < 12 ? 60 : (index < 20 ? 40 : 20);
        double angle = (double)(index - start) * Math.PI * 2.0 / (double)count;
        return origin.m_7918_(Mth.m_14107_((double)((double)radius * Math.cos(angle))), index >= 12 && index < 20 ? 24 : 14, Mth.m_14107_((double)((double)radius * Math.sin(angle))));
    }

    public static Vec3 approach(BlockPos origin, Vec3 player) {
        Vec3 direction = player == null ? new Vec3(-1.0, 0.0, 0.0) : new Vec3(player.f_82479_ - (double)origin.m_123341_(), 0.0, player.f_82481_ - (double)origin.m_123343_()).m_82541_();
        return new Vec3((double)origin.m_123341_() - direction.f_82479_ * 40.0, (double)(origin.m_123342_() + 40), (double)origin.m_123343_() - direction.f_82481_ * 40.0);
    }
}


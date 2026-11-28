/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.boss.enderdragon.EnderDragon
 *  net.minecraft.world.entity.boss.enderdragon.phases.DragonChargePlayerPhase
 *  net.minecraft.world.entity.boss.enderdragon.phases.DragonStrafePlayerPhase
 *  net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase
 *  net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhaseManager
 */
package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.encounter.DragonArena;
import com.pla.spiral_abyss.platform.AbyssPlatform;
import com.pla.spiral_abyss.world.AbyssParties;
import com.pla.spiral_abyss.world.ArenaSession;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonChargePlayerPhase;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonStrafePlayerPhase;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhaseManager;

public final class DragonCombat {
    private EnderDragonPhase<?> previous;
    private int phaseTicks;
    private int flightTicks;
    private int nextAttack;

    public static ServerPlayer target(EnderDragon dragon) {
        ServerPlayer player;
        LivingEntity livingEntity;
        if (!DragonArena.managed(dragon) || dragon.m_9236_().f_46443_ || !((livingEntity = dragon.m_5448_()) instanceof ServerPlayer) || !(player = (ServerPlayer)livingEntity).m_6084_() || player.m_5833_() || player.m_7500_() || player.m_9236_() != dragon.m_9236_() || !AbyssPlatform.data((Entity)dragon).m_128403_("spiral_abyss_host")) {
            return null;
        }
        ArenaSession session = AbyssParties.get(player.f_8924_).hostedBy(AbyssPlatform.data((Entity)dragon).m_128342_("spiral_abyss_host"));
        return session != null && session.isCombatant(player) && !session.bounds().outsidePlayer((Entity)player) ? player : null;
    }

    public void tick(EnderDragon dragon) {
        if (!DragonArena.managed(dragon) || dragon.m_9236_().f_46443_ || dragon.m_21525_() || DragonArena.defeated((LivingEntity)dragon)) {
            return;
        }
        EnderDragonPhaseManager manager = dragon.m_31157_();
        EnderDragonPhase phase = manager.m_31415_().m_7309_();
        if (phase != this.previous) {
            this.previous = phase;
            this.phaseTicks = 0;
        }
        ++this.phaseTicks;
        ServerPlayer player = DragonCombat.target(dragon);
        if (phase == EnderDragonPhase.f_31377_ || phase == EnderDragonPhase.f_31378_) {
            if (player == null) {
                this.flightTicks = 0;
                if (phase == EnderDragonPhase.f_31378_) {
                    manager.m_31416_(EnderDragonPhase.f_31377_);
                }
                return;
            }
            if (++this.flightTicks < 200) {
                return;
            }
            this.flightTicks = 0;
            switch (this.nextAttack++ % 3) {
                case 0: {
                    manager.m_31416_(EnderDragonPhase.f_31385_);
                    ((DragonChargePlayerPhase)manager.m_31418_(EnderDragonPhase.f_31385_)).m_31207_(player.m_20182_());
                    break;
                }
                case 1: {
                    manager.m_31416_(EnderDragonPhase.f_31379_);
                    break;
                }
                default: {
                    manager.m_31416_(EnderDragonPhase.f_31378_);
                    ((DragonStrafePlayerPhase)manager.m_31418_(EnderDragonPhase.f_31378_)).m_31358_((LivingEntity)player);
                    break;
                }
            }
        } else {
            this.flightTicks = 0;
            if (phase == EnderDragonPhase.f_31383_) {
                if (player != null) {
                    double dx = player.m_20185_() - dragon.m_20185_();
                    double dz = player.m_20189_() - dragon.m_20189_();
                    float yaw = (float)(180.0 - Mth.m_14136_((double)dx, (double)dz) * 180.0 / Math.PI);
                    dragon.m_146922_(Mth.m_14148_((float)dragon.m_146908_(), (float)yaw, (float)6.0f));
                    if (this.phaseTicks >= 30) {
                        manager.m_31416_(EnderDragonPhase.f_31384_);
                    }
                } else if (this.phaseTicks >= 100) {
                    manager.m_31416_(EnderDragonPhase.f_31381_);
                }
            } else if (phase == EnderDragonPhase.f_31382_ && this.phaseTicks >= 160) {
                manager.m_31416_(EnderDragonPhase.f_31381_);
            } else if (phase == EnderDragonPhase.f_31385_ && (player == null || this.phaseTicks >= 100) || phase == EnderDragonPhase.f_31381_ && this.phaseTicks >= 100 || phase == EnderDragonPhase.f_31380_ && this.phaseTicks >= 240) {
                manager.m_31416_(EnderDragonPhase.f_31377_);
            } else if (phase == EnderDragonPhase.f_31379_ && this.phaseTicks >= 200) {
                manager.m_31416_(EnderDragonPhase.f_31380_);
            }
        }
    }
}


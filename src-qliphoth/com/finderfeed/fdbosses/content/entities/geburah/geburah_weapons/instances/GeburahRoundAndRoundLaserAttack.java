/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.FDHelpers
 *  com.finderfeed.fdlib.FDLibCalls
 *  com.finderfeed.fdlib.data_structures.Pair
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.entities.geburah.geburah_weapons.instances;

import com.finderfeed.fdbosses.BossUtil;
import com.finderfeed.fdbosses.content.entities.geburah.GeburahEntity;
import com.finderfeed.fdbosses.content.entities.geburah.geburah_weapons.GeburahWeaponAttack;
import com.finderfeed.fdbosses.content.entities.geburah.judgement_bird.JudgementBirdEntity;
import com.finderfeed.fdbosses.content.entities.geburah.particles.geburah_ray.GeburahRayOptions;
import com.finderfeed.fdbosses.content.entities.geburah.rotating_weapons.GeburahWeaponRotationController;
import com.finderfeed.fdbosses.content.entities.geburah.rotating_weapons.rotations.GeburahConstantWeaponRotation;
import com.finderfeed.fdbosses.content.entities.geburah.rotating_weapons.rotations.GeburahLerpingRotation;
import com.finderfeed.fdbosses.content.entities.geburah.rotating_weapons.rotations.GeburahWeaponRotation;
import com.finderfeed.fdbosses.init.BossConfigs;
import com.finderfeed.fdbosses.init.BossDamageSources;
import com.finderfeed.fdbosses.init.BossSounds;
import com.finderfeed.fdbosses.init.GeburahSins;
import com.finderfeed.fdlib.FDHelpers;
import com.finderfeed.fdlib.FDLibCalls;
import com.finderfeed.fdlib.data_structures.Pair;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class GeburahRoundAndRoundLaserAttack
extends GeburahWeaponAttack {
    private boolean forward;
    private boolean circleComplete = false;
    private int cannonId = -1;
    private Player targetPlayer;
    private int fireShotTime = -1;
    private Vec3 targetPos;
    private float rotationSnapshot;
    private boolean birdsWereHit = false;

    public GeburahRoundAndRoundLaserAttack(GeburahEntity geburah, boolean forward) {
        super(geburah);
        this.forward = forward;
    }

    @Override
    public void onAttackStart() {
        this.targetPlayer = this.geburah.pickRandomCombatant();
        this.rotationSnapshot = this.geburah.getWeaponRotationController().getCurrentRotation();
        if (this.targetPlayer != null) {
            this.geburah.laserAttackPreparator.launchPreparation(20);
        }
    }

    @Override
    public void tickAttack() {
        if (this.targetPlayer == null || this.targetPlayer.m_21224_()) {
            this.targetPlayer = this.geburah.pickRandomCombatant();
        }
        if (this.fireShotTime == -1) {
            if (this.targetPlayer != null) {
                float maxSpeed = 15.0f;
                this.rotateUntilStopAngleIsReached(maxSpeed, 50.0f);
                this.rotateToTargetPos(maxSpeed);
            }
        } else {
            this.fireShotTime = Mth.m_14045_((int)(this.fireShotTime - 1), (int)0, (int)Integer.MAX_VALUE);
            this.fireLasers(false);
            if (this.fireShotTime <= 0) {
                this.geburah.setLaserVisualsState(false);
            }
        }
    }

    private void fireLasers(boolean laserStart) {
        if (laserStart) {
            BossUtil.geburahWeaponsStartLaser((ServerLevel)this.geburah.m_9236_(), this.geburah.m_20182_(), 120.0, this.geburah);
        }
        this.geburah.setLaserVisualsState(true);
        ArrayList allHitEntities = new ArrayList();
        for (Pair<Vec3, Vec3> cannonData : this.geburah.getCannonsPositionAndDirection()) {
            Vec3 position = (Vec3)cannonData.first;
            Vec3 direction = (Vec3)cannonData.second;
            Vec3 end = position.m_82549_(direction.m_82490_(27.0));
            if (laserStart) {
                GeburahRayOptions options = GeburahRayOptions.builder().width(0.5f).color(0.3f, 0.8f, 1.0f, 1.0f).end(end).stay(2).out(3).build();
                FDLibCalls.sendParticles((ServerLevel)((ServerLevel)this.geburah.m_9236_()), (ParticleOptions)options, (Vec3)position, (double)120.0);
            }
            List entities = FDHelpers.traceEntities((Level)this.geburah.m_9236_(), (Vec3)position, (Vec3)end, (double)0.5, e -> e instanceof LivingEntity);
            allHitEntities.addAll(entities);
        }
        List<Entity> birds = allHitEntities.stream().filter(e -> {
            boolean bl;
            if (e instanceof JudgementBirdEntity) {
                JudgementBirdEntity judgementBirdEntity = (JudgementBirdEntity)e;
                bl = true;
            } else {
                bl = false;
            }
            return bl;
        }).toList();
        allHitEntities.removeAll(birds);
        if (!this.geburah.m_21224_()) {
            if (this.geburah.getSinnedTimes() == 5 && this.geburah.f_19802_ > 0) {
                return;
            }
            boolean geburahWasHurt = false;
            for (Entity bird : birds) {
                if (((List)this.geburah.m_20088_().m_135370_(GeburahEntity.ACTIVE_SINS)).contains(GeburahSins.KILL_ENTITY_SIN.get()) && this.geburah.canBeDamaged && this.geburah.f_19802_ == 0) {
                    this.geburah.f_19802_ = 20;
                    this.geburah.setSinnedTimes(this.geburah.getSinnedTimes() + 1);
                    geburahWasHurt = true;
                }
                bird.m_6074_();
            }
            float damage = BossUtil.transformDamage(this.geburah.m_9236_(), BossConfigs.BOSS_CONFIG.get().geburahConfig.predictiveLaserAttackDamage);
            for (Entity entity : allHitEntities) {
                if (!(entity instanceof LivingEntity)) continue;
                LivingEntity livingEntity = (LivingEntity)entity;
                int geburahSinnedTimes = this.geburah.getSinnedTimes();
                if ((geburahSinnedTimes == 5 || geburahSinnedTimes == 10) && geburahWasHurt) {
                    livingEntity.f_19802_ = 20;
                    continue;
                }
                livingEntity.m_6469_(BossDamageSources.GEBURAH_LASER_STRIKE_SOURCE, damage);
            }
        }
    }

    private void rotateToTargetPos(float maxSpeed) {
        if (this.targetPos != null) {
            GeburahWeaponRotationController controller = this.geburah.getWeaponRotationController();
            if (this.rotationSnapshot != -1.0f) {
                if (!this.forward) {
                    this.rotationSnapshot = -this.rotationSnapshot;
                }
                BossUtil.geburahPrepareRoundAndRoundLasers((ServerLevel)this.geburah.m_9236_(), this.geburah.m_20182_(), 100.0, this.geburah);
                controller.rotateWeapons(new GeburahLerpingRotation(controller, maxSpeed, this.rotationSnapshot));
                this.rotationSnapshot = -1.0f;
            } else if (controller.finishedRotation()) {
                this.fireLasers(true);
                this.geburah.m_9236_().m_6263_(null, this.geburah.m_20185_(), this.geburah.m_20186_(), this.geburah.m_20189_(), BossSounds.GEBURAH_RAY_SHOT.get(), SoundSource.HOSTILE, 4.0f, 1.0f);
                this.fireShotTime = 5;
            }
        }
    }

    private void rotateUntilStopAngleIsReached(float maxSpeed, float startDecelerationAngle) {
        if (this.targetPos == null) {
            if (this.hasFinishedCircle()) {
                if (this.cannonId == -1) {
                    this.cannonId = this.getFarthestCannonToPlayer(this.targetPlayer);
                }
                this.circleComplete = true;
                Vec3 targetPos = this.getPlayerTargetPos(20.0f);
                double angle = this.getCurrentAngleToTargetPos(targetPos);
                if (angle <= (double)startDecelerationAngle) {
                    this.rotationSnapshot = (float)angle;
                    this.targetPos = targetPos;
                }
            }
            if (!this.forward) {
                maxSpeed = -maxSpeed;
            }
            this.geburah.getWeaponRotationController().startConstantRotation(maxSpeed);
            GeburahWeaponRotation geburahWeaponRotation = this.geburah.getWeaponRotationController().getWeaponRotation();
            if (geburahWeaponRotation instanceof GeburahConstantWeaponRotation) {
                GeburahConstantWeaponRotation constantWeaponRotation = (GeburahConstantWeaponRotation)geburahWeaponRotation;
                constantWeaponRotation.setPlaySound(true);
            }
        }
    }

    private double getCurrentAngleToTargetPos(Vec3 targetPos) {
        Pair<Vec3, Vec3> cannonData = this.geburah.getCannonsPositionAndDirection().get(this.cannonId);
        Vec3 pos = (Vec3)cannonData.first;
        Vec3 cannonDirection = (Vec3)cannonData.second;
        ((ServerLevel)this.geburah.m_9236_()).m_8767_((ParticleOptions)BallParticleOptions.builder().stay(3).build(), pos.f_82479_ + cannonDirection.f_82479_, pos.f_82480_, pos.f_82481_ + cannonDirection.f_82481_, 1, 0.0, 0.0, 0.0, 0.0);
        Vec3 geb = this.geburah.m_20182_().m_82542_(1.0, 0.0, 1.0);
        Vec3 between = targetPos.m_82546_(geb);
        Vec3 rotatedCannonDirection = cannonDirection.m_82524_(1.5707964f);
        double angle = Math.toDegrees(FDMathUtil.angleBetweenVectors((Vec3)cannonDirection, (Vec3)between));
        double dot = rotatedCannonDirection.m_82526_(between.m_82541_());
        if (this.forward && dot < 0.0 || dot > 0.0 && !this.forward) {
            angle = 360.0 - angle;
        }
        return angle;
    }

    private Vec3 getPlayerTargetPos(float angleModifier) {
        Pair<Vec3, Vec3> playerData = this.geburah.getPlayerPositionsCollector().getOldAndCurrentPlayerPosition(this.targetPlayer);
        Vec3 old = ((Vec3)playerData.first).m_82546_(this.geburah.m_20182_()).m_82542_(1.0, 0.0, 1.0);
        Vec3 current = ((Vec3)playerData.second).m_82546_(this.geburah.m_20182_()).m_82542_(1.0, 0.0, 1.0);
        double angle = FDMathUtil.angleBetweenVectors((Vec3)old.m_82541_(), (Vec3)current.m_82541_());
        if (Double.isNaN(angle)) {
            angle = 0.0;
        }
        Vec3 rotated = current.m_82524_(1.5707964f);
        double dot = old.m_82541_().m_82526_(rotated.m_82541_());
        if (dot > 0.0) {
            return this.geburah.m_20182_().m_82549_(current.m_82524_(-((float)angle) * angleModifier)).m_82542_(1.0, 0.0, 1.0);
        }
        return this.geburah.m_20182_().m_82549_(current.m_82524_((float)angle * angleModifier)).m_82542_(1.0, 0.0, 1.0);
    }

    private boolean hasFinishedCircle() {
        if (this.circleComplete) {
            return true;
        }
        GeburahWeaponRotationController rotationController = this.geburah.getWeaponRotationController();
        float currentRotation = rotationController.getCurrentRotation();
        if (this.forward) {
            return currentRotation - this.rotationSnapshot > 90.0f;
        }
        return currentRotation - this.rotationSnapshot < -90.0f;
    }

    private int getFarthestCannonToPlayer(Player player) {
        List<Pair<Vec3, Vec3>> cannonData = this.geburah.getCannonsPositionAndDirection();
        double maxAngle = 0.0;
        int maxId = 0;
        for (int i = 0; i < 8; ++i) {
            Vec3 cannonDirection = (Vec3)cannonData.get((int)i).second;
            Vec3 between = player.m_20182_().m_82546_(this.geburah.m_20182_()).m_82542_(1.0, 0.0, 1.0);
            double angle = FDMathUtil.angleBetweenVectors((Vec3)cannonDirection.m_82541_(), (Vec3)between.m_82541_());
            if (!(angle > maxAngle)) continue;
            maxAngle = angle;
            maxId = i;
        }
        return maxId;
    }

    @Override
    public boolean hasEnded() {
        return this.fireShotTime == 0 || this.targetPlayer == null || this.targetPlayer.m_21224_();
    }

    @Override
    public void onAttackEnd() {
        this.geburah.getWeaponRotationController().stopRotation();
    }

    @Override
    public boolean canBeChanged() {
        return false;
    }
}


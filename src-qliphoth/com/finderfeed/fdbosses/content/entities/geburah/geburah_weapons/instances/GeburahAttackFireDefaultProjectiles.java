/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.data_structures.Pair
 *  com.finderfeed.fdlib.systems.bedrock.animations.Animation
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker
 *  com.finderfeed.fdlib.util.ProjectileMovementPath
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.entities.geburah.geburah_weapons.instances;

import com.finderfeed.fdbosses.content.entities.geburah.GeburahEntity;
import com.finderfeed.fdbosses.content.entities.geburah.geburah_weapons.GeburahWeaponAttack;
import com.finderfeed.fdbosses.content.entities.geburah.judgement_ball_projectile.JudgementBallProjectile;
import com.finderfeed.fdbosses.init.BossAnims;
import com.finderfeed.fdbosses.init.BossConfigs;
import com.finderfeed.fdbosses.init.BossSounds;
import com.finderfeed.fdlib.data_structures.Pair;
import com.finderfeed.fdlib.systems.bedrock.animations.Animation;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker;
import com.finderfeed.fdlib.util.ProjectileMovementPath;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public class GeburahAttackFireDefaultProjectiles
extends GeburahWeaponAttack {
    private int projectileFlyTime;
    private float radius;
    private float speed;

    public GeburahAttackFireDefaultProjectiles(GeburahEntity geburah, float radius, int projectileFlyTime, float speed) {
        super(geburah);
        this.radius = radius;
        this.projectileFlyTime = projectileFlyTime;
        this.speed = speed;
    }

    @Override
    public void onAttackStart() {
        this.geburah.getAnimationSystem().startAnimation("cannons", AnimationTicker.builder(BossAnims.GEBURAH_FIRE_CANNONS).setSpeed(this.speed).build());
    }

    @Override
    public void tickAttack() {
        int attackTick = Math.round((float)((Animation)BossAnims.GEBURAH_FIRE_CANNONS.get()).getAnimTime() / this.speed * 0.6f);
        if (this.getCurrentTick() == attackTick) {
            this.geburah.m_9236_().m_6263_(null, this.geburah.m_20185_(), this.geburah.m_20186_(), this.geburah.m_20189_(), BossSounds.GEBURAH_CANNON_SHOOT.get(), SoundSource.HOSTILE, 4.0f, 1.0f);
            for (Pair<Vec3, Vec3> cannon : this.geburah.getCannonsPositionAndDirection()) {
                Vec3 position = ((Vec3)cannon.first).m_82520_(0.0, -0.25, 0.0);
                Vec3 direction = (Vec3)cannon.second;
                JudgementBallProjectile judgementBallProjectile = JudgementBallProjectile.summon(this.geburah.m_9236_(), new ProjectileMovementPath(this.projectileFlyTime, false).addPos(position).addPos(position.m_82549_(direction.m_82490_((double)this.radius))), BossConfigs.BOSS_CONFIG.get().geburahConfig.cannonProjectileDamage);
            }
        }
    }

    @Override
    public boolean hasEnded() {
        return (float)this.getCurrentTick() > (float)((Animation)BossAnims.GEBURAH_FIRE_CANNONS.get()).getAnimTime() / this.speed;
    }

    @Override
    public void onAttackEnd() {
    }

    @Override
    public boolean canBeChanged() {
        return false;
    }
}


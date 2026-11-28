/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.data_structures.Pair
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.phys.Vec2
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.entities.geburah.geburah_weapons.instances;

import com.finderfeed.fdbosses.BossTargetFinder;
import com.finderfeed.fdbosses.BossUtil;
import com.finderfeed.fdbosses.content.entities.geburah.GeburahEntity;
import com.finderfeed.fdbosses.content.entities.geburah.geburah_weapons.GeburahWeaponAttack;
import com.finderfeed.fdbosses.init.BossConfigs;
import com.finderfeed.fdbosses.init.BossDamageSources;
import com.finderfeed.fdbosses.init.BossSounds;
import com.finderfeed.fdlib.data_structures.Pair;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class GeburahLasersAttack
extends GeburahWeaponAttack {
    public GeburahLasersAttack(GeburahEntity geburah) {
        super(geburah);
    }

    @Override
    public void onAttackStart() {
        this.geburah.m_9236_().m_6263_(null, this.geburah.m_20185_(), this.geburah.m_20186_(), this.geburah.m_20189_(), BossSounds.GEBURAH_RAY_SHOT.get(), SoundSource.HOSTILE, 4.0f, 1.0f);
        BossUtil.geburahWeaponsStartLaser((ServerLevel)this.geburah.m_9236_(), this.geburah.m_20182_(), 120.0, this.geburah);
    }

    @Override
    public void tickAttack() {
        this.geburah.setLaserVisualsState(true);
        if (this.getCurrentTick() % 2 == 0) {
            this.geburah.m_9236_().m_6263_(null, this.geburah.m_20185_(), this.geburah.m_20186_(), this.geburah.m_20189_(), BossSounds.GEBURAH_RAY_LOOP.get(), SoundSource.HOSTILE, 4.0f, 1.0f);
        }
        List<Pair<Vec3, Vec3>> rotationOld = this.geburah.getCannonsPositionAndDirection(0.0f);
        List<Pair<Vec3, Vec3>> rotationNew = this.geburah.getCannonsPositionAndDirection(1.0f);
        for (int i = 0; i < rotationOld.size(); ++i) {
            Pair<Vec3, Vec3> oldR = rotationOld.get(i);
            Pair<Vec3, Vec3> newR = rotationNew.get(i);
            Vec3 oldDirection = (Vec3)oldR.second;
            Vec3 newDirection = (Vec3)newR.second;
            Vec3 centeredDirection = oldDirection.m_82549_(newDirection).m_82541_();
            float angle = (float)FDMathUtil.angleBetweenVectors((Vec3)oldDirection, (Vec3)newDirection);
            if (Float.isNaN(angle) || angle == 0.0f) {
                angle = 0.05f;
            }
            List<LivingEntity> targets = BossTargetFinder.getEntitiesInArc(LivingEntity.class, this.geburah.m_9236_(), this.geburah.m_20182_().m_82520_(0.0, -0.1, 0.0), new Vec2((float)centeredDirection.f_82479_, (float)centeredDirection.f_82481_), angle * 2.0f, 2.0f, 32.0f, livingEntity -> !(livingEntity instanceof GeburahEntity));
            float damage = BossUtil.transformDamage(this.geburah.m_9236_(), BossConfigs.BOSS_CONFIG.get().geburahConfig.rotatingLaserAttackDamage);
            for (LivingEntity target : targets) {
                target.m_6469_(BossDamageSources.GEBURAH_LASER_STRIKE_SOURCE, damage);
            }
        }
    }

    @Override
    public boolean hasEnded() {
        return false;
    }

    @Override
    public void onAttackEnd() {
        this.geburah.setLaserVisualsState(false);
    }

    @Override
    public boolean canBeChanged() {
        return true;
    }
}


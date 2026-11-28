/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.FDHelpers
 *  com.finderfeed.fdlib.FDLibCalls
 *  com.finderfeed.fdlib.systems.shake.FDShakeData
 *  com.finderfeed.fdlib.systems.shake.PositionedScreenShakePacket
 *  com.finderfeed.fdlib.util.FDColor
 *  com.finderfeed.fdlib.util.FDTargetFinder
 *  com.finderfeed.fdlib.util.client.particles.options.AlphaOptions
 *  net.minecraft.core.Direction
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.level.ClipContext
 *  net.minecraft.world.level.ClipContext$Block
 *  net.minecraft.world.level.ClipContext$Fluid
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.entities.geburah;

import com.finderfeed.fdbosses.BossUtil;
import com.finderfeed.fdbosses.client.BossParticles;
import com.finderfeed.fdbosses.client.particles.DecalParticleOptions;
import com.finderfeed.fdbosses.client.particles.arc_preparation_particle.ArcAttackPreparationParticleOptions;
import com.finderfeed.fdbosses.client.particles.stripe_particle.StripeParticleOptions;
import com.finderfeed.fdbosses.content.entities.geburah.GeburahEntity;
import com.finderfeed.fdbosses.content.entities.geburah.particles.geburah_ray.GeburahRayOptions;
import com.finderfeed.fdbosses.content.util.HorizontalCircleRandomDirections;
import com.finderfeed.fdbosses.init.BossConfigs;
import com.finderfeed.fdbosses.init.BossDamageSources;
import com.finderfeed.fdbosses.init.BossSounds;
import com.finderfeed.fdlib.FDHelpers;
import com.finderfeed.fdlib.FDLibCalls;
import com.finderfeed.fdlib.systems.shake.FDShakeData;
import com.finderfeed.fdlib.systems.shake.PositionedScreenShakePacket;
import com.finderfeed.fdlib.util.FDColor;
import com.finderfeed.fdlib.util.FDTargetFinder;
import com.finderfeed.fdlib.util.client.particles.options.AlphaOptions;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class GeburahRayController {
    private List<Vec3> targets = new ArrayList<Vec3>();
    private int currentShotCharge = -1;
    private float damageRadius = 1.0f;
    private GeburahEntity geburah;

    public GeburahRayController(GeburahEntity geburah) {
        this.geburah = geburah;
    }

    public void tick() {
        if (this.geburah.m_9236_().f_46443_) {
            return;
        }
        Level level = this.geburah.m_9236_();
        if (this.currentShotCharge > 0) {
            if (this.currentShotCharge >= 10) {
                if (this.currentShotCharge % 2 == 0) {
                    Vec3 pos = this.geburah.getCorePosition();
                    StripeParticleOptions stripeParticleOptions = StripeParticleOptions.createHorizontalCircling(new FDColor(1.0f, 0.3f, 0.2f, 1.0f), new FDColor(1.0f, 0.6f, 0.2f, 1.0f), new Vec3(0.0, 1.0, 0.0), (float)(this.currentShotCharge / 2) * 1.5707964f, 0.3f, 10, 100, 0.0f, 10.0f, 0.5f, 0.5f, true, true);
                    FDLibCalls.sendParticles((ServerLevel)((ServerLevel)level), (ParticleOptions)stripeParticleOptions, (Vec3)pos, (double)200.0);
                }
                BossUtil.geburahRayChargeParticles((ServerLevel)level, this.geburah.getCorePosition(), 100.0, this.geburah);
            }
            this.currentShotCharge = Mth.m_14045_((int)(this.currentShotCharge - 1), (int)0, (int)Integer.MAX_VALUE);
        } else if (this.currentShotCharge == 0) {
            if (!this.targets.isEmpty()) {
                float damage = BossUtil.transformDamage(this.geburah.m_9236_(), BossConfigs.BOSS_CONFIG.get().geburahConfig.coreRayStrikeDamage);
                for (Vec3 target : this.targets) {
                    this.fireRayAtPos(target, damage, this.damageRadius, false);
                }
                Vec3 start = this.geburah.getCorePosition();
                this.geburah.m_9236_().m_6263_(null, start.f_82479_, start.f_82480_, start.f_82481_, BossSounds.GEBURAH_CORE_RAY_STRIKE.get(), SoundSource.HOSTILE, 10.0f, 1.0f);
            }
            this.currentShotCharge = -1;
            this.targets.clear();
        }
    }

    public void fireRayAtPos(Vec3 target, float damage, float damageRadius, boolean playSound) {
        Vec3 start = this.geburah.getCorePosition();
        if (playSound) {
            this.geburah.m_9236_().m_6263_(null, start.f_82479_, start.f_82480_, start.f_82481_, BossSounds.GEBURAH_CORE_RAY_STRIKE.get(), SoundSource.HOSTILE, 5.0f, 1.0f);
        }
        Level level = this.geburah.m_9236_();
        Vec3 between = target.m_82546_(start).m_82541_();
        Vec3 end = target.m_82549_(between.m_82549_(between.m_82541_().m_82490_(10.0)));
        between = between.m_82541_();
        ClipContext clipContext = new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null);
        BlockHitResult result = level.m_45547_(clipContext);
        Vec3 location = result.m_82450_();
        ArrayList<LivingEntity> targets = new ArrayList<LivingEntity>();
        targets.addAll(FDTargetFinder.getEntitiesInSphere(LivingEntity.class, (Level)level, (Vec3)target, (float)damageRadius));
        targets.addAll(FDHelpers.traceEntities((Level)level, (Vec3)start, (Vec3)location, (double)0.1f, r -> r instanceof LivingEntity).stream().map(v -> (LivingEntity)v).toList());
        for (LivingEntity living : targets) {
            living.m_6469_(BossDamageSources.GEBURAH_RAY_STRIKE_SOURCE, damage);
        }
        GeburahRayOptions options = GeburahRayOptions.builder().end(location).color(1.0f, 0.8f, 0.2f).time(0, 2, 7).width(0.5f).build();
        FDLibCalls.sendParticles((ServerLevel)((ServerLevel)level), (ParticleOptions)options, (Vec3)start, (double)200.0);
        if (result.m_6662_() != HitResult.Type.MISS) {
            BossUtil.createOnEarthBlockExplosionEffect(level, result.m_82450_(), between, 10, Blocks.f_50069_.m_49966_());
            Direction direction = result.m_82434_();
            DecalParticleOptions decal = new DecalParticleOptions(BossParticles.GEBURAH_RAY_DECAL.get(), new Vec3((double)direction.m_122429_(), (double)direction.m_122430_(), (double)direction.m_122431_()), AlphaOptions.builder().stay(30).out(10).build(), 4.0f);
            FDLibCalls.sendParticles((ServerLevel)((ServerLevel)this.geburah.m_9236_()), (ParticleOptions)decal, (Vec3)result.m_82450_().m_82520_((double)((float)direction.m_122429_() * 0.03f), (double)((float)direction.m_122430_() * 0.03f), (double)((float)direction.m_122431_() * 0.03f)), (double)120.0);
        }
        BossUtil.geburahRayParticles((ServerLevel)level, result.m_82450_(), 200.0, between.m_82548_());
        PositionedScreenShakePacket.send((ServerLevel)((ServerLevel)level), (FDShakeData)FDShakeData.builder().amplitude(1.0f).stayTime(5).outTime(5).frequency(40.0f).build(), (Vec3)result.m_82450_(), (double)120.0);
    }

    public void shoot(int shootTime, float damageRadius, boolean preparationParticles, List<Vec3> targets) {
        if (targets.isEmpty()) {
            return;
        }
        if (this.currentShotCharge == -1) {
            this.currentShotCharge = shootTime;
        }
        this.damageRadius = damageRadius;
        this.targets.clear();
        this.targets.addAll(targets);
        if (preparationParticles) {
            for (Vec3 pos : targets) {
                for (Vec3 dir : new HorizontalCircleRandomDirections(this.geburah.m_217043_(), 4, 0.0f)) {
                    ArcAttackPreparationParticleOptions options = new ArcAttackPreparationParticleOptions(dir, damageRadius, 0.7853982f, shootTime, Math.round((float)shootTime * 0.25f), 10, 1.0f, 0.8f, 0.2f, 0.2f);
                    FDLibCalls.sendParticles((ServerLevel)((ServerLevel)this.geburah.m_9236_()), (ParticleOptions)options, (Vec3)pos.m_82520_(0.0, (double)0.05f, 0.0), (double)120.0);
                }
            }
        }
    }

    public int getCurrentShotCharge() {
        return this.currentShotCharge;
    }
}


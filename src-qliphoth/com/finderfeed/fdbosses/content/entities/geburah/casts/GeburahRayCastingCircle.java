/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.FDHelpers
 *  com.finderfeed.fdlib.FDLibCalls
 *  com.finderfeed.fdlib.systems.shake.FDShakeData
 *  com.finderfeed.fdlib.systems.shake.PositionedScreenShakePacket
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.level.ClipContext
 *  net.minecraft.world.level.ClipContext$Block
 *  net.minecraft.world.level.ClipContext$Fluid
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.entities.geburah.casts;

import com.finderfeed.fdbosses.BossUtil;
import com.finderfeed.fdbosses.content.entities.geburah.GeburahBossBuddy;
import com.finderfeed.fdbosses.content.entities.geburah.casts.GeburahCastingCircle;
import com.finderfeed.fdbosses.content.entities.geburah.particles.geburah_ray.GeburahRayOptions;
import com.finderfeed.fdbosses.init.BossConfigs;
import com.finderfeed.fdbosses.init.BossDamageSources;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdbosses.init.BossSounds;
import com.finderfeed.fdlib.FDHelpers;
import com.finderfeed.fdlib.FDLibCalls;
import com.finderfeed.fdlib.systems.shake.FDShakeData;
import com.finderfeed.fdlib.systems.shake.PositionedScreenShakePacket;
import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class GeburahRayCastingCircle
extends GeburahCastingCircle {
    public static GeburahRayCastingCircle summon(Level level, Vec3 pos, Vec3 dir) {
        GeburahRayCastingCircle castCircle = new GeburahRayCastingCircle(BossEntities.GEBURAH_CASTING_CIRCLE_RAY.get(), level);
        castCircle.m_146884_(pos);
        castCircle.setDirection(dir);
        castCircle.setCastDuration(0);
        castCircle.m_20088_().m_135381_(COLOR, (Object)-14483457);
        level.m_7967_((Entity)castCircle);
        return castCircle;
    }

    public GeburahRayCastingCircle(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void m_8119_() {
        super.m_8119_();
    }

    @Override
    public void cast() {
        Vec3 end = this.m_20182_().m_82549_(this.getCastDirection().m_82490_(20.0));
        ClipContext clipContext = new ClipContext(this.m_20182_(), end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null);
        BlockHitResult res = this.m_9236_().m_45547_(clipContext);
        end = res.m_82450_();
        this.m_9236_().m_6263_(null, end.f_82479_, end.f_82480_, end.f_82481_, BossSounds.GEBURAH_RAY_SHOT.get(), SoundSource.HOSTILE, 2.0f, 1.0f);
        List entities = FDHelpers.traceEntities((Level)this.m_9236_(), (Vec3)this.m_20182_(), (Vec3)end, (double)0.0, e -> {
            if (!(e instanceof LivingEntity)) return false;
            LivingEntity livingEntity = (LivingEntity)e;
            if (e instanceof GeburahBossBuddy) return false;
            return true;
        });
        float damage = BossUtil.transformDamage(this.m_9236_(), BossConfigs.BOSS_CONFIG.get().geburahConfig.judgementBirdDamage);
        for (Entity entity : entities) {
            entity.m_6469_(BossDamageSources.GEBURAH_LASER_STRIKE_SOURCE, damage);
        }
        PositionedScreenShakePacket.send((ServerLevel)((ServerLevel)this.m_9236_()), (FDShakeData)FDShakeData.builder().amplitude(1.0f).outTime(5).frequency(20.0f).build(), (Vec3)end, (double)40.0);
        GeburahRayOptions options = GeburahRayOptions.builder().width(0.25f).color(0.3f, 0.8f, 1.0f, 1.0f).end(end).stay(2).out(3).build();
        FDLibCalls.sendParticles((ServerLevel)((ServerLevel)this.m_9236_()), (ParticleOptions)options, (Vec3)this.m_20182_(), (double)120.0);
        if (res.m_6662_() != HitResult.Type.MISS) {
            BossUtil.judgementBirdRayParticles((ServerLevel)this.m_9236_(), end, 100.0, this.m_20182_().m_82546_(end).m_82541_());
        }
    }
}


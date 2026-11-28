/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDEntity
 *  com.finderfeed.fdlib.systems.trails.FDTrailDataGenerator
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.level.ClipContext
 *  net.minecraft.world.level.ClipContext$Block
 *  net.minecraft.world.level.ClipContext$Fluid
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.entities.geburah.chain_trap;

import com.finderfeed.fdbosses.BossUtil;
import com.finderfeed.fdbosses.content.entities.geburah.chain_trap.GeburahChainTrapEntity;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDEntity;
import com.finderfeed.fdlib.systems.trails.FDTrailDataGenerator;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class ChainTrapSummonProjectile
extends FDEntity {
    public FDTrailDataGenerator<ChainTrapSummonProjectile> trail = new FDTrailDataGenerator(Entity::m_20318_, 3, 0.01f);
    public static final double GRAVITY = 0.08;

    public static ChainTrapSummonProjectile summon(Level level, Vec3 pos, Vec3 targetPos) {
        ChainTrapSummonProjectile projectile = new ChainTrapSummonProjectile(BossEntities.GEBURAH_CHAIN_TRAP_SUMMON_PROJECTILE.get(), level);
        Vec3 velocity = BossUtil.calculateMortarProjectileVelocity(pos, targetPos, -0.08, 20);
        projectile.m_146884_(pos);
        projectile.m_20256_(velocity);
        level.m_7967_((Entity)projectile);
        return projectile;
    }

    public ChainTrapSummonProjectile(EntityType<?> type, Level level) {
        super(type, level);
    }

    public void m_8119_() {
        super.m_8119_();
        if (!this.m_9236_().f_46443_) {
            if (this.f_19797_ > 2000) {
                this.m_142467_(Entity.RemovalReason.DISCARDED);
            }
            this.tickAndSummonTrap();
        } else {
            this.trail.tick((Object)this);
        }
        this.m_146884_(this.m_20182_().m_82549_(this.m_20184_()));
        BossUtil.applyGravity((Entity)this, this.getDefaultGravity());
    }

    private void tickAndSummonTrap() {
        ClipContext clipContext = new ClipContext(this.m_20182_(), this.m_20182_().m_82549_(this.m_20184_()), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null);
        BlockHitResult result = this.m_9236_().m_45547_(clipContext);
        if (result.m_6662_() != HitResult.Type.MISS) {
            Vec3 location = result.m_82450_();
            GeburahChainTrapEntity trapEntity = new GeburahChainTrapEntity(BossEntities.GEBURAH_CHAIN_TRAP.get(), this.m_9236_());
            float fraction = (float)(location.f_82480_ - (double)((int)location.f_82480_));
            Vec3 pos = location.m_82520_(0.0, (double)0.01f, 0.0);
            if ((double)fraction > 0.25) {
                pos = new Vec3(pos.f_82479_, Math.ceil(pos.f_82480_), pos.f_82481_);
            }
            trapEntity.m_146884_(pos);
            this.m_9236_().m_7967_((Entity)trapEntity);
            this.m_142467_(Entity.RemovalReason.DISCARDED);
        }
    }

    protected double getDefaultGravity() {
        return 0.08;
    }

    protected void m_8097_() {
    }

    public boolean m_6087_() {
        return true;
    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.util.ProjectileMovementPath
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.EntityDataSerializers
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Vector3f
 */
package com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_repair_crystal;

import com.finderfeed.fdbosses.BossTargetFinder;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthAttackType;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthEntity;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_cannon.MalkuthCannonEntity;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdbosses.init.BossEntityDataSerializers;
import com.finderfeed.fdlib.util.ProjectileMovementPath;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class MalkuthRepairEntity
extends Entity {
    public static final EntityDataAccessor<Integer> RANDOMIZED_DIRECTION = SynchedEntityData.m_135353_(MalkuthRepairEntity.class, (EntityDataSerializer)EntityDataSerializers.f_135028_);
    public static final EntityDataAccessor<MalkuthAttackType> ATTACK_TYPE = SynchedEntityData.m_135353_(MalkuthRepairEntity.class, (EntityDataSerializer)((EntityDataSerializer)BossEntityDataSerializers.MALKUTH_ATTACK_TYPE.get()));
    private ProjectileMovementPath movementPath;
    private MalkuthCannonEntity flyingTo = null;

    public static MalkuthRepairEntity summon(Level level, Vec3 pos, MalkuthAttackType repairEntityType) {
        MalkuthRepairEntity repairEntity = new MalkuthRepairEntity(BossEntities.MALKUTH_REPAIR_ENTITY.get(), level);
        repairEntity.m_146884_(pos);
        repairEntity.f_19804_.m_135381_(ATTACK_TYPE, (Object)repairEntityType);
        level.m_7967_((Entity)repairEntity);
        return repairEntity;
    }

    public MalkuthRepairEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public void m_8119_() {
        super.m_8119_();
        this.m_146884_(this.m_20182_().m_82549_(this.m_20184_()));
        if (!this.m_9236_().f_46443_) {
            this.tickMovementPath();
        } else {
            Vector3f color = MalkuthEntity.getAndRandomizeColor((MalkuthAttackType)((Object)this.f_19804_.m_135370_(ATTACK_TYPE)), this.m_9236_().f_46441_);
            BallParticleOptions options = BallParticleOptions.builder().color(color.x, color.y, color.z).brightness(2).scalingOptions(0, 0, 10).size(0.15f).build();
            this.m_9236_().m_6493_((ParticleOptions)options, true, this.f_19854_, this.f_19855_, this.f_19856_, 0.0, 0.0, 0.0);
        }
    }

    private void tickMovementPath() {
        if (this.movementPath == null && !this.initMovementPath()) {
            this.m_142687_(Entity.RemovalReason.DISCARDED);
            return;
        }
        if (!this.movementPath.isFinished()) {
            this.movementPath.tick((Entity)this);
        } else {
            this.flyingTo.repairWithMaterial();
            this.m_142687_(Entity.RemovalReason.DISCARDED);
        }
    }

    private boolean initMovementPath() {
        MalkuthAttackType type = (MalkuthAttackType)((Object)this.f_19804_.m_135370_(ATTACK_TYPE));
        List<MalkuthCannonEntity> cannons = BossTargetFinder.getEntitiesInSphere(MalkuthCannonEntity.class, this.m_9236_(), this.m_20182_(), 30.0f, cannon -> cannon.getCannonType().equals((Object)type) && cannon.isPlayerControlled());
        if (cannons.isEmpty()) {
            return false;
        }
        this.flyingTo = cannons.get(0);
        Vec3 startPos = this.m_20182_();
        Vec3 endPos = this.flyingTo.m_20182_().m_82520_(0.0, 1.0, 0.0);
        Vec3 b = endPos.m_82546_(startPos);
        Vec3 left = b.m_82537_(new Vec3(0.0, 1.0, 0.0)).m_82541_();
        if (this.f_19796_.m_188501_() > 0.5f) {
            left = left.m_82548_();
            this.f_19804_.m_135381_(RANDOMIZED_DIRECTION, (Object)-1);
        }
        Vec3 bp1 = startPos.m_82549_(b.m_82542_(0.25, 0.25, 0.25)).m_82549_(left.m_82542_(1.5, 1.5, 1.5));
        Vec3 bp2 = startPos.m_82549_(b.m_82542_(0.5, 0.5, 0.5)).m_82549_(left.m_82542_(2.0, 2.0, 2.0));
        Vec3 bp3 = startPos.m_82549_(b.m_82542_(0.75, 0.75, 0.75)).m_82549_(left.m_82542_(1.5, 1.5, 1.5));
        List<Vector3f> positions = new ArrayList<Vec3>(List.of(startPos, bp1, bp2, bp3, endPos)).stream().map(Vec3::m_252839_).toList();
        this.movementPath = new ProjectileMovementPath(20, false);
        int segments = 20;
        for (int k = 0; k < segments; ++k) {
            float p = (float)k / ((float)segments - 1.0f);
            Vector3f p1 = FDMathUtil.catmullRom(positions, (float)p);
            this.movementPath.addPos(new Vec3(p1));
        }
        return true;
    }

    protected void m_8097_() {
        this.f_19804_.m_135372_(ATTACK_TYPE, (Object)MalkuthAttackType.FIRE);
        this.f_19804_.m_135372_(RANDOMIZED_DIRECTION, (Object)1);
    }

    protected void m_7378_(CompoundTag tag) {
        if (tag.m_128441_("attackType")) {
            this.f_19804_.m_135381_(ATTACK_TYPE, (Object)MalkuthAttackType.valueOf(tag.m_128461_("attackType")));
        }
    }

    protected void m_7380_(CompoundTag tag) {
        tag.m_128359_("attackType", ((MalkuthAttackType)((Object)this.f_19804_.m_135370_(ATTACK_TYPE))).name());
    }

    public boolean m_6783_(double p_19883_) {
        return true;
    }

    public boolean m_6000_(double p_20296_, double p_20297_, double p_20298_) {
        return super.m_6000_(p_20296_, p_20297_, p_20298_);
    }

    public boolean m_142038_() {
        return true;
    }

    public int m_19876_() {
        return ((MalkuthAttackType)((Object)this.f_19804_.m_135370_(ATTACK_TYPE))).isFire() ? 0xFFBB11 : 0x11AAFF;
    }
}


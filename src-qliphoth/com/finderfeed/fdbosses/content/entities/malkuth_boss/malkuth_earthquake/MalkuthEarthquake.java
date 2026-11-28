/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.FDLibCalls
 *  com.finderfeed.fdlib.data_structures.Pair
 *  com.finderfeed.fdlib.init.FDEDataSerializers
 *  com.finderfeed.fdlib.nbt.AutoSerializable
 *  com.finderfeed.fdlib.nbt.SerializableField
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleTypes
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.EntityDataSerializers
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.damagesource.DamageSource
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec2
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_earthquake;

import com.finderfeed.fdbosses.BossTargetFinder;
import com.finderfeed.fdbosses.client.BossParticles;
import com.finderfeed.fdbosses.client.particles.GravityParticleOptions;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthAttackType;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthBossBuddy;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthDamageSource;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthWeaknessHandler;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_earthquake.MalkuthEarthquakeSegment;
import com.finderfeed.fdbosses.init.BossDamageSources;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdbosses.init.BossEntityDataSerializers;
import com.finderfeed.fdbosses.init.BossSounds;
import com.finderfeed.fdlib.FDLibCalls;
import com.finderfeed.fdlib.data_structures.Pair;
import com.finderfeed.fdlib.init.FDEDataSerializers;
import com.finderfeed.fdlib.nbt.AutoSerializable;
import com.finderfeed.fdlib.nbt.SerializableField;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class MalkuthEarthquake
extends Entity
implements AutoSerializable {
    public static final EntityDataAccessor<MalkuthAttackType> MALKUTH_ATTACK_TYPE = SynchedEntityData.m_135353_(MalkuthEarthquake.class, (EntityDataSerializer)((EntityDataSerializer)BossEntityDataSerializers.MALKUTH_ATTACK_TYPE.get()));
    public static final EntityDataAccessor<Float> ANGLE = SynchedEntityData.m_135353_(MalkuthEarthquake.class, (EntityDataSerializer)EntityDataSerializers.f_135029_);
    public static final EntityDataAccessor<Vec3> DIRECTION = SynchedEntityData.m_135353_(MalkuthEarthquake.class, (EntityDataSerializer)((EntityDataSerializer)FDEDataSerializers.VEC3.get()));
    public static final EntityDataAccessor<Integer> TIME = SynchedEntityData.m_135353_(MalkuthEarthquake.class, (EntityDataSerializer)EntityDataSerializers.f_135028_);
    @SerializableField
    private float damage;
    private List<MalkuthEarthquakeSegment> segments = new ArrayList<MalkuthEarthquakeSegment>();

    public static MalkuthEarthquake summon(Level level, MalkuthAttackType malkuthAttackType, Vec3 pos, Vec3 directionAndLength, int time, float arcAngle, float damage) {
        MalkuthEarthquake malkuthEarthquake = new MalkuthEarthquake(BossEntities.MALKUTH_EARTHQUAKE.get(), level);
        malkuthEarthquake.setEarthquakeTime(time);
        malkuthEarthquake.setDirectionAndLength(directionAndLength);
        malkuthEarthquake.setAngle(arcAngle);
        malkuthEarthquake.setMalkuthAttackType(malkuthAttackType);
        malkuthEarthquake.m_146884_(pos);
        malkuthEarthquake.damage = damage;
        level.m_7967_((Entity)malkuthEarthquake);
        return malkuthEarthquake;
    }

    public MalkuthEarthquake(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    public void m_8119_() {
        super.m_8119_();
        if (this.m_9236_().f_46443_) {
            this.summonSegments();
            this.manageSegments();
        } else {
            this.tickDamage();
            if (this.f_19797_ > this.getEarthquakeTime() + 20) {
                this.m_142687_(Entity.RemovalReason.DISCARDED);
            }
            Pair<Float, Float> damageRadiuses = this.getCurrentDamageRadius();
            float v = FDMathUtil.lerp((float)((Float)damageRadiuses.first).floatValue(), (float)((Float)damageRadiuses.second).floatValue(), (float)0.5f);
            Vec3 vec = this.getDirectionAndLength().m_82541_().m_82542_((double)v, (double)v, (double)v).m_82549_(this.m_20182_());
            int tick = this.f_19797_ - 1;
            if (tick % 5 == 0) {
                this.m_9236_().m_6263_(null, vec.f_82479_, vec.f_82480_, vec.f_82481_, BossSounds.MALKUTH_EARTHQUAKE_ROLLING.get(), SoundSource.HOSTILE, 1.0f, 0.75f);
            }
            if (tick % 3 == 0 && this.getEarthquakeType().isIce()) {
                this.m_9236_().m_6263_(null, vec.f_82479_, vec.f_82480_, vec.f_82481_, BossSounds.MALKUTH_EARTHQUAKE_SPIKE.get(), SoundSource.HOSTILE, 1.0f, 1.1f);
            }
        }
    }

    private void tickDamage() {
        Pair<Float, Float> radius = this.getCurrentDamageRadius();
        Vec3 dirAndLength = this.getDirectionAndLength();
        float length = (float)dirAndLength.m_82553_();
        Vec2 dir = new Vec2((float)dirAndLength.f_82479_, (float)dirAndLength.f_82481_);
        List<LivingEntity> entities = BossTargetFinder.getEntitiesInArc(LivingEntity.class, this.m_9236_(), this.m_20182_().m_82520_(0.0, -1.0, 0.0), dir, this.getAngle(), 3.0f, length, livingEntity -> !(livingEntity instanceof MalkuthBossBuddy));
        List<LivingEntity> entities2 = BossTargetFinder.getEntitiesInCylinder(LivingEntity.class, this.m_9236_(), this.m_20182_().m_82520_(0.0, -1.0, 0.0), 3.0f, 1.0f, livingEntity -> !(livingEntity instanceof MalkuthBossBuddy));
        entities.addAll(entities2);
        for (LivingEntity entity : entities) {
            ServerPlayer serverPlayer;
            Vec3 position = entity.m_20182_();
            Vec3 b = position.m_82546_(this.m_20182_()).m_82542_(1.0, 0.0, 1.0);
            double k = b.m_82553_();
            if (k < (double)((Float)radius.first).floatValue() || k > (double)((Float)radius.second).floatValue()) continue;
            Vec3 speed = new Vec3((double)dir.f_82470_, 0.0, (double)dir.f_82471_).m_82541_().m_82520_(0.0, 1.2, 0.0);
            boolean setPlayerSpeed = false;
            if (entity instanceof ServerPlayer) {
                serverPlayer = (ServerPlayer)entity;
                if (MalkuthWeaknessHandler.isWeakTo((Player)serverPlayer, this.getEarthquakeType())) {
                    setPlayerSpeed = true;
                }
            } else {
                entity.m_20256_(speed);
            }
            entity.m_6469_((DamageSource)new MalkuthDamageSource(BossDamageSources.MALKUTH_IMPALING_DOOM_SOURCE, this.getEarthquakeType(), 51), this.damage);
            if (!(entity instanceof ServerPlayer)) continue;
            serverPlayer = (ServerPlayer)entity;
            if (!setPlayerSpeed) continue;
            FDLibCalls.setServerPlayerSpeed((ServerPlayer)serverPlayer, (Vec3)speed);
        }
    }

    public List<MalkuthEarthquakeSegment> getSegments() {
        return this.segments;
    }

    private Pair<Float, Float> getCurrentDamageRadius() {
        float fullLength = (float)this.getDirectionAndLength().m_82553_();
        float currentCompletionPercent = (float)(this.f_19797_ - 1) / (float)this.getEarthquakeTime();
        float currentLength = fullLength * currentCompletionPercent;
        float sizeOfDamageArea = this.getSizeOfDamageArea();
        return new Pair((Object)Float.valueOf(currentLength), (Object)Float.valueOf(currentLength + sizeOfDamageArea));
    }

    private float getSizeOfDamageArea() {
        float fullLength = (float)this.getDirectionAndLength().m_82553_();
        return fullLength / (float)this.getEarthquakeTime();
    }

    private void summonSegments() {
        if (this.f_19797_ > this.getEarthquakeTime()) {
            return;
        }
        Pair<Float, Float> pair = this.getCurrentDamageRadius();
        Vec3 direction = this.getDirectionAndLength();
        float angle = this.getAngle();
        float start = ((Float)pair.first).floatValue();
        float end = ((Float)pair.second).floatValue();
        float sizeOfSegment = 1.25f;
        Vec3 ndir = direction.m_82541_();
        boolean reverse = false;
        for (float i = start + 0.01f; i <= end; i += sizeOfSegment) {
            float lengthOfCircle = (float)Math.PI * 2 * i;
            float anglePercent = sizeOfSegment / lengthOfCircle;
            float sizeInAngle = anglePercent * (float)Math.PI * 2.0f;
            for (float b = this.f_19796_.m_188501_() * sizeInAngle / 2.0f; b <= angle; b += sizeInAngle) {
                float rot = reverse ? angle - b : b;
                float f = i + sizeOfSegment * this.f_19796_.m_188501_() / 2.0f;
                Vec3 offset = ndir.m_82542_((double)f, (double)f, (double)f).m_82524_(rot - angle / 2.0f);
                MalkuthEarthquakeSegment.Type segmentType = this.getRandomSegmentType(this.f_19796_.m_188503_(2));
                float segmentAngle = 0.7853982f - this.f_19796_.m_188501_() * (float)Math.PI / 6.0f;
                int segmentTime = 10;
                MalkuthEarthquakeSegment malkuthEarthquakeSegment = new MalkuthEarthquakeSegment(segmentType, offset, segmentAngle, segmentTime);
                this.segments.add(malkuthEarthquakeSegment);
                if (this.getEarthquakeType().isIce() && this.f_19796_.m_188501_() > 0.6f) {
                    float additionalAngle = (float)(reverse ? -1 : 1) * this.f_19796_.m_188501_() * sizeInAngle;
                    offset = ndir.m_82524_(rot - angle / 2.0f + additionalAngle).m_82542_((double)i, (double)i, (double)i);
                    MalkuthEarthquakeSegment s = new MalkuthEarthquakeSegment(MalkuthEarthquakeSegment.Type.ICE_SPIKE, offset, this.f_19796_.m_188501_() * (float)Math.PI / 32.0f, segmentTime);
                    this.segments.add(s);
                }
                this.segments.sort(Comparator.comparingInt(v -> v.getType().getId()));
                for (int k = 0; k < 1; ++k) {
                    Object options;
                    float hspeed = 0.4f;
                    if (this.getEarthquakeType().isFire()) {
                        float rnd = this.f_19796_.m_188501_();
                        if ((double)rnd > 0.66) {
                            options = new GravityParticleOptions(BossParticles.FLAME_WITH_STONE.get(), 20 + this.f_19796_.m_188503_(4), 0.15f + this.f_19796_.m_188501_() * 0.2f, 1.5999999f, 2.0f, true);
                        } else if ((double)rnd > 0.33) {
                            float rc = 0.9f + this.f_19796_.m_188501_() * 0.1f;
                            float gc = 0.2f + this.f_19796_.m_188501_() * 0.2f;
                            float bc = this.f_19796_.m_188501_() * 0.2f;
                            options = BallParticleOptions.builder().color(rc, gc, bc).size(0.2f + this.f_19796_.m_188501_() * 0.2f).scalingOptions(0, 0, 20).friction(0.7f).build();
                            hspeed = 0.75f;
                        } else {
                            options = ParticleTypes.f_123756_;
                        }
                    } else if ((double)this.f_19796_.m_188501_() > 0.5) {
                        options = new GravityParticleOptions(BossParticles.ICE_CHUNK.get(), 20 + this.f_19796_.m_188503_(4), 0.5f + this.f_19796_.m_188501_() * 0.2f, 1.5999999f, 2.0f, true);
                    } else {
                        float rc = this.f_19796_.m_188501_() * 0.2f;
                        float gc = 0.7f + this.f_19796_.m_188501_() * 0.1f;
                        float bc = 0.9f + this.f_19796_.m_188501_() * 0.1f;
                        options = BallParticleOptions.builder().color(rc, gc, bc).size(0.2f + this.f_19796_.m_188501_() * 0.2f).scalingOptions(0, 0, 20).friction(0.7f).build();
                        hspeed = 0.75f;
                    }
                    Vec3 pos = offset.m_82549_(this.m_20182_()).m_82520_((double)this.f_19796_.m_188501_() * 0.8 - 0.4, (double)this.f_19796_.m_188501_() * 0.8 - 0.4, (double)this.f_19796_.m_188501_() * 0.8 - 0.4);
                    this.m_9236_().m_6493_((ParticleOptions)options, true, pos.f_82479_, pos.f_82480_, pos.f_82481_, ndir.f_82479_ * (double)hspeed, (double)this.f_19796_.m_188501_() * 0.75 + 0.1, ndir.f_82481_ * (double)hspeed);
                }
            }
            reverse = true;
        }
    }

    private MalkuthEarthquakeSegment.Type getRandomSegmentType(int segmentId) {
        if (this.getEarthquakeType().isFire()) {
            if (segmentId == 0) {
                return MalkuthEarthquakeSegment.Type.FIRE_1;
            }
            return MalkuthEarthquakeSegment.Type.FIRE_2;
        }
        if (segmentId == 0) {
            return MalkuthEarthquakeSegment.Type.ICE_1;
        }
        return MalkuthEarthquakeSegment.Type.ICE_2;
    }

    private void manageSegments() {
        Iterator<MalkuthEarthquakeSegment> iter = this.segments.iterator();
        while (iter.hasNext()) {
            MalkuthEarthquakeSegment segment = iter.next();
            if (segment.hasFinished()) {
                iter.remove();
                continue;
            }
            segment.tick();
        }
    }

    public int getEarthquakeTime() {
        return (Integer)this.f_19804_.m_135370_(TIME);
    }

    public void setEarthquakeTime(int time) {
        this.f_19804_.m_135381_(TIME, (Object)time);
    }

    public void setMalkuthAttackType(MalkuthAttackType malkuthAttackType) {
        this.f_19804_.m_135381_(MALKUTH_ATTACK_TYPE, (Object)malkuthAttackType);
    }

    public void setDirectionAndLength(Vec3 direction) {
        this.f_19804_.m_135381_(DIRECTION, (Object)direction);
    }

    public Vec3 getDirectionAndLength() {
        return (Vec3)this.f_19804_.m_135370_(DIRECTION);
    }

    public MalkuthAttackType getEarthquakeType() {
        return (MalkuthAttackType)((Object)this.f_19804_.m_135370_(MALKUTH_ATTACK_TYPE));
    }

    public void setAngle(float angle) {
        this.f_19804_.m_135381_(ANGLE, (Object)Float.valueOf(angle));
    }

    public float getAngle() {
        return ((Float)this.f_19804_.m_135370_(ANGLE)).floatValue();
    }

    protected void m_8097_() {
        this.f_19804_.m_135372_(MALKUTH_ATTACK_TYPE, (Object)MalkuthAttackType.FIRE);
        this.f_19804_.m_135372_(DIRECTION, (Object)new Vec3(1.0, 0.0, 0.0));
        this.f_19804_.m_135372_(TIME, (Object)20);
        this.f_19804_.m_135372_(ANGLE, (Object)Float.valueOf(0.5235988f));
    }

    protected void m_7378_(CompoundTag tag) {
        this.autoLoad(tag);
        this.f_19804_.m_135381_(ANGLE, (Object)Float.valueOf(tag.m_128457_("angle")));
        this.f_19804_.m_135381_(DIRECTION, (Object)new Vec3(tag.m_128459_("xdir"), 0.0, tag.m_128459_("zdir")));
        this.f_19804_.m_135381_(MALKUTH_ATTACK_TYPE, (Object)MalkuthAttackType.valueOf(tag.m_128461_("earthquake_type")));
        this.f_19804_.m_135381_(TIME, (Object)tag.m_128451_("time"));
    }

    protected void m_7380_(CompoundTag tag) {
        this.autoSave(tag);
        tag.m_128350_("angle", ((Float)this.f_19804_.m_135370_(ANGLE)).floatValue());
        tag.m_128347_("xdir", ((Vec3)this.f_19804_.m_135370_(MalkuthEarthquake.DIRECTION)).f_82479_);
        tag.m_128347_("zdir", ((Vec3)this.f_19804_.m_135370_(MalkuthEarthquake.DIRECTION)).f_82481_);
        tag.m_128359_("earthquake_type", ((MalkuthAttackType)((Object)this.f_19804_.m_135370_(MALKUTH_ATTACK_TYPE))).name());
        tag.m_128405_("time", ((Integer)this.f_19804_.m_135370_(TIME)).intValue());
    }

    public boolean m_6000_(double p_20296_, double p_20297_, double p_20298_) {
        return true;
    }

    public boolean m_6783_(double p_19883_) {
        return true;
    }
}


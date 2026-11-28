/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.nbt.AutoSerializable
 *  com.finderfeed.fdlib.nbt.SerializableField
 *  com.finderfeed.fdlib.network.FDPacketHandler
 *  com.finderfeed.fdlib.systems.shake.FDShakeData
 *  com.finderfeed.fdlib.systems.shake.PositionedScreenShakePacket
 *  com.finderfeed.fdlib.util.FDProjectile
 *  com.finderfeed.fdlib.util.ProjectileMovementPath
 *  com.finderfeed.fdlib.util.client.particles.FDBlockParticleOptions
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.HolderGetter
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.NbtUtils
 *  net.minecraft.nbt.Tag
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.EntityDataSerializers
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.projectile.AbstractHurtingProjectile
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.network.PacketDistributor
 *  org.joml.AxisAngle4f
 *  org.joml.Math
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 *  org.joml.Vector3f
 */
package com.finderfeed.fdbosses.content.projectiles;

import com.finderfeed.fdbosses.content.entities.chesed_boss.ChesedBossBuddy;
import com.finderfeed.fdbosses.content.entities.chesed_boss.earthshatter_entity.EarthShatterEntity;
import com.finderfeed.fdbosses.content.entities.chesed_boss.earthshatter_entity.EarthShatterSettings;
import com.finderfeed.fdbosses.content.entities.chesed_boss.flying_block_entity.FlyingBlockEntity;
import com.finderfeed.fdbosses.init.BossDamageSources;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdbosses.init.BossSounds;
import com.finderfeed.fdbosses.packets.SlamParticlesPacket;
import com.finderfeed.fdlib.nbt.AutoSerializable;
import com.finderfeed.fdlib.nbt.SerializableField;
import com.finderfeed.fdlib.network.FDPacketHandler;
import com.finderfeed.fdlib.systems.shake.FDShakeData;
import com.finderfeed.fdlib.systems.shake.PositionedScreenShakePacket;
import com.finderfeed.fdlib.util.FDProjectile;
import com.finderfeed.fdlib.util.ProjectileMovementPath;
import com.finderfeed.fdlib.util.client.particles.FDBlockParticleOptions;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

public class ChesedBlockProjectile
extends FDProjectile
implements AutoSerializable {
    public static final EntityDataAccessor<Float> ROTATION_SPEED = SynchedEntityData.m_135353_(ChesedBlockProjectile.class, (EntityDataSerializer)EntityDataSerializers.f_135029_);
    public static final EntityDataAccessor<BlockState> STATE = SynchedEntityData.m_135353_(ChesedBlockProjectile.class, (EntityDataSerializer)EntityDataSerializers.f_135034_);
    public static final EntityDataAccessor<Boolean> DROP_PARTICLES = SynchedEntityData.m_135353_(ChesedBlockProjectile.class, (EntityDataSerializer)EntityDataSerializers.f_135035_);
    @SerializableField
    private float damage;
    public ProjectileMovementPath movementPath = null;
    private int dropParticlesTime = 0;
    public Quaternionf currentRotation = new Quaternionf(new AxisAngle4f(0.0f, 0.01f, 1.0f, 0.0f));
    public Quaternionf previousRotation = new Quaternionf(new AxisAngle4f(0.0f, 0.01f, 1.0f, 0.0f));

    public ChesedBlockProjectile(EntityType<? extends AbstractHurtingProjectile> type, Level level) {
        super(type, level);
    }

    public void m_8119_() {
        super.m_8119_();
        this.dropParticles();
        if (this.m_9236_().f_46443_) {
            this.handleRotation();
        } else {
            if (this.movementPath != null) {
                this.movementPath.tick((Entity)this);
                if (this.movementPath.isFinished()) {
                    this.movementPath.tick((Entity)this);
                    this.movementPath = this.movementPath.getNext();
                }
            }
            if (this.f_19797_ >= 2000) {
                this.m_142687_(Entity.RemovalReason.DISCARDED);
            }
        }
    }

    private Vec3 targetGroundPosition(LivingEntity target) {
        Vec3 toReturn = target.m_20182_();
        BlockPos pos = new BlockPos((int)org.joml.Math.floor((double)toReturn.f_82479_), (int)org.joml.Math.floor((double)toReturn.f_82480_), (int)org.joml.Math.floor((double)toReturn.f_82481_));
        for (int i = 0; i < 5; ++i) {
            if (!this.m_9236_().m_8055_(pos.m_7918_(0, -i, 0)).m_60795_()) {
                return toReturn;
            }
            toReturn = toReturn.m_82492_(0.0, 1.0, 0.0);
        }
        return toReturn;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public float getDamage() {
        return this.damage;
    }

    private void dropParticles() {
        if (this.m_9236_().f_46443_) {
            if (!((Boolean)this.f_19804_.m_135370_(DROP_PARTICLES)).booleanValue()) {
                return;
            }
            FDBlockParticleOptions options = FDBlockParticleOptions.builder().state(this.getBlockState()).lifetime(20 + this.f_19796_.m_188503_(5)).quadSizeMultiplier(1.5f).build();
            this.m_9236_().m_6493_((ParticleOptions)options, true, this.m_20185_() + (double)this.f_19796_.m_188501_() - 0.5, this.m_20186_() + (double)(this.f_19796_.m_188501_() * 0.5f), this.m_20189_() + (double)this.f_19796_.m_188501_() - 0.5, 0.0, -0.05 + (double)(this.f_19796_.m_188501_() * 0.025f), 0.0);
        } else {
            this.dropParticlesTime = Mth.m_14045_((int)(this.dropParticlesTime - 1), (int)0, (int)Integer.MAX_VALUE);
            this.f_19804_.m_135381_(DROP_PARTICLES, (Object)(this.dropParticlesTime > 0 ? 1 : 0));
        }
    }

    private void handleRotation() {
        Vec3 oldPos = new Vec3(this.f_19854_, this.f_19855_, this.f_19856_);
        Vec3 currentPos = this.m_20182_();
        Vec3 b = currentPos.m_82546_(oldPos).m_82520_(0.01, 0.0, 0.01);
        double len = b.m_82553_();
        if (len > 0.01) {
            Vector3f v = this.currentRotation.transform(0.0f, 1.0f, 0.0f, new Vector3f());
            Vec3 v3 = FDMathUtil.vector3fToVec3((Vector3f)v);
            float currentAngleBetween = (float)FDMathUtil.angleBetweenVectors((Vec3)b, (Vec3)v3);
            Vec3 axis = b.m_82537_(new Vec3(0.0, 1.0, 0.0)).m_82541_();
            this.previousRotation = new Quaternionf((Quaternionfc)this.currentRotation);
            if ((double)currentAngleBetween > 0.1) {
                float bt = (float)FDMathUtil.angleBetweenVectors((Vec3)b, (Vec3)new Vec3(0.0, 1.0, 0.0));
                Quaternionf targetQuaternion = new Quaternionf(new AxisAngle4f(-bt, (float)axis.f_82479_, (float)axis.f_82480_, (float)axis.f_82481_));
                float rot = (float)Math.toRadians((double)this.getRotationSpeed() * len);
                this.currentRotation.slerp((Quaternionfc)targetQuaternion, Mth.m_14036_((float)(rot / currentAngleBetween), (float)0.0f, (float)1.0f));
            }
        }
    }

    public float getRotationSpeed() {
        return ((Float)this.f_19804_.m_135370_(ROTATION_SPEED)).floatValue();
    }

    public void setRotationSpeed(float rotationSpeed) {
        this.f_19804_.m_135381_(ROTATION_SPEED, (Object)Float.valueOf(rotationSpeed));
    }

    public BlockState getBlockState() {
        return (BlockState)this.f_19804_.m_135370_(STATE);
    }

    public void setBlockState(BlockState state) {
        this.f_19804_.m_135381_(STATE, (Object)state);
    }

    public void setDropParticlesTime(int dropParticlesTime) {
        this.dropParticlesTime = dropParticlesTime;
    }

    public int getDropParticlesTime() {
        return this.dropParticlesTime;
    }

    protected void m_8060_(BlockHitResult blockHitResult) {
        super.m_8060_(blockHitResult);
        if (!this.m_9236_().f_46443_ && !this.f_19794_) {
            this.damageEntities(blockHitResult.m_82450_(), 7.0f);
            SlamParticlesPacket packet = new SlamParticlesPacket(new SlamParticlesPacket.SlamData(blockHitResult.m_82425_(), blockHitResult.m_82450_(), this.m_20184_()));
            FDPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> this), (Object)packet);
            PositionedScreenShakePacket.send((ServerLevel)((ServerLevel)this.m_9236_()), (FDShakeData)FDShakeData.builder().frequency(1.5f).stayTime(0).inTime(2).outTime(6).amplitude(15.0f).build(), (Vec3)blockHitResult.m_82450_().m_82549_(this.m_20184_().m_82542_(1.0, 0.0, 1.0).m_82541_().m_82542_(2.0, 2.0, 2.0)), (double)5.0);
            this.shatter(blockHitResult, 0.5235988f);
            this.launchBlocks(blockHitResult, 0.5235988f);
            this.m_142687_(Entity.RemovalReason.DISCARDED);
            this.m_9236_().m_6263_(null, this.m_20182_().f_82479_, this.m_20182_().f_82480_, this.m_20182_().f_82481_, BossSounds.ROCK_IMPACT.get(), SoundSource.HOSTILE, 0.75f, 1.0f);
        }
    }

    private void damageEntities(Vec3 pos, float radius) {
        AABB box = new AABB((double)(-radius), -1.0, (double)(-radius), (double)radius, 4.0, (double)radius);
        Vec3 movement = this.m_20184_().m_82541_();
        Vec3 pos2 = pos.m_82549_(movement.m_82548_());
        List list = this.m_9236_().m_6443_(LivingEntity.class, box.m_82383_(pos2.m_82549_(movement)), entity -> !(entity instanceof ChesedBossBuddy) && entity.m_20182_().m_82542_(1.0, 0.0, 1.0).m_82554_(pos2.m_82542_(1.0, 0.0, 1.0)) <= (double)radius);
        for (LivingEntity entity2 : list) {
            Vec3 b = entity2.m_20182_().m_82546_(pos2).m_82541_();
            Vec3 forwardVec = movement.m_82542_(1.0, 0.0, 1.0).m_82541_();
            double angle = Math.toDegrees(FDMathUtil.angleBetweenVectors((Vec3)b.m_82542_(1.0, 0.0, 1.0), (Vec3)forwardVec));
            if (!(angle < 25.0)) continue;
            entity2.m_6469_(BossDamageSources.CHESED_BLOCK_ATTACK_SOURCE, this.damage);
            entity2.f_19802_ = 0;
        }
    }

    private void shatter(BlockHitResult result, float rotationLimit) {
        Vec3 direction = this.m_20184_().m_82542_(1.0, 0.0, 1.0).m_82541_();
        BlockPos hitPos = result.m_82425_();
        int maxDist = 7;
        Vec3 hitCenter = hitPos.m_252807_();
        for (int x = -maxDist; x <= maxDist; ++x) {
            for (int z = -maxDist; z <= maxDist; ++z) {
                double diff;
                BlockPos p = hitPos.m_7918_(x, 0, z);
                Vec3 center = p.m_252807_();
                Vec3 b = center.m_82546_(hitCenter).m_82542_(1.0, 0.0, 1.0);
                double len = b.m_82553_();
                if (len > (double)maxDist || !((diff = FDMathUtil.angleBetweenVectors((Vec3)direction, (Vec3)b)) <= (double)rotationLimit)) continue;
                EarthShatterEntity earthShatterEntity = EarthShatterEntity.summon(this.m_9236_(), p, EarthShatterSettings.builder().direction(b.m_82541_().m_82548_().m_82520_(0.0, 0.5, 0.0)).delay((int)Math.round(b.m_82553_())).upDistance(0.1f + (float)b.m_82553_() / (float)maxDist).upTime(1).stayTime(2).downTime(3).build());
            }
        }
    }

    private void launchBlocks(BlockHitResult blockHitResult, float rotationLimit) {
        float baseSpeed = 2.4f;
        Vec3 base = this.m_20184_().m_82542_(1.0, 0.0, 1.0).m_82541_().m_82542_((double)baseSpeed, 0.0, (double)baseSpeed);
        int count = 3 + this.f_19796_.m_188503_(3);
        Vec3 center = blockHitResult.m_82425_().m_252807_().m_82520_(0.0, 0.5, 0.0);
        List<BlockState> states = this.collectStates(blockHitResult.m_82425_(), 2);
        float angle = rotationLimit * 2.0f / (float)count;
        for (int i = 0; i <= count; ++i) {
            float ang = (float)i * angle - rotationLimit + this.f_19796_.m_188501_() * (angle / 2.0f) - angle / 4.0f;
            float md = 1.0f - Math.abs(ang) / rotationLimit;
            float randY = this.f_19796_.m_188501_() * 0.05f * md + 0.32f;
            float speedMd = 1.0f + (this.f_19796_.m_188501_() * 0.25f + md * 0.75f);
            Vec3 f = base.m_82542_((double)speedMd, 0.0, (double)speedMd).m_82524_(ang).m_82520_(0.0, (double)randY, 0.0);
            BlockState state = states.get(this.f_19796_.m_188503_(states.size()));
            FlyingBlockEntity blockEntity = new FlyingBlockEntity(BossEntities.FLYING_BLOCK.get(), this.m_9236_());
            blockEntity.setAirFriction(0.7f);
            blockEntity.m_146884_(center);
            blockEntity.setNoPhysicsTime(2);
            blockEntity.setBlockState(state);
            blockEntity.setRotationSpeed((float)f.m_82553_() / 2.0f * 15.0f);
            blockEntity.m_20256_(f);
            this.m_9236_().m_7967_((Entity)blockEntity);
            int g = 0;
            while ((float)g < md * 2.0f) {
                float mod = 0.6f + this.f_19796_.m_188501_() * 0.4f - 0.2f;
                f = base.m_82542_((double)speedMd, 0.0, (double)speedMd).m_82524_(ang + this.f_19796_.m_188501_() * (angle / 2.0f) - angle / 4.0f).m_82520_(0.0, (double)randY, 0.0).m_82542_((double)mod, (double)mod, (double)mod);
                blockEntity = new FlyingBlockEntity(BossEntities.FLYING_BLOCK.get(), this.m_9236_());
                blockEntity.setAirFriction(0.72f);
                blockEntity.m_146884_(center);
                blockEntity.setNoPhysicsTime(2);
                blockEntity.setBlockState(state);
                blockEntity.setRotationSpeed((float)f.m_82553_() / 2.0f * 15.0f);
                blockEntity.m_20256_(f);
                this.m_9236_().m_7967_((Entity)blockEntity);
                ++g;
            }
        }
    }

    private List<BlockState> collectStates(BlockPos baseImpactPos, int blockRadius) {
        ArrayList<BlockState> states = new ArrayList<BlockState>();
        for (int x = -blockRadius; x <= blockRadius; ++x) {
            for (int y = -blockRadius; y <= blockRadius; ++y) {
                for (int z = -blockRadius; z <= blockRadius; ++z) {
                    BlockPos takePos = baseImpactPos.m_7918_(x, y, z);
                    BlockState state = this.m_9236_().m_8055_(takePos);
                    if (state.m_60795_() || states.contains(state)) continue;
                    states.add(state);
                }
            }
        }
        return states;
    }

    protected void m_8097_() {
        super.m_8097_();
        this.f_19804_.m_135372_(ROTATION_SPEED, (Object)Float.valueOf(20.0f));
        this.f_19804_.m_135372_(STATE, (Object)Blocks.f_152550_.m_49966_());
        this.f_19804_.m_135372_(DROP_PARTICLES, (Object)false);
    }

    public void m_7380_(CompoundTag tag) {
        super.m_7380_(tag);
        if (this.movementPath != null) {
            this.movementPath.autoSave("path", tag);
        }
        tag.m_128350_("rotationSpeed", this.getRotationSpeed());
        tag.m_128365_("state", (Tag)NbtUtils.m_129202_((BlockState)this.getBlockState()));
        this.autoSave(tag);
    }

    public void m_7378_(CompoundTag tag) {
        super.m_7378_(tag);
        if (tag.m_128441_("path")) {
            this.movementPath = new ProjectileMovementPath();
            this.movementPath.autoLoad("path", tag);
        }
        this.setRotationSpeed(tag.m_128457_("rotationSpeed"));
        this.setBlockState(NbtUtils.m_247651_((HolderGetter)this.m_9236_().m_246945_(Registries.f_256747_), (CompoundTag)tag.m_128469_("state")));
        this.autoLoad(tag);
    }
}


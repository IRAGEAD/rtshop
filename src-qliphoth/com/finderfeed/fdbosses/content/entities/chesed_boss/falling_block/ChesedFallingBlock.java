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
 *  net.minecraft.core.HolderGetter
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.NbtUtils
 *  net.minecraft.nbt.Tag
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.EntityDataSerializers
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.sounds.SoundSource
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
 */
package com.finderfeed.fdbosses.content.entities.chesed_boss.falling_block;

import com.finderfeed.fdbosses.content.entities.chesed_boss.ChesedBossBuddy;
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
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
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

public class ChesedFallingBlock
extends FDProjectile
implements AutoSerializable {
    public static final EntityDataAccessor<BlockState> STATE = SynchedEntityData.m_135353_(ChesedFallingBlock.class, (EntityDataSerializer)EntityDataSerializers.f_135034_);
    public static final EntityDataAccessor<Float> GRAVITY = SynchedEntityData.m_135353_(ChesedFallingBlock.class, (EntityDataSerializer)EntityDataSerializers.f_135029_);
    @SerializableField
    public float damage;
    public boolean softerSound = false;

    public ChesedFallingBlock(EntityType<? extends AbstractHurtingProjectile> type, Level level) {
        super(type, level);
    }

    public static ChesedFallingBlock summon(Level level, BlockState state, Vec3 pos, Vec3 speed, float damage, float gravity) {
        ChesedFallingBlock block = new ChesedFallingBlock(BossEntities.CHESED_FALLING_BLOCK.get(), level);
        block.m_146884_(pos);
        block.m_20256_(speed);
        block.setBlockState(state);
        block.m_20088_().m_135381_(GRAVITY, (Object)Float.valueOf(gravity));
        level.m_7967_((Entity)block);
        block.damage = damage;
        return block;
    }

    public static ChesedFallingBlock summon(Level level, BlockState state, Vec3 pos, float damage) {
        return ChesedFallingBlock.summon(level, state, pos, Vec3.f_82478_, damage);
    }

    public static ChesedFallingBlock summon(Level level, BlockState state, Vec3 pos, Vec3 speed, float damage) {
        return ChesedFallingBlock.summon(level, state, pos, speed, damage, 0.025f);
    }

    public void m_8119_() {
        super.m_8119_();
        this.m_20256_(this.m_20184_().m_82520_(0.0, -this.getDefaultGravity(), 0.0));
    }

    protected void m_8060_(BlockHitResult result) {
        super.m_8060_(result);
        Vec3 pos = result.m_82450_();
        if (!this.m_9236_().f_46443_) {
            float rd = 1.5f;
            AABB box = new AABB((double)(-rd), (double)(-rd), (double)(-rd), (double)rd, (double)rd, (double)rd).m_82383_(pos);
            for (LivingEntity entity : this.m_9236_().m_6443_(LivingEntity.class, box, pr -> !(pr instanceof ChesedBossBuddy))) {
                if (this.damage != 0.0f) {
                    entity.m_6469_(BossDamageSources.CHESED_FALLING_BLOCK_SOURCE, this.damage);
                }
                if (!(entity instanceof ServerPlayer)) continue;
                ServerPlayer player = (ServerPlayer)entity;
                PositionedScreenShakePacket.send((ServerLevel)((ServerLevel)this.m_9236_()), (FDShakeData)FDShakeData.builder().frequency(1.5f).stayTime(0).inTime(2).outTime(6).amplitude(15.0f).build(), (Vec3)pos, (double)5.0);
            }
            if (this.f_19797_ > 10) {
                float volume = 0.5f;
                if (this.softerSound) {
                    volume = 0.2f;
                }
                this.m_9236_().m_6263_(null, pos.f_82479_, pos.f_82480_, pos.f_82481_, BossSounds.ROCK_IMPACT.get(), SoundSource.HOSTILE, volume, 1.0f);
                SlamParticlesPacket packet = new SlamParticlesPacket(new SlamParticlesPacket.SlamData(result.m_82425_(), pos.m_82520_(0.0, 0.5, 0.0), new Vec3(1.0, 0.0, 0.0)).maxAngle((float)Math.PI * 2).maxSpeed(0.3f).collectRadius(2).maxParticleLifetime(15).count(5).maxVerticalSpeedEdges(0.15f).maxVerticalSpeedCenter(0.15f));
                FDPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> this), (Object)packet);
            }
            this.m_142687_(Entity.RemovalReason.DISCARDED);
        }
    }

    public BlockState getBlockState() {
        return (BlockState)this.f_19804_.m_135370_(STATE);
    }

    public void setBlockState(BlockState state) {
        this.f_19804_.m_135381_(STATE, (Object)state);
    }

    protected void m_8097_() {
        this.f_19804_.m_135372_(STATE, (Object)Blocks.f_50069_.m_49966_());
        this.f_19804_.m_135372_(GRAVITY, (Object)Float.valueOf(0.025f));
    }

    public boolean m_20223_(CompoundTag tag) {
        tag.m_128365_("state", (Tag)NbtUtils.m_129202_((BlockState)this.getBlockState()));
        this.autoSave(tag);
        return super.m_20223_(tag);
    }

    public void m_20258_(CompoundTag tag) {
        this.setBlockState(NbtUtils.m_247651_((HolderGetter)this.m_9236_().m_246945_(Registries.f_256747_), (CompoundTag)tag.m_128469_("state")));
        this.autoLoad(tag);
        super.m_20258_(tag);
    }

    protected double getDefaultGravity() {
        return ((Float)this.m_20088_().m_135370_(GRAVITY)).floatValue();
    }

    public boolean m_6783_(double dist) {
        return dist < 14400.0;
    }
}


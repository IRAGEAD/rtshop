/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.bedrock.animations.Animation
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDLivingEntity
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions
 *  net.minecraft.commands.arguments.EntityAnchorArgument$Anchor
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.EntityDataSerializers
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.world.damagesource.DamageSource
 *  net.minecraft.world.damagesource.DamageTypes
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 */
package com.finderfeed.fdbosses.content.entities.geburah.geburah_bell;

import com.finderfeed.fdbosses.content.entities.geburah.GeburahEntity;
import com.finderfeed.fdbosses.init.BossAnims;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdlib.systems.bedrock.animations.Animation;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDLivingEntity;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions;
import java.util.UUID;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class GeburahBell
extends FDLivingEntity {
    public static final EntityDataAccessor<Boolean> IS_RED = SynchedEntityData.m_135353_(GeburahBell.class, (EntityDataSerializer)EntityDataSerializers.f_135035_);
    private UUID geburah;
    private boolean alreadyRang = false;

    public static GeburahBell summon(GeburahEntity geburah, Vec3 pos, Vec3 lookAt, boolean isRed) {
        GeburahBell bell = new GeburahBell(BossEntities.GEBURAH_BELL.get(), geburah.m_9236_());
        bell.m_146884_(pos);
        bell.m_7618_(EntityAnchorArgument.Anchor.FEET, lookAt);
        bell.m_20088_().m_135381_(IS_RED, (Object)isRed);
        bell.geburah = geburah.m_20148_();
        geburah.m_9236_().m_7967_((Entity)bell);
        return bell;
    }

    public GeburahBell(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
        this.f_19794_ = true;
        this.m_20242_(true);
    }

    public void m_8119_() {
        super.m_8119_();
        if (!this.m_9236_().f_46443_) {
            if (this.geburah == null) {
                this.m_142467_(Entity.RemovalReason.DISCARDED);
            }
        } else if (this.f_19797_ > 10 && !this.m_21224_()) {
            for (int i = 0; i < 2; ++i) {
                Vec3 offset = new Vec3(1.0, 0.0, 0.0).m_82524_((float)i * (float)Math.PI + (float)this.f_19797_ * (float)Math.PI / 32.0f);
                BallParticleOptions options = this.isRed() ? BallParticleOptions.builder().brightness(4).color(1.0f, 0.3f, 0.1f, 1.0f).scalingOptions(0, 0, 20).size(0.1f + this.f_19796_.m_188501_() * 0.2f).build() : BallParticleOptions.builder().brightness(4).color(0.1f, 0.8f, 1.0f, 1.0f).scalingOptions(0, 0, 20).size(0.1f + this.f_19796_.m_188501_() * 0.2f).build();
                this.m_9236_().m_7106_((ParticleOptions)options, this.m_20185_() + offset.f_82479_, this.m_20186_() + 0.5, this.m_20189_() + offset.f_82481_, (double)this.f_19796_.m_188501_() * 0.01 - 0.005, (double)this.f_19796_.m_188501_() * 0.01 - 0.005, (double)this.f_19796_.m_188501_() * 0.01 - 0.005);
            }
        }
    }

    protected void m_6153_() {
        if (!this.m_9236_().f_46443_ && this.f_20919_ == 0) {
            this.getAnimationSystem().startAnimation("MAIN", AnimationTicker.builder(BossAnims.GEBURAH_BELL_RING).build());
        }
        ++this.f_20919_;
        if (this.f_20919_ >= ((Animation)BossAnims.GEBURAH_BELL_RING.get()).getAnimTime() && !this.m_9236_().m_5776_() && !this.m_213877_()) {
            this.m_142687_(Entity.RemovalReason.KILLED);
        }
    }

    @Nullable
    protected SoundEvent m_7975_(DamageSource p_21239_) {
        return null;
    }

    @Nullable
    protected SoundEvent m_5592_() {
        return null;
    }

    public boolean m_6469_(DamageSource source, float damage) {
        Entity entity = source.m_7639_();
        if (entity instanceof ServerPlayer) {
            GeburahEntity geburah;
            ServerPlayer serverPlayer = (ServerPlayer)entity;
            if (!this.alreadyRang && (geburah = this.getGeburah()) != null) {
                geburah.bellRang(this);
                this.alreadyRang = true;
            }
            return super.m_6469_(source, 10.0f);
        }
        if (source.m_276093_(DamageTypes.f_286979_) || source.m_276093_(DamageTypes.f_268724_)) {
            return super.m_6469_(source, damage);
        }
        return false;
    }

    public boolean isRed() {
        return (Boolean)this.m_20088_().m_135370_(IS_RED);
    }

    public void setState(boolean isRed) {
        this.m_20088_().m_135381_(IS_RED, (Object)isRed);
    }

    public GeburahEntity getGeburah() {
        ServerLevel serverLevel;
        Level level = this.m_9236_();
        if (level instanceof ServerLevel && (level = (serverLevel = (ServerLevel)level).m_8791_(this.geburah)) instanceof GeburahEntity) {
            GeburahEntity redMist = (GeburahEntity)level;
            return redMist;
        }
        return null;
    }

    public void m_7378_(CompoundTag tag) {
        super.m_7378_(tag);
        this.m_20088_().m_135381_(IS_RED, (Object)tag.m_128471_("is_red"));
        if (tag.m_128441_("geburah")) {
            this.geburah = tag.m_128342_("geburah");
        }
    }

    public void m_7380_(CompoundTag tag) {
        super.m_7380_(tag);
        tag.m_128379_("is_red", ((Boolean)this.m_20088_().m_135370_(IS_RED)).booleanValue());
        if (this.geburah != null) {
            tag.m_128362_("geburah", this.geburah);
        }
    }

    protected void m_8097_() {
        super.m_8097_();
        this.m_20088_().m_135372_(IS_RED, (Object)true);
    }

    public void m_7334_(Entity p_21294_) {
    }

    public void m_5997_(double p_20286_, double p_20287_, double p_20288_) {
    }

    protected void m_6138_() {
    }

    public void m_20334_(double p_20335_, double p_20336_, double p_20337_) {
        super.m_20334_(0.0, 0.0, 0.0);
    }

    public void m_20256_(Vec3 p_20257_) {
        super.m_20256_(Vec3.f_82478_);
    }
}


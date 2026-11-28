/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.init.FDEDataSerializers
 *  com.finderfeed.fdlib.systems.bedrock.animations.Animation
 *  com.finderfeed.fdlib.systems.bedrock.animations.Animation$LoopMode
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDLivingEntity
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.world.damagesource.DamageSource
 *  net.minecraft.world.damagesource.DamageTypes
 *  net.minecraft.world.effect.MobEffect
 *  net.minecraft.world.effect.MobEffectInstance
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.AxeItem
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.gameevent.GameEvent
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 */
package com.finderfeed.fdbosses.content.entities.chesed_boss.chesed_crystal;

import com.finderfeed.fdbosses.content.entities.chesed_boss.ChesedBossBuddy;
import com.finderfeed.fdbosses.init.BossAnims;
import com.finderfeed.fdbosses.init.BossEffects;
import com.finderfeed.fdbosses.init.BossSounds;
import com.finderfeed.fdlib.init.FDEDataSerializers;
import com.finderfeed.fdlib.systems.bedrock.animations.Animation;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.FDLivingEntity;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class ChesedCrystalEntity
extends FDLivingEntity
implements ChesedBossBuddy {
    public static final EntityDataAccessor<Vec3> DIRECTION = SynchedEntityData.m_135353_(ChesedCrystalEntity.class, (EntityDataSerializer)((EntityDataSerializer)FDEDataSerializers.VEC3.get()));
    private boolean dead = false;
    private int deathTime = 20;

    public ChesedCrystalEntity(EntityType<? extends FDLivingEntity> type, Level level) {
        super(type, level);
        this.getAnimationSystem().startAnimation("SPAWN", AnimationTicker.builder((Animation)((Animation)BossAnims.CHESED_CRYSTAL_SPAWN.get())).setToNullTransitionTime(0).build());
    }

    public void m_8119_() {
        super.m_8119_();
        if (!this.m_9236_().f_46443_ && this.dead && this.deathTime-- < 0) {
            this.m_142467_(Entity.RemovalReason.KILLED);
            this.m_146850_(GameEvent.f_223707_);
        }
    }

    public void m_6667_(DamageSource src) {
        if (!this.m_9236_().f_46443_) {
            if (this.f_20888_ != null) {
                this.f_20888_.m_7292_(new MobEffectInstance((MobEffect)BossEffects.CHESED_ENERGIZED.get(), 400, 1, false, true));
            }
            this.dead = true;
            this.deathTime = 10;
            this.getAnimationSystem().startAnimation("DEATH", AnimationTicker.builder((Animation)((Animation)BossAnims.CHESED_CRYSTAL_SPAWN.get())).setLoopMode(Animation.LoopMode.HOLD_ON_LAST_FRAME).reversed().setSpeed(2.0f).build());
        }
    }

    public boolean m_6469_(DamageSource src, float amount) {
        Entity entity = src.m_7639_();
        if (entity instanceof Player) {
            Player player = (Player)entity;
            this.m_6598_(player);
            float modifier = 1.0f;
            Item item = player.m_21205_().m_41720_();
            if (item instanceof AxeItem) {
                AxeItem axe = (AxeItem)item;
            } else {
                modifier = 0.05f;
            }
            return super.m_6469_(src, amount * modifier);
        }
        if (src.m_276093_(DamageTypes.f_286979_) || src.m_276093_(DamageTypes.f_268724_)) {
            this.f_20888_ = null;
            return super.m_6469_(src, amount);
        }
        this.f_20888_ = null;
        return false;
    }

    @Nullable
    protected SoundEvent m_7975_(DamageSource src) {
        if (src.m_276093_(DamageTypes.f_286979_)) {
            return null;
        }
        return BossSounds.CHESED_CRYSTAL_HIT.get();
    }

    @Nullable
    protected SoundEvent m_5592_() {
        return BossSounds.CHESED_CRYSTAL_HIT.get();
    }

    public boolean m_20068_() {
        return true;
    }

    public void m_147240_(double p_147241_, double p_147242_, double p_147243_) {
    }

    public void m_7334_(Entity p_21294_) {
    }

    public void m_5997_(double p_20286_, double p_20287_, double p_20288_) {
    }

    protected void m_6138_() {
    }

    protected void m_6677_(DamageSource src) {
        super.m_6677_(src);
    }

    public Vec3 getCrystalFacingDirection() {
        return (Vec3)this.f_19804_.m_135370_(DIRECTION);
    }

    public void setCrystalFacingDirection(Vec3 v) {
        this.f_19804_.m_135381_(DIRECTION, (Object)v);
    }

    public boolean m_6087_() {
        return true;
    }

    public boolean m_6000_(double p_20296_, double p_20297_, double p_20298_) {
        return true;
    }

    protected void m_8097_() {
        super.m_8097_();
        this.f_19804_.m_135372_(DIRECTION, (Object)new Vec3(0.0, 1.0, 0.0));
    }
}


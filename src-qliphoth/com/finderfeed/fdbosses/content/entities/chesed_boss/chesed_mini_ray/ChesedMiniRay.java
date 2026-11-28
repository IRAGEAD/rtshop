/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.FDLibCalls
 *  com.finderfeed.fdlib.nbt.AutoSerializable
 *  com.finderfeed.fdlib.nbt.SerializableField
 *  com.finderfeed.fdlib.systems.shake.FDShakeData
 *  com.finderfeed.fdlib.systems.shake.PositionedScreenShakePacket
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions
 *  com.finderfeed.fdlib.util.client.particles.lightning_particle.LightningParticleOptions
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  com.google.common.collect.Multimap
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.EntityDataSerializers
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.util.Mth
 *  net.minecraft.world.damagesource.DamageSource
 *  net.minecraft.world.effect.MobEffect
 *  net.minecraft.world.effect.MobEffectInstance
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.EquipmentSlot
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.MobType
 *  net.minecraft.world.entity.ai.attributes.AttributeInstance
 *  net.minecraft.world.entity.ai.attributes.AttributeModifier
 *  net.minecraft.world.entity.ai.attributes.AttributeModifier$Operation
 *  net.minecraft.world.entity.ai.attributes.Attributes
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.enchantment.EnchantmentHelper
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix4f
 *  org.joml.Vector3f
 */
package com.finderfeed.fdbosses.content.entities.chesed_boss.chesed_mini_ray;

import com.finderfeed.fdbosses.client.particles.chesed_attack_ray.ChesedRayOptions;
import com.finderfeed.fdbosses.init.BossConfigs;
import com.finderfeed.fdbosses.init.BossEffects;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdbosses.init.BossSounds;
import com.finderfeed.fdlib.FDLibCalls;
import com.finderfeed.fdlib.nbt.AutoSerializable;
import com.finderfeed.fdlib.nbt.SerializableField;
import com.finderfeed.fdlib.systems.shake.FDShakeData;
import com.finderfeed.fdlib.systems.shake.PositionedScreenShakePacket;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions;
import com.finderfeed.fdlib.util.client.particles.lightning_particle.LightningParticleOptions;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class ChesedMiniRay
extends Entity
implements AutoSerializable {
    public static final EntityDataAccessor<Integer> TARGET = SynchedEntityData.m_135353_(ChesedMiniRay.class, (EntityDataSerializer)EntityDataSerializers.f_135028_);
    public static final int TIME_UNTIL_ATTACK = 25;
    public static final int ATTACK_TIME = 8;
    private ItemStack item;
    @SerializableField
    private UUID target;
    @SerializableField
    private UUID owner;

    public static ChesedMiniRay summon(Level level, LivingEntity target, ItemStack item, LivingEntity owner) {
        ChesedMiniRay chesedMiniRay = new ChesedMiniRay(BossEntities.CHESED_MINI_RAY.get(), level);
        chesedMiniRay.item = item;
        chesedMiniRay.target = target.m_20148_();
        chesedMiniRay.owner = owner.m_20148_();
        chesedMiniRay.f_19804_.m_135381_(TARGET, (Object)target.m_19879_());
        Vec3 targetPos = target.m_20182_();
        Vec3 between = targetPos.m_82546_(owner.m_20182_()).m_82542_(1.0, 0.0, 1.0).m_82541_();
        float rndDistMod = Mth.m_14036_((float)(target.m_20205_() * 3.0f), (float)2.0f, (float)10.0f) + level.f_46441_.m_188501_() * 2.0f;
        float rndOffsNum = level.f_46441_.m_188499_() ? 1.0f : -1.0f;
        Vec3 randomOffset = between.m_82542_((double)rndDistMod, (double)rndDistMod, (double)rndDistMod).m_82524_(rndOffsNum * (float)Math.PI / 2.0f + -rndOffsNum * (float)Math.PI / 8.0f).m_82520_(0.0, 1.5 + (double)(level.f_46441_.m_188501_() * 2.0f), 0.0);
        Vec3 resultPos = targetPos.m_82549_(randomOffset);
        chesedMiniRay.m_146884_(resultPos);
        level.m_7967_((Entity)chesedMiniRay);
        return chesedMiniRay;
    }

    public ChesedMiniRay(EntityType<?> p_19870_, Level p_19871_) {
        super(p_19870_, p_19871_);
    }

    public void m_8119_() {
        block5: {
            LivingEntity target;
            block3: {
                block4: {
                    super.m_8119_();
                    if (this.m_9236_().f_46443_) break block3;
                    this.checkRemoved();
                    if (this.f_19797_ != 1) break block4;
                    this.m_9236_().m_6263_(null, this.m_20185_(), this.m_20186_(), this.m_20189_(), BossSounds.CHESED_RAY_CHARGE.get(), SoundSource.NEUTRAL, 2.0f, 1.15f);
                    break block5;
                }
                if (this.f_19797_ != 25) break block5;
                this.attack();
                break block5;
            }
            if (this.f_19797_ == 25 && (target = this.getTarget()) != null) {
                this.attackParticles(this.getTargetPos(target, 0.0f));
            }
            if (this.f_19797_ < 25) {
                for (int i = 0; i < 10; ++i) {
                    BallParticleOptions options = BallParticleOptions.builder().friction(0.5f).size(0.25f).color(100 + this.f_19796_.m_188503_(50), 255, 255).scalingOptions(3, 0, 10).build();
                    LightningParticleOptions lightningParticleOptions = LightningParticleOptions.builder().color(20, 150 + this.f_19796_.m_188503_(50), 255).lifetime(10).maxLightningSegments(3).randomRoll(true).build();
                    this.m_9236_().m_7106_((ParticleOptions)options, this.m_20185_(), this.m_20186_(), this.m_20189_(), (double)(this.m_9236_().f_46441_.m_188501_() * 0.25f) - 0.125, (double)(this.m_9236_().f_46441_.m_188501_() * 0.25f) - 0.125, (double)(this.m_9236_().f_46441_.m_188501_() * 0.25f) - 0.125);
                    if (!this.f_19796_.m_188499_()) continue;
                    this.m_9236_().m_7106_((ParticleOptions)lightningParticleOptions, this.m_20185_(), this.m_20186_(), this.m_20189_(), (double)(this.m_9236_().f_46441_.m_188501_() * 0.1f) - 0.05, (double)(this.m_9236_().f_46441_.m_188501_() * 0.1f) - 0.05, (double)(this.m_9236_().f_46441_.m_188501_() * 0.1f) - 0.05);
                }
            }
        }
    }

    private void attackParticles(Vec3 basePos) {
        LivingEntity target = this.getTarget();
        if (target == null) {
            return;
        }
        Vec3 pos = this.m_20182_();
        Vec3 targetPos = this.getTargetPos(target, 0.0f);
        Vec3 dir = pos.m_82546_(targetPos);
        Matrix4f mat = new Matrix4f();
        FDRenderUtil.applyMovementMatrixRotations((Matrix4f)mat, (Vec3)dir);
        int countOnCircle = 16;
        float angle = (float)Math.PI * 2 / (float)countOnCircle;
        int count = 16;
        for (int i = 0; i < countOnCircle; ++i) {
            for (int k = 0; k < count; ++k) {
                float currentAngle = (float)i * angle + this.f_19796_.m_188501_() * angle;
                Vec3 particleDirection = new Vec3(1.0, 0.0, 0.0).m_82524_(currentAngle);
                float p = (float)k / ((float)count - 1.0f);
                float size = 0.25f + p * 0.25f;
                float speedMod = (1.0f - p) * (2.0f + this.f_19796_.m_188501_() * 0.5f);
                BallParticleOptions options = BallParticleOptions.builder().friction(0.5f).size(size).color(100 + this.f_19796_.m_188503_(50), 255, 255).scalingOptions(3, 0, 10).build();
                Vector3f v = new Vector3f((float)particleDirection.f_82479_, 3.0f, (float)particleDirection.f_82481_).normalize();
                float offsetRandom = this.f_19796_.m_188501_() * 0.5f;
                Vector3f v2 = new Vector3f((float)particleDirection.f_82479_ * offsetRandom, 0.0f, (float)particleDirection.f_82481_ * offsetRandom);
                Vector3f finalSpeed = mat.transformDirection(v);
                Vector3f finalPosOffset = mat.transformDirection(v2);
                Vec3 ppos = basePos.m_82520_((double)finalPosOffset.x, (double)finalPosOffset.y, (double)finalPosOffset.z);
                this.m_9236_().m_6493_((ParticleOptions)options, true, ppos.f_82479_, ppos.f_82480_, ppos.f_82481_, (double)(finalSpeed.x * speedMod), (double)(finalSpeed.y * speedMod), (double)(finalSpeed.z * speedMod));
            }
        }
    }

    public void attack() {
        ServerLevel serverLevel;
        Level level = this.m_9236_();
        if (level instanceof ServerLevel && (level = (serverLevel = (ServerLevel)level).m_8791_(this.owner)) instanceof LivingEntity) {
            LivingEntity owner = (LivingEntity)level;
            LivingEntity target = this.getTarget();
            if (target == null) {
                return;
            }
            Vec3 end = this.getTargetPos(target, 0.0f);
            ChesedRayOptions options = ChesedRayOptions.builder().time(2, 2, 7).lightningColor(90, 180, 255).color(100, 255, 255).end(end).width(0.6f).build();
            FDLibCalls.sendParticles((ServerLevel)((ServerLevel)this.m_9236_()), (ParticleOptions)options, (Vec3)this.m_20182_(), (double)120.0);
            FDLibCalls.sendParticles((ServerLevel)((ServerLevel)this.m_9236_()), (ParticleOptions)BallParticleOptions.builder().size(5.0f).scalingOptions(1, 0, 2).color(150, 230, 255).build(), (Vec3)end, (double)120.0);
            PositionedScreenShakePacket.send((ServerLevel)((ServerLevel)this.m_9236_()), (FDShakeData)FDShakeData.builder().frequency(5.0f).amplitude(5.0f).inTime(0).stayTime(0).outTime(5).build(), (Vec3)end, (double)30.0);
            this.m_9236_().m_6263_(null, this.m_20185_(), this.m_20186_(), this.m_20189_(), BossSounds.CHESED_LIGHTNING_RAY.get(), SoundSource.NEUTRAL, 2.0f, 1.0f);
            this.hurtTarget(owner, target);
        }
    }

    public Vec3 getTargetPos(LivingEntity target, float pticks) {
        return target.m_20318_(pticks).m_82520_(0.0, (double)(target.m_20206_() / 2.0f), 0.0);
    }

    public void checkRemoved() {
        Level level = this.m_9236_();
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            if (this.f_19797_ > 33) {
                this.m_142467_(Entity.RemovalReason.DISCARDED);
            }
            if (this.target == null || this.owner == null) {
                this.m_142467_(Entity.RemovalReason.DISCARDED);
            }
            if (serverLevel.m_8791_(this.owner) == null) {
                this.m_142467_(Entity.RemovalReason.DISCARDED);
            } else {
                Entity entity = serverLevel.m_8791_(this.target);
                if (!(entity instanceof LivingEntity)) {
                    this.m_142467_(Entity.RemovalReason.DISCARDED);
                } else {
                    LivingEntity living = (LivingEntity)entity;
                    this.f_19804_.m_135381_(TARGET, (Object)living.m_19879_());
                }
            }
        }
    }

    public LivingEntity getTarget() {
        if (this.m_9236_().f_46443_) {
            Entity entity = this.m_9236_().m_6815_(((Integer)this.f_19804_.m_135370_(TARGET)).intValue());
            if (entity instanceof LivingEntity) {
                LivingEntity living = (LivingEntity)entity;
                return living;
            }
        } else {
            Entity entity = ((ServerLevel)this.m_9236_()).m_8791_(this.target);
            if (entity instanceof LivingEntity) {
                LivingEntity living = (LivingEntity)entity;
                return living;
            }
        }
        return null;
    }

    private void hurtTarget(LivingEntity owner, LivingEntity target) {
        AttributeInstance attribute = owner.m_21051_(Attributes.f_22281_);
        double base = attribute.m_22115_();
        Multimap map = this.item.m_41638_(EquipmentSlot.MAINHAND);
        if (map.containsKey((Object)Attributes.f_22281_)) {
            Collection attributes = map.get((Object)Attributes.f_22281_);
            Map<AttributeModifier.Operation, List<AttributeModifier>> operationAttributes = this.modifierCollectionToOperationMap(attributes);
            for (AttributeModifier mod : operationAttributes.get(AttributeModifier.Operation.ADDITION)) {
                base += mod.m_22218_();
            }
            double damage = base;
            for (AttributeModifier mod : operationAttributes.get(AttributeModifier.Operation.MULTIPLY_BASE)) {
                damage += base * mod.m_22218_();
            }
            for (AttributeModifier mod : operationAttributes.get(AttributeModifier.Operation.MULTIPLY_TOTAL)) {
                damage *= 1.0 + mod.m_22218_();
            }
            float dmg = BossConfigs.BOSS_CONFIG.get().itemConfig.lightningStrikeDamagePercent / 100.0f;
            DamageSource damageSource = this.m_9236_().m_269111_().m_269333_(owner);
            damage += (double)EnchantmentHelper.m_44833_((ItemStack)this.item, (MobType)MobType.f_21640_);
            target.m_7311_(0);
            target.f_19802_ = 0;
            if (target.m_6469_(damageSource, (float)(damage *= (double)dmg))) {
                int duration = BossConfigs.BOSS_CONFIG.get().itemConfig.lightningStrikeShockDuration;
                target.m_7292_(new MobEffectInstance((MobEffect)BossEffects.SHOCKED.get(), duration, 0));
                EnchantmentHelper.m_44823_((LivingEntity)target, (Entity)owner);
                EnchantmentHelper.m_44896_((LivingEntity)owner, (Entity)target);
                target.f_19802_ = 0;
            }
        }
    }

    private Map<AttributeModifier.Operation, List<AttributeModifier>> modifierCollectionToOperationMap(Collection<AttributeModifier> collection) {
        LinkedHashMap<AttributeModifier.Operation, List<AttributeModifier>> operationListMap = new LinkedHashMap<AttributeModifier.Operation, List<AttributeModifier>>();
        operationListMap.put(AttributeModifier.Operation.ADDITION, new ArrayList());
        operationListMap.put(AttributeModifier.Operation.MULTIPLY_BASE, new ArrayList());
        operationListMap.put(AttributeModifier.Operation.MULTIPLY_TOTAL, new ArrayList());
        for (AttributeModifier mod : collection) {
            ((List)operationListMap.get(mod.m_22217_())).add(mod);
        }
        return operationListMap;
    }

    protected void m_8097_() {
        this.f_19804_.m_135372_(TARGET, (Object)-1);
    }

    protected void m_7378_(CompoundTag tag) {
        this.item = ItemStack.m_41712_((CompoundTag)tag.m_128469_("item"));
        this.autoLoad(tag);
    }

    protected void m_7380_(CompoundTag tag) {
        CompoundTag item = this.item.m_41739_(new CompoundTag());
        tag.m_128365_("item", (Tag)item);
        this.autoSave(tag);
    }
}


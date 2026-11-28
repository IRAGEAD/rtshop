/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.EntityDataSerializers
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.util.Mth
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EquipmentSlot
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.ai.attributes.Attribute
 *  net.minecraft.world.entity.ai.attributes.AttributeInstance
 *  net.minecraft.world.entity.ai.attributes.AttributeModifier
 *  net.minecraft.world.entity.ai.attributes.Attributes
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraftforge.common.MinecraftForge
 *  net.minecraftforge.event.entity.EntityJoinLevelEvent
 *  net.minecraftforge.event.entity.living.LivingEvent$LivingTickEvent
 *  net.minecraftforge.event.entity.living.LivingFallEvent
 *  net.minecraftforge.eventbus.api.Event
 *  org.jetbrains.annotations.ApiStatus$Internal
 */
package yesman.epicfight.world.capabilities.entitypatch.player;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.Event;
import org.jetbrains.annotations.ApiStatus;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.forgeevent.BattleModeSustainableEvent;
import yesman.epicfight.api.forgeevent.ChangePlayerModeEvent;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.ValueModifier;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.gameasset.EpicFightSkills;
import yesman.epicfight.skill.BasicAttack;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlot;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.skill.modules.ChargeableSkill;
import yesman.epicfight.skill.modules.HoldableSkill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.Faction;
import yesman.epicfight.world.capabilities.entitypatch.Factions;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.skill.CapabilitySkill;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;
import yesman.epicfight.world.damagesource.EpicFightDamageSources;
import yesman.epicfight.world.damagesource.StunType;
import yesman.epicfight.world.entity.ai.attribute.EpicFightAttributes;
import yesman.epicfight.world.entity.eventlistener.FallEvent;
import yesman.epicfight.world.entity.eventlistener.ModifyAttackSpeedEvent;
import yesman.epicfight.world.entity.eventlistener.ModifyBaseDamageEvent;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener;
import yesman.epicfight.world.entity.eventlistener.SkillConsumeEvent;
import yesman.epicfight.world.gamerule.EpicFightGameRules;

public abstract class PlayerPatch<T extends Player>
extends LivingEntityPatch<T> {
    public static EntityDataAccessor<Float> STAMINA;
    protected static final UUID PLAYER_EVENT_UUID;
    protected static final float PLAYER_SCALE = 0.9375f;
    protected PlayerEventListener eventListeners;
    protected PlayerMode playerMode = PlayerMode.VANILLA;
    protected boolean battleModeRestricted;
    protected float modelYRotO;
    protected float modelYRot;
    protected boolean useModelYRot;
    protected int tickSinceLastAction;
    protected int staminaRegenAwaitTicks;
    protected int lastChargingTick;
    protected int chargingAmount;
    protected HoldableSkill holdingSkill;
    protected double xo;
    protected double yo;
    protected double zo;
    public double dx;
    public double dz;

    public static void initPlayerDataAccessor() {
        STAMINA = SynchedEntityData.m_135353_(Player.class, (EntityDataSerializer)EntityDataSerializers.f_135029_);
    }

    public static void createSyncedEntityData(LivingEntity livingentity) {
        livingentity.m_20088_().m_135372_(STAMINA, (Object)Float.valueOf(0.0f));
    }

    public PlayerPatch() {
        this.eventListeners = new PlayerEventListener(this);
    }

    @Override
    public void onJoinWorld(T entity, EntityJoinLevelEvent event) {
        super.onJoinWorld(entity, event);
        CapabilitySkill skillCapability = this.getSkillCapability();
        skillCapability.getSkillContainerFor(SkillSlots.BASIC_ATTACK).setSkill(EpicFightSkills.BASIC_ATTACK);
        skillCapability.getSkillContainerFor(SkillSlots.KNOCKDOWN_WAKEUP).setSkill(EpicFightSkills.KNOCKDOWN_WAKEUP);
        this.tickSinceLastAction = 0;
        this.staminaRegenAwaitTicks = 30;
    }

    @Override
    public void initAnimator(Animator animator) {
        super.initAnimator(animator);
        animator.addLivingAnimation(LivingMotions.IDLE, Animations.BIPED_IDLE);
        animator.addLivingAnimation(LivingMotions.WALK, Animations.BIPED_WALK);
        animator.addLivingAnimation(LivingMotions.RUN, Animations.BIPED_RUN);
        animator.addLivingAnimation(LivingMotions.SNEAK, Animations.BIPED_SNEAK);
        animator.addLivingAnimation(LivingMotions.SWIM, Animations.BIPED_SWIM);
        animator.addLivingAnimation(LivingMotions.FLOAT, Animations.BIPED_FLOAT);
        animator.addLivingAnimation(LivingMotions.KNEEL, Animations.BIPED_KNEEL);
        animator.addLivingAnimation(LivingMotions.FALL, Animations.BIPED_FALL);
        animator.addLivingAnimation(LivingMotions.MOUNT, Animations.BIPED_MOUNT);
        animator.addLivingAnimation(LivingMotions.SIT, Animations.BIPED_SIT);
        animator.addLivingAnimation(LivingMotions.FLY, Animations.BIPED_FLYING);
        animator.addLivingAnimation(LivingMotions.DEATH, Animations.BIPED_DEATH);
        animator.addLivingAnimation(LivingMotions.JUMP, Animations.BIPED_JUMP);
        animator.addLivingAnimation(LivingMotions.CLIMB, Animations.BIPED_CLIMBING);
        animator.addLivingAnimation(LivingMotions.SLEEP, Animations.BIPED_SLEEPING);
        animator.addLivingAnimation(LivingMotions.CREATIVE_FLY, Animations.BIPED_CREATIVE_FLYING);
        animator.addLivingAnimation(LivingMotions.CREATIVE_IDLE, Animations.BIPED_CREATIVE_IDLE);
        animator.addLivingAnimation(LivingMotions.DIGGING, Animations.BIPED_DIG);
        animator.addLivingAnimation(LivingMotions.AIM, Animations.BIPED_BOW_AIM);
        animator.addLivingAnimation(LivingMotions.SHOT, Animations.BIPED_BOW_SHOT);
        animator.addLivingAnimation(LivingMotions.DRINK, Animations.BIPED_DRINK);
        animator.addLivingAnimation(LivingMotions.EAT, Animations.BIPED_EAT);
        animator.addLivingAnimation(LivingMotions.SPECTATE, Animations.BIPED_SPYGLASS_USE);
    }

    public void copySkillsFrom(PlayerPatch<?> old, boolean isDeath) {
        this.getSkillCapability().copyFrom(old.getSkillCapability());
        if (!isDeath) {
            old.getSkillCapability().listSkillContainers().forEach(skillContainer -> skillContainer.transferDataTo(this.getSkillCapability().getSkillContainerFor(skillContainer.getSlot())));
        }
    }

    public void setModelYRot(float rotDeg, boolean sendPacket) {
        this.useModelYRot = true;
        this.modelYRot = rotDeg;
    }

    public void disableModelYRot(boolean sendPacket) {
        this.useModelYRot = false;
    }

    @Override
    public OpenMatrix4f getModelMatrix(float partialTicks) {
        float yRot;
        float oYRot;
        float scale = (((Player)this.original).m_6162_() ? 0.5f : 1.0f) * 0.9375f;
        Entity entity = ((Player)this.original).m_20202_();
        if (entity instanceof LivingEntity) {
            LivingEntity ridingEntity = (LivingEntity)entity;
            oYRot = ridingEntity.f_20884_;
            yRot = ridingEntity.f_20883_;
        } else {
            oYRot = this.modelYRotO;
            yRot = this.modelYRot;
        }
        return MathUtils.getModelMatrixIntegral(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, oYRot, yRot, partialTicks, scale, scale, scale);
    }

    @Override
    public void serverTick(LivingEvent.LivingTickEvent event) {
        super.serverTick(event);
        if (this.state.canBasicAttack()) {
            ++this.tickSinceLastAction;
        }
        if (!this.state.inaction() && this.staminaRegenAwaitTicks > 0) {
            --this.staminaRegenAwaitTicks;
        }
        float stamina = this.getStamina();
        float maxStamina = this.getMaxStamina();
        float staminaRegen = (float)((Player)this.original).m_21133_((Attribute)EpicFightAttributes.STAMINA_REGEN.get());
        if (staminaRegen > 0.0f) {
            int regenWhenLessThan = 30 - 900 / (int)(30.0f * staminaRegen);
            if (stamina < maxStamina && this.staminaRegenAwaitTicks <= regenWhenLessThan) {
                float staminaFactor = 1.0f + (float)Math.pow(stamina / (maxStamina - stamina * 0.5f), 2.0);
                this.setStamina(stamina + maxStamina * 0.01f * staminaFactor * staminaRegen);
            }
        }
        if (maxStamina < stamina) {
            this.setStamina(maxStamina);
        }
    }

    @Override
    public void tick(LivingEvent.LivingTickEvent event) {
        if (this.playerMode == PlayerMode.EPICFIGHT || this.battleModeRestricted) {
            BattleModeSustainableEvent battleModeSustainableEvent = new BattleModeSustainableEvent(this);
            MinecraftForge.EVENT_BUS.post((Event)battleModeSustainableEvent);
            if (battleModeSustainableEvent.isCanceled()) {
                if (this.playerMode == PlayerMode.EPICFIGHT) {
                    this.toVanillaMode(false);
                    this.battleModeRestricted = true;
                }
            } else if (this.battleModeRestricted) {
                this.battleModeRestricted = false;
                this.toEpicFightMode(false);
            }
        }
        if (!this.isLogicalClient() || ((Player)this.original).m_7578_()) {
            this.getSkillCapability().listSkillContainers().forEach(SkillContainer::update);
        }
        this.modelYRotO = this.modelYRot;
        super.tick(event);
        if (!this.state.canUseItem()) {
            this.cancelItemUse();
        }
        if (this.getEntityState().turningLocked()) {
            if (!this.useModelYRot) {
                this.setModelYRot(((Player)this.original).m_146908_(), false);
            }
        } else if (this.useModelYRot) {
            this.disableModelYRot(false);
        }
        if (this.getEntityState().inaction() && ((Player)this.original).m_275832_() == null) {
            ((Player)this.original).f_20883_ = ((Player)this.original).m_146908_();
            ((Player)this.original).f_20885_ = ((Player)this.original).m_146908_();
        }
        if (!this.useModelYRot) {
            float originalYRot = this.isLogicalClient() ? ((Player)this.original).f_20883_ : ((Player)this.original).m_146908_();
            this.modelYRot += Mth.m_14036_((float)Mth.m_14177_((float)(originalYRot - this.modelYRot)), (float)-45.0f, (float)45.0f);
        }
        this.xo = ((Player)this.original).m_20185_();
        this.yo = ((Player)this.original).m_20186_();
        this.zo = ((Player)this.original).m_20189_();
    }

    public SkillContainer getSkill(Skill skill) {
        if (skill == null) {
            return null;
        }
        return this.getSkillCapability().getSkillContainer(skill);
    }

    public Optional<SkillContainer> getSkillContainerFor(Skill skill) {
        if (skill == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.getSkillCapability().getSkillContainer(skill));
    }

    public SkillContainer getSkill(SkillSlot slot) {
        return this.getSkill(slot.universalOrdinal());
    }

    public SkillContainer getSkill(int slotIndex) {
        return this.getSkillCapability().getSkillContainerFor(slotIndex);
    }

    public CapabilitySkill getSkillCapability() {
        return (CapabilitySkill)((Player)this.original).getCapability(EpicFightCapabilities.CAPABILITY_SKILL).orElse((Object)CapabilitySkill.EMPTY);
    }

    public PlayerEventListener getEventListener() {
        return this.eventListeners;
    }

    @Override
    public float getModifiedBaseDamage(float baseDamage) {
        ModifyBaseDamageEvent<PlayerPatch> event = new ModifyBaseDamageEvent<PlayerPatch>(this, baseDamage, ValueModifier.calculator());
        this.getEventListener().triggerEvents(PlayerEventListener.EventType.MODIFY_DAMAGE_EVENT, event);
        return event.calculateModifiedDamage();
    }

    public float getAttackSpeed(InteractionHand hand) {
        float baseSpeed = hand == InteractionHand.MAIN_HAND ? (float)((Player)this.original).m_21133_(Attributes.f_22283_) : (float)(this.isOffhandItemValid() ? ((Player)this.original).m_21133_((Attribute)EpicFightAttributes.OFFHAND_ATTACK_SPEED.get()) : ((Player)this.original).m_21172_(Attributes.f_22283_));
        return this.getModifiedAttackSpeed(this.getAdvancedHoldingItemCapability(hand), baseSpeed);
    }

    public float getModifiedAttackSpeed(CapabilityItem itemCapability, float baseSpeed) {
        ModifyAttackSpeedEvent event = new ModifyAttackSpeedEvent(this, itemCapability, baseSpeed);
        this.eventListeners.triggerEvents(PlayerEventListener.EventType.MODIFY_ATTACK_SPEED_EVENT, event);
        float weight = this.getWeight();
        if (weight > 40.0f) {
            float attenuation = (float)Mth.m_14045_((int)EpicFightGameRules.WEIGHT_PENALTY.getRuleValue(((Player)this.getOriginal()).m_9236_()), (int)0, (int)100) / 100.0f;
            return event.getAttackSpeed() + -0.1f * (weight / 40.0f) * Math.max(event.getAttackSpeed() - 0.8f, 0.0f) * attenuation;
        }
        return event.getAttackSpeed();
    }

    public double getWeaponAttribute(Attribute attribute, ItemStack itemstack) {
        AttributeInstance attrInstance = new AttributeInstance(attribute, ai -> {});
        Set<AttributeModifier> itemModifiers = Set.copyOf(CapabilityItem.getAttributeModifiers(attribute, EquipmentSlot.MAINHAND, itemstack, this));
        Set<AttributeModifier> mainhandModifiers = Set.copyOf(CapabilityItem.getAttributeModifiers(attribute, EquipmentSlot.MAINHAND, ((Player)this.original).m_21205_(), this));
        double baseValue = ((Player)this.original).m_21051_(attribute) == null ? attribute.m_22082_() : Objects.requireNonNull(((Player)this.original).m_21051_(attribute)).m_22115_();
        attrInstance.m_22100_(baseValue);
        for (AttributeModifier modifier : Objects.requireNonNull(((Player)this.original).m_21051_(attribute)).m_22122_()) {
            if (itemModifiers.contains(modifier) || mainhandModifiers.contains(modifier)) continue;
            attrInstance.m_22118_(modifier);
        }
        for (AttributeModifier modifier : itemModifiers) {
            if (attrInstance.m_22109_(modifier)) continue;
            attrInstance.m_22118_(modifier);
        }
        CapabilityItem itemCapability = EpicFightCapabilities.getItemStackCapabilityOr(itemstack, null);
        if (itemCapability != null) {
            for (AttributeModifier modifier : itemCapability.getAttributeModifiers(EquipmentSlot.MAINHAND, this).get((Object)attribute)) {
                if (attrInstance.m_22109_(modifier)) continue;
                attrInstance.m_22118_(modifier);
            }
        }
        return attrInstance.m_22135_();
    }

    @Override
    public AttackResult attack(EpicFightDamageSource damageSource, Entity target, InteractionHand hand) {
        float fallDist = ((Player)this.original).f_19789_;
        boolean onGround = ((Player)this.original).f_19861_;
        boolean offhandValid = this.isOffhandItemValid();
        ItemStack mainHandItem = ((Player)this.getOriginal()).m_21205_();
        ItemStack offHandItem = ((Player)this.getOriginal()).m_21206_();
        List<AttributeModifier> mainHandAttributes = CapabilityItem.getAttributeModifiers(Attributes.f_22281_, EquipmentSlot.MAINHAND, ((Player)this.original).m_21205_(), this);
        Set<AttributeModifier> offHandAttributes = this.isOffhandItemValid() ? CapabilityItem.getAttributeModifiers(Attributes.f_22281_, EquipmentSlot.MAINHAND, ((Player)this.original).m_21206_(), this) : Set.of();
        this.epicFightDamageSource = damageSource;
        ((Player)this.original).f_20922_ = Integer.MAX_VALUE;
        ((Player)this.original).f_19789_ = 0.0f;
        ((Player)this.original).f_19861_ = false;
        this.setOffhandDamage(hand, mainHandItem, offHandItem, offhandValid, mainHandAttributes, offHandAttributes);
        ((Player)this.original).m_5706_(target);
        this.recoverMainhandDamage(hand, mainHandItem, offHandItem, mainHandAttributes, offHandAttributes);
        this.epicFightDamageSource = null;
        ((Player)this.original).f_19789_ = fallDist;
        ((Player)this.original).f_19861_ = onGround;
        return super.attack(damageSource, target, hand);
    }

    @Override
    public EpicFightDamageSource getDamageSource(AnimationManager.AnimationAccessor<? extends StaticAnimation> animation, InteractionHand hand) {
        EpicFightDamageSource damagesource = EpicFightDamageSources.playerAttack((Player)this.original).setAnimation(animation).setBaseArmorNegation(this.getArmorNegation(hand)).setBaseImpact(this.getImpact(hand)).setUsedItem(((Player)this.getOriginal()).m_21120_(hand));
        boolean isBasicAttack = ((StaticAnimation)animation.get()).isBasicAttackAnimation() || ((Animator)this.getAnimator()).getVariables().get(BasicAttack.COMBO, animation).orElse(false) != false;
        damagesource.setBasicAttack(isBasicAttack);
        return damagesource;
    }

    @Override
    public void cancelItemUse() {
        super.cancelItemUse();
        this.resetHolding();
    }

    public float getMaxStamina() {
        AttributeInstance maxStamina = ((Player)this.original).m_21051_((Attribute)EpicFightAttributes.MAX_STAMINA.get());
        return (float)(maxStamina == null ? 0.0 : maxStamina.m_22135_());
    }

    public float getStamina() {
        return this.getMaxStamina() <= 0.0f ? 0.0f : (((Player)this.original).m_20088_().m_285897_(STAMINA) ? ((Float)((Player)this.original).m_20088_().m_135370_(STAMINA)).floatValue() : 0.0f);
    }

    public float getModifiedStaminaConsume(float amount) {
        float attenuation = (float)Mth.m_14045_((int)EpicFightGameRules.WEIGHT_PENALTY.getRuleValue(((Player)this.getOriginal()).m_9236_()), (int)0, (int)100) / 100.0f;
        float weight = this.getWeight();
        return ((weight / 40.0f - 1.0f) * attenuation + 1.0f) * amount;
    }

    public boolean hasStamina(float amount) {
        return this.getStamina() >= amount;
    }

    public void setStamina(float value) {
        if (((Player)this.original).m_20088_().m_285897_(STAMINA)) {
            float f1 = Mth.m_14036_((float)value, (float)0.0f, (float)this.getMaxStamina());
            ((Player)this.original).m_20088_().m_135381_(STAMINA, (Object)Float.valueOf(f1));
        }
    }

    public void clampMaxAttributes() {
        float maxStamina;
        float currentStamina;
        float maxHealth;
        float currentHealth = ((Player)this.original).m_21223_();
        if (currentHealth > (maxHealth = ((Player)this.original).m_21233_())) {
            ((Player)this.original).m_21153_(maxHealth);
        }
        if ((currentStamina = this.getStamina()) > (maxStamina = this.getMaxStamina())) {
            this.setStamina(maxStamina);
        }
    }

    public boolean consumeForSkill(Skill skill, Skill.Resource consumeResource) {
        return this.consumeForSkill(skill, consumeResource, skill.getDefaultConsumptionAmount(this));
    }

    public boolean consumeForSkill(Skill skill, Skill.Resource consumeResource, float amount) {
        return this.consumeForSkill(skill, consumeResource, amount, false, null);
    }

    @ApiStatus.Internal
    public boolean consumeForSkill(Skill skill, Skill.Resource consumeResource, @Nullable FriendlyByteBuf args) {
        return this.consumeForSkill(skill, consumeResource, skill.getDefaultConsumptionAmount(this), false, args);
    }

    public boolean consumeForSkill(Skill skill, Skill.Resource consumeResource, float amount, boolean activateConsumeForce, @Nullable FriendlyByteBuf args) {
        Optional<SkillContainer> oContainer = this.getSkillContainerFor(skill);
        if (oContainer.isEmpty()) {
            return false;
        }
        SkillContainer skillContainer = oContainer.get();
        SkillConsumeEvent skillConsumeEvent = new SkillConsumeEvent(this, skill, consumeResource, amount, args);
        this.getEventListener().triggerEvents(PlayerEventListener.EventType.SKILL_CONSUME_EVENT, skillConsumeEvent);
        if (skillConsumeEvent.isCanceled()) {
            return false;
        }
        float modifiedAmount = skillConsumeEvent.getAmount();
        if (args != null) {
            args.resetReaderIndex();
        }
        if (skillConsumeEvent.getResourceType().predicate.canExecute(skillContainer, this, modifiedAmount)) {
            if (!this.isLogicalClient()) {
                skillConsumeEvent.getResourceType().consumer.consume(skillContainer, (ServerPlayerPatch)this, modifiedAmount);
            }
            return true;
        }
        if (activateConsumeForce && !this.isLogicalClient()) {
            skillConsumeEvent.getResourceType().consumer.consume(skillContainer, (ServerPlayerPatch)this, modifiedAmount);
        }
        return false;
    }

    public void resetActionTick() {
        this.tickSinceLastAction = 0;
        this.staminaRegenAwaitTicks = 30;
    }

    public int getTickSinceLastAction() {
        return this.tickSinceLastAction;
    }

    public void setStaminaRegenAwaitTicks(int tick) {
        this.staminaRegenAwaitTicks = tick;
    }

    public int getStaminaRegenAwaitTicks() {
        return this.staminaRegenAwaitTicks;
    }

    public boolean startSkillHolding(HoldableSkill holdableSkill) {
        Optional<SkillContainer> containerOptional = this.getSkillContainerFor(holdableSkill.asSkill());
        if (containerOptional.isEmpty()) {
            return false;
        }
        holdableSkill.startHolding(containerOptional.get());
        this.lastChargingTick = ((Player)this.original).f_19797_;
        this.holdingSkill = holdableSkill;
        return true;
    }

    public void resetHolding() {
        if (this.holdingSkill != null) {
            if (this.holdingSkill instanceof ChargeableSkill) {
                this.chargingAmount = 0;
            }
            this.holdingSkill.resetHolding(this.getSkill(this.holdingSkill.asSkill()));
            this.holdingSkill = null;
        }
    }

    public boolean isHoldingAny() {
        return this.holdingSkill != null;
    }

    public boolean isHoldingSkill(Skill holdingSkill) {
        return this.holdingSkill == holdingSkill;
    }

    public int getLastChargingTick() {
        return this.lastChargingTick;
    }

    public void setChargingAmount(int amount) {
        HoldableSkill holdableSkill;
        if (this.isHoldingAny() && (holdableSkill = this.getHoldingSkill()) instanceof ChargeableSkill) {
            ChargeableSkill chargeableSkill = (ChargeableSkill)holdableSkill;
            this.chargingAmount = Math.min(amount, chargeableSkill.getMaxChargingTicks());
        } else {
            this.chargingAmount = 0;
        }
    }

    public int getChargingAmount() {
        return this.chargingAmount;
    }

    public float getSkillChargingTicks(float partialTicks) {
        return this.isHoldingAny() ? (float)(((Player)this.original).f_19797_ - this.getLastChargingTick()) - 1.0f + partialTicks : 0.0f;
    }

    public int getSkillChargingTicks() {
        int n;
        HoldableSkill holdableSkill;
        if (this.isHoldingAny() && (holdableSkill = this.holdingSkill) instanceof ChargeableSkill) {
            ChargeableSkill chargingSkill = (ChargeableSkill)holdableSkill;
            n = Math.min(((Player)this.original).f_19797_ - this.getLastChargingTick(), chargingSkill.getMaxChargingTicks());
        } else {
            n = 0;
        }
        return n;
    }

    public int getAccumulatedChargeAmount() {
        return this.getHoldingSkill() instanceof ChargeableSkill ? this.getChargingAmount() : 0;
    }

    public HoldableSkill getHoldingSkill() {
        return this.holdingSkill;
    }

    public boolean isInAir() {
        return ((Player)this.original).m_21255_() || this.currentLivingMotion == LivingMotions.FALL;
    }

    @Override
    public boolean shouldMoveOnCurrentSide(ActionAnimation actionAnimation) {
        return this.isLogicalClient();
    }

    public void openSkillBook(ItemStack itemstack, InteractionHand hand) {
    }

    @Override
    public void onFall(LivingFallEvent event) {
        FallEvent fallEvent = new FallEvent(this, event);
        this.getEventListener().triggerEvents(PlayerEventListener.EventType.FALL_EVENT, fallEvent);
        super.onFall(event);
        this.setAirborneState(false);
    }

    public void toggleMode() {
        switch (this.playerMode) {
            case VANILLA: {
                this.toEpicFightMode(true);
                break;
            }
            case EPICFIGHT: {
                this.toVanillaMode(true);
            }
        }
    }

    public void toMode(PlayerMode playerMode, boolean synchronize) {
        switch (playerMode) {
            case VANILLA: {
                this.toVanillaMode(synchronize);
                break;
            }
            case EPICFIGHT: {
                this.toEpicFightMode(synchronize);
            }
        }
    }

    public PlayerMode getPlayerMode() {
        return this.playerMode;
    }

    public void toVanillaMode(boolean synchronize) {
        ChangePlayerModeEvent prepareModelEvent;
        if (this.playerMode == PlayerMode.VANILLA) {
            return;
        }
        if (this.battleModeRestricted) {
            this.battleModeRestricted = false;
        }
        if (!MinecraftForge.EVENT_BUS.post((Event)(prepareModelEvent = new ChangePlayerModeEvent(this, PlayerMode.VANILLA)))) {
            this.playerMode = prepareModelEvent.getPlayerMode();
        }
    }

    public void toEpicFightMode(boolean synchronize) {
        if (this.playerMode == PlayerMode.EPICFIGHT) {
            return;
        }
        ChangePlayerModeEvent prepareModelEvent = new ChangePlayerModeEvent(this, PlayerMode.EPICFIGHT);
        if (!MinecraftForge.EVENT_BUS.post((Event)prepareModelEvent)) {
            this.playerMode = prepareModelEvent.getPlayerMode();
        }
    }

    public boolean isEpicFightMode() {
        return this.playerMode == PlayerMode.EPICFIGHT;
    }

    public boolean isVanillaMode() {
        return this.playerMode == PlayerMode.VANILLA;
    }

    @Override
    public double getXOld() {
        return this.xo;
    }

    @Override
    public double getYOld() {
        return this.yo;
    }

    @Override
    public double getZOld() {
        return this.zo;
    }

    @Override
    public float getYRot() {
        return this.modelYRot;
    }

    @Override
    public float getYRotO() {
        return this.modelYRotO;
    }

    @Override
    public void setYRotO(float yRot) {
        this.modelYRotO = yRot;
    }

    @Override
    public void setYRot(float yRot) {
        this.setModelYRot(yRot, true);
    }

    @Override
    public float getYRotLimit() {
        return 180.0f;
    }

    public AnimationManager.AnimationAccessor<? extends StaticAnimation> getHitAnimation(StunType stunType) {
        if (((Player)this.original).m_20202_() != null) {
            return Animations.BIPED_HIT_ON_MOUNT;
        }
        return switch (stunType) {
            case StunType.LONG -> Animations.BIPED_HIT_LONG;
            case StunType.SHORT, StunType.HOLD -> Animations.BIPED_HIT_SHORT;
            case StunType.KNOCKDOWN -> Animations.BIPED_KNOCKDOWN;
            case StunType.NEUTRALIZE -> Animations.BIPED_COMMON_NEUTRALIZED;
            case StunType.FALL -> Animations.BIPED_LANDING;
            case StunType.NONE -> null;
            default -> null;
        };
    }

    public double checkXTurn(double xRot) {
        return xRot;
    }

    public double checkYTurn(double yRot) {
        return yRot;
    }

    @Override
    public Faction getFaction() {
        return Factions.NEUTRAL;
    }

    static {
        PLAYER_EVENT_UUID = UUID.fromString("e6beeac4-77d2-11eb-9439-0242ac130002");
    }

    public static enum PlayerMode {
        VANILLA,
        EPICFIGHT;

    }
}


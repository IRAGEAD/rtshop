/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Maps
 *  com.mojang.datafixers.util.Pair
 *  javax.annotation.Nullable
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.entity.ai.attributes.Attribute
 *  net.minecraft.world.entity.ai.attributes.AttributeModifier
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.UseAnim
 */
package yesman.epicfight.world.capabilities.item;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.MainFrameAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.gameasset.EpicFightSounds;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.particle.EpicFightParticles;
import yesman.epicfight.particle.HitParticleType;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.Style;
import yesman.epicfight.world.capabilities.item.WeaponCategory;
import yesman.epicfight.world.entity.eventlistener.ComboCounterHandleEvent;

public class WeaponCapability
extends CapabilityItem {
    @Deprecated(since="1.21.1", forRemoval=true)
    protected final Function<LivingEntityPatch<?>, Style> stylegetter;
    @Deprecated(since="1.21.1", forRemoval=true)
    protected final Function<LivingEntityPatch<?>, Boolean> weaponCombinationPredicator;
    @Deprecated(since="1.21.1", forRemoval=true)
    protected final Skill passiveSkill;
    protected final SoundEvent smashingSound;
    protected final SoundEvent hitSound;
    protected final HitParticleType hitParticle;
    @Deprecated(since="1.21.1", forRemoval=true)
    protected final Map<Style, List<AnimationManager.AnimationAccessor<? extends AttackAnimation>>> autoAttackMotions;
    @Deprecated(since="1.21.1", forRemoval=true)
    protected final Map<Style, Function<ItemStack, Skill>> innateSkill;
    @Deprecated(since="1.21.1", forRemoval=true)
    protected final Map<Style, Map<LivingMotion, AnimationManager.AnimationAccessor<? extends StaticAnimation>>> livingMotionModifiers;
    protected final boolean canBePlacedOffhand;
    @Deprecated
    protected final Function<Style, Boolean> comboCancel;
    protected final ComboCounterHandleEvent.ComboCounterHandler comboCounterHandler;
    protected final CapabilityItem.ZoomInType zoomInType;
    protected final float reach;
    protected Set<ResourceLocation> customTags;

    protected WeaponCapability(CapabilityItem.Builder builder) {
        super(builder);
        Builder weaponBuilder = (Builder)builder;
        this.autoAttackMotions = weaponBuilder.autoAttackMotionMap;
        this.innateSkill = weaponBuilder.innateSkillByStyle;
        this.livingMotionModifiers = weaponBuilder.livingMotionModifiers;
        this.stylegetter = weaponBuilder.styleProvider;
        this.weaponCombinationPredicator = weaponBuilder.weaponCombinationPredicator;
        this.passiveSkill = weaponBuilder.passiveSkill;
        this.smashingSound = weaponBuilder.swingSound;
        this.hitParticle = weaponBuilder.hitParticle;
        this.hitSound = weaponBuilder.hitSound;
        this.canBePlacedOffhand = weaponBuilder.canBePlacedOffhand;
        this.comboCancel = weaponBuilder.comboCancel;
        this.comboCounterHandler = weaponBuilder.comboCounterHandler;
        this.zoomInType = weaponBuilder.zoomInType;
        this.reach = weaponBuilder.reach;
        this.customTags = Collections.unmodifiableSet(weaponBuilder.customTags);
    }

    @Override
    public final List<AnimationManager.AnimationAccessor<? extends AttackAnimation>> getAutoAttackMotion(PlayerPatch<?> playerpatch) {
        return this.autoAttackMotions.getOrDefault(this.getStyle(playerpatch), this.autoAttackMotions.get(CapabilityItem.Styles.COMMON));
    }

    @Override
    public final Skill getInnateSkill(PlayerPatch<?> playerpatch, ItemStack itemstack) {
        Function<ItemStack, Skill> innateProvider = this.innateSkill.getOrDefault(this.getStyle(playerpatch), this.innateSkill.get(CapabilityItem.Styles.COMMON));
        return innateProvider == null ? null : innateProvider.apply(itemstack);
    }

    @Override
    @Deprecated(since="1.21.1", forRemoval=true)
    public Skill getPassiveSkill() {
        return this.passiveSkill;
    }

    @Override
    @Deprecated(since="1.21.1", forRemoval=true)
    public final List<AnimationManager.AnimationAccessor<? extends AttackAnimation>> getMountAttackMotion() {
        return this.autoAttackMotions.get(CapabilityItem.Styles.MOUNT);
    }

    @Override
    public Style getStyle(LivingEntityPatch<?> entitypatch) {
        return this.stylegetter.apply(entitypatch);
    }

    @Override
    public SoundEvent getSmashingSound() {
        return this.smashingSound;
    }

    @Override
    public SoundEvent getHitSound() {
        return this.hitSound;
    }

    @Override
    public HitParticleType getHitParticle() {
        return this.hitParticle;
    }

    @Override
    public boolean canBePlacedOffhand() {
        return this.canBePlacedOffhand;
    }

    @Override
    public boolean shouldCancelCombo(LivingEntityPatch<?> entitypatch) {
        return this.comboCancel.apply(this.getStyle(entitypatch));
    }

    @Override
    public int handleComboCounter(ComboCounterHandleEvent.Causal causal, PlayerPatch<?> entitypatch, @Nullable AnimationManager.AnimationAccessor<? extends MainFrameAnimation> nextAnimation, int original) {
        return this.comboCounterHandler.handleComboCounter(this, causal, entitypatch, nextAnimation, original);
    }

    @Override
    public CapabilityItem.ZoomInType getZoomInType() {
        return this.zoomInType;
    }

    @Override
    public Map<LivingMotion, AnimationManager.AnimationAccessor<? extends StaticAnimation>> getLivingMotionModifier(LivingEntityPatch<?> player, InteractionHand hand) {
        if (this.livingMotionModifiers == null || hand == InteractionHand.OFF_HAND) {
            return super.getLivingMotionModifier(player, hand);
        }
        Map<LivingMotion, AnimationManager.AnimationAccessor<? extends StaticAnimation>> motions = this.livingMotionModifiers.getOrDefault(this.getStyle(player), Maps.newHashMap());
        this.livingMotionModifiers.getOrDefault(CapabilityItem.Styles.COMMON, Maps.newHashMap()).forEach(motions::putIfAbsent);
        return motions;
    }

    @Override
    public UseAnim getUseAnimation(LivingEntityPatch<?> playerpatch) {
        Style style;
        if (this.livingMotionModifiers != null && this.livingMotionModifiers.containsKey(style = this.getStyle(playerpatch)) && this.livingMotionModifiers.get(style).containsKey(LivingMotions.BLOCK)) {
            return UseAnim.BLOCK;
        }
        return UseAnim.NONE;
    }

    @Override
    public boolean canHoldInOffhandAlone() {
        return false;
    }

    @Override
    public boolean checkOffhandValid(LivingEntityPatch<?> entitypatch) {
        return super.checkOffhandValid(entitypatch) || this.weaponCombinationPredicator.apply(entitypatch) != false;
    }

    @Override
    @Deprecated(since="1.21.1", forRemoval=true)
    public boolean availableOnHorse() {
        return this.autoAttackMotions.containsKey(CapabilityItem.Styles.MOUNT);
    }

    @Override
    public float getReach() {
        return this.reach;
    }

    public boolean hasMatchingTag(ResourceLocation rl) {
        return this.customTags.contains(rl);
    }

    public Set<ResourceLocation> getTags() {
        return this.customTags;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder
    extends CapabilityItem.Builder {
        @Deprecated(since="1.21.1", forRemoval=true)
        Function<LivingEntityPatch<?>, Style> styleProvider;
        @Deprecated(since="1.21.1", forRemoval=true)
        Function<LivingEntityPatch<?>, Boolean> weaponCombinationPredicator;
        @Deprecated(since="1.21.1", forRemoval=true)
        Skill passiveSkill;
        SoundEvent swingSound;
        SoundEvent hitSound;
        HitParticleType hitParticle;
        @Deprecated(since="1.21.1", forRemoval=true)
        Map<Style, List<AnimationManager.AnimationAccessor<? extends AttackAnimation>>> autoAttackMotionMap;
        @Deprecated(since="1.21.1", forRemoval=true)
        Map<Style, Function<ItemStack, Skill>> innateSkillByStyle;
        @Deprecated(since="1.21.1", forRemoval=true)
        Map<Style, Map<LivingMotion, AnimationManager.AnimationAccessor<? extends StaticAnimation>>> livingMotionModifiers;
        Function<Style, Boolean> comboCancel;
        ComboCounterHandleEvent.ComboCounterHandler comboCounterHandler;
        boolean canBePlacedOffhand;
        CapabilityItem.ZoomInType zoomInType;
        float reach;
        Set<ResourceLocation> customTags = new HashSet<ResourceLocation>();

        protected Builder() {
            this.constructor = WeaponCapability::new;
            this.styleProvider = entitypatch -> CapabilityItem.Styles.ONE_HAND;
            this.weaponCombinationPredicator = entitypatch -> false;
            this.passiveSkill = null;
            this.swingSound = (SoundEvent)EpicFightSounds.WHOOSH.get();
            this.hitSound = (SoundEvent)EpicFightSounds.BLUNT_HIT.get();
            this.hitParticle = (HitParticleType)((Object)EpicFightParticles.HIT_BLADE.get());
            this.autoAttackMotionMap = Maps.newHashMap();
            this.innateSkillByStyle = Maps.newHashMap();
            this.livingMotionModifiers = null;
            this.canBePlacedOffhand = true;
            this.comboCancel = style -> true;
            this.comboCounterHandler = ComboCounterHandleEvent.ComboCounterHandler.DEFAULT_COMBO_HANDLER;
            this.zoomInType = CapabilityItem.ZoomInType.NONE;
            this.reach = 0.2f;
        }

        @Override
        public Builder category(WeaponCategory category) {
            super.category(category);
            return this;
        }

        @Deprecated(since="1.21.1", forRemoval=true)
        public Builder styleProvider(Function<LivingEntityPatch<?>, Style> styleProvider) {
            this.styleProvider = styleProvider;
            return this;
        }

        @Deprecated(since="1.21.1", forRemoval=true)
        public Builder passiveSkill(Skill passiveSkill) {
            this.passiveSkill = passiveSkill;
            return this;
        }

        public Builder swingSound(SoundEvent swingSound) {
            this.swingSound = swingSound;
            return this;
        }

        public Builder hitSound(SoundEvent hitSound) {
            this.hitSound = hitSound;
            return this;
        }

        public Builder hitParticle(HitParticleType hitParticle) {
            this.hitParticle = hitParticle;
            return this;
        }

        @Override
        public Builder collider(Collider collider) {
            this.collider = collider;
            return this;
        }

        public Builder canBePlacedOffhand(boolean canBePlacedOffhand) {
            this.canBePlacedOffhand = canBePlacedOffhand;
            return this;
        }

        public Builder reach(float reach) {
            this.reach = reach;
            return this;
        }

        public Builder addTag(ResourceLocation customTag) {
            this.customTags.add(customTag);
            return this;
        }

        @Deprecated(since="1.21.1", forRemoval=true)
        public Builder livingMotionModifier(Style wieldStyle, LivingMotion livingMotion, AnimationManager.AnimationAccessor<? extends StaticAnimation> animation) {
            if (AnimationManager.checkNull(animation)) {
                EpicFightMod.LOGGER.warn("Unable to put an empty animation to weapon capability builder: " + String.valueOf(livingMotion) + ", " + String.valueOf(animation));
                return this;
            }
            if (this.livingMotionModifiers == null) {
                this.livingMotionModifiers = Maps.newHashMap();
            }
            if (!this.livingMotionModifiers.containsKey(wieldStyle)) {
                this.livingMotionModifiers.put(wieldStyle, Maps.newHashMap());
            }
            this.livingMotionModifiers.get(wieldStyle).put(livingMotion, animation);
            return this;
        }

        @Override
        public Builder addStyleAttibutes(Style style, Pair<Attribute, AttributeModifier> attributePair) {
            super.addStyleAttibutes(style, attributePair);
            return this;
        }

        @SafeVarargs
        @Deprecated(since="1.21.1", forRemoval=true)
        public final Builder newStyleCombo(Style style, AnimationManager.AnimationAccessor<? extends AttackAnimation> ... animation) {
            this.autoAttackMotionMap.put(style, Lists.newArrayList((Object[])animation));
            return this;
        }

        @Deprecated(since="1.21.1", forRemoval=true)
        public Builder weaponCombinationPredicator(Function<LivingEntityPatch<?>, Boolean> predicator) {
            this.weaponCombinationPredicator = predicator;
            return this;
        }

        @Deprecated(since="1.21.1")
        public Builder innateSkill(Style style, Function<ItemStack, Skill> innateSkill) {
            this.innateSkillByStyle.put(style, innateSkill);
            return this;
        }

        @Deprecated
        public Builder comboCancel(Function<Style, Boolean> comboCancel) {
            this.comboCancel = comboCancel;
            return this;
        }

        public Builder comboCounterHandler(ComboCounterHandleEvent.ComboCounterHandler comboHandler) {
            this.comboCounterHandler = comboHandler;
            return this;
        }

        public Builder zoomInType(CapabilityItem.ZoomInType zoomInType) {
            this.zoomInType = zoomInType;
            return this;
        }

        public Map<Style, List<AnimationManager.AnimationAccessor<? extends AttackAnimation>>> getComboAnimations() {
            return ImmutableMap.copyOf(this.autoAttackMotionMap);
        }
    }
}


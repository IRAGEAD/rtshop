/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.HashMultimap
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableMap$Builder
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Maps
 *  com.google.common.collect.Multimap
 *  com.mojang.datafixers.util.Pair
 *  javax.annotation.Nullable
 *  net.minecraft.ChatFormatting
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.contents.TranslatableContents
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EquipmentSlot
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.ai.attributes.Attribute
 *  net.minecraft.world.entity.ai.attributes.AttributeModifier
 *  net.minecraft.world.entity.ai.attributes.Attributes
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.UseAnim
 *  net.minecraft.world.item.enchantment.Enchantments
 */
package yesman.epicfight.world.capabilities.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.Enchantments;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.MainFrameAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.gameasset.ColliderPreset;
import yesman.epicfight.gameasset.EpicFightSounds;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.server.SPChangeSkill;
import yesman.epicfight.network.server.SPSetRemotePlayerSkill;
import yesman.epicfight.network.server.SPSetSkillContainerValue;
import yesman.epicfight.particle.EpicFightParticles;
import yesman.epicfight.particle.HitParticleType;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.skill.guard.GuardSkill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.RangedWeaponCapability;
import yesman.epicfight.world.capabilities.item.Style;
import yesman.epicfight.world.capabilities.item.WeaponCategory;
import yesman.epicfight.world.entity.ai.attribute.EpicFightAttributes;
import yesman.epicfight.world.entity.eventlistener.ComboCounterHandleEvent;

public class CapabilityItem {
    public static CapabilityItem EMPTY = CapabilityItem.builder().build();
    protected static List<AnimationManager.AnimationAccessor<? extends AttackAnimation>> commonAutoAttackMotion = Lists.newArrayList();
    protected final WeaponCategory weaponCategory;
    protected Map<Style, Map<Attribute, AttributeModifier>> attributeMap;
    protected Collider collider;

    public static List<AnimationManager.AnimationAccessor<? extends AttackAnimation>> getBasicAutoAttackMotion() {
        return commonAutoAttackMotion;
    }

    public static List<AttributeModifier> getAttributeModifiers(Attribute attribute, EquipmentSlot slot, ItemStack itemstack, @Nullable LivingEntityPatch<?> entitypatch) {
        ArrayList attributeModifiers = Lists.newArrayList();
        itemstack.m_41638_(slot).forEach((attribute$1, modifier) -> {
            if (attribute$1 == attribute) {
                attributeModifiers.add(modifier);
            }
        });
        CapabilityItem itemCap = EpicFightCapabilities.getItemStackCapability(itemstack);
        if (!itemCap.isEmpty()) {
            itemCap.getAttributeModifiers(slot, entitypatch).forEach((attribute$1, modifier) -> {
                if (attribute$1 == attribute) {
                    attributeModifiers.add(modifier);
                }
            });
        }
        return attributeModifiers;
    }

    protected static boolean validateAttribute(LivingEntityPatch<?> patch, Attribute attributeHolder) {
        return ((LivingEntity)patch.getOriginal()).m_21204_().m_22171_(attributeHolder);
    }

    protected CapabilityItem(Builder builder) {
        this.weaponCategory = builder.category;
        this.collider = builder.collider;
        ImmutableMap.Builder attributeMapbuilder = ImmutableMap.builder();
        for (Map.Entry<Style, Map<Attribute, AttributeModifier>> entry : builder.attributeMap.entrySet()) {
            attributeMapbuilder.put((Object)entry.getKey(), entry.getValue());
        }
        this.attributeMap = attributeMapbuilder.build();
    }

    public void modifyItemTooltip(ItemStack itemstack, List<Component> itemTooltip, LivingEntityPatch<?> entitypatch) {
        Styles style;
        Style style2 = style = this instanceof RangedWeaponCapability ? Styles.RANGED : this.getStyle(entitypatch);
        if (style == null) {
            return;
        }
        itemTooltip.add(1, (Component)Component.m_237115_((String)("epicfight.style." + style.toString().toLowerCase(Locale.ROOT))).m_130940_(ChatFormatting.DARK_GRAY));
        int index = 0;
        boolean modifyIn = false;
        int i = 0;
        while (i < itemTooltip.size()) {
            Component textComp = itemTooltip.get(i);
            index = i++;
            if (this.findComponentArgument(textComp, Attributes.f_22283_.m_22087_()) == null) continue;
            modifyIn = true;
            break;
        }
        ++index;
        Map<Attribute, AttributeModifier> attribute = this.getDamageAttributesInCondition(style);
        if (attribute != null) {
            double value;
            if (!modifyIn) {
                itemTooltip.add(index, (Component)Component.m_237113_((String)""));
                itemTooltip.add(++index, (Component)Component.m_237115_((String)"epicfight.gui.attribute").m_130940_(ChatFormatting.GRAY));
                ++index;
            }
            Attribute armorNegation = (Attribute)EpicFightAttributes.ARMOR_NEGATION.get();
            Attribute impact = (Attribute)EpicFightAttributes.IMPACT.get();
            Attribute maxStrikes = (Attribute)EpicFightAttributes.MAX_STRIKES.get();
            if (attribute.containsKey(armorNegation) && (value = attribute.get(armorNegation).m_22218_() + ((LivingEntity)entitypatch.getOriginal()).m_21051_(armorNegation).m_22115_()) > 0.0) {
                itemTooltip.add(index, (Component)Component.m_237113_((String)" ").m_7220_((Component)Component.m_237110_((String)(armorNegation.m_22087_() + ".value"), (Object[])new Object[]{ItemStack.f_41584_.format(value)})));
            }
            if (attribute.containsKey(impact) && (value = attribute.get(impact).m_22218_() + ((LivingEntity)entitypatch.getOriginal()).m_21051_(impact).m_22115_()) > 0.0) {
                int i2 = itemstack.getEnchantmentLevel(Enchantments.f_44980_);
                itemTooltip.add(index++, (Component)Component.m_237113_((String)" ").m_7220_((Component)Component.m_237110_((String)(impact.m_22087_() + ".value"), (Object[])new Object[]{ItemStack.f_41584_.format(value *= (double)(1.0f + (float)i2 * 0.12f))})));
            }
            if (attribute.containsKey(maxStrikes)) {
                value = attribute.get(maxStrikes).m_22218_() + ((LivingEntity)entitypatch.getOriginal()).m_21051_(maxStrikes).m_22115_();
                if (value > 0.0) {
                    itemTooltip.add(index++, (Component)Component.m_237113_((String)" ").m_7220_((Component)Component.m_237110_((String)(maxStrikes.m_22087_() + ".value"), (Object[])new Object[]{ItemStack.f_41584_.format(value)})));
                }
            } else {
                itemTooltip.add(index++, (Component)Component.m_237113_((String)" ").m_7220_((Component)Component.m_237110_((String)(maxStrikes.m_22087_() + ".value"), (Object[])new Object[]{ItemStack.f_41584_.format(maxStrikes.m_22082_())})));
            }
        }
    }

    protected Object findComponentArgument(Component component, String key) {
        Object[] objectArray = component.m_214077_();
        if (objectArray instanceof TranslatableContents) {
            TranslatableContents contents = (TranslatableContents)objectArray;
            if (contents.m_237508_().equals(key)) {
                return component;
            }
            if (contents.m_237523_() != null) {
                for (Object arg : contents.m_237523_()) {
                    Component argComponent;
                    Object ret;
                    if (!(arg instanceof Component) || (ret = this.findComponentArgument(argComponent = (Component)arg, key)) == null) continue;
                    return ret;
                }
            }
        }
        for (Component siblingComponent : component.m_7360_()) {
            Object ret = this.findComponentArgument(siblingComponent, key);
            if (ret == null) continue;
            return ret;
        }
        return null;
    }

    public List<AnimationManager.AnimationAccessor<? extends AttackAnimation>> getAutoAttackMotion(PlayerPatch<?> playerpatch) {
        return CapabilityItem.getBasicAutoAttackMotion();
    }

    @Deprecated(since="1.21.1", forRemoval=true)
    public List<AnimationManager.AnimationAccessor<? extends AttackAnimation>> getMountAttackMotion() {
        return null;
    }

    @Nullable
    public Skill getInnateSkill(PlayerPatch<?> playerpatch, ItemStack itemstack) {
        return null;
    }

    @Nullable
    @Deprecated(since="1.21.1", forRemoval=true)
    public Skill getPassiveSkill() {
        return null;
    }

    public WeaponCategory getWeaponCategory() {
        return this.weaponCategory;
    }

    public void changeWeaponInnateSkill(PlayerPatch<?> playerpatch, ItemStack itemstack) {
        Skill weaponInnateSkill = this.getInnateSkill(playerpatch, itemstack);
        SkillContainer weaponInnateSkillContainer = playerpatch.getSkill(SkillSlots.WEAPON_INNATE);
        EpicFightNetworkManager.PayloadBundleBuilder toLocal = EpicFightNetworkManager.PayloadBundleBuilder.create();
        EpicFightNetworkManager.PayloadBundleBuilder toRemote = EpicFightNetworkManager.PayloadBundleBuilder.create();
        if (weaponInnateSkill != null) {
            if (weaponInnateSkillContainer.getSkill() != weaponInnateSkill) {
                weaponInnateSkillContainer.setSkill(weaponInnateSkill);
            }
            toLocal.and(new SPChangeSkill(SkillSlots.WEAPON_INNATE, ((Player)playerpatch.getOriginal()).m_19879_(), weaponInnateSkill));
        } else {
            toLocal.and(SPSetSkillContainerValue.enable(SkillSlots.WEAPON_INNATE, true, ((Player)playerpatch.getOriginal()).m_19879_()));
        }
        weaponInnateSkillContainer.setDisabled(weaponInnateSkill == null);
        toRemote.and(new SPSetRemotePlayerSkill(((Player)playerpatch.getOriginal()).m_19879_(), SkillSlots.WEAPON_INNATE, weaponInnateSkill));
        Skill passiveSkill = this.getPassiveSkill();
        SkillContainer passiveSkillContainer = playerpatch.getSkill(SkillSlots.WEAPON_PASSIVE);
        if (passiveSkill != null) {
            if (passiveSkillContainer.getSkill() != passiveSkill) {
                passiveSkillContainer.setSkill(passiveSkill);
                toLocal.and(new SPChangeSkill(SkillSlots.WEAPON_PASSIVE, ((Player)playerpatch.getOriginal()).m_19879_(), passiveSkill));
                toRemote.and(new SPSetRemotePlayerSkill(((Player)playerpatch.getOriginal()).m_19879_(), SkillSlots.WEAPON_PASSIVE, passiveSkill));
            }
        } else {
            passiveSkillContainer.setSkill(null);
            toLocal.and(new SPChangeSkill(SkillSlots.WEAPON_PASSIVE, ((Player)playerpatch.getOriginal()).m_19879_(), null));
            toRemote.and(new SPSetRemotePlayerSkill(((Player)playerpatch.getOriginal()).m_19879_(), SkillSlots.WEAPON_PASSIVE, passiveSkill));
        }
        toLocal.send((first, others) -> EpicFightNetworkManager.sendToPlayer(first, (ServerPlayer)playerpatch.getOriginal(), others));
        toRemote.send((first, others) -> EpicFightNetworkManager.sendToAllPlayerTrackingThisEntity(first, (Entity)((ServerPlayer)playerpatch.getOriginal()), others));
    }

    public SoundEvent getSmashingSound() {
        return (SoundEvent)EpicFightSounds.WHOOSH.get();
    }

    public SoundEvent getHitSound() {
        return (SoundEvent)EpicFightSounds.BLUNT_HIT.get();
    }

    public Collider getWeaponCollider() {
        return this.collider != null ? this.collider : ColliderPreset.FIST;
    }

    public HitParticleType getHitParticle() {
        return (HitParticleType)((Object)EpicFightParticles.HIT_BLUNT.get());
    }

    public final Map<Attribute, AttributeModifier> getDamageAttributesInCondition(Style style) {
        Map<Attribute, AttributeModifier> attributes = this.attributeMap.getOrDefault(style, Maps.newHashMap());
        this.attributeMap.getOrDefault(Styles.COMMON, Maps.newHashMap()).forEach(attributes::putIfAbsent);
        return attributes;
    }

    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot equipmentSlot, @Nullable LivingEntityPatch<?> entitypatch) {
        Map<Attribute, AttributeModifier> modifierMap;
        HashMultimap map = HashMultimap.create();
        if (entitypatch != null && (modifierMap = this.getDamageAttributesInCondition(this.getStyle(entitypatch))) != null) {
            for (Map.Entry<Attribute, AttributeModifier> entry : modifierMap.entrySet()) {
                map.put((Object)entry.getKey(), (Object)entry.getValue());
            }
        }
        return map;
    }

    public Multimap<Attribute, AttributeModifier> getAllAttributeModifiers(EquipmentSlot equipmentSlot) {
        HashMultimap map = HashMultimap.create();
        for (Map<Attribute, AttributeModifier> attrMap : this.attributeMap.values()) {
            for (Map.Entry<Attribute, AttributeModifier> entry : attrMap.entrySet()) {
                map.put((Object)entry.getKey(), (Object)entry.getValue());
            }
        }
        return map;
    }

    public Map<LivingMotion, AnimationManager.AnimationAccessor<? extends StaticAnimation>> getLivingMotionModifier(LivingEntityPatch<?> playerpatch, InteractionHand hand) {
        return Maps.newHashMap();
    }

    public Style getStyle(LivingEntityPatch<?> entitypatch) {
        return this.canBePlacedOffhand() ? Styles.ONE_HAND : Styles.TWO_HAND;
    }

    public AnimationManager.AnimationAccessor<? extends StaticAnimation> getGuardMotion(GuardSkill skill, GuardSkill.BlockType blockType, PlayerPatch<?> playerpatch) {
        return null;
    }

    public boolean canBePlacedOffhand() {
        return true;
    }

    @Deprecated(since="1.21.1", forRemoval=true)
    public boolean shouldCancelCombo(LivingEntityPatch<?> entitypatch) {
        return true;
    }

    public int handleComboCounter(ComboCounterHandleEvent.Causal causal, PlayerPatch<?> entitypatch, @Nullable AnimationManager.AnimationAccessor<? extends MainFrameAnimation> nextAnimation, int original) {
        return ComboCounterHandleEvent.ComboCounterHandler.DEFAULT_COMBO_HANDLER.handleComboCounter(this, causal, entitypatch, nextAnimation, original);
    }

    public boolean isEmpty() {
        return this == EMPTY;
    }

    public CapabilityItem getResult(ItemStack item) {
        return this;
    }

    @Deprecated(since="1.21.1", forRemoval=true)
    public boolean availableOnHorse() {
        return true;
    }

    public boolean checkOffhandValid(LivingEntityPatch<?> entitypatch) {
        return this.getStyle(entitypatch).canUseOffhand() && EpicFightCapabilities.getItemStackCapability(((LivingEntity)entitypatch.getOriginal()).m_21206_()).canHoldInOffhandAlone();
    }

    public boolean canHoldInOffhandAlone() {
        return true;
    }

    public float getReach() {
        return 0.0f;
    }

    public LivingMotion getLivingMotion(LivingEntityPatch<?> entitypatch, InteractionHand hand) {
        return null;
    }

    public void onStrike(LivingEntityPatch<?> entitypatch, AttackAnimation animation) {
    }

    public UseAnim getUseAnimation(LivingEntityPatch<?> entitypatch) {
        return UseAnim.NONE;
    }

    public ZoomInType getZoomInType() {
        return ZoomInType.NONE;
    }

    public static Builder builder() {
        return new Builder();
    }

    static {
        commonAutoAttackMotion.add(Animations.FIST_AUTO1);
        commonAutoAttackMotion.add(Animations.FIST_AUTO2);
        commonAutoAttackMotion.add(Animations.FIST_AUTO3);
        commonAutoAttackMotion.add(Animations.FIST_DASH);
        commonAutoAttackMotion.add(Animations.FIST_AIR_SLASH);
    }

    public static class Builder {
        Function<Builder, CapabilityItem> constructor = CapabilityItem::new;
        Map<Style, Map<Attribute, AttributeModifier>> attributeMap = Maps.newHashMap();
        WeaponCategory category = WeaponCategories.FIST;
        Collider collider = ColliderPreset.FIST;

        protected Builder() {
        }

        public Builder constructor(Function<Builder, CapabilityItem> constructor) {
            this.constructor = constructor;
            return this;
        }

        public Builder category(WeaponCategory category) {
            this.category = category;
            return this;
        }

        public Builder collider(Collider collider) {
            this.collider = collider;
            return this;
        }

        public Builder addStyleAttibutes(Style style, Pair<Attribute, AttributeModifier> attributePair) {
            Map map = this.attributeMap.computeIfAbsent(style, key -> Maps.newHashMap());
            map.put((Attribute)attributePair.getFirst(), (AttributeModifier)attributePair.getSecond());
            return this;
        }

        public final CapabilityItem build() {
            return this.constructor.apply(this);
        }

        public Collider getCollider() {
            return this.collider;
        }
    }

    public static enum Styles implements Style
    {
        COMMON(true),
        ONE_HAND(true),
        TWO_HAND(false),
        MOUNT(true),
        RANGED(false),
        SHEATH(false),
        OCHS(false);

        final boolean canUseOffhand;
        final int id = Style.ENUM_MANAGER.assign(this);

        private Styles(boolean canUseOffhand) {
            this.canUseOffhand = canUseOffhand;
        }

        @Override
        public int universalOrdinal() {
            return this.id;
        }

        @Override
        public boolean canUseOffhand() {
            return this.canUseOffhand;
        }
    }

    public static enum ZoomInType {
        NONE,
        ALWAYS,
        USE_TICK,
        AIMING,
        CUSTOM;

    }

    public static enum WeaponCategories implements WeaponCategory
    {
        NOT_WEAPON,
        AXE,
        FIST,
        GREATSWORD,
        HOE,
        PICKAXE,
        SHOVEL,
        SWORD,
        UCHIGATANA,
        SPEAR,
        TACHI,
        TRIDENT,
        LONGSWORD,
        DAGGER,
        SHIELD,
        CROSSBOW,
        BOW;

        final int id = WeaponCategory.ENUM_MANAGER.assign(this);

        @Override
        public int universalOrdinal() {
            return this.id;
        }
    }
}


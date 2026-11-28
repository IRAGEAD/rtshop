/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Supplier
 *  com.google.common.base.Suppliers
 *  net.minecraft.core.Holder
 *  net.minecraft.core.Holder$Reference
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  net.spell_power.api.SpellPower
 *  net.spell_power.api.SpellPower$Result
 *  net.spell_power.api.SpellSchool
 *  org.jetbrains.annotations.Nullable
 */
package net.spell_engine.internals;

import com.google.common.base.Suppliers;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.spell_engine.Platform;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.event.SpellEvents;
import net.spell_engine.fx.ReleaseFx;
import net.spell_engine.internals.SpellModifiers;
import net.spell_engine.internals.SpellParameters;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.casting.SpellCaster;
import net.spell_engine.internals.casting.SpellCasting;
import net.spell_engine.internals.container.SpellContainerSource;
import net.spell_engine.internals.cost.Ammo;
import net.spell_engine.internals.cost.SpellCooldowns;
import net.spell_engine.internals.cost.SpellCost;
import net.spell_engine.internals.delivery.SpellDelivery;
import net.spell_engine.internals.target.SpellIntents;
import net.spell_engine.internals.target.SpellTarget;
import net.spell_engine.utils.AnimationHelper;
import net.spell_engine.utils.RegistryHelper;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchool;
import org.jetbrains.annotations.Nullable;

public class SpellExecution {
    public static void performSpell(Level world, Player player, Holder<Spell> spellEntry, SpellTarget.SearchResult targetResult, SpellCast.Action action, float progress) {
        if (player.m_5833_()) {
            return;
        }
        Spell spell = (Spell)spellEntry.m_203334_();
        ResourceLocation spellId = ((ResourceKey)spellEntry.m_203543_().get()).m_135782_();
        ItemStack heldItemStack = player.m_21205_();
        SpellContainerSource.SourcedContainer spellSource = SpellContainerSource.getFirstSourceOfSpell(spellId, player);
        if (spellSource == null) {
            return;
        }
        SpellCast.Attempt attempt = SpellCasting.attempt(player, heldItemStack, spellId);
        if (!attempt.isSuccess()) {
            return;
        }
        SpellCaster.Player caster = (SpellCaster.Player)player;
        List<Entity> targets = targetResult.entities();
        float castingSpeed = caster.getCastingSpeed();
        progress = Math.max(Math.min(progress, 1.0f), 0.0f);
        float channelMultiplier = 1.0f;
        int channelTickIndex = 0;
        int incrementChannelTicks = 0;
        boolean shouldPerformImpact = true;
        Spell.Modifier chargeModifier = null;
        float curvedRatio = 1.0f;
        com.google.common.base.Supplier trackingPlayers = Suppliers.memoize(() -> Platform.tracking((Entity)player));
        switch (action) {
            case CHANNEL: {
                float channelInterval;
                channelTickIndex = caster.getInteractor().channelTickIndex();
                incrementChannelTicks = 1;
                channelMultiplier = SpellParameters.channelValueMultiplier(spell);
                if (caster.getSpellCastProcess() == null || !((channelInterval = caster.getSpellCastProcess().channelInterval((LivingEntity)player)) < 1.0f)) break;
                channelMultiplier *= 1.0f / channelInterval;
                break;
            }
            case RELEASE: {
                Spell.Active.Cast cast = spell.active.cast;
                if (SpellParameters.isChanneled(spell)) {
                    shouldPerformImpact = false;
                    channelMultiplier = 1.0f;
                } else if (cast.type == Spell.Active.Cast.Type.CHARGE && cast.charge != null) {
                    if (progress >= cast.charge.min_release_ratio) {
                        curvedRatio = cast.charge.curve.apply(progress);
                        chargeModifier = SpellModifiers.scaledBy(cast.charge.bonus, curvedRatio);
                        channelMultiplier = 1.0f;
                    } else {
                        channelMultiplier = 0.0f;
                    }
                } else {
                    channelMultiplier = progress >= 1.0f ? 1.0f : 0.0f;
                }
                caster.getInteractor().requestClear();
                break;
            }
        }
        Ammo.Result ammoResult = Ammo.ammoForSpell(player, spell, heldItemStack);
        if (channelMultiplier > 0.0f && ammoResult.satisfied()) {
            Spell.Target targeting = spell.target;
            boolean finished = action == SpellCast.Action.RELEASE || action == SpellCast.Action.TRIGGER && spell.type == Spell.Type.PASSIVE;
            boolean shouldSendReleaseFx = switch (action) {
                default -> throw new IncompatibleClassChangeError();
                case SpellCast.Action.CHANNEL -> spell.active.cast.channelReleaseFx();
                case SpellCast.Action.RELEASE -> {
                    if (!SpellParameters.isChanneled(spell) || !spell.active.cast.channelReleaseFx()) {
                        yield true;
                    }
                    yield false;
                }
                case SpellCast.Action.TRIGGER -> spell.type == Spell.Type.PASSIVE;
            };
            boolean success = true;
            if (targeting.cap > 0) {
                targets = targets.stream().sorted(Comparator.comparingDouble(target -> target.m_20238_(player.m_20182_()))).limit(targeting.cap).toList();
            }
            Consumer<DeliveryCompletion> completion = null;
            if (shouldSendReleaseFx || finished) {
                float finalProgress = progress;
                List<Entity> finalTargets = targets;
                completion = arg_0 -> SpellExecution.lambda$performSpell$3(shouldSendReleaseFx, world, player, spellEntry, finalProgress, (Supplier)trackingPlayers, spell, castingSpeed, finished, spellSource, spellId, heldItemStack, ammoResult, finalTargets, action, arg_0);
            }
            if (shouldPerformImpact) {
                SpellCooldowns.imposeAttemptCooldown(player, spellEntry);
                success = false;
                ImpactContext context = new ImpactContext(channelMultiplier, 1.0f, null, SpellPower.getSpellPower((SpellSchool)spell.school, (LivingEntity)player), SpellIntents.focusMode(spell), channelTickIndex).chargeModifier(chargeModifier).charge(curvedRatio);
                success = SpellDelivery.resolveAndDeliver(world, (LivingEntity)player, spellEntry, targetResult, context, completion);
                SpellCast.Process castProcess = caster.getSpellCastProcess();
                if (incrementChannelTicks > 0 && castProcess != null && castProcess.id().equals((Object)spellId)) {
                    caster.getInteractor().setChannelTickIndex(channelTickIndex + incrementChannelTicks);
                }
            } else if (finished && completion != null) {
                completion.accept(new DeliveryCompletion(true));
            }
        }
    }

    private static /* synthetic */ void lambda$performSpell$3(boolean shouldSendReleaseFx, Level world, Player player, Holder spellEntry, float finalProgress, Supplier trackingPlayers, Spell spell, float castingSpeed, boolean finished, SpellContainerSource.SourcedContainer spellSource, ResourceLocation spellId, ItemStack heldItemStack, Ammo.Result ammoResult, List finalTargets, SpellCast.Action action, DeliveryCompletion completionArgs) {
        if (!completionArgs.success()) {
            return;
        }
        if (shouldSendReleaseFx) {
            ReleaseFx.send(world, (LivingEntity)player, (Holder<Spell>)spellEntry, finalProgress);
            AnimationHelper.sendAnimation(player, (Collection)trackingPlayers.get(), SpellCast.Animation.RELEASE, spell.release.animation, castingSpeed);
        }
        if (finished) {
            SpellCost.consume(player, finalProgress, spellSource, spellId, (Holder<Spell>)spellEntry, heldItemStack, ammoResult, false);
            SpellEvents.SpellCastEvent.Args args = new SpellEvents.SpellCastEvent.Args(player, (Holder<Spell>)spellEntry, finalTargets, action, finalProgress);
            SpellEvents.SPELL_CAST.invoke(listener -> listener.onSpellCast(args));
        }
    }

    public record ImpactContext(float channel, float distance, @Nullable Vec3 position, SpellPower.Result power, SpellTarget.FocusMode focusMode, int channelTickIndex, @Nullable int effectiveCasterId, @Nullable Spell.Modifier chargeModifier, float charge) {
        public ImpactContext() {
            this(1.0f, 1.0f, null, null, SpellTarget.FocusMode.DIRECT, 0, 0, null, 1.0f);
        }

        public ImpactContext(float channel, float distance, @Nullable Vec3 position, SpellPower.Result power, SpellTarget.FocusMode focusMode, int channelTickIndex) {
            this(channel, distance, position, power, focusMode, channelTickIndex, 0, null, 1.0f);
        }

        public ImpactContext channeled(float multiplier) {
            return new ImpactContext(multiplier, this.distance, this.position, this.power, this.focusMode, this.channelTickIndex, this.effectiveCasterId, this.chargeModifier, this.charge);
        }

        public ImpactContext distance(float multiplier) {
            return new ImpactContext(this.channel, multiplier, this.position, this.power, this.focusMode, this.channelTickIndex, this.effectiveCasterId, this.chargeModifier, this.charge);
        }

        public ImpactContext position(Vec3 position) {
            return new ImpactContext(this.channel, this.distance, position, this.power, this.focusMode, this.channelTickIndex, this.effectiveCasterId, this.chargeModifier, this.charge);
        }

        public ImpactContext power(SpellPower.Result spellPower) {
            return new ImpactContext(this.channel, this.distance, this.position, spellPower, this.focusMode, this.channelTickIndex, this.effectiveCasterId, this.chargeModifier, this.charge);
        }

        public ImpactContext target(SpellTarget.FocusMode focusMode) {
            return new ImpactContext(this.channel, this.distance, this.position, this.power, focusMode, this.channelTickIndex, this.effectiveCasterId, this.chargeModifier, this.charge);
        }

        public ImpactContext effectiveCaster(@Nullable LivingEntity effectiveCaster) {
            int id = effectiveCaster != null ? effectiveCaster.m_19879_() : 0;
            return new ImpactContext(this.channel, this.distance, this.position, this.power, this.focusMode, this.channelTickIndex, id, this.chargeModifier, this.charge);
        }

        public ImpactContext chargeModifier(@Nullable Spell.Modifier chargeModifier) {
            return new ImpactContext(this.channel, this.distance, this.position, this.power, this.focusMode, this.channelTickIndex, this.effectiveCasterId, chargeModifier, this.charge);
        }

        public ImpactContext charge(float charge) {
            return new ImpactContext(this.channel, this.distance, this.position, this.power, this.focusMode, this.channelTickIndex, this.effectiveCasterId, this.chargeModifier, charge);
        }

        @Nullable
        public LivingEntity effectiveCaster(Level world) {
            return this.effectiveCasterId != 0 ? (LivingEntity)world.m_6815_(this.effectiveCasterId) : null;
        }

        public boolean hasOffset() {
            return this.position != null;
        }

        public Vec3 knockbackDirection(Vec3 targetPosition) {
            return targetPosition.m_82546_(this.position).m_82541_();
        }

        public boolean isChanneled() {
            return this.channel != 1.0f;
        }

        public float total(@Nullable Holder<Spell> spellEntry) {
            return this.channel * this.distance * SpellParameters.chargeOutputMultiplier(spellEntry, this.charge);
        }
    }

    public record DeliveryCompletion(boolean success) {
    }

    public static class Power {
        public static SpellPower.Result blended(SpellPower.Result base, @Nullable List<Spell.Impact.PowerBlend> blend, LivingEntity caster) {
            if (blend == null || blend.isEmpty()) {
                return base;
            }
            double power = base.baseValue();
            double critChance = base.criticalChance();
            double critDamage = base.criticalDamage();
            double powerWeight = 1.0;
            double critChanceWeight = 1.0;
            double critDamageWeight = 1.0;
            for (Spell.Impact.PowerBlend component : blend) {
                if (component.school == null || component.weight <= 0.0f || !component.power && !component.critical_chance && !component.critical_damage) continue;
                SpellPower.Result other = SpellPower.getSpellPower((SpellSchool)component.school, (LivingEntity)caster);
                if (component.power) {
                    power += other.baseValue() * (double)component.weight;
                    powerWeight += (double)component.weight;
                }
                if (component.critical_chance) {
                    critChance += other.criticalChance() * (double)component.weight;
                    critChanceWeight += (double)component.weight;
                }
                if (!component.critical_damage) continue;
                critDamage += other.criticalDamage() * (double)component.weight;
                critDamageWeight += (double)component.weight;
            }
            return new SpellPower.Result(base.school(), power / powerWeight, critChance / critChanceWeight, critDamage / critDamageWeight);
        }

        public static SpellPower.Result resolve(Spell spell, Spell.Impact impact, LivingEntity caster, @Nullable SpellPower.Result cached, @Nullable Entity attributeTarget) {
            Optional attributeEntry;
            SpellSchool school = impact.school != null ? impact.school : spell.school;
            SpellPower.Result power = cached;
            if (power == null || power.school() != school) {
                power = SpellPower.getSpellPower((SpellSchool)school, (LivingEntity)caster);
            }
            if (impact.attribute != null && !impact.attribute.isEmpty() && (attributeEntry = RegistryHelper.getEntry(BuiltInRegistries.f_256951_, new ResourceLocation(impact.attribute))).isPresent()) {
                LivingEntity livingEntity;
                Holder.Reference attribute = attributeEntry.get();
                double value = impact.attribute_from_target && attributeTarget instanceof LivingEntity ? ((livingEntity = (LivingEntity)attributeTarget).m_21204_().m_247503_(attribute) ? livingEntity.m_246858_(attribute) : 0.0) : (caster.m_21204_().m_247503_(attribute) ? caster.m_246858_(attribute) : 0.0);
                power = new SpellPower.Result(power.school(), value, power.criticalChance(), power.criticalDamage());
            }
            return Power.blended(power, impact.power_blend, caster);
        }

        public static SpellPower.Result clamped(SpellPower.Result power, Spell.Impact.Action action) {
            if (power.baseValue() < (double)action.min_power || power.baseValue() > (double)action.max_power) {
                double clampedValue = Mth.m_14008_((double)power.baseValue(), (double)action.min_power, (double)action.max_power);
                return new SpellPower.Result(power.school(), clampedValue, power.criticalChance(), power.criticalDamage());
            }
            return power;
        }
    }

    public record ConditionResult(boolean allowed, List<Spell.Impact.Modifier> modifiers) {
        public static final ConditionResult ALLOWED = new ConditionResult(true, List.of());
        public static final ConditionResult DENIED = new ConditionResult(false, List.of());
    }

    public record DeliveryTarget(Entity entity, ImpactContext context) {
    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.Holder
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package net.spell_engine.api.spell.event;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.spell_engine.api.event.Event;
import net.spell_engine.api.event.StagedEvent;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.entity.SpellProjectile;
import net.spell_engine.internals.SpellExecution;
import net.spell_engine.internals.casting.SpellCast;
import org.jetbrains.annotations.Nullable;

public class SpellEvents {
    public static final StagedEvent<CastingAttemptEvent> CASTING_ATTEMPT = new StagedEvent();
    public static final Event<SpellCostConsumeEvent> COST_CONSUME = new Event();
    public static final Event<SpellCastEvent> SPELL_CAST = new Event();
    public static final Event<HealEvent> HEAL = new Event();
    public static final Event<ProjectileLaunch> PROJECTILE_SHOOT = new Event();
    public static final Event<ProjectileLaunch> PROJECTILE_FALL = new Event();

    public static interface ProjectileLaunch {
        public void onProjectileLaunch(ProjectileLaunchEvent var1);
    }

    public record ProjectileLaunchEvent(SpellProjectile projectile, Spell.LaunchProperties mutableLaunchProperties, LivingEntity caster, @Nullable Entity target, Holder<Spell> spellEntry, SpellExecution.ImpactContext context, int sequenceIndex) {
    }

    public static interface HealEvent {
        public void onHeal(Args var1);

        public record Args(LivingEntity caster, Holder<Spell> spell, LivingEntity target, float amount) {
        }
    }

    public static interface SpellCastEvent {
        public void onSpellCast(Args var1);

        public record Args(Player caster, Holder<Spell> spell, List<Entity> targets, SpellCast.Action action, float progress) {
        }
    }

    public static interface SpellCostConsumeEvent {
        public void onSpellCostConsume(Args var1);

        public record Args(Player caster, Holder<Spell> spell, ItemStack itemStack) {
        }
    }

    public static interface CastingAttemptEvent {
        @Nullable
        public SpellCast.Attempt onCastingAttempt(Args var1);

        public record Args(Player caster, Holder<Spell> spell, ItemStack itemStack) {
        }
    }
}


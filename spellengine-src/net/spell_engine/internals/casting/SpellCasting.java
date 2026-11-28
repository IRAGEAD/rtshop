/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.Holder
 *  net.minecraft.core.Holder$Reference
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 */
package net.spell_engine.internals.casting;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.event.SpellEvents;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.casting.SpellCaster;
import net.spell_engine.internals.cost.Ammo;
import net.spell_engine.utils.RegistryHelper;

public class SpellCasting {
    public static SpellCast.Attempt attempt(Player player, ItemStack itemStack, ResourceLocation spellId) {
        return SpellCasting.attempt(player, itemStack, spellId, true);
    }

    public static SpellCast.Attempt attempt(Player player, ItemStack itemStack, ResourceLocation spellId, boolean checkAmmo) {
        Ammo.Result ammoResult;
        SpellEvents.CastingAttemptEvent.Args args;
        SpellCast.Attempt injected;
        SpellCaster.Player caster = (SpellCaster.Player)player;
        Holder.Reference spellEntry = RegistryHelper.getEntry(SpellRegistry.from(player.m_9236_()), spellId).orElse(null);
        if (spellEntry == null) {
            return SpellCast.Attempt.none();
        }
        Spell spell = (Spell)spellEntry.m_203334_();
        if (SpellEvents.CASTING_ATTEMPT.PRE.isListened() && (injected = SpellEvents.CASTING_ATTEMPT.PRE.invokeWithResult(arg_0 -> SpellCasting.lambda$attempt$0(args = new SpellEvents.CastingAttemptEvent.Args(player, (Holder<Spell>)spellEntry, itemStack), arg_0))) != null) {
            return injected;
        }
        if (caster.getCooldownManager().isCoolingDown((Holder<Spell>)spellEntry)) {
            return SpellCast.Attempt.failOnCooldown(new SpellCast.Attempt.OnCooldownInfo());
        }
        if (checkAmmo && !(ammoResult = Ammo.ammoForSpell(player, spell, itemStack)).satisfied()) {
            return SpellCast.Attempt.failMissingItem(new SpellCast.Attempt.MissingItemInfo(ammoResult.item()));
        }
        if (SpellEvents.CASTING_ATTEMPT.POST.isListened() && (injected = SpellEvents.CASTING_ATTEMPT.POST.invokeWithResult(arg_0 -> SpellCasting.lambda$attempt$1(args = new SpellEvents.CastingAttemptEvent.Args(player, (Holder<Spell>)spellEntry, itemStack), arg_0))) != null) {
            return injected;
        }
        return SpellCast.Attempt.success();
    }

    private static /* synthetic */ SpellCast.Attempt lambda$attempt$1(SpellEvents.CastingAttemptEvent.Args args, SpellEvents.CastingAttemptEvent listener) {
        return listener.onCastingAttempt(args);
    }

    private static /* synthetic */ SpellCast.Attempt lambda$attempt$0(SpellEvents.CastingAttemptEvent.Args args, SpellEvents.CastingAttemptEvent listener) {
        return listener.onCastingAttempt(args);
    }
}


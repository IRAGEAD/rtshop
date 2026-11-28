/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.Holder
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemCooldowns
 */
package net.spell_engine.internals.cost;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.spell_engine.SpellEngineMod;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.tags.SpellEngineItemTags;
import net.spell_engine.internals.SpellParameters;
import net.spell_engine.internals.casting.SpellCaster;
import net.spell_engine.internals.container.SpellContainerSource;
import net.spell_engine.utils.ItemCooldownManagerExtension;

public class SpellCooldowns {
    public static void imposeCooldown(Player player, SpellContainerSource.SourcedContainer source, Holder<Spell> spellEntry, float progress) {
        Spell spell = (Spell)spellEntry.m_203334_();
        float duration = SpellCooldowns.cooldownToSet((LivingEntity)player, spellEntry, progress);
        int durationTicks = Math.round(duration * 20.0f);
        if (duration > 0.0f) {
            ((SpellCaster.Player)player).getCooldownManager().set(spellEntry, durationTicks);
        }
        if (SpellEngineMod.config.spell_item_cooldown_lock && spell.cost.cooldown.hosting_item && source.itemStack() != null) {
            float durationLeft;
            Item hostingItem = source.itemStack().m_41720_();
            ItemCooldowns itemCooldowns = player.m_36335_();
            if (source.itemStack().m_204117_(SpellEngineItemTags.SPELL_BOOK)) {
                durationTicks += (int)(SpellEngineMod.config.spell_book_additional_cooldown * 20.0f);
            }
            if ((float)durationTicks > (durationLeft = (float)((ItemCooldownManagerExtension)itemCooldowns).SE_getLastCooldownDuration(hostingItem) * itemCooldowns.m_41521_(hostingItem, 0.0f))) {
                itemCooldowns.m_41524_(hostingItem, durationTicks);
            }
        }
    }

    private static float cooldownToSet(LivingEntity caster, Holder<Spell> spellEntry, float progress) {
        Spell spell = (Spell)spellEntry.m_203334_();
        if (spell.cost.cooldown.proportional) {
            return SpellParameters.getCooldownDuration(caster, spellEntry) * progress;
        }
        return SpellParameters.getCooldownDuration(caster, spellEntry);
    }

    public static void imposeAttemptCooldown(Player player, Holder<Spell> spellEntry) {
        float attemptCooldown;
        Spell spell = (Spell)spellEntry.m_203334_();
        if (spell.cost.cooldown != null && (attemptCooldown = spell.cost.cooldown.attempt_duration) > 0.0f) {
            int durationTicks = Math.round(attemptCooldown * 20.0f);
            ((SpellCaster.Player)player).getCooldownManager().set(spellEntry, durationTicks);
        }
    }
}


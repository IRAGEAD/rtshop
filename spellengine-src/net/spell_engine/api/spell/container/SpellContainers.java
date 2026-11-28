/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.Holder
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 */
package net.spell_engine.api.spell.container;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.container.SpellContainer;

public class SpellContainers {
    public static SpellContainer forRangedWeapon() {
        return SpellContainers.forWeapon(SpellContainer.ContentType.ARCHERY, List.of());
    }

    public static SpellContainer forMagicWeapon() {
        return SpellContainers.forWeapon(SpellContainer.ContentType.MAGIC, List.of());
    }

    public static SpellContainer forMeleeWeapon() {
        return SpellContainers.forWeapon(SpellContainer.ContentType.MAGIC, List.of());
    }

    public static SpellContainer forWeapon(SpellContainer.ContentType contentType, List<ResourceLocation> spellIds) {
        List<String> spellIdStrings = spellIds.stream().map(ResourceLocation::toString).toList();
        return new SpellContainer(contentType, "", "", 0, spellIdStrings);
    }

    public static SpellContainer forShield(ResourceLocation spellId) {
        return SpellContainers.forShield(List.of(spellId));
    }

    public static SpellContainer forShield(List<ResourceLocation> spellIds) {
        return new SpellContainer(SpellContainer.ContentType.MAGIC, "", "", "offhand", 0, spellIds.stream().map(ResourceLocation::toString).toList());
    }

    public static SpellContainer forRelic(ResourceLocation spellId) {
        return new SpellContainer(SpellContainer.ContentType.NONE, "", "", 0, List.of(spellId.toString()));
    }

    public static SpellContainer forModifier(ResourceLocation spellId) {
        return new SpellContainer(SpellContainer.ContentType.NONE, "", "", 0, List.of(spellId.toString()));
    }

    public static SpellContainer forScroll(Holder<Spell> spellEntry) {
        return new SpellContainer(SpellContainer.ContentType.NONE, "", "", 0, List.of(((ResourceKey)spellEntry.m_203543_().get()).m_135782_().toString()));
    }
}


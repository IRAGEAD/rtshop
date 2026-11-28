/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package net.spell_engine.internals.casting;

import net.spell_engine.api.spell.Spell;
import net.spell_engine.internals.casting.SpellCaster;
import org.jetbrains.annotations.Nullable;

@Deprecated
public interface SpellCasterEntity
extends SpellCaster.Player {
    @Nullable
    default public Spell getCurrentSpell() {
        return this.getCastedSpell();
    }
}


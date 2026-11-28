/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonPrimitive
 *  net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance
 *  net.minecraft.advancements.critereon.ContextAwarePredicate
 *  net.minecraft.advancements.critereon.DeserializationContext
 *  net.minecraft.advancements.critereon.SerializationContext
 *  net.minecraft.advancements.critereon.SimpleCriterionTrigger
 *  net.minecraft.core.Holder
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerPlayer
 *  org.jetbrains.annotations.Nullable
 */
package net.spell_engine.misc.criteria;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.utils.PatternMatching;
import org.jetbrains.annotations.Nullable;

public class SpellCastCriteria
extends SimpleCriterionTrigger<Condition> {
    public static final ResourceLocation ID = new ResourceLocation("spell_engine", "spell_cast");
    public static final SpellCastCriteria INSTANCE = new SpellCastCriteria();
    private static final String SPELL_KEY = "spell";
    private static final String OTHER_SPELL_KEY = "other_spell";

    public ResourceLocation m_7295_() {
        return ID;
    }

    protected Condition conditionsFromJson(JsonObject obj, ContextAwarePredicate playerPredicate, DeserializationContext predicateDeserializer) {
        return new Condition(playerPredicate, SpellCastCriteria.optionalString(obj, SPELL_KEY), SpellCastCriteria.optionalString(obj, OTHER_SPELL_KEY));
    }

    @Nullable
    private static String optionalString(JsonObject obj, String key) {
        JsonElement element = obj.get(key);
        return element != null && !element.isJsonNull() ? element.getAsString() : null;
    }

    public void trigger(ServerPlayer player, Holder<Spell> spell) {
        this.m_66234_(player, condition -> condition.matches(spell));
    }

    public static class Condition
    extends AbstractCriterionTriggerInstance {
        @Nullable
        private final String spell;
        @Nullable
        private final String other_spell;

        public Condition(ContextAwarePredicate player, @Nullable String spell, @Nullable String other_spell) {
            super(ID, player);
            this.spell = spell;
            this.other_spell = other_spell;
        }

        public boolean matches(Holder<Spell> spellEntry) {
            if (this.spell == null && this.other_spell == null) {
                return true;
            }
            if (this.spell != null && PatternMatching.matches(spellEntry, SpellRegistry.KEY, this.spell)) {
                return true;
            }
            return this.other_spell != null && PatternMatching.matches(spellEntry, SpellRegistry.KEY, this.other_spell);
        }

        public JsonObject m_7683_(SerializationContext predicateSerializer) {
            JsonObject json = super.m_7683_(predicateSerializer);
            if (this.spell != null) {
                json.add(SpellCastCriteria.SPELL_KEY, (JsonElement)new JsonPrimitive(this.spell));
            }
            if (this.other_spell != null) {
                json.add(SpellCastCriteria.OTHER_SPELL_KEY, (JsonElement)new JsonPrimitive(this.other_spell));
            }
            return json;
        }
    }
}


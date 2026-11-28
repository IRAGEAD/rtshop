/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  net.minecraft.resources.ResourceLocation
 *  org.jetbrains.annotations.Nullable
 */
package net.spell_engine.api.spell.container;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record SpellContainer(ContentType access, String access_param, String pool, String slot, int max_spell_count, List<String> spell_ids, int extra_tier_binding) {
    public static final Codec<SpellContainer> CODEC = RecordCodecBuilder.create(instance -> instance.group((App)ContentType.CODEC.optionalFieldOf("access", (Object)ContentType.NONE).forGetter(x -> x.access), (App)Codec.STRING.optionalFieldOf("access_param", (Object)"").forGetter(x -> x.access_param), (App)Codec.STRING.optionalFieldOf("pool", (Object)"").forGetter(x -> x.pool), (App)Codec.STRING.optionalFieldOf("slot", (Object)"").forGetter(x -> x.slot), (App)Codec.INT.optionalFieldOf("max_spell_count", (Object)0).forGetter(x -> x.max_spell_count), (App)Codec.STRING.listOf().optionalFieldOf("spell_ids", List.of()).forGetter(x -> x.spell_ids), (App)Codec.INT.optionalFieldOf("extra_tier_binding", (Object)0).forGetter(x -> x.extra_tier_binding)).apply((Applicative)instance, SpellContainer::new));
    public static final SpellContainer EMPTY = new SpellContainer(ContentType.NONE, "", "", "", 0, List.of(), 0);

    public SpellContainer(ContentType content, String access_tag, String pool, int max_spell_count, List<String> spell_ids) {
        this(content, access_tag, pool, "", max_spell_count, spell_ids, 0);
    }

    public SpellContainer(ContentType content, String access_tag, String pool, String slot, int max_spell_count, List<String> spell_ids) {
        this(content, access_tag, pool, slot, max_spell_count, spell_ids, 0);
    }

    public SpellContainer(ContentType access, String access_param, String pool, String slot, int max_spell_count, List<String> spell_ids, int extra_tier_binding) {
        this.access = access != null ? access : ContentType.NONE;
        this.access_param = access_param;
        this.pool = pool != null ? pool : "";
        this.slot = slot != null ? slot : "";
        this.max_spell_count = max_spell_count;
        this.spell_ids = spell_ids != null ? spell_ids.stream().map(SpellContainer::normalizeId).toList() : List.of();
        this.extra_tier_binding = extra_tier_binding;
    }

    public static String normalizeId(String id) {
        return id.toLowerCase().replaceAll("[^a-z0-9/._:-]", "");
    }

    public int binding_mutex_count() {
        return Math.max(0, this.extra_tier_binding + 1);
    }

    public boolean isResolver() {
        if (this.access == null) {
            return false;
        }
        switch (this.access.ordinal()) {
            case 1: {
                return false;
            }
            case 5: {
                return !this.access_param.isEmpty();
            }
        }
        return true;
    }

    public boolean isValid() {
        if (this.isResolver()) {
            return true;
        }
        if (this.max_spell_count < 0) {
            return false;
        }
        return !this.spell_ids.isEmpty() || this.pool != null && !this.pool.isEmpty();
    }

    public boolean contains(ResourceLocation spellId) {
        return this.spell_ids.contains(spellId.toString());
    }

    public boolean slotMatches(@Nullable String other) {
        return other == null || this.slot().contains(other);
    }

    public boolean isUsable() {
        return this.isValid() && !this.spell_ids.isEmpty();
    }

    public SpellContainer copy() {
        return new SpellContainer(this.access, this.access_param, this.pool, this.slot, this.max_spell_count, new ArrayList<String>(this.spell_ids), this.extra_tier_binding);
    }

    public SpellContainer copyWith(List<String> spell_ids) {
        return new SpellContainer(this.access, this.access_param, this.pool, this.slot, this.max_spell_count, spell_ids, this.extra_tier_binding);
    }

    public SpellContainer withContentType(ContentType content) {
        return new SpellContainer(content, this.access_param, this.pool, this.slot, this.max_spell_count, this.spell_ids, this.extra_tier_binding);
    }

    public SpellContainer withBindingPool(ResourceLocation poolId) {
        return new SpellContainer(this.access, this.access_param, poolId.toString(), this.slot, this.max_spell_count, this.spell_ids, this.extra_tier_binding);
    }

    public SpellContainer withMaxSpellCount(int maxSpellCount) {
        return new SpellContainer(this.access, this.access_param, this.pool, this.slot, maxSpellCount, this.spell_ids, this.extra_tier_binding);
    }

    public SpellContainer withSpell(String spellId) {
        return new SpellContainer(this.access, this.access_param, this.pool, this.slot, this.max_spell_count, List.of(spellId), this.extra_tier_binding);
    }

    public SpellContainer withSpellId(ResourceLocation spellId) {
        return new SpellContainer(this.access, this.access_param, this.pool, this.slot, this.max_spell_count, List.of(spellId.toString()), this.extra_tier_binding);
    }

    public SpellContainer withSpellIds(List<ResourceLocation> spellIds) {
        List<String> spellIdStrings = spellIds.stream().map(ResourceLocation::toString).toList();
        return new SpellContainer(this.access, this.access_param, this.pool, this.slot, this.max_spell_count, spellIdStrings, this.extra_tier_binding);
    }

    public SpellContainer withAdditionalSpell(List<String> spellIds) {
        ArrayList<String> newSpellIds = new ArrayList<String>(this.spell_ids);
        newSpellIds.addAll(spellIds);
        return new SpellContainer(this.access, this.access_param, this.pool, this.slot, this.max_spell_count, newSpellIds, this.extra_tier_binding);
    }

    public static enum ContentType {
        ANY,
        NONE,
        MAGIC,
        ARCHERY,
        CONTAINED,
        TAG;

        public static Codec<ContentType> CODEC;

        static {
            CODEC = Codec.STRING.xmap(ContentType::valueOf, Enum::name);
        }
    }
}


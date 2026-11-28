/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.Holder
 *  net.minecraft.core.Holder$Reference
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  org.jetbrains.annotations.Nullable
 */
package net.spell_engine.api.spell.container;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.spell_engine.api.item.SpellItemData;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.container.SpellChoice;
import net.spell_engine.api.spell.container.SpellContainer;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.internals.container.SpellAssignments;
import org.jetbrains.annotations.Nullable;

public class SpellContainerHelper {
    public static final Comparator<Map.Entry<ResourceLocation, Spell>> containerSorter = Comparator.comparingInt(entry -> ((Spell)entry.getValue()).tier).thenComparingInt(entry -> ((Spell)entry.getValue()).sub_tier).thenComparing(entry -> ((ResourceLocation)entry.getKey()).toString());
    public static final Comparator<Map.Entry<ResourceLocation, Spell>> catalogSorter = (entry1, entry2) -> SpellContainerHelper.compareInCatalog((ResourceLocation)entry1.getKey(), (Spell)entry1.getValue(), (ResourceLocation)entry2.getKey(), (Spell)entry2.getValue());
    public static final Comparator<Holder<Spell>> catalogEntrySorter = (entry1, entry2) -> SpellContainerHelper.compareInCatalog(((ResourceKey)entry1.m_203543_().get()).m_135782_(), (Spell)entry1.m_203334_(), ((ResourceKey)entry2.m_203543_().get()).m_135782_(), (Spell)entry2.m_203334_());

    public static SpellContainer containerFromItemStack(ItemStack itemStack) {
        if (itemStack.m_41619_()) {
            return null;
        }
        SpellContainer stored = SpellItemData.get(itemStack, "spell_container", SpellContainer.CODEC);
        if (stored != null) {
            return stored;
        }
        ResourceLocation id = BuiltInRegistries.f_257033_.m_7981_((Object)itemStack.m_41720_());
        SpellAssignments.Assignment assignment = SpellAssignments.assignment(id);
        if (assignment != null && assignment.container() != null) {
            return assignment.container();
        }
        SpellContainer itemDefault = SpellItemData.defaultSpellContainer(itemStack.m_41720_());
        if (itemDefault != null) {
            return itemDefault;
        }
        return SpellAssignments.fallbackContainerForItem(id);
    }

    @Nullable
    public static SpellChoice choiceFromItemStack(ItemStack itemStack) {
        if (itemStack.m_41619_()) {
            return null;
        }
        SpellChoice stored = SpellItemData.get(itemStack, "spell_choice", SpellChoice.CODEC);
        if (stored != null) {
            return stored;
        }
        ResourceLocation id = BuiltInRegistries.f_257033_.m_7981_((Object)itemStack.m_41720_());
        SpellAssignments.Assignment assignment = SpellAssignments.assignment(id);
        if (assignment != null) {
            if (assignment.choice() != null) {
                return assignment.choice();
            }
            if (assignment.container() != null) {
                return null;
            }
        }
        return SpellItemData.defaultSpellChoice(itemStack.m_41720_());
    }

    public static ResourceLocation getPoolId(SpellContainer container) {
        if (container != null && container.pool() != null) {
            return new ResourceLocation(container.pool());
        }
        return null;
    }

    public static boolean contains(SpellContainer container, ResourceLocation spellId) {
        return container != null && container.spell_ids().contains(spellId.toString());
    }

    public static List<String> sortedSpells(Level world, List<String> spellIds) {
        HashMap<ResourceLocation, Spell> spells = new HashMap<ResourceLocation, Spell>();
        for (String idString : spellIds) {
            ResourceLocation id = new ResourceLocation(idString);
            Holder.Reference spellEntry = SpellRegistry.from(world).m_203636_(ResourceKey.m_135785_(SpellRegistry.KEY, (ResourceLocation)id)).orElse(null);
            if (spellEntry == null) continue;
            spells.put(id, (Spell)spellEntry.m_203334_());
        }
        return spells.entrySet().stream().sorted(containerSorter).map(entry -> ((ResourceLocation)entry.getKey()).toString()).collect(Collectors.toList());
    }

    public static SpellContainer addSpell(Level world, ResourceLocation spellId, SpellContainer container) {
        ArrayList<String> spellIds = new ArrayList<String>(container.spell_ids());
        spellIds.add(spellId.toString());
        return container.copyWith(SpellContainerHelper.sortedSpells(world, spellIds));
    }

    public static void addSpell(Level world, ResourceLocation spellId, ItemStack itemStack) {
        SpellContainer container = SpellContainerHelper.containerFromItemStack(itemStack);
        if (container == null || !container.isValid()) {
            System.err.println("Trying to add spell: " + String.valueOf(spellId) + " to an ItemStack without valid spell container");
            return;
        }
        SpellContainer modifiedContainer = SpellContainerHelper.addSpell(world, spellId, container);
        SpellItemData.setSpellContainer(itemStack, modifiedContainer);
    }

    public static SpellContainer removeSpell(Level world, ResourceLocation spellId, SpellContainer container) {
        ArrayList<String> spellIds = new ArrayList<String>(container.spell_ids());
        spellIds.remove(spellId.toString());
        return container.copyWith(SpellContainerHelper.sortedSpells(world, spellIds));
    }

    public static void removeSpell(Level world, ResourceLocation spellId, ItemStack itemStack) {
        SpellContainer container = SpellContainerHelper.containerFromItemStack(itemStack);
        if (container == null || !container.isValid()) {
            System.err.println("Trying to remove spell: " + String.valueOf(spellId) + " from an ItemStack without valid spell container");
            return;
        }
        SpellContainer modifiedContainer = SpellContainerHelper.removeSpell(world, spellId, container);
        SpellItemData.setSpellContainer(itemStack, modifiedContainer);
    }

    public static int compareInCatalog(ResourceLocation id1, Spell spell1, ResourceLocation id2, Spell spell2) {
        int byTier = Integer.compare(spell1.tier, spell2.tier);
        if (byTier != 0) {
            return byTier;
        }
        int byNamespace = id1.m_135827_().compareTo(id2.m_135827_());
        if (byNamespace != 0) {
            return byNamespace;
        }
        int byGroup = SpellContainerHelper.groupOf(spell1).compareTo(SpellContainerHelper.groupOf(spell2));
        if (byGroup != 0) {
            return byGroup;
        }
        int bySubTier = Integer.compare(spell1.sub_tier, spell2.sub_tier);
        if (bySubTier != 0) {
            return bySubTier;
        }
        return id1.m_135815_().compareTo(id2.m_135815_());
    }

    private static String groupOf(Spell spell) {
        return spell.group != null ? spell.group : "";
    }

    public static boolean hasValidContainer(ItemStack itemStack) {
        return SpellContainerHelper.containerFromItemStack(itemStack) != null;
    }

    public static boolean hasBindableContainer(ItemStack itemStack) {
        SpellContainer container = SpellContainerHelper.containerFromItemStack(itemStack);
        return container != null && container.pool() != null && !container.pool().isEmpty();
    }

    public static boolean hasUsableContainer(ItemStack itemStack) {
        SpellContainer container = SpellContainerHelper.containerFromItemStack(itemStack);
        return container != null && (container.isUsable() || container.isResolver());
    }

    public static int poolTierSize(List<Holder<Spell>> spells) {
        HashSet<Integer> tiers = new HashSet<Integer>();
        for (Holder<Spell> spellEntry : spells) {
            Spell spell = (Spell)spellEntry.m_203334_();
            tiers.add(spell.tier);
        }
        return tiers.size();
    }
}


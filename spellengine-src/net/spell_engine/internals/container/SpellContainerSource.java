/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.Holder
 *  net.minecraft.core.Holder$Reference
 *  net.minecraft.core.Registry
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.tags.TagKey
 *  net.minecraft.world.entity.EquipmentSlot
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  org.jetbrains.annotations.Nullable
 */
package net.spell_engine.internals.container;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.spell_engine.Platform;
import net.spell_engine.SpellEngineMod;
import net.spell_engine.api.item.set.EquipmentSet;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.container.SpellContainer;
import net.spell_engine.api.spell.container.SpellContainerHelper;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.internals.casting.SpellCaster;
import net.spell_engine.network.Packets;
import org.jetbrains.annotations.Nullable;

public class SpellContainerSource {
    public static final List<Entry> sources = new ArrayList<Entry>();
    public static final List<ItemEntry> itemSources = new ArrayList<ItemEntry>();
    public static final ItemEntry MAIN_HAND = SpellContainerSource.itemEntry("main_hand", (player, sourceName) -> List.of(player.m_21205_()));
    public static final ItemEntry OFF_HAND = SpellContainerSource.itemEntry("off_hand", (player, sourceName) -> List.of(player.m_21206_()));
    public static final ItemEntry ARMOR = SpellContainerSource.itemEntry("armor", (player, sourceName) -> List.of((ItemStack)player.m_150109_().f_35975_.get(0), (ItemStack)player.m_150109_().f_35975_.get(1), (ItemStack)player.m_150109_().f_35975_.get(2), (ItemStack)player.m_150109_().f_35975_.get(3)));

    public static SpellContainer activeContainerOf(Player player) {
        return ((Owner)player).getSpellContainers().activeContainer;
    }

    public static List<Holder<Spell>> activeSpellsOf(Player player) {
        return ((Owner)player).getSpellContainers().actives;
    }

    public static List<Holder<Spell>> passiveSpellsOf(Player player) {
        return ((Owner)player).getSpellContainers().passives;
    }

    public static Result getSpellsOf(Player player) {
        return ((Owner)player).getSpellContainers();
    }

    public static void setDirty(Player player, Entry source) {
        SpellContainerSource.setDirty(player, source.name());
    }

    public static void setDirty(Player player, ItemEntry source) {
        SpellContainerSource.setDirty(player, source.name());
    }

    public static void setDirty(Player player, String source) {
        ((Owner)player).spellContainerCache().remove(source);
    }

    public static void setDirtyServerSide(Player player) {
        ((Owner)player).markServerSideSpellContainersDirty();
    }

    public static void syncServerSideContainers(Player player) {
        if (!player.m_9236_().f_46443_) {
            LinkedHashMap<String, SpellContainer> containers = ((Owner)player).serverSideSpellContainers();
            Packets.SpellContainerSync packet = new Packets.SpellContainerSync(containers);
            Platform.util().networkS2C_Send((ServerPlayer)player, packet);
            SpellContainerSource.setDirty(player, MAIN_HAND);
        }
    }

    private static Entry entry(String name, Source source) {
        return SpellContainerSource.entry(name, source, null);
    }

    private static Entry entry(String name, Source source, @Nullable DirtyChecker dirtyChecker) {
        Entry newEntry = new Entry(name, source, dirtyChecker);
        sources.add(newEntry);
        return newEntry;
    }

    private static ItemEntry itemEntry(String name, ItemStackSource source) {
        ItemEntry newEntry = ItemEntry.of(name, source);
        SpellContainerSource.addItemSource(newEntry);
        return newEntry;
    }

    public static void addSource(Entry entry) {
        sources.add(entry);
    }

    public static void addSource(Entry entry, @Nullable String after) {
        boolean added = false;
        if (after != null) {
            int index = -1;
            for (int i = 0; i < sources.size(); ++i) {
                if (!sources.get(i).name().equals(after)) continue;
                index = i;
                break;
            }
            if (index != -1) {
                sources.add(index + 1, entry);
                added = true;
            }
        }
        if (!added) {
            sources.add(entry);
        }
    }

    public static void addItemSource(ItemEntry entry) {
        SpellContainerSource.addItemSource(entry, null);
    }

    public static void addItemSource(ItemEntry entry, @Nullable String after) {
        itemSources.add(entry);
        SpellContainerSource.addSource(new Entry(entry.name(), entry.source(), entry.checker()), after);
    }

    public static void init() {
    }

    public static void update(Player player) {
        Owner owner = (Owner)player;
        ArrayList<SourcedContainer> allContainers = new ArrayList<SourcedContainer>();
        boolean updated = false;
        if (SpellEngineMod.config.spell_container_caching) {
            for (Entry entry : sources) {
                List<SourcedContainer> resolvedContainers = owner.spellContainerCache().get(entry.name());
                if (resolvedContainers != null) {
                    allContainers.addAll(resolvedContainers);
                    continue;
                }
                List<SourcedContainer> freshContainers = entry.source().getSpellContainers(player, entry.name());
                allContainers.addAll(freshContainers);
                owner.spellContainerCache().put(entry.name(), freshContainers);
                updated = true;
            }
        } else {
            for (Entry entry : sources) {
                List<SourcedContainer> freshContainers = entry.source().getSpellContainers(player, entry.name());
                allContainers.addAll(freshContainers);
            }
            updated = true;
        }
        for (Map.Entry entry : owner.serverSideSpellContainers().entrySet()) {
            allContainers.add(new SourcedContainer((String)entry.getKey(), null, (SpellContainer)entry.getValue()));
        }
        if (updated) {
            owner.spellModifierCache().clear();
            SpellContainerSource.updateEquipmentSets(player, allContainers);
            ItemStack heldItemStack = player.m_21205_();
            SpellContainer spellContainer = SpellContainerHelper.containerFromItemStack(heldItemStack);
            SpellContainer activeContainer = SpellContainer.EMPTY;
            List<Holder<Spell>> activeSpells = List.of();
            if (spellContainer != null && spellContainer.isResolver()) {
                MergeResult merged = SpellContainerSource.mergedContainerSources1(allContainers, spellContainer, Spell.Type.ACTIVE, player.m_9236_());
                activeContainer = merged.container();
                activeSpells = merged.spells();
            }
            List<Holder<Spell>> passiveSpells = SpellContainerSource.mergedContainerSources(allContainers, null, null, Spell.Type.PASSIVE, player.m_9236_());
            Registry<Spell> registry = SpellRegistry.from(player.m_9236_());
            LinkedHashSet<Holder.Reference> modifiers = new LinkedHashSet<Holder.Reference>();
            for (SourcedContainer container : allContainers) {
                SpellContainer spellContainer2 = container.container();
                for (String idString : spellContainer2.spell_ids()) {
                    ResourceLocation id = new ResourceLocation(idString);
                    Holder.Reference spell = registry.m_203636_(ResourceKey.m_135785_(SpellRegistry.KEY, (ResourceLocation)id)).orElse(null);
                    if (spell == null || ((Spell)spell.m_203334_()).type != Spell.Type.MODIFIER) continue;
                    modifiers.add(spell);
                }
            }
            ((Owner)player).setSpellContainers(new Result(activeContainer, activeSpells, passiveSpells, modifiers.stream().toList(), allContainers));
            if (!player.m_9236_().f_46443_) {
                ((SpellCaster.Player)player).getInteractor().invalidateOptions();
            }
        }
    }

    public static List<Holder<Spell>> mergedContainerSources(List<SourcedContainer> sources, @Nullable SpellContainer.ContentType contentType, @Nullable String accessParams, Spell.Type type, Level world) {
        ResourceLocation id;
        if (sources.isEmpty()) {
            return List.of();
        }
        ArrayList<Holder<Spell>> spells = new ArrayList<Holder<Spell>>();
        Registry<Spell> registry = SpellRegistry.from(world);
        TagKey spellTag = null;
        if (accessParams != null && !accessParams.isEmpty() && contentType == SpellContainer.ContentType.TAG && (id = ResourceLocation.m_135820_((String)accessParams)) != null) {
            spellTag = TagKey.m_203882_(SpellRegistry.KEY, (ResourceLocation)id);
        }
        for (SourcedContainer source : sources) {
            SpellContainer container = source.container();
            if (type == Spell.Type.ACTIVE && source.name.equals("off_hand") && !SpellEngineMod.config.spell_container_from_offhand_any && !container.slotMatches(EquipmentSlot.OFFHAND.m_20751_())) continue;
            for (String idString : container.spell_ids()) {
                ResourceLocation id2 = new ResourceLocation(idString);
                Holder.Reference spell = registry.m_203636_(ResourceKey.m_135785_(SpellRegistry.KEY, (ResourceLocation)id2)).orElse(null);
                if (spell == null || ((Spell)spell.m_203334_()).type != type || !SpellContainerSource.spellMatchesContentType((Holder<Spell>)spell, contentType, (TagKey<Spell>)spellTag)) continue;
                spells.add((Holder<Spell>)spell);
            }
        }
        return spells;
    }

    private static boolean spellMatchesContentType(Holder<Spell> spellEntry, @Nullable SpellContainer.ContentType contentType, @Nullable TagKey<Spell> spellTag) {
        boolean matches;
        block15: {
            boolean bl;
            block18: {
                block17: {
                    block16: {
                        if (contentType == null || contentType == SpellContainer.ContentType.ANY) {
                            return true;
                        }
                        if (contentType == SpellContainer.ContentType.TAG) {
                            return spellTag != null && spellEntry.m_203656_(spellTag);
                        }
                        Spell spell = (Spell)spellEntry.m_203334_();
                        switch (spell.school.archetype) {
                            case ARCHERY: {
                                boolean bl2;
                                if (contentType == SpellContainer.ContentType.ARCHERY) {
                                    bl2 = true;
                                    break;
                                }
                                bl2 = false;
                                break;
                            }
                            case MAGIC: 
                            case MELEE: {
                                boolean bl2;
                                if (contentType == SpellContainer.ContentType.MAGIC) {
                                    bl2 = true;
                                    break;
                                }
                                bl2 = false;
                                break;
                            }
                            default: {
                                boolean bl2 = matches = false;
                            }
                        }
                        if (spell.secondary_archetype == null) break block15;
                        if (matches) break block16;
                        switch (spell.secondary_archetype) {
                            case ARCHERY: {
                                if (contentType == SpellContainer.ContentType.ARCHERY) {
                                    break;
                                }
                                break block17;
                            }
                            case MAGIC: 
                            case MELEE: {
                                if (contentType == SpellContainer.ContentType.MAGIC) {
                                    break;
                                }
                                break block17;
                            }
                            case ANY: {
                                break;
                            }
                            default: {
                                break block17;
                            }
                        }
                    }
                    bl = true;
                    break block18;
                }
                bl = false;
            }
            matches = bl;
        }
        return matches;
    }

    public static MergeResult mergedContainerSources1(List<SourcedContainer> sources, SpellContainer heldContainer, Spell.Type type, Level world) {
        if (sources.isEmpty() || heldContainer.access() == SpellContainer.ContentType.NONE) {
            return MergeResult.EMPTY;
        }
        if (heldContainer.access() == SpellContainer.ContentType.CONTAINED) {
            ArrayList<Holder<Spell>> spells = new ArrayList<Holder<Spell>>();
            Registry<Spell> registry = SpellRegistry.from(world);
            for (String idString : heldContainer.spell_ids()) {
                ResourceLocation id = new ResourceLocation(idString);
                Holder.Reference spell = registry.m_203636_(ResourceKey.m_135785_(SpellRegistry.KEY, (ResourceLocation)id)).orElse(null);
                if (spell == null || ((Spell)spell.m_203334_()).type != type) continue;
                spells.add((Holder<Spell>)spell);
            }
            return new MergeResult(heldContainer, spells);
        }
        SpellContainer.ContentType contentType = heldContainer.access();
        List<Holder<Spell>> spells = SpellContainerSource.mergedContainerSources(sources, contentType, heldContainer.access_param(), type, world);
        LinkedHashSet<String> spellIds = new LinkedHashSet<String>();
        for (Holder<Spell> spell : spells) {
            spellIds.add(((ResourceKey)spell.m_203543_().get()).m_135782_().toString());
        }
        SpellContainer.ContentType finalContentType = contentType != null ? contentType : SpellContainer.ContentType.NONE;
        SpellContainer container = new SpellContainer(finalContentType, "", null, "", 0, new ArrayList<String>(spellIds), 0);
        return new MergeResult(container, spells);
    }

    @Nullable
    public static SourcedContainer getFirstSourceOfSpell(ResourceLocation spellId, Player player) {
        Result result = ((Owner)player).getSpellContainers();
        for (SourcedContainer source : result.sources()) {
            if (!SpellContainerSource.contains(source.container(), spellId)) continue;
            return source;
        }
        return null;
    }

    private static boolean contains(SpellContainer container, ResourceLocation spellId) {
        return container != null && container.spell_ids().contains(spellId.toString());
    }

    private static void updateEquipmentSets(Player player, ArrayList<SourcedContainer> allContainers) {
        ArrayList<EquipmentSet.SourcedItemStack> equipmentStacks = new ArrayList<EquipmentSet.SourcedItemStack>();
        for (ItemEntry entry : itemSources) {
            String sourceName = entry.name();
            List<ItemStack> stacks = entry.source().getSpellContainerItemStacks(player, sourceName);
            stacks.stream().map(stack -> new EquipmentSet.SourcedItemStack((ItemStack)stack, sourceName)).forEach(equipmentStacks::add);
        }
        List<EquipmentSet.Result> equipmentSets = EquipmentSet.collectFrom(equipmentStacks, player.m_9236_());
        ((EquipmentSet.Owner)player).setActiveEquipmentSets(equipmentSets);
        allContainers.addAll(SpellContainerSource.sourcedContainersFrom(equipmentSets));
    }

    private static List<SourcedContainer> sourcedContainersFrom(List<EquipmentSet.Result> results) {
        ArrayList<SourcedContainer> spellContainers = new ArrayList<SourcedContainer>();
        for (EquipmentSet.Result result : results) {
            EquipmentSet.Definition set = (EquipmentSet.Definition)result.set().m_203334_();
            for (EquipmentSet.Bonus bonus : set.bonuses()) {
                if (result.items().size() < bonus.requiredPieceCount() || bonus.spells() == null) continue;
                spellContainers.add(new SourcedContainer(set.name(), result.items().get(0), bonus.spells()));
            }
        }
        return spellContainers;
    }

    public static interface Owner {
        public Map<String, List<SourcedContainer>> spellContainerCache();

        public Map<ResourceLocation, List<Spell.Modifier>> spellModifierCache();

        public LinkedHashMap<String, SpellContainer> serverSideSpellContainers();

        public void markServerSideSpellContainersDirty();

        public void setSpellContainers(Result var1);

        public Result getSpellContainers();
    }

    public record Result(SpellContainer activeContainer, List<Holder<Spell>> actives, List<Holder<Spell>> passives, List<Holder<Spell>> modifiers, List<SourcedContainer> sources) {
        public static final Result EMPTY = new Result(SpellContainer.EMPTY, List.of(), List.of(), List.of(), List.of());
    }

    public record Entry(String name, Source source, @Nullable DirtyChecker checker) {
    }

    public record ItemEntry(String name, ItemStackSource source, @Nullable DirtyChecker checker) {
        public static ItemEntry of(String name, ItemStackSource source, @Nullable DirtyChecker dirtyChecker) {
            return new ItemEntry(name, source, dirtyChecker);
        }

        public static ItemEntry of(String name, ItemStackSource source) {
            return ItemEntry.of(name, source, player -> source.getSpellContainerItemStacks(player, name));
        }
    }

    public static interface Source {
        public List<SourcedContainer> getSpellContainers(Player var1, String var2);
    }

    public static interface DirtyChecker {
        public Object current(Player var1);
    }

    public static interface ItemStackSource
    extends Source {
        public List<ItemStack> getSpellContainerItemStacks(Player var1, String var2);

        @Override
        default public List<SourcedContainer> getSpellContainers(Player player, String name) {
            List<ItemStack> itemStacks = this.getSpellContainerItemStacks(player, name);
            ArrayList<SourcedContainer> sources = new ArrayList<SourcedContainer>();
            for (ItemStack itemStack : itemStacks) {
                SpellContainer container = SpellContainerHelper.containerFromItemStack(itemStack);
                if (container == null || !container.isValid()) continue;
                sources.add(new SourcedContainer(name, itemStack, container));
            }
            return sources;
        }
    }

    public record SourcedContainer(String name, @Nullable ItemStack itemStack, SpellContainer container) {
    }

    public record MergeResult(SpellContainer container, List<Holder<Spell>> spells) {
        public static final MergeResult EMPTY = new MergeResult(SpellContainer.EMPTY, List.of());
    }
}


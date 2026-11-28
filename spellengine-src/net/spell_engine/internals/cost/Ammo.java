/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.tags.TagKey
 *  net.minecraft.world.Container
 *  net.minecraft.world.ContainerHelper
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.enchantment.Enchantment
 *  net.minecraft.world.item.enchantment.EnchantmentHelper
 *  net.minecraft.world.item.enchantment.Enchantments
 *  org.jetbrains.annotations.Nullable
 */
package net.spell_engine.internals.cost;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.spell_engine.SpellEngineMod;
import net.spell_engine.api.enchantment.SpellEngineEnchantments;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.compat.container.ContainerCompat;
import org.jetbrains.annotations.Nullable;

public class Ammo {
    public static Result ammoForSpell(Player player, Spell spell, ItemStack casterStack) {
        boolean satisfied = true;
        Searched ammo = null;
        int consume = 0;
        List<Source> sources = List.of();
        if (spell.cost.item != null && spell.cost.item.id != null && !spell.cost.item.id.isEmpty()) {
            boolean hasInfinity;
            ammo = Searched.from(spell.cost.item.id);
            if (player.m_150110_().f_35937_ || !SpellEngineMod.config.spell_cost_item_allowed) {
                return new Result(satisfied, ammo, consume, sources);
            }
            ResourceLocation id = new ResourceLocation(spell.cost.item.id);
            boolean needsArrow = id.m_135815_().contains("arrow");
            boolean bl = needsArrow ? EnchantmentHelper.m_44843_((Enchantment)Enchantments.f_44952_, (ItemStack)casterStack) > 0 : (hasInfinity = EnchantmentHelper.m_44843_((Enchantment)SpellEngineEnchantments.SPELL_INFINITY, (ItemStack)casterStack) > 0);
            if (hasInfinity) {
                return new Result(satisfied, ammo, consume, sources);
            }
            if (ammo.isValid()) {
                int amountNeeded = spell.cost.item.amount;
                sources = Ammo.findSources(player, ammo, amountNeeded);
                int amountAvailable = sources.stream().mapToInt(Source::found).sum();
                satisfied = amountAvailable >= amountNeeded;
                consume = satisfied && spell.cost.item.consume ? amountAvailable : 0;
            }
        }
        return new Result(satisfied, ammo, consume, sources);
    }

    public static List<Source> findSources(Player player, Searched searched, int totalAmount) {
        ArrayList<Source> sources = new ArrayList<Source>();
        int foundAmount = 0;
        Source container = Ammo.findContainer(player, searched.asPredicate(), totalAmount);
        if (container != null) {
            sources.add(container);
            foundAmount += container.found();
        }
        if (foundAmount < totalAmount) {
            Inventory inventory = player.m_150109_();
            for (int i = 0; i < inventory.m_6643_(); ++i) {
                ItemStack stack = inventory.m_8020_(i);
                if (searched.matches(stack)) {
                    Source source = Ammo.sourceFromStack(stack, totalAmount - foundAmount);
                    sources.add(source);
                    foundAmount += source.found();
                }
                if (foundAmount >= totalAmount) break;
            }
        }
        return sources;
    }

    private static Source sourceFromStack(ItemStack stack, int amount) {
        int found = Math.min(stack.m_41613_(), amount);
        return new Source(stack, found, false);
    }

    @Nullable
    public static Source findContainer(Player player, Predicate<ItemStack> item, int amount) {
        for (Function<Player, List<ItemStack>> provider : ContainerCompat.providers) {
            List<ItemStack> stacks = provider.apply(player);
            for (ItemStack stack : stacks) {
                int found = Math.min(Ammo.findInContainer(stack, item), amount);
                if (found <= 0) continue;
                return new Source(stack, found, true);
            }
        }
        return null;
    }

    public static int findInContainer(ItemStack containerStack, Predicate<ItemStack> consumedItem) {
        int found = 0;
        ContainerCompat.Adapter bundle = ContainerCompat.getContainerComponent(containerStack);
        if (bundle != null) {
            for (int i = 0; i < bundle.size(); ++i) {
                ItemStack storedStack = bundle.get(i);
                if (!consumedItem.test(storedStack)) continue;
                found += storedStack.m_41613_();
            }
        }
        return found;
    }

    public static ItemStack findFirstInContainer(ItemStack containerStack, Predicate<ItemStack> consumedItem) {
        ContainerCompat.Adapter bundle = ContainerCompat.getContainerComponent(containerStack);
        if (bundle != null) {
            for (int i = 0; i < bundle.size(); ++i) {
                ItemStack storedStack = bundle.get(i);
                if (!consumedItem.test(storedStack)) continue;
                return storedStack;
            }
        }
        return ItemStack.f_41583_;
    }

    public static void consume(Result result, Player player) {
        if (result.consume() > 0) {
            for (Source source : result.sources()) {
                if (source.isContainer()) {
                    Ammo.takeFromContainer(source.itemStack(), result.item(), source.found());
                    continue;
                }
                ContainerHelper.m_18956_((Container)player.m_150109_(), result.item().asPredicate(), (int)source.found(), (boolean)false);
            }
        }
    }

    public static int takeFromContainer(ItemStack containerStack, Searched consumedItem, int amount) {
        return Ammo.takeFromContainer(containerStack, consumedItem.asPredicate(), amount);
    }

    public static int takeFromContainer(ItemStack containerStack, Predicate<ItemStack> consumedItem, int amount) {
        int taken = 0;
        ContainerCompat.Adapter bundle = ContainerCompat.getContainerComponent(containerStack);
        int toDecreement = amount;
        if (bundle != null) {
            ArrayList<ItemStack> putBack = new ArrayList<ItemStack>();
            for (int i = 0; i < bundle.size(); ++i) {
                ItemStack storedStack = bundle.get(i);
                if (consumedItem.test(storedStack)) {
                    int decrementable = Math.min(storedStack.m_41613_(), toDecreement);
                    storedStack.m_41774_(decrementable);
                    toDecreement -= decrementable;
                    taken += decrementable;
                }
                if (storedStack.m_41619_()) continue;
                putBack.add(storedStack);
            }
            Collections.reverse(putBack);
            ContainerCompat.Adapter newBundle = bundle.createNewWithContents(putBack);
            newBundle.attachTo(containerStack);
        }
        return taken;
    }

    public record Searched(@Nullable TagKey<Item> tag, @Nullable Item item) {
        public static Searched from(String stringId) {
            if (stringId.startsWith("#")) {
                return new Searched((TagKey<Item>)TagKey.m_203882_((ResourceKey)Registries.f_256913_, (ResourceLocation)new ResourceLocation(stringId.substring(1))), null);
            }
            return new Searched(null, (Item)BuiltInRegistries.f_257033_.m_7745_(new ResourceLocation(stringId)));
        }

        public boolean isValid() {
            return this.item != null || this.tag != null;
        }

        public boolean matches(ItemStack stack) {
            if (this.tag != null) {
                return stack.m_204117_(this.tag);
            }
            if (this.item != null) {
                return stack.m_150930_(this.item);
            }
            return false;
        }

        public Predicate<ItemStack> asPredicate() {
            return this::matches;
        }

        public String getTranslationKey() {
            if (this.tag != null) {
                return "tag.item." + this.tag.f_203868_().m_135827_() + "." + this.tag.f_203868_().m_135815_();
            }
            if (this.item != null) {
                return this.item.m_5524_();
            }
            return "";
        }
    }

    public record Result(boolean satisfied, Searched item, int consume, List<Source> sources) {
    }

    public record Source(ItemStack itemStack, int found, boolean isContainer) {
    }
}


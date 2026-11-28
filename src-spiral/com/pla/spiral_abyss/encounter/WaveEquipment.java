/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.entity.EquipmentSlot
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.entity.monster.Vindicator
 *  net.minecraft.world.item.AxeItem
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.item.enchantment.Enchantment
 *  net.minecraft.world.item.enchantment.EnchantmentHelper
 *  net.minecraft.world.item.enchantment.Enchantments
 *  net.minecraft.world.level.ItemLike
 */
package com.pla.spiral_abyss.encounter;

import com.mojang.logging.LogUtils;
import com.pla.spiral_abyss.config.GearConfig;
import com.pla.spiral_abyss.encounter.data.MobCatalog;
import com.pla.spiral_abyss.platform.AbyssRegistries;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;

public final class WaveEquipment {
    private static final EquipmentSlot[] ARMOR = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private final MobCatalog.Equipment whitelist;
    private final GearConfig.Floor floor;
    private final List<Item> armor;
    private final List<Item> weapons;

    public WaveEquipment(MobCatalog.Equipment whitelist, GearConfig.Floor floor, RandomSource random) {
        this.whitelist = whitelist;
        this.floor = floor;
        ArrayList<List<Item>> sets = new ArrayList<List<Item>>();
        for (List<ResourceLocation> ids : floor.armorSets()) {
            List<Item> items = ids.stream().map(WaveEquipment::item).toList();
            boolean valid = items.size() == ARMOR.length;
            for (int i = 0; valid && i < ARMOR.length; ++i) {
                valid = items.get(i) != null && Mob.m_147233_((ItemStack)new ItemStack((ItemLike)items.get(i))) == ARMOR[i];
            }
            if (valid) {
                sets.add(items);
                continue;
            }
            LogUtils.getLogger().warn("Skipping unavailable or incorrectly ordered Abyss armor set: {}", ids);
        }
        this.armor = sets.isEmpty() ? List.of() : (List)sets.get(random.m_188503_(sets.size()));
        this.weapons = floor.weapons().stream().map(WaveEquipment::item).filter(Objects::nonNull).toList();
    }

    private static Item item(ResourceLocation id) {
        Item item = AbyssRegistries.ITEMS.containsKey(id) ? AbyssRegistries.ITEMS.getValue(id) : null;
        return item == Items.f_41852_ ? null : item;
    }

    public void apply(Mob mob) {
        mob.m_21553_(false);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            mob.m_21409_(slot, 0.0f);
        }
        ResourceLocation id = AbyssRegistries.ENTITY_TYPES.getKey(mob.m_6095_());
        RandomSource random = mob.m_217043_();
        if (this.whitelist.armor().contains(id)) {
            for (EquipmentSlot slot : ARMOR) {
                mob.m_8061_(slot, ItemStack.f_41583_);
            }
            if (!this.armor.isEmpty() && random.m_188500_() < this.floor.armorChance()) {
                boolean enchanted = random.m_188500_() < this.floor.armor().chance();
                for (int i = 0; i < ARMOR.length; ++i) {
                    ItemStack stack = new ItemStack((ItemLike)this.armor.get(i));
                    if (enchanted) {
                        WaveEquipment.enchant(stack, this.floor.armor(), random, true);
                    }
                    mob.m_8061_(ARMOR[i], stack);
                }
            }
        }
        if (this.whitelist.tridents().contains(id)) {
            ItemStack stack = new ItemStack((ItemLike)Items.f_42713_);
            stack.m_41663_(Enchantments.f_44955_, this.floor.loyaltyLevel());
            WaveEquipment.maybeEnchant(stack, this.floor.trident(), random);
            WaveEquipment.weapon(mob, stack);
        } else if (this.whitelist.crossbows().contains(id)) {
            ItemStack stack = new ItemStack((ItemLike)Items.f_42717_);
            WaveEquipment.maybeEnchant(stack, this.floor.crossbow(), random);
            WaveEquipment.weapon(mob, stack);
        } else if (this.whitelist.bows().contains(id)) {
            ItemStack stack = new ItemStack((ItemLike)Items.f_42411_);
            WaveEquipment.maybeEnchant(stack, this.floor.bow(), random);
            WaveEquipment.weapon(mob, stack);
        } else if (this.whitelist.melee().contains(id)) {
            if (mob instanceof Vindicator) {
                if (!this.weapons.isEmpty() && random.m_188500_() < this.floor.weaponChance()) {
                    Item nativeItem = mob.m_21205_().m_41619_() ? Items.f_42386_ : mob.m_21205_().m_41720_();
                    List<AxeItem> upgrades = this.weapons.stream().filter(AxeItem.class::isInstance).map(AxeItem.class::cast).filter(axe -> {
                        if (!(nativeItem instanceof AxeItem)) return false;
                        AxeItem current = (AxeItem)nativeItem;
                        if (axe.m_43314_().m_6604_() <= current.m_43314_().m_6604_()) return false;
                        return true;
                    }).toList();
                    if (!upgrades.isEmpty()) {
                        ItemStack stack = new ItemStack((ItemLike)upgrades.get(random.m_188503_(upgrades.size())));
                        WaveEquipment.maybeEnchant(stack, this.floor.melee(), random);
                        WaveEquipment.weapon(mob, stack);
                    }
                }
                if (mob.m_21205_().m_41619_()) {
                    WaveEquipment.weapon(mob, new ItemStack((ItemLike)Items.f_42386_));
                }
                return;
            }
            WaveEquipment.weapon(mob, ItemStack.f_41583_);
            if (!this.weapons.isEmpty() && random.m_188500_() < this.floor.weaponChance()) {
                ItemStack stack = new ItemStack((ItemLike)this.weapons.get(random.m_188503_(this.weapons.size())));
                WaveEquipment.maybeEnchant(stack, this.floor.melee(), random);
                WaveEquipment.weapon(mob, stack);
            }
        }
    }

    private static void weapon(Mob mob, ItemStack stack) {
        mob.m_8061_(EquipmentSlot.OFFHAND, ItemStack.f_41583_);
        mob.m_8061_(EquipmentSlot.MAINHAND, stack);
    }

    private static void maybeEnchant(ItemStack stack, GearConfig.Rule rule, RandomSource random) {
        if (random.m_188500_() < rule.chance()) {
            WaveEquipment.enchant(stack, rule, random, false);
        }
    }

    private static void enchant(ItemStack stack, GearConfig.Rule rule, RandomSource random, boolean armor) {
        ArrayList<Enchantment> pool = new ArrayList<Enchantment>();
        for (ResourceLocation id : rule.entries()) {
            Enchantment enchantment = AbyssRegistries.ENCHANTMENTS.getValue(id);
            if (enchantment == null || pool.contains(enchantment) || armor && !enchantment.m_6081_(stack)) continue;
            pool.add(enchantment);
        }
        int count = pool.isEmpty() ? 0 : 1 + random.m_188503_(Math.min(3, pool.size()));
        ArrayList selected = new ArrayList(EnchantmentHelper.m_44831_((ItemStack)stack).keySet());
        while (count > 0 && !pool.isEmpty()) {
            Enchantment next = (Enchantment)pool.remove(random.m_188503_(pool.size()));
            if (selected.stream().anyMatch(previous -> previous == next || !previous.m_44695_(next) || !next.m_44695_(previous))) continue;
            int level = next == Enchantments.f_44960_ ? Math.min(3, rule.level()) : (next == Enchantments.f_44959_ ? 1 : rule.level());
            stack.m_41663_(next, level);
            selected.add(next);
            --count;
        }
    }
}


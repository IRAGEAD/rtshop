/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.Registry
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.effect.MobEffect
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.enchantment.Enchantment
 *  net.minecraft.world.level.block.Block
 */
package com.pla.spiral_abyss.platform;

import java.util.Collection;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;

public final class AbyssRegistries {
    public static final Lookup<Item> ITEMS = new Lookup(BuiltInRegistries.f_257033_);
    public static final Lookup<Block> BLOCKS = new Lookup(BuiltInRegistries.f_256975_);
    public static final Lookup<EntityType<?>> ENTITY_TYPES = new Lookup(BuiltInRegistries.f_256780_);
    public static final Lookup<MobEffect> MOB_EFFECTS = new Lookup(BuiltInRegistries.f_256974_);
    public static final Lookup<Enchantment> ENCHANTMENTS = new Lookup(BuiltInRegistries.f_256876_);

    public record Lookup<T>(Registry<T> registry) {
        public boolean containsKey(ResourceLocation id) {
            return this.registry.m_7804_(id);
        }

        public T getValue(ResourceLocation id) {
            return (T)(this.registry.m_7804_(id) ? this.registry.m_7745_(id) : null);
        }

        public ResourceLocation getKey(T value) {
            return this.registry.m_7981_(value);
        }

        public Collection<T> getValues() {
            return this.registry.m_123024_().toList();
        }
    }
}


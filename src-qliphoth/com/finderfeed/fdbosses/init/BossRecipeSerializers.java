/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.world.item.crafting.RecipeSerializer
 *  net.minecraftforge.registries.DeferredRegister
 */
package com.finderfeed.fdbosses.init;

import com.finderfeed.fdbosses.content.recipes.CoreRecipe;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;

public class BossRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create((ResourceKey)Registries.f_256764_, (String)"fdbosses");
    public static final Supplier<CoreRecipe.Serializer> WEAPON_CORE = RECIPE_SERIALIZERS.register("weapon_core", CoreRecipe.Serializer::new);
}


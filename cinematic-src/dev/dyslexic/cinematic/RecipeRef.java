/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.item.crafting.Recipe
 */
package dev.dyslexic.cinematic;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

public record RecipeRef<T>(ResourceLocation id, T value) {
    public static <T extends Recipe<?>> RecipeRef<T> of(T recipe) {
        return new RecipeRef<T>(recipe.m_6423_(), recipe);
    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.item.ItemStack
 */
package dev.dyslexic.cinematic.client.guide;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public interface RecipeViewer {
    public List<ItemStack> search(String var1, int var2);

    public List<ItemStack> stationOutputs(ItemStack var1);

    public List<StationRecipe> stationRecipes(ItemStack var1, ItemStack var2);

    public void showHowToGet(ItemStack var1);

    public void showUses(ItemStack var1);

    public List<SourcePage> sources(ItemStack var1);

    @Nullable
    public Runnable professionPage(ItemStack var1);

    public boolean isShowRecipeKey(int var1, int var2);

    public boolean isShowUsesKey(int var1, int var2);

    public Component keyNames();

    public static interface Icon {
        public void draw(GuiGraphics var1, int var2, int var3);
    }

    public record SourcePage(Component title, int entries, Runnable open, Icon icon) {
    }

    public static interface StationRecipe {
        public Component category();

        @Nullable
        public ResourceLocation recipeId();

        public List<ItemStack> inputs();

        public int width();

        public int height();

        public void draw(GuiGraphics var1, int var2, int var3, int var4, int var5);

        public void open();
    }
}


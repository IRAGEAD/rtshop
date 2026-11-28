/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.data_structures.Pair
 *  com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec
 *  com.google.gson.JsonObject
 *  net.minecraft.core.RegistryAccess
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.inventory.CraftingContainer
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.crafting.CraftingBookCategory
 *  net.minecraft.world.item.crafting.CraftingRecipe
 *  net.minecraft.world.item.crafting.RecipeSerializer
 *  net.minecraft.world.level.Level
 *  org.jetbrains.annotations.Nullable
 */
package com.finderfeed.fdbosses.content.recipes;

import com.finderfeed.fdbosses.content.items.CoreItem;
import com.finderfeed.fdbosses.init.BossRecipeSerializers;
import com.finderfeed.fdlib.data_structures.Pair;
import com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec;
import com.google.gson.JsonObject;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class CoreRecipe
implements CraftingRecipe {
    private ResourceLocation id;

    public CoreRecipe(ResourceLocation id) {
        this.id = id;
    }

    public CraftingBookCategory m_245232_() {
        return CraftingBookCategory.MISC;
    }

    public boolean matches(CraftingContainer input, Level level) {
        Pair<ItemStack, ItemStack> weaponAndCore = this.getItemAndCore(input);
        return weaponAndCore != null;
    }

    public ItemStack assemble(CraftingContainer input, RegistryAccess registryAccess) {
        Pair<ItemStack, ItemStack> weaponAndCore = this.getItemAndCore(input);
        if (weaponAndCore == null) {
            return ItemStack.f_41583_;
        }
        ItemStack weapon = (ItemStack)weaponAndCore.first;
        ItemStack coreStack = (ItemStack)weaponAndCore.second;
        if (weapon != null && coreStack != null) {
            ItemStack result = weapon.m_41777_();
            CoreItem.setItemCore(((CoreItem)coreStack.m_41720_()).coreType, result);
            return result;
        }
        return ItemStack.f_41583_;
    }

    private Pair<ItemStack, ItemStack> getItemAndCore(CraftingContainer input) {
        List<ItemStack> items = input.m_280657_().stream().filter(itemstack -> !itemstack.m_41619_()).toList();
        if (items.size() != 2) {
            return null;
        }
        ItemStack theItem = null;
        ItemStack coreStack = null;
        ItemStack firstStack = items.get(0);
        ItemStack secondStack = items.get(1);
        if (firstStack.m_41720_() instanceof CoreItem) {
            coreStack = firstStack;
            theItem = secondStack;
        } else if (secondStack.m_41720_() instanceof CoreItem) {
            coreStack = secondStack;
            theItem = firstStack;
        }
        if (coreStack == null) {
            return null;
        }
        CoreItem coreItem = (CoreItem)coreStack.m_41720_();
        if (coreItem.canBeAppliedTo(theItem)) {
            return new Pair((Object)theItem, (Object)coreStack);
        }
        return null;
    }

    public boolean m_8004_(int x, int y) {
        return true;
    }

    public ItemStack m_8043_(RegistryAccess registryAccess) {
        return ItemStack.f_41583_;
    }

    public ResourceLocation m_6423_() {
        return this.id;
    }

    public RecipeSerializer<?> m_7707_() {
        return BossRecipeSerializers.WEAPON_CORE.get();
    }

    public static class Serializer
    implements RecipeSerializer<CoreRecipe> {
        public static final NetworkCodec<CoreRecipe> STREAM_CODEC = NetworkCodec.composite((NetworkCodec)NetworkCodec.RESOURCE_LOCATION, CoreRecipe::m_6423_, CoreRecipe::new);

        public CoreRecipe fromJson(ResourceLocation resourceLocation, JsonObject jsonObject) {
            return new CoreRecipe(resourceLocation);
        }

        @Nullable
        public CoreRecipe fromNetwork(ResourceLocation resourceLocation, FriendlyByteBuf friendlyByteBuf) {
            return (CoreRecipe)STREAM_CODEC.fromNetwork(friendlyByteBuf);
        }

        public void toNetwork(FriendlyByteBuf friendlyByteBuf, CoreRecipe weaponCoreRecipe) {
            STREAM_CODEC.toNetwork(friendlyByteBuf, (Object)weaponCoreRecipe);
        }
    }
}


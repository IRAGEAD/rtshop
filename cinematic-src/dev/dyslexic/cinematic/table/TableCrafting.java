/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.Direction
 *  net.minecraft.core.NonNullList
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.Container
 *  net.minecraft.world.SimpleContainer
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.crafting.CraftingRecipe
 *  net.minecraft.world.item.crafting.Recipe
 *  net.minecraft.world.item.crafting.RecipeType
 *  net.minecraft.world.level.GameRules
 *  net.minecraft.world.level.Level
 *  net.minecraftforge.common.ForgeHooks
 *  net.minecraftforge.event.ForgeEventFactory
 */
package dev.dyslexic.cinematic.table;

import dev.dyslexic.cinematic.station.RecipeChoices;
import dev.dyslexic.cinematic.table.TableGrid;
import dev.dyslexic.cinematic.table.TableInput;
import dev.dyslexic.cinematic.table.TableStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ForgeEventFactory;

public final class TableCrafting {
    private TableCrafting() {
    }

    public static Optional<Match> match(Level level, List<ItemStack> world, Direction side) {
        return TableCrafting.match(level, world, side, null);
    }

    public static Optional<Match> match(Level level, List<ItemStack> world, Direction side, @Nullable ResourceLocation chosen) {
        TableInput input = new TableInput(TableGrid.oriented(world, side));
        if (input.m_7983_()) {
            return Optional.empty();
        }
        Optional found = Optional.empty();
        if (chosen != null) {
            found = RecipeChoices.matching(level, RecipeType.f_44107_, input).stream().filter(recipe -> recipe.m_6423_().equals((Object)chosen)).findFirst();
        }
        if (found.isEmpty()) {
            found = level.m_7465_().m_44015_(RecipeType.f_44107_, (Container)input, level);
        }
        if (found.isEmpty()) {
            return Optional.empty();
        }
        ItemStack result = ((CraftingRecipe)found.get()).m_5874_((Container)input, level.m_9598_());
        if (result.m_41619_() || !result.m_246617_(level.m_246046_())) {
            return Optional.empty();
        }
        return Optional.of(new Match((CraftingRecipe)found.get(), input, result));
    }

    public static List<ItemStack> craftOnce(ServerPlayer player, ServerLevel level, TableStore.Table table, Direction side, boolean toInventory) {
        Optional<Match> found = TableCrafting.match((Level)level, table.items, side, RecipeChoices.chosen((Player)player));
        if (found.isEmpty()) {
            return null;
        }
        Match match = found.get();
        CraftingRecipe recipe = match.recipe();
        if (!recipe.m_5598_() && level.m_46469_().m_46207_(GameRules.f_46151_) && !player.m_8952_().m_12709_((Recipe)recipe)) {
            return null;
        }
        ItemStack result = match.result();
        ItemStack carried = player.f_36096_.m_142621_();
        if (toInventory ? !TableCrafting.canFit(player.m_150109_(), result) : !carried.m_41619_() && (!ItemStack.m_150942_((ItemStack)carried, (ItemStack)result) || carried.m_41613_() + result.m_41613_() > carried.m_41741_())) {
            return null;
        }
        List<ItemStack> grid = TableGrid.oriented(table.items, side);
        ArrayList<ItemStack> snapshot = new ArrayList<ItemStack>(grid.size());
        for (ItemStack stack : grid) {
            snapshot.add(stack.m_41777_());
        }
        result.m_41678_((Level)level, (Player)player, result.m_41613_());
        ForgeEventFactory.firePlayerCraftingEvent((Player)player, (ItemStack)result, (Container)new SimpleContainer((ItemStack[])snapshot.toArray(ItemStack[]::new)));
        player.m_280300_((Recipe)recipe, snapshot);
        if (!recipe.m_5598_()) {
            player.m_7281_(Collections.singleton(recipe));
        }
        TableInput input = match.input();
        ForgeHooks.setCraftingPlayer((Player)player);
        NonNullList remaining = level.m_7465_().m_44069_(RecipeType.f_44107_, (Container)input, (Level)level);
        ForgeHooks.setCraftingPlayer(null);
        ArrayList<ItemStack> consumed = new ArrayList<ItemStack>();
        for (int row = 0; row < input.m_39346_(); ++row) {
            for (int col = 0; col < input.m_39347_(); ++col) {
                int world = TableGrid.worldIndex(side, row, col);
                ItemStack stack = (ItemStack)table.items.get(world);
                ItemStack leftover = (ItemStack)remaining.get(col + row * input.m_39347_());
                if (!stack.m_41619_()) {
                    consumed.add(stack.m_255036_(1));
                    stack.m_41774_(1);
                    if (stack.m_41619_()) {
                        table.items.set(world, (Object)ItemStack.f_41583_);
                    }
                    stack = (ItemStack)table.items.get(world);
                }
                if (leftover.m_41619_()) continue;
                if (stack.m_41619_()) {
                    table.items.set(world, (Object)leftover);
                    continue;
                }
                if (ItemStack.m_150942_((ItemStack)stack, (ItemStack)leftover)) {
                    leftover.m_41769_(stack.m_41613_());
                    table.items.set(world, (Object)leftover);
                    continue;
                }
                if (player.m_150109_().m_36054_(leftover)) continue;
                player.m_36176_(leftover, false);
            }
        }
        if (toInventory) {
            if (!player.m_150109_().m_36054_(result)) {
                player.m_36176_(result, false);
            }
        } else if (carried.m_41619_()) {
            player.f_36096_.m_142503_(result);
        } else {
            carried.m_41769_(result.m_41613_());
        }
        return consumed;
    }

    private static boolean canFit(Inventory inventory, ItemStack stack) {
        return inventory.m_36050_(stack) != -1 || inventory.m_36062_() != -1;
    }

    public record Match(CraftingRecipe recipe, TableInput input, ItemStack result) {
    }
}


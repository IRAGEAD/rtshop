/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.illusivesoulworks.polymorph.api.PolymorphApi
 *  javax.annotation.Nullable
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.Container
 *  net.minecraft.world.SimpleContainer
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.inventory.SmithingMenu
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.crafting.AbstractCookingRecipe
 *  net.minecraft.world.item.crafting.Recipe
 *  net.minecraft.world.item.crafting.RecipeType
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraftforge.fml.ModList
 */
package dev.dyslexic.cinematic.station;

import com.illusivesoulworks.polymorph.api.PolymorphApi;
import dev.dyslexic.cinematic.furnace.FurnaceActions;
import dev.dyslexic.cinematic.station.Sessions;
import dev.dyslexic.cinematic.station.StationKind;
import dev.dyslexic.cinematic.table.TableGrid;
import dev.dyslexic.cinematic.table.TableInput;
import dev.dyslexic.cinematic.table.TableStore;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.ModList;

public final class RecipeChoices {
    private static final boolean POLYMORPH = ModList.get().isLoaded("polymorph");

    private RecipeChoices() {
    }

    public static boolean available() {
        return POLYMORPH;
    }

    public static <C extends Container, T extends Recipe<C>> List<T> matching(Level level, RecipeType<T> type, C input) {
        ArrayList<Recipe> recipes = new ArrayList<Recipe>(level.m_7465_().m_44056_(type, input, level));
        recipes.removeIf(recipe -> recipe.m_5874_(input, level.m_9598_()).m_41619_());
        recipes.sort(Comparator.comparing(recipe -> recipe.m_6423_().toString()));
        return recipes;
    }

    @Nullable
    public static ResourceLocation chosen(Player player) {
        return POLYMORPH ? Polymorph.chosen(player) : null;
    }

    public static RecipeType<? extends AbstractCookingRecipe> cookingType(StationKind kind) {
        return switch (kind) {
            case StationKind.SMOKER -> RecipeType.f_44110_;
            case StationKind.BLAST_FURNACE -> RecipeType.f_44109_;
            default -> RecipeType.f_44108_;
        };
    }

    public static String furnaceChoice(AbstractFurnaceBlockEntity furnace, StationKind kind) {
        if (!POLYMORPH || furnace.m_58904_() == null || furnace.m_8020_(0).m_41619_()) {
            return "";
        }
        SimpleContainer input = new SimpleContainer(new ItemStack[]{furnace.m_8020_(0)});
        List<? extends AbstractCookingRecipe> recipes = RecipeChoices.matching(furnace.m_58904_(), RecipeChoices.cookingType(kind), input);
        if (recipes.size() < 2) {
            return "";
        }
        ResourceLocation selected = Polymorph.chosen((BlockEntity)furnace);
        return selected != null && recipes.stream().anyMatch(recipe -> recipe.m_6423_().equals((Object)selected)) ? selected.toString() : "";
    }

    static void choose(ServerPlayer player, ServerLevel level, Sessions.Session session, int index) {
        if (!POLYMORPH || index < 0) {
            return;
        }
        if (session.kind().isFurnace()) {
            AbstractFurnaceBlockEntity furnace;
            BlockEntity blockEntity = level.m_7702_(session.pos());
            if (!(blockEntity instanceof AbstractFurnaceBlockEntity) || (furnace = (AbstractFurnaceBlockEntity)blockEntity).m_8020_(0).m_41619_()) {
                return;
            }
            List<? extends AbstractCookingRecipe> recipes = RecipeChoices.matching((Level)level, RecipeChoices.cookingType(session.kind()), new SimpleContainer(new ItemStack[]{furnace.m_8020_(0)}));
            if (index < recipes.size()) {
                Polymorph.choose((BlockEntity)furnace, (Recipe)recipes.get(index));
                furnace.m_6596_();
                FurnaceActions.sendState(player, session);
            }
            return;
        }
        if (session.kind() == StationKind.SMITHING) {
            AbstractContainerMenu recipes = player.f_36096_;
            if (!(recipes instanceof SmithingMenu)) {
                return;
            }
            SmithingMenu menu = (SmithingMenu)recipes;
            SimpleContainer input = new SimpleContainer(new ItemStack[]{menu.m_38853_(0).m_7993_(), menu.m_38853_(1).m_7993_(), menu.m_38853_(2).m_7993_()});
            List recipes2 = RecipeChoices.matching((Level)level, RecipeType.f_44113_, input);
            if (index < recipes2.size()) {
                Polymorph.choose((Player)player, (Recipe)recipes2.get(index));
                menu.m_6199_(menu.m_38853_((int)0).f_40218_);
                menu.m_38946_();
            }
            return;
        }
        if (session.kind() != StationKind.TABLE) {
            return;
        }
        TableStore.Table table = TableStore.get(level).get(session.pos());
        if (table == null) {
            return;
        }
        TableInput input = new TableInput(TableGrid.oriented(table.items, session.side()));
        List recipes = RecipeChoices.matching((Level)level, RecipeType.f_44107_, input);
        if (index < recipes.size()) {
            Polymorph.choose((Player)player, (Recipe)recipes.get(index));
        }
    }

    private static final class Polymorph {
        private Polymorph() {
        }

        @Nullable
        static ResourceLocation chosen(Player player) {
            return PolymorphApi.common().getRecipeData(player).flatMap(data -> data.getSelectedRecipe()).map(Recipe::m_6423_).orElse(null);
        }

        static void choose(Player player, Recipe<?> recipe) {
            PolymorphApi.common().getRecipeData(player).ifPresent(data -> data.selectRecipe(recipe));
        }

        @Nullable
        static ResourceLocation chosen(BlockEntity blockEntity) {
            return PolymorphApi.common().getRecipeData(blockEntity).flatMap(data -> data.getSelectedRecipe()).map(Recipe::m_6423_).orElse(null);
        }

        static void choose(BlockEntity blockEntity, Recipe<?> recipe) {
            PolymorphApi.common().getRecipeData(blockEntity).ifPresent(data -> data.selectRecipe(recipe));
        }
    }
}


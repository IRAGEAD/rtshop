/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.InputConstants
 *  javax.annotation.Nullable
 *  mezz.jei.api.constants.VanillaTypes
 *  mezz.jei.api.gui.IRecipeLayoutDrawable
 *  mezz.jei.api.gui.ingredient.IRecipeSlotView
 *  mezz.jei.api.ingredients.IIngredientType
 *  mezz.jei.api.recipe.IFocus
 *  mezz.jei.api.recipe.IFocusGroup
 *  mezz.jei.api.recipe.IRecipeManager
 *  mezz.jei.api.recipe.RecipeIngredientRole
 *  mezz.jei.api.recipe.RecipeType
 *  mezz.jei.api.recipe.category.IRecipeCategory
 *  mezz.jei.api.runtime.IIngredientVisibility
 *  mezz.jei.api.runtime.IJeiRuntime
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.crafting.Recipe
 */
package dev.dyslexic.cinematic.client.compat.jei;

import com.mojang.blaze3d.platform.InputConstants;
import dev.dyslexic.cinematic.client.guide.RecipeViewer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.runtime.IIngredientVisibility;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

public final class JeiRecipeViewer
implements RecipeViewer {
    private static final List<ResourceLocation> SOURCE_TYPES = List.of(new ResourceLocation("jeresources", "worldgen"), new ResourceLocation("jeresources", "mob"), new ResourceLocation("jeresources", "plant"), new ResourceLocation("jeresources", "dungeon"), new ResourceLocation("jeresources", "villager"), new ResourceLocation("jeresources", "enchantment"));
    private static final ResourceLocation PROFESSIONS = new ResourceLocation("justenoughprofessions", "professions");
    private static final List<String> NOT_STATION_RECIPES = List.of("jeresources", "justenoughprofessions");
    private static final int MAX_OUTPUTS = 4000;
    @Nullable
    private static JeiRecipeViewer instance;
    private final IJeiRuntime jei;
    private final Map<Item, List<ItemStack>> outputs = new HashMap<Item, List<ItemStack>>();

    private JeiRecipeViewer(IJeiRuntime jei) {
        this.jei = jei;
    }

    static void available(@Nullable IJeiRuntime runtime) {
        instance = runtime == null ? null : new JeiRecipeViewer(runtime);
    }

    @Nullable
    public static RecipeViewer current() {
        return instance;
    }

    private IRecipeManager recipes() {
        return this.jei.getRecipeManager();
    }

    private IFocus<ItemStack> focus(RecipeIngredientRole role, ItemStack stack) {
        return this.jei.getJeiHelpers().getFocusFactory().createFocus(role, (IIngredientType)VanillaTypes.ITEM_STACK, (Object)stack);
    }

    private List<IRecipeCategory<?>> stationCategories(ItemStack station) {
        return this.recipes().createRecipeCategoryLookup().limitFocus(List.of(this.focus(RecipeIngredientRole.CATALYST, station))).get().filter(c -> !NOT_STATION_RECIPES.contains(c.getRecipeType().getUid().m_135827_())).toList();
    }

    @Override
    public List<ItemStack> search(String query, int limit) {
        String q = query.strip().toLowerCase(Locale.ROOT);
        String id = q.replace(' ', '_');
        ArrayList<ItemStack> found = new ArrayList<ItemStack>();
        if (q.isEmpty()) {
            return found;
        }
        IIngredientVisibility visibility = this.jei.getJeiHelpers().getIngredientVisibility();
        for (ItemStack stack : this.jei.getIngredientManager().getAllItemStacks()) {
            if (!visibility.isIngredientVisible((IIngredientType)VanillaTypes.ITEM_STACK, (Object)stack) || !stack.m_41786_().getString().toLowerCase(Locale.ROOT).contains(q) && !BuiltInRegistries.f_257033_.m_7981_((Object)stack.m_41720_()).toString().contains(id)) continue;
            found.add(stack);
            if (found.size() < limit) continue;
            break;
        }
        return found;
    }

    @Override
    public List<ItemStack> stationOutputs(ItemStack station) {
        return this.outputs.computeIfAbsent(station.m_41720_(), item -> {
            ArrayList<ItemStack> made = new ArrayList<ItemStack>();
            for (IRecipeCategory<?> category : this.stationCategories(station)) {
                this.collectOutputs(category, made);
            }
            made.sort(Comparator.comparing(s -> s.m_41786_().getString()));
            return List.copyOf(made);
        });
    }

    private <T> void collectOutputs(IRecipeCategory<T> category, List<ItemStack> made) {
        ClientLevel level = Minecraft.m_91087_().f_91073_;
        IFocusGroup none = this.jei.getJeiHelpers().getFocusFactory().getEmptyFocusGroup();
        this.recipes().createRecipeLookup(category.getRecipeType()).get().forEach(recipe -> {
            if (made.size() >= 4000) {
                return;
            }
            if (recipe instanceof Recipe) {
                Recipe vanilla = (Recipe)recipe;
                if (level != null) {
                    JeiRecipeViewer.addOutput(made, vanilla.m_8043_(level.m_9598_()));
                    return;
                }
            }
            this.recipes().createRecipeLayoutDrawable(category, recipe, none).ifPresent(layout -> {
                for (IRecipeSlotView slot : layout.getRecipeSlotsView().getSlotViews(RecipeIngredientRole.OUTPUT)) {
                    slot.getItemStacks().forEach(s -> JeiRecipeViewer.addOutput(made, s));
                }
            });
        });
    }

    private static void addOutput(List<ItemStack> made, ItemStack stack) {
        if (!stack.m_41619_() && made.stream().noneMatch(m -> ItemStack.m_150942_((ItemStack)m, (ItemStack)stack))) {
            made.add(stack.m_255036_(1));
        }
    }

    @Override
    public List<RecipeViewer.StationRecipe> stationRecipes(ItemStack station, ItemStack output) {
        IFocus<ItemStack> made = this.focus(RecipeIngredientRole.OUTPUT, output);
        ArrayList<RecipeViewer.StationRecipe> found = new ArrayList<RecipeViewer.StationRecipe>();
        for (IRecipeCategory<?> category : this.stationCategories(station)) {
            this.collectRecipes(category, made, found);
        }
        return found;
    }

    private <T> void collectRecipes(IRecipeCategory<T> category, IFocus<ItemStack> made, List<RecipeViewer.StationRecipe> found) {
        IFocusGroup focuses = this.jei.getJeiHelpers().getFocusFactory().createFocusGroup(List.of(made));
        this.recipes().createRecipeLookup(category.getRecipeType()).limitFocus(List.of(made)).get().forEach(recipe -> found.add(new JeiStationRecipe<Object>(category, recipe, made, focuses)));
    }

    @Override
    public void showHowToGet(ItemStack stack) {
        this.jei.getRecipesGui().show(this.focus(RecipeIngredientRole.OUTPUT, stack));
    }

    @Override
    public void showUses(ItemStack stack) {
        this.jei.getRecipesGui().show(this.focus(RecipeIngredientRole.INPUT, stack));
    }

    @Override
    public List<RecipeViewer.SourcePage> sources(ItemStack stack) {
        IFocus<ItemStack> made = this.focus(RecipeIngredientRole.OUTPUT, stack);
        ArrayList<RecipeViewer.SourcePage> pages = new ArrayList<RecipeViewer.SourcePage>();
        for (ResourceLocation id : SOURCE_TYPES) {
            this.recipes().getRecipeType(id).ifPresent(type -> this.sourcePage((RecipeType)type, made).ifPresent(pages::add));
        }
        return pages;
    }

    private <T> Optional<RecipeViewer.SourcePage> sourcePage(RecipeType<T> type, IFocus<ItemStack> made) {
        IRecipeCategory category = this.recipes().getRecipeCategory(type);
        List entries = this.recipes().createRecipeLookup(type).limitFocus(List.of(made)).get().toList();
        if (entries.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new RecipeViewer.SourcePage(category.getTitle(), entries.size(), () -> this.jei.getRecipesGui().showRecipes(category, entries, List.of(made)), (graphics, x, y) -> category.getIcon().draw(graphics, x, y)));
    }

    @Override
    @Nullable
    public Runnable professionPage(ItemStack station) {
        return this.recipes().getRecipeType(PROFESSIONS).map(type -> this.professionPage((RecipeType)type, station)).orElse(null);
    }

    @Nullable
    private <T> Runnable professionPage(RecipeType<T> type, ItemStack station) {
        IRecipeCategory category = this.recipes().getRecipeCategory(type);
        for (RecipeIngredientRole role : new RecipeIngredientRole[]{RecipeIngredientRole.OUTPUT, RecipeIngredientRole.INPUT, RecipeIngredientRole.CATALYST}) {
            IFocus<ItemStack> block = this.focus(role, station);
            List entries = this.recipes().createRecipeLookup(type).limitFocus(List.of(block)).get().toList();
            if (entries.isEmpty()) continue;
            return () -> this.jei.getRecipesGui().showRecipes(category, entries, List.of(block));
        }
        return null;
    }

    @Override
    public boolean isShowRecipeKey(int key, int scanCode) {
        return this.jei.getKeyMappings().getShowRecipe().isActiveAndMatches(InputConstants.m_84827_((int)key, (int)scanCode));
    }

    @Override
    public Component keyNames() {
        return Component.m_237119_().m_7220_(this.jei.getKeyMappings().getShowRecipe().getTranslatedKeyMessage()).m_130946_(" / ").m_7220_(this.jei.getKeyMappings().getShowUses().getTranslatedKeyMessage());
    }

    @Override
    public boolean isShowUsesKey(int key, int scanCode) {
        return this.jei.getKeyMappings().getShowUses().isActiveAndMatches(InputConstants.m_84827_((int)key, (int)scanCode));
    }

    private final class JeiStationRecipe<T>
    implements RecipeViewer.StationRecipe {
        private final IRecipeCategory<T> category;
        private final T recipe;
        private final IFocus<ItemStack> made;
        private final IFocusGroup focuses;
        @Nullable
        private IRecipeLayoutDrawable<T> layout;
        private boolean tried;

        JeiStationRecipe(IRecipeCategory<T> category, T recipe, IFocus<ItemStack> made, IFocusGroup focuses) {
            this.category = category;
            this.recipe = recipe;
            this.made = made;
            this.focuses = focuses;
        }

        @Nullable
        private IRecipeLayoutDrawable<T> layout() {
            if (!this.tried) {
                this.tried = true;
                this.layout = JeiRecipeViewer.this.recipes().createRecipeLayoutDrawable(this.category, this.recipe, this.focuses).orElse(null);
            }
            return this.layout;
        }

        @Override
        public Component category() {
            return this.category.getTitle();
        }

        @Override
        @Nullable
        public ResourceLocation recipeId() {
            ResourceLocation resourceLocation;
            T t = this.recipe;
            if (t instanceof Recipe) {
                Recipe vanilla = (Recipe)t;
                resourceLocation = vanilla.m_6423_();
            } else {
                resourceLocation = this.category.getRegistryName(this.recipe);
            }
            return resourceLocation;
        }

        @Override
        public List<ItemStack> inputs() {
            IRecipeLayoutDrawable<T> drawn = this.layout();
            if (drawn == null) {
                return List.of();
            }
            ArrayList<ItemStack> inputs = new ArrayList<ItemStack>();
            for (IRecipeSlotView slot : drawn.getRecipeSlotsView().getSlotViews(RecipeIngredientRole.INPUT)) {
                inputs.add(slot.getItemStacks().findFirst().orElse(ItemStack.f_41583_));
            }
            return inputs;
        }

        @Override
        public int width() {
            IRecipeLayoutDrawable<T> drawn = this.layout();
            return drawn == null ? this.category.getWidth() : drawn.getRect().m_110090_();
        }

        @Override
        public int height() {
            IRecipeLayoutDrawable<T> drawn = this.layout();
            return drawn == null ? this.category.getHeight() : drawn.getRect().m_110091_();
        }

        @Override
        public void draw(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
            IRecipeLayoutDrawable<T> drawn = this.layout();
            if (drawn == null) {
                return;
            }
            drawn.setPosition(x, y);
            drawn.drawRecipe(graphics, mouseX, mouseY);
        }

        @Override
        public void open() {
            JeiRecipeViewer.this.jei.getRecipesGui().showRecipes(this.category, List.of(this.recipe), List.of(this.made));
        }
    }
}


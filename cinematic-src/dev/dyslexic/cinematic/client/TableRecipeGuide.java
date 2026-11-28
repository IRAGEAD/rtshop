/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.client.Minecraft
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.crafting.CraftingRecipe
 *  net.minecraft.world.item.crafting.Ingredient
 *  net.minecraft.world.item.crafting.RecipeManager
 *  net.minecraft.world.item.crafting.RecipeType
 *  net.minecraft.world.item.crafting.ShapedRecipe
 *  net.minecraft.world.item.crafting.ShapelessRecipe
 */
package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.RecipeRef;
import dev.dyslexic.cinematic.client.ClientTables;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.TableStation;
import dev.dyslexic.cinematic.table.TableGrid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;

final class TableRecipeGuide {
    final List<Entry> entries = new ArrayList<Entry>();
    @Nullable
    Entry selected;
    final Ingredient[] grid = new Ingredient[9];
    final ItemStack[] display = new ItemStack[9];
    final boolean[] placed = new boolean[9];
    final boolean[] missing = new boolean[9];
    int missingCount;
    int revision;
    private final TableStation station;
    private List<ItemStack> snapshot = List.of();
    private boolean indexed;
    private RecipeManager indexedManager;
    @Nullable
    private List<int[]> movesCache;
    private int movesRevision = -1;
    @Nullable
    private List<int[]> takeCache;
    private int takeRevision = -1;
    private int stuckCount;
    @Nullable
    private MaxLayout maxCache;
    private int maxRevision = -1;
    private static final int SEARCH_STEPS = 20000;

    TableRecipeGuide(TableStation station) {
        this.station = station;
        Arrays.fill(this.grid, Ingredient.f_43901_);
        Arrays.fill(this.display, ItemStack.f_41583_);
    }

    void tick() {
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91073_ == null || mc.f_91074_ == null) {
            return;
        }
        RecipeManager manager = mc.f_91073_.m_7465_();
        if (!this.indexed || this.indexedManager != manager || this.entries.isEmpty() && !manager.m_44013_(RecipeType.f_44107_).isEmpty()) {
            ResourceLocation selectedId = this.selected == null ? null : this.selected.holder.id();
            this.entries.clear();
            this.selected = null;
            for (CraftingRecipe recipe : manager.m_44013_(RecipeType.f_44107_)) {
                ItemStack result;
                RecipeRef<CraftingRecipe> holder = RecipeRef.of(recipe);
                if (recipe.m_5598_() || !(recipe instanceof ShapedRecipe) && !(recipe instanceof ShapelessRecipe) || !recipe.m_8004_(3, 3) || recipe.m_142505_() || (result = recipe.m_8043_(mc.f_91073_.m_9598_())).m_41619_()) continue;
                this.entries.add(new Entry(holder, result.m_41777_()));
            }
            this.entries.sort(Comparator.comparing(e -> e.result.m_41786_().getString()).thenComparing(e -> e.holder.id().toString()));
            if (selectedId != null) {
                this.selected = this.entry(selectedId);
            }
            this.snapshot = List.of();
            this.indexed = true;
            this.indexedManager = manager;
        }
        ArrayList<ItemStack> next = new ArrayList<ItemStack>();
        for (int i2 = 0; i2 < 36; ++i2) {
            next.add(mc.f_91074_.m_150109_().m_8020_(i2).m_41777_());
        }
        next.add(mc.f_91074_.f_36096_.m_142621_().m_41777_());
        ClientTables.Entry table = ClientTables.get(this.station.pos);
        for (int i3 = 0; i3 < 9; ++i3) {
            next.add(table == null ? ItemStack.f_41583_ : table.items[i3].m_41777_());
        }
        boolean changed = next.size() != this.snapshot.size();
        for (int i4 = 0; !changed && i4 < next.size(); ++i4) {
            changed = !ItemStack.m_41728_((ItemStack)((ItemStack)next.get(i4)), (ItemStack)this.snapshot.get(i4));
        }
        if (!changed) {
            return;
        }
        this.snapshot = next;
        List<ItemStack> pool = TableRecipeGuide.units(next);
        for (Entry entry : this.entries) {
            int[] assignment = TableRecipeGuide.match(entry.ingredients, pool);
            entry.craftable = Arrays.stream(assignment).allMatch(i -> i >= 0);
        }
        this.refresh();
        ++this.revision;
    }

    @Nullable
    Entry entry(ResourceLocation id) {
        for (Entry e : this.entries) {
            if (!e.holder.id().equals((Object)id)) continue;
            return e;
        }
        return null;
    }

    void select(@Nullable Entry entry) {
        this.selected = entry;
        Arrays.fill(this.grid, Ingredient.f_43901_);
        this.refresh();
        ++this.revision;
    }

    private ItemStack actual(int logical) {
        ClientTables.Entry table = ClientTables.get(this.station.pos);
        return table == null ? ItemStack.f_41583_ : table.items[TableGrid.worldIndex(this.station.side, logical / 3, logical % 3)];
    }

    private void refresh() {
        Arrays.fill(this.display, ItemStack.f_41583_);
        Arrays.fill(this.placed, false);
        Arrays.fill(this.missing, false);
        this.missingCount = 0;
        if (this.selected == null) {
            return;
        }
        this.layout();
        ArrayList<ItemStack> resources = new ArrayList<ItemStack>();
        for (int i = 0; i < Math.min(37, this.snapshot.size()); ++i) {
            resources.add(this.snapshot.get(i).m_41777_());
        }
        ArrayList<Ingredient> needed = new ArrayList<Ingredient>();
        ArrayList<Integer> cells = new ArrayList<Integer>();
        for (int i = 0; i < 9; ++i) {
            ItemStack stack = this.actual(i).m_41777_();
            boolean bl = this.placed[i] = !this.grid[i].m_43947_() && this.grid[i].test(stack);
            if (this.placed[i]) {
                stack.m_41774_(1);
            }
            resources.add(stack);
            if (this.grid[i].m_43947_() || this.placed[i]) continue;
            needed.add(this.grid[i]);
            cells.add(i);
        }
        List<ItemStack> pool = TableRecipeGuide.units(resources);
        int[] assignment = TableRecipeGuide.match(needed, pool);
        for (int n = 0; n < cells.size(); ++n) {
            int cell = (Integer)cells.get(n);
            boolean bl = this.missing[cell] = assignment[n] < 0;
            if (this.missing[cell]) {
                ++this.missingCount;
            }
            if (this.missing[cell]) continue;
            this.display[cell] = pool.get(assignment[n]).m_255036_(1);
        }
    }

    private void layout() {
        CraftingRecipe craftingRecipe = this.selected.holder.value();
        if (craftingRecipe instanceof ShapedRecipe) {
            ShapedRecipe shaped = (ShapedRecipe)craftingRecipe;
            int w = shaped.m_44220_();
            int h = shaped.m_44221_();
            Ingredient[] best = this.shapedLayout(shaped, w == 1 ? 1 : 0, h == 1 ? 1 : 0, false);
            int score = this.score(best);
            if (!this.gridEmpty() && this.score(this.grid) >= score) {
                best = (Ingredient[])this.grid.clone();
                score = this.score(best);
            }
            for (int y = 0; y <= 3 - h; ++y) {
                for (int x = 0; x <= 3 - w; ++x) {
                    for (int m = 0; m < 2; ++m) {
                        Ingredient[] candidate = this.shapedLayout(shaped, x, y, m == 1);
                        int value = this.score(candidate);
                        if (value <= score) continue;
                        best = candidate;
                        score = value;
                    }
                }
            }
            System.arraycopy(best, 0, this.grid, 0, 9);
        } else {
            int i;
            Arrays.fill(this.grid, Ingredient.f_43901_);
            ArrayList<ItemStack> table = new ArrayList<ItemStack>();
            for (int i2 = 0; i2 < 9; ++i2) {
                table.add(this.actual(i2));
            }
            int[] assignment = TableRecipeGuide.match(this.selected.ingredients, table);
            for (i = 0; i < assignment.length; ++i) {
                if (assignment[i] < 0) continue;
                this.grid[assignment[i]] = this.selected.ingredients.get(i);
            }
            for (i = 0; i < assignment.length; ++i) {
                int j;
                if (assignment[i] >= 0) continue;
                int cell = -1;
                for (j = 0; j < 9; ++j) {
                    if (!this.grid[j].m_43947_() || !this.actual(j).m_41619_()) continue;
                    cell = j;
                    break;
                }
                if (cell < 0) {
                    for (j = 0; j < 9; ++j) {
                        if (!this.grid[j].m_43947_()) continue;
                        cell = j;
                        break;
                    }
                }
                this.grid[cell] = this.selected.ingredients.get(i);
            }
        }
    }

    private boolean gridEmpty() {
        return Arrays.stream(this.grid).allMatch(Ingredient::m_43947_);
    }

    private Ingredient[] shapedLayout(ShapedRecipe recipe, int x, int y, boolean mirror) {
        Object[] result = new Ingredient[9];
        Arrays.fill(result, Ingredient.f_43901_);
        for (int row = 0; row < recipe.m_44221_(); ++row) {
            for (int col = 0; col < recipe.m_44220_(); ++col) {
                result[(row + y) * 3 + col + x] = (Ingredient)recipe.m_7527_().get(row * recipe.m_44220_() + (mirror ? recipe.m_44220_() - col - 1 : col));
            }
        }
        return result;
    }

    private int score(Ingredient[] candidate) {
        int score = 0;
        for (int i = 0; i < 9; ++i) {
            if (this.actual(i).m_41619_()) continue;
            score += candidate[i].test(this.actual(i)) ? 4 : -1;
        }
        return score;
    }

    List<int[]> layoutMoves() {
        if (this.movesRevision == this.revision && this.movesCache != null) {
            return this.movesCache;
        }
        this.movesRevision = this.revision;
        this.movesCache = this.workOutMoves();
        return this.movesCache;
    }

    private List<int[]> workOutMoves() {
        Minecraft mc = Minecraft.m_91087_();
        ArrayList<int[]> moves = new ArrayList<int[]>();
        if (this.selected == null || mc.f_91074_ == null) {
            return moves;
        }
        ArrayList<ItemStack> units = new ArrayList<ItemStack>();
        ArrayList<Integer> slots = new ArrayList<Integer>();
        for (int slot = -1; slot < 36; ++slot) {
            ItemStack stack = slot < 0 ? mc.f_91074_.f_36096_.m_142621_() : mc.f_91074_.m_150109_().m_8020_(slot);
            for (int i = 0; i < Math.min(9, stack.m_41613_()); ++i) {
                units.add(stack);
                slots.add(slot);
            }
        }
        ArrayList<Ingredient> needed = new ArrayList<Ingredient>();
        ArrayList<Integer> cells = new ArrayList<Integer>();
        for (int logical = 0; logical < 9; ++logical) {
            if (this.grid[logical].m_43947_() || this.placed[logical] || !this.actual(logical).m_41619_()) continue;
            needed.add(this.grid[logical]);
            cells.add(logical);
        }
        int[] assignment = TableRecipeGuide.match(needed, units);
        for (int n = 0; n < cells.size(); ++n) {
            if (assignment[n] < 0) continue;
            int logical = (Integer)cells.get(n);
            moves.add(new int[]{TableGrid.worldIndex(this.station.side, logical / 3, logical % 3), (Integer)slots.get(assignment[n]), 1});
        }
        return moves;
    }

    List<int[]> takeBackMoves() {
        if (this.takeRevision == this.revision && this.takeCache != null) {
            return this.takeCache;
        }
        this.takeRevision = this.revision;
        this.takeCache = new ArrayList<int[]>();
        this.stuckCount = 0;
        Minecraft mc = Minecraft.m_91087_();
        if (this.selected == null || mc.f_91074_ == null) {
            return this.takeCache;
        }
        ItemStack[] room = new ItemStack[36];
        for (int slot = 0; slot < 36; ++slot) {
            room[slot] = mc.f_91074_.m_150109_().m_8020_(slot).m_41777_();
        }
        for (int logical = 0; logical < 9; ++logical) {
            if (!this.wrong(logical)) continue;
            ItemStack lying = this.actual(logical);
            if (TableRecipeGuide.fits(room, lying)) {
                this.takeCache.add(new int[]{TableGrid.worldIndex(this.station.side, logical / 3, logical % 3), -2, lying.m_41613_()});
                continue;
            }
            ++this.stuckCount;
        }
        return this.takeCache;
    }

    int stuck() {
        this.takeBackMoves();
        return this.stuckCount;
    }

    private static boolean fits(ItemStack[] room, ItemStack stack) {
        int left = stack.m_41613_();
        for (ItemStack slot : room) {
            if (slot.m_41619_() || !ItemStack.m_150942_((ItemStack)slot, (ItemStack)stack)) continue;
            left -= Math.max(0, slot.m_41741_() - slot.m_41613_());
        }
        for (ItemStack slot : room) {
            if (left <= 0 || !slot.m_41619_()) continue;
            left -= stack.m_41741_();
        }
        if (left > 0) {
            return false;
        }
        int put = stack.m_41613_();
        for (ItemStack slot : room) {
            int move;
            if (put <= 0 || slot.m_41619_() || !ItemStack.m_150942_((ItemStack)slot, (ItemStack)stack) || (move = Math.min(put, slot.m_41741_() - slot.m_41613_())) <= 0) continue;
            slot.m_41769_(move);
            put -= move;
        }
        for (int i = 0; i < room.length && put > 0; ++i) {
            if (!room[i].m_41619_()) continue;
            int move = Math.min(put, stack.m_41741_());
            room[i] = stack.m_255036_(move);
            put -= move;
        }
        return true;
    }

    int layableAfterTakingBack() {
        Minecraft mc = Minecraft.m_91087_();
        if (this.selected == null || mc.f_91074_ == null) {
            return 0;
        }
        ArrayList<ItemStack> stacks = new ArrayList<ItemStack>();
        for (int slot = -1; slot < 36; ++slot) {
            stacks.add(slot < 0 ? mc.f_91074_.f_36096_.m_142621_() : mc.f_91074_.m_150109_().m_8020_(slot));
        }
        ArrayList<Integer> cleared = new ArrayList<Integer>();
        for (int[] move : this.takeBackMoves()) {
            int logical = this.logicalOf(move[0]);
            cleared.add(logical);
            stacks.add(this.actual(logical));
        }
        ArrayList<Ingredient> needed = new ArrayList<Ingredient>();
        for (int logical = 0; logical < 9; ++logical) {
            if (this.grid[logical].m_43947_() || this.placed[logical] || !this.actual(logical).m_41619_() && !cleared.contains(logical)) continue;
            needed.add(this.grid[logical]);
        }
        return (int)Arrays.stream(TableRecipeGuide.match(needed, TableRecipeGuide.units(stacks))).filter(i -> i >= 0).count();
    }

    private int logicalOf(int world) {
        for (int logical = 0; logical < 9; ++logical) {
            if (TableGrid.worldIndex(this.station.side, logical / 3, logical % 3) != world) continue;
            return logical;
        }
        return -1;
    }

    MaxLayout maxLayout() {
        if (this.maxRevision == this.revision && this.maxCache != null) {
            return this.maxCache;
        }
        this.maxRevision = this.revision;
        this.maxCache = this.workOutMax();
        return this.maxCache;
    }

    private MaxLayout workOutMax() {
        Minecraft mc = Minecraft.m_91087_();
        if (this.selected == null || mc.f_91074_ == null) {
            return new MaxLayout(0, 0, List.of());
        }
        ArrayList<ItemStack> kinds = new ArrayList<ItemStack>();
        ArrayList<Integer> stock = new ArrayList<Integer>();
        ArrayList kindSlots = new ArrayList();
        for (int slot = -1; slot < 36; ++slot) {
            Object stack;
            Object object = stack = slot < 0 ? mc.f_91074_.f_36096_.m_142621_() : mc.f_91074_.m_150109_().m_8020_(slot);
            if (stack.m_41619_()) continue;
            int kind = TableRecipeGuide.kindOf(kinds, (ItemStack)stack);
            if (kind < 0) {
                kinds.add((ItemStack)stack);
                stock.add(0);
                kindSlots.add(new ArrayList());
                kind = kinds.size() - 1;
            }
            stock.set(kind, (Integer)stock.get(kind) + stack.m_41613_());
            ((List)kindSlots.get(kind)).add(slot);
        }
        ArrayList<Integer> takenBack = new ArrayList<Integer>();
        for (int[] move : this.takeBackMoves()) {
            int logical = this.logicalOf(move[0]);
            takenBack.add(logical);
            ItemStack stack = this.actual(logical);
            int kind = TableRecipeGuide.kindOf(kinds, stack);
            if (kind < 0) {
                kinds.add(stack);
                stock.add(0);
                kindSlots.add(new ArrayList());
                kind = kinds.size() - 1;
            }
            stock.set(kind, (Integer)stock.get(kind) + stack.m_41613_());
            if (((List)kindSlots.get(kind)).contains(-3)) continue;
            ((List)kindSlots.get(kind)).add(-3);
        }
        ArrayList<Integer> cells = new ArrayList<Integer>();
        ArrayList<Integer> fixedKind = new ArrayList<Integer>();
        ArrayList<Integer> have = new ArrayList<Integer>();
        ArrayList<Integer> limit = new ArrayList<Integer>();
        ArrayList<int[]> choices = new ArrayList<int[]>();
        int current = Integer.MAX_VALUE;
        int upper = Integer.MAX_VALUE;
        for (int logical = 0; logical < 9; ++logical) {
            ItemStack lying;
            if (this.grid[logical].m_43947_()) continue;
            ItemStack itemStack = lying = takenBack.contains(logical) ? ItemStack.f_41583_ : this.actual(logical);
            if (!lying.m_41619_() && !this.grid[logical].test(lying)) continue;
            cells.add(logical);
            if (!lying.m_41619_()) {
                fixedKind.add(TableRecipeGuide.kindOf(kinds, lying));
                have.add(lying.m_41613_());
                limit.add(lying.m_41741_());
                choices.add(new int[0]);
                current = Math.min(current, lying.m_41613_());
                upper = Math.min(upper, lying.m_41741_());
                continue;
            }
            ArrayList<Integer> accepted = new ArrayList<Integer>();
            int best = 0;
            for (int k = 0; k < kinds.size(); ++k) {
                if (!this.grid[logical].test((ItemStack)kinds.get(k))) continue;
                accepted.add(k);
                best = Math.max(best, ((ItemStack)kinds.get(k)).m_41741_());
            }
            fixedKind.add(-1);
            have.add(0);
            limit.add(best);
            choices.add(accepted.stream().mapToInt(Integer::intValue).toArray());
            current = 0;
            upper = Math.min(upper, best);
        }
        if (cells.isEmpty()) {
            return new MaxLayout(0, 0, List.of());
        }
        int[] chosen = new int[cells.size()];
        for (int sets = upper; sets > current; --sets) {
            if (!TableRecipeGuide.assign(sets, cells.size(), fixedKind, have, limit, choices, kinds, stock, chosen)) continue;
            int[] left = new int[37];
            for (int slot = -1; slot < 36; ++slot) {
                ItemStack stack = slot < 0 ? mc.f_91074_.f_36096_.m_142621_() : mc.f_91074_.m_150109_().m_8020_(slot);
                left[slot + 1] = stack.m_41613_();
            }
            ArrayList<int[]> moves = new ArrayList<int[]>();
            block6: for (int i = 0; i < cells.size(); ++i) {
                int need = sets - (Integer)have.get(i);
                if (need <= 0) continue;
                int logical = (Integer)cells.get(i);
                int world = TableGrid.worldIndex(this.station.side, logical / 3, logical % 3);
                Iterator iterator = ((List)kindSlots.get(chosen[i])).iterator();
                while (iterator.hasNext()) {
                    int slot = (Integer)iterator.next();
                    if (need <= 0) continue block6;
                    if (slot == -3) {
                        moves.add(new int[]{world, slot, need});
                        need = 0;
                        continue block6;
                    }
                    int take = Math.min(need, left[slot + 1]);
                    if (take <= 0) continue;
                    int n = slot + 1;
                    left[n] = left[n] - take;
                    need -= take;
                    moves.add(new int[]{world, slot, take});
                }
            }
            return new MaxLayout(sets, current, moves);
        }
        return new MaxLayout(current, current, List.of());
    }

    private static int kindOf(List<ItemStack> kinds, ItemStack stack) {
        for (int k = 0; k < kinds.size(); ++k) {
            if (!ItemStack.m_150942_((ItemStack)kinds.get(k), (ItemStack)stack)) continue;
            return k;
        }
        return -1;
    }

    private static boolean assign(int sets, int count, List<Integer> fixedKind, List<Integer> have, List<Integer> limit, List<int[]> choices, List<ItemStack> kinds, List<Integer> stock, int[] chosen) {
        int[] left = stock.stream().mapToInt(Integer::intValue).toArray();
        ArrayList<Integer> open = new ArrayList<Integer>();
        for (int i2 = 0; i2 < count; ++i2) {
            int need = sets - have.get(i2);
            if (fixedKind.get(i2) >= 0 || have.get(i2) > 0) {
                if (need <= 0) {
                    chosen[i2] = Math.max(0, fixedKind.get(i2));
                    continue;
                }
                int kind = fixedKind.get(i2);
                if (kind < 0 || sets > limit.get(i2) || left[kind] < need) {
                    return false;
                }
                int n = kind;
                left[n] = left[n] - need;
                chosen[i2] = kind;
                continue;
            }
            if (choices.get(i2).length == 0) {
                return false;
            }
            open.add(i2);
        }
        open.sort(Comparator.comparingInt(i -> ((int[])choices.get((int)i)).length));
        int[] steps = new int[]{0};
        return TableRecipeGuide.place(0, open, sets, choices, kinds, left, chosen, steps);
    }

    private static boolean place(int at, List<Integer> open, int sets, List<int[]> choices, List<ItemStack> kinds, int[] left, int[] chosen, int[] steps) {
        Integer[] order;
        if (at == open.size()) {
            return true;
        }
        steps[0] = steps[0] + 1;
        if (steps[0] > 20000) {
            return false;
        }
        int cell = open.get(at);
        int[] options = (int[])choices.get(cell).clone();
        Integer[] integerArray = order = (Integer[])Arrays.stream(options).boxed().sorted((a, b) -> left[b] - left[a]).toArray(Integer[]::new);
        int n = integerArray.length;
        for (int i = 0; i < n; ++i) {
            int kind = integerArray[i];
            if (left[kind] < sets || kinds.get(kind).m_41741_() < sets) continue;
            int n2 = kind;
            left[n2] = left[n2] - sets;
            chosen[cell] = kind;
            if (TableRecipeGuide.place(at + 1, open, sets, choices, kinds, left, chosen, steps)) {
                return true;
            }
            int n3 = kind;
            left[n3] = left[n3] + sets;
        }
        return false;
    }

    ItemStack ghost(int logical) {
        if (!this.display[logical].m_41619_()) {
            return this.display[logical];
        }
        ItemStack[] choices = this.grid[logical].m_43908_();
        return choices.length == 0 ? ItemStack.f_41583_ : choices[(int)(Scene.clock() / 1.8) % choices.length].m_255036_(1);
    }

    boolean wrong(int logical) {
        return !this.actual(logical).m_41619_() && !this.placed[logical];
    }

    private static List<ItemStack> units(List<ItemStack> stacks) {
        ArrayList<ItemStack> result = new ArrayList<ItemStack>();
        for (ItemStack stack : stacks) {
            for (int i = 0; i < Math.min(9, stack.m_41613_()); ++i) {
                result.add(stack);
            }
        }
        return result;
    }

    static int[] match(List<Ingredient> ingredients, List<ItemStack> units) {
        int[] owner = new int[units.size()];
        Arrays.fill(owner, -1);
        for (int i = 0; i < ingredients.size(); ++i) {
            TableRecipeGuide.augment(i, ingredients, units, owner, new boolean[units.size()]);
        }
        int[] result = new int[ingredients.size()];
        Arrays.fill(result, -1);
        for (int i = 0; i < owner.length; ++i) {
            if (owner[i] < 0) continue;
            result[owner[i]] = i;
        }
        return result;
    }

    private static boolean augment(int ingredient, List<Ingredient> ingredients, List<ItemStack> units, int[] owner, boolean[] seen) {
        for (int i = 0; i < units.size(); ++i) {
            if (seen[i] || !ingredients.get(ingredient).test(units.get(i))) continue;
            seen[i] = true;
            if (owner[i] >= 0 && !TableRecipeGuide.augment(owner[i], ingredients, units, owner, seen)) continue;
            owner[i] = ingredient;
            return true;
        }
        return false;
    }

    static final class Entry {
        final RecipeRef<CraftingRecipe> holder;
        final ItemStack result;
        final List<Ingredient> ingredients;
        final String search;
        boolean craftable;

        Entry(RecipeRef<CraftingRecipe> holder, ItemStack result) {
            this.holder = holder;
            this.result = result;
            this.ingredients = holder.value().m_7527_().stream().filter(i -> !i.m_43947_()).toList();
            this.search = (result.m_41786_().getString() + " " + String.valueOf(holder.id())).toLowerCase(Locale.ROOT);
        }
    }

    record MaxLayout(int sets, int current, List<int[]> moves) {
    }
}


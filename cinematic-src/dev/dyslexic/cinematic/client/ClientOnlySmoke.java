/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.screens.ConnectScreen
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.gui.screens.TitleScreen
 *  net.minecraft.client.multiplayer.ServerData
 *  net.minecraft.client.multiplayer.resolver.ServerAddress
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.inventory.AbstractFurnaceMenu
 *  net.minecraft.world.inventory.AnvilMenu
 *  net.minecraft.world.inventory.ClickType
 *  net.minecraft.world.inventory.CraftingMenu
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.fml.util.ObfuscationReflectionHelper
 */
package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.Cinematic;
import dev.dyslexic.cinematic.client.ClientOnly;
import dev.dyslexic.cinematic.client.Picking;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.SceneScreen;
import dev.dyslexic.cinematic.client.Settings;
import dev.dyslexic.cinematic.client.TableRecipeGuide;
import dev.dyslexic.cinematic.client.TableStation;
import dev.dyslexic.cinematic.client.guide.RecipeViewers;
import dev.dyslexic.cinematic.station.StationKind;
import dev.dyslexic.cinematic.table.TableGrid;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

public final class ClientOnlySmoke {
    private static final boolean ENABLED = Boolean.getBoolean("cinematic.clientOnlySmoke");
    private static final BlockPos TABLE = new BlockPos(0, 64, 1);
    private static final BlockPos FURNACE = new BlockPos(1, 64, 1);
    private static final BlockPos ANVIL = new BlockPos(1, 64, 3);
    private static int stage;
    private static int ticks;
    private static Vec3 original;
    private static int dirtSlot;

    private ClientOnlySmoke() {
    }

    public static void onScreen(Screen screen) {
        if (!ENABLED || stage != 0 || !(screen instanceof TitleScreen)) {
            return;
        }
        stage = 1;
        Settings.set(Settings.CLIENT_ONLY_MODE, true);
        Minecraft mc = Minecraft.m_91087_();
        mc.execute(() -> ConnectScreen.m_278792_((Screen)screen, (Minecraft)mc, (ServerAddress)ServerAddress.m_171864_((String)"127.0.0.1:25586"), (ServerData)new ServerData("Client-only smoke", "127.0.0.1:25586", false), (boolean)false));
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public static void tick() {
        if (!ENABLED || stage == 0 || stage == 99 || stage == -1) {
            return;
        }
        Minecraft mc = Minecraft.m_91087_();
        if (++ticks > 600) {
            AbstractContainerMenu abstractContainerMenu;
            StringBuilder grid = new StringBuilder();
            if (mc.f_91074_ != null && (abstractContainerMenu = mc.f_91074_.f_36096_) instanceof CraftingMenu) {
                CraftingMenu menu = (CraftingMenu)abstractContainerMenu;
                for (int slot = 0; slot <= 9; ++slot) {
                    grid.append(' ').append(slot).append('=').append(menu.m_38853_(slot).m_7993_());
                }
                grid.append(" carried=").append(menu.m_142621_());
            }
            ClientOnlySmoke.fail("timed out at stage " + stage + String.valueOf(grid));
            return;
        }
        if (mc.f_91074_ == null || mc.f_91073_ == null || mc.f_91072_ == null) {
            return;
        }
        if (ClientOnly.serverHasMod()) {
            ClientOnlySmoke.fail("the test server unexpectedly has Cinematic Interactions");
            return;
        }
        switch (stage) {
            case 1: {
                if (!StationKind.TABLE.matches(mc.f_91073_.m_8055_(TABLE)) || mc.f_91074_.m_20238_(TABLE.m_252807_()) > 9.0 || Math.abs(mc.f_91074_.m_20185_() - 0.9) > 0.05 || mc.f_91074_.m_150109_().m_8020_(0).m_41720_() != Items.f_41837_) {
                    return;
                }
                original = mc.f_91074_.m_20182_();
                BlockHitResult hit = new BlockHitResult(new Vec3(0.5, 65.0, 1.5), Direction.UP, TABLE, false);
                ClientOnlySmoke.clickAsPlayer(mc, hit);
                if (mc.f_91074_.f_20911_) {
                    ClientOnlySmoke.fail("vanilla hand swung before the client-only scene");
                    return;
                }
                Cinematic.LOGGER.info("[ClientOnlySmoke] used table at {} without an initial swing", (Object)original);
                stage = 2;
                ticks = 0;
                return;
            }
            case 2: {
                if (ticks == 40 && mc.f_91074_.f_36096_ == mc.f_91074_.f_36095_ && !Scene.active()) {
                    ClientOnlySmoke.clickAsPlayer(mc, new BlockHitResult(new Vec3(0.5, 65.0, 1.5), Direction.UP, TABLE, false));
                    Cinematic.LOGGER.info("[ClientOnlySmoke] retried table use after spawn settled");
                }
                if (ticks == 80) {
                    Cinematic.LOGGER.info("[ClientOnlySmoke] waiting: screen={}, menu={}, scene={}, refused={}", new Object[]{mc.f_91080_ == null ? "none" : mc.f_91080_.getClass().getSimpleName(), mc.f_91074_.f_36096_.getClass().getSimpleName(), Scene.phase, Scene.refused});
                }
                if (!(mc.f_91074_.f_36096_ instanceof CraftingMenu) || !Scene.clientOnly()) {
                    return;
                }
                if (Scene.phase != Scene.Phase.ACTIVE) {
                    return;
                }
                if (mc.f_91074_.m_20182_().m_82557_(original) > 0.01) {
                    ClientOnlySmoke.fail("the player moved during the scene");
                    return;
                }
                CraftingMenu menu = (CraftingMenu)mc.f_91074_.f_36096_;
                mc.f_91072_.m_171799_(menu.f_38840_, 37, 0, ClickType.PICKUP, (Player)mc.f_91074_);
                TableStation table = Scene.station(TableStation.class);
                table.hoverCell = 4;
                table.click(1, false);
                mc.f_91072_.m_171799_(menu.f_38840_, 37, 0, ClickType.PICKUP, (Player)mc.f_91074_);
                stage = 3;
                ticks = 0;
                return;
            }
            case 3: {
                AbstractContainerMenu table = mc.f_91074_.f_36096_;
                if (!(table instanceof CraftingMenu)) {
                    ClientOnlySmoke.fail("the vanilla crafting menu closed before the recipe synced");
                    return;
                }
                CraftingMenu menu = (CraftingMenu)table;
                if (menu.m_38853_(0).m_7993_().m_41720_() != Items.f_42647_) {
                    return;
                }
                Scene.station(TableStation.class).craft(false);
                stage = 4;
                ticks = 0;
                return;
            }
            case 4: {
                AbstractContainerMenu table = mc.f_91074_.f_36096_;
                if (!(table instanceof CraftingMenu)) {
                    ClientOnlySmoke.fail("the vanilla crafting menu closed before taking the result");
                    return;
                }
                CraftingMenu menu = (CraftingMenu)table;
                if (menu.m_142621_().m_41720_() != Items.f_42647_) {
                    return;
                }
                stage = 40;
                ticks = 0;
                return;
            }
            case 40: {
                ClientOnlySmoke.pointAt(mc, Scene.station(TableStation.class).cellPoint(0));
                stage = 41;
                return;
            }
            case 41: {
                TableStation table = Scene.station(TableStation.class);
                if (table.hoverCell != 0 || !(mc.f_91080_ instanceof SceneScreen)) {
                    return;
                }
                double[] at = Picking.mouseGui();
                mc.f_91080_.m_6375_(at[0], at[1], 0);
                ClientOnlySmoke.pointAt(mc, table.cellPoint(8));
                stage = 42;
                return;
            }
            case 42: {
                TableStation table = Scene.station(TableStation.class);
                double[] at = Picking.mouseGui();
                mc.f_91080_.m_7979_(at[0], at[1], 0, 1.0, 0.0);
                if (Integer.bitCount(table.spreadCells()) != 2) {
                    ClientOnlySmoke.fail("the left-drag did not gather two cells: " + Integer.bitCount(table.spreadCells()));
                    return;
                }
                mc.f_91080_.m_6348_(at[0], at[1], 0);
                stage = 43;
                ticks = 0;
                return;
            }
            case 43: {
                AbstractContainerMenu at = mc.f_91074_.f_36096_;
                if (!(at instanceof CraftingMenu)) {
                    ClientOnlySmoke.fail("the vanilla crafting menu closed during the left-drag");
                    return;
                }
                CraftingMenu menu = (CraftingMenu)at;
                Direction side = Scene.station.sideDirection();
                int a = ClientOnlySmoke.menuSlot(side, 0);
                int b = ClientOnlySmoke.menuSlot(side, 8);
                if (menu.m_38853_(a).m_7993_().m_41613_() != 2 || menu.m_38853_(b).m_7993_().m_41613_() != 2 || !menu.m_142621_().m_41619_()) {
                    return;
                }
                Cinematic.LOGGER.info("[ClientOnlySmoke] left-drag shared 4 planks as 2 + 2 through vanilla quick-craft clicks");
                mc.f_91072_.m_171799_(menu.f_38840_, a, 0, ClickType.QUICK_MOVE, (Player)mc.f_91074_);
                mc.f_91072_.m_171799_(menu.f_38840_, b, 0, ClickType.QUICK_MOVE, (Player)mc.f_91074_);
                stage = 44;
                ticks = 0;
                return;
            }
            case 44: {
                AbstractContainerMenu side = mc.f_91074_.f_36096_;
                if (!(side instanceof CraftingMenu)) {
                    return;
                }
                CraftingMenu menu = (CraftingMenu)side;
                for (int slot = 1; slot <= 9; ++slot) {
                    if (menu.m_38853_(slot).m_7993_().m_41619_()) continue;
                    return;
                }
                TableStation table = Scene.station(TableStation.class);
                if (table.busy()) {
                    return;
                }
                TableRecipeGuide guide = table.recipeBook.guide;
                guide.tick();
                Optional<TableRecipeGuide.Entry> stick = guide.entries.stream().filter(e -> e.holder.id().m_135815_().equals("stick")).findFirst();
                if (stick.isEmpty()) {
                    return;
                }
                guide.select(stick.get());
                guide.tick();
                if (guide.maxLayout().sets() != 2) {
                    if (ticks <= 40) return;
                    ClientOnlySmoke.fail("Shift + Lay out offered " + guide.maxLayout().sets() + " stick sets instead of 2");
                    return;
                }
                table.recipeBook.layOut(true);
                if (!table.busy()) {
                    ClientOnlySmoke.fail("Shift + Lay out did not start laying");
                    return;
                }
                stage = 45;
                ticks = 0;
                return;
            }
            case 45: {
                AbstractContainerMenu table = mc.f_91074_.f_36096_;
                if (!(table instanceof CraftingMenu)) {
                    return;
                }
                CraftingMenu menu = (CraftingMenu)table;
                if (!menu.m_38853_(0).m_7993_().m_150930_(Items.f_42398_) || Scene.station.busy()) {
                    return;
                }
                int planks = 0;
                for (int slot = 1; slot <= 9; ++slot) {
                    planks += menu.m_38853_(slot).m_7993_().m_150930_(Items.f_42647_) ? menu.m_38853_(slot).m_7993_().m_41613_() : 0;
                }
                if (planks != 4) {
                    return;
                }
                Cinematic.LOGGER.info("[ClientOnlySmoke] Shift + Lay out put two stick sets down");
                Scene.station(TableStation.class).craft(true);
                stage = 46;
                ticks = 0;
                return;
            }
            case 46: {
                if (mc.f_91074_.m_150109_().m_18947_(Items.f_42398_) != 8) {
                    return;
                }
                Cinematic.LOGGER.info("[ClientOnlySmoke] crafting every laid set gave 8 sticks");
                if (RecipeViewers.get() != null) {
                    Scene.consult(() -> RecipeViewers.get().showHowToGet(new ItemStack((ItemLike)Items.f_42398_)));
                    stage = 47;
                } else {
                    stage = 49;
                }
                ticks = 0;
                return;
            }
            case 47: {
                if (mc.f_91080_ == null || !mc.f_91080_.getClass().getName().startsWith("mezz.jei")) {
                    return;
                }
                if (!Scene.clientOnly() || Scene.phase != Scene.Phase.ACTIVE || !(mc.f_91074_.f_36096_ instanceof CraftingMenu)) {
                    ClientOnlySmoke.fail("JEI's page over a client-only scene dropped the scene or the vanilla menu");
                    return;
                }
                mc.f_91080_.m_7379_();
                stage = 48;
                ticks = 0;
                return;
            }
            case 48: {
                if (!(mc.f_91080_ instanceof SceneScreen)) {
                    return;
                }
                if (!Scene.clientOnly() || Scene.phase != Scene.Phase.ACTIVE || !(mc.f_91074_.f_36096_ instanceof CraftingMenu)) {
                    ClientOnlySmoke.fail("back from JEI, the client-only scene or its vanilla menu was gone");
                    return;
                }
                Cinematic.LOGGER.info("[ClientOnlySmoke] JEI's page opened over the client-only scene and handed the work back");
                stage = 49;
                ticks = 0;
                return;
            }
            case 49: {
                Screen planks = mc.f_91080_;
                if (!(planks instanceof SceneScreen)) return;
                SceneScreen screen = (SceneScreen)planks;
                if (Scene.station.busy()) {
                    return;
                }
                int slot = ClientOnlySmoke.inventorySlot(mc, Items.f_42398_);
                if (slot < 0) {
                    ClientOnlySmoke.fail("no sticks to shift-click onto the table");
                    return;
                }
                screen.panelClick(slot < 9 ? 36 + slot : slot, 0, true);
                stage = 50;
                ticks = 0;
                return;
            }
            case 50: {
                AbstractContainerMenu slot = mc.f_91074_.f_36096_;
                if (!(slot instanceof CraftingMenu)) return;
                CraftingMenu menu = (CraftingMenu)slot;
                if (Scene.station.busy()) {
                    return;
                }
                int onTable = 0;
                for (int i = 1; i <= 9; ++i) {
                    onTable += menu.m_38853_(i).m_7993_().m_150930_(Items.f_42398_) ? menu.m_38853_(i).m_7993_().m_41613_() : 0;
                }
                if (onTable != 8 || mc.f_91074_.m_150109_().m_18947_(Items.f_42398_) != 0 || !menu.m_142621_().m_41619_()) {
                    if (ticks <= 60) return;
                    ClientOnlySmoke.fail("shift-click put " + onTable + " sticks on the table");
                    return;
                }
                Cinematic.LOGGER.info("[ClientOnlySmoke] shift-click laid all 8 sticks on one cell with vanilla clicks");
                Scene.leave();
                stage = 5;
                ticks = 0;
                return;
            }
            case 5: {
                if (Scene.active() || mc.f_91074_.f_36096_ != mc.f_91074_.f_36095_) {
                    return;
                }
                if (mc.f_91074_.m_20182_().m_82557_(original) > 0.01) {
                    ClientOnlySmoke.fail("the player moved on exit");
                    return;
                }
                if (!StationKind.FURNACE.matches(mc.f_91073_.m_8055_(FURNACE)) || !StationKind.ANVIL.matches(mc.f_91073_.m_8055_(ANVIL))) {
                    return;
                }
                mc.f_91072_.m_233732_(mc.f_91074_, InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(1.5, 64.5, 2.0), Direction.SOUTH, FURNACE, false));
                stage = 6;
                ticks = 0;
                return;
            }
            case 6: {
                Object onTable;
                if (!(Scene.clientOnly() && Scene.phase == Scene.Phase.ACTIVE && Scene.station.kind() == StationKind.FURNACE && mc.f_91074_.f_36096_ instanceof AbstractFurnaceMenu && (onTable = mc.f_91080_) instanceof SceneScreen)) {
                    return;
                }
                SceneScreen screen = (SceneScreen)((Object)onTable);
                for (Item item : List.of(Items.f_151050_, Items.f_42398_, Items.f_42329_)) {
                    int slot = ClientOnlySmoke.inventorySlot(mc, item);
                    if (slot < 0) continue;
                    screen.panelClick(slot < 9 ? 36 + slot : slot, 0, true);
                }
                dirtSlot = ClientOnlySmoke.inventorySlot(mc, Items.f_42329_);
                stage = 60;
                ticks = 0;
                return;
            }
            case 60: {
                boolean dirtStays;
                AbstractContainerMenu onTable = mc.f_91074_.f_36096_;
                if (!(onTable instanceof AbstractFurnaceMenu)) {
                    return;
                }
                AbstractFurnaceMenu menu = (AbstractFurnaceMenu)onTable;
                boolean ore = menu.m_38853_(0).m_7993_().m_150930_(Items.f_151050_);
                boolean fuel = menu.m_38853_(1).m_7993_().m_150930_(Items.f_42398_);
                boolean bl = dirtStays = dirtSlot >= 0 && mc.f_91074_.m_150109_().m_8020_(dirtSlot).m_150930_(Items.f_42329_);
                if (!(ore && fuel && dirtStays)) {
                    if (ticks <= 60) return;
                    ClientOnlySmoke.fail("furnace shift-clicks: ore=" + ore + " fuel=" + fuel + " dirt stays=" + dirtStays + " furnace=" + String.valueOf(menu.m_38853_(0).m_7993_()) + "/" + String.valueOf(menu.m_38853_(1).m_7993_()) + "/" + String.valueOf(menu.m_38853_(2).m_7993_()) + " sticks in pack=" + mc.f_91074_.m_150109_().m_18947_(Items.f_42398_));
                    return;
                }
                Cinematic.LOGGER.info("[ClientOnlySmoke] furnace shift-clicks: raw iron in at the top, sticks into the fire, dirt left where it was");
                Scene.leave();
                stage = 7;
                ticks = 0;
                return;
            }
            case 7: {
                if (Scene.active() || mc.f_91074_.f_36096_ != mc.f_91074_.f_36095_) {
                    return;
                }
                mc.f_91072_.m_233732_(mc.f_91074_, InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(1.0, 64.5, 3.5), Direction.WEST, ANVIL, false));
                stage = 8;
                ticks = 0;
                return;
            }
            case 8: {
                if (!Scene.clientOnly() || Scene.phase != Scene.Phase.ACTIVE || Scene.station.kind() != StationKind.ANVIL || !(mc.f_91074_.f_36096_ instanceof AnvilMenu)) {
                    return;
                }
                Scene.leave();
                stage = 9;
                ticks = 0;
                return;
            }
            case 9: {
                if (Scene.active() || mc.f_91074_.f_36096_ != mc.f_91074_.f_36095_) {
                    return;
                }
                if (mc.f_91074_.m_20182_().m_82557_(original) > 0.01) {
                    ClientOnlySmoke.fail("the player moved during a furnace or anvil scene");
                    return;
                }
                Cinematic.LOGGER.info("[ClientOnlySmoke] PASS: vanilla server, crafting result, left-drag sharing, Shift lay-out of every set, shift-click into table and furnace, furnace and anvil scenes, menu close, no player movement");
                stage = 99;
                mc.m_91395_();
                return;
            }
        }
    }

    private static void pointAt(Minecraft mc, Vec3 world) {
        double[] gui = Picking.toGui(world);
        if (gui == null) {
            return;
        }
        mc.f_91067_.f_91507_ = gui[0] * (double)mc.m_91268_().m_85443_() / (double)mc.m_91268_().m_85445_();
        mc.f_91067_.f_91508_ = gui[1] * (double)mc.m_91268_().m_85444_() / (double)mc.m_91268_().m_85446_();
    }

    private static int menuSlot(Direction side, int world) {
        for (int logical = 0; logical < 9; ++logical) {
            if (TableGrid.worldIndex(side, logical / 3, logical % 3) != world) continue;
            return logical + 1;
        }
        return -1;
    }

    private static void clickAsPlayer(Minecraft mc, BlockHitResult hit) {
        mc.f_91077_ = hit;
        try {
            Method use = ObfuscationReflectionHelper.findMethod(Minecraft.class, (String)"m_91277_", (Class[])new Class[0]);
            use.invoke((Object)mc, new Object[0]);
        }
        catch (ReflectiveOperationException | RuntimeException e) {
            throw new IllegalStateException("Cannot simulate the use key", e);
        }
    }

    private static int inventorySlot(Minecraft mc, Item item) {
        for (int i = 0; i < 36; ++i) {
            if (!mc.f_91074_.m_150109_().m_8020_(i).m_150930_(item)) continue;
            return i;
        }
        return -1;
    }

    private static void fail(String reason) {
        Cinematic.LOGGER.error("[ClientOnlySmoke] FAIL: {}", (Object)reason);
        stage = -1;
        Minecraft.m_91087_().m_91395_();
    }

    static {
        dirtSlot = -1;
    }
}


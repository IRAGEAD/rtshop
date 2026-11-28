/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.Window
 *  net.lopymine.ip.config.InventoryParticlesConfig
 *  net.lopymine.ip.config.sub.InventoryParticleConfig
 *  net.lopymine.ip.config.sub.InventoryParticlesCoefficientsConfig
 *  net.lopymine.ip.element.base.IMovableElement
 *  net.lopymine.ip.element.inventory.IInventoryElement
 *  net.lopymine.ip.element.mod.InventoryCursor
 *  net.lopymine.ip.element.mod.InventoryParticle
 *  net.lopymine.ip.element.mod.spawner.IParticleSpawner
 *  net.lopymine.ip.element.mod.spawner.context.ParticleSpawnContext
 *  net.lopymine.ip.renderer.InventoryParticlesRenderer
 *  net.lopymine.ip.resourcepack.manager.ParticlesConfigsManager
 *  net.lopymine.ipi.config.InventoryInteractionsConfig
 *  net.lopymine.ipi.renderer.CursorItemRenderer
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.world.Container
 *  net.minecraft.world.SimpleContainer
 *  net.minecraft.world.inventory.ClickType
 *  net.minecraft.world.inventory.Slot
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.LecternBlockEntity
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.fml.ModList
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.platform.Window;
import dev.dyslexic.cinematic.Cinematic;
import dev.dyslexic.cinematic.Compat;
import dev.dyslexic.cinematic.client.ClientFurnaces;
import dev.dyslexic.cinematic.client.ClientStation;
import dev.dyslexic.cinematic.client.ClientTables;
import dev.dyslexic.cinematic.client.EnchantStation;
import dev.dyslexic.cinematic.client.FreeLook;
import dev.dyslexic.cinematic.client.FurnaceStation;
import dev.dyslexic.cinematic.client.Hands;
import dev.dyslexic.cinematic.client.LecternStation;
import dev.dyslexic.cinematic.client.Picking;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.SpatialAppearance;
import dev.dyslexic.cinematic.client.SpatialTarget;
import dev.dyslexic.cinematic.client.SurfaceAppearance;
import dev.dyslexic.cinematic.client.SurfaceStation;
import dev.dyslexic.cinematic.client.TableStation;
import dev.dyslexic.cinematic.furnace.FurnaceLayout;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import net.lopymine.ip.config.InventoryParticlesConfig;
import net.lopymine.ip.config.sub.InventoryParticleConfig;
import net.lopymine.ip.config.sub.InventoryParticlesCoefficientsConfig;
import net.lopymine.ip.element.base.IMovableElement;
import net.lopymine.ip.element.inventory.IInventoryElement;
import net.lopymine.ip.element.mod.InventoryCursor;
import net.lopymine.ip.element.mod.InventoryParticle;
import net.lopymine.ip.element.mod.spawner.IParticleSpawner;
import net.lopymine.ip.element.mod.spawner.context.ParticleSpawnContext;
import net.lopymine.ip.renderer.InventoryParticlesRenderer;
import net.lopymine.ip.resourcepack.manager.ParticlesConfigsManager;
import net.lopymine.ipi.config.InventoryInteractionsConfig;
import net.lopymine.ipi.renderer.CursorItemRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;

final class InventoryEffects {
    private final List<Target> targets = new ArrayList<Target>();
    private final Map<String, ItemStack> previous = new HashMap<String, ItemStack>();
    private Bridge bridge;
    private ClientStation station;
    private boolean visible;
    private int hoveredWorld = -1;
    private double actionUntil = -1.0;
    private long inventoryBefore;
    private final List<Change> awaitingTransfer = new ArrayList<Change>();
    private double changedAt;
    private double transferredAt = -10.0;
    private ClickType action = ClickType.PICKUP;

    InventoryEffects() {
        if (ModList.get().isLoaded("inventory_particles") && InventoryEffects.supported("inventory_particles", "3.")) {
            try {
                this.bridge = new ParticlesBridge();
            }
            catch (LinkageError | RuntimeException e) {
                this.failed(e);
            }
        }
    }

    private static boolean supported(String id, String major) {
        return ModList.get().getModContainerById(id).map(mod -> mod.getModInfo().getVersion().toString().startsWith(major)).orElse(false);
    }

    private void failed(Throwable e) {
        this.bridge = null;
        Cinematic.LOGGER.warn("[Cinematic] Inventory effects integration unavailable; using the usual scene interface", e);
    }

    void beginFrame(boolean visible) {
        this.targets.clear();
        this.visible = visible;
        if (this.station != Scene.station || !visible) {
            this.previous.clear();
            this.awaitingTransfer.clear();
            this.actionUntil = -1.0;
            this.transferredAt = -10.0;
        }
        this.station = Scene.station;
    }

    void drawer(int index, ItemStack stack, double x, double y, boolean hovered) {
        if (this.bridge != null && this.visible) {
            this.targets.add(new Target("inventory:" + index, stack, x, y, hovered));
        }
    }

    void endFrame(boolean overPanel) {
        ClientStation clientStation;
        int n;
        if (this.bridge == null || !this.visible || this.station == null) {
            return;
        }
        if (overPanel || this.station.busy()) {
            n = -1;
        } else {
            clientStation = this.station;
            if (clientStation instanceof TableStation) {
                TableStation table = (TableStation)clientStation;
                n = table.hoverResult ? 9 : table.hoverCell;
            } else {
                clientStation = this.station;
                if (clientStation instanceof FurnaceStation) {
                    FurnaceStation furnace = (FurnaceStation)clientStation;
                    n = furnace.hoverZone;
                } else {
                    clientStation = this.station;
                    if (clientStation instanceof SurfaceStation) {
                        SurfaceStation surface = (SurfaceStation)clientStation;
                        n = surface.surfaceSlot(false);
                    } else {
                        clientStation = this.station;
                        if (clientStation instanceof SpatialTarget) {
                            SpatialTarget spatial = (SpatialTarget)((Object)clientStation);
                            n = spatial.layoutPick();
                        } else {
                            n = -1;
                        }
                    }
                }
            }
        }
        this.hoveredWorld = n;
        clientStation = this.station;
        if (clientStation instanceof TableStation) {
            TableStation table = (TableStation)clientStation;
            entry = ClientTables.get(table.pos);
            for (int i = 0; i < 9; ++i) {
                this.world(i, entry == null ? ItemStack.f_41583_ : ((ClientTables.Entry)entry).items[i], table.cellPoint(i), overPanel);
            }
            this.world(9, table.result, table.resultPosition(), overPanel);
        } else {
            entry = this.station;
            if (entry instanceof FurnaceStation) {
                FurnaceStation furnace = (FurnaceStation)entry;
                for (FurnaceLayout.Bed bed : furnace.layout.beds) {
                    Vec3 at = furnace.geometry().point(furnace.layout, furnace.pos, furnace.sideDirection(), bed.slot(), (bed.u0() + bed.u1()) / 2.0, bed.floor() + 0.002, -bed.depth(), true);
                    this.world(bed.slot(), ClientFurnaces.item(furnace.pos, bed.slot()), at, overPanel);
                }
            } else {
                entry = this.station;
                if (entry instanceof SurfaceStation) {
                    SurfaceStation surface = (SurfaceStation)entry;
                    for (int i = 0; i < SurfaceAppearance.count(surface.kind()); ++i) {
                        this.world(i, surface.slot(i), surface.restPoint(i), overPanel);
                    }
                } else {
                    ClientStation i = this.station;
                    if (i instanceof SpatialTarget) {
                        SpatialTarget spatial = (SpatialTarget)((Object)i);
                        int count = switch (this.station.kind()) {
                            case StationKind.ANVIL, StationKind.GRINDSTONE, StationKind.ENCHANTING -> 2;
                            case StationKind.STONECUTTER, StationKind.BEACON, StationKind.LECTERN -> 1;
                            case StationKind.JUKEBOX -> 0;
                            default -> SpatialAppearance.count(this.station.kind());
                        };
                        for (int i2 = 0; i2 < count; ++i2) {
                            BlockEntity blockEntity;
                            ItemStack item = spatial.layoutItem(i2);
                            if (this.station instanceof EnchantStation && EnchantStation.menu() != null) {
                                item = EnchantStation.menu().m_38853_(i2).m_7993_();
                            }
                            if (this.station instanceof LecternStation && Minecraft.m_91087_().f_91073_ != null && (blockEntity = Minecraft.m_91087_().f_91073_.m_7702_(this.station.pos)) instanceof LecternBlockEntity) {
                                LecternBlockEntity lectern = (LecternBlockEntity)blockEntity;
                                item = lectern.m_59566_();
                            }
                            this.world(i2, item, spatial.layoutPoint(i2), overPanel);
                        }
                    }
                }
            }
        }
        if (Hands.itemInHand() && !Hands.carried().m_41619_()) {
            this.world(-1, Hands.carried(), Hands.heldPoint(), true);
        }
    }

    private void world(int index, ItemStack stack, Vec3 at, boolean overPanel) {
        double[] screen = Picking.toGui(at);
        if (screen == null) {
            return;
        }
        Window window = Minecraft.m_91087_().m_91268_();
        if (screen[0] < 0.0 || screen[0] >= (double)window.m_85445_() || screen[1] < 0.0 || screen[1] >= (double)window.m_85446_()) {
            return;
        }
        boolean hover = !overPanel && index >= 0 && index == this.hoveredWorld;
        this.targets.add(new Target("station:" + index, stack, screen[0], screen[1], hover));
    }

    void action(ClickType type) {
        if (this.bridge == null) {
            return;
        }
        if (Scene.clock() > this.actionUntil) {
            this.inventoryBefore = InventoryEffects.inventoryFingerprint();
        }
        this.action = type;
        this.actionUntil = Scene.clock() + 6.0;
    }

    private static long inventoryFingerprint() {
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        if (player == null) {
            return 0L;
        }
        long result = 1L;
        for (ItemStack stack : player.m_150109_().f_35974_) {
            result = result * 31L + (long)Compat.hashItem(stack) * 31L + (long)stack.m_41613_();
        }
        ItemStack carried = player.f_36096_.m_142621_();
        return result * 31L + (long)Compat.hashItem(carried) * 31L + (long)carried.m_41613_();
    }

    void tick() {
        if (this.bridge == null) {
            return;
        }
        try {
            boolean working = this.visible && Scene.phase == Scene.Phase.ACTIVE && this.station == Scene.station && !FreeLook.looking();
            ItemStack cursor = working && !Hands.itemInHand() && this.station.handsOverride() == null ? Hands.carried() : ItemStack.f_41583_;
            List<Object> shown = working ? this.targets : List.of();
            ArrayList<Change> changes = new ArrayList<Change>();
            for (Target target : shown) {
                ItemStack before = this.previous.put(target.id(), target.stack().m_41777_());
                if (before == null || !(Scene.clock() <= this.actionUntil) || target.id().equals("station:-1") || ItemStack.m_41728_((ItemStack)before, (ItemStack)target.stack())) continue;
                ItemStack affected = target.stack().m_41619_() ? before : target.stack();
                boolean same = ItemStack.m_150942_((ItemStack)before, (ItemStack)target.stack());
                changes.add(new Change(new Target(target.id(), affected, target.x(), target.y(), target.hovered()), !before.m_41619_() && (!same || before.m_41613_() > target.stack().m_41613_()), !target.stack().m_41619_() && (!same || before.m_41613_() < target.stack().m_41613_())));
            }
            if (!working) {
                this.previous.clear();
                this.awaitingTransfer.clear();
            }
            if (!changes.isEmpty()) {
                this.awaitingTransfer.addAll(changes);
                this.changedAt = Scene.clock();
            }
            long now = InventoryEffects.inventoryFingerprint();
            List<Change> confirmed = List.of();
            if (now != this.inventoryBefore && Scene.clock() <= this.actionUntil) {
                this.transferredAt = Scene.clock();
            }
            if (Scene.clock() - this.transferredAt <= 0.25 && Scene.clock() <= this.actionUntil) {
                confirmed = List.copyOf(this.awaitingTransfer);
                this.awaitingTransfer.clear();
                this.inventoryBefore = now;
            } else if (Scene.clock() - this.changedAt > 0.25) {
                this.awaitingTransfer.clear();
            }
            this.bridge.tick(shown, confirmed, this.action, cursor, working);
        }
        catch (LinkageError | RuntimeException e) {
            this.failed(e);
        }
    }

    void carried(GuiGraphics graphics, ItemStack stack, int mouseX, int mouseY, BiConsumer<Integer, Integer> draw) {
        if (this.bridge != null) {
            try {
                if (this.bridge.carried(graphics, stack, mouseX, mouseY, draw)) {
                    return;
                }
            }
            catch (LinkageError | RuntimeException e) {
                this.failed(e);
            }
        }
        draw.accept(mouseX - 8, mouseY - 8);
    }

    private static final class ParticlesBridge
    implements Bridge {
        private boolean active;
        private final InteractionsBridge interactions = InventoryEffects.supported("inventory_interactions", "2.") ? new InteractionsBridge() : null;
        private final ParticleSpawnContext handContext = new ParticleSpawnContext(InventoryParticleConfig::isCursorSpawnEnabled, InventoryParticlesCoefficientsConfig::getCursorConfig);

        private ParticlesBridge() {
        }

        @Override
        public void tick(List<Target> targets, List<Change> changes, ClickType action, ItemStack carried, boolean active) {
            InventoryParticlesConfig config = InventoryParticlesConfig.getInstance();
            if (!config.getMainConfig().isModEnabled()) {
                return;
            }
            InventoryParticlesRenderer renderer = InventoryParticlesRenderer.getInstance();
            if (this.active && !active) {
                renderer.clear();
                renderer.init();
            }
            this.active = active;
            InventoryCursor cursor = renderer.getCursor();
            double[] mouse = Picking.mouseGui();
            cursor.setMouseX(mouse[0]);
            cursor.setMouseY(mouse[1]);
            cursor.setCurrentStack(carried);
            cursor.setHoveredSlot(null);
            InventoryParticleConfig options = config.getParticleConfig();
            boolean moving = Scene.station != null && Scene.station.busy();
            for (Target target : targets) {
                ParticleSpawnContext context;
                boolean hover;
                if (target.hovered()) {
                    cursor.setHoveredSlot(ParticlesBridge.slot(target));
                }
                if (target.stack().m_41619_()) continue;
                boolean hand = target.id().equals("station:-1");
                boolean bl = hover = target.hovered() && options.isHoveredSlotSpawnEnabled();
                if (moving && !hand && target.id().startsWith("station:") || renderer.isStoppedTicking() || (!hand ? !hover && !options.isGuiSlotsSpawnEnabled() : !options.isCursorSpawnEnabled())) continue;
                List spawners = ParticlesConfigsManager.getSpawnersForItem((Item)target.stack().m_41720_());
                if (spawners == null) continue;
                ParticleSpawnContext particleSpawnContext = hand ? this.handContext : (context = hover ? ParticleSpawnContext.hoveredSlot((Slot)ParticlesBridge.slot(target), (int)0, (int)0) : ParticleSpawnContext.slots((Slot)ParticlesBridge.slot(target), (int)0, (int)0));
                if (hand) {
                    context.setStack(target.stack());
                    context.setX(target.x());
                    context.setY(target.y());
                }
                for (IParticleSpawner spawner : spawners) {
                    for (InventoryParticle particle : spawner.tickAndSpawn(context)) {
                        renderer.addPendingElement((IInventoryElement)particle);
                    }
                }
            }
            if (options.isGuiActionsSpawnEnabled()) {
                for (Change change : changes) {
                    boolean enabled = action == ClickType.QUICK_MOVE ? options.isGuiActionQuickMoveSpawnEnabled() : change.take() && options.isGuiActionTakeSpawnEnabled() || change.put() && options.isGuiActionPutSpawnEnabled();
                    Target target = change.target();
                    if (!enabled) continue;
                    renderer.onGuiAction(ParticlesBridge.slot(target), target.stack(), 0, 0);
                }
            }
            renderer.tick(null, null, null);
            if (this.interactions != null) {
                this.interactions.tick(carried);
            }
        }

        private static Slot slot(Target target) {
            Slot slot = new Slot((Container)new SimpleContainer(new ItemStack[]{target.stack()}), 0, (int)Math.round(target.x() - 8.0), (int)Math.round(target.y() - 8.0));
            slot.f_40219_ = target.id().hashCode();
            return slot;
        }

        @Override
        public boolean carried(GuiGraphics graphics, ItemStack stack, int x, int y, BiConsumer<Integer, Integer> draw) {
            return InventoryParticlesConfig.getInstance().getMainConfig().isModEnabled() && this.interactions != null && this.interactions.carried(graphics, stack, x, y, draw);
        }
    }

    private static interface Bridge {
        public void tick(List<Target> var1, List<Change> var2, ClickType var3, ItemStack var4, boolean var5);

        public boolean carried(GuiGraphics var1, ItemStack var2, int var3, int var4, BiConsumer<Integer, Integer> var5);
    }

    record Target(String id, ItemStack stack, double x, double y, boolean hovered) {
    }

    private record Change(Target target, boolean take, boolean put) {
    }

    private static final class InteractionsBridge {
        private InteractionsBridge() {
        }

        void tick(ItemStack carried) {
            if (InventoryInteractionsConfig.getInstance().getMainConfig().isModEnabled()) {
                CursorItemRenderer.getInstance().tick(carried, (IMovableElement)InventoryParticlesRenderer.getInstance().getCursor());
            }
        }

        boolean carried(GuiGraphics graphics, ItemStack stack, int x, int y, BiConsumer<Integer, Integer> draw) {
            if (!InventoryInteractionsConfig.getInstance().getMainConfig().isModEnabled()) {
                return false;
            }
            CursorItemRenderer.getInstance().render(graphics, stack, x, y, x - 8, y - 8, draw::accept);
            return true;
        }
    }
}


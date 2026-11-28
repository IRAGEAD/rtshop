/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.math.Axis
 *  javax.annotation.Nullable
 *  net.minecraft.ChatFormatting
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.client.renderer.LightTexture
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.MultiBufferSource$BufferSource
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.world.Container
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.inventory.ClickType
 *  net.minecraft.world.inventory.CraftingMenu
 *  net.minecraft.world.item.ItemDisplayContext
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.crafting.CraftingRecipe
 *  net.minecraft.world.item.crafting.RecipeType
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix4f
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dyslexic.cinematic.client.Anim;
import dev.dyslexic.cinematic.client.CameraRig;
import dev.dyslexic.cinematic.client.ClientNet;
import dev.dyslexic.cinematic.client.ClientStation;
import dev.dyslexic.cinematic.client.ClientTables;
import dev.dyslexic.cinematic.client.Fx;
import dev.dyslexic.cinematic.client.Hands;
import dev.dyslexic.cinematic.client.ItemPlacer;
import dev.dyslexic.cinematic.client.Picking;
import dev.dyslexic.cinematic.client.RecipeChoiceStrip;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.TableGridEditor;
import dev.dyslexic.cinematic.client.TableLayout;
import dev.dyslexic.cinematic.client.TableLayouts;
import dev.dyslexic.cinematic.client.TableRecipeBook;
import dev.dyslexic.cinematic.client.TableRecipeGuide;
import dev.dyslexic.cinematic.net.StationActionPayload;
import dev.dyslexic.cinematic.station.RecipeChoices;
import dev.dyslexic.cinematic.station.StationKind;
import dev.dyslexic.cinematic.table.TableCrafting;
import dev.dyslexic.cinematic.table.TableGrid;
import dev.dyslexic.cinematic.table.TableInput;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class TableStation
extends ClientStation {
    static final double HAND_Y = 0.062;
    static final double REST_BACK = 0.4;
    static final double REST_SIDE = 0.24;
    static final double ITEM_REACH = 0.075;
    public static final double LEAN_BACK = 0.86;
    public static final double LEAN_UP = 0.72;
    public static final double LOOK_BACK = -0.04;
    public static final double CRAFT_LENGTH = 0.5;
    public static final double CRAFT_MID = 0.2;
    private static final float RESULT_SIZE = 0.2f;
    private static final float OCCLUDED_OPACITY = 0.28f;
    private static final double FADE_RATE = 14.0;
    public final Vec3 top;
    final TableRecipeBook recipeBook;
    final TableGridEditor gridEditor;
    public int hoverCell = -1;
    public boolean hoverResult;
    public ItemStack result = ItemStack.f_41583_;
    public double resultChangedAt = -10.0;
    private int resultVersion = -1;
    private final RecipeChoiceStrip choiceStrip = new RecipeChoiceStrip();
    private List<CraftingRecipe> competing = List.of();
    @Nullable
    private ResourceLocation chosenRecipe;
    public double craftAt = -10.0;
    @Nullable
    public ItemStack[] craftItems;
    @Nullable
    public byte[] craftFacing;
    private boolean painting;
    private final boolean[] painted = new boolean[9];
    private final float[] itemOpacity = new float[9];
    static final double LAY_MOVE = 0.36;
    private static final double LAY_DROP = 0.64;
    static final int TAKE = -2;
    static final int REPLAN = -3;
    private static final double REPLAN_WAIT = 1.5;
    private final ArrayDeque<int[]> layQueue = new ArrayDeque();
    @Nullable
    private int[] laying;
    private ItemStack layItem = ItemStack.f_41583_;
    private double layAt;
    private boolean layDropped;
    private final List<Integer> takenBack = new ArrayList<Integer>();
    private static final double OCCLUSION_MOVE = 0.06;
    private static final double[][] CELL_SAMPLES = new double[][]{{0.0, 0.0}, {-0.3, -0.3}, {0.3, -0.3}, {-0.3, 0.3}, {0.3, 0.3}};
    private final boolean[] inTheWay = new boolean[9];
    private int occlusionCell = Integer.MIN_VALUE;
    private int occlusionVersion = Integer.MIN_VALUE;
    @Nullable
    private Vec3 occlusionCamera;
    private int spreadStart = -1;
    private int spreadMask;
    private ItemStack spreadStack = ItemStack.f_41583_;
    private double spreadSince;

    public TableStation(BlockPos pos, Direction side) {
        super(pos, side);
        this.top = new Vec3((double)pos.m_123341_() + 0.5, (double)pos.m_123342_() + 1.0, (double)pos.m_123343_() + 0.5);
        Arrays.fill(this.itemOpacity, 1.0f);
        this.recipeBook = new TableRecipeBook(this);
        this.gridEditor = new TableGridEditor(this);
    }

    @Override
    public StationKind kind() {
        return StationKind.TABLE;
    }

    @Override
    public Vec3 center() {
        return this.top;
    }

    @Override
    public List<Vec3> approach(LocalPlayer player) {
        Vec3 stand = new Vec3(this.top.f_82479_ + (double)this.side.m_122429_() * 0.86, player.m_20186_(), this.top.f_82481_ + (double)this.side.m_122431_() * 0.86);
        boolean walk = Math.hypot(stand.f_82479_ - player.m_20185_(), stand.f_82481_ - player.m_20189_()) > 0.06;
        return walk ? List.of(stand) : List.of();
    }

    @Override
    public boolean busy() {
        return this.craftProgress() >= 0.0 || this.laying != null || this.gridEditor.editing();
    }

    @Override
    public boolean showsInventory() {
        return !this.gridEditor.visible();
    }

    @Override
    public Vec3 workPoint() {
        return TableLayouts.current().center(this.pos);
    }

    @Override
    public List<Vec3> workSamples() {
        TableLayout layout = TableLayouts.current();
        if (layout.equals(TableLayout.VANILLA)) {
            return super.workSamples();
        }
        double low = 0.21875;
        double high = 0.78125;
        return List.of(layout.center(this.pos).m_82520_(0.0, 0.04, 0.0), layout.point(this.pos, low, low).m_82520_(0.0, 0.04, 0.0), layout.point(this.pos, high, low).m_82520_(0.0, 0.04, 0.0), layout.point(this.pos, low, high).m_82520_(0.0, 0.04, 0.0), layout.point(this.pos, high, high).m_82520_(0.0, 0.04, 0.0));
    }

    @Override
    public void onEnd() {
        this.gridEditor.cancel();
        this.cancelSpread();
    }

    double surfaceY() {
        return TableLayouts.current().surfaceY(this.pos);
    }

    void layOut(List<int[]> moves) {
        if (this.busy() || moves.isEmpty()) {
            return;
        }
        this.layQueue.addAll(moves);
        this.nextLay();
    }

    @Override
    public void quickMove(int inventorySlot, int menuSlot) {
        int world;
        int logical;
        Minecraft mc = Minecraft.m_91087_();
        if (this.busy() || mc.f_91074_ == null) {
            return;
        }
        ItemStack stack = mc.f_91074_.m_150109_().m_8020_(inventorySlot);
        if (stack.m_41619_()) {
            return;
        }
        ClientTables.Entry entry = ClientTables.get(this.pos);
        TableRecipeGuide guide = this.recipeBook.guide;
        int target = -1;
        for (logical = 0; logical < 9 && target < 0 && guide.selected != null; ++logical) {
            world = TableGrid.worldIndex(this.side, logical / 3, logical % 3);
            if (guide.grid[logical].m_43947_() || guide.placed[logical] || !TableStation.onCell(entry, world).m_41619_() || !guide.grid[logical].test(stack)) continue;
            target = world;
        }
        for (logical = 0; logical < 9 && target < 0; ++logical) {
            world = TableGrid.worldIndex(this.side, logical / 3, logical % 3);
            ItemStack there = TableStation.onCell(entry, world);
            if (there.m_41619_() || !ItemStack.m_150942_((ItemStack)there, (ItemStack)stack) || there.m_41613_() >= there.m_41741_()) continue;
            target = world;
        }
        for (logical = 0; logical < 9 && target < 0; ++logical) {
            world = TableGrid.worldIndex(this.side, logical / 3, logical % 3);
            if (!TableStation.onCell(entry, world).m_41619_()) continue;
            target = world;
        }
        if (target < 0) {
            Scene.sound(SoundEvents.f_12634_, 0.25f, 0.7f);
            return;
        }
        ItemStack there = TableStation.onCell(entry, target);
        int count = Math.min(stack.m_41613_(), there.m_41619_() ? stack.m_41741_() : there.m_41741_() - there.m_41613_());
        this.layOut(List.of(new int[]{target, inventorySlot, count}));
    }

    private static ItemStack onCell(@Nullable ClientTables.Entry entry, int world) {
        return entry == null ? ItemStack.f_41583_ : entry.items[world];
    }

    private void nextLay() {
        this.laying = this.layQueue.poll();
        this.layDropped = false;
        this.layAt = Scene.clock();
        if (this.laying == null) {
            this.takenBack.clear();
        }
        if (this.laying != null && this.laying[1] == -3) {
            this.layItem = ItemStack.f_41583_;
            return;
        }
        Minecraft mc = Minecraft.m_91087_();
        ItemStack source = this.laying == null || mc.f_91074_ == null ? ItemStack.f_41583_ : (this.laying[1] == -2 ? TableStation.onCell(ClientTables.get(this.pos), this.laying[0]) : (this.laying[1] < 0 ? mc.f_91074_.f_36096_.m_142621_() : mc.f_91074_.m_150109_().m_8020_(this.laying[1])));
        ItemStack itemStack = this.layItem = this.laying == null ? ItemStack.f_41583_ : source.m_255036_(Math.min(source.m_41613_(), this.laying[2]));
        if (this.laying != null && this.layItem.m_41619_()) {
            this.nextLay();
        }
    }

    private double layProgress() {
        return Anim.clamp01((Scene.clock() - this.layAt) / 0.36);
    }

    private double layPath() {
        return this.laying != null && this.laying[1] == -2 ? 1.0 - this.layProgress() : this.layProgress();
    }

    private void replan() {
        boolean clear = true;
        ClientTables.Entry entry = ClientTables.get(this.pos);
        for (int cell : this.takenBack) {
            clear &= TableStation.onCell(entry, cell).m_41619_();
        }
        double waited = Scene.clock() - this.layAt;
        if (waited < 0.1 || !clear && waited < 1.5) {
            return;
        }
        boolean all = this.laying[2] == 1;
        TableRecipeGuide guide = this.recipeBook.guide;
        guide.tick();
        for (int[] move : all ? guide.maxLayout().moves() : guide.layoutMoves()) {
            if (move[1] < -1) continue;
            this.layQueue.add(move);
        }
        this.nextLay();
    }

    @Override
    public void beforeFrame() {
        this.gridEditor.frame();
        if (this.laying == null || Scene.phase != Scene.Phase.ACTIVE) {
            if (Scene.phase != Scene.Phase.ACTIVE) {
                this.layQueue.clear();
                this.laying = null;
            }
            return;
        }
        if (this.laying[1] == -3) {
            this.replan();
            return;
        }
        double t = this.layProgress();
        if (this.laying[1] == -2) {
            if (!this.layDropped && t >= 0.36) {
                this.layDropped = true;
                Hands.tap();
                this.takenBack.add(this.laying[0]);
                if (Scene.clientOnly()) {
                    this.clickVanilla(this.laying[0], 0, true);
                } else {
                    ClientNet.sendToServer(new StationActionPayload(this.pos, StationKind.TABLE, StationActionPayload.Action.CLICK, this.laying[0], 0, true, 0));
                }
            }
            if (t >= 1.0) {
                this.nextLay();
            }
            return;
        }
        if (!this.layDropped && t >= 0.64) {
            this.layDropped = true;
            Hands.tap();
            if (Scene.clientOnly()) {
                this.placeVanilla(this.laying[0], this.laying[1], this.layItem.m_41613_());
            } else {
                ClientNet.sendToServer(new StationActionPayload(this.pos, StationKind.TABLE, StationActionPayload.Action.PLACE, this.laying[0], this.laying[1], false, this.layItem.m_41613_()));
            }
        }
        if (t >= 1.0) {
            this.nextLay();
        }
    }

    Vec3 cellPoint(int cell) {
        return TableLayouts.current().cell(this.pos, cell);
    }

    @Nullable
    private Vec3[] layHands() {
        Vec3[] vec3Array;
        Vec3 hand;
        if (this.laying == null || this.laying[1] == -3) {
            return null;
        }
        double t = this.layPath();
        Vec3 cell = this.cellPoint(this.laying[0]);
        boolean right = this.lateral(cell) >= 0.0;
        Vec3 rest = this.rest(right);
        Vec3 take = rest.m_82549_(this.side().m_82490_(0.14)).m_82520_(0.0, 0.03, 0.0);
        Vec3 toward = new Vec3(cell.f_82479_ - rest.f_82479_, 0.0, cell.f_82481_ - rest.f_82481_).m_82541_();
        Vec3 over = cell.m_82520_(0.0, 0.132, 0.0).m_82546_(toward.m_82490_(0.075));
        Vec3 down = over.m_82520_(0.0, -0.06, 0.0);
        if (t < 0.28) {
            hand = Anim.lerp(rest, take, Anim.smoother(t / 0.28));
        } else if (t < 0.5800000000000001) {
            double k = Anim.inOutCubic((t - 0.28) / 0.30000000000000004);
            hand = Anim.lerp(take, over, k).m_82520_(0.0, 0.05 * Anim.hump(k), 0.0);
        } else {
            hand = t < 0.7 ? Anim.lerp(over, down, Anim.hump((t - 0.5800000000000001) / 0.12)) : Anim.lerp(over, rest, Anim.smoother((t - 0.64 - 0.06) / 0.3));
        }
        Vec3 other = this.rest(!right);
        if (right) {
            Vec3[] vec3Array2 = new Vec3[2];
            vec3Array2[0] = hand;
            vec3Array = vec3Array2;
            vec3Array2[1] = other;
        } else {
            Vec3[] vec3Array3 = new Vec3[2];
            vec3Array3[0] = other;
            vec3Array = vec3Array3;
            vec3Array3[1] = hand;
        }
        return vec3Array;
    }

    private void drawLayItem(PoseStack pose, MultiBufferSource buffers, Vec3 camera, int light) {
        if (this.laying == null || this.layItem.m_41619_()) {
            return;
        }
        double t = this.layProgress();
        if (t < 0.24 || t > 0.68) {
            return;
        }
        Vec3 cell = this.cellPoint(this.laying[0]);
        boolean right = this.lateral(cell) >= 0.0;
        Vec3 hand = Hands.position(right);
        Vec3 toward = new Vec3(cell.f_82479_ - hand.f_82479_, 0.0, cell.f_82481_ - hand.f_82481_);
        toward = toward.m_82556_() < 1.0E-6 ? this.forward() : toward.m_82541_();
        Vec3 at = hand.m_82549_(toward.m_82490_(0.075)).m_82520_(0.0, -0.05, 0.0);
        float grow = (float)Anim.smoother((t - 0.24) / 0.08);
        TableLayouts.current().renderItem(pose, buffers, camera, this.layItem, at, this.side, 91L, grow, 14.0f, light, 1.0f, true);
    }

    @Override
    public boolean worksAtAnAngle() {
        return true;
    }

    @Override
    public CameraRig.Pose lean() {
        Vec3 at = this.top.m_82549_(this.side().m_82490_(0.86)).m_82520_(0.0, 0.72, 0.0);
        float pitch = (float)Math.toDegrees(Math.atan2(0.72, 0.9));
        return new CameraRig.Pose(at, this.side.m_122424_().m_122435_(), pitch, 0.0f);
    }

    @Override
    public Vec3 rest(boolean right) {
        return this.top.m_82549_(this.side().m_82490_(0.4)).m_82549_(this.right().m_82490_(right ? 0.24 : -0.24)).m_82520_(0.0, 0.062 + TableLayouts.current().height() / 16.0, 0.0);
    }

    @Override
    public void updateCursor(boolean overPanel) {
        double[] at;
        double[] mouse = Picking.mouseGui();
        overPanel |= this.recipeBook.over(mouse[0], mouse[1]);
        this.hoverResult = false;
        if (!this.result.m_41619_() && !(overPanel |= this.gridEditor.over(mouse[0], mouse[1])) && (at = Picking.toGui(this.resultPosition())) != null) {
            double radius = this.resultRadiusGui();
            this.hoverResult = (mouse[0] - at[0]) * (mouse[0] - at[0]) + (mouse[1] - at[1]) * (mouse[1] - at[1]) < radius * radius;
        }
        Vec3 hit = overPanel || this.hoverResult ? null : Picking.cursorOnPlane(this.surfaceY());
        this.hoverCell = -1;
        this.aim = null;
        if (hit == null) {
            return;
        }
        double lx = hit.f_82479_ - (double)this.pos.m_123341_();
        double lz = hit.f_82481_ - (double)this.pos.m_123343_();
        if (lx > -0.02 && lx < 1.02 && lz > -0.02 && lz < 1.02) {
            this.hoverCell = TableLayouts.current().cellAt(this.pos, hit);
            this.aim = new Vec3(Math.max((double)this.pos.m_123341_() + 0.07, Math.min((double)this.pos.m_123341_() + 0.93, hit.f_82479_)), this.surfaceY(), Math.max((double)this.pos.m_123343_() + 0.07, Math.min((double)this.pos.m_123343_() + 0.93, hit.f_82481_)));
        }
    }

    @Override
    public void clearCursor() {
        super.clearCursor();
        this.hoverCell = -1;
        this.hoverResult = false;
    }

    void updateOcclusion(Vec3 camera) {
        int version;
        ClientTables.Entry entry = ClientTables.get(this.pos);
        int cell = Scene.phase == Scene.Phase.ACTIVE && this.craftProgress() < 0.0 ? this.hoverCell : -1;
        int n = version = entry == null ? -1 : entry.version;
        if (cell != this.occlusionCell || version != this.occlusionVersion || this.occlusionCamera == null || camera.m_82557_(this.occlusionCamera) > 0.0036) {
            this.occlusionCell = cell;
            this.occlusionVersion = version;
            this.occlusionCamera = camera;
            TableLayout layout = TableLayouts.current();
            block0: for (int i = 0; i < 9; ++i) {
                ItemStack stack = entry == null ? ItemStack.f_41583_ : entry.items[i];
                this.inTheWay[i] = false;
                if (cell < 0 || i == cell || stack.m_41619_()) continue;
                Vec3 base = this.cellPoint(i);
                AABB bounds = (layout.equals(TableLayout.VANILLA) ? ItemPlacer.restingBounds(stack, base) : layout.bounds(stack, base)).m_82400_(0.003);
                Vec3 target = this.cellPoint(cell);
                for (double[] at : CELL_SAMPLES) {
                    Vec3 point = new Vec3(target.f_82479_ + at[0] * 0.1875, this.surfaceY(), target.f_82481_ + at[1] * 0.1875);
                    if (!bounds.m_82371_(camera, point).isPresent()) continue;
                    this.inTheWay[i] = true;
                    continue block0;
                }
            }
        }
        for (int i = 0; i < 9; ++i) {
            ItemStack stack = entry == null ? ItemStack.f_41583_ : entry.items[i];
            float target = this.inTheWay[i] ? 0.28f : 1.0f;
            float f = this.itemOpacity[i] = stack.m_41619_() ? 1.0f : (float)Anim.approach(this.itemOpacity[i], target, 14.0, Scene.frameDt);
            if (!(Math.abs(this.itemOpacity[i] - target) < 0.001f)) continue;
            this.itemOpacity[i] = target;
        }
    }

    public float itemOpacity(int cell) {
        return cell >= 0 && cell < this.itemOpacity.length ? this.itemOpacity[cell] : 1.0f;
    }

    boolean hasFadedItems() {
        for (float opacity : this.itemOpacity) {
            if (!(opacity < 0.999f)) continue;
            return true;
        }
        return false;
    }

    @Override
    public Vec3 handTarget(Vec3 aim, Vec3 shoulder, boolean carrying, double tap) {
        Vec3 reach = new Vec3(aim.f_82479_ - shoulder.f_82479_, 0.0, aim.f_82481_ - shoulder.f_82481_).m_82541_();
        double hover = carrying ? 0.075 : 0.045;
        return new Vec3(aim.f_82479_, this.surfaceY() + 0.062 + hover - 0.06 * tap, aim.f_82481_).m_82546_(reach.m_82490_(0.075));
    }

    @Override
    public Vec3 heldItemPoint(Vec3 hand, Vec3 shoulder) {
        Vec3 reach = new Vec3(hand.f_82479_ - shoulder.f_82479_, 0.0, hand.f_82481_ - shoulder.f_82481_).m_82541_();
        return hand.m_82549_(reach.m_82490_(0.075)).m_82520_(0.0, -0.05, 0.0);
    }

    @Override
    @Nullable
    public Vec3[] handsOverride() {
        Vec3[] lay = this.layHands();
        if (lay != null) {
            return lay;
        }
        double craft = this.craftProgress();
        if (craft < 0.0) {
            return null;
        }
        Vec3 middle = TableLayouts.current().center(this.pos).m_82549_(this.side().m_82490_(0.04));
        double y = 0.112 - 0.07 * Anim.hump(Anim.window(craft, 0.08, 0.62));
        return new Vec3[]{middle.m_82549_(this.right().m_82490_(0.085)).m_82520_(0.0, y, 0.0), middle.m_82549_(this.right().m_82490_(-0.085)).m_82520_(0.0, y, 0.0)};
    }

    @Override
    public void click(int button, boolean shift) {
        boolean carrying;
        if (this.laying != null) {
            return;
        }
        if (this.hoverResult && button == 0) {
            this.craft(shift);
            return;
        }
        if (this.hoverCell < 0) {
            return;
        }
        boolean bl = carrying = !Hands.carried().m_41619_();
        if (button == 0 && !shift && carrying) {
            this.spreadStart = this.hoverCell;
            this.spreadMask = this.spreadable(this.hoverCell) ? 1 << this.hoverCell : 0;
            this.spreadStack = Hands.carried().m_41777_();
            this.spreadSince = Scene.clock();
            return;
        }
        this.send(StationActionPayload.Action.CLICK, this.hoverCell, button, shift);
        if (button == 1 && carrying) {
            this.painting = true;
            Arrays.fill(this.painted, false);
            this.painted[this.hoverCell] = true;
        }
    }

    @Override
    public void drag(int button) {
        if (this.painting && button == 1) {
            Scene.updateCursor();
            if (this.hoverCell >= 0 && !this.painted[this.hoverCell]) {
                this.painted[this.hoverCell] = true;
                this.send(StationActionPayload.Action.PAINT, this.hoverCell, 1, false);
            }
        }
        if (this.spreadStart >= 0 && button == 0) {
            Scene.updateCursor();
            if (this.hoverCell >= 0 && (this.spreadMask & 1 << this.hoverCell) == 0 && this.spreadable(this.hoverCell) && this.spreadStack.m_41613_() > Integer.bitCount(this.spreadMask)) {
                this.spreadMask |= 1 << this.hoverCell;
                Scene.sound(SoundEvents.f_12634_, 0.12f, 1.6f + 0.05f * (float)Integer.bitCount(this.spreadMask));
            }
        }
    }

    @Override
    public void release(int button) {
        if (button == 1) {
            this.painting = false;
        }
        if (button == 0 && this.spreadStart >= 0) {
            int start = this.spreadStart;
            int mask = this.spreadMask;
            this.spreadStart = -1;
            this.spreadMask = 0;
            if (Integer.bitCount(mask) > 1) {
                this.spread(mask);
            } else {
                this.send(StationActionPayload.Action.CLICK, start, 0, false);
            }
        }
    }

    void cancelSpread() {
        this.spreadStart = -1;
        this.spreadMask = 0;
    }

    private boolean spreadable(int cell) {
        ClientTables.Entry entry = ClientTables.get(this.pos);
        ItemStack onCell = entry == null ? ItemStack.f_41583_ : entry.items[cell];
        ItemStack held = Hands.carried();
        return !held.m_41619_() && (onCell.m_41619_() || ItemStack.m_150942_((ItemStack)held, (ItemStack)onCell));
    }

    int spreadCells() {
        return this.spreadStart >= 0 && Integer.bitCount(this.spreadMask) > 1 ? this.spreadMask : 0;
    }

    int spreadShare() {
        int cells = Integer.bitCount(this.spreadCells());
        return cells == 0 ? 0 : Hands.carried().m_41613_() / cells;
    }

    private void spread(int mask) {
        Hands.tap();
        if (!Scene.clientOnly()) {
            ClientNet.sendToServer(new StationActionPayload(this.pos, StationKind.TABLE, StationActionPayload.Action.SPREAD, mask, 0, false, 0));
            return;
        }
        Minecraft mc = Minecraft.m_91087_();
        CraftingMenu menu = this.vanillaMenu();
        if (menu == null || mc.f_91072_ == null || mc.f_91074_ == null) {
            return;
        }
        int type = 0;
        mc.f_91072_.m_171799_(menu.f_38840_, -999, AbstractContainerMenu.m_38930_((int)0, (int)type), ClickType.QUICK_CRAFT, (Player)mc.f_91074_);
        for (int cell = 0; cell < 9; ++cell) {
            if ((mask & 1 << cell) == 0 || this.menuCell(cell) <= 0) continue;
            mc.f_91072_.m_171799_(menu.f_38840_, this.menuCell(cell), AbstractContainerMenu.m_38930_((int)1, (int)type), ClickType.QUICK_CRAFT, (Player)mc.f_91074_);
        }
        mc.f_91072_.m_171799_(menu.f_38840_, -999, AbstractContainerMenu.m_38930_((int)2, (int)type), ClickType.QUICK_CRAFT, (Player)mc.f_91074_);
    }

    private void send(StationActionPayload.Action action, int cell, int button, boolean shift) {
        Hands.tap();
        if (Scene.clientOnly()) {
            this.clickVanilla(cell, button, shift);
        } else {
            ClientNet.sendToServer(new StationActionPayload(this.pos, StationKind.TABLE, action, cell, button, shift, 0));
        }
    }

    @Nullable
    private CraftingMenu vanillaMenu() {
        CraftingMenu menu;
        AbstractContainerMenu abstractContainerMenu;
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        return player != null && (abstractContainerMenu = player.f_36096_) instanceof CraftingMenu ? (menu = (CraftingMenu)abstractContainerMenu) : null;
    }

    private int menuCell(int world) {
        for (int logical = 0; logical < 9; ++logical) {
            if (TableGrid.worldIndex(this.side, logical / 3, logical % 3) != world) continue;
            return logical + 1;
        }
        return -1;
    }

    private void clickVanilla(int world, int button, boolean shift) {
        Minecraft mc = Minecraft.m_91087_();
        CraftingMenu menu = this.vanillaMenu();
        int slot = this.menuCell(world);
        if (menu != null && slot > 0 && mc.f_91072_ != null && mc.f_91074_ != null) {
            mc.f_91072_.m_171799_(menu.f_38840_, slot, button, shift ? ClickType.QUICK_MOVE : ClickType.PICKUP, (Player)mc.f_91074_);
        }
    }

    private void placeVanilla(int world, int source, int count) {
        int inventorySlot;
        Minecraft mc = Minecraft.m_91087_();
        CraftingMenu menu = this.vanillaMenu();
        if (menu == null || mc.f_91072_ == null || mc.f_91074_ == null || this.menuCell(world) < 0) {
            return;
        }
        if (source < 0) {
            for (int i = 0; i < count && !menu.m_142621_().m_41619_(); ++i) {
                this.clickVanilla(world, 1, false);
            }
            return;
        }
        if (!menu.m_142621_().m_41619_() || source >= 36) {
            return;
        }
        int n = inventorySlot = source < 9 ? 37 + source : 10 + source - 9;
        if (menu.m_38853_(inventorySlot).m_7993_().m_41619_()) {
            return;
        }
        mc.f_91072_.m_171799_(menu.f_38840_, inventorySlot, 0, ClickType.PICKUP, (Player)mc.f_91074_);
        if (menu.m_142621_().m_41613_() <= count) {
            this.clickVanilla(world, 0, false);
        } else {
            for (int i = 0; i < count && !menu.m_142621_().m_41619_(); ++i) {
                this.clickVanilla(world, 1, false);
            }
        }
        if (!menu.m_142621_().m_41619_()) {
            mc.f_91072_.m_171799_(menu.f_38840_, inventorySlot, 0, ClickType.PICKUP, (Player)mc.f_91074_);
        }
    }

    public void craft(boolean all) {
        if (this.result.m_41619_()) {
            return;
        }
        ClientTables.Entry entry = ClientTables.get(this.pos);
        if (entry != null) {
            this.craftItems = new ItemStack[9];
            for (int i = 0; i < 9; ++i) {
                this.craftItems[i] = entry.items[i].m_41777_();
            }
            this.craftFacing = (byte[])entry.facing.clone();
        }
        this.craftAt = Scene.clock();
        if (Scene.clientOnly()) {
            Minecraft mc = Minecraft.m_91087_();
            CraftingMenu menu = this.vanillaMenu();
            if (menu != null && mc.f_91072_ != null && mc.f_91074_ != null) {
                mc.f_91072_.m_171799_(menu.f_38840_, 0, 0, all ? ClickType.QUICK_MOVE : ClickType.PICKUP, (Player)mc.f_91074_);
            }
        } else {
            ClientNet.sendToServer(new StationActionPayload(this.pos, StationKind.TABLE, StationActionPayload.Action.CRAFT, 0, 0, all, 0));
        }
    }

    public double craftProgress() {
        double t = (Scene.clock() - this.craftAt) / 0.5;
        return t >= 0.0 && t <= 1.0 ? t : -1.0;
    }

    @Override
    RecipeChoiceStrip choices() {
        return this.choiceStrip;
    }

    @Override
    void chose(int index) {
        if (index < 0 || index >= this.competing.size()) {
            return;
        }
        this.chosenRecipe = this.competing.get(index).m_6423_();
        super.chose(index);
        this.resultVersion = Integer.MIN_VALUE;
    }

    private void updateChoices(@Nullable ClientTables.Entry entry) {
        Minecraft mc = Minecraft.m_91087_();
        if (!RecipeChoices.available() || Scene.clientOnly() || entry == null || mc.f_91073_ == null || mc.f_91074_ == null) {
            this.choiceStrip.clear();
            this.competing = List.of();
            return;
        }
        TableInput input = new TableInput(TableGrid.oriented(entry.list(), this.side));
        List<Object> list = this.competing = input.m_7983_() ? List.of() : RecipeChoices.matching((Level)mc.f_91073_, RecipeType.f_44107_, input);
        if (this.competing.size() < 2) {
            this.choiceStrip.clear();
            return;
        }
        int index = -1;
        for (int i = 0; i < this.competing.size(); ++i) {
            if (!this.competing.get(i).m_6423_().equals((Object)this.chosenRecipe)) continue;
            index = i;
        }
        if (index < 0) {
            ResourceLocation known = RecipeChoices.chosen((Player)mc.f_91074_);
            for (int i = 0; i < this.competing.size(); ++i) {
                if (!this.competing.get(i).m_6423_().equals((Object)known)) continue;
                index = i;
            }
            index = Math.max(0, index);
            this.chosenRecipe = this.competing.get(index).m_6423_();
            RecipeChoiceStrip.send(this.pos, this.kind(), index);
        }
        ArrayList<ItemStack> outputs = new ArrayList<ItemStack>();
        for (CraftingRecipe recipe : this.competing) {
            outputs.add(recipe.m_5874_((Container)input, mc.f_91073_.m_9598_()));
        }
        this.choiceStrip.set(outputs, index);
    }

    @Override
    public void tick() {
        ItemStack next;
        int version;
        this.recipeBook.tick();
        Minecraft mc = Minecraft.m_91087_();
        ClientTables.Entry entry = ClientTables.get(this.pos);
        int n = version = entry == null ? -1 : entry.version;
        if (version == this.resultVersion && !Scene.clientOnly()) {
            return;
        }
        this.resultVersion = version;
        this.updateChoices(entry);
        ItemStack itemStack = Scene.clientOnly() ? (this.vanillaMenu() == null ? ItemStack.f_41583_ : this.vanillaMenu().m_38853_(0).m_7993_()) : (next = entry == null || mc.f_91073_ == null ? ItemStack.f_41583_ : TableCrafting.match((Level)mc.f_91073_, entry.list(), this.side, this.chosenRecipe).map(TableCrafting.Match::result).orElse(ItemStack.f_41583_));
        if (!ItemStack.m_150942_((ItemStack)next, (ItemStack)this.result) || next.m_41613_() != this.result.m_41613_()) {
            this.resultChangedAt = Scene.clock();
        }
        this.result = next;
    }

    @Override
    public void renderWorld(PoseStack pose, MultiBufferSource buffers, Vec3 camera, int light) {
        this.gridEditor.renderWorld(pose, buffers, camera);
        if (this.gridEditor.visible()) {
            return;
        }
        this.drawResult(pose, buffers, camera, light);
        this.drawLayItem(pose, buffers, camera, light);
        this.drawSpreadPreview(pose, buffers, camera, light);
        this.recipeBook.renderWorld(pose, buffers, camera, light);
    }

    private void drawSpreadPreview(PoseStack pose, MultiBufferSource buffers, Vec3 camera, int light) {
        int spread = this.spreadCells();
        ItemStack held = Hands.carried();
        if (spread == 0 || held.m_41619_() || this.spreadShare() <= 0) {
            return;
        }
        ClientTables.Entry entry = ClientTables.get(this.pos);
        float appear = (float)Anim.smoother((Scene.clock() - this.spreadSince) / 0.2);
        if (buffers instanceof MultiBufferSource.BufferSource) {
            MultiBufferSource.BufferSource source = (MultiBufferSource.BufferSource)buffers;
            source.m_109911_();
        }
        for (int cell = 0; cell < 9; ++cell) {
            if ((spread & 1 << cell) == 0 || entry != null && !entry.items[cell].m_41619_()) continue;
            TableLayout layout = TableLayouts.current();
            layout.renderItem(pose, buffers, camera, held.m_255036_(this.spreadShare()), layout.cell(this.pos, cell).m_82520_(0.0, 0.002, 0.0), this.side, cell, 1.0f, 0.0f, light, appear * 0.4f, false);
        }
        if (buffers instanceof MultiBufferSource.BufferSource) {
            MultiBufferSource.BufferSource source = (MultiBufferSource.BufferSource)buffers;
            source.m_109911_();
        }
    }

    @Override
    public void renderScreen(GuiGraphics graphics, Font font, float alpha) {
        if (!this.gridEditor.visible()) {
            this.recipeBook.render(graphics, font, alpha);
        }
        this.gridEditor.render(graphics, font, alpha);
    }

    void drawHighlight(PoseStack pose, MultiBufferSource buffers, Vec3 cam) {
        if (this.craftProgress() >= 0.0) {
            return;
        }
        int spread = this.spreadCells();
        for (int cell = 0; cell < 9; ++cell) {
            if (cell != this.hoverCell && (spread & 1 << cell) == 0) continue;
            this.drawHighlight(pose, buffers, cam, cell);
        }
    }

    private void drawHighlight(PoseStack pose, MultiBufferSource buffers, Vec3 cam, int cell) {
        double half = 0.08775;
        pose.m_85836_();
        TableLayouts.current().cellPose(pose, this.pos, cell, cam);
        double cx = 0.0;
        double cz = 0.0;
        double y = 0.0;
        int alpha = 47;
        Matrix4f m = pose.m_85850_().m_252922_();
        VertexConsumer quads = buffers.m_6299_(Fx.HIGHLIGHT);
        quads.m_252986_(m, (float)(cx - half), (float)y, (float)(cz - half)).m_6122_(255, 244, 214, alpha).m_5752_();
        quads.m_252986_(m, (float)(cx - half), (float)y, (float)(cz + half)).m_6122_(255, 244, 214, alpha).m_5752_();
        quads.m_252986_(m, (float)(cx + half), (float)y, (float)(cz + half)).m_6122_(255, 244, 214, alpha).m_5752_();
        quads.m_252986_(m, (float)(cx + half), (float)y, (float)(cz - half)).m_6122_(255, 244, 214, alpha).m_5752_();
        double width = 0.004;
        TableStation.highlightStrip(quads, m, cx - half, cz - half, cx + half, cz - half + width, y, 150);
        TableStation.highlightStrip(quads, m, cx - half, cz + half - width, cx + half, cz + half, y, 150);
        TableStation.highlightStrip(quads, m, cx - half, cz - half, cx - half + width, cz + half, y, 150);
        TableStation.highlightStrip(quads, m, cx + half - width, cz - half, cx + half, cz + half, y, 150);
        pose.m_85849_();
    }

    private static void highlightStrip(VertexConsumer quads, Matrix4f m, double x0, double z0, double x1, double z1, double y, int alpha) {
        quads.m_252986_(m, (float)x0, (float)y, (float)z0).m_6122_(255, 235, 185, alpha).m_5752_();
        quads.m_252986_(m, (float)x0, (float)y, (float)z1).m_6122_(255, 235, 185, alpha).m_5752_();
        quads.m_252986_(m, (float)x1, (float)y, (float)z1).m_6122_(255, 235, 185, alpha).m_5752_();
        quads.m_252986_(m, (float)x1, (float)y, (float)z0).m_6122_(255, 235, 185, alpha).m_5752_();
    }

    public Vec3 resultPosition() {
        double bob = Math.sin(Scene.clock() * 2.1) * 0.012;
        return this.top.m_82549_(this.forward().m_82490_(0.42)).m_82520_(0.0, 0.17 + bob, 0.0);
    }

    private double resultRadiusGui() {
        Vec3 at = this.resultPosition();
        double[] a = Picking.toGui(at);
        double[] b = Picking.toGui(at.m_82520_(0.0, (double)0.1f, 0.0));
        if (a == null || b == null) {
            return 12.0;
        }
        return Math.max(10.0, Math.hypot(a[0] - b[0], a[1] - b[1]) * 1.25);
    }

    private void drawResult(PoseStack pose, MultiBufferSource buffers, Vec3 cam, int light) {
        double pop;
        double hover;
        if (this.result.m_41619_()) {
            return;
        }
        double craft = this.craftProgress();
        double appear = Anim.outBack(Anim.window(Scene.clock() - this.resultChangedAt, 0.0, 0.35), 1.8);
        float size = (float)((double)0.2f * appear * (hover = this.hoverResult ? 1.14 : 1.0) * (pop = craft >= 0.0 ? 1.0 - 0.6 * Anim.hump(Anim.window(craft, 0.15, 0.55)) : 1.0));
        if (size <= 0.002f) {
            return;
        }
        Vec3 at = this.resultPosition();
        float spin = (float)(Scene.clock() * (double)(this.hoverResult ? 70 : 32) % 360.0);
        Minecraft mc = Minecraft.m_91087_();
        pose.m_85836_();
        pose.m_85837_(at.f_82479_ - cam.f_82479_, at.f_82480_ - cam.f_82480_, at.f_82481_ - cam.f_82481_);
        pose.m_252781_(Axis.f_252436_.m_252977_(spin));
        boolean block = mc.m_91291_().m_174264_(this.result, (Level)mc.f_91073_, null, 0).m_7539_();
        float scale = block ? size * 1.6f : size;
        pose.m_85841_(scale, scale, scale);
        int glow = LightTexture.m_109885_((int)Math.max(LightTexture.m_109883_((int)light), 12), (int)LightTexture.m_109894_((int)light));
        mc.m_91291_().m_269128_(this.result, ItemDisplayContext.FIXED, glow, OverlayTexture.f_118083_, pose, buffers, (Level)mc.f_91073_, 0);
        pose.m_85849_();
    }

    @Override
    @Nullable
    public List<Component> tooltip(ItemStack carried) {
        double[] mouse = Picking.mouseGui();
        List<Component> editorTip = this.gridEditor.tooltip(mouse[0], mouse[1]);
        if (editorTip != null || this.gridEditor.editing()) {
            return editorTip;
        }
        List<Component> guide = this.recipeBook.tooltip(mouse[0], mouse[1]);
        if (guide != null || this.recipeBook.over(mouse[0], mouse[1])) {
            return guide;
        }
        int spread = Integer.bitCount(this.spreadCells());
        if (spread > 0) {
            int share = this.spreadShare();
            int rest = carried.m_41613_() - share * spread;
            ArrayList<Component> lines = new ArrayList<Component>();
            lines.add((Component)Component.m_237110_((String)"cinematic.hint.spread", (Object[])new Object[]{share, spread}));
            if (rest > 0) {
                lines.add((Component)Component.m_237110_((String)"cinematic.hint.spread_rest", (Object[])new Object[]{rest}).m_130940_(ChatFormatting.GRAY));
            }
            return lines;
        }
        if (!carried.m_41619_()) {
            return null;
        }
        if (this.hoverResult && !this.result.m_41619_()) {
            ArrayList<Component> lines = new ArrayList<Component>(Screen.m_280152_((Minecraft)Minecraft.m_91087_(), (ItemStack)this.result));
            lines.add((Component)Component.m_237115_((String)"cinematic.hint.result").m_130940_(ChatFormatting.GRAY));
            return lines;
        }
        ClientTables.Entry entry = ClientTables.get(this.pos);
        if (this.hoverCell >= 0 && entry != null && !entry.items[this.hoverCell].m_41619_()) {
            ItemStack stack = entry.items[this.hoverCell];
            ArrayList<Component> lines = new ArrayList<Component>(Screen.m_280152_((Minecraft)Minecraft.m_91087_(), (ItemStack)stack));
            lines.add((Component)Component.m_237110_((String)"cinematic.hint.table_count", (Object[])new Object[]{stack.m_41613_()}).m_130940_(ChatFormatting.GRAY));
            return lines;
        }
        return null;
    }

    @Override
    public Component hint() {
        if (this.gridEditor.editing()) {
            return TableGridEditor.text("hint", new Object[0]);
        }
        return Component.m_237115_((String)"cinematic.hint.table");
    }
}


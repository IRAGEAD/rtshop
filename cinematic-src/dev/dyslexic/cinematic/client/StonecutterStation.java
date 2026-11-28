/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.math.Axis
 *  javax.annotation.Nullable
 *  net.minecraft.ChatFormatting
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.Font$DisplayMode
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Vec3i
 *  net.minecraft.network.chat.Component
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.ClickType
 *  net.minecraft.world.inventory.StonecutterMenu
 *  net.minecraft.world.item.BlockItem
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.crafting.StonecutterRecipe
 *  net.minecraft.world.phys.Vec3
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dyslexic.cinematic.client.Anim;
import dev.dyslexic.cinematic.client.CameraRig;
import dev.dyslexic.cinematic.client.ClientMenus;
import dev.dyslexic.cinematic.client.EnchantFx;
import dev.dyslexic.cinematic.client.Fx;
import dev.dyslexic.cinematic.client.GrindstoneStation;
import dev.dyslexic.cinematic.client.Hands;
import dev.dyslexic.cinematic.client.ItemPlacer;
import dev.dyslexic.cinematic.client.MenuStation;
import dev.dyslexic.cinematic.client.Picking;
import dev.dyslexic.cinematic.client.RemainingLayouts;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.SpatialAppearance;
import dev.dyslexic.cinematic.client.SpatialEditor;
import dev.dyslexic.cinematic.client.SpatialLayouts;
import dev.dyslexic.cinematic.client.SpatialTarget;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.phys.Vec3;

public final class StonecutterStation
extends MenuStation<StonecutterMenu>
implements SpatialTarget {
    final SpatialEditor gridEditor = new SpatialEditor(this);
    static final double HAND_Y = 0.062;
    static final double ITEM_REACH = 0.07;
    static final double TABLE = 0.5625;
    static final double BLADE = 0.4375;
    static final double DIAL_WIDE = 0.72;
    static final double DIAL_HIGH = 0.5;
    static final int DIAL_SLOTS = 9;
    static final float SHAPE_SIZE = 0.15f;
    static final float PIECE_SIZE = 0.2f;
    public static final double CUT_LENGTH = 1.05;
    public static final double CUT_ALL_LENGTH = 1.5;
    static final double BITE = 0.26;
    public final Vec3 top;
    public Target target = Target.NONE;
    public int hoverShape = -1;
    private double scroll;
    private int scrollTarget;
    private int lastShapes = -1;
    private double shapesAt = -10.0;
    private int lastHover = -1;
    private ItemStack input = ItemStack.f_41583_;
    private ItemStack piece = ItemStack.f_41583_;
    private boolean cutAll;
    private double length = 1.05;

    @Override
    public SpatialEditor layoutEditor() {
        return this.gridEditor;
    }

    public StonecutterStation(BlockPos pos, Direction side) {
        super(pos, side, StonecutterMenu.class, 1);
        this.top = StonecutterStation.topOf(pos);
    }

    static Vec3 topOf(BlockPos pos) {
        return new Vec3((double)pos.m_123341_() + 0.5, (double)pos.m_123342_() + 0.5625, (double)pos.m_123343_() + 0.5);
    }

    private static Vec3 rightOf(Direction side) {
        return Vec3.m_82528_((Vec3i)side.m_122424_().m_122427_().m_122436_());
    }

    @Override
    public StationKind kind() {
        return StationKind.STONECUTTER;
    }

    @Override
    public Vec3 center() {
        return this.top;
    }

    @Override
    public SoundEvent touchSound() {
        return SoundEvents.f_12450_;
    }

    @Override
    public BlockPos lightPos() {
        return this.pos.m_7494_();
    }

    static Vec3 stackPoint(Vec3 top, Direction side) {
        return RemainingLayouts.point(StationKind.STONECUTTER, top, side, 0);
    }

    @Override
    protected Vec3 restPoint(int slot) {
        return StonecutterStation.stackPoint(this.top, this.side).m_82549_(this.right().m_82490_(this.denyShake()));
    }

    @Override
    public List<Vec3> approach(LocalPlayer player) {
        Vec3 stand = new Vec3(this.top.f_82479_ + (double)this.side.m_122429_() * 0.95, player.m_20186_(), this.top.f_82481_ + (double)this.side.m_122431_() * 0.95);
        boolean walk = Math.hypot(stand.f_82479_ - player.m_20185_(), stand.f_82481_ - player.m_20189_()) > 0.06;
        return walk ? List.of(stand) : List.of();
    }

    @Override
    public CameraRig.Pose lean() {
        Vec3 eye = this.top.m_82549_(this.side().m_82490_(1.0)).m_82520_(0.0, 0.95, 0.0);
        Vec3 look = this.top.m_82549_(this.side().m_82490_(-0.05)).m_82520_(0.0, 0.25, 0.0).m_82546_(eye);
        float yaw = (float)Math.toDegrees(Math.atan2(-look.f_82479_, look.f_82481_));
        float pitch = (float)(-Math.toDegrees(Math.atan2(look.f_82480_, Math.hypot(look.f_82479_, look.f_82481_))));
        return new CameraRig.Pose(eye, yaw, pitch, 1.5f);
    }

    @Override
    public float bodyPitch() {
        return 50.0f;
    }

    @Override
    public Vec3 rest(boolean right) {
        return this.top.m_82549_(this.side().m_82490_(0.36)).m_82549_(this.right().m_82490_(right ? 0.26 : -0.38)).m_82520_(0.0, 0.062, 0.0);
    }

    private List<StonecutterRecipe> shapes() {
        StonecutterMenu menu = (StonecutterMenu)this.menu();
        return menu == null || !menu.m_40341_() ? List.of() : menu.m_40339_();
    }

    private ItemStack shapeResult(int k) {
        Minecraft mc = Minecraft.m_91087_();
        List<StonecutterRecipe> shapes = this.shapes();
        return k < 0 || k >= shapes.size() || mc.f_91073_ == null ? ItemStack.f_41583_ : shapes.get(k).m_8043_(mc.f_91073_.m_9598_());
    }

    private int visible() {
        return Math.min(9, this.shapes().size());
    }

    private double step() {
        int n = this.visible();
        return n <= 1 ? 0.0 : Math.min(22.0, 144.0 / (double)(n - 1));
    }

    private double angle(int k) {
        return 90.0 + ((double)(this.visible() - 1) / 2.0 - ((double)k - this.scroll)) * this.step();
    }

    private double onDial(int k) {
        double a = this.angle(k);
        return Anim.smoother(Anim.window(a, 4.0, 18.0)) * Anim.smoother(Anim.window(176.0 - a, 0.0, 14.0));
    }

    public Vec3 shapePoint(int k) {
        double a = Math.toRadians(this.angle(k));
        double bob = 0.008 * Math.sin(Scene.clock() * 1.7 + (double)k * 1.3);
        Vec3 original = this.top.m_82549_(this.side().m_82490_(-0.06)).m_82549_(this.right().m_82490_(Math.cos(a) * 0.72)).m_82520_(0.0, 0.04 + Math.sin(a) * 0.5 + bob, 0.0);
        return RemainingLayouts.transform(this.kind(), this.top, this.side, 2, original);
    }

    @Override
    public void scroll(double delta) {
        int max = Math.max(0, this.shapes().size() - 9);
        int next = Mth.m_14045_((int)(this.scrollTarget - (int)Math.signum(delta)), (int)0, (int)max);
        if (next != this.scrollTarget) {
            this.scrollTarget = next;
            Scene.sound(SoundEvents.f_12495_, 0.3f, 1.4f);
        }
    }

    @Override
    public void updateCursor(boolean overPanel) {
        this.clearCursor();
        if (overPanel || this.menu() == null || this.moving()) {
            return;
        }
        double[] mouse = Picking.mouseGui();
        List<StonecutterRecipe> shapes = this.shapes();
        for (int k = 0; k < shapes.size(); ++k) {
            if (!(this.onDial(k) > 0.5) || !GrindstoneStation.under(mouse, this.shapePoint(k), 0.075 * (double)this.geometry().scale(2))) continue;
            this.target = Target.SHAPE;
            this.hoverShape = k;
            this.aim = this.shapePoint(k).m_82520_(0.0, -0.1, 0.0).m_82549_(this.side().m_82490_(0.12));
            return;
        }
        ItemStack carried = Hands.carried();
        if (!this.geometry().equals(SpatialAppearance.VANILLA)) {
            if (this.layoutPick() == 0) {
                this.target = carried.m_41619_() ? (this.slot(0).m_41619_() ? Target.NONE : Target.STACK) : Target.PLACE;
                this.aim = this.restPoint(0);
            }
            return;
        }
        if (carried.m_41619_() && !this.slot(0).m_41619_() && GrindstoneStation.under(mouse, this.restPoint(0).m_82520_(0.0, 0.08, 0.0), 0.12)) {
            this.target = Target.STACK;
            this.aim = this.restPoint(0).m_82520_(0.0, 0.12, 0.0);
            return;
        }
        Vec3 hit = Picking.cursorOnPlane(this.top.f_82480_);
        if (!carried.m_41619_() && hit != null && Math.abs(hit.m_82546_(this.top).m_82526_(this.right())) < 0.52 && Math.abs(hit.m_82546_(this.top).m_82526_(this.side())) < 0.52) {
            this.target = Target.PLACE;
            this.aim = this.restPoint(0);
        }
    }

    @Override
    public void clearCursor() {
        super.clearCursor();
        this.target = Target.NONE;
        this.hoverShape = -1;
    }

    @Override
    public Vec3 handTarget(Vec3 aim, Vec3 shoulder, boolean carrying, double tap) {
        Vec3 reach = new Vec3(aim.f_82479_ - shoulder.f_82479_, 0.0, aim.f_82481_ - shoulder.f_82481_).m_82541_();
        double hover = carrying ? 0.075 : 0.045;
        return aim.m_82520_(0.0, 0.062 + hover - 0.06 * tap, 0.0).m_82546_(reach.m_82490_(0.07));
    }

    @Override
    public Vec3 heldItemPoint(Vec3 hand, Vec3 shoulder) {
        Vec3 reach = new Vec3(hand.f_82479_ - shoulder.f_82479_, 0.0, hand.f_82481_ - shoulder.f_82481_).m_82541_();
        return hand.m_82549_(reach.m_82490_(0.07)).m_82520_(0.0, -0.05, 0.0);
    }

    @Override
    protected void act(int button, boolean shift) {
        switch (this.target) {
            case PLACE: {
                this.slotClick(0, button, false);
                Scene.sound(SoundEvents.f_12447_, 0.6f, 1.1f);
                break;
            }
            case STACK: {
                this.slotClick(0, button, shift);
                break;
            }
            case SHAPE: {
                boolean room;
                ItemStack carried = Hands.carried();
                ItemStack result = this.shapeResult(this.hoverShape);
                boolean bl = room = carried.m_41619_() || ItemStack.m_150942_((ItemStack)carried, (ItemStack)result) && carried.m_41613_() + result.m_41613_() <= carried.m_41741_();
                if (button != 0 || result.m_41619_() || !room && !shift) {
                    this.deny();
                    return;
                }
                this.cut(this.hoverShape, shift);
                break;
            }
        }
    }

    private void cut(int k, boolean all) {
        StonecutterMenu menu = (StonecutterMenu)this.menu();
        Minecraft mc = Minecraft.m_91087_();
        if (menu == null || mc.f_91072_ == null || mc.f_91074_ == null) {
            return;
        }
        if (menu.m_40338_() != k) {
            menu.m_6366_((Player)mc.f_91074_, k);
            mc.f_91072_.m_105208_(menu.f_38840_, k);
            Scene.sound(SoundEvents.f_12495_, 0.6f, 1.0f);
        }
        this.input = this.slot(0).m_41777_();
        this.piece = this.slot(1).m_41619_() ? this.shapeResult(k) : this.slot(1).m_41777_();
        this.cutAll = all;
        this.length = all ? 1.5 : 1.05;
        this.startMove(this.length, 0.26, () -> {
            StonecutterMenu open = (StonecutterMenu)this.menu();
            if (this.slot(1).m_41619_()) {
                this.cancelMove();
                return;
            }
            mc.f_91072_.m_171799_(open.f_38840_, 1, 0, this.cutAll ? ClickType.QUICK_MOVE : ClickType.PICKUP, (Player)mc.f_91074_);
        });
        this.showMove(all);
    }

    @Override
    public void tick() {
        int count = this.shapes().size();
        if (count != this.lastShapes) {
            this.lastShapes = count;
            this.shapesAt = Scene.clock();
            this.scrollTarget = 0;
        }
        if (this.hoverShape != this.lastHover && this.hoverShape >= 0) {
            Scene.sound(SoundEvents.f_12446_, 0.3f, 1.6f + (float)(this.hoverShape % 3) * 0.1f);
        }
        this.lastHover = this.hoverShape;
    }

    static double outAt(boolean all) {
        return all ? 1.15 : 0.72;
    }

    private double along(double t) {
        return StonecutterStation.along(t, this.cutAll);
    }

    static double along(double t, boolean all) {
        return -0.46 + 0.76 * Anim.inOutSine(Anim.window(t, 0.22, StonecutterStation.outAt(all)));
    }

    static Vec3 piecePath(Vec3 top, Direction side, double t, boolean all) {
        if (t < 0.26) {
            Vec3 onStack = StonecutterStation.stackPoint(top, side).m_82520_(0.0, 0.11000000149011611, 0.0);
            Vec3 start = RemainingLayouts.transform(StationKind.STONECUTTER, top, side, 1, top.m_82549_(StonecutterStation.rightOf(side).m_82490_(-0.46)).m_82520_(0.0, (double)0.1f, 0.0));
            double k = Anim.inOutCubic(Anim.window(t, 0.06, 0.24000000000000002));
            return Anim.lerp(onStack, start, k).m_82520_(0.0, 0.07 * Anim.hump(k), 0.0);
        }
        return RemainingLayouts.transform(StationKind.STONECUTTER, top, side, 1, top.m_82549_(StonecutterStation.rightOf(side).m_82490_(StonecutterStation.along(t, all))).m_82520_(0.0, (double)0.1f, 0.0));
    }

    private Vec3 piecePos(double t, Camera camera) {
        double out = StonecutterStation.outAt(this.cutAll);
        Vec3 sliding = StonecutterStation.piecePath(this.top, this.side, Math.min(t, out), this.cutAll);
        if (t < out) {
            return sliding;
        }
        if (this.cutAll) {
            Vec3 drawer = camera.m_90583_().m_82520_(0.0, 0.5, 0.0).m_82549_(Scene.yawForward(camera.m_90590_()).m_82490_(0.6));
            return Anim.lerp(sliding, drawer, Anim.inCubic(Anim.window(t, out, this.length)));
        }
        Vec3 held = this.heldItemPoint(Hands.position(true), Hands.shoulder(true, camera.m_90583_(), camera.m_90590_()));
        double k = Anim.inOutCubic(Anim.window(t, out, this.length));
        return Anim.lerp(sliding, held, k).m_82520_(0.0, 0.06 * Anim.hump(k), 0.0);
    }

    private Vec3 catchPoint() {
        return RemainingLayouts.transform(this.kind(), this.top, this.side, 1, this.top.m_82549_(this.right().m_82490_(0.34)).m_82549_(this.side().m_82490_(0.08)).m_82520_(0.0, 0.18200000178813935, 0.0));
    }

    @Override
    @Nullable
    public Vec3[] handsOverride() {
        Vec3 left;
        double t = this.moveTime();
        if (t < 0.0) {
            return null;
        }
        double out = this.cutAll ? 1.15 : 0.72;
        Vec3 onStack = this.restPoint(0).m_82520_(0.0, 0.26200000298023224, 0.0).m_82549_(this.side().m_82490_(0.05));
        if (t < 0.08) {
            left = Anim.lerp(this.rest(false), onStack, Anim.inOutCubic(t / 0.08));
        } else {
            Vec3 start = RemainingLayouts.transform(this.kind(), this.top, this.side, 1, this.top.m_82549_(this.right().m_82490_(-0.63)).m_82520_(0.0, 0.17200000163912774, 0.0).m_82549_(this.side().m_82490_(0.05)));
            Vec3 path = t < 0.26 ? Anim.lerp(onStack, start, Anim.inOutCubic(Anim.window(t, 0.06, 0.24000000000000002))) : RemainingLayouts.transform(this.kind(), this.top, this.side, 1, this.top.m_82549_(this.right().m_82490_(this.along(t) - 0.17)).m_82520_(0.0, 0.17200000163912774, 0.0).m_82549_(this.side().m_82490_(0.05)));
            left = Anim.lerp(path, this.rest(false), Anim.inOutCubic(Anim.window(t, out - 0.05, this.length)));
        }
        Vec3 waiting = Anim.lerp(this.rest(true), this.catchPoint(), Anim.inOutCubic(Anim.window(t, 0.26, out - 0.05)));
        Vec3 end = this.rest(true).m_82520_(0.0, this.cutAll ? 0.0 : 0.05, 0.0);
        Vec3 right = Anim.lerp(waiting, end, Anim.inOutCubic(Anim.window(t, out, this.length)));
        return new Vec3[]{right, left};
    }

    @Override
    public double cameraKick() {
        double t = this.moveTime();
        if (t < 0.0) {
            return 0.0;
        }
        return this.cutting(t) ? 0.12 : 0.0;
    }

    private boolean cutting(double t) {
        return StonecutterStation.cutting(t, this.cutAll);
    }

    static boolean cutting(double t, boolean all) {
        return t >= 0.26 && Math.abs(StonecutterStation.along(t, all)) < 0.4375 && t < StonecutterStation.outAt(all);
    }

    @Override
    void frame() {
        super.frame();
        double t = this.moveTime();
        if (t >= 0.0) {
            StonecutterStation.cutFx(this.top, this.side, t, this.cutAll, this.input, Scene.frameDt);
        }
    }

    static void cutFx(Vec3 top, Direction side, double t, boolean all, ItemStack stone, double dt) {
        if (!StonecutterStation.cutting(t, all)) {
            return;
        }
        Vec3 out = Vec3.m_82528_((Vec3i)side.m_122436_());
        Vec3 bite = RemainingLayouts.transform(StationKind.STONECUTTER, top, side, 1, top.m_82549_(StonecutterStation.rightOf(side).m_82490_(StonecutterStation.along(t, all) + 0.06000000089406967)).m_82520_(0.0, 0.1700000025331974, 0.0));
        if (Fx.RANDOM.m_188500_() < dt * 45.0) {
            Fx.fountain(bite, out.m_82490_(-0.3), out, 1, 1.6);
        }
        StonecutterStation.dust(bite, StonecutterStation.rightOf(side).m_82490_(0.5), (int)Math.round(dt * (double)(all ? 110 : 75) + Fx.RANDOM.m_188500_()), stone);
    }

    static void dust(Vec3 at, Vec3 away, int count, ItemStack stone) {
        int n;
        Item item = stone.m_41720_();
        if (item instanceof BlockItem) {
            BlockItem block = (BlockItem)item;
            n = block.m_40614_().m_284356_().f_283871_;
        } else {
            n = 0x8A8A8A;
        }
        int rgb = n;
        float r = (float)(rgb >> 16 & 0xFF) / 255.0f;
        float g = (float)(rgb >> 8 & 0xFF) / 255.0f;
        float b = (float)(rgb & 0xFF) / 255.0f;
        for (int i = 0; i < count; ++i) {
            Vec3 dir = away.m_82520_(Fx.RANDOM.m_188583_() * 0.5, 0.8 + Fx.RANDOM.m_188500_() * 0.7, Fx.RANDOM.m_188583_() * 0.5).m_82541_();
            float k = 0.55f + Fx.RANDOM.m_188501_() * 0.45f;
            Fx.spark(at.m_82520_(Fx.RANDOM.m_188583_() * 0.015, 0.0, Fx.RANDOM.m_188583_() * 0.015), dir.m_82490_(0.6 + Fx.RANDOM.m_188500_() * 1.1), 0.3 + Fx.RANDOM.m_188500_() * 0.4, 0.005f + Fx.RANDOM.m_188501_() * 0.007f, r * k, g * k, b * k, 2.6, 0.012);
        }
    }

    @Override
    public void renderScene(PoseStack pose, MultiBufferSource buffers, Camera camera, int light) {
        Vec3 cam = camera.m_90583_();
        this.scroll = Anim.approach(this.scroll, this.scrollTarget, 10.0, Scene.frameDt);
        if (this.outro()) {
            this.drawLeaving(pose, buffers, camera, light);
            return;
        }
        double t = this.moveTime();
        ItemStack stack = this.slot(0);
        if (t >= 0.0 && t < 0.26 && !this.cutAll) {
            stack = this.input.m_255036_(Math.max(0, this.input.m_41613_() - 1));
        } else if (t >= 0.0 && this.cutAll) {
            stack = ItemStack.f_41583_;
        }
        StonecutterStation.drawStack(pose, buffers, cam, stack, this.restPoint(0), this.side, light);
        if (t >= 0.0) {
            this.drawPiece(pose, buffers, camera, t, light);
        }
    }

    private void drawPiece(PoseStack pose, MultiBufferSource buffers, Camera camera, double t, int light) {
        float size = 0.2f * RemainingLayouts.scale(StationKind.STONECUTTER, 1);
        double out = StonecutterStation.outAt(this.cutAll);
        if (t > out) {
            size *= this.cutAll ? (float)(1.0 - 0.85 * Anim.inCubic(Anim.window(t, out, this.length))) : (float)(1.0 - 0.3 * Anim.window(t, out, this.length));
        }
        StonecutterStation.drawPieceAt(pose, buffers, camera, this.top, this.side, this.piecePos(t, camera), t, this.input, this.piece, this.cutAll, size, light);
    }

    static void drawPieceAt(PoseStack pose, MultiBufferSource buffers, Camera camera, Vec3 top, Direction side, Vec3 at, double t, ItemStack input, ItemStack piece, boolean all, float size, int light) {
        Vec3 cam = camera.m_90583_();
        Vec3 right = StonecutterStation.rightOf(side);
        boolean cut = t >= 0.26 && StonecutterStation.along(t, all) > 0.0;
        ItemStack shown = cut ? piece : input.m_255036_(1);
        float yaw = ItemPlacer.awayYaw(side) + RemainingLayouts.rotation(StationKind.STONECUTTER, 1);
        boolean block = ItemPlacer.isBlock(shown);
        int copies = cut ? Math.min(3, Math.max(1, shown.m_41613_())) : (all ? Math.min(3, Math.max(1, input.m_41613_() / 8)) : 1);
        for (int i = 0; i < copies; ++i) {
            Vec3 offset = right.m_82490_(((double)i - (double)(copies - 1) / 2.0) * (double)size * 0.75).m_82520_(0.0, (double)i * 0.01, 0.0);
            Vec3 c = at.m_82549_(offset).m_82520_(0.0, block ? 0.0 : (double)(-size / 2.0f) + 0.006, 0.0);
            ItemPlacer.drawOriented(pose, buffers, cam, shown, c, block ? Axis.f_252436_.m_252977_(yaw + 180.0f) : ItemPlacer.lying(yaw, 0.0f), block ? size : size * 1.3f, light);
        }
        if (StonecutterStation.cutting(t, all)) {
            float b = (float)(0.45 + 0.2 * Math.sin(Scene.clock() * 50.0));
            Vec3 contact = RemainingLayouts.transform(StationKind.STONECUTTER, top, side, 1, top.m_82549_(right.m_82490_(StonecutterStation.along(t, all) + 0.06000000089406967)).m_82520_(0.0, 0.1600000023841858, 0.0));
            EnchantFx.billboard(pose, buffers, camera, contact, 0.04, 0.0, EnchantFx.GLOW, b, b * 0.8f, b * 0.55f);
        }
    }

    static void drawStack(PoseStack pose, MultiBufferSource buffers, Vec3 cam, ItemStack stack, Vec3 at, Direction side, int light) {
        if (stack.m_41619_()) {
            return;
        }
        float yaw = ItemPlacer.awayYaw(side) + RemainingLayouts.rotation(StationKind.STONECUTTER, 0);
        float itemScale = RemainingLayouts.scale(StationKind.STONECUTTER, 0);
        if (!ItemPlacer.isBlock(stack)) {
            ItemPlacer.render(pose, buffers, cam, stack, at, side, 31L, 1.1f * itemScale, RemainingLayouts.rotation(StationKind.STONECUTTER, 0), light);
            return;
        }
        float cubeSize = 0.2f * itemScale;
        int cubes = stack.m_41613_() >= 40 ? 3 : (stack.m_41613_() >= 12 ? 2 : 1);
        Vec3 right = SpatialLayouts.current(StationKind.STONECUTTER).axis(RemainingLayouts.frameAt(StationKind.STONECUTTER, Vec3.f_82478_, side), 0, true);
        Vec3 out = SpatialLayouts.current(StationKind.STONECUTTER).axis(RemainingLayouts.frameAt(StationKind.STONECUTTER, Vec3.f_82478_, side), 0, false);
        Vec3[] spots = new Vec3[]{at, at.m_82520_(0.0, (double)cubeSize, 0.0).m_82549_(right.m_82490_(0.015)), at.m_82549_(right.m_82490_((double)(-cubeSize) * 1.05)).m_82549_(out.m_82490_(0.03))};
        float[] turns = new float[]{6.0f, -9.0f, 14.0f};
        for (int i = 0; i < cubes; ++i) {
            ItemPlacer.drawOriented(pose, buffers, cam, stack, spots[i].m_82520_(0.0, (double)(cubeSize / 2.0f), 0.0), Axis.f_252436_.m_252977_(yaw + 180.0f + turns[i]), cubeSize, light);
        }
    }

    @Override
    public void renderWorld(PoseStack pose, MultiBufferSource buffers, Vec3 camera, int light) {
        this.gridEditor.renderWorld(pose, buffers, camera, light);
        Minecraft mc = Minecraft.m_91087_();
        Camera cam = mc.f_91063_.m_109153_();
        StonecutterMenu menu = (StonecutterMenu)this.menu();
        if (menu == null) {
            return;
        }
        double clock = Scene.clock();
        if (this.target == Target.PLACE && this.aim != null) {
            float p = (float)(0.16 + 0.06 * Math.sin(clock * 5.0));
            EnchantFx.disc(pose, buffers, camera, this.aim.m_82520_(0.0, 0.004, 0.0), 0.13, 0.0, EnchantFx.GLOW, p, p * 0.9f, p * 0.75f);
        }
        List<StonecutterRecipe> shapes = this.shapes();
        double appear = Anim.outBack(Anim.window(clock - this.shapesAt, 0.0, 0.4), 1.4);
        double dim = this.moving() ? 0.45 : 1.0;
        for (int k = 0; k < shapes.size(); ++k) {
            double on = this.onDial(k) * appear;
            if (on < 0.02) continue;
            ItemStack result = this.shapeResult(k);
            Vec3 at = this.shapePoint(k);
            boolean hovered = k == this.hoverShape;
            boolean selected = k == menu.m_40338_();
            float g = (float)((hovered ? 0.34 : 0.15) * on * dim);
            EnchantFx.billboard(pose, buffers, cam, at, (hovered ? 0.095 : 0.075) * on, 0.0, EnchantFx.GLOW, g, g * 0.95f, g * 0.85f);
            if (selected) {
                float s = (float)(0.4 * on * dim);
                EnchantFx.billboard(pose, buffers, cam, at, 0.085 * on, clock * 0.8, EnchantFx.CIRCLE, s, s * 0.85f, s * 0.4f);
            }
            float size = (float)((double)(0.15f * this.geometry().scale(2)) * on * (hovered ? 1.35 : 1.0));
            float spin = (float)(clock * (double)(hovered ? 90 : 25) + (double)(k * 40) + (double)RemainingLayouts.rotation(this.kind(), 2));
            boolean block = ItemPlacer.isBlock(result);
            ItemPlacer.drawOriented(pose, buffers, camera, result, at, Axis.f_252436_.m_252977_(spin), block ? size * 0.85f : size * 1.2f, 0xF000F0);
            if (result.m_41613_() <= 1) continue;
            StonecutterStation.drawCount(pose, buffers, cam, "\u00d7" + result.m_41613_(), at.m_82520_(0.0, (double)(-size) * 0.85 - 0.02, 0.0), (float)on);
        }
        if (shapes.size() > 9) {
            if (this.scrollTarget > 0) {
                StonecutterStation.drawCount(pose, buffers, cam, "\u2039", this.shapeEnd(true), 1.0f);
            }
            if (this.scrollTarget < shapes.size() - 9) {
                StonecutterStation.drawCount(pose, buffers, cam, "\u203a", this.shapeEnd(false), 1.0f);
            }
        }
    }

    private Vec3 shapeEnd(boolean left) {
        double a = Math.toRadians(left ? 176.0 : 4.0);
        return RemainingLayouts.transform(this.kind(), this.top, this.side, 2, this.top.m_82549_(this.side().m_82490_(-0.06)).m_82549_(this.right().m_82490_(Math.cos(a) * 0.72)).m_82520_(0.0, 0.07 + Math.sin(a) * 0.5, 0.0));
    }

    private static void drawCount(PoseStack pose, MultiBufferSource buffers, Camera camera, String text, Vec3 at, float alpha) {
        Font font = Minecraft.m_91087_().f_91062_;
        Vec3 cam = camera.m_90583_();
        pose.m_85836_();
        pose.m_85837_(at.f_82479_ - cam.f_82479_, at.f_82480_ - cam.f_82480_, at.f_82481_ - cam.f_82481_);
        pose.m_252781_(camera.m_253121_());
        float s = 0.0065f;
        pose.m_85841_(-s, -s, s);
        int a = Math.max(8, (int)(alpha * 255.0f));
        font.m_271703_(text, (float)(-font.m_92895_(text)) / 2.0f, -4.0f, a << 24 | 0xF2EAD8, false, pose.m_85850_().m_252922_(), buffers, Font.DisplayMode.NORMAL, 0, 0xF000F0);
        pose.m_85849_();
    }

    static void drawContents(PoseStack pose, MultiBufferSource buffers, Vec3 cam, BlockPos pos, Direction side, List<ItemStack> items, int light) {
        if (!items.isEmpty()) {
            StonecutterStation.drawStack(pose, buffers, cam, items.get(0), StonecutterStation.stackPoint(StonecutterStation.topOf(pos), side), side, light);
        }
    }

    static void drawOnlooker(PoseStack pose, MultiBufferSource buffers, Camera camera, BlockPos pos, ClientMenus.Entry entry, double t, int light) {
        Vec3 top = StonecutterStation.topOf(pos);
        boolean all = entry.moveToBag;
        ItemStack input = entry.moved(0);
        StonecutterStation.drawStack(pose, buffers, camera.m_90583_(), all ? ItemStack.f_41583_ : input.m_255036_(Math.max(0, input.m_41613_() - 1)), StonecutterStation.stackPoint(top, entry.side), entry.side, light);
        double out = StonecutterStation.outAt(all);
        Vec3 at = StonecutterStation.piecePath(top, entry.side, Math.min(t, out), all);
        float size = 0.2f * RemainingLayouts.scale(StationKind.STONECUTTER, 1);
        if (t > out) {
            double away = Anim.inCubic(Anim.window(t, out, all ? 1.5 : 1.05));
            at = Anim.lerp(at, entry.taker(at.m_82549_(Vec3.m_82528_((Vec3i)entry.side.m_122436_()).m_82490_(0.6))), away);
            size *= (float)(1.0 - 0.8 * away);
        }
        StonecutterStation.drawPieceAt(pose, buffers, camera, top, entry.side, at, t, input, entry.moveResult, all, size, light);
    }

    static void onlookerFx(BlockPos pos, ClientMenus.Entry entry, double t, double dt) {
        if (t >= 0.0) {
            StonecutterStation.cutFx(StonecutterStation.topOf(pos), entry.side, t, entry.moveToBag, entry.moved(0), dt);
        }
    }

    public static float[] armTurns(double t) {
        double push = Anim.window(StonecutterStation.along(t, false), -0.46, 0.3);
        double reach = Anim.smoother(Anim.window(t, 0.26, StonecutterStation.outAt(false))) * (1.0 - Anim.smoother(Anim.window(t, StonecutterStation.outAt(false), 1.05)));
        return new float[]{(float)(-0.25 + 0.6 * push), (float)(-0.12 + 0.36 * reach)};
    }

    @Override
    @Nullable
    public List<Component> tooltip(ItemStack carried) {
        double[] pointer = Picking.mouseGui();
        List<Component> editorTip = this.gridEditor.tooltip(pointer[0], pointer[1]);
        if (editorTip != null) {
            return editorTip;
        }
        Minecraft mc = Minecraft.m_91087_();
        switch (this.target) {
            case SHAPE: {
                ItemStack result = this.shapeResult(this.hoverShape);
                ArrayList<Component> lines = new ArrayList<Component>(Screen.m_280152_((Minecraft)mc, (ItemStack)result));
                if (result.m_41613_() > 1) {
                    lines.set(0, (Component)((Component)lines.get(0)).m_6881_().m_130946_(" \u00d7" + result.m_41613_()));
                }
                boolean room = carried.m_41619_() || ItemStack.m_150942_((ItemStack)carried, (ItemStack)result) && carried.m_41613_() + result.m_41613_() <= carried.m_41741_();
                lines.add((Component)(room ? Component.m_237115_((String)"cinematic.stonecutter.cut").m_130940_(ChatFormatting.GOLD) : Component.m_237115_((String)"cinematic.stonecutter.hands_full").m_130940_(ChatFormatting.RED)));
                return lines;
            }
            case STACK: {
                ArrayList<Component> lines = new ArrayList<Component>(Screen.m_280152_((Minecraft)mc, (ItemStack)this.slot(0)));
                lines.set(0, (Component)((Component)lines.get(0)).m_6881_().m_130946_(" \u00d7" + this.slot(0).m_41613_()));
                if (this.shapes().isEmpty()) {
                    lines.add((Component)Component.m_237115_((String)"cinematic.stonecutter.no_shapes").m_130940_(ChatFormatting.RED));
                }
                lines.add((Component)Component.m_237115_((String)"cinematic.stonecutter.take").m_130940_(ChatFormatting.GRAY));
                return lines;
            }
        }
        return null;
    }

    @Override
    public Component hint() {
        return Component.m_237115_((String)"cinematic.hint.stonecutter");
    }

    @Override
    public void beforeFrame() {
        this.gridEditor.frame();
    }

    @Override
    public void onEnd() {
        this.gridEditor.cancel();
        super.onEnd();
    }

    @Override
    public boolean busy() {
        return super.busy() || this.gridEditor.editing();
    }

    @Override
    public boolean showsInventory() {
        return !this.gridEditor.visible();
    }

    @Override
    public void renderScreen(GuiGraphics g, Font font, float alpha) {
        this.gridEditor.render(g, font, alpha);
    }

    @Override
    public List<Vec3> workSamples() {
        return RemainingLayouts.samples(this, super.workSamples());
    }

    @Override
    public float layoutHeldScale() {
        return this.geometry().scale(this.target == Target.SHAPE ? 1 : 0);
    }

    public static enum Target {
        NONE,
        PLACE,
        STACK,
        SHAPE;

    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  javax.annotation.Nullable
 *  net.minecraft.ChatFormatting
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.client.renderer.LevelRenderer
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.client.renderer.texture.TextureAtlasSprite
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Vec3i
 *  net.minecraft.network.chat.Component
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.ClickType
 *  net.minecraft.world.inventory.GrindstoneMenu
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.enchantment.Enchantment
 *  net.minecraft.world.item.enchantment.EnchantmentHelper
 *  net.minecraft.world.level.BlockAndTintGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.GrindstoneBlock
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.AttachFace
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.Vec3
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dyslexic.cinematic.client.Anim;
import dev.dyslexic.cinematic.client.CameraRig;
import dev.dyslexic.cinematic.client.ClientMenus;
import dev.dyslexic.cinematic.client.EnchantFx;
import dev.dyslexic.cinematic.client.Fx;
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
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;

public final class GrindstoneStation
extends MenuStation<GrindstoneMenu>
implements SpatialTarget {
    final SpatialEditor gridEditor = new SpatialEditor(this);
    static final double HALF = 0.375;
    static final double WIDTH = 0.25;
    static final double HAND_Y = 0.062;
    static final double ITEM_REACH = 0.07;
    static final float ITEM_SIZE = 0.3f;
    static final float TOOL_TURN = -45.0f;
    static final double SLOT_SIDE = 0.1;
    static final float SLANT = 25.0f;
    public static final double GRIND_LENGTH = 2.4;
    static final double CONTACT = 0.6;
    static final double STROKE = 0.433;
    static final double CLEAN_AT = 1.5;
    static final double LET_GO = 1.9;
    public final Vec3 wheel;
    public final Vec3 top;
    public Target target = Target.NONE;
    public int hoverSlot = -1;
    private int placeSlot = -1;
    private double spin;
    private double run;
    private final ItemStack[] before = new ItemStack[]{ItemStack.f_41583_, ItemStack.f_41583_};
    private ItemStack after = ItemStack.f_41583_;
    private boolean toInventory;
    private boolean stripping;

    @Override
    public SpatialEditor layoutEditor() {
        return this.gridEditor;
    }

    public GrindstoneStation(BlockPos pos, Direction side, BlockState state) {
        super(pos, side, GrindstoneMenu.class, 2);
        this.wheel = GrindstoneStation.wheelCenter(pos, state);
        this.top = this.wheel.m_82520_(0.0, 0.375, 0.0);
    }

    @Override
    public StationKind kind() {
        return StationKind.GRINDSTONE;
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
        return this.pos;
    }

    static Vec3 wheelCenter(BlockPos pos, BlockState state) {
        Vec3 c = Vec3.m_82512_((Vec3i)pos);
        return switch ((AttachFace)state.m_61143_((Property)GrindstoneBlock.f_53179_)) {
            default -> throw new IncompatibleClassChangeError();
            case AttachFace.FLOOR -> c.m_82520_(0.0, 0.125, 0.0);
            case AttachFace.CEILING -> c.m_82520_(0.0, -0.125, 0.0);
            case AttachFace.WALL -> c.m_82549_(Vec3.m_82528_((Vec3i)((Direction)state.m_61143_((Property)GrindstoneBlock.f_54117_)).m_122436_()).m_82490_(0.125));
        };
    }

    static Vec3 rightOf(Direction side) {
        return Vec3.m_82528_((Vec3i)side.m_122424_().m_122427_().m_122436_());
    }

    static Vec3 restPoint(Vec3 wheel, Direction side, int slot) {
        return RemainingLayouts.point(StationKind.GRINDSTONE, wheel.m_82520_(0.0, 0.375, 0.0), side, slot);
    }

    @Override
    protected Vec3 restPoint(int slot) {
        return GrindstoneStation.restPoint(this.wheel, this.side, slot).m_82549_(this.right().m_82490_(this.denyShake()));
    }

    @Override
    public List<Vec3> approach(LocalPlayer player) {
        Vec3 stand = new Vec3(this.wheel.f_82479_ + (double)this.side.m_122429_() * 0.9, player.m_20186_(), this.wheel.f_82481_ + (double)this.side.m_122431_() * 0.9);
        boolean walk = Math.hypot(stand.f_82479_ - player.m_20185_(), stand.f_82481_ - player.m_20189_()) > 0.06;
        return walk ? List.of(stand) : List.of();
    }

    @Override
    public boolean worksAtAnAngle() {
        return true;
    }

    @Override
    public CameraRig.Pose lean() {
        Vec3 eye = this.wheel.m_82549_(this.side().m_82490_(0.95)).m_82520_(0.0, 1.0, 0.0);
        Vec3 look = this.wheel.m_82520_(0.0, 0.32, 0.0).m_82546_(eye);
        float yaw = (float)Math.toDegrees(Math.atan2(-look.f_82479_, look.f_82481_));
        float pitch = (float)(-Math.toDegrees(Math.atan2(look.f_82480_, Math.hypot(look.f_82479_, look.f_82481_))));
        return new CameraRig.Pose(eye, yaw, pitch, -1.5f);
    }

    @Override
    public double leanFov() {
        return 64.0;
    }

    @Override
    public float bodyPitch() {
        return 40.0f;
    }

    @Override
    public Vec3 rest(boolean right) {
        return this.wheel.m_82549_(this.right().m_82490_(right ? 0.31 : -0.31)).m_82549_(this.side().m_82490_(0.1)).m_82520_(0.0, 0.2495, 0.0);
    }

    @Override
    public void updateCursor(boolean overPanel) {
        this.clearCursor();
        GrindstoneMenu menu = (GrindstoneMenu)this.menu();
        if (overPanel || menu == null || this.moving()) {
            return;
        }
        ItemStack carried = Hands.carried();
        if (!this.geometry().equals(SpatialAppearance.VANILLA)) {
            int picked = this.layoutPick();
            if (picked >= 0 && picked < 2) {
                if (!carried.m_41619_()) {
                    this.target = Target.PLACE;
                    this.placeSlot = this.slotFor(carried, picked);
                    this.aim = this.restPoint(picked);
                } else if (!this.slot(picked).m_41619_()) {
                    this.target = Target.ITEM;
                    this.hoverSlot = picked;
                    this.aim = this.restPoint(picked);
                }
            }
            return;
        }
        double[] mouse = Picking.mouseGui();
        if (carried.m_41619_()) {
            for (int i = 0; i < 2; ++i) {
                if (this.slot(i).m_41619_() || !GrindstoneStation.under(mouse, this.restPoint(i), 0.13)) continue;
                this.target = Target.ITEM;
                this.hoverSlot = i;
                this.aim = this.restPoint(i);
                return;
            }
            return;
        }
        if (this.overWheel()) {
            this.target = Target.PLACE;
            this.placeSlot = this.slotFor(carried, this.slot(0).m_41619_() || !this.slot(1).m_41619_() ? 0 : 1);
            this.aim = this.restPoint(this.placeSlot < 0 ? 0 : this.placeSlot);
        }
    }

    private boolean overWheel() {
        Vec3 rel;
        Vec3 hit = Picking.cursorOnPlane(this.top.f_82480_);
        if (hit != null && Math.abs((rel = hit.m_82546_(this.top)).m_82526_(this.right())) < 0.33 && Math.abs(rel.m_82526_(this.side())) < 0.435) {
            return true;
        }
        Vec3 front = this.wheel.m_82549_(this.side().m_82490_(0.375));
        hit = Picking.cursorOnPlane(front, this.side());
        return hit != null && Math.abs(hit.m_82546_(front).m_82526_(this.right())) < 0.33 && Math.abs(hit.f_82480_ - this.wheel.f_82480_) < 0.375;
    }

    static boolean under(double[] mouse, Vec3 center, double radius) {
        double[] at = Picking.toGui(center);
        double[] edge = Picking.toGui(center.m_82520_(0.0, radius, 0.0));
        if (at == null || edge == null) {
            return false;
        }
        double r = Math.max(9.0, Math.hypot(at[0] - edge[0], at[1] - edge[1]));
        return Math.hypot(mouse[0] - at[0], mouse[1] - at[1]) < r;
    }

    @Override
    public void clearCursor() {
        super.clearCursor();
        this.target = Target.NONE;
        this.hoverSlot = -1;
        this.placeSlot = -1;
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
                if (this.placeSlot < 0) {
                    this.deny();
                    return;
                }
                this.slotClick(this.placeSlot, button, false);
                Scene.sound(SoundEvents.f_12447_, 0.5f, 1.3f);
                break;
            }
            case ITEM: {
                if (button == 0 && !this.slot(2).m_41619_()) {
                    this.grind(shift);
                    break;
                }
                this.slotClick(this.hoverSlot, 0, shift);
                break;
            }
        }
    }

    private void grind(boolean shift) {
        this.before[0] = this.slot(0).m_41777_();
        this.before[1] = this.slot(1).m_41777_();
        this.after = this.slot(2).m_41777_();
        this.toInventory = shift;
        this.stripping = GrindstoneStation.removesEnchantments(this.before[0]) || GrindstoneStation.removesEnchantments(this.before[1]);
        this.startMove(2.4, 0.6, () -> {
            if (this.slot(2).m_41619_()) {
                this.cancelMove();
                return;
            }
            GrindstoneMenu menu = (GrindstoneMenu)this.menu();
            Minecraft mc = Minecraft.m_91087_();
            mc.f_91072_.m_171799_(menu.f_38840_, 2, 0, this.toInventory ? ClickType.QUICK_MOVE : ClickType.PICKUP, (Player)mc.f_91074_);
        });
        this.showMove(shift);
        Scene.sound(SoundEvents.f_12446_, 0.5f, 0.8f);
    }

    static boolean removesEnchantments(ItemStack stack) {
        for (Enchantment enchantment : EnchantmentHelper.m_44831_((ItemStack)stack).keySet()) {
            if (enchantment.m_6589_()) continue;
            return true;
        }
        return false;
    }

    static double stroke(double t) {
        double s = Anim.window(t, 0.6, 1.9);
        double fade = Anim.smoother(s * 8.0) * Anim.smoother((1.0 - s) * 8.0);
        return 0.075 * Math.sin(Math.PI * (t - 0.6) / 0.433) * fade;
    }

    private Vec3 blade(int k) {
        return GrindstoneStation.blade(this.side, k);
    }

    static Vec3 blade(Direction side, int k) {
        double a = Math.toRadians(25.0);
        return Vec3.m_82528_((Vec3i)side.m_122436_()).m_82490_(-Math.cos(a)).m_82549_(GrindstoneStation.rightOf(side).m_82490_(k == 0 ? -Math.sin(a) : Math.sin(a))).m_82524_((float)Math.toRadians(RemainingLayouts.rotation(StationKind.GRINDSTONE, k)));
    }

    static Vec3 bite(Vec3 wheel, Direction side, int k, double t) {
        Vec3 at = GrindstoneStation.heldPos(wheel, side, k, t).m_82549_(GrindstoneStation.blade(side, k).m_82490_(0.07));
        return new Vec3(at.f_82479_, RemainingLayouts.point((StationKind)StationKind.GRINDSTONE, (Vec3)wheel.m_82520_((double)0.0, (double)0.375, (double)0.0), (Direction)side, (int)2).f_82480_ - 0.026, at.f_82481_);
    }

    private Vec3 heldPos(int k, double t) {
        return GrindstoneStation.heldPos(this.wheel, this.side, k, t);
    }

    static Vec3 heldPos(Vec3 wheel, Direction side, int k, double t) {
        double sideways = (k == 0 ? 0.08 : -0.08) + GrindstoneStation.stroke(t) * (double)(k == 0 ? 1 : -1);
        SpatialAppearance layout = SpatialLayouts.current(StationKind.GRINDSTONE);
        SpatialAppearance.Frame frame = RemainingLayouts.frameAt(StationKind.GRINDSTONE, wheel.m_82520_(0.0, 0.375, 0.0), side);
        Vec3 grinding = layout.point(frame, 2).m_82549_(layout.axis(frame, 2, true).m_82490_(sideways));
        Vec3 lying = GrindstoneStation.restPoint(wheel, side, k).m_82520_(0.0, 0.008, 0.0);
        double lift = Anim.inOutCubic(Anim.window(t, 0.28, 0.6));
        Vec3 at = Anim.lerp(lying, grinding, lift).m_82520_(0.0, 0.09 * Anim.hump(lift), 0.0);
        return at.m_82520_(0.0, -0.008 * Math.abs(Math.sin(Math.PI * (t - 0.6) / 0.433)) * Anim.window(t, 0.6, 0.7), 0.0);
    }

    static float heldYaw(Direction side, int k, double t) {
        double lift = Anim.inOutCubic(Anim.window(t, 0.28, 0.6));
        float lying = k == 0 ? -8.0f : 8.0f;
        float slant = k == 0 ? 25.0f : -25.0f;
        return ItemPlacer.awayYaw(side) + -45.0f + RemainingLayouts.rotation(StationKind.GRINDSTONE, k) + (float)((double)lying + (double)(slant - lying) * lift);
    }

    static float heldTilt(double t) {
        return (float)(-9.0 * Anim.inOutCubic(Anim.window(t, 0.28, 0.6)));
    }

    private Vec3 handle(int k, Vec3 item) {
        return item.m_82546_(this.blade(k).m_82490_(0.13 * (double)this.geometry().scale(k))).m_82520_(0.0, 0.0372, 0.0);
    }

    private Vec3 lifted() {
        return GrindstoneStation.lifted(this.wheel, this.side);
    }

    static Vec3 lifted(Vec3 wheel, Direction side) {
        return wheel.m_82520_(0.0, 0.375, 0.0).m_82549_(Vec3.m_82528_((Vec3i)side.m_122436_()).m_82490_(0.3)).m_82549_(GrindstoneStation.rightOf(side).m_82490_(0.06)).m_82520_(0.0, 0.25, 0.0);
    }

    private Vec3 inspectPos(double t, Camera camera) {
        double up = Anim.inOutCubic(Anim.window(t, 1.9, 2.1999999999999997));
        Vec3 at = Anim.lerp(this.heldPos(0, 1.9), this.lifted(), up);
        if (this.toInventory) {
            Vec3 drawer = camera.m_90583_().m_82520_(0.0, 0.55, 0.0).m_82549_(Scene.yawForward(camera.m_90590_()).m_82490_(0.6));
            return Anim.lerp(at, drawer, Anim.inCubic(Anim.window(t, 2.12, 2.4)));
        }
        Vec3 hand = Hands.position(true);
        Vec3 held = this.heldItemPoint(hand, Hands.shoulder(true, camera.m_90583_(), camera.m_90590_()));
        return Anim.lerp(at, held, Anim.inOutCubic(Anim.window(t, 2.1999999999999997, 2.4)));
    }

    @Override
    @Nullable
    public Vec3[] handsOverride() {
        Vec3 left;
        Vec3 grip;
        double t = this.moveTime();
        if (t < 0.0) {
            return null;
        }
        if (t < 1.9) {
            grip = Anim.lerp(this.rest(true), this.handle(0, this.heldPos(0, t)), Anim.inOutCubic(Anim.window(t, 0.0, 0.28)));
        } else {
            Vec3 up = Anim.lerp(this.handle(0, this.heldPos(0, 1.9)), this.lifted().m_82549_(this.side().m_82490_(0.1)).m_82520_(0.0, 0.0372, 0.0), Anim.inOutCubic(Anim.window(t, 1.9, 2.1999999999999997)));
            Vec3 end = this.rest(true).m_82520_(0.0, this.toInventory ? 0.0 : 0.05, 0.0);
            grip = Anim.lerp(up, end, Anim.inOutCubic(Anim.window(t, 2.1999999999999997, 2.4)));
        }
        if (!this.before[1].m_41619_()) {
            double hold = Anim.inOutCubic(Anim.window(t, 0.05, 0.33)) * (1.0 - Anim.inOutCubic(Anim.window(t, 1.5, 1.8)));
            left = Anim.lerp(this.rest(false), this.handle(1, this.heldPos(1, Math.min(t, 1.5))), hold);
        } else {
            double bear = Math.abs(Math.sin(Math.PI * (t - 0.6) / 0.433)) * Anim.window(t, 0.6, 0.7) * (1.0 - Anim.window(t, 1.7999999999999998, 1.9));
            left = this.rest(false).m_82520_(0.0, -0.008 * bear, 0.0).m_82549_(this.side().m_82490_(-0.02 * Anim.window(t, 0.1, 0.4)));
        }
        return new Vec3[]{grip, left};
    }

    @Override
    public double cameraKick() {
        double t = this.moveTime();
        if (t < 0.0) {
            return 0.0;
        }
        return 0.13 * Anim.window(t, 0.5499999999999999, 0.75) * (1.0 - Anim.window(t, 1.75, 2.0));
    }

    @Override
    public void tick() {
        double t = this.moveTime();
        if (t >= 0.6 && t < 1.9 && Minecraft.m_91087_().f_91074_.f_19797_ % 9 == 0) {
            Scene.sound(SoundEvents.f_11998_, 0.25f, 1.25f + Fx.RANDOM.m_188501_() * 0.2f);
        }
    }

    @Override
    void frame() {
        super.frame();
        double t = this.moveTime();
        double dt = Scene.frameDt;
        boolean driven = t >= 0.0 && t < 2.0;
        this.spin = driven ? Anim.approach(this.spin, 1.0, 5.0, dt) : Anim.approach(this.spin, 0.0, 1.3, dt);
        this.run -= this.spin * 70.0 * dt;
        if (t >= 0.0) {
            GrindstoneStation.grindFx(this.wheel, this.side, !this.before[1].m_41619_(), this.stripping, t, dt);
        }
    }

    static void grindFx(Vec3 wheel, Direction side, boolean two, boolean stripping, double t, double dt) {
        if (t < 0.6 || t >= 1.9) {
            return;
        }
        int count = (int)Math.round(dt * 160.0 + Fx.RANDOM.m_188500_());
        Vec3 lean = Vec3.m_82528_((Vec3i)side.m_122436_()).m_82490_(-0.55);
        Fx.fountain(GrindstoneStation.bite(wheel, side, 0, t), lean, GrindstoneStation.rightOf(side), count, 2.2);
        if (two && t < 1.5) {
            Fx.fountain(GrindstoneStation.bite(wheel, side, 1, t), lean, GrindstoneStation.rightOf(side), count, 2.2);
        }
        if (stripping && t < 1.6) {
            Vec3 steel = GrindstoneStation.heldPos(wheel, side, 0, t).m_82549_(GrindstoneStation.blade(side, 0).m_82490_(0.04)).m_82520_(0.0, 0.02, 0.0);
            Fx.flakes(steel, 0.06, (int)Math.round(dt * 34.0 + Fx.RANDOM.m_188500_() * 0.6));
        }
        if (stripping && t - dt < 1.5 && t >= 1.5) {
            Vec3 at = GrindstoneStation.heldPos(wheel, side, 0, t);
            Fx.flakes(at, 0.1, 30);
            Fx.motes(at, 0.06, 12, 0.85f, 0.55f, 1.0f, 0.2);
            GrindstoneStation.sound(at, SoundEvents.f_11738_, 0.35f, 1.7f);
            GrindstoneStation.sound(at, SoundEvents.f_144050_, 0.4f, 1.4f);
        }
    }

    private static void sound(Vec3 at, SoundEvent sound, float volume, float pitch) {
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91073_ != null) {
            mc.f_91073_.m_7785_(at.f_82479_, at.f_82480_, at.f_82481_, sound, SoundSource.BLOCKS, volume, pitch, false);
        }
    }

    @Override
    public void renderScene(PoseStack pose, MultiBufferSource buffers, Camera camera, int light) {
        Vec3 cam = camera.m_90583_();
        if (this.spin > 0.01) {
            GrindstoneStation.drawRim(pose, buffers, cam, this.wheel, this.side, this.run, LevelRenderer.m_109541_((BlockAndTintGetter)Minecraft.m_91087_().f_91073_, (BlockPos)this.pos));
        }
        if (this.outro()) {
            this.drawLeaving(pose, buffers, camera, light);
            return;
        }
        double t = this.moveTime();
        for (int i = 0; i < 2; ++i) {
            ItemStack stack;
            if (t >= 0.0 && !this.before[i].m_41619_() || (stack = this.slot(i)).m_41619_()) continue;
            GrindstoneStation.drawLying(pose, buffers, cam, stack, this.restPoint(i), this.side, i, i == this.hoverSlot ? 1.08f : 1.0f, light);
        }
        if (t >= 0.0) {
            this.drawMove(pose, buffers, camera, t, light);
        }
    }

    private void drawMove(PoseStack pose, MultiBufferSource buffers, Camera camera, double t, int light) {
        if (t < 1.9) {
            GrindstoneStation.drawGrinding(pose, buffers, camera, this.wheel, this.side, this.before[0], this.before[1], this.after, this.stripping, t, light);
            return;
        }
        double k = Anim.window(t, 1.9, 2.4);
        float size = (float)(this.toInventory ? (double)0.3f * (1.0 - 0.8 * Anim.inCubic(Anim.window(k, 0.4, 1.0))) : (double)0.3f + -0.0950000137090683 * Anim.window(k, 0.6, 1.0));
        GrindstoneStation.drawLifted(pose, buffers, camera.m_90583_(), this.side, this.after, this.inspectPos(t, camera), t, size, light);
    }

    static void drawGrinding(PoseStack pose, MultiBufferSource buffers, Camera camera, Vec3 wheel, Direction side, ItemStack first, ItemStack second, ItemStack after, boolean stripping, double t, int light) {
        Vec3 cam = camera.m_90583_();
        boolean clean = !stripping || t >= 1.5;
        ItemStack shown = clean || !second.m_41619_() && t >= 1.5 ? after : first;
        Vec3 at = GrindstoneStation.heldPos(wheel, side, 0, t);
        ItemPlacer.drawOriented(pose, buffers, cam, shown, at, ItemPlacer.lying(GrindstoneStation.heldYaw(side, 0, t), GrindstoneStation.heldTilt(t)), 0.3f * RemainingLayouts.scale(StationKind.GRINDSTONE, 0), light);
        if (stripping && t >= 0.5 && t < 1.55) {
            double left = 1.0 - Anim.window(t, 0.6, 1.5);
            float g = (float)(0.35 * left);
            EnchantFx.billboard(pose, buffers, camera, at.m_82520_(0.0, 0.02, 0.0), 0.17, Scene.clock() * 0.6, EnchantFx.GLOW, 0.65f * g, 0.35f * g, g);
        }
        if (!second.m_41619_() && t < 1.5) {
            ItemPlacer.drawOriented(pose, buffers, cam, second, GrindstoneStation.heldPos(wheel, side, 1, t), ItemPlacer.lying(GrindstoneStation.heldYaw(side, 1, t), GrindstoneStation.heldTilt(t)), 0.3f * RemainingLayouts.scale(StationKind.GRINDSTONE, 1), light);
        } else if (!second.m_41619_() && t < 1.75) {
            double k = Anim.inCubic(Anim.window(t, 1.5, 1.75));
            Vec3 from = GrindstoneStation.heldPos(wheel, side, 1, 1.5);
            ItemPlacer.drawOriented(pose, buffers, cam, second, Anim.lerp(from, GrindstoneStation.heldPos(wheel, side, 0, t), k), ItemPlacer.lying(GrindstoneStation.heldYaw(side, 1, t), GrindstoneStation.heldTilt(t)), (float)((double)(0.3f * RemainingLayouts.scale(StationKind.GRINDSTONE, 1)) * (1.0 - k)), light);
        }
        if (t >= 0.6 && t < 1.9) {
            float b = (float)(0.55 + 0.25 * Math.sin(Scene.clock() * 40.0));
            EnchantFx.billboard(pose, buffers, camera, GrindstoneStation.bite(wheel, side, 0, t).m_82520_(0.0, 0.008, 0.0), 0.05, 0.0, EnchantFx.GLOW, b, b * 0.6f, b * 0.25f);
        }
    }

    static void drawLifted(PoseStack pose, MultiBufferSource buffers, Vec3 cam, Direction side, ItemStack after, Vec3 at, double t, float size, int light) {
        double k = Anim.window(t, 1.9, 2.4);
        float tilt = (float)((double)GrindstoneStation.heldTilt(1.9) + (double)(60.0f - GrindstoneStation.heldTilt(1.9)) * Anim.hump(Math.min(1.0, k * 1.25)) + 18.0 * Anim.window(k, 0.8, 1.0));
        float from = GrindstoneStation.heldYaw(side, 0, 1.9);
        float turn = (float)((double)from + (double)(ItemPlacer.awayYaw(side) - from) * Anim.inOutCubic(Anim.window(k, 0.5, 1.0)) + 25.0 * Math.sin(k * Math.PI));
        ItemPlacer.drawOriented(pose, buffers, cam, after, at, ItemPlacer.lying(turn, tilt), size *= RemainingLayouts.scale(StationKind.GRINDSTONE, 0), light);
    }

    static void drawLying(PoseStack pose, MultiBufferSource buffers, Vec3 cam, ItemStack stack, Vec3 at, Direction side, int slot, float scale, int light) {
        float yaw = ItemPlacer.awayYaw(side) + -45.0f + (slot == 0 ? -8.0f : 8.0f) + RemainingLayouts.rotation(StationKind.GRINDSTONE, slot);
        boolean block = ItemPlacer.isBlock(stack);
        float size = (block ? 0.16f : 0.3f) * scale * RemainingLayouts.scale(StationKind.GRINDSTONE, slot);
        Vec3 center = at.m_82520_(0.0, block ? (double)(size / 2.0f) : 0.004 + (double)slot * 0.003, 0.0);
        ItemPlacer.drawOriented(pose, buffers, cam, stack, center, ItemPlacer.lying(yaw, 0.0f), size, light);
    }

    static void drawRim(PoseStack pose, MultiBufferSource buffers, Vec3 cam, Vec3 wheel, Direction side, double texels, int light) {
        TextureAtlasSprite sprite = Fx.sprite("block/grindstone_round");
        VertexConsumer vc = Fx.atlas(buffers);
        double h = 0.3775;
        Vec3 up = new Vec3(0.0, 1.0, 0.0);
        Vec3 out = Vec3.m_82528_((Vec3i)side.m_122436_());
        Vec3 across = GrindstoneStation.rightOf(side).m_82490_(0.25);
        Vec3[][] faces = new Vec3[][]{{wheel.m_82549_(up.m_82490_(h)).m_82546_(out.m_82490_(0.375)), out, up}, {wheel.m_82549_(out.m_82490_(h)).m_82549_(up.m_82490_(0.375)), up.m_82490_(-1.0), out}, {wheel.m_82546_(up.m_82490_(h)).m_82549_(out.m_82490_(0.375)), out.m_82490_(-1.0), up.m_82490_(-1.0)}, {wheel.m_82546_(out.m_82490_(h)).m_82546_(up.m_82490_(0.375)), up, out.m_82490_(-1.0)}};
        double offset = (texels % 12.0 + 12.0) % 12.0;
        for (Vec3[] face : faces) {
            double split = (12.0 - offset) / 12.0 * 0.75;
            GrindstoneStation.rimQuad(pose, vc, cam, across, face[0], face[1], face[2], 0.0, split, offset, 12.0, sprite, light);
            GrindstoneStation.rimQuad(pose, vc, cam, across, face[0], face[1], face[2], split, 0.75, 0.0, offset, sprite, light);
        }
    }

    private static void rimQuad(PoseStack pose, VertexConsumer vc, Vec3 cam, Vec3 across, Vec3 start, Vec3 run, Vec3 normal, double from, double to, double v0, double v1, TextureAtlasSprite sprite, int light) {
        if (to - from < 1.0E-4) {
            return;
        }
        Vec3 a = start.m_82549_(run.m_82490_(from));
        Vec3 b = start.m_82549_(run.m_82490_(to));
        Vec3[] p = new Vec3[]{a.m_82546_(across), a.m_82549_(across), b.m_82549_(across), b.m_82546_(across)};
        float[][] uv = new float[][]{{0.0f, (float)v0}, {8.0f, (float)v0}, {8.0f, (float)v1}, {0.0f, (float)v1}};
        boolean flip = across.m_82537_(run).m_82526_(normal) < 0.0;
        for (int j = 0; j < 4; ++j) {
            int i = flip ? 3 - j : j;
            Vec3 q = p[i].m_82546_(cam);
            vc.m_252986_(pose.m_85850_().m_252922_(), (float)q.f_82479_, (float)q.f_82480_, (float)q.f_82481_).m_6122_(255, 255, 255, 255).m_7421_(Fx.u(sprite, uv[i][0] / 16.0f), Fx.v(sprite, uv[i][1] / 16.0f)).m_86008_(OverlayTexture.f_118083_).m_85969_(light).m_252939_(pose.m_85850_().m_252943_(), (float)normal.f_82479_, (float)normal.f_82480_, (float)normal.f_82481_).m_5752_();
        }
    }

    @Override
    public void renderWorld(PoseStack pose, MultiBufferSource buffers, Vec3 camera, int light) {
        this.gridEditor.renderWorld(pose, buffers, camera, light);
        if (this.target == Target.PLACE && this.aim != null) {
            float p = (float)(0.18 + 0.06 * Math.sin(Scene.clock() * 5.0));
            boolean ok = this.placeSlot >= 0;
            EnchantFx.disc(pose, buffers, camera, this.aim.m_82520_(0.0, 0.004, 0.0), 0.12, 0.0, EnchantFx.GLOW, p, ok ? p * 0.8f : p * 0.2f, ok ? p * 0.55f : p * 0.2f);
        }
    }

    static void drawContents(PoseStack pose, MultiBufferSource buffers, Vec3 cam, BlockPos pos, BlockState state, Direction side, List<ItemStack> items, int light) {
        Vec3 wheel = GrindstoneStation.wheelCenter(pos, state);
        for (int i = 0; i < Math.min(2, items.size()); ++i) {
            if (items.get(i).m_41619_()) continue;
            GrindstoneStation.drawLying(pose, buffers, cam, items.get(i), GrindstoneStation.restPoint(wheel, side, i), side, i, 1.0f, light);
        }
    }

    static void drawOnlooker(PoseStack pose, MultiBufferSource buffers, Camera camera, BlockPos pos, BlockState state, ClientMenus.Entry entry, double t, int light) {
        Vec3 wheel = GrindstoneStation.wheelCenter(pos, state);
        ItemStack first = entry.moved(0);
        ItemStack second = entry.moved(1);
        if (t < 1.9) {
            GrindstoneStation.drawGrinding(pose, buffers, camera, wheel, entry.side, first, second, entry.moveResult, GrindstoneStation.removesEnchantments(first) || GrindstoneStation.removesEnchantments(second), t, light);
            return;
        }
        Vec3 up = Anim.lerp(GrindstoneStation.heldPos(wheel, entry.side, 0, 1.9), GrindstoneStation.lifted(wheel, entry.side), Anim.inOutCubic(Anim.window(t, 1.9, 2.1999999999999997)));
        double away = Anim.inCubic(Anim.window(t, 2.12, 2.4));
        Vec3 at = Anim.lerp(up, entry.taker(GrindstoneStation.lifted(wheel, entry.side).m_82549_(Vec3.m_82528_((Vec3i)entry.side.m_122436_()).m_82490_(0.6))), away);
        GrindstoneStation.drawLifted(pose, buffers, camera.m_90583_(), entry.side, entry.moveResult, at, t, (float)((double)0.3f * (1.0 - 0.8 * away)), light);
    }

    static void onlookerFx(BlockPos pos, BlockState state, ClientMenus.Entry entry, double t, double dt) {
        boolean driven = t >= 0.0 && t < 2.0;
        entry.spin = driven ? Anim.approach(entry.spin, 1.0, 5.0, dt) : Anim.approach(entry.spin, 0.0, 1.3, dt);
        entry.run -= entry.spin * 70.0 * dt;
        if (t < 0.0 || t > 2.4) {
            return;
        }
        Vec3 wheel = GrindstoneStation.wheelCenter(pos, state);
        ItemStack first = entry.moved(0);
        ItemStack second = entry.moved(1);
        GrindstoneStation.grindFx(wheel, entry.side, !second.m_41619_(), GrindstoneStation.removesEnchantments(first) || GrindstoneStation.removesEnchantments(second), t, dt);
        if (t >= 0.6 && t < 1.9 && Math.floor((t - 0.6) / 0.45) != Math.floor((t - dt - 0.6) / 0.45)) {
            GrindstoneStation.sound(wheel, SoundEvents.f_11998_, 0.25f, 1.25f + Fx.RANDOM.m_188501_() * 0.2f);
        }
    }

    public static float armSway(double t) {
        return (float)(GrindstoneStation.stroke(t) * 2.4);
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
        if (this.target == Target.PLACE) {
            return this.placeSlot < 0 ? List.of(Component.m_237115_((String)"cinematic.grindstone.refuse").m_130940_(ChatFormatting.RED)) : null;
        }
        if (this.target != Target.ITEM || !carried.m_41619_()) {
            return null;
        }
        ArrayList<Component> lines = new ArrayList<Component>(Screen.m_280152_((Minecraft)mc, (ItemStack)this.slot(this.hoverSlot)));
        ItemStack result = this.slot(2);
        if (!result.m_41619_()) {
            boolean merges;
            lines.add((Component)Component.m_237119_());
            boolean strips = GrindstoneStation.removesEnchantments(this.slot(0)) || GrindstoneStation.removesEnchantments(this.slot(1));
            boolean bl = merges = !this.slot(0).m_41619_() && !this.slot(1).m_41619_();
            if (strips) {
                lines.add((Component)Component.m_237115_((String)"cinematic.grindstone.strips").m_130940_(ChatFormatting.LIGHT_PURPLE));
            }
            if (merges) {
                lines.add((Component)Component.m_237115_((String)"cinematic.grindstone.merges").m_130940_(ChatFormatting.GRAY));
            }
            lines.add((Component)Component.m_237115_((String)"cinematic.grindstone.grind").m_130940_(ChatFormatting.GOLD));
        } else {
            lines.add((Component)Component.m_237115_((String)"cinematic.grindstone.take").m_130940_(ChatFormatting.GRAY));
        }
        return lines;
    }

    @Override
    public Component hint() {
        return Component.m_237115_((String)"cinematic.hint.grindstone");
    }

    @Override
    public boolean stillThere(Level level) {
        return super.stillThere(level) && GrindstoneStation.wheelCenter(this.pos, level.m_8055_(this.pos)).m_82557_(this.wheel) < 1.0E-6;
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
        int slot = this.target == Target.PLACE ? this.placeSlot : this.hoverSlot;
        return slot < 0 ? 1.0f : this.geometry().scale(slot);
    }

    public static enum Target {
        NONE,
        PLACE,
        ITEM;

    }
}


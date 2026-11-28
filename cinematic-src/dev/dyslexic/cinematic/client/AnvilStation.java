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
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ServerboundRenameItemPacket
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.AnvilMenu
 *  net.minecraft.world.inventory.ClickType
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
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
import dev.dyslexic.cinematic.client.HammerModel;
import dev.dyslexic.cinematic.client.Hands;
import dev.dyslexic.cinematic.client.ItemPlacer;
import dev.dyslexic.cinematic.client.MenuStation;
import dev.dyslexic.cinematic.client.Picking;
import dev.dyslexic.cinematic.client.RemainingLayouts;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.SceneScreen;
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
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public final class AnvilStation
extends MenuStation<AnvilMenu>
implements SpatialTarget {
    final SpatialEditor gridEditor = new SpatialEditor(this);
    static final double HAND_Y = 0.062;
    static final double ITEM_REACH = 0.075;
    static final float MAIN_SIZE = 0.4f;
    static final float ADDITION_SIZE = 0.3f;
    static final float MAIN_TURN = 45.0f;
    static final float TEXT_SCALE = 0.006f;
    public static final double FORGE_LENGTH = 2.05;
    static final double[] BLOWS = new double[]{0.7, 1.02, 1.3};
    static final double SWING = 0.09;
    static final double PUT_AWAY = BLOWS[2] + 0.32;
    public final Vec3 top;
    public Target target = Target.NONE;
    private int layoutSlot = -1;
    private ItemStack mainBefore = ItemStack.f_41583_;
    private ItemStack additionBefore = ItemStack.f_41583_;
    private ItemStack after = ItemStack.f_41583_;
    private boolean toInventory;
    private int blowsDone;
    private ItemStack lastMain = ItemStack.f_41583_;
    private double renamedAt = -10.0;
    private static final float ARM_REST = -0.98f;
    private static final float ARM_HIP = 0.2f;
    private static final float ARM_RAISED = -2.75f;
    private static final float ARM_IMPACT = -1.26f;
    private static final float ARM_REBOUND = -1.6f;

    @Override
    public SpatialEditor layoutEditor() {
        return this.gridEditor;
    }

    public AnvilStation(BlockPos pos, Direction side) {
        super(pos, side, AnvilMenu.class, 2);
        this.top = AnvilStation.topOf(pos);
    }

    static Vec3 topOf(BlockPos pos) {
        return new Vec3((double)pos.m_123341_() + 0.5, (double)pos.m_123342_() + 1.0, (double)pos.m_123343_() + 0.5);
    }

    @Override
    public StationKind kind() {
        return StationKind.ANVIL;
    }

    @Override
    public Vec3 center() {
        return this.top;
    }

    @Override
    public SoundEvent touchSound() {
        return SoundEvents.f_12068_;
    }

    static Vec3 rightOf(Direction side) {
        return Vec3.m_82528_((Vec3i)side.m_122424_().m_122427_().m_122436_());
    }

    static Vec3 mainPoint(Vec3 top, Direction side) {
        return RemainingLayouts.point(StationKind.ANVIL, top, side, 0);
    }

    static Vec3 additionPoint(Vec3 top, Direction side) {
        return RemainingLayouts.point(StationKind.ANVIL, top, side, 1);
    }

    @Override
    protected Vec3 restPoint(int slot) {
        Vec3 at = slot == 0 ? AnvilStation.mainPoint(this.top, this.side) : AnvilStation.additionPoint(this.top, this.side);
        return at.m_82549_(this.right().m_82490_(this.denyShake()));
    }

    Vec3 namePoint() {
        return this.layoutPoint(2);
    }

    private Vec3 costPoint() {
        return this.namePoint().m_82549_(this.side().m_82490_(-0.06)).m_82520_(0.0, 0.1, 0.0);
    }

    @Override
    public List<Vec3> approach(LocalPlayer player) {
        Vec3 stand = new Vec3(this.top.f_82479_ + (double)this.side.m_122429_() * 0.86, player.m_20186_(), this.top.f_82481_ + (double)this.side.m_122431_() * 0.86);
        boolean walk = Math.hypot(stand.f_82479_ - player.m_20185_(), stand.f_82481_ - player.m_20189_()) > 0.06;
        return walk ? List.of(stand) : List.of();
    }

    @Override
    public boolean worksAtAnAngle() {
        return true;
    }

    @Override
    public CameraRig.Pose lean() {
        Vec3 eye = this.top.m_82549_(this.side().m_82490_(0.66)).m_82520_(0.0, 0.7, 0.0);
        Vec3 look = this.top.m_82549_(this.side().m_82490_(-0.06)).m_82520_(0.0, 0.12, 0.0).m_82546_(eye);
        float yaw = (float)Math.toDegrees(Math.atan2(-look.f_82479_, look.f_82481_));
        float pitch = (float)(-Math.toDegrees(Math.atan2(look.f_82480_, Math.hypot(look.f_82479_, look.f_82481_))));
        return new CameraRig.Pose(eye, yaw, pitch, 0.0f);
    }

    @Override
    public float bodyPitch() {
        return 45.0f;
    }

    @Override
    public Vec3 rest(boolean right) {
        return this.top.m_82549_(this.side().m_82490_(0.24)).m_82549_(this.right().m_82490_(right ? 0.3 : -0.3)).m_82520_(0.0, 0.062, 0.0);
    }

    @Override
    public void updateCursor(boolean overPanel) {
        boolean pile;
        this.clearCursor();
        if (overPanel || this.menu() == null || this.moving()) {
            return;
        }
        ItemStack carried = Hands.carried();
        if (!this.geometry().equals(SpatialAppearance.VANILLA)) {
            int picked;
            this.layoutSlot = picked = this.layoutPick();
            if (picked == 2 && carried.m_41619_() && !this.slot(0).m_41619_()) {
                this.target = Target.NAME;
                this.aim = this.namePoint();
            } else if (picked >= 0 && picked < 2) {
                this.target = carried.m_41619_() ? (!this.slot(picked).m_41619_() ? Target.STACK : Target.NONE) : Target.SURFACE;
                this.aim = this.layoutPoint(picked);
            }
            return;
        }
        double[] mouse = Picking.mouseGui();
        boolean bl = pile = !this.slot(0).m_41619_() || !this.slot(1).m_41619_();
        if (carried.m_41619_() && !this.slot(0).m_41619_() && this.overName(mouse)) {
            this.target = Target.NAME;
            this.aim = this.namePoint().m_82520_(0.0, -0.08, 0.0);
            return;
        }
        if (carried.m_41619_() && pile && GrindstoneStation.under(mouse, this.slot(0).m_41619_() ? this.restPoint(1) : this.restPoint(0), 0.16)) {
            this.target = Target.STACK;
            this.aim = this.slot(1).m_41619_() ? this.restPoint(0) : this.restPoint(1);
            return;
        }
        Vec3 hit = Picking.cursorOnPlane(this.top.f_82480_);
        if (hit == null || carried.m_41619_()) {
            return;
        }
        Vec3 rel = hit.m_82546_(this.top);
        if (Math.abs(rel.m_82526_(this.right())) < 0.52 && Math.abs(rel.m_82526_(this.side())) < 0.36) {
            this.target = Target.SURFACE;
            this.aim = this.slot(0).m_41619_() ? this.restPoint(0) : this.restPoint(1);
        }
    }

    private boolean overName(double[] mouse) {
        Font font = Minecraft.m_91087_().f_91062_;
        double half = (double)font.m_92852_((FormattedText)this.nameText()) / 2.0 * (double)0.006f + 0.03;
        double[] a = Picking.toGui(this.namePoint().m_82549_(this.right().m_82490_(-half)));
        double[] b = Picking.toGui(this.namePoint().m_82549_(this.right().m_82490_(half)));
        double[] c = Picking.toGui(this.namePoint());
        double[] d = Picking.toGui(this.namePoint().m_82520_(0.0, 0.05, 0.0));
        if (a == null || b == null || c == null || d == null) {
            return false;
        }
        double h = Math.max(6.0, Math.abs(c[1] - d[1]));
        return mouse[0] > Math.min(a[0], b[0]) && mouse[0] < Math.max(a[0], b[0]) && Math.abs(mouse[1] - c[1]) < h;
    }

    @Override
    public void clearCursor() {
        super.clearCursor();
        this.target = Target.NONE;
        this.layoutSlot = -1;
    }

    @Override
    public Vec3 handTarget(Vec3 aim, Vec3 shoulder, boolean carrying, double tap) {
        Vec3 reach = new Vec3(aim.f_82479_ - shoulder.f_82479_, 0.0, aim.f_82481_ - shoulder.f_82481_).m_82541_();
        double hover = carrying ? 0.075 : 0.045;
        return aim.m_82520_(0.0, 0.062 + hover - 0.06 * tap, 0.0).m_82546_(reach.m_82490_(0.075));
    }

    @Override
    public Vec3 heldItemPoint(Vec3 hand, Vec3 shoulder) {
        Vec3 reach = new Vec3(hand.f_82479_ - shoulder.f_82479_, 0.0, hand.f_82481_ - shoulder.f_82481_).m_82541_();
        return hand.m_82549_(reach.m_82490_(0.075)).m_82520_(0.0, -0.05, 0.0);
    }

    private int cost() {
        AnvilMenu menu = (AnvilMenu)this.menu();
        return menu == null ? 0 : menu.m_39028_();
    }

    private boolean affordable() {
        AnvilMenu menu = (AnvilMenu)this.menu();
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        return menu != null && player != null && menu.m_38853_(menu.m_266562_()).m_8010_((Player)player);
    }

    private boolean tooExpensive() {
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        return this.slot(2).m_41619_() && this.cost() >= 40 && player != null && !player.m_150110_().f_35937_;
    }

    @Override
    protected void act(int button, boolean shift) {
        switch (this.target) {
            case SURFACE: {
                int slot = this.layoutSlot >= 0 && this.layoutSlot < 2 ? this.layoutSlot : (this.slot(0).m_41619_() ? 0 : 1);
                this.slotClick(slot, button, false);
                Scene.sound(SoundEvents.f_11745_, 0.6f, slot == 0 ? 0.9f : 1.2f);
                break;
            }
            case STACK: {
                if (button == 0 && !this.slot(2).m_41619_()) {
                    if (this.affordable()) {
                        this.forge(shift);
                        break;
                    }
                    this.deny();
                    break;
                }
                this.slotClick(this.layoutSlot >= 0 && this.layoutSlot < 2 ? this.layoutSlot : (this.slot(1).m_41619_() ? 0 : 1), 0, shift);
                break;
            }
            case NAME: {
                Hands.tap();
                SceneScreen.edit(this.nameText().getString(), 50, this::rename);
                break;
            }
        }
    }

    public void rename(String name) {
        if (this.sendName(name)) {
            this.renamedAt = Scene.clock();
            Scene.sound(SoundEvents.f_11713_, 0.6f, 1.1f);
        }
    }

    private boolean sendName(String name) {
        String value;
        AnvilMenu menu = (AnvilMenu)this.menu();
        Minecraft mc = Minecraft.m_91087_();
        ItemStack item = this.slot(0);
        if (menu == null || item.m_41619_() || mc.f_91074_ == null) {
            return false;
        }
        String string = value = !item.m_41788_() && name.equals(item.m_41786_().getString()) ? "" : name;
        if (!menu.m_39020_(value)) {
            return false;
        }
        mc.f_91074_.f_108617_.m_104955_((Packet)new ServerboundRenameItemPacket(value));
        return true;
    }

    private Component nameText() {
        ItemStack result = this.slot(2);
        return result.m_41619_() ? this.slot(0).m_41786_() : result.m_41786_();
    }

    private boolean renaming() {
        ItemStack result = this.slot(2);
        return !result.m_41619_() && !result.m_41786_().getString().equals(this.slot(0).m_41786_().getString());
    }

    private void forge(boolean shift) {
        this.mainBefore = this.slot(0).m_41777_();
        this.additionBefore = this.slot(1).m_41777_();
        this.after = this.slot(2).m_41777_();
        this.toInventory = shift;
        this.blowsDone = 0;
        this.startMove(2.05, BLOWS[0], () -> {
            AnvilMenu menu = (AnvilMenu)this.menu();
            Minecraft mc = Minecraft.m_91087_();
            if (this.slot(2).m_41619_() || !this.affordable()) {
                this.cancelMove();
                return;
            }
            mc.f_91072_.m_171799_(menu.f_38840_, 2, 0, this.toInventory ? ClickType.QUICK_MOVE : ClickType.PICKUP, (Player)mc.f_91074_);
        });
        this.showMove(shift);
        Scene.sound(SoundEvents.f_11677_, 0.5f, 0.9f);
    }

    @Override
    public void tick() {
        ItemStack main = this.slot(0);
        if (!ItemStack.m_41728_((ItemStack)main, (ItemStack)this.lastMain)) {
            this.lastMain = main.m_41777_();
            if (!main.m_41619_() && !this.moving()) {
                this.sendName(main.m_41786_().getString());
            }
        }
    }

    private Vec3 anvilPoint() {
        return AnvilStation.anvilPoint(this.top, this.side);
    }

    static Vec3 anvilPoint(Vec3 top, Direction side) {
        return AnvilStation.mainPoint(top, side).m_82549_(AnvilStation.rightOf(side).m_82490_(0.03));
    }

    private Vec3 swingDirection() {
        return this.side().m_82490_(-0.3).m_82549_(this.right().m_82490_(-0.12)).m_82541_();
    }

    private Vec3 handleDirection(double angle) {
        double a = Math.toRadians(angle);
        return this.swingDirection().m_82490_(Math.cos(a)).m_82520_(0.0, Math.sin(a), 0.0);
    }

    private Swing impact() {
        double angle = -12.0;
        Vec3 grip = this.anvilPoint().m_82546_(this.handleDirection(angle).m_82490_(HammerModel.STRIKE_ALONG)).m_82549_(this.headAxis(angle).m_82490_(0.11250000447034836));
        return new Swing(grip, angle);
    }

    private Swing raised() {
        Swing hit = this.impact();
        return new Swing(hit.grip().m_82520_(0.0, 0.16, 0.0).m_82549_(this.side().m_82490_(0.05)), 95.0);
    }

    private Swing rebound() {
        Swing hit = this.impact();
        return new Swing(hit.grip().m_82520_(0.0, 0.05, 0.0).m_82549_(this.side().m_82490_(0.02)), 28.0);
    }

    private Swing hidden() {
        CameraRig.Pose cam = CameraRig.last() != null ? CameraRig.last() : this.lean();
        return new Swing(Hands.hidden(true, cam.pos(), cam.yaw()), 80.0);
    }

    private static Swing mix(Swing a, Swing b, double k) {
        return new Swing(Anim.lerp(a.grip(), b.grip(), k), a.angle() + (b.angle() - a.angle()) * k);
    }

    @Nullable
    private Swing hammer(double t) {
        if (t < 0.3 || t > PUT_AWAY) {
            return null;
        }
        if (t < 0.55) {
            return AnvilStation.mix(this.hidden(), this.raised(), Anim.outCubic(Anim.window(t, 0.3, 0.55)));
        }
        for (int k = 0; k < BLOWS.length; ++k) {
            double windFrom;
            double blow = BLOWS[k];
            double d = windFrom = k == 0 ? 0.55 : BLOWS[k - 1] + 0.12;
            if (t < blow - 0.09) {
                Swing from = k == 0 ? this.raised() : this.rebound();
                return AnvilStation.mix(from, this.raised(), Anim.inOutSine(Anim.window(t, windFrom, blow - 0.09)));
            }
            if (t < blow) {
                return AnvilStation.mix(this.raised(), this.impact(), Anim.inCubic(Anim.window(t, blow - 0.09, blow)));
            }
            if (!(t < blow + 0.12) && k != BLOWS.length - 1) continue;
            if (k == BLOWS.length - 1 && t >= blow + 0.12) {
                return AnvilStation.mix(this.rebound(), this.hidden(), Anim.inCubic(Anim.window(t, blow + 0.12, PUT_AWAY)));
            }
            return AnvilStation.mix(this.impact(), this.rebound(), Anim.outCubic(Anim.window(t, blow, blow + 0.12)));
        }
        return null;
    }

    private Vec3 resultPos(double t, Camera camera) {
        Vec3 lifted = AnvilStation.mainPoint(this.top, this.side).m_82520_(0.0, 0.13, 0.0);
        Vec3 at = Anim.lerp(AnvilStation.mainPoint(this.top, this.side), lifted, Anim.inOutCubic(Anim.window(t, BLOWS[2] + 0.18, PUT_AWAY + 0.08)));
        if (this.toInventory) {
            Vec3 drawer = camera.m_90583_().m_82520_(0.0, 0.55, 0.0).m_82549_(Scene.yawForward(camera.m_90590_()).m_82490_(0.6));
            return Anim.lerp(at, drawer, Anim.inCubic(Anim.window(t, PUT_AWAY + 0.12, 2.05)));
        }
        Vec3 held = this.heldItemPoint(Hands.position(true), Hands.shoulder(true, camera.m_90583_(), camera.m_90590_()));
        return Anim.lerp(at, held, Anim.inOutCubic(Anim.window(t, PUT_AWAY + 0.12, 2.05)));
    }

    @Override
    @Nullable
    public Vec3[] handsOverride() {
        Vec3 right;
        double t = this.moveTime();
        if (t < 0.0) {
            return null;
        }
        Swing swing = this.hammer(t);
        if (t < 0.3) {
            right = Anim.lerp(this.rest(true), this.hidden().grip(), Anim.inCubic(Anim.window(t, 0.0, 0.3)));
        } else if (swing != null) {
            right = swing.grip();
        } else {
            Vec3 lifted = AnvilStation.mainPoint(this.top, this.side).m_82520_(0.0, 0.192, 0.0).m_82549_(this.side().m_82490_(0.075));
            Vec3 up = Anim.lerp(this.hidden().grip(), lifted, Anim.outCubic(Anim.window(t, PUT_AWAY, PUT_AWAY + 0.16)));
            right = Anim.lerp(up, this.rest(true).m_82520_(0.0, this.toInventory ? 0.0 : 0.05, 0.0), Anim.inOutCubic(Anim.window(t, PUT_AWAY + 0.16, 2.05)));
        }
        double hold = Anim.inOutCubic(Anim.window(t, 0.05, 0.4)) * (1.0 - Anim.inOutCubic(Anim.window(t, BLOWS[2] + 0.1, BLOWS[2] + 0.4)));
        Vec3 steady = AnvilStation.mainPoint(this.top, this.side).m_82549_(this.layoutAxis(0, true).m_82490_(-0.2 * (double)AnvilStation.geometryScale(0))).m_82549_(this.layoutAxis(0, false).m_82490_(0.05)).m_82520_(0.0, 0.077, 0.0);
        return new Vec3[]{right, Anim.lerp(this.rest(false), steady, hold)};
    }

    @Override
    public double handsStiffness() {
        return this.moving() ? 55.0 : 20.0;
    }

    private static double sinceBlow(double t) {
        double since = Double.MAX_VALUE;
        for (double blow : BLOWS) {
            if (!(t >= blow)) continue;
            since = t - blow;
        }
        return since;
    }

    @Override
    public double cameraKick() {
        double t = this.moveTime();
        return t < 0.0 ? 0.0 : 0.5 * Math.exp(-AnvilStation.sinceBlow(t) * 13.0);
    }

    @Override
    public double flash() {
        double t = this.moveTime();
        return t < 0.0 ? 0.0 : 0.11 * Math.exp(-AnvilStation.sinceBlow(t) * 16.0);
    }

    @Override
    public int flashColor() {
        return 16770744;
    }

    @Override
    void frame() {
        super.frame();
        double t = this.moveTime();
        if (t < 0.0 || this.blowsDone >= BLOWS.length || t < BLOWS[this.blowsDone]) {
            return;
        }
        AnvilStation.blow(this.top, this.side, this.blowsDone++, this.additionBefore, this.after);
    }

    static void blow(Vec3 top, Direction side, int k, ItemStack addition, ItemStack after) {
        Vec3 at = AnvilStation.anvilPoint(top, side).m_82520_(0.0, 0.03, 0.0);
        Fx.blowSparks(at, 34, 2.4);
        Fx.hotSparks(at, Vec3.m_82528_((Vec3i)side.m_122436_()).m_82490_(-1.0).m_82520_(0.0, 0.7, 0.0), 0.5, 12, 2.6);
        if (k > 0) {
            AnvilStation.ring(at, SoundEvents.f_11671_, 0.45f, 1.05f + (float)k * 0.12f);
        }
        if (addition.m_150930_(Items.f_42690_)) {
            for (int i = 0; i < 10; ++i) {
                EnchantFx.spark(at.m_82520_(0.0, 0.02, 0.0), new Vec3(Fx.RANDOM.m_188583_() * 0.2, 0.3 + Fx.RANDOM.m_188500_() * 0.3, Fx.RANDOM.m_188583_() * 0.2), 0.7 + Fx.RANDOM.m_188500_() * 0.5, 0.012f);
            }
        }
        if (k == BLOWS.length - 1) {
            Vec3 at2 = AnvilStation.mainPoint(top, side).m_82520_(0.0, 0.05, 0.0);
            if (after.m_41793_()) {
                Fx.motes(at2, 0.12, 24, 0.8f, 0.5f, 1.0f, 0.3);
            }
            Fx.motes(at2, 0.1, 10, 1.0f, 0.75f, 0.4f, 0.2);
            AnvilStation.ring(at2, SoundEvents.f_144243_, 0.7f, 0.8f);
        }
    }

    private static void ring(Vec3 at, SoundEvent sound, float volume, float pitch) {
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91073_ != null) {
            mc.f_91073_.m_7785_(at.f_82479_, at.f_82480_, at.f_82481_, sound, SoundSource.BLOCKS, volume, pitch, false);
        }
    }

    @Override
    public void renderScene(PoseStack pose, MultiBufferSource buffers, Camera camera, int light) {
        Vec3 cam = camera.m_90583_();
        if (this.outro()) {
            this.drawLeaving(pose, buffers, camera, light);
            return;
        }
        double t = this.moveTime();
        if (t < 0.0) {
            AnvilStation.drawPile(pose, buffers, cam, this.top, this.side, this.slot(0), this.slot(1), this.denyShake(), 1.0, light);
            return;
        }
        if (t < BLOWS[2] + 0.04) {
            AnvilStation.drawForging(pose, buffers, camera, this.top, this.side, this.mainBefore, this.additionBefore, t, light);
        } else {
            float size = this.toInventory ? (float)((double)0.4f * (1.0 - 0.85 * Anim.inCubic(Anim.window(t, PUT_AWAY + 0.12, 2.05)))) : (float)((double)0.4f + -0.1950000059604645 * Anim.window(t, PUT_AWAY + 0.1, 2.05));
            AnvilStation.drawForged(pose, buffers, camera, this.side, this.after, this.resultPos(t, camera), t, size, light);
        }
        Swing swing = this.hammer(t);
        if (swing != null) {
            this.drawHammer(pose, buffers, cam, Hands.position(true), swing.angle(), light);
        }
    }

    static void drawForging(PoseStack pose, MultiBufferSource buffers, Camera camera, Vec3 top, Direction side, ItemStack main, ItemStack addition, double t, int light) {
        double sunk = 0.0;
        for (double blow : BLOWS) {
            sunk += Anim.smoother(Anim.window(t, blow, blow + 0.07)) / (double)BLOWS.length;
        }
        AnvilStation.drawPile(pose, buffers, camera.m_90583_(), top, side, main, addition, 0.0, 1.0 - sunk, light);
        double heat = sunk + 0.25 * Math.exp(-AnvilStation.sinceBlow(t) * 10.0);
        if (!addition.m_41619_() && heat > 0.02) {
            float g = (float)(0.22 * heat);
            boolean book = addition.m_150930_(Items.f_42690_);
            EnchantFx.billboard(pose, buffers, camera, AnvilStation.anvilPoint(top, side).m_82520_(0.0, 0.03, 0.0), 0.16 + 0.05 * heat, Scene.clock(), EnchantFx.GLOW, book ? 0.7f * g : g, book ? 0.4f * g : 0.6f * g, book ? g : 0.25f * g);
        }
    }

    static void drawForged(PoseStack pose, MultiBufferSource buffers, Camera camera, Direction side, ItemStack after, Vec3 at, double t, float size, int light) {
        double turn = 200.0 * Anim.inOutCubic(Anim.window(t, BLOWS[2] + 0.1, PUT_AWAY + 0.1));
        float tilt = (float)(35.0 * Anim.hump(Anim.window(t, BLOWS[2] + 0.1, 2.05)));
        ItemPlacer.drawOriented(pose, buffers, camera.m_90583_(), after, at.m_82520_(0.0, 0.004, 0.0), ItemPlacer.lying((float)((double)(ItemPlacer.awayYaw(side) + 45.0f + RemainingLayouts.rotation(StationKind.ANVIL, 0)) + turn), tilt), size *= AnvilStation.geometryScale(0), light);
        double glow = 1.0 - Anim.window(t, BLOWS[2], PUT_AWAY + 0.2);
        if (glow > 0.01) {
            float g = (float)(0.4 * glow);
            EnchantFx.billboard(pose, buffers, camera, at, 0.2, Scene.clock(), EnchantFx.GLOW, g, 0.8f * g, 0.5f * g);
        }
    }

    static void drawPile(PoseStack pose, MultiBufferSource buffers, Vec3 cam, Vec3 top, Direction side, ItemStack main, ItemStack addition, double shake, double additionScale, int light) {
        float size;
        boolean block;
        Vec3 shift = AnvilStation.rightOf(side).m_82490_(shake);
        float yaw = ItemPlacer.awayYaw(side) + 45.0f + RemainingLayouts.rotation(StationKind.ANVIL, 0);
        if (!main.m_41619_()) {
            block = ItemPlacer.isBlock(main);
            size = (block ? 0.2f : 0.4f) * AnvilStation.geometryScale(0);
            Vec3 at = AnvilStation.mainPoint(top, side).m_82549_(shift).m_82520_(0.0, block ? (double)(size / 2.0f) : 0.004, 0.0);
            ItemPlacer.drawOriented(pose, buffers, cam, main, at, ItemPlacer.lying(yaw, 0.0f), size, light);
        }
        if (!addition.m_41619_() && additionScale > 0.01) {
            block = ItemPlacer.isBlock(addition);
            size = (float)((block ? 0.16 : (double)0.3f) * (0.35 + 0.65 * additionScale) * (double)AnvilStation.geometryScale(1));
            Vec3 home = main.m_41619_() && SpatialLayouts.current(StationKind.ANVIL).equals(SpatialAppearance.VANILLA) ? AnvilStation.mainPoint(top, side) : AnvilStation.additionPoint(top, side);
            Vec3 at = Anim.lerp(AnvilStation.mainPoint(top, side), home, additionScale).m_82549_(shift).m_82520_(0.0, (block ? (double)(size / 2.0f) : 0.006) - 0.01 * (1.0 - additionScale), 0.0);
            ItemPlacer.drawOriented(pose, buffers, cam, addition, at, ItemPlacer.lying(yaw + 70.0f + RemainingLayouts.rotation(StationKind.ANVIL, 1) - RemainingLayouts.rotation(StationKind.ANVIL, 0), 0.0f), size, light);
            if (addition.m_41613_() > 1) {
                ItemPlacer.drawOriented(pose, buffers, cam, addition, at.m_82549_(AnvilStation.rightOf(side).m_82490_(0.03)).m_82520_(0.0, 0.008, 0.0), ItemPlacer.lying(yaw + 95.0f + RemainingLayouts.rotation(StationKind.ANVIL, 1) - RemainingLayouts.rotation(StationKind.ANVIL, 0), 0.0f), size, light);
            }
        }
    }

    private static float geometryScale(int slot) {
        return RemainingLayouts.scale(StationKind.ANVIL, slot);
    }

    private Vec3 headAxis(double angle) {
        double a = Math.toRadians(angle);
        return new Vec3(0.0, Math.cos(a), 0.0).m_82546_(this.swingDirection().m_82490_(Math.sin(a)));
    }

    private void drawHammer(PoseStack pose, MultiBufferSource buffers, Vec3 cam, Vec3 grip, double angle, int light) {
        HammerModel.render(pose, buffers, cam, grip, this.handleDirection(angle), this.headAxis(angle), light);
    }

    @Override
    public void renderWorld(PoseStack pose, MultiBufferSource buffers, Vec3 camera, int light) {
        this.gridEditor.renderWorld(pose, buffers, camera, light);
        Minecraft mc = Minecraft.m_91087_();
        Camera cam = mc.f_91063_.m_109153_();
        double clock = Scene.clock();
        if (this.target == Target.SURFACE && this.aim != null) {
            float p = (float)(0.16 + 0.06 * Math.sin(clock * 5.0));
            EnchantFx.disc(pose, buffers, camera, this.aim.m_82520_(0.0, 0.004, 0.0), 0.14, 0.0, EnchantFx.GLOW, p, p * 0.75f, p * 0.45f);
        }
        if (this.moving() || this.slot(0).m_41619_()) {
            return;
        }
        boolean renamed = this.renaming();
        double pulse = Anim.hump(Anim.window(clock - this.renamedAt, 0.0, 0.5));
        int nameColor = renamed ? -8070 : -857384;
        AnvilStation.drawText(pose, buffers, cam, this.nameText(), this.namePoint(), 0.006f * (float)(1.0 + 0.15 * pulse), nameColor, this.target == Target.NAME ? 0.55f : 0.35f);
        if (this.target == Target.NAME) {
            AnvilStation.drawText(pose, buffers, cam, (Component)Component.m_237113_((String)"\u270e"), this.namePoint().m_82549_(this.right().m_82490_(-((double)mc.f_91062_.m_92852_((FormattedText)this.nameText()) / 2.0 * (double)0.006f + 0.035))), 0.006f, -8070, 0.0f);
        }
        int cost = this.cost();
        if (!this.slot(2).m_41619_() && cost > 0) {
            boolean ok = this.affordable();
            Vec3 at = this.costPoint();
            float g = (float)(0.22 + 0.06 * Math.sin(clock * 3.0));
            EnchantFx.billboard(pose, buffers, cam, at, 0.06, clock * 0.5, EnchantFx.GLOW, ok ? 0.45f * g : g, ok ? g : 0.25f * g, ok ? 0.2f * g : 0.2f * g);
            AnvilStation.drawText(pose, buffers, cam, (Component)Component.m_237113_((String)String.valueOf(cost)), at, 0.012f, ok ? -6488230 : -38037, 0.0f);
        } else if (this.tooExpensive()) {
            AnvilStation.drawText(pose, buffers, cam, (Component)Component.m_237115_((String)"container.repair.expensive"), this.costPoint(), 0.006f, -38037, 0.35f);
        }
    }

    private static void drawText(PoseStack pose, MultiBufferSource buffers, Camera camera, Component text, Vec3 at, float scale, int color, float backdrop) {
        Font font = Minecraft.m_91087_().f_91062_;
        Vec3 cam = camera.m_90583_();
        pose.m_85836_();
        pose.m_85837_(at.f_82479_ - cam.f_82479_, at.f_82480_ - cam.f_82480_, at.f_82481_ - cam.f_82481_);
        pose.m_252781_(camera.m_253121_());
        pose.m_252781_(Axis.f_252403_.m_252977_((float)(-SpatialLayouts.current(StationKind.ANVIL).rotation(2))));
        pose.m_85841_(-(scale *= AnvilStation.geometryScale(2)), -scale, scale);
        int background = backdrop > 0.0f ? (int)(backdrop * 255.0f) << 24 : 0;
        font.m_272077_(text, (float)(-font.m_92852_((FormattedText)text)) / 2.0f, -4.0f, color, false, pose.m_85850_().m_252922_(), buffers, Font.DisplayMode.NORMAL, background, 0xF000F0);
        pose.m_85849_();
    }

    static void drawContents(PoseStack pose, MultiBufferSource buffers, Vec3 cam, BlockPos pos, Direction side, List<ItemStack> items, int light) {
        Vec3 top = new Vec3((double)pos.m_123341_() + 0.5, (double)pos.m_123342_() + 1.0, (double)pos.m_123343_() + 0.5);
        AnvilStation.drawPile(pose, buffers, cam, top, side, items.isEmpty() ? ItemStack.f_41583_ : items.get(0), items.size() < 2 ? ItemStack.f_41583_ : items.get(1), 0.0, 1.0, light);
    }

    static void drawOnlooker(PoseStack pose, MultiBufferSource buffers, Camera camera, BlockPos pos, ClientMenus.Entry entry, double t, int light) {
        Vec3 top = AnvilStation.topOf(pos);
        if (t < BLOWS[2] + 0.04) {
            AnvilStation.drawForging(pose, buffers, camera, top, entry.side, entry.moved(0), entry.moved(1), t, light);
            return;
        }
        Vec3 lifted = AnvilStation.mainPoint(top, entry.side).m_82520_(0.0, 0.13, 0.0);
        Vec3 at = Anim.lerp(AnvilStation.mainPoint(top, entry.side), lifted, Anim.inOutCubic(Anim.window(t, BLOWS[2] + 0.18, PUT_AWAY + 0.08)));
        double away = Anim.inCubic(Anim.window(t, PUT_AWAY + 0.12, 2.05));
        at = Anim.lerp(at, entry.taker(lifted.m_82549_(Vec3.m_82528_((Vec3i)entry.side.m_122436_()).m_82490_(0.8))), away);
        AnvilStation.drawForged(pose, buffers, camera, entry.side, entry.moveResult, at, t, (float)((double)0.4f * (1.0 - 0.8 * away)), light);
    }

    static void onlookerFx(BlockPos pos, ClientMenus.Entry entry, double t, double dt) {
        for (int k = 0; k < BLOWS.length; ++k) {
            if (!(t - dt < BLOWS[k]) || !(t >= BLOWS[k])) continue;
            AnvilStation.blow(AnvilStation.topOf(pos), entry.side, k, entry.moved(1), entry.moveResult);
        }
    }

    public static float armPitch(double t) {
        if (t < 0.3) {
            return Mth.m_14179_((float)((float)Anim.smoother(t / 0.3)), (float)-0.98f, (float)0.2f);
        }
        if (t < 0.55) {
            return Mth.m_14179_((float)((float)Anim.outCubic(Anim.window(t, 0.3, 0.55))), (float)0.2f, (float)-2.75f);
        }
        if (t >= PUT_AWAY) {
            return Mth.m_14179_((float)((float)Anim.inOutCubic(Anim.window(t, PUT_AWAY, 2.05))), (float)0.2f, (float)-0.98f);
        }
        for (int k = 0; k < BLOWS.length; ++k) {
            double windFrom;
            double blow = BLOWS[k];
            double d = windFrom = k == 0 ? 0.55 : BLOWS[k - 1] + 0.12;
            if (t < blow - 0.09) {
                return Mth.m_14179_((float)((float)Anim.inOutSine(Anim.window(t, windFrom, blow - 0.09))), (float)(k == 0 ? -2.75f : -1.6f), (float)-2.75f);
            }
            if (t < blow) {
                return Mth.m_14179_((float)((float)Anim.inCubic(Anim.window(t, blow - 0.09, blow))), (float)-2.75f, (float)-1.26f);
            }
            if (!(t < blow + 0.12)) continue;
            return Mth.m_14179_((float)((float)Anim.outCubic(Anim.window(t, blow, blow + 0.12))), (float)-1.26f, (float)-1.6f);
        }
        return Mth.m_14179_((float)((float)Anim.inCubic(Anim.window(t, BLOWS[2] + 0.12, PUT_AWAY))), (float)-1.6f, (float)0.2f);
    }

    public static double hammerShown(double t) {
        return Anim.smoother(Anim.window(t, 0.26, 0.36)) * (1.0 - Anim.smoother(Anim.window(t, PUT_AWAY - 0.1, PUT_AWAY)));
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
            case NAME: {
                return List.of(Component.m_237115_((String)"cinematic.anvil.rename").m_130940_(ChatFormatting.GRAY));
            }
            case STACK: {
                ItemStack result = this.slot(2);
                ArrayList<Component> lines = new ArrayList<Component>();
                if (!result.m_41619_()) {
                    lines.addAll(Screen.m_280152_((Minecraft)mc, (ItemStack)result));
                    lines.add((Component)Component.m_237119_());
                    lines.add((Component)Component.m_237110_((String)"container.repair.cost", (Object[])new Object[]{this.cost()}).m_130940_(this.affordable() ? ChatFormatting.GREEN : ChatFormatting.RED));
                    lines.add((Component)Component.m_237115_((String)"cinematic.anvil.forge").m_130940_(ChatFormatting.GOLD));
                    return lines;
                }
                lines.addAll(Screen.m_280152_((Minecraft)mc, (ItemStack)(this.slot(1).m_41619_() ? this.slot(0) : this.slot(1))));
                if (this.tooExpensive()) {
                    lines.add((Component)Component.m_237115_((String)"container.repair.expensive").m_130940_(ChatFormatting.RED));
                } else if (this.slot(1).m_41619_()) {
                    lines.add((Component)Component.m_237115_((String)"cinematic.anvil.add").m_130940_(ChatFormatting.GRAY));
                } else {
                    lines.add((Component)Component.m_237115_((String)"cinematic.anvil.no_match").m_130940_(ChatFormatting.RED));
                }
                lines.add((Component)Component.m_237115_((String)"cinematic.anvil.take").m_130940_(ChatFormatting.GRAY));
                return lines;
            }
        }
        return null;
    }

    @Override
    public Component hint() {
        return Component.m_237115_((String)"cinematic.hint.anvil");
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
        return this.layoutSlot >= 0 && this.layoutSlot < 2 ? this.geometry().scale(this.layoutSlot) : this.geometry().scale(0);
    }

    public static enum Target {
        NONE,
        SURFACE,
        STACK,
        NAME;

    }

    private record Swing(Vec3 grip, double angle) {
    }
}


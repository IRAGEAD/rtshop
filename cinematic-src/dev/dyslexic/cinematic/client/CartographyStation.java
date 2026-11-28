/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.math.Axis
 *  javax.annotation.Nullable
 *  net.minecraft.ChatFormatting
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.CartographyTableMenu
 *  net.minecraft.world.inventory.ClickType
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.item.MapItem
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.saveddata.maps.MapItemSavedData
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix4f
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dyslexic.cinematic.client.Anim;
import dev.dyslexic.cinematic.client.CameraRig;
import dev.dyslexic.cinematic.client.EnchantFx;
import dev.dyslexic.cinematic.client.Fx;
import dev.dyslexic.cinematic.client.GrindstoneStation;
import dev.dyslexic.cinematic.client.Hands;
import dev.dyslexic.cinematic.client.ItemPlacer;
import dev.dyslexic.cinematic.client.Picking;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.SmithingStation;
import dev.dyslexic.cinematic.client.SurfaceAppearance;
import dev.dyslexic.cinematic.client.SurfaceEditor;
import dev.dyslexic.cinematic.client.SurfaceLayouts;
import dev.dyslexic.cinematic.client.SurfaceStation;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

public final class CartographyStation
extends SurfaceStation<CartographyTableMenu> {
    static final double HAND_Y = 0.062;
    static final double ITEM_REACH = 0.075;
    static final double LEAN_BACK = 0.7;
    static final double LEAN_UP = 0.64;
    static final double[][] LAYOUT = new double[][]{{-0.04, 0.04}, {0.34, -0.1}};
    static final float MAP_SIZE = 0.5f;
    static final float ADDITION_SIZE = 0.24f;
    public static final double WORK_LENGTH = 2.1;
    static final double BEAT = 1.55;
    private static final ResourceLocation MAP_BACKGROUND = new ResourceLocation("textures/map/map_background.png");
    public final Vec3 top;
    public Target target = Target.NONE;
    public int hoverSlot = -1;
    private int placeSlot = -1;
    private Work work = Work.ZOOM;
    private ItemStack mapBefore = ItemStack.f_41583_;
    private ItemStack addition = ItemStack.f_41583_;
    private ItemStack result = ItemStack.f_41583_;
    private boolean toInventory;
    private boolean gleamed;

    public CartographyStation(BlockPos pos, Direction side) {
        super(pos, side, CartographyTableMenu.class, 2);
        this.top = new Vec3((double)pos.m_123341_() + 0.5, (double)pos.m_123342_() + 1.0, (double)pos.m_123343_() + 0.5);
    }

    @Override
    public StationKind kind() {
        return StationKind.CARTOGRAPHY;
    }

    @Override
    public Vec3 center() {
        return this.top;
    }

    @Override
    public SoundEvent touchSound() {
        return SoundEvents.f_12638_;
    }

    static Vec3 slotPoint(Vec3 top, Direction side, int slot) {
        return SurfaceLayouts.current(StationKind.CARTOGRAPHY).point(StationKind.CARTOGRAPHY, top, side, slot);
    }

    @Override
    float heldItemScale() {
        return this.heldItemScale(this.placeSlot >= 0 ? this.placeSlot : this.hoverSlot);
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
        Vec3 at = this.top.m_82549_(this.side().m_82490_(0.7)).m_82520_(0.0, 0.64, 0.0);
        float pitch = (float)Math.toDegrees(Math.atan2(0.64, 0.72));
        return new CameraRig.Pose(at, this.side.m_122424_().m_122435_(), pitch, 0.0f);
    }

    @Override
    public Vec3 rest(boolean right) {
        return this.top.m_82549_(this.side().m_82490_(0.4)).m_82549_(this.right().m_82490_(right ? 0.26 : -0.26)).m_82520_(0.0, 0.062, 0.0);
    }

    @Override
    public void updateCursor(boolean overPanel) {
        this.clearCursor();
        if (overPanel || this.gridEditor.editing() || this.menu() == null || this.moving()) {
            return;
        }
        if (!this.geometry().equals(SurfaceAppearance.VANILLA)) {
            int selected = this.surfaceSlot(Hands.carried().m_41619_());
            if (selected >= 0) {
                if (Hands.carried().m_41619_()) {
                    this.target = Target.ITEM;
                    this.hoverSlot = selected;
                } else {
                    this.target = Target.PLACE;
                    this.placeSlot = this.slotFor(Hands.carried(), selected);
                }
                this.aim = this.restPoint(this.placeSlot < 0 ? selected : this.placeSlot);
            }
            return;
        }
        ItemStack carried = Hands.carried();
        double[] mouse = Picking.mouseGui();
        if (carried.m_41619_()) {
            for (int i = 1; i >= 0; --i) {
                if (this.slot(i).m_41619_() || !GrindstoneStation.under(mouse, this.restPoint(i), i == 0 ? 0.2 : 0.1)) continue;
                this.target = Target.ITEM;
                this.hoverSlot = i;
                this.aim = this.restPoint(i);
                return;
            }
            return;
        }
        Vec3 hit = Picking.cursorOnPlane(this.top.f_82480_);
        if (hit == null || Math.abs(hit.f_82479_ - this.top.f_82479_) > 0.52 || Math.abs(hit.f_82481_ - this.top.f_82481_) > 0.52) {
            return;
        }
        int nearest = hit.m_82557_(this.restPoint(0)) < hit.m_82557_(this.restPoint(1)) ? 0 : 1;
        this.target = Target.PLACE;
        this.placeSlot = this.slotFor(carried, nearest);
        this.aim = this.restPoint(this.placeSlot < 0 ? nearest : this.placeSlot);
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
        return aim.m_82520_(0.0, 0.062 + hover - 0.06 * tap, 0.0).m_82546_(reach.m_82490_(0.075));
    }

    @Override
    public Vec3 heldItemPoint(Vec3 hand, Vec3 shoulder) {
        Vec3 reach = new Vec3(hand.f_82479_ - shoulder.f_82479_, 0.0, hand.f_82481_ - shoulder.f_82481_).m_82541_();
        return hand.m_82549_(reach.m_82490_(0.075)).m_82520_(0.0, -0.05, 0.0);
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
                Scene.sound(SoundEvents.f_11713_, 0.5f, 1.2f);
                break;
            }
            case ITEM: {
                if (button == 0 && this.hoverSlot == 0 && !this.slot(2).m_41619_()) {
                    this.work(shift);
                    break;
                }
                this.slotClick(this.hoverSlot, 0, shift);
                break;
            }
        }
    }

    private void work(boolean shift) {
        this.mapBefore = this.slot(0).m_255036_(1);
        this.addition = this.slot(1).m_255036_(1);
        this.result = this.slot(2).m_41777_();
        this.work = this.addition.m_150930_(Items.f_42516_) ? Work.ZOOM : (this.addition.m_150930_(Items.f_42027_) ? Work.LOCK : Work.COPY);
        this.toInventory = shift;
        this.gleamed = false;
        Minecraft mc = Minecraft.m_91087_();
        this.startMove(2.1, 1.55, () -> {
            CartographyTableMenu menu = (CartographyTableMenu)this.menu();
            if (this.slot(2).m_41619_()) {
                this.cancelMove();
                return;
            }
            mc.f_91072_.m_171799_(menu.f_38840_, 2, 0, this.toInventory ? ClickType.QUICK_MOVE : ClickType.PICKUP, (Player)mc.f_91074_);
        });
        Scene.sound(SoundEvents.f_11713_, 0.7f, 0.9f);
    }

    private float mapSize() {
        return 0.5f * this.surfaceScale(0);
    }

    private float additionSize() {
        return 0.24f * this.surfaceScale(1);
    }

    private float additionYaw(double t) {
        double blend = Anim.inOutCubic(Anim.window(t, 0.1, 0.55));
        return ItemPlacer.awayYaw(this.side) + (float)((double)this.surfaceRotation(1) + (double)(this.surfaceRotation(0) - this.surfaceRotation(1)) * blend);
    }

    private Vec3 additionPos(double t) {
        Vec3 from = this.restPoint(1);
        Vec3 to = switch (this.work) {
            default -> throw new IncompatibleClassChangeError();
            case Work.ZOOM -> this.restPoint(0).m_82520_(0.0, -0.002, 0.0);
            case Work.LOCK -> this.restPoint(0).m_82520_(0.0, 0.012, 0.0);
            case Work.COPY -> this.restPoint(0).m_82549_(this.surfaceRight(0).m_82490_(-0.02)).m_82549_(this.surfaceOut(0).m_82490_(0.02));
        };
        double k = Anim.inOutCubic(Anim.window(t, 0.1, 0.55));
        return Anim.lerp(from, to, k).m_82520_(0.0, (this.work == Work.LOCK ? 0.16 : 0.07) * Anim.hump(k), 0.0);
    }

    @Override
    @Nullable
    public Vec3[] handsOverride() {
        double t = this.moveTime();
        if (t < 0.0) {
            return null;
        }
        double carry = Anim.inOutCubic(Anim.window(t, 0.0, 0.12)) * (1.0 - Anim.inOutCubic(Anim.window(t, 0.6, 0.85)));
        Vec3 right = Anim.lerp(this.rest(true), this.additionPos(t).m_82549_(this.surfaceOut(0).m_82490_(0.075)).m_82520_(0.0, 0.082, 0.0), carry);
        Vec3 corner = this.restPoint(0).m_82549_(this.surfaceRight(0).m_82490_((double)(-this.mapSize()) * 0.4)).m_82549_(this.surfaceOut(0).m_82490_((double)this.mapSize() * 0.35)).m_82520_(0.0, 0.072, 0.0);
        double hold = Anim.inOutCubic(Anim.window(t, 0.05, 0.3)) * (1.0 - Anim.inOutCubic(Anim.window(t, 1.45, 1.6500000000000001)));
        Vec3 left = Anim.lerp(this.rest(false), corner, hold);
        if (t > 0.85 && t < 1.55) {
            double s = Anim.window(t, 0.85, 1.5);
            Vec3 sweep = this.restPoint(0).m_82549_(this.surfaceRight(0).m_82490_(-0.15 + 0.3 * s)).m_82549_(this.surfaceOut(0).m_82490_(0.08 * Math.sin(s * Math.PI * 3.0))).m_82520_(0.0, 0.082, 0.0);
            right = Anim.lerp(this.rest(true), sweep, Anim.hump(s) * 0.9 + 0.1);
        }
        if (t >= 1.45) {
            Vec3 grab = this.restPoint(0).m_82549_(this.surfaceOut(0).m_82490_(0.18)).m_82520_(0.0, 0.092, 0.0);
            right = Anim.lerp(Anim.lerp(right, grab, Anim.inOutCubic(Anim.window(t, 1.45, 1.6))), this.rest(true).m_82520_(0.0, this.toInventory ? 0.0 : 0.05, 0.0), Anim.inOutCubic(Anim.window(t, 1.6, 2.1)));
        }
        return new Vec3[]{right, left};
    }

    @Override
    void frame() {
        super.frame();
        double t = this.moveTime();
        if (t < 0.0) {
            return;
        }
        Vec3 map = this.restPoint(0).m_82520_(0.0, 0.02, 0.0);
        if (this.work == Work.COPY && t > 0.6 && t < 1.55 && Fx.RANDOM.m_188500_() < Scene.frameDt * 30.0) {
            Fx.motes(map, (double)this.mapSize() * 0.3, 1, 0.35f, 0.3f, 0.25f, 0.08);
        }
        if (!this.gleamed && t > 0.9) {
            this.gleamed = true;
            Scene.sound(this.work == Work.LOCK ? SoundEvents.f_11986_ : SoundEvents.f_11713_, 0.6f, this.work == Work.LOCK ? 1.4f : 1.0f);
            if (this.work == Work.LOCK) {
                Fx.motes(map, (double)this.mapSize() * 0.4, 12, 0.8f, 0.9f, 1.0f, 0.15);
            }
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
        float yaw = ItemPlacer.awayYaw(this.side) + this.surfaceRotation(0);
        if (t < 0.0) {
            CartographyStation.drawMap(pose, buffers, cam, this.slot(0), this.restPoint(0), yaw, this.mapSize() * (this.hoverSlot == 0 ? 1.03f : 1.0f), light);
            SmithingStation.drawLaid(pose, buffers, cam, this.slot(1), this.restPoint(1), this.side, 2, (this.hoverSlot == 1 ? 1.08f : 1.0f) * this.additionSize() / 0.2f, light, this.surfaceRotation(1));
            return;
        }
        ItemStack restAddition = t < 1.55 ? this.slot(1).m_255036_(Math.max(0, this.slot(1).m_41613_() - 1)) : this.slot(1);
        SmithingStation.drawLaid(pose, buffers, cam, restAddition, this.restPoint(1), this.side, 2, this.additionSize() / 0.2f, light, this.surfaceRotation(1));
        if (t < 1.55) {
            this.drawWorking(pose, buffers, camera, t, yaw, light);
            return;
        }
        double k = Anim.inOutCubic(Anim.window(t, 1.55, 2.1));
        Vec3 destination = this.toInventory ? cam.m_82520_(0.0, 0.5, 0.0).m_82549_(Scene.yawForward(camera.m_90590_()).m_82490_(0.6)) : this.heldItemPoint(Hands.position(true), Hands.shoulder(true, cam, camera.m_90590_()));
        Vec3 at = Anim.lerp(this.restPoint(0).m_82520_(0.0, 0.1 * Anim.hump(k), 0.0), destination, Anim.inCubic(k));
        float size = (float)((double)this.mapSize() * (1.0 - 0.7 * k));
        CartographyStation.drawMap(pose, buffers, cam, this.mapBefore, at, yaw, size, light);
        if (this.work == Work.COPY && this.result.m_41613_() > 1) {
            CartographyStation.drawMap(pose, buffers, cam, this.mapBefore, at.m_82549_(this.surfaceRight(0).m_82490_(0.03 * (1.0 - k))).m_82520_(0.0, -0.004, 0.0), yaw + 8.0f, size, light);
        }
    }

    private void drawWorking(PoseStack pose, MultiBufferSource buffers, Camera camera, double t, float yaw, int light) {
        Vec3 cam = camera.m_90583_();
        Vec3 map = this.restPoint(0);
        Vec3 add = this.additionPos(t);
        double settle = Anim.inOutCubic(Anim.window(t, 0.55, 1.25));
        switch (this.work) {
            case ZOOM: {
                float sheet = (float)((double)this.additionSize() + ((double)this.mapSize() * 1.04 - (double)this.additionSize()) * settle);
                ItemPlacer.drawOriented(pose, buffers, cam, this.addition, add.m_82520_(0.0, 0.002, 0.0), ItemPlacer.lying(this.additionYaw(t), 0.0f), sheet, light);
                CartographyStation.drawMap(pose, buffers, cam, this.mapBefore, map.m_82520_(0.0, 0.006, 0.0), yaw, (float)((double)this.mapSize() * (1.0 - 0.5 * settle)), light);
                break;
            }
            case LOCK: {
                CartographyStation.drawMap(pose, buffers, cam, this.mapBefore, map, yaw, this.mapSize(), light);
                double flip = 180.0 * Anim.inOutCubic(Anim.window(t, 0.15, 0.5));
                Quaternionf turn = new Quaternionf((Quaternionfc)ItemPlacer.lying(this.additionYaw(t), 0.0f)).premul((Quaternionfc)new Quaternionf().rotateAxis((float)Math.toRadians(flip), (float)this.surfaceRight((int)0).f_82479_, 0.0f, (float)this.surfaceRight((int)0).f_82481_));
                ItemPlacer.drawOriented(pose, buffers, cam, this.addition, add.m_82520_(0.0, 0.006, 0.0), turn, (float)((double)this.additionSize() + ((double)this.mapSize() * 1.02 - (double)this.additionSize()) * settle), light);
                if (!(t > 0.9)) break;
                double g = Anim.window(t, 0.9, 1.4);
                float b = (float)(0.4 * Anim.hump(g));
                Vec3 at = map.m_82549_(this.surfaceRight(0).m_82490_((double)(-this.mapSize() / 2.0f) + (double)this.mapSize() * g)).m_82549_(this.surfaceOut(0).m_82490_((double)this.mapSize() * (0.5 - g) * 0.6)).m_82520_(0.0, 0.02, 0.0);
                EnchantFx.billboard(pose, buffers, camera, at, 0.08, 0.0, EnchantFx.GLOW, b, b, b);
                break;
            }
            case COPY: {
                CartographyStation.drawMap(pose, buffers, cam, this.mapBefore, map.m_82549_(this.surfaceRight(0).m_82490_(-0.06 * settle)), yaw - 6.0f * (float)settle, this.mapSize(), light);
                Vec3 copy = add.m_82549_(this.surfaceRight(0).m_82490_(0.08 * settle)).m_82549_(this.surfaceOut(0).m_82490_(-0.02 * settle)).m_82520_(0.0, 0.004, 0.0);
                double traced = Anim.smoother(Anim.window(t, 0.85, 1.5));
                if (traced < 0.5) {
                    ItemPlacer.drawOriented(pose, buffers, cam, this.addition, copy, ItemPlacer.lying(this.additionYaw(t) + 6.0f, 0.0f), (float)((double)this.additionSize() + (double)(this.mapSize() - this.additionSize()) * settle), light);
                } else {
                    CartographyStation.drawMap(pose, buffers, cam, this.mapBefore, copy, yaw + 6.0f, this.mapSize(), light);
                }
                if (!(t > 0.85)) break;
                float b = (float)(0.3 * Anim.hump(traced));
                EnchantFx.billboard(pose, buffers, camera, Anim.lerp(map, copy, traced).m_82520_(0.0, 0.03, 0.0), 0.07, 0.0, EnchantFx.GLOW, b, b * 0.9f, b * 0.7f);
            }
        }
    }

    static void drawMap(PoseStack pose, MultiBufferSource buffers, Vec3 cam, ItemStack stack, Vec3 at, float yaw, float size, int light) {
        MapItemSavedData data;
        if (stack.m_41619_()) {
            return;
        }
        Minecraft mc = Minecraft.m_91087_();
        Integer id = MapItem.m_151131_((ItemStack)stack);
        MapItemSavedData mapItemSavedData = data = id == null || mc.f_91073_ == null ? null : MapItem.m_151128_((Integer)id, (Level)mc.f_91073_);
        if (data == null) {
            ItemPlacer.drawOriented(pose, buffers, cam, stack, at.m_82520_(0.0, 0.004, 0.0), ItemPlacer.lying(yaw, 0.0f), size, light);
            return;
        }
        pose.m_85836_();
        pose.m_85837_(at.f_82479_ - cam.f_82479_, at.f_82480_ + 0.004 - cam.f_82480_, at.f_82481_ - cam.f_82481_);
        pose.m_252781_(Axis.f_252436_.m_252977_(yaw));
        pose.m_252781_(Axis.f_252529_.m_252977_(90.0f));
        float s = size / 128.0f;
        pose.m_85841_(s, s, s);
        pose.m_252880_(-64.0f, -64.0f, 0.0f);
        VertexConsumer paper = buffers.m_6299_(RenderType.m_110497_((ResourceLocation)MAP_BACKGROUND));
        Matrix4f m = pose.m_85850_().m_252922_();
        paper.m_252986_(m, -7.0f, 135.0f, 0.005f).m_6122_(255, 255, 255, 255).m_7421_(0.0f, 1.0f).m_85969_(light).m_5752_();
        paper.m_252986_(m, 135.0f, 135.0f, 0.005f).m_6122_(255, 255, 255, 255).m_7421_(1.0f, 1.0f).m_85969_(light).m_5752_();
        paper.m_252986_(m, 135.0f, -7.0f, 0.005f).m_6122_(255, 255, 255, 255).m_7421_(1.0f, 0.0f).m_85969_(light).m_5752_();
        paper.m_252986_(m, -7.0f, -7.0f, 0.005f).m_6122_(255, 255, 255, 255).m_7421_(0.0f, 0.0f).m_85969_(light).m_5752_();
        mc.f_91063_.m_109151_().m_168771_(pose, buffers, id.intValue(), data, false, light);
        pose.m_85849_();
    }

    @Override
    public void renderWorld(PoseStack pose, MultiBufferSource buffers, Vec3 camera, int light) {
        this.gridEditor.renderWorld(pose, buffers, camera, light);
        if (this.gridEditor.visible()) {
            return;
        }
        if (this.target == Target.PLACE && this.aim != null) {
            float p = (float)(0.18 + 0.06 * Math.sin(Scene.clock() * 5.0));
            boolean ok = this.placeSlot >= 0;
            EnchantFx.disc(pose, buffers, camera, this.aim.m_82520_(0.0, 0.004, 0.0), this.placeSlot == 0 ? 0.26 * (double)this.surfaceScale(0) : 0.12 * (double)this.surfaceScale(1), 0.0, EnchantFx.GLOW, p, ok ? p * 0.8f : p * 0.2f, ok ? p * 0.6f : p * 0.2f);
        }
    }

    static void drawContents(PoseStack pose, MultiBufferSource buffers, Vec3 cam, BlockPos pos, Direction side, List<ItemStack> items, int light) {
        Vec3 top = new Vec3((double)pos.m_123341_() + 0.5, (double)pos.m_123342_() + 1.0, (double)pos.m_123343_() + 0.5);
        if (!items.isEmpty()) {
            CartographyStation.drawMap(pose, buffers, cam, items.get(0), CartographyStation.slotPoint(top, side, 0), ItemPlacer.awayYaw(side) + (float)SurfaceLayouts.current(StationKind.CARTOGRAPHY).region(0).rotation(), 0.5f * (float)SurfaceLayouts.current(StationKind.CARTOGRAPHY).region(0).itemScale(), light);
        }
        if (items.size() > 1) {
            SmithingStation.drawLaid(pose, buffers, cam, items.get(1), CartographyStation.slotPoint(top, side, 1), side, 2, 1.1999999f * (float)SurfaceLayouts.current(StationKind.CARTOGRAPHY).region(1).itemScale(), light, (float)SurfaceLayouts.current(StationKind.CARTOGRAPHY).region(1).rotation());
        }
    }

    @Override
    @Nullable
    public List<Component> tooltip(ItemStack carried) {
        double[] mouse = Picking.mouseGui();
        List<Component> editorTip = this.gridEditor.tooltip(mouse[0], mouse[1]);
        if (editorTip != null || this.gridEditor.editing()) {
            return editorTip;
        }
        Minecraft mc = Minecraft.m_91087_();
        if (this.target == Target.PLACE) {
            return this.placeSlot < 0 ? List.of(Component.m_237115_((String)"cinematic.cartography.refuse").m_130940_(ChatFormatting.RED)) : List.of(Component.m_237115_((String)(this.placeSlot == 0 ? "cinematic.cartography.slot.map" : "cinematic.cartography.slot.addition")).m_130940_(ChatFormatting.GRAY));
        }
        if (this.target != Target.ITEM) {
            return null;
        }
        ArrayList<Component> lines = new ArrayList<Component>(Screen.m_280152_((Minecraft)mc, (ItemStack)this.slot(this.hoverSlot)));
        lines.add((Component)Component.m_237119_());
        ItemStack addition = this.slot(1);
        if (!this.slot(2).m_41619_() && this.hoverSlot == 0) {
            String key = addition.m_150930_(Items.f_42516_) ? "zoom" : (addition.m_150930_(Items.f_42027_) ? "lock" : "copy");
            lines.add((Component)Component.m_237115_((String)("cinematic.cartography." + key)).m_130940_(ChatFormatting.GRAY));
            lines.add((Component)Component.m_237115_((String)"cinematic.cartography.work").m_130940_(ChatFormatting.GOLD));
        } else {
            if (this.hoverSlot == 0 && addition.m_41619_()) {
                lines.add((Component)Component.m_237115_((String)"cinematic.cartography.add").m_130940_(ChatFormatting.DARK_GRAY));
            } else if (!addition.m_41619_() && this.slot(2).m_41619_() && !this.slot(0).m_41619_()) {
                lines.add((Component)Component.m_237115_((String)"cinematic.cartography.cannot").m_130940_(ChatFormatting.RED));
            }
            lines.add((Component)Component.m_237115_((String)"cinematic.cartography.take").m_130940_(ChatFormatting.GRAY));
        }
        return lines;
    }

    @Override
    public Component hint() {
        if (this.gridEditor.editing()) {
            return SurfaceEditor.text("hint", new Object[0]);
        }
        return Component.m_237115_((String)"cinematic.hint.cartography");
    }

    public static enum Target {
        NONE,
        PLACE,
        ITEM;

    }

    private static enum Work {
        ZOOM,
        LOCK,
        COPY;

    }
}


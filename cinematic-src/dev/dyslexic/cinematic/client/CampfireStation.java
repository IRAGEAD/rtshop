/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.math.Axis
 *  javax.annotation.Nullable
 *  net.minecraft.ChatFormatting
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.NonNullList
 *  net.minecraft.core.Position
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.world.Container
 *  net.minecraft.world.SimpleContainer
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.crafting.RecipeType
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.CampfireBlock
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.CampfireBlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Vector3f
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dyslexic.cinematic.client.Anim;
import dev.dyslexic.cinematic.client.CameraRig;
import dev.dyslexic.cinematic.client.ClientNet;
import dev.dyslexic.cinematic.client.ClientStation;
import dev.dyslexic.cinematic.client.EnchantFx;
import dev.dyslexic.cinematic.client.Fx;
import dev.dyslexic.cinematic.client.GrindstoneStation;
import dev.dyslexic.cinematic.client.Hands;
import dev.dyslexic.cinematic.client.LegMesh;
import dev.dyslexic.cinematic.client.Picking;
import dev.dyslexic.cinematic.client.Reach;
import dev.dyslexic.cinematic.client.RemainingLayouts;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.SpatialAppearance;
import dev.dyslexic.cinematic.client.SpatialEditor;
import dev.dyslexic.cinematic.client.SpatialLayouts;
import dev.dyslexic.cinematic.client.SpatialTarget;
import dev.dyslexic.cinematic.net.CampfireProgressPayload;
import dev.dyslexic.cinematic.net.StationActionPayload;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Position;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class CampfireStation
extends ClientStation
implements SpatialTarget {
    final SpatialEditor gridEditor = new SpatialEditor(this);
    static final double SEAT = 1.6;
    static final double EYE = 0.8;
    static final double HAND_Y = 0.055;
    static final double ITEM_REACH = 0.07;
    public final Vec3 fire;
    private final Vec3 toward;
    private final Vec3 across;
    private final Vec3 seat;
    private final Direction facing;
    public Target target = Target.NONE;
    public int hoverSpot = -1;
    private int placeSpot = -1;
    private double leanIn;
    @Nullable
    private double[] lastMouse;
    private final int[] cookingProgress = new int[4];
    private final int[] cookingTime = new int[4];
    private final double[] shownProgress = new double[4];
    private boolean cookingLit;
    private double progressReceivedAt;
    static final double LEAN_IN = 0.85;

    @Override
    public SpatialEditor layoutEditor() {
        return this.gridEditor;
    }

    public CampfireStation(BlockPos pos, LocalPlayer player) {
        super(pos, CampfireStation.sideOf(pos, player));
        this.fire = new Vec3((double)pos.m_123341_() + 0.5, (double)pos.m_123342_(), (double)pos.m_123343_() + 0.5);
        Vec3 out = CampfireStation.outward(pos, player);
        this.toward = out.m_82490_(-1.0);
        this.across = new Vec3(-this.toward.f_82481_, 0.0, this.toward.f_82479_);
        double ground = Math.abs(player.m_20186_() - (double)pos.m_123342_()) < 0.6 ? player.m_20186_() : (double)pos.m_123342_();
        Vec3 at = this.fire.m_82549_(out.m_82490_(1.6));
        this.seat = new Vec3(at.f_82479_, ground, at.f_82481_);
        BlockState state = player.m_9236_().m_8055_(pos);
        this.facing = state.m_61138_((Property)CampfireBlock.f_51230_) ? (Direction)state.m_61143_((Property)CampfireBlock.f_51230_) : Direction.NORTH;
    }

    private static Vec3 outward(BlockPos pos, LocalPlayer player) {
        Vec3 out = new Vec3(player.m_20185_() - ((double)pos.m_123341_() + 0.5), 0.0, player.m_20189_() - ((double)pos.m_123343_() + 0.5));
        if (out.m_82556_() < 1.0E-4) {
            out = Scene.yawForward(player.m_146908_()).m_82490_(-1.0);
        }
        return out.m_82541_();
    }

    private static Direction sideOf(BlockPos pos, LocalPlayer player) {
        Vec3 out = CampfireStation.outward(pos, player);
        return Direction.m_122366_((double)out.f_82479_, (double)0.0, (double)out.f_82481_);
    }

    @Override
    public StationKind kind() {
        return StationKind.CAMPFIRE;
    }

    @Override
    public Vec3 center() {
        return this.fire.m_82520_(0.0, 0.45, 0.0);
    }

    @Override
    public Vec3 forward() {
        return this.toward;
    }

    @Override
    public Vec3 right() {
        return this.across;
    }

    @Override
    public Vec3 side() {
        return this.toward.m_82490_(-1.0);
    }

    @Override
    public SoundEvent touchSound() {
        return SoundEvents.f_11992_;
    }

    @Override
    public BlockPos lightPos() {
        return BlockPos.m_274446_((Position)this.seat.m_82520_(0.0, 0.5, 0.0));
    }

    static Vec3 vanillaSpot(BlockPos pos, Direction facing, int j) {
        Vector3f v = new Vector3f(-0.3125f, -0.3125f, 0.0f);
        Axis.f_252529_.m_252977_(90.0f).transform(v);
        Direction d = Direction.m_122407_((int)((j + facing.m_122416_()) % 4));
        Axis.f_252436_.m_252977_(-d.m_122435_()).transform(v);
        return new Vec3((double)pos.m_123341_() + 0.5 + (double)v.x, (double)pos.m_123342_() + 0.44921875 + (double)v.y, (double)pos.m_123343_() + 0.5 + (double)v.z);
    }

    static Vec3 spot(BlockPos pos, Direction facing, int j) {
        return SpatialLayouts.current(StationKind.CAMPFIRE).point(RemainingLayouts.frame(StationKind.CAMPFIRE, pos, facing), j);
    }

    Vec3 spot(int j) {
        return CampfireStation.spot(this.pos, this.facing, j);
    }

    private List<ItemStack> food() {
        NonNullList nonNullList;
        BlockEntity blockEntity;
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91073_ != null && (blockEntity = mc.f_91073_.m_7702_(this.pos)) instanceof CampfireBlockEntity) {
            CampfireBlockEntity campfire = (CampfireBlockEntity)blockEntity;
            nonNullList = campfire.m_59065_();
        } else {
            nonNullList = List.of();
        }
        return nonNullList;
    }

    void applyProgress(CampfireProgressPayload payload) {
        this.cookingLit = payload.lit();
        this.progressReceivedAt = Scene.clock();
        for (int i = 0; i < 4; ++i) {
            int nextTotal = Math.max(0, payload.total()[i]);
            int nextProgress = Math.max(0, payload.progress()[i]);
            if (nextTotal == 0 || nextTotal != this.cookingTime[i] || this.cookingLit && nextProgress + 2 < this.cookingProgress[i]) {
                this.shownProgress[i] = 0.0;
            }
            this.cookingTime[i] = nextTotal;
            this.cookingProgress[i] = nextProgress;
        }
    }

    private double cookingFraction(int slot) {
        int total = this.cookingTime[slot];
        if (total <= 0) {
            return 0.0;
        }
        double elapsed = Math.max(0.0, Math.min(0.25, Scene.clock() - this.progressReceivedAt));
        double progress = (double)this.cookingProgress[slot] + elapsed * (double)(this.cookingLit ? 20 : -40);
        return Math.max(0.0, Math.min((double)(total - 1), progress)) / (double)total;
    }

    int syncedProgress(int slot) {
        return this.cookingProgress[slot];
    }

    int syncedCookingTime(int slot) {
        return this.cookingTime[slot];
    }

    boolean syncedLit() {
        return this.cookingLit;
    }

    @Override
    public List<Vec3> approach(LocalPlayer player) {
        boolean walk = Math.hypot(this.seat.f_82479_ - player.m_20185_(), this.seat.f_82481_ - player.m_20189_()) > 0.06;
        return walk ? List.of(this.seat) : List.of();
    }

    @Override
    public CameraRig.Pose lean() {
        Vec3 eye = this.seat.m_82520_(0.0, 0.8 - 0.1 * this.leanIn, 0.0).m_82549_(this.toward.m_82490_(-0.05 + 0.85 * this.leanIn));
        Vec3 look = this.fire.m_82520_(0.0, 0.18, 0.0).m_82546_(eye);
        float yaw = (float)Math.toDegrees(Math.atan2(-look.f_82479_, look.f_82481_));
        float pitch = (float)(-Math.toDegrees(Math.atan2(look.f_82480_, Math.hypot(look.f_82479_, look.f_82481_))));
        return new CameraRig.Pose(eye, yaw, pitch, 0.0f);
    }

    @Override
    public double leanFov() {
        return 68.0;
    }

    @Override
    public boolean workable(Vec3 feet) {
        return Math.hypot(feet.f_82479_ - this.seat.f_82479_, feet.f_82481_ - this.seat.f_82481_) < 0.3 && Math.abs(feet.f_82480_ - this.fire.f_82480_) < 0.6;
    }

    @Override
    public boolean inFront(Vec3 feet) {
        return true;
    }

    @Override
    public boolean crouchesFor(double cameraHeight) {
        return false;
    }

    static boolean sittable(LocalPlayer player, BlockPos pos) {
        return Reach.plan(new CampfireStation(pos, player), player).scene();
    }

    @Override
    public double maxReach() {
        return 1.0;
    }

    @Override
    public float bodyPitch() {
        return 22.0f;
    }

    private double seated() {
        return switch (Scene.phase) {
            case Scene.Phase.INTRO -> Anim.smoother(Anim.window(Scene.introProgress(), 0.4, 0.95));
            case Scene.Phase.ACTIVE -> 1.0;
            case Scene.Phase.OUTRO -> 1.0 - Anim.smoother(Anim.window(Scene.time / 1.05, 0.0, 0.45));
            default -> 0.0;
        };
    }

    private Vec3 at(Vec3 base, double fwd, double right, double up) {
        return base.m_82549_(this.toward.m_82490_(fwd)).m_82549_(this.across.m_82490_(right)).m_82520_(0.0, up, 0.0);
    }

    private Vec3 kneeRight() {
        return this.at(this.seat, 0.36, 0.17, 0.46);
    }

    private Vec3 kneeLeft() {
        return this.at(this.seat, 0.45, -0.13, 0.13);
    }

    @Override
    public Vec3 rest(boolean right) {
        return right ? this.kneeRight().m_82549_(this.toward.m_82490_(0.02)).m_82520_(0.0, 0.075, 0.0) : this.kneeLeft().m_82549_(this.toward.m_82490_(-0.08)).m_82520_(0.0, 0.085, 0.0);
    }

    @Override
    public void renderBody(PoseStack pose, MultiBufferSource buffers, Vec3 camera, int light) {
        Minecraft mc = Minecraft.m_91087_();
        LocalPlayer player = mc.f_91074_;
        if (player == null) {
            return;
        }
        double want = Scene.phase == Scene.Phase.ACTIVE && this.aim != null ? Anim.clamp01((Math.hypot(this.aim.f_82479_ - this.seat.f_82479_, this.aim.f_82481_ - this.seat.f_82481_) - 1.15) / 0.7) : 0.0;
        this.leanIn = Anim.approach(this.leanIn, want, want > this.leanIn ? 5.0 : 3.0, Scene.frameDt);
        double s = this.seated();
        if (s <= 0.001) {
            return;
        }
        Vec3 feet = player.m_20318_(mc.m_91296_());
        Vec3 base = Anim.lerp(new Vec3(feet.f_82479_, this.seat.f_82480_, feet.f_82481_), this.seat, s);
        for (boolean right : new boolean[]{true, false}) {
            double r = right ? 1.0 : -1.0;
            Vec3 standHip = this.at(base, 0.0, 0.12 * r, 0.75);
            Vec3 standKnee = this.at(base, 0.0, 0.12 * r, 0.38);
            Vec3 standFoot = this.at(base, 0.0, 0.12 * r, 0.02);
            Vec3 hip = right ? this.at(this.seat, -0.02, 0.1, 0.14) : this.at(this.seat, 0.0, -0.1, 0.12);
            Vec3 knee = right ? this.kneeRight() : this.kneeLeft();
            Vec3 foot = right ? this.at(this.seat, 0.56, 0.15, 0.05) : this.at(this.seat, 0.7, -0.15, 0.07);
            LegMesh.draw(pose, buffers, camera, player, right, Anim.lerp(standHip, hip, s), Anim.lerp(standKnee, knee, s), Anim.lerp(standFoot, foot, s), this.across.m_82490_(r), light);
        }
        if (Scene.phase == Scene.Phase.ACTIVE && this.lit() && Fx.RANDOM.m_188500_() < Scene.frameDt * 5.0) {
            if (this.soul()) {
                Fx.motes(this.fire.m_82520_(0.0, 0.45, 0.0), 0.18, 1, 0.25f, 0.85f, 1.0f, 0.45);
            } else {
                Fx.motes(this.fire.m_82520_(0.0, 0.45, 0.0), 0.18, 1, 1.0f, 0.55f, 0.2f, 0.45);
            }
        }
    }

    private boolean soul() {
        Minecraft mc = Minecraft.m_91087_();
        return mc.f_91073_ != null && mc.f_91073_.m_8055_(this.pos).m_60713_(Blocks.f_50684_);
    }

    private boolean lit() {
        Minecraft mc = Minecraft.m_91087_();
        BlockState state = mc.f_91073_ == null ? null : mc.f_91073_.m_8055_(this.pos);
        return state != null && state.m_61138_((Property)CampfireBlock.f_51227_) && (Boolean)state.m_61143_((Property)CampfireBlock.f_51227_) != false;
    }

    @Override
    public void updateCursor(boolean overPanel) {
        int was = this.hoverSpot;
        int previousPlace = this.placeSpot;
        this.clearCursor();
        double[] mouse = Picking.mouseGui();
        boolean still = this.lastMouse != null && this.lastMouse[0] == mouse[0] && this.lastMouse[1] == mouse[1];
        this.lastMouse = mouse;
        if (overPanel) {
            return;
        }
        List<ItemStack> food = this.food();
        if (food.size() < 4) {
            return;
        }
        ItemStack carried = Hands.carried();
        if (!this.geometry().equals(SpatialAppearance.VANILLA)) {
            int picked = this.layoutPick();
            if (carried.m_41619_() && was >= 0 && !food.get(was).m_41619_() && (still || GrindstoneStation.under(mouse, this.spot(was), 0.25 * (double)this.geometry().scale(was)))) {
                picked = was;
            } else if (!carried.m_41619_() && previousPlace >= 0 && still) {
                picked = previousPlace;
            }
            if (picked >= 0) {
                if (carried.m_41619_() && !food.get(picked).m_41619_()) {
                    this.target = Target.FOOD;
                    this.hoverSpot = picked;
                    this.aim = this.spot(picked);
                } else if (!carried.m_41619_()) {
                    this.target = Target.PLACE;
                    boolean cookable = CampfireStation.cookable(carried);
                    this.placeSpot = cookable && food.get(picked).m_41619_() ? picked : -1;
                    this.aim = this.spot(picked);
                }
            }
            return;
        }
        if (carried.m_41619_()) {
            for (int k = -1; k < 4; ++k) {
                int j;
                int n = j = k < 0 ? was : k;
                if (j < 0 || food.get(j).m_41619_() || (k >= 0 || !still) && !GrindstoneStation.under(mouse, this.spot(j), k < 0 ? 0.25 : 0.1)) continue;
                this.target = Target.FOOD;
                this.hoverSpot = j;
                this.aim = this.spot(j);
                return;
            }
            return;
        }
        Vec3 hit = Picking.cursorOnPlane(this.spot((int)0).f_82480_);
        if (hit == null || Math.hypot(hit.f_82479_ - this.fire.f_82479_, hit.f_82481_ - this.fire.f_82481_) > 0.6) {
            return;
        }
        this.target = Target.PLACE;
        boolean cookable = CampfireStation.cookable(carried);
        double best = Double.MAX_VALUE;
        for (int j = 0; j < 4 && cookable; ++j) {
            double d = hit.m_82557_(this.spot(j));
            if (!food.get(j).m_41619_() || !(d < best)) continue;
            best = d;
            this.placeSpot = j;
        }
        this.aim = this.placeSpot >= 0 ? this.spot(this.placeSpot) : hit;
    }

    @Override
    public void clearCursor() {
        super.clearCursor();
        this.target = Target.NONE;
        this.hoverSpot = -1;
        this.placeSpot = -1;
    }

    @Override
    public Vec3 handTarget(Vec3 aim, Vec3 shoulder, boolean carrying, double tap) {
        Vec3 reach = new Vec3(aim.f_82479_ - shoulder.f_82479_, 0.0, aim.f_82481_ - shoulder.f_82481_).m_82541_();
        double hover = carrying ? 0.08 : 0.05;
        return aim.m_82520_(0.0, 0.055 + hover - 0.06 * tap, 0.0).m_82546_(reach.m_82490_(0.07));
    }

    @Override
    public Vec3 heldItemPoint(Vec3 hand, Vec3 shoulder) {
        Vec3 reach = new Vec3(hand.f_82479_ - shoulder.f_82479_, 0.0, hand.f_82481_ - shoulder.f_82481_).m_82541_();
        return hand.m_82549_(reach.m_82490_(0.07)).m_82520_(0.0, -0.043, 0.0);
    }

    @Override
    public void click(int button, boolean shift) {
        int index;
        int n = this.target == Target.FOOD ? this.hoverSpot : (index = this.target == Target.PLACE ? this.placeSpot : -1);
        if (index < 0) {
            return;
        }
        Hands.tap();
        ClientNet.sendToServer(new StationActionPayload(this.pos, StationKind.CAMPFIRE, StationActionPayload.Action.CLICK, index, button, shift, this.sideDirection().m_122416_()));
        Scene.sound(this.target == Target.FOOD ? SoundEvents.f_12019_ : SoundEvents.f_12635_, 0.4f, this.target == Target.FOOD ? 1.4f : 1.2f);
    }

    @Override
    public void renderWorld(PoseStack pose, MultiBufferSource buffers, Vec3 camera, int light) {
        this.gridEditor.renderWorld(pose, buffers, camera, light);
        if (this.aim == null || this.target == Target.NONE) {
            return;
        }
        float p = (float)(0.2 + 0.07 * Math.sin(Scene.clock() * 5.0));
        boolean ok = this.target == Target.FOOD || this.placeSpot >= 0;
        EnchantFx.disc(pose, buffers, camera, this.aim.m_82520_(0.0, 0.02, 0.0), 0.14, 0.0, EnchantFx.GLOW, p, ok ? p * 0.75f : p * 0.2f, ok ? p * 0.4f : p * 0.2f);
    }

    @Override
    @Nullable
    public List<Component> tooltip(ItemStack carried) {
        double[] pointer = Picking.mouseGui();
        List<Component> editorTip = this.gridEditor.tooltip(pointer[0], pointer[1]);
        if (editorTip != null) {
            return editorTip;
        }
        if (this.target == Target.FOOD) {
            ArrayList<Component> lines = new ArrayList<Component>(Screen.m_280152_((Minecraft)Minecraft.m_91087_(), (ItemStack)this.food().get(this.hoverSpot)));
            if (this.cookingTime[this.hoverSpot] > 0) {
                int percent = (int)Math.round(this.cookingFraction(this.hoverSpot) * 100.0);
                int seconds = (int)Math.ceil(((double)this.cookingTime[this.hoverSpot] - this.cookingFraction(this.hoverSpot) * (double)this.cookingTime[this.hoverSpot]) / 20.0);
                MutableComponent status = this.cookingLit ? Component.m_237110_((String)"cinematic.campfire.cooking", (Object[])new Object[]{percent, seconds}) : Component.m_237110_((String)"cinematic.campfire.cooling", (Object[])new Object[]{percent});
                lines.add((Component)status.m_130940_(ChatFormatting.GRAY));
            }
            lines.add((Component)Component.m_237115_((String)"cinematic.campfire.take").m_130940_(ChatFormatting.GRAY));
            return lines;
        }
        if (this.target == Target.PLACE && this.placeSpot < 0) {
            return List.of(Component.m_237115_((String)(CampfireStation.cookable(carried) ? "cinematic.campfire.full" : "cinematic.campfire.not_food")).m_130940_(ChatFormatting.RED));
        }
        return null;
    }

    @Override
    public Component hint() {
        return Component.m_237115_((String)"cinematic.hint.campfire");
    }

    @Override
    public boolean stillThere(Level level) {
        return this.kind().matches(level.m_8055_(this.pos));
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
        this.renderCookingProgress(g, alpha);
        this.gridEditor.render(g, font, alpha);
    }

    private void renderCookingProgress(GuiGraphics g, float alpha) {
        if (Scene.phase != Scene.Phase.ACTIVE || this.gridEditor.visible()) {
            return;
        }
        List<ItemStack> items = this.food();
        if (items.size() < 4) {
            return;
        }
        boolean soul = this.soul();
        for (int i = 0; i < 4; ++i) {
            if (items.get(i).m_41619_() || this.cookingTime[i] <= 0) continue;
            this.shownProgress[i] = Anim.approach(this.shownProgress[i], this.cookingFraction(i), 14.0, Scene.frameDt);
            double[] at = Picking.toGui(this.spot(i).m_82520_(0.0, 0.02, 0.0));
            if (at == null) continue;
            int x = (int)Math.round(at[0]) - 15;
            int y = (int)Math.round(at[1]) - 8;
            if (this.hoverSpot == i) {
                y = Math.min(y, (int)Math.floor(Picking.mouseGui()[1]) - 24);
            }
            if (x < 2 || x + 30 >= g.m_280182_() - 2 || y < 18 || y > g.m_280206_() - 26) continue;
            float a = alpha * (this.hoverSpot == i ? 1.0f : 0.82f);
            g.m_280509_(x - 2, y - 2, x + 32, y + 7, CampfireStation.color(a * 0.55f, 12, 16, 19));
            g.m_280509_(x, y, x + 30, y + 4, CampfireStation.color(a * 0.5f, 75, 80, 82));
            int filled = (int)Math.round(30.0 * this.shownProgress[i]);
            if (filled <= 0) continue;
            int fill = this.cookingLit ? (soul ? CampfireStation.color(a, 103, 211, 231) : CampfireStation.color(a, 255, 188, 99)) : CampfireStation.color(a, 155, 176, 183);
            g.m_280509_(x, y, x + filled, y + 4, fill);
        }
    }

    private static boolean cookable(ItemStack carried) {
        ClientLevel level = Minecraft.m_91087_().f_91073_;
        return level.m_7465_().m_44015_(RecipeType.f_44111_, (Container)new SimpleContainer(new ItemStack[]{carried}), (Level)level).isPresent();
    }

    private static int color(float alpha, int red, int green, int blue) {
        return Math.max(0, Math.min(255, Math.round(alpha * 255.0f))) << 24 | red << 16 | green << 8 | blue;
    }

    @Override
    public List<Vec3> workSamples() {
        return RemainingLayouts.samples(this, super.workSamples());
    }

    @Override
    public float layoutHeldScale() {
        int slot = this.target == Target.FOOD ? this.hoverSpot : (this.target == Target.PLACE ? this.placeSpot : -1);
        return slot < 0 ? 1.0f : this.geometry().scale(slot);
    }

    public static enum Target {
        NONE,
        PLACE,
        FOOD;

    }
}


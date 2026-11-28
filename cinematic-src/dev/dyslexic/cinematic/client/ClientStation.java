/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  javax.annotation.Nullable
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Vec3i
 *  net.minecraft.network.chat.Component
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.client.CameraRig;
import dev.dyslexic.cinematic.client.ClientNet;
import dev.dyslexic.cinematic.client.QuickMoves;
import dev.dyslexic.cinematic.client.Reach;
import dev.dyslexic.cinematic.client.RecipeChoiceStrip;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.net.StationActionPayload;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public abstract class ClientStation {
    public final BlockPos pos;
    protected final Direction side;
    @Nullable
    public Vec3 aim;
    public double swing;

    protected ClientStation(BlockPos pos, Direction side) {
        this.pos = pos.m_7949_();
        this.side = side;
    }

    public abstract StationKind kind();

    public Direction sideDirection() {
        return this.side;
    }

    public Vec3 side() {
        return Vec3.m_82528_((Vec3i)this.side.m_122436_());
    }

    public Vec3 forward() {
        return Vec3.m_82528_((Vec3i)this.side.m_122424_().m_122436_());
    }

    public Vec3 right() {
        return Vec3.m_82528_((Vec3i)this.side.m_122424_().m_122427_().m_122436_());
    }

    public Vec3 center() {
        return Vec3.m_82512_((Vec3i)this.pos);
    }

    static Direction freeSide(Level level, BlockPos pos, LocalPlayer player, StationKind kind) {
        double dx = player.m_20185_() - ((double)pos.m_123341_() + 0.5);
        double dz = player.m_20189_() - ((double)pos.m_123343_() + 0.5);
        List<Direction> sides = kind.sides(level.m_8055_(pos));
        for (int pass = 0; pass < 2; ++pass) {
            Direction best = null;
            double bestScore = -1.7976931348623157E308;
            for (Direction candidate : sides) {
                double score = (double)candidate.m_122429_() * dx + (double)candidate.m_122431_() * dz;
                boolean room = pass == 0 ? Reach.standable(level, (Entity)player, new Vec3((double)pos.m_123341_() + 0.5 + (double)candidate.m_122429_(), player.m_20186_(), (double)pos.m_123343_() + 0.5 + (double)candidate.m_122431_())) : StationKind.roomAt((BlockGetter)level, pos.m_121945_(candidate));
                if (!room || !(score > bestScore)) continue;
                bestScore = score;
                best = candidate;
            }
            if (best == null) continue;
            return best;
        }
        Direction facing = Direction.m_122364_((double)player.m_146908_()).m_122424_();
        return sides.contains(facing) ? facing : sides.get(0);
    }

    static List<Direction> sidesByNearness(Level level, BlockPos pos, LocalPlayer player, StationKind kind) {
        double dx = player.m_20185_() - ((double)pos.m_123341_() + 0.5);
        double dz = player.m_20189_() - ((double)pos.m_123343_() + 0.5);
        ArrayList<Direction> sides = new ArrayList<Direction>(kind.sides(level.m_8055_(pos)));
        sides.sort(Comparator.comparingDouble(side -> -((double)side.m_122429_() * dx + (double)side.m_122431_() * dz)));
        return sides;
    }

    public abstract List<Vec3> approach(LocalPlayer var1);

    public boolean crouches() {
        return false;
    }

    public boolean crouchesFor(double cameraHeight) {
        return this.crouches() ? cameraHeight < 1.4 : cameraHeight < 1.0;
    }

    public Vec3 workPoint() {
        return this.center();
    }

    public boolean workOnTop() {
        return true;
    }

    public List<Vec3> workSamples() {
        Vec3 work = this.workPoint();
        if (this.workOnTop()) {
            Vec3 lift = new Vec3(0.0, 0.04, 0.0);
            Vec3 a = this.right().m_82490_(0.3);
            Vec3 b = this.side().m_82490_(0.3);
            return List.of(work.m_82549_(lift), work.m_82549_(a).m_82549_(lift), work.m_82546_(a).m_82549_(lift), work.m_82549_(b).m_82549_(lift), work.m_82546_(b).m_82549_(lift));
        }
        Vec3 out = this.side().m_82490_(0.04);
        Vec3 a = this.right().m_82490_(0.3);
        Vec3 b = new Vec3(0.0, 0.3, 0.0);
        return List.of(work.m_82549_(out), work.m_82549_(a).m_82549_(out), work.m_82546_(a).m_82549_(out), work.m_82549_(b).m_82549_(out), work.m_82546_(b).m_82549_(out));
    }

    public boolean workable(Vec3 feet) {
        return Reach.inReach(this.workPoint(), this.workOnTop(), feet);
    }

    public boolean inFront(Vec3 feet) {
        Vec3 off = feet.m_82546_(this.workPoint());
        return off.f_82479_ * this.side().f_82479_ + off.f_82481_ * this.side().f_82481_ >= 0.2;
    }

    public boolean freeCamera() {
        return false;
    }

    public abstract CameraRig.Pose lean();

    public final CameraRig.Pose view() {
        CameraRig.Pose lean = this.lean();
        if (Math.abs(this.swing) < 0.001) {
            return lean;
        }
        Vec3 pivot = this.center();
        Vec3 off = ClientStation.turned(lean.pos().m_82546_(pivot), this.swing);
        return new CameraRig.Pose(pivot.m_82549_(off), lean.yaw() + (float)this.swing, lean.pitch(), lean.roll());
    }

    static Vec3 turned(Vec3 v, double degrees) {
        double t = Math.toRadians(degrees);
        double cos = Math.cos(t);
        double sin = Math.sin(t);
        return new Vec3(v.f_82479_ * cos - v.f_82481_ * sin, v.f_82480_, v.f_82479_ * sin + v.f_82481_ * cos);
    }

    public boolean worksAtAnAngle() {
        return false;
    }

    public double leanFov() {
        return 66.0;
    }

    public float bodyPitch() {
        return 50.0f;
    }

    public abstract Vec3 rest(boolean var1);

    public double lateral(Vec3 point) {
        return point.m_82546_(this.center()).m_82526_(this.right());
    }

    public abstract void updateCursor(boolean var1);

    public void clearCursor() {
        this.aim = null;
    }

    public abstract Vec3 handTarget(Vec3 var1, Vec3 var2, boolean var3, double var4);

    public abstract Vec3 heldItemPoint(Vec3 var1, Vec3 var2);

    public float heldItemTilt() {
        return 18.0f;
    }

    @Nullable
    public Vec3[] handsOverride() {
        return null;
    }

    public double maxReach() {
        return Double.MAX_VALUE;
    }

    public double handsStiffness() {
        return 20.0;
    }

    public abstract void click(int var1, boolean var2);

    public void drag(int button) {
    }

    public void release(int button) {
    }

    public void scroll(double delta) {
    }

    public boolean keyPressed(int key) {
        return false;
    }

    public boolean showsInventory() {
        return true;
    }

    public void beforeFrame() {
    }

    public void tick() {
    }

    public boolean busy() {
        return false;
    }

    public void onEnd() {
    }

    public void quickMove(int inventorySlot, int menuSlot) {
        Minecraft mc = Minecraft.m_91087_();
        if (this.busy() || mc.f_91074_ == null || mc.f_91072_ == null || mc.f_91073_ == null) {
            return;
        }
        if (Scene.clientOnly()) {
            QuickMoves.sendVanilla(mc, mc.f_91074_.f_36096_, menuSlot);
            return;
        }
        if (this.kind().usesMenu() || this.kind().isFurnace() || this.kind() == StationKind.CAMPFIRE) {
            ClientNet.sendToServer(new StationActionPayload(this.pos, this.kind(), StationActionPayload.Action.QUICK, inventorySlot, 0, true, 0));
        }
    }

    public double cameraKick() {
        return 0.0;
    }

    public double flash() {
        return 0.0;
    }

    public int flashColor() {
        return 0xFFFFFF;
    }

    public boolean stillThere(Level level) {
        return this.kind().matches(level.m_8055_(this.pos));
    }

    public abstract void renderWorld(PoseStack var1, MultiBufferSource var2, Vec3 var3, int var4);

    public void renderBody(PoseStack pose, MultiBufferSource buffers, Vec3 camera, int light) {
    }

    public void renderScreen(GuiGraphics graphics, Font font, float alpha) {
    }

    @Nullable
    RecipeChoiceStrip choices() {
        return null;
    }

    void chose(int index) {
        RecipeChoiceStrip.send(this.pos, this.kind(), index);
    }

    @Nullable
    public List<Component> tooltip(ItemStack carried) {
        return null;
    }

    public abstract Component hint();

    public BlockPos lightPos() {
        return this.pos.m_7494_();
    }

    public SoundEvent touchSound() {
        return SoundEvents.f_12638_;
    }

    public Vec3 soundPoint() {
        return this.center();
    }
}


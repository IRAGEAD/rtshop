package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.station.StationKind;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** 컴파일용 자리표시 (Cinematic Interactions 의 실제 클래스가 실행 때 쓰인다; jar 에 들어가지 않음). */
public abstract class ClientStation {
    public final BlockPos pos;
    protected final Direction side;
    public Vec3 aim;
    public double swing;

    protected ClientStation(BlockPos pos, Direction side) {
        this.pos = pos;
        this.side = side;
    }

    public abstract StationKind kind();

    public Direction sideDirection() {
        return side;
    }

    public Vec3 side() {
        return Vec3.ZERO;
    }

    public Vec3 forward() {
        return Vec3.ZERO;
    }

    public Vec3 right() {
        return Vec3.ZERO;
    }

    public Vec3 center() {
        return Vec3.ZERO;
    }

    public abstract List<Vec3> approach(LocalPlayer player);

    public boolean crouches() {
        return false;
    }

    public Vec3 workPoint() {
        return Vec3.ZERO;
    }

    public boolean workOnTop() {
        return true;
    }

    public abstract CameraRig.Pose lean();

    public double leanFov() {
        return 66.0;
    }

    public float bodyPitch() {
        return 50.0f;
    }

    public abstract Vec3 rest(boolean right);

    public abstract void updateCursor(boolean overPanel);

    public void clearCursor() {
    }

    public abstract Vec3 handTarget(Vec3 aim, Vec3 shoulder, boolean carrying, double tap);

    public abstract Vec3 heldItemPoint(Vec3 hand, Vec3 shoulder);

    public Vec3[] handsOverride() {
        return null;
    }

    public abstract void click(int button, boolean shift);

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

    public double cameraKick() {
        return 0.0;
    }

    public double flash() {
        return 0.0;
    }

    public int flashColor() {
        return 0xFFFFFF;
    }

    public abstract void renderWorld(PoseStack pose, MultiBufferSource buffers, Vec3 camera, int light);

    public void renderScreen(GuiGraphics graphics, Font font, float alpha) {
    }

    public List<Component> tooltip(ItemStack carried) {
        return null;
    }

    public abstract Component hint();

    public BlockPos lightPos() {
        return pos;
    }

    public SoundEvent touchSound() {
        return null;
    }
}

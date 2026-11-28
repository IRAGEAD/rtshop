/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  javax.annotation.Nullable
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.inventory.ClickType
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.phys.Vec3
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.client.Anim;
import dev.dyslexic.cinematic.client.ClientNet;
import dev.dyslexic.cinematic.client.ClientStation;
import dev.dyslexic.cinematic.client.Hands;
import dev.dyslexic.cinematic.client.ItemPlacer;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.net.StationActionPayload;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public abstract class MenuStation<M extends AbstractContainerMenu>
extends ClientStation {
    private final Class<M> menuType;
    private final int inputs;
    private double moveAt = -10.0;
    private double moveLength = 1.0;
    private double beatAt;
    @Nullable
    private Runnable beat;
    private boolean moveShown;
    protected double denyAt = -10.0;
    private List<ItemStack> leaving = List.of();

    protected MenuStation(BlockPos pos, Direction side, Class<M> menuType, int inputs) {
        super(pos, side);
        this.menuType = menuType;
        this.inputs = inputs;
    }

    @Nullable
    public M menu() {
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        return (M)(player != null && this.menuType.isInstance(player.f_36096_) ? (AbstractContainerMenu)this.menuType.cast(player.f_36096_) : null);
    }

    public ItemStack slot(int index) {
        M menu = this.menu();
        return menu == null ? ItemStack.f_41583_ : menu.m_38853_(index).m_7993_();
    }

    protected void slotClick(int slot, int button, boolean shift) {
        Minecraft mc = Minecraft.m_91087_();
        M menu = this.menu();
        if (menu == null || mc.f_91072_ == null || mc.f_91074_ == null || this.moving()) {
            return;
        }
        Hands.tap();
        mc.f_91072_.m_171799_(((AbstractContainerMenu)menu).f_38840_, slot, button, shift ? ClickType.QUICK_MOVE : ClickType.PICKUP, (Player)mc.f_91074_);
    }

    protected boolean accepts(int slot, ItemStack stack) {
        M menu = this.menu();
        return menu != null && menu.m_38853_(slot).m_5857_(stack);
    }

    protected int slotFor(ItemStack stack, int preferred) {
        if (preferred >= 0 && this.fits(preferred, stack)) {
            return preferred;
        }
        for (int i = 0; i < this.inputs; ++i) {
            if (!this.fits(i, stack)) continue;
            return i;
        }
        return -1;
    }

    private boolean fits(int slot, ItemStack stack) {
        ItemStack there = this.slot(slot);
        return this.accepts(slot, stack) && (there.m_41619_() || ItemStack.m_150942_((ItemStack)there, (ItemStack)stack) && there.m_41613_() < there.m_41741_());
    }

    protected void startMove(double length, double beat, Runnable atBeat) {
        this.moveAt = Scene.clock();
        this.moveLength = length;
        this.beatAt = beat;
        this.beat = atBeat;
        this.moveShown = false;
    }

    protected void showMove(boolean bag) {
        this.moveShown = true;
        if (!Scene.clientOnly()) {
            ClientNet.sendToServer(new StationActionPayload(this.pos, this.kind(), StationActionPayload.Action.CRAFT, 1, bag ? 1 : 0, false, 0));
        }
    }

    public double moveTime() {
        double t = Scene.clock() - this.moveAt;
        return t >= 0.0 && t <= this.moveLength ? t : -1.0;
    }

    public boolean moving() {
        return this.moveTime() >= 0.0;
    }

    void frame() {
        if (this.beat == null || Scene.clock() - this.moveAt < this.beatAt) {
            return;
        }
        Runnable action = this.beat;
        this.beat = null;
        if (Scene.phase == Scene.Phase.ACTIVE && this.menu() != null) {
            action.run();
            if (!this.moveShown && !Scene.clientOnly()) {
                ClientNet.sendToServer(StationActionPayload.of(this.pos, this.kind(), StationActionPayload.Action.CRAFT));
            }
        } else {
            this.moveAt = -10.0;
        }
    }

    protected void cancelMove() {
        this.moveAt = -10.0;
        this.beat = null;
    }

    protected void deny() {
        this.denyAt = Scene.clock();
        Scene.sound(SoundEvents.f_11744_, 0.5f, 0.7f);
    }

    protected double denyShake() {
        double d = Scene.clock() - this.denyAt;
        return d >= 0.0 && d < 0.35 ? 0.012 * Math.sin(d * 60.0) * (1.0 - d / 0.35) : 0.0;
    }

    @Override
    public void click(int button, boolean shift) {
        if (!this.moving() && this.menu() != null) {
            this.act(button, shift);
        }
    }

    @Override
    public boolean busy() {
        return this.moving();
    }

    protected abstract void act(int var1, boolean var2);

    @Override
    public void onEnd() {
        ArrayList<ItemStack> left = new ArrayList<ItemStack>(this.inputs);
        for (int i = 0; i < this.inputs; ++i) {
            left.add(this.slot(i).m_41777_());
        }
        this.leaving = left;
        this.beat = null;
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        if (!Scene.clientOnly() && player != null && this.menuType.isInstance(player.f_36096_)) {
            player.f_36096_ = player.f_36095_;
        }
    }

    public abstract void renderScene(PoseStack var1, MultiBufferSource var2, Camera var3, int var4);

    protected List<ItemStack> leftBehind() {
        return this.leaving;
    }

    protected abstract Vec3 restPoint(int var1);

    protected void drawLeaving(PoseStack pose, MultiBufferSource buffers, Camera camera, int light) {
        Vec3 cam = camera.m_90583_();
        Vec3 to = cam.m_82549_(Scene.yawForward(camera.m_90590_()).m_82490_(0.25)).m_82520_(0.0, -0.5, 0.0);
        for (int i = 0; i < this.leaving.size(); ++i) {
            ItemStack stack = this.leaving.get(i);
            double k = Anim.inOutCubic(Anim.window(Scene.time, 0.06 + (double)i * 0.07, 0.5 + (double)i * 0.07));
            if (stack.m_41619_() || k >= 1.0) continue;
            Vec3 at = Anim.lerp(this.restPoint(i), to, k).m_82520_(0.0, 0.14 * Anim.hump(k), 0.0);
            float size = (float)((ItemPlacer.isBlock(stack) ? 0.16 : 0.24) * (1.0 - k * 0.85));
            ItemPlacer.drawOriented(pose, buffers, cam, stack, at, ItemPlacer.lying(ItemPlacer.awayYaw(this.side), (float)(30.0 * k)), size, light);
        }
    }

    protected boolean outro() {
        return Scene.phase == Scene.Phase.OUTRO;
    }
}


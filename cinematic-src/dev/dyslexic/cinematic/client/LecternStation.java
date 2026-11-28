/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  javax.annotation.Nullable
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.client.renderer.LightTexture
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Vec3i
 *  net.minecraft.network.chat.Component
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.world.inventory.LecternMenu
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.phys.Vec3
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.client.Anim;
import dev.dyslexic.cinematic.client.CameraRig;
import dev.dyslexic.cinematic.client.Fx;
import dev.dyslexic.cinematic.client.MenuStation;
import dev.dyslexic.cinematic.client.OpenBook;
import dev.dyslexic.cinematic.client.Picking;
import dev.dyslexic.cinematic.client.RemainingLayouts;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.SpatialAppearance;
import dev.dyslexic.cinematic.client.SpatialEditor;
import dev.dyslexic.cinematic.client.SpatialTarget;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.LecternMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class LecternStation
extends MenuStation<LecternMenu>
implements SpatialTarget {
    final SpatialEditor gridEditor = new SpatialEditor(this);
    private final Vec3 f;
    private final Vec3 a;
    private final Vec3 s;
    private final Vec3 n;
    private final Vec3 origin;
    private final OpenBook book = new OpenBook();
    public Target target = Target.NONE;
    private int sentPage = -1;
    private double takeAt = -10.0;
    private boolean takeSent;
    private boolean riffleDue;

    @Override
    public SpatialEditor layoutEditor() {
        return this.gridEditor;
    }

    public LecternStation(BlockPos pos, Direction side) {
        super(pos, side, LecternMenu.class, 0);
        this.f = this.side();
        this.a = this.right();
        Vec3 up = new Vec3(0.0, 1.0, 0.0);
        double c = Math.cos(Math.toRadians(22.5));
        double sn = Math.sin(Math.toRadians(22.5));
        this.s = up.m_82490_(sn).m_82546_(this.f.m_82490_(c));
        this.n = up.m_82490_(c).m_82549_(this.f.m_82490_(sn));
        this.origin = Vec3.m_82539_((Vec3i)pos).m_82520_(0.0, 0.9975, 0.0).m_82549_(this.f.m_82490_(0.104375)).m_82549_(this.n.m_82490_(0.012));
        this.book.place(this.origin, this.a, this.s, this.n);
    }

    @Override
    public StationKind kind() {
        return StationKind.LECTERN;
    }

    @Override
    public Vec3 center() {
        return this.origin;
    }

    @Override
    protected Vec3 restPoint(int slot) {
        return this.origin;
    }

    @Override
    public SoundEvent touchSound() {
        return SoundEvents.f_11713_;
    }

    @Override
    public boolean showsInventory() {
        return false;
    }

    public static boolean drawsBookAt(BlockPos pos) {
        LecternStation station = Scene.station(LecternStation.class);
        return station != null && station.pos.equals((Object)pos) && (station.book.spread >= 0 || station.taking());
    }

    private ItemStack book() {
        LecternMenu menu = (LecternMenu)this.menu();
        return menu == null ? ItemStack.f_41583_ : menu.m_39835_();
    }

    private static int pageOf(int spread) {
        return Math.max(0, 2 * spread - 1);
    }

    private void sendPage() {
        Minecraft mc = Minecraft.m_91087_();
        LecternMenu menu = (LecternMenu)this.menu();
        int page = LecternStation.pageOf(this.book.spread);
        if (menu != null && mc.f_91072_ != null && page != this.sentPage && page < Math.max(1, this.book.pageCount())) {
            this.sentPage = page;
            mc.f_91072_.m_105208_(menu.f_38840_, 100 + page);
            menu.m_7511_(0, page);
        }
    }

    @Override
    protected void deny() {
        this.denyAt = Scene.clock();
        Scene.sound(SoundEvents.f_11713_, 0.35f, 0.55f);
    }

    private boolean taking() {
        return this.takeAt > 0.0;
    }

    private void turn(int dir) {
        if (this.gridEditor.visible() || this.taking() || this.book.spread < 0 || Scene.phase != Scene.Phase.ACTIVE) {
            return;
        }
        switch (this.book.turn(dir)) {
            case NO_PAGE: {
                this.deny();
                break;
            }
            case YES: {
                this.sendPage();
                Scene.sound(SoundEvents.f_11713_, 0.9f, 0.9f + Fx.RANDOM.m_188501_() * 0.25f);
                break;
            }
        }
    }

    private void take() {
        if (this.taking() || this.book.turning() || this.book().m_41619_()) {
            return;
        }
        this.takeAt = Scene.clock();
        this.takeSent = false;
        Scene.sound(SoundEvents.f_11713_, 1.0f, 0.6f);
    }

    private double fold() {
        return this.taking() ? Anim.inOutCubic(Anim.window(Scene.clock() - this.takeAt, 0.0, 0.5)) : 0.0;
    }

    private double lift() {
        return this.taking() ? Anim.inOutSine(Anim.window(Scene.clock() - this.takeAt, 0.5, 1.05)) : 0.0;
    }

    @Override
    public void beforeFrame() {
        this.gridEditor.frame();
        this.book.read(this.book());
        LecternMenu menu = (LecternMenu)this.menu();
        if (this.book.spread < 0 && menu != null && !this.book().m_41619_()) {
            this.book.openAt((menu.m_39836_() + 1) / 2);
            this.sentPage = menu.m_39836_();
            this.riffleDue = Scene.phase == Scene.Phase.INTRO;
        }
        this.book.closing = this.taking();
        this.book.fold = this.fold();
        double k = this.lift();
        Vec3 editedOrigin = this.layoutPoint(0);
        Vec3 editedAcross = this.layoutAxis(0, true);
        Vec3 editedSlope = this.layoutAxis(0, false);
        Vec3 to = editedOrigin.m_82549_(this.n.m_82490_(0.1)).m_82549_(editedSlope.m_82490_(-0.75));
        double scale = (1.0 - 0.75 * k) * (double)this.geometry().scale(0);
        this.book.place(Anim.lerp(editedOrigin, to, k), editedAcross.m_82490_(scale), editedSlope.m_82490_(scale), this.n.m_82490_(scale));
        if (this.book.spread < 0) {
            return;
        }
        if (this.riffleDue && Scene.arrived()) {
            this.riffleDue = false;
            if (Scene.phase == Scene.Phase.INTRO) {
                this.book.riffle(Scene.clock() + Scene.introSecondsUntil(0.42));
            }
        }
        OpenBook.Side hover = this.target == Target.LEFT ? OpenBook.Side.LEFT : (this.target == Target.RIGHT ? OpenBook.Side.RIGHT : OpenBook.Side.NONE);
        this.book.settle(hover, Scene.phase == Scene.Phase.ACTIVE, Scene.frameDt);
        this.book.preparePages();
    }

    @Override
    void frame() {
        super.frame();
        if (this.taking()) {
            double t = Scene.clock() - this.takeAt;
            LecternMenu menu = (LecternMenu)this.menu();
            Minecraft mc = Minecraft.m_91087_();
            if (!this.takeSent && t >= 0.5 && menu != null && mc.f_91072_ != null) {
                this.takeSent = true;
                mc.f_91072_.m_105208_(menu.f_38840_, 3);
                Scene.sound(SoundEvents.f_11714_, 0.9f, 1.1f);
            }
            if (t > 2.2 && !this.book().m_41619_() && Scene.phase == Scene.Phase.ACTIVE) {
                this.takeAt = -10.0;
            }
        }
    }

    @Override
    public void tick() {
        LecternMenu menu = (LecternMenu)this.menu();
        if (menu == null || this.book.spread < 0 || this.book.turning() || this.taking()) {
            return;
        }
        int page = menu.m_39836_();
        if (page != this.sentPage && page >= 0) {
            this.sentPage = page;
            this.book.follow((page + 1) / 2);
        }
    }

    public int spread() {
        return this.book.spread;
    }

    public Vec3 pageCenter(boolean left) {
        return this.book.pageCenter(left);
    }

    @Override
    public List<Vec3> approach(LocalPlayer player) {
        Vec3 c = Vec3.m_82539_((Vec3i)this.pos);
        Vec3 stand = new Vec3(c.f_82479_ + this.f.f_82479_ * 0.98, player.m_20186_(), c.f_82481_ + this.f.f_82481_ * 0.98);
        boolean walk = Math.hypot(stand.f_82479_ - player.m_20185_(), stand.f_82481_ - player.m_20189_()) > 0.06;
        return walk ? List.of(stand) : List.of();
    }

    @Override
    public CameraRig.Pose lean() {
        Vec3 eye = this.origin.m_82549_(this.n.m_82490_(0.66)).m_82549_(this.f.m_82490_(0.07)).m_82549_(this.s.m_82490_(-0.02));
        Vec3 look = this.origin.m_82549_(this.s.m_82490_(-0.012)).m_82546_(eye);
        float yaw = (float)Math.toDegrees(Math.atan2(-look.f_82479_, look.f_82481_));
        float pitch = (float)(-Math.toDegrees(Math.atan2(look.f_82480_, Math.hypot(look.f_82479_, look.f_82481_))));
        return new CameraRig.Pose(eye, yaw, pitch, 0.0f);
    }

    @Override
    public double leanFov() {
        return 52.0;
    }

    @Override
    public float bodyPitch() {
        return 42.0f;
    }

    @Override
    public Vec3 rest(boolean right) {
        double x = 0.36700000000000005;
        return this.layoutPoint(0).m_82549_(this.layoutAxis(0, true).m_82490_((right ? x : -x) * (double)this.geometry().scale(0))).m_82549_(this.layoutAxis(0, false).m_82490_(-0.268381679389313 * (double)this.geometry().scale(0))).m_82549_(this.n.m_82490_(0.035));
    }

    @Override
    public void updateCursor(boolean overPanel) {
        this.clearCursor();
        if (overPanel || this.gridEditor.visible() || this.book.spread < 0 || this.taking()) {
            return;
        }
        OpenBook.Side side = this.book.under();
        if (!this.geometry().equals(SpatialAppearance.VANILLA)) {
            Vec3 hit = Picking.cursorOnPlane(this.layoutPoint(0), this.n);
            OpenBook.Side side2 = hit != null && this.layoutPick() == 0 ? (hit.m_82546_(this.layoutPoint(0)).m_82526_(this.layoutAxis(0, true)) < 0.0 ? OpenBook.Side.LEFT : OpenBook.Side.RIGHT) : (side = OpenBook.Side.NONE);
        }
        if (side == OpenBook.Side.NONE) {
            return;
        }
        boolean left = side == OpenBook.Side.LEFT;
        this.target = left ? Target.LEFT : Target.RIGHT;
        this.aim = this.book.corner(left);
    }

    @Override
    public void clearCursor() {
        super.clearCursor();
        this.target = Target.NONE;
    }

    @Override
    public Vec3 handTarget(Vec3 aim, Vec3 shoulder, boolean carrying, double tap) {
        return aim.m_82549_(this.n.m_82490_(0.03 - 0.02 * tap));
    }

    @Override
    public Vec3 heldItemPoint(Vec3 hand, Vec3 shoulder) {
        return hand;
    }

    @Override
    @Nullable
    public Vec3[] handsOverride() {
        if (Scene.phase != Scene.Phase.ACTIVE) {
            return null;
        }
        if (this.taking()) {
            double t = Scene.clock() - this.takeAt;
            Vec3 closed = this.book.at(0.20700000000000002, -0.270381679389313, 0.05);
            double gather = Anim.smoother(Anim.window(t, 0.35, 0.6));
            Vec3 grip = this.layoutAxis(0, true).m_82490_(0.2 * (double)this.geometry().scale(0));
            Vec3 l = Anim.lerp(this.book.foldCorner(), closed.m_82546_(grip), gather);
            Vec3 r = Anim.lerp(this.rest(true), closed.m_82549_(grip), gather);
            return new Vec3[]{r, l};
        }
        return this.book.turningHands();
    }

    @Override
    public double handsStiffness() {
        return 34.0;
    }

    @Override
    protected void act(int button, boolean shift) {
        if (shift) {
            this.take();
            return;
        }
        switch (this.target) {
            case RIGHT: {
                this.turn(1);
                break;
            }
            case LEFT: {
                this.turn(-1);
                break;
            }
        }
    }

    @Override
    public void scroll(double delta) {
        this.turn(delta < 0.0 ? 1 : -1);
    }

    @Override
    public boolean keyPressed(int key) {
        switch (key) {
            case 262: 
            case 267: {
                this.turn(1);
                break;
            }
            case 263: 
            case 266: {
                this.turn(-1);
                break;
            }
            default: {
                return false;
            }
        }
        return true;
    }

    @Override
    public void renderScene(PoseStack pose, MultiBufferSource buffers, Camera camera, int light) {
        if (this.book.spread < 0 && !this.taking()) {
            return;
        }
        if (this.taking() && Scene.clock() - this.takeAt > 1.05) {
            return;
        }
        int lit = LightTexture.m_109885_((int)Math.max(LightTexture.m_109883_((int)light), 11), (int)LightTexture.m_109894_((int)light));
        this.book.render(pose, buffers, camera.m_90583_(), lit);
    }

    @Override
    public void renderWorld(PoseStack pose, MultiBufferSource buffers, Vec3 camera, int light) {
        this.gridEditor.renderWorld(pose, buffers, camera, light);
    }

    @Override
    public Component hint() {
        return Component.m_237115_((String)"cinematic.hint.lectern");
    }

    @Override
    public void onEnd() {
        this.gridEditor.cancel();
        super.onEnd();
    }

    @Override
    public boolean busy() {
        return super.busy() || this.gridEditor.editing() || this.taking() || this.book.turning();
    }

    @Override
    public void renderScreen(GuiGraphics g, Font font, float alpha) {
        this.gridEditor.render(g, font, alpha);
    }

    @Override
    public List<Component> tooltip(ItemStack carried) {
        double[] mouse = Picking.mouseGui();
        return this.gridEditor.tooltip(mouse[0], mouse[1]);
    }

    @Override
    public List<Vec3> workSamples() {
        return RemainingLayouts.samples(this, super.workSamples());
    }

    public static enum Target {
        NONE,
        LEFT,
        RIGHT;

    }
}


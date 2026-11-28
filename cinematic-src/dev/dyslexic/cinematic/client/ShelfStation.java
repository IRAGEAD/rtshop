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
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.client.renderer.LevelRenderer
 *  net.minecraft.client.renderer.LightTexture
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Vec3i
 *  net.minecraft.network.chat.Component
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.tags.ItemTags
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.item.WritableBookItem
 *  net.minecraft.world.item.WrittenBookItem
 *  net.minecraft.world.level.BlockAndTintGetter
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.block.ChiseledBookShelfBlock
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.Vec3
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dyslexic.cinematic.ClientBridge;
import dev.dyslexic.cinematic.client.Anim;
import dev.dyslexic.cinematic.client.CameraRig;
import dev.dyslexic.cinematic.client.ClientMenus;
import dev.dyslexic.cinematic.client.ClientNet;
import dev.dyslexic.cinematic.client.ClientStation;
import dev.dyslexic.cinematic.client.Fx;
import dev.dyslexic.cinematic.client.Hands;
import dev.dyslexic.cinematic.client.OpenBook;
import dev.dyslexic.cinematic.client.Picking;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.ShelfBooks;
import dev.dyslexic.cinematic.client.ShelfLayoutBooks;
import dev.dyslexic.cinematic.client.SpatialAppearance;
import dev.dyslexic.cinematic.client.SpatialEditor;
import dev.dyslexic.cinematic.client.SpatialLayouts;
import dev.dyslexic.cinematic.client.SpatialTarget;
import dev.dyslexic.cinematic.net.StationActionPayload;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.WritableBookItem;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;

public final class ShelfStation
extends ClientStation
implements SpatialTarget {
    final SpatialEditor gridEditor = new SpatialEditor(this);
    private final OpenBook layoutGhost = new OpenBook();
    static final double GRIP = 0.24;
    static final double OUT = 0.5;
    static final double TAKEN = 0.95;
    static final double BAGGED = 0.8;
    static final double PUT_AT = 0.34;
    static final double PUT_HOME = 0.62;
    static final double PUT_END = 0.82;
    static final double OPEN_OUT = 0.52;
    static final double OPEN_HELD = 1.05;
    static final double OPEN_END = 1.45;
    static final double CLOSE_SHUT = 0.45;
    static final double CLOSE_AT = 0.95;
    static final double CLOSE_HOME = 1.25;
    static final double CLOSE_END = 1.42;
    static final double CLOSE_GONE = 0.9;
    static final double PEEK = 0.05;
    static final double READ_DISTANCE = 0.52;
    static final double READ_DROP = 0.04;
    static final double READ_SCALE = 0.86;
    private final Direction facing;
    private final Vec3 front;
    private final int usedSlot;
    public Target target = Target.NONE;
    public int hoverSlot = -1;
    private OpenBook.Side hoverPage = OpenBook.Side.NONE;
    private final double[] peek = new double[ShelfBooks.SPINES.length];
    private double denyAt = -10.0;
    private Move move = Move.NONE;
    private double moveAt;
    private int moveSlot = -1;
    private boolean sent;
    private boolean toBag;
    private boolean started;
    private int from = -1;
    private double peekAt;
    private ItemStack moving = ItemStack.f_41583_;
    private ShelfBooks.Pose fromHand;
    private final OpenBook book = new OpenBook();
    private final OpenBook shut = new OpenBook();
    private boolean reading;
    @Nullable
    private ShelfBooks.Pose readPose;

    @Override
    public SpatialEditor layoutEditor() {
        return this.gridEditor;
    }

    @Override
    public ItemStack layoutItem(int slot) {
        return this.occupied(slot) ? (this.bookIn(slot).m_41619_() ? new ItemStack((ItemLike)Items.f_42517_) : this.bookIn(slot)) : ItemStack.f_41583_;
    }

    @Override
    public SpatialAppearance.Frame layoutFrame() {
        return SpatialAppearance.Frame.of(this.kind(), this.pos, this.side, this.side);
    }

    @Override
    public List<Vec3> workSamples() {
        return this.layoutSamples(super.workSamples());
    }

    @Override
    public void renderScreen(GuiGraphics g, Font font, float alpha) {
        this.gridEditor.render(g, font, alpha);
    }

    @Override
    public void layoutGhost(PoseStack pose, MultiBufferSource buffers, Vec3 camera, int slot, int light) {
        ShelfBooks.draw(pose, buffers, camera, slot, new ItemStack((ItemLike)Items.f_42517_), ShelfBooks.inSlot(this.pos, this.facing, slot, 0.0), this.layoutGhost, light);
    }

    public ShelfStation(BlockPos pos, Direction side) {
        super(pos, side);
        this.facing = side;
        this.front = Vec3.m_82512_((Vec3i)pos).m_82549_(this.side().m_82490_(0.5));
        this.usedSlot = ShelfStation.slotAt(pos, side, ClientBridge.useAt);
    }

    @Override
    public StationKind kind() {
        return StationKind.CHISELED_BOOKSHELF;
    }

    @Override
    public Vec3 center() {
        return this.front;
    }

    @Override
    public BlockPos lightPos() {
        return this.pos.m_121945_(this.facing);
    }

    @Override
    public SoundEvent touchSound() {
        return SoundEvents.f_256874_;
    }

    @Override
    public boolean showsInventory() {
        return !this.gridEditor.visible() && !this.reading && this.move != Move.OPEN && this.move != Move.CLOSE;
    }

    static int slotAt(BlockPos pos, Direction facing, Vec3 point) {
        SpatialAppearance layout = SpatialLayouts.current(StationKind.CHISELED_BOOKSHELF);
        if (!layout.equals(SpatialAppearance.VANILLA)) {
            SpatialAppearance.Frame frame = SpatialAppearance.Frame.of(StationKind.CHISELED_BOOKSHELF, pos, facing, facing);
            Vec3 eye = Minecraft.m_91087_().f_91074_ == null ? point.m_82549_(frame.normal()) : Minecraft.m_91087_().f_91074_.m_146892_();
            Vec3 ray = point.m_82546_(eye);
            double denominator = ray.m_82526_(frame.normal());
            if (Math.abs(denominator) < 1.0E-9) {
                return -1;
            }
            int best = -1;
            double distance = Double.MAX_VALUE;
            for (int i = 0; i < 6; ++i) {
                Vec3 at = layout.point(frame, i);
                double t = at.m_82546_(eye).m_82526_(frame.normal()) / denominator;
                Vec3 hit = eye.m_82549_(ray.m_82490_(t));
                if (!(t >= 0.0) || !layout.contains(frame, i, hit) || !(hit.m_82557_(at) < distance)) continue;
                best = i;
                distance = hit.m_82557_(at);
            }
            return best;
        }
        Vec3 rel = point.m_82546_(Vec3.m_82512_((Vec3i)pos).m_82549_(ShelfBooks.out(facing).m_82490_(0.5)));
        double u = 8.0 + rel.m_82526_(ShelfBooks.right(facing)) * 16.0;
        double v = 8.0 - rel.f_82480_ * 16.0;
        if (Math.abs(rel.m_82526_(ShelfBooks.out(facing))) > 0.05 || u < 0.0 || u > 16.0 || v < 0.0 || v > 16.0) {
            return -1;
        }
        return (v < 8.0 ? 0 : 3) + (u < 6.0 ? 0 : (u < 11.0 ? 1 : 2));
    }

    private ItemStack bookIn(int slot) {
        ClientMenus.Entry entry = ClientMenus.BLOCKS.get(this.pos);
        return entry == null || slot < 0 || slot >= entry.items.size() ? ItemStack.f_41583_ : entry.items.get(slot);
    }

    private boolean occupied(int slot) {
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91073_ == null || slot < 0 || slot == this.moveSlot && (this.move != Move.NONE || this.reading)) {
            return false;
        }
        BlockState state = mc.f_91073_.m_8055_(this.pos);
        return state.m_60734_() instanceof ChiseledBookShelfBlock && (Boolean)state.m_61143_((Property)ChiseledBookShelfBlock.f_260698_.get(slot)) != false;
    }

    static boolean readable(ItemStack stack) {
        return stack.m_41720_() instanceof WritableBookItem || stack.m_41720_() instanceof WrittenBookItem;
    }

    private static boolean shelfBook(ItemStack stack) {
        return stack.m_204117_(ItemTags.f_244646_);
    }

    public Vec3 slotPoint(int slot) {
        return ShelfBooks.spine(this.pos, this.facing, slot);
    }

    @Override
    public List<Vec3> approach(LocalPlayer player) {
        Vec3 stand = new Vec3(this.front.f_82479_ + (double)this.side.m_122429_() * 1.27, player.m_20186_(), this.front.f_82481_ + (double)this.side.m_122431_() * 1.27);
        boolean walk = Math.hypot(stand.f_82479_ - player.m_20185_(), stand.f_82481_ - player.m_20189_()) > 0.06;
        return walk ? List.of(stand) : List.of();
    }

    @Override
    public boolean crouches() {
        return true;
    }

    @Override
    public boolean workOnTop() {
        return false;
    }

    @Override
    public CameraRig.Pose lean() {
        Vec3 eye = this.front.m_82549_(this.side().m_82490_(1.22)).m_82520_(0.0, 0.35, 0.0);
        Vec3 look = this.front.m_82520_(0.0, -0.02, 0.0).m_82546_(eye);
        float pitch = (float)(-Math.toDegrees(Math.atan2(look.f_82480_, Math.hypot(look.f_82479_, look.f_82481_))));
        return new CameraRig.Pose(eye, this.facing.m_122424_().m_122435_(), pitch, 0.0f);
    }

    @Override
    public double leanFov() {
        return 64.0;
    }

    @Override
    public float bodyPitch() {
        return 28.0f;
    }

    @Override
    public Vec3 rest(boolean right) {
        return this.front.m_82549_(this.side().m_82490_(0.3)).m_82549_(this.right().m_82490_(right ? 0.6 : -0.6)).m_82520_(0.0, -0.25, 0.0);
    }

    @Override
    public void updateCursor(boolean overPanel) {
        int slot;
        this.clearCursor();
        if (overPanel || this.move != Move.NONE) {
            return;
        }
        if (this.reading) {
            this.hoverPage = this.book.under();
            if (this.hoverPage != OpenBook.Side.NONE) {
                this.target = Target.PAGE;
                this.aim = this.book.corner(this.hoverPage == OpenBook.Side.LEFT);
            }
            return;
        }
        Vec3 hit = Picking.cursorOnPlane(this.front, this.side());
        int n = this.geometry().equals(SpatialAppearance.VANILLA) ? (hit == null ? -1 : ShelfStation.slotAt(this.pos, this.facing, hit)) : (slot = this.layoutPick());
        if (slot < 0) {
            return;
        }
        this.hoverSlot = slot;
        this.target = this.occupied(slot) ? Target.BOOK : Target.PLACE;
        this.aim = this.slotPoint(slot);
    }

    @Override
    public void clearCursor() {
        super.clearCursor();
        this.target = Target.NONE;
        this.hoverSlot = -1;
        this.hoverPage = OpenBook.Side.NONE;
    }

    @Override
    public Vec3 handTarget(Vec3 aim, Vec3 shoulder, boolean carrying, double tap) {
        if (this.reading) {
            return aim.m_82549_(this.book.n.m_82541_().m_82490_(0.03 - 0.02 * tap));
        }
        Vec3 reach = aim.m_82546_(shoulder).m_82541_();
        return aim.m_82549_(this.side().m_82490_((carrying ? 0.1 : 0.07) - 0.05 * tap)).m_82546_(reach.m_82490_(0.05)).m_82520_(0.0, -0.03, 0.0);
    }

    @Override
    public Vec3 heldItemPoint(Vec3 hand, Vec3 shoulder) {
        Vec3 reach = hand.m_82546_(shoulder).m_82541_();
        return hand.m_82549_(reach.m_82490_(0.07)).m_82520_(0.0, -0.04, 0.0);
    }

    @Override
    public void click(int button, boolean shift) {
        if (this.move != Move.NONE) {
            return;
        }
        if (this.reading) {
            if (button == 1 || shift) {
                this.close(shift);
            } else if (this.target == Target.PAGE) {
                this.turn(this.hoverPage == OpenBook.Side.LEFT ? -1 : 1);
            }
            return;
        }
        if (this.target == Target.BOOK) {
            boolean fits;
            ItemStack there = this.bookIn(this.hoverSlot);
            ItemStack carried = Hands.carried();
            boolean bl = fits = carried.m_41619_() || ItemStack.m_150942_((ItemStack)carried, (ItemStack)there) && carried.m_41613_() < carried.m_41741_();
            if (shift) {
                this.take(this.hoverSlot, true);
            } else if (button == 0 && ShelfStation.readable(there)) {
                this.open(this.hoverSlot);
            } else if (fits) {
                this.take(this.hoverSlot, false);
            } else {
                this.deny();
            }
        } else if (this.target == Target.PLACE) {
            if (ShelfStation.shelfBook(Hands.carried())) {
                this.put(this.hoverSlot, -1, Hands.carried());
            } else {
                this.deny();
            }
        }
    }

    private void deny() {
        this.denyAt = Scene.clock();
        Scene.sound(SoundEvents.f_256874_, 0.6f, 0.6f);
    }

    private void begin(Move next, int slot, ItemStack stack) {
        this.move = next;
        this.moveAt = Scene.clock();
        this.moveSlot = slot;
        this.moving = stack.m_255036_(1);
        this.sent = false;
        this.peekAt = this.peek[slot];
    }

    private void take(int slot, boolean bag) {
        this.toBag = bag;
        this.begin(Move.TAKE, slot, this.bookIn(slot));
    }

    @Override
    public void quickMove(int inventorySlot, int menuSlot) {
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        if (this.busy() || player == null) {
            return;
        }
        ItemStack stack = player.m_150109_().m_8020_(inventorySlot);
        if (!ShelfStation.shelfBook(stack)) {
            this.deny();
            return;
        }
        for (int slot = 0; slot < 6; ++slot) {
            if (!this.bookIn(slot).m_41619_()) continue;
            this.put(slot, inventorySlot, stack);
            return;
        }
        this.deny();
    }

    private void put(int slot, int source, ItemStack stack) {
        this.from = source;
        this.fromHand = source < 0 ? this.handPose(slot, false) : this.lowPose(slot);
        this.begin(Move.PUT, slot, stack);
    }

    private void open(int slot) {
        this.begin(Move.OPEN, slot, this.bookIn(slot));
        this.book.spread = -1;
        this.book.bookId = 0;
        this.book.read(this.moving);
        this.book.openAt(0);
        CameraRig.Pose camera = CameraRig.last() != null ? CameraRig.last() : this.view();
        this.readPose = ShelfBooks.reading(camera, 0.52, 0.04, 0.86);
        Scene.sound(SoundEvents.f_11713_, 0.5f, 1.2f);
    }

    private void close(boolean bag) {
        this.toBag = bag;
        this.reading = false;
        this.move = Move.CLOSE;
        this.moveAt = Scene.clock();
        this.sent = false;
        Scene.sound(SoundEvents.f_11713_, 0.8f, 0.6f);
    }

    private void turn(int dir) {
        switch (this.book.turn(dir)) {
            case NO_PAGE: {
                Scene.sound(SoundEvents.f_11713_, 0.35f, 0.55f);
                break;
            }
            case YES: {
                Scene.sound(SoundEvents.f_11713_, 0.9f, 0.9f + Fx.RANDOM.m_188501_() * 0.25f);
                break;
            }
        }
    }

    @Override
    public void scroll(double delta) {
        if (this.reading && this.move == Move.NONE) {
            this.turn(delta < 0.0 ? 1 : -1);
        }
    }

    @Override
    public boolean keyPressed(int key) {
        if (!this.reading || this.move != Move.NONE) {
            return false;
        }
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
            case 256: {
                this.close(false);
                break;
            }
            default: {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean busy() {
        return this.move != Move.NONE || this.reading || this.gridEditor.editing();
    }

    private void send(StationActionPayload.Action action, int index, int button, boolean shift) {
        ClientNet.sendToServer(new StationActionPayload(this.pos, this.kind(), action, index, button, shift, 0));
    }

    @Override
    public void tick() {
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        if (!this.started && player != null && Scene.time > 0.1) {
            this.started = true;
            ItemStack hand = player.m_21205_();
            if (this.move == Move.NONE && this.usedSlot >= 0 && !this.occupied(this.usedSlot) && ShelfStation.shelfBook(hand) && Hands.carried().m_41619_()) {
                this.put(this.usedSlot, player.m_150109_().f_35977_, hand);
            }
        }
    }

    @Override
    public void beforeFrame() {
        this.gridEditor.frame();
        double t = Scene.clock() - this.moveAt;
        switch (this.move) {
            case TAKE: {
                if (!this.sent) {
                    double d = this.toBag ? 0.8 : 0.24;
                    if (t >= d) {
                        this.sent = true;
                        this.send(StationActionPayload.Action.CLICK, this.moveSlot, 0, this.toBag);
                    }
                }
                if (!(t >= 0.95)) break;
                this.gone();
                break;
            }
            case PUT: {
                if (!this.sent && t >= 0.34) {
                    this.sent = true;
                    this.send(StationActionPayload.Action.PLACE, this.moveSlot, this.from, false);
                }
                if (!(t >= 0.82)) break;
                this.home();
                break;
            }
            case OPEN: {
                if (!this.sent && t >= 0.24) {
                    this.sent = true;
                    this.send(StationActionPayload.Action.CRAFT, this.moveSlot, 1, false);
                }
                if (!(t >= 1.45)) break;
                this.move = Move.NONE;
                this.reading = true;
                break;
            }
            case CLOSE: {
                if (this.toBag) {
                    if (!this.sent && t >= 0.45) {
                        this.sent = true;
                        this.send(StationActionPayload.Action.CLICK, this.moveSlot, 0, true);
                    }
                    if (!(t >= 0.9)) break;
                    this.gone();
                    break;
                }
                if (!this.sent && t >= 1.25) {
                    this.sent = true;
                    this.send(StationActionPayload.Action.CRAFT, this.moveSlot, 0, false);
                }
                if (!(t >= 1.42)) break;
                this.home();
                break;
            }
        }
        double dt = Scene.frameDt;
        for (int i = 0; i < this.peek.length; ++i) {
            boolean up = i == this.hoverSlot && this.target == Target.BOOK && this.move == Move.NONE && Scene.phase == Scene.Phase.ACTIVE;
            this.peek[i] = Anim.approach(this.peek[i], up ? 0.05 : 0.0, 14.0, dt);
        }
        if (this.reading || this.move == Move.OPEN || this.move == Move.CLOSE) {
            this.book.read(this.moving);
            this.book.settle(this.hoverPage, this.reading && Scene.phase == Scene.Phase.ACTIVE, dt);
            this.pose().apply(this.book);
            this.book.preparePages();
        }
        int empty = 0;
        if (this.moveSlot >= 0 && (this.reading || this.move == Move.OPEN || this.move == Move.CLOSE || this.move == Move.PUT && t < 0.62 || this.move == Move.TAKE && t >= 0.192)) {
            empty = 1 << this.moveSlot;
        }
        ShelfBooks.keepEmpty(this.pos, empty);
    }

    private void gone() {
        ShelfBooks.Pose there = this.handPose(this.moveSlot, this.toBag);
        ShelfBooks.fly(new ShelfBooks.Flight(this.pos, this.facing, this.moveSlot, this.moving, ShelfStation.ownId(), List.of(new ShelfBooks.Key(0.0, () -> there)), false));
        this.move = Move.NONE;
        this.moveSlot = -1;
    }

    private void home() {
        ShelfBooks.Pose there = ShelfBooks.inSlot(this.pos, this.facing, this.moveSlot, 0.0);
        ShelfBooks.fly(new ShelfBooks.Flight(this.pos, this.facing, this.moveSlot, this.moving, ShelfStation.ownId(), List.of(new ShelfBooks.Key(0.0, () -> there)), true));
        this.move = Move.NONE;
        this.moveSlot = -1;
    }

    private static int ownId() {
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        return player == null ? -1 : player.m_19879_();
    }

    @Override
    public void onEnd() {
        boolean gone;
        this.gridEditor.cancel();
        ShelfBooks.keepEmpty(this.pos, 0);
        if (this.moveSlot < 0 || this.move == Move.NONE && !this.reading) {
            return;
        }
        ShelfBooks.Pose now = this.pose();
        int slot = this.moveSlot;
        boolean bl = gone = this.move == Move.TAKE && this.sent || this.move == Move.CLOSE && this.toBag && this.sent || this.move == Move.PUT && !this.sent;
        if (gone) {
            ShelfBooks.Pose away = this.handPose(slot, true);
            ShelfBooks.fly(new ShelfBooks.Flight(this.pos, this.facing, slot, this.moving, ShelfStation.ownId(), List.of(new ShelfBooks.Key(0.0, () -> now), new ShelfBooks.Key(0.35, () -> away)), false));
        } else {
            ShelfBooks.Pose clear = ShelfBooks.inSlot(this.pos, this.facing, slot, 0.37);
            ShelfBooks.Pose home = ShelfBooks.inSlot(this.pos, this.facing, slot, 0.0);
            ShelfBooks.fly(new ShelfBooks.Flight(this.pos, this.facing, slot, this.moving, ShelfStation.ownId(), List.of(new ShelfBooks.Key(0.0, () -> now), new ShelfBooks.Key(0.3, () -> now.shut(1.0)), new ShelfBooks.Key(0.65, () -> clear), new ShelfBooks.Key(0.9, () -> home)), true));
        }
        this.move = Move.NONE;
        this.reading = false;
        this.moveSlot = -1;
    }

    private ShelfBooks.Pose handPose(int slot, boolean bag) {
        CameraRig.Pose camera = CameraRig.last() != null ? CameraRig.last() : this.view();
        Vec3 at = bag ? camera.pos().m_82520_(0.0, 0.55, 0.0).m_82549_(Scene.yawForward(camera.yaw()).m_82490_(0.6)) : this.slotPoint(slot).m_82549_(this.side().m_82490_(0.24)).m_82520_(0.0, -0.06, 0.0);
        return this.lying(at, slot, bag ? 0.25 : 0.55);
    }

    private ShelfBooks.Pose lowPose(int slot) {
        CameraRig.Pose camera = CameraRig.last() != null ? CameraRig.last() : this.view();
        return this.lying(camera.pos().m_82549_(Scene.yawForward(camera.yaw()).m_82490_(0.45)).m_82520_(0.0, -0.38, 0.0), slot, 0.55);
    }

    private ShelfBooks.Pose lying(Vec3 at, int slot, double size) {
        return new ShelfBooks.Pose(at, ShelfBooks.basis(this.right(), this.forward(), new Vec3(0.0, 1.0, 0.0)), ShelfBooks.inSlot(this.pos, this.facing, slot, 0.0).size().m_82490_(size), 1.0);
    }

    private ShelfBooks.Pose pose() {
        double t = Scene.clock() - this.moveAt;
        ShelfBooks.Pose home = ShelfBooks.inSlot(this.pos, this.facing, this.moveSlot, 0.0);
        ShelfBooks.Pose clear = ShelfBooks.inSlot(this.pos, this.facing, this.moveSlot, 0.37);
        if (this.reading && this.move == Move.NONE) {
            return this.readPose;
        }
        switch (this.move) {
            case TAKE: {
                if (t < 0.5) {
                    double out = this.peekAt + (0.37 - this.peekAt) * Anim.inOutCubic(Anim.window(t, 0.24, 0.5));
                    return ShelfBooks.inSlot(this.pos, this.facing, this.moveSlot, out);
                }
                double k = Anim.inOutCubic(Anim.window(t, 0.5, 0.95));
                return clear.to(this.handPose(this.moveSlot, this.toBag), k).moved(new Vec3(0.0, 0.07 * Anim.hump(k), 0.0));
            }
            case PUT: {
                if (t < 0.34) {
                    double k = Anim.inOutCubic(Anim.window(t, 0.0, 0.34));
                    return this.fromHand.to(clear, k).moved(new Vec3(0.0, 0.06 * Anim.hump(k), 0.0));
                }
                double out = 0.37 * (1.0 - Anim.inOutCubic(Anim.window(t, 0.34, 0.62)));
                return ShelfBooks.inSlot(this.pos, this.facing, this.moveSlot, out);
            }
            case OPEN: {
                if (t < 0.52) {
                    double out = this.peekAt + (0.37 - this.peekAt) * Anim.inOutCubic(Anim.window(t, 0.24, 0.52));
                    return ShelfBooks.inSlot(this.pos, this.facing, this.moveSlot, out);
                }
                if (t < 1.05) {
                    double k = Anim.inOutCubic(Anim.window(t, 0.52, 1.05));
                    return clear.to(this.readPose.shut(1.0), k).moved(new Vec3(0.0, 0.05 * Anim.hump(k), 0.0));
                }
                return this.readPose.shut(1.0 - Anim.smoother(Anim.window(t, 1.07, 1.45)));
            }
            case CLOSE: {
                if (t < 0.45) {
                    return this.readPose.shut(Anim.smoother(t / 0.45));
                }
                if (this.toBag) {
                    double k = Anim.inOutCubic(Anim.window(t, 0.45, 0.9));
                    return this.readPose.shut(1.0).to(this.handPose(this.moveSlot, true), k);
                }
                if (t < 0.95) {
                    double k = Anim.inOutCubic(Anim.window(t, 0.45, 0.95));
                    return this.readPose.shut(1.0).to(clear, k).moved(new Vec3(0.0, 0.05 * Anim.hump(k), 0.0));
                }
                double out = 0.37 * (1.0 - Anim.inOutCubic(Anim.window(t, 0.95, 1.25)));
                return ShelfBooks.inSlot(this.pos, this.facing, this.moveSlot, out);
            }
        }
        return home;
    }

    @Override
    @Nullable
    public Vec3[] handsOverride() {
        Vec3[] vec3Array;
        Vec3 working;
        if (Scene.phase != Scene.Phase.ACTIVE || this.move == Move.NONE && !this.reading) {
            return null;
        }
        Vec3 restRight = this.rest(true);
        Vec3 restLeft = this.rest(false);
        if (this.reading && this.move == Move.NONE) {
            Vec3[] vec3Array2;
            this.readPose.apply(this.book);
            Vec3[] turning = this.book.turningHands();
            if (turning != null) {
                vec3Array2 = turning;
            } else {
                Vec3[] vec3Array3 = new Vec3[2];
                vec3Array3[0] = this.book.rest(true);
                vec3Array2 = vec3Array3;
                vec3Array3[1] = this.book.rest(false);
            }
            return vec3Array2;
        }
        double t = Scene.clock() - this.moveAt;
        boolean leftWorks = this.moveSlot % 3 == 0;
        Vec3 other = leftWorks ? restRight : restLeft;
        ShelfBooks.Pose where = this.pose();
        Vec3 spine = where.at(0.0, 0.0, 0.016);
        CameraRig.Pose camera = CameraRig.last() != null ? CameraRig.last() : this.view();
        Vec3 grip = spine.m_82549_(camera.pos().m_82546_(spine).m_82541_().m_82490_(0.06)).m_82520_(0.0, -0.02, 0.0);
        Vec3 home = leftWorks ? restLeft : restRight;
        switch (this.move) {
            case TAKE: {
                double reach = Anim.smoother(Anim.window(t, 0.0, 0.24));
                double back = Anim.smoother(Anim.window(t, 0.7, 0.95));
                working = Anim.lerp(Anim.lerp(home, grip, reach), home, this.toBag ? back : back * 0.6);
                break;
            }
            case PUT: {
                double back = Anim.smoother(Anim.window(t, 0.62, 0.82));
                working = Anim.lerp(grip, home, back);
                break;
            }
            case OPEN: {
                double reach = Anim.smoother(Anim.window(t, 0.0, 0.24));
                working = Anim.lerp(home, grip, reach);
                if (!(t >= 0.8500000000000001)) break;
                this.readPose.apply(this.book);
                double k = Anim.smoother(Anim.window(t, 0.8500000000000001, 1.1500000000000001));
                Vec3 right = Anim.lerp(leftWorks ? restRight : working, this.book.rest(true), k);
                Vec3 left = Anim.lerp(leftWorks ? working : restLeft, this.book.rest(false), k);
                where.apply(this.book);
                if (t >= 1.05) {
                    double cover = Anim.hump(Anim.window(t, 1.05, 1.45));
                    left = Anim.lerp(left, this.book.foldCorner(), cover * 0.8);
                }
                return new Vec3[]{right, left};
            }
            case CLOSE: {
                Vec3[] vec3Array4;
                this.readPose.apply(this.book);
                Vec3 right = this.book.rest(true);
                Vec3 left = this.book.rest(false);
                where.apply(this.book);
                if (t < 0.45) {
                    left = Anim.lerp(left, this.book.foldCorner(), Anim.hump(t / 0.45) * 0.8);
                    return new Vec3[]{right, left};
                }
                double k = Anim.smoother(Anim.window(t, 0.45, 0.65));
                double back = Anim.smoother(Anim.window(t, this.toBag ? 0.75 : 1.25, this.toBag ? 0.9 : 1.42));
                Vec3 carry = Anim.lerp(grip, home, back);
                Vec3 free = leftWorks ? Anim.lerp(right, restRight, k) : Anim.lerp(left, restLeft, k);
                Vec3 holding = Anim.lerp(leftWorks ? left : right, carry, k);
                if (leftWorks) {
                    Vec3[] vec3Array5 = new Vec3[2];
                    vec3Array5[0] = free;
                    vec3Array4 = vec3Array5;
                    vec3Array5[1] = holding;
                } else {
                    Vec3[] vec3Array6 = new Vec3[2];
                    vec3Array6[0] = holding;
                    vec3Array4 = vec3Array6;
                    vec3Array6[1] = free;
                }
                return vec3Array4;
            }
            default: {
                working = home;
            }
        }
        if (leftWorks) {
            Vec3[] vec3Array7 = new Vec3[2];
            vec3Array7[0] = other;
            vec3Array = vec3Array7;
            vec3Array7[1] = working;
        } else {
            Vec3[] vec3Array8 = new Vec3[2];
            vec3Array8[0] = working;
            vec3Array = vec3Array8;
            vec3Array8[1] = other;
        }
        return vec3Array;
    }

    @Override
    public double handsStiffness() {
        return 30.0;
    }

    void renderBooks(PoseStack pose, MultiBufferSource buffers, Camera camera, ClientLevel level) {
        Vec3 cam = camera.m_90583_();
        int light = LevelRenderer.m_109541_((BlockAndTintGetter)level, (BlockPos)this.pos.m_121945_(this.facing));
        double shake = Scene.clock() - this.denyAt < 0.3 ? 0.006 * Math.sin((Scene.clock() - this.denyAt) * 60.0) : 0.0;
        for (int i = 0; i < this.peek.length; ++i) {
            boolean shaking;
            boolean bl = shaking = shake != 0.0 && i == this.hoverSlot;
            if (!(this.peek[i] > 0.001) && !shaking && !ShelfLayoutBooks.drawsRestingBooks() || !this.occupied(i) || ShelfBooks.flight(this.pos, i) != null) continue;
            ShelfBooks.Pose there = ShelfBooks.inSlot(this.pos, this.facing, i, this.peek[i]).moved(this.right().m_82490_(shaking ? shake : 0.0));
            ShelfBooks.draw(pose, buffers, cam, i, this.bookIn(i), there, this.shut, light);
        }
        if (this.moveSlot >= 0 && (this.move != Move.NONE || this.reading)) {
            int lit;
            boolean open = this.reading || this.move == Move.OPEN && Scene.clock() - this.moveAt > 1.05 || this.move == Move.CLOSE;
            int n = lit = open ? LightTexture.m_109885_((int)Math.max(LightTexture.m_109883_((int)light), 11), (int)LightTexture.m_109894_((int)light)) : light;
            if (this.move == Move.PUT || this.move == Move.TAKE) {
                ShelfBooks.draw(pose, buffers, cam, this.moveSlot, this.moving, this.pose(), this.shut, lit);
            } else {
                this.book.look = ShelfBooks.look(this.moveSlot);
                this.pose().apply(this.book);
                this.book.render(pose, buffers, cam, lit);
            }
        }
    }

    @Override
    public void renderWorld(PoseStack pose, MultiBufferSource buffers, Vec3 camera, int light) {
        int[] nArray;
        this.gridEditor.renderWorld(pose, buffers, camera, light);
        double clock = Scene.clock();
        if (!this.reading) {
            for (int i = 0; i < ShelfBooks.SPINES.length; ++i) {
                if (!this.occupied(i) || !this.bookIn(i).m_150930_(Items.f_42690_)) continue;
                float g = (float)(0.13 + 0.06 * Math.sin(clock * 2.1 + (double)i * 1.7));
                Fx.facePatch(pose, buffers, camera, this.slotPoint(i).m_82549_(this.side().m_82490_(0.004 + this.peek[i])), this.right(), new Vec3(0.0, 1.0, 0.0), 0.17, 0.75f * g, 0.38f * g, g);
                if (!(Fx.RANDOM.m_188500_() < Scene.frameDt * 1.5)) continue;
                Fx.flakes(this.slotPoint(i).m_82549_(this.side().m_82490_(0.04)), 0.07, 1);
            }
        }
        if (this.hoverSlot < 0 || this.move != Move.NONE || this.reading) {
            return;
        }
        boolean ok = this.target == Target.BOOK || ShelfStation.shelfBook(Hands.carried());
        int[] r = ShelfBooks.SPINES[this.hoverSlot];
        double out = this.target == Target.BOOK ? this.peek[this.hoverSlot] + 0.004 + 0.002 : 0.003;
        Vec3 a = ShelfBooks.face(this.pos, this.facing, (double)r[0] - 0.5, (double)r[1] - 0.5, out);
        Vec3 b = ShelfBooks.face(this.pos, this.facing, (double)r[2] + 0.5, (double)r[1] - 0.5, out);
        Vec3 c = ShelfBooks.face(this.pos, this.facing, (double)r[2] + 0.5, (double)r[3] + 0.5, out);
        Vec3 d = ShelfBooks.face(this.pos, this.facing, (double)r[0] - 0.5, (double)r[3] + 0.5, out);
        if (!this.geometry().equals(SpatialAppearance.VANILLA)) {
            double[] half = this.geometry().half(this.layoutFrame(), this.hoverSlot);
            Vec3 at = this.slotPoint(this.hoverSlot).m_82549_(this.side().m_82490_(out));
            Vec3 u = this.geometry().axis(this.layoutFrame(), this.hoverSlot, true).m_82490_(half[0]);
            Vec3 v = this.geometry().axis(this.layoutFrame(), this.hoverSlot, false).m_82490_(half[1]);
            a = at.m_82546_(u).m_82549_(v);
            b = at.m_82549_(u).m_82549_(v);
            c = at.m_82549_(u).m_82546_(v);
            d = at.m_82546_(u).m_82546_(v);
        }
        if (ok) {
            int[] nArray2 = new int[3];
            nArray2[0] = 255;
            nArray2[1] = 236;
            nArray = nArray2;
            nArray2[2] = 200;
        } else {
            int[] nArray3 = new int[3];
            nArray3[0] = 255;
            nArray3[1] = 70;
            nArray = nArray3;
            nArray3[2] = 60;
        }
        int[] tint = nArray;
        VertexConsumer vc = buffers.m_6299_(Fx.HIGHLIGHT);
        int alpha = this.target == Target.BOOK ? 30 : 44;
        for (Vec3 p : new Vec3[]{a, b, c, d}) {
            vc.m_252986_(pose.m_85850_().m_252922_(), (float)(p.f_82479_ - camera.f_82479_), (float)(p.f_82480_ - camera.f_82480_), (float)(p.f_82481_ - camera.f_82481_)).m_6122_(tint[0], tint[1], tint[2], alpha).m_5752_();
        }
    }

    @Override
    @Nullable
    public List<Component> tooltip(ItemStack carried) {
        double[] editorMouse = Picking.mouseGui();
        List<Component> editorTip = this.gridEditor.tooltip(editorMouse[0], editorMouse[1]);
        if (editorTip != null || this.gridEditor.editing()) {
            return editorTip;
        }
        Minecraft mc = Minecraft.m_91087_();
        if (this.target == Target.BOOK) {
            ItemStack there = this.bookIn(this.hoverSlot);
            ArrayList<Component> lines = new ArrayList<Component>(there.m_41619_() ? List.of(Component.m_237115_((String)"cinematic.shelf.book")) : Screen.m_280152_((Minecraft)mc, (ItemStack)there));
            lines.add((Component)Component.m_237119_());
            if (ShelfStation.readable(there)) {
                lines.add((Component)Component.m_237115_((String)"cinematic.shelf.read").m_130940_(ChatFormatting.GOLD));
                lines.add((Component)Component.m_237115_((String)"cinematic.shelf.take_right").m_130940_(ChatFormatting.GRAY));
            } else {
                lines.add((Component)Component.m_237115_((String)"cinematic.shelf.take").m_130940_(ChatFormatting.GOLD));
            }
            lines.add((Component)Component.m_237115_((String)"cinematic.shelf.take_bag").m_130940_(ChatFormatting.GRAY));
            return lines;
        }
        if (this.target == Target.PLACE) {
            if (ShelfStation.shelfBook(carried)) {
                return List.of(carried.m_41786_(), Component.m_237115_((String)"cinematic.shelf.put").m_130940_(ChatFormatting.GOLD));
            }
            return List.of(Component.m_237115_((String)(carried.m_41619_() ? "cinematic.shelf.empty" : "cinematic.shelf.not_book")).m_130940_(carried.m_41619_() ? ChatFormatting.GRAY : ChatFormatting.RED));
        }
        return null;
    }

    @Override
    public Component hint() {
        if (this.reading || this.move == Move.OPEN || this.move == Move.CLOSE) {
            return Component.m_237115_((String)"cinematic.shelf.hint.read");
        }
        return Component.m_237115_((String)(ShelfStation.shelfBook(Hands.carried()) ? "cinematic.shelf.hint.put" : "cinematic.shelf.hint"));
    }

    public boolean reading() {
        return this.reading;
    }

    public int spread() {
        return this.book.spread;
    }

    public Vec3 pageCenter(boolean left) {
        return this.book.pageCenter(left);
    }

    public String moveName() {
        return this.move.name().toLowerCase(Locale.ROOT) + String.format(Locale.ROOT, " %.2f", this.move == Move.NONE ? 0.0 : Scene.clock() - this.moveAt);
    }

    public static enum Target {
        NONE,
        BOOK,
        PLACE,
        PAGE;

    }

    private static enum Move {
        NONE,
        TAKE,
        PUT,
        OPEN,
        CLOSE;

    }
}


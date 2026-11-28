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
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Vec3i
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.item.RecordItem
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.block.JukeboxBlock
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dyslexic.cinematic.client.Anim;
import dev.dyslexic.cinematic.client.CameraRig;
import dev.dyslexic.cinematic.client.ClientMenus;
import dev.dyslexic.cinematic.client.ClientNet;
import dev.dyslexic.cinematic.client.ClientStation;
import dev.dyslexic.cinematic.client.Fx;
import dev.dyslexic.cinematic.client.Hands;
import dev.dyslexic.cinematic.client.ItemPlacer;
import dev.dyslexic.cinematic.client.Picking;
import dev.dyslexic.cinematic.client.RemainingLayouts;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.Settings;
import dev.dyslexic.cinematic.client.SpatialAppearance;
import dev.dyslexic.cinematic.client.SpatialEditor;
import dev.dyslexic.cinematic.client.SpatialTarget;
import dev.dyslexic.cinematic.net.StationActionPayload;
import dev.dyslexic.cinematic.station.JukeboxActions;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

public final class JukeboxStation
extends ClientStation
implements SpatialTarget {
    final SpatialEditor gridEditor = new SpatialEditor(this);
    private static final float RECORD_SIZE = 0.6f;
    private static final double DISC_R = 0.22500000894069672;
    private static final double INSERT = 1.3;
    private static final double EJECT = 1.15;
    private static final double LINGER = 0.55;
    private static final double INSERT_SEND = 0.62;
    private static final double EJECT_SEND = 0.16;
    private final Vec3 top;
    private Move move = Move.NONE;
    private double moveAt;
    private boolean sent;
    private boolean started;
    private ItemStack record = ItemStack.f_41583_;
    private int from = -1;
    public boolean hoverTop;

    @Override
    public SpatialEditor layoutEditor() {
        return this.gridEditor;
    }

    public JukeboxStation(BlockPos pos, Direction side) {
        super(pos, side);
        this.top = Vec3.m_82512_((Vec3i)pos).m_82520_(0.0, 0.5, 0.0);
    }

    static boolean worthIt(LocalPlayer player, BlockPos pos) {
        if (((Boolean)player.m_9236_().m_8055_(pos).m_61143_((Property)JukeboxBlock.f_54254_)).booleanValue() || JukeboxStation.isRecord(player.f_36096_.m_142621_())) {
            return true;
        }
        for (int i = 0; i < player.m_150109_().m_6643_(); ++i) {
            if (!JukeboxStation.isRecord(player.m_150109_().m_8020_(i))) continue;
            return true;
        }
        return false;
    }

    static boolean isRecord(ItemStack stack) {
        return JukeboxActions.isRecord(stack);
    }

    @Override
    public StationKind kind() {
        return StationKind.JUKEBOX;
    }

    @Override
    public Vec3 center() {
        return this.top;
    }

    @Override
    public boolean worksAtAnAngle() {
        return true;
    }

    @Override
    public SoundEvent touchSound() {
        return SoundEvents.f_12638_;
    }

    private boolean playing() {
        Minecraft mc = Minecraft.m_91087_();
        return mc.f_91073_ != null && (Boolean)mc.f_91073_.m_8055_(this.pos).m_61143_((Property)JukeboxBlock.f_54254_) != false;
    }

    private ItemStack inside() {
        ClientMenus.Entry entry = ClientMenus.BLOCKS.get(this.pos);
        ItemStack stack = entry == null || entry.items.isEmpty() ? ItemStack.f_41583_ : entry.items.get(0);
        return stack.m_41619_() ? new ItemStack((ItemLike)Items.f_42752_) : stack;
    }

    @Override
    public List<Vec3> approach(LocalPlayer player) {
        Vec3 stand = new Vec3(this.top.f_82479_ + (double)this.side.m_122429_() * 0.88, player.m_20186_(), this.top.f_82481_ + (double)this.side.m_122431_() * 0.88);
        boolean walk = Math.hypot(stand.f_82479_ - player.m_20185_(), stand.f_82481_ - player.m_20189_()) > 0.06;
        return walk ? List.of(stand) : List.of();
    }

    @Override
    public CameraRig.Pose lean() {
        Vec3 eye = this.top.m_82549_(this.side().m_82490_(0.8)).m_82520_(0.0, 0.58, 0.0);
        Vec3 look = this.top.m_82549_(this.side().m_82490_(0.04)).m_82520_(0.0, 0.2, 0.0).m_82546_(eye);
        float pitch = (float)(-Math.toDegrees(Math.atan2(look.f_82480_, Math.hypot(look.f_82479_, look.f_82481_))));
        return new CameraRig.Pose(eye, this.side.m_122424_().m_122435_(), pitch, 0.0f);
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
        return this.top.m_82549_(this.side().m_82490_(0.34)).m_82549_(this.right().m_82490_(right ? 0.25 : -0.25)).m_82520_(0.0, 0.035, 0.0);
    }

    @Override
    public void updateCursor(boolean overPanel) {
        this.aim = null;
        this.hoverTop = false;
        if (overPanel || this.gridEditor.visible() || this.move != Move.NONE) {
            return;
        }
        if (!this.geometry().equals(SpatialAppearance.VANILLA)) {
            boolean bl = this.hoverTop = this.layoutPick() == 0;
            if (this.hoverTop) {
                this.aim = this.layoutPoint(0);
            }
            return;
        }
        Vec3 hit = Picking.cursorOnPlane(this.top.f_82480_);
        if (hit != null && Math.abs(hit.f_82479_ - this.top.f_82479_) < 0.52 && Math.abs(hit.f_82481_ - this.top.f_82481_) < 0.52) {
            this.hoverTop = true;
            this.aim = new Vec3(Math.max((double)this.pos.m_123341_() + 0.1, Math.min((double)this.pos.m_123341_() + 0.9, hit.f_82479_)), this.top.f_82480_, Math.max((double)this.pos.m_123343_() + 0.1, Math.min((double)this.pos.m_123343_() + 0.9, hit.f_82481_)));
        }
    }

    @Override
    public Vec3 handTarget(Vec3 aim, Vec3 shoulder, boolean carrying, double tap) {
        Vec3 reach = new Vec3(aim.f_82479_ - shoulder.f_82479_, 0.0, aim.f_82481_ - shoulder.f_82481_).m_82541_();
        return aim.m_82520_(0.0, (carrying ? 0.09 : 0.05) - 0.05 * tap, 0.0).m_82546_(reach.m_82490_(0.07));
    }

    @Override
    public Vec3 heldItemPoint(Vec3 hand, Vec3 shoulder) {
        Vec3 reach = new Vec3(hand.f_82479_ - shoulder.f_82479_, 0.0, hand.f_82481_ - shoulder.f_82481_).m_82541_();
        return hand.m_82549_(reach.m_82490_(0.08)).m_82520_(0.0, -0.03, 0.0);
    }

    @Override
    public void click(int button, boolean shift) {
        if (this.move != Move.NONE || !this.hoverTop || button != 0) {
            return;
        }
        if (this.playing()) {
            this.begin(Move.EJECT, this.inside(), -1);
        } else if (JukeboxStation.isRecord(Hands.carried())) {
            this.begin(Move.INSERT, Hands.carried().m_255036_(1), -1);
        } else {
            Hands.tap();
        }
    }

    @Override
    public void quickMove(int inventorySlot, int menuSlot) {
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        if (this.busy() || player == null) {
            return;
        }
        ItemStack stack = player.m_150109_().m_8020_(inventorySlot);
        if (this.playing() || !JukeboxStation.isRecord(stack)) {
            Hands.tap();
            return;
        }
        this.begin(Move.INSERT, stack.m_255036_(1), inventorySlot);
    }

    private void begin(Move next, ItemStack stack, int source) {
        this.move = next;
        this.record = stack;
        this.from = source;
        this.moveAt = Scene.clock();
        this.sent = false;
    }

    private double progress() {
        return Anim.clamp01((Scene.clock() - this.moveAt) / (this.move == Move.EJECT ? 1.15 : 1.3));
    }

    @Override
    public void tick() {
        double length;
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        if (player == null || this.gridEditor.visible() || Scene.phase != Scene.Phase.ACTIVE) {
            return;
        }
        if (!this.started && Scene.time > 0.1) {
            this.started = true;
            ItemStack hand = player.m_21205_();
            if (Settings.developerMode()) {
                return;
            }
            if (this.playing()) {
                this.begin(Move.EJECT, this.inside(), -1);
            } else if (JukeboxStation.isRecord(hand)) {
                this.begin(Move.INSERT, hand.m_255036_(1), player.m_150109_().f_35977_);
            } else if (JukeboxStation.isRecord(Hands.carried())) {
                this.begin(Move.INSERT, Hands.carried().m_255036_(1), -1);
            }
        }
        if (this.move == Move.NONE) {
            return;
        }
        double t = this.progress();
        if (!this.sent) {
            double d = this.move == Move.EJECT ? 0.16 : 0.62;
            if (t >= d) {
                this.sent = true;
                ClientNet.sendToServer(new StationActionPayload(this.pos, StationKind.JUKEBOX, StationActionPayload.Action.CLICK, this.from < 0 ? 0 : 1, Math.max(this.from, 0), false, 0));
                if (this.move == Move.INSERT) {
                    Fx.motes(this.layoutPoint(0).m_82520_(0.0, 0.06, 0.0), 0.12, 10, 1.0f, 0.82f, 0.45f, 0.25);
                }
            }
        }
        double d = length = this.move == Move.EJECT ? 1.15 : 1.3;
        if (Scene.clock() - this.moveAt > length + 0.55) {
            this.move = Move.NONE;
            if (!Settings.developerMode()) {
                Scene.leave();
            }
        }
    }

    @Override
    public boolean busy() {
        return this.move != Move.NONE || this.gridEditor.editing();
    }

    @Nullable
    private Vec3 recordPoint() {
        if (this.move == Move.NONE) {
            return null;
        }
        double t = this.progress();
        Vec3 low = this.rest(true).m_82549_(this.side().m_82490_(0.18)).m_82520_(0.0, -0.12, 0.0);
        Vec3 contact = this.layoutPoint(0);
        double radius = 0.22500000894069672 * (double)this.geometry().scale(0);
        Vec3 above = contact.m_82520_(0.0, radius + 0.08, 0.0);
        Vec3 halfIn = contact.m_82520_(0.0, radius * 0.25, 0.0);
        Vec3 gone = contact.m_82520_(0.0, -radius - 0.02, 0.0);
        if (this.move == Move.INSERT) {
            if (t < 0.32) {
                double k = Anim.inOutCubic(t / 0.32);
                return Anim.lerp(low, above, k).m_82520_(0.0, 0.06 * Anim.hump(k), 0.0);
            }
            if (t < 0.42) {
                return above;
            }
            if (t < 0.72) {
                return Anim.lerp(above, halfIn, Anim.inOutSine((t - 0.42) / 0.3));
            }
            if (t < 0.86) {
                return Anim.lerp(halfIn, gone, Anim.inCubic((t - 0.72) / 0.14));
            }
            return null;
        }
        if (t < 0.2) {
            return null;
        }
        if (t < 0.55) {
            return Anim.lerp(gone, above, Anim.outCubic((t - 0.2) / 0.35));
        }
        if (t < 0.7) {
            return above;
        }
        return Anim.lerp(above, low.m_82549_(this.side().m_82490_(0.12)).m_82520_(0.0, -0.12, 0.0), Anim.inCubic((t - 0.7) / 0.3));
    }

    private Quaternionf recordTurn() {
        double t = this.progress();
        double lean = this.move == Move.INSERT ? 1.0 - Anim.smoother(Anim.window(t, 0.2, 0.42)) : Anim.smoother(Anim.window(t, 0.7, 1.0));
        return Axis.f_252436_.m_252977_(-this.side.m_122435_() + RemainingLayouts.rotation(this.kind(), 0)).mul((Quaternionfc)Axis.f_252529_.m_252977_((float)(-35.0 * lean)));
    }

    @Override
    @Nullable
    public Vec3[] handsOverride() {
        Vec3 hand;
        if (this.move == Move.NONE) {
            return null;
        }
        double t = this.progress();
        Vec3 restRight = this.rest(true);
        Vec3 restLeft = this.rest(false);
        Vec3 disc = this.recordPoint();
        Vec3 press = this.layoutPoint(0).m_82549_(this.side().m_82490_(0.05)).m_82520_(0.0, 0.045, 0.0);
        if (this.move == Move.INSERT) {
            if (disc != null && t < 0.72) {
                hand = this.edge(disc);
            } else if (t < 0.86) {
                double k = Anim.smoother((t - 0.72) / 0.14);
                hand = Anim.lerp(this.edge(this.layoutPoint(0).m_82520_(0.0, 0.22500000894069672 * (double)this.geometry().scale(0) * 0.25, 0.0)), press, k);
            } else {
                hand = Anim.lerp(press, restRight, Anim.smoother((t - 0.86) / 0.14));
            }
        } else {
            hand = t < 0.2 ? Anim.lerp(restRight, press, Anim.smoother(t / 0.1)).m_82520_(0.0, -0.03 * Anim.hump(t / 0.2), 0.0) : (t < 0.55 ? Anim.lerp(press, restRight, Anim.smoother((t - 0.2) / 0.2)) : (disc != null && t < 0.7 ? Anim.lerp(restRight, this.edge(disc), Anim.smoother((t - 0.55) / 0.15)) : (disc != null ? this.edge(disc) : restRight)));
        }
        return new Vec3[]{hand, restLeft};
    }

    private Vec3 edge(Vec3 disc) {
        return disc.m_82549_(this.layoutAxis(0, true).m_82490_(0.22500000894069672 * (double)this.geometry().scale(0) * 0.88)).m_82549_(this.side().m_82490_(0.03)).m_82520_(0.0, 0.01, 0.0);
    }

    @Override
    public double handsStiffness() {
        return 26.0;
    }

    @Override
    public void renderWorld(PoseStack pose, MultiBufferSource buffers, Vec3 camera, int light) {
        this.gridEditor.renderWorld(pose, buffers, camera, light);
        Vec3 at = this.recordPoint();
        if (at == null || this.record.m_41619_()) {
            return;
        }
        float size = 0.6f * this.geometry().scale(0);
        if (this.move == Move.EJECT) {
            size *= (float)(1.0 - 0.35 * Anim.smoother(Anim.window(this.progress(), 0.75, 1.0)));
        }
        ItemPlacer.drawOriented(pose, buffers, camera, this.record, at, this.recordTurn(), size, light);
    }

    @Override
    public Component hint() {
        if (this.move != Move.NONE && !this.record.m_41619_()) {
            MutableComponent mutableComponent;
            Item item = this.record.m_41720_();
            if (item instanceof RecordItem) {
                RecordItem disc = (RecordItem)item;
                mutableComponent = disc.m_43050_();
            } else {
                mutableComponent = null;
            }
            MutableComponent song = mutableComponent;
            return Component.m_237113_((String)"\u266a ").m_7220_((Component)(song != null ? song : this.record.m_41786_()));
        }
        if (this.playing()) {
            return Component.m_237115_((String)"cinematic.jukebox.hint.take");
        }
        return Component.m_237115_((String)(JukeboxStation.isRecord(Hands.carried()) ? "cinematic.jukebox.hint.put" : "cinematic.jukebox.hint.choose"));
    }

    @Override
    @Nullable
    public List<Component> tooltip(ItemStack carried) {
        double[] pointer = Picking.mouseGui();
        List<Component> editorTip = this.gridEditor.tooltip(pointer[0], pointer[1]);
        if (editorTip != null) {
            return editorTip;
        }
        if (!this.hoverTop || this.move != Move.NONE) {
            return null;
        }
        if (this.playing()) {
            return List.of(this.inside().m_41786_(), Component.m_237115_((String)"cinematic.jukebox.take").m_130940_(ChatFormatting.GRAY));
        }
        if (JukeboxStation.isRecord(carried)) {
            return List.of(carried.m_41786_(), Component.m_237115_((String)"cinematic.jukebox.put").m_130940_(ChatFormatting.GRAY));
        }
        return List.of(Component.m_237115_((String)(carried.m_41619_() ? "cinematic.jukebox.empty" : "cinematic.jukebox.not_record")).m_130940_(carried.m_41619_() ? ChatFormatting.GRAY : ChatFormatting.RED));
    }

    public String moveName() {
        return this.move.name().toLowerCase(Locale.ROOT) + String.format(" %.2f", this.move == Move.NONE ? 0.0 : this.progress());
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

    private static enum Move {
        NONE,
        INSERT,
        EJECT;

    }
}


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
 *  net.minecraft.client.gui.screens.inventory.EnchantmentNames
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.locale.Language
 *  net.minecraft.network.chat.CommonComponents
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.util.FormattedCharSequence
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.inventory.ClickType
 *  net.minecraft.world.inventory.EnchantmentMenu
 *  net.minecraft.world.item.ItemDisplayContext
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.item.enchantment.Enchantment
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.EnchantmentTableBlockEntity
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Vector3f
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dyslexic.cinematic.client.Anim;
import dev.dyslexic.cinematic.client.CameraRig;
import dev.dyslexic.cinematic.client.ClientStation;
import dev.dyslexic.cinematic.client.EnchantFx;
import dev.dyslexic.cinematic.client.Hands;
import dev.dyslexic.cinematic.client.ItemPlacer;
import dev.dyslexic.cinematic.client.Picking;
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
import net.minecraft.client.gui.screens.inventory.EnchantmentNames;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.EnchantmentTableBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class EnchantStation
extends ClientStation
implements SpatialTarget {
    final SpatialEditor gridEditor = new SpatialEditor(this);
    static final double OFFER_HIGH = 0.92;
    static final double OFFER_LOW = 0.74;
    static final double OFFER_FAR = -0.12;
    static final double OFFER_NEAR = 0.02;
    static final double OFFER_SPREAD = 0.4;
    static final double HAND_Y = 0.062;
    static final double ITEM_REACH = 0.075;
    public static final double RITUAL_LENGTH = 1.5;
    public static final double RITUAL_PEAK = 0.5;
    private static final float ITEM_SIZE = 0.34f;
    private static final double SIGIL_TEXT_LIFT = 0.01;
    public final Vec3 top;
    private final List<Vec3> shelves;
    private final RandomSource random = RandomSource.m_216327_();
    public Target target = Target.NONE;
    public int hoverOffer = -1;
    private boolean aimAir;
    private int lastHoverOffer = -1;
    private int runeSeed = Integer.MIN_VALUE;
    private final FormattedCharSequence[] runes = new FormattedCharSequence[3];
    private double offersAt = -10.0;
    private int offersFingerprint;
    private double ritualAt = -10.0;
    private int ritualOffer;
    private ItemStack ritualItem = ItemStack.f_41583_;
    private int ritualLapis;
    private Vec3 ritualFrom = Vec3.f_82478_;
    private boolean burstDone;
    private double burstAt = -10.0;
    private double denyAt = -10.0;
    private int denyOffer = -1;
    private double glow;
    private double lapisFocus;
    private ItemStack leavingItem = ItemStack.f_41583_;
    private int leavingLapis;

    public EnchantStation(BlockPos pos, Direction side) {
        super(pos, side);
        this.top = EnchantFx.top(pos);
        Minecraft mc = Minecraft.m_91087_();
        this.shelves = mc.f_91073_ == null ? List.of() : EnchantFx.shelves(mc.f_91073_, pos);
    }

    @Override
    public StationKind kind() {
        return StationKind.ENCHANTING;
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
    public SpatialEditor layoutEditor() {
        return this.gridEditor;
    }

    @Override
    public SpatialAppearance.Frame layoutFrame() {
        return SpatialAppearance.Frame.of(this.kind(), this.pos, this.side, Direction.UP);
    }

    @Override
    public ItemStack layoutItem(int slot) {
        return slot == 0 ? this.item() : (slot == 1 ? (this.lapis() > 0 ? new ItemStack((ItemLike)Items.f_42534_) : ItemStack.f_41583_) : (this.offered(slot - 2) ? new ItemStack((ItemLike)Items.f_42690_) : ItemStack.f_41583_));
    }

    @Override
    public Vec3 layoutPoint(int slot) {
        return slot == 0 ? this.itemPos() : (slot == 1 ? this.corner() : this.sigilPos(slot - 2));
    }

    @Override
    public Vec3 layoutNormal(int slot) {
        return Picking.camera().m_82546_(this.layoutPoint(slot)).m_82541_();
    }

    @Override
    public Vec3 layoutAxis(int slot, boolean across) {
        Vec3 normal = this.layoutNormal(slot);
        Vec3 a = new Vec3(0.0, 1.0, 0.0).m_82537_(normal).m_82541_();
        Vec3 b = normal.m_82537_(a);
        double angle = Math.toRadians(this.geometry().rotation(slot));
        return across ? a.m_82490_(Math.cos(angle)).m_82549_(b.m_82490_(Math.sin(angle))) : b.m_82490_(Math.cos(angle)).m_82546_(a.m_82490_(Math.sin(angle)));
    }

    @Override
    public boolean busy() {
        return this.ritualTime() >= 0.0 || this.gridEditor.editing();
    }

    @Override
    public boolean showsInventory() {
        return !this.gridEditor.visible();
    }

    @Override
    public void beforeFrame() {
        this.gridEditor.frame();
    }

    @Override
    public void renderScreen(GuiGraphics g, Font font, float alpha) {
        this.gridEditor.render(g, font, alpha);
    }

    @Override
    public List<Vec3> workSamples() {
        ArrayList<Vec3> points = new ArrayList<Vec3>(super.workSamples());
        if (!this.geometry().equals(SpatialAppearance.VANILLA)) {
            points.add(this.corner());
        }
        return points;
    }

    @Override
    public float layoutHeldScale() {
        return this.target == Target.LAPIS ? this.geometry().scale(1) : (this.target == Target.ITEM || this.target == Target.SURFACE ? this.geometry().scale(0) : 1.0f);
    }

    @Override
    public void layoutGhost(PoseStack pose, MultiBufferSource buffers, Vec3 camera, int slot, int light) {
        if (slot == 0) {
            EnchantStation.drawFloating(pose, buffers, camera, new ItemStack((ItemLike)Items.f_42388_), this.itemPos(), (float)(Scene.clock() * 40.0 + this.geometry().rotation(0)), 0.34f * this.geometry().scale(0));
        } else if (slot == 1) {
            EnchantStation.drawLapis(pose, buffers, camera, this.corner(), 1, light, this.geometry().scale(1), (float)(-this.geometry().rotation(1)));
        } else {
            this.drawSigil(pose, buffers, Minecraft.m_91087_().f_91063_.m_109153_(), slot - 2, this.sigilPos(slot - 2), this.gridEditor.presence(), false, true, 0.6f * this.gridEditor.presence(), slot - 1);
        }
    }

    static Vec3 itemPosition(BlockPos pos, Direction side, double clock) {
        return SpatialLayouts.current(StationKind.ENCHANTING).point(SpatialAppearance.Frame.of(StationKind.ENCHANTING, pos, side, Direction.UP), 0).m_82520_(0.0, EnchantFx.itemCenter((BlockPos)pos, (Direction)side, (double)clock).f_82480_ - EnchantFx.itemCenter((BlockPos)pos, (Direction)side, (double)0.0).f_82480_, 0.0);
    }

    static Vec3 lapisPosition(BlockPos pos, Direction side) {
        return SpatialLayouts.current(StationKind.ENCHANTING).point(SpatialAppearance.Frame.of(StationKind.ENCHANTING, pos, side, Direction.UP), 1);
    }

    @Override
    public List<Vec3> approach(LocalPlayer player) {
        Vec3 stand = new Vec3(this.top.f_82479_ + (double)this.side.m_122429_() * 0.86, player.m_20186_(), this.top.f_82481_ + (double)this.side.m_122431_() * 0.86);
        boolean walk = Math.hypot(stand.f_82479_ - player.m_20185_(), stand.f_82481_ - player.m_20189_()) > 0.06;
        return walk ? List.of(stand) : List.of();
    }

    @Override
    public CameraRig.Pose lean() {
        Vec3 eye = this.top.m_82549_(this.side().m_82490_(1.15)).m_82520_(0.0, 1.0, 0.0);
        Vec3 look = this.top.m_82520_(0.0, 0.5, 0.0).m_82546_(eye);
        float yaw = (float)Math.toDegrees(Math.atan2(-look.f_82479_, look.f_82481_));
        float pitch = (float)(-Math.toDegrees(Math.atan2(look.f_82480_, Math.hypot(look.f_82479_, look.f_82481_))));
        return new CameraRig.Pose(eye, yaw, pitch, 0.0f);
    }

    @Override
    public double leanFov() {
        return 68.0;
    }

    @Override
    public float bodyPitch() {
        return 30.0f;
    }

    @Override
    public Vec3 rest(boolean right) {
        return this.top.m_82549_(this.side().m_82490_(0.36)).m_82549_(this.right().m_82490_(right ? 0.24 : -0.24)).m_82520_(0.0, 0.062, 0.0);
    }

    @Nullable
    public static EnchantmentMenu menu() {
        EnchantmentMenu menu;
        AbstractContainerMenu abstractContainerMenu;
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        return player != null && (abstractContainerMenu = player.f_36096_) instanceof EnchantmentMenu ? (menu = (EnchantmentMenu)abstractContainerMenu) : null;
    }

    public ItemStack item() {
        double t = this.ritualTime();
        if (t >= 0.0 && t < 0.5) {
            return this.ritualItem;
        }
        EnchantmentMenu menu = EnchantStation.menu();
        return menu == null ? ItemStack.f_41583_ : menu.m_38853_(0).m_7993_();
    }

    public int lapis() {
        EnchantmentMenu menu = EnchantStation.menu();
        return menu == null ? 0 : menu.m_38853_(1).m_7993_().m_41613_();
    }

    private boolean offered(int k) {
        EnchantmentMenu menu = EnchantStation.menu();
        return menu != null && menu.f_39446_[k] > 0 && !this.item().m_41619_();
    }

    private boolean affordable(int k) {
        EnchantmentMenu menu = EnchantStation.menu();
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        if (menu == null || player == null || menu.f_39446_[k] <= 0 || menu.f_39447_[k] < 0) {
            return false;
        }
        if (player.m_150110_().f_35937_) {
            return true;
        }
        return player.f_36078_ >= menu.f_39446_[k] && player.f_36078_ >= k + 1 && this.lapis() >= k + 1;
    }

    public Vec3 itemPos() {
        double t = this.ritualTime();
        double lift = t >= 0.0 ? 0.1 * Anim.hump(Anim.window(t * 1.5, 0.2, 1.3)) : 0.0;
        return EnchantStation.itemPosition(this.pos, this.side, Scene.clock()).m_82520_(0.0, lift, 0.0);
    }

    public Vec3 sigilPos(int k) {
        double d;
        double clock = Scene.clock();
        Vec3 at = this.geometry().point(this.layoutFrame(), k + 2).m_82520_(0.0, 0.015 * Math.sin(clock * 1.3 + (double)k * 2.1), 0.0);
        if (k == this.denyOffer && (d = clock - this.denyAt) >= 0.0 && d < 0.35) {
            at = at.m_82549_(this.right().m_82490_(0.02 * Math.sin(d * 60.0) * (1.0 - d / 0.35)));
        }
        return at;
    }

    public Vec3 corner() {
        return EnchantStation.lapisPosition(this.pos, this.side);
    }

    private static double guiRadius(Vec3 center, double radius) {
        double[] a = Picking.toGui(center);
        double[] b = Picking.toGui(center.m_82520_(0.0, radius, 0.0));
        return a == null || b == null ? 0.0 : Math.max(8.0, Math.hypot(a[0] - b[0], a[1] - b[1]));
    }

    private static boolean under(double[] mouse, Vec3 center, double radius) {
        double[] at = Picking.toGui(center);
        if (at == null) {
            return false;
        }
        double r = EnchantStation.guiRadius(center, radius);
        return Math.hypot(mouse[0] - at[0], mouse[1] - at[1]) < r;
    }

    @Override
    public void updateCursor(boolean overPanel) {
        this.clearCursor();
        if (overPanel || this.gridEditor.visible() || EnchantStation.menu() == null) {
            return;
        }
        double[] mouse = Picking.mouseGui();
        if (!this.geometry().equals(SpatialAppearance.VANILLA)) {
            int hit = this.layoutPick();
            if (hit >= 2 && this.offered(hit - 2) && this.ritualTime() < 0.0) {
                this.target = Target.OFFER;
                this.hoverOffer = hit - 2;
                this.aim = this.sigilPos(this.hoverOffer);
                this.aimAir = true;
                return;
            }
            if (hit == 0) {
                this.target = this.item().m_41619_() ? Target.SURFACE : Target.ITEM;
                this.aim = this.itemPos();
                this.aimAir = true;
                return;
            }
            if (hit == 1) {
                this.target = Target.LAPIS;
                this.aim = this.corner();
                this.aimAir = true;
                return;
            }
        }
        if (this.geometry().equals(SpatialAppearance.VANILLA) && this.ritualTime() < 0.0) {
            for (int k = 0; k < 3; ++k) {
                if (!this.offered(k) || !EnchantStation.under(mouse, this.sigilPos(k), 0.15)) continue;
                this.target = Target.OFFER;
                this.hoverOffer = k;
                this.aim = this.sigilPos(k);
                this.aimAir = true;
                return;
            }
        }
        if (this.geometry().equals(SpatialAppearance.VANILLA) && !this.item().m_41619_() && EnchantStation.under(mouse, this.itemPos(), 0.15)) {
            this.target = Target.ITEM;
            this.aim = this.itemPos();
            this.aimAir = true;
            return;
        }
        if (this.geometry().equals(SpatialAppearance.VANILLA) && EnchantStation.under(mouse, this.corner().m_82520_(0.0, 0.06, 0.0), 0.09)) {
            this.target = Target.LAPIS;
            this.aim = this.corner();
            this.aimAir = true;
            return;
        }
        Vec3 socketHit = Picking.cursorOnPlane(this.corner().f_82480_);
        if (this.geometry().equals(SpatialAppearance.VANILLA) && socketHit != null && socketHit.m_82557_(this.corner()) <= 0.013225) {
            this.target = Target.LAPIS;
            this.aim = this.corner();
            this.aimAir = true;
            return;
        }
        Vec3 hit = Picking.cursorOnPlane(this.top.f_82480_);
        if (hit == null) {
            return;
        }
        Vec3 rel = hit.m_82546_(this.top);
        if (Math.abs(rel.f_82479_) > 0.52 || Math.abs(rel.f_82481_) > 0.52) {
            return;
        }
        this.aimAir = false;
        this.target = Target.SURFACE;
        this.aim = new Vec3(this.top.f_82479_ + Math.max(-0.43, Math.min(0.43, rel.f_82479_)), this.top.f_82480_, this.top.f_82481_ + Math.max(-0.43, Math.min(0.43, rel.f_82481_)));
    }

    @Override
    public void clearCursor() {
        super.clearCursor();
        this.target = Target.NONE;
        this.hoverOffer = -1;
    }

    @Override
    public Vec3 handTarget(Vec3 aim, Vec3 shoulder, boolean carrying, double tap) {
        if (this.aimAir) {
            Vec3 dir = aim.m_82546_(shoulder).m_82541_();
            return aim.m_82546_(dir.m_82490_(0.14 - 0.05 * tap)).m_82520_(0.0, -0.03, 0.0);
        }
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
    @Nullable
    public Vec3[] handsOverride() {
        double t = this.ritualTime();
        if (t < 0.0) {
            return null;
        }
        double raise = Anim.hump(Anim.window(t, 0.0, 0.85));
        Vec3 c = this.itemPos().m_82549_(this.side().m_82490_(0.07)).m_82520_(0.0, -0.1 + 0.05 * raise, 0.0);
        double spread = (0.17 - 0.04 * raise) * Math.max(0.8, (double)this.geometry().scale(0));
        return new Vec3[]{c.m_82549_(this.right().m_82490_(spread)), c.m_82549_(this.right().m_82490_(-spread))};
    }

    @Override
    public void click(int button, boolean shift) {
        if (this.gridEditor.visible()) {
            return;
        }
        EnchantmentMenu menu = EnchantStation.menu();
        if (menu == null) {
            return;
        }
        switch (this.target) {
            case OFFER: {
                if (this.ritualTime() >= 0.0) {
                    return;
                }
                if (this.affordable(this.hoverOffer)) {
                    this.startRitual(menu, this.hoverOffer);
                    break;
                }
                this.denyAt = Scene.clock();
                this.denyOffer = this.hoverOffer;
                Scene.sound(SoundEvents.f_144098_, 0.7f, 0.7f);
                break;
            }
            case ITEM: 
            case SURFACE: {
                this.slotClick(menu, 0, button, shift);
                break;
            }
            case LAPIS: {
                this.slotClick(menu, 1, button, shift);
                break;
            }
        }
    }

    private void slotClick(EnchantmentMenu menu, int slot, int button, boolean shift) {
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91072_ == null || mc.f_91074_ == null || this.ritualTime() >= 0.0) {
            return;
        }
        Hands.tap();
        boolean placing = !Hands.carried().m_41619_();
        mc.f_91072_.m_171799_(menu.f_38840_, slot, button, shift ? ClickType.QUICK_MOVE : ClickType.PICKUP, (Player)mc.f_91074_);
        if (placing) {
            Scene.sound(SoundEvents.f_144243_, 0.5f, slot == 0 ? 1.5f : 1.1f);
        }
    }

    private void startRitual(EnchantmentMenu menu, int k) {
        Minecraft mc = Minecraft.m_91087_();
        this.ritualFrom = this.sigilPos(k);
        this.ritualItem = this.item().m_41777_();
        this.ritualOffer = k;
        this.ritualLapis = k + 1;
        this.ritualAt = Scene.clock();
        this.burstDone = false;
        mc.f_91072_.m_105208_(menu.f_38840_, k);
        Scene.sound(SoundEvents.f_11736_, 0.45f, 1.7f);
        Scene.sound(SoundEvents.f_276532_, 0.8f, 0.9f);
    }

    public double ritualTime() {
        double t = (Scene.clock() - this.ritualAt) / 1.5;
        return t >= 0.0 && t <= 1.0 ? t : -1.0;
    }

    @Override
    public double cameraKick() {
        double t = this.ritualTime();
        if (t < 0.0) {
            return 0.0;
        }
        double peak = 0.3333333333333333;
        return t < peak ? 0.6 * Anim.smoother(t / peak) : Math.exp(-(t - peak) * 1.5 * 3.2);
    }

    @Override
    public double flash() {
        double d = Scene.clock() - this.burstAt;
        return d >= 0.0 && d < 0.5 ? Math.pow(1.0 - d / 0.5, 2.0) * 0.26 : 0.0;
    }

    @Override
    public int flashColor() {
        return 15260927;
    }

    @Override
    public void onEnd() {
        this.gridEditor.cancel();
        this.leavingItem = this.item().m_41777_();
        this.leavingLapis = this.lapis();
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        if (!Scene.clientOnly() && player != null && player.f_36096_ instanceof EnchantmentMenu) {
            player.f_36096_ = player.f_36095_;
        }
    }

    @Override
    public void tick() {
        ItemStack item;
        BlockEntity blockEntity;
        boolean stirring;
        Minecraft mc = Minecraft.m_91087_();
        EnchantmentMenu menu = EnchantStation.menu();
        if (menu == null || mc.f_91073_ == null) {
            return;
        }
        int seed = menu.m_39493_();
        int fingerprint = seed * 31 + menu.f_39446_[0] * 7 + menu.f_39446_[1] * 5 + menu.f_39446_[2] * 3 + (this.item().m_41619_() ? 0 : 1);
        if (seed != this.runeSeed || fingerprint != this.offersFingerprint) {
            this.runeSeed = seed;
            this.offersFingerprint = fingerprint;
            this.offersAt = Scene.clock();
            EnchantmentNames.m_98734_().m_98735_((long)seed);
            for (int k = 0; k < 3; ++k) {
                this.runes[k] = Language.m_128107_().m_5536_(EnchantmentNames.m_98734_().m_98737_(mc.f_91062_, 66));
            }
        }
        if (this.hoverOffer != this.lastHoverOffer && this.hoverOffer >= 0) {
            Scene.sound(SoundEvents.f_11713_, 0.5f, 0.9f + this.random.m_188501_() * 0.3f);
            Scene.sound(SoundEvents.f_144243_, 0.25f, 1.6f + (float)this.hoverOffer * 0.2f);
        }
        this.lastHoverOffer = this.hoverOffer;
        double t = this.ritualTime();
        boolean bl = stirring = this.hoverOffer >= 0 || t >= 0.0;
        if (stirring && (blockEntity = mc.f_91073_.m_7702_(this.pos)) instanceof EnchantmentTableBlockEntity) {
            EnchantmentTableBlockEntity book = (EnchantmentTableBlockEntity)blockEntity;
            if (mc.f_91074_.f_19797_ % 3 == 0) {
                book.f_59254_ = book.f_59254_ + (float)((this.random.m_188499_() ? 1 : -1) * (1 + this.random.m_188503_(2)));
            }
        }
        if (!(item = this.item()).m_41619_() && mc.f_91074_.f_19797_ % 3 == 0) {
            EnchantFx.sparkle(this.itemPos(), 0.2);
        }
        if (this.hoverOffer >= 0) {
            Vec3 sigil = this.sigilPos(this.hoverOffer);
            EnchantFx.stream(this.source(), sigil, 1);
            EnchantFx.stream(sigil, this.itemPos(), 1);
        }
        if (t >= 0.0 && t * 1.5 < 0.5) {
            for (int i = 0; i < 3; ++i) {
                EnchantFx.stream(this.source(), this.itemPos(), 1);
                EnchantFx.gather(this.itemPos(), 0.55);
            }
        }
        if (t >= 0.0 && !this.burstDone && t * 1.5 >= 0.5) {
            this.burstDone = true;
            this.burstAt = Scene.clock();
            EnchantFx.burst(this.itemPos(), 1.4);
            Scene.sound(SoundEvents.f_11739_, 0.7f, 1.3f);
            Scene.sound(SoundEvents.f_144243_, 1.0f, 0.7f);
        }
    }

    private Vec3 source() {
        if (!this.shelves.isEmpty()) {
            return this.shelves.get(this.random.m_188503_(this.shelves.size())).m_82520_(0.0, 0.3, 0.0);
        }
        double a = this.random.m_188500_() * Math.PI * 2.0;
        return this.top.m_82520_(Math.cos(a) * 1.6, 0.6 + this.random.m_188500_() * 0.8, Math.sin(a) * 1.6);
    }

    public void renderScene(PoseStack pose, MultiBufferSource buffers, Camera camera, int light) {
        double presence;
        Vec3 cam = camera.m_90583_();
        double clock = Scene.clock();
        double t = this.ritualTime();
        double target = switch (Scene.phase) {
            case Scene.Phase.INTRO -> 0.5 * Anim.smoother(Anim.window(Scene.introProgress(), 0.45, 1.0));
            case Scene.Phase.ACTIVE -> 0.45 + (this.item().m_41619_() ? 0.0 : 0.2) + (this.hoverOffer >= 0 ? 0.3 : 0.0) + (t >= 0.0 ? 1.4 * Anim.hump(Anim.window(t, 0.0, 0.75)) : 0.0);
            default -> 0.0;
        };
        this.glow = Anim.approach(this.glow, target, target > this.glow ? 6.0 : 2.5, Scene.frameDt);
        double spin = clock * 0.25 + (t >= 0.0 ? 4.0 * Anim.smoother(t) : 0.0);
        float g = (float)this.glow;
        boolean canEnchant = this.item().m_41619_() || t >= 0.0;
        for (int k = 0; k < 3 && !canEnchant; ++k) {
            canEnchant = this.offered(k) && this.affordable(k);
        }
        EnchantFx.CirclePalette tablePalette = canEnchant ? EnchantFx.CirclePalette.VIOLET : EnchantFx.CirclePalette.RED;
        EnchantFx.circle(pose, buffers, cam, this.top.m_82520_(0.0, 0.012, 0.0), 0.47, spin, tablePalette, 0.85f * g);
        EnchantFx.circle(pose, buffers, cam, this.top.m_82520_(0.0, 0.02, 0.0), 0.25, -spin * 1.7, tablePalette, 0.55f * g);
        boolean outro = Scene.phase == Scene.Phase.OUTRO;
        double fade = outro ? 1.0 - Anim.smoother(Scene.time / 0.45) : 1.0;
        ItemStack item = outro ? this.leavingItem : this.item();
        Vec3 at = this.itemPos();
        if (!item.m_41619_() && fade > 0.01) {
            EnchantFx.billboard(pose, buffers, camera, at, 0.24, clock * 0.4, EnchantFx.GLOW, 0.3f * g, 0.18f * g, 0.5f * g);
            double extraSpin = t >= 0.0 ? 900.0 * Anim.outCubic(t) : 0.0;
            double pulse = t >= 0.0 && t * 1.5 > 0.5 ? 1.0 + 0.18 * Anim.hump(Anim.window(t, 0.3333333333333333, 0.8)) : 1.0;
            EnchantStation.drawFloating(pose, buffers, cam, item, at, (float)(clock * 40.0 + extraSpin + this.geometry().rotation(0)), (float)((double)0.34f * fade * pulse * (double)this.geometry().scale(0)));
        }
        int lapis = outro ? this.leavingLapis : this.lapis();
        double d = presence = Scene.phase == Scene.Phase.INTRO ? Anim.smoother(Anim.window(Scene.introProgress(), 0.45, 1.0)) : fade;
        this.lapisFocus = Anim.approach(this.lapisFocus, t >= 0.0 ? 0.0 : (this.target == Target.LAPIS ? 1.0 : (Hands.carried().m_150930_(Items.f_42534_) ? 0.65 : 0.0)), 12.0, Scene.frameDt);
        if (presence > 0.01) {
            float socket = (float)((0.5 + 0.42 * this.lapisFocus) * presence);
            boolean wrong = this.target == Target.LAPIS && !Hands.carried().m_41619_() && !Hands.carried().m_150930_(Items.f_42534_);
            EnchantFx.circle(pose, buffers, cam, this.corner().m_82520_(0.0, 0.016, 0.0), 0.1 * (double)this.geometry().scale(1), clock * 0.3 + Math.toRadians(this.geometry().rotation(1)), wrong ? EnchantFx.CirclePalette.RED : EnchantFx.CirclePalette.LAPIS, socket);
        }
        if (lapis > 0 && fade > 0.01) {
            EnchantStation.drawLapis(pose, buffers, cam, this.corner(), lapis, light, (float)fade * this.geometry().scale(1), (float)(-this.geometry().rotation(1)));
            EnchantStation.drawLapisCount(pose, buffers, camera, this.corner().m_82520_(0.0, 0.115 * (double)this.geometry().scale(1), 0.0), lapis, (float)presence);
        }
        if (t >= 0.0) {
            double beam;
            double peak = 0.3333333333333333;
            double d2 = beam = t < peak ? 0.0 : 1.0 - Anim.smoother((t - peak) / (1.0 - peak));
            if (beam > 0.01) {
                float b = (float)(beam * 1.3);
                EnchantFx.beam(pose, buffers, cam, new Vec3(this.itemPos().f_82479_, this.top.f_82480_, this.itemPos().f_82481_), 1.9, 0.5 + 0.3 * beam, clock * 0.9, 0.7f * b, 0.5f * b, 1.0f * b);
            }
        }
    }

    @Override
    public void renderWorld(PoseStack pose, MultiBufferSource buffers, Vec3 camera, int light) {
        this.gridEditor.renderWorld(pose, buffers, camera, light);
        Minecraft mc = Minecraft.m_91087_();
        Camera cam = mc.f_91063_.m_109153_();
        EnchantmentMenu menu = EnchantStation.menu();
        if (menu == null) {
            return;
        }
        double clock = Scene.clock();
        double t = this.ritualTime();
        double appear = Anim.outBack(Anim.window(clock - this.offersAt, 0.0, 0.45), 1.6);
        for (int k = 0; k < 3; ++k) {
            if (!this.offered(k) || t >= 0.0 && k == this.ritualOffer) continue;
            float dim = t >= 0.0 ? 0.4f : 1.0f;
            this.drawSigil(pose, buffers, cam, k, this.sigilPos(k), (float)appear, k == this.hoverOffer, this.affordable(k), dim, menu.f_39446_[k]);
        }
        if (t >= 0.0) {
            double flight = Anim.inOutCubic(Anim.window(t * 1.5, 0.0, 0.45));
            if (flight < 1.0) {
                Vec3 to = this.itemPos();
                Vec3 p = Anim.lerp(this.ritualFrom, to, flight).m_82520_(0.0, 0.12 * Anim.hump(flight), 0.0);
                this.drawSigil(pose, buffers, cam, this.ritualOffer, p, (float)(1.0 - 0.8 * flight), true, true, 1.6f, menu.f_39446_[this.ritualOffer]);
            }
            for (int i = 0; i < this.ritualLapis; ++i) {
                double f = Anim.inOutSine(Anim.window(t * 1.5, 0.04 + (double)i * 0.07, 0.48));
                if (!(f > 0.0) || !(f < 1.0)) continue;
                Vec3 p = Anim.lerp(this.corner().m_82520_(0.0, 0.02, 0.0), this.itemPos(), f).m_82520_(0.0, 0.18 * Anim.hump(f), 0.0);
                EnchantStation.drawFloating(pose, buffers, camera, new ItemStack((ItemLike)Items.f_42534_), p, (float)(clock * 300.0 + (double)(i * 90)), (float)(0.16 * (1.0 - 0.6 * f) * (double)this.geometry().scale(1)));
            }
        }
        if (this.target == Target.SURFACE && !Hands.carried().m_41619_() && this.aim != null) {
            float p = (float)(0.25 + 0.1 * Math.sin(clock * 5.0));
            EnchantFx.circle(pose, buffers, camera, this.top.m_82520_(0.0, 0.028, 0.0), 0.16, -clock, EnchantFx.CirclePalette.VIOLET, p);
        }
    }

    private static void drawLapisCount(PoseStack pose, MultiBufferSource buffers, Camera camera, Vec3 at, int count, float presence) {
        int alpha = Math.round(230.0f * presence);
        if (alpha < 8) {
            return;
        }
        Font font = Minecraft.m_91087_().f_91062_;
        Vec3 cam = camera.m_90583_();
        String label = "\u00d7" + count;
        pose.m_85836_();
        pose.m_85837_(at.f_82479_ - cam.f_82479_, at.f_82480_ - cam.f_82480_, at.f_82481_ - cam.f_82481_);
        pose.m_252781_(camera.m_253121_());
        pose.m_85841_(-0.0045f, -0.0045f, 0.0045f);
        font.m_271703_(label, (float)(-font.m_92895_(label)) / 2.0f, -4.0f, alpha << 24 | 0x9CC8FF, true, pose.m_85850_().m_252922_(), buffers, Font.DisplayMode.NORMAL, 0, 0xF000F0);
        pose.m_85849_();
    }

    private void drawSigil(PoseStack pose, MultiBufferSource buffers, Camera camera, int k, Vec3 at, float appear, boolean hovered, boolean affordable, float brightness, int cost) {
        if (appear <= 0.01f) {
            return;
        }
        double clock = Scene.clock();
        float size = (float)(0.13 * (double)appear * (hovered ? 1.2 : 1.0) * (double)this.geometry().scale(k + 2));
        float b = brightness * (hovered ? 1.35f : 0.85f) * Math.min(1.0f, appear);
        float r = affordable ? 0.55f : 0.5f;
        float g = affordable ? 0.45f : 0.12f;
        float bl = affordable ? 1.0f : 0.16f;
        EnchantFx.billboard(pose, buffers, camera, at, (double)size * 1.8, 0.0, EnchantFx.GLOW, r * b * 0.35f, g * b * 0.35f, bl * b * 0.35f);
        double turn = clock * (hovered ? 1.8 : 0.5) * (double)(k % 2 == 0 ? 1 : -1);
        EnchantFx.circleBillboard(pose, buffers, camera, at, size, turn + Math.toRadians(this.geometry().rotation(k + 2)), affordable ? EnchantFx.CirclePalette.VIOLET : EnchantFx.CirclePalette.RED, b);
        Font font = Minecraft.m_91087_().f_91062_;
        Vec3 cam = camera.m_90583_();
        Vector3f look = camera.m_253058_();
        Vec3 text = at.m_82492_((double)look.x() * 0.01, (double)look.y() * 0.01, (double)look.z() * 0.01);
        pose.m_85836_();
        pose.m_85837_(text.f_82479_ - cam.f_82479_, text.f_82480_ - cam.f_82480_, text.f_82481_ - cam.f_82481_);
        pose.m_252781_(camera.m_253121_());
        pose.m_252781_(Axis.f_252403_.m_252977_((float)(-this.geometry().rotation(k + 2))));
        float s = 0.01f * appear * (hovered ? 1.2f : 1.0f) * this.geometry().scale(k + 2);
        pose.m_85841_(-s, -s, s);
        String number = String.valueOf(cost);
        int numberColor = affordable ? -725249 : -38037;
        font.m_271703_(number, (float)(-font.m_92895_(number)) / 2.0f, -4.5f, numberColor, true, pose.m_85850_().m_252922_(), buffers, Font.DisplayMode.NORMAL, 0, 0xF000F0);
        FormattedCharSequence rune = this.runes[k];
        if (rune != null) {
            pose.m_85841_(0.55f, 0.55f, 0.55f);
            font.m_272191_(rune, (float)(-font.m_92724_(rune)) / 2.0f, 26.0f, affordable ? -4610305 : -8762795, false, pose.m_85850_().m_252922_(), buffers, Font.DisplayMode.NORMAL, 0, 0xF000F0);
            String gems = "\u25c6".repeat(k + 1);
            font.m_271703_(gems, (float)(-font.m_92895_(gems)) / 2.0f, 37.0f, -10778881, false, pose.m_85850_().m_252922_(), buffers, Font.DisplayMode.NORMAL, 0, 0xF000F0);
        }
        pose.m_85849_();
    }

    static void drawFloating(PoseStack pose, MultiBufferSource buffers, Vec3 cam, ItemStack item, Vec3 at, float spin, float size) {
        if (size <= 0.002f) {
            return;
        }
        Minecraft mc = Minecraft.m_91087_();
        pose.m_85836_();
        pose.m_85837_(at.f_82479_ - cam.f_82479_, at.f_82480_ - cam.f_82480_, at.f_82481_ - cam.f_82481_);
        pose.m_252781_(Axis.f_252436_.m_252977_(spin));
        boolean block = mc.m_91291_().m_174264_(item, (Level)mc.f_91073_, null, 0).m_7539_();
        float scale = block ? size * 1.4f : size;
        pose.m_85841_(scale, scale, scale);
        mc.m_91291_().m_269128_(item, ItemDisplayContext.FIXED, 0xF000F0, OverlayTexture.f_118083_, pose, buffers, (Level)mc.f_91073_, 0);
        pose.m_85849_();
    }

    static void drawLapis(PoseStack pose, MultiBufferSource buffers, Vec3 cam, Vec3 corner, int count, int light, float scale) {
        EnchantStation.drawLapis(pose, buffers, cam, corner, count, light, scale, 0.0f);
    }

    static void drawLapis(PoseStack pose, MultiBufferSource buffers, Vec3 cam, Vec3 corner, int count, int light, float scale, float rotation) {
        Vec3 pivot = corner.m_82546_(cam);
        pose.m_85836_();
        pose.m_85837_(pivot.f_82479_, pivot.f_82480_, pivot.f_82481_);
        pose.m_252781_(Axis.f_252436_.m_252977_(rotation));
        pose.m_85837_(-pivot.f_82479_, -pivot.f_82480_, -pivot.f_82481_);
        int copies = count >= 16 ? 3 : (count >= 4 ? 2 : 1);
        for (int i = 0; i < copies; ++i) {
            Vec3 at = corner.m_82520_(((double)i - (double)(copies - 1) / 2.0) * 0.05 * (double)scale, (0.001 + (double)i * 0.008) * (double)scale, (double)(i % 2) * 0.03 * (double)scale);
            ItemPlacer.render(pose, buffers, cam, new ItemStack((ItemLike)Items.f_42534_), at, Direction.NORTH, 4242L + (long)i * 17L, 0.8f * scale, 0.0f, light);
        }
        pose.m_85849_();
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
        EnchantmentMenu menu = EnchantStation.menu();
        if (menu == null) {
            return null;
        }
        switch (this.target) {
            case OFFER: {
                return this.offerTooltip(menu, this.hoverOffer);
            }
            case ITEM: {
                if (!carried.m_41619_()) {
                    return null;
                }
                ArrayList<Component> lines = new ArrayList<Component>(Screen.m_280152_((Minecraft)mc, (ItemStack)this.item()));
                lines.add((Component)Component.m_237115_((String)"cinematic.enchanting.take").m_130940_(ChatFormatting.GRAY));
                return lines;
            }
            case SURFACE: {
                return carried.m_41619_() && this.item().m_41619_() ? List.of(Component.m_237115_((String)"cinematic.enchanting.empty.item").m_130940_(ChatFormatting.GRAY)) : null;
            }
            case LAPIS: {
                if (!carried.m_41619_() && !carried.m_150930_(Items.f_42534_)) {
                    return List.of(Component.m_237115_((String)"cinematic.enchanting.only_lapis").m_130940_(ChatFormatting.RED));
                }
                if (!carried.m_41619_()) {
                    return null;
                }
                int lapis = this.lapis();
                return lapis > 0 ? List.of(Items.f_42534_.m_41466_().m_6881_().m_130946_(" \u00d7" + lapis)) : List.of(Component.m_237115_((String)"cinematic.enchanting.empty.lapis").m_130940_(ChatFormatting.GRAY));
            }
        }
        return null;
    }

    private List<Component> offerTooltip(EnchantmentMenu menu, int k) {
        Minecraft mc = Minecraft.m_91087_();
        ArrayList<Component> lines = new ArrayList<Component>();
        Enchantment clue = Enchantment.m_44697_((int)menu.f_39447_[k]);
        lines.add((Component)Component.m_237110_((String)"container.enchant.clue", (Object[])new Object[]{clue == null ? "" : clue.m_44700_(menu.f_39448_[k])}).m_130940_(ChatFormatting.WHITE));
        if (clue == null) {
            lines.add((Component)Component.m_237115_((String)"forge.container.enchant.limitedEnchantability").m_130940_(ChatFormatting.RED));
            return lines;
        }
        if (mc.f_91074_.m_150110_().f_35937_) {
            return lines;
        }
        lines.add(CommonComponents.f_237098_);
        int need = k + 1;
        if (mc.f_91074_.f_36078_ < menu.f_39446_[k]) {
            lines.add((Component)Component.m_237110_((String)"container.enchant.level.requirement", (Object[])new Object[]{menu.f_39446_[k]}).m_130940_(ChatFormatting.RED));
        } else {
            MutableComponent lapisLine = need == 1 ? Component.m_237115_((String)"container.enchant.lapis.one") : Component.m_237110_((String)"container.enchant.lapis.many", (Object[])new Object[]{need});
            lines.add((Component)lapisLine.m_130940_(this.lapis() >= need ? ChatFormatting.GRAY : ChatFormatting.RED));
            MutableComponent levelLine = need == 1 ? Component.m_237115_((String)"container.enchant.level.one") : Component.m_237110_((String)"container.enchant.level.many", (Object[])new Object[]{need});
            lines.add((Component)levelLine.m_130940_(ChatFormatting.GRAY));
        }
        if (this.affordable(k)) {
            lines.add((Component)Component.m_237115_((String)"cinematic.enchanting.touch").m_130940_(ChatFormatting.LIGHT_PURPLE));
        }
        return lines;
    }

    @Override
    public Component hint() {
        return Component.m_237115_((String)"cinematic.hint.enchanting");
    }

    public static enum Target {
        NONE,
        SURFACE,
        ITEM,
        LAPIS,
        OFFER;

    }
}


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
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.client.renderer.texture.TextureAtlasSprite
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Vec3i
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ServerboundSetBeaconPacket
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.tags.ItemTags
 *  net.minecraft.world.effect.MobEffect
 *  net.minecraft.world.inventory.BeaconMenu
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.block.entity.BeaconBlockEntity
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Vector3f
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dyslexic.cinematic.Cinematic;
import dev.dyslexic.cinematic.client.Anim;
import dev.dyslexic.cinematic.client.CameraRig;
import dev.dyslexic.cinematic.client.EnchantFx;
import dev.dyslexic.cinematic.client.EnchantStation;
import dev.dyslexic.cinematic.client.Fx;
import dev.dyslexic.cinematic.client.GrindstoneStation;
import dev.dyslexic.cinematic.client.Hands;
import dev.dyslexic.cinematic.client.MenuStation;
import dev.dyslexic.cinematic.client.Picking;
import dev.dyslexic.cinematic.client.RemainingLayouts;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.SpatialAppearance;
import dev.dyslexic.cinematic.client.SpatialEditor;
import dev.dyslexic.cinematic.client.SpatialTarget;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundSetBeaconPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class BeaconStation
extends MenuStation<BeaconMenu>
implements SpatialTarget {
    final SpatialEditor gridEditor = new SpatialEditor(this);
    static final double HAND_Y = 0.06;
    static final double ITEM_REACH = 0.07;
    static final double LIGHT_LENGTH = 3.0;
    static final double BURST = 0.62;
    public static final ResourceLocation RING = Cinematic.id("textures/fx/ring.png");
    public static final ResourceLocation RAY = Cinematic.id("textures/fx/ray.png");
    private final List<Icon> icons = new ArrayList<Icon>();
    private final Vec3 core;
    private final Vec3 top;
    public Target target = Target.NONE;
    public int hoverPower = -1;
    @Nullable
    private MobEffect primary;
    @Nullable
    private MobEffect secondary;
    private boolean chosenRead;
    private int lastHover = -1;
    private double burstAt = -10.0;
    private float[] burstColor = new float[]{1.0f, 0.95f, 0.7f};
    private ItemStack lightingWith = ItemStack.f_41583_;
    private double pull;

    @Override
    public SpatialEditor layoutEditor() {
        return this.gridEditor;
    }

    public BeaconStation(BlockPos pos, Direction side) {
        super(pos, side, BeaconMenu.class, 1);
        this.core = Vec3.m_82512_((Vec3i)pos);
        this.top = this.core.m_82520_(0.0, 0.5, 0.0);
    }

    @Override
    public StationKind kind() {
        return StationKind.BEACON;
    }

    @Override
    public Vec3 center() {
        return this.top;
    }

    @Override
    public SoundEvent touchSound() {
        return SoundEvents.f_11987_;
    }

    private int levels() {
        BeaconMenu menu = (BeaconMenu)this.menu();
        return menu == null ? 0 : menu.m_39056_();
    }

    private ItemStack payment() {
        return this.slot(0);
    }

    private List<Power> powers() {
        ArrayList<Power> list = new ArrayList<Power>();
        for (int tier = 0; tier < 3; ++tier) {
            MobEffect[] mobEffectArray = BeaconBlockEntity.f_58646_[tier];
            int n = mobEffectArray.length;
            for (int i = 0; i < n; ++i) {
                MobEffect effect = mobEffectArray[i];
                list.add(new Power(effect, tier, false, false));
            }
        }
        for (MobEffect effect : BeaconBlockEntity.f_58646_[3]) {
            list.add(new Power(effect, 3, true, false));
        }
        if (this.primary != null) {
            list.add(new Power(this.primary, 3, true, true));
        }
        return list;
    }

    private boolean open(Power power) {
        return power.tier() < this.levels();
    }

    private boolean chosen(Power power) {
        return power.secondary() ? power.effect().equals(this.secondary) && power.upgrade() == power.effect().equals(this.primary) : power.effect().equals(this.primary);
    }

    private boolean active(Power power) {
        BeaconMenu menu = (BeaconMenu)this.menu();
        if (menu == null) {
            return false;
        }
        return power.secondary() ? power.effect().equals(menu.m_39058_()) && power.upgrade() == power.effect().equals(menu.m_39057_()) : power.effect().equals(menu.m_39057_());
    }

    private boolean ready() {
        return this.primary != null && this.levels() > 0 && !this.payment().m_41619_();
    }

    Vec3 powerPos(int index) {
        return this.layoutPoint(index + 1).m_82520_(0.0, 0.012 * Math.sin(Scene.clock() * 1.4 + (double)index * 1.7), 0.0);
    }

    Vec3 paymentPos() {
        return this.layoutPoint(0).m_82520_(0.0, 0.012 * Math.sin(Scene.clock() * 1.9), 0.0);
    }

    private Vec3 beamAt(Vec3 point) {
        return new Vec3(this.core.f_82479_, point.f_82480_, this.core.f_82481_);
    }

    @Override
    protected Vec3 restPoint(int slot) {
        return this.paymentPos();
    }

    @Override
    public List<Vec3> approach(LocalPlayer player) {
        Vec3 stand = new Vec3(this.core.f_82479_ + (double)this.side.m_122429_() * 1.05, player.m_20186_(), this.core.f_82481_ + (double)this.side.m_122431_() * 1.05);
        boolean walk = Math.hypot(stand.f_82479_ - player.m_20185_(), stand.f_82481_ - player.m_20189_()) > 0.06;
        return walk ? List.of(stand) : List.of();
    }

    @Override
    public CameraRig.Pose lean() {
        double d = 1.5 + 1.7 * this.pull;
        double h = 0.62 + 1.5 * this.pull;
        Vec3 eye = this.top.m_82549_(this.side().m_82490_(d)).m_82520_(0.0, h, 0.0);
        Vec3 look = this.top.m_82520_(0.0, 0.42 - 0.85 * this.pull, 0.0).m_82546_(eye);
        float yaw = (float)Math.toDegrees(Math.atan2(-look.f_82479_, look.f_82481_));
        float pitch = (float)(-Math.toDegrees(Math.atan2(look.f_82480_, Math.hypot(look.f_82479_, look.f_82481_))));
        return new CameraRig.Pose(eye, yaw, pitch, 0.0f);
    }

    @Override
    public double leanFov() {
        return 60.0 + 8.0 * this.pull;
    }

    @Override
    public boolean freeCamera() {
        return this.pull > 0.01;
    }

    @Override
    public float bodyPitch() {
        return 10.0f;
    }

    @Override
    public Vec3 rest(boolean right) {
        return this.top.m_82549_(this.side().m_82490_(0.86)).m_82549_(this.right().m_82490_(right ? 0.32 : -0.32)).m_82520_(0.0, -0.1, 0.0);
    }

    @Override
    public void updateCursor(boolean overPanel) {
        this.clearCursor();
        if (overPanel || this.menu() == null || this.moving()) {
            return;
        }
        double[] mouse = Picking.mouseGui();
        ItemStack carried = Hands.carried();
        List<Power> list = this.powers();
        if (!this.geometry().equals(SpatialAppearance.VANILLA)) {
            int picked = this.layoutPick();
            if (picked > 0 && picked <= list.size() && carried.m_41619_()) {
                this.target = Target.POWER;
                this.hoverPower = picked - 1;
                this.aim = this.powerPos(this.hoverPower);
            } else if (picked == 0) {
                this.target = this.payment().m_41619_() ? (carried.m_41619_() ? Target.NONE : Target.TOP) : Target.PAYMENT;
                this.aim = this.paymentPos();
            }
            return;
        }
        if (carried.m_41619_()) {
            for (int i = 0; i < list.size(); ++i) {
                if (!GrindstoneStation.under(mouse, this.powerPos(i), 0.12)) continue;
                this.target = Target.POWER;
                this.hoverPower = i;
                this.aim = this.powerPos(i);
                return;
            }
        }
        if (!this.payment().m_41619_() && GrindstoneStation.under(mouse, this.paymentPos(), 0.1)) {
            this.target = Target.PAYMENT;
            this.aim = this.paymentPos();
            return;
        }
        if (!carried.m_41619_() && GrindstoneStation.under(mouse, this.top.m_82520_(0.0, 0.05, 0.0), 0.32)) {
            this.target = Target.TOP;
            this.aim = this.paymentPos();
        }
    }

    @Override
    public void clearCursor() {
        super.clearCursor();
        this.target = Target.NONE;
        this.hoverPower = -1;
    }

    @Override
    public Vec3 handTarget(Vec3 aim, Vec3 shoulder, boolean carrying, double tap) {
        Vec3 dir = aim.m_82546_(shoulder).m_82541_();
        double shortOf = this.target == Target.POWER ? 0.42 : (carrying ? 0.05 : 0.1);
        return aim.m_82546_(dir.m_82490_(shortOf - 0.05 * tap)).m_82520_(0.0, -0.03, 0.0);
    }

    @Override
    public Vec3 heldItemPoint(Vec3 hand, Vec3 shoulder) {
        Vec3 reach = new Vec3(hand.f_82479_ - shoulder.f_82479_, 0.0, hand.f_82481_ - shoulder.f_82481_).m_82541_();
        return hand.m_82549_(reach.m_82490_(0.07)).m_82520_(0.0, -0.039999999999999994, 0.0);
    }

    @Override
    @Nullable
    public Vec3[] handsOverride() {
        double t = this.moveTime();
        if (t < 0.0) {
            return null;
        }
        double raise = Anim.inOutSine(Anim.window(t, 0.0, 0.62));
        double away = Anim.smoother(Anim.window(t, 0.57, 0.87)) * (1.0 - Anim.smoother(Anim.window(t, 2.45, 3.0)));
        Vec3 cam = Picking.camera();
        float yaw = CameraRig.last() != null ? CameraRig.last().yaw() : 0.0f;
        Vec3[] hands = new Vec3[2];
        for (int i = 0; i < 2; ++i) {
            boolean right = i == 0;
            Vec3 rest = this.rest(right);
            Vec3 up = rest.m_82549_(this.side().m_82490_(-0.12)).m_82549_(this.right().m_82490_(right ? -0.06 : 0.06)).m_82520_(0.0, 0.16, 0.0);
            hands[i] = Anim.lerp(Anim.lerp(rest, up, raise * (1.0 - away)), Hands.hidden(right, cam, yaw), away);
        }
        return hands;
    }

    @Override
    public double handsStiffness() {
        return 22.0;
    }

    @Override
    protected void act(int button, boolean shift) {
        switch (this.target) {
            case POWER: {
                this.choose(this.hoverPower);
                break;
            }
            case PAYMENT: {
                if (button == 0 && !shift && Hands.carried().m_41619_() && this.ready()) {
                    this.light();
                    break;
                }
                this.slotClick(0, 0, false);
                break;
            }
            case TOP: {
                if (!Hands.carried().m_204117_(ItemTags.f_13164_)) {
                    this.deny();
                    return;
                }
                this.slotClick(0, 1, false);
                Scene.sound(SoundEvents.f_144243_, 0.6f, 1.2f);
                break;
            }
        }
    }

    private void choose(int index) {
        List<Power> list = this.powers();
        if (index < 0 || index >= list.size()) {
            return;
        }
        Power power = list.get(index);
        if (!this.open(power)) {
            this.deny();
            return;
        }
        if (power.secondary()) {
            this.secondary = power.effect();
        } else {
            boolean upgraded = this.secondary != null && this.secondary.equals(this.primary);
            this.primary = power.effect();
            if (upgraded) {
                this.secondary = this.primary;
            }
        }
        Scene.sound(SoundEvents.f_11739_, 0.35f, 1.6f);
        Scene.sound(SoundEvents.f_144243_, 0.5f, 1.4f + (float)power.tier() * 0.15f);
        float[] c = BeaconStation.color(power.effect());
        Fx.motes(this.powerPos(index), 0.06, 10, c[0], c[1], c[2], 0.15);
    }

    private void light() {
        MobEffect first = this.primary;
        MobEffect second = this.levels() >= 4 ? this.secondary : null;
        this.lightingWith = this.payment().m_41777_();
        this.burstColor = BeaconStation.color(first);
        Scene.sound(SoundEvents.f_11737_, 1.0f, 1.4f);
        Scene.sound(SoundEvents.f_276532_, 0.9f, 0.8f);
        this.startMove(3.0, 0.62, () -> {
            Minecraft mc = Minecraft.m_91087_();
            if (mc.m_91403_() != null) {
                mc.m_91403_().m_104955_((Packet)new ServerboundSetBeaconPacket(Optional.ofNullable(first), Optional.ofNullable(second)));
            }
            this.burstAt = Scene.clock();
            BeaconStation.burst(this.core, this.burstColor, this.levels());
            Scene.sound(SoundEvents.f_11736_, 1.0f, 1.1f);
            Scene.sound(SoundEvents.f_11930_, 0.6f, 0.8f);
            Scene.sound(SoundEvents.f_144243_, 1.0f, 0.6f);
        });
    }

    static void burst(Vec3 core, float[] c, int levels) {
        int i;
        for (i = 0; i < 170; ++i) {
            double a = Fx.RANDOM.m_188500_() * Math.PI * 2.0;
            double rise = -0.15 + Fx.RANDOM.m_188500_() * 0.75;
            Vec3 dir = new Vec3(Math.cos(a), rise, Math.sin(a)).m_82541_();
            double speed = 2.5 + Fx.RANDOM.m_188500_() * 4.5;
            float w = Fx.RANDOM.m_188501_() * 0.6f;
            Fx.spark(core.m_82549_(dir.m_82490_(0.3)), dir.m_82490_(speed), 0.55 + Fx.RANDOM.m_188500_() * 0.7, 0.012f + Fx.RANDOM.m_188501_() * 0.01f, c[0] + (1.0f - c[0]) * w, c[1] + (1.0f - c[1]) * w, c[2] + (1.0f - c[2]) * w, 1.2, 0.05);
        }
        for (i = 0; i < 70; ++i) {
            Vec3 from = core.m_82520_(Fx.RANDOM.m_188583_() * 0.12, 0.4, Fx.RANDOM.m_188583_() * 0.12);
            Fx.spark(from, new Vec3(Fx.RANDOM.m_188583_() * 0.15, 4.0 + Fx.RANDOM.m_188500_() * 9.0, Fx.RANDOM.m_188583_() * 0.15), 1.2 + Fx.RANDOM.m_188500_() * 0.8, 0.02f + Fx.RANDOM.m_188501_() * 0.02f, c[0], c[1], c[2], -2.0, 0.07);
        }
        for (i = 0; i < 40 + 20 * levels; ++i) {
            double a = Fx.RANDOM.m_188500_() * Math.PI * 2.0;
            double r = 0.8 + Fx.RANDOM.m_188500_() * (1.5 + (double)levels);
            Vec3 at = core.m_82520_(Math.cos(a) * r, -Fx.RANDOM.m_188500_() * (double)levels * 0.8 + 0.2, Math.sin(a) * r);
            Fx.spark(at, new Vec3(0.0, 0.15 + Fx.RANDOM.m_188500_() * 0.3, 0.0), 1.2 + Fx.RANDOM.m_188500_() * 1.4, 0.015f + Fx.RANDOM.m_188501_() * 0.02f, c[0], c[1], c[2], -0.1, 0.0);
        }
    }

    @Override
    protected void deny() {
        this.denyAt = Scene.clock();
        Scene.sound(SoundEvents.f_11738_, 0.3f, 1.6f);
    }

    static float[] color(@Nullable MobEffect effect) {
        int rgb = effect == null ? 16773808 : effect.m_19484_();
        float r = (float)(rgb >> 16 & 0xFF) / 255.0f;
        float g = (float)(rgb >> 8 & 0xFF) / 255.0f;
        float b = (float)(rgb & 0xFF) / 255.0f;
        float max = Math.max(0.1f, Math.max(r, Math.max(g, b)));
        return new float[]{r / max, g / max, b / max};
    }

    @Override
    public double cameraKick() {
        double d = Scene.clock() - this.burstAt;
        double t = this.moveTime();
        double build = t >= 0.0 && t < 0.62 ? 0.25 * Anim.smoother(t / 0.62) : 0.0;
        return Math.max(build, d >= 0.0 && d < 1.2 ? Math.exp(-d * 3.2) : 0.0);
    }

    @Override
    public double flash() {
        double d = Scene.clock() - this.burstAt;
        return d >= 0.0 && d < 0.6 ? Math.pow(1.0 - d / 0.6, 2.0) * 0.32 : 0.0;
    }

    @Override
    public int flashColor() {
        int r = Math.round(255.0f * (0.6f + 0.4f * this.burstColor[0]));
        int g = Math.round(255.0f * (0.6f + 0.4f * this.burstColor[1]));
        int b = Math.round(255.0f * (0.6f + 0.4f * this.burstColor[2]));
        return r << 16 | g << 8 | b;
    }

    @Override
    public void tick() {
        BeaconMenu menu = (BeaconMenu)this.menu();
        if (menu == null) {
            return;
        }
        if (!this.chosenRead) {
            this.chosenRead = true;
            this.primary = menu.m_39057_();
            this.secondary = menu.m_39058_();
        }
        if (this.hoverPower != this.lastHover && this.hoverPower >= 0) {
            Scene.sound(SoundEvents.f_144243_, 0.25f, 1.7f + (float)this.hoverPower * 0.06f);
        }
        this.lastHover = this.hoverPower;
        if (!this.payment().m_41619_() && Minecraft.m_91087_().f_91074_.f_19797_ % 4 == 0) {
            float[] fArray;
            if (this.ready()) {
                fArray = BeaconStation.color(this.primary);
            } else {
                float[] fArray2 = new float[3];
                fArray2[0] = 1.0f;
                fArray2[1] = 0.9f;
                fArray = fArray2;
                fArray2[2] = 0.6f;
            }
            float[] c = fArray;
            Fx.motes(this.paymentPos(), 0.06, 1, c[0], c[1], c[2], 0.1);
        }
    }

    @Override
    void frame() {
        super.frame();
        double t = this.moveTime();
        double target = t < 0.0 ? 0.0 : Anim.smoother(Anim.window(t, 0.57, 1.07)) * (1.0 - Anim.smoother(Anim.window(t, 1.8, 2.65)));
        this.pull = Anim.approach(this.pull, target, 5.0, Scene.frameDt);
        if (t >= 0.0 && t < 0.62 && Fx.RANDOM.m_188500_() < Scene.frameDt * 40.0) {
            Vec3 dir = new Vec3(Fx.RANDOM.m_188583_(), Fx.RANDOM.m_188583_() * 0.6, Fx.RANDOM.m_188583_()).m_82541_();
            double life = 0.35 + Fx.RANDOM.m_188500_() * 0.2;
            double r = 0.7 + Fx.RANDOM.m_188500_() * 0.4;
            Fx.spark(this.core.m_82549_(dir.m_82490_(r)), dir.m_82490_(-r / life * 1.5), life, 0.012f + Fx.RANDOM.m_188501_() * 0.01f, this.burstColor[0], this.burstColor[1], this.burstColor[2], 0.0, 0.04);
        }
    }

    @Override
    public void renderScene(PoseStack pose, MultiBufferSource buffers, Camera camera, int light) {
        double presence;
        Vec3 cam = camera.m_90583_();
        double clock = Scene.clock();
        double t = this.moveTime();
        if (this.outro()) {
            this.drawLeaving(pose, buffers, camera, light);
        } else {
            ItemStack item;
            ItemStack itemStack = item = t >= 0.0 && t < 0.62 ? this.lightingWith : this.payment();
            if (!item.m_41619_()) {
                float[] fArray;
                double sink = t >= 0.0 ? Anim.inCubic(Anim.window(t, 0.05, 0.62)) : 0.0;
                Vec3 at = Anim.lerp(this.paymentPos(), this.core.m_82549_(this.side().m_82490_(0.1)).m_82520_(0.0, 0.1, 0.0), sink);
                float spin = (float)(clock * 50.0 + (t >= 0.0 ? 1500.0 * sink * sink : 0.0));
                float g = (float)(0.22 + 0.5 * sink);
                if (this.ready() || t >= 0.0) {
                    fArray = this.burstColor(t);
                } else {
                    float[] fArray2 = new float[3];
                    fArray2[0] = 1.0f;
                    fArray2[1] = 0.85f;
                    fArray = fArray2;
                    fArray2[2] = 0.5f;
                }
                float[] c = fArray;
                EnchantFx.billboard(pose, buffers, camera, at, 0.14 + 0.1 * sink, clock * 0.5, EnchantFx.GLOW, c[0] * g, c[1] * g, c[2] * g);
                EnchantStation.drawFloating(pose, buffers, cam, item, at, spin + (float)this.geometry().rotation(0), (float)(0.26 * (double)this.geometry().scale(0) * (1.0 - 0.7 * sink)));
            }
        }
        double d = Scene.phase == Scene.Phase.INTRO ? Anim.smoother(Anim.window(Scene.introProgress(), 0.4, 1.0)) : (presence = this.outro() ? 1.0 - Anim.smoother(Scene.time / 0.5) : 1.0);
        if (presence > 0.01 && this.payment().m_41619_() && t < 0.0) {
            float p = (float)((0.22 + 0.06 * Math.sin(clock * 2.0)) * presence * (Hands.carried().m_204117_(ItemTags.f_13164_) ? 1.8 : 1.0));
            EnchantFx.disc(pose, buffers, cam, this.top.m_82549_(this.side().m_82490_(0.27)).m_82520_(0.0, 0.004, 0.0), 0.17, clock * 0.4, RING, p, p * 0.85f, p * 0.55f);
        }
        this.drawLight(pose, buffers, camera);
    }

    private float[] burstColor(double t) {
        return t >= 0.0 ? this.burstColor : BeaconStation.color(this.primary);
    }

    private void drawLight(PoseStack pose, MultiBufferSource buffers, Camera camera) {
        double d;
        Vec3 cam = camera.m_90583_();
        double clock = Scene.clock();
        double t = this.moveTime();
        float[] c = this.burstColor;
        if (t >= 0.0 && t < 0.62) {
            float g = (float)(0.6 * Anim.smoother(t / 0.62));
            EnchantFx.billboard(pose, buffers, camera, this.core, 0.5 + 0.3 * (double)g, clock, EnchantFx.GLOW, c[0] * g, c[1] * g, c[2] * g);
        }
        if ((d = clock - this.burstAt) < 0.0 || d > 2.4) {
            return;
        }
        BeaconStation.drawBurst(pose, buffers, camera, this.core, d, c, this.levels());
    }

    static void drawBurst(PoseStack pose, MultiBufferSource buffers, Camera camera, Vec3 core, double d, float[] c, int levels) {
        float flare;
        Vec3 cam = camera.m_90583_();
        float glass = (float)Math.exp(-d * 2.5);
        EnchantFx.billboard(pose, buffers, camera, core, 0.9 * (double)glass + 0.3, d, EnchantFx.GLOW, c[0] * glass, c[1] * glass, c[2] * glass);
        int steps = Math.max(1, levels);
        for (int k = 0; k <= steps; ++k) {
            double dk = d - (double)k * 0.09;
            if (dk < 0.0 || dk > 1.2) continue;
            double radius = 0.4 + (double)k + 4.5 * Anim.outCubic(dk / 1.2);
            float fade = (float)((1.0 - Anim.smoother(dk / 1.2)) * (k == 0 ? 1.0 : 0.7));
            Vec3 at = new Vec3(core.f_82479_, core.f_82480_ - 0.5 - (double)k + 0.02, core.f_82481_);
            EnchantFx.disc(pose, buffers, cam, at, radius, d * 0.3, RING, c[0] * fade, c[1] * fade, c[2] * fade);
        }
        double rays = Anim.outCubic(Anim.window(d, 0.0, 0.55));
        float rayFade = (float)(1.0 - Anim.smoother(Anim.window(d, 0.2, 1.3)));
        if ((double)rayFade > 0.01) {
            VertexConsumer vc = buffers.m_6299_(RenderType.m_110488_((ResourceLocation)RAY));
            for (int i = 0; i < 24; ++i) {
                double a = (double)i * Math.PI / 12.0 + 0.13 * Math.sin((double)i * 3.7);
                double length = (1.4 + 1.2 * (double)(i * 7 % 5) / 4.0) * (0.3 + 2.6 * rays);
                double width = 0.12 + 0.08 * (double)(i * 3 % 4) / 3.0;
                Vec3 dir = new Vec3(Math.cos(a), 0.1 + 0.08 * (double)(i * 5 % 3), Math.sin(a)).m_82541_();
                float w = 0.45f;
                BeaconStation.ray(pose, vc, cam, core.m_82549_(dir.m_82490_(0.3)), dir, length, width, (c[0] + (1.0f - c[0]) * w) * rayFade, (c[1] + (1.0f - c[1]) * w) * rayFade, (c[2] + (1.0f - c[2]) * w) * rayFade);
            }
        }
        if ((double)(flare = (float)(Anim.smoother(Anim.window(d, 0.0, 0.12)) * (1.0 - Anim.smoother(Anim.window(d, 0.4, 2.4))))) > 0.01) {
            EnchantFx.beam(pose, buffers, cam, core.m_82520_(0.0, 0.3, 0.0), 48.0, 0.5 + 0.9 * (double)flare, d * 2.0, c[0] * flare, c[1] * flare, c[2] * flare);
            EnchantFx.beam(pose, buffers, cam, core.m_82520_(0.0, 0.3, 0.0), 48.0, 0.22, -d * 3.0, flare, flare, flare);
        }
    }

    private static void ray(PoseStack pose, VertexConsumer vc, Vec3 cam, Vec3 from, Vec3 dir, double length, double width, float r, float g, float b) {
        Vec3 across = dir.m_82537_(from.m_82546_(cam)).m_82541_().m_82490_(width / 2.0);
        Vec3 to = from.m_82549_(dir.m_82490_(length));
        Vec3[] c = new Vec3[]{from.m_82546_(across), to.m_82546_(across), to.m_82549_(across), from.m_82549_(across)};
        float[][] uv = new float[][]{{0.0f, 0.0f}, {1.0f, 0.0f}, {1.0f, 1.0f}, {0.0f, 1.0f}};
        int ri = BeaconStation.channel(r);
        int gi = BeaconStation.channel(g);
        int bi = BeaconStation.channel(b);
        int[][] nArrayArray = new int[][]{{0, 1, 2, 3}, {3, 2, 1, 0}};
        int n = nArrayArray.length;
        for (int i = 0; i < n; ++i) {
            int[] order;
            for (int i2 : order = nArrayArray[i]) {
                Vec3 p = c[i2].m_82546_(cam);
                vc.m_252986_(pose.m_85850_().m_252922_(), (float)p.f_82479_, (float)p.f_82480_, (float)p.f_82481_).m_6122_(ri, gi, bi, 255).m_7421_(uv[i2][0], uv[i2][1]).m_86008_(OverlayTexture.f_118083_).m_85969_(0xF000F0).m_252939_(pose.m_85850_().m_252943_(), 0.0f, 1.0f, 0.0f).m_5752_();
            }
        }
    }

    private static int channel(float v) {
        return Math.max(0, Math.min(255, Math.round(v * 255.0f)));
    }

    @Override
    public void renderWorld(PoseStack pose, MultiBufferSource buffers, Vec3 camera, int light) {
        this.gridEditor.renderWorld(pose, buffers, camera, light);
        BeaconMenu menu = (BeaconMenu)this.menu();
        if (menu == null) {
            return;
        }
        Camera cam = Minecraft.m_91087_().f_91063_.m_109153_();
        this.icons.clear();
        double clock = Scene.clock();
        double t = this.moveTime();
        double appear = Anim.outBack(Anim.window(Scene.time, 0.0, 0.5), 1.4);
        List<Power> list = this.powers();
        for (int i = 0; i < list.size(); ++i) {
            double fade;
            Power power = list.get(i);
            boolean chosen = this.chosen(power);
            double fly = t >= 0.0 && chosen ? Anim.inOutCubic(Anim.window(t, 0.1, 0.62)) : 0.0;
            double d = fade = t >= 0.0 && !chosen ? 1.0 - 0.7 * Anim.smoother(Anim.window(t, 0.0, 0.4)) * (1.0 - Anim.smoother(Anim.window(t, 1.8, 3.0))) : 1.0;
            if (fly >= 0.999) continue;
            Vec3 at = Anim.lerp(this.powerPos(i), this.top.m_82520_(0.0, 0.2, 0.0), fly);
            this.drawPower(pose, buffers, cam, power, at, (float)(appear * (1.0 - 0.6 * fly) * fade), i == this.hoverPower, chosen, this.active(power), this.open(power));
        }
        if (this.target == Target.TOP && this.aim != null) {
            boolean ok = Hands.carried().m_204117_(ItemTags.f_13164_);
            float p = (float)(0.25 + 0.08 * Math.sin(clock * 5.0));
            EnchantFx.disc(pose, buffers, camera, this.top.m_82549_(this.side().m_82490_(0.27)).m_82520_(0.0, 0.006, 0.0), 0.2, -clock, RING, p, ok ? p * 0.85f : p * 0.2f, ok ? p * 0.5f : p * 0.2f);
        }
    }

    private void drawPower(PoseStack pose, MultiBufferSource buffers, Camera camera, Power power, Vec3 at, float appear, boolean hovered, boolean chosen, boolean active, boolean open) {
        if (appear <= 0.01f) {
            return;
        }
        double clock = Scene.clock();
        int layoutSlot = this.powers().indexOf(power) + 1;
        float[] c = BeaconStation.color(power.effect());
        float k = open ? (chosen ? 1.0f : (hovered ? 0.85f : (active ? 0.65f : 0.45f))) : 0.14f;
        k *= appear;
        double size = 0.1 * (double)this.geometry().scale(layoutSlot) * (double)appear * (hovered && open ? 1.15 : 1.0) * (chosen ? 1.1 : 1.0);
        Fx.shade(pose, buffers, camera, at, size * 1.4, 0.75f * appear);
        if (chosen) {
            Vec3 to = this.beamAt(at);
            Vec3 along = to.m_82546_(at);
            BeaconStation.ray(pose, buffers.m_6299_(RenderType.m_110488_((ResourceLocation)RAY)), camera.m_90583_(), at, along.m_82541_(), along.m_82553_(), 0.05, c[0] * 0.6f * appear, c[1] * 0.6f * appear, c[2] * 0.6f * appear);
        }
        EnchantFx.billboard(pose, buffers, camera, at, size * 2.0, 0.0, EnchantFx.GLOW, c[0] * k * 0.35f, c[1] * k * 0.35f, c[2] * k * 0.35f);
        double turn = clock * (chosen ? 1.2 : 0.35) * (double)(power.tier() % 2 == 0 ? 1 : -1) + Math.toRadians(this.geometry().rotation(layoutSlot));
        EnchantFx.billboard(pose, buffers, camera, at, size * 1.3, turn, chosen ? EnchantFx.CIRCLE : RING, c[0] * k, c[1] * k, c[2] * k);
        String label = !open || power.upgrade() ? (power.upgrade() ? "II" : "I".repeat(power.tier() + 1).replace("IIII", "IV")) : "";
        this.icons.add(new Icon(Minecraft.m_91087_().m_91306_().m_118732_(power.effect()), at, size * 0.7, open ? 1.0f : 0.35f, appear, label, !open ? 0x9A9AA6 : 16773824, at.m_82520_(0.0, -size * 1.62, 0.0), (float)this.geometry().rotation(layoutSlot)));
        if (chosen && Fx.RANDOM.m_188500_() < Scene.frameDt * 6.0) {
            double a = Fx.RANDOM.m_188500_() * Math.PI * 2.0;
            Fx.spark(at.m_82520_(Math.cos(a) * size, Math.sin(a) * size, 0.0), new Vec3(0.0, 0.05, 0.0), 0.6, 0.008f, c[0], c[1], c[2], -0.05, 0.0);
        }
    }

    @Override
    public void renderScreen(GuiGraphics g, Font font, float alpha) {
        if (Scene.phase != Scene.Phase.ACTIVE) {
            return;
        }
        Vector3f up = Minecraft.m_91087_().f_91063_.m_109153_().m_253028_();
        Vec3 upward = new Vec3((double)up.x(), (double)up.y(), (double)up.z());
        for (Icon icon : this.icons) {
            double[] at = Picking.toGui(icon.at());
            double[] edge = Picking.toGui(icon.at().m_82549_(upward.m_82490_(icon.half())));
            if (at == null || edge == null) continue;
            double perBlock = Math.hypot(edge[0] - at[0], edge[1] - at[1]) / icon.half();
            float a = Math.min(1.0f, icon.alpha()) * alpha;
            g.m_280168_().m_85836_();
            g.m_280168_().m_85837_(at[0], at[1], 0.0);
            g.m_280168_().m_252781_(Axis.f_252403_.m_252977_(-icon.rotation()));
            float s = (float)(icon.half() * perBlock / 9.0);
            g.m_280168_().m_85841_(s, s, 1.0f);
            g.m_280565_(-9, -9, 0, 18, 18, icon.sprite(), icon.brightness(), icon.brightness(), icon.brightness(), a);
            g.m_280168_().m_85849_();
            double[] label = icon.label().isEmpty() ? null : Picking.toGui(icon.labelAt());
            if (label == null) continue;
            g.m_280168_().m_85836_();
            g.m_280168_().m_85837_(label[0], label[1], 0.0);
            float t = (float)(0.0055 * (double)icon.alpha() * perBlock);
            g.m_280168_().m_85841_(t, t, 1.0f);
            int textAlpha = Math.max(8, Math.round(220.0f * a));
            g.m_280056_(font, icon.label(), -font.m_92895_(icon.label()) / 2, -4, textAlpha << 24 | icon.labelColor(), false);
            g.m_280168_().m_85849_();
        }
        this.gridEditor.render(g, font, alpha);
    }

    static void drawContents(PoseStack pose, MultiBufferSource buffers, Camera camera, BlockPos pos, Direction side, List<ItemStack> items) {
        if (items.isEmpty() || items.get(0).m_41619_()) {
            return;
        }
        Vec3 at = RemainingLayouts.point(StationKind.BEACON, Vec3.m_82539_((Vec3i)pos).m_82520_(0.0, 1.0, 0.0), side, 0).m_82520_(0.0, 0.012 * Math.sin(Scene.clock() * 1.9), 0.0);
        EnchantFx.billboard(pose, buffers, camera, at, 0.14, Scene.clock() * 0.5, EnchantFx.GLOW, 0.22f, 0.19f, 0.11f);
        EnchantStation.drawFloating(pose, buffers, camera.m_90583_(), items.get(0), at, (float)(Scene.clock() * 50.0), 0.26f * RemainingLayouts.scale(StationKind.BEACON, 0));
    }

    static void onlookerFx(PoseStack pose, MultiBufferSource buffers, Camera camera, BlockPos pos, double age, double dt) {
        Vec3 core = Vec3.m_82512_((Vec3i)pos);
        float[] c = new float[]{1.0f, 0.92f, 0.62f};
        double d = age;
        if (d >= 0.0 && d < dt * 1.5) {
            BeaconStation.burst(core, c, 2);
        }
        if (d >= 0.0 && d < 2.4) {
            BeaconStation.drawBurst(pose, buffers, camera, core, d, c, 2);
        }
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
            case POWER: {
                List<Power> list = this.powers();
                if (this.hoverPower < 0 || this.hoverPower >= list.size()) {
                    return null;
                }
                Power power = list.get(this.hoverPower);
                ArrayList<Component> lines = new ArrayList<Component>();
                MutableComponent name = Component.m_237115_((String)power.effect().m_19481_());
                lines.add((Component)(power.upgrade() ? name.m_6881_().m_130946_(" II") : name));
                lines.add((Component)Component.m_237115_((String)(power.secondary() ? "block.minecraft.beacon.secondary" : "block.minecraft.beacon.primary")).m_130940_(ChatFormatting.DARK_GRAY));
                if (this.levels() == 0) {
                    lines.add((Component)Component.m_237115_((String)"cinematic.beacon.no_pyramid").m_130940_(ChatFormatting.RED));
                } else if (!this.open(power)) {
                    lines.add((Component)Component.m_237110_((String)"cinematic.beacon.needs", (Object[])new Object[]{power.tier() + 1}).m_130940_(ChatFormatting.RED));
                } else if (this.chosen(power)) {
                    lines.add((Component)Component.m_237115_((String)"cinematic.beacon.chosen").m_130940_(ChatFormatting.GOLD));
                } else {
                    lines.add((Component)Component.m_237115_((String)"cinematic.beacon.choose").m_130940_(ChatFormatting.GRAY));
                }
                if (this.active(power)) {
                    lines.add((Component)Component.m_237115_((String)"cinematic.beacon.active").m_130940_(ChatFormatting.AQUA));
                }
                return lines;
            }
            case PAYMENT: {
                ArrayList<Component> lines = new ArrayList<Component>(Screen.m_280152_((Minecraft)mc, (ItemStack)this.payment()));
                if (this.ready()) {
                    lines.add((Component)Component.m_237115_((String)"cinematic.beacon.light").m_130940_(ChatFormatting.GOLD));
                } else {
                    lines.add((Component)Component.m_237115_((String)(this.levels() == 0 ? "cinematic.beacon.no_pyramid" : "cinematic.beacon.pick_first")).m_130940_(ChatFormatting.YELLOW));
                }
                lines.add((Component)Component.m_237115_((String)"cinematic.beacon.take").m_130940_(ChatFormatting.GRAY));
                return lines;
            }
            case TOP: {
                return carried.m_204117_(ItemTags.f_13164_) ? List.of(Component.m_237115_((String)"cinematic.beacon.pay").m_130940_(ChatFormatting.GRAY)) : List.of(Component.m_237115_((String)"cinematic.beacon.refuse").m_130940_(ChatFormatting.RED));
            }
        }
        return null;
    }

    @Override
    public Component hint() {
        return Component.m_237115_((String)"cinematic.hint.beacon");
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
    public List<Vec3> workSamples() {
        return RemainingLayouts.samples(this, super.workSamples());
    }

    public static enum Target {
        NONE,
        POWER,
        PAYMENT,
        TOP;

    }

    private record Power(MobEffect effect, int tier, boolean secondary, boolean upgrade) {
    }

    private record Icon(TextureAtlasSprite sprite, Vec3 at, double half, float brightness, float alpha, String label, int labelColor, Vec3 labelAt, float rotation) {
    }
}


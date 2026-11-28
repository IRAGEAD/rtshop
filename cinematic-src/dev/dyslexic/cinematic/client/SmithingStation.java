/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  javax.annotation.Nullable
 *  net.minecraft.ChatFormatting
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.RegistryAccess
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.TextColor
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.world.Container
 *  net.minecraft.world.SimpleContainer
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.ClickType
 *  net.minecraft.world.inventory.SmithingMenu
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.armortrim.ArmorTrim
 *  net.minecraft.world.item.armortrim.TrimMaterial
 *  net.minecraft.world.item.crafting.RecipeType
 *  net.minecraft.world.item.crafting.SmithingRecipe
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Quaternionf
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.client.Anim;
import dev.dyslexic.cinematic.client.CameraRig;
import dev.dyslexic.cinematic.client.EnchantFx;
import dev.dyslexic.cinematic.client.Fx;
import dev.dyslexic.cinematic.client.GrindstoneStation;
import dev.dyslexic.cinematic.client.Hands;
import dev.dyslexic.cinematic.client.ItemPlacer;
import dev.dyslexic.cinematic.client.Picking;
import dev.dyslexic.cinematic.client.RecipeChoiceStrip;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.SurfaceAppearance;
import dev.dyslexic.cinematic.client.SurfaceEditor;
import dev.dyslexic.cinematic.client.SurfaceLayouts;
import dev.dyslexic.cinematic.client.SurfaceStation;
import dev.dyslexic.cinematic.station.RecipeChoices;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public final class SmithingStation
extends SurfaceStation<SmithingMenu> {
    static final double HAND_Y = 0.062;
    static final double ITEM_REACH = 0.075;
    static final double LEAN_BACK = 0.7;
    static final double LEAN_UP = 0.62;
    static final double[][] LAYOUT = new double[][]{{-0.26, -0.12}, {0.0, 0.06}, {0.27, -0.12}};
    static final float[] SIZES = new float[]{0.22f, 0.34f, 0.2f};
    public static final double SMITH_LENGTH = 2.6;
    static final double TEMPLATE_DOWN = 0.38;
    static final double MATERIAL_UP = 0.5;
    static final double MATERIAL_DOWN = 0.76;
    static final double PRESS = 0.95;
    static final double PEAK = 1.35;
    static final double COOLED = 2.15;
    static final double ON_GEAR = 0.025;
    static final double ON_TEMPLATE = 0.041;
    private final RecipeChoiceStrip choiceStrip = new RecipeChoiceStrip();
    public final Vec3 top;
    public Target target = Target.NONE;
    public int hoverSlot = -1;
    private int placeSlot = -1;
    private final ItemStack[] before = new ItemStack[]{ItemStack.f_41583_, ItemStack.f_41583_, ItemStack.f_41583_};
    private ItemStack after = ItemStack.f_41583_;
    private boolean toInventory;
    private float[] molten = new float[]{1.0f, 0.55f, 0.2f};
    private boolean templateLaid;
    private boolean materialLaid;
    private boolean pressed;
    private boolean peaked;
    private boolean hissed;
    private static final String[] SLOT_KEYS = new String[]{"template", "base", "addition"};

    @Override
    RecipeChoiceStrip choices() {
        return this.choiceStrip;
    }

    @Override
    public void tick() {
        super.tick();
        Minecraft mc = Minecraft.m_91087_();
        SmithingMenu menu = (SmithingMenu)this.menu();
        if (!RecipeChoices.available() || Scene.clientOnly() || menu == null || mc.f_91073_ == null) {
            this.choiceStrip.clear();
            return;
        }
        SimpleContainer input = new SimpleContainer(new ItemStack[]{menu.m_38853_(0).m_7993_(), menu.m_38853_(1).m_7993_(), menu.m_38853_(2).m_7993_()});
        List competing = RecipeChoices.matching((Level)mc.f_91073_, RecipeType.f_44113_, input);
        if (competing.size() < 2) {
            this.choiceStrip.clear();
            return;
        }
        ArrayList<ItemStack> outputs = new ArrayList<ItemStack>();
        int inUse = -1;
        ItemStack result = menu.m_38853_(3).m_7993_();
        for (int i = 0; i < competing.size(); ++i) {
            ItemStack output = ((SmithingRecipe)competing.get(i)).m_5874_((Container)input, mc.f_91073_.m_9598_());
            outputs.add(output);
            if (inUse >= 0 || result.m_41619_() || !ItemStack.m_150942_((ItemStack)output, (ItemStack)result)) continue;
            inUse = i;
        }
        this.choiceStrip.set(outputs, inUse);
    }

    public SmithingStation(BlockPos pos, Direction side) {
        super(pos, side, SmithingMenu.class, 3);
        this.top = new Vec3((double)pos.m_123341_() + 0.5, (double)pos.m_123342_() + 1.0, (double)pos.m_123343_() + 0.5);
    }

    @Override
    public StationKind kind() {
        return StationKind.SMITHING;
    }

    @Override
    public Vec3 center() {
        return this.top;
    }

    @Override
    public SoundEvent touchSound() {
        return SoundEvents.f_12068_;
    }

    static Vec3 slotPoint(Vec3 top, Direction side, int slot) {
        return SurfaceLayouts.current(StationKind.SMITHING).point(StationKind.SMITHING, top, side, slot);
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
        Vec3 at = this.top.m_82549_(this.side().m_82490_(0.7)).m_82520_(0.0, 0.62, 0.0);
        float pitch = (float)Math.toDegrees(Math.atan2(0.62, 0.74));
        return new CameraRig.Pose(at, this.side.m_122424_().m_122435_(), pitch, 0.0f);
    }

    @Override
    public Vec3 rest(boolean right) {
        return this.top.m_82549_(this.side().m_82490_(0.4)).m_82549_(this.right().m_82490_(right ? 0.25 : -0.25)).m_82520_(0.0, 0.062, 0.0);
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
            for (int i = 0; i < 3; ++i) {
                if (this.slot(i).m_41619_() || !GrindstoneStation.under(mouse, this.restPoint(i), (double)SIZES[i] * 0.42)) continue;
                this.target = Target.ITEM;
                this.hoverSlot = i;
                this.aim = this.restPoint(i);
                return;
            }
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
        int nearest = 0;
        for (int i = 1; i < 3; ++i) {
            if (!(hit.m_82557_(this.restPoint(i)) < hit.m_82557_(this.restPoint(nearest)))) continue;
            nearest = i;
        }
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
                Scene.sound(SoundEvents.f_11745_, 0.5f, 1.0f + (float)this.placeSlot * 0.15f);
                break;
            }
            case ITEM: {
                if (button == 0 && !this.slot(3).m_41619_()) {
                    this.smith(shift);
                    break;
                }
                this.slotClick(this.hoverSlot, 0, shift);
                break;
            }
        }
    }

    private void smith(boolean shift) {
        for (int i = 0; i < 3; ++i) {
            this.before[i] = this.slot(i).m_255036_(1);
        }
        this.after = this.slot(3).m_41777_();
        this.toInventory = shift;
        this.molten = SmithingStation.moltenColor(this.after);
        this.hissed = false;
        this.peaked = false;
        this.pressed = false;
        this.materialLaid = false;
        this.templateLaid = false;
        this.startMove(2.6, 1.35, () -> {
            SmithingMenu menu = (SmithingMenu)this.menu();
            Minecraft mc = Minecraft.m_91087_();
            if (this.slot(3).m_41619_()) {
                this.cancelMove();
                return;
            }
            mc.f_91072_.m_171799_(menu.f_38840_, 3, 0, this.toInventory ? ClickType.QUICK_MOVE : ClickType.PICKUP, (Player)mc.f_91074_);
        });
        Scene.sound(SoundEvents.f_11679_, 0.5f, 1.0f);
    }

    static float[] moltenColor(ItemStack result) {
        TextColor color;
        ArmorTrim trim;
        ClientLevel level = Minecraft.m_91087_().f_91073_;
        ArmorTrim armorTrim = trim = level == null ? null : (ArmorTrim)ArmorTrim.m_266285_((RegistryAccess)level.m_9598_(), (ItemStack)result).orElse(null);
        if (trim != null && (color = ((TrimMaterial)trim.m_266210_().m_203334_()).f_266021_().m_7383_().m_131135_()) != null) {
            int rgb = color.m_131265_();
            float r = (float)(rgb >> 16 & 0xFF) / 255.0f;
            float g = (float)(rgb >> 8 & 0xFF) / 255.0f;
            float b = (float)(rgb & 0xFF) / 255.0f;
            float max = Math.max(0.05f, Math.max(r, Math.max(g, b)));
            return new float[]{r / max, g / max, b / max};
        }
        return new float[]{1.0f, 0.5f, 0.16f};
    }

    private static double fuse(double t) {
        return Anim.smoother(Anim.window(t, 1.0, 1.32));
    }

    private double templateHeight() {
        return 0.025 + (double)((SIZES[1] * (this.surfaceScale(1) - 1.0f) + SIZES[0] * (this.surfaceScale(0) - 1.0f)) / 32.0f);
    }

    private double materialHeight() {
        return 0.041 + (double)(SIZES[1] * (this.surfaceScale(1) - 1.0f) / 32.0f) + (double)(SIZES[0] * (this.surfaceScale(0) - 1.0f) / 16.0f) + (double)(SIZES[2] * (this.surfaceScale(2) - 1.0f) / 32.0f);
    }

    private Vec3 movingAxis(double t, int slot, double from, double to, boolean across) {
        double k = Anim.inOutCubic(Anim.window(t, from, to));
        double angle = (double)this.surfaceRotation(slot) + (double)(this.surfaceRotation(1) - this.surfaceRotation(slot)) * k;
        return (across ? this.right() : this.side()).m_82524_((float)Math.toRadians(angle));
    }

    private Vec3 templatePos(double t) {
        Vec3 from = this.restPoint(0).m_82520_(0.0, 0.004, 0.0);
        Vec3 over = this.restPoint(1).m_82520_(0.0, 0.14, 0.0);
        double height = this.templateHeight();
        Vec3 on = this.restPoint(1).m_82520_(0.0, height, 0.0);
        Vec3 at = Anim.lerp(from, from.m_82520_(0.0, 0.1, 0.0), Anim.inOutCubic(Anim.window(t, 0.06, 0.24)));
        at = Anim.lerp(at, over, Anim.inOutCubic(Anim.window(t, 0.14, 0.3)));
        at = Anim.lerp(at, on, Anim.inCubic(Anim.window(t, 0.28, 0.38)));
        return at.m_82520_(0.0, -(height - 0.006) * SmithingStation.fuse(t) - 0.004 * Anim.hump(Anim.window(t, 0.95, 1.0699999999999998)), 0.0);
    }

    private Vec3 materialPos(double t) {
        Vec3 from = this.restPoint(2).m_82520_(0.0, 0.004, 0.0);
        double height = this.materialHeight();
        Vec3 over = this.restPoint(1).m_82520_(0.0, 0.17, 0.0).m_82549_(this.surfaceRight(1).m_82490_(0.012));
        Vec3 on = this.restPoint(1).m_82520_(0.0, height, 0.0).m_82549_(this.surfaceRight(1).m_82490_(0.012));
        Vec3 at = Anim.lerp(from, from.m_82520_(0.0, 0.1, 0.0), Anim.inOutCubic(Anim.window(t, 0.46, 0.58)));
        at = Anim.lerp(at, over, Anim.inOutCubic(Anim.window(t, 0.52, 0.7)));
        at = Anim.lerp(at, on, Anim.inCubic(Anim.window(t, 0.67, 0.76)));
        return at.m_82520_(0.0, -(height - 0.008) * SmithingStation.fuse(t) - 0.006 * Anim.hump(Anim.window(t, 0.95, 1.0699999999999998)), 0.0);
    }

    private Quaternionf turn(double t, float laidYaw, float stackYaw, double from, double to, int slot) {
        double k = Anim.inOutCubic(Anim.window(t, from, to));
        return ItemPlacer.lying(ItemPlacer.awayYaw(this.side) + (float)((double)(laidYaw += this.surfaceRotation(slot)) + (double)((stackYaw += this.surfaceRotation(1)) - laidYaw) * k), 0.0f);
    }

    private double heat(double t) {
        return Anim.smoother(Anim.window(t, 0.76, 1.35)) * (1.0 - Anim.smoother(Anim.window(t, 1.35, 2.15)));
    }

    private Vec3 resultPos(double t, Camera camera) {
        Vec3 at = this.restPoint(1);
        if (this.toInventory) {
            Vec3 drawer = camera.m_90583_().m_82520_(0.0, 0.55, 0.0).m_82549_(Scene.yawForward(camera.m_90590_()).m_82490_(0.6));
            return Anim.lerp(at, drawer, Anim.inCubic(Anim.window(t, 2.1999999999999997, 2.6)));
        }
        Vec3 held = this.heldItemPoint(Hands.position(true), Hands.shoulder(true, camera.m_90583_(), camera.m_90590_()));
        return Anim.lerp(at, held, Anim.inOutCubic(Anim.window(t, 2.27, 2.6)));
    }

    @Override
    @Nullable
    public Vec3[] handsOverride() {
        double t = this.moveTime();
        if (t < 0.0) {
            return null;
        }
        Vec3 r = this.rest(true);
        Vec3 l = this.rest(false);
        double carry = Anim.inOutCubic(Anim.window(t, 0.0, 0.12)) * (1.0 - Anim.inOutCubic(Anim.window(t, 0.38, 0.48)));
        Vec3 template = this.templatePos(Math.max(t, 0.06));
        Vec3 across = this.movingAxis(t, 0, 0.14, 0.38, true);
        Vec3 out = this.movingAxis(t, 0, 0.14, 0.38, false);
        r = Anim.lerp(r, template.m_82549_(across.m_82490_(0.09 * (double)this.surfaceScale(0))).m_82549_(out.m_82490_(0.04)).m_82520_(0.0, 0.062, 0.0), carry);
        l = Anim.lerp(l, template.m_82549_(across.m_82490_(-0.09 * (double)this.surfaceScale(0))).m_82549_(out.m_82490_(0.04)).m_82520_(0.0, 0.062, 0.0), carry);
        double fetch = Anim.inOutCubic(Anim.window(t, 0.4, 0.47)) * (1.0 - Anim.inOutCubic(Anim.window(t, 0.76, 0.84)));
        r = Anim.lerp(r, this.materialPos(t).m_82549_(this.movingAxis(t, 2, 0.5, 0.76, false).m_82490_(0.075)).m_82520_(0.0, 0.062, 0.0), fetch);
        double steady = Anim.inOutCubic(Anim.window(t, 0.38, 0.5)) * (1.0 - Anim.inOutCubic(Anim.window(t, 0.76, 0.8999999999999999)));
        l = Anim.lerp(l, this.restPoint(1).m_82549_(this.surfaceRight(1).m_82490_(-0.14 * (double)this.surfaceScale(1))).m_82549_(this.surfaceOut(1).m_82490_(0.05)).m_82520_(0.0, 0.074, 0.0), steady);
        double press = Anim.inOutCubic(Anim.window(t, 0.78, 0.9199999999999999)) * (1.0 - Anim.inOutCubic(Anim.window(t, 1.1099999999999999, 1.29)));
        Vec3 stack = this.restPoint(1).m_82549_(this.surfaceOut(1).m_82490_(0.03)).m_82520_(0.0, 0.112 - 0.03 * Anim.hump(Anim.window(t, 0.8799999999999999, 1.0999999999999999)), 0.0);
        r = Anim.lerp(r, stack.m_82549_(this.surfaceRight(1).m_82490_(0.07 * (double)this.surfaceScale(1))), press);
        l = Anim.lerp(l, stack.m_82549_(this.surfaceRight(1).m_82490_(-0.07 * (double)this.surfaceScale(1))), press);
        double away = Anim.hump(Anim.window(t, 1.2, 2.05));
        r = r.m_82549_(this.side().m_82490_(0.06 * away)).m_82520_(0.0, 0.04 * away, 0.0);
        l = l.m_82549_(this.side().m_82490_(0.06 * away)).m_82520_(0.0, 0.04 * away, 0.0);
        if (t > 2.05) {
            Vec3 grab = this.restPoint(1).m_82520_(0.0, 0.082, 0.0).m_82549_(this.surfaceOut(1).m_82490_(0.075));
            double take = Anim.inOutCubic(Anim.window(t, 2.05, 2.27));
            Vec3 end = this.rest(true).m_82520_(0.0, this.toInventory ? 0.0 : 0.05, 0.0);
            r = Anim.lerp(Anim.lerp(r, grab, take), end, Anim.inOutCubic(Anim.window(t, 2.27, 2.6)));
        }
        return new Vec3[]{r, l};
    }

    @Override
    public double cameraKick() {
        double t = this.moveTime();
        if (t < 0.0) {
            return 0.0;
        }
        return 0.35 * Math.exp(-Math.max(0.0, t - 0.95) * 14.0) * (double)(t >= 0.95 ? 1 : 0) + 0.3 * Math.exp(-Math.max(0.0, t - 1.35) * 9.0) * (double)(t >= 1.35 ? 1 : 0);
    }

    @Override
    public double flash() {
        double t = this.moveTime();
        return t < 1.35 ? 0.0 : 0.2 * Math.exp(-(t - 1.35) * 7.0);
    }

    @Override
    public int flashColor() {
        return 16771272;
    }

    @Override
    void frame() {
        super.frame();
        double t = this.moveTime();
        if (t < 0.0) {
            return;
        }
        double dt = Scene.frameDt;
        Vec3 gear = this.restPoint(1);
        if (!this.templateLaid && t >= 0.38) {
            this.templateLaid = true;
            Scene.sound(SoundEvents.f_11745_, 0.5f, 1.1f);
        }
        if (!this.materialLaid && t >= 0.76) {
            this.materialLaid = true;
            Scene.sound(SoundEvents.f_11745_, 0.5f, 1.45f);
        }
        if (!this.pressed && t >= 0.95) {
            this.pressed = true;
            Scene.sound(SoundEvents.f_12471_, 0.6f, 0.9f);
            Fx.motes(gear.m_82520_(0.0, 0.05, 0.0), 0.08, 10, 0.6f, 0.85f, 1.0f, 0.1);
        }
        if (t >= 0.95 && t < 1.35) {
            int drops = (int)Math.round(dt * 40.0 + Fx.RANDOM.m_188500_());
            for (int i = 0; i < drops; ++i) {
                double a = Fx.RANDOM.m_188500_() * Math.PI * 2.0;
                Vec3 edge = gear.m_82520_(Math.cos(a) * 0.1, 0.04, Math.sin(a) * 0.1);
                Vec3 v = new Vec3(Math.cos(a) * 0.25, 0.1, Math.sin(a) * 0.25);
                float k = 0.9f + 0.1f * Fx.RANDOM.m_188501_();
                Fx.spark(edge, v, 0.18 + Fx.RANDOM.m_188500_() * 0.08, 0.009f, this.molten[0] * k, this.molten[1] * k, this.molten[2] * k, 2.4, 0.04);
            }
            if (Fx.RANDOM.m_188500_() < dt * 24.0) {
                Fx.motes(gear.m_82520_(0.0, 0.05, 0.0), 0.08, 1, 1.0f, 0.55f, 0.2f, 0.25);
            }
        } else if (t >= 0.76 && t < 0.95 && Fx.RANDOM.m_188500_() < dt * 14.0) {
            Fx.motes(gear.m_82520_(0.0, 0.05, 0.0), 0.07, 1, this.molten[0], this.molten[1] * 0.7f, this.molten[2] * 0.4f, 0.15);
        }
        if (!this.peaked && t >= 1.35) {
            this.peaked = true;
            Fx.motes(gear.m_82520_(0.0, 0.04, 0.0), 0.1, 18, this.molten[0], this.molten[1], this.molten[2], 0.25);
            Fx.blowSparks(gear.m_82520_(0.0, 0.03, 0.0), 10, 1.4);
        }
        if (!this.hissed && t >= 1.4300000000000002) {
            this.hissed = true;
            Scene.sound(SoundEvents.f_12031_, 0.4f, 1.1f);
        }
        if (t >= 1.4000000000000001 && t < 1.95) {
            double rate = 70.0 * (1.0 - Anim.window(t, 1.35, 1.95));
            Fx.steam(gear.m_82520_(0.0, 0.03, 0.0), 0.07, (int)Math.round(dt * rate + Fx.RANDOM.m_188500_()));
        }
    }

    @Override
    public void renderScene(PoseStack pose, MultiBufferSource buffers, Camera camera, int light) {
        float size;
        Vec3 cam = camera.m_90583_();
        if (this.outro()) {
            this.drawLeaving(pose, buffers, camera, light);
            return;
        }
        double t = this.moveTime();
        if (t < 0.0) {
            for (int i = 0; i < 3; ++i) {
                SmithingStation.drawLaid(pose, buffers, cam, this.slot(i), this.restPoint(i), this.side, i, (i == this.hoverSlot ? 1.06f : 1.0f) * this.surfaceScale(i), light, this.surfaceRotation(i));
            }
            return;
        }
        for (int i = 0; i < 3; ++i) {
            int left;
            ItemStack rest = this.slot(i);
            int n = left = t < 1.35 ? rest.m_41613_() - 1 : rest.m_41613_();
            if (i == 1 || left <= 0) continue;
            SmithingStation.drawLaid(pose, buffers, cam, rest.m_255036_(left), this.restPoint(i), this.side, i, this.surfaceScale(i), light, this.surfaceRotation(i));
        }
        double heat = this.heat(t);
        ItemStack gear = t < 1.35 ? this.before[1] : this.after;
        Vec3 at = t < 1.35 ? this.restPoint(1).m_82520_(0.0, 0.004, 0.0) : this.resultPos(t, camera).m_82520_(0.0, 0.004, 0.0);
        float f = size = t < 2.15 || this.toInventory ? SIZES[1] * this.surfaceScale(1) : (float)((double)(SIZES[1] * this.surfaceScale(1)) + (0.205 - (double)(SIZES[1] * this.surfaceScale(1))) * Anim.window(t, 2.25, 2.6));
        if (this.toInventory) {
            size *= (float)(1.0 - 0.85 * Anim.inCubic(Anim.window(t, 2.1999999999999997, 2.6)));
        }
        float tilt = (float)(25.0 * Anim.window(t, 2.15, 2.6));
        ItemPlacer.drawOriented(pose, buffers, cam, gear, at, ItemPlacer.lying(ItemPlacer.awayYaw(this.side) + this.surfaceRotation(1), tilt), size, light);
        double left = 1.0 - SmithingStation.fuse(t);
        if (left > 0.02 && t < 1.35) {
            ItemPlacer.drawOriented(pose, buffers, cam, this.before[0], this.templatePos(t), this.turn(t, -12.0f, 0.0f, 0.14, 0.38, 0), (float)((double)(SIZES[0] * this.surfaceScale(0)) * (0.55 + 0.45 * left)), light);
            ItemPlacer.drawOriented(pose, buffers, cam, this.before[2], this.materialPos(t), this.turn(t, 18.0f, 8.0f, 0.5, 0.76, 2), (float)((double)(SIZES[2] * this.surfaceScale(2)) * (0.5 + 0.5 * left)), light);
            float glow = (float)(Anim.smoother(Anim.window(t, 0.66, 0.95)) * left);
            if ((double)glow > 0.01) {
                float k = glow * (float)(0.85 + 0.15 * Math.sin(Scene.clock() * 13.0));
                EnchantFx.billboard(pose, buffers, camera, this.materialPos(t).m_82520_(0.0, 0.02, 0.0), 0.12, Scene.clock(), EnchantFx.GLOW, this.molten[0] * k, this.molten[1] * 0.75f * k, this.molten[2] * 0.55f * k);
            }
        }
        if (heat > 0.01) {
            float h = (float)heat;
            float white = (float)Anim.smoother(Anim.window(t, 0.95, 1.35));
            float flicker = (float)(0.92 + 0.08 * Math.sin(Scene.clock() * 17.0));
            float r = h * flicker;
            float g = (0.3f + 0.6f * white) * h * flicker;
            float b = (0.08f + 0.6f * white * white) * h * flicker;
            EnchantFx.billboard(pose, buffers, camera, this.restPoint(1).m_82520_(0.0, 0.05, 0.0), 0.17 + 0.08 * (double)h, Scene.clock() * 0.3, EnchantFx.GLOW, r, g, b);
            EnchantFx.disc(pose, buffers, cam, this.restPoint(1).m_82520_(0.0, 0.003, 0.0), 0.2, 0.0, EnchantFx.GLOW, r * 0.7f, g * 0.7f, b * 0.7f);
        }
        if (t >= 0.9299999999999999 && t < 1.35) {
            float k = (float)Math.exp(-(t - 0.95) * 7.0) * 0.6f;
            EnchantFx.disc(pose, buffers, cam, this.restPoint(1).m_82520_(0.0, 0.004, 0.0), 0.2, 0.0, EnchantFx.GLOW, 0.6f * k, 0.85f * k, k);
        }
    }

    static void drawLaid(PoseStack pose, MultiBufferSource buffers, Vec3 cam, ItemStack stack, Vec3 at, Direction side, int slot, float scale, int light) {
        SmithingStation.drawLaid(pose, buffers, cam, stack, at, side, slot, scale, light, 0.0f);
    }

    static void drawLaid(PoseStack pose, MultiBufferSource buffers, Vec3 cam, ItemStack stack, Vec3 at, Direction side, int slot, float scale, int light, float rotation) {
        if (stack.m_41619_()) {
            return;
        }
        boolean block = ItemPlacer.isBlock(stack);
        float size = (block ? 0.13f : SIZES[slot]) * scale;
        float yaw = ItemPlacer.awayYaw(side) + rotation + (slot == 0 ? -12.0f : (slot == 2 ? 18.0f : 0.0f));
        ItemPlacer.drawOriented(pose, buffers, cam, stack, at.m_82520_(0.0, block ? (double)(size / 2.0f) : 0.004, 0.0), ItemPlacer.lying(yaw, 0.0f), size, light);
        if (stack.m_41613_() > 1 && !block) {
            ItemPlacer.drawOriented(pose, buffers, cam, stack, at.m_82520_(0.02, 0.011, -0.015), ItemPlacer.lying(yaw + 25.0f, 0.0f), size, light);
        }
    }

    @Override
    public void renderWorld(PoseStack pose, MultiBufferSource buffers, Vec3 camera, int light) {
        this.gridEditor.renderWorld(pose, buffers, camera, light);
        if (this.gridEditor.visible()) {
            return;
        }
        if (this.target != Target.PLACE) {
            return;
        }
        double clock = Scene.clock();
        for (int i = 0; i < 3; ++i) {
            boolean dest;
            boolean bl = dest = i == this.placeSlot;
            if (!dest && !this.slot(i).m_41619_()) continue;
            float p = dest ? (float)(0.2 + 0.07 * Math.sin(clock * 5.0)) : 0.05f;
            EnchantFx.disc(pose, buffers, camera, this.restPoint(i).m_82520_(0.0, 0.003, 0.0), (double)SIZES[i] * 0.55 * (double)this.surfaceScale(i), 0.0, EnchantFx.GLOW, p, p * 0.8f, p * 0.6f);
        }
        if (this.placeSlot < 0 && this.aim != null) {
            float p = (float)(0.2 + 0.06 * Math.sin(clock * 5.0));
            EnchantFx.disc(pose, buffers, camera, this.aim.m_82520_(0.0, 0.004, 0.0), 0.12, 0.0, EnchantFx.GLOW, p, p * 0.2f, p * 0.2f);
        }
    }

    static void drawContents(PoseStack pose, MultiBufferSource buffers, Vec3 cam, BlockPos pos, Direction side, List<ItemStack> items, int light) {
        Vec3 top = new Vec3((double)pos.m_123341_() + 0.5, (double)pos.m_123342_() + 1.0, (double)pos.m_123343_() + 0.5);
        for (int i = 0; i < Math.min(3, items.size()); ++i) {
            SmithingStation.drawLaid(pose, buffers, cam, items.get(i), SmithingStation.slotPoint(top, side, i), side, i, (float)SurfaceLayouts.current(StationKind.SMITHING).region(i).itemScale(), light, (float)SurfaceLayouts.current(StationKind.SMITHING).region(i).rotation());
        }
    }

    static void onlookerFx(BlockPos pos, Direction side, double age, double dt) {
        Vec3 gear = SmithingStation.slotPoint(new Vec3((double)pos.m_123341_() + 0.5, (double)pos.m_123342_() + 1.0, (double)pos.m_123343_() + 0.5), side, 1);
        if (age >= 0.0 && age < dt * 1.5) {
            Fx.motes(gear.m_82520_(0.0, 0.04, 0.0), 0.1, 14, 1.0f, 0.6f, 0.25f, 0.25);
        }
        if (age < 0.8) {
            Fx.steam(gear.m_82520_(0.0, 0.03, 0.0), 0.07, (int)Math.round(dt * 50.0 + Fx.RANDOM.m_188500_()));
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
            return this.placeSlot < 0 ? List.of(Component.m_237115_((String)"cinematic.smithing.refuse").m_130940_(ChatFormatting.RED)) : List.of(Component.m_237115_((String)("cinematic.smithing.slot." + SLOT_KEYS[this.placeSlot])).m_130940_(ChatFormatting.GRAY));
        }
        if (this.target != Target.ITEM) {
            return null;
        }
        ArrayList<Component> lines = new ArrayList<Component>(Screen.m_280152_((Minecraft)mc, (ItemStack)this.slot(this.hoverSlot)));
        lines.add((Component)Component.m_237119_());
        ItemStack result = this.slot(3);
        if (!result.m_41619_()) {
            lines.add((Component)Component.m_237110_((String)"cinematic.smithing.result", (Object[])new Object[]{result.m_41786_()}).m_130940_(ChatFormatting.GRAY));
            lines.add((Component)Component.m_237115_((String)"cinematic.smithing.smith").m_130940_(ChatFormatting.GOLD));
        } else {
            for (int i = 0; i < 3; ++i) {
                if (!this.slot(i).m_41619_()) continue;
                lines.add((Component)Component.m_237115_((String)("cinematic.smithing.missing." + SLOT_KEYS[i])).m_130940_(ChatFormatting.DARK_GRAY));
            }
            lines.add((Component)Component.m_237115_((String)"cinematic.smithing.take").m_130940_(ChatFormatting.GRAY));
        }
        return lines;
    }

    @Override
    public Component hint() {
        if (this.gridEditor.editing()) {
            return SurfaceEditor.text("hint", new Object[0]);
        }
        return Component.m_237115_((String)"cinematic.hint.smithing");
    }

    public static enum Target {
        NONE,
        PLACE,
        ITEM;

    }
}


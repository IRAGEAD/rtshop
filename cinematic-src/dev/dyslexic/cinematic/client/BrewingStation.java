/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  javax.annotation.Nullable
 *  net.minecraft.ChatFormatting
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Vec3i
 *  net.minecraft.network.chat.Component
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.world.inventory.BrewingStandMenu
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.PotionItem
 *  net.minecraft.world.item.alchemy.PotionUtils
 *  net.minecraft.world.phys.Vec3
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.client.CameraRig;
import dev.dyslexic.cinematic.client.EnchantFx;
import dev.dyslexic.cinematic.client.Fx;
import dev.dyslexic.cinematic.client.GrindstoneStation;
import dev.dyslexic.cinematic.client.Hands;
import dev.dyslexic.cinematic.client.ItemPlacer;
import dev.dyslexic.cinematic.client.MenuStation;
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
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.phys.Vec3;

public final class BrewingStation
extends MenuStation<BrewingStandMenu>
implements SpatialTarget {
    final SpatialEditor gridEditor = new SpatialEditor(this);
    static final double HAND_Y = 0.055;
    static final double ITEM_REACH = 0.07;
    static final double BOTTLE_TOUCH_HALF_HEIGHT = 0.12;
    static final double LOOSE_TOUCH_HALF_HEIGHT = 0.065;
    static final double[][] FEET = new double[][]{{12.0, 8.0}, {4.0, 4.0}, {4.0, 12.0}};
    static final double BELLY = 0.30874999426305294;
    static final int BOTTLES = 3;
    static final int INGREDIENT = 3;
    static final int FUEL = 4;
    static final int BREW_TIME = 400;
    public Target target = Target.NONE;
    public int hoverSlot = -1;
    private int placeSlot = -1;
    private final Vec3 base;
    private int lastTicks;
    private final double[] flaredAt = new double[]{-10.0, -10.0, -10.0};
    private static final String[] SLOT_KEYS = new String[]{"bottle", "bottle", "bottle", "ingredient", "fuel"};

    public BrewingStation(BlockPos pos, Direction side) {
        super(pos, side, BrewingStandMenu.class, 5);
        this.base = Vec3.m_82539_((Vec3i)pos);
    }

    @Override
    public SpatialEditor layoutEditor() {
        return this.gridEditor;
    }

    @Override
    public ItemStack layoutItem(int slot) {
        return this.slot(slot);
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
    public void beforeFrame() {
        this.gridEditor.frame();
    }

    @Override
    public void onEnd() {
        this.gridEditor.cancel();
        super.onEnd();
    }

    @Override
    public void renderScreen(GuiGraphics g, Font font, float alpha) {
        this.gridEditor.render(g, font, alpha);
    }

    @Override
    public SpatialAppearance.Frame layoutFrame() {
        return SpatialAppearance.Frame.of(this.kind(), this.pos, this.side, Direction.UP);
    }

    @Override
    public float layoutHeldScale() {
        int slot = this.placeSlot >= 0 ? this.placeSlot : this.hoverSlot;
        return slot < 0 ? 1.0f : this.geometry().scale(slot);
    }

    @Override
    public List<Vec3> workSamples() {
        return this.layoutSamples(super.workSamples());
    }

    @Override
    public StationKind kind() {
        return StationKind.BREWING;
    }

    @Override
    public Vec3 center() {
        return this.base.m_82520_(0.0, 0.45, 0.0);
    }

    @Override
    public SoundEvent touchSound() {
        return SoundEvents.f_12068_;
    }

    static Vec3 slotPoint(BlockPos pos, int slot) {
        return BrewingStation.slotPoint(pos, Direction.SOUTH, slot);
    }

    static Vec3 slotPoint(BlockPos pos, Direction side, int slot) {
        return SpatialLayouts.current(StationKind.BREWING).point(SpatialAppearance.Frame.of(StationKind.BREWING, pos, side, Direction.UP), slot);
    }

    static float itemScale(int slot) {
        return SpatialLayouts.current(StationKind.BREWING).scale(slot);
    }

    static float itemRotation(int slot) {
        return (float)(-SpatialLayouts.current(StationKind.BREWING).rotation(slot));
    }

    @Override
    protected Vec3 restPoint(int slot) {
        return BrewingStation.slotPoint(this.pos, this.side, slot).m_82549_(this.right().m_82490_(this.denyShake()));
    }

    Vec3 handle(int slot) {
        return this.restPoint(slot);
    }

    @Override
    public List<Vec3> approach(LocalPlayer player) {
        Vec3 stand = new Vec3(this.base.f_82479_ + (double)this.side.m_122429_() * 0.95, player.m_20186_(), this.base.f_82481_ + (double)this.side.m_122431_() * 0.95);
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
    public boolean worksAtAnAngle() {
        return true;
    }

    @Override
    public CameraRig.Pose lean() {
        Vec3 eye = this.base.m_82549_(this.side().m_82490_(1.2)).m_82520_(0.0, 0.88, 0.0);
        Vec3 look = this.base.m_82520_(0.0, 0.46, 0.0).m_82546_(eye);
        float yaw = (float)Math.toDegrees(Math.atan2(-look.f_82479_, look.f_82481_));
        float pitch = (float)(-Math.toDegrees(Math.atan2(look.f_82480_, Math.hypot(look.f_82479_, look.f_82481_))));
        return new CameraRig.Pose(eye, yaw, pitch, -2.0f);
    }

    @Override
    public float bodyPitch() {
        return 32.0f;
    }

    @Override
    public Vec3 rest(boolean right) {
        return this.base.m_82549_(this.side().m_82490_(0.55)).m_82549_(this.right().m_82490_(right ? 0.3 : -0.3)).m_82520_(0.0, 0.2, 0.0);
    }

    @Override
    public void updateCursor(boolean overPanel) {
        this.clearCursor();
        if (overPanel || this.menu() == null) {
            return;
        }
        double[] mouse = Picking.mouseGui();
        ItemStack carried = Hands.carried();
        int nearest = -1;
        double best = Double.MAX_VALUE;
        int editedHit = this.geometry().equals(SpatialAppearance.VANILLA) ? -1 : this.layoutPick();
        for (int i = 0; i < 5; ++i) {
            boolean candidate;
            boolean near;
            double[] at = Picking.toGui(this.handle(i));
            if (at == null) continue;
            double d = Math.hypot(mouse[0] - at[0], mouse[1] - at[1]);
            boolean bl = this.geometry().equals(SpatialAppearance.VANILLA) ? GrindstoneStation.under(mouse, this.handle(i), i < 3 ? 0.1 : 0.09) : (near = editedHit == i);
            boolean bl2 = carried.m_41619_() ? near && !this.slot(i).m_41619_() : (candidate = near || this.nearStand(mouse));
            if (!candidate || !(d < best)) continue;
            best = d;
            nearest = i;
        }
        if (nearest < 0) {
            return;
        }
        if (carried.m_41619_()) {
            this.target = Target.ITEM;
            this.hoverSlot = nearest;
            this.aim = this.handle(nearest);
            return;
        }
        this.target = Target.PLACE;
        this.placeSlot = this.slotFor(carried, nearest);
        this.aim = this.handle(this.placeSlot < 0 ? nearest : this.placeSlot);
    }

    private boolean nearStand(double[] mouse) {
        return GrindstoneStation.under(mouse, this.base.m_82520_(0.0, 0.4, 0.0), 0.5);
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
        double hover = carrying ? 0.07 : 0.04;
        return aim.m_82520_(0.0, 0.055 + hover - 0.05 * tap, 0.0).m_82546_(reach.m_82490_(0.07));
    }

    @Override
    public Vec3 heldItemPoint(Vec3 hand, Vec3 shoulder) {
        Vec3 reach = new Vec3(hand.f_82479_ - shoulder.f_82479_, 0.0, hand.f_82481_ - shoulder.f_82481_).m_82541_();
        return hand.m_82549_(reach.m_82490_(0.07)).m_82520_(0.0, -0.043, 0.0);
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
                Scene.sound(this.placeSlot < 3 ? SoundEvents.f_11770_ : SoundEvents.f_12334_, 0.4f, 1.3f);
                break;
            }
            case ITEM: {
                this.slotClick(this.hoverSlot, 0, shift);
                break;
            }
        }
    }

    private int ticks() {
        BrewingStandMenu menu = (BrewingStandMenu)this.menu();
        return menu == null ? 0 : menu.m_39103_();
    }

    private double progress() {
        int t = this.ticks();
        return t > 0 ? 1.0 - (double)t / 400.0 : -1.0;
    }

    static float[] color(ItemStack stack) {
        int rgb = stack.m_41720_() instanceof PotionItem ? PotionUtils.m_43575_((ItemStack)stack) : 4159204;
        float r = (float)(rgb >> 16 & 0xFF) / 255.0f;
        float g = (float)(rgb >> 8 & 0xFF) / 255.0f;
        float b = (float)(rgb & 0xFF) / 255.0f;
        float max = Math.max(0.1f, Math.max(r, Math.max(g, b)));
        return new float[]{r / max, g / max, b / max};
    }

    @Override
    public void tick() {
        int ticks = this.ticks();
        if (this.lastTicks > 0 && ticks == 0) {
            for (int i = 0; i < 3; ++i) {
                if (this.slot(i).m_41619_()) continue;
                this.flaredAt[i] = Scene.clock();
                float[] c = BrewingStation.color(this.slot(i));
                Fx.motes(this.handle(i), 0.06, 14, c[0], c[1], c[2], 0.3);
            }
        }
        this.lastTicks = ticks;
    }

    @Override
    void frame() {
        super.frame();
        double p = this.progress();
        double dt = Scene.frameDt;
        if (p < 0.0) {
            return;
        }
        Vec3 bowl = this.restPoint(3).m_82520_(0.0, 0.03, 0.0);
        for (int i = 0; i < 3; ++i) {
            ItemStack bottle = this.slot(i);
            if (bottle.m_41619_()) continue;
            float[] c = BrewingStation.color(bottle);
            if (Fx.RANDOM.m_188500_() < dt * 6.0) {
                Fx.spark(this.handle(i).m_82520_(Fx.RANDOM.m_188583_() * 0.015, -0.05, Fx.RANDOM.m_188583_() * 0.015), new Vec3(0.0, 0.08 + Fx.RANDOM.m_188500_() * 0.05, 0.0), 0.6, 0.006f, c[0] * 0.8f, c[1] * 0.8f, c[2] * 0.8f, -0.05, 0.0);
            }
            if (!(Fx.RANDOM.m_188500_() < dt * 3.5)) continue;
            double flight = 0.45;
            Vec3 to = this.handle(i).m_82520_(0.0, 0.1, 0.0);
            double gravity = 1.2;
            Vec3 v = to.m_82546_(bowl).m_82490_(1.0 / flight).m_82520_(0.0, 0.5 * gravity * flight, 0.0);
            Fx.spark(bowl, v, flight, 0.008f, 0.85f, 0.6f, 1.0f, gravity, 0.03);
        }
        if (Fx.RANDOM.m_188500_() < dt * 4.0) {
            Fx.steam(bowl.m_82520_(0.0, 0.02, 0.0), 0.03, 1);
        }
    }

    @Override
    public void renderScene(PoseStack pose, MultiBufferSource buffers, Camera camera, int light) {
        int fuel;
        if (this.outro()) {
            return;
        }
        double p = this.progress();
        ArrayList<ItemStack> items = new ArrayList<ItemStack>(5);
        for (int i = 0; i < 5; ++i) {
            items.add(this.slot(i));
        }
        BrewingStation.draw(pose, buffers, camera, this.pos, this.side, items, this.hoverSlot, p, light);
        double clock = Scene.clock();
        for (int i = 0; i < 3; ++i) {
            ItemStack bottle = this.slot(i);
            double flare = Math.exp(-Math.max(0.0, clock - this.flaredAt[i]) * 2.5);
            double glow = (p >= 0.0 ? 0.14 + 0.07 * Math.sin(clock * 4.0 + (double)i) : 0.05) + 0.5 * flare + (i == this.hoverSlot ? 0.1 : 0.0);
            if (bottle.m_41619_() || !(glow > 0.01)) continue;
            float[] c = BrewingStation.color(bottle);
            float g = (float)glow;
            EnchantFx.billboard(pose, buffers, camera, this.handle(i).m_82520_(0.0, -0.04, 0.0), 0.09 + 0.05 * flare, 0.0, EnchantFx.GLOW, c[0] * g, c[1] * g, c[2] * g);
        }
        BrewingStandMenu menu = (BrewingStandMenu)this.menu();
        int n = fuel = menu == null ? 0 : menu.m_39102_();
        if (fuel > 0) {
            float f = (float)(0.12 + 0.18 * (double)fuel / 20.0) * (float)(0.85 + 0.15 * Math.sin(clock * 9.0));
            EnchantFx.billboard(pose, buffers, camera, this.restPoint(4).m_82520_(0.0, 0.05, 0.0), 0.07, 0.0, EnchantFx.GLOW, f, f * 0.55f, f * 0.15f);
        }
    }

    static void draw(PoseStack pose, MultiBufferSource buffers, Camera camera, BlockPos pos, Direction side, List<ItemStack> items, int hover, double progress, int light) {
        Vec3 cam = camera.m_90583_();
        for (int i = 0; i < Math.min(5, items.size()); ++i) {
            float grow;
            ItemStack stack = items.get(i);
            if (stack.m_41619_()) continue;
            Vec3 at = BrewingStation.slotPoint(pos, side, i);
            float f = grow = i == hover ? 1.08f : 1.0f;
            if (i < 3) continue;
            if (i == 3) {
                float melt = progress >= 0.0 ? (float)(1.0 - 0.65 * progress) : 1.0f;
                boolean block = ItemPlacer.isBlock(stack);
                float size = (block ? 0.1f : 0.17f) * melt * grow * BrewingStation.itemScale(i);
                ItemPlacer.drawOriented(pose, buffers, cam, stack, at.m_82520_(0.0, block ? (double)(size / 2.0f) : 0.004, 0.0), ItemPlacer.lying(ItemPlacer.awayYaw(side) + 20.0f + BrewingStation.itemRotation(3), 0.0f), size, light);
                continue;
            }
            Vec3 toEye = new Vec3(cam.f_82479_ - at.f_82479_, 0.0, cam.f_82481_ - at.f_82481_);
            float yaw = (float)(180.0 - Math.toDegrees(Math.atan2(-toEye.f_82479_, toEye.f_82481_)));
            ItemPlacer.drawOriented(pose, buffers, cam, stack, at, ItemPlacer.lying(yaw + BrewingStation.itemRotation(4), 90.0f), 0.17f * grow * BrewingStation.itemScale(4), light);
        }
    }

    @Override
    public void renderWorld(PoseStack pose, MultiBufferSource buffers, Vec3 camera, int light) {
        this.gridEditor.renderWorld(pose, buffers, camera, light);
        if (this.target == Target.PLACE && this.aim != null) {
            float p = (float)(0.2 + 0.07 * Math.sin(Scene.clock() * 5.0));
            boolean ok = this.placeSlot >= 0;
            Minecraft mc = Minecraft.m_91087_();
            EnchantFx.billboard(pose, buffers, mc.f_91063_.m_109153_(), this.aim, 0.07, 0.0, EnchantFx.GLOW, p, ok ? p * 0.8f : p * 0.2f, ok ? p * 0.6f : p * 0.2f);
        }
    }

    static void drawContents(PoseStack pose, MultiBufferSource buffers, Camera camera, BlockPos pos, Direction side, List<ItemStack> items, int light) {
        BrewingStation.draw(pose, buffers, camera, pos, side, items, -1, -1.0, light);
    }

    static void drawLoose(PoseStack pose, MultiBufferSource buffers, Vec3 cam, BlockPos pos, Direction side, ItemStack ingredient, ItemStack fuel, int light) {
        Vec3 at;
        if (!ingredient.m_41619_()) {
            at = BrewingStation.slotPoint(pos, side, 3);
            boolean block = ItemPlacer.isBlock(ingredient);
            float size = (block ? 0.1f : 0.17f) * BrewingStation.itemScale(3);
            ItemPlacer.drawOriented(pose, buffers, cam, ingredient, at.m_82520_(0.0, block ? (double)(size / 2.0f) : 0.004, 0.0), ItemPlacer.lying(ItemPlacer.awayYaw(side) + 20.0f + BrewingStation.itemRotation(3), 0.0f), size, light);
        }
        if (!fuel.m_41619_()) {
            at = BrewingStation.slotPoint(pos, side, 4);
            Vec3 toEye = new Vec3(cam.f_82479_ - at.f_82479_, 0.0, cam.f_82481_ - at.f_82481_);
            float yaw = (float)(180.0 - Math.toDegrees(Math.atan2(-toEye.f_82479_, toEye.f_82481_)));
            ItemPlacer.drawOriented(pose, buffers, cam, fuel, at, ItemPlacer.lying(yaw + BrewingStation.itemRotation(4), 90.0f), 0.17f * BrewingStation.itemScale(4), light);
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
        if (this.target == Target.PLACE) {
            return this.placeSlot < 0 ? List.of(Component.m_237115_((String)"cinematic.brewing.refuse").m_130940_(ChatFormatting.RED)) : List.of(Component.m_237115_((String)("cinematic.brewing.slot." + SLOT_KEYS[this.placeSlot])).m_130940_(ChatFormatting.GRAY));
        }
        if (this.target != Target.ITEM) {
            return null;
        }
        ArrayList<Component> lines = new ArrayList<Component>(Screen.m_280152_((Minecraft)mc, (ItemStack)this.slot(this.hoverSlot)));
        double p = this.progress();
        if (p >= 0.0 && this.hoverSlot != 4) {
            lines.add((Component)Component.m_237110_((String)"cinematic.brewing.progress", (Object[])new Object[]{(int)Math.round(p * 100.0)}).m_130940_(ChatFormatting.LIGHT_PURPLE));
        }
        if (this.hoverSlot == 4) {
            BrewingStandMenu menu = (BrewingStandMenu)this.menu();
            lines.add((Component)Component.m_237110_((String)"cinematic.brewing.fuel", (Object[])new Object[]{menu == null ? 0 : menu.m_39102_()}).m_130940_(ChatFormatting.GOLD));
        }
        lines.add((Component)Component.m_237115_((String)"cinematic.brewing.take").m_130940_(ChatFormatting.GRAY));
        return lines;
    }

    @Override
    public Component hint() {
        return Component.m_237115_((String)"cinematic.hint.brewing");
    }

    public static enum Target {
        NONE,
        PLACE,
        ITEM;

    }
}


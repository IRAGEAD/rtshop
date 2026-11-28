/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  javax.annotation.Nullable
 *  net.minecraft.ChatFormatting
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.MultiBufferSource$BufferSource
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Vec3i
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.util.Mth
 *  net.minecraft.world.Container
 *  net.minecraft.world.SimpleContainer
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.inventory.AbstractFurnaceMenu
 *  net.minecraft.world.inventory.ClickType
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.crafting.AbstractCookingRecipe
 *  net.minecraft.world.item.crafting.Ingredient
 *  net.minecraft.world.item.crafting.Recipe
 *  net.minecraft.world.item.crafting.RecipeType
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.AbstractFurnaceBlock
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.common.ForgeHooks
 *  org.joml.Matrix4f
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dyslexic.cinematic.client.Anim;
import dev.dyslexic.cinematic.client.CameraRig;
import dev.dyslexic.cinematic.client.ClientFurnaces;
import dev.dyslexic.cinematic.client.ClientNet;
import dev.dyslexic.cinematic.client.ClientStation;
import dev.dyslexic.cinematic.client.FurnaceAppearance;
import dev.dyslexic.cinematic.client.FurnaceGridEditor;
import dev.dyslexic.cinematic.client.FurnaceLayouts;
import dev.dyslexic.cinematic.client.Fx;
import dev.dyslexic.cinematic.client.Hands;
import dev.dyslexic.cinematic.client.ItemPlacer;
import dev.dyslexic.cinematic.client.Picking;
import dev.dyslexic.cinematic.client.RecipeChoiceStrip;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.TableGridEditor;
import dev.dyslexic.cinematic.furnace.FurnaceLayout;
import dev.dyslexic.cinematic.furnace.FurnaceZones;
import dev.dyslexic.cinematic.net.FurnaceStatePayload;
import dev.dyslexic.cinematic.net.StationActionPayload;
import dev.dyslexic.cinematic.station.RecipeChoices;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.ArrayList;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import org.joml.Matrix4f;

public final class FurnaceStation
extends ClientStation {
    private static final Staging LOW = new Staging(true, 0.9, 0.5, 0.1, 0.44, -3.5f, 32.0f, FurnaceZones.Face.FRONT, 0.2, 0.09, 0.36);
    private static final Staging TALL = new Staging(false, 0.7, 1.15, 0.08, 0.97, -2.0f, 45.0f, FurnaceZones.Face.TOP, 0.15, 0.13, 0.06);
    static final double ITEM_REACH = 0.07;
    static final double HAND_Y = 0.062;
    private final StationKind kind;
    public final FurnaceLayout layout;
    final FurnaceGridEditor gridEditor;
    private final Staging staging;
    public int hoverZone = -1;
    private FurnaceZones.Face aimFace = FurnaceZones.Face.FRONT;
    private int litTime;
    private int litDuration;
    private int cookingProgress;
    private int cookingTotalTime;
    private ItemStack hint = ItemStack.f_41583_;
    private double hintAt;
    private static final double SUNK_REST_U = 0.18;
    private static final double SUNK_REST_V = 0.14;
    private static final double SUNK_REST_OUT = 0.06;
    private final RecipeChoiceStrip choiceStrip = new RecipeChoiceStrip();
    private String furnaceChoice = "";
    private static final int[][] BOTH_WAYS = new int[][]{{0, 1, 2, 3}, {3, 2, 1, 0}};
    private static final int[][] ONE_WAY = new int[][]{{0, 1, 2, 3}};

    public FurnaceStation(BlockPos pos, Direction facing, StationKind kind) {
        super(pos, facing);
        this.kind = kind;
        this.layout = FurnaceLayout.of(kind);
        this.staging = kind == StationKind.BLAST_FURNACE ? TALL : LOW;
        this.gridEditor = new FurnaceGridEditor(this);
    }

    @Override
    public StationKind kind() {
        return this.kind;
    }

    FurnaceAppearance geometry() {
        return FurnaceLayouts.current(this.kind);
    }

    @Override
    public boolean busy() {
        return this.gridEditor.editing();
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
        this.hint = ItemStack.f_41583_;
    }

    @Nullable
    ItemStack cookingInput(ResourceLocation id) {
        ItemStack[] choices;
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91073_ == null) {
            return null;
        }
        Recipe recipe = mc.f_91073_.m_7465_().m_44043_(id).orElse(null);
        if (recipe == null || recipe.m_6671_() != this.layout.recipeType || recipe.m_7527_().isEmpty()) {
            return null;
        }
        Ingredient ingredient = (Ingredient)recipe.m_7527_().get(0);
        if (mc.f_91074_ != null) {
            for (ItemStack stack : mc.f_91074_.m_150109_().f_35974_) {
                if (!ingredient.test(stack)) continue;
                return stack.m_255036_(1);
            }
        }
        return (choices = ingredient.m_43908_()).length == 0 ? null : choices[0].m_255036_(1);
    }

    ItemStack guideHint() {
        return this.hint;
    }

    void showHint(@Nullable ItemStack stack) {
        this.hint = stack == null ? ItemStack.f_41583_ : stack.m_255036_(1);
        this.hintAt = Scene.clock();
    }

    private void drawHint(PoseStack pose, MultiBufferSource buffers, Vec3 camera, int light) {
        MultiBufferSource.BufferSource source;
        ItemStack inside;
        if (this.hint.m_41619_() || Minecraft.m_91087_().f_91073_ == null || !this.stillThere((Level)Minecraft.m_91087_().f_91073_)) {
            return;
        }
        ClientFurnaces.Entry entry = ClientFurnaces.get(this.pos);
        ItemStack itemStack = inside = entry == null ? ItemStack.f_41583_ : entry.items[0];
        if (!inside.m_41619_()) {
            if (ItemStack.m_41656_((ItemStack)inside, (ItemStack)this.hint)) {
                this.hint = ItemStack.f_41583_;
            }
            return;
        }
        FurnaceLayout.Bed bed = null;
        for (FurnaceLayout.Bed b : this.layout.beds) {
            if (b.slot() != 0) continue;
            bed = b;
        }
        if (bed == null) {
            return;
        }
        Direction facing = (Direction)Minecraft.m_91087_().f_91073_.m_8055_(this.pos).m_61143_((Property)AbstractFurnaceBlock.f_48683_);
        FurnaceAppearance appearance = this.geometry();
        double u = (bed.u0() + bed.u1()) / 2.0;
        Vec3 base = appearance.point(this.layout, this.pos, facing, bed.slot(), u, bed.face() == FurnaceZones.Face.FRONT ? bed.floor() + 0.002 : bed.floor(), -bed.depth(), true);
        float appear = (float)Anim.smoother((Scene.clock() - this.hintAt) / 0.3);
        float opacity = appear * (0.3f + 0.07f * (float)Math.sin(Scene.clock() * 3.0));
        if (buffers instanceof MultiBufferSource.BufferSource) {
            source = (MultiBufferSource.BufferSource)buffers;
            source.m_109911_();
        }
        ItemPlacer.render(pose, buffers, camera, this.hint, base, facing, 7L, 0.95f * (float)appearance.region(bed.slot()).itemScale(), bed.tilt(), light, opacity);
        if (buffers instanceof MultiBufferSource.BufferSource) {
            source = (MultiBufferSource.BufferSource)buffers;
            source.m_109911_();
        }
    }

    @Override
    public List<Vec3> workSamples() {
        if (this.geometry().equals(FurnaceAppearance.VANILLA)) {
            return super.workSamples();
        }
        ArrayList<Vec3> points = new ArrayList<Vec3>(super.workSamples());
        for (int i = 0; i < 3; ++i) {
            Vec3 normal = this.geometry().face(this.layout, i) == FurnaceZones.Face.TOP ? new Vec3(0.0, 0.04, 0.0) : this.side().m_82490_(0.04);
            points.add(this.geometry().center(this.layout, this.pos, this.side, i).m_82549_(normal));
        }
        return points;
    }

    private String key() {
        return this.kind.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public Vec3 center() {
        return FurnaceZones.point(this.pos, this.side, 0.5, 0.5, 0.0);
    }

    @Override
    public BlockPos lightPos() {
        return this.pos.m_121945_(this.side);
    }

    @Override
    public SoundEvent touchSound() {
        return this.kind == StationKind.BLAST_FURNACE ? SoundEvents.f_12068_ : SoundEvents.f_12450_;
    }

    @Override
    public boolean stillThere(Level level) {
        return super.stillThere(level) && level.m_8055_(this.pos).m_61143_((Property)AbstractFurnaceBlock.f_48683_) == this.side;
    }

    @Override
    public List<Vec3> approach(LocalPlayer player) {
        Vec3 middle = new Vec3((double)this.pos.m_123341_() + 0.5, player.m_20186_(), (double)this.pos.m_123343_() + 0.5);
        Vec3 out = this.side();
        Vec3 across = this.right();
        Vec3 rel = player.m_20182_().m_82546_(middle);
        double ahead = rel.m_82526_(out);
        double aside = rel.m_82526_(across);
        ArrayList<Vec3> path = new ArrayList<Vec3>();
        if (ahead < 0.45) {
            double sign;
            double d = sign = aside >= 0.0 ? 1.0 : -1.0;
            if (ahead < -0.45) {
                path.add(middle.m_82549_(across.m_82490_(sign * 1.15)));
            }
            path.add(middle.m_82549_(across.m_82490_(sign * 1.1)).m_82549_(out.m_82490_(1.0)));
        }
        path.add(middle.m_82549_(out.m_82490_(0.92)));
        return path;
    }

    @Override
    public boolean crouches() {
        return this.staging.crouch();
    }

    @Override
    public boolean workOnTop() {
        return false;
    }

    @Override
    public float bodyPitch() {
        return this.staging.bodyPitch();
    }

    @Override
    public boolean worksAtAnAngle() {
        return true;
    }

    @Override
    public CameraRig.Pose lean() {
        Vec3 face = this.center();
        Vec3 eye = face.m_82549_(this.side().m_82490_(this.staging.out())).m_82520_(0.0, this.staging.up(), 0.0).m_82549_(this.right().m_82490_(this.staging.side()));
        Vec3 look = FurnaceZones.point(this.pos, this.side, 0.5, this.staging.lookV(), 0.0).m_82546_(eye);
        float yaw = (float)Math.toDegrees(Math.atan2(-look.f_82479_, look.f_82481_));
        float pitch = (float)(-Math.toDegrees(Math.atan2(look.f_82480_, Math.hypot(look.f_82479_, look.f_82481_))));
        return new CameraRig.Pose(eye, yaw, pitch, this.staging.roll());
    }

    @Override
    public Vec3 rest(boolean right) {
        if (this.staging.restFace() == FurnaceZones.Face.FRONT && this.sunk()) {
            Vec3 at = FurnaceZones.point(this.pos, this.side, FurnaceZones.Face.TOP, right ? 0.8200000000000001 : 0.18, 0.14, 0.06);
            return at.m_82520_(0.0, 0.0020000000000000018, 0.0);
        }
        double u = right ? 1.0 - this.staging.restU() : this.staging.restU();
        Vec3 at = FurnaceZones.point(this.pos, this.side, this.staging.restFace(), u, this.staging.restV(), this.staging.restOut());
        return this.staging.restFace() == FurnaceZones.Face.TOP ? at.m_82520_(0.0, 0.062 - this.staging.restOut(), 0.0) : at;
    }

    private boolean sunk() {
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        return player != null && player.m_20186_() >= (double)this.pos.m_123342_() + 0.8;
    }

    @Override
    public void updateCursor(boolean overPanel) {
        Vec3 topCenter;
        Vec3 above;
        this.hoverZone = -1;
        this.aim = null;
        double[] mouse = Picking.mouseGui();
        if (overPanel || this.gridEditor.over(mouse[0], mouse[1])) {
            return;
        }
        if (!this.geometry().equals(FurnaceAppearance.VANILLA)) {
            ZoneHit hit = this.zoneHit(false);
            if (hit != null) {
                this.hoverZone = hit.index();
                this.aimFace = this.geometry().face(this.layout, this.hoverZone);
                FurnaceAppearance.Region region = this.geometry().region(this.hoverZone);
                Vec3 vertical = this.aimFace == FurnaceZones.Face.FRONT ? new Vec3(0.0, 1.0, 0.0) : this.side().m_82490_(-1.0);
                this.aim = hit.world().m_82549_(this.right().m_82490_(region.itemU() / 16.0)).m_82549_(vertical.m_82490_(region.itemV() / 16.0));
            }
            return;
        }
        Vec3 camera = Picking.camera();
        double bestDistance = Double.MAX_VALUE;
        Vec3 face = this.center();
        Vec3 hit = Picking.cursorOnPlane(face, this.side());
        if (hit != null) {
            double u = hit.m_82546_(face).m_82526_(this.right()) + 0.5;
            double v = hit.f_82480_ - (double)this.pos.m_123342_();
            if (u > -0.08 && u < 1.08 && v > -0.08 && v < 1.08) {
                bestDistance = hit.m_82557_(camera);
                this.pick(FurnaceZones.Face.FRONT, u, v);
            }
        }
        if (this.layout.usesTop() && (above = Picking.cursorOnPlane(topCenter = FurnaceZones.top(this.pos, this.side, 0.5, 0.5, 0.0), new Vec3(0.0, 1.0, 0.0))) != null && above.m_82557_(camera) < bestDistance) {
            Vec3 rel = above.m_82546_(topCenter);
            double u = rel.m_82526_(this.right()) + 0.5;
            double v = 0.5 - rel.m_82526_(this.side());
            if (u > -0.05 && u < 1.05 && v > -0.05 && v < 1.05) {
                this.pick(FurnaceZones.Face.TOP, u, v);
            }
        }
    }

    private void pick(FurnaceZones.Face face, double u, double v) {
        this.aimFace = face;
        this.hoverZone = this.layout.zoneAt(face, u, v);
        this.aim = FurnaceZones.point(this.pos, this.side, face, Mth.m_14008_((double)u, (double)0.08, (double)0.92), Mth.m_14008_((double)v, (double)0.05, (double)0.95), 0.0);
    }

    ZoneHit zoneHit(boolean editor) {
        FurnaceAppearance appearance = this.geometry();
        ZoneHit best = null;
        double distance = Double.MAX_VALUE;
        for (int i = 0; i < (editor ? 5 : 3); ++i) {
            FurnaceZones.Face face = appearance.face(this.layout, i);
            Vec3 normal = face == FurnaceZones.Face.FRONT ? this.side() : new Vec3(0.0, 1.0, 0.0);
            double out = appearance.region(i).out() / 16.0;
            Vec3 middle = FurnaceZones.point(this.pos, this.side, face, 0.5, 0.5, out);
            Vec3 hit = Picking.cursorOnPlane(middle, normal);
            if (hit == null) continue;
            Vec3 rel = hit.m_82546_(middle);
            double u = rel.m_82526_(this.right()) + 0.5;
            double v = face == FurnaceZones.Face.FRONT ? hit.f_82480_ - (double)this.pos.m_123342_() : 0.5 - rel.m_82526_(this.side());
            double d = hit.m_82557_(Picking.camera());
            if (!(d < distance) || !FurnaceAppearance.contains(appearance.rect(this.layout, i, !editor), u, v)) continue;
            distance = d;
            best = new ZoneHit(i, hit);
        }
        return best;
    }

    @Override
    public void clearCursor() {
        super.clearCursor();
        this.hoverZone = -1;
    }

    @Override
    public Vec3 handTarget(Vec3 aim, Vec3 shoulder, boolean carrying, double tap) {
        if (this.aimFace == FurnaceZones.Face.TOP) {
            Vec3 reach = new Vec3(aim.f_82479_ - shoulder.f_82479_, 0.0, aim.f_82481_ - shoulder.f_82481_).m_82541_();
            double hover = carrying ? 0.075 : 0.045;
            return aim.m_82520_(0.0, 0.062 + hover - 0.07 * tap, 0.0).m_82546_(reach.m_82490_(0.07));
        }
        Vec3 reach = aim.m_82546_(shoulder).m_82541_();
        double hover = carrying ? 0.11 : 0.075;
        return aim.m_82549_(this.side().m_82490_(hover - 0.18 * tap)).m_82546_(reach.m_82490_(0.07)).m_82520_(0.0, 0.035, 0.0);
    }

    @Override
    public Vec3 heldItemPoint(Vec3 hand, Vec3 shoulder) {
        if (this.aimFace == FurnaceZones.Face.TOP) {
            Vec3 reach = new Vec3(hand.f_82479_ - shoulder.f_82479_, 0.0, hand.f_82481_ - shoulder.f_82481_).m_82541_();
            return hand.m_82549_(reach.m_82490_(0.07)).m_82520_(0.0, -0.05, 0.0);
        }
        Vec3 reach = hand.m_82546_(shoulder).m_82541_();
        return hand.m_82549_(reach.m_82490_(0.07)).m_82520_(0.0, -0.045, 0.0);
    }

    @Override
    public float heldItemTilt() {
        return 10.0f;
    }

    float heldItemScale() {
        return this.hoverZone < 0 ? 1.0f : (float)this.geometry().region(this.hoverZone).itemScale();
    }

    @Override
    public void click(int button, boolean shift) {
        if (this.hoverZone < 0) {
            return;
        }
        Hands.tap();
        if (Scene.clientOnly()) {
            AbstractContainerMenu abstractContainerMenu;
            Minecraft mc = Minecraft.m_91087_();
            if (mc.f_91074_ != null && (abstractContainerMenu = mc.f_91074_.f_36096_) instanceof AbstractFurnaceMenu) {
                AbstractFurnaceMenu menu = (AbstractFurnaceMenu)abstractContainerMenu;
                if (mc.f_91072_ != null) {
                    mc.f_91072_.m_171799_(menu.f_38840_, this.hoverZone, button, shift ? ClickType.QUICK_MOVE : ClickType.PICKUP, (Player)mc.f_91074_);
                }
            }
        } else {
            ClientNet.sendToServer(new StationActionPayload(this.pos, this.kind, StationActionPayload.Action.CLICK, this.hoverZone, button, shift, 0));
        }
    }

    private boolean accepts(int zone, ItemStack carried) {
        if (carried.m_41619_()) {
            return true;
        }
        return switch (zone) {
            case 1 -> {
                if (ForgeHooks.getBurnTime((ItemStack)carried, this.layout.recipeType) > 0) {
                    yield true;
                }
                yield false;
            }
            case 2 -> ItemStack.m_150942_((ItemStack)carried, (ItemStack)ClientFurnaces.item(this.pos, 2));
            default -> true;
        };
    }

    private boolean cookable(ItemStack stack) {
        Minecraft mc = Minecraft.m_91087_();
        RecipeType<?> type = this.layout.recipeType;
        return mc.f_91073_ != null && mc.f_91073_.m_7465_().m_44015_(type, (Container)new SimpleContainer(new ItemStack[]{stack}), (Level)mc.f_91073_).isPresent();
    }

    public void applyState(FurnaceStatePayload state) {
        this.litTime = state.litTime();
        this.litDuration = state.litDuration();
        this.cookingProgress = state.cookingProgress();
        this.cookingTotalTime = state.cookingTotalTime();
        this.furnaceChoice = state.choice();
    }

    @Override
    RecipeChoiceStrip choices() {
        return this.choiceStrip;
    }

    private void updateChoices() {
        Minecraft mc = Minecraft.m_91087_();
        ItemStack input = ClientFurnaces.item(this.pos, 0);
        if (!RecipeChoices.available() || Scene.clientOnly() || input.m_41619_() || mc.f_91073_ == null) {
            this.choiceStrip.clear();
            return;
        }
        SimpleContainer single = new SimpleContainer(new ItemStack[]{input});
        List<?> competing = RecipeChoices.matching((Level)mc.f_91073_, this.layout.recipeType, single);
        if (competing.size() < 2) {
            this.choiceStrip.clear();
            return;
        }
        ArrayList<ItemStack> outputs = new ArrayList<ItemStack>();
        int inUse = -1;
        for (int i = 0; i < competing.size(); ++i) {
            outputs.add(((AbstractCookingRecipe)competing.get(i)).m_5874_((Container)single, mc.f_91073_.m_9598_()));
            if (!((AbstractCookingRecipe)competing.get(i)).m_6423_().toString().equals(this.furnaceChoice)) continue;
            inUse = i;
        }
        this.choiceStrip.set(outputs, inUse);
    }

    @Override
    public void tick() {
        this.updateChoices();
        if (Scene.clientOnly()) {
            return;
        }
        if (this.litTime > 0) {
            --this.litTime;
            if (!ClientFurnaces.item(this.pos, 0).m_41619_() && this.cookingTotalTime > 0) {
                this.cookingProgress = Math.min(this.cookingTotalTime - 1, this.cookingProgress + 1);
            }
        }
    }

    public double burn() {
        AbstractContainerMenu abstractContainerMenu;
        if (Scene.clientOnly() && Minecraft.m_91087_().f_91074_ != null && (abstractContainerMenu = Minecraft.m_91087_().f_91074_.f_36096_) instanceof AbstractFurnaceMenu) {
            AbstractFurnaceMenu menu = (AbstractFurnaceMenu)abstractContainerMenu;
            return menu.m_38996_();
        }
        return this.litTime <= 0 || this.litDuration <= 0 ? 0.0 : Mth.m_14008_((double)((double)this.litTime / (double)this.litDuration), (double)0.0, (double)1.0);
    }

    public double cook() {
        AbstractContainerMenu abstractContainerMenu;
        if (Scene.clientOnly() && Minecraft.m_91087_().f_91074_ != null && (abstractContainerMenu = Minecraft.m_91087_().f_91074_.f_36096_) instanceof AbstractFurnaceMenu) {
            AbstractFurnaceMenu menu = (AbstractFurnaceMenu)abstractContainerMenu;
            return menu.m_38995_();
        }
        return this.cookingTotalTime <= 0 ? 0.0 : Mth.m_14008_((double)((double)this.cookingProgress / (double)this.cookingTotalTime), (double)0.0, (double)1.0);
    }

    @Override
    public void renderWorld(PoseStack pose, MultiBufferSource buffers, Vec3 camera, int light) {
        int[] nArray;
        FurnaceLayout.Zone zone;
        FurnaceAppearance appearance = this.geometry();
        VertexConsumer glow = buffers.m_6299_(Fx.flatGlow());
        double flicker = 0.9 + 0.1 * Math.sin(Scene.clock() * 9.0);
        this.gauge(pose, glow, camera, appearance.rect(this.layout, 3, false), Mth.m_14139_((double)this.gridEditor.amount(), (double)this.cook(), (double)0.7), new int[]{255, 196, 90}, flicker, appearance.region(3).out() / 16.0);
        this.gauge(pose, glow, camera, appearance.rect(this.layout, 4, false), Mth.m_14139_((double)this.gridEditor.amount(), (double)this.burn(), (double)0.5), new int[]{255, 92, 30}, flicker, appearance.region(4).out() / 16.0);
        this.gridEditor.renderWorld(pose, buffers, camera, light);
        if (this.gridEditor.visible()) {
            return;
        }
        this.drawHint(pose, buffers, camera, light);
        FurnaceLayout.Zone zone2 = zone = this.hoverZone < 0 ? null : this.layout.zone(this.hoverZone);
        if (zone == null) {
            return;
        }
        boolean ok = this.accepts(this.hoverZone, Hands.carried());
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
        FurnaceStation.quad(pose, buffers.m_6299_(Fx.HIGHLIGHT), camera, this.pos, this.side, zone.face(), appearance.rect(this.layout, this.hoverZone, false), 0.004 + appearance.region(this.hoverZone).out() / 16.0, tint, tint, 44, false);
    }

    private void gauge(PoseStack pose, VertexConsumer glow, Vec3 camera, double[] strip, double fill, int[] rgb, double flicker, double out) {
        int[] track = new int[]{(int)((double)rgb[0] * 0.12), (int)((double)rgb[1] * 0.12), (int)((double)rgb[2] * 0.12)};
        FurnaceStation.glowQuad(pose, glow, camera, this.pos, this.side, FurnaceZones.Face.FRONT, strip, out + 0.003, track, track);
        if (fill > 0.001) {
            int[] lit = new int[]{(int)((double)rgb[0] * 0.75 * flicker), (int)((double)rgb[1] * 0.75 * flicker), (int)((double)rgb[2] * 0.75 * flicker)};
            FurnaceStation.glowQuad(pose, glow, camera, this.pos, this.side, FurnaceZones.Face.FRONT, new double[]{strip[0], strip[0] + (strip[1] - strip[0]) * fill, strip[2], strip[3]}, out + 0.0035, lit, lit);
        }
    }

    static void glowQuad(PoseStack pose, VertexConsumer buffer, Vec3 camera, BlockPos pos, Direction facing, FurnaceZones.Face face, double[] rect, double out, int[] near, int[] far) {
        Matrix4f m = pose.m_85850_().m_252922_();
        Vec3[] corners = new Vec3[]{FurnaceZones.point(pos, facing, face, rect[0], rect[2], out), FurnaceZones.point(pos, facing, face, rect[1], rect[2], out), FurnaceZones.point(pos, facing, face, rect[1], rect[3], out), FurnaceZones.point(pos, facing, face, rect[0], rect[3], out)};
        Vec3 normal = face == FurnaceZones.Face.TOP ? new Vec3(0.0, 1.0, 0.0) : Vec3.m_82528_((Vec3i)facing.m_122436_());
        int[][] nArray = BOTH_WAYS;
        int n = nArray.length;
        for (int i = 0; i < n; ++i) {
            int[] order;
            for (int i2 : order = nArray[i]) {
                Vec3 c = corners[i2];
                int[] rgb = i2 < 2 ? near : far;
                Fx.glowVertex(buffer, m, (float)(c.f_82479_ - camera.f_82479_), (float)(c.f_82480_ - camera.f_82480_), (float)(c.f_82481_ - camera.f_82481_), rgb[0], rgb[1], rgb[2], normal);
            }
        }
    }

    static void quad(PoseStack pose, VertexConsumer buffer, Vec3 camera, BlockPos pos, Direction facing, FurnaceZones.Face face, double[] rect, double out, int[] near, int[] far, int alpha) {
        FurnaceStation.quad(pose, buffer, camera, pos, facing, face, rect, out, near, far, alpha, true);
    }

    static void quad(PoseStack pose, VertexConsumer buffer, Vec3 camera, BlockPos pos, Direction facing, FurnaceZones.Face face, double[] rect, double out, int[] near, int[] far, int alpha, boolean bothWays) {
        int[][] orders;
        Matrix4f m = pose.m_85850_().m_252922_();
        Vec3[] corners = new Vec3[]{FurnaceZones.point(pos, facing, face, rect[0], rect[2], out), FurnaceZones.point(pos, facing, face, rect[1], rect[2], out), FurnaceZones.point(pos, facing, face, rect[1], rect[3], out), FurnaceZones.point(pos, facing, face, rect[0], rect[3], out)};
        int[][] nArray = orders = bothWays ? BOTH_WAYS : ONE_WAY;
        int n = nArray.length;
        for (int i = 0; i < n; ++i) {
            int[] order;
            for (int i2 : order = nArray[i]) {
                Vec3 c = corners[i2];
                int[] rgb = i2 < 2 ? near : far;
                buffer.m_252986_(m, (float)(c.f_82479_ - camera.f_82479_), (float)(c.f_82480_ - camera.f_82480_), (float)(c.f_82481_ - camera.f_82481_)).m_6122_(rgb[0], rgb[1], rgb[2], alpha).m_5752_();
            }
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
        if (this.hoverZone < 0) {
            return null;
        }
        ArrayList<Component> lines = new ArrayList<Component>();
        lines.add((Component)Component.m_237115_((String)("cinematic." + this.key() + ".zone." + this.hoverZone)).m_130940_(ChatFormatting.GOLD));
        if (!carried.m_41619_()) {
            if (!this.accepts(this.hoverZone, carried)) {
                lines.add((Component)Component.m_237115_((String)(this.hoverZone == 1 ? "cinematic.furnace.not_fuel" : "cinematic.furnace.no_room")).m_130940_(ChatFormatting.RED));
            } else if (this.hoverZone == 0 && !this.cookable(carried)) {
                lines.add((Component)Component.m_237115_((String)("cinematic." + this.key() + ".not_cookable")).m_130940_(ChatFormatting.YELLOW));
            } else {
                return null;
            }
            return lines;
        }
        ItemStack inside = ClientFurnaces.item(this.pos, this.hoverZone);
        if (inside.m_41619_()) {
            lines.add((Component)Component.m_237115_((String)("cinematic." + this.key() + ".empty." + this.hoverZone)).m_130940_(ChatFormatting.GRAY));
        } else {
            lines.add((Component)inside.m_41786_().m_6881_().m_130946_(" \u00d7" + inside.m_41613_()));
        }
        if (this.hoverZone == 0 && this.cook() > 0.0) {
            lines.add((Component)Component.m_237110_((String)("cinematic." + this.key() + ".progress"), (Object[])new Object[]{Math.round(this.cook() * 100.0)}).m_130940_(ChatFormatting.GRAY));
        } else if (this.hoverZone == 1 && this.litTime > 0) {
            lines.add((Component)Component.m_237110_((String)"cinematic.furnace.burning", (Object[])new Object[]{(this.litTime + 19) / 20}).m_130940_(ChatFormatting.GRAY));
        } else if (this.hoverZone == 2 && !inside.m_41619_()) {
            lines.add((Component)Component.m_237115_((String)"cinematic.furnace.take").m_130940_(ChatFormatting.GRAY));
        }
        return lines;
    }

    @Override
    public Component hint() {
        if (this.gridEditor.editing()) {
            return TableGridEditor.text("hint", new Object[0]);
        }
        return Component.m_237115_((String)("cinematic.hint." + this.key()));
    }

    @Override
    public void renderScreen(GuiGraphics graphics, Font font, float alpha) {
        this.gridEditor.render(graphics, font, alpha);
    }

    private record Staging(boolean crouch, double out, double up, double side, double lookV, float roll, float bodyPitch, FurnaceZones.Face restFace, double restU, double restV, double restOut) {
    }

    record ZoneHit(int index, Vec3 world) {
    }
}


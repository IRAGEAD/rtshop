/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.datafixers.util.Pair
 *  javax.annotation.Nullable
 *  net.minecraft.ChatFormatting
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.model.geom.ModelLayers
 *  net.minecraft.client.model.geom.ModelPart
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.Sheets
 *  net.minecraft.client.renderer.blockentity.BannerRenderer
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.client.renderer.texture.TextureAtlasSprite
 *  net.minecraft.client.resources.model.Material
 *  net.minecraft.client.resources.model.ModelBakery
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Holder
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.ClickType
 *  net.minecraft.world.inventory.LoomMenu
 *  net.minecraft.world.item.BannerItem
 *  net.minecraft.world.item.DyeColor
 *  net.minecraft.world.item.DyeItem
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.block.entity.BannerBlockEntity
 *  net.minecraft.world.level.block.entity.BannerPattern
 *  net.minecraft.world.level.block.entity.BannerPatterns
 *  net.minecraft.world.phys.Vec3
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import dev.dyslexic.cinematic.client.Anim;
import dev.dyslexic.cinematic.client.CameraRig;
import dev.dyslexic.cinematic.client.EnchantFx;
import dev.dyslexic.cinematic.client.Fx;
import dev.dyslexic.cinematic.client.GrindstoneStation;
import dev.dyslexic.cinematic.client.Hands;
import dev.dyslexic.cinematic.client.ItemPlacer;
import dev.dyslexic.cinematic.client.MenuStation;
import dev.dyslexic.cinematic.client.Picking;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.Settings;
import dev.dyslexic.cinematic.client.SmithingStation;
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
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatterns;
import net.minecraft.world.phys.Vec3;

public final class LoomStation
extends MenuStation<LoomMenu>
implements SpatialTarget {
    final SpatialEditor gridEditor = new SpatialEditor(this);
    static final double HAND_Y = 0.062;
    static final double ITEM_REACH = 0.075;
    private static final double EDIT_HAND_DROP = 0.24;
    private static final double EDIT_HAND_OUT = 0.12;
    static final double CLOTH_WIDTH = 0.42;
    static final double CLOTH_LENGTH = 0.84;
    static final double[][] CORNERS = new double[][]{{0.0, 0.0}, {0.37, 0.34}, {-0.37, 0.34}};
    public static final double WEAVE_LENGTH = 2.4;
    static final double WEAVE_FROM = 0.2;
    static final double WEAVE_TO = 1.85;
    static final int PASSES = 6;
    static final double BEAT = 2.0;
    private static final int COLS = 5;
    private static final int ROWS = 4;
    private static final int CELL_W = 22;
    private static final int CELL_H = 38;
    private static final int PAD = 6;
    private static final int HEADER = 13;
    public final Vec3 top;
    public Target target = Target.NONE;
    public int hoverSlot = -1;
    public int hoverPattern = -1;
    private int placeSlot = -1;
    private int scrollRow;
    private double scroll;
    private double board;
    private int boardX;
    private int boardY;
    private int boardW;
    private int boardH;
    private boolean boardShown;
    private int lastHover = -1;
    @Nullable
    private ModelPart flag;
    private DyeColor weaveBase = DyeColor.WHITE;
    private List<Layer> weaveLayers = List.of();
    @Nullable
    private Layer weaveNew;
    private ItemStack woven = ItemStack.f_41583_;
    private ItemStack shuttle = ItemStack.f_41583_;
    private boolean toInventory;
    private int passesDone;

    static List<Layer> layersOf(ItemStack banner) {
        ListTag list = BannerBlockEntity.m_58487_((ItemStack)banner);
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        ArrayList<Layer> layers = new ArrayList<Layer>(list.size());
        for (int i = 0; i < list.size(); ++i) {
            CompoundTag entry = list.m_128728_(i);
            Holder pattern = BannerPattern.m_222700_((String)entry.m_128461_("Pattern"));
            if (pattern == null) continue;
            layers.add(new Layer((Holder<BannerPattern>)pattern, DyeColor.m_41053_((int)entry.m_128451_("Color"))));
        }
        return layers;
    }

    static int diffuse(DyeColor color) {
        float[] c = color.m_41068_();
        return Math.round(c[0] * 255.0f) << 16 | Math.round(c[1] * 255.0f) << 8 | Math.round(c[2] * 255.0f);
    }

    private static Material material(Holder<BannerPattern> pattern) {
        return Sheets.m_234347_((ResourceKey)pattern.m_203543_().orElse(BannerPatterns.f_222726_));
    }

    public LoomStation(BlockPos pos, Direction side) {
        super(pos, side, LoomMenu.class, 3);
        this.top = new Vec3((double)pos.m_123341_() + 0.5, (double)pos.m_123342_() + 1.0, (double)pos.m_123343_() + 0.5);
    }

    @Override
    public StationKind kind() {
        return StationKind.LOOM;
    }

    @Override
    public Vec3 center() {
        return this.top;
    }

    @Override
    public SoundEvent touchSound() {
        return SoundEvents.f_12591_;
    }

    static Vec3 slotPoint(Vec3 top, Direction side, int slot) {
        BlockPos pos = BlockPos.m_274561_((double)top.f_82479_, (double)(top.f_82480_ - 1.0), (double)top.f_82481_);
        return SpatialLayouts.current(StationKind.LOOM).point(SpatialAppearance.Frame.of(StationKind.LOOM, pos, side, Direction.UP), slot);
    }

    @Override
    protected Vec3 restPoint(int slot) {
        return LoomStation.slotPoint(this.top, this.side, slot).m_82549_(this.right().m_82490_(this.denyShake()));
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
    public List<Vec3> workSamples() {
        return this.layoutSamples(super.workSamples());
    }

    @Override
    public float layoutHeldScale() {
        int slot = this.hoverSlot >= 0 ? this.hoverSlot : this.placeSlot;
        return slot < 0 ? 1.0f : this.geometry().scale(slot);
    }

    private Vec3 clothAcross() {
        return this.geometry().axis(this.layoutFrame(), 0, true);
    }

    private Vec3 clothNear() {
        return this.geometry().axis(this.layoutFrame(), 0, false);
    }

    private double clothWidth() {
        return 0.42 * (double)this.geometry().scale(0);
    }

    private double clothLength() {
        return 0.84 * (double)this.geometry().scale(0);
    }

    private float itemRotation(int slot) {
        return (float)(-this.geometry().rotation(slot));
    }

    @Override
    public void layoutGhost(PoseStack pose, MultiBufferSource buffers, Vec3 cam, int slot, int light) {
        if (slot == 0) {
            LoomStation.drawCloth(pose, buffers, cam, this.layoutPoint(0).m_82520_(0.0, 0.003, 0.0), this.clothNear().m_82490_(-1.0), this.clothAcross(), new Vec3(0.0, 1.0, 0.0), this.clothWidth(), this.clothLength(), DyeColor.WHITE, List.of(), null, 1.0, light);
        } else {
            SmithingStation.drawLaid(pose, buffers, cam, new ItemStack((ItemLike)(slot == 1 ? Items.f_42497_ : Items.f_42720_)), this.layoutPoint(slot), this.side, 2, 0.95f * this.geometry().scale(slot), light, this.itemRotation(slot));
        }
    }

    @Override
    public List<Vec3> approach(LocalPlayer player) {
        Vec3 stand = new Vec3(this.top.f_82479_ + (double)this.side.m_122429_() * 0.86, player.m_20186_(), this.top.f_82481_ + (double)this.side.m_122431_() * 0.86);
        boolean walk = Math.hypot(stand.f_82479_ - player.m_20185_(), stand.f_82481_ - player.m_20189_()) > 0.06;
        return walk ? List.of(stand) : List.of();
    }

    @Override
    public CameraRig.Pose lean() {
        Vec3 eye = this.top.m_82549_(this.side().m_82490_(0.8)).m_82549_(this.right().m_82490_(0.08)).m_82520_(0.0, 0.72, 0.0);
        Vec3 look = this.top.m_82549_(this.side().m_82490_(-0.02)).m_82549_(this.right().m_82490_(0.12)).m_82520_(0.0, 0.05, 0.0).m_82546_(eye);
        float yaw = (float)Math.toDegrees(Math.atan2(-look.f_82479_, look.f_82481_));
        float pitch = (float)(-Math.toDegrees(Math.atan2(look.f_82480_, Math.hypot(look.f_82479_, look.f_82481_))));
        return new CameraRig.Pose(eye, yaw, pitch, 0.0f);
    }

    @Override
    public Vec3 rest(boolean right) {
        double editing = this.gridEditor.presence();
        return this.top.m_82549_(this.side().m_82490_(0.4 + 0.12 * editing)).m_82549_(this.right().m_82490_(right ? 0.25 : -0.25)).m_82520_(0.0, 0.062 - 0.24 * editing, 0.0);
    }

    private List<Holder<BannerPattern>> patterns() {
        LoomMenu menu = (LoomMenu)this.menu();
        return menu == null || this.slot(0).m_41619_() || this.slot(1).m_41619_() || this.full() ? List.of() : menu.m_219995_();
    }

    private boolean full() {
        return BannerBlockEntity.m_58504_((ItemStack)this.slot(0)) >= 6;
    }

    private static DyeColor baseOf(ItemStack banner) {
        DyeColor dyeColor;
        Item item = banner.m_41720_();
        if (item instanceof BannerItem) {
            BannerItem item2 = (BannerItem)item;
            dyeColor = item2.m_40545_();
        } else {
            dyeColor = DyeColor.WHITE;
        }
        return dyeColor;
    }

    private DyeColor dye() {
        DyeColor dyeColor;
        Item item = this.slot(1).m_41720_();
        if (item instanceof DyeItem) {
            DyeItem dye = (DyeItem)item;
            dyeColor = dye.m_41089_();
        } else {
            dyeColor = DyeColor.WHITE;
        }
        return dyeColor;
    }

    @Override
    public void scroll(double delta) {
        int rows = (this.patterns().size() + 5 - 1) / 5;
        int next = Mth.m_14045_((int)(this.scrollRow - (int)Math.signum(delta)), (int)0, (int)Math.max(0, rows - 4));
        if (next != this.scrollRow) {
            this.scrollRow = next;
            Scene.sound(SoundEvents.f_12491_, 0.25f, 1.5f);
        }
    }

    @Override
    public void updateCursor(boolean overPanel) {
        this.clearCursor();
        if (overPanel || this.gridEditor.visible() || this.menu() == null || this.moving()) {
            return;
        }
        double[] mouse = Picking.mouseGui();
        if (this.boardShown && mouse[0] >= (double)this.boardX && mouse[0] < (double)(this.boardX + this.boardW) && mouse[1] >= (double)this.boardY && mouse[1] < (double)(this.boardY + this.boardH)) {
            int col = (int)((mouse[0] - (double)this.boardX - 6.0) / 22.0);
            int row = (int)((mouse[1] - (double)this.boardY - 6.0 - 13.0) / 38.0);
            int k = (row + this.scrollRow) * 5 + col;
            if (col >= 0 && col < 5 && row >= 0 && row < 4 && mouse[1] >= (double)(this.boardY + 6 + 13) && k < this.patterns().size()) {
                this.target = Target.PATTERN;
                this.hoverPattern = k;
            }
            this.aim = this.top.m_82549_(this.right().m_82490_(0.42)).m_82549_(this.side().m_82490_(0.05)).m_82520_(0.0, 0.12, 0.0);
            return;
        }
        ItemStack carried = Hands.carried();
        if (!this.geometry().equals(SpatialAppearance.VANILLA)) {
            int picked = this.layoutPick();
            if (picked >= 0) {
                if (carried.m_41619_() && !this.slot(picked).m_41619_()) {
                    this.target = Target.ITEM;
                    this.hoverSlot = picked;
                    this.aim = this.restPoint(picked);
                } else if (!carried.m_41619_()) {
                    this.target = Target.PLACE;
                    this.placeSlot = this.slotFor(carried, picked);
                    this.aim = this.restPoint(this.placeSlot < 0 ? picked : this.placeSlot);
                }
            }
            return;
        }
        if (carried.m_41619_()) {
            for (int i = 0; i < 3; ++i) {
                if (this.slot(i).m_41619_() || !GrindstoneStation.under(mouse, this.restPoint(i), i == 0 ? 0.24 : 0.09)) continue;
                this.target = Target.ITEM;
                this.hoverSlot = i;
                this.aim = this.restPoint(i);
                return;
            }
            return;
        }
        Vec3 hit = Picking.cursorOnPlane(this.top.f_82480_);
        if (hit == null || Math.abs(hit.f_82479_ - this.top.f_82479_) > 0.52 || Math.abs(hit.f_82481_ - this.top.f_82481_) > 0.52) {
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
        this.hoverPattern = -1;
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
        if (this.gridEditor.visible()) {
            return;
        }
        switch (this.target) {
            case PLACE: {
                if (this.placeSlot < 0) {
                    this.deny();
                    return;
                }
                this.slotClick(this.placeSlot, button, false);
                Scene.sound(SoundEvents.f_12642_, 0.5f, 1.1f);
                break;
            }
            case ITEM: {
                this.slotClick(this.hoverSlot, 0, shift);
                break;
            }
            case PATTERN: {
                if (button != 0) break;
                this.weave(this.hoverPattern, shift);
                break;
            }
        }
    }

    private void weave(int k, boolean shift) {
        LoomMenu menu = (LoomMenu)this.menu();
        Minecraft mc = Minecraft.m_91087_();
        List<Holder<BannerPattern>> patterns = this.patterns();
        if (menu == null || mc.f_91072_ == null || mc.f_91074_ == null || k < 0 || k >= patterns.size()) {
            return;
        }
        ItemStack banner = this.slot(0);
        this.weaveBase = LoomStation.baseOf(banner);
        this.weaveLayers = LoomStation.layersOf(banner);
        this.weaveNew = new Layer(patterns.get(k), this.dye());
        this.shuttle = this.slot(1).m_255036_(1);
        menu.m_6366_((Player)mc.f_91074_, k);
        mc.f_91072_.m_105208_(menu.f_38840_, k);
        this.woven = this.slot(3).m_41777_();
        this.toInventory = shift;
        this.passesDone = 0;
        Scene.sound(SoundEvents.f_12491_, 0.7f, 1.0f);
        this.startMove(2.4, 2.0, () -> {
            LoomMenu open = (LoomMenu)this.menu();
            if (this.slot(3).m_41619_()) {
                this.cancelMove();
                return;
            }
            mc.f_91072_.m_171799_(open.f_38840_, 3, 0, this.toInventory ? ClickType.QUICK_MOVE : ClickType.PICKUP, (Player)mc.f_91074_);
        });
    }

    private double woven(double t) {
        return Anim.window(t, 0.2, 1.85);
    }

    private double shuttleAcross(double t) {
        double passes = this.woven(t) * 6.0;
        int pass = Math.min(5, (int)passes);
        double f = Anim.inOutSine(passes - (double)pass);
        return pass % 2 == 0 ? -1.0 + 2.0 * f : 1.0 - 2.0 * f;
    }

    private Vec3 weaveLine(double t) {
        return this.layoutPoint(0).m_82549_(this.clothNear().m_82490_(-this.clothLength() / 2.0 + this.clothLength() * this.woven(t))).m_82520_(0.0, 0.01, 0.0);
    }

    @Override
    @Nullable
    public Vec3[] handsOverride() {
        double t = this.moveTime();
        if (t < 0.0) {
            return null;
        }
        double in = Anim.inOutCubic(Anim.window(t, 0.0, 0.2)) * (1.0 - Anim.inOutCubic(Anim.window(t, 1.8, 2.0)));
        Vec3 line = this.weaveLine(Math.min(t, 1.85)).m_82549_(this.clothNear().m_82490_(0.06)).m_82520_(0.0, 0.092, 0.0);
        double across = this.shuttleAcross(t);
        double beat = Anim.hump(Anim.window(this.woven(t) * 6.0 % 1.0, 0.8, 1.0)) * 0.02;
        Vec3 r = line.m_82549_(this.clothAcross().m_82490_(this.clothWidth() / 2.0 + 0.06 - 0.05 * Math.max(0.0, across))).m_82549_(this.clothNear().m_82490_(-beat));
        Vec3 l = line.m_82549_(this.clothAcross().m_82490_(-this.clothWidth() / 2.0 - 0.06 + 0.05 * Math.max(0.0, -across))).m_82549_(this.clothNear().m_82490_(-beat));
        Vec3 right = Anim.lerp(this.rest(true), r, in);
        Vec3 left = Anim.lerp(this.rest(false), l, in);
        if (t > 1.9) {
            Vec3 grab = this.layoutPoint(0).m_82549_(this.clothNear().m_82490_(0.2)).m_82520_(0.0, 0.10200000000000001, 0.0);
            right = Anim.lerp(Anim.lerp(right, grab, Anim.inOutCubic(Anim.window(t, 1.9, 2.05))), this.rest(true).m_82520_(0.0, this.toInventory ? 0.0 : 0.05, 0.0), Anim.inOutCubic(Anim.window(t, 2.05, 2.4)));
        }
        return new Vec3[]{right, left};
    }

    @Override
    void frame() {
        super.frame();
        double t = this.moveTime();
        double target = this.boardShown ? 1.0 : 0.0;
        this.board = Anim.approach(this.board, target, target > this.board ? 9.0 : 14.0, Scene.frameDt);
        this.scroll = Anim.approach(this.scroll, this.scrollRow, 14.0, Scene.frameDt);
        if (t < 0.0) {
            return;
        }
        int passes = (int)Math.min(6.0, this.woven(t) * 6.0 + 0.5);
        if (passes > this.passesDone) {
            this.passesDone = passes;
            Scene.sound(SoundEvents.f_12491_, 0.35f, 1.3f + (float)(passes % 2) * 0.15f);
            float[] c = LoomStation.rgb(this.weaveNew == null ? DyeColor.WHITE : this.weaveNew.color());
            Fx.motes(this.weaveLine(t).m_82549_(this.clothAcross().m_82490_(this.shuttleAcross(t) * this.clothWidth() / 2.0)), 0.05, 4, c[0], c[1], c[2], 0.12);
        }
    }

    @Override
    public void tick() {
        if (this.hoverPattern != this.lastHover && this.hoverPattern >= 0) {
            Scene.sound(SoundEvents.f_12641_, 0.35f, 1.4f);
        }
        this.lastHover = this.hoverPattern;
    }

    static float[] rgb(DyeColor color) {
        return (float[])color.m_41068_().clone();
    }

    @Override
    public void renderScene(PoseStack pose, MultiBufferSource buffers, Camera camera, int light) {
        Vec3 cam = camera.m_90583_();
        if (this.outro()) {
            this.drawLeaving(pose, buffers, camera, light);
            return;
        }
        double t = this.moveTime();
        Vec3 far = this.clothNear().m_82490_(-1.0);
        Vec3 up = new Vec3(0.0, 1.0, 0.0);
        if (t >= 0.0) {
            this.drawWeaving(pose, buffers, camera, t, light);
        } else if (!this.slot(0).m_41619_()) {
            ItemStack banner = this.slot(0);
            List<Layer> layers = LoomStation.layersOf(banner);
            Layer preview = null;
            if (this.hoverPattern >= 0 && this.hoverPattern < this.patterns().size()) {
                preview = new Layer(this.patterns().get(this.hoverPattern), this.dye());
            }
            LoomStation.drawCloth(pose, buffers, cam, this.layoutPoint(0).m_82520_(0.0, 0.003, 0.0).m_82549_(this.right().m_82490_(this.denyShake())), far, this.clothAcross(), up, this.clothWidth(), this.clothLength(), LoomStation.baseOf(banner), layers, preview, 1.0, light);
        }
        for (int i = 1; i < 3; ++i) {
            ItemStack stack = this.slot(i);
            if (i == 1 && t >= 0.0 && t < 1.85) {
                stack = stack.m_255036_(Math.max(0, stack.m_41613_() - 1));
            }
            SmithingStation.drawLaid(pose, buffers, cam, stack, this.restPoint(i), this.side, 2, (i == this.hoverSlot ? 1.08f : 0.95f) * this.geometry().scale(i), light, this.itemRotation(i));
        }
    }

    private void drawWeaving(PoseStack pose, MultiBufferSource buffers, Camera camera, double t, int light) {
        Vec3 cam = camera.m_90583_();
        Vec3 far = this.clothNear().m_82490_(-1.0);
        Vec3 up = new Vec3(0.0, 1.0, 0.0);
        if (t < 2.0) {
            LoomStation.drawCloth(pose, buffers, cam, this.layoutPoint(0).m_82520_(0.0, 0.003, 0.0), far, this.clothAcross(), up, this.clothWidth(), this.clothLength(), this.weaveBase, this.weaveLayers, this.weaveNew, this.woven(t), light);
            if (t >= 0.2 && t < 1.85) {
                Vec3 line = this.weaveLine(t);
                Vec3 at = line.m_82549_(this.clothAcross().m_82490_(this.shuttleAcross(t) * (this.clothWidth() / 2.0 + 0.04))).m_82520_(0.0, 0.03, 0.0);
                ItemPlacer.drawOriented(pose, buffers, cam, this.shuttle, at, ItemPlacer.lying(ItemPlacer.awayYaw(this.side) + this.itemRotation(0) + 90.0f, 15.0f), 0.13f * this.geometry().scale(1), light);
                float[] c = LoomStation.rgb(this.weaveNew.color());
                float g = 0.35f;
                EnchantFx.billboard(pose, buffers, camera, line.m_82549_(this.clothAcross().m_82490_(this.shuttleAcross(t) * this.clothWidth() / 2.0)), 0.05, 0.0, EnchantFx.GLOW, c[0] * g + 0.1f, c[1] * g + 0.1f, c[2] * g + 0.1f);
            }
            return;
        }
        double k = Anim.inOutCubic(Anim.window(t, 2.0, 2.4));
        Vec3 lift = this.layoutPoint(0).m_82520_(0.0, 0.003 + 0.25 * k, 0.0).m_82549_(this.clothNear().m_82490_(0.15 * k));
        Vec3 destination = this.toInventory ? cam.m_82520_(0.0, 0.5, 0.0).m_82549_(Scene.yawForward(camera.m_90590_()).m_82490_(0.6)) : this.heldItemPoint(Hands.position(true), Hands.shoulder(true, cam, camera.m_90590_()));
        Vec3 center = Anim.lerp(lift, destination, Anim.inCubic(Anim.window(t, 2.15, 2.4)));
        double stand = Anim.smoother(k * 1.4);
        Vec3 vAxis = Anim.lerp(far, up, stand).m_82541_();
        Vec3 normal = Anim.lerp(up, this.clothNear(), stand).m_82541_();
        double size = 1.0 - 0.75 * Anim.inCubic(Anim.window(t, 2.1, 2.4));
        List<Layer> after = LoomStation.layersOf(this.woven);
        LoomStation.drawCloth(pose, buffers, cam, center, vAxis, this.clothAcross(), normal, this.clothWidth() * size, this.clothLength() * size, LoomStation.baseOf(this.woven), after, null, 1.0, light);
    }

    static void drawCloth(PoseStack pose, MultiBufferSource buffers, Vec3 cam, Vec3 center, Vec3 up, Vec3 right, Vec3 normal, double width, double length, DyeColor base, List<Layer> layers, @Nullable Layer extra, double reveal, int light) {
        double[][] pieces;
        double[][] dArrayArray;
        boolean partly;
        Vec3 topLeft = center.m_82549_(up.m_82490_(length / 2.0)).m_82546_(right.m_82490_(width / 2.0));
        boolean bl = partly = extra != null && reveal > 0.001 && reveal < 0.999;
        if (partly) {
            double[][] dArrayArray2 = new double[2][];
            dArrayArray2[0] = new double[]{0.0, reveal};
            dArrayArray = dArrayArray2;
            dArrayArray2[1] = new double[]{reveal, 1.0};
        } else {
            double[][] dArrayArray3 = new double[1][];
            dArrayArray = dArrayArray3;
            dArrayArray3[0] = new double[]{0.0, 1.0};
        }
        for (double[] piece : pieces = dArrayArray) {
            LoomStation.layer(pose, buffers.m_6299_(RenderType.m_110446_((ResourceLocation)Sheets.f_110737_)), cam, topLeft, up, right, normal, width, length, ModelBakery.f_119224_, -1, piece, 0, light);
            LoomStation.layer(pose, buffers.m_6299_(RenderType.m_110482_((ResourceLocation)Sheets.f_110737_)), cam, topLeft, up, right, normal, width, length, Sheets.m_234347_((ResourceKey)BannerPatterns.f_222726_), LoomStation.diffuse(base), piece, 1, light);
            int i = 2;
            for (Layer l : layers) {
                LoomStation.layer(pose, buffers.m_6299_(RenderType.m_110482_((ResourceLocation)Sheets.f_110737_)), cam, topLeft, up, right, normal, width, length, LoomStation.material(l.pattern()), LoomStation.diffuse(l.color()), piece, i++, light);
            }
        }
        if (extra != null && reveal > 0.001) {
            LoomStation.layer(pose, buffers.m_6299_(RenderType.m_110482_((ResourceLocation)Sheets.f_110737_)), cam, topLeft, up, right, normal, width, length, LoomStation.material(extra.pattern()), LoomStation.diffuse(extra.color()), new double[]{0.0, Math.min(1.0, reveal)}, 2 + layers.size(), light);
        }
    }

    private static void layer(PoseStack pose, VertexConsumer vc, Vec3 cam, Vec3 topLeft, Vec3 up, Vec3 right, Vec3 normal, double width, double length, Material material, int argb, double[] piece, int index, int light) {
        TextureAtlasSprite sprite = material.m_119204_();
        Vec3 lift = normal.m_82490_(7.0E-4 * (double)index);
        Vec3 a = topLeft.m_82549_(lift).m_82546_(cam).m_82546_(up.m_82490_(length * piece[0]));
        Vec3 b = a.m_82549_(right.m_82490_(width));
        Vec3 c = b.m_82546_(up.m_82490_(length * (piece[1] - piece[0])));
        Vec3 d = a.m_82546_(up.m_82490_(length * (piece[1] - piece[0])));
        float u0 = Fx.u(sprite, 0.015625);
        float u1 = Fx.u(sprite, 0.328125);
        float v0 = Fx.v(sprite, (float)((1.0 + 40.0 * piece[0]) / 64.0));
        float v1 = Fx.v(sprite, (float)((1.0 + 40.0 * piece[1]) / 64.0));
        Vec3[] p = new Vec3[]{a, d, c, b};
        float[][] uv = new float[][]{{u0, v0}, {u0, v1}, {u1, v1}, {u1, v0}};
        int r = argb >> 16 & 0xFF;
        int g = argb >> 8 & 0xFF;
        int bl = argb & 0xFF;
        for (int k = 0; k < 4; ++k) {
            vc.m_252986_(pose.m_85850_().m_252922_(), (float)p[k].f_82479_, (float)p[k].f_82480_, (float)p[k].f_82481_).m_6122_(r, g, bl, 255).m_7421_(uv[k][0], uv[k][1]).m_86008_(OverlayTexture.f_118083_).m_85969_(light).m_252939_(pose.m_85850_().m_252943_(), (float)normal.f_82479_, (float)normal.f_82480_, (float)normal.f_82481_).m_5752_();
        }
    }

    @Override
    public void renderWorld(PoseStack pose, MultiBufferSource buffers, Vec3 camera, int light) {
        this.gridEditor.renderWorld(pose, buffers, camera, light);
        if (this.target == Target.PLACE && this.aim != null) {
            float p = (float)(0.18 + 0.06 * Math.sin(Scene.clock() * 5.0));
            boolean ok = this.placeSlot >= 0;
            EnchantFx.disc(pose, buffers, camera, this.aim.m_82520_(0.0, 0.004, 0.0), this.placeSlot == 0 ? 0.24 : 0.1, 0.0, EnchantFx.GLOW, p, ok ? p * 0.8f : p * 0.2f, ok ? p * 0.6f : p * 0.2f);
        }
    }

    @Override
    public void renderScreen(GuiGraphics g, Font font, float alpha) {
        this.renderBoard(g, font, alpha);
        this.gridEditor.render(g, font, alpha);
    }

    private void renderBoard(GuiGraphics g, Font font, float alpha) {
        List<Holder<BannerPattern>> patterns = this.patterns();
        boolean bl = this.boardShown = !this.gridEditor.visible() && !patterns.isEmpty() && !this.moving() && Scene.phase == Scene.Phase.ACTIVE;
        if (this.board < 0.02 || patterns.isEmpty()) {
            return;
        }
        int rows = Math.min(4, (patterns.size() + 5 - 1) / 5);
        this.boardW = 122;
        this.boardH = 25 + rows * 38;
        double[] anchor = Picking.toGui(this.top.m_82549_(this.right().m_82490_(0.58)).m_82520_(0.0, 0.28, 0.0));
        int width = g.m_280182_();
        int height = g.m_280206_();
        double ax = anchor == null ? (double)width * 0.7 : anchor[0];
        double ay = anchor == null ? (double)height * 0.45 : anchor[1];
        float appear = (float)Anim.smoother(this.board) * alpha * (1.0f - this.gridEditor.presence());
        if (Math.round(0.75f * appear * 255.0f) < 8) {
            return;
        }
        this.boardX = (int)Mth.m_14008_((double)(ax + 6.0 + (1.0 - this.board) * 18.0), (double)8.0, (double)(width - this.boardW - (Settings.developerMode() ? 48 : 8)));
        this.boardY = (int)Mth.m_14008_((double)(ay - (double)this.boardH / 2.0), (double)((double)height * 0.14), (double)((double)height * 0.86 - (double)this.boardH));
        g.m_280509_(this.boardX, this.boardY, this.boardX + this.boardW, this.boardY + this.boardH, LoomStation.argb(0.5f * appear, 12, 12, 16));
        g.m_280509_(this.boardX, this.boardY + this.boardH - 1, this.boardX + this.boardW, this.boardY + this.boardH, LoomStation.argb(0.22f * appear, 255, 255, 255));
        g.m_280509_(this.boardX, this.boardY, this.boardX + 1, this.boardY + this.boardH - 1, LoomStation.argb(0.08f * appear, 255, 255, 255));
        g.m_280509_(this.boardX + this.boardW - 1, this.boardY, this.boardX + this.boardW, this.boardY + this.boardH - 1, LoomStation.argb(0.08f * appear, 255, 255, 255));
        MutableComponent title = Component.m_237115_((String)"cinematic.loom.board");
        g.m_280614_(font, (Component)title, this.boardX + 6, this.boardY + 6 - 1, LoomStation.argb(0.75f * appear, 242, 234, 216), false);
        int total = (patterns.size() + 5 - 1) / 5;
        if (total > 4) {
            String page = this.scrollRow + 1 + "\u2013" + Math.min(total, this.scrollRow + 4) + " / " + total;
            g.m_280056_(font, page, this.boardX + this.boardW - 6 - font.m_92895_(page), this.boardY + 6 - 1, LoomStation.argb(0.45f * appear, 242, 234, 216), false);
        }
        if (appear < 0.3f) {
            return;
        }
        ItemStack banner = this.slot(0);
        DyeColor base = LoomStation.baseOf(banner);
        List<Layer> layers = LoomStation.layersOf(banner);
        DyeColor dye = this.dye();
        LoomMenu menu = (LoomMenu)this.menu();
        int selected = menu == null ? -1 : menu.m_39891_();
        g.m_280588_(this.boardX, this.boardY + 6 + 13 - 1, this.boardX + this.boardW, this.boardY + this.boardH - 6 + 1);
        double offset = (this.scroll - (double)this.scrollRow) * 38.0;
        for (int row = -1; row <= rows; ++row) {
            for (int col = 0; col < 5; ++col) {
                int k = (row + this.scrollRow) * 5 + col;
                if (k < 0 || k >= patterns.size()) continue;
                int x = this.boardX + 6 + col * 22;
                int y = (int)((double)(this.boardY + 6 + 13 + row * 38) - offset);
                boolean hovered = k == this.hoverPattern;
                g.m_280509_(x + 1, y + 1, x + 22 - 1, y + 38 - 1, LoomStation.argb((hovered ? 0.3f : 0.1f) * appear, 255, 255, 255));
                if (k == selected) {
                    int c = LoomStation.argb(0.8f * appear, 255, 226, 150);
                    g.m_280509_(x, y, x + 22, y + 1, c);
                    g.m_280509_(x, y + 38 - 1, x + 22, y + 38, c);
                }
                ArrayList<Layer> with = new ArrayList<Layer>(layers);
                with.add(new Layer(patterns.get(k), dye));
                this.drawFlag(g, (float)x + 11.0f, y + 3 - (hovered ? 1 : 0), hovered ? 15.0f : 14.0f, base, with);
            }
        }
        g.m_280618_();
    }

    @Nullable
    public double[] boardCell(int k) {
        int row = k / 5 - this.scrollRow;
        if (!this.boardShown || row < 0 || row >= 4) {
            return null;
        }
        return new double[]{(double)(this.boardX + 6) + ((double)(k % 5) + 0.5) * 22.0, (double)(this.boardY + 6 + 13) + ((double)row + 0.5) * 38.0};
    }

    public int patternIndex(String asset) {
        List<Holder<BannerPattern>> patterns = this.patterns();
        for (int k = 0; k < patterns.size(); ++k) {
            if (!patterns.get(k).m_203543_().map(key -> key.m_135782_().toString()).orElse("").equals(asset)) continue;
            return k;
        }
        return -1;
    }

    private void drawFlag(GuiGraphics g, float x, float y, float width, DyeColor base, List<Layer> layers) {
        if (this.flag == null) {
            this.flag = Minecraft.m_91087_().m_167973_().m_171103_(ModelLayers.f_171264_).m_171324_("flag");
        }
        this.flag.f_104203_ = 0.0f;
        this.flag.f_104201_ = 0.0f;
        float k = width / 1.25f;
        PoseStack ps = g.m_280168_();
        ps.m_85836_();
        ps.m_252880_(x, y, 120.0f);
        ps.m_85841_(k, k, -k);
        ArrayList<Pair> patterns = new ArrayList<Pair>(layers.size() + 1);
        patterns.add(Pair.of((Object)BuiltInRegistries.f_256878_.m_246971_(BannerPatterns.f_222726_), (Object)base));
        for (Layer layer : layers) {
            patterns.add(Pair.of(layer.pattern(), (Object)layer.color()));
        }
        BannerRenderer.m_112065_((PoseStack)ps, (MultiBufferSource)g.m_280091_(), (int)0xF000F0, (int)OverlayTexture.f_118083_, (ModelPart)this.flag, (Material)ModelBakery.f_119224_, (boolean)true, patterns);
        ps.m_85849_();
        g.m_280262_();
    }

    private static int argb(float a, int r, int g, int b) {
        return Mth.m_14045_((int)Math.round(a * 255.0f), (int)0, (int)255) << 24 | r << 16 | g << 8 | b;
    }

    static void drawContents(PoseStack pose, MultiBufferSource buffers, Vec3 cam, BlockPos pos, Direction side, List<ItemStack> items, int light) {
        SpatialAppearance.Frame frame = SpatialAppearance.Frame.of(StationKind.LOOM, pos, side, Direction.UP);
        SpatialAppearance layout = SpatialLayouts.current(StationKind.LOOM);
        Vec3 far = layout.axis(frame, 0, false).m_82490_(-1.0);
        Vec3 right = layout.axis(frame, 0, true);
        float size = layout.scale(0);
        if (!items.isEmpty() && !items.get(0).m_41619_()) {
            ItemStack banner = items.get(0);
            LoomStation.drawCloth(pose, buffers, cam, layout.point(frame, 0).m_82520_(0.0, 0.003, 0.0), far, right, new Vec3(0.0, 1.0, 0.0), 0.42 * (double)size, 0.84 * (double)size, LoomStation.baseOf(banner), LoomStation.layersOf(banner), null, 1.0, light);
        }
        for (int i = 1; i < Math.min(3, items.size()); ++i) {
            SmithingStation.drawLaid(pose, buffers, cam, items.get(i), layout.point(frame, i), side, 2, 0.95f * layout.scale(i), light, (float)(-layout.rotation(i)));
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
        switch (this.target) {
            case PATTERN: {
                ArrayList<Component> lines = new ArrayList<Component>();
                lines.add((Component)new Layer(this.patterns().get(this.hoverPattern), this.dye()).description().m_130940_(ChatFormatting.WHITE));
                lines.add((Component)Component.m_237115_((String)"cinematic.loom.weave").m_130940_(ChatFormatting.GOLD));
                return lines;
            }
            case ITEM: {
                ArrayList<Component> lines = new ArrayList<Component>(Screen.m_280152_((Minecraft)mc, (ItemStack)this.slot(this.hoverSlot)));
                if (this.hoverSlot == 0 && this.full()) {
                    lines.add((Component)Component.m_237115_((String)"cinematic.loom.full").m_130940_(ChatFormatting.RED));
                } else if (this.hoverSlot == 0 && this.slot(1).m_41619_()) {
                    lines.add((Component)Component.m_237115_((String)"cinematic.loom.need_dye").m_130940_(ChatFormatting.GRAY));
                }
                lines.add((Component)Component.m_237115_((String)"cinematic.loom.take").m_130940_(ChatFormatting.GRAY));
                return lines;
            }
            case PLACE: {
                return this.placeSlot < 0 ? List.of(Component.m_237115_((String)"cinematic.loom.refuse").m_130940_(ChatFormatting.RED)) : null;
            }
        }
        return null;
    }

    @Override
    public Component hint() {
        return Component.m_237115_((String)"cinematic.hint.loom");
    }

    record Layer(Holder<BannerPattern> pattern, DyeColor color) {
        MutableComponent description() {
            String key = this.pattern.m_203543_().map(k -> k.m_135782_().m_214299_()).orElse("base");
            ResourceLocation file = new ResourceLocation(key);
            return Component.m_237115_((String)("block." + file.m_135827_() + ".banner." + file.m_135815_() + "." + this.color.m_41065_()));
        }
    }

    public static enum Target {
        NONE,
        PLACE,
        ITEM,
        PATTERN;

    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  javax.annotation.Nullable
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.renderer.LevelRenderer
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.block.BlockModelShaper
 *  net.minecraft.client.renderer.block.model.BakedQuad
 *  net.minecraft.client.renderer.culling.Frustum
 *  net.minecraft.client.resources.model.BakedModel
 *  net.minecraft.client.resources.model.ModelResourceLocation
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.SectionPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.util.Mth
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.BlockAndTintGetter
 *  net.minecraft.world.level.ChunkPos
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.ChiseledBookShelfBlock
 *  net.minecraft.world.level.block.HorizontalDirectionalBlock
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.client.event.ModelEvent$ModifyBakingResult
 *  net.minecraftforge.client.model.BakedModelWrapper
 *  net.minecraftforge.client.model.data.ModelData
 *  net.minecraftforge.client.model.data.ModelProperty
 *  org.joml.Matrix3f
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 *  org.joml.Vector3f
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.client.Anim;
import dev.dyslexic.cinematic.client.CameraRig;
import dev.dyslexic.cinematic.client.ClientOnly;
import dev.dyslexic.cinematic.client.Fx;
import dev.dyslexic.cinematic.client.OpenBook;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.ShelfLayoutBooks;
import dev.dyslexic.cinematic.client.ShelfStation;
import dev.dyslexic.cinematic.client.SpatialAppearance;
import dev.dyslexic.cinematic.client.SpatialLayouts;
import dev.dyslexic.cinematic.net.MenuFxPayload;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

public final class ShelfBooks {
    static final int[][] SPINES = new int[][]{{1, 2, 5, 7}, {6, 1, 10, 7}, {11, 1, 15, 7}, {1, 9, 5, 15}, {6, 10, 10, 15}, {11, 9, 15, 15}};
    private static final String TEXTURE = "block/chiseled_bookshelf_occupied";
    static final double DEPTH = 0.32;
    static final double PROUD = 0.004;
    static final double CLEAR = 0.37;
    private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);
    private static final List<Flight> FLIGHTS = new ArrayList<Flight>();
    private static final Map<BlockPos, Integer> LOCAL = new HashMap<BlockPos, Integer>();
    private static final Map<BlockPos, Integer> HIDDEN = new ConcurrentHashMap<BlockPos, Integer>();
    private static final ModelProperty<Integer> EMPTIED = new ModelProperty();

    private ShelfBooks() {
    }

    static Quaternionf basis(Vec3 a, Vec3 s, Vec3 n) {
        return new Matrix3f((float)a.f_82479_, (float)a.f_82480_, (float)a.f_82481_, (float)s.f_82479_, (float)s.f_82480_, (float)s.f_82481_, (float)n.f_82479_, (float)n.f_82480_, (float)n.f_82481_).getNormalizedRotation(new Quaternionf());
    }

    static Vec3 out(Direction facing) {
        return Vec3.m_82528_((Vec3i)facing.m_122436_());
    }

    static Vec3 right(Direction facing) {
        return Vec3.m_82528_((Vec3i)facing.m_122428_().m_122436_());
    }

    static Vec3 face(BlockPos pos, Direction facing, double u, double v, double out) {
        return Vec3.m_82512_((Vec3i)pos).m_82549_(ShelfBooks.out(facing).m_82490_(0.5 + out)).m_82549_(ShelfBooks.right(facing).m_82490_(u / 16.0 - 0.5)).m_82520_(0.0, 0.5 - v / 16.0, 0.0);
    }

    static Vec3 spine(BlockPos pos, Direction facing, int slot) {
        return SpatialLayouts.current(StationKind.CHISELED_BOOKSHELF).point(SpatialAppearance.Frame.of(StationKind.CHISELED_BOOKSHELF, pos, facing, facing), slot);
    }

    static Pose inSlot(BlockPos pos, Direction facing, int slot, double out) {
        int[] r = SPINES[slot];
        Vec3 size = new Vec3(0.7600950118764845, (double)(r[3] - r[1]) / 16.0 / 0.528763358778626, (double)(r[2] - r[0]) / 16.0 / 0.056);
        SpatialAppearance layout = SpatialLayouts.current(StationKind.CHISELED_BOOKSHELF);
        SpatialAppearance.Frame frame = SpatialAppearance.Frame.of(StationKind.CHISELED_BOOKSHELF, pos, facing, facing);
        size = size.m_82490_((double)layout.scale(slot));
        Vec3 side = ShelfBooks.out(facing);
        Vec3 right = layout.axis(frame, slot, true);
        Vec3 up = layout.axis(frame, slot, false);
        Vec3 origin = ShelfBooks.spine(pos, facing, slot).m_82549_(side.m_82490_(out *= (double)layout.scale(slot))).m_82546_(right.m_82490_(0.016 * size.f_82481_));
        return new Pose(origin, ShelfBooks.basis(side.m_82490_(-1.0), up, right), size, 1.0);
    }

    static OpenBook.Look look(int slot) {
        int[] r = SPINES[slot];
        float column = r[0] + 1;
        return new OpenBook.Look(TEXTURE, (column + 0.3f) / 16.0f, (float)r[1] / 16.0f, (column + 0.7f) / 16.0f, (float)r[3] / 16.0f, (float)r[0] / 16.0f, (float)r[1] / 16.0f, (float)r[2] / 16.0f, (float)r[3] / 16.0f);
    }

    static Pose reading(CameraRig.Pose camera, double distance, double drop, double scale) {
        Vec3 forward = Vec3.m_82498_((float)camera.pitch(), (float)camera.yaw());
        Vec3 right = forward.m_82537_(UP).m_82541_();
        Vec3 up = right.m_82537_(forward).m_82541_();
        return new Pose(camera.pos().m_82549_(forward.m_82490_(distance)).m_82549_(up.m_82490_(-drop)), ShelfBooks.basis(right, up, forward.m_82490_(-1.0)), new Vec3(scale, scale, scale), 0.0);
    }

    static Pose held(Entity reader, float partial) {
        float f;
        if (reader instanceof LivingEntity) {
            LivingEntity living = (LivingEntity)reader;
            f = Mth.m_14189_((float)partial, (float)living.f_20884_, (float)living.f_20883_);
        } else {
            f = reader.m_5675_(partial);
        }
        float yaw = f;
        Vec3 forward = Vec3.m_82498_((float)0.0f, (float)yaw);
        Vec3 right = forward.m_82537_(UP).m_82541_();
        double tilt = Math.toRadians(52.0);
        Vec3 n = UP.m_82490_(Math.sin(tilt)).m_82546_(forward.m_82490_(Math.cos(tilt)));
        Vec3 s = UP.m_82490_(Math.cos(tilt)).m_82549_(forward.m_82490_(Math.sin(tilt)));
        Vec3 origin = reader.m_20299_(partial).m_82520_(0.0, reader.m_6047_() ? -0.4 : -0.5, 0.0).m_82549_(forward.m_82490_(0.34));
        return new Pose(origin, ShelfBooks.basis(right, s, n), new Vec3(0.5, 0.5, 0.5), 0.0);
    }

    static Pose atChest(Entity who, BlockPos pos, Direction facing, int slot, float partial) {
        Pose slotted = ShelfBooks.inSlot(pos, facing, slot, 0.37);
        Vec3 chest = who.m_20299_(partial).m_82520_(0.0, -0.55, 0.0).m_82549_(Vec3.m_82498_((float)0.0f, (float)who.m_5675_(partial)).m_82490_(0.25));
        return new Pose(chest, slotted.turn(), slotted.size().m_82490_(0.6), 1.0);
    }

    static void draw(PoseStack pose, MultiBufferSource buffers, Vec3 cam, int slot, ItemStack stack, Pose where, OpenBook book, int light) {
        book.look = ShelfBooks.look(slot);
        where.apply(book);
        if (book.spread < 0) {
            book.openAt(0);
        }
        book.render(pose, buffers, cam, light);
        if (stack.m_150930_(Items.f_42690_) && Fx.RANDOM.m_188500_() < Scene.frameDt * 6.0) {
            Fx.flakes(where.at(0.21050000000000002, 0.0, 0.032), 0.05, 1);
        }
    }

    static void fly(Flight flight) {
        FLIGHTS.removeIf(f -> f.pos.equals((Object)flight.pos) && f.slot == flight.slot);
        FLIGHTS.add(flight);
    }

    @Nullable
    static Flight flight(BlockPos pos, int slot) {
        for (Flight f : FLIGHTS) {
            if (!f.pos.equals((Object)pos) || f.slot != slot) continue;
            return f;
        }
        return null;
    }

    static void keepEmpty(BlockPos pos, int slots) {
        if (slots == 0) {
            LOCAL.remove(pos);
        } else {
            LOCAL.put(pos.m_7949_(), slots);
        }
    }

    static void cue(MenuFxPayload payload) {
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91073_ == null || mc.f_91074_ == null || payload.entityId() == mc.f_91074_.m_19879_()) {
            return;
        }
        BlockState state = mc.f_91073_.m_8055_(payload.pos());
        int slot = payload.variant();
        if (!state.m_60713_(Blocks.f_244299_) || slot < 0 || slot >= SPINES.length) {
            return;
        }
        Direction facing = (Direction)state.m_61143_((Property)HorizontalDirectionalBlock.f_54117_);
        BlockPos pos = payload.pos();
        int id = payload.entityId();
        Supplier<Pose> chest = () -> {
            Entity who = mc.f_91073_.m_6815_(id);
            return who == null ? ShelfBooks.inSlot(pos, facing, slot, 0.87).scaled(0.6) : ShelfBooks.atChest(who, pos, facing, slot, 0.0f);
        };
        Supplier<Pose> out = () -> ShelfBooks.inSlot(pos, facing, slot, 0.37);
        Supplier<Pose> home = () -> ShelfBooks.inSlot(pos, facing, slot, 0.0);
        switch (payload.cue()) {
            case PULL: {
                Flight was = ShelfBooks.flight(pos, slot);
                if (was != null && was.held) {
                    Pose from = was.pose();
                    ShelfBooks.fly(new Flight(pos, facing, slot, payload.result(), id, List.of(new Key(0.0, () -> from), new Key(0.3, () -> from.shut(1.0)), new Key(0.7, () -> ((Pose)chest.get()).scaled(0.2))), false));
                    break;
                }
                ShelfBooks.fly(new Flight(pos, facing, slot, payload.result(), id, List.of(new Key(0.0, home), new Key(0.3, out), new Key(0.75, () -> ((Pose)chest.get()).scaled(0.2))), false));
                break;
            }
            case PUT: {
                ShelfBooks.fly(new Flight(pos, facing, slot, payload.result(), id, List.of(new Key(0.0, () -> ((Pose)chest.get()).scaled(0.2)), new Key(0.4, out), new Key(0.75, home)), true));
                break;
            }
            case READ: {
                Supplier<Pose> hands = () -> {
                    Entity who = mc.f_91073_.m_6815_(id);
                    return who == null ? ShelfBooks.inSlot(pos, facing, slot, 0.77).shut(0.0) : ShelfBooks.held(who, mc.m_91296_());
                };
                Flight reading = new Flight(pos, facing, slot, payload.result(), id, List.of(new Key(0.0, home), new Key(0.3, out), new Key(0.8, () -> ((Pose)hands.get()).shut(1.0)), new Key(1.2, hands)), false);
                reading.held = true;
                ShelfBooks.fly(reading);
                break;
            }
            case READ_END: {
                Flight was = ShelfBooks.flight(pos, slot);
                Pose from = was != null ? was.pose() : ShelfBooks.inSlot(pos, facing, slot, 0.37);
                ShelfBooks.fly(new Flight(pos, facing, slot, payload.result(), id, List.of(new Key(0.0, () -> from), new Key(0.35, () -> from.shut(1.0)), new Key(0.8, out), new Key(1.1, home)), true));
                break;
            }
        }
    }

    public static void renderAll(PoseStack pose, MultiBufferSource buffers, Camera camera, Frustum frustum, ClientLevel level) {
        Vec3 cam = camera.m_90583_();
        ShelfStation mine = Scene.station(ShelfStation.class);
        if (mine != null) {
            mine.renderBooks(pose, buffers, camera, level);
        }
        if (FLIGHTS.isEmpty() && LOCAL.isEmpty() && HIDDEN.isEmpty()) {
            return;
        }
        HashMap<BlockPos, Integer> masks = new HashMap<BlockPos, Integer>(LOCAL);
        Iterator<Flight> it = FLIGHTS.iterator();
        while (it.hasNext()) {
            boolean done;
            boolean occupied;
            Flight f = it.next();
            BlockState state = level.m_8055_(f.pos);
            double t = f.time();
            boolean bl = occupied = state.m_60713_(Blocks.f_244299_) && (Boolean)state.m_61143_((Property)ChiseledBookShelfBlock.f_260698_.get(f.slot)) != false;
            boolean bl2 = !state.m_60713_(Blocks.f_244299_) || (f.intoSlot ? t > f.length() + 0.3 && occupied || t > f.length() + 1.5 : !f.held && t > f.length() && (!occupied || t > f.length() + 1.5)) ? true : (done = false);
            if (done) {
                it.remove();
                continue;
            }
            if (!f.intoSlot || t < f.length()) {
                masks.merge(f.pos, 1 << f.slot, (x, y) -> x | y);
            }
            if (!f.intoSlot && !f.held && !(t < f.length()) || !frustum.m_113029_(new AABB(f.pos).m_82400_(1.5))) continue;
            int light = LevelRenderer.m_109541_((BlockAndTintGetter)level, (BlockPos)f.pos.m_121945_(f.facing));
            ShelfBooks.draw(pose, buffers, cam, f.slot, f.book, f.pose(), f.open, light);
        }
        ShelfBooks.applyMasks(masks);
    }

    private static void applyMasks(Map<BlockPos, Integer> masks) {
        Minecraft mc = Minecraft.m_91087_();
        ArrayList<BlockPos> changed = new ArrayList<BlockPos>();
        for (Map.Entry<BlockPos, Integer> e : masks.entrySet()) {
            Integer was = HIDDEN.get(e.getKey());
            if (was != null && was.equals(e.getValue())) continue;
            changed.add(e.getKey());
        }
        for (BlockPos pos : HIDDEN.keySet()) {
            if (masks.containsKey(pos)) continue;
            changed.add(pos);
        }
        for (BlockPos pos : changed) {
            Integer mask = masks.get(pos);
            if (mask == null || mask == 0) {
                HIDDEN.remove(pos);
            } else {
                HIDDEN.put(pos, mask);
            }
            mc.f_91060_.m_109770_(SectionPos.m_123171_((int)pos.m_123341_()), SectionPos.m_123171_((int)pos.m_123342_()), SectionPos.m_123171_((int)pos.m_123343_()));
        }
    }

    public static boolean reading(int entityId) {
        for (Flight f : FLIGHTS) {
            if (!f.held || f.entityId != entityId) continue;
            return true;
        }
        return false;
    }

    static void forgetEntity(int entityId) {
        FLIGHTS.removeIf(f -> f.entityId == entityId && f.held);
    }

    static void unloadChunk(ChunkPos chunk) {
        ShelfLayoutBooks.unloadChunk(chunk);
        FLIGHTS.removeIf(f -> f.pos.m_123341_() >> 4 == chunk.f_45578_ && f.pos.m_123343_() >> 4 == chunk.f_45579_);
    }

    static void clear() {
        ShelfLayoutBooks.clear();
        FLIGHTS.clear();
        LOCAL.clear();
        HIDDEN.clear();
    }

    public static void emptySlots(ModelEvent.ModifyBakingResult event) {
        Map models = event.getModels();
        IdentityHashMap<BakedModel, BakedModel> wrapped = new IdentityHashMap<BakedModel, BakedModel>();
        for (BlockState state : Blocks.f_244299_.m_49965_().m_61056_()) {
            ModelResourceLocation key = BlockModelShaper.m_110895_((BlockState)state);
            BakedModel model = (BakedModel)models.get(key);
            if (model == null) continue;
            models.put(key, wrapped.computeIfAbsent(model, Emptied::new));
        }
    }

    record Pose(Vec3 origin, Quaternionf turn, Vec3 size, double fold) {
        Pose to(Pose other, double k) {
            return new Pose(Anim.lerp(this.origin, other.origin, k), new Quaternionf((Quaternionfc)this.turn).slerp((Quaternionfc)other.turn, (float)k), Anim.lerp(this.size, other.size, k), Mth.m_14139_((double)k, (double)this.fold, (double)other.fold));
        }

        Pose moved(Vec3 by) {
            return new Pose(this.origin.m_82549_(by), this.turn, this.size, this.fold);
        }

        Pose scaled(double k) {
            return new Pose(this.origin, this.turn, this.size.m_82490_(k), this.fold);
        }

        Pose shut(double fold) {
            return new Pose(this.origin, this.turn, this.size, fold);
        }

        Vec3 axis(float x, float y, float z, double length) {
            Vector3f v = this.turn.transform(new Vector3f(x, y, z));
            return new Vec3((double)v.x(), (double)v.y(), (double)v.z()).m_82490_(length);
        }

        void apply(OpenBook book) {
            book.place(this.origin, this.axis(1.0f, 0.0f, 0.0f, this.size.f_82479_), this.axis(0.0f, 1.0f, 0.0f, this.size.f_82480_), this.axis(0.0f, 0.0f, 1.0f, this.size.f_82481_));
            book.fold = this.fold;
            book.closing = this.fold > 0.001;
        }

        Vec3 at(double x, double v, double h) {
            return this.origin.m_82549_(this.axis(1.0f, 0.0f, 0.0f, this.size.f_82479_ * x)).m_82549_(this.axis(0.0f, 1.0f, 0.0f, this.size.f_82480_ * v)).m_82549_(this.axis(0.0f, 0.0f, 1.0f, this.size.f_82481_ * h));
        }
    }

    static final class Flight {
        final BlockPos pos;
        final Direction facing;
        final int slot;
        final ItemStack book;
        final int entityId;
        final double at;
        final List<Key> keys;
        final boolean intoSlot;
        boolean held;
        final OpenBook open = new OpenBook();

        Flight(BlockPos pos, Direction facing, int slot, ItemStack book, int entityId, List<Key> keys, boolean intoSlot) {
            this.pos = pos.m_7949_();
            this.facing = facing;
            this.slot = slot;
            this.book = book;
            this.entityId = entityId;
            this.at = Scene.clock();
            this.keys = keys;
            this.intoSlot = intoSlot;
        }

        double length() {
            return this.keys.get(this.keys.size() - 1).t();
        }

        double time() {
            return Scene.clock() - this.at;
        }

        Pose pose() {
            double t = this.time();
            for (int i = 1; i < this.keys.size(); ++i) {
                Key next = this.keys.get(i);
                if (!(t < next.t())) continue;
                Key from = this.keys.get(i - 1);
                double k = Anim.inOutCubic(Anim.window(t, from.t(), next.t()));
                return from.pose().get().to(next.pose().get(), k);
            }
            return this.keys.get(this.keys.size() - 1).pose().get();
        }
    }

    record Key(double t, Supplier<Pose> pose) {
    }

    private static final class Emptied
    extends BakedModelWrapper<BakedModel> {
        Emptied(BakedModel model) {
            super(model);
        }

        public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
            ModelData own = this.originalModel.getModelData(level, pos, state, data);
            if (!ClientOnly.serverHasMod()) {
                return own;
            }
            Integer mask = HIDDEN.get(pos);
            if (SpatialLayouts.customShelf()) {
                mask = 63;
            }
            return mask == null ? own : own.derive().with(EMPTIED, (Object)mask).build();
        }

        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
            if (!ClientOnly.serverHasMod()) {
                return this.originalModel.getQuads(state, side, rand, data, renderType);
            }
            Integer mask = (Integer)data.get(EMPTIED);
            if (mask != null && state != null && state.m_60713_(Blocks.f_244299_)) {
                for (int i = 0; i < SPINES.length; ++i) {
                    if ((mask >> i & 1) == 0) continue;
                    state = (BlockState)state.m_61124_((Property)ChiseledBookShelfBlock.f_260698_.get(i), (Comparable)Boolean.valueOf(false));
                }
            }
            return this.originalModel.getQuads(state, side, rand, data, renderType);
        }
    }
}


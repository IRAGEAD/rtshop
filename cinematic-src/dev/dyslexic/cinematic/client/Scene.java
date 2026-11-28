/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.client.CameraType
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.player.Input
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Vec3i
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.util.Mth
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.ClipContext
 *  net.minecraft.world.level.ClipContext$Block
 *  net.minecraft.world.level.ClipContext$Fluid
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.AbstractFurnaceBlock
 *  net.minecraft.world.level.block.HorizontalDirectionalBlock
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.minecraft.world.phys.Vec3
 */
package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.Cinematic;
import dev.dyslexic.cinematic.ClientBridge;
import dev.dyslexic.cinematic.api.AddonStations;
import dev.dyslexic.cinematic.api.client.AddonScenes;
import dev.dyslexic.cinematic.client.Anim;
import dev.dyslexic.cinematic.client.AnvilStation;
import dev.dyslexic.cinematic.client.BeaconStation;
import dev.dyslexic.cinematic.client.BrewingStation;
import dev.dyslexic.cinematic.client.CameraRig;
import dev.dyslexic.cinematic.client.CampfireStation;
import dev.dyslexic.cinematic.client.CartographyStation;
import dev.dyslexic.cinematic.client.ClientNet;
import dev.dyslexic.cinematic.client.ClientStation;
import dev.dyslexic.cinematic.client.EnchantStation;
import dev.dyslexic.cinematic.client.FreeLook;
import dev.dyslexic.cinematic.client.FurnaceLayouts;
import dev.dyslexic.cinematic.client.FurnaceStation;
import dev.dyslexic.cinematic.client.GrindstoneStation;
import dev.dyslexic.cinematic.client.Hands;
import dev.dyslexic.cinematic.client.JukeboxStation;
import dev.dyslexic.cinematic.client.LecternStation;
import dev.dyslexic.cinematic.client.LoomStation;
import dev.dyslexic.cinematic.client.Picking;
import dev.dyslexic.cinematic.client.Reach;
import dev.dyslexic.cinematic.client.ReachNotice;
import dev.dyslexic.cinematic.client.SceneScreen;
import dev.dyslexic.cinematic.client.Settings;
import dev.dyslexic.cinematic.client.ShelfStation;
import dev.dyslexic.cinematic.client.SmithingStation;
import dev.dyslexic.cinematic.client.SpatialLayouts;
import dev.dyslexic.cinematic.client.StonecutterStation;
import dev.dyslexic.cinematic.client.SurfaceLayouts;
import dev.dyslexic.cinematic.client.TableLayouts;
import dev.dyslexic.cinematic.client.TableStation;
import dev.dyslexic.cinematic.net.StationActionPayload;
import dev.dyslexic.cinematic.station.MenuActions;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class Scene {
    public static final double OUTRO_LENGTH = 1.05;
    public static final double LEAN_LENGTH = 1.2;
    static final double QUICK_LEAN_LENGTH = 0.4;
    private static double leanLength = 1.2;
    static boolean quick;
    @Nullable
    private static BlockPos pendingPos;
    @Nullable
    private static StationKind pendingKind;
    private static double pendingAt;
    @Nullable
    private static BlockPos landingPos;
    @Nullable
    private static StationKind landingKind;
    private static int landingTicks;
    private static final int LANDING_WAIT = 30;
    @Nullable
    private static BlockPos leftPos;
    private static double leftAt;
    static final double WALK_SHARE = 0.3;
    private static final double STEP_BACK = 0.9;
    public static final double UI_FADE_OUT = 0.22;
    public static Phase phase;
    @Nullable
    public static ClientStation station;
    public static double time;
    public static double introLength;
    public static double walkShare;
    private static double arrivedAt;
    private static double walkEstimate;
    private static boolean crouch;
    private static boolean stepBack;
    private static double outroSeconds;
    private static double bestDistance;
    private static double progressAt;
    public static String refused;
    public static double frameDt;
    private static double clock;
    private static long lastNanos;
    private static final List<Vec3> path;
    private static int waypoint;
    private static boolean leaveSent;
    private static boolean clientOnly;
    private static int clientMenuId;
    @Nullable
    private static CameraType savedCamera;
    @Nullable
    static CameraRig.Pose outroFrom;
    static double outroWeight;
    private static int lastHurtTime;
    public static boolean overPanel;
    private static boolean cursorEngaged;
    private static double mouseStartX;
    private static double mouseStartY;
    static boolean consulting;
    @Nullable
    public static BlockPos neighbor;
    @Nullable
    public static StationKind neighborKind;
    static boolean transition;
    @Nullable
    static CameraRig.Pose transitionFrom;
    private static final double NEIGHBOR_REACH = 4.5;
    private static final Set<StationKind> OWN_WINDOW;
    private static final Set<ResourceLocation> OWN_ADDON_WINDOWS;
    private static boolean awaitingMenu;
    @Nullable
    private static ClientStation released;
    private static double releasedAt;
    private static final double LATE_ANSWER = 5.0;

    private Scene() {
    }

    public static double clock() {
        return clock;
    }

    public static boolean active() {
        return phase != Phase.NONE && station != null;
    }

    static void consult(Runnable open) {
        if (phase != Phase.ACTIVE) {
            return;
        }
        consulting = true;
        open.run();
        if (Minecraft.m_91087_().f_91080_ instanceof SceneScreen) {
            consulting = false;
        }
    }

    static void consulted() {
        if (!consulting) {
            return;
        }
        consulting = false;
        if (phase == Phase.ACTIVE && !Scene.stillAt()) {
            Scene.leave();
        }
    }

    private static boolean stillAt() {
        Minecraft mc = Minecraft.m_91087_();
        LocalPlayer player = mc.f_91074_;
        if (player == null || mc.f_91073_ == null || station == null || !player.m_6084_()) {
            return false;
        }
        return StationKind.of(mc.f_91073_.m_8055_(Scene.station.pos)) == station.kind() && player.m_20182_().m_82554_(Vec3.m_82512_((Vec3i)Scene.station.pos)) < 3.5;
    }

    public static boolean clientOnly() {
        return clientOnly && Scene.active();
    }

    public static double introProgress() {
        if (phase != Phase.INTRO) {
            return phase == Phase.NONE ? 0.0 : 1.0;
        }
        if (arrivedAt < 0.0) {
            return 0.3 * Math.min(0.98, time / Math.max(0.2, walkEstimate));
        }
        return walkShare + (1.0 - walkShare) * Anim.clamp01((time - arrivedAt) / leanLength);
    }

    public static boolean arrived() {
        return phase != Phase.INTRO || arrivedAt >= 0.0;
    }

    public static double introSecondsUntil(double u) {
        if (phase != Phase.INTRO || arrivedAt < 0.0) {
            return 0.0;
        }
        return Math.max(0.0, arrivedAt + (u - walkShare) / (1.0 - walkShare) * leanLength - time) / Settings.approachSpeed();
    }

    @Nullable
    public static <T extends ClientStation> T station(Class<T> type) {
        return (T)(Scene.active() && type.isInstance(station) ? (ClientStation)type.cast(station) : null);
    }

    public static void begin(BlockPos pos, StationKind kind) {
        AddonStations.Definition addonDefinition;
        Minecraft mc = Minecraft.m_91087_();
        LocalPlayer player = mc.f_91074_;
        if (phase == Phase.OUTRO && player != null) {
            pendingPos = pos.m_7949_();
            pendingKind = kind;
            pendingAt = clock;
            return;
        }
        if (phase != Phase.NONE || player == null || mc.f_91073_ == null) {
            return;
        }
        BlockState currentState = mc.f_91073_.m_8055_(pos);
        AddonStations.Definition definition = addonDefinition = kind == StationKind.ADDON ? AddonStations.forBlock(currentState) : null;
        if (!Settings.scene(kind) || OWN_WINDOW.contains((Object)kind) || addonDefinition != null && OWN_ADDON_WINDOWS.contains(addonDefinition.id())) {
            if (kind != StationKind.CAMPFIRE) {
                Scene.openWindow(pos, kind);
            }
            return;
        }
        if (kind == StationKind.ADDON && !AddonScenes.available(currentState)) {
            Scene.openWindow(pos, kind);
            return;
        }
        if (kind == StationKind.JUKEBOX && !JukeboxStation.worthIt(player, pos)) {
            return;
        }
        if (Scene.airborne(player)) {
            landingPos = pos.m_7949_();
            landingKind = kind;
            landingTicks = 0;
            return;
        }
        Choice choice = Scene.choose(pos, kind, player);
        refused = choice.plan().why();
        if (!choice.plan().scene()) {
            Cinematic.LOGGER.info("[Cinematic] {} at {}: no scene ({})", new Object[]{kind, pos.m_123344_(), choice.plan().why()});
            Scene.refuse(choice.station(), choice.plan().reason(), choice.plan().blocker());
            return;
        }
        station = choice.station();
        clientOnly = false;
        transition = false;
        transitionFrom = null;
        quick = Settings.quickMode() == 2 || Settings.quickMode() == 1 && pos.equals((Object)leftPos) && clock - leftAt <= (double)Settings.quickWindow();
        leanLength = quick ? 0.4 : 1.2;
        savedCamera = mc.f_91066_.m_92176_();
        if (!savedCamera.m_90612_()) {
            mc.f_91066_.m_92157_(CameraType.FIRST_PERSON);
        }
        CameraRig.reset();
        Hands.begin();
        Scene.startIntro(choice.plan(), player);
        Scene.sound(SoundEvents.f_11678_, 0.35f, 1.1f);
    }

    public static boolean beginClientOnly(BlockPos pos, StationKind kind) {
        Minecraft mc = Minecraft.m_91087_();
        LocalPlayer player = mc.f_91074_;
        if (phase != Phase.NONE || player == null || mc.f_91073_ == null || mc.f_91072_ == null) {
            return false;
        }
        ClientStation candidate = Scene.create(kind, pos, mc.f_91073_.m_8055_(pos), player, ClientStation.freeSide((Level)mc.f_91073_, pos, player, kind));
        Reach.Plan here = Reach.planHere(candidate, player);
        if (!here.scene()) {
            refused = here.why();
            Cinematic.LOGGER.info("[Cinematic] client-only {} at {}: vanilla window ({})", new Object[]{kind, pos.m_123344_(), here.why()});
            return false;
        }
        station = candidate;
        clientOnly = true;
        refused = "";
        clientMenuId = player.f_36096_.f_38840_;
        transition = false;
        transitionFrom = null;
        quick = Settings.quickMode() == 2;
        leanLength = quick ? 0.4 : 1.2;
        savedCamera = mc.f_91066_.m_92176_();
        if (!savedCamera.m_90612_()) {
            mc.f_91066_.m_92157_(CameraType.FIRST_PERSON);
        }
        CameraRig.reset();
        Hands.begin();
        Scene.startIntro(here, player);
        Scene.sound(SoundEvents.f_11678_, 0.35f, 1.1f);
        return true;
    }

    private static Choice choose(BlockPos pos, StationKind kind, LocalPlayer player) {
        Level level = player.m_9236_();
        BlockState state = level.m_8055_(pos);
        ClientStation first = Scene.create(kind, pos, state, player, ClientStation.freeSide(level, pos, player, kind));
        Reach.Plan plan = Reach.plan(first, player);
        if (plan.scene() || kind == StationKind.CAMPFIRE) {
            return new Choice(first, plan);
        }
        List<Direction> sides = ClientStation.sidesByNearness(level, pos, player, kind);
        for (boolean beside : new boolean[]{false, true}) {
            for (Direction side : sides) {
                if (!beside && side == first.sideDirection()) continue;
                ClientStation other = Scene.create(kind, pos, state, player, side);
                if (beside && !other.worksAtAnAngle()) {
                    return new Choice(first, plan);
                }
                Reach.Plan otherPlan = beside ? Reach.planBeside(other, player) : Reach.plan(other, player);
                if (!otherPlan.scene()) continue;
                return new Choice(other, otherPlan);
            }
        }
        return new Choice(first, plan);
    }

    private static void startIntro(Reach.Plan plan, LocalPlayer player) {
        path.clear();
        path.addAll(plan.path());
        waypoint = 0;
        double distance = 0.0;
        Vec3 at = player.m_20182_();
        for (Vec3 point : path) {
            distance += Math.hypot(point.f_82479_ - at.f_82479_, point.f_82481_ - at.f_82481_);
            at = point;
        }
        walkEstimate = path.isEmpty() ? 0.0 : 0.25 + distance / 3.6;
        introLength = walkEstimate + leanLength;
        arrivedAt = path.isEmpty() ? 0.0 : -1.0;
        walkShare = path.isEmpty() ? 0.0 : 1.0;
        bestDistance = Double.MAX_VALUE;
        progressAt = 0.0;
        crouch = !clientOnly && station.crouchesFor(Scene.station.view().pos().f_82480_ - plan.feet().f_82480_);
        phase = Phase.INTRO;
        time = 0.0;
        leaveSent = false;
        outroFrom = null;
        neighbor = null;
        lastHurtTime = player.f_20916_;
        if (!clientOnly) {
            player.m_6858_(false);
        }
        boolean bl = awaitingMenu = !clientOnly && station.kind().usesMenu();
        if (!clientOnly) {
            ClientNet.sendToServer(new StationActionPayload(Scene.station.pos, station.kind(), StationActionPayload.Action.BEGIN, 0, 0, false, station.sideDirection().m_122416_()));
        }
    }

    private static void findNeighbor() {
        neighbor = null;
        neighborKind = null;
        if (clientOnly) {
            return;
        }
        Minecraft mc = Minecraft.m_91087_();
        if (Scene.station.aim != null || overPanel || station.busy() || mc.f_91073_ == null || mc.f_91074_ == null) {
            return;
        }
        Vec3[] ray = Picking.cursorRay();
        if (ray == null) {
            return;
        }
        Vec3 eye = mc.f_91074_.m_146892_();
        BlockHitResult hit = mc.f_91073_.m_45547_(new ClipContext(ray[0], ray[0].m_82549_(ray[1].m_82490_(6.5)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, (Entity)mc.f_91074_));
        if (hit.m_6662_() != HitResult.Type.BLOCK || hit.m_82425_().equals((Object)Scene.station.pos) || hit.m_82425_().m_252807_().m_82554_(eye) > 4.5) {
            return;
        }
        BlockState state = mc.f_91073_.m_8055_(hit.m_82425_());
        StationKind kind = StationKind.of(state);
        if (kind == null || !kind.startsOnUse() || !Settings.scene(kind) || !kind.usable((BlockGetter)mc.f_91073_, hit.m_82425_(), state) || kind == StationKind.ADDON && !AddonScenes.available(state)) {
            return;
        }
        neighbor = hit.m_82425_().m_7949_();
        neighborKind = kind;
    }

    public static void moveOver() {
        if (clientOnly) {
            return;
        }
        Minecraft mc = Minecraft.m_91087_();
        LocalPlayer player = mc.f_91074_;
        if (phase != Phase.ACTIVE || neighbor == null || neighborKind == null || player == null || station.busy()) {
            return;
        }
        if (neighborKind == StationKind.ADDON && !AddonScenes.available(mc.f_91073_.m_8055_(neighbor))) {
            return;
        }
        Choice choice = Scene.choose(neighbor, neighborKind, player);
        if (!choice.plan().scene()) {
            Cinematic.LOGGER.info("[Cinematic] {} at {}: cannot move over ({})", new Object[]{neighborKind, neighbor.m_123344_(), choice.plan().why()});
            if (choice.plan().reason() != null) {
                ReachNotice.show(choice.plan().reason(), choice.plan().blocker());
            }
            return;
        }
        ClientStation old = station;
        transitionFrom = CameraRig.last();
        old.clearCursor();
        old.onEnd();
        player.f_36095_.m_142503_(ItemStack.f_41583_);
        ClientNet.sendToServer(StationActionPayload.of(old.pos, old.kind(), StationActionPayload.Action.LEAVE));
        released = old;
        releasedAt = clock;
        Hands.beginTransition();
        station = choice.station();
        transition = true;
        quick = Settings.quickMode() == 2;
        leanLength = quick ? 0.4 : 1.2;
        refused = "";
        Scene.startIntro(choice.plan(), player);
        Scene.sound(SoundEvents.f_11678_, 0.22f, 1.25f);
    }

    static double released() {
        return transition && walkShare > 0.0 ? Anim.smoother(phase == Phase.INTRO && arrivedAt < 0.0 ? time / 0.45 : 1.0) : 0.0;
    }

    private static ClientStation create(StationKind kind, BlockPos pos, BlockState state, LocalPlayer player, Direction side) {
        return switch (kind) {
            default -> throw new IncompatibleClassChangeError();
            case StationKind.TABLE -> new TableStation(pos, side);
            case StationKind.FURNACE, StationKind.SMOKER, StationKind.BLAST_FURNACE -> new FurnaceStation(pos, (Direction)state.m_61143_((Property)AbstractFurnaceBlock.f_48683_), kind);
            case StationKind.ENCHANTING -> new EnchantStation(pos, side);
            case StationKind.ANVIL -> new AnvilStation(pos, side);
            case StationKind.GRINDSTONE -> new GrindstoneStation(pos, side, state);
            case StationKind.SMITHING -> new SmithingStation(pos, side);
            case StationKind.STONECUTTER -> new StonecutterStation(pos, side);
            case StationKind.LOOM -> new LoomStation(pos, side);
            case StationKind.CARTOGRAPHY -> new CartographyStation(pos, side);
            case StationKind.BREWING -> new BrewingStation(pos, side);
            case StationKind.CAMPFIRE -> new CampfireStation(pos, player);
            case StationKind.LECTERN -> new LecternStation(pos, side);
            case StationKind.BEACON -> new BeaconStation(pos, side);
            case StationKind.JUKEBOX -> new JukeboxStation(pos, side);
            case StationKind.CHISELED_BOOKSHELF -> new ShelfStation(pos, side);
            case StationKind.ADDON -> AddonScenes.create(pos, state, player, side);
        };
    }

    public static void leave() {
        if (phase != Phase.INTRO && phase != Phase.ACTIVE) {
            return;
        }
        leftPos = Scene.station.pos;
        leftAt = clock;
        neighbor = null;
        outroFrom = CameraRig.last() != null ? CameraRig.last() : station.view();
        outroWeight = CameraRig.weight();
        outroSeconds = 0.0;
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        stepBack = !clientOnly && player != null && player.m_20096_() && Reach.walkable(player.m_9236_(), (Entity)player, List.of(player.m_20182_().m_82549_(Vec3.m_82498_((float)0.0f, (float)player.m_146908_()).m_82490_(-0.9))));
        Hands.beginOutro();
        phase = Phase.OUTRO;
        time = 0.0;
        station.clearCursor();
        station.onEnd();
        released = station;
        releasedAt = clock;
        Scene.sendLeave();
        Scene.sound(SoundEvents.f_11678_, 0.3f, 0.9f);
    }

    public static void serverClosed() {
        leaveSent = true;
        Scene.leave();
    }

    static MenuAnswer answer(AbstractContainerMenu menu) {
        if (phase != Phase.OUTRO && station != null && Scene.accepts(station, menu)) {
            awaitingMenu = false;
            return MenuAnswer.OURS;
        }
        if (released != null && clock - releasedAt <= 5.0 && Scene.accepts(released, menu)) {
            return MenuAnswer.LATE;
        }
        if (phase != Phase.OUTRO && awaitingMenu) {
            awaitingMenu = false;
            return MenuAnswer.FOREIGN;
        }
        return MenuAnswer.UNRELATED;
    }

    private static boolean accepts(ClientStation at, AbstractContainerMenu menu) {
        if (at.kind() == StationKind.ADDON && Minecraft.m_91087_().f_91073_ != null) {
            AddonStations.Definition definition = AddonStations.forBlock(Minecraft.m_91087_().f_91073_.m_8055_(at.pos));
            return definition != null && definition.accepts(menu);
        }
        return at.kind().usesMenu() && MenuActions.isOpen(at.kind(), menu);
    }

    static void yieldToWindow() {
        if (phase == Phase.NONE || station == null) {
            return;
        }
        if (station.kind() == StationKind.ADDON && Minecraft.m_91087_().f_91073_ != null) {
            AddonStations.Definition definition = AddonStations.forBlock(Minecraft.m_91087_().f_91073_.m_8055_(Scene.station.pos));
            if (definition != null) {
                OWN_ADDON_WINDOWS.add(definition.id());
            }
        } else if (station.kind() != StationKind.LECTERN) {
            OWN_WINDOW.add(station.kind());
        }
        Cinematic.LOGGER.info("[Cinematic] {} opened a menu of its own ({}): its window instead of the scene", (Object)station.kind(), (Object)(Minecraft.m_91087_().f_91074_ == null ? "-" : Minecraft.m_91087_().f_91074_.f_36096_.getClass().getName()));
        leaveSent = true;
        Scene.finish(false);
    }

    public static void forgetOwnWindows() {
        OWN_WINDOW.clear();
        OWN_ADDON_WINDOWS.clear();
        awaitingMenu = false;
        released = null;
        landingPos = null;
        landingKind = null;
    }

    public static void abort() {
        if (phase == Phase.NONE) {
            return;
        }
        Scene.sendLeave();
        Scene.finish(true);
    }

    private static void fallBack() {
        ClientStation refusedAt = station;
        leaveSent = true;
        Scene.finish(true);
        if (refusedAt != null && Minecraft.m_91087_().m_91403_() != null) {
            Scene.refuse(refusedAt, Reach.Reason.BLOCKED, null);
        }
    }

    private static void refuse(ClientStation at, @Nullable Reach.Reason reason, @Nullable Component blocker) {
        boolean window;
        boolean bl = window = at.kind() != StationKind.CAMPFIRE;
        if (window && Settings.heightLogic()) {
            Scene.openWindow(at.pos, at.kind());
        } else if (reason != null) {
            ReachNotice.show(reason, blocker);
        }
    }

    private static void openWindow(BlockPos pos, StationKind kind) {
        ClientNet.sendToServer(Scene.window(pos, kind));
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        if (player != null) {
            player.m_6674_(InteractionHand.MAIN_HAND);
        }
    }

    private static StationActionPayload window(BlockPos pos, StationKind kind) {
        int slot = kind == StationKind.CHISELED_BOOKSHELF ? ShelfStation.slotAt(pos, (Direction)Minecraft.m_91087_().f_91073_.m_8055_(pos).m_61143_((Property)HorizontalDirectionalBlock.f_54117_), ClientBridge.useAt) : 0;
        return new StationActionPayload(pos, kind, StationActionPayload.Action.WINDOW, slot, 0, false, 0);
    }

    public static void drop() {
        if (phase == Phase.NONE) {
            return;
        }
        Scene.sendLeave();
        Scene.finish(false);
    }

    private static void finish(boolean closeScreen) {
        Minecraft mc = Minecraft.m_91087_();
        awaitingMenu = false;
        FreeLook.reset();
        transition = false;
        transitionFrom = null;
        neighbor = null;
        consulting = false;
        if (phase != Phase.OUTRO && station != null) {
            station.onEnd();
        }
        phase = Phase.NONE;
        if (closeScreen && mc.f_91080_ instanceof SceneScreen) {
            mc.m_91152_(null);
        }
        if (savedCamera != null && !savedCamera.m_90612_()) {
            mc.f_91066_.m_92157_(savedCamera);
        }
        savedCamera = null;
        station = null;
        clientOnly = false;
        clientMenuId = -1;
        outroFrom = null;
        overPanel = false;
        path.clear();
        CameraRig.reset();
        BlockPos next = pendingPos;
        StationKind nextKind = pendingKind;
        pendingPos = null;
        pendingKind = null;
        if (next != null && nextKind != null && clock - pendingAt < 2.0 && mc.f_91073_ != null && nextKind.matches(mc.f_91073_.m_8055_(next))) {
            Scene.begin(next, nextKind);
        }
    }

    private static void sendLeave() {
        if (clientOnly) {
            if (!leaveSent) {
                leaveSent = true;
                LocalPlayer player = Minecraft.m_91087_().f_91074_;
                if (player != null && player.f_36096_ != player.f_36095_ && player.f_36096_.f_38840_ == clientMenuId) {
                    player.m_6915_();
                }
            }
            return;
        }
        if (!leaveSent && station != null && Minecraft.m_91087_().m_91403_() != null) {
            ClientNet.sendToServer(StationActionPayload.of(Scene.station.pos, station.kind(), StationActionPayload.Action.LEAVE));
        }
        leaveSent = true;
    }

    private static void enterActive() {
        Minecraft mc = Minecraft.m_91087_();
        boolean carriedOn = transition;
        transition = false;
        transitionFrom = null;
        phase = Phase.ACTIVE;
        time = 0.0;
        Hands.enterActive();
        mc.m_91152_((Screen)new SceneScreen());
        cursorEngaged = carriedOn;
        mouseStartX = mc.f_91067_.m_91589_();
        mouseStartY = mc.f_91067_.m_91594_();
    }

    public static void frame() {
        Minecraft mc = Minecraft.m_91087_();
        long now = System.nanoTime();
        double dt = lastNanos == 0L ? 0.0 : Math.min(0.1, (double)(now - lastNanos) / 1.0E9);
        lastNanos = now;
        if (mc.m_91104_()) {
            dt = 0.0;
        }
        frameDt = dt;
        clock += dt;
        TableLayouts.frame(dt);
        FurnaceLayouts.frame(dt);
        SurfaceLayouts.frame(dt);
        SpatialLayouts.frame(dt);
        if (phase == Phase.NONE) {
            return;
        }
        if (station != null) {
            station.beforeFrame();
        }
        FreeLook.frame(dt);
        double pace = phase == Phase.INTRO && arrivedAt >= 0.0 ? Settings.approachSpeed() : (phase == Phase.OUTRO ? Settings.leaveSpeed() : 1.0);
        time += dt * pace;
        if (phase == Phase.OUTRO) {
            outroSeconds += dt;
        }
        if (phase == Phase.INTRO && arrivedAt >= 0.0 && time - arrivedAt >= leanLength) {
            Scene.enterActive();
        } else if (phase == Phase.OUTRO && time >= 1.05) {
            Scene.finish(true);
        } else if (phase == Phase.OUTRO && time >= 0.22 && mc.f_91080_ instanceof SceneScreen) {
            mc.m_91152_(null);
        }
    }

    private static boolean airborne(LocalPlayer player) {
        return !player.m_20096_() && !player.m_20069_() && !player.m_20077_() && !player.m_6147_() && !player.m_150110_().f_35935_ && !player.m_21255_() && !player.m_20159_();
    }

    private static void tickLanding(Minecraft mc) {
        boolean landed;
        BlockPos pos = landingPos;
        StationKind kind = landingKind;
        LocalPlayer player = mc.f_91074_;
        if (pos == null || kind == null) {
            return;
        }
        if (phase != Phase.NONE || player == null || mc.f_91073_ == null || !kind.matches(mc.f_91073_.m_8055_(pos)) || mc.f_91080_ != null) {
            landingPos = null;
            landingKind = null;
            return;
        }
        boolean bl = landed = !Scene.airborne(player);
        if (!landed && ++landingTicks < 30) {
            return;
        }
        landingPos = null;
        landingKind = null;
        if (player.m_20238_(pos.m_252807_()) > 36.0) {
            return;
        }
        if (landed) {
            Scene.begin(pos, kind);
        } else if (kind != StationKind.CAMPFIRE) {
            Scene.openWindow(pos, kind);
        }
    }

    public static void tick() {
        Scene.tickLanding(Minecraft.m_91087_());
        if (phase == Phase.NONE) {
            return;
        }
        Minecraft mc = Minecraft.m_91087_();
        LocalPlayer player = mc.f_91074_;
        if (player == null || mc.f_91073_ == null || station == null) {
            Scene.finish(false);
            return;
        }
        if (!player.m_6084_()) {
            Scene.abort();
            return;
        }
        if (consulting && mc.f_91080_ == null) {
            consulting = false;
            if (phase == Phase.ACTIVE && Scene.stillAt()) {
                mc.m_91152_((Screen)new SceneScreen());
            } else {
                Scene.leave();
            }
        }
        if (clientOnly && phase != Phase.OUTRO && (player.f_36096_ == player.f_36095_ || player.f_36096_.f_38840_ != clientMenuId)) {
            Scene.drop();
            return;
        }
        if (phase != Phase.OUTRO) {
            boolean falling;
            boolean gone = !station.stillThere((Level)mc.f_91073_) || player.m_20238_(Scene.station.pos.m_252807_()) > 49.0;
            boolean bl = falling = !player.m_20096_() && !player.m_20069_() && !player.m_6147_() && (double)player.f_19789_ > 0.7;
            if (gone || falling || player.f_20916_ > lastHurtTime) {
                Scene.leave();
            }
        }
        lastHurtTime = player.f_20916_;
        player.m_6858_(false);
        if (!mc.f_91066_.m_92176_().m_90612_() && !FreeLook.showsBody()) {
            mc.f_91066_.m_92157_(CameraType.FIRST_PERSON);
        }
        if (phase == Phase.ACTIVE) {
            station.tick();
        }
    }

    public static void steer(LocalPlayer player) {
        if (!Scene.active() || clientOnly) {
            return;
        }
        float yaw = station.sideDirection().m_122424_().m_122435_() + (float)Scene.station.swing;
        float pitch = switch (phase) {
            case Phase.INTRO -> station.view().pitch();
            case Phase.ACTIVE -> station.bodyPitch();
            default -> 24.0f;
        };
        float rate = phase == Phase.INTRO ? 0.25f : 0.22f;
        float newYaw = player.m_146908_() + Mth.m_14177_((float)(yaw - player.m_146908_())) * rate;
        float newPitch = player.m_146909_() + (pitch - player.m_146909_()) * rate;
        player.m_146922_(newYaw);
        player.m_146926_(newPitch);
        player.m_5616_(newYaw);
    }

    public static void movementInput(Input input) {
        boolean walked;
        if (!Scene.active()) {
            return;
        }
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        input.f_108571_ = false;
        input.f_108570_ = false;
        input.f_108569_ = false;
        input.f_108568_ = false;
        input.f_108572_ = false;
        input.f_108573_ = false;
        input.f_108567_ = 0.0f;
        input.f_108566_ = 0.0f;
        if (player == null) {
            return;
        }
        boolean bl = walked = waypoint >= path.size();
        if (phase == Phase.INTRO && !walked) {
            boolean last;
            Vec3 target = path.get(waypoint);
            double dx = target.f_82479_ - player.m_20185_();
            double dz = target.f_82481_ - player.m_20189_();
            double distance = Math.hypot(dx, dz);
            boolean bl2 = last = waypoint == path.size() - 1;
            if (last && distance < 0.3 && arrivedAt < 0.0) {
                arrivedAt = time;
                walkShare = 0.3 * Math.min(0.98, time / Math.max(0.2, walkEstimate));
                introLength = arrivedAt + leanLength;
            }
            if (distance < bestDistance - 0.02) {
                bestDistance = distance;
                progressAt = time;
            }
            if (arrivedAt < 0.0 && (time - progressAt > 0.7 || time > walkEstimate * 2.0 + 1.5)) {
                refused = "walked into something on the way";
                Cinematic.LOGGER.info("[Cinematic] {} at {}: {}, the block's own window instead", new Object[]{station.kind(), Scene.station.pos.m_123344_(), refused});
                Scene.fallBack();
                return;
            }
            double d = last ? 0.05 : 0.3;
            if (distance < d) {
                ++waypoint;
                bestDistance = Double.MAX_VALUE;
                progressAt = time;
            } else {
                double yaw = Math.toRadians(player.m_146908_());
                double sin = Math.sin(yaw);
                double cos = Math.cos(yaw);
                double forward = -dx * sin + dz * cos;
                double left = dx * cos + dz * sin;
                double strength = (last ? Math.min(1.0, distance * 2.4) : 1.0) / Math.max(distance, 1.0E-4);
                input.f_108567_ = (float)(forward * strength);
                input.f_108566_ = (float)(left * strength);
            }
        }
        if (crouch) {
            boolean settling = phase == Phase.INTRO && arrivedAt >= 0.0;
            boolean bl3 = input.f_108573_ = settling || phase == Phase.ACTIVE || phase == Phase.OUTRO && time < 0.15;
        }
        if (phase == Phase.OUTRO && stepBack && outroSeconds > 0.2 && outroSeconds < 0.6) {
            input.f_108567_ = -0.45f;
        }
    }

    public static void updateCursor() {
        if (!Scene.active()) {
            return;
        }
        if (phase != Phase.ACTIVE || FreeLook.looking()) {
            station.clearCursor();
            neighbor = null;
            return;
        }
        if (!cursorEngaged) {
            Minecraft mc = Minecraft.m_91087_();
            double moved = Math.abs(mc.f_91067_.m_91589_() - mouseStartX) + Math.abs(mc.f_91067_.m_91594_() - mouseStartY);
            if (moved < 4.0) {
                station.clearCursor();
                return;
            }
            cursorEngaged = true;
        }
        station.updateCursor(overPanel);
        Scene.findNeighbor();
    }

    static void sound(SoundEvent event, float volume, float pitch) {
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91073_ != null && station != null) {
            Vec3 at = station.soundPoint();
            mc.f_91073_.m_7785_(at.f_82479_, at.f_82480_, at.f_82481_, event, SoundSource.PLAYERS, volume, pitch, false);
        }
    }

    static Vec3 yawForward(float yaw) {
        double r = Math.toRadians(yaw);
        return new Vec3(-Math.sin(r), 0.0, Math.cos(r));
    }

    static {
        leftAt = -1000.0;
        phase = Phase.NONE;
        introLength = 1.2;
        arrivedAt = -1.0;
        refused = "";
        path = new ArrayList<Vec3>();
        clientMenuId = -1;
        outroWeight = 1.0;
        OWN_WINDOW = EnumSet.noneOf(StationKind.class);
        OWN_ADDON_WINDOWS = new HashSet<ResourceLocation>();
    }

    public static enum Phase {
        NONE,
        INTRO,
        ACTIVE,
        OUTRO;

    }

    private record Choice(ClientStation station, Reach.Plan plan) {
    }

    static enum MenuAnswer {
        OURS,
        LATE,
        FOREIGN,
        UNRELATED;

    }
}


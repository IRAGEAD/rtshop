/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.gui.screens.inventory.InventoryScreen
 *  net.minecraft.client.gui.screens.inventory.MenuAccess
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult
 *  net.minecraftforge.client.ConfigScreenHandler$ConfigScreenFactory
 *  net.minecraftforge.client.event.ClientPlayerNetworkEvent$LoggingIn
 *  net.minecraftforge.client.event.ClientPlayerNetworkEvent$LoggingOut
 *  net.minecraftforge.client.event.InputEvent$InteractionKeyMappingTriggered
 *  net.minecraftforge.client.event.InputEvent$MouseScrollingEvent
 *  net.minecraftforge.client.event.MovementInputUpdateEvent
 *  net.minecraftforge.client.event.RenderGuiEvent$Post
 *  net.minecraftforge.client.event.RenderGuiOverlayEvent$Pre
 *  net.minecraftforge.client.event.RenderHandEvent
 *  net.minecraftforge.client.event.RenderHighlightEvent$Block
 *  net.minecraftforge.client.event.RenderLevelStageEvent
 *  net.minecraftforge.client.event.RenderLevelStageEvent$Stage
 *  net.minecraftforge.client.event.RenderPlayerEvent$Post
 *  net.minecraftforge.client.event.RenderPlayerEvent$Pre
 *  net.minecraftforge.client.event.ScreenEvent$Init$Post
 *  net.minecraftforge.client.event.ScreenEvent$Opening
 *  net.minecraftforge.client.event.ViewportEvent$ComputeCameraAngles
 *  net.minecraftforge.client.event.ViewportEvent$ComputeFov
 *  net.minecraftforge.common.MinecraftForge
 *  net.minecraftforge.event.TickEvent$ClientTickEvent
 *  net.minecraftforge.event.TickEvent$Phase
 *  net.minecraftforge.event.TickEvent$PlayerTickEvent
 *  net.minecraftforge.event.TickEvent$RenderTickEvent
 *  net.minecraftforge.event.entity.EntityLeaveLevelEvent
 *  net.minecraftforge.event.level.ChunkEvent$Unload
 *  net.minecraftforge.event.level.LevelEvent$Unload
 *  net.minecraftforge.eventbus.api.EventPriority
 *  net.minecraftforge.eventbus.api.IEventBus
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.ModLoadingContext
 *  net.minecraftforge.fml.config.IConfigSpec
 *  net.minecraftforge.fml.config.ModConfig$Type
 */
package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.ClientBridge;
import dev.dyslexic.cinematic.client.BrewingBottles;
import dev.dyslexic.cinematic.client.CameraRig;
import dev.dyslexic.cinematic.client.CampfireHint;
import dev.dyslexic.cinematic.client.ClientBrewing;
import dev.dyslexic.cinematic.client.ClientEnchants;
import dev.dyslexic.cinematic.client.ClientFurnaces;
import dev.dyslexic.cinematic.client.ClientMenus;
import dev.dyslexic.cinematic.client.ClientOnly;
import dev.dyslexic.cinematic.client.ClientOnlySmoke;
import dev.dyslexic.cinematic.client.ClientPayloads;
import dev.dyslexic.cinematic.client.ClientTables;
import dev.dyslexic.cinematic.client.FreeLook;
import dev.dyslexic.cinematic.client.FurnaceBreakSmoke;
import dev.dyslexic.cinematic.client.Fx;
import dev.dyslexic.cinematic.client.HammerModel;
import dev.dyslexic.cinematic.client.KneeLayer;
import dev.dyslexic.cinematic.client.Overlay;
import dev.dyslexic.cinematic.client.Picking;
import dev.dyslexic.cinematic.client.PropLayer;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.SceneRenderer;
import dev.dyslexic.cinematic.client.SceneScreen;
import dev.dyslexic.cinematic.client.ServerListHint;
import dev.dyslexic.cinematic.client.ServerSmoke;
import dev.dyslexic.cinematic.client.Settings;
import dev.dyslexic.cinematic.client.SettingsScreen;
import dev.dyslexic.cinematic.client.ShelfBooks;
import dev.dyslexic.cinematic.client.ShelfLayoutBooks;
import dev.dyslexic.cinematic.client.Showcase;
import dev.dyslexic.cinematic.client.compat.FirstPersonBodyCompat;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

public final class CinematicClient {
    private CinematicClient() {
    }

    public static void init(IEventBus modBus) {
        ClientBridge.useStation = Scene::begin;
        ClientBridge.vanillaUse = ClientOnly::onVanillaUse;
        modBus.addListener(event -> event.register(HammerModel.LOCATION));
        modBus.addListener(event -> {
            event.register(CampfireHint.SIT);
            event.register(FreeLook.LOOK);
        });
        modBus.addListener(BrewingBottles::hideVanillaBottles);
        modBus.addListener(ShelfBooks::emptySlots);
        modBus.addListener(PropLayer::addTo);
        modBus.addListener(KneeLayer::addTo);
        modBus.addListener(event -> event.registerBlockEntityRenderer(BlockEntityType.f_58927_, BrewingBottles::new));
        modBus.addListener(event -> event.registerBlockEntityRenderer(BlockEntityType.f_244310_, ShelfLayoutBooks::new));
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, (IConfigSpec)Settings.SPEC);
        modBus.addListener(Settings::onReloading);
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new SettingsScreen((Screen)parent)));
        MinecraftForge.EVENT_BUS.register(Events.class);
        FirstPersonBodyCompat.register();
    }

    public static final class Events {
        private static final double SITTING_DROP = 0.62;
        private static final Set<Integer> SEATED = new HashSet<Integer>();

        private Events() {
        }

        @SubscribeEvent
        public static void onFrame(TickEvent.RenderTickEvent event) {
            if (event.phase == TickEvent.Phase.START) {
                Scene.frame();
            }
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            Settings.tick();
            Scene.tick();
            ClientOnlySmoke.tick();
            ServerSmoke.tick();
            FurnaceBreakSmoke.tick();
            CampfireHint.tick();
            Minecraft mc = Minecraft.m_91087_();
            if (mc.f_91073_ != null && mc.f_91073_.m_46467_() % 40L == 0L) {
                ClientFurnaces.prune(mc.f_91073_);
            }
            Showcase.tick();
        }

        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            LocalPlayer player;
            Player player2;
            if (event.phase == TickEvent.Phase.END && (player2 = event.player) instanceof LocalPlayer && (player = (LocalPlayer)player2) == Minecraft.m_91087_().f_91074_) {
                Scene.steer(player);
            }
        }

        @SubscribeEvent(priority=EventPriority.LOWEST)
        public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
            if (!event.isCanceled() && ClientPayloads.atStation(event.getEntity().m_19879_()) == StationKind.CAMPFIRE) {
                event.getPoseStack().m_85836_();
                event.getPoseStack().m_85837_(0.0, -0.62, 0.0);
                SEATED.add(event.getEntity().m_19879_());
            }
        }

        @SubscribeEvent
        public static void onRenderPlayerDone(RenderPlayerEvent.Post event) {
            if (SEATED.remove(event.getEntity().m_19879_())) {
                event.getPoseStack().m_85849_();
            }
        }

        @SubscribeEvent
        public static void onMovementInput(MovementInputUpdateEvent event) {
            Scene.movementInput(event.getInput());
        }

        @SubscribeEvent
        public static void onInteractKey(InputEvent.InteractionKeyMappingTriggered event) {
            BlockHitResult hit;
            HitResult hitResult;
            if (Scene.active()) {
                event.setCanceled(true);
                event.setSwingHand(false);
            } else if (event.isUseItem() && event.getHand() == InteractionHand.MAIN_HAND && (hitResult = Minecraft.m_91087_().f_91077_) instanceof BlockHitResult && ClientOnly.suppressUseSwing(hit = (BlockHitResult)hitResult)) {
                event.setSwingHand(false);
            }
        }

        @SubscribeEvent
        public static void onScroll(InputEvent.MouseScrollingEvent event) {
            if (Scene.active() && Minecraft.m_91087_().f_91080_ == null) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onRenderLevel(RenderLevelStageEvent event) {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
                Picking.capture(event);
            } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
                SceneRenderer.render(event);
            }
        }

        @SubscribeEvent
        public static void onFov(ViewportEvent.ComputeFov event) {
            if (Scene.active() && event.usedConfiguredFov()) {
                event.setFOV(CameraRig.fov(event.getFOV()));
            }
        }

        @SubscribeEvent
        public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
            float roll = CameraRig.roll();
            if (roll != 0.0f) {
                event.setRoll(event.getRoll() + roll);
            }
        }

        @SubscribeEvent
        public static void onRenderHand(RenderHandEvent event) {
            if (Scene.active()) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onHighlight(RenderHighlightEvent.Block event) {
            if (Scene.active()) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onGuiLayer(RenderGuiOverlayEvent.Pre event) {
            if (Overlay.hides(event.getOverlay().id())) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onGui(RenderGuiEvent.Post event) {
            Overlay.render(event.getGuiGraphics());
        }

        @SubscribeEvent(priority=EventPriority.LOWEST)
        public static void onScreenOpening(ScreenEvent.Opening event) {
            Screen next = event.getNewScreen();
            if (!Scene.active() && ClientOnly.onScreenOpening(next)) {
                event.setCanceled(true);
                return;
            }
            if (!Scene.active() || next instanceof SceneScreen) {
                return;
            }
            LocalPlayer player = Minecraft.m_91087_().f_91074_;
            if (next instanceof MenuAccess) {
                MenuAccess window = (MenuAccess)next;
                if (player != null && window.m_6262_() == player.f_36096_ && window.m_6262_() != player.f_36095_) {
                    switch (Scene.answer(window.m_6262_())) {
                        case OURS: {
                            event.setCanceled(true);
                            return;
                        }
                        case LATE: {
                            event.setCanceled(true);
                            player.f_36096_ = player.f_36095_;
                            return;
                        }
                        case FOREIGN: {
                            Scene.yieldToWindow();
                            return;
                        }
                    }
                }
            }
            if (next instanceof InventoryScreen) {
                if (Scene.phase != Scene.Phase.OUTRO) {
                    event.setCanceled(true);
                }
            } else if (Scene.phase == Scene.Phase.ACTIVE && next != null && !next.m_7043_() && !Scene.consulting) {
                Scene.drop();
            }
        }

        @SubscribeEvent
        public static void onScreenInit(ScreenEvent.Init.Post event) {
            Showcase.onScreen(event.getScreen());
            ClientOnlySmoke.onScreen(event.getScreen());
            ServerListHint.onScreenInit(event);
        }

        @SubscribeEvent
        public static void onChunkUnload(ChunkEvent.Unload event) {
            if (event.getLevel().m_5776_()) {
                ClientTables.unloadChunk(event.getChunk().m_7697_());
                ClientFurnaces.unloadChunk(event.getChunk().m_7697_());
                ClientBrewing.unloadChunk(event.getChunk().m_7697_());
                ClientEnchants.unloadChunk(event.getChunk().m_7697_());
                ClientMenus.unloadChunk(event.getChunk().m_7697_());
                ShelfBooks.unloadChunk(event.getChunk().m_7697_());
            }
        }

        @SubscribeEvent
        public static void onEntityLeave(EntityLeaveLevelEvent event) {
            if (event.getLevel().m_5776_() && event.getEntity() instanceof Player) {
                ClientPayloads.AT_STATION.remove(event.getEntity().m_19879_());
                ClientMenus.forgetEntity(event.getEntity().m_19879_());
                ShelfBooks.forgetEntity(event.getEntity().m_19879_());
            }
        }

        @SubscribeEvent
        public static void onLevelUnload(LevelEvent.Unload event) {
            if (event.getLevel().m_5776_()) {
                Scene.drop();
                ClientTables.clear();
                ClientFurnaces.clear();
                ClientBrewing.clear();
                ClientEnchants.clear();
                ClientMenus.clear();
                ShelfBooks.clear();
                Fx.clearSparks();
                ClientPayloads.clear();
            }
        }

        @SubscribeEvent
        public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
            ClientOnly.onLogin();
        }

        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            Scene.drop();
            Scene.forgetOwnWindows();
            ClientOnly.clear();
            ClientTables.clear();
            ClientFurnaces.clear();
            ClientBrewing.clear();
            ClientEnchants.clear();
            ClientMenus.clear();
            ShelfBooks.clear();
            ClientPayloads.clear();
        }
    }
}


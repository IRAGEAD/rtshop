/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.GameProfile
 *  com.mojang.blaze3d.pipeline.RenderTarget
 *  com.mojang.blaze3d.platform.InputConstants$Key
 *  io.netty.buffer.Unpooled
 *  javax.annotation.Nullable
 *  net.minecraft.ChatFormatting
 *  net.minecraft.client.CameraType
 *  net.minecraft.client.KeyMapping
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.Screenshot
 *  net.minecraft.client.gui.components.AbstractButton
 *  net.minecraft.client.gui.components.AbstractWidget
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.CycleButton
 *  net.minecraft.client.gui.components.events.ContainerEventHandler
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.gui.screens.TitleScreen
 *  net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
 *  net.minecraft.client.gui.screens.inventory.AnvilScreen
 *  net.minecraft.client.gui.screens.inventory.ContainerScreen
 *  net.minecraft.client.gui.screens.inventory.EnchantmentScreen
 *  net.minecraft.client.player.AbstractClientPlayer
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.client.player.RemotePlayer
 *  net.minecraft.client.renderer.block.model.BakedQuad
 *  net.minecraft.client.resources.model.BakedModel
 *  net.minecraft.client.server.IntegratedServer
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.NonNullList
 *  net.minecraft.core.RegistryAccess
 *  net.minecraft.core.Vec3i
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.locale.Language
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.StringTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.Component$Serializer
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.util.Mth
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.Container
 *  net.minecraft.world.Difficulty
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.MenuProvider
 *  net.minecraft.world.SimpleMenuProvider
 *  net.minecraft.world.effect.MobEffect
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.EquipmentSlot
 *  net.minecraft.world.entity.Pose
 *  net.minecraft.world.entity.item.ItemEntity
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.inventory.AnvilMenu
 *  net.minecraft.world.inventory.BeaconMenu
 *  net.minecraft.world.inventory.ChestMenu
 *  net.minecraft.world.inventory.ClickType
 *  net.minecraft.world.inventory.ContainerLevelAccess
 *  net.minecraft.world.inventory.CraftingMenu
 *  net.minecraft.world.inventory.EnchantmentMenu
 *  net.minecraft.world.inventory.Slot
 *  net.minecraft.world.inventory.SmithingMenu
 *  net.minecraft.world.inventory.StonecutterMenu
 *  net.minecraft.world.item.EnchantedBookItem
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.item.MapItem
 *  net.minecraft.world.item.alchemy.Potion
 *  net.minecraft.world.item.alchemy.PotionUtils
 *  net.minecraft.world.item.alchemy.Potions
 *  net.minecraft.world.item.armortrim.ArmorTrim
 *  net.minecraft.world.item.crafting.Ingredient
 *  net.minecraft.world.item.crafting.RecipeManager
 *  net.minecraft.world.item.crafting.StonecutterRecipe
 *  net.minecraft.world.item.enchantment.Enchantment
 *  net.minecraft.world.item.enchantment.EnchantmentHelper
 *  net.minecraft.world.item.enchantment.EnchantmentInstance
 *  net.minecraft.world.item.enchantment.Enchantments
 *  net.minecraft.world.level.BlockAndTintGetter
 *  net.minecraft.world.level.GameRules
 *  net.minecraft.world.level.GameType
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelSettings
 *  net.minecraft.world.level.WorldDataConfiguration
 *  net.minecraft.world.level.block.AbstractFurnaceBlock
 *  net.minecraft.world.level.block.AnvilBlock
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.BrewingStandBlock
 *  net.minecraft.world.level.block.CampfireBlock
 *  net.minecraft.world.level.block.CandleBlock
 *  net.minecraft.world.level.block.GrindstoneBlock
 *  net.minecraft.world.level.block.HorizontalDirectionalBlock
 *  net.minecraft.world.level.block.JukeboxBlock
 *  net.minecraft.world.level.block.LecternBlock
 *  net.minecraft.world.level.block.LoomBlock
 *  net.minecraft.world.level.block.StonecutterBlock
 *  net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BrewingStandBlockEntity
 *  net.minecraft.world.level.block.entity.CampfireBlockEntity
 *  net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity
 *  net.minecraft.world.level.block.entity.JukeboxBlockEntity
 *  net.minecraft.world.level.block.entity.LecternBlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.BooleanProperty
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.level.levelgen.WorldOptions
 *  net.minecraft.world.level.levelgen.presets.WorldPreset
 *  net.minecraft.world.level.levelgen.presets.WorldPresets
 *  net.minecraft.world.level.saveddata.maps.MapItemSavedData
 *  net.minecraft.world.level.storage.LevelResource
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.client.ConfigScreenHandler$ConfigScreenFactory
 *  net.minecraftforge.client.model.data.ModelData
 *  net.minecraftforge.common.ForgeConfigSpec$ConfigValue
 *  net.minecraftforge.fml.ModContainer
 *  net.minecraftforge.fml.ModList
 *  org.lwjgl.glfw.GLFW
 */
package dev.dyslexic.cinematic.client;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.InputConstants;
import dev.dyslexic.cinematic.Cinematic;
import dev.dyslexic.cinematic.client.AnvilStation;
import dev.dyslexic.cinematic.client.BeaconStation;
import dev.dyslexic.cinematic.client.BrewingStation;
import dev.dyslexic.cinematic.client.CameraRig;
import dev.dyslexic.cinematic.client.CampfireHint;
import dev.dyslexic.cinematic.client.CampfireStation;
import dev.dyslexic.cinematic.client.CartographyStation;
import dev.dyslexic.cinematic.client.ClientBrewing;
import dev.dyslexic.cinematic.client.ClientFurnaces;
import dev.dyslexic.cinematic.client.ClientMenus;
import dev.dyslexic.cinematic.client.ClientNet;
import dev.dyslexic.cinematic.client.ClientPayloads;
import dev.dyslexic.cinematic.client.ClientStation;
import dev.dyslexic.cinematic.client.ClientTables;
import dev.dyslexic.cinematic.client.EnchantFx;
import dev.dyslexic.cinematic.client.EnchantStation;
import dev.dyslexic.cinematic.client.FieldGuide;
import dev.dyslexic.cinematic.client.FrameProbe;
import dev.dyslexic.cinematic.client.FreeLook;
import dev.dyslexic.cinematic.client.FurnaceAppearance;
import dev.dyslexic.cinematic.client.FurnaceLayouts;
import dev.dyslexic.cinematic.client.FurnaceStation;
import dev.dyslexic.cinematic.client.GrindstoneStation;
import dev.dyslexic.cinematic.client.HammerModel;
import dev.dyslexic.cinematic.client.Hands;
import dev.dyslexic.cinematic.client.InventoryEffectsProbe;
import dev.dyslexic.cinematic.client.JukeboxStation;
import dev.dyslexic.cinematic.client.LayoutProfiles;
import dev.dyslexic.cinematic.client.LecternStation;
import dev.dyslexic.cinematic.client.LoomStation;
import dev.dyslexic.cinematic.client.Picking;
import dev.dyslexic.cinematic.client.PropLayer;
import dev.dyslexic.cinematic.client.ReachNotice;
import dev.dyslexic.cinematic.client.RecipeChoiceStrip;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.SceneScreen;
import dev.dyslexic.cinematic.client.Settings;
import dev.dyslexic.cinematic.client.SettingsScreen;
import dev.dyslexic.cinematic.client.ShelfBooks;
import dev.dyslexic.cinematic.client.ShelfStation;
import dev.dyslexic.cinematic.client.SmithingStation;
import dev.dyslexic.cinematic.client.SpatialAppearance;
import dev.dyslexic.cinematic.client.SpatialEditor;
import dev.dyslexic.cinematic.client.SpatialLayouts;
import dev.dyslexic.cinematic.client.SpatialTarget;
import dev.dyslexic.cinematic.client.StationDisplayLayouts;
import dev.dyslexic.cinematic.client.StonecutterStation;
import dev.dyslexic.cinematic.client.SurfaceAppearance;
import dev.dyslexic.cinematic.client.SurfaceLayouts;
import dev.dyslexic.cinematic.client.SurfaceStation;
import dev.dyslexic.cinematic.client.TableLayout;
import dev.dyslexic.cinematic.client.TableLayouts;
import dev.dyslexic.cinematic.client.TableRecipeBook;
import dev.dyslexic.cinematic.client.TableRecipeGuide;
import dev.dyslexic.cinematic.client.TableStation;
import dev.dyslexic.cinematic.client.guide.RecipeViewers;
import dev.dyslexic.cinematic.furnace.FurnaceLayout;
import dev.dyslexic.cinematic.furnace.FurnaceZones;
import dev.dyslexic.cinematic.net.MenuDisplayPayload;
import dev.dyslexic.cinematic.net.MenuFxPayload;
import dev.dyslexic.cinematic.net.StationActionPayload;
import dev.dyslexic.cinematic.station.RecipeChoices;
import dev.dyslexic.cinematic.station.Sessions;
import dev.dyslexic.cinematic.station.StationKind;
import dev.dyslexic.cinematic.table.TableActions;
import dev.dyslexic.cinematic.table.TableCrafting;
import dev.dyslexic.cinematic.table.TableGrid;
import dev.dyslexic.cinematic.table.TableStore;
import io.netty.buffer.Unpooled;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.LoomBlock;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import org.lwjgl.glfw.GLFW;

public final class Showcase {
    public static final boolean ENABLED = Boolean.getBoolean("cinematic.showcase");
    private static final String FROM = System.getProperty("cinematic.showcase.from", "0");
    private static final boolean ONLY = Boolean.getBoolean("cinematic.showcase.only");
    private static final boolean EDITOR_BATCH = Boolean.getBoolean("cinematic.showcase.editorBatch");
    private static final String WORLD = System.getProperty("cinematic.showcase.world", "cinematic-showcase");
    private static final BlockPos TABLE = new BlockPos(0, -60, 2);
    private static final BlockPos FURNACE = TABLE.m_122024_();
    private static final BlockPos SMOKER = new BlockPos(2, -60, 2);
    private static final BlockPos BLAST = new BlockPos(4, -60, 2);
    private static final BlockPos ENCHANT = new BlockPos(-5, -60, 8);
    private static final BlockPos ANVIL = new BlockPos(8, -60, 5);
    private static final BlockPos GRINDSTONE = new BlockPos(10, -60, 5);
    private static final BlockPos SMITHING = new BlockPos(12, -60, 5);
    private static final BlockPos STONECUTTER = new BlockPos(14, -60, 5);
    private static final BlockPos LOOM = new BlockPos(8, -60, 11);
    private static final BlockPos CARTOGRAPHY = new BlockPos(10, -60, 11);
    private static final BlockPos BREWING = new BlockPos(12, -60, 11);
    private static final BlockPos CAMPFIRE = new BlockPos(11, -60, 17);
    private static final BlockPos SOUL_CAMPFIRE = new BlockPos(15, -60, 17);
    private static final BlockPos LECTERN = new BlockPos(6, -60, 21);
    private static final BlockPos BEACON = new BlockPos(22, -56, 22);
    private static final BlockPos SHELF = new BlockPos(3, -60, 21);
    private static final Vec3[] SHELF_SLOTS = new Vec3[]{new Vec3(0.19, 0.72, 1.0), new Vec3(0.5, 0.75, 1.0), new Vec3(0.81, 0.75, 1.0), new Vec3(0.19, 0.25, 1.0), new Vec3(0.5, 0.22, 1.0), new Vec3(0.81, 0.25, 1.0)};
    private static final BlockPos JUKEBOX = new BlockPos(19, -60, 17);
    private static final int HEIGHTS_X = -10;
    private static boolean requested;
    private static int t;
    private static int wait;
    private static final Deque<Step> STEPS;
    private static final List<String> REPORT;
    private static final double[] INTRO_SHOTS;
    private static final double[] OUTRO_SHOTS;
    private static int introShot;
    private static int outroShot;
    private static final double[] MOVE_SHOTS;
    private static int moveShot;
    private static String act;
    private static boolean waitingForActive;
    private static int waitingTicks;
    private static boolean done;
    private static final Map<String, String> ANVIL_RESULTS;
    private static double devSpineY;
    private static final long[] beefPace;
    private static final List<ItemStack> savedInventory;
    private static RemotePlayer smith;
    private static final int SMITH_ID = 424242;

    private Showcase() {
    }

    public static void onScreen(Screen screen) {
        if (!ENABLED || requested || !(screen instanceof TitleScreen)) {
            return;
        }
        requested = true;
        Minecraft mc = Minecraft.m_91087_();
        mc.execute(() -> {
            try {
                if (mc.m_91392_().m_78255_(WORLD)) {
                    mc.m_231466_().m_233133_((Screen)new TitleScreen(), WORLD);
                    return;
                }
            }
            catch (Exception e) {
                Cinematic.LOGGER.warn("[Showcase] could not reopen world", (Throwable)e);
            }
            LevelSettings settings = new LevelSettings(WORLD, GameType.SURVIVAL, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.f_244649_);
            mc.m_231466_().m_233157_(WORLD, settings, new WorldOptions(20261010L, false, false), registry -> ((WorldPreset)registry.m_175515_(Registries.f_256729_).m_246971_(WorldPresets.f_226438_).m_203334_()).m_247748_());
        });
    }

    public static void tick() {
        ClientStation clientStation;
        if (!ENABLED || done) {
            return;
        }
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91074_ == null || mc.f_91073_ == null || mc.m_91092_() == null) {
            return;
        }
        if (mc.f_91080_ != null && t < 0) {
            return;
        }
        if (++t == 1) {
            Showcase.setup();
        } else if (t == 70) {
            Showcase.shot("00_before");
        } else if (t == 75) {
            switch (FROM) {
                case "ip": {
                    Showcase.nextAct("ip", new Vec3(0.7, -60.0, 5.6), 172.0f, TABLE, new Vec3(0.5, 1.0, 0.8), Direction.UP);
                    break;
                }
                case "tw": {
                    Showcase.nextAct("tw", new Vec3(0.7, -60.0, 5.6), 172.0f, TABLE, new Vec3(0.5, 1.0, 0.8), Direction.UP);
                    break;
                }
                case "pm": 
                case "pf": 
                case "ps": {
                    Showcase.polymorphAct(FROM);
                    break;
                }
                case "ji": {
                    Showcase.nextAct("ji", new Vec3(0.7, -60.0, 5.6), 172.0f, TABLE, new Vec3(0.5, 1.0, 0.8), Direction.UP);
                    break;
                }
                case "3": {
                    Showcase.nextAct("3", new Vec3(-3.2, -60.0, 4.6), 200.0f, FURNACE, new Vec3(0.5, 0.6, 1.0), Direction.SOUTH);
                    break;
                }
                case "fb": {
                    Showcase.nextAct("fb", new Vec3(-3.2, -60.0, 4.6), 200.0f, FURNACE, new Vec3(0.5, 0.6, 1.0), Direction.SOUTH);
                    break;
                }
                case "4": {
                    Showcase.nextAct("4", new Vec3(2.5, -60.0, 5.4), 180.0f, SMOKER, new Vec3(0.5, 0.6, 1.0), Direction.SOUTH);
                    break;
                }
                case "5": {
                    Showcase.nextAct("5", new Vec3(4.5, -60.0, 5.4), 180.0f, BLAST, new Vec3(0.5, 0.6, 1.0), Direction.SOUTH);
                    break;
                }
                case "i": 
                case "r": {
                    REPORT.add("act " + FROM + ": the crafter is not in Minecraft 1.20.1");
                    Showcase.step(2, () -> Showcase.finish(Minecraft.m_91087_()));
                    break;
                }
                case "6": {
                    Showcase.nextAct("6", new Vec3(-4.5, -60.0, 11.6), 180.0f, ENCHANT, new Vec3(0.5, 0.75, 0.6), Direction.UP);
                    break;
                }
                case "a": {
                    Showcase.smithyAct("a", ANVIL);
                    break;
                }
                case "g": {
                    Showcase.smithyAct("g", GRINDSTONE);
                    break;
                }
                case "s": {
                    Showcase.smithyAct("s", SMITHING);
                    break;
                }
                case "c": {
                    Showcase.smithyAct("c", STONECUTTER);
                    break;
                }
                case "l": {
                    Showcase.restock();
                    Showcase.smithyAct("l", LOOM);
                    break;
                }
                case "m": {
                    Showcase.restock();
                    Showcase.smithyAct("m", CARTOGRAPHY);
                    break;
                }
                case "b": {
                    Showcase.restock();
                    Showcase.smithyAct("b", BREWING);
                    break;
                }
                case "f": {
                    Showcase.restock();
                    Showcase.campfireAct();
                    break;
                }
                case "u": {
                    Showcase.restock();
                    Showcase.soulCampfireAct();
                    break;
                }
                case "k": {
                    Showcase.restock();
                    Showcase.lecternAct();
                    break;
                }
                case "n": {
                    Showcase.restock();
                    Showcase.beaconAct();
                    break;
                }
                case "h": {
                    Showcase.heightAct();
                    break;
                }
                case "o": {
                    Showcase.optionsAct();
                    break;
                }
                case "t": {
                    Showcase.transitionAct();
                    break;
                }
                case "y": {
                    Showcase.layoutAct();
                    break;
                }
                case "v": {
                    Showcase.lookAct();
                    break;
                }
                case "j": {
                    Showcase.jukeboxAct();
                    break;
                }
                case "q": {
                    Showcase.quickAct();
                    break;
                }
                case "e": {
                    Showcase.restock();
                    Showcase.shelfAct();
                    break;
                }
                case "w": {
                    Showcase.watchAct();
                    break;
                }
                case "pn": {
                    Showcase.penchantAct();
                    break;
                }
                case "av": {
                    Showcase.anvilRulesAct();
                    break;
                }
                case "sq": {
                    Showcase.quickMoveAct();
                    break;
                }
                case "cr": {
                    Showcase.customRecipesAct();
                    break;
                }
                case "fm": {
                    Showcase.foreignMenuAct();
                    break;
                }
                case "d": {
                    Showcase.nextAct("d", new Vec3(0.7, -60.0, 5.6), 172.0f, TABLE, new Vec3(0.5, 1.0, 0.8), Direction.UP);
                    break;
                }
                case "p": {
                    Showcase.nextAct("p", new Vec3(-3.2, -60.0, 4.6), 200.0f, FURNACE, new Vec3(0.5, 0.6, 1.0), Direction.SOUTH);
                    break;
                }
                case "z": {
                    Showcase.developerOptionsScript();
                    Showcase.step(3, () -> Showcase.finish(Minecraft.m_91087_()));
                    break;
                }
                default: {
                    Showcase.use(TABLE, new Vec3(0.5, 1.0, 0.8), Direction.UP);
                }
            }
        }
        if (Scene.phase == Scene.Phase.INTRO && introShot < INTRO_SHOTS.length && Scene.introProgress() >= INTRO_SHOTS[introShot]) {
            Showcase.shot(String.format("%s1_intro_%02d", act, Math.round(INTRO_SHOTS[introShot++] * 100.0)));
        }
        if (Scene.phase == Scene.Phase.OUTRO && outroShot < OUTRO_SHOTS.length && Scene.time / 1.05 >= OUTRO_SHOTS[outroShot]) {
            Showcase.shot(String.format("%s8_outro_%02d", act, Math.round(OUTRO_SHOTS[outroShot++] * 100.0)));
        }
        if ((clientStation = Scene.station) instanceof JukeboxStation) {
            JukeboxStation jukebox = (JukeboxStation)clientStation;
            if (Scene.phase == Scene.Phase.ACTIVE) {
                String[] move = jukebox.moveName().split(" ");
                double progress = Double.parseDouble(move[1].replace(',', '.'));
                if (!move[0].equals("none") && moveShot < MOVE_SHOTS.length && progress >= MOVE_SHOTS[moveShot]) {
                    Showcase.shot(String.format("%s5_%s_%02d", act, move[0], Math.round(MOVE_SHOTS[moveShot++] * 100.0)));
                }
            }
        }
        if (waitingForActive && Scene.phase == Scene.Phase.NONE && ++waitingTicks > 300) {
            Showcase.check("scene of act " + act + " started", false, "refused=" + Scene.refused + " screen=" + String.valueOf(Minecraft.m_91087_().f_91080_));
            Showcase.closeWindow();
            waitingTicks = 0;
            waitingForActive = false;
            Showcase.runScript();
        }
        if (!waitingForActive) {
            waitingTicks = 0;
        }
        if (waitingForActive && Scene.phase == Scene.Phase.ACTIVE && Scene.time > 0.9) {
            waitingForActive = false;
            Showcase.runScript();
        }
        if (wait > 0) {
            --wait;
        } else if (!STEPS.isEmpty()) {
            Step step = STEPS.poll();
            try {
                step.action().run();
            }
            catch (RuntimeException e) {
                Showcase.check("step in act " + act + " ran", false, e.toString());
                Cinematic.LOGGER.warn("[Showcase] a step failed", (Throwable)e);
            }
            int n = wait = STEPS.isEmpty() ? 0 : STEPS.peek().delay();
        }
        if (t > 40000) {
            REPORT.add("timed out");
            Showcase.finish(mc);
        }
    }

    private static void runScript() {
        switch (act) {
            case "ip": {
                Showcase.inventoryEffectsScript();
                break;
            }
            case "tw": {
                Showcase.tableWindowScript();
                break;
            }
            case "pm": {
                Showcase.polymorphScript();
                break;
            }
            case "pf": {
                Showcase.polymorphFurnaceScript();
                break;
            }
            case "ps": {
                Showcase.polymorphSmithingScript();
                break;
            }
            case "ji": {
                Showcase.fieldGuideScript();
                break;
            }
            case "0": {
                Showcase.tableScript();
                break;
            }
            case "d": {
                Showcase.tableGridEditorScript();
                Showcase.step(30, () -> Showcase.finish(Minecraft.m_91087_()));
                break;
            }
            case "p": {
                StationKind kind = Scene.station.kind();
                Showcase.furnaceEditorScript();
                Showcase.step(3, Scene::leave);
                Showcase.step(45, () -> {});
                if (kind == StationKind.FURNACE) {
                    Showcase.nextAct("p", new Vec3(2.5, -60.0, 5.4), 180.0f, SMOKER, new Vec3(0.5, 0.6, 1.0), Direction.SOUTH);
                    break;
                }
                if (kind == StationKind.SMOKER) {
                    Showcase.nextAct("p", new Vec3(4.5, -60.0, 5.4), 180.0f, BLAST, new Vec3(0.5, 0.6, 1.0), Direction.SOUTH);
                    break;
                }
                Showcase.developerOptionsScript();
                Showcase.step(3, () -> Showcase.finish(Minecraft.m_91087_()));
                break;
            }
            case "3": {
                Showcase.furnaceScript();
                break;
            }
            case "fb": {
                Showcase.furnaceBreakScript();
                break;
            }
            case "4": {
                Showcase.smokerScript();
                break;
            }
            case "5": {
                Showcase.blastScript();
                break;
            }
            case "6": {
                Showcase.enchantScript();
                break;
            }
            case "a": {
                Showcase.anvilScript();
                break;
            }
            case "g": {
                Showcase.grindstoneScript();
                break;
            }
            case "s": {
                Showcase.smithingScript();
                break;
            }
            case "c": {
                Showcase.stonecutterScript();
                break;
            }
            case "l": {
                Showcase.loomScript();
                break;
            }
            case "m": {
                Showcase.cartographyScript();
                break;
            }
            case "b": {
                Showcase.brewingScript();
                break;
            }
            case "u": {
                Showcase.soulCampfireScript();
                break;
            }
            case "k": {
                Showcase.lecternScript();
                break;
            }
            case "e": {
                Showcase.shelfScript();
                break;
            }
            case "n": {
                Showcase.beaconScript();
                break;
            }
            case "cr": {
                Showcase.customRecipesScript();
                break;
            }
            default: {
                Showcase.campfireScript();
            }
        }
    }

    private static void setup() {
        Minecraft mc = Minecraft.m_91087_();
        mc.f_91066_.m_92157_(CameraType.FIRST_PERSON);
        mc.f_91066_.f_92062_ = false;
        for (String c : new String[]{"gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "gamerule doMobSpawning false", "weather clear", "time set 4000", "gamerule announceAdvancements false"}) {
            Showcase.cmd(c);
        }
        Showcase.server(p -> {
            BlockEntity patt30258$temp;
            int y;
            int x;
            ServerLevel level = p.m_284548_();
            for (x = -4; x <= 6; ++x) {
                for (y = -60; y <= -57; ++y) {
                    level.m_46597_(new BlockPos(x, y, 0), y == -57 ? Blocks.f_50741_.m_49966_() : Blocks.f_50222_.m_49966_());
                }
            }
            level.m_46597_(TABLE, Blocks.f_50091_.m_49966_());
            level.m_46597_(FURNACE, (BlockState)Blocks.f_50094_.m_49966_().m_61124_((Property)AbstractFurnaceBlock.f_48683_, (Comparable)Direction.SOUTH));
            level.m_46597_(TABLE.m_122029_(), Blocks.f_50618_.m_49966_());
            level.m_46597_(SMOKER, (BlockState)Blocks.f_50619_.m_49966_().m_61124_((Property)AbstractFurnaceBlock.f_48683_, (Comparable)Direction.SOUTH));
            level.m_46597_(BLAST, (BlockState)Blocks.f_50620_.m_49966_().m_61124_((Property)AbstractFurnaceBlock.f_48683_, (Comparable)Direction.SOUTH));
            level.m_46597_(SMOKER.m_122029_(), Blocks.f_50741_.m_49966_());
            level.m_46597_(FURNACE.m_122024_(), Blocks.f_50000_.m_49966_());
            level.m_46597_(FURNACE.m_122024_().m_7494_(), Blocks.f_50681_.m_49966_());
            level.m_46597_(ENCHANT, Blocks.f_50201_.m_49966_());
            for (int y2 = 0; y2 <= 1; ++y2) {
                for (int dx = -2; dx <= 2; ++dx) {
                    level.m_46597_(ENCHANT.m_7918_(dx, y2, -2), Blocks.f_50078_.m_49966_());
                }
                for (int dz = -1; dz <= 0; ++dz) {
                    level.m_46597_(ENCHANT.m_7918_(-2, y2, dz), Blocks.f_50078_.m_49966_());
                    level.m_46597_(ENCHANT.m_7918_(2, y2, dz), Blocks.f_50078_.m_49966_());
                }
            }
            level.m_46597_(ENCHANT.m_7918_(-2, 2, -2), (BlockState)Blocks.f_152482_.m_49966_().m_61124_((Property)CandleBlock.f_152791_, (Comparable)Boolean.valueOf(true)));
            level.m_46597_(ENCHANT.m_7918_(2, 2, -2), (BlockState)Blocks.f_152482_.m_49966_().m_61124_((Property)CandleBlock.f_152791_, (Comparable)Boolean.valueOf(true)));
            for (x = 7; x <= 15; ++x) {
                for (y = -60; y <= -58; ++y) {
                    level.m_46597_(new BlockPos(x, y, 3), y == -58 ? Blocks.f_50741_.m_49966_() : Blocks.f_152589_.m_49966_());
                }
            }
            level.m_46597_(ANVIL, (BlockState)Blocks.f_50322_.m_49966_().m_61124_((Property)AnvilBlock.f_48764_, (Comparable)Direction.EAST));
            level.m_46597_(GRINDSTONE, (BlockState)Blocks.f_50623_.m_49966_().m_61124_((Property)GrindstoneBlock.f_54117_, (Comparable)Direction.SOUTH));
            level.m_46597_(SMITHING, Blocks.f_50625_.m_49966_());
            level.m_46597_(STONECUTTER, (BlockState)Blocks.f_50679_.m_49966_().m_61124_((Property)StonecutterBlock.f_57063_, (Comparable)Direction.SOUTH));
            level.m_46597_(new BlockPos(9, -60, 4), (BlockState)Blocks.f_50620_.m_49966_().m_61124_((Property)AbstractFurnaceBlock.f_48683_, (Comparable)Direction.SOUTH));
            level.m_46597_(new BlockPos(11, -57, 3), Blocks.f_50681_.m_49966_());
            level.m_46597_(new BlockPos(13, -57, 3), Blocks.f_50681_.m_49966_());
            level.m_46597_(new BlockPos(15, -60, 4), Blocks.f_50069_.m_49966_());
            for (x = 7; x <= 15; ++x) {
                for (y = -60; y <= -58; ++y) {
                    level.m_46597_(new BlockPos(x, y, 10), y == -58 ? Blocks.f_50741_.m_49966_() : Blocks.f_50222_.m_49966_());
                }
            }
            level.m_46597_(LOOM, (BlockState)Blocks.f_50617_.m_49966_().m_61124_((Property)LoomBlock.f_54117_, (Comparable)Direction.SOUTH));
            level.m_46597_(CARTOGRAPHY, Blocks.f_50621_.m_49966_());
            level.m_46597_(BREWING, Blocks.f_50255_.m_49966_());
            level.m_46597_(new BlockPos(11, -57, 10), Blocks.f_50681_.m_49966_());
            level.m_46597_(CAMPFIRE, Blocks.f_50683_.m_49966_());
            level.m_46597_(SOUL_CAMPFIRE, Blocks.f_50684_.m_49966_());
            level.m_46597_(LECTERN, (BlockState)Blocks.f_50624_.m_49966_().m_61124_((Property)LecternBlock.f_54465_, (Comparable)Direction.SOUTH));
            LecternBlock.m_269125_(null, (Level)level, (BlockPos)LECTERN, (BlockState)level.m_8055_(LECTERN), (ItemStack)Showcase.storyBook());
            level.m_46597_(LECTERN.m_122024_(), Blocks.f_50078_.m_49966_());
            level.m_46597_(LECTERN.m_122024_().m_7494_(), Blocks.f_50681_.m_49966_());
            level.m_46597_(LECTERN.m_122029_(), Blocks.f_50078_.m_49966_());
            Block[] layers = new Block[]{Blocks.f_50075_, Blocks.f_50074_, Blocks.f_50268_, Blocks.f_50090_};
            for (int layer = 0; layer < 4; ++layer) {
                int r = 4 - layer;
                for (int dx = -r; dx <= r; ++dx) {
                    for (int dz = -r; dz <= r; ++dz) {
                        level.m_46597_(BEACON.m_7918_(dx, -4 + layer, dz), layers[layer].m_49966_());
                    }
                }
            }
            level.m_46597_(BEACON, Blocks.f_50273_.m_49966_());
            level.m_46597_(JUKEBOX, Blocks.f_50131_.m_49966_());
            Showcase.buildHeights(level);
            p.m_150109_().m_6211_();
            p.m_150109_().m_6836_(0, new ItemStack((ItemLike)Items.f_42647_, 24));
            p.m_150109_().m_6836_(1, new ItemStack((ItemLike)Items.f_42398_, 12));
            p.m_150109_().m_6836_(2, new ItemStack((ItemLike)Items.f_42594_, 40));
            p.m_150109_().m_6836_(3, new ItemStack((ItemLike)Items.f_42451_, 9));
            p.m_150109_().m_6836_(4, new ItemStack((ItemLike)Items.f_42416_, 5));
            p.m_150109_().m_6836_(5, new ItemStack((ItemLike)Items.f_42413_, 8));
            p.m_150109_().m_6836_(6, new ItemStack((ItemLike)Items.f_151050_, 6));
            p.m_150109_().m_6836_(9, new ItemStack((ItemLike)Items.f_42413_, 16));
            p.m_150109_().m_6836_(10, new ItemStack((ItemLike)Items.f_42000_, 16));
            p.m_150109_().m_6836_(11, new ItemStack((ItemLike)Items.f_42579_, 4));
            p.m_150109_().m_6836_(12, new ItemStack((ItemLike)Items.f_42410_, 3));
            p.m_150109_().m_6836_(13, new ItemStack((ItemLike)Items.f_42388_));
            p.m_150109_().m_6836_(14, new ItemStack((ItemLike)Items.f_42534_, 12));
            p.m_150109_().m_6836_(15, new ItemStack((ItemLike)Items.f_151050_, 4));
            p.m_150109_().m_6836_(16, new ItemStack((ItemLike)Items.f_42413_, 16));
            ItemStack pickaxe = new ItemStack((ItemLike)Items.f_42385_);
            pickaxe.m_41721_(140);
            p.m_150109_().m_6836_(17, pickaxe);
            p.m_150109_().m_6836_(19, EnchantedBookItem.m_41161_((EnchantmentInstance)new EnchantmentInstance(Enchantments.f_44984_, 3)));
            ItemStack golden = new ItemStack((ItemLike)Items.f_42430_);
            golden.m_41663_(Enchantments.f_44977_, 3);
            golden.m_41663_(Enchantments.f_44982_, 2);
            p.m_150109_().m_6836_(20, golden);
            BlockState shelfState = (BlockState)Blocks.f_244299_.m_49966_().m_61124_((Property)HorizontalDirectionalBlock.f_54117_, (Comparable)Direction.SOUTH);
            level.m_46597_(SHELF, shelfState);
            level.m_46597_(SHELF.m_7494_(), shelfState);
            BlockEntity patt29196$temp = level.m_7702_(SHELF);
            if (patt29196$temp instanceof ChiseledBookShelfBlockEntity) {
                ChiseledBookShelfBlockEntity shelf = (ChiseledBookShelfBlockEntity)patt29196$temp;
                shelf.m_6836_(0, EnchantedBookItem.m_41161_((EnchantmentInstance)new EnchantmentInstance(Enchantments.f_44977_, 4)));
                shelf.m_6836_(1, Showcase.notesBook());
                shelf.m_6836_(3, Showcase.storyBook());
                shelf.m_6836_(4, new ItemStack((ItemLike)Items.f_42517_));
                shelf.m_6836_(5, EnchantedBookItem.m_41161_((EnchantmentInstance)new EnchantmentInstance(Enchantments.f_44962_, 1)));
            }
            if ((patt30258$temp = level.m_7702_(SHELF.m_7494_())) instanceof ChiseledBookShelfBlockEntity) {
                ChiseledBookShelfBlockEntity shelf = (ChiseledBookShelfBlockEntity)patt30258$temp;
                shelf.m_6836_(0, new ItemStack((ItemLike)Items.f_42517_));
                shelf.m_6836_(2, new ItemStack((ItemLike)Items.f_42517_));
                shelf.m_6836_(3, new ItemStack((ItemLike)Items.f_42614_));
                shelf.m_6836_(4, new ItemStack((ItemLike)Items.f_42517_));
            }
            p.m_150109_().m_6836_(21, new ItemStack((ItemLike)Items.f_265918_));
            p.m_150109_().m_6836_(22, new ItemStack((ItemLike)Items.f_42473_));
            p.m_150109_().m_6836_(23, new ItemStack((ItemLike)Items.f_42418_));
            p.m_150109_().m_6836_(24, new ItemStack((ItemLike)Items.f_265914_));
            p.m_150109_().m_6836_(25, new ItemStack((ItemLike)Items.f_151049_, 3));
            p.m_150109_().m_6836_(26, new ItemStack((ItemLike)Items.f_41905_, 24));
            p.m_150109_().m_6836_(27, new ItemStack((ItemLike)Items.f_151035_, 16));
            p.m_150109_().f_35977_ = 0;
            p.m_6749_(30);
            p.m_8999_(level, 0.7, -60.0, 5.6, 172.0f, 18.0f);
            p.m_21008_(InteractionHand.MAIN_HAND, p.m_150109_().m_8020_(0));
        });
    }

    private static void use(BlockPos pos, Vec3 local, Direction face) {
        Minecraft mc = Minecraft.m_91087_();
        BlockHitResult hit = new BlockHitResult(Vec3.m_82528_((Vec3i)pos).m_82549_(local), face, pos, false);
        mc.f_91072_.m_233732_(mc.f_91074_, InteractionHand.MAIN_HAND, hit);
        REPORT.add("used " + pos.m_123344_() + ", phase=" + String.valueOf((Object)Scene.phase) + " intro=" + String.format("%.2f", Scene.introLength) + " station=" + String.valueOf(Scene.station == null ? "-" : Scene.station.kind()));
        introShot = 0;
        outroShot = 0;
        waitingForActive = true;
    }

    private static boolean stopHere(String code) {
        if (ONLY && !code.equals(FROM)) {
            Showcase.step(2, () -> Showcase.finish(Minecraft.m_91087_()));
            return true;
        }
        return false;
    }

    private static void nextAct(String next, Vec3 from, float yaw, BlockPos station, Vec3 local, Direction face) {
        if (ONLY && !next.equals(FROM)) {
            Showcase.step(2, () -> Showcase.finish(Minecraft.m_91087_()));
            return;
        }
        Showcase.step(2, () -> {
            act = next;
            Showcase.server(p -> p.m_8999_(p.m_284548_(), from.f_82479_, from.f_82480_, from.f_82481_, yaw, 20.0f));
        });
        Showcase.step(30, () -> Showcase.shot(next + "0_before"));
        Showcase.step(4, () -> Showcase.use(station, local, face));
    }

    private static ItemStack inv(ServerPlayer p, int slot) {
        return p.m_150109_().m_8020_(slot);
    }

    private static boolean holds(ItemStack stack, Item item, int count) {
        return count == 0 ? stack.m_41619_() : stack.m_150930_(item) && stack.m_41613_() == count;
    }

    private static List<ItemStack> menuItems(ServerPlayer p) {
        ArrayList<ItemStack> items = new ArrayList<ItemStack>();
        for (Slot slot : p.f_36096_.f_38839_) {
            if (slot.f_40218_ == p.m_150109_()) continue;
            items.add(slot.m_7993_());
        }
        return items;
    }

    private static String describeAll(List<ItemStack> items) {
        StringBuilder b = new StringBuilder("[");
        for (ItemStack stack : items) {
            b.append(Showcase.describe(stack)).append(' ');
        }
        return b.append(']').toString();
    }

    private static void quickStation(String name, Consumer<ServerPlayer> setup, Runnable enter, int settle, Function<ServerPlayer, String> state, List<QuickCase> cases) {
        Showcase.step(4, () -> Showcase.server(p -> {
            for (int i = 0; i < 36; ++i) {
                p.m_150109_().m_6836_(i, ItemStack.f_41583_);
            }
            p.m_150109_().f_35977_ = 8;
            setup.accept((ServerPlayer)p);
            p.f_36095_.m_38946_();
        }));
        Showcase.step(4, () -> {
            Minecraft.m_91087_().f_91074_.m_150109_().f_35977_ = 8;
            Minecraft.m_91087_().m_91403_().m_104955_((Packet)new ServerboundSetCarriedItemPacket(8));
        });
        enter.run();
        Showcase.step(settle, () -> Showcase.check(name + ": the scene is at work", Scene.phase == Scene.Phase.ACTIVE && Minecraft.m_91087_().f_91080_ instanceof SceneScreen, "phase=" + String.valueOf((Object)Scene.phase) + " station=" + String.valueOf(Scene.station == null ? "-" : Scene.station.kind())));
        for (QuickCase c : cases) {
            Showcase.step(4, () -> Showcase.server(c.before()));
            Showcase.step(4, () -> {
                Screen patt35896$temp = Minecraft.m_91087_().f_91080_;
                if (patt35896$temp instanceof SceneScreen) {
                    SceneScreen screen = (SceneScreen)patt35896$temp;
                    screen.panelClick(c.slot() < 9 ? 36 + c.slot() : c.slot(), 0, true);
                }
            });
            Showcase.step(Math.max(8, c.settle()), () -> Showcase.server(p -> Showcase.check(name + ": " + c.name(), c.expect().test((ServerPlayer)p), "slot " + c.slot() + "=" + Showcase.describe(Showcase.inv(p, c.slot())) + " station=" + (String)state.apply((ServerPlayer)p))));
        }
        Showcase.step(2, () -> Showcase.shot("sq_" + name.replace(' ', '_')));
        Showcase.step(2, () -> {
            if (Scene.phase == Scene.Phase.ACTIVE) {
                Scene.leave();
            }
        });
        Showcase.step(50, () -> Showcase.server(p -> Showcase.check(name + ": no session after leaving", Sessions.get(p) == null, "session")));
    }

    private static Runnable enterAt(Vec3 feet, float yaw, BlockPos station, Vec3 local, Direction face) {
        return () -> {
            Showcase.step(2, () -> Showcase.server(p -> p.m_8999_(p.m_284548_(), feet.f_82479_, feet.f_82480_, feet.f_82481_, yaw, 20.0f)));
            Showcase.step(20, () -> Showcase.useAt(station, local, face));
        };
    }

    private static Runnable enterSmithy(BlockPos station) {
        double y = station.equals((Object)STONECUTTER) ? 0.56 : 1.0;
        return Showcase.enterAt(new Vec3((double)station.m_123341_() + 0.5, -60.0, (double)station.m_123343_() + 3.4), 180.0f, station, new Vec3(0.5, y, 0.5), Direction.UP);
    }

    private static ItemStack tableCell(ServerPlayer p, int world) {
        TableStore.Table table = TableStore.get(p.m_284548_()).get(TABLE);
        return table == null ? ItemStack.f_41583_ : (ItemStack)table.items.get(world);
    }

    private static int tableWorld(int logical) {
        return TableGrid.worldIndex(Scene.station != null ? Scene.station.sideDirection() : Direction.SOUTH, logical / 3, logical % 3);
    }

    private static List<ItemStack> furnaceItems(ServerPlayer p, BlockPos pos) {
        List<ItemStack> list;
        BlockEntity blockEntity = p.m_284548_().m_7702_(pos);
        if (blockEntity instanceof AbstractFurnaceBlockEntity) {
            AbstractFurnaceBlockEntity f = (AbstractFurnaceBlockEntity)blockEntity;
            list = List.of(f.m_8020_(0), f.m_8020_(1), f.m_8020_(2));
        } else {
            list = List.of();
        }
        return list;
    }

    private static void setFurnace(ServerPlayer p, BlockPos pos, ItemStack input, ItemStack fuel) {
        BlockEntity blockEntity = p.m_284548_().m_7702_(pos);
        if (blockEntity instanceof AbstractFurnaceBlockEntity) {
            AbstractFurnaceBlockEntity f = (AbstractFurnaceBlockEntity)blockEntity;
            f.m_6836_(0, input);
            f.m_6836_(1, fuel);
            f.m_6836_(2, ItemStack.f_41583_);
        }
    }

    private static void quickFurnace(String name, BlockPos pos, Vec3 feet, float yaw, ItemStack food, ItemStack wrong) {
        Showcase.quickStation(name, p -> {
            Showcase.setFurnace(p, pos, ItemStack.f_41583_, ItemStack.f_41583_);
            p.m_150109_().m_6836_(1, food.m_255036_(20));
            p.m_150109_().m_6836_(12, new ItemStack((ItemLike)Items.f_42413_, 10));
            p.m_150109_().m_6836_(13, wrong.m_41777_());
            p.m_150109_().m_6836_(14, food.m_255036_(10));
            p.m_150109_().m_6836_(15, food.m_255036_(3));
        }, Showcase.enterAt(feet, yaw, pos, new Vec3(0.5, 0.6, 1.0), Direction.SOUTH), 90, p -> Showcase.describeAll(Showcase.furnaceItems(p, pos)), List.of(new QuickCase("what smelts goes in at the top (hotbar)", 1, p -> Showcase.inv(p, 1).m_41619_() && Showcase.holds(Showcase.furnaceItems(p, pos).get(0), food.m_41720_(), 20)), new QuickCase("fuel goes into the fire box (pack)", 12, p -> Showcase.inv(p, 12).m_41619_() && Showcase.furnaceItems(p, pos).get(1).m_150930_(Items.f_42413_) && Showcase.furnaceItems(p, pos).get(1).m_41613_() >= 9), new QuickCase("what neither smelts nor burns stays put", 13, p -> Showcase.holds(Showcase.inv(p, 13), wrong.m_41720_(), wrong.m_41613_())), new QuickCase("a nearly full opening takes what fits, the rest stays", q -> Showcase.furnaceItems(q, pos).get(0).m_41764_(60), 14, p -> Showcase.holds(Showcase.inv(p, 14), food.m_41720_(), 6) && Showcase.holds(Showcase.furnaceItems(p, pos).get(0), food.m_41720_(), 64), 0), new QuickCase("an opening holding something else takes nothing", q -> Showcase.setFurnace(q, pos, new ItemStack((ItemLike)Items.f_41833_), Showcase.furnaceItems(q, pos).get(1).m_41777_()), 15, p -> Showcase.holds(Showcase.inv(p, 15), food.m_41720_(), 3) && Showcase.furnaceItems(p, pos).get(0).m_150930_(Items.f_41833_), 0)));
    }

    private static void polymorphAct(String which) {
        Showcase.step(2, () -> Showcase.server(p -> {
            try {
                Path pack = p.f_8924_.m_129843_(LevelResource.f_78180_).resolve("cinematic_polymorph_test");
                Path recipes = pack.resolve("data/cinematic_polymorph_test/recipes");
                Files.createDirectories(recipes, new FileAttribute[0]);
                Files.writeString(pack.resolve("pack.mcmeta"), (CharSequence)"{\"pack\": {\"pack_format\": 15, \"description\": \"Cinematic Polymorph test\"}}", new OpenOption[0]);
                Files.writeString(recipes.resolve("a_barrel.json"), (CharSequence)"{\"type\":\"minecraft:crafting_shapeless\",\"ingredients\":[{\"item\":\"minecraft:cobblestone\"}],\"result\":{\"item\":\"minecraft:barrel\"}}", new OpenOption[0]);
                Files.writeString(recipes.resolve("b_crafting_table.json"), (CharSequence)"{\"type\":\"minecraft:crafting_shapeless\",\"ingredients\":[{\"item\":\"minecraft:cobblestone\"}],\"result\":{\"item\":\"minecraft:crafting_table\"}}", new OpenOption[0]);
                Files.writeString(recipes.resolve("c_nugget.json"), (CharSequence)"{\"type\":\"minecraft:smelting\",\"ingredient\":{\"item\":\"minecraft:raw_iron\"},\"result\":\"minecraft:gold_nugget\",\"experience\":0,\"cookingtime\":100}", new OpenOption[0]);
                Files.writeString(recipes.resolve("d_ingot.json"), (CharSequence)"{\"type\":\"minecraft:smelting\",\"ingredient\":{\"item\":\"minecraft:raw_iron\"},\"result\":\"minecraft:iron_ingot\",\"experience\":0,\"cookingtime\":100}", new OpenOption[0]);
                Files.writeString(recipes.resolve("e_gold_sword.json"), (CharSequence)"{\"type\":\"minecraft:smithing_transform\",\"template\":{\"item\":\"minecraft:netherite_upgrade_smithing_template\"},\"base\":{\"item\":\"minecraft:diamond_sword\"},\"addition\":{\"item\":\"minecraft:netherite_ingot\"},\"result\":{\"item\":\"minecraft:golden_sword\"}}", new OpenOption[0]);
                Files.writeString(recipes.resolve("f_iron_sword.json"), (CharSequence)"{\"type\":\"minecraft:smithing_transform\",\"template\":{\"item\":\"minecraft:netherite_upgrade_smithing_template\"},\"base\":{\"item\":\"minecraft:diamond_sword\"},\"addition\":{\"item\":\"minecraft:netherite_ingot\"},\"result\":{\"item\":\"minecraft:iron_sword\"}}", new OpenOption[0]);
            }
            catch (IOException e) {
                Showcase.check("polymorph: conflict pack written", false, e.toString());
            }
        }));
        Showcase.step(4, () -> Showcase.cmd("datapack enable \"file/cinematic_polymorph_test\""));
        Showcase.step(4, () -> Showcase.cmd("reload"));
        Showcase.step(60, () -> Showcase.check("polymorph: both recipes loaded", Minecraft.m_91087_().f_91073_.m_7465_().m_44043_(new ResourceLocation("cinematic_polymorph_test", "a_barrel")).isPresent() && Minecraft.m_91087_().f_91073_.m_7465_().m_44043_(new ResourceLocation("cinematic_polymorph_test", "b_crafting_table")).isPresent(), "recipe reload"));
        if (which.equals("pf")) {
            Showcase.nextAct("pf", new Vec3(-3.2, -60.0, 4.6), 200.0f, FURNACE, new Vec3(0.5, 0.6, 1.0), Direction.SOUTH);
        } else if (which.equals("ps")) {
            Showcase.smithyAct("ps", SMITHING);
        } else {
            Showcase.nextAct("pm", new Vec3(0.7, -60.0, 5.6), 172.0f, TABLE, new Vec3(0.5, 1.0, 0.8), Direction.UP);
        }
    }

    private static void polymorphScript() {
        Showcase.step(3, () -> Showcase.server(p -> {
            TableStore store = TableStore.get(p.m_284548_());
            TableStore.Table table = store.getOrCreate(TABLE);
            for (int i = 0; i < 9; ++i) {
                table.items.set(i, (Object)ItemStack.f_41583_);
            }
            table.items.set(4, (Object)new ItemStack((ItemLike)Items.f_42594_));
            store.m_77762_();
            TableActions.sync(p.m_284548_(), TABLE);
        }));
        Showcase.step(15, () -> {
            boolean loaded = RecipeChoices.available();
            Showcase.check("polymorph: expected test mod loaded", loaded, "loaded=" + loaded);
            Showcase.check("polymorph: two results offered", Showcase.table().choices().active(), "selected=" + Showcase.table().choices().selected());
            Showcase.table().chose(1);
        });
        Showcase.step(12, () -> {
            Showcase.check("polymorph: second result preview", Showcase.table().result.m_150930_(Items.f_41960_), "result=" + Showcase.describe(Showcase.table().result));
            Showcase.server(p -> Showcase.check("polymorph: server records selection", new ResourceLocation("cinematic_polymorph_test", "b_crafting_table").equals((Object)RecipeChoices.chosen((Player)p)), "selected=" + String.valueOf(RecipeChoices.chosen((Player)p))));
        });
        Showcase.step(2, () -> ClientNet.sendToServer(new StationActionPayload(TABLE, StationKind.TABLE, StationActionPayload.Action.CRAFT, 0, 0, false, 0)));
        Showcase.step(20, () -> Showcase.server(p -> Showcase.check("polymorph: selected recipe crafted", p.f_36095_.m_142621_().m_150930_(Items.f_41960_), "carried=" + Showcase.describe(p.f_36095_.m_142621_()))));
        Showcase.step(2, () -> Showcase.finish(Minecraft.m_91087_()));
    }

    private static void polymorphFurnaceScript() {
        Showcase.step(3, () -> Showcase.server(p -> Showcase.setFurnace(p, FURNACE, new ItemStack((ItemLike)Items.f_151050_, 2), new ItemStack((ItemLike)Items.f_42413_))));
        Showcase.step(18, () -> {
            RecipeChoiceStrip strip = Scene.station.choices();
            Showcase.check("polymorph furnace: competing results offered", strip != null && strip.active(), "active=" + (strip != null && strip.active()));
            Scene.station.chose(0);
        });
        Showcase.step(14, () -> {
            Showcase.check("polymorph furnace: server choice reaches scene", Scene.station.choices().selected() == 0, "selected=" + Scene.station.choices().selected());
            Showcase.server(p -> {
                AbstractFurnaceBlockEntity furnace;
                BlockEntity patt187785$temp = p.m_284548_().m_7702_(FURNACE);
                Showcase.check("polymorph furnace: block stores chosen recipe", patt187785$temp instanceof AbstractFurnaceBlockEntity && "cinematic_polymorph_test:c_nugget".equals(RecipeChoices.furnaceChoice(furnace = (AbstractFurnaceBlockEntity)patt187785$temp, StationKind.FURNACE)), "furnace selection");
            });
        });
        Showcase.step(230, () -> Showcase.server(p -> Showcase.check("polymorph furnace: selected output smelted", Showcase.furnaceItems(p, FURNACE).get(2).m_150930_(Items.f_42587_), "output=" + Showcase.describe(Showcase.furnaceItems(p, FURNACE).get(2)))));
        Showcase.step(2, () -> Showcase.finish(Minecraft.m_91087_()));
    }

    private static void polymorphSmithingScript() {
        Showcase.step(3, () -> Showcase.server(p -> {
            AbstractContainerMenu patt188518$temp = p.f_36096_;
            if (patt188518$temp instanceof SmithingMenu) {
                SmithingMenu menu = (SmithingMenu)patt188518$temp;
                menu.m_38853_(0).m_5852_(new ItemStack((ItemLike)Items.f_265918_));
                menu.m_38853_(1).m_5852_(new ItemStack((ItemLike)Items.f_42388_));
                menu.m_38853_(2).m_5852_(new ItemStack((ItemLike)Items.f_42418_));
                menu.m_6199_(menu.m_38853_((int)0).f_40218_);
                menu.m_38946_();
            }
        }));
        Showcase.step(18, () -> {
            RecipeChoiceStrip strip = Scene.station.choices();
            Showcase.check("polymorph smithing: competing results offered", strip != null && strip.active(), "active=" + (strip != null && strip.active()));
            Scene.station.chose(0);
        });
        Showcase.step(14, () -> {
            Showcase.server(p -> Showcase.check("polymorph smithing: selected result is in the real menu", p.f_36096_.m_38853_(3).m_7993_().m_150930_(Items.f_42430_), "result=" + Showcase.describe(p.f_36096_.m_38853_(3).m_7993_())));
            Showcase.check("polymorph smithing: scene shows selected result", Scene.station.choices().selected() == 0, "selected=" + Scene.station.choices().selected());
        });
        Showcase.step(2, () -> Showcase.finish(Minecraft.m_91087_()));
    }

    private static void customRecipesAct() {
        Showcase.step(2, () -> Showcase.server(p -> {
            try {
                Path pack = p.f_8924_.m_129843_(LevelResource.f_78180_).resolve("cinematic_test");
                Path recipes = pack.resolve("data/cinematic_test/recipes");
                Files.createDirectories(recipes, new FileAttribute[0]);
                Files.writeString(pack.resolve("pack.mcmeta"), (CharSequence)"{\"pack\": {\"pack_format\": 15, \"description\": \"Cinematic Interactions test recipes\"}}", new OpenOption[0]);
                Files.writeString(recipes.resolve("named_emeralds.json"), (CharSequence)"{\"type\": \"minecraft:crafting_shaped\", \"pattern\": [\"C\", \"S\"],\n \"key\": {\"C\": {\"item\": \"minecraft:cobblestone\"}, \"S\": {\"item\": \"minecraft:stick\"}},\n \"result\": {\"item\": \"minecraft:emerald\", \"count\": 2, \"nbt\": \"{display:{Name:'{\\\"text\\\":\\\"\u0418\u0437\u0443\u043c\u0440\u0443\u0434 \u0438\u0437 \u0434\u0430\u0442\u0430\u043f\u0430\u043a\u0430\\\"}'}}\"}}\n", new OpenOption[0]);
                Files.writeString(recipes.resolve("golden_apple.json"), (CharSequence)"{\"type\": \"minecraft:smelting\", \"ingredient\": {\"item\": \"minecraft:apple\"},\n \"result\": \"minecraft:golden_apple\", \"experience\": 0.5, \"cookingtime\": 40}\n", new OpenOption[0]);
            }
            catch (IOException e) {
                Showcase.check("custom recipe pack written", false, e.toString());
            }
        }));
        Showcase.step(4, () -> Showcase.cmd("datapack enable \"file/cinematic_test\""));
        Showcase.step(4, () -> Showcase.cmd("reload"));
        Showcase.step(60, () -> {
            RecipeManager recipes = Minecraft.m_91087_().f_91073_.m_7465_();
            Showcase.check("custom recipes reach the client", recipes.m_44043_(new ResourceLocation("cinematic_test", "named_emeralds")).isPresent() && recipes.m_44043_(new ResourceLocation("cinematic_test", "golden_apple")).isPresent(), "client recipe book after /reload");
        });
        Showcase.nextAct("cr", new Vec3(0.7, -60.0, 5.6), 172.0f, TABLE, new Vec3(0.5, 1.0, 0.8), Direction.UP);
    }

    private static void customRecipesScript() {
        if (Scene.station != null && Scene.station.kind() == StationKind.FURNACE) {
            Showcase.customSmeltingScript();
            return;
        }
        Showcase.shot("cr5_table");
        Showcase.step(4, () -> Showcase.invClickItem(Items.f_42594_));
        Showcase.step(3, () -> Showcase.point(0, 1));
        Showcase.step(8, () -> Showcase.clickCell(0, 1, 1));
        Showcase.step(4, Showcase::stash);
        Showcase.step(4, () -> Showcase.invClickItem(Items.f_42398_));
        Showcase.step(3, () -> Showcase.point(1, 1));
        Showcase.step(8, () -> Showcase.clickCell(1, 1, 1));
        Showcase.step(4, Showcase::stash);
        Showcase.step(4, Showcase::pointAside);
        Showcase.step(12, () -> Showcase.shot("cr6_custom_recipe"));
        Showcase.step(2, () -> Showcase.pointAt(Showcase.table().resultPosition()));
        Showcase.step(8, () -> Showcase.shot("cr7_custom_result"));
        Showcase.step(2, Showcase::clickResult);
        Showcase.step(30, () -> {
            ItemStack hand = Showcase.carried();
            Showcase.check("data pack recipe crafted on the table", hand.m_150930_(Items.f_42616_) && hand.m_41613_() == 2 && hand.m_41786_().getString().equals("\u0418\u0437\u0443\u043c\u0440\u0443\u0434 \u0438\u0437 \u0434\u0430\u0442\u0430\u043f\u0430\u043a\u0430"), "carried=" + String.valueOf(hand) + " name=" + hand.m_41786_().getString());
        });
        Showcase.step(2, () -> Showcase.server(p -> {
            ItemStack hand = p.f_36095_.m_142621_();
            Showcase.check("server holds the named result", hand.m_150930_(Items.f_42616_) && hand.m_41788_(), "server carried=" + String.valueOf(hand) + " tag=" + String.valueOf(hand.m_41783_()));
        }));
        Showcase.step(4, Showcase::stash);
        Showcase.step(4, Scene::leave);
        Showcase.step(45, () -> {});
        Showcase.nextAct("cr", new Vec3(-3.2, -60.0, 4.6), 200.0f, FURNACE, new Vec3(0.5, 0.6, 1.0), Direction.SOUTH);
    }

    private static void customSmeltingScript() {
        Showcase.shot("cr8_furnace");
        Showcase.step(2, () -> Showcase.server(p -> p.m_150109_().m_36054_(new ItemStack((ItemLike)Items.f_42410_, 2))));
        Showcase.step(6, () -> Showcase.invClickItem(Items.f_42410_));
        Showcase.step(3, () -> Showcase.pointZone(FURNACE, 0));
        Showcase.step(8, () -> Showcase.shot("cr8_apple_over_input"));
        Showcase.step(2, () -> Showcase.clickZone(0, 1));
        Showcase.step(4, Showcase::stash);
        Showcase.step(4, () -> Showcase.invClickItem(Items.f_42413_));
        Showcase.step(3, () -> Showcase.pointZone(FURNACE, 1));
        Showcase.step(8, () -> Showcase.clickZone(1, 1));
        Showcase.step(4, Showcase::stash);
        Showcase.step(4, Showcase::pointAside);
        Showcase.step(70, () -> Showcase.shot("cr9_smelted"));
        Showcase.step(2, () -> Showcase.server(p -> {
            ItemStack itemStack;
            BlockEntity patt194555$temp = p.m_284548_().m_7702_(FURNACE);
            if (patt194555$temp instanceof AbstractFurnaceBlockEntity) {
                AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity)patt194555$temp;
                itemStack = furnace.m_8020_(2);
            } else {
                itemStack = ItemStack.f_41583_;
            }
            ItemStack result = itemStack;
            Showcase.check("data pack smelting in the furnace scene", result.m_150930_(Items.f_42436_), "result=" + Showcase.describe(result));
        }));
        Showcase.step(2, () -> Showcase.check("watchers see the custom result in the furnace", ClientFurnaces.item(FURNACE, 2).m_150930_(Items.f_42436_), "client=" + Showcase.describe(ClientFurnaces.item(FURNACE, 2))));
        Showcase.step(4, Scene::leave);
        Showcase.step(40, () -> Showcase.finish(Minecraft.m_91087_()));
    }

    private static void foreignMenuAct() {
        Showcase.step(2, () -> Showcase.server(p -> {
            p.m_150109_().m_36054_(new ItemStack((ItemLike)Items.f_42383_));
            p.m_8999_(p.m_284548_(), -4.5, -60.0, 11.6, 180.0f, 20.0f);
        }));
        Showcase.step(30, () -> Showcase.use(ENCHANT, new Vec3(0.5, 0.75, 0.6), Direction.UP));
        Showcase.step(80, () -> {
            Showcase.check("the enchanting scene plays", Scene.phase == Scene.Phase.ACTIVE && Scene.station != null && Scene.station.kind() == StationKind.ENCHANTING, "phase=" + String.valueOf((Object)Scene.phase));
            Showcase.shot("fm1_scene");
            Showcase.server(p -> p.m_5893_((MenuProvider)new SimpleMenuProvider((id, inventory, player) -> ChestMenu.m_39255_((int)id, (Inventory)inventory), (Component)Component.m_237113_((String)"Foreign menu"))));
        });
        Showcase.step(30, () -> {
            Minecraft mc = Minecraft.m_91087_();
            Showcase.check("the scene steps aside for the other menu", Scene.phase == Scene.Phase.NONE, "phase=" + String.valueOf((Object)Scene.phase));
            Showcase.check("the other menu's own window is open", mc.f_91080_ instanceof ContainerScreen && mc.f_91074_.f_36096_ instanceof ChestMenu, "screen=" + String.valueOf(mc.f_91080_));
            Showcase.server(p -> Showcase.check("no session is left on the server", Sessions.get(p) == null, "menu=" + p.f_36096_.getClass().getSimpleName()));
            Showcase.shot("fm2_window");
        });
        Showcase.step(4, () -> {
            Minecraft mc = Minecraft.m_91087_();
            AbstractContainerMenu menu = mc.f_91074_.f_36096_;
            for (int i = 0; i < menu.f_38839_.size(); ++i) {
                if (((Slot)menu.f_38839_.get((int)i)).f_40218_ != mc.f_91074_.m_150109_() || !((Slot)menu.f_38839_.get(i)).m_7993_().m_150930_(Items.f_42383_)) continue;
                mc.f_91072_.m_171799_(menu.f_38840_, i, 0, ClickType.QUICK_MOVE, (Player)mc.f_91074_);
                break;
            }
        });
        Showcase.step(20, () -> {
            Minecraft mc = Minecraft.m_91087_();
            AbstractContainerMenu menu = mc.f_91074_.f_36096_;
            boolean placed = false;
            for (Slot slot : menu.f_38839_) {
                placed |= slot.f_40218_ != mc.f_91074_.m_150109_() && slot.m_7993_().m_150930_(Items.f_42383_);
            }
            Showcase.check("the window works on (an item went in)", placed && mc.f_91080_ instanceof ContainerScreen, "screen=" + String.valueOf(mc.f_91080_));
            mc.f_91080_.m_7379_();
        });
        Showcase.step(10, () -> Showcase.server(p -> {
            p.m_150109_().m_36022_(st -> st.m_150930_(Items.f_42383_), -1, (Container)p.f_36095_.m_39730_());
            Showcase.check("the server closed it", p.f_36096_ == p.f_36095_, "menu=" + p.f_36096_.getClass().getSimpleName());
        }));
        Showcase.step(10, () -> Showcase.use(ENCHANT, new Vec3(0.5, 0.75, 0.6), Direction.UP));
        Showcase.step(30, () -> {
            Minecraft mc = Minecraft.m_91087_();
            Showcase.check("used again: its own window, no scene first", Scene.phase == Scene.Phase.NONE && mc.f_91080_ instanceof EnchantmentScreen, "screen=" + String.valueOf(mc.f_91080_) + " phase=" + String.valueOf((Object)Scene.phase));
            Showcase.shot("fm3_own_window");
            Showcase.closeWindow();
        });
        Showcase.step(10, () -> Showcase.finish(Minecraft.m_91087_()));
    }

    private static void quickMoveAct() {
        Showcase.quickStation("table", p -> {
            TableStore store = TableStore.get(p.m_284548_());
            store.remove(TABLE);
            p.m_150109_().m_6836_(0, new ItemStack((ItemLike)Items.f_42647_, 64));
            p.m_150109_().m_6836_(9, new ItemStack((ItemLike)Items.f_42647_, 10));
            p.m_150109_().m_6836_(10, new ItemStack((ItemLike)Items.f_42647_, 60));
            p.m_150109_().m_6836_(11, new ItemStack((ItemLike)Items.f_42383_));
            p.m_150109_().m_6836_(12, new ItemStack((ItemLike)Items.f_42398_, 5));
        }, Showcase.enterAt(new Vec3(0.7, -60.0, 5.6), 172.0f, TABLE, new Vec3(0.5, 1.0, 0.8), Direction.UP), 90, p -> {
            TableStore.Table t = TableStore.get(p.m_284548_()).get(TABLE);
            return t == null ? "[]" : Showcase.describeAll(t.items);
        }, List.of(new QuickCase("a whole stack from the hotbar lands on the first cell", 0, p -> Showcase.inv(p, 0).m_41619_() && Showcase.holds(Showcase.tableCell(p, Showcase.tableWorld(0)), Items.f_42647_, 64)), new QuickCase("a full cell is passed over for the next free one (pack)", p -> {}, 9, p -> Showcase.inv(p, 9).m_41619_() && Showcase.holds(Showcase.tableCell(p, Showcase.tableWorld(1)), Items.f_42647_, 10), 16), new QuickCase("a partial stack tops up the cell with the same item, the rest stays", p -> {}, 10, p -> Showcase.holds(Showcase.inv(p, 10), Items.f_42647_, 6) && Showcase.holds(Showcase.tableCell(p, Showcase.tableWorld(1)), Items.f_42647_, 64), 16), new QuickCase("a full table takes nothing", q -> {
            TableStore.Table t = TableStore.get(q.m_284548_()).getOrCreate(TABLE);
            for (int i = 0; i < 9; ++i) {
                if (!((ItemStack)t.items.get(i)).m_41619_()) continue;
                t.items.set(i, (Object)new ItemStack((ItemLike)Items.f_42329_, 64));
            }
        }, 11, p -> Showcase.holds(Showcase.inv(p, 11), Items.f_42383_, 1), 16)));
        Showcase.quickStation("table recipe", p -> {
            TableStore.get(p.m_284548_()).remove(TABLE);
            p.m_150109_().m_6836_(12, new ItemStack((ItemLike)Items.f_42398_, 5));
            p.m_150109_().m_6836_(13, new ItemStack((ItemLike)Items.f_42413_, 4));
        }, () -> {
            Showcase.enterAt(new Vec3(0.7, -60.0, 5.6), 172.0f, TABLE, new Vec3(0.5, 1.0, 0.8), Direction.UP).run();
            Showcase.step(95, () -> {
                Optional<TableRecipeGuide.Entry> entry = Showcase.table().recipeBook.guide.entries.stream().filter(e -> e.holder.id().m_135815_().equals("torch")).findFirst();
                entry.ifPresent(Showcase.table().recipeBook::show);
            });
        }, 100, p -> {
            TableStore.Table t = TableStore.get(p.m_284548_()).get(TABLE);
            return t == null ? "[]" : Showcase.describeAll(t.items);
        }, List.of(new QuickCase("the stick goes to the diagram's stick cell", p -> {}, 12, p -> {
            TableRecipeGuide guide = Showcase.table().recipeBook.guide;
            for (int l = 0; l < 9; ++l) {
                if (guide.grid[l].m_43947_() || !guide.grid[l].test(new ItemStack((ItemLike)Items.f_42398_))) continue;
                return Showcase.holds(Showcase.tableCell(p, Showcase.tableWorld(l)), Items.f_42398_, 5) && Showcase.inv(p, 12).m_41619_();
            }
            return false;
        }, 16), new QuickCase("the coal to the diagram's coal cell", p -> {}, 13, p -> {
            TableRecipeGuide guide = Showcase.table().recipeBook.guide;
            for (int l = 0; l < 9; ++l) {
                if (guide.grid[l].m_43947_() || !guide.grid[l].test(new ItemStack((ItemLike)Items.f_42413_))) continue;
                return Showcase.holds(Showcase.tableCell(p, Showcase.tableWorld(l)), Items.f_42413_, 4) && Showcase.inv(p, 13).m_41619_();
            }
            return false;
        }, 16)));
        Showcase.step(2, () -> Showcase.server(p -> TableStore.get(p.m_284548_()).remove(TABLE)));
        Showcase.quickFurnace("furnace", FURNACE, new Vec3(-3.2, -60.0, 4.6), 200.0f, new ItemStack((ItemLike)Items.f_151050_), new ItemStack((ItemLike)Items.f_42329_, 5));
        Showcase.quickFurnace("smoker", SMOKER, new Vec3(2.5, -60.0, 5.4), 180.0f, new ItemStack((ItemLike)Items.f_42579_), new ItemStack((ItemLike)Items.f_151050_, 5));
        Showcase.quickFurnace("blast furnace", BLAST, new Vec3(4.5, -60.0, 5.4), 180.0f, new ItemStack((ItemLike)Items.f_151050_), new ItemStack((ItemLike)Items.f_42579_, 5));
        Showcase.quickStation("enchanting table", p -> {
            p.m_150109_().m_6836_(0, new ItemStack((ItemLike)Items.f_42534_, 10));
            p.m_150109_().m_6836_(9, new ItemStack((ItemLike)Items.f_42383_));
            p.m_150109_().m_6836_(10, new ItemStack((ItemLike)Items.f_42385_));
        }, Showcase.enterAt(new Vec3(-4.5, -60.0, 11.6), 180.0f, ENCHANT, new Vec3(0.5, 0.75, 0.6), Direction.UP), 90, p -> Showcase.describeAll(Showcase.menuItems(p)), List.of(new QuickCase("lapis into its own place", 0, p -> Showcase.inv(p, 0).m_41619_() && Showcase.holds(Showcase.menuItems(p).get(1), Items.f_42534_, 10)), new QuickCase("the sword onto the book", 9, p -> Showcase.inv(p, 9).m_41619_() && Showcase.holds(Showcase.menuItems(p).get(0), Items.f_42383_, 1)), new QuickCase("a second tool finds the place taken and stays put", 10, p -> Showcase.holds(Showcase.inv(p, 10), Items.f_42385_, 1))));
        Showcase.quickStation("anvil", p -> {
            p.m_150109_().m_6836_(0, new ItemStack((ItemLike)Items.f_42383_));
            p.m_150109_().m_6836_(9, new ItemStack((ItemLike)Items.f_42416_, 5));
            p.m_150109_().m_6836_(10, new ItemStack((ItemLike)Items.f_42398_, 3));
        }, Showcase.enterSmithy(ANVIL), 90, p -> Showcase.describeAll(Showcase.menuItems(p)), List.of(new QuickCase("the sword onto the anvil", 0, p -> Showcase.inv(p, 0).m_41619_() && Showcase.holds(Showcase.menuItems(p).get(0), Items.f_42383_, 1)), new QuickCase("the ingots beside it", 9, p -> Showcase.inv(p, 9).m_41619_() && Showcase.holds(Showcase.menuItems(p).get(1), Items.f_42416_, 5)), new QuickCase("a full anvil takes nothing more", 10, p -> Showcase.holds(Showcase.inv(p, 10), Items.f_42398_, 3))));
        Showcase.quickStation("grindstone", p -> {
            ItemStack worn = new ItemStack((ItemLike)Items.f_42383_);
            worn.m_41721_(100);
            p.m_150109_().m_6836_(0, worn);
            p.m_150109_().m_6836_(9, new ItemStack((ItemLike)Items.f_42329_, 4));
        }, Showcase.enterSmithy(GRINDSTONE), 90, p -> Showcase.describeAll(Showcase.menuItems(p)), List.of(new QuickCase("a worn sword onto the wheel", 0, p -> Showcase.inv(p, 0).m_41619_() && Showcase.menuItems(p).get(0).m_150930_(Items.f_42383_)), new QuickCase("dirt does not go on a grindstone", 9, p -> Showcase.holds(Showcase.inv(p, 9), Items.f_42329_, 4))));
        Showcase.quickStation("smithing table", p -> {
            p.m_150109_().m_6836_(0, new ItemStack((ItemLike)Items.f_265918_, 2));
            p.m_150109_().m_6836_(9, new ItemStack((ItemLike)Items.f_42388_));
            p.m_150109_().m_6836_(10, new ItemStack((ItemLike)Items.f_42329_, 4));
        }, Showcase.enterSmithy(SMITHING), 90, p -> Showcase.describeAll(Showcase.menuItems(p)), List.of(new QuickCase("the template into its place", 0, p -> Showcase.inv(p, 0).m_41619_() && Showcase.holds(Showcase.menuItems(p).get(0), Items.f_265918_, 2)), new QuickCase("the sword onto it", 9, p -> Showcase.inv(p, 9).m_41619_() && Showcase.holds(Showcase.menuItems(p).get(1), Items.f_42388_, 1)), new QuickCase("dirt stays put", 10, p -> Showcase.holds(Showcase.inv(p, 10), Items.f_42329_, 4))));
        Showcase.quickStation("stonecutter", p -> {
            p.m_150109_().m_6836_(0, new ItemStack((ItemLike)Items.f_41905_, 12));
            p.m_150109_().m_6836_(9, new ItemStack((ItemLike)Items.f_42329_, 4));
        }, Showcase.enterSmithy(STONECUTTER), 90, p -> Showcase.describeAll(Showcase.menuItems(p)), List.of(new QuickCase("dirt has nothing to be cut into: it stays", 9, p -> Showcase.holds(Showcase.inv(p, 9), Items.f_42329_, 4)), new QuickCase("stone onto the saw", 0, p -> Showcase.inv(p, 0).m_41619_() && Showcase.holds(Showcase.menuItems(p).get(0), Items.f_41905_, 12))));
        Showcase.quickStation("loom", p -> {
            p.m_150109_().m_6836_(0, new ItemStack((ItemLike)Items.f_42660_));
            p.m_150109_().m_6836_(9, new ItemStack((ItemLike)Items.f_42497_, 4));
            p.m_150109_().m_6836_(10, new ItemStack((ItemLike)Items.f_42329_, 4));
        }, Showcase.enterSmithy(LOOM), 90, p -> Showcase.describeAll(Showcase.menuItems(p)), List.of(new QuickCase("the banner onto the loom", 0, p -> Showcase.inv(p, 0).m_41619_() && Showcase.holds(Showcase.menuItems(p).get(0), Items.f_42660_, 1)), new QuickCase("the dye beside it", 9, p -> Showcase.inv(p, 9).m_41619_() && Showcase.holds(Showcase.menuItems(p).get(1), Items.f_42497_, 4)), new QuickCase("dirt stays put", 10, p -> Showcase.holds(Showcase.inv(p, 10), Items.f_42329_, 4))));
        Showcase.quickStation("cartography table", p -> {
            p.m_150109_().m_6836_(0, new ItemStack((ItemLike)Items.f_42516_, 3));
            p.m_150109_().m_6836_(9, new ItemStack((ItemLike)Items.f_42329_, 4));
        }, Showcase.enterSmithy(CARTOGRAPHY), 90, p -> Showcase.describeAll(Showcase.menuItems(p)), List.of(new QuickCase("paper into its place", 0, p -> Showcase.inv(p, 0).m_41619_() && Showcase.holds(Showcase.menuItems(p).get(1), Items.f_42516_, 3)), new QuickCase("dirt stays put", 9, p -> Showcase.holds(Showcase.inv(p, 9), Items.f_42329_, 4))));
        Showcase.quickStation("brewing stand", p -> {
            BlockEntity patt208557$temp = p.m_284548_().m_7702_(BREWING);
            if (patt208557$temp instanceof BrewingStandBlockEntity) {
                BrewingStandBlockEntity stand = (BrewingStandBlockEntity)patt208557$temp;
                for (int i = 0; i < 5; ++i) {
                    stand.m_6836_(i, ItemStack.f_41583_);
                }
            }
            ItemStack water = PotionUtils.m_43549_((ItemStack)new ItemStack((ItemLike)Items.f_42589_), (Potion)Potions.f_43599_);
            p.m_150109_().m_6836_(0, new ItemStack((ItemLike)Items.f_42593_, 3));
            p.m_150109_().m_6836_(9, new ItemStack((ItemLike)Items.f_42588_, 2));
            p.m_150109_().m_6836_(10, water);
            p.m_150109_().m_6836_(11, new ItemStack((ItemLike)Items.f_42329_, 4));
        }, Showcase.enterSmithy(BREWING), 90, p -> Showcase.describeAll(Showcase.menuItems(p)), List.of(new QuickCase("blaze powder to the foot", 0, p -> Showcase.inv(p, 0).m_41619_() && Showcase.menuItems(p).get(4).m_150930_(Items.f_42593_) && Showcase.menuItems(p).get(4).m_41613_() >= 2), new QuickCase("nether wart into the bowl", 9, p -> Showcase.inv(p, 9).m_41619_() && Showcase.holds(Showcase.menuItems(p).get(3), Items.f_42588_, 2)), new QuickCase("a bottle onto a foot", 10, p -> Showcase.inv(p, 10).m_41619_() && Showcase.menuItems(p).get(0).m_150930_(Items.f_42589_)), new QuickCase("dirt stays put", 11, p -> Showcase.holds(Showcase.inv(p, 11), Items.f_42329_, 4))));
        Showcase.quickStation("beacon", p -> {
            p.m_150109_().m_6836_(0, new ItemStack((ItemLike)Items.f_42416_, 5));
            p.m_150109_().m_6836_(9, new ItemStack((ItemLike)Items.f_42329_, 4));
        }, Showcase.enterAt(new Vec3((double)BEACON.m_123341_() + 0.5, (double)BEACON.m_123342_(), (double)BEACON.m_123343_() + 1.75), 180.0f, BEACON, new Vec3(0.5, 0.9, 0.5), Direction.UP), 120, p -> Showcase.describeAll(Showcase.menuItems(p)), List.of(new QuickCase("dirt is no payment: it stays", 9, p -> Showcase.holds(Showcase.inv(p, 9), Items.f_42329_, 4)), new QuickCase("one ingot onto the beacon, the rest stays", 0, p -> Showcase.holds(Showcase.inv(p, 0), Items.f_42416_, 4) && Showcase.holds(Showcase.menuItems(p).get(0), Items.f_42416_, 1))));
        Showcase.quickStation("lectern", p -> p.m_150109_().m_6836_(9, new ItemStack((ItemLike)Items.f_42614_)), Showcase.enterAt(new Vec3((double)LECTERN.m_123341_() + 0.5, -60.0, (double)LECTERN.m_123343_() + 2.6), 180.0f, LECTERN, new Vec3(0.5, 0.9, 0.6), Direction.UP), 110, p -> Showcase.describeAll(Showcase.menuItems(p)), List.of(new QuickCase("a lectern holding its book takes no other", 9, p -> Showcase.holds(Showcase.inv(p, 9), Items.f_42614_, 1))));
        Showcase.quickStation("chiseled bookshelf", p -> {
            BlockEntity patt211779$temp = p.m_284548_().m_7702_(SHELF);
            if (patt211779$temp instanceof ChiseledBookShelfBlockEntity) {
                ChiseledBookShelfBlockEntity shelf = (ChiseledBookShelfBlockEntity)patt211779$temp;
                for (int i = 0; i < 6; ++i) {
                    if (i == 1) {
                        shelf.m_6836_(i, new ItemStack((ItemLike)Items.f_42517_));
                        continue;
                    }
                    shelf.m_7407_(i, 1);
                }
            }
            p.m_150109_().m_6836_(9, new ItemStack((ItemLike)Items.f_42517_, 3));
            p.m_150109_().m_6836_(10, new ItemStack((ItemLike)Items.f_42329_, 4));
        }, Showcase.enterAt(new Vec3((double)SHELF.m_123341_() + 0.5, -60.0, (double)SHELF.m_123343_() + 3.0), 180.0f, SHELF, SHELF_SLOTS[1], Direction.SOUTH), 90, p -> {
            ArrayList<ItemStack> books = new ArrayList<ItemStack>();
            BlockEntity patt212743$temp = p.m_284548_().m_7702_(SHELF);
            if (patt212743$temp instanceof ChiseledBookShelfBlockEntity) {
                ChiseledBookShelfBlockEntity shelf = (ChiseledBookShelfBlockEntity)patt212743$temp;
                for (int i = 0; i < 6; ++i) {
                    books.add(shelf.m_8020_(i));
                }
            }
            return Showcase.describeAll(books);
        }, List.of(new QuickCase("dirt is no book: it stays", 10, p -> Showcase.holds(Showcase.inv(p, 10), Items.f_42329_, 4)), new QuickCase("a book from the pack into the first empty place", p -> {}, 9, p -> {
            ChiseledBookShelfBlockEntity shelf;
            BlockEntity patt213265$temp;
            return Showcase.holds(Showcase.inv(p, 9), Items.f_42517_, 2) && (patt213265$temp = p.m_284548_().m_7702_(SHELF)) instanceof ChiseledBookShelfBlockEntity && (shelf = (ChiseledBookShelfBlockEntity)patt213265$temp).m_8020_(0).m_150930_(Items.f_42517_);
        }, 30), new QuickCase("the next one skips the taken places", p -> {}, 9, p -> {
            ChiseledBookShelfBlockEntity shelf;
            BlockEntity patt213601$temp;
            return Showcase.holds(Showcase.inv(p, 9), Items.f_42517_, 1) && (patt213601$temp = p.m_284548_().m_7702_(SHELF)) instanceof ChiseledBookShelfBlockEntity && (shelf = (ChiseledBookShelfBlockEntity)patt213601$temp).m_8020_(2).m_150930_(Items.f_42517_);
        }, 30), new QuickCase("a full shelf takes nothing", q -> {
            BlockEntity patt213883$temp = q.m_284548_().m_7702_(SHELF);
            if (patt213883$temp instanceof ChiseledBookShelfBlockEntity) {
                ChiseledBookShelfBlockEntity shelf = (ChiseledBookShelfBlockEntity)patt213883$temp;
                for (int i = 0; i < 6; ++i) {
                    if (!shelf.m_8020_(i).m_41619_()) continue;
                    shelf.m_6836_(i, new ItemStack((ItemLike)Items.f_42517_));
                }
            }
        }, 9, p -> Showcase.holds(Showcase.inv(p, 9), Items.f_42517_, 1), 30)));
        Showcase.quickStation("campfire", p -> {
            BlockEntity patt214268$temp = p.m_284548_().m_7702_(CAMPFIRE);
            if (patt214268$temp instanceof CampfireBlockEntity) {
                CampfireBlockEntity fire = (CampfireBlockEntity)patt214268$temp;
                fire.m_59065_().set(0, (Object)new ItemStack((ItemLike)Items.f_42581_));
                fire.f_59044_[0] = 600;
                fire.f_59043_[0] = 0;
                for (int i = 1; i < 4; ++i) {
                    fire.m_59065_().set(i, (Object)ItemStack.f_41583_);
                }
                fire.m_6596_();
                p.m_284548_().m_7260_(CAMPFIRE, fire.m_58900_(), fire.m_58900_(), 3);
            }
            p.m_150109_().m_6836_(0, new ItemStack((ItemLike)Items.f_42579_, 5));
            p.m_150109_().m_6836_(9, new ItemStack((ItemLike)Items.f_42329_, 4));
        }, () -> {
            Showcase.step(2, () -> Showcase.server(p -> p.m_8999_(p.m_284548_(), (double)CAMPFIRE.m_123341_() + 0.5, -60.0, (double)CAMPFIRE.m_123343_() + 2.9, 180.0f, 24.0f)));
            Showcase.step(30, () -> KeyMapping.m_90835_((InputConstants.Key)CampfireHint.SIT.getKey()));
        }, 120, p -> {
            String string;
            BlockEntity patt215701$temp = p.m_284548_().m_7702_(CAMPFIRE);
            if (patt215701$temp instanceof CampfireBlockEntity) {
                CampfireBlockEntity fire = (CampfireBlockEntity)patt215701$temp;
                string = Showcase.describeAll((List<ItemStack>)fire.m_59065_());
            } else {
                string = "-";
            }
            return string;
        }, List.of(new QuickCase("dirt does not cook: it stays", 9, p -> Showcase.holds(Showcase.inv(p, 9), Items.f_42329_, 4)), new QuickCase("beef, one piece to each free spot, the rest stays", 0, p -> {
            CampfireBlockEntity fire;
            BlockEntity patt216138$temp;
            return Showcase.holds(Showcase.inv(p, 0), Items.f_42579_, 2) && (patt216138$temp = p.m_284548_().m_7702_(CAMPFIRE)) instanceof CampfireBlockEntity && (fire = (CampfireBlockEntity)patt216138$temp).m_59065_().stream().filter(st -> st.m_150930_(Items.f_42579_)).count() == 3L && ((ItemStack)fire.m_59065_().get(0)).m_150930_(Items.f_42581_);
        })));
        Showcase.quickStation("jukebox", p -> {
            BlockEntity patt216677$temp = p.m_284548_().m_7702_(JUKEBOX);
            if (patt216677$temp instanceof JukeboxBlockEntity) {
                JukeboxBlockEntity box = (JukeboxBlockEntity)patt216677$temp;
                box.m_272252_();
            }
            p.m_150109_().m_6836_(9, new ItemStack((ItemLike)Items.f_42701_));
            p.m_150109_().m_6836_(10, new ItemStack((ItemLike)Items.f_42329_, 4));
        }, () -> {
            Showcase.step(2, () -> Showcase.standLooking(new Vec3((double)JUKEBOX.m_123341_() - 1.6, -60.0, (double)JUKEBOX.m_123343_() + 0.5), Vec3.m_82512_((Vec3i)JUKEBOX)));
            Showcase.step(20, () -> Showcase.useAt(JUKEBOX, new Vec3(0.5, 1.0, 0.5), Direction.UP));
        }, 90, p -> {
            String string;
            BlockEntity patt217486$temp = p.m_284548_().m_7702_(JUKEBOX);
            if (patt217486$temp instanceof JukeboxBlockEntity) {
                JukeboxBlockEntity box = (JukeboxBlockEntity)patt217486$temp;
                string = Showcase.describe(box.m_272036_());
            } else {
                string = "-";
            }
            return string;
        }, List.of(new QuickCase("dirt is no record: it stays", 10, p -> Showcase.holds(Showcase.inv(p, 10), Items.f_42329_, 4)), new QuickCase("a record from the pack slides in and plays", p -> {}, 9, p -> {
            JukeboxBlockEntity box;
            BlockEntity patt217925$temp;
            return Showcase.inv(p, 9).m_41619_() && (patt217925$temp = p.m_284548_().m_7702_(JUKEBOX)) instanceof JukeboxBlockEntity && (box = (JukeboxBlockEntity)patt217925$temp).m_272036_().m_150930_(Items.f_42701_);
        }, 50)));
        Showcase.step(2, () -> Showcase.server(p -> {
            BlockEntity patt218590$temp = p.m_284548_().m_7702_(JUKEBOX);
            if (patt218590$temp instanceof JukeboxBlockEntity) {
                JukeboxBlockEntity box = (JukeboxBlockEntity)patt218590$temp;
                box.m_272252_();
            }
        }));
        Showcase.step(10, () -> Showcase.finish(Minecraft.m_91087_()));
    }

    private static boolean hasThorns(ItemStack stack) {
        Enchantment thorns = Enchantments.f_44972_;
        return EnchantmentHelper.m_44843_((Enchantment)thorns, (ItemStack)stack) > 0;
    }

    private static void anvilRound(boolean creative, boolean scene) {
        String tag = (creative ? "creative" : "survival") + (scene ? " scene" : " window");
        Showcase.step(4, () -> {
            Settings.set((ForgeConfigSpec.ConfigValue)Settings.SCENES.get((Object)StationKind.ANVIL), scene);
            Showcase.server(p -> {
                p.m_143403_(creative ? GameType.CREATIVE : GameType.SURVIVAL);
                p.m_9174_(30);
                p.m_150109_().m_36022_(st -> st.m_150930_(Items.f_42383_) || st.m_150930_(Items.f_42690_), -1, (Container)p.f_36095_.m_39730_());
                Enchantment thorns = Enchantments.f_44972_;
                p.m_150109_().m_36054_(new ItemStack((ItemLike)Items.f_42383_));
                p.m_150109_().m_36054_(EnchantedBookItem.m_41161_((EnchantmentInstance)new EnchantmentInstance(thorns, 3)));
                p.m_8999_(p.m_284548_(), (double)ANVIL.m_123341_() + 0.5, -60.0, (double)ANVIL.m_123343_() + 3.4, 180.0f, 20.0f);
            });
        });
        Showcase.step(20, () -> {
            Minecraft mc = Minecraft.m_91087_();
            mc.f_91072_.m_233732_(mc.f_91074_, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.m_82528_((Vec3i)ANVIL).m_82520_(0.5, 1.0, 0.5), Direction.UP, ANVIL, false));
        });
        Showcase.step(scene ? 80 : 20, () -> {
            Minecraft mc = Minecraft.m_91087_();
            boolean open = mc.f_91074_.f_36096_ instanceof AnvilMenu && (scene ? Scene.phase == Scene.Phase.ACTIVE : mc.f_91080_ instanceof AnvilScreen);
            Showcase.check(tag + ": the anvil is open", open, "phase=" + String.valueOf((Object)Scene.phase) + " screen=" + String.valueOf(mc.f_91080_));
            AbstractContainerMenu menu = mc.f_91074_.f_36096_;
            block0: for (Item item : List.of(Items.f_42383_, Items.f_42690_)) {
                for (int i = 0; i < menu.f_38839_.size(); ++i) {
                    if (((Slot)menu.f_38839_.get((int)i)).f_40218_ != mc.f_91074_.m_150109_() || !((Slot)menu.f_38839_.get(i)).m_7993_().m_150930_(item)) continue;
                    mc.f_91072_.m_171799_(menu.f_38840_, i, 0, ClickType.QUICK_MOVE, (Player)mc.f_91074_);
                    continue block0;
                }
            }
        });
        Showcase.step(12, () -> {
            Minecraft mc = Minecraft.m_91087_();
            ItemStack client = mc.f_91074_.f_36096_.m_38853_(2).m_7993_().m_41777_();
            Showcase.server(p -> {
                int cost;
                ItemStack itemStack;
                AbstractContainerMenu patt222787$temp = p.f_36096_;
                if (patt222787$temp instanceof AnvilMenu) {
                    AnvilMenu menu = (AnvilMenu)patt222787$temp;
                    itemStack = menu.m_38853_(2).m_7993_();
                } else {
                    itemStack = ItemStack.f_41583_;
                }
                ItemStack result = itemStack;
                AbstractContainerMenu patt222934$temp = p.f_36096_;
                if (patt222934$temp instanceof AnvilMenu) {
                    AnvilMenu menu = (AnvilMenu)patt222934$temp;
                    v1 = menu.m_39028_();
                } else {
                    v1 = cost = -1;
                }
                String line = (String)(result.m_41619_() ? "no result" : Showcase.describe(result) + (Showcase.hasThorns(result) ? " with Thorns" : " without Thorns")) + ", cost " + cost;
                ANVIL_RESULTS.put(tag, line);
                REPORT.add("anvil " + tag + ": server " + line + "; client shows " + (String)(client.m_41619_() ? "nothing" : Showcase.describe(client) + (Showcase.hasThorns(client) ? " with Thorns" : "")) + "; mods " + Showcase.loadedEnchantMods());
            });
            Showcase.shot("av_" + (creative ? "creative" : "survival") + "_" + (scene ? "scene" : "window"));
        });
        Showcase.step(4, () -> {
            if (scene) {
                Scene.leave();
            } else if (Minecraft.m_91087_().f_91080_ != null) {
                Minecraft.m_91087_().f_91080_.m_7379_();
            }
        });
        Showcase.step(scene ? 50 : 10, () -> {});
    }

    private static String loadedEnchantMods() {
        ArrayList<String> found = new ArrayList<String>();
        for (String id : List.of("penchant", "apotheosis", "apothic_enchanting", "enchdesc", "easymagic", "enchantmentlevelcapindicator")) {
            if (!ModList.get().isLoaded(id)) continue;
            found.add(id);
        }
        return found.isEmpty() ? "none" : String.join((CharSequence)",", found);
    }

    private static void anvilRulesAct() {
        for (boolean creative : new boolean[]{false, true}) {
            Showcase.anvilRound(creative, true);
            Showcase.anvilRound(creative, false);
        }
        Showcase.step(4, () -> {
            for (String mode : List.of("survival", "creative")) {
                String inScene = ANVIL_RESULTS.get(mode + " scene");
                String inWindow = ANVIL_RESULTS.get(mode + " window");
                Showcase.check("anvil, " + mode + ": the scene gives what the plain window gives", inScene != null && inScene.equals(inWindow), "scene=" + inScene + " | window=" + inWindow);
            }
            Settings.set((ForgeConfigSpec.ConfigValue)Settings.SCENES.get((Object)StationKind.ANVIL), true);
            Showcase.server(p -> p.m_143403_(GameType.SURVIVAL));
        });
        Showcase.step(4, () -> Showcase.finish(Minecraft.m_91087_()));
    }

    private static boolean penchantWindow() {
        Screen screen = Minecraft.m_91087_().f_91080_;
        return screen != null && screen.getClass().getName().startsWith("archives.tater.penchant");
    }

    private static void penchantAct() {
        boolean penchant = ModList.get().isLoaded("penchant");
        Showcase.step(2, () -> Showcase.server(p -> {
            p.m_150109_().m_36054_(new ItemStack((ItemLike)Items.f_42383_));
            p.m_8999_(p.m_284548_(), -4.5, -60.0, 11.6, 180.0f, 20.0f);
        }));
        Showcase.step(30, () -> {
            Minecraft mc = Minecraft.m_91087_();
            mc.f_91072_.m_233732_(mc.f_91074_, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.m_82528_((Vec3i)ENCHANT).m_82520_(0.5, 0.75, 0.6), Direction.UP, ENCHANT, false));
        });
        if (!penchant) {
            Showcase.step(60, () -> Showcase.check("without Penchant the enchanting scene plays", Scene.phase != Scene.Phase.NONE && Scene.station != null && Scene.station.kind() == StationKind.ENCHANTING, "phase=" + String.valueOf((Object)Scene.phase)));
            Showcase.step(4, Scene::leave);
            Showcase.step(40, () -> Showcase.finish(Minecraft.m_91087_()));
            return;
        }
        Showcase.step(30, () -> {
            Minecraft mc = Minecraft.m_91087_();
            Showcase.check("Penchant's own window opens at the enchanting table", Showcase.penchantWindow(), "screen=" + String.valueOf(mc.f_91080_));
            Showcase.check("the scene stepped aside for it", Scene.phase == Scene.Phase.NONE, "phase=" + String.valueOf((Object)Scene.phase));
            Showcase.server(p -> {
                Showcase.check("no session is left on the server", Sessions.get(p) == null, "session");
                Showcase.check("the server keeps Penchant's menu open", p.f_36096_.getClass().getName().startsWith("archives.tater.penchant"), "menu=" + p.f_36096_.getClass().getSimpleName());
            });
            Showcase.shot("pn1_penchant_window");
        });
        Showcase.step(4, () -> {
            Minecraft mc = Minecraft.m_91087_();
            AbstractContainerMenu menu = mc.f_91074_.f_36096_;
            for (int i = 0; i < menu.f_38839_.size(); ++i) {
                if (((Slot)menu.f_38839_.get((int)i)).f_40218_ != mc.f_91074_.m_150109_() || !((Slot)menu.f_38839_.get(i)).m_7993_().m_150930_(Items.f_42383_)) continue;
                mc.f_91072_.m_171799_(menu.f_38840_, i, 0, ClickType.QUICK_MOVE, (Player)mc.f_91074_);
                break;
            }
        });
        Showcase.step(20, () -> {
            Minecraft mc = Minecraft.m_91087_();
            AbstractContainerMenu menu = mc.f_91074_.f_36096_;
            boolean placed = false;
            for (Slot slot : menu.f_38839_) {
                placed |= slot.f_40218_ != mc.f_91074_.m_150109_() && slot.m_7993_().m_150930_(Items.f_42383_);
            }
            Showcase.check("the sword went into Penchant's table and its window lives on", placed && Showcase.penchantWindow(), "screen=" + String.valueOf(mc.f_91080_));
            Showcase.shot("pn2_sword_in");
            mc.f_91080_.m_7379_();
        });
        Showcase.step(10, () -> {
            Minecraft mc = Minecraft.m_91087_();
            Showcase.check("closing gives the sword back", mc.f_91080_ == null && mc.f_91074_.f_36096_ == mc.f_91074_.f_36095_ && mc.f_91074_.m_150109_().m_18947_(Items.f_42383_) == 1, "screen=" + String.valueOf(mc.f_91080_));
            Showcase.server(p -> Showcase.check("the server closed it too", p.f_36096_ == p.f_36095_ && Sessions.get(p) == null, "menu"));
        });
        Showcase.step(10, () -> {
            Minecraft mc = Minecraft.m_91087_();
            mc.f_91072_.m_233732_(mc.f_91074_, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.m_82528_((Vec3i)ENCHANT).m_82520_(0.5, 0.75, 0.6), Direction.UP, ENCHANT, false));
            Showcase.check("used again, no scene starts", Scene.phase == Scene.Phase.NONE, "phase=" + String.valueOf((Object)Scene.phase));
        });
        Showcase.step(20, () -> {
            Showcase.check("Penchant's window opens again", Showcase.penchantWindow(), "screen=" + String.valueOf(Minecraft.m_91087_().f_91080_));
            if (Showcase.penchantWindow()) {
                Minecraft.m_91087_().f_91080_.m_7379_();
            }
        });
        Showcase.step(10, () -> Showcase.server(p -> Showcase.check("nothing left open", p.f_36096_ == p.f_36095_ && Sessions.get(p) == null, "menu")));
        Showcase.step(4, () -> Showcase.finish(Minecraft.m_91087_()));
    }

    private static FieldGuide guide() {
        FieldGuide fieldGuide;
        Screen screen = Minecraft.m_91087_().f_91080_;
        if (screen instanceof SceneScreen) {
            SceneScreen screen2 = (SceneScreen)screen;
            fieldGuide = screen2.guide;
        } else {
            fieldGuide = null;
        }
        return fieldGuide;
    }

    private static void typeText(String text) {
        for (char c : text.toCharArray()) {
            Minecraft.m_91087_().f_91080_.m_5534_(c, 0);
        }
    }

    private static boolean viewerOpen() {
        Screen screen = Minecraft.m_91087_().f_91080_;
        return screen != null && !(screen instanceof SceneScreen) && screen.getClass().getName().startsWith("mezz.jei");
    }

    private static void guideFind(String query, Item item) {
        Showcase.step(3, () -> {
            double[] at = Showcase.guide().searchPoint();
            Showcase.pointGui(at[0], at[1]);
            Showcase.screenClick(0);
        });
        Showcase.step(3, () -> {
            double[] at = Showcase.guide().searchPoint();
            Showcase.pointGui(at[0], at[1]);
            for (int i = 0; i < 24; ++i) {
                Minecraft.m_91087_().f_91080_.m_7933_(259, 0, 0);
            }
            Showcase.typeText(query);
        });
        Showcase.step(6, () -> {
            int index = Showcase.guide().indexOf(item);
            Showcase.check("guide finds " + query, index >= 0 && index < 24, "index=" + index + " items=" + Showcase.guide().itemCount());
            if (index >= 0) {
                double[] at = Showcase.guide().entryPoint(index);
                Showcase.pointGui(at[0], at[1]);
            }
        });
        Showcase.step(3, () -> Showcase.screenClick(0));
    }

    private static void guideButton(String key) {
        double[] at = Showcase.guide().buttonPoint(key);
        Showcase.check("guide button " + key, at != null, "drawn");
        if (at != null) {
            Showcase.pointGui(at[0], at[1]);
            Showcase.screenClick(0);
        }
    }

    private static void fieldGuideScript() {
        if (RecipeViewers.get() == null) {
            Showcase.step(10, Showcase::lambda$fieldGuideScript$241);
            Showcase.step(3, () -> Showcase.finish(Minecraft.m_91087_()));
            return;
        }
        if (Scene.station instanceof TableStation) {
            boolean originalDev = Settings.developerMode();
            Showcase.step(10, () -> {
                Settings.set(Settings.DEVELOPER_MODE, true);
                Showcase.pointAside();
            });
            Showcase.step(6, () -> {
                double[] spine = Showcase.guide().togglePoint();
                double[] gear = Showcase.table().gridEditor.togglePoint();
                Showcase.check("developer mode: the guide sits above the gear", gear[1] - spine[1] >= 20.0 && Math.abs(gear[0] - spine[0]) < 2.0, "spine=" + Math.round(spine[1]) + " gear=" + Math.round(gear[1]));
                devSpineY = spine[1];
                Showcase.pointGui(spine[0], spine[1]);
            });
            Showcase.step(4, () -> {
                Showcase.shot("ji0_dev_toggles");
                Showcase.screenClick(0);
            });
            Showcase.step(16, () -> {
                Showcase.check("developer mode: the spine opens the guide, not the editor", Showcase.guide().opened() && !Showcase.table().gridEditor.visible(), "opened");
                double[] at = Showcase.guide().closePoint();
                Showcase.pointGui(at[0], at[1]);
            });
            Showcase.step(3, () -> Showcase.screenClick(0));
            Showcase.step(16, () -> {
                Settings.set(Settings.DEVELOPER_MODE, originalDev);
                Showcase.pointAside();
            });
            Showcase.step(4, () -> {
                double[] spine = Showcase.guide().togglePoint();
                double[] gear = Showcase.table().gridEditor.togglePoint();
                Showcase.check("without developer mode the guide is back in place", !Showcase.guide().opened() && spine[1] - devSpineY >= 20.0 && Math.abs(spine[1] - gear[1]) < 3.0, "spine=" + Math.round(spine[1]) + " before=" + Math.round(devSpineY));
                Showcase.shot("ji0_plain_toggle");
            });
            Showcase.step(10, () -> {
                double[] at = Showcase.guide().togglePoint();
                Showcase.pointGui(at[0], at[1]);
                Showcase.shot("ji0_toggle");
                Showcase.screenClick(0);
            });
            Showcase.step(16, () -> Showcase.check("guide opens beside the table", Showcase.guide().opened(), "opened"));
            Showcase.guideFind("torch", Items.f_42000_);
            Showcase.step(8, () -> {
                Showcase.check("guide: torch recipes at the table", Showcase.guide().recipeCount() >= 1, "recipes=" + Showcase.guide().recipeCount());
                Showcase.pointAside();
                Showcase.shot("ji1_guide_recipe");
            });
            Showcase.step(3, () -> Showcase.guideButton("show_table"));
            Showcase.step(12, () -> {
                TableRecipeGuide.Entry chosen = Showcase.table().recipeBook.guide.selected;
                Showcase.check("guide shows the torch on the tabletop", chosen != null && chosen.holder.id().m_135815_().equals("torch"), "selected=" + String.valueOf(chosen == null ? "-" : chosen.holder.id()));
                Showcase.server(p -> Showcase.check("looking a recipe up moves nothing", TableStore.get(p.m_284548_()).get(TABLE) == null || TableStore.get((ServerLevel)p.m_284548_()).get((BlockPos)Showcase.TABLE).items.stream().allMatch(ItemStack::m_41619_), "table empty"));
                Showcase.pointAside();
                Showcase.shot("ji2_table_hint");
            });
            Showcase.step(3, () -> Showcase.guideButton("how_to_get"));
            Showcase.step(14, () -> {
                Showcase.check("JEI's page opens over the scene, which holds", Showcase.viewerOpen() && Scene.phase == Scene.Phase.ACTIVE && Scene.consulting, "screen=" + String.valueOf(Minecraft.m_91087_().f_91080_) + " phase=" + String.valueOf((Object)Scene.phase));
                Showcase.server(p -> Showcase.check("the session stays while JEI is open", Sessions.get(p) != null, "session"));
                Showcase.shot("ji3_jei_page");
                if (Showcase.viewerOpen()) {
                    Minecraft.m_91087_().f_91080_.m_7379_();
                }
            });
            Showcase.step(10, () -> {
                Showcase.check("back from JEI to the work", Minecraft.m_91087_().f_91080_ instanceof SceneScreen && Scene.phase == Scene.Phase.ACTIVE && !Scene.consulting, "screen=" + String.valueOf(Minecraft.m_91087_().f_91080_));
                Showcase.pointAside();
                Showcase.shot("ji4_back");
            });
            Showcase.step(4, () -> {
                double[] at = Showcase.guide().entryPoint(Showcase.guide().indexOf(Items.f_42000_));
                Showcase.pointGui(at[0], at[1]);
            });
            Showcase.step(4, () -> {
                Minecraft mc = Minecraft.m_91087_();
                int scan = GLFW.glfwGetKeyScancode((int)82);
                mc.f_91068_.m_90893_(mc.m_91268_().m_85439_(), 82, scan, 1, 0);
                mc.f_91068_.m_90893_(mc.m_91268_().m_85439_(), 82, scan, 0, 0);
            });
            Showcase.step(12, () -> {
                Showcase.check("JEI's recipe key over the guide opens one page", Showcase.viewerOpen() && Scene.consulting && Scene.phase == Scene.Phase.ACTIVE, "screen=" + String.valueOf(Minecraft.m_91087_().f_91080_));
                if (Showcase.viewerOpen()) {
                    Minecraft.m_91087_().f_91080_.m_7379_();
                }
            });
            Showcase.step(10, () -> Showcase.check("one close brings the work back", Minecraft.m_91087_().f_91080_ instanceof SceneScreen && Scene.phase == Scene.Phase.ACTIVE, "screen=" + String.valueOf(Minecraft.m_91087_().f_91080_)));
            if (ModList.get().isLoaded("jeresources")) {
                Showcase.guideFind("rotten", Items.f_42583_);
                Showcase.step(8, () -> {
                    Showcase.check("JER knows where rotten flesh comes from", Showcase.guide().sourceCount() >= 1, "sources=" + Showcase.guide().sourceCount());
                    double[] at = Showcase.guide().tabPoint("sources");
                    Showcase.pointGui(at[0], at[1]);
                    Showcase.screenClick(0);
                });
                Showcase.step(8, () -> {
                    Showcase.check("guide: sources page", Showcase.guide().tabName().equals("SOURCES"), "tab=" + Showcase.guide().tabName());
                    Showcase.pointAside();
                    Showcase.shot("ji5_sources");
                    double[] at = Showcase.guide().sourcePoint(0);
                    Showcase.pointGui(at[0], at[1]);
                    Showcase.screenClick(0);
                });
                Showcase.step(14, () -> {
                    Showcase.check("JER's own page opens over the scene", Showcase.viewerOpen() && Scene.phase == Scene.Phase.ACTIVE, "screen=" + String.valueOf(Minecraft.m_91087_().f_91080_));
                    Showcase.shot("ji6_jer_page");
                    if (Showcase.viewerOpen()) {
                        Minecraft.m_91087_().f_91080_.m_7379_();
                    }
                });
                Showcase.step(10, () -> Showcase.check("back from JER to the work", Minecraft.m_91087_().f_91080_ instanceof SceneScreen && Scene.phase == Scene.Phase.ACTIVE, "screen=" + String.valueOf(Minecraft.m_91087_().f_91080_)));
                Showcase.step(3, () -> {
                    double[] at = Showcase.guide().tabPoint("recipes");
                    Showcase.pointGui(at[0], at[1]);
                    Showcase.screenClick(0);
                });
            }
            Showcase.guideFind("torch", Items.f_42000_);
            Showcase.step(16, () -> Showcase.guideButton("uses"));
            Showcase.step(14, () -> Showcase.server(p -> p.m_8999_(p.m_284548_(), 0.5, -60.0, 12.5, 180.0f, 20.0f)));
            Showcase.step(60, () -> {
                Showcase.check("the scene let go under JEI", Scene.phase == Scene.Phase.NONE && Showcase.viewerOpen(), "phase=" + String.valueOf((Object)Scene.phase));
                if (Showcase.viewerOpen()) {
                    Minecraft.m_91087_().f_91080_.m_7379_();
                }
            });
            Showcase.step(10, () -> {
                Showcase.check("JEI hands back nothing stale", Minecraft.m_91087_().f_91080_ == null && !Scene.active(), "screen=" + String.valueOf(Minecraft.m_91087_().f_91080_));
                Showcase.server(p -> Showcase.check("no session left behind", Sessions.get(p) == null, "session"));
            });
            Showcase.nextAct("ji", new Vec3(-3.2, -60.0, 4.6), 200.0f, FURNACE, new Vec3(0.5, 0.6, 1.0), Direction.SOUTH);
        } else if (Scene.station != null && Scene.station.kind() == StationKind.FURNACE) {
            Showcase.step(10, () -> {
                double[] at = Showcase.guide().togglePoint();
                Showcase.pointGui(at[0], at[1]);
                Showcase.screenClick(0);
            });
            Showcase.step(16, () -> Showcase.check("guide opens beside the furnace", Showcase.guide().opened(), "opened"));
            Showcase.guideFind("iron ingot", Items.f_42416_);
            Showcase.step(8, () -> Showcase.check("guide: iron ingot smelts here", Showcase.guide().recipeCount() >= 1, "recipes=" + Showcase.guide().recipeCount()));
            Showcase.step(3, () -> Showcase.guideButton("show_furnace"));
            Showcase.step(16, () -> {
                Showcase.check("the raw item shows in the furnace", !Showcase.furnace().guideHint().m_41619_(), "hint=" + Showcase.describe(Showcase.furnace().guideHint()));
                Showcase.pointAside();
                Showcase.shot("ji7_furnace_hint");
            });
            Showcase.step(4, Scene::leave);
            Showcase.step(45, () -> {});
            Showcase.nextAct("ji", new Vec3(2.5, -60.0, 5.4), 180.0f, SMOKER, new Vec3(0.5, 0.6, 1.0), Direction.SOUTH);
        } else {
            Showcase.step(10, () -> {
                double[] at = Showcase.guide().togglePoint();
                Showcase.pointGui(at[0], at[1]);
                Showcase.screenClick(0);
            });
            Showcase.step(16, () -> Showcase.check("the smoker's villager: butcher", Showcase.guide().profession() != null && Showcase.guide().profession().getString().equals(Component.m_237115_((String)"entity.minecraft.villager.butcher").getString()), "profession=" + String.valueOf(Showcase.guide().profession())));
            if (ModList.get().isLoaded("justenoughprofessions")) {
                Showcase.step(4, () -> {
                    Showcase.check("JEP has the profession's page", Showcase.guide().professionPageReady(), "page");
                    double[] at = Showcase.guide().tabPoint("profession");
                    Showcase.pointGui(at[0], at[1]);
                    Showcase.screenClick(0);
                });
                Showcase.step(8, () -> {
                    Showcase.pointAside();
                    Showcase.shot("ji8_profession");
                });
                Showcase.step(3, () -> Showcase.guideButton("open_jep"));
                Showcase.step(14, () -> {
                    Showcase.check("JEP's page opens over the scene", Showcase.viewerOpen() && Scene.phase == Scene.Phase.ACTIVE, "screen=" + String.valueOf(Minecraft.m_91087_().f_91080_));
                    Showcase.shot("ji9_jep_page");
                    if (Showcase.viewerOpen()) {
                        Minecraft.m_91087_().f_91080_.m_7379_();
                    }
                });
                Showcase.step(10, () -> Showcase.check("back from JEP to the work", Minecraft.m_91087_().f_91080_ instanceof SceneScreen && Scene.phase == Scene.Phase.ACTIVE, "screen=" + String.valueOf(Minecraft.m_91087_().f_91080_)));
            } else {
                Showcase.step(4, () -> Showcase.check("without JEP the profession tab stays hidden", !Showcase.guide().professionPageReady(), "hidden"));
            }
            Showcase.step(4, () -> {
                double[] at = Showcase.guide().closePoint();
                Showcase.pointGui(at[0], at[1]);
            });
            Showcase.step(4, () -> {
                Showcase.shot("ji10_close_hover");
                Showcase.screenClick(0);
            });
            Showcase.step(16, () -> {
                Showcase.check("the ribbon folds the guide away", !Showcase.guide().opened(), "closed");
                Showcase.check("folding the guide leaves the work on", Scene.phase == Scene.Phase.ACTIVE, "phase=" + String.valueOf((Object)Scene.phase));
                Showcase.pointAside();
                Showcase.shot("ji11_closed");
            });
            Showcase.step(4, Scene::leave);
            Showcase.step(45, () -> Showcase.finish(Minecraft.m_91087_()));
        }
    }

    private static void inventoryEffectsScript() {
        if (Scene.station instanceof TableStation) {
            float[] angle = new float[1];
            int[] scale = new int[1];
            Showcase.step(3, InventoryEffectsProbe::defaults);
            Showcase.step(3, () -> {
                Showcase.drawerPoint(3);
                Showcase.screenClick(0);
            });
            Showcase.step(12, () -> Showcase.pointAside());
            Showcase.step(30, () -> {
                Showcase.check("inventory effects carried stack", Hands.carried().m_150930_(Items.f_42451_) && InventoryEffectsProbe.cursor() == Items.f_42451_, "actual=" + String.valueOf(Hands.carried()) + " native=" + String.valueOf(InventoryEffectsProbe.cursor()));
                Showcase.check("inventory effects native cursor particles", InventoryEffectsProbe.particles() > 0, "count=" + InventoryEffectsProbe.particles());
                Showcase.check("inventory effects native physics", !InventoryEffectsProbe.hasInteractions() || InventoryEffectsProbe.physicsItem() == Items.f_42451_, "installed=" + InventoryEffectsProbe.hasInteractions() + " native=" + String.valueOf(InventoryEffectsProbe.physicsItem()));
                angle[0] = InventoryEffectsProbe.angle();
                Showcase.shot("ip1_cursor");
                Showcase.pointGui(40.0, 120.0);
            });
            Showcase.step(4, () -> Showcase.pointGui(Minecraft.m_91087_().f_91080_.f_96543_ - 40, 120.0));
            Showcase.step(4, () -> {
                Showcase.check("inventory effects physics follows motion", !InventoryEffectsProbe.hasInteractions() || Float.isFinite(InventoryEffectsProbe.angle()) && (double)Math.abs(InventoryEffectsProbe.angle() - angle[0]) > 0.1, "installed=" + InventoryEffectsProbe.hasInteractions() + " angle=" + InventoryEffectsProbe.angle());
                Showcase.point(1, 1);
                InventoryEffectsProbe.clear();
            });
            Showcase.step(25, () -> {
                Showcase.check("inventory effects world hand hides GUI copy", Hands.itemInHand() && InventoryEffectsProbe.cursor() == Items.f_41852_, "inHand=" + Hands.itemInHand() + " cursor=" + String.valueOf(InventoryEffectsProbe.cursor()));
                double[] at = Picking.toGui(Hands.heldPoint());
                Showcase.check("inventory effects follow world hand", at != null && InventoryEffectsProbe.near(at[0], at[1]), "particles=" + InventoryEffectsProbe.particles());
                Showcase.shot("ip2_hand");
                InventoryEffectsProbe.actionOnly();
                Showcase.clickCell(1, 1, 1);
            });
            Showcase.step(7, () -> {
                double[] at = Picking.toGui(Showcase.table().cellPoint(4));
                Showcase.check("inventory effects placement uses projected cell", InventoryEffectsProbe.particles() > 0 && at != null && InventoryEffectsProbe.near(at[0], at[1]), "particles=" + InventoryEffectsProbe.particles() + InventoryEffectsProbe.positions() + (at == null ? " cell off screen" : String.format(Locale.ROOT, " cell (%.0f, %.0f)", at[0], at[1])));
                Showcase.server(p -> Showcase.check("inventory effects real table placement", TableStore.get(p.m_284548_()).get(TABLE) != null && ((ItemStack)TableStore.get((ServerLevel)p.m_284548_()).get((BlockPos)Showcase.TABLE).items.get(4)).m_150930_(Items.f_42451_), "server table"));
                Showcase.shot("ip3_placed");
                Showcase.drawerPoint(3);
                Showcase.screenClick(0);
            });
            Showcase.step(8, () -> {
                InventoryEffectsProbe.clear();
                Showcase.point(1, 1);
            });
            Showcase.step(5, () -> Showcase.clickCell(1, 1, 0));
            Showcase.step(7, () -> {
                Showcase.check("inventory effects taking from world cell", Hands.carried().m_150930_(Items.f_42451_) && Hands.carried().m_41613_() == 1 && InventoryEffectsProbe.particles() > 0, "carried=" + String.valueOf(Hands.carried()) + " particles=" + InventoryEffectsProbe.particles());
                Showcase.drawerPoint(3);
                Showcase.screenClick(0);
            });
            Showcase.step(10, InventoryEffectsProbe::disable);
            Showcase.step(2, () -> {
                Showcase.drawerPoint(3);
                Showcase.screenClick(0);
                Showcase.pointAside();
            });
            Showcase.step(18, () -> {
                Showcase.check("inventory effects respect particle toggles", InventoryEffectsProbe.particles() == 0 && Hands.carried().m_150930_(Items.f_42451_), "no particles, normal transfer");
                scale[0] = (Integer)Minecraft.m_91087_().f_91066_.m_231928_().m_231551_();
                Minecraft.m_91087_().f_91066_.m_231928_().m_231514_((Object)3);
                Minecraft.m_91087_().m_5741_();
                InventoryEffectsProbe.defaults();
                Showcase.pointGui(40.0, 100.0);
            });
            Showcase.step(14, () -> {
                Showcase.check("inventory effects GUI scale coordinates", Math.abs(InventoryEffectsProbe.x() - 40.0) < 1.0 && Math.abs(InventoryEffectsProbe.y() - 100.0) < 1.0, "native=" + InventoryEffectsProbe.x() + "," + InventoryEffectsProbe.y());
                Showcase.shot("ip4_scale3");
                Minecraft.m_91087_().f_91066_.m_231928_().m_231514_((Object)scale[0]);
                Minecraft.m_91087_().m_5741_();
            });
            Showcase.step(10, () -> {
                Settings.set(Settings.DEVELOPER_MODE, true);
                Minecraft.m_91087_().f_91080_.m_7933_(71, 0, 0);
            });
            Showcase.step(15, () -> {
                Showcase.check("inventory effects editor hides carried effects", Showcase.table().gridEditor.visible() && InventoryEffectsProbe.cursor() == Items.f_41852_ && InventoryEffectsProbe.particles() == 0, "editor remains usable");
                Showcase.shot("ip5_editor");
                Minecraft.m_91087_().f_91080_.m_7933_(256, 0, 0);
                Settings.set(Settings.DEVELOPER_MODE, false);
            });
            Showcase.step(8, () -> Minecraft.m_91087_().f_91080_.m_7933_(342, 0, 0));
            Showcase.step(16, () -> {
                Showcase.check("inventory effects free look hides carried effects", FreeLook.looking() && InventoryEffectsProbe.cursor() == Items.f_41852_ && InventoryEffectsProbe.particles() == 0, "no GUI trail over third-person view");
                Minecraft.m_91087_().f_91080_.m_7920_(342, 0, 0);
            });
            Showcase.step(16, Showcase::pointAside);
            Showcase.step(8, () -> {
                Showcase.drawerPoint(3);
                Showcase.screenClick(0);
                Scene.leave();
            });
            Showcase.step(45, () -> Showcase.check("inventory effects scene exits", Scene.phase == Scene.Phase.NONE, "phase=" + String.valueOf((Object)Scene.phase)));
            Showcase.nextAct("ip", new Vec3(-3.2, -60.0, 4.6), 200.0f, FURNACE, new Vec3(0.5, 0.6, 1.0), Direction.SOUTH);
        } else if (Scene.station instanceof FurnaceStation) {
            Showcase.step(3, InventoryEffectsProbe::actionOnly);
            Showcase.step(3, () -> {
                Showcase.drawerPoint(5);
                Showcase.screenClick(0);
            });
            Showcase.step(10, () -> {
                InventoryEffectsProbe.clear();
                Showcase.pointZone(FURNACE, 1);
            });
            Showcase.step(5, () -> Showcase.screenClick(1));
            Showcase.step(7, () -> {
                Showcase.check("inventory effects furnace server transfer", ClientFurnaces.item(FURNACE, 1).m_150930_(Items.f_42413_), "fuel=" + String.valueOf(ClientFurnaces.item(FURNACE, 1)));
                Showcase.check("inventory effects furnace action particles", InventoryEffectsProbe.particles() > 0, "particles=" + InventoryEffectsProbe.particles());
                Showcase.shot("ip6_furnace");
                Showcase.drawerPoint(5);
                Showcase.screenClick(0);
            });
            Showcase.step(8, Scene::leave);
            Showcase.step(45, () -> {});
            Showcase.nextAct("ip", new Vec3(8.5, -60.0, 8.4), 180.0f, ANVIL, new Vec3(0.5, 1.0, 0.7), Direction.UP);
        } else {
            int[] ambientParticles = new int[]{0};
            Showcase.step(3, () -> Showcase.server(p -> p.m_150109_().m_6836_(3, p.m_150109_().m_8020_(17).m_41777_())));
            Showcase.step(10, InventoryEffectsProbe::actionOnly);
            Showcase.step(3, () -> {
                Showcase.drawerPoint(3);
                Showcase.screenClick(0);
            });
            Showcase.step(10, () -> {
                InventoryEffectsProbe.clear();
                Showcase.pointAt(((AnvilStation)Scene.station).layoutPoint(0));
            });
            Showcase.step(5, () -> Showcase.screenClick(0));
            Showcase.step(7, () -> {
                Showcase.check("inventory effects vanilla menu transfer", ((AnvilStation)Scene.station).slot(0).m_150930_(Items.f_42385_), "real anvil menu input");
                Showcase.check("inventory effects vanilla menu particles", InventoryEffectsProbe.particles() > 0, "particles=" + InventoryEffectsProbe.particles());
                Showcase.shot("ip7_anvil");
                InventoryEffectsProbe.allSlots();
                Showcase.pointAside();
                Showcase.server(p -> p.m_150109_().m_6836_(8, new ItemStack((ItemLike)Items.f_42000_, 16)));
            });
            for (int i = 0; i < 12; ++i) {
                Showcase.step(8, () -> {
                    ambientParticles[0] = Math.max(ambientParticles[0], InventoryEffectsProbe.particles());
                });
            }
            Showcase.step(1, () -> {
                Showcase.check("inventory effects stationary slot particles", ambientParticles[0] > 0, "peak=" + ambientParticles[0]);
                Showcase.shot("ip8_slots");
                InventoryEffectsProbe.defaults();
                Showcase.server(p -> p.m_150109_().m_6836_(4, p.m_150109_().m_8020_(19).m_41777_()));
            });
            Showcase.step(8, () -> {
                Showcase.drawerPoint(4);
                Showcase.screenClick(0);
            });
            Showcase.step(8, () -> Showcase.pointAt(((AnvilStation)Scene.station).layoutPoint(1)));
            Showcase.step(5, () -> Showcase.screenClick(0));
            Showcase.step(7, () -> {
                Showcase.check("inventory effects real forge recipe", !((AnvilStation)Scene.station).slot(2).m_41619_(), "enchantment output available");
                Showcase.pointAt(((AnvilStation)Scene.station).restPoint(0));
            });
            Showcase.step(5, () -> {
                InventoryEffectsProbe.actionOnly();
                Showcase.screenClick(0);
            });
            Showcase.step(4, () -> Showcase.check("inventory effects special move hides GUI copy", ((AnvilStation)Scene.station).moving() && InventoryEffectsProbe.cursor() == Items.f_41852_, "hammer animation owns the carried item"));
            Showcase.step(13, () -> {
                Showcase.check("inventory effects animation beat particles", InventoryEffectsProbe.particles() > 0 && ((AnvilStation)Scene.station).moving(), "particles=" + InventoryEffectsProbe.particles());
                Showcase.shot("ip9_forge_beat");
            });
            Showcase.step(27, Showcase::pointAside);
            Showcase.step(8, () -> {
                Showcase.check("inventory effects restore cursor after forging", Hands.carried().m_150930_(Items.f_42385_) && Hands.carried().m_41793_() && InventoryEffectsProbe.cursor() == Items.f_42385_, "real output=" + String.valueOf(Hands.carried()) + " enchanted=" + Hands.carried().m_41793_() + " inHand=" + Hands.itemInHand() + " cursor=" + String.valueOf(InventoryEffectsProbe.cursor()));
                Showcase.shot("ipa_forged");
                Scene.leave();
            });
            Showcase.step(50, () -> Showcase.finish(Minecraft.m_91087_()));
        }
    }

    private static void drawerPoint(int inventorySlot) {
        Minecraft mc = Minecraft.m_91087_();
        double scale = Mth.m_14008_((double)((double)mc.f_91080_.f_96544_ * 0.068 / 18.0), (double)0.6, (double)1.25);
        Showcase.pointGui((double)mc.f_91080_.f_96543_ / 2.0 + (double)((inventorySlot - 4) * 18) * scale, 6.0 + 14.0 * scale);
    }

    private static void tableWindowScript() {
        Showcase.step(2, Scene::leave);
        Showcase.step(40, () -> Showcase.server(player -> {
            ServerLevel level = player.m_284548_();
            TableStore store = TableStore.get(level);
            TableStore.Table table = store.getOrCreate(TABLE);
            table.items.set(4, (Object)new ItemStack((ItemLike)Items.f_42415_, 3));
            store.m_77762_();
            Showcase.check("table window: scene ended", Sessions.get(player) == null, "session=" + String.valueOf(Sessions.get(player)));
            player.m_5893_((MenuProvider)new SimpleMenuProvider((id, inventory, owner) -> new CraftingMenu(id, inventory, ContainerLevelAccess.m_39289_((Level)level, (BlockPos)TABLE)), (Component)Component.m_237115_((String)"container.crafting")));
            Showcase.check("table window: real crafting menu opened", player.f_36096_ instanceof CraftingMenu, "menu=" + player.f_36096_.getClass().getName());
            Showcase.check("table window: stack moved into center cell", ((Slot)player.f_36096_.f_38839_.get(5)).m_7993_().m_150930_(Items.f_42415_) && ((Slot)player.f_36096_.f_38839_.get(5)).m_7993_().m_41613_() == 3, "center=" + String.valueOf(((Slot)player.f_36096_.f_38839_.get(5)).m_7993_()));
            Showcase.check("table window: physical table cleared", store.get(TABLE) == null, "stored=" + String.valueOf(store.get(TABLE)));
            player.m_6915_();
        }));
        Showcase.step(8, () -> Showcase.finish(Minecraft.m_91087_()));
    }

    private static void tableScript() {
        Showcase.shot("05_active");
        Showcase.tableGridAlignmentScript();
        Showcase.tableGridEditorScript();
        Showcase.tableRecipeBookScript();
        Showcase.tableVisibilityScript();
        Showcase.tableGlintVisibilityScript();
        Showcase.step(4, () -> Showcase.inventoryClick(36, 0));
        Showcase.step(3, () -> Showcase.point(0, 0));
        Showcase.step(12, () -> Showcase.shot("06_carry"));
        Showcase.step(2, () -> Showcase.clickCell(0, 0, 1));
        Showcase.step(6, () -> Showcase.point(0, 1));
        Showcase.step(8, () -> Showcase.clickCell(0, 1, 1));
        Showcase.step(6, () -> Showcase.point(0, 2));
        Showcase.step(8, () -> Showcase.clickCell(0, 2, 1));
        Showcase.step(6, () -> Showcase.inventoryClick(36, 0));
        Showcase.step(4, () -> Showcase.inventoryClick(37, 0));
        Showcase.step(3, () -> Showcase.point(1, 1));
        Showcase.step(8, () -> Showcase.clickCell(1, 1, 1));
        Showcase.step(6, () -> Showcase.point(2, 1));
        Showcase.step(8, () -> Showcase.clickCell(2, 1, 1));
        Showcase.step(6, () -> Showcase.inventoryClick(37, 0));
        Showcase.step(4, Showcase::pointAside);
        Showcase.step(10, () -> Showcase.shot("07_recipe"));
        Showcase.step(2, () -> Showcase.pointAt(Showcase.table().resultPosition()));
        Showcase.step(8, () -> Showcase.shot("08_result_hover"));
        Showcase.step(2, Showcase::clickResult);
        Showcase.step(3, () -> Showcase.shot("09_crafting_a"));
        Showcase.step(3, () -> Showcase.shot("09_crafting_b"));
        Showcase.step(14, () -> Showcase.shot("10_crafted"));
        Showcase.step(2, () -> Showcase.inventoryClick(44, 0));
        Showcase.step(4, () -> Showcase.inventoryClick(36, 0));
        Showcase.step(3, () -> Showcase.point(0, 1));
        Showcase.step(6, () -> Showcase.clickCell(0, 1, 1));
        Showcase.step(3, () -> Showcase.clickCell(0, 1, 1));
        Showcase.step(3, () -> Showcase.clickCell(0, 1, 1));
        Showcase.step(4, () -> Showcase.point(1, 1));
        Showcase.step(6, () -> Showcase.clickCell(1, 1, 1));
        Showcase.step(3, () -> Showcase.clickCell(1, 1, 1));
        Showcase.step(3, () -> Showcase.clickCell(1, 1, 1));
        Showcase.step(4, () -> Showcase.inventoryClick(36, 0));
        Showcase.step(4, Showcase::pointAside);
        Showcase.step(8, () -> Showcase.shot("10b_sticks"));
        Showcase.step(2, () -> Showcase.table().craft(true));
        Showcase.step(14, () -> Showcase.report("after craft-all"));
        Showcase.step(4, () -> Showcase.inventoryClick(38, 0));
        Showcase.step(3, () -> Showcase.point(1, 1));
        Showcase.step(8, () -> Showcase.clickCell(1, 1, 0));
        Showcase.step(6, () -> Showcase.inventoryClick(40, 0));
        Showcase.step(3, () -> Showcase.point(0, 2));
        Showcase.step(8, () -> Showcase.clickCell(0, 2, 1));
        Showcase.step(5, () -> Showcase.point(2, 2));
        Showcase.step(8, () -> Showcase.clickCell(2, 2, 1));
        Showcase.step(5, () -> Showcase.inventoryClick(40, 0));
        Showcase.step(4, () -> Showcase.inventoryClick(39, 0));
        Showcase.step(3, () -> Showcase.point(1, 0));
        Showcase.step(12, () -> Showcase.shot("11_left_hand"));
        Showcase.step(2, () -> Showcase.clickCell(1, 0, 0));
        Showcase.step(6, Showcase::pointAside);
        Showcase.step(8, () -> Showcase.shot("12_items"));
        Showcase.step(4, Showcase::hoverDrawer);
        Showcase.step(10, () -> Showcase.shot("13_drawer"));
        Showcase.step(2, Showcase::pointAside);
        Showcase.step(6, () -> Showcase.report("table before leaving"));
        Showcase.step(2, Scene::leave);
        Showcase.step(50, () -> Showcase.shot("09_after"));
        Showcase.nextAct("3", new Vec3(-3.2, -60.0, 4.6), 200.0f, FURNACE, new Vec3(0.5, 0.6, 1.0), Direction.SOUTH);
    }

    private static void tableGridEditorScript() {
        TableLayout[] original = TableLayouts.profiles();
        int active = TableLayouts.selected();
        int[] previousGuiScale = new int[1];
        boolean originalDev = Settings.developerMode();
        Showcase.step(2, () -> {
            Settings.set(Settings.DEVELOPER_MODE, false);
            Minecraft.m_91087_().f_91080_.m_7933_(71, 0, 0);
            double[] at = Showcase.table().gridEditor.togglePoint();
            Showcase.check("table editor hidden with developer mode off", !Showcase.table().gridEditor.editing() && !Showcase.table().gridEditor.over(at[0], at[1]), "G and icon unavailable");
            Settings.set(Settings.DEVELOPER_MODE, true);
        });
        Showcase.step(2, () -> {
            TableLayout turned = new TableLayout(1.0, -1.0, 90.0, 1.0, 1.0, 1.0, 1.0);
            Vec3 first = turned.cell(TABLE, 0);
            Showcase.check("editor quarter-turn geometry", Math.abs(first.f_82479_ - ((double)TABLE.m_123341_() + 0.375)) < 1.0E-9 && Math.abs(first.f_82481_ - ((double)TABLE.m_123343_() + 0.625)) < 1.0E-9 && Math.abs(first.f_82480_ - ((double)TABLE.m_123342_() + 1.0625)) < 1.0E-9, "independent 90-degree reference=" + String.valueOf(first));
            TableLayout bounded = new TableLayout(100.0, -100.0, 45.0, 1.2, 1.2, Double.NaN, Double.POSITIVE_INFINITY);
            boolean inside = true;
            for (double x : new double[]{0.21875, 0.78125}) {
                for (double z : new double[]{0.21875, 0.78125}) {
                    Vec3 p = bounded.point(BlockPos.f_121853_, x, z);
                    inside &= p.f_82479_ >= 0.009 && p.f_82479_ <= 0.991 && p.f_82481_ >= 0.009 && p.f_82481_ <= 0.991;
                }
            }
            Showcase.check("editor clamps malformed layouts", inside && bounded.height() == 0.0 && bounded.itemScale() == 1.0, "rotated corners stay on the block");
        });
        Showcase.step(2, () -> Showcase.inventoryClick(36, 0));
        Showcase.step(2, () -> {
            double[] at = Showcase.table().gridEditor.togglePoint();
            Showcase.pointGui(at[0], at[1]);
            Showcase.screenClick(0);
        });
        Showcase.step(20, () -> {
            Showcase.check("editor opens without replacing scene", Showcase.table().gridEditor.editing() && Minecraft.m_91087_().f_91080_ instanceof SceneScreen && !Showcase.table().showsInventory(), "same scene, inventory hidden");
            Showcase.shot("0d0_editor_open");
        });
        Showcase.editorSliderStep(2, 24.0);
        Showcase.editorSliderStep(3, 90.0);
        Showcase.editorSliderStep(4, 80.0);
        Showcase.editorSliderStep(5, 0.5);
        Showcase.editorSliderStep(6, 75.0);
        Showcase.step(16, () -> {
            Showcase.check("editor live preview", Math.abs(TableLayouts.current().rotation() - 24.0) < 0.1 && Math.abs(TableLayouts.current().height() - 0.5) < 0.01, "layout=" + String.valueOf(TableLayouts.current()));
            Showcase.shot("0d1_editor_rotated");
            Showcase.pointAt(Showcase.table().cellPoint(4));
            Showcase.screenClick(1);
        });
        Showcase.step(3, () -> {
            Showcase.check("editor consumes item clicks", Hands.carried().m_41613_() == 24 && ClientTables.get(TABLE) == null, "carried=" + String.valueOf(Hands.carried()));
            Minecraft.m_91087_().f_91080_.m_7933_(256, 0, 0);
        });
        Showcase.step(25, () -> {
            Showcase.check("editor cancel restores profile", !Showcase.table().gridEditor.editing() && TableLayouts.current().equals(original[active]) && Scene.phase == Scene.Phase.ACTIVE, "scene remains active");
            Showcase.inventoryClick(36, 0);
        });
        Showcase.step(3, () -> Minecraft.m_91087_().f_91080_.m_7933_(71, 0, 0));
        Showcase.step(20, () -> {
            previousGuiScale[0] = (Integer)Minecraft.m_91087_().f_91066_.m_231928_().m_231551_();
            Minecraft.m_91087_().f_91066_.m_231928_().m_231514_((Object)3);
            Minecraft.m_91087_().m_5741_();
        });
        Showcase.step(8, () -> {
            Showcase.shot("0d5_editor_gui_scale_3");
            Minecraft.m_91087_().f_91066_.m_231928_().m_231514_((Object)previousGuiScale[0]);
            Minecraft.m_91087_().m_5741_();
        });
        Showcase.step(8, () -> {
            Showcase.editorControl(80.0, 249.0);
            Minecraft.m_91087_().f_91080_.m_7933_(87, 0, 0);
            Showcase.check("editor movement keys do not leave", Scene.phase == Scene.Phase.ACTIVE && Showcase.table().gridEditor.editing(), "still editing");
        });
        Showcase.editorSliderStep(2, 30.0);
        Showcase.editorSliderStep(3, 85.0);
        Showcase.editorSliderStep(4, 90.0);
        Showcase.editorSliderStep(6, 80.0);
        Showcase.step(15, () -> Showcase.editorControl(145.0, 38.0));
        Showcase.step(12, () -> {
            Showcase.check("editor profile switching", Math.abs(TableLayouts.current().rotation() - original[(active + 1) % 3].rotation()) < 0.1, "other profile restored");
            Showcase.editorControl(18.0, 38.0);
        });
        Showcase.step(20, () -> {
            Showcase.check("editor profile draft retained", Math.abs(TableLayouts.current().rotation() - 30.0) < 0.1, "draft survives switching");
            Vec3 hit = Showcase.table().cellPoint(4);
            Showcase.pointAt(hit);
            Showcase.screenClick(0);
            Showcase.pointAt(hit.m_82520_(0.04, 0.0, -0.025));
            double[] mouse = Picking.mouseGui();
            Minecraft.m_91087_().f_91080_.m_7979_(mouse[0], mouse[1], 0, 4.0, -2.0);
            Minecraft.m_91087_().f_91080_.m_6348_(mouse[0], mouse[1], 0);
        });
        Showcase.step(20, () -> {
            Showcase.check("editor drag translates grid", TableLayouts.current().x() > 0.4 && TableLayouts.current().z() < -0.2, "layout=" + String.valueOf(TableLayouts.current()));
            Showcase.shot("0d2_editor_dragged");
            Showcase.editorControl(42.0, 273.0);
        });
        Showcase.step(25, () -> {
            Showcase.check("editor saves profile", !Showcase.table().gridEditor.editing() && Files.exists(TableLayouts.path(), new LinkOption[0]) && TableLayouts.profiles()[active].rotation() == 30.0, "saved=" + String.valueOf(TableLayouts.path()));
            try {
                Showcase.check("editor persists values", Files.readString(TableLayouts.path()).contains("30.0"), "JSON contains rotation");
            }
            catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
        Showcase.step(3, () -> Minecraft.m_91087_().f_91080_.m_7933_(71, 0, 0));
        Showcase.step(18, () -> Showcase.editorControl(80.0, 249.0));
        Showcase.step(20, () -> {
            Showcase.check("editor reset preview", TableLayouts.current().equals(TableLayout.VANILLA), "vanilla preview");
            Minecraft.m_91087_().f_91080_.m_7933_(256, 0, 0);
        });
        Showcase.step(25, () -> Showcase.check("editor reset can be cancelled", Math.abs(TableLayouts.current().rotation() - 30.0) < 0.1, "saved layout retained"));
        int i = 0;
        while (i < 9) {
            int cell = i++;
            Showcase.step(2, () -> Showcase.pointAt(Showcase.table().cellPoint(cell)));
            Showcase.step(4, () -> Showcase.check("editor transformed picking " + cell, Showcase.table().hoverCell == cell, "hover=" + Showcase.table().hoverCell));
        }
        Showcase.step(3, () -> {
            double[] at = Showcase.table().recipeBook.togglePoint();
            Showcase.pointGui(at[0], at[1]);
            Showcase.screenClick(0);
        });
        Showcase.step(18, () -> Showcase.recipeSearch("wooden_pickaxe"));
        Showcase.step(5, () -> Showcase.pointRecipe("wooden_pickaxe"));
        Showcase.step(3, () -> Showcase.screenClick(0));
        Showcase.step(15, () -> {
            Showcase.shot("0d3_editor_recipe_ghosts");
            double[] at = Showcase.table().recipeBook.layPoint();
            Showcase.pointGui(at[0], at[1]);
            Showcase.screenClick(0);
        });
        Showcase.step(60, () -> {
            Showcase.check("editor recipe layout reaches adjusted cells", !Showcase.table().result.m_41619_() && Showcase.table().result.m_150930_(Items.f_42422_) && Showcase.table().recipeBook.guide.missingCount == 0, "result=" + String.valueOf(Showcase.table().result));
            Showcase.shot("0d4_editor_laid_out");
        });
        for (int[] rc : new int[][]{{0, 0}, {0, 1}, {0, 2}, {1, 1}, {2, 1}}) {
            Showcase.step(2, () -> Showcase.pointAt(Showcase.table().cellPoint(TableGrid.worldIndex(Showcase.table().sideDirection(), rc[0], rc[1]))));
            Showcase.step(4, () -> Showcase.screenClick(0));
            Showcase.step(4, () -> Showcase.inventoryClick(rc[0] == 0 ? 36 : 37, 0));
        }
        Showcase.step(3, () -> Showcase.recipeControl(113, 194));
        Showcase.step(3, () -> Showcase.recipeControl(113, 12));
        Showcase.step(20, () -> {
            Showcase.check("editor walkthrough restores ingredients", ClientTables.get(TABLE) == null || ClientTables.get(TABLE).list().stream().allMatch(ItemStack::m_41619_), "table emptied");
            Showcase.check("editor restore original profiles", TableLayouts.save(original, active), "original profiles restored");
        });
        Showcase.step(25, () -> {
            Settings.set(Settings.DEVELOPER_MODE, originalDev);
            Showcase.pointAside();
        });
    }

    private static void editorSliderStep(int field, double value) {
        Showcase.step(3, () -> {
            double[] at = Showcase.table().gridEditor.sliderPoint(field, value);
            Showcase.pointGui(at[0], at[1]);
            Showcase.screenClick(0);
            Minecraft.m_91087_().f_91080_.m_6348_(at[0], at[1], 0);
        });
    }

    private static void editorControl(double x, double y) {
        double[] at = Showcase.table().gridEditor.controlPoint(x, y);
        Showcase.pointGui(at[0], at[1]);
        Showcase.screenClick(0);
        Minecraft.m_91087_().f_91080_.m_6348_(at[0], at[1], 0);
    }

    private static void tableGridAlignmentScript() {
        Showcase.step(2, () -> {
            boolean aligned = true;
            for (int i = 0; i < 9; ++i) {
                double x = (double)(5 + 3 * (i % 3)) / 16.0;
                double z = (double)(5 + 3 * (i / 3)) / 16.0;
                aligned &= Math.abs(TableGrid.cellX(i) - x) < 1.0E-9 && Math.abs(TableGrid.cellZ(i) - z) < 1.0E-9;
            }
            Showcase.check("table cells centered on texture", aligned && TableGrid.cellX(4) == 0.5 && TableGrid.cellZ(4) == 0.5, "texture centres 5, 8, 11 on both axes");
            double low = 0.21875;
            double high = 0.78125;
            double epsilon = 1.0E-4;
            boolean bounds = TableGrid.cellAt(low - epsilon, 0.5) == -1 && TableGrid.cellAt(high, 0.5) == -1 && TableGrid.cellAt(0.5, low - epsilon) == -1 && TableGrid.cellAt(0.5, high) == -1 && TableGrid.cellAt(low + epsilon, low + epsilon) == 0 && TableGrid.cellAt(high - epsilon, high - epsilon) == 8;
            for (int line = 1; line <= 2; ++line) {
                double boundary = (3.5 + (double)(3 * line)) / 16.0;
                bounds &= TableGrid.cellAt(boundary - epsilon, 0.5) == 3 + line - 1 && TableGrid.cellAt(boundary + epsilon, 0.5) == 3 + line;
            }
            Showcase.check("table picking follows painted boundaries", bounds, "line centres delimit adjacent cells");
        });
        int i = 0;
        while (i < 9) {
            int cell = i++;
            Showcase.step(2, () -> Showcase.pointAt(new Vec3((double)TABLE.m_123341_() + (double)(5 + 3 * (cell % 3)) / 16.0, (double)TABLE.m_123342_() + 1.0, (double)TABLE.m_123343_() + (double)(5 + 3 * (cell / 3)) / 16.0)));
            Showcase.step(5, () -> {
                Showcase.check("table texture cell reachable " + cell, Showcase.table().hoverCell == cell, "hover=" + Showcase.table().hoverCell);
                Showcase.shot("0q" + cell + "_aligned_cell");
            });
        }
        Showcase.step(2, Showcase::pointAside);
    }

    private static void tableRecipeBookScript() {
        Showcase.step(4, () -> {
            List<ItemStack> units = List.of(new ItemStack((ItemLike)Items.f_42647_), new ItemStack((ItemLike)Items.f_42753_));
            int[] assignment = TableRecipeGuide.match(List.of(Ingredient.m_43929_((ItemLike[])new ItemLike[]{Items.f_42647_, Items.f_42753_}), Ingredient.m_43929_((ItemLike[])new ItemLike[]{Items.f_42647_})), units);
            Showcase.check("recipe tag matching", assignment[0] == 1 && assignment[1] == 0, Arrays.toString(assignment));
            int[] repeated = TableRecipeGuide.match(List.of(Ingredient.m_43929_((ItemLike[])new ItemLike[]{Items.f_42415_}), Ingredient.m_43929_((ItemLike[])new ItemLike[]{Items.f_42415_})), List.of(new ItemStack((ItemLike)Items.f_42415_)));
            Showcase.check("recipe duplicate quantities", Arrays.stream(repeated).filter(i -> i >= 0).count() == 1L, Arrays.toString(repeated));
            TableRecipeBook book = Showcase.table().recipeBook;
            double[] at = book.togglePoint();
            Showcase.pointGui(at[0], at[1]);
            Showcase.screenClick(0);
        });
        Showcase.step(16, Showcase::pointAside);
        Showcase.step(4, () -> Showcase.shot("0r1_recipe_reference"));
        Showcase.step(2, () -> Showcase.recipeSearch("diamond_pickaxe"));
        Showcase.step(4, () -> Showcase.pointRecipe("diamond_pickaxe"));
        Showcase.step(3, () -> Showcase.screenClick(0));
        Showcase.step(12, () -> {
            TableRecipeGuide guide = Showcase.table().recipeBook.guide;
            Showcase.check("recipe missing diamonds", guide.selected != null && guide.missingCount == 3 && !guide.selected.craftable, "missing=" + guide.missingCount);
            Showcase.check("recipe search movement keys", Scene.phase == Scene.Phase.ACTIVE, "phase=" + String.valueOf((Object)Scene.phase));
            ClientTables.Entry contents = ClientTables.get(TABLE);
            Showcase.check("recipe guide is local", (contents == null || contents.list().stream().allMatch(ItemStack::m_41619_)) && Hands.carried().m_41619_(), "no real items placed");
            Showcase.server(p -> {
                TableStore.Table stored = TableStore.get(p.m_284548_()).get(TABLE);
                Showcase.check("recipe guide server unchanged", (stored == null || stored.items.stream().allMatch(ItemStack::m_41619_)) && p.m_150109_().m_8020_(0).m_41613_() == 24 && p.m_150109_().m_8020_(1).m_41613_() == 12, "planks=" + p.m_150109_().m_8020_(0).m_41613_() + " sticks=" + p.m_150109_().m_8020_(1).m_41613_());
            });
            Showcase.pointAside();
        });
        Showcase.step(4, () -> Showcase.shot("0r2_missing_diamonds"));
        Showcase.step(2, () -> Showcase.recipeControl(12, 52));
        Showcase.step(4, () -> Showcase.check("recipe availability filter", Showcase.table().recipeBook.recipePoint(new ResourceLocation("diamond_pickaxe")) == null, "diamond pickaxe hidden"));
        Showcase.step(2, () -> Showcase.recipeSearch("wooden_pickaxe"));
        Showcase.step(4, () -> Showcase.pointRecipe("wooden_pickaxe"));
        Showcase.step(3, () -> Showcase.screenClick(0));
        Showcase.step(10, () -> {
            TableRecipeGuide guide = Showcase.table().recipeBook.guide;
            Showcase.check("recipe available planks and sticks", guide.selected != null && guide.selected.craftable && guide.missingCount == 0, "missing=" + guide.missingCount);
            Showcase.pointAside();
        });
        Showcase.step(4, () -> Showcase.shot("0r3_available_guide"));
        Showcase.step(2, () -> Showcase.inventoryClick(36, 0));
        Showcase.step(3, () -> Showcase.recipeControl(64, 12));
        Showcase.step(3, () -> Showcase.check("recipe panel consumes carried click", Hands.carried().m_41613_() == 24 && ClientTables.get(TABLE) == null, "carried=" + Showcase.describe(Hands.carried())));
        Showcase.step(2, () -> Showcase.point(0, 0));
        Showcase.step(8, () -> Showcase.clickCell(0, 0, 1));
        Showcase.step(8, () -> {
            Showcase.check("recipe placed ingredient", Showcase.table().recipeBook.guide.placed[0], "first plank accepted");
            Showcase.shot("0r4_partial_recipe");
        });
        Showcase.step(2, () -> Showcase.inventoryClick(36, 0));
        Showcase.step(2, () -> Showcase.inventoryClick(38, 0));
        Showcase.step(2, () -> Showcase.point(0, 1));
        Showcase.step(8, () -> Showcase.clickCell(0, 1, 1));
        Showcase.step(8, () -> {
            Showcase.check("recipe wrong ingredient", Showcase.table().recipeBook.guide.wrong(1), "cobblestone instead of planks");
            Showcase.shot("0r5_wrong_ingredient");
        });
        Showcase.step(2, () -> Showcase.inventoryClick(38, 0));
        Showcase.step(2, () -> Showcase.clickCell(0, 1, 0));
        Showcase.step(6, () -> Showcase.inventoryClick(38, 0));
        Showcase.step(2, () -> Showcase.point(0, 0));
        Showcase.step(6, () -> Showcase.clickCell(0, 0, 0));
        Showcase.step(6, () -> Showcase.inventoryClick(36, 0));
        Showcase.step(6, () -> {
            ClientTables.Entry contents = ClientTables.get(TABLE);
            Showcase.check("recipe walkthrough restores inventory", (contents == null || contents.list().stream().allMatch(ItemStack::m_41619_)) && Hands.carried().m_41619_() && Minecraft.m_91087_().f_91074_.m_150109_().m_8020_(0).m_41613_() == 24, "planks=" + Minecraft.m_91087_().f_91074_.m_150109_().m_8020_(0).m_41613_());
        });
        Showcase.step(2, () -> Showcase.recipeControl(113, 12));
        Showcase.step(15, () -> {
            Showcase.check("recipe folded guide retained", Showcase.table().recipeBook.guide.selected != null, "selection retained");
            Showcase.pointAside();
            Showcase.shot("0r6_folded_reference");
            double[] at = Showcase.table().recipeBook.togglePoint();
            Showcase.pointGui(at[0], at[1]);
            Showcase.screenClick(0);
        });
        Showcase.step(15, () -> Showcase.recipeControl(113, 194));
        Showcase.step(3, () -> Showcase.check("recipe clear guide", Showcase.table().recipeBook.guide.selected == null, "cleared"));
        Showcase.tableRecipeLayoutScript();
        Showcase.step(2, () -> Showcase.recipeControl(113, 12));
        Showcase.step(15, Showcase::pointAside);
    }

    private static void tableRecipeLayoutScript() {
        Showcase.step(2, () -> Showcase.server(p -> {
            p.m_150109_().m_6836_(8, new ItemStack((ItemLike)Items.f_41837_));
            p.f_36096_.m_38946_();
        }));
        Showcase.step(8, () -> Showcase.recipeSearch("minecraft:oak_planks"));
        Showcase.step(3, () -> Showcase.pointRecipe("oak_planks"));
        Showcase.step(3, () -> Showcase.screenClick(0));
        Showcase.step(4, () -> Showcase.inventoryClick(44, 0));
        Showcase.step(3, () -> Showcase.point(2, 2));
        Showcase.step(6, () -> Showcase.clickCell(2, 2, 0));
        Showcase.step(8, () -> {
            TableRecipeGuide guide = Showcase.table().recipeBook.guide;
            Showcase.check("recipe shapeless follows placement", guide.placed[8] && guide.grid[0].m_43947_() && guide.missingCount == 0, "log at bottom right");
            Showcase.shot("0r7_shapeless_recipe");
        });
        Showcase.step(2, () -> Showcase.clickCell(2, 2, 0));
        Showcase.step(6, () -> Showcase.inventoryClick(44, 0));
        Showcase.step(4, () -> Showcase.server(p -> {
            p.m_150109_().m_6836_(8, ItemStack.f_41583_);
            p.f_36096_.m_38946_();
        }));
        Showcase.step(6, () -> Showcase.recipeSearch("minecraft:stick"));
        Showcase.step(3, () -> Showcase.pointRecipe("stick"));
        Showcase.step(3, () -> Showcase.screenClick(0));
        Showcase.step(6, () -> {
            TableRecipeGuide guide = Showcase.table().recipeBook.guide;
            Showcase.check("recipe narrow pattern centered", !guide.grid[1].m_43947_() && !guide.grid[4].m_43947_() && guide.grid[0].m_43947_(), "one-column pattern");
        });
        Showcase.step(2, () -> Showcase.inventoryClick(36, 0));
        Showcase.step(3, () -> Showcase.point(1, 0));
        Showcase.step(6, () -> Showcase.clickCell(1, 0, 1));
        Showcase.step(8, () -> {
            TableRecipeGuide guide = Showcase.table().recipeBook.guide;
            Showcase.check("recipe shaped follows offset", guide.placed[3] && (!guide.grid[0].m_43947_() || !guide.grid[6].m_43947_()) && guide.grid[1].m_43947_(), "pattern moved to left column");
            Showcase.shot("0r8_offset_recipe");
        });
        Showcase.step(2, () -> Showcase.inventoryClick(36, 0));
        Showcase.step(2, () -> Showcase.clickCell(1, 0, 0));
        Showcase.step(6, () -> Showcase.inventoryClick(36, 0));
        Showcase.step(3, () -> Showcase.recipeSearch("minecraft:wooden_axe"));
        Showcase.step(3, () -> Showcase.pointRecipe("wooden_axe"));
        Showcase.step(3, () -> Showcase.screenClick(0));
        Showcase.step(3, () -> Showcase.inventoryClick(37, 0));
        Showcase.step(3, () -> Showcase.point(2, 0));
        Showcase.step(6, () -> Showcase.clickCell(2, 0, 1));
        Showcase.step(8, () -> {
            TableRecipeGuide guide = Showcase.table().recipeBook.guide;
            Showcase.check("recipe shaped follows mirror", guide.placed[6] && guide.grid[7].m_43947_() && guide.grid[4].test(new ItemStack((ItemLike)Items.f_42647_)), "mirrored axe pattern");
            Showcase.shot("0r9_mirrored_recipe");
        });
        Showcase.step(2, () -> Showcase.inventoryClick(37, 0));
        Showcase.step(2, () -> Showcase.clickCell(2, 0, 0));
        Showcase.step(6, () -> Showcase.inventoryClick(37, 0));
        Showcase.step(4, () -> {
            BlockPos testPos = TABLE.m_7918_(100, 0, 0);
            TableRecipeGuide.Entry entry = Showcase.table().recipeBook.guide.entries.stream().filter(e -> e.holder.id().equals((Object)new ResourceLocation("wooden_pickaxe"))).findFirst().orElseThrow();
            Direction[] sides = new Direction[]{Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST};
            int[] firstCells = new int[]{0, 8, 6, 2};
            try {
                for (int i = 0; i < sides.length; ++i) {
                    ClientTables.Entry contents = new ClientTables.Entry();
                    contents.items[firstCells[i]] = new ItemStack((ItemLike)Items.f_42647_);
                    ClientTables.TABLES.put(testPos, contents);
                    TableRecipeGuide guide = new TableRecipeGuide(new TableStation(testPos, sides[i]));
                    guide.select(entry);
                    Showcase.check("recipe orientation " + String.valueOf(sides[i]), guide.placed[0], "world cell=" + firstCells[i]);
                }
            }
            finally {
                ClientTables.TABLES.remove(testPos);
            }
        });
        Showcase.step(2, () -> Showcase.recipeControl(113, 194));
        Showcase.step(6, () -> {
            LocalPlayer player = Minecraft.m_91087_().f_91074_;
            ClientTables.Entry contents = ClientTables.get(TABLE);
            Showcase.check("recipe layout checks restore inventory", (contents == null || contents.list().stream().allMatch(ItemStack::m_41619_)) && Hands.carried().m_41619_() && player.m_150109_().m_8020_(0).m_41613_() == 24 && player.m_150109_().m_8020_(1).m_41613_() == 12 && player.m_150109_().m_8020_(8).m_41619_(), "planks=" + player.m_150109_().m_8020_(0).m_41613_() + " sticks=" + player.m_150109_().m_8020_(1).m_41613_());
        });
    }

    private static void recipeControl(int x, int y) {
        double[] at = Showcase.table().recipeBook.controlPoint(x, y);
        Showcase.pointGui(at[0], at[1]);
        Showcase.screenClick(0);
    }

    private static void recipeSearch(String query) {
        Showcase.recipeControl(30, 34);
        Screen screen = Minecraft.m_91087_().f_91080_;
        screen.m_7933_(269, 0, 0);
        for (int i = 0; i < 96; ++i) {
            screen.m_7933_(259, 0, 0);
        }
        for (char c : query.toCharArray()) {
            if (c >= 'a' && c <= 'z') {
                screen.m_7933_(65 + c - 97, 0, 0);
            }
            screen.m_5534_(c, 0);
        }
        screen.m_7933_(256, 0, 0);
    }

    private static void pointRecipe(String id) {
        double[] at = Showcase.table().recipeBook.recipePoint(new ResourceLocation(id));
        Showcase.check("recipe found " + id, at != null, "reference lookup");
        if (at != null) {
            Showcase.pointGui(at[0], at[1]);
        }
    }

    private static void tableVisibilityScript() {
        Showcase.step(4, () -> Showcase.inventoryClick(38, 0));
        Showcase.step(3, () -> Showcase.point(1, 1));
        Showcase.step(6, () -> Showcase.clickCell(1, 1, 1));
        Showcase.step(3, () -> Showcase.point(1, 0));
        Showcase.step(6, () -> Showcase.clickCell(1, 0, 1));
        Showcase.step(6, () -> Showcase.inventoryClick(38, 0));
        Showcase.step(2, Showcase::pointAside);
        Showcase.step(8, () -> {
            Showcase.shot("05a_occlusion_solid");
            Showcase.checkTableOpacity("table cubes at rest", false);
        });
        Showcase.step(2, () -> Showcase.inventoryClick(40, 0));
        Showcase.step(3, () -> Showcase.point(0, 1));
        Showcase.step(1, () -> Showcase.shot("05b_occlusion_fading"));
        Showcase.step(6, () -> {
            Showcase.shot("05c_occlusion_clear");
            Showcase.checkTableOpacity("table far cell revealed", true);
        });
        Showcase.step(2, () -> Showcase.clickCell(0, 1, 1));
        Showcase.step(8, () -> {
            Direction side = Showcase.table().sideDirection();
            int centre = TableGrid.worldIndex(side, 1, 1);
            int far = TableGrid.worldIndex(side, 0, 1);
            int beside = TableGrid.worldIndex(side, 1, 0);
            Showcase.server(p -> {
                TableStore.Table stored = TableStore.get(p.m_284548_()).get(TABLE);
                boolean correct = stored != null && ((ItemStack)stored.items.get(centre)).m_150930_(Items.f_42594_) && ((ItemStack)stored.items.get(centre)).m_41613_() == 1 && ((ItemStack)stored.items.get(beside)).m_150930_(Items.f_42594_) && ((ItemStack)stored.items.get(beside)).m_41613_() == 1 && ((ItemStack)stored.items.get(far)).m_150930_(Items.f_42416_) && ((ItemStack)stored.items.get(far)).m_41613_() == 1;
                Showcase.check("placement behind the table cube", correct, "centre=" + centre + " far=" + far + " beside=" + beside + " stored=" + String.valueOf(stored == null ? "-" : stored.items));
            });
            Showcase.shot("05d_occlusion_placed");
        });
        Showcase.step(2, () -> Showcase.inventoryClick(40, 0));
        Showcase.step(2, Showcase::pointAside);
        Showcase.step(8, () -> {
            Showcase.shot("05e_occlusion_recovered");
            Showcase.checkTableOpacity("table cubes recover opacity", false);
        });
        Showcase.step(2, () -> Showcase.point(0, 1));
        Showcase.step(6, () -> Showcase.clickCell(0, 1, 0));
        Showcase.step(6, () -> Showcase.inventoryClick(40, 0));
        Showcase.step(2, () -> Showcase.point(1, 1));
        Showcase.step(6, () -> Showcase.clickCell(1, 1, 0));
        Showcase.step(6, () -> Showcase.inventoryClick(38, 0));
        Showcase.step(2, () -> Showcase.point(1, 0));
        Showcase.step(6, () -> Showcase.clickCell(1, 0, 0));
        Showcase.step(6, () -> Showcase.inventoryClick(38, 0));
        Showcase.step(2, Showcase::pointAside);
    }

    private static void checkTableOpacity(String name, boolean occluded) {
        int expectedHover;
        TableStation station = Showcase.table();
        if (station == null) {
            Showcase.check(name, false, "no table scene");
            return;
        }
        Direction side = station.sideDirection();
        int centre = TableGrid.worldIndex(side, 1, 1);
        int beside = TableGrid.worldIndex(side, 1, 0);
        double centreOpacity = station.itemOpacity(centre);
        double besideOpacity = station.itemOpacity(beside);
        int n = expectedHover = occluded ? TableGrid.worldIndex(side, 0, 1) : -1;
        boolean correct = station.hoverCell == expectedHover && besideOpacity >= 0.98 && (occluded ? centreOpacity < 0.6 : centreOpacity >= 0.98);
        Showcase.check(name, correct, String.format("centre=%.3f beside=%.3f hover=%d expected=%d", centreOpacity, besideOpacity, station.hoverCell, expectedHover));
    }

    private static void tableGlintVisibilityScript() {
        Showcase.step(4, () -> Showcase.server(p -> {
            p.m_150109_().m_8020_(2).m_41663_(Enchantments.f_44986_, 1);
            p.f_36096_.m_38946_();
        }));
        Showcase.step(8, () -> Showcase.inventoryClick(38, 0));
        Showcase.step(3, () -> Showcase.point(1, 1));
        Showcase.step(6, () -> Showcase.clickCell(1, 1, 1));
        Showcase.step(6, () -> Showcase.inventoryClick(38, 0));
        Showcase.step(2, Showcase::pointAside);
        Showcase.step(8, () -> Showcase.shot("05f_glint_solid"));
        Showcase.step(2, () -> Showcase.point(0, 1));
        Showcase.step(8, () -> {
            Showcase.shot("05g_glint_faded");
            Showcase.checkTableOpacity("glint cube reveals the far cell", true);
            int centre = TableGrid.worldIndex(Showcase.table().sideDirection(), 1, 1);
            ClientTables.Entry entry = ClientTables.get(TABLE);
            Showcase.check("faded cube keeps its glint", entry != null && entry.items[centre].m_41790_(), "stack=" + String.valueOf(entry == null ? "-" : entry.items[centre]));
        });
        Showcase.step(2, Showcase::pointAside);
        Showcase.step(8, () -> {
            Showcase.shot("05h_glint_recovered");
            Showcase.checkTableOpacity("glint cube recovers opacity", false);
        });
        Showcase.step(2, () -> Showcase.point(1, 1));
        Showcase.step(6, () -> Showcase.clickCell(1, 1, 0));
        Showcase.step(6, () -> Showcase.inventoryClick(38, 0));
        Showcase.step(6, () -> Showcase.server(p -> {
            ItemStack cobblestone = p.m_150109_().m_8020_(2);
            cobblestone.m_41749_("Enchantments");
            p.f_36096_.m_38946_();
            TableStore.Table stored = TableStore.get(p.m_284548_()).get(TABLE);
            boolean empty = stored == null || stored.items.stream().allMatch(ItemStack::m_41619_);
            Showcase.check("visibility checks restore ingredients", empty && cobblestone.m_150930_(Items.f_42594_) && cobblestone.m_41613_() == 40 && p.m_150109_().m_8020_(4).m_150930_(Items.f_42416_) && p.m_150109_().m_8020_(4).m_41613_() == 5 && p.f_36096_.m_142621_().m_41619_(), "tableEmpty=" + empty + " cobblestone=" + String.valueOf(cobblestone) + " iron=" + String.valueOf(p.m_150109_().m_8020_(4)) + " carried=" + String.valueOf(p.f_36096_.m_142621_()));
        }));
        Showcase.step(8, Showcase::pointAside);
    }

    private static TableStation table() {
        return Scene.station(TableStation.class);
    }

    private static Vec3 cellPoint(int row, int col) {
        int index = TableGrid.worldIndex(Scene.station.sideDirection(), row, col);
        return new Vec3((double)TABLE.m_123341_() + TableGrid.cellX(index), (double)TABLE.m_123342_() + 1.0, (double)TABLE.m_123343_() + TableGrid.cellZ(index));
    }

    private static void point(int row, int col) {
        Showcase.pointAt(Showcase.cellPoint(row, col));
    }

    private static void clickCell(int row, int col, int button) {
        int expected = TableGrid.worldIndex(Scene.station.sideDirection(), row, col);
        TableStation station = Showcase.table();
        if (station == null || station.hoverCell != expected) {
            REPORT.add("cursor missed cell r" + row + "c" + col + ": hover=" + String.valueOf(station == null ? "-" : Integer.valueOf(station.hoverCell)) + " expected=" + expected);
        }
        Showcase.screenClick(button);
        Showcase.screenRelease(button);
    }

    private static void clickResult() {
        TableStation station = Showcase.table();
        if (station == null || !station.hoverResult) {
            REPORT.add("cursor missed the result");
        }
        Showcase.screenClick(0);
    }

    private static FurnaceStation furnace() {
        return Scene.station(FurnaceStation.class);
    }

    private static void furnaceEditorScript() {
        FurnaceStation station = Showcase.furnace();
        StationKind kind = station.kind();
        FurnaceAppearance[] original = FurnaceLayouts.profiles(kind);
        int selected = FurnaceLayouts.selected(kind);
        boolean originalDev = Settings.developerMode();
        String prefix = "p" + kind.name().toLowerCase(Locale.ROOT);
        int[] originalGui = new int[1];
        StationKind[] otherKinds = (StationKind[])Arrays.stream(new StationKind[]{StationKind.FURNACE, StationKind.SMOKER, StationKind.BLAST_FURNACE}).filter(k -> k != kind).toArray(StationKind[]::new);
        FurnaceAppearance[][] otherProfiles = new FurnaceAppearance[][]{FurnaceLayouts.profiles(otherKinds[0]), FurnaceLayouts.profiles(otherKinds[1])};
        Showcase.step(2, () -> {
            Settings.set(Settings.DEVELOPER_MODE, false);
            Minecraft.m_91087_().f_91080_.m_7933_(71, 0, 0);
            double[] at = station.gridEditor.togglePoint();
            Showcase.check(prefix + " developer mode off", !station.gridEditor.editing() && !station.gridEditor.over(at[0], at[1]), "no G or icon");
            FurnaceAppearance.Region shifted = FurnaceAppearance.Region.VANILLA.with(0, 1.0).with(1, 1.0);
            FurnaceAppearance independent = FurnaceAppearance.VANILLA.with(0, shifted);
            double[] base = station.layout.center(0);
            Vec3 expected = FurnaceZones.point(station.pos, station.sideDirection(), station.layout.zone(0).face(), base[0] + 0.0625, base[1] + 0.0625, 0.0);
            Showcase.check(prefix + " independent pixel geometry", independent.point(station.layout, station.pos, station.sideDirection(), 0, base[0], base[1], 0.0, false).m_82554_(expected) < 1.0E-9, "one texture pixel on the original face");
            FurnaceAppearance.Region malformed = new FurnaceAppearance.Region(Double.NaN, 999.0, 999.0, -999.0, Double.POSITIVE_INFINITY, 999.0, -999.0, 999.0, -999.0, 999.0);
            Showcase.check(prefix + " malformed values bounded", malformed.u() == 0.0 && malformed.v() == 6.0 && malformed.itemScale() == 1.5 && malformed.out() == 0.0, "finite clamped values");
            Settings.set(Settings.DEVELOPER_MODE, true);
        });
        Showcase.step(3, () -> Showcase.inventoryClick(16, 0));
        Showcase.step(3, () -> {
            double[] at = station.gridEditor.togglePoint();
            Showcase.pointGui(at[0], at[1]);
            Showcase.screenClick(0);
        });
        Showcase.step(20, () -> {
            Showcase.check(prefix + " opens in scene", station.gridEditor.editing() && !station.showsInventory(), "inventory hidden");
            Showcase.shot(prefix + "_open");
        });
        Showcase.furnaceEditorSlider(0, 0.5);
        Showcase.furnaceEditorSlider(1, 0.5);
        Showcase.furnaceEditorSlider(2, 85.0);
        Showcase.furnaceEditorSlider(3, 90.0);
        Showcase.furnaceEditorSlider(4, 0.5);
        Showcase.furnaceEditorSlider(5, 80.0);
        Showcase.step(18, () -> {
            Showcase.check(prefix + " preview", Math.abs(station.geometry().input().u() - 0.5) < 0.01 && Math.abs(station.geometry().input().itemScale() - 0.8) < 0.01, "live geometry");
            Showcase.pointAt(station.geometry().center(station.layout, station.pos, station.sideDirection(), 0));
            Showcase.screenClick(1);
        });
        Showcase.step(3, () -> {
            Showcase.check(prefix + " editor consumes carried clicks", Hands.carried().m_150930_(Items.f_42413_) && Hands.carried().m_41613_() == 16 && ClientFurnaces.item(station.pos, 0).m_41619_() && ClientFurnaces.item(station.pos, 1).m_41619_(), "real stack untouched");
            Showcase.furnaceEditorControl(80.0, 90.0);
        });
        Showcase.furnaceEditorSlider(0, 120.0);
        Showcase.furnaceEditorSlider(1, 110.0);
        Showcase.furnaceEditorSlider(2, 0.25);
        Showcase.furnaceEditorSlider(3, -0.25);
        Showcase.step(18, () -> {
            Showcase.check(prefix + " advanced independent", Math.abs(station.geometry().input().itemU() - 0.25) < 0.01 && Math.abs(station.geometry().input().clickWidth() - 1.2) < 0.01, "item and target offsets");
            Showcase.shot(prefix + "_advanced");
            Minecraft.m_91087_().f_91080_.m_7933_(256, 0, 0);
        });
        Showcase.step(25, () -> {
            Showcase.check(prefix + " cancel", station.geometry().equals(original[selected]) && Scene.phase == Scene.Phase.ACTIVE, "saved profile restored");
            Showcase.inventoryClick(16, 0);
        });
        Showcase.step(3, () -> Minecraft.m_91087_().f_91080_.m_7933_(71, 0, 0));
        Showcase.step(20, () -> Showcase.furnaceEditorControl(80.0, 298.0));
        Showcase.furnaceEditorSlider(0, 0.5);
        Showcase.furnaceEditorSlider(4, 0.5);
        Showcase.step(18, () -> {
            Vec3 start = station.geometry().center(station.layout, station.pos, station.sideDirection(), 0);
            Showcase.pointAt(start);
            Showcase.screenClick(0);
            Showcase.pointAt(start.m_82549_(station.right().m_82490_(0.025)));
            double[] at = Picking.mouseGui();
            Minecraft.m_91087_().f_91080_.m_7979_(at[0], at[1], 0, 3.0, 0.0);
            Minecraft.m_91087_().f_91080_.m_6348_(at[0], at[1], 0);
        });
        Showcase.step(20, () -> {
            Showcase.check(prefix + " drag", station.geometry().input().u() > 0.7, "zone follows face drag");
            Showcase.furnaceEditorControl(160.0, 38.0);
        });
        Showcase.step(20, () -> {
            Showcase.check(prefix + " profile switching", station.geometry().equals(original[(selected + 1) % 3]), "independent profile");
            Showcase.furnaceEditorControl(18.0, 38.0);
        });
        Showcase.step(20, () -> {
            Showcase.check(prefix + " draft retained", station.geometry().input().u() > 0.7, "draft survived switching");
            Showcase.furnaceEditorControl(160.0, 65.0);
        });
        Showcase.furnaceEditorSlider(0, -0.25);
        Showcase.furnaceEditorSlider(1, 0.25);
        Showcase.furnaceEditorSlider(2, 90.0);
        Showcase.furnaceEditorSlider(4, 0.25);
        Showcase.step(15, () -> Showcase.furnaceEditorControl(160.0, 65.0));
        Showcase.furnaceEditorSlider(0, 0.25);
        Showcase.furnaceEditorSlider(1, 0.25);
        Showcase.furnaceEditorSlider(5, 90.0);
        Showcase.step(18, () -> {
            Showcase.check(prefix + " all slots independently edited", Math.abs(station.geometry().fuel().u() + 0.25) < 0.01 && Math.abs(station.geometry().result().itemScale() - 0.9) < 0.01, "fuel and output settings");
            Showcase.shot(prefix + "_all_zones");
            Showcase.furnaceEditorControl(160.0, 65.0);
        });
        Showcase.furnaceEditorSlider(0, 0.5);
        Showcase.furnaceEditorSlider(1, 0.5);
        Showcase.step(18, () -> {
            Showcase.check(prefix + " gauge preview", Math.abs(station.geometry().cooking().v() - 0.5) < 0.01, "indicator moved independently");
            Showcase.shot(prefix + "_gauge");
            Showcase.furnaceEditorControl(160.0, 65.0);
        });
        Showcase.furnaceEditorSlider(0, -0.5);
        Showcase.furnaceEditorSlider(1, 0.25);
        Showcase.furnaceEditorSlider(4, 0.125);
        Showcase.step(18, () -> {
            Showcase.check(prefix + " fuel gauge preview", Math.abs(station.geometry().burning().v() - 0.25) < 0.01 && Math.abs(station.geometry().cooking().v() - 0.5) < 0.01, "two independent indicators");
            Showcase.shot(prefix + "_fuel_gauge");
            originalGui[0] = (Integer)Minecraft.m_91087_().f_91066_.m_231928_().m_231551_();
            Minecraft.m_91087_().f_91066_.m_231928_().m_231514_((Object)3);
            Minecraft.m_91087_().m_5741_();
        });
        Showcase.step(8, () -> {
            Showcase.shot(prefix + "_gui_3");
            Minecraft.m_91087_().f_91066_.m_231928_().m_231514_((Object)originalGui[0]);
            Minecraft.m_91087_().m_5741_();
        });
        Showcase.step(8, () -> {
            Minecraft.m_91087_().f_91080_.m_7933_(87, 0, 0);
            Showcase.check(prefix + " movement blocked", station.gridEditor.editing() && Scene.phase == Scene.Phase.ACTIVE, "same scene");
            Showcase.furnaceEditorControl(45.0, 323.0);
        });
        Showcase.step(25, () -> {
            Showcase.check(prefix + " saves JSON", !station.gridEditor.editing() && Files.exists(FurnaceLayouts.path(), new LinkOption[0]) && FurnaceLayouts.profiles(kind)[selected].input().u() > 0.7, "local profile saved");
            boolean boundedGlow = true;
            for (FurnaceLayout.Glow glow : station.layout.glows) {
                for (int zone = 0; zone < 3; ++zone) {
                    double[] rect;
                    if (station.layout.zone(zone).face() != glow.face() || (rect = station.geometry().glowRect(station.layout, glow, zone)) == null) continue;
                    boundedGlow &= rect[0] >= 0.0 && rect[1] <= 1.0 && rect[2] >= 0.0 && rect[3] <= 1.0;
                }
            }
            Showcase.check(prefix + " warm light bounded", boundedGlow, "light remains on the configured face");
            Showcase.check(prefix + " furnace types independent", Arrays.equals(otherProfiles[0], FurnaceLayouts.profiles(otherKinds[0])) && Arrays.equals(otherProfiles[1], FurnaceLayouts.profiles(otherKinds[1])), "other furnaces unchanged");
            Settings.set(Settings.DEVELOPER_MODE, false);
            Minecraft.m_91087_().f_91080_.m_7933_(71, 0, 0);
            Showcase.check(prefix + " saved layout with dev off", !station.gridEditor.editing() && station.geometry().input().u() > 0.7, "geometry applies, editor unavailable");
        });
        int i = 0;
        while (i < 3) {
            int zone = i++;
            Showcase.step(3, () -> Showcase.pointAt(station.geometry().center(station.layout, station.pos, station.sideDirection(), zone)));
            Showcase.step(5, () -> Showcase.check(prefix + " adjusted picking " + zone, station.hoverZone == zone, "hover=" + station.hoverZone));
        }
        Showcase.step(3, () -> Showcase.inventoryClick(16, 0));
        Showcase.step(3, () -> Showcase.pointAt(station.geometry().center(station.layout, station.pos, station.sideDirection(), 1)));
        Showcase.step(5, () -> Showcase.clickZone(1, 1));
        Showcase.step(8, () -> {
            Showcase.check(prefix + " real placement", ClientFurnaces.item(station.pos, 1).m_41613_() == 1 && Hands.carried().m_41613_() == 15, "one coal in logical fuel slot");
            Showcase.inventoryClick(16, 0);
            Showcase.server(p -> p.m_284548_().m_46597_(station.pos, (BlockState)p.m_284548_().m_8055_(station.pos).m_61124_((Property)AbstractFurnaceBlock.f_48684_, (Comparable)Boolean.valueOf(true))));
        });
        Showcase.step(8, () -> {
            Showcase.shot(prefix + "_adjusted_warm_light");
            Showcase.server(p -> p.m_284548_().m_46597_(station.pos, (BlockState)p.m_284548_().m_8055_(station.pos).m_61124_((Property)AbstractFurnaceBlock.f_48684_, (Comparable)Boolean.valueOf(false))));
        });
        Showcase.step(3, () -> Showcase.pointAt(station.geometry().center(station.layout, station.pos, station.sideDirection(), 1)));
        Showcase.step(5, () -> Showcase.clickZone(1, 0));
        Showcase.step(8, () -> Showcase.inventoryClick(16, 0));
        Showcase.step(3, () -> {
            Settings.set(Settings.DEVELOPER_MODE, true);
            Minecraft.m_91087_().f_91080_.m_7933_(71, 0, 0);
        });
        Showcase.step(20, () -> Showcase.furnaceEditorSliderNow(0, -0.5));
        Showcase.step(18, () -> Settings.set(Settings.DEVELOPER_MODE, false));
        Showcase.step(25, () -> {
            Showcase.check(prefix + " disabling dev cancels draft", !station.gridEditor.editing() && station.geometry().equals(FurnaceLayouts.profiles(kind)[selected]), "saved geometry restored");
            Showcase.check(prefix + " inventory restored", Hands.carried().m_41619_() && Minecraft.m_91087_().f_91074_.m_150109_().m_8020_(16).m_41613_() == 16 && ClientFurnaces.item(station.pos, 1).m_41619_(), "coal returned");
            Showcase.check(prefix + " restore profiles", FurnaceLayouts.save(kind, original, selected), "original settings restored");
            Settings.set(Settings.DEVELOPER_MODE, originalDev);
            Showcase.pointAside();
        });
        Showcase.step(25, () -> {});
    }

    private static Runnable surfaceEditorScript() {
        SurfaceStation station = (SurfaceStation)Scene.station;
        StationKind kind = station.kind();
        StationKind other = kind == StationKind.SMITHING ? StationKind.CARTOGRAPHY : StationKind.SMITHING;
        String prefix = kind == StationKind.SMITHING ? "sx" : "mx";
        SurfaceAppearance[] original = SurfaceLayouts.profiles(kind);
        SurfaceAppearance[] untouched = SurfaceLayouts.profiles(other);
        int selected = SurfaceLayouts.selected(kind);
        boolean dev = Settings.developerMode();
        int[] stackCount = new int[]{0};
        int[] guiScale = new int[]{0};
        Showcase.step(2, () -> {
            Settings.set(Settings.DEVELOPER_MODE, false);
            Minecraft.m_91087_().f_91080_.m_7933_(71, 0, 0);
            double[] at = station.gridEditor.togglePoint();
            Showcase.pointGui(at[0], at[1]);
            Showcase.screenClick(0);
            Showcase.check(prefix + " developer gate", !station.gridEditor.editing() && !station.gridEditor.over(at[0], at[1]), "G and icon unavailable");
            SurfaceAppearance moved = SurfaceAppearance.VANILLA.with(0, SurfaceAppearance.Region.VANILLA.with(0, 1.0).with(1, 1.0).with(5, 1.0));
            Vec3 p = moved.point(kind, Vec3.f_82478_, Direction.SOUTH, 0);
            double expectedX = kind == StationKind.SMITHING ? -0.1975 : 0.0225;
            double expectedZ = kind == StationKind.SMITHING ? -0.057499999999999996 : 0.10250000000000001;
            Showcase.check(prefix + " independent pixel geometry", p.m_82554_(new Vec3(expectedX, 0.0625, expectedZ)) < 1.0E-9, "independent south-facing reference=" + String.valueOf(p));
            SurfaceAppearance.Region malformed = new SurfaceAppearance.Region(100.0, -100.0, Double.NaN, 100.0, 100.0, Double.POSITIVE_INFINITY, Double.NaN);
            Showcase.check(prefix + " malformed values bounded", malformed.x() == 6.0 && malformed.z() == -6.0 && malformed.rotation() == 0.0 && malformed.width() == 1.5 && malformed.height() == 0.0 && malformed.itemScale() == 1.0, "finite values");
            Settings.set(Settings.DEVELOPER_MODE, true);
            stackCount[0] = Minecraft.m_91087_().f_91074_.m_150109_().m_8020_(0).m_41613_();
            Showcase.invClick(0, 0);
        });
        Showcase.step(4, () -> {
            double[] at = station.gridEditor.togglePoint();
            Showcase.pointGui(at[0], at[1]);
            Showcase.screenClick(0);
        });
        Showcase.step(20, () -> {
            Showcase.check(prefix + " editor opens", station.gridEditor.editing() && !station.showsInventory() && Minecraft.m_91087_().f_91080_ instanceof SceneScreen, "same scene");
            Showcase.pointAside();
            Showcase.shot(prefix + "0_editor_open");
        });
        Showcase.surfaceSlider(0, 0.5);
        Showcase.surfaceSlider(1, 0.25);
        Showcase.surfaceSlider(2, 20.0);
        Showcase.surfaceSlider(3, 90.0);
        Showcase.surfaceSlider(4, 85.0);
        Showcase.surfaceSlider(5, 0.25);
        Showcase.surfaceSlider(6, 90.0);
        Showcase.step(18, () -> {
            Showcase.check(prefix + " live preview", Math.abs(station.geometry().first().rotation() - 20.0) < 0.1 && Math.abs((double)station.surfaceScale(0) - 0.9) < 0.01, "rotation and size");
            Showcase.pointAside();
            Showcase.shot(prefix + "1_editor_preview");
            Showcase.pointAt(station.restPoint(0));
            Showcase.screenClick(1);
        });
        Showcase.step(4, () -> {
            Showcase.check(prefix + " editing does not move ingredients", Showcase.carried().m_41613_() == stackCount[0] && station.slot(0).m_41619_(), "carried stack unchanged");
            Minecraft.m_91087_().f_91080_.m_7933_(256, 0, 0);
        });
        Showcase.step(25, () -> {
            Showcase.check(prefix + " cancel restores saved", !station.gridEditor.editing() && station.geometry().equals(original[selected]), "scene stays active");
            Showcase.invClick(0, 0);
        });
        Showcase.step(4, () -> Minecraft.m_91087_().f_91080_.m_7933_(71, 0, 0));
        Showcase.step(20, () -> Showcase.surfaceControl(90.0, 314.0));
        Showcase.surfaceSlider(0, -0.25);
        Showcase.surfaceSlider(1, -0.25);
        Showcase.surfaceSlider(2, 12.0);
        Showcase.surfaceSlider(3, 90.0);
        Showcase.surfaceSlider(4, 90.0);
        Showcase.surfaceSlider(5, 0.25);
        Showcase.surfaceSlider(6, 85.0);
        Showcase.step(18, () -> Showcase.surfaceControl(150.0, 38.0));
        Showcase.step(18, () -> {
            Showcase.check(prefix + " profiles separate", station.geometry().equals(original[(selected + 1) % 3]), "other profile");
            Showcase.surfaceControl(18.0, 38.0);
        });
        Showcase.step(20, () -> {
            Showcase.check(prefix + " profile draft retained", Math.abs(station.geometry().first().rotation() - 12.0) < 0.1, "switch back");
            Vec3 at = station.restPoint(0);
            Showcase.pointAt(at);
            Showcase.screenClick(0);
            Showcase.pointAt(at.m_82549_(station.right().m_82490_(0.025)));
            double[] mouse = Picking.mouseGui();
            Minecraft.m_91087_().f_91080_.m_7979_(mouse[0], mouse[1], 0, 3.0, 0.0);
            Minecraft.m_91087_().f_91080_.m_6348_(mouse[0], mouse[1], 0);
        });
        Showcase.step(20, () -> {
            Showcase.check(prefix + " world drag", station.geometry().first().x() > 0.1, "drag along working right axis");
            Showcase.pointAside();
            Showcase.shot(prefix + "2_editor_drag");
            Showcase.surfaceControl(150.0, 65.0);
        });
        Showcase.surfaceSlider(0, -0.5);
        Showcase.surfaceSlider(1, 0.25);
        Showcase.surfaceSlider(2, -12.0);
        Showcase.surfaceSlider(5, 0.5);
        Showcase.surfaceSlider(6, 90.0);
        if (kind == StationKind.SMITHING) {
            Showcase.step(5, () -> Showcase.surfaceControl(150.0, 65.0));
            Showcase.surfaceSlider(0, -0.25);
            Showcase.surfaceSlider(1, 0.25);
            Showcase.surfaceSlider(2, -20.0);
            Showcase.surfaceSlider(5, 0.25);
            Showcase.surfaceSlider(6, 85.0);
        }
        Showcase.step(18, () -> {
            Showcase.check(prefix + " zones independent", station.geometry().first().rotation() > 11.9 && station.geometry().second().rotation() < -11.9, "independent rotation and heights");
            Minecraft.m_91087_().f_91080_.m_7933_(87, 0, 0);
            Showcase.check(prefix + " movement blocked", Scene.phase == Scene.Phase.ACTIVE && station.gridEditor.editing(), "still editing");
            guiScale[0] = (Integer)Minecraft.m_91087_().f_91066_.m_231928_().m_231551_();
            Minecraft.m_91087_().f_91066_.m_231928_().m_231514_((Object)3);
            Minecraft.m_91087_().m_5741_();
        });
        Showcase.step(8, () -> {
            Showcase.pointAside();
            Showcase.shot(prefix + "3_editor_gui3");
            Minecraft.m_91087_().f_91066_.m_231928_().m_231514_((Object)guiScale[0]);
            Minecraft.m_91087_().m_5741_();
        });
        Showcase.step(8, () -> Showcase.surfaceControl(40.0, 343.0));
        Showcase.step(25, () -> {
            Showcase.check(prefix + " saved JSON", !station.gridEditor.editing() && Files.exists(SurfaceLayouts.path(), new LinkOption[0]) && SurfaceLayouts.profiles(kind)[selected].first().rotation() == 12.0, "local profiles written");
            Showcase.check(prefix + " station types independent", Arrays.equals(untouched, SurfaceLayouts.profiles(other)), "other station untouched");
            Settings.set(Settings.DEVELOPER_MODE, false);
            Minecraft.m_91087_().f_91080_.m_7933_(71, 0, 0);
            Showcase.check(prefix + " saved geometry with dev off", !station.gridEditor.editing() && station.geometry().first().rotation() > 11.9, "saved layout remains active");
        });
        int i = 0;
        while (i < SurfaceAppearance.count(kind)) {
            int spot = i++;
            Showcase.step(3, () -> Showcase.pointAt(station.restPoint(spot)));
            Showcase.step(4, () -> Showcase.check(prefix + " edited picking " + spot, station.surfaceSlot(false) == spot, "raised rotated contact plane"));
        }
        Showcase.step(3, () -> {
            Settings.set(Settings.DEVELOPER_MODE, true);
            Minecraft.m_91087_().f_91080_.m_7933_(71, 0, 0);
        });
        Showcase.step(18, () -> Showcase.surfaceControl(90.0, 314.0));
        Showcase.step(25, () -> {
            Showcase.check(prefix + " reset preview", station.geometry().equals(SurfaceAppearance.VANILLA), "vanilla draft");
            Settings.set(Settings.DEVELOPER_MODE, false);
        });
        Showcase.step(25, () -> {
            Showcase.check(prefix + " disabling cancels draft", !station.gridEditor.editing() && station.geometry().first().rotation() > 11.9, "saved layout restored");
            Showcase.check(prefix + " inventory restored", Showcase.carried().m_41619_() && Minecraft.m_91087_().f_91074_.m_150109_().m_8020_(0).m_41613_() == stackCount[0], "no items created or lost");
            Showcase.pointAside();
        });
        return () -> {
            Showcase.check(prefix + " restore profiles", SurfaceLayouts.save(kind, original, selected), "original profiles restored");
            Settings.set(Settings.DEVELOPER_MODE, dev);
        };
    }

    private static Runnable spatialEditorScript() {
        SpatialTarget target = (SpatialTarget)((Object)Scene.station);
        ClientStation station = target.layoutStation();
        StationKind kind = station.kind();
        SpatialEditor editor = target.layoutEditor();
        String prefix = "x" + act;
        SpatialAppearance[] original = SpatialLayouts.profiles(kind);
        StationKind otherKind = kind == StationKind.BREWING ? StationKind.CHISELED_BOOKSHELF : StationKind.BREWING;
        SpatialAppearance[] otherProfiles = SpatialLayouts.profiles(otherKind);
        int selected = SpatialLayouts.selected(kind);
        boolean dev = Settings.developerMode();
        ItemStack hand = Showcase.carried().m_41777_();
        int[] guiScale = new int[]{0};
        Showcase.step(2, () -> Showcase.lambda$spatialEditorScript$614(editor, prefix, kind));
        Showcase.step(5, () -> {
            Showcase.pointAside();
            Showcase.shot(prefix + "0_gear");
        });
        Showcase.step(2, () -> {
            double[] gear = editor.togglePoint();
            Showcase.pointGui(gear[0], gear[1]);
            Showcase.check(prefix + " gear offered", editor.over(gear[0], gear[1]) && !editor.visible(), "visible=" + editor.visible() + " developer=" + Settings.developerMode() + " busy=" + station.busy() + " at=" + gear[0] + "," + gear[1]);
            Showcase.screenClick(0);
        });
        Showcase.step(22, () -> {
            Showcase.check(prefix + " editor opens", editor.editing() && !station.showsInventory(), "same scene");
            Showcase.pointAside();
            Showcase.shot(prefix + "0_editor_open");
        });
        Showcase.spatialSlider(0, 0.25);
        Showcase.spatialSlider(1, -0.25);
        Showcase.spatialSlider(2, 5.0);
        Showcase.spatialSlider(3, 95.0);
        Showcase.spatialSlider(4, 95.0);
        Showcase.spatialSlider(5, 0.25);
        Showcase.spatialSlider(6, 90.0);
        Showcase.step(22, () -> {
            Showcase.check(prefix + " whole layout preview", Math.abs(target.geometry().group().rotation() - 5.0) < 0.1, "smooth transform");
            Showcase.pointAside();
            Showcase.shot(prefix + "1_editor_preview");
            Showcase.pointAt(target.layoutPoint(0));
            Showcase.screenClick(1);
            Showcase.check(prefix + " preview leaves items intact", ItemStack.m_41728_((ItemStack)hand, (ItemStack)Showcase.carried()), "no menu click");
            Minecraft.m_91087_().f_91080_.m_7933_(256, 0, 0);
        });
        Showcase.step(25, () -> {
            Showcase.check(prefix + " cancel restores saved", !editor.editing() && target.geometry().equals(original[selected]), "still at station");
            Minecraft.m_91087_().f_91080_.m_7933_(71, 0, 0);
        });
        Showcase.step(20, () -> Showcase.spatialControl(90.0, 314.0));
        Showcase.spatialSlider(0, 0.125);
        Showcase.spatialSlider(1, -0.125);
        Showcase.spatialSlider(2, 3.0);
        Showcase.spatialSlider(3, 95.0);
        Showcase.spatialSlider(4, 95.0);
        Showcase.spatialSlider(5, 0.125);
        Showcase.spatialSlider(6, 95.0);
        Showcase.step(15, () -> Showcase.spatialControl(150.0, 65.0));
        Showcase.spatialSlider(0, 0.25);
        Showcase.spatialSlider(1, 0.125);
        Showcase.spatialSlider(2, -8.0);
        Showcase.spatialSlider(6, 90.0);
        Showcase.step(20, () -> {
            Showcase.check(prefix + " slots independent", target.geometry().region(0).rotation() < -7.9 && target.geometry().region(1).rotation() == 0.0, "only first slot changed");
            Vec3 at = target.layoutPoint(0);
            Showcase.pointAt(at);
            Showcase.screenClick(0);
            Showcase.pointAt(at.m_82549_(target.layoutFrame().across().m_82490_(0.0125)));
            double[] mouse = Picking.mouseGui();
            Minecraft.m_91087_().f_91080_.m_7979_(mouse[0], mouse[1], 0, 3.0, 0.0);
            Minecraft.m_91087_().f_91080_.m_6348_(mouse[0], mouse[1], 0);
        });
        Showcase.step(20, () -> {
            Showcase.check(prefix + " direct world drag", target.geometry().region(0).x() > 0.3, "dragged first spot");
            Showcase.pointAside();
            Showcase.shot(prefix + "2_editor_drag");
        });
        if (kind == StationKind.BREWING) {
            Showcase.step(3, () -> {
                Vec3 at = target.layoutPoint(3);
                Showcase.pointAt(at);
                Showcase.screenClick(0);
                Showcase.pointAt(at.m_82520_(0.0, 0.0125, 0.0));
                double[] mouse = Picking.mouseGui();
                Minecraft.m_91087_().f_91080_.m_7979_(mouse[0], mouse[1], 0, 0.0, -3.0);
                Minecraft.m_91087_().f_91080_.m_6348_(mouse[0], mouse[1], 0);
            });
            Showcase.step(20, () -> Showcase.check(prefix + " bowl drag at eye level", target.geometry().region(3).height() > 0.1, "raised the bowl directly"));
        }
        if (kind == StationKind.ENCHANTING) {
            Showcase.step(3, () -> {
                Vec3 at = target.layoutPoint(4);
                Showcase.pointAt(at);
                Showcase.screenClick(0);
                Showcase.pointAt(at.m_82520_(0.0, 0.0125, 0.0));
                double[] mouse = Picking.mouseGui();
                Minecraft.m_91087_().f_91080_.m_7979_(mouse[0], mouse[1], 0, 0.0, -3.0);
                Minecraft.m_91087_().f_91080_.m_6348_(mouse[0], mouse[1], 0);
            });
            Showcase.step(20, () -> {
                Showcase.check(prefix + " floating offer drag", target.geometry().region(4).height() > 0.1 && target.geometry().region(1).height() == 0.0, "offer moves independently");
                Showcase.shot(prefix + "2b_offer_drag");
            });
        }
        Showcase.step(3, () -> Showcase.spatialControl(150.0, 38.0));
        Showcase.step(20, () -> {
            Showcase.check(prefix + " profiles separate", target.geometry().equals(original[(selected + 1) % 3]), "other profile");
            Showcase.spatialControl(18.0, 38.0);
        });
        Showcase.step(20, () -> {
            Showcase.check(prefix + " draft retained", target.geometry().region(0).rotation() < -7.9, "back to edited profile");
            Minecraft.m_91087_().f_91080_.m_7933_(87, 0, 0);
            Showcase.check(prefix + " movement blocked", editor.editing() && Scene.phase == Scene.Phase.ACTIVE, "no accidental exit");
            guiScale[0] = (Integer)Minecraft.m_91087_().f_91066_.m_231928_().m_231551_();
            Minecraft.m_91087_().f_91066_.m_231928_().m_231514_((Object)3);
            Minecraft.m_91087_().m_5741_();
        });
        Showcase.step(12, () -> {
            Showcase.pointAside();
            Showcase.shot(prefix + "3_editor_gui3");
            Minecraft.m_91087_().f_91066_.m_231928_().m_231514_((Object)guiScale[0]);
            Minecraft.m_91087_().m_5741_();
        });
        Showcase.step(12, () -> Showcase.spatialControl(40.0, 343.0));
        Showcase.step(25, () -> {
            Showcase.check(prefix + " saved JSON", !editor.editing() && Files.exists(SpatialLayouts.path(), new LinkOption[0]) && SpatialLayouts.profiles(kind)[selected].region(0).rotation() == -8.0, "saved local profile");
            Showcase.check(prefix + " station profiles independent", Arrays.equals(otherProfiles, SpatialLayouts.profiles(otherKind)), "other type untouched");
            if (kind == StationKind.CHISELED_BOOKSHELF) {
                Minecraft mc = Minecraft.m_91087_();
                BlockState state = mc.f_91073_.m_8055_(station.pos);
                BakedModel model = mc.m_91289_().m_110910_(state);
                ModelData data = model.getModelData((BlockAndTintGetter)mc.f_91073_, station.pos, state, ModelData.EMPTY);
                boolean occupiedPicture = false;
                for (Direction face : new Direction[]{null, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN}) {
                    occupiedPicture |= model.getQuads(state, face, RandomSource.m_216335_((long)1L), data, null).stream().anyMatch(q -> q.m_173410_().m_245424_().m_246162_().m_135815_().contains("chiseled_bookshelf_occupied"));
                }
                Showcase.check(prefix + " original spines hidden", !occupiedPicture, "real block state untouched");
                ShelfBooks.Pose slotted = ShelfBooks.inSlot(station.pos, station.sideDirection(), 0, 0.0);
                Showcase.check(prefix + " book and hand contact agree", slotted.at(0.0, 0.0, 0.016).m_82554_(target.layoutPoint(0)) < 1.0E-8, "shared spine");
            }
            Settings.set(Settings.DEVELOPER_MODE, false);
            Minecraft.m_91087_().f_91080_.m_7933_(71, 0, 0);
            Showcase.check(prefix + " saved with developer off", !editor.editing() && target.geometry().region(0).rotation() < -7.9, "geometry still active");
        });
        Showcase.step(2, () -> Showcase.check(prefix + " settings profile switch", LayoutProfiles.select(kind, (selected + 1) % 3), "saved selection changed"));
        Showcase.step(25, () -> {
            Showcase.check(prefix + " settings profile applies", target.geometry().equals(original[(selected + 1) % 3]), "actual world geometry changed");
            Showcase.check(prefix + " settings preserve geometry", SpatialLayouts.profiles(kind)[selected].region(0).rotation() == -8.0, "edited profile retained");
            Showcase.check(prefix + " settings restore selection", LayoutProfiles.select(kind, selected), "same profiles as the scene editor");
        });
        Showcase.step(25, () -> Showcase.check(prefix + " selected geometry returns", target.geometry().region(0).rotation() == -8.0, "smoothly restored"));
        int i = 0;
        while (i < SpatialAppearance.count(kind)) {
            int slot = i++;
            Showcase.step(3, () -> Showcase.pointAt(target.layoutPoint(slot)));
            Showcase.step(5, () -> {
                Showcase.check(prefix + " edited picking " + slot, target.layoutPick() == slot, "surface contact");
                if (kind == StationKind.CHISELED_BOOKSHELF) {
                    Showcase.check(prefix + " initial use picking " + slot, ShelfStation.slotAt(station.pos, station.sideDirection(), target.layoutPoint(slot)) == slot, "initial use and vanilla fallback follow layout");
                }
            });
        }
        Showcase.step(3, () -> {
            Settings.set(Settings.DEVELOPER_MODE, true);
            Minecraft.m_91087_().f_91080_.m_7933_(71, 0, 0);
        });
        Showcase.step(20, () -> Showcase.spatialControl(90.0, 314.0));
        Showcase.step(25, () -> {
            Showcase.check(prefix + " reset preview", target.geometry().equals(SpatialAppearance.VANILLA), "vanilla draft");
            Settings.set(Settings.DEVELOPER_MODE, false);
        });
        Showcase.step(25, () -> {
            Showcase.check(prefix + " disabling cancels draft", !editor.editing() && target.geometry().region(0).rotation() < -7.9, "saved profile restored");
            Showcase.pointAside();
        });
        return () -> {
            Showcase.check(prefix + " restore profiles", SpatialLayouts.save(kind, original, selected), "original profiles restored");
            Settings.set(Settings.DEVELOPER_MODE, dev);
        };
    }

    private static void spatialControl(double x, double y) {
        SpatialEditor editor = ((SpatialTarget)((Object)Scene.station)).layoutEditor();
        double[] at = editor.controlPoint(x, y);
        Showcase.pointGui(at[0], at[1]);
        Showcase.screenClick(0);
        Minecraft.m_91087_().f_91080_.m_6348_(at[0], at[1], 0);
    }

    private static void spatialSlider(int row, double value) {
        Showcase.step(3, () -> {
            SpatialEditor editor = ((SpatialTarget)((Object)Scene.station)).layoutEditor();
            double[] at = editor.sliderPoint(row, value);
            Showcase.pointGui(at[0], at[1]);
            Showcase.screenClick(0);
            Minecraft.m_91087_().f_91080_.m_6348_(at[0], at[1], 0);
        });
    }

    private static void surfaceControl(double x, double y) {
        SurfaceStation station = (SurfaceStation)Scene.station;
        double[] at = station.gridEditor.controlPoint(x, y);
        Showcase.pointGui(at[0], at[1]);
        Showcase.screenClick(0);
        Minecraft.m_91087_().f_91080_.m_6348_(at[0], at[1], 0);
    }

    private static void surfaceSlider(int row, double value) {
        Showcase.step(3, () -> {
            SurfaceStation station = (SurfaceStation)Scene.station;
            double[] at = station.gridEditor.sliderPoint(row, value);
            Showcase.pointGui(at[0], at[1]);
            Showcase.screenClick(0);
            Minecraft.m_91087_().f_91080_.m_6348_(at[0], at[1], 0);
        });
    }

    private static void furnaceEditorControl(double x, double y) {
        double[] at = Showcase.furnace().gridEditor.controlPoint(x, y);
        Showcase.pointGui(at[0], at[1]);
        Showcase.screenClick(0);
        Minecraft.m_91087_().f_91080_.m_6348_(at[0], at[1], 0);
    }

    private static void furnaceEditorSliderNow(int row, double value) {
        double[] at = Showcase.furnace().gridEditor.sliderPoint(row, value);
        Showcase.pointGui(at[0], at[1]);
        Showcase.screenClick(0);
        Minecraft.m_91087_().f_91080_.m_6348_(at[0], at[1], 0);
    }

    private static void furnaceEditorSlider(int row, double value) {
        Showcase.step(3, () -> Showcase.furnaceEditorSliderNow(row, value));
    }

    private static void developerOptionsScript() {
        Showcase.step(8, () -> {
            Minecraft mc = Minecraft.m_91087_();
            ModContainer mod = (ModContainer)ModList.get().getModContainerById("cinematic").orElseThrow();
            mc.m_91152_((Screen)((ConfigScreenHandler.ConfigScreenFactory)mod.getCustomExtension(ConfigScreenHandler.ConfigScreenFactory.class).orElseThrow()).screenFunction().apply(mc, null));
        });
        Showcase.step(15, () -> {
            Showcase.check("advanced settings tab opens", Showcase.clickSettingsSection((GuiEventListener)Minecraft.m_91087_().f_91080_, Component.m_237115_((String)"cinematic.settings.tab.advanced").getString()), "tab button");
            Screen patt333918$temp = Minecraft.m_91087_().f_91080_;
            if (patt333918$temp instanceof SettingsScreen) {
                SettingsScreen settings = (SettingsScreen)patt333918$temp;
                settings.showDeveloper();
            }
        });
        Showcase.step(18, () -> {
            Showcase.verifyProfileControls();
            Showcase.shot("pz_developer_settings");
            Showcase.check("developer mode defaults off", (Boolean)Settings.DEVELOPER_MODE.getDefault() == false, "editor access is opt-in");
        });
        Showcase.step(3, () -> Minecraft.m_91087_().m_91152_(null));
    }

    private static void profileWidgets(GuiEventListener node, List<Object> result) {
        CycleButton cycle;
        if (node instanceof CycleButton && (cycle = (CycleButton)node).m_168883_() instanceof Integer || node.getClass().getName().endsWith("IntegerSliderEntry")) {
            result.add(node);
        }
        if (node instanceof ContainerEventHandler) {
            ContainerEventHandler parent = (ContainerEventHandler)node;
            for (GuiEventListener child : parent.m_6702_()) {
                Showcase.profileWidgets(child, result);
            }
        }
    }

    private static void verifyProfileControls() {
        ArrayList<Object> selectors = new ArrayList<Object>();
        Showcase.profileWidgets((GuiEventListener)Minecraft.m_91087_().f_91080_, selectors);
        Showcase.check("settings have a profile for every built-in station", selectors.size() == LayoutProfiles.supportedCount(), "selectors=" + selectors.size());
        if (selectors.size() != LayoutProfiles.supportedCount()) {
            return;
        }
        int selected = LayoutProfiles.selected(StationKind.TABLE);
        int next = (selected + 1) % 3;
        Object first = selectors.get(0);
        try {
            if (first instanceof CycleButton) {
                CycleButton cycle = (CycleButton)first;
                cycle.m_5691_();
                Showcase.check("profile choice waits for Done", LayoutProfiles.selected(StationKind.TABLE) == selected && cycle.m_168883_().equals(next), "cancel does not write a profile");
                while (!cycle.m_168883_().equals(selected)) {
                    cycle.m_5691_();
                }
            } else {
                first.getClass().getMethod("setValue", Integer.TYPE).invoke(first, next + 1);
                Showcase.check("Cloth unsaved profile stays local", LayoutProfiles.selected(StationKind.TABLE) == selected, "cancel does not write a profile");
                first.getClass().getMethod("save", new Class[0]).invoke(first, new Object[0]);
                Showcase.check("Cloth profile slider saves", LayoutProfiles.selected(StationKind.TABLE) == next, "shared JSON selection");
                first.getClass().getMethod("setValue", Integer.TYPE).invoke(first, selected + 1);
                first.getClass().getMethod("save", new Class[0]).invoke(first, new Object[0]);
            }
            Showcase.check("settings profile restored", LayoutProfiles.selected(StationKind.TABLE) == selected, "test preserves selection");
            int anvilProfile = LayoutProfiles.selected(StationKind.ANVIL);
            Showcase.check("all station profile files writable", LayoutProfiles.select(StationKind.ANVIL, (anvilProfile + 1) % 3) && LayoutProfiles.select(StationKind.ANVIL, anvilProfile), "format migration and selection saved");
        }
        catch (ReflectiveOperationException error) {
            Showcase.check("profile control API", false, error.toString());
        }
    }

    private static boolean clickSettingsSection(GuiEventListener node, String label) {
        AbstractButton button;
        AbstractButton target = null;
        if (node instanceof AbstractButton && (button = (AbstractButton)node).m_6035_().getString().equals(label)) {
            target = button;
        }
        if (target == null && node instanceof ContainerEventHandler) {
            ContainerEventHandler container = (ContainerEventHandler)node;
            for (GuiEventListener child2 : container.m_6702_()) {
                if (!Showcase.clickSettingsSection(child2, label)) continue;
                return true;
            }
            boolean named = container.m_6702_().stream().anyMatch(child -> {
                AbstractWidget widget;
                return child instanceof AbstractWidget && (widget = (AbstractWidget)child).m_6035_().getString().contains(label);
            });
            if (named) {
                for (GuiEventListener child3 : container.m_6702_()) {
                    if (!(child3 instanceof Button)) continue;
                    Button button2 = (Button)child3;
                    target = button2;
                    break;
                }
            }
        }
        if (target == null) {
            return false;
        }
        double x = (double)target.m_252754_() + (double)target.m_5711_() / 2.0;
        double y = (double)target.m_252907_() + (double)target.m_93694_() / 2.0;
        Minecraft.m_91087_().f_91080_.m_6375_(x, y, 0);
        Minecraft.m_91087_().f_91080_.m_6348_(x, y, 0);
        return true;
    }

    private static void furnaceBreakScript() {
        Showcase.step(10, () -> Showcase.check("furnace break starts in the scene", Scene.phase == Scene.Phase.ACTIVE, "phase=" + String.valueOf((Object)Scene.phase)));
        Showcase.step(2, () -> Showcase.server(p -> p.m_284548_().m_46597_(FURNACE, Blocks.f_50016_.m_49966_())));
        Showcase.step(30, () -> {
            Showcase.check("removed furnace closes the scene", Scene.phase == Scene.Phase.NONE, "phase=" + String.valueOf((Object)Scene.phase));
            Showcase.server(p -> Showcase.check("removed furnace ends the server session", Sessions.get(p) == null, "session=" + String.valueOf(Sessions.get(p))));
            Showcase.shot("fb_removed");
        });
        Showcase.step(2, () -> Showcase.finish(Minecraft.m_91087_()));
    }

    private static void furnaceScript() {
        Showcase.furnaceEditorScript();
        Showcase.shot("35_active");
        Showcase.step(4, () -> Showcase.inventoryClick(41, 0));
        Showcase.step(3, () -> Showcase.pointZone(FURNACE, 1));
        Showcase.step(12, () -> Showcase.shot("36_carry_fuel"));
        Showcase.step(2, () -> Showcase.clickZone(1, 0));
        Showcase.step(8, () -> Showcase.shot("37_fueled"));
        Showcase.step(4, () -> Showcase.inventoryClick(42, 0));
        Showcase.step(3, () -> Showcase.pointZone(FURNACE, 0));
        Showcase.step(12, () -> Showcase.shot("38_carry_ore"));
        Showcase.step(2, () -> Showcase.clickZone(0, 1));
        Showcase.step(6, () -> Showcase.clickZone(0, 1));
        Showcase.step(6, () -> Showcase.clickZone(0, 1));
        Showcase.step(6, () -> Showcase.inventoryClick(42, 0));
        Showcase.step(4, () -> Showcase.pointLedge(FURNACE));
        Showcase.step(30, () -> Showcase.shot("39_lit"));
        Showcase.step(2, () -> Showcase.pointZone(FURNACE, 0));
        Showcase.step(8, () -> Showcase.shot("40_input_tooltip"));
        Showcase.step(2, () -> Showcase.pointLedge(FURNACE));
        Showcase.step(380, () -> Showcase.shot("42_smelted"));
        Showcase.step(2, () -> Showcase.pointZone(FURNACE, 2));
        Showcase.step(10, () -> Showcase.shot("43_result_hover"));
        Showcase.step(2, () -> Showcase.clickZone(2, 0));
        Showcase.step(10, () -> Showcase.shot("44_ingot_in_hand"));
        Showcase.step(2, () -> Showcase.inventoryClick(43, 0));
        Showcase.step(4, () -> Showcase.inventoryClick(40, 0));
        Showcase.step(3, () -> Showcase.pointZone(FURNACE, 1));
        Showcase.step(10, () -> Showcase.shot("45_not_fuel"));
        Showcase.step(2, () -> Showcase.clickZone(1, 0));
        Showcase.step(4, () -> Showcase.inventoryClick(40, 0));
        Showcase.step(4, () -> Showcase.pointLedge(FURNACE));
        Showcase.step(6, () -> Showcase.report("furnace before leaving"));
        Showcase.step(2, Scene::leave);
        Showcase.step(50, () -> Showcase.shot("39_after"));
        Showcase.nextAct("4", new Vec3(2.5, -60.0, 5.4), 180.0f, SMOKER, new Vec3(0.5, 0.6, 1.0), Direction.SOUTH);
    }

    private static void smokerScript() {
        Showcase.furnaceEditorScript();
        Showcase.shot("45_active");
        Showcase.step(4, () -> Showcase.inventoryClick(9, 0));
        Showcase.step(3, () -> Showcase.pointZone(SMOKER, 1));
        Showcase.step(12, () -> Showcase.shot("46_carry_fuel"));
        Showcase.step(2, () -> Showcase.clickZone(1, 0));
        Showcase.step(6, () -> Showcase.inventoryClick(11, 0));
        Showcase.step(3, () -> Showcase.pointZone(SMOKER, 0));
        Showcase.step(12, () -> Showcase.shot("47_carry_food"));
        Showcase.step(2, () -> Showcase.clickZone(0, 1));
        Showcase.step(6, () -> Showcase.clickZone(0, 1));
        Showcase.step(6, () -> Showcase.inventoryClick(11, 0));
        Showcase.step(4, () -> Showcase.pointLedge(SMOKER));
        Showcase.step(40, () -> Showcase.shot("48_lit"));
        Showcase.step(2, () -> Showcase.pointZone(SMOKER, 0));
        Showcase.step(8, () -> Showcase.shot("49_input_tooltip"));
        Showcase.step(2, () -> Showcase.pointLedge(SMOKER));
        Showcase.step(180, () -> Showcase.shot("50_smoked"));
        Showcase.step(2, () -> Showcase.pointZone(SMOKER, 2));
        Showcase.step(10, () -> Showcase.shot("51_result_hover"));
        Showcase.step(2, () -> Showcase.report("smoker before taking"));
        Showcase.step(2, () -> {
            Showcase.pointZone(SMOKER, 2);
            FurnaceStation station = Scene.station(FurnaceStation.class);
            if (station != null) {
                station.click(0, true);
            }
        });
        Showcase.step(10, () -> Showcase.report("smoker after taking"));
        Showcase.step(2, Scene::leave);
        Showcase.step(50, () -> Showcase.shot("49_after"));
        Showcase.nextAct("5", new Vec3(4.5, -60.0, 5.4), 180.0f, BLAST, new Vec3(0.5, 0.6, 1.0), Direction.SOUTH);
    }

    private static void blastScript() {
        Showcase.furnaceEditorScript();
        Showcase.shot("55_active");
        Showcase.step(4, () -> Showcase.inventoryClick(16, 0));
        Showcase.step(3, () -> Showcase.pointZone(BLAST, 1));
        Showcase.step(12, () -> Showcase.shot("56_carry_fuel"));
        Showcase.step(2, () -> Showcase.clickZone(1, 0));
        Showcase.step(6, () -> Showcase.inventoryClick(15, 0));
        Showcase.step(3, () -> Showcase.pointZone(BLAST, 0));
        Showcase.step(12, () -> Showcase.shot("57_carry_ore"));
        Showcase.step(2, () -> Showcase.clickZone(0, 0));
        Showcase.step(8, () -> Showcase.shot("58_charged"));
        Showcase.step(4, () -> Showcase.pointZone(BLAST, 2));
        Showcase.step(40, () -> Showcase.shot("59_lit"));
        Showcase.step(170, () -> Showcase.shot("60_blasted"));
        Showcase.step(4, () -> Showcase.shot("61_result_hover"));
        Showcase.step(2, () -> Showcase.report("blast furnace before taking"));
        Showcase.step(2, () -> Showcase.pointZone(BLAST, 2));
        Showcase.step(2, () -> Showcase.clickZone(2, 0));
        Showcase.step(10, () -> Showcase.shot("62_ingots_in_hand"));
        Showcase.step(2, () -> Showcase.inventoryClick(17, 0));
        Showcase.step(6, () -> Showcase.report("blast furnace after taking"));
        Showcase.step(2, Scene::leave);
        Showcase.step(50, () -> Showcase.shot("59_after"));
        Showcase.nextAct("6", new Vec3(-4.5, -60.0, 11.6), 180.0f, ENCHANT, new Vec3(0.5, 0.75, 0.6), Direction.UP);
    }

    private static void pointZone(BlockPos pos, int slot) {
        StationKind kind = StationKind.of(Minecraft.m_91087_().f_91073_.m_8055_(pos));
        FurnaceLayout layout = FurnaceLayout.of(kind);
        FurnaceLayout.Zone zone = layout.zone(slot);
        double[] c = layout.center(slot);
        Showcase.pointAt(FurnaceZones.point(pos, Direction.SOUTH, zone.face(), c[0], c[1], 0.0));
    }

    private static void pointLedge(BlockPos pos) {
        Showcase.pointAt(FurnaceZones.point(pos, Direction.SOUTH, 0.5, 0.45, 0.0));
    }

    private static void clickZone(int zone, int button) {
        FurnaceStation station = Scene.station(FurnaceStation.class);
        if (station == null || station.hoverZone != zone) {
            REPORT.add("cursor missed furnace zone " + zone + ": hover=" + String.valueOf(station == null ? "-" : Integer.valueOf(station.hoverZone)));
        }
        Showcase.screenClick(button);
    }

    private static void enchantScript() {
        Runnable restoreLayout = Showcase.spatialEditorScript();
        Showcase.shot("65_active");
        Showcase.step(2, Showcase::pointAside);
        Showcase.step(8, () -> {
            Showcase.shot("65a_lapis_socket_empty");
            Minecraft mc = Minecraft.m_91087_();
            double[] at = Picking.toGui(Showcase.enchant().corner());
            double width = mc.m_91268_().m_85445_();
            double height = mc.m_91268_().m_85446_();
            boolean visible = at != null && at[0] > width * 0.1 && at[0] < width * 0.9 && at[1] > height * 0.15 && at[1] < height * 0.85;
            Showcase.check("lapis socket stays in the usable frame", visible, at == null ? "behind camera" : String.format("screen=%.3f,%.3f", at[0] / width, at[1] / height));
        });
        Showcase.step(2, () -> Showcase.pointAt(Showcase.enchant().corner()));
        Showcase.step(6, () -> {
            Showcase.shot("65b_lapis_socket_hover");
            Showcase.check("empty lapis socket is reachable", Showcase.enchant().target == EnchantStation.Target.LAPIS && Showcase.enchant().lapis() == 0, "target=" + String.valueOf((Object)Showcase.enchant().target) + " lapis=" + Showcase.enchant().lapis());
        });
        Showcase.step(4, () -> Showcase.invClickItem(Items.f_42388_));
        Showcase.step(3, () -> Showcase.pointAt(EnchantFx.top(ENCHANT).m_82520_(0.0, 0.0, 0.1)));
        Showcase.step(12, () -> Showcase.shot("66_carry_sword"));
        Showcase.step(2, () -> Showcase.clickEnchant(EnchantStation.Target.SURFACE, 0));
        Showcase.step(20, () -> Showcase.shot("67_sword_floats"));
        Showcase.step(2, () -> Showcase.invClickItem(Items.f_42534_));
        Showcase.step(3, () -> Showcase.pointAt(Showcase.enchant().corner()));
        Showcase.step(10, () -> Showcase.shot("68_carry_lapis"));
        Showcase.step(2, () -> Showcase.clickEnchant(EnchantStation.Target.LAPIS, 0));
        Showcase.step(8, () -> {
            Showcase.shot("68a_lapis_socket_filled");
            Showcase.checkLapisCount("lapis placed in the visible socket", 12);
        });
        Showcase.step(4, Showcase::pointAside);
        Showcase.step(16, () -> Showcase.shot("69_offers"));
        Showcase.step(2, () -> Showcase.pointAt(Showcase.enchant().sigilPos(0)));
        Showcase.step(10, () -> Showcase.shot("70_hover_first"));
        Showcase.step(2, () -> Showcase.pointAt(Showcase.enchant().sigilPos(2)));
        Showcase.step(10, () -> Showcase.shot("71_hover_third"));
        Showcase.step(2, () -> Showcase.report("enchanting before the rite"));
        Showcase.step(2, () -> Showcase.clickEnchant(EnchantStation.Target.OFFER, 0));
        Showcase.step(2, () -> {
            boolean dev = Settings.developerMode();
            Settings.set(Settings.DEVELOPER_MODE, true);
            Minecraft.m_91087_().f_91080_.m_7933_(71, 0, 0);
            Showcase.check("enchant editor blocked during rite", !Showcase.enchant().gridEditor.editing() && Showcase.enchant().busy(), "ritual continues");
            Settings.set(Settings.DEVELOPER_MODE, dev);
            Showcase.shot("72_rite_010");
        });
        Showcase.step(3, () -> Showcase.shot("72_rite_025"));
        Showcase.step(3, () -> Showcase.shot("72_rite_040"));
        Showcase.step(3, () -> Showcase.shot("72_rite_055"));
        Showcase.step(3, () -> Showcase.shot("72_rite_070"));
        Showcase.step(6, () -> Showcase.shot("72_rite_100"));
        Showcase.step(10, () -> Showcase.shot("72_rite_150"));
        Showcase.step(20, () -> Showcase.shot("73_enchanted"));
        Showcase.step(2, () -> {
            Showcase.checkLapisCount("lapis consumed by the third offer", 9);
            Showcase.check("edited enchanting produced an enchanted sword", Showcase.enchant().item().m_150930_(Items.f_42388_) && Showcase.enchant().item().m_41793_(), "vanilla enchantments applied");
        });
        Showcase.step(2, () -> Showcase.pointAt(Showcase.enchant().itemPos()));
        Showcase.step(8, () -> Showcase.shot("74_item_hover"));
        Showcase.step(2, () -> Showcase.report("enchanting after the rite"));
        Showcase.step(2, () -> Showcase.clickEnchant(EnchantStation.Target.ITEM, 0));
        Showcase.step(8, () -> Showcase.shot("75_sword_in_hand"));
        Showcase.step(2, () -> Showcase.invClick(18, 0));
        Showcase.step(6, () -> Showcase.report("enchanting before leaving"));
        Showcase.step(2, restoreLayout);
        Showcase.step(25, Showcase::pointAside);
        Showcase.step(2, Scene::leave);
        Showcase.step(50, () -> Showcase.shot("69_after"));
        Showcase.step(2, () -> Showcase.report("enchanting after leaving"));
        Showcase.smithyAct("a", ANVIL);
    }

    private static EnchantStation enchant() {
        return Scene.station(EnchantStation.class);
    }

    private static void checkLapisCount(String name, int expected) {
        Showcase.check(name + " client", Showcase.enchant() != null && Showcase.enchant().lapis() == expected, "lapis=" + String.valueOf(Showcase.enchant() == null ? "-" : Integer.valueOf(Showcase.enchant().lapis())) + " expected=" + expected);
        Showcase.server(p -> {
            int n;
            AbstractContainerMenu patt350763$temp = p.f_36096_;
            if (patt350763$temp instanceof EnchantmentMenu) {
                EnchantmentMenu menu = (EnchantmentMenu)patt350763$temp;
                n = menu.m_38853_(1).m_7993_().m_41613_();
            } else {
                n = -1;
            }
            int count = n;
            Showcase.check(name + " server", count == expected, "lapis=" + count + " expected=" + expected);
        });
    }

    private static void clickEnchant(EnchantStation.Target expected, int button) {
        EnchantStation station = Showcase.enchant();
        if (station == null || station.target != expected) {
            REPORT.add("cursor missed enchanting target " + String.valueOf((Object)expected) + ": was " + String.valueOf(station == null ? "-" : station.target));
        }
        Showcase.screenClick(button);
    }

    private static void smithyAct(String next, BlockPos station) {
        double y = station.equals((Object)STONECUTTER) ? 0.56 : 1.0;
        Showcase.nextAct(next, new Vec3((double)station.m_123341_() + 0.5, -60.0, (double)station.m_123343_() + 3.4), 180.0f, station, new Vec3(0.5, y, 0.5), Direction.UP);
    }

    private static void moveShots(String prefix, double ... times) {
        double at = 0.0;
        for (double time : times) {
            int ticks = (int)Math.max(2L, Math.round((time - at) * 20.0));
            at += (double)ticks / 20.0;
            String name = String.format("%s_%03d", prefix, Math.round(time * 100.0));
            Showcase.step(ticks - 1, () -> Showcase.shot(name));
        }
    }

    private static AnvilStation anvil() {
        return Scene.station(AnvilStation.class);
    }

    private static void clickAnvil(AnvilStation.Target expected, int button) {
        AnvilStation station = Showcase.anvil();
        if (station == null || station.target != expected) {
            REPORT.add("cursor missed anvil target " + String.valueOf((Object)expected) + ": was " + String.valueOf(station == null ? "-" : station.target));
        }
        Showcase.screenClick(button);
    }

    private static void anvilScript() {
        Runnable restoreLayout = Showcase.spatialEditorScript();
        BakedModel model = HammerModel.model();
        List quads = model.getQuads(null, null, RandomSource.m_216335_((long)42L), ModelData.EMPTY, null);
        HashSet<ResourceLocation> textures = new HashSet<ResourceLocation>();
        for (BakedQuad quad : quads) {
            textures.add(quad.m_173410_().m_245424_().m_246162_());
        }
        Showcase.check("imported hammer model and textures loaded", model != Minecraft.m_91087_().m_91304_().m_119409_() && quads.size() == 24 && textures.size() == 4 && textures.stream().allMatch(texture -> texture.m_135827_().equals("cinematic") && texture.m_135815_().startsWith("item/prop/hammer_")), "faces=" + quads.size() + " textures=" + String.valueOf(textures));
        Showcase.shot("a5_active");
        Showcase.step(4, () -> Showcase.invClickItem(Items.f_42385_));
        Showcase.step(3, () -> Showcase.pointAt(Showcase.anvil().restPoint(0)));
        Showcase.step(12, () -> Showcase.shot("a6_carry_pickaxe"));
        Showcase.step(2, () -> Showcase.clickAnvil(AnvilStation.Target.SURFACE, 0));
        Showcase.step(10, () -> Showcase.shot("a7_pickaxe_laid"));
        Showcase.step(2, () -> Showcase.invClickItem(Items.f_42690_));
        Showcase.step(3, () -> Showcase.pointAt(Showcase.anvil().restPoint(1)));
        Showcase.step(10, () -> Showcase.shot("a8_carry_book"));
        Showcase.step(2, () -> Showcase.clickAnvil(AnvilStation.Target.SURFACE, 0));
        Showcase.step(4, Showcase::pointAside);
        Showcase.step(14, () -> Showcase.shot("a9_pile_cost"));
        Showcase.step(2, () -> Showcase.pointAt(Showcase.anvil().restPoint(1)));
        Showcase.step(8, () -> Showcase.shot("aa_pile_tooltip"));
        Showcase.step(2, () -> Showcase.report("anvil before forging"));
        Showcase.step(2, () -> Showcase.clickAnvil(AnvilStation.Target.STACK, 0));
        Showcase.moveShots("ab_forge", 0.25, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 1.3, 1.45, 1.65, 1.85, 2.05);
        Showcase.step(10, () -> Showcase.shot("ac_forged"));
        Showcase.step(2, () -> {
            ItemStack result = Minecraft.m_91087_().f_91074_.f_36096_.m_142621_();
            Showcase.check("hammer forging produces enchanted pickaxe client", result.m_150930_(Items.f_42385_) && result.m_41793_(), "result=" + String.valueOf(result));
            Showcase.server(p -> {
                ItemStack forged = p.f_36096_.m_142621_();
                Showcase.check("hammer forging produces enchanted pickaxe server", forged.m_150930_(Items.f_42385_) && forged.m_41793_() && p.f_36096_.m_38853_(0).m_7993_().m_41619_() && p.f_36096_.m_38853_(1).m_7993_().m_41619_(), "result=" + String.valueOf(forged));
            });
        });
        Showcase.step(2, () -> Showcase.report("anvil after forging"));
        Showcase.step(2, () -> Showcase.pointAt(Showcase.anvil().restPoint(0)));
        Showcase.step(6, () -> Showcase.clickAnvil(AnvilStation.Target.SURFACE, 0));
        Showcase.step(8, () -> Showcase.pointAt(Showcase.anvil().namePoint()));
        Showcase.step(8, () -> Showcase.shot("ad_name_hover"));
        Showcase.step(2, () -> Showcase.clickAnvil(AnvilStation.Target.NAME, 0));
        Showcase.step(6, () -> Showcase.shot("ae_name_field"));
        Showcase.step(2, () -> SceneScreen.commitEdit("\u041a\u0438\u0440\u043a\u0430 \u043c\u0430\u0441\u0442\u0435\u0440\u0430"));
        Showcase.step(4, Showcase::pointAside);
        Showcase.step(10, () -> Showcase.shot("af_renamed"));
        Showcase.step(2, () -> Showcase.pointAt(Showcase.anvil().restPoint(0)));
        Showcase.step(6, () -> Showcase.clickAnvil(AnvilStation.Target.STACK, 0));
        Showcase.step(46, () -> Showcase.shot("ag_named"));
        Showcase.step(2, () -> Showcase.invClick(28, 0));
        Showcase.step(6, () -> Showcase.report("anvil before leaving"));
        Showcase.step(2, () -> Showcase.server(p -> {
            ItemStack named = p.m_150109_().m_8020_(28);
            Showcase.check("hammer engraving preserves the forged item", named.m_150930_(Items.f_42385_) && named.m_41793_() && named.m_41786_().getString().equals("\u041a\u0438\u0440\u043a\u0430 \u043c\u0430\u0441\u0442\u0435\u0440\u0430"), "result=" + named.m_41786_().getString());
        }));
        Showcase.step(2, restoreLayout);
        Showcase.step(2, Scene::leave);
        Showcase.step(50, () -> Showcase.shot("a9_after"));
        Showcase.smithyAct("g", GRINDSTONE);
    }

    private static GrindstoneStation grindstone() {
        return Scene.station(GrindstoneStation.class);
    }

    private static void clickGrindstone(GrindstoneStation.Target expected, int button) {
        GrindstoneStation station = Showcase.grindstone();
        if (station == null || station.target != expected) {
            REPORT.add("cursor missed grindstone target " + String.valueOf((Object)expected) + ": was " + String.valueOf(station == null ? "-" : station.target));
        }
        Showcase.screenClick(button);
    }

    private static void grindstoneScript() {
        Runnable restoreLayout = Showcase.spatialEditorScript();
        Showcase.shot("g5_active");
        Showcase.step(4, () -> Showcase.invClickItem(Items.f_42430_));
        Showcase.step(3, () -> Showcase.pointAt(Showcase.grindstone().restPoint(0)));
        Showcase.step(12, () -> Showcase.shot("g6_carry_sword"));
        Showcase.step(2, () -> Showcase.clickGrindstone(GrindstoneStation.Target.PLACE, 0));
        Showcase.step(4, Showcase::pointAside);
        Showcase.step(10, () -> Showcase.shot("g7_on_wheel"));
        Showcase.step(2, () -> Showcase.pointAt(Showcase.grindstone().restPoint(0)));
        Showcase.step(8, () -> Showcase.shot("g8_tooltip"));
        Showcase.step(2, () -> Showcase.report("grindstone before grinding"));
        Showcase.step(2, () -> Showcase.clickGrindstone(GrindstoneStation.Target.ITEM, 0));
        Showcase.moveShots("g9_grind", 0.15, 0.4, 0.62, 0.8, 1.0, 1.2, 1.4, 1.55, 1.75, 1.95, 2.15, 2.4);
        Showcase.step(12, () -> Showcase.shot("ga_ground"));
        Showcase.step(30, () -> Showcase.report("grindstone after grinding"));
        Showcase.step(2, () -> Showcase.invClick(29, 0));
        Showcase.step(4, Showcase::pointAside);
        Showcase.step(2, restoreLayout);
        Showcase.step(2, Scene::leave);
        Showcase.step(50, () -> Showcase.shot("g9_after"));
        Showcase.smithyAct("s", SMITHING);
    }

    private static SmithingStation smithing() {
        return Scene.station(SmithingStation.class);
    }

    private static void clickSmithing(SmithingStation.Target expected, int button) {
        SmithingStation station = Showcase.smithing();
        if (station == null || station.target != expected) {
            REPORT.add("cursor missed smithing target " + String.valueOf((Object)expected) + ": was " + String.valueOf(station == null ? "-" : station.target));
        }
        Showcase.screenClick(button);
    }

    private static void laySmithing(Item item, int slot, String shotName, int button) {
        Showcase.step(4, () -> Showcase.invClickItem(item));
        Showcase.step(3, () -> Showcase.pointAt(Showcase.smithing().restPoint(slot)));
        if (shotName != null) {
            Showcase.step(10, () -> Showcase.shot(shotName));
        }
        Showcase.step(4, () -> Showcase.clickSmithing(SmithingStation.Target.PLACE, button));
    }

    private static void smithingScript() {
        Runnable restoreLayout = Showcase.surfaceEditorScript();
        Showcase.shot("s5_active");
        Showcase.laySmithing(Items.f_265918_, 0, "s6_carry_template", 0);
        Showcase.laySmithing(Items.f_42473_, 1, null, 0);
        Showcase.laySmithing(Items.f_42418_, 2, "s6_carry_ingot", 0);
        Showcase.step(4, Showcase::pointAside);
        Showcase.step(10, () -> Showcase.shot("s7_laid"));
        Showcase.step(2, () -> Showcase.pointAt(Showcase.smithing().restPoint(1)));
        Showcase.step(8, () -> Showcase.shot("s8_tooltip"));
        Showcase.step(2, () -> Showcase.report("smithing before the upgrade"));
        Showcase.step(2, () -> Showcase.clickSmithing(SmithingStation.Target.ITEM, 0));
        Showcase.moveShots("s9_upgrade", 0.2, 0.4, 0.5, 0.75, 0.95, 1.15, 1.3, 1.4, 1.6, 1.9, 2.2, 2.6);
        Showcase.step(10, () -> Showcase.shot("sa_netherite"));
        Showcase.step(2, () -> Showcase.check("edited smithing produces netherite", Showcase.carried().m_150930_(Items.f_42481_), "real vanilla result=" + String.valueOf(Showcase.carried())));
        Showcase.step(2, () -> Showcase.report("smithing after the upgrade"));
        Showcase.step(3, () -> Showcase.pointAt(Showcase.smithing().restPoint(1)));
        Showcase.step(6, () -> Showcase.clickSmithing(SmithingStation.Target.PLACE, 0));
        Showcase.laySmithing(Items.f_265914_, 0, null, 0);
        Showcase.laySmithing(Items.f_151049_, 2, null, 1);
        Showcase.step(4, () -> Showcase.invClick(30, 0));
        Showcase.step(2, () -> Showcase.pointAt(Showcase.smithing().restPoint(1)));
        Showcase.step(8, () -> Showcase.shot("sb_trim_laid"));
        Showcase.step(2, () -> Showcase.clickSmithing(SmithingStation.Target.ITEM, 0));
        Showcase.moveShots("sc_trim", 0.5, 0.95, 1.2, 1.4, 1.9);
        Showcase.step(16, () -> Showcase.shot("sd_trimmed"));
        Showcase.step(2, () -> Showcase.check("edited smithing applies trim", ArmorTrim.m_266285_((RegistryAccess)Minecraft.m_91087_().f_91073_.m_9598_(), (ItemStack)Showcase.carried()).isPresent(), "real trim=" + String.valueOf(Showcase.carried())));
        Showcase.step(2, () -> Showcase.invClick(31, 0));
        Showcase.step(6, () -> Showcase.report("smithing after the trim"));
        Showcase.step(2, restoreLayout);
        Showcase.step(25, Showcase::pointAside);
        Showcase.step(2, Scene::leave);
        Showcase.step(50, () -> Showcase.shot("s9_after"));
        Showcase.smithyAct("c", STONECUTTER);
    }

    private static StonecutterStation stonecutter() {
        return Scene.station(StonecutterStation.class);
    }

    private static void clickStonecutter(StonecutterStation.Target expected, int button) {
        StonecutterStation station = Showcase.stonecutter();
        if (station == null || station.target != expected) {
            REPORT.add("cursor missed stonecutter target " + String.valueOf((Object)expected) + ": was " + String.valueOf(station == null ? "-" : station.target));
        }
        Showcase.screenClick(button);
    }

    private static int shape(Item item) {
        Minecraft mc = Minecraft.m_91087_();
        StonecutterMenu menu = (StonecutterMenu)Showcase.stonecutter().menu();
        for (int i = 0; menu != null && i < menu.m_40339_().size(); ++i) {
            if (!((StonecutterRecipe)menu.m_40339_().get(i)).m_8043_(mc.f_91073_.m_9598_()).m_150930_(item)) continue;
            return i;
        }
        REPORT.add("no stonecutter shape for " + String.valueOf(item));
        return 0;
    }

    private static void stonecutterScript() {
        Runnable restoreLayout = Showcase.spatialEditorScript();
        Showcase.shot("c5_active");
        Showcase.step(4, () -> Showcase.invClickItem(Items.f_41905_));
        Showcase.step(3, () -> Showcase.pointAt(Showcase.stonecutter().restPoint(0)));
        Showcase.step(10, () -> Showcase.shot("c6_carry_stone"));
        Showcase.step(2, () -> Showcase.clickStonecutter(StonecutterStation.Target.PLACE, 0));
        Showcase.step(4, Showcase::pointAside);
        Showcase.step(14, () -> Showcase.shot("c7_dial"));
        Showcase.step(2, () -> Showcase.pointAt(Showcase.stonecutter().shapePoint(Showcase.shape(Items.f_42018_))));
        Showcase.step(8, () -> Showcase.shot("c8_hover_bricks"));
        Showcase.step(2, () -> Showcase.clickStonecutter(StonecutterStation.Target.SHAPE, 0));
        Showcase.moveShots("c9_cut", 0.1, 0.25, 0.4, 0.5, 0.6, 0.75, 0.95);
        Showcase.step(8, () -> Showcase.shot("ca_bricks_in_hand"));
        Showcase.step(2, () -> Showcase.invClick(32, 0));
        Showcase.step(2, () -> Showcase.pointAt(Showcase.stonecutter().shapePoint(Showcase.shape(Items.f_41922_))));
        Showcase.step(6, () -> Showcase.clickStonecutter(StonecutterStation.Target.SHAPE, 0));
        Showcase.step(12, () -> Showcase.shot("cb_cut_slab"));
        Showcase.step(14, () -> Showcase.invClick(33, 0));
        Showcase.step(2, () -> Showcase.report("stonecutter after two cuts"));
        Showcase.step(2, () -> Showcase.pointAt(Showcase.stonecutter().shapePoint(Showcase.shape(Items.f_42371_))));
        Showcase.step(6, () -> {
            StonecutterStation station = Showcase.stonecutter();
            if (station == null || station.target != StonecutterStation.Target.SHAPE) {
                REPORT.add("cursor missed the stairs");
            }
            if (station != null) {
                station.click(0, true);
            }
        });
        Showcase.moveShots("cc_cut_all", 0.3, 0.6, 0.9, 1.3);
        Showcase.step(10, () -> Showcase.report("stonecutter after cutting all"));
        Showcase.step(2, () -> Showcase.invClickItem(Items.f_151035_));
        Showcase.step(3, () -> Showcase.pointAt(Showcase.stonecutter().restPoint(0)));
        Showcase.step(6, () -> Showcase.clickStonecutter(StonecutterStation.Target.PLACE, 0));
        Showcase.step(4, Showcase::pointAside);
        Showcase.step(14, () -> Showcase.shot("cd_dial_many"));
        Showcase.step(2, () -> {
            for (int i = 0; i < 4; ++i) {
                Showcase.stonecutter().scroll(-1.0);
            }
        });
        Showcase.step(12, () -> Showcase.shot("ce_dial_turned"));
        Showcase.step(4, () -> Showcase.report("stonecutter before leaving"));
        Showcase.step(2, restoreLayout);
        Showcase.step(2, Scene::leave);
        Showcase.step(50, () -> Showcase.shot("c9_after"));
        Showcase.restock();
        Showcase.smithyAct("l", LOOM);
    }

    private static void restock() {
        Showcase.step(2, () -> Showcase.server(p -> {
            ServerLevel level = p.m_284548_();
            for (int i = 9; i < 36; ++i) {
                p.m_150109_().m_6836_(i, ItemStack.f_41583_);
            }
            ItemStack map = MapItem.m_42886_((Level)level, (int)11, (int)14, (byte)0, (boolean)true, (boolean)false);
            MapItemSavedData data = MapItem.m_42853_((ItemStack)map, (Level)level);
            for (int pass = 0; pass < 8 && data != null; ++pass) {
                ((MapItem)Items.f_42573_).m_42893_((Level)level, (Entity)p, data);
            }
            ItemStack water = PotionUtils.m_43549_((ItemStack)new ItemStack((ItemLike)Items.f_42589_), (Potion)Potions.f_43599_);
            ItemStack[] stock = new ItemStack[]{new ItemStack((ItemLike)Items.f_42660_, 2), new ItemStack((ItemLike)Items.f_42497_, 4), map, new ItemStack((ItemLike)Items.f_42516_, 2), new ItemStack((ItemLike)Items.f_42027_), water.m_41777_(), water.m_41777_(), water.m_41777_(), new ItemStack((ItemLike)Items.f_42588_, 2), new ItemStack((ItemLike)Items.f_42593_, 2), new ItemStack((ItemLike)Items.f_42581_, 3), new ItemStack((ItemLike)Items.f_42658_, 2), new ItemStack((ItemLike)Items.f_42647_, 8), new ItemStack((ItemLike)Items.f_42417_, 2), new ItemStack((ItemLike)Items.f_42579_, 3)};
            for (int i = 0; i < stock.length; ++i) {
                p.m_150109_().m_6836_(9 + i, stock[i]);
            }
            p.f_36095_.m_38946_();
        }));
    }

    private static void stash() {
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        for (int i = 9; i < 36; ++i) {
            if (!player.m_150109_().m_8020_(i).m_41619_()) continue;
            Showcase.invClick(i, 0);
            return;
        }
        REPORT.add("no empty slot to stash " + String.valueOf(player.f_36096_.m_142621_()));
    }

    private static ItemStack carried() {
        return Minecraft.m_91087_().f_91074_.f_36096_.m_142621_();
    }

    private static LoomStation loom() {
        return Scene.station(LoomStation.class);
    }

    private static void clickLoom(LoomStation.Target expected, int button) {
        LoomStation station = Showcase.loom();
        if (station == null || station.target != expected) {
            REPORT.add("cursor missed loom target " + String.valueOf((Object)expected) + ": was " + String.valueOf(station == null ? "-" : station.target));
        }
        Showcase.screenClick(button);
    }

    private static void pointPattern(String asset) {
        double[] at;
        LoomStation station = Showcase.loom();
        int k = station == null ? -1 : station.patternIndex(asset);
        double[] dArray = at = k < 0 ? null : station.boardCell(k);
        if (at == null) {
            REPORT.add("pattern " + asset + " not on the board");
            return;
        }
        Showcase.pointGui(at[0], at[1]);
    }

    private static void loomScript() {
        Runnable restoreLayout = Showcase.spatialEditorScript();
        Showcase.shot("l5_active");
        Showcase.step(4, () -> Showcase.invClickItem(Items.f_42660_));
        Showcase.step(3, () -> Showcase.pointAt(Showcase.loom().restPoint(0)));
        Showcase.step(10, () -> Showcase.shot("l6_carry_banner"));
        Showcase.step(2, () -> Showcase.clickLoom(LoomStation.Target.PLACE, 1));
        Showcase.step(4, Showcase::stash);
        Showcase.step(4, () -> Showcase.invClickItem(Items.f_42497_));
        Showcase.step(6, () -> Showcase.pointAt(Showcase.loom().restPoint(1)));
        Showcase.step(1, () -> Showcase.clickLoom(LoomStation.Target.PLACE, 0));
        Showcase.step(4, Showcase::pointAside);
        Showcase.step(16, () -> {
            Showcase.shot("l7_board");
            Settings.set(Settings.DEVELOPER_MODE, true);
            double[] gear = Showcase.loom().gridEditor.togglePoint();
            Showcase.pointGui(gear[0], gear[1]);
            Showcase.screenClick(0);
        });
        Showcase.step(22, () -> {
            Showcase.check("loom gear accessible with pattern board", Showcase.loom().gridEditor.editing() && Showcase.loom().boardCell(0) == null, "editor consumes input, patterns hidden");
            Showcase.check("loom live inputs preserved", Showcase.loom().slot(0).m_150930_(Items.f_42660_) && Showcase.loom().slot(1).m_150930_(Items.f_42497_), "banner and dye intact");
            Showcase.pointAside();
            Showcase.shot("xl4_editor_with_cloth");
            Minecraft.m_91087_().f_91080_.m_7933_(256, 0, 0);
        });
        Showcase.step(25, () -> {
            Settings.set(Settings.DEVELOPER_MODE, false);
            Showcase.check("loom patterns return after editor", Showcase.loom().boardCell(0) != null, "board visible");
        });
        Showcase.step(3, () -> Showcase.server(p -> p.m_150109_().m_36054_(new ItemStack((ItemLike)Items.f_42720_))));
        Showcase.step(5, () -> Showcase.invClickItem(Items.f_42720_));
        Showcase.step(3, () -> Showcase.pointAt(Showcase.loom().restPoint(2)));
        Showcase.step(5, () -> Showcase.clickLoom(LoomStation.Target.PLACE, 0));
        Showcase.step(8, () -> {
            Showcase.check("loom edited pattern slot accepts template", Showcase.loom().slot(2).m_150930_(Items.f_42720_) && Showcase.loom().patternIndex("minecraft:flower") >= 0, "vanilla special pattern available");
            Showcase.pointAt(Showcase.loom().restPoint(2));
        });
        Showcase.step(5, () -> Showcase.clickLoom(LoomStation.Target.ITEM, 0));
        Showcase.step(5, () -> {
            Showcase.check("loom template retrieved from edited slot", Showcase.carried().m_150930_(Items.f_42720_) && Showcase.loom().slot(2).m_41619_(), "same logical slot");
            Showcase.stash();
        });
        Showcase.step(2, () -> Showcase.loom().scroll(-1.0));
        Showcase.step(12, () -> Showcase.shot("l7b_board_scrolled"));
        Showcase.step(2, () -> Showcase.loom().scroll(1.0));
        Showcase.step(12, () -> Showcase.pointPattern("minecraft:cross"));
        Showcase.step(10, () -> Showcase.shot("l8_hover_cross"));
        Showcase.step(2, () -> Showcase.report("loom before weaving"));
        Showcase.step(2, () -> Showcase.pointPattern("minecraft:cross"));
        Showcase.step(1, () -> Showcase.check("loom pattern under the cursor", Showcase.loom() != null && Showcase.loom().target == LoomStation.Target.PATTERN, "target=" + String.valueOf(Showcase.loom() == null ? "-" : Showcase.loom().target)));
        Showcase.step(1, () -> Showcase.clickLoom(LoomStation.Target.PATTERN, 0));
        Showcase.step(1, () -> {
            Settings.set(Settings.DEVELOPER_MODE, true);
            Minecraft.m_91087_().f_91080_.m_7933_(71, 0, 0);
            Showcase.check("loom editor blocked during weaving", !Showcase.loom().gridEditor.editing() && Showcase.loom().moving(), "shuttle continues");
            Settings.set(Settings.DEVELOPER_MODE, false);
        });
        Showcase.moveShots("l9_weave", 0.15, 0.4, 0.7, 1.0, 1.3, 1.6, 1.9, 2.1, 2.3);
        Showcase.step(12, () -> Showcase.shot("la_woven"));
        Showcase.step(2, () -> {
            List<LoomStation.Layer> layers = LoomStation.layersOf(Showcase.carried());
            Showcase.check("woven banner in hand", Showcase.carried().m_150930_(Items.f_42660_) && layers.size() == 1 && layers.get(0).pattern().m_203543_().map(k -> k.m_135782_().m_135815_()).orElse("").equals("cross"), "carried=" + String.valueOf(Showcase.carried()) + " layers=" + layers.size());
        });
        Showcase.step(2, Showcase::stash);
        Showcase.step(4, () -> Showcase.report("loom after weaving"));
        Showcase.step(2, restoreLayout);
        Showcase.step(25, Showcase::pointAside);
        Showcase.step(2, Scene::leave);
        Showcase.step(50, () -> Showcase.shot("l9_after"));
        Showcase.smithyAct("m", CARTOGRAPHY);
    }

    private static CartographyStation cartography() {
        return Scene.station(CartographyStation.class);
    }

    private static void clickCartography(CartographyStation.Target expected, int button) {
        CartographyStation station = Showcase.cartography();
        if (station == null || station.target != expected) {
            REPORT.add("cursor missed cartography target " + String.valueOf((Object)expected) + ": was " + String.valueOf(station == null ? "-" : station.target));
        }
        Showcase.screenClick(button);
    }

    private static void cartographyScript() {
        Runnable restoreLayout = Showcase.surfaceEditorScript();
        Showcase.shot("m5_active");
        Showcase.step(4, () -> Showcase.invClickItem(Items.f_42573_));
        Showcase.step(3, () -> Showcase.pointAt(Showcase.cartography().restPoint(0)));
        Showcase.step(6, () -> Showcase.clickCartography(CartographyStation.Target.PLACE, 0));
        Showcase.step(4, () -> Showcase.invClickItem(Items.f_42516_));
        Showcase.step(3, () -> Showcase.pointAt(Showcase.cartography().restPoint(1)));
        Showcase.step(10, () -> Showcase.shot("m6_carry_paper"));
        Showcase.step(2, () -> Showcase.clickCartography(CartographyStation.Target.PLACE, 1));
        Showcase.step(4, Showcase::stash);
        Showcase.step(4, Showcase::pointAside);
        Showcase.step(10, () -> Showcase.shot("m7_laid"));
        Showcase.step(2, () -> Showcase.pointAt(Showcase.cartography().restPoint(0)));
        Showcase.step(8, () -> Showcase.shot("m8_tooltip"));
        Showcase.step(2, () -> Showcase.report("cartography before zooming"));
        Showcase.step(2, () -> Showcase.clickCartography(CartographyStation.Target.ITEM, 0));
        Showcase.moveShots("m9_zoom", 0.2, 0.45, 0.7, 1.0, 1.3, 1.6, 1.9);
        Showcase.step(10, () -> Showcase.shot("ma_zoomed"));
        Showcase.step(2, () -> Showcase.check("zoomed map in hand", Showcase.carried().m_150930_(Items.f_42573_), "carried=" + String.valueOf(Showcase.carried())));
        Showcase.step(2, Showcase::stash);
        Showcase.step(8, () -> Showcase.server(p -> p.m_150109_().m_36054_(new ItemStack((ItemLike)Items.f_42676_))));
        Showcase.step(4, () -> Showcase.invClickItem(Items.f_42573_));
        Showcase.step(3, () -> Showcase.pointAt(Showcase.cartography().restPoint(0)));
        Showcase.step(6, () -> Showcase.clickCartography(CartographyStation.Target.PLACE, 1));
        Showcase.step(4, Showcase::stash);
        Showcase.step(4, () -> Showcase.invClickItem(Items.f_42676_));
        Showcase.step(3, () -> Showcase.pointAt(Showcase.cartography().restPoint(1)));
        Showcase.step(6, () -> Showcase.clickCartography(CartographyStation.Target.PLACE, 0));
        Showcase.step(4, () -> Showcase.pointAt(Showcase.cartography().restPoint(0)));
        Showcase.step(6, () -> Showcase.clickCartography(CartographyStation.Target.ITEM, 0));
        Showcase.moveShots("mc_copy", 0.3, 0.6, 1.0, 1.2, 1.4, 1.7);
        Showcase.step(10, () -> Showcase.check("copied maps in hand", Showcase.carried().m_150930_(Items.f_42573_) && Showcase.carried().m_41613_() == 2, "carried=" + String.valueOf(Showcase.carried())));
        Showcase.step(2, Showcase::stash);
        Showcase.step(4, () -> {
            LocalPlayer player = Minecraft.m_91087_().f_91074_;
            for (int i = 35; i >= 9; --i) {
                if (!player.m_150109_().m_8020_(i).m_150930_(Items.f_42573_)) continue;
                Showcase.invClick(i, 0);
                return;
            }
        });
        Showcase.step(3, () -> Showcase.pointAt(Showcase.cartography().restPoint(0)));
        Showcase.step(6, () -> Showcase.clickCartography(CartographyStation.Target.PLACE, 0));
        Showcase.step(4, () -> Showcase.invClickItem(Items.f_42027_));
        Showcase.step(3, () -> Showcase.pointAt(Showcase.cartography().restPoint(1)));
        Showcase.step(6, () -> Showcase.clickCartography(CartographyStation.Target.PLACE, 0));
        Showcase.step(4, () -> Showcase.pointAt(Showcase.cartography().restPoint(0)));
        Showcase.step(6, () -> Showcase.clickCartography(CartographyStation.Target.ITEM, 0));
        Showcase.moveShots("mb_lock", 0.3, 0.6, 1.0, 1.2, 1.4, 1.7);
        Showcase.step(10, () -> Showcase.server(p -> {
            ItemStack held = p.f_36096_.m_142621_();
            MapItemSavedData data = MapItem.m_42853_((ItemStack)held, (Level)p.m_9236_());
            Showcase.check("locked map in hand", held.m_150930_(Items.f_42573_) && data != null && data.f_77892_, "carried=" + String.valueOf(held));
        }));
        Showcase.step(2, Showcase::stash);
        Showcase.step(4, () -> Showcase.report("cartography before leaving"));
        Showcase.step(2, restoreLayout);
        Showcase.step(25, Showcase::pointAside);
        Showcase.step(2, Scene::leave);
        Showcase.step(50, () -> Showcase.shot("m9_after"));
        Showcase.smithyAct("b", BREWING);
    }

    private static BrewingStation brewing() {
        return Scene.station(BrewingStation.class);
    }

    private static void clickBrewing(BrewingStation.Target expected, int button) {
        BrewingStation station = Showcase.brewing();
        if (station == null || station.target != expected) {
            REPORT.add("cursor missed brewing target " + String.valueOf((Object)expected) + ": was " + String.valueOf(station == null ? "-" : station.target));
        }
        Showcase.screenClick(button);
    }

    private static void brewingScript() {
        Runnable restoreLayout = Showcase.spatialEditorScript();
        Showcase.shot("b5_active");
        for (int i = 0; i < 3; ++i) {
            int bottle = i;
            Showcase.step(4, () -> Showcase.invClickItem(Items.f_42589_));
            Showcase.step(3, () -> Showcase.pointAt(Showcase.brewing().handle(bottle)));
            if (i == 0) {
                Showcase.step(10, () -> Showcase.shot("b6_carry_bottle"));
            }
            Showcase.step(6, () -> Showcase.clickBrewing(BrewingStation.Target.PLACE, 0));
        }
        Showcase.step(4, () -> Showcase.invClickItem(Items.f_42593_));
        Showcase.step(3, () -> Showcase.pointAt(Showcase.brewing().handle(4)));
        Showcase.step(6, () -> Showcase.clickBrewing(BrewingStation.Target.PLACE, 0));
        Showcase.step(4, () -> Showcase.invClickItem(Items.f_42588_));
        Showcase.step(3, () -> Showcase.pointAt(Showcase.brewing().handle(3)));
        Showcase.step(10, () -> Showcase.shot("b6_carry_wart"));
        Showcase.step(2, () -> Showcase.clickBrewing(BrewingStation.Target.PLACE, 1));
        Showcase.step(4, Showcase::stash);
        Showcase.step(4, Showcase::pointAside);
        Showcase.step(30, () -> Showcase.shot("b7_brewing_start"));
        Showcase.step(2, () -> {
            BrewingStandBlockEntity stand;
            BlockState empty;
            Minecraft mc = Minecraft.m_91087_();
            BlockState full = empty = Blocks.f_50255_.m_49966_();
            for (BooleanProperty bottle : BrewingStandBlock.f_50905_) {
                full = (BlockState)full.m_61124_((Property)bottle, (Comparable)Boolean.valueOf(true));
            }
            int withBottles = 0;
            int without = 0;
            for (Direction face : new Direction[]{null, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN}) {
                withBottles += mc.m_91289_().m_110910_(full).m_213637_(full, face, RandomSource.m_216335_((long)1L)).size();
                without += mc.m_91289_().m_110910_(empty).m_213637_(empty, face, RandomSource.m_216335_((long)1L)).size();
            }
            Showcase.check("stand's own bottle pictures left out", withBottles == without, "quads with bottles=" + withBottles + " without=" + without);
            BlockEntity patt381723$temp = mc.f_91073_.m_7702_(BREWING);
            boolean known = patt381723$temp instanceof BrewingStandBlockEntity && ClientBrewing.item(stand = (BrewingStandBlockEntity)patt381723$temp, 0).m_150930_(Items.f_42589_) && ClientBrewing.item(stand, 2).m_150930_(Items.f_42589_);
            Showcase.check("client knows the potions in the stand", known, "");
        });
        Showcase.step(100, () -> Showcase.shot("b8_brewing_mid"));
        Showcase.step(2, () -> Showcase.pointAt(Showcase.brewing().handle(3)));
        Showcase.step(10, () -> Showcase.shot("b8_ingredient_tooltip"));
        Showcase.step(2, Showcase::pointAside);
        Showcase.step(150, () -> Showcase.shot("b8_brewing_late"));
        Showcase.step(150, () -> Showcase.shot("b9_done"));
        Showcase.step(4, () -> Showcase.server(p -> {
            BlockEntity patt382420$temp = p.m_284548_().m_7702_(BREWING);
            if (patt382420$temp instanceof BrewingStandBlockEntity) {
                BrewingStandBlockEntity stand = (BrewingStandBlockEntity)patt382420$temp;
                int awkward = 0;
                for (int i = 0; i < 3; ++i) {
                    if (PotionUtils.m_43579_((ItemStack)stand.m_8020_(i)) != Potions.f_43602_) continue;
                    ++awkward;
                }
                Showcase.check("three awkward potions brewed", awkward == 3, "awkward=" + awkward + " wart=" + String.valueOf(stand.m_8020_(3)));
            }
        }));
        Showcase.step(4, () -> {
            BlockEntity patt384767$temp;
            BrewingStandBlockEntity stand;
            int awkward = 0;
            BlockEntity patt383533$temp = Minecraft.m_91087_().f_91073_.m_7702_(BREWING);
            if (patt383533$temp instanceof BrewingStandBlockEntity) {
                stand = (BrewingStandBlockEntity)patt383533$temp;
                for (int i = 0; i < 3; ++i) {
                    if (PotionUtils.m_43579_((ItemStack)ClientBrewing.item(stand, i)) != Potions.f_43602_) continue;
                    ++awkward;
                }
            }
            Showcase.check("client sees the brewed potions", awkward == 3, "awkward=" + awkward);
            if (ModList.get().isLoaded("amendments") && (patt384767$temp = Minecraft.m_91087_().f_91073_.m_7702_(BREWING)) instanceof BrewingStandBlockEntity) {
                stand = (BrewingStandBlockEntity)patt384767$temp;
                Showcase.check("Amendments' own stand data reaches the client", stand.m_8020_(0).m_150930_(Items.f_42589_) && stand.m_8020_(2).m_150930_(Items.f_42589_), "slot0=" + Showcase.describe(stand.m_8020_(0)));
            }
        });
        Showcase.step(2, () -> Showcase.pointAt(Showcase.brewing().handle(0)));
        Showcase.step(6, () -> Showcase.clickBrewing(BrewingStation.Target.ITEM, 0));
        Showcase.step(8, () -> Showcase.shot("b9_bottle_in_hand"));
        Showcase.step(2, Showcase::stash);
        Showcase.step(4, restoreLayout);
        Showcase.step(4, () -> Showcase.report("brewing before leaving"));
        Showcase.step(2, Scene::leave);
        Showcase.step(50, () -> Showcase.shot("b9_after"));
        Showcase.step(2, () -> Showcase.server(p -> p.m_8999_(p.m_284548_(), 600.5, -60.0, 600.5, 0.0f, 20.0f)));
        Showcase.step(80, () -> Showcase.check("brewing stand forgotten with its chunk", !(Minecraft.m_91087_().f_91073_.m_7702_(BREWING) instanceof BrewingStandBlockEntity), "unloaded"));
        Showcase.step(2, () -> Showcase.server(p -> p.m_8999_(p.m_284548_(), (double)BREWING.m_123341_() + 0.5, -60.0, (double)BREWING.m_123343_() + 3.5, 180.0f, 20.0f)));
        Showcase.step(80, () -> {
            Object client = "-";
            int potions = 0;
            BlockEntity patt386300$temp = Minecraft.m_91087_().f_91073_.m_7702_(BREWING);
            if (patt386300$temp instanceof BrewingStandBlockEntity) {
                BrewingStandBlockEntity stand = (BrewingStandBlockEntity)patt386300$temp;
                client = "";
                for (int i = 0; i < 3; ++i) {
                    client = (String)client + Showcase.describe(ClientBrewing.item(stand, i)) + " ";
                    if (!ClientBrewing.item(stand, i).m_150930_(Items.f_42589_)) continue;
                    ++potions;
                }
            }
            int seen = potions;
            String shown = client;
            Showcase.server(p -> {
                int real = 0;
                BlockEntity patt386842$temp = p.m_284548_().m_7702_(BREWING);
                if (patt386842$temp instanceof BrewingStandBlockEntity) {
                    BrewingStandBlockEntity stand = (BrewingStandBlockEntity)patt386842$temp;
                    for (int i = 0; i < 3; ++i) {
                        if (!stand.m_8020_(i).m_150930_(Items.f_42589_)) continue;
                        ++real;
                    }
                }
                Showcase.check("the stand's potions come back with its chunk", real > 0 && seen == real, "server=" + real + " client=" + shown);
            });
        });
        Showcase.step(4, () -> Showcase.shot("b9_chunk_back"));
        Showcase.campfireAct();
    }

    private static long cookedBeef(ServerPlayer p, BlockPos fire) {
        long count = 0L;
        AABB box = new AABB((double)(fire.m_123341_() - 4), (double)(fire.m_123342_() - 2), (double)(fire.m_123343_() - 4), (double)(fire.m_123341_() + 5), (double)(fire.m_123342_() + 3), (double)(fire.m_123343_() + 5));
        for (ItemEntity item : p.m_9236_().m_45976_(ItemEntity.class, box)) {
            if (!item.m_32055_().m_150930_(Items.f_42580_)) continue;
            count += (long)item.m_32055_().m_41613_();
        }
        for (int i = 0; i < p.m_150109_().m_6643_(); ++i) {
            if (!p.m_150109_().m_8020_(i).m_150930_(Items.f_42580_)) continue;
            count += (long)p.m_150109_().m_8020_(i).m_41613_();
        }
        ItemStack carried = p.f_36096_.m_142621_();
        return carried.m_150930_(Items.f_42580_) ? count + (long)carried.m_41613_() : count;
    }

    private static CampfireStation campfire() {
        return Scene.station(CampfireStation.class);
    }

    private static void clickCampfire(CampfireStation.Target expected, int button) {
        CampfireStation station = Showcase.campfire();
        if (station == null || station.target != expected) {
            REPORT.add("cursor missed campfire target " + String.valueOf((Object)expected) + ": was " + String.valueOf(station == null ? "-" : station.target));
        }
        Showcase.screenClick(button);
    }

    private static void campfireAct() {
        Showcase.step(2, () -> {
            act = "f";
            Showcase.server(p -> {
                if ("f".equals(FROM)) {
                    p.m_8061_(EquipmentSlot.LEGS, new ItemStack((ItemLike)Items.f_42470_));
                    p.m_8061_(EquipmentSlot.FEET, new ItemStack((ItemLike)Items.f_42471_));
                }
                p.m_8999_(p.m_284548_(), (double)CAMPFIRE.m_123341_() + 0.5, -60.0, (double)CAMPFIRE.m_123343_() + 2.9, 180.0f, 24.0f);
            });
        });
        Showcase.step(30, () -> Showcase.shot("f0_hint"));
        Showcase.campfireHintFadeScript("f");
        Showcase.step(2, () -> {
            BlockHitResult hit;
            Minecraft mc = Minecraft.m_91087_();
            HitResult patt392899$temp = mc.f_91077_;
            boolean looking = patt392899$temp instanceof BlockHitResult && (hit = (BlockHitResult)patt392899$temp).m_82425_().equals((Object)CAMPFIRE);
            Showcase.check("looking at the campfire for the hint", looking, "hit=" + String.valueOf(mc.f_91077_));
            introShot = 0;
            outroShot = 0;
            waitingForActive = true;
            KeyMapping.m_90835_((InputConstants.Key)CampfireHint.SIT.getKey());
        });
        Showcase.step(6, () -> Showcase.check("sit key starts the campfire scene", Scene.station instanceof CampfireStation, "station=" + String.valueOf(Scene.station)));
    }

    private static void campfireHintFadeScript(String prefix) {
        Showcase.step(2, () -> Minecraft.m_91087_().f_91074_.m_146922_(90.0f));
        Showcase.step(1, () -> Showcase.shot(prefix + "0_hint_fade_100"));
        Showcase.step(0, () -> Showcase.shot(prefix + "0_hint_fade_150"));
        Showcase.step(0, () -> Showcase.shot(prefix + "0_hint_fade_200"));
        Showcase.step(0, () -> Showcase.shot(prefix + "0_hint_fade_250"));
        Showcase.step(0, () -> Showcase.shot(prefix + "0_hint_fade_300"));
        Showcase.step(0, () -> Showcase.shot(prefix + "0_hint_fade_350"));
        Showcase.step(4, () -> Showcase.shot(prefix + "0_hint_fade_600"));
        Showcase.step(2, () -> Minecraft.m_91087_().f_91074_.m_146922_(180.0f));
        Showcase.step(20, () -> Showcase.shot(prefix + "0_hint_restored"));
    }

    private static void campfireScript() {
        Runnable restoreLayout = Showcase.spatialEditorScript();
        Showcase.shot("f5_active");
        Showcase.step(4, () -> Showcase.invClickItem(Items.f_42581_));
        Showcase.step(3, () -> Showcase.pointAt(Showcase.campfire().spot(0)));
        Showcase.step(10, () -> Showcase.shot("f6_carry_chicken"));
        Showcase.step(2, () -> Showcase.clickCampfire(CampfireStation.Target.PLACE, 1));
        Showcase.step(4, () -> Showcase.pointAt(Showcase.campfire().spot(2)));
        Showcase.step(6, () -> Showcase.clickCampfire(CampfireStation.Target.PLACE, 1));
        Showcase.step(4, Showcase::stash);
        Showcase.step(4, () -> Showcase.invClickItem(Items.f_42658_));
        Showcase.step(3, () -> Showcase.pointAt(Showcase.campfire().spot(1)));
        Showcase.step(6, () -> Showcase.clickCampfire(CampfireStation.Target.PLACE, 1));
        Showcase.step(4, Showcase::stash);
        Showcase.step(4, Showcase::pointAside);
        Showcase.step(14, () -> Showcase.shot("f7_food_on_fire"));
        Showcase.step(2, () -> Showcase.server(p -> {
            int food = 0;
            BlockEntity patt395473$temp = p.m_284548_().m_7702_(CAMPFIRE);
            if (patt395473$temp instanceof CampfireBlockEntity) {
                CampfireBlockEntity fire = (CampfireBlockEntity)patt395473$temp;
                for (ItemStack stack : fire.m_59065_()) {
                    food += stack.m_41619_() ? 0 : 1;
                }
            }
            Showcase.check("three pieces on the fire", food == 3, "food=" + food);
        }));
        int[] beforeCooldown = new int[]{0};
        Showcase.step(2, () -> {
            CampfireStation station = Showcase.campfire();
            Showcase.check("campfire timers reach the seated player", station != null && station.syncedCookingTime(0) > 0 && station.syncedProgress(0) > 0 && station.syncedLit(), "time=" + (String)(station == null ? "-" : station.syncedProgress(0) + "/" + station.syncedCookingTime(0)));
            beforeCooldown[0] = station == null ? 0 : station.syncedProgress(0);
            Showcase.server(p -> {
                ServerLevel level = p.m_284548_();
                BlockState state = level.m_8055_(CAMPFIRE);
                level.m_7731_(CAMPFIRE, (BlockState)state.m_61124_((Property)CampfireBlock.f_51227_, (Comparable)Boolean.valueOf(false)), 3);
            });
        });
        Showcase.step(20, () -> Showcase.shot("f7_cooling"));
        Showcase.step(2, () -> {
            CampfireStation station = Showcase.campfire();
            Showcase.check("extinguished campfire cools its progress", station != null && !station.syncedLit() && station.syncedProgress(0) < beforeCooldown[0], "progress=" + String.valueOf(station == null ? "-" : Integer.valueOf(station.syncedProgress(0))));
            Showcase.server(p -> {
                ServerLevel level = p.m_284548_();
                BlockState state = level.m_8055_(CAMPFIRE);
                level.m_7731_(CAMPFIRE, (BlockState)state.m_61124_((Property)CampfireBlock.f_51227_, (Comparable)Boolean.valueOf(true)), 3);
            });
        });
        Showcase.step(12, () -> Showcase.shot("f7_relit"));
        Showcase.step(2, () -> Showcase.check("relit campfire resumes cooking", Showcase.campfire() != null && Showcase.campfire().syncedLit() && Showcase.campfire().syncedProgress(0) > 0, "progress=" + String.valueOf(Showcase.campfire() == null ? "-" : Integer.valueOf(Showcase.campfire().syncedProgress(0)))));
        Showcase.step(2, () -> Showcase.pointAt(Showcase.campfire().spot(Showcase.farSpot())));
        Showcase.step(14, () -> Showcase.shot("f8_lean_far_spot"));
        Showcase.step(2, () -> Showcase.check("leaning in to reach the far side of the fire", CameraRig.last() != null && Minecraft.m_91087_().f_91074_ != null && Math.hypot(CameraRig.last().pos().f_82479_ - (double)CAMPFIRE.m_123341_() - 0.5, CameraRig.last().pos().f_82481_ - (double)CAMPFIRE.m_123343_() - 0.5) < 1.2, "camera=" + String.valueOf(CameraRig.last() == null ? "-" : CameraRig.last().pos())));
        Showcase.step(2, () -> Showcase.pointAt(Showcase.campfire().spot(0)));
        Showcase.step(8, () -> Showcase.shot("f8_hover_food"));
        Showcase.step(2, () -> Showcase.clickCampfire(CampfireStation.Target.FOOD, 0));
        Showcase.step(8, () -> Showcase.shot("f8_taken"));
        Showcase.step(2, () -> Showcase.check("chicken taken off the fire", Showcase.carried().m_150930_(Items.f_42581_), "carried=" + String.valueOf(Showcase.carried())));
        Showcase.step(2, () -> Showcase.pointAt(Showcase.campfire().spot(0)));
        Showcase.step(6, () -> Showcase.clickCampfire(CampfireStation.Target.PLACE, 0));
        Showcase.step(60, () -> Showcase.shot("f9_resting"));
        Showcase.step(4, () -> Showcase.invClickItem(Items.f_42579_));
        Showcase.step(3, () -> Showcase.pointAt(Showcase.campfire().spot(3)));
        Showcase.step(6, () -> Showcase.clickCampfire(CampfireStation.Target.PLACE, 1));
        Showcase.step(4, Showcase::stash);
        Showcase.step(24, () -> {
            CampfireStation station = Showcase.campfire();
            Showcase.check("seated beef timer advances on the client", station != null && station.syncedLit() && station.syncedCookingTime(3) > 0 && station.syncedProgress(3) > 0, "timer=" + (String)(station == null ? "-" : station.syncedProgress(3) + "/" + station.syncedCookingTime(3)));
            Showcase.server(p -> {
                BlockEntity patt399264$temp = p.m_9236_().m_7702_(CAMPFIRE);
                if (patt399264$temp instanceof CampfireBlockEntity) {
                    CampfireBlockEntity fire = (CampfireBlockEntity)patt399264$temp;
                    Showcase.check("seated beef cooks on the server", ((ItemStack)fire.m_59065_().get(3)).m_150930_(Items.f_42579_) && fire.f_59044_[3] > 0 && fire.f_59043_[3] > 0, "timer=" + fire.f_59043_[3] + "/" + fire.f_59044_[3]);
                    Showcase.beefPace[0] = p.m_9236_().m_46467_();
                    Showcase.beefPace[1] = fire.f_59043_[3];
                }
            });
        });
        Showcase.step(20, () -> Showcase.server(p -> {
            BlockEntity patt399998$temp = p.m_9236_().m_7702_(CAMPFIRE);
            if (patt399998$temp instanceof CampfireBlockEntity) {
                CampfireBlockEntity fire = (CampfireBlockEntity)patt399998$temp;
                long ticks = p.m_9236_().m_46467_() - beefPace[0];
                long cooked = (long)fire.f_59043_[3] - beefPace[1];
                Showcase.check("seated beef cooks at the vanilla pace", ticks > 0L && cooked > 0L && cooked <= ticks, "progress +" + cooked + " in " + ticks + " ticks");
            }
        }));
        Showcase.step(2, () -> Showcase.server(p -> {
            BlockEntity patt400496$temp = p.m_9236_().m_7702_(CAMPFIRE);
            if (patt400496$temp instanceof CampfireBlockEntity) {
                CampfireBlockEntity fire = (CampfireBlockEntity)patt400496$temp;
                fire.f_59044_[3] = 0;
                fire.f_59043_[3] = 0;
                fire.m_6596_();
            }
        }));
        Showcase.step(12, () -> {
            CampfireStation station = Showcase.campfire();
            Showcase.check("seated beef recovers a missing timer", station != null && station.syncedCookingTime(3) > 0 && station.syncedProgress(3) > 0, "timer=" + (String)(station == null ? "-" : station.syncedProgress(3) + "/" + station.syncedCookingTime(3)));
        });
        Showcase.step(2, () -> Showcase.server(p -> {
            Showcase.beefPace[2] = Showcase.cookedBeef(p, CAMPFIRE);
            BlockEntity patt401407$temp = p.m_9236_().m_7702_(CAMPFIRE);
            if (patt401407$temp instanceof CampfireBlockEntity) {
                CampfireBlockEntity fire = (CampfireBlockEntity)patt401407$temp;
                fire.f_59043_[3] = Math.max(0, fire.f_59044_[3] - 4);
                fire.m_6596_();
            }
        }));
        Showcase.step(16, () -> Showcase.server(p -> {
            BlockEntity patt401721$temp = p.m_9236_().m_7702_(CAMPFIRE);
            if (patt401721$temp instanceof CampfireBlockEntity) {
                CampfireBlockEntity fire = (CampfireBlockEntity)patt401721$temp;
                long cooked = Showcase.cookedBeef(p, CAMPFIRE) - beefPace[2];
                Showcase.check("seated beef pops off cooked once", ((ItemStack)fire.m_59065_().get(3)).m_41619_() && cooked == 1L, "spot=" + String.valueOf(fire.m_59065_().get(3)) + " cooked=" + cooked);
            }
        }));
        Showcase.step(4, () -> Showcase.report("campfire before standing up"));
        Showcase.step(2, Scene::leave);
        Showcase.step(50, () -> Showcase.shot("f9_after"));
        Showcase.step(2, restoreLayout);
        Showcase.soulCampfireAct();
    }

    private static void soulCampfireAct() {
        Showcase.step(2, () -> {
            act = "u";
            Showcase.server(p -> p.m_8999_(p.m_284548_(), (double)SOUL_CAMPFIRE.m_123341_() + 0.5, -60.0, (double)SOUL_CAMPFIRE.m_123343_() + 2.9, 180.0f, 24.0f));
        });
        Showcase.step(30, () -> Showcase.shot("u0_hint"));
        Showcase.campfireHintFadeScript("u");
        Showcase.step(2, () -> {
            introShot = 0;
            outroShot = 0;
            waitingForActive = true;
            KeyMapping.m_90835_((InputConstants.Key)CampfireHint.SIT.getKey());
        });
        Showcase.step(6, () -> Showcase.check("sit key works at a soul campfire too", Showcase.campfire() != null && Showcase.campfire().pos.equals((Object)SOUL_CAMPFIRE), "station=" + String.valueOf(Scene.station)));
    }

    private static void soulCampfireScript() {
        Runnable restoreLayout = Showcase.spatialEditorScript();
        Showcase.shot("u5_active");
        Showcase.step(4, () -> Showcase.invClickItem(Items.f_42581_));
        Showcase.step(3, () -> Showcase.pointAt(Showcase.campfire().spot(0)));
        Showcase.step(6, () -> Showcase.clickCampfire(CampfireStation.Target.PLACE, 1));
        Showcase.step(2, Showcase::stash);
        Showcase.step(2, () -> Showcase.server(p -> {
            BlockEntity patt403916$temp = p.m_284548_().m_7702_(SOUL_CAMPFIRE);
            if (patt403916$temp instanceof CampfireBlockEntity) {
                CampfireBlockEntity fire = (CampfireBlockEntity)patt403916$temp;
                fire.f_59044_[0] = 80;
                fire.m_6596_();
            }
        }));
        Showcase.step(12, () -> Showcase.shot("u6_cooking_early"));
        int[] early = new int[]{0};
        Showcase.step(2, () -> {
            CampfireStation station = Showcase.campfire();
            Showcase.check("soul campfire has its own cooking timer", station != null && station.syncedCookingTime(0) == 80 && station.syncedProgress(0) > 0 && station.syncedLit(), "time=" + (String)(station == null ? "-" : station.syncedProgress(0) + "/" + station.syncedCookingTime(0)));
            early[0] = station == null ? 0 : station.syncedProgress(0);
        });
        Showcase.step(24, () -> Showcase.shot("u7_cooking_late"));
        Showcase.step(2, () -> Showcase.check("soul campfire progress rises", Showcase.campfire() != null && Showcase.campfire().syncedProgress(0) > early[0], "progress=" + String.valueOf(Showcase.campfire() == null ? "-" : Integer.valueOf(Showcase.campfire().syncedProgress(0)))));
        Showcase.step(52, () -> Showcase.shot("u8_cooked"));
        Showcase.step(2, () -> {
            CampfireBlockEntity fire;
            BlockEntity patt405257$temp;
            Showcase.check("cooking indicator clears when food finishes", Showcase.campfire() != null && Showcase.campfire().syncedCookingTime(0) == 0 && (patt405257$temp = Minecraft.m_91087_().f_91073_.m_7702_(SOUL_CAMPFIRE)) instanceof CampfireBlockEntity && ((ItemStack)(fire = (CampfireBlockEntity)patt405257$temp).m_59065_().get(0)).m_41619_(), "time=" + String.valueOf(Showcase.campfire() == null ? "-" : Integer.valueOf(Showcase.campfire().syncedCookingTime(0))));
        });
        Showcase.step(4, () -> Showcase.invClickItem(Items.f_42579_));
        Showcase.step(3, () -> Showcase.pointAt(Showcase.campfire().spot(1)));
        Showcase.step(6, () -> Showcase.clickCampfire(CampfireStation.Target.PLACE, 1));
        Showcase.step(4, Showcase::stash);
        Showcase.step(24, () -> {
            CampfireStation station = Showcase.campfire();
            Showcase.check("soul campfire beef timer advances on the client", station != null && station.syncedLit() && station.syncedCookingTime(1) > 0 && station.syncedProgress(1) > 0, "timer=" + (String)(station == null ? "-" : station.syncedProgress(1) + "/" + station.syncedCookingTime(1)));
            Showcase.server(p -> {
                BlockEntity patt406244$temp = p.m_9236_().m_7702_(SOUL_CAMPFIRE);
                if (patt406244$temp instanceof CampfireBlockEntity) {
                    CampfireBlockEntity fire = (CampfireBlockEntity)patt406244$temp;
                    Showcase.check("soul campfire beef cooks on the server", ((ItemStack)fire.m_59065_().get(1)).m_150930_(Items.f_42579_) && fire.f_59044_[1] > 0 && fire.f_59043_[1] > 0, "timer=" + fire.f_59043_[1] + "/" + fire.f_59044_[1]);
                    Showcase.beefPace[0] = p.m_9236_().m_46467_();
                    Showcase.beefPace[1] = fire.f_59043_[1];
                }
            });
        });
        Showcase.step(20, () -> Showcase.server(p -> {
            BlockEntity patt406883$temp = p.m_9236_().m_7702_(SOUL_CAMPFIRE);
            if (patt406883$temp instanceof CampfireBlockEntity) {
                CampfireBlockEntity fire = (CampfireBlockEntity)patt406883$temp;
                long ticks = p.m_9236_().m_46467_() - beefPace[0];
                long cooked = (long)fire.f_59043_[1] - beefPace[1];
                Showcase.check("soul campfire beef cooks at the vanilla pace", ticks > 0L && cooked > 0L && cooked <= ticks, "progress +" + cooked + " in " + ticks + " ticks");
                Showcase.beefPace[2] = Showcase.cookedBeef(p, SOUL_CAMPFIRE);
                fire.f_59043_[1] = Math.max(0, fire.f_59044_[1] - 4);
                fire.m_6596_();
            }
        }));
        Showcase.step(16, () -> Showcase.server(p -> {
            BlockEntity patt407572$temp = p.m_9236_().m_7702_(SOUL_CAMPFIRE);
            if (patt407572$temp instanceof CampfireBlockEntity) {
                CampfireBlockEntity fire = (CampfireBlockEntity)patt407572$temp;
                long cooked = Showcase.cookedBeef(p, SOUL_CAMPFIRE) - beefPace[2];
                Showcase.check("soul campfire beef pops off cooked once", ((ItemStack)fire.m_59065_().get(1)).m_41619_() && cooked == 1L, "spot=" + String.valueOf(fire.m_59065_().get(1)) + " cooked=" + cooked);
            }
        }));
        Showcase.step(2, restoreLayout);
        Showcase.step(2, Scene::leave);
        Showcase.step(50, () -> Showcase.shot("u9_after"));
        Showcase.lecternAct();
    }

    private static ItemStack notesBook() {
        ItemStack book = new ItemStack((ItemLike)Items.f_42614_);
        ListTag pages = new ListTag();
        for (String page : List.of("\u0417\u0430\u043f\u0438\u0441\u043a\u0438 \u043c\u0430\u0441\u0442\u0435\u0440\u0430.\n\n\u041a\u043d\u0438\u0433\u0438 \u043d\u0430 \u0440\u0435\u0437\u043d\u043e\u0439 \u043f\u043e\u043b\u043a\u0435 \u0434\u043e\u0441\u0442\u0430\u044e\u0442 \u043f\u043e \u043e\u0434\u043d\u043e\u0439: \u043a\u043e\u0440\u0435\u0448\u043e\u043a \u043f\u043e\u0434 \u0440\u0443\u043a\u043e\u0439 \u0447\u0443\u0442\u044c \u0432\u044b\u0434\u0432\u0438\u0433\u0430\u0435\u0442\u0441\u044f, \u0430 \u043a\u043b\u0438\u043a \u0432\u044b\u0442\u044f\u0433\u0438\u0432\u0430\u0435\u0442 \u043a\u043d\u0438\u0433\u0443.", "\u041a\u043d\u0438\u0433\u0443 \u0441 \u0442\u0435\u043a\u0441\u0442\u043e\u043c \u043c\u043e\u0436\u043d\u043e \u0440\u0430\u0441\u043a\u0440\u044b\u0442\u044c \u043f\u0440\u044f\u043c\u043e \u0443 \u043f\u043e\u043b\u043a\u0438 \u0438 \u0447\u0438\u0442\u0430\u0442\u044c, \u043a\u0430\u043a \u043d\u0430 \u043a\u0430\u0444\u0435\u0434\u0440\u0435: \u043b\u0438\u0441\u0442 \u0437\u0430 \u043b\u0438\u0441\u0442\u043e\u043c.", "\u0417\u0430\u0447\u0430\u0440\u043e\u0432\u0430\u043d\u043d\u044b\u0435 \u043a\u043d\u0438\u0433\u0438 \u043c\u0435\u0440\u0446\u0430\u044e\u0442 \u043d\u0430 \u043f\u043e\u043b\u043a\u0435, \u0430 \u0438\u0445 \u0447\u0430\u0440\u044b \u0432\u0438\u0434\u043d\u044b \u0432 \u043f\u043e\u0434\u0441\u043a\u0430\u0437\u043a\u0435.")) {
            pages.add((Object)StringTag.m_129297_((String)page));
        }
        book.m_41784_().m_128365_("pages", (Tag)pages);
        return book;
    }

    private static ItemStack storyBook() {
        String[][] text = new String[][]{{"\u041a\u0438\u043d\u0435\u043c\u0430\u0442\u043e\u0433\u0440\u0430\u0444\u0438\u0447\u043d\u044b\u0435 \u0432\u0437\u0430\u0438\u043c\u043e\u0434\u0435\u0439\u0441\u0442\u0432\u0438\u044f", "\u042d\u0442\u0430 \u043a\u043d\u0438\u0433\u0430 \u043b\u0435\u0436\u0438\u0442 \u043d\u0430 \u043a\u0430\u0444\u0435\u0434\u0440\u0435, \u0447\u0442\u043e\u0431\u044b \u0435\u0451 \u043c\u043e\u0436\u043d\u043e \u0431\u044b\u043b\u043e \u043f\u0440\u043e\u0447\u0438\u0442\u0430\u0442\u044c \u043f\u0440\u044f\u043c\u043e \u0432 \u043c\u0438\u0440\u0435. \u041a\u0430\u043c\u0435\u0440\u0430 \u0441\u043a\u043b\u043e\u043d\u044f\u0435\u0442\u0441\u044f \u043d\u0430\u0434 \u0441\u0442\u0440\u0430\u043d\u0438\u0446\u0430\u043c\u0438, \u0430 \u0440\u0443\u043a\u0430 \u043f\u0435\u0440\u0435\u0432\u043e\u0440\u0430\u0447\u0438\u0432\u0430\u0435\u0442 \u043b\u0438\u0441\u0442 \u0437\u0430 \u0443\u0433\u043e\u043b\u043e\u043a."}, {"\u0413\u043b\u0430\u0432\u0430 \u043f\u0435\u0440\u0432\u0430\u044f", "\u0412 \u043c\u0430\u0441\u0442\u0435\u0440\u0441\u043a\u043e\u0439 \u043f\u0430\u0445\u043d\u0435\u0442 \u0434\u0435\u0440\u0435\u0432\u043e\u043c \u0438 \u0443\u0433\u043b\u0451\u043c. \u041d\u0430 \u0432\u0435\u0440\u0441\u0442\u0430\u043a\u0435 \u043b\u0435\u0436\u0430\u0442 \u0434\u043e\u0441\u043a\u0438, \u0432 \u043f\u0435\u0447\u0438 \u0434\u043e\u0433\u043e\u0440\u0430\u0435\u0442 \u0443\u0433\u043e\u043b\u044c, \u0430 \u0443 \u0441\u0442\u0435\u043d\u044b \u0436\u0434\u0451\u0442 \u043d\u0430\u043a\u043e\u0432\u0430\u043b\u044c\u043d\u044f \u0441 \u0442\u044f\u0436\u0451\u043b\u044b\u043c \u043c\u043e\u043b\u043e\u0442\u043e\u043c."}, {"", "\u0415\u0441\u043b\u0438 \u043f\u043e\u0434\u043e\u0439\u0442\u0438 \u043a \u0441\u0442\u043e\u043b\u0443 \u0437\u0430\u0447\u0430\u0440\u043e\u0432\u0430\u043d\u0438\u0439, \u043d\u0430\u0434 \u043a\u043d\u0438\u0433\u043e\u0439 \u0437\u0430\u0433\u043e\u0440\u0430\u044e\u0442\u0441\u044f \u0440\u0443\u043d\u044b. \u041b\u0430\u0437\u0443\u0440\u0438\u0442 \u043b\u043e\u0436\u0438\u0442\u0441\u044f \u0432 \u0447\u0430\u0448\u0443, \u043f\u0440\u0435\u0434\u043c\u0435\u0442 \u043f\u043e\u0434\u043d\u0438\u043c\u0430\u0435\u0442\u0441\u044f \u0432 \u0432\u043e\u0437\u0434\u0443\u0445, \u0438 \u043f\u043e\u043b\u043a\u0438 \u043e\u0442\u0434\u0430\u044e\u0442 \u0435\u043c\u0443 \u0441\u0432\u043e\u0438 \u0437\u043d\u0430\u043a\u0438."}, {"\u0413\u043b\u0430\u0432\u0430 \u0432\u0442\u043e\u0440\u0430\u044f", "\u0422\u043a\u0430\u0446\u043a\u0438\u0439 \u0441\u0442\u0430\u043d\u043e\u043a \u0434\u0435\u0440\u0436\u0438\u0442 \u0431\u0435\u043b\u044b\u0439 \u0444\u043b\u0430\u0433. \u0427\u0435\u043b\u043d\u043e\u043a \u043b\u0435\u0442\u0430\u0435\u0442 \u0438\u0437 \u0440\u0443\u043a\u0438 \u0432 \u0440\u0443\u043a\u0443, \u0438 \u0443\u0437\u043e\u0440 \u043f\u0440\u043e\u0441\u0442\u0443\u043f\u0430\u0435\u0442 \u0440\u044f\u0434 \u0437\u0430 \u0440\u044f\u0434\u043e\u043c, \u043f\u043e\u043a\u0430 \u043d\u0435 \u0441\u0442\u0430\u043d\u0435\u0442 \u043a\u0440\u0430\u0441\u043d\u044b\u043c \u043a\u0440\u0435\u0441\u0442\u043e\u043c."}, {"", "\u0423 \u0432\u0430\u0440\u043e\u0447\u043d\u043e\u0439 \u0441\u0442\u043e\u0439\u043a\u0438 \u043c\u043e\u0436\u043d\u043e \u043f\u0440\u0438\u0441\u0435\u0441\u0442\u044c. \u0411\u0443\u0442\u044b\u043b\u043a\u0438 \u0441\u0442\u043e\u044f\u0442 \u043d\u0430 \u0435\u0451 \u043a\u0430\u043c\u0435\u043d\u043d\u044b\u0445 \u043d\u043e\u0436\u043a\u0430\u0445, \u043d\u0435\u0437\u0435\u0440\u0441\u043a\u0438\u0439 \u043d\u0430\u0440\u043e\u0441\u0442 \u0442\u0430\u0435\u0442 \u0432 \u0447\u0430\u0448\u0435, \u0438 \u043a\u0430\u043f\u043b\u0438 \u0441\u0442\u0435\u043a\u0430\u044e\u0442 \u0432\u043d\u0438\u0437."}, {"\u0413\u043b\u0430\u0432\u0430 \u0442\u0440\u0435\u0442\u044c\u044f", "\u041c\u0430\u044f\u043a \u0441\u0442\u043e\u0438\u0442 \u043d\u0430 \u043f\u0438\u0440\u0430\u043c\u0438\u0434\u0435 \u0438\u0437 \u0436\u0435\u043b\u0435\u0437\u0430, \u0437\u043e\u043b\u043e\u0442\u0430, \u0438\u0437\u0443\u043c\u0440\u0443\u0434\u043e\u0432 \u0438 \u0430\u043b\u043c\u0430\u0437\u043e\u0432. \u041d\u0430\u0434 \u043d\u0438\u043c \u0432\u0438\u0441\u044f\u0442 \u0441\u0438\u043b\u044b, \u0430 \u043f\u043b\u0430\u0442\u0430 \u043f\u0430\u0440\u0438\u0442 \u043d\u0430\u0434 \u0441\u0442\u0435\u043a\u043b\u043e\u043c."}, {"\u041a\u043e\u043d\u0435\u0446", "\u0417\u0430\u043a\u0440\u043e\u0439 \u043a\u043d\u0438\u0433\u0443 \u0438 \u0437\u0430\u0431\u0435\u0440\u0438 \u0435\u0451 \u0441 \u0441\u043e\u0431\u043e\u0439. \u0418\u043b\u0438 \u043e\u0441\u0442\u0430\u0432\u044c \u043d\u0430 \u043a\u0430\u0444\u0435\u0434\u0440\u0435: \u0441\u043b\u0435\u0434\u0443\u044e\u0449\u0438\u0439 \u0447\u0438\u0442\u0430\u0442\u0435\u043b\u044c \u043e\u0442\u043a\u0440\u043e\u0435\u0442 \u0435\u0451 \u043d\u0430 \u0442\u043e\u0439 \u0436\u0435 \u0441\u0442\u0440\u0430\u043d\u0438\u0446\u0435."}};
        ListTag pages = new ListTag();
        for (String[] page : text) {
            MutableComponent c = Component.m_237119_();
            if (!page[0].isEmpty()) {
                c.m_7220_((Component)Component.m_237113_((String)page[0]).m_130944_(new ChatFormatting[]{ChatFormatting.BOLD, ChatFormatting.DARK_RED})).m_130946_("\n\n");
            }
            c.m_130946_(page[1]);
            pages.add((Object)StringTag.m_129297_((String)Component.Serializer.m_130703_((Component)c)));
        }
        ItemStack book = new ItemStack((ItemLike)Items.f_42615_);
        CompoundTag tag = book.m_41784_();
        tag.m_128359_("title", "\u041c\u0430\u0441\u0442\u0435\u0440\u0441\u043a\u0430\u044f");
        tag.m_128359_("author", "dcvke");
        tag.m_128405_("generation", 0);
        tag.m_128379_("resolved", true);
        tag.m_128365_("pages", (Tag)pages);
        return book;
    }

    private static LecternStation lectern() {
        return Scene.station(LecternStation.class);
    }

    private static void lecternAct() {
        Showcase.nextAct("k", new Vec3((double)LECTERN.m_123341_() + 0.5, -60.0, (double)LECTERN.m_123343_() + 2.6), 180.0f, LECTERN, new Vec3(0.5, 0.9, 0.6), Direction.UP);
    }

    private static int lecternPage(ServerPlayer p) {
        int n;
        BlockEntity blockEntity = p.m_284548_().m_7702_(LECTERN);
        if (blockEntity instanceof LecternBlockEntity) {
            LecternBlockEntity lectern = (LecternBlockEntity)blockEntity;
            n = lectern.m_59568_();
        } else {
            n = -1;
        }
        return n;
    }

    private static void lecternScript() {
        Runnable restoreLayout = Showcase.spatialEditorScript();
        Showcase.step(2, Scene::leave);
        Showcase.step(40, () -> {
            Showcase.check("lectern profile outside scene", StationDisplayLayouts.customLectern() && Scene.phase == Scene.Phase.NONE, "vanilla display uses saved geometry");
            Showcase.shot("xk4_book_outside");
        });
        Showcase.step(2, () -> Showcase.useAt(LECTERN, new Vec3(0.5, 0.9, 0.6), Direction.UP));
        Showcase.step(65, () -> {
            Showcase.check("lectern reopens with saved geometry", Showcase.lectern() != null && Scene.phase == Scene.Phase.ACTIVE, "book remains readable");
            Showcase.shot("k5_active");
        });
        Showcase.step(2, () -> Showcase.check("lectern book open at the first spread", Showcase.lectern() != null && Showcase.lectern().spread() == 0, "spread=" + String.valueOf(Showcase.lectern() == null ? "-" : Integer.valueOf(Showcase.lectern().spread()))));
        Showcase.step(2, () -> Showcase.pointAt(Showcase.lectern().pageCenter(false)));
        Showcase.step(10, () -> Showcase.shot("k6_peek"));
        Showcase.step(2, () -> Showcase.screenClick(0));
        Showcase.step(2, () -> Showcase.shot("k6_turn_010"));
        Showcase.step(2, () -> Showcase.shot("k6_turn_020"));
        Showcase.step(2, () -> Showcase.shot("k6_turn_030"));
        Showcase.step(2, () -> Showcase.shot("k6_turn_040"));
        Showcase.step(3, () -> Showcase.shot("k6_turn_055"));
        Showcase.step(14, () -> Showcase.shot("k7_spread1"));
        Showcase.step(2, () -> Showcase.screenClick(0));
        Showcase.step(20, () -> Showcase.shot("k7_spread2"));
        Showcase.step(2, () -> Showcase.server(p -> Showcase.check("lectern turned to page 4 (the left of the third spread)", Showcase.lecternPage(p) == 3, "page=" + Showcase.lecternPage(p))));
        Showcase.step(2, () -> Showcase.pointAt(Showcase.lectern().pageCenter(true)));
        Showcase.step(10, () -> Showcase.shot("k7_peek_back"));
        Showcase.step(2, () -> Showcase.lectern().scroll(1.0));
        Showcase.step(4, () -> Showcase.shot("k7_back_020"));
        Showcase.step(16, () -> Showcase.shot("k7_back_done"));
        Showcase.step(2, () -> Showcase.server(p -> Showcase.check("lectern turned back by the wheel", Showcase.lecternPage(p) == 1, "page=" + Showcase.lecternPage(p))));
        Showcase.step(2, () -> Showcase.lectern().keyPressed(262));
        Showcase.step(4, () -> Showcase.lectern().keyPressed(262));
        Showcase.step(4, () -> Showcase.lectern().keyPressed(262));
        Showcase.step(30, () -> Showcase.shot("k8_last_spread"));
        Showcase.step(2, () -> Showcase.check("arrow keys turned to the last spread", Showcase.lectern() != null && Showcase.lectern().spread() == 3, "spread=" + String.valueOf(Showcase.lectern() == null ? "-" : Integer.valueOf(Showcase.lectern().spread()))));
        Showcase.step(2, () -> Showcase.pointAt(Showcase.lectern().pageCenter(false)));
        Showcase.step(4, () -> Showcase.lectern().click(0, true));
        Showcase.step(5, () -> Showcase.shot("k8_take_025"));
        Showcase.step(5, () -> Showcase.shot("k8_take_050"));
        Showcase.step(5, () -> Showcase.shot("k8_take_075"));
        Showcase.step(30, () -> Showcase.server(p -> {
            boolean taken = p.m_150109_().m_216874_(stack -> stack.m_150930_(Items.f_42615_));
            boolean empty = (Boolean)p.m_284548_().m_8055_(LECTERN).m_61143_((Property)LecternBlock.f_54467_) == false;
            Showcase.check("book taken off the lectern", taken && empty, "inventory=" + taken + " lecternEmpty=" + empty);
        }));
        Showcase.step(2, restoreLayout);
        Showcase.step(30, () -> Showcase.shot("k9_after"));
        Showcase.step(2, () -> Showcase.server(p -> LecternBlock.m_269125_(null, (Level)p.m_284548_(), (BlockPos)LECTERN, (BlockState)p.m_284548_().m_8055_(LECTERN), (ItemStack)Showcase.storyBook())));
        Showcase.shelfAct();
    }

    private static ShelfStation shelf() {
        return Scene.station(ShelfStation.class);
    }

    private static void clickShelf(ShelfStation.Target expected, int button) {
        ShelfStation station = Showcase.shelf();
        if (station == null || station.target != expected) {
            REPORT.add("cursor missed shelf target " + String.valueOf((Object)expected) + ": was " + (String)(station == null ? "-" : String.valueOf((Object)station.target) + " slot " + station.hoverSlot));
        }
        Showcase.screenClick(button);
    }

    private static ItemStack shelfItem(ServerPlayer p, int slot) {
        ItemStack itemStack;
        BlockEntity blockEntity = p.m_284548_().m_7702_(SHELF);
        if (blockEntity instanceof ChiseledBookShelfBlockEntity) {
            ChiseledBookShelfBlockEntity shelf = (ChiseledBookShelfBlockEntity)blockEntity;
            itemStack = shelf.m_8020_(slot);
        } else {
            itemStack = ItemStack.f_41583_;
        }
        return itemStack;
    }

    private static void hold(int slot, ItemStack stack) {
        Showcase.server(p -> {
            p.m_150109_().m_6836_(slot, stack);
            p.m_150109_().f_35977_ = slot;
        });
        Minecraft.m_91087_().f_91074_.m_150109_().f_35977_ = slot;
        Minecraft.m_91087_().m_91403_().m_104955_((Packet)new ServerboundSetCarriedItemPacket(slot));
    }

    private static void shelfAct() {
        if (ONLY && !"e".equals(FROM)) {
            Showcase.step(2, () -> Showcase.finish(Minecraft.m_91087_()));
            return;
        }
        Showcase.step(2, () -> Showcase.hold(7, ItemStack.f_41583_));
        Showcase.nextAct("e", new Vec3((double)SHELF.m_123341_() + 0.5, -60.0, (double)SHELF.m_123343_() + 3.0), 180.0f, SHELF, SHELF_SLOTS[1], Direction.SOUTH);
    }

    private static void shelfScript() {
        Runnable restoreLayout = Showcase.spatialEditorScript();
        Showcase.shot("e5_active");
        Showcase.step(2, () -> Showcase.pointAt(Showcase.shelf().slotPoint(0)));
        Showcase.step(12, () -> Showcase.shot("e6_hover_enchanted"));
        Showcase.step(2, () -> Showcase.clickShelf(ShelfStation.Target.BOOK, 0));
        Showcase.moveShots("e7_take", 0.15, 0.3, 0.45, 0.6, 0.8, 1.0);
        Showcase.step(10, () -> {
            ItemStack carried = Minecraft.m_91087_().f_91074_.f_36096_.m_142621_();
            Showcase.check("shelf: the enchanted book taken into the hand", carried.m_150930_(Items.f_42690_), "carried=" + Showcase.describe(carried));
            Showcase.server(p -> Showcase.check("shelf: its place is empty on the server", Showcase.shelfItem(p, 0).m_41619_(), "slot 0=" + Showcase.describe(Showcase.shelfItem(p, 0))));
        });
        Showcase.step(2, () -> Showcase.pointAt(Showcase.shelf().slotPoint(2)));
        Showcase.step(8, () -> Showcase.shot("e8_place_hover"));
        Showcase.step(2, () -> Showcase.clickShelf(ShelfStation.Target.PLACE, 0));
        Showcase.moveShots("e9_put", 0.15, 0.3, 0.45, 0.6, 0.8);
        Showcase.step(14, () -> {
            Showcase.shot("ea_put_done");
            Showcase.server(p -> Showcase.check("shelf: the book stands in the other place", Showcase.shelfItem(p, 2).m_150930_(Items.f_42690_) && p.f_36096_.m_142621_().m_41619_(), "slot 2=" + Showcase.describe(Showcase.shelfItem(p, 2)) + " carried=" + Showcase.describe(p.f_36096_.m_142621_())));
        });
        Showcase.step(2, () -> Showcase.pointAt(Showcase.shelf().slotPoint(1)));
        Showcase.step(10, () -> Showcase.shot("eb_hover_notes"));
        Showcase.step(2, () -> Showcase.clickShelf(ShelfStation.Target.BOOK, 0));
        Showcase.moveShots("ec_open", 0.15, 0.35, 0.55, 0.75, 0.95, 1.15, 1.3, 1.5);
        Showcase.step(10, () -> {
            Showcase.shot("ed_reading");
            Showcase.check("shelf: the notes open in the hands", Showcase.shelf() != null && Showcase.shelf().reading() && Showcase.shelf().spread() == 0, "reading=" + (Showcase.shelf() != null && Showcase.shelf().reading()) + " spread=" + String.valueOf(Showcase.shelf() == null ? "-" : Integer.valueOf(Showcase.shelf().spread())));
            Showcase.server(p -> Showcase.check("shelf: the notes stay on the shelf while read", Showcase.shelfItem(p, 1).m_150930_(Items.f_42614_), "slot 1=" + Showcase.describe(Showcase.shelfItem(p, 1))));
        });
        Showcase.step(2, () -> Showcase.pointAt(Showcase.shelf().pageCenter(false)));
        Showcase.step(8, () -> Showcase.shot("ee_peek"));
        Showcase.step(2, () -> Showcase.screenClick(0));
        Showcase.step(4, () -> Showcase.shot("ee_turn_020"));
        Showcase.step(4, () -> Showcase.shot("ee_turn_040"));
        Showcase.step(16, () -> {
            Showcase.shot("ef_spread1");
            Showcase.check("shelf: a leaf of the notes turned", Showcase.shelf() != null && Showcase.shelf().spread() == 1, "spread=" + String.valueOf(Showcase.shelf() == null ? "-" : Integer.valueOf(Showcase.shelf().spread())));
        });
        Showcase.step(2, () -> Minecraft.m_91087_().f_91080_.m_7933_(256, 0, 0));
        Showcase.moveShots("eg_back", 0.2, 0.45, 0.7, 0.95, 1.15, 1.35);
        Showcase.step(14, () -> {
            Showcase.shot("eh_back_done");
            Showcase.check("shelf: Esc put the notes back first", Showcase.shelf() != null && !Showcase.shelf().reading() && Scene.phase == Scene.Phase.ACTIVE, "phase=" + String.valueOf((Object)Scene.phase));
        });
        Showcase.step(2, () -> Showcase.pointAt(Showcase.shelf().slotPoint(4)));
        Showcase.step(8, () -> {
            ShelfStation station = Showcase.shelf();
            if (station == null || station.target != ShelfStation.Target.BOOK) {
                REPORT.add("cursor missed the plain book");
            }
            if (station != null) {
                station.click(0, true);
            }
        });
        Showcase.moveShots("ei_bag", 0.3, 0.6, 0.9);
        Showcase.step(10, () -> Showcase.server(p -> Showcase.check("shelf: the plain book into the inventory", Showcase.shelfItem(p, 4).m_41619_() && p.m_150109_().m_216874_(stack -> stack.m_150930_(Items.f_42517_)), "slot 4=" + Showcase.describe(Showcase.shelfItem(p, 4)))));
        Showcase.step(4, () -> Showcase.report("shelf before leaving"));
        Showcase.step(2, Scene::leave);
        Showcase.step(40, () -> Showcase.shot("e9_after"));
        Showcase.step(4, restoreLayout);
        Showcase.step(2, () -> {
            act = "ej";
            Showcase.hold(7, new ItemStack((ItemLike)Items.f_42517_));
        });
        Showcase.step(10, () -> {
            introShot = 0;
            Showcase.useAt(SHELF, SHELF_SLOTS[4], Direction.SOUTH);
        });
        Showcase.step(100, () -> Showcase.server(p -> Showcase.check("shelf: the book in hand went into the place used", Showcase.shelfItem(p, 4).m_150930_(Items.f_42517_) && p.m_150109_().m_8020_(7).m_41619_(), "slot 4=" + Showcase.describe(Showcase.shelfItem(p, 4)) + " hand=" + Showcase.describe(p.m_150109_().m_8020_(7)))));
        Showcase.step(2, () -> Showcase.shot("ej_put_from_hand"));
        Showcase.step(2, Scene::leave);
        Showcase.step(40, () -> {
            act = "ek";
            Settings.set((ForgeConfigSpec.ConfigValue)Settings.SCENES.get((Object)StationKind.CHISELED_BOOKSHELF), false);
        });
        Showcase.step(4, () -> Showcase.useAt(SHELF, SHELF_SLOTS[3], Direction.SOUTH));
        Showcase.step(20, () -> {
            Showcase.check("shelf, scene off: no scene", Scene.phase == Scene.Phase.NONE, "phase=" + String.valueOf((Object)Scene.phase));
            Showcase.server(p -> Showcase.check("shelf, scene off: the written book taken out the vanilla way", Showcase.shelfItem(p, 3).m_41619_() && p.m_150109_().m_216874_(stack -> stack.m_150930_(Items.f_42615_)), "slot 3=" + Showcase.describe(Showcase.shelfItem(p, 3))));
            Settings.set((ForgeConfigSpec.ConfigValue)Settings.SCENES.get((Object)StationKind.CHISELED_BOOKSHELF), true);
        });
        Showcase.beaconAct();
    }

    private static BeaconStation beacon() {
        return Scene.station(BeaconStation.class);
    }

    private static void clickBeacon(BeaconStation.Target expected, int button) {
        BeaconStation station = Showcase.beacon();
        if (station == null || station.target != expected) {
            REPORT.add("cursor missed beacon target " + String.valueOf((Object)expected) + ": was " + String.valueOf(station == null ? "-" : station.target));
        }
        Showcase.screenClick(button);
    }

    private static void beaconAct() {
        Showcase.nextAct("n", new Vec3((double)BEACON.m_123341_() + 0.5, (double)BEACON.m_123342_(), (double)BEACON.m_123343_() + 1.75), 180.0f, BEACON, new Vec3(0.5, 0.9, 0.5), Direction.UP);
    }

    private static void beaconScript() {
        Runnable restoreLayout = Showcase.spatialEditorScript();
        Showcase.shot("n5_active");
        Showcase.step(2, () -> Showcase.check("beacon stands on a full pyramid", Showcase.beacon() != null && Showcase.beacon().menu() != null && ((BeaconMenu)Showcase.beacon().menu()).m_39056_() == 4, "levels=" + String.valueOf(Showcase.beacon() == null || Showcase.beacon().menu() == null ? "-" : Integer.valueOf(((BeaconMenu)Showcase.beacon().menu()).m_39056_()))));
        Showcase.step(2, () -> Showcase.pointAt(Showcase.beacon().powerPos(0)));
        Showcase.step(10, () -> Showcase.shot("n6_hover_speed"));
        Showcase.step(2, () -> Showcase.clickBeacon(BeaconStation.Target.POWER, 0));
        Showcase.step(6, () -> Showcase.pointAt(Showcase.beacon().powerPos(5)));
        Showcase.step(4, () -> Showcase.clickBeacon(BeaconStation.Target.POWER, 0));
        Showcase.step(8, () -> Showcase.shot("n6_chosen"));
        Showcase.step(2, () -> Showcase.invClickItem(Items.f_42417_));
        Showcase.step(3, () -> Showcase.pointAt(Showcase.beacon().paymentPos()));
        Showcase.step(10, () -> Showcase.shot("n7_carry_gold"));
        Showcase.step(2, () -> Showcase.clickBeacon(BeaconStation.Target.TOP, 0));
        Showcase.step(4, Showcase::stash);
        Showcase.step(4, () -> Showcase.pointAt(Showcase.beacon().paymentPos()));
        Showcase.step(10, () -> Showcase.shot("n7_payment_tooltip"));
        Showcase.step(2, () -> Showcase.clickBeacon(BeaconStation.Target.PAYMENT, 0));
        Showcase.moveShots("n8_light", 0.2, 0.45, 0.62, 0.72, 0.85, 1.05, 1.4, 1.9, 2.6);
        Showcase.step(10, () -> Showcase.server(p -> {
            CompoundTag tag = p.m_284548_().m_7702_(BEACON).m_187482_();
            MobEffect first = MobEffect.m_19453_((int)tag.m_128451_("Primary"));
            MobEffect second = MobEffect.m_19453_((int)tag.m_128451_("Secondary"));
            String primary = first == null ? "-" : String.valueOf(BuiltInRegistries.f_256974_.m_7981_((Object)first));
            String secondary = second == null ? "-" : String.valueOf(BuiltInRegistries.f_256974_.m_7981_((Object)second));
            Showcase.check("beacon lit with speed and regeneration", primary.equals("minecraft:speed") && secondary.equals("minecraft:regeneration"), "primary=" + primary + " secondary=" + secondary);
        }));
        Showcase.step(2, () -> Showcase.check("payment used up", Showcase.beacon() != null && Showcase.beacon().slot(0).m_41619_(), "slot=" + String.valueOf(Showcase.beacon() == null ? "-" : Showcase.beacon().slot(0))));
        Showcase.step(4, () -> Showcase.report("beacon before leaving"));
        Showcase.step(2, restoreLayout);
        Showcase.step(2, Scene::leave);
        Showcase.step(50, () -> Showcase.shot("n9_after"));
        Showcase.heightAct();
    }

    private static int farSpot() {
        CampfireStation station = Showcase.campfire();
        Vec3 eye = Minecraft.m_91087_().f_91074_.m_20182_();
        int far = 0;
        for (int j = 1; j < 4; ++j) {
            if (!(station.spot(j).m_82557_(eye) > station.spot(far).m_82557_(eye))) continue;
            far = j;
        }
        return far;
    }

    private static BlockPos heightSpot(int i) {
        return new BlockPos(-10 - 4 * i, -60, 22);
    }

    private static void buildHeights(ServerLevel level) {
        int dz;
        int dx;
        int dz2;
        int dx2;
        BlockState stone = Blocks.f_50222_.m_49966_();
        BlockState table = Blocks.f_50091_.m_49966_();
        BlockState south = (BlockState)Blocks.f_50094_.m_49966_().m_61124_((Property)AbstractFurnaceBlock.f_48683_, (Comparable)Direction.SOUTH);
        BlockPos b = Showcase.heightSpot(0);
        level.m_46597_(b, stone);
        level.m_46597_(b.m_7494_(), table);
        b = Showcase.heightSpot(1);
        level.m_46597_(b, stone);
        level.m_46597_(b.m_7494_(), south);
        b = Showcase.heightSpot(2);
        level.m_46597_(b, stone);
        level.m_46597_(b.m_7494_(), stone);
        level.m_46597_(b.m_6630_(2), south);
        b = Showcase.heightSpot(3);
        level.m_46597_(b, table);
        for (dx2 = -1; dx2 <= 1; ++dx2) {
            for (dz2 = 1; dz2 <= 3; ++dz2) {
                level.m_46597_(b.m_7918_(dx2, 0, dz2), Blocks.f_50741_.m_49966_());
            }
        }
        b = Showcase.heightSpot(4);
        level.m_46597_(b.m_7495_(), table);
        b = Showcase.heightSpot(5);
        level.m_46597_(b.m_7495_(), Blocks.f_50016_.m_49966_());
        level.m_46597_(b.m_6625_(2), table);
        b = Showcase.heightSpot(6);
        level.m_46597_(b.m_7495_(), south);
        level.m_46597_(b.m_7495_().m_122019_(), Blocks.f_50016_.m_49966_());
        b = Showcase.heightSpot(7);
        level.m_46597_(b.m_7495_(), (BlockState)Blocks.f_50094_.m_49966_().m_61124_((Property)AbstractFurnaceBlock.f_48683_, (Comparable)Direction.NORTH));
        level.m_46597_(b.m_7495_().m_122012_(), Blocks.f_50016_.m_49966_());
        b = Showcase.heightSpot(8);
        level.m_46597_(b, table);
        level.m_46597_(b.m_7494_(), Blocks.f_50404_.m_49966_());
        b = Showcase.heightSpot(9);
        level.m_46597_(b, table);
        b = Showcase.heightSpot(10);
        level.m_46597_(b, south);
        for (int dz3 = 1; dz3 <= 2; ++dz3) {
            level.m_46597_(b.m_122020_(dz3).m_7495_(), Blocks.f_49991_.m_49966_());
        }
        level.m_46597_(Showcase.heightSpot(11), table);
        b = Showcase.heightSpot(12);
        level.m_46597_(b, south);
        for (dx2 = -1; dx2 <= 1; ++dx2) {
            for (dz2 = 1; dz2 <= 3; ++dz2) {
                level.m_46597_(b.m_7918_(dx2, 0, dz2), Blocks.f_50741_.m_49966_());
            }
        }
        b = Showcase.heightSpot(13);
        level.m_46597_(b.m_7495_(), (BlockState)Blocks.f_50094_.m_49966_().m_61124_((Property)AbstractFurnaceBlock.f_48683_, (Comparable)Direction.EAST));
        level.m_46597_(b.m_7495_().m_122029_(), Blocks.f_50016_.m_49966_());
        b = Showcase.heightSpot(14);
        level.m_46597_(b, Blocks.f_50255_.m_49966_());
        for (dx2 = -1; dx2 <= 1; ++dx2) {
            for (dz2 = 1; dz2 <= 3; ++dz2) {
                level.m_46597_(b.m_7918_(dx2, 0, dz2), Blocks.f_50741_.m_49966_());
            }
        }
        Block[] furnaces = new Block[]{Blocks.f_50094_, Blocks.f_50619_, Blocks.f_50620_};
        for (int k = 0; k < 3; ++k) {
            b = Showcase.heightSpot(16 + k);
            level.m_46597_(b.m_7495_(), (BlockState)furnaces[k].m_49966_().m_61124_((Property)AbstractFurnaceBlock.f_48683_, (Comparable)Direction.SOUTH));
            level.m_46597_(b.m_7495_().m_122019_(), Blocks.f_50016_.m_49966_());
        }
        b = Showcase.heightSpot(19);
        level.m_46597_(b, south);
        for (dx = -1; dx <= 1; ++dx) {
            for (dz = 2; dz <= 4; ++dz) {
                level.m_46597_(b.m_7918_(dx, 0, dz), Blocks.f_50741_.m_49966_());
            }
        }
        b = Showcase.heightSpot(20);
        level.m_46597_(b, south);
        for (dx = -1; dx <= 1; ++dx) {
            level.m_46597_(b.m_7918_(dx, 0, 2), stone);
            level.m_46597_(b.m_7918_(dx, 1, 2), stone);
        }
        level.m_46597_(Showcase.heightSpot(21).m_7495_(), south);
        b = Showcase.heightSpot(22);
        level.m_46597_(b, (BlockState)Blocks.f_50094_.m_49966_().m_61124_((Property)AbstractFurnaceBlock.f_48683_, (Comparable)Direction.NORTH));
        level.m_46597_(b.m_122012_(), stone);
        level.m_46597_(b.m_122012_().m_7494_(), stone);
        b = Showcase.heightSpot(23);
        level.m_46597_(b, south);
        for (dx = -2; dx <= 2; ++dx) {
            for (dz = 1; dz <= 3; ++dz) {
                level.m_46597_(b.m_7918_(dx, -1, dz), Blocks.f_49991_.m_49966_());
            }
        }
        b = Showcase.heightSpot(15);
        level.m_46597_(b, (BlockState)Blocks.f_50624_.m_49966_().m_61124_((Property)LecternBlock.f_54465_, (Comparable)Direction.SOUTH));
        LecternBlock.m_269125_(null, (Level)level, (BlockPos)b, (BlockState)level.m_8055_(b), (ItemStack)Showcase.storyBook());
        for (dz2 = 1; dz2 <= 3; ++dz2) {
            level.m_46597_(b.m_122020_(dz2), Blocks.f_50350_.m_49966_());
        }
    }

    private static void standLooking(Vec3 feet, Vec3 point) {
        Showcase.server(p -> {
            Vec3 eye = feet.m_82520_(0.0, (double)p.m_20192_(), 0.0);
            Vec3 d = point.m_82546_(eye);
            float yaw = (float)Math.toDegrees(Math.atan2(-d.f_82479_, d.f_82481_));
            float pitch = (float)(-Math.toDegrees(Math.atan2(d.f_82480_, Math.hypot(d.f_82479_, d.f_82481_))));
            p.m_8999_(p.m_284548_(), feet.f_82479_, feet.f_82480_, feet.f_82481_, yaw, pitch);
        });
    }

    private static void closeWindow() {
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91074_ != null && mc.f_91080_ != null && !(mc.f_91080_ instanceof SceneScreen)) {
            mc.f_91074_.m_6915_();
        }
    }

    private static double cameraOverEyes() {
        Minecraft mc = Minecraft.m_91087_();
        return CameraRig.last() == null || mc.f_91074_ == null ? 99.0 : CameraRig.last().pos().f_82480_ - mc.f_91074_.m_20188_();
    }

    private static void heightCase(int i, String name, BlockPos station, Vec3 feet, Vec3 local, Direction face, boolean scene, @Nullable Runnable during) {
        Showcase.heightCase(i, name, station, feet, local, face, scene, during, true);
    }

    private static void heightCase(int i, String name, BlockPos station, Vec3 feet, Vec3 local, Direction face, boolean scene, @Nullable Runnable during, boolean leave) {
        BlockPos base = Showcase.heightSpot(i);
        Vec3 at = Vec3.m_82528_((Vec3i)base).m_82549_(feet);
        Showcase.step(2, () -> {
            act = name;
            Showcase.closeWindow();
            Showcase.standLooking(at, Vec3.m_82512_((Vec3i)station));
        });
        Showcase.step(18, () -> Showcase.shot(name + "_0_before"));
        Showcase.step(2, () -> {
            introShot = 0;
            outroShot = 0;
            Scene.refused = "";
            Minecraft mc = Minecraft.m_91087_();
            mc.f_91072_.m_233732_(mc.f_91074_, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.m_82528_((Vec3i)station).m_82549_(local), face, station, false));
        });
        if (scene) {
            Showcase.step(90, () -> {
                Showcase.check(name + ": a scene", Scene.phase == Scene.Phase.ACTIVE, "phase=" + String.valueOf((Object)Scene.phase) + " refused=" + Scene.refused + String.format(" camera over eyes=%.2f crouching=%s", Showcase.cameraOverEyes(), Minecraft.m_91087_().f_91074_.m_6047_()));
                Showcase.shot(name + "_5_scene");
            });
            if (during != null) {
                Showcase.step(2, during);
            }
            if (leave) {
                Showcase.step(2, () -> {
                    if (Scene.phase == Scene.Phase.ACTIVE) {
                        Scene.leave();
                    }
                });
            }
            Showcase.step(36, () -> {});
        } else {
            Showcase.step(1, () -> Showcase.check(name + ": the hand reaches out", Minecraft.m_91087_().f_91074_.f_20911_, "swing=" + Minecraft.m_91087_().f_91074_.f_20913_));
            Showcase.step(15, () -> {
                Minecraft mc = Minecraft.m_91087_();
                Showcase.check(name + ": its own window", mc.f_91080_ instanceof AbstractContainerScreen && !Scene.active(), "screen=" + (mc.f_91080_ == null ? "-" : mc.f_91080_.getClass().getSimpleName()) + " phase=" + String.valueOf((Object)Scene.phase) + " why=" + Scene.refused);
                Showcase.shot(name + "_5_window");
            });
            Showcase.step(4, Showcase::closeWindow);
        }
    }

    private static void heightAct() {
        if (EDITOR_BATCH) {
            Showcase.developerOptionsScript();
            Showcase.jukeboxAct();
            return;
        }
        if (Showcase.stopHere("h")) {
            return;
        }
        BlockPos b = Showcase.heightSpot(0);
        Showcase.heightCase(0, "h0_table_on_pillar", b.m_7494_(), new Vec3(0.5, 0.0, 2.2), new Vec3(0.5, 0.5, 1.0), Direction.SOUTH, false, null);
        b = Showcase.heightSpot(1);
        Showcase.heightCase(1, "h1_furnace_on_block", b.m_7494_(), new Vec3(0.5, 0.0, 2.4), new Vec3(0.5, 0.5, 1.0), Direction.SOUTH, true, () -> Showcase.check("furnace on a block: the camera stays with the head, the player stands up", Showcase.cameraOverEyes() <= 0.17 && !Minecraft.m_91087_().f_91074_.m_6047_(), String.format("over eyes=%.2f", Showcase.cameraOverEyes())));
        b = Showcase.heightSpot(2);
        Showcase.heightCase(2, "h2_furnace_two_up", b.m_6630_(2), new Vec3(0.5, 0.0, 2.0), new Vec3(0.5, 0.5, 1.0), Direction.SOUTH, false, null);
        b = Showcase.heightSpot(3);
        Showcase.heightCase(3, "h3_table_below_floor", b, new Vec3(0.5, 1.0, 2.4), new Vec3(0.5, 1.0, 0.7), Direction.UP, true, () -> Showcase.check("table below the floor: the player crouches to it", Minecraft.m_91087_().f_91074_.m_6047_() && Showcase.cameraOverEyes() >= -0.97, String.format("over eyes=%.2f", Showcase.cameraOverEyes())));
        b = Showcase.heightSpot(4);
        Showcase.heightCase(4, "h4_table_flush", b.m_7495_(), new Vec3(0.5, 0.0, 2.2), new Vec3(0.5, 1.0, 0.7), Direction.UP, true, () -> Showcase.check("table flush with the ground: the player bends down to it", Minecraft.m_91087_().f_91074_.m_6047_(), ""));
        b = Showcase.heightSpot(5);
        Showcase.heightCase(5, "h5_table_in_a_hole", b.m_6625_(2), new Vec3(0.5, 0.0, 1.9), new Vec3(0.5, 1.0, 0.5), Direction.UP, false, null);
        b = Showcase.heightSpot(6);
        Showcase.heightCase(6, "h6_furnace_sunk_facing", b.m_7495_(), new Vec3(0.5, 0.0, 2.35), new Vec3(0.5, 0.5, 1.0), Direction.SOUTH, true, () -> Showcase.check("sunk furnace facing the player: bending down to it", Scene.station instanceof FurnaceStation && Minecraft.m_91087_().f_91074_.m_6047_(), String.format("over eyes=%.2f", Showcase.cameraOverEyes())));
        b = Showcase.heightSpot(7);
        Showcase.heightCase(7, "h7_furnace_sunk_away", b.m_7495_(), new Vec3(0.5, 0.0, 1.4), new Vec3(0.5, 1.0, 0.6), Direction.UP, true, () -> Showcase.check("sunk furnace facing away: the player walks round to its front", Scene.station instanceof FurnaceStation && Minecraft.m_91087_().f_91074_.m_20189_() < (double)Showcase.heightSpot(7).m_123343_() - 0.4, String.format("z=%.2f", Minecraft.m_91087_().f_91074_.m_20189_())));
        b = Showcase.heightSpot(8);
        Showcase.heightCase(8, "h8_table_covered", b, new Vec3(0.5, 0.0, 2.2), new Vec3(0.5, 0.5, 1.0), Direction.SOUTH, false, null);
        b = Showcase.heightSpot(9);
        Showcase.heightCase(9, "h9_standing_on_table", b, new Vec3(0.5, 1.0, 0.5), new Vec3(0.5, 1.0, 0.5), Direction.UP, false, null);
        b = Showcase.heightSpot(10);
        Showcase.heightCase(10, "ha_lava_in_front", b, new Vec3(0.5, 0.0, 3.4), new Vec3(0.5, 0.5, 1.0), Direction.SOUTH, true, () -> Showcase.check("lava before the furnace: the player goes round it and works from beside, crouching", Scene.station instanceof FurnaceStation && Math.abs(Scene.station.swing) > 1.0 && Minecraft.m_91087_().f_91074_.m_6047_() && Math.abs(Minecraft.m_91087_().f_91074_.m_20185_() - ((double)Showcase.heightSpot(10).m_123341_() + 0.5)) > 0.75, String.format("swing=%.0f x=%.2f z=%.2f", Scene.station == null ? 0.0 : Scene.station.swing, Minecraft.m_91087_().f_91074_.m_20185_() - (double)Showcase.heightSpot(10).m_123341_(), Minecraft.m_91087_().f_91074_.m_20189_() - (double)Showcase.heightSpot(10).m_123343_())));
        Showcase.heightCase(23, "hp_lava_all_round", Showcase.heightSpot(23), new Vec3(0.5, 0.0, 4.6), new Vec3(0.5, 0.5, 1.0), Direction.SOUTH, false, null);
        String[] names = new String[]{"hg_furnace_sunk_far", "hh_smoker_sunk_far", "hi_blast_furnace_sunk_far"};
        for (int k = 0; k < 3; ++k) {
            int spot = 16 + k;
            Showcase.heightCase(spot, names[k], Showcase.heightSpot(spot).m_7495_(), new Vec3(0.5, 0.0, 4.6), new Vec3(0.5, 1.0, 0.6), Direction.UP, true, () -> {
                Showcase.check(names[spot - 16] + ": walked to the edge of the pit and bends down", Scene.station instanceof FurnaceStation && Minecraft.m_91087_().f_91074_.m_6047_() && Minecraft.m_91087_().f_91074_.m_20189_() < (double)Showcase.heightSpot(spot).m_123343_() + 2.3, String.format("z=%.2f", Minecraft.m_91087_().f_91074_.m_20189_() - (double)Showcase.heightSpot(spot).m_123343_()));
                Vec3 eye = CameraRig.last() == null ? Vec3.f_82478_ : CameraRig.last().pos();
                double arm = Math.max(Hands.position(true).m_82554_(eye), Hands.position(false).m_82554_(eye));
                Showcase.check(names[spot - 16] + ": the resting hands are not stretched down into the pit", arm < 1.15, String.format("hands from the camera=%.2f", arm));
            });
        }
        Showcase.heightCase(19, "hj_furnace_below_floor_far", Showcase.heightSpot(19), new Vec3(0.5, 1.0, 4.6), new Vec3(0.5, 0.5, 1.0), Direction.SOUTH, true, () -> Showcase.check("furnace below the floor's edge: walked to the edge and bends down", Scene.station instanceof FurnaceStation && Minecraft.m_91087_().f_91074_.m_6047_(), String.format("z=%.2f", Minecraft.m_91087_().f_91074_.m_20189_() - (double)Showcase.heightSpot(19).m_123343_())));
        Showcase.heightCase(20, "hk_furnace_behind_wall", Showcase.heightSpot(20), new Vec3(0.5, 0.0, 4.5), new Vec3(0.5, 0.5, 1.0), Direction.SOUTH, true, () -> Showcase.check("furnace behind a wall: the player went round it", Scene.station instanceof FurnaceStation && Math.abs(Minecraft.m_91087_().f_91074_.m_20189_() - (double)Showcase.heightSpot(20).m_123343_() - 1.42) < 0.2, String.format("z=%.2f", Minecraft.m_91087_().f_91074_.m_20189_() - (double)Showcase.heightSpot(20).m_123343_())));
        Showcase.heightCase(21, "hm_furnace_buried", Showcase.heightSpot(21).m_7495_(), new Vec3(0.5, 0.0, 3.0), new Vec3(0.5, 1.0, 0.5), Direction.UP, false, null);
        Showcase.heightCase(22, "hn_furnace_facing_wall", Showcase.heightSpot(22), new Vec3(0.5, 0.0, 1.6), new Vec3(0.5, 1.0, 0.5), Direction.UP, false, null);
        b = Showcase.heightSpot(12);
        Showcase.heightCase(12, "hc_furnace_front_against_floor", b, new Vec3(0.5, 1.0, 2.2), new Vec3(0.5, 1.0, 0.6), Direction.UP, false, null);
        b = Showcase.heightSpot(13);
        Showcase.heightCase(13, "hd_furnace_sunk_sideways", b.m_7495_(), new Vec3(0.9, 0.0, 1.6), new Vec3(0.5, 1.0, 0.5), Direction.UP, true, () -> Showcase.check("sunk furnace facing sideways: the player walks round to its front", Scene.station instanceof FurnaceStation && Minecraft.m_91087_().f_91074_.m_20185_() > (double)Showcase.heightSpot(13).m_123341_() + 1.4, String.format("x=%.2f", Minecraft.m_91087_().f_91074_.m_20185_() - (double)Showcase.heightSpot(13).m_123341_())));
        b = Showcase.heightSpot(14);
        Showcase.heightCase(14, "he_brewing_below_floor", b, new Vec3(0.5, 1.0, 2.3), new Vec3(0.5, 0.5, 0.5), Direction.SOUTH, true, () -> Showcase.check("brewing stand below the floor: bending down to it", Scene.station instanceof BrewingStation && Minecraft.m_91087_().f_91074_.m_6047_() && Showcase.cameraOverEyes() >= -0.97, String.format("over eyes=%.2f", Showcase.cameraOverEyes())));
        b = Showcase.heightSpot(15);
        Showcase.heightCase(15, "hf_lectern_carpet_in_front", b, new Vec3(0.5, 0.0625, 2.6), new Vec3(0.5, 0.9, 0.6), Direction.UP, true, () -> Showcase.check("lectern with a carpet in front: reading it", Scene.station instanceof LecternStation, "station=" + String.valueOf(Scene.station)));
        b = Showcase.heightSpot(11);
        Showcase.heightCase(11, "hb_ground_gives_way", b, new Vec3(0.5, 0.0, 2.2), new Vec3(0.5, 1.0, 0.7), Direction.UP, true, () -> Showcase.server(p -> {
            BlockPos under = p.m_20183_().m_7495_();
            p.m_284548_().m_46597_(under, Blocks.f_50016_.m_49966_());
            p.m_284548_().m_46597_(under.m_7495_(), Blocks.f_50016_.m_49966_());
        }), false);
        Showcase.step(2, () -> {
            Showcase.check("falling lets the player go", Scene.phase != Scene.Phase.ACTIVE, "phase=" + String.valueOf((Object)Scene.phase));
            Showcase.shot("hb_ground_gives_way_6_fallen");
        });
        Showcase.optionsAct();
    }

    private static void optionsAct() {
        if (Showcase.stopHere("o")) {
            return;
        }
        Showcase.step(30, () -> {
            act = "o";
            Showcase.closeWindow();
            Minecraft mc = Minecraft.m_91087_();
            ModContainer mod = (ModContainer)ModList.get().getModContainerById("cinematic").orElseThrow();
            Screen screen = (Screen)((ConfigScreenHandler.ConfigScreenFactory)mod.getCustomExtension(ConfigScreenHandler.ConfigScreenFactory.class).orElseThrow()).screenFunction().apply(mc, null);
            mc.m_91152_(screen);
            Showcase.check("the settings screen opens", mc.f_91080_ instanceof SettingsScreen, mc.f_91080_ == null ? "-" : mc.f_91080_.getClass().getName());
            Showcase.pointGui((double)mc.m_91268_().m_85445_() * 0.5, mc.m_91268_().m_85446_() - 4);
        });
        Showcase.step(20, () -> Showcase.shot("o1_settings_general"));
        Showcase.step(2, () -> Showcase.pointGui((double)Minecraft.m_91087_().m_91268_().m_85445_() * 0.4, 78.0));
        Showcase.step(16, () -> Showcase.shot("o2_settings_description"));
        Showcase.step(2, () -> Showcase.clickWidget(Component.m_237115_((String)"cinematic.settings.tab.stations").getString()));
        Showcase.step(16, () -> Showcase.shot("o3_settings_stations"));
        Showcase.step(2, () -> Showcase.clickWidget(Component.m_237115_((String)"cinematic.settings.tab.camera").getString()));
        Showcase.step(16, () -> Showcase.shot("o4_settings_camera"));
        Showcase.step(2, () -> Showcase.check("advanced tab opens", Showcase.clickSettingsSection((GuiEventListener)Minecraft.m_91087_().f_91080_, Component.m_237115_((String)"cinematic.settings.tab.advanced").getString()), "client-only switch visible in-game"));
        Showcase.step(16, () -> Showcase.shot("o5_settings_advanced"));
        Showcase.step(2, () -> {
            Screen patt451549$temp = Minecraft.m_91087_().f_91080_;
            if (patt451549$temp instanceof SettingsScreen) {
                SettingsScreen settings = (SettingsScreen)patt451549$temp;
                settings.showDeveloper();
            }
        });
        Showcase.step(16, () -> {
            Showcase.verifyProfileControls();
            Showcase.shot("o6_settings_profiles");
        });
        Showcase.step(2, () -> Minecraft.m_91087_().m_91152_(null));
        Showcase.step(10, () -> Settings.set(Settings.HEIGHT_LOGIC, false));
        Showcase.noticeCase(0, "o6_too_high", Showcase.heightSpot(0).m_7494_(), new Vec3(0.5, 0.0, 2.2), new Vec3(0.5, 0.5, 1.0), Direction.SOUTH, "too_high");
        Showcase.noticeCase(5, "o6_too_low", Showcase.heightSpot(5).m_6625_(2), new Vec3(0.5, 0.0, 1.9), new Vec3(0.5, 1.0, 0.5), Direction.UP, "too_low");
        Showcase.noticeCase(22, "o6_facing_wall", Showcase.heightSpot(22), new Vec3(0.5, 0.0, 1.6), new Vec3(0.5, 1.0, 0.5), Direction.UP, "no_way");
        Showcase.noticeCase(8, "o6_covered", Showcase.heightSpot(8), new Vec3(0.5, 0.0, 2.2), new Vec3(0.5, 0.5, 1.0), Direction.SOUTH, "covered");
        Showcase.noticeCase(9, "o6_on_top", Showcase.heightSpot(9), new Vec3(0.5, 1.0, 0.5), new Vec3(0.5, 1.0, 0.5), Direction.UP, "on_top");
        Showcase.noticeCase(23, "o6_no_way", Showcase.heightSpot(23), new Vec3(0.5, 0.0, 4.6), new Vec3(0.5, 0.5, 1.0), Direction.SOUTH, "no_way");
        Showcase.step(2, () -> Settings.set(Settings.HEIGHT_LOGIC, true));
        Vec3 atTable = Vec3.m_82528_((Vec3i)Showcase.heightSpot(4)).m_82520_(0.5, 0.0, 1.36);
        BlockPos flush = Showcase.heightSpot(4).m_7495_();
        Showcase.step(4, () -> {
            Settings.set(Settings.APPROACH_SPEED, 200);
            Settings.set(Settings.QUICK_RETURN, false);
        });
        Showcase.paceCase("o9_pace_fast", atTable, flush, true);
        Showcase.step(4, () -> Settings.set(Settings.APPROACH_SPEED, 100));
        Showcase.paceCase("o9_pace_usual", atTable, flush, false);
        Showcase.step(4, () -> {
            Settings.set(Settings.LEAN_STRENGTH, 0);
            Settings.set(Settings.QUICK_RETURN, true);
        });
        Showcase.step(2, () -> {
            act = "o8_lean_0";
            Showcase.standLooking(atTable, Vec3.m_82512_((Vec3i)flush));
        });
        Showcase.step(10, () -> Showcase.useAt(flush, new Vec3(0.5, 1.0, 0.7), Direction.UP));
        Showcase.step(60, () -> {
            Minecraft mc = Minecraft.m_91087_();
            Vec3 eye = mc.f_91074_.m_146892_();
            double off = CameraRig.last() == null ? 99.0 : CameraRig.last().pos().m_82554_(eye);
            Showcase.check("camera lean 0%: the camera stays at the eyes", Scene.phase == Scene.Phase.ACTIVE && off < 0.08, String.format("off the eyes=%.3f", off));
            Showcase.shot("o8_lean_0");
        });
        Showcase.step(2, Scene::leave);
        Showcase.step(36, () -> Settings.set(Settings.LEAN_STRENGTH, 100));
        Showcase.step(2, () -> {
            act = "o8_lean_100";
            Showcase.standLooking(atTable, Vec3.m_82512_((Vec3i)flush));
        });
        Showcase.step(10, () -> Showcase.useAt(flush, new Vec3(0.5, 1.0, 0.7), Direction.UP));
        Showcase.step(60, () -> Showcase.shot("o8_lean_100"));
        Showcase.step(2, Scene::leave);
        Showcase.step(36, () -> {});
        Showcase.step(2, () -> Settings.set((ForgeConfigSpec.ConfigValue)Settings.SCENES.get((Object)StationKind.TABLE), false));
        Showcase.heightCase(4, "o7_table_scene_off", Showcase.heightSpot(4).m_7495_(), new Vec3(0.5, 0.0, 2.2), new Vec3(0.5, 1.0, 0.7), Direction.UP, false, null);
        Showcase.step(2, () -> Settings.set((ForgeConfigSpec.ConfigValue)Settings.SCENES.get((Object)StationKind.TABLE), true));
        Showcase.transitionAct();
    }

    private static void transitionAct() {
        if (Showcase.stopHere("t")) {
            return;
        }
        Showcase.step(10, () -> {
            act = "t";
            Showcase.closeWindow();
            Showcase.standLooking(new Vec3(0.5, -60.0, 3.36), Vec3.m_82512_((Vec3i)TABLE));
        });
        Showcase.step(10, () -> Showcase.useAt(TABLE, new Vec3(0.5, 1.0, 0.8), Direction.UP));
        Showcase.step(70, () -> Showcase.check("moving over: working at the table", Scene.station instanceof TableStation && Scene.phase == Scene.Phase.ACTIVE, "phase=" + String.valueOf((Object)Scene.phase)));
        Showcase.step(2, () -> Showcase.pointGui((double)Minecraft.m_91087_().m_91268_().m_85445_() * 0.5, (double)Minecraft.m_91087_().m_91268_().m_85446_() * 0.6));
        Showcase.step(4, () -> Showcase.invClickItem(Items.f_42647_));
        Showcase.step(4, () -> Showcase.pointAt(new Vec3((double)FURNACE.m_123341_() + 0.6, (double)FURNACE.m_123342_() + 1.0, (double)FURNACE.m_123343_() + 0.75)));
        Showcase.step(8, () -> {
            Showcase.check("moving over: the furnace beside is offered", FURNACE.equals((Object)Scene.neighbor), "neighbor=" + String.valueOf(Scene.neighbor) + " cursor=" + Arrays.toString(Picking.mouseGui()));
            Showcase.shot("t0_offer_furnace");
        });
        Showcase.step(2, () -> {
            act = "t2";
            introShot = 0;
            Showcase.screenClick(1);
        });
        Showcase.step(2, () -> Showcase.check("moving over: the item in hand is put away", Hands.carried().m_41619_(), "carried=" + Showcase.describe(Hands.carried())));
        Showcase.step(60, () -> {
            Showcase.check("moving over: working at the furnace, no window", Scene.station instanceof FurnaceStation && Scene.phase == Scene.Phase.ACTIVE && Minecraft.m_91087_().f_91080_ instanceof SceneScreen, "phase=" + String.valueOf((Object)Scene.phase) + " station=" + String.valueOf(Scene.station));
            Showcase.shot("t5_at_furnace");
            Showcase.server(p -> {
                Sessions.Session s = Sessions.get(p);
                boolean planks = false;
                for (ItemStack stack : p.m_150109_().f_35974_) {
                    planks |= stack.m_150930_(Items.f_42647_);
                }
                Showcase.check("moving over: the server moved the session, the planks are back", s != null && s.kind() == StationKind.FURNACE && planks && p.f_36096_.m_142621_().m_41619_(), "session=" + String.valueOf(s == null ? "-" : s.kind()) + " planks=" + planks);
            });
        });
        Showcase.step(2, () -> Showcase.pointAt(new Vec3((double)TABLE.m_123341_() + 0.3, (double)TABLE.m_123342_() + 0.9, (double)TABLE.m_123343_() + 1.0)));
        Showcase.step(8, () -> {
            Showcase.check("moving over: the table beside is offered", TABLE.equals((Object)Scene.neighbor), "neighbor=" + String.valueOf(Scene.neighbor) + " cursor=" + Arrays.toString(Picking.mouseGui()));
            Showcase.shot("t6_offer_table");
        });
        Showcase.step(2, () -> {
            act = "t7";
            introShot = 0;
            Showcase.screenClick(1);
        });
        Showcase.step(60, () -> {
            Showcase.check("moving over: back at the table", Scene.station instanceof TableStation && Scene.phase == Scene.Phase.ACTIVE, "phase=" + String.valueOf((Object)Scene.phase));
            Showcase.shot("t9_back_at_table");
        });
        Showcase.step(2, Scene::leave);
        Showcase.step(40, () -> {
            act = "u_t";
            Showcase.standLooking(new Vec3((double)LOOM.m_123341_() + 0.5, -60.0, (double)LOOM.m_123343_() + 1.86), Vec3.m_82512_((Vec3i)LOOM));
        });
        Showcase.step(10, () -> Showcase.useAt(LOOM, new Vec3(0.5, 1.0, 0.7), Direction.UP));
        Showcase.step(70, () -> Showcase.check("moving over: working at the loom", Scene.station instanceof LoomStation && Scene.phase == Scene.Phase.ACTIVE, "phase=" + String.valueOf((Object)Scene.phase)));
        Showcase.step(2, () -> Showcase.pointAt(new Vec3((double)CARTOGRAPHY.m_123341_() + 0.3, (double)CARTOGRAPHY.m_123342_() + 1.0, (double)CARTOGRAPHY.m_123343_() + 0.6)));
        Showcase.step(8, () -> {
            Showcase.check("moving over: the cartography table is offered", CARTOGRAPHY.equals((Object)Scene.neighbor), "neighbor=" + String.valueOf(Scene.neighbor) + " cursor=" + Arrays.toString(Picking.mouseGui()));
            Showcase.shot("t10_offer_cartography");
        });
        Showcase.step(2, () -> {
            act = "t11";
            introShot = 0;
            Showcase.screenClick(1);
        });
        Showcase.step(90, () -> {
            Showcase.check("moving over: walked to the cartography table", Scene.station instanceof CartographyStation && Scene.phase == Scene.Phase.ACTIVE && Minecraft.m_91087_().f_91074_.m_20185_() > (double)LOOM.m_123341_() + 1.2, String.format("phase=%s x=%.2f side=%s", new Object[]{Scene.phase, Minecraft.m_91087_().f_91074_.m_20185_(), Scene.station == null ? "-" : Scene.station.sideDirection()}));
            Showcase.shot("t15_at_cartography");
        });
        Showcase.step(2, Scene::leave);
        Showcase.layoutAct();
    }

    private static void layoutAct() {
        if (Showcase.stopHere("y")) {
            return;
        }
        Showcase.step(40, () -> {
            act = "y";
            Showcase.closeWindow();
            Showcase.server(p -> {
                TableActions.spill(p.m_284548_(), TABLE);
                p.m_150109_().m_36054_(new ItemStack((ItemLike)Items.f_42647_, 6));
                p.m_150109_().m_36054_(new ItemStack((ItemLike)Items.f_42398_, 4));
            });
            Showcase.standLooking(new Vec3(0.5, -60.0, 3.36), Vec3.m_82512_((Vec3i)TABLE));
        });
        Showcase.step(30, () -> Showcase.useAt(TABLE, new Vec3(0.5, 1.0, 0.8), Direction.UP));
        Showcase.step(70, () -> Showcase.check("lay out: at the table", Scene.station instanceof TableStation && Scene.phase == Scene.Phase.ACTIVE, "phase=" + String.valueOf((Object)Scene.phase)));
        Showcase.step(4, () -> {
            double[] at = Showcase.table().recipeBook.togglePoint();
            Showcase.pointGui(at[0], at[1]);
            Showcase.screenClick(0);
        });
        Showcase.step(15, () -> Showcase.recipeSearch("wooden_pickaxe"));
        Showcase.step(4, () -> Showcase.pointRecipe("wooden_pickaxe"));
        Showcase.step(3, () -> Showcase.screenClick(0));
        Showcase.step(10, () -> {
            double[] at = Showcase.table().recipeBook.layPoint();
            Showcase.pointGui(at[0], at[1]);
        });
        Showcase.step(6, () -> {
            Showcase.shot("y0_lay_button");
            Showcase.screenClick(0);
        });
        Showcase.step(9, () -> Showcase.shot("y1_laying"));
        Showcase.step(9, () -> Showcase.shot("y2_laying"));
        Showcase.step(45, () -> {
            ClientTables.Entry entry = ClientTables.get(TABLE);
            int laid = 0;
            for (int i = 0; entry != null && i < 9; ++i) {
                laid += entry.items[i].m_41619_() ? 0 : 1;
            }
            ItemStack result = entry == null ? ItemStack.f_41583_ : TableCrafting.match((Level)Minecraft.m_91087_().f_91073_, entry.list(), Scene.station.sideDirection()).map(TableCrafting.Match::result).orElse(ItemStack.f_41583_);
            Showcase.check("lay out: the pickaxe laid out from the inventory", laid == 5 && result.m_150930_(Items.f_42422_), "laid=" + laid + " result=" + Showcase.describe(result));
            Showcase.shot("y3_laid_out");
        });
        Showcase.step(4, () -> Showcase.server(p -> TableActions.spill(p.m_284548_(), TABLE)));
        Showcase.step(20, () -> Showcase.check("lay out: the table cleared", ClientTables.get(TABLE) == null || ClientTables.get(TABLE).list().stream().allMatch(ItemStack::m_41619_), ""));
        Showcase.step(4, () -> Showcase.useAt(TABLE, new Vec3(0.5, 1.0, 0.8), Direction.UP));
        Showcase.step(70, () -> {
            double[] at = Showcase.table().recipeBook.togglePoint();
            Showcase.pointGui(at[0], at[1]);
            Showcase.screenClick(0);
        });
        Showcase.step(15, () -> Showcase.recipeSearch("diamond_pickaxe"));
        Showcase.step(4, () -> Showcase.pointRecipe("diamond_pickaxe"));
        Showcase.step(3, () -> Showcase.screenClick(0));
        Showcase.step(10, () -> {
            double[] at = Showcase.table().recipeBook.layPoint();
            Showcase.pointGui(at[0], at[1]);
            Showcase.screenClick(0);
        });
        Showcase.step(40, () -> {
            ClientTables.Entry entry = ClientTables.get(TABLE);
            int laid = 0;
            for (int i = 0; entry != null && i < 9; ++i) {
                laid += entry.items[i].m_41619_() ? 0 : 1;
            }
            Showcase.check("lay out: only the sticks go down, the diamonds stay see-through", laid == 2 && Showcase.table().recipeBook.guide.missingCount == 3, "laid=" + laid + " missing=" + Showcase.table().recipeBook.guide.missingCount);
            Showcase.pointAside();
            Showcase.shot("y4_partial");
        });
        Showcase.step(2, () -> Showcase.recipeControl(113, 194));
        Showcase.step(2, () -> Showcase.recipeControl(113, 12));
        Showcase.tableSpreadScript();
        Showcase.tableLayAllScript();
        Showcase.lookAct();
    }

    private static void testInventory(ItemStack ... stacks) {
        Showcase.server(p -> {
            int i;
            if (savedInventory.isEmpty()) {
                for (i = 0; i < 36; ++i) {
                    savedInventory.add(p.m_150109_().m_8020_(i).m_41777_());
                }
            }
            for (i = 0; i < 36; ++i) {
                p.m_150109_().m_6836_(i, i < stacks.length ? stacks[i].m_41777_() : ItemStack.f_41583_);
            }
            p.f_36095_.m_142503_(ItemStack.f_41583_);
            p.f_36095_.m_38946_();
        });
    }

    private static void restoreInventory() {
        Showcase.server(p -> {
            for (int i = 0; i < 36 && i < savedInventory.size(); ++i) {
                p.m_150109_().m_6836_(i, savedInventory.get(i));
            }
            savedInventory.clear();
            p.f_36095_.m_142503_(ItemStack.f_41583_);
            p.f_36095_.m_38946_();
        });
    }

    private static void wipeTable(ServerPlayer p) {
        TableStore.get(p.m_284548_()).remove(TABLE);
        TableActions.sync(p.m_284548_(), TABLE);
    }

    private static void putOnTable(ServerPlayer p, int row, int col, ItemStack stack) {
        TableStore store = TableStore.get(p.m_284548_());
        store.getOrCreate((BlockPos)Showcase.TABLE).items.set(TableGrid.worldIndex(Scene.station.sideDirection(), row, col), (Object)stack.m_41777_());
        store.m_77762_();
        TableActions.sync(p.m_284548_(), TABLE);
    }

    private static ItemStack onTable(ServerPlayer p, int row, int col) {
        TableStore.Table stored = TableStore.get(p.m_284548_()).get(TABLE);
        return stored == null ? ItemStack.f_41583_ : (ItemStack)stored.items.get(TableGrid.worldIndex(Scene.station.sideDirection(), row, col));
    }

    private static int heldEverywhere(ServerPlayer p, Item item) {
        int total = p.f_36095_.m_142621_().m_150930_(item) ? p.f_36095_.m_142621_().m_41613_() : 0;
        for (int i = 0; i < 36; ++i) {
            if (!p.m_150109_().m_8020_(i).m_150930_(item)) continue;
            total += p.m_150109_().m_8020_(i).m_41613_();
        }
        TableStore.Table stored = TableStore.get(p.m_284548_()).get(TABLE);
        for (int i = 0; stored != null && i < 9; ++i) {
            if (!((ItemStack)stored.items.get(i)).m_150930_(item)) continue;
            total += ((ItemStack)stored.items.get(i)).m_41613_();
        }
        return total;
    }

    private static void screenPress(int button) {
        Showcase.screenClick(button);
    }

    private static void screenDrag(int button) {
        double[] at = Picking.mouseGui();
        Minecraft.m_91087_().f_91080_.m_7979_(at[0], at[1], button, 1.0, 0.0);
    }

    private static void screenRelease(int button) {
        double[] at = Picking.mouseGui();
        Minecraft.m_91087_().f_91080_.m_6348_(at[0], at[1], button);
    }

    private static void tableSpreadScript() {
        Showcase.step(2, () -> {
            StationActionPayload spread = new StationActionPayload(TABLE, StationKind.TABLE, StationActionPayload.Action.SPREAD, 511, 0, false, 0);
            StationActionPayload place = new StationActionPayload(TABLE, StationKind.TABLE, StationActionPayload.Action.PLACE, 8, -1, true, 99);
            StationActionPayload quick = new StationActionPayload(TABLE, StationKind.BREWING, StationActionPayload.Action.QUICK, 35, 0, true, 0);
            for (StationActionPayload sent : List.of(spread, place, quick)) {
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                sent.write(buf);
                StationActionPayload read = StationActionPayload.read(buf);
                Showcase.check("station action " + String.valueOf((Object)sent.action()) + " survives the codec", read.equals(sent) && buf.readableBytes() == 0, "read=" + String.valueOf(read));
                buf.release();
            }
        });
        Showcase.step(4, () -> {
            Showcase.testInventory(new ItemStack[0]);
            Showcase.server(p -> {
                Showcase.wipeTable(p);
                p.f_36095_.m_142503_(new ItemStack((ItemLike)Items.f_42753_, 10));
                p.f_36095_.m_38946_();
            });
        });
        Showcase.step(8, () -> Showcase.point(0, 0));
        Showcase.step(4, () -> Showcase.screenPress(0));
        Showcase.step(3, () -> Showcase.point(0, 1));
        Showcase.step(3, () -> Showcase.screenDrag(0));
        Showcase.step(3, () -> Showcase.point(0, 2));
        Showcase.step(3, () -> Showcase.screenDrag(0));
        Showcase.step(3, () -> Showcase.point(0, 1));
        Showcase.step(3, () -> Showcase.screenDrag(0));
        Showcase.step(6, () -> {
            Showcase.check("spread: three cells gathered, three each", Integer.bitCount(Showcase.table().spreadCells()) == 3 && Showcase.table().spreadShare() == 3, "cells=" + Integer.bitCount(Showcase.table().spreadCells()) + " share=" + Showcase.table().spreadShare());
            Showcase.shot("y5_spread_preview");
        });
        Showcase.step(2, () -> Showcase.screenRelease(0));
        Showcase.step(16, () -> {
            Showcase.server(p -> Showcase.check("spread: 3 + 3 + 3 on the table, 1 in hand, nothing made or lost", Showcase.onTable(p, 0, 0).m_41613_() == 3 && Showcase.onTable(p, 0, 1).m_41613_() == 3 && Showcase.onTable(p, 0, 2).m_41613_() == 3 && p.f_36095_.m_142621_().m_41613_() == 1 && Showcase.heldEverywhere(p, Items.f_42753_) == 10, Showcase.describe(Showcase.onTable(p, 0, 0)) + " " + Showcase.describe(Showcase.onTable(p, 0, 1)) + " " + Showcase.describe(Showcase.onTable(p, 0, 2)) + " hand=" + Showcase.describe(p.f_36095_.m_142621_()) + " total=" + Showcase.heldEverywhere(p, Items.f_42753_)));
            Showcase.shot("y6_spread_done");
        });
        Showcase.step(4, () -> Showcase.server(p -> {
            Showcase.wipeTable(p);
            Showcase.putOnTable(p, 1, 0, new ItemStack((ItemLike)Items.f_42753_, 62));
            Showcase.putOnTable(p, 1, 1, new ItemStack((ItemLike)Items.f_42398_));
            p.f_36095_.m_142503_(new ItemStack((ItemLike)Items.f_42753_, 10));
            p.f_36095_.m_38946_();
        }));
        Showcase.step(10, () -> Showcase.point(1, 0));
        Showcase.step(4, () -> Showcase.screenPress(0));
        Showcase.step(3, () -> Showcase.point(1, 1));
        Showcase.step(3, () -> Showcase.screenDrag(0));
        Showcase.step(3, () -> Showcase.point(1, 2));
        Showcase.step(3, () -> Showcase.screenDrag(0));
        Showcase.step(4, () -> Showcase.check("spread: the other item is passed over", Integer.bitCount(Showcase.table().spreadCells()) == 2, "cells=" + Integer.bitCount(Showcase.table().spreadCells())));
        Showcase.step(2, () -> Showcase.screenRelease(0));
        Showcase.step(16, () -> Showcase.server(p -> Showcase.check("spread: 62 + 2 = a full stack, 5 beside it, 3 in hand, the stick untouched", Showcase.onTable(p, 1, 0).m_41613_() == 64 && Showcase.onTable(p, 1, 2).m_41613_() == 5 && Showcase.onTable(p, 1, 1).m_150930_(Items.f_42398_) && Showcase.onTable(p, 1, 1).m_41613_() == 1 && p.f_36095_.m_142621_().m_41613_() == 3 && Showcase.heldEverywhere(p, Items.f_42753_) == 72, Showcase.describe(Showcase.onTable(p, 1, 0)) + " " + Showcase.describe(Showcase.onTable(p, 1, 1)) + " " + Showcase.describe(Showcase.onTable(p, 1, 2)) + " hand=" + Showcase.describe(p.f_36095_.m_142621_()))));
        Showcase.step(4, () -> Showcase.server(p -> {
            Showcase.wipeTable(p);
            p.f_36095_.m_142503_(new ItemStack((ItemLike)Items.f_42753_, 10));
            p.f_36095_.m_38946_();
        }));
        Showcase.step(10, () -> Showcase.point(2, 2));
        Showcase.step(4, () -> Showcase.clickCell(2, 2, 0));
        Showcase.step(16, () -> Showcase.server(p -> Showcase.check("spread: a plain click lays the whole stack, as before", Showcase.onTable(p, 2, 2).m_41613_() == 10 && p.f_36095_.m_142621_().m_41619_(), Showcase.describe(Showcase.onTable(p, 2, 2)) + " hand=" + Showcase.describe(p.f_36095_.m_142621_()))));
        Showcase.step(4, () -> Showcase.server(p -> {
            Showcase.wipeTable(p);
            p.f_36095_.m_142503_(new ItemStack((ItemLike)Items.f_42753_, 2));
            p.f_36095_.m_38946_();
        }));
        Showcase.step(10, () -> Showcase.point(0, 0));
        Showcase.step(4, () -> Showcase.screenPress(0));
        Showcase.step(3, () -> Showcase.point(0, 1));
        Showcase.step(3, () -> Showcase.screenDrag(0));
        Showcase.step(3, () -> Showcase.point(0, 2));
        Showcase.step(3, () -> Showcase.screenDrag(0));
        Showcase.step(2, () -> Showcase.screenRelease(0));
        Showcase.step(16, () -> Showcase.server(p -> Showcase.check("spread: no more cells than items", Showcase.onTable(p, 0, 0).m_41613_() == 1 && Showcase.onTable(p, 0, 1).m_41613_() == 1 && Showcase.onTable(p, 0, 2).m_41619_() && p.f_36095_.m_142621_().m_41619_(), Showcase.describe(Showcase.onTable(p, 0, 0)) + " " + Showcase.describe(Showcase.onTable(p, 0, 1)) + " " + Showcase.describe(Showcase.onTable(p, 0, 2)))));
        Showcase.step(4, () -> Showcase.server(Showcase::wipeTable));
    }

    private static void tableLayAllScript() {
        Showcase.step(6, () -> Showcase.testInventory(new ItemStack((ItemLike)Items.f_42413_, 5), new ItemStack((ItemLike)Items.f_42414_, 7), new ItemStack((ItemLike)Items.f_42398_, 20)));
        Showcase.step(10, () -> {
            double[] at = Showcase.table().recipeBook.togglePoint();
            Showcase.pointGui(at[0], at[1]);
            Showcase.screenClick(0);
        });
        Showcase.step(15, () -> Showcase.recipeSearch("torch"));
        Showcase.step(4, () -> Showcase.pointRecipe("torch"));
        Showcase.step(3, () -> Showcase.screenClick(0));
        Showcase.step(6, () -> {
            TableRecipeGuide.MaxLayout max = Showcase.table().recipeBook.guide.maxLayout();
            Showcase.check("lay out all: seven torch sets offered", max.sets() == 7 && max.current() == 0, "sets=" + max.sets() + " moves=" + max.moves().size());
            Showcase.table().recipeBook.layOut(true);
        });
        Showcase.step(8, () -> Showcase.shot("y7_lay_all_moving"));
        Showcase.step(40, () -> {
            Showcase.server(p -> Showcase.check("lay out all: 7 charcoal over 7 sticks, the coal stays", Showcase.heldEverywhere(p, Items.f_42414_) == 7 && Showcase.heldEverywhere(p, Items.f_42413_) == 5 && Showcase.heldEverywhere(p, Items.f_42398_) == 20 && TableStore.get(p.m_284548_()).get(TABLE) != null && TableStore.get((ServerLevel)p.m_284548_()).get((BlockPos)Showcase.TABLE).items.stream().mapToInt(s -> s.m_150930_(Items.f_42414_) || s.m_150930_(Items.f_42398_) ? s.m_41613_() : 0).sum() == 14, "charcoal=" + Showcase.heldEverywhere(p, Items.f_42414_) + " sticks=" + Showcase.heldEverywhere(p, Items.f_42398_)));
            Showcase.check("lay out all: the table shows the torch", Showcase.table().result.m_150930_(Items.f_42000_), "result=" + Showcase.describe(Showcase.table().result));
            Showcase.pointAside();
            Showcase.shot("y8_lay_all_torches");
        });
        Showcase.step(4, () -> Showcase.table().craft(true));
        Showcase.step(30, () -> Showcase.server(p -> Showcase.check("lay out all: a real craft of every set gives 28 torches", Showcase.heldEverywhere(p, Items.f_42000_) == 28 && Showcase.heldEverywhere(p, Items.f_42414_) == 0 && Showcase.heldEverywhere(p, Items.f_42398_) == 13, "torches=" + Showcase.heldEverywhere(p, Items.f_42000_) + " sticks=" + Showcase.heldEverywhere(p, Items.f_42398_))));
        Showcase.step(4, () -> {
            Showcase.testInventory(new ItemStack((ItemLike)Items.f_42584_, 16), new ItemStack((ItemLike)Items.f_42584_, 16), new ItemStack((ItemLike)Items.f_42584_, 8), new ItemStack((ItemLike)Items.f_42593_, 40));
            Showcase.server(Showcase::wipeTable);
        });
        Showcase.step(10, () -> Showcase.recipeSearch("ender_eye"));
        Showcase.step(4, () -> Showcase.pointRecipe("ender_eye"));
        Showcase.step(3, () -> Showcase.screenClick(0));
        Showcase.step(6, () -> Showcase.table().recipeBook.layOut(false));
        Showcase.step(30, () -> {
            TableRecipeGuide.MaxLayout max = Showcase.table().recipeBook.guide.maxLayout();
            Showcase.check("lay out all: one set lies, sixteen offered", max.current() == 1 && max.sets() == 16, "current=" + max.current() + " sets=" + max.sets());
            Showcase.table().recipeBook.layOut(true);
        });
        Showcase.step(40, () -> Showcase.server(p -> {
            TableStore.Table stored = TableStore.get(p.m_284548_()).get(TABLE);
            int pearls = 0;
            int powder = 0;
            for (int i = 0; stored != null && i < 9; ++i) {
                if (((ItemStack)stored.items.get(i)).m_150930_(Items.f_42584_)) {
                    pearls = Math.max(pearls, ((ItemStack)stored.items.get(i)).m_41613_());
                }
                if (!((ItemStack)stored.items.get(i)).m_150930_(Items.f_42593_)) continue;
                powder = Math.max(powder, ((ItemStack)stored.items.get(i)).m_41613_());
            }
            Showcase.check("lay out all: topped up to the pearl's stack limit, nothing lost", pearls == 16 && powder == 16 && Showcase.heldEverywhere(p, Items.f_42584_) == 40 && Showcase.heldEverywhere(p, Items.f_42593_) == 40, "pearls=" + pearls + " powder=" + powder + " total=" + Showcase.heldEverywhere(p, Items.f_42584_));
        }));
        Showcase.step(2, () -> Showcase.shot("y9_lay_all_pearls"));
        Showcase.step(4, () -> {
            Showcase.testInventory(new ItemStack((ItemLike)Items.f_42398_, 10));
            Showcase.server(Showcase::wipeTable);
        });
        Showcase.step(10, () -> Showcase.recipeSearch("diamond_pickaxe"));
        Showcase.step(4, () -> Showcase.pointRecipe("diamond_pickaxe"));
        Showcase.step(3, () -> Showcase.screenClick(0));
        Showcase.step(8, () -> {
            TableRecipeGuide.MaxLayout max = Showcase.table().recipeBook.guide.maxLayout();
            Showcase.check("lay out all: no full set, nothing offered", max.moves().isEmpty() && max.current() == 0, "sets=" + max.sets());
            Showcase.table().recipeBook.layOut(true);
        });
        Showcase.step(20, () -> Showcase.server(p -> Showcase.check("lay out all: nothing laid without a full set", TableStore.get(p.m_284548_()).get(TABLE) == null && Showcase.heldEverywhere(p, Items.f_42398_) == 10, "sticks=" + Showcase.heldEverywhere(p, Items.f_42398_))));
        Showcase.step(2, () -> Showcase.recipeControl(113, 194));
        Showcase.step(2, () -> Showcase.recipeControl(113, 12));
        Showcase.step(4, Showcase::restoreInventory);
    }

    private static void lookAct() {
        if (Showcase.stopHere("v")) {
            return;
        }
        Showcase.step(10, () -> {
            act = "v";
            if (!(Scene.station instanceof TableStation) || Scene.phase != Scene.Phase.ACTIVE) {
                Showcase.closeWindow();
                Showcase.standLooking(new Vec3(0.5, -60.0, 3.36), Vec3.m_82512_((Vec3i)TABLE));
            }
        });
        Showcase.step(10, () -> {
            if (Scene.phase == Scene.Phase.NONE) {
                Showcase.useAt(TABLE, new Vec3(0.5, 1.0, 0.8), Direction.UP);
            }
        });
        Showcase.step(70, () -> {
            Showcase.pointAside();
            Minecraft.m_91087_().f_91080_.m_7933_(342, 0, 0);
        });
        Showcase.step(30, () -> {
            Minecraft mc = Minecraft.m_91087_();
            double away = CameraRig.last() == null ? 0.0 : CameraRig.last().pos().m_82554_(mc.f_91074_.m_146892_());
            Showcase.check("look round: the camera goes behind the player, the body is drawn", FreeLook.amount() > 0.95 && mc.f_91066_.m_92176_() == CameraType.THIRD_PERSON_BACK && away > 1.4, String.format("amount=%.2f camera=%s away=%.2f", FreeLook.amount(), mc.f_91066_.m_92176_(), away));
            Showcase.shot("v1_look");
        });
        Showcase.step(2, () -> Minecraft.m_91087_().f_91067_.f_91507_ += 500.0);
        Showcase.step(14, () -> Showcase.shot("v2_look_turned"));
        Showcase.step(2, () -> Minecraft.m_91087_().f_91067_.f_91507_ += 700.0);
        Showcase.step(14, () -> Showcase.shot("v3_look_behind"));
        Showcase.step(2, () -> Minecraft.m_91087_().f_91080_.m_7920_(342, 0, 0));
        Showcase.step(30, () -> {
            Minecraft mc = Minecraft.m_91087_();
            Showcase.check("look round: letting go, back at work", FreeLook.amount() < 0.02 && mc.f_91066_.m_92176_().m_90612_() && Scene.phase == Scene.Phase.ACTIVE, String.format("amount=%.2f camera=%s phase=%s", new Object[]{FreeLook.amount(), mc.f_91066_.m_92176_(), Scene.phase}));
            Showcase.shot("v4_back");
        });
        Showcase.step(2, () -> {
            Settings.set(Settings.LOOK_TOGGLES, true);
            Minecraft.m_91087_().f_91080_.m_7933_(342, 0, 0);
            Minecraft.m_91087_().f_91080_.m_7920_(342, 0, 0);
        });
        Showcase.step(30, () -> Showcase.check("look round, pressed on: still looking after the key is let go", FreeLook.amount() > 0.95, String.format("amount=%.2f", FreeLook.amount())));
        Showcase.step(2, () -> Minecraft.m_91087_().f_91080_.m_7933_(256, 0, 0));
        Showcase.step(30, () -> Showcase.check("look round: Esc comes back to work first", FreeLook.amount() < 0.02 && Scene.phase == Scene.Phase.ACTIVE, String.format("amount=%.2f phase=%s", new Object[]{FreeLook.amount(), Scene.phase})));
        Showcase.step(2, () -> Settings.set(Settings.LOOK_TOGGLES, false));
        Showcase.step(2, Scene::leave);
        Showcase.jukeboxAct();
    }

    private static void jukeboxAct() {
        if (Showcase.stopHere("j")) {
            return;
        }
        boolean developer = Settings.developerMode();
        Showcase.step(35, () -> {
            act = "jx";
            Showcase.closeWindow();
            Settings.set(Settings.DEVELOPER_MODE, true);
            Showcase.hold(8, new ItemStack((ItemLike)Items.f_42701_));
            Showcase.standLooking(new Vec3((double)JUKEBOX.m_123341_() - 1.6, -60.0, (double)JUKEBOX.m_123343_() + 0.5), Vec3.m_82512_((Vec3i)JUKEBOX));
        });
        Showcase.step(20, () -> Showcase.useAt(JUKEBOX, new Vec3(0.5, 1.0, 0.5), Direction.UP));
        Showcase.step(75, () -> {
            Showcase.check("jukebox waits for editing", Scene.phase == Scene.Phase.ACTIVE && Scene.station.kind() == StationKind.JUKEBOX && !Scene.station.busy(), "no automatic insertion in developer mode");
            Runnable restoreLayout = Showcase.spatialEditorScript();
            Showcase.step(2, Scene::leave);
            Showcase.step(45, () -> Settings.set(Settings.DEVELOPER_MODE, false));
            Showcase.jukeboxOperations(developer, restoreLayout);
        });
    }

    private static void jukeboxOperations(boolean developer, Runnable restoreLayout) {
        Showcase.step(40, () -> {
            act = "j";
            Showcase.closeWindow();
            Showcase.server(p -> {
                p.m_150109_().m_6836_(8, new ItemStack((ItemLike)Items.f_42701_));
                p.m_150109_().f_35977_ = 8;
            });
            Minecraft.m_91087_().f_91074_.m_150109_().f_35977_ = 8;
            Minecraft.m_91087_().m_91403_().m_104955_((Packet)new ServerboundSetCarriedItemPacket(8));
            Showcase.standLooking(new Vec3((double)JUKEBOX.m_123341_() - 1.6, -60.0, (double)JUKEBOX.m_123343_() + 0.5), Vec3.m_82512_((Vec3i)JUKEBOX));
        });
        Showcase.step(30, () -> Showcase.shot("j0_before"));
        Showcase.step(4, () -> {
            introShot = 0;
            moveShot = 0;
            Showcase.useAt(JUKEBOX, new Vec3(0.5, 1.0, 0.5), Direction.UP);
        });
        Showcase.step(110, () -> {
            Showcase.check("jukebox: a short scene, over by itself", Scene.phase == Scene.Phase.NONE, "phase=" + String.valueOf((Object)Scene.phase));
            Showcase.server(p -> Showcase.check("jukebox: the record went in and plays", (Boolean)p.m_284548_().m_8055_(JUKEBOX).m_61143_((Property)JukeboxBlock.f_54254_) != false && p.m_150109_().m_8020_(8).m_41619_(), "slot 8=" + Showcase.describe(p.m_150109_().m_8020_(8))));
        });
        Showcase.step(30, () -> {
            act = "j2";
            introShot = 0;
            moveShot = 0;
            Showcase.useAt(JUKEBOX, new Vec3(0.5, 1.0, 0.5), Direction.UP);
        });
        Showcase.step(110, () -> Showcase.server(p -> {
            boolean back = false;
            for (ItemStack stack : p.m_150109_().f_35974_) {
                back |= stack.m_150930_(Items.f_42701_);
            }
            Showcase.check("jukebox: the record taken out into the inventory", (Boolean)p.m_284548_().m_8055_(JUKEBOX).m_61143_((Property)JukeboxBlock.f_54254_) == false && back, "back=" + back);
        }));
        Showcase.step(20, () -> {
            act = "j3";
            Settings.set((ForgeConfigSpec.ConfigValue)Settings.SCENES.get((Object)StationKind.JUKEBOX), false);
            Showcase.server(p -> {
                for (int i = 0; i < p.m_150109_().f_35974_.size(); ++i) {
                    if (!((ItemStack)p.m_150109_().f_35974_.get(i)).m_150930_(Items.f_42701_)) continue;
                    p.m_150109_().m_6836_(8, p.m_150109_().m_8016_(i));
                    break;
                }
            });
        });
        Showcase.step(10, () -> Showcase.useAt(JUKEBOX, new Vec3(0.5, 1.0, 0.5), Direction.UP));
        Showcase.step(20, () -> {
            Showcase.check("jukebox, scene off: no scene", Scene.phase == Scene.Phase.NONE, "phase=" + String.valueOf((Object)Scene.phase));
            Showcase.server(p -> Showcase.check("jukebox, scene off: the record went in the vanilla way", (Boolean)p.m_284548_().m_8055_(JUKEBOX).m_61143_((Property)JukeboxBlock.f_54254_), ""));
        });
        Showcase.step(10, () -> Showcase.useAt(JUKEBOX, new Vec3(0.5, 1.0, 0.5), Direction.UP));
        Showcase.step(20, () -> {
            Showcase.server(p -> Showcase.check("jukebox, scene off: the record pops out the vanilla way", (Boolean)p.m_284548_().m_8055_(JUKEBOX).m_61143_((Property)JukeboxBlock.f_54254_) == false, ""));
            Settings.set((ForgeConfigSpec.ConfigValue)Settings.SCENES.get((Object)StationKind.JUKEBOX), true);
            restoreLayout.run();
            Settings.set(Settings.DEVELOPER_MODE, developer);
        });
        Showcase.quickAct();
    }

    private static void quickAct() {
        if (EDITOR_BATCH) {
            Showcase.step(5, () -> Showcase.finish(Minecraft.m_91087_()));
            return;
        }
        if (Showcase.stopHere("q")) {
            return;
        }
        Vec3 atTable = new Vec3(0.5, -60.0, 3.36);
        Showcase.step(40, () -> {
            act = "q";
            Showcase.closeWindow();
            Settings.set(Settings.QUICK_WINDOW, 12);
            Showcase.standLooking(atTable, Vec3.m_82512_((Vec3i)TABLE));
        });
        Showcase.step(10, () -> Showcase.useAt(TABLE, new Vec3(0.5, 1.0, 0.8), Direction.UP));
        Showcase.step(70, () -> Showcase.check("quick return: the first time, the usual way in", Scene.phase == Scene.Phase.ACTIVE && !Scene.quick, "quick=" + Scene.quick));
        Showcase.step(2, Scene::leave);
        Showcase.step(30, () -> {
            act = "q2";
            introShot = 0;
            Showcase.useAt(TABLE, new Vec3(0.5, 1.0, 0.8), Direction.UP);
        });
        Showcase.step(34, () -> {
            Showcase.check("quick return: back at the same table, at work within a second and a bit", Scene.quick && Scene.phase == Scene.Phase.ACTIVE, "quick=" + Scene.quick + " phase=" + String.valueOf((Object)Scene.phase));
            Showcase.shot("q3_quick_back");
        });
        Showcase.step(2, Scene::leave);
        Showcase.step(30, () -> {
            act = "q4";
            introShot = 0;
            Showcase.useAt(FURNACE, new Vec3(0.5, 0.6, 1.0), Direction.SOUTH);
        });
        Showcase.step(20, () -> Showcase.check("quick return: another station, the usual way in", !Scene.quick && Scene.phase == Scene.Phase.INTRO, "quick=" + Scene.quick + " phase=" + String.valueOf((Object)Scene.phase)));
        Showcase.step(80, Scene::leave);
        Showcase.step(30, () -> {
            Settings.set(Settings.QUICK_WINDOW, 5);
            Showcase.standLooking(atTable, Vec3.m_82512_((Vec3i)TABLE));
        });
        Showcase.step(10, () -> Showcase.useAt(TABLE, new Vec3(0.5, 1.0, 0.8), Direction.UP));
        Showcase.step(70, Scene::leave);
        Showcase.step(130, () -> Showcase.useAt(TABLE, new Vec3(0.5, 1.0, 0.8), Direction.UP));
        Showcase.step(20, () -> Showcase.check("quick return: past the window, the usual way in", !Scene.quick && Scene.phase == Scene.Phase.INTRO, "quick=" + Scene.quick + " phase=" + String.valueOf((Object)Scene.phase)));
        Showcase.step(70, Scene::leave);
        Showcase.step(30, () -> {
            Settings.set(Settings.QUICK_WINDOW, 12);
            Settings.set(Settings.QUICK_RETURN, false);
            Showcase.useAt(TABLE, new Vec3(0.5, 1.0, 0.8), Direction.UP);
        });
        Showcase.step(20, () -> Showcase.check("quick return turned off: the usual way in", !Scene.quick && Scene.phase == Scene.Phase.INTRO, "quick=" + Scene.quick + " phase=" + String.valueOf((Object)Scene.phase)));
        Showcase.step(70, () -> {
            Settings.set(Settings.QUICK_RETURN, true);
            Scene.leave();
        });
        Showcase.watchAct();
    }

    private static void smithAt(Vec3 feet, float yaw, StationKind kind, BlockPos pos, boolean crouch) {
        smith.m_7678_(feet.f_82479_, feet.f_82480_, feet.f_82481_, yaw, 30.0f);
        smith.m_5616_(yaw);
        Showcase.smith.f_20883_ = yaw;
        Showcase.smith.f_20884_ = yaw;
        Showcase.smith.f_20886_ = yaw;
        smith.m_20260_(crouch);
        smith.m_20124_(crouch ? Pose.CROUCHING : Pose.STANDING);
        ClientPayloads.AT_STATION.put(424242, new ClientPayloads.AtStation(kind, pos));
    }

    private static void smithMoves(BlockPos pos, StationKind kind, Direction side, List<ItemStack> items, ItemStack result, boolean bag) {
        ClientPayloads.menuDisplay(new MenuDisplayPayload(pos, kind, side.m_122416_(), items));
        ClientPayloads.menuFx(new MenuFxPayload(pos, kind, 424242, MenuFxPayload.Cue.MOVE, bag ? 1 : 0, items, result));
    }

    private static void smithShelf(MenuFxPayload.Cue cue, int slot, ItemStack book) {
        ClientPayloads.menuFx(new MenuFxPayload(SHELF, StationKind.CHISELED_BOOKSHELF, 424242, cue, slot, List.of(), book));
    }

    private static void watchAct() {
        if (Showcase.stopHere("w")) {
            return;
        }
        Showcase.step(40, () -> {
            act = "w";
            Showcase.closeWindow();
            Minecraft mc = Minecraft.m_91087_();
            smith = new RemotePlayer(mc.f_91073_, new GameProfile(UUID.nameUUIDFromBytes(new byte[]{7}), "Smith"));
            smith.m_20234_(424242);
            mc.f_91073_.m_104630_(424242, (AbstractClientPlayer)smith);
            Showcase.smithAt(new Vec3((double)ANVIL.m_123341_() + 0.5, -60.0, (double)ANVIL.m_123343_() + 0.5 + 0.86), 180.0f, StationKind.ANVIL, ANVIL, false);
            Showcase.standLooking(new Vec3((double)ANVIL.m_123341_() + 3.1, -60.0, (double)ANVIL.m_123343_() + 1.7), new Vec3((double)ANVIL.m_123341_() + 0.5, -59.0, (double)ANVIL.m_123343_() + 1.0));
        });
        Showcase.step(20, () -> Showcase.shot("w0_smith"));
        Showcase.step(2, () -> {
            ItemStack forged = new ItemStack((ItemLike)Items.f_42385_);
            forged.m_41663_(Enchantments.f_44984_, 3);
            Showcase.smithMoves(ANVIL, StationKind.ANVIL, Direction.SOUTH, List.of(new ItemStack((ItemLike)Items.f_42385_), EnchantedBookItem.m_41161_((EnchantmentInstance)new EnchantmentInstance(Enchantments.f_44984_, 3))), forged, false);
        });
        Showcase.step(1, () -> Showcase.check("watching: onlookers are told the smith began forging", ClientMenus.sinceMove(424242) < 0.3 && ClientMenus.BLOCKS.get(ANVIL) != null && ClientMenus.BLOCKS.get((Object)Showcase.ANVIL).moveResult.m_41793_(), "since=" + ClientMenus.sinceMove(424242)));
        Showcase.moveShots("w1_anvil", 0.2, 0.45, 0.6, 0.66, 0.7, 0.8, 1.02, 1.3, 1.5, 1.75, 2.05);
        Showcase.step(2, () -> Showcase.check("watching: the hammer is put away after forging", PropLayer.hammer((Entity)smith) == 0.0, "shown=" + PropLayer.hammer((Entity)smith)));
        Showcase.step(20, () -> {
            Showcase.smithAt(new Vec3((double)GRINDSTONE.m_123341_() + 0.5, -60.0, (double)GRINDSTONE.m_123343_() + 0.5 + 0.9), 180.0f, StationKind.GRINDSTONE, GRINDSTONE, false);
            Showcase.standLooking(new Vec3((double)GRINDSTONE.m_123341_() + 3.0, -60.0, (double)GRINDSTONE.m_123343_() + 2.0), new Vec3((double)GRINDSTONE.m_123341_() + 0.5, -59.2, (double)GRINDSTONE.m_123343_() + 1.0));
        });
        Showcase.step(16, () -> {
            ItemStack sword = new ItemStack((ItemLike)Items.f_42430_);
            sword.m_41663_(Enchantments.f_44977_, 3);
            Showcase.smithMoves(GRINDSTONE, StationKind.GRINDSTONE, Direction.SOUTH, List.of(sword, ItemStack.f_41583_), new ItemStack((ItemLike)Items.f_42430_), false);
        });
        Showcase.moveShots("w2_grind", 0.2, 0.45, 0.7, 1.0, 1.3, 1.55, 1.9, 2.2);
        Showcase.step(20, () -> {
            Showcase.smithAt(new Vec3((double)STONECUTTER.m_123341_() + 0.5, -60.0, (double)STONECUTTER.m_123343_() + 0.5 + 0.95), 180.0f, StationKind.STONECUTTER, STONECUTTER, false);
            Showcase.standLooking(new Vec3((double)STONECUTTER.m_123341_() - 2.3, -60.0, (double)STONECUTTER.m_123343_() + 2.1), new Vec3((double)STONECUTTER.m_123341_() + 0.5, -59.3, (double)STONECUTTER.m_123343_() + 1.0));
        });
        Showcase.step(16, () -> Showcase.smithMoves(STONECUTTER, StationKind.STONECUTTER, Direction.SOUTH, List.of(new ItemStack((ItemLike)Items.f_41905_, 24)), new ItemStack((ItemLike)Items.f_42018_), false));
        Showcase.moveShots("w3_cut", 0.15, 0.3, 0.45, 0.6, 0.8, 1.0);
        Showcase.step(20, () -> {
            Showcase.smithAt(new Vec3((double)SHELF.m_123341_() + 0.5, -60.0, (double)SHELF.m_123343_() + 0.5 + 1.77), 180.0f, StationKind.CHISELED_BOOKSHELF, SHELF, true);
            Showcase.standLooking(new Vec3((double)SHELF.m_123341_() + 2.4, -60.0, (double)SHELF.m_123343_() + 1.4), new Vec3((double)SHELF.m_123341_() + 0.3, -59.4, (double)SHELF.m_123343_() + 1.9));
        });
        Showcase.step(16, () -> Showcase.smithShelf(MenuFxPayload.Cue.PULL, 5, new ItemStack((ItemLike)Items.f_42690_)));
        Showcase.moveShots("w4_pull", 0.1, 0.3, 0.5, 0.75);
        Showcase.step(30, () -> Showcase.smithShelf(MenuFxPayload.Cue.READ, 1, new ItemStack((ItemLike)Items.f_42614_)));
        Showcase.moveShots("w5_read", 0.25, 0.6, 0.9, 1.3, 2.0);
        Showcase.step(2, () -> Showcase.check("watching: the stand-in reads a book off the shelf", ShelfBooks.reading(424242), ""));
        Showcase.step(2, () -> Showcase.smithShelf(MenuFxPayload.Cue.READ_END, 1, new ItemStack((ItemLike)Items.f_42614_)));
        Showcase.moveShots("w6_back", 0.2, 0.5, 0.85, 1.2);
        Showcase.step(10, () -> {
            Showcase.check("watching: the book is back on the shelf", !ShelfBooks.reading(424242), "");
            ClientPayloads.AT_STATION.remove(424242);
            Minecraft.m_91087_().f_91073_.m_171642_(424242, Entity.RemovalReason.DISCARDED);
        });
        Showcase.step(30, Showcase::thirdPersonShots);
    }

    private static void useAt(BlockPos station, Vec3 local, Direction face) {
        Minecraft mc = Minecraft.m_91087_();
        mc.f_91072_.m_233732_(mc.f_91074_, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.m_82528_((Vec3i)station).m_82549_(local), face, station, false));
    }

    private static void paceCase(String name, Vec3 feet, BlockPos station, boolean fast) {
        Showcase.step(2, () -> {
            act = name;
            Showcase.standLooking(feet, Vec3.m_82512_((Vec3i)station));
        });
        Showcase.step(10, () -> Showcase.useAt(station, new Vec3(0.5, 1.0, 0.7), Direction.UP));
        Showcase.step(18, () -> {
            Showcase.check(name + ": " + (fast ? "already working" : "still leaning in") + " after 0.9 s", fast ? Scene.phase == Scene.Phase.ACTIVE : Scene.phase == Scene.Phase.INTRO, "phase=" + String.valueOf((Object)Scene.phase) + " why=" + Scene.refused);
            Showcase.shot(name);
        });
        Showcase.step(30, () -> {
            if (Scene.phase == Scene.Phase.ACTIVE) {
                Scene.leave();
            }
        });
        Showcase.step(36, () -> {});
    }

    private static void clickWidget(String text) {
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91080_ == null) {
            return;
        }
        for (GuiEventListener child : mc.f_91080_.m_6702_()) {
            AbstractWidget widget;
            if (!(child instanceof AbstractWidget) || !(widget = (AbstractWidget)child).m_6035_().getString().equals(text)) continue;
            double x = (double)widget.m_252754_() + (double)widget.m_5711_() / 2.0;
            double y = (double)widget.m_252907_() + (double)widget.m_93694_() / 2.0;
            mc.f_91080_.m_6375_(x, y, 0);
            mc.f_91080_.m_6348_(x, y, 0);
            return;
        }
        REPORT.add("no widget \"" + text + "\" on " + mc.f_91080_.getClass().getSimpleName());
    }

    private static void noticeCase(int i, String name, BlockPos station, Vec3 feet, Vec3 local, Direction face, String reason) {
        Vec3 at = Vec3.m_82528_((Vec3i)Showcase.heightSpot(i)).m_82549_(feet);
        Showcase.step(2, () -> {
            act = name;
            Showcase.closeWindow();
            Showcase.standLooking(at, Vec3.m_82512_((Vec3i)station));
        });
        Showcase.step(18, () -> {
            Scene.refused = "";
            Minecraft mc = Minecraft.m_91087_();
            mc.f_91072_.m_233732_(mc.f_91074_, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.m_82528_((Vec3i)station).m_82549_(local), face, station, false));
        });
        Showcase.step(12, () -> {
            Minecraft mc = Minecraft.m_91087_();
            String want = Language.m_128107_().m_6834_("cinematic.notice." + reason).split("%s")[0];
            String got = ReachNotice.text();
            Showcase.check(name + ": a notice instead of a window", mc.f_91080_ == null && !Scene.active() && got != null && got.contains(want), "screen=" + (mc.f_91080_ == null ? "-" : mc.f_91080_.getClass().getSimpleName()) + " notice=" + got + " why=" + Scene.refused);
            Showcase.shot(name);
        });
    }

    private static void thirdPersonShots() {
        Showcase.step(2, () -> Showcase.server(p -> {
            p.m_8061_(EquipmentSlot.LEGS, new ItemStack((ItemLike)Items.f_42470_));
            p.m_8061_(EquipmentSlot.FEET, new ItemStack((ItemLike)Items.f_42471_));
            p.m_8999_(p.m_284548_(), -0.4, -60.0, 5.2, 168.0f, 12.0f);
        }));
        Showcase.step(30, () -> Showcase.shot("80_workshop"));
        Showcase.poseShot("81_pose_furnace", StationKind.FURNACE, FURNACE);
        Showcase.poseShot("82_pose_table", StationKind.TABLE, TABLE);
        Showcase.poseShot("83_pose_enchanting", StationKind.ENCHANTING, ENCHANT);
        Showcase.step(2, () -> ClientMenus.MOVE_BY_ENTITY.put(Minecraft.m_91087_().f_91074_.m_19879_(), Scene.clock() - 0.2));
        Showcase.poseShot("84_pose_anvil", StationKind.ANVIL, ANVIL);
        Showcase.poseShot("85_pose_grindstone", StationKind.GRINDSTONE, GRINDSTONE);
        Showcase.poseShot("86_pose_stonecutter", StationKind.STONECUTTER, STONECUTTER);
        Showcase.poseShot("87_pose_loom", StationKind.LOOM, LOOM);
        Showcase.poseShot("88_pose_brewing", StationKind.BREWING, BREWING);
        Showcase.poseShot("89_pose_campfire", StationKind.CAMPFIRE, CAMPFIRE);
        Showcase.poseShot("8a_pose_lectern", StationKind.LECTERN, LECTERN);
        Showcase.poseShot("8b_pose_beacon", StationKind.BEACON, BEACON);
        Showcase.poseShot("8c_pose_shelf", StationKind.CHISELED_BOOKSHELF, SHELF);
        Showcase.step(2, () -> {
            Minecraft mc = Minecraft.m_91087_();
            ClientPayloads.AT_STATION.remove(mc.f_91074_.m_19879_());
            mc.f_91066_.m_92157_(CameraType.FIRST_PERSON);
        });
        Showcase.step(4, () -> Showcase.server(p -> p.m_284548_().m_46961_(TABLE, true)));
        Showcase.step(10, () -> Showcase.server(p -> {
            List drops = p.m_284548_().m_45976_(ItemEntity.class, new AABB(TABLE).m_82400_(2.0));
            StringBuilder s = new StringBuilder();
            for (ItemEntity drop : drops) {
                s.append(drop.m_32055_().m_41613_()).append('x').append(drop.m_32055_().m_41720_()).append(' ');
            }
            REPORT.add("table broken, drops: " + String.valueOf(s) + "| stored: " + (TableStore.get(p.m_284548_()).get(TABLE) != null));
        }));
        Showcase.step(6, () -> Showcase.report("the end"));
        Showcase.step(6, () -> Showcase.finish(Minecraft.m_91087_()));
    }

    private static void poseShot(String name, StationKind kind, BlockPos pos) {
        Showcase.step(2, () -> {
            Minecraft mc = Minecraft.m_91087_();
            ClientPayloads.AT_STATION.put(mc.f_91074_.m_19879_(), new ClientPayloads.AtStation(kind, pos));
            mc.f_91066_.m_92157_(CameraType.THIRD_PERSON_FRONT);
            mc.f_91074_.m_146922_(0.0f);
            mc.f_91074_.m_5616_(0.0f);
            mc.f_91074_.m_5618_(0.0f);
            mc.f_91074_.m_146926_(10.0f);
        });
        Showcase.step(8, () -> Showcase.shot(name));
    }

    private static void step(int delay, Runnable action) {
        STEPS.add(new Step(delay, action));
    }

    private static void pointAt(Vec3 world) {
        double[] gui = Picking.toGui(world);
        if (gui != null) {
            Showcase.pointGui(gui[0], gui[1]);
        }
    }

    private static void pointGui(double gx, double gy) {
        Minecraft mc = Minecraft.m_91087_();
        double sx = (double)mc.m_91268_().m_85443_() / (double)mc.m_91268_().m_85445_();
        double sy = (double)mc.m_91268_().m_85444_() / (double)mc.m_91268_().m_85446_();
        mc.f_91067_.f_91507_ = gx * sx;
        mc.f_91067_.f_91508_ = gy * sy;
    }

    private static void pointAside() {
        Minecraft mc = Minecraft.m_91087_();
        Showcase.pointGui((double)mc.m_91268_().m_85445_() * 0.86, (double)mc.m_91268_().m_85446_() * 0.62);
    }

    private static void hoverDrawer() {
        Minecraft mc = Minecraft.m_91087_();
        Showcase.pointGui((double)mc.m_91268_().m_85445_() * 0.5 + 20.0, 14.0);
    }

    private static void screenClick(int button) {
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91080_ != null) {
            double[] at = Picking.mouseGui();
            mc.f_91080_.m_6375_(at[0], at[1], button);
        }
    }

    private static void inventoryClick(int slot, int button) {
        Minecraft mc = Minecraft.m_91087_();
        mc.f_91072_.m_171799_(mc.f_91074_.f_36096_.f_38840_, slot, button, ClickType.PICKUP, (Player)mc.f_91074_);
    }

    private static void invClick(int inventoryIndex, int button) {
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        NonNullList slots = player.f_36096_.f_38839_;
        for (int i = 0; i < slots.size(); ++i) {
            if (((Slot)slots.get((int)i)).f_40218_ != player.m_150109_() || ((Slot)slots.get(i)).m_150661_() != inventoryIndex) continue;
            Showcase.inventoryClick(i, button);
            return;
        }
        REPORT.add("no menu slot for inventory " + inventoryIndex);
    }

    private static void invClickItem(Item item) {
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        for (int i = 0; i < 36; ++i) {
            if (!player.m_150109_().m_8020_(i).m_150930_(item)) continue;
            Showcase.invClick(i, 0);
            return;
        }
        REPORT.add("no " + String.valueOf(item) + " in the inventory");
    }

    private static void check(String name, boolean passed, String details) {
        String line = "check " + (passed ? "passed " : "FAILED ") + name + ": " + details;
        REPORT.add(line);
        Cinematic.LOGGER.info("[Showcase] {}", (Object)line);
    }

    private static String describe(ItemStack stack) {
        return stack.m_41619_() ? "-" : stack.m_41613_() + "x" + String.valueOf(stack.m_41720_());
    }

    private static void report(String when) {
        Minecraft mc = Minecraft.m_91087_();
        REPORT.add(when + " client carried=" + String.valueOf(mc.f_91074_.f_36096_.m_142621_()) + " menu=" + mc.f_91074_.f_36096_.getClass().getSimpleName());
        Showcase.server(p -> {
            StringBuilder s = new StringBuilder();
            TableStore.Table table = TableStore.get(p.m_284548_()).get(TABLE);
            if (table != null) {
                s.append("table: ");
                for (int i = 0; i < 9; ++i) {
                    s.append(i).append('=').append(Showcase.describe((ItemStack)table.items.get(i))).append(' ');
                }
            }
            for (BlockPos pos : new BlockPos[]{FURNACE, SMOKER, BLAST}) {
                BlockEntity patt512280$temp = p.m_284548_().m_7702_(pos);
                if (!(patt512280$temp instanceof AbstractFurnaceBlockEntity)) continue;
                AbstractFurnaceBlockEntity f = (AbstractFurnaceBlockEntity)patt512280$temp;
                s.append("| ").append(p.m_284548_().m_8055_(pos).m_60734_().m_49954_().getString()).append(": ");
                for (int slot = 0; slot < 3; ++slot) {
                    s.append(Showcase.describe(f.m_8020_(slot))).append(' ');
                }
                s.append(String.format("lit=%d cook=%d/%d ", f.f_58316_, f.f_58318_, f.f_58319_));
            }
            StringBuilder inv = new StringBuilder();
            for (int i = 0; i < p.m_150109_().f_35974_.size(); ++i) {
                Optional trim;
                ItemStack stack = (ItemStack)p.m_150109_().f_35974_.get(i);
                if (stack.m_41619_()) continue;
                inv.append(i).append(':').append(Showcase.describe(stack));
                if (stack.m_41793_()) {
                    inv.append(stack.m_41785_());
                }
                if (stack.m_150930_(Items.f_42690_)) {
                    inv.append(EnchantedBookItem.m_41163_((ItemStack)stack));
                }
                if (stack.m_41788_()) {
                    inv.append('\u00ab').append(stack.m_41786_().getString()).append('\u00bb');
                }
                if ((trim = ArmorTrim.m_266285_((RegistryAccess)p.m_284548_().m_9598_(), (ItemStack)stack)).isPresent()) {
                    inv.append("[trim ").append(((ArmorTrim)trim.get()).m_266210_().m_203543_().map(k -> k.m_135782_().toString()).orElse("?")).append(']');
                }
                if (stack.m_41768_()) {
                    inv.append("{damage ").append(stack.m_41773_()).append('}');
                }
                inv.append(' ');
            }
            if (p.f_36096_ != p.f_36095_) {
                s.append("| menu slots: ");
                for (int slot = 0; slot < 4 && slot < p.f_36096_.f_38839_.size(); ++slot) {
                    s.append(slot).append('=').append(Showcase.describe(p.f_36096_.m_38853_(slot).m_7993_())).append(' ');
                }
            }
            s.append("| anvil=").append(p.m_284548_().m_8055_(ANVIL).m_60734_().m_49954_().getString()).append(' ');
            String line = when + " server: " + String.valueOf(s) + "| inventory: " + String.valueOf(inv) + "| carried=" + String.valueOf(p.f_36096_.m_142621_()) + " menu=" + p.f_36096_.getClass().getSimpleName() + String.format(" xp=%d+%.2f", p.f_36078_, Float.valueOf(p.f_36080_)) + String.format(" pos=%.2f %.2f %.2f yaw %.1f pitch %.1f crouch=%s", p.m_20185_(), p.m_20186_(), p.m_20189_(), Float.valueOf(p.m_146908_()), Float.valueOf(p.m_146909_()), p.m_6047_());
            Cinematic.LOGGER.info("[Showcase] {}", (Object)line);
            REPORT.add(line);
        });
    }

    private static void shot(String name) {
        Object object;
        Minecraft mc = Minecraft.m_91087_();
        ClientStation clientStation = Scene.station;
        if (clientStation instanceof TableStation) {
            TableStation ts = (TableStation)clientStation;
            object = "cell=" + ts.hoverCell + " result=" + ts.hoverResult + String.format(" centreOpacity=%.3f", Float.valueOf(ts.itemOpacity(4)));
        } else {
            clientStation = Scene.station;
            if (clientStation instanceof FurnaceStation) {
                FurnaceStation fs = (FurnaceStation)clientStation;
                object = "zone=" + fs.hoverZone;
            } else {
                clientStation = Scene.station;
                if (clientStation instanceof EnchantStation) {
                    EnchantStation es = (EnchantStation)clientStation;
                    object = "target=" + String.valueOf((Object)es.target) + " offer=" + es.hoverOffer;
                } else {
                    clientStation = Scene.station;
                    if (clientStation instanceof AnvilStation) {
                        AnvilStation as = (AnvilStation)clientStation;
                        object = "target=" + String.valueOf((Object)as.target) + " move=" + String.format("%.2f", as.moveTime());
                    } else {
                        clientStation = Scene.station;
                        if (clientStation instanceof GrindstoneStation) {
                            GrindstoneStation gs = (GrindstoneStation)clientStation;
                            object = "target=" + String.valueOf((Object)gs.target) + " move=" + String.format("%.2f", gs.moveTime());
                        } else {
                            clientStation = Scene.station;
                            if (clientStation instanceof SmithingStation) {
                                SmithingStation ss = (SmithingStation)clientStation;
                                object = "target=" + String.valueOf((Object)ss.target) + " move=" + String.format("%.2f", ss.moveTime());
                            } else {
                                clientStation = Scene.station;
                                if (clientStation instanceof LoomStation) {
                                    LoomStation ls = (LoomStation)clientStation;
                                    object = "target=" + String.valueOf((Object)ls.target) + " pattern=" + ls.hoverPattern + " move=" + String.format("%.2f", ls.moveTime());
                                } else {
                                    clientStation = Scene.station;
                                    if (clientStation instanceof CartographyStation) {
                                        CartographyStation ms = (CartographyStation)clientStation;
                                        object = "target=" + String.valueOf((Object)ms.target) + " move=" + String.format("%.2f", ms.moveTime());
                                    } else {
                                        clientStation = Scene.station;
                                        if (clientStation instanceof BrewingStation) {
                                            BrewingStation bs = (BrewingStation)clientStation;
                                            object = "target=" + String.valueOf((Object)bs.target) + " slot=" + bs.hoverSlot;
                                        } else {
                                            clientStation = Scene.station;
                                            if (clientStation instanceof CampfireStation) {
                                                CampfireStation fs2 = (CampfireStation)clientStation;
                                                object = "target=" + String.valueOf((Object)fs2.target) + " spot=" + fs2.hoverSpot;
                                            } else {
                                                clientStation = Scene.station;
                                                if (clientStation instanceof ShelfStation) {
                                                    ShelfStation es = (ShelfStation)clientStation;
                                                    object = "target=" + String.valueOf((Object)es.target) + " slot=" + es.hoverSlot + " move=" + es.moveName();
                                                } else {
                                                    clientStation = Scene.station;
                                                    if (clientStation instanceof StonecutterStation) {
                                                        StonecutterStation cs = (StonecutterStation)clientStation;
                                                        object = "target=" + String.valueOf((Object)cs.target) + " shape=" + cs.hoverShape + " move=" + String.format("%.2f", cs.moveTime());
                                                    } else {
                                                        object = "-";
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        String hover = object;
        String line = String.format("%s fps=%d phase=%s t=%.2f %s", new Object[]{name, mc.m_260875_(), Scene.phase, Scene.time, hover});
        REPORT.add(line);
        Cinematic.LOGGER.info("[Showcase] {}", (Object)line);
        mc.m_91300_().m_94919_();
        FrameProbe.shotTaken();
        Screenshot.m_92295_((File)mc.f_91069_, (String)("showcase_" + name + ".png"), (RenderTarget)mc.m_91385_(), message -> {});
    }

    private static void finish(Minecraft mc) {
        done = true;
        for (String line : REPORT) {
            Cinematic.LOGGER.info("[Showcase] report {}", (Object)line);
        }
        mc.m_91395_();
    }

    private static void server(Consumer<ServerPlayer> action) {
        Minecraft mc = Minecraft.m_91087_();
        IntegratedServer server = mc.m_91092_();
        if (server == null || mc.f_91074_ == null) {
            return;
        }
        UUID id = mc.f_91074_.m_20148_();
        server.execute(() -> {
            ServerPlayer player = server.m_6846_().m_11259_(id);
            if (player != null) {
                action.accept(player);
            }
        });
    }

    private static void cmd(String command) {
        Showcase.server(p -> p.f_8924_.m_129892_().m_230957_(p.m_20203_().m_81324_().m_81325_(4), command));
    }

    /*
     * Unable to fully structure code
     */
    private static /* synthetic */ void lambda$spatialEditorScript$614(SpatialEditor editor, String prefix, StationKind kind) {
        block5: {
            block4: {
                Settings.set(Settings.DEVELOPER_MODE, false);
                Minecraft.m_91087_().f_91080_.m_7933_(71, 0, 0);
                at = editor.togglePoint();
                Showcase.pointGui(at[0], at[1]);
                Showcase.screenClick(0);
                Showcase.check(prefix + " developer gate", editor.editing() == false && editor.over(at[0], at[1]) == false, "G and gear unavailable");
                invalid = new SpatialAppearance(null, Arrays.asList(new SurfaceAppearance.Region[]{null}));
                Showcase.check(prefix + " malformed profile normalized", invalid.equals(SpatialAppearance.VANILLA), "missing slots restored");
                if (kind != StationKind.LOOM) break block4;
                loomFrame = SpatialAppearance.Frame.of(kind, BlockPos.f_121853_, Direction.SOUTH, Direction.UP);
                v0 = prefix + " original loom corners";
                v1 = new Vec3(0.87, 1.0, 0.84);
                if (!(SpatialAppearance.VANILLA.point(loomFrame, 1).m_82554_(v1) < 1.0E-9)) ** GOTO lbl-1000
                v2 = new Vec3(0.13, 1.0, 0.84);
                if (SpatialAppearance.VANILLA.point(loomFrame, 2).m_82554_(v2) < 1.0E-9) {
                    v3 = true;
                } else lbl-1000:
                // 2 sources

                {
                    v3 = false;
                }
                Showcase.check(v0, v3, "original corner references");
                lowered = SpatialAppearance.VANILLA.with(0, SurfaceAppearance.Region.VANILLA.with(5, -1.0));
                Showcase.check(prefix + " cloth can move down", Math.abs(lowered.point((SpatialAppearance.Frame)loomFrame, (int)0).f_82480_ - 0.9375) < 1.0E-9, "lower model surfaces supported");
                break block5;
            }
            if (kind != StationKind.ENCHANTING) break block5;
            enchantFrame = SpatialAppearance.Frame.of(kind, BlockPos.f_121853_, Direction.SOUTH, Direction.UP);
            v4 = prefix + " original enchanting positions";
            v5 = new Vec3(0.5, 1.37, 0.5);
            if (!(SpatialAppearance.VANILLA.point(enchantFrame, 0).m_82554_(v5) < 1.0E-9)) ** GOTO lbl-1000
            v6 = new Vec3(0.94, 1.02, 0.68);
            if (!(SpatialAppearance.VANILLA.point(enchantFrame, 1).m_82554_(v6) < 1.0E-9)) ** GOTO lbl-1000
            v7 = new Vec3(0.5, 1.67, 0.38);
            if (SpatialAppearance.VANILLA.point(enchantFrame, 3).m_82554_(v7) < 1.0E-9) {
                v8 = true;
            } else lbl-1000:
            // 3 sources

            {
                v8 = false;
            }
            Showcase.check(v4, v8, "original item, socket and crown");
        }
        Settings.set(Settings.DEVELOPER_MODE, true);
    }

    /*
     * Unable to fully structure code
     */
    private static /* synthetic */ void lambda$fieldGuideScript$241() {
        Showcase.check("no recipe viewer: no field guide", Showcase.guide() == null || Showcase.guide().present() == false, "absent");
        patt232801$temp = Scene.station;
        if (!(patt232801$temp instanceof TableStation)) ** GOTO lbl-1000
        t = (TableStation)patt232801$temp;
        if (t.recipeBook.guide.entries.size() > 100) {
            v0 = true;
        } else lbl-1000:
        // 2 sources

        {
            v0 = false;
        }
        Showcase.check("no recipe viewer: the table's reference is still there", v0, "entries");
    }

    static {
        t = -1;
        STEPS = new ArrayDeque<Step>();
        REPORT = new ArrayList<String>();
        INTRO_SHOTS = new double[]{0.15, 0.35, 0.55, 0.75, 0.92};
        OUTRO_SHOTS = new double[]{0.1, 0.25, 0.45, 0.65, 0.85};
        MOVE_SHOTS = new double[]{0.15, 0.35, 0.55, 0.75, 0.95};
        act = "0";
        ANVIL_RESULTS = new LinkedHashMap<String, String>();
        beefPace = new long[3];
        savedInventory = new ArrayList<ItemStack>();
    }

    private record Step(int delay, Runnable action) {
    }

    private record QuickCase(String name, Consumer<ServerPlayer> before, int slot, Predicate<ServerPlayer> expect, int settle) {
        QuickCase(String name, int slot, Predicate<ServerPlayer> expect) {
            this(name, p -> {}, slot, expect, 0);
        }
    }
}


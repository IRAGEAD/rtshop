/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.gui.screens.inventory.MenuAccess
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.inventory.AnvilMenu
 *  net.minecraft.world.inventory.BlastFurnaceMenu
 *  net.minecraft.world.inventory.CartographyTableMenu
 *  net.minecraft.world.inventory.CraftingMenu
 *  net.minecraft.world.inventory.EnchantmentMenu
 *  net.minecraft.world.inventory.FurnaceMenu
 *  net.minecraft.world.inventory.GrindstoneMenu
 *  net.minecraft.world.inventory.LoomMenu
 *  net.minecraft.world.inventory.SmithingMenu
 *  net.minecraft.world.inventory.SmokerMenu
 *  net.minecraft.world.inventory.StonecutterMenu
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.BlockHitResult
 */
package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.client.ClientNet;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.Settings;
import dev.dyslexic.cinematic.station.StationKind;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.BlastFurnaceMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.SmokerMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class ClientOnly {
    @Nullable
    private static BlockPos pendingPos;
    @Nullable
    private static StationKind pendingKind;
    private static long pendingAt;
    private static boolean enabledForConnection;

    private ClientOnly() {
    }

    public static void onLogin() {
        enabledForConnection = Settings.clientOnlyMode();
        pendingPos = null;
        pendingKind = null;
    }

    public static boolean serverHasMod() {
        return ClientNet.serverHasChannel();
    }

    public static boolean onVanillaUse(BlockPos pos, StationKind kind) {
        if (ClientOnly.serverHasMod()) {
            return false;
        }
        pendingPos = null;
        pendingKind = null;
        if (enabledForConnection && Settings.scene(kind) && ClientOnly.supported(kind)) {
            pendingPos = pos.m_7949_();
            pendingKind = kind;
            pendingAt = System.nanoTime();
        }
        return true;
    }

    public static boolean suppressUseSwing(BlockHitResult hit) {
        Minecraft mc = Minecraft.m_91087_();
        if (!enabledForConnection || ClientOnly.serverHasMod() || mc.f_91074_ == null || mc.f_91073_ == null || mc.f_91074_.m_36341_() || mc.f_91074_.m_5833_()) {
            return false;
        }
        BlockState state = mc.f_91073_.m_8055_(hit.m_82425_());
        StationKind kind = StationKind.of(state);
        return kind != null && kind.startsOnUse() && ClientOnly.supported(kind) && Settings.scene(kind) && kind.usable((BlockGetter)mc.f_91073_, hit.m_82425_(), state) && kind.usedOn(state, hit.m_82434_());
    }

    public static boolean onScreenOpening(Screen next) {
        MenuAccess window;
        BlockPos pos = pendingPos;
        StationKind kind = pendingKind;
        if (pos == null || kind == null || next == null) {
            return false;
        }
        if (!(next instanceof MenuAccess) || !ClientOnly.matches(kind, (window = (MenuAccess)next).m_6262_())) {
            pendingPos = null;
            pendingKind = null;
            return false;
        }
        pendingPos = null;
        pendingKind = null;
        Minecraft mc = Minecraft.m_91087_();
        return enabledForConnection && System.nanoTime() - pendingAt < 4000000000L && !ClientOnly.serverHasMod() && mc.f_91074_ != null && mc.f_91073_ != null && kind.matches(mc.f_91073_.m_8055_(pos)) && mc.f_91074_.m_20238_(pos.m_252807_()) <= 25.0 && Scene.beginClientOnly(pos, kind);
    }

    public static void clear() {
        enabledForConnection = false;
        pendingPos = null;
        pendingKind = null;
    }

    private static boolean supported(StationKind kind) {
        return switch (kind) {
            case StationKind.TABLE, StationKind.FURNACE, StationKind.SMOKER, StationKind.BLAST_FURNACE, StationKind.ENCHANTING, StationKind.ANVIL, StationKind.GRINDSTONE, StationKind.SMITHING, StationKind.STONECUTTER, StationKind.LOOM, StationKind.CARTOGRAPHY -> true;
            default -> false;
        };
    }

    private static boolean matches(StationKind kind, Object menu) {
        return switch (kind) {
            case StationKind.TABLE -> menu instanceof CraftingMenu;
            case StationKind.FURNACE -> menu instanceof FurnaceMenu;
            case StationKind.SMOKER -> menu instanceof SmokerMenu;
            case StationKind.BLAST_FURNACE -> menu instanceof BlastFurnaceMenu;
            case StationKind.ENCHANTING -> menu instanceof EnchantmentMenu;
            case StationKind.ANVIL -> menu instanceof AnvilMenu;
            case StationKind.GRINDSTONE -> menu instanceof GrindstoneMenu;
            case StationKind.SMITHING -> menu instanceof SmithingMenu;
            case StationKind.STONECUTTER -> menu instanceof StonecutterMenu;
            case StationKind.LOOM -> menu instanceof LoomMenu;
            case StationKind.CARTOGRAPHY -> menu instanceof CartographyTableMenu;
            default -> false;
        };
    }
}


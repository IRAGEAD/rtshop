/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.MenuProvider
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.inventory.AnvilMenu
 *  net.minecraft.world.inventory.BeaconMenu
 *  net.minecraft.world.inventory.BrewingStandMenu
 *  net.minecraft.world.inventory.CartographyTableMenu
 *  net.minecraft.world.inventory.ClickType
 *  net.minecraft.world.inventory.EnchantmentMenu
 *  net.minecraft.world.inventory.GrindstoneMenu
 *  net.minecraft.world.inventory.LecternMenu
 *  net.minecraft.world.inventory.LoomMenu
 *  net.minecraft.world.inventory.Slot
 *  net.minecraft.world.inventory.SmithingMenu
 *  net.minecraft.world.inventory.StonecutterMenu
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ChunkPos
 */
package dev.dyslexic.cinematic.station;

import dev.dyslexic.cinematic.Compat;
import dev.dyslexic.cinematic.api.AddonStations;
import dev.dyslexic.cinematic.net.MenuDisplayPayload;
import dev.dyslexic.cinematic.net.MenuFxPayload;
import dev.dyslexic.cinematic.net.Network;
import dev.dyslexic.cinematic.net.StationActionPayload;
import dev.dyslexic.cinematic.station.Sessions;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.LecternMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;

public final class MenuActions {
    private static final Map<UUID, Integer> SHOWN = new HashMap<UUID, Integer>();
    private static final Map<UUID, Long> LAST_FX = new HashMap<UUID, Long>();
    private static final int FX_COOLDOWN = 10;

    private MenuActions() {
    }

    public static boolean open(ServerPlayer player, Sessions.Session session) {
        MenuProvider provider = MenuActions.provider(player, session.pos(), session.kind());
        return provider != null && player.m_5893_(provider).isPresent() && MenuActions.isOpen(session, player.f_36096_);
    }

    @Nullable
    public static MenuProvider provider(ServerPlayer player, BlockPos pos, StationKind kind) {
        return player.m_9236_().m_8055_(pos).m_60750_(player.m_9236_(), pos);
    }

    public static void close(ServerPlayer player, Sessions.Session session) {
        LAST_FX.remove(player.m_20148_());
        if (MenuActions.isOpen(session, player.f_36096_)) {
            player.m_9230_();
        }
        if (SHOWN.remove(player.m_20148_()) != null) {
            MenuActions.show(player, session, List.of());
        }
    }

    public static void quickMove(ServerPlayer player, Sessions.Session session, int inventorySlot) {
        int i;
        AbstractContainerMenu menu = player.f_36096_;
        if (!MenuActions.isOpen(session, menu) || !menu.m_6875_((Player)player) || player.m_5833_()) {
            return;
        }
        int index = -1;
        for (int i2 = 0; i2 < menu.f_38839_.size(); ++i2) {
            if (((Slot)menu.f_38839_.get((int)i2)).f_40218_ != player.m_150109_() || ((Slot)menu.f_38839_.get(i2)).m_150661_() != inventorySlot) continue;
            index = i2;
            break;
        }
        if (index < 0 || !menu.m_38853_(index).m_6657_()) {
            return;
        }
        ArrayList<ItemStack> before = new ArrayList<ItemStack>(menu.f_38839_.size());
        for (Slot slot : menu.f_38839_) {
            before.add(slot.m_7993_().m_41777_());
        }
        menu.m_150399_(index, 0, ClickType.QUICK_MOVE, (Player)player);
        boolean intoStation = false;
        for (i = 0; i < menu.f_38839_.size(); ++i) {
            if (((Slot)menu.f_38839_.get((int)i)).f_40218_ == player.m_150109_() || ItemStack.m_41728_((ItemStack)((ItemStack)before.get(i)), (ItemStack)((Slot)menu.f_38839_.get(i)).m_7993_())) continue;
            intoStation = true;
            break;
        }
        if (!intoStation) {
            for (i = 0; i < menu.f_38839_.size(); ++i) {
                Slot slot = (Slot)menu.f_38839_.get(i);
                if (slot.f_40218_ != player.m_150109_() || ItemStack.m_41728_((ItemStack)((ItemStack)before.get(i)), (ItemStack)slot.m_7993_())) continue;
                slot.m_5852_((ItemStack)before.get(i));
            }
        }
        menu.m_38946_();
    }

    public static boolean isOpen(StationKind kind, AbstractContainerMenu menu) {
        return switch (kind) {
            case StationKind.ENCHANTING -> menu instanceof EnchantmentMenu;
            case StationKind.ANVIL -> menu instanceof AnvilMenu;
            case StationKind.GRINDSTONE -> menu instanceof GrindstoneMenu;
            case StationKind.SMITHING -> menu instanceof SmithingMenu;
            case StationKind.STONECUTTER -> menu instanceof StonecutterMenu;
            case StationKind.LOOM -> menu instanceof LoomMenu;
            case StationKind.CARTOGRAPHY -> menu instanceof CartographyTableMenu;
            case StationKind.BREWING -> menu instanceof BrewingStandMenu;
            case StationKind.LECTERN -> menu instanceof LecternMenu;
            case StationKind.BEACON -> menu instanceof BeaconMenu;
            default -> false;
        };
    }

    public static boolean isOpen(Sessions.Session session, AbstractContainerMenu menu) {
        if (session.kind() != StationKind.ADDON) {
            return MenuActions.isOpen(session.kind(), menu);
        }
        AddonStations.Definition definition = AddonStations.byId(session.addonId());
        return definition != null && definition.accepts(menu);
    }

    public static int inputs(StationKind kind) {
        return switch (kind) {
            case StationKind.ANVIL, StationKind.GRINDSTONE, StationKind.CARTOGRAPHY -> 2;
            case StationKind.SMITHING, StationKind.LOOM -> 3;
            case StationKind.STONECUTTER, StationKind.BEACON -> 1;
            case StationKind.BREWING -> 5;
            default -> 0;
        };
    }

    public static void tick(ServerPlayer player, Sessions.Session session) {
        int inputs = MenuActions.inputs(session.kind());
        if (inputs == 0 || !MenuActions.isOpen(session, player.f_36096_)) {
            return;
        }
        int fingerprint = 1;
        for (int i = 0; i < inputs; ++i) {
            ItemStack stack = player.f_36096_.m_38853_(i).m_7993_();
            fingerprint = fingerprint * 31 + (stack.m_41619_() ? 0 : Compat.hashItem(stack) * 7 + stack.m_41613_());
        }
        Integer last = SHOWN.put(player.m_20148_(), fingerprint);
        if (last == null || last != fingerprint) {
            ArrayList<ItemStack> items = new ArrayList<ItemStack>(inputs);
            for (int i = 0; i < inputs; ++i) {
                items.add(player.f_36096_.m_38853_(i).m_7993_().m_41777_());
            }
            MenuActions.show(player, session, items);
        }
    }

    private static void show(ServerPlayer player, Sessions.Session session, List<ItemStack> items) {
        Network.sendToTrackingChunk(player.m_284548_(), new ChunkPos(session.pos()), new MenuDisplayPayload(session.pos(), session.kind(), session.side().m_122416_(), items));
    }

    private static int resultSlot(StationKind kind) {
        return switch (kind) {
            case StationKind.ANVIL, StationKind.GRINDSTONE -> 2;
            case StationKind.STONECUTTER -> 1;
            default -> -1;
        };
    }

    public static void handle(ServerPlayer player, Sessions.Session session, StationActionPayload action) {
        if (action.action() != StationActionPayload.Action.CRAFT) {
            return;
        }
        long now = player.m_284548_().m_46467_();
        Long last = LAST_FX.get(player.m_20148_());
        if (last != null && now - last < 10L) {
            return;
        }
        LAST_FX.put(player.m_20148_(), now);
        MenuFxPayload fx = MenuFxPayload.beat(session.pos(), session.kind(), player.m_19879_());
        int result = MenuActions.resultSlot(session.kind());
        if (action.index() == 1 && result >= 0 && MenuActions.isOpen(session, player.f_36096_)) {
            ArrayList<ItemStack> items = new ArrayList<ItemStack>();
            for (int i = 0; i < MenuActions.inputs(session.kind()); ++i) {
                items.add(player.f_36096_.m_38853_(i).m_7993_().m_41777_());
            }
            fx = new MenuFxPayload(session.pos(), session.kind(), player.m_19879_(), MenuFxPayload.Cue.MOVE, action.button(), items, player.f_36096_.m_38853_(result).m_7993_().m_41777_());
        }
        Network.sendToTrackingChunk(player.m_284548_(), new ChunkPos(session.pos()), fx);
    }
}


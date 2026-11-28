/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.inventory.AbstractFurnaceMenu
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ChunkPos
 */
package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.client.ClientStation;
import dev.dyslexic.cinematic.client.FurnaceStation;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.net.FurnaceContentsPayload;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;

public final class ClientFurnaces {
    public static final Map<BlockPos, Entry> FURNACES = new HashMap<BlockPos, Entry>();
    @Nullable
    private static Entry local;
    @Nullable
    private static BlockPos localPos;
    private static int localMenuId;

    private ClientFurnaces() {
    }

    @Nullable
    public static Entry get(BlockPos pos) {
        ClientStation clientStation;
        if (Scene.clientOnly() && (clientStation = Scene.station) instanceof FurnaceStation) {
            FurnaceStation station = (FurnaceStation)clientStation;
            if (station.pos.equals((Object)pos)) {
                AbstractContainerMenu abstractContainerMenu;
                LocalPlayer player = Minecraft.m_91087_().f_91074_;
                if (player == null || !((abstractContainerMenu = player.f_36096_) instanceof AbstractFurnaceMenu)) {
                    return null;
                }
                AbstractFurnaceMenu menu = (AbstractFurnaceMenu)abstractContainerMenu;
                if (local == null || !pos.equals((Object)localPos) || localMenuId != menu.f_38840_) {
                    local = new Entry();
                    localPos = pos.m_7949_();
                    localMenuId = menu.f_38840_;
                }
                for (int i = 0; i < 3; ++i) {
                    ItemStack before = ClientFurnaces.local.items[i];
                    ItemStack after = menu.m_38853_(i).m_7993_();
                    if (ItemStack.m_41728_((ItemStack)before, (ItemStack)after)) continue;
                    if (!after.m_41619_() && (before.m_41619_() || after.m_41613_() > before.m_41613_())) {
                        ClientFurnaces.local.landedAt[i] = Scene.clock();
                    }
                    ClientFurnaces.local.items[i] = after.m_41777_();
                }
                return local;
            }
        }
        return FURNACES.get(pos);
    }

    public static ItemStack item(BlockPos pos, int slot) {
        Entry entry = ClientFurnaces.get(pos);
        return entry == null ? ItemStack.f_41583_ : entry.items[slot];
    }

    public static void apply(FurnaceContentsPayload payload) {
        if (payload.items().isEmpty()) {
            FURNACES.remove(payload.pos());
            return;
        }
        Entry entry = FURNACES.computeIfAbsent(payload.pos().m_7949_(), p -> new Entry());
        for (int i = 0; i < 3 && i < payload.items().size(); ++i) {
            ItemStack before = entry.items[i];
            ItemStack after = payload.items().get(i);
            if (!(after.m_41619_() || !before.m_41619_() && ItemStack.m_150942_((ItemStack)before, (ItemStack)after) && after.m_41613_() <= before.m_41613_())) {
                entry.landedAt[i] = Scene.clock();
            }
            entry.items[i] = after;
        }
    }

    public static void prune(ClientLevel level) {
        FURNACES.keySet().removeIf(pos -> {
            StationKind kind = StationKind.of(level.m_8055_(pos));
            return level.m_46749_(pos) && (kind == null || !kind.isFurnace());
        });
    }

    public static void unloadChunk(ChunkPos chunk) {
        FURNACES.keySet().removeIf(pos -> pos.m_123341_() >> 4 == chunk.f_45578_ && pos.m_123343_() >> 4 == chunk.f_45579_);
    }

    public static void clear() {
        FURNACES.clear();
        local = null;
        localPos = null;
        localMenuId = -1;
    }

    static {
        localMenuId = -1;
    }

    public static final class Entry {
        public final ItemStack[] items = new ItemStack[3];
        public final double[] landedAt = new double[3];

        Entry() {
            Arrays.fill(this.items, ItemStack.f_41583_);
            Arrays.fill(this.landedAt, -10.0);
        }
    }
}


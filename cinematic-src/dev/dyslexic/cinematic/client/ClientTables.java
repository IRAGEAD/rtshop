/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.inventory.CraftingMenu
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ChunkPos
 */
package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.client.ClientStation;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.TableStation;
import dev.dyslexic.cinematic.net.TableContentsPayload;
import dev.dyslexic.cinematic.table.TableGrid;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;

public final class ClientTables {
    public static final Map<BlockPos, Entry> TABLES = new HashMap<BlockPos, Entry>();
    @Nullable
    private static Entry local;
    @Nullable
    private static BlockPos localPos;
    private static int localMenuId;

    private ClientTables() {
    }

    @Nullable
    public static Entry get(BlockPos pos) {
        ClientStation clientStation;
        if (Scene.clientOnly() && (clientStation = Scene.station) instanceof TableStation) {
            TableStation table = (TableStation)clientStation;
            if (table.pos.equals((Object)pos)) {
                AbstractContainerMenu abstractContainerMenu;
                LocalPlayer player = Minecraft.m_91087_().f_91074_;
                if (player == null || !((abstractContainerMenu = player.f_36096_) instanceof CraftingMenu)) {
                    return null;
                }
                CraftingMenu menu = (CraftingMenu)abstractContainerMenu;
                if (local == null || !pos.equals((Object)localPos) || localMenuId != menu.f_38840_) {
                    local = new Entry();
                    localPos = pos.m_7949_();
                    localMenuId = menu.f_38840_;
                }
                for (int logical = 0; logical < 9; ++logical) {
                    ItemStack after;
                    int world = TableGrid.worldIndex(table.sideDirection(), logical / 3, logical % 3);
                    ItemStack before = ClientTables.local.items[world];
                    if (!ItemStack.m_41728_((ItemStack)before, (ItemStack)(after = menu.m_38853_(logical + 1).m_7993_()))) {
                        if (!after.m_41619_() && (before.m_41619_() || after.m_41613_() > before.m_41613_())) {
                            ClientTables.local.landedAt[world] = Scene.clock();
                        }
                        ClientTables.local.items[world] = after.m_41777_();
                        ++ClientTables.local.version;
                    }
                    ClientTables.local.facing[world] = (byte)table.sideDirection().m_122416_();
                }
                return local;
            }
        }
        return TABLES.get(pos);
    }

    public static void apply(TableContentsPayload payload) {
        BlockPos pos = payload.pos();
        if (payload.items().isEmpty()) {
            TABLES.remove(pos);
            return;
        }
        Entry entry = TABLES.computeIfAbsent(pos.m_7949_(), p -> new Entry());
        double now = Scene.clock();
        for (int i = 0; i < 9 && i < payload.items().size(); ++i) {
            boolean grew;
            ItemStack before = entry.items[i];
            ItemStack after = payload.items().get(i);
            boolean bl = grew = !after.m_41619_() && (before.m_41619_() || !ItemStack.m_150942_((ItemStack)before, (ItemStack)after) || after.m_41613_() > before.m_41613_());
            if (grew) {
                entry.landedAt[i] = now;
            }
            entry.items[i] = after;
            entry.facing[i] = i < payload.facing().length ? payload.facing()[i] : (byte)0;
        }
        ++entry.version;
    }

    public static void unloadChunk(ChunkPos chunk) {
        TABLES.keySet().removeIf(pos -> pos.m_123341_() >> 4 == chunk.f_45578_ && pos.m_123343_() >> 4 == chunk.f_45579_);
    }

    public static void clear() {
        TABLES.clear();
        local = null;
        localPos = null;
        localMenuId = -1;
    }

    static {
        localMenuId = -1;
    }

    public static final class Entry {
        public final ItemStack[] items = new ItemStack[9];
        public final byte[] facing = new byte[9];
        public final double[] landedAt = new double[9];
        public int version;

        Entry() {
            Arrays.fill(this.items, ItemStack.f_41583_);
            Arrays.fill(this.landedAt, -10.0);
        }

        public List<ItemStack> list() {
            return Arrays.asList(this.items);
        }
    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.particles.ItemParticleOption
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleTypes
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.Containers
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.CraftingMenu
 *  net.minecraft.world.inventory.InventoryMenu
 *  net.minecraft.world.inventory.Slot
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ChunkPos
 *  net.minecraft.world.level.Level
 */
package dev.dyslexic.cinematic.table;

import dev.dyslexic.cinematic.mixin.CraftingMenuAccess;
import dev.dyslexic.cinematic.net.Network;
import dev.dyslexic.cinematic.net.StationActionPayload;
import dev.dyslexic.cinematic.net.TableContentsPayload;
import dev.dyslexic.cinematic.station.RecipeChoices;
import dev.dyslexic.cinematic.station.Sessions;
import dev.dyslexic.cinematic.table.TableCrafting;
import dev.dyslexic.cinematic.table.TableGrid;
import dev.dyslexic.cinematic.table.TableStore;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public final class TableActions {
    private TableActions() {
    }

    public static void handle(ServerPlayer player, ServerLevel level, Sessions.Session session, StationActionPayload action) {
        switch (action.action()) {
            case CLICK: {
                TableActions.click(player, level, session, action.index(), action.button(), action.shift());
                break;
            }
            case PAINT: {
                TableActions.paint(player, level, session, action.index());
                break;
            }
            case CRAFT: {
                TableActions.craft(player, level, session, action.shift());
                break;
            }
            case PLACE: {
                TableActions.place(player, level, session, action.index(), action.button(), Math.max(1, action.side()));
                break;
            }
            case SPREAD: {
                TableActions.spread(player, level, session, action.index());
                break;
            }
        }
    }

    private static void click(ServerPlayer player, ServerLevel level, Sessions.Session session, int cell, int button, boolean shift) {
        if (cell < 0 || cell >= 9) {
            return;
        }
        TableStore store = TableStore.get(level);
        TableStore.Table table = store.getOrCreate(session.pos());
        InventoryMenu menu = player.f_36095_;
        ItemStack carried = menu.m_142621_();
        ItemStack onCell = (ItemStack)table.items.get(cell);
        boolean placed = false;
        boolean taken = false;
        if (shift && button == 0) {
            if (!onCell.m_41619_()) {
                player.m_150109_().m_36054_(onCell);
                taken = true;
                if (onCell.m_41619_()) {
                    table.items.set(cell, (Object)ItemStack.f_41583_);
                }
            }
        } else if (button == 0) {
            if (carried.m_41619_()) {
                if (!onCell.m_41619_()) {
                    menu.m_142503_(onCell);
                    table.items.set(cell, (Object)ItemStack.f_41583_);
                    taken = true;
                }
            } else if (onCell.m_41619_()) {
                int count = Math.min(carried.m_41613_(), carried.m_41741_());
                table.items.set(cell, (Object)carried.m_41620_(count));
                placed = true;
            } else if (ItemStack.m_150942_((ItemStack)carried, (ItemStack)onCell)) {
                int move = Math.min(carried.m_41613_(), onCell.m_41741_() - onCell.m_41613_());
                if (move > 0) {
                    onCell.m_41769_(move);
                    carried.m_41774_(move);
                    placed = true;
                }
            } else {
                table.items.set(cell, (Object)carried);
                menu.m_142503_(onCell);
                placed = true;
            }
        } else if (button == 1) {
            if (carried.m_41619_()) {
                if (!onCell.m_41619_()) {
                    menu.m_142503_(onCell.m_41620_((onCell.m_41613_() + 1) / 2));
                    if (onCell.m_41619_()) {
                        table.items.set(cell, (Object)ItemStack.f_41583_);
                    }
                    taken = true;
                }
            } else {
                placed = TableActions.placeOne(table, cell, carried);
                if (!placed && !ItemStack.m_150942_((ItemStack)carried, (ItemStack)onCell)) {
                    table.items.set(cell, (Object)carried);
                    menu.m_142503_(onCell);
                    placed = true;
                }
            }
        }
        if (placed) {
            table.facing[cell] = (byte)session.side().m_122416_();
        }
        if (menu.m_142621_().m_41619_()) {
            menu.m_142503_(ItemStack.f_41583_);
        }
        if (!placed && !taken) {
            store.cleanup(session.pos());
        } else {
            TableActions.finish(player, level, store, session.pos());
            float pitch = 0.9f + level.f_46441_.m_188501_() * 0.25f;
            if (placed) {
                level.m_5594_(null, session.pos(), SoundEvents.f_12634_, SoundSource.PLAYERS, 0.35f, pitch * 1.3f);
            } else {
                level.m_5594_(null, session.pos(), SoundEvents.f_12019_, SoundSource.PLAYERS, 0.18f, pitch * 1.4f);
            }
        }
    }

    private static void paint(ServerPlayer player, ServerLevel level, Sessions.Session session, int cell) {
        if (cell < 0 || cell >= 9) {
            return;
        }
        TableStore store = TableStore.get(level);
        TableStore.Table table = store.getOrCreate(session.pos());
        ItemStack carried = player.f_36095_.m_142621_();
        if (!carried.m_41619_() && TableActions.placeOne(table, cell, carried)) {
            table.facing[cell] = (byte)session.side().m_122416_();
            if (carried.m_41619_()) {
                player.f_36095_.m_142503_(ItemStack.f_41583_);
            }
            TableActions.finish(player, level, store, session.pos());
            level.m_5594_(null, session.pos(), SoundEvents.f_12634_, SoundSource.PLAYERS, 0.3f, 1.2f + level.f_46441_.m_188501_() * 0.3f);
        } else {
            store.cleanup(session.pos());
        }
    }

    private static void spread(ServerPlayer player, ServerLevel level, Sessions.Session session, int mask) {
        ItemStack carried = player.f_36095_.m_142621_();
        if (carried.m_41619_() || mask <= 0 || mask >= 512) {
            return;
        }
        TableStore store = TableStore.get(level);
        TableStore.Table table = store.getOrCreate(session.pos());
        ArrayList<Integer> cells = new ArrayList<Integer>();
        for (int cell = 0; cell < 9; ++cell) {
            ItemStack onCell = (ItemStack)table.items.get(cell);
            if ((mask & 1 << cell) == 0 || !onCell.m_41619_() && !ItemStack.m_150942_((ItemStack)carried, (ItemStack)onCell)) continue;
            cells.add(cell);
        }
        int share = cells.isEmpty() ? 0 : carried.m_41613_() / cells.size();
        boolean placed = false;
        Iterator iterator = cells.iterator();
        while (iterator.hasNext()) {
            int cell = (Integer)iterator.next();
            ItemStack onCell = (ItemStack)table.items.get(cell);
            int move = Math.min(share, carried.m_41741_() - onCell.m_41613_());
            if (move <= 0) continue;
            if (onCell.m_41619_()) {
                table.items.set(cell, (Object)carried.m_41620_(move));
            } else {
                onCell.m_41769_(move);
                carried.m_41774_(move);
            }
            table.facing[cell] = (byte)session.side().m_122416_();
            placed = true;
        }
        if (!placed) {
            store.cleanup(session.pos());
            return;
        }
        if (carried.m_41619_()) {
            player.f_36095_.m_142503_(ItemStack.f_41583_);
        }
        TableActions.finish(player, level, store, session.pos());
        level.m_5594_(null, session.pos(), SoundEvents.f_12634_, SoundSource.PLAYERS, 0.35f, 1.15f + level.f_46441_.m_188501_() * 0.25f);
    }

    private static void place(ServerPlayer player, ServerLevel level, Sessions.Session session, int cell, int slot, int count) {
        int laid;
        ItemStack source;
        if (cell < 0 || cell >= 9 || slot < -1 || slot >= 36) {
            return;
        }
        ItemStack itemStack = source = slot < 0 ? player.f_36095_.m_142621_() : player.m_150109_().m_8020_(slot);
        if (source.m_41619_()) {
            return;
        }
        TableStore store = TableStore.get(level);
        TableStore.Table table = store.getOrCreate(session.pos());
        for (laid = 0; laid < count && !source.m_41619_() && TableActions.placeOne(table, cell, source); ++laid) {
        }
        if (laid == 0) {
            store.cleanup(session.pos());
            return;
        }
        if (slot >= 0) {
            player.m_150109_().m_6596_();
        }
        if (slot < 0 && source.m_41619_()) {
            player.f_36095_.m_142503_(ItemStack.f_41583_);
        }
        table.facing[cell] = (byte)session.side().m_122416_();
        TableActions.finish(player, level, store, session.pos());
        level.m_5594_(null, session.pos(), SoundEvents.f_12634_, SoundSource.PLAYERS, 0.3f, 1.25f + level.f_46441_.m_188501_() * 0.25f);
    }

    private static boolean placeOne(TableStore.Table table, int cell, ItemStack carried) {
        ItemStack onCell = (ItemStack)table.items.get(cell);
        if (onCell.m_41619_()) {
            table.items.set(cell, (Object)carried.m_41620_(1));
            return true;
        }
        if (ItemStack.m_150942_((ItemStack)carried, (ItemStack)onCell) && onCell.m_41613_() < onCell.m_41741_()) {
            onCell.m_41769_(1);
            carried.m_41774_(1);
            return true;
        }
        return false;
    }

    private static void craft(ServerPlayer player, ServerLevel level, Sessions.Session session, boolean all) {
        List<ItemStack> used;
        TableStore store = TableStore.get(level);
        TableStore.Table table = store.get(session.pos());
        if (table == null) {
            return;
        }
        ArrayList<ItemStack> consumed = new ArrayList<ItemStack>();
        ResourceLocation chosen = RecipeChoices.chosen((Player)player);
        ItemStack first = TableCrafting.match((Level)level, table.items, session.side(), chosen).map(TableCrafting.Match::result).orElse(ItemStack.f_41583_);
        int crafts = 0;
        while ((used = TableCrafting.craftOnce(player, level, table, session.side(), all)) != null) {
            ItemStack next;
            consumed.addAll(used);
            if (all && ItemStack.m_150942_((ItemStack)first, (ItemStack)(next = TableCrafting.match((Level)level, table.items, session.side(), chosen).map(TableCrafting.Match::result).orElse(ItemStack.f_41583_))) && ++crafts < 64) continue;
        }
        if (crafts == 0) {
            return;
        }
        TableActions.finish(player, level, store, session.pos());
        BlockPos pos = session.pos();
        double x = (double)pos.m_123341_() + 0.5;
        double y = (double)pos.m_123342_() + 1.05;
        double z = (double)pos.m_123343_() + 0.5;
        for (int i = 0; i < consumed.size() && i < 9; ++i) {
            level.m_8767_((ParticleOptions)new ItemParticleOption(ParticleTypes.f_123752_, (ItemStack)consumed.get(i)), x, y, z, 3, 0.12, 0.02, 0.12, 0.04);
        }
        level.m_5594_(null, pos, SoundEvents.f_12635_, SoundSource.PLAYERS, 0.55f, 1.15f + level.f_46441_.m_188501_() * 0.15f);
        level.m_5594_(null, pos, SoundEvents.f_12019_, SoundSource.PLAYERS, 0.3f, 0.75f);
    }

    private static void finish(ServerPlayer player, ServerLevel level, TableStore store, BlockPos pos) {
        store.m_77762_();
        TableActions.sync(level, pos);
        store.cleanup(pos);
        player.f_36095_.m_38946_();
    }

    public static TableContentsPayload contents(BlockPos pos, @Nullable TableStore.Table table) {
        if (table == null || table.isEmpty()) {
            return new TableContentsPayload(pos, List.of(), new byte[0]);
        }
        ArrayList<ItemStack> items = new ArrayList<ItemStack>(9);
        for (ItemStack stack : table.items) {
            items.add(stack.m_41777_());
        }
        return new TableContentsPayload(pos, items, (byte[])table.facing.clone());
    }

    public static void sync(ServerLevel level, BlockPos pos) {
        Network.sendToTrackingChunk(level, new ChunkPos(pos), TableActions.contents(pos, TableStore.get(level).get(pos)));
    }

    public static void intoWindow(ServerPlayer player, CraftingMenu menu) {
        ServerLevel level = player.m_284548_();
        BlockPos pos = ((CraftingMenuAccess)menu).cinematic$access().m_6721_((at, where) -> at == level ? where : null).orElse(null);
        if (pos == null || !TableGrid.isTable(level.m_8055_(pos))) {
            return;
        }
        TableStore store = TableStore.get(level);
        TableStore.Table table = store.get(pos);
        if (table == null || table.isEmpty()) {
            return;
        }
        for (ServerPlayer other : level.m_6907_()) {
            Sessions.Session session = Sessions.get(other);
            if (session == null || !session.pos().equals((Object)pos)) continue;
            return;
        }
        for (int i = 1; i <= 9; ++i) {
            if (((Slot)menu.f_38839_.get(i)).m_7993_().m_41619_()) continue;
            return;
        }
        List<ItemStack> grid = TableGrid.oriented(table.items, TableGrid.sideFacing(pos, player.m_20185_(), player.m_20189_()));
        store.remove(pos);
        for (int i = 0; i < 9; ++i) {
            ((Slot)menu.f_38839_.get(i + 1)).m_5852_(grid.get(i));
        }
        TableActions.sync(level, pos);
        menu.m_38946_();
    }

    public static void spill(ServerLevel level, BlockPos pos) {
        TableStore store = TableStore.get(level);
        TableStore.Table table = store.remove(pos);
        if (table == null) {
            return;
        }
        Containers.m_19010_((Level)level, (BlockPos)pos, table.items);
        TableActions.sync(level, pos);
        Sessions.endAllAt(level, pos);
    }
}


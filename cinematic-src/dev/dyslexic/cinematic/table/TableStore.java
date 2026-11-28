/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.Long2ObjectMap
 *  it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.NonNullList
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.ContainerHelper
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ChunkPos
 *  net.minecraft.world.level.saveddata.SavedData
 */
package dev.dyslexic.cinematic.table;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

public final class TableStore
extends SavedData {
    private static final String NAME = "cinematic_tables";
    private final Map<BlockPos, Table> tables = new HashMap<BlockPos, Table>();
    private final Long2ObjectMap<Set<BlockPos>> byChunk = new Long2ObjectOpenHashMap();

    public static TableStore get(ServerLevel level) {
        return (TableStore)level.m_8895_().m_164861_(TableStore::load, TableStore::new, NAME);
    }

    @Nullable
    public Table get(BlockPos pos) {
        return this.tables.get(pos);
    }

    public Table getOrCreate(BlockPos pos) {
        return this.tables.computeIfAbsent(pos.m_7949_(), p -> {
            ((Set)this.byChunk.computeIfAbsent(ChunkPos.m_151388_((BlockPos)p), c -> new HashSet())).add(p);
            return new Table();
        });
    }

    @Nullable
    public Table remove(BlockPos pos) {
        Table table = this.tables.remove(pos);
        if (table != null) {
            long chunk = ChunkPos.m_151388_((BlockPos)pos);
            Set set = (Set)this.byChunk.get(chunk);
            if (set != null) {
                set.remove(pos);
                if (set.isEmpty()) {
                    this.byChunk.remove(chunk);
                }
            }
            this.m_77762_();
        }
        return table;
    }

    public void cleanup(BlockPos pos) {
        Table table = this.tables.get(pos);
        if (table != null && table.isEmpty()) {
            this.remove(pos);
        }
    }

    public Collection<BlockPos> inChunk(ChunkPos chunk) {
        Set set = (Set)this.byChunk.get(chunk.m_45588_());
        return set == null ? List.of() : set;
    }

    public List<BlockPos> positions() {
        return new ArrayList<BlockPos>(this.tables.keySet());
    }

    public CompoundTag m_7176_(CompoundTag tag) {
        ListTag list = new ListTag();
        this.tables.forEach((pos, table) -> {
            CompoundTag entry = new CompoundTag();
            entry.m_128356_("pos", pos.m_121878_());
            entry.m_128382_("facing", table.facing);
            ContainerHelper.m_18973_((CompoundTag)entry, table.items);
            list.add((Object)entry);
        });
        tag.m_128365_("tables", (Tag)list);
        return tag;
    }

    private static TableStore load(CompoundTag tag) {
        TableStore store = new TableStore();
        ListTag list = tag.m_128437_("tables", 10);
        for (int i = 0; i < list.size(); ++i) {
            CompoundTag entry = list.m_128728_(i);
            Table table = store.getOrCreate(BlockPos.m_122022_((long)entry.m_128454_("pos")));
            ContainerHelper.m_18980_((CompoundTag)entry, table.items);
            byte[] facing = entry.m_128463_("facing");
            System.arraycopy(facing, 0, table.facing, 0, Math.min(facing.length, 9));
        }
        return store;
    }

    public static final class Table {
        public final NonNullList<ItemStack> items = NonNullList.m_122780_((int)9, (Object)ItemStack.f_41583_);
        public final byte[] facing = new byte[9];

        public boolean isEmpty() {
            for (ItemStack stack : this.items) {
                if (stack.m_41619_()) continue;
                return false;
            }
            return true;
        }
    }
}


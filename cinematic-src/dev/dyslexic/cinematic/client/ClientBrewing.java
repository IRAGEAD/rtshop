/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ChunkPos
 *  net.minecraft.world.level.block.entity.BrewingStandBlockEntity
 */
package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.net.BrewingContentsPayload;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;

public final class ClientBrewing {
    private static final Map<BlockPos, ItemStack[]> STANDS = new HashMap<BlockPos, ItemStack[]>();

    private ClientBrewing() {
    }

    public static void apply(BrewingContentsPayload payload) {
        ItemStack[] slots = new ItemStack[5];
        for (int i = 0; i < slots.length; ++i) {
            slots[i] = i < payload.items().size() ? payload.items().get(i) : ItemStack.f_41583_;
        }
        STANDS.put(payload.pos().m_7949_(), slots);
    }

    public static ItemStack item(BrewingStandBlockEntity stand, int slot) {
        ItemStack[] slots = STANDS.get(stand.m_58899_());
        return slots != null ? slots[slot] : stand.m_8020_(slot);
    }

    public static void unloadChunk(ChunkPos chunk) {
        STANDS.keySet().removeIf(pos -> pos.m_123341_() >> 4 == chunk.f_45578_ && pos.m_123343_() >> 4 == chunk.f_45579_);
    }

    public static void clear() {
        STANDS.clear();
    }
}


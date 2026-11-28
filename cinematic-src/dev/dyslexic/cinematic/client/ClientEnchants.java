/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ChunkPos
 */
package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.client.EnchantFx;
import dev.dyslexic.cinematic.client.EnchantStation;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.net.EnchantDisplayPayload;
import java.lang.invoke.LambdaMetafactory;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;

public final class ClientEnchants {
    public static final Map<BlockPos, Entry> TABLES = new HashMap<BlockPos, Entry>();

    private ClientEnchants() {
    }

    @Nullable
    public static Entry get(BlockPos pos) {
        return TABLES.get(pos);
    }

    public static void apply(EnchantDisplayPayload payload) {
        Entry entry = TABLES.computeIfAbsent(payload.pos().m_7949_(), p -> new Entry());
        entry.item = payload.item();
        entry.lapis = payload.lapis();
        entry.side = Direction.m_122407_((int)payload.side());
        if (entry.item.m_41619_() && entry.lapis == 0 && Scene.clock() - entry.flashAt > 2.0) {
            TABLES.remove(payload.pos());
        }
    }

    public static void flash(BlockPos pos) {
        ClientEnchants.TABLES.computeIfAbsent((BlockPos)pos.m_7949_(), (Function<BlockPos, Entry>)LambdaMetafactory.metafactory(null, null, null, (Ljava/lang/Object;)Ljava/lang/Object;, lambda$flash$1(net.minecraft.core.BlockPos ), (Lnet/minecraft/core/BlockPos;)Ldev/dyslexic/cinematic/client/ClientEnchants$Entry;)()).flashAt = Scene.clock();
        EnchantFx.burst(EnchantStation.itemPosition(pos, ClientEnchants.TABLES.get((Object)pos).side, Scene.clock()), 0.7);
    }

    public static void unloadChunk(ChunkPos chunk) {
        TABLES.keySet().removeIf(pos -> pos.m_123341_() >> 4 == chunk.f_45578_ && pos.m_123343_() >> 4 == chunk.f_45579_);
    }

    public static void clear() {
        TABLES.clear();
    }

    private static /* synthetic */ Entry lambda$flash$1(BlockPos p) {
        return new Entry();
    }

    public static final class Entry {
        public ItemStack item = ItemStack.f_41583_;
        public int lapis;
        public Direction side = Direction.SOUTH;
        public double flashAt = -10.0;
    }
}


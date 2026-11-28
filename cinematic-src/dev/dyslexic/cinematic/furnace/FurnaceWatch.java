/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.Container
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ChunkPos
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.chunk.LevelChunk
 */
package dev.dyslexic.cinematic.furnace;

import dev.dyslexic.cinematic.net.FurnaceContentsPayload;
import dev.dyslexic.cinematic.net.Network;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

public final class FurnaceWatch {
    private FurnaceWatch() {
    }

    public static void afterTick(Level level, BlockPos pos, BlockState state, AbstractFurnaceBlockEntity furnace) {
        ServerLevel server;
        block5: {
            block4: {
                if (!(level instanceof ServerLevel)) break block4;
                server = (ServerLevel)level;
                if (FurnaceWatch.shown(state) && furnace instanceof Fingerprinted) break block5;
            }
            return;
        }
        Fingerprinted marked = (Fingerprinted)furnace;
        int fingerprint = FurnaceWatch.fingerprint(furnace);
        if (fingerprint != marked.cinematic$fingerprint()) {
            marked.cinematic$setFingerprint(fingerprint);
            FurnaceWatch.sync(server, pos, furnace);
        }
    }

    public static boolean shown(BlockState state) {
        StationKind kind = StationKind.of(state);
        return kind != null && kind.isFurnace();
    }

    public static int contentsFingerprint(Container container, int slots) {
        int hash = 1;
        for (int slot = 0; slot < slots; ++slot) {
            ItemStack stack = container.m_8020_(slot);
            hash = hash * 31 + (stack.m_41619_() ? 0 : System.identityHashCode(stack) * 67 + stack.m_41613_());
        }
        return hash;
    }

    private static int fingerprint(AbstractFurnaceBlockEntity furnace) {
        return FurnaceWatch.contentsFingerprint((Container)furnace, 3);
    }

    public static FurnaceContentsPayload contents(BlockPos pos, AbstractFurnaceBlockEntity furnace) {
        ArrayList<ItemStack> items = new ArrayList<ItemStack>(3);
        boolean any = false;
        for (int slot = 0; slot < 3; ++slot) {
            ItemStack stack = furnace.m_8020_(slot);
            any |= !stack.m_41619_();
            items.add(stack.m_41777_());
        }
        return new FurnaceContentsPayload(pos, any ? items : List.of());
    }

    public static void sync(ServerLevel level, BlockPos pos, AbstractFurnaceBlockEntity furnace) {
        if (furnace instanceof Fingerprinted) {
            Fingerprinted marked = (Fingerprinted)furnace;
            marked.cinematic$setFingerprint(FurnaceWatch.fingerprint(furnace));
        }
        Network.sendToTrackingChunk(level, new ChunkPos(pos), FurnaceWatch.contents(pos, furnace));
    }

    public static void chunkSent(ServerPlayer player, LevelChunk chunk) {
        for (BlockEntity entity : chunk.m_62954_().values()) {
            FurnaceContentsPayload payload;
            if (!(entity instanceof AbstractFurnaceBlockEntity)) continue;
            AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity)entity;
            if (!FurnaceWatch.shown(entity.m_58900_()) || (payload = FurnaceWatch.contents(entity.m_58899_(), furnace)).items().isEmpty()) continue;
            Network.sendToPlayer(player, payload);
        }
    }

    public static interface Fingerprinted {
        public int cinematic$fingerprint();

        public void cinematic$setFingerprint(int var1);
    }
}


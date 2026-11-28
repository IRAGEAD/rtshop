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
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BrewingStandBlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.chunk.LevelChunk
 */
package dev.dyslexic.cinematic.brewing;

import dev.dyslexic.cinematic.furnace.FurnaceWatch;
import dev.dyslexic.cinematic.net.BrewingContentsPayload;
import dev.dyslexic.cinematic.net.Network;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

public final class BrewingWatch {
    public static final int SLOTS = 5;
    public static final int BOTTLES = 3;

    private BrewingWatch() {
    }

    public static void afterTick(Level level, BlockPos pos, BlockState state, BrewingStandBlockEntity stand) {
        ServerLevel server;
        block5: {
            block4: {
                if (!(level instanceof ServerLevel)) break block4;
                server = (ServerLevel)level;
                if (stand instanceof Fingerprinted) break block5;
            }
            return;
        }
        Fingerprinted marked = (Fingerprinted)stand;
        int fingerprint = BrewingWatch.fingerprint(stand);
        if (fingerprint != marked.cinematic$fingerprint()) {
            marked.cinematic$setFingerprint(fingerprint);
            Network.sendToTrackingChunk(server, new ChunkPos(pos), BrewingWatch.contents(pos, stand));
        }
    }

    private static int fingerprint(BrewingStandBlockEntity stand) {
        return FurnaceWatch.contentsFingerprint((Container)stand, 5);
    }

    public static BrewingContentsPayload contents(BlockPos pos, BrewingStandBlockEntity stand) {
        ArrayList<ItemStack> items = new ArrayList<ItemStack>(5);
        boolean any = false;
        for (int slot = 0; slot < 5; ++slot) {
            ItemStack stack = stand.m_8020_(slot);
            any |= !stack.m_41619_();
            items.add(stack.m_41777_());
        }
        return new BrewingContentsPayload(pos, any ? items : List.of());
    }

    public static void chunkSent(ServerPlayer player, LevelChunk chunk) {
        for (BlockEntity entity : chunk.m_62954_().values()) {
            if (!(entity instanceof BrewingStandBlockEntity)) continue;
            BrewingStandBlockEntity stand = (BrewingStandBlockEntity)entity;
            BrewingContentsPayload payload = BrewingWatch.contents(entity.m_58899_(), stand);
            if (payload.items().isEmpty()) continue;
            Network.sendToPlayer(player, payload);
        }
    }

    public static interface Fingerprinted {
        public int cinematic$fingerprint();

        public void cinematic$setFingerprint(int var1);
    }
}


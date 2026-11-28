/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ChunkPos
 *  net.minecraft.world.phys.Vec3
 */
package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.net.MenuDisplayPayload;
import dev.dyslexic.cinematic.net.MenuFxPayload;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

public final class ClientMenus {
    public static final Map<BlockPos, Entry> BLOCKS = new HashMap<BlockPos, Entry>();
    public static final Map<Integer, Double> MOVE_BY_ENTITY = new HashMap<Integer, Double>();

    private ClientMenus() {
    }

    public static void apply(MenuDisplayPayload payload) {
        if (payload.items().stream().allMatch(ItemStack::m_41619_)) {
            Entry entry = BLOCKS.get(payload.pos());
            if (entry != null && (Scene.clock() - entry.fxAt < 2.0 || Scene.clock() - entry.moveAt < 3.0)) {
                entry.items = List.of();
            } else {
                BLOCKS.remove(payload.pos());
            }
            return;
        }
        Entry entry = BLOCKS.computeIfAbsent(payload.pos().m_7949_(), p -> new Entry());
        entry.kind = payload.kind();
        entry.side = Direction.m_122407_((int)payload.side());
        entry.items = payload.items();
    }

    public static void fx(MenuFxPayload payload) {
        Entry entry = BLOCKS.computeIfAbsent(payload.pos().m_7949_(), p -> new Entry());
        entry.kind = payload.kind();
        entry.fxAt = Scene.clock();
    }

    public static void move(MenuFxPayload payload) {
        Entry entry = BLOCKS.computeIfAbsent(payload.pos().m_7949_(), p -> new Entry());
        entry.kind = payload.kind();
        entry.moveAt = Scene.clock();
        entry.moveItems = payload.items();
        entry.moveResult = payload.result();
        entry.moveToBag = payload.variant() == 1;
        entry.moveBy = payload.entityId();
        MOVE_BY_ENTITY.put(payload.entityId(), Scene.clock());
    }

    public static double sinceMove(int entityId) {
        Double at = MOVE_BY_ENTITY.get(entityId);
        return at == null ? 1.0E9 : Scene.clock() - at;
    }

    public static void forgetEntity(int entityId) {
        MOVE_BY_ENTITY.remove(entityId);
    }

    public static void unloadChunk(ChunkPos chunk) {
        BLOCKS.keySet().removeIf(pos -> pos.m_123341_() >> 4 == chunk.f_45578_ && pos.m_123343_() >> 4 == chunk.f_45579_);
    }

    public static void clear() {
        BLOCKS.clear();
        MOVE_BY_ENTITY.clear();
    }

    public static final class Entry {
        public StationKind kind;
        public Direction side = Direction.SOUTH;
        public List<ItemStack> items = List.of();
        public double fxAt = -10.0;
        public double moveAt = -10.0;
        public List<ItemStack> moveItems = List.of();
        public ItemStack moveResult = ItemStack.f_41583_;
        public boolean moveToBag;
        public int moveBy = -1;
        public double spin;
        public double run;

        public ItemStack moved(int slot) {
            return slot < this.moveItems.size() ? this.moveItems.get(slot) : ItemStack.f_41583_;
        }

        public Vec3 taker(Vec3 fallback) {
            Entity who;
            Minecraft mc = Minecraft.m_91087_();
            Entity entity = who = mc.f_91073_ == null ? null : mc.f_91073_.m_6815_(this.moveBy);
            if (who == null) {
                return fallback;
            }
            Vec3 eye = who.m_20299_(mc.m_91296_());
            return this.moveToBag ? eye.m_82520_(0.0, 0.25, 0.0) : eye.m_82520_(0.0, -0.55, 0.0);
        }
    }
}


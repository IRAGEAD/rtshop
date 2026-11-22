package com.rtshop.land;

import com.rtshop.net.BorderPacket;
import com.rtshop.net.ShopNet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 토지 경계 표시: 입자 대신 청크 경계(F3+G)처럼 선으로 그리도록 모양을 모아서 클라이언트로 보낸다.
 * 서버가 10틱마다 그 플레이어에게 보여 줄 모양을 모두 다시 모아 보낸다 (클라이언트는 잠깐 동안만 그림).
 */
public final class Borders {
    private Borders() {
    }

    public static final class Shape {
        public int x0, z0, x1, z1, y0, y1, color;
        /** 잘라낸 쪽 (1 서, 2 동, 4 북, 8 남): 그쪽은 진짜 경계가 아니므로 선을 긋지 않는다. */
        public int open;
        /** null = 네모 전체. */
        public BitSet mask;
    }

    private static final Map<UUID, List<Shape>> PENDING = new HashMap<>();
    private static final Set<UUID> SHOWING = new HashSet<>();
    /** 플레이어 주변 이만큼만 (멀리 있는 칸 모양은 잘라서 보낸다). */
    private static final int RANGE = 64;

    public static int rgb(org.joml.Vector3f c) {
        return ((int) (c.x * 255) << 16) | ((int) (c.y * 255) << 8) | (int) (c.z * 255);
    }

    public static void box(ServerPlayer p, int x0, int y0, int z0, int x1, int y1, int z1, int color) {
        Shape s = new Shape();
        s.x0 = x0;
        s.y0 = y0;
        s.z0 = z0;
        s.x1 = x1;
        s.y1 = y1;
        s.z1 = z1;
        s.color = color;
        PENDING.computeIfAbsent(p.getUUID(), k -> new ArrayList<>()).add(s);
    }

    /** 칸 모양 (플레이어 주변만 잘라서). */
    public static void columns(ServerPlayer p, PlotManager.ColumnTest t, int x0, int z0, int x1, int z1, int y0, int y1, int color) {
        int px = p.getBlockX(), pz = p.getBlockZ();
        int ax0 = Math.max(x0, px - RANGE), ax1 = Math.min(x1, px + RANGE);
        int az0 = Math.max(z0, pz - RANGE), az1 = Math.min(z1, pz + RANGE);
        if (ax1 < ax0 || az1 < az0) return;
        Shape s = new Shape();
        s.x0 = ax0;
        s.z0 = az0;
        s.x1 = ax1;
        s.z1 = az1;
        s.y0 = y0;
        s.y1 = y1;
        s.color = color;
        int w = ax1 - ax0 + 1;
        BitSet m = new BitSet(w * (az1 - az0 + 1));
        boolean full = true;
        for (int z = az0; z <= az1; z++) {
            for (int x = ax0; x <= ax1; x++) {
                if (t.has(x, z)) m.set((z - az0) * w + (x - ax0));
                else full = false;
            }
        }
        if (m.isEmpty()) return;
        s.mask = full ? null : m;
        s.open = (ax0 > x0 ? 1 : 0) | (ax1 < x1 ? 2 : 0) | (az0 > z0 ? 4 : 0) | (az1 < z1 ? 8 : 0);
        PENDING.computeIfAbsent(p.getUUID(), k -> new ArrayList<>()).add(s);
    }

    public static void claim(ServerPlayer p, LandData.Claim c, int color) {
        if (c.mask == null && Math.abs(p.getBlockX() - (c.x0 + c.x1) / 2) < 400 && Math.abs(p.getBlockZ() - (c.z0 + c.z1) / 2) < 400) {
            box(p, c.x0, c.y0, c.z0, c.x1, c.y1, c.z1, color);
        } else {
            columns(p, c::hasColumn, c.x0, c.z0, c.x1, c.z1, c.y0, c.y1, color);
        }
    }

    /** 모은 모양을 보낸다. 보여 줄 것이 없으면 (전에 보여 주던 것이 있을 때만) 지우라고 보낸다. */
    public static void flush(ServerPlayer p) {
        List<Shape> list = PENDING.remove(p.getUUID());
        if (list == null || list.isEmpty()) {
            if (SHOWING.remove(p.getUUID())) {
                send(p, List.of());
            }
            return;
        }
        SHOWING.add(p.getUUID());
        send(p, list);
    }

    private static void send(ServerPlayer p, List<Shape> list) {
        try {
            ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new BorderPacket(list));
        } catch (Exception ignored) {
        }
    }

    public static void forget(UUID id) {
        PENDING.remove(id);
        SHOWING.remove(id);
    }
}

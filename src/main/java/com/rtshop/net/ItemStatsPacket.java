package com.rtshop.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 장비 능력치 설정: 서버 -> 클라이언트 (CONFIG 설정 전체, OPEN 편집기 열기) / 클라이언트 -> 서버 (SAVE 한 아이템 저장). */
public final class ItemStatsPacket {
    public static final int CONFIG = 0, OPEN = 1, SAVE = 2;
    public final int kind;
    public final String item;
    public final String json;

    public ItemStatsPacket(int kind, String item, String json) {
        this.kind = kind;
        this.item = item;
        this.json = json;
    }

    public static void encode(ItemStatsPacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.kind);
        buf.writeUtf(m.item, 256);
        buf.writeUtf(m.json, 1 << 20);
    }

    public static ItemStatsPacket decode(FriendlyByteBuf buf) {
        return new ItemStatsPacket(buf.readVarInt(), buf.readUtf(256), buf.readUtf(1 << 20));
    }

    public static void handle(ItemStatsPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        if (m.kind == SAVE) {
            var p = c.getSender();
            c.enqueueWork(() -> {
                if (p != null) com.rtshop.stats.ItemStats.saveFromEditor(p, m.item, m.json);
            });
        } else {
            c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                com.rtshop.stats.ItemStats.setClient(m.json);
                if (m.kind == OPEN) com.rtshop.client.ItemStatsScreen.open(m.item);
                else com.rtshop.client.ItemStatsScreen.refresh();
            }));
        }
        c.setPacketHandled(true);
    }
}

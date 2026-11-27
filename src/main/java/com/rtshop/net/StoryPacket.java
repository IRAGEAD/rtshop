package com.rtshop.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 서버 -> 클라이언트: 이야기 연출 (CINEMA: 장 제목·보스 등장·환영·자막) / LORE: 아이템 이야기 목록. */
public final class StoryPacket {
    public static final int CINEMA = 0, LORE = 1, GATE = 2, DIALOGUE = 3, BOUNTY = 4, SPOTS = 5, NOTEBOOK = 6, MUSIC = 7, GRADES = 8, READ = 9, RANKS = 10, TITLES = 11, CODEX = 12, CODEX_MOB = 13, GUIDE = 14, ITEM_NOTES = 15, MAP_MARKS = 16, PETITIONS = 17, NPC_LOOKS = 18, BOSS_CAM = 19, STOCKS = 20;
    public final int kind;
    public final String json;

    public StoryPacket(int kind, String json) {
        this.kind = kind;
        this.json = json;
    }

    public static void encode(StoryPacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.kind);
        buf.writeUtf(m.json, 1 << 20);
    }

    public static StoryPacket decode(FriendlyByteBuf buf) {
        return new StoryPacket(buf.readVarInt(), buf.readUtf(1 << 20));
    }

    public static void handle(StoryPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.rtshop.client.StoryClient.receive(m.kind, m.json)));
        ctx.get().setPacketHandled(true);
    }
}

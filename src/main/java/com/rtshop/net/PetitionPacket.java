package com.rtshop.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 클라이언트 -> 서버: 민원 창에서 한 일. 0 = 접수(arg = 분류 번호, text = 글) / 1 = 거두기(arg = 민원 번호) / 2 = 다시 보기. */
public final class PetitionPacket {
    private final int action;
    private final int arg;
    private final String text;

    public PetitionPacket(int action, int arg, String text) {
        this.action = action;
        this.arg = arg;
        this.text = text == null ? "" : text;
    }

    public static void encode(PetitionPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.action);
        buf.writeVarInt(msg.arg);
        buf.writeUtf(msg.text, 2048);
    }

    public static PetitionPacket decode(FriendlyByteBuf buf) {
        int action = buf.readVarInt();
        int arg = buf.readVarInt();
        return new PetitionPacket(action, arg, buf.readUtf(2048));
    }

    public static void handle(PetitionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer sender = c.getSender();
            if (sender != null) {
                com.rtshop.petition.Petitions.fromScreen(sender, msg.action, msg.arg, msg.text);
            }
        });
        c.setPacketHandled(true);
    }
}

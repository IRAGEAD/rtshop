package com.rtshop.net;

import com.rtshop.client.ClientHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 서버 -> 클라이언트: 출석 창(VIEW, 내 진행도와 보상 표) 또는 출석 편집기(EDITOR, attendance.json)를 연다. */
public final class AttendancePacket {
    public static final int VIEW = 0;
    public static final int EDITOR = 1;

    public final int mode;
    public final String json;

    public AttendancePacket(int mode, String json) {
        this.mode = mode;
        this.json = json;
    }

    public static void encode(AttendancePacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.mode);
        buf.writeUtf(m.json, 262_144);
    }

    public static AttendancePacket decode(FriendlyByteBuf buf) {
        return new AttendancePacket(buf.readVarInt(), buf.readUtf(262_144));
    }

    public static void handle(AttendancePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openAttendance(msg)));
        ctx.get().setPacketHandled(true);
    }
}

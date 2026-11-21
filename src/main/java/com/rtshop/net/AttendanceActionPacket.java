package com.rtshop.net;

import com.rtshop.attendance.Attendance;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 클라이언트 -> 서버: 출석 창의 버튼 (출석하기 / 복구권 사용 / 출석 상점 / 편집기 열기 / 편집 저장). */
public final class AttendanceActionPacket {
    public static final int CHECK_IN = 0;
    public static final int RESTORE = 1;
    public static final int SHOP = 2;
    public static final int EDIT = 3;
    public static final int SAVE = 4;

    public final int action;
    public final String arg;

    public AttendanceActionPacket(int action, String arg) {
        this.action = action;
        this.arg = arg == null ? "" : arg;
    }

    public static void encode(AttendanceActionPacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.action);
        buf.writeUtf(m.arg, 262_144);
    }

    public static AttendanceActionPacket decode(FriendlyByteBuf buf) {
        return new AttendanceActionPacket(buf.readVarInt(), buf.readUtf(262_144));
    }

    public static void handle(AttendanceActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer p = ctx.get().getSender();
            if (p != null) {
                Attendance.handleAction(p, msg.action, msg.arg);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

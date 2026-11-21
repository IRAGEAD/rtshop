package com.rtshop.net;

import com.rtshop.client.ClientHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 서버 -> 클라이언트: 뽑기 창(VIEW), 뽑기 결과(RESULT), 편집기(EDITOR, gacha.json), 자동 분류 목록(AUTOFILL), 모의 뽑기 결과(SIM). */
public final class GachaPacket {
    public static final int VIEW = 0;
    public static final int EDITOR = 1;
    public static final int RESULT = 2;
    public static final int AUTOFILL = 3;
    public static final int SIM = 4;

    public final int mode;
    public final String json;

    public GachaPacket(int mode, String json) {
        this.mode = mode;
        this.json = json;
    }

    public static void encode(GachaPacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.mode);
        Packed.write(buf, m.json);
    }

    public static GachaPacket decode(FriendlyByteBuf buf) {
        return new GachaPacket(buf.readVarInt(), Packed.read(buf));
    }

    public static void handle(GachaPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openGacha(msg)));
        ctx.get().setPacketHandled(true);
    }
}

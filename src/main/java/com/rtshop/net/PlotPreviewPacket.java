package com.rtshop.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 서버 -> 클라이언트: 구획 둘레를 위에서 본 지도 (색은 압축, 칸 표시는 그대로). */
public final class PlotPreviewPacket {
    public final int landId, x0, z0, step, w, h;
    public final byte[] rgb;
    public final byte[] flags;
    /** true = 넓은 지도 (확대·이동용), false = 토지 둘레 미리보기. */
    public final boolean area;

    public PlotPreviewPacket(int landId, int x0, int z0, int step, int w, int h, byte[] rgb, byte[] flags, boolean area) {
        this.area = area;
        this.landId = landId;
        this.x0 = x0;
        this.z0 = z0;
        this.step = step;
        this.w = w;
        this.h = h;
        this.rgb = rgb;
        this.flags = flags;
    }

    public static void encode(PlotPreviewPacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.landId);
        buf.writeInt(m.x0);
        buf.writeInt(m.z0);
        buf.writeVarInt(m.step);
        buf.writeVarInt(m.w);
        buf.writeVarInt(m.h);
        buf.writeByteArray(m.rgb);
        buf.writeByteArray(m.flags);
        buf.writeBoolean(m.area);
    }

    public static PlotPreviewPacket decode(FriendlyByteBuf buf) {
        return new PlotPreviewPacket(buf.readVarInt(), buf.readInt(), buf.readInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readByteArray(1 << 20), buf.readByteArray(1 << 16), buf.readBoolean());
    }

    public static void handle(PlotPreviewPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.rtshop.client.PlotBrowserScreen.onPreview(msg)));
        ctx.get().setPacketHandled(true);
    }
}

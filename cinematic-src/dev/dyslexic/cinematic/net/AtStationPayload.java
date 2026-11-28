/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.FriendlyByteBuf
 */
package dev.dyslexic.cinematic.net;

import dev.dyslexic.cinematic.net.Payload;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record AtStationPayload(int entityId, Optional<BlockPos> pos, StationKind kind) implements Payload
{
    @Override
    public void write(FriendlyByteBuf buf) {
        buf.m_130130_(this.entityId);
        buf.m_236835_(this.pos, FriendlyByteBuf::m_130064_);
        buf.m_130068_((Enum)this.kind);
    }

    public static AtStationPayload read(FriendlyByteBuf buf) {
        return new AtStationPayload(buf.m_130242_(), buf.m_236860_(FriendlyByteBuf::m_130135_), (StationKind)buf.m_130066_(StationKind.class));
    }
}


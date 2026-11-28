/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.world.item.ItemStack
 */
package dev.dyslexic.cinematic.net;

import dev.dyslexic.cinematic.net.Payload;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public record MenuDisplayPayload(BlockPos pos, StationKind kind, int side, List<ItemStack> items) implements Payload
{
    @Override
    public void write(FriendlyByteBuf buf) {
        buf.m_130064_(this.pos);
        buf.m_130068_((Enum)this.kind);
        buf.m_130130_(this.side);
        buf.m_236828_(this.items, FriendlyByteBuf::m_130055_);
    }

    public static MenuDisplayPayload read(FriendlyByteBuf buf) {
        return new MenuDisplayPayload(buf.m_130135_(), (StationKind)buf.m_130066_(StationKind.class), buf.m_130242_(), buf.m_236845_(FriendlyByteBuf::m_130267_));
    }
}


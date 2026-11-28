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
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public record TableContentsPayload(BlockPos pos, List<ItemStack> items, byte[] facing) implements Payload
{
    @Override
    public void write(FriendlyByteBuf buf) {
        buf.m_130064_(this.pos);
        buf.m_236828_(this.items, FriendlyByteBuf::m_130055_);
        buf.m_130087_(this.facing);
    }

    public static TableContentsPayload read(FriendlyByteBuf buf) {
        return new TableContentsPayload(buf.m_130135_(), buf.m_236845_(FriendlyByteBuf::m_130267_), buf.m_130052_());
    }
}


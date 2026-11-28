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
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public record EnchantDisplayPayload(BlockPos pos, ItemStack item, int lapis, int side) implements Payload
{
    @Override
    public void write(FriendlyByteBuf buf) {
        buf.m_130064_(this.pos);
        buf.m_130055_(this.item);
        buf.m_130130_(this.lapis);
        buf.m_130130_(this.side);
    }

    public static EnchantDisplayPayload read(FriendlyByteBuf buf) {
        return new EnchantDisplayPayload(buf.m_130135_(), buf.m_130267_(), buf.m_130242_(), buf.m_130242_());
    }
}


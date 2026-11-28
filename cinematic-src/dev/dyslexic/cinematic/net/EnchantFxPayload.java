/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.FriendlyByteBuf
 */
package dev.dyslexic.cinematic.net;

import dev.dyslexic.cinematic.net.Payload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record EnchantFxPayload(BlockPos pos, int entityId) implements Payload
{
    @Override
    public void write(FriendlyByteBuf buf) {
        buf.m_130064_(this.pos);
        buf.m_130130_(this.entityId);
    }

    public static EnchantFxPayload read(FriendlyByteBuf buf) {
        return new EnchantFxPayload(buf.m_130135_(), buf.m_130242_());
    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.world.phys.AABB
 */
package com.finderfeed.fdbosses.content.util;

import com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.AABB;

public class BossByteBufCodecs {
    public static NetworkCodec<AABB> AABB = new NetworkCodec<AABB>(){

        public AABB fromNetwork(FriendlyByteBuf buf) {
            return new AABB(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble());
        }

        public void toNetwork(FriendlyByteBuf b, AABB aabb) {
            b.writeDouble(aabb.f_82288_);
            b.writeDouble(aabb.f_82289_);
            b.writeDouble(aabb.f_82290_);
            b.writeDouble(aabb.f_82291_);
            b.writeDouble(aabb.f_82292_);
            b.writeDouble(aabb.f_82293_);
        }
    };
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.network.FDPacket
 *  com.finderfeed.fdlib.network.FDPacketHandler
 *  com.finderfeed.fdlib.network.RegisterFDPacket
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.EntityType
 *  net.minecraftforge.network.NetworkDirection
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.finderfeed.fdbosses.packets;

import com.finderfeed.fdbosses.packets.OpenBossDossierEntityTypePacket;
import com.finderfeed.fdlib.network.FDPacket;
import com.finderfeed.fdlib.network.FDPacketHandler;
import com.finderfeed.fdlib.network.RegisterFDPacket;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

@RegisterFDPacket(value="fdbosses:request_dossier_screen")
public class RequestDossierScreenPacket
extends FDPacket {
    private EntityType<?> entityType;

    public RequestDossierScreenPacket(FriendlyByteBuf buf) {
        this.entityType = (EntityType)BuiltInRegistries.f_256780_.m_7745_(buf.m_130281_());
    }

    public RequestDossierScreenPacket(EntityType<?> bossEntityType) {
        this.entityType = bossEntityType;
    }

    public void write(FriendlyByteBuf registryFriendlyByteBuf) {
        ResourceLocation key = BuiltInRegistries.f_256780_.m_7981_(this.entityType);
        registryFriendlyByteBuf.m_130085_(key);
    }

    public void clientAction(Supplier<NetworkEvent.Context> supplier) {
    }

    public void serverAction(Supplier<NetworkEvent.Context> supplier) {
        if (this.entityType != null) {
            ServerPlayer player = supplier.get().getSender();
            FDPacketHandler.INSTANCE.sendTo((Object)new OpenBossDossierEntityTypePacket(player.m_9236_(), this.entityType), player.f_8906_.f_9742_, NetworkDirection.PLAY_TO_CLIENT);
        }
    }
}


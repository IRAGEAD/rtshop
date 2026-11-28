/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.network.FDPacket
 *  com.finderfeed.fdlib.network.RegisterFDPacket
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.world.entity.player.Player
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.finderfeed.fdbosses.content.items.chesed;

import com.finderfeed.fdbosses.BossClientPackets;
import com.finderfeed.fdlib.network.FDPacket;
import com.finderfeed.fdlib.network.RegisterFDPacket;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

@RegisterFDPacket(value="fdbosses:update_player_abilities_packet")
public class PhaseSpherePacket
extends FDPacket {
    private CompoundTag abilities;
    private boolean noPhysics;
    private boolean startedUsing;

    public PhaseSpherePacket(Player player, boolean startedUsing) {
        CompoundTag tag = new CompoundTag();
        player.m_150110_().m_35945_(tag);
        this.abilities = tag;
        this.noPhysics = player.f_19794_;
        this.startedUsing = startedUsing;
    }

    public PhaseSpherePacket(FriendlyByteBuf buf) {
        this.abilities = buf.m_130260_();
        this.noPhysics = buf.readBoolean();
        this.startedUsing = buf.readBoolean();
    }

    public void write(FriendlyByteBuf registryFriendlyByteBuf) {
        registryFriendlyByteBuf.m_130079_(this.abilities);
        registryFriendlyByteBuf.writeBoolean(this.noPhysics);
        registryFriendlyByteBuf.writeBoolean(this.startedUsing);
    }

    public void clientAction(Supplier<NetworkEvent.Context> supplier) {
        BossClientPackets.chesedItemUse(this.abilities, this.noPhysics, this.startedUsing);
    }

    public void serverAction(Supplier<NetworkEvent.Context> supplier) {
    }
}


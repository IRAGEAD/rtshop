/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.network.FDPacket
 *  com.finderfeed.fdlib.network.RegisterFDPacket
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.player.Player
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.finderfeed.fdbosses.content.entities.geburah.sins;

import com.finderfeed.fdbosses.BossUtil;
import com.finderfeed.fdbosses.content.entities.geburah.sins.PlayerSinsHandler;
import com.finderfeed.fdbosses.content.entities.geburah.sins.attachment.ActivePlayerSinInstance;
import com.finderfeed.fdbosses.content.entities.geburah.sins.attachment.PlayerSins;
import com.finderfeed.fdbosses.init.GeburahSins;
import com.finderfeed.fdlib.network.FDPacket;
import com.finderfeed.fdlib.network.RegisterFDPacket;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

@RegisterFDPacket(value="fdbosses:player_pressed_button")
public class HandleButtonPressSinPacket
extends FDPacket {
    public HandleButtonPressSinPacket() {
    }

    public HandleButtonPressSinPacket(FriendlyByteBuf buf) {
    }

    public void write(FriendlyByteBuf registryFriendlyByteBuf) {
    }

    public void clientAction(Supplier<NetworkEvent.Context> supplier) {
    }

    public void serverAction(Supplier<NetworkEvent.Context> supplier) {
        ServerPlayer player = supplier.get().getSender();
        PlayerSins sins = PlayerSins.getPlayerSins((Player)player);
        if (BossUtil.isPlayerInSurvival((Player)player) && sins.hasSinActive(GeburahSins.PRESSED_TOO_MANY_BUTTONS_SIN.get()) && !sins.isGainingSinsOnCooldown()) {
            ActivePlayerSinInstance sin = sins.getSin(GeburahSins.PRESSED_TOO_MANY_BUTTONS_SIN.get());
            int pressedTimes = sin.getCustomData();
            if (pressedTimes + 1 > 5) {
                PlayerSinsHandler.sin(player, 0);
            } else {
                sin.setCustomData(pressedTimes + 1);
                PlayerSins.setPlayerSins((Player)player, sins);
            }
        }
    }
}


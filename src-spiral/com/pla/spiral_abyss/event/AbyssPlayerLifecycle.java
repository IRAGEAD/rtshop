/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.player.Player
 */
package com.pla.spiral_abyss.event;

import com.pla.spiral_abyss.encounter.QliphothEncounters;
import com.pla.spiral_abyss.platform.event.AbyssEvents;
import com.pla.spiral_abyss.world.AbyssParties;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class AbyssPlayerLifecycle {
    private AbyssPlayerLifecycle() {
    }

    public static void login(AbyssEvents.PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer) {
            ServerPlayer player2 = (ServerPlayer)player;
            QliphothEncounters.clearPlayer(player2);
            AbyssParties.get(player2.f_8924_).loggedIn(player2);
        }
    }

    public static void logout(AbyssEvents.PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer) {
            ServerPlayer player2 = (ServerPlayer)player;
            QliphothEncounters.clearPlayer(player2);
            AbyssParties.disconnect(player2);
        }
    }
}


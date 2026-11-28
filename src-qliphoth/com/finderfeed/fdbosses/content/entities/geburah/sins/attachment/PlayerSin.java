/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.entity.player.Player
 */
package com.finderfeed.fdbosses.content.entities.geburah.sins.attachment;

import com.finderfeed.fdbosses.content.entities.geburah.sins.attachment.ActivePlayerSinInstance;
import net.minecraft.world.entity.player.Player;

public abstract class PlayerSin {
    public abstract void playerTick(Player var1, ActivePlayerSinInstance var2);

    public abstract void onSinAdded(Player var1, ActivePlayerSinInstance var2);
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.network.FDPacketHandler
 *  net.minecraft.util.Mth
 *  net.minecraftforge.network.PacketDistributor
 */
package com.finderfeed.fdbosses.content.entities.geburah;

import com.finderfeed.fdbosses.content.entities.geburah.GeburahEntity;
import com.finderfeed.fdbosses.content.entities.geburah.GeburahPrepareAttackPacket;
import com.finderfeed.fdlib.network.FDPacketHandler;
import net.minecraft.util.Mth;
import net.minecraftforge.network.PacketDistributor;

public class GeburahLaserAttackPreparator {
    public static final int FADE_OUT = 10;
    public GeburahEntity geburah;
    public int time = -1;
    public int currentTime = -1;

    public GeburahLaserAttackPreparator(GeburahEntity geburah) {
        this.geburah = geburah;
    }

    public void launchPreparation(int timeUntilAttack) {
        if (this.geburah.m_9236_().f_46443_) {
            this.currentTime = this.time = timeUntilAttack + 10;
        } else {
            FDPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> this.geburah), (Object)new GeburahPrepareAttackPacket(this.geburah, timeUntilAttack));
        }
    }

    public void tick() {
        this.currentTime = Mth.m_14045_((int)(this.currentTime - 1), (int)0, (int)this.time);
        if (this.currentTime == 0) {
            this.time = -1;
            this.currentTime = -1;
        }
    }
}


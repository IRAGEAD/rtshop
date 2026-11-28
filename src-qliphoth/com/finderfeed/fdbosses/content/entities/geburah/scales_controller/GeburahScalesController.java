/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.nbt.AutoSerializable
 *  com.finderfeed.fdlib.nbt.SerializableField
 *  com.finderfeed.fdlib.network.FDPacketHandler
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.util.Mth
 *  net.minecraft.world.level.Level
 *  net.minecraftforge.network.PacketDistributor
 */
package com.finderfeed.fdbosses.content.entities.geburah.scales_controller;

import com.finderfeed.fdbosses.content.entities.geburah.GeburahEntity;
import com.finderfeed.fdbosses.content.entities.geburah.scales_controller.GeburahScalesControllerSetDisplacement;
import com.finderfeed.fdlib.nbt.AutoSerializable;
import com.finderfeed.fdlib.nbt.SerializableField;
import com.finderfeed.fdlib.network.FDPacketHandler;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

public class GeburahScalesController
implements AutoSerializable {
    public static final int MAX_DISPLACEMENT = 5;
    @SerializableField
    private int currentDisplacement = 0;
    private int oldDisplacement = 0;
    private int displacementTime = -1;
    private int displacementMaxTime = -1;
    public GeburahEntity geburah;

    public GeburahScalesController(GeburahEntity geburah) {
        this.geburah = geburah;
    }

    public void tick() {
        Level level = this.geburah.m_9236_();
        if (level.f_46443_) {
            this.clientTick();
        }
    }

    private void clientTick() {
        if (this.displacementTime == -1 || this.displacementMaxTime == -1) {
            this.oldDisplacement = this.currentDisplacement;
        } else {
            ++this.displacementTime;
            if (this.displacementTime >= this.displacementMaxTime) {
                this.displacementTime = -1;
                this.displacementMaxTime = -1;
                this.oldDisplacement = this.currentDisplacement;
            }
        }
    }

    public float getClientDisplacementAngle(float pticks) {
        float anglePerDisplacement = 5.0f;
        if (this.displacementTime == -1) {
            return (float)this.currentDisplacement * anglePerDisplacement;
        }
        float p = Mth.m_14036_((float)((float)this.displacementTime + pticks), (float)0.0f, (float)this.displacementMaxTime) / (float)this.displacementMaxTime;
        p = this.displacementMaxTime < 40 ? FDEasings.easeOutBack((float)p) : FDEasings.easeInOut((float)p);
        return FDMathUtil.lerp((float)this.oldDisplacement, (float)this.currentDisplacement, (float)p) * anglePerDisplacement;
    }

    public void setCurrentDisplacement(int currentDisplacement, int displacementTime) {
        currentDisplacement = Mth.m_14045_((int)currentDisplacement, (int)-5, (int)5);
        if (!this.geburah.m_9236_().f_46443_) {
            if (this.currentDisplacement != currentDisplacement) {
                FDPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> this.geburah), (Object)new GeburahScalesControllerSetDisplacement(this.geburah, currentDisplacement, displacementTime));
            }
        } else {
            if (this.displacementMaxTime != -1 || this.displacementTime != -1) {
                this.oldDisplacement = this.currentDisplacement;
            }
            this.displacementMaxTime = displacementTime;
            this.displacementTime = 0;
        }
        this.currentDisplacement = currentDisplacement;
    }

    public int getCurrentDisplacement() {
        return this.currentDisplacement;
    }

    public void syncToPlayer(ServerPlayer serverPlayer) {
        FDPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer), (Object)new GeburahScalesControllerSetDisplacement(this.geburah, this.currentDisplacement, 100));
    }
}


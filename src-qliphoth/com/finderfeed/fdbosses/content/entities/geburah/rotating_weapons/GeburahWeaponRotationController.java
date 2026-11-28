/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.network.FDPacketHandler
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.sounds.SoundSource
 *  net.minecraftforge.network.PacketDistributor
 */
package com.finderfeed.fdbosses.content.entities.geburah.rotating_weapons;

import com.finderfeed.fdbosses.content.entities.geburah.GeburahEntity;
import com.finderfeed.fdbosses.content.entities.geburah.rotating_weapons.GeburahWeaponRotationSyncPacket;
import com.finderfeed.fdbosses.content.entities.geburah.rotating_weapons.StopGeburahWeaponRotationPacket;
import com.finderfeed.fdbosses.content.entities.geburah.rotating_weapons.rotations.GeburahConstantWeaponRotation;
import com.finderfeed.fdbosses.content.entities.geburah.rotating_weapons.rotations.GeburahWeaponRotation;
import com.finderfeed.fdbosses.content.entities.geburah.rotating_weapons.rotations.GeburahWeaponsRotateTo;
import com.finderfeed.fdbosses.content.entities.geburah.rotating_weapons.rotations.StartGeburahWeaponRotationPacket;
import com.finderfeed.fdbosses.init.BossSounds;
import com.finderfeed.fdlib.network.FDPacketHandler;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.PacketDistributor;

public class GeburahWeaponRotationController {
    private GeburahEntity geburah;
    public float currentRotation = 0.0f;
    protected float oldRotation = 0.0f;
    private float rotationForSound = 0.0f;
    protected GeburahWeaponRotation weaponRotation;

    public GeburahWeaponRotationController(GeburahEntity geburah) {
        this.geburah = geburah;
    }

    public void tick() {
        this.oldRotation = this.currentRotation;
        if (this.weaponRotation != null) {
            this.weaponRotation.tick();
            if (this.weaponRotation.shouldPlayRotationSound()) {
                this.playRotationSound();
            }
            if (this.weaponRotation.finishedRotation()) {
                this.weaponRotation = null;
                this.trySendRotationSyncPacket();
            }
        }
    }

    private void playRotationSound() {
        if (!this.geburah.m_9236_().f_46443_) {
            this.rotationForSound += Math.abs(this.currentRotation - this.oldRotation);
            float distForSound = 20.0f;
            if (this.rotationForSound > distForSound) {
                this.rotationForSound %= distForSound;
                this.geburah.m_9236_().m_6263_(null, this.geburah.m_20185_(), this.geburah.m_20186_(), this.geburah.m_20189_(), BossSounds.GEBURAH_WEAPON_ROTATION_CLING.get(), SoundSource.HOSTILE, 3.0f, 1.0f);
            }
        }
    }

    public void rotateWeapons(GeburahWeaponRotation geburahWeaponRotation) {
        geburahWeaponRotation.setRotatingWeaponsHandler(this);
        if (!this.geburah.m_9236_().f_46443_) {
            if (this.weaponRotation != null) {
                this.trySendRotationSyncPacket();
            }
            FDPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> this.geburah), (Object)new StartGeburahWeaponRotationPacket(this.geburah, geburahWeaponRotation));
        }
        this.weaponRotation = geburahWeaponRotation;
    }

    public void rotateWeaponsBy(float rotationDelta, int rotationTime, boolean easeInOut) {
        this.rotateWeapons(new GeburahWeaponsRotateTo(this, this.currentRotation + rotationDelta, rotationTime, easeInOut));
    }

    public void rotateWeaponsBy(float rotationDelta, int rotationTime) {
        this.rotateWeaponsBy(rotationDelta, rotationTime, false);
    }

    public void startConstantRotation(float rotationSpeed) {
        GeburahConstantWeaponRotation constantWeaponRotation;
        float currentRotSpeed;
        GeburahWeaponRotation geburahWeaponRotation = this.weaponRotation;
        if (geburahWeaponRotation instanceof GeburahConstantWeaponRotation && (currentRotSpeed = (constantWeaponRotation = (GeburahConstantWeaponRotation)geburahWeaponRotation).getRotationSpeed()) == rotationSpeed) {
            return;
        }
        this.rotateWeapons(new GeburahConstantWeaponRotation(rotationSpeed));
    }

    public void stopRotation() {
        this.weaponRotation = null;
        if (!this.geburah.m_9236_().f_46443_) {
            FDPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> this.geburah), (Object)new StopGeburahWeaponRotationPacket(this.geburah));
            this.trySendRotationSyncPacket();
        }
    }

    public float getCurrentRotation() {
        return this.currentRotation;
    }

    public float getOldRotation() {
        return this.oldRotation;
    }

    public void setRotation(float rotation) {
        this.oldRotation = this.currentRotation;
        this.currentRotation = rotation;
        this.trySendRotationSyncPacket();
    }

    public float getLerpedRotation(float pticks) {
        return FDMathUtil.lerp((float)this.oldRotation, (float)this.currentRotation, (float)pticks);
    }

    public GeburahWeaponRotation getWeaponRotation() {
        return this.weaponRotation;
    }

    public boolean finishedRotation() {
        return this.weaponRotation == null || this.weaponRotation.finishedRotation();
    }

    public void onStartSeeingGeburah(ServerPlayer serverPlayer) {
        GeburahWeaponRotationSyncPacket syncPacket = new GeburahWeaponRotationSyncPacket(this.geburah);
        FDPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer), (Object)syncPacket);
        if (this.weaponRotation != null) {
            StartGeburahWeaponRotationPacket geburahWeaponRotationPacket = new StartGeburahWeaponRotationPacket(this.geburah, this.weaponRotation);
            FDPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer), (Object)geburahWeaponRotationPacket);
        }
    }

    private void trySendRotationSyncPacket() {
        if (!this.geburah.m_9236_().f_46443_) {
            FDPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> this.geburah), (Object)new GeburahWeaponRotationSyncPacket(this.geburah));
        }
    }
}


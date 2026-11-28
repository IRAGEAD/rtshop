/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.network.FDPacket
 *  com.finderfeed.fdlib.network.RegisterFDPacket
 *  com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.finderfeed.fdbosses.content.entities.geburah.rotating_weapons.rotations;

import com.finderfeed.fdbosses.BossClientPackets;
import com.finderfeed.fdbosses.content.entities.geburah.GeburahEntity;
import com.finderfeed.fdbosses.content.entities.geburah.rotating_weapons.rotations.GeburahWeaponRotation;
import com.finderfeed.fdlib.network.FDPacket;
import com.finderfeed.fdlib.network.RegisterFDPacket;
import com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

@RegisterFDPacket(value="fdbosses:geburah_weapon_rotation")
public class StartGeburahWeaponRotationPacket
extends FDPacket {
    private GeburahWeaponRotation geburahWeaponRotation;
    private int entityId;

    public StartGeburahWeaponRotationPacket(GeburahEntity geburah, GeburahWeaponRotation geburahWeaponRotation) {
        this.geburahWeaponRotation = geburahWeaponRotation.rotationType().copyHandler.apply(geburahWeaponRotation);
        this.entityId = geburah.m_19879_();
    }

    public StartGeburahWeaponRotationPacket(FriendlyByteBuf buf) {
        GeburahWeaponRotation.Type e = (GeburahWeaponRotation.Type)buf.m_130066_(GeburahWeaponRotation.Type.class);
        this.geburahWeaponRotation = (GeburahWeaponRotation)e.codec.fromNetwork(buf);
        this.entityId = buf.readInt();
    }

    public void write(FriendlyByteBuf registryFriendlyByteBuf) {
        registryFriendlyByteBuf.m_130068_((Enum)this.geburahWeaponRotation.rotationType());
        this.useCodec(this.geburahWeaponRotation.rotationType().codec, registryFriendlyByteBuf, this.geburahWeaponRotation);
        registryFriendlyByteBuf.writeInt(this.entityId);
    }

    public void clientAction(Supplier<NetworkEvent.Context> supplier) {
        BossClientPackets.startGeburahWeaponRotation(this.entityId, this.geburahWeaponRotation);
    }

    public void serverAction(Supplier<NetworkEvent.Context> supplier) {
    }

    private <T extends GeburahWeaponRotation> void useCodec(NetworkCodec<T> codec, FriendlyByteBuf buf, GeburahWeaponRotation weaponRotation) {
        codec.toNetwork(buf, (Object)weaponRotation);
    }
}


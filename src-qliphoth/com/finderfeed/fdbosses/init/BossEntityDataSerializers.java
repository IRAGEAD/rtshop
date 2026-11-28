/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.resources.ResourceKey
 *  net.minecraftforge.registries.DeferredRegister
 *  net.minecraftforge.registries.ForgeRegistries$Keys
 *  net.minecraftforge.registries.RegistryObject
 */
package com.finderfeed.fdbosses.init;

import com.finderfeed.fdbosses.content.entities.chesed_boss.earthshatter_entity.EarthShatterEntityDataSerializer;
import com.finderfeed.fdbosses.content.entities.geburah.SinsEntityDataSerializer;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_slash.MalkuthSlashTypeSerializer;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BossEntityDataSerializers {
    public static final DeferredRegister<EntityDataSerializer<?>> ENTITY_DATA_SERIALIZERS = DeferredRegister.create((ResourceKey)ForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS, (String)"fdbosses");
    public static final RegistryObject<MalkuthSlashTypeSerializer> MALKUTH_ATTACK_TYPE = ENTITY_DATA_SERIALIZERS.register("malkuth_attack", MalkuthSlashTypeSerializer::new);
    public static final RegistryObject<EarthShatterEntityDataSerializer> EARTH_SHATTER_SETTINGS = ENTITY_DATA_SERIALIZERS.register("earthshatter_settings", EarthShatterEntityDataSerializer::new);
    public static final RegistryObject<SinsEntityDataSerializer> SINS = ENTITY_DATA_SERIALIZERS.register("sins", SinsEntityDataSerializer::new);
}


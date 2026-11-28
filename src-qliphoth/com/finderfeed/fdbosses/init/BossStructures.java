/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.world.level.levelgen.structure.StructureType
 *  net.minecraftforge.registries.DeferredRegister
 */
package com.finderfeed.fdbosses.init;

import com.finderfeed.fdbosses.content.structures.GeburahArenaStructure;
import com.finderfeed.fdbosses.content.structures.MalkuthArenaStructure;
import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraftforge.registries.DeferredRegister;

public class BossStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURES = DeferredRegister.create((ResourceKey)Registries.f_256938_, (String)"fdbosses");
    public static final Supplier<StructureType<MalkuthArenaStructure>> MALKUTH_ARENA = STRUCTURES.register("malkuth_arena", () -> new StructureType<MalkuthArenaStructure>(){

        public Codec<MalkuthArenaStructure> m_226884_() {
            return MalkuthArenaStructure.CODEC;
        }
    });
    public static final Supplier<StructureType<GeburahArenaStructure>> GEBURAH_ARENA = STRUCTURES.register("geburah_arena", () -> new StructureType<GeburahArenaStructure>(){

        public Codec<GeburahArenaStructure> m_226884_() {
            return GeburahArenaStructure.CODEC;
        }
    });
}


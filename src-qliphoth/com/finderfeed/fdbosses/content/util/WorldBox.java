/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.AABB
 */
package com.finderfeed.fdbosses.content.util;

import com.finderfeed.fdbosses.content.util.BossByteBufCodecs;
import com.finderfeed.fdbosses.content.util.BossCodecs;
import com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class WorldBox {
    public static NetworkCodec<WorldBox> STREAM_CODEC = NetworkCodec.composite((NetworkCodec)NetworkCodec.RESOURCE_LOCATION, v -> v.dimension.m_135782_(), BossByteBufCodecs.AABB, v -> v.box, (location, aabb) -> new WorldBox((ResourceKey<Level>)ResourceKey.m_135785_((ResourceKey)Registries.f_256858_, (ResourceLocation)location), (AABB)aabb));
    public static Codec<WorldBox> CODEC = RecordCodecBuilder.create(p -> p.group((App)Codec.STRING.fieldOf("dimension").forGetter(v -> v.dimension.m_135782_().toString()), (App)BossCodecs.AABB.fieldOf("box").forGetter(v -> v.box)).apply((Applicative)p, (location, aabb) -> new WorldBox((ResourceKey<Level>)ResourceKey.m_135785_((ResourceKey)Registries.f_256858_, (ResourceLocation)new ResourceLocation(location)), (AABB)aabb)));
    private final ResourceKey<Level> dimension;
    private final AABB box;

    public WorldBox(ResourceKey<Level> dimension, AABB box) {
        this.dimension = dimension;
        this.box = box;
    }

    public ResourceKey<Level> getDimension() {
        return this.dimension;
    }

    public AABB getBox() {
        return this.box;
    }

    public boolean isEntityInBox(Entity entity) {
        return entity.m_9236_().m_46472_().equals(this.dimension) && this.box.m_82390_(entity.m_20182_());
    }
}


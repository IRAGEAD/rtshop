/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  net.minecraft.world.phys.AABB
 */
package com.finderfeed.fdbosses.content.util;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.phys.AABB;

public class BossCodecs {
    public static final Codec<AABB> AABB = RecordCodecBuilder.create(p -> p.group((App)Codec.DOUBLE.fieldOf("minX").forGetter(v -> v.f_82288_), (App)Codec.DOUBLE.fieldOf("minY").forGetter(v -> v.f_82289_), (App)Codec.DOUBLE.fieldOf("minZ").forGetter(v -> v.f_82290_), (App)Codec.DOUBLE.fieldOf("maxX").forGetter(v -> v.f_82291_), (App)Codec.DOUBLE.fieldOf("maxY").forGetter(v -> v.f_82292_), (App)Codec.DOUBLE.fieldOf("maxZ").forGetter(v -> v.f_82293_)).apply((Applicative)p, AABB::new));
}


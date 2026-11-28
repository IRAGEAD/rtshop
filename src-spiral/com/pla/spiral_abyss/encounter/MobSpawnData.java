/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonParseException
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  net.minecraft.nbt.ByteTag
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.NumericTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.nbt.TagParser
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 */
package com.pla.spiral_abyss.encounter;

import com.google.gson.JsonParseException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.pla.spiral_abyss.platform.AbyssPlatform;
import java.util.Set;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class MobSpawnData {
    public static final String SKIP_RAISING_HEALTH = "SkipRaisingHealth";
    private static final Set<String> RESERVED = Set.of("Health", "AbsorptionAmount", "DeathTime", "HurtTime", "HurtByTimestamp", "Age", "ForcedAge", "PersistenceRequired", "NoAI", "LeftHanded", "CanPickUpLoot", "FallFlying", "SleepingX", "SleepingY", "SleepingZ");

    private MobSpawnData() {
    }

    public static CompoundTag parse(String snbt) {
        if (snbt == null || snbt.length() > 4096) {
            throw new JsonParseException("spawn_nbt exceeds 4096 characters");
        }
        try {
            CompoundTag tag = TagParser.m_129359_((String)snbt);
            if (tag.m_128440_() > 16) {
                throw new JsonParseException("spawn_nbt allows at most 16 fields");
            }
            for (String key : tag.m_128431_()) {
                ByteTag flag;
                NumericTag numeric;
                Tag value = tag.m_128423_(key);
                if (!key.matches("[A-Za-z][A-Za-z0-9_]{0,63}") || RESERVED.contains(key) || !(value instanceof NumericTag) || !Double.isFinite((numeric = (NumericTag)value).m_7061_())) {
                    throw new JsonParseException("spawn_nbt requires native numeric/boolean state fields: " + key);
                }
                if (!SKIP_RAISING_HEALTH.equals(key) || value instanceof ByteTag && (flag = (ByteTag)value).m_7063_() >= 0 && flag.m_7063_() <= 1) continue;
                throw new JsonParseException("SkipRaisingHealth must be 0b or 1b");
            }
            return tag;
        }
        catch (CommandSyntaxException ex) {
            throw new JsonParseException("Invalid spawn_nbt", (Throwable)ex);
        }
    }

    public static void apply(LivingEntity entity, CompoundTag overrides) {
        if (overrides.m_128456_()) {
            return;
        }
        CompoundTag saved = new CompoundTag();
        entity.m_7380_(saved);
        for (String key : overrides.m_128431_()) {
            if (SKIP_RAISING_HEALTH.equals(key)) continue;
            Tag value = overrides.m_128423_(key);
            if (value == null || !saved.m_128425_(key, (int)value.m_7060_())) {
                throw new IllegalArgumentException(String.valueOf(entity.m_6095_()) + " has no matching native save field " + key);
            }
            saved.m_128365_(key, value.m_6426_());
        }
        entity.m_7378_(saved);
        if (overrides.m_128441_(SKIP_RAISING_HEALTH)) {
            AbyssPlatform.data((Entity)entity).m_128379_(SKIP_RAISING_HEALTH, overrides.m_128471_(SKIP_RAISING_HEALTH));
        }
        entity.m_6210_();
    }
}


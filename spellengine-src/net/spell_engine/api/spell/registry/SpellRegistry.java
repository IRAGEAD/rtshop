/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonElement
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.DynamicOps
 *  net.minecraft.core.Holder
 *  net.minecraft.core.Holder$Reference
 *  net.minecraft.core.HolderSet$Named
 *  net.minecraft.core.Registry
 *  net.minecraft.core.RegistryAccess
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.tags.TagKey
 *  net.minecraft.util.ExtraCodecs
 *  net.minecraft.world.level.Level
 *  org.jetbrains.annotations.Nullable
 */
package net.spell_engine.api.spell.registry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.Level;
import net.spell_engine.api.spell.Spell;
import org.jetbrains.annotations.Nullable;

public class SpellRegistry {
    public static final ResourceLocation ID = new ResourceLocation("minecraft", "spell");
    public static final ResourceKey<Registry<Spell>> KEY = ResourceKey.m_135788_((ResourceLocation)ID);
    private static final Gson gson = new GsonBuilder().create();
    private static final Codec<Spell> JSON_CODEC = ExtraCodecs.f_252400_.xmap(json -> (Spell)gson.fromJson(json, Spell.class), spell -> gson.toJsonTree(spell));
    private static final Codec<Spell> BYTES_CODEC = Codec.BYTE_BUFFER.comapFlatMap(encoded -> {
        String json = new String(encoded.array(), StandardCharsets.UTF_8);
        return DataResult.success((Object)((Spell)gson.fromJson(json, Spell.class)));
    }, spell -> ByteBuffer.wrap(gson.toJson(spell).getBytes(StandardCharsets.UTF_8))).fieldOf("data").codec();
    public static final Codec<Spell> CODEC = new Codec<Spell>(){

        public <T> DataResult<Pair<Spell, T>> decode(DynamicOps<T> ops, T input) {
            return 1.delegate(ops).decode(ops, input);
        }

        public <T> DataResult<T> encode(Spell input, DynamicOps<T> ops, T prefix) {
            return 1.delegate(ops).encode((Object)input, ops, prefix);
        }

        private static Codec<Spell> delegate(DynamicOps<?> ops) {
            return ops.empty() instanceof JsonElement ? JSON_CODEC : BYTES_CODEC;
        }

        public String toString() {
            return "SpellRegistry.CODEC";
        }
    };

    public static Registry<Spell> from(Level world) {
        return world.m_9598_().m_175515_(KEY);
    }

    public static HolderSet.Named<Spell> find(Level world, ResourceLocation tagId) {
        Registry<Spell> registry = SpellRegistry.from(world);
        TagKey tag = TagKey.m_203882_(KEY, (ResourceLocation)tagId);
        return (HolderSet.Named)registry.m_203431_(tag).orElseThrow(() -> new IllegalStateException("Missing tag " + String.valueOf(tagId) + " in " + String.valueOf(KEY)));
    }

    public static List<Holder<Spell>> entries(Level world, @Nullable ResourceLocation id) {
        try {
            return SpellRegistry.find(world, id).m_203614_().toList();
        }
        catch (Exception e) {
            return List.of();
        }
    }

    public static List<Holder<Spell>> entries(Level world, @Nullable String pool) {
        if (pool == null || pool.isEmpty()) {
            return List.of();
        }
        ResourceLocation id = new ResourceLocation(pool);
        return SpellRegistry.entries(world, id);
    }

    public static Stream<Holder.Reference<Spell>> stream(Level world) {
        RegistryAccess manager = world.m_9598_();
        Registry registry = manager.m_175515_(KEY);
        return registry.m_203611_();
    }
}


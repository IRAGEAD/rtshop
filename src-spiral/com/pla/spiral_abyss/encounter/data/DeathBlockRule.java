/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParseException
 *  net.minecraft.resources.ResourceLocation
 */
package com.pla.spiral_abyss.encounter.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.pla.spiral_abyss.encounter.data.FloorParser;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public record DeathBlockRule(Set<ResourceLocation> blocks, int radius, int height, int ticks) {
    public static final DeathBlockRule NONE = new DeathBlockRule(Set.of(), 0, 0, 0);

    public DeathBlockRule {
        blocks = Set.copyOf(blocks);
        if (blocks.size() > 64 || radius < 0 || radius > 16 || height < 0 || height > 64 || ticks < (blocks.isEmpty() ? 0 : 1) || ticks > 1200) {
            throw new IllegalArgumentException("Invalid death block suppression bounds");
        }
    }

    public boolean configured() {
        return !this.blocks.isEmpty();
    }

    public static DeathBlockRule parse(JsonObject object) {
        if (!object.keySet().equals(Set.of("blocks", "radius", "height", "ticks")) || !object.get("blocks").isJsonArray()) {
            throw new JsonParseException("suppress_death_blocks requires blocks, radius, height and ticks");
        }
        JsonArray entries = object.getAsJsonArray("blocks");
        if (entries.isEmpty() || entries.size() > 64) {
            throw new JsonParseException("suppress_death_blocks needs 1..64 block IDs");
        }
        LinkedHashSet<ResourceLocation> blocks = new LinkedHashSet<ResourceLocation>();
        for (JsonElement entry : entries) {
            blocks.add(FloorParser.id(entry));
        }
        return new DeathBlockRule(blocks, DeathBlockRule.integer(object, "radius"), DeathBlockRule.integer(object, "height"), DeathBlockRule.integer(object, "ticks"));
    }

    private static int integer(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new JsonParseException(key + " must be an integer");
        }
        try {
            return value.getAsBigDecimal().intValueExact();
        }
        catch (ArithmeticException exception) {
            throw new JsonParseException(key + " must be an integer", (Throwable)exception);
        }
    }
}


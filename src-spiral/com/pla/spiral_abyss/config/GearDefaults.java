/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 */
package com.pla.spiral_abyss.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;

public final class GearDefaults {
    private GearDefaults() {
    }

    public static JsonObject assignments() {
        JsonObject floors = new JsonObject();
        for (int floor = 1; floor <= 6; ++floor) {
            floors.addProperty(Integer.toString(floor), "floor_" + floor);
        }
        JsonArray endless = new JsonArray();
        for (int floor : new int[]{1, 2, 2, 3, 3, 4, 4, 5, 6, 6}) {
            endless.add("floor_" + floor);
        }
        JsonObject result = new JsonObject();
        result.add("floors", (JsonElement)floors);
        result.add("endless", (JsonElement)endless);
        return result;
    }

    public static JsonObject forFloor(int floor) {
        List<Object> materials = switch (floor) {
            case 1 -> List.of();
            case 2 -> List.of("leather");
            case 3 -> List.of("golden", "chainmail");
            case 4 -> List.of("iron");
            case 5 -> List.of("diamond");
            case 6 -> List.of("netherite");
            default -> throw new IllegalArgumentException("Gear floor must be from 1 to 6");
        };
        String weaponMaterial = floor == 2 ? "wooden" : (materials.isEmpty() ? null : (String)materials.get(0));
        double chance = (double)(floor - 1) / 5.0;
        int level = Math.max(1, floor - 1);
        double rangedChance = floor >= 4 ? chance : 0.0;
        int rangedLevel = Math.max(1, floor - 3);
        JsonArray sets = new JsonArray();
        for (String string : materials) {
            sets.add((JsonElement)GearDefaults.items(string, "helmet", "chestplate", "leggings", "boots"));
        }
        JsonObject armor = new JsonObject();
        armor.addProperty("chance", (Number)chance);
        armor.add("sets", (JsonElement)sets);
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("chance", (Number)chance);
        jsonObject.add("items", (JsonElement)(weaponMaterial == null ? new JsonArray() : GearDefaults.items(weaponMaterial, "sword", "pickaxe", "axe", "shovel", "hoe")));
        JsonObject enchantments = new JsonObject();
        enchantments.add("armor", (JsonElement)GearDefaults.rule(chance, level, "protection", "fire_protection", "blast_protection", "projectile_protection", "unbreaking"));
        enchantments.add("melee", (JsonElement)GearDefaults.rule(chance, level, "sharpness", "fire_aspect", "knockback", "unbreaking"));
        enchantments.add("bow", (JsonElement)GearDefaults.rule(rangedChance, rangedLevel, "power", "flame"));
        enchantments.add("crossbow", (JsonElement)GearDefaults.rule(rangedChance, rangedLevel, "quick_charge", "multishot", "piercing"));
        JsonObject trident = GearDefaults.rule(chance, level, "impaling", "unbreaking");
        trident.addProperty("loyalty_level", (Number)rangedLevel);
        enchantments.add("trident", (JsonElement)trident);
        JsonObject result = new JsonObject();
        result.add("armor", (JsonElement)armor);
        result.add("equipment", (JsonElement)jsonObject);
        result.add("enchantments", (JsonElement)enchantments);
        return result;
    }

    private static JsonArray items(String material, String ... slots) {
        JsonArray result = new JsonArray();
        for (String slot : slots) {
            result.add("minecraft:" + material + "_" + slot);
        }
        return result;
    }

    private static JsonObject rule(double chance, int level, String ... enchantments) {
        JsonObject rule = new JsonObject();
        rule.addProperty("chance", (Number)chance);
        rule.addProperty("level", (Number)level);
        JsonArray entries = new JsonArray();
        for (String enchantment : enchantments) {
            entries.add("minecraft:" + enchantment);
        }
        rule.add("entries", (JsonElement)entries);
        return rule;
    }
}


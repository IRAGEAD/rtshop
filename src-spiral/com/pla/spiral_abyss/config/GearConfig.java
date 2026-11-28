/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  com.mojang.logging.LogUtils
 *  dev.architectury.platform.Platform
 *  net.minecraft.resources.ResourceLocation
 */
package com.pla.spiral_abyss.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.pla.spiral_abyss.config.GearDefaults;
import dev.architectury.platform.Platform;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

public final class GearConfig {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();

    private GearConfig() {
    }

    public static void prepare() {
        GearConfig.prepare(Platform.getConfigFolder());
    }

    public static void prepare(Path configRoot) {
        Path directory = configRoot.resolve("spiral_abyss");
        try {
            Files.createDirectories(directory.resolve("gear"), new FileAttribute[0]);
            Path old = configRoot.resolve("spiral_abyss-common.toml");
            Path current = directory.resolve("spiral_abyss-common.toml");
            if (!Files.exists(current, new LinkOption[0]) && Files.isRegularFile(old, new LinkOption[0])) {
                Files.copy(old, current, new CopyOption[0]);
            }
            for (int floor = 1; floor <= 6; ++floor) {
                Path file = directory.resolve("gear/floor_" + floor + ".json");
                if (Files.exists(file, new LinkOption[0])) continue;
                Files.writeString(file, (CharSequence)(JSON.toJson((JsonElement)GearDefaults.forFloor(floor)) + "\n"), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            }
            Path assignments = directory.resolve("gear/assignments.json");
            if (!Files.exists(assignments, new LinkOption[0])) {
                Files.writeString(assignments, (CharSequence)(JSON.toJson((JsonElement)GearDefaults.assignments()) + "\n"), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            }
        }
        catch (IOException ex) {
            throw new IllegalStateException("Cannot prepare Spiral Abyss gear configuration", ex);
        }
    }

    public static Floor load(int floor) {
        return GearConfig.load(Platform.getConfigFolder(), floor);
    }

    public static Floor load(Path configRoot, int floor) {
        if (floor < 1 || floor > 6) {
            throw new IllegalArgumentException("Gear floor must be from 1 to 6");
        }
        return GearConfig.loadProfile(configRoot, GearConfig.assignments(configRoot).floor(floor), floor);
    }

    public static Floor loadEndless(int wave) {
        return GearConfig.loadEndless(Platform.getConfigFolder(), wave);
    }

    public static Floor loadEndless(Path configRoot, int wave) {
        if (wave < 1) {
            throw new IllegalArgumentException("Endless wave must be positive");
        }
        int defaultFloor = Integer.parseInt(GearConfig.parseAssignments(GearDefaults.assignments()).endless(wave).substring(6));
        return GearConfig.loadProfile(configRoot, GearConfig.assignments(configRoot).endless(wave), defaultFloor);
    }

    private static Floor loadProfile(Path configRoot, String profile, int defaultFloor) {
        Floor floor;
        block8: {
            Path file = configRoot.resolve("spiral_abyss/gear/" + profile + ".json");
            BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);
            try {
                floor = GearConfig.parse(JsonParser.parseReader((Reader)reader).getAsJsonObject());
                if (reader == null) break block8;
            }
            catch (Throwable throwable) {
                try {
                    if (reader != null) {
                        try {
                            ((Reader)reader).close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                catch (IOException | RuntimeException ex) {
                    LogUtils.getLogger().error("Invalid gear file {}; using code-defined defaults for this attempt", (Object)file, (Object)ex);
                    return GearConfig.parse(GearDefaults.forFloor(defaultFloor));
                }
            }
            ((Reader)reader).close();
        }
        return floor;
    }

    private static Assignments assignments(Path configRoot) {
        Assignments assignments;
        block9: {
            Path file = configRoot.resolve("spiral_abyss/gear/assignments.json");
            if (!Files.exists(file, new LinkOption[0])) {
                return GearConfig.parseAssignments(GearDefaults.assignments());
            }
            BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);
            try {
                assignments = GearConfig.parseAssignments(JsonParser.parseReader((Reader)reader).getAsJsonObject());
                if (reader == null) break block9;
            }
            catch (Throwable throwable) {
                try {
                    if (reader != null) {
                        try {
                            ((Reader)reader).close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                catch (IOException | RuntimeException ex) {
                    LogUtils.getLogger().error("Invalid gear assignments {}; using code-defined assignments for this attempt", (Object)file, (Object)ex);
                    return GearConfig.parseAssignments(GearDefaults.assignments());
                }
            }
            ((Reader)reader).close();
        }
        return assignments;
    }

    public static Assignments parseAssignments(JsonObject json) {
        JsonObject floors = json.getAsJsonObject("floors");
        ArrayList<String> floorProfiles = new ArrayList<String>();
        for (int floor = 1; floor <= 6; ++floor) {
            floorProfiles.add(GearConfig.profile(floors.get(Integer.toString(floor))));
        }
        JsonArray endless = json.getAsJsonArray("endless");
        if (endless.size() != 10) {
            throw new IllegalArgumentException("Endless gear assignments need exactly ten profile names");
        }
        ArrayList<String> endlessProfiles = new ArrayList<String>();
        for (JsonElement value : endless) {
            endlessProfiles.add(GearConfig.profile(value));
        }
        return new Assignments(floorProfiles, endlessProfiles);
    }

    private static String profile(JsonElement value) {
        String name = value.getAsString();
        if (!name.matches("[a-z0-9][a-z0-9_-]{0,63}")) {
            throw new IllegalArgumentException("Gear profile must be a simple lowercase name, without a path or .json suffix");
        }
        return name;
    }

    public static Floor parse(JsonObject json) {
        JsonObject armor = json.getAsJsonObject("armor");
        JsonObject equipment = json.getAsJsonObject("equipment");
        ArrayList<List<ResourceLocation>> sets = new ArrayList<List<ResourceLocation>>();
        for (JsonElement value : armor.getAsJsonArray("sets")) {
            List<ResourceLocation> set = GearConfig.ids(value.getAsJsonArray());
            if (set.size() != 4) {
                throw new IllegalArgumentException("Each armor set needs helmet, chestplate, leggings, boots, in that order");
            }
            sets.add(set);
        }
        JsonObject enchant = json.getAsJsonObject("enchantments");
        return new Floor(GearConfig.chance(armor), sets, GearConfig.chance(equipment), GearConfig.ids(equipment.getAsJsonArray("items")), GearConfig.rule(enchant.getAsJsonObject("armor")), GearConfig.rule(enchant.getAsJsonObject("melee")), GearConfig.rule(enchant.getAsJsonObject("bow")), GearConfig.rule(enchant.getAsJsonObject("crossbow")), GearConfig.rule(enchant.getAsJsonObject("trident")), GearConfig.integer(enchant.getAsJsonObject("trident"), "loyalty_level", 1, 3));
    }

    private static Rule rule(JsonObject json) {
        return new Rule(GearConfig.chance(json), GearConfig.integer(json, "level", 1, 255), GearConfig.ids(json.getAsJsonArray("entries")));
    }

    private static int integer(JsonObject json, String key, int min, int max) {
        double value = json.get(key).getAsDouble();
        if (!Double.isFinite(value) || value != Math.rint(value) || value < (double)min || value > (double)max) {
            throw new IllegalArgumentException(key + " must be an integer from " + min + " to " + max);
        }
        return (int)value;
    }

    private static double chance(JsonObject json) {
        double value = json.get("chance").getAsDouble();
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException("chance must be from 0 to 1");
        }
        return value;
    }

    private static List<ResourceLocation> ids(JsonArray array) {
        ArrayList<ResourceLocation> result = new ArrayList<ResourceLocation>();
        for (JsonElement entry : array) {
            ResourceLocation id = ResourceLocation.m_135820_((String)entry.getAsString());
            if (id == null) {
                throw new IllegalArgumentException("Invalid registry ID: " + String.valueOf(entry));
            }
            result.add(id);
        }
        return List.copyOf(result);
    }

    public record Floor(double armorChance, List<List<ResourceLocation>> armorSets, double weaponChance, List<ResourceLocation> weapons, Rule armor, Rule melee, Rule bow, Rule crossbow, Rule trident, int loyaltyLevel) {
        public Floor {
            armorSets = armorSets.stream().map(List::copyOf).toList();
            weapons = List.copyOf(weapons);
        }
    }

    public record Assignments(List<String> floors, List<String> endless) {
        public Assignments {
            floors = List.copyOf(floors);
            endless = List.copyOf(endless);
        }

        public String floor(int floor) {
            if (floor < 1 || floor > 6) {
                throw new IllegalArgumentException("Gear floor must be from 1 to 6");
            }
            return this.floors.get(floor - 1);
        }

        public String endless(int wave) {
            if (wave < 1) {
                throw new IllegalArgumentException("Endless wave must be positive");
            }
            return this.endless.get((wave - 1) % 10);
        }
    }

    public record Rule(double chance, int level, List<ResourceLocation> entries) {
        public Rule {
            entries = List.copyOf(entries);
        }
    }
}


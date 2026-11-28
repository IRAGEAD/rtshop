/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParseException
 *  com.google.gson.JsonParser
 *  com.mojang.logging.LogUtils
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.packs.resources.Resource
 *  net.minecraft.server.packs.resources.ResourceManager
 *  net.minecraft.server.packs.resources.SimplePreparableReloadListener
 *  net.minecraft.util.profiling.ProfilerFiller
 *  org.slf4j.Logger
 */
package com.pla.spiral_abyss.encounter.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.pla.spiral_abyss.encounter.data.FloorDefinition;
import com.pla.spiral_abyss.encounter.data.FloorParser;
import java.io.BufferedReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

public final class FloorCatalog
extends SimplePreparableReloadListener<Map<Integer, FloorDefinition>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Pattern FILE = Pattern.compile("floor_([1-9][0-9]*)/(floor|wave_([1-9][0-9]*))\\.json");
    private static volatile Snapshot current = new Snapshot(0, Map.of());
    private static volatile Map<ResourceLocation, FloorDefinition.WaveType> templates = Map.of();

    public static Snapshot snapshot() {
        return current;
    }

    public static Map<ResourceLocation, FloorDefinition.WaveType> waveTypes() {
        return templates;
    }

    protected Map<Integer, FloorDefinition> prepare(ResourceManager manager, ProfilerFiller profiler) {
        HashMap<ResourceLocation, FloorDefinition.WaveType> waveTypes = new HashMap<ResourceLocation, FloorDefinition.WaveType>();
        manager.m_214159_("wave_types", id -> id.m_135815_().endsWith(".json")).forEach((id, resource) -> {
            ResourceLocation name = new ResourceLocation(id.m_135827_(), id.m_135815_().substring("wave_types/".length(), id.m_135815_().length() - ".json".length()));
            try (BufferedReader reader = resource.m_215508_();){
                waveTypes.put(name, FloorParser.waveType(name, JsonParser.parseReader((Reader)reader).getAsJsonObject()));
            }
            catch (Exception ex) {
                LOGGER.error("Invalid wave type {}: {}", id, (Object)ex.getMessage());
            }
        });
        templates = Map.copyOf(waveTypes);
        HashMap settings = new HashMap();
        HashMap waves = new HashMap();
        HashSet invalid = new HashSet();
        HashMap<ResourceLocation, Resource> resources = new HashMap<ResourceLocation, Resource>();
        for (int number = 1; number <= 6; ++number) {
            resources.putAll(manager.m_214159_("floor_" + number, id -> id.m_135827_().equals("spiral_abyss") && FILE.matcher(id.m_135815_()).matches()));
        }
        resources.forEach((id, resource) -> {
            int floor;
            Matcher match = FILE.matcher(id.m_135815_());
            if (!match.matches()) {
                return;
            }
            try {
                floor = Integer.parseInt(match.group(1));
            }
            catch (NumberFormatException ex) {
                LOGGER.error("Ignoring invalid floor path {}", id);
                return;
            }
            if (floor > 6) {
                LOGGER.error("Ignoring {}: maximum floor is 6", id);
                return;
            }
            try (BufferedReader reader = resource.m_215508_();){
                JsonElement json = JsonParser.parseReader((Reader)reader);
                if (!json.isJsonObject()) {
                    throw new JsonParseException("file must contain an object");
                }
                if (match.group(3) == null) {
                    settings.put(floor, json.getAsJsonObject());
                } else {
                    waves.computeIfAbsent(floor, n -> new TreeMap()).put(Integer.parseInt(match.group(3)), json.getAsJsonObject());
                }
            }
            catch (Exception ex) {
                invalid.add(floor);
                LOGGER.error("Invalid floor resource {}: {}", id, (Object)ex.getMessage());
            }
        });
        HashMap<Integer, FloorDefinition> result = new HashMap<Integer, FloorDefinition>();
        HashSet numbers = new HashSet(settings.keySet());
        numbers.addAll(waves.keySet());
        Iterator iterator = numbers.iterator();
        while (iterator.hasNext()) {
            int number = (Integer)iterator.next();
            if (invalid.contains(number)) continue;
            try {
                if (!settings.containsKey(number)) {
                    throw new JsonParseException("missing floor.json");
                }
                result.put(number, FloorParser.parse(number, (JsonObject)settings.get(number), waves.getOrDefault(number, new TreeMap()), waveTypes));
            }
            catch (Exception ex) {
                LOGGER.error("Floor {} disabled: {}", (Object)number, (Object)ex.getMessage());
            }
        }
        return Map.copyOf(result);
    }

    protected void apply(Map<Integer, FloorDefinition> floors, ResourceManager manager, ProfilerFiller profiler) {
        current = new Snapshot(current.revision() + 1, floors);
        LOGGER.info("Loaded SpiralAbyss floors: {}", new TreeSet<Integer>(floors.keySet()));
    }

    public record Snapshot(int revision, Map<Integer, FloorDefinition> floors) {
        public Snapshot {
            floors = Map.copyOf(floors);
        }
    }
}


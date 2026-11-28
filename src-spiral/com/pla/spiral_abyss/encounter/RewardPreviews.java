/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.packs.resources.Resource
 *  net.minecraft.tags.TagKey
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.storage.loot.Deserializers
 *  net.minecraft.world.level.storage.loot.LootTable
 */
package com.pla.spiral_abyss.encounter;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pla.spiral_abyss.encounter.FloorRewards;
import com.pla.spiral_abyss.encounter.RewardItems;
import com.pla.spiral_abyss.encounter.RewardPool;
import com.pla.spiral_abyss.encounter.data.FloorDefinition;
import com.pla.spiral_abyss.encounter.data.MobCatalog;
import com.pla.spiral_abyss.network.AbyssNetwork;
import com.pla.spiral_abyss.platform.AbyssRegistries;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.Deserializers;
import net.minecraft.world.level.storage.loot.LootTable;

public final class RewardPreviews {
    public static final int MAX_REWARDS = 128;
    public static final int MAX_CONTENTS = 128;
    private final MinecraftServer server;
    private final Gson gson = Deserializers.m_78800_().create();
    private final Map<ResourceLocation, Contents> cache = new HashMap<ResourceLocation, Contents>();

    public RewardPreviews(MinecraftServer server) {
        this.server = server;
    }

    public List<AbyssNetwork.RewardPreview> floor(FloorDefinition.Rewards base, Set<ResourceLocation> enemies) {
        FloorDefinition.Rewards rewards = FloorRewards.withMobRewards(base, enemies, MobCatalog.rewards());
        HashMap<ResourceLocation, Double> tableChances = new HashMap<ResourceLocation, Double>();
        double tableWeight = 0.0;
        for (FloorDefinition.RewardEntry entry2 : rewards.entries()) {
            if (entry2.item() || this.table(entry2.id()) == null) continue;
            tableWeight += (double)entry2.weight();
            tableChances.merge(entry2.id(), (double)entry2.weight() * entry2.chance(), Double::sum);
        }
        LinkedHashMap unique = new LinkedHashMap();
        double rareChance = RewardPool.rareChance();
        rewards.entries().stream().filter(entry -> (entry.rare() ? rareChance : entry.chance()) > 0.0).sorted(Comparator.comparingInt(entry -> !entry.item() ? 0 : (entry.rare() ? 2 : 1))).forEach(entry -> unique.putIfAbsent(new PreviewKey(entry.id(), entry.item(), entry.rare(), entry.enchantments()), entry));
        ArrayList<AbyssNetwork.RewardPreview> result = new ArrayList<AbyssNetwork.RewardPreview>();
        for (FloorDefinition.RewardEntry entry3 : unique.values()) {
            if (result.size() >= 128) break;
            if (entry3.item()) {
                if (!RewardPreviews.available(entry3.id())) continue;
                result.add(new AbyssNetwork.RewardPreview(entry3.id(), false, entry3.rare(), List.of(), false, RewardItems.stack(entry3, 1), entry3.rare() ? rareChance : 0.0));
                continue;
            }
            if (this.table(entry3.id()) == null) continue;
            Contents contents = this.cache.computeIfAbsent(entry3.id(), id -> {
                Scan scan = new Scan();
                this.scanTable((ResourceLocation)id, scan, 0);
                return new Contents(List.copyOf(scan.items), scan.incomplete);
            });
            double chance = tableWeight > 0.0 ? tableChances.getOrDefault(entry3.id(), 0.0) / tableWeight : 0.0;
            result.add(new AbyssNetwork.RewardPreview(entry3.id(), true, false, contents.items(), contents.incomplete(), ItemStack.f_41583_, Math.min(1.0, chance)));
        }
        return List.copyOf(result);
    }

    private LootTable table(ResourceLocation id) {
        return FloorRewards.table(this.server, id);
    }

    private static boolean available(ResourceLocation id) {
        return AbyssRegistries.ITEMS.containsKey(id) && AbyssRegistries.ITEMS.getValue(id) != Items.f_41852_;
    }

    private void add(ResourceLocation id, Scan scan) {
        if (!RewardPreviews.available(id)) {
            return;
        }
        if (scan.items.size() < 128) {
            scan.items.add(id);
        } else if (!scan.items.contains(id)) {
            scan.incomplete = true;
        }
    }

    private void scanTable(ResourceLocation id, Scan scan, int depth) {
        this.scanTable(id, scan, depth, 0);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void scanTable(ResourceLocation id, Scan scan, int depth, int transforms) {
        if (depth > 16 || !scan.visiting.add(id)) {
            scan.incomplete = true;
            return;
        }
        try {
            LootTable table = this.table(id);
            if (table == null) {
                scan.incomplete = true;
                return;
            }
            ResourceLocation source = new ResourceLocation(id.m_135827_(), "loot_tables/" + id.m_135815_() + ".json");
            Optional resource = this.server.m_177941_().m_213713_(source);
            if (resource.isPresent()) {
                try (BufferedReader reader = ((Resource)resource.get()).m_215508_();){
                    this.scan(JsonParser.parseReader((Reader)reader), scan, depth + 1, transforms);
                }
                return;
            }
            this.scan(this.gson.toJsonTree((Object)table, LootTable.class), scan, depth + 1, transforms);
        }
        catch (IOException | LinkageError | RuntimeException ex) {
            scan.incomplete = true;
        }
        finally {
            scan.visiting.remove(id);
        }
    }

    private void scan(JsonElement value, Scan state, int depth, int transforms) {
        if (++state.nodes > 8192 || depth > 32) {
            state.incomplete = true;
            return;
        }
        if (value.isJsonArray()) {
            for (JsonElement child : value.getAsJsonArray()) {
                this.scan(child, state, depth + 1, transforms);
            }
            return;
        }
        if (!value.isJsonObject()) {
            return;
        }
        JsonObject object = value.getAsJsonObject();
        transforms |= RewardPreviews.transforms(object.get("functions"), state);
        if (object.has("type")) {
            Object type = object.get("type").getAsString();
            if (!((String)type).contains(":")) {
                type = "minecraft:" + (String)type;
            }
            ResourceLocation id = object.has("name") && object.get("name").isJsonPrimitive() ? ResourceLocation.m_135820_((String)object.get("name").getAsString()) : null;
            switch (type) {
                case "minecraft:item": {
                    if (id == null) break;
                    this.addTransformed(id, state, transforms);
                    break;
                }
                case "minecraft:loot_table": {
                    if (id == null) break;
                    this.scanTable(id, state, depth + 1, transforms);
                    break;
                }
                case "minecraft:tag": {
                    int appliedTransforms = transforms;
                    if (id == null) break;
                    this.server.m_206579_().m_175515_(Registries.f_256913_).m_203431_(TagKey.m_203882_((ResourceKey)Registries.f_256913_, (ResourceLocation)id)).ifPresent(tag -> tag.forEach(item -> this.addTransformed(AbyssRegistries.ITEMS.getKey((Item)item.m_203334_()), state, appliedTransforms)));
                    break;
                }
                case "minecraft:empty": 
                case "minecraft:alternatives": 
                case "minecraft:sequence": 
                case "minecraft:group": 
                case "minecraft:chest": {
                    break;
                }
                default: {
                    if (object.has("pools")) break;
                    state.incomplete = true;
                }
            }
        }
        for (String key : List.of("pools", "entries", "children")) {
            if (!object.has(key)) continue;
            this.scan(object.get(key), state, depth + 1, transforms);
        }
    }

    private void addTransformed(ResourceLocation id, Scan state, int transforms) {
        if ((transforms & 1) != 0 && id.toString().equals("minecraft:book")) {
            this.add(new ResourceLocation("minecraft", "enchanted_book"), state);
            if ((transforms & 4) == 0) {
                return;
            }
        }
        if ((transforms & 2) != 0 && id.toString().equals("minecraft:map")) {
            this.add(new ResourceLocation("minecraft", "filled_map"), state);
        }
        this.add(id, state);
    }

    private static int transforms(JsonElement value, Scan state) {
        if (value == null || !value.isJsonArray()) {
            return 0;
        }
        int result = 0;
        for (JsonElement element : value.getAsJsonArray()) {
            if (!element.isJsonObject()) {
                state.incomplete = true;
                continue;
            }
            JsonObject function = element.getAsJsonObject();
            if (!function.has("function")) continue;
            Object name = function.get("function").getAsString();
            if (!((String)name).contains(":")) {
                name = "minecraft:" + (String)name;
            }
            if (((String)name).equals("minecraft:enchant_randomly") || ((String)name).equals("minecraft:enchant_with_levels")) {
                result |= 1;
                if (!function.has("conditions") || function.getAsJsonArray("conditions").isEmpty()) continue;
                result |= 4;
                continue;
            }
            if (((String)name).equals("minecraft:exploration_map")) {
                result |= 2;
                continue;
            }
            if (((String)name).startsWith("minecraft:") && !((String)name).equals("minecraft:furnace_smelt")) continue;
            state.incomplete = true;
        }
        return result;
    }

    private record Contents(List<ResourceLocation> items, boolean incomplete) {
    }

    private static final class Scan {
        final Set<ResourceLocation> items = new TreeSet<ResourceLocation>();
        final Set<ResourceLocation> visiting = new HashSet<ResourceLocation>();
        boolean incomplete;
        int nodes;

        private Scan() {
        }
    }

    private record PreviewKey(ResourceLocation id, boolean item, boolean rare, Map<ResourceLocation, Integer> enchantments) {
    }
}


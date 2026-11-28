/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParseException
 *  com.google.gson.JsonParser
 *  com.mojang.logging.LogUtils
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.stats.Stats
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.storage.LevelResource
 */
package com.pla.spiral_abyss.world;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.pla.spiral_abyss.platform.AbyssPlatform;
import com.pla.spiral_abyss.platform.AbyssRegistries;
import com.pla.spiral_abyss.platform.event.AbyssEvents;
import com.pla.spiral_abyss.world.AbyssDimension;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;

public final class BossProgress {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<MinecraftServer, Map<UUID, Progress>> SERVERS = new IdentityHashMap<MinecraftServer, Map<UUID, Progress>>();

    private BossProgress() {
    }

    public static boolean hasDefeated(ServerPlayer player, ResourceLocation id) {
        return BossProgress.progress((ServerPlayer)player).defeated.contains(id);
    }

    public static void initialize(ServerPlayer player) {
        BossProgress.progress(player);
    }

    public static void recordKill(ServerPlayer player, Entity victim) {
        Progress progress = BossProgress.progress(player);
        if (victim.m_9236_().f_46443_ || victim.m_9236_().m_46472_().equals(AbyssDimension.LEVEL) || AbyssPlatform.data(victim).m_128403_("spiral_abyss_run")) {
            return;
        }
        ResourceLocation id = AbyssRegistries.ENTITY_TYPES.getKey(victim.m_6095_());
        if (id != null && progress.writable && progress.defeated.add(id)) {
            BossProgress.save(player, progress);
        }
    }

    private static Path path(ServerPlayer player) {
        return player.f_8924_.m_129843_(LevelResource.f_78182_).resolve("spiral_abyss/boss_progress").resolve(String.valueOf(player.m_20148_()) + ".json");
    }

    private static Progress progress(ServerPlayer player) {
        return SERVERS.computeIfAbsent(player.f_8924_, unused -> new HashMap()).computeIfAbsent(player.m_20148_(), unused -> BossProgress.load(player));
    }

    private static Progress load(ServerPlayer player) {
        Progress progress = new Progress();
        Path path = BossProgress.path(player);
        if (Files.exists(path, new LinkOption[0])) {
            try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);){
                JsonObject root = JsonParser.parseReader((Reader)reader).getAsJsonObject();
                if (root.get("version").getAsInt() != 1) {
                    throw new JsonParseException("Unsupported boss progress version");
                }
                for (JsonElement entry : root.getAsJsonArray("defeated")) {
                    ResourceLocation id = ResourceLocation.m_135820_((String)entry.getAsString());
                    if (id == null) {
                        throw new JsonParseException("Invalid boss ID");
                    }
                    progress.defeated.add(id);
                }
            }
            catch (Exception error) {
                progress.defeated.clear();
                progress.writable = false;
                LogUtils.getLogger().error("Cannot read Abyss boss progression {}; preserve and repair this file", (Object)path, (Object)error);
            }
        } else {
            for (EntityType<?> type : AbyssRegistries.ENTITY_TYPES.getValues()) {
                if (player.m_8951_().m_13015_(Stats.f_12986_.m_12902_(type)) <= 0) continue;
                progress.defeated.add(AbyssRegistries.ENTITY_TYPES.getKey(type));
            }
            BossProgress.save(player, progress);
        }
        return progress;
    }

    private static void save(ServerPlayer player, Progress progress) {
        Path path = BossProgress.path(player);
        Path temporary = path.resolveSibling(String.valueOf(path.getFileName()) + ".tmp");
        JsonObject root = new JsonObject();
        root.addProperty("version", (Number)1);
        JsonArray defeated = new JsonArray();
        progress.defeated.forEach(id -> defeated.add(id.toString()));
        root.add("defeated", (JsonElement)defeated);
        try {
            Files.createDirectories(path.getParent(), new FileAttribute[0]);
            Files.writeString(temporary, (CharSequence)GSON.toJson((JsonElement)root), StandardCharsets.UTF_8, new OpenOption[0]);
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
            catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (IOException error) {
            LogUtils.getLogger().error("Cannot save Abyss boss progression {}", (Object)path, (Object)error);
        }
    }

    public static void login(AbyssEvents.PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer) {
            ServerPlayer player2 = (ServerPlayer)player;
            BossProgress.initialize(player2);
        }
    }

    public static void stopped(AbyssEvents.ServerStoppedEvent event) {
        SERVERS.remove(event.getServer());
    }

    private static final class Progress {
        final Set<ResourceLocation> defeated = new TreeSet<ResourceLocation>(Comparator.comparing(ResourceLocation::toString));
        boolean writable = true;

        private Progress() {
        }
    }
}


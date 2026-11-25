package com.rtshop.quest;

import com.rtshop.RtShop;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * NPC 가 어디에 서 있는지 기억해 둔다 (월드 데이터 rtshop_npc_places): 퀘스트 HUD 가 "어느 쪽 몇 칸"을 알려 주는 데 쓴다.
 * 사람 가까이(48칸)에 있는 NPC 를 5초마다 살펴 적는다. NPC 를 옮기면 다음에 누가 그 곁을 지날 때 고쳐진다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class NpcPlaces extends SavedData {
    private static final String NAME = "rtshop_npc_places";

    public record Place(String dim, int x, int y, int z) {
        public String encode() {
            return dim + ";" + x + ";" + y + ";" + z;
        }
    }

    private final Map<String, Place> places = new LinkedHashMap<>();

    public static NpcPlaces get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(NpcPlaces::load, NpcPlaces::new, NAME);
    }

    public static NpcPlaces load(CompoundTag tag) {
        NpcPlaces d = new NpcPlaces();
        ListTag l = tag.getList("places", Tag.TAG_COMPOUND);
        for (int i = 0; i < l.size(); i++) {
            CompoundTag c = l.getCompound(i);
            d.places.put(c.getString("key"), new Place(c.getString("dim"), c.getInt("x"), c.getInt("y"), c.getInt("z")));
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag l = new ListTag();
        for (Map.Entry<String, Place> e : places.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putString("key", e.getKey());
            c.putString("dim", e.getValue().dim());
            c.putInt("x", e.getValue().x());
            c.putInt("y", e.getValue().y());
            c.putInt("z", e.getValue().z());
            l.add(c);
        }
        tag.put("places", l);
        return tag;
    }

    /** 이 NPC 의 자리 ("차원;x;y;z"), 모르면 "". */
    public static String where(MinecraftServer server, String npcKey) {
        if (server == null || npcKey == null || npcKey.isEmpty()) return "";
        Place p = get(server).places.get(npcKey);
        return p == null ? "" : p.encode();
    }

    public static Map<String, Place> all(MinecraftServer server) {
        return java.util.Collections.unmodifiableMap(get(server).places);
    }

    public static void note(Entity e) {
        if (e == null || e.level().isClientSide() || e.getServer() == null) return;
        String key = QuestManager.npcKey(e);
        if (key == null) return;
        NpcPlaces d = get(e.getServer());
        Place now = new Place(e.level().dimension().location().toString(), e.getBlockX(), e.getBlockY(), e.getBlockZ());
        if (!now.equals(d.places.get(key))) {
            d.places.put(key, now);
            d.setDirty();
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 100 != 41) return;
        try {
            for (ServerPlayer p : event.getServer().getPlayerList().getPlayers()) {
                for (Entity e : p.level().getEntities(p, p.getBoundingBox().inflate(48), x -> QuestManager.npcKey(x) != null)) note(e);
            }
        } catch (Throwable ignored) {
        }
    }
}

package com.rtshop.client;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * NPC 와 나눈 대화 내역 (이 컴퓨터에 서버·세계별로 저장). NPC 마다 최근 MAX 줄.
 * config/rtshop/talk_history.json : { "<서버>|<npc>": [["말한 사람", "대사"], ...] }
 */
public final class TalkHistory {
    private TalkHistory() {
    }

    public static final int MAX = 150;
    private static JsonObject all;

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("talk_history.json");
    }

    /** 지금 접속한 곳 (멀티: 서버 주소, 싱글: 세계 이름). */
    static String where() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getCurrentServer() != null) return mc.getCurrentServer().ip;
        if (mc.getSingleplayerServer() != null) return "sp:" + mc.getSingleplayerServer().getWorldData().getLevelName();
        return "?";
    }

    private static synchronized JsonObject all() {
        if (all == null) {
            all = new JsonObject();
            try {
                if (Files.exists(file())) all = JsonParser.parseString(Files.readString(file(), StandardCharsets.UTF_8)).getAsJsonObject();
            } catch (Exception e) {
                RtShop.LOGGER.warn("[rtshop] 대화 내역을 읽지 못했습니다: {}", e.toString());
            }
        }
        return all;
    }

    public static synchronized void add(String npc, String speaker, String line) {
        if (line == null || line.isBlank()) return;
        String key = where() + "|" + npc;
        JsonObject a = all();
        JsonArray arr = a.has(key) ? a.getAsJsonArray(key) : new JsonArray();
        if (!arr.isEmpty()) {
            JsonArray last = arr.get(arr.size() - 1).getAsJsonArray();
            if (last.get(0).getAsString().equals(speaker) && last.get(1).getAsString().equals(line)) return; // 같은 줄 반복 안 함
        }
        JsonArray e = new JsonArray();
        e.add(speaker);
        e.add(line);
        arr.add(e);
        while (arr.size() > MAX) arr.remove(0);
        a.add(key, arr);
        save();
    }

    public static synchronized List<String[]> of(String npc) {
        List<String[]> out = new ArrayList<>();
        JsonObject a = all();
        String key = where() + "|" + npc;
        if (!a.has(key)) return out;
        for (JsonElement e : a.getAsJsonArray(key)) {
            JsonArray x = e.getAsJsonArray();
            out.add(new String[]{x.get(0).getAsString(), x.get(1).getAsString()});
        }
        return out;
    }

    private static void save() {
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), new GsonBuilder().disableHtmlEscaping().create().toJson(all), StandardCharsets.UTF_8);
        } catch (Exception e) {
            RtShop.LOGGER.warn("[rtshop] 대화 내역을 저장하지 못했습니다: {}", e.toString());
        }
    }
}

package com.rtshop.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.net.StoryPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 이야기 연출 받기, 아이템 이야기(보스가 떨어뜨린 물건의 설명) 붙이기. */
public final class StoryClient {
    private StoryClient() {
    }

    /** 아이템 ID → 이야기 줄들. */
    public static final Map<String, List<String>> LORE = new HashMap<>();

    public static void receive(int kind, String json) {
        if (kind == StoryPacket.CINEMA) {
            // 대화창이 열려 있으면 닫고 연출을 보여 준다 (장 제목·환영은 화면 전체를 쓴다)
            try {
                JsonObject o = JsonParser.parseString(json).getAsJsonObject();
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (o.has("closeScreen") && o.get("closeScreen").getAsBoolean() && mc.screen != null) mc.setScreen(null);
            } catch (Exception ignored) {
            }
            Cinematic.play(json);
        } else if (kind == StoryPacket.LORE) {
            try {
                JsonObject o = JsonParser.parseString(json).getAsJsonObject();
                LORE.clear();
                for (Map.Entry<String, JsonElement> e : o.entrySet()) {
                    List<String> lines = new ArrayList<>();
                    if (e.getValue().isJsonArray()) {
                        for (JsonElement l : e.getValue().getAsJsonArray()) lines.add(l.getAsString());
                    }
                    LORE.put(e.getKey(), lines);
                }
            } catch (Exception ignored) {
            }
        }
    }

    public static void onTooltip(ItemTooltipEvent event) {
        if (LORE.isEmpty() || event.getItemStack().isEmpty()) return;
        String boss = event.getItemStack().hasTag() ? event.getItemStack().getTag().getString("rtshop_lore") : "";
        List<String> lines = boss.isEmpty() ? null : LORE.get("boss:" + boss);
        if (lines == null) {
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());
            if (rl == null) return;
            lines = LORE.get(rl.toString());
        }
        if (lines == null || lines.isEmpty()) return;
        event.getToolTip().add(Component.empty());
        for (String l : lines) event.getToolTip().add(Component.literal(l).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}

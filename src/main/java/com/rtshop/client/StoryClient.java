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
        } else if (kind == StoryPacket.MUSIC) {
            try {
                BossMusicClient.receive(JsonParser.parseString(json).getAsJsonObject());
            } catch (Exception ignored) {
            }
        } else if (kind == StoryPacket.NOTEBOOK) {
            try {
                net.minecraft.client.Minecraft.getInstance().setScreen(new NotebookScreen(JsonParser.parseString(json).getAsJsonObject()));
            } catch (Exception ignored) {
            }
        } else if (kind == StoryPacket.SPOTS) {
            try {
                SpotClient.receive(JsonParser.parseString(json).getAsJsonObject());
            } catch (Exception ignored) {
            }
        } else if (kind == StoryPacket.BOUNTY) {
            try {
                BountyScreen.open(JsonParser.parseString(json).getAsJsonObject());
            } catch (Exception ignored) {
            }
        } else if (kind == StoryPacket.DIALOGUE) {
            try {
                DialogueScreen.open(JsonParser.parseString(json).getAsJsonObject());
            } catch (Exception ignored) {
            }
        } else if (kind == StoryPacket.GATE) {
            try {
                JsonObject o = JsonParser.parseString(json).getAsJsonObject();
                GATE_RARE = o.has("rare") ? o.get("rare").getAsString() : "";
                GATE_EPIC = o.has("epic") ? o.get("epic").getAsString() : "";
            } catch (Exception ignored) {
            }
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

    /** 서버가 알려 준 장비 잠금: 희귀/서사 장비를 사려면 마쳐야 하는 장 ("" = 열림). */
    public static String GATE_RARE = "", GATE_EPIC = "";

    /** 상점에서 이 장비가 잠겨 있으면 "1장" 같은 장 이름, 아니면 null (서버 StoryGates 와 같은 규칙). */
    public static String lockedChapter(net.minecraft.world.item.ItemStack s) {
        if (s.isEmpty() || !(s.isDamageableItem() || s.getItem() instanceof net.minecraft.world.item.ArmorItem)) return null;
        net.minecraft.world.entity.player.Player p = net.minecraft.client.Minecraft.getInstance().player;
        if (p != null && p.isCreative()) return null;
        net.minecraft.world.item.Rarity r = s.getRarity();
        if (r == net.minecraft.world.item.Rarity.EPIC && !GATE_EPIC.isEmpty()) return GATE_EPIC;
        if ((r == net.minecraft.world.item.Rarity.RARE || r == net.minecraft.world.item.Rarity.EPIC) && !GATE_RARE.isEmpty()) return GATE_RARE;
        return null;
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

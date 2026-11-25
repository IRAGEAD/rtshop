package com.rtshop.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 아이템 쓰임새 설명: 마우스를 올리면 맨 아래에 "[Shift] 쓰임새" 가 보이고, Shift 를 누르고 있으면 그 물건이 이 서버에서 어디에 쓰이는지 나온다
 * (파는 곳·사 주는 곳·강화 재료·뽑기·보스 전리품·퀘스트). 내용은 서버가 보낸다 (rules/ItemNotes).
 */
public final class ItemNoteTips {
    private ItemNoteTips() {
    }

    private static volatile Map<String, List<String[]>> notes = new HashMap<>();

    public static void receive(JsonObject o) {
        Map<String, List<String[]>> m = new HashMap<>();
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            List<String[]> lines = new ArrayList<>();
            for (JsonElement le : e.getValue().getAsJsonArray()) {
                JsonArray a = le.getAsJsonArray();
                String[] s = new String[a.size()];
                for (int i = 0; i < s.length; i++) s[i] = a.get(i).getAsString();
                if (s.length > 0) lines.add(s);
            }
            m.put(e.getKey(), lines);
        }
        notes = m;
    }

    public static int countForTest() {
        return notes.size();
    }

    /** 이 아이템의 쓰임새 줄들 (없으면 빈 목록). */
    public static List<Component> lines(ItemStack stack) {
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        List<String[]> src = rl == null ? null : notes.get(rl.toString());
        List<Component> out = new ArrayList<>();
        if (src == null) return out;
        for (String[] s : src) {
            MutableComponent line;
            if (s[0].startsWith("=")) {
                line = Component.literal(s[0].substring(1)).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
            } else {
                Object[] args = new Object[s.length - 1];
                for (int i = 1; i < s.length; i++) {
                    if (s[i].startsWith("@grade:")) args[i - 1] = GachaColors.name(s[i].substring(7));
                    else if (s[i].matches("\\d+")) args[i - 1] = Ui.num(Long.parseLong(s[i]));
                    else args[i - 1] = s[i];
                }
                line = Component.literal("▸ ").append(Component.translatable(s[0], args)).withStyle(ChatFormatting.DARK_AQUA);
            }
            out.add(line);
        }
        return out;
    }

    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack s = event.getItemStack();
        if (s.isEmpty() || notes.isEmpty()) return;
        List<Component> lines = lines(s);
        if (lines.isEmpty()) return;
        List<Component> tip = event.getToolTip();
        if (Screen.hasShiftDown()) {
            tip.addAll(lines);
        } else {
            tip.add(Component.translatable("rtshop.note.hint").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}

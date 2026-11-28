package com.rtshop.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.net.RulesDataPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

/** 조합법·전리품 편집기 공통: 서버 자료 받기. */
public final class RulesScreens {
    private RulesScreens() {
    }

    public static void onData(int kind, String json) {
        Minecraft mc = Minecraft.getInstance();
        JsonObject o = JsonParser.parseString(json).getAsJsonObject();
        readSold(o);
        if (kind == RulesDataPacket.LOOT_FOUND) {
            if (mc.screen instanceof LootEditScreen s) s.applyFound(o);
            return;
        }
        if (kind == RulesDataPacket.RECIPES) {
            if (mc.screen instanceof RecipeEditScreen s) s.apply(o);
            else if (mc.screen instanceof RecipeMakeScreen m && m.parent != null) m.parent.apply(o);
            else mc.setScreen(new RecipeEditScreen(o));
        } else if (kind == RulesDataPacket.LOOT_LIST) {
            mc.setScreen(new LootEditScreen(o));
        } else if (kind == RulesDataPacket.LOOT_TABLE) {
            if (mc.screen instanceof LootEditScreen s) s.applyTable(o);
            else if (mc.screen instanceof PickerScreen p && p.parent instanceof LootEditScreen s) s.applyTable(o);
        }
    }

    /** NPC 상점에서 살 수 있는 아이템 (서버가 편집 창을 열 때 알려 준다). */
    static final java.util.Set<String> SOLD = new java.util.HashSet<>();

    static void readSold(JsonObject o) {
        if (!o.has("sold") || !o.get("sold").isJsonArray()) return;
        SOLD.clear();
        for (com.google.gson.JsonElement e : o.getAsJsonArray("sold")) SOLD.add(e.getAsString());
    }

    /** 아이템 종류 (분류용): 무기 · 방어구 · 도구 · 음식 · 블록 · 그 밖. */
    static final String[] KINDS = {"weapon", "armor", "tool", "food", "block", "other"};
    private static final java.util.Map<String, String> KIND_CACHE = new java.util.HashMap<>();
    private static final java.util.Map<String, String> NAME_LC = new java.util.HashMap<>();

    static String kind(String id) {
        String key = id == null ? "" : id;
        String k = KIND_CACHE.get(key);
        if (k == null) {
            k = kind0(key);
            KIND_CACHE.put(key, k);
        }
        return k;
    }

    private static String kind0(String id) {
        Item it = item(id);
        if (it == Items.BARRIER || it == Items.AIR) return "other";
        try {
            if (it instanceof net.minecraft.world.item.ArmorItem || it instanceof net.minecraft.world.item.ShieldItem
                    || it instanceof net.minecraft.world.item.ElytraItem) return "armor";
            if (it.isEdible()) return "food";
            if (it instanceof net.minecraft.world.item.BlockItem) return "block";
            String wt = com.rtshop.job.JobConfig.get().baseWeaponType(new net.minecraft.world.item.ItemStack(it));   // 3.84: 바닐라 규칙과 상관없이 원래 종류로
            if ("tool".equals(wt)) return "tool";
            if (wt != null && !wt.isEmpty() && !com.rtshop.job.WeaponTypes.NONE.equals(wt) && !com.rtshop.job.WeaponTypes.FIST.equals(wt)) return "weapon";
            if (it instanceof net.minecraft.world.item.DiggerItem || it instanceof net.minecraft.world.item.ShearsItem
                    || it instanceof net.minecraft.world.item.FishingRodItem || it instanceof net.minecraft.world.item.FlintAndSteelItem) return "tool";
            if (it instanceof net.minecraft.world.item.SwordItem || it instanceof net.minecraft.world.item.ProjectileWeaponItem
                    || it instanceof net.minecraft.world.item.TridentItem) return "weapon";
        } catch (Throwable ignored) {
            // 이상한 아이템은 '그 밖'
        }
        return "other";
    }

    static String kindName(String kind) {
        return net.minecraft.network.chat.Component.translatable("rtshop.rules.kind." + kind).getString();
    }

    static int kindColor(String kind) {
        return switch (kind) {
            case "weapon" -> 0xA8412E;
            case "armor" -> 0x4E6A8C;
            case "tool" -> 0x7A6238;
            case "food" -> 0x5E8A3A;
            case "block" -> 0x6E6E6E;
            default -> 0x8A7A60;
        };
    }

    /** 검색용: 소문자로 바꾼 아이템 이름 (한 번 구해 두고 다시 쓴다). */
    static String nameLc(String id) {
        String key = id == null ? "" : id;
        String n = NAME_LC.get(key);
        if (n == null) {
            n = name(key).toLowerCase(java.util.Locale.ROOT);
            NAME_LC.put(key, n);
        }
        return n;
    }

    /** 이름이나 ID 에 이 글자가 든 아이템 ID 들 (많아야 max 개). */
    static java.util.List<String> itemsMatching(String q, int max) {
        java.util.List<String> out = new java.util.ArrayList<>();
        String s = q == null ? "" : q.toLowerCase(java.util.Locale.ROOT).trim();
        if (s.isEmpty()) return out;
        for (ResourceLocation rl : ForgeRegistries.ITEMS.getKeys()) {
            String id = rl.toString();
            if (id.contains(s) || nameLc(id).contains(s)) {
                out.add(id);
                if (out.size() >= max) break;
            }
        }
        return out;
    }

    public static Item item(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id == null ? "" : id);
        return rl == null || !ForgeRegistries.ITEMS.containsKey(rl) ? Items.BARRIER : ForgeRegistries.ITEMS.getValue(rl);
    }

    public static String name(String id) {
        Item it = item(id);
        return it == Items.BARRIER ? id : it.getDescription().getString();
    }
}

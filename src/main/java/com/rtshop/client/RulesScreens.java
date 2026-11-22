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

    public static Item item(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id == null ? "" : id);
        return rl == null || !ForgeRegistries.ITEMS.containsKey(rl) ? Items.BARRIER : ForgeRegistries.ITEMS.getValue(rl);
    }

    public static String name(String id) {
        Item it = item(id);
        return it == Items.BARRIER ? id : it.getDescription().getString();
    }
}

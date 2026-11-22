package com.rtshop.raid;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 보상 한 줄 (레이드·주간 던전 보상 공통): 어느 묶음(group)의, 어떤 등급(grade 0~5)의, 어떤 아이템 몇 개, 확률(chance %).
 * 등급 0~4 는 뽑기 등급과 같고(일반·고급·희귀·영웅·전설), 5 는 검은색 [???] 등급.
 */
public final class Reward {
    public static final String[] GRADES = {"common", "uncommon", "rare", "epic", "legendary", "mystery"};

    public String group = "";
    public int grade;
    public String item = "minecraft:air";
    public int count = 1;
    public String nbt = "";
    /** 확률(%): 쓰는 곳에서만 의미가 있다 (주간 던전 보상). */
    public double chance = 100;

    public Reward() {
    }

    public Reward(String group, int grade, String item, int count) {
        this.group = group;
        this.grade = grade;
        this.item = item;
        this.count = count;
    }

    public static Reward of(String group, int grade, ItemStack s) {
        Reward r = new Reward(group, grade, String.valueOf(ForgeRegistries.ITEMS.getKey(s.getItem())), s.getCount());
        if (s.hasTag()) {
            r.nbt = String.valueOf(s.getTag());
        }
        return r;
    }

    /** 아이템이 이 게임에 없으면 빈 묶음. */
    public ItemStack stack() {
        ResourceLocation rl = ResourceLocation.tryParse(item);
        Item it = rl == null ? null : ForgeRegistries.ITEMS.getValue(rl);
        if (it == null || it == Items.AIR) {
            return ItemStack.EMPTY;
        }
        ItemStack s = new ItemStack(it, Math.max(1, count));
        if (nbt != null && !nbt.isBlank()) {
            try {
                CompoundTag t = TagParser.parseTag(nbt);
                s.setTag(t);
            } catch (Exception ignored) {
            }
        }
        return s;
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("group", group);
        o.addProperty("grade", grade);
        o.addProperty("item", item);
        o.addProperty("count", count);
        if (nbt != null && !nbt.isBlank()) {
            o.addProperty("nbt", nbt);
        }
        o.addProperty("chance", chance);
        return o;
    }

    public static Reward parse(JsonObject o) {
        Reward r = new Reward();
        if (o.has("group")) r.group = o.get("group").getAsString();
        if (o.has("grade")) r.grade = Math.max(0, Math.min(GRADES.length - 1, o.get("grade").getAsInt()));
        if (o.has("item")) r.item = o.get("item").getAsString();
        if (o.has("count")) r.count = Math.max(1, Math.min(6400, o.get("count").getAsInt()));
        if (o.has("nbt")) r.nbt = o.get("nbt").getAsString();
        if (o.has("chance")) r.chance = Math.max(0, Math.min(100, o.get("chance").getAsDouble()));
        return r;
    }

    /** 아이템을 준다 (가방이 차면 발밑에 떨어뜨린다). */
    public static void give(ServerPlayer p, ItemStack stack) {
        int left = stack.getCount();
        while (left > 0) {
            ItemStack part = stack.copy();
            part.setCount(Math.min(left, part.getMaxStackSize()));
            left -= part.getCount();
            if (!p.getInventory().add(part) && !part.isEmpty()) {
                p.drop(part, false);
            }
        }
    }
}

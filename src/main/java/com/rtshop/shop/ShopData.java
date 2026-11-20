package com.rtshop.shop;

import com.rtshop.farm.SeedRules;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** 상점 한 개의 전체 데이터(서버가 로드하고, 열 때 클라이언트로 전송). 가격 단위는 Gold(G). */
public final class ShopData {

    public static final String FISH_INFO_TAG = "starcatcher:caught_fish_info";

    public static final class Category {
        public final String id;
        public final String name;
        public final ItemStack icon;

        public Category(String id, String name, ItemStack icon) {
            this.id = id;
            this.name = name;
            this.icon = icon;
        }
    }

    /** 가격의 근거가 된 조리법의 재료 한 가지 (화면에 "이 재료 기준으로 값을 매겼다"를 보여 주는 용도). */
    public static final class Part {
        public final Item item;
        public final float count;

        public Part(Item item, float count) {
            this.item = item;
            this.count = count;
        }
    }

    public static final class Entry {
        public final ItemStack stack;  // 개수 1 기준 견본
        public final int price;        // 1개당 구매 가격(G)
        public final double sellUnit;  // 1개당 기본 매입가(G). 품질 배율 적용 전
        public final String category;
        /** true 면 판매 시 NBT(별 등급, 무게 등)를 무시하고 같은 아이템이면 받는다. 손상·인챈트된 것은 제외. */
        public final boolean looseSell;
        public final boolean buyable;
        public final boolean sellable;
        /** 물고기 품목(무게 정보가 없으면 낮은 배율 적용). */
        public final boolean fish;
        /** 만들 수 있는 품목이면: 매입가를 계산한 조리법의 재료(가장 싼 것 기준)와 1회 생산량. 없으면 빈 목록. */
        public final List<Part> recipe;
        public final float recipeOut;

        public Entry(ItemStack stack, int price, double sellUnit, String category, boolean looseSell,
                     boolean buyable, boolean sellable, boolean fish) {
            this(stack, price, sellUnit, category, looseSell, buyable, sellable, fish, List.of(), 1.0F);
        }

        public Entry(ItemStack stack, int price, double sellUnit, String category, boolean looseSell,
                     boolean buyable, boolean sellable, boolean fish, List<Part> recipe, float recipeOut) {
            this.stack = stack;
            this.price = price;
            this.sellUnit = sellUnit;
            this.category = category;
            this.looseSell = looseSell;
            this.buyable = buyable;
            this.sellable = sellable;
            this.fish = fish;
            this.recipe = recipe;
            this.recipeOut = recipeOut;
        }

        public boolean matchesExact(ItemStack s) {
            return !s.isEmpty() && ItemStack.isSameItemSameTags(s, stack);
        }

        public boolean matchesLoose(ItemStack s) {
            return looseSell && !s.isEmpty() && s.is(stack.getItem()) && !s.isDamaged() && !s.isEnchanted()
                    && !SeedRules.isLicensed(s);
        }
    }

    public final String id;
    public final int version;
    public final String title;
    public final String npcName;
    public final String profession;
    public final List<Category> categories;
    public final List<Entry> entries;
    // 품질 배율 (서버 설정을 클라이언트에 그대로 전달해 같은 가격을 보여 준다)
    public final double[] starMult;
    /** 낚은 물고기(무게 정보가 있는 것)의 기본 배율과 무게에 따른 변동 폭. */
    public final double fishBase;
    public final double fishBonus;
    public final double fishGolden;
    public final double fishNoInfo;

    public ShopData(String id, int version, String title, String npcName, String profession,
                    List<Category> categories, List<Entry> entries,
                    double[] starMult, double fishBase, double fishBonus, double fishGolden, double fishNoInfo) {
        this.id = id;
        this.version = version;
        this.title = title;
        this.npcName = npcName;
        this.profession = profession;
        this.categories = categories;
        this.entries = entries;
        this.starMult = starMult;
        this.fishBase = fishBase;
        this.fishBonus = fishBonus;
        this.fishGolden = fishGolden;
        this.fishNoInfo = fishNoInfo;
    }

    /** 이 아이템을 팔 수 있는 품목의 번호. 없으면 -1. NBT 가 정확히 같은 품목이 우선. */
    public int findSellEntry(ItemStack s) {
        if (s.isEmpty()) {
            return -1;
        }
        int loose = -1;
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            if (!e.sellable || !s.is(e.stack.getItem())) {
                continue;
            }
            if (e.matchesExact(s)) {
                return i;
            }
            if (loose < 0 && e.matchesLoose(s)) {
                loose = i;
            }
        }
        return loose;
    }

    /** 품질 배율(기준 매입가에 곱하는 값): 작물 별 등급, 물고기 무게. 품질 정보가 없으면 1.0. */
    public double qualityMultiplier(Entry e, ItemStack s) {
        int star = SeedRules.star(s);
        if (star > 0) {
            return starMult[Math.min(2, star - 1)];
        }
        CompoundTag tag = s.getTag();
        if (tag != null && tag.contains(FISH_INFO_TAG, 10)) {
            CompoundTag info = tag.getCompound(FISH_INFO_TAG);
            if ("golden".equals(info.getString("rarity"))) {
                return fishGolden;
            }
            double p = Math.max(0, Math.min(100, info.getFloat("percentile")));
            return fishBase * (1.0 + fishBonus * (50.0 - p) / 50.0); // 백분위 0 = 가장 무거움
        }
        return e.fish ? fishNoInfo : 1.0;
    }

    /** 이 아이템 1개의 매입가. */
    public double sellUnitFor(Entry e, ItemStack s) {
        return e.sellUnit * qualityMultiplier(e, s);
    }

    /** 판매 목록에서 같은 줄로 묶는 기준(1/100 G 단위 매입가). */
    public long variantKey(Entry e, ItemStack s) {
        return Math.round(sellUnitFor(e, s) * 100.0);
    }

    public static long revenue(double unit, long amount) {
        return (long) Math.floor(unit * amount + 1.0e-6);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(id);
        buf.writeVarInt(version);
        buf.writeUtf(title);
        for (int i = 0; i < 3; i++) {
            buf.writeDouble(starMult[i]);
        }
        buf.writeDouble(fishBase);
        buf.writeDouble(fishBonus);
        buf.writeDouble(fishGolden);
        buf.writeDouble(fishNoInfo);
        buf.writeVarInt(categories.size());
        for (Category c : categories) {
            buf.writeUtf(c.id);
            buf.writeUtf(c.name);
            buf.writeItem(c.icon);
        }
        buf.writeVarInt(entries.size());
        for (Entry e : entries) {
            buf.writeItem(e.stack);
            buf.writeVarInt(e.price);
            buf.writeDouble(e.sellUnit);
            buf.writeUtf(e.category);
            int flags = (e.looseSell ? 1 : 0) | (e.buyable ? 2 : 0) | (e.sellable ? 4 : 0) | (e.fish ? 8 : 0);
            buf.writeByte(flags);
            int parts = Math.min(e.recipe.size(), 12);
            buf.writeVarInt(parts);
            if (parts > 0) {
                buf.writeFloat(e.recipeOut);
                for (int i = 0; i < parts; i++) {
                    Part part = e.recipe.get(i);
                    buf.writeVarInt(Item.getId(part.item));
                    buf.writeFloat(part.count);
                }
            }
        }
    }

    public static ShopData read(FriendlyByteBuf buf) {
        String id = buf.readUtf();
        int version = buf.readVarInt();
        String title = buf.readUtf();
        double[] star = new double[3];
        for (int i = 0; i < 3; i++) {
            star[i] = buf.readDouble();
        }
        double fishBase = buf.readDouble();
        double fishBonus = buf.readDouble();
        double fishGolden = buf.readDouble();
        double fishNoInfo = buf.readDouble();
        int cn = buf.readVarInt();
        List<Category> cats = new ArrayList<>(cn);
        for (int i = 0; i < cn; i++) {
            String cid = buf.readUtf();
            String name = buf.readUtf();
            ItemStack icon = buf.readItem();
            cats.add(new Category(cid, name, icon));
        }
        int en = buf.readVarInt();
        List<Entry> ents = new ArrayList<>(en);
        for (int i = 0; i < en; i++) {
            ItemStack st = buf.readItem();
            int price = buf.readVarInt();
            double sellUnit = buf.readDouble();
            String cat = buf.readUtf();
            int flags = buf.readByte();
            int parts = buf.readVarInt();
            List<Part> recipe = List.of();
            float recipeOut = 1.0F;
            if (parts > 0) {
                recipeOut = buf.readFloat();
                recipe = new ArrayList<>(parts);
                for (int k = 0; k < parts; k++) {
                    Item item = Item.byId(buf.readVarInt());
                    float count = buf.readFloat();
                    if (item != Items.AIR) {
                        recipe.add(new Part(item, count));
                    }
                }
            }
            ents.add(new Entry(st, price, sellUnit, cat, (flags & 1) != 0, (flags & 2) != 0, (flags & 4) != 0, (flags & 8) != 0,
                    recipe, recipeOut));
        }
        return new ShopData(id, version, title, "", "", cats, ents, star, fishBase, fishBonus, fishGolden, fishNoInfo);
    }
}

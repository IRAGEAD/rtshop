package com.rtshop.shop;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** 상점 한 개의 전체 데이터(서버가 로드하고, 열 때 클라이언트로 전송). 가격 단위는 Gold(G). */
public final class ShopData {

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

    public static final class Entry {
        public final ItemStack stack; // 개수 1 기준 견본
        public final int price;       // 1개당 구매 가격(G)
        public final String category;
        /** true 면 판매 시 NBT 를 무시(크기/무게가 붙는 물고기 등). 단, 손상되었거나 인챈트된 아이템은 제외. */
        public final boolean looseSell;

        public Entry(ItemStack stack, int price, String category, boolean looseSell) {
            this.stack = stack;
            this.price = price;
            this.category = category;
            this.looseSell = looseSell;
        }

        /** 플레이어가 가진 s 를 이 항목으로 판매할 수 있는가. 서버/클라이언트 공통 규칙. */
        public boolean matches(ItemStack s) {
            if (s.isEmpty()) {
                return false;
            }
            if (looseSell) {
                return s.is(stack.getItem()) && !s.isDamaged() && !s.isEnchanted();
            }
            return ItemStack.isSameItemSameTags(s, stack);
        }
    }

    public final String id;
    public final int version;
    public final String title;
    public final String npcName;
    public final String profession;
    public final double sellRatio;
    public final List<Category> categories;
    public final List<Entry> entries;

    public ShopData(String id, int version, String title, String npcName, String profession, double sellRatio,
                    List<Category> categories, List<Entry> entries) {
        this.id = id;
        this.version = version;
        this.title = title;
        this.npcName = npcName;
        this.profession = profession;
        this.sellRatio = sellRatio;
        this.categories = categories;
        this.entries = entries;
    }

    /** 가격 * 비율 * 수량 (소수점 버림). */
    public long sellRevenue(int unitPrice, long amount) {
        return (long) Math.floor(unitPrice * sellRatio * amount + 1.0e-9);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(id);
        buf.writeVarInt(version);
        buf.writeUtf(title);
        buf.writeDouble(sellRatio);
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
            buf.writeUtf(e.category);
            buf.writeBoolean(e.looseSell);
        }
    }

    public static ShopData read(FriendlyByteBuf buf) {
        String id = buf.readUtf();
        int version = buf.readVarInt();
        String title = buf.readUtf();
        double ratio = buf.readDouble();
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
            String cat = buf.readUtf();
            boolean loose = buf.readBoolean();
            ents.add(new Entry(st, price, cat, loose));
        }
        return new ShopData(id, version, title, "", "", ratio, cats, ents);
    }
}

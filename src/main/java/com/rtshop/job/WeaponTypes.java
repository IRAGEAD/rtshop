package com.rtshop.job;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 무기 종류 분류. 직업별 무기 상성(피해·공격 속도 증감)은 이 종류 단위로 정한다.
 * 분류 순서: 관리자가 정한 아이템별 예외 -> 종류 정의의 규칙(위에서부터 처음 맞는 것). 맨손은 "fist".
 * "magic" 은 무기가 아니라 주문 피해(Iron's Spells, Wizards 등)에 쓰는 특별한 종류다.
 */
public final class WeaponTypes {
    private WeaponTypes() {
    }

    public static final String MAGIC = "magic";
    public static final String FIST = "fist";
    public static final String NONE = "none";
    /** 뽑기의 고급 무기(야마토·무라사마 등): 어느 직업의 무기 분류에도 들지 않는다 — 직업의 보정도 불이익도 받지 않는다 (3.50). */
    public static final String PREMIUM = "premium";

    /** 무기 종류 하나: 화면 이름, 아이콘, 근접인지(공격 속도 적용 여부), 분류 규칙. */
    public static final class Type {
        public String id;
        public String name;
        public String icon = "minecraft:iron_sword";
        public boolean melee = true;
        /** item 클래스: sword, axe, bow, crossbow, trident, ranged(그 밖 발사 무기), tool(곡괭이·삽·괭이) */
        public final List<String> classes = new ArrayList<>();
        /** 아이템 ID 의 이름 부분에 이 글자가 들어 있으면 */
        public final List<String> nameHas = new ArrayList<>();
        /** 아이템 ID 의 이름 부분이 이 글자로 끝나면 */
        public final List<String> nameEnds = new ArrayList<>();
        /** 이 모드의 아이템이면 */
        public final List<String> mods = new ArrayList<>();
    }

    public static List<Type> defaults() {
        List<Type> l = new ArrayList<>();
        l.add(type("gun", "총", "minecraft:crossbow", false, new String[]{}, new String[]{}, new String[]{}, new String[]{"tacz"}));
        l.add(type("crossbow", "석궁", "minecraft:crossbow", false, new String[]{"crossbow"}, new String[]{"crossbow"}, new String[]{}, new String[]{}));
        l.add(type("bow", "활", "minecraft:bow", false, new String[]{"bow", "ranged"}, new String[]{"longbow", "shortbow", "recurve"},
                new String[]{"_bow"}, new String[]{}));
        l.add(type("staff", "지팡이·마법서", "minecraft:blaze_rod", true, new String[]{},
                new String[]{"staff", "wand", "scepter", "spell_book", "spellbook", "grimoire", "tome", "cane", "orb"}, new String[]{}, new String[]{}));
        l.add(type("dagger", "단검", "minecraft:iron_sword", true, new String[]{},
                new String[]{"dagger", "knife", "sickle", "kunai", "sai", "tanto", "dirk"}, new String[]{}, new String[]{}));
        l.add(type("spear", "창", "minecraft:trident", true, new String[]{"trident"},
                new String[]{"spear", "glaive", "halberd", "lance", "pike", "naginata", "scythe", "trident", "polearm", "javelin"}, new String[]{}, new String[]{}));
        l.add(type("blunt", "둔기", "minecraft:bone", true, new String[]{},
                new String[]{"mace", "hammer", "club", "flail", "morning_star", "warhammer", "maul", "cudgel", "staff_of"}, new String[]{}, new String[]{}));
        l.add(type("fist", "주먹·너클", "minecraft:leather", true, new String[]{},
                new String[]{"knuckle", "gauntlet", "claw", "fist", "cestus", "tonfa"}, new String[]{}, new String[]{}));
        l.add(type("tool", "채집 도구", "minecraft:iron_pickaxe", true, new String[]{"tool"}, new String[]{"pickaxe", "shovel", "hoe", "paxel"},
                new String[]{}, new String[]{}));
        l.add(type("axe", "도끼", "minecraft:iron_axe", true, new String[]{"axe"}, new String[]{"axe", "hatchet", "tomahawk"}, new String[]{}, new String[]{}));
        l.add(type("sword", "검", "minecraft:iron_sword", true, new String[]{"sword"},
                new String[]{"sword", "claymore", "katana", "rapier", "saber", "sabre", "blade", "cutlass", "scimitar", "longsword", "greatsword",
                        "uchigatana", "tachi", "estoc", "falchion"}, new String[]{}, new String[]{}));
        return l;
    }

    private static Type type(String id, String name, String icon, boolean melee, String[] classes, String[] has, String[] ends, String[] mods) {
        Type t = new Type();
        t.id = id;
        t.name = name;
        t.icon = icon;
        t.melee = melee;
        t.classes.addAll(List.of(classes));
        t.nameHas.addAll(List.of(has));
        t.nameEnds.addAll(List.of(ends));
        t.mods.addAll(List.of(mods));
        return t;
    }

    // ------------------------------------------------------------------ JSON

    private static void strings(JsonObject o, String key, List<String> out) {
        if (o.has(key) && o.get(key).isJsonArray()) {
            for (JsonElement e : o.getAsJsonArray(key)) {
                out.add(e.getAsString().toLowerCase(Locale.ROOT));
            }
        }
    }

    public static Type read(JsonObject o) {
        Type t = new Type();
        t.id = o.get("id").getAsString();
        t.name = o.has("name") ? o.get("name").getAsString() : t.id;
        t.icon = o.has("icon") ? o.get("icon").getAsString() : t.icon;
        t.melee = !o.has("melee") || o.get("melee").getAsBoolean();
        strings(o, "classes", t.classes);
        strings(o, "nameHas", t.nameHas);
        strings(o, "nameEnds", t.nameEnds);
        strings(o, "mods", t.mods);
        return t;
    }

    private static JsonArray arr(List<String> l) {
        JsonArray a = new JsonArray();
        l.forEach(a::add);
        return a;
    }

    public static JsonObject write(Type t) {
        JsonObject o = new JsonObject();
        o.addProperty("id", t.id);
        o.addProperty("name", t.name);
        o.addProperty("icon", t.icon);
        o.addProperty("melee", t.melee);
        o.add("classes", arr(t.classes));
        o.add("nameHas", arr(t.nameHas));
        o.add("nameEnds", arr(t.nameEnds));
        o.add("mods", arr(t.mods));
        return o;
    }

    // ------------------------------------------------------------------ 분류

    private static boolean classMatch(Item item, String cls) {
        return switch (cls) {
            case "sword" -> item instanceof SwordItem;
            case "axe" -> item instanceof AxeItem;
            case "bow" -> item instanceof BowItem;
            case "crossbow" -> item instanceof CrossbowItem;
            case "trident" -> item instanceof TridentItem;
            case "ranged" -> item instanceof ProjectileWeaponItem;
            case "tool" -> item instanceof DiggerItem && !(item instanceof AxeItem);
            default -> false;
        };
    }

    /** 아이템의 무기 종류 ID. 무기가 아니면 "none", 빈손이면 "fist". 결과는 아이템별로 기억한다 (설정을 다시 읽으면 지움). */
    public static String categoryOf(ItemStack stack, List<Type> types, Map<String, String> overrides, Map<Item, String> cache) {
        if (stack.isEmpty()) {
            return FIST;
        }
        Item item = stack.getItem();
        String hit = cache.get(item);
        if (hit != null) {
            return hit;
        }
        hit = classify(item, types, overrides);
        cache.put(item, hit);
        return hit;
    }

    public static String classify(Item item, List<Type> types, Map<String, String> overrides) {
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
        String id = rl == null ? "" : rl.toString();
        String over = overrides.get(id);
        if (over != null) {
            return over;
        }
        String ns = rl == null ? "" : rl.getNamespace();
        String path = rl == null ? "" : rl.getPath().toLowerCase(Locale.ROOT);
        for (Type t : types) {
            if (t.mods.contains(ns)) {
                return t.id;
            }
        }
        if (!weaponLike(item)) {
            return NONE; // 무기가 아닌 물건(사탕수수 등)이 이름 때문에 무기로 분류되지 않게
        }
        for (Type t : types) {
            for (String c : t.classes) {
                if (classMatch(item, c)) {
                    return t.id;
                }
            }
            for (String h : t.nameHas) {
                if (path.contains(h)) {
                    return t.id;
                }
            }
            for (String e : t.nameEnds) {
                if (path.endsWith(e)) {
                    return t.id;
                }
            }
        }
        return NONE;
    }

    /** 공격력 수치가 붙어 있거나 발사 무기인 아이템. */
    public static boolean weaponLike(Item item) {
        if (item instanceof ProjectileWeaponItem || item instanceof TridentItem) {
            return true;
        }
        try {
            return new ItemStack(item).getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND)
                    .containsKey(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        } catch (Throwable t) {
            return false;
        }
    }

    /** 주문(마법) 피해인가: 바닐라 마법 피해, 또는 마법 모드가 만든 피해. */
    public static boolean isMagic(DamageSource source) {
        String msg = source.getMsgId() == null ? "" : source.getMsgId().toLowerCase(Locale.ROOT);
        if (msg.equals("magic") || msg.equals("indirectmagic") || msg.contains("spell")) {
            return true;
        }
        try {
            ResourceLocation type = source.typeHolder().unwrapKey().map(k -> k.location()).orElse(null);
            if (type != null) {
                String ns = type.getNamespace();
                return ns.equals("irons_spellbooks") || ns.equals("spell_engine") || ns.equals("spell_power") || ns.equals("wizards")
                        || type.getPath().contains("spell") || type.getPath().equals("magic") || type.getPath().equals("indirect_magic");
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static Map<Item, String> newCache() {
        return new HashMap<>();
    }
}

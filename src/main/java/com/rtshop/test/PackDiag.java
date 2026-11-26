package com.rtshop.test;

import com.rtshop.shop.RecipeSolver;
import com.rtshop.shop.ShopLoader;
import com.rtshop.shop.ShopServer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** 모드팩 점검용 자료 (config/rtshop/export/diag.txt): 가격 근거, 뽑기 후보 아이템·생물의 성능. */
final class PackDiag {
    private PackDiag() {
    }

    private static final Pattern MODS = Pattern.compile(
            "(?i).*(nightfall|shadow|lesraisin|lrmelee|melee|artifact|fantasy|hamster|dragon|mount|avalon|abyss|efn).*");

    static void write(MinecraftServer server) {
        List<String> out = new ArrayList<>();
        out.add("== mods");
        List<String> picked = new ArrayList<>();
        ModList.get().getMods().forEach(m -> {
            out.add(m.getModId() + " | " + m.getDisplayName() + " | " + m.getVersion());
            if (MODS.matcher(m.getModId()).matches() || MODS.matcher(m.getDisplayName()).matches()) {
                picked.add(m.getModId());
            }
        });
        out.add("picked: " + picked);
        try {
            prices(out);
        } catch (Throwable t) {
            out.add("prices failed: " + t);
        }
        try {
            gear(server, out);
        } catch (Throwable t) {
            out.add("gear failed: " + t);
        }
        try {
            loops(out);
        } catch (Throwable t) {
            out.add("loops failed: " + t);
        }
        out.add("== lrtactical melee variants");
        try {
            for (ItemStack v : com.rtshop.gacha.GachaConfig.variantsForDiag("lrtactical:melee")) {
                out.add(v.getHoverName().getString() + " | " + v.getTag());
            }
        } catch (Throwable t) {
            out.add("variants failed: " + t);
        }
        out.add("== shadow items");
        for (Item item : ForgeRegistries.ITEMS) {
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
            try {
                String n = new ItemStack(item).getHoverName().getString().toLowerCase(Locale.ROOT);
                if (rl != null && (n.contains("shadow") || rl.getPath().contains("shadow"))) {
                    out.add(rl + " | " + n + " | " + item.getClass().getName());
                }
            } catch (Throwable ignored) {
            }
        }
        out.add("== items");
        for (Item item : ForgeRegistries.ITEMS) {
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
            if (rl == null || item == Items.AIR || !picked.contains(rl.getNamespace()) || rl.getNamespace().equals("simplehats")) {
                continue;
            }
            try {
                ItemStack s = new ItemStack(item);
                double dmg = 0;
                double spd = 0;
                for (var m : s.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE)) {
                    dmg += m.getAmount();
                }
                for (var m : s.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_SPEED)) {
                    spd += m.getAmount();
                }
                StringBuilder tags = new StringBuilder();
                for (TagKey<Item> tag : item.builtInRegistryHolder().tags().toList()) {
                    String ns = tag.location().getNamespace();
                    if (ns.equals("curios") || ns.equals("forge") && tag.location().getPath().startsWith("tools")) {
                        tags.append(tag.location()).append(' ');
                    }
                }
                out.add(String.format(Locale.ROOT, "%s | %s | %s | dmg %.1f spd %.2f dur %d | %s | %s | %s",
                        rl, s.getHoverName().getString(), s.getRarity(), dmg, spd, s.getMaxDamage(),
                        item.getClass().getName(), superName(item.getClass()), tags.toString().trim()));
            } catch (Throwable t) {
                out.add(rl + " | error " + t);
            }
        }
        out.add("== entities");
        for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES) {
            ResourceLocation rl = ForgeRegistries.ENTITY_TYPES.getKey(type);
            if (rl == null) {
                continue;
            }
            boolean mine = picked.contains(rl.getNamespace());
            try {
                Entity e = type.create(server.overworld());
                if (e == null) {
                    continue;
                }
                String cls = chain(e.getClass());
                boolean tame = e instanceof net.minecraft.world.entity.TamableAnimal
                        || e instanceof net.minecraft.world.entity.OwnableEntity
                        || e instanceof net.minecraft.world.entity.animal.horse.AbstractHorse || cls.contains("FantasyBeast");
                e.discard();
                if (!mine && !tame) {
                    continue;
                }
                StringBuilder at = new StringBuilder();
                if (e instanceof LivingEntity) {
                    @SuppressWarnings("unchecked")
                    EntityType<? extends LivingEntity> lt = (EntityType<? extends LivingEntity>) type;
                    if (DefaultAttributes.hasSupplier(lt)) {
                        var sup = DefaultAttributes.getSupplier(lt);
                        for (Attribute a : ForgeRegistries.ATTRIBUTES) {
                            if (sup.hasAttribute(a)) {
                                double v = sup.getBaseValue(a);
                                ResourceLocation an = ForgeRegistries.ATTRIBUTES.getKey(a);
                                if (an != null && (v != 0 || an.getPath().contains("health"))) {
                                    String n = an.getNamespace().equals("minecraft") ? an.getPath() : an.toString();
                                    if (n.startsWith("generic.") || n.startsWith("horse.") || !an.getNamespace().equals("minecraft")) {
                                        at.append(n.replace("generic.", "")).append('=')
                                                .append(String.format(Locale.ROOT, "%.3f", v)).append(' ');
                                    }
                                }
                            }
                        }
                    }
                }
                out.add(rl + " | " + type.getDescription().getString() + " | " + type.getCategory() + " | tame=" + tame
                        + " | " + at.toString().trim() + " | " + cls);
            } catch (Throwable t) {
                out.add(rl + " | error " + t);
            }
        }
        try {
            Path p = FMLPaths.CONFIGDIR.get().resolve("rtshop/export/diag.txt");
            Files.createDirectories(p.getParent());
            Files.write(p, out, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    private static String superName(Class<?> c) {
        Class<?> s = c.getSuperclass();
        return s == null ? "" : s.getName();
    }

    private static String chain(Class<?> c) {
        StringBuilder sb = new StringBuilder();
        int n = 0;
        while (c != null && n < 6) {
            if (sb.length() > 0) {
                sb.append(" < ");
            }
            sb.append(c.getSimpleName());
            c = c.getSuperclass();
            n++;
        }
        return sb.toString();
    }

    private static final String[] PRICE_IDS = {"minecraft:iron_ingot", "minecraft:gold_ingot", "minecraft:netherite_ingot",
            "minecraft:copper_ingot", "minecraft:diamond", "minecraft:iron_sword", "minecraft:iron_chestplate",
            "minecraft:golden_sword", "minecraft:diamond_sword", "minecraft:netherite_sword", "minecraft:iron_pickaxe",
            "minecraft:raw_iron", "minecraft:iron_nugget", "minecraft:iron_block", "minecraft:gold_nugget", "spelunkery:raw_iron_nugget", "spelunkery:raw_gold_nugget", "minecraft:raw_gold"};

    /**
     * 밸런스 검토용 장비 비교표: 무기는 공격력(맨손 1 포함)·초당 공격 횟수·DPS, 방어구는 방어력·방어 강도,
     * 그리고 얻는 길(NPC 상점 구매가, 뽑기 등급, 조합 가능 여부, 보스 전리품).
     */
    private static void gear(MinecraftServer server, List<String> out) {
        ShopLoader.Loaded l = ShopServer.loaded();
        java.util.Map<String, Integer> shop = new java.util.HashMap<>();
        if (l != null) {
            for (com.rtshop.shop.ShopData sd : l.shops.values()) {
                for (com.rtshop.shop.ShopData.Entry e : sd.entries) {
                    if (!e.buyable || e.stack.isEmpty()) continue;
                    ResourceLocation id = ForgeRegistries.ITEMS.getKey(e.stack.getItem());
                    if (id != null) shop.merge(id.toString(), e.price, Math::min);
                }
            }
        }
        java.util.Map<String, String> gacha = new java.util.HashMap<>();
        for (var en : com.rtshop.gacha.GachaConfig.get().pools.entrySet()) {
            for (var e : en.getValue()) {
                if (e.item == null || e.item.isEmpty()) continue;
                gacha.merge(e.item, e.grade, (a, b) -> java.util.Arrays.asList(a.split("/")).contains(b) ? a : a + "/" + b);
            }
        }
        java.util.Set<String> craft = new java.util.HashSet<>();
        var access = server.registryAccess();
        for (var r : server.getRecipeManager().getRecipes()) {
            try {
                ItemStack res = r.getResultItem(access);
                if (res == null || res.isEmpty()) continue;
                ResourceLocation oid = ForgeRegistries.ITEMS.getKey(res.getItem());
                if (oid != null && !com.rtshop.rules.ItemPolicy.recipeBanned(r.getId(), oid)) craft.add(oid.toString());
            } catch (Throwable ignored) {
            }
        }
        out.add("== weapons (id | name | dmg | hits/s | dps | rarity | kind | shop | gacha | craft | boss)");
        int n = 0;
        for (Item item : ForgeRegistries.ITEMS) {
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
            if (rl == null || item == Items.AIR) continue;
            try {
                ItemStack s = new ItemStack(item);
                if (s.getMaxStackSize() > 1) continue;
                double dmg = 0;
                double spd = 0;
                for (var m : s.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE)) dmg += m.getAmount();
                for (var m : s.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_SPEED)) spd += m.getAmount();
                String id = rl.toString();
                String kind = com.rtshop.rules.ItemPolicy.gun(item) ? "gun" : com.rtshop.rules.ItemPolicy.staff(item) ? "staff"
                        : item instanceof net.minecraft.world.item.ProjectileWeaponItem ? "ranged" : "melee";
                boolean known = shop.containsKey(id) || craft.contains(id) || gacha.containsKey(id);
                if (!(dmg >= 6 || gacha.containsKey(id) || !kind.equals("melee") && known)) continue;
                double total = 1 + dmg;
                double per = Math.max(0.1, 4 + spd);
                out.add(String.format(Locale.ROOT, "%s | %s | %.1f | %.2f | %.1f | %s | %s | %s | %s | %s | %s", id,
                        s.getHoverName().getString(), total, per, total * per, s.getRarity(), kind,
                        shop.containsKey(id) ? String.valueOf(shop.get(id)) : "-", gacha.getOrDefault(id, "-"),
                        craft.contains(id) ? "Y" : "-", com.rtshop.rules.ItemPolicy.bossItem(item) ? "Y" : "-"));
                n++;
            } catch (Throwable t) {
                out.add(rl + " | error " + t);
            }
        }
        out.add("weapons listed: " + n);
        // 3.65.0: 강화에서 원거리 · 마법 무기로 보는 것 (쏜 것 / 주문의 피해가 오른다)
        StringBuilder rk = new StringBuilder();
        StringBuilder mk = new StringBuilder();
        for (Item item : ForgeRegistries.ITEMS) {
            try {
                com.rtshop.enhance.Enhance.Kind k = com.rtshop.enhance.Enhance.kind(new ItemStack(item));
                if (k == com.rtshop.enhance.Enhance.Kind.RANGED) rk.append(' ').append(ForgeRegistries.ITEMS.getKey(item));
                if (k == com.rtshop.enhance.Enhance.Kind.MAGIC) mk.append(' ').append(ForgeRegistries.ITEMS.getKey(item));
            } catch (Throwable ignored) {
            }
        }
        out.add("== enhance ranged:" + rk);
        out.add("== enhance magic:" + mk);
        extreme(server, out, craft);
        out.add("== armor (id | name | slot | armor | toughness | shop | craft | boss)");
        n = 0;
        for (Item item : ForgeRegistries.ITEMS) {
            if (!(item instanceof net.minecraft.world.item.ArmorItem a)) continue;
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
            if (rl == null) continue;
            try {
                ItemStack s = new ItemStack(item);
                EquipmentSlot es = a.getEquipmentSlot();
                double ar = 0;
                double to = 0;
                for (var m : s.getAttributeModifiers(es).get(Attributes.ARMOR)) ar += m.getAmount();
                for (var m : s.getAttributeModifiers(es).get(Attributes.ARMOR_TOUGHNESS)) to += m.getAmount();
                if (ar < 3 && to < 2) continue;
                String id = rl.toString();
                out.add(String.format(Locale.ROOT, "%s | %s | %s | %.1f | %.1f | %s | %s | %s", id, s.getHoverName().getString(),
                        es.getName(), ar, to, shop.containsKey(id) ? String.valueOf(shop.get(id)) : "-",
                        craft.contains(id) ? "Y" : "-", com.rtshop.rules.ItemPolicy.bossItem(item) ? "Y" : "-"));
                n++;
            } catch (Throwable t) {
                out.add(rl + " | error " + t);
            }
        }
        out.add("armor listed: " + n);
    }

    /**
     * 터무니없이 센 장비(공격력 30 이상 · 방어력 30 이상, 대부분 Arcane Vortex)를 정말 얻을 수 없는지: 그것을 만드는 조합법(막혔는지),
     * 그것이 든 전리품표(전리품 규칙이 빼는지)를 적는다. 전리품표는 JSON 으로 바꿔 아이템 ID 를 찾는다.
     */
    private static void extreme(MinecraftServer server, List<String> out, java.util.Set<String> craftable) {
        java.util.Set<String> ids = new java.util.TreeSet<>();
        for (Item item : ForgeRegistries.ITEMS) {
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
            if (rl == null || item == Items.AIR) continue;
            try {
                ItemStack s = new ItemStack(item);
                double dmg = 0;
                for (var m : s.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE)) dmg += m.getAmount();
                double ar = 0;
                if (item instanceof net.minecraft.world.item.ArmorItem a) {
                    for (var m : s.getAttributeModifiers(a.getEquipmentSlot()).get(Attributes.ARMOR)) ar += m.getAmount();
                }
                if (dmg >= 30 || ar >= 30) ids.add(rl.toString());
            } catch (Throwable ignored) {
            }
        }
        out.add("== extreme gear sources (" + ids.size() + " items with attack or armor 30+)");
        int recipes = 0;
        for (var r : server.getRecipeManager().getRecipes()) {
            try {
                ResourceLocation oid = com.rtshop.rules.RecipeRules.outputId(r, server);
                if (oid == null || !ids.contains(oid.toString())) continue;
                recipes++;
                out.add("recipe " + r.getId() + " (" + net.minecraftforge.registries.ForgeRegistries.RECIPE_SERIALIZERS.getKey(r.getSerializer()) + ") -> " + oid
                        + (com.rtshop.rules.ItemPolicy.recipeBanned(r.getId(), oid) ? " BANNED" : " OPEN"));
            } catch (Throwable ignored) {
            }
        }
        int tables = 0;
        int failed = 0;
        com.google.gson.Gson gson = net.minecraft.world.level.storage.loot.Deserializers.createLootTableSerializer().create();
        for (ResourceLocation key : server.getLootData().getKeys(net.minecraft.world.level.storage.loot.LootDataType.TABLE)) {
            String json;
            try {
                json = gson.toJsonTree(server.getLootData().getLootTable(key)).toString();
            } catch (Throwable t) {
                failed++;
                continue;
            }
            for (String id : ids) {
                if (!json.contains("\"" + id + "\"")) continue;
                tables++;
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
                ItemStack st = item == null ? ItemStack.EMPTY : new ItemStack(item);
                out.add("loot " + key + " has " + id + (com.rtshop.rules.ItemPolicy.lootBanned(st, false) ? " (removed by the loot rules)" : " KEPT")
                        + (com.rtshop.rules.ItemPolicy.lootBanned(st, true) ? "" : " (kept in boss tables)"));
            }
        }
        out.add("extreme: " + recipes + " recipes, " + tables + " loot table hits, " + failed + " tables not readable");
    }

    /**
     * 3.65.0 돈이 불어나는 길 찾기: (1) 같은 물건을 어느 상점에서 사서 다른 상점에 더 비싸게 팔 수 있는가,
     * (2) 가격표에 있는 재료만으로 만드는 물건(레시피 원가가 정확한 것)을 그 원가보다 비싸게 팔 수 있는가 (음식은 수고비 몫이 있으니 따로 표시).
     * 품질(별 · 무게) 배율 전 기본 매입가 기준.
     */
    private static void loops(List<String> out) {
        ShopLoader.Loaded l = ShopServer.loaded();
        if (l == null) return;
        java.util.Map<String, Integer> minBuy = new java.util.HashMap<>();
        java.util.Map<String, String> minBuyShop = new java.util.HashMap<>();
        java.util.Map<String, Double> maxSell = new java.util.HashMap<>();
        java.util.Map<String, String> maxSellShop = new java.util.HashMap<>();
        for (java.util.Map.Entry<String, com.rtshop.shop.ShopData> se : l.shops.entrySet()) {
            for (com.rtshop.shop.ShopData.Entry e : se.getValue().entries) {
                if (e.stack.isEmpty() || e.stack.hasTag()) continue;
                ResourceLocation rl = ForgeRegistries.ITEMS.getKey(e.stack.getItem());
                if (rl == null) continue;
                String id = rl.toString();
                if (e.buyable && (!minBuy.containsKey(id) || e.price < minBuy.get(id))) {
                    minBuy.put(id, e.price);
                    minBuyShop.put(id, se.getKey());
                }
                if (e.sellable && e.sellUnit > 0 && (!maxSell.containsKey(id) || e.sellUnit > maxSell.get(id))) {
                    maxSell.put(id, e.sellUnit);
                    maxSellShop.put(id, se.getKey());
                }
            }
        }
        out.add("== money loops: buy in one shop, sell in another for more");
        int n1 = 0;
        for (java.util.Map.Entry<String, Double> e : maxSell.entrySet()) {
            Integer b = minBuy.get(e.getKey());
            if (b != null && e.getValue() > b + 1.0e-6) {
                out.add(String.format(Locale.ROOT, "%s | buy %d (%s) | sell %.2f (%s)", e.getKey(), b, minBuyShop.get(e.getKey()), e.getValue(), maxSellShop.get(e.getKey())));
                n1++;
            }
        }
        out.add("same-item loops: " + n1);
        out.add("== money loops: make from shop materials, sell for more than they cost (item | sell | material cost | recipe | food?)");
        int n2 = 0;
        int food = 0;
        if (l.solved != null) {
            List<String> rows = new ArrayList<>();
            for (java.util.Map.Entry<String, Double> e : maxSell.entrySet()) {
                String id = e.getKey();
                Double c = l.solved.cost.get(id);
                if (c == null || c < 0.005 || !l.solved.exact.contains(id)) continue;
                double gain = e.getValue() - c;
                if (gain <= Math.max(0.5, c * 0.02)) continue;
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
                boolean isFood = item != null && (item.isEdible() || l.solved.processed.contains(id));
                if (isFood) food++;
                else n2++;
                rows.add(String.format(Locale.ROOT, "%08.1f|%s | sell %.2f | cost %.2f | %s | %s", gain, id, e.getValue(), c, l.solved.costRecipe.get(id), isFood ? "food" : "NOT FOOD"));
            }
            rows.sort(java.util.Comparator.reverseOrder());
            for (int i = 0; i < Math.min(80, rows.size()); i++) out.add(rows.get(i).substring(rows.get(i).indexOf('|') + 1));
        }
        out.add("craft loops: " + n2 + " non-food, " + food + " food (food has the cook's margin by design)");
        // 뽑기에 든 물건을 상점에서도 살 수 있으면 뽑기가 헛일이 된다
        out.add("== gacha items you can also buy in a shop (item | grade | shop price)");
        int n3 = 0;
        for (var en : com.rtshop.gacha.GachaConfig.get().pools.entrySet()) {
            for (var e : en.getValue()) {
                if (e.item == null || e.item.isEmpty()) continue;
                Integer b = minBuy.get(e.item);
                if (b != null) {
                    out.add(e.item + " | " + en.getKey() + ":" + e.grade + " | " + b + " (" + minBuyShop.get(e.item) + ")");
                    n3++;
                }
            }
        }
        out.add("gacha items in shops: " + n3);
    }

    private static void prices(List<String> out) {
        ShopLoader.Loaded l = ShopServer.loaded();
        RecipeSolver.Result r = l.solved;
        out.add("== prices");
        for (String id : PRICE_IDS) {
            ShopLoader.Price p = l.prices.get(id);
            StringBuilder sb = new StringBuilder(id);
            if (p != null) {
                sb.append(String.format(Locale.ROOT, " price %d sell %.3f fixedSell %s gear %s finished %s basis %s",
                        p.price, p.sellUnit, p.fixedSell, p.gear, p.finished, p.basis));
            } else {
                sb.append(" (no price)");
            }
            if (r != null) {
                sb.append(" | cost ").append(r.cost.get(id)).append(" value ").append(r.value.get(id))
                        .append(" exact ").append(r.exact.contains(id))
                        .append(" costRecipe ").append(r.costRecipe.get(id))
                        .append(" deriv ").append(r.costDeriv.get(id));
                String rid = r.costRecipe.get(id);
                RecipeSolver.Recipe rec = l.scan == null ? null : l.scan.byId(rid);
                if (rec != null) {
                    sb.append(" | type ").append(rec.type).append(" in ");
                    for (RecipeSolver.Ing ing : rec.ingredients) {
                        sb.append(ing.count).append('x').append(ing.alts.size() > 3 ? ing.alts.subList(0, 3) + "…" : ing.alts).append(' ');
                        if (!ing.alts.isEmpty()) {
                            String a = ing.alts.get(0);
                            sb.append("(=").append(r.cost.get(a)).append('/').append(r.known.get(a)).append(") ");
                        }
                    }
                    sb.append("out ");
                    for (RecipeSolver.Out o : rec.outputs) {
                        sb.append(o.count).append('x').append(o.item).append(' ');
                    }
                }
            }
            out.add(sb.toString());
        }
        // 매입가가 0 이 되는 품목 (장비 상점 매입 불가 원인 점검)
        int zero = 0;
        for (var e : l.prices.entrySet()) {
            ShopLoader.Price p = e.getValue();
            if (p.sellableSomewhere && p.sellUnit >= 0 && Math.round(p.sellUnit * 100) <= 0 && zero < 80) {
                zero++;
                out.add("zero-sell: " + e.getKey() + " price " + p.price + " basis " + p.basis);
            }
        }
        out.add("zero-sell total shown " + zero);
    }
}

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

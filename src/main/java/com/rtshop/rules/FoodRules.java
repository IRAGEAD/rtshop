package com.rtshop.rules;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 양산 음식 규칙 (config/rtshop/food_rules.json, 3.50): 빵·케이크·쿠키처럼 밭과 화로만으로 쉽게 쌓아 둘 수 있는 음식은 배고픔이 적게 찬다.
 * 요리사가 손으로 조리하는 요리(상점이 '손으로 만든 요리'로 치는 것)는 목록에 있어도 줄이지 않는다 — 요리사의 요리가 값어치를 갖게 하려는 규칙이다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class FoodRules {
    private FoodRules() {
    }

    public static final class Config {
        public boolean enabled = true;
        /** 채워지는 배고픔·포만감의 비율 (0.5 = 절반). */
        public double factor = 0.5;
        public final List<String> items = new ArrayList<>();

        public JsonObject toJson() {
            JsonObject o = new JsonObject();
            o.addProperty("_설명", "양산 음식: items 에 적은 음식은 배고픔과 포만감이 factor 배만 찬다 (0.5 = 절반, 적어도 1칸). "
                    + "요리사가 손으로 조리하는 요리는 목록에 있어도 줄이지 않는다. 케이크(minecraft:cake)는 조각마다 적용된다. 고친 뒤 /rtshop reload.");
            o.addProperty("enabled", enabled);
            o.addProperty("factor", factor);
            JsonArray a = new JsonArray();
            items.forEach(a::add);
            o.add("items", a);
            return o;
        }
    }

    public static Config fresh() {
        Config c = new Config();
        for (String s : new String[]{"bread", "cake", "cookie", "pumpkin_pie", "baked_potato", "dried_kelp", "melon_slice", "sweet_berries", "glow_berries",
                "apple", "carrot", "potato", "beetroot"}) {
            c.items.add("minecraft:" + s);
        }
        return c;
    }

    private static Config config;

    public static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("food_rules.json");
    }

    public static synchronized Config get() {
        if (config == null) config = load();
        return config;
    }

    public static synchronized void reload() {
        config = load();
    }

    public static synchronized void setForTest(Config c) {
        config = c;
    }

    private static Config load() {
        Config c = fresh();
        Path p = path();
        try {
            if (Files.exists(p)) {
                JsonObject o = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
                if (o.has("enabled")) c.enabled = o.get("enabled").getAsBoolean();
                if (o.has("factor")) c.factor = Math.max(0, Math.min(1, o.get("factor").getAsDouble()));
                if (o.has("items") && o.get("items").isJsonArray()) {
                    c.items.clear();
                    o.getAsJsonArray("items").forEach(e -> c.items.add(e.getAsString()));
                }
            } else {
                Files.createDirectories(p.getParent());
                Files.writeString(p, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(c.toJson()), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            RtShop.LOGGER.error("[rtshop] food_rules.json 을 읽지 못했습니다: {}", e.getMessage());
        }
        return c;
    }

    /** 이 음식이 양산 음식으로 줄어드는가. */
    public static boolean weak(ItemStack s) {
        Config c = get();
        if (!c.enabled || s.isEmpty() || c.factor >= 1) return false;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(s.getItem());
        if (id == null || !c.items.contains(id.toString())) return false;
        try {
            if (com.rtshop.shop.ShopLoader.handCooked(com.rtshop.shop.ShopServer.loaded(), s)) return false; // 요리사가 손으로 만드는 요리는 그대로
        } catch (Throwable ignored) {
        }
        return true;
    }

    /** 원래 nutrition 칸이 찰 음식에서 덜어 낼 칸 수 (적어도 1칸은 차게 둔다). */
    public static int cut(int nutrition) {
        int keep = (int) Math.max(1, Math.round(nutrition * get().factor));
        return Math.max(0, nutrition - keep);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEat(LivingEntityUseItemEvent.Finish event) {
        try {
            if (!(event.getEntity() instanceof ServerPlayer sp)) return;
            ItemStack used = event.getItem();
            FoodProperties food = used.getFoodProperties(sp);
            if (food == null || !weak(used)) return;
            int less = cut(food.getNutrition());
            if (less <= 0) return;
            // 방금 먹어서 찬 것에서 덜어 낸다 (포만감도 같은 비율로)
            var fd = sp.getFoodData();
            fd.setFoodLevel(Math.max(0, fd.getFoodLevel() - less));
            float satGain = food.getNutrition() * food.getSaturationModifier() * 2F;
            fd.setSaturation(Math.max(0F, Math.min(fd.getFoodLevel(), fd.getSaturationLevel() - satGain * (float) (1 - get().factor))));
        } catch (Throwable ignored) {
        }
    }

    /** 케이크: 한 조각이 2칸 대신 줄어든 만큼만 찬다. 서버가 직접 한 조각을 먹인다. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onCake(PlayerInteractEvent.RightClickBlock event) {
        try {
            if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer sp)) return;
            BlockState st = event.getLevel().getBlockState(event.getPos());
            if (!(st.getBlock() instanceof CakeBlock) || !st.hasProperty(CakeBlock.BITES)) return;
            if (!weak(new ItemStack(st.getBlock().asItem())) || cut(2) <= 0) return;
            if (sp.getItemInHand(event.getHand()).is(net.minecraft.tags.ItemTags.CANDLES)) return; // 초를 꽂는 것은 그대로
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (!sp.canEat(false)) return;
            sp.awardStat(Stats.EAT_CAKE_SLICE);
            sp.getFoodData().eat(2 - cut(2), 0.1F);
            int bites = st.getValue(CakeBlock.BITES);
            event.getLevel().gameEvent(sp, GameEvent.EAT, event.getPos());
            if (bites < 6) {
                event.getLevel().setBlock(event.getPos(), st.setValue(CakeBlock.BITES, bites + 1), 3);
            } else {
                event.getLevel().removeBlock(event.getPos(), false);
                event.getLevel().gameEvent(sp, GameEvent.BLOCK_DESTROY, event.getPos());
            }
        } catch (Throwable ignored) {
        }
    }
}

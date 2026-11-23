package com.rtshop.shop;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import com.rtshop.job.JobConfig;
import com.rtshop.job.Jobs;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 식당 주인 (상점 ID: restaurant).
 *  - 요리사 직업을 가진 플레이어만 요리를 팔 수 있고, 식품 상인보다 bonus(기본 5%) 더 받는다 (요리사 패시브와 따로 곱해진다).
 *  - 처음에는 아무것도 팔지 않는다. 요리사가 한 번이라도 판 요리가 도감처럼 하나씩 메뉴에 올라가고, 메뉴는 누구나 살 수 있다.
 * 품목과 가격은 식품 상인(sourceShop)의 '요리'·'음료' 구분에서 가져온다.
 */
public final class Restaurant {
    private Restaurant() {
    }

    public static final String ID = "restaurant";

    // ------------------------------------------------------------------ 설정

    public static final class Config {
        public boolean enabled = true;
        /** 품목과 가격을 가져올 상점. */
        public String sourceShop = "food";
        /** 그 상점에서 '요리'로 치는 구분. */
        public List<String> categories = new ArrayList<>(List.of("dish", "drink"));
        /** 이 직업만 팔 수 있다 ("" = 누구나). */
        public String sellJob = "cook";
        /** 식당 판매 보너스 (0.05 = 식품 상인 매입가의 +5%). */
        public double bonus = 0.05;
        /** 식당이 파는 값 = 식품 상인 판매가 × 이 값. */
        public double priceMultiplier = 1.0;
        public String title = "식당";
        public String npcName = "식당 주인";
        public String profession = "butcher";
        /** 새 메뉴가 올라오면 서버 전체에 알린다. */
        public boolean announce = true;
        /** false 면 식품 상인은 요리를 사 주기만 하고 팔지는 않는다 (요리는 식당에서만 살 수 있게). */
        public boolean sourceSellsDishes = true;
    }

    private static Config config;

    public static synchronized Config config() {
        if (config == null) {
            config = loadConfig();
        }
        return config;
    }

    public static synchronized Config reloadConfig() {
        config = loadConfig();
        return config;
    }

    private static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("restaurant.json");
    }

    private static Config loadConfig() {
        Config c = new Config();
        Path p = path();
        try {
            if (Files.exists(p)) {
                JsonObject o = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
                if (o.has("enabled")) c.enabled = o.get("enabled").getAsBoolean();
                if (o.has("sourceShop")) c.sourceShop = o.get("sourceShop").getAsString();
                if (o.has("categories") && o.get("categories").isJsonArray()) {
                    c.categories = new ArrayList<>();
                    for (JsonElement e : o.getAsJsonArray("categories")) c.categories.add(e.getAsString());
                }
                if (o.has("sellJob")) c.sellJob = o.get("sellJob").getAsString();
                if (o.has("bonus")) c.bonus = Math.max(0, Math.min(1.0, o.get("bonus").getAsDouble()));
                if (o.has("priceMultiplier")) c.priceMultiplier = Math.max(0.1, Math.min(100, o.get("priceMultiplier").getAsDouble()));
                if (o.has("title")) c.title = o.get("title").getAsString();
                if (o.has("npcName")) c.npcName = o.get("npcName").getAsString();
                if (o.has("profession")) c.profession = o.get("profession").getAsString();
                if (o.has("announce")) c.announce = o.get("announce").getAsBoolean();
                if (o.has("sourceSellsDishes")) c.sourceSellsDishes = o.get("sourceSellsDishes").getAsBoolean();
                return c;
            }
        } catch (Exception ex) {
            RtShop.LOGGER.error("[rtshop] restaurant.json 을 읽지 못해 기본값을 사용합니다.", ex);
            return c;
        }
        saveConfig(c);
        return c;
    }

    public static void saveConfig(Config c) {
        try {
            JsonObject o = new JsonObject();
            JsonArray help = new JsonArray();
            help.add("식당 주인 (/rtshop npc restaurant): 요리사(sellJob) 직업을 가진 플레이어만 요리를 팔 수 있고, 식품 상인보다 bonus(0.05 = 5%) 더 받습니다. 요리사 패시브 보너스와는 따로 곱해집니다.");
            help.add("처음에는 아무것도 팔지 않습니다. 요리사가 한 번이라도 판 요리가 메뉴에 하나씩 올라가고, 메뉴에 오른 요리는 직업과 상관없이 누구나 살 수 있습니다.");
            help.add("품목과 가격은 sourceShop(식품 상인)의 categories(요리·음료) 구분에서 가져옵니다. priceMultiplier: 식당 판매가 = 식품 상인 판매가 × 이 값");
            help.add("sourceSellsDishes: false 로 바꾸면 식품 상인은 요리를 사 주기만 하고 팔지 않습니다 (요리는 식당에서만 살 수 있게 하려면).");
            help.add("메뉴 관리 (OP): /rtshop restaurant status · unlock <아이템> · unlock all · lock <아이템> · reset");
            o.add("_설명", help);
            o.addProperty("enabled", c.enabled);
            o.addProperty("sourceShop", c.sourceShop);
            JsonArray cats = new JsonArray();
            c.categories.forEach(cats::add);
            o.add("categories", cats);
            o.addProperty("sellJob", c.sellJob);
            o.addProperty("bonus", c.bonus);
            o.addProperty("priceMultiplier", c.priceMultiplier);
            o.addProperty("title", c.title);
            o.addProperty("npcName", c.npcName);
            o.addProperty("profession", c.profession);
            o.addProperty("announce", c.announce);
            o.addProperty("sourceSellsDishes", c.sourceSellsDishes);
            Path p = path();
            Files.createDirectories(p.getParent());
            Files.writeString(p, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(o), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            RtShop.LOGGER.error("[rtshop] restaurant.json 을 저장하지 못했습니다.", ex);
        }
    }

    // ------------------------------------------------------------------ 메뉴 (월드 저장)

    /** world/data/rtshop_restaurant.dat : 메뉴에 오른 요리 (ShopLoader.key) → {처음 판 요리사, 시각}. */
    public static final class Data extends SavedData {
        private static final String NAME = "rtshop_restaurant";
        public final Map<String, String[]> unlocked = new LinkedHashMap<>();

        public static Data get(MinecraftServer server) {
            return server.overworld().getDataStorage().computeIfAbsent(Data::load, Data::new, NAME);
        }

        public static Data load(CompoundTag tag) {
            Data d = new Data();
            ListTag list = tag.getList("menu", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag c = list.getCompound(i);
                d.unlocked.put(c.getString("key"), new String[]{c.getString("by"), Long.toString(c.getLong("at"))});
            }
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            ListTag list = new ListTag();
            for (Map.Entry<String, String[]> e : unlocked.entrySet()) {
                CompoundTag c = new CompoundTag();
                c.putString("key", e.getKey());
                c.putString("by", e.getValue()[0]);
                try {
                    c.putLong("at", Long.parseLong(e.getValue()[1]));
                } catch (NumberFormatException ignored) {
                }
                list.add(c);
            }
            tag.put("menu", list);
            return tag;
        }
    }

    // ------------------------------------------------------------------ 상점 만들기

    private static final String EMPTY_NOTE = "rtshop.restaurant.empty";

    /** 이 모드가 만든 식당 상점인가 (같은 이름의 상점 파일을 직접 만들어 둔 경우는 일반 상점으로 둔다). */
    public static boolean is(ShopData d) {
        return d != null && ID.equals(d.id) && EMPTY_NOTE.equals(d.emptyNote);
    }

    /** 두 직업 칸(전투·생활)에서 얻을 수 있는 이 패시브의 최댓값 합. */
    private static double maxStat(String key) {
        Map<String, Double> best = new LinkedHashMap<>();
        for (JobConfig.JobDef j : JobConfig.get().jobs.values()) {
            best.merge(String.valueOf(j.slot), Math.max(0, j.param(key, 0)), Math::max);
        }
        double v = 0;
        for (double b : best.values()) v += b;
        return v;
    }

    /**
     * 식품 상인의 요리·음료로 식당 상점을 만든다 (가격표를 읽은 뒤, 그리고 메뉴가 늘 때마다 부른다).
     * 품목 순서는 늘 같아서(식품 상인 순서) 메뉴가 늘어도 열려 있는 창의 번호가 어긋나지 않는다.
     */
    public static ShopData build(ShopLoader.Loaded out, MinecraftServer server, int version) {
        Config c = config();
        ShopData src = out.shops.get(c.sourceShop);
        if (!c.enabled || src == null) {
            return null;
        }
        Map<String, String[]> unlocked = Map.of();
        try {
            if (server != null) unlocked = Data.get(server).unlocked;
        } catch (Throwable t) {
            // 세계를 아직 읽기 전 (서버가 다 켜진 뒤 한 번 더 만든다)
        }
        // 식당에서 사서 식당에 되팔아 남지 않도록: 판매가는 (보너스 포함 매입가 × 요리사 최대 보너스) 보다 항상 비싸다
        double worst = (1 + c.bonus) * (1 + maxStat("dishSell"));
        List<ShopData.Entry> entries = new ArrayList<>();
        List<ShopData.Category> cats = new ArrayList<>();
        for (ShopData.Category cat : src.categories) {
            if (c.categories.contains(cat.id)) cats.add(cat);
        }
        for (ShopData.Entry e : src.entries) {
            if (!e.sellable || !c.categories.contains(e.category)) {
                continue;
            }
            double sell = e.sellUnit * (1 + c.bonus);
            long price = Math.max(Math.round(e.price * c.priceMultiplier), (long) Math.ceil(e.sellUnit * worst + 1.0e-6) + 1);
            price = Math.max(1, Math.min(2_000_000_000L, price));
            entries.add(new ShopData.Entry(e.stack, (int) price, sell, e.category, e.looseSell,
                    unlocked.containsKey(ShopLoader.key(e.stack)), true, false, e.recipe, e.recipeOut));
        }
        ShopData d = new ShopData(ID, version, c.title, c.npcName, c.profession, cats, entries,
                src.starMult, src.fishBase, src.fishBonus, src.fishGolden, src.fishNoInfo);
        d.emptyNote = EMPTY_NOTE;
        d.sellNote = "rtshop.restaurant.sell_note|" + Math.round(c.bonus * 100);
        return d;
    }

    /** 가격표를 읽은 직후: 식당 상점을 넣고, 설정에 따라 식품 상인의 요리 판매를 끈다. */
    public static void install(ShopLoader.Loaded out, MinecraftServer server, int version) {
        Config c = reloadConfig();
        if (out.shops.containsKey(ID)) {
            RtShop.LOGGER.info("[rtshop] shops/{}.json 이 있어 그 파일을 일반 상점으로 씁니다 (식당 주인 기능은 꺼짐)", ID);
            return;
        }
        ShopData src = out.shops.get(c.sourceShop);
        if (c.enabled && src != null && !c.sourceSellsDishes) {
            List<ShopData.Entry> entries = new ArrayList<>();
            for (ShopData.Entry e : src.entries) {
                entries.add(c.categories.contains(e.category) && e.buyable
                        ? new ShopData.Entry(e.stack, e.price, e.sellUnit, e.category, e.looseSell, false, e.sellable, e.fish, e.recipe, e.recipeOut) : e);
            }
            out.shops.put(src.id, new ShopData(src.id, src.version, src.title, src.npcName, src.profession, src.categories, entries,
                    src.starMult, src.fishBase, src.fishBonus, src.fishGolden, src.fishNoInfo));
        }
        ShopData d = build(out, server, version);
        if (d != null) {
            out.shops.put(ID, d);
            RtShop.LOGGER.info("[rtshop] 식당 '{}' 준비: 요리 {}종 (메뉴에 오른 것 {})", ID, d.entries.size(), menuCount(d));
        }
    }

    public static int menuCount(ShopData d) {
        int n = 0;
        for (ShopData.Entry e : d.entries) if (e.buyable) n++;
        return n;
    }

    public static boolean canSell(ServerPlayer p) {
        String job = config().sellJob;
        return job == null || job.isBlank() || Jobs.has(p, job);
    }

    /** 이 플레이어에게 보낼 식당 데이터: 요리사가 아니면 판매 탭을 잠근다. */
    public static ShopData viewFor(ServerPlayer p, ShopData d) {
        ShopData v = new ShopData(d.id, d.version, d.title, d.npcName, d.profession, d.categories, d.entries,
                d.starMult, d.fishBase, d.fishBonus, d.fishGolden, d.fishNoInfo);
        v.emptyNote = d.emptyNote;
        v.sellNote = d.sellNote;
        v.sellLockNote = canSell(p) ? "" : "rtshop.restaurant.cook_only";
        return v;
    }

    /**
     * 요리사가 요리를 팔았다: 처음 팔린 요리면 메뉴에 올린다.
     * @return 새로 메뉴에 올랐으면 true
     */
    public static boolean onSold(ServerPlayer p, ShopData d, int index) {
        ShopData.Entry e = d.entries.get(index);
        if (e.buyable) {
            return false;
        }
        return unlock(p.server, e.stack, p.getGameProfile().getName(), p);
    }

    public static boolean unlock(MinecraftServer server, ItemStack stack, String by, ServerPlayer seller) {
        Data data = Data.get(server);
        String key = ShopLoader.key(stack);
        if (data.unlocked.containsKey(key)) {
            return false;
        }
        data.unlocked.put(key, new String[]{by == null ? "" : by, Long.toString(System.currentTimeMillis())});
        data.setDirty();
        ShopData now = ShopServer.rebuildRestaurant(server);
        if (now != null && config().announce) {
            Component msg = Component.translatable("rtshop.restaurant.new_menu", by == null ? "" : by,
                    stack.getHoverName().copy().withStyle(ChatFormatting.YELLOW), menuCount(now), now.entries.size()).withStyle(ChatFormatting.GOLD);
            for (ServerPlayer o : server.getPlayerList().getPlayers()) {
                com.rtshop.notice.Notice.send(o, msg);
            }
        }
        return true;
    }

    public static boolean lock(MinecraftServer server, ItemStack stack) {
        Data data = Data.get(server);
        if (data.unlocked.remove(ShopLoader.key(stack)) == null) {
            return false;
        }
        data.setDirty();
        ShopServer.rebuildRestaurant(server);
        return true;
    }

    public static int reset(MinecraftServer server) {
        Data data = Data.get(server);
        int n = data.unlocked.size();
        data.unlocked.clear();
        data.setDirty();
        ShopServer.rebuildRestaurant(server);
        return n;
    }

    // ------------------------------------------------------------------ 명령어 (OP)

    private static final com.mojang.brigadier.suggestion.SuggestionProvider<net.minecraft.commands.CommandSourceStack> DISHES = (ctx, b) -> {
        List<String> ids = new ArrayList<>();
        ShopData d = ShopServer.shops().get(ID);
        if (d != null) {
            for (ShopData.Entry e : d.entries) ids.add(ShopLoader.itemId(e.stack));
        }
        return net.minecraft.commands.SharedSuggestionProvider.suggest(ids, b);
    };

    private static ShopData.Entry dish(String id) {
        ShopData d = ShopServer.shops().get(ID);
        if (d == null) {
            return null;
        }
        for (ShopData.Entry e : d.entries) {
            if (ShopLoader.itemId(e.stack).equals(id)) return e;
        }
        return null;
    }

    /** /rtshop restaurant status | unlock <아이템|all> | lock <아이템> | reset */
    public static com.mojang.brigadier.builder.LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack> adminCommand() {
        return net.minecraft.commands.Commands.literal("restaurant").requires(src -> src.hasPermission(2))
                .executes(Restaurant::status)
                .then(net.minecraft.commands.Commands.literal("status").executes(Restaurant::status))
                .then(net.minecraft.commands.Commands.literal("reset").executes(ctx -> {
                    int n = reset(ctx.getSource().getServer());
                    ctx.getSource().sendSuccess(() -> Component.translatable("rtshop.restaurant.cmd_reset", n), true);
                    return 1;
                }))
                .then(net.minecraft.commands.Commands.literal("unlock")
                        .then(net.minecraft.commands.Commands.literal("all").executes(ctx -> {
                            int n = unlockAll(ctx.getSource().getServer());
                            ctx.getSource().sendSuccess(() -> Component.translatable("rtshop.restaurant.cmd_unlock_all", n), true);
                            return 1;
                        }))
                        .then(net.minecraft.commands.Commands.argument("item", net.minecraft.commands.arguments.ResourceLocationArgument.id()).suggests(DISHES).executes(ctx -> {
                            String id = net.minecraft.commands.arguments.ResourceLocationArgument.getId(ctx, "item").toString();
                            ShopData.Entry e = dish(id);
                            if (e == null) {
                                ctx.getSource().sendFailure(Component.translatable("rtshop.restaurant.cmd_unknown", id));
                                return 0;
                            }
                            boolean ok = unlock(ctx.getSource().getServer(), e.stack, ctx.getSource().getTextName(), null);
                            ctx.getSource().sendSuccess(() -> Component.translatable(ok ? "rtshop.restaurant.cmd_unlocked" : "rtshop.restaurant.cmd_already",
                                    e.stack.getHoverName()), true);
                            return ok ? 1 : 0;
                        })))
                .then(net.minecraft.commands.Commands.literal("lock")
                        .then(net.minecraft.commands.Commands.argument("item", net.minecraft.commands.arguments.ResourceLocationArgument.id()).suggests(DISHES).executes(ctx -> {
                            String id = net.minecraft.commands.arguments.ResourceLocationArgument.getId(ctx, "item").toString();
                            ShopData.Entry e = dish(id);
                            if (e == null) {
                                ctx.getSource().sendFailure(Component.translatable("rtshop.restaurant.cmd_unknown", id));
                                return 0;
                            }
                            boolean ok = lock(ctx.getSource().getServer(), e.stack);
                            ctx.getSource().sendSuccess(() -> Component.translatable(ok ? "rtshop.restaurant.cmd_locked" : "rtshop.restaurant.cmd_not_on_menu",
                                    e.stack.getHoverName()), true);
                            return ok ? 1 : 0;
                        })));
    }

    private static int status(com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> ctx) {
        ShopData d = ShopServer.shops().get(ID);
        if (!is(d)) {
            ctx.getSource().sendFailure(Component.translatable("rtshop.restaurant.cmd_off"));
            return 0;
        }
        Config c = config();
        ctx.getSource().sendSuccess(() -> Component.translatable("rtshop.restaurant.cmd_status", menuCount(d), d.entries.size(),
                Math.round(c.bonus * 100), c.sellJob.isBlank() ? "-" : c.sellJob), false);
        return 1;
    }

    /** 모든 요리를 메뉴에 올린다 (OP). 새로 올린 수. */
    public static int unlockAll(MinecraftServer server) {
        ShopData d = ShopServer.shops().get(ID);
        if (d == null) {
            return 0;
        }
        Data data = Data.get(server);
        int n = 0;
        for (ShopData.Entry e : d.entries) {
            if (data.unlocked.putIfAbsent(ShopLoader.key(e.stack), new String[]{"", Long.toString(System.currentTimeMillis())}) == null) n++;
        }
        data.setDirty();
        ShopServer.rebuildRestaurant(server);
        return n;
    }
}

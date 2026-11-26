package com.rtshop.raid;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.rtshop.RtShop;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootDataId;
import net.minecraft.world.level.storage.loot.LootDataManager;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SetNbtFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Spiral Abyss 층 보상(층 선택 화면의 "Possible Rewards" 상자들)을 게임 안에서 고친다. config/rtshop/abyss_floors.json
 * 층마다 상자 다섯 칸: 상자마다 "비중"(어느 상자가 뽑힐지)과 내용물(아이템·개수·확률 %).
 * 모드는 상자를 전리품표로 다루므로, 고친 상자를 전리품표(rtshop_abyss:floor_N/chest_K)로 만들어 게임에 끼우고
 * 모드의 층 목록에서 그 층의 보상만 바꿔 둔다. 모드 화면의 확률·내용물 미리보기와 실제 지급이 모두 그대로 따라온다.
 * "사용"을 끄면 모드 원래 보상으로 돌아간다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class AbyssFloors {
    private AbyssFloors() {
    }

    public static final int FLOORS = 6, CHESTS = 5;
    public static final String NS = "rtshop_abyss";

    public static final class Floor {
        public boolean use;
        public int experience = -1; // -1 = 모드 원래 값
        public final int[] weight = {1, 1, 1, 1, 1};
        public final List<Reward> rewards = new ArrayList<>(); // group = "c1".."c5", chance = %

        List<Reward> chest(int k) {
            List<Reward> out = new ArrayList<>();
            for (Reward r : rewards) if (r.group.equals("c" + k) && !r.stack().isEmpty() && r.chance > 0) out.add(r);
            return out;
        }

        boolean custom() {
            if (!use) return false;
            for (int k = 1; k <= CHESTS; k++) if (weight[k - 1] > 0 && !chest(k).isEmpty()) return true;
            return false;
        }
    }

    private static Floor[] floors;
    /** 고칠 때마다 올라간다 (끼워 둔 것이 낡았는지 본다). */
    private static int version = 1, applied = 0;
    /** 모드가 읽은 원래 보상 (층 -> Rewards 객체). */
    private static final Map<Integer, Object> ORIGINAL = new HashMap<>();
    public static String lastError = "";

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("abyss_floors.json");
    }

    public static synchronized Floor[] get() {
        if (floors == null) floors = load();
        return floors;
    }

    private static Floor[] load() {
        Floor[] f = new Floor[FLOORS];
        for (int i = 0; i < FLOORS; i++) f[i] = new Floor();
        try {
            if (Files.exists(file())) {
                JsonObject o = JsonParser.parseString(Files.readString(file(), StandardCharsets.UTF_8)).getAsJsonObject();
                JsonObject fl = o.has("floors") ? o.getAsJsonObject("floors") : new JsonObject();
                for (int i = 0; i < FLOORS; i++) {
                    if (!fl.has(Integer.toString(i + 1))) continue;
                    JsonObject x = fl.getAsJsonObject(Integer.toString(i + 1));
                    f[i].use = x.has("use") && x.get("use").getAsBoolean();
                    if (x.has("experience")) f[i].experience = x.get("experience").getAsInt();
                    if (x.has("weights")) {
                        JsonArray w = x.getAsJsonArray("weights");
                        for (int k = 0; k < CHESTS && k < w.size(); k++) f[i].weight[k] = Math.max(0, Math.min(10000, w.get(k).getAsInt()));
                    }
                    if (x.has("rewards")) for (JsonElement e : x.getAsJsonArray("rewards")) f[i].rewards.add(Reward.parse(e.getAsJsonObject()));
                }
            } else {
                save(f);
            }
        } catch (Exception e) {
            RtShop.LOGGER.warn("[rtshop] abyss_floors.json 을 읽지 못했습니다: {}", e.toString());
        }
        return f;
    }

    public static synchronized void save(Floor[] f) {
        try {
            JsonObject o = new JsonObject();
            JsonArray help = new JsonArray();
            help.add("Spiral Abyss 층 보상. 게임 안에서 /rtshop abyss floor <1~6> (OP) 으로 고칩니다.");
            help.add("use: true 면 아래 상자들을 쓰고, false 면 모드 원래 보상. weights: 상자 1~5 가 뽑히는 비중(0 = 안 나옴). experience: 경험치(-1 = 모드 원래 값).");
            help.add("rewards: group 이 c1~c5 인 줄이 그 상자의 내용물. chance 는 그 상자가 뽑혔을 때 그 아이템이 나올 확률(%).");
            o.add("_help", help);
            JsonObject fl = new JsonObject();
            for (int i = 0; i < FLOORS; i++) {
                JsonObject x = new JsonObject();
                x.addProperty("use", f[i].use);
                x.addProperty("experience", f[i].experience);
                JsonArray w = new JsonArray();
                for (int k : f[i].weight) w.add(k);
                x.add("weights", w);
                x.add("rewards", RewardEditor.rewardsJson(f[i].rewards));
                fl.add(Integer.toString(i + 1), x);
            }
            o.add("floors", fl);
            Files.createDirectories(file().getParent());
            Files.writeString(file(), new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(o), StandardCharsets.UTF_8);
        } catch (Exception e) {
            RtShop.LOGGER.warn("[rtshop] abyss_floors.json 을 쓰지 못했습니다: {}", e.toString());
        }
        floors = f;
        version++;
    }

    public static synchronized void reload() {
        floors = null;
        version++;
    }

    /** 시험용 */
    public static synchronized void setForTest(Floor[] f) {
        floors = f;
        version++;
    }

    public static ResourceLocation tableId(int floor, int chest) {
        return new ResourceLocation(NS, "floor_" + floor + "/chest_" + chest);
    }

    // ------------------------------------------------------------------ 전리품표 만들기·끼우기

    public static LootTable build(List<Reward> items) {
        LootTable.Builder t = LootTable.lootTable().setParamSet(LootContextParamSets.CHEST);
        for (Reward r : items) {
            ItemStack s = r.stack();
            if (s.isEmpty()) continue;
            LootPoolSingletonContainer.Builder<?> e = LootItem.lootTableItem(s.getItem());
            if (s.getCount() > 1) e.apply(SetItemCountFunction.setCount(ConstantValue.exactly(s.getCount())));
            CompoundTag tag = s.getTag();
            if (tag != null && !tag.isEmpty()) e.apply(SetNbtFunction.setTag(tag.copy()));
            LootPool.Builder pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(e);
            if (r.chance < 100) pool.when(LootItemRandomChanceCondition.randomChance((float) (r.chance / 100.0)));
            t.withPool(pool);
        }
        return t.build();
    }

    /** LootDataManager 의 표 목록(하나뿐인 Map 필드)에 우리 표를 끼운다. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void putTables(MinecraftServer server, Map<ResourceLocation, LootTable> tables) throws Exception {
        LootDataManager m = server.getLootData();
        Field target = null;
        for (Field f : LootDataManager.class.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers()) && Map.class.isAssignableFrom(f.getType())) target = f;
        }
        if (target == null) throw new IllegalStateException("no table map");
        target.setAccessible(true);
        Map old = (Map) target.get(m);
        Map next = new HashMap();
        for (Object e : old.entrySet()) {
            Map.Entry en = (Map.Entry) e;
            LootDataId id = (LootDataId) en.getKey();
            if (id.location().getNamespace().equals(NS)) continue;
            next.put(id, en.getValue());
        }
        for (Map.Entry<ResourceLocation, LootTable> e : tables.entrySet()) {
            e.getValue().setLootTableId(e.getKey());
            next.put(new LootDataId<>(LootDataType.TABLE, e.getKey()), e.getValue());
        }
        target.set(m, java.util.Collections.unmodifiableMap(next));
    }

    private static Class<?> cls(String n) throws ClassNotFoundException {
        return Class.forName("com.pla.spiral_abyss.encounter." + n);
    }

    private static Object call(Object o, String name) throws Exception {
        Method m = o.getClass().getMethod(name);
        m.setAccessible(true);
        return m.invoke(o);
    }

    /** 모드의 층 목록에서 고친 층의 보상을 바꿔 끼운다 (모드가 없으면 아무 일도 없다). true = 모드에 반영됨. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static synchronized boolean apply(MinecraftServer server) {
        if (!AbyssWeekly.detected()) return false;
        try {
            Floor[] fs = get();
            Class<?> catalog = cls("data.FloorCatalog"), defC = cls("data.FloorDefinition"), rewC = cls("data.FloorDefinition$Rewards"),
                    entC = cls("data.FloorDefinition$RewardEntry"), snapC = cls("data.FloorCatalog$Snapshot");
            Object snap = catalog.getMethod("snapshot").invoke(null);
            Map<Integer, Object> cur = (Map<Integer, Object>) call(snap, "floors");
            Map<ResourceLocation, LootTable> tables = new LinkedHashMap<>();
            Map<Integer, Object> next = new HashMap<>(cur);
            Constructor<?> entNew = entC.getConstructor(ResourceLocation.class, boolean.class, int.class, int.class, int.class, double.class);
            Constructor<?> rewNew = rewC.getConstructor(List.class, int.class, int.class);
            Constructor<?> defNew = defC.getDeclaredConstructors()[0];
            for (Constructor<?> c : defC.getDeclaredConstructors()) if (c.getParameterCount() == 6) defNew = c;
            boolean changed = false;
            for (int n = 1; n <= FLOORS; n++) {
                Object def = cur.get(n);
                if (def == null) continue;
                Object rewards = call(def, "rewards");
                boolean ours = false;
                for (Object e : (List<Object>) call(rewards, "entries")) {
                    if (((ResourceLocation) call(e, "id")).getNamespace().equals(NS)) ours = true;
                }
                if (!ours) ORIGINAL.put(n, rewards);
                Object base = ORIGINAL.getOrDefault(n, rewards);
                Floor f = fs[n - 1];
                Object want = base;
                if (f.custom()) {
                    List<Object> entries = new ArrayList<>();
                    for (int k = 1; k <= CHESTS; k++) {
                        List<Reward> items = f.chest(k);
                        if (items.isEmpty() || f.weight[k - 1] <= 0) continue;
                        ResourceLocation id = tableId(n, k);
                        tables.put(id, build(items));
                        entries.add(entNew.newInstance(id, false, 1, 1, f.weight[k - 1], 1.0));
                    }
                    int exp = f.experience >= 0 ? f.experience : (Integer) call(base, "experience");
                    want = rewNew.newInstance(entries, (Integer) call(base, "rolls"), exp);
                }
                if (f.custom() || ours) {
                    next.put(n, defNew.newInstance(call(def, "number"), call(def, "timeTicks"), call(def, "waveDelayTicks"), want, call(def, "waves"), call(def, "challenge")));
                    changed = true;
                }
            }
            putTables(server, tables);
            if (changed) {
                Field current = catalog.getDeclaredField("current");
                current.setAccessible(true);
                // 판 번호는 그대로 둔다: 올리면 모드가 진행 중인 도전을 "다시 읽음"으로 끊는다. 새 보상은 다음 입장부터.
                current.set(null, snapC.getConstructor(int.class, Map.class).newInstance(call(snap, "revision"), next));
            }
            applied = version;
            lastError = "";
            return true;
        } catch (Throwable t) {
            lastError = t.toString();
            RtShop.LOGGER.warn("[rtshop] Spiral Abyss 층 보상을 끼우지 못했습니다: {}", t.toString());
            applied = version;
            return false;
        }
    }

    /** 끼워 둔 것이 사라졌는지(데이터팩 다시 읽기) 본다. */
    @SuppressWarnings("unchecked")
    private static boolean stale(MinecraftServer server) {
        if (applied != version) return true;
        try {
            Floor[] fs = get();
            Object snap = cls("data.FloorCatalog").getMethod("snapshot").invoke(null);
            Map<Integer, Object> cur = (Map<Integer, Object>) call(snap, "floors");
            for (int n = 1; n <= FLOORS; n++) {
                if (!fs[n - 1].custom() || !cur.containsKey(n)) continue;
                boolean ours = false;
                for (Object e : (List<Object>) call(call(cur.get(n), "rewards"), "entries")) {
                    ResourceLocation id = (ResourceLocation) call(e, "id");
                    if (id.getNamespace().equals(NS)) {
                        ours = true;
                        if (server.getLootData().getElement(new LootDataId<>(LootDataType.TABLE, id)) == null) return true;
                    }
                }
                if (!ours) return true;
            }
        } catch (Throwable t) {
            return false;
        }
        return false;
    }

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent event) {
        apply(event.getServer());
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 40 != 23) return;
        if (!AbyssWeekly.detected()) return;
        if (stale(event.getServer())) apply(event.getServer());
    }

    // ------------------------------------------------------------------ 편집기 / 명령어

    static {
        for (int n = 1; n <= FLOORS; n++) {
            final int floor = n;
            RewardEditor.TARGETS.put("abyssfloor" + n, new RewardEditor.Target() {
                @Override
                public JsonObject model(ServerPlayer p) {
                    Floor f = get()[floor - 1];
                    JsonObject o = new JsonObject();
                    o.addProperty("id", "abyssfloor" + floor);
                    boolean en = com.rtshop.story.StoryText.en(p);
                    o.addProperty("title", en ? "Spiral Abyss Floor " + floor + " Chest Rewards" : "Spiral Abyss " + floor + "층 상자 보상");
                    JsonArray fl = new JsonArray();
                    fl.add(RewardEditor.field("use", en ? "Use custom rewards (1 = yes, 0 = the mod's own rewards)" : "직접 정한 보상 사용 (1 = 사용, 0 = 모드 원래 보상)", true, f.use ? "1" : "0"));
                    fl.add(RewardEditor.field("experience", en ? "Experience (-1 = the mod's own value)" : "경험치 (-1 = 모드 원래 값)", true, Integer.toString(f.experience)));
                    for (int k = 1; k <= CHESTS; k++) {
                        fl.add(RewardEditor.field("weight" + k, en ? "Weight of chest " + k + " (0 = never)" : "상자 " + k + " 이 뽑히는 비중 (0 = 안 나옴)", true, Integer.toString(f.weight[k - 1])));
                    }
                    o.add("fields", fl);
                    JsonArray g = new JsonArray();
                    for (int k = 1; k <= CHESTS; k++) g.add(RewardEditor.group("c" + k, (en ? "Chest " : "상자 ") + k, false, true));
                    o.add("groups", g);
                    o.add("rewards", RewardEditor.rewardsJson(f.rewards));
                    return o;
                }

                @Override
                public void apply(ServerPlayer p, JsonObject edited) {
                    Floor[] fs = get();
                    Floor f = fs[floor - 1];
                    Map<String, String> v = RewardEditor.fields(edited);
                    f.use = RewardEditor.dbl(v, "use", f.use ? 1 : 0) >= 0.5;
                    f.experience = (int) Math.max(-1, Math.min(100000, RewardEditor.dbl(v, "experience", f.experience)));
                    for (int k = 1; k <= CHESTS; k++) f.weight[k - 1] = (int) Math.max(0, Math.min(10000, RewardEditor.dbl(v, "weight" + k, f.weight[k - 1])));
                    if (edited.has("rewards")) {
                        f.rewards.clear();
                        f.rewards.addAll(RewardEditor.rewards(edited));
                    }
                    save(fs);
                    AbyssFloors.apply(p.server);
                    if (f.use && !f.custom()) p.sendSystemMessage(Component.literal(com.rtshop.story.StoryText.pick(p, "상자가 모두 비어 있어 이 층은 모드 원래 보상을 씁니다. [보상] 갈래에서 아이템을 손에 들고 추가하세요.", "Every chest is empty, so this floor uses the mod's own rewards. Hold an item and add it under the [Rewards] tab.")));
                }

                @Override
                public void add(ServerPlayer p, Reward r) {
                    Floor[] fs = get();
                    if (!r.group.matches("c[1-5]")) r.group = "c1";
                    r.chance = 100;
                    fs[floor - 1].rewards.add(r);
                    save(fs);
                    AbyssFloors.apply(p.server);
                }
            });
        }
    }

    /** 클래스를 일찍 읽어 편집 대상이 등록되게 한다. */
    public static void init() {
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        init();
        event.getDispatcher().register(Commands.literal("rtshop").then(Commands.literal("abyss").requires(s -> s.hasPermission(2))
                .then(Commands.literal("floor")
                        .executes(ctx -> {
                            Floor[] fs = get();
                            StringBuilder b = new StringBuilder("Spiral Abyss 층 보상: ");
                            for (int n = 1; n <= FLOORS; n++) b.append(n).append("층 ").append(fs[n - 1].custom() ? "직접 정함" : "모드 원래").append(n < FLOORS ? " · " : "");
                            b.append("  (/rtshop abyss floor <1~6>)");
                            if (!lastError.isEmpty()) b.append("  오류: ").append(lastError);
                            ctx.getSource().sendSuccess(() -> Component.literal(b.toString()), false);
                            return 1;
                        })
                        .then(Commands.argument("n", IntegerArgumentType.integer(1, FLOORS)).executes(ctx -> {
                            Raid.openEditor(ctx.getSource().getPlayerOrException(), "abyssfloor" + IntegerArgumentType.getInteger(ctx, "n"));
                            return 1;
                        })))));
    }
}

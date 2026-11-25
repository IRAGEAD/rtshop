package com.rtshop.rules;

import com.google.common.base.Suppliers;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.rtshop.RtShop;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;

/**
 * 상자 등 전리품 규칙 (config/rtshop/loot.json).
 *  tables: { "전리품표 ID 또는 끝이 * 인 묶음": { clear, remove:[아이템], add:[{item, chance(0~1), min, max}] } }
 *  - clear: 원래 나오던 것을 모두 지움 · remove: 이 아이템들은 나오지 않음 · add: 확률대로 추가.
 *  - 원래 나오던 아이템의 확률을 바꾸려면 remove 와 add 에 함께 넣는다 (편집기가 그렇게 저장함).
 */
public class LootRules extends LootModifier {
    public static final Supplier<Codec<LootRules>> CODEC = Suppliers.memoize(
            () -> RecordCodecBuilder.create(inst -> codecStart(inst).apply(inst, LootRules::new)));

    static {
        com.rtshop.farm.FarmLootModifier.SERIALIZERS.register("loot_rules", CODEC);
    }

    /** 클래스를 일찍 읽어서 위 등록이 돌게 한다. */
    public static void init() {
    }

    public LootRules(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }

    public static final class Add {
        public String item;
        public double chance;
        public int min, max;
    }

    public static final class Table {
        public boolean clear;
        public final Set<String> remove = new LinkedHashSet<>();
        public final List<Add> add = new ArrayList<>();

        public boolean empty() {
            return !clear && remove.isEmpty() && add.isEmpty();
        }

        public JsonObject toJson() {
            JsonObject o = new JsonObject();
            if (clear) o.addProperty("clear", true);
            JsonArray r = new JsonArray();
            remove.forEach(r::add);
            o.add("remove", r);
            JsonArray a = new JsonArray();
            for (Add x : add) {
                JsonObject e = new JsonObject();
                e.addProperty("item", x.item);
                e.addProperty("chance", x.chance);
                e.addProperty("min", x.min);
                e.addProperty("max", x.max);
                a.add(e);
            }
            o.add("add", a);
            return o;
        }

        public static Table fromJson(JsonObject o) {
            Table t = new Table();
            t.clear = o.has("clear") && o.get("clear").getAsBoolean();
            if (o.has("remove")) for (JsonElement e : o.getAsJsonArray("remove")) t.remove.add(e.getAsString().trim());
            if (o.has("add")) {
                for (JsonElement e : o.getAsJsonArray("add")) {
                    JsonObject x = e.getAsJsonObject();
                    Add a = new Add();
                    a.item = x.get("item").getAsString().trim();
                    a.chance = Math.max(0, Math.min(1, x.has("chance") ? x.get("chance").getAsDouble() : 1));
                    a.min = Math.max(1, x.has("min") ? x.get("min").getAsInt() : 1);
                    a.max = Math.max(a.min, x.has("max") ? x.get("max").getAsInt() : a.min);
                    t.add.add(a);
                }
            }
            return t;
        }
    }

    public static final class Rules {
        public final Map<String, Table> tables = new TreeMap<>();

        public JsonObject toJson() {
            JsonObject o = new JsonObject();
            JsonArray help = new JsonArray();
            help.add("상자 등 전리품 규칙. 게임 안에서 /rtshop loot (OP) 로 확률을 보며 고칠 수 있습니다.");
            help.add("tables 의 키: 전리품표 ID (예: minecraft:chests/simple_dungeon) 또는 끝이 * 인 묶음 (예: minecraft:chests/*)");
            help.add("clear: true 면 원래 나오던 것을 모두 지움 · remove: 나오지 않게 할 아이템 · add: {item, chance(0~1), min, max} 확률대로 추가");
            help.add("원래 나오던 아이템의 확률을 바꾸려면 remove 와 add 에 함께 넣습니다");
            o.add("_help", help);
            JsonObject t = new JsonObject();
            for (Map.Entry<String, Table> e : tables.entrySet()) {
                if (!e.getValue().empty()) t.add(e.getKey(), e.getValue().toJson());
            }
            o.add("tables", t);
            return o;
        }

        public static Rules fromJson(JsonObject o) {
            Rules r = new Rules();
            if (o.has("tables")) {
                for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("tables").entrySet()) {
                    r.tables.put(e.getKey().trim(), Table.fromJson(e.getValue().getAsJsonObject()));
                }
            }
            return r;
        }

        /** 이 전리품표에 맞는 규칙들 (묶음 먼저, 정확한 ID 나중). */
        public List<Table> matching(String id) {
            List<Table> out = new ArrayList<>();
            for (Map.Entry<String, Table> e : tables.entrySet()) {
                String k = e.getKey();
                if (k.endsWith("*") && id.startsWith(k.substring(0, k.length() - 1))) out.add(e.getValue());
            }
            Table exact = tables.get(id);
            if (exact != null) out.add(exact);
            return out;
        }
    }

    public static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("loot.json");
    }

    private static Rules rules;

    public static synchronized Rules get() {
        if (rules == null) rules = load();
        return rules;
    }

    private static Rules load() {
        try {
            if (Files.exists(path())) {
                return Rules.fromJson(JsonParser.parseString(Files.readString(path(), StandardCharsets.UTF_8)).getAsJsonObject());
            }
            Rules r = new Rules();
            save(r);
            return r;
        } catch (Exception e) {
            RtShop.LOGGER.error("[rtshop] loot.json 을 읽지 못했습니다: {}", e.getMessage());
            return new Rules();
        }
    }

    public static synchronized void save(Rules r) throws Exception {
        Files.createDirectories(path().getParent());
        Files.writeString(path(), new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(r.toJson()), StandardCharsets.UTF_8);
        rules = r;
    }

    public static synchronized void reload() {
        rules = load();
    }

    /** 시험용: 파일을 건드리지 않고 규칙을 바꾼다. */
    public static synchronized void setForTest(Rules r) {
        rules = r;
    }

    static Item item(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null || !ForgeRegistries.ITEMS.containsKey(rl) ? null : ForgeRegistries.ITEMS.getValue(rl);
    }

    static String idOf(ItemStack s) {
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(s.getItem());
        return rl == null ? "" : rl.toString();
    }

    public static void applyRules(List<Table> ts, ObjectArrayList<ItemStack> loot, net.minecraft.util.RandomSource rnd) {
        for (Table t : ts) {
            if (t.clear) loot.clear();
            if (!t.remove.isEmpty()) loot.removeIf(s -> t.remove.contains(idOf(s)));
            for (Add a : t.add) {
                Item it = item(a.item);
                if (it == null || rnd.nextDouble() >= a.chance) continue;
                int n = a.min + (a.max > a.min ? rnd.nextInt(a.max - a.min + 1) : 0);
                while (n > 0) {
                    int k = Math.min(n, it.getMaxStackSize());
                    loot.add(new ItemStack(it, k));
                    n -= k;
                }
            }
        }
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext ctx) {
        try {
            loot.removeIf(WorldRules::lootBlocked); // 월드 규칙: 이 서버에 없는 모드의 물건은 전리품에서 뺀다
            Rules r = get();
            if (r.tables.isEmpty()) return loot;
            List<Table> ts = r.matching(ctx.getQueriedLootTableId().toString());
            if (!ts.isEmpty()) applyRules(ts, loot, ctx.getRandom());
        } catch (Throwable t) {
            RtShop.LOGGER.warn("[rtshop] 전리품 규칙 적용 실패: {}", t.toString());
        }
        return loot;
    }

    // ------------------------------------------------------------------ 확률 보기 (표본 뽑기)

    /** 편집기에 보일 전리품표: 상자(야생 보관함) + 몹 처치 전리품. */
    public static List<String> editableTables(MinecraftServer server) {
        List<String> out = chestTables(server);
        List<String> mobs = new ArrayList<>();
        for (ResourceLocation id : server.getLootData().getKeys(LootDataType.TABLE)) {
            if (!id.getPath().startsWith("entities/")) continue;
            String e = id.getNamespace() + ":" + id.getPath().substring("entities/".length());
            if (ForgeRegistries.ENTITY_TYPES.containsKey(ResourceLocation.tryParse(e))) mobs.add(id.toString());
        }
        mobs.sort(String::compareTo);
        out.addAll(mobs);
        return out;
    }

    /** 상자 전리품표 목록. */
    public static List<String> chestTables(MinecraftServer server) {
        List<String> out = new ArrayList<>();
        for (ResourceLocation id : server.getLootData().getKeys(LootDataType.TABLE)) {
            if (id.getPath().startsWith("chests/") || id.getPath().contains("/chests/")) out.add(id.toString());
        }
        out.sort(String::compareTo);
        return out;
    }

    public static final class Stat {
        public int hits;
        public long total;
        public int min = Integer.MAX_VALUE, max;
    }

    /** 전리품표를 n번 열어 본 결과: 아이템 -> 나온 횟수·개수. raw = 규칙 적용 전(원래), 아니면 적용 후(다른 모드 규칙 포함). */
    public static Map<String, Stat> sample(ServerLevel level, ResourceLocation table, int n, boolean raw) {
        Map<String, Stat> out = new LinkedHashMap<>();
        LootTable lt = level.getServer().getLootData().getLootTable(table);
        // 몹 전리품표: 그 몹을 (세계에 넣지 않고) 하나 만들어 플레이어가 잡은 것처럼 연다
        net.minecraft.world.entity.Entity victim = null;
        net.minecraftforge.common.util.FakePlayer killer = null;
        if (table.getPath().startsWith("entities/")) {
            ResourceLocation eid = new ResourceLocation(table.getNamespace(), table.getPath().substring("entities/".length()));
            net.minecraft.world.entity.EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(eid);
            try {
                victim = type == null ? null : type.create(level);
            } catch (Throwable t) {
                victim = null;
            }
            if (victim == null) return out;
            killer = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);
        }
        for (int i = 0; i < n; i++) {
            LootParams params = victim != null
                    ? new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY, victim)
                    .withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                    .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(killer))
                    .withOptionalParameter(LootContextParams.KILLER_ENTITY, killer)
                    .withOptionalParameter(LootContextParams.DIRECT_KILLER_ENTITY, killer)
                    .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer).create(LootContextParamSets.ENTITY)
                    : new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST);
            List<ItemStack> got = new ArrayList<>();
            try {
                if (raw) {
                    LootContext ctx = new LootContext.Builder(params).create(null);
                    lt.getRandomItemsRaw(ctx, got::add);
                } else {
                    got.addAll(lt.getRandomItems(params));
                }
            } catch (Throwable t) {
                break;
            }
            Map<String, Integer> once = new LinkedHashMap<>();
            for (ItemStack s : got) {
                if (!s.isEmpty()) once.merge(idOf(s), s.getCount(), Integer::sum);
            }
            for (Map.Entry<String, Integer> e : once.entrySet()) {
                Stat st = out.computeIfAbsent(e.getKey(), k -> new Stat());
                st.hits++;
                st.total += e.getValue();
                st.min = Math.min(st.min, e.getValue());
                st.max = Math.max(st.max, e.getValue());
            }
        }
        return out;
    }
}

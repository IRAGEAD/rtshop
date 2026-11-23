package com.rtshop.gacha;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import com.rtshop.economy.EconomyConfig;
import com.rtshop.item.ModItems;
import com.rtshop.net.GachaActionPacket;
import com.rtshop.net.GachaPacket;
import com.rtshop.net.ResultPacket;
import com.rtshop.net.ShopNet;
import com.rtshop.shop.ShopServer;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 뽑기: 무기·펫·장신구 × 일반·중급·고급. 티켓(Gold 로 삼, 주고받기 가능) 한 장에 한 번.
 * 뽑은 아이템에는 '뽑기 표시'가 붙어 상점에 되팔 수 없고, 대신 뽑기 NPC 에게 분해해 조각을 받아 원하는 아이템과 바꿀 수 있다.
 * 천장(일정 횟수 안에 높은 등급 보장)은 아직 구상만: 뽑은 횟수는 GachaData 에 기록해 둔다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class Gacha {
    private Gacha() {
    }

    public static final String NPC_TAG = "rtshop_gacha";
    /** 뽑은 아이템 표시 (NBT): {g: 등급, c: 종류, k: 칸 key} */
    public static final String MARK = "rtshop_gacha";

    // ------------------------------------------------------------------ 뽑기 (순수 계산)

    private static List<List<GachaConfig.Entry>> byGrade(GachaConfig c, String cat) {
        List<List<GachaConfig.Entry>> g = new ArrayList<>();
        for (int i = 0; i < GachaConfig.GRADES.length; i++) {
            g.add(new ArrayList<>());
        }
        for (GachaConfig.Entry e : c.pool(cat)) {
            int gi = GachaConfig.gradeIndex(e.grade);
            if (gi >= 0 && e.weight > 0 && GachaConfig.valid(e)) {
                g.get(gi).add(e);
            }
        }
        return g;
    }

    /** 등급별 실제 확률 (아이템이 없는 등급은 빼고 나머지 비율). */
    public static double[] gradeChances(GachaConfig c, String cat, String tier) {
        double[] out = new double[GachaConfig.GRADES.length];
        GachaConfig.Tier t = c.tiers.get(tier);
        if (t == null) {
            return out;
        }
        List<List<GachaConfig.Entry>> g = byGrade(c, cat);
        double sum = 0;
        for (int i = 0; i < out.length; i++) {
            out[i] = g.get(i).isEmpty() ? 0 : t.weights[i];
            sum += out[i];
        }
        for (int i = 0; i < out.length; i++) {
            out[i] = sum <= 0 ? 0 : out[i] / sum;
        }
        return out;
    }

    private static int pickGrade(double[] w, int minGrade, RandomSource rng) {
        double sum = 0;
        for (int i = minGrade; i < w.length; i++) {
            sum += w[i];
        }
        if (sum <= 0) {
            return -1;
        }
        double r = rng.nextDouble() * sum;
        for (int i = minGrade; i < w.length; i++) {
            r -= w[i];
            if (r < 0 && w[i] > 0) {
                return i;
            }
        }
        for (int i = w.length - 1; i >= minGrade; i--) {
            if (w[i] > 0) {
                return i;
            }
        }
        return -1;
    }

    private static GachaConfig.Entry pickIn(List<GachaConfig.Entry> list, RandomSource rng) {
        int sum = 0;
        for (GachaConfig.Entry e : list) {
            sum += e.weight;
        }
        int r = rng.nextInt(Math.max(1, sum));
        for (GachaConfig.Entry e : list) {
            r -= e.weight;
            if (r < 0) {
                return e;
            }
        }
        return list.get(list.size() - 1);
    }

    /** n 번 뽑는다. 10회면 하나는 보장 등급 이상. 뽑을 것이 없으면 빈 목록. */
    public static List<GachaConfig.Entry> pull(GachaConfig c, String cat, String tier, int n, RandomSource rng) {
        List<GachaConfig.Entry> out = new ArrayList<>();
        double[] w = gradeChances(c, cat, tier);
        List<List<GachaConfig.Entry>> g = byGrade(c, cat);
        for (int i = 0; i < n; i++) {
            int gi = pickGrade(w, 0, rng);
            if (gi < 0) {
                return new ArrayList<>();
            }
            out.add(pickIn(g.get(gi), rng));
        }
        int min = GachaConfig.gradeIndex(c.tenGuarantee);
        if (n >= 10 && min >= 0) {
            boolean ok = false;
            for (GachaConfig.Entry e : out) {
                ok |= GachaConfig.gradeIndex(e.grade) >= min;
            }
            if (!ok) {
                int gi = pickGrade(w, min, rng);
                if (gi < 0) {
                    // 이 뽑기 확률표에 보장 등급이 없으면, 아이템이 있는 가장 낮은 보장 등급 이상에서
                    for (int k = min; k < GachaConfig.GRADES.length && gi < 0; k++) {
                        if (!g.get(k).isEmpty()) {
                            gi = k;
                        }
                    }
                }
                if (gi >= 0) {
                    out.set(out.size() - 1, pickIn(g.get(gi), rng));
                }
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ 아이템

    /** 뽑기 종류·단계별 티켓. */
    public static Item ticket(String cat, String tier) {
        int t = "high".equals(tier) ? 2 : "mid".equals(tier) ? 1 : 0;
        return switch (cat) {
            case "pet" -> (t == 2 ? ModItems.PET_TICKET_HIGH : t == 1 ? ModItems.PET_TICKET_MID : ModItems.PET_TICKET_NORMAL).get();
            case "accessory" -> (t == 2 ? ModItems.ACC_TICKET_HIGH : t == 1 ? ModItems.ACC_TICKET_MID : ModItems.ACC_TICKET_NORMAL).get();
            default -> (t == 2 ? ModItems.TICKET_HIGH : t == 1 ? ModItems.TICKET_MID : ModItems.TICKET_NORMAL).get();
        };
    }

    /** 조각 보상 칸 (펫 뽑기의 일반·고급 등급): 뽑기 표시 없이 그냥 조각을 준다. */
    public static boolean isShardEntry(GachaConfig.Entry e) {
        return e.entity.isEmpty() && (RtShop.MODID + ":gacha_shard").equals(e.item);
    }

    /** 뽑은 칸을 실제 아이템으로 (펫은 소환권). 뽑기 표시를 붙인다. */
    public static ItemStack stackOf(GachaConfig.Entry e, String cat) {
        ItemStack s;
        if (!e.entity.isEmpty()) {
            s = new ItemStack(ModItems.PET_SUMMON.get());
            s.getOrCreateTag().putString("entity", e.entity);
        } else {
            Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(e.item));
            if (item == null) {
                return ItemStack.EMPTY;
            }
            s = new ItemStack(item, e.count);
            if (!e.nbt.isEmpty()) {
                try {
                    CompoundTag t = TagParser.parseTag(e.nbt);
                    s.getOrCreateTag().merge(t);
                } catch (Exception ex) {
                    RtShop.LOGGER.warn("[rtshop] 뽑기 아이템 {} 의 NBT 를 읽지 못했습니다: {}", e.item, ex.getMessage());
                }
            }
        }
        if (isShardEntry(e)) {
            return s; // 조각은 표시 없이 (분해·교환에 바로 쓰도록)
        }
        CompoundTag m = new CompoundTag();
        m.putString("g", e.grade);
        m.putString("c", cat);
        m.putString("k", e.key());
        s.getOrCreateTag().put(MARK, m);
        protect(s);
        return s;
    }

    /** 이 뽑기 장비가 내구도 보호 대상인가 (설정한 등급 이상, 내구도가 있는 물건). */
    public static boolean shouldProtect(ItemStack s) {
        if (!marked(s)) return false;
        String from = GachaConfig.get().unbreakableFrom;
        if (from == null || from.isEmpty()) return false;
        int min = GachaConfig.gradeIndex(from);
        return min >= 0 && GachaConfig.gradeIndex(gradeOf(s)) >= min && s.getItem().canBeDepleted();
    }

    /** 영웅 이상 뽑기 장비: 닳지 않게(Unbreakable) 하고 닳아 있던 내구도는 채운다. 바꿨으면 true. */
    public static boolean protect(ItemStack s) {
        if (!shouldProtect(s)) return false;
        CompoundTag t = s.getOrCreateTag();
        if (t.getBoolean("Unbreakable") && s.getDamageValue() == 0) return false;
        t.putBoolean("Unbreakable", true);
        s.setDamageValue(0);
        return true;
    }

    /** 예전에 뽑아 둔 장비도 가방에 들어오면 보호한다 (2초마다 가방만 훑음). */
    @SubscribeEvent
    public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END || event.player.level().isClientSide()
                || (event.player.tickCount + event.player.getId()) % 40 != 0) {
            return;
        }
        net.minecraft.world.entity.player.Inventory inv = event.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (marked(st)) protect(st);
        }
    }

    public static boolean marked(ItemStack s) {
        return !s.isEmpty() && s.hasTag() && s.getTag().contains(MARK);
    }

    public static String gradeOf(ItemStack s) {
        return marked(s) ? s.getTag().getCompound(MARK).getString("g") : "";
    }

    private static void give(ServerPlayer p, ItemStack s) {
        if (s.isEmpty()) {
            return;
        }
        if (!p.getInventory().add(s)) {
            p.drop(s, false);
        }
    }

    private static int count(ServerPlayer p, Item item) {
        return p.getInventory().countItem(item);
    }

    /** 티켓 n 장을 쓰고 난 뒤 가방(주 가방 36칸)의 빈칸 수. 다 써서 비는 티켓 칸도 센다. */
    public static int freeSlotsAfterTaking(ServerPlayer p, Item item, int n) {
        Inventory inv = p.getInventory();
        int free = 0;
        int left = n;
        for (int i = 0; i < inv.items.size(); i++) {
            ItemStack s = inv.items.get(i);
            if (s.isEmpty()) {
                free++;
            } else if (left > 0 && s.is(item)) {
                int t = Math.min(left, s.getCount());
                left -= t;
                if (t == s.getCount()) free++;
            }
        }
        return free;
    }

    private static boolean take(ServerPlayer p, Item item, int n) {
        if (count(p, item) < n) {
            return false;
        }
        Inventory inv = p.getInventory();
        int left = n;
        for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item)) {
                int t = Math.min(left, s.getCount());
                s.shrink(t);
                left -= t;
            }
        }
        inv.setChanged();
        return true;
    }

    private static void giveShards(ServerPlayer p, int n) {
        Item shard = ModItems.SHARD.get();
        int max = new ItemStack(shard).getMaxStackSize();
        while (n > 0) {
            int k = Math.min(n, max);
            give(p, new ItemStack(shard, k));
            n -= k;
        }
    }

    // ------------------------------------------------------------------ 플레이어 동작

    private static final Map<UUID, Integer> SESSIONS = new HashMap<>();

    private static boolean real(ServerPlayer p) {
        return !(p instanceof FakePlayer) && p.connection != null;
    }

    public static boolean canEdit(ServerPlayer p) {
        return !(p instanceof FakePlayer) && p.server.getPlayerList().isOp(p.getGameProfile());
    }

    private static boolean nearNpc(ServerPlayer p) {
        Integer id = SESSIONS.get(p.getUUID());
        if (id == null || id < 0) {
            return false;
        }
        Entity e = p.level().getEntity(id);
        double r = EconomyConfig.get().npcRange;
        return e != null && e.isAlive() && e.getTags().contains(NPC_TAG) && e.distanceToSqr(p) <= r * r;
    }

    private static void result(ServerPlayer p, Component msg, boolean ok) {
        if (real(p)) {
            ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new ResultPacket(msg, ok));
        }
    }

    private static void send(ServerPlayer p, int mode, JsonObject o) {
        if (real(p)) {
            ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new GachaPacket(mode, o.toString()));
        }
    }

    /** 티켓 상점에서 뽑기 화면으로 돌아간다 (같은 행운 상인 앞에서). */
    public static void back(ServerPlayer p) {
        Integer id = SESSIONS.get(p.getUUID());
        open(p, id == null ? -1 : id);
    }

    public static void open(ServerPlayer p, int npcId) {
        SESSIONS.put(p.getUUID(), npcId);
        send(p, GachaPacket.VIEW, view(p));
    }

    /** 뽑기. 성공하면 뽑은 칸들 (점검에서 쓰려고 돌려준다), 실패하면 null. needNpc=false 는 점검용. */
    public static List<GachaConfig.Entry> doPull(ServerPlayer p, String cat, String tier, int n, boolean needNpc) {
        GachaConfig c = GachaConfig.get();
        if (needNpc && !nearNpc(p)) {
            result(p, Component.translatable("rtshop.gacha.need_npc", c.npcName), false);
            return null;
        }
        if ((n != 1 && n != 10) || !c.tiers.containsKey(tier) || !c.pools.containsKey(cat)) {
            return null;
        }
        if (c.loadError != null) {
            result(p, Component.translatable("rtshop.gacha.broken"), false);
            return null;
        }
        double[] w = gradeChances(c, cat, tier);
        double sum = 0;
        for (double x : w) {
            sum += x;
        }
        if (sum <= 0) {
            result(p, Component.translatable("rtshop.gacha.empty"), false);
            return null;
        }
        Item t = ticket(cat, tier);
        if (count(p, t) >= n) {
            int free = freeSlotsAfterTaking(p, t, n);
            if (free < n) {
                result(p, Component.translatable("rtshop.gacha.inventory_full", n, free), false);
                return null;
            }
        }
        if (!take(p, t, n)) {
            result(p, Component.translatable("rtshop.gacha.no_ticket", new ItemStack(t).getHoverName(), n), false);
            return null;
        }
        List<GachaConfig.Entry> got = pull(c, cat, tier, n, p.getRandom());
        GachaData d = GachaData.get(p.server);
        GachaData.Entry pe = d.of(p.getUUID());
        pe.pulls.merge(cat + "/" + tier, n, Integer::sum);
        JsonArray res = new JsonArray();
        long now = System.currentTimeMillis();
        for (GachaConfig.Entry e : got) {
            boolean fresh = !isShardEntry(e) && pe.owned.add(e.key());
            ItemStack s = stackOf(e, cat);
            JsonObject r = e.toJson();
            r.addProperty("new", fresh);
            res.add(r);
            if (isShardEntry(e)) {
                giveShards(p, e.count);
            } else {
                give(p, s);
            }
            pe.history.add(0, e.grade + "|" + cat + "|" + e.key() + "|" + now);
            int gIdx = GachaConfig.gradeIndex(e.grade);
            if (gIdx >= 4) {
                Component name = s.getItem() == ModItems.PET_SUMMON.get() ? petName(e.entity) : s.getHoverName();
                boolean yamato = gIdx == GachaConfig.YAMATO;
                p.server.getPlayerList().broadcastSystemMessage(Component.translatable(yamato ? "rtshop.gacha.broadcast_yamato" : "rtshop.gacha.broadcast",
                        p.getDisplayName(), Component.translatable("rtshop.gacha.tier." + tier), name)
                        .withStyle(yamato ? ChatFormatting.AQUA : ChatFormatting.GOLD), false);
            }
        }
        while (pe.history.size() > GachaData.HISTORY) {
            pe.history.remove(pe.history.size() - 1);
        }
        d.dirty();
        JsonObject o = view(p);
        o.add("results", res);
        o.addProperty("resultCat", cat);
        o.addProperty("resultTier", tier);
        send(p, GachaPacket.RESULT, o);
        return got;
    }

    public static Component petName(String entity) {
        EntityType<?> t = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.tryParse(entity));
        return t == null ? Component.literal(entity) : t.getDescription();
    }

    /** 뽑기 표시가 붙은 인벤토리 칸 하나를 분해한다. 받은 조각 수 (실패 0). */
    public static int dismantle(ServerPlayer p, int slot, boolean needNpc) {
        if (needNpc && !nearNpc(p)) {
            result(p, Component.translatable("rtshop.gacha.need_npc", GachaConfig.get().npcName), false);
            return 0;
        }
        Inventory inv = p.getInventory();
        if (slot < 0 || slot >= inv.items.size()) {
            return 0;
        }
        ItemStack s = inv.items.get(slot);
        int gi = GachaConfig.gradeIndex(gradeOf(s));
        if (gi < 0) {
            return 0;
        }
        int n = GachaConfig.get().dismantle[gi] * s.getCount();
        inv.items.set(slot, ItemStack.EMPTY);
        inv.setChanged();
        giveShards(p, n);
        return n;
    }

    /** 이 등급 이하의 뽑기 아이템을 모두 분해. */
    public static int dismantleAll(ServerPlayer p, int maxGrade, boolean needNpc) {
        if (needNpc && !nearNpc(p)) {
            result(p, Component.translatable("rtshop.gacha.need_npc", GachaConfig.get().npcName), false);
            return 0;
        }
        int total = 0;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.items.size(); i++) {
            int gi = GachaConfig.gradeIndex(gradeOf(inv.items.get(i)));
            if (gi >= 0 && gi <= maxGrade) {
                total += dismantle(p, i, false);
            }
        }
        return total;
    }

    /** 조각으로 뽑기 티켓을 바꾼다 (n 장). */
    public static boolean exchange(ServerPlayer p, String cat, String tier, int n, boolean needNpc) {
        GachaConfig c = GachaConfig.get();
        if (needNpc && !nearNpc(p)) {
            result(p, Component.translatable("rtshop.gacha.need_npc", c.npcName), false);
            return false;
        }
        int ti = java.util.Arrays.asList(GachaConfig.TIERS).indexOf(tier);
        int ci = java.util.Arrays.asList(GachaConfig.CATEGORIES).indexOf(cat);
        if (!c.exchangeEnabled || ti < 0 || ci < 0 || n < 1 || n > 64) {
            return false;
        }
        int cost = c.ticketCost[ci][ti] * n;
        if (!take(p, ModItems.SHARD.get(), cost)) {
            result(p, Component.translatable("rtshop.gacha.no_shards", cost), false);
            return false;
        }
        ItemStack s = new ItemStack(ticket(cat, tier), n);
        Component name = s.getHoverName();
        give(p, s);
        result(p, Component.translatable("rtshop.gacha.exchanged", name, n, cost), true);
        return true;
    }

    public static void handleAction(ServerPlayer p, int action, String arg) {
        JsonObject a;
        try {
            a = arg.isEmpty() ? new JsonObject() : JsonParser.parseString(arg).getAsJsonObject();
        } catch (Exception e) {
            return;
        }
        String cat = a.has("cat") ? a.get("cat").getAsString() : "";
        switch (action) {
            case GachaActionPacket.PULL -> doPull(p, cat, a.has("tier") ? a.get("tier").getAsString() : "", a.has("n") ? a.get("n").getAsInt() : 1, true);
            case GachaActionPacket.DISMANTLE -> {
                int n = dismantle(p, a.has("slot") ? a.get("slot").getAsInt() : -1, true);
                if (n > 0) {
                    result(p, Component.translatable("rtshop.gacha.dismantled", n), true);
                }
                send(p, GachaPacket.VIEW, view(p));
            }
            case GachaActionPacket.DISMANTLE_ALL -> {
                int n = dismantleAll(p, a.has("max") ? a.get("max").getAsInt() : 0, true);
                result(p, n > 0 ? Component.translatable("rtshop.gacha.dismantled", n) : Component.translatable("rtshop.gacha.nothing_to_dismantle"), n > 0);
                send(p, GachaPacket.VIEW, view(p));
            }
            case GachaActionPacket.EXCHANGE -> {
                exchange(p, cat, a.has("tier") ? a.get("tier").getAsString() : "", a.has("n") ? a.get("n").getAsInt() : 1, true);
                send(p, GachaPacket.VIEW, view(p));
            }
            case GachaActionPacket.SHOP -> {
                GachaConfig c = GachaConfig.get();
                if (!nearNpc(p)) {
                    result(p, Component.translatable("rtshop.gacha.need_npc", c.npcName), false);
                } else if (ShopServer.shops().get(c.shop) == null) {
                    result(p, Component.translatable("rtshop.msg.unknown_shop", c.shop), false);
                } else {
                    ShopServer.openShop(p, c.shop, SESSIONS.get(p.getUUID()));
                }
            }
            case GachaActionPacket.EDIT -> openEditor(p);
            case GachaActionPacket.SAVE -> handleSave(p, a.has("json") ? a.get("json").getAsString() : "");
            case GachaActionPacket.AUTOFILL -> {
                if (!canEdit(p)) {
                    return;
                }
                JsonObject o = new JsonObject();
                o.addProperty("cat", cat);
                JsonArray arr = new JsonArray();
                for (GachaConfig.Entry e : GachaConfig.get().auto(cat, p.server)) {
                    arr.add(e.toJson());
                }
                o.add("pool", arr);
                send(p, GachaPacket.AUTOFILL, o);
            }
            case GachaActionPacket.SIMULATE -> {
                if (!canEdit(p)) {
                    return;
                }
                send(p, GachaPacket.SIM, simulate(GachaConfig.get(), cat, a.has("tier") ? a.get("tier").getAsString() : "normal", 1000));
            }
            default -> {
            }
        }
    }

    /** 모의 뽑기: 등급별 개수와 가장 많이 나온 아이템. */
    public static JsonObject simulate(GachaConfig c, String cat, String tier, int n) {
        RandomSource rng = RandomSource.create();
        int[] grades = new int[GachaConfig.GRADES.length];
        Map<String, Integer> top = new LinkedHashMap<>();
        Map<String, String> label = new HashMap<>();
        for (int i = 0; i < n; i++) {
            List<GachaConfig.Entry> r = pull(c, cat, tier, 1, rng);
            if (r.isEmpty()) {
                break;
            }
            GachaConfig.Entry e = r.get(0);
            grades[GachaConfig.gradeIndex(e.grade)]++;
            top.merge(e.key(), 1, Integer::sum);
            label.put(e.key(), e.entity.isEmpty() ? e.item : e.entity);
        }
        JsonObject o = new JsonObject();
        o.addProperty("cat", cat);
        o.addProperty("tier", tier);
        o.addProperty("n", n);
        JsonArray g = new JsonArray();
        for (int x : grades) {
            g.add(x);
        }
        o.add("grades", g);
        JsonArray t = new JsonArray();
        top.entrySet().stream().sorted((x, y) -> y.getValue() - x.getValue()).limit(5).forEach(en -> {
            JsonObject j = new JsonObject();
            j.addProperty("id", label.get(en.getKey()));
            j.addProperty("count", en.getValue());
            t.add(j);
        });
        o.add("top", t);
        return o;
    }

    // ------------------------------------------------------------------ 창에 보낼 내용

    public static JsonObject view(ServerPlayer p) {
        GachaConfig c = GachaConfig.get();
        JsonObject o = new JsonObject();
        o.addProperty("npc", c.npcName);
        o.addProperty("atNpc", nearNpc(p));
        o.addProperty("canEdit", canEdit(p));
        o.addProperty("shop", ShopServer.shops().get(c.shop) != null);
        o.addProperty("shards", count(p, ModItems.SHARD.get()));
        o.addProperty("tenGuarantee", c.tenGuarantee);
        o.addProperty("exchangeEnabled", c.exchangeEnabled);
        JsonObject tickets = new JsonObject();
        JsonObject costs = new JsonObject();
        JsonObject chances = new JsonObject();
        for (int ci = 0; ci < GachaConfig.CATEGORIES.length; ci++) {
            String cat = GachaConfig.CATEGORIES[ci];
            JsonArray tk = new JsonArray();
            JsonArray cs = new JsonArray();
            for (int ti = 0; ti < GachaConfig.TIERS.length; ti++) {
                tk.add(count(p, ticket(cat, GachaConfig.TIERS[ti])));
                cs.add(c.ticketCost[ci][ti]);
            }
            tickets.add(cat, tk);
            costs.add(cat, cs);
        }
        for (String t : GachaConfig.TIERS) {
            JsonObject byCat = new JsonObject();
            for (String cat : GachaConfig.CATEGORIES) {
                JsonArray arr = new JsonArray();
                for (double x : gradeChances(c, cat, t)) {
                    arr.add(x);
                }
                byCat.add(cat, arr);
            }
            chances.add(t, byCat);
        }
        o.add("tickets", tickets);
        o.add("ticketCost", costs);
        o.add("chances", chances);
        JsonArray dm = new JsonArray();
        for (int i = 0; i < GachaConfig.GRADES.length; i++) {
            dm.add(c.dismantle[i]);
        }
        o.add("dismantle", dm);
        GachaData.Entry pe = GachaData.get(p.server).of(p.getUUID());
        JsonObject pools = new JsonObject();
        for (String cat : GachaConfig.CATEGORIES) {
            JsonArray arr = new JsonArray();
            List<GachaConfig.Entry> pool = c.pool(cat);
            for (int i = 0; i < pool.size(); i++) {
                GachaConfig.Entry e = pool.get(i);
                JsonObject j = e.toJson();
                j.addProperty("index", i);
                j.addProperty("valid", GachaConfig.valid(e) && e.weight > 0);
                j.addProperty("owned", pe.owned.contains(e.key()));
                arr.add(j);
            }
            pools.add(cat, arr);
        }
        o.add("pools", pools);
        JsonArray inv = new JsonArray();
        List<ItemStack> items = p.getInventory().items;
        for (int i = 0; i < items.size(); i++) {
            ItemStack s = items.get(i);
            if (marked(s)) {
                JsonObject j = new JsonObject();
                j.addProperty("slot", i);
                j.addProperty("item", String.valueOf(ForgeRegistries.ITEMS.getKey(s.getItem())));
                j.addProperty("count", s.getCount());
                j.addProperty("grade", gradeOf(s));
                if (s.getTag().contains("entity")) {
                    j.addProperty("entity", s.getTag().getString("entity"));
                }
                inv.add(j);
            }
        }
        o.add("inventory", inv);
        return o;
    }

    // ------------------------------------------------------------------ 편집 (OP)

    public static void openEditor(ServerPlayer p) {
        if (!canEdit(p)) {
            p.sendSystemMessage(Component.translatable("rtshop.gacha.op_only"));
            return;
        }
        send(p, GachaPacket.EDITOR, GachaConfig.get().toJson());
    }

    public static String save(String json, net.minecraft.server.MinecraftServer server) {
        GachaConfig c;
        try {
            c = GachaConfig.fromJson(JsonParser.parseString(json).getAsJsonObject());
        } catch (Exception e) {
            return "저장하지 못했습니다: " + e.getMessage();
        }
        try {
            c.save();
        } catch (Exception e) {
            return "파일에 쓰지 못했습니다: " + e.getMessage();
        }
        GachaConfig.reload(server);
        return null;
    }

    private static void handleSave(ServerPlayer p, String json) {
        if (!canEdit(p)) {
            p.sendSystemMessage(Component.translatable("rtshop.gacha.op_only"));
            return;
        }
        String err = save(json, p.server);
        if (err == null) {
            RtShop.LOGGER.info("[rtshop] {} 이(가) 뽑기 설정을 고쳤습니다", p.getGameProfile().getName());
            String name = GachaConfig.get().npcName;
            for (ServerLevel level : p.server.getAllLevels()) {
                for (Entity e : level.getAllEntities()) {
                    if (e.getTags().contains(NPC_TAG) && (e.getCustomName() == null || !e.getCustomName().getString().equals(name))) {
                        e.setCustomName(Component.literal(name));
                    }
                }
            }
        }
        openEditor(p);
        result(p, err == null ? Component.translatable("rtshop.gacha.saved") : Component.literal(err), err == null);
    }

    // ------------------------------------------------------------------ NPC

    public static Villager spawnNpc(ServerLevel level, Vec3 pos, float yaw) {
        Villager v = EntityType.VILLAGER.create(level);
        if (v == null) {
            return null;
        }
        v.moveTo(pos.x, pos.y, pos.z, yaw, 0.0F);
        v.setNoAi(true);
        v.setInvulnerable(true);
        v.setSilent(true);
        v.setPersistenceRequired();
        v.addTag(NPC_TAG);
        v.setCustomName(Component.literal(GachaConfig.get().npcName));
        v.setCustomNameVisible(true);
        v.setVillagerData(new VillagerData(VillagerType.PLAINS, VillagerProfession.CLERIC, 5));
        level.addFreshEntity(v);
        return v;
    }

    public static boolean isNpc(Entity e) {
        return e.getTags().contains(NPC_TAG);
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() || !event.getTarget().getTags().contains(NPC_TAG)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getHand() == InteractionHand.MAIN_HAND && event.getEntity() instanceof ServerPlayer sp) {
            open(sp, event.getTarget().getId());
        }
    }

    /** 예전 이름표의 오타('릭키')를 고친다. */
    @SubscribeEvent
    public static void onJoin(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        Entity e = event.getEntity();
        if (event.getLevel().isClientSide() || !e.getTags().contains(NPC_TAG) || e.getCustomName() == null) return;
        String n = e.getCustomName().getString();
        if (n.contains("릭키")) e.setCustomName(net.minecraft.network.chat.Component.literal(n.replace("릭키", "럭키")));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SESSIONS.remove(event.getEntity().getUUID());
    }
}

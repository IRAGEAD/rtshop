package com.rtshop.magic;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import com.rtshop.gacha.Gacha;
import com.rtshop.gacha.GachaConfig;
import com.rtshop.net.MagicPacket;
import com.rtshop.net.ResultPacket;
import com.rtshop.net.ShopNet;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.server.ServerStartingEvent;
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
 * 마법 뽑기 (별빛 수정 구체): 스킬북 뽑기(분야 선택·무작위)와 무기 인챈트 재설정.
 * 뽑기 계산은 무기 뽑기 엔진(Gacha.pull)을 그대로 쓰고, 뽑은 스킬북에는 무기 뽑기와 같은 표시를 붙여 행운 상인에게서 분해할 수 있다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class MagicGacha {
    private MagicGacha() {
    }

    /** 플레이어 -> 연 구체의 자리 (가까이 있어야 뽑을 수 있다). */
    private static final Map<UUID, BlockPos> SESSIONS = new HashMap<>();
    public static final double RANGE = 8;

    @SubscribeEvent
    public static void onStarting(ServerStartingEvent event) {
        MagicConfig.reload(event.getServer());
    }

    // ------------------------------------------------------------------ 도우미

    private static boolean real(ServerPlayer p) {
        return !(p instanceof FakePlayer) && p.connection != null;
    }

    public static boolean canEdit(ServerPlayer p) {
        return !(p instanceof FakePlayer) && p.server.getPlayerList().isOp(p.getGameProfile()) && com.rtshop.util.Perm.op(p); // 서바이벌의 OP 는 편집할 수 없다
    }

    private static void send(ServerPlayer p, int kind, JsonObject o) {
        if (real(p)) ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new MagicPacket(kind, o.toString()));
    }

    private static void fail(ServerPlayer p, Component msg) {
        if (real(p)) ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new ResultPacket(msg, false));
    }

    private static boolean near(ServerPlayer p) {
        BlockPos pos = SESSIONS.get(p.getUUID());
        return pos != null && p.level().getBlockState(pos).getBlock() instanceof ArcaneOrbBlock
                && p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= RANGE * RANGE;
    }

    public static Item ticketFor(String mode) {
        return switch (mode) {
            case MagicConfig.MODE_SELECT -> MagicRegistry.TICKET_SELECT.get();
            case "reroll" -> MagicRegistry.SCROLL.get();
            default -> MagicRegistry.TICKET.get();
        };
    }

    private static int count(ServerPlayer p, Item item) {
        return p.getInventory().countItem(item);
    }

    private static boolean take(ServerPlayer p, Item item, int n) {
        if (count(p, item) < n) return false;
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

    private static void give(ServerPlayer p, ItemStack s) {
        if (s.isEmpty()) return;
        if (!p.getInventory().add(s)) p.drop(s, false);
    }

    // ------------------------------------------------------------------ 창

    /** 룬 제단 잠금: 설정한 표식(이야기 진행)이 없으면 거울이 아무것도 비추지 않는다. */
    public static boolean locked(ServerPlayer p) {
        com.rtshop.story.StorySystems.ensure();
        String f = com.rtshop.story.StorySystems.altarLockFlag;
        if (f == null || f.isEmpty() || canEdit(p)) return false;
        return !com.rtshop.quest.QuestManager.hasFlag(p, f);
    }

    /** 제단을 연다: 언제나 메뉴로 (0번 칸에 장비를 올려놓을 수 있어야 하므로). 잠금·마을 제단 검사는 메뉴 제공자가 한다. */
    public static void open(ServerPlayer p, BlockPos pos) {
        net.minecraft.world.MenuProvider prov = p.level().getBlockState(pos).getMenuProvider(p.level(), pos);
        if (prov != null) p.openMenu(prov);
    }

    /** 제단 위에 올려 둔 장비 (제단 창이 열려 있지 않으면 빈 것). */
    public static ItemStack altarItem(ServerPlayer p) {
        return p.containerMenu instanceof MagicMenu m ? m.altarItem() : ItemStack.EMPTY;
    }

    /** 창 내용을 다시 보낸다 (제단 위 장비가 바뀌었을 때). */
    public static void refresh(ServerPlayer p) {
        if (SESSIONS.containsKey(p.getUUID())) send(p, MagicPacket.VIEW, view(p));
    }

    /** 메뉴로 열렸을 때 (Cinematic 장면 또는 그 대신 뜨는 창): 자리를 기억하고 창 내용을 먼저 보낸다. */
    public static void menuOpened(ServerPlayer p, BlockPos pos) {
        SESSIONS.put(p.getUUID(), pos.immutable());
        JsonObject o = view(p);
        o.addProperty("menu", true);
        o.addProperty("x", pos.getX());
        o.addProperty("y", pos.getY());
        o.addProperty("z", pos.getZ());
        send(p, MagicPacket.VIEW, o);
    }

    private static JsonArray chances(GachaConfig g, String cat, String mode) {
        JsonArray a = new JsonArray();
        for (double d : Gacha.gradeChances(g, cat, mode)) a.add(d);
        return a;
    }

    /** 창 내용: 티켓 수, 분야별 확률(첫 칸 = 실패)과 목록 수, 제단 위 장비와 재설정 확률. */
    public static JsonObject view(ServerPlayer p) {
        MagicConfig c = MagicConfig.get();
        GachaConfig g = c.engine();
        JsonObject o = new JsonObject();
        JsonObject tickets = new JsonObject();
        tickets.addProperty("select", count(p, MagicRegistry.TICKET_SELECT.get()));
        tickets.addProperty("random", count(p, MagicRegistry.TICKET.get()));
        tickets.addProperty("reroll", count(p, MagicRegistry.SCROLL.get()));
        o.add("tickets", tickets);
        JsonObject costs = new JsonObject();
        costs.addProperty("select", c.selectCost);
        costs.addProperty("random", c.randomCost);
        costs.addProperty("reroll", c.rerollCost);
        o.add("costs", costs);
        JsonObject cats = new JsonObject();
        int total = 0;
        for (String cat : MagicConfig.CATS) {
            JsonObject x = new JsonObject();
            x.addProperty("count", c.pool(cat).size());
            x.add("chances", chances(g, cat, MagicConfig.MODE_SELECT));
            x.addProperty("cost1", c.cost(MagicConfig.MODE_SELECT));
            total += c.pool(cat).size();
            cats.add(cat, x);
        }
        o.add("cats", cats);
        JsonObject rnd = new JsonObject();
        rnd.addProperty("count", total);
        rnd.add("chances", chances(g, MagicConfig.RANDOM, MagicConfig.MODE_RANDOM));
        rnd.addProperty("cost1", c.cost(MagicConfig.MODE_RANDOM));
        o.add("random", rnd);
        // 천장(확정) 은 3.49 에서 없앴다: 창 내용에도 싣지 않는다
        ItemStack held = altarItem(p);
        o.add("enchantChances", enchantChances(c, held));
        o.addProperty("held", held.isEmpty() ? "" : String.valueOf(ForgeRegistries.ITEMS.getKey(held.getItem())));
        o.addProperty("heldOk", rerollable(c, held));
        o.addProperty("canEdit", canEdit(p));
        o.add("gradeDefs", com.rtshop.gacha.GachaGrades.toJson()); // 화면이 등급의 이름·색·차례를 여기서 읽는다
        if (c.loadError != null) o.addProperty("error", c.loadError);
        return o;
    }

    // ------------------------------------------------------------------ 스킬북 뽑기

    /**
     * 뽑기 (한 번에 한 장). 뽑은 칸들을 돌려주고, 뽑을 수 없었으면 null. 실패 칸(MagicConfig.FAIL)이 나오면 티켓만 쓰이고 아무것도 받지 못한다.
     * needOrb=false 는 점검용.
     */
    public static List<GachaConfig.Entry> pull(ServerPlayer p, String mode, String cat, int n, boolean needOrb) {
        MagicConfig c = MagicConfig.get();
        if (needOrb && !near(p)) {
            fail(p, Component.translatable("rtshop.magic.need_orb"));
            return null;
        }
        boolean select = MagicConfig.MODE_SELECT.equals(mode);
        String pool = select ? cat : MagicConfig.RANDOM;
        if (n != 1 || (select && !List.of(MagicConfig.CATS).contains(cat))) return null;
        if (c.loadError != null) {
            fail(p, Component.translatable("rtshop.gacha.broken"));
            return null;
        }
        GachaConfig g = c.engine();
        double sum = 0;
        double[] ch = Gacha.gradeChances(g, pool, mode);
        for (int i = 1; i < ch.length; i++) sum += ch[i]; // 실패 칸만 있는 목록은 빈 목록이다
        if (sum <= 0) {
            fail(p, Component.translatable("rtshop.magic.empty"));
            return null;
        }
        Item t = ticketFor(mode);
        int cost = c.cost(mode);
        if (count(p, t) < cost) {
            fail(p, Component.translatable("rtshop.gacha.no_ticket", new ItemStack(t).getHoverName(), cost));
            return null;
        }
        int free = Gacha.freeSlotsAfterTaking(p, t, cost);
        if (free < n) {
            fail(p, Component.translatable("rtshop.gacha.inventory_full", n, free));
            return null;
        }
        take(p, t, cost);
        List<GachaConfig.Entry> got = new ArrayList<>(Gacha.pull(g, pool, mode, n, p.getRandom()));
        boolean pityHit = applyPity(p, c, g, pool, mode, got);
        JsonArray res = new JsonArray();
        for (GachaConfig.Entry e : got) {
            if (e == MagicConfig.FAIL) { // 실패: 아무것도 나오지 않는다
                JsonObject r = new JsonObject();
                r.addProperty("fail", true);
                r.addProperty("grade", "fail");
                res.add(r);
                continue;
            }
            String ecat = catOf(c, e);
            ItemStack s = Gacha.stackOf(e, "magic_" + ecat);
            JsonObject r = e.toJson();
            r.addProperty("cat", ecat);
            r.addProperty("name", s.getHoverName().getString());
            r.addProperty("skill", skillName(e));
            res.add(r);
            give(p, s);
            int gi = GachaConfig.gradeIndex(e.grade), legendary = GachaConfig.gradeIndex("legendary");
            if (legendary >= 0 && gi >= legendary) {
                boolean yamato = gi == GachaConfig.yamato();
                com.rtshop.notice.Notice.broadcast(p.server, Component.translatable(yamato ? "rtshop.magic.broadcast_yamato" : "rtshop.magic.broadcast",
                        p.getDisplayName(), Component.literal(skillName(e).isEmpty() ? s.getHoverName().getString() : skillName(e)))
                        .withStyle(yamato ? ChatFormatting.AQUA : ChatFormatting.LIGHT_PURPLE), false);
            }
        }
        JsonObject o = view(p);
        o.add("results", res);
        if (pityHit) o.addProperty("pityHit", true);
        BlockPos at = SESSIONS.get(p.getUUID());
        if (at != null) {
            o.addProperty("x", at.getX());
            o.addProperty("y", at.getY());
            o.addProperty("z", at.getZ());
        }
        o.addProperty("mode", mode);
        o.addProperty("resultCat", select ? cat : MagicConfig.RANDOM);
        send(p, MagicPacket.RESULT, o);
        com.rtshop.story.ScyllaClues.roll(p, "gacha", "", n, null);
        return got;
    }

    // ------------------------------------------------------------------ 천장

    private static String pityKey(MagicConfig c, String mode) {
        return c.pityShared ? "rtshop_magic_pity" : "rtshop_magic_pity_" + mode;
    }

    /** 확정 등급 없이 이어진 뽑기 횟수. */
    public static int pityOf(ServerPlayer p, MagicConfig c, String mode) {
        return p.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG).getInt(pityKey(c, mode));
    }

    public static void setPity(ServerPlayer p, MagicConfig c, String mode, int v) {
        CompoundTag root = p.getPersistentData();
        CompoundTag pers = root.getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
        pers.putInt(pityKey(c, mode), Math.max(0, v));
        root.put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG, pers);
    }

    /** (옛 천장) 지금은 아무것도 하지 않는다. */
    private static boolean applyPity(ServerPlayer p, MagicConfig c, GachaConfig g, String pool, String mode, List<GachaConfig.Entry> got) {
        return false; // 천장(확정)은 3.49 에서 없앴다: 세지도 바꾸지도 않는다
    }

    /** 이 칸이 들어 있는 분야. */
    static String catOf(MagicConfig c, GachaConfig.Entry e) {
        for (String cat : MagicConfig.CATS) if (c.pool(cat).contains(e)) return cat;
        return "passive";
    }

    /** 스킬북 칸의 스킬 이름 (Epic Fight 번역 키 skill.모드.이름). 클라이언트에서 번역한다. */
    public static String skillName(GachaConfig.Entry e) {
        try {
            CompoundTag t = net.minecraft.nbt.TagParser.parseTag(e.nbt);
            return t.contains("skill") ? t.getString("skill") : "";
        } catch (Exception ex) {
            return "";
        }
    }

    // ------------------------------------------------------------------ 인챈트 재설정

    /** 이 장비에 인챈트를 다시 붙일 수 있는가. */
    public static boolean rerollable(MagicConfig c, ItemStack s) {
        if (s.isEmpty() || s.getCount() != 1 || s.is(net.minecraft.world.item.Items.ENCHANTED_BOOK) || s.is(net.minecraft.world.item.Items.BOOK)) return false;
        Item it = s.getItem();
        boolean kind = (c.rerollKinds.contains("weapon") && (it instanceof SwordItem || it instanceof net.minecraft.world.item.AxeItem
                || s.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND).containsKey(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)))
                || (c.rerollKinds.contains("bow") && it instanceof BowItem)
                || (c.rerollKinds.contains("crossbow") && it instanceof CrossbowItem)
                || (c.rerollKinds.contains("trident") && it instanceof TridentItem)
                || (c.rerollKinds.contains("armor") && it instanceof ArmorItem)
                || (c.rerollKinds.contains("tool") && it instanceof DiggerItem);
        if (!kind) return false;
        for (MagicConfig.Ench e : c.enchants) {
            Enchantment en = e.enchantment();
            if (en != null && e.weight > 0 && en.canEnchant(s)) return true;
        }
        return false;
    }

    /** 이 장비에 붙을 수 있는 인챈트만 모은 등급별 목록. */
    private static List<List<MagicConfig.Ench>> enchByGrade(MagicConfig c, ItemStack s) {
        List<List<MagicConfig.Ench>> g = new ArrayList<>();
        for (int i = 0; i < GachaConfig.GRADES.length; i++) g.add(new ArrayList<>());
        for (MagicConfig.Ench e : c.enchants) {
            Enchantment en = e.enchantment();
            int gi = GachaConfig.gradeIndex(e.grade);
            if (en == null || gi < 0 || gi >= g.size() || e.weight <= 0 || (!s.isEmpty() && !en.canEnchant(s))) continue;
            if (!com.rtshop.gacha.GachaGrades.enabled(e.grade)) continue; // 꺼 둔 등급
            g.get(gi).add(e);
        }
        return g;
    }

    /** 등급별 가중치: 첫 칸은 실패(붙을 인챈트가 하나라도 있을 때만), 나머지는 이 장비에 붙을 것이 있는 등급만. */
    private static double[] enchantWeights(MagicConfig c, List<List<MagicConfig.Ench>> g) {
        double[] w = new double[g.size()];
        int fail = MagicConfig.failIndex();
        double real = 0;
        for (int i = 0; i < w.length; i++) {
            if (i == fail) continue;
            w[i] = g.get(i).isEmpty() || i >= c.enchantOdds.length ? 0 : c.enchantOdds[i];
            real += w[i];
        }
        if (fail >= 0 && fail < w.length && fail < c.enchantOdds.length) w[fail] = real > 0 ? c.enchantOdds[fail] : 0;
        return w;
    }

    public static JsonArray enchantChances(MagicConfig c, ItemStack s) {
        double[] w = enchantWeights(c, enchByGrade(c, s));
        double sum = 0;
        for (double v : w) sum += v;
        JsonArray a = new JsonArray();
        for (double v : w) a.add(sum <= 0 ? 0 : v / sum);
        return a;
    }

    /** 한 번 굴린다 (계산만). 이 장비에 붙을 것이 없으면 null, 실패 칸이 나오면 MagicConfig.FAIL_ENCH. */
    public static MagicConfig.Ench rollEnchant(MagicConfig c, ItemStack s, RandomSource rng) {
        List<List<MagicConfig.Ench>> g = enchByGrade(c, s);
        double[] w = enchantWeights(c, g);
        double sum = 0;
        for (double v : w) sum += v;
        if (sum <= 0) return null;
        double r = rng.nextDouble() * sum;
        int gi = -1;
        for (int i = 0; i < w.length; i++) {
            if (w[i] <= 0) continue;
            r -= w[i];
            gi = i;
            if (r < 0) break;
        }
        if (gi < 0) return null;
        if (gi == MagicConfig.failIndex()) return MagicConfig.FAIL_ENCH;
        List<MagicConfig.Ench> list = g.get(gi);
        int total = 0;
        for (MagicConfig.Ench e : list) total += e.weight;
        int k = rng.nextInt(Math.max(1, total));
        for (MagicConfig.Ench e : list) {
            k -= e.weight;
            if (k < 0) return e;
        }
        return list.get(list.size() - 1);
    }

    /** 제단 위에 올려 둔 장비의 인챈트를 다시 붙인다. */
    public static MagicConfig.Ench reroll(ServerPlayer p, boolean needOrb) {
        return reroll(p, altarItem(p), needOrb);
    }

    /**
     * 이 장비의 인챈트를 모두 지우고 하나를 새로 붙인다. 붙인 칸을 돌려준다.
     * 실패 칸이 나오면 주문서만 쓰이고 장비는 그대로이며 MagicConfig.FAIL_ENCH 를 돌려준다. 할 수 없었으면 null.
     */
    public static MagicConfig.Ench reroll(ServerPlayer p, ItemStack s, boolean needOrb) {
        MagicConfig c = MagicConfig.get();
        if (needOrb && !near(p)) {
            fail(p, Component.translatable("rtshop.magic.need_orb"));
            return null;
        }
        if (s.isEmpty()) {
            fail(p, Component.translatable("rtshop.magic.reroll_hold"));
            return null;
        }
        if (!rerollable(c, s)) {
            fail(p, Component.translatable("rtshop.magic.reroll_bad_item"));
            return null;
        }
        Item t = ticketFor("reroll");
        if (count(p, t) < c.rerollCost) {
            fail(p, Component.translatable("rtshop.gacha.no_ticket", new ItemStack(t).getHoverName(), c.rerollCost));
            return null;
        }
        MagicConfig.Ench e = rollEnchant(c, s, p.getRandom());
        if (e == null) {
            fail(p, Component.translatable("rtshop.magic.reroll_bad_item"));
            return null;
        }
        take(p, t, c.rerollCost);
        boolean failed = e == MagicConfig.FAIL_ENCH;
        Map<Enchantment, Integer> before = EnchantmentHelper.getEnchantments(s);
        if (!failed) {
            Map<Enchantment, Integer> now = new LinkedHashMap<>();
            now.put(e.enchantment(), e.level);
            EnchantmentHelper.setEnchantments(now, s);
            if (p.containerMenu instanceof MagicMenu m) m.broadcastChanges();
        }
        JsonObject o = view(p);
        JsonObject r = new JsonObject();
        if (failed) {
            r.addProperty("fail", true);
            r.addProperty("grade", "fail");
            r.addProperty("name", "");
        } else {
            r = e.toJson();
            r.addProperty("name", e.enchantment().getFullname(e.level).getString());
        }
        JsonArray old = new JsonArray();
        before.forEach((en, lv) -> old.add(en.getFullname(lv).getString()));
        r.add("before", old);
        r.addProperty("item", s.getHoverName().getString());
        o.add("reroll", r);
        o.addProperty("mode", "reroll");
        send(p, MagicPacket.RESULT, o);
        if (!failed && GachaConfig.gradeIndex("legendary") >= 0 && GachaConfig.gradeIndex(e.grade) >= GachaConfig.gradeIndex("legendary")) {
            com.rtshop.notice.Notice.broadcast(p.server, Component.translatable("rtshop.magic.broadcast_enchant", p.getDisplayName(),
                    e.enchantment().getFullname(e.level)).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        }
        return e;
    }

    // ------------------------------------------------------------------ 편집기 (OP)

    public static JsonObject editorJson() {
        MagicConfig c = MagicConfig.get();
        JsonObject o = c.toJson();
        o.add("gradeDefs", com.rtshop.gacha.GachaGrades.toJson());
        JsonArray skills = new JsonArray();
        for (MagicSkills.Skill s : MagicSkills.discover()) {
            String cat = MagicSkills.categoryOf(s);
            if (cat == null) continue;
            JsonObject x = new JsonObject();
            x.addProperty("id", s.id());
            x.addProperty("cat", cat);
            x.addProperty("grade", MagicSkills.gradeOf(s, cat));
            skills.add(x);
        }
        o.add("knownSkills", skills);
        JsonArray ench = new JsonArray();
        for (var en : ForgeRegistries.ENCHANTMENTS.getEntries()) {
            if (en.getValue().isCurse() || com.rtshop.rules.ItemPolicy.gachaBanned(en.getKey().location())) continue;
            JsonObject x = new JsonObject();
            x.addProperty("id", en.getKey().location().toString());
            x.addProperty("max", en.getValue().getMaxLevel());
            ench.add(x);
        }
        o.add("knownEnchants", ench);
        return o;
    }

    public static String save(String json, net.minecraft.server.MinecraftServer server) {
        try {
            MagicConfig c = MagicConfig.fromJson(JsonParser.parseString(json).getAsJsonObject());
            c.save();
            MagicConfig.set(c);
            return null;
        } catch (Exception e) {
            return String.valueOf(e.getMessage());
        }
    }

    public static void handleAction(ServerPlayer p, int action, String arg) {
        JsonObject a;
        try {
            a = arg == null || arg.isEmpty() ? new JsonObject() : JsonParser.parseString(arg).getAsJsonObject();
        } catch (Exception e) {
            a = new JsonObject();
        }
        switch (action) {
            case com.rtshop.net.MagicActionPacket.PULL -> pull(p, a.has("mode") ? a.get("mode").getAsString() : MagicConfig.MODE_RANDOM,
                    a.has("cat") ? a.get("cat").getAsString() : "", a.has("n") ? a.get("n").getAsInt() : 1, true);
            case com.rtshop.net.MagicActionPacket.REROLL -> reroll(p, true);
            case com.rtshop.net.MagicActionPacket.REFRESH -> send(p, MagicPacket.VIEW, view(p));
            case com.rtshop.net.MagicActionPacket.SHOP -> {
                // 티켓은 티켓 상인에게서만 산다: 제단에서는 상점을 열지 않는다
            }
            case com.rtshop.net.MagicActionPacket.EDIT -> {
                if (canEdit(p)) send(p, MagicPacket.EDITOR, editorJson());
            }
            case com.rtshop.net.MagicActionPacket.SAVE -> {
                if (!canEdit(p)) return;
                String err = save(a.has("json") ? a.get("json").getAsString() : "", p.server);
                if (err != null) {
                    fail(p, Component.translatable("rtshop.magic.save_failed", err));
                } else {
                    com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.magic.saved").withStyle(ChatFormatting.GREEN));
                    send(p, MagicPacket.EDITOR, editorJson());
                }
            }
            case com.rtshop.net.MagicActionPacket.AUTOFILL -> {
                if (!canEdit(p)) return;
                MagicConfig c = MagicConfig.get();
                int n = MagicSkills.autoFill(c);
                c.autoEnchants();
                c.save();
                com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.magic.autofilled", n, c.enchants.size()).withStyle(ChatFormatting.GREEN));
                send(p, MagicPacket.EDITOR, editorJson());
            }
            default -> {
            }
        }
    }

    /** 점검용: 연 구체 자리를 정한다. */
    public static void sessionForTest(ServerPlayer p, BlockPos pos) {
        SESSIONS.put(p.getUUID(), pos);
    }

    public static ResourceLocation id(Item i) {
        return ForgeRegistries.ITEMS.getKey(i);
    }
}

package com.rtshop.story;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import com.rtshop.item.ModItems;
import com.rtshop.quest.QuestManager;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetNbtFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 히든 보스 스킬라 추적: 우연히 얻는 단서를 모아 폭풍의 눈을 완성한다. 모두 config/rtshop/scylla.json 으로 바꿀 수 있다.
 * <ul>
 * <li>단서는 물건(rtshop:scylla_clue + 단서 ID)으로 얻고, 손에 들고 쓰면 단서 수첩(표식 scylla_clue_ID)에 기록된다. 퀘스트가 그 표식을 직접 줘도 된다.</li>
 * <li>단서를 들고 주민에게 말을 걸면 그 단서에 대한 소문을 들려준다 (결정적인 답은 주지 않는다). 들은 말은 수첩에 남는다.</li>
 * <li>웅크리고 단서를 쓰면 수첩을 본다. 필요한 만큼 모으면 연대기에 맞춰져 폭풍의 눈이 완성되고, 결계(seal_scylla)가 열린다.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class ScyllaClues {
    private ScyllaClues() {
    }

    public static final String TAG = "rtshop_clue";
    public static final String FLAG_PREFIX = "scylla_clue_";

    public static final class Clue {
        public String id = "", name = "", desc = "";
        /** fish | kill | loot | storm_height | quest */
        public String source = "quest";
        /** kill: 몹 ID, loot: 전리품 표 ID, fish: 바이옴 이름에 들어갈 글자(비우면 어디서나) */
        public String target = "";
        public double chance = 0.05;
        public boolean thunder;
        public int minY = 200;
        /** 주민 직업(ID 의 끝, 예: fisherman) 또는 "*" -> 들려줄 소문 */
        public final Map<String, List<String>> hints = new LinkedHashMap<>();
    }

    public boolean enabled = true;
    public int needed = 6;
    public String rewardItem = "cataclysm:storm_eye";
    public String grantFlag = "seal_scylla";
    /** 단서를 다 모아도 이 표식(메인 스토리의 끝)이 있어야 폭풍이 열린다. 그 표식을 주는 퀘스트가 없으면 따지지 않는다. */
    public String requireFlag = "story_done";
    public final Map<String, Clue> clues = new LinkedHashMap<>();

    private static ScyllaClues current;
    private static final Random RNG = new Random();

    public static synchronized ScyllaClues get() {
        if (current == null) current = load();
        return current;
    }

    public static synchronized void reload() {
        current = load();
    }

    private static Clue clue(String id, String name, String desc, String source, String target, double chance) {
        Clue c = new Clue();
        c.id = id;
        c.name = name;
        c.desc = desc;
        c.source = source;
        c.target = target;
        c.chance = chance;
        return c;
    }

    private static void hint(Clue c, String who, String... lines) {
        c.hints.put(who, new ArrayList<>(List.of(lines)));
    }

    static Map<String, Clue> defaults() {
        Map<String, Clue> m = new LinkedHashMap<>();
        Clue a = clue("wet_logbook", "젖은 항해일지 조각", "따뜻한 바다에서 낚아 올린, 소금에 절은 종이. \"여덟 번째 배는 폭풍 속으로…\"", "fish", "warm", 0.015);
        hint(a, "fisherman", "따뜻한 바다 쪽 배들은 폭풍이 오기 전날 꼭 노래를 듣는다더군. 누가 부르는지는 아무도 몰라.");
        hint(a, "cartographer", "그 항로… 내 지도엔 없는 바다로 이어지는군. 지워진 바다라면 모를까.");
        hint(a, "*", "바다 이야기는 어부한테 물어보게. 난 물이 싫어.");
        m.put(a.id, a);
        Clue b = clue("thunder_scale", "번개 맞은 비늘", "뇌우 치는 바다의 드라운드에게서 떨어진, 아직도 찌릿한 비늘.", "kill", "minecraft:drowned", 0.04);
        b.thunder = true;
        hint(b, "armorer", "이 비늘, 쇠보다 단단해. 그런데 이건 물고기 비늘이 아니야. 뱀… 아니, 그보다 큰 무언가의 것이지.");
        hint(b, "cleric", "번개를 품은 비늘이라… 옛날 폭풍을 다스리던 여인이 있었다는 이야기를 들은 적이 있어요.");
        hint(b, "*", "번쩍이는 비늘이네. 폭풍 치는 날 바다에 나가는 미친 사람이 또 있었군.");
        m.put(b.id, b);
        Clue c = clue("singing_shell", "노래하는 소라껍데기", "귀에 대면 바다가 아닌 여자의 노랫소리가 들린다.", "kill", "cataclysm:deepling_priest", 0.08);
        hint(c, "cleric", "이 노래… 성가예요. 일곱 봉인보다 오래된 성가. 첫 소절이 지워져 있네요.");
        hint(c, "librarian", "가사가 연대기의 문체와 같군요. 같은 사람이 썼거나, 같은 사람을 노래한 거예요.");
        hint(c, "*", "귀에 대 봤는데 아무 소리도 안 나는데? 너한테만 들리는 거 아니야?");
        m.put(c.id, c);
        Clue d = clue("torn_hymn", "찢긴 성가 악보", "따뜻한 바다 유적의 모래 속에서 나온 악보. 제목만 남았다: 「폭풍의 여제께」", "loot", "minecraft:archaeology/ocean_ruin_warm", 0.12);
        hint(d, "librarian", "「폭풍의 여제께」… 연대기에 여제라는 말은 한 번도 안 나와요. 일부러 뺀 거죠.");
        hint(d, "*", "악보는 못 읽어. 그래도 제목은 무섭게 생겼네.");
        m.put(d.id, d);
        Clue e = clue("sky_chart", "하늘의 해도", "폭풍이 칠 때 아주 높은 곳에서 바람에 날려 온 해도. 별 대신 번개가 그려져 있다.", "storm_height", "", 0.06);
        e.minY = 200;
        hint(e, "cartographer", "이건 바다 지도가 아니라 폭풍의 지도야. 번개가 떨어지는 자리를 이어 보면… 원이 되는군.");
        hint(e, "*", "하늘에도 지도가 있다고? 별 이상한 걸 다 주워 오는군.");
        m.put(e.id, e);
        Clue f = clue("seal_shard", "여제의 인장 조각", "항해일지의 숨은 페이지에 끼어 있던 인장 조각. 여덟 갈래의 번개 문양.", "quest", "", 1);
        hint(f, "*", "여덟 갈래 번개라… 일곱이 아니고 여덟이야? 세어 보니 정말 여덟이네.");
        m.put(f.id, f);
        return m;
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("scylla.json");
    }

    private static ScyllaClues load() {
        ScyllaClues s = new ScyllaClues();
        Path f = file();
        try {
            if (Files.exists(f)) {
                JsonObject o = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
                if (o.has("enabled")) s.enabled = o.get("enabled").getAsBoolean();
                if (o.has("needed")) s.needed = Math.max(1, o.get("needed").getAsInt());
                if (o.has("rewardItem")) s.rewardItem = o.get("rewardItem").getAsString();
                if (o.has("grantFlag")) s.grantFlag = o.get("grantFlag").getAsString();
                if (o.has("requireFlag")) s.requireFlag = o.get("requireFlag").getAsString();
                if (o.has("clues")) {
                    for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("clues").entrySet()) {
                        JsonObject x = e.getValue().getAsJsonObject();
                        Clue c = new Clue();
                        c.id = e.getKey();
                        c.name = str(x, "name", c.id);
                        c.desc = str(x, "desc", "");
                        c.source = str(x, "source", "quest");
                        c.target = str(x, "target", "");
                        if (x.has("chance")) c.chance = x.get("chance").getAsDouble();
                        if (x.has("thunder")) c.thunder = x.get("thunder").getAsBoolean();
                        if (x.has("minY")) c.minY = x.get("minY").getAsInt();
                        if (x.has("hints")) {
                            for (Map.Entry<String, JsonElement> h : x.getAsJsonObject("hints").entrySet()) {
                                List<String> lines = new ArrayList<>();
                                for (JsonElement l : h.getValue().getAsJsonArray()) lines.add(l.getAsString());
                                c.hints.put(h.getKey(), lines);
                            }
                        }
                        s.clues.put(c.id, c);
                    }
                }
            } else {
                s.clues.putAll(defaults());
                Files.createDirectories(f.getParent());
                Files.writeString(f, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(s.toJson()), StandardCharsets.UTF_8);
            }
        } catch (Exception ex) {
            RtShop.LOGGER.warn("[rtshop] scylla.json 을 읽지 못했습니다: {}", ex.toString());
            if (s.clues.isEmpty()) s.clues.putAll(defaults());
        }
        return s;
    }

    private static String str(JsonObject o, String k, String d) {
        return o.has(k) ? o.get(k).getAsString() : d;
    }

    JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("_help", "히든 보스 스킬라 추적. clues: 단서 (source: fish=낚시(target 은 바이옴 이름에 들어갈 글자), kill=몹 처치(target 몹 ID, thunder: 뇌우 때만), "
                + "loot=전리품 표(target 표 ID, 예: 수상한 모래), storm_height=뇌우 때 minY 이상 높이에서 1분마다 chance, quest=퀘스트가 scylla_clue_<ID> 표식을 준다). "
                + "hints: 주민 직업(fisherman, librarian…, *=그 밖) -> 단서를 들고 말을 걸면 들려줄 소문. needed 개를 모으면 rewardItem 과 grantFlag(결계)를 준다.");
        o.addProperty("enabled", enabled);
        o.addProperty("needed", needed);
        o.addProperty("rewardItem", rewardItem);
        o.addProperty("grantFlag", grantFlag);
        o.addProperty("requireFlag", requireFlag);
        JsonObject cs = new JsonObject();
        for (Clue c : clues.values()) {
            JsonObject x = new JsonObject();
            x.addProperty("name", c.name);
            x.addProperty("desc", c.desc);
            x.addProperty("source", c.source);
            x.addProperty("target", c.target);
            x.addProperty("chance", c.chance);
            if (c.thunder) x.addProperty("thunder", true);
            if (c.source.equals("storm_height")) x.addProperty("minY", c.minY);
            JsonObject h = new JsonObject();
            for (Map.Entry<String, List<String>> e : c.hints.entrySet()) {
                JsonArray a = new JsonArray();
                e.getValue().forEach(a::add);
                h.add(e.getKey(), a);
            }
            x.add("hints", h);
            cs.add(c.id, x);
        }
        o.add("clues", cs);
        return o;
    }

    // ------------------------------------------------------------------ 물건

    public static ItemStack stack(Clue c) {
        ItemStack s = new ItemStack(ModItems.SCYLLA_CLUE.get());
        CompoundTag t = s.getOrCreateTag();
        t.putString(TAG, c.id);
        s.setHoverName(Component.literal(c.name).withStyle(ChatFormatting.AQUA));
        return s;
    }

    public static Clue of(ItemStack s) {
        if (s.isEmpty() || !s.is(ModItems.SCYLLA_CLUE.get()) || !s.hasTag()) return null;
        return get().clues.get(s.getTag().getString(TAG));
    }

    public static boolean has(ServerPlayer p, String clueId) {
        return QuestManager.hasFlag(p, FLAG_PREFIX + clueId);
    }

    public static int count(ServerPlayer p) {
        int n = 0;
        for (String id : get().clues.keySet()) if (has(p, id)) n++;
        return n;
    }

    public static boolean grantsFlag(String flag) {
        ScyllaClues s = get();
        return s.enabled && flag.equals(s.grantFlag);
    }

    public static boolean grantsItem(String item) {
        ScyllaClues s = get();
        return s.enabled && item.equals(s.rewardItem);
    }

    /** 단서를 수첩에 적는다. 새로 적었으면 true. 다 모이면 폭풍의 눈. */
    public static boolean record(ServerPlayer p, Clue c) {
        if (has(p, c.id)) return false;
        QuestManager.setFlag(p, FLAG_PREFIX + c.id, true);
        ScyllaClues s = get();
        int n = count(p);
        com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.scylla.recorded", c.name, n, s.needed).withStyle(ChatFormatting.AQUA));
        p.level().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1F, 0.8F);
        checkComplete(p);
        return true;
    }

    public static void checkComplete(ServerPlayer p) {
        ScyllaClues s = get();
        if (!s.enabled || count(p) < s.needed || QuestManager.hasFlag(p, s.grantFlag)) return;
        if (!s.requireFlag.isEmpty() && !QuestManager.hasFlag(p, s.requireFlag) && StorySeals.grantedByAnyQuest(s.requireFlag)) {
            // 단서는 다 모였지만 폭풍은 연대기의 마지막 장이 쓰인 뒤에야 열린다
            if (!QuestManager.hasFlag(p, "scylla_waiting")) {
                QuestManager.setFlag(p, "scylla_waiting", true);
                com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.scylla.waiting").withStyle(ChatFormatting.DARK_AQUA));
            }
            return;
        }
        QuestManager.setFlag(p, s.grantFlag, true);
        ResourceLocation rl = ResourceLocation.tryParse(s.rewardItem);
        if (rl != null && ForgeRegistries.ITEMS.containsKey(rl)) {
            ItemStack eye = new ItemStack(ForgeRegistries.ITEMS.getValue(rl));
            if (!p.getInventory().add(eye)) p.drop(eye, false);
        }
        JsonObject v = new JsonObject();
        v.addProperty("kind", "vision");
        v.addProperty("speaker", "폭풍의 여제");
        v.addProperty("text", "일곱은 내 이름을 지웠다.\n너는… 그 이름을 다시 쓰러 왔느냐.");
        v.addProperty("color", "7FD4FF");
        v.addProperty("sound", "minecraft:entity.lightning_bolt.thunder");
        v.addProperty("shake", 2);
        v.addProperty("seconds", 7);
        v.addProperty("closeScreen", true);
        BossIntros.send(p, v);
        com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.scylla.complete").withStyle(ChatFormatting.GOLD));
    }

    /** 수첩: 모은 단서와 들은 소문 (채팅으로). */
    public static void showNotebook(ServerPlayer p) {
        ScyllaClues s = get();
        p.sendSystemMessage(Component.translatable("rtshop.scylla.notebook", count(p), s.needed).withStyle(ChatFormatting.GOLD));
        CompoundTag heard = p.getPersistentData().getCompound("rtshop_clue_heard");
        for (Clue c : s.clues.values()) {
            if (has(p, c.id)) {
                p.sendSystemMessage(Component.literal(" ✦ " + c.name).withStyle(ChatFormatting.AQUA));
                if (!c.desc.isEmpty()) p.sendSystemMessage(Component.literal("    " + c.desc).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                String h = heard.getString(c.id);
                if (!h.isEmpty()) p.sendSystemMessage(Component.literal("    \"" + h + "\"").withStyle(ChatFormatting.DARK_AQUA));
            } else {
                p.sendSystemMessage(Component.literal(" ✧ ???").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }

    /** 주민이 들려줄 소문. */
    public static String hintFor(Clue c, String profession) {
        List<String> l = c.hints.get(profession);
        if (l == null || l.isEmpty()) l = c.hints.get("*");
        if (l == null || l.isEmpty()) return null;
        return l.get(RNG.nextInt(l.size()));
    }

    // ------------------------------------------------------------------ 얻는 곳

    public static boolean modOk(String id) {
        int i = id.indexOf(':');
        String ns = i < 0 ? "minecraft" : id.substring(0, i);
        return ns.equals("minecraft") || ModList.get().isLoaded(ns);
    }

    public static void give(ServerPlayer p, Clue c, net.minecraft.world.phys.Vec3 at) {
        ItemStack st = stack(c);
        if (at != null) {
            p.level().addFreshEntity(new ItemEntity(p.level(), at.x, at.y, at.z, st));
        } else if (!p.getInventory().add(st)) {
            p.drop(st, false);
        }
        com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.scylla.found", c.name).withStyle(ChatFormatting.AQUA), true);
    }

    @SubscribeEvent
    public static void onFish(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p instanceof FakePlayer) return;
        ScyllaClues s = get();
        if (!s.enabled) return;
        String biome = p.level().getBiome(p.blockPosition()).unwrapKey().map(k -> k.location().getPath()).orElse("");
        for (Clue c : s.clues.values()) {
            if (!c.source.equals("fish") || has(p, c.id) || !biome.contains(c.target)) continue;
            if (c.thunder && !p.level().isThundering()) continue;
            if (RNG.nextDouble() < c.chance) give(p, c, null);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer p) || p instanceof FakePlayer) return;
        ScyllaClues s = get();
        if (!s.enabled) return;
        LivingEntity dead = event.getEntity();
        String id = BossIntros.id(dead);
        for (Clue c : s.clues.values()) {
            if (!c.source.equals("kill") || !c.target.equals(id) || has(p, c.id)) continue;
            if (c.thunder && !dead.level().isThundering()) continue;
            if (RNG.nextDouble() < c.chance) {
                event.getDrops().add(new ItemEntity(dead.level(), dead.getX(), dead.getY(), dead.getZ(), stack(c)));
            }
        }
    }

    @SubscribeEvent
    public static void onLootLoad(LootTableLoadEvent event) {
        ScyllaClues s = get();
        if (!s.enabled) return;
        for (Clue c : s.clues.values()) {
            if (!c.source.equals("loot") || !event.getName().toString().equals(c.target)) continue;
            CompoundTag t = stack(c).getTag();
            event.getTable().addPool(LootPool.lootPool().name("rtshop_clue_" + c.id)
                    .add(LootItem.lootTableItem(ModItems.SCYLLA_CLUE.get()).apply(SetNbtFunction.setTag(t)))
                    .when(LootItemRandomChanceCondition.randomChance((float) c.chance)).build());
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 1200 != 600) return;
        ScyllaClues s = get();
        if (!s.enabled) return;
        for (ServerPlayer p : event.getServer().getPlayerList().getPlayers()) {
            ServerLevel l = p.serverLevel();
            if (!l.isThundering() || p.isSpectator()) continue;
            for (Clue c : s.clues.values()) {
                if (c.source.equals("storm_height") && !has(p, c.id) && p.getY() >= c.minY && RNG.nextDouble() < c.chance) give(p, c, null);
            }
        }
    }

    /** 단서를 들고 주민에게 말을 건다 → 소문. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onTalk(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !(event.getTarget() instanceof Villager v)) return;
        if (v.getTags().contains("rtshop_npc") || com.rtshop.shop.NpcGuard.isNpc(v)) return;
        Clue c = of(p.getItemInHand(event.getHand()));
        if (c == null) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        ResourceLocation prof = ForgeRegistries.VILLAGER_PROFESSIONS.getKey(v.getVillagerData().getProfession());
        String line = hintFor(c, prof == null ? "none" : prof.getPath());
        if (line == null) return;
        com.rtshop.notice.Notice.send(p, Component.literal("[" + v.getDisplayName().getString() + "] ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(line).withStyle(ChatFormatting.WHITE)));
        CompoundTag heard = p.getPersistentData().getCompound("rtshop_clue_heard");
        heard.putString(c.id, line);
        p.getPersistentData().put("rtshop_clue_heard", heard);
        v.playSound(SoundEvents.VILLAGER_AMBIENT, 1F, 1F);
    }

    /** 단서 아이템을 쓸 때 (ClueItem 에서 부른다). */
    public static InteractionResult use(ServerPlayer p, InteractionHand hand) {
        ItemStack st = p.getItemInHand(hand);
        Clue c = of(st);
        if (p.isShiftKeyDown() || c == null) {
            showNotebook(p);
            return InteractionResult.SUCCESS;
        }
        if (record(p, c)) {
            st.shrink(1);
        } else {
            showNotebook(p);
        }
        return InteractionResult.SUCCESS;
    }
}

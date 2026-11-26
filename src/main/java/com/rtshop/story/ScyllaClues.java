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
import net.minecraft.world.entity.player.Player;
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
        /** gacha(뽑기) | boss(이야기 보스) | wild(야생의 상자) | fish(낚시) | quest(이야기 선택지) | kill | loot | storm_height */
        public String source = "quest";
        /** kill: 몹 ID, loot: 전리품 표 ID, fish: 바이옴 이름에 들어갈 글자(비우면 어디서나) */
        public String target = "";
        public double chance = 0.05;
        public boolean thunder;
        public int minY = 200;
        /** 천장: 이만큼 시도하고도 못 얻었으면 다음 시도에서 반드시 얻는다 (0 = 없음). 시도할수록 확률도 조금씩 오른다. */
        public int pity = 0;
        /** 주민 직업(ID 의 끝, 예: fisherman) 또는 "*" -> 들려줄 소문 */
        public final Map<String, List<String>> hints = new LinkedHashMap<>();
    }

    public boolean enabled = true;
    public int needed = 5;
    public int version = 2;
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
        // 다섯 단서 = 폭풍의 여제가 쓴 「마중의 항해일지」의 흩어진 다섯 조각. 얻는 때는 운과 선택에 따라 다르지만, 천장이 있어 결국 모두 얻는다.
        Clue a = clue("storm_coin", "여덟 갈래 번개가 찍힌 품삯", "행운 상인의 상품 더미에 섞여 있던 낡은 은화. 앞면에 여덟 갈래 번개, 뒷면에 한 줄: \"마중 나간 날의 품삯. 돌아와서 치름.\"", "gacha", "", 0.002);
        a.pity = 300;
        hint(a, "*", "그런 돈은 처음 보는데. 품삯을 '돌아와서 치른다'니, 못 돌아온 배의 돈이겠구먼.");
        hint(a, "cleric", "여덟 갈래라. 일곱이 아니고? …세어 보니 정말 여덟이군.");
        m.put(a.id, a);
        Clue b = clue("thunder_scale", "번개 맞은 비늘", "잠든 수호자의 재 속에서 나온 비늘. 아직도 찌릿하다. 수호자의 것이 아니다. 누군가 그 곁을 다녀갔다.", "boss", "", 0.04);
        b.pity = 14;
        hint(b, "*", "수호자 곁에서 나왔다고? 그럼 그 폭풍이 봉인마다 들렀다는 얘기 아닌가. 뭘 찾느라고.");
        hint(b, "fisherman", "이 비늘… 물고기 것이 아니야. 훨씬 큰 것. 폭풍 속에서 딱 한 번 본 적 있네. 번개인 줄 알았지.");
        m.put(b.id, b);
        Clue c = clue("torn_hymn", "찢긴 뱃노래", "어느 버려진 상자 바닥에 깔려 있던 악보. 제목만 온전하다: 「마중 가는 노래」. 가사에 사람 이름이 들어갈 자리만 전부 비어 있다.", "wild", "", 0.006);
        c.pity = 150;
        hint(c, "*", "이 가락, 들어 본 것 같은데… 폭풍 치는 밤이면 바다 쪽에서 들리는 그거 아닌가?");
        hint(c, "librarian", "이름 자리만 비어 있네요. 지운 게 아니에요. 잉크가 닿은 적이 없어요. 부르려던 이름을 잊은 사람이 적은 악보예요.");
        m.put(c.id, c);
        Clue d = clue("wet_logbook", "젖은 항해일지 조각", "바다에서 낚아 올린, 소금에 절은 종이. \"여덟 번째 함대, 폭풍의 눈 안에 닻을 내림. 제독께서 이르시길, 누가 오는지는 잊었으나 맞을 채비는 풀지 말라 하심.\"", "fish", "ocean", 0.01);
        d.pity = 180;
        hint(d, "fisherman", "폭풍의 눈 안에 닻을 내린다고? 미친 소리… 라고 하고 싶은데, 눈 안은 잔잔하긴 하지. 거기 천 년 있을 수 있다면 말이야.");
        hint(d, "*", "여덟 번째 함대? 왕국은 일곱이었다던데. 여덟째 나라의 배라는 건가.");
        m.put(d.id, d);
        Clue f = clue("seal_shard", "제독의 인장 조각", "여덟 갈래 번개 문양의 인장 조각. 깨진 면에 글자 반 토막: \"…중하는 이\".", "quest", "", 1);
        hint(f, "*", "'…중하는 이'? 앞 글자가 깨졌네. 존중? 마중? 인장에 그런 말을 새기는 사람도 있나.");
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
                if (!o.has("version") || o.get("version").getAsInt() < 2) {
                    // 3.36: 단서가 다섯(뽑기·보스·야생·낚시·선택지)으로 바뀌었다. 예전 파일은 옆에 남기고 새 기본값으로 다시 쓴다
                    Files.copy(f, f.resolveSibling("scylla_v1_backup.json"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    s.clues.putAll(defaults());
                    if (o.has("enabled")) s.enabled = o.get("enabled").getAsBoolean();
                    Files.writeString(f, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(s.toJson()), StandardCharsets.UTF_8);
                    return s;
                }
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
                        if (x.has("pity")) c.pity = Math.max(0, x.get("pity").getAsInt());
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
        o.addProperty("version", 2);
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
            x.addProperty("pity", c.pity);
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

    /** 그 플레이어의 이야기 언어로 이름을 붙인 단서. */
    public static ItemStack stack(Clue c, ServerPlayer p) {
        ItemStack s = stack(c);
        s.setHoverName(Component.literal(StoryText.t(p, c.name)).withStyle(ChatFormatting.AQUA));
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
        com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.scylla.recorded", StoryText.t(p, c.name), n, s.needed).withStyle(ChatFormatting.AQUA));
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
        v.addProperty("text", "배가 온다.\n…이번에는, 네가 그 사람이냐.");
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
                p.sendSystemMessage(Component.literal(" ✦ " + StoryText.t(p, c.name)).withStyle(ChatFormatting.AQUA));
                if (!c.desc.isEmpty()) p.sendSystemMessage(Component.literal("    " + StoryText.t(p, c.desc)).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                String h = heard.getString(c.id);
                if (!h.isEmpty()) p.sendSystemMessage(Component.literal("    \"" + StoryText.t(p, h) + "\"").withStyle(ChatFormatting.DARK_AQUA));
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
        ItemStack st = stack(c, p);
        if (at != null) {
            p.level().addFreshEntity(new ItemEntity(p.level(), at.x, at.y, at.z, st));
        } else if (!p.getInventory().add(st)) {
            p.drop(st, false);
        }
        com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.scylla.found", StoryText.t(p, c.name)).withStyle(ChatFormatting.AQUA), true);
    }

    private static final String TRIES = "rtshop_scylla_tries";

    public static int tries(ServerPlayer p, String clueId) {
        return p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound(TRIES).getInt(clueId);
    }

    private static void setTries(ServerPlayer p, String clueId, int n) {
        CompoundTag pers = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag t = pers.getCompound(TRIES);
        t.putInt(clueId, n);
        pers.put(TRIES, t);
        p.getPersistentData().put(Player.PERSISTED_NBT_TAG, pers);
    }

    /** 가방에 이 단서 물건을 이미 갖고 있는가 (아직 수첩에 안 적었어도 또 주지는 않는다). */
    private static boolean carrying(ServerPlayer p, String clueId) {
        for (ItemStack st : p.getInventory().items) {
            Clue c = of(st);
            if (c != null && c.id.equals(clueId)) return true;
        }
        return false;
    }

    /**
     * 운에 달린 단서를 굴린다 (뽑기 한 번, 보스 한 마리, 상자 하나, 낚시 한 번이 시도 한 번). 시도할수록 확률이 오르고,
     * 천장에 닿으면 반드시 나온다. 그래서 얻는 때는 사람마다 달라도 결국 모두 얻는다. 얻었으면 true.
     */
    public static boolean roll(ServerPlayer p, String source, String where, int attempts, net.minecraft.world.phys.Vec3 at) {
        ScyllaClues s = get();
        if (!s.enabled || p instanceof FakePlayer && !testRolls || attempts <= 0) return false;
        boolean any = false;
        for (Clue c : s.clues.values()) {
            if (!c.source.equals(source) || has(p, c.id) || carrying(p, c.id) || !modOk(c.target.contains(":") ? c.target : "")) continue;
            if (source.equals("fish") && !c.target.isEmpty() && !where.contains(c.target)) continue;
            if (c.thunder && !p.level().isThundering()) continue;
            int t = tries(p, c.id);
            boolean hit = false;
            for (int i = 0; i < attempts && !hit; i++) {
                t++;
                double chance = c.pity > 0 ? c.chance * (1.0 + 3.0 * t / c.pity) : c.chance;
                hit = (c.pity > 0 && t >= c.pity) || RNG.nextDouble() < chance;
            }
            if (hit) {
                setTries(p, c.id, 0);
                give(p, c, at);
                any = true;
            } else {
                setTries(p, c.id, t);
            }
        }
        return any;
    }

    /** 점검용: 가짜 플레이어도 굴릴 수 있게 한다. */
    public static boolean testRolls = false;

    @SubscribeEvent
    public static void onFish(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p instanceof FakePlayer) return;
        String biome = p.level().getBiome(p.blockPosition()).unwrapKey().map(k -> k.location().getPath()).orElse("");
        roll(p, "fish", biome, 1, null);
    }

    /** 야생의 상자: 아직 아무도 열지 않은(전리품이 정해지지 않은) 상자를 여는 것이 시도 한 번. */
    @SubscribeEvent
    public static void onOpenChest(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p instanceof FakePlayer || event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(p.level().getBlockEntity(event.getPos()) instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity be)) return;
        if (!be.saveWithoutMetadata().contains("LootTable")) return;
        roll(p, "wild", "", 1, null);
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
                .append(Component.literal(StoryText.t(p, line)).withStyle(ChatFormatting.WHITE)));
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

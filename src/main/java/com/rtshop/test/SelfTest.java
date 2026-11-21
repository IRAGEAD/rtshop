package com.rtshop.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.rtshop.RtShop;
import com.rtshop.economy.Economy;
import com.rtshop.economy.EconomyConfig;
import com.rtshop.economy.EconomyData;
import com.rtshop.economy.EconomyGuards;
import com.rtshop.farm.CropData;
import com.rtshop.farm.FarmConfig;
import com.rtshop.farm.FarmEvents;
import com.rtshop.farm.SeedRules;
import com.rtshop.item.CheckItem;
import com.rtshop.item.DeedItem;
import com.rtshop.item.ModItems;
import com.rtshop.land.LandConfig;
import com.rtshop.land.LandData;
import com.rtshop.land.LandManager;
import com.rtshop.land.PlotManager;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import com.rtshop.quest.QuestData;
import com.rtshop.quest.QuestDef;
import com.rtshop.quest.QuestEditor;
import com.rtshop.quest.QuestManager;
import com.rtshop.quest.QuestView;
import com.rtshop.shop.ShopData;
import com.rtshop.shop.ShopLoader;
import com.rtshop.shop.ShopServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * 서버 자체 점검. JVM 옵션 -Drtshop.selftest=true 로 켰을 때만 동작한다 (일반 서버에서는 아무 일도 하지 않음).
 * 가짜 플레이어로 경제·상점·농사·토지·PIN 의 핵심 규칙을 실제로 실행해 보고, 결과를 rtshop-selftest.txt 에 적은 뒤 서버를 끈다.
 * 빌드할 때마다 자동으로 돌려서, 게임을 직접 켜 보지 않고도 기본 동작이 깨지지 않았는지 확인하는 용도.
 */
public final class SelfTest {
    private SelfTest() {
    }

    private static final List<String> LINES = new ArrayList<>();
    private static int passed;
    private static int failed;

    public static boolean enabled() {
        return Boolean.getBoolean("rtshop.selftest");
    }

    public static void onServerStarted(MinecraftServer server) {
        if (!enabled()) {
            return;
        }
        try {
            run(server);
        } catch (Throwable t) {
            RtShop.LOGGER.error("[rtshop-selftest] crashed", t);
        }
        if (!Boolean.getBoolean("rtshop.selftest.keepRunning")) {
            RtShop.LOGGER.info("[rtshop-selftest] stopping the server");
            server.halt(false);
        }
    }

    // ------------------------------------------------------------------ 도우미

    private static void section(String name) {
        LINES.add("");
        LINES.add("== " + name);
    }

    private static void check(String name, boolean ok) {
        check(name, ok, null);
    }

    private static void check(String name, boolean ok, Object detail) {
        if (ok) {
            passed++;
        } else {
            failed++;
        }
        String line = (ok ? "PASS  " : "FAIL  ") + name + (detail == null ? "" : "  [" + detail + "]");
        LINES.add(line);
        if (ok) {
            RtShop.LOGGER.info("[rtshop-selftest] {}", line);
        } else {
            RtShop.LOGGER.error("[rtshop-selftest] {}", line);
        }
    }

    private static void info(String text) {
        LINES.add("      " + text);
        RtShop.LOGGER.info("[rtshop-selftest] {}", text);
    }

    private static FakePlayer fake(ServerLevel level, String name) {
        UUID id = UUID.nameUUIDFromBytes(("rtshop:" + name).getBytes(StandardCharsets.UTF_8));
        return FakePlayerFactory.get(level, new GameProfile(id, name));
    }

    private static long count(ServerPlayer p, Predicate<ItemStack> f) {
        long n = 0;
        for (ItemStack s : p.getInventory().items) {
            if (!s.isEmpty() && f.test(s)) {
                n += s.getCount();
            }
        }
        return n;
    }

    private static ItemStack first(ServerPlayer p, Predicate<ItemStack> f) {
        for (ItemStack s : p.getInventory().items) {
            if (!s.isEmpty() && f.test(s)) {
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void clear(ServerPlayer p) {
        p.getInventory().clearContent();
    }

    private static int findEntry(ShopData d, Predicate<ShopData.Entry> f) {
        for (int i = 0; i < d.entries.size(); i++) {
            if (f.test(d.entries.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private static BlockHitResult hitTop(BlockPos pos) {
        return new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false);
    }

    private static List<ItemStack> takeDrops(ServerLevel level, BlockPos pos) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(4))) {
            out.add(e.getItem().copy());
            e.discard();
        }
        return out;
    }

    private static void crash(String section, Throwable t) {
        failed++;
        LINES.add("FAIL  " + section + " threw " + t);
        RtShop.LOGGER.error("[rtshop-selftest] {} threw", section, t);
    }

    // ------------------------------------------------------------------ 실행

    public static synchronized boolean run(MinecraftServer server) {
        LINES.clear();
        passed = 0;
        failed = 0;
        ServerLevel level = server.overworld();
        FakePlayer a = fake(level, "rtshop_test_a");
        FakePlayer b = fake(level, "rtshop_test_b");
        BlockPos spawn = level.getSharedSpawnPos();
        int gx = Math.floorDiv(spawn.getX(), 32) * 32;
        int gz = Math.floorDiv(spawn.getZ(), 32) * 32;
        int y = Math.min(level.getMaxBuildHeight() - 60, 250);
        level.getChunkAt(new BlockPos(gx, y, gz));
        level.getChunkAt(new BlockPos(gx + 31, y, gz + 31));

        LINES.add("RT Shop self-test");
        try {
            economy(server, a);
        } catch (Throwable t) {
            crash("economy", t);
        }
        try {
            shops(a);
        } catch (Throwable t) {
            crash("shops", t);
        }
        try {
            farming(level, a, new BlockPos(gx + 20, y, gz + 20));
        } catch (Throwable t) {
            crash("farming", t);
        }
        try {
            otherMods(level, a, new BlockPos(gx + 24, y, gz + 24));
        } catch (Throwable t) {
            crash("other mods", t);
        }
        try {
            land(server, level, a, b, gx, y, gz);
        } catch (Throwable t) {
            crash("land", t);
        }
        try {
            plots(server, level, a, b, gx + 960, y, gz + 128);
        } catch (Throwable t) {
            crash("plots", t);
        }
        try {
            pricing();
        } catch (Throwable t) {
            crash("pricing", t);
        }
        try {
            guards(server, level);
        } catch (Throwable t) {
            crash("guards", t);
        }
        try {
            upgrade(server);
        } catch (Throwable t) {
            crash("upgrade", t);
        }
        try {
            quests(server, level, a);
        } catch (Throwable t) {
            crash("quests", t);
        }
        try {
            questEditor(server, level, a);
        } catch (Throwable t) {
            crash("quest editor", t);
        }
        try {
            jobs(server, level, a);
        } catch (Throwable t) {
            crash("jobs", t);
        }
        try {
            jobQuests(server, level, a);
        } catch (Throwable t) {
            crash("job quests", t);
        }
        try {
            attendance(server, level, a);
        } catch (Throwable t) {
            crash("attendance", t);
        }
        try {
            npcGuard(level, a);
        } catch (Throwable t) {
            crash("npc guard", t);
        }
        try {
            gacha(server, level, a);
        } catch (Throwable t) {
            crash("gacha", t);
        }
        try {
            cleanup(server, level, a, b, gx, y, gz);
        } catch (Throwable t) {
            RtShop.LOGGER.warn("[rtshop-selftest] cleanup failed", t);
        }
        try {
            PackDiag.write(server);
        } catch (Throwable t) {
            RtShop.LOGGER.warn("[rtshop-selftest] diag failed", t);
        }
        try {
            int n = ShopLoader.export(ShopServer.loaded());
            info("exported " + n + " price lines to config/rtshop/export");
        } catch (Throwable t) {
            RtShop.LOGGER.warn("[rtshop-selftest] export failed", t);
        }

        LINES.add("");
        String result = failed == 0 ? "RESULT: ALL PASS (" + passed + " checks)" : "RESULT: FAILED (" + failed + " of " + (passed + failed) + " checks)";
        LINES.add(result);
        RtShop.LOGGER.info("[rtshop-selftest] {}", result);
        try {
            Files.write(FMLPaths.GAMEDIR.get().resolve("rtshop-selftest.txt"), LINES, StandardCharsets.UTF_8);
        } catch (Throwable t) {
            RtShop.LOGGER.error("[rtshop-selftest] could not write the result file", t);
        }
        return failed == 0;
    }


    // ------------------------------------------------------------------ 퀘스트

    private static QuestDef tempQuest(String json) {
        return QuestDef.parse(JsonParser.parseString(json).getAsJsonObject());
    }

    private static boolean offered(ServerPlayer p, String npc, String id) {
        for (QuestView v : QuestManager.viewsFor(p, npc)) {
            if (v.id().equals(id) && v.state() == QuestView.AVAILABLE) {
                return true;
            }
        }
        return false;
    }

    private static void quests(MinecraftServer server, ServerLevel level, FakePlayer a) throws Exception {
        section("quests");
        QuestManager.Loaded l = QuestManager.reload();
        check("quest files load without problems", l.problems.isEmpty(), l.problems);
        check("the tutorial chain and the guide NPC are installed",
                l.quests.containsKey("tutorial_1") && l.quests.containsKey("tutorial_2")
                        && l.quests.containsKey("tutorial_3") && l.quests.containsKey("tutorial_4")
                        && l.npcs.containsKey("guide"));
        BlockPos npcAt = level.getSharedSpawnPos().above(4);
        level.getChunkAt(npcAt);
        check("a quest NPC can be spawned from its definition",
                ShopServer.spawnNpc(level, Vec3.atBottomCenterOf(npcAt), 0, "quest_guide") != null);
        for (Villager v : level.getEntitiesOfClass(Villager.class, new AABB(npcAt).inflate(3))) {
            if (QuestManager.isQuestNpc(v)) {
                check("the quest NPC is recognised as an NPC (no trading, no damage)", ShopServer.isNpc(v) && "quest_guide".equals(QuestManager.npcKey(v)));
                v.discard();
            }
        }

        QuestManager.reset(a);
        Economy.setWallet(a, 0);
        clear(a);
        List<QuestView> start = QuestManager.viewsFor(a, "quest_guide");
        check("only the first tutorial quest is offered at the start",
                start.size() == 1 && start.get(0).id().equals("tutorial_1") && start.get(0).state() == QuestView.AVAILABLE);
        check("a quest cannot be taken from the wrong NPC", !QuestManager.accept(a, "tutorial_1", "bank"));
        check("a quest can be accepted", QuestManager.accept(a, "tutorial_1", "quest_guide") && QuestManager.isActive(a, "tutorial_1"));
        check("it cannot be accepted twice", !QuestManager.accept(a, "tutorial_1", "quest_guide"));
        check("a quest cannot be turned in before its goals", !QuestManager.turnIn(a, "tutorial_1", "quest_guide", -1));
        QuestManager.onTalk(a, "shop_farmer");
        check("talking to some other NPC does not count", !QuestManager.turnIn(a, "tutorial_1", "quest_guide", -1));
        QuestManager.onTalk(a, "bank");
        check("a quest must be turned in at the right NPC", !QuestManager.turnIn(a, "tutorial_1", "bank", -1));
        check("turning in pays the reward", QuestManager.turnIn(a, "tutorial_1", "quest_guide", -1)
                && Economy.wallet(a) == 1000 && QuestManager.isCompleted(a, "tutorial_1"), Economy.wallet(a));
        check("a finished quest cannot be taken again", !QuestManager.accept(a, "tutorial_1", "quest_guide"));
        check("finishing step 1 unlocks step 2 and not step 3", offered(a, "quest_guide", "tutorial_2") && !offered(a, "quest_guide", "tutorial_3"));

        check("delivery quest is accepted", QuestManager.accept(a, "tutorial_2", "quest_guide"));
        a.getInventory().add(new ItemStack(Items.WHEAT, 7));
        check("7 of 8 items are not enough", !QuestManager.turnIn(a, "tutorial_2", "quest_guide", -1));
        a.getInventory().add(new ItemStack(Items.WHEAT, 1));
        check("8 items complete the delivery, are taken, and pay the reward",
                QuestManager.turnIn(a, "tutorial_2", "quest_guide", -1) && count(a, s -> s.is(Items.WHEAT)) == 0
                        && Economy.wallet(a) == 1800, Economy.wallet(a));

        check("kill quest is accepted", QuestManager.accept(a, "tutorial_3", "quest_guide"));
        QuestManager.onKill(a, EntityType.COW.create(level));
        for (int i = 0; i < 4; i++) {
            QuestManager.onKill(a, EntityType.ZOMBIE.create(level));
        }
        check("a cow does not count and 4 of 5 monsters are not enough", !QuestManager.turnIn(a, "tutorial_3", "quest_guide", -1));
        QuestManager.onKill(a, EntityType.SKELETON.create(level));
        check("5 monsters complete the hunt and pay gold and items",
                QuestManager.turnIn(a, "tutorial_3", "quest_guide", -1) && Economy.wallet(a) == 3300
                        && count(a, s -> s.is(Items.BREAD)) == 5, Economy.wallet(a));

        check("last tutorial quest is accepted", QuestManager.accept(a, "tutorial_4", "quest_guide"));
        QuestManager.onTalk(a, "shop_land");
        check("a quest with choices cannot be finished without choosing", !QuestManager.turnIn(a, "tutorial_4", "quest_guide", -1));
        check("an invalid choice is refused", !QuestManager.turnIn(a, "tutorial_4", "quest_guide", 5));
        check("choosing finishes it and sets the chosen flags only",
                QuestManager.turnIn(a, "tutorial_4", "quest_guide", 1) && QuestManager.hasFlag(a, "path_combat")
                        && QuestManager.hasFlag(a, "tutorial_done") && !QuestManager.hasFlag(a, "path_life")
                        && Economy.wallet(a) == 5300, Economy.wallet(a));

        QuestData data = QuestData.get(server);
        QuestData reloaded = QuestData.load(data.save(new CompoundTag()));
        QuestData.Progress rp = reloaded.peek(a.getUUID());
        check("quest records survive saving and loading", rp != null && rp.completed.size() == 4
                && rp.flags.contains("path_combat") && rp.flags.contains("tutorial_done"));

        // 임시 퀘스트로 나머지 규칙 점검
        BlockPos at = a.blockPosition();
        QuestDef visit = tempQuest("{\"id\":\"t_visit\",\"giver\":\"quest_guide\",\"objectives\":[{\"type\":\"VISIT\",\"x\":" + at.getX()
                + ",\"y\":" + at.getY() + ",\"z\":" + at.getZ() + ",\"radius\":5}],\"reward\":{\"gold\":10}}");
        QuestDef pay = tempQuest("{\"id\":\"t_pay\",\"giver\":\"quest_guide\",\"objectives\":[{\"type\":\"PAY\",\"count\":100}],\"reward\":{\"gold\":10}}");
        QuestDef secret = tempQuest("{\"id\":\"t_secret\",\"giver\":\"quest_guide\",\"requiresFlags\":[\"secret\"],\"blockedByFlags\":[\"blocked\"]}");
        QuestManager.loaded().quests.put(visit.id, visit);
        QuestManager.loaded().quests.put(pay.id, pay);
        QuestManager.loaded().quests.put(secret.id, secret);
        for (int i = 0; i < 11; i++) {
            QuestDef q = tempQuest("{\"id\":\"t_l" + i + "\",\"giver\":\"quest_guide\"}");
            QuestManager.loaded().quests.put(q.id, q);
        }

        a.setPos(at.getX() + 100.5, at.getY(), at.getZ() + 0.5);
        check("visit quest is accepted", QuestManager.accept(a, "t_visit", "quest_guide"));
        QuestManager.checkVisits(a);
        check("being far away does not complete a visit", !QuestManager.turnIn(a, "t_visit", "quest_guide", -1));
        a.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        QuestManager.checkVisits(a);
        check("arriving at the place completes the visit", QuestManager.turnIn(a, "t_visit", "quest_guide", -1) && Economy.wallet(a) == 5310);

        Economy.setWallet(a, 50);
        check("pay quest is accepted", QuestManager.accept(a, "t_pay", "quest_guide"));
        check("cannot pay without the gold", !QuestManager.turnIn(a, "t_pay", "quest_guide", -1) && Economy.wallet(a) == 50);
        Economy.setWallet(a, 500);
        check("paying takes the gold and pays the reward", QuestManager.turnIn(a, "t_pay", "quest_guide", -1) && Economy.wallet(a) == 410, Economy.wallet(a));

        check("a quest needing a flag is hidden until the flag is set", !offered(a, "quest_guide", "t_secret"));
        QuestManager.setFlag(a, "secret", true);
        check("setting the flag reveals it", offered(a, "quest_guide", "t_secret"));
        QuestManager.setFlag(a, "blocked", true);
        check("a blocking flag hides it again", !offered(a, "quest_guide", "t_secret"));
        QuestManager.setFlag(a, "secret", false);
        QuestManager.setFlag(a, "blocked", false);

        int taken = 0;
        for (int i = 0; i < 11; i++) {
            if (QuestManager.accept(a, "t_l" + i, "quest_guide")) {
                taken++;
            }
        }
        check("at most " + QuestManager.MAX_ACTIVE + " quests can be active", taken == QuestManager.MAX_ACTIVE, taken);
        check("abandoning frees a slot", QuestManager.abandon(a, "t_l0") && QuestManager.accept(a, "t_l10", "quest_guide"));
        check("an abandoned quest can be taken again later", QuestManager.accept(a, "t_l0", "quest_guide") == false
                && QuestManager.abandon(a, "t_l1") && QuestManager.accept(a, "t_l0", "quest_guide"));

        boolean badType = false;
        try {
            tempQuest("{\"id\":\"x\",\"giver\":\"bank\",\"objectives\":[{\"type\":\"FLY\"}]}");
        } catch (IllegalArgumentException e) {
            badType = true;
        }
        boolean badId = false;
        try {
            tempQuest("{\"id\":\"Bad Id\",\"giver\":\"bank\"}");
        } catch (IllegalArgumentException e) {
            badId = true;
        }
        check("quest files with an unknown goal type or a bad id are rejected", badType && badId);

        // 잘못된 파일: 없는 선행 퀘스트, 순환
        Path bad = QuestManager.dir().resolve("zz_selftest.json");
        Files.writeString(bad, "{\"quests\":["
                + "{\"id\":\"zz_missing\",\"giver\":\"bank\",\"requires\":[\"does_not_exist\"]},"
                + "{\"id\":\"zz_a\",\"giver\":\"bank\",\"requires\":[\"zz_b\"]},"
                + "{\"id\":\"zz_b\",\"giver\":\"bank\",\"requires\":[\"zz_a\"]}]}", StandardCharsets.UTF_8);
        QuestManager.Loaded lb = QuestManager.reload();
        check("a missing prerequisite and a prerequisite loop are reported and those quests are dropped",
                !lb.quests.containsKey("zz_missing") && !lb.quests.containsKey("zz_a") && !lb.quests.containsKey("zz_b")
                        && lb.problems.size() >= 3 && lb.quests.containsKey("tutorial_1"), lb.problems.size());
        Files.deleteIfExists(bad);

        QuestManager.reload();
        QuestManager.reset(a);
        Economy.setWallet(a, 0);
        clear(a);
        a.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
    }


    private static void questEditor(MinecraftServer server, ServerLevel level, FakePlayer a) throws Exception {
        section("quest editor");
        Path dir = QuestManager.dir();
        Path tut = dir.resolve("tutorial.json");
        Path npcs = dir.resolve("npcs.json");
        Path custom = dir.resolve(QuestManager.CUSTOM_FILE);
        byte[] tutOrig = Files.readAllBytes(tut);
        byte[] npcOrig = Files.readAllBytes(npcs);
        Files.deleteIfExists(custom);
        try {
            QuestManager.reload();
            QuestDef t1 = QuestManager.loaded().quests.get("tutorial_1");
            QuestDef round = QuestDef.parse(t1.toJson());
            check("a quest written back to a file reads the same",
                    round.title.equals(t1.title) && round.objectives.size() == t1.objectives.size()
                            && round.rewardGold == t1.rewardGold && round.giver.equals(t1.giver));

            // 기존 퀘스트 고치기 -> 원래 파일에 그대로
            JsonObject edited = t1.toJson();
            edited.addProperty("title", "고친 제목");
            QuestEditor.Result r = QuestEditor.saveQuest("tutorial_1", edited.toString());
            check("editing a quest saves it in its own file and applies at once",
                    r.ok() && "고친 제목".equals(QuestManager.loaded().quests.get("tutorial_1").title)
                            && Files.readString(tut, StandardCharsets.UTF_8).contains("고친 제목") && !Files.exists(custom), r.message());
            check("the rest of that file is kept (other quests, help text, order)",
                    Files.readString(tut, StandardCharsets.UTF_8).contains("_help")
                            && QuestManager.loaded().quests.containsKey("tutorial_4")
                            && QuestManager.loaded().questFile.get("tutorial_4").equals("tutorial.json"));

            // 업데이트(기본 파일 설치)가 고친 내용을 덮어쓰지 않는가
            Files.deleteIfExists(dir.resolve(".installed_defaults"));
            QuestManager.reload();
            check("a mod update never overwrites an edited quest file",
                    "고친 제목".equals(QuestManager.loaded().quests.get("tutorial_1").title));
            Files.delete(npcs);
            QuestManager.reload();
            check("a default file the owner deleted is not put back", !Files.exists(npcs));
            Files.write(npcs, npcOrig);
            QuestManager.reload();

            JsonObject renamed = t1.toJson();
            renamed.addProperty("id", "tutorial_1_new");
            check("the id of an existing quest cannot be changed",
                    !QuestEditor.saveQuest("tutorial_1", renamed.toString()).ok());

            String fresh = "{\"id\":\"ed_new\",\"title\":\"새 이야기\",\"giver\":\"quest_guide\",\"requires\":[\"tutorial_1\"],"
                    + "\"intro\":\"첫 줄\\n둘째 줄\",\"objectives\":[{\"type\":\"KILL\",\"target\":\"minecraft:zombie\",\"count\":3}],"
                    + "\"reward\":{\"gold\":500,\"flags\":[\"ed_flag\"]}}";
            r = QuestEditor.saveQuest("", fresh);
            check("a new quest goes to " + QuestManager.CUSTOM_FILE + " and is usable at once",
                    r.ok() && Files.exists(custom) && QuestManager.loaded().quests.containsKey("ed_new")
                            && QuestManager.CUSTOM_FILE.equals(QuestManager.loaded().questFile.get("ed_new")), r.message());
            check("line breaks in story text survive saving",
                    QuestManager.loaded().quests.get("ed_new").intro.equals("첫 줄\n둘째 줄"));
            check("a second new quest with the same id is refused", !QuestEditor.saveQuest("", fresh).ok());
            check("a quest without a giver is refused",
                    !QuestEditor.saveQuest("", "{\"id\":\"ed_bad\",\"title\":\"x\"}").ok()
                            && !QuestManager.loaded().questFile.containsKey("ed_bad"));
            check("broken JSON is refused", !QuestEditor.saveQuest("", "{\"id\":").ok());

            r = QuestEditor.saveQuest("", "{\"id\":\"ed_orphan\",\"giver\":\"bank\",\"requires\":[\"nope\"]}");
            check("a quest with a missing prerequisite is saved but reported as unused",
                    r.ok() && r.message().contains("nope") && !QuestManager.loaded().quests.containsKey("ed_orphan"), r.message());
            boolean listed = false;
            for (var e : QuestEditor.snapshot(server).quests) {
                if (e.id().equals("ed_orphan") && !e.status().isEmpty()) {
                    listed = true;
                }
            }
            check("the editor still lists an unused quest with the reason, so it can be fixed", listed);
            check("fixing it makes it usable",
                    QuestEditor.saveQuest("ed_orphan", "{\"id\":\"ed_orphan\",\"giver\":\"bank\"}").ok()
                            && QuestManager.loaded().quests.containsKey("ed_orphan"));

            String child = "{\"id\":\"ed_child\",\"giver\":\"bank\",\"requires\":[\"ed_new\"]}";
            check("a dependent quest is saved", QuestEditor.saveQuest("", child).ok());
            check("a quest another quest depends on cannot be deleted", !QuestEditor.deleteQuest("ed_new").ok()
                    && QuestManager.loaded().quests.containsKey("ed_new"));
            check("deleting works once nothing depends on it",
                    QuestEditor.deleteQuest("ed_child").ok() && QuestEditor.deleteQuest("ed_new").ok()
                            && !QuestManager.loaded().quests.containsKey("ed_new"));

            // NPC
            r = QuestEditor.saveNpc(server, "", "{\"id\":\"ed_npc\",\"name\":\"편집 NPC\",\"profession\":\"farmer\",\"greeting\":\"안녕\"}");
            check("a new NPC is saved and can be spawned", r.ok() && QuestManager.loaded().npcs.containsKey("ed_npc"), r.message());
            BlockPos at = level.getSharedSpawnPos().above(6);
            level.getChunkAt(at);
            Villager v = ShopServer.spawnNpc(level, Vec3.atBottomCenterOf(at), 0, "quest_ed_npc");
            r = QuestEditor.saveNpc(server, "ed_npc", "{\"id\":\"ed_npc\",\"name\":\"바뀐 이름\",\"profession\":\"farmer\"}");
            check("renaming an NPC also renames the spawned one",
                    r.ok() && v != null && "바뀐 이름".equals(v.getCustomName().getString()), r.message());
            QuestEditor.saveQuest("", "{\"id\":\"ed_uses_npc\",\"giver\":\"quest_ed_npc\"}");
            check("an NPC used by a quest cannot be deleted", !QuestEditor.deleteNpc("ed_npc").ok());
            QuestEditor.deleteQuest("ed_uses_npc");
            check("an unused NPC can be deleted", QuestEditor.deleteNpc("ed_npc").ok() && !QuestManager.loaded().npcs.containsKey("ed_npc"));
            check("an NPC id with spaces or capitals is refused",
                    !QuestEditor.saveNpc(server, "", "{\"id\":\"Bad Npc\",\"name\":\"x\"}").ok());
            if (v != null) {
                v.discard();
            }
            check("the editor snapshot lists quests, NPCs and every NPC to pick from",
                    QuestEditor.snapshot(server).quests.size() >= 4 && !QuestEditor.snapshot(server).npcs.isEmpty()
                            && QuestEditor.snapshot(server).npcKeys.size() >= 2 + ShopServer.shops().size());
        } finally {
            Files.write(tut, tutOrig);
            Files.write(npcs, npcOrig);
            Files.deleteIfExists(custom);
            QuestManager.reload();
        }
    }



    // ------------------------------------------------------------------ 직업 선행 퀘스트

    private static Item deliverItem(String target) {
        if (target.startsWith("#")) {
            var tag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, new ResourceLocation(target.substring(1)));
            var tags = ForgeRegistries.ITEMS.tags();
            if (tags == null) {
                return null;
            }
            return tags.getTag(tag).stream().findFirst().orElse(null);
        }
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(target));
        return item == null || item == Items.AIR ? null : item;
    }

    private static void jobQuests(MinecraftServer server, ServerLevel level, FakePlayer a) {
        section("job quests");
        QuestManager.Loaded l = QuestManager.reload();
        com.rtshop.job.JobConfig cfg = com.rtshop.job.JobConfig.get();
        check("job quest file loads without problems", l.problems.isEmpty(), l.problems);
        try {
            Path jq = QuestManager.dir().resolve("job_quests.json");
            byte[] now = Files.readAllBytes(jq);
            Files.writeString(jq, "{\"quests\":[]}");
            QuestManager.reload();
            check("an edited job quest file is never replaced", Files.readString(jq).equals("{\"quests\":[]}"));
            Files.write(jq, now);
            QuestManager.reload();
        } catch (Exception e) {
            check("an edited job quest file is never replaced", false, e);
        }
        int count = 0;
        for (String id : l.quests.keySet()) {
            if (id.startsWith("job_")) {
                count++;
            }
        }
        check("every job has a 7-step quest line (63 quests)", count == 63, count);
        for (String npc : new String[]{"sword_master", "war_chief", "shield_keeper", "hunter", "archmage"}) {
            check("mentor NPC '" + npc + "' is defined", l.npcs.containsKey(npc));
        }
        List<String> missing = new ArrayList<>();
        for (QuestDef q : l.quests.values()) {
            if (!q.id.startsWith("job_")) {
                continue;
            }
            for (QuestDef.Objective ob : q.objectives) {
                if (ob.type == QuestDef.Type.DELIVER && deliverItem(ob.target) == null) {
                    missing.add(q.id + ":" + ob.target);
                }
                if (ob.type == QuestDef.Type.KILL && !ob.target.equals("*") && EntityType.byString(ob.target).isEmpty()) {
                    missing.add(q.id + ":" + ob.target);
                }
            }
        }
        check("every item and monster the job quests ask for exists in this game", missing.isEmpty(), missing);
        // 모드 조건 (3.6.2): 모드가 있으면 모드 목표·보상, 없으면 바닐라 목표만 남는다
        check("mod conditions: always / with / without a mod",
                QuestDef.modOk("") && QuestDef.modOk("minecraft") && !QuestDef.modOk("!minecraft")
                        && !QuestDef.modOk("no_such_mod_xyz") && QuestDef.modOk("!no_such_mod_xyz"));
        {
            QuestDef sw = l.quests.get("job_swordsman_2");
            boolean ef = net.minecraftforge.fml.ModList.get().isLoaded("epicfight");
            String want = ef ? "epicfight:iron_tachi" : "minecraft:iron_sword";
            check("the swordsman asks for the Epic Fight tachi only when Epic Fight is installed (else an iron sword)",
                    sw != null && sw.objectives.size() == 1 && sw.objectives.get(0).target.equals(want),
                    sw == null ? "missing" : sw.objectives.stream().map(o -> o.target).toList());
            QuestDef fi = l.quests.get("job_fisher_4");
            boolean sc = net.minecraftforge.fml.ModList.get().isLoaded("starcatcher");
            check("the fisher's tool step follows Starcatcher (parts and a Starcatcher rod as reward, or a plain rod)",
                    fi != null && fi.objectives.size() == (sc ? 3 : 2) && fi.rewardItems.size() == (sc ? 1 : 0),
                    fi == null ? "missing" : fi.objectives.stream().map(o -> o.target).toList() + " " + fi.rewardItems.size());
            List<String> modsUsed = new ArrayList<>();
            try (java.io.InputStream in = QuestManager.class.getResourceAsStream("/rtshop_defaults/quests/job_quests.json")) {
                com.google.gson.JsonObject root = com.google.gson.JsonParser.parseString(
                        new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\[([a-z0-9_.\\-]+)]|\"mod\":\\s*\"!?([a-z0-9_.\\-]+)\"")
                        .matcher(root.toString());
                while (m.find()) {
                    String id = m.group(1) != null ? m.group(1) : m.group(2);
                    if (!modsUsed.contains(id)) {
                        modsUsed.add(id);
                    }
                }
                // 편집기로 저장해도 조건이 그대로 남는지
                QuestDef parsed = null;
                for (com.google.gson.JsonElement e : root.getAsJsonArray("quests")) {
                    if (e.getAsJsonObject().get("id").getAsString().equals("job_fisher_4")) {
                        parsed = QuestDef.parse(e.getAsJsonObject());
                    }
                }
                QuestDef again = parsed == null ? null : QuestDef.parse(parsed.toJson());
                check("mod conditions survive saving from the editor",
                        again != null && again.objectives.get(0).mod.equals("starcatcher") && again.objectives.get(2).mod.equals("!starcatcher")
                                && again.rewardItems.get(0).mod.equals("starcatcher"));
            } catch (Exception ex) {
                check("mod conditions survive saving from the editor", false, ex);
            }
            check("the job quests name the mods they explain", modsUsed.containsAll(List.of("epicfight", "irons_spellbooks",
                    "kaleidoscope_cookery", "handson_delight", "starcatcher", "farmersdelight")), modsUsed);
            if (ef) {
                List<String> absent = new ArrayList<>();
                for (String id : modsUsed) {
                    if (!net.minecraftforge.fml.ModList.get().isLoaded(id)) {
                        absent.add(id);
                    }
                }
                check("every mod the job quests explain is in the pack", absent.isEmpty(), absent);
            }
        }

        for (com.rtshop.job.JobConfig.JobDef d : cfg.jobs.values()) {
            QuestManager.reset(a);
            clear(a);
            check("job '" + d.id + "' is locked until its quest line is done", !com.rtshop.job.Jobs.unlocked(a, d));
            boolean ok = true;
            String fail = "";
            for (int n = 1; n <= 20 && ok; n++) {
                String qid = "job_" + d.id + "_" + n;
                QuestDef q = l.quests.get(qid);
                if (q == null) {
                    if (n <= 7) {
                        ok = false;
                        fail = qid + " missing";
                    }
                    break;
                }
                boolean offered = false;
                for (QuestView v : QuestManager.viewsFor(a, q.giver)) {
                    offered |= v.id().equals(qid) && v.state() == QuestView.AVAILABLE;
                }
                if (!offered || !QuestManager.accept(a, qid, q.giver)) {
                    ok = false;
                    fail = qid + " not offered by " + q.giver;
                    break;
                }
                for (QuestDef.Objective ob : q.objectives) {
                    switch (ob.type) {
                        case TALK -> QuestManager.onTalk(a, ob.target);
                        case KILL -> {
                            var type = EntityType.byString(ob.target);
                            if (type.isPresent() && type.get().create(level) instanceof net.minecraft.world.entity.LivingEntity victim) {
                                for (int k = 0; k < ob.count; k++) {
                                    QuestManager.onKill(a, victim);
                                }
                            }
                        }
                        case DELIVER -> {
                            Item item = deliverItem(ob.target);
                            if (item != null) {
                                int left = ob.count;
                                while (left > 0) {
                                    int c = Math.min(left, new ItemStack(item).getMaxStackSize());
                                    a.getInventory().add(new ItemStack(item, c));
                                    left -= c;
                                }
                            }
                        }
                        case PAY -> Economy.setWallet(a, Economy.wallet(a) + ob.count);
                        default -> {
                        }
                    }
                }
                if (!QuestManager.turnIn(a, qid, q.turnInNpc(), -1)) {
                    ok = false;
                    fail = qid + " could not be turned in";
                }
                clear(a);
            }
            check("the " + d.name + " quest line can be played from start to end and unlocks the job",
                    ok && QuestManager.hasFlag(a, "unlock_job_" + d.id) && com.rtshop.job.Jobs.unlocked(a, d), fail);
        }
        QuestManager.reset(a);
        Economy.setWallet(a, 0);
        clear(a);
    }

    // ------------------------------------------------------------------ 직업

    /** 모드팩 전체에서 무기가 어떻게 분류되는지 보고서에 적는다 (무기 같은데 분류되지 않은 것 포함). */
    private static void weaponReport(com.rtshop.job.JobConfig jc) {
        java.util.Map<String, java.util.Map<String, Integer>> byMod = new java.util.TreeMap<>();
        List<String> unknown = new ArrayList<>();
        for (Item item : ForgeRegistries.ITEMS) {
            if (!com.rtshop.job.WeaponTypes.weaponLike(item)) {
                continue;
            }
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
            String type = jc.weaponType(new ItemStack(item));
            byMod.computeIfAbsent(rl.getNamespace(), k -> new java.util.TreeMap<>()).merge(type, 1, Integer::sum);
            if ("none".equals(type)) {
                unknown.add(rl.toString());
            }
        }
        byMod.forEach((mod, m) -> info("weapons in " + mod + ": " + m));
        info("weapon-like items without a type (" + unknown.size() + "): " + String.join(", ", unknown.subList(0, Math.min(80, unknown.size()))));
    }

    private static void jobs(MinecraftServer server, ServerLevel level, FakePlayer a) throws Exception {
        section("jobs");
        com.rtshop.job.JobConfig cfg = com.rtshop.job.JobConfig.get();
        com.rtshop.job.JobData data = com.rtshop.job.JobData.get(server);
        data.clear(a.getUUID());
        com.rtshop.job.Jobs.testClockOffset = 0;
        QuestManager.reset(a);
        clear(a);
        for (String id : cfg.jobs.keySet()) {
            QuestManager.setFlag(a, "unlock_job_" + id, true); // 선행 퀘스트는 아래 'job quests' 에서 따로 점검
        }
        try {
            check("9 jobs are defined (5 combat, 4 life)", cfg.jobs.size() >= 9
                    && cfg.jobs.values().stream().filter(d -> d.slot.equals("combat")).count() == 5
                    && cfg.jobs.values().stream().filter(d -> d.slot.equals("life")).count() == 4);
            Economy.setWallet(a, 20_000);
            check("the first combat job is free", com.rtshop.job.Jobs.choose(a, "swordsman").ok()
                    && com.rtshop.job.Jobs.has(a, "swordsman") && Economy.wallet(a) == 20_000);
            check("choosing the same job again does nothing", !com.rtshop.job.Jobs.choose(a, "swordsman").ok() && Economy.wallet(a) == 20_000);
            check("there is no waiting time after the first pick", com.rtshop.job.Jobs.cooldownLeft(a, "combat") == 0);
            check("changing costs " + cfg.changeCostCombat + " G and applies at once",
                    com.rtshop.job.Jobs.choose(a, "warrior").ok() && com.rtshop.job.Jobs.has(a, "warrior")
                            && !com.rtshop.job.Jobs.has(a, "swordsman") && Economy.wallet(a) == 20_000 - cfg.changeCostCombat, Economy.wallet(a));
            long left = com.rtshop.job.Jobs.cooldownLeft(a, "combat");
            check("after a change the combat slot is locked for " + cfg.cooldownHoursCombat + " hours",
                    left > cfg.cooldownMillis("combat") - 60_000 && left <= cfg.cooldownMillis("combat"), left);
            check("changing again during the wait is refused and costs nothing",
                    !com.rtshop.job.Jobs.choose(a, "tank").ok() && com.rtshop.job.Jobs.has(a, "warrior")
                            && Economy.wallet(a) == 20_000 - cfg.changeCostCombat);
            check("the life slot is separate (first pick free even while combat waits)",
                    com.rtshop.job.Jobs.choose(a, "farmer").ok() && com.rtshop.job.Jobs.has(a, "farmer") && com.rtshop.job.Jobs.has(a, "warrior"));
            com.rtshop.job.Jobs.testClockOffset = cfg.cooldownMillis("combat") + 1000;
            check("after the wait (real time) the combat job can be changed again",
                    com.rtshop.job.Jobs.choose(a, "tank").ok() && com.rtshop.job.Jobs.has(a, "tank"));
            Economy.setWallet(a, cfg.changeCostLife - 1);
            check("changing without enough Gold is refused", !com.rtshop.job.Jobs.choose(a, "miner").ok() && com.rtshop.job.Jobs.has(a, "farmer"));
            Economy.setWallet(a, cfg.changeCostLife);
            check("a life job change costs " + cfg.changeCostLife + " G", com.rtshop.job.Jobs.choose(a, "miner").ok() && Economy.wallet(a) == 0);

            // 선행 퀘스트로 잠긴 직업
            com.rtshop.job.Jobs.resetCooldowns(a);
            QuestManager.setFlag(a, "unlock_job_cook", false);
            QuestDef unlock = tempQuest("{\"id\":\"t_job_unlock\",\"giver\":\"job\",\"reward\":{\"flags\":[\"unlock_job_cook\"]}}");
            QuestManager.loaded().quests.put(unlock.id, unlock);
            Economy.setWallet(a, 100_000);
            check("a job whose unlock quest exists is locked until the quest is done",
                    !com.rtshop.job.Jobs.choose(a, "cook").ok() && com.rtshop.job.Jobs.has(a, "miner") && Economy.wallet(a) == 100_000);
            boolean listedLocked = false;
            for (var v : com.rtshop.job.Jobs.view(a, true).jobs) {
                listedLocked |= v.id().equals("cook") && v.state() == com.rtshop.net.JobOpenPacket.LOCKED && !v.lockQuest().isEmpty();
            }
            check("the job screen shows it as locked with the quest to do", listedLocked);
            check("finishing the quest unlocks it", QuestManager.accept(a, "t_job_unlock", "job")
                    && QuestManager.turnIn(a, "t_job_unlock", "job", -1) && com.rtshop.job.Jobs.choose(a, "cook").ok());
            QuestManager.reload();

            // 패시브
            com.rtshop.job.Jobs.force(a, "combat", "tank");
            check("tank takes 10% less damage", Math.abs(com.rtshop.job.JobPassives.incoming(a) - 0.90) < 1e-6, com.rtshop.job.JobPassives.incoming(a));
            check("tank has 4 more max health", Math.abs(a.getMaxHealth() - 24.0F) < 1e-3, a.getMaxHealth());
            com.rtshop.job.Jobs.applyAttributes(a);
            check("tank has knockback resistance", a.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE) >= 0.2 - 1e-6);
            com.rtshop.job.Jobs.force(a, "combat", "warrior");
            check("warrior has 2 more max health", Math.abs(a.getMaxHealth() - 22.0F) < 1e-3, a.getMaxHealth());
            check("knockback resistance is removed when no longer a tank",
                    a.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE) < 1e-6);
            check("warrior deals 10% more with an axe, the same with a sword, 20% less with a bow",
                    Math.abs(com.rtshop.job.JobPassives.outgoing(a, "axe") - 1.10) < 1e-6
                            && Math.abs(com.rtshop.job.JobPassives.outgoing(a, "sword") - 1.0) < 1e-6
                            && Math.abs(com.rtshop.job.JobPassives.outgoing(a, "bow") - 0.80) < 1e-6);
            double baseMove = a.getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
            check("warrior moves 10% faster", Math.abs(a.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) - baseMove * 1.10) < 1e-6);
            com.rtshop.job.Jobs.force(a, "combat", "archer");
            check("archer deals 15% more with a bow and 20% less with an axe",
                    Math.abs(com.rtshop.job.JobPassives.outgoing(a, "bow") - 1.15) < 1e-6 && Math.abs(com.rtshop.job.JobPassives.outgoing(a, "axe") - 0.80) < 1e-6);
            check("archer has 2 less max health (the warrior bonus is gone)", Math.abs(a.getMaxHealth() - 18.0F) < 1e-3, a.getMaxHealth());
            check("archer moves at normal speed", Math.abs(a.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) - baseMove) < 1e-6);
            check("archer distance bonus: none up close, half way at mid range, +15% at 32 blocks or more",
                    Math.abs(com.rtshop.job.JobPassives.distanceMultiplier(a, 4) - 1.0) < 1e-6
                            && Math.abs(com.rtshop.job.JobPassives.distanceMultiplier(a, 18.5) - 1.075) < 1e-6
                            && Math.abs(com.rtshop.job.JobPassives.distanceMultiplier(a, 50) - 1.15) < 1e-6,
                    com.rtshop.job.JobPassives.distanceMultiplier(a, 18.5));
            com.rtshop.job.Jobs.force(a, "combat", "tank");
            check("a non-archer gets no distance bonus", Math.abs(com.rtshop.job.JobPassives.distanceMultiplier(a, 50) - 1.0) < 1e-6);
            BlockPos zp = level.getSharedSpawnPos().above(3);
            level.getChunkAt(zp);
            net.minecraft.world.entity.monster.Zombie z = EntityType.ZOMBIE.create(level);
            if (z != null) {
                a.setPos(zp.getX() + 0.5, zp.getY(), zp.getZ() + 0.5);
                z.moveTo(zp.getX() + 2.5, zp.getY(), zp.getZ() + 0.5, 0, 0);
                z.setNoAi(true);
                level.addFreshEntity(z);
                float before = z.getHealth();
                com.rtshop.job.JobPassives.shieldResponse(a, z, 10.0F);
                check("a tank's shield block pushes the attacker away and reflects 5% of the damage (less the attacker's armor)",
                        z.getDeltaMovement().x > 0.01 && before - z.getHealth() > 0.3F && before - z.getHealth() <= 0.5F + 1e-3,
                        z.getDeltaMovement().x + " / " + (before - z.getHealth()));
                z.discard();
            }
            com.rtshop.job.Jobs.force(a, "combat", "mage");
            check("mage deals 25% less with physical weapons and loses 2 health",
                    Math.abs(com.rtshop.job.JobPassives.outgoing(a, "sword") - 0.75) < 1e-6 && Math.abs(com.rtshop.job.JobPassives.outgoing(a, "bow") - 0.75) < 1e-6
                            && Math.abs(com.rtshop.job.JobPassives.outgoing(a, "magic") - 1.0) < 1e-6 && Math.abs(a.getMaxHealth() - 18.0F) < 1e-3);
            com.rtshop.job.Jobs.force(a, "combat", "swordsman");
            check("swordsman: sword +10%, axe -15%, spells -15%", Math.abs(com.rtshop.job.JobPassives.outgoing(a, "sword") - 1.10) < 1e-6
                    && Math.abs(com.rtshop.job.JobPassives.outgoing(a, "axe") - 0.85) < 1e-6 && Math.abs(com.rtshop.job.JobPassives.outgoing(a, "magic") - 0.85) < 1e-6);
            com.rtshop.job.Jobs.force(a, "combat", "");
            check("without a combat job there is no weapon bonus or penalty", Math.abs(com.rtshop.job.JobPassives.outgoing(a, "axe") - 1.0) < 1e-6
                    && Math.abs(a.getMaxHealth() - 20.0F) < 1e-3);

            com.rtshop.job.JobConfig jc = com.rtshop.job.JobConfig.get();
            check("weapons are sorted into types (sword, axe, bow, crossbow, trident=spear, pickaxe=tool, empty hand=fist)",
                    "sword".equals(jc.weaponType(new ItemStack(Items.IRON_SWORD))) && "axe".equals(jc.weaponType(new ItemStack(Items.IRON_AXE)))
                            && "bow".equals(jc.weaponType(new ItemStack(Items.BOW))) && "crossbow".equals(jc.weaponType(new ItemStack(Items.CROSSBOW)))
                            && "spear".equals(jc.weaponType(new ItemStack(Items.TRIDENT))) && "tool".equals(jc.weaponType(new ItemStack(Items.IRON_PICKAXE)))
                            && "fist".equals(jc.weaponType(ItemStack.EMPTY)),
                    jc.weaponType(new ItemStack(Items.TRIDENT)) + "/" + jc.weaponType(new ItemStack(Items.IRON_PICKAXE)));
            check("non-weapons are not weapons (sugar cane is not a staff, a bowl is not a bow)",
                    "none".equals(jc.weaponType(new ItemStack(Items.SUGAR_CANE))) && "none".equals(jc.weaponType(new ItemStack(Items.BOWL))));
            weaponReport(jc);
            com.rtshop.job.Jobs.force(a, "combat", "swordsman");
            a.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
            double base = a.getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED);
            com.rtshop.job.Jobs.applyAttributes(a);
            double withSword = a.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED);
            a.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            com.rtshop.job.Jobs.applyAttributes(a);
            double bare = a.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED);
            check("swordsman attacks faster only while holding a sword", withSword > bare + 1e-6 && Math.abs(bare - base) < 1e-6,
                    base + " / " + withSword + " / " + bare);

            com.rtshop.job.Jobs.force(a, "combat", "mage");
            int found = 0;
            int raised = 0;
            List<String> names = new ArrayList<>();
            for (String id : new String[]{"irons_spellbooks:max_mana", "irons_spellbooks:spell_power", "spell_power:arcane", "spell_power:fire",
                    "spell_power:frost", "spell_power:healing", "spell_power:lightning", "spell_power:soul"}) {
                net.minecraft.world.entity.ai.attributes.Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(id));
                if (attr == null || a.getAttribute(attr) == null) {
                    continue;
                }
                found++;
                if (a.getAttribute(attr).getModifiers().stream().anyMatch(m -> m.getName().startsWith("rtshop job"))) {
                    raised++;
                    names.add(id);
                }
            }
            info("mage bonus applied to " + raised + " of " + found + " magic attributes: " + names);
            if (ModList.get().isLoaded("irons_spellbooks")) {
                check("mage raises Iron's Spells max mana and spell power", names.contains("irons_spellbooks:max_mana")
                        && names.contains("irons_spellbooks:spell_power"), names);
            }
            if (ModList.get().isLoaded("spell_power")) {
                check("mage raises Wizards (Spell Power) school power", names.stream().anyMatch(n -> n.startsWith("spell_power:")), names);
            }
            com.rtshop.job.Jobs.force(a, "combat", "swordsman");
            check("the mage bonus is removed after leaving the job", ForgeRegistries.ATTRIBUTES.getValues().stream().noneMatch(at ->
                    a.getAttribute(at) != null && a.getAttribute(at).getModifiers().stream().anyMatch(m -> m.getName().equals("rtshop job: mage"))));

            ShopData fisher = ShopServer.shops().get("fisher");
            int codIdx = fisher == null ? -1 : findEntry(fisher, e -> e.stack.is(Items.COD) && e.fish);
            com.rtshop.job.Jobs.force(a, "life", "fisher");
            check("fisher sells fish for 20% more", codIdx >= 0
                    && Math.abs(ShopServer.jobSellMultiplier(a, fisher.entries.get(codIdx)) - 1.20) < 1e-6);
            com.rtshop.job.Jobs.force(a, "life", "cook");
            ShopData food = ShopServer.shops().get("food");
            int steakIdx = food == null ? -1 : findEntry(food, e -> e.stack.is(Items.COOKED_BEEF));
            check("a cook gets no bonus on furnace food (no hopper money farm)", steakIdx < 0
                    || Math.abs(ShopServer.jobSellMultiplier(a, food.entries.get(steakIdx)) - 1.0) < 1e-6);
            check("a fisher bonus does not apply to a cook", codIdx < 0 || Math.abs(ShopServer.jobSellMultiplier(a, fisher.entries.get(codIdx)) - 1.0) < 1e-6);

            // 요리: 손으로 만드는 요리는 누구나 재료비의 110% 이상, 요리사는 140% 이상
            double margin = com.rtshop.shop.PricingConfig.get().handCookedMargin;
            int hand = 0;
            List<String> low = new ArrayList<>();
            for (ShopLoader.Price p : ShopServer.loaded().prices.values()) {
                if (!p.handMade || p.purchaseNeed <= 0) {
                    continue;
                }
                hand++;
                if (p.sellUnit + 1e-6 < p.purchaseNeed * (1 + margin)) {
                    low.add(ShopLoader.itemId(p.stack) + " " + p.sellUnit + "<" + p.purchaseNeed * (1 + margin));
                }
                double cook = p.sellUnit * (1 + cfg.job("cook").param("dishSell", 0));
                if (p.cookable && cook + 1e-6 < p.purchaseNeed * 1.40) {
                    low.add(ShopLoader.itemId(p.stack) + " cook " + cook + "<" + p.purchaseNeed * 1.4);
                }
            }
            info(hand + " hand-made foods");
            check("hand-made food pays everyone 10% over the ingredients and a cook 40% or more", low.isEmpty(),
                    low.isEmpty() ? null : low.subList(0, Math.min(6, low.size())));
            ShopLoader.Price bread = ShopServer.loaded().prices.get("minecraft:bread");
            ShopLoader.Price wheatP = ShopServer.loaded().prices.get("minecraft:wheat");
            check("bread pays at least 110% of its 3 wheat (also when dough can be baked)", bread != null && wheatP != null
                    && bread.sellUnit + 1e-6 >= 3 * wheatP.price * (1 + margin), bread == null ? null : bread.sellUnit);
            check("a cook gets the bonus on hand-made food but not on anything a furnace can make from shop goods",
                    ShopLoader.handCooked(ShopServer.loaded(), new ItemStack(Items.BREAD)) == (bread != null && bread.cookable));

            // 직업 편집기 저장
            java.nio.file.Path jf = com.rtshop.job.JobConfig.path();
            byte[] orig = Files.readAllBytes(jf);
            try {
                JsonObject edited = cfg.toJson();
                for (var e : edited.getAsJsonArray("jobs")) {
                    JsonObject jo = e.getAsJsonObject();
                    if (jo.get("id").getAsString().equals("warrior")) {
                        jo.getAsJsonObject("weapons").getAsJsonObject("axe").addProperty("damage", 0.30);
                    }
                }
                edited.getAsJsonObject("weaponOverrides").addProperty("minecraft:stick", "staff");
                check("the job editor saves and applies at once", com.rtshop.job.JobEditor.save(edited.toString()) == null
                        && Math.abs(com.rtshop.job.JobConfig.get().job("warrior").weaponDamage("axe") - 0.30) < 1e-9
                        && "staff".equals(com.rtshop.job.JobConfig.get().weaponType(new ItemStack(Items.STICK))));
                check("a broken job file from the editor is refused", com.rtshop.job.JobEditor.save("{\"jobs\":[{\"id\":\"Bad Id\"}]}") != null
                        && Math.abs(com.rtshop.job.JobConfig.get().job("warrior").weaponDamage("axe") - 0.30) < 1e-9);
                Files.writeString(jf, "{\"changeCostCombat\": 9000, \"jobs\": [{\"id\": \"tank\", \"params\": {\"damageTaken\": 0.06}}]}");
                com.rtshop.job.JobConfig old = com.rtshop.job.JobConfig.reload();
                check("an old (3.4) job file is replaced by the new defaults but keeps its costs",
                        old.changeCostCombat == 9000 && Math.abs(old.job("tank").param("damageTaken", 0) - 0.10) < 1e-9
                                && old.job("mage").weaponDamage("sword") < 0);
            } finally {
                Files.write(jf, orig);
                com.rtshop.job.JobConfig.reload();
            }

            com.rtshop.job.JobData copy = com.rtshop.job.JobData.load(data.save(new CompoundTag()));
            check("jobs survive saving and loading", "swordsman".equals(copy.of(a.getUUID()).combat) && "cook".equals(copy.of(a.getUUID()).life));
        } finally {
            com.rtshop.job.Jobs.testClockOffset = 0;
            data.clear(a.getUUID());
            com.rtshop.job.Jobs.applyAttributes(a);
            QuestManager.reset(a);
            Economy.setWallet(a, 0);
            clear(a);
        }
    }

    // ------------------------------------------------------------------ 경제

    private static void economy(MinecraftServer server, FakePlayer a) {
        section("economy");
        EconomyConfig cfg = EconomyConfig.get();
        clear(a);
        Economy.setWallet(a, 1000);
        EconomyData.get(server).setBank(a.getUUID(), 0);
        check("wallet can be set", Economy.wallet(a) == 1000);
        check("deposit moves gold to the bank", Economy.deposit(a, 400) && Economy.wallet(a) == 600 && Economy.bank(a) == 400);
        check("withdraw moves gold back", Economy.withdraw(a, 100) && Economy.wallet(a) == 700 && Economy.bank(a) == 300);
        check("cannot deposit more than carried", !Economy.deposit(a, 10_000) && Economy.wallet(a) == 700);
        check("cannot withdraw more than saved", !Economy.withdraw(a, 10_000) && Economy.bank(a) == 300);

        check("check is issued from carried gold", Economy.issueCheck(a, 250) && Economy.wallet(a) == 450);
        ItemStack chk = first(a, s -> s.is(ModItems.GOLD_CHECK.get()));
        check("check item holds the amount", !chk.isEmpty() && CheckItem.amount(chk) == 250);
        ItemStack copy = chk.copy();
        check("check larger than wallet is refused", !Economy.issueCheck(a, 100_000) && Economy.wallet(a) == 450);
        check("redeeming adds to carried gold, not the bank",
                Economy.redeemCheck(a, chk) && Economy.wallet(a) == 700 && Economy.bank(a) == 300);
        check("redeemed check is consumed", chk.isEmpty());
        check("a copied check cannot be redeemed twice", !Economy.redeemCheck(a, copy) && Economy.wallet(a) == 700);
        check("a forged check is worthless", !Economy.redeemCheck(a, CheckItem.create(UUID.randomUUID(), 999_999, "x"))
                && Economy.wallet(a) == 700);
        clear(a);
        Economy.issueCheck(a, 100);
        ItemStack tampered = first(a, s -> s.is(ModItems.GOLD_CHECK.get()));
        CheckItem.setAmount(tampered, 100_000);
        check("an edited check still pays only the recorded amount", Economy.redeemCheck(a, tampered) && Economy.wallet(a) == 700);

        // 사망 손실
        clear(a);
        Economy.setWallet(a, 1000);
        Economy.issueCheck(a, 200);
        ItemStack carried = first(a, s -> s.is(ModItems.GOLD_CHECK.get()));
        UUID carriedId = CheckItem.id(carried);
        MinecraftForge.EVENT_BUS.post(new LivingDeathEvent(a, a.damageSources().generic()));
        long w = Economy.wallet(a);
        double lo = 800 * (1 - cfg.deathLossMaxPercent / 100.0) - 1;
        double hi = 800 * (1 - cfg.deathLossMinPercent / 100.0) + 1;
        check("death takes " + cfg.deathLossMinPercent + "-" + cfg.deathLossMaxPercent + "% of carried gold", w >= lo && w <= hi, w);
        check("death does not touch the bank", Economy.bank(a) == 300);
        if (cfg.checkDeathLoss && carriedId != null) {
            long left = EconomyData.get(server).checkAmount(carriedId);
            check("a carried check loses value on death too", left < 200 && left >= 200 * (1 - cfg.deathLossMaxPercent / 100.0) - 1, left);
        }
        clear(a);
    }

    // ------------------------------------------------------------------ 상점

    private static ItemStack fish(Item item, int count, float percentile, String rarity) {
        ItemStack s = new ItemStack(item, count);
        CompoundTag infoTag = new CompoundTag();
        infoTag.putFloat("size", 30.0F);
        infoTag.putFloat("weight", 1200.0F);
        infoTag.putFloat("percentile", percentile);
        infoTag.putString("rarity", rarity);
        s.getOrCreateTag().put(ShopData.FISH_INFO_TAG, infoTag);
        return s;
    }

    private static void shops(FakePlayer a) {
        section("shops");
        Map<String, ShopData> shops = ShopServer.shops();
        for (String id : ShopLoader.DEFAULTS) {
            ShopData d = shops.get(id);
            check("shop '" + id + "' is loaded", d != null && !d.entries.isEmpty(), d == null ? "missing" : d.entries.size() + " items");
        }
        ShopData farmer = shops.get("farmer");
        if (farmer == null) {
            return;
        }
        int seedIdx = findEntry(farmer, e -> e.stack.is(Items.WHEAT_SEEDS) && SeedRules.isLicensed(e.stack) && e.buyable);
        int wheatIdx = findEntry(farmer, e -> e.stack.is(Items.WHEAT) && !e.stack.hasTag() && e.sellable);
        int carrotSeedIdx = findEntry(farmer, e -> e.stack.is(Items.CARROT) && SeedRules.isLicensed(e.stack) && e.buyable);
        check("farmer sells plantable wheat seeds", seedIdx >= 0);
        check("farmer sells planting-only carrots, separate from the crop", carrotSeedIdx >= 0
                && findEntry(farmer, e -> e.stack.is(Items.CARROT) && !e.stack.hasTag()) >= 0);
        check("farmer buys wheat", wheatIdx >= 0);
        // 씨앗은 상점에서만 구할 수 있다. 심을 수 있는 아이템이 어느 상점에도 없으면 그 작물은 키울 수 없게 된다.
        List<String> unsold = new ArrayList<>();
        int plantable = 0;
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            ItemStack plain = new ItemStack(item);
            String id = String.valueOf(ForgeRegistries.ITEMS.getKey(item));
            if (!SeedRules.isPlantable(plain) || id.startsWith("tacz:") || id.startsWith("kubejs:")) {
                continue;
            }
            plantable++;
            boolean sold = false;
            for (ShopData d : shops.values()) {
                if (findEntry(d, e -> e.buyable && e.stack.is(item) && SeedRules.isLicensed(e.stack)) >= 0) {
                    sold = true;
                    break;
                }
            }
            if (!sold) {
                unsold.add(id);
            }
        }
        check("every seed and plantable crop in the game is sold as a shop seed", unsold.isEmpty(),
                plantable + " plantable items" + (unsold.isEmpty() ? "" : "; not sold: " + unsold));
        if (seedIdx < 0 || wheatIdx < 0) {
            return;
        }
        ShopData.Entry seed = farmer.entries.get(seedIdx);
        ShopData.Entry wheat = farmer.entries.get(wheatIdx);
        ShopServer.openShop(a, "farmer", -1);

        clear(a);
        Economy.setWallet(a, 10_000);
        ShopServer.handleTransaction(a, "farmer", farmer.version, seedIdx, false, 5, 0);
        check("buying takes the listed price", Economy.wallet(a) == 10_000 - 5L * seed.price, Economy.wallet(a));
        check("bought seeds are plantable shop seeds", count(a, s -> s.is(Items.WHEAT_SEEDS) && SeedRules.isLicensed(s)) == 5);
        ItemStack owned = first(a, s -> s.is(Items.WHEAT_SEEDS));
        long before = Economy.wallet(a);
        ShopServer.handleTransaction(a, "farmer", farmer.version, seedIdx, true, 5, farmer.variantKey(seed, owned));
        check("selling back pays 50%", Economy.wallet(a) - before == ShopData.revenue(seed.price * 0.5, 5)
                && count(a, s -> s.is(Items.WHEAT_SEEDS)) == 0, Economy.wallet(a) - before);

        clear(a);
        for (int i = 0; i < 36; i++) {
            a.getInventory().items.set(i, new ItemStack(Items.STONE, 64));
        }
        before = Economy.wallet(a);
        ShopServer.handleTransaction(a, "farmer", farmer.version, seedIdx, false, 1, 0);
        check("a full inventory blocks buying", Economy.wallet(a) == before && count(a, s -> s.is(Items.WHEAT_SEEDS)) == 0);

        clear(a);
        Economy.setWallet(a, 0);
        ShopServer.handleTransaction(a, "farmer", farmer.version, seedIdx, false, 1, 0);
        check("no gold, no purchase", Economy.wallet(a) == 0 && count(a, s -> s.is(Items.WHEAT_SEEDS)) == 0);

        // 별 등급에 따른 매입가
        clear(a);
        ItemStack w3 = new ItemStack(Items.WHEAT, 10);
        SeedRules.setStar(w3, 3);
        ItemStack w1 = new ItemStack(Items.WHEAT, 10);
        SeedRules.setStar(w1, 1);
        a.getInventory().setItem(1, w3);
        a.getInventory().setItem(2, w1);
        a.getInventory().setItem(3, new ItemStack(Items.WHEAT, 10));
        check("graded wheat is accepted as wheat", farmer.findSellEntry(w3) == wheatIdx && farmer.findSellEntry(w1) == wheatIdx);
        long k3 = farmer.variantKey(wheat, w3);
        long k1 = farmer.variantKey(wheat, w1);
        long k0 = farmer.variantKey(wheat, new ItemStack(Items.WHEAT));
        check("3 stars > 1 star > ungraded", k3 > k1 && k1 > k0, k3 + " / " + k1 + " / " + k0);
        check("ungraded produce sells at exactly 50%", k0 == Math.round(wheat.price * 0.5 * 100.0) || wheat.sellUnit != wheat.price * 0.5, k0);
        Economy.setWallet(a, 0);
        ShopServer.handleTransaction(a, "farmer", farmer.version, wheatIdx, true, 10, k3);
        check("3-star wheat pays the 3-star price", Economy.wallet(a) == ShopData.revenue(k3 / 100.0, 10)
                && count(a, s -> s.is(Items.WHEAT) && SeedRules.star(s) == 3) == 0, Economy.wallet(a));
        before = Economy.wallet(a);
        ShopServer.handleTransaction(a, "farmer", farmer.version, wheatIdx, true, 10, k3);
        check("lower grades cannot be sold at the 3-star price", Economy.wallet(a) == before && count(a, s -> s.is(Items.WHEAT)) == 20);
        ShopServer.handleSellAll(a, "farmer", farmer.version, new int[]{wheatIdx, wheatIdx}, new long[]{k1, k0});
        check("sell-all sells every grade at its own price",
                Economy.wallet(a) - before == ShopData.revenue(k1 / 100.0, 10) + ShopData.revenue(k0 / 100.0, 10)
                        && count(a, s -> s.is(Items.WHEAT)) == 0, Economy.wallet(a) - before);

        // 물고기 무게
        ShopData fisher = shops.get("fisher");
        int codIdx = fisher == null ? -1 : findEntry(fisher, e -> e.stack.is(Items.COD) && !e.stack.hasTag() && e.sellable);
        check("fisher buys cod", codIdx >= 0);
        if (codIdx >= 0) {
            ShopData.Entry cod = fisher.entries.get(codIdx);
            ItemStack heavy = fish(Items.COD, 3, 0.0F, "common");
            ItemStack light = fish(Items.COD, 3, 100.0F, "common");
            ItemStack mid = fish(Items.COD, 3, 50.0F, "common");
            ItemStack golden = fish(Items.COD, 1, 50.0F, "golden");
            double mh = fisher.qualityMultiplier(cod, heavy);
            double ml = fisher.qualityMultiplier(cod, light);
            double mm = fisher.qualityMultiplier(cod, mid);
            check("caught fish with weight data are accepted", fisher.findSellEntry(heavy) == codIdx);
            check("heavier fish sell for more", mh > mm && mm > ml, ml + " < " + mm + " < " + mh);
            check("weight swing stays within the configured band",
                    Math.abs(mh - fisher.fishBase * (1 + fisher.fishBonus)) < 1.0e-6 && Math.abs(ml - fisher.fishBase * (1 - fisher.fishBonus)) < 1.0e-6);
            check("golden fish use the golden multiplier", fisher.qualityMultiplier(cod, golden) == fisher.fishGolden);
            check("fish without weight data sell at the plain resale rate",
                    fisher.qualityMultiplier(cod, new ItemStack(Items.COD)) == (cod.fish ? fisher.fishNoInfo : 1.0));
            ShopServer.openShop(a, "fisher", -1);
            clear(a);
            a.getInventory().setItem(1, heavy);
            Economy.setWallet(a, 0);
            long kh = fisher.variantKey(cod, heavy);
            ShopServer.handleTransaction(a, "fisher", fisher.version, codIdx, true, 3, kh);
            check("heavy fish are paid at the heavy price", Economy.wallet(a) == ShopData.revenue(kh / 100.0, 3), Economy.wallet(a));
        }
        ShopServer.closeSession(a);
        clear(a);
    }

    // ------------------------------------------------------------------ 농사

    private static void field(ServerLevel level, BlockPos soil, BlockState crop, boolean licensed) {
        level.setBlock(soil, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7), 2);
        level.setBlock(soil.above(), crop, 2);
        CropData data = CropData.get(level);
        if (licensed) {
            data.mark(CropData.dim(level), soil.above());
        } else {
            data.unmark(CropData.dim(level), soil.above());
        }
    }

    private static void farming(ServerLevel level, FakePlayer a, BlockPos soil) {
        section("farming");
        FarmConfig cfg = FarmConfig.get();
        CropData data = CropData.get(level);
        String dim = CropData.dim(level);
        BlockPos crop = soil.above();
        BlockState ripeWheat = Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7);
        BlockState ripeCarrot = Blocks.CARROTS.defaultBlockState().setValue(CropBlock.AGE, 7);
        a.setPos(crop.getX() + 0.5, crop.getY(), crop.getZ() + 2.5);
        clear(a);
        takeDrops(level, crop);

        // 심기
        level.setBlock(soil, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7), 2);
        level.setBlock(crop, Blocks.AIR.defaultBlockState(), 2);
        ItemStack plain = new ItemStack(Items.WHEAT_SEEDS, 4);
        a.setItemInHand(InteractionHand.MAIN_HAND, plain);
        a.gameMode.useItemOn(a, level, plain, InteractionHand.MAIN_HAND, hitTop(soil));
        FarmEvents.flushPending();
        check("seeds that are not from the shop cannot be planted", level.getBlockState(crop).isAir() && plain.getCount() == 4);
        ItemStack lic = SeedRules.license(new ItemStack(Items.WHEAT_SEEDS, 4));
        a.setItemInHand(InteractionHand.MAIN_HAND, lic);
        a.gameMode.useItemOn(a, level, lic, InteractionHand.MAIN_HAND, hitTop(soil));
        FarmEvents.flushPending();
        check("shop seeds can be planted", level.getBlockState(crop).is(Blocks.WHEAT) && lic.getCount() == 3);
        check("the planted spot is recorded", data.isMarked(dim, crop));

        level.setBlock(crop, Blocks.AIR.defaultBlockState(), 2);
        data.unmark(dim, crop);
        ItemStack carrot = new ItemStack(Items.CARROT, 4);
        a.setItemInHand(InteractionHand.MAIN_HAND, carrot);
        a.gameMode.useItemOn(a, level, carrot, InteractionHand.MAIN_HAND, hitTop(soil));
        FarmEvents.flushPending();
        check("a harvested carrot cannot be planted", level.getBlockState(crop).isAir());
        ItemStack seedCarrot = SeedRules.license(new ItemStack(Items.CARROT, 4));
        check("the planting-only carrot is a different stack with its own name",
                !ItemStack.isSameItemSameTags(seedCarrot, carrot) && seedCarrot.hasCustomHoverName() && SeedRules.isLicensed(seedCarrot));
        a.setItemInHand(InteractionHand.MAIN_HAND, seedCarrot);
        a.gameMode.useItemOn(a, level, seedCarrot, InteractionHand.MAIN_HAND, hitTop(soil));
        FarmEvents.flushPending();
        check("the planting-only carrot can be planted", level.getBlockState(crop).is(Blocks.CARROTS));
        clear(a);

        // 수확 (블록 부수기)
        boolean seedSeen = false;
        boolean wheatSeen = false;
        boolean allStar = true;
        for (int i = 0; i < 8; i++) {
            field(level, soil, ripeWheat, true);
            a.gameMode.destroyBlock(crop);
            for (ItemStack s : takeDrops(level, crop)) {
                if (SeedRules.isPureSeed(s)) {
                    seedSeen = true;
                }
                if (s.is(Items.WHEAT)) {
                    wheatSeen = true;
                    if (SeedRules.star(s) < 1) {
                        allStar = false;
                    }
                }
            }
        }
        check("harvesting drops wheat", wheatSeen);
        check("harvesting never drops seeds", !seedSeen);
        check("harvested wheat gets a star grade", wheatSeen && allStar);
        check("the planting record is cleared by the harvest", !data.isMarked(dim, crop));

        field(level, soil, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 3), true);
        a.gameMode.destroyBlock(crop);
        check("an unripe crop gives nothing back (no seed refund)", takeDrops(level, crop).isEmpty());

        field(level, soil, ripeCarrot, true);
        a.gameMode.destroyBlock(crop);
        List<ItemStack> carrots = takeDrops(level, crop);
        boolean okCarrots = !carrots.isEmpty();
        for (ItemStack s : carrots) {
            if (s.is(Items.CARROT) && (SeedRules.isLicensed(s) || SeedRules.star(s) < 1)) {
                okCarrots = false;
            }
        }
        check("harvested carrots are graded produce, not seeds", okCarrots, carrots);

        field(level, soil, ripeWheat, false);
        a.gameMode.destroyBlock(crop);
        boolean ungraded = true;
        for (ItemStack s : takeDrops(level, crop)) {
            if (SeedRules.star(s) > 0 || SeedRules.isPureSeed(s)) {
                ungraded = false;
            }
        }
        check("crops not grown from shop seeds give ungraded produce", ungraded || !cfg.requireShopSeeds);

        // 우클릭 수확
        if (cfg.rightClickHarvest) {
            field(level, soil, ripeCarrot, true);
            clear(a);
            a.getInventory().setItem(5, SeedRules.license(new ItemStack(Items.CARROT, 3)));
            a.gameMode.useItemOn(a, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hitTop(crop));
            FarmEvents.flushPending();
            BlockState after = level.getBlockState(crop);
            check("right-click harvest replants using one shop seed", after.is(Blocks.CARROTS) && after.getValue(CropBlock.AGE) == 0
                    && count(a, SeedRules::isLicensed) == 2, after + " seeds=" + count(a, SeedRules::isLicensed));
            check("right-click harvest hands over graded produce",
                    count(a, s -> s.is(Items.CARROT) && !SeedRules.isLicensed(s) && SeedRules.star(s) > 0) >= 1);
            check("the replanted crop is recorded again", data.isMarked(dim, crop));
            takeDrops(level, crop);

            field(level, soil, ripeWheat, true);
            clear(a);
            a.gameMode.useItemOn(a, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hitTop(crop));
            FarmEvents.flushPending();
            check("without a shop seed the field is left empty", level.getBlockState(crop).isAir()
                    && count(a, s -> s.is(Items.WHEAT) && SeedRules.star(s) > 0) >= 1);
            takeDrops(level, crop);

            // 괭이 범위 수확
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    field(level, soil.offset(dx, 0, dz), ripeWheat, true);
                }
            }
            clear(a);
            ItemStack hoe = new ItemStack(Items.IRON_HOE);
            a.setItemInHand(InteractionHand.MAIN_HAND, hoe);
            a.getInventory().setItem(5, SeedRules.license(new ItemStack(Items.WHEAT_SEEDS, 9)));
            a.gameMode.useItemOn(a, level, hoe, InteractionHand.MAIN_HAND, hitTop(crop));
            FarmEvents.flushPending();
            int replanted = 0;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockState st = level.getBlockState(crop.offset(dx, 0, dz));
                    if (st.is(Blocks.WHEAT) && st.getValue(CropBlock.AGE) == 0) {
                        replanted++;
                    }
                }
            }
            Integer sweep = cfg.harvestSweep.get("minecraft:iron_hoe");
            int expected = sweep != null && sweep >= 1 ? 9 : 1;
            check("an iron hoe harvests and replants its whole area", replanted == expected
                    && count(a, SeedRules::isLicensed) == 9 - expected && count(a, s -> s.is(Items.WHEAT)) >= expected,
                    replanted + " replanted, seeds left " + count(a, SeedRules::isLicensed));
            check("the hoe wears down per crop", hoe.getDamageValue() == expected * cfg.harvestToolDamage, hoe.getDamageValue());
            takeDrops(level, crop);
        }

        // 다른 모드가 씨앗 없이 다시 심는 경우
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos s = soil.offset(dx, 0, dz);
                level.setBlock(s.above(), Blocks.AIR.defaultBlockState(), 2);
                level.setBlock(s, Blocks.AIR.defaultBlockState(), 2);
                data.unmark(dim, s.above());
            }
        }
        level.setBlock(soil, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7), 2);
        level.setBlock(crop, Blocks.CARROTS.defaultBlockState(), 3);
        FarmEvents.flushPending();
        check("a sprout replanted for free by something else is removed",
                level.getBlockState(crop).isAir() || !cfg.requireShopSeeds || !cfg.removeUnlicensedSprouts);
        data.mark(dim, crop);
        level.setBlock(crop, Blocks.CARROTS.defaultBlockState(), 3);
        FarmEvents.flushPending();
        check("a sprout planted from a shop seed stays", level.getBlockState(crop).is(Blocks.CARROTS));
        data.unmark(dim, crop);
        level.setBlock(crop, Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(soil, Blocks.AIR.defaultBlockState(), 2);
        takeDrops(level, crop);
        clear(a);
    }

    // ------------------------------------------------------------------ 다른 모드와 함께 쓸 때

    private static final String[] COOK_MODS = {"farmersdelight", "kaleidoscope_cookery", "kaleidoscope_tavern", "handson_delight",
            "farm_and_charm", "starcatcher", "better_farming_plus"};
    private static final String[] MOD_CROPS = {"kaleidoscope_cookery:tomato_crop", "kaleidoscope_cookery:chili_crop",
            "kaleidoscope_cookery:lettuce_crop", "farmersdelight:cabbages", "farmersdelight:onions", "farmersdelight:tomatoes",
            "farm_and_charm:barley_crop", "farm_and_charm:oat_crop", "farm_and_charm:corn_crop", "farm_and_charm:strawberry_crop",
            "farm_and_charm:tomato_crop", "farm_and_charm:lettuce_crop", "farm_and_charm:onion_crop"};

    /** 요리·농사 모드가 함께 설치된 서버에서만 의미가 있는 점검 (모드가 없으면 건너뜀). */
    private static void otherMods(ServerLevel level, FakePlayer a, BlockPos soil) {
        StringBuilder loaded = new StringBuilder();
        for (String m : COOK_MODS) {
            if (ModList.get().isLoaded(m)) {
                loaded.append(' ').append(m);
            }
        }
        if (loaded.length() == 0) {
            return;
        }
        section("other mods");
        info("installed:" + loaded);
        FarmConfig cfg = FarmConfig.get();
        CropData data = CropData.get(level);
        String dim = CropData.dim(level);
        if (ModList.get().isLoaded("kaleidoscope_cookery")) {
            check("the Kaleidoscope sickle is hooked (no free replanting)", FarmEvents.sickleCompat);
        }
        if (ModList.get().isLoaded("starcatcher")) {
            ShopData fisher = ShopServer.shops().get("fisher");
            int fish = 0;
            if (fisher != null) {
                for (ShopData.Entry e : fisher.entries) {
                    if (e.fish && e.sellable) {
                        fish++;
                    }
                }
            }
            check("Starcatcher fish are listed at the fisher by rarity", fish > 20, fish + " fish");
        }
        BlockPos crop = soil.above();
        a.setPos(crop.getX() + 0.5, crop.getY(), crop.getZ() + 2.5);
        for (String id : MOD_CROPS) {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            Block block = rl == null ? null : ForgeRegistries.BLOCKS.getValue(rl);
            if (block == null || block == Blocks.AIR) {
                continue;
            }
            BlockState st = block.defaultBlockState();
            IntegerProperty age = FarmEvents.ageProperty(st);
            if (age == null) {
                info(id + ": no age property, skipped");
                continue;
            }
            int max = 0;
            for (int v : age.getPossibleValues()) {
                max = Math.max(max, v);
            }
            st = st.setValue(age, max);
            level.setBlock(soil, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7), 2);
            level.setBlock(crop, st, 2);
            if (!level.getBlockState(crop).is(block)) {
                info(id + ": does not stay on farmland, skipped");
                continue;
            }
            data.mark(dim, crop);
            clear(a);
            takeDrops(level, crop);
            a.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            boolean byClick = cfg.rightClickHarvest && FarmEvents.harvestable(st);
            if (byClick) {
                a.gameMode.useItemOn(a, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hitTop(crop));
            } else {
                a.gameMode.destroyBlock(crop);
            }
            FarmEvents.flushPending();
            List<ItemStack> got = takeDrops(level, crop);
            for (ItemStack s : a.getInventory().items) {
                if (!s.isEmpty()) {
                    got.add(s.copy());
                }
            }
            boolean seed = false;
            boolean star = false;
            int produce = 0;
            for (ItemStack s : got) {
                if (SeedRules.isPureSeed(s)) {
                    seed = true;
                } else {
                    produce += s.getCount();
                    star |= SeedRules.star(s) > 0;
                }
            }
            BlockState after = level.getBlockState(crop);
            check(id + ": " + (byClick ? "right-click" : "breaking") + " gives graded produce, no seeds, and no free replant",
                    !seed && star && produce >= 1 && !after.is(block), got + " -> " + after);
            data.unmark(dim, crop);
            level.setBlock(crop, Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(soil, Blocks.AIR.defaultBlockState(), 2);
            takeDrops(level, crop);
            clear(a);
        }
    }

    // ------------------------------------------------------------------ 토지

    /**
     * 구획(3.9): 구불구불한 타원형 밀밭(가운데 나무, 둘레 풀 한 줄, 그 밖은 흙길)을 만들고
     * 자동 채우기가 밭 모양을 정확히 고르는지, 구획 보호·구매·반환·저장이 맞는지 본다.
     */
    private static void plots(MinecraftServer server, ServerLevel level, FakePlayer a, FakePlayer b, int ox, int y, int oz) {
        section("plots");
        LandData data = LandData.get(server);
        String dim = LandManager.dim(level);
        int size = 40;
        for (int cx = ox >> 4; cx <= (ox + size) >> 4; cx++) {
            for (int cz = oz >> 4; cz <= (oz + size) >> 4; cz++) {
                level.getChunk(cx, cz);
            }
        }
        // 0 = 밖(풀), 1 = 밭, 2 = 풀 테두리, 3 = 흙길
        int[][] kind = new int[size][size];
        for (int dz = 0; dz < size; dz++) {
            for (int dx = 0; dx < size; dx++) {
                double ex = (dx - 19.5) / 15.0, ez = (dz - 19.5) / 9.0;
                boolean notch = dx > 26 && dz < 16; // 오른쪽 위를 파낸 모양
                kind[dz][dx] = ex * ex + ez * ez <= 1.0 && !notch ? 1 : 0;
            }
        }
        for (int pass = 2; pass <= 3; pass++) {
            int[][] next = new int[size][];
            for (int dz = 0; dz < size; dz++) {
                next[dz] = kind[dz].clone();
            }
            for (int dz = 1; dz < size - 1; dz++) {
                for (int dx = 1; dx < size - 1; dx++) {
                    if (kind[dz][dx] != 0) {
                        continue;
                    }
                    int want = pass - 1;
                    if (kind[dz][dx - 1] == want || kind[dz][dx + 1] == want || kind[dz - 1][dx] == want || kind[dz + 1][dx] == want) {
                        next[dz][dx] = pass;
                    }
                }
            }
            kind = next;
        }
        LongOpenHashSet expected = new LongOpenHashSet();
        for (int dz = 0; dz < size; dz++) {
            for (int dx = 0; dx < size; dx++) {
                BlockPos ground = new BlockPos(ox + dx, y, oz + dz);
                for (int up = 1; up <= 6; up++) {
                    level.setBlock(ground.above(up), Blocks.AIR.defaultBlockState(), 2);
                }
                switch (kind[dz][dx]) {
                    case 1 -> {
                        level.setBlock(ground, Blocks.FARMLAND.defaultBlockState(), 2);
                        level.setBlock(ground.above(), Blocks.WHEAT.defaultBlockState(), 2);
                        expected.add(PlotManager.key(ox + dx, oz + dz));
                    }
                    case 2 -> {
                        level.setBlock(ground, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                        expected.add(PlotManager.key(ox + dx, oz + dz));
                    }
                    case 3 -> level.setBlock(ground, Blocks.DIRT_PATH.defaultBlockState(), 2);
                    default -> {
                        level.setBlock(ground, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                        level.setBlock(ground.above(), Blocks.GRASS.defaultBlockState(), 2);
                    }
                }
            }
        }
        // 밭 가운데 나무와 물길 (자동 채우기 때 구멍으로 남았다가 메워져야 함)
        BlockPos tree = new BlockPos(ox + 12, y, oz + 20);
        level.setBlock(tree, Blocks.DIRT.defaultBlockState(), 2);
        for (int up = 1; up <= 3; up++) {
            level.setBlock(tree.above(up), Blocks.OAK_LOG.defaultBlockState(), 2);
        }
        level.setBlock(tree.above(4), Blocks.OAK_LEAVES.defaultBlockState(), 2);
        for (int dx = 22; dx <= 26; dx++) {
            BlockPos w = new BlockPos(ox + dx, y, oz + 22);
            level.setBlock(w.above(), Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(w, Blocks.WATER.defaultBlockState(), 2);
        }

        LongOpenHashSet got = PlotManager.floodField(level, ox + 20, oz + 20, -1);
        int missing = 0, extra = 0;
        if (got != null) {
            for (long k : expected) {
                if (!got.contains(k)) {
                    missing++;
                }
            }
            for (long k : got) {
                if (!expected.contains(k)) {
                    extra++;
                }
            }
        }
        check("auto fill picks exactly the curved field, its tree and water, and the grass rim, but not the path",
                got != null && missing == 0 && extra == 0, (got == null ? "null" : got.size()) + " vs " + expected.size()
                        + " (missing " + missing + ", extra " + extra + ")");
        int pathX = -1, pathZ = -1;
        for (int dz = 0; dz < size && pathX < 0; dz++) {
            for (int dx = 0; dx < size; dx++) {
                if (kind[dz][dx] == 3) {
                    pathX = ox + dx;
                    pathZ = oz + dz;
                    break;
                }
            }
        }
        LongOpenHashSet onPath = PlotManager.floodField(level, pathX, pathZ, -1);
        check("auto fill does not start on a path", onPath != null && onPath.isEmpty());
        List<int[]> square = List.of(new int[]{0, 0}, new int[]{4, 0}, new int[]{4, 4}, new int[]{0, 4});
        check("a polygon covers its inside and its edges", PlotManager.polygonCells(square).size() == 25);

        int[] dropped = {0};
        LandData.Claim claim = PlotManager.buildClaim(level, expected, -1, dropped);
        check("a plot claim keeps the exact shape", claim != null && claim.area() == expected.size() && claim.mask != null
                && claim.y0 == y - LandConfig.get().plotDepthBelow && claim.y1 == level.getMaxBuildHeight() - 1,
                claim == null ? "null" : claim.area() + " y" + claim.y0);
        if (claim == null) {
            return;
        }
        LandData.Land plot = data.createPlot("test field", 5000);
        data.addClaim(plot, claim);
        BlockPos wheat = new BlockPos(ox + 20, y + 1, oz + 20);
        BlockPos corner = new BlockPos(claim.x0, y, claim.z0); // 네모 범위 안이지만 모양 밖
        check("the bounding-box corner is outside the plot shape", !claim.hasColumn(claim.x0, claim.z0)
                && data.landAt(dim, corner) == null);
        check("a plot for sale is protected from everyone", plot.forSale() && !LandManager.can(b, level, wheat, LandData.BREAK)
                && !LandManager.can(a, level, wheat, LandData.PLACE));
        check("right outside the curved edge nothing is protected", LandManager.can(b, level, corner, LandData.BREAK));

        a.setPos(ox + 20.5, y + 1, oz + 20.5);
        a.setXRot(0.0F);
        ItemStack deed = new ItemStack(ModItems.LAND_DEED.get(), 1);
        int aLands = data.landsOf(a.getUUID()).size();
        LandManager.useDeed(a, deed, DeedItem.Type.NORMAL);
        check("a land deed cannot be used over a plot", data.landsOf(a.getUUID()).size() == aLands && deed.getCount() == 1);

        b.setPos(ox + 20.5, y + 1, oz + 20.5);
        Economy.setWallet(b, 4999);
        check("buying needs enough carried Gold", !PlotManager.buy(b) && plot.forSale() && Economy.wallet(b) == 4999);
        Economy.setWallet(b, 6000);
        check("standing in the plot you can buy it", PlotManager.buy(b) && plot.owner.equals(b.getUUID()) && Economy.wallet(b) == 1000);
        check("after buying the owner may farm it and others may not",
                LandManager.can(b, level, wheat, LandData.BREAK) && !LandManager.can(a, level, wheat, LandData.BREAK));
        check("it cannot be bought twice", !PlotManager.buy(a));

        LandData copy = LandData.load(data.save(new CompoundTag()));
        LandData.Land loaded = copy.landAt(dim, wheat);
        check("plots and their shape survive a save and reload", loaded != null && loaded.plot && loaded.price == 5000
                && loaded.owner.equals(b.getUUID()) && copy.landAt(dim, corner) == null
                && loaded.claims.get(0).area() == claim.area());

        LandManager.handleAction(b, plot.id, LandManager.ACT_ABANDON, "", 0);
        long refund = 5000L * LandConfig.get().plotRefundPercent / 100;
        check("returning a plot refunds part of the price and puts it back on sale",
                plot.forSale() && data.land(plot.id) != null && Economy.wallet(b) == 1000 + refund, Economy.wallet(b));

        // 모양 고치기: 한쪽 절반만 남긴다
        LongOpenHashSet half = new LongOpenHashSet();
        for (long k : expected) {
            if (PlotManager.kx(k) < ox + 20) {
                half.add(k);
            }
        }
        LandData.Claim smaller = PlotManager.buildClaim(level, half, plot.id, dropped);
        data.replaceClaim(plot.claims.get(0), smaller);
        check("a plot's shape can be edited", data.landAt(dim, new BlockPos(ox + 15, y, oz + 20)) == plot
                && data.landAt(dim, new BlockPos(ox + 25, y, oz + 20)) == null);

        data.removeLand(plot);

        // 나누기 (3.11): 같은 밭을 8칸 격자로 나눈다
        LandData.Claim whole = PlotManager.buildClaim(level, expected, -1, dropped);
        LandData.Land big = data.createPlot("split field", 4840);
        data.addClaim(big, whole);
        List<LandData.Land> parts = PlotManager.split(data, big, 8);
        int partArea = 0;
        boolean allSale = true;
        for (LandData.Land l : parts) {
            partArea += l.claims.get(0).area();
            allSale &= l.forSale();
        }
        check("splitting a plot makes several plots that cover exactly the same blocks",
                parts.size() > 3 && partArea == expected.size() && allSale && data.land(big.id) == null,
                parts.size() + " parts, " + partArea + " blocks");
        boolean noTiny = true;
        for (LandData.Land l : parts) {
            noTiny &= l.claims.get(0).area() >= 8 * 8 * 2 / 5;
        }
        check("tiny leftover pieces are joined to a neighbour", noTiny);

        // 초보 농지: 농부가 되면 묶음의 빈 구획 하나를 무료로 받는다
        String group = LandConfig.get().starterPlotGroup;
        for (LandData.Land l : parts) {
            l.group = group;
        }
        data.setStarterGiven(b.getUUID(), false);
        com.rtshop.job.Jobs.force(b, com.rtshop.job.JobConfig.LIFE, "");
        Economy.setWallet(b, 0);
        com.rtshop.job.Jobs.force(b, com.rtshop.job.JobConfig.LIFE, LandConfig.get().starterPlotJob);
        LandData.Land got1 = null;
        int owned = 0;
        for (LandData.Land l : parts) {
            if (l.owner.equals(b.getUUID())) {
                got1 = l;
                owned++;
            }
        }
        check("a new farmer gets one starter field for free", owned == 1 && got1.paid == 0 && Economy.wallet(b) == 0
                && data.starterGiven(b.getUUID()), owned);
        com.rtshop.job.Jobs.force(b, com.rtshop.job.JobConfig.LIFE, LandConfig.get().starterPlotJob);
        int again = 0;
        for (LandData.Land l : parts) {
            if (l.owner.equals(b.getUUID())) {
                again++;
            }
        }
        check("checking again does not give a second starter field", again == 1);
        boolean revoke = LandConfig.get().starterPlotReturnWhenJobLost;
        LandConfig.get().starterPlotReturnWhenJobLost = true;
        com.rtshop.job.Jobs.force(b, com.rtshop.job.JobConfig.LIFE, "");
        check("leaving the farmer job takes the starter field back for the next farmer",
                got1 != null && got1.forSale() && data.landsOf(b.getUUID()).stream().noneMatch(l -> l.plot));
        com.rtshop.job.Jobs.force(b, com.rtshop.job.JobConfig.LIFE, LandConfig.get().starterPlotJob);
        int owned2 = 0;
        for (LandData.Land l : parts) {
            if (l.owner.equals(b.getUUID())) {
                owned2++;
            }
        }
        check("only one starter field per player, ever (no new one after coming back)", owned2 == 0);
        LandConfig.get().starterPlotReturnWhenJobLost = revoke;
        // 반환 시험을 위해 다시 준다
        if (got1 != null && got1.forSale()) {
            com.rtshop.land.StarterPlots.give(data, got1, b);
        }
        LandData.Land other = null;
        for (LandData.Land l : parts) {
            if (l.forSale()) {
                other = l;
                break;
            }
        }
        if (other != null) {
            LandData.Claim oc = other.claims.get(0);
            int sx = oc.x0, sz = oc.z0;
            outer:
            for (int zz = oc.z0; zz <= oc.z1; zz++) {
                for (int xx = oc.x0; xx <= oc.x1; xx++) {
                    if (oc.hasColumn(xx, zz)) {
                        sx = xx;
                        sz = zz;
                        break outer;
                    }
                }
            }
            a.setPos(sx + 0.5, y + 1, sz + 0.5);
            Economy.setWallet(a, 1_000_000);
            check("starter fields cannot be bought", !PlotManager.buy(a) && other.forSale() && Economy.wallet(a) == 1_000_000);
        }
        if (got1 != null) {
            LandManager.handleAction(b, got1.id, LandManager.ACT_ABANDON, "", 0);
            check("returning a starter field gives no Gold and puts it back for the next farmer",
                    got1.forSale() && Economy.wallet(b) == 0 && group.equals(got1.group));
        }
        for (LandData.Land l : parts) {
            data.removeLand(l);
        }
        data.setStarterGiven(b.getUUID(), false);
        com.rtshop.job.Jobs.force(b, com.rtshop.job.JobConfig.LIFE, "");
        Economy.setWallet(a, 0);
        Economy.setWallet(b, 0);
        for (int dz = 0; dz < size; dz++) {
            for (int dx = 0; dx < size; dx++) {
                for (int up = 0; up <= 6; up++) {
                    level.setBlock(new BlockPos(ox + dx, y + up, oz + dz), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }

    private static void land(MinecraftServer server, ServerLevel level, FakePlayer a, FakePlayer b, int gx, int y, int gz) {
        section("land");
        LandData data = LandData.get(server);
        LandConfig cfg = LandConfig.get();
        String dim = LandManager.dim(level);
        for (FakePlayer p : new FakePlayer[]{a, b}) {
            for (LandData.Land l : new ArrayList<>(data.landsOf(p.getUUID()))) {
                data.removeLand(l);
            }
            data.clearFirstDeed(p.getUUID());
            clear(p);
        }
        LandManager.clearRuntime();

        a.setPos(gx + 8.5, y, gz + 8.5);
        a.setXRot(0.0F);
        ItemStack deeds = new ItemStack(ModItems.LAND_DEED.get(), 8);
        LandManager.useDeed(a, deeds, DeedItem.Type.NORMAL);
        List<LandData.Land> mine = data.landsOf(a.getUUID());
        check("a deed creates a land and is used up", mine.size() == 1 && mine.get(0).claims.size() == 1 && deeds.getCount() == 7);
        if (mine.isEmpty()) {
            return;
        }
        LandData.Land land = mine.get(0);
        LandData.Claim c = land.claims.get(0);
        int topY = level.getMaxBuildHeight() - 1;
        check("the land is 16x16 on the grid, from below the feet up to the top of the world",
                c.x0 == gx && c.z0 == gz && c.x1 - c.x0 == 15 && c.z1 - c.z0 == 15
                        && c.y0 == y - cfg.depthBelowFeet && c.y1 == topY, c.x0 + "," + c.y0 + "," + c.z0 + " ~ y" + c.y1);
        check("building high above the floor of the land is still protected",
                !LandManager.can(b, level, new BlockPos(gx + 8, topY - 2, gz + 8), LandData.PLACE));
        BlockPos inside = new BlockPos(gx + 8, y, gz + 8);
        BlockPos outside = new BlockPos(gx - 3, y, gz + 8);
        check("the owner may do everything", LandManager.can(a, level, inside, LandData.BREAK)
                && LandManager.can(a, level, inside, LandData.CONTAINER) && LandManager.can(a, level, inside, LandData.PLACE));
        check("by default nobody else may do anything", !LandManager.can(b, level, inside, LandData.BREAK)
                && !LandManager.can(b, level, inside, LandData.PLACE) && !LandManager.can(b, level, inside, LandData.CONTAINER)
                && !LandManager.can(b, level, inside, LandData.USE) && !LandManager.can(b, level, inside, LandData.ENTITY));
        check("outside the land is unaffected", LandManager.can(b, level, outside, LandData.BREAK));

        // 실제 블록으로 확인
        level.setBlock(inside, Blocks.STONE.defaultBlockState(), 3);
        b.setPos(gx + 8.5, y + 1, gz + 10.5);
        check("a stranger cannot break blocks", !b.gameMode.destroyBlock(inside) && level.getBlockState(inside).is(Blocks.STONE));
        ItemStack cobble = new ItemStack(Items.COBBLESTONE, 4);
        b.setItemInHand(InteractionHand.MAIN_HAND, cobble);
        b.gameMode.useItemOn(b, level, cobble, InteractionHand.MAIN_HAND, hitTop(inside));
        check("a stranger cannot place blocks", level.getBlockState(inside.above()).isAir() && cobble.getCount() == 4);

        LandManager.handleAction(a, land.id, LandManager.ACT_SET_DEFAULT, "", LandData.PLACE);
        b.gameMode.useItemOn(b, level, cobble, InteractionHand.MAIN_HAND, hitTop(inside));
        check("the owner can let everyone place blocks", level.getBlockState(inside.above()).is(Blocks.COBBLESTONE));
        check("...which still does not allow breaking", !b.gameMode.destroyBlock(inside.above())
                && level.getBlockState(inside.above()).is(Blocks.COBBLESTONE));
        LandManager.handleAction(a, land.id, LandManager.ACT_SET_DEFAULT, "", 0);
        LandManager.handleAction(b, land.id, LandManager.ACT_SET_DEFAULT, "", LandData.ALL);
        check("only the owner can change the settings", land.defaultPerms == 0);

        LandData.Member m = new LandData.Member();
        m.id = b.getUUID();
        m.name = "rtshop_test_b";
        m.perms = LandData.BREAK;
        land.members.put(m.id, m);
        data.touch();
        check("a player given the break permission can break", b.gameMode.destroyBlock(inside.above())
                && level.getBlockState(inside.above()).isAir());
        takeDrops(level, inside.above());
        LandManager.handleAction(a, land.id, LandManager.ACT_SET_MEMBER, b.getUUID().toString(), LandData.CONTAINER);
        check("per-player permissions can be changed at any time", land.members.get(b.getUUID()).perms == LandData.CONTAINER
                && !LandManager.can(b, level, inside, LandData.BREAK) && LandManager.can(b, level, inside, LandData.CONTAINER));

        // 보관함 PIN
        BlockPos chest = inside.above();
        level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 3);
        a.setPos(chest.getX() + 0.5, chest.getY(), chest.getZ() + 2.5);
        b.setPos(chest.getX() + 0.5, chest.getY(), chest.getZ() - 1.5);
        b.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        check("a chest counts as storage", LandManager.isStorage(level, chest));
        LandManager.setPin(b, chest, "1234");
        check("only the owner can set a PIN", !LandManager.hasPin(level, chest));
        LandManager.setPin(a, chest, "12");
        LandManager.setPin(a, chest, "12ab");
        check("a PIN must be " + cfg.pinMinLength + "-" + cfg.pinMaxLength + " digits", !LandManager.hasPin(level, chest));
        LandManager.setPin(a, chest, "1234");
        check("the owner sets a PIN", LandManager.hasPin(level, chest));
        check("the owner is never asked for the PIN", !LandManager.needsPin(a, level, chest));
        check("anyone else must enter the PIN", LandManager.needsPin(b, level, chest));
        b.gameMode.useItemOn(b, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hitTop(chest));
        check("a locked chest does not open without the PIN", b.containerMenu == b.inventoryMenu);
        check("a wrong PIN is rejected", !LandManager.tryPin(b, chest, "0000") && LandManager.needsPin(b, level, chest));
        check("the right PIN unlocks", LandManager.tryPin(b, chest, "1234") && !LandManager.needsPin(b, level, chest));
        LandManager.openStorage(b, chest);
        check("the chest opens after the right PIN", b.containerMenu != b.inventoryMenu);
        b.closeContainer();
        LandManager.clearRuntime();
        for (int i = 0; i < cfg.pinMaxAttempts; i++) {
            LandManager.tryPin(b, chest, "9999");
        }
        check("too many wrong tries lock the player out for a while", !LandManager.tryPin(b, chest, "1234"));
        LandManager.clearRuntime();
        land.members.get(b.getUUID()).perms = LandData.ALL;
        check("a PIN chest cannot be broken by others", !b.gameMode.destroyBlock(chest) && level.getBlockState(chest).is(Blocks.CHEST));
        LandManager.setPin(a, chest, "");
        check("the owner can remove the PIN", !LandManager.hasPin(level, chest));
        LandManager.setPin(a, chest, "4321");
        check("the owner can break the chest, which clears its PIN", a.gameMode.destroyBlock(chest) && !LandManager.hasPin(level, chest));
        takeDrops(level, chest);
        land.members.remove(b.getUUID());

        // 권한 없는 사람은 보관함을 열 수 없다
        level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 3);
        b.gameMode.useItemOn(b, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hitTop(chest));
        check("a stranger cannot open storage", b.containerMenu == b.inventoryMenu);
        a.gameMode.useItemOn(a, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hitTop(chest));
        check("the owner can open storage", a.containerMenu != a.inventoryMenu);
        a.closeContainer();
        level.setBlock(chest, Blocks.AIR.defaultBlockState(), 3);
        takeDrops(level, chest);

        // 확장
        a.setPos(gx + 8.5, y, gz + 8.5);
        a.setXRot(0.0F);
        int deedsBefore = deeds.getCount();
        LandManager.useDeed(a, deeds, DeedItem.Type.NORMAL);
        check("a deed is not wasted inside your own land", land.claims.size() == 1 && deeds.getCount() == deedsBefore);
        a.setXRot(-90.0F);
        int beforeUp = deeds.getCount();
        LandManager.useDeed(a, deeds, DeedItem.Type.NORMAL);
        check("looking up does not waste a deed (the land already reaches the sky)", land.claims.size() == 1 && deeds.getCount() == beforeUp);
        a.setXRot(90.0F);
        LandManager.useDeed(a, deeds, DeedItem.Type.NORMAL);
        check("looking down adds a 16-block box below the same land", data.landsOf(a.getUUID()).size() == 1 && land.claims.size() == 2
                && land.claims.get(1).y1 == c.y0 - 1 && land.claims.get(1).y1 - land.claims.get(1).y0 == 15);
        a.setXRot(0.0F);
        a.setPos(gx + 16 + 8.5, y, gz + 8.5);
        LandManager.useDeed(a, deeds, DeedItem.Type.NORMAL);
        check("an adjacent box joins the existing land", data.landsOf(a.getUUID()).size() == 1 && land.claims.size() == 3);
        a.setPos(gx + 96 + 8.5, y, gz + 8.5);
        LandManager.useDeed(a, deeds, DeedItem.Type.NORMAL);
        check("a box somewhere else becomes a separate land", data.landsOf(a.getUUID()).size() == 2);
        ItemStack big = new ItemStack(ModItems.LARGE_LAND_DEED.get(), 1);
        a.setPos(gx + 320 + 5.5, y, gz + 5.5);
        LandManager.useDeed(a, big, DeedItem.Type.LARGE);
        LandData.Land bigLand = data.landAt(dim, new BlockPos(gx + 325, y, gz + 5));
        check("the large deed makes a 32x32 land up to the sky", bigLand != null && bigLand.claims.size() == 1
                && bigLand.claims.get(0).x1 - bigLand.claims.get(0).x0 == 31 && bigLand.claims.get(0).y1 == topY
                && bigLand.claims.get(0).y0 == y - 2 * cfg.depthBelowFeet
                && bigLand.claims.get(0).z1 - bigLand.claims.get(0).z0 == 31 && big.isEmpty());

        // 예전(3.2 이하) 토지: 높이 16칸짜리 구역이 서버를 켤 때 위로 늘어나는지, 위에 남의 토지가 있으면 그 아래까지만인지
        LandData.Land oldLow = data.createLand(a.getUUID(), "a");
        LandData.Claim low = new LandData.Claim();
        low.dim = dim;
        low.x0 = gx + 640;
        low.z0 = gz;
        low.x1 = low.x0 + 15;
        low.z1 = low.z0 + 15;
        low.y0 = y - 4;
        low.y1 = y + 11;
        low.type = "normal";
        data.addClaim(oldLow, low);
        LandData.Land oldHigh = data.createLand(b.getUUID(), "b");
        LandData.Claim high = new LandData.Claim();
        high.dim = dim;
        high.x0 = low.x0;
        high.z0 = low.z0;
        high.x1 = low.x1;
        high.z1 = low.z1;
        high.y0 = y + 40;
        high.y1 = y + 55;
        high.type = "normal";
        data.addClaim(oldHigh, high);
        LandManager.extendClaimsToSky(level.getServer());
        check("old lands are raised to the sky, stopping under someone else's land",
                low.y1 == y + 39 && low.y0 == y - 4 && high.y1 == topY && high.y0 == y + 40, low.y1 + " / " + high.y1);
        data.removeLand(oldLow);
        data.removeLand(oldHigh);

        b.setPos(gx + 8.5, y, gz + 8.5);
        b.setXRot(0.0F);
        ItemStack bDeed = new ItemStack(ModItems.LAND_DEED.get(), 2);
        LandManager.useDeed(b, bDeed, DeedItem.Type.NORMAL);
        check("land that is already owned cannot be claimed", data.landsOf(b.getUUID()).isEmpty() && bDeed.getCount() == 2);
        b.setPos(gx - 8 + 0.5, y, gz + 8.5);
        LandManager.useDeed(b, bDeed, DeedItem.Type.NORMAL);
        check("the neighbouring plot can be claimed by someone else", data.landsOf(b.getUUID()).size() == 1 && bDeed.getCount() == 1);
        check("neighbours cannot build on each other's land",
                !LandManager.can(b, level, inside, LandData.PLACE) && !LandManager.can(a, level, new BlockPos(gx - 8, y, gz + 8), LandData.PLACE));

        // 저장/불러오기
        LandData copy = LandData.load(data.save(new CompoundTag()));
        LandData.Land loaded = copy.landAt(dim, inside);
        check("lands survive a save and reload", loaded != null && loaded.claims.size() == land.claims.size()
                && loaded.owner.equals(a.getUUID()) && copy.landsOf(a.getUUID()).size() == data.landsOf(a.getUUID()).size());

        // 반환
        clear(a);
        int claims = land.claims.size();
        int landId = land.id;
        LandManager.handleAction(a, landId, LandManager.ACT_ABANDON, "", 0);
        check("returning a land frees it", data.land(landId) == null && data.landAt(dim, inside) == null);
        check("returning a land gives the deeds back", count(a, s -> s.getItem() instanceof DeedItem) == claims);
        clear(a);

        // 토지 상점
        ShopData shop = ShopServer.shops().get("land");
        if (shop != null) {
            int firstIdx = findEntry(shop, e -> e.stack.is(ModItems.FIRST_LAND_DEED.get()));
            int normalIdx = findEntry(shop, e -> e.stack.is(ModItems.LAND_DEED.get()));
            int largeIdx = findEntry(shop, e -> e.stack.is(ModItems.LARGE_LAND_DEED.get()));
            check("the land merchant sells all three deeds", firstIdx >= 0 && normalIdx >= 0 && largeIdx >= 0);
            if (firstIdx >= 0 && normalIdx >= 0 && largeIdx >= 0) {
                ShopData.Entry first = shop.entries.get(firstIdx);
                check("first deed < normal deed < large deed", first.price < shop.entries.get(normalIdx).price
                        && shop.entries.get(normalIdx).price < shop.entries.get(largeIdx).price,
                        first.price + " / " + shop.entries.get(normalIdx).price + " / " + shop.entries.get(largeIdx).price);
                check("deeds cannot be sold back for gold", !first.sellable && !shop.entries.get(normalIdx).sellable);
                ShopServer.openShop(a, "land", -1);
                Economy.setWallet(a, 1_000_000);
                ShopServer.handleTransaction(a, "land", shop.version, firstIdx, false, 1, 0);
                check("the first-land deed can be bought once", count(a, s -> s.is(ModItems.FIRST_LAND_DEED.get())) == 1
                        && Economy.wallet(a) == 1_000_000 - first.price);
                ShopServer.handleTransaction(a, "land", shop.version, firstIdx, false, 1, 0);
                check("...and only once per player", count(a, s -> s.is(ModItems.FIRST_LAND_DEED.get())) == 1
                        && Economy.wallet(a) == 1_000_000 - first.price);
                ShopServer.closeSession(a);
            }
        }
    }

    // ------------------------------------------------------------------ 가격

    private static void pricing() {
        section("pricing");
        ShopLoader.Loaded l = ShopServer.loaded();
        check("recipe costs were calculated", l.solved != null);
        info("price table: " + l.prices.size() + " prices, " + (l.scan == null ? 0 : l.scan.recipes.size()) + " recipes, built in "
                + l.loadMillis + " ms (recipe pricing " + l.pricingMillis + " ms)");
        check("the price table is built quickly enough not to stall the server", l.loadMillis < 30_000, l.loadMillis + " ms");
        int deficits = 0;
        int loops = 0;
        int craftLoops = 0;
        int foods = 0;
        StringBuilder sample = new StringBuilder();
        for (ShopLoader.Price p : l.prices.values()) {
            if (p.buyableSomewhere && p.sellUnit > p.price + 1.0e-6) {
                loops++;
                sample.append(' ').append(ShopLoader.itemId(p.stack));
            }
            if (p.cost < 0 || !ShopLoader.isPlain(p.stack) || p.fixedSell || !p.sellableSomewhere) {
                continue; // 매입하지 않는 품목(건축 자재 등)은 매입가가 의미가 없다
            }
            boolean food = p.stack.isEdible() || p.dynamicFood || (l.solved != null && l.solved.processed.contains(ShopLoader.itemId(p.stack)));
            if (food) {
                if (!p.finished) {
                    continue; // 원가 이하로 파는 기본 재료 (설탕·수박 조각처럼 사서 쓰는 것)
                }
                foods++;
                if (p.sellUnit + 1.0e-6 < p.cost) {
                    deficits++;
                    sample.append(' ').append(ShopLoader.itemId(p.stack));
                }
            } else if (p.sellUnit > p.cost + 1.0e-6) {
                craftLoops++;
                sample.append(' ').append(ShopLoader.itemId(p.stack));
            }
        }
        info(foods + " craftable foods priced from their recipes");
        check("no food sells for less than its ingredients cost", deficits == 0, deficits == 0 ? null : deficits + ":" + sample);
        check("nothing sells for more than the shop charges for it", loops == 0, loops == 0 ? null : loops + ":" + sample);
        check("no crafted item sells for more than its materials", craftLoops == 0, craftLoops == 0 ? null : craftLoops + ":" + sample);
        List<String> buyLoss = ShopLoader.purchaseDeficits(l);
        check("buying ingredients from the shops and cooking never loses money, and a hopper-fed furnace cannot farm money",
                buyLoss.isEmpty(), buyLoss.isEmpty() ? null : buyLoss.size() + ": " + String.join(" / ", buyLoss.subList(0, Math.min(8, buyLoss.size()))));
        int floored = 0;
        for (ShopLoader.Price p : l.prices.values()) {
            if (p.purchaseNeed > 0) {
                floored++;
            }
        }
        info(floored + " foods have at least one recipe whose ingredients can all be bought");

        // 되팔 때 50% 를 넘는 품목은 매입가를 직접 정한 것(양동이 보증금 등)뿐이어야 한다
        int over = 0;
        StringBuilder overSample = new StringBuilder();
        for (ShopLoader.Price p : l.prices.values()) {
            if (!p.fixedSell && p.buyableSomewhere && p.sellUnit > p.price * 0.5 + 1.0e-6) {
                over++;
                overSample.append(' ').append(ShopLoader.itemId(p.stack));
            }
        }
        check("anything bought from a merchant resells for at most 50%", over == 0, over == 0 ? null : over + ":" + overSample);

        ShopLoader.Price wheat = plain(l, Items.WHEAT);
        ShopLoader.Price bread = plain(l, Items.BREAD);
        ShopLoader.Price ingot = plain(l, Items.IRON_INGOT);
        ShopLoader.Price rawIron = plain(l, Items.RAW_IRON);
        ShopLoader.Price sword = plain(l, Items.IRON_SWORD);
        ShopLoader.Price bucket = plain(l, Items.BUCKET);
        ShopLoader.Price milk = plain(l, Items.MILK_BUCKET);
        ShopLoader.Price sugar = plain(l, Items.SUGAR);
        ShopLoader.Price cane = plain(l, Items.SUGAR_CANE);
        ShopLoader.Price beef = plain(l, Items.BEEF);
        ShopLoader.Price steak = plain(l, Items.COOKED_BEEF);
        check("the price table has the reference items", wheat != null && bread != null && ingot != null && rawIron != null
                && sword != null && bucket != null && milk != null && sugar != null && cane != null && beef != null && steak != null);
        if (wheat != null && bread != null) {
            // 빵을 만드는 가장 싼 방법 기준. 바닐라만 있으면 밀 3개, 요리 모드가 있으면 더 싼 반죽 조리법이 기준이 된다
            boolean vanillaOnly = !ModList.get().isLoaded("farmersdelight") && !ModList.get().isLoaded("farm_and_charm");
            double hm = 1.0 + com.rtshop.shop.PricingConfig.get().handCookedMargin;
            check("bread made from bought wheat sells for its ingredients plus the hand-made margin",
                    bread.sellUnit <= 3.0 * wheat.price * hm + 1.0e-6 && bread.sellUnit >= wheat.price * 0.9
                            && (!vanillaOnly || Math.abs(bread.sellUnit - 3.0 * wheat.price * hm) < 1.0e-6),
                    bread.sellUnit + " (wheat " + wheat.price + ", " + bread.basis + ")");
            check("...and costs at least twice that to buy back", bread.price >= 2 * bread.sellUnit - 1.0e-6, bread.price);
        }
        if (beef != null && steak != null) {
            check("cooking bought meat is break-even (no fuel bonus that a furnace array could farm)",
                    Math.abs(steak.sellUnit - beef.price) < 1.0e-6, steak.sellUnit + " vs " + beef.price);
        }
        if (ingot != null && rawIron != null && sword != null) {
            check("smelting does not add value", ingot.price == rawIron.price, ingot.price + " / " + rawIron.price);
            check("an iron sword sells for the value of its materials", sword.sellUnit >= 2.0 * ingot.price - 1.0e-6
                    && sword.sellUnit <= 2.0 * ingot.price + 10 && sword.price >= 2 * sword.sellUnit - 1.0e-6,
                    sword.sellUnit + " (shop " + sword.price + ") cost=" + sword.cost + " basis=" + sword.basis
                            + " exact=" + (l.solved != null && l.solved.exact.contains("minecraft:iron_sword"))
                            + " ingotCost=" + ingot.cost + " via=" + (l.solved == null ? null : l.solved.costRecipe.get("minecraft:iron_ingot")));
        }
        ShopLoader.Price stonePick = plain(l, Items.STONE_PICKAXE);
        check("tools made from free materials are not worth crafting for sale", stonePick != null && stonePick.sellUnit <= 20,
                stonePick == null ? null : stonePick.sellUnit + " (shop " + stonePick.price + ")");
        if (bucket != null && milk != null && ingot != null) {
            check("an empty bucket is bought back for three iron ingots", Math.abs(bucket.sellUnit - 3.0 * ingot.price) < 1.0e-6,
                    bucket.sellUnit);
            check("a milk bucket costs the bucket deposit plus a little for the milk",
                    milk.price > bucket.sellUnit && milk.price - bucket.sellUnit <= 20 && milk.sellUnit <= milk.price, milk.price);
        }
        if (sugar != null && cane != null) {
            // (Create 의 맷돌처럼 더 많이 나오는 가공법이 있으면 설탕은 그 원가로 매입한다)
            check("basic ingredients are sold at or below what they cost to make", sugar.price <= cane.price
                    && sugar.sellUnit <= cane.price * com.rtshop.shop.PricingConfig.get().sellRatio + 1.0e-6,
                    sugar.price + " / " + cane.price + " sell " + sugar.sellUnit + " finished " + sugar.finished);
        }
        ShopLoader.Price gold = plain(l, Items.GOLD_INGOT);
        ShopLoader.Price netherite = plain(l, Items.NETHERITE_INGOT);
        if (ingot != null && gold != null && netherite != null) {
            double r = com.rtshop.shop.PricingConfig.get().sellRatio;
            check("iron, gold and netherite ingots sell back for the usual share of their price (ore processing mods don't lower it)",
                    Math.abs(ingot.sellUnit - ingot.price * r) < 1.0e-6 && Math.abs(gold.sellUnit - gold.price * r) < 1.0e-6
                            && Math.abs(netherite.sellUnit - netherite.price * r) < 1.0e-6,
                    ingot.sellUnit + " / " + gold.sellUnit + " / " + netherite.sellUnit);
        }
        ShopData food = ShopServer.shops().get("food");
        if (food != null) {
            int breadIdx = findEntry(food, e -> e.stack.is(Items.BREAD) && !e.stack.hasTag());
            check("the food merchant shows which ingredients a dish is priced from", breadIdx >= 0
                    && !food.entries.get(breadIdx).recipe.isEmpty(),
                    breadIdx < 0 ? "no bread" : food.entries.get(breadIdx).recipe.size() + " parts");
        }
        for (String note : l.notes) {
            info(note);
        }
    }

    private static ShopLoader.Price plain(ShopLoader.Loaded l, Item item) {
        return l.prices.get(ShopLoader.key(new ItemStack(item)));
    }

    // ------------------------------------------------------------------ 경제 보호 장치

    private static void guards(MinecraftServer server, ServerLevel level) {
        section("guards");
        EconomyConfig cfg = EconomyConfig.get();
        IronGolem golem = EntityType.IRON_GOLEM.create(level);
        if (golem != null) {
            boolean guarded = cfg.playerKillOnlyDrops.contains("minecraft:iron_golem");
            LivingDropsEvent farm = new LivingDropsEvent(golem, level.damageSources().lava(), new ArrayList<>(), 0, false);
            MinecraftForge.EVENT_BUS.post(farm);
            check("an iron golem killed by a farm drops nothing", farm.isCanceled() == guarded);
            LivingDropsEvent hunted = new LivingDropsEvent(golem, level.damageSources().generic(), new ArrayList<>(), 0, true);
            MinecraftForge.EVENT_BUS.post(hunted);
            check("an iron golem killed by a player still drops iron", !hunted.isCanceled());
            golem.discard();
        }
        Villager villager = EntityType.VILLAGER.create(level);
        if (villager != null) {
            villager.getOffers().clear();
            villager.getOffers().add(new MerchantOffer(new ItemStack(Items.EMERALD), new ItemStack(Items.BREAD, 6), 16, 1, 0.05F));
            villager.getOffers().add(new MerchantOffer(new ItemStack(Items.EMERALD, 9), new ItemStack(Items.IRON_CHESTPLATE), 12, 1, 0.2F));
            villager.getOffers().add(new MerchantOffer(new ItemStack(Items.STICK, 32), new ItemStack(Items.EMERALD), 16, 2, 0.05F));
            villager.getOffers().add(new MerchantOffer(new ItemStack(Items.EMERALD, 20),
                    EnchantedBookItem.createForEnchantment(new EnchantmentInstance(Enchantments.MENDING, 1)), 12, 1, 0.2F));
            int removed = EconomyGuards.filterOffers(villager);
            boolean breadLeft = false;
            boolean armorLeft = false;
            boolean emeraldLeft = false;
            boolean bookLeft = false;
            for (MerchantOffer o : villager.getOffers()) {
                breadLeft |= o.getResult().is(Items.BREAD);
                armorLeft |= o.getResult().is(Items.IRON_CHESTPLATE);
                emeraldLeft |= o.getResult().is(Items.EMERALD);
                bookLeft |= o.getResult().is(Items.ENCHANTED_BOOK);
            }
            check("villager trades that hand out goods the merchants buy are removed", removed == 2 && !breadLeft && !armorLeft, removed);
            check("villagers still buy goods for emeralds and sell enchanted books", emeraldLeft && bookLeft);
            villager.discard();
        }
        // 이전 버전(물가 1/10)의 저장 데이터는 10배로 환산된다
        UUID id = UUID.randomUUID();
        CompoundTag old = new CompoundTag();
        ListTag accounts = new ListTag();
        CompoundTag acc = new CompoundTag();
        acc.putUUID("id", id);
        acc.putLong("wallet", 123);
        acc.putLong("bank", 45);
        accounts.add(acc);
        old.put("accounts", accounts);
        EconomyData migrated = EconomyData.load(old);
        check("balances saved by RT Shop 2.x are converted to the new price scale (x10)",
                migrated.wallet(id) == 1230 && migrated.bank(id) == 450, migrated.wallet(id) + " / " + migrated.bank(id));
        EconomyData again = EconomyData.load(migrated.save(new CompoundTag()));
        check("...and only once", again.wallet(id) == 1230 && again.bank(id) == 450);
    }

    // ------------------------------------------------------------------ 이전 버전에서 올린 경우

    private static void upgrade(MinecraftServer server) throws Exception {
        section("upgrade");
        Path root = FMLPaths.CONFIGDIR.get().resolve("rtshop");
        Path marker = root.resolve("defaults_version.txt");
        Path dir = ShopLoader.dir();
        check("the installed price table is marked with its version", Files.exists(marker)
                && Files.readString(marker).trim().equals(Integer.toString(ShopLoader.DEFAULTS_VERSION)));
        // RT Shop 2.x 의 상점 폴더를 흉내 낸다: 표시 파일이 없고, 예전 farmer.json 과 사용자가 만든 파일이 있다
        Files.deleteIfExists(marker);
        Files.deleteIfExists(dir.resolve("food.json"));
        Files.deleteIfExists(dir.resolve("land.json"));
        Files.writeString(dir.resolve("farmer.json"), "{\"title\":\"old\",\"items\":[{\"item\":\"minecraft:wheat\",\"price\":4}]}");
        Files.writeString(dir.resolve("my_custom_shop.json"), "{\"title\":\"custom\",\"items\":[]}");
        ShopServer.reload(server);
        Map<String, ShopData> shops = ShopServer.shops();
        boolean all = true;
        for (String name : ShopLoader.DEFAULTS) {
            all &= shops.containsKey(name) && !shops.get(name).entries.isEmpty();
        }
        check("upgrading from 2.x installs the new shops (food and land merchants included)", all, shops.keySet());
        ShopLoader.Price wheat = plain(ShopServer.loaded(), Items.WHEAT);
        check("...with the new price table instead of the old one", wheat != null && wheat.price > 4, wheat == null ? null : wheat.price);
        boolean backedUp = false;
        Path backup = root.resolve("backup");
        if (Files.exists(backup)) {
            try (Stream<Path> st = Files.walk(backup)) {
                backedUp = st.anyMatch(f -> f.getFileName().toString().equals("my_custom_shop.json"));
            }
        }
        check("...and keeps the old files in config/rtshop/backup", backedUp && !Files.exists(dir.resolve("my_custom_shop.json")));
        check("the upgrade happens only once", Files.exists(marker));
    }

    // ------------------------------------------------------------------ 정리

    private static void cleanup(MinecraftServer server, ServerLevel level, FakePlayer a, FakePlayer b, int gx, int y, int gz) {
        LandData data = LandData.get(server);
        for (FakePlayer p : new FakePlayer[]{a, b}) {
            for (LandData.Land l : new ArrayList<>(data.landsOf(p.getUUID()))) {
                data.removeLand(l);
            }
            data.clearFirstDeed(p.getUUID());
            clear(p);
            EconomyData.get(server).removeAccount(p.getUUID());
            ShopServer.closeSession(p);
        }
        LandManager.clearRuntime();
        for (int dx = 0; dx < 32; dx++) {
            for (int dz = 0; dz < 32; dz++) {
                for (int dy = -1; dy <= 3; dy++) {
                    BlockPos p = new BlockPos(gx + dx, y + dy, gz + dz);
                    if (!level.getBlockState(p).isAir()) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                    CropData.get(level).unmark(CropData.dim(level), p);
                }
            }
        }
        takeDrops(level, new BlockPos(gx + 16, y, gz + 16));
    }

    // ------------------------------------------------------------------ 출석

    private static void attendance(MinecraftServer server, ServerLevel level, FakePlayer a) throws Exception {
        section("attendance");
        com.rtshop.attendance.AttendanceConfig c = com.rtshop.attendance.AttendanceConfig.get();
        check("attendance.json is created with a 28-day reward table and weekly tiers", c.days.size() == 28 && c.weekly.size() == 3
                && c.timezone.equals("Asia/Seoul") && c.resetHour == 6, c.days.size() + " " + c.weekly.size());
        List<String> bad = new ArrayList<>();
        for (com.rtshop.attendance.AttendanceConfig.Reward r : c.days) {
            for (com.rtshop.attendance.AttendanceConfig.Item it : r.items) {
                if (ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(it.item())) == null
                        || ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(it.item())) == Items.AIR) {
                    bad.add(it.item());
                }
            }
        }
        check("every default attendance reward item exists", bad.isEmpty(), bad);
        com.rtshop.attendance.AttendanceConfig again = com.rtshop.attendance.AttendanceConfig.fromJson(c.toJson());
        check("attendance settings survive saving", again.days.size() == c.days.size() && again.weekly.get(2).days == 7
                && again.days.get(27).items.get(0).item().equals("rtshop:land_deed") && again.days.get(6).special);
        boolean rejected;
        try {
            com.google.gson.JsonObject broken = c.toJson();
            broken.addProperty("resetHour", 30);
            com.rtshop.attendance.AttendanceConfig.fromJson(broken);
            rejected = false;
        } catch (IllegalArgumentException e) {
            rejected = true;
        }
        check("a wrong attendance setting is refused with a reason", rejected);
        // 3.7.0 이 만든 설정 파일(오전 9시, configVersion 없음)은 오전 6시로 옮겨지고 보상은 그대로
        java.nio.file.Path ap = com.rtshop.attendance.AttendanceConfig.path();
        byte[] apOrig = Files.readAllBytes(ap);
        try {
            com.google.gson.JsonObject old = c.toJson();
            old.remove("configVersion");
            old.addProperty("resetHour", 9);
            old.getAsJsonArray("days").get(0).getAsJsonObject().addProperty("gold", 1234);
            Files.writeString(ap, old.toString());
            com.rtshop.attendance.AttendanceConfig.reload();
            com.rtshop.attendance.AttendanceConfig m = com.rtshop.attendance.AttendanceConfig.get();
            check("a 3.7.0 attendance file moves from 09:00 to 06:00 and keeps its rewards", m.resetHour == 6 && m.days.get(0).gold == 1234
                    && Files.readString(ap).contains("configVersion"), m.resetHour + " " + m.days.get(0).gold);
            com.google.gson.JsonObject chosen = m.toJson();
            chosen.addProperty("resetHour", 9);
            Files.writeString(ap, chosen.toString());
            com.rtshop.attendance.AttendanceConfig.reload();
            check("...but 09:00 chosen later in the editor is kept", com.rtshop.attendance.AttendanceConfig.get().resetHour == 9);
        } finally {
            Files.write(ap, apOrig);
            com.rtshop.attendance.AttendanceConfig.reload();
            c = com.rtshop.attendance.AttendanceConfig.get();
        }

        // 날짜: 한국 시간 오전 6시에 바뀐다
        java.time.ZoneId kst = java.time.ZoneId.of("Asia/Seoul");
        long t0859 = java.time.ZonedDateTime.of(2026, 11, 23, 5, 59, 0, 0, kst).toInstant().toEpochMilli();
        long t0900 = java.time.ZonedDateTime.of(2026, 11, 23, 6, 0, 0, 0, kst).toInstant().toEpochMilli();
        long base = com.rtshop.attendance.Attendance.dayOf(t0900, c);
        check("the attendance day changes at 06:00 Korean time (not at 09:00)", com.rtshop.attendance.Attendance.dayOf(t0859, c) == base - 1
                && com.rtshop.attendance.Attendance.dayStart(base, c) == t0900
                && com.rtshop.attendance.Attendance.dayOf(t0900 + 3 * 3_600_000L - 1, c) == base);
        check("the week starts on Monday 06:00 (2026-11-23 is a Monday)", com.rtshop.attendance.Attendance.weekOf(base, c) == base
                && com.rtshop.attendance.Attendance.weekOf(base + 6, c) == base && com.rtshop.attendance.Attendance.weekOf(base + 7, c) == base + 7);
        long H = 3_600_000L;
        java.util.function.LongUnaryOperator day = n -> t0900 + n * 24 * H + 6 * H; // n 일 뒤 정오
        com.rtshop.attendance.AttendanceData.Entry e = new com.rtshop.attendance.AttendanceData.Entry();
        var ci = com.rtshop.attendance.Attendance.checkIn(e, day.applyAsLong(0), c);
        check("first check-in: day 1 reward", ci != null && ci.dayIndex() == 0 && e.streak == 1);
        check("checking in twice on one day is refused", com.rtshop.attendance.Attendance.checkIn(e, day.applyAsLong(0) + 5 * H, c) == null);
        com.rtshop.attendance.Attendance.evaluate(e, day.applyAsLong(1), c);
        com.rtshop.attendance.Attendance.checkIn(e, day.applyAsLong(1), c);
        ci = com.rtshop.attendance.Attendance.checkIn(e, day.applyAsLong(2), c);
        check("three days in a row → day 3 and the 3-day weekly reward", e.streak == 3 && ci != null && ci.weekly().size() == 1
                && ci.weekly().get(0).days == 3, e.streak);
        // 하루 빠짐 (day 3) → 처음 한 번은 유지
        var ch = com.rtshop.attendance.Attendance.evaluate(e, day.applyAsLong(4), c);
        check("missing one day the first time keeps the streak (with a warning)", ch == com.rtshop.attendance.Attendance.Change.GRACE
                && e.streak == 3 && e.graceUsed);
        com.rtshop.attendance.Attendance.checkIn(e, day.applyAsLong(4), c);
        check("...and the next check-in continues it", e.streak == 4);
        // 또 하루 빠짐 (day 5) → 초기화
        ch = com.rtshop.attendance.Attendance.evaluate(e, day.applyAsLong(6), c);
        check("after that, missing a single day resets to day 0", ch == com.rtshop.attendance.Attendance.Change.RESET
                && e.streak == 0 && e.savedStreak == 4 && e.resetAt == com.rtshop.attendance.Attendance.dayStart(base + 6, c));
        com.rtshop.attendance.Attendance.checkIn(e, day.applyAsLong(6), c);
        var rr = com.rtshop.attendance.Attendance.restore(e, day.applyAsLong(6), c);
        check("a restore ticket within 24 h brings the streak back (+ today's check-in)", rr == com.rtshop.attendance.Attendance.RestoreResult.OK
                && e.streak == 5, rr + " " + e.streak);
        check("the restored streak keeps going the next day", com.rtshop.attendance.Attendance.evaluate(e, day.applyAsLong(7), c)
                == com.rtshop.attendance.Attendance.Change.NONE && com.rtshop.attendance.Attendance.checkIn(e, day.applyAsLong(7), c) != null && e.streak == 6);
        // 이틀 빠짐 → 초기화, 그리고 24시간이 지나면 복구 불가
        com.rtshop.attendance.AttendanceData.Entry f = new com.rtshop.attendance.AttendanceData.Entry();
        for (int n = 0; n < 5; n++) {
            com.rtshop.attendance.Attendance.evaluate(f, day.applyAsLong(n), c);
            com.rtshop.attendance.Attendance.checkIn(f, day.applyAsLong(n), c);
        }
        ch = com.rtshop.attendance.Attendance.evaluate(f, day.applyAsLong(7), c);
        check("missing two days resets at once even with the free miss unused", ch == com.rtshop.attendance.Attendance.Change.RESET
                && f.savedStreak == 5 && f.resetAt == com.rtshop.attendance.Attendance.dayStart(base + 7, c));
        check("the restore window closes 24 h after the reset", com.rtshop.attendance.Attendance.restore(f, f.resetAt + 24 * H, c)
                == com.rtshop.attendance.Attendance.RestoreResult.EXPIRED);
        com.rtshop.attendance.AttendanceData.Entry g = new com.rtshop.attendance.AttendanceData.Entry();
        com.rtshop.attendance.Attendance.checkIn(g, day.applyAsLong(0), c);
        com.rtshop.attendance.Attendance.checkIn(g, day.applyAsLong(1), c);
        com.rtshop.attendance.Attendance.evaluate(g, day.applyAsLong(10), c);
        check("coming back long after the reset cannot be restored", com.rtshop.attendance.Attendance.restore(g, day.applyAsLong(10), c)
                == com.rtshop.attendance.Attendance.RestoreResult.EXPIRED);
        // 쿨타임
        com.rtshop.attendance.AttendanceData.Entry k = new com.rtshop.attendance.AttendanceData.Entry();
        k.savedStreak = 9;
        k.resetAt = day.applyAsLong(20);
        k.restoreAt = day.applyAsLong(20) - 2 * H;
        check("a ticket cannot be used again within 24 h", com.rtshop.attendance.Attendance.restore(k, day.applyAsLong(20) + H, c)
                == com.rtshop.attendance.Attendance.RestoreResult.COOLDOWN);
        com.rtshop.attendance.AttendanceData.Entry none = new com.rtshop.attendance.AttendanceData.Entry();
        check("a ticket does nothing when there is nothing to restore", com.rtshop.attendance.Attendance.restore(none, day.applyAsLong(0), c)
                == com.rtshop.attendance.Attendance.RestoreResult.NOTHING);
        // 28일 뒤에는 1일차부터
        check("after day 28 the rewards start again from day 1", com.rtshop.attendance.Attendance.rewardIndex(29, c) == 0
                && com.rtshop.attendance.Attendance.rewardIndex(28, c) == 27);
        // 새 주에는 주간 출석이 새로 시작
        com.rtshop.attendance.AttendanceData.Entry w = new com.rtshop.attendance.AttendanceData.Entry();
        for (int n = 0; n < 7; n++) {
            com.rtshop.attendance.Attendance.checkIn(w, day.applyAsLong(n), c);
        }
        check("seven days in one week earn all three weekly rewards", Integer.bitCount(w.weekClaimed) == 3 && w.weekCount() == 7);
        com.rtshop.attendance.Attendance.checkIn(w, day.applyAsLong(7), c);
        check("a new week starts the weekly count again", w.weekCount() == 1 && w.weekClaimed == 0);

        // 실제 플레이어: 출석(보상 지급) → 하루 건너뛰기 → 초기화 → 복구권
        java.util.function.LongSupplier oldClock = com.rtshop.attendance.Attendance.clock;
        try {
            com.rtshop.attendance.Attendance.reset(a);
            clear(a);
            Economy.setWallet(a, 0);
            com.rtshop.attendance.Attendance.clock = () -> day.applyAsLong(0);
            check("players can't check in without the attendance NPC", !com.rtshop.attendance.Attendance.doCheckIn(a, true));
            check("checking in gives the day-1 reward", com.rtshop.attendance.Attendance.doCheckIn(a, false)
                    && Economy.wallet(a) == c.days.get(0).gold, Economy.wallet(a));
            com.rtshop.attendance.Attendance.clock = () -> day.applyAsLong(1);
            com.rtshop.attendance.Attendance.doCheckIn(a, false);
            check("day 2 gives its item", count(a, s -> s.is(Items.BREAD)) == 8);
            com.rtshop.attendance.Attendance.setStreak(a, 20); // 어제(0일째)까지 20일 연속으로 친다
            com.rtshop.attendance.Attendance.clock = () -> day.applyAsLong(2); // 1일째를 빠짐 → 봐주기
            check("the first missed day is forgiven for a real player too", com.rtshop.attendance.Attendance.refresh(a)
                    == com.rtshop.attendance.Attendance.Change.GRACE);
            com.rtshop.attendance.Attendance.clock = () -> day.applyAsLong(3); // 2일째도 빠짐 → 초기화
            check("the reset is applied when the player shows up", com.rtshop.attendance.Attendance.refresh(a) == com.rtshop.attendance.Attendance.Change.RESET);
            ItemStack ticket = new ItemStack(ModItems.RESTORE_TICKET.get(), 2);
            a.setItemInHand(InteractionHand.MAIN_HAND, ticket);
            ModItems.RESTORE_TICKET.get().use(level, a, InteractionHand.MAIN_HAND);
            com.rtshop.attendance.AttendanceData.Entry ae = com.rtshop.attendance.AttendanceData.get(server).of(a.getUUID());
            check("right-clicking a restore ticket restores the streak and uses one ticket", ae.streak == 20
                    && a.getMainHandItem().getCount() == 1, ae.streak + " " + a.getMainHandItem().getCount());
            ModItems.RESTORE_TICKET.get().use(level, a, InteractionHand.MAIN_HAND);
            check("a ticket that can't be used is not consumed", a.getMainHandItem().getCount() == 1);
        } finally {
            com.rtshop.attendance.Attendance.clock = oldClock;
            com.rtshop.attendance.Attendance.reset(a);
            clear(a);
        }

        // 편집은 OP 만
        check("players who are not OP can't edit the attendance rewards", !com.rtshop.attendance.Attendance.canEdit(a)
                && !com.rtshop.attendance.Attendance.view(a, -1).get("canEdit").getAsBoolean());
        {
            com.google.gson.JsonObject hacked = com.rtshop.attendance.AttendanceConfig.get().toJson();
            hacked.getAsJsonArray("days").get(0).getAsJsonObject().addProperty("gold", 999_999);
            com.rtshop.attendance.Attendance.handleAction(a, com.rtshop.net.AttendanceActionPacket.SAVE, hacked.toString());
            check("...and a save sent by a non-OP is ignored", com.rtshop.attendance.AttendanceConfig.get().days.get(0).gold != 999_999);
        }

        // 출석 상점: 복구권을 높은 값에 팔고, 되사지 않는다
        ShopData shop = ShopServer.shops().get("attendance_shop");
        check("the attendance shop is installed for existing servers too", shop != null);
        if (shop != null) {
            int idx = findEntry(shop, en -> en.stack.is(ModItems.RESTORE_TICKET.get()));
            check("the attendance shop sells the restore ticket at a high price and never buys it back", idx >= 0
                    && shop.entries.get(idx).price >= 20_000 && !shop.entries.get(idx).sellable, idx < 0 ? "missing" : shop.entries.get(idx).price);
        }
        BlockPos at = level.getSharedSpawnPos().above(6);
        level.getChunkAt(at);
        net.minecraft.world.entity.npc.Villager v = ShopServer.spawnNpc(level, Vec3.atBottomCenterOf(at), 0, "attendance");
        check("the attendance NPC can be placed with /rtshop npc attendance", v != null && v.getTags().contains(com.rtshop.attendance.Attendance.NPC_TAG)
                && v.getCustomName() != null && v.getCustomName().getString().equals(c.npcName));
        check("the attendance NPC also takes part in quests", v != null && "attendance".equals(QuestManager.npcKey(v)));
        if (v != null) {
            v.discard();
        }
    }

    // ------------------------------------------------------------------ 뽑기

    private static void npcGuard(ServerLevel level, FakePlayer a) {
        section("npc guard");
        for (String type : new String[]{"fisher", "bank", "job", "attendance", "gacha"}) {
            net.minecraft.world.entity.npc.Villager v = ShopServer.spawnNpc(level, a.position().add(3, 0, 0), 0, type);
            if (v == null) {
                check(type + " NPC spawns", false);
                continue;
            }
            float hp = v.getHealth();
            v.hurt(level.damageSources().playerAttack(a), 1000);
            v.hurt(level.damageSources().magic(), 1000);
            v.hurt(level.damageSources().explosion(null, null), 1000);
            v.setRemainingFireTicks(100);
            check(type + " NPC takes no damage and does not die", v.isAlive() && v.getHealth() >= hp, v.getHealth());
            v.setHealth(1);
            v.hurt(level.damageSources().magic(), 5);
            check(type + " NPC cannot be killed by damage that skips the hurt events", v.isAlive(), v.getHealth());
            v.discard();
        }
    }

    private static boolean ownedByReflection(net.minecraft.world.entity.Entity e, FakePlayer a) {
        try {
            Object o = e.getClass().getMethod("ownerUuid").invoke(e);
            return o instanceof java.util.Optional<?> op && op.isPresent() && op.get().equals(a.getUUID());
        } catch (Exception ex) {
            return false;
        }
    }

    /** 예전 뽑기 설정(1판)을 새 규칙으로 바꾸는지: 이름·확률은 남기고 목록만 다시 만든다. */
    private static void gachaUpgrade(MinecraftServer server) throws Exception {
        java.nio.file.Path p = com.rtshop.gacha.GachaConfig.path();
        String saved = Files.readString(p, StandardCharsets.UTF_8);
        try {
            com.google.gson.JsonObject o = com.google.gson.JsonParser.parseString(saved).getAsJsonObject();
            o.addProperty("configVersion", 1);
            o.addProperty("npcName", "옛 이름");
            o.getAsJsonObject("tiers").getAsJsonObject("mid").addProperty("epic", 2.5);
            com.google.gson.JsonArray pets = new com.google.gson.JsonArray();
            com.google.gson.JsonObject wolf = new com.google.gson.JsonObject();
            wolf.addProperty("entity", "minecraft:wolf");
            wolf.addProperty("grade", "common");
            wolf.addProperty("weight", 10);
            pets.add(wolf);
            o.getAsJsonObject("pools").add("pet", pets);
            o.getAsJsonObject("shards").remove("tickets");
            Files.writeString(p, o.toString(), StandardCharsets.UTF_8);
            com.rtshop.gacha.GachaConfig.reload(server);
            com.rtshop.gacha.GachaConfig c = com.rtshop.gacha.GachaConfig.get();
            boolean petsOk = c.pool("pet").stream().noneMatch(e -> e.entity.equals("minecraft:wolf") && e.grade.equals("common"))
                    && c.pool("pet").stream().anyMatch(com.rtshop.gacha.Gacha::isShardEntry);
            check("an old gacha.json is upgraded: name and odds kept, lists rebuilt with the new rules", c.version == com.rtshop.gacha.GachaConfig.CONFIG_VERSION
                    && c.npcName.equals("옛 이름") && c.tiers.get("mid").weights[3] == 2.5 && petsOk && c.ticketCost[0][2] == 100,
                    c.version + " " + c.npcName + " " + petsOk);
            String now = Files.readString(p, StandardCharsets.UTF_8);
            check("...and saved as the new version", now.contains("\"configVersion\": " + com.rtshop.gacha.GachaConfig.CONFIG_VERSION));
        } finally {
            Files.writeString(p, saved, StandardCharsets.UTF_8);
            com.rtshop.gacha.GachaConfig.reload(server);
        }
    }

    private static void gacha(MinecraftServer server, ServerLevel level, FakePlayer a) throws Exception {
        section("gacha");
        gachaUpgrade(server);
        com.rtshop.gacha.GachaConfig c = com.rtshop.gacha.GachaConfig.get();
        int nw = c.pool("weapon").size();
        int np = c.pool("pet").size();
        int na = c.pool("accessory").size();
        info("gacha pools: weapons " + nw + ", pets " + np + ", accessories " + na);
        check("gacha.json is filled automatically (weapons and pets from the game)", nw >= 10 && np >= 3, nw + " / " + np + " / " + na);
        boolean fullPack = net.minecraftforge.fml.ModList.get().isLoaded("cataclysm");
        List<String> boss = new ArrayList<>();
        for (String cat : com.rtshop.gacha.GachaConfig.CATEGORIES) {
            for (com.rtshop.gacha.GachaConfig.Entry e : c.pool(cat)) {
                String id = e.entity.isEmpty() ? e.item : e.entity;
                if (id.startsWith("cataclysm:") || c.excludeItems.contains(id)) {
                    boss.add(id);
                }
            }
        }
        check("boss weapons (Cataclysm etc.) are left out of the gacha", boss.isEmpty(), boss);
        // 펫: 일반·고급은 조각, 펫은 희귀 이상
        List<String> lowPets = new ArrayList<>();
        int shardRows = 0;
        for (com.rtshop.gacha.GachaConfig.Entry e : c.pool("pet")) {
            int gi = com.rtshop.gacha.GachaConfig.gradeIndex(e.grade);
            if (com.rtshop.gacha.Gacha.isShardEntry(e)) {
                shardRows++;
                if (gi > 1) {
                    lowPets.add("shard@" + e.grade);
                }
            } else if (gi < 2) {
                lowPets.add(e.entity + "@" + e.grade);
            }
        }
        check("pets are only rare or better; common/uncommon pet pulls give shards", shardRows == 2 && lowPets.isEmpty(), lowPets);
        if (fullPack) {
            List<String> bad = new ArrayList<>();
            int hats = 0;
            for (com.rtshop.gacha.GachaConfig.Entry e : c.pool("accessory")) {
                int gi = com.rtshop.gacha.GachaConfig.gradeIndex(e.grade);
                if (e.item.startsWith("simplehats:")) {
                    hats++;
                    if (gi > 2) {
                        bad.add(e.item);
                    }
                } else if (e.item.startsWith("artifacts:") ? gi < 3 : gi > 2) {
                    bad.add(e.item + "@" + e.grade);
                }
            }
            check("accessories: cosmetic hats are common..rare, Artifacts epic/legendary, others at most rare", na >= 10 && hats > 0 && bad.isEmpty(),
                    hats + " hats, bad " + bad);
            List<String> badPets = new ArrayList<>();
            for (com.rtshop.gacha.GachaConfig.Entry e : c.pool("pet")) {
                if ((e.entity.startsWith("adorablehamsterpets:") || e.entity.startsWith("rpg_companions_tiny_dragons:")) && !e.grade.equals("epic")) {
                    badPets.add(e.entity + "@" + e.grade);
                }
            }
            check("hamsters and tiny dragons are epic pets", badPets.isEmpty(), badPets);
            List<String> badW = new ArrayList<>();
            int topN = 0;
            for (com.rtshop.gacha.GachaConfig.Entry e : c.pool("weapon")) {
                int gi = com.rtshop.gacha.GachaConfig.gradeIndex(e.grade);
                String ns = e.item.substring(0, e.item.indexOf(':'));
                String nbt = e.nbt.toLowerCase(java.util.Locale.ROOT);
                boolean lr = e.item.equals("lrtactical:melee");
                if (gi >= 3) {
                    topN++;
                    if (!ns.equals("efn") && !(lr && nbt.contains("karambit"))) {
                        badW.add(e.item + "@" + e.grade);
                    }
                } else if (!ns.equals("epicfight") && !(lr && (nbt.contains("baseball") || nbt.contains("dagger")) && gi == 2)) {
                    badW.add(e.item + "@" + e.grade);
                }
            }
            if (c.pool("weapon").stream().filter(e -> e.item.startsWith("efn:")).count() != 12) {
                badW.add("efn count " + c.pool("weapon").stream().filter(e -> e.item.startsWith("efn:")).count());
            }
            java.util.Map<String, String> want = java.util.Map.of("efn:hf_murasama", "legendary", "efn:yamato_dmc_in_sheath", "legendary",
                    "efn:kusabimaru", "epic", "efn:ruinsgreatsword", "epic");
            for (var w : want.entrySet()) {
                if (c.pool("weapon").stream().noneMatch(e -> e.item.equals(w.getKey()) && e.grade.equals(w.getValue()))) {
                    badW.add("missing " + w.getKey());
                }
            }
            check("weapons: only the 12 fixed Nightfall weapons and the karambit are epic/legendary, the rest are Epic Fight base weapons", topN > 0 && badW.isEmpty(), badW);
            StringBuilder sb = new StringBuilder();
            for (String cat : com.rtshop.gacha.GachaConfig.CATEGORIES) {
                int[] g = new int[5];
                for (com.rtshop.gacha.GachaConfig.Entry e : c.pool(cat)) {
                    g[com.rtshop.gacha.GachaConfig.gradeIndex(e.grade)]++;
                }
                sb.append(cat).append(java.util.Arrays.toString(g)).append(' ');
                List<String> top = new ArrayList<>();
                for (com.rtshop.gacha.GachaConfig.Entry e : c.pool(cat)) {
                    if (e.grade.equals("legendary") || e.grade.equals("epic")) {
                        top.add(e.entity.isEmpty() ? e.item : e.entity);
                    }
                }
                info("gacha " + cat + " epic+legendary (" + top.size() + "): " + String.join(" ", top.subList(0, Math.min(40, top.size()))));
                java.util.Map<String, Integer> ns = new java.util.TreeMap<>();
                for (com.rtshop.gacha.GachaConfig.Entry e : c.pool(cat)) {
                    String id = e.entity.isEmpty() ? e.item : e.entity;
                    ns.merge(id.substring(0, id.indexOf(':')), 1, Integer::sum);
                }
                info("gacha " + cat + " mods: " + ns);
                if (cat.equals("pet")) {
                    List<String> all = new ArrayList<>();
                    for (com.rtshop.gacha.GachaConfig.Entry e : c.pool(cat)) {
                        all.add(e.entity + "=" + e.grade);
                    }
                    info("gacha pets: " + String.join(" ", all));
                }
            }
            info("gacha grades: " + sb);
        }
        double[] hw = com.rtshop.gacha.Gacha.gradeChances(c, "weapon", "high");
        double sum = 0;
        for (double x : hw) {
            sum += x;
        }
        check("grade odds add up to 100% and legendary is rare", Math.abs(sum - 1) < 1e-9 && hw[4] <= 0.0031 && hw[3] <= 0.033,
                java.util.Arrays.toString(hw));
        com.rtshop.gacha.GachaConfig again = com.rtshop.gacha.GachaConfig.fromJson(c.toJson());
        check("gacha settings survive saving", again.pool("weapon").size() == nw && again.tiers.get("high").weights[4] == c.tiers.get("high").weights[4]);
        boolean refused;
        try {
            com.google.gson.JsonObject broken = c.toJson();
            broken.getAsJsonObject("pools").getAsJsonArray("weapon").get(0).getAsJsonObject().addProperty("grade", "mythic");
            com.rtshop.gacha.GachaConfig.fromJson(broken);
            refused = false;
        } catch (IllegalArgumentException e) {
            refused = true;
        }
        check("an unknown grade in gacha.json is refused", refused);

        // 시험용 목록: 등급마다 하나씩
        com.google.gson.JsonObject tj = c.toJson();
        com.google.gson.JsonArray tp = new com.google.gson.JsonArray();
        String[] ids = {"minecraft:wooden_sword", "minecraft:stone_sword", "minecraft:iron_sword", "minecraft:diamond_sword", "minecraft:netherite_sword"};
        for (int i = 0; i < 5; i++) {
            com.google.gson.JsonObject e = new com.google.gson.JsonObject();
            e.addProperty("item", ids[i]);
            e.addProperty("count", 1);
            e.addProperty("grade", com.rtshop.gacha.GachaConfig.GRADES[i]);
            e.addProperty("weight", 10);
            tp.add(e);
        }
        tj.getAsJsonObject("pools").add("weapon", tp);
        com.rtshop.gacha.GachaConfig t = com.rtshop.gacha.GachaConfig.fromJson(tj);
        net.minecraft.util.RandomSource rng = net.minecraft.util.RandomSource.create(42);
        int[] counts = new int[5];
        int n = 40_000;
        for (int k = 0; k < n; k++) {
            counts[com.rtshop.gacha.GachaConfig.gradeIndex(com.rtshop.gacha.Gacha.pull(t, "weapon", "high", 1, rng).get(0).grade)]++;
        }
        double[] want = com.rtshop.gacha.Gacha.gradeChances(t, "weapon", "high");
        boolean close = true;
        for (int g = 0; g < 5; g++) {
            close &= Math.abs(counts[g] / (double) n - want[g]) < 0.01;
        }
        check("pulls follow the published odds (40,000 simulated pulls)", close, java.util.Arrays.toString(counts));
        boolean noRareInNormalSingles = true;
        com.google.gson.JsonObject nj = t.toJson();
        nj.getAsJsonObject("tiers").getAsJsonObject("normal").addProperty("rare", 0);
        com.rtshop.gacha.GachaConfig t2 = com.rtshop.gacha.GachaConfig.fromJson(nj);
        for (int k = 0; k < 2000; k++) {
            noRareInNormalSingles &= com.rtshop.gacha.GachaConfig.gradeIndex(com.rtshop.gacha.Gacha.pull(t2, "weapon", "normal", 1, rng).get(0).grade) < 2;
        }
        boolean guaranteed = true;
        for (int k = 0; k < 200; k++) {
            int best = 0;
            for (var e : com.rtshop.gacha.Gacha.pull(t2, "weapon", "normal", 10, rng)) {
                best = Math.max(best, com.rtshop.gacha.GachaConfig.gradeIndex(e.grade));
            }
            guaranteed &= best >= 2;
        }
        check("10 pulls always include a rare or better, even when single pulls can't give one", noRareInNormalSingles && guaranteed);

        // 실제 플레이어
        clear(a);
        a.getInventory().add(new ItemStack(ModItems.TICKET_NORMAL.get(), 12));
        check("pulling needs the gacha merchant", com.rtshop.gacha.Gacha.doPull(a, "weapon", "normal", 1, true) == null);
        var got = com.rtshop.gacha.Gacha.doPull(a, "weapon", "normal", 10, false);
        long marked = count(a, com.rtshop.gacha.Gacha::marked);
        check("a 10-pull uses 10 tickets and gives 10 marked items", got != null && got.size() == 10
                && count(a, st -> st.is(ModItems.TICKET_NORMAL.get())) == 2 && marked >= 1, (got == null ? "null" : got.size()) + " " + marked);
        check("with 2 tickets left a 10-pull is refused", com.rtshop.gacha.Gacha.doPull(a, "weapon", "normal", 10, false) == null
                && count(a, st -> st.is(ModItems.TICKET_NORMAL.get())) == 2);
        ItemStack sample = ItemStack.EMPTY;
        for (ItemStack st : a.getInventory().items) {
            if (com.rtshop.gacha.Gacha.marked(st)) {
                sample = st;
                break;
            }
        }
        boolean unsellable = !sample.isEmpty();
        for (ShopData d : ShopServer.shops().values()) {
            unsellable &= d.findSellEntry(sample) < 0;
        }
        check("gacha items can't be sold to any shop", unsellable, sample);
        int shards = com.rtshop.gacha.Gacha.dismantleAll(a, 4, false);
        check("dismantling gives shards and removes the items", shards > 0 && count(a, com.rtshop.gacha.Gacha::marked) == 0
                && count(a, st -> st.is(ModItems.SHARD.get())) == shards, shards);
        a.getInventory().add(new ItemStack(ModItems.SHARD.get(), 64));
        a.getInventory().add(new ItemStack(ModItems.SHARD.get(), 64));
        long before = count(a, st -> st.is(ModItems.SHARD.get()));
        boolean ex = com.rtshop.gacha.Gacha.exchange(a, "pet", "mid", 2, false);
        check("shards can be traded for gacha tickets", ex && count(a, st -> st.is(ModItems.PET_TICKET_MID.get())) == 2
                && before - count(a, st -> st.is(ModItems.SHARD.get())) == 2L * c.ticketCost[1][1], before);
        check("...but not without enough shards", !com.rtshop.gacha.Gacha.exchange(a, "weapon", "high", 64, false));
        clear(a);
        // 펫 뽑기의 조각 보상은 표시 없는 조각
        a.getInventory().add(new ItemStack(ModItems.PET_TICKET_NORMAL.get(), 10));
        a.getInventory().add(new ItemStack(ModItems.TICKET_NORMAL.get(), 10));
        check("a weapon ticket can't be used for the pet gacha", com.rtshop.gacha.Gacha.doPull(a, "pet", "mid", 1, false) == null);
        var pets = com.rtshop.gacha.Gacha.doPull(a, "pet", "normal", 10, false);
        long shardGot = count(a, st -> st.is(ModItems.SHARD.get()));
        long expect = 0;
        if (pets != null) {
            for (var e : pets) {
                if (com.rtshop.gacha.Gacha.isShardEntry(e)) {
                    expect += e.count;
                }
            }
        }
        check("pet tickets pull pets; low-grade results arrive as plain shards", pets != null && pets.size() == 10 && shardGot == expect
                && count(a, st -> st.is(ModItems.PET_TICKET_NORMAL.get())) == 0 && count(a, st -> st.is(ModItems.TICKET_NORMAL.get())) == 10,
                shardGot + " / " + expect);
        clear(a);
        // 펫 소환권
        BlockPos at = level.getSharedSpawnPos().above(8);
        level.getChunkAt(at);
        List<String> petFails = new ArrayList<>();
        int tested = 0;
        for (com.rtshop.gacha.GachaConfig.Entry e : c.pool("pet")) {
            if (com.rtshop.gacha.Gacha.isShardEntry(e)) {
                continue;
            }
            ItemStack st = com.rtshop.gacha.Gacha.stackOf(e, "pet");
            net.minecraft.world.entity.Entity pet = com.rtshop.item.PetSummonItem.summon(a, st, Vec3.atBottomCenterOf(at));
            tested++;
            boolean owned = pet instanceof net.minecraft.world.entity.TamableAnimal ta ? a.getUUID().equals(ta.getOwnerUUID())
                    : pet instanceof net.minecraft.world.entity.animal.horse.AbstractHorse h ? h.isTamed()
                    : pet != null && ownedByReflection(pet, a);
            if (!owned) {
                petFails.add(e.entity);
            }
            if (pet != null) {
                pet.discard();
            }
        }
        check("every pet in the gacha appears tamed to the player (" + tested + " pets)", tested > 0 && petFails.isEmpty(), petFails);
        clear(a);
        // 편집은 OP 만
        check("players who are not OP can't edit the gacha", !com.rtshop.gacha.Gacha.canEdit(a)
                && !com.rtshop.gacha.Gacha.view(a).get("canEdit").getAsBoolean());
        com.google.gson.JsonObject hack = new com.google.gson.JsonObject();
        com.google.gson.JsonObject hj = c.toJson();
        hj.addProperty("npcName", "hacked");
        hack.addProperty("json", hj.toString());
        com.rtshop.gacha.Gacha.handleAction(a, com.rtshop.net.GachaActionPacket.SAVE, hack.toString());
        check("...and a save sent by a non-OP is ignored", !com.rtshop.gacha.GachaConfig.get().npcName.equals("hacked"));
        // 티켓 상점
        ShopData shop = ShopServer.shops().get("gacha_shop");
        check("the ticket shop is installed", shop != null);
        if (shop != null) {
            int i1 = findEntry(shop, en -> en.stack.is(ModItems.TICKET_NORMAL.get()));
            int i2 = findEntry(shop, en -> en.stack.is(ModItems.TICKET_MID.get()));
            int i3 = findEntry(shop, en -> en.stack.is(ModItems.TICKET_HIGH.get()));
            check("weapon tickets cost 1,000 / 5,000 / 20,000 G and can't be sold back", i1 >= 0 && i2 >= 0 && i3 >= 0
                    && shop.entries.get(i1).price == 1000 && shop.entries.get(i2).price == 5000 && shop.entries.get(i3).price == 20000
                    && !shop.entries.get(i3).sellable);
            int p1 = findEntry(shop, en -> en.stack.is(ModItems.PET_TICKET_NORMAL.get()));
            int p3 = findEntry(shop, en -> en.stack.is(ModItems.PET_TICKET_HIGH.get()));
            int a1 = findEntry(shop, en -> en.stack.is(ModItems.ACC_TICKET_NORMAL.get()));
            int a3 = findEntry(shop, en -> en.stack.is(ModItems.ACC_TICKET_HIGH.get()));
            check("pet and accessory tickets are 25% cheaper (750 / 15,000 G)", p1 >= 0 && p3 >= 0 && a1 >= 0 && a3 >= 0
                    && shop.entries.get(p1).price == 750 && shop.entries.get(p3).price == 15000
                    && shop.entries.get(a1).price == 750 && shop.entries.get(a3).price == 15000);
        }
        net.minecraft.world.entity.npc.Villager v = ShopServer.spawnNpc(level, Vec3.atBottomCenterOf(at), 0, "gacha");
        check("the gacha merchant can be placed with /rtshop npc gacha", v != null && "gacha".equals(QuestManager.npcKey(v)));
        if (v != null) {
            v.discard();
        }
    }
}

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

    /** 기록이 쌓이면 칭호가 열리고, 효과는 둘까지·내거는 것은 하나, 한 주가 끝나면 윗자리에 보상이 맡겨진다. */
    /**
     * 3.72: 조합 증서 (주식) — 시세(실적 · 소식 · 하루 한도), 사기 · 되팔기(수수료는 기금으로), 장부에 없는 복사본, 배당(기금에서만, 지난주부터 가진 증서, 도장),
     * 죽어도 떨어지지 않음, 창 내용, 은행원의 [증권 거래], 명령.
     */
    /** 3.74.0: 화면 · 문구 · 퀘스트 표시 (사용자 메모 W-*) 중 서버 쪽 */
    private static void v374(MinecraftServer server, FakePlayer a) {
        // W-15: 식품 상인은 농작물 상인이 다루는 품목을 올리지 않는다 (직접 적은 것 · 자동 규칙 모두). 빈 구분 탭은 없다
        ShopData food = ShopServer.shops().get("food"), farmer = ShopServer.shops().get("farmer");
        java.util.Set<String> farmKeys = new java.util.HashSet<>();
        if (farmer != null) for (ShopData.Entry e : farmer.entries) farmKeys.add(ShopLoader.key(e.stack));
        List<String> dup = new ArrayList<>();
        java.util.Set<String> usedCats = new java.util.HashSet<>();
        if (food != null) {
            for (ShopData.Entry e : food.entries) {
                if (farmKeys.contains(ShopLoader.key(e.stack))) dup.add(ShopLoader.itemId(e.stack));
                usedCats.add(e.category);
            }
        }
        List<String> emptyCats = new ArrayList<>();
        if (food != null) for (ShopData.Category c : food.categories) if (!usedCats.contains(c.id)) emptyCats.add(c.id);
        boolean wheatAtFarmer = farmer != null && findEntry(farmer, e -> e.stack.is(Items.WHEAT)) >= 0;
        boolean breadAtFood = food != null && findEntry(food, e -> e.stack.is(Items.BREAD) && !e.stack.hasTag()) >= 0;
        boolean mig = false, migAgain = true;
        String migText = "";
        try {
            Path tmp = Files.createTempFile("rtshop-food374", ".json");
            Files.writeString(tmp, "{\"categories\":[{\"id\":\"ingredient\",\"name\":\"농산물\",\"icon\":\"minecraft:carrot\"},{\"id\":\"dish\",\"name\":\"요리\"}],\"items\":[]}",
                    StandardCharsets.UTF_8);
            mig = ShopLoader.migrateFoodSkipFarmer(tmp);
            migText = Files.readString(tmp, StandardCharsets.UTF_8);
            migAgain = ShopLoader.migrateFoodSkipFarmer(tmp);
            Files.deleteIfExists(tmp);
        } catch (Exception e) {
            migText = e.toString();
        }
        check("3.74 the food merchant lists nothing the farmer sells (crops, raw meat, eggs, milk go to the farmer); no empty tab; installed food.json files get skipItemsOf once",
                food != null && farmer != null && dup.isEmpty() && emptyCats.isEmpty() && wheatAtFarmer && breadAtFood
                        && mig && !migAgain && migText.contains("skipItemsOf") && migText.contains("식재료") && !migText.contains("농산물"),
                "dup " + dup + " empty " + emptyCats + " wheat@farmer " + wheatAtFarmer + " bread@food " + breadAtFood + " mig " + mig + "/" + migAgain + " " + migText);

        // W-1 · W-24: 퀘스트 HUD 의 "다음 퀘스트" 에 직업 선행 퀘스트는 「조사대원의 밑천」 뒤부터, 직업이 없고 다른 선행 퀘스트를 하지 않을 때만
        QuestManager.reset(a);
        com.rtshop.job.Jobs.force(a, com.rtshop.job.JobConfig.COMBAT, "");
        com.rtshop.job.Jobs.force(a, com.rtshop.job.JobConfig.LIFE, "");
        QuestDef jf = QuestManager.loaded().quests.get("job_farmer_1"), jw = QuestManager.loaded().quests.get("job_warrior_1");
        boolean before = jf != null && jw != null && QuestManager.jobQuestHiddenInHud(a, jf) && QuestManager.jobQuestHiddenInHud(a, jw);
        QuestManager.forceComplete(a, QuestManager.JOB_HINT_AFTER);
        boolean open = jf != null && jw != null && !QuestManager.jobQuestHiddenInHud(a, jf) && !QuestManager.jobQuestHiddenInHud(a, jw);
        com.rtshop.job.Jobs.force(a, com.rtshop.job.JobConfig.COMBAT, "swordsman");
        boolean withJob = jf != null && jw != null && QuestManager.jobQuestHiddenInHud(a, jf) && QuestManager.jobQuestHiddenInHud(a, jw);
        com.rtshop.job.Jobs.force(a, com.rtshop.job.JobConfig.COMBAT, "");
        QuestManager.accept(a, "job_swordsman_1", "job");
        boolean doing = jf != null && jw != null && QuestManager.jobQuestHiddenInHud(a, jf) && QuestManager.jobQuestHiddenInHud(a, jw);
        QuestDef s001 = QuestManager.loaded().quests.get("story_0_01");
        boolean storyShown = s001 != null && !QuestManager.jobQuestHiddenInHud(a, s001);
        check("3.74 the quest HUD tells about job trial quests only after 「조사대원의 밑천」 and only while the player has no job and no trial under way (taking them still works)",
                before && open && withJob && doing && storyShown, before + " " + open + " " + withJob + " " + doing + " " + storyShown);
        QuestManager.reset(a);

        // W-28: NPC 에게 다시 말을 걸면 그 NPC 의 지도 표시를 다시 보낸다 (지도에서 지웠어도 다시 생긴다)
        com.rtshop.story.MapMarks.lastAgainForTest = "";
        boolean firstMark = com.rtshop.story.MapMarks.mark(a, "t374", "Test", "minecraft:overworld", 10, 64, 10, 0xFFFFFFFF, "", true);
        boolean noAgainFirst = com.rtshop.story.MapMarks.lastAgainForTest.isEmpty();
        boolean secondMark = com.rtshop.story.MapMarks.mark(a, "t374", "Test", "minecraft:overworld", 11, 64, 10, 0xFFFFFFFF, "", true);
        boolean againSent = "t374".equals(com.rtshop.story.MapMarks.lastAgainForTest);
        com.rtshop.story.MapMarks.lastAgainForTest = "";
        com.rtshop.story.MapMarks.mark(a, "t374", "Test", "minecraft:overworld", 10, 64, 10, 0xFFFFFFFF, "", false);
        boolean quietWithout = com.rtshop.story.MapMarks.lastAgainForTest.isEmpty();
        check("3.74 talking to an NPC again sends its map mark again (so a mark deleted on the map comes back); other unchanged marks stay quiet",
                !com.rtshop.story.MapMarks.get().enabled || (firstMark && noAgainFirst && !secondMark && againSent && quietWithout),
                com.rtshop.story.MapMarks.get().enabled + " " + firstMark + " " + noAgainFirst + " " + secondMark + " " + againSent + " " + quietWithout);
        com.rtshop.story.MapMarks.forgetAll(a);

        // W-20: 모험 안내인 모리스의 퀘스트 창 인사도 해요체, 영어 번역이 있다
        QuestManager.NpcDef guide = QuestManager.loaded().npcs.get("guide");
        String greet = guide == null ? "" : guide.greeting;
        com.rtshop.story.StoryText.setLang(a, "en_us");
        String greetEn = com.rtshop.story.StoryText.t(a, greet);
        com.rtshop.story.StoryText.setLang(a, "");
        check("3.74 Morris's quest-window greeting speaks like his other lines (해요체) and has an English line",
                greet.contains("드릴게요") && !greet.contains("오게") && !greetEn.equals(greet) && greetEn.startsWith("Welcome"), greet + " / " + greetEn);

        // W-11: Cataclysm 보스 건축물은 나침반 대신 그 모드의 눈으로 (다른 목표가 있으면 나침반은 그쪽을 가리킨다)
        boolean cata = net.minecraftforge.fml.ModList.get().isLoaded("cataclysm");
        QuestManager.reset(a);
        QuestDef qEye = tempQuest("{\"id\":\"t_eye374\",\"giver\":\"quest_guide\",\"objectives\":[{\"type\":\"STRUCTURE\",\"target\":\"cataclysm:soul_black_smith\",\"text\":\"Forge\"}],\"reward\":{\"gold\":1}}");
        QuestDef qFar = tempQuest("{\"id\":\"t_far374\",\"giver\":\"quest_guide\",\"objectives\":[{\"type\":\"STRUCTURE\",\"target\":\"minecraft:stronghold\",\"text\":\"Hold\"}],\"reward\":{\"gold\":1}}");
        QuestManager.loaded().quests.put(qEye.id, qEye);
        QuestManager.loaded().quests.put(qFar.id, qFar);
        boolean tookEye = QuestManager.accept(a, qEye.id, "quest_guide"), tookFar = QuestManager.accept(a, qFar.id, "quest_guide");
        String[] both = com.rtshop.item.SurveyCompassItem.pickTarget(a);
        QuestManager.abandon(a, qFar.id);
        String[] eyeOnly = com.rtshop.item.SurveyCompassItem.pickTarget(a);
        Item eye = com.rtshop.item.SurveyCompassItem.eyeFor("cataclysm:soul_black_smith");
        boolean eyes = cata ? eye != null && "cataclysm:monstrous_eye".equals(String.valueOf(ForgeRegistries.ITEMS.getKey(eye)))
                && com.rtshop.item.SurveyCompassItem.eyeFor("cataclysm:burning_arena") != null && com.rtshop.item.SurveyCompassItem.eyeFor("cataclysm:amethyst_nest") == null
                : eye == null;
        boolean picks = tookEye && tookFar && both != null && eyeOnly != null && "cataclysm:soul_black_smith".equals(eyeOnly[0])
                && (cata ? "minecraft:stronghold".equals(both[0]) : "cataclysm:soul_black_smith".equals(both[0]));
        check("3.74 the survey compass leaves Cataclysm boss buildings to that mod's eyes (it points to the quest's other places first; amethyst nests stay on the compass)",
                eyes && picks && com.rtshop.item.SurveyCompassItem.eyeFor("minecraft:stronghold") == null,
                cata + " eye " + eye + " both " + (both == null ? "null" : both[0]) + " eyeOnly " + (eyeOnly == null ? "null" : eyeOnly[0]) + " took " + tookEye + tookFar);
        QuestManager.reset(a);
        QuestManager.loaded().quests.remove(qEye.id);
        QuestManager.loaded().quests.remove(qFar.id);
    }

    private static void stocks(MinecraftServer server, FakePlayer a) {
        com.rtshop.stock.StockConfig sc = com.rtshop.stock.StockConfig.copyOfDefaults();
        sc.noise = 0;
        sc.pull = 0;
        sc.events = false;   // 3.73 의 무작위 시장 사건은 아래에서 따로 본다
        com.rtshop.stock.StockConfig.forTest(sc);
        com.rtshop.stock.Stocks.testAnywhere = true;
        com.rtshop.stock.Stocks.testing = true;
        try {
            com.rtshop.stock.Stocks.resetForTest(server);
            com.rtshop.stock.StockData sd = com.rtshop.stock.StockData.get(server);
            boolean start = sd.board("fish").price == 400 && sd.board("survey").price == 1000 && sd.boards.size() == 6;
            // 시세: 첫날 실적 5,000 → 그대로 / 둘째 날 어업 40,000(한 사람 상한 30,000) · 농업 0 → +6% · -6%
            java.util.UUID id = a.getUUID();
            com.rtshop.stock.Stocks.activity(server, id, "fish", 5000);
            com.rtshop.stock.Stocks.activity(server, id, "farm", 5000);
            com.rtshop.stock.Stocks.advance(server, 1, true);
            long f1 = sd.board("fish").price, r1 = sd.board("farm").price;
            com.rtshop.stock.Stocks.activity(server, id, "fish", 40000);
            com.rtshop.stock.Stocks.advance(server, 1, true);
            long f2 = sd.board("fish").price, r2 = sd.board("farm").price;
            // 이야기 소식(얼어붙은 바다가 풀렸다 +8%)은 서버에서 한 번만, 하루 한도 ±10% 안에서
            com.rtshop.stock.Stocks.onQuestDone(a, "story_3_38");
            com.rtshop.stock.Stocks.onQuestDone(a, "story_3_38");
            boolean news = sd.board("fish").pendingNews > 0.079 && sd.board("fish").pendingNews < 0.081 && sd.newsLog.size() == 1;
            com.rtshop.stock.Stocks.activity(server, id, "fish", 30000);
            com.rtshop.stock.Stocks.advance(server, 1, true);
            long f3 = sd.board("fish").price, r3 = sd.board("farm").price;
            boolean prices = f1 == 400 && r1 == 400 && f2 == 424 && r2 == 376 && f3 == 466 && r3 == 365 && sd.board("fish").pendingNews == 0
                    && sd.board("fish").history.size() == 3;
            // 사기 · 되팔기: 수수료 2% 는 기금으로
            com.rtshop.economy.Economy.setWallet(a, 10_000_000L);
            a.getInventory().clearContent();
            long w0 = com.rtshop.economy.Economy.wallet(a), fund0 = sd.fund;
            int b1 = com.rtshop.stock.Stocks.buy(a, "fish", 10);
            long paid = w0 - com.rtshop.economy.Economy.wallet(a);
            boolean bought = b1 == com.rtshop.stock.Stocks.OK && paid == 466L * 10 + 94 && sd.fund - fund0 == 94 && sd.certs.size() == 1;
            ItemStack cert = ItemStack.EMPTY;
            for (ItemStack s : a.getInventory().items) if (s.is(com.rtshop.item.ModItems.STOCK_CERTIFICATE.get())) cert = s;
            boolean item = !cert.isEmpty() && "fish".equals(com.rtshop.item.StockCertificateItem.union(cert)) && com.rtshop.item.StockCertificateItem.shares(cert) == 10
                    && cert.getHoverName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc372
                    && tc372.getKey().equals("item.rtshop.stock_certificate.named") && tc372.getArgs().length == 3 && Integer.valueOf(10).equals(tc372.getArgs()[2]);
            sc.buyPerDay = 15;
            int bLimit = com.rtshop.stock.Stocks.buy(a, "fish", 10);
            sc.buyPerDay = 200;
            int bAmount = com.rtshop.stock.Stocks.buy(a, "fish", sc.maxPerCertificate + 1);
            int bUnion = com.rtshop.stock.Stocks.buy(a, "nope", 1);
            boolean refused = bLimit == com.rtshop.stock.Stocks.ERR_LIMIT && bAmount == com.rtshop.stock.Stocks.ERR_AMOUNT && bUnion == com.rtshop.stock.Stocks.ERR_UNION;
            // 복사본: 같은 일련번호의 두 번째 장은 장부에 없어 되팔 수 없다
            java.util.UUID serial = com.rtshop.item.StockCertificateItem.id(cert);
            a.getInventory().add(cert.copy());
            long w1 = com.rtshop.economy.Economy.wallet(a), fund1 = sd.fund;
            long sold = com.rtshop.stock.Stocks.sell(a, serial);
            long soldCopy = com.rtshop.stock.Stocks.sell(a, serial);
            boolean sellOk = sold == 466L * 10 - 94 && com.rtshop.economy.Economy.wallet(a) - w1 == sold && sd.fund - fund1 == 94 && sd.certs.isEmpty()
                    && soldCopy == -com.rtshop.stock.Stocks.ERR_INVALID;
            a.getInventory().clearContent();
            // 배당: 지난주부터 가진 증서에, 기금에서만, 한 주에 한 번 (증서에 도장)
            int b2 = com.rtshop.stock.Stocks.buy(a, "fish", 10);
            com.rtshop.stock.Stocks.activity(server, id, "fish", 10000);
            long fundBefore = sd.fund;
            long early = com.rtshop.stock.Stocks.claim(a);   // 이번 주에 산 증서: 아직 없음
            com.rtshop.stock.Stocks.advance(server, 7, true);
            long divPer = sd.board("fish").dividend;
            long w2 = com.rtshop.economy.Economy.wallet(a), fund2 = sd.fund;
            long got = com.rtshop.stock.Stocks.claim(a);
            long again = com.rtshop.stock.Stocks.claim(a);
            ItemStack c2 = ItemStack.EMPTY;
            for (ItemStack s : a.getInventory().items) if (s.is(com.rtshop.item.ModItems.STOCK_CERTIFICATE.get())) c2 = s;
            net.minecraft.nbt.CompoundTag ct = c2.getTagElement(com.rtshop.item.StockCertificateItem.TAG);
            boolean dividend = b2 == com.rtshop.stock.Stocks.OK && early == 0 && divPer > 0 && got == divPer * 10 && com.rtshop.economy.Economy.wallet(a) - w2 == got
                    && fund2 - sd.fund == got && again == 0 && ct != null && ct.contains("paidDay") && sd.fund >= 0 && fundBefore > 0;
            // 죽어도 떨어지지 않는다 (떨어질 것에서 빼 두었다가 되살아날 때 돌려준다)
            java.util.List<net.minecraft.world.entity.item.ItemEntity> drops = new java.util.ArrayList<>();
            drops.add(new net.minecraft.world.entity.item.ItemEntity(a.level(), a.getX(), a.getY(), a.getZ(), c2.copy()));
            a.getInventory().clearContent();
            net.minecraftforge.event.entity.living.LivingDropsEvent de = new net.minecraftforge.event.entity.living.LivingDropsEvent(a, a.level().damageSources().generic(), drops, 0, false);
            com.rtshop.stock.Stocks.onDrops(de);
            boolean kept = de.getDrops().isEmpty() && a.getPersistentData().getList("rtshop_keep_certs", net.minecraft.nbt.Tag.TAG_COMPOUND).size() == 1;
            com.rtshop.stock.Stocks.onClone(new net.minecraftforge.event.entity.player.PlayerEvent.Clone(a, a, true));
            boolean back = false;
            for (ItemStack s : a.getInventory().items) back |= s.is(com.rtshop.item.ModItems.STOCK_CERTIFICATE.get()) && serialOf(s) != null;
            back &= !a.getPersistentData().contains("rtshop_keep_certs");
            // 창 · 은행원의 [증권 거래] · 명령
            com.google.gson.JsonObject v = com.rtshop.stock.Stocks.view(a, true, "rtshop.stock.claimed", new String[]{"80"}, true);
            boolean view = v.get("trade").getAsBoolean() && v.getAsJsonArray("unions").size() == 6 && v.getAsJsonArray("mine").size() == 1
                    && v.getAsJsonArray("news").size() == 1 && v.get("msg").getAsString().equals("rtshop.stock.claimed")
                    && v.getAsJsonArray("unions").get(1).getAsJsonObject().getAsJsonArray("history").size() >= 2;
            boolean desk = com.rtshop.story.Dialogue.buildForTest(a, "bank").has("stocks") && !com.rtshop.story.Dialogue.buildForTest(a, "job").has("stocks");
            boolean cmd = server.getCommands().getDispatcher().findNode(List.of("rtshop", "stocks", "trade")) != null
                    && server.getCommands().getDispatcher().findNode(List.of("rtshop", "stocks", "status")) != null
                    && server.getCommands().getDispatcher().findNode(List.of("rtshop", "stocks", "nextday")) != null;
            check("3.72 stocks: six unions; the daily price follows the day's work against the last days (one person's share capped), story news once, within ±10%",
                    start && prices && news, "start " + start + " prices " + f1 + "/" + r1 + " " + f2 + "/" + r2 + " " + f3 + "/" + r3 + " news " + news + " " + sd.board("fish").pendingNews);
            check("3.72 stocks: buying and selling back pay a 2% fee into the fund; limits per day · per certificate; a copied certificate is not in the ledger",
                    bought && item && refused && sellOk, "bought " + bought + " (" + b1 + ", paid " + paid + ") item " + item + " refused " + bLimit + "/" + bAmount + "/" + bUnion
                            + " sell " + sold + "/" + soldCopy + " " + sellOk);
            check("3.72 stocks: weekly dividends come only from the fund, for certificates held since last week, once (stamped); certificates are kept on death",
                    dividend && kept && back, "dividend " + dividend + " (early " + early + " per " + divPer + " got " + got + " again " + again + " fund " + fundBefore + "→" + sd.fund
                            + ") kept " + kept + " back " + back);
            check("3.72 stocks: the window gets prices · history · my certificates · news; the banker offers [Exchange]; commands", view && desk && cmd,
                    "view " + view + " desk " + desk + " cmd " + cmd + " " + v);
            // 3.73: 무작위 시장 사건 — 뇌우가 길었던 다음 날의 폭풍 경보(조건 사건), 하루 하나의 무작위 사건, 다음 날의 뒷몫, 끄기
            com.rtshop.stock.StockConfig ec = com.rtshop.stock.StockConfig.copyOfDefaults();
            ec.noise = 0;
            ec.pull = 0;
            ec.eventChance = 1;
            ec.conditionChance = 1;
            com.rtshop.stock.StockConfig.forTest(ec);
            com.rtshop.stock.Stocks.resetForTest(server);
            com.rtshop.stock.Stocks.seedForTest(373);
            sd.thunderTicks = 40_000;
            com.rtshop.stock.Stocks.advance(server, 1, true);
            long evN = sd.newsLog.stream().filter(l -> "event".equals(l.kind)).count();
            boolean storm = sd.newsLog.stream().anyMatch(l -> "event".equals(l.kind) && l.union.equals("fish") && l.change < 0 && l.text.contains("폭풍"));
            boolean follow = Math.abs(sd.board("fish").pendingNext) > 1e-6 && sd.thunderTicks == 0;
            boolean inRange = true;
            for (com.rtshop.stock.StockData.NewsLine l : sd.newsLog) inRange &= Math.abs(l.change) >= 0.019 && Math.abs(l.change) <= 0.081;
            ec.events = false;
            com.rtshop.stock.Stocks.advance(server, 1, true);
            boolean consumed = true;
            for (com.rtshop.stock.StockData.Board bb : sd.boards.values()) consumed &= bb.pendingNext == 0;
            boolean off = sd.newsLog.stream().filter(l -> "event".equals(l.kind)).count() == evN;
            check("3.73 stocks: market events — after a long thunderstorm a storm warning for the fishers, one random event a day when it rolls (3~8%), part of it returns the next day; events can be turned off",
                    evN >= 2 && storm && follow && inRange && consumed && off, "events " + evN + " storm " + storm + " follow " + follow + " range " + inRange + " consumed " + consumed
                            + " off " + off + " last " + com.rtshop.stock.Stocks.lastEventForTest);
        } finally {
            com.rtshop.stock.Stocks.testAnywhere = false;
            com.rtshop.stock.Stocks.testing = false;
            com.rtshop.stock.StockConfig.forTest(null);
            com.rtshop.stock.Stocks.resetForTest(server);
            a.getInventory().clearContent();
        }
    }

    private static java.util.UUID serialOf(ItemStack s) {
        return com.rtshop.item.StockCertificateItem.id(s);
    }

    private static void ranks(MinecraftServer server, FakePlayer a, FakePlayer b) {
        com.rtshop.rank.Ranks.testFakes = true;
        try {
            com.rtshop.rank.Ranks.resetForTest(a);
            com.rtshop.rank.Ranks.resetForTest(b);
            com.rtshop.rank.Ranks.Config c = com.rtshop.rank.Ranks.get();
            check("ranks: every category is on by default and each has titles in rising steps",
                    c.enabled && c.maxEffectTitles == 2 && java.util.Arrays.stream(com.rtshop.rank.Ranks.CATS).allMatch(cat -> com.rtshop.rank.Ranks.on(cat)
                            && c.titles.stream().filter(t -> t.category.equals(cat)).count() >= 3), c.titles.size());
            // 기록 → 칭호
            com.rtshop.rank.Ranks.add(a, com.rtshop.rank.Ranks.KILLS, 9);
            boolean closed = !com.rtshop.rank.Ranks.has(com.rtshop.rank.Ranks.rec(a), "kills_1");
            com.rtshop.rank.Ranks.add(a, com.rtshop.rank.Ranks.KILLS, 1);
            boolean open = com.rtshop.rank.Ranks.has(com.rtshop.rank.Ranks.rec(a), "kills_1") && !com.rtshop.rank.Ranks.has(com.rtshop.rank.Ranks.rec(a), "kills_2");
            String lockedErr = com.rtshop.rank.Ranks.setEffect(a, "kills_2", true);
            String ok1 = com.rtshop.rank.Ranks.setEffect(a, "kills_1", true);
            double dmg = com.rtshop.rank.Ranks.effect(a, "mob_damage");
            check("titles: the first hunter title opens at 10 kills, a locked one can not be used, a used one gives its small effect",
                    closed && open && lockedErr != null && ok1 == null && dmg == 0.5, closed + "/" + open + "/" + lockedErr + "/" + ok1 + "/" + dmg);
            // 같은 종목은 바꿔 끼우고, 서로 다른 종목으로 둘까지
            com.rtshop.rank.Ranks.add(a, com.rtshop.rank.Ranks.KILLS, 90);
            String swap = com.rtshop.rank.Ranks.setEffect(a, "kills_2", true);
            com.rtshop.rank.Ranks.add(a, com.rtshop.rank.Ranks.QUESTS, 10);
            com.rtshop.rank.Ranks.add(a, com.rtshop.rank.Ranks.RAIDS, 1);
            String second = com.rtshop.rank.Ranks.setEffect(a, "quests_1", true);
            String third = com.rtshop.rank.Ranks.setEffect(a, "raids_1", true);
            java.util.List<String> on = new java.util.ArrayList<>(com.rtshop.rank.Ranks.rec(a).effects);
            check("titles: a higher step replaces the lower one of its category, two different categories can be used at once and a third is refused",
                    swap == null && second == null && "rtshop.title.err.full".equals(third) && on.equals(java.util.List.of("kills_2", "quests_1"))
                            && com.rtshop.rank.Ranks.effect(a, "mob_damage") == 1.0 && com.rtshop.rank.Ranks.plus(a, "quest_gold", 1000) == 1010
                            && com.rtshop.rank.Ranks.plus(a, "bounty_gold", 1000) == 1000 && com.rtshop.rank.Ranks.minus(a, "buy_discount", 1000) == 1000,
                    swap + "/" + second + "/" + third + "/" + on);
            // 최대 체력 칭호
            com.rtshop.rank.Ranks.setEffect(a, "quests_1", false);
            float hpBefore = a.getMaxHealth();
            com.rtshop.rank.Ranks.setEffect(a, "raids_1", true);
            float hpWith = a.getMaxHealth();
            com.rtshop.rank.Ranks.setEffect(a, "raids_1", false);
            check("titles: the expedition title adds a little max health while it is used and takes it back when it is not",
                    hpWith == hpBefore + 1 && a.getMaxHealth() == hpBefore, hpBefore + " -> " + hpWith + " -> " + a.getMaxHealth());
            // 내걸기: 하나만, 이름 앞에
            String showLocked = com.rtshop.rank.Ranks.setShow(a, "kills_5");
            String show = com.rtshop.rank.Ranks.setShow(a, "kills_1");
            String shownName = a.getDisplayName().getString();
            com.rtshop.rank.Ranks.setShow(a, "");
            String plain = a.getDisplayName().getString();
            check("titles: one opened title can be shown in front of the name (chat, tab list) and taken down again; it need not be one of the used ones",
                    showLocked != null && show == null && shownName.startsWith("[") && shownName.contains("rtshop_test_a") && !plain.startsWith("["), shownName + " / " + plain);
            // 주간 순위: 수익·소비는 저마다, 보유 Gold 는 지금 가진 것
            com.rtshop.rank.Ranks.earned(a, 5000);
            com.rtshop.rank.Ranks.earned(b, 9000);
            com.rtshop.rank.Ranks.spent(b, 100);
            java.util.List<com.rtshop.rank.Ranks.Row> earned = com.rtshop.rank.Ranks.ranking(server, com.rtshop.rank.Ranks.EARNED);
            int ia = -1, ib = -1;
            for (int i = 0; i < earned.size(); i++) {
                if (earned.get(i).id().equals(a.getUUID())) ia = i;
                if (earned.get(i).id().equals(b.getUUID())) ib = i;
            }
            com.google.gson.JsonObject view = com.rtshop.rank.Ranks.ranksView(a);
            check("ranks: this week's ranking orders players by what they did this week and a player sees their own place",
                    ia >= 0 && ib >= 0 && ib < ia && view.getAsJsonArray("cats").size() == com.rtshop.rank.Ranks.CATS.length && view.get("resetIn").getAsLong() > 0,
                    ib + " < " + ia + " / " + view.getAsJsonArray("cats").size());
            // 주간 마감: 다른 사람의 기록은 치워 두고 a·b 만으로
            for (java.util.Map.Entry<java.util.UUID, com.rtshop.rank.Ranks.Rec> e : com.rtshop.rank.Ranks.data(server).recs.entrySet()) {
                if (!e.getKey().equals(a.getUUID()) && !e.getKey().equals(b.getUUID())) {
                    e.getValue().week.clear();
                    e.getValue().goldSeen = 0;
                }
            }
            long walletA = Economy.wallet(a), pendingBefore = com.rtshop.rank.Ranks.rec(b).pendingGold;
            com.rtshop.rank.Ranks.settle(server);
            com.rtshop.rank.Ranks.Rec ra = com.rtshop.rank.Ranks.rec(a), rb = com.rtshop.rank.Ranks.rec(b);
            long[] rw = c.rewards;
            boolean wonB = rb.weekWon.contains(com.rtshop.rank.Ranks.EARNED) && rb.weekWon.contains(com.rtshop.rank.Ranks.SPENT) && !ra.weekWon.contains(com.rtshop.rank.Ranks.EARNED);
            boolean cleared = ra.week.isEmpty() && rb.week.isEmpty() && ra.life.get(com.rtshop.rank.Ranks.KILLS) == 100;
            boolean lastKept = com.rtshop.rank.Ranks.data(server).last.get(com.rtshop.rank.Ranks.EARNED).get(0).id().equals(b.getUUID());
            String weekShow = com.rtshop.rank.Ranks.setShow(b, com.rtshop.rank.Ranks.WEEK + com.rtshop.rank.Ranks.EARNED);
            String weekNo = com.rtshop.rank.Ranks.setShow(a, com.rtshop.rank.Ranks.WEEK + com.rtshop.rank.Ranks.EARNED);
            long pendA = ra.pendingGold;
            long got = com.rtshop.rank.Ranks.claim(a);
            check("ranks: when the week ends the top places get rewards kept for them, the first place may show the week's title, weekly counts start over and lifetime counts stay",
                    wonB && cleared && lastKept && weekShow == null && weekNo != null && rb.pendingGold >= pendingBefore + rw[0] * 2 && pendA >= rw[1]
                            && got == pendA && Economy.wallet(a) == walletA + got && com.rtshop.rank.Ranks.claim(a) == 0,
                    wonB + "/" + cleared + "/" + lastKept + "/" + weekShow + "/" + weekNo + "/" + rb.pendingGold + "/" + pendA + "/" + got);
            com.google.gson.JsonObject tv = com.rtshop.rank.Ranks.titlesView(b);
            check("titles: the keeper's list shows every category with its steps and the week's titles a player won",
                    tv.getAsJsonArray("cats").size() == com.rtshop.rank.Ranks.CATS.length && tv.getAsJsonArray("week").size() >= 2 && tv.get("max").getAsInt() == 2,
                    tv.getAsJsonArray("week").size());
            check("ranks: the three keepers exist (bounty board, weekly ranking, titles) and have their own lines",
                    QuestManager.loaded().npcs.containsKey("bounty") && QuestManager.loaded().npcs.containsKey("ranks") && QuestManager.loaded().npcs.containsKey("titles")
                            && com.rtshop.story.Dialogue.npcForTest("quest_ranks") != null && com.rtshop.story.Dialogue.npcForTest("quest_titles") != null
                            && com.rtshop.story.Dialogue.npcForTest("quest_bounty") != null && com.rtshop.story.Hunts.isBoardKey("quest_bounty"),
                    QuestManager.loaded().npcs.keySet());
        } finally {
            com.rtshop.rank.Ranks.resetForTest(a);
            com.rtshop.rank.Ranks.resetForTest(b);
            com.rtshop.rank.Ranks.testFakes = false;
        }
    }

    /** 도감: 마주치면 모습만, 잡으면 능력치·전리품까지, 장비는 지니면 적힌다. */
    /** 지도 표시 (3.54): 말을 건 NPC 와 가 본 장소가 사람마다 적히고, 같은 것은 다시 보내지 않는다. */
    private static void mapMarks(MinecraftServer server, ServerLevel level, FakePlayer a) {
        com.rtshop.story.MapMarks.Config mc = com.rtshop.story.MapMarks.fresh();
        com.rtshop.story.MapMarks.setForTest(mc);
        Villager banker = null;
        try {
            com.rtshop.story.MapMarks.forgetAll(a);
            com.rtshop.story.MapMarks.Place pl = new com.rtshop.story.MapMarks.Place();
            pl.name = "점검 광장";
            pl.dim = a.level().dimension().location().toString();
            pl.x = a.blockPosition().getX() + 30;
            pl.y = a.blockPosition().getY();
            pl.z = a.blockPosition().getZ();
            pl.radius = 10;
            mc.places.add(pl);
            int far = com.rtshop.story.MapMarks.visit(a);
            pl.x = a.blockPosition().getX() + 6;
            int near = com.rtshop.story.MapMarks.visit(a), again = com.rtshop.story.MapMarks.visit(a);
            String sent = com.rtshop.story.MapMarks.lastSentForTest;
            check("map marks: a place is noted once when the player steps inside its radius (not from outside, not twice)",
                    far == 0 && near == 1 && again == 0 && com.rtshop.story.MapMarks.knows(a, "place:점검 광장") && sent.contains("점검 광장") && sent.contains("\"notify\":true"),
                    far + " " + near + " " + again + " " + sent);
            banker = ShopServer.spawnNpc(level, a.position().add(2, 0, 0), 0F, "bank");
            boolean t1 = banker != null && com.rtshop.story.MapMarks.talked(a, banker), t2 = banker != null && com.rtshop.story.MapMarks.talked(a, banker);
            String sentNpc = com.rtshop.story.MapMarks.lastSentForTest;
            boolean moved = false, movedSilently = false;
            if (banker != null) {
                banker.setPos(banker.getX() + 12, banker.getY(), banker.getZ());
                moved = com.rtshop.story.MapMarks.talked(a, banker);
                movedSilently = com.rtshop.story.MapMarks.lastSentForTest.contains("\"notify\":false");
            }
            net.minecraft.world.entity.animal.Cow cow = net.minecraft.world.entity.EntityType.COW.create(level);
            boolean notNpc = cow != null && !com.rtshop.story.MapMarks.talked(a, cow);
            if (cow != null) cow.discard();
            check("map marks: talking to an NPC notes its name and place once; a moved NPC is corrected without a new notice; other creatures are not noted",
                    t1 && !t2 && moved && movedSilently && notNpc && com.rtshop.story.MapMarks.known(a) == 2 && banker.getCustomName() != null
                            && sentNpc.contains(banker.getCustomName().getString())
                            && sentNpc.contains("\"radar\":true") && sentNpc.contains("npc:" + banker.getUUID()),   // 화면 쪽은 이 목록으로 미니맵의 NPC 아이콘을 고른다
                    t1 + " " + t2 + " " + moved + " " + movedSilently + " " + notNpc + " " + com.rtshop.story.MapMarks.known(a));
            // K-1 (3.69.0): NPC 표시에는 하는 일의 그림(직업 상징)과 무리 색이 붙는다. 그림이 없던 옛 표시는 다시 보낼 때 붙는다
            java.util.List<String> noPng = new java.util.ArrayList<>();
            for (String n : com.rtshop.story.MapMarks.ICONS) {
                if (SelfTest.class.getResourceAsStream("/assets/rtshop/textures/waypoint_icons/" + n + ".png") == null) noPng.add(n);
            }
            boolean mapping = "anvil".equals(com.rtshop.story.MapMarks.iconOf("shop_blacksmith")[0]) && "fish".equals(com.rtshop.story.MapMarks.iconOf("shop_fisher")[0])
                    && "book".equals(com.rtshop.story.MapMarks.iconOf("quest_order_archivist")[0]) && "sword".equals(com.rtshop.story.MapMarks.iconOf("quest_sword_master")[0])
                    && "quest".equals(com.rtshop.story.MapMarks.iconOf("quest_new_story_npc")[0]) && "person".equals(com.rtshop.story.MapMarks.iconOf(null)[0])
                    && (Integer) com.rtshop.story.MapMarks.iconOf("shop_food")[1] == com.rtshop.story.MapMarks.C_SHOP
                    && (Integer) com.rtshop.story.MapMarks.iconOf("quest_hunter")[1] == com.rtshop.story.MapMarks.C_MASTER;
            String oldResent = "";
            if (banker != null) {
                com.rtshop.story.MapMarks.mark(a, "npc:" + banker.getUUID(), banker.getCustomName().getString(), banker.level().dimension().location().toString(),
                        banker.blockPosition().getX(), banker.blockPosition().getY(), banker.blockPosition().getZ(), 0xFFE8C56A);   // 3.68 까지처럼 그림 없이
                com.rtshop.story.MapMarks.sendAll(a, false);
                oldResent = com.rtshop.story.MapMarks.lastSentForTest;
            }
            check("K-1 an NPC mark carries the picture of what the NPC does (the bank: a coin) and its group colour; an older mark without one gets it when sent again",
                    sentNpc.contains("\"icon\":\"rtshop:coin\"") && mapping && noPng.isEmpty() && oldResent.contains("\"icon\":\"rtshop:coin\"")
                            && oldResent.contains("\"color\":" + com.rtshop.story.MapMarks.C_TOWN) && mc.npcIcons,
                    mapping + " " + noPng + " " + sentNpc + " // " + oldResent);
            mc.places.clear();
            com.rtshop.story.MapMarks.sendAll(a, false);
            String afterRemove = com.rtshop.story.MapMarks.lastSentForTest;
            check("map marks: a place the operator removed leaves the player's record and is sent as gone",
                    !com.rtshop.story.MapMarks.knows(a, "place:점검 광장") && com.rtshop.story.MapMarks.known(a) == 1 && afterRemove.contains("\"gone\":[\"place:점검 광장\"]"), afterRemove);
            mc.enabled = false;
            boolean off = banker != null && !com.rtshop.story.MapMarks.mark(a, "npc:x", "x", pl.dim, 0, 0, 0, -1);
            com.rtshop.story.MapMarks.sendAll(a, false);
            check("map marks can be switched off in map_marks.json (then the minimap shows every NPC icon again)",
                    off && com.rtshop.story.MapMarks.lastSentForTest.contains("\"radar\":false"), com.rtshop.story.MapMarks.lastSentForTest);
        } finally {
            if (banker != null) banker.discard();
            com.rtshop.story.MapMarks.forgetAll(a);
            com.rtshop.story.MapMarks.reload();
        }
    }

    private static void codex(MinecraftServer server, ServerLevel level, FakePlayer a) {
        com.rtshop.story.Codex.testFakes = true;
        try {
            com.rtshop.story.Codex.resetForTest(a);
            java.util.Map<String, Boolean> mobs = com.rtshop.story.Codex.mobs();
            java.util.List<String> gear = com.rtshop.story.Codex.gear();
            check("codex: the list holds monsters, animals and bosses (not armor stands or players), and every kind of gear",
                    mobs.containsKey("minecraft:zombie") && mobs.containsKey("minecraft:cow") && Boolean.TRUE.equals(mobs.get("minecraft:wither"))
                            && Boolean.FALSE.equals(mobs.get("minecraft:zombie")) && !mobs.containsKey("minecraft:armor_stand") && !mobs.containsKey("minecraft:player")
                            && !mobs.containsKey("minecraft:arrow") && gear.contains("minecraft:iron_sword") && gear.contains("minecraft:diamond_chestplate")
                            && !gear.contains("minecraft:apple"), mobs.size() + " mobs, " + gear.size() + " gear");
            net.minecraft.world.entity.monster.Zombie z = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
            com.rtshop.story.Codex.Rec r = com.rtshop.story.Codex.rec(a);
            int s0 = com.rtshop.story.Codex.state(r, "minecraft:zombie");
            boolean metNew = com.rtshop.story.Codex.meet(a, z), metAgain = com.rtshop.story.Codex.meet(a, z);
            int s1 = com.rtshop.story.Codex.state(r, "minecraft:zombie");
            com.google.gson.JsonObject before = com.rtshop.story.Codex.detail(a, "minecraft:zombie");
            com.rtshop.story.Codex.onKill(a, z);
            com.rtshop.story.Codex.onKill(a, z);
            int s2 = com.rtshop.story.Codex.state(r, "minecraft:zombie");
            com.google.gson.JsonObject after = com.rtshop.story.Codex.detail(a, "minecraft:zombie");
            check("codex: a mob is unknown, then met (look and name only), then slain (stats and drops are written)",
                    s0 == 0 && metNew && !metAgain && s1 == 1 && !before.has("hp") && s2 == 2 && after.get("hp").getAsDouble() == 20.0
                            && after.get("kills").getAsInt() == 2 && after.has("drops") && after.has("atk"),
                    s0 + "/" + s1 + "/" + s2 + " " + after);
            a.getInventory().clearContent();
            a.getInventory().add(new ItemStack(Items.IRON_SWORD));
            a.getInventory().add(new ItemStack(Items.APPLE));
            int added = com.rtshop.story.Codex.scanGear(a), again = com.rtshop.story.Codex.scanGear(a);
            a.getInventory().clearContent();
            com.google.gson.JsonObject view = com.rtshop.story.Codex.view(a);
            check("codex: gear is written once it is carried (food is not gear), and the whole book is sent with what is known",
                    added == 1 && again == 0 && r.gear.contains("minecraft:iron_sword") && view.getAsJsonArray("mobs").size() == mobs.size()
                            && view.getAsJsonArray("gear").size() == gear.size(), added + "/" + again);
            com.google.gson.JsonObject guide = com.rtshop.story.Guide.get();
            boolean texts = true, images = true;
            int pages = 0;
            for (com.google.gson.JsonElement ce : guide.getAsJsonArray("categories")) {
                for (com.google.gson.JsonElement pe : ce.getAsJsonObject().getAsJsonArray("pages")) {
                    com.google.gson.JsonObject pg = pe.getAsJsonObject();
                    pages++;
                    texts &= pg.get("title").getAsString().length() > 1 && pg.get("text").getAsString().length() > 20;
                    if (pg.has("image")) {
                        String path = pg.get("image").getAsString().replace("rtshop:", "/assets/rtshop/");
                        images &= SelfTest.class.getResource(path) != null;
                    }
                    if (pg.has("icons")) {
                        for (com.google.gson.JsonElement ie : pg.getAsJsonArray("icons")) {
                            String iid = ie.getAsString();
                            if (iid.startsWith("rtshop:")) texts &= net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(new ResourceLocation(iid));
                        }
                    }
                }
            }
            check("guidebook: every page has a title and text, its picture exists and its rtshop item icons are real items; the store sells the book again",
                    pages >= 30 && texts && images && guide.getAsJsonArray("categories").size() >= 7
                            && ShopServer.shops().get("general").entries.stream().anyMatch(en -> en.stack.is(com.rtshop.item.ModItems.GUIDEBOOK.get()) && en.buyable),
                    pages + " pages / texts " + texts + " / images " + images);
            check("codex: the book is an item that the general store sells again",
                    com.rtshop.item.ModItems.CODEX.get() != null && ShopServer.shops().get("general") != null
                            && ShopServer.shops().get("general").entries.stream().anyMatch(en -> en.stack.is(com.rtshop.item.ModItems.CODEX.get()) && en.buyable));
        } finally {
            com.rtshop.story.Codex.resetForTest(a);
            com.rtshop.story.Codex.testFakes = false;
        }
    }

    /** 어느 상점에서든 팔 수 있는 가장 높은 기본 매입가 (없으면 -1). */
    private static double sellable(String id) {
        double best = -1;
        for (com.rtshop.shop.ShopData sd : ShopServer.shops().values()) {
            for (com.rtshop.shop.ShopData.Entry en : sd.entries) {
                if (!en.sellable || en.stack.isEmpty()) continue;
                ResourceLocation rl = ForgeRegistries.ITEMS.getKey(en.stack.getItem());
                if (rl != null && rl.toString().equals(id) && en.sellUnit > best) best = en.sellUnit;
            }
        }
        return best;
    }

    /** 어느 상점에서든 살 수 있는 가장 싼 값 (없으면 -1). */
    private static int buyable(MinecraftServer server, String id) {
        int best = -1;
        for (com.rtshop.shop.ShopData sd : ShopServer.shops().values()) {
            for (com.rtshop.shop.ShopData.Entry en : sd.entries) {
                if (!en.buyable || en.stack.isEmpty()) continue;
                ResourceLocation rl = ForgeRegistries.ITEMS.getKey(en.stack.getItem());
                if (rl != null && rl.toString().equals(id) && (best < 0 || en.price < best)) best = en.price;
            }
        }
        return best;
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
            castle(server, level, a, b);
        } catch (Throwable t) {
            crash("castle lots", t);
        }
        try {
            rules(server, level, a);
        } catch (Throwable t) {
            crash("recipe and loot rules", t);
        }
        try {
            enhance(server, level, a);
        } catch (Throwable t) {
            crash("enhance", t);
        }
        try {
            gacha(server, level, a);
        } catch (Throwable t) {
            crash("gacha", t);
        }
        try {
            playerShop(server, level, a, b, new BlockPos(gx + 6, y, gz + 6));
        } catch (Throwable t) {
            crash("player shop", t);
        }
        try {
            plotClear(server, level, a);
            landSweep(level);
        } catch (Throwable t) {
            crash("plot clear", t);
        }
        try {
            pvp(server, level, a, b);
        } catch (Throwable t) {
            crash("pvp", t);
        }
        try {
            raid(server, level, a, b);
        } catch (Throwable t) {
            crash("raid", t);
        }
        try {
            abyss(server, level, a);
        } catch (Throwable t) {
            crash("abyss", t);
        }
        try {
            party(server, level, a, b);
        } catch (Throwable t) {
            crash("party", t);
        }
        try {
            story(server, level, a);
        } catch (Throwable t) {
            crash("story", t);
        }
        try {
            findersAndStats(server, level, a);
        } catch (Throwable t) {
            crash("finders", t);
        }
        try {
            magicGacha(server, level, a);
        } catch (Throwable t) {
            crash("magic", t);
        }
        try {
            storySystems(server, level, a);
            npcLayout(server, level);
        } catch (Throwable t) {
            crash("story systems", t);
        }
        try {
            restaurant(server, level, a, b);
        } catch (Throwable t) {
            crash("restaurant", t);
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


    // ------------------------------------------------------------------ 성 안 토지

    private static LandData.Land testLot(LandData data, String dim, int no, int x0, int z0, int ground) {
        LandData.Land l = data.createPlot(com.rtshop.land.CastlePlots.name(no), 12000);
        l.castleNo = no;
        l.castleGround = ground;
        LandData.Claim c = new LandData.Claim();
        c.dim = dim;
        c.type = "plot";
        c.x0 = x0;
        c.z0 = z0;
        c.x1 = x0 + 15;
        c.z1 = z0 + 15;
        c.y0 = ground + 1 - LandConfig.get().depthBelowFeet;
        c.y1 = 319;
        data.addClaim(l, c);
        return l;
    }

    private static void castle(MinecraftServer server, ServerLevel level, FakePlayer a, FakePlayer b) throws Exception {
        section("castle lots");
        com.rtshop.land.CastlePlots.Def d = com.rtshop.land.CastlePlots.def();
        // 4판: 예전 번호(1~34)는 모두 없어지고, 주인이 표시한 네 구역(A~D)을 빈틈 없이 나눈 칸 모양 토지 (칸마다 약 256블록, 서로 맞닿음)
        boolean overlap = false, sized = true, inside = true, connected = true, whiteClear = true;
        java.util.Set<Integer> nos = new java.util.HashSet<>();
        java.util.Set<Long> allCells = new java.util.HashSet<>();
        double[][] white = {{-24322.7, -5526.0, 5.6}, {-24306.2, -5512.5, 9.5}}; // 건드리지 말라고 표시한 두 건물 (흰 원)
        int touching = 0;
        for (com.rtshop.land.CastlePlots.Tile t : d.tiles) {
            nos.add(t.no);
            if (t.area() < 225 || t.area() > 285) sized = false;
            java.util.Set<Long> mine = new java.util.HashSet<>();
            for (int dz = 0; dz < t.h; dz++) {
                for (int dx = 0; dx < t.w; dx++) {
                    if (!t.has(dx, dz)) continue;
                    int x = t.x0 + dx, z = t.z0 + dz;
                    long k = ((long) x << 32) ^ (z & 0xffffffffL);
                    mine.add(k);
                    if (!allCells.add(k)) overlap = true;
                    if (!com.rtshop.land.CastlePlots.inZone(x, z)) inside = false;
                    for (double[] wcirc : white) {
                        if (Math.hypot(x + 0.5 - wcirc[0], z + 0.5 - wcirc[1]) <= wcirc[2] + 1) whiteClear = false;
                    }
                }
            }
            for (com.rtshop.land.CastlePlots.Obj o : t.objects) {
                for (double[] wcirc : white) {
                    if (Math.hypot(o.x + 0.5 - wcirc[0], o.z + 0.5 - wcirc[1]) <= wcirc[2]) whiteClear = false;
                }
            }
            // 한 덩어리인가
            java.util.ArrayDeque<Long> q = new java.util.ArrayDeque<>();
            java.util.Set<Long> seen = new java.util.HashSet<>();
            if (!mine.isEmpty()) {
                long s0 = mine.iterator().next();
                q.add(s0);
                seen.add(s0);
            }
            while (!q.isEmpty()) {
                long k = q.poll();
                int x = (int) (k >> 32), z = (int) k;
                long[] ns = {((long) (x + 1) << 32) ^ (z & 0xffffffffL), ((long) (x - 1) << 32) ^ (z & 0xffffffffL),
                        ((long) x << 32) ^ ((z + 1) & 0xffffffffL), ((long) x << 32) ^ ((z - 1) & 0xffffffffL)};
                for (long n : ns) {
                    if (mine.contains(n) && seen.add(n)) q.add(n);
                }
            }
            if (seen.size() != mine.size()) connected = false;
        }
        for (com.rtshop.land.CastlePlots.Tile t : d.tiles) {
            boolean touch = false;
            for (int dz = 0; dz < t.h && !touch; dz++) {
                for (int dx = 0; dx < t.w && !touch; dx++) {
                    if (!t.has(dx, dz)) continue;
                    int x = t.x0 + dx, z = t.z0 + dz;
                    for (int[] nb : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                        long k = ((long) (x + nb[0]) << 32) ^ ((z + nb[1]) & 0xffffffffL);
                        if (allCells.contains(k) && !t.has(dx + nb[0], dz + nb[1])) touch = true;
                    }
                }
            }
            if (touch) touching++;
        }
        boolean removed = nos.stream().allMatch(n -> n >= 101 && n < 500);
        check("castle lots v4: only the new lots (A~D), each about 256 blocks and in one piece, inside the castle zone, no cell shared",
                removed && sized && inside && connected && !overlap && d.tiles.size() >= 20,
                d.tiles.size() + " " + removed + " " + sized + " " + inside + " " + connected + " " + overlap);
        check("the lots fill their areas without gaps: every lot touches a neighbour", touching == d.tiles.size(), touching + "/" + d.tiles.size());
        check("no lot (and nothing a lot removes) touches the two buildings marked to be left alone", whiteClear);
        check("lots are named by area: 101 -> A-1, 304 -> C-4", com.rtshop.land.CastlePlots.label(101).equals("A-1")
                && com.rtshop.land.CastlePlots.label(304).equals("C-4") && com.rtshop.land.CastlePlots.label(7).equals("7"));
        check("the castle zone polygon covers the city and the farmland but not across the river",
                com.rtshop.land.CastlePlots.inZone(-24360, -5420) && com.rtshop.land.CastlePlots.inZone(-24200, -5350)
                        && com.rtshop.land.CastlePlots.inZone(-24500, -5460) && !com.rtshop.land.CastlePlots.inZone(-24360, -5650)
                        && !com.rtshop.land.CastlePlots.inZone(-24650, -5400) && !com.rtshop.land.CastlePlots.inZone(-24350, -5100));
        com.rtshop.land.CastlePlots.Tile fullTile = new com.rtshop.land.CastlePlots.Tile();
        long full = com.rtshop.land.CastlePlots.price(fullTile);
        com.rtshop.land.CastlePlots.Tile small = null, large = null;
        for (com.rtshop.land.CastlePlots.Tile t : d.tiles) {
            if (small == null || t.area() < small.area()) small = t;
            if (large == null || t.area() > large.area()) large = t;
        }
        check("a 256-block castle lot costs castlePlotPrice; the price follows the number of blocks",
                full == LandConfig.get().castlePlotPrice && com.rtshop.land.CastlePlots.price(small) < full
                        && com.rtshop.land.CastlePlots.price(large) > full, full + " / " + com.rtshop.land.CastlePlots.price(small));
        LandData data = LandData.get(server);
        boolean anyCastle = false;
        for (LandData.Land l : data.lands()) if (l.castle()) anyCastle = true;
        check("a world that is not the Amonos map gets no castle lots and no castle zone", !anyCastle && !com.rtshop.land.CastlePlots.spawnNear(server)
                && LandManager.guardAt(level, new BlockPos(-24360, 70, -5420)) == null);
        int before = data.castleInstalled;
        data.castleInstalled = com.rtshop.land.CastlePlots.VERSION;
        try {
            LandData.Land z = LandManager.guardAt(level, new BlockPos(-24360, 70, -5420));
            check("with castle lots installed the castle zone is protected: nothing allowed outside bought land",
                    z == LandManager.ZONE && LandManager.perms(a, z) == 0 && LandManager.guardAt(level, new BlockPos(-24650, 70, -5400)) == null);
            check("in the castle zone (outside bought lots) anyone may use doors and buttons, but not chests",
                    com.rtshop.land.LandEvents.zoneDoor(LandManager.ZONE, net.minecraft.world.level.block.Blocks.OAK_DOOR.defaultBlockState())
                            && com.rtshop.land.LandEvents.zoneDoor(LandManager.ZONE, net.minecraft.world.level.block.Blocks.SPRUCE_FENCE_GATE.defaultBlockState())
                            && !com.rtshop.land.LandEvents.zoneDoor(LandManager.ZONE, net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState())
                            && com.rtshop.land.LandEvents.zoneDoor(LandManager.ZONE, net.minecraft.world.level.block.Blocks.STONE_BUTTON.defaultBlockState())
                            && !com.rtshop.land.LandEvents.zoneDoor(LandManager.ZONE, net.minecraft.world.level.block.Blocks.LEVER.defaultBlockState())
                            && !com.rtshop.land.LandEvents.zoneDoor(z, net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState()));
        } finally {
            data.castleInstalled = before;
        }
        check("only land merchants open the lot browser", com.rtshop.land.PlotBrowser.isLandShop("land") && !com.rtshop.land.PlotBrowser.isLandShop("farmer"));
        check("lots can no longer be bought by command", server.getCommands().getDispatcher().parse("rtshop plot buy", server.createCommandSourceStack()).getReader().canRead());

        // 시험용 토지 2칸 (A·B 는 서로 붙어 있음)
        String dim = LandManager.dim(level);
        int X = 3000, Z = 3000, Y = 100;
        net.minecraft.world.level.block.state.BlockState stone = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
        net.minecraft.world.level.block.state.BlockState air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        for (int x = X - 8; x < X + 50; x++) {
            for (int z = Z - 2; z < Z + 18; z++) {
                for (int y = Y - 9; y <= Y + 12; y++) {
                    level.setBlock(new BlockPos(x, y, z), y < Y ? stone : y == Y ? net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState() : air, 2);
                }
            }
        }
        // A 에서 B 로 걸친 건물, A 서쪽 밖의 나무(옆에 가로등), 남의 땅에 걸친 건물 한 칸
        for (int x = X + 10; x <= X + 21; x++)
            for (int y = Y + 1; y <= Y + 5; y++)
                for (int z = Z + 4; z <= Z + 10; z++)
                    level.setBlock(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState(), 2);
        level.setBlock(new BlockPos(X + 5, Y - 2, Z + 5), air, 2);
        for (int y = Y + 1; y <= Y + 4; y++) level.setBlock(new BlockPos(X - 3, y, Z + 8), net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState(), 2);
        for (int x = X - 4; x <= X - 1; x++)
            for (int z = Z + 7; z <= Z + 9; z++)
                level.setBlock(new BlockPos(x, Y + 5, z), net.minecraft.world.level.block.Blocks.OAK_LEAVES.defaultBlockState(), 2);
        level.setBlock(new BlockPos(X - 3, Y + 1, Z + 9), net.minecraft.world.level.block.Blocks.GLOWSTONE.defaultBlockState(), 2);
        level.setBlock(new BlockPos(X + 40, Y + 1, Z + 5), net.minecraft.world.level.block.Blocks.GOLD_BLOCK.defaultBlockState(), 2);
        com.rtshop.land.CastlePlots.Tile ta = new com.rtshop.land.CastlePlots.Tile();
        ta.no = 991;
        ta.x0 = X;
        ta.z0 = Z;
        ta.ground = Y;
        for (int x = X + 16; x <= X + 21; x++)
            for (int z = Z + 4; z <= Z + 10; z++) ta.objects.add(new com.rtshop.land.CastlePlots.Obj(x, z, Y, false));
        for (int x = X - 4; x <= X - 1; x++)
            for (int z = Z + 7; z <= Z + 9; z++) ta.objects.add(new com.rtshop.land.CastlePlots.Obj(x, z, Y, true));
        ta.objects.add(new com.rtshop.land.CastlePlots.Obj(X + 40, Z + 5, Y, false));
        com.rtshop.land.CastlePlots.TEST_TILES.put(991, ta);
        LandData.Land la = testLot(data, dim, 991, X, Z, Y);
        LandData.Land lb = testLot(data, dim, 992, X + 16, Z, Y);
        LandData.Land other = data.createLand(b.getUUID(), "rtshop_test_b");
        LandData.Claim oc = new LandData.Claim();
        oc.dim = dim;
        oc.type = "normal";
        oc.x0 = X + 38;
        oc.x1 = X + 42;
        oc.z0 = Z + 3;
        oc.z1 = Z + 7;
        oc.y0 = Y - 8;
        oc.y1 = 319;
        data.addClaim(other, oc);
        try {
            LandData.Claim ca = PlotManager.plotClaim(la);
            check("castle lot height works like normal land (from depthBelowFeet under the ground up to the sky)",
                    ca.y0 == Y + 1 - LandConfig.get().depthBelowFeet && ca.y1 == 319 && ca.mask == null);
            com.rtshop.net.PlotPreviewPacket pv = com.rtshop.land.PlotBrowser.render(level, la, a.getUUID());
            int nPlot = 0, nClear = 0;
            for (byte f : pv.flags) {
                if ((f & com.rtshop.land.PlotBrowser.FLAG_PLOT) != 0) nPlot++;
                if ((f & com.rtshop.land.PlotBrowser.FLAG_CLEAR) != 0) nClear++;
            }
            check("the preview marks the whole lot (no holes) and the outside parts of objects that will be removed",
                    nPlot == 256 && nClear >= 40 && nClear < 60, nPlot + " " + nClear);

            Economy.setWallet(a, 1_000_000);
            boolean bought = PlotManager.buyLand(a, la);
            check("buying a castle lot from the merchant works", bought && la.owner.equals(a.getUUID()) && la.defaultPerms == 0
                    && Economy.wallet(a) == 1_000_000 - 12000);
            boolean gone = true;
            for (int x = X + 10; x <= X + 21; x++)
                for (int y = Y + 1; y <= Y + 5; y++)
                    for (int z = Z + 4; z <= Z + 10; z++)
                        if (!level.getBlockState(new BlockPos(x, y, z)).isAir()) gone = false;
            check("a building crossing the border is removed whole, also the part outside the lot", gone);
            check("a tree touching the lot is removed whole (logs and leaves) but other things next to it stay",
                    level.getBlockState(new BlockPos(X - 3, Y + 3, Z + 8)).isAir() && level.getBlockState(new BlockPos(X - 4, Y + 5, Z + 7)).isAir()
                            && level.getBlockState(new BlockPos(X - 3, Y + 1, Z + 9)).is(net.minecraft.world.level.block.Blocks.GLOWSTONE));
            check("objects are never removed from someone else's land",
                    level.getBlockState(new BlockPos(X + 40, Y + 1, Z + 5)).is(net.minecraft.world.level.block.Blocks.GOLD_BLOCK));
            check("the lot is flat grass with dirt below; holes under it are filled",
                    level.getBlockState(new BlockPos(X + 12, Y, Z + 6)).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)
                            && level.getBlockState(new BlockPos(X + 8, Y - 1, Z + 6)).is(net.minecraft.world.level.block.Blocks.DIRT)
                            && level.getBlockState(new BlockPos(X + 5, Y - 2, Z + 5)).is(net.minecraft.world.level.block.Blocks.DIRT));
            check("the neighbour lot stays for sale", lb.forSale() && !lb.cleared && la.cleared);
            check("the look before buying is backed up", Files.exists(com.rtshop.land.CastlePlots.backupFile(server, la)));
            PlotManager.giveBack(a, la);
            check("giving a castle lot back keeps it level and puts it up for sale again (owner-only rules)", la.forSale() && la.cleared && la.defaultPerms == 0);
            boolean restored = com.rtshop.land.CastlePlots.restore(server, la);
            check("an OP can restore a lot and the removed objects (backup)", restored && !la.cleared
                    && level.getBlockState(new BlockPos(X + 12, Y + 3, Z + 6)).is(net.minecraft.world.level.block.Blocks.STONE_BRICKS)
                    && level.getBlockState(new BlockPos(X + 18, Y + 3, Z + 6)).is(net.minecraft.world.level.block.Blocks.STONE_BRICKS)
                    && level.getBlockState(new BlockPos(X - 3, Y + 3, Z + 8)).is(net.minecraft.world.level.block.Blocks.OAK_LOG));
        } finally {
            com.rtshop.land.CastlePlots.TEST_TILES.clear();
            for (LandData.Land l : new LandData.Land[]{la, lb, other}) {
                try {
                    Files.deleteIfExists(com.rtshop.land.CastlePlots.backupFile(server, l));
                    Files.deleteIfExists(com.rtshop.land.CastlePlots.backupFile(server, l).resolveSibling("castle_" + l.castleNo + "_objects.nbt"));
                } catch (Exception ignored) {
                }
                data.removeLand(l);
            }
        }

        // 3판으로 올리기: 주인 없는 예전 칸은 지우고, 주인 있는 칸은 남기고, 새 칸을 넣는다
        int was = data.castleInstalled;
        LandData.Land oldFree = testLot(data, dim, 7, 5000, 5000, 100);
        LandData.Land oldOwned = testLot(data, dim, 8, 5040, 5000, 100);
        oldOwned.owner = b.getUUID();
        data.castleInstalled = 2;
        try {
            int[] up = com.rtshop.land.CastlePlots.upgrade(server);
            int fresh = 0;
            boolean oldGone = true, ownedKept = false;
            for (LandData.Land l : data.lands()) {
                if (!l.castle()) continue;
                if (l.castleNo >= 100) fresh++;
                if (l.castleNo == 7) oldGone = false;
                if (l.castleNo == 8 && l.owner.equals(b.getUUID())) ownedKept = true;
            }
            check("moving to the new castle layout removes ownerless old lots, keeps owned ones and adds the new lots",
                    oldGone && ownedKept && fresh == d.tiles.size() && up[0] == d.tiles.size() && up[1] == 1
                            && data.castleInstalled == com.rtshop.land.CastlePlots.VERSION, fresh + " " + up[0] + "/" + up[1] + " " + oldGone + " " + ownedKept);
            LandData.Land a1 = com.rtshop.land.CastlePlots.landOf(data, 101);
            check("new lots are named by area (성 안 토지 A-1) and are for sale", a1 != null && a1.forSale() && a1.name.equals("성 안 토지 A-1"),
                    a1 == null ? null : a1.name);
            LandData.Claim c1 = a1 == null ? null : PlotManager.plotClaim(a1);
            com.rtshop.land.CastlePlots.Tile t1 = com.rtshop.land.CastlePlots.tile(101);
            int cells = 0;
            if (c1 != null) {
                for (int x = c1.x0; x <= c1.x1; x++) for (int z = c1.z0; z <= c1.z1; z++) if (c1.hasColumn(x, z)) cells++;
            }
            check("a lot's land is exactly its own cells (not its bounding box), so touching lots do not overlap",
                    c1 != null && c1.mask != null && cells == t1.area() && cells < (c1.x1 - c1.x0 + 1) * (c1.z1 - c1.z0 + 1), cells);
        } finally {
            List<LandData.Land> gone = new ArrayList<>();
            for (LandData.Land l : data.lands()) {
                if (l.castle()) gone.add(l);
            }
            gone.forEach(data::removeLand);
            data.castleInstalled = was;
        }
    }

    // ------------------------------------------------------------------ 농경지를 사면 작물·구조물을 치운다

    private static void plotClear(MinecraftServer server, ServerLevel level, FakePlayer a) throws Exception {
        section("farmland lot is cleared on purchase");
        LandData data = LandData.get(server);
        int X = 3200, Z = 3200, Y = 100;
        net.minecraft.world.level.block.Block[] B = {net.minecraft.world.level.block.Blocks.STONE, net.minecraft.world.level.block.Blocks.FARMLAND,
                net.minecraft.world.level.block.Blocks.WHEAT, net.minecraft.world.level.block.Blocks.GRASS_BLOCK, net.minecraft.world.level.block.Blocks.OAK_FENCE,
                net.minecraft.world.level.block.Blocks.WATER, net.minecraft.world.level.block.Blocks.OAK_PLANKS, net.minecraft.world.level.block.Blocks.AIR};
        for (int x = X - 2; x <= X + 9; x++) {
            for (int z = Z - 2; z <= Z + 9; z++) {
                // 모드팩 전체의 세계는 씨앗이 매번 다르다: 이 자리가 산속이면 Y+8 위에 지형이 남아 clearPlot 이 그 꼭대기를 "땅"으로 본다.
                // 그러니 그 칸의 지표면까지 전부 비운다.
                int topY = Math.max(Y + 8, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z));
                for (int y = topY; y >= Y - 3; y--) level.setBlock(new BlockPos(x, y, z), (y < Y ? B[0] : B[7]).defaultBlockState(), 2);
                level.setBlock(new BlockPos(x, Y, z), B[1].defaultBlockState(), 2);
                level.setBlock(new BlockPos(x, Y + 1, z), B[2].defaultBlockState(), 2);
            }
        }
        level.setBlock(new BlockPos(X + 2, Y, Z + 2), B[5].defaultBlockState(), 2); // 물길
        level.setBlock(new BlockPos(X + 2, Y + 1, Z + 2), B[7].defaultBlockState(), 2);
        level.setBlock(new BlockPos(X + 4, Y, Z + 4), B[3].defaultBlockState(), 2); // 허수아비 자리: 풀 위 울타리 + 판자
        level.setBlock(new BlockPos(X + 4, Y + 1, Z + 4), B[4].defaultBlockState(), 2);
        level.setBlock(new BlockPos(X + 4, Y + 2, Z + 4), B[4].defaultBlockState(), 2);
        level.setBlock(new BlockPos(X + 4, Y + 3, Z + 4), B[6].defaultBlockState(), 2);
        // 토지 밖(둘레)과 토지가 아닌 한 칸에는 흔들리지 않는 표지(판자)를 둔다: 밀은 막 만든 청크의 빛 계산이 늦으면 이웃 갱신만으로도 뽑힌다
        for (BlockPos mk : new BlockPos[]{new BlockPos(X + 7, Y + 1, Z + 7), new BlockPos(X - 1, Y + 1, Z + 3), new BlockPos(X + 8, Y + 1, Z + 3)}) {
            level.setBlock(mk, B[6].defaultBlockState(), 2);
        }
        LandData.Land l = data.createPlot("시험 밭", 1000);
        LandData.Claim c = new LandData.Claim();
        c.dim = LandManager.dim(level);
        c.type = "plot";
        c.x0 = X;
        c.z0 = Z;
        c.x1 = X + 7;
        c.z1 = Z + 7;
        c.y0 = level.getMinBuildHeight();
        c.y1 = level.getMaxBuildHeight() - 1;
        java.util.BitSet m = new java.util.BitSet(64);
        m.set(0, 64);
        m.clear(7 * 8 + 7); // 한 칸은 토지가 아님
        c.mask = m;
        data.addClaim(l, c);
        try {
            Economy.setWallet(a, 5000);
            boolean bought = PlotManager.buyLand(a, l);
            check("buying a farmland lot removes the crops and structures standing on it",
                    bought && level.getBlockState(new BlockPos(X + 1, Y + 1, Z + 1)).isAir() && level.getBlockState(new BlockPos(X + 4, Y + 1, Z + 4)).isAir()
                            && level.getBlockState(new BlockPos(X + 4, Y + 3, Z + 4)).isAir(),
                    bought + " " + level.getBlockState(new BlockPos(X + 1, Y + 1, Z + 1)) + " " + level.getBlockState(new BlockPos(X + 4, Y + 1, Z + 4))
                            + " " + level.getBlockState(new BlockPos(X + 4, Y + 3, Z + 4)) + " ground=" + level.getBlockState(new BlockPos(X + 1, Y, Z + 1))
                            + " surface=" + level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, X + 4, Z + 4) + " (Y=" + Y + ")");
            check("...but keeps the farmland, the ground and the water channel",
                    level.getBlockState(new BlockPos(X + 1, Y, Z + 1)).is(B[1]) && level.getBlockState(new BlockPos(X + 4, Y, Z + 4)).is(B[3])
                            && level.getBlockState(new BlockPos(X + 2, Y, Z + 2)).is(B[5]) && level.getBlockState(new BlockPos(X + 1, Y - 1, Z + 1)).is(B[0]));
            check("...and touches nothing outside the lot's own cells",
                    level.getBlockState(new BlockPos(X + 7, Y + 1, Z + 7)).is(B[6]) && level.getBlockState(new BlockPos(X - 1, Y + 1, Z + 3)).is(B[6])
                            && level.getBlockState(new BlockPos(X + 8, Y + 1, Z + 3)).is(B[6]),
                    level.getBlockState(new BlockPos(X + 7, Y + 1, Z + 7)) + " " + level.getBlockState(new BlockPos(X - 1, Y + 1, Z + 3)) + " "
                            + level.getBlockState(new BlockPos(X + 8, Y + 1, Z + 3)));
            boolean backup = false;
            java.nio.file.Path dir = PlotManager.plotBackupDir(server);
            if (Files.isDirectory(dir)) {
                try (java.util.stream.Stream<java.nio.file.Path> st = Files.list(dir)) {
                    for (java.nio.file.Path f : st.toList()) {
                        if (f.getFileName().toString().startsWith("plot_" + l.id + "_")) {
                            backup = true;
                            Files.deleteIfExists(f);
                        }
                    }
                }
            }
            check("the removed blocks are saved as a backup", backup);
        } finally {
            data.removeLand(l);
            for (int x = X - 2; x <= X + 9; x++)
                for (int z = Z - 2; z <= Z + 9; z++)
                    for (int y = Y - 3; y <= Y + 8; y++) level.setBlock(new BlockPos(x, y, z), B[7].defaultBlockState(), 2);
        }
    }

    // ------------------------------------------------------------------ 토지를 비운 뒤의 뒷정리 (잘린 나무·뜬 조각)

    private static void landSweep(ServerLevel level) {
        section("land sweep");
        int X = 3300, Z = 3300, Y = 100;
        net.minecraft.world.level.block.state.BlockState air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),
                dirt = net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState(), log = net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState(),
                leaf = net.minecraft.world.level.block.Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true),
                plank = net.minecraft.world.level.block.Blocks.OAK_PLANKS.defaultBlockState(), fence = net.minecraft.world.level.block.Blocks.OAK_FENCE.defaultBlockState();
        it.unimi.dsi.fastutil.longs.LongOpenHashSet gone = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
        try {
            // 모드팩 전체에서는 세계의 씨앗이 매번 달라, 이 자리에 산이 솟아 있으면 잎을 놓을 빈칸이 없어 시험이 틀어진다 → 먼저 위쪽을 비운다
            for (int x = X - 6; x <= X + 27; x++) for (int z = Z - 6; z <= Z + 27; z++) for (int y = Y + 1; y <= Y + 12; y++) {
                BlockPos cp = new BlockPos(x, y, z);
                if (!level.getBlockState(cp).isAir()) level.setBlock(cp, air, 2);
            }
            for (int x = X - 4; x <= X + 25; x++) for (int z = Z - 4; z <= Z + 25; z++) level.setBlock(new BlockPos(x, Y, z), dirt, 2);
            // 나무 둘 (줄기 5칸 + 잎 5x5x3): 하나는 건드려도 되는 자리, 하나는 남의 토지(touch = false)
            for (int tz : new int[]{Z + 8, Z - 1}) {
                for (int y = Y + 1; y <= Y + 5; y++) level.setBlock(new BlockPos(X + 8, y, tz), log, 2);
                for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) for (int y = Y + 4; y <= Y + 6; y++) {
                    BlockPos p = new BlockPos(X + 8 + dx, y, tz + dz);
                    if (level.getBlockState(p).isAir()) level.setBlock(p, leaf, 2);
                }
                // 토지(x <= X+6) 안으로 뻗은 잎은 토지를 비울 때 잘려 나갔다
                for (int dz = -2; dz <= 2; dz++) for (int y = Y + 4; y <= Y + 6; y++) {
                    BlockPos p = new BlockPos(X + 6, y, tz + dz);
                    level.setBlock(p, air, 2);
                    gone.add(p.asLong());
                }
            }
            // 통나무 벽 (너비 13칸, 잎 없음): 나무가 아니다
            for (int x = X + 10; x <= X + 22; x++) for (int y = Y + 1; y <= Y + 3; y++) level.setBlock(new BlockPos(x, y, Z + 20), log, 2);
            gone.add(BlockPos.asLong(X + 9, Y + 1, Z + 20));
            // 공중에 뜬 판자 두 장, 땅에 선 울타리 기둥
            level.setBlock(new BlockPos(X + 2, Y + 5, Z + 15), plank, 2);
            level.setBlock(new BlockPos(X + 2, Y + 6, Z + 15), plank, 2);
            gone.add(BlockPos.asLong(X + 1, Y + 5, Z + 15));
            for (int y = Y + 1; y <= Y + 3; y++) level.setBlock(new BlockPos(X + 2, y, Z + 2), fence, 2);
            gone.add(BlockPos.asLong(X + 1, Y + 2, Z + 2));
            final int line = Z + 2;
            com.rtshop.land.LandSweep.Result r = com.rtshop.land.LandSweep.run(level, gone, (x, y, z) -> z >= line, 10, null);
            check("a tree cut by the lot's edge is removed whole (trunk and leaves); the same tree on someone else's land is left alone",
                    r.trees == 1 && level.getBlockState(new BlockPos(X + 8, Y + 1, Z + 8)).isAir() && level.getBlockState(new BlockPos(X + 8, Y + 5, Z + 8)).isAir()
                            && level.getBlockState(new BlockPos(X + 10, Y + 4, Z + 10)).isAir() && level.getBlockState(new BlockPos(X + 7, Y + 6, Z + 6)).isAir()
                            && level.getBlockState(new BlockPos(X + 8, Y + 1, Z - 1)).is(log.getBlock()) && level.getBlockState(new BlockPos(X + 10, Y + 4, Z - 1)).is(leaf.getBlock()),
                    r + " trunk " + level.getBlockState(new BlockPos(X + 8, Y + 1, Z + 8)) + " leaf " + level.getBlockState(new BlockPos(X + 10, Y + 4, Z + 10))
                            + " other " + level.getBlockState(new BlockPos(X + 8, Y + 1, Z - 1)));
            check("a piece left floating beside the cleared space is removed; a log wall, a post standing on the ground and the ground itself stay",
                    r.floating == 1 && level.getBlockState(new BlockPos(X + 2, Y + 5, Z + 15)).isAir() && level.getBlockState(new BlockPos(X + 2, Y + 6, Z + 15)).isAir()
                            && level.getBlockState(new BlockPos(X + 10, Y + 1, Z + 20)).is(log.getBlock()) && level.getBlockState(new BlockPos(X + 22, Y + 3, Z + 20)).is(log.getBlock())
                            && level.getBlockState(new BlockPos(X + 2, Y + 1, Z + 2)).is(fence.getBlock()) && level.getBlockState(new BlockPos(X + 8, Y, Z + 8)).is(dirt.getBlock()),
                    r + " plank " + level.getBlockState(new BlockPos(X + 2, Y + 5, Z + 15)) + " wall " + level.getBlockState(new BlockPos(X + 10, Y + 1, Z + 20))
                            + " post " + level.getBlockState(new BlockPos(X + 2, Y + 1, Z + 2)));
        } finally {
            for (int x = X - 4; x <= X + 25; x++) for (int z = Z - 4; z <= Z + 25; z++) for (int y = Y; y <= Y + 8; y++) level.setBlock(new BlockPos(x, y, z), air, 2);
        }
    }

    // ------------------------------------------------------------------ PvP 켜기/끄기

    private static void pvp(MinecraftServer server, ServerLevel level, FakePlayer a, FakePlayer b) throws Exception {
        section("pvp toggle");
        com.rtshop.pvp.PvpConfig cfg = new com.rtshop.pvp.PvpConfig();
        com.rtshop.pvp.PvpConfig.setForTest(cfg);
        LandData data = LandData.get(server);
        int installedBefore = data.castleInstalled;
        try {
            com.rtshop.pvp.Pvp.forgetForTest(a);
            com.rtshop.pvp.Pvp.forgetForTest(b);
            a.setPos(700.5, 100, 700.5);
            b.setPos(703.5, 100, 700.5);
            check("by default there is no PvP anywhere: PvP cannot be turned on outside a PvP zone",
                    com.rtshop.pvp.Pvp.area(a) == com.rtshop.pvp.Pvp.AREA_OFF && !com.rtshop.pvp.Pvp.set(a, true, false) && !com.rtshop.pvp.Pvp.on(a)
                            && "rtshop.pvp.hit_area".equals(com.rtshop.pvp.Pvp.denyReason(a, b)));
            com.rtshop.pvp.PvpConfig.Zone zone = new com.rtshop.pvp.PvpConfig.Zone();
            zone.name = "arena";
            zone.dim = com.rtshop.pvp.Pvp.dim(level);
            zone.x0 = 690;
            zone.x1 = 710;
            zone.z0 = 690;
            zone.z1 = 710;
            cfg.zones.add(zone);
            check("inside a PvP zone PvP can be turned on; hits need both players on",
                    com.rtshop.pvp.Pvp.area(a) == com.rtshop.pvp.Pvp.AREA_ZONE && "rtshop.pvp.hit_self_off".equals(com.rtshop.pvp.Pvp.denyReason(a, b))
                            && com.rtshop.pvp.Pvp.set(a, true, false) && com.rtshop.pvp.Pvp.on(a)
                            && "rtshop.pvp.hit_other_off".equals(com.rtshop.pvp.Pvp.denyReason(a, b))
                            && com.rtshop.pvp.Pvp.set(b, true, false) && com.rtshop.pvp.Pvp.denyReason(a, b) == null
                            && com.rtshop.pvp.Pvp.denyReason(b, a) == null);
            com.rtshop.pvp.Pvp.combat(a);
            boolean lockedOff = !com.rtshop.pvp.Pvp.set(a, false, false) && com.rtshop.pvp.Pvp.on(a) && com.rtshop.pvp.Pvp.lockSeconds(a) > 0;
            com.rtshop.pvp.Pvp.testClockOffset += (cfg.combatSeconds + 1) * 1000L;
            check("PvP cannot be turned off during combat, only after the combat lock",
                    lockedOff && com.rtshop.pvp.Pvp.lockSeconds(a) == 0 && com.rtshop.pvp.Pvp.set(a, false, false) && !com.rtshop.pvp.Pvp.on(a));
            com.rtshop.pvp.Pvp.testClockOffset += 5000;
            com.rtshop.pvp.Pvp.set(a, true, false);
            // 화살·길들인 동물도 쏜 사람·주인의 공격으로 친다
            net.minecraft.world.entity.projectile.Arrow arrow = new net.minecraft.world.entity.projectile.Arrow(level, a);
            net.minecraft.world.entity.animal.Wolf wolf = net.minecraft.world.entity.EntityType.WOLF.create(level);
            check("arrows count as their shooter's attack",
                    com.rtshop.pvp.Pvp.attackerOf(level.damageSources().arrow(arrow, a), server) == a
                            && com.rtshop.pvp.Pvp.attackerOf(level.damageSources().playerAttack(b), server) == b
                            && wolf != null && com.rtshop.pvp.Pvp.attackerOf(level.damageSources().mobAttack(wolf), server) == null);
            // 구역을 벗어나면 저절로 꺼진다
            a.setPos(900.5, 100, 900.5);
            com.rtshop.pvp.Pvp.tick(a);
            check("leaving the PvP zone turns PvP off by itself", !com.rtshop.pvp.Pvp.on(a) && com.rtshop.pvp.Pvp.area(a) == com.rtshop.pvp.Pvp.AREA_OFF);
            // 마을 밖 PvP 를 켜면 구역 밖에서도 가능
            cfg.outside = true;
            com.rtshop.pvp.Pvp.testClockOffset += 5000;
            check("with 'outside' on, PvP can be turned on anywhere outside safe zones",
                    com.rtshop.pvp.Pvp.area(a) == com.rtshop.pvp.Pvp.AREA_WILD && com.rtshop.pvp.Pvp.set(a, true, false) && com.rtshop.pvp.Pvp.denyReason(a, b) == null);
            // 안전 구역: 누구도 PvP 불가, 들어가면 꺼진다
            com.rtshop.pvp.PvpConfig.Zone safe = new com.rtshop.pvp.PvpConfig.Zone();
            safe.name = "town";
            safe.dim = zone.dim;
            safe.x0 = 890;
            safe.x1 = 910;
            safe.z0 = 890;
            safe.z1 = 910;
            safe.safe = true;
            cfg.zones.add(safe);
            boolean safeHit = "rtshop.pvp.hit_safe".equals(com.rtshop.pvp.Pvp.denyReason(a, b)) && "rtshop.pvp.hit_safe".equals(com.rtshop.pvp.Pvp.denyReason(b, a));
            com.rtshop.pvp.Pvp.tick(a);
            com.rtshop.pvp.Pvp.testClockOffset += 5000;
            check("a safe zone blocks every hit, turns PvP off on entry and refuses /pvp on",
                    safeHit && !com.rtshop.pvp.Pvp.on(a) && com.rtshop.pvp.Pvp.area(a) == com.rtshop.pvp.Pvp.AREA_SAFE && !com.rtshop.pvp.Pvp.set(a, true, false));
            // 성 영역은 늘 안전 구역 (성 안 토지를 넣은 세계)
            data.castleInstalled = com.rtshop.land.CastlePlots.VERSION;
            ServerLevel over = server.overworld();
            check("the castle area is always a safe zone, even with 'outside' on",
                    com.rtshop.pvp.Pvp.area(over, -24360, -5420) == com.rtshop.pvp.Pvp.AREA_SAFE
                            && com.rtshop.pvp.Pvp.area(over, -24650, -5400) == com.rtshop.pvp.Pvp.AREA_WILD);
            data.castleInstalled = installedBefore;
            // 설정 저장·읽기
            com.rtshop.pvp.PvpConfig back = com.rtshop.pvp.PvpConfig.parse(cfg.toJson());
            check("PvP settings and zones survive saving and loading", back.outside && back.zones.size() == 2 && back.zone("town").safe
                    && !back.zone("arena").safe && back.zone("arena").x1 == 710 && back.combatSeconds == cfg.combatSeconds);
            check("the /pvp command exists for everyone and /rtshop pvp for OPs",
                    !server.getCommands().getDispatcher().parse("pvp on", server.createCommandSourceStack()).getReader().canRead()
                            && !server.getCommands().getDispatcher().parse("rtshop pvp outside on", server.createCommandSourceStack()).getReader().canRead()
                            && !server.getCommands().getDispatcher().parse("rtshop pvp zone here test 30", server.createCommandSourceStack()).getReader().canRead());
        } finally {
            data.castleInstalled = installedBefore;
            com.rtshop.pvp.Pvp.testClockOffset = 0;
            com.rtshop.pvp.Pvp.forgetForTest(a);
            com.rtshop.pvp.Pvp.forgetForTest(b);
            com.rtshop.pvp.PvpConfig.reload();
        }
    }

    // ------------------------------------------------------------------ 주간 레이드

    private static void raid(MinecraftServer server, ServerLevel level, FakePlayer a, FakePlayer b) throws Exception {
        section("weekly raid");
        java.nio.file.Path file = FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("raid.json");
        byte[] fileBefore = Files.exists(file) ? Files.readAllBytes(file) : null;
        com.rtshop.raid.RaidConfig c = com.rtshop.raid.RaidConfig.fresh();
        c.boss = "minecraft:zombie";
        c.requireKill = "minecraft:pig";
        c.requireKills = 1;
        c.healthMultiplier = 3.0;
        c.damageMultiplier = 2.0;
        c.arenaDim = com.rtshop.pvp.Pvp.dim(level);
        c.arenaX = 6000;
        c.arenaY = 150;
        c.arenaZ = 6000;
        c.arenaRadius = 12;
        c.arenaHeight = 12;
        c.returnSet = true;
        c.returnX = 100.5;
        c.returnY = 100;
        c.returnZ = 100.5;
        c.weeklyGold = 777;
        com.rtshop.raid.RaidConfig.setForTest(c);
        com.rtshop.raid.RaidData data = com.rtshop.raid.RaidData.get(server);
        Item ticket = com.rtshop.item.ModItems.RAID_TICKET.get(), eye = com.rtshop.item.ModItems.RAID_EYE.get(), esc = com.rtshop.item.ModItems.RAID_ESCAPE.get();
        try {
            com.rtshop.raid.Raid.resetForTest(server);
            com.rtshop.raid.Raid.TEST_PLAYERS.add(a);
            com.rtshop.raid.Raid.TEST_PLAYERS.add(b);
            com.rtshop.raid.RaidConfig back = com.rtshop.raid.RaidConfig.parse(c.toJson());
            check("raid settings, texts and rewards survive saving and loading", back.boss.equals("minecraft:zombie") && back.healthMultiplier == 3.0
                    && back.rewards.size() == c.rewards.size() && back.text("successTitle").equals(c.text("successTitle")) && back.chanceFresh[5] == 0.05
                    && back.weeklyGold == 777 && back.returnSet);
            // 주간 초기화: 월요일 오전 6시
            java.time.ZonedDateTime wed = java.time.ZonedDateTime.of(2026, 11, 25, 10, 0, 0, 0, java.time.ZoneId.systemDefault());
            long ws = com.rtshop.raid.Weekly.weekStart(wed.toInstant().toEpochMilli(), 1, 6);
            java.time.ZonedDateTime start = java.time.Instant.ofEpochMilli(ws).atZone(java.time.ZoneId.systemDefault());
            long monEarly = java.time.ZonedDateTime.of(2026, 11, 30, 5, 59, 0, 0, java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            long monLate = java.time.ZonedDateTime.of(2026, 11, 30, 6, 1, 0, 0, java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            check("the week starts on Monday 06:00 (weekly rewards reset then)", start.getDayOfWeek() == java.time.DayOfWeek.MONDAY && start.getHour() == 6
                    && start.getDayOfMonth() == 23 && com.rtshop.raid.Weekly.id(monEarly, 1, 6) == com.rtshop.raid.Weekly.id(wed.toInstant().toEpochMilli(), 1, 6)
                    && com.rtshop.raid.Weekly.id(monLate, 1, 6) != com.rtshop.raid.Weekly.id(monEarly, 1, 6), start);
            // 입장 조건
            clear(a);
            clear(b);
            data.of(a.getUUID()).weeklyWeek = -1;
            com.rtshop.raid.Raid.setKills(a, 0);
            a.setPos(100.5, 100, 100.5);
            String why0 = com.rtshop.raid.Raid.cannotEnter(a);
            com.rtshop.raid.Raid.setKills(a, 1);
            String why1 = com.rtshop.raid.Raid.cannotEnter(a);
            a.getInventory().add(new ItemStack(ticket, 3));
            check("entering needs one regular boss kill and a raid ticket", why0 != null && why0.equals(c.text("needKill", 1, 0)) && why1 != null
                    && why1.equals(c.text("needTicket")) && com.rtshop.raid.Raid.cannotEnter(a) == null, why0 + " / " + why1);
            boolean in = com.rtshop.raid.Raid.enter(a);
            BlockPos altar = com.rtshop.raid.Raid.altarPos();
            check("entering uses a ticket, builds the arena with an altar in the middle and hands out the summoning eye and the escape ticket",
                    in && count(a, s -> s.is(ticket)) == 2 && count(a, s -> s.is(eye)) == 1 && count(a, s -> s.is(esc)) == 1 && com.rtshop.raid.Raid.inArena(a)
                            && !level.getBlockState(altar).isAir() && level.getBlockState(altar.below()).is(net.minecraft.world.level.block.Blocks.GILDED_BLACKSTONE)
                            && level.getBlockState(new BlockPos(6000 + 13, 152, 6000)).is(net.minecraft.world.level.block.Blocks.POLISHED_BLACKSTONE_BRICKS)
                            && level.getBlockState(new BlockPos(6000 + 13, 158, 6000)).is(net.minecraft.world.level.block.Blocks.BARRIER),
                    in + " " + count(a, s -> s.is(ticket)) + " " + level.getBlockState(altar));
            // 구역 보호: 부수기·놓기 불가, PvP 불가
            net.minecraftforge.event.level.BlockEvent.BreakEvent be = new net.minecraftforge.event.level.BlockEvent.BreakEvent(level, altar.below(), level.getBlockState(altar.below()), a);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(be);
            check("blocks in the raid arena cannot be broken, and there is no PvP there", be.isCanceled()
                    && com.rtshop.pvp.Pvp.area(level, 6000, 6000) == com.rtshop.pvp.Pvp.AREA_SAFE);
            // 소환
            boolean summoned = com.rtshop.raid.Raid.summon(a);
            net.minecraft.world.entity.Entity boss = level.getEntity(com.rtshop.raid.Raid.bossIdForTest());
            float bossTakes = -1;
            if (boss instanceof net.minecraft.world.entity.LivingEntity le0) {
                net.minecraftforge.event.entity.living.LivingHurtEvent bh = new net.minecraftforge.event.entity.living.LivingHurtEvent(le0, level.damageSources().magic(), 9F);
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(bh);
                bossTakes = bh.getAmount();
            }
            check("using the eye on the altar wakes the boss; it takes 1/3 damage (= three times the health, without hitting the 1024 health limit)",
                    summoned && com.rtshop.raid.Raid.state() == com.rtshop.raid.Raid.ACTIVE && boss instanceof net.minecraft.world.entity.LivingEntity le
                            && le.getHealth() == le.getMaxHealth() && boss.getTags().contains(com.rtshop.raid.Raid.BOSS_TAG) && Math.abs(bossTakes - 3F) < 0.01,
                    bossTakes);
            // 보스가 깨어난 뒤에는 들어올 수 없다
            com.rtshop.raid.Raid.setKills(b, 1);
            b.getInventory().add(new ItemStack(ticket, 1));
            b.setPos(100.5, 100, 100.5);
            check("once the boss is awake nobody else can enter", !com.rtshop.raid.Raid.enter(b) && count(b, s -> s.is(ticket)) == 1
                    && c.text("locked").equals(com.rtshop.raid.Raid.cannotEnter(b)));
            // 공격력 배율
            if (boss instanceof net.minecraft.world.entity.LivingEntity le) {
                net.minecraftforge.event.entity.living.LivingHurtEvent he = new net.minecraftforge.event.entity.living.LivingHurtEvent(a, level.damageSources().mobAttack(le), 4F);
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(he);
                net.minecraft.world.entity.monster.Zombie plain = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
                net.minecraftforge.event.entity.living.LivingHurtEvent control = new net.minecraftforge.event.entity.living.LivingHurtEvent(a, level.damageSources().mobAttack(plain), 4F);
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(control);
                check("the raid boss hits harder by the configured factor", control.getAmount() > 0 && Math.abs(he.getAmount() - control.getAmount() * 2F) < 0.01,
                        he.getAmount() + " vs " + control.getAmount());
            }
            // 아무도 남지 않으면 보스는 사라지고, 처치로 치지 않는다
            long walletBefore = Economy.wallet(a);
            boolean escaped = com.rtshop.raid.Raid.escape(a);
            com.rtshop.raid.Raid.tick(server);
            check("the escape ticket returns the player; with nobody left the boss disappears without counting as a kill",
                    escaped && !com.rtshop.raid.Raid.inArena(a) && Math.abs(a.getX() - 100.5) < 0.01 && count(a, s -> s.is(eye) || s.is(esc)) == 0
                            && com.rtshop.raid.Raid.state() == com.rtshop.raid.Raid.IDLE && (boss == null || !boss.isAlive())
                            && !com.rtshop.raid.Raid.weeklyClaimed(a) && Economy.wallet(a) == walletBefore && !com.rtshop.raid.Raid.wasRewarded());
            // 성공: 주간 보상 + 드랍 (가장 높은 등급은 서버 전체에 알림)
            com.rtshop.raid.Raid.enter(a);
            com.rtshop.raid.Raid.summon(a);
            com.rtshop.raid.Raid.testRandom = () -> 0.0;
            com.rtshop.raid.Raid.lastBroadcast = "";
            walletBefore = Economy.wallet(a);
            com.rtshop.raid.Raid.bossDead(server, true);
            check("a kill by a player pays the weekly reward once and rolls the boss drops (top grade is announced with its custom name)",
                    com.rtshop.raid.Raid.state() == com.rtshop.raid.Raid.DONE && com.rtshop.raid.Raid.weeklyClaimed(a) && Economy.wallet(a) == walletBefore + 777
                            && count(a, s -> s.is(Items.DIAMOND)) == 8 && count(a, s -> s.is(Items.NETHER_STAR)) == c.rollsFresh
                            && com.rtshop.raid.Raid.lastBroadcast.contains(c.text("mysteryName")),
                    Economy.wallet(a) - walletBefore + " " + count(a, s -> s.is(Items.NETHER_STAR)) + " " + com.rtshop.raid.Raid.lastBroadcast);
            // 이동 투표: 구역 안의 모두가 동의하면 바로 돌아간다
            com.rtshop.raid.Raid.tick(server);
            boolean waits = com.rtshop.raid.Raid.state() == com.rtshop.raid.Raid.DONE && com.rtshop.raid.Raid.inArena(a);
            com.rtshop.raid.Raid.vote(a);
            com.rtshop.raid.Raid.tick(server);
            check("after the kill players wait for the countdown, or leave at once when everyone agrees", waits && !com.rtshop.raid.Raid.inArena(a)
                    && com.rtshop.raid.Raid.state() == com.rtshop.raid.Raid.IDLE && Math.abs(a.getZ() - 100.5) < 0.01);
            // 같은 주에 다시: 주간 보상 없음, 낮은 확률
            clear(a);
            a.getInventory().add(new ItemStack(ticket, 3));
            walletBefore = Economy.wallet(a);
            com.rtshop.raid.Raid.testRandom = () -> 0.6;
            int got = com.rtshop.raid.Raid.reward(a);
            check("the same week again: no weekly reward and lower drop odds (a 60 roll drops nothing where it would before)",
                    Economy.wallet(a) == walletBefore && got == 0 && count(a, s -> s.is(Items.DIAMOND)) == 0
                            && c.chanceRepeat[0] + c.chanceRepeat[1] + c.chanceRepeat[2] + c.chanceRepeat[3] + c.chanceRepeat[4] + c.chanceRepeat[5]
                            < c.chanceFresh[0] + c.chanceFresh[1] + c.chanceFresh[2] + c.chanceFresh[3] + c.chanceFresh[4] + c.chanceFresh[5], got);
            com.rtshop.raid.Weekly.testClockOffset += 8L * 24 * 3600 * 1000;
            check("the weekly reward is available again after the Monday reset", !com.rtshop.raid.Raid.weeklyClaimed(a));
            com.rtshop.raid.Weekly.testClockOffset = 0;
            // 플레이어의 공격이 아닌 죽음: 보상 없음
            data.of(a.getUUID()).weeklyWeek = -1;
            com.rtshop.raid.Raid.enter(a);
            com.rtshop.raid.Raid.summon(a);
            walletBefore = Economy.wallet(a);
            com.rtshop.raid.Raid.bossDead(server, false);
            check("a boss that dies without a player's attack gives nothing", !com.rtshop.raid.Raid.weeklyClaimed(a) && Economy.wallet(a) == walletBefore
                    && !com.rtshop.raid.Raid.wasRewarded() && com.rtshop.raid.Raid.state() == com.rtshop.raid.Raid.DONE);
            com.rtshop.raid.Raid.forceEnd(server);
            // 실패: 모두 쓰러지면 보스가 사라지고, 확인하면 마을로
            com.rtshop.raid.Raid.enter(a);
            com.rtshop.raid.Raid.summon(a);
            net.minecraft.world.entity.Entity boss2 = level.getEntity(com.rtshop.raid.Raid.bossIdForTest());
            com.rtshop.raid.Raid.downForTest(a);
            com.rtshop.raid.Raid.tick(server);
            boolean failed = com.rtshop.raid.Raid.state() == com.rtshop.raid.Raid.FAILED && (boss2 == null || !boss2.isAlive()) && com.rtshop.raid.Raid.inArena(a);
            com.rtshop.raid.Raid.handle(a, com.rtshop.raid.Raid.ACT_LEAVE, "");
            com.rtshop.raid.Raid.tick(server);
            check("when everyone is down the raid fails: the boss goes, and players return after confirming", failed && !com.rtshop.raid.Raid.inArena(a)
                    && !com.rtshop.raid.Raid.downed(a) && com.rtshop.raid.Raid.state() == com.rtshop.raid.Raid.IDLE && !com.rtshop.raid.Raid.weeklyClaimed(a));
            // 모드팩: 진짜 이그니스로도 소환된다 (체력 배율, 제단 블록)
            if (ForgeRegistries.ENTITY_TYPES.containsKey(new ResourceLocation("cataclysm", "ignis"))) {
                c.boss = "cataclysm:ignis";
                a.getInventory().add(new ItemStack(ticket, 1));
                com.rtshop.raid.Raid.build(level);
                com.rtshop.raid.Raid.enter(a);
                boolean ok = com.rtshop.raid.Raid.summon(a);
                net.minecraft.world.entity.Entity ig = ok ? level.getEntity(com.rtshop.raid.Raid.bossIdForTest()) : null;
                String altarId = String.valueOf(ForgeRegistries.BLOCKS.getKey(level.getBlockState(com.rtshop.raid.Raid.altarPos()).getBlock()));
                check("in the pack the raid summons Cataclysm's Ignis at full health, on Cataclysm's altar of fire",
                        ig instanceof net.minecraft.world.entity.LivingEntity li && li.getHealth() == li.getMaxHealth()
                                && ig.getClass().getName().contains("Ignis") && altarId.equals("cataclysm:altar_of_fire"),
                        (ig == null ? "no boss" : ig.getClass().getSimpleName() + " " + ((net.minecraft.world.entity.LivingEntity) ig).getMaxHealth()) + " " + altarId);
                com.rtshop.raid.Raid.forceEnd(server);
                c.boss = "minecraft:zombie";
            }
            // 편집기: 수치·문구·보상을 게임 안에서 고친다
            com.rtshop.raid.RewardEditor.Target target = com.rtshop.raid.RewardEditor.TARGETS.get("raid");
            com.google.gson.JsonObject model = target.model(a);
            com.google.gson.JsonObject edited = new com.google.gson.JsonObject();
            edited.addProperty("id", "raid");
            com.google.gson.JsonObject ef = new com.google.gson.JsonObject();
            ef.addProperty("healthMultiplier", "4.5");
            ef.addProperty("chanceFresh5", "1.5");
            ef.addProperty("text.mysteryName", "시험용 이름");
            edited.add("fields", ef);
            com.google.gson.JsonArray er = new com.google.gson.JsonArray();
            er.add(new com.rtshop.raid.Reward(com.rtshop.raid.RaidConfig.G_DROP, 5, "minecraft:dragon_egg", 1).toJson());
            edited.add("rewards", er);
            target.apply(a, edited);
            com.rtshop.raid.RaidConfig now = com.rtshop.raid.RaidConfig.get();
            a.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.GOLDEN_APPLE, 5));
            target.add(a, com.rtshop.raid.Reward.of(com.rtshop.raid.RaidConfig.G_WEEKLY, 0, a.getMainHandItem()));
            check("the in-game editor changes numbers, texts and rewards (held items can be added with their count)",
                    model.getAsJsonArray("fields").size() > 30 && model.getAsJsonArray("groups").size() == 2 && now.healthMultiplier == 4.5
                            && now.chanceFresh[5] == 1.5 && now.text("mysteryName").equals("시험용 이름") && now.boss.equals("minecraft:zombie")
                            && now.rewards.size() == 2 && now.rewards.get(1).count == 5 && now.rewards.get(1).item.equals("minecraft:golden_apple"),
                    now.healthMultiplier + " " + now.rewards.size());
            check("the raid guide and the raid merchant can be spawned, and the OP commands exist",
                    !server.getCommands().getDispatcher().parse("rtshop npc raid_guide", server.createCommandSourceStack()).getReader().canRead()
                            && !server.getCommands().getDispatcher().parse("rtshop raid arena here", server.createCommandSourceStack()).getReader().canRead()
                            && !server.getCommands().getDispatcher().parse("rtshop raid edit", server.createCommandSourceStack()).getReader().canRead()
                            && ShopServer.shops().containsKey("raid")
                            && findEntry(ShopServer.shops().get("raid"), e -> e.stack.is(ticket) && e.buyable) >= 0);
            // 잠금: 레이드 관련 NPC 는 이름 있는 보스를 처음 쓰러뜨린 뒤에야 말을 받아 준다
            data.of(a.getUUID()).kills.clear();
            boolean hadBossQuest = com.rtshop.raid.Raid.firstBossDown(a);
            net.minecraft.world.entity.npc.Villager guide = com.rtshop.raid.Raid.spawnGuide(level, a.position().add(1, 0, 0), 0);
            net.minecraft.world.entity.npc.Villager seller = ShopServer.spawnNpc(level, a.position().add(-1, 0, 0), 0, "raid");
            boolean lockedBefore = com.rtshop.raid.Raid.npcLocked(a);
            String firstBoss = com.rtshop.party.PartyConfig.get().bosses.isEmpty() ? "cataclysm:ignis" : com.rtshop.party.PartyConfig.get().bosses.iterator().next();
            data.of(a.getUUID()).kills.put("minecraft:zombie", 5); // 보스가 아닌 것은 세지 않는다
            boolean stillLocked = com.rtshop.raid.Raid.npcLocked(a);
            data.of(a.getUUID()).kills.put(firstBoss, 1);
            boolean openAfter = !com.rtshop.raid.Raid.npcLocked(a) && com.rtshop.raid.Raid.bossKills(a, firstBoss) == 1;
            com.rtshop.raid.RaidConfig.get().npcsAfterFirstBoss = false;
            data.of(a.getUUID()).kills.clear();
            boolean offOpen = !com.rtshop.raid.Raid.npcLocked(a);
            com.rtshop.raid.RaidConfig.get().npcsAfterFirstBoss = true;
            check("raid NPCs (guide, raid merchant) answer only after a first named boss is down; each boss kill is written per boss",
                    (hadBossQuest || (lockedBefore && stillLocked)) && openAfter && offOpen && guide != null && seller != null,
                    hadBossQuest + "/" + lockedBefore + "/" + stillLocked + "/" + openAfter + "/" + offOpen);
            if (guide != null) {
                net.minecraft.world.entity.vehicle.Boat boat = EntityType.BOAT.create(level);
                boat.moveTo(guide.getX(), guide.getY(), guide.getZ(), 0, 0);
                level.addFreshEntity(boat);
                boolean rode = guide.startRiding(boat, true);
                check("NPCs do not get into boats or minecarts (they cannot be carried off)", !rode && !guide.isPassenger(), rode);
                boat.discard();
            }
            if (guide != null) guide.discard();
            if (seller != null) seller.discard();
            // 보스 음악: 싸우고 있는 보스의 곡을 고른다 (곡이 없는 보스는 조용하고, 싸우지 않으면 흐르지 않는다)
            com.rtshop.story.BossMusic.Config mc0 = com.rtshop.story.BossMusic.fresh();
            com.rtshop.story.BossMusic.setForTest(mc0);
            // 임시 보스: 허스크 (모드팩에서 다른 모드가 허스크를 막거나 바꾸면 갑옷 거치대로)
            net.minecraft.world.entity.LivingEntity husk = EntityType.HUSK.create(level);
            husk.moveTo(a.getX() + 3, a.getY(), a.getZ(), 0, 0);
            if (husk instanceof net.minecraft.world.entity.Mob hm) hm.setNoAi(true);
            boolean huskIn = level.addFreshEntity(husk) && !husk.isRemoved()
                    && level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, a.getBoundingBox().inflate(16)).contains(husk);
            if (!huskIn) {
                husk.discard();
                husk = EntityType.ARMOR_STAND.create(level);
                husk.moveTo(a.getX() + 3, a.getY(), a.getZ(), 0, 0);
                level.addFreshEntity(husk);
            }
            boolean aWasAlive = a.isAlive();
            if (!aWasAlive) a.setHealth(a.getMaxHealth()); // 앞의 레이드 점검에서 쓰러진 채면 일으켜 세운다
            String foeId = com.rtshop.party.BossCredit.id(husk);
            mc0.tracks.put(foeId, "husk_theme");
            boolean huskWas = com.rtshop.party.PartyConfig.get().bosses.add(foeId);
            // (가까운 것 찾기는 청크가 불려 있어야 해서 점검에서는 후보를 직접 넘긴다)
            Object[] quiet = com.rtshop.story.BossMusic.pick(a, java.util.List.of(husk));
            com.rtshop.party.BossCredit.record(husk, a, 4F);
            Object[] fight = com.rtshop.story.BossMusic.pick(a, java.util.List.of(husk));
            mc0.tracks.put(foeId, "");
            Object[] silent = com.rtshop.story.BossMusic.pick(a, java.util.List.of(husk));
            mc0.tracks.remove(foeId);
            Object[] common = com.rtshop.story.BossMusic.pick(a, java.util.List.of(husk));
            final net.minecraft.world.entity.LivingEntity foe = husk;
            check("boss music: nothing plays before the fight, the boss's own track plays while fighting, '' silences a boss, '*' is the common track",
                    quiet[0].equals("") && fight[0].equals("husk_theme") && (int) fight[1] == husk.getId() && silent[0].equals("") && common[0].equals("boss"),
                    quiet[0] + "/" + fight[0] + "/" + silent[0] + "/" + common[0] + " | " + foeId + " husk=" + huskIn + " gone=" + husk.isRemoved() + " alive=" + aWasAlive
                            + " raid=" + com.rtshop.raid.Raid.state() + " near=" + level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, a.getBoundingBox().inflate(48), e -> e == foe).size()
                            + " boss=" + com.rtshop.party.BossCredit.isBoss(husk) + " dealt=" + com.rtshop.party.BossCredit.dealt(husk, a));
            com.rtshop.party.BossCredit.forget(husk);
            husk.discard();
            if (huskWas) com.rtshop.party.PartyConfig.get().bosses.remove(foeId);
            com.rtshop.story.BossMusic.reload();

            // 무기 설명서: 특수기가 있는 무기를 처음 얻으면 한 권, 같은 종류·같은 특수기는 다시 주지 않는다
            ItemStack ironSword = new ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
            ItemStack manual0 = com.rtshop.item.WeaponManualItem.of(ironSword);
            check("a weapon manual remembers its weapon and is named after it",
                    com.rtshop.item.WeaponManualItem.weapon(manual0).is(net.minecraft.world.item.Items.IRON_SWORD)
                            && manual0.getHoverName().getString().length() > 0, manual0.getHoverName().getString());
            com.rtshop.item.WeaponManuals.forgetAll(a);
            a.getInventory().clearContent();
            boolean efLoaded = net.minecraftforge.fml.ModList.get().isLoaded("epicfight");
            check("Epic Fight's weapon data can be reached whenever Epic Fight is installed (also on a dedicated server)",
                    com.rtshop.compat.EfWeapons.present() == efLoaded, efLoaded + " / " + com.rtshop.compat.EfWeapons.failure);
            if (com.rtshop.compat.EfWeapons.present()) {
                com.rtshop.compat.EfWeapons.Info wi = com.rtshop.compat.EfWeapons.info(a, ironSword);
                check("Epic Fight weapon data is readable: an iron sword is a sword with an innate skill",
                        wi != null && wi.category().equals("sword") && !wi.skill().isEmpty(), wi);
                // 스스로 주는 것은 "특별한 조작이 있는 모드 무기"뿐: 바닐라 무기와 흔한 특수기만 가진 무기는 주지 않는다
                ItemStack vanilla = com.rtshop.item.WeaponManuals.offer(a, ironSword);
                java.util.List<com.rtshop.compat.EfWeapons.Form> noForms = java.util.List.of();
                boolean rule = !com.rtshop.item.WeaponManuals.special(new com.rtshop.compat.EfWeapons.Info("sword", "epicfight:sweeping_edge", 4, true, noForms))
                        && !com.rtshop.item.WeaponManuals.special(new com.rtshop.compat.EfWeapons.Info("sword", "epicfightx:dancing_edge", 4, true, noForms))
                        && !com.rtshop.item.WeaponManuals.special(new com.rtshop.compat.EfWeapons.Info("fist", "", 3, true, noForms))
                        && com.rtshop.item.WeaponManuals.special(new com.rtshop.compat.EfWeapons.Info("uchigatana", "somemod:judgement_cut", 5, true, noForms))
                        && com.rtshop.item.WeaponManuals.special(new com.rtshop.compat.EfWeapons.Info("sword", "epicfight:sweeping_edge", 4, true,
                        java.util.List.of(new com.rtshop.compat.EfWeapons.Form("two_hand", "somemod:spin", 4, null))));
                check("weapon manuals are handed out only for mod weapons with a skill of their own (a vanilla iron sword gets none; the common sword skill is not 'special')",
                        vanilla.isEmpty() && rule && a.getInventory().countItem(com.rtshop.item.ModItems.WEAPON_MANUAL.get()) == 0, vanilla + " / " + rule);
                // 바닐라 철 도끼와 똑같이 쓰는 모드 도끼(다른 모드가 도끼 전체에 기술을 덧붙였더라도)는 받지 않는다
                com.rtshop.compat.EfWeapons.Info axeInfo = com.rtshop.compat.EfWeapons.info(a, new ItemStack(net.minecraft.world.item.Items.IRON_AXE));
                String twin = "";
                boolean twinGiven = false;
                if (axeInfo != null) {
                    for (net.minecraft.world.item.Item it : net.minecraftforge.registries.ForgeRegistries.ITEMS) {
                        net.minecraft.resources.ResourceLocation rl = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(it);
                        if (rl == null || rl.getNamespace().equals("minecraft") || rl.getNamespace().equals("rtshop")) continue;
                        ItemStack cand = new ItemStack(it);
                        com.rtshop.compat.EfWeapons.Info ci = com.rtshop.compat.EfWeapons.info(a, cand);
                        if (ci != null && ci.key().equals(axeInfo.key())) {
                            twin = rl.toString();
                            twinGiven = com.rtshop.item.WeaponManuals.handedOut(cand, ci);
                            break;
                        }
                    }
                }
                check("a mod axe that works exactly like the vanilla iron axe gets no manual (skills every axe has are not 'special')",
                        axeInfo != null && !com.rtshop.item.WeaponManuals.special(axeInfo) && !twinGiven, twin + " " + axeInfo);
                // 모드팩의 무기 가운데 특별한 조작이 있는 것을 하나 찾아 실제로 받아 본다
                ItemStack modWeapon = ItemStack.EMPTY;
                for (net.minecraft.world.item.Item it : net.minecraftforge.registries.ForgeRegistries.ITEMS) {
                    net.minecraft.resources.ResourceLocation rl = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(it);
                    if (rl == null || rl.getNamespace().equals("minecraft") || rl.getNamespace().equals("rtshop")) continue;
                    ItemStack cand = new ItemStack(it);
                    if (com.rtshop.item.WeaponManuals.handedOut(cand, com.rtshop.compat.EfWeapons.info(a, cand))) {
                        modWeapon = cand;
                        break;
                    }
                }
                if (!modWeapon.isEmpty()) {
                    ItemStack first = com.rtshop.item.WeaponManuals.offer(a, modWeapon);
                    ItemStack again = com.rtshop.item.WeaponManuals.offer(a, modWeapon.copy());
                    check("a mod weapon with its own skill gives its manual the first time only",
                            !first.isEmpty() && again.isEmpty() && a.getInventory().countItem(com.rtshop.item.ModItems.WEAPON_MANUAL.get()) == 1,
                            net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(modWeapon.getItem()) + " " + com.rtshop.compat.EfWeapons.info(a, modWeapon));
                    com.rtshop.rules.WorldRules.get().weaponManuals = false;
                    com.rtshop.item.WeaponManuals.forgetAll(a);
                    a.getInventory().clearContent();
                    a.getInventory().add(modWeapon.copy());
                    a.getInventory().add(new ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
                    int off = com.rtshop.item.WeaponManuals.scan(a);
                    com.rtshop.rules.WorldRules.get().weaponManuals = true;
                    int on = com.rtshop.item.WeaponManuals.scan(a);
                    int later = com.rtshop.item.WeaponManuals.scan(a);
                    check("weaponManuals=false gives nothing; turned on, the bag scan hands out that weapon's manual once (and nothing for the iron sword)",
                            off == 0 && on == 1 && later == 0, off + " / " + on + " / " + later);
                } else {
                    RtShop.LOGGER.info("[rtshop-selftest] no mod weapon with a skill of its own in this pack: the hand-out check is skipped");
                }
            } else {
                a.getInventory().add(new ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
                check("without Epic Fight no weapon manuals are handed out", com.rtshop.item.WeaponManuals.scan(a) == 0
                        && com.rtshop.compat.EfWeapons.info(a, ironSword) == null);
            }
            com.rtshop.item.WeaponManuals.forgetAll(a);
        } finally {
            com.rtshop.raid.Raid.testRandom = null;
            com.rtshop.raid.Weekly.testClockOffset = 0;
            com.rtshop.raid.Raid.forceEnd(server);
            com.rtshop.raid.Raid.TEST_PLAYERS.clear();
            com.rtshop.raid.Raid.resetForTest(server);
            data.of(a.getUUID()).weeklyWeek = -1;
            data.of(a.getUUID()).kills.clear();
            data.of(b.getUUID()).kills.clear();
            clear(a);
            clear(b);
            // 구역을 치운다
            for (int x = 6000 - 15; x <= 6000 + 15; x++)
                for (int z = 6000 - 15; z <= 6000 + 15; z++)
                    for (int y = 148; y <= 164; y++) level.setBlock(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
            if (fileBefore != null) Files.write(file, fileBefore);
            else Files.deleteIfExists(file);
            com.rtshop.raid.RaidConfig.reload();
        }
    }

    // ------------------------------------------------------------------ Spiral Abyss 주간 보상

    private static void abyss(MinecraftServer server, ServerLevel level, FakePlayer a) throws Exception {
        section("spiral abyss weekly rewards");
        java.nio.file.Path file = FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("abyss.json");
        byte[] fileBefore = Files.exists(file) ? Files.readAllBytes(file) : null;
        com.rtshop.raid.AbyssWeekly.Config c = com.rtshop.raid.AbyssWeekly.fresh();
        c.gold[1] = 1234;
        c.endless = false; // 먼저 예전 방식(층 클리어 보상)을 확인한다
        c.rewards.clear();
        com.rtshop.raid.Reward sure = new com.rtshop.raid.Reward("floor2", 0, "minecraft:diamond", 3);
        com.rtshop.raid.Reward half = new com.rtshop.raid.Reward("floor2", 0, "minecraft:emerald", 2);
        half.chance = 50;
        c.rewards.add(sure);
        c.rewards.add(half);
        com.rtshop.raid.AbyssWeekly.setForTest(c);
        try {
            clear(a);
            com.rtshop.raid.AbyssWeekly.resetPlayer(a);
            Economy.setWallet(a, 0);
            com.rtshop.raid.Raid.testRandom = () -> 0.7; // 70: 확률 50% 보상은 나오지 않는다
            boolean first = com.rtshop.raid.AbyssWeekly.cleared(a, 2);
            check("clearing a floor pays that floor's weekly reward (Gold + items by their chance)", first && Economy.wallet(a) == 1234
                    && count(a, s -> s.is(Items.DIAMOND)) == 3 && count(a, s -> s.is(Items.EMERALD)) == 0
                    && com.rtshop.raid.AbyssWeekly.claimedMask(a) == 2, Economy.wallet(a) + " " + com.rtshop.raid.AbyssWeekly.claimedMask(a));
            boolean again = com.rtshop.raid.AbyssWeekly.cleared(a, 2);
            check("...only once a week per floor, and the list sent to the screen marks it as received", !again && Economy.wallet(a) == 1234
                    && com.rtshop.raid.AbyssWeekly.view(a).get("mask").getAsLong() == 2
                    && com.rtshop.raid.AbyssWeekly.view(a).getAsJsonArray("rewards").size() == 2);
            com.rtshop.raid.Weekly.testClockOffset += 8L * 24 * 3600 * 1000;
            com.rtshop.raid.Raid.testRandom = () -> 0.2;
            boolean nextWeek = com.rtshop.raid.AbyssWeekly.claimedMask(a) == 0 && com.rtshop.raid.AbyssWeekly.cleared(a, 2);
            check("after the Monday 06:00 reset the reward can be earned again", nextWeek && Economy.wallet(a) == 2468 && count(a, s -> s.is(Items.EMERALD)) == 2);
            com.rtshop.raid.Weekly.testClockOffset = 0;
            // 3.41: 주간 보상은 무한 모드에서 (10웨이브마다 한 단계), 층 모드는 이야기가 부를 때만
            c.endless = true;
            c.gold[0] = 100;
            com.rtshop.raid.AbyssWeekly.resetPlayer(a);
            Economy.setWallet(a, 0);
            boolean floorPays = com.rtshop.raid.AbyssWeekly.cleared(a, 2);
            boolean early = com.rtshop.raid.AbyssWeekly.endlessCleared(a, 9);
            boolean w20 = com.rtshop.raid.AbyssWeekly.endlessCleared(a, 20);
            boolean w20again = com.rtshop.raid.AbyssWeekly.endlessCleared(a, 20);
            check("with weekly rewards on the endless mode: a floor clear pays nothing (the story mark is still set), wave 20 pays stages 1 and 2 once",
                    !floorPays && !early && w20 && !w20again && Economy.wallet(a) == 100 + 1234 && com.rtshop.raid.AbyssWeekly.claimedMask(a) == 3
                            && QuestManager.hasFlag(a, "abyss_floor_2") && com.rtshop.raid.AbyssWeekly.view(a).get("endless").getAsBoolean(),
                    Economy.wallet(a) + " " + com.rtshop.raid.AbyssWeekly.claimedMask(a));
            QuestManager.reset(a);
            boolean lockedIdle = com.rtshop.raid.AbyssWeekly.floorLocked(a, 1) && com.rtshop.raid.AbyssWeekly.floorLocked(a, 6) && !com.rtshop.raid.AbyssWeekly.floorLocked(a, 7);
            QuestDef abyssQ = tempQuest("{\"id\":\"t_abyss\",\"giver\":\"quest_guide\",\"objectives\":[{\"type\":\"FLAG\",\"target\":\"abyss_floor_2\",\"count\":1}],\"reward\":{\"gold\":1}}");
            QuestManager.loaded().quests.put(abyssQ.id, abyssQ);
            QuestManager.accept(a, "t_abyss", "quest_guide");
            boolean openStory = !com.rtshop.raid.AbyssWeekly.floorLocked(a, 1) && !com.rtshop.raid.AbyssWeekly.floorLocked(a, 2) && com.rtshop.raid.AbyssWeekly.floorLocked(a, 3)
                    && QuestManager.storyAbyssFloor(a) == 2;
            QuestManager.setFlag(a, "abyss_floor_2", true);
            boolean closedAgain = com.rtshop.raid.AbyssWeekly.floorLocked(a, 2);
            c.storyOnlyFloors = false;
            boolean off = !com.rtshop.raid.AbyssWeekly.floorLocked(a, 5);
            c.storyOnlyFloors = true;
            check("floor modes open only while a story quest calls for that floor (floors below it too); the endless mode is always open",
                    lockedIdle && openStory && closedAgain && off, lockedIdle + "/" + openStory + "/" + closedAgain + "/" + off);
            QuestManager.reset(a);
            QuestManager.loaded().quests.remove("t_abyss");
            // 월드 규칙: 생기지 않는 건축물·조합법·전리품, 하나뿐인 문
            com.rtshop.rules.WorldRules wr0 = com.rtshop.rules.WorldRules.get();
            com.rtshop.rules.WorldRules wr = com.rtshop.rules.WorldRules.defaults();
            wr.lootBlockedMods.add("minecraft");
            com.rtshop.rules.WorldRules.setForTest(wr);
            boolean neverOk = com.rtshop.story.StagedStructures.blocks("spiral_abyss:ruined_portal") && !com.rtshop.rules.WorldRules.neverStructure("minecraft:village_plains")
                    && com.rtshop.rules.WorldRules.recipeBlocked(new ResourceLocation("x", "y"), new ResourceLocation("spiral_abyss", "abyss_gate_activate_block"))
                    && !com.rtshop.rules.WorldRules.recipeBlocked(new ResourceLocation("x", "y"), new ResourceLocation("minecraft", "stick"))
                    && com.rtshop.rules.WorldRules.lootBlocked(new ItemStack(Items.STICK));
            boolean freeBefore = com.rtshop.rules.WorldRules.portalAllowed(a);
            wr.abyssPortalSet = true;
            wr.abyssPortalDim = a.level().dimension().location().toString();
            wr.abyssPortalX = a.getBlockX() + 500;
            wr.abyssPortalY = a.getBlockY();
            wr.abyssPortalZ = a.getBlockZ();
            boolean far = !com.rtshop.rules.WorldRules.portalAllowed(a) && com.rtshop.raid.AbyssWeekly.refuseFloor(a, 7);
            wr.abyssPortalX = a.getBlockX() + 3;
            boolean near = com.rtshop.rules.WorldRules.portalAllowed(a);
            boolean altarFree = com.rtshop.rules.WorldRules.altarAllowed(a.level(), a.blockPosition());
            wr.altarSet = true;
            wr.altarDim = a.level().dimension().location().toString();
            wr.altarX = a.getBlockX();
            wr.altarY = a.getBlockY();
            wr.altarZ = a.getBlockZ();
            boolean altarOne = com.rtshop.rules.WorldRules.altarAllowed(a.level(), a.blockPosition()) && !com.rtshop.rules.WorldRules.altarAllowed(a.level(), a.blockPosition().above(3))
                    && com.rtshop.rules.WorldRules.refuseAltar(a, a.blockPosition().east(9));
            QuestDef orbQ = QuestManager.loaded().quests.get("story_1_42");
            boolean orbNotGiven = orbQ != null && orbQ.rewardItems.stream().noneMatch(x -> x.item.equals("rtshop:arcane_orb"))
                    && server.getRecipeManager().byKey(new ResourceLocation("rtshop", "arcane_orb")).isEmpty();
            check("the rune altar is one for the whole town: only the chosen orb opens, it is neither handed out by the story nor craftable",
                    altarFree && altarOne && orbNotGiven, altarFree + "/" + altarOne + "/" + orbNotGiven);
            com.rtshop.rules.WorldRules.setForTest(wr0);
            check("world rules: the ruined abyss gate never generates, its key cannot be crafted, blocked mods fall out of loot, and only the one chosen portal lets people in",
                    neverOk && freeBefore && far && near, neverOk + "/" + freeBefore + "/" + far + "/" + near);
            // 층 상자 보상
            com.rtshop.raid.AbyssFloors.Floor[] fl0 = com.rtshop.raid.AbyssFloors.get();
            com.rtshop.raid.AbyssFloors.Floor[] flT = new com.rtshop.raid.AbyssFloors.Floor[com.rtshop.raid.AbyssFloors.FLOORS];
            for (int i = 0; i < flT.length; i++) flT[i] = new com.rtshop.raid.AbyssFloors.Floor();
            com.rtshop.raid.Reward fr = new com.rtshop.raid.Reward("c2", 0, "minecraft:diamond", 3);
            flT[0].use = true;
            flT[0].rewards.add(fr);
            flT[0].weight[1] = 7;
            net.minecraft.world.level.storage.loot.LootTable built = com.rtshop.raid.AbyssFloors.build(flT[0].rewards);
            net.minecraft.world.level.storage.loot.LootParams lp = new net.minecraft.world.level.storage.loot.LootParams.Builder(a.serverLevel())
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, a.position())
                    .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
            List<ItemStack> rolledF = built.getRandomItems(lp);
            boolean builtOk = rolledF.size() == 1 && rolledF.get(0).is(Items.DIAMOND) && rolledF.get(0).getCount() == 3;
            String floorDetail = "no mod";
            boolean floorOk = true;
            if (com.rtshop.raid.AbyssWeekly.detected()) {
                com.rtshop.raid.AbyssFloors.setForTest(flT);
                boolean ap = com.rtshop.raid.AbyssFloors.apply(server);
                Object snapF = Class.forName("com.pla.spiral_abyss.encounter.data.FloorCatalog").getMethod("snapshot").invoke(null);
                java.util.Map<?, ?> fm = (java.util.Map<?, ?>) snapF.getClass().getMethod("floors").invoke(snapF);
                Object rw = fm.get(1).getClass().getMethod("rewards").invoke(fm.get(1));
                String entriesF = String.valueOf(rw.getClass().getMethod("entries").invoke(rw));
                boolean tableIn = server.getLootData().getElement(new net.minecraft.world.level.storage.loot.LootDataId<>(net.minecraft.world.level.storage.loot.LootDataType.TABLE,
                        com.rtshop.raid.AbyssFloors.tableId(1, 2))) != null;
                List<ItemStack> viaGame = server.getLootData().getLootTable(com.rtshop.raid.AbyssFloors.tableId(1, 2)).getRandomItems(lp);
                flT[0].use = false;
                com.rtshop.raid.AbyssFloors.setForTest(flT);
                com.rtshop.raid.AbyssFloors.apply(server);
                Object snapG = Class.forName("com.pla.spiral_abyss.encounter.data.FloorCatalog").getMethod("snapshot").invoke(null);
                java.util.Map<?, ?> gm = (java.util.Map<?, ?>) snapG.getClass().getMethod("floors").invoke(snapG);
                Object rw2 = gm.get(1).getClass().getMethod("rewards").invoke(gm.get(1));
                String back = String.valueOf(rw2.getClass().getMethod("entries").invoke(rw2));
                floorOk = ap && entriesF.contains("rtshop_abyss:floor_1/chest_2") && entriesF.contains("weight=7") && tableIn && viaGame.size() == 1 && viaGame.get(0).getCount() == 3
                        && !back.contains("rtshop_abyss") && back.length() > 10;
                floorDetail = ap + " " + tableIn + " " + viaGame.size() + " " + entriesF.length() + " " + back.length() + " " + com.rtshop.raid.AbyssFloors.lastError;
            }
            com.rtshop.raid.AbyssFloors.setForTest(fl0);
            com.rtshop.raid.AbyssFloors.apply(server);
            check("abyss floor chests: an edited chest becomes a real loot table the mod rolls and previews, and turning it off brings the mod's own rewards back",
                    builtOk && floorOk && com.rtshop.raid.RewardEditor.TARGETS.containsKey("abyssfloor6"), builtOk + " " + floorDetail);
            // 편집기
            com.rtshop.raid.RewardEditor.Target target = com.rtshop.raid.RewardEditor.TARGETS.get("abyss");
            com.google.gson.JsonObject model = target.model(a);
            com.google.gson.JsonObject edited = new com.google.gson.JsonObject();
            edited.addProperty("id", "abyss");
            com.google.gson.JsonObject ef = new com.google.gson.JsonObject();
            ef.addProperty("gold0", "999");
            edited.add("fields", ef);
            com.google.gson.JsonArray er = new com.google.gson.JsonArray();
            com.rtshop.raid.Reward nr = new com.rtshop.raid.Reward("floor6", 0, "minecraft:nether_star", 1);
            nr.chance = 0.5;
            er.add(nr.toJson());
            edited.add("rewards", er);
            target.apply(a, edited);
            com.rtshop.raid.AbyssWeekly.Config now = com.rtshop.raid.AbyssWeekly.config();
            check("the in-game editor sets each floor's rewards, their chance and the Gold", model.getAsJsonArray("groups").size() == 6
                    && now.gold[0] == 999 && now.gold[1] == 1234 && now.rewards.size() == 1 && now.rewards.get(0).chance == 0.5
                    && now.floor(6).size() == 1, now.gold[0] + " " + now.rewards.size());
            check("the abyss commands exist", !server.getCommands().getDispatcher().parse("rtshop abyss edit", server.createCommandSourceStack()).getReader().canRead());
            info("spiral abyss detected: " + com.rtshop.raid.AbyssWeekly.detected());
            if (net.minecraftforge.fml.ModList.get().isLoaded("spiral_abyss")) {
                check("Spiral Abyss is installed and its progress can be read (floor clears are detected)", com.rtshop.raid.AbyssWeekly.detected());
            }
        } finally {
            com.rtshop.raid.Raid.testRandom = null;
            com.rtshop.raid.Weekly.testClockOffset = 0;
            com.rtshop.raid.AbyssWeekly.resetPlayer(a);
            clear(a);
            if (fileBefore != null) Files.write(file, fileBefore);
            else Files.deleteIfExists(file);
            com.rtshop.raid.AbyssWeekly.reload();
        }
    }

    // ------------------------------------------------------------------ 식당 주인

    private static void restaurant(MinecraftServer server, ServerLevel level, FakePlayer a, FakePlayer b) throws Exception {
        section("restaurant owner");
        ShopData rest = ShopServer.shops().get(com.rtshop.shop.Restaurant.ID);
        ShopData food = ShopServer.shops().get("food");
        check("the restaurant shop exists and lists the food merchant's dishes", rest != null && food != null && !rest.entries.isEmpty(),
                rest == null ? null : rest.entries.size());
        if (rest == null || food == null || rest.entries.isEmpty()) {
            return;
        }
        com.rtshop.shop.Restaurant.Config rc = com.rtshop.shop.Restaurant.config();
        com.rtshop.shop.Restaurant.reset(server);
        rest = ShopServer.shops().get(com.rtshop.shop.Restaurant.ID);
        check("the restaurant starts with an empty menu (nothing to buy)", com.rtshop.shop.Restaurant.menuCount(rest) == 0);
        double worst = (1 + rc.bonus) * (1 + com.rtshop.job.JobConfig.get().job("cook").param("dishSell", 0));
        List<String> bad = new ArrayList<>();
        for (ShopData.Entry e : rest.entries) {
            int fi = findEntry(food, f -> ItemStack.isSameItemSameTags(f.stack, e.stack));
            if (fi < 0 || Math.abs(e.sellUnit - food.entries.get(fi).sellUnit * (1 + rc.bonus)) > 1e-6 || !e.sellable) {
                bad.add(ShopLoader.itemId(e.stack) + " sell");
            } else if (e.price <= food.entries.get(fi).sellUnit * worst) {
                bad.add(ShopLoader.itemId(e.stack) + " price " + e.price);
            }
        }
        check("every dish pays 5% more than the food merchant, and buying it back never pays (even for a cook)", bad.isEmpty(),
                bad.isEmpty() ? null : bad.subList(0, Math.min(5, bad.size())));
        // 팔 요리: 값이 1G 이상 나오는 것
        int idx = findEntry(rest, e -> e.sellUnit >= 2 && e.stack.getMaxStackSize() >= 8);
        if (idx < 0) idx = findEntry(rest, e -> e.sellUnit >= 1);
        if (idx < 0) {
            info("no dish with a sell price; skipping the selling checks");
            return;
        }
        ShopData.Entry dish = rest.entries.get(idx);
        int n = Math.min(4, dish.stack.getMaxStackSize());
        String lifeBefore = com.rtshop.job.Jobs.entry(a).life;
        try {
            // 요리사가 아니면 팔 수 없다
            com.rtshop.job.Jobs.force(a, com.rtshop.job.JobConfig.LIFE, "");
            clear(a);
            ItemStack give = dish.stack.copy();
            give.setCount(n);
            a.getInventory().add(give);
            Economy.setWallet(a, 0);
            ShopServer.openShop(a, com.rtshop.shop.Restaurant.ID, -1);
            long key = rest.variantKey(dish, dish.stack);
            ShopServer.handleTransaction(a, com.rtshop.shop.Restaurant.ID, rest.version, idx, true, n, key);
            final ShopData.Entry dishF = dish;
            check("a player without the cook job cannot sell to the restaurant owner",
                    Economy.wallet(a) == 0 && count(a, s -> s.is(dishF.stack.getItem())) == n && !com.rtshop.shop.Restaurant.canSell(a)
                            && !com.rtshop.shop.Restaurant.viewFor(a, rest).sellLockNote.isEmpty());
            ShopServer.handleSellAll(a, com.rtshop.shop.Restaurant.ID, rest.version, new int[]{idx}, new long[]{key});
            check("'sell all' is refused for non-cooks as well", Economy.wallet(a) == 0 && count(a, s -> s.is(dishF.stack.getItem())) == n);
            // 요리사: 식품 상인 값 × 1.05 × 요리사 보너스
            com.rtshop.job.Jobs.force(a, com.rtshop.job.JobConfig.LIFE, "cook");
            double jobMult = ShopServer.jobSellMultiplier(a, dish);
            long expect = ShopData.revenue(key / 100.0 * jobMult, n);
            int fi = findEntry(food, f -> ItemStack.isSameItemSameTags(f.stack, dishF.stack));
            long atFood = ShopData.revenue(food.variantKey(food.entries.get(fi), dish.stack) / 100.0 * ShopServer.jobSellMultiplier(a, food.entries.get(fi)), n);
            ShopServer.openShop(a, com.rtshop.shop.Restaurant.ID, -1);
            ShopServer.handleTransaction(a, com.rtshop.shop.Restaurant.ID, rest.version, idx, true, n, key);
            check("a cook sells dishes for the food merchant's price +5%, on top of the cook bonus",
                    Economy.wallet(a) == expect && expect >= atFood && expect <= Math.ceil(atFood * (1 + rc.bonus)) + 2
                            && com.rtshop.shop.Restaurant.viewFor(a, rest).sellLockNote.isEmpty(), Economy.wallet(a) + " / " + expect + " / food " + atFood);
            ShopData now = ShopServer.shops().get(com.rtshop.shop.Restaurant.ID);
            check("the dish a cook sold appears on the menu (only that one), same shop version",
                    com.rtshop.shop.Restaurant.menuCount(now) == 1 && now.entries.get(idx).buyable && now.version == rest.version, com.rtshop.shop.Restaurant.menuCount(now));
            // 메뉴에 오른 요리는 직업과 상관없이 누구나 산다 (b 는 직업 없음)
            clear(b);
            Economy.setWallet(b, 1_000_000);
            ShopServer.openShop(b, com.rtshop.shop.Restaurant.ID, -1);
            ShopServer.handleTransaction(b, com.rtshop.shop.Restaurant.ID, now.version, idx, false, 2, 0);
            int other = findEntry(now, e -> !e.buyable);
            long afterBuy = Economy.wallet(b);
            if (other >= 0) {
                ShopServer.handleTransaction(b, com.rtshop.shop.Restaurant.ID, now.version, other, false, 1, 0);
            }
            check("anyone can buy a dish on the menu; dishes not yet on the menu cannot be bought",
                    count(b, s -> s.is(dishF.stack.getItem())) == 2 && afterBuy == 1_000_000 - 2L * now.entries.get(idx).price && Economy.wallet(b) == afterBuy,
                    afterBuy);
            // 저장·읽기, OP 관리
            com.rtshop.shop.Restaurant.Data copy = com.rtshop.shop.Restaurant.Data.load(com.rtshop.shop.Restaurant.Data.get(server).save(new CompoundTag()));
            check("the menu survives saving and loading", copy.unlocked.size() == 1 && copy.unlocked.containsKey(ShopLoader.key(dish.stack)));
            int all = com.rtshop.shop.Restaurant.unlockAll(server);
            ShopData full = ShopServer.shops().get(com.rtshop.shop.Restaurant.ID);
            boolean fullMenu = com.rtshop.shop.Restaurant.menuCount(full) == full.entries.size() && all == full.entries.size() - 1;
            int cleared = com.rtshop.shop.Restaurant.reset(server);
            check("an OP can put every dish on the menu and clear it again", fullMenu && cleared == full.entries.size()
                    && com.rtshop.shop.Restaurant.menuCount(ShopServer.shops().get(com.rtshop.shop.Restaurant.ID)) == 0, all + " " + cleared);
            check("the restaurant owner NPC can be spawned like other merchants (/rtshop npc restaurant)",
                    !server.getCommands().getDispatcher().parse("rtshop npc restaurant", server.createCommandSourceStack()).getReader().canRead()
                            && !server.getCommands().getDispatcher().parse("rtshop restaurant unlock all", server.createCommandSourceStack()).getReader().canRead());
        } finally {
            com.rtshop.job.Jobs.force(a, com.rtshop.job.JobConfig.LIFE, lifeBefore == null ? "" : lifeBefore);
            com.rtshop.shop.Restaurant.reset(server);
            ShopServer.closeSession(a);
            ShopServer.closeSession(b);
            clear(a);
            clear(b);
        }
    }

    // ------------------------------------------------------------------ 조합법·전리품 규칙

    private static void rules(MinecraftServer server, ServerLevel level, FakePlayer a) throws Exception {
        section("recipe and loot rules");
        net.minecraft.world.item.crafting.RecipeManager rm = server.getRecipeManager();
        ResourceLocation tnt = new ResourceLocation("minecraft:tnt");
        check("TNT can be crafted normally", rm.byKey(tnt).isPresent());
        com.rtshop.rules.RecipeRules.Rules r = new com.rtshop.rules.RecipeRules.Rules();
        r.disabled.add("minecraft:tnt");
        r.disabled.add("item:minecraft:bread");
        com.google.gson.JsonObject custom = com.google.gson.JsonParser.parseString(
                "{\"type\":\"minecraft:crafting_shapeless\",\"ingredients\":[{\"item\":\"minecraft:dirt\"},{\"item\":\"minecraft:dirt\"},"
                        + "{\"item\":\"minecraft:dirt\"}],\"result\":{\"item\":\"minecraft:bread\",\"count\":2}}").getAsJsonObject();
        r.custom.put("rtshop:custom/test_bread", custom);
        com.rtshop.rules.RecipeRules.apply(server, r);
        rm = server.getRecipeManager();
        int breadRecipes = 0;
        net.minecraft.world.item.crafting.Recipe<?> mine = null;
        for (net.minecraft.world.item.crafting.Recipe<?> rec : rm.getRecipes()) {
            ResourceLocation out = com.rtshop.rules.RecipeRules.outputId(rec, server);
            if (out != null && out.toString().equals("minecraft:bread")) {
                breadRecipes++;
                if (rec.getId().toString().equals("rtshop:custom/test_bread")) mine = rec;
            }
        }
        check("a blocked recipe disappears (TNT)", rm.byKey(tnt).isEmpty());
        check("item:... blocks every recipe of that item, but a custom recipe for it still works",
                breadRecipes == 1 && mine != null && mine.getResultItem(server.registryAccess()).getCount() == 2, breadRecipes);
        net.minecraft.world.inventory.TransientCraftingContainer grid = new net.minecraft.world.inventory.TransientCraftingContainer(
                new net.minecraft.world.inventory.AbstractContainerMenu(null, -1) {
                    @Override
                    public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p, int i) {
                        return ItemStack.EMPTY;
                    }

                    @Override
                    public boolean stillValid(net.minecraft.world.entity.player.Player p) {
                        return true;
                    }
                }, 3, 3);
        grid.setItem(0, new ItemStack(Items.DIRT));
        grid.setItem(4, new ItemStack(Items.DIRT));
        grid.setItem(8, new ItemStack(Items.DIRT));
        var found = rm.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, grid, level);
        check("the custom recipe crafts in a crafting grid (3 dirt -> 2 bread)", found.isPresent()
                && found.get().assemble(grid, server.registryAccess()).is(Items.BREAD));
        com.rtshop.rules.RecipeRules.apply(server, new com.rtshop.rules.RecipeRules.Rules());
        check("removing the rules brings the original recipes back", server.getRecipeManager().byKey(tnt).isPresent()
                && server.getRecipeManager().byKey(new ResourceLocation("rtshop:custom/test_bread")).isEmpty());
        com.rtshop.rules.RecipeRules.apply(server, com.rtshop.rules.RecipeRules.get());
        com.google.gson.JsonObject list = com.rtshop.rules.RecipeRules.listJson(server);
        check("the recipe editor gets the full recipe list", list.getAsJsonArray("recipes").size() > 500, list.getAsJsonArray("recipes").size());

        // 전리품
        java.util.List<String> chests = com.rtshop.rules.LootRules.chestTables(server);
        check("chest loot tables are listed for the loot editor", chests.contains("minecraft:chests/simple_dungeon") && chests.size() > 10, chests.size());
        ResourceLocation dungeon = new ResourceLocation("minecraft:chests/simple_dungeon");
        java.util.Map<String, com.rtshop.rules.LootRules.Stat> raw = com.rtshop.rules.LootRules.sample(level, dungeon, 300, true);
        com.rtshop.rules.LootRules.Stat saddle = raw.get("minecraft:saddle");
        check("sampling shows the real odds (dungeon saddle about 15-45%)", saddle != null && saddle.hits > 300 * 0.12 && saddle.hits < 300 * 0.6,
                saddle == null ? "none" : saddle.hits);
        java.util.List<String> editable = com.rtshop.rules.LootRules.editableTables(server);
        java.util.Map<String, com.rtshop.rules.LootRules.Stat> zombie = com.rtshop.rules.LootRules.sample(level, new ResourceLocation("minecraft:entities/zombie"), 200, true);
        check("the loot editor also lists mob drops and samples them (zombie drops rotten flesh)",
                editable.contains("minecraft:entities/zombie") && zombie.containsKey("minecraft:rotten_flesh"), zombie.keySet());
        // C-5: 아이템으로 전리품표 찾기 · 상점에서 파는 아이템 목록 (편집 창의 검색·분류가 쓰는 것)
        java.util.List<String> bySaddleC5 = com.rtshop.rules.RulesEditor.findTables(server, java.util.Set.of("minecraft:saddle"));
        java.util.List<String> byFleshC5 = com.rtshop.rules.RulesEditor.findTables(server, java.util.Set.of("minecraft:rotten_flesh"));
        java.util.List<String> byNothingC5 = com.rtshop.rules.RulesEditor.findTables(server, java.util.Set.of("rtshop:no_such_item_c5"));
        check("loot tables can be found by item (saddle -> dungeon chest, rotten flesh -> zombie, an unknown item -> none)",
                bySaddleC5.contains("minecraft:chests/simple_dungeon") && byFleshC5.contains("minecraft:entities/zombie") && byNothingC5.isEmpty()
                        && bySaddleC5.size() < editable.size(), bySaddleC5.size() + " " + byFleshC5.size() + " " + byNothingC5.size() + " of " + editable.size());
        com.google.gson.JsonArray soldC5 = com.rtshop.rules.RulesEditor.soldJson();
        boolean soldTicketC5 = false;
        for (com.google.gson.JsonElement seC5 : soldC5) if (seC5.getAsString().equals("rtshop:return_ticket")) soldTicketC5 = true;
        check("the editors are told which items the NPC shops sell (the return ticket is one of them)", soldC5.size() > 20 && soldTicketC5, soldC5.size());
        // C-6: 자유 조리법(flex)으로 만든 요리(품질 꼬리표)는 재료 한 벌 값 기준으로 따로 산다. 정식 조리법 기준의 매입가는 그대로다
        {
            ShopData anyShopC6 = ShopServer.shops().get("food");
            ShopData.Entry dishC6 = new ShopData.Entry(new ItemStack(Items.RABBIT_STEW), 300, 200.0, "dish", true, false, true, false).withFlex(60.0);
            ItemStack plainC6 = new ItemStack(Items.RABBIT_STEW);
            ItemStack poorC6 = new ItemStack(Items.RABBIT_STEW);
            poorC6.getOrCreateTag().putInt(ShopData.CUISINE_QUALITY_TAG, 3);
            ItemStack excellentC6 = new ItemStack(Items.RABBIT_STEW);
            excellentC6.getOrCreateTag().putInt(ShopData.CUISINE_QUALITY_TAG, 1);
            ItemStack superbC6 = new ItemStack(Items.RABBIT_STEW);
            superbC6.getOrCreateTag().putInt(ShopData.CUISINE_QUALITY_TAG, 0);
            ShopData.Entry noFlexC6 = new ShopData.Entry(new ItemStack(Items.RABBIT_STEW), 300, 200.0, "dish", true, false, true, false);
            check("a dish from a free-form pot recipe (it carries a quality tag) is bought for its own ingredient cost; the standard dish and a Superb one keep the full price",
                    anyShopC6 != null && anyShopC6.sellUnitFor(dishC6, plainC6) == 200.0 && anyShopC6.sellUnitFor(dishC6, poorC6) == 60.0
                            && anyShopC6.sellUnitFor(dishC6, excellentC6) == 60.0 && anyShopC6.sellUnitFor(dishC6, superbC6) == 200.0
                            && anyShopC6.variantKey(dishC6, poorC6) != anyShopC6.variantKey(dishC6, plainC6)
                            && anyShopC6.sellUnitFor(noFlexC6, poorC6) == 200.0 && dishC6.matchesLoose(poorC6)
                            && ShopData.cuisineQuality(plainC6) == -1 && ShopData.cuisineQuality(poorC6) == 3,
                    anyShopC6 == null ? "no food shop" : anyShopC6.sellUnitFor(dishC6, plainC6) + " " + anyShopC6.sellUnitFor(dishC6, poorC6) + " " + anyShopC6.sellUnitFor(dishC6, superbC6));
            // 운영자가 막은 조리법은 매입가의 기준에서 빠진다 (ID · mod: · item: 세 가지 적는 법)
            com.rtshop.shop.RecipeSolver.Recipe rcpC6 = new com.rtshop.shop.RecipeSolver.Recipe();
            rcpC6.id = "create:smelting/bread";
            rcpC6.type = "minecraft:smelting";
            rcpC6.outputs.add(new com.rtshop.shop.RecipeSolver.Out("minecraft:bread", 1));
            check("a recipe the operator blocked (by id, by mod or by item) is left out of the food price basis; others stay in",
                    com.rtshop.shop.ShopLoader.blockedForTest(rcpC6, java.util.Set.of("create:smelting/bread"))
                            && com.rtshop.shop.ShopLoader.blockedForTest(rcpC6, java.util.Set.of("mod:create"))
                            && com.rtshop.shop.ShopLoader.blockedForTest(rcpC6, java.util.Set.of("item:minecraft:bread"))
                            && !com.rtshop.shop.ShopLoader.blockedForTest(rcpC6, java.util.Set.of("minecraft:bread", "mod:farmersdelight", "item:minecraft:cake"))
                            && !com.rtshop.shop.ShopLoader.blockedForTest(rcpC6, java.util.Set.of()));
            if (net.minecraftforge.fml.ModList.get().isLoaded("kaleidoscope_cookery")) {
                net.minecraft.world.item.Item beefC6 = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation("kaleidoscope_cookery", "braised_beef"));
                ShopData.Entry beC6 = null;
                for (ShopData shopC6 : ShopServer.shops().values()) {
                    for (ShopData.Entry enC6 : shopC6.entries) {
                        if (beC6 == null && beefC6 != null && enC6.sellable && enC6.stack.is(beefC6)) beC6 = enC6;
                    }
                }
                // 어느 상점도 그 요리를 사지 않으면(상점 파일을 고친 서버) 볼 것이 없다
                if (beC6 != null) {
                    check("with the cooking mod: braised beef keeps its full-recipe price, and the free-form version is bought for well under it",
                            beC6.flexUnit > 0 && beC6.flexUnit < beC6.sellUnit * 0.7 && beC6.looseSell, beC6.sellUnit + " / flex " + beC6.flexUnit);
                }
            }
        }
        com.rtshop.rules.LootRules.Rules lr = new com.rtshop.rules.LootRules.Rules();
        com.rtshop.rules.LootRules.Table t = new com.rtshop.rules.LootRules.Table();
        t.remove.add("minecraft:saddle");
        com.rtshop.rules.LootRules.Add add = new com.rtshop.rules.LootRules.Add();
        add.item = "minecraft:diamond";
        add.chance = 1.0;
        add.min = 2;
        add.max = 2;
        t.add.add(add);
        lr.tables.put("minecraft:chests/simple_dungeon", t);
        com.rtshop.rules.LootRules.Table g = new com.rtshop.rules.LootRules.Table();
        com.rtshop.rules.LootRules.Add em = new com.rtshop.rules.LootRules.Add();
        em.item = "minecraft:emerald_block";
        em.chance = 0.5;
        em.min = 1;
        em.max = 1;
        g.add.add(em);
        lr.tables.put("minecraft:chests/*", g);
        try {
            com.rtshop.rules.LootRules.setForTest(lr);
            java.util.Map<String, com.rtshop.rules.LootRules.Stat> after = com.rtshop.rules.LootRules.sample(level, dungeon, 300, false);
            com.rtshop.rules.LootRules.Stat dia = after.get("minecraft:diamond");
            check("rules apply to real chest loot: no saddle, always 2 diamonds", !after.containsKey("minecraft:saddle") && dia != null
                    && dia.hits == 300 && dia.min >= 2, dia == null ? "no diamond" : dia.hits + " " + dia.min);
            java.util.Map<String, com.rtshop.rules.LootRules.Stat> desert = com.rtshop.rules.LootRules.sample(level,
                    new ResourceLocation("minecraft:chests/desert_pyramid"), 400, false);
            com.rtshop.rules.LootRules.Stat eb = desert.get("minecraft:emerald_block");
            check("a group rule (minecraft:chests/*) applies to every chest at its chance (about 50%)",
                    eb != null && eb.hits > 400 * 0.38 && eb.hits < 400 * 0.62, eb == null ? "none" : eb.hits);
            level.setBlockAndUpdate(new BlockPos(0, 120, 4100), net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
            if (level.getBlockEntity(new BlockPos(0, 120, 4100)) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity cb) {
                cb.setLootTable(dungeon, 7L);
                cb.unpackLootTable(null);
                int d = 0;
                boolean s2 = false;
                for (int i = 0; i < cb.getContainerSize(); i++) {
                    if (cb.getItem(i).is(Items.DIAMOND)) d += cb.getItem(i).getCount();
                    if (cb.getItem(i).is(Items.SADDLE)) s2 = true;
                }
                check("a placed dungeon chest gets the changed loot when opened", d >= 2 && !s2, d);
            }
            level.setBlockAndUpdate(new BlockPos(0, 120, 4100), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        } finally {
            com.rtshop.rules.LootRules.reload();
        }
    }

    // ------------------------------------------------------------------ 강화

    private static double attr(ItemStack s, net.minecraft.world.entity.EquipmentSlot slot, net.minecraft.world.entity.ai.attributes.Attribute a) {
        double v = 0;
        for (net.minecraft.world.entity.ai.attributes.AttributeModifier m : s.getAttributeModifiers(slot).get(a)) {
            if (m.getOperation() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION) v += m.getAmount();
        }
        return v;
    }

    /** 3.68.1: Better Nether 블록을 실제로 캐 본다 — 그 모드의 물건은 나오지 않고, 같은 방법으로 캔 석탄 광석에서는 석탄이 나온다. */
    private static void betterNetherDrops(ServerLevel level, FakePlayer a) {
        net.minecraft.world.level.GameType mode = a.gameMode.getGameModeForPlayer();
        ItemStack hand = a.getMainHandItem().copy();
        BlockPos spawn = level.getSharedSpawnPos();
        int gx = Math.floorDiv(spawn.getX(), 32) * 32, gz = Math.floorDiv(spawn.getZ(), 32) * 32;
        int y = Math.min(level.getMaxBuildHeight() - 60, 250);
        try {
            a.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            a.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_PICKAXE));
            BlockPos pc = new BlockPos(gx + 2, y, gz + 6);
            level.setBlock(pc, Blocks.COAL_ORE.defaultBlockState(), 3);
            a.gameMode.destroyBlock(pc);
            boolean coal = takeDrops(level, pc).stream().anyMatch(s -> s.is(Items.COAL));
            int tried = 0;
            int guardBefore = com.rtshop.rules.BlockDropGuard.blockedForTest;
            List<String> leaked = new ArrayList<>();
            for (String id : new String[]{"cincinnasite_ore", "nether_ruby_ore", "cincinnasite_block", "nether_brick_tile_large", "willow_planks", "stalagnate_planks"}) {
                net.minecraft.world.level.block.Block blk = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("betternether", id));
                if (blk == null || blk == Blocks.AIR) continue;
                BlockPos p = new BlockPos(gx + 6 + tried * 4, y, gz + 6);
                level.setBlock(p, blk.defaultBlockState(), 3);
                a.gameMode.destroyBlock(p);
                for (ItemStack s : takeDrops(level, p)) {
                    ResourceLocation rl = ForgeRegistries.ITEMS.getKey(s.getItem());
                    if (rl != null && rl.getNamespace().equals("betternether")) leaked.add(rl.toString());
                }
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                tried++;
            }
            check("3.68.1 breaking Better Nether blocks drops none of their items (a coal ore broken the same way still drops coal)",
                    coal && tried > 0 && leaked.isEmpty(),
                    "coal " + coal + " tried " + tried + " leaked " + leaked + " stopped by the guard " + (com.rtshop.rules.BlockDropGuard.blockedForTest - guardBefore));
        } finally {
            a.setGameMode(mode);
            a.setItemInHand(InteractionHand.MAIN_HAND, hand);
        }
    }

    /** 3.68.0: 직업별 수입 기록 — 출처 나누기, 직업 칸, 움직인 시간, 시간당, 표 파일. */
    private static void incomeStats368(MinecraftServer server, FakePlayer a, ShopData fisher, ShopData food) {
        com.rtshop.stats.IncomeStats is = com.rtshop.stats.IncomeStats.get(server);
        is.reset();
        com.rtshop.stats.IncomeStats.testing = true;
        try {
            com.rtshop.job.Jobs.force(a, "life", "fisher");
            com.rtshop.job.Jobs.force(a, "combat", "swordsman");
            com.rtshop.stats.IncomeStats.add(a, "sell_fish", 2500);
            com.rtshop.stats.IncomeStats.add(a, "quest", 500);
            com.rtshop.stats.IncomeStats.add(a, "sell_fish", 0);   // 0 은 무시
            com.rtshop.stats.IncomeStats.addTime(a, 1800);
            com.rtshop.stats.IncomeStats.addTime(a, 1800);
            com.rtshop.job.Jobs.force(a, "life", "farmer");
            com.rtshop.stats.IncomeStats.add(a, "sell_farm", 700);
            com.rtshop.stats.IncomeStats.addTime(a, 300);   // 10분이 안 되면 시간당은 '-'
            List<com.rtshop.stats.IncomeStats.Sum> life = is.sums("life");
            List<com.rtshop.stats.IncomeStats.Sum> combat = is.sums("combat");
            com.rtshop.stats.IncomeStats.Sum fish = life.stream().filter(x -> x.key.equals("life:fisher")).findFirst().orElse(null);
            com.rtshop.stats.IncomeStats.Sum farm = life.stream().filter(x -> x.key.equals("life:farmer")).findFirst().orElse(null);
            com.rtshop.stats.IncomeStats.Sum sword = combat.stream().filter(x -> x.key.equals("combat:swordsman")).findFirst().orElse(null);
            check("3.68.0 income is written to the earner's life and combat job with its source; time adds up; per hour = gold / hours (none under 10 minutes)",
                    fish != null && fish.seconds == 3600 && fish.gold.get("sell_fish") == 2500 && fish.gold.get("quest") == 500 && Math.abs(fish.perHour() - 3000) < 1e-6
                            && farm != null && farm.perHour() < 0 && farm.gold.get("sell_farm") == 700
                            && sword != null && sword.total() == 3700 && sword.seconds == 3900,
                    (fish == null ? "no fisher" : fish.seconds + "s " + fish.gold) + " / " + (farm == null ? "no farmer" : farm.seconds + "s") + " / " + (sword == null ? "no sword" : sword.total() + " " + sword.seconds));
            String fishLine = fish == null ? "" : com.rtshop.stats.IncomeStats.line(fish);
            java.nio.file.Path txt = is.export();
            String body = Files.readString(txt, StandardCharsets.UTF_8);
            check("3.68.0 the report line and the exported table show the job, hours and gold per hour",
                    fishLine.startsWith("어부") && fishLine.contains("시간당 3,000 G") && body.contains("== 생활 직업") && body.contains("물고기 판매: 2,500 G")
                            && Files.exists(txt.resolveSibling("income.csv")), fishLine);
            // 움직인 시간만: 처음 본 때는 움직인 것으로, 5분 넘게 그대로면 잠수, 다시 움직이면 셈
            com.rtshop.stats.IncomeStats.forgetForTest(a.getUUID());
            long t0 = 1_000_000L;
            boolean first = com.rtshop.stats.IncomeStats.activeForTest(a, t0);
            boolean idle = com.rtshop.stats.IncomeStats.activeForTest(a, t0 + 6 * 60 * 1000L);
            double ox = a.getX();
            a.setPos(ox + 3, a.getY(), a.getZ());   // 가짜 플레이어는 teleportTo 로 자리가 바뀌지 않는다
            boolean back = com.rtshop.stats.IncomeStats.activeForTest(a, t0 + 7 * 60 * 1000L);
            a.setPos(ox, a.getY(), a.getZ());
            com.rtshop.stats.IncomeStats.forgetForTest(a.getUUID());
            check("3.68.0 only active time counts: standing still for over 5 minutes stops the clock, moving starts it again", first && !idle && back,
                    first + " " + idle + " " + back);
            // 상점 판매의 출처 나누기
            int codIdx = fisher == null ? -1 : findEntry(fisher, e -> e.stack.is(Items.COD) && e.fish);
            ShopData farmerShop = ShopServer.shops().get("farmer");
            int wheatIdx = farmerShop == null ? -1 : findEntry(farmerShop, e -> e.stack.is(Items.WHEAT) && e.sellable);
            int beefIdx = food == null ? -1 : findEntry(food, e -> e.stack.is(Items.COOKED_BEEF) && e.sellable);
            String srcFish = codIdx < 0 ? "?" : ShopServer.incomeSourceForTest(fisher, fisher.entries.get(codIdx));
            String srcFarm = wheatIdx < 0 ? "?" : ShopServer.incomeSourceForTest(farmerShop, farmerShop.entries.get(wheatIdx));
            String srcFood = beefIdx < 0 ? "?" : ShopServer.incomeSourceForTest(food, food.entries.get(beefIdx));
            check("3.68.0 shop sales are sorted by source: fish, farm goods, food",
                    (codIdx < 0 || srcFish.equals("sell_fish")) && (wheatIdx < 0 || srcFarm.equals("sell_farm")) && (beefIdx < 0 || srcFood.equals("sell_food")),
                    srcFish + " " + srcFarm + " " + srcFood);
        } catch (Exception ex) {
            check("3.68.0 income stats work", false, ex.toString());
        } finally {
            com.rtshop.stats.IncomeStats.testing = false;
            is.reset();
            com.rtshop.job.Jobs.force(a, "life", "cook");
        }
    }

    /** 3.66.0: 실패 누적(B) · 보호권 값 그대로 · 높은 단계 수치 · 방어구 피해 감소 · 마법 뽑기 천장 없음(A) · 씨앗값(N). */
    private static void enhance366(ServerLevel level, BlockPos pos, FakePlayer a, com.rtshop.enhance.EnhanceConfig cfg, ItemStack sword) {
        com.rtshop.enhance.EnhanceConfig.Level raw11 = cfg.level(11);
        ItemStack st3 = new ItemStack(Items.IRON_SWORD);
        com.rtshop.enhance.Enhance.setFailStacks(st3, 3);
        com.rtshop.enhance.EnhanceConfig.Level up11 = com.rtshop.enhance.Enhance.levelFor(cfg, raw11, st3);
        ItemStack st9 = new ItemStack(Items.IRON_SWORD);
        com.rtshop.enhance.Enhance.setFailStacks(st9, 9);
        com.rtshop.enhance.EnhanceConfig.Level cap11 = com.rtshop.enhance.Enhance.levelFor(cfg, raw11, st9);
        double sum = up11.success + up11.maintain + up11.damage + up11.destroy;
        check("3.66.0 each failure raises the next success chance by 10% of that level's chance, up to 5 times; the other outcomes shrink so the total stays 100",
                cfg.failBonusPct == 10 && cfg.failStackMax == 5 && Math.abs(up11.success - raw11.success * 1.3) < 1e-9 && Math.abs(sum - 100) < 1e-6
                        && Math.abs(cap11.success - raw11.success * 1.5) < 1e-9 && up11.destroy < raw11.destroy
                        && com.rtshop.enhance.Enhance.failStacksAfter(cfg, 0, com.rtshop.enhance.Enhance.Outcome.MAINTAIN) == 1
                        && com.rtshop.enhance.Enhance.failStacksAfter(cfg, 5, com.rtshop.enhance.Enhance.Outcome.DAMAGE) == 5
                        && com.rtshop.enhance.Enhance.failStacksAfter(cfg, 4, com.rtshop.enhance.Enhance.Outcome.SUCCESS) == 0,
                raw11.success + " -> " + up11.success + " / cap " + cap11.success + " sum " + sum);
        // 모루에서: 유지 두 번 → 누적 2, 그다음 성공 → 0
        com.rtshop.enhance.Enhance.setActiveGuard(a, 0);
        giveMaterials(a, cfg, 7, 4);
        ItemStack f1 = enhanceOnce(level, pos, a, sword, 6, 0, 0.30, net.minecraft.world.inventory.ClickType.PICKUP);
        ItemStack f2 = enhanceOnce(level, pos, a, f1, 6, 0, 0.30, net.minecraft.world.inventory.ClickType.PICKUP);
        ItemStack f3 = enhanceOnce(level, pos, a, f2, 6, 0, 0.0, net.minecraft.world.inventory.ClickType.PICKUP);
        check("3.66.0 on the anvil a failed attempt adds one to the item's failure count, and a success clears it",
                com.rtshop.enhance.Enhance.level(f1) == 6 && com.rtshop.enhance.Enhance.failStacks(f1) == 1 && com.rtshop.enhance.Enhance.failStacks(f2) == 2
                        && com.rtshop.enhance.Enhance.level(f3) == 7 && com.rtshop.enhance.Enhance.failStacks(f3) == 0,
                com.rtshop.enhance.Enhance.level(f1) + "/" + com.rtshop.enhance.Enhance.failStacks(f1) + " " + com.rtshop.enhance.Enhance.failStacks(f2)
                        + " " + com.rtshop.enhance.Enhance.level(f3) + "/" + com.rtshop.enhance.Enhance.failStacks(f3));
        ShopLoader.Price dp = ShopServer.loaded().prices.get("rtshop:enhance_guard_damage");
        ShopLoader.Price xp = ShopServer.loaded().prices.get("rtshop:enhance_guard_destroy");
        ShopLoader.Price fp = ShopServer.loaded().prices.get("rtshop:enhance_guard_full");
        check("3.69.0 guard ticket prices fit their levels: damage 15,000 (gear that cannot break) · destruction 4,000 (+5 ~ +6) · full 35,000 G (+7 and up)",
                dp != null && dp.price == 15000 && xp != null && xp.price == 4000 && fp != null && fp.price == 35000
                        && java.util.Arrays.asList(ShopLoader.REPRICED).contains("blacksmith:rtshop:enhance_guard_destroy=20000>4000@1"),
                (dp == null ? "?" : dp.price) + " / " + (xp == null ? "?" : xp.price) + " / " + (fp == null ? "?" : fp.price));
        check("3.66.0 high levels give much more: +8 ~ +11 attack bonus 40 / 55 / 75 / 100%, armour 40 / 50 / 60 / 75%; +11 costs 40,000 G at 5%",
                cfg.weaponBonus[8] == 40 && cfg.weaponBonus[11] == 100 && cfg.armorBonus[9] == 50 && cfg.armorBonus[11] == 75
                        && cfg.level(11).gold == 40000 && cfg.level(11).success == 5 && cfg.level(9).success == 10,
                java.util.Arrays.toString(cfg.weaponBonus) + " " + cfg.level(11).gold);
        // +8 이상 방어구: 받는 피해가 조각마다 줄어든다 (방어력은 30 에서 막히므로)
        net.minecraft.world.item.Item[] set = {Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS};
        net.minecraft.world.entity.EquipmentSlot[] slots = {net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET};
        ItemStack[] keepArmor = new ItemStack[4];
        for (int i = 0; i < 4; i++) {
            keepArmor[i] = a.getItemBySlot(slots[i]).copy();
            ItemStack piece = new ItemStack(set[i]);
            com.rtshop.enhance.Enhance.setLevel(piece, 11);
            a.setItemSlot(slots[i], piece);
        }
        double full = com.rtshop.enhance.EnhanceRanged.armorReducePct(cfg, a);
        net.minecraftforge.event.entity.living.LivingHurtEvent hit =
                new net.minecraftforge.event.entity.living.LivingHurtEvent(a, level.damageSources().generic(), 10F);
        com.rtshop.enhance.EnhanceRanged.onHurtArmor(hit);
        com.rtshop.enhance.Enhance.setLevel(a.getItemBySlot(slots[0]), 8);
        for (int i = 1; i < 4; i++) a.setItemSlot(slots[i], ItemStack.EMPTY);
        double one8 = com.rtshop.enhance.EnhanceRanged.armorReducePct(cfg, a);
        for (int i = 0; i < 4; i++) a.setItemSlot(slots[i], keepArmor[i]);
        check("3.66.0 armour from +8 also cuts damage taken: four +11 pieces take 30% less (10 -> 7), one +8 piece 1.5%",
                Math.abs(full - 30) < 1e-9 && Math.abs(hit.getAmount() - 7F) < 0.01 && Math.abs(one8 - 1.5) < 1e-9, full + " " + hit.getAmount() + " " + one8);
        check("3.66.0 the magic gacha has no pity (the old 80-pull setting is not used)", !com.rtshop.magic.MagicConfig.get().pityOn);
        int seed30 = 0, seed50 = 0;
        ShopData farmer = ShopServer.shops().get("farmer");
        if (farmer != null) {
            for (ShopData.Entry en : farmer.entries) {
                if (!en.buyable || !(en.stack.is(Items.CARROT) || en.stack.is(Items.POTATO))) continue;
                if (en.price == 30) seed30++;
                if (en.price == 50) seed50++;
            }
            check("3.66.0 carrot and potato seeds cost 30 G (were 50 G, more than the crop is worth)", seed30 >= 2 && seed50 == 0, seed30 + " at 30, " + seed50 + " at 50");
        }
    }

    private static void giveMaterials(FakePlayer a, com.rtshop.enhance.EnhanceConfig cfg, int target, int times) {
        for (com.rtshop.enhance.Enhance.Need n : com.rtshop.enhance.Enhance.needs(cfg, cfg.level(target), a)) {
            a.getInventory().add(new ItemStack(n.items.get(0), n.need * times));
        }
    }

    private static com.rtshop.enhance.EnhanceMenu enhanceMenu(ServerLevel level, BlockPos pos, FakePlayer a) {
        net.minecraft.world.level.block.state.BlockState st = level.getBlockState(pos);
        com.rtshop.enhance.EnhanceMenu m = (com.rtshop.enhance.EnhanceMenu) st.getMenuProvider(level, pos).createMenu(1, a.getInventory(), a);
        a.containerMenu = m;
        return m;
    }

    /** 장비를 올리고(단계 cur), 방지권을 넣고, 굴림값을 정해서 결과 칸을 클릭. 손에 든(또는 가방의) 결과를 돌려준다. */
    private static ItemStack enhanceOnce(ServerLevel level, BlockPos pos, FakePlayer a, ItemStack gear, int cur, int guardMask, double roll,
                                         net.minecraft.world.inventory.ClickType click) {
        com.rtshop.enhance.EnhanceMenu m = enhanceMenu(level, pos, a);
        ItemStack g = gear.copy();
        com.rtshop.enhance.Enhance.setLevel(g, cur);
        m.getSlot(0).set(g);
        com.rtshop.enhance.Enhance.setActiveGuard(a, guardMask);
        com.rtshop.enhance.Enhance.testRoll = roll;
        try {
            m.clicked(2, 0, click, a);
        } finally {
            com.rtshop.enhance.Enhance.testRoll = null;
        }
        ItemStack out = m.getCarried().copy();
        m.setCarried(ItemStack.EMPTY);
        return out;
    }

    private static void enhance(MinecraftServer server, ServerLevel level, FakePlayer a) throws Exception {
        section("enhance");
        com.rtshop.enhance.EnhanceConfig cfg = com.rtshop.enhance.EnhanceConfig.get();
        boolean shape = true;
        StringBuilder bad = new StringBuilder();
        for (int i = 1; i <= 11; i++) {
            com.rtshop.enhance.EnhanceConfig.Level l = cfg.level(i);
            double sum = l.success + l.maintain + l.damage + l.destroy;
            if (Math.abs(sum - 100) > 1e-6) { shape = false; bad.append(" sum+").append(i); }
            if (i > 1) {
                com.rtshop.enhance.EnhanceConfig.Level p = cfg.level(i - 1);
                if (!(l.success < p.success && l.damage >= p.damage && l.destroy >= p.destroy && l.gold > p.gold)) {
                    shape = false;
                    bad.append(" +").append(i);
                }
            }
        }
        check("11 enhancement levels: success falls, damage/destroy rise, cost rises, odds add up to 100", shape, bad);
        enhanceRanged(level, a);
        extremeGear(level, a);
        check("from +4 success drops sharply and keep has a big share", cfg.level(4).success <= 50 && cfg.level(3).success - cfg.level(4).success >= 20
                && cfg.level(4).maintain >= 30 && cfg.level(11).success <= 5 && cfg.level(8).maintain >= 30);
        // 1판 기본 확률 파일은 2판 확률로 올라간다 (OP 가 바꾼 단계는 그대로)
        java.nio.file.Path ep = com.rtshop.enhance.EnhanceConfig.path();
        String keep = Files.exists(ep) ? Files.readString(ep, StandardCharsets.UTF_8) : null;
        try {
            com.google.gson.JsonObject old = cfg.toJson();
            old.addProperty("configVersion", 1);
            double[][] o1 = {{90, 9, 1, 0}, {83, 9, 8, 0}, {75, 8.5, 16.5, 0}};
            for (int i = 0; i < 3; i++) {
                com.google.gson.JsonObject lo = old.getAsJsonArray("levels").get(i).getAsJsonObject();
                lo.addProperty("success", o1[i][0]);
                lo.addProperty("maintain", o1[i][1]);
                lo.addProperty("damage", o1[i][2]);
                lo.addProperty("destroy", o1[i][3]);
            }
            old.getAsJsonArray("levels").get(2).getAsJsonObject().addProperty("success", 70.5);
            Files.writeString(ep, old.toString(), StandardCharsets.UTF_8);
            com.rtshop.enhance.EnhanceConfig.reload();
            com.rtshop.enhance.EnhanceConfig up = com.rtshop.enhance.EnhanceConfig.get();
            com.rtshop.enhance.EnhanceConfig dft = new com.rtshop.enhance.EnhanceConfig();
            dft.defaults();
            check("an old enhance.json with default odds is upgraded; edited levels are kept",
                    up.level(1).success == dft.level(1).success && up.level(1).maintain == dft.level(1).maintain && up.level(2).success == dft.level(2).success
                            && up.level(3).success == 70.5,
                    up.level(1).success + " " + up.level(2).success + " " + up.level(3).success);
            // 3.66.0: 2판 기본값 그대로인 단계는 3판(성공률 ↑ · Gold ↓)으로, 서버에서 바꾼 단계와 보너스 표는 그대로
            com.google.gson.JsonObject v2 = cfg.toJson();
            v2.addProperty("configVersion", 2);
            com.google.gson.JsonObject l11 = v2.getAsJsonArray("levels").get(10).getAsJsonObject();
            l11.addProperty("success", 3);
            l11.addProperty("maintain", 31);
            l11.addProperty("damage", 32);
            l11.addProperty("destroy", 34);
            l11.addProperty("gold", 65000);
            com.google.gson.JsonObject l10 = v2.getAsJsonArray("levels").get(9).getAsJsonObject();
            l10.addProperty("success", 6);
            l10.addProperty("maintain", 32);
            l10.addProperty("damage", 32);
            l10.addProperty("destroy", 30);
            l10.addProperty("gold", 40000);
            com.google.gson.JsonArray oldWeapon = new com.google.gson.JsonArray();
            for (double v : new double[]{0, 3, 6, 9, 13, 17, 22, 28, 35, 43, 52, 62}) oldWeapon.add(v);
            v2.add("weaponBonus", oldWeapon);
            v2.remove("failBonusPct");
            Files.writeString(ep, v2.toString(), StandardCharsets.UTF_8);
            com.rtshop.enhance.EnhanceConfig.reload();
            com.rtshop.enhance.EnhanceConfig up3 = com.rtshop.enhance.EnhanceConfig.get();
            check("3.66.0 an enhance.json from 3.65 moves its default levels to the new odds and Gold, keeps a level the server changed, and gets the new bonus table",
                    up3.level(11).success == dft.level(11).success && up3.level(11).gold == dft.level(11).gold && up3.level(10).success == 6
                            && up3.level(10).gold == 40000 && up3.weaponBonus[11] == dft.weaponBonus[11] && up3.failBonusPct == dft.failBonusPct,
                    up3.level(11).success + "/" + up3.level(11).gold + " " + up3.level(10).success + "/" + up3.level(10).gold + " " + up3.weaponBonus[11]);
            // OP 편집기 저장
            com.google.gson.JsonObject ed = cfg.toJson();
            ed.getAsJsonArray("levels").get(0).getAsJsonObject().addProperty("success", 77);
            ed.getAsJsonArray("levels").get(0).getAsJsonObject().addProperty("maintain", 23);
            com.rtshop.enhance.EnhanceConfig.saveFromEditor(ed.toString());
            check("the OP editor saves odds and applies them at once", com.rtshop.enhance.EnhanceConfig.get().level(1).success == 77
                    && Files.readString(ep, StandardCharsets.UTF_8).contains("77"));
            boolean refused = false;
            try {
                com.google.gson.JsonObject badj = cfg.toJson();
                com.google.gson.JsonObject l1 = badj.getAsJsonArray("levels").get(0).getAsJsonObject();
                l1.addProperty("success", 0);
                l1.addProperty("maintain", 0);
                l1.addProperty("damage", 0);
                l1.addProperty("destroy", 0);
                com.rtshop.enhance.EnhanceConfig.saveFromEditor(badj.toString());
            } catch (Exception e) {
                refused = true;
            }
            check("the editor refuses a level whose odds are all zero", refused && com.rtshop.enhance.EnhanceConfig.get().level(1).success == 77);
        } finally {
            if (keep != null) Files.writeString(ep, keep, StandardCharsets.UTF_8);
            else Files.deleteIfExists(ep);
            com.rtshop.enhance.EnhanceConfig.reload();
            cfg = com.rtshop.enhance.EnhanceConfig.get();
        }
        check("the first levels cannot destroy, high levels can", cfg.level(3).destroy == 0 && cfg.level(4).destroy > 0 && cfg.level(11).destroy > 30);
        boolean mm = net.minecraftforge.fml.ModList.get().isLoaded("miningmaster");
        boolean sp = net.minecraftforge.fml.ModList.get().isLoaded("spelunkery");
        int mats1 = com.rtshop.enhance.Enhance.needs(cfg, cfg.level(1), null).size();
        int mats11 = com.rtshop.enhance.Enhance.needs(cfg, cfg.level(11), null).size();
        info("enhance materials: +1 needs " + mats1 + " kinds, +11 needs " + mats11 + " kinds (miningmaster " + mm + ", spelunkery " + sp + ")");
        check("materials of uninstalled mods are skipped, installed ones are required", mats1 == (mm ? 1 : 0) + (sp ? 1 : 0)
                && mats11 == (mm ? 2 : 0) + (sp ? 2 : 0), mats1 + " / " + mats11);
        if (mm) {
            check("+10 and +11 need the rarest Mining Master gems (divine beryl / air malachite)",
                    cfg.itemsOf(cfg.level(10).materials.get(0)).contains(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation("miningmaster:divine_beryl"))));
        }
        // 상점: 장비 상인이 방지권을 판다, 손상 < 파괴, 보호권 = 둘의 합
        ShopLoader.Price pd = ShopServer.loaded().prices.get("rtshop:enhance_guard_damage");
        ShopLoader.Price px = ShopServer.loaded().prices.get("rtshop:enhance_guard_destroy");
        ShopLoader.Price pf = ShopServer.loaded().prices.get("rtshop:enhance_guard_full");
        ShopData bs = ShopServer.shops().get("blacksmith");
        boolean sold = bs != null && findEntry(bs, e -> net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(e.stack.getItem()).toString().equals("rtshop:enhance_guard_full")) >= 0
                && findEntry(bs, e -> net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(e.stack.getItem()).toString().equals("rtshop:enhance_anvil")) < 0;
        check("the equipment merchant sells the guard tickets but not the enhance anvil (there is one in town)", sold);
        check("3.69.0 guard tickets fit their levels: destruction guard is the cheapest (+5 ~ +6), protection the dearest (+7 and up), damage guard in between (gear that cannot break)",
                pd != null && px != null && pf != null && px.price < pd.price && pd.price < pf.price,
                pd == null ? "missing" : pd.price + " / " + (px == null ? "?" : px.price) + " / " + (pf == null ? "?" : pf.price));
        // 기존 상점 파일에 한 번만 덧붙임
        java.nio.file.Path tmp = Files.createTempFile("rtshop_bs", ".json");
        try {
            Files.writeString(tmp, "{\"title\":\"x\",\"categories\":[{\"id\":\"weapon\",\"name\":\"w\",\"icon\":\"minecraft:stone\"}],"
                    + "\"items\":[{\"item\":\"rtshop:enhance_guard_damage\",\"price\":1}]}", StandardCharsets.UTF_8);
            int n1 = ShopLoader.appendItems(tmp, "blacksmith_enhance");
            int n2 = ShopLoader.appendItems(tmp, "blacksmith_enhance");
            String txt = Files.readString(tmp, StandardCharsets.UTF_8);
            check("new enhance items are added to an existing blacksmith file once, keeping edited prices",
                    n1 > 3 && n2 == 0 && txt.contains("\"price\": 1,") == false && txt.contains("\"price\": 1") && txt.contains("enhance_guard_full")
                            && txt.contains("\"id\": \"enhance\""), n1 + " " + n2);
        } finally {
            Files.deleteIfExists(tmp);
        }

        // OP 상점 편집기: 품목 추가·가격 바꾸기가 저장·적용되고 .bak 로 되돌릴 수 있다
        if (com.rtshop.shop.ShopEditor.validId("general")) {
            java.nio.file.Path gf = ShopLoader.dir().resolve("general.json");
            String orig = Files.readString(gf, StandardCharsets.UTF_8);
            try {
                com.google.gson.JsonObject go = com.google.gson.JsonParser.parseString(orig).getAsJsonObject();
                com.google.gson.JsonObject it = new com.google.gson.JsonObject();
                it.addProperty("item", "minecraft:end_crystal");
                it.addProperty("price", 12345);
                it.addProperty("category", go.getAsJsonArray("categories").get(0).getAsJsonObject().get("id").getAsString());
                go.getAsJsonArray("items").add(it);
                java.nio.file.Path bak = com.rtshop.shop.ShopEditor.save(server, "general", go.toString());
                ShopLoader.Price pr = ShopServer.loaded().prices.get("minecraft:end_crystal");
                check("the OP shop editor saves items and prices and reloads the shops", pr != null && pr.price == 12345 && Files.exists(bak),
                        pr == null ? "missing" : pr.price);
            } finally {
                Files.writeString(gf, orig, StandardCharsets.UTF_8);
                Files.deleteIfExists(gf.resolveSibling("general.json.bak"));
                ShopServer.reload(server);
            }
        }

        // 블록과 창
        BlockPos pos = new BlockPos(0, 120, 4000);
        level.setBlockAndUpdate(pos.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos, com.rtshop.enhance.ModEnhance.ANVIL.get().defaultBlockState());
        check("the enhance anvil is an anvil (Cinematic Interactions plays its anvil scene on it)",
                level.getBlockState(pos).is(net.minecraft.tags.BlockTags.ANVIL));
        if (net.minecraftforge.fml.ModList.get().isLoaded("cinematic")) {
            String kind = "?";
            try {
                kind = String.valueOf(Class.forName("dev.dyslexic.cinematic.station.StationKind")
                        .getMethod("of", net.minecraft.world.level.block.state.BlockState.class).invoke(null, level.getBlockState(pos)));
            } catch (Throwable t) {
                kind = String.valueOf(t);
            }
            check("Cinematic Interactions treats the enhance anvil as an anvil station", "ANVIL".equals(kind), kind);
        }
        clear(a);
        Economy.setWallet(a, 0);
        com.rtshop.enhance.EnhanceMenu m = enhanceMenu(level, pos, a);
        check("its menu is a vanilla anvil menu with its own type", m instanceof net.minecraft.world.inventory.AnvilMenu
                && m.getType() == com.rtshop.enhance.ModEnhance.MENU.get() && m.stillValid(a) == (a.distanceToSqr(Vec3.atCenterOf(pos)) <= 64));
        m.getSlot(0).set(new ItemStack(Items.STICK));
        check("a stick cannot be enhanced (no result)", !m.getSlot(2).hasItem());
        m.getSlot(0).set(new ItemStack(Items.IRON_SWORD));
        ItemStack prev = m.getSlot(2).getItem();
        check("a sword shows a +1 preview", com.rtshop.enhance.Enhance.level(prev) == 1 && com.rtshop.enhance.Enhance.isPreview(prev));
        check("without Gold and materials it cannot be taken", !m.getSlot(2).mayPickup(a));
        m.clicked(2, 0, net.minecraft.world.inventory.ClickType.PICKUP, a);
        check("...and clicking does nothing", m.getCarried().isEmpty() && m.getSlot(0).hasItem());
        m.removed(a);
        check("closing the anvil gives the sword back", count(a, s -> s.is(Items.IRON_SWORD)) == 1);
        clear(a);

        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        Economy.setWallet(a, 1_000_000);
        giveMaterials(a, cfg, 1, 1);
        ItemStack r = enhanceOnce(level, pos, a, sword, 0, 0, 0.0, net.minecraft.world.inventory.ClickType.PICKUP);
        boolean matsGone = com.rtshop.enhance.Enhance.needs(cfg, cfg.level(1), a).stream().allMatch(n -> n.have == 0);
        check("success: +0 -> +1, Gold and materials are used", com.rtshop.enhance.Enhance.level(r) == 1 && !com.rtshop.enhance.Enhance.isPreview(r)
                && Economy.wallet(a) == 1_000_000 - cfg.level(1).gold && matsGone, com.rtshop.enhance.Enhance.level(r) + " " + Economy.wallet(a));
        giveMaterials(a, cfg, 1, 1);
        enhanceOnce(level, pos, a, sword, 0, 0, 0.0, net.minecraft.world.inventory.ClickType.QUICK_MOVE);
        ItemStack inBag = first(a, s -> s.is(Items.IRON_SWORD));
        check("shift-click works too (result goes to the bag)", com.rtshop.enhance.Enhance.level(inBag) == 1 && !com.rtshop.enhance.Enhance.isPreview(inBag));
        clear(a);

        giveMaterials(a, cfg, 7, 10);
        ItemStack dmgGuard = new ItemStack(com.rtshop.item.ModItems.GUARD_DAMAGE.get(), 3);
        // +6 -> +7: success 37, keep 6.5, damage 39, destroy 17.5
        r = enhanceOnce(level, pos, a, sword, 6, 0, 0.40, net.minecraft.world.inventory.ClickType.PICKUP);
        check("keep: the level stays", com.rtshop.enhance.Enhance.level(r) == 6 && r.is(Items.IRON_SWORD));
        r = enhanceOnce(level, pos, a, sword, 6, 0, 0.60, net.minecraft.world.inventory.ClickType.PICKUP);
        check("damage: the level drops by one", com.rtshop.enhance.Enhance.level(r) == 5 && r.is(Items.IRON_SWORD));
        r = enhanceOnce(level, pos, a, sword, 6, 0, 0.99, net.minecraft.world.inventory.ClickType.PICKUP);
        check("destroy: the item is gone", r.isEmpty() && count(a, s -> s.is(Items.IRON_SWORD)) == 0);
        // +7 이상 성공: 서버 알림은 강화한 사람의 화면 연출(긴장 구간)이 끝난 뒤에 나간다
        com.rtshop.util.Later.flushForTest();
        com.rtshop.enhance.Enhance.lastBroadcast = "";
        giveMaterials(a, cfg, 7, 1);
        r = enhanceOnce(level, pos, a, sword, 6, 0, 0.0, net.minecraft.world.inventory.ClickType.PICKUP);
        check("+7 success: the server-wide notice waits for the on-screen build-up", com.rtshop.enhance.Enhance.level(r) == 7
                && com.rtshop.enhance.Enhance.lastBroadcast.isEmpty() && com.rtshop.util.Later.pendingForTest() == 1,
                com.rtshop.enhance.Enhance.lastBroadcast + " / " + com.rtshop.util.Later.pendingForTest());
        com.rtshop.util.Later.flushForTest();
        check("...and is then sent to everyone", com.rtshop.enhance.Enhance.lastBroadcast.startsWith("7:"), com.rtshop.enhance.Enhance.lastBroadcast);
        enhance366(level, pos, a, cfg, sword);
        check("the scene bands are +7~8, +9~10 and +11, each with a longer build-up", com.rtshop.enhance.Enhance.fxTier(6, 7) == 0
                && com.rtshop.enhance.Enhance.fxTier(7, 7) == 1 && com.rtshop.enhance.Enhance.fxTier(8, 7) == 1
                && com.rtshop.enhance.Enhance.fxTier(9, 7) == 2 && com.rtshop.enhance.Enhance.fxTier(10, 7) == 2
                && com.rtshop.enhance.Enhance.fxTier(11, 7) == 3
                && com.rtshop.enhance.Enhance.suspenseMs(1) < com.rtshop.enhance.Enhance.suspenseMs(2)
                && com.rtshop.enhance.Enhance.suspenseMs(2) < com.rtshop.enhance.Enhance.suspenseMs(3));
        // 보호권은 손에 들고 우클릭해 미리 사용 (1장 소모), 같은 보호를 또 걸 수는 없음
        // (빈 칸을 손으로 고른다: 0번 칸에는 강화 재료가 들어 있어서, 그 칸에 보호권을 쥐여 주면 재료가 사라진다)
        int handSlot = a.getInventory().selected;
        a.getInventory().selected = 8;
        check("the test's hand slot is free", a.getMainHandItem().isEmpty());
        a.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, dmgGuard.copy());
        dmgGuard.getItem().use(level, a, net.minecraft.world.InteractionHand.MAIN_HAND);
        int afterUse = a.getMainHandItem().getCount();
        dmgGuard.getItem().use(level, a, net.minecraft.world.InteractionHand.MAIN_HAND);
        check("right-clicking a guard ticket no longer uses it (it only says to put it on the anvil): nothing is spent, nothing is armed",
                com.rtshop.enhance.Enhance.activeGuard(a) == 0 && afterUse == dmgGuard.getCount() && a.getMainHandItem().getCount() == dmgGuard.getCount(),
                com.rtshop.enhance.Enhance.activeGuard(a) + " " + afterUse + " " + a.getMainHandItem().getCount());
        a.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        a.getInventory().selected = handSlot;
        com.rtshop.enhance.EnhanceMenu gm = enhanceMenu(level, pos, a);
        ItemStack g6 = sword.copy();
        com.rtshop.enhance.Enhance.setLevel(g6, 6);
        gm.getSlot(0).set(g6);
        // 3.49: 보호권은 모루 위(1번 칸)에 장비와 함께 올린다 — 한 번에 한 장이 쓰인다
        ItemStack twoTickets = dmgGuard.copy();
        twoTickets.setCount(2);
        boolean onlyTickets = !gm.getSlot(1).mayPlace(new ItemStack(Items.IRON_SWORD)) && gm.getSlot(1).mayPlace(twoTickets)
                && gm.getSlotToQuickMoveTo(twoTickets) == 1 && gm.getSlotToQuickMoveTo(new ItemStack(Items.IRON_SWORD)) == 0;
        gm.getSlot(1).set(twoTickets);
        com.rtshop.enhance.Enhance.testRoll = 0.60;
        gm.clicked(2, 0, net.minecraft.world.inventory.ClickType.PICKUP, a);
        com.rtshop.enhance.Enhance.testRoll = null;
        check("damage guard on the anvil: only guard tickets fit the second slot, damage becomes keep, and exactly one ticket is used",
                onlyTickets && com.rtshop.enhance.Enhance.level(gm.getCarried()) == 6 && gm.getSlot(1).getItem().getCount() == 1
                        && com.rtshop.enhance.Enhance.activeGuard(a) == 0, onlyTickets + " " + com.rtshop.enhance.Enhance.level(gm.getCarried()) + " " + gm.getSlot(1).getItem().getCount());
        gm.getSlot(1).set(ItemStack.EMPTY);
        // 3.64.2: 보호는 모루 위 장비 옆 칸의 보호권으로만 건다 — 옛 방식(우클릭)으로 걸어 둔 보호는 접속할 때 보호권 한 장으로 돌려준다
        int full642 = a.getInventory().countItem(com.rtshop.item.ModItems.GUARD_FULL.get());
        a.getPersistentData().putInt(com.rtshop.enhance.Enhance.GUARD_KEY, 3);
        int back642 = com.rtshop.enhance.Enhance.refundLegacyGuard(a);
        int again642 = com.rtshop.enhance.Enhance.refundLegacyGuard(a);
        int fullNow642 = a.getInventory().countItem(com.rtshop.item.ModItems.GUARD_FULL.get());
        check("3.64.2 protection set the old way (right-click) comes back once as a guard ticket and no longer applies by itself",
                back642 == 1 && again642 == 0 && fullNow642 == full642 + 1 && com.rtshop.enhance.Enhance.activeGuard(a) == 0
                        && com.rtshop.enhance.Enhance.guardFor(a, ItemStack.EMPTY) == com.rtshop.enhance.Enhance.Guard.NONE,
                back642 + " " + again642 + " " + full642 + "->" + fullNow642);
        for (int i642 = 0; i642 < a.getInventory().getContainerSize() && fullNow642 > full642; i642++) {
            ItemStack s642 = a.getInventory().getItem(i642);
            if (s642.is(com.rtshop.item.ModItems.GUARD_FULL.get())) {
                s642.shrink(1);
                fullNow642--;
            }
        }
        // 몹 규칙: 모드 몹의 자연 스폰은 줄이고(기사 hollow 는 그대로, lycanth 는 더 많이), 보스가 아닌 몹은 무기·스킬북·인챈트·모드 아이템을 떨어뜨리지 않는다
        com.rtshop.rules.MobRules.Config mr = com.rtshop.rules.MobRules.fresh();
        com.rtshop.rules.MobRules.setForTest(mr);
        String hollowId = "", lycanId = "", otherId = "";
        double hollowKeep = -1, lycanKeep = -1, otherKeep = -1;
        for (EntityType<?> et : net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES) {
            net.minecraft.resources.ResourceLocation rl = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(et);
            if (rl == null || rl.getNamespace().equals("minecraft") || rl.getNamespace().equals("rtshop") || et.getCategory() != net.minecraft.world.entity.MobCategory.MONSTER) continue;
            String nm = (rl + " " + et.getDescription().getString()).toLowerCase(java.util.Locale.ROOT);
            double k = com.rtshop.rules.MobRules.keepChance(et);
            if (nm.contains("hollow")) {
                if (hollowId.isEmpty()) { hollowId = rl.toString(); hollowKeep = k; }
            } else if (nm.contains("lycanth")) {
                if (lycanId.isEmpty()) { lycanId = rl.toString(); lycanKeep = k; }
            } else if (otherId.isEmpty() && k < 1) {
                otherId = rl.toString();
                otherKeep = k;
            }
        }
        check("spawn rule: vanilla mobs spawn as before; a mod monster keeps 20% of its natural spawns, a hollow (knight) all of them, a lycanth 10%",
                com.rtshop.rules.MobRules.keepChance(EntityType.ZOMBIE) == 1 && (otherId.isEmpty() || Math.abs(otherKeep - 0.2) < 1e-9)
                        && (hollowId.isEmpty() || hollowKeep == 1) && (lycanId.isEmpty() || Math.abs(lycanKeep - 0.1) < 1e-9),
                "other " + otherId + "=" + otherKeep + " hollow " + hollowId + "=" + hollowKeep + " lycanth " + lycanId + "=" + lycanKeep);
        ItemStack enchSword = new ItemStack(Items.GOLDEN_HELMET);
        enchSword.enchant(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING, 1);
        ItemStack modThing = ItemStack.EMPTY;
        for (net.minecraft.world.item.Item it : net.minecraftforge.registries.ForgeRegistries.ITEMS) {
            net.minecraft.resources.ResourceLocation rl = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(it);
            if (rl == null || rl.getNamespace().equals("minecraft") || rl.getNamespace().equals("rtshop")) continue;
            boolean questItem = false;
            for (QuestDef qd : QuestManager.loaded().quests.values()) {
                for (QuestDef.Objective ob : qd.objectives) questItem |= ob.type == QuestDef.Type.DELIVER && ob.target.equals(rl.toString());
            }
            if (!questItem) { modThing = new ItemStack(it); break; }
        }
        net.minecraft.world.entity.boss.wither.WitherBoss witherProbe = EntityType.WITHER.create(level);
        net.minecraft.world.entity.monster.Zombie zombieProbe = EntityType.ZOMBIE.create(level);
        check("drop rule: a common mob keeps its ordinary drops but loses weapons, enchanted things, enchanted books and mod items (RT Shop's own items stay); bosses are left alone",
                com.rtshop.rules.MobRules.dropAllowed(new ItemStack(Items.ROTTEN_FLESH)) && com.rtshop.rules.MobRules.dropAllowed(new ItemStack(Items.IRON_INGOT))
                        && !com.rtshop.rules.MobRules.dropAllowed(new ItemStack(Items.IRON_SWORD)) && !com.rtshop.rules.MobRules.dropAllowed(new ItemStack(Items.BOW))
                        && !com.rtshop.rules.MobRules.dropAllowed(new ItemStack(Items.IRON_AXE)) && !com.rtshop.rules.MobRules.dropAllowed(enchSword)
                        && !com.rtshop.rules.MobRules.dropAllowed(new ItemStack(Items.ENCHANTED_BOOK))
                        && com.rtshop.rules.MobRules.dropAllowed(new ItemStack(com.rtshop.item.ModItems.WEAPON_MANUAL.get()))
                        && (modThing.isEmpty() || !com.rtshop.rules.MobRules.dropAllowed(modThing))
                        && com.rtshop.rules.MobRules.boss(witherProbe) && !com.rtshop.rules.MobRules.boss(zombieProbe), modThing);
        zombieProbe.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
        boolean ownGear = com.rtshop.rules.MobRules.pickedUp(zombieProbe).isEmpty();
        zombieProbe.setGuaranteedDrop(net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        java.util.List<ItemStack> picked = com.rtshop.rules.MobRules.pickedUp(zombieProbe);
        check("a weapon a mob picked up from the ground (a player's) is still dropped: only the mob's own gear is removed",
                ownGear && picked.size() == 1 && picked.get(0).is(Items.DIAMOND_SWORD), ownGear + " / " + picked);
        // 아이템 규칙: 상점·전리품·조합법에서 무엇을 얻을 수 있는가 (item_policy.json)
        com.rtshop.rules.ItemPolicy.Pat pat = new com.rtshop.rules.ItemPolicy.Pat(java.util.List.of("efn:*", "irons_spellbooks:*_ink", "*fruit*:*", "minecraft:tnt"));
        check("item patterns: a whole mod, a wildcard inside the name, a wildcard in the mod id, one exact item",
                pat.hit(new ResourceLocation("efn:yamato")) && pat.hit(new ResourceLocation("irons_spellbooks:rare_ink")) && !pat.hit(new ResourceLocation("irons_spellbooks:ink_sac_x"))
                        && pat.hit(new ResourceLocation("blox_fruits:gomu")) && !pat.hit(new ResourceLocation("minecraft:fruit")) && pat.hit(new ResourceLocation("minecraft:tnt"))
                        && !pat.hit(new ResourceLocation("minecraft:tnt_minecart")));
        {
            com.rtshop.rules.RecipeRules.Rules cg = new com.rtshop.rules.RecipeRules.Rules();
            boolean cgFirst = com.rtshop.rules.RecipeRules.addDefaults(cg, false);
            boolean cgNoVanilla = !cg.disabled.contains("minecraft:beetroot_soup") && cg.disabled.contains("create:smelting/bread") && cg.disabled.size() == 6;
            cg.disabled.remove("create:crafting/curiosities/cake");
            boolean cgOnce = !com.rtshop.rules.RecipeRules.addDefaults(cg, false) && !cg.disabled.contains("create:crafting/curiosities/cake");
            boolean cgFd = com.rtshop.rules.RecipeRules.addDefaults(cg, true) && cg.disabled.contains("minecraft:beetroot_soup") && cg.disabled.contains("minecraft:rabbit_stew_from_red_mushroom");
            com.rtshop.rules.RecipeRules.Rules cgBack = com.rtshop.rules.RecipeRules.Rules.fromJson(cg.toJson());
            check("the cheap duplicate cooking recipes are blocked by default, once: what the operator opens again stays open, and the vanilla soups are blocked only with Farmer's Delight",
                    cgFirst && cgNoVanilla && cgOnce && cgFd && cgBack.applied.size() == 2 && !com.rtshop.rules.RecipeRules.addDefaults(cgBack, true),
                    cgFirst + " " + cgNoVanilla + " " + cgOnce + " " + cgFd + " " + cgBack.applied);
            com.rtshop.rules.ItemPolicy.Config crOld = com.rtshop.rules.ItemPolicy.Config.fromJson(JsonParser.parseString(
                    "{\"version\":2,\"lootBan\":[\"efn:*\"],\"recipeBan\":[],\"recipeBanMods\":[\"tacz\"]}").getAsJsonObject());
            com.rtshop.rules.ItemPolicy.Config crDef = com.rtshop.rules.ItemPolicy.Config.defaults();
            check("Create: its recipes, loot and block drops are banned by default, and an installed item_policy.json (version 2) is brought up to that",
                    crDef.recipeBanMods.contains("create") && crDef.lootBan.contains("create:*") && crDef.recipeBan.contains("create:*") && crDef.blockDropBan.contains("create:*")
                            && crOld.recipeBanMods.contains("create") && crOld.recipeBanMods.contains("tacz") && crOld.lootBan.contains("create:*") && crOld.lootBan.contains("efn:*")
                            && crOld.blockDropBan.contains("create:*") && crOld.recipeBan.contains("create:*")
                            && com.rtshop.rules.ItemPolicy.recipeReason(new ResourceLocation("create:crafting/kinetics/shaft"), null) != null
                            && !com.rtshop.rules.ItemPolicy.blockDropBanned(new ItemStack(Items.COBBLESTONE)),
                    crOld.recipeBanMods + " " + crOld.lootBan + " " + crOld.blockDropBan);
            // 3.58.1 (4판): Create 전용 아이템은 상점과 뽑기에도 오르지 않는다
            com.rtshop.rules.ItemPolicy.Config crOld3 = com.rtshop.rules.ItemPolicy.Config.fromJson(JsonParser.parseString(
                    "{\"version\":3,\"shopBan\":[\"efn:*\"],\"gachaBan\":[\"mymod:thing\"]}").getAsJsonObject());
            com.rtshop.rules.ItemPolicy.Config crKeep4 = com.rtshop.rules.ItemPolicy.Config.fromJson(JsonParser.parseString(
                    "{\"version\":4,\"shopBan\":[\"efn:*\"],\"gachaBan\":[]}").getAsJsonObject());
            List<String> crLeft = new ArrayList<>();
            for (Map.Entry<String, com.rtshop.shop.ShopData> crSh : com.rtshop.shop.ShopServer.shops().entrySet()) {
                for (com.rtshop.shop.ShopData.Entry crSe : crSh.getValue().entries) {
                    ResourceLocation crId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(crSe.stack.getItem());
                    if (crId != null && crId.getNamespace().equals("create")) crLeft.add(crSh.getKey() + ":" + crId);
                }
            }
            for (String crCat : com.rtshop.gacha.GachaConfig.CATEGORIES) {
                for (com.rtshop.gacha.GachaConfig.Entry crGe : com.rtshop.gacha.GachaConfig.get().pool(crCat)) {
                    if ((crGe.item.startsWith("create:") || crGe.entity.startsWith("create:")) && com.rtshop.gacha.GachaConfig.valid(crGe)) crLeft.add("gacha " + crCat + ":" + crGe.item + crGe.entity);
                }
            }
            for (com.rtshop.magic.MagicConfig.Ench crEn : com.rtshop.magic.MagicConfig.get().enchants) {
                if (crEn.id.startsWith("create:") && crEn.enchantment() != null) crLeft.add("magic enchant:" + crEn.id);
            }
            com.rtshop.gacha.GachaConfig.Entry crProbe = new com.rtshop.gacha.GachaConfig.Entry();
            crProbe.item = "minecraft:diamond";
            check("Create: nothing of it is sold in an NPC shop or comes out of a gacha (shopBan + the new gachaBan), an installed version-3 file gains both, and a version-4 file is left as the operator wrote it",
                    crDef.shopBan.contains("create:*") && crDef.gachaBan.contains("create:*")
                            && crOld3.shopBan.contains("create:*") && crOld3.shopBan.contains("efn:*") && crOld3.gachaBan.contains("create:*") && crOld3.gachaBan.contains("mymod:thing")
                            && !crKeep4.shopBan.contains("create:*") && crKeep4.gachaBan.isEmpty()
                            && com.rtshop.rules.ItemPolicy.gachaBanned(new ResourceLocation("create:goggles")) && com.rtshop.rules.ItemPolicy.gachaBanned(new ResourceLocation("create:capacity"))
                            && !com.rtshop.rules.ItemPolicy.gachaBanned(new ResourceLocation("minecraft:diamond")) && com.rtshop.gacha.GachaConfig.valid(crProbe)
                            && crLeft.isEmpty(),
                    crLeft + " " + crOld3.shopBan + " " + crOld3.gachaBan + " " + crKeep4.shopBan);
            // 3.59: 상점 설정을 기본값으로 / 내 설정으로 (/rtshop shop defaults · mine)
            java.nio.file.Path sdFile = com.rtshop.shop.ShopLoader.dir().resolve("farmer.json");
            java.nio.file.Path sdMineFile = com.rtshop.shop.ShopDefaults.mineDir().resolve("farmer.json");
            String sdOrig = Files.exists(sdFile) ? Files.readString(sdFile, java.nio.charset.StandardCharsets.UTF_8) : null;
            String sdTrace = "";
            boolean sdOk = false;
            try {
                if (sdOrig != null) {
                    com.google.gson.JsonObject sdEdit = JsonParser.parseString(sdOrig).getAsJsonObject();
                    sdEdit.addProperty("_selftest_note", "mine");
                    String sdMine = sdEdit.toString();
                    Files.writeString(sdFile, sdMine, java.nio.charset.StandardCharsets.UTF_8);
                    boolean sdEdited = !com.rtshop.shop.ShopDefaults.isDefault("farmer");
                    com.rtshop.shop.ShopDefaults.Outcome sd1 = com.rtshop.shop.ShopDefaults.toDefaults("farmer");
                    boolean sdNowDef = com.rtshop.shop.ShopDefaults.isDefault("farmer") && com.rtshop.shop.ShopDefaults.hasMine("farmer")
                            && com.rtshop.shop.ShopDefaults.same(Files.readString(sdMineFile, java.nio.charset.StandardCharsets.UTF_8), sdMine);
                    com.rtshop.shop.ShopDefaults.Outcome sd2 = com.rtshop.shop.ShopDefaults.toDefaults("farmer");
                    com.rtshop.shop.ShopDefaults.Outcome sd3 = com.rtshop.shop.ShopDefaults.toMine("farmer");
                    boolean sdBack = com.rtshop.shop.ShopDefaults.same(Files.readString(sdFile, java.nio.charset.StandardCharsets.UTF_8), sdMine)
                            && com.rtshop.shop.ShopDefaults.hasMine("farmer");
                    com.rtshop.shop.ShopDefaults.Outcome sd4 = com.rtshop.shop.ShopDefaults.toMine("farmer");
                    com.rtshop.shop.ShopDefaults.Outcome sd5 = com.rtshop.shop.ShopDefaults.toDefaults("no_such_shop");
                    com.rtshop.shop.ShopDefaults.Outcome sd6 = com.rtshop.shop.ShopDefaults.toMine("no_such_shop");
                    String sdGeneral = com.rtshop.shop.ShopDefaults.defaultJson("general");
                    sdTrace = sdEdited + " " + sd1 + " " + sdNowDef + " " + sd2 + " " + sd3 + " " + sdBack + " " + sd4 + " " + sd5 + " " + sd6;
                    sdOk = sdEdited && sd1 == com.rtshop.shop.ShopDefaults.Outcome.RESET && sdNowDef && sd2 == com.rtshop.shop.ShopDefaults.Outcome.ALREADY
                            && sd3 == com.rtshop.shop.ShopDefaults.Outcome.RESTORED && sdBack && sd4 == com.rtshop.shop.ShopDefaults.Outcome.SAME
                            && sd5 == com.rtshop.shop.ShopDefaults.Outcome.NO_DEFAULT && sd6 == com.rtshop.shop.ShopDefaults.Outcome.NO_SAVED
                            && sdGeneral != null && sdGeneral.contains("rtshop:return_ticket") && com.rtshop.shop.ShopDefaults.known().contains("gacha_shop");
                }
            } catch (Exception sdEx) {
                sdTrace = sdTrace + " " + sdEx;
            } finally {
                try {
                    if (sdOrig != null) Files.writeString(sdFile, sdOrig, java.nio.charset.StandardCharsets.UTF_8);
                    Files.deleteIfExists(sdMineFile);
                } catch (Exception sdIgnored) {
                }
            }
            check("/rtshop shop defaults puts a shop back on the bundled defaults and keeps the operator's file; /rtshop shop mine brings that file back, as often as wanted",
                    sdOk, sdTrace);
        }
        check("by default the NPC shops never list the enhance anvil or a boss item (nether star); ordinary goods are untouched, and nothing vanilla loses its recipe",
                com.rtshop.rules.ItemPolicy.shopBanned(new ItemStack(com.rtshop.item.ModItems.ENHANCE_ANVIL.get())) && com.rtshop.rules.ItemPolicy.bossItem(Items.NETHER_STAR)
                        && com.rtshop.rules.ItemPolicy.shopBanned(new ItemStack(Items.NETHER_STAR)) && !com.rtshop.rules.ItemPolicy.shopBanned(new ItemStack(Items.IRON_SWORD))
                        && !com.rtshop.rules.ItemPolicy.shopBanned(new ItemStack(Items.GUNPOWDER))
                        && com.rtshop.rules.ItemPolicy.recipeReason(new ResourceLocation("minecraft:iron_sword"), new ResourceLocation("minecraft:iron_sword")) == null
                        && !com.rtshop.rules.ItemPolicy.lootBanned(new ItemStack(Items.IRON_SWORD), false) && com.rtshop.rules.ItemPolicy.priceFactor(new ItemStack(Items.IRON_SWORD)) == 1,
                com.rtshop.rules.ItemPolicy.bossCountForTest() + " boss items, " + com.rtshop.rules.ItemPolicy.listedCountForTest() + " listed gear");
        ResourceLocation tntRecipe = new ResourceLocation("minecraft:tnt");
        ShopLoader.Price swordBefore = ShopServer.loaded().prices.get("minecraft:diamond_sword");
        int swordPrice = swordBefore == null ? -1 : swordBefore.price;
        com.rtshop.rules.ItemPolicy.Config ip = com.rtshop.rules.ItemPolicy.Config.defaults();
        ip.shopBan.add("minecraft:arrow");
        ip.priceFactor.put("minecraft:diamond_sword", 3.0);
        ip.lootBan.add("minecraft:saddle");
        ip.lootBan.add("minecraft:golden_apple");
        ip.lootAllow.add("minecraft:golden_apple");
        ip.recipeBan.add("minecraft:tnt");
        ip.vanillaGearToo = true;
        ip.keep.add("minecraft:iron_pickaxe");
        ip.mobDrops.clear();
        ip.mobDrops.put("minecraft:paper", 0.5);
        com.rtshop.rules.ItemPolicy.setForTest(ip);
        try {
            com.rtshop.rules.ItemPolicy.prepare(server);
            ShopLoader.Loaded trial = ShopLoader.loadAll(server);
            com.rtshop.rules.ItemPolicy.afterShops(server, trial);
            ShopData tb = trial.shops.get("blacksmith");
            ShopLoader.Price swordNow = trial.prices.get("minecraft:diamond_sword");
            check("a shop ban removes the item from every NPC shop (arrows), and a price factor multiplies the shop price (diamond sword x3)",
                    tb != null && findEntry(tb, e -> e.stack.is(Items.ARROW)) < 0 && findEntry(tb, e -> e.stack.is(Items.SPECTRAL_ARROW)) >= 0
                            && swordNow != null && swordPrice > 0 && Math.abs(swordNow.price - swordPrice * 3.0) <= swordPrice * 0.1,
                    swordPrice + " -> " + (swordNow == null ? "missing" : swordNow.price) + " skipped " + com.rtshop.rules.ItemPolicy.skippedForTest());
            net.minecraft.world.item.crafting.RecipeManager prm = server.getRecipeManager();
            check("gear listed in a shop cannot be crafted (iron sword, with the rule widened to vanilla), a kept item still can (iron pickaxe), ordinary things too (bread), a banned recipe is gone (TNT)",
                    prm.byKey(new ResourceLocation("minecraft:iron_sword")).isEmpty() && prm.byKey(new ResourceLocation("minecraft:iron_pickaxe")).isPresent()
                            && prm.byKey(new ResourceLocation("minecraft:bread")).isPresent() && prm.byKey(tntRecipe).isEmpty()
                            && com.rtshop.rules.ItemPolicy.recipesBlockedForTest("listed") > 5,
                    "listed " + com.rtshop.rules.ItemPolicy.recipesBlockedForTest("listed") + " gear " + com.rtshop.rules.ItemPolicy.recipesBlockedForTest("gear")
                            + " ban " + com.rtshop.rules.ItemPolicy.recipesBlockedForTest("ban"));
            check("loot: a banned item never comes out (saddle), an allowed one does (golden apple), gear comes only from bosses (iron sword), kept items and plain goods are left alone",
                    com.rtshop.rules.ItemPolicy.lootBanned(new ItemStack(Items.SADDLE), false) && com.rtshop.rules.ItemPolicy.lootBanned(new ItemStack(Items.SADDLE), true)
                            && !com.rtshop.rules.ItemPolicy.lootBanned(new ItemStack(Items.GOLDEN_APPLE), false)
                            && com.rtshop.rules.ItemPolicy.lootBanned(new ItemStack(Items.IRON_SWORD), false) && !com.rtshop.rules.ItemPolicy.lootBanned(new ItemStack(Items.IRON_SWORD), true)
                            && !com.rtshop.rules.ItemPolicy.lootBanned(new ItemStack(Items.IRON_PICKAXE), false) && !com.rtshop.rules.ItemPolicy.lootBanned(new ItemStack(Items.IRON_INGOT), false)
                            && !com.rtshop.rules.ItemPolicy.lootBanned(new ItemStack(Items.NETHER_STAR), false));
            java.util.List<net.minecraft.world.entity.item.ItemEntity> extra = new java.util.ArrayList<>();
            com.rtshop.rules.ItemPolicy.testRoll = 0.4;
            com.rtshop.rules.ItemPolicy.mobDrops(zombieProbe, false, a, extra);
            int byPlayer = extra.size();
            com.rtshop.rules.ItemPolicy.mobDrops(zombieProbe, false, null, extra);
            com.rtshop.rules.ItemPolicy.testRoll = 0.6;
            com.rtshop.rules.ItemPolicy.mobDrops(zombieProbe, false, a, extra);
            int missed = extra.size();
            com.rtshop.rules.ItemPolicy.mobDrops(witherProbe, true, a, extra);
            check("extra monster drops (ink): rolled only when a player made the kill, and bosses roll with a higher chance",
                    byPlayer == 1 && missed == 1 && extra.size() == 2 && extra.get(0).getItem().is(Items.PAPER), byPlayer + " " + missed + " " + extra.size());
        } finally {
            com.rtshop.rules.ItemPolicy.testRoll = -1;
            com.rtshop.rules.ItemPolicy.reload();
            com.rtshop.rules.ItemPolicy.prepare(server);
            com.rtshop.rules.ItemPolicy.afterShops(server, ShopServer.loaded());
        }
        check("after the trial the real rules are back: the iron sword and TNT can be crafted again",
                server.getRecipeManager().byKey(new ResourceLocation("minecraft:iron_sword")).isPresent() && server.getRecipeManager().byKey(tntRecipe).isPresent());
        {
            // 3.66.0: 옛 판(4판) item_policy.json 을 읽으면 Better Nether 금지가 더해진다 (운영자가 적은 다른 줄은 그대로)
            com.google.gson.JsonObject old4 = new com.google.gson.JsonObject();
            old4.addProperty("version", 4);
            com.google.gson.JsonArray lb = new com.google.gson.JsonArray();
            lb.add("minecraft:saddle");
            old4.add("lootBan", lb);
            com.rtshop.rules.ItemPolicy.Config migrated = com.rtshop.rules.ItemPolicy.Config.fromJson(old4);
            com.rtshop.rules.ItemPolicy.Config fresh = com.rtshop.rules.ItemPolicy.Config.defaults();
            check("3.66.0 item rules: Better Nether's recipes, loot and block drops are banned by default and added to an older rules file (keeping its own lines)",
                    migrated.lootBan.contains("betternether:*") && migrated.lootBan.contains("minecraft:saddle") && migrated.recipeBan.contains("betternether:*")
                            && migrated.recipeBanMods.contains("betternether") && migrated.blockDropBan.contains("betternether:*")
                            && fresh.recipeBanMods.contains("betternether") && fresh.lootBan.contains("betternether:*") && fresh.blockDropBan.contains("betternether:*"),
                    migrated.lootBan + " " + migrated.recipeBanMods + " " + migrated.blockDropBan);
        }
        if (net.minecraftforge.fml.ModList.get().isLoaded("wom") || net.minecraftforge.fml.ModList.get().isLoaded("efn") || net.minecraftforge.fml.ModList.get().isLoaded("tacz")) {
            int banned = 0, womListed = 0, gunsListed = 0, bossListed = 0, tacz = 0, craftableListed = 0;
            String sample = "";
            for (ShopData sd : ShopServer.shops().values()) {
                for (ShopData.Entry en : sd.entries) {
                    net.minecraft.resources.ResourceLocation rl = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(en.stack.getItem());
                    if (rl == null) continue;
                    if (rl.getNamespace().equals("efn") || rl.getNamespace().equals("arcanevortex") || rl.getNamespace().equals("betternether")) { banned++; sample = rl.toString(); }
                    if (rl.getNamespace().equals("wom")) womListed++;
                    if (com.rtshop.rules.ItemPolicy.gun(en.stack.getItem())) { gunsListed++; sample = rl.toString(); }
                    if (com.rtshop.rules.ItemPolicy.bossItem(en.stack.getItem())) { bossListed++; sample = rl.toString(); }
                }
            }
            for (net.minecraft.world.item.crafting.Recipe<?> rec : server.getRecipeManager().getRecipes()) {
                ResourceLocation out = com.rtshop.rules.RecipeRules.outputId(rec, server);
                if (rec.getId().getNamespace().equals("tacz") || out != null && out.getNamespace().equals("tacz")) tacz++;
                if (out != null && !out.getNamespace().equals("minecraft") && !out.getNamespace().equals("rtshop")
                        && com.rtshop.rules.ItemPolicy.recipeReason(rec.getId(), out) != null) { craftableListed++; sample = out.toString(); }
            }
            if (net.minecraftforge.fml.ModList.get().isLoaded("betternether")) {
                // 3.66.0: Better Nether 의 아이템은 만들지도, 상자 · 몹 · 보스 · 블록에서 얻지도 못한다 (상점은 전부터 금지)
                int bnRecipes = 0;
                String bnSample = "";
                for (net.minecraft.world.item.crafting.Recipe<?> rec : server.getRecipeManager().getRecipes()) {
                    ResourceLocation out = com.rtshop.rules.RecipeRules.outputId(rec, server);
                    if (rec.getId().getNamespace().equals("betternether") || out != null && out.getNamespace().equals("betternether")) {
                        bnRecipes++;
                        bnSample = rec.getId().toString();
                    }
                }
                net.minecraft.world.item.Item ruby = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation("betternether", "nether_ruby"));
                net.minecraft.world.item.Item rubySword = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation("betternether", "flaming_ruby_sword"));
                boolean bnLoot = true;
                for (net.minecraft.world.item.Item it : new net.minecraft.world.item.Item[]{ruby, rubySword}) {
                    if (it == null || it == Items.AIR) continue;
                    ItemStack st = new ItemStack(it);
                    bnLoot &= com.rtshop.rules.ItemPolicy.lootBanned(st, false) && com.rtshop.rules.ItemPolicy.lootBanned(st, true)
                            && com.rtshop.rules.ItemPolicy.blockDropBanned(st);
                }
                check("3.66.0 mod pack: no Better Nether item can be crafted, and none comes from chests, mobs, bosses or broken blocks",
                        bnRecipes == 0 && bnLoot, bnRecipes + " recipes " + bnSample + " loot " + bnLoot);
                betterNetherDrops(level, a);
            }
            if (net.minecraftforge.fml.ModList.get().isLoaded("distanthorizons") && net.minecraftforge.fml.ModList.get().isLoaded("chunky")) {
                // 3.70.0: DH 3.3.3 은 Chunky 가 등록되기 전(세계를 여는 동안)에 연결하려다 튕겼다 → RT Shop 이 Chunky 가 켜질 때까지 미룬다
                boolean hooked = false;
                String names = "";
                try {
                    Class<?> acc = Class.forName("com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.AbstractChunkyAccessor", false, SelfTest.class.getClassLoader());
                    for (java.lang.reflect.Method dm : acc.getDeclaredMethods()) {
                        if (dm.getName().contains("waitForChunky")) {
                            hooked = true;
                            names = dm.getName();
                        }
                    }
                } catch (Throwable t) {
                    names = t.toString();
                }
                check("3.70.0 Distant Horizons waits until Chunky is loaded before linking to it (no crash on the first chunk without dhcompat)",
                        hooked && com.rtshop.compat.ChunkyReady.now() && !"none".equals(com.rtshop.compat.ChunkyReady.probeForTest()),
                        names + " ready " + com.rtshop.compat.ChunkyReady.now() + " probe " + com.rtshop.compat.ChunkyReady.probeForTest()
                                + " deferred " + com.rtshop.compat.ChunkyReady.wasDeferred());
            }
            check("mod pack: no Nightfall or Arcane Vortex item, no gun and no boss item is listed in an NPC shop; no gun recipe and no recipe of shop-only gear is left",
                    banned == 0 && gunsListed == 0 && bossListed == 0 && tacz == 0 && craftableListed == 0,
                    "banned " + banned + " guns " + gunsListed + " boss " + bossListed + " tacz recipes " + tacz + " craftable " + craftableListed + " " + sample);
            if (net.minecraftforge.fml.ModList.get().isLoaded("irons_spellbooks")) {
                // 보스가 떨어뜨리기는 해도 흔한 재료(비전 정수·두루마리)와 그것으로 만드는 마법사 장비는 보스 아이템이 아니다
                int irons = 0;
                for (net.minecraft.world.item.Item it : net.minecraftforge.registries.ForgeRegistries.ITEMS) {
                    net.minecraft.resources.ResourceLocation rl = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(it);
                    if (rl != null && rl.getNamespace().equals("irons_spellbooks") && com.rtshop.rules.ItemPolicy.bossItem(it)) irons++;
                }
                net.minecraft.world.item.Item essence = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("irons_spellbooks", "arcane_essence"));
                net.minecraft.world.item.Item robe = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("irons_spellbooks", "wizard_chestplate"));
                check("mod pack: common materials a boss also drops (arcane essence) and the gear made from them are not boss items",
                        essence != null && !com.rtshop.rules.ItemPolicy.bossItem(essence) && robe != null && !com.rtshop.rules.ItemPolicy.bossItem(robe) && irons <= 30,
                        irons + " irons_spellbooks boss items");
                net.minecraft.world.item.Item wander = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("irons_spellbooks", "wandering_magician_chestplate"));
                net.minecraft.world.item.Item graybeard = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("irons_spellbooks", "graybeard_staff"));
                check("3.64.2 the Wandering Magician armour is armour, not a staff (its class name has 'wand' in 'wandering'), while a real staff still is",
                        wander != null && wander != net.minecraft.world.item.Items.AIR && !com.rtshop.rules.ItemPolicy.staff(wander)
                                && (graybeard == null || graybeard == net.minecraft.world.item.Items.AIR || com.rtshop.rules.ItemPolicy.staff(graybeard)),
                        wander + " " + graybeard);
                if (net.minecraftforge.fml.ModList.get().isLoaded("spelunkery")) {
                    double variant = sellable("spelunkery:andesite_iron_ore");
                    double vanilla = sellable("minecraft:iron_ore");
                    check("3.65.0 stone-variant ores (Spelunkery's andesite iron ore) can be sold like the plain ore, for the same price",
                            variant > 0 && Math.abs(variant - vanilla) < 1.0e-6, variant + " vs " + vanilla);
                }
                if (net.minecraftforge.fml.ModList.get().isLoaded("wizards")) {
                    int novice = buyable(server, "wizards:wand_novice");
                    int neth = buyable(server, "wizards:wand_netherite_arcane");
                    int nethStaff = buyable(server, "wizards:staff_netherite_arcane");
                    check("3.65.0 wands are priced by what they are made of (novice wand cheap, netherite wand near the netherite staff), not all 6,000 G",
                            novice > 0 && neth > 0 && novice < neth && neth >= 20000 && (nethStaff <= 0 || neth >= nethStaff / 3),
                            "novice " + novice + " netherite " + neth + " netherite staff " + nethStaff);
                }
            }
            info("item policy: " + com.rtshop.rules.ItemPolicy.bossCountForTest() + " boss items, " + com.rtshop.rules.ItemPolicy.listedCountForTest() + " listed gear, wom listed " + womListed
                    + ", recipes blocked: mod " + com.rtshop.rules.ItemPolicy.recipesBlockedForTest("mod") + " ban " + com.rtshop.rules.ItemPolicy.recipesBlockedForTest("ban")
                    + " staff " + com.rtshop.rules.ItemPolicy.recipesBlockedForTest("staff") + " listed " + com.rtshop.rules.ItemPolicy.recipesBlockedForTest("listed")
                    + " gear " + com.rtshop.rules.ItemPolicy.recipesBlockedForTest("gear"));
        }
        // 마을의 기준 자리와 귀환권 (3.52)
        com.rtshop.rules.TownSpawn.Config ts = com.rtshop.rules.TownSpawn.fresh();
        com.rtshop.rules.TownSpawn.setForTest(ts);
        try {
            boolean noSpot = com.rtshop.item.ReturnTicketItem.refusal(a, true) != null;
            ts.set = true;
            ts.dim = a.level().dimension().location().toString();
            ts.x = a.getX() + 2900;
            ts.y = a.getY();
            ts.z = a.getZ();
            boolean near = com.rtshop.rules.TownSpawn.inNormalRange(a) && com.rtshop.item.ReturnTicketItem.refusal(a, false) == null;
            ts.x = a.getX() + 3001;
            boolean far = !com.rtshop.rules.TownSpawn.inNormalRange(a) && com.rtshop.item.ReturnTicketItem.refusal(a, false) != null
                    && com.rtshop.item.ReturnTicketItem.refusal(a, true) == null;
            ts.x = a.getX();
            ts.dim = "minecraft:the_nether";
            boolean otherWorld = !com.rtshop.rules.TownSpawn.inNormalRange(a) && com.rtshop.item.ReturnTicketItem.refusal(a, true) == null;
            check("return tickets: nothing works until the town point is set; the ordinary one works within 3000 blocks (height ignored) in the town's world, the greater one anywhere",
                    noSpot && near && far && otherWorld && ts.normalRange == 3000, noSpot + " " + near + " " + far + " " + otherWorld);
            // 3.53: 다시 쓰기까지의 시간 (실제 시간, 일반 60분 / 고급 30분, 종류마다 따로)
            ts.dim = a.level().dimension().location().toString();
            ts.x = a.getX();
            com.rtshop.rules.TownSpawn.clearCooldown(a);
            long t0 = 1_800_000_000_000L;
            com.rtshop.rules.TownSpawn.testNow = t0;
            boolean cdFree = com.rtshop.item.ReturnTicketItem.refusal(a, false) == null && com.rtshop.item.ReturnTicketItem.refusal(a, true) == null;
            com.rtshop.rules.TownSpawn.used(a, false);
            boolean cdNormal = com.rtshop.item.ReturnTicketItem.refusal(a, false) != null && com.rtshop.item.ReturnTicketItem.refusal(a, true) == null
                    && com.rtshop.rules.TownSpawn.cooldownLeft(a, false) == 60 * 60000L && com.rtshop.rules.TownSpawn.cooldownLeft(a, true) == 0;
            com.rtshop.rules.TownSpawn.used(a, true);
            com.rtshop.rules.TownSpawn.testNow = t0 + 29 * 60000L;
            boolean cdBoth = com.rtshop.item.ReturnTicketItem.refusal(a, false) != null && com.rtshop.item.ReturnTicketItem.refusal(a, true) != null
                    && com.rtshop.rules.TownSpawn.cooldownLeft(a, true) == 60000L;
            com.rtshop.rules.TownSpawn.testNow = t0 + 30 * 60000L;
            boolean cdPlusDone = com.rtshop.item.ReturnTicketItem.refusal(a, false) != null && com.rtshop.item.ReturnTicketItem.refusal(a, true) == null;
            com.rtshop.rules.TownSpawn.testNow = t0 + 59 * 60000L + 59000L;
            boolean cdAlmost = com.rtshop.item.ReturnTicketItem.refusal(a, false) != null;
            com.rtshop.rules.TownSpawn.testNow = t0 + 60 * 60000L;
            boolean cdDone = com.rtshop.item.ReturnTicketItem.refusal(a, false) == null;
            // 죽어도(되살아나도) 남는 자리에 적힌다
            com.rtshop.rules.TownSpawn.used(a, false);
            boolean cdKept = a.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG).getCompound("rtshop_return_cd").getLong("normal") == t0 + 60 * 60000L;
            com.rtshop.rules.TownSpawn.clearCooldown(a);
            boolean cdCleared = com.rtshop.item.ReturnTicketItem.refusal(a, false) == null;
            check("return ticket cooldowns run on real time and per kind: the ordinary one 60 minutes, the greater one 30 minutes, kept through death and relog",
                    cdFree && cdNormal && cdBoth && cdPlusDone && cdAlmost && cdDone && cdKept && cdCleared && ts.normalCooldownMinutes == 60 && ts.premiumCooldownMinutes == 30,
                    cdFree + " " + cdNormal + " " + cdBoth + " " + cdPlusDone + " " + cdAlmost + " " + cdDone + " " + cdKept + " " + cdCleared);
            com.rtshop.rules.TownSpawn.testNow = 0;
            ItemStack plus = new ItemStack(com.rtshop.item.ModItems.RETURN_TICKET_PLUS.get()), plain = new ItemStack(com.rtshop.item.ModItems.RETURN_TICKET.get());
            com.rtshop.rules.TownSpawn.clearBought(a);
            com.rtshop.rules.TownSpawn.testDay = "2026-01-01";
            int d0 = com.rtshop.rules.TownSpawn.dailyLeft(a, plus);
            com.rtshop.rules.TownSpawn.bought(a, plus, 3);
            int d1 = com.rtshop.rules.TownSpawn.dailyLeft(a, plus);
            com.rtshop.rules.TownSpawn.bought(a, plus, 2);
            int d2 = com.rtshop.rules.TownSpawn.dailyLeft(a, plus);
            com.rtshop.rules.TownSpawn.testDay = "2026-01-02";
            int d3 = com.rtshop.rules.TownSpawn.dailyLeft(a, plus);
            check("the greater return ticket can be bought 5 times a day (the count starts again the next day); the ordinary one has no limit",
                    d0 == 5 && d1 == 2 && d2 == 0 && d3 == 5 && com.rtshop.rules.TownSpawn.dailyLeft(a, plain) > 1000, d0 + " " + d1 + " " + d2 + " " + d3);
            ShopData gen = ShopServer.shops().get("general");
            int ti = gen == null ? -1 : findEntry(gen, e -> e.stack.is(com.rtshop.item.ModItems.RETURN_TICKET.get()) && e.buyable);
            int tj = gen == null ? -1 : findEntry(gen, e -> e.stack.is(com.rtshop.item.ModItems.RETURN_TICKET_PLUS.get()) && e.buyable);
            check("the general store sells both return tickets: 10000 G and 30000 G (3.55; an installed shop still at an old default price is updated)",
                    ti >= 0 && tj >= 0 && gen.entries.get(ti).price == 10000 && gen.entries.get(tj).price == 30000,
                    ti < 0 || tj < 0 ? "missing" : gen.entries.get(ti).price + " " + gen.entries.get(tj).price);
            // 옛 값으로 깔린 상점 파일의 값 고치기: 기본값 그대로인 것만 고치고, 운영자가 바꾼 값은 그대로 둔다
            try {
                java.nio.file.Path priceFile = java.nio.file.Files.createTempFile("rtshop_reprice", ".json");
                java.nio.file.Files.writeString(priceFile, "{\"items\":[{\"item\":\"rtshop:return_ticket\",\"price\":1500},{\"item\":\"rtshop:return_ticket_plus\",\"price\":9999}]}");
                int n1 = com.rtshop.shop.ShopLoader.reprice(priceFile, "rtshop:return_ticket", 1500, 5000);
                int n2 = com.rtshop.shop.ShopLoader.reprice(priceFile, "rtshop:return_ticket_plus", 12000, 30000);
                String after = java.nio.file.Files.readString(priceFile);
                java.nio.file.Files.deleteIfExists(priceFile);
                check("repricing touches only entries still at the old default price", n1 == 1 && n2 == 0 && after.contains("5000") && after.contains("9999"), n1 + " " + n2 + " " + after);
                // 3.55: 1,500 → 5,000 → 10,000 으로 차례로 오르고, 운영자가 정한 값(7,777)은 그대로
                java.nio.file.Path rpFile55 = java.nio.file.Files.createTempFile("rtshop_reprice2", ".json");
                java.nio.file.Files.writeString(rpFile55, "{\"items\":[{\"item\":\"rtshop:return_ticket\",\"price\":1500},{\"item\":\"rtshop:return_ticket\",\"price\":7777}]}");
                int rpN55a = com.rtshop.shop.ShopLoader.reprice(rpFile55, "rtshop:return_ticket", 1500, 5000);
                int rpN55b = com.rtshop.shop.ShopLoader.reprice(rpFile55, "rtshop:return_ticket", 5000, 10000);
                String rpAfter55 = java.nio.file.Files.readString(rpFile55);
                java.nio.file.Files.deleteIfExists(rpFile55);
                boolean rpListed55 = java.util.Arrays.asList(com.rtshop.shop.ShopLoader.REPRICED).contains("general:rtshop:return_ticket=5000>10000@2");
                check("the plain return ticket's default price rises to 10000 G on installed shops too (old defaults only)",
                        rpN55a == 1 && rpN55b == 1 && rpAfter55.contains("10000") && rpAfter55.contains("7777") && !rpAfter55.contains("1500") && rpListed55, rpN55a + " " + rpN55b + " " + rpAfter55);
            } catch (Exception ex) {
                check("repricing touches only entries still at the old default price", false, ex.toString());
            }
        } finally {
            com.rtshop.rules.TownSpawn.testDay = null;
            com.rtshop.rules.TownSpawn.testNow = 0;
            com.rtshop.rules.TownSpawn.clearBought(a);
            com.rtshop.rules.TownSpawn.clearCooldown(a);
            com.rtshop.rules.TownSpawn.reload();
        }
        witherProbe.discard();
        zombieProbe.discard();
        com.rtshop.rules.MobRules.reload();
        // 운영자 기능은 크리에이티브(관전)일 때만: 서바이벌의 OP 는 보통 플레이어와 같다
        com.rtshop.util.Perm.enforceForTest = true;
        try {
            boolean plain = !com.rtshop.util.Perm.op(a) && !com.rtshop.util.Perm.lockedOp(a);
            check("operator features need creative mode: a non-operator has none either way (and is not 'a locked operator')", plain || a.hasPermissions(2), a.hasPermissions(2));
        } finally {
            com.rtshop.util.Perm.enforceForTest = false;
        }
        // 마법은 마법사만: 다른 전투 직업(과 직업이 없는 사람)은 주문을 쓸 수 없다
        String combatWas = com.rtshop.job.Jobs.entry(a).combat;
        com.rtshop.job.MagicLock.testFake = true;
        try {
            com.rtshop.job.Jobs.force(a, "combat", "warrior");
            boolean warriorNo = !com.rtshop.job.MagicLock.canCast(a);
            com.rtshop.job.Jobs.force(a, "combat", "mage");
            boolean mageYes = com.rtshop.job.MagicLock.canCast(a);
            com.rtshop.job.Jobs.force(a, "combat", "");
            boolean noneNo = !com.rtshop.job.MagicLock.canCast(a);
            com.rtshop.rules.WorldRules.get().magicOnlyForMage = false;
            boolean offYes = com.rtshop.job.MagicLock.canCast(a);
            com.rtshop.rules.WorldRules.get().magicOnlyForMage = true;
            check("only a mage can cast magic (a warrior and a jobless player cannot; with the rule off anyone can); Iron's Spells is hooked when installed",
                    warriorNo && mageYes && noneNo && offYes
                            && com.rtshop.job.MagicLock.hookedForTest() == net.minecraftforge.fml.ModList.get().isLoaded("irons_spellbooks"),
                    warriorNo + "/" + mageYes + "/" + noneNo + "/" + offYes + " hooked=" + com.rtshop.job.MagicLock.hookedForTest());
        } finally {
            com.rtshop.job.MagicLock.testFake = false;
            com.rtshop.job.Jobs.force(a, "combat", combatWas == null ? "" : combatWas);
        }
        check("the skill-stack keeper can reach Epic Fight's innate-skill slot whenever Epic Fight is installed (also on a dedicated server)",
                com.rtshop.compat.EfStacks.ready() == net.minecraftforge.fml.ModList.get().isLoaded("epicfight"), com.rtshop.compat.EfStacks.failure);
        // 뽑기의 고급 무기(영웅 등급 이상)는 직업의 무기 분류에 들지 않는다: 보정도 불이익도 없다
        ItemStack plainSword = new ItemStack(Items.DIAMOND_SWORD), prizeSword = new ItemStack(Items.DIAMOND_SWORD), rareSword = new ItemStack(Items.DIAMOND_SWORD);
        net.minecraft.nbt.CompoundTag mk1 = new net.minecraft.nbt.CompoundTag(), mk2 = new net.minecraft.nbt.CompoundTag();
        mk1.putString("g", "epic");
        mk2.putString("g", "rare");
        prizeSword.getOrCreateTag().put(com.rtshop.gacha.Gacha.MARK, mk1);
        rareSword.getOrCreateTag().put(com.rtshop.gacha.Gacha.MARK, mk2);
        com.rtshop.job.JobConfig jcfg = com.rtshop.job.JobConfig.get();
        String combatWas2 = com.rtshop.job.Jobs.entry(a).combat;
        com.rtshop.job.Jobs.force(a, "combat", "tank");
        double[] onPlain = com.rtshop.job.Jobs.weapon(a, jcfg.weaponType(plainSword)), onPrize = com.rtshop.job.Jobs.weapon(a, jcfg.weaponType(prizeSword));
        com.rtshop.job.Jobs.force(a, "combat", combatWas2 == null ? "" : combatWas2);
        check("a premium gacha weapon (epic or above) is outside the jobs' weapon classes: no bonus and no penalty, while a rare one is classed as usual",
                jcfg.weaponType(prizeSword).equals(com.rtshop.job.WeaponTypes.PREMIUM) && jcfg.weaponType(rareSword).equals(jcfg.weaponType(plainSword))
                        && onPrize[0] == 0 && onPrize[1] == 0, jcfg.weaponType(plainSword) + " " + java.util.Arrays.toString(onPlain) + " / " + jcfg.weaponType(prizeSword) + " " + java.util.Arrays.toString(onPrize));
        // 양산 음식은 배고픔이 적게 찬다 (빵 5칸 → 3칸, 쿠키 2칸 → 1칸; 스테이크는 그대로)
        com.rtshop.rules.FoodRules.setForTest(com.rtshop.rules.FoodRules.fresh());
        check("mass-produced food fills less: bread and cookies are on the list (bread 5 -> 3, cookie 2 -> 1, never below 1), steak is not",
                com.rtshop.rules.FoodRules.weak(new ItemStack(Items.BREAD)) && com.rtshop.rules.FoodRules.weak(new ItemStack(Items.COOKIE))
                        && !com.rtshop.rules.FoodRules.weak(new ItemStack(Items.COOKED_BEEF)) && com.rtshop.rules.FoodRules.cut(5) == 2
                        && com.rtshop.rules.FoodRules.cut(2) == 1 && com.rtshop.rules.FoodRules.cut(1) == 0,
                com.rtshop.rules.FoodRules.cut(5) + " / " + com.rtshop.rules.FoodRules.cut(2));
        com.rtshop.rules.FoodRules.reload();
        // 스킬북으로 배운 기술은 죽어도 남는다: Epic Fight 의 keepSkills 규칙을 켠다 (Epic Fight 가 없으면 규칙도 없다)
        boolean efHere = net.minecraftforge.fml.ModList.get().isLoaded("epicfight");
        boolean ruleFound = com.rtshop.rules.KeepSkills.apply(server);
        check("skills learned from skill books are kept after death (Epic Fight's keepSkills game rule is switched on when Epic Fight is installed)",
                ruleFound == efHere && com.rtshop.rules.KeepSkills.on(server) == efHere, efHere + " / " + ruleFound + " / " + com.rtshop.rules.KeepSkills.on(server));
        // 강화 모루는 마을에 하나뿐: 정해 둔 모루만 열린다
        com.rtshop.rules.WorldRules.setAnvil(level, pos.above(30));
        boolean otherRefused = level.getBlockState(pos).getMenuProvider(level, pos).createMenu(7, a.getInventory(), a) == null;
        com.rtshop.rules.WorldRules.setAnvil(level, pos);
        boolean theOneOpens = level.getBlockState(pos).getMenuProvider(level, pos).createMenu(7, a.getInventory(), a) != null;
        com.rtshop.rules.WorldRules.clearAnvil();
        check("only the one enhance anvil of the town opens once it is set (any anvil opens while none is set)",
                otherRefused && theOneOpens && com.rtshop.rules.WorldRules.anvilAllowed(level, pos.above(30)), otherRefused + " / " + theOneOpens);
        a.containerMenu = gm;
        // 전설 이상의 장비는 파괴되지 않는다: 파괴가 나올 자리의 굴림이 유지·손상으로 바뀐다
        gm.setCarried(ItemStack.EMPTY);
        ItemStack legend = g6.copy();
        net.minecraft.nbt.CompoundTag gmark = new net.minecraft.nbt.CompoundTag();
        gmark.putString("g", "legendary");
        legend.getOrCreateTag().put(com.rtshop.gacha.Gacha.MARK, gmark);
        com.rtshop.enhance.EnhanceConfig.Level raw = cfg.level(7), safeLv = com.rtshop.enhance.Enhance.levelFor(cfg, raw, legend);
        giveMaterials(a, cfg, 7, 1);
        gm.getSlot(0).set(legend);
        com.rtshop.enhance.Enhance.testRoll = 0.999;
        gm.clicked(2, 0, net.minecraft.world.inventory.ClickType.PICKUP, a);
        com.rtshop.enhance.Enhance.testRoll = null;
        check("legendary (or higher) gear cannot be destroyed: the destroy chance is split between keep and damage, and the worst roll only damages it",
                com.rtshop.enhance.Enhance.unbreakable(cfg, legend) && !com.rtshop.enhance.Enhance.unbreakable(cfg, g6) && safeLv.destroy == 0
                        && Math.abs(safeLv.maintain - (raw.maintain + raw.destroy / 2)) < 1e-9 && Math.abs(safeLv.damage - (raw.damage + raw.destroy / 2)) < 1e-9
                        && !gm.getCarried().isEmpty() && com.rtshop.enhance.Enhance.level(gm.getCarried()) == (raw.destroy > 0 ? 6 - cfg.damageDrop : com.rtshop.enhance.Enhance.level(gm.getCarried())),
                raw.destroy + " " + safeLv.maintain + "/" + safeLv.damage + " -> " + gm.getCarried());
        gm.setCarried(ItemStack.EMPTY);
        gm.getSlot(0).set(g6.copy());
        com.rtshop.enhance.Enhance.setActiveGuard(a, com.rtshop.enhance.Enhance.G_DAMAGE);
        com.rtshop.enhance.Enhance.testRoll = 0.99;
        gm.clicked(2, 0, net.minecraft.world.inventory.ClickType.PICKUP, a);
        com.rtshop.enhance.Enhance.testRoll = null;
        check("damage guard does not stop destruction (and is used up anyway)", gm.getCarried().isEmpty() && com.rtshop.enhance.Enhance.activeGuard(a) == 0);
        r = enhanceOnce(level, pos, a, sword, 6, 2, 0.99, net.minecraft.world.inventory.ClickType.PICKUP);
        check("destruction guard: destroy becomes damage", com.rtshop.enhance.Enhance.level(r) == 5 && r.is(Items.IRON_SWORD));
        r = enhanceOnce(level, pos, a, sword, 6, 3, 0.99, net.minecraft.world.inventory.ClickType.PICKUP);
        ItemStack r2 = enhanceOnce(level, pos, a, sword, 6, 3, 0.60, net.minecraft.world.inventory.ClickType.PICKUP);
        check("protection ticket: both destroy and damage become keep", com.rtshop.enhance.Enhance.level(r) == 6 && com.rtshop.enhance.Enhance.level(r2) == 6);
        r = enhanceOnce(level, pos, a, sword, 6, 3, 0.0, net.minecraft.world.inventory.ClickType.PICKUP);
        check("success still succeeds with a ticket", com.rtshop.enhance.Enhance.level(r) == 7);
        giveMaterials(a, cfg, 1, 1);
        com.rtshop.enhance.EnhanceMenu gm2 = enhanceMenu(level, pos, a);
        gm2.getSlot(0).set(sword.copy());
        com.rtshop.enhance.Enhance.setActiveGuard(a, com.rtshop.enhance.Enhance.G_DESTROY);
        gm2.clicked(2, 0, net.minecraft.world.inventory.ClickType.PICKUP, a);
        check("a used guard is spent on the next attempt whatever happens, even at +1", com.rtshop.enhance.Enhance.activeGuard(a) == 0);
        gm2.setCarried(ItemStack.EMPTY);
        ItemStack max = sword.copy();
        com.rtshop.enhance.Enhance.setLevel(max, 11);
        gm2.getSlot(0).set(max);
        check("+11 is the top: no further preview", !gm2.getSlot(2).hasItem());
        gm2.getSlot(0).set(ItemStack.EMPTY);
        a.containerMenu = a.inventoryMenu;

        // 능력치
        ItemStack s7 = sword.copy();
        com.rtshop.enhance.Enhance.setLevel(s7, 7);
        double base = attr(sword, net.minecraft.world.entity.EquipmentSlot.MAINHAND, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        double up = attr(s7, net.minecraft.world.entity.EquipmentSlot.MAINHAND, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        double want = com.rtshop.enhance.Enhance.weaponBonus(cfg, 7, base);
        check("a +7 sword hits harder (" + String.format("%.2f", base) + " -> " + String.format("%.2f", up) + ")", Math.abs(up - base - want) < 1e-6 && up > base);
        double offhand = attr(s7, net.minecraft.world.entity.EquipmentSlot.OFFHAND, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        check("...only in the main hand", offhand == 0);
        ItemStack chest = new ItemStack(Items.IRON_CHESTPLATE);
        ItemStack c8 = chest.copy();
        com.rtshop.enhance.Enhance.setLevel(c8, 8);
        double ab = attr(chest, net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
        double au = attr(c8, net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
        double tu = attr(c8, net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS)
                - attr(chest, net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS);
        check("a +8 chestplate has more armor and some toughness (" + ab + " -> " + String.format("%.2f", au) + ", +" + tu + ")",
                au > ab && Math.abs(tu - com.rtshop.enhance.Enhance.toughnessBonus(cfg, 8)) < 1e-6 && tu > 0);
        clear(a);
        // 굴림 분포
        com.rtshop.enhance.EnhanceConfig.Level l5 = cfg.level(5);
        int[] hist = new int[4];
        java.util.Random rnd = new java.util.Random(5);
        int N = 200000;
        for (int i = 0; i < N; i++) hist[com.rtshop.enhance.Enhance.roll(l5, rnd.nextDouble()).ordinal()]++;
        check("rolls follow the configured odds (+5: " + hist[0] * 100.0 / N + "/" + hist[1] * 100.0 / N + "/" + hist[2] * 100.0 / N + "/" + hist[3] * 100.0 / N + ")",
                Math.abs(hist[0] * 100.0 / N - l5.success) < 0.6 && Math.abs(hist[3] * 100.0 / N - l5.destroy) < 0.6);
        level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
    }

    // ------------------------------------------------------------------ 퀘스트

    /** 3.65.0: 터무니없이 센 장비(공격력 25 · 방어력 20 이상, 모드팩에서는 Arcane Vortex)는 전리품 · 드롭 · 조합 · 가방에서 빠진다. */
    private static void extremeGear(ServerLevel level, FakePlayer a) {
        Item ex = null;
        int count = 0;
        StringBuilder mods = new StringBuilder();
        for (Item it : ForgeRegistries.ITEMS) {
            if (com.rtshop.rules.ItemPolicy.extreme(new ItemStack(it))) {
                count++;
                if (ex == null) ex = it;
                String ns = ForgeRegistries.ITEMS.getKey(it).getNamespace();
                if (mods.indexOf(ns) < 0) mods.append(' ').append(ns);
            }
        }
        boolean normalOk = !com.rtshop.rules.ItemPolicy.extreme(new ItemStack(Items.NETHERITE_SWORD))
                && !com.rtshop.rules.ItemPolicy.extreme(new ItemStack(Items.NETHERITE_CHESTPLATE))
                && !com.rtshop.rules.ItemPolicy.extreme(new ItemStack(Items.TRIDENT));
        Item best = ForgeRegistries.ITEMS.getValue(new ResourceLocation("paladins", "ruby_great_hammer"));
        boolean bestOk = best == null || best == Items.AIR || !com.rtshop.rules.ItemPolicy.extreme(new ItemStack(best));
        if (ex == null) {
            check("3.65.0 normal gear is not taken for extreme gear (none in this pack)", normalOk && bestOk);
            return;
        }
        ResourceLocation exId = ForgeRegistries.ITEMS.getKey(ex);
        boolean loot = com.rtshop.rules.ItemPolicy.lootBanned(new ItemStack(ex), true) && com.rtshop.rules.ItemPolicy.lootBanned(new ItemStack(ex), false);
        boolean recipe = com.rtshop.rules.ItemPolicy.recipeBanned(new ResourceLocation("rtshop", "test_extreme"), exId);
        int slot = a.getInventory().getFreeSlot();
        int removed = -1;
        if (slot >= 0) {
            a.getInventory().setItem(slot, new ItemStack(ex));
            removed = com.rtshop.rules.ExtremeGuard.sweep(a);
        }
        check("3.65.0 extreme gear (" + count + " items:" + mods + ") is kept out of chests, mob and boss loot and recipes, and taken out of a survival player's bag; "
                        + "normal top gear (netherite, the strongest pack weapon) is not",
                loot && recipe && removed == 1 && a.getInventory().countItem(ex) == 0 && normalOk && bestOk,
                exId + " loot=" + loot + " recipe=" + recipe + " removed=" + removed);
    }

    /** 3.65.0: 활 · 쇠뇌 · 총은 쏜 것의 피해가, 지팡이 · 완드는 주문 피해가 강화 단계만큼 오른다 (EnhanceRanged). */
    private static void enhanceRanged(ServerLevel level, FakePlayer a) {
        com.rtshop.enhance.Enhance.Kind bowKind = com.rtshop.enhance.Enhance.kind(new ItemStack(Items.BOW));
        com.rtshop.enhance.Enhance.Kind xbowKind = com.rtshop.enhance.Enhance.kind(new ItemStack(Items.CROSSBOW));
        check("3.65.0 bows and crossbows can be enhanced as ranged weapons; swords, tridents and armour keep their kinds",
                bowKind == com.rtshop.enhance.Enhance.Kind.RANGED && xbowKind == com.rtshop.enhance.Enhance.Kind.RANGED
                        && com.rtshop.enhance.Enhance.kind(new ItemStack(Items.IRON_SWORD)) == com.rtshop.enhance.Enhance.Kind.WEAPON
                        && com.rtshop.enhance.Enhance.kind(new ItemStack(Items.TRIDENT)) == com.rtshop.enhance.Enhance.Kind.WEAPON
                        && com.rtshop.enhance.Enhance.kind(new ItemStack(Items.DIAMOND_CHESTPLATE)) == com.rtshop.enhance.Enhance.Kind.ARMOR
                        && com.rtshop.enhance.Enhance.kind(new ItemStack(Items.STICK)) == com.rtshop.enhance.Enhance.Kind.NONE
                        && com.rtshop.enhance.Enhance.enhanceable(new ItemStack(Items.BOW)),
                bowKind + " " + xbowKind);
        ItemStack bow = new ItemStack(Items.BOW);
        com.rtshop.enhance.Enhance.setLevel(bow, 7);
        ItemStack mainBefore = a.getMainHandItem().copy();
        net.minecraft.world.entity.projectile.Arrow arrow = null;
        try {
            a.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, bow);
            arrow = new net.minecraft.world.entity.projectile.Arrow(level, a);
            arrow.setPos(a.getX(), a.getY() + 300, a.getZ());
            arrow.setNoGravity(true);
            level.addFreshEntity(arrow);
            int tagged = arrow.getPersistentData().getInt(com.rtshop.enhance.EnhanceRanged.TAG);
            net.minecraft.world.entity.monster.Zombie target = new net.minecraft.world.entity.monster.Zombie(level);
            net.minecraftforge.event.entity.living.LivingHurtEvent hit =
                    new net.minecraftforge.event.entity.living.LivingHurtEvent(target, level.damageSources().arrow(arrow, a), 10F);
            com.rtshop.enhance.EnhanceRanged.onHurt(hit);
            double want = 10.0 * (1 + com.rtshop.enhance.Enhance.bonusPct(com.rtshop.enhance.EnhanceConfig.get(), com.rtshop.enhance.Enhance.Kind.RANGED, 7) / 100.0);
            // 주문 피해에는 활의 단계를 쓰지 않는다
            net.minecraftforge.event.entity.living.LivingHurtEvent spell =
                    new net.minecraftforge.event.entity.living.LivingHurtEvent(target, level.damageSources().indirectMagic(a, a), 10F);
            com.rtshop.enhance.EnhanceRanged.onHurt(spell);
            // 화살이 나온 뒤에 활을 내려놓아도 그 화살은 쏜 때의 단계
            a.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            net.minecraftforge.event.entity.living.LivingHurtEvent later =
                    new net.minecraftforge.event.entity.living.LivingHurtEvent(target, level.damageSources().arrow(arrow, a), 10F);
            com.rtshop.enhance.EnhanceRanged.onHurt(later);
            check("3.65.0 an arrow from a +7 bow carries the level and hits 28% harder (even after the bow is put away); a spell does not use the bow's level",
                    tagged == 7 && Math.abs(hit.getAmount() - want) < 0.01 && Math.abs(later.getAmount() - want) < 0.01 && Math.abs(spell.getAmount() - 10F) < 0.01,
                    tagged + " " + hit.getAmount() + " " + later.getAmount() + " " + spell.getAmount() + " want " + want);
            // 지팡이 · 완드 (마법 모드가 있을 때): 주문 피해가 오른다
            Item staff = null;
            for (Item it : ForgeRegistries.ITEMS) {
                if (com.rtshop.enhance.Enhance.kind(new ItemStack(it)) == com.rtshop.enhance.Enhance.Kind.MAGIC) {
                    staff = it;
                    break;
                }
            }
            if (staff != null) {
                ItemStack st = new ItemStack(staff);
                com.rtshop.enhance.Enhance.setLevel(st, 11);
                a.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, st);
                net.minecraftforge.event.entity.living.LivingHurtEvent cast =
                        new net.minecraftforge.event.entity.living.LivingHurtEvent(target, level.damageSources().indirectMagic(a, a), 10F);
                com.rtshop.enhance.EnhanceRanged.onHurt(cast);
                double want11 = 10.0 * (1 + com.rtshop.enhance.Enhance.bonusPct(com.rtshop.enhance.EnhanceConfig.get(), com.rtshop.enhance.Enhance.Kind.MAGIC, 11) / 100.0);
                net.minecraftforge.event.entity.living.LivingHurtEvent swing =
                        new net.minecraftforge.event.entity.living.LivingHurtEvent(target, level.damageSources().playerAttack(a), 10F);
                com.rtshop.enhance.EnhanceRanged.onHurt(swing);
                check("3.65.0 a +11 staff or wand makes spells stronger by the +11 bonus (its melee swing is not raised this way)",
                        Math.abs(cast.getAmount() - want11) < 0.01 && Math.abs(swing.getAmount() - 10F) < 0.01,
                        ForgeRegistries.ITEMS.getKey(staff) + " " + cast.getAmount() + " " + swing.getAmount());
            } else {
                info("3.65.0 no staff or wand in this pack: the spell side was not tried");
            }
            if (net.minecraftforge.fml.ModList.get().isLoaded("tacz") || net.minecraftforge.fml.ModList.get().isLoaded("irons_spellbooks")) {
                Item gun = ForgeRegistries.ITEMS.getValue(new ResourceLocation("tacz", "modern_kinetic_gun"));
                Item wander = ForgeRegistries.ITEMS.getValue(new ResourceLocation("irons_spellbooks", "wandering_magician_chestplate"));
                Item graybeard = ForgeRegistries.ITEMS.getValue(new ResourceLocation("irons_spellbooks", "graybeard_staff"));
                Item womStaff = ForgeRegistries.ITEMS.getValue(new ResourceLocation("wom", "iron_staff"));
                Item symmetry = ForgeRegistries.ITEMS.getValue(new ResourceLocation("create", "wand_of_symmetry"));
                check("3.65.0 in the pack: a TaCZ gun is a ranged weapon, Iron's staffs are magic weapons, the Wandering Magician chestplate is armour, "
                                + "a Weapons of Miracles fighting staff stays a melee weapon and Create's wand of symmetry is not a weapon",
                        (gun == null || gun == Items.AIR || com.rtshop.enhance.Enhance.kind(new ItemStack(gun)) == com.rtshop.enhance.Enhance.Kind.RANGED)
                                && (graybeard == null || graybeard == Items.AIR || com.rtshop.enhance.Enhance.kind(new ItemStack(graybeard)) == com.rtshop.enhance.Enhance.Kind.MAGIC)
                                && (wander == null || wander == Items.AIR || com.rtshop.enhance.Enhance.kind(new ItemStack(wander)) == com.rtshop.enhance.Enhance.Kind.ARMOR)
                                && (womStaff == null || womStaff == Items.AIR || com.rtshop.enhance.Enhance.kind(new ItemStack(womStaff)) == com.rtshop.enhance.Enhance.Kind.WEAPON)
                                && (symmetry == null || symmetry == Items.AIR || com.rtshop.enhance.Enhance.kind(new ItemStack(symmetry)) == com.rtshop.enhance.Enhance.Kind.NONE),
                        gun + " " + graybeard + " " + wander + " " + womStaff + " " + symmetry);
                int magic = 0;
                int ranged = 0;
                for (Item it : ForgeRegistries.ITEMS) {
                    com.rtshop.enhance.Enhance.Kind k = com.rtshop.enhance.Enhance.kind(new ItemStack(it));
                    if (k == com.rtshop.enhance.Enhance.Kind.MAGIC) magic++;
                    if (k == com.rtshop.enhance.Enhance.Kind.RANGED) ranged++;
                }
                info("3.65.0 enhanceable as magic weapons: " + magic + ", as ranged weapons: " + ranged);
            }
        } catch (Throwable t) {
            check("3.65.0 ranged and magic enhancement could be tried", false, t.toString());
        } finally {
            if (arrow != null) arrow.discard();
            a.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, mainBefore);
        }
    }

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
        check("merchants with job quests always get a [Quests] button, even when nothing can be taken yet",
                QuestManager.hasQuests("shop_farmer") && QuestManager.viewsFor(a, "shop_farmer").isEmpty() && !QuestManager.hasQuests("bank_nobody"));
        String hint = QuestManager.lockedHint(a, "shop_farmer");
        check("the empty merchant quest window says which quest to finish first", hint.contains("rtshop.quest.locked_hint"), hint);
        List<QuestView> start = QuestManager.viewsFor(a, "quest_guide");
        check("only the first tutorial quest (and the prologue that wraps it) is offered at the start",
                start.stream().anyMatch(v -> v.id().equals("tutorial_1") && v.state() == QuestView.AVAILABLE)
                        && start.stream().allMatch(v -> v.id().equals("tutorial_1") || v.id().equals("story_0_01")),
                start.stream().map(QuestView::id).toList().toString());
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
        // 직업 선행 퀘스트: 전투·생활 직업마다 하나씩만, 포기하면 다른 직업을 고를 수 있다
        QuestManager.reset(a);
        boolean s1 = QuestManager.accept(a, "job_swordsman_1", null);
        boolean w1 = QuestManager.accept(a, "job_warrior_1", null);
        boolean f1 = QuestManager.accept(a, "job_farmer_1", null);
        check("only one combat job line at a time (a second combat line is refused, a life line is fine)", s1 && !w1 && f1, s1 + "/" + w1 + "/" + f1);
        QuestManager.abandon(a, "job_swordsman_1");
        boolean w2 = QuestManager.accept(a, "job_warrior_1", null);
        boolean c2 = QuestManager.accept(a, "job_cook_1", null);
        check("abandoning a job line frees the slot for another job (life slot stays taken by the farmer line)", w2 && !c2, w2 + "/" + c2);
        QuestManager.reset(a);
        Economy.setWallet(a, 0);
        clear(a);
    }

    // ------------------------------------------------------------------ 파티 · 보스 처치 인정

    /** 메인 스토리: 퀘스트가 모두 실제로 있는 몹·구조물·아이템을 가리키는지, 새 목표 종류와 연출이 도는지. */
    /** 마법 뽑기: 분야 선택/무작위 스킬북 뽑기, 야마토는 공격 분야에만, 10회 보장, 티켓, 인챈트 재설정(하나만). */
    /** NPC 자리 배치: 세운 자리가 파일에 적히고, 그 파일을 가진 다른 세계에서는 같은 자리에 저절로 선다. */
    private static void npcLayout(MinecraftServer server, ServerLevel level) throws Exception {
        section("npc layout");
        java.nio.file.Path lf = com.rtshop.quest.NpcLayout.file();
        byte[] lfKeep = java.nio.file.Files.exists(lf) ? java.nio.file.Files.readAllBytes(lf) : null;
        // 엔티티를 찾는 시험은 세계의 시작 지점 곁(늘 엔티티가 도는 청크)에서 한다: 씨앗이 매번 다른 모드팩 전체의 세계에서는
        // 시작 지점이 원점에서 멀 수 있고, 그러면 (40, 60) 의 청크는 블록만 읽히고 엔티티는 getEntities 에 잡히지 않는다 (세웠는데 0명으로 보임).
        BlockPos lsp = level.getSharedSpawnPos();
        int lpx = lsp.getX() + 24, lpz = lsp.getZ() + 8;
        BlockPos lp = new BlockPos(lpx, 150, lpz);
        String lxKey = "\"x\": " + (lpx + 0.5), lxMoved = "\"x\": " + (lpx + 2.5), lxAir = "\"x\": " + (lpx + 12.5);
        net.minecraft.world.phys.AABB lbox = new net.minecraft.world.phys.AABB(lp).inflate(6.0);
        com.rtshop.quest.NpcLayout.forceForTest = true;
        try {
            com.rtshop.quest.NpcLayout.resetForTest(server);
            level.getChunk(lp.getX() >> 4, lp.getZ() >> 4);
            for (int dx = -1; dx <= 4; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    level.setBlockAndUpdate(lp.offset(dx, -1, dz), Blocks.STONE.defaultBlockState());
                    level.setBlockAndUpdate(lp.offset(dx, 0, dz), Blocks.AIR.defaultBlockState());
                    level.setBlockAndUpdate(lp.offset(dx, 1, dz), Blocks.AIR.defaultBlockState());
                }
            }
            // "바닥이 없는 자리" 시험의 자리(12칸 옆)는 지형이 무엇이든 비워 둔다
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -2; dy <= 2; dy++) level.setBlockAndUpdate(lp.offset(12, dy, dz), Blocks.AIR.defaultBlockState());
            }
            for (net.minecraft.world.entity.Entity e : level.getEntities((net.minecraft.world.entity.Entity) null, lbox, x -> com.rtshop.quest.NpcLayout.keyOf(x) != null)) e.discard();
            com.rtshop.quest.NpcLayout.resetForTest(server);
            net.minecraft.world.entity.npc.Villager lv = ShopServer.spawnNpc(level, new net.minecraft.world.phys.Vec3(lpx + 0.5, 150, lpz + 0.5), 90F, "bank");
            int wrote = com.rtshop.quest.NpcLayout.export(server);
            String text = java.nio.file.Files.readString(lf);
            check("an NPC set down in the game is written to npc_layout.json with its key and place",
                    lv != null && wrote == 1 && com.rtshop.quest.NpcLayout.knownForTest(server, "bank") == 1 && text.contains("\"bank\"") && text.contains(lxKey) && text.contains("\"z\": " + (lpz + 0.5)),
                    wrote + " " + text.length() + " " + lxKey);
            // 다른 서버: 같은 파일, NPC 가 하나도 없는 세계
            if (lv != null) lv.discard();
            com.rtshop.quest.NpcLayout.resetForTest(server);
            int want = com.rtshop.quest.NpcLayout.apply(server, false);
            int made = com.rtshop.quest.NpcLayout.settle(server);
            List<net.minecraft.world.entity.Entity> stand = level.getEntities((net.minecraft.world.entity.Entity) null, lbox, x -> "bank".equals(com.rtshop.quest.NpcLayout.keyOf(x)));
            check("a server that gets the file sets the NPC down at the same place by itself, once (the same file is not applied twice)",
                    want == 1 && made == 1 && stand.size() == 1 && Math.abs(stand.get(0).getX() - (lpx + 0.5)) < 0.01 && Math.abs(stand.get(0).getZ() - (lpz + 0.5)) < 0.01
                            && com.rtshop.quest.NpcLayout.apply(server, false) < 0 && com.rtshop.quest.NpcLayout.pendingForTest(server) == 0,
                    want + " " + made + " " + stand.size() + " at " + lp + " entities tick there: " + level.isPositionEntityTicking(lp));
            // 배치를 고친 파일: 두 칸 옆으로
            java.nio.file.Files.writeString(lf, java.nio.file.Files.readString(lf).replace(lxKey, lxMoved).replace("\"id\": \"", "\"id\": \"moved-"));
            int moved = com.rtshop.quest.NpcLayout.apply(server, false);
            int made2 = com.rtshop.quest.NpcLayout.settle(server);
            stand = level.getEntities((net.minecraft.world.entity.Entity) null, lbox, x -> "bank".equals(com.rtshop.quest.NpcLayout.keyOf(x)));
            check("a changed file moves the NPC: the old one is taken away and a new one stands at the new place",
                    moved == 1 && made2 == 1 && stand.size() == 1 && Math.abs(stand.get(0).getX() - (lpx + 2.5)) < 0.01, moved + " " + made2 + " " + stand.size());
            // 바닥이 없는 자리(지형이 다른 세계)에는 세우지 않는다
            java.nio.file.Files.writeString(lf, java.nio.file.Files.readString(lf).replace(lxMoved, lxAir).replace("\"id\": \"", "\"id\": \"air-"));
            int moved2 = com.rtshop.quest.NpcLayout.apply(server, false);
            int made3 = com.rtshop.quest.NpcLayout.settle(server);
            check("where there is no floor (a world with other terrain) the NPC is not set down; the place keeps waiting",
                    moved2 == 1 && made3 == 0 && com.rtshop.quest.NpcLayout.pendingForTest(server) == 1, moved2 + " " + made3 + " " + com.rtshop.quest.NpcLayout.pendingForTest(server));
            // 3.74.0 (사용자 W-18): 바라보는 NPC 하나 지우기 (/rtshop npc remove) · 배치 초기화 (/rtshop npc layout reset)
            for (net.minecraft.world.entity.Entity e : level.getEntities((net.minecraft.world.entity.Entity) null, lbox, x -> com.rtshop.quest.NpcLayout.keyOf(x) != null)) e.discard();
            com.rtshop.quest.NpcLayout.resetForTest(server);
            net.minecraft.world.entity.npc.Villager rb = ShopServer.spawnNpc(level, new net.minecraft.world.phys.Vec3(lpx + 0.5, 150, lpz + 0.5), 90F, "bank");
            net.minecraft.world.entity.npc.Villager rf = ShopServer.spawnNpc(level, new net.minecraft.world.phys.Vec3(lpx + 4.5, 150, lpz + 0.5), 90F, "fisher");
            com.rtshop.quest.NpcLayout.export(server);
            int knownBank = com.rtshop.quest.NpcLayout.knownForTest(server, "bank"), knownFisher = com.rtshop.quest.NpcLayout.knownForTest(server, "fisher");
            net.minecraft.world.phys.Vec3 keep374 = a.position();
            float keepYaw374 = a.getYRot(), keepPitch374 = a.getXRot();
            a.moveTo(lpx + 3.5, 150, lpz + 0.5, 90F, 0F);    // 서쪽(-x)을 본다: 앞의 은행원을 고른다 (등 뒤 1칸의 어부는 아니다)
            net.minecraft.world.entity.Entity pickW = com.rtshop.quest.NpcLayout.picked(a);
            a.moveTo(lpx + 3.5, 150, lpz + 0.5, 180F, 60F);  // 아무도 보지 않으면 4칸 안에서 가장 가까운 NPC (어부)
            net.minecraft.world.entity.Entity pickN = com.rtshop.quest.NpcLayout.picked(a);
            a.moveTo(keep374.x, keep374.y, keep374.z, keepYaw374, keepPitch374);
            int[] rs374 = com.rtshop.quest.NpcLayout.reset(server, e -> lbox.contains(e.position()));
            int left374 = level.getEntities((net.minecraft.world.entity.Entity) null, lbox, x -> com.rtshop.quest.NpcLayout.keyOf(x) != null).size();
            boolean cmd374 = server.getCommands().getDispatcher().findNode(List.of("rtshop", "npc", "remove")) != null
                    && server.getCommands().getDispatcher().findNode(List.of("rtshop", "npc", "layout", "reset")) != null;
            check("3.74 /rtshop npc remove picks the NPC you look at (else the nearest); /rtshop npc layout reset takes the NPCs away and forgets their places",
                    rb != null && rf != null && pickW == rb && pickN == rf && rs374[0] == 2 && left374 == 0 && knownBank == 1 && knownFisher == 1
                            && com.rtshop.quest.NpcLayout.knownForTest(server, "bank") == 0 && com.rtshop.quest.NpcLayout.knownForTest(server, "fisher") == 0 && cmd374,
                    (pickW == null ? "null" : com.rtshop.quest.NpcLayout.keyOf(pickW)) + " / " + (pickN == null ? "null" : com.rtshop.quest.NpcLayout.keyOf(pickN))
                            + " reset " + java.util.Arrays.toString(rs374) + " left " + left374 + " known " + knownBank + "," + knownFisher + " cmd " + cmd374);
        } finally {
            for (net.minecraft.world.entity.Entity e : level.getEntities((net.minecraft.world.entity.Entity) null, lbox, x -> com.rtshop.quest.NpcLayout.keyOf(x) != null)) e.discard();
            com.rtshop.quest.NpcLayout.resetForTest(server);
            com.rtshop.quest.NpcLayout.forceForTest = false;
            if (lfKeep != null) java.nio.file.Files.write(lf, lfKeep);
            else java.nio.file.Files.deleteIfExists(lf);
            for (int dx = -1; dx <= 4; dx++) {
                for (int dz = -1; dz <= 1; dz++) level.setBlockAndUpdate(lp.offset(dx, -1, dz), Blocks.AIR.defaultBlockState());
            }
        }
    }

    /** 장소 조사 · 동행 · 룬 제단 잠금 · 용 소환식 */
    private static void storySystems(MinecraftServer server, ServerLevel level, FakePlayer a) throws Exception {
        QuestManager.reset(a);
        QuestManager.NpcDef n = new QuestManager.NpcDef();
        n.id = "t_comp";
        n.name = "Tess";
        QuestManager.loaded().npcs.put(n.id, n);
        QuestDef q = tempQuest("{\"id\":\"t_inspect\",\"giver\":\"quest_guide\",\"objectives\":["
                + "{\"type\":\"INSPECT\",\"target\":\"t_circle\",\"shape\":\"mural\",\"text\":\"Old circle\",\"lines\":[\"A ring of soot.\"]},"
                + "{\"type\":\"INSPECT\",\"target\":\"t_far@minecraft:end_city\"},"
                + "{\"type\":\"KILL\",\"target\":\"minecraft:zombie\",\"count\":1}],"
                + "\"companion\":{\"npc\":\"t_comp\",\"style\":\"melee\",\"lines\":{\"arrive\":[\"Here.\"],\"idle\":[\"One.\",\"Two.\"],\"fight\":[\"Ha!\",\"Ho!\"],"
                + "\"spot\":{\"t_far\":\"See that.\"},\"done\":[\"Bye.\"]}},\"reward\":{\"gold\":1}}");
        QuestManager.loaded().quests.put(q.id, q);
        QuestDef back = QuestDef.parse(q.toJson());
        check("an inspect goal keeps its trace id, place, shape and lines through saving (and the companion too)",
                back != null && back.objectives.get(0).type == QuestDef.Type.INSPECT && back.objectives.get(0).target.equals("t_circle")
                        && back.objectives.get(0).shape.equals("mural") && back.objectives.get(1).target.equals("t_far")
                        && back.objectives.get(1).at.equals("minecraft:end_city") && back.companion != null
                        && back.companion.get("npc").getAsString().equals("t_comp"),
                back == null ? "null" : back.toJson());
        QuestDef qAgain = tempQuest("{\"id\":\"t_inspect_again\",\"giver\":\"quest_guide\",\"objectives\":[{\"type\":\"INSPECT\",\"target\":\"t_again\"}],\"reward\":{\"gold\":1}}");
        QuestManager.loaded().quests.put(qAgain.id, qAgain);
        boolean againTaken = QuestManager.accept(a, "t_inspect_again", "quest_guide");
        com.rtshop.story.Spots.update(a);
        com.rtshop.story.Spots.placeForTest(a, "t_again", a.blockPosition().offset(40, 0, 0));
        boolean againGone = QuestManager.abandon(a, "t_inspect_again") && com.rtshop.story.Spots.placed(a, "t_again") == null;
        boolean againBack = QuestManager.accept(a, "t_inspect_again", "quest_guide");
        com.rtshop.story.Spots.update(a);
        BlockPos againAt = com.rtshop.story.Spots.placed(a, "t_again");
        check("giving a quest up clears its waiting trace; taking it again puts the trace next to the player anew",
                againTaken && againGone && againBack && againAt != null && againAt.distSqr(a.blockPosition()) <= 20 * 20,
                againTaken + " " + againGone + " " + againBack + " " + againAt);
        QuestManager.abandon(a, "t_inspect_again");
        QuestManager.loaded().quests.remove("t_inspect_again");
        check("inspect quest is accepted", QuestManager.accept(a, "t_inspect", "quest_guide"));
        check("two traces wait to be looked at", QuestManager.pendingInspects(a).size() == 2, QuestManager.pendingInspects(a).size());
        com.rtshop.story.Spots.update(a);
        BlockPos sp = com.rtshop.story.Spots.placed(a, "t_circle");
        check("a trace with no place appears next to the player right away; one tied to a structure waits until the player gets there",
                sp != null && sp.distSqr(a.blockPosition()) <= 20 * 20 && com.rtshop.story.Spots.placed(a, "t_far") == null, sp);
        check("the quest HUD points to a trace that is waiting to be looked at (so a player who walked off can find it again)",
                sp != null && QuestManager.hudWhere(a, "t_inspect").equals(a.level().dimension().location() + ";" + sp.getX() + ";" + sp.getY() + ";" + sp.getZ()),
                QuestManager.hudWhere(a, "t_inspect"));
        com.rtshop.story.Spots.placeForTest(a, "t_circle", a.blockPosition().offset(30, 0, 0));
        check("a trace can not be looked at from far away", !com.rtshop.story.Spots.done(a, "t_circle"));
        check("the quest HUD follows the trace when it is somewhere else",
                QuestManager.hudWhere(a, "t_inspect").equals(a.level().dimension().location() + ";" + (a.blockPosition().getX() + 30) + ";" + a.blockPosition().getY() + ";" + a.blockPosition().getZ()),
                QuestManager.hudWhere(a, "t_inspect"));
        com.rtshop.story.Spots.placeForTest(a, "t_circle", a.blockPosition().offset(2, 0, 0));
        check("looking closer opens the text first (the goal is not filled yet)",
                com.rtshop.story.Spots.look(a, "t_circle") && QuestManager.pendingInspects(a).size() == 2);
        check("finishing the look fills the goal, removes the trace and leaves a mark for the notebook",
                com.rtshop.story.Spots.done(a, "t_circle") && QuestManager.pendingInspects(a).size() == 1
                        && com.rtshop.story.Spots.placed(a, "t_circle") == null && QuestManager.hasFlag(a, "spot_t_circle"));
        check("the same trace can not be looked at twice", !com.rtshop.story.Spots.done(a, "t_circle"));
        com.google.gson.JsonArray note = com.rtshop.story.Spots.notebook(a).getAsJsonArray("entries");
        check("the field notebook lists what was looked at, with its text",
                note.size() == 1 && note.get(0).getAsJsonObject().get("label").getAsString().equals("Old circle")
                        && note.get(0).getAsJsonObject().getAsJsonArray("lines").size() == 1, note);

        // 하는 일(verb)·드는 아이템(use)·이어지는 흔적(after)
        QuestDef actQ = tempQuest("{\"id\":\"t_act\",\"giver\":\"quest_guide\",\"objectives\":["
                + "{\"type\":\"INSPECT\",\"target\":\"t_wick\",\"verb\":\"Change the wick\",\"use\":\"minecraft:coal\"},"
                + "{\"type\":\"INSPECT\",\"target\":\"t_next\",\"after\":\"t_wick\"}],\"reward\":{\"gold\":1}}");
        QuestManager.loaded().quests.put(actQ.id, actQ);
        QuestDef actBack = QuestDef.parse(actQ.toJson());
        clear(a);
        QuestManager.accept(a, "t_act", "quest_guide");
        com.rtshop.story.Spots.update(a);
        boolean firstOnly = com.rtshop.story.Spots.placed(a, "t_wick") != null && com.rtshop.story.Spots.placed(a, "t_next") == null;
        com.rtshop.story.Spots.placeForTest(a, "t_wick", a.blockPosition().offset(1, 0, 0));
        boolean refused = !com.rtshop.story.Spots.done(a, "t_wick");
        a.getInventory().add(new ItemStack(Items.COAL, 2));
        boolean wickDone = com.rtshop.story.Spots.done(a, "t_wick");
        int coalLeft = (int) count(a, st -> st.is(Items.COAL));
        com.rtshop.story.Spots.update(a);
        boolean nextShown = com.rtshop.story.Spots.placed(a, "t_next") != null;
        check("a trace can be a thing to do: it needs its item (taken when done), and the next trace shows only after it",
                actBack != null && actBack.objectives.get(0).verb.equals("Change the wick") && actBack.objectives.get(0).use.equals("minecraft:coal")
                        && actBack.objectives.get(1).after.equals("t_wick") && firstOnly && refused && wickDone && coalLeft == 1 && nextShown,
                firstOnly + "/" + refused + "/" + wickDone + "/" + coalLeft + "/" + nextShown);
        QuestManager.abandon(a, "t_act");
        QuestManager.loaded().quests.remove("t_act");
        QuestManager.setFlag(a, "spot_t_wick", false);
        clear(a);

        // 동행
        com.rtshop.story.CompanionEntity c = com.rtshop.story.Companions.spawn(a, q);
        check("the companion appears beside the player with the npc's name", c != null && c.isAlive()
                && c.getName().getString().equals("Tess") && com.rtshop.story.Companions.of(a, "t_inspect") == c
                && c.distanceTo(a) < 6, c == null ? "null" : c.getName().getString());
        if (c != null) {
            // 동행의 말: 도착 인사는 한 번, 혼잣말은 줄마다 한 번, 외침은 드물게, 흔적 한마디는 그 흔적에 붙은 말만 한 번
            boolean arrived = com.rtshop.story.Companions.said(a, "t_inspect", "arrive");
            c.lastSaid = 0;
            boolean arriveAgain = com.rtshop.story.Companions.say(c, "arrive");
            c.lastSaid = 0;
            boolean idle1 = com.rtshop.story.Companions.say(c, "idle");
            boolean tooSoon = com.rtshop.story.Companions.say(c, "idle");
            c.lastSaid = 0;
            boolean idle2 = com.rtshop.story.Companions.say(c, "idle");
            c.lastSaid = 0;
            boolean idle3 = com.rtshop.story.Companions.say(c, "idle");
            c.lastSaid = 0;
            boolean fight1 = com.rtshop.story.Companions.say(c, "fight");
            c.lastSaid = 0;
            boolean fight2 = com.rtshop.story.Companions.say(c, "fight");
            check("a companion greets once, says each idle line once (never two lines within seconds), and shouts in a fight only now and then",
                    arrived && !arriveAgain && idle1 && !tooSoon && idle2 && !idle3 && fight1 && !fight2,
                    arrived + "/" + arriveAgain + "/" + idle1 + "/" + tooSoon + "/" + idle2 + "/" + idle3 + "/" + fight1 + "/" + fight2);
            c.lastSaid = 0;
            com.rtshop.story.Companions.onSpotNear(a, "t_inspect", "t_circle");
            boolean otherSpot = c.lastSaid != 0;
            com.rtshop.story.Companions.onSpotNear(a, "t_inspect", "t_far@minecraft:end_city");
            boolean ownSpot = c.lastSaid != 0 && com.rtshop.story.Companions.said(a, "t_inspect", "spot:t_far");
            c.lastSaid = 0;
            com.rtshop.story.Companions.onSpotNear(a, "t_inspect", "t_far@minecraft:end_city");
            check("a companion speaks about a trace only when that trace has its own line, and only once", !otherSpot && ownSpot && c.lastSaid == 0,
                    otherSpot + "/" + ownSpot + "/" + c.lastSaid);
            c.hurt(level.damageSources().generic(), 10000F);
            check("a companion never dies (it only falls back for a moment)", c.isAlive() && !c.isRemoved(), c.getHealth());
            net.minecraft.world.entity.monster.Zombie z = EntityType.ZOMBIE.create(level);
            z.moveTo(a.getX() + 1, a.getY(), a.getZ(), 0, 0);
            level.addFreshEntity(z);
            z.hurt(level.damageSources().mobAttack(c), 10000F);
            int[] pg = QuestData.get(server).peek(a.getUUID()).active.get("t_inspect");
            check("a kill by the companion counts for its owner's quest", !z.isAlive() && pg != null && pg[2] == 1,
                    pg == null ? "null" : java.util.Arrays.toString(pg));
            if (z.isAlive()) z.discard();
            // 여럿이 함께: 같은 NPC 가 동행하는 퀘스트를 가진 둘째 플레이어는 새 동행을 부르지 않고 일행이 된다
            FakePlayer walker = fake(level, "rt_walker");
            QuestManager.reset(walker);
            walker.moveTo(a.getX() + 2, a.getY(), a.getZ(), 0, 0);
            boolean took = QuestManager.accept(walker, "t_inspect", "quest_guide");
            com.rtshop.story.CompanionEntity there = com.rtshop.story.Companions.nearby(walker, "t_comp");
            if (there != null) com.rtshop.story.Companions.join(walker, q, there);
            boolean one = there == c && com.rtshop.story.Companions.of(walker, "t_inspect") == c && c.isGuest(walker.getUUID()) && c.group().size() == 2;
            boolean greeted = com.rtshop.story.Companions.said(walker, "t_inspect", "arrive");
            net.minecraft.world.entity.monster.Zombie z2 = EntityType.ZOMBIE.create(level);
            z2.moveTo(a.getX() + 1, a.getY(), a.getZ(), 0, 0);
            level.addFreshEntity(z2);
            z2.hurt(level.damageSources().mobAttack(c), 10000F);
            int[] pw = QuestData.get(server).peek(walker.getUUID()).active.get("t_inspect");
            check("playing together: a second player with the same companion joins the one already there (no twin), is greeted once, and shares its kills",
                    took && one && greeted && pw != null && pw[2] == 1, took + "/" + one + "/" + greeted + "/" + (pw == null ? "null" : java.util.Arrays.toString(pw)));
            if (z2.isAlive()) z2.discard();
            // 점검에서는 동행을 손으로 틱한다: 발밑 청크가 없어 아래로 떨어지지 않게 띄워 두고 제자리에 다시 세운다
            c.setNoGravity(true);
            c.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            c.moveTo(a.getX() + 1, a.getY(), a.getZ(), 0, 0);
            QuestManager.abandon(a, "t_inspect");
            for (int i = 0; i < 220 && !c.isRemoved() && c.owner() != walker; i++) { // 쓰러져 있던 동행이 일어난 뒤에 알아챈다
                c.tickCount++;
                c.tick();
            }
            check("when the one in front leaves, the companion stays and walks with the rest", !c.isRemoved() && c.owner() == walker
                    && com.rtshop.story.Companions.of(walker, "t_inspect") == c && com.rtshop.story.Companions.count(a) == 0
                    && !com.rtshop.story.Companions.said(a, "t_inspect", "arrive"), c.isRemoved() + "/" + (c.owner() == walker));
            QuestManager.abandon(walker, "t_inspect");
            for (int i = 0; i < 220 && !c.isRemoved(); i++) {
                c.tickCount++;
                c.tick();
            }
            check("the companion leaves when its quest is no longer active (and what it said there is forgotten)", c.isRemoved() && com.rtshop.story.Companions.count(a) == 0
                    && com.rtshop.story.Companions.count(walker) == 0 && !com.rtshop.story.Companions.said(walker, "t_inspect", "arrive"));
            QuestManager.reset(walker);
            if (!c.isRemoved()) c.discard();
        }

        // 룬 제단 잠금
        String keep = altarKeep != null ? altarKeep : com.rtshop.story.StorySystems.altarLockFlag;
        check("by default the rune altar opens once chapter 1 is done", "story_ch1_done".equals(keep) || net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("rtshop/story_systems.json").toFile().exists(), keep);
        com.rtshop.story.StorySystems.altarLockFlag = "t_altar_open";
        boolean lockedBefore = com.rtshop.magic.MagicGacha.locked(a);
        QuestManager.setFlag(a, "t_altar_open", true);
        boolean lockedAfter = com.rtshop.magic.MagicGacha.locked(a);
        com.rtshop.story.StorySystems.altarLockFlag = "";
        QuestManager.setFlag(a, "t_altar_open", false);
        check("the rune altar stays shut until the story mark is earned (and is open to all when no mark is set)",
                lockedBefore && !lockedAfter && !com.rtshop.magic.MagicGacha.locked(a), lockedBefore + "/" + lockedAfter);
        com.rtshop.story.StorySystems.altarLockFlag = keep;

        // 용 소환식: 허공 밖에서는 아무 일도 하지 않는다
        check("the dragon rite does nothing outside the End", !com.rtshop.story.DragonRite.settle(level));
        ServerLevel end = server.getLevel(net.minecraft.world.level.Level.END);
        if (end != null && end.getDragonFight() != null) {
            boolean before = end.getDragonFight().hasPreviouslyKilledDragon();
            boolean did = com.rtshop.story.DragonRite.settle(end);
            check("with nobody in the End (no dragon loaded) the rite waits and changes nothing",
                    did == !end.getDragons().isEmpty() || end.getDragonFight().hasPreviouslyKilledDragon() == before || did, did);
        }
        QuestManager.loaded().quests.remove("t_inspect");
        QuestManager.loaded().npcs.remove("t_comp");
        QuestManager.reset(a);
        // 마나 통일 (RPG Series → Iron's Spells 마나)
        {
            boolean both = net.minecraftforge.fml.ModList.get().isLoaded("spell_engine") && net.minecraftforge.fml.ModList.get().isLoaded("irons_spellbooks");
            com.rtshop.compat.SpellMana.SPELL.put("t:free", 0);
            check("spell mana: cost follows the spell's tier, the highest tier covers anything above, and a per-spell value wins",
                    com.rtshop.compat.SpellMana.cost("t:x", 2) == com.rtshop.compat.SpellMana.TIER.get(2)
                            && com.rtshop.compat.SpellMana.cost("t:x", 99) == com.rtshop.compat.SpellMana.TIER.get(4)
                            && com.rtshop.compat.SpellMana.cost("t:free", 3) == 0, com.rtshop.compat.SpellMana.TIER);
            com.rtshop.compat.SpellMana.SPELL.remove("t:free");
            check("spell mana: hooked into Spell Engine when it and Iron's Spells are both present (and idle otherwise)",
                    com.rtshop.compat.SpellMana.hookedForTest() == both, both + " hooked=" + com.rtshop.compat.SpellMana.hookedForTest());
            if (both) check("spell mana: a player's Iron's Spells mana can be read", com.rtshop.compat.SpellMana.mana(a) >= 0, com.rtshop.compat.SpellMana.mana(a));
            if (both && net.minecraftforge.fml.ModList.get().isLoaded("wizards")) {
                String empty = "?", full = "?";
                try {
                    java.lang.reflect.Method at = Class.forName("net.spell_engine.internals.casting.SpellCasting")
                            .getMethod("attempt", net.minecraft.world.entity.player.Player.class, ItemStack.class, ResourceLocation.class, boolean.class);
                    ResourceLocation fb = new ResourceLocation("wizards", "fireball");
                    com.rtshop.compat.SpellMana.setManaForTest(a, 0F);
                    Object r0 = at.invoke(null, a, ItemStack.EMPTY, fb, false);
                    empty = String.valueOf(r0.getClass().getMethod("result").invoke(r0));
                    com.rtshop.compat.SpellMana.setManaForTest(a, 500F);
                    Object r1 = at.invoke(null, a, ItemStack.EMPTY, fb, false);
                    full = String.valueOf(r1.getClass().getMethod("result").invoke(r1));
                } catch (Throwable t) {
                    empty = String.valueOf(t.getCause() != null ? t.getCause() : t);
                }
                check("spell mana: with no mana an RPG Series spell (wizards:fireball) can not be cast, with mana it can",
                        empty.equals("ON_COOLDOWN") && full.equals("SUCCESS"), empty + " / " + full);
            }
        }
        // 황혼불: 지니고 있을 때만 열리는 대화 조건
        {
            com.google.gson.JsonObject cond = com.google.gson.JsonParser.parseString("{\"need\":{\"items\":[\"minecraft:blaze_rod\"]}}").getAsJsonObject();
            a.getInventory().clearContent();
            boolean without = com.rtshop.story.Dialogue.okForTest(a, cond);
            a.getInventory().add(new ItemStack(net.minecraft.world.item.Items.BLAZE_ROD));
            boolean with = com.rtshop.story.Dialogue.okForTest(a, cond);
            a.getInventory().clearContent();
            check("a conversation can depend on an item being carried (Duskfire's hidden talk) and the item has its story text",
                    !without && with && com.rtshop.story.BossIntros.ITEM_LORE.containsKey("efn:meen_spear")
                            && com.rtshop.story.Dialogue.npcForTest("quest_order_elder").toString().contains("duskfire"), without + "/" + with);
        }
        // 3.52: 처음 한 번만 하는 인사(once) 와 가끔만 하는 인사(chance). 그 창을 든 사람에게 놀라는 말은 한 번뿐이다
        {
            com.google.gson.JsonObject npc = com.google.gson.JsonParser.parseString("{\"greet\":[\"hello\","
                    + "{\"text\":\"now and then\",\"chance\":0.2,\"need\":{\"items\":[\"minecraft:blaze_rod\"]}},"
                    + "{\"text\":\"what is that\",\"once\":true,\"need\":{\"items\":[\"minecraft:blaze_rod\"]}}]}").getAsJsonObject();
            String key = "selftest_greeter";
            a.getInventory().clearContent();
            QuestManager.setFlag(a, com.rtshop.story.Dialogue.greetFlag(key, "what is that"), false);
            com.rtshop.story.Dialogue.forcedRoll = 0.0;
            String none = com.rtshop.story.Dialogue.greet(a, key, npc, true);
            a.getInventory().add(new ItemStack(net.minecraft.world.item.Items.BLAZE_ROD));
            String hidden = com.rtshop.story.Dialogue.greet(a, key, npc, false);   // 장면 중: 아껴 둔다
            String first = com.rtshop.story.Dialogue.greet(a, key, npc, true);
            String second = com.rtshop.story.Dialogue.greet(a, key, npc, true);    // 주사위 0.0: 가끔 하는 말
            com.rtshop.story.Dialogue.forcedRoll = 0.9;
            String third = com.rtshop.story.Dialogue.greet(a, key, npc, true);     // 주사위 0.9: 보통 인사
            com.rtshop.story.Dialogue.forcedRoll = -1;
            a.getInventory().clearContent();
            String dusk = com.rtshop.story.Dialogue.npcForTest("shop_blacksmith").toString();
            String lore = String.join(" ", com.rtshop.story.BossIntros.ITEM_LORE.get("efn:meen_spear"));
            check("a greeting can be said only the first time, and another only now and then; people point at the spear without saying its name and its text says whose name was left off",
                    none.equals("hello") && hidden.equals("what is that") && first.equals("what is that") && second.equals("now and then") && third.equals("hello")
                            && dusk.contains("\"once\":true") && dusk.contains("\"chance\"") && dusk.contains("그 창에 대하여") && !dusk.contains("황혼의 불꽃")
                            && lore.contains("자기 이름을 새기지 않았다"),
                    none + " / " + hidden + " / " + first + " / " + second + " / " + third);
        }
        // 3.52: 이야기 물건을 펼쳐 읽는다 (서약서의 셋째 줄은 종장 뒤에야 보이고, 연대기 페이지에는 기록관이 적은 이름이 쌓인다)
        {
            boolean had510 = QuestManager.setCompletedForTest(a, "story_5_10", false), had104 = QuestManager.setCompletedForTest(a, "story_1_04", false);
            com.google.gson.JsonObject oath = com.rtshop.story.Readables.build(a, "rtshop:oath_scroll");
            int before = com.rtshop.story.Readables.chronicle(a).size();
            QuestManager.setCompletedForTest(a, "story_5_10", true);
            QuestManager.setCompletedForTest(a, "story_1_04", true);
            com.google.gson.JsonObject oath2 = com.rtshop.story.Readables.build(a, "rtshop:oath_scroll");
            com.google.gson.JsonObject chron = com.rtshop.story.Readables.build(a, "rtshop:chronicle_page");
            int after = com.rtshop.story.Readables.chronicle(a).size();
            QuestManager.setCompletedForTest(a, "story_5_10", had510);
            QuestManager.setCompletedForTest(a, "story_1_04", had104);
            String o1 = String.valueOf(oath), o2 = String.valueOf(oath2), chronText = String.valueOf(chron);
            ItemStack scroll = new ItemStack(com.rtshop.item.ModItems.OATH_SCROLL.get());
            check("story papers can be read: the oath shows its third line only after the ending, the chronicle page lists the names written so far, other items are not readable",
                    oath != null && oath.getAsJsonArray("pages").size() == 1 && o1.contains("지워져 있다") && !o1.contains("때가 되면 놓아준다")
                            && o2.contains("때가 되면 놓아준다") && oath2.getAsJsonArray("pages").size() == 1
                            && after == before + 2 && chronText.contains("도윈, 광부") && chronText.contains("길 위의 이름")
                            && com.rtshop.story.Readables.build(a, "minecraft:stick") == null
                            && com.rtshop.story.Readables.open(a, scroll) && !com.rtshop.story.Readables.open(a, new ItemStack(net.minecraft.world.item.Items.STICK))
                            && com.rtshop.story.Readables.has("rtshop:mother_journal") && com.rtshop.story.Readables.has("rtshop:frozen_logbook")
                            && com.rtshop.story.Readables.has("rtshop:old_expedition_map") && com.rtshop.story.Readables.has("rtshop:chronicle_last_page"),
                    before + " -> " + after + " / " + (oath == null ? "null" : oath.getAsJsonArray("pages").size()));
        }
        // 3.53: 기록·주간 순위·칭호
        ranks(server, a, fake(level, "rtshop_test_b"));
        // 3.54: 도감
        codex(server, level, a);
        mapMarks(server, level, a);
        // 3.53: 아이템 쓰임새 설명 (상점·강화·퀘스트에서 저절로)
        {
            java.util.List<String[]> wheat = com.rtshop.rules.ItemNotes.of("minecraft:wheat");
            boolean sold = false, bought = false;
            for (String[] ln : wheat) {
                sold |= ln[0].equals("rtshop.note.sold_by");
                bought |= ln[0].equals("rtshop.note.bought_by") && Long.parseLong(ln[2]) >= 1;
            }
            boolean bossNote = com.rtshop.rules.ItemNotes.of("minecraft:nether_star").stream().anyMatch(ln -> ln[0].equals("rtshop.note.boss"));
            check("item notes: an item says who buys it (wheat), boss loot says shops do not trade it, and things with no use have no note",
                    bought && bossNote && com.rtshop.rules.ItemNotes.of("minecraft:bedrock").isEmpty() && com.rtshop.rules.ItemNotes.countForTest() > 50,
                    com.rtshop.rules.ItemNotes.countForTest() + " items, wheat " + wheat.size() + " sold " + sold + " bought " + bought + " boss " + bossNote);
        }
        // 4판(대개편) 서장·1장
        QuestManager.Loaded l = QuestManager.loaded();
        QuestDef helvar = l.quests.get("story_2_06b"), lesson = l.quests.get("story_0_02b"), walk = l.quests.get("story_1_02");
        check("the rewritten prologue and chapter 1 are installed (lesson at the practice gate, Isabel walks along, the orb after the chapter) and Helvar waits in chapter 2",
                lesson != null && helvar != null && walk != null && l.quests.containsKey("story_1_41") && walk.companion != null
                        && lesson.objectives.stream().anyMatch(o -> o.type == QuestDef.Type.INSPECT)
                        && helvar.objectives.size() == 1 && helvar.objectives.get(0).type == QuestDef.Type.KILL
                        && l.npcs.containsKey("felix") && l.npcs.containsKey("bram"),
                helvar == null ? "null" : helvar.toJson());
        java.util.Set<String> ids = new java.util.HashSet<>();
        List<String> dup = new ArrayList<>();
        for (QuestDef sq : l.quests.values()) {
            for (QuestDef.Objective ob : sq.objectives) if (ob.type == QuestDef.Type.INSPECT && !ids.add(ob.target)) dup.add(ob.target);
        }
        check("every trace in the story has its own id", dup.isEmpty(), dup);
        // 이주: 1장 도중이던 사람은 1장의 처음부터 (서장은 끝낸 것으로), 1장을 끝낸 사람은 새 1장도 끝낸 것으로
        QuestManager.reset(a);
        QuestManager.setFlag(a, "order_apprentice", true);
        QuestManager.forceComplete(a, "story_1_05");
        QuestManager.setFlag(a, "story_v4", false);
        QuestManager.migrateStory4(a);
        boolean mid = !QuestManager.isCompleted(a, "story_1_05") && QuestManager.isCompleted(a, "story_0_02b") && QuestManager.hasFlag(a, "story_v4");
        QuestManager.reset(a);
        QuestManager.setFlag(a, "order_apprentice", true);
        QuestManager.setFlag(a, "story_ch1_done", true);
        QuestManager.setFlag(a, "story_v4", false);
        QuestManager.migrateStory4(a);
        boolean fin = QuestManager.isCompleted(a, "story_1_41") && QuestManager.isCompleted(a, "story_1_21") && !l.quests.containsKey("story_1_19") && QuestManager.hasFlag(a, "ch1_three_ends");
        check("moving to the rewritten story: mid-chapter players restart chapter 1 (prologue kept), finished players keep chapter 1 done", mid && fin, mid + "/" + fin);
        QuestDef skor = l.quests.get("story_2_13"), rest = l.quests.get("story_2_10"), letter = l.quests.get("story_2_24");
        check("chapter 2 is rewritten (Roran walks along, those who wait: Helvar, Sirok, Ashlord; Skor; Kael's letter; the roster is written) and the antidote replaces milk",
                skor != null && rest != null && letter != null && l.quests.containsKey("story_2_26") && !l.quests.containsKey("story_2_21")
                        && l.quests.containsKey("story_2_09b") && l.quests.containsKey("story_2_18b")
                        && l.quests.get("story_1_17").objectives.stream().anyMatch(o -> o.target.equals("rtshop:antidote"))
                        && l.quests.get("story_2_02").companion != null && l.quests.get("story_2_25").rewardFlags.contains("ch2_roster_written")
                        && l.quests.get("story_3_01").requires.contains("story_2_26"), skor == null ? "null" : skor.toJson());
        QuestManager.reset(a);
        QuestManager.setFlag(a, "story_v4", true);
        QuestManager.forceComplete(a, "story_2_05");
        QuestManager.setFlag(a, "seal_wroughtnaut", true);
        QuestManager.setFlag(a, "story_v5", false);
        QuestManager.migrateStory5(a);
        boolean mid2 = !QuestManager.isCompleted(a, "story_2_05") && !QuestManager.hasFlag(a, "seal_wroughtnaut") && QuestManager.hasFlag(a, "story_v5");
        QuestManager.reset(a);
        QuestManager.setFlag(a, "story_ch2_done", true);
        QuestManager.setFlag(a, "story_v5", false);
        QuestManager.migrateStory5(a);
        boolean fin2 = QuestManager.isCompleted(a, "story_2_26") && QuestManager.hasFlag(a, "seal_umvuthi");
        check("chapter 2 progress moves over the same way", mid2 && fin2, mid2 + "/" + fin2);
        QuestDef ner = l.quests.get("story_3_20"), orb = l.quests.get("story_3_36"), king = l.quests.get("story_3_28"), capt = l.quests.get("story_3_32");
        boolean fd = net.minecraftforge.fml.ModList.get().isLoaded("fdbosses");
        check("chapter 3 is rebuilt (the village's night, the twentieth boat, Nerakyss, the giving king under the sea, and the captain last)",
                ner != null && orb != null && king != null && capt != null && l.quests.containsKey("story_3_39") && !l.quests.containsKey("story_3_40")
                        && l.quests.get("story_3_06").companion != null && l.quests.get("story_3_25").companion != null
                        && ner.objectives.size() == 1 && ner.objectives.get(0).target.equals(net.minecraftforge.fml.ModList.get().isLoaded("block_factorys_bosses") ? "block_factorys_bosses:kraken" : "minecraft:elder_guardian")
                        && king.objectives.size() == 1 && king.objectives.get(0).target.equals(fd ? "fdbosses:malkuth" : "minecraft:elder_guardian")
                        && king.order < capt.order && capt.order < orb.order
                        && orb.rewardFlags.contains("clue_two_cloaks") && l.quests.get("story_3_33").rewardFlags.contains("clue_hidden_page")
                        && l.quests.get("story_3_35").choices.size() == 2 && l.quests.get("story_3_39").rewardFlags.contains("story_ch3_done")
                        && l.quests.get("story_4_01").requires.contains("story_3_39") && (!l.quests.containsKey("hidden_illusioner_0") || l.quests.get("hidden_illusioner_0").requires.contains("story_3_33")),
                ner == null ? "null" : ner.toJson());
        QuestDef stone = l.quests.get("story_2_24f"), lamp = l.quests.get("story_3_03");
        check("chapter 2 ends under the first cave (Chesed), and some goals are things to do rather than look at (a verb, an item used, one trace after another)",
                stone != null && stone.objectives.size() == 1 && stone.objectives.get(0).target.equals(fd ? "fdbosses:chesed" : "minecraft:warden")
                        && l.quests.get("story_2_25").requires.contains("story_2_24f")
                        && lamp.objectives.get(0).verb.equals("심지를 간다") && lamp.objectives.get(0).use.equals("minecraft:coal")
                        && (!net.minecraftforge.fml.ModList.get().isLoaded("aquamirae") || l.quests.get("story_3_08").objectives.stream().anyMatch(o -> o.after.equals("c3_wall1")))
                        && com.rtshop.party.PartyConfig.get().arenaBosses.contains("fdbosses:chesed"),
                stone == null ? "null" : stone.toJson());
        QuestManager.reset(a);
        for (String f : List.of("story_v4", "story_v5")) QuestManager.setFlag(a, f, true);
        QuestManager.forceComplete(a, "story_3_05");
        QuestManager.setFlag(a, "seal_maze_mother", true);
        QuestManager.setFlag(a, "story_v6", false);
        QuestManager.migrateStory6(a);
        boolean mid3 = !QuestManager.isCompleted(a, "story_3_05") && !QuestManager.hasFlag(a, "seal_maze_mother") && QuestManager.hasFlag(a, "story_v6");
        QuestManager.reset(a);
        for (String f : List.of("story_ch3_done", "ch3_released")) QuestManager.setFlag(a, f, true);
        QuestManager.setFlag(a, "story_v6", false);
        QuestManager.migrateStory6(a);
        boolean fin3 = QuestManager.isCompleted(a, "story_3_39") && QuestManager.hasFlag(a, "seal_cornelia") && QuestManager.hasFlag(a, "clue_two_cloaks")
                && QuestManager.hasFlag(a, "ch3_released") && !QuestManager.hasFlag(a, "ch3_kept_lantern");
        check("chapter 3 progress moves over the same way (the lantern choice is kept)", mid3 && fin3, mid3 + "/" + fin3);
        QuestManager.reset(a);
        for (String f : List.of("story_v4", "story_v5", "story_v6", "story_v7", "story_v8")) QuestManager.setFlag(a, f, true);
        QuestManager.forceComplete(a, "story_3_12");
        QuestManager.setFlag(a, "ch3_found_elric", true);
        QuestManager.migrateStory9(a);
        boolean mid9 = !QuestManager.isCompleted(a, "story_3_12") && !QuestManager.hasFlag(a, "ch3_found_elric") && QuestManager.hasFlag(a, "story_v9");
        check("a player in the middle of the old chapter 3 starts the rebuilt chapter from its beginning", mid9, mid9);
        QuestDef door = l.quests.get("story_4_26"), glove = l.quests.get("story_4_28");
        check("chapter 4 is rewritten (six seals walked six ways, the portraits, the council, through the door, under the glove)",
                door != null && glove != null && l.quests.containsKey("story_4_29") && !l.quests.containsKey("story_4_30")
                        && l.quests.get("story_4_02").companion != null && l.quests.get("story_4_12").companion != null && door.choices.size() == 2
                        && glove.choices.size() == 2 && glove.rewardFlags.contains("erwin_betrayal") && l.quests.get("story_4_11").rewardFlags.contains("clue_eighth_king")
                        && l.quests.get("story_4_29").rewardFlags.contains("story_ch4_done") && l.quests.get("story_5_01").requires.contains("story_4_29"),
                door == null ? "null" : door.toJson());
        QuestManager.reset(a);
        for (String f : List.of("story_v4", "story_v5", "story_v6")) QuestManager.setFlag(a, f, true);
        QuestManager.forceComplete(a, "story_4_05");
        QuestManager.setFlag(a, "seal_monstrosity", true);
        QuestManager.setFlag(a, "story_v7", false);
        QuestManager.migrateStory7(a);
        boolean mid4 = !QuestManager.isCompleted(a, "story_4_05") && !QuestManager.hasFlag(a, "seal_monstrosity") && QuestManager.hasFlag(a, "story_v7");
        QuestManager.reset(a);
        for (String f : List.of("story_ch4_done", "ch4_feigned")) QuestManager.setFlag(a, f, true);
        QuestManager.setFlag(a, "story_v7", false);
        QuestManager.migrateStory7(a);
        boolean fin4 = QuestManager.isCompleted(a, "story_4_29") && QuestManager.hasFlag(a, "seal_maledictus") && QuestManager.hasFlag(a, "erwin_betrayal")
                && QuestManager.hasFlag(a, "ch4_feigned") && !QuestManager.hasFlag(a, "ch4_refused");
        check("chapter 4 progress moves over the same way (the answer to Erwin is kept)", mid4 && fin4, mid4 + "/" + fin4);
        QuestManager.reset(a);
        for (String f : List.of("story_v4", "story_v5", "story_v6", "story_v7", "story_v8", "story_v9")) QuestManager.setFlag(a, f, true);
        QuestManager.forceComplete(a, "story_4_01");
        QuestManager.forceComplete(a, "story_4_02");
        QuestManager.forceComplete(a, "story_4_14");
        QuestManager.migrateStory10(a);
        boolean filled = QuestManager.isCompleted(a, "story_4_01a") && QuestManager.isCompleted(a, "story_4_05a") && QuestManager.isCompleted(a, "story_4_13a")
                && QuestManager.hasFlag(a, "clue_211th") && !QuestManager.isCompleted(a, "story_4_14a") && !QuestManager.isCompleted(a, "story_4_17a") && QuestManager.hasFlag(a, "story_v10");
        QuestDef scale = l.quests.get("story_4_23c"), pegs = l.quests.get("story_4_02a");
        boolean ch4more = scale != null && pegs != null && pegs.companion != null && l.quests.get("story_4_24").requires.contains("story_4_23e")
                && l.quests.get("story_4_28").requires.contains("story_4_27a") && l.quests.get("story_4_03").requires.contains("story_4_02a")
                && l.quests.values().stream().filter(x -> x.id.startsWith("story_4_")).count() >= 50
                && l.quests.values().stream().filter(x -> x.companion != null).noneMatch(x -> x.companion.toString().contains("\".\""));
        check("chapter 4 has its added steps (pegs, canal, drawers, song, the court of the scale, the cells) and steps slipped in behind a player count as walked",
                filled && ch4more, filled + "/" + ch4more);
        QuestDef ash = l.quests.get("story_5_11"), palm = l.quests.get("story_5_07");
        boolean ill = net.minecraftforge.fml.ModList.get().isLoaded("illusioner");
        check("the finale is rewritten (the empty room, ink dots, a name on a palm, the third line, one line of ash) and the mirror story ends with the name",
                ash != null && palm != null && l.quests.containsKey("story_5_12") && !l.quests.containsKey("story_5_13") && ash.choices.size() == 2
                        && l.quests.get("story_5_04").companion != null && l.quests.get("story_5_12").rewardFlags.contains("story_done")
                        && l.quests.get("story_5_10").rewardFlags.contains("third_line_found") && l.quests.get("story_5_08").rewardFlags.contains("ignis_raid_story")
                        && l.quests.containsKey("hidden_illusioner_6") == ill
                        && (!ill || (l.quests.get("hidden_illusioner_1").requires.contains("story_5_12") && l.quests.get("hidden_illusioner_6").choices.size() == 2)),
                ash == null ? "null" : ash.toJson());
        // 3.60: 이글루 걸음은 사람마다 따로 받아야 하는 발전과제 대신 흔적에 황금 사과를 건넨다 / 프로스트모의 뜻 없는 선택지는 없앴다
        QuestDef ig360 = l.quests.get("story_1_10"), fr360 = l.quests.get("story_2_15");
        boolean adv360 = false, gold360 = false;
        if (ig360 != null) {
            for (QuestDef.Objective ob360 : ig360.objectives) {
                if (ob360.type == QuestDef.Type.ADVANCEMENT) adv360 = true;
                if (ob360.type == QuestDef.Type.INSPECT && ob360.use.equals("minecraft:golden_apple") && ob360.after.equals("c1_igloo")) gold360 = true;
            }
        }
        check("3.60: the igloo step needs no personal advancement (a golden apple at a trace instead) and the frost giant's leftover choice is gone",
                ig360 != null && !adv360 && gold360 && (fr360 == null || fr360.choices.isEmpty()), adv360 + " " + gold360);
        // 3.60: 민원 — 접수(글 다듬기 · 자리) · 간격 · 빈 글 · 긴 글 · 답변 · 답 달린 것은 못 거둠 · 종결 · 한도 · 저장 · 명령
        com.rtshop.petition.Petitions.forTestReset(a.server);
        int mwA = com.rtshop.petition.Petitions.file(a, "  땅 경계가   이상해요  ");
        int mwSoon = com.rtshop.petition.Petitions.file(a, "바로 또");
        com.rtshop.petition.Petitions.forTestNoCooldown();
        int mwEmpty = com.rtshop.petition.Petitions.file(a, "   ");
        int mwLong = com.rtshop.petition.Petitions.file(a, "가".repeat(com.rtshop.petition.Petitions.MAX_LEN + 1));
        com.rtshop.petition.Petitions.Entry mwE = com.rtshop.petition.Petitions.find(a.server, mwA);
        boolean mwText = mwE != null && mwE.text.equals("땅 경계가 이상해요") && mwE.state == com.rtshop.petition.Petitions.OPEN && !mwE.dim.isEmpty();
        boolean mwAns = com.rtshop.petition.Petitions.answer(a.server, mwA, "op", "확인했습니다") && mwE != null
                && mwE.state == com.rtshop.petition.Petitions.ANSWERED && mwE.answer.equals("확인했습니다");
        boolean mwKeep = !com.rtshop.petition.Petitions.cancel(a, mwA);
        boolean mwClose = com.rtshop.petition.Petitions.close(a.server, mwA) && !com.rtshop.petition.Petitions.close(a.server, mwA);
        int mwMany = 0;
        for (int mwI = 0; mwI < com.rtshop.petition.Petitions.MAX_OPEN + 1; mwI++) {
            com.rtshop.petition.Petitions.forTestNoCooldown();
            mwMany = com.rtshop.petition.Petitions.file(a, "민원 " + mwI);
        }
        net.minecraft.nbt.CompoundTag mwTag = com.rtshop.petition.Petitions.data(a.server).save(new net.minecraft.nbt.CompoundTag());
        boolean mwRound = com.rtshop.petition.Petitions.Data.load(mwTag).list.size() == com.rtshop.petition.Petitions.MAX_OPEN + 1;
        boolean mwCmd = a.server.getCommands().getDispatcher().findNode(List.of("rtshop", "report", "write")) != null
                && a.server.getCommands().getDispatcher().findNode(List.of("rtshop", "report", "answer")) != null;
        // 3.61: 민원의 분류 · 창에 보낼 내용 / NPC 외형: 열쇠 → 모습, 모습 파일
        com.rtshop.petition.Petitions.forTestReset(a.server);
        int mwCatId = com.rtshop.petition.Petitions.file(a, "land", "옆 땅과 경계가 겹쳐요");
        com.rtshop.petition.Petitions.forTestNoCooldown();
        int mwBadCat = com.rtshop.petition.Petitions.file(a, "nonsense", "분류가 없는 글");
        com.google.gson.JsonObject mwView = com.rtshop.petition.Petitions.view(a, "", false);
        com.rtshop.petition.Petitions.Entry mwCatE = com.rtshop.petition.Petitions.find(a.server, mwCatId), mwBadE = com.rtshop.petition.Petitions.find(a.server, mwBadCat);
        check("3.61 reports: a category is kept (an unknown one becomes 'etc'), and the clerk's window gets the categories, my reports (newest first) and the open count",
                mwCatE != null && mwCatE.cat.equals("land") && mwBadE != null && mwBadE.cat.equals("etc") && mwView.getAsJsonArray("cats").size() == 5
                        && mwView.getAsJsonArray("mine").size() == 2 && mwView.getAsJsonArray("mine").get(0).getAsJsonObject().get("id").getAsInt() == mwBadCat
                        && mwView.get("open").getAsInt() == 2 && !com.rtshop.petition.Petitions.nearClerk(a),
                mwCatId + " " + mwBadCat + " " + mwView);
        // 3.71: 민원 설정 파일 (한도를 바꾸면 그대로 따름) · 운영 탭 (운영자에게만 모든 민원, 창에서 답변 · 종결 · 지우기)
        com.rtshop.petition.Petitions.forTestReset(a.server);
        com.rtshop.petition.Petitions.Config pc371 = new com.rtshop.petition.Petitions.Config();
        pc371.maxLength = 40;
        pc371.maxOpen = 2;
        pc371.cooldownSeconds = 0;
        com.rtshop.petition.Petitions.forTestConfig(pc371);
        int pl371 = com.rtshop.petition.Petitions.file(a, "bug", "가".repeat(41));
        int p1371 = com.rtshop.petition.Petitions.file(a, "bug", "상점 창의 글자가 겹쳐요");
        int p2371 = com.rtshop.petition.Petitions.file(a, "idea", "낚시 대회를 열어 주세요");
        int p3371 = com.rtshop.petition.Petitions.file(a, "etc", "세 번째");
        com.google.gson.JsonObject pv371 = com.rtshop.petition.Petitions.view(a, "", false);
        boolean pLimits = pl371 == com.rtshop.petition.Petitions.ERR_LONG && p1371 > 0 && p2371 > 0 && p3371 == com.rtshop.petition.Petitions.ERR_MANY
                && pv371.get("max").getAsInt() == 40 && pv371.get("maxOpen").getAsInt() == 2;
        com.rtshop.petition.Petitions.forTestConfig(null);
        boolean pDefaults = com.rtshop.petition.Petitions.cfg().maxLength == com.rtshop.petition.Petitions.MAX_LEN && com.rtshop.petition.Petitions.cfg().maxOpen == com.rtshop.petition.Petitions.MAX_OPEN;
        // 운영자가 아니면 운영 탭의 일이 막힌다
        boolean wasOp371 = a.hasPermissions(2);
        com.rtshop.petition.Petitions.fromScreen(a, 4, p1371, "");
        boolean pDenied = wasOp371 || ("denied".equals(com.rtshop.petition.Petitions.lastAdminForTest) && com.rtshop.petition.Petitions.find(a.server, p1371).state == com.rtshop.petition.Petitions.OPEN)
                && !pv371.get("op").getAsBoolean() && !pv371.has("all");
        if (!wasOp371) a.server.getPlayerList().op(a.getGameProfile());
        com.rtshop.petition.Petitions.fromScreen(a, 3, p1371, "다음 판에서 고칩니다");
        com.rtshop.petition.Petitions.Entry pe371 = com.rtshop.petition.Petitions.find(a.server, p1371);
        boolean pAnswered = pe371 != null && pe371.state == com.rtshop.petition.Petitions.ANSWERED && pe371.answer.equals("다음 판에서 고칩니다");
        com.rtshop.petition.Petitions.fromScreen(a, 4, p1371, "");
        boolean pClosed = pe371 != null && pe371.state == com.rtshop.petition.Petitions.CLOSED;
        com.rtshop.petition.Petitions.fromScreen(a, 5, p2371, "");
        boolean pDeleted = com.rtshop.petition.Petitions.find(a.server, p2371) == null;
        com.google.gson.JsonObject pa371 = com.rtshop.petition.Petitions.view(a, "", false, 2);
        boolean pAdminView = pa371.get("op").getAsBoolean() && pa371.get("tab").getAsInt() == 2 && pa371.getAsJsonArray("all").size() == 1
                && pa371.getAsJsonArray("all").get(0).getAsJsonObject().get("where").getAsString().contains(",")
                && pa371.getAsJsonArray("all").get(0).getAsJsonObject().get("name").getAsString().equals(a.getGameProfile().getName());
        if (!wasOp371) a.server.getPlayerList().deop(a.getGameProfile());
        boolean pCmd371 = a.server.getCommands().getDispatcher().findNode(List.of("rtshop", "report", "admin")) != null
                && a.server.getCommands().getDispatcher().findNode(List.of("rtshop", "report", "reload")) != null;
        check("3.71 reports: the limits come from petitions.json (length · open count), operators get every report in the window and can answer, close and delete there; others cannot",
                pLimits && pDefaults && pDenied && pAnswered && pClosed && pDeleted && pAdminView && pCmd371,
                pl371 + " " + p1371 + " " + p2371 + " " + p3371 + " limits " + pLimits + " defaults " + pDefaults + " denied " + pDenied + " (op before " + wasOp371 + ") answered " + pAnswered
                        + " closed " + pClosed + " deleted " + pDeleted + " view " + pAdminView + " cmd " + pCmd371 + " " + com.rtshop.petition.Petitions.lastAdminForTest);
        com.rtshop.petition.Petitions.forTestReset(a.server);
        stocks(a.server, a);
        // 3.73: 성능 진단 (/fpscheck · /rtshop perfcheck) — 스택의 프레임을 모드로 돌리는 규칙
        StackTraceElement[] st373 = {
                new StackTraceElement("java.lang.Object", "wait", null, -1),
                new StackTraceElement("net.minecraft.world.level.Level", "m_6249_", null, -1),
                new StackTraceElement("com.rtshop.shop.ShopServer", "buy", null, -1),
                new StackTraceElement("net.minecraft.server.level.ServerLevel", "wrapOperation$fde000$spiral_abyss$tick", null, -1),
                new StackTraceElement("net.minecraft.server.MinecraftServer", "m_5705_", null, -1)};
        com.rtshop.stats.PerfCheck.Run pr373 = new com.rtshop.stats.PerfCheck.Run("test", Thread.currentThread(), 5);
        com.rtshop.stats.PerfCheckForTest.sample(pr373, st373);
        String rt373 = com.rtshop.stats.PerfCheck.modOf(st373[2]);
        java.util.List<String> rep373 = com.rtshop.stats.PerfCheck.report(pr373, java.util.List.of("head"), "MSPT");
        check("3.73 performance check: a sampled stack is charged to the innermost mod (RT Shop here, not Minecraft), a mixin handler to its mod id, and the report lists them",
                com.rtshop.stats.PerfCheck.modOf(st373[0]) == null && com.rtshop.stats.PerfCheck.modOf(st373[1]) == null && rt373 != null
                        && "mixin:spiral_abyss".equals(com.rtshop.stats.PerfCheck.modOf(st373[3])) && pr373.samples == 1 && pr373.self.getOrDefault(rt373, 0) == 1
                        && pr373.incl.getOrDefault("mixin:spiral_abyss", 0) == 1 && String.join("\n", rep373).contains(rt373)
                        && a.server.getCommands().getDispatcher().findNode(List.of("rtshop", "perfcheck")) != null,
                rt373 + " " + pr373.self + " " + pr373.incl);
        v374(a.server, a);
        // 3.62: 보스마다 다른 카메라 방식 / Cinematic Cataclysm 컷신으로 깨우기 (그 모드가 있을 때만)
        boolean cc362 = net.minecraftforge.fml.ModList.get().isLoaded("cinematiccataclysm");
        String ccTrace = "";
        boolean ccAll = true, ccNone = true;
        for (String cb362 : List.of("cataclysm:ancient_remnant", "cataclysm:the_leviathan", "cataclysm:maledictus", "cataclysm:ignis")) {
            String r362 = com.rtshop.story.StoryArena.cutsceneForTest(a.serverLevel(), a.serverLevel().getSharedSpawnPos().above(60), cb362);
            ccTrace += cb362.substring(10) + "=" + r362 + " ";
            if (!r362.equals("ok")) ccAll = false;
            else ccNone = false;
        }
        check("3.62 boss cutscenes: with Cinematic Cataclysm installed its four boss cutscenes start in the arena; without it bosses are raised directly; every boss has a camera style",
                (cc362 ? ccAll : ccNone) && com.rtshop.story.BossIntros.camOf("mowziesmobs:frostmaw").equals("rise")
                        && com.rtshop.story.BossIntros.camOf("cataclysm:netherite_monstrosity").equals("rise") && com.rtshop.story.BossIntros.camOf("no:such_boss").equals("orbit"),
                cc362 + " " + ccTrace);
        String lkMissing = "";
        for (String lk361 : com.rtshop.npc.NpcLooks.ALL) {
            int n363 = com.rtshop.npc.NpcLooks.quadsOf(lk361);   // 3.63: 인물당 네모 수는 프레임을 지키려고 600 을 넘기지 않는다
            if (n363 < 50 || n363 > 600 || com.rtshop.npc.NpcLooks.class.getResource("/assets/rtshop/textures/npc_looks/" + lk361 + ".png") == null) lkMissing += lk361 + "(" + n363 + ") ";
        }
        check("3.61 npc looks: every look has its model file, and NPC keys find their look (exact first, then the longest prefix; unknown keys stay villagers)",
                lkMissing.isEmpty() && com.rtshop.npc.NpcLooks.lookOf("quest_order_archivist").equals("isabel") && com.rtshop.npc.NpcLooks.lookOf("shop_general_books").equals("m_general")
                        && com.rtshop.npc.NpcLooks.lookOf("shop_blacksmith").equals("bram") && com.rtshop.npc.NpcLooks.lookOf("attendance").equals("haru")
                        && com.rtshop.npc.NpcLooks.lookOf("job").equals("albert") && com.rtshop.npc.NpcLooks.lookOf("shop_restaurant").equals("lorenzo")
                        && com.rtshop.npc.NpcLooks.lookOf("shop_attendance_shop").equals("eli") && com.rtshop.npc.NpcLooks.lookOf("shop_no_such_shop").isEmpty() && com.rtshop.npc.NpcLooks.lookOf(null).isEmpty(),
                lkMissing + "/" + com.rtshop.npc.NpcLooks.lookOf("quest_order_archivist") + "/" + com.rtshop.npc.NpcLooks.lookOf("shop_general_books"));
        com.rtshop.petition.Petitions.forTestReset(a.server);
        check("3.60 reports: filing trims the text and records the place, the cooldown, empty and long texts and the open limit refuse, an answer and a close change the state, an answered one cannot be withdrawn, the data survives saving, the commands exist",
                mwA > 0 && mwSoon == com.rtshop.petition.Petitions.ERR_SOON && mwEmpty == com.rtshop.petition.Petitions.ERR_EMPTY && mwLong == com.rtshop.petition.Petitions.ERR_LONG
                        && mwText && mwAns && mwKeep && mwClose && mwMany == com.rtshop.petition.Petitions.ERR_MANY && mwRound && mwCmd,
                mwA + " " + mwSoon + " " + mwEmpty + " " + mwLong + " " + mwText + " " + mwAns + " " + mwKeep + " " + mwClose + " " + mwMany + " " + mwRound + " " + mwCmd);
        QuestManager.reset(a);
        for (String f : List.of("story_v4", "story_v5", "story_v6", "story_v7")) QuestManager.setFlag(a, f, true);
        QuestManager.forceComplete(a, "story_5_03");
        QuestManager.setFlag(a, "seal_ignis", true);
        QuestManager.setFlag(a, "story_v8", false);
        QuestManager.migrateStory8(a);
        boolean mid5 = !QuestManager.isCompleted(a, "story_5_03") && !QuestManager.hasFlag(a, "seal_ignis") && QuestManager.hasFlag(a, "story_v8");
        QuestManager.reset(a);
        for (String f : List.of("story_done", "fin_erased")) QuestManager.setFlag(a, f, true);
        QuestManager.setFlag(a, "story_v8", false);
        QuestManager.migrateStory8(a);
        boolean fin5 = QuestManager.isCompleted(a, "story_5_12") && QuestManager.hasFlag(a, "seal_ignis") && QuestManager.hasFlag(a, "fin_erased") && !QuestManager.hasFlag(a, "fin_forgave");
        check("finale progress moves over the same way (the line of ash is kept)", mid5 && fin5, mid5 + "/" + fin5);
        QuestManager.reset(a);
        // 3.63: 이사벨은 사막 · 늪에 따라가지 않는다 / 1장 가운데의 쉬는 장면 둘 / 채우기용 처치 목표 줄임 / 목표가 늘어난 퀘스트의 진행 배열 맞추기 / 생물 군계에 놓는 흔적
        QuestManager.Loaded l363 = QuestManager.loaded();
        QuestDef q107 = l363.quests.get("story_1_07"), q113 = l363.quests.get("story_1_13"), q207 = l363.quests.get("story_2_07"), q407 = l363.quests.get("story_4_07");
        int big363 = 0;
        StringBuilder bigNames363 = new StringBuilder();
        for (QuestDef q363 : l363.quests.values()) {
            if (!q363.id.startsWith("story_2_") && !q363.id.startsWith("story_3_") && !q363.id.startsWith("story_4_") && !q363.id.startsWith("story_5_")) continue;
            for (QuestDef.Objective ob363 : q363.objectives) {
                // 보스·중간 보스가 아닌 채우기용 처치는 많아야 4 (모드가 없을 때의 대신 목표는 세지 않는다: 그것은 읽을 때 걸러진다)
                if (ob363.type == QuestDef.Type.KILL && ob363.count > 4 && ob363.target.indexOf(':') > 0 && !ob363.target.startsWith("minecraft:")) {
                    big363++;
                    bigNames363.append(q363.id).append(' ').append(ob363.target).append('x').append(ob363.count).append(' ');
                }
            }
        }
        boolean scenes363 = q107 != null && q113 != null && q107.companion == null && q113.companion == null
                && q107.objectives.stream().anyMatch(o -> o.type == QuestDef.Type.INSPECT && o.target.equals("c1_shade") && o.after.equals("c1_pyramid"))
                && q113.objectives.stream().anyMatch(o -> o.type == QuestDef.Type.INSPECT && o.target.equals("c1_sill") && o.after.equals("c1_ink"))
                && l363.quests.get("story_1_02").companion != null && l363.quests.get("story_1_37").companion != null;
        boolean mod363 = (q207 == null || q207.objectives.stream().noneMatch(o -> o.target.startsWith("mowziesmobs:"))
                || q207.objectives.stream().anyMatch(o -> o.target.equals("c2_trunks") && o.at.equals("biome:minecraft:dark_forest")))
                && (q407 == null || q407.objectives.stream().noneMatch(o -> o.target.startsWith("cataclysm:"))
                || q407.objectives.stream().anyMatch(o -> o.target.equals("c4_yoke") && o.after.equals("c4_basin")));
        check("3.63 story: Isabel stays behind for the pyramid and the swamp hut (she still walks to the portal and the tower), both quests end on a quiet scene, the dark wood and the cursed pyramid have theirs when their mods are in, and no filler kill in chapters 2 to 5 asks for more than 4",
                scenes363 && mod363 && big363 == 0, scenes363 + " " + mod363 + " " + big363 + " " + bigNames363);
        for (String f : List.of("story_v4", "story_v5", "story_v6", "story_v7", "story_v8")) QuestManager.setFlag(a, f, true);
        QuestManager.forceComplete(a, "story_1_06");
        boolean took363 = QuestManager.accept(a, "story_1_07", "quest_order_archivist");
        int full363 = QuestManager.progressOf(a, "story_1_07") == null ? -1 : QuestManager.progressOf(a, "story_1_07").length;
        if (took363) QuestManager.progressOf(a, "story_1_07")[0] = 1;
        QuestManager.shortenForTest(a, "story_1_07", 2);
        int short363 = QuestManager.progressOf(a, "story_1_07") == null ? -1 : QuestManager.progressOf(a, "story_1_07").length;
        int fit363 = QuestManager.fitProgress(a);
        int[] pg363 = QuestManager.progressOf(a, "story_1_07");
        check("3.63 a quest that gained a goal: someone already on it gets a progress slot for the new goal (what was done is kept, the new goal starts at 0, nothing to fix the second time)",
                took363 && full363 == 3 && short363 == 2 && fit363 == 1 && pg363 != null && pg363.length == 3 && pg363[0] == 1 && pg363[2] == 0 && QuestManager.fitProgress(a) == 0,
                took363 + " " + full363 + " " + short363 + " " + fit363 + " " + (pg363 == null ? -1 : pg363.length));
        String biome363 = a.level().getBiome(a.blockPosition()).unwrapKey().map(k -> k.location().toString()).orElse("");
        check("3.63 a trace can be tied to a biome: it appears only while the player stands in that biome",
                !biome363.isEmpty() && QuestManager.atStructure(a, "biome:" + biome363) && !QuestManager.atStructure(a, "biome:rtshop:no_such_biome") && !QuestManager.atStructure(a, "biome:"),
                biome363);
        QuestManager.reset(a);
    }

    private static String altarKeep = null;

    private static void magicGacha(MinecraftServer server, ServerLevel level, FakePlayer a) throws Exception {
        // 룬 제단은 기본으로 1장을 끝내야 열린다: 뽑기 점검 동안은 잠금을 풀어 둔다 (잠금 자체는 storySystems 에서 본다)
        com.rtshop.story.StorySystems.ensure();
        altarKeep = com.rtshop.story.StorySystems.altarLockFlag;
        com.rtshop.story.StorySystems.altarLockFlag = "";
        section("Magic gacha");
        com.rtshop.magic.MagicConfig real = com.rtshop.magic.MagicConfig.get();
        int realBooks = 0;
        for (String cat : com.rtshop.magic.MagicConfig.CATS) realBooks += real.pool(cat).size();
        boolean ef = net.minecraftforge.fml.ModList.get().isLoaded("epicfight");
        LINES.add("  magic pools: " + realBooks + " books, " + real.enchants.size() + " enchants"
                + (ef ? " (epicfight: " + com.rtshop.magic.MagicSkills.discover().size() + " skills)" : ""));
        if (ef) {
            check("with Epic Fight installed the magic gacha fills itself with this pack's skill books", realBooks > 0, realBooks);
            boolean yamatoOk = true;
            for (String cat : com.rtshop.magic.MagicConfig.CATS) {
                if (cat.equals("attack")) continue;
                for (var e : real.pool(cat)) yamatoOk &= !e.grade.equals("yamato");
            }
            check("the yamato grade is only in the attack field", yamatoOk);
            for (String cat : com.rtshop.magic.MagicConfig.CATS) {
                StringBuilder sb = new StringBuilder();
                for (var e : real.pool(cat)) sb.append(e.nbt.replace("{skill:\"", "").replace("\"}", "")).append('=').append(e.grade).append(' ');
                LINES.add("  magic " + cat + ": " + sb);
            }
        }
        check("the enchant reroll list is filled from this world's enchantments (no curses)", !real.enchants.isEmpty()
                && real.enchants.stream().noneMatch(e -> e.enchantment() != null && e.enchantment().isCurse()), real.enchants.size());
        // 시험용 설정 (바닐라 아이템): 공격 분야에 야마토 1·일반 1, 방어에 희귀 1, 회피·패시브·이동에 일반 1
        com.rtshop.magic.MagicConfig t = new com.rtshop.magic.MagicConfig();
        t.defaults();
        java.util.function.BiFunction<String, String, com.rtshop.gacha.GachaConfig.Entry> mk = (item, grade) -> {
            com.rtshop.gacha.GachaConfig.Entry e = new com.rtshop.gacha.GachaConfig.Entry();
            e.item = item;
            e.grade = grade;
            e.weight = 10;
            return e;
        };
        t.pool("attack").add(mk.apply("minecraft:nether_star", "yamato"));
        t.pool("attack").add(mk.apply("minecraft:book", "uncommon"));
        t.pool("guard").add(mk.apply("minecraft:shield", "rare"));
        t.pool("guard").add(mk.apply("minecraft:paper", "yamato")); // 공격 밖의 야마토는 전설로 내려가야 한다
        t.pool("dodge").add(mk.apply("minecraft:feather", "uncommon"));
        t.pool("passive").add(mk.apply("minecraft:apple", "uncommon"));
        t.pool("mover").add(mk.apply("minecraft:ender_pearl", "uncommon"));
        t.pool("mover").add(mk.apply("minecraft:stick", "common")); // 일반 등급은 '실패' 자리라 고급으로 올라가야 한다
        t.autoEnchants();
        t.normalize();
        t.pityOn = false; // 천장은 아래에서 따로 확인
        check("a yamato prize outside the attack field is moved down to legendary", t.pool("guard").get(1).grade.equals("legendary"));
        check("the common grade is the 'fail' place: a prize listed as common is lifted to uncommon", t.pool("mover").get(1).grade.equals("uncommon"));
        check("default odds start with a fail chance (books 40%, enchant 50%) and the enchant list reaches legendary",
                t.selectOdds[0] == 40 && t.randomOdds[0] == 40 && t.enchantOdds[0] == 50 && t.enchantOdds[4] > 0
                        && t.enchants.stream().anyMatch(e -> e.grade.equals("legendary")) && t.enchants.stream().noneMatch(e -> e.grade.equals("common")),
                t.enchants.stream().filter(e -> e.grade.equals("legendary")).count());
        check("armour and tools can be laid on the altar too, and their enchantments are in the list",
                t.rerollKinds.contains("armor") && t.rerollKinds.contains("tool")
                        && com.rtshop.magic.MagicGacha.rerollable(t, new ItemStack(Items.DIAMOND_CHESTPLATE))
                        && com.rtshop.magic.MagicGacha.rerollable(t, new ItemStack(Items.DIAMOND_PICKAXE))
                        && !com.rtshop.magic.MagicGacha.rerollable(t, new ItemStack(Items.DIRT)));
        {
            // 옛 설정(1)을 읽으면: 확률표는 새 기본값, 인챈트 등급은 한 칸씩 위로
            com.rtshop.magic.MagicConfig old = new com.rtshop.magic.MagicConfig();
            old.defaults();
            old.version = 1;
            com.rtshop.magic.MagicConfig.Ench c0 = new com.rtshop.magic.MagicConfig.Ench(), c3 = new com.rtshop.magic.MagicConfig.Ench();
            c0.id = "minecraft:sharpness";
            c0.grade = "common";
            c3.id = "minecraft:looting";
            c3.level = 3;
            c3.grade = "epic";
            old.enchants.add(c0);
            old.enchants.add(c3);
            old.selectOdds[0] = 0;
            com.google.gson.JsonObject oj = old.toJson();
            oj.addProperty("configVersion", 1);
            com.rtshop.magic.MagicConfig moved = com.rtshop.magic.MagicConfig.fromJson(oj);
            moved.migrate();
            moved.normalize();
            check("an old magic gacha file is moved to the new rules (fail column, enchant grades one step up to legendary)",
                    moved.version == 2 && moved.selectOdds[0] == 40 && moved.enchants.get(0).grade.equals("uncommon")
                            && moved.enchants.get(1).grade.equals("legendary"),
                    moved.enchants.get(0).grade + "/" + moved.enchants.get(1).grade);
        }
        // 아래 점검은 결과가 정해져 있어야 하므로 실패 확률을 0 으로 두고, 실패는 따로 확인한다
        t.selectOdds[0] = 0;
        t.randomOdds[0] = 0;
        t.enchantOdds[0] = 0;
        com.rtshop.magic.MagicConfig.set(t);
        try {
            a.getInventory().clearContent();
            a.getInventory().add(new ItemStack(com.rtshop.magic.MagicRegistry.TICKET_SELECT.get(), 10));
            a.getInventory().add(new ItemStack(com.rtshop.magic.MagicRegistry.TICKET.get(), 1));
            check("the 10× draw is gone: asking for it is refused and uses no ticket", com.rtshop.magic.MagicGacha.pull(a, "select", "attack", 10, false) == null
                    && a.getInventory().countItem(com.rtshop.magic.MagicRegistry.TICKET_SELECT.get()) == 10);
            var got = com.rtshop.magic.MagicGacha.pull(a, "select", "attack", 1, false);
            check("a chosen-field draw gives one prize from the chosen field", got != null && got.size() == 1
                    && got.stream().allMatch(e -> t.pool("attack").contains(e)), got == null ? "null" : got.size());
            check("the draw used one chosen-field ticket", a.getInventory().countItem(com.rtshop.magic.MagicRegistry.TICKET_SELECT.get()) == 9);
            int marked = 0;
            for (ItemStack st : a.getInventory().items) if (com.rtshop.gacha.Gacha.marked(st)) marked++;
            check("drawn prizes carry the gacha mark (can be dismantled for shards)", marked > 0, marked);
            a.getInventory().clearOrCountMatchingItems(st -> st.is(com.rtshop.magic.MagicRegistry.TICKET_SELECT.get()), -1, a.inventoryMenu.getCraftSlots());
            var none = com.rtshop.magic.MagicGacha.pull(a, "select", "guard", 1, false);
            check("without a ticket the draw is refused", none == null);
            var rnd = com.rtshop.magic.MagicGacha.pull(a, "random", "", 1, false);
            check("a random draw uses the cheap ticket and can give any field", rnd != null && rnd.size() == 1
                    && a.getInventory().countItem(com.rtshop.magic.MagicRegistry.TICKET.get()) == 0);
            // 등급 확률: 공격 분야 선택 뽑기의 야마토 확률이 0 보다 크고, 방어 분야에는 야마토가 없다
            var g = t.engine();
            double[] att = com.rtshop.gacha.Gacha.gradeChances(g, "attack", "select");
            double[] gua = com.rtshop.gacha.Gacha.gradeChances(g, "guard", "select");
            check("the attack field has the yamato grade, other fields do not", att[5] > 0 && gua[5] == 0, att[5] + " / " + gua[5]);
            // 실패: 첫 칸의 확률만큼 아무것도 나오지 않는다 (티켓은 쓰인다)
            double[] keepSel = t.selectOdds.clone();
            t.selectOdds[0] = 40;
            double[] withFail = com.rtshop.gacha.Gacha.gradeChances(t.engine(), "dodge", "select");
            check("the window's odds show the fail chance first (40% at the default)", Math.abs(withFail[0] - 40 / (40 + 41.98)) < 1e-6, withFail[0]);
            t.selectOdds[0] = 1e12;
            a.getInventory().clearContent();
            a.getInventory().add(new ItemStack(com.rtshop.magic.MagicRegistry.TICKET_SELECT.get(), 3));
            var failed = com.rtshop.magic.MagicGacha.pull(a, "select", "dodge", 1, false);
            int things = 0;
            for (ItemStack st : a.getInventory().items) if (!st.isEmpty()) things++;
            check("a failed skill-book draw takes the ticket and gives nothing", failed != null && failed.size() == 1
                    && failed.get(0) == com.rtshop.magic.MagicConfig.FAIL && things == 1
                    && a.getInventory().countItem(com.rtshop.magic.MagicRegistry.TICKET_SELECT.get()) == 2, things);
            System.arraycopy(keepSel, 0, t.selectOdds, 0, keepSel.length);
            com.rtshop.magic.MagicConfig empty = new com.rtshop.magic.MagicConfig();
            empty.defaults();
            double sumEmpty = 0;
            for (double d : com.rtshop.gacha.Gacha.gradeChances(empty.engine(), "attack", "select")) sumEmpty += d;
            check("a field with no books has no fail entry either (it is simply empty)", sumEmpty == 0, sumEmpty);
            // 인챈트 재설정: 제단 위에 올려 둔 장비에 한다
            a.getInventory().clearContent();
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.enchant(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS, 3);
            sword.enchant(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING, 2);
            a.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, sword.copy());
            a.getInventory().add(new ItemStack(com.rtshop.magic.MagicRegistry.SCROLL.get(), 3));
            check("holding a weapon is not enough any more: with nothing on the altar the reroll is refused",
                    com.rtshop.magic.MagicGacha.reroll(a, false) == null && a.getInventory().countItem(com.rtshop.magic.MagicRegistry.SCROLL.get()) == 3);
            a.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            com.rtshop.magic.MagicMenu menu = new com.rtshop.magic.MagicMenu(99, a.getInventory(), null);
            check("the altar menu has one altar slot in front of the 36 bag slots, and it takes gear but not dirt",
                    menu.slots.size() == 37 && menu.getSlot(com.rtshop.magic.MagicMenu.ALTAR).mayPlace(sword)
                            && !menu.getSlot(com.rtshop.magic.MagicMenu.ALTAR).mayPlace(new ItemStack(Items.DIRT))
                            && menu.getSlot(com.rtshop.magic.MagicMenu.slotOfInventory(0)).getContainerSlot() == 0
                            && menu.getSlot(com.rtshop.magic.MagicMenu.slotOfInventory(9)).getContainerSlot() == 9
                            && menu.getSlot(com.rtshop.magic.MagicMenu.slotOfInventory(35)).getContainerSlot() == 35, menu.slots.size());
            a.getInventory().setItem(5, sword);
            a.containerMenu = menu;
            menu.quickMoveStack(a, com.rtshop.magic.MagicMenu.slotOfInventory(5));
            check("shift-clicking gear in the bag lays it on the altar", menu.altarItem().is(Items.DIAMOND_SWORD) && a.getInventory().getItem(5).isEmpty());
            check("the window data follows what lies on the altar", com.rtshop.magic.MagicGacha.view(a).get("heldOk").getAsBoolean()
                    && com.rtshop.magic.MagicGacha.view(a).get("held").getAsString().equals("minecraft:diamond_sword"));
            var e1 = com.rtshop.magic.MagicGacha.reroll(a, false);
            var ench = net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(menu.altarItem());
            check("enchant reroll leaves exactly one enchantment that fits the gear on the altar", e1 != null && e1 != com.rtshop.magic.MagicConfig.FAIL_ENCH
                    && ench.size() == 1 && ench.keySet().iterator().next().canEnchant(menu.altarItem()), ench);
            check("enchant reroll used one scroll", a.getInventory().countItem(com.rtshop.magic.MagicRegistry.SCROLL.get()) == 2);
            t.enchantOdds[0] = 1e12;
            var beforeFail = net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(menu.altarItem());
            var e2 = com.rtshop.magic.MagicGacha.reroll(a, false);
            check("a failed reroll takes the scroll and leaves the gear as it was", e2 == com.rtshop.magic.MagicConfig.FAIL_ENCH
                    && net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(menu.altarItem()).equals(beforeFail)
                    && a.getInventory().countItem(com.rtshop.magic.MagicRegistry.SCROLL.get()) == 1);
            t.enchantOdds[0] = 0;
            menu.quickMoveStack(a, com.rtshop.magic.MagicMenu.ALTAR);
            check("shift-clicking the altar slot takes the gear back into the bag", menu.altarItem().isEmpty() && a.getInventory().countItem(Items.DIAMOND_SWORD) == 1);
            int swordAt = -1;
            for (int i = 0; i < 36; i++) if (a.getInventory().getItem(i).is(Items.DIAMOND_SWORD)) swordAt = i;
            if (swordAt >= 0) menu.quickMoveStack(a, com.rtshop.magic.MagicMenu.slotOfInventory(swordAt));
            boolean laid = menu.altarItem().is(Items.DIAMOND_SWORD);
            menu.removed(a);
            a.containerMenu = a.inventoryMenu;
            check("closing the altar gives back what was left on it", laid && a.getInventory().countItem(Items.DIAMOND_SWORD) == 1,
                    laid + "/" + a.getInventory().countItem(Items.DIAMOND_SWORD));
            check("enchant reroll refuses things that are not gear", com.rtshop.magic.MagicGacha.reroll(a, new ItemStack(Items.DIRT), false) == null
                    && a.getInventory().countItem(com.rtshop.magic.MagicRegistry.SCROLL.get()) == 1);
            // 저장·불러오기 그대로
            var back = com.rtshop.magic.MagicConfig.fromJson(t.toJson());
            check("the magic gacha config survives save and load", back.pool("attack").size() == 2 && back.enchants.size() == t.enchants.size()
                    && back.selectOdds[5] == t.selectOdds[5]);
            // ---- 천장(확정)은 없앴다 (3.49): 몇 번을 뽑아도 세지 않고, 옛 설정을 켜 두어도 확정으로 바뀌지 않는다
            String SEL = "select";
            t.pityOn = true;
            t.pityCount = 2;
            t.pityGrade = "rare";
            com.rtshop.magic.MagicGacha.setPity(a, t, SEL, 0);
            a.getInventory().clearContent();
            a.getInventory().add(new ItemStack(com.rtshop.magic.MagicRegistry.TICKET_SELECT.get(), 20));
            for (int i = 0; i < 6; i++) com.rtshop.magic.MagicGacha.pull(a, SEL, "passive", 1, false);
            check("there is no pity any more: draws are not counted and nothing is turned into a guaranteed grade",
                    com.rtshop.magic.MagicGacha.pityOf(a, t, SEL) == 0 && !com.rtshop.magic.MagicGacha.view(a).has("pity") && !t.toJson().has("pity")
                            && !com.rtshop.magic.MagicConfig.fromJson(t.toJson()).pityOn, com.rtshop.magic.MagicGacha.pityOf(a, t, SEL));
            t.pityOn = false;
            // ---- '오늘의 룬' 은 없앴다: 어느 분야든 값과 확률이 같고, 창 내용에도 실리지 않는다
            t.selectCost = 5;
            check("today's rune is gone: every field costs the same and the window data carries no blessing",
                    t.cost(SEL) == 5 && t.cost("random") == 1 && !com.rtshop.magic.MagicGacha.view(a).has("blessing")
                            && !t.toJson().has("blessing"));
            t.selectCost = 1;
            var vw = com.rtshop.magic.MagicGacha.view(a);
            check("the window data carries one-ticket prices only (no 10x, no pity gauge)", !vw.has("pity")
                    && vw.getAsJsonObject("cats").getAsJsonObject("attack").get("cost1").getAsInt() == 1
                    && !vw.getAsJsonObject("cats").getAsJsonObject("attack").has("cost10") && !vw.has("tenGuarantee"));
        } finally {
            com.rtshop.magic.MagicConfig.set(real);
            a.getInventory().clearContent();
        }
    }

    /** 조사대 나침반·/locate 는 실제로 생긴 건축물만, 마을에서 1000블럭 이상 / 장비 능력치 설정 / 이야기 물건 안내 / 대화 먼저. */
    private static void findersAndStats(MinecraftServer server, ServerLevel level, FakePlayer a) throws Exception {
        section("Survey compass / locate / equipment stats");
        ServerLevel ow = server.overworld();
        BlockPos spawn = ow.getSharedSpawnPos();
        // 나침반: 이 시험 세계(평지)에 있는 마을로 확인 — 실제로 생긴 곳만, 마을(성 영역/스폰)에서 1000블럭 이상
        long t0 = System.currentTimeMillis();
        var villages = com.rtshop.story.StructureFinder.resolve(ow, "#minecraft:village");
        var f = com.rtshop.story.StructureFinder.find(ow, villages, spawn, 200, 1000);
        long ms = System.currentTimeMillis() - t0;
        LINES.add("  compass search: checked=" + com.rtshop.story.StructureFinder.lastChecked + " rejected=" + com.rtshop.story.StructureFinder.lastRejected
                + " townSkipped=" + com.rtshop.story.StructureFinder.lastTownSkipped + " ms=" + ms + " at=" + (f == null ? "none" : f.pos.toShortString())
                + " " + com.rtshop.story.StructureFinder.lastInfo);
        String compassTarget = "#minecraft:village";
        if (f == null) {
            // 모드팩 평지 세계에는 마을이 생기지 않을 수 있다 (마을 모드가 바닐라 마을을 바꿔 놓음): 무너진 차원문으로 확인
            compassTarget = "#minecraft:ruined_portal";
            f = com.rtshop.story.StructureFinder.find(ow, com.rtshop.story.StructureFinder.resolve(ow, compassTarget), spawn, 200, 1000);
            LINES.add("  compass search (ruined portal): at=" + (f == null ? "none" : f.pos.toShortString()) + " " + com.rtshop.story.StructureFinder.lastInfo);
        }
        check("the survey compass finds a structure that really generated (its start exists in the chunk)",
                f != null && com.rtshop.story.StructureFinder.verify(ow, f.structure, f.cx, f.cz) != null,
                f == null ? "none" : f.pos.toShortString());
        check("the place it points to is at least 1000 blocks from town",
                f != null && com.rtshop.story.StructureFinder.townDistance(ow, f.pos.getX(), f.pos.getZ()) >= 1000,
                f == null ? "none" : String.valueOf((int) com.rtshop.story.StructureFinder.townDistance(ow, f.pos.getX(), f.pos.getZ())));
        check("the compass search stays quick (under 6 s)", ms < 6000, ms + " ms");
        BlockPos viaItem = com.rtshop.item.SurveyCompassItem.locate(ow, compassTarget, spawn);
        check("the compass item uses the same verified search", viaItem != null && f != null && viaItem.equals(f.pos));
        // 이 세계에 생길 수 없는 건축물(평지의 무너진 차원문)은 절대 가리키지 않는다
        var portals = com.rtshop.story.StructureFinder.resolve(ow, "#minecraft:ruined_portal");
        var none = com.rtshop.story.StructureFinder.find(ow, portals, spawn, 200, 1000);
        LINES.add("  ruined portal search: at=" + (none == null ? "none" : none.pos.toShortString()) + " " + com.rtshop.story.StructureFinder.lastInfo);
        check("a structure that cannot generate in this world is never pointed to (or, if found, it is really there)",
                none == null || com.rtshop.story.StructureFinder.verify(ow, none.structure, none.cx, none.cz) != null);
        var v = com.rtshop.story.StructureFinder.find(ow, com.rtshop.story.StructureFinder.resolve(ow, compassTarget), spawn, 100, 0);
        check("/locate structure (verified) finds a real one", v != null
                && com.rtshop.story.StructureFinder.verify(ow, v.structure, v.cx, v.cz) != null);
        check("an unknown structure target resolves to nothing", com.rtshop.story.StructureFinder.resolve(ow, "rtshop:no_such_structure") == null);
        // /locate structure 명령이 우리 검색으로 바뀌었는가 (결과 문장)
        List<String> out = new ArrayList<>();
        net.minecraft.commands.CommandSource sink = new net.minecraft.commands.CommandSource() {
            public void sendSystemMessage(net.minecraft.network.chat.Component c) {
                out.add(c.getString());
            }

            public boolean acceptsSuccess() {
                return true;
            }

            public boolean acceptsFailure() {
                return true;
            }

            public boolean shouldInformAdmins() {
                return false;
            }
        };
        net.minecraft.commands.CommandSourceStack css = new net.minecraft.commands.CommandSourceStack(sink, net.minecraft.world.phys.Vec3.atCenterOf(spawn),
                net.minecraft.world.phys.Vec2.ZERO, ow, 4, "selftest", net.minecraft.network.chat.Component.literal("selftest"), server, null);
        server.getCommands().performPrefixedCommand(css, "locate structure " + compassTarget);
        LINES.add("  /locate output: " + out);
        check("/locate structure answers with a verified place", !out.isEmpty() && com.rtshop.story.StructureFinder.lastChecked > 0 && !out.get(0).contains("Could not"), out);

        // 장비 능력치 설정
        String sword = "minecraft:diamond_sword";
        Map<String, Map<String, Double>> keep = new java.util.LinkedHashMap<>(com.rtshop.stats.ItemStats.table(false));
        try {
            Map<String, Double> m = new java.util.LinkedHashMap<>();
            m.put("minecraft:generic.attack_damage", 12.0);
            m.put("minecraft:generic.attack_speed", 2.0);
            com.rtshop.stats.ItemStats.put(sword, m);
            ItemStack st = new ItemStack(Items.DIAMOND_SWORD);
            var mods = st.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND);
            double dmg = mods.get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).stream().mapToDouble(x -> x.getAmount()).sum();
            double spd = mods.get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED).stream().mapToDouble(x -> x.getAmount()).sum();
            check("equipment stats: a diamond sword set to damage 12 / speed 2 shows exactly that", Math.abs(dmg - 11) < 1e-6 && Math.abs(spd + 2) < 1e-6,
                    "dmg+1=" + (dmg + 1) + " spd+4=" + (spd + 4));
            Map<String, Double> arm = new java.util.LinkedHashMap<>();
            arm.put("minecraft:generic.armor", 11.0);
            com.rtshop.stats.ItemStats.put("minecraft:iron_chestplate", arm);
            ItemStack chest = new ItemStack(Items.IRON_CHESTPLATE);
            double armor = chest.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.CHEST)
                    .get(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).stream().mapToDouble(x -> x.getAmount()).sum();
            double handArmor = chest.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND)
                    .get(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).stream().mapToDouble(x -> x.getAmount()).sum();
            check("equipment stats: an iron chestplate set to armor 11 gives 11 when worn and nothing in the hand", Math.abs(armor - 11) < 1e-6 && handArmor == 0,
                    armor + " / " + handArmor);
            com.rtshop.stats.ItemStats.put(sword, null);
            double back = new ItemStack(Items.DIAMOND_SWORD).getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND)
                    .get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).stream().mapToDouble(x -> x.getAmount()).sum();
            check("equipment stats: clearing an item gives back its original stats (diamond sword 7)", Math.abs(back + 1 - 7) < 1e-6, back + 1);
            var json = com.rtshop.stats.ItemStats.toJson();
            check("equipment stats are written to item_stats.json with a help line", json.has("_help") && json.getAsJsonObject("items").has("minecraft:iron_chestplate"));
        } finally {
            for (String k : new ArrayList<>(com.rtshop.stats.ItemStats.table(false).keySet())) com.rtshop.stats.ItemStats.put(k, null);
            keep.forEach(com.rtshop.stats.ItemStats::put);
        }

        // 이야기 물건 안내 문구
        int guides = 0;
        String[] storyItems = {"undying_ember", "chronicle_page", "frost_heart", "oath_scroll", "mother_journal", "grey_coin", "old_expedition_map",
                "expedition_tag", "broken_sun_mask", "crew_name_tag", "frozen_logbook", "grey_ash", "memory_shard", "chronicle_last_page"};
        try (var in = SelfTest.class.getResourceAsStream("/assets/rtshop/lang/ko_kr.json")) {
            com.google.gson.JsonObject lang = com.google.gson.JsonParser.parseString(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            for (String id : storyItems) if (lang.has("item.rtshop." + id + ".guide")) guides++;
            check("every main story item has a guide line in its tooltip (" + storyItems.length + ")", guides == storyItems.length
                    && lang.has("rtshop.story_item.header") && lang.has("item.rtshop.scylla_clue.guide") && lang.has("item.rtshop.survey_compass.desc3"), guides);
        }
        List<net.minecraft.network.chat.Component> tip = new ArrayList<>();
        new ItemStack(com.rtshop.item.ModItems.FROST_HEART.get()).getItem().appendHoverText(new ItemStack(com.rtshop.item.ModItems.FROST_HEART.get()), null, tip,
                net.minecraft.world.item.TooltipFlag.NORMAL);
        check("a story item's tooltip shows the 'main story item' header and where it is used",
                tip.stream().anyMatch(c -> c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t && t.getKey().equals("item.rtshop.frost_heart.guide")));

        // 알림: 채팅 대신 알림 상자로 (누를 수 있는 글은 채팅으로)
        int sentBefore = com.rtshop.notice.Notice.sentForTest;
        com.rtshop.notice.Notice.send(a, net.minecraft.network.chat.Component.translatable("rtshop.castle.zone_denied"));
        check("mod notices go to the notice box instead of chat", com.rtshop.notice.Notice.sentForTest == sentBefore + 1);
        net.minecraft.network.chat.Component link = net.minecraft.network.chat.Component.literal("x").withStyle(st -> st.withClickEvent(
                new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/rtshop quest")));
        check("a notice with a clickable link stays in chat (it can only be clicked there)",
                com.rtshop.notice.Notice.hasClick(net.minecraft.network.chat.Component.literal("a ").append(link))
                        && !com.rtshop.notice.Notice.hasClick(net.minecraft.network.chat.Component.literal("plain")));

        // 마법 뽑기 조사: 이 세계의 스킬북(창작 탭에 있는 것)과 스킬 분류
        if (net.minecraftforge.fml.ModList.get().isLoaded("epicfight")) {
            try {
                net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(server.getWorldData().enabledFeatures(), true, server.registryAccess());
                java.util.Set<String> seen = new java.util.TreeSet<>();
                for (var tab : net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB) {
                    for (ItemStack st : tab.getDisplayItems()) {
                        ResourceLocation rid = ForgeRegistries.ITEMS.getKey(st.getItem());
                        if (rid == null || !(rid.getPath().contains("skill") || rid.getPath().contains("book") && st.hasTag())) continue;
                        String tag = st.hasTag() ? st.getTag().toString() : "";
                        if (!seen.add(rid + " " + tag)) continue;
                        String cat = "";
                        try {
                            String sk = st.hasTag() && st.getTag().contains("skill") ? st.getTag().getString("skill") : "";
                            if (!sk.isEmpty()) {
                                Class<?> sm = Class.forName("yesman.epicfight.api.data.reloader.SkillManager");
                                Object skill = null;
                                for (var m : sm.getMethods()) {
                                    if (m.getName().equals("getSkill") && m.getParameterCount() == 1 && m.getParameterTypes()[0] == String.class) skill = m.invoke(null, sk);
                                }
                                if (skill != null) cat = String.valueOf(skill.getClass().getMethod("getCategory").invoke(skill));
                            }
                        } catch (Throwable ignored) {
                            cat = "?";
                        }
                        LINES.add("  skillbook: " + rid + " | " + tag + " | " + st.getHoverName().getString() + " | cat=" + cat);
                    }
                }
                LINES.add("  skillbook total: " + seen.size());
            } catch (Throwable t) {
                LINES.add("  skillbook dump failed: " + t);
            }
        }

        // 꿈속의 기사: 퀘스트를 끝낼 때가 아니라 침대에서 잠들 때 나온다
        var dream = com.rtshop.quest.QuestManager.loaded().quests.get("story_0_03");
        com.google.gson.JsonObject dj = dream == null ? null : dream.sceneDone;
        while (com.rtshop.quest.QuestManager.showDream(a)) {
        }
        if (dj != null) com.rtshop.quest.QuestManager.scene(a, dj);
        if (dj != null) com.rtshop.quest.QuestManager.scene(a, dj); // 같은 꿈은 두 번 쌓이지 않는다
        var dream2 = com.rtshop.quest.QuestManager.loaded().quests.get("story_4_29");
        if (dream2 != null && dream2.sceneDream != null) com.rtshop.quest.QuestManager.queueDream(a, dream2.sceneDream);
        a.getPersistentData().putString(com.rtshop.quest.QuestManager.SLEEP_SCENE, "{\"kind\":\"vision\",\"text\":\"old\"}"); // 옛 자리의 꿈도 옮겨진다
        int waiting = com.rtshop.quest.QuestManager.dreamsWaiting(a);
        boolean survives = a.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG).contains(com.rtshop.quest.QuestManager.DREAMS)
                && !a.getPersistentData().contains(com.rtshop.quest.QuestManager.SLEEP_SCENE);
        boolean one = com.rtshop.quest.QuestManager.showDream(a) && com.rtshop.quest.QuestManager.dreamsWaiting(a) == waiting - 1;
        while (com.rtshop.quest.QuestManager.showDream(a)) {
        }
        long dreams = com.rtshop.quest.QuestManager.loaded().quests.values().stream().filter(x -> x.sceneDream != null || (x.sceneDone != null && x.sceneDone.has("onSleep"))).count();
        check("dream-knight scenes wait for a bed, pile up instead of replacing each other, survive death, come one a night, and run through chapters 3 and 4",
                dj != null && dj.has("onSleep") && waiting == 3 && survives && one && dreams >= 10 && com.rtshop.quest.QuestManager.dreamsWaiting(a) == 0,
                waiting + " " + survives + " " + one + " " + dreams);

        // 설정 파일 돌보기: 옛 마을 이름, 손대지 않은 기본 파일은 새 판으로
        try {
            boolean renamed = com.rtshop.util.ConfigNames.rename("엠버헤이븐을 떠나 골디아로, 엠버헤이븐은 Emberhaven").equals("베스탈리아를 떠나 베스탈리아로, 베스탈리아는 Vestalia")
                    && com.rtshop.util.ConfigNames.rename("그대로").equals("그대로");
            java.nio.file.Path td = java.nio.file.Files.createTempDirectory("rtshop_defaults_test");
            java.nio.file.Files.writeString(td.resolve("story.json"), "{\"quests\":[]}");
            java.nio.file.Files.writeString(td.resolve("npcs.json"), "{\"npcs\":[],\"mine\":true}");
            int first = QuestManager.refreshDefaults(td, null); // 지문 기록이 없던 설치: 한 번은 모두 새 판으로
            boolean storyNew = java.nio.file.Files.size(td.resolve("story.json")) > 10000 && QuestManager.UPDATED_DEFAULTS.contains("npcs.json");
            int again = QuestManager.refreshDefaults(td, null);
            java.nio.file.Files.writeString(td.resolve("story.json"), "{\"quests\":[],\"edited\":1}");
            int edited = QuestManager.refreshDefaults(td, null); // 손댄 파일은 그대로
            boolean kept = QuestManager.STALE_DEFAULTS.contains("story.json") && java.nio.file.Files.size(td.resolve("story.json")) < 100;
            int forced = QuestManager.refreshDefaults(td, "all");
            boolean done = QuestManager.STALE_DEFAULTS.isEmpty() && java.nio.file.Files.size(td.resolve("story.json")) > 10000;
            QuestManager.refreshDefaults(QuestManager.dir(), null); // 진짜 폴더 기준으로 목록을 되돌린다
            var cornelia = com.rtshop.story.StoryConfig.defaults().get("aquamirae:captain_cornelia");
            check("config care: old town names are rewritten, untouched default files move to the new edition (edited ones wait for the operator), boss lore matches the rewritten story",
                    renamed && first == 2 && storyNew && again == 0 && edited == 0 && kept && forced == 1 && done && cornelia != null
                            && com.rtshop.story.StoryConfig.defaults().containsKey("fdbosses:geburah"),
                    renamed + " " + first + " " + storyNew + " " + again + " " + edited + " " + kept + " " + forced + " " + done);
        } catch (Exception e) {
            check("config care: old town names are rewritten, untouched default files move to the new edition", false, e.toString());
        }

        // 직업 스승 다섯도 일상 이야기와 스토리 이야기를 한다
        int mentors = 0;
        for (String k : List.of("quest_sword_master", "quest_war_chief", "quest_shield_keeper", "quest_hunter", "quest_archmage")) {
            var n = com.rtshop.story.Dialogue.npcForTest(k);
            if (n != null && n.has("chatter") && n.getAsJsonArray("chatter").size() >= 4 && n.has("topics") && n.getAsJsonArray("topics").size() >= 3) mentors++;
        }
        check("the five job mentors (Leon, Bron, Garen, Ria, Elise) have small talk and story topics", mentors == 5, mentors);

        // 대화 먼저: 퀘스트 NPC 도 대화가 가로챈다 (퀘스트 창이 대화 상자를 덮지 않게)
        check("quest NPCs open the dialogue first (Dialogue claims the click)", com.rtshop.story.Dialogue.claims(a, "quest_order_archivist")
                && com.rtshop.story.Dialogue.claims(a, "quest_order_captain"));
    }

    private static void story(MinecraftServer server, ServerLevel level, FakePlayer a) throws Exception {
        {
            StringBuilder ids = new StringBuilder();
            for (net.minecraftforge.forgespi.language.IModInfo m : net.minecraftforge.fml.ModList.get().getMods())
                if ((m.getModId() + m.getDisplayName()).toLowerCase(java.util.Locale.ROOT).contains("fant")) ids.append(m.getModId()).append('=').append(m.getDisplayName()).append(' ');
            RtShop.LOGGER.info("[rtshop-selftest] Fantasy mods: {} -> {}", ids, com.rtshop.compat.FantasyQuestsOff.modId());
        }
        if (com.rtshop.compat.FantasyQuestsOff.modId() != null) {
            com.rtshop.compat.FantasyQuestsOff.apply(server, true);
            RtShop.LOGGER.info("[rtshop-selftest] Fantasy Mounts quest settings: {}", com.rtshop.compat.FantasyQuestsOff.LAST);
            check("Fantasy Mounts quests are off by default (its quest settings were found)", java.nio.file.Files.exists(com.rtshop.compat.FantasyQuestsOff.marker(server)),
                    com.rtshop.compat.FantasyQuestsOff.modId() + " " + com.rtshop.compat.FantasyQuestsOff.LAST);
        }
        if (net.minecraftforge.fml.ModList.get().isLoaded("idas")) {
            int n = com.rtshop.compat.IdasVanilla.apply(true);
            RtShop.LOGGER.info("[rtshop-selftest] IDAS vanilla structures: {} {}", n, com.rtshop.compat.IdasVanilla.LAST);
            check("IDAS no longer switches off vanilla structures the story needs (desert pyramid etc.)",
                    java.nio.file.Files.exists(com.rtshop.compat.IdasVanilla.marker()), n + " " + com.rtshop.compat.IdasVanilla.LAST);
            // 스토리에 필요한 바닐라 구조물을 실제로 찾을 수 있는가 (/locate 와 같은 방법, 시작점 둘레 64 청크)
            var sreg = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
            for (String sid : List.of("#minecraft:ruined_portal", "minecraft:desert_pyramid", "minecraft:ancient_city")) { // 맨션은 히든 보스 자리라 단계적으로 생긴다
                net.minecraft.core.HolderSet<net.minecraft.world.level.levelgen.structure.Structure> set;
                if (sid.startsWith("#")) {
                    set = sreg.getTag(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.STRUCTURE, new ResourceLocation(sid.substring(1)))).orElse(null);
                } else {
                    var h = sreg.getHolder(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE, new ResourceLocation(sid)));
                    set = h.isPresent() ? net.minecraft.core.HolderSet.direct(h.get()) : null;
                }
                long t0 = System.currentTimeMillis();
                var found = set == null ? null : level.getChunkSource().getGenerator().findNearestMapStructure(level, set, level.getSharedSpawnPos(), 64, false);
                RtShop.LOGGER.info("[rtshop-selftest] locate {}: {} ({} ms)", sid, found == null ? "none" : found.getFirst(), System.currentTimeMillis() - t0);
                check("the story's vanilla structure can be found in this world: " + sid, found != null, found == null ? "none" : found.getFirst());
            }
        }
        check("vanilla illagers never appear inside a woodland mansion (the hidden boss and raids are left alone)",
                com.rtshop.story.MansionIllagers.BLOCKED.contains("minecraft:vindicator") && com.rtshop.story.MansionIllagers.BLOCKED.contains("minecraft:evoker")
                        && !com.rtshop.story.MansionIllagers.BLOCKED.contains("illusioner:illusioner"));
        check("merchants and service NPCs open with a dialogue first, their own window being the first choice; story-only NPCs have none",
                "rtshop.talk.main.shop".equals(com.rtshop.story.Dialogue.mainAction("shop_farmer")) && "rtshop.talk.main.bank".equals(com.rtshop.story.Dialogue.mainAction("bank"))
                        && com.rtshop.story.Dialogue.mainAction("quest_order_archivist") == null);
        {
            var arch = com.rtshop.story.Dialogue.npcForTest("quest_order_archivist");
            var lucky = com.rtshop.story.Dialogue.npcForTest("gacha");
            check("long small talk can let slip hints toward the hidden bosses; the lucky merchant calls himself Lucky",
                    arch != null && arch.has("hints") && arch.getAsJsonArray("hints").size() >= 2
                            && lucky != null && lucky.toString().contains("럭키") && !lucky.toString().contains("릭키"));
        }
        check("world-generation log spam (empty height range, unknown structure entities) is filtered, other warnings are not",
                com.rtshop.compat.LogNoise.noisy("Empty height range: {}") && com.rtshop.compat.LogNoise.noisy("Skipping Entity with id {}")
                        && !com.rtshop.compat.LogNoise.noisy("Can't keep up! Is the server overloaded?"));
        {
            // 공허 추락 방지: 공허 아래로 떨어지면 마지막으로 딛은 땅으로 돌아온다 (기억의 문·환술사의 공허 차원)
            var mem = server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, new ResourceLocation("rtshop:memory")));
            boolean ok = mem != null && com.rtshop.story.VoidGuard.guarded(mem) && !com.rtshop.story.VoidGuard.DIMS.contains("illusioner:corrupted_void");
            {
                a.setPos(0.5, 100, 0.5);
                a.setPos(0.5, level.getMinBuildHeight() - 10, 0.5);
                com.rtshop.story.VoidGuard.rescue(a);
                ok &= a.getY() > level.getMinBuildHeight();
                a.setPos(0.5, 100, 0.5);
            }
            check("falling into the void in the Memory Gate brings you back to solid ground (the Illusioner's own void is left alone)", ok);
        }
        // 단계적 이야기 건축물: 풀리기 전에는 새 지형에 생기지 않는다
        {
            com.rtshop.story.StagedStructures.reload();
            com.rtshop.story.StagedStructures.resetForTest(server);
            boolean lockedAtStart = com.rtshop.story.StagedStructures.blocks("minecraft:mansion") && com.rtshop.story.StagedStructures.blocks("cataclysm:burning_arena")
                    && !com.rtshop.story.StagedStructures.blocks("minecraft:village_plains");
            long asked0 = com.rtshop.story.StagedStructures.ASKED.get();
            var nether = server.getLevel(net.minecraft.world.level.Level.NETHER);
            if (nether != null) {
                // 네더 요새·보루 자리 후보 청크를 몇 개 만들어 본다 (그 자리에서만 건축물을 물어본다)
                var state = nether.getChunkSource().getGeneratorState();
                var sets = nether.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE_SET);
                var nc = sets.getHolder(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE_SET, new ResourceLocation("minecraft:nether_complexes")));
                if (nc.isPresent() && nc.get().value().placement() instanceof net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement rsp) {
                    for (int i = 0; i < 3; i++) {
                        net.minecraft.world.level.ChunkPos cp = rsp.getPotentialStructureChunk(state.getLevelSeed(), 40 + i, 40);
                        nether.getChunk(cp.x, cp.z, net.minecraft.world.level.chunk.ChunkStatus.STRUCTURE_STARTS, true);
                    }
                }
            }
            long asked = com.rtshop.story.StagedStructures.ASKED.get() - asked0;
            check("staged story structures: locked at first, plain structures are not, and world generation really asks (mixin applied)",
                    lockedAtStart && asked > 0, lockedAtStart + " asked=" + asked);
            QuestManager.reset(a);
            // 모드 없이도 시험할 수 있게 이글루를 잠깐 단계적 건축물로 넣는다 (1장 10번 퀘스트가 이글루로 보낸다)
            com.rtshop.story.StagedStructures.get().structures.put("minecraft:igloo", "");
            com.rtshop.story.StagedStructures.recompute(server);
            for (String id : List.of("story_0_01", "story_0_02", "story_0_02b", "story_0_03", "story_1_01", "story_1_02", "story_1_03", "story_1_04", "story_1_05", "story_1_06", "story_1_07", "story_1_08", "story_1_09"))
                QuestManager.forceComplete(a, id);
            boolean took = com.rtshop.story.StagedStructures.isLocked("minecraft:igloo");
            boolean took3 = QuestManager.accept(a, "story_1_10", "quest_order_archivist");
            boolean chamberOpen = !com.rtshop.story.StagedStructures.isLocked("minecraft:igloo");
            com.rtshop.story.StagedStructures.get().structures.remove("minecraft:igloo");
            boolean othersStill = com.rtshop.story.StagedStructures.isLocked("aquamirae:ship") && com.rtshop.story.StagedStructures.isLocked("minecraft:mansion");
            check("taking the quest that leads to a story structure starts generating it for the whole server; later ones stay locked",
                    took3 && chamberOpen && othersStill, took + " " + took3 + " " + chamberOpen + " " + othersStill);
            com.rtshop.story.StagedStructures.resetForTest(server);
            QuestManager.reset(a);
        }
        // 기억의 문: 이야기 보스전 전용 공간
        {
            com.rtshop.story.StoryArena.reload();
            com.rtshop.story.StoryArena.testOverworld = true;
            com.rtshop.story.StoryArena.addTestPlayer(a);
            a.setPos(0.5, 120, 0.5);
            boolean noQuest = !com.rtshop.story.StoryArena.openGate(a, "minecraft:evoker", false);
            boolean mgOpened = com.rtshop.story.StoryArena.openGate(a, "minecraft:evoker", true);
            var mgGate = com.rtshop.story.StoryArena.gatesForTest().get(a.getUUID());
            com.rtshop.story.StoryArena.forceTick(server, 5);
            boolean mgStayed = mgGate != null && com.rtshop.story.StoryArena.sessionsForTest().isEmpty();
            check("pressing the Memory Gate button only opens a gate in front of the player (only with the quest, or OP force); nobody is moved yet",
                    noQuest && mgOpened && mgStayed, noQuest + " " + mgOpened + " " + mgStayed);
            if (mgGate != null) {
                mgGate.x = 0.5;
                mgGate.y = 120;
                mgGate.z = 0.5;
                a.setPos(6.5, 120, 0.5);
                com.rtshop.story.StoryArena.forceTick(server, 2);   // 문 밖에 한 번 서야 문이 켜진다
                a.setPos(0.5, 120, 0.5);
            }
            com.rtshop.story.StoryArena.forceTick(server, 2);
            boolean in = !com.rtshop.story.StoryArena.sessionsForTest().isEmpty();
            var s0 = com.rtshop.story.StoryArena.sessionsForTest().get(0);
            boolean moved = s0 != null && com.rtshop.story.StoryArena.inSlot(a, s0);
            com.rtshop.story.StoryArena.forceTick(server, 120);
            boolean mgAsleep = s0 != null && s0.state == com.rtshop.story.StoryArena.WAIT && com.rtshop.story.StoryArena.bossForTest(server, 0) == null;
            BlockPos mgC = com.rtshop.story.StoryArena.center(server, 0);
            boolean mgEmber = com.rtshop.story.StoryArena.level(server).getBlockState(mgC.above(2)).is(net.minecraft.world.level.block.Blocks.CAMPFIRE);
            boolean mgWoke = com.rtshop.story.StoryArena.wake(a) && !com.rtshop.story.StoryArena.wake(a);
            com.rtshop.story.StoryArena.forceTick(server, com.rtshop.story.StoryArena.WAKE_TICKS - 10);
            boolean mgStillWaking = com.rtshop.story.StoryArena.bossForTest(server, 0) == null;
            com.rtshop.story.StoryArena.forceTick(server, 20);
            net.minecraft.world.entity.LivingEntity boss = com.rtshop.story.StoryArena.bossForTest(server, 0);
            check("walking into the gate takes the player to a private arena; the boss sleeps there until the ember is touched, then wakes after a short scene",
                    in && moved && mgAsleep && mgEmber && mgWoke && mgStillWaking && boss != null && boss.isAlive()
                            && com.rtshop.story.StoryArena.level(server).getBlockState(mgC.above(2)).isAir() && com.rtshop.story.StoryArena.gatesForTest().isEmpty(),
                    in + " " + moved + " " + mgAsleep + " " + mgEmber + " " + mgWoke + " " + mgStillWaking + " " + boss + " gates " + com.rtshop.story.StoryArena.gatesForTest().size());
            check("party members come along only from within 16 blocks of the one who walks in (story_arenas.json: partyRange), and the wait is set there too",
                    com.rtshop.story.StoryArena.partyRange == 16 && com.rtshop.story.StoryArena.waitSeconds == 45 && com.rtshop.story.StoryArena.walkIn,
                    com.rtshop.story.StoryArena.partyRange + " " + com.rtshop.story.StoryArena.waitSeconds);
            if (boss != null) boss.kill();
            com.rtshop.story.StoryArena.forceTick(server, 2);
            boolean won = s0 != null && s0.state == com.rtshop.story.StoryArena.WON;
            com.rtshop.story.StoryArena.forceTick(server, 210);
            check("after the boss falls the player is sent back where they came from and the arena closes",
                    won && com.rtshop.story.StoryArena.sessionsForTest().isEmpty() && Math.abs(a.getX() - 0.5) < 1 && Math.abs(a.getY() - 120) < 1,
                    won + " " + com.rtshop.story.StoryArena.sessionsForTest().size() + " " + a.position());
            com.rtshop.story.StoryArena.clearForTest(server);
            com.rtshop.story.StoryArena.testOverworld = false;
            check("the Memory Gate dimension is installed (rtshop:memory)", server.getLevel(com.rtshop.story.StoryArena.DIM) != null);
        }
        com.rtshop.story.WildSpawns.reload();
        check("story mods spawn far less in the open (Mowzie's / Aquamirae keep 15%), normal in their own structures and story biomes",
                com.rtshop.story.WildSpawns.keepFor("mowziesmobs") < 0.5 && com.rtshop.story.WildSpawns.keepFor("aquamirae") < 0.5
                        && com.rtshop.story.WildSpawns.keepFor("minecraft") >= 1 && !com.rtshop.story.WildSpawns.storyArea(level, new BlockPos(0, 80, 0), "mowziesmobs"),
                com.rtshop.story.WildSpawns.get().keep);
        QuestManager.Loaded l = QuestManager.loaded();
        List<String> story = new ArrayList<>();
        for (String id : l.quests.keySet()) if (id.startsWith("story_") || id.startsWith("hidden_")) story.add(id);
        check("the main story (3rd edition: prologue, 4 chapters, finale + hidden story) is installed", story.size() >= 100 && l.quests.containsKey("story_5_08") && l.quests.containsKey("story_4_29") && l.quests.containsKey("story_1_29")
                && l.quests.containsKey("hidden_illusioner_2") == net.minecraftforge.fml.ModList.get().isLoaded("illusioner"), story.size());
        check("the story quests load without problems", l.problems.stream().noneMatch(x -> x.contains("story")), l.problems);
        check("the order's three NPCs exist", l.npcs.containsKey("order_elder") && l.npcs.containsKey("order_archivist") && l.npcs.containsKey("order_captain"));
        net.minecraft.core.Registry<net.minecraft.world.level.levelgen.structure.Structure> sreg =
                server.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        List<String> bad = new ArrayList<>();
        for (String id : story) {
            QuestDef q = l.quests.get(id);
            for (QuestDef.Objective ob : q.objectives) {
                String ns = ob.target.contains(":") ? ob.target.replace("#", "").substring(0, ob.target.replace("#", "").indexOf(':')) : "minecraft";
                if (!net.minecraftforge.fml.ModList.get().isLoaded(ns) && !ns.equals("minecraft")) continue;
                switch (ob.type) {
                    case KILL -> {
                        if (!ob.target.equals("*") && !ForgeRegistries.ENTITY_TYPES.containsKey(new ResourceLocation(ob.target))) bad.add(id + " kill " + ob.target);
                    }
                    case DELIVER -> {
                        if (!ob.target.startsWith("#") && !ForgeRegistries.ITEMS.containsKey(new ResourceLocation(ob.target))) bad.add(id + " item " + ob.target);
                    }
                    case STRUCTURE -> {
                        if (ob.target.startsWith("#")) {
                            if (sreg.getTag(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.STRUCTURE, new ResourceLocation(ob.target.substring(1)))).isEmpty())
                                bad.add(id + " structure tag " + ob.target);
                        } else if (!sreg.containsKey(new ResourceLocation(ob.target))) bad.add(id + " structure " + ob.target);
                    }
                    case TALK -> {
                        if (ob.target.startsWith("quest_") && !l.npcs.containsKey(ob.target.substring(6))) bad.add(id + " npc " + ob.target);
                    }
                    default -> {
                    }
                }
            }
            for (QuestDef.ItemReward ir : q.rewardItems) {
                String ns = ir.item.substring(0, ir.item.indexOf(':'));
                if ((ns.equals("minecraft") || net.minecraftforge.fml.ModList.get().isLoaded(ns)) && !ForgeRegistries.ITEMS.containsKey(new ResourceLocation(ir.item)))
                    bad.add(id + " reward " + ir.item);
            }
        }
        check("every story quest points at real monsters, structures, items and NPCs", bad.isEmpty(), bad);
        // 풀팩에서: 이야기가 부르는 모드가 모두 깔려 있고 (이름 틀림 = 조용히 건너뛰는 일 방지),
        // 발전 과제·차원·결계·단계적 생성·전용 경기장의 이름도 모두 진짜인지
        {
            var mods = net.minecraftforge.fml.ModList.get();
            boolean full = mods.isLoaded("mowziesmobs") && mods.isLoaded("cataclysm") && mods.isLoaded("aquamirae");
            List<String> wrong = new ArrayList<>();
            java.util.Set<String> namespaces = new java.util.TreeSet<>();
            for (String id : story) {
                QuestDef q = l.quests.get(id);
                for (QuestDef.Objective ob : q.objectives) {
                    String t = ob.target.replace("#", "");
                    if (t.contains(":") && (ob.type == QuestDef.Type.KILL || ob.type == QuestDef.Type.DELIVER || ob.type == QuestDef.Type.STRUCTURE
                            || ob.type == QuestDef.Type.ADVANCEMENT || ob.type == QuestDef.Type.DIMENSION)) {
                        namespaces.add(t.substring(0, t.indexOf(':')));
                    }
                    if (ob.type == QuestDef.Type.ADVANCEMENT && server.getAdvancements().getAdvancement(new ResourceLocation(ob.target)) == null
                            && (ob.target.startsWith("minecraft:") || mods.isLoaded(ob.target.substring(0, ob.target.indexOf(':'))))) {
                        wrong.add(id + " advancement " + ob.target);
                    }
                    if (ob.type == QuestDef.Type.DIMENSION && server.getLevel(net.minecraft.resources.ResourceKey.create(
                            net.minecraft.core.registries.Registries.DIMENSION, new ResourceLocation(ob.target))) == null
                            && (ob.target.startsWith("minecraft:") || mods.isLoaded(ob.target.substring(0, ob.target.indexOf(':'))))) {
                        wrong.add(id + " dimension " + ob.target);
                    }
                }
            }
            java.util.Set<String> structs = new java.util.TreeSet<>();
            for (com.rtshop.story.StorySeals.Seal se : com.rtshop.story.StorySeals.defaults().values()) structs.addAll(se.structures);
            structs.addAll(com.rtshop.story.StagedStructures.defaults().structures.keySet());
            for (String st : structs) {
                String ns = st.substring(0, st.indexOf(':'));
                namespaces.add(ns);
                if ((ns.equals("minecraft") || mods.isLoaded(ns)) && !sreg.containsKey(new ResourceLocation(st))) wrong.add("seal/staged structure " + st);
            }
            for (String e : com.rtshop.story.StoryArena.defaults().keySet()) {
                String ns = e.substring(0, e.indexOf(':'));
                namespaces.add(ns);
                if ((ns.equals("minecraft") || mods.isLoaded(ns)) && !ForgeRegistries.ENTITY_TYPES.containsKey(new ResourceLocation(e))) wrong.add("arena boss " + e);
            }
            List<String> missingMods = new ArrayList<>();
            for (String ns : namespaces) if (!ns.equals("minecraft") && !ns.equals("rtshop") && !mods.isLoaded(ns)) missingMods.add(ns);
            RtShop.LOGGER.info("[rtshop-selftest] story namespaces {} (not installed here: {})", namespaces, missingMods);
            check("advancements, dimensions, sealed/staged structures and arena bosses named by the story are real", wrong.isEmpty(), wrong);
            if (full) {
                check("full pack: every mod the story names is installed (no misspelled mod ids)", missingMods.isEmpty(), missingMods);
            }
        }
        // 스토리 언어: 영어를 고른 플레이어에게는 퀘스트·대화가 영어로, 한국어는 원문 그대로
        {
            com.rtshop.story.StoryText.setLang(a, "en_us");
            QuestDef q101 = l.quests.get("story_1_01");
            String en = com.rtshop.story.StoryText.t(a, q101.title);
            com.google.gson.JsonObject probe = new com.google.gson.JsonObject();
            probe.addProperty("id", "story_1_01");
            probe.addProperty("text", q101.intro);
            com.google.gson.JsonElement pj = com.rtshop.story.StoryText.json(a, probe);
            // 원문 문장 중 번역이 있는 비율
            int all = 0, has = 0;
            List<String> miss = new ArrayList<>();
            for (String id : story) {
                QuestDef q = l.quests.get(id);
                for (String s : new String[]{q.title, q.intro, q.progress, q.done}) {
                    if (s == null || !s.matches("(?s).*[가-힣].*")) continue;
                    all++;
                    if (!com.rtshop.story.StoryText.t(a, s).equals(s)) has++;
                    else if (miss.size() < 5) miss.add(s.length() > 30 ? s.substring(0, 30) : s);
                }
            }
            // 3.54: 모드에 든 퀘스트 파일의 모든 글(목표 이름 · 조사 글 · 대사 · 선택지 · 연출)에 영어판이 있다
            int deepAll = 0, deepHas = 0;
            List<String> deepMiss = new ArrayList<>();
            for (String file : new String[]{"story.json", "tutorial.json", "job_quests.json"}) {
                try (java.io.InputStream in = SelfTest.class.getResourceAsStream("/rtshop_defaults/quests/" + file)) {
                    com.google.gson.JsonObject root = com.google.gson.JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                    java.util.ArrayDeque<com.google.gson.JsonElement> todo = new java.util.ArrayDeque<>();
                    todo.add(root.get("quests"));
                    while (!todo.isEmpty()) {
                        com.google.gson.JsonElement e = todo.poll();
                        if (e.isJsonArray()) e.getAsJsonArray().forEach(todo::add);
                        else if (e.isJsonObject()) {
                            for (var en2 : e.getAsJsonObject().entrySet()) if (!en2.getKey().startsWith("_")) todo.add(en2.getValue());
                        } else if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) {
                            String t = e.getAsString();
                            if (!t.matches("(?s).*[가-힣].*")) continue;
                            deepAll++;
                            if (!com.rtshop.story.StoryText.t(a, t).equals(t)) deepHas++;
                            else if (deepMiss.size() < 5) deepMiss.add(file + ": " + (t.length() > 30 ? t.substring(0, 30) : t));
                        }
                    }
                } catch (Exception ex) {
                    deepMiss.add(file + ": " + ex);
                }
            }
            check("every text in the bundled quest files has an English version (" + deepHas + "/" + deepAll + ")", deepAll > 1500 && deepHas == deepAll, deepMiss);
            // 3.64: NPC 대사의 언어 — 두 언어를 나란히 적은 글({"ko","en"}: 직업 사범들 · 몇몇 이야기 주제)도 이야기 언어를 따른다.
            // 영어로 하는 사람(지금 a)에게는 어떤 NPC 의 인사 · 일상 이야기 · 주제에도 한글이 남지 않아야 한다.
            Object[] audEn364 = com.rtshop.story.Dialogue.languageAuditForTest(a);
            @SuppressWarnings("unchecked") List<String> leftKo364 = (List<String>) audEn364[1];
            com.google.gson.JsonObject master364 = com.rtshop.story.Dialogue.buildForTest(a, "quest_sword_master");
            String greetEn364 = master364.get("greet").getAsString(), noNpcEn364 = com.rtshop.story.Dialogue.buildForTest(a, "quest_no_such_npc_364").get("greet").getAsString();
            boolean langEn364 = "en".equals(com.rtshop.story.Loc.lang(a));
            com.rtshop.story.StoryText.setLang(a, "ko_kr");
            String greetKo364 = com.rtshop.story.Dialogue.buildForTest(a, "quest_sword_master").get("greet").getAsString();
            String noNpcKo364 = com.rtshop.story.Dialogue.buildForTest(a, "quest_no_such_npc_364").get("greet").getAsString();
            boolean langKo364 = "ko".equals(com.rtshop.story.Loc.lang(a));
            com.rtshop.story.Loc.set(a, "en");
            boolean oldCmd364 = com.rtshop.story.StoryText.en(a);
            com.rtshop.story.StoryText.setLang(a, "en_us");
            check("3.64 NPC talk follows the story language everywhere: an English player sees no Korean in any NPC's greeting, small talk or topic (the job masters and the two-language topics included), "
                            + "a Korean player gets the original, the fallback greeting is the player's language and not the server's, and the old language command sets the same setting",
                    (int) audEn364[0] > 400 && leftKo364.isEmpty() && langEn364 && langKo364 && oldCmd364
                            && !greetEn364.isEmpty() && greetEn364.chars().noneMatch(c364 -> c364 >= 0xAC00 && c364 <= 0xD7A3) && greetKo364.chars().anyMatch(c364 -> c364 >= 0xAC00 && c364 <= 0xD7A3)
                            && "What can I do for you?".equals(noNpcEn364) && "무슨 일이세요?".equals(noNpcKo364),
                    audEn364[0] + " lines, korean left: " + (leftKo364.size() > 6 ? leftKo364.subList(0, 6) + " … " + leftKo364.size() : leftKo364) + " / " + greetEn364 + " / " + greetKo364 + " / " + noNpcEn364 + " / " + noNpcKo364);
            com.rtshop.story.StoryText.setLang(a, "ko_kr");
            String ko = com.rtshop.story.StoryText.t(a, q101.title);
            check("story language: English players get the English story, Korean players the original (and ids stay untouched)",
                    "The Torn Chronicle".equals(en) && q101.title.equals(ko) && pj.getAsJsonObject().get("id").getAsString().equals("story_1_01")
                            && !pj.getAsJsonObject().get("text").getAsString().equals(q101.intro), en + " / " + ko);
            check("almost every story line has an English version (" + has + "/" + all + ")", all > 0 && has >= all * 0.97, miss);
            // A-25 2차: 설정 기본 문구(레이드·주간 보상·보스 소개·결계·스킬라 단서·직업·무기 종류·세계 규칙)의 영어판
            {
                java.util.List<String> cfgTexts4 = new java.util.ArrayList<>();
                cfgTexts4.addAll(com.rtshop.raid.RaidConfig.defaultTexts().values());
                cfgTexts4.addAll(com.rtshop.raid.AbyssWeekly.defaultTexts().values());
                for (com.rtshop.story.StoryConfig.Intro in4 : com.rtshop.story.StoryConfig.defaults().values()) {
                    cfgTexts4.add(in4.name);
                    cfgTexts4.add(in4.epithet);
                    cfgTexts4.add(in4.quote);
                    cfgTexts4.addAll(in4.lore);
                }
                for (com.rtshop.story.StorySeals.Seal se4 : com.rtshop.story.StorySeals.defaults().values()) cfgTexts4.add(se4.message);
                for (com.rtshop.story.ScyllaClues.Clue cl4 : com.rtshop.story.ScyllaClues.get().clues.values()) {
                    cfgTexts4.add(cl4.name);
                    cfgTexts4.add(cl4.desc);
                    for (java.util.List<String> hl4 : cl4.hints.values()) cfgTexts4.addAll(hl4);
                }
                for (String jid4 : new String[]{"swordsman", "warrior", "tank", "archer", "mage", "farmer", "fisher", "miner", "cook"}) {
                    com.rtshop.job.JobConfig.JobDef jd4 = com.rtshop.job.JobConfig.get().job(jid4);
                    if (jd4 != null) cfgTexts4.add(jd4.name);
                }
                for (com.rtshop.job.WeaponTypes.Type wt4 : com.rtshop.job.WeaponTypes.defaults()) cfgTexts4.add(wt4.name);
                cfgTexts4.add(com.rtshop.job.JobConfig.get().typeName(com.rtshop.job.WeaponTypes.MAGIC));
                cfgTexts4.add(com.rtshop.job.JobConfig.get().typeName(com.rtshop.job.WeaponTypes.PREMIUM));
                com.rtshop.rules.WorldRules wr4 = com.rtshop.rules.WorldRules.defaults();
                cfgTexts4.addAll(java.util.List.of(wr4.textAnvilOnly, wr4.textAnvilBuild, wr4.textAltarOnly, wr4.textAltarBuild, wr4.textPortalOnly, wr4.textPortalBuild));
                java.util.List<String> cfgMiss4 = new java.util.ArrayList<>();
                int cfgAll4 = 0;
                for (String s4 : cfgTexts4) {
                    if (s4 == null || s4.isEmpty()) continue;
                    boolean hangul4 = false;
                    for (int ci4 = 0; ci4 < s4.length() && !hangul4; ci4++) hangul4 = s4.charAt(ci4) >= 0xAC00 && s4.charAt(ci4) <= 0xD7A3;
                    if (!hangul4) continue;
                    cfgAll4++;
                    if (!com.rtshop.story.StoryText.hasEnglish(s4)) cfgMiss4.add(s4.length() > 40 ? s4.substring(0, 40) : s4);
                }
                check("every default config text (raid, weekly reward, boss intros, seals, Scylla clues, jobs, weapon types, world rules) has an English version ("
                        + (cfgAll4 - cfgMiss4.size()) + "/" + cfgAll4 + ")", cfgAll4 > 200 && cfgMiss4.isEmpty(), cfgMiss4);
                com.rtshop.job.JobConfig.JobDef sw4 = com.rtshop.job.JobConfig.get().job("swordsman");
                String koDesc4 = sw4 == null ? "" : String.join(" | ", com.rtshop.job.JobConfig.get().describe(sw4, 0.1, a));
                com.rtshop.story.StoryText.setLang(a, "en_us");
                String enDesc4 = sw4 == null ? "" : String.join(" | ", com.rtshop.job.JobConfig.get().describe(sw4, 0.1, a));
                String enRaid4 = com.rtshop.raid.RaidConfig.get().textFor(a, "needKill", 3, 1);
                String enPick4 = com.rtshop.story.StoryText.pick(a, "한국어", "English");
                com.rtshop.story.StoryText.setLang(a, "ko_kr");
                String koRaid4 = com.rtshop.raid.RaidConfig.get().textFor(a, "needKill", 3, 1);
                boolean noHangul4 = true;
                for (int ci4 = 0; ci4 < enDesc4.length() && noHangul4; ci4++) noHangul4 = !(enDesc4.charAt(ci4) >= 0xAC00 && enDesc4.charAt(ci4) <= 0xD7A3);
                check("English players get job passives, weapon lines and raid notices in English; Korean players keep the original",
                        sw4 != null && noHangul4 && enDesc4.contains("Sword") && !koDesc4.equals(enDesc4) && koDesc4.contains("검")
                                && enRaid4.contains("3") && !enRaid4.equals(koRaid4) && koRaid4.contains("이그니스") && "English".equals(enPick4)
                                && "한국어".equals(com.rtshop.story.StoryText.pick(a, "한국어", "English")),
                        enDesc4 + " // " + enRaid4 + " // " + koRaid4);
                // 3.69.0: 상점 창의 제목 · 상인 이름 · 분류 이름은 받는 사람의 언어로 (기본 문구만, 운영자가 고친 이름은 그대로)
                com.rtshop.shop.ShopData bs369 = ShopServer.loaded().shops.get("blacksmith");
                com.rtshop.story.StoryText.setLang(a, "en_us");
                com.rtshop.shop.ShopData en369 = ShopServer.localized(a, bs369);
                String mine369 = ShopServer.shopText(a, "철수네 대장간");
                java.util.List<String> shopMiss369 = new java.util.ArrayList<>();
                int shopAll369 = 0;
                for (com.rtshop.shop.ShopData sd369 : ShopServer.loaded().shops.values()) {
                    java.util.List<String> ts369 = new java.util.ArrayList<>(java.util.List.of(sd369.title, sd369.npcName));
                    for (com.rtshop.shop.ShopData.Category c369 : sd369.categories) ts369.add(c369.name);
                    for (String t369 : ts369) {
                        if (t369 == null || t369.chars().noneMatch(ch -> ch >= 0xAC00 && ch <= 0xD7A3)) continue;
                        shopAll369++;
                        if (ShopServer.shopText(a, t369).chars().anyMatch(ch -> ch >= 0xAC00 && ch <= 0xD7A3)) shopMiss369.add(sd369.id + ":" + t369);
                    }
                }
                com.rtshop.story.StoryText.setLang(a, "ko_kr");
                com.rtshop.shop.ShopData ko369 = ShopServer.localized(a, bs369);
                check("3.69.0 the shop window's title, merchant name and category names follow the player's language; a name the server typed stays as it is ("
                                + (shopAll369 - shopMiss369.size()) + "/" + shopAll369 + ")",
                        bs369 != null && en369 != null && "Equipment Shop".equals(en369.title) && "Equipment Merchant".equals(en369.npcName)
                                && en369.categories.stream().anyMatch(c -> "Melee Weapons".equals(c.name)) && en369.entries == bs369.entries
                                && ko369 == bs369 && "장비 상점".equals(ko369.title) && "철수네 대장간".equals(mine369) && shopAll369 > 20 && shopMiss369.isEmpty(),
                        (en369 == null ? "?" : en369.title) + " " + shopMiss369);
                // 3.58.1: 편집 화면의 제목·항목 이름 — 영어로 보는 운영자에게는 한글이 남지 않아야 한다
                com.rtshop.story.StoryText.setLang(a, "en_us");
                StringBuilder enEd5 = new StringBuilder();
                for (String tk5 : new String[]{"raid", "abyss", "abyssfloor1"}) {
                    com.rtshop.raid.RewardEditor.Target tg5 = com.rtshop.raid.RewardEditor.TARGETS.get(tk5);
                    if (tg5 == null) continue;
                    com.google.gson.JsonObject md5 = tg5.model(a);
                    enEd5.append(md5.get("title").getAsString()).append('|');
                    for (com.google.gson.JsonElement fe5 : md5.getAsJsonArray("fields")) enEd5.append(fe5.getAsJsonObject().get("label").getAsString()).append('|');
                    for (com.google.gson.JsonElement ge5 : md5.getAsJsonArray("groups")) enEd5.append(ge5.getAsJsonObject().get("label").getAsString()).append('|');
                }
                String enCmd5 = com.rtshop.story.StoryText.saveError(a, "저장하지 못했습니다: x");
                com.rtshop.story.StoryText.setLang(a, "ko_kr");
                String koEd5 = com.rtshop.raid.RewardEditor.TARGETS.get("raid").model(a).get("title").getAsString();
                String hangulAt5 = "";
                for (String part5 : enEd5.toString().split("\\|")) {
                    for (int ci5 = 0; ci5 < part5.length(); ci5++) {
                        if (part5.charAt(ci5) >= 0xAC00 && part5.charAt(ci5) <= 0xD7A3) hangulAt5 = part5;
                    }
                }
                check("the reward editors' titles and field names are English for an English operator and Korean for a Korean one",
                        enEd5.length() > 300 && hangulAt5.isEmpty() && koEd5.equals("주간 레이드 설정") && "Could not save: x".equals(enCmd5)
                                && "저장하지 못했습니다: x".equals(com.rtshop.story.StoryText.saveError(a, "저장하지 못했습니다: x")),
                        hangulAt5 + " // " + koEd5 + " // " + enCmd5 + " // " + enEd5.length());
            }
            com.rtshop.story.StoryText.setLang(a, "");
        }
        // 새 목표 종류: 표식(FLAG) / 차원(DIMENSION) 은 받자마자 맞으면 바로 채워진다
        QuestManager.reset(a);
        QuestManager.setFlag(a, "tutorial_done", true);
        boolean took = QuestManager.accept(a, "story_0_01", "quest_guide");
        long before = Economy.wallet(a);
        boolean done = QuestManager.turnIn(a, "story_0_01", "quest_guide", -1);
        check("a FLAG goal is met by an existing flag and the prologue's first quest can be handed in", took && done && Economy.wallet(a) > before, took + " " + done);
        // 대화: 모든 NPC 에게 대화가 있고, 장면이 있는 TALK 목표는 장면을 다 봐야 채워진다
        check("every NPC has things to talk about (18+ NPCs)", com.rtshop.story.Dialogue.npcCountForTest() >= 18
                && com.rtshop.story.Dialogue.npcForTest("quest_order_elder") != null && com.rtshop.story.Dialogue.npcForTest("shop_fisher") != null);
        QuestManager.forceComplete(a, "story_0_02");
        QuestManager.forceComplete(a, "story_0_02b");
        boolean took2 = QuestManager.accept(a, "story_0_03", "quest_order_elder");
        boolean pending = com.rtshop.story.Dialogue.pendingScene(a, "quest_order_archivist") != null;
        QuestManager.onTalk(a, "quest_order_archivist");
        boolean stillOpen = QuestManager.progressOf(a, "story_0_03")[0] == 0;
        QuestManager.onTalk(a, "quest_order_archivist", true);
        boolean filled = QuestManager.progressOf(a, "story_0_03")[0] == 1 && com.rtshop.story.Dialogue.pendingScene(a, "quest_order_archivist") == null;
        check("a talk goal with a scene waits until the scene is seen, then fills", took2 && pending && stillOpen && filled,
                took2 + " " + pending + " " + stillOpen + " " + filled);
        QuestDef.Objective dimGoal = new QuestDef.Objective();
        dimGoal.type = QuestDef.Type.DIMENSION;
        dimGoal.target = a.level().dimension().location().toString();
        check("a DIMENSION goal is met in that dimension", QuestManager.reached(a, dimGoal, dimGoal.target));
        QuestDef.Objective advGoal = new QuestDef.Objective();
        advGoal.type = QuestDef.Type.ADVANCEMENT;
        advGoal.target = "minecraft:story/mine_stone";
        net.minecraft.advancements.Advancement adv = server.getAdvancements().getAdvancement(new ResourceLocation(advGoal.target));
        QuestDef.Objective noAdv = new QuestDef.Objective();
        noAdv.type = QuestDef.Type.ADVANCEMENT;
        noAdv.target = "rtshop:no_such_advancement";
        check("an ADVANCEMENT goal finds the advancement and an unknown one never counts", adv != null && !QuestManager.reached(a, noAdv, ""));
        check("story quests carry their scenes (chapter title on taking 1-1, dream on finishing 1-2)",
                l.quests.get("story_0_01").sceneAccept != null && l.quests.get("story_0_03").sceneDone != null && l.quests.get("story_1_01").sceneAccept != null);
        check("the dream gives the undying ember (needed until the very end)",
                l.quests.get("story_0_03").rewardItems.stream().anyMatch(r -> r.item.equals("rtshop:undying_ember")));
        // 장비 진열대: 희귀는 1장, 서사는 2장 뒤
        com.rtshop.story.StoryGates.applyToFakeForTest = true;
        try {
            ItemStack rare = new ItemStack(Items.DIAMOND_SWORD);
            rare.setHoverName(null);
            ItemStack epicArmor = new ItemStack(Items.NETHERITE_CHESTPLATE);
            String r1 = com.rtshop.story.StoryGates.lockedChapter(a, new ItemStack(Items.IRON_SWORD));
            boolean shown = rare.getRarity() == net.minecraft.world.item.Rarity.COMMON;
            ItemStack enchanted = new ItemStack(Items.IRON_SWORD);
            enchanted.enchant(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS, 1); // 마법 부여 = RARE
            String r2 = com.rtshop.story.StoryGates.lockedChapter(a, enchanted);
            QuestManager.setFlag(a, "ch1_three_ends", true);
            String r3 = com.rtshop.story.StoryGates.lockedChapter(a, enchanted);
            check("story-gated gear: common gear is free, rare gear waits for the middle of chapter 1 (the three ends)", r1 == null && "1장 중반".equals(r2) && r3 == null, r1 + " " + r2 + " " + r3 + " " + shown);
        } finally {
            QuestManager.setFlag(a, "ch1_three_ends", false);
            com.rtshop.story.StoryGates.applyToFakeForTest = false;
        }
        // 봉인을 가리키는 눈은 퀘스트 보상으로만 (스킬라의 폭풍의 눈은 아직 주는 퀘스트가 없어 그대로)
        check("the seal eyes are quest/clue rewards only (flame eye and storm eye locked, others untouched)",
                (com.rtshop.story.StorySeals.recipeLocked("cataclysm:flame_eye") || !net.minecraftforge.fml.ModList.get().isLoaded("cataclysm"))
                        && com.rtshop.story.StorySeals.recipeLocked("cataclysm:storm_eye")
                        && !com.rtshop.story.StorySeals.recipeLocked("minecraft:ender_eye"));
        // 스킬라 추적: 단서를 모으면 폭풍의 눈과 결계가 열린다
        com.rtshop.story.ScyllaClues sc = com.rtshop.story.ScyllaClues.get();
        List<String> badClue = new ArrayList<>();
        for (com.rtshop.story.ScyllaClues.Clue c : sc.clues.values()) {
            if (c.source.equals("kill") && com.rtshop.story.ScyllaClues.modOk(c.target) && !ForgeRegistries.ENTITY_TYPES.containsKey(new ResourceLocation(c.target))) badClue.add(c.id);
            if (c.hints.isEmpty()) badClue.add(c.id + " (no hints)");
        }
        check("every Scylla clue points at a real monster and has villager hints", badClue.isEmpty() && sc.clues.size() >= sc.needed, badClue);
        check("a villager gives a profession-specific rumour, others a general one",
                com.rtshop.story.ScyllaClues.hintFor(sc.clues.get("wet_logbook"), "fisherman").contains("폭풍의 눈")
                        && com.rtshop.story.ScyllaClues.hintFor(sc.clues.get("wet_logbook"), "farmer") != null);
        // 다섯 단서: 뽑기·보스·야생·낚시는 운에 달렸지만 천장이 있어 결국 얻고, 인장은 선택지(카스)에 따라 다른 때에 얻는다
        {
            java.util.Set<String> src = new java.util.TreeSet<>();
            for (com.rtshop.story.ScyllaClues.Clue c : sc.clues.values()) src.add(c.source);
            check("Scylla has five clues: gacha, story boss, wild chest, fishing and one from story choices",
                    sc.needed == 5 && sc.clues.size() == 5 && src.equals(new java.util.TreeSet<>(List.of("gacha", "boss", "wild", "fish", "quest"))), src);
            com.rtshop.story.ScyllaClues.Clue coin = sc.clues.get("storm_coin");
            double keep = coin.chance;
            coin.chance = 0;
            com.rtshop.story.ScyllaClues.testRolls = true;
            a.getInventory().clearContent();
            boolean early = com.rtshop.story.ScyllaClues.roll(a, "gacha", "", coin.pity - 1, null);
            int tries = com.rtshop.story.ScyllaClues.tries(a, "storm_coin");
            boolean atCap = com.rtshop.story.ScyllaClues.roll(a, "gacha", "", 1, null);
            boolean held = com.rtshop.story.ScyllaClues.of(a.getInventory().getItem(0)) == coin;
            boolean noSecond = !com.rtshop.story.ScyllaClues.roll(a, "gacha", "", coin.pity + 5, null);
            com.rtshop.story.ScyllaClues.testRolls = false;
            coin.chance = keep;
            check("an unlucky player still gets a luck clue at the pity cap (tries are counted, and no second copy while one is carried)",
                    !early && tries == coin.pity - 1 && atCap && held && noSecond, early + " " + tries + " " + atCap + " " + held + " " + noSecond);
            a.getInventory().clearContent();
            QuestDef ka = QuestManager.loaded().quests.get("hidden_scylla_kas_a"), kb = QuestManager.loaded().quests.get("hidden_scylla_kas_b");
            check("the choice clue comes either way: early for those who freed Kas, later for those who handed him over",
                    ka != null && kb != null && ka.requiresFlags.contains("ch1_kas_spared") && kb.requiresFlags.contains("ch1_kas_held")
                            && ka.rewardFlags.contains("scylla_clue_seal_shard") && kb.rewardFlags.contains("scylla_clue_seal_shard")
                            && QuestManager.loaded().quests.containsKey("hidden_scylla_6"));
        }
        check("the Scylla seal is on because the clues grant its flag",
                com.rtshop.story.StorySeals.active(com.rtshop.story.StorySeals.get().seals.get("scylla")));
        ItemStack clueItem = com.rtshop.story.ScyllaClues.stack(sc.clues.get("wet_logbook"));
        check("a clue item knows which clue it is", com.rtshop.story.ScyllaClues.of(clueItem) == sc.clues.get("wet_logbook"));
        a.getInventory().clearContent();
        int n = 0;
        for (com.rtshop.story.ScyllaClues.Clue c : sc.clues.values()) {
            if (n == sc.needed - 1) break;
            com.rtshop.story.ScyllaClues.record(a, c);
            n++;
        }
        boolean notYet = !QuestManager.hasFlag(a, sc.grantFlag);
        com.rtshop.story.ScyllaClues.Clue last = null;
        for (com.rtshop.story.ScyllaClues.Clue c : sc.clues.values()) if (!com.rtshop.story.ScyllaClues.has(a, c.id)) last = c;
        boolean again = com.rtshop.story.ScyllaClues.record(a, sc.clues.values().iterator().next());
        com.rtshop.story.ScyllaClues.record(a, last);
        // 메인 스토리를 끝내기 전에는 단서를 다 모아도 열리지 않는다
        boolean waits = !QuestManager.hasFlag(a, sc.grantFlag);
        QuestManager.setFlag(a, "story_done", true);
        com.rtshop.story.ScyllaClues.checkComplete(a);
        boolean eye = !net.minecraftforge.fml.ModList.get().isLoaded("cataclysm") || a.getInventory().countItem(ForgeRegistries.ITEMS.getValue(new ResourceLocation("cataclysm:storm_eye"))) == 1;
        check("collecting every clue completes the storm eye and opens Scylla's seal, but only after the main story ends (not before, and a clue counts once)",
                notYet && !again && waits && QuestManager.hasFlag(a, sc.grantFlag) && eye, notYet + " " + again + " " + waits + " " + eye);
        QuestManager.setFlag(a, "story_done", false);
        QuestManager.setFlag(a, "scylla_waiting", false);
        for (String id : sc.clues.keySet()) QuestManager.setFlag(a, com.rtshop.story.ScyllaClues.FLAG_PREFIX + id, false);
        QuestManager.setFlag(a, sc.grantFlag, false);
        a.getInventory().clearContent();
        if (net.minecraftforge.fml.ModList.get().isLoaded("cataclysm")) {
            boolean craftable = false;
            for (net.minecraft.world.item.crafting.Recipe<?> r : server.getRecipeManager().getRecipes()) {
                ResourceLocation out = com.rtshop.rules.RecipeRules.outputId(r, server);
                if (out != null && out.toString().equals("cataclysm:flame_eye")) craftable = true;
            }
            check("Cataclysm's flame eye has no crafting recipe on the server", !craftable);
        }
        // 토벌 의뢰판과 몹 도감
        com.rtshop.story.Hunts.clearForTest(a);
        com.rtshop.story.Hunts.testMode = true;
        for (String f : List.of("story_ch1_done", "story_ch2_done", "story_ch3_done")) QuestManager.setFlag(a, f, false);
        net.minecraft.nbt.ListTag bl = com.rtshop.story.Hunts.bounties(a);
        com.rtshop.story.Hunts hs = com.rtshop.story.Hunts.get();
        boolean onlyVanilla = true;
        for (int i = 0; i < bl.size(); i++) if (!bl.getCompound(i).getString("entity").startsWith("minecraft:")) onlyVanilla = false;
        int notTaken = 0;
        for (int i = 0; i < bl.size(); i++) if (!com.rtshop.story.Hunts.accepted(bl.getCompound(i))) notTaken++;
        check("the bounty board offers today's and this week's bounties (nothing taken yet), only from the opening chapter's mobs",
                bl.size() == Math.max(hs.dailyCount, hs.dailyOffers) + Math.max(hs.weeklyCount, hs.weeklyOffers) && notTaken == bl.size() && onlyVanilla, bl);
        int t0 = com.rtshop.story.Hunts.take(a, -1, 0), t1 = com.rtshop.story.Hunts.take(a, -1, 0);
        int dailyIdx = 0, takenDaily = 0;
        for (int i = 0; i < bl.size() && takenDaily < hs.dailyCount + 1; i++) {
            if (bl.getCompound(i).getBoolean("weekly")) continue;
            int r = com.rtshop.story.Hunts.take(a, -1, i);
            if (r == 0) takenDaily++;
            dailyIdx = r;
        }
        check("a bounty is taken at the board; taking it twice does nothing; no more than the daily limit",
                t0 == 0 && t1 == 1 && dailyIdx == 2, t0 + " " + t1 + " " + dailyIdx + " " + takenDaily);
        int far = com.rtshop.story.Hunts.take(a, 999999, 1);
        check("bounties can only be taken next to the board NPC (not from anywhere)", far == 3, far);
        List<String> badHunt = new ArrayList<>();
        for (com.rtshop.story.Hunts.Pool pool : hs.pools) for (com.rtshop.story.Hunts.Target t : pool.targets) {
            if (com.rtshop.story.ScyllaClues.modOk(t.entity) && !ForgeRegistries.ENTITY_TYPES.containsKey(new ResourceLocation(t.entity))) badHunt.add(t.entity);
        }
        for (com.rtshop.story.Hunts.Book bk : hs.books) for (String e : bk.entities) {
            if (com.rtshop.story.ScyllaClues.modOk(e) && !ForgeRegistries.ENTITY_TYPES.containsKey(new ResourceLocation(e))) badHunt.add(e);
        }
        check("every bounty and bestiary mob exists (when its mod is installed)", badHunt.isEmpty(), badHunt);
        {
            com.rtshop.story.Hunts.Target zt = null, et = null;
            for (com.rtshop.story.Hunts.Pool pool : hs.pools) for (com.rtshop.story.Hunts.Target t : pool.targets) {
                if (t.entity.equals("minecraft:zombie")) zt = t;
                if (t.entity.equals("minecraft:enderman")) et = t;
            }
            boolean auto = zt != null && et != null;
            long gz = auto ? com.rtshop.story.Hunts.goldEach(zt) : 0, ge = auto ? com.rtshop.story.Hunts.goldEach(et) : 0;
            int[] rz = auto ? com.rtshop.story.Hunts.range(zt) : new int[2], re = auto ? com.rtshop.story.Hunts.range(et) : new int[2];
            check("bounty rewards follow the mob's difficulty: an enderman pays more per kill and asks for fewer than a zombie",
                    auto && ge > gz && re[1] < rz[1] && com.rtshop.story.Hunts.stars("minecraft:enderman") > com.rtshop.story.Hunts.stars("minecraft:zombie"),
                    gz + "G " + rz[0] + "-" + rz[1] + " / " + ge + "G " + re[0] + "-" + re[1]);
            boolean added = com.rtshop.story.Hunts.poolAdd(null, "minecraft:cave_spider", 0, 0, 0) == 1;
            boolean listed = hs.pools.stream().anyMatch(pl -> pl.targets.stream().anyMatch(t -> t.entity.equals("minecraft:cave_spider")));
            boolean fixed = com.rtshop.story.Hunts.poolAdd(null, "minecraft:cave_spider", 4, 6, 77) == 1;
            com.rtshop.story.Hunts.Target ct = null;
            for (com.rtshop.story.Hunts.Pool pool : hs.pools) for (com.rtshop.story.Hunts.Target t : pool.targets) if (t.entity.equals("minecraft:cave_spider")) ct = t;
            boolean set = ct != null && com.rtshop.story.Hunts.goldEach(ct) == 77 && com.rtshop.story.Hunts.range(ct)[0] == 4 && com.rtshop.story.Hunts.range(ct)[1] == 6;
            boolean removed = com.rtshop.story.Hunts.poolRemove(null, "minecraft:cave_spider") == 1;
            boolean unknown = com.rtshop.story.Hunts.poolAdd(null, "minecraft:not_a_mob", 0, 0, 0) == 0;
            check("an OP can add a mob to the bounty list (auto or with set count/reward) and remove it again",
                    added && listed && fixed && set && removed && unknown, added + " " + listed + " " + fixed + " " + set + " " + removed + " " + unknown);
        }
        com.rtshop.story.Hunts.setForTest(a, "minecraft:zombie", 2, 100);
        net.minecraft.world.entity.monster.Zombie z = new net.minecraft.world.entity.monster.Zombie(level);
        long w0 = Economy.wallet(a);
        com.rtshop.story.Hunts.onKill(a, z);
        boolean firstSeen = com.rtshop.story.Hunts.seen(a, "minecraft:zombie");
        long w1 = Economy.wallet(a);
        com.rtshop.story.Hunts.onKill(a, z);
        long w2 = Economy.wallet(a);
        check("a bounty pays out once its count is reached (100 G, more with a combat job), and the first kill fills the bestiary",
                firstSeen && w1 == w0 && w2 - w1 == com.rtshop.story.Hunts.reward(a, 100) && w2 > w1, w0 + " " + w1 + " " + w2);
        com.rtshop.story.Hunts.onKill(a, z);
        check("a finished bounty does not pay twice", Economy.wallet(a) == w2);
        QuestManager.setFlag(a, "story_ch1_done", true);
        boolean mowzie = com.rtshop.story.Hunts.available(a).stream().anyMatch(t -> t.entity.startsWith("mowziesmobs:"));
        check("finishing chapter 1 adds Mowzie's mobs to the bounty pool (when installed)", mowzie || !net.minecraftforge.fml.ModList.get().isLoaded("mowziesmobs"));
        QuestManager.setFlag(a, "story_ch1_done", false);
        com.rtshop.story.Hunts.clearForTest(a);
        com.rtshop.story.Hunts.testMode = false;
        // 아바스 층을 깨면 영구 표식이 남는다 (2장 시련)
        com.rtshop.raid.AbyssWeekly.cleared(a, 2);
        check("clearing a Spiral Abyss floor leaves a lasting flag for the story", QuestManager.hasFlag(a, "abyss_floor_2"));
        QuestManager.reset(a);
    }

    private static void party(MinecraftServer server, ServerLevel level, FakePlayer a, FakePlayer b) throws Exception {
        section("party and boss credit");
        com.rtshop.party.PartyConfig pc = com.rtshop.party.PartyConfig.fresh();
        pc.bosses.add("minecraft:iron_golem");
        com.rtshop.party.PartyConfig.setForTest(pc);
        com.rtshop.raid.Raid.TEST_PLAYERS.add(a);
        com.rtshop.raid.Raid.TEST_PLAYERS.add(b);
        try {
            com.rtshop.party.Party.clearForTest(server);
            com.rtshop.party.Party.Group g = com.rtshop.party.Party.createForTest(a);
            com.rtshop.party.Party.inviteForTest(b, g);
            boolean joined = com.rtshop.party.Party.accept(b);
            check("a party can be made and an invited player joins it", joined && com.rtshop.party.Party.together(server, a.getUUID(), b.getUUID())
                    && g.members.size() == 2);
            check("the party size limit is 12 and a boss kill needs 10% of its health", pc.maxMembers == 12 && pc.bossCreditPercent == 10.0);
            check("merchants have personal names (farmer: 농작물 상인 한나)", com.rtshop.shop.NpcNames.display("farmer", "농작물 상인").equals("농작물 상인 한나")
                    && com.rtshop.shop.NpcNames.display("unknown_shop", "가게").equals("가게"), com.rtshop.shop.NpcNames.display("farmer", "농작물 상인"));
            // 파티원끼리 피해 없음
            net.minecraftforge.event.entity.living.LivingAttackEvent ev = new net.minecraftforge.event.entity.living.LivingAttackEvent(b, a.damageSources().playerAttack(a), 4F);
            com.rtshop.party.Party.onAttack(ev);
            check("party members cannot hurt each other", ev.isCanceled());
            // 보스 처치 인정: 최대 체력의 5% 이상
            net.minecraft.world.entity.animal.IronGolem boss = EntityType.IRON_GOLEM.create(level);
            boss.setPos(a.getX(), a.getY(), a.getZ());
            com.rtshop.party.BossCredit.record(boss, a, boss.getMaxHealth() * 0.11F);
            com.rtshop.party.BossCredit.record(boss, b, boss.getMaxHealth() * 0.09F);
            List<ServerPlayer> q = com.rtshop.party.BossCredit.qualified(boss, server);
            check("a boss kill counts only for players who dealt at least 10% of its health (11% yes, 9% no)",
                    q.contains(a) && !q.contains(b), q.size());
            com.rtshop.party.BossCredit.forget(boss);
            // 보스 등장 연출: 처음 가까이 왔을 때 한 번, 떨어뜨린 물건에 이야기 표식
            com.rtshop.story.StoryConfig sc = com.rtshop.story.StoryConfig.get();
            sc.intros.put("minecraft:iron_golem", com.rtshop.story.StoryConfig.defaults().get("cataclysm:ignis"));
            boolean shown = com.rtshop.story.BossIntros.show(boss);
            check("a story boss plays its entrance once (marked as seen)", shown && com.rtshop.story.BossIntros.seen(boss));
            check("every story boss has an entrance and a life story (13 bosses)", com.rtshop.story.StoryConfig.defaults().size() >= 13
                    && com.rtshop.story.StoryConfig.defaults().values().stream().allMatch(i -> !i.lore.isEmpty() && !i.name.isEmpty()));
            sc.intros.remove("minecraft:iron_golem");
            // 결계: 표식을 주는 퀘스트가 없으면 꺼져 있고, 켜지면 표식이 있어야 지나간다 (파티원이 데려갈 수 있다)
            com.rtshop.story.StorySeals.Seal seal = com.rtshop.story.StorySeals.get().seals.get("ignis");
            check("seals turn on only when a quest grants their flag (story quests open the fire altar; an unknown flag stays off)",
                    seal != null && com.rtshop.story.StorySeals.grantedByAnyQuest(seal.flag) && !com.rtshop.story.StorySeals.grantedByAnyQuest("seal_nobody_gives_this"));
            com.rtshop.story.StorySeals.forceActiveForTest = true;
            com.rtshop.story.StorySeals.TEST_PLAYERS.put(a.getUUID(), a);
            com.rtshop.story.StorySeals.TEST_PLAYERS.put(b.getUUID(), b);
            try {
                com.rtshop.quest.QuestManager.setFlag(a, seal.flag, false);
                com.rtshop.quest.QuestManager.setFlag(b, seal.flag, false);
                com.rtshop.party.Party.clearForTest(server);
                boolean blocked = !com.rtshop.story.StorySeals.passes(a, seal);
                com.rtshop.quest.QuestManager.setFlag(a, seal.flag, true);
                boolean opened = com.rtshop.story.StorySeals.passes(a, seal);
                boolean aloneB = !com.rtshop.story.StorySeals.passes(b, seal);
                com.rtshop.party.Party.createForTest(a, b);
                boolean carried = com.rtshop.story.StorySeals.passes(b, seal);
                check("a seal blocks a player without its flag, opens with it, and a party member with it carries others",
                        blocked && opened && aloneB && carried, blocked + " " + opened + " " + aloneB + " " + carried);
                check("the fire altar is sealed until the seven seals are broken",
                        com.rtshop.story.StorySeals.ofBlock("cataclysm:altar_of_fire") == seal);
                com.google.gson.JsonObject ss = com.google.gson.JsonParser.parseString(
                        "{\"structures\":[],\"placement\":{\"type\":\"minecraft:random_spread\",\"spacing\":25,\"separation\":8,\"salt\":1}}").getAsJsonObject();
                com.google.gson.JsonObject sc2 = com.rtshop.story.StructureRarity.scale(ss, 1.6);
                check("boss structures are spread further apart (25/8 x1.6 -> 40/13)",
                        sc2 != null && sc2.getAsJsonObject("placement").get("spacing").getAsInt() == 40
                                && sc2.getAsJsonObject("placement").get("separation").getAsInt() == 13, sc2);
                if (net.minecraftforge.fml.ModList.get().isLoaded("mowziesmobs")) {
                    check("the story structure pack spreads Mowzie's boss structures out", com.rtshop.story.StructureRarity.LAST.containsKey("mowziesmobs:wrought_chambers")
                            && server.getPackRepository().getSelectedIds().contains(com.rtshop.story.StructureRarity.PACK_ID), com.rtshop.story.StructureRarity.LAST);
                }
            } finally {
                com.rtshop.story.StorySeals.forceActiveForTest = false;
                com.rtshop.story.StorySeals.TEST_PLAYERS.clear();
                com.rtshop.quest.QuestManager.setFlag(a, seal.flag, false);
                com.rtshop.party.Party.clearForTest(server);
                com.rtshop.party.Party.createForTest(a, b); // 뒤의 파티 점검(나가기 등)을 위해 원래대로
            }
            boolean left = com.rtshop.party.Party.leave(b);
            check("a member can leave the party", left && !com.rtshop.party.Party.together(server, a.getUUID(), b.getUUID()));
        } finally {
            com.rtshop.party.Party.clearForTest(server);
            com.rtshop.raid.Raid.TEST_PLAYERS.clear();
            com.rtshop.party.PartyConfig.reload();
        }
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
            // 3.67.0 생활 직업 수입 맞추기: 광부는 캐낸 광석 · 원석 · 보석을 20% 더 받고, 주괴나 물고기는 아니다
            com.rtshop.job.Jobs.force(a, "life", "miner");
            ShopData.Entry rawIron = null, ingot = null;
            for (ShopData sd : ShopServer.shops().values()) {
                for (ShopData.Entry en : sd.entries) {
                    if (rawIron == null && en.sellable && en.stack.is(Items.RAW_IRON)) rawIron = en;
                    if (ingot == null && en.sellable && en.stack.is(Items.IRON_INGOT)) ingot = en;
                }
            }
            check("3.67.0 a miner sells mined goods (raw iron) for 20% more, but not ingots or fish",
                    rawIron != null && Math.abs(ShopServer.jobSellMultiplier(a, rawIron) - 1.20) < 1e-6
                            && (ingot == null || Math.abs(ShopServer.jobSellMultiplier(a, ingot) - 1.0) < 1e-6)
                            && (codIdx < 0 || Math.abs(ShopServer.jobSellMultiplier(a, fisher.entries.get(codIdx)) - 1.0) < 1e-6)
                            && ShopServer.minedGood(new ItemStack(Items.DIAMOND)) && ShopServer.minedGood(new ItemStack(Items.IRON_ORE))
                            && !ShopServer.minedGood(new ItemStack(Items.CHARCOAL)) && !ShopServer.minedGood(new ItemStack(Items.GOLD_NUGGET)),
                    rawIron == null ? "no raw iron in shops" : ShopServer.jobSellMultiplier(a, rawIron) + "");
            com.rtshop.job.Jobs.force(a, "life", "cook");
            com.rtshop.job.JobConfig jc367 = com.rtshop.job.JobConfig.get();
            check("3.67.0 life jobs are brought closer: farmer star-up 35% · extra crop 20%, cook dish bonus 20%, miner ore sale +20%, fish bought at 85% of the list",
                    jc367.job("farmer").param("starUp", 0) == 0.35 && jc367.job("farmer").param("extraHarvest", 0) == 0.20 && jc367.job("cook").param("dishSell", 0) == 0.20
                            && jc367.job("miner").param("oreSell", 0) == 0.20 && com.rtshop.shop.PricingConfig.get().fishBaseMultiplier == 1.7,
                    jc367.job("farmer").params + " " + jc367.job("cook").params + " " + jc367.job("miner").params + " fish " + com.rtshop.shop.PricingConfig.get().fishBaseMultiplier);
            // 옛 jobs.json: 기본값 그대로인 수치만 새 값으로, 운영자가 고친 수치는 그대로
            com.google.gson.JsonObject oldJobs367 = jc367.toJson();
            oldJobs367.remove("paramsVersion");
            for (com.google.gson.JsonElement je : oldJobs367.getAsJsonArray("jobs")) {
                com.google.gson.JsonObject jo = je.getAsJsonObject();
                String jid = jo.get("id").getAsString();
                if (jid.equals("farmer")) {
                    jo.getAsJsonObject("params").addProperty("starUp", 0.20);
                    jo.getAsJsonObject("params").addProperty("extraHarvest", 0.15);
                }
                if (jid.equals("cook")) jo.getAsJsonObject("params").addProperty("dishSell", 0.28);
            }
            com.rtshop.job.JobConfig up367 = com.rtshop.job.JobConfig.fromJson(oldJobs367);
            int changedParams367 = up367.upgradeParams(1);
            check("3.67.0 an older jobs.json gets the new life-job numbers where it still had the old defaults, and keeps numbers the server changed",
                    changedParams367 == 2 && up367.job("farmer").param("starUp", 0) == 0.35 && up367.job("farmer").param("extraHarvest", 0) == 0.15
                            && up367.job("cook").param("dishSell", 0) == 0.20, changedParams367 + " " + up367.job("farmer").params + " " + up367.job("cook").params);
            incomeStats368(server, a, fisher, food);

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
                double cookBonus = cfg.job("cook").param("dishSell", 0);
                double cook = p.sellUnit * (1 + cookBonus);
                double cookWant = p.purchaseNeed * (1 + margin) * (1 + cookBonus);   // 3.67.0: 요리사 보너스 28% → 20% (재료비의 약 132%)
                if (p.cookable && cook + 1e-6 < cookWant) {
                    low.add(ShopLoader.itemId(p.stack) + " cook " + cook + "<" + cookWant);
                }
            }
            info(hand + " hand-made foods");
            check("hand-made food pays everyone 10% over the ingredients and a cook the cook's bonus on top (about 132%)", low.isEmpty(),
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
        // 3.69.0: 사망 손실 5~50% → 5~30% (옛 기본값 50 그대로인 파일만 고치고, 서버가 정한 값은 그대로)
        java.nio.file.Path ecoP = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("economy.json");
        double fresh369 = cfg.deathLossMaxPercent, old369 = -1, mine369 = -1;
        try {
        String ecoKeep = Files.exists(ecoP) ? Files.readString(ecoP, StandardCharsets.UTF_8) : null;
        try {
            Files.createDirectories(ecoP.getParent());
            Files.writeString(ecoP, "{\"configVersion\":3,\"deathLossMinPercent\":5.0,\"deathLossMaxPercent\":50.0}", StandardCharsets.UTF_8);
            old369 = EconomyConfig.reload().deathLossMaxPercent;
            Files.writeString(ecoP, "{\"configVersion\":3,\"deathLossMinPercent\":5.0,\"deathLossMaxPercent\":40.0}", StandardCharsets.UTF_8);
            mine369 = EconomyConfig.reload().deathLossMaxPercent;
        } finally {
            if (ecoKeep != null) Files.writeString(ecoP, ecoKeep, StandardCharsets.UTF_8);
            else Files.deleteIfExists(ecoP);
            EconomyConfig.reload();
        }
        } catch (java.io.IOException ex369) {
            info("economy file test: " + ex369);
        }
        check("3.69.0 death takes 5~30% of carried gold by default; an old file with the old default 50 moves to 30, a value the server set stays",
                fresh369 == 30.0 && cfg.deathLossMinPercent == 5.0 && old369 == 30.0 && mine369 == 40.0, fresh369 + " " + old369 + " " + mine369);
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
            if (!SeedRules.isPlantable(plain) || id.startsWith("tacz:") || id.startsWith("kubejs:")
                    || com.rtshop.shop.PricingConfig.get().notForSaleMods.contains(id.substring(0, id.indexOf(':')))) {
                continue; // 보스 모드 물건(예: 폴리아스 씨앗)은 상점에서 팔지 않는다
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
        // 경계 표시: 들어섰다고 켜지지 않고, 명령어(/rtshop border)로 켰을 때만
        java.util.BitSet packed = com.rtshop.land.PlotBrowser.unpackMask(com.rtshop.land.PlotBrowser.packMask(claim.mask));
        check("a lot's shape survives packing for the merchant's map", claim.mask.equals(packed));
        com.google.gson.JsonObject listed = com.rtshop.land.PlotBrowser.list(b);
        boolean shaped = false;
        for (com.google.gson.JsonElement el : listed.getAsJsonArray("plots")) {
            com.google.gson.JsonObject eo = el.getAsJsonObject();
            if (eo.get("id").getAsInt() == plot.id) shaped = eo.has("shape");
        }
        check("the merchant's list carries the real shape of a non-square lot", shaped);
        check("the border display is off until the player turns it on", !LandManager.borderOn(b));
        check("/rtshop border toggles it", LandManager.setBorder(b, null) && LandManager.borderOn(b) && !LandManager.setBorder(b, null)
                && !LandManager.borderOn(b));
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
        ShopLoader.Price goldenCarrot = plain(l, Items.GOLDEN_CARROT);
        ShopLoader.Price ingotForCarrot = plain(l, Items.GOLD_INGOT);
        if (goldenCarrot != null && goldenCarrot.sellableSomewhere && ingotForCarrot != null) {
            // 금 조각 8개 = 금 주괴 8/9 개. 전에는 조각을 '금 장비를 녹여서'만 마련한다고 셈해 황금 당근을 5,585 G 에 사 주었다 (3.65.0)
            double viaIngots = ingotForCarrot.price * 8.0 / 9.0;
            check("3.65.0 a golden carrot is bought back near what its gold costs (no buy-gold, craft, sell loop)",
                    goldenCarrot.sellUnit <= (viaIngots + 60) * 1.3, goldenCarrot.sellUnit + " vs gold " + viaIngots);
        }
        {
            // 값 계산이 읽는 조합법 파일에는 게임에서 지운 조합법(Create 전체 등)도 있다 → 음식 매입가의 하한에서는 뺀다
            com.rtshop.shop.RecipeSolver.Recipe createCake = new com.rtshop.shop.RecipeSolver.Recipe();
            createCake.id = "create:crafting/curiosities/cake";
            createCake.type = "minecraft:crafting_shaped";
            createCake.outputs.add(new com.rtshop.shop.RecipeSolver.Out("minecraft:cake", 1));
            com.rtshop.shop.RecipeSolver.Recipe vanillaCake = new com.rtshop.shop.RecipeSolver.Recipe();
            vanillaCake.id = "minecraft:cake";
            vanillaCake.type = "minecraft:crafting_shaped";
            vanillaCake.outputs.add(new com.rtshop.shop.RecipeSolver.Out("minecraft:cake", 1));
            check("3.65.0 food price floors only use recipes that exist in game (Create's cake recipe is removed by the pack, the vanilla one is not)",
                    !ShopLoader.inGameForTest(createCake) && ShopLoader.inGameForTest(vanillaCake));
            com.rtshop.shop.RecipeSolver.Recipe createFlour = new com.rtshop.shop.RecipeSolver.Recipe();
            createFlour.id = "create:milling/wheat";
            createFlour.type = "create:milling";
            createFlour.outputs.add(new com.rtshop.shop.RecipeSolver.Out("create:wheat_flour", 1));
            check("3.65.0 price costs skip removed mods' recipes for other mods' things (Create's cake), but keep the ones for their own things (Create's flour)",
                    ShopLoader.foreignBannedRouteForTest(createCake) && !ShopLoader.foreignBannedRouteForTest(createFlour) && !ShopLoader.foreignBannedRouteForTest(vanillaCake));
            // 음식이 아닌 물건은 재료를 사서 만드는 값보다 비싸게 사 주지 않는다 (사서 만들어 팔기만 해도 돈이 불어나는 길이 없다)
            java.util.Map<String, ShopLoader.BuyRoute> buyRoutes365 = ShopLoader.buyRoutes(l);
            List<String> buyCraftLoops = new ArrayList<>();
            for (ShopLoader.Price pr : l.prices.values()) {
                if (pr.stack.isEmpty() || pr.stack.hasTag() || !pr.sellableSomewhere || pr.dynamicFood || pr.stack.isEdible()) continue;
                String pid = String.valueOf(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(pr.stack.getItem()));
                if (l.solved != null && l.solved.processed.contains(pid)) continue;
                ShopLoader.BuyRoute br = buyRoutes365.get(pid);
                double sell = pr.sellUnit >= 0 ? pr.sellUnit : pr.price * 0.5;
                if (br != null && sell > br.cost() + 1.0) buyCraftLoops.add(pid + " " + sell + ">" + br.cost());
            }
            check("3.65.0 nothing but food is bought back for more than its materials cost in the shops (no buy, craft, sell loop)",
                    buyCraftLoops.isEmpty(), buyCraftLoops.size() + " " + String.join(", ", buyCraftLoops.subList(0, Math.min(6, buyCraftLoops.size()))));
        }
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
        ShopLoader.Price shovel = plain(l, Items.WOODEN_SHOVEL);
        ShopLoader.Price bricks = plain(l, Items.BRICKS);
        check("3.65.0 things anyone can craft from priced materials are not sold for many times what they cost to make (wooden shovel, bricks, stone pickaxe)",
                (shovel == null || shovel.price <= 20) && (bricks == null || bricks.price <= 5) && (stonePick == null || stonePick.price <= 40),
                (shovel == null ? "-" : shovel.price + "") + " / " + (bricks == null ? "-" : bricks.price + "") + " / " + (stonePick == null ? "-" : stonePick.price + ""));
        if (l.solved != null) {
            boolean guessed = false;
            StringBuilder g = new StringBuilder();
            for (String id : new String[]{"minecraft:nether_star", "minecraft:elytra", "minecraft:totem_of_undying", "minecraft:echo_shard", "minecraft:dragon_breath"}) {
                if (l.solved.estimated.containsKey(id)) {
                    guessed = true;
                    g.append(' ').append(id).append('=').append(l.solved.estimated.get(id));
                }
            }
            check("3.65.0 rare materials (nether star, elytra, totem, echo shard, dragon breath) use real values in recipe costs, not the 2 G guess",
                    !guessed, l.solved.estimated.size() + " guessed" + g);
        }
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
                && v.getCustomName() != null && v.getCustomName().getString().equals(com.rtshop.shop.NpcNames.display("attendance", c.npcName)));
        check("the attendance NPC also takes part in quests", v != null && "attendance".equals(QuestManager.npcKey(v)));
        if (v != null) {
            // NPC 의 자리를 기억하고, 오늘 출석을 안 받았으면 퀘스트 HUD 가 알려 준다 (받으면 사라지고, 날짜·시각이 기록된다)
            com.rtshop.quest.NpcPlaces.note(v);
            String where = com.rtshop.quest.NpcPlaces.where(level.getServer(), "attendance");
            com.rtshop.attendance.Attendance.reset(a);
            boolean due = com.rtshop.attendance.Attendance.dueToday(a);
            boolean hudDue = QuestManager.hudEntries(a).stream().anyMatch(x -> x.id().equals(QuestManager.ATTEND_ID) && x.where().equals(where));
            com.rtshop.attendance.Attendance.doCheckIn(a, false);
            boolean hudGone = QuestManager.hudEntries(a).stream().noneMatch(x -> x.id().equals(QuestManager.ATTEND_ID));
            int hist = com.rtshop.attendance.Attendance.view(a, -1).getAsJsonArray("history").size();
            check("the quest HUD reminds a player who has not checked in today (with where the clerk stands) until they do; check-ins are kept with date and time",
                    where.startsWith(level.dimension().location().toString() + ";") && due && hudDue && hudGone && hist == 1, where + " " + due + " " + hudDue + " " + hudGone + " " + hist);
            com.rtshop.attendance.Attendance.reset(a);
            clear(a);
            v.discard();
        }
        // 직업 선행 퀘스트: 그 칸의 직업을 이미 가졌거나 그 칸의 선행 퀘스트를 하고 있으면 "받을 수 있는 퀘스트" 셈에서 뺀다 (전투·생활 따로)
        QuestManager.reset(a);
        com.rtshop.job.Jobs.force(a, com.rtshop.job.JobConfig.COMBAT, "");
        com.rtshop.job.Jobs.force(a, com.rtshop.job.JobConfig.LIFE, "");
        QuestDef jw = QuestManager.loaded().quests.get("job_warrior_1"), jf = QuestManager.loaded().quests.get("job_farmer_1"), js = QuestManager.loaded().quests.get("job_swordsman_1");
        if (jw != null && jf != null && js != null) {
            int before = QuestManager.viewsCount(a, "job");
            boolean free = !QuestManager.jobQuestSettled(a, jw) && !QuestManager.jobQuestSettled(a, jf);
            com.rtshop.job.Jobs.force(a, com.rtshop.job.JobConfig.COMBAT, "swordsman");
            boolean combatOut = QuestManager.jobQuestSettled(a, jw) && !QuestManager.jobQuestSettled(a, jf);
            int withJob = QuestManager.viewsCount(a, "job");
            com.rtshop.job.Jobs.force(a, com.rtshop.job.JobConfig.COMBAT, "");
            QuestManager.accept(a, "job_swordsman_1", "job");
            boolean doing = QuestManager.jobQuestSettled(a, jw) && !QuestManager.jobQuestSettled(a, js) && !QuestManager.jobQuestSettled(a, jf);
            check("job trial quests stop counting as available once that slot (combat or life, separately) has a job or a trial under way",
                    free && combatOut && doing && withJob < before && "combat".equals(QuestManager.jobSlotOf(jw)) && "life".equals(QuestManager.jobSlotOf(jf)),
                    free + " " + combatOut + " " + doing + " " + before + ">" + withJob);
            QuestManager.reset(a);
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

    /** 플레이어 상점: 주인만 올리기·관리, 다른 사람은 사기, 판 돈은 주인 예금으로, NPC 가격 비교. */
    private static void playerShop(MinecraftServer server, ServerLevel level, FakePlayer a, FakePlayer b, BlockPos pos) {
        LINES.add("");
        LINES.add("[player shop]");
        com.rtshop.pshop.PlayerShops.lastResult = null;
        level.setBlock(pos.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(pos, com.rtshop.enhance.ModEnhance.PSHOP.get().defaultBlockState(), 3);
        check("the player shop block has its block entity", level.getBlockEntity(pos) instanceof com.rtshop.pshop.PlayerShopBlockEntity);
        if (!(level.getBlockEntity(pos) instanceof com.rtshop.pshop.PlayerShopBlockEntity be)) return;
        be.owner = a.getUUID();
        be.ownerName = "rtshop_test_a";
        a.setPos(pos.getX() + 1.5, pos.getY(), pos.getZ() + 0.5);
        b.setPos(pos.getX() - 0.5, pos.getY(), pos.getZ() + 0.5);
        // NPC 상점에 있는 물건 하나를 골라 비교가 나오는지 본다
        net.minecraft.world.item.Item ware = null;
        for (com.rtshop.shop.ShopData d : ShopServer.shops().values()) {
            for (com.rtshop.shop.ShopData.Entry e : d.entries) {
                if (e.buyable && !e.stack.hasTag() && e.stack.getMaxStackSize() == 64) {
                    ware = e.stack.getItem();
                    break;
                }
            }
            if (ware != null) break;
        }
        check("an item from an NPC shop was found for the comparison", ware != null);
        if (ware == null) return;
        check("NPC shops selling the item are listed with their price", com.rtshop.pshop.PlayerShops.npcPrices(new ItemStack(ware)).size() > 0);
        a.getInventory().clearContent();
        b.getInventory().clearContent();
        a.getInventory().setItem(0, new ItemStack(ware, 40));
        b.getInventory().setItem(0, new ItemStack(ware, 10));
        // 남은 올리지 못한다
        com.rtshop.pshop.PlayerShops.handle(b, pos, com.rtshop.pshop.PlayerShops.ADD, "{\"slot\":0,\"price\":5,\"n\":10}");
        check("someone else cannot put items on your shop", be.listings.isEmpty() && b.getInventory().getItem(0).getCount() == 10);
        com.rtshop.pshop.PlayerShops.handle(a, pos, com.rtshop.pshop.PlayerShops.ADD, "{\"slot\":0,\"price\":7,\"n\":30}");
        check("the owner lists 30 items at 7G each", be.listings.size() == 1 && be.listings.get(0).stock == 30 && be.listings.get(0).price == 7
                && a.getInventory().getItem(0).getCount() == 10, be.listings.isEmpty() ? "none" : be.listings.get(0).stock);
        com.rtshop.economy.Economy.setWallet(a, 0);
        EconomyData ed = EconomyData.get(server);
        long bankBefore = ed.bank(a.getUUID());
        com.rtshop.economy.Economy.setWallet(b, 50);
        com.rtshop.pshop.PlayerShops.handle(b, pos, com.rtshop.pshop.PlayerShops.BUY, "{\"i\":0,\"n\":10}");
        check("a buyer without enough money cannot buy", be.listings.get(0).stock == 30 && com.rtshop.economy.Economy.wallet(b) == 50);
        com.rtshop.pshop.PlayerShops.handle(b, pos, com.rtshop.pshop.PlayerShops.BUY, "{\"i\":0,\"n\":5}");
        check("buying 5 takes 35G and gives the items", com.rtshop.economy.Economy.wallet(b) == 15 && be.listings.get(0).stock == 25
                && countOf(b, ware) == 15, com.rtshop.economy.Economy.wallet(b) + "G, " + countOf(b, ware));
        check("the money goes to the owner's bank", ed.bank(a.getUUID()) == bankBefore + 35, ed.bank(a.getUUID()) - bankBefore);
        com.rtshop.pshop.PlayerShops.handle(a, pos, com.rtshop.pshop.PlayerShops.BUY, "{\"i\":0,\"n\":1}");
        check("the owner cannot buy from their own shop", be.listings.get(0).stock == 25);
        com.rtshop.pshop.PlayerShops.handle(b, pos, com.rtshop.pshop.PlayerShops.PRICE, "{\"i\":0,\"price\":1}");
        check("someone else cannot change the price", be.listings.get(0).price == 7);
        com.rtshop.pshop.PlayerShops.handle(a, pos, com.rtshop.pshop.PlayerShops.PRICE, "{\"i\":0,\"price\":9}");
        com.rtshop.pshop.PlayerShops.handle(a, pos, com.rtshop.pshop.PlayerShops.TAKE, "{\"i\":0,\"n\":5}");
        check("the owner changes the price and takes 5 back", be.listings.get(0).price == 9 && be.listings.get(0).stock == 20 && countOf(a, ware) == 15);
        com.rtshop.pshop.PlayerShops.handle(a, pos, com.rtshop.pshop.PlayerShops.RESTOCK, "{\"i\":0,\"n\":15}");
        check("restocking from the bag", be.listings.get(0).stock == 35 && countOf(a, ware) == 0);
        // 저장 후 다시 읽기
        net.minecraft.nbt.CompoundTag tag = be.saveWithoutMetadata();
        com.rtshop.pshop.PlayerShopBlockEntity copy = new com.rtshop.pshop.PlayerShopBlockEntity(pos, level.getBlockState(pos));
        copy.load(tag);
        check("the shop is saved with its owner and listings", copy.owner.equals(a.getUUID()) && copy.listings.size() == 1
                && copy.listings.get(0).stock == 35 && copy.totalSales == 35);
        com.rtshop.pshop.PlayerShops.handle(a, pos, com.rtshop.pshop.PlayerShops.REMOVE, "{\"i\":0}");
        check("taking the listing down returns the stock", be.listings.isEmpty() && countOf(a, ware) == 35);
        net.minecraftforge.event.level.BlockEvent.BreakEvent ev = new net.minecraftforge.event.level.BlockEvent.BreakEvent(level, pos, level.getBlockState(pos), b);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(ev);
        check("someone else cannot break the shop", ev.isCanceled());
        employees(server, level, a, b, pos, be, ware);
        java.util.UUID clerk = com.rtshop.pshop.Employees.entry(level, pos) == null ? null : com.rtshop.pshop.Employees.entry(level, pos).entity;
        level.removeBlock(pos, false);
        check("the first clerk leaves with the stand too", clerk == null || level.getEntity(clerk) == null);
        level.setBlock(pos.below(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        a.getInventory().clearContent();
        b.getInventory().clearContent();
    }

    private static String trKey(net.minecraft.network.chat.Component c) {
        return c != null && c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc ? tc.getKey() : String.valueOf(c);
    }

    /** 플레이어 상점 직원: 고용, 이름, 우클릭, 고용비(예금 → 지갑), 모자라면 휴무, [고용비 내기], 진열대를 부수면 퇴사. */
    private static void employees(MinecraftServer server, ServerLevel level, FakePlayer a, FakePlayer b, BlockPos pos,
                                  com.rtshop.pshop.PlayerShopBlockEntity be, net.minecraft.world.item.Item ware) {
        LINES.add("");
        LINES.add("[player shop clerks]");
        com.rtshop.pshop.Employees.Data sd = com.rtshop.pshop.Employees.data(server);
        com.rtshop.pshop.Employees.Entry e1 = com.rtshop.pshop.Employees.hire(level, pos, be);
        net.minecraft.world.entity.Entity v1 = e1 == null ? null : level.getEntity(e1.entity);
        check("a stand with an owner gets a clerk standing right next to it", v1 instanceof net.minecraft.world.entity.npc.Villager
                && com.rtshop.pshop.Employees.isEmployee(v1) && v1.distanceToSqr(net.minecraft.world.phys.Vec3.atBottomCenterOf(pos)) <= 2.5, v1);
        check("the clerk is a protected NPC with a name tag", v1 != null && ShopServer.isNpc(v1) && v1.hasCustomName()
                && trKey(v1.getCustomName()).equals("rtshop.pshop.emp_label"), v1 == null ? "none" : v1.getCustomName());
        check("hiring again keeps the same clerk", com.rtshop.pshop.Employees.hire(level, pos, be) == e1 && level.getEntity(e1.entity) == v1);
        if (v1 != null) {
            net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract ie = new net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract(b, net.minecraft.world.InteractionHand.MAIN_HAND, v1);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(ie);
            check("right-clicking the clerk opens the stand (no villager trading)", ie.isCanceled());
        }
        BlockPos pos2 = pos.offset(4, 0, 0);
        level.setBlock(pos2.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(pos2, com.rtshop.enhance.ModEnhance.PSHOP.get().defaultBlockState(), 3);
        com.rtshop.pshop.PlayerShopBlockEntity be2 = (com.rtshop.pshop.PlayerShopBlockEntity) level.getBlockEntity(pos2);
        be2.owner = b.getUUID();
        be2.ownerName = "rtshop_test_b";
        com.rtshop.pshop.Employees.Entry e2 = com.rtshop.pshop.Employees.hire(level, pos2, be2);
        check("each owner's clerk has a different name", e2 != null && !e2.name.isEmpty() && !e2.name.equals(e1.name), e1.name + " / " + (e2 == null ? "-" : e2.name));
        check("a newly hired clerk works on the first day for free", com.rtshop.pshop.Employees.working(level, pos2));
        // 다음 날: 출석 초기화 시각이 지나 고용비를 낸다
        java.util.function.LongSupplier oldClock = com.rtshop.attendance.Attendance.clock;
        long now = System.currentTimeMillis();
        try {
            com.rtshop.attendance.Attendance.clock = () -> now;
            sd.lastDay = com.rtshop.pshop.Employees.today();
            com.rtshop.attendance.Attendance.clock = () -> now + 86_400_000L;
            long tomorrow = com.rtshop.pshop.Employees.today();
            EconomyData ed = EconomyData.get(server);
            ed.setBank(a.getUUID(), 600);
            ed.setWallet(a.getUUID(), 600);
            ed.setBank(b.getUUID(), 0);
            ed.setWallet(b.getUUID(), 300);
            sd.pending.remove(b.getUUID());
            com.rtshop.pshop.Employees.payDay(server);
            check("at the daily reset the wage is taken from the bank first, then the wallet",
                    ed.bank(a.getUUID()) == 0 && ed.wallet(a.getUUID()) == 200 && e1.paidDay == tomorrow,
                    ed.bank(a.getUUID()) + " / " + ed.wallet(a.getUUID()));
            check("an owner who cannot pay keeps the money and the clerk takes the day off",
                    ed.wallet(b.getUUID()) == 300 && !com.rtshop.pshop.Employees.working(level, pos2));
            check("the owner gets a wage notice (kept for when they log in)", sd.pending.containsKey(b.getUUID())
                    && sd.pending.get(b.getUUID()).stream().anyMatch(m -> m.contains("rtshop.pshop.wage_short")), sd.pending.get(b.getUUID()));
            com.rtshop.pshop.Employees.payDay(server);
            check("the wage is taken only once a day", ed.wallet(a.getUUID()) == 200);
            // 쉬는 날에는 살 수 없다
            com.rtshop.pshop.PlayerShopBlockEntity.Listing l = new com.rtshop.pshop.PlayerShopBlockEntity.Listing();
            l.item = new ItemStack(ware);
            l.price = 1;
            l.stock = 5;
            be2.listings.add(l);
            com.rtshop.economy.Economy.setWallet(a, 100);
            a.setPos(pos2.getX() + 1.5, pos2.getY(), pos2.getZ() + 0.5);
            com.rtshop.pshop.PlayerShops.handle(a, pos2, com.rtshop.pshop.PlayerShops.BUY, "{\"i\":0,\"n\":1}");
            check("customers cannot buy while the clerk is off", l.stock == 5 && trKey(com.rtshop.pshop.PlayerShops.lastResult).equals("rtshop.pshop.closed_today"),
                    trKey(com.rtshop.pshop.PlayerShops.lastResult));
            b.setPos(pos2.getX() - 0.5, pos2.getY(), pos2.getZ() + 0.5);
            ed.setWallet(b.getUUID(), 2500);
            com.rtshop.pshop.PlayerShops.handle(b, pos2, com.rtshop.pshop.PlayerShops.PAY_WAGE, "{}");
            check("[Pay wage] brings the clerk back to work right away", com.rtshop.pshop.Employees.working(level, pos2) && ed.wallet(b.getUUID()) == 1500,
                    ed.wallet(b.getUUID()));
            com.rtshop.pshop.PlayerShops.handle(a, pos2, com.rtshop.pshop.PlayerShops.BUY, "{\"i\":0,\"n\":1}");
            check("...and customers can buy again", l.stock == 4);
        } finally {
            com.rtshop.attendance.Attendance.clock = oldClock;
        }
        // 직원이 사라지면 다시 선다, 진열대를 부수면 떠난다
        net.minecraft.world.entity.Entity v2 = level.getEntity(e2.entity);
        if (v2 != null) v2.discard();
        com.rtshop.pshop.Employees.checkStaff(server, true);
        check("a missing clerk comes back to the stand", level.getEntity(e2.entity) instanceof net.minecraft.world.entity.npc.Villager nv && nv.isAlive());
        java.util.UUID id2 = e2.entity;
        level.removeBlock(pos2, false);
        level.setBlock(pos2.below(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        check("breaking the stand sends the clerk away", level.getEntity(id2) == null && com.rtshop.pshop.Employees.entry(level, pos2) == null);
        a.setPos(pos.getX() + 1.5, pos.getY(), pos.getZ() + 0.5);
    }

    private static int countOf(FakePlayer p, net.minecraft.world.item.Item item) {
        int n = 0;
        for (ItemStack s : p.getInventory().items) {
            if (s.is(item)) n += s.getCount();
        }
        return n;
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
                boolean bos = nbt.contains("\"katana:");
                if (gi >= 3) {
                    topN++;
                    if (gi == 5 && !java.util.Arrays.asList(com.rtshop.gacha.GachaConfig.YAMATO_ITEMS).contains(e.item)) {
                        badW.add("not yamato " + e.item);
                    }
                    if (bos && gi != 3) badW.add("blade of shadow not epic " + e.nbt);
                    if (!ns.equals("efn") && !(lr && nbt.contains("karambit")) && !bos) {
                        badW.add(e.item + "@" + e.grade);
                    }
                } else if (!ns.equals("epicfight") && !(lr && (nbt.contains("baseball") || nbt.contains("dagger")) && gi == 2)) {
                    badW.add(e.item + "@" + e.grade);
                }
            }
            if (c.pool("weapon").stream().filter(e -> e.item.startsWith("efn:")).count() != 12) {
                badW.add("efn count " + c.pool("weapon").stream().filter(e -> e.item.startsWith("efn:")).count());
            }
            java.util.Map<String, String> want = java.util.Map.of("efn:hf_murasama", "legendary", "efn:yamato_dmc_in_sheath", "yamato",
                    "efn:kusabimaru", "epic", "efn:ruinsgreatsword", "epic");
            for (var w : want.entrySet()) {
                if (c.pool("weapon").stream().noneMatch(e -> e.item.equals(w.getKey()) && e.grade.equals(w.getValue()))) {
                    badW.add("missing " + w.getKey());
                }
            }
            if (c.pool("weapon").stream().filter(e -> e.nbt.contains("\"katana:")).count() != 17) {
                badW.add("blade of shadow count " + c.pool("weapon").stream().filter(e -> e.nbt.contains("\"katana:")).count());
            }
            check("weapons: the 12 fixed Nightfall weapons, the karambit and the 17 Blade of Shadow weapons are epic+ (Yamato alone is YAMATO), the rest are Epic Fight base weapons",
                    topN > 0 && badW.isEmpty(), badW);
            StringBuilder sb = new StringBuilder();
            for (String cat : com.rtshop.gacha.GachaConfig.CATEGORIES) {
                int[] g = new int[com.rtshop.gacha.GachaConfig.GRADES.length];
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
        boolean yamatoHere = c.pool("weapon").stream().anyMatch(e -> e.grade.equals("yamato") && com.rtshop.gacha.GachaConfig.valid(e));
        check("YAMATO is 0.04% in the high weapon draw (only when the Yamato exists), never in pet/accessory draws or lower tiers",
                (!yamatoHere || Math.abs(hw[5] - 0.0004) < 1e-9)
                        && com.rtshop.gacha.Gacha.gradeChances(c, "pet", "high")[5] == 0
                        && com.rtshop.gacha.Gacha.gradeChances(c, "weapon", "mid")[5] == 0, java.util.Arrays.toString(hw));
        // 3판 설정 → 4판: 목록은 그대로, 야마토는 전설에서 야마토로, Blade of Shadow 가 영웅으로 더해지고 고급 뽑기에 0.04%
        com.google.gson.JsonObject v3 = c.toJson();
        v3.addProperty("configVersion", 3);
        com.rtshop.gacha.GachaConfig up = com.rtshop.gacha.GachaConfig.fromJson(v3);
        for (com.rtshop.gacha.GachaConfig.Entry e : up.pool("weapon")) if (e.grade.equals("yamato")) e.grade = "legendary";
        up.pool("weapon").removeIf(e -> e.nbt.contains("\"katana:"));
        up.tiers.get("high").weights[5] = 0;
        up.tiers.get("high").weights[1] = 70;
        int beforeUp = up.pool("weapon").size();
        com.rtshop.gacha.GachaConfig.upgradeTo4ForTest(up);
        boolean movedYamato = up.pool("weapon").stream().filter(e -> java.util.Arrays.asList(com.rtshop.gacha.GachaConfig.YAMATO_ITEMS).contains(e.item))
                .allMatch(e -> e.grade.equals("yamato"));
        check("upgrading an old gacha.json moves Yamato to YAMATO, adds the 17 Blade of Shadow weapons as epic and sets 0.04% (keeping the list)",
                movedYamato && up.pool("weapon").size() == beforeUp + 17 && up.tiers.get("high").weights[5] == 0.04
                        && Math.abs(up.tiers.get("high").weights[1] - 69.96) < 1e-9, up.pool("weapon").size() + " vs " + beforeUp);
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

        // 등급 추가·삭제 (3.52): 직접 만든 등급을 끼워 넣고, 기본 등급을 끄고, 지운 뒤에는 원래대로
        java.util.List<com.rtshop.gacha.GachaGrades.Def> gradesBefore = com.rtshop.gacha.GachaGrades.all();
        com.rtshop.magic.MagicConfig magicBefore = com.rtshop.magic.MagicConfig.get();
        double uncBefore = magicBefore.selectOdds[com.rtshop.gacha.GachaConfig.gradeIndex("uncommon")];
        double failBefore = magicBefore.enchantOdds[com.rtshop.magic.MagicConfig.failIndex()];
        try {
            com.google.gson.JsonObject gj = c.toJson();
            com.google.gson.JsonArray gdefs = new com.google.gson.JsonArray();
            for (String gid : new String[]{"common", "uncommon", "rare", "epic", "legendary"}) {
                com.google.gson.JsonObject d = new com.google.gson.JsonObject();
                d.addProperty("id", gid);
                if (gid.equals("rare")) d.addProperty("enabled", false);
                gdefs.add(d);
            }
            com.google.gson.JsonObject myth = new com.google.gson.JsonObject();
            myth.addProperty("id", "mythic");
            myth.addProperty("name", "신화");
            myth.addProperty("color", "E85A6A");
            myth.addProperty("fx", 4);
            gdefs.add(myth); // 야마토는 적지 않는다: 빠진 기본 등급은 제자리에 끼워진다
            gj.add("grades", gdefs);
            com.google.gson.JsonArray gp = new com.google.gson.JsonArray();
            String[][] rowsG = {{"minecraft:wooden_sword", "common"}, {"minecraft:iron_sword", "rare"}, {"minecraft:netherite_sword", "mythic"}};
            for (String[] rg : rowsG) {
                com.google.gson.JsonObject e = new com.google.gson.JsonObject();
                e.addProperty("item", rg[0]);
                e.addProperty("count", 1);
                e.addProperty("grade", rg[1]);
                e.addProperty("weight", 10);
                gp.add(e);
            }
            gj.getAsJsonObject("pools").add("weapon", gp);
            com.google.gson.JsonObject highOdds = new com.google.gson.JsonObject();
            highOdds.addProperty("common", 50);
            highOdds.addProperty("rare", 30);
            highOdds.addProperty("mythic", 20);
            gj.getAsJsonObject("tiers").add("high", highOdds);
            com.rtshop.gacha.GachaConfig g7 = com.rtshop.gacha.GachaConfig.fromJson(gj);
            String[] gradesNow = com.rtshop.gacha.GachaConfig.GRADES;
            check("a custom grade is added between the built-in ones (and a missing built-in grade is put back in its place): common … legendary, mythic, yamato",
                    gradesNow.length == 7 && gradesNow[5].equals("mythic") && gradesNow[6].equals("yamato") && com.rtshop.gacha.GachaConfig.yamato() == 6
                            && com.rtshop.gacha.GachaGrades.fx("mythic") == 4 && com.rtshop.gacha.GachaGrades.color("mythic") == 0xE85A6A
                            && "신화".equals(com.rtshop.gacha.GachaGrades.customName("mythic")) && g7.dismantle.length == 7
                            && g7.dismantle[5] >= 150 && g7.dismantle[5] <= 600, java.util.Arrays.toString(gradesNow) + " " + java.util.Arrays.toString(g7.dismantle));
            double[] chances7 = com.rtshop.gacha.Gacha.gradeChances(g7, "weapon", "high");
            net.minecraft.util.RandomSource grng = net.minecraft.util.RandomSource.create(7);
            int rareSeen = 0, mythSeen = 0;
            for (int k = 0; k < 2000; k++) {
                String got = com.rtshop.gacha.Gacha.pull(g7, "weapon", "high", 1, grng).get(0).grade;
                if (got.equals("rare")) rareSeen++;
                if (got.equals("mythic")) mythSeen++;
            }
            check("a switched-off grade never drops and its share goes to the others; the custom grade drops at its own odds (20 of 70)",
                    chances7.length == 7 && chances7[2] == 0 && Math.abs(chances7[0] - 50.0 / 70) < 1e-9 && Math.abs(chances7[5] - 20.0 / 70) < 1e-9 && rareSeen == 0
                            && Math.abs(mythSeen / 2000.0 - 20.0 / 70) < 0.04, java.util.Arrays.toString(chances7) + " rare " + rareSeen + " mythic " + mythSeen);
            com.rtshop.gacha.GachaConfig reread = com.rtshop.gacha.GachaConfig.fromJson(g7.toJson());
            check("the grade list is written to gacha.json and read back the same",
                    java.util.Arrays.equals(com.rtshop.gacha.GachaConfig.GRADES, gradesNow) && !com.rtshop.gacha.GachaGrades.enabled("rare")
                            && reread.tiers.get("high").weights[5] == 20 && reread.pool("weapon").size() == 3);
            com.rtshop.magic.MagicConfig m7 = com.rtshop.magic.MagicConfig.get();
            check("the magic gacha follows the new grade list: its odds move with their grades (the fail slot stays on the common grade, the new grade starts at 0)",
                    m7.selectOdds.length == 7 && m7.selectOdds[com.rtshop.gacha.GachaConfig.gradeIndex("uncommon")] == uncBefore && m7.selectOdds[5] == 0
                            && m7.enchantOdds[com.rtshop.magic.MagicConfig.failIndex()] == failBefore,
                    java.util.Arrays.toString(m7.selectOdds));
            int refusedGrades = 0;
            String[] badLists = {"[{\"id\":\"a\"},{\"id\":\"a\"}]", "[{\"id\":\"My Grade\"}]", "[{\"id\":\"rare\"},{\"id\":\"uncommon\"}]"};
            for (String bad : badLists) {
                try {
                    com.rtshop.gacha.GachaGrades.parse(bad);
                } catch (IllegalArgumentException ex) {
                    refusedGrades++;
                }
            }
            check("a bad grade list is refused: the same ID twice, an ID with capitals or spaces, built-in grades out of order", refusedGrades == 3, refusedGrades);
        } finally {
            com.rtshop.gacha.GachaGrades.apply(gradesBefore);
        }
        com.rtshop.magic.MagicConfig magicAfter = com.rtshop.magic.MagicConfig.get();
        check("after the trial the six built-in grades are back, and the magic gacha's odds are where they were",
                com.rtshop.gacha.GachaConfig.GRADES.length == gradesBefore.size() && magicAfter.selectOdds.length == gradesBefore.size()
                        && magicAfter.selectOdds[com.rtshop.gacha.GachaConfig.gradeIndex("uncommon")] == uncBefore
                        && magicAfter.enchantOdds[com.rtshop.magic.MagicConfig.failIndex()] == failBefore,
                java.util.Arrays.toString(com.rtshop.gacha.GachaConfig.GRADES));

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
        // 가방 빈칸이 뽑을 개수보다 적으면 뽑기를 막는다 (티켓은 그대로)
        a.getInventory().items.set(0, new ItemStack(ModItems.TICKET_NORMAL.get(), 10));
        for (int i = 1; i < a.getInventory().items.size() - 5; i++) {
            a.getInventory().items.set(i, new ItemStack(net.minecraft.world.item.Items.DIRT, 64));
        }
        // 빈칸 5 + 티켓 10장을 다 쓰면 비는 칸 1 = 6칸: 10회는 막히고 1회는 된다
        boolean tenBlocked = com.rtshop.gacha.Gacha.doPull(a, "weapon", "normal", 10, false) == null
                && count(a, st -> st.is(ModItems.TICKET_NORMAL.get())) == 10;
        boolean oneOk = com.rtshop.gacha.Gacha.doPull(a, "weapon", "normal", 1, false) != null;
        check("a pull is refused (tickets kept) when the inventory lacks free slots for every item", tenBlocked && oneOk
                && com.rtshop.gacha.Gacha.freeSlotsAfterTaking(a, ModItems.TICKET_NORMAL.get(), 9) == 5, tenBlocked + " " + oneOk);
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
        // 영웅 이상 뽑기 장비는 내구도가 닳지 않는다 (희귀는 그대로)
        {
            com.rtshop.gacha.GachaConfig.Entry ep = new com.rtshop.gacha.GachaConfig.Entry();
            ep.item = "minecraft:diamond_sword";
            ep.grade = "epic";
            ItemStack es = com.rtshop.gacha.Gacha.stackOf(ep, "weapon");
            com.rtshop.gacha.GachaConfig.Entry rr = new com.rtshop.gacha.GachaConfig.Entry();
            rr.item = "minecraft:diamond_sword";
            rr.grade = "rare";
            ItemStack rs = com.rtshop.gacha.Gacha.stackOf(rr, "weapon");
            boolean wasDamageable = es.isDamageableItem();
            es.hurt(50, level.random, null);
            check("epic+ gacha gear does not lose durability (rare still does)",
                    !wasDamageable && es.getDamageValue() == 0 && rs.isDamageableItem(), es.getTag() + " / " + rs.getTag());
            // 예전에 뽑은 장비: 표시만 있고 보호가 없던 것도 가방에서 보호된다
            ItemStack old = es.copy();
            old.getTag().remove("Unbreakable");
            old.setDamageValue(100);
            boolean fixed = com.rtshop.gacha.Gacha.protect(old);
            check("old epic+ gacha gear gets protected and repaired", fixed && !old.isDamageableItem() && old.getDamageValue() == 0, old.getTag());
        }
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

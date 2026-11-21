package com.rtshop.client;

import com.rtshop.RtShop;
import com.rtshop.economy.Economy;
import com.rtshop.economy.EconomyData;
import com.rtshop.farm.CropData;
import com.rtshop.farm.SeedRules;
import com.rtshop.item.DeedItem;
import com.rtshop.item.ModItems;
import com.rtshop.land.LandData;
import com.rtshop.land.LandManager;
import com.rtshop.net.BankPacket;
import com.rtshop.net.LandActionPacket;
import com.rtshop.net.PinActionPacket;
import com.rtshop.net.SellAllPacket;
import com.rtshop.net.ShopNet;
import com.rtshop.net.TransactionPacket;
import com.rtshop.shop.ShopData;
import com.rtshop.shop.ShopServer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 클라이언트 화면 자체 점검. JVM 옵션 -Drtshop.clienttest=true 로 켰을 때만 동작한다 (일반 게임에서는 아무 일도 하지 않음).
 * 싱글플레이 월드에 들어가면 상점·은행·토지·PIN 화면을 차례로 열어 보고 screenshots 폴더에 사진을 남긴 뒤 게임을 끈다.
 * 빌드할 때 가상 화면에서 자동으로 돌려, 화면을 여는 순간 튕기는 문제를 미리 잡기 위한 용도.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID, value = Dist.CLIENT)
public final class ClientSelfTest {
    private ClientSelfTest() {
    }

    private static final boolean ENABLED = Boolean.getBoolean("rtshop.clienttest");
    private static final List<String> LINES = new ArrayList<>();
    private static int step = 0;
    private static int wait = 0;
    private static int idle = 0;
    private static int passed = 0;
    private static int failed = 0;
    private static boolean done = false;
    private static volatile String serverError = null;
    private static volatile BlockPos chestPos = null;
    private static volatile int landId = -1;
    private static volatile int serverPerms = -1;
    private static volatile boolean serverPin = false;
    private static long before = 0;
    private static long expected = 0;
    private static volatile BlockPos cropPos = null;
    private static volatile String harvest = null;
    private static final int LAST = 63;
    private static volatile int jobNpc = -1;
    /** 상인 NPC 를 실제로 우클릭해 보는 단계 (상호작용 때 다른 모드와 부딪혀 튕기는지). */
    private static final String[] NPCS = {"fisher", "farmer", "food", "blacksmith", "general", "land", "bank",
            "attendance", "gacha", "job"};
    private static final int FX_END = LAST + 18 + 10;
    private static final int NPC_END = FX_END + NPCS.length * 3;
    private static volatile int gachaNpc = -1;
    private static volatile int testNpc = -1;
    private static final int EN_STEPS = 6;
    private static final int ENH_STEPS = 9;
    private static final int ENH_END = NPC_END + ENH_STEPS;
    private static volatile BlockPos anvilPos = null;

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || done || event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) {
            idle++;
            try {
                autoEnter(mc);
            } catch (Throwable t) {
                RtShop.LOGGER.error("[rtshop-clienttest] could not open the test world", t);
            }
            if (idle > 20 * 600) {
                check("entered a singleplayer world", false, mc.screen == null ? "no screen" : mc.screen.getClass().getName());
                finish(mc);
            }
            return;
        }
        if (wait > 0) {
            wait--;
            return;
        }
        try {
            next(mc);
        } catch (Throwable t) {
            check("step " + step + " ran without an exception", false, t);
            RtShop.LOGGER.error("[rtshop-clienttest] step {} failed", step, t);
            mc.setScreen(null);
            wait = 10;
        }
        step++;
    }

    private static boolean opening = false;

    /**
     * 런처로 실행해 "빠른 시작" 인자가 없을 때: 타이틀 화면에서 시험용 세계(rtshoptest)를 직접 연다.
     * 세계를 여는 도중에 뜨는 확인 화면(실험적 설정 경고, 백업 권유 등)은 진행 버튼을 누른다.
     */
    private static void autoEnter(Minecraft mc) {
        if (idle < 100 || idle % 40 != 0 || mc.level != null) {
            return;
        }
        net.minecraft.client.gui.screens.Screen s = mc.screen;
        if (s == null) {
            return;
        }
        if (s instanceof net.minecraft.client.gui.screens.TitleScreen) {
            if (!opening) {
                opening = true;
                RtShop.LOGGER.info("[rtshop-clienttest] opening the test world from the title screen");
                mc.createWorldOpenFlows().loadLevel(s, "rtshoptest");
            }
            return;
        }
        String cls = s.getClass().getName();
        if (idle % 200 == 0) {
            RtShop.LOGGER.info("[rtshop-clienttest] waiting on screen {}", cls);
        }
        boolean confirm = s instanceof net.minecraft.client.gui.screens.ConfirmScreen
                || s instanceof net.minecraft.client.gui.screens.BackupConfirmScreen
                || cls.contains("Confirm") || cls.contains("Warning");
        if (!confirm) {
            // 다른 모드가 띄운 안내 창 (예: Fantasy Mounts 의 단축키 겹침 안내): 닫고 넘어간다
            boolean loading = s instanceof net.minecraft.client.gui.screens.LevelLoadingScreen
                    || s instanceof net.minecraft.client.gui.screens.ProgressScreen
                    || s instanceof net.minecraft.client.gui.screens.ReceivingLevelScreen
                    || s instanceof net.minecraft.client.gui.screens.GenericDirtMessageScreen
                    || cls.contains("Loading") || cls.contains("Progress");
            if (!loading && (cls.contains("Clash") || idle % 400 == 0)) {
                RtShop.LOGGER.info("[rtshop-clienttest] closing {}", cls);
                s.onClose();
                if (mc.screen == s) {
                    mc.setScreen(new net.minecraft.client.gui.screens.TitleScreen());
                }
            }
            return;
        }
        net.minecraft.client.gui.components.AbstractButton pick = null;
        for (net.minecraft.client.gui.components.events.GuiEventListener child : s.children()) {
            if (!(child instanceof net.minecraft.client.gui.components.AbstractButton b) || !b.active || !b.visible) {
                continue;
            }
            String key = b.getMessage().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc ? tc.getKey() : "";
            if (key.equals("gui.cancel") || key.equals("gui.back") || key.equals("gui.no") || key.equals("selectWorld.backupJoinConfirmButton")) {
                continue;
            }
            if (key.equals("selectWorld.backupJoinSkipButton") || key.equals("gui.proceed") || key.equals("gui.yes") || key.equals("gui.continue")) {
                pick = b;
                break;
            }
            if (pick == null) {
                pick = b;
            }
        }
        if (pick != null) {
            RtShop.LOGGER.info("[rtshop-clienttest] pressing '{}' on {}", pick.getMessage().getString(), cls);
            pick.onPress();
        }
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
        RtShop.LOGGER.info("[rtshop-clienttest] {}", line);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), c -> {
        });
    }

    private static void onServer(Minecraft mc, Consumer<ServerPlayer> action) {
        IntegratedServer server = mc.getSingleplayerServer();
        UUID id = mc.player.getUUID();
        server.execute(() -> {
            try {
                ServerPlayer sp = server.getPlayerList().getPlayer(id);
                if (sp != null) {
                    action.accept(sp);
                }
            } catch (Throwable t) {
                serverError = String.valueOf(t);
                RtShop.LOGGER.error("[rtshop-clienttest] server-side action failed", t);
            }
        });
    }

    private static ItemStack starred(Item item, int count, int star) {
        ItemStack s = new ItemStack(item, count);
        if (star > 0) {
            SeedRules.setStar(s, star);
        }
        return s;
    }

    private static ItemStack fish(Item item, int count, float percentile, String rarity) {
        ItemStack s = new ItemStack(item, count);
        CompoundTag info = new CompoundTag();
        info.putFloat("size", 42.0F);
        info.putFloat("weight", 1850.0F);
        info.putFloat("percentile", percentile);
        info.putString("rarity", rarity);
        s.getOrCreateTag().put(ShopData.FISH_INFO_TAG, info);
        return s;
    }

    /** 뽑기 종류별 화면, 티켓 교환, 영웅·전설 연출의 장면들. */
    private static void gachaStep(Minecraft mc, int k) {
        GachaScreen gs = mc.screen instanceof GachaScreen s ? s : null;
        switch (k) {
            case 0 -> {
                mc.setScreen(null);
                onServer(mc, sp -> {
                    sp.getInventory().add(new ItemStack(com.rtshop.item.ModItems.SHARD.get(), 64));
                    com.rtshop.gacha.Gacha.open(sp, gachaNpc);
                });
                wait = 20;
            }
            case 1 -> {
                check("the gacha window opens again", gs != null, mc.screen);
                if (gs != null) {
                    gs.showForTest(1, 0, -1);
                }
                wait = 12;
            }
            case 2 -> {
                shot(mc, "51_gacha_pet");
                if (gs != null) {
                    gs.showForTest(2, 0, -1);
                }
                wait = 12;
            }
            case 3 -> {
                shot(mc, "52_gacha_accessory");
                if (gs != null) {
                    gs.showForTest(2, 1, -1);
                }
                wait = 5;
            }
            case 4 -> {
                shot(mc, "53_gacha_ticket_exchange");
                if (gs != null) {
                    gs.fxForTest(3, 28);
                }
                wait = 2;
            }
            case 5 -> {
                shot(mc, "54_fx_epic_charge");
                check("the epic animation plays", gs != null && gs.fxActiveForTest());
                if (gs != null) {
                    gs.fxForTest(3, 48);
                }
                wait = 2;
            }
            case 6 -> {
                shot(mc, "55_fx_epic_reveal");
                if (gs != null) {
                    gs.fxForTest(4, 40);
                }
                wait = 2;
            }
            case 7 -> {
                shot(mc, "56_fx_legendary_charge");
                if (gs != null) {
                    gs.fxForTest(4, 58);
                }
                wait = 2;
            }
            case 8 -> {
                shot(mc, "57_fx_legendary_upgrade");
                if (gs != null) {
                    gs.fxForTest(4, 100);
                }
                wait = 2;
            }
            default -> {
                shot(mc, "58_fx_legendary_reveal");
                check("the legendary animation plays", gs != null && gs.fxActiveForTest());
                wait = 5;
            }
        }
    }


    private static ItemStack leveled(Item item, int lv) {
        ItemStack s = new ItemStack(item);
        com.rtshop.enhance.Enhance.setLevel(s, lv);
        return s;
    }

    /** 강화 모루: 우클릭으로 열기(Cinematic 이 있으면 모루 장면), 재료·확률 패널, 성공·방지권 연출. */
    private static void enhanceStep(Minecraft mc, int k) {
        switch (k) {
            case 0 -> {
                mc.setScreen(null);
                anvilPos = null;
                onServer(mc, sp -> {
                    BlockPos at = sp.blockPosition().relative(sp.getDirection(), 2);
                    sp.serverLevel().setBlockAndUpdate(at.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                    sp.serverLevel().setBlockAndUpdate(at, com.rtshop.enhance.ModEnhance.ANVIL.get().defaultBlockState());
                    sp.serverLevel().setBlockAndUpdate(at.above(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                    com.rtshop.enhance.EnhanceConfig cfg = com.rtshop.enhance.EnhanceConfig.get();
                    for (int lv : new int[]{5, 6}) {
                        for (com.rtshop.enhance.Enhance.Need n : com.rtshop.enhance.Enhance.needs(cfg, cfg.level(lv), null)) {
                            sp.getInventory().add(new ItemStack(n.items.get(0), n.need * 2));
                        }
                    }
                    sp.getInventory().add(new ItemStack(com.rtshop.item.ModItems.GUARD_DESTROY.get(), 3));
                    com.rtshop.economy.Economy.setWallet(sp, 200000);
                    anvilPos = at;
                });
                wait = 10;
            }
            case 1 -> {
                BlockPos at = anvilPos;
                check("the enhance anvil was placed", at != null && mc.level.getBlockState(at).is(com.rtshop.enhance.ModEnhance.ANVIL.get()));
                if (at != null) {
                    net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(
                            net.minecraft.world.phys.Vec3.atCenterOf(at).add(0, 0.5, 0), net.minecraft.core.Direction.UP, at, false);
                    try {
                        mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
                    } catch (Throwable t) {
                        check("right-clicking the enhance anvil does not crash", false, t);
                    }
                }
                wait = net.minecraftforge.fml.ModList.get().isLoaded("cinematic") ? 120 : 20;
            }
            case 2 -> {
                boolean open = mc.player.containerMenu instanceof com.rtshop.enhance.EnhanceMenu;
                check("right-clicking the enhance anvil opens the enhance menu"
                        + (net.minecraftforge.fml.ModList.get().isLoaded("cinematic") ? " (through the Cinematic anvil scene)" : ""), open,
                        mc.screen == null ? "no screen" : mc.screen.getClass().getName());
                if (!net.minecraftforge.fml.ModList.get().isLoaded("cinematic")) {
                    check("without Cinematic the enhance window is shown", mc.screen instanceof EnhanceScreen, mc.screen);
                    if (mc.screen instanceof EnhanceScreen es) {
                        check("the enhance window fits on the screen", es.fitsForTest());
                    }
                }
                shot(mc, "80_enhance_open");
                onServer(mc, sp -> {
                    if (sp.containerMenu instanceof com.rtshop.enhance.EnhanceMenu m) {
                        m.getSlot(0).set(leveled(net.minecraft.world.item.Items.DIAMOND_SWORD, 4));
                        m.getSlot(1).set(new ItemStack(com.rtshop.item.ModItems.GUARD_DESTROY.get(), 2));
                    }
                    com.rtshop.enhance.Enhance.testRoll = 0.0;
                });
                wait = 15;
            }
            case 3 -> {
                var m = mc.player.containerMenu;
                boolean ready = m instanceof com.rtshop.enhance.EnhanceMenu && m.getSlot(2).hasItem() && m.getSlot(2).mayPickup(mc.player);
                check("the +5 preview is ready and affordable", ready);
                if (m.getSlot(2).hasItem()) {
                    List<net.minecraft.network.chat.Component> tip = net.minecraft.client.gui.screens.Screen.getTooltipFromItem(mc, m.getSlot(2).getItem());
                    check("the preview tooltip shows the odds and the materials (also shown in the Cinematic scene)",
                            tip.stream().anyMatch(c -> c.getString().contains("56%")), tip.size());
                }
                shot(mc, "81_enhance_ready");
                mc.gameMode.handleInventoryMouseClick(m.containerId, 2, 0, net.minecraft.world.inventory.ClickType.PICKUP, mc.player);
                wait = 6;
            }
            case 4 -> {
                shot(mc, "82_enhance_success");
                check("success: +4 -> +5 with the result banner", EnhanceFx.last.startsWith("SUCCESS:4:5") && EnhanceFx.active(), EnhanceFx.last);
                onServer(mc, sp -> {
                    if (sp.containerMenu instanceof com.rtshop.enhance.EnhanceMenu m) {
                        ItemStack got = m.getCarried();
                        m.setCarried(ItemStack.EMPTY);
                        m.getSlot(0).set(got.isEmpty() ? leveled(net.minecraft.world.item.Items.DIAMOND_SWORD, 5) : got);
                    }
                    com.rtshop.enhance.Enhance.testRoll = 0.99;
                });
                wait = 60;
            }
            case 5 -> {
                var m = mc.player.containerMenu;
                mc.gameMode.handleInventoryMouseClick(m.containerId, 2, 0, net.minecraft.world.inventory.ClickType.PICKUP, mc.player);
                wait = 6;
            }
            case 6 -> {
                shot(mc, "83_enhance_guarded");
                check("a destroy roll with a destruction guard only damages the item (+5 -> +4)",
                        EnhanceFx.last.startsWith("DAMAGE:5:4:guarded"), EnhanceFx.last);
                onServer(mc, sp -> {
                    com.rtshop.enhance.Enhance.testRoll = null;
                    if (sp.containerMenu instanceof com.rtshop.enhance.EnhanceMenu m) {
                        sp.getInventory().add(m.getCarried());
                        m.setCarried(ItemStack.EMPTY);
                    }
                });
                wait = 60;
            }
            case 7 -> {
                ItemStack held = ItemStack.EMPTY;
                for (ItemStack s : mc.player.getInventory().items) {
                    if (s.is(net.minecraft.world.item.Items.DIAMOND_SWORD)) held = s;
                }
                List<net.minecraft.network.chat.Component> tip = held.isEmpty() ? List.of()
                        : net.minecraft.client.gui.screens.Screen.getTooltipFromItem(mc, held);
                check("an enhanced item shows +N in front of its name", !tip.isEmpty() && tip.get(0).getString().startsWith("+4 "),
                        tip.isEmpty() ? "no sword" : tip.get(0).getString());
                mc.player.closeContainer();
                wait = 40;
            }
            default -> {
                mc.setScreen(null);
                BlockPos at = anvilPos;
                onServer(mc, sp -> {
                    if (at != null) sp.serverLevel().setBlockAndUpdate(at, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                });
                wait = 10;
            }
        }
    }

    /** 영어로 바꿔서 뽑기 화면 글자가 버튼 밖으로 나가지 않는지 본다. */
    private static void englishStep(Minecraft mc, int k) {
        GachaScreen gs = mc.screen instanceof GachaScreen s ? s : null;
        switch (k) {
            case 0 -> {
                mc.setScreen(null);
                mc.options.languageCode = "en_us";
                mc.getLanguageManager().setSelected("en_us");
                mc.reloadResourcePacks();
                wait = 40;
            }
            case 1 -> {
                if (mc.getOverlay() != null) {
                    step--; // 다시 읽는 중
                    wait = 20;
                    return;
                }
                onServer(mc, sp -> com.rtshop.gacha.Gacha.open(sp, gachaNpc));
                wait = 20;
            }
            case 2 -> {
                if (gs != null) {
                    gs.showForTest(2, 0, -1);
                }
                wait = 12;
            }
            case 3 -> {
                shot(mc, "70_gacha_en_accessory");
                if (gs != null) {
                    check("English gacha tabs fit inside their buttons", gs.tabsFitForTest(), gs.tabsReportForTest());
                    gs.showForTest(2, 1, -1);
                }
                wait = 5;
            }
            case 4 -> {
                shot(mc, "71_gacha_en_tickets");
                if (gs != null) {
                    gs.showForTest(0, 2, -1);
                }
                wait = 5;
            }
            default -> {
                shot(mc, "72_gacha_en_dismantle");
                mc.setScreen(null);
                wait = 5;
            }
        }
    }

    private static void npcStep(Minecraft mc, int k) {
        String type = NPCS[k / 3];
        switch (k % 3) {
            case 0 -> {
                mc.setScreen(null);
                testNpc = -1;
                onServer(mc, sp -> {
                    net.minecraft.world.phys.Vec3 look = sp.getLookAngle();
                    net.minecraft.world.phys.Vec3 at = sp.position().add(look.x * 2, 0, look.z * 2);
                    var v = ShopServer.spawnNpc(sp.serverLevel(), at, sp.getYRot() + 180, type);
                    testNpc = v == null ? -2 : v.getId();
                });
                wait = 20;
            }
            case 1 -> {
                net.minecraft.world.entity.Entity e = testNpc >= 0 ? mc.level.getEntity(testNpc) : null;
                if (e == null) {
                    check("the " + type + " NPC can be summoned", false, "id " + testNpc);
                    return;
                }
                try {
                    net.minecraft.world.phys.EntityHitResult hit = new net.minecraft.world.phys.EntityHitResult(e, e.position().add(0, 1, 0));
                    var r = mc.gameMode.interactAt(mc.player, e, hit, net.minecraft.world.InteractionHand.MAIN_HAND);
                    if (!r.consumesAction()) {
                        mc.gameMode.interact(mc.player, e, net.minecraft.world.InteractionHand.MAIN_HAND);
                    }
                } catch (Throwable t) {
                    check("right-clicking the " + type + " NPC does not crash", false, t);
                }
                wait = 30;
            }
            default -> {
                if (testNpc >= 0) {
                    check("right-clicking the " + type + " NPC opens its window", mc.screen != null, mc.screen);
                    shot(mc, "60_npc_" + type);
                }
                int id = testNpc;
                onServer(mc, sp -> {
                    var e = sp.serverLevel().getEntity(id);
                    if (e != null) {
                        e.discard();
                    }
                });
                wait = 10;
            }
        }
    }

    private static ShopScreen shop(Minecraft mc) {
        return mc.screen instanceof ShopScreen s ? s : null;
    }

    private static void next(Minecraft mc) {
        switch (step) {
            case 0 -> wait = 100; // 월드가 다 뜰 때까지
            case 1 -> {
                onServer(mc, sp -> {
                    sp.getInventory().clearContent();
                    Economy.setWallet(sp, 128_450);
                    EconomyData.get(sp.server).setBank(sp.getUUID(), 300_000);
                    Economy.setWallet(sp, 128_450);
                    sp.getInventory().add(starred(Items.WHEAT, 24, 3));
                    sp.getInventory().add(starred(Items.WHEAT, 40, 2));
                    sp.getInventory().add(starred(Items.WHEAT, 64, 1));
                    sp.getInventory().add(starred(Items.WHEAT, 12, 0));
                    sp.getInventory().add(starred(Items.CARROT, 33, 2));
                    sp.getInventory().add(SeedRules.license(new ItemStack(Items.CARROT, 16)));
                    sp.getInventory().add(SeedRules.license(new ItemStack(Items.WHEAT_SEEDS, 32)));
                    sp.getInventory().add(fish(Items.COD, 5, 3.0F, "common"));
                    sp.getInventory().add(fish(Items.COD, 4, 96.0F, "common"));
                    sp.getInventory().add(fish(Items.SALMON, 1, 50.0F, "golden"));
                    sp.getInventory().add(new ItemStack(Items.BREAD, 20));
                    sp.getInventory().add(new ItemStack(Items.IRON_SWORD));
                    sp.getInventory().add(new ItemStack(Items.BUCKET, 3));
                    Economy.issueCheck(sp, 2_500);
                    sp.getInventory().add(new ItemStack(ModItems.LAND_DEED.get(), 2));
                    sp.getInventory().add(new ItemStack(ModItems.LARGE_LAND_DEED.get(), 1));
                    // 퀘스트 HUD 용: 진행 중(처치), 진행 중(대화), 완료 가능(납품) 퀘스트 하나씩
                    com.rtshop.quest.QuestManager.forceComplete(sp, "job_swordsman_1");
                    com.rtshop.quest.QuestManager.forceComplete(sp, "job_swordsman_2");
                    com.rtshop.quest.QuestManager.forceComplete(sp, "job_cook_1");
                    com.rtshop.quest.QuestManager.accept(sp, "job_swordsman_3", null);
                    com.rtshop.quest.QuestManager.accept(sp, "job_warrior_1", null);
                    com.rtshop.quest.QuestManager.accept(sp, "job_cook_2", null);
                });
                mc.setScreen(null);
                wait = 40;
            }
            case 2 -> {
                check("server-side setup worked", serverError == null, serverError);
                check("the wallet reached the client (minus the 2,500 G check)", ClientEconomy.wallet == 128_450 - 2_500, ClientEconomy.wallet);
                List<com.rtshop.net.QuestHudPacket.Entry> hud = ClientQuestHud.entries;
                check("the quest HUD receives the active quests in the order they were taken", hud.size() == 3
                        && hud.get(0).id().equals("job_swordsman_3") && hud.get(2).id().equals("job_cook_2"),
                        hud.stream().map(com.rtshop.net.QuestHudPacket.Entry::id).toList());
                check("the quest HUD shows a finished quest as ready to hand in", hud.size() == 3 && hud.get(2).ready() && !hud.get(0).ready());
                shot(mc, "01_hud");
                HudEditScreen hudEdit = new HudEditScreen();
                mc.setScreen(hudEdit);
                if (mc.screen instanceof HudEditScreen hs) {
                    ClientConfig cfg = ClientConfig.get();
                    hs.select(HudEditScreen.QUEST);
                    double before = cfg.questHudScale;
                    double beforeW = cfg.questHudWidth;
                    hs.mouseScrolled(0, 0, 1);
                    check("the quest HUD can be resized in the HUD editor", cfg.questHudScale > before && cfg.questHudWidth > beforeW, cfg.questHudScale);
                    cfg.questHudScale = before;
                    cfg.questHudWidth = beforeW;
                    cfg.clampQuest();
                    hs.clickRow2ForTest(1);
                    check("the number of quests to show opens as a list", hs.popupForTest() != null && hs.popupForTest().sizeForTest() == 5);
                    if (hs.popupForTest() != null) {
                        hs.popupForTest().pickForTest("3");
                    }
                    // 글꼴: 기본 + 게임·모드가 가진 글꼴
                    hs.clickRow3ForTest(0);
                    check("the HUD font opens as a list of the fonts in this game", hs.popupForTest() != null
                            && HudFonts.available().contains("minecraft:uniform"), HudFonts.available());
                    if (hs.popupForTest() != null) {
                        hs.popupForTest().pickForTest("minecraft:uniform");
                    }
                    check("picking a font changes the quest HUD font", "minecraft:uniform".equals(cfg.questHudFont), cfg.questHudFont);
                    // 글자 크기: 너비는 그대로
                    double w0 = cfg.questHudWidth;
                    hs.clickRow3ForTest(1);
                    if (hs.popupForTest() != null) {
                        hs.popupForTest().pickForTest("125");
                    }
                    check("picking a text size changes only the text size", Math.abs(cfg.questHudScale - 1.25) < 1e-6 && cfg.questHudWidth == w0,
                            cfg.questHudScale + " " + cfg.questHudWidth);
                    hs.select(HudEditScreen.GOLD);
                    hs.clickRow3ForTest(1);
                    if (hs.popupForTest() != null) {
                        hs.popupForTest().pickForTest("80");
                    }
                    check("the Gold display has its own text size", Math.abs(cfg.hudTextScale - 0.8) < 1e-6, cfg.hudTextScale);
                    cfg.hudTextScale = 1.0;
                    hs.select(HudEditScreen.QUEST);
                    hs.clickRow3ForTest(0);
                }
                wait = 20;
            }
            case 3 -> {
                check("the HUD position editor opens", mc.screen instanceof HudEditScreen);
                shot(mc, "02_hud_edit");
                ClientConfig hc = ClientConfig.get();
                hc.questHudFont = "";
                hc.questHudScale = ClientConfig.QDEF_SCALE;
                hc.clampQuest();
                mc.setScreen(null);
                onServer(mc, com.rtshop.quest.QuestManager::reset);
                onServer(mc, sp -> ShopServer.openShop(sp, "farmer", -1));
                wait = 40;
            }
            case 4 -> {
                check("the farmer shop opens", shop(mc) != null, mc.screen);
                shot(mc, "03_farmer_buy_grid");
                if (shop(mc) != null) {
                    shop(mc).debugView(false, false, false);
                }
                wait = 10;
            }
            case 5 -> {
                shot(mc, "04_farmer_buy_list");
                if (shop(mc) != null) {
                    shop(mc).debugView(true, true, false);
                }
                wait = 10;
            }
            case 6 -> {
                shot(mc, "05_farmer_sell_grid");
                if (shop(mc) != null) {
                    shop(mc).debugView(true, false, true);
                }
                wait = 10;
            }
            case 7 -> {
                check("the quantity dialog opens on the sell tab", shop(mc) != null);
                shot(mc, "06_farmer_sell_dialog");
                mc.setScreen(null);
                onServer(mc, sp -> ShopServer.openShop(sp, "food", -1));
                wait = 40;
            }
            case 8 -> {
                check("the food shop opens", shop(mc) != null, mc.screen);
                shot(mc, "07_food_buy_grid");
                if (shop(mc) != null) {
                    shop(mc).debugView(true, false, false);
                }
                wait = 10;
            }
            case 9 -> {
                shot(mc, "08_food_sell_list");
                mc.setScreen(null);
                onServer(mc, sp -> ShopServer.openShop(sp, "fisher", -1));
                wait = 40;
            }
            case 10 -> {
                check("the fisher shop opens", shop(mc) != null, mc.screen);
                if (shop(mc) != null) {
                    shop(mc).debugView(true, false, false);
                }
                wait = 10;
            }
            case 11 -> {
                shot(mc, "09_fisher_sell_list");
                mc.setScreen(null);
                onServer(mc, sp -> ShopServer.openShop(sp, "blacksmith", -1));
                wait = 40;
            }
            case 12 -> {
                check("the blacksmith shop opens", shop(mc) != null, mc.screen);
                shot(mc, "10_blacksmith_buy_grid");
                mc.setScreen(null);
                onServer(mc, sp -> ShopServer.openShop(sp, "land", -1));
                wait = 40;
            }
            case 13 -> {
                check("the land shop opens", shop(mc) != null, mc.screen);
                shot(mc, "11_land_shop");
                mc.setScreen(null);
                onServer(mc, sp -> ShopServer.openBank(sp, -1));
                wait = 40;
            }
            case 14 -> {
                check("the bank opens", mc.screen instanceof BankScreen, mc.screen);
                shot(mc, "12_bank");
                mc.setScreen(null);
                onServer(mc, sp -> {
                    ItemStack deed = new ItemStack(ModItems.LAND_DEED.get(), 1);
                    LandManager.useDeed(sp, deed, DeedItem.Type.NORMAL);
                    LandData.Land land = LandManager.landAt(sp.serverLevel(), sp.blockPosition());
                    landId = land == null ? -1 : land.id;
                    LandManager.openScreen(sp);
                });
                wait = 40;
            }
            case 15 -> {
                check("the land settings screen opens", mc.screen instanceof LandScreen, mc.screen);
                shot(mc, "13_land_settings");
                mc.setScreen(null);
                onServer(mc, sp -> {
                    // 플레이어가 바라보는 방향과 상관없이 항상 토지 안쪽에 상자를 둔다
                    BlockPos pos = sp.blockPosition().relative(sp.getDirection(), 2);
                    LandData.Land land = LandManager.landAt(sp.serverLevel(), sp.blockPosition());
                    if (land != null && !land.claims.isEmpty()) {
                        LandData.Claim c = land.claims.get(0);
                        int cx = Math.max(c.x0 + 1, Math.min(c.x1 - 1, pos.getX()));
                        int cz = Math.max(c.z0 + 1, Math.min(c.z1 - 1, pos.getZ()));
                        pos = new BlockPos(cx, pos.getY(), cz);
                        if (pos.equals(sp.blockPosition())) {
                            pos = pos.offset(cx + 1 < c.x1 ? 1 : -1, 0, 0);
                        }
                    }
                    sp.serverLevel().setBlock(pos, Blocks.CHEST.defaultBlockState(), 3);
                    chestPos = pos;
                    LandManager.openStorage(sp, pos);
                });
                wait = 40;
            }
            case 16 -> {
                check("a chest on my land opens", mc.screen instanceof AbstractContainerScreen, mc.screen);
                check("...and offers the PIN button to the owner", ClientLand.canSetPin && ClientLand.containerPos != null);
                shot(mc, "14_chest_pin_button");
                if (mc.player != null) {
                    mc.player.closeContainer();
                }
                BlockPos pos = chestPos == null ? mc.player.blockPosition() : chestPos;
                mc.setScreen(new PinScreen(pos, PinScreen.Mode.SET, false));
                wait = 10;
            }
            case 17 -> {
                check("the PIN setup screen opens", mc.screen instanceof PinScreen);
                shot(mc, "15_pin_set");
                BlockPos pos = chestPos == null ? mc.player.blockPosition() : chestPos;
                mc.setScreen(new PinScreen(pos, PinScreen.Mode.ENTER, false));
                wait = 10;
            }
            case 18 -> {
                check("the PIN entry screen opens", mc.screen instanceof PinScreen);
                shot(mc, "16_pin_enter");
                mc.setScreen(null);
                wait = 10;
            }
            // ---- 여기부터는 실제 패킷을 보내 서버까지 왕복시켜 본다 (구매, 판매, 입금, 토지 권한, PIN, 명령어)
            case 19 -> {
                onServer(mc, sp -> ShopServer.openShop(sp, "farmer", -1));
                wait = 40;
            }
            case 20 -> {
                ShopScreen screen = shop(mc);
                check("the farmer shop opens again", screen != null);
                if (screen != null) {
                    ShopData d = screen.debugData();
                    int idx = find(d, Items.WHEAT_SEEDS, true);
                    before = ClientEconomy.wallet;
                    expected = idx < 0 ? -1 : before - 3L * d.entries.get(idx).price;
                    if (idx >= 0) {
                        ShopNet.CHANNEL.sendToServer(new TransactionPacket(d.id, d.version, idx, false, 3, 0));
                    }
                }
                wait = 20;
            }
            case 21 -> {
                check("buying through the shop screen charges the listed price", expected >= 0 && ClientEconomy.wallet == expected,
                        ClientEconomy.wallet + " (expected " + expected + ")");
                ShopScreen screen = shop(mc);
                expected = -1;
                if (screen != null && mc.player != null) {
                    ShopData d = screen.debugData();
                    int idx = find(d, Items.WHEAT, false);
                    ItemStack best = ItemStack.EMPTY;
                    for (ItemStack st : mc.player.getInventory().items) {
                        if (st.is(Items.WHEAT) && SeedRules.star(st) == 3) {
                            best = st;
                        }
                    }
                    if (idx >= 0 && !best.isEmpty()) {
                        ShopData.Entry e = d.entries.get(idx);
                        long key = d.variantKey(e, best);
                        before = ClientEconomy.wallet;
                        expected = before + ShopData.revenue(key / 100.0, 2);
                        ShopNet.CHANNEL.sendToServer(new TransactionPacket(d.id, d.version, idx, true, 2, key));
                    }
                }
                wait = 20;
            }
            case 22 -> {
                check("selling 3-star wheat pays the 3-star price", expected >= 0 && ClientEconomy.wallet == expected,
                        ClientEconomy.wallet + " (expected " + expected + ")");
                ShopScreen screen = shop(mc);
                expected = -1;
                if (screen != null && mc.player != null) {
                    ShopData d = screen.debugData();
                    int idx = find(d, Items.WHEAT, false);
                    ItemStack plainWheat = new ItemStack(Items.WHEAT);
                    long owned = 0;
                    for (ItemStack st : mc.player.getInventory().items) {
                        if (st.is(Items.WHEAT) && SeedRules.star(st) == 0) {
                            owned += st.getCount();
                        }
                    }
                    if (idx >= 0 && owned > 0) {
                        ShopData.Entry e = d.entries.get(idx);
                        before = ClientEconomy.wallet;
                        expected = before + ShopData.revenue(d.variantKey(e, plainWheat) / 100.0, owned);
                        ShopNet.CHANNEL.sendToServer(new SellAllPacket(d.id, d.version, new int[]{idx}, new long[]{d.variantKey(e, plainWheat)}));
                    }
                }
                wait = 20;
            }
            case 23 -> {
                check("'sell all' sells the listed rows", expected >= 0 && ClientEconomy.wallet == expected,
                        ClientEconomy.wallet + " (expected " + expected + ")");
                mc.setScreen(null);
                wait = 10; // 화면을 닫았다는 알림이 서버에 먼저 도착하도록 잠시 기다린다
            }
            case 24 -> {
                onServer(mc, sp -> ShopServer.openBank(sp, -1));
                wait = 40;
            }
            case 25 -> {
                check("the bank opens again", mc.screen instanceof BankScreen);
                before = ClientEconomy.bank;
                ShopNet.CHANNEL.sendToServer(new BankPacket(true, 1_000));
                wait = 20;
            }
            case 26 -> {
                check("a deposit moves gold into the bank", ClientEconomy.bank == before + 1_000, ClientEconomy.bank);
                mc.setScreen(null);
                if (mc.player != null) {
                    mc.player.connection.sendCommand("rtshop land");
                }
                wait = 40;
            }
            case 27 -> {
                check("/rtshop land opens the land settings", mc.screen instanceof LandScreen, mc.screen);
                ShopNet.CHANNEL.sendToServer(new LandActionPacket(landId, LandManager.ACT_SET_DEFAULT, "", LandData.CONTAINER | LandData.USE));
                wait = 20;
            }
            case 28 -> {
                onServer(mc, sp -> {
                    LandData.Land land = LandManager.landAt(sp.serverLevel(), sp.blockPosition());
                    serverPerms = land == null ? -2 : land.defaultPerms;
                });
                wait = 10;
            }
            case 29 -> {
                check("changing permissions on the screen reaches the server", serverPerms == (LandData.CONTAINER | LandData.USE), serverPerms);
                shot(mc, "17_land_settings_changed");
                mc.setScreen(null);
                if (chestPos != null) {
                    ShopNet.CHANNEL.sendToServer(new PinActionPacket(chestPos, PinActionPacket.SET, "2468"));
                }
                wait = 20;
            }
            case 30 -> {
                onServer(mc, sp -> serverPin = chestPos != null && LandManager.hasPin(sp.serverLevel(), chestPos));
                wait = 10;
            }
            case 31 -> {
                check("setting a PIN from the client locks the chest on the server", serverPin);
                if (mc.player != null) {
                    mc.player.connection.sendCommand("rtshop hud");
                }
                wait = 20;
            }
            case 32 -> {
                check("/rtshop hud opens the position editor", mc.screen instanceof HudEditScreen, mc.screen);
                mc.setScreen(null);
                before = ClientEconomy.wallet;
                if (mc.player != null) {
                    mc.player.connection.sendCommand("rtshop check 300");
                }
                wait = 20;
            }
            case 33 -> {
                check("/rtshop check turns carried gold into a check", ClientEconomy.wallet == before - 300, ClientEconomy.wallet);
                if (mc.player != null) {
                    mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
                }
                wait = 20;
            }
            case 34 -> {
                shot(mc, "18_inventory_marks");
                mc.setScreen(null);
                wait = 10;
            }
            // ---- 실제 클라이언트의 우클릭으로 작물 수확 (클라이언트 예측 -> 서버 처리 -> 다시 심기)
            case 35 -> {
                onServer(mc, sp -> {
                    BlockPos crop = sp.blockPosition().relative(sp.getDirection().getOpposite(), 2);
                    sp.serverLevel().setBlock(crop.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7), 3);
                    sp.serverLevel().setBlock(crop, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7), 3);
                    CropData.get(sp.serverLevel()).mark(CropData.dim(sp.serverLevel()), crop);
                    cropPos = crop;
                });
                wait = 20;
            }
            case 36 -> {
                if (cropPos != null && mc.player != null && mc.gameMode != null) {
                    mc.player.getInventory().selected = 8;
                    mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                            new BlockHitResult(Vec3.atCenterOf(cropPos), Direction.UP, cropPos, false));
                }
                wait = 20;
            }
            case 37 -> {
                onServer(mc, sp -> {
                    BlockState st = sp.serverLevel().getBlockState(cropPos);
                    boolean replanted = st.is(Blocks.WHEAT) && st.getValue(CropBlock.AGE) == 0
                            && CropData.get(sp.serverLevel()).isMarked(CropData.dim(sp.serverLevel()), cropPos);
                    harvest = replanted ? "ok" : "block after harvest: " + st;
                });
                wait = 10;
            }
            case 38 -> {
                check("right-clicking a ripe crop from a real client harvests and replants it", "ok".equals(harvest), harvest);
                onServer(mc, sp -> {
                    sp.server.getPlayerList().op(sp.getGameProfile());
                    com.rtshop.quest.QuestEditor.open(sp);
                });
                wait = 20;
            }
            case 39 -> {
                check("the quest editor opens for an operator", mc.screen instanceof QuestEditScreen, mc.screen);
                if (mc.screen instanceof QuestEditScreen s) {
                    s.selectForTest("tutorial_4", 0);
                    check("a tutorial quest loads into the editor", s.hasDraftForTest());
                }
                wait = 5;
            }
            case 40 -> {
                shot(mc, "20_qedit_basic");
                if (mc.screen instanceof QuestEditScreen s) {
                    s.selectForTest("tutorial_4", 1);
                    s.openKeyPopupForTest();
                    check("the story tab lists every control key to insert", s.popupForTest() != null && s.popupForTest().sizeForTest() > 20);
                    if (s.popupForTest() != null) {
                        s.popupForTest().pickForTest("key.jump");
                    }
                    check("picking a key inserts {key:...} into the story", s.introForTest().contains("{key:key.jump}"), s.introForTest());
                    String shown = QuestText.keys("{key:key.jump}");
                    check("{key:...} shows the player's own key", shown.length() > 2 && !shown.contains("key."), shown);
                    s.openKeyPopupForTest();
                }
                wait = 5;
            }
            case 41 -> {
                shot(mc, "21_qedit_story");
                if (mc.screen instanceof QuestEditScreen s) {
                    s.selectForTest("tutorial_3", 2);
                }
                wait = 5;
            }
            case 42 -> {
                shot(mc, "22_qedit_goals");
                if (mc.screen instanceof QuestEditScreen s) {
                    s.selectForTest("tutorial_4", 3);
                }
                wait = 5;
            }
            case 43 -> {
                shot(mc, "23_qedit_rewards");
                mc.setScreen(null);
                onServer(mc, sp -> com.rtshop.quest.QuestManager.openDialog(sp, "quest_guide", -1));
                wait = 20;
            }
            case 44 -> {
                check("the quest window opens from the guide", mc.screen instanceof QuestScreen, mc.screen);
                shot(mc, "24_quest_window");
                mc.setScreen(null);
                onServer(mc, com.rtshop.quest.QuestEditor::open);
                wait = 20;
            }
            case 45 -> {
                if (mc.screen instanceof QuestEditScreen s) {
                    s.selectForTest("tutorial_2", 1);
                    shot(mc, "25_qedit_story_after_fix");
                    s.saveTitleForTest("클라이언트에서 고친 제목");
                }
                wait = 30;
            }
            case 46 -> {
                com.rtshop.quest.QuestDef d = com.rtshop.quest.QuestManager.loaded().quests.get("tutorial_2");
                check("saving from the editor screen reaches the server and applies at once",
                        d != null && "클라이언트에서 고친 제목".equals(d.title), d == null ? null : d.title);
                check("after a successful save the editor is no longer marked unsaved",
                        mc.screen instanceof QuestEditScreen s && !s.isDirtyForTest());
                shot(mc, "26_qedit_saved");
                mc.setScreen(null);
                onServer(mc, com.rtshop.quest.QuestEditor::open);
                wait = 20;
            }
            case 47 -> {
                if (mc.screen instanceof QuestEditScreen s) {
                    s.selectForTest("tutorial_2", 2);
                    s.openTypePopupForTest();
                }
                wait = 3;
            }
            case 48 -> {
                shot(mc, "27_qedit_goal_popup");
                if (mc.screen instanceof QuestEditScreen s) {
                    check("goal types open as a popup list with all 5 choices", s.popupForTest() != null && s.popupForTest().sizeForTest() == 5);
                    if (s.popupForTest() != null) {
                        s.popupForTest().pickForTest("DELIVER");
                    }
                    check("picking from the popup closes it", s.popupForTest() == null);
                    s.openFirstGoalPickerForTest();
                }
                wait = 5;
            }
            case 49 -> {
                check("the item picker opens from a delivery goal", mc.screen instanceof PickerScreen, mc.screen);
                shot(mc, "28_pick_item");
                if (mc.screen instanceof PickerScreen p) {
                    p.searchForTest("minecraft:bread");
                    check("searching the item picker narrows the list", p.shownForTest() == 1, p.shownForTest());
                    p.pickFirstForTest();
                }
                wait = 5;
            }
            case 50 -> {
                check("picking an item fills the goal and returns to the editor",
                        mc.screen instanceof QuestEditScreen s && "minecraft:bread".equals(s.firstGoalTargetForTest()),
                        mc.screen instanceof QuestEditScreen s2 ? s2.firstGoalTargetForTest() : mc.screen);
                if (mc.screen instanceof QuestEditScreen s) {
                    s.selectForTest("tutorial_3", 2);
                }
                wait = 3;
            }
            case 51 -> {
                if (mc.screen instanceof QuestEditScreen s) {
                    s.openFirstGoalPickerForTest();
                }
                wait = 5;
            }
            case 52 -> {
                check("the monster picker opens from a kill goal", mc.screen instanceof PickerScreen, mc.screen);
                shot(mc, "29_pick_entity");
                if (mc.screen instanceof PickerScreen p) {
                    check("the monster picker lists many monsters", p.shownForTest() > 10, p.shownForTest());
                    p.searchForTest("minecraft:skeleton");
                }
                wait = 3;
            }
            case 53 -> {
                if (mc.screen instanceof PickerScreen p) {
                    p.pickFirstForTest();
                }
                wait = 5;
            }
            case 54 -> {
                String t = mc.screen instanceof QuestEditScreen s ? s.firstGoalTargetForTest() : null;
                check("picking a monster fills the kill goal", t != null && t.startsWith("minecraft:skeleton"), t);
                shot(mc, "30_qedit_after_pick");
                mc.setScreen(null);
                onServer(mc, sp -> {
                    com.rtshop.job.JobData.get(sp.server).clear(sp.getUUID());
                    for (String id : com.rtshop.job.JobConfig.get().jobs.keySet()) {
                        com.rtshop.quest.QuestManager.setFlag(sp, "unlock_job_" + id, true);
                    }
                    net.minecraft.world.entity.npc.Villager v = com.rtshop.job.Jobs.spawnNpc(sp.serverLevel(),
                            sp.position().add(sp.getLookAngle().scale(2)), 0);
                    jobNpc = v == null ? -1 : v.getId();
                    com.rtshop.job.Jobs.open(sp, jobNpc);
                });
                wait = 20;
            }
            case 55 -> {
                check("the job screen opens at the job guide", mc.screen instanceof JobScreen, mc.screen);
                shot(mc, "31_jobs");
                if (mc.screen instanceof JobScreen s) {
                    s.selectForTest("combat", "warrior");
                }
                wait = 3;
            }
            case 56 -> {
                shot(mc, "32_jobs_warrior");
                if (mc.screen instanceof JobScreen s) {
                    s.pressForTest();
                }
                wait = 20;
            }
            case 57 -> {
                check("choosing a job from the screen works and the screen updates",
                        mc.screen instanceof JobScreen s && "warrior".equals(s.currentForTest("combat")), mc.screen);
                check("the warrior health bonus reaches the client", mc.player != null && Math.abs(mc.player.getMaxHealth() - 22.0F) < 1e-3,
                        mc.player == null ? null : mc.player.getMaxHealth());
                if (mc.screen instanceof JobScreen s) {
                    s.selectForTest("life", "farmer");
                }
                wait = 3;
            }
            case 58 -> {
                shot(mc, "33_jobs_life");
                if (mc.screen instanceof JobScreen s) {
                    s.selectForTest("combat", "mage");
                }
                wait = 3;
            }
            case 59 -> {
                shot(mc, "34_jobs_mage");
                mc.setScreen(null);
                onServer(mc, com.rtshop.job.JobEditor::open);
                wait = 20;
            }
            case 60 -> {
                check("the job editor opens for an operator", mc.screen instanceof JobEditScreen, mc.screen);
                if (mc.screen instanceof JobEditScreen s) {
                    s.showForTest("swordsman", 1);
                }
                wait = 3;
            }
            case 61 -> {
                shot(mc, "35_jedit_weapons");
                if (mc.screen instanceof JobEditScreen s) {
                    s.showForTest("tank", 0);
                }
                wait = 3;
            }
            case 62 -> {
                shot(mc, "36_jedit_params");
                if (mc.screen instanceof JobEditScreen s) {
                    s.showForTest("#types", 0);
                    s.openFirstTypePopupForTest();
                    check("weapon types open as a popup list", s.popupForTest() != null && s.popupForTest().sizeForTest() >= 12);
                }
                wait = 3;
            }
            case LAST -> {
                shot(mc, "37_jedit_types");
                mc.setScreen(null);
                onServer(mc, sp -> {
                    com.rtshop.job.Jobs.force(sp, "combat", "");
                    net.minecraft.world.entity.Entity e = sp.serverLevel().getEntity(jobNpc);
                    if (e != null) {
                        e.discard();
                    }
                    com.rtshop.quest.QuestManager.reset(sp);
                    com.rtshop.quest.QuestManager.openDialog(sp, "job", -1);
                });
                wait = 20;
            }
            case LAST + 1 -> {
                check("the job guide offers the 9 job quest lines", mc.screen instanceof QuestScreen, mc.screen);
                shot(mc, "38_job_quests");
                mc.setScreen(null);
                onServer(mc, sp -> {
                    com.rtshop.quest.QuestManager.forceComplete(sp, "job_swordsman_1");
                    com.rtshop.quest.QuestManager.forceComplete(sp, "job_swordsman_2");
                    com.rtshop.quest.QuestManager.openDialog(sp, "quest_sword_master", -1);
                });
                wait = 20;
            }
            case LAST + 2 -> {
                check("the sword master offers the next job quest", mc.screen instanceof QuestScreen, mc.screen);
                boolean ef = net.minecraftforge.fml.ModList.get().isLoaded("epicfight");
                if (mc.screen instanceof QuestScreen s) {
                    String raw = s.currentTextForTest();
                    long tips = QuestText.lines(raw).stream().filter(QuestText.Line::tip).count();
                    check("mod tips appear only when that mod is installed", ef ? tips > 0 : tips == 0, tips + " " + raw);
                    check("key names in the tips turn into the player's keys", !QuestText.lines(raw).toString().contains("{key:"), raw);
                }
                List<String> missing = new ArrayList<>();
                try (java.io.InputStream in = ClientSelfTest.class.getResourceAsStream("/rtshop_defaults/quests/job_quests.json")) {
                    com.google.gson.JsonObject root = com.google.gson.JsonParser.parseString(
                            new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                    for (com.google.gson.JsonElement e : root.getAsJsonArray("quests")) {
                        com.google.gson.JsonObject o = e.getAsJsonObject();
                        for (String f : new String[]{"intro", "progress", "done"}) {
                            if (o.has(f)) {
                                for (String k : QuestText.missingKeys(o.get(f).getAsString())) {
                                    missing.add(o.get("id").getAsString() + ":" + k);
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    missing.add("read failed: " + ex);
                }
                check("every key named in the job quest tips exists in this game", missing.isEmpty(), missing);
                shot(mc, "39_quest_mod_tips");
                mc.setScreen(null);
                onServer(mc, sp -> {
                    com.rtshop.attendance.Attendance.reset(sp);
                    net.minecraft.world.entity.npc.Villager v = com.rtshop.shop.ShopServer.spawnNpc(sp.serverLevel(),
                            sp.position().add(sp.getLookAngle().scale(2)), sp.getYRot() + 180, "attendance");
                    com.rtshop.attendance.Attendance.open(sp, v == null ? -1 : v.getId());
                });
                wait = 20;
            }
            case LAST + 3 -> {
                check("talking to the attendance NPC opens the attendance window", mc.screen instanceof AttendanceScreen, mc.screen);
                if (mc.screen instanceof AttendanceScreen s) {
                    check("the attendance window lets you check in at the NPC", s.dataForTest().get("atNpc").getAsBoolean()
                            && !s.dataForTest().get("checkedToday").getAsBoolean() && s.dataForTest().getAsJsonArray("days").size() == 28);
                }
                shot(mc, "40_attendance");
                before = ClientEconomy.wallet;
                if (mc.screen instanceof AttendanceScreen s) {
                    s.clickForTest(com.rtshop.net.AttendanceActionPacket.CHECK_IN);
                }
                wait = 20;
            }
            case LAST + 4 -> {
                if (mc.screen instanceof AttendanceScreen s) {
                    check("checking in from the window works and gives the day-1 Gold", s.dataForTest().get("checkedToday").getAsBoolean()
                            && ClientEconomy.wallet - before == com.rtshop.attendance.AttendanceConfig.get().days.get(0).gold,
                            ClientEconomy.wallet - before);
                    shot(mc, "41_attendance_checked");
                    s.clickForTest(com.rtshop.net.AttendanceActionPacket.EDIT);
                } else {
                    check("the attendance window stays open after checking in", false, mc.screen);
                }
                wait = 20;
            }
            case LAST + 5 -> {
                check("the attendance editor opens for an operator", mc.screen instanceof AttendanceEditScreen, mc.screen);
                if (mc.screen instanceof AttendanceEditScreen s) {
                    s.selectForTest(0, 6);
                }
                wait = 5;
            }
            case LAST + 6 -> {
                shot(mc, "42_attendance_edit");
                if (mc.screen instanceof AttendanceEditScreen s) {
                    s.setGoldForTest("3333");
                    s.saveForTest();
                }
                wait = 20;
            }
            case LAST + 7 -> {
                check("saving in the attendance editor changes the reward table", com.rtshop.attendance.AttendanceConfig.get().days.get(6).gold == 3333,
                        com.rtshop.attendance.AttendanceConfig.get().days.get(6).gold);
                if (mc.screen instanceof AttendanceEditScreen s) {
                    s.selectForTest(1, 1);
                }
                wait = 5;
            }
            case LAST + 8 -> {
                shot(mc, "43_attendance_weekly");
                if (mc.screen instanceof AttendanceEditScreen s) {
                    s.selectForTest(2, 0);
                }
                wait = 5;
            }
            case LAST + 9 -> {
                shot(mc, "44_attendance_settings");
                mc.setScreen(null);
                onServer(mc, sp -> {
                    sp.getInventory().add(new ItemStack(com.rtshop.item.ModItems.TICKET_NORMAL.get(), 10));
                    sp.getInventory().add(new ItemStack(com.rtshop.item.ModItems.TICKET_HIGH.get(), 10));
                    net.minecraft.world.entity.npc.Villager v = com.rtshop.shop.ShopServer.spawnNpc(sp.serverLevel(),
                            sp.position().add(sp.getLookAngle().scale(-2)), sp.getYRot(), "gacha");
                    gachaNpc = v == null ? -1 : v.getId();
                    com.rtshop.gacha.Gacha.open(sp, gachaNpc);
                });
                wait = 20;
            }
            case LAST + 10 -> {
                check("talking to the gacha merchant opens the gacha window", mc.screen instanceof GachaScreen, mc.screen);
                shot(mc, "45_gacha");
                if (mc.screen instanceof GachaScreen s) {
                    check("the gacha window shows the tickets you carry", s.dataForTest().getAsJsonObject("tickets").getAsJsonArray("weapon").get(2).getAsInt() == 10);
                    s.pullForTest(0, 2, 10);
                }
                wait = 20;
            }
            case LAST + 11 -> {
                check("a premium 10-pull from the window shows 10 result cards", mc.screen instanceof GachaScreen s && s.resultsForTest() != null
                        && s.resultsForTest().size() == 10, mc.screen instanceof GachaScreen s2 ? s2.resultsForTest() : mc.screen);
                if (mc.screen instanceof GachaScreen s) {
                    s.revealForTest();
                }
                wait = 5;
            }
            case LAST + 12 -> {
                shot(mc, "46_gacha_result");
                if (mc.screen instanceof GachaScreen s) {
                    s.showForTest(0, 0, 2);
                }
                wait = 5;
            }
            case LAST + 13 -> {
                shot(mc, "47_gacha_odds");
                if (mc.screen instanceof GachaScreen s) {
                    s.showForTest(0, 2, -1);
                }
                wait = 5;
            }
            case LAST + 14 -> {
                shot(mc, "48_gacha_dismantle");
                if (mc.screen instanceof GachaScreen s) {
                    check("the dismantle tab lists the pulled items", s.dataForTest().getAsJsonArray("inventory").size() >= 1);
                    s.clickForTest();
                }
                wait = 20;
            }
            case LAST + 15 -> {
                check("the gacha editor opens for an operator", mc.screen instanceof GachaEditScreen, mc.screen);
                if (mc.screen instanceof GachaEditScreen s) {
                    s.selectForTest(0, 0);
                }
                wait = 5;
            }
            case LAST + 16 -> {
                shot(mc, "49_gacha_edit");
                if (mc.screen instanceof GachaEditScreen s) {
                    s.selectForTest(3, 0);
                    s.simulateForTest(0, 2);
                }
                wait = 20;
            }
            case LAST + 17 -> {
                if (mc.screen instanceof GachaEditScreen s) {
                    check("the editor's simulation runs 1,000 pulls on the server", s.simForTest() != null
                            && s.simForTest().get("n").getAsInt() == 1000);
                }
                wait = 3;
            }
            case LAST + 18 -> {
                shot(mc, "50_gacha_odds_edit");
                mc.setScreen(null);
                wait = 5;
            }
            default -> {
                if (step > LAST + 18 && step <= FX_END) {
                    gachaStep(mc, step - LAST - 19);
                    return;
                }
                if (step > FX_END && step <= NPC_END) {
                    npcStep(mc, step - FX_END - 1);
                    return;
                }
                if (step > NPC_END && step <= ENH_END) {
                    enhanceStep(mc, step - NPC_END - 1);
                    return;
                }
                if (step > ENH_END && step <= ENH_END + EN_STEPS) {
                    englishStep(mc, step - ENH_END - 1);
                    return;
                }
                if (step == ENH_END + EN_STEPS + 1) {
                    check("no server-side error happened", serverError == null, serverError);
                    finish(mc);
                    return;
                }
                // 사진이 디스크에 다 써질 때까지 조금 기다렸다가 끈다
                if (step > ENH_END + EN_STEPS + 40) {
                    done = true;
                    mc.stop();
                }
            }
        }
    }

    private static int find(ShopData d, Item item, boolean seed) {
        for (int i = 0; i < d.entries.size(); i++) {
            ShopData.Entry e = d.entries.get(i);
            if (e.stack.is(item) && SeedRules.isLicensed(e.stack) == seed && (seed ? e.buyable : e.sellable)) {
                return i;
            }
        }
        return -1;
    }

    private static void finish(Minecraft mc) {
        List<String> out = new ArrayList<>();
        out.add("RT Shop client self-test");
        out.addAll(LINES);
        out.add("");
        out.add(failed == 0 ? "RESULT: ALL PASS (" + passed + " checks)" : "RESULT: FAILED (" + failed + " of " + (passed + failed) + " checks)");
        try {
            Files.write(mc.gameDirectory.toPath().resolve("rtshop-clienttest.txt"), out, StandardCharsets.UTF_8);
        } catch (Throwable t) {
            RtShop.LOGGER.error("[rtshop-clienttest] could not write the result file", t);
        }
        RtShop.LOGGER.info("[rtshop-clienttest] {}", out.get(out.size() - 1));
        if (step < LAST) {
            step = LAST; // 월드에 못 들어간 경우에도 종료 절차로 넘어간다
            done = true;
            mc.stop();
        }
    }
}

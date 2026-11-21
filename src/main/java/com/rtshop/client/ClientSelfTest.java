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
    private static final int LAST = 47;

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
                });
                mc.setScreen(null);
                wait = 40;
            }
            case 2 -> {
                check("server-side setup worked", serverError == null, serverError);
                check("the wallet reached the client (minus the 2,500 G check)", ClientEconomy.wallet == 128_450 - 2_500, ClientEconomy.wallet);
                shot(mc, "01_hud");
                mc.setScreen(new HudEditScreen());
                wait = 20;
            }
            case 3 -> {
                check("the HUD position editor opens", mc.screen instanceof HudEditScreen);
                shot(mc, "02_hud_edit");
                mc.setScreen(null);
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
                    BlockPos pos = sp.blockPosition().relative(sp.getDirection(), 2);
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
                wait = 10;
            }
            case LAST -> {
                check("no server-side error happened", serverError == null, serverError);
                finish(mc);
            }
            default -> {
                // 사진이 디스크에 다 써질 때까지 조금 기다렸다가 끈다
                if (step > LAST + 60) {
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

package com.rtshop.client;

import com.rtshop.RtShop;
import com.rtshop.economy.Economy;
import com.rtshop.economy.EconomyData;
import com.rtshop.farm.SeedRules;
import com.rtshop.item.DeedItem;
import com.rtshop.item.ModItems;
import com.rtshop.land.LandManager;
import com.rtshop.shop.ShopData;
import com.rtshop.shop.ShopServer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
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

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || done || event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) {
            if (++idle > 20 * 300) {
                check("entered a singleplayer world", false);
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
            case 19 -> {
                check("no server-side error happened", serverError == null, serverError);
                finish(mc);
            }
            default -> {
                // 사진이 디스크에 다 써질 때까지 조금 기다렸다가 끈다
                if (step > 19 + 60) {
                    done = true;
                    mc.stop();
                }
            }
        }
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
        if (step < 19) {
            step = 19; // 월드에 못 들어간 경우에도 종료 절차로 넘어간다
            done = true;
            mc.stop();
        }
    }
}

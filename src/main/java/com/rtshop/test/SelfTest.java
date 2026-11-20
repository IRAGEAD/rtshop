package com.rtshop.test;

import com.mojang.authlib.GameProfile;
import com.rtshop.RtShop;
import com.rtshop.economy.Economy;
import com.rtshop.economy.EconomyConfig;
import com.rtshop.economy.EconomyData;
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
import com.rtshop.shop.ShopData;
import com.rtshop.shop.ShopLoader;
import com.rtshop.shop.ShopServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

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
            land(server, level, a, b, gx, y, gz);
        } catch (Throwable t) {
            crash("land", t);
        }
        try {
            pricing();
        } catch (Throwable t) {
            crash("pricing", t);
        }
        try {
            cleanup(server, level, a, b, gx, y, gz);
        } catch (Throwable t) {
            RtShop.LOGGER.warn("[rtshop-selftest] cleanup failed", t);
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

    // ------------------------------------------------------------------ 토지

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
        check("the land is a 16x16x16 box on the grid", c.x0 == gx && c.z0 == gz && c.x1 - c.x0 == 15 && c.y1 - c.y0 == 15
                && c.z1 - c.z0 == 15 && c.y0 == y - cfg.depthBelowFeet, c.x0 + "," + c.y0 + "," + c.z0);
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
        LandManager.useDeed(a, deeds, DeedItem.Type.NORMAL);
        check("looking up adds a box on top of the same land", data.landsOf(a.getUUID()).size() == 1 && land.claims.size() == 2
                && land.claims.get(1).y0 == c.y1 + 1);
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
        check("the large deed makes a 32x32x32 box", bigLand != null && bigLand.claims.size() == 1
                && bigLand.claims.get(0).x1 - bigLand.claims.get(0).x0 == 31 && bigLand.claims.get(0).y1 - bigLand.claims.get(0).y0 == 31
                && bigLand.claims.get(0).z1 - bigLand.claims.get(0).z0 == 31 && big.isEmpty());

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
            if (p.cost < 0 || p.stack.hasTag() || p.fixedSell) {
                continue;
            }
            boolean food = p.stack.isEdible() || p.dynamicFood || (l.solved != null && l.solved.processed.contains(ShopLoader.itemId(p.stack)));
            if (food) {
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
        for (String note : l.notes) {
            info(note);
        }
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
}

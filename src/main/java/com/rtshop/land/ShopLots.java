package com.rtshop.land;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import com.rtshop.land.LandData.Claim;
import com.rtshop.land.LandData.Land;
import com.rtshop.pshop.Employees;
import com.rtshop.pshop.PlayerShopBlockEntity;
import com.rtshop.raid.Weekly;
import com.rtshop.shop.ShopData;
import com.rtshop.shop.ShopServer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 상점 토지 (3.79, 사용자 메모 W-4 "플레이어 상점: 개인 토지에는 설치 불가, 상점 토지를 산 뒤 설치" + 11-28 답):
 * <ul>
 * <li>자리 (사용자 11-28 10:56 "남동쪽 입구 근처의 넓은 공간", 11:21 "기존 맵에 있는 가판대의 위치를 덮어쓰는 방식으로"): 아모노스 맵이면 남동쪽 입구(남동문) 안 광장에
 *     이미 있는 가판대 10곳을 그대로 판매 중 상점 토지로 등록한다 (rtshop_defaults/shop_lots.json — 칸 = 그 가판대의 차양 · 기둥 · 곁의 수레를 덮는 네모).
 *     가판대 건물은 치우지 않고 넘긴다 (산 사람이 그 안에 진열대를 놓거나 고쳐 짓는다). 높이는 둘레 길의 높이부터 잰다 (차양 위가 아니라).
 *     운영자는 구획 지팡이로 만든 구획이나 서 있는 자리를 상점 토지로 만들 수 있다.</li>
 * <li>값 = 토지 문서(16×16) 값의 블록당 값 × 넓이 × 2 (사용자 답: 2배). 한 사람이 가질 수 있는 수에 제한이 없다 (사용자 답).</li>
 * <li>플레이어 상점 진열대는 상점 토지(내 것, 또는 놓기 권한을 받은 곳) 안에만 새로 놓는다. 개인 토지에 이미 놓인 진열대는 그대로 (사용자 답).</li>
 * <li>자릿세 (사용자 11-28 10:56 "기존 자릿세 + 판매 금액의 일정 %", "기존 자릿세는 더 올리는 것이"): 기본 자릿세 = 산 값의 15% 를 주간 초기화 때 예금 → 지갑 순으로
 *     (접속하지 않아도, 산 주는 없음) + 판매 자릿세 = 그 토지의 진열대에서 팔릴 때 판매 금액의 5% 를 떼어 간다.
 *     기본 자릿세를 못 내면 밀린 자릿세가 쌓이고(4주 치까지) 그 토지의 진열대가 문을 닫는다. 돈이 생기면 저절로 내고 다시 연다 (토지 상인 창 · 진열대에서 바로 낼 수도 있다).</li>
 * <li>되팔기: 일반 구획처럼 산 값의 절반 (진열대를 먼저 치워야 하고, 밀린 자릿세는 빼고 돌려준다).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class ShopLots {
    private ShopLots() {
    }

    /**
     * 기본 장터의 판. 올리면 이미 깔린 서버에도 없는 칸만 더하고, 판에서 빠진 칸(retired)은 판매 중이면 지운다 (주인이 있으면 남김).
     * 2 (3.82, 사용자 11-28 19:32): 남동문 광장 서쪽 3칸(가판대 3 · 6 · 8)을 빼고, 북쪽 광장의 가판대 2곳(가판대 11 · 12)을 더함.
     */
    public static final int VERSION = 2;

    // ------------------------------------------------------------------ 기본 장터 (아모노스 맵)

    public static final class Lot {
        public String name = "";
        public int x0, z0, w, h;
    }

    public static final class Def {
        public String dim = "minecraft:overworld";
        public final List<Lot> lots = new ArrayList<>();
        /** 판에서 빠진 칸: 이름과 자리가 같은 상점 토지를 판매 중이면 지운다. */
        public final List<Lot> retired = new ArrayList<>();
    }

    /** 시험용: 판에서 빠진 칸 대신 쓸 목록. */
    public static volatile List<Lot> retiredForTest;

    private static Def def;

    public static synchronized Def def() {
        if (def == null) {
            Def d = new Def();
            try (InputStream in = ShopLots.class.getResourceAsStream("/rtshop_defaults/shop_lots.json")) {
                if (in != null) {
                    JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                    if (o.has("dimension")) d.dim = o.get("dimension").getAsString();
                    for (String key : new String[]{"lots", "retired"}) {
                        if (!o.has(key)) continue;
                        for (JsonElement e : o.getAsJsonArray(key)) {
                            JsonObject lo = e.getAsJsonObject();
                            Lot t = new Lot();
                            t.name = lo.get("name").getAsString();
                            t.x0 = lo.get("x0").getAsInt();
                            t.z0 = lo.get("z0").getAsInt();
                            t.w = lo.get("w").getAsInt();
                            t.h = lo.get("h").getAsInt();
                            (key.equals("lots") ? d.lots : d.retired).add(t);
                        }
                    }
                }
            } catch (Exception ex) {
                RtShop.LOGGER.error("[rtshop] shop_lots.json 을 읽지 못했습니다", ex);
            }
            def = d;
        }
        return def;
    }

    // ------------------------------------------------------------------ 값 · 자릿세

    /** 값의 바탕: 토지 상인이 파는 토지 문서의 값과 넓이 {값, 넓이}. 상점을 아직 읽지 않았으면 설정값 (16×16). */
    public static long[] deed() {
        LandConfig c = LandConfig.get();
        long price = c.shopLotDeedPrice;
        int side = 16;
        try {
            ResourceLocation want = ResourceLocation.tryParse(c.shopLotDeedItem);
            Item item = want == null ? null : ForgeRegistries.ITEMS.getValue(want);
            if (item instanceof com.rtshop.item.DeedItem di) side = di.type.size;
            boolean found = false;
            for (ShopData d : ShopServer.shops().values()) {
                for (ShopData.Entry e : d.entries) {
                    if (e.buyable && e.stack.getItem() == item && item != null) {
                        price = e.price;
                        found = true;
                        break;
                    }
                }
                if (found) break;
            }
        } catch (Throwable ignored) {
        }
        return new long[]{Math.max(0, price), (long) side * side};
    }

    /** 넓이 area 칸의 상점 토지 값 (10 G 단위로 반올림). */
    public static long priceFor(int area) {
        long[] d = deed();
        double v = d[1] <= 0 ? 0 : (double) d[0] / d[1] * area * LandConfig.get().shopLotPriceFactor;
        return Math.max(0, Math.round(v / 10.0) * 10);
    }

    /**
     * 판매 자릿세: 이 자리의 진열대에서 amount 어치가 팔릴 때 떼어 가는 몫 (상점 토지 밖이면 0, shopSalesFeeAllStands 면 어디서나).
     */
    public static long salesFee(ServerLevel level, BlockPos pos, long amount) {
        LandConfig c = LandConfig.get();
        if (amount <= 0 || c.shopLotSalesFeePercent <= 0) return 0;
        if (!c.shopSalesFeeAllStands && lotAt(level, pos) == null) return 0;
        return Math.max(0, Math.min(amount, Math.round(amount * c.shopLotSalesFeePercent / 100.0)));
    }

    /** 판매 자릿세 % 를 글로 (5.0 → "5", 2.5 → "2.5"). */
    public static String feePctText() {
        return pctText(LandConfig.get().shopLotSalesFeePercent);
    }

    public static String pctText(double v) {
        return v == Math.rint(v) ? Long.toString((long) v) : String.format(java.util.Locale.ROOT, "%.1f", v);
    }

    /** 이 상점 토지의 한 주 자릿세. */
    public static long rentOf(Land l) {
        if (l == null || !l.shopLot) return 0;
        return Math.max(0, Math.round(l.price * LandConfig.get().shopLotRentPercent / 100.0));
    }

    /** 점검용: 주의 시계를 따로 둔다 (Weekly.testClockOffset 와 같게 Weekly.now 를 쓴다). */
    public static long weekNow() {
        LandConfig c = LandConfig.get();
        return Weekly.id(Weekly.now(), c.shopLotRentDay, c.shopLotRentHour);
    }

    /** 몇 주가 지났는가 (Weekly.id 는 주가 시작하는 시각을 시간 단위로 센 값). */
    static long weeksBetween(long from, long to) {
        return Math.max(0, Math.round((to - from) / 168.0));
    }

    // ------------------------------------------------------------------ 등록

    private static ServerLevel level(MinecraftServer server, String dim) {
        for (ServerLevel l : server.getAllLevels()) {
            if (LandManager.dim(l).equals(dim)) return l;
        }
        return server.overworld();
    }

    /** 서버가 켜질 때: 아모노스 맵(성 안 토지를 넣은 세계)이면 기본 장터를 등록한다. 운영자가 걷어 냈으면(-1) 다시 넣지 않는다. */
    public static void autoInstall(MinecraftServer server) {
        try {
            LandData data = LandData.get(server);
            if (!LandConfig.get().shopLotsAuto || data.shopLotsInstalled < 0 || data.shopLotsInstalled >= VERSION) return;
            if (data.castleInstalled <= 0 || def().lots.isEmpty()) return;   // 아모노스 맵이 아니면: 운영자가 /rtshop plot shoplot 으로 직접
            int[] r = install(server);
            RtShop.LOGGER.info("[rtshop] 상점 토지(장터) {}칸을 등록했습니다 (이미 있음 {}, 다른 토지와 겹쳐 뺀 칸 {}) · 판에서 빠진 칸 {}칸을 지움 (주인이 있어 남김 {})",
                    r[0], r[1], r[2], r[3], r[4]);
        } catch (Throwable t) {
            RtShop.LOGGER.warn("[rtshop] 상점 토지 자동 등록 실패", t);
        }
    }

    /**
     * 기본 장터를 판매 중 상점 토지로 등록 (같은 이름이 있거나 다른 토지와 겹치는 칸은 건너뜀). 판에서 빠진 칸은 먼저 정리한다 (retire).
     * {만든 수, 이미 있음, 겹쳐서 뺌, 빠진 칸을 지운 수, 빠진 칸인데 주인이 있어 남긴 수}.
     */
    public static int[] install(MinecraftServer server) {
        LandData data = LandData.get(server);
        int[] gone = retire(data);
        ServerLevel level = level(server, def().dim);
        int made = 0, skipped = 0, dropped = 0;
        for (Lot t : def().lots) {
            if (PlotManager.findByName(data, t.name) != null) {
                skipped++;
                continue;
            }
            if (create(level, t.name, t.x0, t.z0, t.w, t.h) == null) dropped++;
            else made++;
        }
        data.shopLotsInstalled = VERSION;
        data.touch();
        return new int[]{made, skipped, dropped, gone[0], gone[1]};
    }

    /**
     * 판에서 빠진 칸 정리 (3.82): 이름과 자리(네모)가 같은 상점 토지를 찾아, 판매 중이면 지우고 주인이 있으면 남긴다
     * (산 사람의 토지는 빼앗지 않는다 — 운영자가 /rtshop plot shoplot status 로 보고 정함). {지운 수, 남긴 수}.
     */
    public static int[] retire(LandData data) {
        List<Lot> gone = retiredForTest != null ? retiredForTest : def().retired;
        List<Land> rm = new ArrayList<>();
        int kept = 0;
        for (Land l : data.lands()) {
            if (!l.shopLot || !l.plot) continue;
            Claim c = PlotManager.plotClaim(l);
            if (c == null) continue;
            for (Lot t : gone) {
                if (!t.name.equals(l.name) || c.x0 != t.x0 || c.z0 != t.z0 || c.x1 != t.x0 + t.w - 1 || c.z1 != t.z0 + t.h - 1) continue;
                if (l.forSale()) rm.add(l);
                else kept++;
                break;
            }
        }
        rm.forEach(data::removeLand);
        if (!rm.isEmpty()) data.touch();
        return new int[]{rm.size(), kept};
    }

    /** 판에서 빠졌는데 주인이 있어 남아 있는 칸의 이름 (운영자 status 용). */
    public static List<String> retiredKept(LandData data) {
        List<String> out = new ArrayList<>();
        for (Land l : data.lands()) {
            if (!l.shopLot || l.forSale()) continue;
            for (Lot t : def().retired) {
                if (t.name.equals(l.name)) {
                    out.add(l.name + " (" + l.ownerName + ")");
                    break;
                }
            }
        }
        return out;
    }

    /**
     * 네모 상점 토지 하나를 판매 중으로 만든다 (다른 토지와 겹치면 null). 높이는 그 자리의 지면에서 문서와 같은 깊이 ~ 꼭대기.
     * 지면 = 네 모서리 · 가운데와 바로 바깥 둘레 한 줄 가운데 가장 낮은 곳 (3.79: 가판대를 덮는 칸은 안쪽이 모두 차양이라, 둘레의 길 높이로 잰다).
     */
    public static Land create(ServerLevel level, String name, int x0, int z0, int w, int h) {
        LandData data = LandData.get(level.getServer());
        String dim = LandManager.dim(level);
        int minY = level.getMinBuildHeight(), maxY = level.getMaxBuildHeight() - 1;
        if (!data.claimsNear(dim, x0, minY, z0, x0 + w - 1, maxY, z0 + h - 1, 0).isEmpty()) return null;
        int ground = Integer.MAX_VALUE;
        List<int[]> pts = new ArrayList<>(List.of(new int[]{x0, z0}, new int[]{x0 + w - 1, z0}, new int[]{x0, z0 + h - 1}, new int[]{x0 + w - 1, z0 + h - 1},
                new int[]{x0 + w / 2, z0 + h / 2}));
        for (int x = x0 - 1; x <= x0 + w; x++) {
            pts.add(new int[]{x, z0 - 1});
            pts.add(new int[]{x, z0 + h});
        }
        for (int z = z0; z < z0 + h; z++) {
            pts.add(new int[]{x0 - 1, z});
            pts.add(new int[]{x0 + w, z});
        }
        for (int[] p : pts) {
            level.getChunk(p[0] >> 4, p[1] >> 4);
            ground = Math.min(ground, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p[0], p[1]) - 1);
        }
        lastGroundForTest = ground;
        Claim c = new Claim();
        c.dim = dim;
        c.type = "plot";
        c.x0 = x0;
        c.z0 = z0;
        c.x1 = x0 + w - 1;
        c.z1 = z0 + h - 1;
        c.y0 = Math.max(minY, ground + 1 - LandConfig.get().depthBelowFeet);
        c.y1 = maxY;
        Land land = data.createPlot(name, priceFor(w * h));
        land.shopLot = true;
        land.defaultPerms = 0;
        data.addClaim(land, c);
        data.touch();
        return land;
    }

    /** 시험용: 마지막으로 만든 상점 토지의 지면 높이. */
    public static int lastGroundForTest;

    /** 판매 중인(주인 없는) 상점 토지를 모두 지운다 (다시 자동으로 넣지 않게 -1). {지운 수, 주인이 있어 남긴 수}. */
    public static int[] uninstall(MinecraftServer server) {
        LandData data = LandData.get(server);
        List<Land> rm = new ArrayList<>();
        int kept = 0;
        for (Land l : data.lands()) {
            if (!l.shopLot) continue;
            if (l.forSale()) rm.add(l);
            else kept++;
        }
        rm.forEach(data::removeLand);
        data.shopLotsInstalled = -1;
        data.touch();
        return new int[]{rm.size(), kept};
    }

    /** 운영자: 구획을 상점 토지로 / 일반 구획으로. 상점 토지로 바꾸면 값을 상점 토지 값으로 맞춘다 (판매 중일 때만). */
    public static void setShopLot(LandData data, Land l, boolean on) {
        l.shopLot = on;
        if (on && l.forSale()) {
            Claim c = PlotManager.plotClaim(l);
            if (c != null) l.price = priceFor(c.area());
        }
        if (!on) {
            l.rentOwed = 0;
            l.rentWeek = 0;
        }
        data.touch();
    }

    public static int owned(LandData data, UUID id) {
        int n = 0;
        for (Land l : data.lands()) {
            if (l.shopLot && l.owner.equals(id)) n++;
        }
        return n;
    }

    // ------------------------------------------------------------------ 진열대 놓기

    /** 이 자리에 진열대를 새로 놓을 수 없는 까닭 (놓을 수 있으면 null). 운영자 우회 중이면 어디든. */
    public static Component standDenied(ServerPlayer p, BlockPos pos) {
        LandConfig cfg = LandConfig.get();
        if (!cfg.shopStandsOnlyOnShopLots || LandManager.bypass(p)) return null;
        Land l = LandData.get(p.server).landAt(LandManager.dim(p.level()), pos);
        if (l == null || !l.shopLot) return Component.translatable("rtshop.shoplot.stand_only");
        if (l.forSale()) return Component.translatable("rtshop.shoplot.stand_for_sale", l.name, PlotManager.money(l.price));
        if ((LandManager.perms(p, l) & LandData.PLACE) == 0) return Component.translatable("rtshop.shoplot.stand_not_yours", l.name, l.ownerName);
        return null;
    }

    // ------------------------------------------------------------------ 자릿세

    /** 이 자리가 속한 상점 토지 (아니면 null). */
    public static Land lotAt(ServerLevel level, BlockPos pos) {
        Land l = LandData.get(level.getServer()).landAt(LandManager.dim(level), pos);
        return l != null && l.shopLot ? l : null;
    }

    /** 자릿세가 밀려 이 자리의 진열대가 문을 닫았는가. */
    public static boolean closedAt(ServerLevel level, BlockPos pos) {
        Land l = lotAt(level, pos);
        return l != null && !l.forSale() && l.rentOwed > 0;
    }

    public static long owedAt(ServerLevel level, BlockPos pos) {
        Land l = lotAt(level, pos);
        return l == null || l.forSale() ? 0 : l.rentOwed;
    }

    /** 산 때: 그 주의 자릿세는 내지 않는다. */
    public static void onBought(Land l) {
        if (l == null || !l.shopLot) return;
        l.rentWeek = weekNow();
        l.rentOwed = 0;
    }

    /** 점검용: 마지막으로 보낸 자릿세 알림. */
    public static Component lastNotice;
    private static long lastTickMs;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.getServer() == null) return;
        long now = System.currentTimeMillis();
        if (now - lastTickMs < 60_000L) return;   // 1분에 한 번
        lastTickMs = now;
        try {
            tick(e.getServer());
        } catch (Throwable t) {
            RtShop.LOGGER.warn("[rtshop] 자릿세를 셈하지 못했습니다", t);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            try {
                tick(sp.server);
                for (Land l : LandData.get(sp.server).lands()) {
                    if (l.shopLot && l.owner.equals(sp.getUUID()) && l.rentOwed > 0) {
                        com.rtshop.notice.Notice.send(sp, Component.translatable("rtshop.shoplot.rent_still_owed", l.name, PlotManager.money(l.rentOwed))
                                .withStyle(ChatFormatting.RED));
                    }
                }
            } catch (Throwable t) {
                RtShop.LOGGER.debug("[rtshop] shop lot login check failed", t);
            }
        }
    }

    /**
     * 주가 바뀐 상점 토지마다 자릿세를 셈해 밀린 몫에 더하고, 밀린 몫이 있으면 예금 → 지갑에서 낸다 (모자라면 그대로 두고 진열대는 닫힘).
     * {이번에 낸 토지 수, 밀린 토지 수}.
     */
    public static int[] tick(MinecraftServer server) {
        LandConfig cfg = LandConfig.get();
        LandData data = LandData.get(server);
        long week = weekNow();
        int paid = 0, owing = 0;
        boolean dirty = false;
        for (Land l : data.lands()) {
            if (!l.shopLot || l.forSale()) continue;
            long rent = rentOf(l);
            boolean due = false;
            if (l.rentWeek <= 0) {
                l.rentWeek = week;   // 예전에 산(기록이 없는) 토지: 이번 주부터 센다
                dirty = true;
            } else if (l.rentWeek < week) {
                long weeks = weeksBetween(l.rentWeek, week);
                l.rentWeek = week;
                if (rent > 0 && weeks > 0) {
                    l.rentOwed = Math.min(l.rentOwed + rent * weeks, rent * Math.max(1, cfg.shopLotRentMaxWeeks));
                    due = true;
                }
                dirty = true;
            }
            if (l.rentOwed <= 0) continue;
            long owed = l.rentOwed;
            long[] took = Employees.charge(server, l.owner, owed);
            if (took != null) {
                l.rentOwed = 0;
                dirty = true;
                paid++;
                notify(server, l.owner, Component.translatable("rtshop.shoplot.rent_paid", l.name, PlotManager.money(owed),
                        PlotManager.money(took[0]), PlotManager.money(took[1])).withStyle(ChatFormatting.GOLD));
            } else {
                owing++;
                if (due) {
                    notify(server, l.owner, Component.translatable("rtshop.shoplot.rent_short", l.name, PlotManager.money(owed))
                            .withStyle(ChatFormatting.RED));
                }
            }
        }
        if (dirty) data.touch();
        return new int[]{paid, owing};
    }

    private static void notify(MinecraftServer server, UUID owner, Component msg) {
        lastNotice = msg;
        Employees.notify(server, owner, msg);
    }

    /** 주인이 밀린 자릿세를 바로 낸다 (토지 상인 창 · 진열대 창). null = 낼 것이 없음. */
    public static Component payNow(ServerPlayer p, Land l) {
        if (l == null || !l.shopLot || l.forSale() || l.rentOwed <= 0) return null;
        if (!l.owner.equals(p.getUUID())) return Component.translatable("rtshop.pshop.not_owner").withStyle(ChatFormatting.RED);
        long owed = l.rentOwed;
        long[] took = Employees.charge(p.server, l.owner, owed);
        if (took == null) {
            return Component.translatable("rtshop.shoplot.rent_cant", PlotManager.money(owed)).withStyle(ChatFormatting.RED);
        }
        l.rentOwed = 0;
        LandData.get(p.server).touch();
        return Component.translatable("rtshop.shoplot.rent_paid_now", l.name, PlotManager.money(owed)).withStyle(ChatFormatting.GREEN);
    }

    // ------------------------------------------------------------------ 되팔기

    /** 이 토지 안의 진열대 수 (불러온 청크만 본다 — 토지 상인 창에서 쓰므로 대개 불러와져 있다). */
    public static int standsOn(MinecraftServer server, Land l) {
        int n = 0;
        for (Claim c : l.claims) {
            ServerLevel level = level(server, c.dim);
            for (int cx = c.x0 >> 4; cx <= c.x1 >> 4; cx++) {
                for (int cz = c.z0 >> 4; cz <= c.z1 >> 4; cz++) {
                    LevelChunk ch = level.getChunk(cx, cz);
                    for (BlockEntity be : ch.getBlockEntities().values()) {
                        BlockPos q = be.getBlockPos();
                        if (be instanceof PlayerShopBlockEntity && c.contains(q.getX(), q.getY(), q.getZ())) n++;
                    }
                }
            }
        }
        return n;
    }

    /** 되팔 수 없는 까닭 (되팔 수 있으면 null). */
    public static Component giveBackDenied(ServerPlayer p, Land l) {
        if (l == null || !l.shopLot) return null;
        int n = standsOn(p.server, l);
        if (n > 0) return Component.translatable("rtshop.shoplot.give_back_stands", l.name, n);
        return null;
    }

    // ------------------------------------------------------------------ 시험용

    /** 시험용: 지금 서버의 상점 토지 요약 [{id, name, price, rent, owner, owed}]. */
    public static JsonArray summaryForTest(MinecraftServer server) {
        JsonArray a = new JsonArray();
        for (Land l : LandData.get(server).lands()) {
            if (!l.shopLot) continue;
            JsonObject o = new JsonObject();
            o.addProperty("id", l.id);
            o.addProperty("name", l.name);
            o.addProperty("price", l.price);
            o.addProperty("rent", rentOf(l));
            o.addProperty("owner", l.ownerName);
            o.addProperty("owed", l.rentOwed);
            a.add(o);
        }
        return a;
    }
}

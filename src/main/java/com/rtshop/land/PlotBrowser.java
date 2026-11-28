package com.rtshop.land;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.rtshop.economy.Economy;
import com.rtshop.item.DeedItem;
import com.rtshop.land.LandData.Claim;
import com.rtshop.land.LandData.Land;
import com.rtshop.net.PlotListPacket;
import com.rtshop.net.PlotPreviewPacket;
import com.rtshop.net.ShopNet;
import com.rtshop.shop.ShopData;
import com.rtshop.shop.ShopServer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.network.PacketDistributor;

import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.Deflater;

/**
 * 토지 상인 창의 [구획 토지]: 성 안 토지·판매 중인 구획·초보 농지를 한 곳에서 보고(지도·미리보기), 사거나 받는다.
 */
public final class PlotBrowser {
    private PlotBrowser() {
    }

    public static final int OPEN = 0;
    public static final int PREVIEW = 1;
    public static final int BUY = 2;
    public static final int BORDER = 3;
    public static final int STARTER = 4;
    public static final int BACK = 5;
    public static final int AREA = 6;
    /** 3.79: 상점 토지의 밀린 자릿세를 낸다. */
    public static final int PAY_RENT = 7;

    /** 가까이에 토지 상인(토지 문서를 파는 상인 NPC)이 있는가. OP 는 어디서나. */
    public static boolean nearMerchant(ServerPlayer p) {
        return com.rtshop.util.Perm.op(p) || merchant(p) != null;
    }

    /** 가까운 토지 상인 NPC 와 그 상점 ID. */
    public static Object[] merchant(ServerPlayer p) {
        int r = LandConfig.get().plotMerchantRange;
        Object[] best = null;
        double bd = Double.MAX_VALUE;
        for (Entity e : p.level().getEntities(p, p.getBoundingBox().inflate(r))) {
            for (String tag : e.getTags()) {
                if (tag.startsWith(ShopServer.SHOP_TAG_PREFIX) && isLandShop(tag.substring(ShopServer.SHOP_TAG_PREFIX.length()))) {
                    double dd = e.distanceToSqr(p);
                    if (dd < bd) {
                        bd = dd;
                        best = new Object[]{e, tag.substring(ShopServer.SHOP_TAG_PREFIX.length())};
                    }
                }
            }
        }
        return best;
    }

    public static boolean isLandShop(String id) {
        if ("land".equals(id)) return true;
        ShopData d = ShopServer.shops().get(id);
        return d != null && d.entries.stream().anyMatch(e -> e.stack.getItem() instanceof DeedItem);
    }

    public static String kind(Land l) {
        if (l.shopLot) return "shop";
        if (l.castle()) return "castle";
        String starter = LandConfig.get().starterPlotGroup;
        if (!starter.isEmpty() && starter.equals(l.group)) return "starter";
        return "plot";
    }

    public static void handle(ServerPlayer p, int action, int id) {
        if (!nearMerchant(p)) {
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.plotui.too_far").withStyle(ChatFormatting.RED));
            return;
        }
        LandData data = LandData.get(p.server);
        switch (action) {
            case OPEN -> send(p, null);
            case PREVIEW -> {
                Land l = data.land(id);
                if (l != null && l.plot) preview(p, l);
            }
            case BUY -> {
                Land l = data.land(id);
                Component why = l == null ? Component.translatable("rtshop.plot.not_in_plot") : PlotManager.cannotBuy(p, l);
                boolean ok = why == null && PlotManager.buyLand(p, l);
                send(p, ok ? Component.translatable("rtshop.plot.bought", l.name, PlotManager.money(l.paid)).withStyle(ChatFormatting.GREEN)
                        : (why == null ? Component.translatable("rtshop.plotui.failed") : why).copy().withStyle(ChatFormatting.RED));
                if (ok) preview(p, l);
            }
            case BORDER -> {
                Land l = data.land(id);
                Claim c = l == null ? null : PlotManager.plotClaim(l);
                if (c != null) {
                    LandManager.SHOW_BORDER.put(p.getUUID(), new long[]{l.id, p.level().getGameTime() + 1200});
                    com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.plotui.border", l.name, (c.x0 + c.x1) / 2, (c.z0 + c.z1) / 2)
                            .withStyle(ChatFormatting.AQUA));
                }
            }
            case BACK -> {
                Object[] m = merchant(p);
                ShopServer.openShop(p, m == null ? "land" : (String) m[1], m == null ? -1 : ((Entity) m[0]).getId());
            }
            case AREA -> {
                Land l = data.land(id);
                if (l != null && l.plot) queueArea(p, l);
            }
            case PAY_RENT -> {
                Land l = data.land(id);
                Component r = ShopLots.payNow(p, l);
                send(p, r == null ? Component.translatable("rtshop.shoplot.rent_none") : r);
            }
            case STARTER -> {
                boolean before = data.starterGiven(p.getUUID());
                StarterPlots.check(p);
                boolean after = data.starterGiven(p.getUUID());
                send(p, Component.translatable(after && !before ? "rtshop.plotui.starter_got" : "rtshop.plotui.starter_not")
                        .withStyle(after && !before ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
            }
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------ 목록

    /** 칸 모양(BitSet)을 압축해 글자로 (Base64(deflate)). */
    public static String packMask(java.util.BitSet mask) {
        byte[] raw = mask.toByteArray();
        java.util.zip.Deflater d = new java.util.zip.Deflater(java.util.zip.Deflater.BEST_SPEED);
        d.setInput(raw);
        d.finish();
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(Math.max(16, raw.length / 4));
        byte[] buf = new byte[1024];
        while (!d.finished()) {
            out.write(buf, 0, d.deflate(buf));
        }
        d.end();
        return java.util.Base64.getEncoder().encodeToString(out.toByteArray());
    }

    public static java.util.BitSet unpackMask(String text) {
        try {
            java.util.zip.Inflater inf = new java.util.zip.Inflater();
            inf.setInput(java.util.Base64.getDecoder().decode(text));
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            while (!inf.finished()) {
                int n = inf.inflate(buf);
                if (n == 0 && (inf.needsInput() || inf.needsDictionary())) break;
                out.write(buf, 0, n);
                if (out.size() > (1 << 20)) break;
            }
            inf.end();
            return java.util.BitSet.valueOf(out.toByteArray());
        } catch (Exception ex) {
            return null;
        }
    }

    public static JsonObject list(ServerPlayer p) {
        LandData data = LandData.get(p.server);
        LandConfig cfg = LandConfig.get();
        JsonObject o = new JsonObject();
        o.addProperty("wallet", Economy.wallet(p));
        o.addProperty("castleInstalled", data.castleInstalled);
        o.addProperty("shopLotMax", cfg.shopLotMaxPerPlayer);
        o.addProperty("shopLotRentPct", cfg.shopLotRentPercent);
        o.addProperty("shopLotFeePct", cfg.shopLotSalesFeePercent);
        JsonObject st = new JsonObject();
        st.addProperty("job", cfg.starterPlotJob);
        st.addProperty("hasJob", !cfg.starterPlotJob.isEmpty() && com.rtshop.job.Jobs.has(p, cfg.starterPlotJob));
        st.addProperty("given", data.starterGiven(p.getUUID()));
        o.add("starter", st);
        JsonArray arr = new JsonArray();
        for (Land l : data.lands()) {
            if (!l.plot) continue;
            String kind = kind(l);
            boolean mine = l.owner.equals(p.getUUID());
            if (!kind.equals("castle") && !kind.equals("starter") && !kind.equals("shop") && !l.forSale() && !mine) continue;
            Claim c = PlotManager.plotClaim(l);
            if (c == null) continue;
            JsonObject e = new JsonObject();
            e.addProperty("id", l.id);
            e.addProperty("name", l.name);
            e.addProperty("kind", kind);
            e.addProperty("area", c.area());
            e.addProperty("price", l.price);
            e.addProperty("status", mine ? "mine" : l.forSale() ? "sale" : "sold");
            e.addProperty("owner", l.ownerName);
            e.addProperty("x0", c.x0);
            e.addProperty("z0", c.z0);
            e.addProperty("x1", c.x1);
            e.addProperty("z1", c.z1);
            e.addProperty("castleNo", l.castleNo);
            e.addProperty("group", -1);
            e.addProperty("cleared", l.cleared);
            if (l.shopLot) {
                e.addProperty("rent", ShopLots.rentOf(l));
                if (mine) e.addProperty("owed", l.rentOwed);
            }
            if (l.castle() && !l.cleared && l.forSale()) {
                e.addProperty("clearWith", CastlePlots.objectColumns(l));
            }
            if (c.mask != null) {
                // 네모가 아닌 토지(농경지 등): 지도에 실제 모양대로 그리도록 칸 모양을 함께 보낸다
                e.addProperty("shape", packMask(c.mask));
            }
            arr.add(e);
        }
        o.add("plots", arr);
        return o;
    }

    public static void send(ServerPlayer p, Component result) {
        JsonObject o = list(p);
        if (result != null) {
            o.addProperty("result", Component.Serializer.toJson(result));
        }
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new PlotListPacket(o.toString()));
    }

    // ------------------------------------------------------------------ 미리보기 (위에서 본 지도)

    public static final int FLAG_PLOT = 1;
    public static final int FLAG_CLEAR = 2;
    public static final int FLAG_OTHER = 4;
    public static final int FLAG_MINE = 8;
    public static final int MAX_SAMPLES = 80;

    public static PlotPreviewPacket render(ServerLevel level, Land land, java.util.UUID viewer) {
        LandData data = LandData.get(level.getServer());
        Claim c = PlotManager.plotClaim(land);
        String dim = LandManager.dim(level);
        int pad = 8;
        int bx0 = c.x0 - pad, bz0 = c.z0 - pad, bx1 = c.x1 + pad, bz1 = c.z1 + pad;
        int w = bx1 - bx0 + 1, h = bz1 - bz0 + 1;
        int step = Math.max(1, (int) Math.ceil(Math.max(w, h) / (double) MAX_SAMPLES));
        int sw = (w + step - 1) / step, sh = (h + step - 1) / step;
        Set<Long> objCols = new HashSet<>();
        CastlePlots.Tile tile = land.castle() && !land.cleared ? CastlePlots.tile(land.castleNo) : null;
        if (tile != null) {
            for (CastlePlots.Obj o : tile.objects) objCols.add(((long) o.x << 32) ^ (o.z & 0xffffffffL));
        }
        byte[] rgb = new byte[sw * sh * 3];
        byte[] flags = new byte[sw * sh];
        int minY = level.getMinBuildHeight(), maxY = level.getMaxBuildHeight() - 1;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int[] prevRow = new int[sw];
        for (int sx = 0; sx < sw; sx++) {
            int x = bx0 + sx * step, z = bz0 - step;
            level.getChunk(x >> 4, z >> 4);
            prevRow[sx] = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        }
        for (int sz = 0; sz < sh; sz++) {
            for (int sx = 0; sx < sw; sx++) {
                int x = bx0 + sx * step, z = bz0 + sz * step;
                level.getChunk(x >> 4, z >> 4);
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                pos.set(x, top - 1, z);
                BlockState st = level.getBlockState(pos);
                MapColor mc = st.getMapColor(level, pos);
                int col = mc == null || mc == MapColor.NONE ? 0x202020 : mc.col;
                int b = top > prevRow[sx] ? 255 : top == prevRow[sx] ? 220 : 185;
                prevRow[sx] = top;
                int i = sz * sw + sx;
                rgb[i * 3] = (byte) (((col >> 16) & 255) * b / 255);
                rgb[i * 3 + 1] = (byte) (((col >> 8) & 255) * b / 255);
                rgb[i * 3 + 2] = (byte) ((col & 255) * b / 255);
                int f = 0;
                List<Claim> near = data.claimsNear(dim, x, minY, z, x, maxY, z, 0);
                boolean owned = false;
                for (Claim o : near) {
                    if (!o.hasColumn(x, z)) continue;
                    if (o.landId == land.id) f |= FLAG_PLOT;
                    else {
                        Land ol = data.land(o.landId);
                        f |= ol != null && ol.owner.equals(viewer) ? FLAG_MINE : FLAG_OTHER;
                        if (ol != null && !(ol.castle() && ol.forSale())) owned = true;
                    }
                }
                if ((f & FLAG_PLOT) == 0 && !owned && !objCols.isEmpty()) {
                    for (int dz = 0; dz < step && (f & FLAG_CLEAR) == 0; dz++)
                        for (int dx = 0; dx < step; dx++)
                            if (objCols.contains(((long) (x + dx) << 32) ^ ((z + dz) & 0xffffffffL))) {
                                f |= FLAG_CLEAR;
                                break;
                            }
                }
                flags[i] = (byte) f;
            }
        }
        return new PlotPreviewPacket(land.id, bx0, bz0, step, sw, sh, deflate(rgb), flags, false);
    }

    static byte[] deflate(byte[] in) {
        Deflater d = new Deflater(Deflater.BEST_SPEED);
        d.setInput(in);
        d.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream(in.length / 3 + 64);
        byte[] buf = new byte[8192];
        while (!d.finished()) {
            int n = d.deflate(buf);
            out.write(buf, 0, n);
        }
        d.end();
        return out.toByteArray();
    }

    // ------------------------------------------------------------------ 넓은 지도 (여러 틱에 나눠 그림)

    public static final int AREA_RADIUS = 112;
    public static final int AREA_STEP = 2;

    private static final class AreaJob {
        ServerPlayer p;
        ServerLevel level;
        int landId, x0, z0, w, h, row;
        byte[] rgb;
        byte[] flags;
        int[] prev;
    }

    private static final java.util.Map<java.util.UUID, AreaJob> JOBS = new java.util.HashMap<>();

    static void queueArea(ServerPlayer p, Land l) {
        Claim c = PlotManager.plotClaim(l);
        if (c == null) return;
        AreaJob j = new AreaJob();
        j.p = p;
        j.level = p.serverLevel();
        for (ServerLevel lv : p.server.getAllLevels()) {
            if (LandManager.dim(lv).equals(c.dim)) j.level = lv;
        }
        j.landId = l.id;
        int cx = (c.x0 + c.x1) / 2, cz = (c.z0 + c.z1) / 2;
        j.x0 = cx - AREA_RADIUS;
        j.z0 = cz - AREA_RADIUS;
        j.w = j.h = AREA_RADIUS * 2 / AREA_STEP;
        j.rgb = new byte[j.w * j.h * 3];
        j.flags = new byte[j.w * j.h];
        j.prev = new int[j.w];
        for (int sx = 0; sx < j.w; sx++) {
            int x = j.x0 + sx * AREA_STEP;
            j.level.getChunk(x >> 4, (j.z0 - AREA_STEP) >> 4);
            j.prev[sx] = j.level.getHeight(Heightmap.Types.WORLD_SURFACE, x, j.z0 - AREA_STEP);
        }
        JOBS.put(p.getUUID(), j);
    }

    /** 서버 틱마다: 넓은 지도를 몇 줄씩 그려서 다 되면 보낸다 (청크를 한꺼번에 읽어 서버가 멈추지 않게). */
    public static void tickJobs() {
        if (JOBS.isEmpty()) return;
        var it = JOBS.values().iterator();
        while (it.hasNext()) {
            AreaJob j = it.next();
            if (j.p.isRemoved()) {
                it.remove();
                continue;
            }
            LandData data = LandData.get(j.level.getServer());
            String dim = LandManager.dim(j.level);
            int minY = j.level.getMinBuildHeight(), maxY = j.level.getMaxBuildHeight() - 1;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int n = 0; n < 6 && j.row < j.h; n++, j.row++) {
                int z = j.z0 + j.row * AREA_STEP;
                for (int sx = 0; sx < j.w; sx++) {
                    int x = j.x0 + sx * AREA_STEP;
                    j.level.getChunk(x >> 4, z >> 4);
                    int top = j.level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                    pos.set(x, top - 1, z);
                    BlockState st = j.level.getBlockState(pos);
                    MapColor mc = st.getMapColor(j.level, pos);
                    int col = mc == null || mc == MapColor.NONE ? 0x202020 : mc.col;
                    int b = top > j.prev[sx] ? 255 : top == j.prev[sx] ? 220 : 185;
                    j.prev[sx] = top;
                    int i = j.row * j.w + sx;
                    j.rgb[i * 3] = (byte) (((col >> 16) & 255) * b / 255);
                    j.rgb[i * 3 + 1] = (byte) (((col >> 8) & 255) * b / 255);
                    j.rgb[i * 3 + 2] = (byte) ((col & 255) * b / 255);
                }
            }
            if (j.row >= j.h) {
                it.remove();
                ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> j.p),
                        new PlotPreviewPacket(j.landId, j.x0, j.z0, AREA_STEP, j.w, j.h, deflate(j.rgb), j.flags, true));
            }
        }
    }

    public static void preview(ServerPlayer p, Land l) {
        Claim c = PlotManager.plotClaim(l);
        if (c == null) return;
        ServerLevel level = p.serverLevel();
        for (ServerLevel lv : p.server.getAllLevels()) {
            if (LandManager.dim(lv).equals(c.dim)) level = lv;
        }
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), render(level, l, p.getUUID()));
    }
}

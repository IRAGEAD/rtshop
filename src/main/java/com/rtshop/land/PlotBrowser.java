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

    /** 가까이에 토지 상인(토지 문서를 파는 상인 NPC)이 있는가. OP 는 어디서나. */
    public static boolean nearMerchant(ServerPlayer p) {
        if (p.hasPermissions(2)) return true;
        int r = LandConfig.get().plotMerchantRange;
        for (Entity e : p.level().getEntities(p, p.getBoundingBox().inflate(r))) {
            for (String tag : e.getTags()) {
                if (tag.startsWith(ShopServer.SHOP_TAG_PREFIX) && isLandShop(tag.substring(ShopServer.SHOP_TAG_PREFIX.length()))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isLandShop(String id) {
        if ("land".equals(id)) return true;
        ShopData d = ShopServer.shops().get(id);
        return d != null && d.entries.stream().anyMatch(e -> e.stack.getItem() instanceof DeedItem);
    }

    public static String kind(Land l) {
        if (l.castle()) return "castle";
        String starter = LandConfig.get().starterPlotGroup;
        if (!starter.isEmpty() && starter.equals(l.group)) return "starter";
        return "plot";
    }

    public static void handle(ServerPlayer p, int action, int id) {
        if (!nearMerchant(p)) {
            p.sendSystemMessage(Component.translatable("rtshop.plotui.too_far").withStyle(ChatFormatting.RED));
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
                    p.sendSystemMessage(Component.translatable("rtshop.plotui.border", l.name, (c.x0 + c.x1) / 2, (c.z0 + c.z1) / 2)
                            .withStyle(ChatFormatting.AQUA));
                }
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

    public static JsonObject list(ServerPlayer p) {
        LandData data = LandData.get(p.server);
        LandConfig cfg = LandConfig.get();
        JsonObject o = new JsonObject();
        o.addProperty("wallet", Economy.wallet(p));
        o.addProperty("castleInstalled", data.castleInstalled);
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
            if (!kind.equals("castle") && !kind.equals("starter") && !l.forSale() && !mine) continue;
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
            e.addProperty("group", l.castleGroup);
            e.addProperty("cleared", l.cleared);
            if (l.castle() && !l.cleared && l.forSale()) {
                e.addProperty("clearWith", CastlePlots.clearSet(data, l).size() - 1);
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
        Set<Integer> clearIds = new HashSet<>();
        for (Land l : CastlePlots.clearSet(data, land)) clearIds.add(l.id);
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
                for (Claim o : near) {
                    if (!o.hasColumn(x, z)) continue;
                    if (o.landId == land.id) f |= FLAG_PLOT;
                    else if (clearIds.contains(o.landId)) f |= FLAG_CLEAR;
                    else {
                        Land ol = data.land(o.landId);
                        f |= ol != null && ol.owner.equals(viewer) ? FLAG_MINE : FLAG_OTHER;
                    }
                }
                flags[i] = (byte) f;
            }
        }
        return new PlotPreviewPacket(land.id, bx0, bz0, step, sw, sh, deflate(rgb), flags);
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

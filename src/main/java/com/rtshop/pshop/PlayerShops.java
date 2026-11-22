package com.rtshop.pshop;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import com.rtshop.economy.Economy;
import com.rtshop.economy.EconomyData;
import com.rtshop.land.LandManager;
import com.rtshop.net.PShopDataPacket;
import com.rtshop.net.ShopNet;
import com.rtshop.shop.ShopData;
import com.rtshop.shop.ShopServer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;

/**
 * 플레이어 상점: 주인이 품목(가방에서 고른 아이템)·개당 가격·수량을 올려 두면 다른 플레이어가 Gold 로 산다.
 * 판 돈은 주인의 예금으로 들어간다 (주인이 접속해 있지 않아도). 품목을 올릴 때 같은 물건을 파는 NPC 상점과 그 가격을 보여 준다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class PlayerShops {
    private PlayerShops() {
    }

    public static final int BUY = 0;
    public static final int ADD = 1;
    public static final int RESTOCK = 2;
    public static final int TAKE = 3;
    public static final int PRICE = 4;
    public static final int REMOVE = 5;
    public static final int RENAME = 6;
    public static final int REFRESH = 7;

    static PlayerShopBlockEntity at(ServerPlayer p, BlockPos pos) {
        if (p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64) return null;
        return p.level().getBlockEntity(pos) instanceof PlayerShopBlockEntity be ? be : null;
    }

    static boolean isOwner(ServerPlayer p, PlayerShopBlockEntity be) {
        return be.owner.equals(p.getUUID()) || (!be.hasOwner() && p.hasPermissions(2));
    }

    public static void open(ServerPlayer p, BlockPos pos) {
        PlayerShopBlockEntity be = at(p, pos);
        if (be == null) return;
        if (!be.hasOwner()) {
            be.owner = p.getUUID();
            be.ownerName = p.getGameProfile().getName();
            be.name = "";
            be.setChanged();
        }
        send(p, pos, be, null);
    }

    /** 같은 물건을 파는 NPC 상점들: [{shop, price, sell, buyable, sellable}]. */
    public static JsonArray npcPrices(ItemStack stack) {
        JsonArray out = new JsonArray();
        for (ShopData d : ShopServer.shops().values()) {
            for (ShopData.Entry e : d.entries) {
                if (!ItemStack.isSameItem(e.stack, stack)) continue;
                JsonObject o = new JsonObject();
                o.addProperty("shop", d.title);
                o.addProperty("price", e.price);
                o.addProperty("sell", e.sellUnit);
                o.addProperty("buyable", e.buyable);
                o.addProperty("sellable", e.sellable);
                out.add(o);
                break;
            }
        }
        return out;
    }

    /** 시험용: 마지막 동작 결과. */
    public static Component lastResult;

    static void send(ServerPlayer p, BlockPos pos, PlayerShopBlockEntity be, Component result) {
        lastResult = result;
        boolean owner = isOwner(p, be);
        JsonObject o = new JsonObject();
        o.addProperty("x", pos.getX());
        o.addProperty("y", pos.getY());
        o.addProperty("z", pos.getZ());
        o.addProperty("name", be.name);
        o.addProperty("owner", be.ownerName);
        o.addProperty("isOwner", owner);
        o.addProperty("wallet", Economy.wallet(p));
        o.addProperty("totalSales", be.totalSales);
        JsonArray ls = new JsonArray();
        for (int i = 0; i < be.listings.size(); i++) {
            PlayerShopBlockEntity.Listing l = be.listings.get(i);
            JsonObject lo = new JsonObject();
            lo.addProperty("i", i);
            lo.addProperty("item", l.item.save(new net.minecraft.nbt.CompoundTag()).toString());
            lo.addProperty("price", l.price);
            lo.addProperty("stock", l.stock);
            lo.addProperty("sold", l.sold);
            lo.add("npc", npcPrices(l.item));
            ls.add(lo);
        }
        o.add("listings", ls);
        if (owner) {
            JsonArray inv = new JsonArray();
            Inventory in = p.getInventory();
            for (int s = 0; s < in.items.size(); s++) {
                ItemStack st = in.items.get(s);
                if (st.isEmpty()) continue;
                JsonObject io = new JsonObject();
                io.addProperty("slot", s);
                io.addProperty("item", st.save(new net.minecraft.nbt.CompoundTag()).toString());
                io.addProperty("count", count(in, st));
                io.add("npc", npcPrices(st));
                inv.add(io);
            }
            o.add("inventory", inv);
        }
        if (result != null) o.addProperty("result", Component.Serializer.toJson(result));
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new PShopDataPacket(o.toString()));
    }

    static int count(Inventory in, ItemStack like) {
        int n = 0;
        for (ItemStack s : in.items) {
            if (!s.isEmpty() && ItemStack.isSameItemSameTags(s, like)) n += s.getCount();
        }
        return n;
    }

    static int takeFromInventory(Inventory in, ItemStack like, int want) {
        int left = want;
        for (ItemStack s : in.items) {
            if (left <= 0) break;
            if (!s.isEmpty() && ItemStack.isSameItemSameTags(s, like)) {
                int t = Math.min(left, s.getCount());
                s.shrink(t);
                left -= t;
            }
        }
        in.setChanged();
        return want - left;
    }

    static void give(ServerPlayer p, ItemStack like, int n) {
        int left = n;
        while (left > 0) {
            ItemStack s = like.copy();
            int k = Math.min(left, s.getMaxStackSize());
            s.setCount(k);
            if (!p.getInventory().add(s)) p.drop(s, false);
            left -= k;
        }
    }

    /** 판 돈: 주인의 예금으로 (접속하지 않아도). */
    static void payOwner(net.minecraft.server.MinecraftServer server, UUID owner, long amount) {
        EconomyData d = EconomyData.get(server);
        d.setBank(owner, d.bank(owner) + amount);
        ServerPlayer op = server.getPlayerList().getPlayer(owner);
        if (op != null) Economy.sync(op);
    }

    public static void handle(ServerPlayer p, BlockPos pos, int action, String json) {
        PlayerShopBlockEntity be = at(p, pos);
        if (be == null) return;
        JsonObject a = json == null || json.isEmpty() ? new JsonObject() : JsonParser.parseString(json).getAsJsonObject();
        boolean owner = isOwner(p, be);
        Component result = null;
        try {
            switch (action) {
                case BUY -> result = buy(p, be, a.get("i").getAsInt(), a.get("n").getAsInt());
                case REFRESH -> {
                }
                default -> {
                    if (!owner) {
                        result = Component.translatable("rtshop.pshop.not_owner").withStyle(ChatFormatting.RED);
                        break;
                    }
                    result = ownerAction(p, be, action, a);
                }
            }
        } catch (Exception e) {
            result = Component.translatable("rtshop.pshop.failed").withStyle(ChatFormatting.RED);
            RtShop.LOGGER.debug("[rtshop] player shop action failed", e);
        }
        be.setChanged();
        send(p, pos, be, result);
    }

    static Component buy(ServerPlayer p, PlayerShopBlockEntity be, int i, int n) {
        if (i < 0 || i >= be.listings.size() || n <= 0) return null;
        PlayerShopBlockEntity.Listing l = be.listings.get(i);
        if (be.owner.equals(p.getUUID())) return Component.translatable("rtshop.pshop.own_shop").withStyle(ChatFormatting.RED);
        n = Math.min(n, l.stock);
        if (n <= 0) return Component.translatable("rtshop.pshop.sold_out").withStyle(ChatFormatting.RED);
        long cost = l.price * n;
        if (Economy.wallet(p) < cost) {
            return Component.translatable("rtshop.pshop.no_money", Long.toString(cost)).withStyle(ChatFormatting.RED);
        }
        Economy.addWallet(p, -cost);
        l.stock -= n;
        l.sold += n;
        be.totalSales += cost;
        give(p, l.item, n);
        payOwner(p.server, be.owner, cost);
        ServerPlayer op = p.server.getPlayerList().getPlayer(be.owner);
        if (op != null) {
            op.sendSystemMessage(Component.translatable("rtshop.pshop.sold_msg", p.getDisplayName(), l.item.getHoverName(), n, Long.toString(cost))
                    .withStyle(ChatFormatting.GOLD));
        }
        RtShop.LOGGER.info("[rtshop] 플레이어 상점 {}: {} 이(가) {} x{} 을(를) {}G 에 샀습니다", be.ownerName, p.getGameProfile().getName(),
                l.item.getItem(), n, cost);
        return Component.translatable("rtshop.pshop.bought", l.item.getHoverName(), n, Long.toString(cost)).withStyle(ChatFormatting.GREEN);
    }

    static Component ownerAction(ServerPlayer p, PlayerShopBlockEntity be, int action, JsonObject a) {
        Inventory in = p.getInventory();
        switch (action) {
            case ADD -> {
                if (be.listings.size() >= PlayerShopBlockEntity.MAX_LISTINGS) return Component.translatable("rtshop.pshop.full");
                int slot = a.get("slot").getAsInt();
                if (slot < 0 || slot >= in.items.size() || in.items.get(slot).isEmpty()) return null;
                ItemStack like = in.items.get(slot).copy();
                like.setCount(1);
                if (like.getItem() instanceof net.minecraft.world.item.BlockItem bi && bi.getBlock() instanceof PlayerShopBlock) {
                    return Component.translatable("rtshop.pshop.no_shop_in_shop").withStyle(ChatFormatting.RED);
                }
                long price = Math.max(1, a.get("price").getAsLong());
                int want = Math.max(1, Math.min(PlayerShopBlockEntity.MAX_STOCK, a.get("n").getAsInt()));
                for (PlayerShopBlockEntity.Listing l : be.listings) {
                    if (ItemStack.isSameItemSameTags(l.item, like)) {
                        int took = takeFromInventory(in, like, Math.min(want, PlayerShopBlockEntity.MAX_STOCK - l.stock));
                        l.stock += took;
                        l.price = price;
                        return Component.translatable("rtshop.pshop.restocked", like.getHoverName(), took).withStyle(ChatFormatting.GREEN);
                    }
                }
                int took = takeFromInventory(in, like, want);
                if (took <= 0) return null;
                PlayerShopBlockEntity.Listing l = new PlayerShopBlockEntity.Listing();
                l.item = like;
                l.price = price;
                l.stock = took;
                be.listings.add(l);
                return Component.translatable("rtshop.pshop.added", like.getHoverName(), took, Long.toString(price)).withStyle(ChatFormatting.GREEN);
            }
            case RESTOCK -> {
                PlayerShopBlockEntity.Listing l = be.listings.get(a.get("i").getAsInt());
                int want = Math.max(1, Math.min(PlayerShopBlockEntity.MAX_STOCK - l.stock, a.get("n").getAsInt()));
                int took = takeFromInventory(in, l.item, want);
                l.stock += took;
                return Component.translatable("rtshop.pshop.restocked", l.item.getHoverName(), took).withStyle(ChatFormatting.GREEN);
            }
            case TAKE -> {
                PlayerShopBlockEntity.Listing l = be.listings.get(a.get("i").getAsInt());
                int n = Math.max(0, Math.min(l.stock, a.get("n").getAsInt()));
                l.stock -= n;
                give(p, l.item, n);
                return Component.translatable("rtshop.pshop.taken", l.item.getHoverName(), n).withStyle(ChatFormatting.YELLOW);
            }
            case PRICE -> {
                PlayerShopBlockEntity.Listing l = be.listings.get(a.get("i").getAsInt());
                l.price = Math.max(1, a.get("price").getAsLong());
                return Component.translatable("rtshop.pshop.priced", l.item.getHoverName(), Long.toString(l.price)).withStyle(ChatFormatting.GREEN);
            }
            case REMOVE -> {
                int i = a.get("i").getAsInt();
                PlayerShopBlockEntity.Listing l = be.listings.remove(i);
                give(p, l.item, l.stock);
                return Component.translatable("rtshop.pshop.removed", l.item.getHoverName(), l.stock).withStyle(ChatFormatting.YELLOW);
            }
            case RENAME -> {
                String name = a.get("name").getAsString().trim();
                if (name.length() > 32) name = name.substring(0, 32);
                if (!name.isEmpty()) be.name = name;
                return null;
            }
            default -> {
                return null;
            }
        }
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer sp && event.getLevel().getBlockEntity(event.getPos()) instanceof PlayerShopBlockEntity be
                && be.hasOwner() && !be.owner.equals(sp.getUUID()) && !LandManager.bypass(sp)) {
            event.setCanceled(true);
            PlayerShopBlock.denyBreak(sp);
        }
    }
}

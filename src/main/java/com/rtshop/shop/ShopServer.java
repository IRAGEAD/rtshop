package com.rtshop.shop;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.rtshop.RtShop;
import com.rtshop.economy.Economy;
import com.rtshop.economy.EconomyConfig;
import com.rtshop.net.OpenBankPacket;
import com.rtshop.net.OpenShopPacket;
import com.rtshop.net.ResultPacket;
import com.rtshop.net.ShopNet;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 서버 쪽 로직: 상점 로드, 명령어, NPC 우클릭, 구매/판매, 은행 입출금. */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class ShopServer {
    private ShopServer() {
    }

    /** 엔티티 태그: rtshop_shop_<상점ID> = 상인, rtshop_bank = 은행원 */
    public static final String SHOP_TAG_PREFIX = "rtshop_shop_";
    public static final String BANK_TAG = "rtshop_bank";
    private static final String BANK = "bank";

    private record Session(boolean bank, String shopId, int npcId) {
    }

    private static volatile Map<String, ShopData> shops = new LinkedHashMap<>();
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    public static synchronized Map<String, ShopData> reload() {
        EconomyConfig.reload();
        shops = ShopLoader.loadAll();
        SESSIONS.clear();
        return shops;
    }

    // ------------------------------------------------------------------ 이벤트

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        reload();
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SESSIONS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) {
            return; // 엔티티 태그는 서버에만 있음
        }
        Entity target = event.getTarget();
        String shopId = null;
        boolean bank = false;
        for (String tag : target.getTags()) {
            if (tag.equals(BANK_TAG)) {
                bank = true;
                break;
            }
            if (tag.startsWith(SHOP_TAG_PREFIX)) {
                shopId = tag.substring(SHOP_TAG_PREFIX.length());
                break;
            }
        }
        if (!bank && shopId == null) {
            return;
        }
        // 주민 거래창 등 기본 동작을 막는다.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer sp)) {
            return;
        }
        if (bank) {
            openBank(sp, target.getId());
        } else {
            openShop(sp, shopId, target.getId());
        }
    }

    // ------------------------------------------------------------------ 명령어

    private static final SuggestionProvider<CommandSourceStack> SHOP_IDS =
            (ctx, b) -> SharedSuggestionProvider.suggest(shops.keySet(), b);
    private static final SuggestionProvider<CommandSourceStack> NPC_IDS = (ctx, b) -> {
        List<String> all = new ArrayList<>(shops.keySet());
        all.add(BANK);
        return SharedSuggestionProvider.suggest(all, b);
    };

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("rtshop")
                .then(Commands.literal("balance").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    p.sendSystemMessage(Component.translatable("rtshop.msg.balance",
                            num(Economy.wallet(p)), num(Economy.bank(p))));
                    return 1;
                }))
                .then(Commands.literal("pay")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                        .executes(ShopServer::cmdPay))))
                .then(Commands.literal("reload").requires(src -> src.hasPermission(2))
                        .executes(ShopServer::cmdReload))
                .then(Commands.literal("export").requires(src -> src.hasPermission(2))
                        .executes(ShopServer::cmdExport))
                .then(Commands.literal("npc").requires(src -> src.hasPermission(2))
                        .then(Commands.argument("type", StringArgumentType.word()).suggests(NPC_IDS)
                                .executes(ShopServer::cmdSpawnNpc)))
                .then(Commands.literal("open").requires(src -> src.hasPermission(2))
                        .then(Commands.argument("shop", StringArgumentType.word()).suggests(SHOP_IDS)
                                .executes(ctx -> {
                                    openShop(ctx.getSource().getPlayerOrException(),
                                            StringArgumentType.getString(ctx, "shop"), -1);
                                    return 1;
                                })))
                .then(Commands.literal("gold").requires(src -> src.hasPermission(2))
                        .then(Commands.literal("give").then(goldArgs(0)))
                        .then(Commands.literal("take").then(goldArgs(1)))
                        .then(Commands.literal("set").then(goldArgs(2)))
                        .then(Commands.literal("check")
                                .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                                    ServerPlayer t = EntityArgument.getPlayer(ctx, "player");
                                    feedback(ctx.getSource(), Component.translatable("rtshop.msg.balance_other",
                                            t.getDisplayName(), num(Economy.wallet(t)), num(Economy.bank(t))));
                                    return 1;
                                }))))
        );
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, ?> goldArgs(int mode) {
        return Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("amount", LongArgumentType.longArg(0)).executes(ctx -> {
                    ServerPlayer t = EntityArgument.getPlayer(ctx, "player");
                    long a = LongArgumentType.getLong(ctx, "amount");
                    long w = Economy.wallet(t);
                    long nv = mode == 0 ? w + a : (mode == 1 ? Math.max(0, w - a) : a);
                    Economy.setWallet(t, nv);
                    feedback(ctx.getSource(), Component.translatable("rtshop.msg.gold_set",
                            t.getDisplayName(), num(Economy.wallet(t))));
                    return 1;
                }));
    }

    private static String num(long v) {
        return String.format("%,d", v);
    }

    private static void feedback(CommandSourceStack src, Component msg) {
        Entity e = src.getEntity();
        if (e instanceof ServerPlayer sp) {
            sp.sendSystemMessage(msg);
        } else {
            src.getServer().sendSystemMessage(msg);
        }
    }

    private static int cmdPay(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer from = ctx.getSource().getPlayerOrException();
        ServerPlayer to = EntityArgument.getPlayer(ctx, "player");
        long amount = LongArgumentType.getLong(ctx, "amount");
        if (from == to) {
            return 0;
        }
        if (Economy.wallet(from) < amount) {
            from.sendSystemMessage(Component.translatable("rtshop.msg.no_money"));
            return 0;
        }
        Economy.addWallet(from, -amount);
        Economy.addWallet(to, amount);
        from.sendSystemMessage(Component.translatable("rtshop.msg.pay_sent", to.getDisplayName(), num(amount)));
        to.sendSystemMessage(Component.translatable("rtshop.msg.pay_received", from.getDisplayName(), num(amount)));
        return 1;
    }

    private static int cmdReload(CommandContext<CommandSourceStack> ctx) {
        Map<String, ShopData> m = reload();
        int items = 0;
        for (ShopData d : m.values()) {
            items += d.entries.size();
        }
        feedback(ctx.getSource(), Component.translatable("rtshop.msg.reloaded", m.size(), items));
        return 1;
    }

    private static int cmdExport(CommandContext<CommandSourceStack> ctx) {
        try {
            int n = ShopLoader.export(shops);
            feedback(ctx.getSource(), Component.translatable("rtshop.msg.exported", n));
            return 1;
        } catch (Exception ex) {
            RtShop.LOGGER.error("[rtshop] export 실패", ex);
            feedback(ctx.getSource(), Component.literal("export failed: " + ex));
            return 0;
        }
    }

    private static VillagerProfession profession(String name) {
        switch (name) {
            case "farmer":
                return VillagerProfession.FARMER;
            case "fisherman":
                return VillagerProfession.FISHERMAN;
            case "weaponsmith":
                return VillagerProfession.WEAPONSMITH;
            case "armorer":
                return VillagerProfession.ARMORER;
            case "toolsmith":
                return VillagerProfession.TOOLSMITH;
            case "librarian":
                return VillagerProfession.LIBRARIAN;
            case "cleric":
                return VillagerProfession.CLERIC;
            case "cartographer":
                return VillagerProfession.CARTOGRAPHER;
            case "butcher":
                return VillagerProfession.BUTCHER;
            case "shepherd":
                return VillagerProfession.SHEPHERD;
            case "mason":
                return VillagerProfession.MASON;
            case "fletcher":
                return VillagerProfession.FLETCHER;
            default:
                return VillagerProfession.LEATHERWORKER;
        }
    }

    private static int cmdSpawnNpc(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        String type = StringArgumentType.getString(ctx, "type");
        String name;
        String tag;
        VillagerProfession prof;
        if (type.equals(BANK)) {
            name = EconomyConfig.get().bankNpcName;
            tag = BANK_TAG;
            prof = VillagerProfession.CLERIC;
        } else {
            ShopData d = shops.get(type);
            if (d == null) {
                feedback(src, Component.translatable("rtshop.msg.unknown_shop", type));
                return 0;
            }
            name = d.npcName;
            tag = SHOP_TAG_PREFIX + d.id;
            prof = profession(d.profession);
        }
        ServerLevel level = src.getLevel();
        Villager v = EntityType.VILLAGER.create(level);
        if (v == null) {
            return 0;
        }
        Vec3 pos = src.getPosition();
        v.moveTo(pos.x, pos.y, pos.z, src.getRotation().y, 0.0F);
        v.setNoAi(true);
        v.setInvulnerable(true);
        v.setSilent(true);
        v.setPersistenceRequired();
        v.addTag(tag);
        v.setCustomName(Component.literal(name));
        v.setCustomNameVisible(true);
        v.setVillagerData(new VillagerData(VillagerType.PLAINS, prof, 5));
        level.addFreshEntity(v);
        feedback(src, Component.translatable("rtshop.msg.npc_spawned", name));
        return 1;
    }

    // ------------------------------------------------------------------ 열기/닫기

    private static void sendResult(ServerPlayer player, Component msg, boolean ok) {
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ResultPacket(msg, ok));
    }

    public static void openShop(ServerPlayer player, String shopId, int npcId) {
        ShopData d = shops.get(shopId);
        if (d == null) {
            player.sendSystemMessage(Component.translatable("rtshop.msg.unknown_shop", shopId));
            return;
        }
        Economy.sync(player);
        SESSIONS.put(player.getUUID(), new Session(false, d.id, npcId));
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenShopPacket(d));
    }

    public static void openBank(ServerPlayer player, int npcId) {
        Economy.sync(player);
        SESSIONS.put(player.getUUID(), new Session(true, "", npcId));
        EconomyConfig c = EconomyConfig.get();
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new OpenBankPacket(c.deathLossMinPercent, c.deathLossMaxPercent));
    }

    public static void closeSession(ServerPlayer player) {
        SESSIONS.remove(player.getUUID());
    }

    /** 세션이 유효하고, 플레이어가 아직 그 NPC 근처에 있는지 확인. */
    private static boolean nearNpc(ServerPlayer player, Session s) {
        if (s.npcId() < 0) {
            return true; // 관리자 명령어로 연 경우
        }
        Entity e = player.level().getEntity(s.npcId());
        double r = EconomyConfig.get().npcRange;
        return e != null && e.isAlive() && e.distanceToSqr(player) <= r * r;
    }

    // ------------------------------------------------------------------ 상점 거래

    public static void handleTransaction(ServerPlayer player, String shopId, int version, int index, boolean sell, int amount) {
        Session s = SESSIONS.get(player.getUUID());
        if (s == null || s.bank() || !s.shopId().equals(shopId)) {
            sendResult(player, Component.translatable("rtshop.msg.no_session"), false);
            return;
        }
        if (!nearNpc(player, s)) {
            sendResult(player, Component.translatable("rtshop.msg.too_far"), false);
            return;
        }
        ShopData d = shops.get(shopId);
        if (d == null || version != d.version || index < 0 || index >= d.entries.size()) {
            sendResult(player, Component.translatable("rtshop.msg.stale"), false);
            return;
        }
        if (amount < 1 || amount > 36 * InvUtil.MAX_STACK_CAP) {
            return;
        }
        ShopData.Entry entry = d.entries.get(index);
        if (sell) {
            sell(player, d, entry, amount);
        } else {
            buy(player, entry, amount);
        }
    }

    private static void buy(ServerPlayer player, ShopData.Entry entry, int amount) {
        long total = (long) entry.price * amount;
        if (Economy.wallet(player) < total) {
            sendResult(player, Component.translatable("rtshop.msg.no_money"), false);
            return;
        }
        // 구매한 아이템이 들어갈 자리가 없으면 구매 불가 (아이템을 바닥에 떨어뜨리지 않음)
        List<ItemStack> sim = InvUtil.copyMain(player.getInventory());
        if (!InvUtil.insert(sim, entry.stack, amount)) {
            sendResult(player, Component.translatable("rtshop.msg.inv_full_buy"), false);
            return;
        }
        InvUtil.apply(player.getInventory(), sim);
        Economy.addWallet(player, -total);
        sendResult(player, Component.translatable("rtshop.msg.bought",
                entry.stack.getHoverName(), amount, num(total)), true);
    }

    private static void sell(ServerPlayer player, ShopData d, ShopData.Entry entry, int amount) {
        List<ItemStack> sim = InvUtil.copyMain(player.getInventory());
        long owned = InvUtil.count(sim, entry::matches);
        if (owned <= 0) {
            sendResult(player, Component.translatable("rtshop.msg.no_item"), false);
            return;
        }
        int sellAmount = (int) Math.min((long) amount, owned);
        long revenue = d.sellRevenue(entry.price, sellAmount);
        if (revenue <= 0) {
            sendResult(player, Component.translatable("rtshop.msg.sell_zero"), false);
            return;
        }
        InvUtil.remove(sim, entry::matches, sellAmount);
        InvUtil.apply(player.getInventory(), sim);
        Economy.addWallet(player, revenue);
        sendResult(player, Component.translatable("rtshop.msg.sold",
                entry.stack.getHoverName(), sellAmount, num(revenue)), true);
    }

    // ------------------------------------------------------------------ 은행

    public static void handleBank(ServerPlayer player, boolean deposit, long amount) {
        Session s = SESSIONS.get(player.getUUID());
        if (s == null || !s.bank()) {
            sendResult(player, Component.translatable("rtshop.msg.no_session"), false);
            return;
        }
        if (!nearNpc(player, s)) {
            sendResult(player, Component.translatable("rtshop.msg.too_far"), false);
            return;
        }
        if (amount <= 0) {
            sendResult(player, Component.translatable("rtshop.msg.bank_bad_amount"), false);
            return;
        }
        if (deposit) {
            if (Economy.deposit(player, amount)) {
                sendResult(player, Component.translatable("rtshop.msg.deposited", num(amount)), true);
            } else {
                sendResult(player, Component.translatable("rtshop.msg.no_money"), false);
            }
        } else {
            if (Economy.withdraw(player, amount)) {
                sendResult(player, Component.translatable("rtshop.msg.withdrawn", num(amount)), true);
            } else {
                sendResult(player, Component.translatable("rtshop.msg.bank_short"), false);
            }
        }
    }
}

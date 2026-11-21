package com.rtshop.shop;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.rtshop.RtShop;
import com.rtshop.economy.Economy;
import com.rtshop.economy.EconomyConfig;
import com.rtshop.farm.FarmConfig;
import com.rtshop.farm.FarmEvents;
import com.rtshop.item.ModItems;
import com.rtshop.land.LandConfig;
import com.rtshop.land.LandManager;
import com.rtshop.net.OpenBankPacket;
import com.rtshop.net.OpenHudEditorPacket;
import com.rtshop.net.OpenShopPacket;
import com.rtshop.quest.QuestManager;
import com.rtshop.net.ResultPacket;
import com.rtshop.net.ShopNet;
import com.rtshop.test.SelfTest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
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
import net.minecraftforge.event.server.ServerStartedEvent;
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
    private static final int MAX_AMOUNT = 36 * InvUtil.MAX_STACK_CAP;

    private record Session(boolean bank, String shopId, int npcId) {
    }

    private static volatile ShopLoader.Loaded loaded = new ShopLoader.Loaded();
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    public static Map<String, ShopData> shops() {
        return loaded.shops;
    }

    public static ShopLoader.Loaded loaded() {
        return loaded;
    }

    public static synchronized ShopLoader.Loaded reload(MinecraftServer server) {
        EconomyConfig.reload();
        PricingConfig.reload();
        FarmConfig.reload();
        FarmEvents.clearCache();
        LandConfig.reload();
        com.rtshop.job.JobConfig.reload();
        com.rtshop.attendance.AttendanceConfig.reload();
        com.rtshop.gacha.GachaConfig.reload(server);
        com.rtshop.enhance.EnhanceConfig.reload();
        com.rtshop.enhance.Enhance.syncAll(server);
        QuestManager.reload();
        loaded = ShopLoader.loadAll(server);
        SESSIONS.clear();
        return loaded;
    }

    public static boolean isNpc(Entity e) {
        for (String tag : e.getTags()) {
            if (tag.equals(BANK_TAG) || tag.startsWith(SHOP_TAG_PREFIX) || tag.startsWith(QuestManager.QUEST_TAG_PREFIX)
                    || tag.equals(com.rtshop.job.Jobs.NPC_TAG) || tag.equals(com.rtshop.attendance.Attendance.NPC_TAG)
                    || tag.equals(com.rtshop.gacha.Gacha.NPC_TAG)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ 이벤트

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        reload(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        try {
            LandManager.extendClaimsToSky(event.getServer());
        } catch (Throwable t) {
            RtShop.LOGGER.error("[rtshop] 예전 토지 높이를 늘리지 못했습니다", t);
        }
        com.rtshop.land.CastlePlots.autoInstall(event.getServer());
        SelfTest.onServerStarted(event.getServer());
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
            (ctx, b) -> SharedSuggestionProvider.suggest(loaded.shops.keySet(), b);
    private static final SuggestionProvider<CommandSourceStack> NPC_IDS = (ctx, b) -> {
        List<String> all = new ArrayList<>(loaded.shops.keySet());
        all.add(BANK);
        all.add("job");
        all.add("attendance");
        all.add("gacha");
        for (String id : QuestManager.loaded().npcs.keySet()) {
            all.add("quest_" + id);
        }
        return SharedSuggestionProvider.suggest(all, b);
    };

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("rtshop")
                .then(Commands.literal("balance").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    p.sendSystemMessage(Component.translatable("rtshop.msg.balance",
                            num(Economy.wallet(p)), num(Economy.bank(p))));
                    return 1;
                }))
                .then(Commands.literal("check")
                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                .executes(ctx -> Economy.issueCheck(ctx.getSource().getPlayerOrException(),
                                        LongArgumentType.getLong(ctx, "amount")) ? 1 : 0)))
                .then(Commands.literal("hud").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new OpenHudEditorPacket());
                    return 1;
                }))
                .then(Commands.literal("land")
                        .executes(ctx -> {
                            LandManager.openScreen(ctx.getSource().getPlayerOrException());
                            return 1;
                        })
                        .then(Commands.literal("bypass").requires(src -> src.hasPermission(2)).executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            boolean on = LandManager.toggleBypass(p);
                            p.sendSystemMessage(Component.translatable(on ? "rtshop.land.bypass_on" : "rtshop.land.bypass_off"));
                            return 1;
                        })))
                .then(Commands.literal("job")
                        .executes(ctx -> {
                            com.rtshop.job.Jobs.open(ctx.getSource().getPlayerOrException(), -1);
                            return 1;
                        })
                        .then(Commands.literal("edit").requires(src -> src.hasPermission(2)).executes(ctx -> {
                            com.rtshop.job.JobEditor.open(ctx.getSource().getPlayerOrException());
                            return 1;
                        }))
                        .then(Commands.literal("set").requires(src -> src.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("job", StringArgumentType.word())
                                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(com.rtshop.job.JobConfig.get().jobs.keySet(), b))
                                                .executes(ctx -> {
                                                    ServerPlayer t = EntityArgument.getPlayer(ctx, "player");
                                                    com.rtshop.job.JobConfig.JobDef jd = com.rtshop.job.JobConfig.get().job(StringArgumentType.getString(ctx, "job"));
                                                    if (jd == null) {
                                                        feedback(ctx.getSource(), Component.translatable("rtshop.job.unknown", StringArgumentType.getString(ctx, "job")));
                                                        return 0;
                                                    }
                                                    com.rtshop.job.Jobs.force(t, jd.slot, jd.id);
                                                    feedback(ctx.getSource(), Component.translatable("rtshop.job.cmd_set", t.getDisplayName(), jd.name));
                                                    return 1;
                                                }))))
                        .then(Commands.literal("clear").requires(src -> src.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                                    ServerPlayer t = EntityArgument.getPlayer(ctx, "player");
                                    com.rtshop.job.Jobs.force(t, com.rtshop.job.JobConfig.COMBAT, "");
                                    com.rtshop.job.Jobs.force(t, com.rtshop.job.JobConfig.LIFE, "");
                                    feedback(ctx.getSource(), Component.translatable("rtshop.job.cmd_clear", t.getDisplayName()));
                                    return 1;
                                })))
                        .then(Commands.literal("cooldown").requires(src -> src.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                                    ServerPlayer t = EntityArgument.getPlayer(ctx, "player");
                                    com.rtshop.job.Jobs.resetCooldowns(t);
                                    feedback(ctx.getSource(), Component.translatable("rtshop.job.cmd_cooldown", t.getDisplayName()));
                                    return 1;
                                }))))
                .then(Commands.literal("gacha")
                        .executes(ctx -> {
                            com.rtshop.gacha.Gacha.open(ctx.getSource().getPlayerOrException(), -1);
                            return 1;
                        })
                        .then(Commands.literal("edit").requires(src -> src.getPlayer() == null || com.rtshop.gacha.Gacha.canEdit(src.getPlayer()))
                                .executes(ctx -> {
                                    com.rtshop.gacha.Gacha.openEditor(ctx.getSource().getPlayerOrException());
                                    return 1;
                                })))
                .then(Commands.literal("enhance").requires(src -> src.hasPermission(2))
                        .then(Commands.literal("set").then(Commands.argument("level", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 11))
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    net.minecraft.world.item.ItemStack held = p.getMainHandItem();
                                    if (!com.rtshop.enhance.Enhance.enhanceable(held)) {
                                        ctx.getSource().sendFailure(Component.translatable("rtshop.enhance.not_enhanceable"));
                                        return 0;
                                    }
                                    int lv = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "level");
                                    com.rtshop.enhance.Enhance.setLevel(held, lv);
                                    feedback(ctx.getSource(), Component.translatable("rtshop.enhance.cmd_set", held.getHoverName(), lv));
                                    return 1;
                                }))))
                .then(Commands.literal("attendance")
                        .executes(ctx -> {
                            com.rtshop.attendance.Attendance.open(ctx.getSource().getPlayerOrException(), -1);
                            return 1;
                        })
                        .then(Commands.literal("edit").requires(src -> src.getPlayer() == null || com.rtshop.attendance.Attendance.canEdit(src.getPlayer()))
                                .executes(ctx -> {
                            com.rtshop.attendance.Attendance.openEditor(ctx.getSource().getPlayerOrException());
                            return 1;
                        }))
                        .then(Commands.literal("set").requires(src -> src.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("days", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 100000))
                                                .executes(ctx -> {
                                                    ServerPlayer t = EntityArgument.getPlayer(ctx, "player");
                                                    int days = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "days");
                                                    com.rtshop.attendance.Attendance.setStreak(t, days);
                                                    feedback(ctx.getSource(), Component.translatable("rtshop.att.cmd_set", t.getDisplayName(), days));
                                                    return 1;
                                                }))))
                        .then(Commands.literal("reset").requires(src -> src.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                                    ServerPlayer t = EntityArgument.getPlayer(ctx, "player");
                                    com.rtshop.attendance.Attendance.reset(t);
                                    feedback(ctx.getSource(), Component.translatable("rtshop.att.cmd_reset", t.getDisplayName()));
                                    return 1;
                                }))))
                .then(Commands.literal("quest")
                        .executes(ctx -> {
                            QuestManager.openJournal(ctx.getSource().getPlayerOrException());
                            return 1;
                        })
                        .then(Commands.literal("open").then(Commands.argument("entity", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                                .executes(ctx -> QuestManager.openFromLink(ctx.getSource().getPlayerOrException(),
                                        com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "entity")) ? 1 : 0)))
                        .then(Commands.literal("edit").requires(src -> src.hasPermission(2)).executes(ctx -> {
                            com.rtshop.quest.QuestEditor.open(ctx.getSource().getPlayerOrException());
                            return 1;
                        }))
                        .then(Commands.literal("reset").requires(src -> src.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                                    ServerPlayer t = EntityArgument.getPlayer(ctx, "player");
                                    QuestManager.reset(t);
                                    feedback(ctx.getSource(), Component.translatable("rtshop.quest.cmd_reset", t.getDisplayName()));
                                    return 1;
                                })))
                        .then(Commands.literal("complete").requires(src -> src.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("quest", StringArgumentType.word())
                                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(QuestManager.loaded().quests.keySet(), b))
                                                .executes(ctx -> {
                                                    ServerPlayer t = EntityArgument.getPlayer(ctx, "player");
                                                    String id = StringArgumentType.getString(ctx, "quest");
                                                    boolean ok = QuestManager.forceComplete(t, id);
                                                    feedback(ctx.getSource(), Component.translatable(ok ? "rtshop.quest.cmd_completed" : "rtshop.quest.unknown", id));
                                                    return ok ? 1 : 0;
                                                }))))
                        .then(Commands.literal("flag").requires(src -> src.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.literal("set").then(Commands.argument("flag", StringArgumentType.word()).executes(ctx -> {
                                            ServerPlayer t = EntityArgument.getPlayer(ctx, "player");
                                            QuestManager.setFlag(t, StringArgumentType.getString(ctx, "flag"), true);
                                            return 1;
                                        })))
                                        .then(Commands.literal("clear").then(Commands.argument("flag", StringArgumentType.word()).executes(ctx -> {
                                            ServerPlayer t = EntityArgument.getPlayer(ctx, "player");
                                            QuestManager.setFlag(t, StringArgumentType.getString(ctx, "flag"), false);
                                            return 1;
                                        }))))))
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
                                }))));
        if (SelfTest.enabled()) {
            root.then(Commands.literal("selftest").requires(src -> src.hasPermission(4)).executes(ctx -> {
                SelfTest.run(ctx.getSource().getServer());
                return 1;
            }));
        }
        d.register(root);
    }

    private static RequiredArgumentBuilder<CommandSourceStack, ?> goldArgs(int mode) {
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

    private static int cmdReload(CommandContext<CommandSourceStack> ctx) {
        ShopLoader.Loaded l = reload(ctx.getSource().getServer());
        int items = 0;
        for (ShopData d : l.shops.values()) {
            items += d.entries.size();
        }
        feedback(ctx.getSource(), Component.translatable("rtshop.msg.reloaded", l.shops.size(), items));
        return 1;
    }

    private static int cmdExport(CommandContext<CommandSourceStack> ctx) {
        try {
            int n = ShopLoader.export(loaded);
            feedback(ctx.getSource(), Component.translatable("rtshop.msg.exported", n));
            return 1;
        } catch (Exception ex) {
            RtShop.LOGGER.error("[rtshop] export 실패", ex);
            feedback(ctx.getSource(), Component.literal("export failed: " + ex));
            return 0;
        }
    }

    public static VillagerProfession profession(String name) {
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
        Villager v = spawnNpc(src.getLevel(), src.getPosition(), src.getRotation().y, type);
        if (v == null) {
            feedback(src, Component.translatable("rtshop.msg.unknown_shop", type));
            return 0;
        }
        feedback(src, Component.translatable("rtshop.msg.npc_spawned", v.getCustomName()));
        return 1;
    }

    /** 상인(또는 은행원) NPC 를 소환한다. 알 수 없는 종류면 null. */
    public static Villager spawnNpc(ServerLevel level, Vec3 pos, float yaw, String type) {
        if (type.startsWith("quest_")) {
            return QuestManager.spawnNpc(level, pos, yaw, type.substring(6));
        }
        if (type.equals("job")) {
            return com.rtshop.job.Jobs.spawnNpc(level, pos, yaw);
        }
        if (type.equals("attendance")) {
            return com.rtshop.attendance.Attendance.spawnNpc(level, pos, yaw);
        }
        if (type.equals("gacha")) {
            return com.rtshop.gacha.Gacha.spawnNpc(level, pos, yaw);
        }
        String name;
        String tag;
        VillagerProfession prof;
        if (type.equals(BANK)) {
            name = EconomyConfig.get().bankNpcName;
            tag = BANK_TAG;
            prof = VillagerProfession.CLERIC;
        } else {
            ShopData d = loaded.shops.get(type);
            if (d == null) {
                return null;
            }
            name = d.npcName;
            tag = SHOP_TAG_PREFIX + d.id;
            prof = profession(d.profession);
        }
        Villager v = EntityType.VILLAGER.create(level);
        if (v == null) {
            return null;
        }
        v.moveTo(pos.x, pos.y, pos.z, yaw, 0.0F);
        v.setNoAi(true);
        v.setInvulnerable(true);
        v.setSilent(true);
        v.setPersistenceRequired();
        v.addTag(tag);
        v.setCustomName(Component.literal(name));
        v.setCustomNameVisible(true);
        v.setVillagerData(new VillagerData(VillagerType.PLAINS, prof, 5));
        level.addFreshEntity(v);
        return v;
    }

    // ------------------------------------------------------------------ 열기/닫기

    private static void sendResult(ServerPlayer player, Component msg, boolean ok) {
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ResultPacket(msg, ok));
    }

    public static void openShop(ServerPlayer player, String shopId, int npcId) {
        ShopData d = loaded.shops.get(shopId);
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

    /** 요청이 유효하면 상점 데이터를, 아니면 null (결과 메시지는 직접 보냄). */
    private static ShopData validate(ServerPlayer player, String shopId, int version) {
        Session s = SESSIONS.get(player.getUUID());
        if (s == null || s.bank() || !s.shopId().equals(shopId)) {
            sendResult(player, Component.translatable("rtshop.msg.no_session"), false);
            return null;
        }
        if (!nearNpc(player, s)) {
            sendResult(player, Component.translatable("rtshop.msg.too_far"), false);
            return null;
        }
        ShopData d = loaded.shops.get(shopId);
        if (d == null || version != d.version) {
            sendResult(player, Component.translatable("rtshop.msg.stale"), false);
            return null;
        }
        return d;
    }

    // ------------------------------------------------------------------ 상점 거래

    public static void handleTransaction(ServerPlayer player, String shopId, int version, int index, boolean sell,
                                         int amount, long variantKey) {
        ShopData d = validate(player, shopId, version);
        if (d == null) {
            return;
        }
        if (index < 0 || index >= d.entries.size()) {
            sendResult(player, Component.translatable("rtshop.msg.stale"), false);
            return;
        }
        if (amount < 1 || amount > MAX_AMOUNT) {
            return;
        }
        ShopData.Entry entry = d.entries.get(index);
        if (sell) {
            long[] r = sellVariant(player, d, index, variantKey, amount);
            if (r[0] == 0) {
                sendResult(player, Component.translatable("rtshop.msg.no_item"), false);
            } else if (r[1] <= 0) {
                sendResult(player, Component.translatable("rtshop.msg.sell_zero"), false);
            } else {
                sendResult(player, Component.translatable("rtshop.msg.sold",
                        entry.stack.getHoverName(), r[0], num(r[1])), true);
            }
        } else {
            buy(player, entry, amount);
        }
    }

    public static void handleSellAll(ServerPlayer player, String shopId, int version, int[] indexes, long[] keys) {
        ShopData d = validate(player, shopId, version);
        if (d == null) {
            return;
        }
        long count = 0;
        long revenue = 0;
        int n = Math.min(indexes.length, keys.length);
        for (int i = 0; i < n; i++) {
            if (indexes[i] < 0 || indexes[i] >= d.entries.size()) {
                continue;
            }
            long[] r = sellVariant(player, d, indexes[i], keys[i], MAX_AMOUNT);
            if (r[1] > 0) {
                count += r[0];
                revenue += r[1];
            }
        }
        if (count <= 0) {
            sendResult(player, Component.translatable("rtshop.msg.no_item"), false);
        } else {
            sendResult(player, Component.translatable("rtshop.msg.sold_all", count, num(revenue)), true);
        }
    }

    private static void buy(ServerPlayer player, ShopData.Entry entry, int amount) {
        if (!entry.buyable) {
            sendResult(player, Component.translatable("rtshop.msg.not_buyable"), false);
            return;
        }
        boolean firstDeed = entry.stack.is(ModItems.FIRST_LAND_DEED.get());
        if (firstDeed && (amount != 1 || LandManager.firstDeedUsed(player))) {
            sendResult(player, Component.translatable("rtshop.msg.first_deed_once"), false);
            return;
        }
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
        if (firstDeed) {
            LandManager.markFirstDeed(player);
        }
        sendResult(player, Component.translatable("rtshop.msg.bought",
                entry.stack.getHoverName(), amount, num(total)), true);
    }

    /** 직업 판매 보너스: 어부는 수산물, 요리사는 손으로 조리한 요리. */
    public static double jobSellMultiplier(ServerPlayer player, ShopData.Entry entry) {
        double m = 1.0;
        if (entry.fish) {
            m += com.rtshop.job.Jobs.stat(player, "fishSell");
        }
        if (ShopLoader.handCooked(loaded, entry.stack)) {
            m += com.rtshop.job.Jobs.stat(player, "dishSell");
        }
        return m;
    }

    /**
     * 인벤토리에서 이 품목·이 품질(variantKey)에 해당하는 아이템을 amount 개까지 판다.
     * @return {판 개수, 받은 Gold}. 받을 금액이 0이면 팔지 않고 {가진 개수, 0} 을 돌려준다.
     */
    private static long[] sellVariant(ServerPlayer player, ShopData d, int index, long variantKey, int amount) {
        ShopData.Entry entry = d.entries.get(index);
        if (!entry.sellable) {
            return new long[]{0, 0};
        }
        List<ItemStack> items = player.getInventory().items;
        long owned = 0;
        for (ItemStack s : items) {
            if (!s.isEmpty() && d.findSellEntry(s) == index && d.variantKey(entry, s) == variantKey) {
                owned += s.getCount();
            }
        }
        if (owned <= 0) {
            return new long[]{0, 0};
        }
        long sellAmount = Math.min(amount, owned);
        long revenue = ShopData.revenue(variantKey / 100.0 * jobSellMultiplier(player, entry), sellAmount);
        if (revenue <= 0) {
            return new long[]{owned, 0};
        }
        long left = sellAmount;
        for (int i = 0; i < items.size() && left > 0; i++) {
            ItemStack s = items.get(i);
            if (!s.isEmpty() && d.findSellEntry(s) == index && d.variantKey(entry, s) == variantKey) {
                int take = (int) Math.min((long) s.getCount(), left);
                s.shrink(take);
                left -= take;
                if (s.isEmpty()) {
                    items.set(i, ItemStack.EMPTY);
                }
            }
        }
        player.getInventory().setChanged();
        Economy.addWallet(player, revenue);
        return new long[]{sellAmount, revenue};
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

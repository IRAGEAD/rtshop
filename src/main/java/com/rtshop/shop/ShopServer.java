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
        com.rtshop.gacha.Gacha.syncGrades(server); // 등급 목록(이름·색·연출)을 접속한 사람들에게 다시 보낸다
        com.rtshop.enhance.EnhanceConfig.reload();
        com.rtshop.pvp.PvpConfig.reload();
        com.rtshop.raid.RaidConfig.reload();
        com.rtshop.story.BossMusic.reload();
        com.rtshop.rules.MobRules.reload();
        com.rtshop.rules.FoodRules.reload();
        com.rtshop.rules.TownSpawn.reload();
        com.rtshop.raid.AbyssWeekly.reload();
        com.rtshop.pvp.Pvp.resyncAll(server);
        com.rtshop.rules.LootRules.reload();
        com.rtshop.rules.RecipeRules.reload();
        com.rtshop.rules.WorldRules.reload();
        com.rtshop.raid.AbyssFloors.reload();
        if (server != null && server.getPlayerList() != null) {
            com.rtshop.rules.RecipeRules.applyAndSync(server);
        }
        com.rtshop.enhance.Enhance.syncAll(server);
        QuestManager.reload();
        com.rtshop.rules.ItemPolicy.reload();
        if (server != null) {
            com.rtshop.rules.ItemPolicy.prepare(server); // 보스 아이템을 먼저 셈한다 (상점에 오르지 않게)
        }
        loaded = ShopLoader.loadAll(server);
        com.rtshop.rules.ItemPolicy.afterShops(server, loaded);
        com.rtshop.rules.ItemNotes.rebuild(server);   // 아이템 쓰임새 설명 (상점·강화·뽑기·퀘스트에서)
        SESSIONS.clear();
        return loaded;
    }

    public static boolean isNpc(Entity e) {
        for (String tag : e.getTags()) {
            if (tag.equals(BANK_TAG) || tag.startsWith(SHOP_TAG_PREFIX) || tag.startsWith(QuestManager.QUEST_TAG_PREFIX)
                    || tag.equals(com.rtshop.job.Jobs.NPC_TAG) || tag.equals(com.rtshop.attendance.Attendance.NPC_TAG)
                    || tag.equals(com.rtshop.gacha.Gacha.NPC_TAG) || tag.equals(com.rtshop.pshop.Employees.TAG)
                    || tag.equals(com.rtshop.raid.Raid.GUIDE_TAG)) {
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
        com.rtshop.land.ShopLots.autoInstall(event.getServer());   // 3.79: 동문 안 광장의 장터 (상점 토지)
        try {
            rebuildRestaurant(event.getServer()); // 메뉴(월드 저장)를 읽어 살 수 있는 요리를 맞춘다
        } catch (Throwable t) {
            RtShop.LOGGER.warn("[rtshop] 식당 메뉴를 읽지 못했습니다", t);
        }
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
        all.add("raid_guide");
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
                    com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.msg.balance",
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
                .then(Commands.literal("border")
                        .executes(ctx -> LandManager.setBorder(ctx.getSource().getPlayerOrException(), null) ? 1 : 0)
                        .then(Commands.literal("on").executes(ctx -> LandManager.setBorder(ctx.getSource().getPlayerOrException(), true) ? 1 : 0))
                        .then(Commands.literal("off").executes(ctx -> LandManager.setBorder(ctx.getSource().getPlayerOrException(), false) ? 1 : 0)))
                .then(Commands.literal("land")
                        .executes(ctx -> {
                            LandManager.openScreen(ctx.getSource().getPlayerOrException());
                            return 1;
                        })
                        .then(Commands.literal("bypass").requires(src -> src.hasPermission(2)).executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            boolean on = LandManager.toggleBypass(p);
                            com.rtshop.notice.Notice.send(p, Component.translatable(on ? "rtshop.land.bypass_on" : "rtshop.land.bypass_off"));
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
                        .then(Commands.literal("back").executes(ctx -> {
                            com.rtshop.gacha.Gacha.back(ctx.getSource().getPlayerOrException());
                            return 1;
                        }))
                        .then(Commands.literal("edit").requires(src -> src.getPlayer() == null || com.rtshop.gacha.Gacha.canEdit(src.getPlayer()))
                                .executes(ctx -> {
                                    com.rtshop.gacha.Gacha.openEditor(ctx.getSource().getPlayerOrException());
                                    return 1;
                                })))
                .then(Commands.literal("recipes").requires(src -> src.hasPermission(2)).executes(ctx -> {
                    com.rtshop.rules.RulesEditor.handle(ctx.getSource().getPlayerOrException(), com.rtshop.rules.RulesEditor.RECIPE_OPEN, "", "");
                    return 1;
                }))
                .then(Commands.literal("loot").requires(src -> src.hasPermission(2)).executes(ctx -> {
                    com.rtshop.rules.RulesEditor.handle(ctx.getSource().getPlayerOrException(), com.rtshop.rules.RulesEditor.LOOT_OPEN, "", "");
                    return 1;
                }))
                .then(Commands.literal("enhance")
                        .then(Commands.literal("odds").executes(ctx -> {
                            com.rtshop.enhance.Enhance.action(ctx.getSource().getPlayerOrException(), com.rtshop.net.EnhanceActionPacket.OPEN_ODDS, "");
                            return 1;
                        }))
                        .then(Commands.literal("edit").requires(src -> src.hasPermission(2)).executes(ctx -> {
                            com.rtshop.enhance.Enhance.action(ctx.getSource().getPlayerOrException(), com.rtshop.net.EnhanceActionPacket.OPEN_EDITOR, "");
                            return 1;
                        }))
                        .then(Commands.literal("set").requires(src -> src.hasPermission(2)).then(Commands.argument("level", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 11))
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
                .then(Commands.literal("arena")
                        .then(Commands.argument("boss", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    String b = StringArgumentType.getString(ctx, "boss").trim();
                                    boolean force = b.endsWith(" force") && ctx.getSource().hasPermission(2);
                                    if (b.endsWith(" force")) b = b.substring(0, b.length() - 6).trim();
                                    if (b.equals("start")) return com.rtshop.story.StoryArena.wake(p) ? 1 : 0;   // 보스 구역 안에서: 불씨를 건드린다
                                    return com.rtshop.story.StoryArena.openGate(p, b, force) ? 1 : 0;
                                })))
                .then(Commands.literal("storylang").then(Commands.argument("lang", StringArgumentType.word())
                        .executes(ctx -> {
                            ServerPlayer sp = ctx.getSource().getPlayerOrException();
                            com.rtshop.story.StoryText.setLang(sp, StringArgumentType.getString(ctx, "lang"));
                            com.rtshop.story.BossIntros.sendLore(sp);
                            QuestManager.syncHud(sp);
                            try {
                                com.rtshop.job.Jobs.applyAttributes(sp);   // 직업 딱지의 이름·설명도 고른 언어로 다시 보낸다
                            } catch (Throwable ignored) {
                            }
                            return 1;
                        })))
                .then(Commands.literal("notebook").executes(ctx -> {
                    com.rtshop.story.Spots.openNotebook(ctx.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("spot")
                        .then(Commands.literal("look").then(Commands.argument("id", StringArgumentType.word())
                                .executes(ctx -> com.rtshop.story.Spots.look(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "id")) ? 1 : 0)))
                        .then(Commands.literal("done").then(Commands.argument("id", StringArgumentType.word())
                                .executes(ctx -> com.rtshop.story.Spots.done(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "id")) ? 1 : 0))))
                .then(Commands.literal("talk")
                        .then(Commands.literal("here").executes(ctx -> {
                            ServerPlayer sp = ctx.getSource().getPlayerOrException();
                            int id = QuestManager.sessionEntity(sp);
                            return id >= 0 && com.rtshop.story.Dialogue.openFor(sp, id) ? 1 : 0;
                        }))
                        .then(Commands.literal("open").then(Commands.argument("entity", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                                .executes(ctx -> com.rtshop.story.Dialogue.openFor(ctx.getSource().getPlayerOrException(),
                                        com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "entity")) ? 1 : 0)))
                        .then(Commands.literal("main").then(Commands.argument("entity", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                                .executes(ctx -> com.rtshop.story.Dialogue.main(ctx.getSource().getPlayerOrException(),
                                        com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "entity")) ? 1 : 0)))
                        .then(Commands.literal("chat").then(Commands.argument("npc", StringArgumentType.word())
                                .executes(ctx -> {
                                    com.rtshop.story.Dialogue.chatted(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "npc"));
                                    return 1;
                                })))
                        .then(Commands.literal("done").then(Commands.argument("entity", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                                .executes(ctx -> {
                                    com.rtshop.story.Dialogue.sceneDone(ctx.getSource().getPlayerOrException(),
                                            com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "entity"));
                                    return 1;
                                })))
                        .then(Commands.literal("read").then(Commands.argument("npc", StringArgumentType.word()).then(Commands.argument("topic", StringArgumentType.word())
                                .executes(ctx -> {
                                    com.rtshop.story.Dialogue.read(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "npc"),
                                            StringArgumentType.getString(ctx, "topic"));
                                    return 1;
                                }))))
                        .then(Commands.literal("lang").then(Commands.argument("lang", StringArgumentType.word())
                                .executes(ctx -> {
                                    com.rtshop.story.Loc.set(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "lang"));
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
                .then(ShopDefaults.command())
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
        root.then(com.rtshop.pvp.Pvp.adminCommand());
        root.then(Restaurant.adminCommand());
        root.then(com.rtshop.raid.Raid.adminCommand());
        root.then(com.rtshop.raid.AbyssWeekly.adminCommand());
        if (SelfTest.enabled()) {
            root.then(Commands.literal("selftest").requires(src -> src.hasPermission(4)).executes(ctx -> {
                SelfTest.run(ctx.getSource().getServer());
                return 1;
            }));
        }
        d.register(root);
        d.register(com.rtshop.pvp.Pvp.playerCommand("pvp"));
        // 짧은 이름: /border (= /rtshop border)
        d.register(Commands.literal("border")
                .executes(ctx -> LandManager.setBorder(ctx.getSource().getPlayerOrException(), null) ? 1 : 0)
                .then(Commands.literal("on").executes(ctx -> LandManager.setBorder(ctx.getSource().getPlayerOrException(), true) ? 1 : 0))
                .then(Commands.literal("off").executes(ctx -> LandManager.setBorder(ctx.getSource().getPlayerOrException(), false) ? 1 : 0)));
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
            com.rtshop.notice.Notice.send(sp, msg);
        } else {
            src.getServer().sendSystemMessage(msg);
        }
    }

    private static int cmdReload(CommandContext<CommandSourceStack> ctx) {
        NpcNames.reload();
        com.rtshop.party.PartyConfig.reload();
        com.rtshop.story.StoryConfig.reload();
        com.rtshop.story.StorySeals.reload();
        com.rtshop.story.ScyllaClues.reload();
        com.rtshop.story.Hunts.reload();
        com.rtshop.story.Dialogue.reload();
        com.rtshop.story.Readables.reload();
        com.rtshop.rank.Ranks.reload();
        com.rtshop.story.Codex.reload();
        com.rtshop.story.Guide.reload();
        com.rtshop.story.MapMarks.reload();
        com.rtshop.story.StoryText.reload();
        com.rtshop.story.StorySystems.load();
        com.rtshop.compat.SpellMana.load();
        com.rtshop.stats.ItemStats.load();
        com.rtshop.stats.ItemStats.syncAll(ctx.getSource().getServer());
        com.rtshop.story.WildSpawns.reload();
        com.rtshop.story.StoryArena.reload();
        com.rtshop.story.Examine.reload();
        for (ServerPlayer sp : ctx.getSource().getServer().getPlayerList().getPlayers()) {
            com.rtshop.story.Examine.sync(sp);
            com.rtshop.story.BossIntros.sendLore(sp);
            com.rtshop.story.StoryGates.sync(sp);
        }
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

    /** 상인(또는 은행원) NPC 를 소환한다. 알 수 없는 종류면 null. 상인은 개인 이름이 붙는다 (NpcNames). */
    public static Villager spawnNpc(ServerLevel level, Vec3 pos, float yaw, String type) {
        Villager v = spawnNpc0(level, pos, yaw, type);
        if (v != null) {
            NpcNames.refresh(v);
            try {
                com.rtshop.quest.NpcLayout.note(v);   // 자리 배치(npc_layout.json)에 적는다
            } catch (Throwable ignored) {
            }
        }
        return v;
    }

    private static Villager spawnNpc0(ServerLevel level, Vec3 pos, float yaw, String type) {
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
        if (type.equals("raid_guide")) {
            return com.rtshop.raid.Raid.spawnGuide(level, pos, yaw);
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
        v.setYHeadRot(yaw);   // 3.61: 몸과 머리도 그쪽을 보게 (moveTo 는 yRot 만 바꿔, 화면에서는 늘 남쪽을 보고 섰다)
        v.setYBodyRot(yaw);
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
            com.rtshop.notice.Notice.send(player, Component.translatable("rtshop.msg.unknown_shop", shopId));
            return;
        }
        Economy.sync(player);
        SESSIONS.put(player.getUUID(), new Session(false, d.id, npcId));
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenShopPacket(localized(player, Restaurant.is(d) ? Restaurant.viewFor(player, d) : d)));
    }

    /**
     * 3.69.0: 상점 창의 제목 · 상인 이름 · 분류 이름을 받는 사람의 언어로 (사용자 요청 "상점 창 제목과 분류 이름도 게임 언어에 따라").
     * 번역 표에 있는 기본 문구만 바뀌고, 운영자가 고친 이름은 적힌 그대로 나간다.
     */
    public static ShopData localized(ServerPlayer p, ShopData d) {
        if (d == null || !com.rtshop.story.StoryText.en(p)) return d;
        List<ShopData.Category> cats = new ArrayList<>();
        for (ShopData.Category c : d.categories) cats.add(new ShopData.Category(c.id, shopText(p, c.name), c.icon));
        ShopData v = new ShopData(d.id, d.version, shopText(p, d.title), shopText(p, d.npcName), d.profession, cats, d.entries,
                d.starMult, d.fishBase, d.fishBonus, d.fishGolden, d.fishNoInfo);
        v.emptyNote = d.emptyNote;
        v.sellNote = d.sellNote;
        v.sellLockNote = d.sellLockNote;
        return v;
    }

    /** 상점 글 하나: "상점:" 을 붙인 번역(상점에서만 쓰는 뜻) → 그냥 번역 → 원문. */
    public static String shopText(ServerPlayer p, String s) {
        if (s == null || s.isEmpty()) return s;
        String k = "상점:" + s;
        String v = com.rtshop.story.StoryText.t(p, k);
        return v.equals(k) ? com.rtshop.story.StoryText.t(p, s) : v;
    }

    /** 식당 메뉴가 바뀌었다: 상점 데이터를 다시 만들고, 식당 창을 열어 둔 사람들에게 새 메뉴를 알려 준다. */
    public static ShopData rebuildRestaurant(MinecraftServer server) {
        ShopData old = loaded.shops.get(Restaurant.ID);
        if (!Restaurant.is(old)) {
            return null;
        }
        ShopData now = Restaurant.build(loaded, server, old.version);
        if (now == null) {
            return null;
        }
        loaded.shops.put(Restaurant.ID, now);
        if (server != null && server.getPlayerList() != null) {
            int[] menu = new int[Restaurant.menuCount(now)];
            int k = 0;
            for (int i = 0; i < now.entries.size(); i++) {
                if (now.entries.get(i).buyable) menu[k++] = i;
            }
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                Session s = SESSIONS.get(p.getUUID());
                if (s != null && !s.bank() && Restaurant.ID.equals(s.shopId())) {
                    ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new com.rtshop.net.ShopMenuPacket(Restaurant.ID, now.version, menu));
                }
            }
        }
        return now;
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
            if (r[1] < 0) {
                sendResult(player, Component.translatable("rtshop.restaurant.cook_only"), false);
            } else if (r[0] == 0) {
                sendResult(player, Component.translatable("rtshop.msg.no_item"), false);
            } else if (r[1] <= 0) {
                sendResult(player, Component.translatable("rtshop.msg.sell_zero"), false);
            } else {
                sendResult(player, Component.translatable("rtshop.msg.sold",
                        entry.stack.getHoverName(), r[0], num(r[1])), true);
            }
        } else {
            buy(player, d.id, entry, amount);
        }
    }

    public static void handleSellAll(ServerPlayer player, String shopId, int version, int[] indexes, long[] keys) {
        ShopData d = validate(player, shopId, version);
        if (d == null) {
            return;
        }
        if (Restaurant.is(d) && !Restaurant.canSell(player)) {
            sendResult(player, Component.translatable("rtshop.restaurant.cook_only"), false);
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

    private static void buy(ServerPlayer player, String shopId, ShopData.Entry entry, int amount) {
        if (!entry.buyable) {
            sendResult(player, Component.translatable("rtshop.msg.not_buyable"), false);
            return;
        }
        String lock = com.rtshop.story.StoryGates.lockedChapter(player, entry.stack);
        if (lock != null) {
            sendResult(player, Component.translatable("rtshop.msg.story_locked", com.rtshop.story.StoryGates.chapterName(lock)), false);
            return;
        }
        boolean firstDeed = entry.stack.is(ModItems.FIRST_LAND_DEED.get());
        if (firstDeed && (amount != 1 || LandManager.firstDeedUsed(player))) {
            sendResult(player, Component.translatable("rtshop.msg.first_deed_once"), false);
            return;
        }
        // 하루에 살 수 있는 수가 정해진 물건 (고급 귀환권)
        int left = com.rtshop.rules.TownSpawn.dailyLeft(player, entry.stack);
        if (amount > left) {
            sendResult(player, Component.translatable("rtshop.msg.daily_limit", entry.stack.getHoverName(), com.rtshop.rules.TownSpawn.get().premiumDailyLimit, left), false);
            return;
        }
        long total = com.rtshop.rank.Ranks.minus(player, "buy_discount", (long) entry.price * amount);   // 칭호 효과: 단골 할인
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
        com.rtshop.rank.Ranks.spent(player, total);
        com.rtshop.stock.Stocks.onShopBuy(player, shopId, total);   // 조합 실적 (3.72): 대장간 · 토지
        com.rtshop.rules.TownSpawn.bought(player, entry.stack, amount);
        if (firstDeed) {
            LandManager.markFirstDeed(player);
        }
        sendResult(player, Component.translatable("rtshop.msg.bought",
                entry.stack.getHoverName(), amount, num(total)), true);
    }

    /** 수입 기록(3.68.0)의 판매 출처: 물고기 · 농산물(농작물 상점) · 요리와 음식 · 캐낸 광물 · 그 밖. */
    static String incomeSource(ShopData d, ShopData.Entry entry, boolean restaurant) {
        if (entry.fish) return "sell_fish";
        if ("farmer".equals(d.id)) return "sell_farm";
        if (restaurant || ShopLoader.handCooked(loaded, entry.stack) || entry.stack.isEdible()) return "sell_food";
        if (minedGood(entry.stack)) return "sell_mined";
        return "sell_other";
    }

    public static String incomeSourceForTest(ShopData d, ShopData.Entry entry) {
        return incomeSource(d, entry, false);
    }

    /** 직업 판매 보너스: 어부는 수산물, 요리사는 손으로 조리한 요리, 광부는 캐낸 광석 · 원석 · 보석 (3.67.0). */
    public static double jobSellMultiplier(ServerPlayer player, ShopData.Entry entry) {
        double m = 1.0;
        if (entry.fish) {
            m += com.rtshop.job.Jobs.stat(player, "fishSell");
        }
        if (minedGood(entry.stack)) {
            m += com.rtshop.job.Jobs.stat(player, "oreSell");
        }
        if (ShopLoader.handCooked(loaded, entry.stack)) {
            m += com.rtshop.job.Jobs.stat(player, "dishSell");
        }
        m *= 1.0 + com.rtshop.rank.Ranks.effect(player, "sell_bonus") / 100.0;   // 칭호 효과: 장사 수완
        return m;
    }

    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item>[] MINED = mined("forge:ores", "forge:raw_materials", "forge:gems", "forge:dusts/redstone");

    @SuppressWarnings("unchecked")
    private static net.minecraft.tags.TagKey<net.minecraft.world.item.Item>[] mined(String... tags) {
        net.minecraft.tags.TagKey<net.minecraft.world.item.Item>[] out = new net.minecraft.tags.TagKey[tags.length];
        for (int i = 0; i < tags.length; i++) out[i] = net.minecraft.tags.ItemTags.create(new net.minecraft.resources.ResourceLocation(tags[i]));
        return out;
    }

    /**
     * 광부가 판매 보너스를 받는 '캐낸 것': 광석 블록 · 원석 · 보석 · 레드스톤 가루 · 석탄 (3.67.0).
     * 주괴 · 조각처럼 만들어 내는 것, 숯처럼 상점 재료로 구워 만드는 것은 뺀다 (사서 만들어 팔아 남는 길이 생기지 않게).
     */
    public static boolean minedGood(ItemStack s) {
        if (s.isEmpty() || s.is(net.minecraft.world.item.Items.CHARCOAL)) return false;
        if (s.is(net.minecraft.world.item.Items.COAL)) return true;
        for (net.minecraft.tags.TagKey<net.minecraft.world.item.Item> t : MINED) {
            if (s.is(t)) return true;
        }
        return false;
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
        boolean restaurant = Restaurant.is(d);
        if (restaurant && !Restaurant.canSell(player)) {
            return new long[]{0, -1}; // 요리사만 팔 수 있다
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
        com.rtshop.rank.Ranks.earned(player, revenue);
        com.rtshop.stats.IncomeStats.add(player, incomeSource(d, entry, restaurant), revenue);
        if (restaurant) {
            try {
                Restaurant.onSold(player, d, index);
            } catch (Throwable t) {
                RtShop.LOGGER.warn("[rtshop] 식당 메뉴 등록 실패", t);
            }
        }
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

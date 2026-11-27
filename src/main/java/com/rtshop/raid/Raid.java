package com.rtshop.raid;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.rtshop.RtShop;
import com.rtshop.economy.Economy;
import com.rtshop.item.ModItems;
import com.rtshop.net.RaidPacket;
import com.rtshop.net.ShopNet;
import com.rtshop.pvp.Pvp;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityMobGriefingEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.DoubleSupplier;

/**
 * 주간 레이드.
 *  - 레이드 안내인에게 말을 걸어 (짧은 대화 → 입장 화면 → 한 번 더 확인) 참가 티켓을 쓰고 레이드 구역으로 들어간다.
 *  - 구역 가운데 제단에 '레이드 소환의 눈'을 쓰면 보스가 나온다. 그 뒤로는 다른 사람이 들어올 수 없다.
 *  - 구역 안에서는 블록을 놓거나 부술 수 없고, PvP 도 안 된다. 쓰러지면 죽는 대신 관전 상태가 된다 (아이템을 잃지 않음).
 *  - 구역에 아무도 남지 않으면 보스는 사라진다 (처치로 치지 않고 드랍도 없음).
 *  - 플레이어의 공격(마법 포함)으로 보스가 쓰러졌을 때만 보상을 준다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class Raid {
    private Raid() {
    }

    public static final String GUIDE_TAG = "rtshop_raid_guide";
    public static final String BOSS_TAG = "rtshop_raid_boss";
    private static final String NBT_IN = "rtshop_raid_in";
    private static final String NBT_FROM = "rtshop_raid_from";
    private static final String NBT_MODE = "rtshop_raid_mode";

    public static final int IDLE = 0, ACTIVE = 1, DONE = 2, FAILED = 3;

    public static final int ACT_ENTER = 1, ACT_VOTE = 2, ACT_LEAVE = 3, ACT_EDIT_SAVE = 4, ACT_EDIT_ADD = 5;

    private static int state = IDLE;
    private static UUID bossId;
    private static long until;
    private static boolean rewarded;
    private static final Set<UUID> VOTED = new HashSet<>();
    private static final Map<UUID, Integer> SESSIONS = new HashMap<>();
    /** 점검용: 드랍 굴림 값 (0~1). null 이면 진짜 난수. */
    public static DoubleSupplier testRandom;
    public static String lastBroadcast = "";

    static {
        Pvp.EXTRA_SAFE.add((level, x, z) -> {
            RaidConfig c = RaidConfig.get();
            return c.enabled && Pvp.dim(level).equals(c.arenaDim) && Math.abs(x - c.arenaX) <= c.arenaRadius + 2 && Math.abs(z - c.arenaZ) <= c.arenaRadius + 2;
        });
    }

    /** 점검용: 접속자 목록에 없는 시험용 플레이어도 구역 안 사람으로 센다. */
    public static final List<ServerPlayer> TEST_PLAYERS = new ArrayList<>();

    public static int state() {
        return state;
    }

    // ------------------------------------------------------------------ 구역

    public static ServerLevel arenaLevel(MinecraftServer server) {
        String dim = RaidConfig.get().arenaDim;
        for (ServerLevel l : server.getAllLevels()) {
            if (Pvp.dim(l).equals(dim)) return l;
        }
        return server.overworld();
    }

    public static boolean inArena(net.minecraft.world.level.Level level, double x, double y, double z) {
        RaidConfig c = RaidConfig.get();
        if (!c.enabled || level == null || !Pvp.dim(level).equals(c.arenaDim)) {
            return false;
        }
        return Math.abs(x - (c.arenaX + 0.5)) <= c.arenaRadius + 2.5 && Math.abs(z - (c.arenaZ + 0.5)) <= c.arenaRadius + 2.5
                && y >= c.arenaY - 4 && y <= c.arenaY + c.arenaHeight + 4;
    }

    public static boolean inArena(Entity e) {
        return inArena(e.level(), e.getX(), e.getY(), e.getZ());
    }

    public static BlockPos altarPos() {
        RaidConfig c = RaidConfig.get();
        return new BlockPos(c.arenaX, c.arenaY + 1, c.arenaZ);
    }

    public static List<ServerPlayer> playersIn(MinecraftServer server) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (inArena(p)) out.add(p);
        }
        for (ServerPlayer p : TEST_PLAYERS) {
            if (inArena(p) && !out.contains(p)) out.add(p);
        }
        return out;
    }

    private static Block block(String id, Block fallback) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        Block b = rl == null ? null : ForgeRegistries.BLOCKS.getValue(rl);
        return b == null || b == Blocks.AIR ? fallback : b;
    }

    /** 레이드 구역을 짓는다: 둥근 바닥, 낮은 담과 그 위의 보이지 않는 벽·천장, 가운데 제단. 놓은 블록 수. */
    public static int build(ServerLevel level) {
        RaidConfig c = RaidConfig.get();
        int R = c.arenaRadius, H = c.arenaHeight, cx = c.arenaX, cy = c.arenaY, cz = c.arenaZ;
        int n = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int dx = -R - 2; dx <= R + 2; dx++) {
            for (int dz = -R - 2; dz <= R + 2; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > R + 2.5) continue;
                level.getChunk((cx + dx) >> 4, (cz + dz) >> 4);
                boolean wall = d > R + 0.5;
                for (int y = cy - 2; y <= cy + H + 1; y++) {
                    BlockState st;
                    if (y == cy - 2) {
                        st = Blocks.BEDROCK.defaultBlockState();
                    } else if (y == cy - 1) {
                        st = Blocks.BLACKSTONE.defaultBlockState();
                    } else if (y == cy) {
                        if (wall) st = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
                        else if (d < 3.5) st = Blocks.GILDED_BLACKSTONE.defaultBlockState();
                        else if (Math.abs(d - 9) < 0.6 || Math.abs(d - 18) < 0.6) st = Blocks.CRYING_OBSIDIAN.defaultBlockState();
                        else if ((Math.abs(dx) % 7 == 0 && Math.abs(dz) % 7 == 0) && d > 4) st = Blocks.SHROOMLIGHT.defaultBlockState();
                        else st = ((dx + dz) & 1) == 0 ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState() : Blocks.POLISHED_BLACKSTONE.defaultBlockState();
                    } else if (wall) {
                        st = y <= cy + 4 ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState() : Blocks.BARRIER.defaultBlockState();
                    } else if (y == cy + H + 1) {
                        st = Blocks.BARRIER.defaultBlockState();
                    } else {
                        st = air;
                    }
                    pos.set(cx + dx, y, cz + dz);
                    if (level.getBlockState(pos) != st) {
                        level.removeBlockEntity(pos);
                        level.setBlock(pos, st, 2);
                        n++;
                    }
                }
            }
        }
        // 담 위의 불빛
        for (int i = 0; i < 16; i++) {
            double a = Math.PI * 2 * i / 16;
            pos.set(cx + (int) Math.round(Math.cos(a) * (R + 1)), cy + 5, cz + (int) Math.round(Math.sin(a) * (R + 1)));
            level.setBlock(pos, Blocks.SHROOMLIGHT.defaultBlockState(), 2);
        }
        // 가운데 제단 (Cataclysm 의 불의 제단이 있으면 그 모양으로)
        level.setBlock(altarPos(), block("cataclysm:altar_of_fire", Blocks.LODESTONE).defaultBlockState(), 2);
        c.arenaBuilt = true;
        c.save();
        return n + 17;
    }

    public static void ensureArena(MinecraftServer server) {
        RaidConfig c = RaidConfig.get();
        ServerLevel level = arenaLevel(server);
        if (!c.arenaBuilt || level.getBlockState(altarPos()).isAir()) {
            int n = build(level);
            RtShop.LOGGER.info("[rtshop] 레이드 구역을 지었습니다 ({} {} {} {}, 블록 {})", c.arenaDim, c.arenaX, c.arenaY, c.arenaZ, n);
        }
    }

    // ------------------------------------------------------------------ 플레이어 상태

    private static CompoundTag keep(Player p) {
        CompoundTag root = p.getPersistentData();
        CompoundTag k = root.getCompound(Player.PERSISTED_NBT_TAG);
        root.put(Player.PERSISTED_NBT_TAG, k);
        return k;
    }

    public static boolean flagged(Player p) {
        return p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getBoolean(NBT_IN);
    }

    public static boolean downed(Player p) {
        return p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).contains(NBT_MODE);
    }

    private static void restoreMode(ServerPlayer p) {
        CompoundTag k = keep(p);
        if (k.contains(NBT_MODE)) {
            GameType t = GameType.byId(k.getInt(NBT_MODE));
            k.remove(NBT_MODE);
            if (!(p instanceof FakePlayer)) {
                p.setGameMode(t);
            }
        }
    }

    public static int kills(ServerPlayer p) {
        RaidConfig c = RaidConfig.get();
        return RaidData.get(p.server).of(p.getUUID()).kills.getOrDefault(c.requireKill, 0);
    }

    public static boolean weeklyClaimed(ServerPlayer p) {
        RaidConfig c = RaidConfig.get();
        return RaidData.get(p.server).of(p.getUUID()).weeklyWeek == Weekly.id(Weekly.now(), c.resetDay, c.resetHour);
    }

    private static int countItem(ServerPlayer p, net.minecraft.world.item.Item item) {
        int n = 0;
        for (ItemStack s : p.getInventory().items) if (s.is(item)) n += s.getCount();
        if (p.getOffhandItem().is(item)) n += p.getOffhandItem().getCount();
        return n;
    }

    private static void removeItem(ServerPlayer p, net.minecraft.world.item.Item item, int max) {
        p.getInventory().clearOrCountMatchingItems(s -> s.is(item), max, p.inventoryMenu.getCraftSlots());
    }

    private static void say(ServerPlayer p, String text, ChatFormatting color) {
        com.rtshop.notice.Notice.send(p, Component.literal(text).withStyle(color));
    }

    private static void title(ServerPlayer p, String title, String sub, ChatFormatting color) {
        if (p instanceof FakePlayer || p.connection == null) return;
        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 90, 25));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(sub).withStyle(ChatFormatting.GRAY)));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title).withStyle(color, ChatFormatting.BOLD)));
    }

    private static void send(ServerPlayer p, int kind, JsonObject o) {
        if (p instanceof FakePlayer || p.connection == null) return;
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new RaidPacket(kind, o.toString()));
    }

    // ------------------------------------------------------------------ 안내인 창

    /** 지금 들어갈 수 없는 이유 (없으면 null). */
    public static String cannotEnter(ServerPlayer p) {
        RaidConfig c = RaidConfig.get();
        if (!c.enabled) return com.rtshop.story.StoryText.pick(p, "레이드가 꺼져 있습니다.", "The raid is turned off.");
        if (state != IDLE) return c.textFor(p, "locked");
        if (!c.requireKill.isBlank() && c.requireKills > 0 && kills(p) < c.requireKills) return c.textFor(p, "needKill", c.requireKills, kills(p));
        if (countItem(p, ModItems.RAID_TICKET.get()) < 1 && !p.getAbilities().instabuild) return c.textFor(p, "needTicket");
        return null;
    }

    public static JsonObject view(ServerPlayer p) {
        RaidConfig c = RaidConfig.get();
        JsonObject o = new JsonObject();
        JsonObject t = new JsonObject();
        for (String k : RaidConfig.defaultTexts().keySet()) t.addProperty(k, c.textFor(p, k));
        o.add("texts", t);
        String why = cannotEnter(p);
        o.addProperty("boss", c.boss);
        o.addProperty("canEnter", why == null);
        o.addProperty("why", why == null ? "" : why);
        o.addProperty("kills", kills(p));
        o.addProperty("needKills", c.requireKill.isBlank() ? 0 : c.requireKills);
        o.addProperty("tickets", countItem(p, ModItems.RAID_TICKET.get()));
        o.addProperty("state", state);
        o.addProperty("inside", playersIn(p.server).size());
        o.addProperty("weeklyClaimed", weeklyClaimed(p));
        o.addProperty("resetIn", Weekly.untilReset(Weekly.now(), c.resetDay, c.resetHour));
        o.addProperty("weeklyGold", c.weeklyGold);
        o.addProperty("health", c.healthMultiplier);
        o.addProperty("damage", c.damageMultiplier);
        o.addProperty("rollsFresh", c.rollsFresh);
        o.addProperty("rollsRepeat", c.rollsRepeat);
        JsonArray cf = new JsonArray(), cr = new JsonArray();
        for (double d : c.chanceFresh) cf.add(d);
        for (double d : c.chanceRepeat) cr.add(d);
        o.add("chanceFresh", cf);
        o.add("chanceRepeat", cr);
        JsonArray rw = new JsonArray();
        for (Reward r : c.rewards) {
            if (r.stack().isEmpty()) continue; // 이 게임에 없는 아이템
            rw.add(r.toJson());
        }
        o.add("rewards", rw);
        return o;
    }

    public static void open(ServerPlayer p, int npcId) {
        SESSIONS.put(p.getUUID(), npcId);
        send(p, RaidPacket.VIEW, view(p));
    }

    public static Villager spawnGuide(ServerLevel level, Vec3 pos, float yaw) {
        Villager v = EntityType.VILLAGER.create(level);
        if (v == null) return null;
        v.moveTo(pos.x, pos.y, pos.z, yaw, 0.0F);
        v.setYHeadRot(yaw);   // 3.61: 몸과 머리도 그쪽을 보게 (moveTo 는 yRot 만 바꿔, 화면에서는 늘 남쪽을 보고 섰다)
        v.setYBodyRot(yaw);
        v.setNoAi(true);
        v.setInvulnerable(true);
        v.setSilent(true);
        v.setPersistenceRequired();
        v.addTag(GUIDE_TAG);
        v.setCustomName(Component.literal(RaidConfig.get().text("npcName")));
        v.setCustomNameVisible(true);
        v.setVillagerData(new VillagerData(VillagerType.SAVANNA, VillagerProfession.WEAPONSMITH, 5));
        level.addFreshEntity(v);
        return v;
    }

    // ------------------------------------------------------------------ 잠금: 보스를 쓰러뜨릴 때마다 열린다

    /** 이 사람이 쓰러뜨린 것으로 적힌 보스의 처치 수 (보스 ID 별). */
    public static int bossKills(ServerPlayer p, String bossId) {
        return RaidData.get(p.server).of(p.getUUID()).kills.getOrDefault(bossId, 0);
    }

    /**
     * 이름 있는 보스를 하나라도 쓰러뜨렸는가. 처치 기록이 없던 예전 세계를 위해, 보스를 잡는 퀘스트를 마친 사람도 그렇게 본다.
     */
    public static boolean firstBossDown(ServerPlayer p) {
        RaidData.Entry e = RaidData.get(p.server).of(p.getUUID());
        com.rtshop.party.PartyConfig pc = com.rtshop.party.PartyConfig.get();
        for (Map.Entry<String, Integer> k : e.kills.entrySet()) {
            if (k.getValue() > 0 && (pc.bosses.contains(k.getKey()) || pc.arenaBosses.contains(k.getKey()) || k.getKey().equals(RaidConfig.get().requireKill))) return true;
        }
        com.rtshop.quest.QuestData.Progress pr = com.rtshop.quest.QuestData.get(p.server).peek(p.getUUID());
        if (pr == null) return false;
        for (String qid : pr.completed) {
            com.rtshop.quest.QuestDef q = com.rtshop.quest.QuestManager.loaded().quests.get(qid);
            if (q == null) continue;
            for (com.rtshop.quest.QuestDef.Objective ob : q.objectives) {
                if (ob.type == com.rtshop.quest.QuestDef.Type.KILL && (pc.bosses.contains(ob.target) || pc.arenaBosses.contains(ob.target))) return true;
            }
        }
        return false;
    }

    /** 레이드 관련 NPC(안내인·레이드 상점)가 아직 이 사람의 말을 받아 주지 않는가. 운영자의 크리에이티브는 통과. */
    public static boolean npcLocked(ServerPlayer p) {
        if (!RaidConfig.get().npcsAfterFirstBoss) return false;
        if (p.isCreative() && com.rtshop.util.Perm.op(p)) return false;
        return !firstBossDown(p);
    }

    private static boolean raidNpc(net.minecraft.world.entity.Entity e) {
        return e.getTags().contains(GUIDE_TAG) || e.getTags().contains(com.rtshop.shop.ShopServer.SHOP_TAG_PREFIX + "raid");
    }

    /** 레이드 관련 NPC 는 첫 보스를 쓰러뜨리기 전에는 상호작용(대화·상점·입장 창) 자체가 열리지 않는다. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLockedNpc(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() || !raidNpc(event.getTarget())) return;
        if (!(event.getEntity() instanceof ServerPlayer sp) || !npcLocked(sp)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            com.rtshop.notice.Notice.send(sp, Component.literal(event.getTarget().getName().getString()).withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(": " + com.rtshop.story.StoryText.t(sp, RaidConfig.get().text("npcLocked"))).withStyle(ChatFormatting.WHITE)));
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() || !event.getTarget().getTags().contains(GUIDE_TAG)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getHand() == InteractionHand.MAIN_HAND && event.getEntity() instanceof ServerPlayer sp) {
            open(sp, event.getTarget().getId());
        }
    }

    // ------------------------------------------------------------------ 입장 / 퇴장

    public static boolean enter(ServerPlayer p) {
        RaidConfig c = RaidConfig.get();
        String why = cannotEnter(p);
        if (why != null) {
            say(p, why, ChatFormatting.RED);
            return false;
        }
        Integer npc = SESSIONS.get(p.getUUID());
        if (npc != null && npc >= 0 && !(p instanceof FakePlayer)) {
            Entity e = p.level().getEntity(npc);
            if (e == null || e.distanceToSqr(p) > 12 * 12) {
                say(p, com.rtshop.story.StoryText.pick(p, "안내인에게서 너무 멀리 있습니다.", "You are too far from the guide."), ChatFormatting.RED);
                return false;
            }
        }
        if (!p.getAbilities().instabuild) {
            removeItem(p, ModItems.RAID_TICKET.get(), 1);
        }
        ensureArena(p.server);
        ServerLevel level = arenaLevel(p.server);
        keep(p).putBoolean(NBT_IN, true);
        // 들어오기 전 자리를 기억해 두었다가 그곳으로 돌려보낸다
        if (!inArena(p)) {
            CompoundTag from = new CompoundTag();
            from.putString("dim", Pvp.dim(p.level()));
            from.putDouble("x", p.getX());
            from.putDouble("y", p.getY());
            from.putDouble("z", p.getZ());
            from.putFloat("yaw", p.getYRot());
            keep(p).put(NBT_FROM, from);
        }
        if (countItem(p, ModItems.RAID_EYE.get()) < 1) Reward.give(p, new ItemStack(ModItems.RAID_EYE.get()));
        if (countItem(p, ModItems.RAID_ESCAPE.get()) < 1) Reward.give(p, new ItemStack(ModItems.RAID_ESCAPE.get()));
        if (p instanceof FakePlayer) {
            p.setPos(c.arenaX + 0.5, c.arenaY + 1, c.arenaZ + c.arenaRadius - 5 + 0.5);
        } else {
            p.teleportTo(level, c.arenaX + 0.5, c.arenaY + 1, c.arenaZ + c.arenaRadius - 5 + 0.5, 180F, 0F);
        }
        say(p, c.textFor(p, "entered"), ChatFormatting.GOLD);
        return true;
    }

    /** 마을(돌아갈 곳)로 보낸다: 관전 상태를 풀고, 레이드 전용 아이템을 거둔다. */
    public static void sendHome(ServerPlayer p) {
        RaidConfig c = RaidConfig.get();
        restoreMode(p);
        keep(p).remove(NBT_IN);
        CompoundTag from = keep(p).getCompound(NBT_FROM);
        keep(p).remove(NBT_FROM);
        PENDING_ESCAPE.remove(p.getUUID());
        // 열려 있는 레이드 창(투표·실패)을 닫게 한다
        JsonObject close = new JsonObject();
        close.addProperty("mode", "close");
        send(p, RaidPacket.PROMPT, close);
        removeItem(p, ModItems.RAID_EYE.get(), -1);
        removeItem(p, ModItems.RAID_ESCAPE.get(), -1);
        if (p instanceof FakePlayer) {
            p.setPos(c.returnSet ? c.returnX : 0, c.returnSet ? c.returnY : 100, c.returnSet ? c.returnZ : 0);
            return;
        }
        ServerLevel to = p.server.overworld();
        double x, y, z;
        float yaw = p.getYRot();
        ServerLevel fromLevel = null;
        if (from.contains("dim")) {
            for (ServerLevel l : p.server.getAllLevels()) {
                if (Pvp.dim(l).equals(from.getString("dim"))) fromLevel = l;
            }
        }
        if (fromLevel != null && !inArena(fromLevel, from.getDouble("x"), from.getDouble("y"), from.getDouble("z"))) {
            to = fromLevel;
            x = from.getDouble("x");
            y = from.getDouble("y");
            z = from.getDouble("z");
            yaw = from.getFloat("yaw");
        } else if (c.returnSet) {
            for (ServerLevel l : p.server.getAllLevels()) {
                if (Pvp.dim(l).equals(c.returnDim)) to = l;
            }
            x = c.returnX;
            y = c.returnY;
            z = c.returnZ;
        } else {
            BlockPos s = to.getSharedSpawnPos();
            x = s.getX() + 0.5;
            y = s.getY();
            z = s.getZ() + 0.5;
        }
        p.setHealth(Math.max(p.getHealth(), 1));
        p.clearFire();
        p.fallDistance = 0;
        if (p.isUsingItem()) p.stopUsingItem();
        if (p.isPassenger()) p.stopRiding();
        if (p.containerMenu != p.inventoryMenu) p.closeContainer();
        // 도착 지점의 청크를 먼저 불러오고, 블록 속이면 위로 올린다
        BlockPos at = BlockPos.containing(x, y, z);
        to.getChunk(at.getX() >> 4, at.getZ() >> 4);
        for (int i = 0; i < 64 && (!to.getBlockState(at).getCollisionShape(to, at).isEmpty() || !to.getBlockState(at.above()).getCollisionShape(to, at.above()).isEmpty()); i++) {
            at = at.above();
            y = at.getY();
        }
        p.teleportTo(to, x, y, z, yaw, 0F);
    }

    /** 탈출권을 쓴 사람: 아이템을 쓰는 도중에 순간이동하지 않도록 다음 틱에 돌려보낸다. */
    private static final java.util.Set<UUID> PENDING_ESCAPE = new java.util.HashSet<>();

    /** 탈출권 사용 (곧바로 돌려보낸다: 명령·점검용). */
    public static boolean escape(ServerPlayer p) {
        if (!inArena(p) && !flagged(p)) {
            say(p, com.rtshop.story.StoryText.pick(p, "레이드 구역 안에서만 쓸 수 있습니다.", "You can only use this inside the raid arena."), ChatFormatting.RED);
            return false;
        }
        sendHome(p);
        say(p, RaidConfig.get().textFor(p, "escaped"), ChatFormatting.YELLOW);
        return true;
    }

    /** 탈출권 아이템: 이번 틱이 끝난 뒤 돌려보내도록 예약한다. */
    public static boolean requestEscape(ServerPlayer p) {
        if (!inArena(p) && !flagged(p)) {
            say(p, com.rtshop.story.StoryText.pick(p, "레이드 구역 안에서만 쓸 수 있습니다.", "You can only use this inside the raid arena."), ChatFormatting.RED);
            return false;
        }
        PENDING_ESCAPE.add(p.getUUID());
        return true;
    }

    private static void runEscapes(MinecraftServer server) {
        if (PENDING_ESCAPE.isEmpty()) return;
        List<UUID> ids = new ArrayList<>(PENDING_ESCAPE);
        PENDING_ESCAPE.clear();
        for (UUID id : ids) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p == null) continue;
            try {
                escape(p);
            } catch (Throwable t) {
                RtShop.LOGGER.error("[rtshop] 레이드 탈출 실패: {}", p.getGameProfile().getName(), t);
            }
        }
    }

    // ------------------------------------------------------------------ 보스

    private static Entity boss(MinecraftServer server) {
        return bossId == null ? null : arenaLevel(server).getEntity(bossId);
    }

    /** 제단에 소환의 눈을 썼다. */
    public static boolean summon(ServerPlayer p) {
        RaidConfig c = RaidConfig.get();
        if (state != IDLE) {
            say(p, c.textFor(p, "alreadySummoned"), ChatFormatting.RED);
            return false;
        }
        ServerLevel level = arenaLevel(p.server);
        ResourceLocation rl = ResourceLocation.tryParse(c.boss);
        EntityType<?> type = rl == null || !ForgeRegistries.ENTITY_TYPES.containsKey(rl) ? null : ForgeRegistries.ENTITY_TYPES.getValue(rl);
        Entity e = type == null ? null : type.create(level);
        if (!(e instanceof LivingEntity living)) {
            say(p, com.rtshop.story.StoryText.pick(p, "레이드 보스를 찾을 수 없습니다: " + c.boss + " (raid.json 의 boss)", "The raid boss could not be found: " + c.boss + " (boss in raid.json)"), ChatFormatting.RED);
            return false;
        }
        living.moveTo(c.arenaX + 0.5, c.arenaY + 1, c.arenaZ - 9 + 0.5, 0F, 0F);
        if (living instanceof Mob mob) {
            try {
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.EVENT, null, null);
            } catch (Throwable t) {
                RtShop.LOGGER.debug("[rtshop] raid boss finalizeSpawn", t);
            }
            mob.setPersistenceRequired();
        }
        // 체력 배율은 최대 체력을 늘리는 대신 "받는 피해를 그만큼 줄이는" 방식으로 건다
        // (게임의 최대 체력 한도 1024 에 걸려 배율이 먹지 않는 일이 없게. 체력 2배 = 받는 피해 절반)
        living.setHealth(living.getMaxHealth());
        living.addTag(BOSS_TAG);
        bossId = living.getUUID();
        state = ACTIVE;
        rewarded = false;
        VOTED.clear();
        level.addFreshEntity(living);
        for (ServerPlayer o : playersIn(p.server)) {
            say(o, c.textFor(o, "summoned", p.getGameProfile().getName()), ChatFormatting.RED);
        }
        return true;
    }

    private static void reset(MinecraftServer server) {
        Entity b = boss(server);
        if (b != null && b.isAlive()) {
            b.discard();
        }
        state = IDLE;
        bossId = null;
        until = 0;
        VOTED.clear();
    }

    /** 강제로 끝낸다 (OP): 안의 사람을 모두 돌려보내고 보스를 지운다. */
    public static void forceEnd(MinecraftServer server) {
        for (ServerPlayer p : playersIn(server)) sendHome(p);
        reset(server);
    }

    private static void prompt(ServerPlayer p, String mode, int seconds) {
        RaidConfig c = RaidConfig.get();
        JsonObject o = new JsonObject();
        o.addProperty("mode", mode);
        o.addProperty("seconds", seconds);
        o.addProperty("agreed", VOTED.size());
        o.addProperty("total", playersIn(p.server).size());
        o.addProperty("voted", VOTED.contains(p.getUUID()));
        JsonObject t = new JsonObject();
        for (String k : new String[]{"voteTitle", "voteAgree", "voteStay", "voteCount", "failTitle", "failSub", "failConfirm", "failBody", "successTitle", "returnNotice"}) {
            t.addProperty(k, c.textFor(p, k));
        }
        o.add("texts", t);
        send(p, RaidPacket.PROMPT, o);
    }

    /** 모두 쓰러졌다. */
    private static void fail(MinecraftServer server) {
        RaidConfig c = RaidConfig.get();
        Entity b = boss(server);
        if (b != null) b.discard();
        state = FAILED;
        until = Weekly.now() + c.failReturnSeconds * 1000L;
        for (ServerPlayer p : playersIn(server)) {
            title(p, c.textFor(p, "failTitle"), c.textFor(p, "failSub"), ChatFormatting.DARK_RED);
            prompt(p, "fail", c.failReturnSeconds);
        }
    }

    /** 보스가 쓰러졌다. byPlayer: 플레이어의 공격(마법·화살·소환수 포함)으로 죽었는가. */
    public static void bossDead(MinecraftServer server, boolean byPlayer) {
        RaidConfig c = RaidConfig.get();
        if (state != ACTIVE) return;
        state = DONE;
        until = Weekly.now() + c.returnSeconds * 1000L;
        VOTED.clear();
        List<ServerPlayer> in = playersIn(server);
        for (ServerPlayer p : in) restoreMode(p);
        if (!byPlayer) {
            for (ServerPlayer p : in) say(p, c.textFor(p, "notByPlayer"), ChatFormatting.YELLOW);
        } else {
            rewarded = true;
            for (ServerPlayer p : in) {
                title(p, c.textFor(p, "successTitle"), c.textFor(p, "successSub"), ChatFormatting.GOLD);
                com.rtshop.rank.Ranks.add(p, com.rtshop.rank.Ranks.RAIDS, 1);
                try {
                    reward(p);
                } catch (Throwable t) {
                    RtShop.LOGGER.error("[rtshop] 레이드 보상 지급 실패: {}", p.getGameProfile().getName(), t);
                }
            }
        }
        for (ServerPlayer p : in) {
            say(p, c.textFor(p, "returnNotice", c.returnSeconds), ChatFormatting.AQUA);
            prompt(p, "vote", c.returnSeconds);
        }
    }

    public static boolean wasRewarded() {
        return rewarded;
    }

    /** 성공 보상: 이번 주 처음이면 주간 보상 전부 + 높은 확률의 드랍 굴림, 이미 받았으면 낮은 확률의 드랍 굴림. 받은 드랍 수를 돌려준다. */
    public static int reward(ServerPlayer p) {
        RaidConfig c = RaidConfig.get();
        RaidData data = RaidData.get(p.server);
        RaidData.Entry e = data.of(p.getUUID());
        long week = Weekly.id(Weekly.now(), c.resetDay, c.resetHour);
        boolean fresh = e.weeklyWeek != week;
        if (fresh) {
            for (Reward r : c.group(RaidConfig.G_WEEKLY)) {
                ItemStack s = r.stack();
                if (!s.isEmpty()) Reward.give(p, s);
            }
            if (c.weeklyGold > 0) {
                Economy.addWallet(p, c.weeklyGold);
                com.rtshop.rank.Ranks.earned(p, c.weeklyGold);
                com.rtshop.stats.IncomeStats.add(p, "raid", c.weeklyGold);
            }
            e.weeklyWeek = week;
            say(p, c.textFor(p, "weeklyGot"), ChatFormatting.GREEN);
        } else {
            say(p, c.textFor(p, "weeklyAlready"), ChatFormatting.GRAY);
        }
        e.clears++;
        data.touch();
        double[] ch = fresh ? c.chanceFresh : c.chanceRepeat;
        int rolls = fresh ? c.rollsFresh : c.rollsRepeat;
        int got = 0;
        List<Reward> drops = c.group(RaidConfig.G_DROP);
        for (int i = 0; i < rolls; i++) {
            double roll = (testRandom != null ? testRandom.getAsDouble() : p.getRandom().nextDouble()) * 100.0;
            int grade = -1;
            double acc = 0;
            for (int g = ch.length - 1; g >= 0; g--) { // 높은 등급부터
                acc += ch[g];
                if (roll < acc) {
                    grade = g;
                    break;
                }
            }
            if (grade < 0) continue;
            List<Reward> pool = new ArrayList<>();
            for (Reward r : drops) {
                if (r.grade == grade && !r.stack().isEmpty()) pool.add(r);
            }
            if (pool.isEmpty()) continue;
            Reward r = pool.get(p.getRandom().nextInt(pool.size()));
            ItemStack s = r.stack();
            Reward.give(p, s.copy());
            got++;
            String name = s.getHoverName().getString() + (s.getCount() > 1 ? " ×" + s.getCount() : "");
            say(p, c.textFor(p, "dropGot", name), ChatFormatting.LIGHT_PURPLE);
            if (grade == Reward.GRADES.length - 1) {
                lastBroadcast = c.text("mysteryGot", p.getGameProfile().getName(), c.text("mysteryName"), name);
                for (ServerPlayer o : p.server.getPlayerList().getPlayers()) {
                    say(o, c.textFor(o, "mysteryGot", p.getGameProfile().getName(), c.textFor(o, "mysteryName"), name), ChatFormatting.DARK_PURPLE);
                }
            }
        }
        if (got == 0 && rolls > 0) say(p, c.textFor(p, "dropNone"), ChatFormatting.GRAY);
        return got;
    }

    public static void vote(ServerPlayer p) {
        if (state != DONE || !inArena(p)) return;
        VOTED.add(p.getUUID());
        for (ServerPlayer o : playersIn(p.server)) {
            prompt(o, "vote", (int) Math.max(0, (until - Weekly.now()) / 1000));
        }
    }

    // ------------------------------------------------------------------ 틱

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer() == null) return;
        runEscapes(event.getServer());
        if (event.getServer().getTickCount() % 10 != 0) {
            return;
        }
        try {
            tick(event.getServer());
        } catch (Throwable t) {
            RtShop.LOGGER.debug("[rtshop] raid tick", t);
        }
    }

    public static void tick(MinecraftServer server) {
        if (state == IDLE) return;
        RaidConfig c = RaidConfig.get();
        List<ServerPlayer> in = playersIn(server);
        long now = Weekly.now();
        if (state == ACTIVE) {
            Entity b = boss(server);
            if (in.isEmpty()) {
                // 아무도 남지 않았다: 보스는 사라진다 (처치 아님, 드랍 없음)
                if (b != null) b.discard();
                RtShop.LOGGER.info("[rtshop] {}", c.text("despawn"));
                reset(server);
                return;
            }
            if (b == null || !b.isAlive() && !(b instanceof LivingEntity l && l.isDeadOrDying())) {
                reset(server);
                return;
            }
            if (!inArena(b)) {
                b.teleportTo(c.arenaX + 0.5, c.arenaY + 1, c.arenaZ + 0.5);
            }
            boolean anyUp = false;
            for (ServerPlayer p : in) {
                if (!downed(p) && !p.isSpectator()) anyUp = true;
            }
            if (!anyUp) fail(server);
            return;
        }
        boolean all = !in.isEmpty();
        for (ServerPlayer p : in) {
            if (!VOTED.contains(p.getUUID())) all = false;
        }
        if (in.isEmpty() || now >= until || (state == DONE && all)) {
            for (ServerPlayer p : in) sendHome(p);
            reset(server);
        }
    }

    // ------------------------------------------------------------------ 이벤트: 죽음·피해·드랍

    private static boolean isBoss(Entity e) {
        return e != null && e.getTags().contains(BOSS_TAG);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity e = event.getEntity();
        if (e.level().isClientSide()) return;
        MinecraftServer server = e.getServer();
        if (server == null) return;
        RaidConfig c = RaidConfig.get();
        if (e instanceof ServerPlayer p && !(p instanceof FakePlayer)) {
            if (state == ACTIVE && inArena(p)) {
                // 레이드 중에는 죽지 않는다: 관전 상태로 남는다 (아이템·Gold 를 잃지 않음)
                event.setCanceled(true);
                p.setHealth(p.getMaxHealth());
                p.removeAllEffects();
                p.clearFire();
                keep(p).putInt(NBT_MODE, p.gameMode.getGameModeForPlayer().getId());
                p.setGameMode(GameType.SPECTATOR);
                say(p, c.textFor(p, "downed"), ChatFormatting.RED);
            }
            return;
        }
        if (isBoss(e)) {
            if (state == ACTIVE && e.getUUID().equals(bossId)) {
                bossDead(server, Pvp.attackerOf(event.getSource(), server) != null);
            }
            return;
        }
        // 보스 처치 기록 (보스마다): 보스 목록(party.json)에 있으면 체력의 10% 이상(party.json)을 입힌 사람 모두에게 적는다.
        // 레이드 관련 NPC 는 첫 보스 뒤에, 각 레이드는 그 보스(requireKill)를 쓰러뜨린 뒤에 열린다.
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
        if (id != null && com.rtshop.party.BossCredit.isBoss(e)) {
            RaidData data = RaidData.get(server);
            for (ServerPlayer p : com.rtshop.party.BossCredit.qualified(e, server)) {
                boolean first = !firstBossDown(p);
                data.of(p.getUUID()).kills.merge(id.toString(), 1, Integer::sum);
                if (first && c.npcsAfterFirstBoss && !(p instanceof FakePlayer)) {
                    say(p, c.textFor(p, "npcOpened"), ChatFormatting.GOLD);
                }
            }
            data.touch();
            return;
        }
        // 보스 목록에 없는 입장 조건 몬스터: 죽인 사람과 근처 40칸 안의 사람
        if (id != null && !c.requireKill.isBlank() && id.toString().equals(c.requireKill)) {
            RaidData data = RaidData.get(server);
            ServerPlayer killer = Pvp.attackerOf(event.getSource(), server);
            if (killer != null) {
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    if (p == killer || (p.level() == e.level() && p.distanceToSqr(e) <= 40 * 40)) {
                        data.of(p.getUUID()).kills.merge(c.requireKill, 1, Integer::sum);
                    }
                }
                data.touch();
            }
        }
    }

    /** 점검용: 처치 수를 정한다. */
    public static void setKills(ServerPlayer p, int n) {
        RaidData data = RaidData.get(p.server);
        data.of(p.getUUID()).kills.put(RaidConfig.get().requireKill, n);
        data.touch();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDrops(LivingDropsEvent event) {
        if (isBoss(event.getEntity())) {
            event.setCanceled(true); // 레이드 보스의 보상은 보상 표로만 준다
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (isBoss(event.getEntity())) {
            event.setAmount((float) (event.getAmount() / Math.max(0.1, RaidConfig.get().healthMultiplier)));
            return;
        }
        if (!(event.getEntity() instanceof Player)) return;
        Entity src = event.getSource().getEntity();
        Entity direct = event.getSource().getDirectEntity();
        if (isBoss(src) || isBoss(direct)) {
            event.setAmount((float) (event.getAmount() * RaidConfig.get().damageMultiplier));
        }
    }

    /** 서버를 껐다 켠 뒤 남아 있는 예전 레이드 보스는 지운다. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        Entity e = event.getEntity();
        if (isBoss(e) && !(state == ACTIVE && e.getUUID().equals(bossId))) {
            event.setCanceled(true);
        }
    }

    // ------------------------------------------------------------------ 이벤트: 구역 보호

    private static boolean builder(Entity e) {
        return e instanceof ServerPlayer p && p.getAbilities().instabuild && com.rtshop.util.Perm.op(p);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBreak(BlockEvent.BreakEvent event) {
        BlockPos p = event.getPos();
        if (event.getPlayer() != null && inArena(event.getPlayer().level(), p.getX() + 0.5, p.getY(), p.getZ() + 0.5) && !builder(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        BlockPos p = event.getPos();
        if (event.getLevel() instanceof net.minecraft.world.level.Level l && inArena(l, p.getX() + 0.5, p.getY(), p.getZ() + 0.5) && !builder(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBucket(FillBucketEvent event) {
        if (event.getTarget() != null && inArena(event.getLevel(), event.getTarget().getLocation().x, event.getTarget().getLocation().y, event.getTarget().getLocation().z)
                && !builder(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        event.getAffectedBlocks().removeIf(p -> inArena(event.getLevel(), p.getX() + 0.5, p.getY(), p.getZ() + 0.5));
    }

    @SubscribeEvent
    public static void onGrief(EntityMobGriefingEvent event) {
        if (event.getEntity() != null && inArena(event.getEntity())) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onSpawnCheck(MobSpawnEvent.SpawnPlacementCheck event) {
        BlockPos p = event.getPos();
        if (inArena(event.getLevel().getLevel(), p.getX() + 0.5, p.getY(), p.getZ() + 0.5)) {
            event.setResult(Event.Result.DENY);
        }
    }

    /** 구역 안의 블록은 누르거나 열 수 없다 (제단 제외). 손에 든 아이템(음식·탈출권 등)은 그대로 쓸 수 있다. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLeftBlock(PlayerInteractEvent.LeftClickBlock event) {
        BlockPos p = event.getPos();
        if (!event.getLevel().isClientSide() && inArena(event.getLevel(), p.getX() + 0.5, p.getY(), p.getZ() + 0.5) && !builder(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer sp)) return;
        BlockPos bp = event.getPos();
        boolean altar = RaidConfig.get().enabled && Pvp.dim(event.getLevel()).equals(RaidConfig.get().arenaDim) && bp.equals(altarPos());
        if (!altar) {
            if (inArena(event.getLevel(), bp.getX() + 0.5, bp.getY(), bp.getZ() + 0.5) && !builder(sp)) {
                event.setUseBlock(Event.Result.DENY);
            }
            return;
        }
        // 제단: 원래 기능(아이템 넣기)은 막고, 소환의 눈만 받는다
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        ItemStack held = sp.getMainHandItem();
        if (!held.is(ModItems.RAID_EYE.get())) {
            say(sp, com.rtshop.story.StoryText.pick(sp, "제단에는 레이드 소환의 눈을 사용하세요.", "Use a Raid Summoning Eye on the altar."), ChatFormatting.GRAY);
            return;
        }
        if (summon(sp) && !sp.getAbilities().instabuild) {
            held.shrink(1);
        }
    }

    // ------------------------------------------------------------------ 접속 / 종료

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && (flagged(p) || downed(p))) {
            sendHome(p); // 레이드 중에 나갔다 들어오면 마을에서 시작한다
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            SESSIONS.remove(p.getUUID());
            VOTED.remove(p.getUUID());
            restoreMode(p);
        }
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        state = IDLE;
        bossId = null;
        VOTED.clear();
        SESSIONS.clear();
    }

    // ------------------------------------------------------------------ 클라이언트 요청

    public static void handle(ServerPlayer p, int action, String payload) {
        switch (action) {
            case ACT_ENTER -> enter(p);
            case ACT_VOTE -> vote(p);
            case ACT_LEAVE -> {
                if ((state == FAILED || state == DONE) && inArena(p) || downed(p)) sendHome(p);
            }
            case ACT_EDIT_SAVE, ACT_EDIT_ADD -> {
                if (!com.rtshop.util.Perm.op(p)) return;
                try {
                    JsonObject o = JsonParser.parseString(payload).getAsJsonObject();
                    String id = o.has("id") ? o.get("id").getAsString() : "";
                    RewardEditor.Target t = RewardEditor.TARGETS.get(id);
                    if (t == null) return;
                    t.apply(p, o);
                    if (action == ACT_EDIT_ADD) {
                        ItemStack held = p.getMainHandItem();
                        if (held.isEmpty()) {
                            say(p, com.rtshop.story.StoryText.pick(p, "손에 든 아이템이 없습니다. 보상으로 넣을 아이템을 손에 들고 누르세요.", "Your hand is empty. Hold the item to add as a reward, then press the button."), ChatFormatting.RED);
                        } else {
                            t.add(p, Reward.of(o.get("addGroup").getAsString(), o.get("addGrade").getAsInt(), held));
                        }
                    } else {
                        say(p, com.rtshop.story.StoryText.pick(p, "저장했습니다.", "Saved."), ChatFormatting.GREEN);
                    }
                    send(p, RaidPacket.EDITOR, t.model(p));
                } catch (Exception e) {
                    RtShop.LOGGER.warn("[rtshop] 보상 편집 저장 실패", e);
                    say(p, com.rtshop.story.StoryText.pick(p, "저장하지 못했습니다: ", "Could not save: ") + e.getMessage(), ChatFormatting.RED);
                }
            }
            case AbyssWeekly.ACT_VIEW -> AbyssWeekly.sendView(p);
            default -> {
            }
        }
    }

    public static void openEditor(ServerPlayer p, String id) {
        RewardEditor.Target t = RewardEditor.TARGETS.get(id);
        if (t != null) send(p, RaidPacket.EDITOR, t.model(p));
    }

    // ------------------------------------------------------------------ 명령어 (OP)

    private static void ok(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendSuccess(() -> Component.literal(text), true);
    }

    /** 명령의 답을 명령을 친 사람의 언어로. */
    private static void ok(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, String ko, String en) {
        ok(ctx, com.rtshop.story.StoryText.pick(ctx.getSource(), ko, en));
    }

    /** /rtshop raid ... */
    public static LiteralArgumentBuilder<CommandSourceStack> adminCommand() {
        return Commands.literal("raid").requires(src -> src.hasPermission(2))
                .executes(ctx -> {
                    RaidConfig c = RaidConfig.get();
                    ok(ctx, "레이드: 보스 " + c.boss + " · 체력 ×" + c.healthMultiplier + " · 공격 ×" + c.damageMultiplier + " · 상태 "
                            + new String[]{"대기", "진행 중", "성공(귀환 대기)", "실패(귀환 대기)"}[state] + " · 구역 " + c.arenaDim + " " + c.arenaX + " " + c.arenaY + " " + c.arenaZ
                            + " · 안에 " + playersIn(ctx.getSource().getServer()).size() + "명  (/rtshop raid edit)",
                            "Raid: boss " + c.boss + " · health ×" + c.healthMultiplier + " · damage ×" + c.damageMultiplier + " · state "
                            + new String[]{"idle", "in progress", "cleared (waiting to return)", "failed (waiting to return)"}[state] + " · arena " + c.arenaDim + " " + c.arenaX + " " + c.arenaY + " " + c.arenaZ
                            + " · " + playersIn(ctx.getSource().getServer()).size() + " inside  (/rtshop raid edit)");
                    return 1;
                })
                .then(Commands.literal("edit").executes(ctx -> {
                    openEditor(ctx.getSource().getPlayerOrException(), "raid");
                    return 1;
                }))
                .then(Commands.literal("arena")
                        .then(Commands.literal("here").executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            forceEnd(p.server);
                            RaidConfig c = RaidConfig.get();
                            c.arenaDim = Pvp.dim(p.level());
                            c.arenaX = p.getBlockX();
                            c.arenaY = Math.max(p.level().getMinBuildHeight() + 3, Math.min(p.level().getMaxBuildHeight() - c.arenaHeight - 3, p.getBlockY() - 1));
                            c.arenaZ = p.getBlockZ();
                            int n = build(p.serverLevel());
                            ok(ctx, "레이드 구역을 여기에 지었습니다 (블록 " + n + "개). 제단은 가운데에 있습니다.", "Built the raid arena here (" + n + " blocks). The altar is in the middle.");
                            return 1;
                        }))
                        .then(Commands.literal("build").executes(ctx -> {
                            int n = build(arenaLevel(ctx.getSource().getServer()));
                            ok(ctx, "레이드 구역을 다시 지었습니다 (블록 " + n + "개).", "Rebuilt the raid arena (" + n + " blocks).");
                            return 1;
                        }))
                        .then(Commands.literal("tp").executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            RaidConfig c = RaidConfig.get();
                            ensureArena(p.server);
                            p.teleportTo(arenaLevel(p.server), c.arenaX + 0.5, c.arenaY + 1, c.arenaZ + c.arenaRadius - 5 + 0.5, 180F, 0F);
                            return 1;
                        })))
                .then(Commands.literal("return").then(Commands.literal("here").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    RaidConfig c = RaidConfig.get();
                    c.returnSet = true;
                    c.returnDim = Pvp.dim(p.level());
                    c.returnX = p.getX();
                    c.returnY = p.getY();
                    c.returnZ = p.getZ();
                    c.save();
                    ok(ctx, "레이드가 끝나면 돌아올 곳을 여기로 정했습니다.", "Players will return here when the raid ends.");
                    return 1;
                })))
                .then(Commands.literal("end").executes(ctx -> {
                    forceEnd(ctx.getSource().getServer());
                    ok(ctx, "레이드를 끝내고 안에 있던 사람을 모두 돌려보냈습니다.", "Ended the raid and sent everyone inside back.");
                    return 1;
                }))
                .then(Commands.literal("kills").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("count", IntegerArgumentType.integer(0, 9999)).executes(ctx -> {
                            ServerPlayer t = EntityArgument.getPlayer(ctx, "player");
                            setKills(t, IntegerArgumentType.getInteger(ctx, "count"));
                            ok(ctx, t.getGameProfile().getName() + " 의 입장 조건 처치 수: " + kills(t), t.getGameProfile().getName() + " — kills toward the entry requirement: " + kills(t));
                            return 1;
                        }))))
                .then(Commands.literal("weekly").then(Commands.literal("reset").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    ServerPlayer t = EntityArgument.getPlayer(ctx, "player");
                    RaidData d = RaidData.get(t.server);
                    d.of(t.getUUID()).weeklyWeek = -1;
                    d.touch();
                    ok(ctx, t.getGameProfile().getName() + " 의 이번 주 주간 보상 기록을 지웠습니다.", "Cleared this week's weekly reward record for " + t.getGameProfile().getName() + ".");
                    return 1;
                }))));
    }

    // ------------------------------------------------------------------ 점검용

    public static void resetForTest(MinecraftServer server) {
        reset(server);
        rewarded = false;
    }

    /** 점검용: 쓰러진(관전) 상태로 표시한다. */
    public static void downForTest(ServerPlayer p) {
        keep(p).putInt(NBT_MODE, GameType.SURVIVAL.getId());
    }

    public static UUID bossIdForTest() {
        return bossId;
    }
}

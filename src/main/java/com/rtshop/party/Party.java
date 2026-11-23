package com.rtshop.party;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.rtshop.RtShop;
import com.rtshop.net.PartyPacket;
import com.rtshop.net.ShopNet;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
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

/**
 * 파티 (최대 12명, party.json): 만들기·초대·수락·나가기·내보내기·파티장 넘기기·해산, 파티 채팅, 파티원끼리 PvP 피해 없음,
 * 파티 표시(HUD), 가까운 파티원과 일반 몹 처치 목표 공유. 보스 처치 인정은 BossCredit (각자 체력 10% 이상).
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class Party {
    private Party() {
    }

    public static final class Group {
        public UUID id = UUID.randomUUID();
        public UUID leader;
        public final List<UUID> members = new ArrayList<>();
        /** 이름 기억 (나간 사람·접속 안 한 사람 표시용). */
        public final Map<UUID, String> names = new LinkedHashMap<>();
    }

    /** world/data/rtshop_party.dat */
    public static final class Data extends SavedData {
        public final Map<UUID, Group> groups = new LinkedHashMap<>();

        public static Data get(MinecraftServer server) {
            return server.overworld().getDataStorage().computeIfAbsent(Data::load, Data::new, "rtshop_party");
        }

        public static Data load(CompoundTag tag) {
            Data d = new Data();
            ListTag list = tag.getList("groups", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag c = list.getCompound(i);
                if (!c.hasUUID("id") || !c.hasUUID("leader")) continue;
                Group g = new Group();
                g.id = c.getUUID("id");
                g.leader = c.getUUID("leader");
                ListTag ms = c.getList("members", Tag.TAG_COMPOUND);
                for (int j = 0; j < ms.size(); j++) {
                    CompoundTag m = ms.getCompound(j);
                    if (!m.hasUUID("id")) continue;
                    g.members.add(m.getUUID("id"));
                    g.names.put(m.getUUID("id"), m.getString("name"));
                }
                if (!g.members.isEmpty()) d.groups.put(g.id, g);
            }
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            ListTag list = new ListTag();
            for (Group g : groups.values()) {
                CompoundTag c = new CompoundTag();
                c.putUUID("id", g.id);
                c.putUUID("leader", g.leader);
                ListTag ms = new ListTag();
                for (UUID u : g.members) {
                    CompoundTag m = new CompoundTag();
                    m.putUUID("id", u);
                    m.putString("name", g.names.getOrDefault(u, "?"));
                    ms.add(m);
                }
                c.put("members", ms);
                list.add(c);
            }
            tag.put("groups", list);
            return tag;
        }

        public void touch() {
            setDirty();
        }
    }

    /** 받은 초대: 초대받은 사람 → (파티 ID, 만료 시각). */
    private static final Map<UUID, Object[]> INVITES = new HashMap<>();
    private static final long INVITE_MS = 60_000L;

    // ------------------------------------------------------------------ 조회

    public static Group of(MinecraftServer server, UUID player) {
        if (server == null) return null;
        for (Group g : Data.get(server).groups.values()) {
            if (g.members.contains(player)) return g;
        }
        return null;
    }

    public static boolean together(MinecraftServer server, UUID a, UUID b) {
        if (a.equals(b)) return true;
        Group g = of(server, a);
        return g != null && g.members.contains(b);
    }

    /** 같은 파티의 접속 중인 사람 (자기 포함). */
    public static List<ServerPlayer> online(MinecraftServer server, Group g) {
        List<ServerPlayer> out = new ArrayList<>();
        if (g == null) return out;
        for (UUID u : g.members) {
            ServerPlayer p = server.getPlayerList().getPlayer(u);
            if (p != null) out.add(p);
        }
        return out;
    }

    // ------------------------------------------------------------------ 행동

    private static void say(ServerPlayer p, Component c) {
        com.rtshop.notice.Notice.send(p, Component.literal("[파티] ").withStyle(ChatFormatting.AQUA).append(c));
    }

    private static void say(ServerPlayer p, String s, ChatFormatting color) {
        say(p, Component.literal(s).withStyle(color));
    }

    private static void tellAll(MinecraftServer server, Group g, String s, ChatFormatting color) {
        for (ServerPlayer p : online(server, g)) say(p, s, color);
    }

    public static Group create(ServerPlayer p) {
        Data d = Data.get(p.server);
        if (of(p.server, p.getUUID()) != null) {
            say(p, "이미 파티에 들어 있습니다. 먼저 /party leave 로 나가세요.", ChatFormatting.RED);
            return null;
        }
        Group g = new Group();
        g.leader = p.getUUID();
        g.members.add(p.getUUID());
        g.names.put(p.getUUID(), p.getGameProfile().getName());
        d.groups.put(g.id, g);
        d.touch();
        say(p, "파티를 만들었습니다. /party invite <이름> 으로 초대하세요 (최대 " + PartyConfig.get().maxMembers + "명).", ChatFormatting.GREEN);
        sync(p.server, g);
        return g;
    }

    public static boolean invite(ServerPlayer from, ServerPlayer to) {
        if (from == to) {
            say(from, "자기 자신은 초대할 수 없습니다.", ChatFormatting.RED);
            return false;
        }
        Group g = of(from.server, from.getUUID());
        if (g == null) g = create(from);
        if (g == null) return false;
        if (!g.leader.equals(from.getUUID())) {
            say(from, "파티장만 초대할 수 있습니다.", ChatFormatting.RED);
            return false;
        }
        if (g.members.size() >= PartyConfig.get().maxMembers) {
            say(from, "파티가 가득 찼습니다 (최대 " + PartyConfig.get().maxMembers + "명).", ChatFormatting.RED);
            return false;
        }
        if (of(from.server, to.getUUID()) != null) {
            say(from, to.getGameProfile().getName() + " 님은 이미 다른 파티에 있습니다.", ChatFormatting.RED);
            return false;
        }
        INVITES.put(to.getUUID(), new Object[]{g.id, System.currentTimeMillis() + INVITE_MS});
        say(from, to.getGameProfile().getName() + " 님을 초대했습니다.", ChatFormatting.GREEN);
        MutableComponent msg = Component.literal(from.getGameProfile().getName() + " 님이 파티에 초대했습니다 (60초). ").withStyle(ChatFormatting.WHITE)
                .append(Component.literal("[수락]").withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party accept"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("파티에 들어갑니다")))))
                .append(Component.literal(" "))
                .append(Component.literal("[거절]").withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party decline"))));
        say(to, msg);
        return true;
    }

    public static boolean accept(ServerPlayer p) {
        Object[] inv = INVITES.remove(p.getUUID());
        if (inv == null || System.currentTimeMillis() > (Long) inv[1]) {
            say(p, "받은 파티 초대가 없습니다 (초대는 60초 동안만 유효합니다).", ChatFormatting.RED);
            return false;
        }
        if (of(p.server, p.getUUID()) != null) {
            say(p, "이미 파티에 들어 있습니다.", ChatFormatting.RED);
            return false;
        }
        Data d = Data.get(p.server);
        Group g = d.groups.get((UUID) inv[0]);
        if (g == null) {
            say(p, "그 파티는 이미 해산되었습니다.", ChatFormatting.RED);
            return false;
        }
        if (g.members.size() >= PartyConfig.get().maxMembers) {
            say(p, "파티가 가득 찼습니다.", ChatFormatting.RED);
            return false;
        }
        g.members.add(p.getUUID());
        g.names.put(p.getUUID(), p.getGameProfile().getName());
        d.touch();
        tellAll(p.server, g, p.getGameProfile().getName() + " 님이 파티에 들어왔습니다. (" + g.members.size() + "/" + PartyConfig.get().maxMembers + ")", ChatFormatting.GREEN);
        sync(p.server, g);
        return true;
    }

    public static void decline(ServerPlayer p) {
        if (INVITES.remove(p.getUUID()) != null) say(p, "파티 초대를 거절했습니다.", ChatFormatting.GRAY);
    }

    public static boolean leave(ServerPlayer p) {
        Group g = of(p.server, p.getUUID());
        if (g == null) {
            say(p, "파티에 들어 있지 않습니다.", ChatFormatting.RED);
            return false;
        }
        remove(p.server, g, p.getUUID(), p.getGameProfile().getName() + " 님이 파티를 나갔습니다.");
        say(p, "파티를 나갔습니다.", ChatFormatting.GRAY);
        sendEmpty(p);
        return true;
    }

    private static void remove(MinecraftServer server, Group g, UUID who, String notice) {
        Data d = Data.get(server);
        g.members.remove(who);
        if (g.members.isEmpty()) {
            d.groups.remove(g.id);
        } else {
            if (g.leader.equals(who)) {
                g.leader = g.members.get(0);
                notice += " 새 파티장: " + g.names.getOrDefault(g.leader, "?");
            }
            tellAll(server, g, notice, ChatFormatting.YELLOW);
            sync(server, g);
        }
        d.touch();
    }

    public static boolean kick(ServerPlayer leader, String name) {
        Group g = of(leader.server, leader.getUUID());
        if (g == null || !g.leader.equals(leader.getUUID())) {
            say(leader, "파티장만 내보낼 수 있습니다.", ChatFormatting.RED);
            return false;
        }
        UUID target = null;
        for (Map.Entry<UUID, String> e : g.names.entrySet()) {
            if (g.members.contains(e.getKey()) && e.getValue().equalsIgnoreCase(name)) target = e.getKey();
        }
        if (target == null || target.equals(leader.getUUID())) {
            say(leader, "그런 파티원이 없습니다.", ChatFormatting.RED);
            return false;
        }
        remove(leader.server, g, target, g.names.get(target) + " 님을 파티에서 내보냈습니다.");
        ServerPlayer t = leader.server.getPlayerList().getPlayer(target);
        if (t != null) {
            say(t, "파티에서 내보내졌습니다.", ChatFormatting.RED);
            sendEmpty(t);
        }
        return true;
    }

    public static boolean promote(ServerPlayer leader, String name) {
        Group g = of(leader.server, leader.getUUID());
        if (g == null || !g.leader.equals(leader.getUUID())) {
            say(leader, "파티장만 넘길 수 있습니다.", ChatFormatting.RED);
            return false;
        }
        for (UUID u : g.members) {
            if (g.names.getOrDefault(u, "").equalsIgnoreCase(name) && !u.equals(leader.getUUID())) {
                g.leader = u;
                Data.get(leader.server).touch();
                tellAll(leader.server, g, g.names.get(u) + " 님이 새 파티장이 되었습니다.", ChatFormatting.GOLD);
                sync(leader.server, g);
                return true;
            }
        }
        say(leader, "그런 파티원이 없습니다.", ChatFormatting.RED);
        return false;
    }

    public static void disband(ServerPlayer leader) {
        Group g = of(leader.server, leader.getUUID());
        if (g == null || !g.leader.equals(leader.getUUID())) {
            say(leader, "파티장만 해산할 수 있습니다.", ChatFormatting.RED);
            return;
        }
        for (ServerPlayer p : online(leader.server, g)) {
            say(p, "파티가 해산되었습니다.", ChatFormatting.YELLOW);
            sendEmpty(p);
        }
        Data d = Data.get(leader.server);
        d.groups.remove(g.id);
        d.touch();
    }

    public static void chat(ServerPlayer p, String msg) {
        Group g = of(p.server, p.getUUID());
        if (g == null) {
            say(p, "파티에 들어 있지 않습니다.", ChatFormatting.RED);
            return;
        }
        Component line = Component.literal("[파티] ").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(p.getGameProfile().getName() + ": ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(msg).withStyle(ChatFormatting.AQUA));
        for (ServerPlayer o : online(p.server, g)) o.sendSystemMessage(line);
    }

    public static void info(ServerPlayer p) {
        Group g = of(p.server, p.getUUID());
        if (g == null) {
            say(p, "파티에 들어 있지 않습니다. /party create 로 만들거나 초대를 받으세요.", ChatFormatting.GRAY);
            return;
        }
        say(p, "파티원 " + g.members.size() + "/" + PartyConfig.get().maxMembers, ChatFormatting.AQUA);
        for (UUID u : g.members) {
            boolean on = p.server.getPlayerList().getPlayer(u) != null;
            p.sendSystemMessage(Component.literal("  " + (u.equals(g.leader) ? "★ " : "· ") + g.names.getOrDefault(u, "?") + (on ? "" : " (접속 안 함)"))
                    .withStyle(on ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY));
        }
    }

    // ------------------------------------------------------------------ 파티 표시 (HUD)

    private static void sendEmpty(ServerPlayer p) {
        if (p instanceof FakePlayer || p.connection == null) return;
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new PartyPacket("{}"));
    }

    public static JsonObject view(MinecraftServer server, Group g) {
        JsonObject o = new JsonObject();
        JsonArray arr = new JsonArray();
        for (UUID u : g.members) {
            JsonObject m = new JsonObject();
            ServerPlayer p = server.getPlayerList().getPlayer(u);
            m.addProperty("name", g.names.getOrDefault(u, "?"));
            m.addProperty("id", u.toString());
            m.addProperty("leader", u.equals(g.leader));
            m.addProperty("online", p != null);
            m.addProperty("hp", p == null ? 0 : p.getHealth());
            m.addProperty("max", p == null ? 20 : p.getMaxHealth());
            m.addProperty("dim", p == null ? "" : p.level().dimension().location().toString());
            arr.add(m);
        }
        o.add("members", arr);
        o.addProperty("max", PartyConfig.get().maxMembers);
        return o;
    }

    public static void sync(MinecraftServer server, Group g) {
        String json = view(server, g).toString();
        for (ServerPlayer p : online(server, g)) {
            if (p instanceof FakePlayer || p.connection == null) continue;
            ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new PartyPacket(json));
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer() == null || event.getServer().getTickCount() % 10 != 0) return;
        try {
            for (Group g : new ArrayList<>(Data.get(event.getServer()).groups.values())) sync(event.getServer(), g);
        } catch (Throwable t) {
            RtShop.LOGGER.debug("[rtshop] party sync", t);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Group g = of(p.server, p.getUUID());
            if (g != null) {
                g.names.put(p.getUUID(), p.getGameProfile().getName());
                sync(p.server, g);
            } else {
                sendEmpty(p);
            }
        }
    }

    /** 파티원끼리는 서로 다치게 하지 않는다. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttack(LivingAttackEvent event) {
        if (!PartyConfig.get().noFriendlyFire || !(event.getEntity() instanceof ServerPlayer victim)) return;
        ServerPlayer attacker = com.rtshop.pvp.Pvp.attackerOf(event.getSource(), victim.server);
        if (attacker != null && attacker != victim && together(victim.server, attacker.getUUID(), victim.getUUID())) {
            event.setCanceled(true);
        }
    }

    // ------------------------------------------------------------------ 명령어

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("party")
                .executes(c -> {
                    info(c.getSource().getPlayerOrException());
                    return 1;
                })
                .then(Commands.literal("create").executes(c -> create(c.getSource().getPlayerOrException()) == null ? 0 : 1))
                .then(Commands.literal("invite").then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> invite(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "player")) ? 1 : 0)))
                .then(Commands.literal("accept").executes(c -> accept(c.getSource().getPlayerOrException()) ? 1 : 0))
                .then(Commands.literal("decline").executes(c -> {
                    decline(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("leave").executes(c -> leave(c.getSource().getPlayerOrException()) ? 1 : 0))
                .then(Commands.literal("kick").then(Commands.argument("name", StringArgumentType.word())
                        .executes(c -> kick(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "name")) ? 1 : 0)))
                .then(Commands.literal("leader").then(Commands.argument("name", StringArgumentType.word())
                        .executes(c -> promote(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "name")) ? 1 : 0)))
                .then(Commands.literal("disband").executes(c -> {
                    disband(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("chat").then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(c -> {
                            chat(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "message"));
                            return 1;
                        }))));
        d.register(Commands.literal("pc").then(Commands.argument("message", StringArgumentType.greedyString())
                .executes(c -> {
                    chat(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "message"));
                    return 1;
                })));
    }

    // ------------------------------------------------------------------ 점검용

    public static Group createForTest(ServerPlayer leader, ServerPlayer... others) {
        Data d = Data.get(leader.server);
        Group old;
        while ((old = of(leader.server, leader.getUUID())) != null) d.groups.remove(old.id);
        for (ServerPlayer o : others) while ((old = of(leader.server, o.getUUID())) != null) d.groups.remove(old.id);
        Group g = new Group();
        g.leader = leader.getUUID();
        g.members.add(leader.getUUID());
        g.names.put(leader.getUUID(), leader.getGameProfile().getName());
        for (ServerPlayer o : others) {
            g.members.add(o.getUUID());
            g.names.put(o.getUUID(), o.getGameProfile().getName());
        }
        d.groups.put(g.id, g);
        return g;
    }

    public static void inviteForTest(ServerPlayer to, Group g) {
        INVITES.put(to.getUUID(), new Object[]{g.id, System.currentTimeMillis() + INVITE_MS});
    }

    public static void clearForTest(MinecraftServer server) {
        Data.get(server).groups.clear();
        INVITES.clear();
    }
}

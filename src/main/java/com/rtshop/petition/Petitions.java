package com.rtshop.petition;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.rtshop.RtShop;
import com.rtshop.story.StoryText;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 민원 (3.60): 플레이어가 운영자에게 글을 남기고, 운영자가 답하고 종결한다.
 * <ul>
 * <li>플레이어: /rtshop report write &lt;글&gt; · /rtshop report list · /rtshop report cancel &lt;번호&gt;</li>
 * <li>운영자: /rtshop report all [done] · answer &lt;번호&gt; &lt;글&gt; · close &lt;번호&gt; · delete &lt;번호&gt; · tp &lt;번호&gt;</li>
 * </ul>
 * 접수한 자리(차원·좌표)가 함께 적힌다. 새 민원은 접속 중인 운영자에게, 답변·종결은 그 플레이어에게 알린다 (없으면 다음 접속 때).
 * 저장: 월드 데이터 rtshop_petitions. 운영자 명령은 다른 운영자 기능처럼 크리에이티브·관전에서만 된다 (util/Perm).
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class Petitions {
    private Petitions() {
    }

    /** 글의 최대 길이, 한 사람이 열어 둘 수 있는 민원 수, 다음 접수까지의 간격(초), 남겨 두는 종결 민원 수. */
    public static final int MAX_LEN = 300, MAX_OPEN = 5, COOLDOWN_SEC = 30, KEEP_CLOSED = 300;
    public static final int OPEN = 0, ANSWERED = 1, CLOSED = 2;
    /** file 의 실패 값. */
    public static final int ERR_EMPTY = -1, ERR_LONG = -2, ERR_MANY = -3, ERR_SOON = -4;

    private static final Map<UUID, Long> LAST = new HashMap<>();
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.systemDefault());

    public static final class Entry {
        public int id, state, x, y, z;
        public UUID player;
        public String name = "", text = "", dim = "", answer = "", answeredBy = "";
        public long time, answeredAt;
        /** 답변·종결을 그 플레이어가 보았는가 (접속해 있지 않을 때 달린 답은 다음 접속 때 알린다). */
        public boolean seen = true;

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putInt("id", id);
            t.putInt("state", state);
            t.putInt("x", x);
            t.putInt("y", y);
            t.putInt("z", z);
            t.putUUID("player", player);
            t.putString("name", name);
            t.putString("text", text);
            t.putString("dim", dim);
            t.putString("answer", answer);
            t.putString("answeredBy", answeredBy);
            t.putLong("time", time);
            t.putLong("answeredAt", answeredAt);
            t.putBoolean("seen", seen);
            return t;
        }

        static Entry load(CompoundTag t) {
            Entry e = new Entry();
            e.id = t.getInt("id");
            e.state = t.getInt("state");
            e.x = t.getInt("x");
            e.y = t.getInt("y");
            e.z = t.getInt("z");
            e.player = t.getUUID("player");
            e.name = t.getString("name");
            e.text = t.getString("text");
            e.dim = t.getString("dim");
            e.answer = t.getString("answer");
            e.answeredBy = t.getString("answeredBy");
            e.time = t.getLong("time");
            e.answeredAt = t.getLong("answeredAt");
            e.seen = t.getBoolean("seen");
            return e;
        }
    }

    public static final class Data extends SavedData {
        public final List<Entry> list = new ArrayList<>();
        int next = 1;

        public static Data load(CompoundTag t) {
            Data d = new Data();
            d.next = Math.max(1, t.getInt("next"));
            ListTag l = t.getList("list", Tag.TAG_COMPOUND);
            for (int i = 0; i < l.size(); i++) {
                try {
                    d.list.add(Entry.load(l.getCompound(i)));
                } catch (Exception ex) {
                    RtShop.LOGGER.warn("[rtshop] 민원 한 건을 읽지 못했습니다: {}", ex.toString());
                }
            }
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag t) {
            t.putInt("next", next);
            ListTag l = new ListTag();
            for (Entry e : list) l.add(e.save());
            t.put("list", l);
            return t;
        }
    }

    public static Data data(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(Data::load, Data::new, "rtshop_petitions");
    }

    public static Entry find(MinecraftServer server, int id) {
        for (Entry e : data(server).list) if (e.id == id) return e;
        return null;
    }

    public static int openCount(MinecraftServer server) {
        int n = 0;
        for (Entry e : data(server).list) if (e.state == OPEN) n++;
        return n;
    }

    /** 글을 다듬는다: 앞뒤 공백을 떼고 줄바꿈·겹친 공백을 한 칸으로. */
    public static String clean(String s) {
        return s == null ? "" : s.replaceAll("\\s+", " ").trim();
    }

    // ------------------------------------------------------------------ 하는 일

    /** 민원을 접수한다. 번호(1 이상)를 돌려주고, 안 되면 ERR_* (0 보다 작다). */
    public static int file(ServerPlayer p, String raw) {
        String text = clean(raw);
        if (text.isEmpty()) return ERR_EMPTY;
        if (text.length() > MAX_LEN) return ERR_LONG;
        Data d = data(p.server);
        int mine = 0;
        for (Entry e : d.list) if (e.state == OPEN && p.getUUID().equals(e.player)) mine++;
        if (mine >= MAX_OPEN) return ERR_MANY;
        long now = System.currentTimeMillis();
        Long last = LAST.get(p.getUUID());
        if (last != null && now - last < COOLDOWN_SEC * 1000L) return ERR_SOON;
        LAST.put(p.getUUID(), now);
        Entry e = new Entry();
        e.id = d.next++;
        e.player = p.getUUID();
        e.name = p.getGameProfile().getName();
        e.text = text;
        e.time = now;
        e.dim = p.level().dimension().location().toString();
        e.x = p.blockPosition().getX();
        e.y = p.blockPosition().getY();
        e.z = p.blockPosition().getZ();
        d.list.add(e);
        d.setDirty();
        for (ServerPlayer op : p.server.getPlayerList().getPlayers()) {
            if (op == p || !op.hasPermissions(2)) continue;
            op.sendSystemMessage(Component.literal(StoryText.pick(op, "[민원] 새 민원 #" + e.id + " (" + e.name + "): ", "[Report] New report #" + e.id + " (" + e.name + "): ") + e.text)
                    .withStyle(ChatFormatting.GOLD).append(button(op, "목록", "list", "/rtshop report all", true)));
        }
        return e.id;
    }

    /** 운영자가 답한다 (상태: 답변함). 그 플레이어가 접속해 있으면 바로 알린다. */
    public static boolean answer(MinecraftServer server, int id, String by, String raw) {
        Entry e = find(server, id);
        String text = clean(raw);
        if (e == null || text.isEmpty()) return false;
        e.answer = text.length() > MAX_LEN ? text.substring(0, MAX_LEN) : text;
        e.answeredBy = by == null ? "" : by;
        e.answeredAt = System.currentTimeMillis();
        if (e.state == OPEN) e.state = ANSWERED;
        tell(server, e);
        data(server).setDirty();
        return true;
    }

    /** 종결한다 (답 없이도 된다). */
    public static boolean close(MinecraftServer server, int id) {
        Entry e = find(server, id);
        if (e == null || e.state == CLOSED) return false;
        e.state = CLOSED;
        tell(server, e);
        Data d = data(server);
        int closed = 0;
        for (Entry x : d.list) if (x.state == CLOSED) closed++;
        for (int i = 0; closed > KEEP_CLOSED && i < d.list.size(); ) {
            if (d.list.get(i).state == CLOSED && d.list.get(i) != e) {
                d.list.remove(i);
                closed--;
            } else {
                i++;
            }
        }
        d.setDirty();
        return true;
    }

    /** 낸 사람이 스스로 거둔다 (아직 답이 없는 것만). */
    public static boolean cancel(ServerPlayer p, int id) {
        Entry e = find(p.server, id);
        if (e == null || !p.getUUID().equals(e.player) || e.state != OPEN) return false;
        data(p.server).list.remove(e);
        data(p.server).setDirty();
        return true;
    }

    public static boolean delete(MinecraftServer server, int id) {
        Entry e = find(server, id);
        if (e == null) return false;
        data(server).list.remove(e);
        data(server).setDirty();
        return true;
    }

    /** 답변·종결을 그 플레이어에게 알린다. 접속해 있지 않으면 다음 접속 때 (seen = false). */
    private static void tell(MinecraftServer server, Entry e) {
        ServerPlayer p = server.getPlayerList().getPlayer(e.player);
        if (p == null) {
            e.seen = false;
            return;
        }
        e.seen = true;
        p.sendSystemMessage(line(p, e, false));
    }

    public static void forTestReset(MinecraftServer server) {
        LAST.clear();
        Data d = data(server);
        d.list.clear();
        d.setDirty();
    }

    public static void forTestNoCooldown() {
        LAST.clear();
    }

    // ------------------------------------------------------------------ 보이는 글

    private static String stateName(boolean en, int state) {
        return state == OPEN ? (en ? "open" : "접수됨") : state == ANSWERED ? (en ? "answered" : "답변함") : (en ? "closed" : "종결");
    }

    private static MutableComponent button(ServerPlayer to, String ko, String en, String command, boolean run) {
        return Component.literal(" [" + StoryText.pick(to, ko, en) + "]").withStyle(s -> s.withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent(run ? ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(command))));
    }

    /** 민원 한 건을 한두 줄로: "#3 [답변함] 11-26 17:51 이름 (자리): 글" + 답변. to 가 없으면(콘솔) 한국어, 단추 없음. */
    public static MutableComponent line(ServerPlayer to, Entry e, boolean forOp) {
        boolean en = to != null && StoryText.en(to);
        ChatFormatting color = e.state == OPEN ? ChatFormatting.YELLOW : e.state == ANSWERED ? ChatFormatting.GREEN : ChatFormatting.GRAY;
        String head = (en ? "[Report] #" : "[민원] #") + e.id + " [" + stateName(en, e.state) + "] " + WHEN.format(Instant.ofEpochMilli(e.time));
        if (forOp) head += " " + e.name + " (" + e.dim.replace("minecraft:", "") + " " + e.x + ", " + e.y + ", " + e.z + ")";
        MutableComponent out = Component.literal(head + ": ").withStyle(color).append(Component.literal(e.text).withStyle(ChatFormatting.WHITE));
        if (!e.answer.isEmpty()) {
            out.append(Component.literal("\n  └ " + (en ? "Answer" : "답변") + (e.answeredBy.isEmpty() ? "" : " (" + e.answeredBy + ")") + ": ").withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(e.answer).withStyle(ChatFormatting.WHITE));
        }
        if (to != null && forOp) {
            if (e.state != CLOSED) {
                out.append(button(to, "답변", "answer", "/rtshop report answer " + e.id + " ", false));
                out.append(button(to, "종결", "close", "/rtshop report close " + e.id, true));
            }
            out.append(button(to, "이동", "go", "/rtshop report tp " + e.id, true));
        } else if (to != null && e.state == OPEN) {
            out.append(button(to, "거두기", "withdraw", "/rtshop report cancel " + e.id, false));
        }
        return out;
    }

    // ------------------------------------------------------------------ 접속할 때

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        try {
            if (!(event.getEntity() instanceof ServerPlayer p)) return;
            Data d = data(p.server);
            boolean dirty = false;
            for (Entry e : d.list) {
                if (!e.seen && p.getUUID().equals(e.player)) {
                    p.sendSystemMessage(line(p, e, false));
                    e.seen = true;
                    dirty = true;
                }
            }
            if (dirty) d.setDirty();
            int open = openCount(p.server);
            if (open > 0 && p.hasPermissions(2)) {
                p.sendSystemMessage(Component.literal(StoryText.pick(p, "[민원] 처리를 기다리는 민원이 " + open + "건 있습니다.", "[Report] " + open + " report(s) are waiting for an answer."))
                        .withStyle(ChatFormatting.GOLD).append(button(p, "목록", "list", "/rtshop report all", true)));
            }
        } catch (Throwable t) {
            RtShop.LOGGER.warn("[rtshop] 민원 알림 실패: {}", t.toString());
        }
    }

    // ------------------------------------------------------------------ 명령

    private static void say(CommandContext<CommandSourceStack> ctx, String ko, String en) {
        ctx.getSource().sendSuccess(() -> Component.literal(StoryText.pick(ctx.getSource(), ko, en)), false);
    }

    private static int fail(CommandContext<CommandSourceStack> ctx, String ko, String en) {
        ctx.getSource().sendFailure(Component.literal(StoryText.pick(ctx.getSource(), ko, en)));
        return 0;
    }

    private static int help(CommandContext<CommandSourceStack> ctx) {
        say(ctx, "[민원] 운영자에게 글을 남깁니다 (버그 · 건의 · 토지나 거래 문제 등). 지금 서 있는 자리가 함께 적힙니다.\n"
                        + "  /rtshop report write <내용>  : 접수 (" + MAX_LEN + "자까지, 처리 전 민원은 " + MAX_OPEN + "건까지)\n"
                        + "  /rtshop report list  : 내 민원과 답변 보기\n"
                        + "  /rtshop report cancel <번호>  : 아직 답이 없는 내 민원 거두기",
                "[Report] Leave a note for the operators (bugs, suggestions, land or trade trouble). Where you stand is recorded with it.\n"
                        + "  /rtshop report write <text>  : file one (up to " + MAX_LEN + " characters, " + MAX_OPEN + " open at a time)\n"
                        + "  /rtshop report list  : your reports and their answers\n"
                        + "  /rtshop report cancel <number>  : withdraw one of yours that has no answer yet");
        if (ctx.getSource().hasPermission(2)) {
            say(ctx, "  (운영자) /rtshop report all [done] · answer <번호> <글> · close <번호> · delete <번호> · tp <번호>",
                    "  (operators) /rtshop report all [done] · answer <number> <text> · close <number> · delete <number> · tp <number>");
        }
        return 1;
    }

    private static int write(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        int r = file(p, StringArgumentType.getString(ctx, "text"));
        if (r == ERR_EMPTY) return fail(ctx, "내용이 비어 있습니다.", "The report is empty.");
        if (r == ERR_LONG) return fail(ctx, "글이 너무 깁니다 (" + MAX_LEN + "자까지). 나눠서 접수해 주세요.", "That is too long (up to " + MAX_LEN + " characters). Please split it up.");
        if (r == ERR_MANY) return fail(ctx, "처리를 기다리는 민원이 이미 " + MAX_OPEN + "건 있습니다. 답을 받거나 /rtshop report cancel 로 거둔 뒤에 접수해 주세요.",
                "You already have " + MAX_OPEN + " open reports. Wait for an answer or withdraw one with /rtshop report cancel.");
        if (r == ERR_SOON) return fail(ctx, "방금 접수했습니다. " + COOLDOWN_SEC + "초 뒤에 다시 해 주세요.", "You just filed one. Try again in " + COOLDOWN_SEC + " seconds.");
        say(ctx, "[민원] #" + r + " 접수했습니다. 답이 달리면 알려 드립니다 (/rtshop report list 로도 볼 수 있습니다).",
                "[Report] #" + r + " filed. You will be told when it is answered (see also /rtshop report list).");
        return 1;
    }

    private static int mine(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        int n = 0;
        for (Entry e : data(p.server).list) {
            if (!p.getUUID().equals(e.player)) continue;
            Entry shown = e;
            ctx.getSource().sendSuccess(() -> line(p, shown, false), false);
            n++;
        }
        if (n == 0) say(ctx, "[민원] 접수한 민원이 없습니다. /rtshop report write <내용> 으로 접수합니다.", "[Report] You have no reports. File one with /rtshop report write <text>.");
        return n;
    }

    private static int all(CommandContext<CommandSourceStack> ctx, boolean done) {
        ServerPlayer to = ctx.getSource().getEntity() instanceof ServerPlayer sp ? sp : null;
        int n = 0;
        for (Entry e : data(ctx.getSource().getServer()).list) {
            if ((e.state == CLOSED) != done) continue;
            Entry shown = e;
            ctx.getSource().sendSuccess(() -> line(to, shown, true), false);
            n++;
        }
        if (n == 0) {
            say(ctx, done ? "[민원] 종결한 민원이 없습니다." : "[민원] 처리를 기다리는 민원이 없습니다. (종결한 것: /rtshop report all done)",
                    done ? "[Report] No closed reports." : "[Report] Nothing is waiting. (Closed ones: /rtshop report all done)");
        }
        return n;
    }

    private static int tp(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        Entry e = find(p.server, IntegerArgumentType.getInteger(ctx, "id"));
        if (e == null) return fail(ctx, "그 번호의 민원이 없습니다.", "There is no report with that number.");
        ResourceLocation dim = ResourceLocation.tryParse(e.dim);
        ServerLevel level = dim == null ? null : p.server.getLevel(ResourceKey.create(Registries.DIMENSION, dim));
        if (level == null) return fail(ctx, "그 민원이 접수된 차원을 찾을 수 없습니다: " + e.dim, "The dimension that report was filed in cannot be found: " + e.dim);
        p.teleportTo(level, e.x + 0.5, e.y, e.z + 0.5, p.getYRot(), p.getXRot());
        return 1;
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> report = Commands.literal("report")
                .executes(Petitions::help)
                .then(Commands.literal("write").then(Commands.argument("text", StringArgumentType.greedyString()).executes(Petitions::write)))
                .then(Commands.literal("list").executes(Petitions::mine))
                .then(Commands.literal("cancel").then(Commands.argument("id", IntegerArgumentType.integer(1)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    int id = IntegerArgumentType.getInteger(ctx, "id");
                    if (!cancel(p, id)) return fail(ctx, "거둘 수 없습니다: 내 민원이 아니거나 이미 답이 달렸습니다.", "That cannot be withdrawn: it is not yours, or it already has an answer.");
                    say(ctx, "[민원] #" + id + " 을(를) 거뒀습니다.", "[Report] #" + id + " withdrawn.");
                    return 1;
                })))
                .then(Commands.literal("all").requires(s -> s.hasPermission(2)).executes(ctx -> all(ctx, false))
                        .then(Commands.literal("done").executes(ctx -> all(ctx, true))))
                .then(Commands.literal("answer").requires(s -> s.hasPermission(2)).then(Commands.argument("id", IntegerArgumentType.integer(1))
                        .then(Commands.argument("text", StringArgumentType.greedyString()).executes(ctx -> {
                            int id = IntegerArgumentType.getInteger(ctx, "id");
                            if (!answer(ctx.getSource().getServer(), id, ctx.getSource().getTextName(), StringArgumentType.getString(ctx, "text")))
                                return fail(ctx, "그 번호의 민원이 없거나 답이 비어 있습니다.", "There is no report with that number, or the answer is empty.");
                            say(ctx, "[민원] #" + id + " 에 답했습니다. (끝났으면 /rtshop report close " + id + ")", "[Report] Answered #" + id + ". (When it is settled: /rtshop report close " + id + ")");
                            return 1;
                        }))))
                .then(Commands.literal("close").requires(s -> s.hasPermission(2)).then(Commands.argument("id", IntegerArgumentType.integer(1)).executes(ctx -> {
                    int id = IntegerArgumentType.getInteger(ctx, "id");
                    if (!close(ctx.getSource().getServer(), id)) return fail(ctx, "그 번호의 민원이 없거나 이미 종결했습니다.", "There is no report with that number, or it is already closed.");
                    say(ctx, "[민원] #" + id + " 을(를) 종결했습니다.", "[Report] #" + id + " closed.");
                    return 1;
                })))
                .then(Commands.literal("delete").requires(s -> s.hasPermission(2)).then(Commands.argument("id", IntegerArgumentType.integer(1)).executes(ctx -> {
                    int id = IntegerArgumentType.getInteger(ctx, "id");
                    if (!delete(ctx.getSource().getServer(), id)) return fail(ctx, "그 번호의 민원이 없습니다.", "There is no report with that number.");
                    say(ctx, "[민원] #" + id + " 을(를) 지웠습니다.", "[Report] #" + id + " deleted.");
                    return 1;
                })))
                .then(Commands.literal("tp").requires(s -> s.hasPermission(2)).then(Commands.argument("id", IntegerArgumentType.integer(1)).executes(Petitions::tp)));
        event.getDispatcher().register(Commands.literal("rtshop").then(report));
    }
}

package com.rtshop.land;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.rtshop.RtShop;
import com.rtshop.item.ModItems;
import com.rtshop.land.LandData.Claim;
import com.rtshop.land.LandData.Land;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/** /rtshop plot ... 명령어. buy·info 는 누구나, 나머지는 OP. */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class PlotCommands {
    private PlotCommands() {
    }

    private static final SuggestionProvider<CommandSourceStack> PLOT_IDS = (ctx, b) -> {
        List<String> ids = new ArrayList<>();
        for (Land l : LandData.get(ctx.getSource().getServer()).lands()) {
            if (l.plot) {
                ids.add(String.valueOf(l.id));
            }
        }
        return SharedSuggestionProvider.suggest(ids, b);
    };

    private static final SuggestionProvider<CommandSourceStack> MODES = (ctx, b) ->
            SharedSuggestionProvider.suggest(List.of(PlotManager.MODE_KEYS), b);

    private static Land plot(CommandContext<CommandSourceStack> ctx) {
        Land l = LandData.get(ctx.getSource().getServer()).land(IntegerArgumentType.getInteger(ctx, "id"));
        if (l == null || !l.plot) {
            ctx.getSource().sendFailure(Component.translatable("rtshop.plot.unknown", IntegerArgumentType.getInteger(ctx, "id")));
            return null;
        }
        return l;
    }

    private static int create(CommandContext<CommandSourceStack> ctx, long price, String name) throws CommandSyntaxException {
        return PlotManager.create(ctx.getSource().getPlayerOrException(), price, name) != null ? 1 : 0;
    }

    /** 3.84: 바라보는 블록(6칸 안)을 "누구나 쓸 수 있음"으로 켜고 끈다. */
    static int useToggle(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        net.minecraft.world.phys.HitResult hit = p.pick(6.0D, 0F, false);
        if (!(hit instanceof net.minecraft.world.phys.BlockHitResult bh) || hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) {
            ctx.getSource().sendFailure(Component.translatable("rtshop.plot.use_none"));
            return 0;
        }
        net.minecraft.core.BlockPos pos = bh.getBlockPos();
        String dim = p.level().dimension().location().toString();
        boolean on = LandData.get(ctx.getSource().getServer()).toggleUsable(dim, pos);
        Component name = p.level().getBlockState(pos).getBlock().getName();
        String at = pos.getX() + " " + pos.getY() + " " + pos.getZ();
        ctx.getSource().sendSuccess(() -> Component.translatable(on ? "rtshop.plot.use_on" : "rtshop.plot.use_off", name, at)
                .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.YELLOW), true);
        return 1;
    }

    static int useList(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        var all = LandData.get(ctx.getSource().getServer()).usableAll();
        int n = 0;
        StringBuilder b = new StringBuilder();
        for (var e : all.entrySet()) {
            for (long v : e.getValue()) {
                n++;
                if (n <= 20) {
                    net.minecraft.core.BlockPos q = net.minecraft.core.BlockPos.of(v);
                    if (b.length() > 0) b.append(", ");
                    b.append(q.getX()).append(' ').append(q.getY()).append(' ').append(q.getZ());
                    if (!e.getKey().equals("minecraft:overworld")) b.append(" (").append(e.getKey()).append(')');
                }
            }
        }
        int total = n;
        String list = b.toString() + (n > 20 ? " …" : "");
        ctx.getSource().sendSuccess(() -> Component.translatable("rtshop.plot.use_list", total, list), false);
        return 1;
    }

    private static int castleInstall(CommandContext<CommandSourceStack> ctx, boolean force) {
        var server = ctx.getSource().getServer();
        if (!force && !CastlePlots.worldMatches(server)) {
            ctx.getSource().sendFailure(Component.translatable("rtshop.castle.not_amonos",
                    CastlePlots.signatureMatches(server) + "/" + CastlePlots.def().signature.size()));
            return 0;
        }
        int[] r = CastlePlots.install(server);
        ctx.getSource().sendSuccess(() -> Component.translatable("rtshop.castle.installed", r[0], r[1], r[2]), true);
        return 1;
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("rtshop").then(Commands.literal("plot")
                // ---- 누구나
                .then(Commands.literal("browse").requires(src -> src.hasPermission(2)).executes(ctx -> {
                    PlotBrowser.handle(ctx.getSource().getPlayerOrException(), PlotBrowser.OPEN, 0);
                    return 1;
                }))
                // ---- 성 안 토지 (OP)
                .then(Commands.literal("castle").requires(src -> src.hasPermission(2))
                        .then(Commands.literal("status").executes(ctx -> {
                            var server = ctx.getSource().getServer();
                            LandData data = LandData.get(server);
                            int n = 0, sold = 0;
                            for (Land l : data.lands()) {
                                if (l.castle()) {
                                    n++;
                                    if (!l.forSale()) sold++;
                                }
                            }
                            String sig = CastlePlots.signatureMatches(server) + "/" + CastlePlots.def().signature.size();
                            Component msg = Component.translatable("rtshop.castle.status", data.castleInstalled, n, sold,
                                    CastlePlots.def().tiles.size(), sig);
                            ctx.getSource().sendSuccess(() -> msg, false);
                            return 1;
                        }))
                        .then(Commands.literal("install").executes(ctx -> castleInstall(ctx, false))
                                .then(Commands.literal("force").executes(ctx -> castleInstall(ctx, true))))
                        .then(Commands.literal("remove").executes(ctx -> {
                            int[] r = CastlePlots.uninstall(ctx.getSource().getServer());
                            ctx.getSource().sendSuccess(() -> Component.translatable("rtshop.castle.removed", r[0], r[1]), true);
                            return 1;
                        }))
                        .then(Commands.literal("restore").then(Commands.argument("no", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    int no = IntegerArgumentType.getInteger(ctx, "no");
                                    Land l = CastlePlots.landOf(LandData.get(ctx.getSource().getServer()), no);
                                    boolean ok = false;
                                    try {
                                        ok = l != null && CastlePlots.restore(ctx.getSource().getServer(), l);
                                    } catch (Exception e) {
                                        RtShop.LOGGER.warn("[rtshop] castle restore failed", e);
                                    }
                                    if (ok) {
                                        ctx.getSource().sendSuccess(() -> Component.translatable("rtshop.castle.restored", l.name), true);
                                    } else {
                                        ctx.getSource().sendFailure(Component.translatable("rtshop.castle.no_backup", no));
                                    }
                                    return ok ? 1 : 0;
                                }))))
                // ---- 3.84: 누구나 쓸 수 있게 고른 블록 (OP) — 바라보는 블록을 켜고 끈다
                .then(Commands.literal("use").requires(src -> src.hasPermission(2))
                        .executes(PlotCommands::useToggle)
                        .then(Commands.literal("list").executes(PlotCommands::useList))
                        .then(Commands.literal("clear").executes(ctx -> {
                            int n = LandData.get(ctx.getSource().getServer()).clearUsable();
                            ctx.getSource().sendSuccess(() -> Component.translatable("rtshop.plot.use_cleared", n), true);
                            return 1;
                        })))
                .then(Commands.literal("info").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Land l = PlotManager.plotAt(p);
                    if (l == null) {
                        com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.plot.not_in_plot").withStyle(ChatFormatting.GRAY));
                        return 0;
                    }
                    Claim c = PlotManager.plotClaim(l);
                    com.rtshop.notice.Notice.send(p, Component.translatable(l.forSale() ? "rtshop.plot.info_sale" : "rtshop.plot.info_owned",
                            l.name, c == null ? 0 : c.area(), PlotManager.money(l.price), l.ownerName).withStyle(ChatFormatting.GOLD));
                    LandManager.SHOW_BORDER.put(p.getUUID(), new long[]{l.id, p.level().getGameTime() + 300});
                    return 1;
                }))
                // ---- OP: 고르기
                .then(Commands.literal("wand").requires(s -> s.hasPermission(2)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    p.getInventory().add(new ItemStack(ModItems.PLOT_WAND.get()));
                    com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.plot.wand_given").withStyle(ChatFormatting.GREEN));
                    PlotManager.showMode(p, PlotManager.selection(p));
                    return 1;
                }))
                .then(Commands.literal("mode").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("mode", StringArgumentType.word()).suggests(MODES).executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            String m = StringArgumentType.getString(ctx, "mode");
                            for (int i = 0; i < PlotManager.MODE_KEYS.length; i++) {
                                if (PlotManager.MODE_KEYS[i].equals(m)) {
                                    PlotManager.Selection s = PlotManager.selection(p);
                                    s.mode = i;
                                    PlotManager.showMode(p, s);
                                    return 1;
                                }
                            }
                            return 0;
                        })))
                .then(Commands.literal("brush").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(0, 8)).executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            PlotManager.Selection s = PlotManager.selection(p);
                            s.brush = IntegerArgumentType.getInteger(ctx, "radius");
                            if (s.mode != PlotManager.BRUSH_REMOVE) {
                                s.mode = PlotManager.BRUSH_ADD;
                            }
                            PlotManager.showMode(p, s);
                            return 1;
                        })))
                .then(Commands.literal("undo").requires(s -> s.hasPermission(2)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    boolean ok = PlotManager.undo(p);
                    com.rtshop.notice.Notice.send(p, Component.translatable(ok ? "rtshop.plot.undone" : "rtshop.plot.nothing_to_undo",
                            PlotManager.selection(p).cells.size()));
                    return ok ? 1 : 0;
                }))
                .then(Commands.literal("clear").requires(s -> s.hasPermission(2)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    PlotManager.Selection s = PlotManager.selection(p);
                    s.snapshot();
                    s.cells.clear();
                    s.polygon.clear();
                    s.editing = -1;
                    s.bounds();
                    com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.plot.cleared"));
                    return 1;
                }))
                .then(Commands.literal("polygon").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("close").executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            return PlotManager.closePolygon(p, PlotManager.selection(p)) ? 1 : 0;
                        }))
                        .then(Commands.literal("clear").executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            PlotManager.selection(p).polygon.clear();
                            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.plot.poly_cleared"));
                            return 1;
                        })))
                // ---- OP: 등록·관리
                .then(Commands.literal("create").requires(s -> s.hasPermission(2))
                        .executes(ctx -> create(ctx, -1, ""))
                        .then(Commands.literal("auto")
                                .executes(ctx -> create(ctx, -1, ""))
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(ctx -> create(ctx, -1, StringArgumentType.getString(ctx, "name")))))
                        .then(Commands.argument("price", LongArgumentType.longArg(0))
                                .executes(ctx -> create(ctx, LongArgumentType.getLong(ctx, "price"), ""))
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(ctx -> create(ctx, LongArgumentType.getLong(ctx, "price"),
                                                StringArgumentType.getString(ctx, "name"))))))
                .then(Commands.literal("list").requires(s -> s.hasPermission(2)).executes(ctx -> {
                    LandData data = LandData.get(ctx.getSource().getServer());
                    int n = 0;
                    for (Land l : data.lands()) {
                        if (!l.plot) {
                            continue;
                        }
                        n++;
                        Claim c = PlotManager.plotClaim(l);
                        MutableComponent line = Component.translatable(!l.group.isEmpty()
                                        ? (l.forSale() ? "rtshop.plot.list_starter_free" : "rtshop.plot.list_starter_owned")
                                        : l.forSale() ? "rtshop.plot.list_sale" : "rtshop.plot.list_owned",
                                l.id, l.name, c == null ? 0 : c.area(), PlotManager.money(l.price), l.ownerName);
                        line.withStyle(Style.EMPTY.withColor(l.forSale() ? ChatFormatting.WHITE : ChatFormatting.AQUA)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rtshop plot tp " + l.id)));
                        ctx.getSource().sendSystemMessage(line);
                    }
                    if (n == 0) {
                        ctx.getSource().sendSystemMessage(Component.translatable("rtshop.plot.list_empty"));
                    }
                    return n;
                }))
                .then(Commands.literal("tp").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("id", IntegerArgumentType.integer(1)).suggests(PLOT_IDS).executes(ctx -> {
                            Land l = plot(ctx);
                            Claim c = PlotManager.plotClaim(l);
                            if (c == null) {
                                return 0;
                            }
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            ServerLevel level = null;
                            for (ServerLevel lv : p.server.getAllLevels()) {
                                if (LandManager.dim(lv).equals(c.dim)) {
                                    level = lv;
                                }
                            }
                            if (level == null) {
                                return 0;
                            }
                            // 구획 가운데에 가장 가까운 칸으로
                            int cx = (c.x0 + c.x1) / 2, cz = (c.z0 + c.z1) / 2;
                            int bx = cx, bz = cz;
                            long best = Long.MAX_VALUE;
                            for (int z = c.z0; z <= c.z1; z++) {
                                for (int x = c.x0; x <= c.x1; x++) {
                                    if (c.hasColumn(x, z)) {
                                        long dd = (long) (x - cx) * (x - cx) + (long) (z - cz) * (z - cz);
                                        if (dd < best) {
                                            best = dd;
                                            bx = x;
                                            bz = z;
                                        }
                                    }
                                }
                            }
                            level.getChunk(bx >> 4, bz >> 4);
                            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
                            p.teleportTo(level, bx + 0.5, y + 0.1, bz + 0.5, p.getYRot(), p.getXRot());
                            return 1;
                        })))
                .then(Commands.literal("edit").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("id", IntegerArgumentType.integer(1)).suggests(PLOT_IDS).executes(ctx -> {
                            Land l = plot(ctx);
                            return l != null && PlotManager.beginEdit(ctx.getSource().getPlayerOrException(), l) ? 1 : 0;
                        })))
                .then(Commands.literal("save").requires(s -> s.hasPermission(2))
                        .executes(ctx -> PlotManager.saveEdit(ctx.getSource().getPlayerOrException()) ? 1 : 0))
                .then(Commands.literal("delete").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("id", IntegerArgumentType.integer(1)).suggests(PLOT_IDS).executes(ctx -> {
                            Land l = plot(ctx);
                            if (l == null) {
                                return 0;
                            }
                            LandData.get(ctx.getSource().getServer()).removeLand(l);
                            ctx.getSource().sendSystemMessage(Component.translatable("rtshop.plot.deleted", l.id, l.name));
                            return 1;
                        })))
                .then(Commands.literal("price").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("id", IntegerArgumentType.integer(1)).suggests(PLOT_IDS)
                                .then(Commands.argument("price", LongArgumentType.longArg(0)).executes(ctx -> {
                                    Land l = plot(ctx);
                                    if (l == null) {
                                        return 0;
                                    }
                                    l.price = LongArgumentType.getLong(ctx, "price");
                                    LandData.get(ctx.getSource().getServer()).touch();
                                    ctx.getSource().sendSystemMessage(Component.translatable("rtshop.plot.price_set", l.id, l.name,
                                            PlotManager.money(l.price)));
                                    return 1;
                                }))))
                .then(Commands.literal("rename").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("id", IntegerArgumentType.integer(1)).suggests(PLOT_IDS)
                                .then(Commands.argument("name", StringArgumentType.greedyString()).executes(ctx -> {
                                    Land l = plot(ctx);
                                    if (l == null) {
                                        return 0;
                                    }
                                    String n = StringArgumentType.getString(ctx, "name").trim();
                                    l.name = n.length() > 24 ? n.substring(0, 24) : n;
                                    LandData.get(ctx.getSource().getServer()).touch();
                                    ctx.getSource().sendSystemMessage(Component.translatable("rtshop.plot.renamed", l.id, l.name));
                                    return 1;
                                }))))
                .then(Commands.literal("reset").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("id", IntegerArgumentType.integer(1)).suggests(PLOT_IDS).executes(ctx -> {
                            Land l = plot(ctx);
                            if (l == null) {
                                return 0;
                            }
                            PlotManager.resetToSale(LandData.get(ctx.getSource().getServer()), l);
                            ctx.getSource().sendSystemMessage(Component.translatable("rtshop.plot.reset_done", l.id, l.name));
                            return 1;
                        })))
                .then(Commands.literal("give").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("id", IntegerArgumentType.integer(1)).suggests(PLOT_IDS)
                                .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                                    Land l = plot(ctx);
                                    if (l == null) {
                                        return 0;
                                    }
                                    ServerPlayer t = EntityArgument.getPlayer(ctx, "player");
                                    LandData data = LandData.get(ctx.getSource().getServer());
                                    PlotManager.resetToSale(data, l);
                                    l.owner = t.getUUID();
                                    l.ownerName = t.getGameProfile().getName();
                                    data.touch();
                                    ctx.getSource().sendSystemMessage(Component.translatable("rtshop.plot.given", l.id, l.name, l.ownerName));
                                    return 1;
                                }))))
                .then(Commands.literal("split").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("id", IntegerArgumentType.integer(1)).suggests(PLOT_IDS)
                                .executes(ctx -> split(ctx, 16))
                                .then(Commands.argument("size", IntegerArgumentType.integer(4, 64))
                                        .executes(ctx -> split(ctx, IntegerArgumentType.getInteger(ctx, "size"))))))
                // ---- 상점 토지 (3.79, OP): 장터 등록 · 걷기 · 서 있는 곳에 만들기 · 구획을 상점 토지로
                .then(Commands.literal("shoplot").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("status").executes(ctx -> {
                            var server = ctx.getSource().getServer();
                            LandData data = LandData.get(server);
                            int n = 0, sold = 0, owing = 0;
                            for (Land l : data.lands()) {
                                if (!l.shopLot) continue;
                                n++;
                                if (!l.forSale()) sold++;
                                if (l.rentOwed > 0) owing++;
                            }
                            int fn = n, fs = sold, fo = owing;
                            ctx.getSource().sendSuccess(() -> Component.translatable("rtshop.shoplot.status", data.shopLotsInstalled, fn, fs, fo,
                                    ShopLots.def().lots.size()), false);
                            java.util.List<String> keptOld = ShopLots.retiredKept(data);
                            if (!keptOld.isEmpty()) {
                                ctx.getSource().sendSuccess(() -> Component.translatable("rtshop.shoplot.retired_kept", String.join(", ", keptOld)), false);
                            }
                            return 1;
                        }))
                        .then(Commands.literal("install").executes(ctx -> {
                            int[] r = ShopLots.install(ctx.getSource().getServer());
                            ctx.getSource().sendSuccess(() -> Component.translatable("rtshop.shoplot.installed", r[0], r[1], r[2]), true);
                            if (r[3] + r[4] > 0) {
                                ctx.getSource().sendSuccess(() -> Component.translatable("rtshop.shoplot.retired", r[3], r[4]), true);
                            }
                            return r[0];
                        }))
                        .then(Commands.literal("remove").executes(ctx -> {
                            int[] r = ShopLots.uninstall(ctx.getSource().getServer());
                            ctx.getSource().sendSuccess(() -> Component.translatable("rtshop.shoplot.removed", r[0], r[1]), true);
                            return 1;
                        }))
                        .then(Commands.literal("here")
                                .then(Commands.argument("w", IntegerArgumentType.integer(2, 32))
                                        .then(Commands.argument("h", IntegerArgumentType.integer(2, 32))
                                                .executes(ctx -> shopLotHere(ctx, ""))
                                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                                        .executes(ctx -> shopLotHere(ctx, StringArgumentType.getString(ctx, "name")))))))
                        .then(Commands.argument("id", IntegerArgumentType.integer(1)).suggests(PLOT_IDS)
                                .then(Commands.literal("on").executes(ctx -> shopLot(ctx, true)))
                                .then(Commands.literal("off").executes(ctx -> shopLot(ctx, false)))))
                .then(Commands.literal("starter").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("id", IntegerArgumentType.integer(1)).suggests(PLOT_IDS)
                                .then(Commands.literal("on").executes(ctx -> starter(ctx, true)))
                                .then(Commands.literal("off").executes(ctx -> starter(ctx, false))))
                        .then(Commands.literal("reset").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                            ServerPlayer t = EntityArgument.getPlayer(ctx, "player");
                            LandData.get(ctx.getSource().getServer()).setStarterGiven(t.getUUID(), false);
                            ctx.getSource().sendSystemMessage(Component.translatable("rtshop.plot.starter_reset", t.getGameProfile().getName()));
                            StarterPlots.check(t);
                            return 1;
                        }))))
                .then(Commands.literal("import").requires(s -> s.hasPermission(2))
                        .executes(ctx -> importCmd(ctx, "plots_import.json"))
                        .then(Commands.argument("file", StringArgumentType.greedyString())
                                .executes(ctx -> importCmd(ctx, StringArgumentType.getString(ctx, "file")))))));
    }

    private static int split(CommandContext<CommandSourceStack> ctx, int size) {
        Land l = plot(ctx);
        if (l == null) {
            return 0;
        }
        if (!l.forSale()) {
            ctx.getSource().sendFailure(Component.translatable("rtshop.plot.split_owned", l.name));
            return 0;
        }
        String name = l.name;
        List<Land> parts = PlotManager.split(LandData.get(ctx.getSource().getServer()), l, size);
        if (parts.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("rtshop.plot.split_small", name, size));
            return 0;
        }
        StringBuilder ids = new StringBuilder();
        for (Land p : parts) {
            Claim c = PlotManager.plotClaim(p);
            ids.append(ids.length() == 0 ? "" : ", ").append('#').append(p.id).append('(').append(c == null ? 0 : c.area()).append(')');
        }
        ctx.getSource().sendSystemMessage(Component.translatable("rtshop.plot.split_done", name, parts.size(), ids.toString())
                .withStyle(ChatFormatting.GREEN));
        return parts.size();
    }

    private static int shopLot(CommandContext<CommandSourceStack> ctx, boolean on) {
        Land l = plot(ctx);
        if (l == null) {
            return 0;
        }
        ShopLots.setShopLot(LandData.get(ctx.getSource().getServer()), l, on);
        ctx.getSource().sendSystemMessage(Component.translatable(on ? "rtshop.shoplot.on" : "rtshop.shoplot.off", l.id, l.name, PlotManager.money(l.price)));
        return 1;
    }

    /** 서 있는 블록을 북서 모서리로 w×h 상점 토지를 만든다 (이름이 없으면 「상점 토지 #번호」). */
    private static int shopLotHere(CommandContext<CommandSourceStack> ctx, String name) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        int w = IntegerArgumentType.getInteger(ctx, "w"), h = IntegerArgumentType.getInteger(ctx, "h");
        Land l = ShopLots.create(p.serverLevel(), name.isBlank() ? "" : name.trim(), p.getBlockX(), p.getBlockZ(), w, h);
        if (l == null) {
            ctx.getSource().sendFailure(Component.translatable("rtshop.shoplot.here_taken", w, h));
            return 0;
        }
        if (name.isBlank()) {
            l.name = "상점 토지 " + l.id;   // 서버에는 번역 파일이 없으므로 글자로 직접 (구획 이름과 같게)
            LandData.get(p.server).touch();
        }
        ctx.getSource().sendSystemMessage(Component.translatable("rtshop.shoplot.made", l.id, l.name, w, h, PlotManager.money(l.price),
                PlotManager.money(ShopLots.rentOf(l))).withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int starter(CommandContext<CommandSourceStack> ctx, boolean on) {
        Land l = plot(ctx);
        if (l == null) {
            return 0;
        }
        l.group = on ? LandConfig.get().starterPlotGroup : "";
        LandData.get(ctx.getSource().getServer()).touch();
        ctx.getSource().sendSystemMessage(Component.translatable(on ? "rtshop.plot.starter_on" : "rtshop.plot.starter_off", l.id, l.name));
        return 1;
    }

    private static int importCmd(CommandContext<CommandSourceStack> ctx, String file) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        if (file.contains("..") || file.contains("/") || file.contains("\\")) {
            return 0;
        }
        try {
            int[] r = PlotManager.importFile(p, file);
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.plot.imported", r[0], r[1], r[2]).withStyle(ChatFormatting.GREEN));
            return r[0];
        } catch (Exception ex) {
            RtShop.LOGGER.warn("[rtshop] plot import failed", ex);
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.plot.import_failed", file, String.valueOf(ex.getMessage()))
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
    }
}

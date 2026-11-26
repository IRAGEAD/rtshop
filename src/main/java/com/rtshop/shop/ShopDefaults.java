package com.rtshop.shop;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.rtshop.RtShop;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * 상점 설정을 RT Shop 기본값으로 되돌리기 / 내가 고친 설정으로 다시 되돌리기 (OP).
 *
 * <pre>
 * /rtshop shop status                 상점마다: 기본 설정 그대로 · 직접 고침 · 보관된 내 설정이 있는가
 * /rtshop shop defaults <상점|all>     지금 파일을 "내 설정"으로 보관하고 기본 설정으로 바꾼다
 * /rtshop shop mine <상점|all>         보관해 둔 "내 설정"을 다시 불러온다
 * </pre>
 *
 * 기본 설정 = 이 판을 처음 깔았을 때 만들어지는 상점 파일 (모드 안의 기본 파일 + 덧붙인 조각 + 바뀐 기본값).
 * 내 설정은 config/rtshop/backup/shop_mine/상점.json 에 한 벌만 두고, 덮어쓸 때는 옛 것을 shop_mine_old 로 옮긴다
 * (어느 쪽으로 몇 번을 오가도 지워지는 설정이 없게).
 */
public final class ShopDefaults {
    private ShopDefaults() {
    }

    public enum Outcome {RESET, ALREADY, NO_DEFAULT, RESTORED, NO_SAVED, SAME}

    public static Path mineDir() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("backup").resolve("shop_mine");
    }

    private static Path oldDir() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("backup").resolve("shop_mine_old");
    }

    private static Path file(String id) {
        return ShopLoader.dir().resolve(id + ".json");
    }

    private static boolean okId(String id) {
        return id != null && id.matches("[a-z0-9_\\-]+");
    }

    /** 기본 설정이 있는 상점 (모드가 깔아 주는 상점). */
    public static List<String> known() {
        List<String> out = new ArrayList<>(List.of(ShopLoader.DEFAULTS));
        for (String a : ShopLoader.ADDED) {
            if (!out.contains(a)) out.add(a);
        }
        return out;
    }

    /** 이 상점의 기본 설정 파일 내용 (처음 깔았을 때와 같게: 기본 파일 + 덧붙인 조각 + 바뀐 기본값). 기본 설정이 없는 상점이면 null. */
    public static String defaultJson(String id) throws IOException {
        if (!okId(id) || !known().contains(id)) return null;
        byte[] base;
        try (InputStream in = ShopLoader.class.getResourceAsStream("/rtshop_defaults/" + id + ".json")) {
            if (in == null) return null;
            base = in.readAllBytes();
        }
        Path tmp = Files.createTempDirectory("rtshop_shopdef");
        Path f = tmp.resolve(id + ".json");
        try {
            Files.write(f, base);
            for (String mark : ShopLoader.APPENDED) {
                String target = mark.substring(0, mark.indexOf('+'));
                if (!target.equals(id)) continue;
                ShopLoader.appendItems(f, mark.substring(mark.indexOf('+') + 1, mark.indexOf('@')));
            }
            for (String mark : ShopLoader.REPRICED) {
                if (!mark.substring(0, mark.indexOf(':')).equals(id)) continue;
                String item = mark.substring(mark.indexOf(':') + 1, mark.indexOf('='));
                long from = Long.parseLong(mark.substring(mark.indexOf('=') + 1, mark.indexOf('>')));
                long to = Long.parseLong(mark.substring(mark.indexOf('>') + 1, mark.indexOf('@')));
                ShopLoader.reprice(f, item, from, to);
            }
            return Files.readString(f, StandardCharsets.UTF_8);
        } finally {
            try {
                Files.deleteIfExists(f);
                Files.deleteIfExists(tmp);
            } catch (IOException ignored) {
            }
        }
    }

    /** 두 파일 내용이 같은 설정인가 (줄 바꿈·들여쓰기는 보지 않는다). */
    public static boolean same(String a, String b) {
        if (a == null || b == null) return false;
        try {
            JsonElement x = JsonParser.parseString(a), y = JsonParser.parseString(b);
            return x.equals(y);
        } catch (Exception e) {
            return a.equals(b);
        }
    }

    private static String read(Path f) throws IOException {
        return Files.exists(f) ? Files.readString(f, StandardCharsets.UTF_8) : null;
    }

    /** 지금 파일이 기본 설정 그대로인가. */
    public static boolean isDefault(String id) {
        try {
            String def = defaultJson(id);
            return def != null && same(read(file(id)), def);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean hasMine(String id) {
        return okId(id) && Files.exists(mineDir().resolve(id + ".json"));
    }

    /** 옛 파일을 shop_mine_old 로 옮겨 둔다 (지우지 않는다). */
    private static void shelve(Path f, String id, String tag) throws IOException {
        Files.createDirectories(oldDir());
        Files.move(f, oldDir().resolve(id + "_" + tag + "_" + System.currentTimeMillis() + ".json"), StandardCopyOption.REPLACE_EXISTING);
    }

    /** 기본 설정으로: 지금 파일을 "내 설정"으로 보관하고 기본 파일을 적는다. (다시 읽기는 부른 쪽이 한다) */
    public static synchronized Outcome toDefaults(String id) throws IOException {
        String def = defaultJson(id);
        if (def == null) return Outcome.NO_DEFAULT;
        Path f = file(id);
        String cur = read(f);
        if (same(cur, def)) return Outcome.ALREADY;
        if (cur != null) {
            Path mine = mineDir().resolve(id + ".json");
            Files.createDirectories(mineDir());
            String saved = read(mine);
            if (saved != null && !same(saved, cur)) shelve(mine, id, "mine");
            Files.writeString(mine, cur, StandardCharsets.UTF_8);
        }
        Files.createDirectories(f.getParent());
        Files.writeString(f, def, StandardCharsets.UTF_8);
        return Outcome.RESET;
    }

    /** 내 설정으로: 보관해 둔 파일을 다시 적는다. 보관본은 그대로 둔다 (몇 번이고 오갈 수 있게). */
    public static synchronized Outcome toMine(String id) throws IOException {
        if (!okId(id)) return Outcome.NO_SAVED;
        Path mine = mineDir().resolve(id + ".json");
        String saved = read(mine);
        if (saved == null) return Outcome.NO_SAVED;
        Path f = file(id);
        String cur = read(f);
        if (same(cur, saved)) return Outcome.SAME;
        if (cur != null && !isDefault(id)) {
            // 기본값도 내 설정도 아닌 것(기본값으로 돌린 뒤 다시 고친 것): 잃지 않게 따로 둔다
            Files.createDirectories(oldDir());
            Files.writeString(oldDir().resolve(id + "_before_mine_" + System.currentTimeMillis() + ".json"), cur, StandardCharsets.UTF_8);
        }
        Files.createDirectories(f.getParent());
        Files.writeString(f, saved, StandardCharsets.UTF_8);
        return Outcome.RESTORED;
    }

    /** 보관된 내 설정이 있는 상점. */
    public static List<String> saved() {
        TreeSet<String> out = new TreeSet<>();
        if (Files.isDirectory(mineDir())) {
            try (Stream<Path> st = Files.list(mineDir())) {
                for (Path p : st.toList()) {
                    String n = p.getFileName().toString();
                    if (n.endsWith(".json")) out.add(n.substring(0, n.length() - 5));
                }
            } catch (IOException ignored) {
            }
        }
        return new ArrayList<>(out);
    }

    // ------------------------------------------------------------------ 명령

    private static int run(CommandContext<CommandSourceStack> ctx, boolean defaults) {
        String arg = StringArgumentType.getString(ctx, "shop");
        List<String> ids = !arg.equals("all") ? List.of(arg) : defaults ? known() : saved();
        int changed = 0;
        if (ids.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("rtshop.shopdef.none_saved").withStyle(ChatFormatting.YELLOW), false);
            return 0;
        }
        for (String id : ids) {
            Component line;
            try {
                Outcome o = defaults ? toDefaults(id) : toMine(id);
                if (o == Outcome.RESET || o == Outcome.RESTORED) changed++;
                line = switch (o) {
                    case RESET -> Component.translatable("rtshop.shopdef.reset", id, id).withStyle(ChatFormatting.GREEN);
                    case ALREADY -> Component.translatable("rtshop.shopdef.already", id).withStyle(ChatFormatting.GRAY);
                    case NO_DEFAULT -> Component.translatable("rtshop.shopdef.no_default", id).withStyle(ChatFormatting.YELLOW);
                    case RESTORED -> Component.translatable("rtshop.shopdef.restored", id, id).withStyle(ChatFormatting.GREEN);
                    case NO_SAVED -> Component.translatable("rtshop.shopdef.no_saved", id).withStyle(ChatFormatting.YELLOW);
                    case SAME -> Component.translatable("rtshop.shopdef.same", id).withStyle(ChatFormatting.GRAY);
                };
            } catch (Exception e) {
                RtShop.LOGGER.warn("[rtshop] 상점 {} 설정을 바꾸지 못했습니다", id, e);
                line = Component.translatable("rtshop.shopdef.failed", id, String.valueOf(e.getMessage())).withStyle(ChatFormatting.RED);
            }
            final Component msg = line;
            ctx.getSource().sendSuccess(() -> msg, false);
        }
        if (changed > 0) {
            ShopServer.reload(ctx.getSource().getServer());
            final int n = changed;
            RtShop.LOGGER.info("[rtshop] 상점 설정 {}곳을 {} 되돌렸습니다 ({})", n, defaults ? "기본값으로" : "내 설정으로", ctx.getSource().getTextName());
            ctx.getSource().sendSuccess(() -> Component.translatable("rtshop.shopdef.done", n).withStyle(ChatFormatting.GOLD), true);
        }
        return changed;
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.translatable("rtshop.shopdef.status_head").withStyle(ChatFormatting.GOLD), false);
        TreeSet<String> ids = new TreeSet<>(ShopServer.shops().keySet());
        ids.addAll(known());
        for (String id : ids) {
            boolean kn = known().contains(id);
            boolean def = kn && isDefault(id);
            MutableComponent line = Component.translatable(!kn ? "rtshop.shopdef.st_custom" : def ? "rtshop.shopdef.st_default" : "rtshop.shopdef.st_edited", id)
                    .withStyle(!kn ? ChatFormatting.GRAY : def ? ChatFormatting.WHITE : ChatFormatting.AQUA);
            if (hasMine(id)) line.append(Component.translatable("rtshop.shopdef.st_saved").withStyle(ChatFormatting.GREEN));
            ctx.getSource().sendSuccess(() -> line, false);
        }
        return 1;
    }

    /** /rtshop shop status | defaults <상점|all> | mine <상점|all> */
    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("shop").requires(src -> src.hasPermission(2))
                .executes(ShopDefaults::status)
                .then(Commands.literal("status").executes(ShopDefaults::status))
                .then(Commands.literal("defaults").then(Commands.argument("shop", StringArgumentType.word())
                        .suggests((ctx, b) -> {
                            List<String> s = new ArrayList<>(known());
                            s.add(0, "all");
                            return SharedSuggestionProvider.suggest(s, b);
                        })
                        .executes(ctx -> run(ctx, true))))
                .then(Commands.literal("mine").then(Commands.argument("shop", StringArgumentType.word())
                        .suggests((ctx, b) -> {
                            List<String> s = new ArrayList<>(saved());
                            s.add(0, "all");
                            return SharedSuggestionProvider.suggest(s, b);
                        })
                        .executes(ctx -> run(ctx, false))));
    }
}

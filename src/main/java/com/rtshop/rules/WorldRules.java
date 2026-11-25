package com.rtshop.rules;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 월드 규칙 (config/rtshop/world_rules.json): 이 서버에서 아예 생기지 않을 것들.
 *  - neverStructures: 새 지형에 절대 생기지 않는 건축물 (나선 비경의 무너진 문, Create 전용 건축물 …)
 *  - neverRecipes: 막는 조합법 ("item:아이템ID" = 그 아이템을 만드는 조합법 전부)
 *  - lootBlockedMods: 이 모드의 아이템은 상자·몹 전리품에서 빠진다
 *  - 나선 비경의 문: 운영자(크리에이티브)만 세울 수 있고, 정해 둔 한 곳(마을)의 문으로만 들어갈 수 있다
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class WorldRules {
    public static final String ABYSS_KEY = "spiral_abyss:abyss_gate_activate_block";
    public static final String ABYSS_DIM = "spiral_abyss:spiral_abyss";

    public final Set<String> neverStructures = new LinkedHashSet<>();
    public final Set<String> neverRecipes = new LinkedHashSet<>();
    public final Set<String> lootBlockedMods = new LinkedHashSet<>();
    public boolean abyssPortalOpOnly = true;
    public boolean abyssPortalSet;
    public String abyssPortalDim = "minecraft:overworld";
    public int abyssPortalX, abyssPortalY, abyssPortalZ, abyssPortalRadius = 24;
    public String textPortalOnly = "나선 비경으로 가는 문은 마을에 있는 하나뿐입니다.";
    public String textPortalBuild = "나선 비경의 문은 새로 세울 수 없습니다. 마을의 문을 이용하세요.";

    private static WorldRules current;
    private static volatile Set<String> never = Collections.emptySet();
    private static volatile Set<String> lootMods = Collections.emptySet();

    public static WorldRules defaults() {
        WorldRules w = new WorldRules();
        w.neverStructures.addAll(List.of("spiral_abyss:ruined_portal",
                "idas:tinkers_citadel", "idas:tinkers_workshop", "idas:train_ruins", "idas:nether_pump_camp", "idas:washing_camp"));
        w.neverRecipes.addAll(List.of("item:" + ABYSS_KEY, "item:spiral_abyss:abyss_gate_vertical", "item:spiral_abyss:abyss_gate_horizontal", "item:spiral_abyss:abyss_gate_curve"));
        w.lootBlockedMods.add("create");
        return w;
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("world_rules.json");
    }

    public static synchronized WorldRules get() {
        if (current == null) {
            current = load();
            publish(current);
        }
        return current;
    }

    public static synchronized void reload() {
        current = null;
        get();
    }

    private static void publish(WorldRules w) {
        never = Set.copyOf(w.neverStructures);
        lootMods = Set.copyOf(w.lootBlockedMods);
    }

    private static void strings(JsonObject o, String k, Set<String> into) {
        if (!o.has(k)) return;
        into.clear();
        for (JsonElement e : o.getAsJsonArray(k)) {
            String s = e.getAsString().trim();
            if (!s.isEmpty()) into.add(s);
        }
    }

    public static WorldRules parse(JsonObject o) {
        WorldRules w = defaults();
        strings(o, "neverStructures", w.neverStructures);
        strings(o, "neverRecipes", w.neverRecipes);
        strings(o, "lootBlockedMods", w.lootBlockedMods);
        if (o.has("abyssPortalOpOnly")) w.abyssPortalOpOnly = o.get("abyssPortalOpOnly").getAsBoolean();
        if (o.has("abyssPortal") && o.get("abyssPortal").isJsonObject()) {
            JsonObject p = o.getAsJsonObject("abyssPortal");
            w.abyssPortalSet = p.has("set") && p.get("set").getAsBoolean();
            if (p.has("dim")) w.abyssPortalDim = p.get("dim").getAsString();
            if (p.has("x")) w.abyssPortalX = p.get("x").getAsInt();
            if (p.has("y")) w.abyssPortalY = p.get("y").getAsInt();
            if (p.has("z")) w.abyssPortalZ = p.get("z").getAsInt();
            if (p.has("radius")) w.abyssPortalRadius = Math.max(4, p.get("radius").getAsInt());
        }
        if (o.has("textPortalOnly")) w.textPortalOnly = o.get("textPortalOnly").getAsString();
        if (o.has("textPortalBuild")) w.textPortalBuild = o.get("textPortalBuild").getAsString();
        return w;
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        JsonArray help = new JsonArray();
        help.add("월드 규칙. neverStructures: 새 지형에 절대 생기지 않는 건축물 ID. neverRecipes: 막는 조합법 (item:아이템ID). lootBlockedMods: 전리품에서 빼는 모드 ID.");
        help.add("abyssPortal: 나선 비경으로 들어갈 수 있는 단 하나의 문. 게임 안에서 문 앞에 서서 /rtshop abyss portal here (OP). abyssPortalOpOnly: 문은 운영자(크리에이티브)만 세울 수 있음.");
        help.add("이미 만들어진 지형에 있는 건축물은 사라지지 않습니다. 고친 뒤 /rtshop reload.");
        o.add("_help", help);
        JsonArray a = new JsonArray();
        neverStructures.forEach(a::add);
        o.add("neverStructures", a);
        JsonArray b = new JsonArray();
        neverRecipes.forEach(b::add);
        o.add("neverRecipes", b);
        JsonArray c = new JsonArray();
        lootBlockedMods.forEach(c::add);
        o.add("lootBlockedMods", c);
        o.addProperty("abyssPortalOpOnly", abyssPortalOpOnly);
        JsonObject p = new JsonObject();
        p.addProperty("set", abyssPortalSet);
        p.addProperty("dim", abyssPortalDim);
        p.addProperty("x", abyssPortalX);
        p.addProperty("y", abyssPortalY);
        p.addProperty("z", abyssPortalZ);
        p.addProperty("radius", abyssPortalRadius);
        o.add("abyssPortal", p);
        o.addProperty("textPortalOnly", textPortalOnly);
        o.addProperty("textPortalBuild", textPortalBuild);
        return o;
    }

    private static WorldRules load() {
        try {
            if (Files.exists(file())) return parse(JsonParser.parseString(Files.readString(file(), StandardCharsets.UTF_8)).getAsJsonObject());
            WorldRules w = defaults();
            save(w);
            return w;
        } catch (Exception e) {
            RtShop.LOGGER.warn("[rtshop] world_rules.json 을 읽지 못했습니다: {}", e.toString());
            return defaults();
        }
    }

    public static synchronized void save(WorldRules w) {
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(w.toJson()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            RtShop.LOGGER.warn("[rtshop] world_rules.json 을 쓰지 못했습니다: {}", e.toString());
        }
        current = w;
        publish(w);
    }

    /** 시험용: 파일을 건드리지 않고 규칙을 바꾼다. */
    public static synchronized void setForTest(WorldRules w) {
        current = w;
        publish(w);
    }

    // ------------------------------------------------------------------ 묻는 곳

    /** 지형 생성이 묻는다 (아무 스레드). */
    public static boolean neverStructure(String id) {
        if (current == null) get();
        return never.contains(id);
    }

    public static boolean recipeBlocked(ResourceLocation id, ResourceLocation output) {
        WorldRules w = get();
        if (w.neverRecipes.isEmpty()) return false;
        return w.neverRecipes.contains(id.toString()) || (output != null && w.neverRecipes.contains("item:" + output));
    }

    /** 전리품에서 빠져야 하는 아이템인가. */
    public static boolean lootBlocked(ItemStack s) {
        if (current == null) get();
        Set<String> m = lootMods;
        if (m.isEmpty() || s.isEmpty()) return false;
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(s.getItem());
        return rl != null && m.contains(rl.getNamespace());
    }

    /** 이 사람이 지금 선 자리에서 나선 비경으로 들어가도 되는가 (정해 둔 문 곁인가). 문을 아직 정하지 않았으면 어디서든 된다. */
    public static boolean portalAllowed(ServerPlayer p) {
        WorldRules w = get();
        if (!w.abyssPortalSet) return true;
        String dim = p.level().dimension().location().toString();
        if (dim.equals(ABYSS_DIM)) return true; // 이미 안에 있다 (층 이동)
        if (p.isCreative() && p.hasPermissions(2)) return true;
        if (!dim.equals(w.abyssPortalDim)) return false;
        double dx = p.getX() - (w.abyssPortalX + 0.5), dy = p.getY() - w.abyssPortalY, dz = p.getZ() - (w.abyssPortalZ + 0.5);
        return dx * dx + dz * dz <= (double) w.abyssPortalRadius * w.abyssPortalRadius && Math.abs(dy) <= w.abyssPortalRadius;
    }

    /** 막아야 하면 알리고 true. */
    public static boolean refusePortal(ServerPlayer p) {
        try {
            if (portalAllowed(p)) return false;
            com.rtshop.notice.Notice.send(p, Component.literal(get().textPortalOnly).withStyle(ChatFormatting.YELLOW));
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void setPortal(Level level, BlockPos pos) {
        WorldRules w = get();
        w.abyssPortalSet = true;
        w.abyssPortalDim = level.dimension().location().toString();
        w.abyssPortalX = pos.getX();
        w.abyssPortalY = pos.getY();
        w.abyssPortalZ = pos.getZ();
        save(w);
    }

    // ------------------------------------------------------------------ 이벤트

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        try {
            ResourceLocation rl = ForgeRegistries.BLOCKS.getKey(event.getPlacedBlock().getBlock());
            if (rl == null || !rl.toString().equals(ABYSS_KEY)) return;
            WorldRules w = get();
            if (!(event.getEntity() instanceof ServerPlayer p)) {
                if (w.abyssPortalOpOnly) event.setCanceled(true);
                return;
            }
            boolean op = p.isCreative() && p.hasPermissions(2);
            if (w.abyssPortalOpOnly && !op) {
                event.setCanceled(true);
                com.rtshop.notice.Notice.send(p, Component.literal(w.textPortalBuild).withStyle(ChatFormatting.YELLOW));
                return;
            }
            if (op && !w.abyssPortalSet && event.getLevel() instanceof Level l) {
                setPortal(l, event.getPos());
                p.sendSystemMessage(Component.literal("이 문을 나선 비경으로 가는 단 하나의 문으로 정했습니다. (바꾸려면 새 문 앞에서 /rtshop abyss portal here)").withStyle(ChatFormatting.GREEN));
            }
        } catch (Throwable ignored) {
        }
    }

    @SubscribeEvent
    public static void onTravel(EntityTravelToDimensionEvent event) {
        try {
            if (!(event.getEntity() instanceof ServerPlayer p)) return;
            if (!event.getDimension().location().toString().equals(ABYSS_DIM)) return;
            if (refusePortal(p)) event.setCanceled(true);
        } catch (Throwable ignored) {
        }
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("rtshop").then(Commands.literal("abyss").requires(s -> s.hasPermission(2))
                .then(Commands.literal("portal")
                        .executes(ctx -> {
                            WorldRules w = get();
                            ctx.getSource().sendSuccess(() -> Component.literal(w.abyssPortalSet
                                    ? "나선 비경의 문: " + w.abyssPortalDim + " " + w.abyssPortalX + " " + w.abyssPortalY + " " + w.abyssPortalZ + " (반경 " + w.abyssPortalRadius + "칸 안에서만 입장)"
                                    : "나선 비경의 문이 아직 정해지지 않았습니다. 문 앞에서 /rtshop abyss portal here"), false);
                            return 1;
                        })
                        .then(Commands.literal("here").executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            setPortal(p.level(), p.blockPosition());
                            ctx.getSource().sendSuccess(() -> Component.literal("여기를 나선 비경으로 가는 단 하나의 문으로 정했습니다. 다른 곳의 문으로는 들어갈 수 없습니다.").withStyle(ChatFormatting.GREEN), true);
                            return 1;
                        }))
                        .then(Commands.literal("clear").executes(ctx -> {
                            WorldRules w = get();
                            w.abyssPortalSet = false;
                            save(w);
                            ctx.getSource().sendSuccess(() -> Component.literal("문 지정을 풀었습니다. 지금은 어느 문으로든 들어갈 수 있습니다."), true);
                            return 1;
                        })))));
    }
}

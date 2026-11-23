package com.rtshop.stats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.rtshop.RtShop;
import com.rtshop.net.ItemStatsPacket;
import com.rtshop.net.ShopNet;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 장비 능력치 설정: 게임 안 모든 장비(바닐라·모드)의 공격력·공격 속도·방어력 등을 바꾼다.
 * config/rtshop/item_stats.json 또는 OP 편집기(/rtshop itemstats)로 고친다. 강화 보너스는 바뀐 값 위에 붙는다.
 * <p>값은 툴팁에 보이는 숫자 그대로 쓴다: 공격력 8 = "공격 피해 8", 공격 속도 1.6 = "공격 속도 1.6", 방어력 6 = "+6 방어".
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class ItemStats {
    private ItemStats() {
    }

    /** 바닐라가 "기본 공격력/속도"로 보여 주는 고정 UUID (Item.BASE_ATTACK_DAMAGE_UUID / BASE_ATTACK_SPEED_UUID). */
    public static final UUID BASE_DAMAGE = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    public static final UUID BASE_SPEED = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");

    /** 아이템 ID → (속성 ID → 툴팁에 보이는 값). */
    private static final Map<String, Map<String, Double>> SERVER = new LinkedHashMap<>();
    private static volatile Map<String, Map<String, Double>> client = new LinkedHashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("item_stats.json");
    }

    // ------------------------------------------------------------------ 값 계산

    /** 툴팁 값 ↔ 수정치: 공격력은 플레이어 기본 1, 공격 속도는 기본 4 를 더해 보인다. */
    public static double playerBase(Attribute a) {
        if (a == Attributes.ATTACK_DAMAGE) return 1.0;
        if (a == Attributes.ATTACK_SPEED) return 4.0;
        return 0.0;
    }

    public static UUID uuid(Attribute a, EquipmentSlot slot, String attrId) {
        if (a == Attributes.ATTACK_DAMAGE && slot == EquipmentSlot.MAINHAND) return BASE_DAMAGE;
        if (a == Attributes.ATTACK_SPEED && slot == EquipmentSlot.MAINHAND) return BASE_SPEED;
        return UUID.nameUUIDFromBytes(("rtshop_item_stats:" + attrId + ":" + slot.getName()).getBytes(StandardCharsets.UTF_8));
    }

    /** 이 아이템이 능력치를 내는 칸 (갑옷은 그 부위, 방패는 왼손, 그 밖은 오른손). */
    public static EquipmentSlot slotOf(ItemStack s) {
        return LivingEntity.getEquipmentSlotForItem(s);
    }

    public static Map<String, Map<String, Double>> table(boolean clientSide) {
        return clientSide ? client : SERVER;
    }

    public static String idOf(Item item) {
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
        return rl == null ? "" : rl.toString();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttributes(ItemAttributeModifierEvent e) {
        ItemStack s = e.getItemStack();
        if (s.isEmpty()) return;
        boolean clientSide = net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()
                && net.minecraftforge.fml.util.thread.EffectiveSide.get().isClient();
        Map<String, Double> m = table(clientSide).get(idOf(s.getItem()));
        if (m == null || m.isEmpty()) return;
        EquipmentSlot slot = slotOf(s);
        if (e.getSlotType() != slot) return;
        for (Map.Entry<String, Double> en : m.entrySet()) {
            ResourceLocation rl = ResourceLocation.tryParse(en.getKey());
            Attribute a = rl == null ? null : ForgeRegistries.ATTRIBUTES.getValue(rl);
            if (a == null || en.getValue() == null) continue;
            e.removeAttribute(a);
            double mod = en.getValue() - playerBase(a);
            e.addModifier(a, new AttributeModifier(uuid(a, slot, en.getKey()), "rtshop_item_stats", mod, AttributeModifier.Operation.ADDITION));
        }
    }

    // ------------------------------------------------------------------ 저장 · 불러오기

    public static synchronized void load() {
        SERVER.clear();
        Path f = file();
        try {
            if (!Files.exists(f)) {
                save();
                return;
            }
            JsonObject o = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
            SERVER.putAll(parse(o));
        } catch (Exception ex) {
            RtShop.LOGGER.warn("[RT Shop] item_stats.json 을 읽지 못했습니다: {}", ex.toString());
        }
    }

    public static Map<String, Map<String, Double>> parse(JsonObject o) {
        Map<String, Map<String, Double>> out = new LinkedHashMap<>();
        JsonObject items = o.has("items") ? o.getAsJsonObject("items") : new JsonObject();
        for (String id : items.keySet()) {
            JsonElement v = items.get(id);
            if (!v.isJsonObject()) continue;
            Map<String, Double> m = new LinkedHashMap<>();
            for (String k : v.getAsJsonObject().keySet()) {
                try {
                    m.put(k, v.getAsJsonObject().get(k).getAsDouble());
                } catch (Exception ignored) {
                }
            }
            if (!m.isEmpty()) out.put(id, m);
        }
        return out;
    }

    public static synchronized JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("_help", "장비 능력치 설정. \"items\" 안에 \"아이템 ID\": {\"속성 ID\": 값} 으로 적는다. 값은 툴팁에 보이는 숫자 그대로 "
                + "(공격력 8, 공격 속도 1.6, 방어력 6 …). 쓸 수 있는 속성 예: minecraft:generic.attack_damage, minecraft:generic.attack_speed, "
                + "minecraft:generic.armor, minecraft:generic.armor_toughness, minecraft:generic.knockback_resistance, minecraft:generic.max_health, "
                + "minecraft:generic.movement_speed, minecraft:generic.luck, 모드 속성(epicfight:impact 등). 적지 않은 속성은 원래 값 그대로. "
                + "게임 안에서는 OP 가 /rtshop itemstats 로 편집기를 연다. 고친 뒤 /rtshop itemstats reload.");
        JsonObject items = new JsonObject();
        for (var en : SERVER.entrySet()) {
            JsonObject m = new JsonObject();
            en.getValue().forEach(m::addProperty);
            items.add(en.getKey(), m);
        }
        o.add("items", items);
        return o;
    }

    public static synchronized void save() {
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), GSON.toJson(toJson()), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            RtShop.LOGGER.warn("[RT Shop] item_stats.json 을 저장하지 못했습니다: {}", ex.toString());
        }
    }

    /** 한 아이템의 설정을 바꾼다 (빈 맵 = 원래대로). */
    public static synchronized void put(String itemId, Map<String, Double> m) {
        if (m == null || m.isEmpty()) SERVER.remove(itemId);
        else SERVER.put(itemId, new LinkedHashMap<>(m));
    }

    public static synchronized Map<String, Double> get(String itemId) {
        return SERVER.get(itemId);
    }

    public static void setClient(String json) {
        try {
            client = parse(JsonParser.parseString(json).getAsJsonObject());
        } catch (Exception ignored) {
        }
    }

    // ------------------------------------------------------------------ 동기화 · 편집기

    public static void sync(ServerPlayer p) {
        try {
            ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new ItemStatsPacket(ItemStatsPacket.CONFIG, "", toJson().toString()));
        } catch (Exception ignored) {
        }
    }

    public static void syncAll(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) sync(p);
    }

    /** 바뀐 설정이 이미 들고·입고 있는 장비에도 바로 걸리게 장비 칸을 다시 계산시킨다. */
    public static void refreshEquipment(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            // 예전 설정으로 붙어 있던 이 기능의 수정치를 모두 떼고
            for (var en : ForgeRegistries.ATTRIBUTES.getEntries()) {
                var inst = p.getAttribute(en.getValue());
                if (inst == null) continue;
                String aid = en.getKey().location().toString();
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    UUID u = uuid(en.getValue(), slot, aid);
                    if (u != BASE_DAMAGE && u != BASE_SPEED) inst.removeModifier(u);
                }
            }
            // 지금 들고·입고 있는 장비의 수정치를 새 설정으로 다시 붙인다
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack s = p.getItemBySlot(slot);
                if (s.isEmpty()) continue;
                p.getAttributes().removeAttributeModifiers(s.getItem().getAttributeModifiers(slot, s));
                var now = s.getAttributeModifiers(slot);
                p.getAttributes().removeAttributeModifiers(now);
                p.getAttributes().addTransientAttributeModifiers(now);
            }
        }
    }

    /** 편집기에서 저장 (OP). json = {"속성 ID": 값} (빈 객체 = 원래대로). */
    public static void saveFromEditor(ServerPlayer p, String itemId, String json) {
        if (!p.hasPermissions(2)) return;
        try {
            ResourceLocation rl = ResourceLocation.tryParse(itemId);
            if (rl == null || !ForgeRegistries.ITEMS.containsKey(rl)) throw new IllegalArgumentException(itemId);
            Map<String, Double> m = new LinkedHashMap<>();
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            for (String k : o.keySet()) {
                ResourceLocation ar = ResourceLocation.tryParse(k);
                if (ar == null || !ForgeRegistries.ATTRIBUTES.containsKey(ar)) continue;
                double v = o.get(k).getAsDouble();
                if (Double.isFinite(v)) m.put(k, v);
            }
            put(itemId, m);
            save();
            syncAll(p.server);
            refreshEquipment(p.server);
            com.rtshop.notice.Notice.send(p, Component.translatable(m.isEmpty() ? "rtshop.itemstats.reset" : "rtshop.itemstats.saved", itemId)
                    .withStyle(ChatFormatting.GREEN));
        } catch (Exception ex) {
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.itemstats.failed", String.valueOf(ex.getMessage())).withStyle(ChatFormatting.RED));
        }
    }

    public static void openEditor(ServerPlayer p, String itemId) {
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new ItemStatsPacket(ItemStatsPacket.OPEN, itemId, toJson().toString()));
    }

    @SubscribeEvent
    public static void onStarting(net.minecraftforge.event.server.ServerStartingEvent event) {
        load();
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) sync(sp);
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("rtshop").then(Commands.literal("itemstats").requires(s -> s.hasPermission(2))
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    ItemStack held = p.getMainHandItem();
                    openEditor(p, held.isEmpty() ? "" : idOf(held.getItem()));
                    return 1;
                })
                .then(Commands.literal("reload").executes(c -> {
                    load();
                    syncAll(c.getSource().getServer());
                    refreshEquipment(c.getSource().getServer());
                    int n = SERVER.size();
                    c.getSource().sendSuccess(() -> Component.translatable("rtshop.itemstats.reloaded", n), true);
                    return 1;
                }))
                .then(Commands.literal("list").executes(c -> {
                    var src = c.getSource();
                    if (SERVER.isEmpty()) {
                        src.sendSuccess(() -> Component.translatable("rtshop.itemstats.list_empty"), false);
                        return 1;
                    }
                    src.sendSuccess(() -> Component.translatable("rtshop.itemstats.list_title", SERVER.size()).withStyle(ChatFormatting.GOLD), false);
                    for (var en : SERVER.entrySet()) {
                        StringBuilder sb = new StringBuilder();
                        en.getValue().forEach((k, v) -> sb.append(sb.length() == 0 ? "" : ", ").append(k.replace("minecraft:generic.", "")).append('=').append(v));
                        Component line = Component.literal(" · " + en.getKey() + "  ").withStyle(ChatFormatting.YELLOW)
                                .append(Component.literal(sb.toString()).withStyle(ChatFormatting.GRAY))
                                .withStyle(st -> st.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rtshop itemstats edit " + en.getKey())));
                        src.sendSuccess(() -> line, false);
                    }
                    return 1;
                }))
                .then(Commands.literal("edit").then(Commands.argument("item", StringArgumentType.greedyString()).executes(c -> {
                    openEditor(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "item").trim());
                    return 1;
                })))));
    }
}

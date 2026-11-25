package com.rtshop.shop;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import com.rtshop.net.ShopEditDataPacket;
import com.rtshop.net.ShopNet;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** OP 가 상인의 상점 화면에서 품목·가격을 직접 고치는 편집기 (서버 쪽: 파일 읽기·쓰기·다시 읽기). */
public final class ShopEditor {
    private ShopEditor() {
    }

    public static final int OPEN = 0;
    public static final int SAVE = 1;

    static Path file(String id) {
        return ShopLoader.dir().resolve(id + ".json");
    }

    public static boolean validId(String id) {
        return id != null && id.matches("[a-z0-9_\\-]+") && ShopServer.shops().containsKey(id);
    }

    /** 상점 파일을 고쳐 쓰고 다시 읽는다. 예전 파일 경로(.bak)를 돌려준다. */
    public static Path save(net.minecraft.server.MinecraftServer server, String id, String json) throws Exception {
        JsonObject o = JsonParser.parseString(json).getAsJsonObject();
        if (!o.has("items") || !o.get("items").isJsonArray()) {
            throw new IllegalArgumentException("items");
        }
        o.remove("_auto"); // 화면에서만 쓰는 자동 상품 목록
        Path f = file(id);
        Path bak = f.resolveSibling(id + ".json.bak");
        Files.copy(f, bak, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        Files.writeString(f, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(o), StandardCharsets.UTF_8);
        ShopServer.reload(server);
        return bak;
    }

    /**
     * 자동 규칙(auto)으로 진열된 상품도 편집 화면에 보이도록 "_auto" 로 붙여 보낸다.
     * 화면에서 고친 자동 상품은 저장할 때 직접 항목(items)이 되고, 지운 것은 exclude 에 들어간다.
     */
    static String withAuto(String id, String content) {
        try {
            JsonObject o = JsonParser.parseString(content).getAsJsonObject();
            java.util.Set<String> explicit = new java.util.HashSet<>();
            if (o.has("items")) {
                for (com.google.gson.JsonElement e : o.getAsJsonArray("items")) {
                    if (e.isJsonObject() && e.getAsJsonObject().has("item")) explicit.add(e.getAsJsonObject().get("item").getAsString());
                }
            }
            ShopData d = ShopServer.shops().get(id);
            if (d == null) return content;
            com.google.gson.JsonArray auto = new com.google.gson.JsonArray();
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (ShopData.Entry e : d.entries) {
                net.minecraft.resources.ResourceLocation rl = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(e.stack.getItem());
                if (rl == null) continue;
                String item = rl.toString();
                if (explicit.contains(item) || !seen.add(item)) continue;
                JsonObject a = new JsonObject();
                a.addProperty("item", item);
                a.addProperty("price", e.price);
                a.addProperty("category", e.category);
                if (!e.buyable) a.addProperty("buy", false);
                if (!e.sellable) a.addProperty("sell", false);
                a.addProperty("_auto", true);
                auto.add(a);
            }
            o.add("_auto", auto);
            return o.toString();
        } catch (Exception ex) {
            return content;
        }
    }

    public static void handle(ServerPlayer p, int action, String id, String json) {
        if (!com.rtshop.util.Perm.op(p) || !validId(id)) {
            return;
        }
        try {
            if (action == OPEN) {
                String content = withAuto(id, Files.readString(file(id), StandardCharsets.UTF_8));
                ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new ShopEditDataPacket(id, content));
            } else if (action == SAVE) {
                Path bak = save(p.server, id, json);
                com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.shopedit.saved", id).withStyle(ChatFormatting.GREEN));
                RtShop.LOGGER.info("[rtshop] {} 이(가) 상점 {} 을(를) 게임 안에서 고쳤습니다 (예전 파일: {})", p.getGameProfile().getName(), id, bak.getFileName());
                ShopServer.openShop(p, id, -1);
            }
        } catch (Exception e) {
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.shopedit.failed", String.valueOf(e.getMessage())).withStyle(ChatFormatting.RED));
        }
    }
}

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
        Path f = file(id);
        Path bak = f.resolveSibling(id + ".json.bak");
        Files.copy(f, bak, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        Files.writeString(f, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(o), StandardCharsets.UTF_8);
        ShopServer.reload(server);
        return bak;
    }

    public static void handle(ServerPlayer p, int action, String id, String json) {
        if (!p.hasPermissions(2) || !validId(id)) {
            return;
        }
        try {
            if (action == OPEN) {
                String content = Files.readString(file(id), StandardCharsets.UTF_8);
                ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new ShopEditDataPacket(id, content));
            } else if (action == SAVE) {
                Path bak = save(p.server, id, json);
                p.sendSystemMessage(Component.translatable("rtshop.shopedit.saved", id).withStyle(ChatFormatting.GREEN));
                RtShop.LOGGER.info("[rtshop] {} 이(가) 상점 {} 을(를) 게임 안에서 고쳤습니다 (예전 파일: {})", p.getGameProfile().getName(), id, bak.getFileName());
                ShopServer.openShop(p, id, -1);
            }
        } catch (Exception e) {
            p.sendSystemMessage(Component.translatable("rtshop.shopedit.failed", String.valueOf(e.getMessage())).withStyle(ChatFormatting.RED));
        }
    }
}

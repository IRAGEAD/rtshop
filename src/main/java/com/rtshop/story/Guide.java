package com.rtshop.story;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import com.rtshop.net.ShopNet;
import com.rtshop.net.StoryPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.PacketDistributor;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 가이드북 (config/rtshop/guide.json, 3.53): 아이템 「조사대 안내서」를 들고 우클릭하면 펼쳐진다.
 * 이야기를 따라가며 저절로 알게 되는 것 말고, 모드팩의 시스템과 쓰는 법을 갈래·모드별로 그림과 글로 적어 둔 책. 이름으로 찾을 수 있다.
 * <pre>
 * "categories": [ { "id", "name", "pages": [ { "id", "title", "mod": "이 모드가 있을 때만 (쉼표로 여럿 = 하나라도)", "icons": [아이템 ID…],
 *                                               "image": "rtshop:textures/guide/bank.png", "iw": 480, "ih": 270, "text": "글 (\n 줄바꿈)", "keys": "찾기에 걸릴 낱말" } ] } ]
 * </pre>
 * 직접 고쳤으면 "edited": true 를 넣어 새 판으로 바뀌지 않게 한다. 처음 들어온 사람에게 한 권 주고, 잃어버리면 잡화점에서 다시 산다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class Guide {
    private Guide() {
    }

    private static JsonObject config;

    static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("guide.json");
    }

    static JsonObject bundled() {
        try (InputStream in = Guide.class.getResourceAsStream("/rtshop_defaults/guide.json")) {
            if (in == null) return new JsonObject();
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            return new JsonObject();
        }
    }

    public static synchronized JsonObject get() {
        if (config == null) {
            Path f = file();
            JsonObject def = bundled();
            try {
                JsonObject cur = Files.exists(f) ? JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject() : null;
                int v = cur != null && cur.has("version") ? cur.get("version").getAsInt() : -1;
                boolean edited = cur != null && cur.has("edited") && cur.get("edited").getAsBoolean();
                int bundledV = def.has("version") ? def.get("version").getAsInt() : 1;
                if (cur == null || (!edited && v < bundledV)) {
                    Files.createDirectories(f.getParent());
                    Files.writeString(f, new com.google.gson.GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(def), StandardCharsets.UTF_8);
                    cur = def;
                }
                config = cur;
            } catch (Exception e) {
                RtShop.LOGGER.warn("[rtshop] guide.json 을 읽지 못해 기본 안내서를 씁니다: {}", e.toString());
                config = def;
            }
        }
        return config;
    }

    public static synchronized void reload() {
        config = null;
    }

    public static int pagesForTest() {
        int n = 0;
        JsonObject g = get();
        if (!g.has("categories")) return 0;
        for (var c : g.getAsJsonArray("categories")) n += c.getAsJsonObject().getAsJsonArray("pages").size();
        return n;
    }

    public static void open(ServerPlayer p) {
        if (p.connection == null || p instanceof FakePlayer) return;
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new StoryPacket(StoryPacket.GUIDE, StoryText.json(p, get()).toString()));
    }

    /** 처음 들어온 사람에게 안내서를 한 권 준다 (도감과 같은 장부에 적는다). */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p instanceof FakePlayer || !Codex.get().giveOnFirstJoin) return;
        MinecraftServer server = p.server;
        Codex.Rec r = Codex.data(server).of(p.getUUID());
        if (r.guideGiven) return;
        r.guideGiven = true;
        Codex.data(server).setDirty();
        ItemStack book = new ItemStack(com.rtshop.item.ModItems.GUIDEBOOK.get());
        if (!p.getInventory().contains(book) && !p.getInventory().add(book)) p.drop(book, false);
    }

    /** 3.75.0 (사용자 W-3): 조사 수첩도 한 권 준다. 예전부터 하던 사람도 다음에 들어올 때 한 번 받는다. */
    @SubscribeEvent
    public static void onLoginNotebook(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p instanceof FakePlayer || !Codex.get().giveOnFirstJoin) return;
        giveNotebook(p);
    }

    public static final String NOTEBOOK_TAG = "rtshop_notebook_given";

    /** 조사 수첩을 아직 받지 않았으면 한 권. 줬으면 true. */
    public static boolean giveNotebook(ServerPlayer p) {
        if (p.getPersistentData().getBoolean(NOTEBOOK_TAG)) return false;
        p.getPersistentData().putBoolean(NOTEBOOK_TAG, true);
        ItemStack book = new ItemStack(com.rtshop.item.ModItems.SURVEY_NOTEBOOK.get());
        if (p.getInventory().contains(book)) return false;
        if (!p.getInventory().add(book)) p.drop(book, false);
        return true;
    }
}

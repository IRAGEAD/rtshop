package com.rtshop.client;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forgespi.language.IModInfo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * 3.70.0: 모드팩의 dhcompat 은 켤 때마다 ImmediatelyFast 의 '빠른 버퍼 업로드'(fast_buffer_upload)를 설정 파일에서 꺼 버렸다.
 * RT Shop 이 Distant Horizons + Chunky 크래시를 직접 막게 되어 dhcompat 을 뺄 수 있는데, 빼도 꺼진 값은 설정 파일에 남는다.
 * dhcompat 이 없을 때 그 값을 한 번 되돌린다 (다음 실행부터 적용, 한 번 한 뒤에는 플레이어가 끄면 그대로 둔다). client.json 의 restoreFastBuffer 로 끈다.
 */
final class FastBufferFix {
    private FastBufferFix() {
    }

    /** 점검용: 마지막에 한 일. */
    static volatile String lastForTest = "";

    static void run() {
        ClientConfig c = ClientConfig.get();
        if (!c.restoreFastBuffer || c.fastBufferRestored) {
            lastForTest = "off or done";
            return;
        }
        if (dhcompatLoaded()) {
            lastForTest = "dhcompat present";   // dhcompat 이 켤 때마다 다시 끈다: 빠진 뒤에 되돌린다
            return;
        }
        if (!ModList.get().isLoaded("immediatelyfast")) {
            lastForTest = "no ImmediatelyFast";
            return;
        }
        lastForTest = restore(FMLPaths.CONFIGDIR.get().resolve("immediatelyfast.json"));
        if (!lastForTest.startsWith("error")) {
            c.fastBufferRestored = true;
            c.save();
        }
    }

    static boolean dhcompatLoaded() {
        try {
            for (IModInfo m : ModList.get().getMods()) {
                String id = m.getModId().toLowerCase(Locale.ROOT);
                String file = "";
                try {
                    file = m.getOwningFile().getFile().getFileName().toLowerCase(Locale.ROOT);
                } catch (Throwable ignored) {
                }
                if (id.contains("dhcompat") || file.contains("dhcompat")) return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /** 설정 파일의 fast_buffer_upload 가 false 면 true 로 (다른 값은 그대로). 결과: "restored" · "already on" · "no file" · "error ...". */
    static String restore(Path p) {
        try {
            if (!Files.exists(p)) return "no file";
            JsonObject o = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!o.has("fast_buffer_upload") || o.get("fast_buffer_upload").getAsBoolean()) return "already on";
            o.addProperty("fast_buffer_upload", true);
            Files.writeString(p, new GsonBuilder().setPrettyPrinting().create().toJson(o), StandardCharsets.UTF_8);
            RtShop.LOGGER.info("[RT Shop] ImmediatelyFast fast_buffer_upload turned back on (it was switched off by dhcompat, which is no longer installed); takes effect from the next start");
            return "restored";
        } catch (Throwable t) {
            return "error " + t;
        }
    }
}

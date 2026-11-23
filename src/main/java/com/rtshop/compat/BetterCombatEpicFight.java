package com.rtshop.compat;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Better Combat 의 "이름으로 무기 속성 자동 부여"(fallback_compatibility)가 Epic Fight 계열 무기
 * (Nightfall 의 ruinsgreatsword·meen_spear·nf_claw 등)에도 claymore/spear 속성을 붙이면,
 * Better Combat 이 그 무기를 든 모습을 가져가서 Epic Fight 의 3D 무기 모델 대신 납작한 아이템 그림이 손에 그려졌다.
 * Epic Fight 계열 모드의 무기는 자동 부여에서 뺀다 (blacklist_item_id_regex 에 추가). 한 번 고치면 그대로 둔다.
 */
public final class BetterCombatEpicFight {
    private BetterCombatEpicFight() {
    }

    public static final String EXCLUDE = "^(efn|wom|epicfight|epicfightx|epic_fight_avalon):";

    public static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("bettercombat").resolve("fallback_compatibility.json");
    }

    public static final java.util.Set<String> NAMESPACES = java.util.Set.of("efn", "wom", "epicfight", "epicfightx", "epic_fight_avalon");
    /** 점검용: 마지막으로 뗀 무기 수 */
    public static volatile int dropped = -1;

    /** Better Combat 의 무기 속성 목록에서 Epic Fight 계열 무기를 뺀다 (목록 이름은 반사로 찾는다). */
    public static void dropFromRegistry() {
        try {
            Class<?> reg = Class.forName("net.bettercombat.logic.WeaponRegistry");
            int n = 0;
            for (java.lang.reflect.Field f : reg.getDeclaredFields()) {
                if (!java.lang.reflect.Modifier.isStatic(f.getModifiers()) || !java.util.Map.class.isAssignableFrom(f.getType())) continue;
                f.setAccessible(true);
                Object v = f.get(null);
                if (!(v instanceof java.util.Map<?, ?> m)) continue;
                java.util.Iterator<?> it = m.keySet().iterator();
                while (it.hasNext()) {
                    String k = String.valueOf(it.next());
                    int c = k.indexOf(':');
                    if (c > 0 && NAMESPACES.contains(k.substring(0, c))) {
                        it.remove();
                        n++;
                    }
                }
            }
            dropped = n;
            if (n > 0) RtShop.LOGGER.info("[rtshop] Better Combat 무기 속성에서 Epic Fight 계열 무기 {}개를 뺐습니다", n);
        } catch (Throwable t) {
            RtShop.LOGGER.debug("[rtshop] Better Combat 목록 정리 생략: {}", t.toString());
        }
    }

    /** 고쳤으면 true. */
    public static boolean patch() {
        Path f = file();
        try {
            if (!Files.exists(f)) return false;
            JsonObject o = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
            String re = o.has("blacklist_item_id_regex") ? o.get("blacklist_item_id_regex").getAsString() : "";
            if (re.contains(EXCLUDE)) return false;
            o.addProperty("blacklist_item_id_regex", re.isBlank() ? EXCLUDE : re + "|" + EXCLUDE);
            Files.writeString(f, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(o), StandardCharsets.UTF_8);
            RtShop.LOGGER.info("[rtshop] Better Combat 이 Epic Fight 무기(Nightfall 등)를 가져가지 않게 {} 를 고쳤습니다", f);
            return true;
        } catch (Exception e) {
            RtShop.LOGGER.warn("[rtshop] Better Combat 설정을 고치지 못했습니다: {}", e.toString());
            return false;
        }
    }
}

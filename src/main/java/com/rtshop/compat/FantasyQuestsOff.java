package com.rtshop.compat;

import com.rtshop.RtShop;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Fantasy Mounts 의 퀘스트(대화창이 저절로 뜨는 것)를 모든 서버에서 기본으로 끈다.
 * 세계마다 처음 한 번만 끄고 표시 파일을 남기므로, 관리자가 다시 켜면 그대로 둔다.
 * 그 모드의 서버 설정에서 이름에 quest 가 들어간 켜짐/꺼짐 값을 찾아 끈다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class FantasyQuestsOff {
    private FantasyQuestsOff() {
    }

    public static final String[] MOD_IDS = {"fantasy", "fantasymounts", "fantasy_mounts"};
    /** 마지막으로 끈 설정 이름 (점검용). */
    public static final List<String> LAST = new ArrayList<>();

    public static Path marker(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("rtshop_fantasy_quests_off");
    }

    /** 설치된 Fantasy Mounts 의 모드 id (이름이나 id 로 찾는다). 없으면 null. */
    public static String modId() {
        for (String id : MOD_IDS) if (ModList.get().isLoaded(id)) return id;
        for (net.minecraftforge.forgespi.language.IModInfo m : ModList.get().getMods())
            if (m.getDisplayName().toLowerCase(java.util.Locale.ROOT).contains("fantasy mounts")) return m.getModId();
        return null;
    }

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent event) {
        apply(event.getServer(), false);
    }

    /** force: 표시 파일이 있어도 다시 끈다 (점검용). 끈 설정 수. */
    public static int apply(MinecraftServer server, boolean force) {
        LAST.clear();
        String mod = modId();
        if (mod == null) return 0;
        Path mark = marker(server);
        if (!force && Files.exists(mark)) return 0;
        int n = 0;
        try {
            for (ModConfig cfg : ConfigTracker.INSTANCE.configSets().get(ModConfig.Type.SERVER)) {
                if (!cfg.getModId().equals(mod) && !cfg.getModId().startsWith("fantasy")) continue;
                if (!(cfg.getSpec() instanceof ForgeConfigSpec spec)) continue;
                n += walk(spec.getValues().valueMap(), "");
                if (n > 0) cfg.save();
            }
            for (ModConfig cfg : ConfigTracker.INSTANCE.configSets().get(ModConfig.Type.COMMON)) {
                if (!cfg.getModId().equals(mod) && !cfg.getModId().startsWith("fantasy")) continue;
                if (!(cfg.getSpec() instanceof ForgeConfigSpec spec)) continue;
                int m = walk(spec.getValues().valueMap(), "");
                if (m > 0) cfg.save();
                n += m;
            }
            Files.writeString(mark, "Fantasy Mounts 퀘스트를 기본으로 껐습니다: " + LAST + "\n다시 켜려면 그 모드의 서버 설정에서 켜면 됩니다 (이 파일이 있으면 다시 끄지 않습니다).\n");
            if (n > 0) RtShop.LOGGER.info("[rtshop] Fantasy Mounts 퀘스트를 기본으로 껐습니다: {}", LAST);
        } catch (Throwable t) {
            RtShop.LOGGER.warn("[rtshop] Fantasy Mounts 퀘스트 설정을 바꾸지 못했습니다: {}", t.toString());
        }
        return n;
    }

    @SuppressWarnings("unchecked")
    private static int walk(Map<String, Object> map, String path) {
        int n = 0;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            String p = path.isEmpty() ? e.getKey() : path + "." + e.getKey();
            Object v = e.getValue();
            if (v instanceof com.electronwill.nightconfig.core.UnmodifiableConfig sub) {
                n += walk(sub.valueMap(), p);
            } else if (v instanceof ForgeConfigSpec.BooleanValue b && p.toLowerCase().contains("quest")) {
                boolean want = e.getKey().toLowerCase().contains("disable"); // disableQuests 같은 이름은 거꾸로
                if (b.get() != want) {
                    b.set(want);
                    LAST.add(p);
                    n++;
                }
            }
        }
        return n;
    }
}

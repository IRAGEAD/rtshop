package com.rtshop.compat;

import com.rtshop.RtShop;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Integrated Dungeons and Structures(IDAS)가 바닐라 구조물(사막 피라미드 등)을 끄면 메인 스토리를 진행할 수 없다.
 * 서버가 켜질 때 IDAS 설정 중 "Disable Vanilla ..." 를 한 번 끈다 (표시 파일이 있으면 운영자의 선택을 존중해 다시 건드리지 않는다).
 * IDAS 구조물과 바닐라 구조물이 함께 생긴다. 새로 만들어지는 땅에만 적용된다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class IdasVanilla {
    private IdasVanilla() {
    }

    public static final List<String> LAST = new ArrayList<>();

    public static Path marker() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("idas_vanilla_on");
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAboutToStart(ServerAboutToStartEvent event) {
        apply(false);
    }

    /** force: 표시 파일이 있어도 다시 켠다 (점검용). 바꾼 설정 수. */
    public static int apply(boolean force) {
        LAST.clear();
        if (!ModList.get().isLoaded("idas")) return 0;
        if (!force && Files.exists(marker())) return 0;
        int n = 0;
        try {
            for (ModConfig.Type type : new ModConfig.Type[]{ModConfig.Type.COMMON, ModConfig.Type.SERVER}) {
                for (ModConfig cfg : ConfigTracker.INSTANCE.configSets().get(type)) {
                    if (!cfg.getModId().equals("idas")) continue;
                    if (!(cfg.getSpec() instanceof ForgeConfigSpec spec)) continue;
                    int m = walk(spec.getValues().valueMap(), "");
                    if (m > 0) cfg.save();
                    n += m;
                }
            }
            // ForgeConfigSpec 이 아닌 설정 방식이면 파일을 직접 고친다
            if (n == 0) n += editFile(FMLPaths.CONFIGDIR.get().resolve("idas.toml"));
            Files.createDirectories(marker().getParent());
            Files.writeString(marker(), "IDAS 가 끄던 바닐라 구조물을 다시 켰습니다: " + LAST
                    + "\n다시 끄려면 config/idas.toml 에서 true 로 바꾸면 됩니다 (이 파일이 있으면 RT Shop 은 다시 건드리지 않습니다).\n", StandardCharsets.UTF_8);
            if (n > 0) RtShop.LOGGER.info("[rtshop] IDAS 가 끄던 바닐라 구조물을 다시 켰습니다 (스토리 진행용): {}", LAST);
        } catch (Throwable t) {
            RtShop.LOGGER.warn("[rtshop] IDAS 설정을 바꾸지 못했습니다: {}", t.toString());
        }
        return n;
    }

    private static int editFile(Path f) throws Exception {
        if (!Files.exists(f)) return 0;
        List<String> lines = Files.readAllLines(f, StandardCharsets.UTF_8);
        int n = 0;
        for (int i = 0; i < lines.size(); i++) {
            String l = lines.get(i);
            String t = l.trim();
            if (t.toLowerCase().startsWith("\"disable vanilla") && t.endsWith("true")) {
                lines.set(i, l.substring(0, l.lastIndexOf("true")) + "false");
                LAST.add(t.substring(0, t.indexOf('=')).trim());
                n++;
            }
        }
        if (n > 0) Files.write(f, lines, StandardCharsets.UTF_8);
        return n;
    }

    private static int walk(Map<String, Object> map, String path) {
        int n = 0;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            String p = path.isEmpty() ? e.getKey() : path + "." + e.getKey();
            Object v = e.getValue();
            if (v instanceof com.electronwill.nightconfig.core.UnmodifiableConfig sub) {
                n += walk(sub.valueMap(), p);
            } else if (v instanceof ForgeConfigSpec.BooleanValue b && e.getKey().toLowerCase().startsWith("disable vanilla") && b.get()) {
                b.set(false);
                LAST.add(p);
                n++;
            }
        }
        return n;
    }
}

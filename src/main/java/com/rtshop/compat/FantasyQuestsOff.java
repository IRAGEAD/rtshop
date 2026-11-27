package com.rtshop.compat;

import com.rtshop.RtShop;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * Fantasy Mounts 의 퀘스트(그 모드의 NPC · 대화창 · 퀘스트 수첩)를 모든 세계에서 기본으로 끈다. 세계마다 처음 한 번만 끄므로, 관리자가 다시 켜면 그대로 둔다.
 * <p>
 * 3.74.0 (사용자 W-19 "fantasy mounts 모드의 월드 세팅 중 quests 세팅의 기본값을 off로"): 팩 11 의 Fantasy Mounts 2.3.3 은 Forge 설정(ForgeConfigSpec)을 쓰지 않아
 * 예전 방법(아래 {@link #apply(MinecraftServer, boolean)})으로는 아무것도 꺼지지 않았다 (모드팩 시험 기록 "fantasymounts []").
 * 그래서 그 모드의 서버 설정 파일 fantasy-server.toml 의 [quests] enabled 를 직접 false 로 고친다:
 * <ul>
 *   <li>공용 config/fantasy-server.toml 과 이미 있는 세계마다의 fantasy-server.toml (「World Settings」 창이 고치는 파일): 처음 한 번만 true → false.
 *       본 파일은 config/rtshop/compat_done.txt 에 적어 두고 다시 건드리지 않는다.</li>
 *   <li>새 세계: defaultconfigs/fantasy-server.toml 이 없으면 그 한 줄만 적어 둔다.</li>
 * </ul>
 * RT Shop 모드를 만들 때(그 모드가 파일을 읽기 전) 한 번, 서버가 켜진 뒤 한 번 더 (처음 켠 세계는 그때 생긴 파일을 고쳐 다음부터 꺼진다).
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class FantasyQuestsOff {
    private FantasyQuestsOff() {
    }

    public static final String[] MOD_IDS = {"fantasy", "fantasymounts", "fantasy_mounts"};
    public static final String FILE = "fantasy-server.toml";
    public static final String MARK = "fantasy-quests-off@1 ";
    /** 마지막으로 끈 설정 이름 (점검용). */
    public static final List<String> LAST = new ArrayList<>();
    /** 점검용: 마지막으로 파일을 고친 수 · 기본 파일을 새로 만들었는가 · 살펴본 파일. */
    public static volatile int lastPatched = -1;
    public static volatile boolean lastWroteDefault;
    public static final List<String> LAST_FILES = new ArrayList<>();

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

    // ------------------------------------------------------------------ 파일 (3.74.0)

    /** RT Shop 모드를 만들 때: 공용 설정 · 이미 있는 세계들의 파일을 고치고, 새 세계용 기본 파일을 둔다. */
    public static void applyFiles() {
        try {
            if (modId() == null) return;
        } catch (Throwable t) {
            return;
        }
        Path game = FMLPaths.GAMEDIR.get();
        List<Path> files = new ArrayList<>();
        files.add(FMLPaths.CONFIGDIR.get().resolve(FILE));
        try {
            if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
                files.addAll(worldFiles(game.resolve(levelName(game))));
            } else {
                Path saves = game.resolve("saves");
                if (Files.isDirectory(saves)) {
                    try (Stream<Path> st = Files.list(saves)) {
                        for (Path w : st.filter(Files::isDirectory).toList()) files.addAll(worldFiles(w));
                    }
                }
            }
        } catch (Throwable t) {
            RtShop.LOGGER.debug("[rtshop] Fantasy Mounts 세계 목록을 읽지 못했습니다: {}", t.toString());
        }
        int n = run(files, game.resolve("defaultconfigs").resolve(FILE), doneList());
        if (n > 0) RtShop.LOGGER.info("[rtshop] Fantasy Mounts 의 퀘스트를 기본으로 껐습니다 (설정 파일 {}개). 켜려면 그 모드의 World Settings 에서 다시 켜면 됩니다.", n);
    }

    static Path doneList() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("compat_done.txt");
    }

    /** 세계 폴더 안의 fantasy-server.toml (serverconfig 등, 세 단계 깊이까지). */
    public static List<Path> worldFiles(Path world) {
        List<Path> out = new ArrayList<>();
        if (!Files.isDirectory(world)) return out;
        out.add(world.resolve("serverconfig").resolve(FILE));
        try (Stream<Path> st = Files.find(world, 3, (p, a) -> a.isRegularFile() && p.getFileName().toString().equals(FILE)
                && !p.toString().contains("region"))) {
            st.forEach(p -> {
                if (!out.contains(p)) out.add(p);
            });
        } catch (IOException ignored) {
        }
        return out;
    }

    /** 파일들을 한 번씩 고치고, 기본 파일이 없으면 만든다. 고친 수 (+ 새로 만든 기본 파일 1). 점검에서 임시 폴더로도 부른다. */
    public static synchronized int run(List<Path> files, Path defaults, Path doneList) {
        List<String> done = new ArrayList<>();
        try {
            if (Files.exists(doneList)) done.addAll(Files.readAllLines(doneList, StandardCharsets.UTF_8));
        } catch (IOException ignored) {
        }
        int patched = 0;
        boolean changed = false;
        LAST_FILES.clear();
        for (Path f : files) {
            String key = MARK + f.toAbsolutePath().normalize();
            if (done.contains(key) || !Files.isRegularFile(f)) continue;
            try {
                boolean p = patchFile(f);
                if (p) patched++;
                LAST_FILES.add(f + (p ? " (끔)" : " (그대로)"));
                done.add(key);   // 고쳤든(이미 꺼져 있었든) 한 번 본 파일은 다시 보지 않는다: 그 뒤에 켠 것은 주인의 뜻
                changed = true;
            } catch (IOException e) {
                RtShop.LOGGER.warn("[rtshop] {} 를 고치지 못했습니다: {}", f, e.toString());
            }
        }
        boolean wrote = false;
        String defKey = MARK + "defaults " + defaults.toAbsolutePath().normalize();
        if (!done.contains(defKey)) {
            try {
                if (!Files.exists(defaults)) {
                    Files.createDirectories(defaults.getParent());
                    Files.writeString(defaults, "# RT Shop: Fantasy Mounts quests are off in new worlds (set enabled = true to turn them on).\n"
                            + "# RT Shop: 새 세계에서는 Fantasy Mounts 의 퀘스트를 꺼 둡니다 (켜려면 enabled = true).\n"
                            + "[quests]\n\tenabled = false\n", StandardCharsets.UTF_8);
                    wrote = true;
                }
                done.add(defKey);   // 지운 뒤에 다시 만들지는 않는다
                changed = true;
            } catch (IOException e) {
                RtShop.LOGGER.warn("[rtshop] {} 를 만들지 못했습니다: {}", defaults, e.toString());
            }
        }
        if (changed) {
            try {
                Files.createDirectories(doneList.getParent());
                Files.write(doneList, done, StandardCharsets.UTF_8);
            } catch (IOException ignored) {
            }
        }
        lastPatched = patched;
        lastWroteDefault = wrote;
        return patched + (wrote ? 1 : 0);
    }

    /** [quests] 칸 바로 아래(그 아래의 [quests.xxx] 칸은 아님)의 enabled = true 를 false 로. 고쳤으면 true. */
    public static boolean patchFile(Path f) throws IOException {
        List<String> lines = new ArrayList<>(Files.readAllLines(f, StandardCharsets.UTF_8));
        boolean in = false, changed = false;
        for (int i = 0; i < lines.size(); i++) {
            String t = lines.get(i).trim();
            if (t.startsWith("[")) {
                in = t.replace(" ", "").equals("[quests]");
                continue;
            }
            if (in && t.replace(" ", "").equals("enabled=true")) {
                lines.set(i, lines.get(i).replaceFirst("true", "false"));
                changed = true;
                break;
            }
        }
        if (changed) Files.write(f, lines, StandardCharsets.UTF_8);
        return changed;
    }

    /** 그 파일의 [quests] enabled 값 (점검용). 없으면 null. */
    public static Boolean questsEnabled(Path f) {
        try {
            boolean in = false;
            for (String line : Files.readAllLines(f, StandardCharsets.UTF_8)) {
                String t = line.trim().replace(" ", "");
                if (t.startsWith("[")) {
                    in = t.equals("[quests]");
                    continue;
                }
                if (in && t.startsWith("enabled=")) return t.startsWith("enabled=true");
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    /** 전용 서버의 세계 폴더 이름 (server.properties 의 level-name, 없으면 world). */
    static String levelName(Path game) {
        Properties p = new Properties();
        Path sp = game.resolve("server.properties");
        if (Files.exists(sp)) {
            try (var in = Files.newBufferedReader(sp, StandardCharsets.UTF_8)) {
                p.load(in);
            } catch (Exception ignored) {
            }
        }
        String n = p.getProperty("level-name", "world").trim();
        return n.isEmpty() ? "world" : n;
    }

    // ------------------------------------------------------------------ 서버가 켜진 뒤

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent event) {
        apply(event.getServer(), false);
        applyWorld(event.getServer());
    }

    /** 처음 켠 세계: 그 모드가 막 만든 파일(공용 · 이 세계)을 고쳐 둔다 (다음에 켤 때부터 꺼짐). 고친 수. */
    public static int applyWorld(MinecraftServer server) {
        try {
            if (modId() == null) return 0;
            List<Path> files = new ArrayList<>();
            files.add(FMLPaths.CONFIGDIR.get().resolve(FILE));
            files.addAll(worldFiles(server.getWorldPath(LevelResource.ROOT)));
            int n = run(files, FMLPaths.GAMEDIR.get().resolve("defaultconfigs").resolve(FILE), doneList());
            if (n > 0) RtShop.LOGGER.info("[rtshop] Fantasy Mounts 의 퀘스트 설정 파일 {}개를 껐습니다 (다음에 켤 때부터).", n);
            return n;
        } catch (Throwable t) {
            RtShop.LOGGER.debug("[rtshop] Fantasy Mounts 파일 점검 생략: {}", t.toString());
            return 0;
        }
    }

    /** (예전 방법) Forge 설정으로 등록된 퀘스트 켜짐/꺼짐 값을 끈다. force: 표시 파일이 있어도 다시. 끈 설정 수. */
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

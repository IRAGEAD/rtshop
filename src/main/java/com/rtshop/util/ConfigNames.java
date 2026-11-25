package com.rtshop.util;

import com.rtshop.RtShop;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * 마을 이름이 바뀌었을 때, 이미 저장돼 있는 설정 파일(config/rtshop 아래의 json) 안의 옛 이름을 새 이름으로 고친다.
 * 직접 고친 대사·퀘스트·문구 파일에도 옛 이름이 남지 않게 하려는 것. 고치기 전 파일은 config/rtshop/backup/names_... 에 둔다.
 * 퀘스트 폴더와 dialogue.json 은 "새 판으로 바꾸기"가 먼저 끝난 뒤에 따로 부른다 (먼저 고치면 직접 고친 파일로 보여 새 판을 못 받는다).
 */
public final class ConfigNames {
    private ConfigNames() {
    }

    public static final String TOWN = "베스탈리아", TOWN_EN = "Vestalia";
    /** 옛 이름(받침 있음): 조사까지 함께 고친다. */
    private static final String[][] PAIRS = {
            {"엠버헤이븐이라", "베스탈리아라"}, {"엠버헤이븐이야", "베스탈리아야"}, {"엠버헤이븐이에요", "베스탈리아예요"}, {"엠버헤이븐이었", "베스탈리아였"},
            {"엠버헤이븐을", "베스탈리아를"}, {"엠버헤이븐은", "베스탈리아는"}, {"엠버헤이븐이 ", "베스탈리아가 "}, {"엠버헤이븐으로", "베스탈리아로"},
            {"엠버헤이븐과", "베스탈리아와"}, {"엠버헤이븐", "베스탈리아"}, {"Emberhaven", "Vestalia"},
            {"골디아", "베스탈리아"}, {"Goldia", "Vestalia"}};

    public static String rename(String s) {
        if (s == null || (!s.contains("엠버헤이븐") && !s.contains("Emberhaven") && !s.contains("골디아") && !s.contains("Goldia"))) return s;
        for (String[] p : PAIRS) s = s.replace(p[0], p[1]);
        return s;
    }

    private static Path root() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop");
    }

    private static Path backupDir;

    /** 파일 하나: 옛 이름이 있으면 고쳐 쓴다. 고쳤으면 true. */
    public static synchronized boolean file(Path f) {
        try {
            if (!Files.isRegularFile(f) || !f.getFileName().toString().endsWith(".json") || Files.size(f) > 16L * 1024 * 1024) return false;
            String s = Files.readString(f, StandardCharsets.UTF_8);
            String r = rename(s);
            if (r.equals(s)) return false;
            if (backupDir == null) backupDir = root().resolve("backup").resolve("names_" + System.currentTimeMillis());
            Path b = backupDir.resolve(root().relativize(f.toAbsolutePath().startsWith(root().toAbsolutePath()) ? f : root().resolve(f.getFileName())));
            Files.createDirectories(b.getParent());
            Files.copy(f, b, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Files.writeString(f, r, StandardCharsets.UTF_8);
            RtShop.LOGGER.info("[rtshop] {} 안의 옛 마을 이름을 {} 로 고쳤습니다 (이전 파일: {}).", root().relativize(f), TOWN, b);
            return true;
        } catch (Exception e) {
            RtShop.LOGGER.warn("[rtshop] {} 의 마을 이름을 고치지 못했습니다: {}", f, e.toString());
            return false;
        }
    }

    /** 폴더 하나 (바로 아래 파일만). */
    public static int dir(Path d) {
        int n = 0;
        if (!Files.isDirectory(d)) return 0;
        try (Stream<Path> s = Files.list(d)) {
            List<Path> l = s.toList();
            for (Path f : l) if (file(f)) n++;
        } catch (Exception ignored) {
        }
        return n;
    }

    private static boolean swept;

    /** 모드가 켜질 때 한 번: 퀘스트 폴더·dialogue.json·백업을 뺀 나머지 설정 파일. */
    public static synchronized void sweep() {
        if (swept) return;
        swept = true;
        Path r = root();
        if (!Files.isDirectory(r)) return;
        try (Stream<Path> s = Files.walk(r, 3)) {
            List<Path> l = s.filter(Files::isRegularFile).toList();
            for (Path f : l) {
                Path rel = r.relativize(f);
                String first = rel.getName(0).toString();
                if (first.equals("backup") || first.equals("quests") || rel.toString().equals("dialogue.json")) continue;
                file(f);
            }
        } catch (Exception e) {
            RtShop.LOGGER.warn("[rtshop] 설정 파일의 마을 이름을 살피지 못했습니다: {}", e.toString());
        }
    }
}

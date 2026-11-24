package com.rtshop.story;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * config/rtshop/story.json : 보스 등장 연출(이름·별칭·테마·색·소리·한마디)과 보스의 이야기(그 보스가 떨어뜨린 물건의 설명).
 * 새 판의 기본값에 있는 보스는 파일에 없으면 자동으로 더해진다 (이미 적힌 것은 그대로).
 */
public final class StoryConfig {
    public static final class Intro {
        public String name = "";
        public String epithet = "";
        public String theme = "ember";
        public String color = "FF5A14";
        public String sound = "";
        public String quote = "";
        public int shake = 2;
        public final List<String> lore = new ArrayList<>();

        JsonObject toJson() {
            JsonObject o = new JsonObject();
            o.addProperty("name", name);
            o.addProperty("epithet", epithet);
            o.addProperty("theme", theme);
            o.addProperty("color", color);
            o.addProperty("sound", sound);
            o.addProperty("quote", quote);
            o.addProperty("shake", shake);
            JsonArray l = new JsonArray();
            for (String s : lore) l.add(s);
            o.add("lore", l);
            return o;
        }

        static Intro parse(JsonObject o) {
            Intro i = new Intro();
            if (o.has("name")) i.name = o.get("name").getAsString();
            if (o.has("epithet")) i.epithet = o.get("epithet").getAsString();
            if (o.has("theme")) i.theme = o.get("theme").getAsString();
            if (o.has("color")) i.color = o.get("color").getAsString();
            if (o.has("sound")) i.sound = o.get("sound").getAsString();
            if (o.has("quote")) i.quote = o.get("quote").getAsString();
            if (o.has("shake")) i.shake = o.get("shake").getAsInt();
            if (o.has("lore")) for (JsonElement e : o.getAsJsonArray("lore")) i.lore.add(e.getAsString());
            return i;
        }
    }

    public boolean introEnabled = true;
    /** 이 거리(블록) 안에 플레이어가 들어오면 등장 연출을 한다. */
    public int introRadius = 36;
    /** 연출 동안 플레이어가 받는 피해를 막는 시간(초). */
    public int introShieldSeconds = 5;
    public final Map<String, Intro> intros = new LinkedHashMap<>();

    private static StoryConfig current;

    public static synchronized StoryConfig get() {
        if (current == null) current = load();
        return current;
    }

    public static synchronized void reload() {
        current = null;
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("story.json");
    }

    private static Intro in(String name, String epithet, String theme, String color, String sound, String quote, int shake, String... lore) {
        Intro i = new Intro();
        i.name = name;
        i.epithet = epithet;
        i.theme = theme;
        i.color = color;
        i.sound = sound;
        i.quote = quote;
        i.shake = shake;
        i.lore.addAll(List.of(lore));
        return i;
    }

    public static Map<String, Intro> defaults() {
        Map<String, Intro> m = new LinkedHashMap<>();
        m.put("mowziesmobs:ferrous_wroughtnaut", in("강철의 기사", "천 년의 파수꾼", "iron", "B8642A",
                "minecraft:block.anvil.land,minecraft:entity.iron_golem.repair", "문을… 지켜라…", 3,
                "불꽃 기사단 제2대의 부관 호르스.",
                "단장 아르덴이 남긴 \"내가 돌아올 때까지 문을 지켜라\"는 말을",
                "천 년 동안 지켰다. 갑옷이 녹슬고 이름을 잊어도,",
                "문 앞을 떠난 적은 없다."));
        m.put("mowziesmobs:frostmaw", in("프로스트모", "얼음을 품은 거인", "frost", "9FD8FF",
                "minecraft:entity.player.hurt_freeze,minecraft:block.glass.break", "", 2,
                "북쪽 서리 왕국의 아이들이 기르던 거인.",
                "왕국이 저주로 얼어붙던 날, 아이들이 마지막으로 맡긴",
                "얼음 결정을 품에 안고 잠들었다.",
                "그 결정은 불을 잠재우는 차가움을 품고 있다."));
        m.put("mowziesmobs:sculptor", in("조각가 통비", "산꼭대기의 석공", "stone", "C8C0B0",
                "minecraft:block.grindstone.use,minecraft:block.stone.break", "봉인은… 일곱이 아니다.", 2,
                "일곱 봉인을 돌에 새겨 남긴 석공.",
                "여덟 번째 자리를 새기려는 순간 스승의 손이 멈췄다.",
                "그는 그 빈칸의 이유를 여전히 산꼭대기에서 묻고 있다."));
        m.put("mowziesmobs:umvuthi", in("움부티", "두 번째 태양의 사제", "sun", "FFB040",
                "minecraft:block.beacon.activate,minecraft:entity.blaze.shoot", "보아라, 남쪽 하늘의 태양을!", 2,
                "남쪽 부족의 제사장.",
                "어느 밤 하늘에 두 번째 태양이 떠오른 뒤로,",
                "그 빛을 신으로 섬기며 점점 타들어 갔다."));
        m.put("aquamirae:captain_cornelia", in("코르넬리아", "얼어붙은 바다의 선장", "ghost", "7FD8C8",
                "minecraft:entity.elder_guardian.curse,minecraft:ambient.cave", "내 선원들은… 어디 있지?", 2,
                "일곱 바다에서 가장 빠른 배를 몰던 해적.",
                "회색 망토의 학자가 판 항로를 따라 눈을 훔치러 갔고,",
                "돌아오는 길에 바다와 함께 얼어붙었다.",
                "선원들의 이름을 아직도 부른다."));
        m.put("cataclysm:netherite_monstrosity", in("네더라이트 몬스트로시티", "멈추지 못한 거인", "ember", "FF7A20",
                "minecraft:block.anvil.land,minecraft:block.lava.extinguish", "", 3,
                "피글린 대장장이들이 불꽃 경기장에서 훔친 영혼의 불로 두들겨 만든 거인.",
                "주인들은 모두 그 불에 타 죽었고, 그는 멈출 명령을 듣지 못했다."));
        m.put("cataclysm:ancient_remnant", in("고대 잔해", "왕을 기다리는 짐승", "sand", "E8C07A",
                "minecraft:entity.husk.ambient,minecraft:block.sand.break", "", 3,
                "사막 왕조의 마지막 파라오가 기르던 충성스러운 짐승.",
                "봉인을 만든 일곱 중 하나였던 주인을 기다리며,",
                "목걸이를 든 자에게만 고개를 든다."));
        m.put("cataclysm:the_harbinger", in("하빈저", "끝을 본 기계", "mech", "7FE0FF",
                "minecraft:block.beacon.power_select,minecraft:entity.wither.spawn", "마지막 문을 여는 것은 견습이다.", 3,
                "일리저들이 \"세계의 끝을 미리 보려고\" 만든 기계.",
                "마지막 예언을 내놓은 뒤 만든 자들에게 감춰졌다.",
                "예언의 마지막 줄: \"마지막 문을 여는 것은 견습이다.\""));
        m.put("cataclysm:the_leviathan", in("리바이어던", "심연의 수호자", "abyss", "2E7FD8",
                "minecraft:entity.elder_guardian.ambient,minecraft:entity.elder_guardian.curse", "", 3,
                "침몰 도시의 딥링들이 천 년 동안 섬긴 심연의 수호자.",
                "눈을 도둑맞은 날 이후 눈을 감지 못한다."));
        m.put("cataclysm:ender_guardian", in("엔더 가디언", "빈 왕좌의 문지기", "void", "A06BFF",
                "minecraft:block.end_portal.spawn,minecraft:entity.enderman.teleport", "", 2,
                "엔드 성채의 문지기.",
                "여덟 개의 왕좌 중 빈 자리를 지키라는 명령을 받았지만,",
                "그 자리의 주인이 누구였는지는 알지 못한다."));
        m.put("cataclysm:maledictus", in("말레딕투스", "약속을 기다리는 왕", "curse", "6A8A90",
                "minecraft:block.bell.use,minecraft:entity.wither_skeleton.ambient", "약속은… 지켜졌는가.", 3,
                "서리 왕국의 왕.",
                "회색 망토의 학자가 약속한 \"끝나지 않는 삶\"을 대가로 봉인을 내주었고,",
                "왕국은 저주로 얼어붙었다.",
                "그는 아직 약속을 지킬 사람을 기다린다."));
        m.put("block_factorys_bosses:yeti", in("스코르", "덮지 않는 자", "frost", "BFE6FF",
                "minecraft:entity.polar_bear.warning,minecraft:block.snow.break", "", 2,
                "설원의 장의사.",
                "눈에 덮이면 잊힌다고 믿어, 얼어 죽은 이들을 굴로 데려와 세워 둔다.",
                "그에게 명패를 거두러 온 자는 도둑이다."));
        m.put("block_factorys_bosses:sandworm", in("시로크", "물을 부르는 것", "stone", "D8B870",
                "minecraft:block.sand.break,minecraft:entity.ravager.roar", "", 2,
                "사막의 왕 아므라트가 수로를 파게 한 짐승.",
                "왕국이 모래에 묻힌 뒤에도 도면대로 파 내려간다.",
                "마지막 구간, 왕의 정원에는 천 년째 닿지 못했다."));
        m.put("block_factorys_bosses:infernal_dragon", in("애쉬로드", "재가 될 때까지 따르는 자", "ember", "FF6A30",
                "minecraft:entity.ender_dragon.growl,minecraft:item.firecharge.use", "", 3,
                "불꽃 기사단장 아르덴이 타던 용.",
                "주인이 \"기다려라\" 하고 떠난 탑을 지킨다.",
                "천 년 동안 숨결로 안장의 그을음을 태워 냈다. 언제든 탈 수 있게."));
        m.put("block_factorys_bosses:underworld_knight", in("헬바르", "저승의 문턱을 지키는 이", "curse", "58D0E0",
                "minecraft:entity.wither_skeleton.ambient,minecraft:block.soul_sand.break", "…문은, 내가 지킨다.", 3,
                "불꽃 기사단의 부단장.",
                "세 대장이 머리를 내놓아 하나의 무기가 되는 「세 머리의 서약」에",
                "혼자 반대했다. \"동료를 무기로 만들 수는 없다.\"",
                "기각된 뒤로 천 년, 서약이 맺어진 자리의 문을 지킨다."));
        m.put("cataclysm:ignis", in("이그니스", "화염의 군주", "ember", "FF5A14",
                "minecraft:entity.wither.spawn,minecraft:item.firecharge.use,minecraft:entity.blaze.ambient", "또 왔구나, 견습이여.", 3,
                "불꽃 기사단장 아르덴.",
                "최초의 불꽃을 스스로 삼켜 일곱 봉인을 하나로 묶었다.",
                "천 년 동안 타면서도, 꿈속에서는 언제나 다음 견습을 기다렸다."));
        m.put("cataclysm:scylla", in("스킬라", "폭풍의 여제", "storm", "9FC8FF",
                "minecraft:entity.lightning_bolt.thunder,minecraft:entity.elder_guardian.curse", "누가 내 이름을 기억하지?", 3,
                "여덟 번째 봉인을 맡았던 폭풍의 여제.",
                "다른 일곱이 그녀의 이름을 연대기에서 지웠다.",
                "왜 지웠는지는 그녀를 만난 사람만 알 수 있다."));
        // 두 번째 숨은 보스: 숲속 저택 아래 (The Illusioner 모드). 모드에 자체 제목·음악이 있어 소리는 비워 둔다.
        m.put("illusioner:illusioner", in("환술사", "거짓 연대기의 필경사", "illusion", "B48CFF",
                "", "이 이야기는… 내가 쓴 것이다.", 1,
                "수도회의 연대기를 고쳐 쓴 환술사 벨로크.",
                "장로 에르윈의 부탁으로 진실을 거울 뒤에 감추고",
                "거짓 한 장마다 금화 한 닢을 받았다.",
                "진짜 연대기의 마지막 장은 지금도 그의 거울 속에 있다."));
        return m;
    }

    private static StoryConfig load() {
        StoryConfig c = new StoryConfig();
        Path f = file();
        boolean write = false;
        try {
            if (Files.exists(f)) {
                JsonObject o = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
                if (o.has("introEnabled")) c.introEnabled = o.get("introEnabled").getAsBoolean();
                if (o.has("introRadius")) c.introRadius = Math.max(8, o.get("introRadius").getAsInt());
                if (o.has("introShieldSeconds")) c.introShieldSeconds = Math.max(0, o.get("introShieldSeconds").getAsInt());
                if (o.has("bosses")) {
                    for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("bosses").entrySet()) {
                        if (e.getValue().isJsonObject()) c.intros.put(e.getKey(), Intro.parse(e.getValue().getAsJsonObject()));
                    }
                }
            } else {
                write = true;
            }
            for (Map.Entry<String, Intro> e : defaults().entrySet()) {
                if (!c.intros.containsKey(e.getKey())) {
                    c.intros.put(e.getKey(), e.getValue());
                    write = true;
                }
            }
            if (write) {
                Files.createDirectories(f.getParent());
                Files.writeString(f, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(c.toJson()), StandardCharsets.UTF_8);
            }
        } catch (Exception ex) {
            RtShop.LOGGER.warn("[rtshop] story.json 을 읽지 못했습니다: {}", ex.toString());
            if (c.intros.isEmpty()) c.intros.putAll(defaults());
        }
        return c;
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("_help", "보스 등장 연출과 보스의 이야기. bosses 의 키는 몹 ID. theme: ember|frost|sand|abyss|mech|void|curse|storm|iron|sun|ghost|stone|illusion · color: 16진수 색 · sound: 소리 ID (쉼표로 여러 개) · quote: 등장 때 한마디 · lore: 그 보스가 떨어뜨린 물건에 붙는 이야기. 저장한 뒤 /rtshop reload");
        o.addProperty("introEnabled", introEnabled);
        o.addProperty("introRadius", introRadius);
        o.addProperty("introShieldSeconds", introShieldSeconds);
        JsonObject b = new JsonObject();
        for (Map.Entry<String, Intro> e : intros.entrySet()) b.add(e.getKey(), e.getValue().toJson());
        o.add("bosses", b);
        return o;
    }
}

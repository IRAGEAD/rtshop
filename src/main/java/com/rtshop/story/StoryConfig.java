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

    /** 기본 보스 이야기의 판: 이야기를 다시 쓸 때 올린다. 낮은 판의 설정 파일은 이전 것을 backup 에 두고 기본 보스들의 이야기를 새로 쓴다. */
    public static final int VERSION = 3;

    public static Map<String, Intro> defaults() {
        Map<String, Intro> m = new LinkedHashMap<>();
        m.put("mowziesmobs:ferrous_wroughtnaut", in("강철의 기사", "천 년의 파수꾼", "iron", "B8642A",
                "minecraft:block.anvil.land,minecraft:entity.iron_golem.repair", "문을… 지켜라…", 3,
                "불꽃 기사단장 아르덴의 종자 호르스.",
                "\"내가 돌아올 때까지 문을 지켜라\"는 말을 천 년 동안 지켰다.",
                "갑옷이 녹슬고 제 이름을 잊어도 문 앞을 떠나지 않았다.",
                "주군이 탈 등을 비워 둔 채로."));
        m.put("mowziesmobs:frostmaw", in("프로스트모", "얼음을 품은 거인", "frost", "9FD8FF",
                "minecraft:entity.player.hurt_freeze,minecraft:block.glass.break", "", 2,
                "북쪽 서리 왕국의 아이들이 기르던 거인.",
                "왕국이 얼어붙던 날, 아이들이 마지막으로 맡긴",
                "얼음 결정을 품에 안고 천 년을 잤다.",
                "그 결정은 불을 잠재우는 차가움을 품고 있다."));
        m.put("mowziesmobs:sculptor", in("조각가 통비", "산꼭대기의 석공", "stone", "C8C0B0",
                "minecraft:block.grindstone.use,minecraft:block.stone.break", "봉인은… 일곱이 아니다.", 2,
                "봉인의 돌을 깎아 남긴 석공.",
                "여덟째 돌은 새기다 말았다. 새길 이름이 주어지지 않았다.",
                "버려진 그 돌이 어디로 갔는지, 산꼭대기에서 아직 묻고 있다."));
        m.put("mowziesmobs:umvuthi", in("태양조", "두 번째 태양의 사제", "sun", "FFB040",
                "minecraft:block.beacon.activate,minecraft:entity.blaze.shoot", "보아라, 남쪽 하늘의 태양을!", 2,
                "가면 부족이 섬기는 두 번째 태양의 사제.",
                "스무 해 전 어느 밤부터 가면을 벗은 적이 없다.",
                "가면 아래의 얼굴을 아는 사람은 하나뿐이다. 인원을 세 번 세는 사람."));
        m.put("fdbosses:chesed", in("케세드", "이름을 받지 못한 돌", "curse", "6FD8FF",
                "", "", 1,
                "버려진 여덟째 봉인석. 첫 굴 밑에 묻혔다.",
                "이름을 새길 자리 여덟 글자는 비어 있고, 새기다 만 한 단어만 남았다. 자비.",
                "스무 해 전 원정대가 처음 사람을 잃은 자리다. 투구 둘이 등을 맞대고 서 있었다."));
        m.put("aquamirae:maze_mother", in("미로의 어머니", "삼킨 목소리로 우는 것", "abyss", "5FB8D8",
                "minecraft:entity.elder_guardian.ambient", "", 2,
                "바다가 얼던 밤 얼음에 갇혀 굶주린 심해의 물고기.",
                "삼킨 사람의 목소리로 다음 사람을 부른다.",
                "쓰러지며 삼킨 목소리를 모두 돌려주었다."));
        m.put("block_factorys_bosses:kraken", in("네라키스", "배를 품는 것", "abyss", "3A6FA8",
                "minecraft:entity.elder_guardian.curse,minecraft:block.wood.break", "", 2,
                "배를 부수지 않고 품어 가라앉히는 것.",
                "서른 해 전 상선 아침별과 선원 서른둘을 가져갔다.",
                "바다가 얼 때 배들의 무덤 밑에 갇혔고, 아침별을 아직 품고 있었다."));
        m.put("fdbosses:malkuth", in("말쿠트", "베푸는 왕", "frost", "FF8A50",
                "", "", 1,
                "달라는 것을 다 주고 가라앉은 나라의 왕.",
                "거절할 줄을 몰랐다. 방파제까지 내주었다.",
                "선장의 청 \"내 배에서는 아무도 죽지 않게\"도 들어주었다. 그래서 바다가 얼었다.",
                "마지막으로 준 것을 제 손으로 거두며, 미안하다고 전하라 했다."));
        m.put("aquamirae:captain_cornelia", in("코넬리아 선장", "얼어붙은 바다의 선장", "ghost", "9FD8FF",
                "minecraft:entity.elder_guardian.curse,minecraft:ambient.cave", "삼백. 전원 승선. 사망 없음.", 2,
                "스물둘에 첫 배와 선원 서른둘을 잃고 혼자 살아남은 선장.",
                "그 뒤로 법은 하나였다. 내 배에서는 아무도 죽지 않는다.",
                "그 법을 베푸는 왕에게 청했고, 배는 삼백 명을 태운 채 얼음에 멈췄다.",
                "열다섯 해 동안 날마다 명부를 덧썼다. 한 줄도 지워지지 않게."));
        m.put("cataclysm:netherite_monstrosity", in("네더라이트 몬스트로시티", "이름을 받지 못한 아들", "ember", "FF7A20",
                "minecraft:block.anvil.land,minecraft:block.lava.extinguish", "", 3,
                "화로의 왕 브라간의 주문으로 대장장이 백 명이 벼린 거인.",
                "왕은 만드는 법이 새어 나가지 않게 대장간 문을 밖에서 잠갔다.",
                "천 년 동안 문을 두드렸다. 둘, 쉬고, 셋.",
                "대장장이들의 말로 \"밥 먹어라\"다."));
        m.put("cataclysm:ancient_remnant", in("고대 잔해", "정원의 문지기", "sand", "E8C07A",
                "minecraft:entity.husk.ambient,minecraft:block.sand.break", "", 3,
                "사막의 왕 아므라트가 타던 짐승.",
                "왕은 \"물소리가 날 때까지 아무도 들이지 말라\"고만 했다.",
                "물은 오지 않는다는 말은 끝내 하지 못했다.",
                "천 년 뒤 물이 왔고, 문지기는 약속대로 문을 열었다."));
        m.put("cataclysm:the_harbinger", in("하빈저", "끝을 본 기계", "mech", "7FE0FF",
                "minecraft:block.beacon.power_select,minecraft:entity.wither.spawn", "셈 완료.", 3,
                "톱니의 여왕 오르델의 도시가 무엇이든 묻던 셈 기계.",
                "여왕이 \"끝이 언제냐\"고 묻자 열두 시라고 답했다.",
                "여왕은 온 도시의 시계를 한 분 전에 세웠다.",
                "천 년 뒤 깨어나 밀린 답을 한꺼번에 뱉었다. 늦은 답에는 미안하다고 덧찍었다."));
        m.put("cataclysm:the_leviathan", in("리바이어던", "자장가를 듣는 것", "abyss", "2E7FD8",
                "minecraft:entity.elder_guardian.ambient,minecraft:entity.elder_guardian.curse", "", 3,
                "심연의 여왕 네리스가 기르던 짐승. 아이들이 그 등에 올라타 놀았다.",
                "눈을 품은 뒤로는 깨면 도시가 무너지기에, 사제들이 자장가를 불렀다.",
                "마흔 대를 이어, 교대 없이, 천 년.",
                "여덟 마디를 잠결에 따라 부른다."));
        m.put("cataclysm:ender_guardian", in("엔더 가디언", "빈 왕좌의 문지기", "void", "A06BFF",
                "minecraft:block.end_portal.spawn,minecraft:entity.enderman.teleport", "", 2,
                "침묵의 왕 벨라스가 빈자리 앞에 세운 문지기.",
                "묻는 자를 막으라는 명을 받았다.",
                "무엇을 묻지 못하게 하는지는 듣지 못했다.",
                "쓰러지며 그 자리를 가리켰다."));
        m.put("fdbosses:geburah", in("게부라", "기울지 않는 저울", "void", "E8D8A0",
                "", "", 1,
                "여덟 왕국의 다툼을 재던 저울. 얼굴을 보지 않아 믿음을 받았다.",
                "왕들이 물었다. 한 이름과 세상, 어느 쪽이 무거운가.",
                "저울은 기울지 않았고, 천 년 동안 같은 물음을 다시 달았다.",
                "마지막 판결: 물은 자들이 답할 것."));
        m.put("cataclysm:maledictus", in("말레딕투스", "열여덟 살에 멈춘 왕", "curse", "A0C8FF",
                "minecraft:block.bell.use,minecraft:entity.wither_skeleton.ambient", "…형?", 3,
                "열여섯에 왕이 되고 열여덟에 여덟 왕의 회의에 앉은 서리의 왕.",
                "눈을 남에게 맡기지 않고 제 손으로 감옥 문을 잠갔다. 간수는 없다.",
                "취사병도, 북 치던 아이도, 글선생도 따라 들어갔다.",
                "혼자 두면 안 될 것 같아서."));
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
                "minecraft:entity.wither.spawn,minecraft:item.firecharge.use,minecraft:entity.blaze.ambient", "오지 마라, 불씨를 쥔 자들아.", 3,
                "불꽃 기사단장 아르덴.",
                "최초의 불꽃의 심지를 제 몸에 받아 천 년 동안 가두었다.",
                "꿈속에서는 언제나 같은 말을 했다. 아직은, 오지 마라."));
        m.put("cataclysm:scylla", in("스킬라", "마중 나간 사람", "storm", "9FC8FF",
                "minecraft:entity.lightning_bolt.thunder,minecraft:entity.elder_guardian.curse", "…너는 그 사람이 아니구나.", 3,
                "새벽의 섬 함대의 제독, 폭풍의 여제.",
                "누군가를 회의에 배로 데려다주며 \"돌아올 때 마중 나오마\" 했다.",
                "그 사람은 돌아오지 않았고, 누구였는지는 그녀도 잊었다.",
                "천 년 동안 지나가는 모든 배를 폭풍으로 감싸 들여다본다."));
        // 두 번째 숨은 보스: 숲속 저택 아래 (The Illusioner 모드). 모드에 자체 제목·음악이 있어 소리는 비워 둔다.
        m.put("illusioner:illusioner", in("환술사", "거울 속의 필경사", "illusion", "B48CFF",
                "", "이 이야기는… 내가 고쳐 썼다.", 1,
                "수도회의 연대기를 고쳐 쓴 필경사 벨로크.",
                "\"일곱이 쫓아냈다\"고 썼다. 미워할 대상이 있어야 견디는 아이가 있어서.",
                "다정한 거짓말이었다.",
                "고치기 전의 원본은 지금도 그의 거울 속에 있다."));
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
                int v = o.has("version") ? o.get("version").getAsInt() : 1;
                if (v < VERSION) { // 이야기가 다시 쓰였다: 이전 파일을 남기고 기본 보스들의 이야기를 새 판으로
                    Path bk = FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("backup").resolve("story_v" + v + "_" + System.currentTimeMillis() + ".json");
                    Files.createDirectories(bk.getParent());
                    Files.copy(f, bk, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    c.intros.putAll(defaults());
                    write = true;
                    RtShop.LOGGER.info("[rtshop] 보스 이야기(story.json)를 새 판으로 바꿨습니다 (이전 파일: {}).", bk);
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
        o.addProperty("version", VERSION);
        o.addProperty("introEnabled", introEnabled);
        o.addProperty("introRadius", introRadius);
        o.addProperty("introShieldSeconds", introShieldSeconds);
        JsonObject b = new JsonObject();
        for (Map.Entry<String, Intro> e : intros.entrySet()) b.add(e.getKey(), e.getValue().toJson());
        o.add("bosses", b);
        return o;
    }
}

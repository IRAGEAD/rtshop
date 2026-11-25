package com.rtshop.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/** 퀘스트 하나의 정의. config/rtshop/quests/*.json 에서 읽는다. */
public final class QuestDef {
    public enum Type {
        /** 아이템을 NPC 에게 건넨다 (완료 때 인벤토리에서 가져간다). */
        DELIVER,
        /** 몬스터를 처치한다. */
        KILL,
        /** 지정한 장소 근처에 간다. */
        VISIT,
        /** 지정한 NPC 와 대화한다. */
        TALK,
        /** Gold 를 낸다 (완료 때 소지금에서 가져간다). */
        PAY,
        /** 구조물 안에 들어간다. target: 구조물 ID, #태그, 또는 "모드:*". */
        STRUCTURE,
        /** 발전과제(업적)를 이룬다. target: 발전과제 ID. */
        ADVANCEMENT,
        /** 차원에 들어간다. target: 차원 ID. */
        DIMENSION,
        /** 표식을 갖고 있다 (예: abyss_floor_2 = 나선의 심연 2층을 깬 적이 있다). target: 표식. */
        FLAG,
        /**
         * 장소 조사: 흔적에 다가가 살펴본다. target: 흔적 ID (영문 소문자·숫자·_). at: 흔적이 나타날 구조물 (STRUCTURE 목표와 같은 형식, 비우면 어디서나),
         * shape: 흔적의 모양, lines: 살펴볼 때 나오는 글. target 을 "id@구조물" 로 적어도 된다.
         */
        INSPECT;

        static Type parse(String s) {
            for (Type t : values()) {
                if (t.name().equalsIgnoreCase(s)) {
                    return t;
                }
            }
            return null;
        }
    }

    public static final class Objective {
        public Type type;
        /** DELIVER: 아이템 ID 또는 #태그 / KILL: 엔티티 ID, "*" = 모든 몬스터 / TALK: NPC 키 / VISIT: 차원 ID */
        public String target = "";
        /** 필요한 수량 (PAY 는 Gold 액수). */
        public int count = 1;
        public int x, y, z, radius = 8;
        /** 화면에 보일 설명 (비우면 자동으로 만든다). */
        public String text = "";
        /** 모드 조건: "" = 항상, "모드ID" = 그 모드가 있을 때만, "!모드ID" = 그 모드가 없을 때만. */
        public String mod = "";
        /** TALK: 그 NPC 에게 말을 걸면 대화 상자로 먼저 나오는 장면 ("이름: 대사" 또는 대사). 없으면 바로 채워진다. */
        public com.google.gson.JsonArray lines;
        /** INSPECT: 흔적이 나타날 구조물 (ID, #태그, "모드:*"). 비우면 퀘스트를 받은 뒤 어디서나 곁에 나타난다. */
        public String at = "";
        /** INSPECT: 흔적의 모양 (circle 마법진 · mural 벽화 · scrawl 낙서 · board 명령판 · camp 야영지 · relic 유품 · bones 유해 · book 기록). */
        public String shape = "";
        /** INSPECT: 살펴보는 대신 "하는 일"의 이름 (예: 놓는다, 붓는다, 건다, 불을 붙인다). 비우면 살펴보기. */
        public String verb = "";
        /** INSPECT: 그 일을 하는 데 드는 아이템 (끝낼 때 가져간다). 비우면 필요 없음. */
        public String use = "";
        public int useCount = 1;
        /** INSPECT: 이 흔적(ID)을 먼저 끝내야 나타난다 (발자국·잉크 점처럼 하나씩 이어지는 흔적). */
        public String after = "";
    }

    public static final String[] SHAPES = {"circle", "mural", "scrawl", "board", "camp", "relic", "bones", "book"};

    public static final class ItemReward {
        public String item;
        public int count = 1;
        /** 모드 조건 (Objective.mod 와 같은 형식). */
        public String mod = "";
    }

    /** 모드 조건을 만족하는지 ("" = 항상, "x" = x 가 있을 때, "!x" = x 가 없을 때). */
    public static boolean modOk(String cond) {
        if (cond == null || cond.isBlank()) {
            return true;
        }
        String c = cond.trim();
        boolean not = c.startsWith("!");
        String id = not ? c.substring(1).trim() : c;
        if (id.isEmpty()) {
            return true;
        }
        boolean loaded = net.minecraftforge.fml.ModList.get() != null && net.minecraftforge.fml.ModList.get().isLoaded(id);
        return not != loaded;
    }

    public static final class Choice {
        public String text = "";
        /** 이 선택지를 고르면 얻는 표식. 숨겨진 퀘스트나 직업 해금 조건으로 쓴다. */
        public final List<String> flags = new ArrayList<>();
    }

    public String id;
    /** 같은 줄기(연작) 이름. 목록에서 묶어 보여 주는 용도. */
    public String chain = "";
    public String title = "";
    /** 받기 전에 보이는 이야기. */
    public String intro = "";
    /** 진행 중에 보이는 안내. */
    public String progress = "";
    /** 완료할 때 보이는 이야기. */
    public String done = "";
    /** 퀘스트를 주는 NPC 키 (예: quest_guide, bank, shop_farmer). */
    public String giver = "";
    /** 완료를 받는 NPC 키 (비우면 giver 와 같음). */
    public String turnIn = "";
    public final List<String> requires = new ArrayList<>();
    public final List<String> requiresFlags = new ArrayList<>();
    public final List<String> blockedByFlags = new ArrayList<>();
    public final List<Objective> objectives = new ArrayList<>();
    public long rewardGold;
    public final List<ItemReward> rewardItems = new ArrayList<>();
    public final List<String> rewardFlags = new ArrayList<>();
    public final List<Choice> choices = new ArrayList<>();
    /** 목록 정렬용 (작을수록 위). */
    public int order;
    /** 받을 때 / 끝낼 때 보여 줄 연출 (StoryPacket CINEMA 형식). 없으면 null. */
    public JsonObject sceneAccept, sceneDone;
    /** 이 퀘스트를 끝낸 뒤 침대에서 잠들 때 보이는 꿈 장면 (sceneDone 과 따로 둔다). */
    public JsonObject sceneDream;
    /** 퀘스트 전체의 모드 조건 ("" = 항상). 맞지 않으면 퀘스트를 아예 불러오지 않는다. */
    public String mod = "";
    /**
     * 동행: {"npc": NPC ID, "at": 나타날 구조물(비우면 받자마자), "style": none·melee·bolt,
     * "lines": {"arrive":[..], "fight":[..], "low":[..], "idle":[..], "spot":{흔적 ID: 대사}, "done":[..], "talk":[..]}}. 없으면 null. (말하는 규칙은 Companions 참고)
     */
    public JsonObject companion;

    public String turnInNpc() {
        return turnIn == null || turnIn.isEmpty() ? giver : turnIn;
    }

    private static String str(JsonObject o, String key, String def) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : def;
    }

    private static void strings(JsonObject o, String key, List<String> out) {
        if (o.has(key) && o.get(key).isJsonArray()) {
            for (JsonElement e : o.getAsJsonArray(key)) {
                String s = e.getAsString().trim();
                if (!s.isEmpty()) {
                    out.add(s);
                }
            }
        }
    }

    private static void putStrings(JsonObject o, String key, List<String> list) {
        if (!list.isEmpty()) {
            JsonArray a = new JsonArray();
            for (String s : list) {
                a.add(s);
            }
            o.add(key, a);
        }
    }

    private static void putText(JsonObject o, String key, String v) {
        if (v != null && !v.isEmpty()) {
            o.addProperty(key, v);
        }
    }

    /** 파일에 쓰는 형식(parse 의 반대). 비어 있는 항목은 적지 않는다. */
    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("id", id);
        putText(o, "chain", chain);
        if (order != 0) {
            o.addProperty("order", order);
        }
        o.addProperty("title", title);
        o.addProperty("giver", giver);
        putText(o, "turnIn", turnIn);
        putStrings(o, "requires", requires);
        putStrings(o, "requiresFlags", requiresFlags);
        putStrings(o, "blockedByFlags", blockedByFlags);
        putText(o, "intro", intro);
        putText(o, "progress", progress);
        putText(o, "done", done);
        putText(o, "mod", mod);
        if (sceneAccept != null) o.add("sceneAccept", sceneAccept);
        if (sceneDone != null) o.add("sceneDone", sceneDone);
        if (sceneDream != null) o.add("sceneDream", sceneDream);
        if (companion != null) o.add("companion", companion);
        JsonArray objs = new JsonArray();
        for (Objective ob : objectives) {
            JsonObject jo = new JsonObject();
            jo.addProperty("type", ob.type.name());
            if (ob.type != Type.PAY) {
                jo.addProperty("target", ob.target);
            }
            jo.addProperty("count", ob.count);
            if (ob.type == Type.VISIT) {
                jo.addProperty("x", ob.x);
                jo.addProperty("y", ob.y);
                jo.addProperty("z", ob.z);
                jo.addProperty("radius", ob.radius);
            }
            putText(jo, "text", ob.text);
            putText(jo, "mod", ob.mod);
            if (ob.lines != null && ob.lines.size() > 0) jo.add("lines", ob.lines);
            putText(jo, "at", ob.at);
            putText(jo, "shape", ob.shape);
            putText(jo, "verb", ob.verb);
            putText(jo, "use", ob.use);
            if (!ob.use.isEmpty() && ob.useCount != 1) jo.addProperty("useCount", ob.useCount);
            putText(jo, "after", ob.after);
            objs.add(jo);
        }
        o.add("objectives", objs);
        JsonObject r = new JsonObject();
        if (rewardGold > 0) {
            r.addProperty("gold", rewardGold);
        }
        if (!rewardItems.isEmpty()) {
            JsonArray items = new JsonArray();
            for (ItemReward ir : rewardItems) {
                JsonObject jo = new JsonObject();
                jo.addProperty("item", ir.item);
                jo.addProperty("count", ir.count);
                putText(jo, "mod", ir.mod);
                items.add(jo);
            }
            r.add("items", items);
        }
        putStrings(r, "flags", rewardFlags);
        o.add("reward", r);
        if (!choices.isEmpty()) {
            JsonArray ch = new JsonArray();
            for (Choice c : choices) {
                JsonObject jo = new JsonObject();
                jo.addProperty("text", c.text);
                putStrings(jo, "flags", c.flags);
                ch.add(jo);
            }
            o.add("choices", ch);
        }
        return o;
    }

    /** 잘못된 정의면 이유를 담아 IllegalArgumentException. */
    public static QuestDef parse(JsonObject o) {
        QuestDef q = new QuestDef();
        q.id = str(o, "id", "").trim();
        if (q.id.isEmpty() || !q.id.matches("[a-z0-9_.-]+")) {
            throw new IllegalArgumentException("id 가 없거나 형식이 틀렸습니다 (영문 소문자·숫자·_ . - 만): '" + q.id + "'");
        }
        q.chain = str(o, "chain", "");
        q.title = str(o, "title", q.id);
        q.intro = str(o, "intro", "");
        q.progress = str(o, "progress", "");
        q.done = str(o, "done", "");
        q.giver = str(o, "giver", "").trim();
        q.turnIn = str(o, "turnIn", "").trim();
        q.order = o.has("order") ? o.get("order").getAsInt() : 0;
        q.mod = str(o, "mod", "").trim();
        if (o.has("sceneAccept") && o.get("sceneAccept").isJsonObject()) q.sceneAccept = o.getAsJsonObject("sceneAccept");
        if (o.has("sceneDone") && o.get("sceneDone").isJsonObject()) q.sceneDone = o.getAsJsonObject("sceneDone");
        if (o.has("sceneDream") && o.get("sceneDream").isJsonObject()) q.sceneDream = o.getAsJsonObject("sceneDream");
        if (o.has("companion") && o.get("companion").isJsonObject() && o.getAsJsonObject("companion").has("npc")) q.companion = o.getAsJsonObject("companion");
        if (q.giver.isEmpty()) {
            throw new IllegalArgumentException("퀘스트 '" + q.id + "' 에 giver(퀘스트를 주는 NPC)가 없습니다");
        }
        strings(o, "requires", q.requires);
        strings(o, "requiresFlags", q.requiresFlags);
        strings(o, "blockedByFlags", q.blockedByFlags);
        if (o.has("objectives") && o.get("objectives").isJsonArray()) {
            for (JsonElement e : o.getAsJsonArray("objectives")) {
                JsonObject jo = e.getAsJsonObject();
                Objective ob = new Objective();
                ob.type = Type.parse(str(jo, "type", ""));
                if (ob.type == null) {
                    throw new IllegalArgumentException("퀘스트 '" + q.id + "' 의 목표 종류를 알 수 없습니다: " + str(jo, "type", ""));
                }
                ob.target = str(jo, "target", "");
                ob.count = Math.max(1, jo.has("count") ? jo.get("count").getAsInt() : 1);
                ob.x = jo.has("x") ? jo.get("x").getAsInt() : 0;
                ob.y = jo.has("y") ? jo.get("y").getAsInt() : 0;
                ob.z = jo.has("z") ? jo.get("z").getAsInt() : 0;
                ob.radius = Math.max(1, jo.has("radius") ? jo.get("radius").getAsInt() : 8);
                ob.text = str(jo, "text", "");
                ob.mod = str(jo, "mod", "").trim();
                if (jo.has("lines") && jo.get("lines").isJsonArray()) ob.lines = jo.getAsJsonArray("lines");
                ob.at = str(jo, "at", "").trim();
                ob.shape = str(jo, "shape", "").trim();
                ob.verb = str(jo, "verb", "").trim();
                ob.use = str(jo, "use", "").trim();
                ob.useCount = Math.max(1, jo.has("useCount") ? jo.get("useCount").getAsInt() : 1);
                ob.after = str(jo, "after", "").trim();
                if (ob.type == Type.INSPECT) {
                    int cut = ob.target.indexOf('@');
                    if (cut > 0) { // "id@구조물" 로 한 줄에 적은 경우
                        if (ob.at.isEmpty()) ob.at = ob.target.substring(cut + 1).trim();
                        ob.target = ob.target.substring(0, cut).trim();
                    }
                    if (!ob.target.matches("[a-z0-9_]+")) {
                        throw new IllegalArgumentException("퀘스트 '" + q.id + "' 의 조사 목표 ID 는 영문 소문자·숫자·_ 만 쓸 수 있습니다: '" + ob.target + "'");
                    }
                    ob.count = 1;
                }
                if (ob.type != Type.PAY && ob.type != Type.VISIT && ob.target.isEmpty()) {
                    throw new IllegalArgumentException("퀘스트 '" + q.id + "' 의 " + ob.type + " 목표에 target 이 없습니다");
                }
                if (ob.type == Type.VISIT && ob.target.isEmpty()) {
                    ob.target = "minecraft:overworld";
                }
                q.objectives.add(ob);
            }
        }
        if (o.has("reward") && o.get("reward").isJsonObject()) {
            JsonObject r = o.getAsJsonObject("reward");
            q.rewardGold = Math.max(0, r.has("gold") ? r.get("gold").getAsLong() : 0);
            if (r.has("items") && r.get("items").isJsonArray()) {
                for (JsonElement e : r.getAsJsonArray("items")) {
                    JsonObject jo = e.getAsJsonObject();
                    ItemReward ir = new ItemReward();
                    ir.item = str(jo, "item", "");
                    ir.count = Math.max(1, jo.has("count") ? jo.get("count").getAsInt() : 1);
                    ir.mod = str(jo, "mod", "").trim();
                    if (!ir.item.isEmpty()) {
                        q.rewardItems.add(ir);
                    }
                }
            }
            strings(r, "flags", q.rewardFlags);
        }
        if (o.has("choices") && o.get("choices").isJsonArray()) {
            JsonArray arr = o.getAsJsonArray("choices");
            for (JsonElement e : arr) {
                JsonObject jo = e.getAsJsonObject();
                Choice c = new Choice();
                c.text = str(jo, "text", "");
                strings(jo, "flags", c.flags);
                if (!c.text.isEmpty()) {
                    q.choices.add(c);
                }
            }
        }
        return q;
    }
}

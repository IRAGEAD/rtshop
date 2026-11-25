package com.rtshop.gacha;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 뽑기 등급의 목록 (3.52): 기본 여섯 등급에 직접 만든 등급을 끼워 넣거나, 쓰지 않을 등급을 꺼 둔다.
 * gacha.json 의 "grades": [{id, name, color, fx, enabled}] — 낮은 등급부터.
 * <ul>
 * <li>기본 등급(common ~ yamato)은 목록에서 지울 수 없고 끌 수만 있다 (enabled: false = 그 등급은 뽑기에 나오지 않고 창에도 보이지 않는다).
 *     기본 등급끼리의 차례는 바뀌지 않는다.</li>
 * <li>직접 만든 등급: id(영문 소문자·숫자·_), name(화면에 보일 이름), color(RRGGBB), fx(연출: 0 일반 ~ 5 야마토 가운데 어느 것으로 보여 줄지).</li>
 * </ul>
 * 서버가 설정을 읽을 때 바뀌고, 접속한 사람에게 보낸다 (화면은 받은 목록으로 이름·색·연출을 정한다).
 */
public final class GachaGrades {
    private GachaGrades() {
    }

    public static final String[] BASE = {"common", "uncommon", "rare", "epic", "legendary", "yamato"};
    public static final int[] BASE_COLOR = {0xB8B8B8, 0x55C24A, 0x4A9BE8, 0xB45AE8, 0xF0A030, 0x49B6FF};
    public static final int MAX = 12;

    public static final class Def {
        public String id = "";
        /** 화면에 보일 이름 (기본 등급은 비워 두면 게임 언어의 이름). */
        public String name = "";
        public int color = 0xB8B8B8;
        /** 연출의 모습: 0 일반 · 1 고급 · 2 희귀 · 3 영웅 · 4 전설 · 5 야마토. */
        public int fx;
        public boolean enabled = true;

        public boolean base() {
            return baseIndex(id) >= 0;
        }

        public JsonObject toJson() {
            JsonObject o = new JsonObject();
            o.addProperty("id", id);
            o.addProperty("name", name);
            o.addProperty("color", String.format("%06X", color & 0xFFFFFF));
            o.addProperty("fx", fx);
            o.addProperty("enabled", enabled);
            return o;
        }

        public Def copy() {
            Def d = new Def();
            d.id = id;
            d.name = name;
            d.color = color;
            d.fx = fx;
            d.enabled = enabled;
            return d;
        }
    }

    private static volatile List<Def> defs = defaults();

    public static List<Def> defaults() {
        List<Def> l = new ArrayList<>();
        for (int i = 0; i < BASE.length; i++) {
            Def d = new Def();
            d.id = BASE[i];
            d.color = BASE_COLOR[i];
            d.fx = i;
            l.add(d);
        }
        return l;
    }

    public static List<Def> all() {
        return defs;
    }

    /** 이 목록을 지금의 등급으로 삼는다 (GachaConfig.GRADES 도 함께 바뀐다). */
    public static synchronized void apply(List<Def> list) {
        List<Def> copy = new ArrayList<>();
        for (Def d : list) copy.add(d.copy());
        defs = List.copyOf(copy);
        String[] ids = new String[copy.size()];
        for (int i = 0; i < ids.length; i++) ids[i] = copy.get(i).id;
        GachaConfig.GRADES = ids;
    }

    public static int baseIndex(String id) {
        for (int i = 0; i < BASE.length; i++) if (BASE[i].equals(id)) return i;
        return -1;
    }

    public static Def def(String id) {
        for (Def d : defs) if (d.id.equals(id)) return d;
        return null;
    }

    public static boolean enabled(String id) {
        Def d = def(id);
        return d != null && d.enabled;
    }

    /** 이 등급의 연출 모습 (0 ~ 5). 모르는 등급은 일반. */
    public static int fx(String id) {
        Def d = def(id);
        return d == null ? 0 : Math.max(0, Math.min(BASE.length - 1, d.fx));
    }

    public static int color(String id) {
        Def d = def(id);
        return d == null ? 0xB8B8B8 : d.color & 0xFFFFFF;
    }

    /** 직접 정한 이름 (없으면 빈 글자: 기본 등급은 게임 언어의 이름을 쓴다). */
    public static String customName(String id) {
        Def d = def(id);
        return d == null ? "" : d.name;
    }

    public static JsonArray toJson() {
        JsonArray a = new JsonArray();
        for (Def d : defs) a.add(d.toJson());
        return a;
    }

    public static List<Def> parse(String json) {
        return parse(JsonParser.parseString(json).getAsJsonArray());
    }

    /**
     * 목록을 읽는다. 잘못된 것은 까닭을 적어 던진다. 빠진 기본 등급은 제자리에 끼워 넣는다 (예전 파일).
     */
    public static List<Def> parse(JsonArray arr) {
        List<Def> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int lastBase = -1;
        for (JsonElement el : arr) {
            JsonObject o = el.getAsJsonObject();
            Def d = new Def();
            d.id = o.has("id") ? o.get("id").getAsString().trim() : "";
            if (!d.id.matches("[a-z0-9_]{1,32}")) {
                throw new IllegalArgumentException("등급 ID 는 영문 소문자·숫자·_ 로 1~32자여야 합니다: " + d.id);
            }
            if (!seen.add(d.id)) {
                throw new IllegalArgumentException("같은 등급 ID 가 두 번 적혀 있습니다: " + d.id);
            }
            int bi = baseIndex(d.id);
            if (bi >= 0) {
                if (bi < lastBase) {
                    throw new IllegalArgumentException("기본 등급의 차례는 바꿀 수 없습니다 (" + d.id + ")");
                }
                lastBase = bi;
            }
            d.name = o.has("name") ? o.get("name").getAsString().trim() : "";
            if (d.name.length() > 24) d.name = d.name.substring(0, 24);
            if (bi < 0 && d.name.isEmpty()) d.name = d.id;
            try {
                d.color = o.has("color") ? Integer.parseInt(o.get("color").getAsString().replace("#", "").trim(), 16) & 0xFFFFFF : (bi >= 0 ? BASE_COLOR[bi] : 0xB8B8B8);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("등급 색은 RRGGBB 로 적습니다 (" + d.id + ")");
            }
            d.fx = Math.max(0, Math.min(BASE.length - 1, o.has("fx") ? o.get("fx").getAsInt() : Math.max(0, bi)));
            if (bi >= 0) d.fx = bi; // 기본 등급의 연출은 제 것
            d.enabled = !o.has("enabled") || o.get("enabled").getAsBoolean();
            out.add(d);
        }
        // 빠진 기본 등급: 자기보다 높은 기본 등급 바로 앞(없으면 맨 뒤)에 넣는다
        for (int bi = 0; bi < BASE.length; bi++) {
            if (seen.contains(BASE[bi])) continue;
            Def d = new Def();
            d.id = BASE[bi];
            d.color = BASE_COLOR[bi];
            d.fx = bi;
            int at = out.size();
            for (int i = 0; i < out.size(); i++) {
                if (baseIndex(out.get(i).id) > bi) {
                    at = i;
                    break;
                }
            }
            out.add(at, d);
            seen.add(d.id);
        }
        if (out.size() > MAX) {
            throw new IllegalArgumentException("등급은 " + MAX + "개까지 만들 수 있습니다");
        }
        int on = 0;
        for (Def d : out) if (d.enabled) on++;
        if (on < 1) {
            throw new IllegalArgumentException("켜진 등급이 하나는 있어야 합니다");
        }
        return out;
    }
}
